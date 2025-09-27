package com.securevote.securevotebackend.Controller;

import com.securevote.securevotebackend.Dto.RegisterRequestDto;
import com.securevote.securevotebackend.Dto.UserResponseDto;
import com.securevote.securevotebackend.Dto.VoteRequestDto;
import com.securevote.securevotebackend.Dto.VoteResponseDto;
import com.securevote.securevotebackend.Entity.User;
import com.securevote.securevotebackend.Repo.UserRepo;
import com.securevote.securevotebackend.Service.CandidateService;
import com.securevote.securevotebackend.Service.FaceRecognitionService;
import com.securevote.securevotebackend.Service.VoterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/voting")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200",allowedHeaders = "*",methods = {RequestMethod.POST,RequestMethod.GET,RequestMethod.OPTIONS})
public class VotingController {
    private final VoterService voterService;

    private final FaceRecognitionService faceRecognitionService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private UserRepo userRepo;

    /**
     * Registers a new voter with face enrollment.
     * @param registerRequestDto DTO containing user details
     * @param image MultipartFile for face image
     * @return ResponseEntity with success status and user details
     * @throws Exception if registration or face enrollment fails
     */
    @PostMapping("/register")
    public ResponseEntity<java.util.Map<String, Object>> registerVoter(
            @ModelAttribute RegisterRequestDto registerRequestDto,
            @RequestParam("image") MultipartFile image) throws Exception {
        log.info("Registering voter with wallet address: {}", registerRequestDto.getWalletAddress());
        File imageFile = convertMultipartToFile(image, registerRequestDto.getWalletAddress());
        try {
            UserResponseDto user = voterService.registerVoter(registerRequestDto, imageFile);
            java.util.Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("user", user);
            return ResponseEntity.ok(response);
        } finally {
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    /**
     * Casts a vote after face verification.
     * @return ResponseEntity with vote status and transaction hash
     * @throws Exception if verification or transaction fails
     */
    @PostMapping("/vote")
    public ResponseEntity<Map<String, Object>> castVote(
            @RequestHeader("X-Wallet-Address") String walletAddress,
            @RequestParam("candidateId") Long candidateId,
            @RequestParam("electionId") Long electionId,
            @RequestParam("image") MultipartFile image)
            throws Exception {
        VoteRequestDto voteRequestDto = new VoteRequestDto();
        voteRequestDto.setWalletAddress(walletAddress);
        voteRequestDto.setCandidateId(candidateId);
        voteRequestDto.setElectionId(electionId);
        //voteRequestDto.setImage(image); // Assuming VoteRequestDto has a setImage method
        VoteResponseDto voteResponse = voterService.castVote(voteRequestDto,image);
        log.info("Vote response : voted ={}, message={}",voteResponse.isVoted(),voteResponse.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("status", voteResponse.isVoted()? "success":"failed");
        response.put("voted", voteResponse.isVoted());
        response.put("transactionHash", voteResponse.getTransactionHash());
        response.put("message", voteResponse.getMessage());
        return ResponseEntity.ok(response);
    }
    /**
     * Verifies a voter for login with face recognition.
     * @param walletAddress The user's wallet address
     * @param image MultipartFile for face image
     * @return ResponseEntity with success status and user details or error message
     * @throws Exception if verification fails
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyVoter(
            @RequestParam("walletAddress") String walletAddress,
            @RequestParam("image") MultipartFile image) throws IOException {
        log.info("Received verify request for walletAddress: {}, image size: {}", walletAddress, image.getSize());
        File imageFile = convertMultipartToFile(image);
        try {
            Map<String, Object> result = faceRecognitionService.verifyVoter(walletAddress, imageFile);
            if (result != null) {
                Optional<User> user = userRepo.findByWalletAddress(walletAddress);
                if(user.isPresent()){
                    result.put("status","success");
                    result.put("role",user.get().getRole().name());
                    result.put("walletAddress",walletAddress);
                    return ResponseEntity.ok(result);
                }else {
                    return ResponseEntity.ok(Map.of("status","failed","message","user not found"));
                }

            } else {
                return ResponseEntity.ok(Map.of("status", "failed", "message", "Verification failed"));
            }
        } catch (IOException e) {
            log.error("I/O error during verification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Failed to process image: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Server error during verification: " + e.getMessage()));
        } finally {
            if (imageFile.exists()) {
                log.info("Deleting temp file: {}", imageFile.getAbsolutePath());
                imageFile.delete();
            }
        }
    }
    private File convertMultipartToFile(MultipartFile file) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir")); // Use system temp directory
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File convFile = new File(tempDir, file.getOriginalFilename()); // Use temp dir with original filename
        log.info("Saving temp file to: {}", convFile.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
    /**
     * Retrieves user details by wallet address.
     * @param walletAddress The user's wallet address
     * @return ResponseEntity with user details or error message
     */
    @GetMapping("/user/{walletAddress}")
    public ResponseEntity<java.util.Map<String, Object>> getUser(@PathVariable String walletAddress) {
        UserResponseDto user = voterService.getUser(walletAddress);
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("status", "success");
            response.put("user", user);
        } else {
            response.put("status", "error");
            response.put("message", "User not found");
        }
        return ResponseEntity.ok(response);
    }
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserResponseDto>> getAllUsers(){
        List<UserResponseDto> users = voterService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    @GetMapping("/{walletAddress}")
    public ResponseEntity<Map<String,Object>> getVotes(@PathVariable String walletAddress) {
        List<VoteResponseDto> votes = voterService.getVotesByWalletAddress(walletAddress);
        Map<String,Object> response = new HashMap<>();
        if (!votes.isEmpty()){
            response.put("status","success");
            response.put("votes",votes);
        }else {
            response.put("status","error");
            response.put("message","No votes found for this wallet");
        }
        return ResponseEntity.ok(response);
    }
    @GetMapping("/results/{electionId}")
    public ResponseEntity<Map<String, Object>> getElectionResults(@PathVariable Long electionId) {
        Map<Long, Long> results = candidateService.getElectionResults(electionId);
        Map<String, Object> response = new HashMap<>();
        if (!results.isEmpty()) {
            response.put("status", "success");
            response.put("results", results);
        } else {
            response.put("status", "error");
            response.put("message", "No results available for this election");
        }
        return ResponseEntity.ok(response);
    }
    /**
     * Converts a MultipartFile to a temporary File for processing.
     * @param file The uploaded MultipartFile
     * @return Temporary File
     * @throws IOException if file conversion fails
     */
    private File convertMultipartToFile(MultipartFile file, String walletAddress) throws IOException {
        String filename = walletAddress + "_" + file.getOriginalFilename(); // Unique filename
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + filename);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
