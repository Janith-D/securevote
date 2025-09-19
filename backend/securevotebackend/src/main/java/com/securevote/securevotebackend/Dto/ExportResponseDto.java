package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponseDto {
    private String fileName;
    private String downloadUrl;
    private Long fileSize;
    private String contentType;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
}
