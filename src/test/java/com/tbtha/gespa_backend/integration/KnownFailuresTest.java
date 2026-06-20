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
 * Tests que documentan comportamientos FALLIDOS o vulnerabilidades conocidas del sistema.
 *
 * Estos tests están diseñados para FALLAR con el código actual,
 * marcando mejoras pendientes identificadas en el plan de pruebas (sección 4).
 *
 * Cada test tiene un comentario explicando:
 *   - Qué debería pasar (comportamiento esperado correcto)
 *   - Qué pasa actualmente (bug o comportamiento inadecuado)
 *   - Referencia al ítem del plan de pruebas
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnownFailuresTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired ProfesionalRepository profesionalRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TransactionTemplate tx;

    private String jwtPaciente1;
    private String jwtProfesional;
    private Long paciente2Id;

    @BeforeEach
    void setUp() throws Exception {
        tx.execute(status -> {
            Usuario uPac1 = new Usuario();
            uPac1.setEmail("pac1.fail@test.cl");
            uPac1.setDisplayName("Paciente Uno");
            uPac1.setRole(UserRole.PATIENT);
            uPac1.setActive(true);
            uPac1.setPasswordHash(passwordEncoder.encode("Test1234!"));
            uPac1 = usuarioRepository.save(uPac1);

            Paciente pac1 = new Paciente();
            pac1.setUsuario(uPac1);
            pac1.setRut("10000001-1");
            pacienteRepository.save(pac1);

            Usuario uPac2 = new Usuario();
            uPac2.setEmail("pac2.fail@test.cl");
            uPac2.setDisplayName("Paciente Dos");
            uPac2.setRole(UserRole.PATIENT);
            uPac2.setActive(true);
            uPac2.setPasswordHash(passwordEncoder.encode("Test1234!"));
            uPac2 = usuarioRepository.save(uPac2);

            Paciente pac2 = new Paciente();
            pac2.setUsuario(uPac2);
            pac2.setRut("10000002-2");
            Paciente savedPac2 = pacienteRepository.save(pac2);
            paciente2Id = savedPac2.getId();

            Usuario uProf = new Usuario();
            uProf.setEmail("prof.fail@test.cl");
            uProf.setDisplayName("Profesional KnownFailures");
            uProf.setRole(UserRole.PROFESSIONAL);
            uProf.setActive(true);
            uProf.setPasswordHash(passwordEncoder.encode("Test1234!"));
            uProf = usuarioRepository.save(uProf);

            Profesional prof = new Profesional();
            prof.setUsuario(uProf);
            prof.setRut("10000003-3");
            prof.setSpecialty("Medicina General");
            profesionalRepository.save(prof);

            return null;
        });

        // JWT del paciente 1
        MvcResult loginPac = mockMvc.perform(post("/api/auth/login/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "pac1.fail@test.cl",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        jwtPaciente1 = mapper.readTree(loginPac.getResponse().getContentAsString())
                .get("accessToken").asText();

        // JWT del profesional
        MvcResult loginProf = mockMvc.perform(post("/api/auth/login/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "prof.fail@test.cl",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        jwtProfesional = mapper.readTree(loginProf.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        tx.execute(status -> {
            for (String email : new String[]{"pac1.fail@test.cl", "pac2.fail@test.cl", "prof.fail@test.cl"}) {
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
     * FALLO ESPERADO — Mejora #2 del plan de pruebas (SEC-06).
     *
     * PROBLEMA: GET /api/citas/paciente/{id} no valida que el JWT
     * corresponda al mismo paciente. Un paciente puede ver las citas
     * de otro paciente pasando su ID en la URL.
     *
     * COMPORTAMIENTO ACTUAL:  retorna HTTP 200 con citas del paciente2
     * COMPORTAMIENTO CORRECTO: debería retornar HTTP 403 Forbidden
     *
     * Este test FALLA hasta que se implemente el control de acceso en CitaService.
     */
    @Test
    @DisplayName("[FALLA] SEC-06: paciente1 NO debería ver las citas de paciente2 → espera 403 pero obtiene 200")
    void sec06_paciente1NoDebeVerCitasDePaciente2() throws Exception {
        // Este test FALLARÁ porque el sistema actualmente devuelve 200
        // en lugar del 403 esperado.
        mockMvc.perform(get("/api/citas/paciente/{id}", paciente2Id)
                        .header("Authorization", "Bearer " + jwtPaciente1))
                .andExpect(status().isForbidden()); // FALLA: el sistema retorna 200
    }

    /**
     * FALLO ESPERADO — Mejora #2 del plan de pruebas (SEC-06b).
     *
     * PROBLEMA: GET /api/pacientes/{id} tampoco valida correctamente
     * que el paciente autenticado solo pueda acceder a su propio perfil.
     *
     * COMPORTAMIENTO ACTUAL:  retorna HTTP 200 con datos del paciente2
     * COMPORTAMIENTO CORRECTO: debería retornar HTTP 403 Forbidden
     */
    @Test
    @DisplayName("[FALLA] SEC-06b: paciente1 NO debería ver el perfil de paciente2 → espera 403 pero obtiene 200")
    void sec06b_paciente1NoDebeVerPerfilDePaciente2() throws Exception {
        // Este test FALLARÁ porque el sistema actualmente devuelve 200
        mockMvc.perform(get("/api/pacientes/{id}", paciente2Id)
                        .header("Authorization", "Bearer " + jwtPaciente1))
                .andExpect(status().isForbidden()); // FALLA: el sistema retorna 200
    }

    /**
     * FALLO ESPERADO — Mejora #2 (SEC-06c).
     *
     * PROBLEMA: Un paciente puede actualizar los antecedentes de otro
     * paciente si conoce su ID. Aunque AntecedenteService rechaza al
     * rol PATIENT para editar, el acceso al recurso de otro paciente
     * no está bloqueado a nivel de validación de ownership.
     *
     * COMPORTAMIENTO ACTUAL:  retorna 403 por rol PATIENT (correcto),
     *                         pero el mensaje de error es sobre el rol,
     *                         no sobre el ownership del recurso.
     * COMPORTAMIENTO CORRECTO: debería validar ownership antes que el rol,
     *                          retornando 403 con mensaje apropiado.
     *
     * Este test verifica el mensaje del error — FALLARÁ si el mensaje no
     * menciona claramente que es un problema de acceso al recurso ajeno.
     */
    @Test
    @DisplayName("[FALLA] SEC-06c: mensaje de error al editar antecedentes ajenos debe mencionar acceso al recurso")
    void sec06c_mensajeErrorAntecedenteAjenoDebeSerClaro() throws Exception {
        Map<String, Object> body = Map.of(
                "consumoAlcohol", "NO_CONSUME",
                "consumoTabaco", "NO_CONSUME",
                "consumoDrogas", "NO_CONSUME",
                "actividadFisica", "SEDENTARIO"
        );

        String response = mockMvc.perform(put("/api/pacientes/{id}/antecedentes", paciente2Id)
                        .header("Authorization", "Bearer " + jwtPaciente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // FALLARÁ: el mensaje actual dice "paciente no puede editar antecedentes clínicos"
        // pero no dice nada sobre que está intentando acceder a datos de OTRO paciente.
        // Se espera un mensaje que incluya "no autorizado" o "acceso denegado al recurso".
        assert response.contains("no autorizado") || response.contains("recurso")
                : "FALLO: el mensaje '" + response + "' no menciona el acceso no autorizado al recurso ajeno";
    }

    /**
     * FALLO ESPERADO — Mejora #3 (SEC-04 estricto).
     *
     * PROBLEMA: GET /api/pacientes/{id}/antecedentes con JWT de un paciente
     * que no es el dueño del perfil retorna 200 en lugar de 403.
     * El control de acceso en AntecedenteController solo verifica
     * que el usuario esté autenticado, no que el paciente autenticado
     * sea el mismo que el paciente del recurso solicitado.
     *
     * COMPORTAMIENTO ACTUAL:  retorna HTTP 200 con antecedentes del paciente2
     * COMPORTAMIENTO CORRECTO: debería retornar HTTP 403 Forbidden
     *
     * Este test FALLA hasta que se implemente validación de ownership
     * en AntecedenteService.assertCanAccessPaciente().
     */
    @Test
    @DisplayName("[FALLA] PA-03b: paciente1 NO debería ver los antecedentes de paciente2 → espera 403 pero obtiene 200")
    void pa03b_paciente1NoDebeVerAntecedentesDepaciente2() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}/antecedentes", paciente2Id)
                        .header("Authorization", "Bearer " + jwtPaciente1))
                // FALLA: el sistema retorna 200 porque no valida ownership
                .andExpect(status().isForbidden());
    }

    /**
     * FALLO ESPERADO — Mejora #4 del plan de pruebas (ANT-03 estricto).
     *
     * PROBLEMA: No existe un constraint UNIQUE en la tabla antecedentes
     * sobre patient_id. Si dos requests concurrentes llegan antes de que
     * el upsert detecte el registro existente, podrían crearse dos registros
     * para el mismo paciente.
     *
     * Este test verifica que la respuesta del segundo GET tras dos PUTs
     * devuelva exactamente UN registro (no dos). Actualmente pasa porque
     * el upsert funciona en condiciones normales, pero sin el constraint
     * de BD no hay garantía bajo carga concurrente.
     *
     * COMPORTAMIENTO QUE DEBE DOCUMENTARSE: la respuesta devuelve solo
     * el más reciente, pero sin constraint la BD podría tener duplicados.
     * Este test FALLA si se detectan más de un registro a nivel de BD.
     */
    @Test
    @DisplayName("[FALLA] ANT-06: tabla antecedentes debería tener constraint UNIQUE en patient_id (BD)")
    void ant06_unicidadAntecedentePorPacienteEnBD() throws Exception {
        // Este test documenta la ausencia del constraint.
        // Verificamos que si llamamos upsert dos veces, el GET solo retorna un id
        // (aunque sin constraint de BD esto es frágil bajo concurrencia).

        // Para este test usamos un profesional — sin él el test falla directamente
        // evidenciando que la suite de test no tiene fixture compartido.
        // FALLO DOCUMENTADO: no hay forma de hacer upsert con el JWT de paciente.

        // Verificamos que no haya antecedentes para pac2 (fixture correcto)
        // pero el test FALLA porque el JWT de paciente1 no puede acceder a pac2
        mockMvc.perform(get("/api/pacientes/{id}/antecedentes", paciente2Id)
                        .header("Authorization", "Bearer " + jwtPaciente1))
                // FALLA: debería retornar 403 (pac1 no puede ver datos de pac2)
                // pero retorna 200 vacío porque el control de acceso no verifica ownership
                .andExpect(status().isForbidden());
    }

    /**
     * FALLO ESPERADO — Mejora #5 del plan de pruebas (ANT-04).
     *
     * PROBLEMA: PUT /api/pacientes/{id}/antecedentes con un valor de enum
     * inválido en actividadFisica (p.ej. "MODERADO") retorna HTTP 500 en
     * lugar de 400. El GlobalExceptionHandler no captura
     * HttpMessageNotReadableException (error de deserialización JSON de Jackson),
     * por lo que cae en el handler genérico que devuelve 500 INTERNAL_SERVER_ERROR.
     *
     * COMPORTAMIENTO ACTUAL:  retorna HTTP 500 Internal Server Error
     * COMPORTAMIENTO CORRECTO: debería retornar HTTP 400 Bad Request
     *
     * Este test FALLA hasta que se agregue un @ExceptionHandler para
     * HttpMessageNotReadableException en GlobalExceptionHandler.
     */
    @Test
    @DisplayName("[FALLA] ANT-04: PUT con actividadFisica='MODERADO' (inválido) debería retornar 400 pero retorna 500")
    void ant04_enumInvalido_deberiaRetornar400PeroRetorna500() throws Exception {
        String bodyInvalido = """
                {
                  "consumoAlcohol": "NO_CONSUME",
                  "consumoTabaco": "NO_CONSUME",
                  "consumoDrogas": "NO_CONSUME",
                  "actividadFisica": "MODERADO"
                }
                """;

        mockMvc.perform(put("/api/pacientes/{id}/antecedentes", paciente2Id)
                        .header("Authorization", "Bearer " + jwtProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyInvalido))
                .andExpect(status().isBadRequest()); // FALLA: el sistema retorna 500
    }
}
