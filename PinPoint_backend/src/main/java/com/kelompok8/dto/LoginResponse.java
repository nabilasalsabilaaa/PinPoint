package com.kelompok8.dto;

public class LoginResponse {
    private Long id;
    private Long idRole;
    private String token;
    private String type = "Bearer";
    private String email;
    private String nama;
    private String role;
    private String peran;
    private String noTelepon;
    private Boolean isSuspended = false;

    public LoginResponse(String token, String email, String nama, String role) {
        this.token = token;
        this.email = email;
        this.nama = nama;
        this.role = role;
        this.peran = role;
    }

    public LoginResponse(Long id, Long idRole, String token, String email, String nama, String role, String noTelepon, Boolean isSuspended) {
        this.id = id;
        this.idRole = idRole;
        this.token = token;
        this.email = email;
        this.nama = nama;
        this.role = role;
        this.peran = role;
        this.noTelepon = noTelepon;
        this.isSuspended = isSuspended;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdRole() { return idRole; }
    public void setIdRole(Long idRole) { this.idRole = idRole; }
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
    public String getPeran() { return peran; }
    public void setPeran(String peran) { this.peran = peran; }
    public String getNoTelepon() { return noTelepon; }
    public void setNoTelepon(String noTelepon) { this.noTelepon = noTelepon; }
    public Boolean getIsSuspended() { return isSuspended; }
    public void setIsSuspended(Boolean isSuspended) { this.isSuspended = isSuspended; }
}
