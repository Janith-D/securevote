package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectionAnalyticsDto {
    private Long electionId;
    private String title;
    private Long totalRegisteredVoters;
    private Long totalVotesCast;
    private Double turnoutPercentage;
    private List<VotingTrendDto> votingTrends;
    private List<CandidateAnalyticsDto> candidateAnalytics;
    private LocalDateTime lastUpdated;
}
