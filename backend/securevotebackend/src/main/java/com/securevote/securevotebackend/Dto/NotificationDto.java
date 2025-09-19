package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private String type; // INFO, WARNING, ERROR, SUCCESS
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
