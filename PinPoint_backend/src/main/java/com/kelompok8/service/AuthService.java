package com.kelompok8.service;

import com.kelompok8.dto.ForgotPasswordRequest;
import com.kelompok8.dto.RegisterRequest;
import com.kelompok8.model.User;
import com.kelompok8.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service untuk logika bisnis autentikasi pengguna PinPoint.
 *
 * <p>Mendukung alur:
 * <ul>
 *   <li>Registrasi (WARGA baru, idRole=2)</li>
 *   <li>Login via email atau nomor telepon</li>
 *   <li>Lupa password: kirim OTP → verifikasi OTP → reset password</li>
 * </ul>
 *
 * <p>OTP disimpan sementara di in-memory {@code ConcurrentHashMap}.
 * Untuk produksi, pertimbangkan Redis atau tabel DB agar tahan restart.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Penyimpanan OTP sementara: key=email, value=[otpCode, expiredAt ISO string].
     * Thread-safe dengan ConcurrentHashMap.
     */
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════════════════════════════════════
    //  REGISTER
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Mendaftarkan pengguna baru sebagai WARGA (idRole = 2).
     *
     * @param request data registrasi dari DTO {@code RegisterRequest}
     * @return pesan sukses atau pesan error (dimulai dengan "Error:")
     */
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return "Error: Email sudah terdaftar!";
        }
        if (request.getNoTelepon() != null && !request.getNoTelepon().isBlank()
                && userRepository.existsByNoTelepon(request.getNoTelepon())) {
            return "Error: Nomor telepon sudah terdaftar!";
        }

        User user = new User();
        user.setNama(request.getNama());
        user.setEmail(request.getEmail());
        user.setNoTelepon(request.getNoTelepon());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIdRole(2L);        // 2 = WARGA
        user.setStatusAkun("ACTIVE");
        userRepository.save(user);

        // Kirim email sambutan secara asinkron
        emailService.sendWelcomeEmail(user.getEmail(), user.getNama());
        logger.info("Pengguna baru terdaftar: {} ({})", user.getNama(), user.getEmail());

        return "Registrasi berhasil! Silakan login.";
    }

    /**
     * Overload untuk backward-compatibility dengan kode lama yang mengirim entitas {@code User}.
     */
    public String register(User userRequest) {
        RegisterRequest req = new RegisterRequest();
        req.setNama(userRequest.getNama());
        req.setEmail(userRequest.getEmail());
        req.setNoTelepon(userRequest.getNoTelepon());
        req.setPassword(userRequest.getPassword()); // masih plaintext di sini
        return register(req);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIN (AUTHENTICATE)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Mengautentikasi pengguna menggunakan email atau nomor telepon + password.
     * Jika berhasil, waktu terakhir login diperbarui.
     *
     * @param identifier email atau nomor telepon pengguna
     * @param rawPassword password plaintext dari request
     * @return objek {@code User} jika autentikasi berhasil, {@code null} jika gagal
     */
    @Transactional
    public User authenticate(String identifier, String rawPassword) {
        logger.debug("Percobaan login dengan identifier: [{}]", identifier);

        Optional<User> optionalUser =
                userRepository.findByEmailOrNoTelepon(identifier, identifier);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isActive() && passwordEncoder.matches(rawPassword, user.getPassword())) {
                user.setTerakhirLogin(LocalDateTime.now());
                userRepository.save(user);
                logger.info("Login berhasil: {} (role={})", user.getEmail(), user.getRole());
                return user;
            }
        }
        logger.warn("Login gagal untuk identifier: {}", identifier);
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 1: Kirim OTP
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Memeriksa keberadaan akun dan mengirim kode OTP ke email pengguna.
     *
     * @param identifier email atau nomor telepon
     * @param type       "EMAIL" atau "PHONE"
     * @return pesan sukses atau error
     */
    public String checkAccountAndSendOtp(String identifier, String type) {
        Optional<User> userOpt;

        if ("EMAIL".equalsIgnoreCase(type)) {
            userOpt = userRepository.findByEmail(identifier);
        } else {
            userOpt = userRepository.findByNoTelepon(identifier);
        }

        if (userOpt.isEmpty()) {
            return "Error: Akun dengan " +
                    ("EMAIL".equalsIgnoreCase(type) ? "email" : "nomor telepon") +
                    " tersebut tidak terdaftar!";
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            return "Error: Akun Anda sedang diblokir atau tidak aktif!";
        }

        // Generate OTP 6 digit, simpan ke store dengan TTL 15 menit
        String otpCode = generateOtp();
        otpStore.put(user.getEmail(), new OtpEntry(otpCode, LocalDateTime.now().plusMinutes(15)));

        // Kirim email OTP secara asinkron
        emailService.sendOtpEmail(user.getEmail(), otpCode, user.getNama());
        logger.info("OTP dikirim ke email: {} (kode disembunyikan dari log)", user.getEmail());

        return "Kode OTP telah dikirim ke email terdaftar Anda. Berlaku 15 menit.";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 2: Verifikasi OTP
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Memverifikasi kode OTP yang dimasukkan pengguna.
     *
     * @param email   email yang digunakan saat meminta OTP
     * @param otpCode kode OTP yang dimasukkan pengguna
     * @return {@code true} jika OTP valid dan belum kedaluwarsa
     */
    public boolean verifyOtp(String email, String otpCode) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(entry.expiredAt())) {
            otpStore.remove(email);
            return false;
        }
        boolean valid = entry.code().equals(otpCode);
        if (valid) {
            otpStore.remove(email); // OTP hanya boleh digunakan sekali
        }
        return valid;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 3: Reset Password
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Mereset password pengguna setelah OTP berhasil diverifikasi.
     *
     * @param request berisi email dan password baru
     * @return pesan sukses atau error
     */
    public String resetPassword(ForgotPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            logger.info("Password berhasil direset untuk: {}", request.getEmail());
            return "Kata sandi berhasil diubah! Silakan login dengan kata sandi baru.";
        }
        return "Error: Email tidak ditemukan di sistem!";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Menghasilkan kode OTP numerik 6 digit yang di-pad dengan leading zero jika perlu. */
    private String generateOtp() {
        int otp = new Random().nextInt(900000) + 100000; // 100000–999999
        return String.valueOf(otp);
    }

    /** Record internal untuk menyimpan OTP beserta waktu kedaluwarsa. */
    private record OtpEntry(String code, LocalDateTime expiredAt) {}
}
