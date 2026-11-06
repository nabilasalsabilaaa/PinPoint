package com.kelompok8.dto;

/**
 * DTO untuk request penghapusan akun.
 *
 * Hanya memerlukan email yang akan dihapus. Dalam implementasi real,
 * biasanya email diambil dari token JWT, tapi untuk keperluan demo ini
 * diambil dari request body.
 */
public class DeleteAccountRequest {
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
