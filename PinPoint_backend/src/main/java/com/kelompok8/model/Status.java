package com.kelompok8.model;

import jakarta.persistence.*;

@Entity
@Table(name = "status")
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String kodeStatus;

    @Column(nullable = false, length = 50)
    private String namaStatus;

    @Column(length = 255)
    private String deskripsi;

    private Integer urutan;

    @Column(nullable = false)
    private Boolean aktif = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKodeStatus() { return kodeStatus; }
    public void setKodeStatus(String kodeStatus) { this.kodeStatus = kodeStatus; }
    public String getNamaStatus() { return namaStatus; }
    public void setNamaStatus(String namaStatus) { this.namaStatus = namaStatus; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public Integer getUrutan() { return urutan; }
    public void setUrutan(Integer urutan) { this.urutan = urutan; }
    public Boolean getAktif() { return aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }
}