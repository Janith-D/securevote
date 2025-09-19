package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDto {
    private boolean isValid;
    private List<String> errors;
    private List<String> warnings;
    private String validationId;
    private LocalDateTime validatedAt;
}
