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

    public static UserResponseDto fromUser(User user){
        if(user == null){
            return null;
        }
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setWalletAddress(user.getWalletAddress());
        dto.setVerified(user.getIsVerified());
        dto.setFaceRegistered(user.getIsFaceRegistered());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
