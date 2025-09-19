package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceRecognitionStatsDto {
    private Long totalAttempts;
    private Long successfulAttempts;
    private Long failedAttempts;
    private Double successRate;
    private Double averageConfidence;
    private List<String> commonFailureReasons;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
