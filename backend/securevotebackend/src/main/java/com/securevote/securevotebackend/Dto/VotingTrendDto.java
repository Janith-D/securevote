package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotingTrendDto {
    private LocalDateTime timestamp;
    private Long cumulativeVotes;
    private Long votesInHour;
}
