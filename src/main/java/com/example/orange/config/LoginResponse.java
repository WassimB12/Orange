package com.example.orange.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class LoginResponse {

    private String token;

    private long expiresIn;

    public String getToken() {
        return token;
    }

    // Getters and setters...
}