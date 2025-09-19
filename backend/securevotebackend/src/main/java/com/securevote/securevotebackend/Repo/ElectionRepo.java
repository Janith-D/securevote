package com.securevote.securevotebackend.Repo;

import com.securevote.securevotebackend.Entity.Election;
import com.securevote.securevotebackend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ElectionRepo extends JpaRepository<Election,Long> {
    List<Election> findByStatus(Election.ElectionStatus status);

    List<Election> findByCreatedBy(User createdBy);

    List<Election> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    @Query("SELECT e FROM Election e WHERE e.startTime <= :now AND e.endTime >= :now AND e.status = 'ACTIVE'")
    List<Election> findActiveElections(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Election e WHERE e.endTime < :now AND e.status = 'ACTIVE'")
    List<Election> findExpiredElections(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Election e WHERE e.startTime > :now ORDER BY e.startTime ASC")
    List<Election> findUpcomingElections(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Election e WHERE e.title LIKE %:keyword% OR e.description LIKE %:keyword%")
    List<Election> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT COUNT(e) FROM Election e WHERE e.createdAt >= :startDate")
    Long countElectionsCreatedSince(@Param("startDate") LocalDateTime startDate);

    Optional<Election> findByContractAddress(String contractAddress);
}
