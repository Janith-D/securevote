package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.Vote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CastVoteRequestDto {
    private Long electionId;
    private Long candidateId;
    private String sessionId;

    private String verificationToken;
}
