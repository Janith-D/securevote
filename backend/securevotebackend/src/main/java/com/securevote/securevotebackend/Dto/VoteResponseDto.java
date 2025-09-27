package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.Vote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class VoteResponseDto {
    private boolean voted;
    private String transactionHash;
    private String message;
    private Long electionId;

    public VoteResponseDto(boolean voted, String transactionHash, String message, Long electionId) {
        this.voted = voted;
        this.transactionHash = transactionHash;
        this.message = message;
        this.electionId = electionId;
    }
}
