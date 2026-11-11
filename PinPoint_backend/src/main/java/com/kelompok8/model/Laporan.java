package com.kelompok8.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "laporan")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Laporan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    private Long idUser;

    @Column(nullable = false)
    private Long idKategori;

    @Column(nullable = false)
    private Long idStatus;

    @Column(length = 150)
    private String judul;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String deskripsi;

    @Column(length = 255)
    private String alamat;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 20)
    private String tingkatKeparahan;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime tanggalLaporan = LocalDateTime.now();

    private LocalDateTime tanggalUpdate;

    @Transient
    private String categoryName;

    @Transient
    private String reporterName;

    @Transient
    private String noTelepon;

    @Transient
    private String fotoProfil;

    @JsonProperty("reportCode")
    public String getReportCode() {
        int year = tanggalLaporan != null ? tanggalLaporan.getYear() : 2026;
        return id != null ? String.format("#LP-MKS-%d-%04d", year, id) : "#LP-MKS-0000";
    }

    @JsonProperty("title")
    public String getTitle() {
        return judul != null && !judul.isBlank() ? judul : "Laporan Kerusakan Fasilitas Umum";
    }

    @JsonProperty("description")
    public String getDescription() {
        return deskripsi;
    }

    @JsonProperty("locationName")
    public String getLocationName() {
        return alamat != null && !alamat.isBlank() ? alamat : "Lokasi Koordinat GPS";
    }

    @JsonProperty("status")
    public String getStatusName() {
        if (idStatus == null) return "MENUNGGU_VERIFIKASI";
        return switch (idStatus.intValue()) {
            case 1 -> "MENUNGGU_VERIFIKASI";
            case 2 -> "TERVERIFIKASI";
            case 3 -> "DITOLAK";
            case 4 -> "DIPROSES";
            case 5 -> "SELESAI";
            default -> "MENUNGGU_VERIFIKASI";
        };
    }

    @JsonProperty("createdAt")
    public String getCreatedAt() {
        if (tanggalLaporan == null) return "-";
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
            return tanggalLaporan.format(fmt);
        } catch (Exception e) {
            return tanggalLaporan.toLocalDate().toString();
        }
    }

    public String getCategoryName() { return categoryName != null ? categoryName : "Fasilitas Umum"; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getReporterName() { return reporterName != null ? reporterName : "Warga Pelapor"; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    @JsonProperty("noTelepon")
    public String getNoTelepon() { return noTelepon != null ? noTelepon : "-"; }
    public void setNoTelepon(String noTelepon) { this.noTelepon = noTelepon; }

    @JsonProperty("reporterPhone")
    public String getReporterPhone() { return getNoTelepon(); }

    @JsonProperty("fotoProfil")
    public String getFotoProfil() { return fotoProfil; }
    public void setFotoProfil(String fotoProfil) { this.fotoProfil = fotoProfil; }

    @JsonProperty("photoUrl")
    public String getPhotoUrl() { return fotoProfil; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdUser() { return idUser; }
    public void setIdUser(Long idUser) { this.idUser = idUser; }
    public Long getIdKategori() { return idKategori; }
    public void setIdKategori(Long idKategori) { this.idKategori = idKategori; }
    public Long getIdStatus() { return idStatus; }
    public void setIdStatus(Long idStatus) { this.idStatus = idStatus; }
    public String getJudul() { return judul; }
    public void setJudul(String judul) { this.judul = judul; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getTingkatKeparahan() { return tingkatKeparahan; }
    public void setTingkatKeparahan(String tingkatKeparahan) { this.tingkatKeparahan = tingkatKeparahan; }
    public LocalDateTime getTanggalLaporan() { return tanggalLaporan; }
    public void setTanggalLaporan(LocalDateTime tanggalLaporan) { this.tanggalLaporan = tanggalLaporan; }
    public LocalDateTime getTanggalUpdate() { return tanggalUpdate; }
    public void setTanggalUpdate(LocalDateTime tanggalUpdate) { this.tanggalUpdate = tanggalUpdate; }
}