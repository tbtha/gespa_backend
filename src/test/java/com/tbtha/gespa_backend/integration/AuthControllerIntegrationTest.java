package com.tbtha.gespa_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.RefreshTokenRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AU-01 a AU-08 del plan de pruebas — tests de integración con H2 + MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired ProfesionalRepository profesionalRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TransactionTemplate tx;

    @BeforeEach
    void seedUsuarios() {
        tx.execute(status -> {
            if (usuarioRepository.findByEmail("prof1@gespa.cl").isEmpty()) {
                Usuario prof = new Usuario();
                prof.setEmail("prof1@gespa.cl");
                prof.setDisplayName("Profesional Test");
                prof.setRole(UserRole.PROFESSIONAL);
                prof.setActive(true);
                prof.setPasswordHash(passwordEncoder.encode("admin123!"));
                prof = usuarioRepository.save(prof);

                Profesional perfProf = new Profesional();
                perfProf.setUsuario(prof);
                perfProf.setRut("11111111-1");
                perfProf.setSpecialty("Medicina General");
                profesionalRepository.save(perfProf);
            }

            if (usuarioRepository.findByEmail("pac1@gespa.cl").isEmpty()) {
                Usuario pac = new Usuario();
                pac.setEmail("pac1@gespa.cl");
                pac.setDisplayName("Paciente Test");
                pac.setRole(UserRole.PATIENT);
                pac.setActive(true);
                pac.setPasswordHash(passwordEncoder.encode("admin123!"));
                pac = usuarioRepository.save(pac);

                Paciente perfPac = new Paciente();
                perfPac.setUsuario(pac);
                perfPac.setRut("22222222-2");
                pacienteRepository.save(perfPac);
            }
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        tx.execute(status -> {
            for (String email : new String[]{"prof1@gespa.cl", "pac1@gespa.cl"}) {
                usuarioRepository.findByEmail(email).ifPresent(u -> {
                    refreshTokenRepository.deleteAll(
                            refreshTokenRepository.findAllByUser_IdAndRevokedFalse(u.getId()));
                    pacienteRepository.findById(u.getId()).ifPresent(pacienteRepository::delete);
                    profesionalRepository.findById(u.getId()).ifPresent(profesionalRepository::delete);
                    usuarioRepository.delete(u);
                });
            }
            return null;
        });
    }

    /**
     * AU-01 — Login profesional con credenciales válidas.
     * Funcionalidad: POST /api/auth/login/professional con email y contraseña
     * correctos debe retornar HTTP 200 con accessToken, refreshToken y
     * role=PROFESSIONAL. Estos tokens se usan en todas las peticiones
     * subsiguientes del profesional.
     */
    @Test
    @DisplayName("AU-01: login profesional con credenciales válidas retorna accessToken y refreshToken")
    void loginProfesional_credencialesValidas_retornaTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "prof1@gespa.cl",
                                "password", "admin123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PROFESSIONAL"));
    }

    /**
     * AU-02 — Login con contraseña incorrecta es rechazado.
     * Funcionalidad: POST /api/auth/login/professional con contraseña errónea
     * debe retornar HTTP 409 Conflict. No debe retornar 401 (que indicaría
     * problema de autenticación a nivel de filtro) sino 409 con mensaje
     * "Credenciales inválidas" desde el servicio de negocio.
     */
    @Test
    @DisplayName("AU-02: login con contraseña incorrecta retorna 409 Conflict")
    void loginProfesional_contrasenaIncorrecta_retorna409() throws Exception {
        mockMvc.perform(post("/api/auth/login/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "prof1@gespa.cl",
                                "password", "wrongpassword"
                        ))))
                .andExpect(status().isConflict());
    }

    /**
     * AU-03 — Login como paciente retorna rol PATIENT.
     * Funcionalidad: POST /api/auth/login/patient con credenciales de un
     * usuario con perfil de paciente debe retornar HTTP 200 y role=PATIENT.
     * El token generado solo permite acceder a los endpoints del portal paciente.
     */
    @Test
    @DisplayName("AU-03: login paciente con credenciales válidas retorna role=PATIENT")
    void loginPaciente_credencialesValidas_retornaRolePaciente() throws Exception {
        mockMvc.perform(post("/api/auth/login/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "pac1@gespa.cl",
                                "password", "admin123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    /**
     * AU-07 — Acceso a endpoint protegido sin token JWT retorna 401.
     * Funcionalidad: cualquier petición a un endpoint protegido sin el
     * header Authorization debe ser rechazada con HTTP 401 Unauthorized
     * por el filtro de seguridad, antes de llegar al controlador.
     */
    @Test
    @DisplayName("AU-07: GET /api/pacientes sin Authorization header retorna 401")
    void getPackientes_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * AU-07b — Endpoint de administración sin JWT también retorna 401.
     * Funcionalidad: GET /api/admin/users sin token debe retornar 401
     * (no 403), ya que el rechazo ocurre antes de evaluar el rol.
     */
    @Test
    @DisplayName("AU-07b: GET /api/admin/users sin Authorization header retorna 401")
    void getAdminUsers_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * AU-14 — Verificar si un email ya está registrado (email existente).
     * Funcionalidad: POST /api/auth/check-email con un email registrado
     * debe retornar exists=true. Se usa en el flujo de registro para
     * informar al usuario antes de intentar crear una cuenta duplicada.
     */
    @Test
    @DisplayName("AU-14: checkEmail con email registrado retorna exists=true")
    void checkEmail_emailExistente_retornaExists() throws Exception {
        mockMvc.perform(post("/api/auth/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "prof1@gespa.cl"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    /**
     * AU-14b — Verificar email no registrado retorna exists=false.
     * Funcionalidad: POST /api/auth/check-email con un email que no existe
     * debe retornar exists=false, permitiendo al frontend ofrecer el flujo
     * de registro en lugar del de login.
     */
    @Test
    @DisplayName("AU-14b: checkEmail con email no registrado retorna exists=false")
    void checkEmail_emailInexistente_retornaNoExists() throws Exception {
        mockMvc.perform(post("/api/auth/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "noexiste@gespa.cl"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    /**
     * Health check — El contexto de Spring levanta correctamente.
     * Funcionalidad: GET /api/health debe retornar HTTP 200 con status=ok.
     * Este test sirve como verificación base de que la aplicación arrancó
     * correctamente con el perfil de test y la BD H2 en memoria.
     */
    @Test
    @DisplayName("GET /api/health retorna status ok")
    void healthCheck_retornaOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
