package com.example.pinpoint

data class PemerintahHistoryItem(
    val id: String,
    val finishDate: String,
    val status: String,          // "ARSIP_DITUTUP" or "SELESAI"
    val title: String,
    val location: String,
    val sectionHeader: String,   // e.g. "Komparasi Hasil Pengerjaan (BAP-0840)"
    val sectionSubHeader: String,// e.g. "Ketuk untuk perbesar" or "4 Jam Pengerjaan"
    val beforeImageUrl: String,
    val beforeLabel: String,     // "SEBELUM", "KERUSAKAN", "PENGERUKAN"
    val afterImageUrl: String,
    val afterLabel: String,      // "SESUDAH", "TERPASANG", "ALIRAN BERSIH"
    val workNotes: String,
    val teamName: String
)
