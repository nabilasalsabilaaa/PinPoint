package com.example.pinpoint.network

import com.example.pinpoint.model.ChangePasswordRequest
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- AUTH DTO ---
data class LoginRequest(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("idRole") val idRole: Long? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("type") val type: String? = "Bearer",
    @SerializedName("email") val email: String? = null,
    @SerializedName(value = "nama", alternate = ["fullName"]) val nama: String? = null,
    @SerializedName(value = "role", alternate = ["peran"]) val role: String? = null,
    @SerializedName(value = "noTelepon", alternate = ["no_telepon", "phoneNumber"]) val phoneNumber: String? = null,
    @SerializedName(value = "fotoProfil", alternate = ["photoUrl"]) val photoUrl: String? = null,
    @SerializedName("isSuspended") val isSuspended: Boolean? = false
) {
    val fullName: String? get() = nama
    val noTelepon: String? get() = phoneNumber
}

data class RegisterRequest(
    @SerializedName("nama") val nama: String,
    @SerializedName("email") val email: String,
    @SerializedName("noTelepon") val phoneNumber: String,
    @SerializedName("kataSandi") val password: String,
    @SerializedName("no_telepon") val no_telepon: String = phoneNumber,
    @SerializedName("password") val rawPassword: String = password
)

data class CheckAccountRequest(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("type") val type: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("mode") val mode: String = "FORGOT_PASSWORD"
)

data class ResetPasswordWithOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("newPassword") val newPassword: String
)

// --- ADMIN DTO ---
data class DashboardStatsDto(
    @SerializedName("totalReports") val totalReports: Int = 0,
    @SerializedName(value = "unverifiedReports", alternate = ["pendingReports"]) val unverifiedReports: Int = 0,
    @SerializedName(value = "processedReports", alternate = ["processingReports"]) val processedReports: Int = 0,
    @SerializedName(value = "completedReports", alternate = ["resolvedReports"]) val completedReports: Int = 0,
    @SerializedName("rejectedReports") val rejectedReports: Int = 0
) {
    val pendingReports: Int get() = unverifiedReports
    val processingReports: Int get() = processedReports
    val resolvedReports: Int get() = completedReports
}

data class ReportDto(
    @SerializedName(value = "id", alternate = ["idLaporan", "id_laporan"]) val id: String? = null,
    @SerializedName("reportCode") val reportCode: String? = null,
    @SerializedName(value = "title", alternate = ["judul"]) val title: String? = null,
    @SerializedName(value = "description", alternate = ["deskripsi"]) val description: String? = null,
    @SerializedName(value = "idKategori", alternate = ["id_kategori"]) val idKategori: Long? = null,
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName(value = "idStatus", alternate = ["id_status"]) val idStatus: Long? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName(value = "idUser", alternate = ["id_user"]) val idUser: Long? = null,
    @SerializedName("reporterName") val reporterName: String? = null,
    @SerializedName(value = "noTelepon", alternate = ["reporterPhone", "no_telepon"]) val reporterPhone: String? = null,
    @SerializedName(value = "locationName", alternate = ["alamat"]) val locationName: String? = null,
    @SerializedName("latitude") val latitude: Double? = -5.1476,
    @SerializedName("longitude") val longitude: Double? = 119.4327,
    @SerializedName(value = "photoUrl", alternate = ["fotoBukti", "foto_bukti", "fotoProfil"]) val photoUrl: String? = null,
    @SerializedName(value = "createdAt", alternate = ["tanggalLaporan", "tanggal_laporan"]) val createdAt: String? = null
) {
    fun resolvedId(): Long = id?.toLongOrNull() ?: 0L

    fun resolvedCode(): String {
        if (!reportCode.isNullOrBlank()) return reportCode
        val rid = resolvedId()
        return String.format("#LP-MKS-2026-%04d", rid)
    }

    fun resolvedTitle(): String = title?.takeIf { it.isNotBlank() } ?: "Laporan Fasilitas Umum"

    fun resolvedDescription(): String = description?.takeIf { it.isNotBlank() } ?: "-"

    fun resolvedCategoryName(): String = categoryName?.takeIf { it.isNotBlank() } ?: "Fasilitas Umum"

    fun resolvedReporterName(): String = reporterName?.takeIf { it.isNotBlank() } ?: "Warga Makassar"

    fun resolvedReporterPhone(): String = reporterPhone?.takeIf { it.isNotBlank() } ?: "-"

    fun resolvedLocationName(): String = locationName?.takeIf { it.isNotBlank() } ?: "Lokasi Titik Koordinat GPS"

    fun resolvedPhotoUrl(): String? = photoUrl?.takeIf { it.isNotBlank() }

    fun resolvedCreatedAt(): String = createdAt?.takeIf { it.isNotBlank() } ?: "Baru saja"

    fun resolvedStatusKey(): String {
        if (!status.isNullOrBlank()) {
            val s = status.uppercase()
            if (s.contains("MENUNGGU")) return "MENUNGGU_VERIFIKASI"
            if (s.contains("TERVERIFIKASI") || s.contains("DISETUJUI")) return "TERVERIFIKASI"
            if (s.contains("TOLAK")) return "DITOLAK"
            if (s.contains("PROSES")) return "DIPROSES"
            if (s.contains("SELESAI")) return "SELESAI"
            return s
        }
        return when (idStatus?.toInt()) {
            1 -> "MENUNGGU_VERIFIKASI"
            2 -> "TERVERIFIKASI"
            3 -> "DITOLAK"
            4 -> "DIPROSES"
            5 -> "SELESAI"
            else -> "MENUNGGU_VERIFIKASI"
        }
    }

    fun resolvedStatusDisplay(): String {
        return when (resolvedStatusKey()) {
            "MENUNGGU_VERIFIKASI" -> "Menunggu Verifikasi"
            "TERVERIFIKASI" -> "Terverifikasi"
            "DITOLAK" -> "Ditolak"
            "DIPROSES" -> "Sedang Diproses"
            "SELESAI" -> "Selesai"
            else -> "Menunggu Verifikasi"
        }
    }
}

data class RejectReportRequest(
    @SerializedName("reasonCategory") val reasonCategory: List<String>,
    @SerializedName("rejectionNote") val rejectionNote: String
)

data class CategoryDto(
    @SerializedName(value = "id", alternate = ["idKategori", "id_kategori"]) val id: Long? = null,
    @SerializedName(value = "code", alternate = ["kodeKategori"]) val code: String? = null,
    @SerializedName(value = "namaKategori", alternate = ["name", "nama_kategori"]) val name: String? = null,
    @SerializedName(value = "ikon", alternate = ["iconName", "icon"]) val iconName: String? = null,
    @SerializedName(value = "deskripsi", alternate = ["description"]) val description: String? = null,
    @SerializedName(value = "aktif", alternate = ["isActive", "status"]) val isActive: Boolean? = true,
    @SerializedName("connectedReportsCount") val connectedReportsCount: Int? = 0
) {
    fun resolvedId(): Long? = id
    fun resolvedName(): String = name?.takeIf { it.isNotBlank() } ?: "Tanpa Nama"
    fun resolvedDescription(): String = description?.takeIf { it.isNotBlank() } ?: "-"
    fun resolvedIconName(): String = iconName?.takeIf { it.isNotBlank() } ?: "jalan"
    fun resolvedIsActive(): Boolean = isActive ?: true
    fun resolvedCode(): String {
        if (!code.isNullOrBlank()) return code
        val cid = resolvedId() ?: 0L
        return "KTG-MKS-${cid.toString().padStart(2, '0')}"
    }
}

data class UserAccountDto(
    @SerializedName(value = "id", alternate = ["idUser", "id_user"]) val id: Long? = null,
    @SerializedName("idRole") val idRole: Long? = null,
    @SerializedName(value = "nama", alternate = ["fullName"]) val fullName: String? = "",
    @SerializedName("email") val email: String? = "",
    @SerializedName(value = "noTelepon", alternate = ["no_telepon", "phoneNumber"]) val phoneNumber: String? = "",
    @SerializedName(value = "peran", alternate = ["role"]) val role: String? = "",
    @SerializedName(value = "kataSandi", alternate = ["password"]) val password: String? = null,
    @SerializedName(value = "aktif", alternate = ["active", "isActive"]) val isActive: Boolean? = true,
    @SerializedName("isSuspended") val isSuspended: Boolean? = false,
    @SerializedName("suspendReason") val suspendReason: String? = null,
    @SerializedName(value = "createdAt", alternate = ["tanggalDaftar"]) val createdAt: String? = null,
    @SerializedName("statusAkun") val statusAkun: String? = "ACTIVE",
    @SerializedName(value = "fotoProfil", alternate = ["photoUrl"]) val photoUrl: String? = null,
    @SerializedName("reportCount") val reportCount: Int? = 0
) {
    fun resolvedFullName(): String = fullName?.takeIf { it.isNotBlank() } ?: "Tanpa Nama"
    fun resolvedPhone(): String = phoneNumber?.takeIf { it.isNotBlank() } ?: "-"
    fun resolvedRole(): String {
        if (idRole != null) {
            return when (idRole.toInt()) {
                1 -> "ADMIN"
                2 -> "USER"
                3 -> "PEMERINTAH"
                else -> "USER"
            }
        }
        val r = (role?.takeIf { it.isNotBlank() } ?: "USER").uppercase()
        return when {
            r.contains("ADMIN") -> "ADMIN"
            r.contains("PEMERINTAH") || r.contains("DINAS") -> "PEMERINTAH"
            else -> "USER"
        }
    }
}

interface ApiService {
    // --- AUTH ---
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<ResponseBody>

    @POST("api/auth/register/request-otp")
    fun requestRegisterOtp(@Body request: RegisterRequest): Call<ResponseBody>

    @POST("api/auth/register/verify-otp")
    fun verifyRegisterOtp(@Body request: VerifyOtpRequest): Call<ResponseBody>

    @POST("api/auth/check-account")
    fun checkAccount(@Body request: CheckAccountRequest): Call<ResponseBody>

    @POST("api/auth/verify-otp")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<ResponseBody>

    @POST("api/auth/forgot-password")
    fun resetPasswordWithOtp(@Body request: ResetPasswordWithOtpRequest): Call<ResponseBody>

    // --- DASHBOARD & REPORTS ---
    @GET("api/admin/dashboard/stats")
    fun getDashboardStats(@Header("Authorization") token: String): Call<DashboardStatsDto>

    @GET("api/admin/reports")
    fun getAllReports(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Call<List<ReportDto>>

    @GET("api/admin/reports/{id}")
    fun getReportDetail(@Header("Authorization") token: String, @Path("id") reportId: String): Call<ReportDto>

    @POST("api/admin/reports/{id}/verify")
    fun verifyReport(@Header("Authorization") token: String, @Path("id") reportId: String?): Call<Void>

    @POST("api/admin/reports/{id}/reject")
    fun rejectReport(
        @Header("Authorization") token: String,
        @Path("id") reportId: String,
        @Body request: RejectReportRequest
    ): Call<Void>

    // --- CATEGORY ---
    @GET("api/categories")
    fun getAllCategories(@Header("Authorization") token: String): Call<List<CategoryDto>>

    @POST("api/categories")
    fun createCategory(@Header("Authorization") token: String, @Body category: CategoryDto): Call<CategoryDto>

    @PUT("api/categories/{id}")
    fun updateCategory(@Header("Authorization") token: String, @Path("id") id: Long, @Body category: CategoryDto): Call<CategoryDto>

    @DELETE("api/categories/{id}")
    fun deleteCategory(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>

    // --- USERS ---
    @GET("api/admin/users")
    fun getAllUsers(
        @Header("Authorization") token: String,
        @Query("role") role: String? = null,
        @Query("search") search: String? = null
    ): Call<List<UserAccountDto>>

    @POST("api/admin/users")
    fun createAdminOrGovAccount(@Header("Authorization") token: String, @Body user: UserAccountDto): Call<UserAccountDto>

    @PUT("api/admin/users/{id}/status")
    fun updateUserStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<UserAccountDto>

    @DELETE("api/admin/users/{id}")
    fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Call<Void>

    @PUT("api/admin/profile")
    fun updateProfile(@Header("Authorization") token: String, @Body user: UserAccountDto): Call<UserAccountDto>

    @POST("api/admin/change-password")
    fun changePassword(@Header("Authorization") token: String, @Body request: ChangePasswordRequest): Call<ResponseBody>
}

object ApiClient {
    // Ganti IP di bawah ini jika IP laptop berubah
    private const val BASE_URL = "http://10.113.109.86:8081/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    fun <T> fetchWithFallback(
        callFactory: (ApiService) -> Call<T>,
        onResponse: (Call<T>, Response<T>) -> Unit,
        onFailure: (Call<T>, Throwable) -> Unit
    ) {
        callFactory(instance).enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                onResponse(call, response)
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                onFailure(call, t)
            }
        })
    }
}
