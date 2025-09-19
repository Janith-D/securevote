package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequestDto {
    private Long electionId;
    private String format;
    private boolean includeVoterDetails;
    private boolean includeBlockchainData;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
