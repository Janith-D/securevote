package com.securevote.securevotebackend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_recognition_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceRecognitionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String sessionId;
    private Double confidenceScore;
    private Boolean isSuccessful;
    private String failureReason;
    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
