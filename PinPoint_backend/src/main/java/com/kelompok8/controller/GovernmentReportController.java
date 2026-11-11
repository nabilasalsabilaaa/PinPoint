package com.kelompok8.controller;

import com.kelompok8.dto.DisposisiRequest;
import com.kelompok8.model.BuktiPerbaikan;
import com.kelompok8.model.Laporan;
import com.kelompok8.service.GovernmentReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/government/reports")
public class GovernmentReportController {

    @Autowired
    private GovernmentReportService governmentReportService;

    @GetMapping("/verified")
    public ResponseEntity<List<Laporan>> getVerifiedReports() {
        // Melihat laporan terverifikasi yang siap ditindaklanjuti
        List<Laporan> reports = governmentReportService.getVerifiedReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedReports(Authentication authentication) {
        try {
            List<Laporan> reports = governmentReportService.getAssignedReports(authentication.getName());
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/disposition")
    public ResponseEntity<?> disposeReport(
            @PathVariable Long id,
            @RequestBody DisposisiRequest request,
            Authentication authentication) {
        try {
            Laporan updated = governmentReportService.disposeReport(id, request, authentication.getName());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/process")
    public ResponseEntity<?> processReport(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Laporan updated = governmentReportService.updateStatusProcess(id, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<?> uploadEvidence(
            @PathVariable Long id, 
            @RequestBody BuktiPerbaikan buktiRequest, 
            Authentication authentication) {
        try {
            String email = authentication.getName();
            BuktiPerbaikan saved = governmentReportService.uploadEvidence(id, email, buktiRequest);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeReport(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Laporan updated = governmentReportService.completeReport(id, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}