package com.kelompok8.controller;

import com.kelompok8.dto.ForgotPasswordRequest;
import com.kelompok8.dto.LoginRequest;
import com.kelompok8.dto.LoginResponse;
import com.kelompok8.model.User;
import com.kelompok8.security.JwtUtils;
import com.kelompok8.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        String result = authService.register(user);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/register/request-otp")
    public ResponseEntity<?> requestRegisterOtp(@RequestBody User user) {
        String result = authService.requestRegisterOtp(user);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(Map.of(
                "message", result,
                "email", user.getEmail()
        ));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyRegisterOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String result = authService.verifyRegisterOtpAndCreateAccount(email, otp);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = authService.authenticate(request.getIdentifier(), request.getPassword());
        if (user == null) {
            return ResponseEntity.status(401).body("Error: Email/Nomor Ponsel atau password salah, atau akun tidak aktif!");
        }

        String token = jwtUtils.generateJwtToken(user.getEmail(), user.getRole());
        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getIdRole(),
                token,
                user.getEmail(),
                user.getNama(),
                user.getRole(),
                user.getNoTelepon(),
                user.getIsSuspended()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-account")
    public ResponseEntity<?> checkAccount(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String type = request.get("type");

        String result = authService.checkAccountAndSendOtp(identifier, type);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(Map.of(
                "message", "Kode OTP 6 digit telah dikirim ke email " + result,
                "email", result
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String mode = request.getOrDefault("mode", "FORGOT_PASSWORD");

        String result;
        if ("REGISTER".equalsIgnoreCase(mode)) {
            result = authService.verifyRegisterOtpAndCreateAccount(email, otp);
        } else {
            result = authService.verifyResetOtp(email, otp);
        }

        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String result = authService.resetPassword(request);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtils.validateToken(token)) {
                    return ResponseEntity.ok("Token masih valid. Sesi diperbarui.");
                }
            }
            return ResponseEntity.status(401).body("Error: Token tidak valid atau sudah kedaluwarsa.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logout berhasil. Silakan hapus token di sisi klien.");
    }
}
