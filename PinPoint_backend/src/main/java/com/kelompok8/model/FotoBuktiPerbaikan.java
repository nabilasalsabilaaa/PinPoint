package com.kelompok8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "foto_bukti_perbaikan")
public class FotoBuktiPerbaikan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idBukti;

    @Column(length = 255)
    private String namaFile;

    @Column(length = 500)
    private String pathFile;

    private Integer ukuranFile;

    @Column(length = 50)
    private String tipeFile;

    private LocalDateTime tanggalUpload = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdBukti() { return idBukti; }
    public void setIdBukti(Long idBukti) { this.idBukti = idBukti; }
    public String getNamaFile() { return namaFile; }
    public void setNamaFile(String namaFile) { this.namaFile = namaFile; }
    public String getPathFile() { return pathFile; }
    public void setPathFile(String pathFile) { this.pathFile = pathFile; }
    public Integer getUkuranFile() { return ukuranFile; }
    public void setUkuranFile(Integer ukuranFile) { this.ukuranFile = ukuranFile; }
    public String getTipeFile() { return tipeFile; }
    public void setTipeFile(String tipeFile) { this.tipeFile = tipeFile; }
    public LocalDateTime getTanggalUpload() { return tanggalUpload; }
    public void setTanggalUpload(LocalDateTime tanggalUpload) { this.tanggalUpload = tanggalUpload; }
}