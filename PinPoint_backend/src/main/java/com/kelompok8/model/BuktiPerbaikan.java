package com.kelompok8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bukti_perbaikan")
public class BuktiPerbaikan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idLaporan;

    @Column(nullable = false)
    private Long idPemerintah;

    @Column(length = 255)
    private String namaFile;

    @Column(length = 500)
    private String pathFile;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    private LocalDateTime tanggalUpload = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdLaporan() { return idLaporan; }
    public void setIdLaporan(Long idLaporan) { this.idLaporan = idLaporan; }
    public Long getIdPemerintah() { return idPemerintah; }
    public void setIdPemerintah(Long idPemerintah) { this.idPemerintah = idPemerintah; }
    public String getNamaFile() { return namaFile; }
    public void setNamaFile(String namaFile) { this.namaFile = namaFile; }
    public String getPathFile() { return pathFile; }
    public void setPathFile(String pathFile) { this.pathFile = pathFile; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public LocalDateTime getTanggalUpload() { return tanggalUpload; }
    public void setTanggalUpload(LocalDateTime tanggalUpload) { this.tanggalUpload = tanggalUpload; }
}