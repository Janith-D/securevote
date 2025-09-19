import argparse
import os
import cv2
import numpy as np
import logging
import json
from datetime import datetime
from pathlib import Path
from typing import Optional, Tuple
from utils import load_config, preprocess_image, detect_face, compute_embedding

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('enrollment.log'),
        logging.StreamHandler()
    ]
)

class FaceEnrollmentSystem:
    def __init__(self, config_path: str = r"D:\PROJECT\SecureVote\ai\config\model_config.json"):
        self.config = load_config(config_path)
        self.enrolled_dir = Path('data/enrolled')
        self.temp_dir = Path('data/temp')
        self.metadata_dir = Path('data/metadata')
        
        # Create directories if they don't exist
        for directory in [self.enrolled_dir, self.temp_dir, self.metadata_dir]:
            directory.mkdir(parents=True, exist_ok=True)
    
    def capture_live_image(self, countdown: int = 3) -> str:
        cap = cv2.VideoCapture(0)
        
        if not cap.isOpened():
            raise RuntimeError("Cannot access webcam")
        
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        cap.set(cv2.CAP_PROP_FPS, 30)
        
        logging.info("Starting face capture sequence")
        
        try:
            for i in range(30):
                ret, frame = cap.read()
                if not ret:
                    continue
                    
                remaining = max(0, countdown - i // 10)
                if remaining > 0:
                    cv2.putText(frame, f"Align face - Capturing in {remaining}s", 
                               (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                else:
                    cv2.putText(frame, "Hold still - Capturing...", 
                               (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
                
                cv2.imshow("Face Enrollment", frame)
                
                if cv2.waitKey(33) & 0xFF == ord('q'):
                    raise KeyboardInterrupt("Capture cancelled by user")
            
            frames = []
            for _ in range(5):
                ret, frame = cap.read()
                if ret:
                    frames.append(frame)
            
            if not frames:
                raise RuntimeError("Failed to capture any frames")
            
            best_frame = frames[len(frames)//2]
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            temp_path = self.temp_dir / f"live_{timestamp}.jpg"
            cv2.imwrite(str(temp_path), best_frame)
            
            logging.info(f"Live image captured: {temp_path}")
            return str(temp_path)
            
        finally:
            cap.release()
            cv2.destroyAllWindows()
    
    def validate_face_quality(self, face: np.ndarray) -> Tuple[bool, str]:
        if face is None or face.size == 0:
            return False, "No face detected"
        
        min_size = self.config.get('min_face_size', 80)
        if min(face.shape[:2]) < min_size:
            return False, f"Face too small (minimum {min_size}x{min_size})"
        
        brightness = np.mean(face)
        if brightness < 50:
            return False, "Face too dark"
        elif brightness > 200:
            return False, "Face too bright"
        
        gray_face = cv2.cvtColor(face, cv2.COLOR_BGR2GRAY) if len(face.shape) == 3 else face
        blur_score = cv2.Laplacian(gray_face, cv2.CV_64F).var()
        min_blur_score = self.config.get('min_blur_score', 100)
        
        if blur_score < min_blur_score:
            return False, f"Face too blurry (score: {blur_score:.2f})"
        
        return True, "Face quality acceptable"
    
    def save_enrollment_metadata(self, user_id: str, image_path: str, embedding_quality: float = None):
        metadata = {
            'user_id': user_id,
            'enrollment_date': datetime.now().isoformat(),
            'image_path': image_path,
            'embedding_quality': embedding_quality,
            'config_version': self.config.get('version', '1.0')
        }
        
        metadata_path = self.metadata_dir / f"{user_id}_metadata.json"
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        logging.info(f"Metadata saved: {metadata_path}")
    
    def check_existing_enrollment(self, user_id: str) -> bool:
        enrollment_path = self.enrolled_dir / f"{user_id}.npy"
        return enrollment_path.exists()
    
    def enroll_face(self, user_id: str, image_path: Optional[str] = None, overwrite: bool = False) -> bool:
        temp_image_path = None
        
        try:
            if self.check_existing_enrollment(user_id) and not overwrite:
                logging.warning(f"User {user_id} already enrolled. Use --overwrite to replace.")
                return False
            
            if image_path is None:
                temp_image_path = self.capture_live_image()
                image_path = temp_image_path
            elif not os.path.exists(image_path):
                raise FileNotFoundError(f"Image file not found: {image_path}")
            
            logging.info(f"Processing enrollment for user: {user_id}")
            
            img = preprocess_image(image_path, self.config['image_size'])
            face = detect_face(img, (self.config['min_face_size'], self.config['min_face_size']))
            
            if face is None:
                raise ValueError("No face detected in the image")
            
            is_valid, quality_msg = self.validate_face_quality(face)
            if not is_valid:
                raise ValueError(f"Face quality check failed: {quality_msg}")
            
            logging.info(f"Face quality check passed: {quality_msg}")
            
            face_resized = cv2.resize(face, tuple(self.config['face_size']))
            embedding = compute_embedding(face_resized, method='simple')
            
            if embedding is None or len(embedding) == 0:
                raise ValueError("Failed to compute face embedding")
            
            embedding_quality = float(np.linalg.norm(embedding))
            
            enrollment_path = self.enrolled_dir / f"{user_id}.npy"
            np.save(enrollment_path, embedding)
            
            self.save_enrollment_metadata(user_id, image_path, embedding_quality)
            
            logging.info(f"Successfully enrolled user {user_id} (quality score: {embedding_quality:.3f})")
            return True
            
        except KeyboardInterrupt:
            logging.info("Enrollment cancelled by user")
            return False
        except Exception as e:
            logging.error(f"Enrollment failed for user {user_id}: {str(e)}")
            return False
        finally:
            if temp_image_path and os.path.exists(temp_image_path):
                try:
                    os.remove(temp_image_path)
                    logging.debug(f"Cleaned up temporary file: {temp_image_path}")
                except OSError as e:
                    logging.warning(f"Failed to remove temporary file {temp_image_path}: {e}")
    
    def list_enrolled_users(self) -> list:
        enrolled_files = list(self.enrolled_dir.glob("*.npy"))
        return [f.stem for f in enrolled_files]
    
    def get_enrollment_info(self, user_id: str) -> dict:
        if not self.check_existing_enrollment(user_id):
            return {"error": "User not enrolled"}
        
        metadata_path = self.metadata_dir / f"{user_id}_metadata.json"
        if metadata_path.exists():
            with open(metadata_path, 'r') as f:
                return json.load(f)
        else:
            return {"user_id": user_id, "metadata": "Not available"}

def main():
    parser = argparse.ArgumentParser(
        description="Enroll a voter's face for secure voting system",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python enrollment.py --user_id VOTER001                    # Live capture
  python enrollment.py --user_id VOTER001 --image_path photo.jpg  # From file
  python enrollment.py --user_id VOTER001 --overwrite        # Replace existing
  python enrollment.py --list                                # List enrolled users
        """
    )
    
    parser.add_argument('--user_id', help="Unique voter ID")
    parser.add_argument('--image_path', help="Path to face image (optional, uses webcam if omitted)")
    parser.add_argument('--overwrite', action='store_true', help="Overwrite existing enrollment")
    parser.add_argument('--list', action='store_true', help="List all enrolled users")
    parser.add_argument('--info', help="Get enrollment info for specific user")
    parser.add_argument('--config', default=r"D:\PROJECT\SecureVote\ai\config\model_config.json", 
                       help="Path to configuration file")
    
    args = parser.parse_args()
    
    try:
        enrollment_system = FaceEnrollmentSystem(args.config)
    except Exception as e:
        logging.error(f"Failed to initialize enrollment system: {e}")
        return 1
    
    if args.list:
        users = enrollment_system.list_enrolled_users()
        print(f"Enrolled users ({len(users)}):")
        for user in sorted(users):
            print(f"  - {user}")
        return 0
    
    if args.info:
        info = enrollment_system.get_enrollment_info(args.info)
        print(f"Enrollment info for {args.info}:")
        print(json.dumps(info, indent=2))
        return 0
    
    if not args.user_id:
        parser.error("--user_id is required for enrollment")
    
    success = enrollment_system.enroll_face(args.user_id, args.image_path, args.overwrite)
    return 0 if success else 1

if __name__ == '__main__':
    exit(main())