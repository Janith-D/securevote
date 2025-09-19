package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateElectionRequestDto {
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<CandidateRequestDto> candidates;

    public boolean isEndTimeAfterStartTime() {
        return endTime != null && startTime != null && endTime.isAfter(startTime);
    }

}
