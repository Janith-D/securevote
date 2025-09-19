package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.Election;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectionResultDto {
    private Long electionId;
    private String title;
    private Election.ElectionStatus status;
    private Long totalVotes;
    private List<CandidateResultDto> candidates;
    private LocalDateTime lastUpdated;
    private boolean isFinalized;
}
