package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CreatePacienteRequest;
import com.tbtha.gespa_backend.dtos.PacienteResponse;
import com.tbtha.gespa_backend.dtos.PagedResponse;
import com.tbtha.gespa_backend.dtos.UpdatePacienteRequest;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.EstadoCivil;
import com.tbtha.gespa_backend.entities.enums.Prevision;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PacienteService(PacienteRepository pacienteRepository,
                           ProfesionalRepository profesionalRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PacienteResponse create(CreatePacienteRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe un usuario con el email indicado");
        }
        if (pacienteRepository.existsByRut(request.rut())) {
            throw new ConflictException("Ya existe un paciente con el RUT indicado");
        }

        Profesional profesional = getProfesional(request.professionalId());

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setDisplayName(request.displayName());
        usuario.setRole(UserRole.PATIENT);
        usuario.setActive(true);
        usuarioRepository.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        paciente.setProfesional(profesional);
        paciente.setRut(request.rut());
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

    @Transactional(readOnly = true)
    public PagedResponse<PacienteResponse> findAll(String q,
                                                   Long profesionalId,
                                                   Prevision prevision,
                                                   EstadoCivil estadoCivil,
                                                   int page,
                                                   int size,
                                                   String sortBy,
                                                   String sortDir) {

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
        return toResponse(getPaciente(id));
    }

    @Transactional
    public PacienteResponse update(Long id, UpdatePacienteRequest request) {
        Paciente paciente = getPaciente(id);
        Profesional profesional = getProfesional(request.professionalId());

        paciente.setProfesional(profesional);
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

    private Profesional getProfesional(Long id) {
        return profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
    }

    private Paciente getPaciente(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
    }

    private PacienteResponse toResponse(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getUsuario().getEmail(),
                paciente.getUsuario().getDisplayName(),
                paciente.getProfesional().getId(),
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
