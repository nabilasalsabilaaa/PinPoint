package com.example.pinpoint

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
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

// 3. DTO Request Register
data class RegisterRequest(
    val nama: String,
    val email: String,
    val no_telepon: String,
    val password: String
)

// 3b. DTO Request Check Account
data class CheckAccountRequest(
    val identifier: String,
    val type: String
)

// 4. DTO Laporan
data class LaporanResponse(
    val id: Long? = null,
    val idUser: Long? = null,
    val idKategori: Long? = null,
    val idStatus: Long? = null,
    val judul: String? = null,
    val deskripsi: String? = null,
    val alamat: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tingkatKeparahan: String? = null,
    val tanggalLaporan: String? = null,
    val tanggalUpdate: String? = null
)

data class CreateLaporanRequest(
    val idKategori: Long,
    val judul: String,
    val deskripsi: String,
    val alamat: String,
    val latitude: Double,
    val longitude: Double,
    val tingkatKeparahan: String
)

// 5. DTO Kategori
data class KategoriResponse(
    val id: Long? = null,
    val namaKategori: String? = null,
    val deskripsi: String? = null,
    val ikon: String? = null,
    val aktif: Boolean? = null
)

// 6. DTO Notifikasi
data class NotifikasiResponse(
    val id: Long? = null,
    val idUser: Long? = null,
    val idLaporan: Long? = null,
    val judul: String? = null,
    val pesan: String? = null,
    val tipe: String? = null,
    val sudahDibaca: Boolean? = null,
    val tanggalDibuat: String? = null
)

interface ApiService {
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<ResponseBody>

    @POST("api/auth/check-account")
    fun checkAccount(@Body request: CheckAccountRequest): Call<ResponseBody>

    // Endpoint Kategori & Laporan
    @GET("api/categories")
    fun getCategories(): Call<List<KategoriResponse>>

    @GET("api/user/reports")
    fun getMyReports(@Header("Authorization") token: String): Call<List<LaporanResponse>>

    @POST("api/user/reports")
    fun createReport(
        @Header("Authorization") token: String,
        @Body request: CreateLaporanRequest
    ): Call<LaporanResponse>

    @GET("api/user/reports/map")
    fun getReportsForMap(): Call<List<LaporanResponse>>

    @GET("api/user/reports/{id}")
    fun getReportDetail(@Path("id") id: Long): Call<LaporanResponse>

    @GET("api/user/notifications")
    fun getNotifications(@Header("Authorization") token: String): Call<List<NotifikasiResponse>>
}

object ApiClient {
    // Gunakan localhost (karena sudah menggunakan ADB Reverse) atau IP Wi-Fi laptop Anda
    private const val BASE_URL = "http://192.168.1.57:8081/"

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
