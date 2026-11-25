package com.kelompok8.config;

import com.kelompok8.security.AuthTokenFilter;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. Endpoint publik (Autentikasi, Swagger, File Media, Peta & Baca Kategori)
                .requestMatchers(
                    "/api/auth/**",
                    "/api/files/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/reports/map").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/reports/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/reports/*/evidence").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/reports/*/status-history").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/reports").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/reports/*").permitAll()

                // 2. Endpoint profil & ubah sandi (boleh diakses oleh seluruh role yang terautentikasi)
                .requestMatchers("/api/admin/profile", "/api/admin/change-password").authenticated()

                // 3. Otorisasi Role ADMIN (verifikasi, penolakan, kelola akun, statistik, CRUD kategori)
                .requestMatchers("/api/admin/dashboard/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/reports/*/verify", "/api/admin/reports/*/approve", "/api/admin/reports/*/reject").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                // 4. Otorisasi Role PEMERINTAH (dan ADMIN untuk disposisi/monitoring)
                .requestMatchers("/api/government/**").hasAnyRole("PEMERINTAH", "DINAS", "ADMIN")

                // 5. Otorisasi Role USER & endpoint lainnya wajib membawa Token JWT yang sah
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN", "PEMERINTAH")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}