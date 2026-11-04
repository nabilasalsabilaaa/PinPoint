package com.example.pinpoint

import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * AuthRepository — Lapisan antara Activity/ViewModel dan Retrofit API.
 *
 * Menyediakan callback yang bersih agar Activity tidak perlu tahu
 * detail implementasi Retrofit. Semua panggilan API Auth ada di sini.
 *
 * Pola callback: `onSuccess(data)` dan `onError(message)`.
 */
class AuthRepository(private val context: Context) {

    private val TAG = "AuthRepository"
    private val api = ApiClient.instance

    // ══════════════════════════════════════════════════════════════════════
    //  LOGIN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Melakukan login ke backend dan menyimpan sesi ke TokenManager.
     *
     * @param identifier    Email atau nomor telepon
     * @param password      Password plaintext
     * @param rememberMe    Jika true, simpan kredensial untuk auto-fill
     * @param onSuccess     Dipanggil dengan data [LoginResponse] jika berhasil
     * @param onError       Dipanggil dengan pesan error jika gagal
     */
    fun login(
        identifier: String,
        password: String,
        rememberMe: Boolean,
        onSuccess: (LoginResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        api.login(LoginRequest(identifier, password))
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.token != null) {
                            // Simpan sesi ke EncryptedSharedPreferences
                            TokenManager.saveSession(
                                context    = context,
                                token      = body.token,
                                nama       = body.nama ?: "",
                                email      = body.email ?: "",
                                role       = body.role ?: "WARGA"
                            )
                            // Simpan / hapus kredensial "Ingat Saya"
                            if (rememberMe) {
                                TokenManager.saveRememberMe(context, identifier, password)
                            } else {
                                TokenManager.clearRememberMe(context)
                            }
                            onSuccess(body)
                        } else {
                            onError("Respons server tidak valid.")
                        }
                    } else {
                        val msg = response.errorBody()?.string()
                            ?: "Login gagal (${response.code()})"
                        Log.w(TAG, "Login error: $msg")
                        onError(parseErrorMessage(msg))
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e(TAG, "Login onFailure: ${t.message}")
                    onError("Gagal terhubung ke server. Periksa koneksi internet Anda.")
                }
            })
    }

    // ══════════════════════════════════════════════════════════════════════
    //  REGISTER
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Mendaftarkan pengguna baru sebagai WARGA.
     *
     * @param nama          Nama lengkap
     * @param email         Email (unik)
     * @param noTelepon     Nomor telepon opsional
     * @param password      Password plaintext
     * @param onSuccess     Dipanggil dengan pesan sukses
     * @param onError       Dipanggil dengan pesan error
     */
    fun register(
        nama: String,
        email: String,
        noTelepon: String?,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        api.register(RegisterRequest(nama, email, noTelepon, password))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onSuccess(response.body()?.message ?: "Registrasi berhasil!")
                    } else {
                        val msg = response.body()?.message
                            ?: response.errorBody()?.string()
                            ?: "Registrasi gagal (${response.code()})"
                        Log.w(TAG, "Register error: $msg")
                        onError(parseErrorMessage(msg))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Register onFailure: ${t.message}")
                    onError("Gagal terhubung ke server. Periksa koneksi internet Anda.")
                }
            })
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 1: Kirim OTP
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Mengirim kode OTP ke email/no.hp terdaftar.
     *
     * @param identifier    Email atau nomor telepon
     * @param type          "EMAIL" atau "PHONE"
     * @param onSuccess     Dipanggil dengan pesan sukses dari server
     * @param onError       Dipanggil dengan pesan error
     */
    fun sendOtp(
        identifier: String,
        type: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        api.checkAccount(CheckAccountRequest(identifier, type))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onSuccess(response.body()?.message ?: "OTP terkirim!")
                    } else {
                        val msg = response.body()?.message
                            ?: response.errorBody()?.string()
                            ?: "Gagal mengirim OTP (${response.code()})"
                        onError(parseErrorMessage(msg))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    onError("Gagal terhubung ke server.")
                }
            })
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 2: Verifikasi OTP
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Memverifikasi kode OTP yang dimasukkan pengguna.
     *
     * @param email     Email tujuan OTP
     * @param otpCode   Kode 6-digit dari pengguna
     * @param onSuccess Dipanggil jika OTP valid
     * @param onError   Dipanggil dengan pesan error jika tidak valid
     */
    fun verifyOtp(
        email: String,
        otpCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        api.verifyOtp(OtpVerifyRequest(email, otpCode))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onSuccess()
                    } else {
                        val msg = response.body()?.message
                            ?: response.errorBody()?.string()
                            ?: "OTP tidak valid atau kedaluwarsa."
                        onError(parseErrorMessage(msg))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    onError("Gagal terhubung ke server.")
                }
            })
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 3: Reset Password
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Mereset password pengguna ke nilai baru.
     *
     * @param email       Email akun yang direset
     * @param newPassword Password baru (minimal 6 karakter)
     * @param onSuccess   Dipanggil dengan pesan sukses
     * @param onError     Dipanggil dengan pesan error
     */
    fun resetPassword(
        email: String,
        newPassword: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        api.resetPassword(ForgotPasswordRequest(email, newPassword))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onSuccess(response.body()?.message ?: "Password berhasil diubah!")
                    } else {
                        val msg = response.body()?.message
                            ?: response.errorBody()?.string()
                            ?: "Gagal mereset password."
                        onError(parseErrorMessage(msg))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    onError("Gagal terhubung ke server.")
                }
            })
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPER
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Membersihkan pesan error dari JSON mentah server menjadi teks yang
     * ramah pengguna. Ambil nilai field "message" jika ada.
     */
    private fun parseErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Terjadi kesalahan. Silakan coba lagi."
        // Coba ambil nilai "message":"..." dari JSON
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(raw)
        return match?.groupValues?.get(1) ?: raw
    }
}
