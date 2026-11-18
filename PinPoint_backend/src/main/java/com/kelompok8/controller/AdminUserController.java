package com.kelompok8.controller;

import com.kelompok8.model.User;
import com.kelompok8.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*") 
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        // Melihat daftar seluruh pengguna terdaftar di sistem
        List<User> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User userRequest) {
        try {
            User created = adminUserService.createUser(userRequest);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id) {
        // Melihat profil detail pengguna tertentu
        return adminUserService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Memblokir atau mengaktifkan akun pengguna (verifikasi akun)
            Object activeObj = requestBody.get("active");
            boolean active = activeObj instanceof Boolean ? (Boolean) activeObj : true;
            String statusAkun = (String) requestBody.getOrDefault("statusAkun", active ? "ACTIVE" : "BLOCKED");

            User updatedUser = adminUserService.updateUserStatus(id, active, statusAkun);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminUserService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody User profileRequest) {
        try {
            String email = authentication != null ? authentication.getName() : profileRequest.getEmail();
            User updated = adminUserService.updateProfile(email, profileRequest);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> requestBody) {
        try {
            String email = authentication != null ? authentication.getName() : requestBody.get("email");
            String oldPassword = requestBody.get("oldPassword");
            String newPassword = requestBody.get("newPassword");
            adminUserService.changePassword(email, oldPassword, newPassword);
            return ResponseEntity.ok("Kata sandi berhasil diperbarui");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}