package com.kelompok8.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "kategori")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kategori {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonAlias({"name", "nama_kategori"})
    @Column(nullable = false, length = 100)
    private String namaKategori;

    @JsonAlias({"description"})
    @Column(length = 255)
    private String deskripsi;

    @JsonAlias({"iconName"})
    @Column(length = 255)
    private String ikon;

    @JsonAlias({"isActive"})
    @Column(nullable = false)
    private Boolean aktif = true;

    @Transient
    private String code;

    @Transient
    private Integer connectedReportsCount = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNamaKategori() { return namaKategori; }
    public void setNamaKategori(String namaKategori) { this.namaKategori = namaKategori; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public String getIkon() { return ikon; }
    public void setIkon(String ikon) { this.ikon = ikon; }
    public Boolean getAktif() { return aktif != null ? aktif : true; }
    public void setAktif(Boolean aktif) { this.aktif = aktif != null ? aktif : true; }

    public String getCode() {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return id != null ? String.format("KTG-MKS-%02d", id) : null;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getConnectedReportsCount() {
        return connectedReportsCount != null ? connectedReportsCount : 0;
    }

    public void setConnectedReportsCount(Integer connectedReportsCount) {
        this.connectedReportsCount = connectedReportsCount;
    }
}