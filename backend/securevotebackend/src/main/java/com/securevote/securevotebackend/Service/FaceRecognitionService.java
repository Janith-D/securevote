package com.securevote.securevotebackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FaceRecognitionService {

    @Value("${ai.api.url}")
    private String aiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Enrolls a user's face with the AI service and returns the face encoding.
     * @param userId The unique identifier for the user (e.g., walletAddress)
     * @param imageFile The image file for face enrollment
     * @return byte[] The face encoding from the AI service, or null if failed
     * @throws IOException if file handling fails
     */
    public byte[] enrollVoter(String userId, File imageFile) throws IOException {
        String url = aiApiUrl + "/enroll";
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("user_id", userId); // Matches api.py's /enroll endpoint
        body.add("image", new FileSystemResource(imageFile));
        log.info("Enrolling voter with file: {}", imageFile.getAbsolutePath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String encodingBase64 = (String) response.getBody().get("face_encoding");
            if (encodingBase64 != null) {
                return java.util.Base64.getDecoder().decode(encodingBase64);
            }
        }
        log.error("Face enrollment failed for userId: {}. Response: {}", userId, response.getBody());
        return null;
    }

    /**
     * Verifies a user's identity using their face image.
     * @param walletAddress The unique identifier for the user (e.g., walletAddress)
     * @param imageFile The image file for face verification
     * @return boolean True if verification succeeds, false otherwise
     * @throws IOException if file handling fails
     */
    public Map<String, Object> verifyVoter(String walletAddress, File imageFile) throws IOException {
        if (!imageFile.exists() || !imageFile.canRead()) {
            throw new IOException("Image file is inaccessible: " + imageFile.getAbsolutePath());
        }
        log.info("Sending verification request to {} for walletAddress: {}", aiApiUrl + "/verify", walletAddress);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("walletAddress", walletAddress);
        body.add("image", new FileSystemResource(imageFile));
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiApiUrl + "/verify", requestEntity, Map.class);
            log.info("Received response from AI service: status={}, body={}", response.getStatusCode(), response.getBody());
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Boolean result = (Boolean) response.getBody().get("verification_result");
                if (result != null && result) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("role", "voter"); // Hardcoded for now; fetch from blockchain if needed
                    userData.put("walletAddress", walletAddress);
                    return userData;
                }
            }
            return null;
        } catch (HttpClientErrorException e) {
            log.error("External service error: {} - {} - Request headers: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString(), headers, body);
            throw new IOException("Failed to verify with AI service: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during verification: {}", e.getMessage(), e);
            throw new IOException("Unexpected error with AI service: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the face encoding for a user (for testing or caching purposes).
     * @param userId The unique identifier for the user
     * @return byte[] The face encoding, or null if not found
     */
    public byte[] getFaceEncoding(String userId) {
        // This is a placeholder. In a real implementation, you might query the AI API or database.
        return null; // Update with actual logic if needed
    }
}