package com.securevote.securevotebackend.Dto;

import com.securevote.securevotebackend.Entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String walletAddress;
    private User.UserRole role;
}
