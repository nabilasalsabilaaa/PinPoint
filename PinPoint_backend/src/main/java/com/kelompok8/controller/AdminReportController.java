package com.kelompok8.controller;

import com.kelompok8.model.Laporan;
import com.kelompok8.service.AdminReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminReportController {

    @Autowired
    private AdminReportService adminReportService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Integer>> getDashboardStats() {
        List<Laporan> reports = adminReportService.getAllReports();
        int total = reports.size();
        int pending = (int) reports.stream().filter(r -> r.getIdStatus() != null && r.getIdStatus() == 1L).count();
        int processing = (int) reports.stream().filter(r -> r.getIdStatus() != null && (r.getIdStatus() == 2L || r.getIdStatus() == 4L)).count();
        int rejected = (int) reports.stream().filter(r -> r.getIdStatus() != null && r.getIdStatus() == 3L).count();
        int resolved = (int) reports.stream().filter(r -> r.getIdStatus() != null && r.getIdStatus() == 5L).count();

        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalReports", total);
        stats.put("pendingReports", pending);
        stats.put("processingReports", processing);
        stats.put("resolvedReports", resolved);
        stats.put("rejectedReports", rejected);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Laporan>> getAllReportsForAdmin() {
        // Mengelola dan memverifikasi data laporan (Melihat semua laporan masuk)
        List<Laporan> reports = adminReportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReportById(@PathVariable Long id) {
        return adminReportService.getAllReports().stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = {"/reports/{id}/approve", "/reports/{id}/verify"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> approveReport(@PathVariable Long id, Authentication authentication) {
        try {
            String adminEmail = authentication != null ? authentication.getName() : null;
            Laporan laporan = adminReportService.approveReport(id, adminEmail);
            return ResponseEntity.ok(laporan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/reports/{id}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> rejectReport(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> requestBody, 
            Authentication authentication) {
        try {
            String adminEmail = authentication != null ? authentication.getName() : null;
            Object reasonsObj = requestBody.get("reasonCategory");
            String reasonsStr = "";
            if (reasonsObj instanceof List<?> list && !list.isEmpty()) {
                reasonsStr = "Alasan: " + list.stream().map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("") + ". ";
            } else if (reasonsObj != null) {
                reasonsStr = "Alasan: " + reasonsObj + ". ";
            }
            Object catatanObj = requestBody.getOrDefault("rejectionNote", requestBody.get("catatan"));
            String catatan = reasonsStr + (catatanObj != null ? "Catatan: " + catatanObj.toString() : "Tidak memenuhi syarat.");
            
            Laporan laporan = adminReportService.rejectReport(id, adminEmail, catatan);
            return ResponseEntity.ok(laporan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}