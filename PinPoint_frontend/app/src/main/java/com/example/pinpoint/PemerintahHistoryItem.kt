package com.example.pinpoint

data class PemerintahHistoryItem(
    val reportId: String,
    val id: String,
    val categoryId: Long?,
    val categoryName: String,
    val finishDate: String,
    val status: String,
    val title: String,
    val location: String,
    val sectionHeader: String,
    val sectionSubHeader: String,
    val beforeImageUrl: String,
    val beforeLabel: String,
    val afterImageUrl: String,
    val afterLabel: String,
    val allPhotos: List<String> = emptyList(),
    val workNotes: String,
    val teamName: String
)
