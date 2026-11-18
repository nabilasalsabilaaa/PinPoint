package com.kelompok8.service;

import com.kelompok8.dto.ForgotPasswordRequest;
import com.kelompok8.model.User;
import com.kelompok8.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    private static class PendingRegistration {
        final User userRequest;
        final String otp;
        final LocalDateTime expiresAt;

        PendingRegistration(User userRequest, String otp, LocalDateTime expiresAt) {
            this.userRequest = userRequest;
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }

    private static class ResetOtpEntry {
        final String email;
        final String otp;
        final LocalDateTime expiresAt;
        boolean verified;

        ResetOtpEntry(String email, String otp, LocalDateTime expiresAt) {
            this.email = email;
            this.otp = otp;
            this.expiresAt = expiresAt;
            this.verified = false;
        }
    }

    private final Map<String, PendingRegistration> pendingRegistrationsByEmail = new ConcurrentHashMap<>();
    private final Map<String, ResetOtpEntry> resetOtpByEmail = new ConcurrentHashMap<>();

    private String generateSixDigitOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Tahap 1 Registrasi: Validasi data & keaktifan email, lalu kirim kode OTP 6 digit (berlaku 15 menit).
     */
    public String requestRegisterOtp(User userRequest) {
        if (userRequest.getNama() == null || userRequest.getNama().isBlank()) {
            return "Error: Nama lengkap wajib diisi!";
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().isBlank()) {
            return "Error: Email wajib diisi!";
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
            return "Error: Kata sandi wajib diisi!";
        }

        String cleanEmail = userRequest.getEmail().trim().toLowerCase();
        String cleanPhone = userRequest.getNoTelepon() != null ? userRequest.getNoTelepon().trim() : "";

        if (!emailService.isEmailActiveAndValid(cleanEmail)) {
            return "Error: Alamat email tidak valid atau domain email tidak aktif!";
        }

        if (userRepository.existsByEmail(cleanEmail) || userRepository.findByEmailOrNoTelepon(cleanEmail, cleanEmail).isPresent()) {
            return "Error: Email sudah terdaftar!";
        }
        if (!cleanPhone.isEmpty() && userRepository.existsByNoTelepon(cleanPhone)) {
            return "Error: Nomor telepon sudah terdaftar!";
        }

        userRequest.setNama(userRequest.getNama().trim());
        userRequest.setEmail(cleanEmail);
        userRequest.setNoTelepon(cleanPhone);

        String otp = generateSixDigitOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        try {
            emailService.sendOtpEmail(cleanEmail, userRequest.getNama(), otp, "Registrasi Akun Baru");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        pendingRegistrationsByEmail.put(cleanEmail, new PendingRegistration(userRequest, otp, expiresAt));
        return "Kode OTP 6 digit telah dikirim ke " + cleanEmail;
    }

    /**
     * Tahap 2 Registrasi: Verifikasi OTP 6 digit. Jika cocok & belum lewat 15 menit, akun disimpan ke DB.
     */
    public String verifyRegisterOtpAndCreateAccount(String email, String otp) {
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return "Error: Email dan kode OTP wajib diisi!";
        }
        String cleanEmail = email.trim().toLowerCase();
        String cleanOtp = otp.trim();

        PendingRegistration pending = pendingRegistrationsByEmail.get(cleanEmail);
        if (pending == null) {
            return "Error: Data pendaftaran tidak ditemukan. Silakan daftar ulang.";
        }
        if (LocalDateTime.now().isAfter(pending.expiresAt)) {
            pendingRegistrationsByEmail.remove(cleanEmail);
            return "Error: Kode OTP sudah kedaluwarsa (lebih dari 15 menit). Silakan kirim ulang OTP.";
        }
        if (!pending.otp.equals(cleanOtp)) {
            return "Error: Kode OTP yang Anda masukkan salah! Gagal membuat akun.";
        }

        User userToSave = pending.userRequest;
        if (userRepository.existsByEmail(cleanEmail)) {
            pendingRegistrationsByEmail.remove(cleanEmail);
            return "Error: Email sudah terdaftar!";
        }

        try {
            userRepository.syncSequence();
        } catch (Exception ignored) {}

        userToSave.setId(null);
        if (userToSave.getIdRole() == null) {
            userToSave.setIdRole(2L); // Role 2 = USER
        }
        userToSave.setStatusAkun("ACTIVE");
        userToSave.setTanggalDaftar(LocalDateTime.now());
        userToSave.setPassword(passwordEncoder.encode(userToSave.getPassword()));
        userRepository.save(userToSave);

        pendingRegistrationsByEmail.remove(cleanEmail);
        return "Registrasi berhasil! Akun Anda telah dibuat.";
    }

    /**
     * Endpoint langsung register (fallback kompatibilitas).
     */
    public String register(User userRequest) {
        if (userRequest.getEmail() == null || userRequest.getEmail().isBlank()) {
            return "Error: Email wajib diisi!";
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
            return "Error: Kata sandi wajib diisi!";
        }
        String cleanEmail = userRequest.getEmail().trim().toLowerCase();
        if (!emailService.isEmailActiveAndValid(cleanEmail)) {
            return "Error: Alamat email tidak valid atau tidak aktif!";
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            return "Error: Email sudah terdaftar!";
        }
        if (userRequest.getNoTelepon() != null && !userRequest.getNoTelepon().isBlank()
                && userRepository.existsByNoTelepon(userRequest.getNoTelepon().trim())) {
            return "Error: Nomor telepon sudah terdaftar!";
        }

        try {
            userRepository.syncSequence();
        } catch (Exception ignored) {}
        userRequest.setId(null);
        userRequest.setEmail(cleanEmail);
        if (userRequest.getIdRole() == null) {
            userRequest.setIdRole(2L);
        }
        userRequest.setStatusAkun("ACTIVE");
        userRequest.setTanggalDaftar(LocalDateTime.now());
        userRequest.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        userRepository.save(userRequest);
        return "Registrasi berhasil!";
    }

    @Transactional
    public User authenticate(String identifier, String rawPassword) {
        if (identifier == null || rawPassword == null) return null;
        String cleanIdentifier = identifier.trim();
        String cleanPassword = rawPassword.trim();
        Optional<User> optionalUser = userRepository.findByEmailOrNoTelepon(cleanIdentifier, cleanIdentifier);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String storedPass = user.getPassword();
            boolean passwordMatches = false;
            if (storedPass != null) {
                if (storedPass.startsWith("$2a$") || storedPass.startsWith("$2b$") || storedPass.startsWith("$2y$")) {
                    passwordMatches = passwordEncoder.matches(cleanPassword, storedPass);
                } else {
                    passwordMatches = cleanPassword.equals(storedPass);
                    if (passwordMatches) {
                        user.setPassword(passwordEncoder.encode(cleanPassword));
                    }
                }
            }
            if (user.isActive() && passwordMatches) {
                if (user.getIdRole() == null) {
                    user.setIdRole(2L);
                }
                user.setTerakhirLogin(LocalDateTime.now());
                userRepository.save(user);
                return user;
            }
        }
        return null;
    }

    /**
     * Tahap 1 Reset Kata Sandi: Cek akun terdaftar & keaktifan emailnya, lalu kirim OTP 6 digit (15 menit).
     * Mengembalikan email tujuan jika berhasil, atau pesan diawali "Error:" jika gagal.
     */
    public String checkAccountAndSendOtp(String identifier, String type) {
        if (identifier == null || identifier.isBlank()) {
            return "Error: Harap masukkan email atau nomor telepon Anda!";
        }
        String cleanId = identifier.trim();
        Optional<User> userOpt = userRepository.findByEmailOrNoTelepon(cleanId, cleanId);

        if (userOpt.isEmpty()) {
            return "Error: Akun dengan " + ("PHONE".equalsIgnoreCase(type) ? "nomor telepon" : "email")
                    + " tersebut tidak terdaftar di sistem!";
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            return "Error: Akun Anda sedang ditangguhkan atau tidak aktif!";
        }

        String targetEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
        if (!emailService.isEmailActiveAndValid(targetEmail)) {
            return "Error: Email yang terdaftar pada akun ini (" + targetEmail + ") tidak valid atau tidak aktif!";
        }

        String otp = generateSixDigitOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        try {
            emailService.sendOtpEmail(targetEmail, user.getNama(), otp, "Reset Kata Sandi");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        resetOtpByEmail.put(targetEmail, new ResetOtpEntry(targetEmail, otp, expiresAt));
        return targetEmail;
    }

    /**
     * Tahap 2 Reset Kata Sandi: Verifikasi kecocokan kode OTP 6 digit & masa berlaku 15 menit.
     */
    public String verifyResetOtp(String email, String otp) {
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return "Error: Email dan kode OTP wajib diisi!";
        }
        String cleanEmail = email.trim().toLowerCase();
        String cleanOtp = otp.trim();

        ResetOtpEntry entry = resetOtpByEmail.get(cleanEmail);
        if (entry == null) {
            return "Error: Permintaan OTP tidak ditemukan. Silakan minta ulang kode OTP.";
        }
        if (LocalDateTime.now().isAfter(entry.expiresAt)) {
            resetOtpByEmail.remove(cleanEmail);
            return "Error: Kode OTP sudah kedaluwarsa (lebih dari 15 menit). Silakan minta kode baru.";
        }
        if (!entry.otp.equals(cleanOtp)) {
            return "Error: Kode OTP tidak cocok! Tidak dapat mereset kata sandi.";
        }

        entry.verified = true;
        return "Kode OTP berhasil diverifikasi.";
    }

    /**
     * Tahap 3 Reset Kata Sandi: Simpan kata sandi baru setelah OTP terverifikasi.
     */
    public String resetPassword(ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return "Error: Email tidak ditemukan!";
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            return "Error: Kata sandi baru minimal 6 karakter!";
        }

        String cleanEmail = request.getEmail().trim().toLowerCase();
        ResetOtpEntry entry = resetOtpByEmail.get(cleanEmail);
        if (entry == null) {
            return "Error: Sesi verifikasi OTP tidak ditemukan atau sudah berakhir!";
        }
        if (LocalDateTime.now().isAfter(entry.expiresAt)) {
            resetOtpByEmail.remove(cleanEmail);
            return "Error: Masa berlaku OTP (15 menit) telah habis! Silakan ulangi proses reset sandi.";
        }
        if (request.getOtp() != null && !request.getOtp().isBlank() && !entry.otp.equals(request.getOtp().trim())) {
            return "Error: Kode OTP tidak valid!";
        }
        if (!entry.verified && (request.getOtp() == null || !entry.otp.equals(request.getOtp().trim()))) {
            return "Error: Kode OTP belum diverifikasi!";
        }

        Optional<User> optionalUser = userRepository.findByEmailOrNoTelepon(cleanEmail, cleanEmail);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
            userRepository.save(user);
            resetOtpByEmail.remove(cleanEmail);
            return "Kata sandi berhasil diubah!";
        }
        return "Error: Email tidak ditemukan di sistem!";
    }
}
