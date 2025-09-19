package com.securevote.securevotebackend.Repo;

import com.securevote.securevotebackend.Entity.Candidate;
import com.securevote.securevotebackend.Entity.Election;
import com.securevote.securevotebackend.Entity.User;
import com.securevote.securevotebackend.Entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoteRepo extends JpaRepository<Vote,Long> {
    List<Vote> findByVoter(User voter);
    List<Vote> findByCandidate(Candidate candidate);
    List<Vote> findByElection(Election election);
    List<Vote> findByVoterIdOrderByVotedAtDesc(Long voterId);

    List<Vote> findByElectionIdOrderByVotedAtDesc(Long electionId);

    Optional<Vote> findByElectionIdAndVoterId(Long electionId, Long voterId);

    boolean existsByElectionIdAndVoterId(Long electionId, Long voterId);

    long countByElectionId(Long electionId);

    long countByCandidateId(Long candidateId);

    long countByElectionIdAndStatus(Long electionId, Vote.VoteStatus status);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.election.id = :electionId AND v.votedAt >= :startTime AND v.votedAt <= :endTime")
    Long countVotesByElectionAndTimeRange(
            @Param("electionId") Long electionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT v FROM Vote v WHERE v.transactionHash = :txHash")
    Optional<Vote> findByTransactionHash(@Param("txHash") String transactionHash);

    @Query("SELECT v FROM Vote v WHERE v.blockHash = :blockHash")
    List<Vote> findByBlockHash(@Param("blockHash") String blockHash);

    @Query("SELECT v FROM Vote v WHERE v.status = :status ORDER BY v.votedAt DESC")
    List<Vote> findByStatusOrderByVotedAtDesc(@Param("status") Vote.VoteStatus status);

    @Query("SELECT v.candidate.id, COUNT(v) FROM Vote v WHERE v.election.id = :electionId GROUP BY v.candidate.id")
    List<Object[]> findVoteCountsByCandidateForElection(@Param("electionId") Long electionId);

    @Query("SELECT DATE(v.votedAt), COUNT(v) FROM Vote v WHERE v.election.id = :electionId GROUP BY DATE(v.votedAt) ORDER BY DATE(v.votedAt)")
    List<Object[]> findDailyVoteCounts(@Param("electionId") Long electionId);

    @Query("SELECT HOUR(v.votedAt), COUNT(v) FROM Vote v WHERE v.election.id = :electionId AND DATE(v.votedAt) = DATE(:date) GROUP BY HOUR(v.votedAt) ORDER BY HOUR(v.votedAt)")
    List<Object[]> findHourlyVoteCounts(@Param("electionId") Long electionId, @Param("date") LocalDateTime date);
}
