package com.securevote.securevotebackend.Service;

import com.securevote.securevotebackend.Dto.*;
import com.securevote.securevotebackend.Entity.Candidate;
import com.securevote.securevotebackend.Entity.Election;
import com.securevote.securevotebackend.Entity.User;
import com.securevote.securevotebackend.Entity.Vote;
import com.securevote.securevotebackend.Repo.CandidateRepo;
import com.securevote.securevotebackend.Repo.UserRepo;
import com.securevote.securevotebackend.Repo.VoteRepo;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VoterService {
    private final UserRepo userRepo;
    private final VoteRepo voteRepo;
    private final CandidateRepo candidateRepo;
    private final FaceRecognitionService aiService;
    private final BlockchainService blockchainService;

    @Autowired
    private CandidateService candidateService;
    private static final Logger log = LoggerFactory.getLogger(VoterService.class);

    public VoterService(UserRepo userRepo,VoteRepo voteRepo, CandidateRepo candidateRepo,FaceRecognitionService aiService,BlockchainService blockchainService,CandidateService candidateService){
        this.userRepo = userRepo;
        this.voteRepo = voteRepo;
        this.candidateRepo = candidateRepo;
        this.aiService = aiService;
        this.blockchainService = blockchainService;
        this.candidateService = candidateService;
    }
    public UserResponseDto registerVoter(RegisterRequestDto registerRequestDto, File imageFile) throws Exception {
        log.info("Registering voter with wallet: {}", registerRequestDto.getWalletAddress());
        Optional<User> existingUser = userRepo.findByWalletAddress(registerRequestDto.getWalletAddress());
        if (existingUser.isPresent()) {
            log.warn("User already registered with wallet: {}", registerRequestDto.getWalletAddress());
            throw new IllegalStateException("User already registered with this wallet address");
        }
        User user = new User();
        user.setUsername(registerRequestDto.getUsername());
        user.setEmail(registerRequestDto.getEmail());
        user.setWalletAddress(registerRequestDto.getWalletAddress());
        user.setPassword("");
        user.setFirstName(registerRequestDto.getFirstName());
        user.setLastName(registerRequestDto.getLastName());
        user.setRole(User.UserRole.VOTER);
        user.setIsVerified(false);
        user.setIsFaceRegistered(false);
        user.setCreatedAt(LocalDateTime.now());

        byte[] faceEncoding = aiService.enrollVoter(registerRequestDto.getWalletAddress(), imageFile);
        log.info("Enrollment result for wallet {}: encoding={}", registerRequestDto.getWalletAddress(), faceEncoding != null);
        if (faceEncoding != null) {
            user.setFaceEncoding(faceEncoding);
            // Avoid using imageFile.getAbsolutePath() for faceImagePath; consider storing a relative path or skipping
            user.setIsFaceRegistered(true);
            user.setIsVerified(true);
        } else {
            log.warn("Face enrollment failed for wallet: {}", registerRequestDto.getWalletAddress());
        }
        User savedUser = userRepo.save(user);
        log.info("User saved: wallet={},role= {}, isFaceRegistered={}", savedUser.getWalletAddress(), savedUser.getRole(), savedUser.getIsFaceRegistered());
        return mapToUserResponseDto(savedUser);
    }
    @Transactional
    public VoteResponseDto castVote(VoteRequestDto voteRequestDto, MultipartFile image) throws Exception {
        log.info("Casting vote for wallet: {}, candidateId: {}", voteRequestDto.getWalletAddress(), voteRequestDto.getCandidateId());
        Optional<User> userOpt = userRepo.findByWalletAddress(voteRequestDto.getWalletAddress());
        if (userOpt.isEmpty() || !userOpt.get().getIsFaceRegistered()) {
            log.warn("User check failed for wallet: {}", voteRequestDto.getWalletAddress());
            return new VoteResponseDto(false, null, "User not registered or face not enrolled");
        }
        User user = userOpt.get();
        // Check role (VOTER or CANDIDATE) only
        if (user.getRole() != User.UserRole.VOTER && user.getRole() != User.UserRole.CANDIDATE) {
            log.warn("Unauthorized role for wallet: {}", voteRequestDto.getWalletAddress());
            return new VoteResponseDto(false, null, "User not authorized");
        }
        Long electionId = 1L; // Hardcoded; consider making dynamic
        if (voteRepo.existsByElectionIdAndVoterId(electionId, user.getId())) {
            log.warn("User already voted for electionId: {}", electionId);
            return new VoteResponseDto(false, null, "User already voted");
        }
        if (image == null || image.isEmpty()) {
            log.warn("Invalid image file");
            return new VoteResponseDto(false, null, "Image file is required");
        }
        File imageFile = convertMultipartToFile(image);
        try {
            Map<String, Object> verificationResult = aiService.verifyVoter(voteRequestDto.getWalletAddress(), imageFile);
            boolean isVerified = verificationResult != null; // Success if Map is returned
            log.info("Verification result for wallet {}: {}", voteRequestDto.getWalletAddress(), isVerified);
            if (isVerified) {
                CompletableFuture<TransactionReceipt> receiptFuture = blockchainService.castVote(voteRequestDto.getCandidateId(), voteRequestDto.getWalletAddress());
                TransactionReceipt receipt;
                try {
                    receipt = receiptFuture.get(30, TimeUnit.SECONDS);
                    log.info("Transaction receipt: status={}, hash={}", receipt.isStatusOK(), receipt.getTransactionHash());
                } catch (Exception e) {
                    log.error("Transaction failed to complete", e);
                    return new VoteResponseDto(false, null, "Transaction failed: " + e.getMessage());
                }
                if (receipt != null && receipt.isStatusOK()) {
                    Optional<Candidate> candidateOpt = candidateService.findById(voteRequestDto.getCandidateId());
                    if (candidateOpt.isEmpty()) {
                        log.warn("Candidate not found for id: {}", voteRequestDto.getCandidateId());
                        return new VoteResponseDto(false, receipt.getTransactionHash(), "Candidate not found");
                    }
                    Candidate candidate = candidateOpt.get();
                    Vote vote = new Vote();
                    vote.setVoter(user);
                    vote.setCandidate(candidate);
                    vote.setElection(candidate.getElection());
                    vote.setVotedAt(LocalDateTime.now());
                    vote.setStatus(Vote.VoteStatus.SUCCESS);
                    vote.setTransactionHash(receipt.getTransactionHash());
                    vote.setBlockHash(receipt.getBlockHash());
                    voteRepo.save(vote);
                    log.info("Vote saved successfully, transaction hash: {}", receipt.getTransactionHash());
                    return new VoteResponseDto(true, receipt.getTransactionHash(), "Success");
                }
            }
            log.warn("Verification or transaction failed for wallet: {}", voteRequestDto.getWalletAddress());
            return new VoteResponseDto(false, null, "Verification or transaction failed");
        } finally {
            if (imageFile != null) imageFile.delete();
        }
    }
    public UserResponseDto verifyVoter(String walletAddress, File imageFile) throws Exception {
        log.info("Verifying voter for login with wallet: {}", walletAddress);
        Optional<User> userOpt = userRepo.findByWalletAddress(walletAddress);
        if (userOpt.isEmpty() || !userOpt.get().getIsFaceRegistered()) {
            log.warn("User not found or face not enrolled for wallet: {}", walletAddress);
            return null;
        }

        User user = userOpt.get();
        Map<String, Object> verificationResult = aiService.verifyVoter(walletAddress, imageFile);
        boolean isVerified = verificationResult != null;
        log.info("Verification result for login with wallet {}: {}", walletAddress, isVerified);

        if (isVerified) {
            user.setIsVerified(true);
            userRepo.save(user);
            return mapToUserResponseDto(user);
        } else {
            log.warn("Face verification failed for login with wallet: {}", walletAddress);
            return null;
        }
    }
    public List<VoteResponseDto> getVotesByWalletAddress(String walletAddress) {
        log.info("Retrieving votes for wallet: {}", walletAddress);
        Optional<User> userOpt = userRepo.findByWalletAddress(walletAddress);
        if (userOpt.isEmpty()) {
            log.warn("User not found for wallet: {}", walletAddress);
            return Collections.emptyList();
        }
        User user = userOpt.get();
        List<Vote> votes = voteRepo.findByVoter(user); // Assuming VoteRepo has this method
        return votes.stream().map(vote -> new VoteResponseDto(
                true,
                vote.getTransactionHash(),
                "Vote retrieved"
        )).collect(Collectors.toList());
    }


    public UserResponseDto getUser(String walletAddress) {
        return userRepo.findByWalletAddress(walletAddress)
                .map(this::mapToUserResponseDto)
                .orElse(null);
    }
        private UserResponseDto mapToUserResponseDto(User user) {
            UserResponseDto dto = new UserResponseDto();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setWalletAddress(user.getWalletAddress());
            dto.setRole(user.getRole() != null? user.getRole() : null);
            dto.setVerified(user.getIsVerified());
            dto.setFaceRegistered(user.getIsFaceRegistered());
            dto.setCreatedAt(user.getCreatedAt());
            return dto;
        }
    private File convertMultipartToFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("MultipartFile is null or empty");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "temp_" + System.currentTimeMillis() + ".jpg"; // Default name
        }
        File convFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
