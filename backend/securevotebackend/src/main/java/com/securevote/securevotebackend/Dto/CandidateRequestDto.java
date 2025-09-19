package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRequestDto {
    private String name;
    private String bio;
    private String imageUrl;
    private Long userId;
}
