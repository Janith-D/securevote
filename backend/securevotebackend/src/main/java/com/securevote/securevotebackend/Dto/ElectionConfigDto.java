package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectionConfigDto {
    private Double minConfidenceThreshold;
    private Integer maxVotingAttempts;
    private Integer sessionTimeout;
    private boolean livenessDetectionEnabled;
    private boolean duplicateVoteCheckEnabled;
    private String blockchainNetwork;
}
