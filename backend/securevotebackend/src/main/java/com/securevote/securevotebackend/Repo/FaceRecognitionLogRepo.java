package com.securevote.securevotebackend.Repo;

import com.securevote.securevotebackend.Entity.FaceRecognitionLog;
import com.securevote.securevotebackend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FaceRecognitionLogRepo extends JpaRepository<FaceRecognitionLog,Long> {
    List<FaceRecognitionLog> findByUser(User user);

    List<FaceRecognitionLog> findByUserOrderByTimestampDesc(User user);

    List<FaceRecognitionLog> findBySessionId(String sessionId);

    List<FaceRecognitionLog> findByIsSuccessful(Boolean isSuccessful);

    @Query("SELECT f FROM FaceRecognitionLog f WHERE f.user.id = :userId ORDER BY f.timestamp DESC")
    List<FaceRecognitionLog> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);

    @Query("SELECT f FROM FaceRecognitionLog f WHERE f.timestamp >= :startTime AND f.timestamp <= :endTime")
    List<FaceRecognitionLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(f) FROM FaceRecognitionLog f WHERE f.isSuccessful = true AND f.timestamp >= :startTime")
    Long countSuccessfulAttemptsSince(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(f) FROM FaceRecognitionLog f WHERE f.isSuccessful = false AND f.timestamp >= :startTime")
    Long countFailedAttemptsSince(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT AVG(f.confidenceScore) FROM FaceRecognitionLog f WHERE f.isSuccessful = true AND f.timestamp >= :startTime")
    Double findAverageConfidenceScoreSince(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT f.failureReason, COUNT(f) FROM FaceRecognitionLog f WHERE f.isSuccessful = false AND f.timestamp >= :startTime GROUP BY f.failureReason ORDER BY COUNT(f) DESC")
    List<Object[]> findCommonFailureReasonsSince(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT f FROM FaceRecognitionLog f WHERE f.user.id = :userId AND f.isSuccessful = false ORDER BY f.timestamp DESC")
    List<FaceRecognitionLog> findFailedAttemptsByUser(@Param("userId") Long userId);

    @Query("SELECT f FROM FaceRecognitionLog f WHERE f.ipAddress = :ipAddress ORDER BY f.timestamp DESC")
    List<FaceRecognitionLog> findByIpAddressOrderByTimestampDesc(@Param("ipAddress") String ipAddress);
}
