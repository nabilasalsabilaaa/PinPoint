package com.kelompok8.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utilitas JWT untuk membuat, mem-parsing, dan memvalidasi token JWT.
 * Token menyertakan klaim "role" dengan nilai: WARGA, ADMIN, atau DINAS.
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // Secret key minimal 512-bit untuk algoritma HS512
    private final String jwtSecretString = "PinPointBackendSecretKeyForJwtAuthenticationSecurity2026SecureKey!";
    private final Key jwtSecret = Keys.hmacShaKeyFor(jwtSecretString.getBytes());

    // Masa berlaku token: 24 jam (dalam milidetik)
    private final int jwtExpirationMs = 86400000;

    /**
     * Membuat JWT token berdasarkan email dan role pengguna.
     *
     * @param email email pengguna (digunakan sebagai subject)
     * @param role  role pengguna: WARGA, ADMIN, atau DINAS (tanpa prefix "ROLE_")
     * @return JWT token string yang telah ditandatangani
     */
    public String generateJwtToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(jwtSecret)
                .compact();
    }

    /**
     * Mengambil email (subject) dari JWT token.
     *
     * @param token JWT token string
     * @return email yang tersimpan sebagai subject token
     */
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Mengambil role dari klaim JWT token.
     *
     * @param token JWT token string
     * @return nilai role (contoh: "WARGA", "ADMIN", "DINAS")
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    /**
     * Memvalidasi JWT token: memeriksa tanda tangan dan masa berlaku.
     *
     * @param authToken JWT token string
     * @return true jika valid, false jika tidak valid atau kedaluwarsa
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Token JWT tidak valid (malformed): {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Token JWT telah kedaluwarsa: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT tidak didukung: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string kosong: {}", e.getMessage());
        } catch (JwtException e) {
            logger.error("JWT error umum: {}", e.getMessage());
        }
        return false;
    }
}
