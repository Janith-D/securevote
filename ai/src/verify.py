import argparse
import os
import cv2
import numpy as np
import logging
import json
from datetime import datetime
from pathlib import Path
from typing import Optional, Tuple, Dict
from utils import load_config, preprocess_image, detect_face, compute_embedding, cosine_similarity

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('verification.log'),
        logging.StreamHandler()
    ]
)

class FaceVerificationSystem:
    def __init__(self, config_path: str = r"D:\PROJECT\SecureVote\ai\config\model_config.json"):
        self.config = load_config(config_path)
        self.enrolled_dir = Path('data/enrolled')
        self.temp_dir = Path('data/temp')
        self.audit_dir = Path('data/audit')
        
        for directory in [self.enrolled_dir, self.temp_dir, self.audit_dir]:
            directory.mkdir(parents=True, exist_ok=True)
        
        self.similarity_threshold = self.config.get('similarity_threshold', 0.75)
        self.max_attempts = self.config.get('max_verification_attempts', 3)
        self.liveness_checks = self.config.get('enable_liveness_detection', True)
    
    def log_verification_attempt(self, user_id: str, similarity: float, result: bool, 
                               attempt_number: int = 1, notes: str = ""):
        audit_entry = {
            'timestamp': datetime.now().isoformat(),
            'user_id': user_id,
            'similarity_score': float(similarity),
            'threshold': self.similarity_threshold,
            'result': 'SUCCESS' if result else 'FAILURE',
            'attempt_number': attempt_number,
            'notes': notes
        }
        audit_file = self.audit_dir / f"verification_log_{datetime.now().strftime('%Y%m%d')}.jsonl"
        with open(audit_file, 'a') as f:
            f.write(json.dumps(audit_entry) + '\n')
        logging.info(f"Verification {audit_entry['result']} for {user_id}: {similarity:.3f}")
    
    def capture_live_image_advanced(self) -> str:
        for cam_index in [0, 1, 2]:  # Try multiple camera indices
            cap = cv2.VideoCapture(cam_index, cv2.CAP_DSHOW)  # Fallback to DirectShow
            if cap.isOpened():
                logging.info(f"Using camera index {cam_index} with DirectShow")
                break
        else:
            raise RuntimeError("Cannot access any webcam after trying indices 0, 1, 2")
        
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        cap.set(cv2.CAP_PROP_FPS, 30)
        
        logging.info("Starting live face capture for verification")
        try:
            for _ in range(10):
                ret, _ = cap.read()
                if not ret:
                    logging.warning("Failed to initialize frame capture")
                    break
            
            countdown = 5
            frames_with_faces = []
            blink_detected = False
            movement_detected = False
            previous_frame = None
            
            for i in range(countdown * 30):
                ret, frame = cap.read()
                if not ret:
                    logging.warning("Failed to read frame from webcam")
                    continue
                
                try:
                    face = detect_face(frame.copy())
                    if face is not None:
                        frames_with_faces.append((frame, face))
                        if self.liveness_checks and len(frames_with_faces) > 1:
                            if previous_frame is not None:
                                diff = cv2.absdiff(cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY),
                                                 cv2.cvtColor(previous_frame, cv2.COLOR_BGR2GRAY))
                                movement_score = np.mean(diff)
                                if movement_score > 5:
                                    movement_detected = True
                            previous_frame = frame.copy()
                except ValueError as e:
                    logging.debug(f"Face detection error: {e}")
                    pass
                
                remaining = max(0, countdown - i // 30)
                status_text = f"Look at camera - {remaining}s"
                if len(frames_with_faces) > 0:
                    status_text += " - Face detected"
                    if self.liveness_checks:
                        if movement_detected:
                            status_text += " - Movement OK"
                        else:
                            status_text += " - Please move slightly"
                
                cv2.putText(frame, status_text, (20, 30), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
                cv2.imshow("Face Verification", frame)
                if cv2.waitKey(33) & 0xFF == ord('q'):
                    raise KeyboardInterrupt("Verification cancelled by user")
            
            if not frames_with_faces:
                raise RuntimeError("No face detected during capture")
            
            best_frame, best_face = frames_with_faces[len(frames_with_faces)//2]
            if self.liveness_checks and not movement_detected:
                logging.warning("Potential liveness check failure - minimal movement detected")
            
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            temp_path = self.temp_dir / f"verify_{timestamp}.jpg"
            cv2.imwrite(str(temp_path), best_frame)
            logging.info(f"Live image captured: {temp_path}")
            return str(temp_path)
        finally:
            cap.release()
            cv2.destroyAllWindows()
    
    def validate_verification_conditions(self, user_id: str) -> Tuple[bool, str]:
        enrolled_path = self.enrolled_dir / f'{user_id}.npy'
        if not enrolled_path.exists():
            return False, f"User {user_id} is not enrolled in the system"
        try:
            enrolled_emb = np.load(enrolled_path)
            if enrolled_emb.size == 0:
                return False, "Invalid enrollment data - empty embedding"
        except Exception as e:
            return False, f"Cannot load enrollment data: {str(e)}"
        return True, "Validation passed"
    
    def verify_face_advanced(self, user_id: str, live_image_path: str, 
                           attempt_number: int = 1) -> Tuple[bool, float, str]:
        try:
            is_valid, validation_msg = self.validate_verification_conditions(user_id)
            if not is_valid:
                self.log_verification_attempt(user_id, 0.0, False, attempt_number, validation_msg)
                return False, 0.0, validation_msg
            
            enrolled_path = self.enrolled_dir / f'{user_id}.npy'
            enrolled_emb = np.load(enrolled_path)
            live_img = preprocess_image(live_image_path, self.config['image_size'])
            live_face = detect_face(live_img)
            
            if live_face is None:
                msg = "No face detected in live image"
                self.log_verification_attempt(user_id, 0.0, False, attempt_number, msg)
                return False, 0.0, msg
            
            if 'face_size' in self.config:
                live_face = cv2.resize(live_face, tuple(self.config['face_size']))
            
            live_emb = compute_embedding(live_face)
            if live_emb is None or len(live_emb) == 0:
                msg = "Failed to compute embedding from live image"
                self.log_verification_attempt(user_id, 0.0, False, attempt_number, msg)
                return False, 0.0, msg
            
            similarity = cosine_similarity(enrolled_emb, live_emb)
            is_verified = similarity >= self.similarity_threshold
            
            if is_verified:
                status_msg = f"Verification successful (similarity: {similarity:.3f})"
            else:
                status_msg = f"Verification failed - insufficient similarity ({similarity:.3f} < {self.similarity_threshold})"
            
            self.log_verification_attempt(user_id, similarity, is_verified, attempt_number, status_msg)
            return is_verified, similarity, status_msg
        except Exception as e:
            error_msg = f"Verification error: {str(e)}"
            logging.error(error_msg)
            self.log_verification_attempt(user_id, 0.0, False, attempt_number, error_msg)
            return False, 0.0, error_msg
    
    def verify_with_retry(self, user_id: str, live_image_path: Optional[str] = None) -> Dict:
        temp_image_path = None
        result = {
            'user_id': user_id,
            'verification_result': False,
            'attempts': [],
            'final_message': '',
            'timestamp': datetime.now().isoformat()
        }
        
        try:
            for attempt in range(1, self.max_attempts + 1):
                logging.info(f"Verification attempt {attempt}/{self.max_attempts} for user {user_id}")
                
                if live_image_path is None:
                    try:
                        temp_image_path = self.capture_live_image_advanced()
                        current_image_path = temp_image_path
                    except Exception as e:
                        attempt_result = {
                            'attempt_number': attempt,
                            'success': False,
                            'similarity': 0.0,
                            'message': f"Image capture failed: {str(e)}"
                        }
                        result['attempts'].append(attempt_result)
                        continue
                else:
                    current_image_path = live_image_path
                
                is_verified, similarity, message = self.verify_face_advanced(
                    user_id, current_image_path, attempt
                )
                attempt_result = {
                    'attempt_number': attempt,
                    'success': is_verified,
                    'similarity': similarity,
                    'message': message
                }
                result['attempts'].append(attempt_result)
                
                if is_verified:
                    result['verification_result'] = True
                    result['final_message'] = f"Verification successful on attempt {attempt}"
                    break
                
                if attempt < self.max_attempts:
                    logging.info(f"Attempt {attempt} failed. {self.max_attempts - attempt} attempts remaining.")
                    if live_image_path is None:
                        print(f"Verification failed. Please try again. ({self.max_attempts - attempt} attempts remaining)")
                
                if temp_image_path and os.path.exists(temp_image_path):
                    os.remove(temp_image_path)
                    temp_image_path = None
            
            if not result['verification_result']:
                result['final_message'] = f"Verification failed after {self.max_attempts} attempts"
            
        except KeyboardInterrupt:
            result['final_message'] = "Verification cancelled by user"
            logging.info("Verification cancelled by user")
        
        except Exception as e:
            result['final_message'] = f"Verification system error: {str(e)}"
            logging.error(f"System error during verification: {str(e)}")
        
        finally:
            if temp_image_path and os.path.exists(temp_image_path):
                try:
                    os.remove(temp_image_path)
                except OSError:
                    pass
        
        return result
    
    def get_verification_statistics(self, user_id: Optional[str] = None, days: int = 7) -> Dict:
        from datetime import timedelta
        cutoff_date = datetime.now() - timedelta(days=days)
        stats = {
            'total_attempts': 0,
            'successful_verifications': 0,
            'failed_verifications': 0,
            'average_similarity': 0.0,
            'users_verified': set()
        }
        for audit_file in self.audit_dir.glob("verification_log_*.jsonl"):
            try:
                with open(audit_file, 'r') as f:
                    for line in f:
                        entry = json.loads(line.strip())
                        entry_date = datetime.fromisoformat(entry['timestamp'])
                        if entry_date >= cutoff_date:
                            if user_id is None or entry['user_id'] == user_id:
                                stats['total_attempts'] += 1
                                if entry['result'] == 'SUCCESS':
                                    stats['successful_verifications'] += 1
                                    stats['users_verified'].add(entry['user_id'])
                                else:
                                    stats['failed_verifications'] += 1
            except Exception as e:
                logging.warning(f"Error reading audit file {audit_file}: {e}")
        if stats['total_attempts'] > 0:
            stats['success_rate'] = stats['successful_verifications'] / stats['total_attempts']
        else:
            stats['success_rate'] = 0.0
        stats['users_verified'] = len(stats['users_verified'])
        return stats

def main():
    parser = argparse.ArgumentParser(
        description="Verify a voter's face for secure voting system",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python verification.py --user_id VOTER001                    # Live verification
  python verification.py --user_id VOTER001 --image photo.jpg  # From file
  python verification.py --stats                               # Show statistics
  python verification.py --stats --user_id VOTER001           # User-specific stats
        """
    )
    
    parser.add_argument('--user_id', help="Unique voter ID to verify")
    parser.add_argument('--image', help="Path to live face image (optional, uses webcam if omitted)")
    parser.add_argument('--stats', action='store_true', help="Show verification statistics")
    parser.add_argument('--days', type=int, default=7, help="Days to look back for statistics")
    parser.add_argument('--config', default=r"D:\PROJECT\SecureVote\ai\config\model_config.json",
                       help="Path to configuration file")
    
    args = parser.parse_args()
    
    try:
        verification_system = FaceVerificationSystem(args.config)
    except Exception as e:
        logging.error(f"Failed to initialize verification system: {e}")
        return 1
    
    if args.stats:
        stats = verification_system.get_verification_statistics(args.user_id, args.days)
        print(f"\nVerification Statistics (last {args.days} days):")
        if args.user_id:
            print(f"User: {args.user_id}")
        print(f"Total attempts: {stats['total_attempts']}")
        print(f"Successful verifications: {stats['successful_verifications']}")
        print(f"Failed verifications: {stats['failed_verifications']}")
        print(f"Success rate: {stats['success_rate']:.1%}")
        if not args.user_id:
            print(f"Unique users verified: {stats['users_verified']}")
        return 0
    
    if not args.user_id:
        parser.error("--user_id is required for verification")
    
    result = verification_system.verify_with_retry(args.user_id, args.image)
    
    print(f"\n{'='*50}")
    print(f"VERIFICATION RESULT FOR USER: {args.user_id}")
    print(f"{'='*50}")
    print(f"Status: {'✓ SUCCESS' if result['verification_result'] else '✗ FAILURE'}")
    print(f"Message: {result['final_message']}")
    print(f"Attempts made: {len(result['attempts'])}")
    
    for attempt in result['attempts']:
        print(f"\nAttempt {attempt['attempt_number']}:")
        print(f"  Result: {'Success' if attempt['success'] else 'Failed'}")
        print(f"  Similarity: {attempt['similarity']:.3f}")
        print(f"  Details: {attempt['message']}")
    
    print(f"\nTimestamp: {result['timestamp']}")
    print(f"{'='*50}")
    
    return 0 if result['verification_result'] else 1

if __name__ == '__main__':
    exit(main())