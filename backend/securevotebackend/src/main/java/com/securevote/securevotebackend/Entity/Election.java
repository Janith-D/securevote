package com.securevote.securevotebackend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "elections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Election {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @Column(unique = true)
    private String contractAddress;
    private String transactionHash;
    @Enumerated(EnumType.STRING)
    private ElectionStatus status = ElectionStatus.CREATED;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @OneToMany(mappedBy = "election",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Candidate> candidates;
    @OneToMany(mappedBy = "election",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Vote> votes;

    public enum ElectionStatus{
        CREATED,ACTIVE,COMPLETED,CANCELLED
    }
}
