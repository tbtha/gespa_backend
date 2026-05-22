package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.AdminCreateProfessionalInvitationRequest;
import com.tbtha.gespa_backend.dtos.AdminResetPasswordResponse;
import com.tbtha.gespa_backend.dtos.AdminCreateSpecialtyRequest;
import com.tbtha.gespa_backend.dtos.AdminSpecialtyResponse;
import com.tbtha.gespa_backend.dtos.AdminUpdateUserStatusRequest;
import com.tbtha.gespa_backend.dtos.AdminUpdateSpecialtyStatusRequest;
import com.tbtha.gespa_backend.dtos.AdminUserResponse;
import com.tbtha.gespa_backend.dtos.CreatePatientInvitationRequest;
import com.tbtha.gespa_backend.dtos.PatientInvitationResponse;
import com.tbtha.gespa_backend.dtos.ProfessionalInvitationResponse;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.ProfessionalInvitationToken;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.RefreshToken;
import com.tbtha.gespa_backend.entities.Specialty;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfessionalInvitationTokenRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.RefreshTokenRepository;
import com.tbtha.gespa_backend.repositories.SpecialtyRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import com.tbtha.gespa_backend.utils.RutUtils;
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
    private final PacienteRepository pacienteRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ProfessionalInvitationTokenRepository invitationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final long invitationExpirationSeconds;

    public AdminService(UsuarioRepository usuarioRepository,
                        ProfesionalRepository profesionalRepository,
                        PacienteRepository pacienteRepository,
                        SpecialtyRepository specialtyRepository,
                        ProfessionalInvitationTokenRepository invitationTokenRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.auth.invitation.expiration-seconds:604800}") long invitationExpirationSeconds) {
        this.usuarioRepository = usuarioRepository;
        this.profesionalRepository = profesionalRepository;
        this.pacienteRepository = pacienteRepository;
        this.specialtyRepository = specialtyRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.invitationExpirationSeconds = invitationExpirationSeconds;
    }

    @Transactional
    public ProfessionalInvitationResponse createProfessionalInvitation(AdminCreateProfessionalInvitationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedRut = RutUtils.normalize(request.rut());
        Usuario existingUser = usuarioRepository.findByEmail(normalizedEmail).orElse(null);

        if (existingUser != null) {
            if (existingUser.getRole() != UserRole.PROFESSIONAL && existingUser.getRole() != UserRole.PATIENT) {
                throw new ConflictException("Ya existe un usuario con el email indicado");
            }
            if (profesionalRepository.existsById(existingUser.getId())) {
                throw new ConflictException("Ya existe un perfil profesional para ese email");
            }
        }

        if (profesionalRepository.existsByRut(normalizedRut)) {
            throw new ConflictException("Ya existe un profesional con ese RUT");
        }

        Usuario user;
        boolean shouldCreateInvitation;

        if (existingUser != null) {
            user = existingUser;
            if (request.displayName() != null && !request.displayName().isBlank()) {
                user.setDisplayName(request.displayName());
            }
            usuarioRepository.save(user);
            shouldCreateInvitation = !Boolean.TRUE.equals(user.getActive());
        } else {
            user = new Usuario();
            user.setEmail(normalizedEmail);
            user.setDisplayName(request.displayName());
            user.setRole(UserRole.PROFESSIONAL);
            user.setActive(false);
            user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
            usuarioRepository.save(user);
            shouldCreateInvitation = true;
        }

        Profesional profesional = new Profesional();
        Specialty selectedSpecialty = resolveSpecialty(request.specialty());
        profesional.setUsuario(user);
        profesional.setRut(normalizedRut);
        profesional.setSpecialtyRef(selectedSpecialty);
        profesional.setSpecialty(selectedSpecialty.getName());
        profesional.setPhone(request.phone());
        profesional.setAddress(request.address());
        profesional.setInstitucion(request.institucion());
        profesional.setDescripcion(request.descripcion());
        profesionalRepository.save(profesional);

        String plainToken = null;
        OffsetDateTime expiresAt = null;

        if (shouldCreateInvitation) {
            plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

            invitationTokenRepository.deleteByUser_Id(user.getId());

            ProfessionalInvitationToken token = new ProfessionalInvitationToken();
            token.setUser(user);
            token.setTokenHash(hashToken(plainToken));
            token.setExpiresAt(OffsetDateTime.now().plusSeconds(invitationExpirationSeconds));
            token.setUsed(false);
            invitationTokenRepository.save(token);
            expiresAt = token.getExpiresAt();
        }

        return new ProfessionalInvitationResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                plainToken,
            expiresAt
        );
    }

    @Transactional
    public PatientInvitationResponse createPatientInvitation(CreatePatientInvitationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedRut = RutUtils.normalize(request.rut());
        Usuario existingUser = usuarioRepository.findByEmail(normalizedEmail).orElse(null);

        if (existingUser != null) {
            if (existingUser.getRole() != UserRole.PROFESSIONAL && existingUser.getRole() != UserRole.PATIENT) {
                throw new ConflictException("Ya existe un usuario con el email indicado");
            }
            if (pacienteRepository.existsById(existingUser.getId())) {
                throw new ConflictException("Ya existe un perfil de paciente para ese email");
            }
        }

        if (pacienteRepository.existsByRut(normalizedRut)) {
            throw new ConflictException("Ya existe un paciente con ese RUT");
        }

        Usuario user;
        boolean shouldCreateInvitation;

        if (existingUser != null) {
            user = existingUser;
            if (request.displayName() != null && !request.displayName().isBlank()) {
                user.setDisplayName(request.displayName());
            }
            usuarioRepository.save(user);
            shouldCreateInvitation = !Boolean.TRUE.equals(user.getActive());
        } else {
            user = new Usuario();
            user.setEmail(normalizedEmail);
            user.setDisplayName(request.displayName());
            user.setRole(UserRole.PATIENT);
            user.setActive(false);
            user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
            usuarioRepository.save(user);
            shouldCreateInvitation = true;
        }

        Paciente paciente = new Paciente();
        paciente.setUsuario(user);
        paciente.setRut(normalizedRut);
        pacienteRepository.save(paciente);

        String plainToken = null;
        OffsetDateTime expiresAt = null;

        if (shouldCreateInvitation) {
            plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

            invitationTokenRepository.deleteByUser_Id(user.getId());

            ProfessionalInvitationToken token = new ProfessionalInvitationToken();
            token.setUser(user);
            token.setTokenHash(hashToken(plainToken));
            token.setExpiresAt(OffsetDateTime.now().plusSeconds(invitationExpirationSeconds));
            token.setUsed(false);
            invitationTokenRepository.save(token);
            expiresAt = token.getExpiresAt();
        }

        return new PatientInvitationResponse(
                user.getId(),
                paciente.getId(),
                user.getEmail(),
                user.getDisplayName(),
                normalizedRut,
                null,
                user.getRole(),
                plainToken,
                expiresAt
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        OffsetDateTime now = OffsetDateTime.now();
        return usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
            .map(user -> {
                boolean hasPatientProfile = pacienteRepository.existsById(user.getId());
                boolean hasProfessionalProfile = profesionalRepository.existsById(user.getId());
                return new AdminUserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole(),
                    user.getActive(),
                    isInvitationPending(user, now),
                    hasPatientProfile,
                    hasProfessionalProfile
                );
            })
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, AdminUpdateUserStatusRequest request) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(request.active())) {
            throw new ConflictException("No se permite activar usuarios manualmente. El usuario debe aceptar su invitación para activar su cuenta");
        }

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
                updated.getActive(),
                isInvitationPending(updated, OffsetDateTime.now()),
                pacienteRepository.existsById(updated.getId()),
                profesionalRepository.existsById(updated.getId())
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

    @Transactional(readOnly = true)
    public List<AdminSpecialtyResponse> listSpecialties() {
        return specialtyRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(item -> new AdminSpecialtyResponse(
                        item.getId(),
                        item.getName(),
                        item.isActive()
                ))
                .toList();
    }

    @Transactional
    public AdminSpecialtyResponse createSpecialty(AdminCreateSpecialtyRequest request) {
        String normalizedName = normalizeSpecialtyName(request.name());

        Specialty specialty = specialtyRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    Specialty newSpecialty = new Specialty();
                    newSpecialty.setName(normalizedName);
                    return newSpecialty;
                });

        specialty.setName(normalizedName);
        specialty.setActive(true);

        Specialty saved = specialtyRepository.save(specialty);
        return new AdminSpecialtyResponse(saved.getId(), saved.getName(), saved.isActive());
    }

    @Transactional
    public AdminSpecialtyResponse updateSpecialtyStatus(Long specialtyId, AdminUpdateSpecialtyStatusRequest request) {
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));

        specialty.setActive(Boolean.TRUE.equals(request.active()));
        Specialty saved = specialtyRepository.save(specialty);

        return new AdminSpecialtyResponse(saved.getId(), saved.getName(), saved.isActive());
    }

    private void revokeActiveRefreshTokens(Long userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUser_IdAndRevokedFalse(userId);
        activeTokens.forEach(token -> token.setRevoked(true));
    }

    private boolean isInvitationPending(Usuario user, OffsetDateTime now) {
        if (Boolean.TRUE.equals(user.getActive())) {
            return false;
        }
        if (user.getRole() != UserRole.PROFESSIONAL && user.getRole() != UserRole.PATIENT) {
            return false;
        }
        return invitationTokenRepository.existsByUser_IdAndUsedFalseAndExpiresAtAfter(user.getId(), now);
    }

    private String normalizeSpecialtyName(String value) {
        if (value == null || value.isBlank()) {
            throw new ConflictException("El nombre de especialidad es obligatorio");
        }
        return value.trim();
    }

    private Specialty resolveSpecialty(String specialty) {
        String desiredName = (specialty == null || specialty.isBlank())
                ? "Medicina General"
                : specialty.trim();

        return specialtyRepository.findByNameIgnoreCase(desiredName)
                .filter(Specialty::isActive)
                .orElseThrow(() -> new ConflictException("Especialidad inválida. Selecciona una especialidad registrada"));
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
