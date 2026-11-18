package com.pinpoint.model

import com.google.gson.annotations.SerializedName

// 1. DTO Statistik Dashboard Admin
data class DashboardStatsDto(
    @SerializedName("totalReports") val totalReports: Int = 0,
    @SerializedName("pendingReports") val pendingReports: Int = 0,
    @SerializedName("resolvedReports") val resolvedReports: Int = 0,
    @SerializedName("rejectedReports") val rejectedReports: Int = 0
)

// 2. DTO Laporan Warga
data class ReportDto(
    @SerializedName("id") val id: String,
    @SerializedName("reportCode") val reportCode: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("categoryName") val categoryName: String,
    @SerializedName("status") val status: String, // MENUNGGU_VERIFIKASI, DIPROSES, SELESAI, DITOLAK
    @SerializedName("reporterName") val reporterName: String,
    @SerializedName("reporterPhone") val reporterPhone: String,
    @SerializedName("locationName") val locationName: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("createdAt") val createdAt: String
)

// 3. DTO Request Tolak Laporan
data class RejectReportRequest(
    @SerializedName("reasonCategory") val reasonCategory: String,
    @SerializedName("rejectionNote") val rejectionNote: String
)

// 4. DTO Master Kategori
data class CategoryDto(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("iconName") val iconName: String,
    @SerializedName("description") val description: String,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("isArchived") val isArchived: Boolean = false,
    @SerializedName("connectedReportsCount") val connectedReportsCount: Int = 0
)

// 5. DTO Pengguna / Akun
data class UserAccountDto(
    @SerializedName("id") val id: String?,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("role") val role: String, // ROLE_WARGA, ROLE_ADMIN, ROLE_DINAS
    @SerializedName("isSuspended") val isSuspended: Boolean = false,
    @SerializedName("suspendReason") val suspendReason: String? = null
)