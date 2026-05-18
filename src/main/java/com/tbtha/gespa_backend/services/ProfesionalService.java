package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CreateProfesionalRequest;
import com.tbtha.gespa_backend.dtos.ProfesionalResponse;
import com.tbtha.gespa_backend.dtos.UpdateProfesionalRequest;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import com.tbtha.gespa_backend.utils.RutUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfesionalService {

    private final ProfesionalRepository profesionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    public ProfesionalService(ProfesionalRepository profesionalRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              AccessControlService accessControlService) {
        this.profesionalRepository = profesionalRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public ProfesionalResponse create(CreateProfesionalRequest request) {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Solo ADMIN puede crear profesionales");
        }

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe un usuario con el email indicado");
        }

        String normalizedRut = RutUtils.normalize(request.rut());

        if (profesionalRepository.existsByRut(normalizedRut)) {
            throw new ConflictException("Ya existe un profesional con ese RUT");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setDisplayName(request.displayName());
        usuario.setRole(UserRole.PROFESSIONAL);
        usuario.setActive(true);
        usuarioRepository.save(usuario);

        Profesional profesional = new Profesional();
        profesional.setUsuario(usuario);
        profesional.setRut(normalizedRut);
        profesional.setSpecialty(request.specialty());
        profesional.setPhone(request.phone());
        profesional.setAddress(request.address());
        profesional.setInstitucion(request.institucion());
        profesional.setDescripcion(request.descripcion());

        return toResponse(profesionalRepository.save(profesional));
    }

    @Transactional(readOnly = true)
    public List<ProfesionalResponse> findAll() {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Solo ADMIN puede listar profesionales");
        }
        return profesionalRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProfesionalResponse findById(Long id) {
        accessControlService.assertCanAccessProfesional(id);
        Profesional profesional = profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
        return toResponse(profesional);
    }

    @Transactional
    public ProfesionalResponse update(Long id, UpdateProfesionalRequest request) {
        accessControlService.assertCanAccessProfesional(id);

        Profesional profesional = profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        String normalizedRut = RutUtils.normalize(request.rut());
        if (profesionalRepository.existsByRutAndIdNot(normalizedRut, id)) {
            throw new ConflictException("Ya existe un profesional con ese RUT");
        }

        profesional.getUsuario().setDisplayName(request.displayName());
        profesional.setSpecialty(request.specialty());
        profesional.setRut(normalizedRut);
        profesional.setPhone(request.phone());
        profesional.setAddress(request.address());
        profesional.setInstitucion(request.institucion());
        profesional.setDescripcion(request.descripcion());

        return toResponse(profesionalRepository.save(profesional));
    }

    private ProfesionalResponse toResponse(Profesional profesional) {
        return new ProfesionalResponse(
                profesional.getId(),
                profesional.getUsuario().getEmail(),
                profesional.getUsuario().getDisplayName(),
                profesional.getRut(),
                profesional.getSpecialty(),
                profesional.getPhone(),
                profesional.getAddress(),
                profesional.getInstitucion(),
                profesional.getDescripcion()
        );
    }
}
