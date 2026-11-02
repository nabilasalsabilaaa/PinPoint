package com.kelompok8.model;

import jakarta.persistence.*;

@Entity
@Table(name = "kategori")
public class Kategori {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String namaKategori;

    @Column(length = 255)
    private String deskripsi;

    @Column(length = 255)
    private String ikon;

    @Column(nullable = false)
    private Boolean aktif = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNamaKategori() { return namaKategori; }
    public void setNamaKategori(String namaKategori) { this.namaKategori = namaKategori; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public String getIkon() { return ikon; }
    public void setIkon(String ikon) { this.ikon = ikon; }
    public Boolean getAktif() { return aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }
}