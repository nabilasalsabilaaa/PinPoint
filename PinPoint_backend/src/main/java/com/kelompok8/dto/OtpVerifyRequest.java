package com.kelompok8.dto;

/**
 * DTO untuk verifikasi kode OTP yang dikirim via email.
 * Digunakan pada endpoint {@code POST /api/auth/verify-otp}.
 */
public class OtpVerifyRequest {

    /** Email tujuan pengiriman OTP */
    private String email;

    /** Kode OTP 6-digit yang dimasukkan pengguna */
    private String otpCode;

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getEmail()               { return email; }
    public void   setEmail(String email)   { this.email = email; }

    public String getOtpCode()                 { return otpCode; }
    public void   setOtpCode(String otpCode)   { this.otpCode = otpCode; }
}
