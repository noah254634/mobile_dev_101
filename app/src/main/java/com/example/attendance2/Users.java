package com.example.attendance2;

public class Users {
    public String userId;
    public String username;
    public String email;
    public String role;
    public long lastLogin;
    public boolean isFlagged;

    public Users() {
        // Default constructor for Firebase
    }

    public Users(String userId, String username, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.lastLogin = System.currentTimeMillis();
        this.isFlagged = false;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
