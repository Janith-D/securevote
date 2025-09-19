package com.securevote.securevotebackend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id",nullable = false)
    @JsonBackReference
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id",nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id",nullable = false)
    private Candidate candidate;

    @Column(unique = true,nullable = false)
    private String transactionHash;

    @Column(nullable = false)
    private String blockHash;

    private Long blockNumber;

    @CreationTimestamp
    private LocalDateTime votedAt;
    @UpdateTimestamp  // Auto-sets on update
    private LocalDateTime updateAt;

    @Enumerated(EnumType.STRING)
    private VoteStatus status = VoteStatus.PENDING;

    public enum VoteStatus{
        PENDING,CONFIRMED, SUCCESS, FAILED
    }
}
