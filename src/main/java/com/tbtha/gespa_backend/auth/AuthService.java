package com.tbtha.gespa_backend.auth;

import com.tbtha.gespa_backend.dtos.LoginRequest;
import com.tbtha.gespa_backend.dtos.LoginResponse;
import com.tbtha.gespa_backend.dtos.MeResponse;
import com.tbtha.gespa_backend.entities.RefreshToken;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.RefreshTokenRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import com.tbtha.gespa_backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AccessControlService accessControlService;
    private final long refreshExpirationSeconds;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       AccessControlService accessControlService,
                       @Value("${app.jwt.refresh-expiration-seconds:1209600}") long refreshExpirationSeconds) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.accessControlService = accessControlService;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new ConflictException("Usuario inactivo");
        }

        usuario.setUltimoLogin(OffsetDateTime.now());

        String accessToken = jwtService.generateAccessToken(usuario);
        String refreshToken = createAndPersistRefreshToken(usuario);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                usuario.getId(),
                usuario.getEmail(),
                usuario.getDisplayName(),
                usuario.getRole()
        );
    }

    @Transactional
    public LoginResponse refresh(String plainRefreshToken) {
        String tokenHash = hashToken(plainRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token inválido"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ConflictException("Refresh token expirado o revocado");
        }

        stored.setRevoked(true);

        Usuario usuario = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(usuario);
        String newRefreshToken = createAndPersistRefreshToken(usuario);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                usuario.getId(),
                usuario.getEmail(),
                usuario.getDisplayName(),
                usuario.getRole()
        );
    }

    @Transactional
    public void logout(String plainRefreshToken) {
        String tokenHash = hashToken(plainRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        Usuario actor = accessControlService.currentUsuario();
        return new MeResponse(
                actor.getId(),
                actor.getEmail(),
                actor.getDisplayName(),
                actor.getRole(),
                actor.getActive()
        );
    }

    private String createAndPersistRefreshToken(Usuario usuario) {
        String plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken token = new RefreshToken();
        token.setUser(usuario);
        token.setTokenHash(hashToken(plainToken));
        token.setExpiresAt(OffsetDateTime.now().plusSeconds(refreshExpirationSeconds));
        token.setRevoked(false);
        refreshTokenRepository.save(token);

        return plainToken;
    }

    private String hashToken(String plainToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo procesar refresh token", e);
        }
    }
}
