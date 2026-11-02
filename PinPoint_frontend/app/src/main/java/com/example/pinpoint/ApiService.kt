package com.example.pinpoint

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// 1. DTO Request Login
data class LoginRequest(
    val identifier: String,
    val password: String
)

// 2. DTO Response Login
data class LoginResponse(
    val token: String?,
    val type: String? = "Bearer",
    val email: String?,
    val nama: String?,
    val role: String?
)

// 3. DTO Request Register (Menyesuaikan dengan struktur User/request di backend Anda)
data class RegisterRequest(
    val nama: String,
    val email: String,
    val no_telepon: String, // Sesuaikan jika di backend namanya 'noPonsel' atau 'phone'
    val password: String
)

data class CheckAccountRequest(
    val identifier: String,
    val type: String // "EMAIL" atau "PHONE"
)

interface ApiService {
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // Endpoint Register yang mengarah ke AuthController Spring Boot Anda
    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<ResponseBody>

    @POST("api/auth/check-account")
    fun checkAccount(@Body request: CheckAccountRequest): Call<ResponseBody>
}

object ApiClient {
    // Gunakan localhost (karena sudah menggunakan ADB Reverse) atau IP Wi-Fi laptop Anda
    private const val BASE_URL = "http://localhost:8081/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}