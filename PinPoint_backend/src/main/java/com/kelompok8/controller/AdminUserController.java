package com.kelompok8.controller;

import com.kelompok8.model.User;
import com.kelompok8.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        // Melihat daftar seluruh pengguna terdaftar di sistem
        List<User> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id) {
        // Melihat profil detail pengguna tertentu
        return adminUserService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Memblokir atau mengaktifkan akun pengguna (verifikasi akun)
            boolean active = (boolean) requestBody.getOrDefault("active", true);
            String statusAkun = (String) requestBody.getOrDefault("statusAkun", active ? "ACTIVE" : "BLOCKED");

            User updatedUser = adminUserService.updateUserStatus(id, active, statusAkun);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}