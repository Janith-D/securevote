package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String userId;
    private String username;
    private String ipAddress;
    private String details;
    private LocalDateTime timestamp;
}
