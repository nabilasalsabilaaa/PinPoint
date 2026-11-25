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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserReportController {

    @Autowired
    private UserReportService userReportService;

    @PostMapping("/reports")
    public ResponseEntity<?> createReport(
            Authentication authentication,
            @RequestBody Map<String, Object> payload) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            Laporan laporan = new Laporan();
            if (payload.get("idKategori") != null) {
                laporan.setIdKategori(Long.valueOf(payload.get("idKategori").toString()));
            } else {
                laporan.setIdKategori(1L);
            }
            laporan.setJudul(payload.get("judul") != null ? payload.get("judul").toString() : "Laporan Fasilitas Umum");
            laporan.setDeskripsi(payload.get("deskripsi") != null ? payload.get("deskripsi").toString() : "-");
            laporan.setAlamat(payload.get("alamat") != null ? payload.get("alamat").toString() : "Kota Makassar");
            laporan.setLatitude(payload.get("latitude") != null ? Double.valueOf(payload.get("latitude").toString()) : -5.147665);
            laporan.setLongitude(payload.get("longitude") != null ? Double.valueOf(payload.get("longitude").toString()) : 119.432732);
            laporan.setTingkatKeparahan(payload.get("tingkatKeparahan") != null ? payload.get("tingkatKeparahan").toString() : "Sedang");

            List<String> photoUrls = new ArrayList<>();
            Object rawPhotos = payload.getOrDefault("photoUrls", payload.get("photos"));
            if (rawPhotos instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null && !item.toString().isBlank()) {
                        photoUrls.add(item.toString());
                    }
                }
            }

            Laporan savedReport = userReportService.createReport(email, laporan, photoUrls);
            return ResponseEntity.ok(savedReport);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getMyReports(Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            List<Laporan> reports = userReportService.getReportsByUser(email);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/reports/map")
    public ResponseEntity<List<Laporan>> getReportsForMap() {
        return ResponseEntity.ok(userReportService.getAllReportsForMap());
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReportDetail(@PathVariable Long id) {
        return userReportService.getReportDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reports/{id}/support")
    public ResponseEntity<?> supportReport(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            return ResponseEntity.ok(userReportService.toggleSupportReport(id, email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
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
            String email = authentication != null ? authentication.getName() : null;
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
