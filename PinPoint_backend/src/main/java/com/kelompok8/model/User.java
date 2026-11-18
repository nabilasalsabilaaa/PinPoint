package com.kelompok8.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private Long idRole;

    @JsonAlias({"fullName", "nama"})
    @Column(nullable = false, length = 100)
    private String nama;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @JsonProperty("noTelepon")
    @JsonAlias({"no_telepon", "noTelepon", "phoneNumber"})
    @Column(name = "no_telepon", length = 20)
    private String noTelepon;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias({"kataSandi", "password"})
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String statusAkun = "ACTIVE";

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime tanggalDaftar = LocalDateTime.now();

    private LocalDateTime terakhirLogin;

    @Transient
    private Integer reportCount = 0;

    @Transient
    @JsonProperty("fotoProfil")
    @JsonAlias({"photoUrl", "fotoProfil"})
    private String fotoProfil;

    // Helper untuk integrasi AuthService, Filter & Frontend DTO (1 = ADMIN, 2 = USER, 3 = PEMERINTAH)
    public String getRole() {
        if (idRole != null && idRole == 1L) return "ADMIN";
        if (idRole != null && idRole == 3L) return "PEMERINTAH";
        return "USER";
    }

    @JsonProperty("peran")
    public String getPeran() {
        return getRole();
    }

    @JsonSetter("peran")
    @JsonAlias({"role", "peran"})
    public void setPeran(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) return;
        String upper = roleStr.trim().toUpperCase();
        if (upper.equals("1") || upper.contains("ADMIN")) {
            this.idRole = 1L;
        } else if (upper.equals("3") || upper.contains("PEMERINTAH") || upper.contains("DINAS") || upper.contains("GOV")) {
            this.idRole = 3L;
        } else {
            this.idRole = 2L;
        }
    }

    public boolean isActive() { 
        return "ACTIVE".equalsIgnoreCase(statusAkun); 
    }

    @JsonProperty("aktif")
    public boolean getAktif() {
        return isActive();
    }

    @JsonProperty("isSuspended")
    public boolean getIsSuspended() {
        return "SUSPENDED".equalsIgnoreCase(statusAkun) || "BLOCKED".equalsIgnoreCase(statusAkun);
    }

    @JsonProperty("createdAt")
    public String getCreatedAt() {
        return tanggalDaftar != null ? tanggalDaftar.toLocalDate().toString() : "-";
    }

    public void setActive(boolean active) {
        this.statusAkun = active ? "ACTIVE" : "BLOCKED";
    }

    public Integer getReportCount() {
        return reportCount != null ? reportCount : 0;
    }

    public void setReportCount(Integer reportCount) {
        this.reportCount = reportCount;
    }

    public String getFotoProfil() {
        return fotoProfil;
    }

    public void setFotoProfil(String fotoProfil) {
        this.fotoProfil = fotoProfil;
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