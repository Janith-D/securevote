package com.securevote.securevotebackend.Repo;

import com.securevote.securevotebackend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByWalletAddress(String walletAddress);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByWalletAddress(String walletAddress);
    List<User> findByRole(User.UserRole role);
    List<User> findByIsVerified(boolean isVerified);
    List<User> findByIsFaceRegistered(boolean isFaceRegistered);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isVerified = true")
    List<User> findVerifiedUsersByRole(@Param("role") User.UserRole role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countUsersRegisteredSince(@Param("startDate") LocalDateTime startDate);

}
