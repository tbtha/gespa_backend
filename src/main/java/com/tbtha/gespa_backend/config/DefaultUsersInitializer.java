package com.tbtha.gespa_backend.config;

import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Specialty;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.SpecialtyRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import com.tbtha.gespa_backend.utils.RutUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultUsersInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ProfesionalRepository profesionalRepository;
    private final PacienteRepository pacienteRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.defaults.enabled:true}")
    private boolean enabled;

    @Value("${app.seed.defaults.admin-email:admin@gespa.local}")
    private String adminEmail;

    @Value("${app.seed.defaults.admin-display-name:Administrador}")
    private String adminDisplayName;

    @Value("${app.seed.defaults.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.defaults.shared-password:Demo12345!}")
    private String sharedPassword;

    @Value("${app.seed.defaults.professional-email:demo.professional@gespa.local}")
    private String professionalEmail;

    @Value("${app.seed.defaults.patient-email:demo.patient@gespa.local}")
    private String patientEmail;

    @Value("${app.seed.defaults.professional-display-name:Profesional Demo}")
    private String professionalDisplayName;

    @Value("${app.seed.defaults.patient-display-name:Paciente Demo}")
    private String patientDisplayName;

    @Value("${app.seed.defaults.professional-specialty:Medicina General}")
    private String professionalSpecialty;

    @Value("${app.seed.defaults.professional-rut:12345678-5}")
    private String professionalRut;

    @Value("${app.seed.defaults.patient-rut:11111111-1}")
    private String patientRut;

    public DefaultUsersInitializer(UsuarioRepository usuarioRepository,
                                   ProfesionalRepository profesionalRepository,
                                   PacienteRepository pacienteRepository,
                                   SpecialtyRepository specialtyRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.profesionalRepository = profesionalRepository;
        this.pacienteRepository = pacienteRepository;
        this.specialtyRepository = specialtyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        requireNonBlank("app.seed.defaults.admin-password", adminPassword);
        requireNonBlank("app.seed.defaults.admin-email", adminEmail);
        requireNonBlank("app.seed.defaults.admin-display-name", adminDisplayName);

        ensureUser(
            adminEmail,
            adminDisplayName,
            UserRole.ADMIN,
            adminPassword
        );

        Usuario professionalUser = ensureUser(
                professionalEmail,
                professionalDisplayName,
            UserRole.PROFESSIONAL,
            sharedPassword
        );

        ensureProfessional(professionalUser);

        Usuario patientUser = ensureUser(
                patientEmail,
                patientDisplayName,
            UserRole.PATIENT,
            sharedPassword
        );

        ensurePatient(patientUser);
    }

    private Usuario ensureUser(String email, String displayName, UserRole role, String rawPassword) {
        return usuarioRepository.findByEmail(email)
                .map(existing -> {
                    boolean changed = false;

                    if (existing.getRole() != role) {
                        existing.setRole(role);
                        changed = true;
                    }

                    if (!displayName.equals(existing.getDisplayName())) {
                        existing.setDisplayName(displayName);
                        changed = true;
                    }

                    if (!Boolean.TRUE.equals(existing.getActive())) {
                        existing.setActive(true);
                        changed = true;
                    }

                    if (changed) {
                        return usuarioRepository.save(existing);
                    }

                    return existing;
                })
                .orElseGet(() -> {
                    Usuario user = new Usuario();
                    user.setEmail(email);
                    user.setDisplayName(displayName);
                    user.setRole(role);
                    user.setActive(true);
                    user.setPasswordHash(passwordEncoder.encode(rawPassword));
                    return usuarioRepository.save(user);
                });
    }

    private void requireNonBlank(String propertyName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Propiedad requerida no configurada: " + propertyName);
        }
    }

    private Profesional ensureProfessional(Usuario professionalUser) {
        Specialty selectedSpecialty = specialtyRepository.findByNameIgnoreCase(professionalSpecialty)
                .orElseGet(() -> {
                    Specialty specialty = new Specialty();
                    specialty.setName(professionalSpecialty);
                    specialty.setActive(true);
                    return specialtyRepository.save(specialty);
                });

        return profesionalRepository.findById(professionalUser.getId())
                .map(existing -> {
                    boolean changed = false;

                    if (existing.getRut() == null || existing.getRut().isBlank()) {
                        existing.setRut(resolveUniqueProfessionalRut(professionalUser.getId()));
                        changed = true;
                    }

                    if (existing.getSpecialtyRef() == null
                            || existing.getSpecialtyRef().getId() == null
                            || !existing.getSpecialtyRef().getId().equals(selectedSpecialty.getId())) {
                        existing.setSpecialtyRef(selectedSpecialty);
                        changed = true;
                    }

                    if (existing.getSpecialty() == null || existing.getSpecialty().isBlank()) {
                        existing.setSpecialty(selectedSpecialty.getName());
                        changed = true;
                    }

                    return changed ? profesionalRepository.save(existing) : existing;
                })
                .orElseGet(() -> {
                    Profesional profesional = new Profesional();
                    profesional.setUsuario(professionalUser);
                    profesional.setRut(resolveUniqueProfessionalRut(professionalUser.getId()));
                    profesional.setSpecialtyRef(selectedSpecialty);
                    profesional.setSpecialty(selectedSpecialty.getName());
                    return profesionalRepository.save(profesional);
                });
    }

    private Paciente ensurePatient(Usuario patientUser) {
        return pacienteRepository.findById(patientUser.getId())
                .orElseGet(() -> {
                    Paciente paciente = new Paciente();
                    paciente.setUsuario(patientUser);
                    paciente.setProfesional(null);
                    paciente.setRut(resolveUniqueRut(patientUser.getId()));
                    return pacienteRepository.save(paciente);
                });
    }

    private String resolveUniqueRut(Long patientUserId) {
        String normalized = RutUtils.normalize(patientRut);
        if (!RutUtils.isValid(normalized)) {
            throw new IllegalStateException("RUT de seed inválido para paciente: " + patientRut);
        }
        if (!pacienteRepository.existsByRut(normalized)) {
            return normalized;
        }
        return buildValidRutFromBase(20_000_000L + patientUserId);
    }

    private String resolveUniqueProfessionalRut(Long professionalUserId) {
        String normalized = RutUtils.normalize(professionalRut);
        if (!RutUtils.isValid(normalized)) {
            throw new IllegalStateException("RUT de seed inválido para profesional: " + professionalRut);
        }
        if (!profesionalRepository.existsByRut(normalized)) {
            return normalized;
        }
        return buildValidRutFromBase(30_000_000L + professionalUserId);
    }

    private String buildValidRutFromBase(long body) {
        String numberPart = String.valueOf(Math.max(body, 1));

        int sum = 0;
        int multiplier = 2;
        for (int i = numberPart.length() - 1; i >= 0; i--) {
            sum += Character.getNumericValue(numberPart.charAt(i)) * multiplier;
            multiplier = multiplier == 7 ? 2 : multiplier + 1;
        }

        int remainder = 11 - (sum % 11);
        String dv;
        if (remainder == 11) {
            dv = "0";
        } else if (remainder == 10) {
            dv = "K";
        } else {
            dv = String.valueOf(remainder);
        }

        return numberPart + "-" + dv;
    }
}
