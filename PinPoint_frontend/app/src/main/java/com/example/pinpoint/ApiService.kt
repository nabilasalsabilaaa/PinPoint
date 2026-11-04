package com.example.pinpoint

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ══════════════════════════════════════════════════════════════════════════════
//  DATA TRANSFER OBJECTS (DTO)
// ══════════════════════════════════════════════════════════════════════════════

/** Request body untuk endpoint POST /api/auth/login */
data class LoginRequest(
    val identifier: String,   // email atau nomor telepon
    val password: String
)

/** Response body dari endpoint POST /api/auth/login */
data class LoginResponse(
    val token: String?,
    val type: String? = "Bearer",
    val email: String?,
    val nama: String?,
    val role: String?          // WARGA / ADMIN / DINAS
)

/** Request body untuk endpoint POST /api/auth/register */
data class RegisterRequest(
    val nama: String,
    val email: String,
    val noTelepon: String?,    // sesuai field di backend (nullable/opsional)
    val password: String
)

/** Request body untuk endpoint POST /api/auth/check-account (kirim OTP) */
data class CheckAccountRequest(
    val identifier: String,
    val type: String           // "EMAIL" atau "PHONE"
)

/** Request body untuk endpoint POST /api/auth/verify-otp */
data class OtpVerifyRequest(
    val email: String,
    val otpCode: String
)

/** Request body untuk endpoint POST /api/auth/forgot-password (reset password) */
data class ForgotPasswordRequest(
    val email: String,
    val newPassword: String
)

/** Respons generik dari semua endpoint auth: { success, message, data? } */
data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val data: Any? = null
)

// ══════════════════════════════════════════════════════════════════════════════
//  RETROFIT API SERVICE INTERFACE
// ══════════════════════════════════════════════════════════════════════════════

interface ApiService {

    // ── Autentikasi ─────────────────────────────────────────────────────────

    /** Login dengan email/no.hp + password → dapat JWT token */
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    /** Registrasi akun WARGA baru */
    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse>

    /** Langkah 1 Reset Password: kirim OTP ke email */
    @POST("api/auth/check-account")
    fun checkAccount(@Body request: CheckAccountRequest): Call<ApiResponse>

    /** Langkah 2 Reset Password: verifikasi kode OTP */
    @POST("api/auth/verify-otp")
    fun verifyOtp(@Body request: OtpVerifyRequest): Call<ApiResponse>

    /** Langkah 3 Reset Password: set password baru */
    @POST("api/auth/forgot-password")
    fun resetPassword(@Body request: ForgotPasswordRequest): Call<ApiResponse>

    /** Validasi JWT token yang masih aktif */
    @POST("api/auth/refresh-token")
    fun refreshToken(@Header("Authorization") bearerToken: String): Call<ApiResponse>

    /** Logout (server-side stateless, hanya informasional) */
    @POST("api/auth/logout")
    fun logout(@Header("Authorization") bearerToken: String): Call<ApiResponse>
}

// ══════════════════════════════════════════════════════════════════════════════
//  RETROFIT API CLIENT (Singleton)
// ══════════════════════════════════════════════════════════════════════════════

object ApiClient {

    /**
     * Base URL backend Spring Boot.
     * - Emulator Android: gunakan 10.0.2.2:8081
     * - Perangkat fisik via USB/Wi-Fi: gunakan IP laptop (mis. 192.168.1.5:8081)
     * - ADB Reverse: gunakan localhost:8081 setelah `adb reverse tcp:8081 tcp:8081`
     */
    private const val BASE_URL = "http://10.0.2.2:8081/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY   // ubah ke NONE di build rilis
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ApiService::class.java)
    }
}
