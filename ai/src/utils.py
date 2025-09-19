import cv2
import numpy as np
import json
import os
import logging
from typing import Optional, Tuple, Union

def load_config(config_path: str) -> dict:
    """
    Load configuration from JSON file with validation
    
    Args:
        config_path: Path to the configuration file
        
    Returns:
        Dictionary containing configuration parameters
        
    Raises:
        FileNotFoundError: If config file doesn't exist
        ValueError: If config file is invalid JSON
    """
    if not os.path.exists(config_path):
        raise FileNotFoundError(f"Configuration file not found: {config_path}")
    
    try:
        with open(config_path, 'r') as f:
            config = json.load(f)
        
        # Validate required configuration parameters
        required_params = ['image_size', 'similarity_threshold']
        for param in required_params:
            if param not in config:
                logging.warning(f"Missing configuration parameter: {param}")
        
        # Set default values if missing
        config.setdefault('similarity_threshold', 0.75)
        config.setdefault('image_size', [640, 480])
        config.setdefault('face_size', [224, 224])
        config.setdefault('max_verification_attempts', 3)
        config.setdefault('enable_liveness_detection', True)
        config.setdefault('min_face_size', 80)
        config.setdefault('min_blur_score', 100)
        
        return config
        
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON in configuration file: {e}")

def preprocess_image(image_path: str, size: Union[list, tuple]) -> np.ndarray:
    """
    Load and preprocess image with enhanced error handling
    
    Args:
        image_path: Path to the image file
        size: Target size as [width, height] or (width, height)
        
    Returns:
        Preprocessed image as numpy array
        
    Raises:
        ValueError: If image cannot be loaded or processed
    """
    if not os.path.exists(image_path):
        raise ValueError(f"Image file not found: {image_path}")
    
    # Load image
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"Failed to load image: {image_path}")
    
    # Validate image
    if img.size == 0:
        raise ValueError(f"Empty image: {image_path}")
    
    # Convert size to tuple if it's a list
    if isinstance(size, list):
        size = tuple(size)
    
    # Resize image
    try:
        img = cv2.resize(img, size)
    except Exception as e:
        raise ValueError(f"Failed to resize image: {e}")
    
    return img

def detect_face(image: np.ndarray, min_face_size: Tuple[int, int] = (30, 30)) -> Optional[np.ndarray]:
    """
    Detect face in image using Haar cascades with multiple detection strategies
    
    Args:
        image: Input image as numpy array
        min_face_size: Minimum face size as (width, height)
        
    Returns:
        Detected face region as numpy array, or None if no face found
        
    Raises:
        ValueError: If input image is invalid
    """
    if image is None or image.size == 0:
        raise ValueError("Invalid input image for face detection")
    
    # Initialize face cascade
    try:
        face_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        )
    except Exception as e:
        raise ValueError(f"Failed to load face cascade classifier: {e}")
    
    # Convert to grayscale for detection
    if len(image.shape) == 3:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    else:
        gray = image.copy()
    
    # Apply histogram equalization for better detection
    gray = cv2.equalizeHist(gray)
    
    # Try multiple detection parameters for better accuracy
    detection_params = [
        {'scaleFactor': 1.1, 'minNeighbors': 5, 'minSize': min_face_size},
        {'scaleFactor': 1.2, 'minNeighbors': 4, 'minSize': min_face_size},
        {'scaleFactor': 1.3, 'minNeighbors': 3, 'minSize': min_face_size},
        {'scaleFactor': 1.1, 'minNeighbors': 3, 'minSize': min_face_size},
        {'scaleFactor': 1.05, 'minNeighbors': 6, 'minSize': min_face_size}
    ]
    
    best_face = None
    best_confidence = 0
    
    for params in detection_params:
        try:
            faces = face_cascade.detectMultiScale(gray, **params)
            
            if len(faces) > 0:
                # If multiple faces detected, choose the largest one
                if len(faces) > 1:
                    areas = [w * h for (x, y, w, h) in faces]
                    largest_face_idx = np.argmax(areas)
                    x, y, w, h = faces[largest_face_idx]
                else:
                    x, y, w, h = faces[0]
                
                # Calculate confidence based on face size and position
                face_area = w * h
                image_area = gray.shape[0] * gray.shape[1]
                area_ratio = face_area / image_area
                
                # Prefer faces that are not too small or too large
                if 0.01 < area_ratio < 0.8:
                    confidence = area_ratio * (1 - abs(0.3 - area_ratio))  # Peak at 30% of image
                    
                    if confidence > best_confidence:
                        best_confidence = confidence
                        # Extract face region with some padding
                        padding = int(min(w, h) * 0.1)  # 10% padding
                        y1 = max(0, y - padding)
                        y2 = min(image.shape[0], y + h + padding)
                        x1 = max(0, x - padding)
                        x2 = min(image.shape[1], x + w + padding)
                        
                        best_face = image[y1:y2, x1:x2]
        
        except Exception as e:
            logging.warning(f"Face detection failed with params {params}: {e}")
            continue
    
    if best_face is not None:
        # Additional quality checks
        if is_valid_face(best_face):
            return best_face
    
    return None

def is_valid_face(face_image: np.ndarray) -> bool:
    """
    Validate if detected face meets quality requirements
    
    Args:
        face_image: Detected face region
        
    Returns:
        True if face meets quality requirements, False otherwise
    """
    if face_image is None or face_image.size == 0:
        return False
    
    # Check minimum size
    if min(face_image.shape[:2]) < 50:
        return False
    
    # Check if image is too dark or too bright
    brightness = np.mean(face_image)
    if brightness < 30 or brightness > 220:
        return False
    
    # Check for sufficient contrast
    gray_face = cv2.cvtColor(face_image, cv2.COLOR_BGR2GRAY) if len(face_image.shape) == 3 else face_image
    contrast = np.std(gray_face)
    if contrast < 20:  # Too low contrast
        return False
    
    return True

def compute_embedding(image: np.ndarray, method: str = 'simple') -> Optional[np.ndarray]:
    """
    Compute face embedding using specified method
    
    Args:
        image: Input face image
        method: Embedding computation method ('simple', 'histogram', 'lbp')
        
    Returns:
        Face embedding as numpy array, or None if computation fails
    """
    if image is None or image.size == 0:
        return None
    
    try:
        if method == 'simple':
            # Simple pixel-based embedding (normalized)
            img = image.astype(np.float32) / 255.0
            embedding = img.flatten()
            
        elif method == 'histogram':
            # Color histogram-based embedding
            if len(image.shape) == 3:
                # Compute histogram for each channel
                hist_b = cv2.calcHist([image], [0], None, [64], [0, 256])
                hist_g = cv2.calcHist([image], [1], None, [64], [0, 256])
                hist_r = cv2.calcHist([image], [2], None, [64], [0, 256])
                embedding = np.concatenate([hist_b.flatten(), hist_g.flatten(), hist_r.flatten()])
            else:
                # Grayscale histogram
                hist = cv2.calcHist([image], [0], None, [256], [0, 256])
                embedding = hist.flatten()
            
            # Normalize histogram
            embedding = embedding.astype(np.float32)
            if np.sum(embedding) > 0:
                embedding = embedding / np.sum(embedding)
                
        elif method == 'lbp':
            # Local Binary Pattern-based embedding
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY) if len(image.shape) == 3 else image
            
            # Compute LBP
            radius = 3
            n_points = 8 * radius
            lbp = local_binary_pattern(gray, n_points, radius, method='uniform')
            
            # Compute LBP histogram
            n_bins = n_points + 2
            hist, _ = np.histogram(lbp.ravel(), bins=n_bins, range=(0, n_bins), density=True)
            embedding = hist.astype(np.float32)
            
        else:
            raise ValueError(f"Unknown embedding method: {method}")
        
        # Ensure embedding is not empty and has valid values
        if embedding is None or len(embedding) == 0:
            return None
        
        # Remove NaN and infinite values
        embedding = np.nan_to_num(embedding, nan=0.0, posinf=0.0, neginf=0.0)
        
        return embedding
        
    except Exception as e:
        logging.error(f"Failed to compute embedding: {e}")
        return None

def local_binary_pattern(image: np.ndarray, n_points: int, radius: float, method: str = 'uniform') -> np.ndarray:
    """
    Simple implementation of Local Binary Pattern
    
    Args:
        image: Input grayscale image
        n_points: Number of sample points
        radius: Radius of sample circle
        method: LBP method ('uniform' or 'default')
        
    Returns:
        LBP image
    """
    # This is a simplified LBP implementation
    # For production use, consider using skimage.feature.local_binary_pattern
    
    rows, cols = image.shape
    lbp = np.zeros_like(image)
    
    for i in range(radius, rows - radius):
        for j in range(radius, cols - radius):
            center_pixel = image[i, j]
            binary_string = ''
            
            # Sample points around the center pixel
            for k in range(n_points):
                angle = 2 * np.pi * k / n_points
                x = int(i + radius * np.cos(angle))
                y = int(j + radius * np.sin(angle))
                
                # Ensure coordinates are within image bounds
                x = max(0, min(rows - 1, x))
                y = max(0, min(cols - 1, y))
                
                if image[x, y] >= center_pixel:
                    binary_string += '1'
                else:
                    binary_string += '0'
            
            # Convert binary string to decimal
            lbp[i, j] = int(binary_string, 2)
    
    return lbp

def cosine_similarity(emb1: np.ndarray, emb2: np.ndarray) -> float:
    """
    Compute cosine similarity between two embeddings with enhanced error handling
    
    Args:
        emb1: First embedding vector
        emb2: Second embedding vector
        
    Returns:
        Cosine similarity score (0-1 range)
        
    Raises:
        ValueError: If embeddings are invalid
    """
    # Validate inputs
    if emb1 is None or emb2 is None:
        raise ValueError("Embeddings cannot be None")
    if not isinstance(emb1, np.ndarray) or not isinstance(emb2, np.ndarray):
        raise ValueError("Embeddings must be numpy arrays")
    if emb1.size == 0 or emb2.size == 0:
        raise ValueError("Embeddings cannot be empty")
    if emb1.shape != emb2.shape:
        raise ValueError(f"Embedding shapes mismatch: {emb1.shape} vs {emb2.shape}")

    try:
        # Compute dot product and norms
        dot_product = np.dot(emb1, emb2)
        norm_product = np.linalg.norm(emb1) * np.linalg.norm(emb2)
        
        # Handle zero norm case
        if norm_product == 0:
            raise ValueError("Zero norm detected, invalid embeddings")
        
        # Calculate and return cosine similarity
        similarity = dot_product / norm_product
        return max(min(similarity, 1.0), -1.0)  # Clamp to [-1, 1] range
        
    except Exception as e:
        raise ValueError(f"Similarity calculation failed: {str(e)}")