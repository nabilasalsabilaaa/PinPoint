package com.kelompok8.service;

import com.kelompok8.model.User;
import com.kelompok8.repository.LaporanRepository;
import com.kelompok8.repository.UserRepository;
import com.kelompok8.util.PhoneNumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LaporanRepository laporanRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Map<String, String> userPhotosByEmail = new ConcurrentHashMap<>();

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            if (u.getId() != null) {
                u.setReportCount((int) laporanRepository.countByIdUser(u.getId()));
            }
            if (u.getEmail() != null) {
                String cachedPhoto = userPhotosByEmail.get(u.getEmail().trim().toLowerCase());
                if (cachedPhoto != null) {
                    u.setFotoProfil(cachedPhoto);
                }
            }
        }
        return users;
    }

    public Optional<User> getUserById(Long id) {
        Optional<User> opt = userRepository.findById(id);
        opt.ifPresent(u -> {
            if (u.getId() != null) {
                u.setReportCount((int) laporanRepository.countByIdUser(u.getId()));
            }
            if (u.getEmail() != null) {
                String cachedPhoto = userPhotosByEmail.get(u.getEmail().trim().toLowerCase());
                if (cachedPhoto != null) {
                    u.setFotoProfil(cachedPhoto);
                }
            }
        });
        return opt;
    }

    public User createUser(User userRequest) {
        if (userRequest.getNama() == null || userRequest.getNama().isBlank()) {
            throw new RuntimeException("Nama lengkap wajib diisi!");
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().isBlank()) {
            throw new RuntimeException("Email wajib diisi!");
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
            throw new RuntimeException("Kata sandi sementara wajib diisi!");
        }
        if (userRepository.existsByEmailIgnoreCase(userRequest.getEmail().trim())) {
            throw new RuntimeException("Email sudah terdaftar!");
        }
        String normalizedPhone = PhoneNumberUtils.normalizeToLocalZero(userRequest.getNoTelepon());
        if (!normalizedPhone.isEmpty()) {
            if (!PhoneNumberUtils.isValidNormalizedPhone(normalizedPhone)) {
                throw new RuntimeException("Format nomor telepon tidak valid! Gunakan awalan 0 atau 62.");
            }
            if (userRepository.existsByNoTeleponIn(PhoneNumberUtils.buildEquivalentVariants(normalizedPhone))) {
                throw new RuntimeException("Nomor telepon sudah terdaftar!");
            }
        }

        try {
            userRepository.syncSequence();
        } catch (Exception ignored) {}
        userRequest.setId(null);
        userRequest.setNama(userRequest.getNama().trim());
        userRequest.setEmail(userRequest.getEmail().trim().toLowerCase());
        if (!normalizedPhone.isEmpty()) {
            userRequest.setNoTelepon(normalizedPhone);
        }
        if (userRequest.getIdRole() == null) {
            userRequest.setIdRole(3L); // Default Dinas/Pemerintah jika dibuat dari Admin
        }
        if (userRequest.getStatusAkun() == null || userRequest.getStatusAkun().isBlank()) {
            userRequest.setStatusAkun("ACTIVE");
        }
        userRequest.setTanggalDaftar(LocalDateTime.now());
        userRequest.setPassword(passwordEncoder.encode(userRequest.getPassword()));

        User saved = userRepository.save(userRequest);
        saved.setReportCount(0);
        return saved;
    }

    public User updateUserStatus(Long id, boolean active, String statusAkun) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pengguna tidak ditemukan"));
        
        if (statusAkun != null && !statusAkun.isBlank()) {
            user.setStatusAkun(statusAkun);
        } else {
            user.setActive(active);
        }
        
        User saved = userRepository.save(user);
        saved.setReportCount((int) laporanRepository.countByIdUser(saved.getId()));
        return saved;
    }

    public User updateProfile(String currentEmail, User profileData) {
        String lookupEmail = (currentEmail != null && !currentEmail.isBlank())
                ? currentEmail
                : profileData.getEmail();
        if (lookupEmail == null || lookupEmail.isBlank()) {
            throw new RuntimeException("Email pengguna tidak ditemukan");
        }
        User user = userRepository.findByEmail(lookupEmail)
                .orElseThrow(() -> new RuntimeException("Pengguna tidak ditemukan"));

        if (profileData.getNama() != null && !profileData.getNama().isBlank()) {
            user.setNama(profileData.getNama().trim());
        }
        if (profileData.getNoTelepon() != null && !profileData.getNoTelepon().isBlank()) {
            String normalizedPhone = PhoneNumberUtils.normalizeToLocalZero(profileData.getNoTelepon());
            if (!PhoneNumberUtils.isValidNormalizedPhone(normalizedPhone)) {
                throw new RuntimeException("Format nomor telepon tidak valid! Gunakan awalan 0 atau 62.");
            }
            List<User> existingWithPhone = userRepository.findAllByNoTeleponIn(
                    PhoneNumberUtils.buildEquivalentVariants(normalizedPhone)
            );
            boolean ownedByOther = existingWithPhone.stream()
                    .anyMatch(other -> other.getId() != null && !other.getId().equals(user.getId()));
            if (ownedByOther) {
                throw new RuntimeException("Nomor telepon sudah digunakan oleh akun lain!");
            }
            user.setNoTelepon(normalizedPhone);
        }
        if (profileData.getFotoProfil() != null && !profileData.getFotoProfil().isBlank()) {
            userPhotosByEmail.put(user.getEmail().trim().toLowerCase(), profileData.getFotoProfil());
            user.setFotoProfil(profileData.getFotoProfil());
        }
        User saved = userRepository.save(user);
        saved.setFotoProfil(userPhotosByEmail.get(saved.getEmail().trim().toLowerCase()));
        return saved;
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Pengguna tidak ditemukan");
        }
        userRepository.deleteById(id);
    }

    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Pengguna tidak ditemukan"));
        if (oldPassword != null && !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Kata sandi lama tidak sesuai");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}