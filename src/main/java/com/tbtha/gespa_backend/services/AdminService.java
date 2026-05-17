package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.AdminCreateProfessionalInvitationRequest;
import com.tbtha.gespa_backend.dtos.AdminResetPasswordResponse;
import com.tbtha.gespa_backend.dtos.AdminUpdateUserStatusRequest;
import com.tbtha.gespa_backend.dtos.AdminUserResponse;
import com.tbtha.gespa_backend.dtos.ProfessionalInvitationResponse;
import com.tbtha.gespa_backend.entities.ProfessionalInvitationToken;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.RefreshToken;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.ProfessionalInvitationTokenRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.RefreshTokenRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final ProfesionalRepository profesionalRepository;
    private final ProfessionalInvitationTokenRepository invitationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final long invitationExpirationSeconds;

    public AdminService(UsuarioRepository usuarioRepository,
                        ProfesionalRepository profesionalRepository,
                        ProfessionalInvitationTokenRepository invitationTokenRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.auth.invitation.expiration-seconds:604800}") long invitationExpirationSeconds) {
        this.usuarioRepository = usuarioRepository;
        this.profesionalRepository = profesionalRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.invitationExpirationSeconds = invitationExpirationSeconds;
    }

    @Transactional
    public ProfessionalInvitationResponse createProfessionalInvitation(AdminCreateProfessionalInvitationRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe un usuario con el email indicado");
        }

        if (request.licenseNumber() != null && !request.licenseNumber().isBlank()
                && profesionalRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new ConflictException("Ya existe un profesional con ese número de registro");
        }

        if (profesionalRepository.existsByRut(request.rut())) {
            throw new ConflictException("Ya existe un profesional con ese RUT");
        }

        Usuario user = new Usuario();
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setRole(UserRole.PROFESSIONAL);
        user.setActive(false);
        user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
        usuarioRepository.save(user);

        Profesional profesional = new Profesional();
        profesional.setUsuario(user);
        profesional.setRut(request.rut());
        profesional.setSpecialty(resolveSpecialty(request.specialty()));
        profesional.setLicenseNumber(request.licenseNumber());
        profesional.setPhone(request.phone());
        profesional.setAddress(request.address());
        profesional.setInstitucion(request.institucion());
        profesional.setDescripcion(request.descripcion());
        profesionalRepository.save(profesional);

        String plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

        invitationTokenRepository.deleteByUser_Id(user.getId());

        ProfessionalInvitationToken token = new ProfessionalInvitationToken();
        token.setUser(user);
        token.setTokenHash(hashToken(plainToken));
        token.setExpiresAt(OffsetDateTime.now().plusSeconds(invitationExpirationSeconds));
        token.setUsed(false);
        invitationTokenRepository.save(token);

        return new ProfessionalInvitationResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                plainToken,
                token.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(user -> new AdminUserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole(),
                        user.getActive()
                ))
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, AdminUpdateUserStatusRequest request) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setActive(request.active());

        if (!Boolean.TRUE.equals(request.active())) {
            revokeActiveRefreshTokens(userId);
        }

        Usuario updated = usuarioRepository.save(user);
        return new AdminUserResponse(
                updated.getId(),
                updated.getEmail(),
                updated.getDisplayName(),
                updated.getRole(),
                updated.getActive()
        );
    }

    @Transactional
    public AdminResetPasswordResponse resetUserPassword(Long userId) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        usuarioRepository.save(user);

        revokeActiveRefreshTokens(userId);

        return new AdminResetPasswordResponse(
                user.getId(),
                user.getEmail(),
                temporaryPassword
        );
    }

    private void revokeActiveRefreshTokens(Long userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUser_IdAndRevokedFalse(userId);
        activeTokens.forEach(token -> token.setRevoked(true));
    }

    private String resolveSpecialty(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return "MEDICO GENERAL";
        }
        return specialty.trim();
    }

    private String generateTemporaryPassword() {
        String seed = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return seed + "A1!";
    }

    private String hashToken(String plainToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo procesar token", e);
        }
    }
}
