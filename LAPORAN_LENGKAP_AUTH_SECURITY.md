# 📘 LAPORAN KOMPREHENSIF PENGEMBANGAN SISTEM
## MODUL AUTENTIKASI, OTORISASI (RBAC), DAN KEAMANAN
**Mata Kuliah:** Pengembangan Perangkat Lunak (PPL)  
**Program Studi:** Teknik Informatika / Sistem Informasi  
**Fakultas:** Fakultas Ilmu Komputer, Universitas Brawijaya  
**Nama Proyek:** PinPoint — Sistem Informasi Pelaporan & Pemantauan Kerusakan Fasilitas Umum Daerah  
**Tim Pengembang:** Kelompok 8  
**Tupoksi Pengembang:** Orang 1 (Modul Auth & Security - Backend & Mobile)  
**Status Modul:** Selesai Diimplementasikan & Terverifikasi 100% Lulus Uji (*Production-Ready*)  
**Tanggal Penyelesaian:** 4 November 2026  

---

## DAFTAR ISI
1. [BAB I: PENDAHULUAN](#bab-i-pendahuluan)
   - 1.1 Latar Belakang
   - 1.2 Rumusan Masalah & Solusi Keamanan
   - 1.3 Ruang Lingkup & Batasan Masalah (Tupoksi Orang 1)
   - 1.4 Tujuan Pengembangan
2. [BAB II: LANDASAN TEORI & ARSITEKTUR KEAMANAN](#bab-ii-landasan-teori--arsitektur-keamanan)
   - 2.1 Arsitektur Stateless Authentication vs Stateful Session
   - 2.2 Mekanisme JSON Web Token (JWT) & Kriptografi HMAC-SHA512
   - 2.3 Role-Based Access Control (RBAC) pada Spring Security 6
   - 2.4 Keamanan Penyimpanan Kredensial Mobile (AES256 & Android Keystore)
   - 2.5 Layanan Email Transaksional & Pola Asinkronus (@Async)
3. [BAB III: DESAIN & PERANCANGAN SISTEM](#bab-iii-desain--perancangan-sistem)
   - 3.1 Diagram Alur Autentikasi & Penerbitan JWT (Sequence Diagram)
   - 3.2 Diagram Alur Validasi Otorisasi RBAC & Filter Chain
   - 3.3 Diagram Alur Pemulihan Akun via Email OTP (Flowchart)
   - 3.4 Desain Basis Data & Struktur Entitas Pengguna
   - 3.5 Spesifikasi Kontrak RESTful API (OpenAPI v3 / Swagger)
   - 3.6 Desain Antarmuka Mobile (Material Design 3)
4. [BAB IV: IMPLEMENTASI SISTEM](#bab-iv-implementasi-sistem)
   - 4.1 Implementasi Backend (Spring Boot 4.1.1 LTS)
   - 4.2 Implementasi Frontend Mobile (Android Kotlin Native)
   - 4.3 Matriks Berkas yang Dibuat & Diperbarui
5. [BAB V: HASIL PENGUJIAN & EVALUASI](#bab-v-hasil-pengujian--evaluasi)
   - 5.1 Hasil Pengujian Otomatis Live Backend (22/22 Pass)
   - 5.2 Pengujian Penanganan Error Keamanan (401 & 403 JSON)
   - 5.3 Pengujian Antarmuka & Logika Mobile
6. [BAB VI: KESIMPULAN & REKOMENDASI](#bab-vi-kesimpulan--rekomendasi)
   - 6.1 Kesimpulan
   - 6.2 Rekomendasi Integrasi untuk Anggota Tim Lain

---

## BAB I: PENDAHULUAN

### 1.1 Latar Belakang
Aplikasi **PinPoint** merupakan platform berbasis mobile dan web service yang menjembatani partisipasi masyarakat dalam melaporkan kerusakan infrastruktur publik (seperti jalan berlubang, lampu penerangan jalan umum mati, saluran drainase tersumbat, dan fasilitas taman rusak) secara langsung kepada pihak instansi kedinasan terkait. 

Mengingat data pelaporan menyangkut validitas identitas warga, koordinat lokasi geografis, serta kewenangan pejabat dinas dalam memproses laporan, maka keberadaan modul autentikasi dan otorisasi yang aman, cepat, dan terisolasi menjadi prioritas utama arsitektur sistem.

### 1.2 Rumusan Masalah & Solusi Keamanan
1. **Bagaimana mencegah pemalsuan identitas pelapor dan penyalahgunaan hak akses?**  
   *Solusi:* Menerapkan token JWT yang ditandatangani secara digital menggunakan algoritma kriptografi HMAC-SHA512 serta penegakan hak akses berbasis peran (*Role-Based Access Control* - RBAC).
2. **Bagaimana memastikan aplikasi mobile menyimpan token dan kredensial secara aman?**  
   *Solusi:* Mengganti penyimpanan biasa (`SharedPreferences` teks polos) dengan `EncryptedSharedPreferences` yang memanfaatkan enkripsi dua lapis (AES256-SIV untuk Key dan AES256-GCM untuk Value) terikat pada Android Keystore hardware.
3. **Bagaimana menyediakan mekanisme pemulihan kata sandi yang aman tanpa membebani performa server?**  
   *Solusi:* Menerapkan pengiriman One-Time Password (OTP) 6-digit ke alamat email terdaftar melalui thread asinkron (`@Async` Spring Mail) dengan pembatasan waktu kedaluwarsa 15 menit dan hak guna sekali pakai (*single-use*).

### 1.3 Ruang Lingkup & Batasan Masalah (Tupoksi Orang 1)
Tugas pokok dan fungsi (Tupoksi) Orang 1 mencakup rekayasa keamanan menyeluruh pada sisi Backend dan Frontend Mobile:
- **Backend:**
  - Konfigurasi inti Spring Security (SecurityFilterChain, CSRF disable, Stateless Session).
  - Pembangunan komponen JWT (`JwtUtils`, `AuthTokenFilter`, `AuthEntryPointJwt`, `AuthAccessDeniedHandler`).
  - Penegakan izin akses peran: `ROLE_WARGA`, `ROLE_ADMIN`, dan `ROLE_DINAS`.
  - Pembuatan REST Controller untuk endpoint registrasi, otentikasi hybrid (email / no. telepon), verifikasi token, dan logout.
  - Implementasi `EmailService` untuk pengiriman email OTP dengan template HTML responsif.
- **Mobile (Android):**
  - Desain antarmuka XML Material Design 3 (Login, Register, OTP 6-Digit, Reset Password).
  - Integrasi jaringan HTTP menggunakan Retrofit 2 dan OkHttp Logging Interceptor.
  - Pengelolaan token dan fitur *Remember Me* menggunakan `EncryptedSharedPreferences`.
  - Logika interaktif form: auto-advance kotak input OTP, countdown timer, dan validasi kata sandi real-time.

---

## BAB II: LANDASAN TEORI & ARSITEKTUR KEAMANAN

### 2.1 Arsitektur Stateless Authentication vs Stateful Session
Pada arsitektur stateful konvensional, server menyimpan sesi pengguna di memori RAM (*HttpSession*) dan mengirimkan *session cookie* ke klien. Pola ini memiliki kelemahan skalabilitas horizontal karena memerlukan *sticky session* atau sinkronisasi memori antar-server.

PinPoint mengadopsi paradigma **Stateless Authentication**:
- Server tidak menyimpan status login di memori.
- Setiap permintaan HTTP dari aplikasi Android wajib membawa token JWT pada header `Authorization: Bearer <token>`.
- Server memverifikasi integritas token secara independen pada setiap request melalui kunci rahasia (*secret key*).

### 2.2 Mekanisme JSON Web Token (JWT) & Kriptografi HMAC-SHA512
Struktur JWT terdiri dari tiga bagian yang dipisahkan oleh tanda titik (`.`):
1. **Header:** Menentukan tipe token (`JWT`) dan algoritma hashing (`HS512`).
2. **Payload:** Memuat klaim identitas:
   - `sub` (Subject): Alamat email unik pengguna.
   - `role`: Peran pengguna (`WARGA`, `ADMIN`, atau `DINAS`).
   - `iat` (Issued At): Waktu penerbitan token.
   - `exp` (Expiration): Waktu kedaluwarsa token (disetel 24 jam).
3. **Signature:** Dihasilkan dari kombinasi `Base64(Header) + "." + Base64(Payload)` yang di-hash menggunakan algoritma HMAC-SHA512 dan kunci privat 512-bit server. Jika ada pihak yang memanipulasi payload, signature akan menjadi tidak valid dan permintaan langsung ditolak.

### 2.3 Role-Based Access Control (RBAC) pada Spring Security 6
Spring Security menerapkan otorisasi ketat berbasis hak istimewa (*GrantedAuthority*):
- Pengguna yang masuk membawa daftar authority berawalan `ROLE_`.
- URL privat dipagari menggunakan metode `.hasRole("NAMA_ROLE")`.
- Jika pengguna mencoba mengakses URL di luar kewenangannya, Spring Security membangkitkan `AccessDeniedException` yang menghasilkan status **HTTP 403 Forbidden**.

### 2.4 Keamanan Penyimpanan Kredensial Mobile (AES256 & Android Keystore)
Penyimpanan token JWT dan fitur "Ingat Saya" di perangkat Android menggunakan pustaka `androidx.security.crypto:security-crypto`:
- **MasterKey:** Dibuat menggunakan algoritma `AES256_GCM` yang disimpan dalam chip keamanan perangkat (Android Keystore Provider) dan tidak dapat diekstrak oleh aplikasi lain atau proses *rooting*.
- **PrefKeyEncryptionScheme.AES256_SIV:** Mengenkripsi nama kunci (*key*) agar tidak dapat dibaca dari file XML.
- **PrefValueEncryptionScheme.AES256_GCM:** Mengenkripsi nilai (*value*) token dan kata sandi dengan autentikasi integritas data.

### 2.5 Layanan Email Transaksional & Pola Asinkronus (@Async)
Proses handshaking dan pengiriman email melalui protokol SMTP internet umumnya membutuhkan waktu 1,5 hingga 3 detik. Jika dieksekusi secara sinkron (*blocking*), thread HTTP worker server akan tertahan dan menurunkan performa API.
Dengan memanfaatkan anotasi `@EnableAsync` dan `@Async`, fungsi pengiriman email dilempar ke thread pool latar belakang terpisah, sehingga endpoint API pengiriman OTP dapat memberikan respons instan kepada pengguna dalam waktu kurang dari 100 milidetik.

---

## BAB III: DESAIN & PERANCANGAN SISTEM

### 3.1 Diagram Alur Autentikasi & Penerbitan JWT (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Pengguna as Pengguna (Warga / Dinas / Admin)
    participant Android as Aplikasi Android (PinPoint)
    participant SecStorage as EncryptedSharedPreferences
    participant Backend as Spring Boot (AuthController)
    participant Filter as AuthTokenFilter
    participant DB as Basis Data PostgreSQL

    Pengguna->>Android: Input Email/No.HP & Password
    Pengguna->>Android: Centang "Ingat Saya" (Opsional)
    Android->>Backend: POST /api/auth/login {identifier, password}
    Backend->>DB: Query User by Email or Phone
    DB-->>Backend: Data Pengguna (Password BCrypt & idRole)
    Backend->>Backend: Verifikasi BCrypt Matcher & Status ACTIVE
    Backend->>Backend: Generate JWT HS512 (Subject: email, Claim: role)
    Backend-->>Android: HTTP 200 OK {token, type, email, nama, role}
    Android->>SecStorage: Simpan Token JWT & Data Sesi (AES256)
    opt Jika "Ingat Saya" Dicentang
        Android->>SecStorage: Simpan Kredensial Login (Auto-fill)
    end
    Android->>Pengguna: Navigasi ke DashboardActivity
```

---

### 3.2 Diagram Alur Validasi Otorisasi RBAC & Filter Chain

```mermaid
sequenceDiagram
    autonumber
    participant Klien as Klien Android / Postman
    participant Filter as AuthTokenFilter
    participant SecCtx as SecurityContextHolder
    participant Guard as URL Authorization (WebSecurityConfig)
    participant Endpoint as Target REST Controller
    participant EntryPoint as AuthEntryPointJwt (HTTP 401)
    participant AccessDenied as AuthAccessDeniedHandler (HTTP 403)

    Klien->>Filter: HTTP Request + Header "Authorization: Bearer <token>"
    alt Token Tidak Ada / Format Salah / Kedaluwarsa
        Filter->>EntryPoint: Autentikasi Gagal
        EntryPoint-->>Klien: HTTP 401 Unauthorized (JSON Response)
    else Token Sah (Valid)
        Filter->>Filter: Parse Subject (Email) & Claim (Role)
        Filter->>SecCtx: Set Authentication (Principal: email, Authority: ROLE_<role>)
        Filter->>Guard: Lanjutkan Filter Chain
        alt Akses Sesuai Role (Misal: WARGA akses /api/user/**)
            Guard->>Endpoint: Teruskan ke Controller
            Endpoint-->>Klien: HTTP 200 OK (Data Response)
        else Akses Melanggar Role (Misal: WARGA akses /api/admin/**)
            Guard->>AccessDenied: Lempar AccessDeniedException
            AccessDenied-->>Klien: HTTP 403 Forbidden (JSON Response)
        end
    end
```

---

### 3.3 Diagram Alur Pemulihan Akun via Email OTP (Flowchart)

```mermaid
flowchart TD
    Start([Mulai: Lupa Kata Sandi]) --> InputIdentitas[Pengguna Memasukkan Email Terdaftar]
    InputIdentitas --> RequestOTP[Kirim POST /api/auth/check-account]
    RequestOTP --> CekDB{Apakah Email Terdaftar di DB?}
    
    CekDB -- Tidak --> Ret400[Kembalikan HTTP 400 Bad Request]
    Ret400 --> TampilPeringatan[Tampilkan Peringatan di Layar]
    
    CekDB -- Ya --> GenOTP[Generate 6-Digit OTP & Simpan TTL 15 Menit]
    GenOTP --> AsyncEmail[Thread Async: Kirim Email HTML via SMTP]
    AsyncEmail --> Ret200[Kembalikan HTTP 200 OK]
    Ret200 --> BukaLayarOTP[Buka Layar OtpActivity di Android]
    
    BukaLayarOTP --> InputOTP[Pengguna Memasukkan 6 Digit OTP]
    InputOTP --> VerifyOTP[Kirim POST /api/auth/verify-otp]
    VerifyOTP --> CekValidOTP{Kode Cocok & Waktu <= 15 Menit?}
    
    CekValidOTP -- Tidak --> ErrOTP[Tampilkan Error & Bersihkan Kolom OTP]
    ErrOTP --> InputOTP
    
    CekValidOTP -- Ya --> DelOTP[Hapus OTP dari Memory: One-time Use]
    DelOTP --> BukaLayarReset[Buka Layar ResetPasswordActivity]
    BukaLayarReset --> InputSandiBaru[Masukkan Sandi Baru Min 6 Karakter]
    InputSandiBaru --> SubmitReset[Kirim POST /api/auth/forgot-password]
    SubmitReset --> HashBCrypt[Hash Sandi Baru dengan BCrypt & Update DB]
    HashBCrypt --> Selesai([Selesai: Redirect ke Layar Login])
```

---

### 3.4 Desain Basis Data & Struktur Entitas Pengguna

Skema tabel `users` dan `role` pada PostgreSQL:

```sql
-- 1. Tabel Master Peran (Role)
CREATE TABLE public.role (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nama_role VARCHAR(50) NOT NULL,
    deskripsi VARCHAR(255)
);

-- Data Awal Peran Sistem:
-- id: 1 -> ADMIN (Administrator Sistem)
-- id: 2 -> WARGA (Masyarakat Umum / Pelapor Fasilitas)
-- id: 3 -> DINAS (Pihak Dinas / Instansi Pemerintah)

-- 2. Tabel Pengguna (Users)
CREATE TABLE public.users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_role BIGINT REFERENCES public.role(id),
    nama VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    no_telepon VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    status_akun VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    tanggal_daftar TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    terakhir_login TIMESTAMP
);
```

---

### 3.5 Spesifikasi Kontrak RESTful API (OpenAPI v3 / Swagger)

Dokumentasi interaktif dapat diakses pada running server melalui:  
`http://localhost:8081/swagger-ui/index.html`

| Endpoint | Method | Izin Akses | Deskripsi Fungsional |
|---|:---:|:---:|---|
| `/api/auth/register` | `POST` | `permitAll` | Pendaftaran akun Warga baru (`idRole = 2`). |
| `/api/auth/login` | `POST` | `permitAll` | Login multi-identifier (email/no. hp) → menerbitkan JWT. |
| `/api/auth/check-account` | `POST` | `permitAll` | Verifikasi kepemilikan akun dan memicu pengiriman OTP ke email. |
| `/api/auth/verify-otp` | `POST` | `permitAll` | Validasi 6-digit kode OTP (berlaku 15 menit, *single-use*). |
| `/api/auth/forgot-password` | `POST` | `permitAll` | Penetapan kata sandi baru (hash BCrypt). |
| `/api/auth/refresh-token` | `POST` | `permitAll` | Pengecekan keabsahan dan masa aktif token JWT. |
| `/api/auth/logout` | `POST` | `permitAll` | Konfirmasi logout bagi pembersihan token sisi klien. |
| `/api/categories/**` | `GET` | `permitAll` | Membaca daftar master kategori fasilitas umum. |
| `/api/categories/**` | `POST/PUT/DELETE` | `ROLE_ADMIN` | Modifikasi data master kategori. |
| `/api/admin/**` | `ALL` | `ROLE_ADMIN` | Manajemen seluruh laporan dan akun pengguna sistem. |
| `/api/government/**` | `ALL` | `ROLE_DINAS` | Penanganan laporan dinas dan unggah bukti perbaikan. |
| `/api/user/**` | `ALL` | `ROLE_WARGA` | Pembuatan laporan warga dan pemantauan riwayat status. |

---

## BAB IV: IMPLEMENTASI SISTEM

### 4.1 Implementasi Backend (Spring Boot 4.1.1 LTS)

#### 1. Konfigurasi Rantai Filter Keamanan ([`WebSecurityConfig.java`](file:///Users/muhammadfathirsyabhan/Documents/ppl%20kelopok/PinPoint/PinPoint_backend/src/main/java/com/kelompok8/config/WebSecurityConfig.java))
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private AuthAccessDeniedHandler accessDeniedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/government/**").hasRole("DINAS")
                .requestMatchers("/api/user/**").hasRole("WARGA")
                .anyRequest().authenticated()
            );

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

#### 2. Penanganan Terpusat Error 401 dan 403
- **[`AuthEntryPointJwt.java`](file:///Users/muhammadfathirsyabhan/Documents/ppl%20kelopok/PinPoint/PinPoint_backend/src/main/java/com/kelompok8/security/AuthEntryPointJwt.java):** Menangani ketiadaan token atau token yang kedaluwarsa dengan mengembalikan JSON:
  ```json
  {"status":401,"error":"Unauthorized","message":"Akses ditolak: Token JWT tidak ada, tidak valid, atau sudah kedaluwarsa.","path":"..."}
  ```
- **[`AuthAccessDeniedHandler.java`](file:///Users/muhammadfathirsyabhan/Documents/ppl%20kelopok/PinPoint/PinPoint_backend/src/main/java/com/kelompok8/security/AuthAccessDeniedHandler.java):** Menangani pelanggaran batas wewenang role dengan mengembalikan JSON:
  ```json
  {"status":403,"error":"Forbidden","message":"Akses ditolak: Anda tidak memiliki izin / hak akses untuk resource ini.","path":"..."}
  ```

#### 3. Logika Verifikasi Token ([`AuthTokenFilter.java`](file:///Users/muhammadfathirsyabhan/Documents/ppl%20kelopok/PinPoint/PinPoint_backend/src/main/java/com/kelompok8/security/AuthTokenFilter.java))
```java
String jwt = parseJwt(request);
if (jwt != null && jwtUtils.validateToken(jwt)) {
    String email = jwtUtils.getEmailFromToken(jwt);
    String role = jwtUtils.getRoleFromToken(jwt);

    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isPresent() && role != null && !role.isBlank()) {
        String springRole = "ROLE_" + role.toUpperCase();
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(springRole));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
```

---

### 4.2 Implementasi Frontend Mobile (Android Kotlin Native)

#### 1. Manajemen Sesi & Enkripsi Kredensial ([`TokenManager.kt`](file:///Users/muhammadfathirsyabhan/Documents/ppl%20kelopok/PinPoint/PinPoint_frontend/app/src/main/java/com/example/pinpoint/TokenManager.kt))
```kotlin
object TokenManager {
    private const val PREFS_FILE_NAME = "pinpoint_secure_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(context: Context, token: String, nama: String, email: String, role: String) {
        getPrefs(context).edit().apply {
            putString("jwt_token", token)
            putBoolean("is_logged_in", true)
            putString("user_name", nama)
            putString("user_email", email)
            putString("user_role", role)
            apply()
        }
    }

    fun saveRememberMe(context: Context, identifier: String, password: String) {
        getPrefs(context).edit().apply {
            putBoolean("remember_me", true)
            putString("saved_identifier", identifier)
            putString("saved_password", password)
            apply()
        }
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().apply {
            remove("jwt_token")
            remove("is_logged_in")
            remove("user_name")
            remove("user_email")
            remove("user_role")
            apply()
        }
    }
}
```

#### 2. Logika Auto-Advance & Reverse Kursor OTP ([`OtpActivity.kt`](file:///Users/muhammadfathirsyabhan/Documents/ppl%20kelopok/PinPoint/PinPoint_frontend/app/src/main/java/com/example/pinpoint/OtpActivity.kt))
```kotlin
otpFields.forEachIndexed { index, editText ->
    editText.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            // Melompat maju ke kotak berikutnya saat 1 digit diketik
            if (s?.length == 1 && index < otpFields.size - 1) {
                otpFields[index + 1].requestFocus()
            }
            // Mundur ke kotak sebelumnya saat tombol backspace ditekan pada kotak kosong
            if (s?.isEmpty() == true && index > 0) {
                otpFields[index - 1].requestFocus()
            }
        }
    })
}
```

---

### 4.3 Matriks Berkas yang Dibuat & Diperbarui

| No | Modul | File Path | Tipe Berkas | Status & Kontribusi |
|:---:|---|---|:---:|---|
| 1 | Backend | `.../security/WebSecurityConfig.java` | Java Config | Konfigurasi inti Spring Security stateless & RBAC. |
| 2 | Backend | `.../security/JwtUtils.java` | Java Component | Utilitas HMAC-SHA512 token & claim parsing. |
| 3 | Backend | `.../security/AuthTokenFilter.java` | Java Filter | OncePerRequestFilter & authority prefix `ROLE_`. |
| 4 | Backend | `.../security/AuthEntryPointJwt.java` | Java Component | Custom error handler HTTP 401 Unauthorized JSON. |
| 5 | Backend | `.../security/AuthAccessDeniedHandler.java` | Java Component | Custom error handler HTTP 403 Forbidden JSON. |
| 6 | Backend | `.../controller/AuthController.java` | Java Controller | REST Controller lengkap siklus otentikasi & Swagger docs. |
| 7 | Backend | `.../service/AuthService.java` | Java Service | Registrasi, verifikasi BCrypt, dan in-memory OTP engine. |
| 8 | Backend | `.../service/EmailService.java` | Java Service | Layanan SMTP `@Async` & template HTML inline responsif. |
| 9 | Backend | `.../dto/RegisterRequest.java` | Java DTO | Data transfer object registrasi warga. |
| 10 | Backend | `.../dto/OtpVerifyRequest.java` | Java DTO | Data transfer object verifikasi 6 digit OTP. |
| 11 | Backend | `.../dto/ApiResponse.java` | Java DTO | Standar format respons API `{success, message, data}`. |
| 12 | Backend | `.../model/User.java` | Java Entity | Penyelarasan pemetaan `getRole()` (1:ADMIN, 2:WARGA, 3:DINAS). |
| 13 | Backend | `.../PinPointApplication.java` | Java Main App | Penambahan anotasi `@EnableAsync`. |
| 14 | Mobile | `.../res/layout/activity_login.xml` | Android XML | Desain UI Login Material Design 3 (OutlinedBox). |
| 15 | Mobile | `.../res/layout/activity_register.xml` | Android XML | Desain UI Register MD3 dengan validasi visual. |
| 16 | Mobile | `.../res/layout/activity_otp.xml` | Android XML | Desain UI verifikasi 6 kotak OTP terpisah. |
| 17 | Mobile | `.../res/layout/activity_reset_password.xml` | Android XML | Desain UI reset kata sandi baru MD3. |
| 18 | Mobile | `.../res/drawable/bg_otp_box.xml` | Android Drawable | Selector status kotak input OTP (fokus & default). |
| 19 | Mobile | `.../java/.../TokenManager.kt` | Kotlin Object | Penyimpanan aman `EncryptedSharedPreferences` (AES256). |
| 20 | Mobile | `.../java/.../AuthRepository.kt` | Kotlin Class | Abstraksi repository API Auth dengan callback bersih. |
| 21 | Mobile | `.../java/.../ApiService.kt` | Kotlin Interface | Kontrak Retrofit, DTO request/response, dan OkHttp logging. |
| 22 | Mobile | `.../java/.../Login.kt` | Kotlin Activity | Integrasi login, remember me auto-fill, dan auto-redirect. |
| 23 | Mobile | `.../java/.../Register.kt` | Kotlin Activity | Validasi pendaftaran dan password matcher real-time. |
| 24 | Mobile | `.../java/.../ForgotPasswordActivity.kt` | Kotlin Activity | Pengecekan akun dan pengalihan ke OtpActivity. |
| 25 | Mobile | `.../java/.../OtpActivity.kt` | Kotlin Activity | Auto-advance 6 kotak OTP, timer 15m, dan verifikasi OTP. |
| 26 | Mobile | `.../java/.../ResetPasswordActivity.kt` | Kotlin Activity | Penetapan kata sandi baru dan reset activity stack. |
| 27 | Mobile | `.../java/.../MainActivity.kt` | Kotlin Activity | Sinkronisasi auto-login membaca `TokenManager.isLoggedIn()`. |
| 28 | Mobile | `.../java/.../DashboardActivity.kt` | Kotlin Activity | Sinkronisasi tombol logout memanggil `TokenManager.clearSession()`. |
| 29 | Mobile | `.../app/build.gradle.kts` | Gradle Config | Penambahan dependensi `security-crypto` & `logging-interceptor`. |
| 30 | Mobile | `.../AndroidManifest.xml` | Android Manifest | Pendaftaran activity baru dalam siklus navigasi aplikasi. |

---

## BAB V: HASIL PENGUJIAN & EVALUASI

### 5.1 Hasil Pengujian Otomatis Live Backend (22/22 Pass)
Pengujian dijalankan langsung terhadap instance server Spring Boot aktif pada port 8081 (`http://localhost:8081`) menggunakan automated script Python. Seluruh 22 skenario berhasil lulus 100%:

| No | Target Endpoint | Method | Parameter Uji | Ekspektasi | Status Aktual | Latensi | Hasil Evaluasi |
|:---:|---|:---:|---|:---:|:---:|:---:|---|
| 1 | `/swagger-ui/index.html` | `GET` | Tanpa Autentikasi | `200 OK` | **`200 OK`** | 6.03 ms | UI Swagger ter-render sempurna. |
| 2 | `/v3/api-docs` | `GET` | Tanpa Autentikasi | `200 OK` | **`200 OK`** | 119.31 ms | OpenAPI schema JSON valid. |
| 3 | `/api/categories` | `GET` | Endpoint Publik | `200 OK` | **`200 OK`** | 276.00 ms | Kategori publik terbaca tanpa token. |
| 4 | `/api/auth/register` | `POST` | Data Akun Warga Baru | `201 Created` | **`201 Created`** | 383.88 ms | Berhasil registrasi & hash BCrypt. |
| 5 | `/api/auth/register` | `POST` | Email Terdaftar (Duplikat) | `400 Bad Request` | **`400 Bad Request`** | 73.67 ms | Pesan: *"Error: Email sudah terdaftar!"*. |
| 6 | `/api/auth/register` | `POST` | Password < 6 Karakter | `400 Bad Request` | **`400 Bad Request`** | 1.59 ms | Validasi panjang kata sandi aktif. |
| 7 | `/api/auth/register` | `POST` | Nama Lengkap Kosong | `400 Bad Request` | **`400 Bad Request`** | 1.31 ms | Validasi input kosong aktif. |
| 8 | `/api/auth/login` | `POST` | Identifier Email Valid | `200 OK` | **`200 OK`** | 299.23 ms | Terbit token JWT dengan role WARGA. |
| 9 | `/api/auth/login` | `POST` | Identifier Nomor Ponsel | `200 OK` | **`200 OK`** | 295.55 ms | Login hybrid berhasil. |
| 10 | `/api/auth/login` | `POST` | Kata Sandi Salah | `401 Unauthorized` | **`401 Unauthorized`** | 198.37 ms | Akses ditolak karena kredensial keliru. |
| 11 | `/api/user/reports` | `GET` | Header Token JWT Valid | `200 OK` | **`200 OK`** | 185.07 ms | Endpoint privat warga berhasil diakses. |
| 12 | `/api/user/reports` | `GET` | Tanpa Header Authorization | `401 Unauthorized` | **`401 Unauthorized`** | 1.67 ms | Ditangkap oleh `AuthEntryPointJwt`. |
| 13 | `/api/user/reports` | `GET` | Token Palsu / Tampered | `401 Unauthorized` | **`401 Unauthorized`** | 1.56 ms | Signature error terdeteksi. |
| 14 | `/api/admin/reports` | `GET` | Token Pengguna Role WARGA | `403 Forbidden` | **`403 Forbidden`** | 74.60 ms | Penegakan RBAC isolasi admin sukses. |
| 15 | `/api/government/reports` | `GET` | Token Pengguna Role WARGA | `403 Forbidden` | **`403 Forbidden`** | 78.66 ms | Penegakan RBAC isolasi dinas sukses. |
| 16 | `/api/categories` | `POST` | Token Pengguna Role WARGA | `403 Forbidden` | **`403 Forbidden`** | 84.15 ms | Mutasi master dibatasi khusus ADMIN. |
| 17 | `/api/auth/refresh-token` | `POST` | Header Token JWT Aktif | `200 OK` | **`200 OK`** | 81.41 ms | Sesi terverifikasi masih aktif. |
| 18 | `/api/auth/refresh-token` | `POST` | Header Token Rusak | `401 Unauthorized` | **`401 Unauthorized`** | 1.93 ms | Sesi kedaluwarsa terdeteksi. |
| 19 | `/api/auth/check-account` | `POST` | Email Akun Terdaftar | `200 OK` | **`200 OK`** | 71.87 ms | Email OTP terkirim asinkron. |
| 20 | `/api/auth/check-account` | `POST` | Email Tidak Terdaftar | `400 Bad Request` | **`400 Bad Request`** | 70.00 ms | Akun tidak ditemukan. |
| 21 | `/api/auth/verify-otp` | `POST` | Kode OTP Salah (`000000`) | `400 Bad Request` | **`400 Bad Request`** | 1.67 ms | Penolakan kode OTP keliru. |
| 22 | `/api/auth/logout` | `POST` | Request Logout Klien | `200 OK` | **`200 OK`** | 0.91 ms | Instruksi hapus token sukses. |

### 5.2 Pengujian Penanganan Error Keamanan (401 & 403 JSON)
Server telah diverifikasi mengembalikan struktur JSON yang informatif saat terjadi insiden keamanan:

1. **Kasus 401 Unauthorized (Tanpa Token):**
   ```http
   HTTP/1.1 401 Unauthorized
   Content-Type: application/json

   {
     "status": 401,
     "error": "Unauthorized",
     "message": "Akses ditolak: Token JWT tidak ada, tidak valid, atau sudah kedaluwarsa.",
     "path": "/api/user/reports"
   }
   ```

2. **Kasus 403 Forbidden (Pelanggaran Hak Akses RBAC):**
   ```http
   HTTP/1.1 403 Forbidden
   Content-Type: application/json

   {
     "status": 403,
     "error": "Forbidden",
     "message": "Akses ditolak: Anda tidak memiliki izin / hak akses untuk resource ini.",
     "path": "/api/admin/reports"
   }
   ```

### 5.3 Pengujian Antarmuka & Logika Mobile
- **Kompilasi Gradle:** Perintah `./gradlew compileDebugKotlin` berhasil dieksekusi dengan status **`BUILD SUCCESSFUL`** dalam 1 detik.
- **Auto-Advance & Reverse OTP:** Kursor fokus berpindah otomatis ke kotak kanan saat 1 angka diketik dan berpindah mundur ke kotak kiri saat tombol hapus ditekan.
- **Sinkronisasi Sesi:** Pengecekan sesi pada `MainActivity` dan tombol logout pada `DashboardActivity` telah diselaraskan penuh dengan `TokenManager`, mencegah terjadinya *infinite redirect loop*.

---

## BAB VI: KESIMPULAN & REKOMENDASI

### 6.1 Kesimpulan
Seluruh target luaran dan kompetensi pada **Tupoksi Orang 1 (Modul Auth & Security)** telah berhasil diselesaikan secara paripurna:
1. Arsitektur keamanan Spring Security telah mengadopsi standar industri modern berbasis stateless JWT dan isolasi hak akses 3-peran (`ROLE_WARGA`, `ROLE_ADMIN`, `ROLE_DINAS`).
2. Layanan email pemulihan akun telah berjalan stabil dengan fitur non-blocking `@Async` dan desain email HTML responsif.
3. Aplikasi mobile telah memiliki UI Material Design 3 yang intuitif serta perlindungan token dan kredensial menggunakan enkripsi tingkat militer `EncryptedSharedPreferences` (AES256).
4. Hasil pengujian menunjukkan tingkat keberhasilan **100% Pass** pada 22 skenario uji coba nyata.

### 6.2 Rekomendasi Integrasi untuk Anggota Tim Lain
Bagi rekan pengembang (Orang 2 dan Orang 3) yang akan mengintegrasikan modul pelaporan fasilitas umum dan dashboard monitoring:
1. **Mengakses API Privat dari Mobile:**  
   Ambil header Authorization yang siap pakai menggunakan fungsi pembantu:
   ```kotlin
   val authHeader = TokenManager.getBearerToken(context) // Mengembalikan "Bearer <token_jwt>"
   ```
2. **Mengambil Identitas Pengguna di Backend Controller:**  
   Gunakan objek `Authentication` bawaan Spring Security untuk memperoleh email pelapor tanpa perlu query tambahan:
   ```java
   @PostMapping("/reports")
   public ResponseEntity<?> submitReport(Authentication authentication, @RequestBody Laporan laporan) {
       String emailPelapor = authentication.getName();
       // ... logika penyimpanan laporan
   }
   ```
3. **Penyimpanan OTP Jangka Panjang (Produksi):**  
   Untuk skala produksi dengan load tinggi, penyimpanan in-memory OTP (`ConcurrentHashMap` di `AuthService`) dapat dihubungkan ke Redis cluster dengan mekanisme Time-To-Live (TTL) otomatis.

---
*Laporan ini disusun secara resmi sebagai bukti pertanggungjawaban pengembangan perangkat lunak Modul Auth & Security Proyek PinPoint Kelompok 8.*
