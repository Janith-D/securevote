package com.securevote.securevotebackend.Service;

import com.securevote.securevotebackend.Dto.CandidateDto;
import com.securevote.securevotebackend.Entity.Candidate;
import com.securevote.securevotebackend.Entity.Election;
import com.securevote.securevotebackend.Repo.CandidateRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CandidateService {
    private static final Logger log = LoggerFactory.getLogger(CandidateService.class);

    @Autowired
    private CandidateRepo candidateRepo;

    @Autowired
    private ElectionService electionService;

    public Candidate saveCandidate(Candidate candidate){
        if(candidate.getElection() == null || candidate.getElection().getId() == null){
            throw new IllegalArgumentException("Election is required for candidate");
        }
        Optional<Election> electionOpt = electionService.findById(candidate.getElection().getId());
        if (electionOpt.isEmpty()){
            throw new IllegalArgumentException("Election not found for id: " + candidate.getElection().getId());
        }
        candidate.setElection(electionOpt.get());
        candidate.setCreatedAt(LocalDateTime.now());
        log.info("Saving candidate: {} ",candidate.getName());
        return candidateRepo.save(candidate);
    }
    public Optional<Candidate> findById(Long id){
        log.info("Fetching candidate by id: {} ",id);
        return candidateRepo.findById(id);
    }
    public Map<Long, Long> getElectionResults(Long electionId) {
        log.info("Retrieving election results for electionId: {}", electionId);
        Optional<Election> electionOpt = electionService.findById(electionId);
        if (electionOpt.isEmpty()) {
            log.warn("Election not found for id: {}", electionId);
            return new HashMap<>(); // Empty map if election not found
        }

        List<Candidate> candidates = candidateRepo.findByElectionId(electionId);
        Map<Long, Long> results = new HashMap<>();
        for (Candidate candidate : candidates) {
            long voteCount = candidate.getVotes() != null ? candidate.getVotes().size() : 0;
            results.put(candidate.getId(), voteCount);
        }
        log.info("Election results for electionId {}: {}", electionId, results);
        return results;
    }

    @Transactional(readOnly = true)
    public List<CandidateDto> getAllCandidates(){
        return candidateRepo.findAllWithVoteCount();
    }
}
