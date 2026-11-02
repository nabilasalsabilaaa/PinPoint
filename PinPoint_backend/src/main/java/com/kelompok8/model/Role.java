package com.kelompok8.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String namaRole;

    @Column(length = 255)
    private String deskripsi;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNamaRole() { return namaRole; }
    public void setNamaRole(String namaRole) { this.namaRole = namaRole; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
}