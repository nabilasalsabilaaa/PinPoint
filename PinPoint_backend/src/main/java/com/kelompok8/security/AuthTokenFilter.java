package com.kelompok8.security;

import com.kelompok8.model.User;
import com.kelompok8.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filter JWT yang dijalankan sekali per request.
 * Mengekstrak token dari header Authorization, memvalidasi, lalu
 * menetapkan autentikasi ke SecurityContext dengan role yang sesuai:
 * ROLE_WARGA, ROLE_ADMIN, atau ROLE_DINAS.
 */
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateToken(jwt)) {
                String email = jwtUtils.getEmailFromToken(jwt);
                // Role dari token: WARGA, ADMIN, atau DINAS (tanpa prefix "ROLE_")
                String role = jwtUtils.getRoleFromToken(jwt);

                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent() && role != null && !role.isBlank()) {
                    // Tambahkan prefix "ROLE_" agar sesuai konvensi Spring Security
                    String springRole = "ROLE_" + role.toUpperCase();
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority(springRole));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Autentikasi berhasil untuk user '{}' dengan role '{}'", email, springRole);
                }
            }
        } catch (Exception e) {
            logger.error("Tidak dapat menetapkan autentikasi pengguna: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Mengekstrak JWT dari header Authorization (format: "Bearer <token>").
     *
     * @param request HTTP request masuk
     * @return JWT string, atau null jika tidak ada / format salah
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
