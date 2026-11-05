package com.kelompok8.controller;

import com.kelompok8.dto.*;
import com.kelompok8.model.User;
import com.kelompok8.security.JwtUtils;
import com.kelompok8.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller untuk endpoint autentikasi PinPoint.
 *
 * <p>Base URL: {@code /api/auth} — semua endpoint bersifat publik (tidak butuh JWT).
 *
 * <p>Alur lupa password (3 langkah):
 * <ol>
 *   <li>{@code POST /check-account} — verifikasi email/no.hp & kirim OTP</li>
 *   <li>{@code POST /verify-otp}    — validasi kode OTP 6-digit</li>
 *   <li>{@code POST /forgot-password} — set password baru</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoint registrasi, login, dan manajemen kata sandi")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    // ══════════════════════════════════════════════════════════════════════════
    //  REGISTER
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Mendaftarkan pengguna baru sebagai WARGA.
     * Email sambutan dikirim otomatis setelah registrasi berhasil.
     *
     * @param request data pendaftaran: nama, email, noTelepon, password
     * @return 201 Created + pesan sukses, atau 400 Bad Request + pesan error
     */
    @Operation(
        summary     = "Registrasi pengguna baru",
        description = "Mendaftarkan akun WARGA baru. Email sambutan dikirim ke alamat yang didaftarkan."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Registrasi berhasil",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Email atau nomor telepon sudah terdaftar",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        logger.info("Permintaan registrasi untuk email: {}", request.getEmail());

        // Validasi input dasar
        if (request.getNama() == null || request.getNama().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Nama tidak boleh kosong."));
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Email tidak boleh kosong."));
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Password minimal 6 karakter."));
        }

        String result = authService.register(request);

        if (result.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, result));
        }

        // Langsung kirim OTP untuk verifikasi email setelah registrasi
        try {
            authService.sendRegistrationOtp(request.getEmail(), request.getNama());
            logger.info("OTP registrasi berhasil dikirim ke: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Registrasi berhasil! Kode OTP telah dikirim ke email Anda."));
        } catch (Exception e) {
            logger.error("Gagal mengirim OTP registrasi ke {}: {}", request.getEmail(), e.getMessage());
            authService.deleteAccount(request.getEmail());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Mengautentikasi pengguna dan mengembalikan JWT token.
     * Login dapat menggunakan email atau nomor telepon sebagai {@code identifier}.
     *
     * @param request berisi {@code identifier} (email/no.hp) dan {@code password}
     * @return 200 OK + LoginResponse (token JWT + info user), atau 401 Unauthorized
     */
    @Operation(
        summary     = "Login pengguna",
        description = "Autentikasi dengan email atau nomor telepon. Mengembalikan JWT Bearer token."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Login berhasil",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Kredensial tidak valid atau akun tidak aktif",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        logger.info("Percobaan login: [{}]", request.getIdentifier());

        User user = authService.authenticate(request.getIdentifier(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false,
                            "Email/Nomor Ponsel atau password salah, atau akun tidak aktif."));
        }

        String token = jwtUtils.generateJwtToken(user.getEmail(), user.getRole());
        LoginResponse response = new LoginResponse(
                token,
                user.getEmail(),
                user.getNama(),
                user.getRole()
        );

        logger.info("Login berhasil: {} (role={})", user.getEmail(), user.getRole());
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 1: Cek Akun & Kirim OTP
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Memverifikasi email/no.hp terdaftar lalu mengirim OTP ke email pengguna.
     *
     * <p>Body request: {@code {"identifier": "user@email.com", "type": "EMAIL"}}
     * atau {@code {"identifier": "08123456789", "type": "PHONE"}}
     *
     * @param request map berisi {@code identifier} dan {@code type}
     * @return 200 OK jika OTP terkirim, atau 400/404 jika gagal
     */
    @Operation(
        summary     = "Kirim OTP lupa password",
        description = "Memverifikasi akun dan mengirim kode OTP 6-digit ke email terdaftar. " +
                      "Gunakan type=EMAIL atau type=PHONE."
    )
    @PostMapping("/check-account")
    public ResponseEntity<ApiResponse> checkAccount(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String type       = request.get("type"); // "EMAIL" atau "PHONE"

        if (identifier == null || identifier.isBlank() || type == null || type.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Field 'identifier' dan 'type' wajib diisi."));
        }

        String result = authService.checkAccountAndSendOtp(identifier, type);

        if (result.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, result));
        }

        return ResponseEntity.ok(new ApiResponse(true, result));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 2: Verifikasi OTP
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Memverifikasi kode OTP yang dimasukkan pengguna.
     * OTP hanya berlaku sekali dan kadaluwarsa setelah 15 menit.
     *
     * @param request berisi {@code email} dan {@code otpCode}
     * @return 200 OK jika OTP valid, atau 400 jika tidak valid / kadaluwarsa
     */
    @Operation(
        summary     = "Verifikasi kode OTP",
        description = "Memvalidasi kode OTP 6-digit. OTP berlaku 15 menit dan hanya dapat digunakan sekali."
    )
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody OtpVerifyRequest request) {
        if (request.getEmail() == null || request.getOtpCode() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Email dan kode OTP wajib diisi."));
        }

        boolean valid = authService.verifyOtp(request.getEmail(), request.getOtpCode());

        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false,
                            "Error: Kode OTP tidak valid atau sudah kedaluwarsa. Silakan minta kode baru."));
        }

        return ResponseEntity.ok(new ApiResponse(true,
                "OTP berhasil diverifikasi. Silakan atur kata sandi baru Anda."));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD — STEP 3: Reset Password
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Menetapkan password baru setelah OTP berhasil diverifikasi.
     *
     * @param request berisi {@code email} dan {@code newPassword}
     * @return 200 OK jika berhasil, atau 400 jika email tidak ditemukan
     */
    @Operation(
        summary     = "Reset kata sandi",
        description = "Menetapkan kata sandi baru. Panggil endpoint ini HANYA setelah OTP berhasil diverifikasi."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getNewPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Email dan kata sandi baru wajib diisi."));
        }
        if (request.getNewPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Kata sandi minimal 6 karakter."));
        }

        String result = authService.resetPassword(request);

        if (result.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, result));
        }

        return ResponseEntity.ok(new ApiResponse(true, result));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REFRESH TOKEN
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Memvalidasi JWT token yang ada. Klien bisa memanggil endpoint ini
     * untuk mengecek apakah token masih valid sebelum melakukan request penting.
     *
     * @param authHeader header Authorization berisi "Bearer {token}"
     * @return 200 OK jika token masih valid, atau 401 jika sudah kedaluwarsa
     */
    @Operation(
        summary     = "Validasi / refresh token",
        description = "Mengecek kevalidan JWT token. Token dikembalikan tanpa perubahan."
    )
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtils.validateToken(token)) {
                String email = jwtUtils.getEmailFromToken(token);
                return ResponseEntity.ok(
                        new ApiResponse(true, "Token masih valid.", Map.of("email", email)));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Error: Token tidak valid atau sudah kedaluwarsa."));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE ACCOUNT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Menghapus akun pengguna berdasarkan email.
     *
     * @param request berisi email akun yang ingin dihapus
     * @return status keberhasilan penghapusan akun
     */
    @Operation(
        summary     = "Hapus akun pengguna",
        description = "Menghapus akun pengguna berdasarkan email."
    )
    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse> deleteAccount(@RequestBody DeleteAccountRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: Email wajib diisi."));
        }
        String result = authService.deleteAccount(request.getEmail());
        if (result.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, result));
        }
        return ResponseEntity.ok(new ApiResponse(true, result));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGOUT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Logout pengguna. Karena JWT stateless, server tidak menyimpan sesi.
     * Klien bertanggung jawab menghapus token dari penyimpanannya.
     *
     * @return 200 OK dengan instruksi untuk klien
     */
    @Operation(
        summary     = "Logout pengguna",
        description = "Karena JWT bersifat stateless, hapus token di sisi klien (localStorage / SecureStorage)."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        return ResponseEntity.ok(
                new ApiResponse(true,
                        "Logout berhasil. Silakan hapus token JWT di sisi klien."));
    }
}
