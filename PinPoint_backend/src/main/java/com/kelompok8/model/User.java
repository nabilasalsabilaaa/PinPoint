package com.kelompok8.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private Long idRole;

    @Column(nullable = false, length = 100)
    private String nama;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @JsonProperty("no_telepon")
    @Column(length = 20)
    private String noTelepon;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String statusAkun = "ACTIVE";

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime tanggalDaftar = LocalDateTime.now();

    private LocalDateTime terakhirLogin;

    // Helper untuk integrasi AuthService & Filter lama
    public String getRole() { 
        return idRole != null && idRole == 1L ? "ADMIN" : "USER"; 
    }
    public boolean isActive() { 
        return "ACTIVE".equalsIgnoreCase(statusAkun); 
    }
    public void setActive(boolean active) {
        this.statusAkun = active ? "ACTIVE" : "BLOCKED";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdRole() { return idRole; }
    public void setIdRole(Long idRole) { this.idRole = idRole; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNoTelepon() { return noTelepon; }
    public void setNoTelepon(String noTelepon) { this.noTelepon = noTelepon; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStatusAkun() { return statusAkun; }
    public void setStatusAkun(String statusAkun) { this.statusAkun = statusAkun; }
    public LocalDateTime getTanggalDaftar() { return tanggalDaftar; }
    public void setTanggalDaftar(LocalDateTime tanggalDaftar) { this.tanggalDaftar = tanggalDaftar; }
    public LocalDateTime getTerakhirLogin() { return terakhirLogin; }
    public void setTerakhirLogin(LocalDateTime terakhirLogin) { this.terakhirLogin = terakhirLogin; }
}