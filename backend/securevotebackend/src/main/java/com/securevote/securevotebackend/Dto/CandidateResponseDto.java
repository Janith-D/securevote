package com.securevote.securevotebackend.Dto;


import com.securevote.securevotebackend.Entity.Candidate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponseDto {
    private Long id;
    private String name;
    private String bio;
    private String imageUrl;
    private Long userId;
    private Long voteCount;
    private LocalDateTime createdAt;

    public static CandidateResponseDto fromCandidate(Candidate candidate) {
        return new CandidateResponseDto(
                candidate.getId(),
                candidate.getName(),
                candidate.getBio(),
                candidate.getImageUrl(),
                candidate.getUser() != null ? candidate.getUser().getId() : null,
                candidate.getVotes() != null ? (long) candidate.getVotes().size() : 0L,
                candidate.getCreatedAt()
        );
    }
}
