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
                .anyRequest().permitAll()
                // .requestMatchers(
                //     "/api/auth/**",
                //     "/swagger-ui/**",
                //     "/swagger-ui.html",
                //     "/v3/api-docs/**",
                //     "/webjars/**"
                // ).permitAll()
                // .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                // .requestMatchers(HttpMethod.GET, "/api/user/reports/map").permitAll()

                // .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // .requestMatchers("/api/government/**").hasRole("PEMERINTAH")
                // .requestMatchers("/api/user/**").hasRole("USER")

                // // 3. Aturan manajemen Kategori (hanya ADMIN untuk tulis/ubah/hapus)
                // .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                // .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                // .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                // // 4. Endpoint lainnya wajib membawa Token JWT yang sah
                // .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}