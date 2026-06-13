package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CreatePacienteRequest;
import com.tbtha.gespa_backend.dtos.CreatePatientInvitationRequest;
import com.tbtha.gespa_backend.dtos.PacienteResponse;
import com.tbtha.gespa_backend.dtos.PatientInvitationResponse;
import com.tbtha.gespa_backend.dtos.PagedResponse;
import com.tbtha.gespa_backend.dtos.UpdatePacienteRequest;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.ProfessionalInvitationToken;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.EstadoCivil;
import com.tbtha.gespa_backend.entities.enums.Prevision;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfessionalInvitationTokenRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import com.tbtha.gespa_backend.services.email.UserAccountEmailService;
import com.tbtha.gespa_backend.utils.RutUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProfessionalInvitationTokenRepository invitationTokenRepository;
    private final AccessControlService accessControlService;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountEmailService userAccountEmailService;
    private final long invitationExpirationSeconds;

    public PacienteService(PacienteRepository pacienteRepository,
                           UsuarioRepository usuarioRepository,
                           ProfessionalInvitationTokenRepository invitationTokenRepository,
                           AccessControlService accessControlService,
                           PasswordEncoder passwordEncoder,
                           UserAccountEmailService userAccountEmailService,
                           @Value("${app.auth.invitation.expiration-seconds:604800}") long invitationExpirationSeconds) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.accessControlService = accessControlService;
        this.passwordEncoder = passwordEncoder;
        this.userAccountEmailService = userAccountEmailService;
        this.invitationExpirationSeconds = invitationExpirationSeconds;
    }

    @Transactional
    public PacienteResponse create(CreatePacienteRequest request) {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.PROFESSIONAL) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para crear pacientes");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedRut = RutUtils.normalize(request.rut());

        Usuario existingUser = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() != UserRole.PROFESSIONAL && existingUser.getRole() != UserRole.PATIENT) {
                throw new ConflictException("Ya existe un usuario con el email indicado");
            }
            if (pacienteRepository.existsById(existingUser.getId())) {
                throw new ConflictException("Ya existe un paciente con el email indicado");
            }
        }
        if (pacienteRepository.existsByRut(normalizedRut)) {
            throw new ConflictException("Ya existe un paciente con el RUT indicado");
        }

        boolean isAdminActor = actor.getRole() == UserRole.ADMIN;
        boolean createdNewUser = false;
        Usuario usuario;
        if (existingUser != null) {
            usuario = existingUser;
            if (request.displayName() != null && !request.displayName().isBlank()) {
                usuario.setDisplayName(request.displayName());
            }
            if (!Boolean.TRUE.equals(usuario.getActive())) {
                usuario.setActive(true);
            }
            usuarioRepository.save(usuario);
        } else {
            createdNewUser = true;
            usuario = new Usuario();
            usuario.setEmail(normalizedEmail);
            usuario.setDisplayName(request.displayName());
            usuario.setRole(UserRole.PATIENT);
            if (isAdminActor) {
                usuario.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
                usuario.setActive(false);
            } else {
                usuario.setPasswordHash(passwordEncoder.encode(request.password()));
                usuario.setActive(true);
            }
            usuarioRepository.save(usuario);
            if (!isAdminActor) {
                userAccountEmailService.sendUserCreatedEmail(usuario);
            }
        }

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        paciente.setProfesional(null);
        paciente.setRut(normalizedRut);
        paciente.setBirthdate(request.birthdate());
        paciente.setGender(request.gender());
        paciente.setPrevision(request.prevision());
        paciente.setEstadoCivil(request.estadoCivil());
        paciente.setOcupacion(request.ocupacion());
        paciente.setPhone(request.phone());
        paciente.setAddress(request.address());
        paciente.setEmergencyContactName(request.emergencyContactName());
        paciente.setEmergencyContactPhone(request.emergencyContactPhone());

        if (isAdminActor && createdNewUser) {
            String plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

            invitationTokenRepository.deleteByUser_Id(usuario.getId());

            ProfessionalInvitationToken token = new ProfessionalInvitationToken();
            token.setUser(usuario);
            token.setTokenHash(hashToken(plainToken));
            token.setExpiresAt(OffsetDateTime.now().plusSeconds(invitationExpirationSeconds));
            token.setUsed(false);
            invitationTokenRepository.save(token);

            userAccountEmailService.sendActivationInvitationEmail(usuario, plainToken, token.getExpiresAt());
        }

        return toResponse(pacienteRepository.save(paciente));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PacienteResponse> findAll(String q,
                                                   Long profesionalId,
                                                   Prevision prevision,
                                                   EstadoCivil estadoCivil,
                                                   int page,
                                                   int size,
                                                   String sortBy,
                                                   String sortDir) {

        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() == UserRole.PATIENT) {
            Paciente paciente = getPaciente(actor.getId());
            List<PacienteResponse> content = List.of(toResponse(paciente));
            return new PagedResponse<>(content, 0, 1, 1, 1);
        }

        String resolvedSortBy = resolveSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, resolvedSortBy));

        Specification<Paciente> spec = Specification.where(null);

        if (profesionalId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("profesional").get("id"), profesionalId));
        }

        if (prevision != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("prevision"), prevision));
        }

        if (estadoCivil != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estadoCivil"), estadoCivil));
        }

        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                Join<Paciente, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
                return cb.or(
                        cb.like(cb.lower(usuarioJoin.get("displayName")), pattern),
                        cb.like(cb.lower(root.get("rut")), pattern)
                );
            });
        }

        Page<Paciente> result = pacienteRepository.findAll(spec, pageable);
        List<PacienteResponse> content = result.getContent().stream().map(this::toResponse).toList();

        return new PagedResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PacienteResponse findById(Long id) {
        accessControlService.assertCanAccessPaciente(id);
        return toResponse(getPaciente(id));
    }

    @Transactional
    public PacienteResponse update(Long id, UpdatePacienteRequest request) {
        Paciente paciente = getPaciente(id);

        accessControlService.assertCanAccessPaciente(id);
        if (request.email() != null && !request.email().isBlank()) {
            String nextEmail = request.email().trim().toLowerCase();
            String currentEmail = String.valueOf(paciente.getUsuario().getEmail()).trim().toLowerCase();
            if (!nextEmail.equals(currentEmail) && usuarioRepository.existsByEmail(nextEmail)) {
                throw new ConflictException("Ya existe un usuario con el email indicado");
            }
            paciente.getUsuario().setEmail(nextEmail);
        }
        if (request.displayName() != null && !request.displayName().isBlank()) {
            paciente.getUsuario().setDisplayName(request.displayName());
        }
        paciente.setBirthdate(request.birthdate());
        paciente.setGender(request.gender());
        paciente.setPrevision(request.prevision());
        paciente.setEstadoCivil(request.estadoCivil());
        paciente.setOcupacion(request.ocupacion());
        paciente.setPhone(request.phone());
        paciente.setAddress(request.address());
        paciente.setEmergencyContactName(request.emergencyContactName());
        paciente.setEmergencyContactPhone(request.emergencyContactPhone());

        return toResponse(pacienteRepository.save(paciente));
    }

    @Transactional
    public PatientInvitationResponse createPatientInvitation(CreatePatientInvitationRequest request) {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.PROFESSIONAL) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para crear invitaciones de paciente");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        Usuario existingUser = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() != UserRole.PROFESSIONAL && existingUser.getRole() != UserRole.PATIENT) {
                throw new ConflictException("Ya existe un usuario con el email indicado");
            }
            if (pacienteRepository.existsById(existingUser.getId())) {
                throw new ConflictException("Ya existe un paciente con el email indicado");
            }
        }

        String normalizedRut = RutUtils.normalize(request.rut());

        if (pacienteRepository.existsByRut(normalizedRut)) {
            throw new ConflictException("Ya existe un paciente con el RUT indicado");
        }

        Usuario user;
        if (existingUser != null) {
            user = existingUser;
            if (request.displayName() != null && !request.displayName().isBlank()) {
                user.setDisplayName(request.displayName());
            }
            usuarioRepository.save(user);
        } else {
            user = new Usuario();
            user.setEmail(normalizedEmail);
            user.setDisplayName(request.displayName());
            user.setRole(UserRole.PATIENT);
            user.setActive(false);
            user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
            usuarioRepository.save(user);
        }

        Paciente paciente = new Paciente();
        paciente.setUsuario(user);
        paciente.setProfesional(null);
        paciente.setRut(normalizedRut);
        pacienteRepository.save(paciente);

        String plainToken = null;
        OffsetDateTime expiresAt = null;

        if (existingUser == null || !Boolean.TRUE.equals(user.getActive())) {
            plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

            invitationTokenRepository.deleteByUser_Id(user.getId());

            ProfessionalInvitationToken token = new ProfessionalInvitationToken();
            token.setUser(user);
            token.setTokenHash(hashToken(plainToken));
            token.setExpiresAt(OffsetDateTime.now().plusSeconds(invitationExpirationSeconds));
            token.setUsed(false);
            invitationTokenRepository.save(token);
            expiresAt = token.getExpiresAt();
            userAccountEmailService.sendActivationInvitationEmail(user, plainToken, expiresAt);
        }

        return new PatientInvitationResponse(
                user.getId(),
                paciente.getId(),
                user.getEmail(),
                user.getDisplayName(),
                paciente.getRut(),
                null,
                user.getRole(),
                plainToken,
            expiresAt
        );
    }

    private Paciente getPaciente(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
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

    private PacienteResponse toResponse(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getUsuario().getEmail(),
                paciente.getUsuario().getDisplayName(),
                paciente.getProfesional() == null ? null : paciente.getProfesional().getId(),
                paciente.getProfesional() == null ? null : paciente.getProfesional().getUsuario().getDisplayName(),
                paciente.getProfesional() == null ? null : paciente.getProfesional().getSpecialty(),
                paciente.getRut(),
                paciente.getBirthdate(),
                paciente.getGender(),
                paciente.getPrevision(),
                paciente.getEstadoCivil(),
                paciente.getOcupacion(),
                paciente.getPhone(),
                paciente.getAddress(),
                paciente.getEmergencyContactName(),
                paciente.getEmergencyContactPhone()
        );
    }

    private String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }

        return switch (sortBy) {
            case "displayName" -> "usuario.displayName";
            case "rut", "birthdate", "createdAt", "id" -> sortBy;
            default -> "id";
        };
    }
}
