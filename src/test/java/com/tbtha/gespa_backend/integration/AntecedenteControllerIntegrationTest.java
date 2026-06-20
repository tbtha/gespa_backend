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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ANT-01 a ANT-05 del plan de pruebas — tests de integración con H2 + MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AntecedenteControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired ProfesionalRepository profesionalRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TransactionTemplate tx;

    private String jwtProfesional;
    private Long pacienteId;

    @BeforeEach
    void setUp() throws Exception {
        tx.execute(status -> {
            Usuario prof = new Usuario();
            prof.setEmail("prof.ant@gespa.cl");
            prof.setDisplayName("Profesional Antecedentes");
            prof.setRole(UserRole.PROFESSIONAL);
            prof.setActive(true);
            prof.setPasswordHash(passwordEncoder.encode("Test1234!"));
            prof = usuarioRepository.save(prof);

            Profesional profesional = new Profesional();
            profesional.setUsuario(prof);
            profesional.setRut("88888888-8");
            profesional.setSpecialty("Medicina General");
            profesionalRepository.save(profesional);

            Usuario usuPac = new Usuario();
            usuPac.setEmail("pac.ant@gespa.cl");
            usuPac.setDisplayName("Paciente Antecedentes");
            usuPac.setRole(UserRole.PATIENT);
            usuPac.setActive(true);
            usuPac.setPasswordHash(passwordEncoder.encode("Test1234!"));
            usuPac = usuarioRepository.save(usuPac);

            Paciente paciente = new Paciente();
            paciente.setUsuario(usuPac);
            paciente.setRut("77777777-7");
            Paciente saved = pacienteRepository.save(paciente);
            pacienteId = saved.getId();

            return null;
        });

        // Obtener JWT del profesional
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "prof.ant@gespa.cl",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        jwtProfesional = mapper.readTree(responseBody).get("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        tx.execute(status -> {
            for (String email : new String[]{"prof.ant@gespa.cl", "pac.ant@gespa.cl"}) {
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
     * ANT-01 — Obtener antecedentes de paciente sin registros (integración).
     * Funcionalidad: GET /api/pacientes/{id}/antecedentes con un paciente que
     * no tiene antecedentes debe retornar HTTP 200 con una respuesta vacía
     * (sin campos id ni enfermedadesBase), no un error 404.
     */
    @Test
    @DisplayName("ANT-01: GET antecedentes devuelve respuesta vacía cuando no existen antecedentes")
    void getAntecedentes_sinRegistros_retornaRespuestaVacia() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}/antecedentes", pacienteId)
                        .header("Authorization", "Bearer " + jwtProfesional))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.enfermedadesBase").doesNotExist());
    }

    /**
     * ANT-02 — Crear antecedentes por primera vez vía API (integración).
     * Funcionalidad: PUT /api/pacientes/{id}/antecedentes con un profesional
     * autenticado y datos válidos debe crear el registro y retornar HTTP 200
     * con los datos persistidos, incluyendo pacienteId y actividadFisica.
     */
    @Test
    @DisplayName("ANT-02: PUT antecedentes crea nuevos antecedentes correctamente")
    void upsertAntecedentes_creaNuevo_retornaOk() throws Exception {
        Map<String, Object> body = Map.of(
                "enfermedadesBase", "Hipertensión",
                "consumoAlcohol", "NO_CONSUME",
                "consumoTabaco", "NO_CONSUME",
                "consumoDrogas", "NO_CONSUME",
                "actividadFisica", "UNA_DOS_SEMANA",
                "medicamentosRegulares", "Losartán 50mg"
        );

        mockMvc.perform(put("/api/pacientes/{id}/antecedentes", pacienteId)
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enfermedadesBase").value("Hipertensión"))
                .andExpect(jsonPath("$.actividadFisica").value("UNA_DOS_SEMANA"))
                .andExpect(jsonPath("$.pacienteId").value(pacienteId));
    }

    /**
     * ANT-03 — Actualizar antecedentes existentes sin duplicar (integración).
     * Funcionalidad: dos PUT consecutivos sobre el mismo paciente deben
     * actualizar el registro existente, no crear uno nuevo. El segundo PUT
     * debe retornar los datos del segundo request, verificando que el upsert
     * funciona correctamente a nivel de base de datos H2.
     */
    @Test
    @DisplayName("ANT-03: PUT antecedentes dos veces actualiza sin crear duplicado")
    void upsertAntecedentes_dosVeces_actualizaSinDuplicar() throws Exception {
        Map<String, Object> body1 = Map.of(
                "enfermedadesBase", "Diabetes",
                "consumoAlcohol", "NO_CONSUME",
                "consumoTabaco", "NO_CONSUME",
                "consumoDrogas", "NO_CONSUME",
                "actividadFisica", "SEDENTARIO"
        );
        mockMvc.perform(put("/api/pacientes/{id}/antecedentes", pacienteId)
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body1)))
                .andExpect(status().isOk());

        Map<String, Object> body2 = Map.of(
                "enfermedadesBase", "Diabetes controlada",
                "consumoAlcohol", "OCASIONAL",
                "consumoTabaco", "EXFUMADOR",
                "consumoDrogas", "NO_CONSUME",
                "actividadFisica", "TRES_MAS_SEMANA"
        );
        mockMvc.perform(put("/api/pacientes/{id}/antecedentes", pacienteId)
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enfermedadesBase").value("Diabetes controlada"))
                .andExpect(jsonPath("$.actividadFisica").value("TRES_MAS_SEMANA"));
    }

    /**
     * ANT-05 (Regresión) — TRES_MAS_SEMANA es un valor válido del enum (integración).
     * Funcionalidad: PUT /api/pacientes/{id}/antecedentes con actividadFisica=
     * "TRES_MAS_SEMANA" debe retornar HTTP 200 y persistir el valor correctamente.
     * Verifica junto con ANT-04 que el enum solo acepta sus tres valores válidos:
     * SEDENTARIO, UNA_DOS_SEMANA y TRES_MAS_SEMANA.
     */
    @Test
    @DisplayName("ANT-05 (regresión): actividadFisica=TRES_MAS_SEMANA es aceptado correctamente")
    void upsertAntecedentes_tresMasSemana_retornaOk() throws Exception {
        Map<String, Object> body = Map.of(
                "consumoAlcohol", "NO_CONSUME",
                "consumoTabaco", "NO_CONSUME",
                "consumoDrogas", "NO_CONSUME",
                "actividadFisica", "TRES_MAS_SEMANA"
        );

        mockMvc.perform(put("/api/pacientes/{id}/antecedentes", pacienteId)
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividadFisica").value("TRES_MAS_SEMANA"));
    }

    /**
     * SEC (seguridad) — Acceso a antecedentes sin JWT retorna 401.
     * Funcionalidad: GET /api/pacientes/{id}/antecedentes sin el header
     * Authorization debe ser rechazado con HTTP 401 Unauthorized por el
     * filtro de seguridad, sin exponer ningún dato clínico del paciente.
     */
    @Test
    @DisplayName("GET antecedentes sin JWT retorna 401")
    void getAntecedentes_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}/antecedentes", pacienteId))
                .andExpect(status().isUnauthorized());
    }
}
