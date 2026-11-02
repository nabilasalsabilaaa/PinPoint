package com.kelompok8.dto;

public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private String email;
    private String nama;
    private String role;

    public LoginResponse(String token, String email, String nama, String role) {
        this.token = token;
        this.email = email;
        this.nama = nama;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}