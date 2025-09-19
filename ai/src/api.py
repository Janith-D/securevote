from flask import Flask, request, jsonify
from flask_cors import CORS
from werkzeug.utils import secure_filename
import os
import shutil
import logging
import json
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, Any, Optional
import uuid
import numpy as np
import base64
from web3 import Web3

# Import enrollment, verification, and utils
from enroll import FaceEnrollmentSystem
from verify import FaceVerificationSystem
from utils import load_config, preprocess_image, detect_face, compute_embedding, cosine_similarity

# Configure Web3 and Voting contract
w3 = Web3(Web3.HTTPProvider("http://127.0.0.1:7545"))  # Ganache URL
with open(r"D:\PROJECT\SecureVote\blockchain\build\contracts\Voting.json") as f:
    voting_abi = json.load(f)["abi"]
voting_address = "0x73Ef8A772EB83F491a9215FDE27B06b76D2403dd"  # Replace with actual address
voting_contract = w3.eth.contract(address=voting_address, abi=voting_abi)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.FileHandler('api.log'), logging.StreamHandler()]
)

app = Flask(__name__)
CORS(app)

# Load configuration
config = load_config(r"D:\PROJECT\SecureVote\ai\config\api_config.json")
model_config = load_config(r"D:\PROJECT\SecureVote\ai\config\model_config.json")

# Initialize systems
enrollment_system = FaceEnrollmentSystem()
verification_system = FaceVerificationSystem()
logging.info("Face recognition systems initialized successfully")

os.makedirs('data/temp', exist_ok=True)
os.makedirs('data/api_logs', exist_ok=True)

app.config['MAX_CONTENT_LENGTH'] = config.get('max_file_size', 5 * 1024 * 1024)
ALLOWED_EXTENSIONS = set(config.get('allowed_extensions', ['jpg', 'jpeg', 'png']))

rate_limit_storage = {}

def allowed_file(filename: str) -> bool:
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def check_rate_limit(client_ip: str) -> bool:
    rate_limit = config.get('rate_limit_per_minute', 60)
    current_time = datetime.now()
    
    if client_ip not in rate_limit_storage:
        rate_limit_storage[client_ip] = []
    
    rate_limit_storage[client_ip] = [
        req_time for req_time in rate_limit_storage[client_ip]
        if current_time - req_time < timedelta(minutes=1)
    ]
    
    if len(rate_limit_storage[client_ip]) >= rate_limit:
        return False
    
    rate_limit_storage[client_ip].append(current_time)
    return True

def validate_api_key() -> bool:
    if not config.get('enable_api_key', False):
        return True
    api_key = request.headers.get('X-API-Key') or request.form.get('api_key')
    expected_key = config.get('api_key')
    return api_key == expected_key

def log_api_request(endpoint: str, user_id: str = None, success: bool = True, error_msg: str = None) -> None:
    log_entry = {
        'timestamp': datetime.now().isoformat(),
        'endpoint': endpoint,
        'user_id': user_id,
        'client_ip': request.remote_addr,
        'success': success,
        'error_message': error_msg,
        'user_agent': request.headers.get('User-Agent', '')
    }
    log_file = Path('data/api_logs') / f"api_log_{datetime.now().strftime('%Y%m%d')}.jsonl"
    with open(log_file, 'a') as f:
        f.write(json.dumps(log_entry) + '\n')

def save_uploaded_file(file, user_id: str = None, prefix: str = 'upload') -> str:
    if not file or not allowed_file(file.filename):
        raise ValueError("Invalid file type")
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    unique_id = str(uuid.uuid4())[:8]
    
    if user_id:
        filename = f"{prefix}_{user_id}_{timestamp}_{unique_id}.jpg"
    else:
        filename = f"{prefix}_{timestamp}_{unique_id}.jpg"
    
    filepath = os.path.join('data/temp', secure_filename(filename))
    file.save(filepath)
    
    return filepath

@app.before_request
def before_request():
    if request.endpoint == 'health_check' or request.endpoint == 'blockchain':
        return
    if not check_rate_limit(request.remote_addr):
        log_api_request(request.endpoint, error_msg="Rate limit exceeded")
        return jsonify({'error': 'Rate limit exceeded. Please try again later.'}), 429
    if not validate_api_key():
        log_api_request(request.endpoint, error_msg="Invalid API key")
        return jsonify({'error': 'Invalid or missing API key'}), 401

@app.route('/health', methods=['GET'])
def health_check():
    status = {
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'enrollment_system': enrollment_system is not None,
        'verification_system': verification_system is not None,
        'blockchain': w3.is_connected()
    }
    return jsonify(status), 200

@app.route('/enroll', methods=['POST'])
def enroll():
    user_id = None
    temp_file_path = None
    
    try:
        if enrollment_system is None:
            raise RuntimeError("Enrollment system not initialized")
        
        user_id = request.form.get('user_id')
        if not user_id:
            raise ValueError("user_id is required")
        
        if 'image' not in request.files:
            raise ValueError("No image file provided")
        
        file = request.files['image']
        if file.filename == '':
            raise ValueError("No image file selected")
        
        temp_file_path = save_uploaded_file(file, user_id, 'enroll')
        success = enrollment_system.enroll_face(user_id, temp_file_path, overwrite=True)
        
        if not success:
            raise ValueError("Enrollment failed")
        
        enrollment_path = Path('data/enrolled') / f"{user_id}.npy"
        embedding = np.load(enrollment_path)
        encoding_base64 = base64.b64encode(embedding.tobytes()).decode()
        
        admin_account = w3.eth.accounts[0]
        tx_hash = voting_contract.functions.registerVoter(w3.to_checksum_address(admin_account)).transact({'from': admin_account})
        w3.eth.wait_for_transaction_receipt(tx_hash)
        
        log_api_request('enroll', user_id, True)
        
        return jsonify({
            'status': 'success',
            'face_encoding': encoding_base64
        }), 200
        
    except ValueError as e:
        error_msg = f"Validation error: {str(e)}"
        log_api_request('enroll', user_id, False, error_msg)
        return jsonify({'error': error_msg}), 400
    except RuntimeError as e:
        error_msg = f"System error: {str(e)}"
        log_api_request('enroll', user_id, False, error_msg)
        return jsonify({'error': error_msg}), 503
    except Exception as e:
        error_msg = f"Unexpected error during enrollment: {str(e)}"
        logging.error(error_msg)
        log_api_request('enroll', user_id, False, error_msg)
        return jsonify({'error': 'Internal server error'}), 500
    finally:
        if temp_file_path and os.path.exists(temp_file_path):
            try:
                os.remove(temp_file_path)
            except OSError:
                logging.warning(f"Failed to clean up temp file: {temp_file_path}")

@app.route('/verify', methods=['POST'])
def verify():
    user_id = None
    temp_file_path = None
    
    try:
        if verification_system is None:
            raise RuntimeError("Verification system not initialized")
        
        user_id = request.form.get('walletAddress')
        if not user_id:
            raise ValueError("walletAddress is required")
        
        if 'image' not in request.files:
            raise ValueError("No image file provided")
        
        file = request.files['image']
        if file.filename == '':
            raise ValueError("No image file selected")
        
        temp_file_path = save_uploaded_file(file, user_id, 'verify')
        result = verification_system.verify_with_retry(user_id, temp_file_path)
        
        # Convert numpy.bool_ to Python bool in result
        result['verification_result'] = bool(result['verification_result'])
        for attempt in result['attempts']:
            if isinstance(attempt['success'], np.bool_):
                attempt['success'] = bool(attempt['success'])
            if isinstance(attempt['similarity'], np.floating):
                attempt['similarity'] = float(attempt['similarity'])
        
        log_api_request('verify', user_id, result['verification_result'])
        
        response = {
            'verification_result': result['verification_result'],
            'final_message': result['final_message'],
            'attempts': result['attempts']
        }
        return jsonify(response), 200 if result['verification_result'] else 400
        
    except ValueError as e:
        error_msg = f"Validation error: {str(e)}"
        log_api_request('verify', user_id, False, error_msg)
        return jsonify({'error': error_msg}), 400
    except RuntimeError as e:
        error_msg = f"System error: {str(e)}"
        log_api_request('verify', user_id, False, error_msg)
        return jsonify({'error': error_msg}), 503
    except Exception as e:
        error_msg = f"Unexpected error during verification: {str(e)}"
        logging.error(error_msg)
        log_api_request('verify', user_id, False, error_msg)
        return jsonify({'error': 'Internal server error'}), 500
    finally:
        if temp_file_path and os.path.exists(temp_file_path):
            try:
                os.remove(temp_file_path)
            except OSError:
                logging.warning(f"Failed to clean up temp file: {temp_file_path}")


@app.route('/user/<user_id>/status', methods=['GET'])
def get_user_status(user_id: str):
    try:
        if enrollment_system is None:
            raise RuntimeError("Enrollment system not initialized")
        
        enrolled_path = Path('data/enrolled') / f'{user_id}.npy'
        is_enrolled = enrolled_path.exists()
        
        response = {
            'user_id': user_id,
            'enrolled': is_enrolled,
            'enrollment_file_exists': is_enrolled
        }
        
        if is_enrolled:
            stat = enrolled_path.stat()
            response['enrollment_date'] = datetime.fromtimestamp(stat.st_ctime).isoformat()
            response['file_size'] = stat.st_size
        
        return jsonify(response), 200
    except Exception as e:
        error_msg = f"Error checking user status: {str(e)}"
        logging.error(error_msg)
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/statistics', methods=['GET'])
def get_statistics():
    try:
        if verification_system is None:
            raise RuntimeError("Verification system not initialized")
        
        days = request.args.get('days', 7, type=int)
        user_id = request.args.get('user_id')
        
        stats = verification_system.get_verification_statistics(user_id, days)
        
        response = {
            'period_days': days,
            'statistics': stats,
            'timestamp': datetime.now().isoformat()
        }
        
        if user_id:
            response['user_id'] = user_id
        
        return jsonify(response), 200
    except Exception as e:
        error_msg = f"Error getting statistics: {str(e)}"
        logging.error(error_msg)
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/users', methods=['GET'])
def list_users():
    try:
        enrolled_dir = Path('data/enrolled')
        if not enrolled_dir.exists():
            return jsonify({'users': [], 'count': 0}), 200
        
        users = []
        for file_path in enrolled_dir.glob('*.npy'):
            user_id = file_path.stem
            stat = file_path.stat()
            users.append({
                'user_id': user_id,
                'enrollment_date': datetime.fromtimestamp(stat.st_ctime).isoformat(),
                'file_size': stat.st_size
            })
        
        users.sort(key=lambda x: x['enrollment_date'], reverse=True)
        
        response = {
            'users': users,
            'count': len(users),
            'timestamp': datetime.now().isoformat()
        }
        
        return jsonify(response), 200
    except Exception as e:
        error_msg = f"Error listing users: {str(e)}"
        logging.error(error_msg)
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/blockchain', methods=['GET'])
def get_blockchain():
    return jsonify({"chain": w3.eth.get_block('latest')}), 200

@app.errorhandler(413)
def request_entity_too_large(error):
    max_size_mb = config.get('max_file_size', 5 * 1024 * 1024) / (1024 * 1024)
    return jsonify({
        'error': f'File too large. Maximum size allowed: {max_size_mb:.1f}MB'
    }), 413

@app.errorhandler(404)
def not_found(error):
    return jsonify({'error': 'Endpoint not found'}), 404

@app.errorhandler(405)
def method_not_allowed(error):
    return jsonify({'error': 'Method not allowed for this endpoint'}), 405

def cleanup_temp_files():
    temp_dir = Path('data/temp')
    if not temp_dir.exists():
        return
    cutoff_time = datetime.now() - timedelta(hours=1)
    for temp_file in temp_dir.glob('*'):
        try:
            if datetime.fromtimestamp(temp_file.stat().st_ctime) < cutoff_time:
                temp_file.unlink()
        except Exception:
            pass

if __name__ == '__main__':
    if os.path.exists('data/temp'):
        shutil.rmtree('data/temp')
    os.makedirs('data/temp', exist_ok=True)
    
    cleanup_temp_files()
    
    logging.info(f"Starting Face Verification API on {config['host']}:{config['port']}")
    app.run(
        host=config.get('host', '127.0.0.1'),
        port=config.get('port', 5000),
        debug=config.get('debug', False)
    )