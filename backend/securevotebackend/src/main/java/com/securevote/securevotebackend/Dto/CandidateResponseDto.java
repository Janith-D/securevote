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

 public CandidateResponseDto( Candidate candidate){
     this.id = candidate.getId();
     this.name = candidate.getName();
     this.bio = candidate.getBio();
     this.imageUrl = candidate.getImageUrl();
     this.createdAt = candidate.getCreatedAt();

 }
}
