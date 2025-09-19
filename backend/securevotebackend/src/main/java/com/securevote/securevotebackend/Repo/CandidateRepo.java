package com.securevote.securevotebackend.Repo;

import com.securevote.securevotebackend.Dto.CandidateDto;
import com.securevote.securevotebackend.Entity.Candidate;
import com.securevote.securevotebackend.Entity.Election;
import com.securevote.securevotebackend.Entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepo extends JpaRepository<Candidate,Long> {
    List<Candidate> findByElection(Election election);
    List<Candidate> findByElectionId(Long electionId);
    List<Candidate> findByUser(User user);
    List<Candidate> findByElectionOrderByCreatedAtAsc(Election election);

    @Query("SELECT new com.securevote.securevotebackend.Dto.CandidateDto(" +
            "c.id, c.name, c.bio, c.imageUrl, COUNT(v), c.createdAt) " +
            "FROM Candidate c LEFT JOIN c.votes v " +
            "GROUP BY c.id")
    List<CandidateDto> findAllWithVoteCount();

    @Query("SELECT c FROM Candidate c WHERE c.election.id = :electionId ORDER BY c.name ASC")
    List<Candidate> findByElectionIdOrderByName(@Param("electionId") Long electionId);

    @Query("SELECT c FROM Candidate c JOIN c.votes v GROUP BY c.id ORDER BY COUNT(v) DESC")
    List<Candidate> findCandidatesOrderByVoteCount();

    @Query("SELECT c FROM Candidate c WHERE c.election.id = :electionId AND c.name LIKE %:name%")
    List<Candidate> findByElectionIdAndNameContaining(@Param("electionId") Long electionId, @Param("name") String name);

    boolean existsByElectionIdAndUserId(Long electionId, Long userId);
}
