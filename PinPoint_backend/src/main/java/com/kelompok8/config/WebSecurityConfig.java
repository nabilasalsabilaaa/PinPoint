package com.kelompok8.config;

import com.kelompok8.security.AuthAccessDeniedHandler;
import com.kelompok8.security.AuthEntryPointJwt;
import com.kelompok8.security.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Konfigurasi keamanan Spring Security berbasis JWT (stateless).
 *
 * <p>Role yang didukung:
 * <ul>
 *   <li>ROLE_WARGA  – pengguna masyarakat biasa</li>
 *   <li>ROLE_ADMIN  – administrator sistem</li>
 *   <li>ROLE_DINAS  – petugas dinas/pemerintah</li>
 * </ul>
 *
 * <p>Mapping endpoint:
 * <ul>
 *   <li>/api/auth/**        → permitAll (publik)</li>
 *   <li>/swagger-ui/**      → permitAll (dokumentasi API)</li>
 *   <li>/v3/api-docs/**     → permitAll (dokumentasi API)</li>
 *   <li>/api/admin/**       → ROLE_ADMIN saja</li>
 *   <li>/api/government/**  → ROLE_DINAS saja</li>
 *   <li>/api/user/**        → ROLE_WARGA saja</li>
 *   <li>GET /api/categories/** → permitAll</li>
 *   <li>POST/PUT/DELETE /api/categories/** → ROLE_ADMIN</li>
 *   <li>Lainnya            → terautentikasi (semua role)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private AuthAccessDeniedHandler accessDeniedHandler;

    /**
     * Mendaftarkan AuthTokenFilter sebagai Spring Bean.
     * Filter ini membaca dan memvalidasi JWT dari setiap request.
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    /**
     * Mendaftarkan BCryptPasswordEncoder untuk hashing password.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Mendefinisikan rantai filter keamanan (SecurityFilterChain).
     * Aplikasi bersifat stateless – tidak menggunakan sesi HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Nonaktifkan CSRF karena menggunakan JWT (stateless)
            .csrf(csrf -> csrf.disable())

            // Tangani error 401 Unauthorized dan 403 Forbidden dengan respons JSON
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler))

            // Stateless: tidak menyimpan sesi di server
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Nonaktifkan form login dan HTTP Basic (tidak digunakan)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Aturan otorisasi endpoint
            .authorizeHttpRequests(auth -> auth

                // ── Endpoint publik (tidak perlu token) ──────────────────────────
                .requestMatchers("/api/auth/**", "/error").permitAll()
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()

                // ── Kategori: GET publik, mutasi hanya ADMIN ──────────────────────
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                // ── Endpoint khusus role ──────────────────────────────────────────
                // ADMIN: manajemen pengguna, laporan semua, konfigurasi sistem
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // DINAS: endpoint laporan yang ditugaskan ke instansi pemerintah
                .requestMatchers("/api/government/**").hasRole("DINAS")

                // WARGA: endpoint laporan milik pengguna (buat, lihat, tracking)
                .requestMatchers("/api/user/**").hasRole("WARGA")

                // ── Semua endpoint lain wajib terautentikasi (semua role valid) ───
                .anyRequest().authenticated()
            );

        // Pasang JWT filter sebelum UsernamePasswordAuthenticationFilter bawaan
        http.addFilterBefore(
            authenticationJwtTokenFilter(),
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
