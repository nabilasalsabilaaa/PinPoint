package com.kelompok8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifikasi")
public class Notifikasi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idUser;

    private Long idLaporan;

    @Column(nullable = false, length = 150)
    private String judul;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String pesan;

    @Column(length = 50)
    private String tipe;

    @Column(nullable = false)
    private Boolean sudahDibaca = false;

    private LocalDateTime tanggalDibuat = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdUser() { return idUser; }
    public void setIdUser(Long idUser) { this.idUser = idUser; }
    public Long getIdLaporan() { return idLaporan; }
    public void setIdLaporan(Long idLaporan) { this.idLaporan = idLaporan; }
    public String getJudul() { return judul; }
    public void setJudul(String judul) { this.judul = judul; }
    public String getPesan() { return pesan; }
    public void setPesan(String pesan) { this.pesan = pesan; }
    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }
    public Boolean getSudahDibaca() { return sudahDibaca; }
    public void setSudahDibaca(Boolean sudahDibaca) { this.sudahDibaca = sudahDibaca; }
    public LocalDateTime getTanggalDibuat() { return tanggalDibuat; }
    public void setTanggalDibuat(LocalDateTime tanggalDibuat) { this.tanggalDibuat = tanggalDibuat; }
}