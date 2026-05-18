package com.tbtha.gespa_backend.security;

import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds:3600}") long accessTokenExpirationSeconds) {
        this.signingKey = buildSigningKey(secret);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public String generateAccessToken(Usuario usuario) {
        return generateAccessToken(usuario, usuario.getRole());
    }

    public String generateAccessToken(Usuario usuario, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claims(Map.of(
                        "uid", usuario.getId(),
                        "role", role.name()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UserRole extractRole(String token) {
        String role = parseClaims(token).get("role", String.class);
        return role == null ? null : UserRole.valueOf(role);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildSigningKey(String secret) {
        try {
            byte[] bytes = sha256(secret);
            return Keys.hmacShaKeyFor(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar la clave JWT", e);
        }
    }

    private byte[] sha256(String secret) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(secret.getBytes(StandardCharsets.UTF_8));
    }
}
