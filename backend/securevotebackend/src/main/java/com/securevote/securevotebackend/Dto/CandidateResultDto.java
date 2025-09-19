package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResultDto {
    private Long candidateId;
    private String name;
    private Long voteCount;
    private Double percentage;
    private Integer rank;
}
