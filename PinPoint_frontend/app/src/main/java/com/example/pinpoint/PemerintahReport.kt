package com.example.pinpoint

data class PemerintahReport(
    val id: String,
    val date: String,
    val category: String,
    val title: String,
    val location: String,
    val imageUrl: String,
    val status: String, // "TERVERIFIKASI", "SEDANG_DIPROSES", "SELESAI"
    val footerTag: String,
    val actionText: String = "Detail & Tindak Lanjut ➔",
    val isPrimaryButton: Boolean = true
)
