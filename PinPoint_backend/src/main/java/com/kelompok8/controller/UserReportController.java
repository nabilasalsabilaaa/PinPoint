package com.kelompok8.controller;

import com.kelompok8.model.BuktiPerbaikan;
import com.kelompok8.model.Laporan;
import com.kelompok8.model.Notifikasi;
import com.kelompok8.model.RiwayatStatus;
import com.kelompok8.service.UserReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserReportController {

    @Autowired
    private UserReportService userReportService;

    @PostMapping("/reports")
    public ResponseEntity<?> createReport(Authentication authentication, @RequestBody Laporan laporan) {
        try {
            String email = authentication.getName(); // Mengambil email dari token JWT
            Laporan savedReport = userReportService.createReport(email, laporan);
            return ResponseEntity.ok(savedReport);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getMyReports(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<Laporan> reports = userReportService.getReportsByUser(email);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/reports/map")
    public ResponseEntity<List<Laporan>> getReportsForMap() {
        // Endpoint untuk melihat koordinat seluruh laporan di peta
        return ResponseEntity.ok(userReportService.getAllReportsForMap());
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReportDetail(@PathVariable Long id) {
        return userReportService.getReportDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reports/{id}/status-history")
    public ResponseEntity<List<RiwayatStatus>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(userReportService.getStatusHistory(id));
    }

    @GetMapping("/reports/{id}/evidence")
    public ResponseEntity<?> getEvidence(@PathVariable Long id) {
        Optional<BuktiPerbaikan> evidence = userReportService.getEvidence(id);
        if (evidence.isPresent()) {
            return ResponseEntity.ok(evidence.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<Notifikasi> notifications = userReportService.getUserNotifications(email);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(@PathVariable Long id) {
        try {
            userReportService.markNotificationAsRead(id);
            return ResponseEntity.ok("Notifikasi ditandai sudah dibaca.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}