package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.Candidate;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CandidateDto {
    private Long id;
    private String name;
    private String bio;
    private String imageUrl;
    private Long voteCount;
    private boolean isActive;
    private LocalDateTime createdAt;

    public CandidateDto(Long id, String name, String bio, String imageUrl, Long voteCount, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.voteCount = voteCount != null ? voteCount : 0L;
        this.isActive = true;
        this.createdAt = createdAt;
    }
}
