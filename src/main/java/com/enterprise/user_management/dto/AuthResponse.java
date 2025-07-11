package com.enterprise.user_management.dto;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private UserResponseDTO user;

    public AuthResponse(String token, UserResponseDTO user) {
        this.token = token;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UserResponseDTO getUser() { return user; }
    public void setUser(UserResponseDTO user) { this.user = user; }
}