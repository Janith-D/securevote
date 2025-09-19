package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.Vote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteStatusDto {
    private Long electionId;
    private String electionTitle;
    private boolean hasVoted;
    private String votedFor; // Candidate name if voted
    private LocalDateTime votedAt;
    private String transactionHash;
    private Vote.VoteStatus status;
}
