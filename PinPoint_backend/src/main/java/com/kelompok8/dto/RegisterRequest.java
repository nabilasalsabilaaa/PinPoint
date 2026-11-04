package com.kelompok8.dto;

/**
 * DTO untuk permintaan registrasi pengguna baru.
 * Memisahkan input registrasi dari entitas {@code User}
 * agar validasi lebih terkontrol.
 */
public class RegisterRequest {

    /** Nama lengkap pengguna */
    private String nama;

    /** Email unik pengguna (digunakan untuk login & notifikasi) */
    private String email;

    /** Nomor telepon (opsional, dapat digunakan untuk login alternatif) */
    private String noTelepon;

    /** Password plaintext — akan di-hash BCrypt di service */
    private String password;

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getNama()                       { return nama; }
    public void   setNama(String nama)            { this.nama = nama; }

    public String getEmail()                      { return email; }
    public void   setEmail(String email)          { this.email = email; }

    public String getNoTelepon()                  { return noTelepon; }
    public void   setNoTelepon(String noTelepon)  { this.noTelepon = noTelepon; }

    public String getPassword()                   { return password; }
    public void   setPassword(String password)    { this.password = password; }
}
