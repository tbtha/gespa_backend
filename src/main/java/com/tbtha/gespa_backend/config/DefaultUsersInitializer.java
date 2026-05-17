package com.tbtha.gespa_backend.config;

import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
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

    @Value("${app.seed.defaults.professional-rut:RUT-PRO-DEMO-001}")
    private String professionalRut;

    @Value("${app.seed.defaults.patient-rut:RUT-DEMO-001}")
    private String patientRut;

    public DefaultUsersInitializer(UsuarioRepository usuarioRepository,
                                   ProfesionalRepository profesionalRepository,
                                   PacienteRepository pacienteRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.profesionalRepository = profesionalRepository;
        this.pacienteRepository = pacienteRepository;
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

        Profesional profesional = ensureProfessional(professionalUser);

        Usuario patientUser = ensureUser(
                patientEmail,
                patientDisplayName,
            UserRole.PATIENT,
            sharedPassword
        );

        ensurePatient(patientUser, profesional);
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
        return profesionalRepository.findById(professionalUser.getId())
                .map(existing -> {
                    boolean changed = false;

                    if (existing.getRut() == null || existing.getRut().isBlank()) {
                        existing.setRut(resolveUniqueProfessionalRut(professionalUser.getId()));
                        changed = true;
                    }

                    if (existing.getSpecialty() == null || existing.getSpecialty().isBlank()) {
                        existing.setSpecialty(professionalSpecialty);
                        changed = true;
                    }

                    if (existing.getLicenseNumber() == null || existing.getLicenseNumber().isBlank()) {
                        existing.setLicenseNumber("LIC-DEMO-" + professionalUser.getId());
                        changed = true;
                    }

                    return changed ? profesionalRepository.save(existing) : existing;
                })
                .orElseGet(() -> {
                    Profesional profesional = new Profesional();
                    profesional.setUsuario(professionalUser);
                    profesional.setRut(resolveUniqueProfessionalRut(professionalUser.getId()));
                    profesional.setSpecialty(professionalSpecialty);
                    profesional.setLicenseNumber("LIC-DEMO-" + professionalUser.getId());
                    return profesionalRepository.save(profesional);
                });
    }

    private Paciente ensurePatient(Usuario patientUser, Profesional profesional) {
        return pacienteRepository.findById(patientUser.getId())
                .orElseGet(() -> {
                    Paciente paciente = new Paciente();
                    paciente.setUsuario(patientUser);
                    paciente.setProfesional(profesional);
                    paciente.setRut(resolveUniqueRut(patientUser.getId()));
                    return pacienteRepository.save(paciente);
                });
    }

    private String resolveUniqueRut(Long patientUserId) {
        if (!pacienteRepository.existsByRut(patientRut)) {
            return patientRut;
        }
        return "RUT-DEMO-" + patientUserId;
    }

    private String resolveUniqueProfessionalRut(Long professionalUserId) {
        if (!profesionalRepository.existsByRut(professionalRut)) {
            return professionalRut;
        }
        return "RUT-PRO-" + professionalUserId;
    }
}
