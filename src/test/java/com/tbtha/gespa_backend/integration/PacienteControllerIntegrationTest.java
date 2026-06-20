package com.tbtha.gespa_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Specialty;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.RefreshTokenRepository;
import com.tbtha.gespa_backend.repositories.SpecialtyRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PA-02, PA-04 a PA-08 del plan de pruebas — tests de integración con H2 + MockMvc.
 *
 * NOTA: No se usa @Transactional a nivel de clase porque MockMvc ejecuta las
 * peticiones HTTP en un hilo del servidor separado, por lo que el rollback de
 * la transacción del test no revierte los datos persistidos por el servidor.
 * Se usa @AfterEach con TransactionTemplate para garantizar que la limpieza
 * ocurra dentro de una transacción real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PacienteControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired ProfesionalRepository profesionalRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TransactionTemplate tx;

    private String jwtProfesional;
    private Long pacienteExistenteId;

    @BeforeEach
    void setUp() throws Exception {
        // Ejecutar el setup dentro de una transacción para que los objetos
        // queden managed y no detached al crear las asociaciones
        tx.execute(status -> {
            // Specialty — reusar la creada por DefaultSpecialtiesInitializer si ya existe
            Specialty spec = specialtyRepository.findByNameIgnoreCase("Medicina General")
                    .orElseGet(() -> {
                        Specialty s = new Specialty();
                        s.setName("Medicina General");
                        s.setActive(true);
                        return specialtyRepository.save(s);
                    });

            // Profesional
            Usuario uProf = new Usuario();
            uProf.setEmail("prof.pac@gespa.cl");
            uProf.setDisplayName("Dr. Profesional Pac");
            uProf.setRole(UserRole.PROFESSIONAL);
            uProf.setActive(true);
            uProf.setPasswordHash(passwordEncoder.encode("Test1234!"));
            uProf = usuarioRepository.save(uProf);

            Profesional prof = new Profesional();
            prof.setUsuario(uProf);
            prof.setRut("66666666-6");
            prof.setSpecialty("Medicina General");
            prof.setSpecialtyRef(spec);
            profesionalRepository.save(prof);

            // Paciente existente para búsqueda
            Usuario uPac = new Usuario();
            uPac.setEmail("juan.perez@test.cl");
            uPac.setDisplayName("Juan Pérez");
            uPac.setRole(UserRole.PATIENT);
            uPac.setActive(true);
            uPac.setPasswordHash(passwordEncoder.encode("Test1234!"));
            uPac = usuarioRepository.save(uPac);

            Paciente pac = new Paciente();
            pac.setUsuario(uPac);
            pac.setRut("55555555-5");
            Paciente saved = pacienteRepository.save(pac);
            pacienteExistenteId = saved.getId();

            return null;
        });

        // JWT del profesional (fuera de la transacción, via HTTP)
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "prof.pac@gespa.cl",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        jwtProfesional = mapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        // Limpieza dentro de una transacción explícita
        tx.execute(status -> {
            for (String email : new String[]{"prof.pac@gespa.cl", "juan.perez@test.cl"}) {
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
     * PA-04 — Buscar paciente por nombre parcial (integración).
     * Funcionalidad: GET /api/pacientes?q=Juan debe retornar todos los
     * pacientes cuyo displayName contenga "Juan" (búsqueda case-insensitive).
     * El resultado viene paginado con la estructura content/totalElements/totalPages.
     */
    @Test
    @DisplayName("PA-04: GET /pacientes?q=Juan retorna lista filtrada por nombre")
    void buscarPorNombre_retornaResultadosFiltrados() throws Exception {
        mockMvc.perform(get("/api/pacientes")
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .param("q", "Juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].displayName").value(containsStringIgnoringCase("Juan")));
    }

    /**
     * PA-05 — Buscar paciente por RUT parcial (integración).
     * Funcionalidad: GET /api/pacientes?q=55555555 debe retornar exactamente
     * el paciente con RUT "55555555-5". La búsqueda por RUT permite al
     * profesional encontrar un paciente aunque no recuerde su nombre completo.
     */
    @Test
    @DisplayName("PA-05: GET /pacientes?q=55555555 retorna paciente por RUT")
    void buscarPorRut_retornaResultadosFiltrados() throws Exception {
        mockMvc.perform(get("/api/pacientes")
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .param("q", "55555555"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].rut").value("55555555-5"));
    }

    /**
     * PA-06 — Obtener datos completos de un paciente por ID (integración).
     * Funcionalidad: GET /api/pacientes/{id} debe retornar HTTP 200 con todos
     * los datos del paciente: id, rut, displayName, email y campos opcionales.
     * Se usa al abrir la ficha clínica desde la agenda o el listado de pacientes.
     */
    @Test
    @DisplayName("PA-06: GET /pacientes/{id} retorna datos completos del paciente")
    void getPacienteById_retornaDatosCompletos() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}", pacienteExistenteId)
                        .header("Authorization", "Bearer " + jwtProfesional))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pacienteExistenteId))
                .andExpect(jsonPath("$.rut").value("55555555-5"))
                .andExpect(jsonPath("$.displayName").value("Juan Pérez"));
    }

    /**
     * PA-07 — Actualizar datos generales del paciente (integración).
     * Funcionalidad: PUT /api/pacientes/{id} con datos actualizados debe
     * retornar HTTP 200 con los nuevos valores persistidos. Cubre la
     * funcionalidad de edición en la pestaña "Datos generales" de la ficha.
     */
    @Test
    @DisplayName("PA-07: PUT /pacientes/{id} actualiza datos del paciente correctamente")
    void actualizarPaciente_datosValidos_retornaActualizado() throws Exception {
        Map<String, Object> update = Map.of(
                "email", "juan.perez@test.cl",
                "displayName", "Juan Pérez Actualizado",
                "prevision", "FONASA"
        );

        mockMvc.perform(put("/api/pacientes/{id}", pacienteExistenteId)
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Juan Pérez Actualizado"))
                .andExpect(jsonPath("$.prevision").value("FONASA"));
    }

    /**
     * PA-08 — Listado paginado de pacientes (integración).
     * Funcionalidad: GET /api/pacientes?page=0&size=5 debe retornar una
     * respuesta paginada con los campos content (array), totalElements
     * (número total de pacientes) y totalPages (número de páginas).
     */
    @Test
    @DisplayName("PA-08: GET /pacientes?page=0&size=5 retorna respuesta paginada")
    void listadoPacientes_paginado_retornaEstructuraCorrecta() throws Exception {
        mockMvc.perform(get("/api/pacientes")
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    /**
     * PA-02 — Crear paciente con RUT duplicado retorna 409 (integración).
     * Funcionalidad: POST /api/pacientes con un RUT que ya pertenece a otro
     * paciente debe retornar HTTP 409 Conflict, impidiendo la duplicación
     * de fichas clínicas para el mismo RUT chileno.
     */
    @Test
    @DisplayName("PA-02: POST /pacientes con RUT duplicado retorna 409")
    void crearPaciente_rutDuplicado_retorna409() throws Exception {
        Map<String, Object> payload = Map.of(
                "email", "nuevo@test.cl",
                "password", "Test1234!",
                "displayName", "Nuevo Paciente",
                "rut", "55555555-5"   // RUT ya registrado
        );

        mockMvc.perform(post("/api/pacientes")
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isConflict());
    }

    /**
     * AD-10 — Profesional no puede acceder al panel de administración (integración).
     * Funcionalidad: GET /api/admin/users con un JWT de rol PROFESSIONAL debe
     * retornar HTTP 403 Forbidden. Solo el rol ADMIN tiene acceso a los
     * endpoints bajo /api/admin/*.
     */
    @Test
    @DisplayName("AD-10: GET /api/admin/users con JWT profesional retorna 403")
    void adminUsers_conJwtProfesional_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + jwtProfesional))
                .andExpect(status().isForbidden());
    }

    /**
     * SEC-02 — Las respuestas de paciente no exponen el hash de contraseña.
     * Funcionalidad: GET /api/pacientes/{id} no debe incluir campos de
     * seguridad como passwordHash en el cuerpo de la respuesta JSON,
     * asegurando que las credenciales del usuario no sean accesibles
     * a través de la API de gestión clínica.
     */
    @Test
    @DisplayName("SEC-02: respuesta de paciente no incluye campo passwordHash")
    void getPaciente_respuestaNoExponePasswordHash() throws Exception {
        String response = mockMvc.perform(get("/api/pacientes/{id}", pacienteExistenteId)
                        .header("Authorization", "Bearer " + jwtProfesional))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assert !response.contains("passwordHash") : "La respuesta no debe contener passwordHash";
        assert !response.contains("password") : "La respuesta no debe contener campos de contraseña";
    }
}
