package com.kelompok8.controller;

import com.kelompok8.dto.DisposisiRequest;
import com.kelompok8.model.BuktiPerbaikan;
import com.kelompok8.model.Laporan;
import com.kelompok8.service.AdminReportService;
import com.kelompok8.service.GovernmentReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class GovernmentReportController {

    @Autowired
    private GovernmentReportService governmentReportService;

    @Autowired
    private AdminReportService adminReportService;

    @GetMapping("/api/government/reports")
    public ResponseEntity<List<Laporan>> getAllGovernmentReports() {
        return ResponseEntity.ok(governmentReportService.getGovernmentReports());
    }

    @GetMapping("/api/government/reports/verified")
    public ResponseEntity<List<Laporan>> getVerifiedReports() {
        List<Laporan> reports = governmentReportService.getVerifiedReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/api/government/reports/assigned")
    public ResponseEntity<?> getAssignedReports(Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            List<Laporan> reports = governmentReportService.getAssignedReports(email);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/api/government/reports/{id}")
    public ResponseEntity<?> getGovernmentReportById(@PathVariable Long id) {
        return adminReportService.getAllReports().stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/government/reports/{id}/disposition")
    public ResponseEntity<?> disposeReport(
            @PathVariable Long id,
            @RequestBody DisposisiRequest request,
            Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            Laporan updated = governmentReportService.disposeReport(id, request, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/api/government/reports/{id}/process", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> processReport(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            String keterangan = body != null && body.get("keterangan") != null ? body.get("keterangan").toString() : null;
            List<String> photos = extractPhotos(body);
            Laporan updated = governmentReportService.updateStatusProcess(id, email, keterangan, photos);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/api/government/reports/{id}/evidence")
    public ResponseEntity<?> uploadEvidence(
            @PathVariable Long id,
            @RequestBody BuktiPerbaikan buktiRequest,
            Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            BuktiPerbaikan saved = governmentReportService.uploadEvidence(id, email, buktiRequest);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/api/government/reports/{id}/complete", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> completeReport(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            String keterangan = body != null && body.get("keterangan") != null ? body.get("keterangan").toString() : null;
            boolean sendNotif = body == null || !Boolean.FALSE.equals(body.get("sendNotification"));
            List<String> photos = extractPhotos(body);
            Laporan updated = governmentReportService.completeReportWithEvidence(id, email, keterangan, photos, sendNotif);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/api/files/{filename:.+}")
    public ResponseEntity<Resource> serveUploadedFile(@PathVariable String filename) {
        File file = new File("uploads", filename);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    private List<String> extractPhotos(Map<String, Object> body) {
        List<String> photos = new ArrayList<>();
        if (body == null) return photos;
        Object rawList = body.getOrDefault("photoUrls", body.get("photos"));
        if (rawList instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !item.toString().isBlank()) {
                    photos.add(item.toString());
                }
            }
        }
        return photos;
    }
}
