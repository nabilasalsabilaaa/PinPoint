package com.kelompok8.dto;

public class LoginRequest {
    private String identifier; // Bisa berisi email atau nomor telepon
    private String password;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}