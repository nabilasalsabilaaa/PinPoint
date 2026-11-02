package com.kelompok8.service;

import com.kelompok8.model.User;
import com.kelompok8.repository.UserRepository;
import com.kelompok8.dto.ForgotPasswordRequest;
import com.kelompok8.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String register(User userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return "Error: Email sudah terdaftar!";
        }
        if (userRequest.getNoTelepon() != null && userRepository.existsByNoTelepon(userRequest.getNoTelepon())) {
            return "Error: Nomor telepon sudah terdaftar!";
        }
        
        userRequest.setIdRole(2L); // ID 2 untuk User/Masyarakat
        userRequest.setStatusAkun("ACTIVE");
        userRequest.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        userRepository.save(userRequest);
        return "Registrasi berhasil!";
    }

    @Transactional
    public User authenticate(String identifier, String rawPassword) {
        System.out.println("Mencoba login dengan identifier: [" + identifier + "] dan password: [" + rawPassword + "]");
        // Cari user berdasarkan email ATAU noTelepon menggunakan input identifier yang sama
        Optional<User> optionalUser = userRepository.findByEmailOrNoTelepon(identifier, identifier);
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isActive() && passwordEncoder.matches(rawPassword, user.getPassword())) {
                
                // Update waktu terakhir login dan simpan ke database
                user.setTerakhirLogin(LocalDateTime.now());
                userRepository.save(user);

                return user;
            }
        }
        return null;
    }

    public String resetPassword(ForgotPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            return "Kata sandi berhasil diubah!";
        }
        return "Error: Email tidak ditemukan di sistem!";
    }

    public String checkAccountAndSendOtp(String identifier, String type) {
        Optional<User> userOpt;
        if ("EMAIL".equalsIgnoreCase(type)) {
            userOpt = userRepository.findByEmail(identifier);
        } else {
            userOpt = userRepository.findByNoTelepon(identifier);
        }

        if (userOpt.isEmpty()) {
            return "Error: Akun dengan " + (type.equalsIgnoreCase("EMAIL") ? "email" : "nomor telepon")
                    + " tersebut tidak terdaftar!";
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            return "Error: Akun Anda sedang diblokir atau tidak aktif!";
        }

        // Logika pengiriman link / OTP via Email Service atau WA Gateway dimasukkan di sini
        return "Tautan pemulihan instan telah dikirim.";
    }

    // public void processForgotPassword(String email) {
    //     // 1. Cek apakah email terdaftar di DB
    //     // 2. Generate OTP 6 digit (contoh: String otp = String.valueOf((int)(Math.random() * 900000) + 100000);)
    //     // 3. Simpan OTP & Jam Kadaluarsa (15 menit) di DB
        
    //     // 4. Kirim Email OTP
    //     emailService.sendOtpEmail(email, otp);
    // }
}