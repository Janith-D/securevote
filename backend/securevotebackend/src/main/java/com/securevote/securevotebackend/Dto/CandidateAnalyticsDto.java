package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAnalyticsDto {
    private Long candidateId;
    private String name;
    private Long voteCount;
    private Double percentage;
    private List<VotingTrendDto> votingTrend;
}
