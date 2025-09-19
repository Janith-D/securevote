package com.securevote.securevotebackend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String walletAddress;
    @Lob
    private byte[] faceEncoding;
    private String faceImagePath;
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.VOTER;
    private Boolean isVerified;
    private Boolean isFaceRegistered;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updateAt;
    @OneToMany(mappedBy = "voter",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JsonManagedReference
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<Vote> votes;
    public enum UserRole{
        ADMIN,VOTER,CANDIDATE
    }
    public UserRole getRole(){
        return role;
    }
}

