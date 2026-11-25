package com.example.pinpoint

import com.example.pinpoint.network.ReportDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportDtoUnitTest {

    @Test
    fun resolvedCode_formatsNumericIdWhenReportCodeBlank() {
        val dto = ReportDto(id = "7", reportCode = null)
        assertEquals("#LP-MKS-2026-0007", dto.resolvedCode())
    }

    @Test
    fun resolvedCode_usesExplicitCodeWhenProvided() {
        val dto = ReportDto(id = "7", reportCode = "#CUSTOM-001")
        assertEquals("#CUSTOM-001", dto.resolvedCode())
    }

    @Test
    fun resolvedStatusKey_mapsIdStatusCorrectly() {
        assertEquals("MENUNGGU_VERIFIKASI", ReportDto(idStatus = 1L).resolvedStatusKey())
        assertEquals("TERVERIFIKASI", ReportDto(idStatus = 2L).resolvedStatusKey())
        assertEquals("DITOLAK", ReportDto(idStatus = 3L).resolvedStatusKey())
        assertEquals("DIPROSES", ReportDto(idStatus = 4L).resolvedStatusKey())
        assertEquals("SELESAI", ReportDto(idStatus = 5L).resolvedStatusKey())
    }

    @Test
    fun resolvedStatusDisplay_returnsIndonesianLabels() {
        assertEquals("Menunggu Verifikasi", ReportDto(idStatus = 1L).resolvedStatusDisplay())
        assertEquals("Terverifikasi", ReportDto(idStatus = 2L).resolvedStatusDisplay())
        assertEquals("Ditolak", ReportDto(idStatus = 3L).resolvedStatusDisplay())
        assertEquals("Sedang Diproses", ReportDto(idStatus = 4L).resolvedStatusDisplay())
        assertEquals("Selesai", ReportDto(idStatus = 5L).resolvedStatusDisplay())
    }

    @Test
    fun resolvedUrgency_mapsSeverityTextCorrectly() {
        assertEquals("Urgensi Tinggi", ReportDto(tingkatKeparahan = "Tinggi").resolvedUrgency())
        assertEquals("Urgensi Sedang", ReportDto(tingkatKeparahan = "Sedang").resolvedUrgency())
        assertEquals("Urgensi Rendah", ReportDto(tingkatKeparahan = "Rendah").resolvedUrgency())
        assertEquals("Prioritas Standar", ReportDto(tingkatKeparahan = null).resolvedUrgency())
    }

    @Test
    fun defaultFallbacks_returnSafeStringsWhenFieldsNull() {
        val empty = ReportDto()
        assertEquals("Laporan Fasilitas Umum", empty.resolvedTitle())
        assertEquals("-", empty.resolvedDescription())
        assertEquals("Fasilitas Umum", empty.resolvedCategoryName())
        assertEquals("Lokasi Titik Koordinat GPS", empty.resolvedLocationName())
    }
}
