package com.kelompok8.dto;

/**
 * DTO standar untuk response API yang seragam.
 * Digunakan pada semua endpoint {@code /api/auth/**}.
 */
public class ApiResponse {

    private boolean success;
    private String  message;
    private Object  data;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data    = data;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public boolean isSuccess()                  { return success; }
    public void    setSuccess(boolean success)  { this.success = success; }

    public String  getMessage()                 { return message; }
    public void    setMessage(String message)   { this.message = message; }

    public Object  getData()                    { return data; }
    public void    setData(Object data)         { this.data = data; }
}
