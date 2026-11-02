package com.kelompok8.controller;

import com.kelompok8.model.Laporan;
import com.kelompok8.service.AdminReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Autowired
    private AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<List<Laporan>> getAllReportsForAdmin() {
        // Mengelola dan memverifikasi data laporan (Melihat semua laporan masuk)
        List<Laporan> reports = adminReportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveReport(@PathVariable Long id, Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            Laporan laporan = adminReportService.approveReport(id, adminEmail);
            return ResponseEntity.ok(laporan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectReport(
            @PathVariable Long id, 
            @RequestBody Map<String, String> requestBody, 
            Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            String catatan = requestBody.getOrDefault("catatan", "Tidak memenuhi syarat.");
            
            Laporan laporan = adminReportService.rejectReport(id, adminEmail, catatan);
            return ResponseEntity.ok(laporan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}