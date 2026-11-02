package com.kelompok8.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "riwayat_status")
public class RiwayatStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idLaporan;

    @Column(nullable = false)
    private Long idStatus;

    @Column(nullable = false)
    private Long idPengubah;

    private LocalDateTime tanggalPerubahan = LocalDateTime.now();

    @Column(length = 255)
    private String keterangan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdLaporan() { return idLaporan; }
    public void setIdLaporan(Long idLaporan) { this.idLaporan = idLaporan; }
    public Long getIdStatus() { return idStatus; }
    public void setIdStatus(Long idStatus) { this.idStatus = idStatus; }
    public Long getIdPengubah() { return idPengubah; }
    public void setIdPengubah(Long idPengubah) { this.idPengubah = idPengubah; }
    public LocalDateTime getTanggalPerubahan() { return tanggalPerubahan; }
    public void setTanggalPerubahan(LocalDateTime tanggalPerubahan) { this.tanggalPerubahan = tanggalPerubahan; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}