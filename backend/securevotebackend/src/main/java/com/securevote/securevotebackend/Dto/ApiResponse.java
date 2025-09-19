package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp;

    public ApiResponse(boolean success,String message,T data){
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    public static <T> ApiResponse<T> success(String message, T data){
        return new ApiResponse<>(true,message,data);
    }
    public static <T> ApiResponse<T> success(String message){
        return new ApiResponse<>(true,message,null);
    }
    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> error(String error, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }
}
