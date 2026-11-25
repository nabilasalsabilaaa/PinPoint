package com.example.pinpoint.model

data class PemerintahReport(
    val reportId: String,
    val id: String,
    val date: String,
    val category: String,
    val title: String,
    val location: String,
    val imageUrl: String,
    val status: String, // "TERVERIFIKASI", "DIPROSES", "SELESAI"
    val footerTag: String,
    val actionText: String = "Detail & Tindak Lanjut",
    val isPrimaryButton: Boolean = true
)
