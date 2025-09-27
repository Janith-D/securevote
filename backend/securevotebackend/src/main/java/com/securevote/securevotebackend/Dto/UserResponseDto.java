package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String walletAddress;
    private User.UserRole role;
    private boolean isVerified;
    private boolean isFaceRegistered;
    private LocalDateTime createdAt;

    public UserResponseDto(User user){
        this.id = user.getId();
        this.username = user.getUsername();
        this.walletAddress = user.getWalletAddress();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.role = user.getRole();
        this.isVerified = user.getIsVerified();
        this.isFaceRegistered = user.getIsFaceRegistered();
        this.createdAt = user.getCreatedAt();

    }
}
