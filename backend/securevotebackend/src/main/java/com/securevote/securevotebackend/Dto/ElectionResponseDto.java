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

    public static ElectionResponseDto fromElection(Election election) {
        ElectionResponseDto dto = new ElectionResponseDto();
        dto.setId(election.getId());
        dto.setTitle(election.getTitle());
        dto.setDescription(election.getDescription());
        dto.setStartTime(election.getStartTime());
        dto.setEndTime(election.getEndTime());
        dto.setContractAddress(election.getContractAddress());
        dto.setStatus(election.getStatus());
        dto.setCreatedAt(election.getCreatedAt());
        dto.setCreatedBy(election.getCreatedBy() != null ? UserResponseDto.fromUser(election.getCreatedBy()) : null);

        if (election.getCandidates() != null) {
            dto.setCandidates(election.getCandidates().stream()
                    .map(CandidateResponseDto::fromCandidate)
                    .toList());
        }

        // Calculate total votes
        dto.setTotalVotes(election.getVotes() != null ? (long) election.getVotes().size() : 0L);

        return dto;
    }
}
