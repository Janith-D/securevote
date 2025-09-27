// com.securevote.securevotebackend.Dto.ElectionResponseDto.java
package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.Election;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectionResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String contractAddress;
    private Election.ElectionStatus status;
    private LocalDateTime createdAt;
    private UserResponseDto createdBy;
    private List<CandidateResponseDto> candidates;
    private Long totalVotes;

    public ElectionResponseDto(Election election) {
        this.id = election.getId();
        this.title = election.getTitle();
        this.description = election.getDescription();
        this.startTime = election.getStartTime();
        this.endTime = election.getEndTime();
        this.contractAddress = election.getContractAddress();
        this.status = election.getStatus();
        this.createdAt = election.getCreatedAt();

        // Map createdBy to DTO using only id and walletAddress
        if (election.getCreatedBy() != null) {
            this.createdBy = new UserResponseDto(election.getCreatedBy());
        }

        // Map candidates to DTOs (assuming CandidateResponseDto is defined)
        if (election.getCandidates() != null) {
            this.candidates = election.getCandidates().stream()
                    .map(CandidateResponseDto::new)
                    .collect(Collectors.toList());
        } else {
            this.candidates = Collections.emptyList();
        }

        // Calculate total votes
        if (election.getVotes() != null) {
            this.totalVotes = (long) election.getVotes().size(); // Use size() for performance
        } else {
            this.totalVotes = 0L;
        }
    }
}