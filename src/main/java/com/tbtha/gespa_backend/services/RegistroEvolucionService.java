package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CreateRegistroEvolucionRequest;
import com.tbtha.gespa_backend.dtos.RegistroEvolucionResponse;
import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.RegistroEvolucion;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.TipoIndicador;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.CitaRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.RegistroEvolucionRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RegistroEvolucionService {

    /**
     * Unidades estándar por indicador.
     * Si el cliente no envía unidad, se usa esta tabla como fallback.
     */
    private static final Map<TipoIndicador, String> UNIDADES_DEFAULT = Map.ofEntries(
            Map.entry(TipoIndicador.PESO,                    "kg"),
            Map.entry(TipoIndicador.TALLA,                   "cm"),
            Map.entry(TipoIndicador.IMC,                     "kg/m²"),
            Map.entry(TipoIndicador.CIRCUNFERENCIA_ABDOMINAL,"cm"),
            Map.entry(TipoIndicador.PRESION_SISTOLICA,       "mmHg"),
            Map.entry(TipoIndicador.PRESION_DIASTOLICA,      "mmHg"),
            Map.entry(TipoIndicador.FRECUENCIA_CARDIACA,     "bpm"),
            Map.entry(TipoIndicador.FRECUENCIA_RESPIRATORIA, "rpm"),
            Map.entry(TipoIndicador.TEMPERATURA,             "°C"),
            Map.entry(TipoIndicador.SATURACION_OXIGENO,      "%"),
            Map.entry(TipoIndicador.GLICEMIA,                "mg/dL"),
            Map.entry(TipoIndicador.HEMOGLOBINA_GLICOSILADA, "%"),
            Map.entry(TipoIndicador.COLESTEROL_TOTAL,        "mg/dL"),
            Map.entry(TipoIndicador.COLESTEROL_LDL,          "mg/dL"),
            Map.entry(TipoIndicador.COLESTEROL_HDL,          "mg/dL"),
            Map.entry(TipoIndicador.TRIGLICERIDOS,           "mg/dL"),
            Map.entry(TipoIndicador.CREATININA,              "mg/dL")
    );

    private final RegistroEvolucionRepository repo;
    private final PacienteRepository pacienteRepo;
    private final CitaRepository citaRepo;
    private final ProfesionalRepository profesionalRepo;
    private final AccessControlService accessControlService;

    public RegistroEvolucionService(RegistroEvolucionRepository repo,
                                    PacienteRepository pacienteRepo,
                                    CitaRepository citaRepo,
                                    ProfesionalRepository profesionalRepo,
                                    AccessControlService accessControlService) {
        this.repo        = repo;
        this.pacienteRepo = pacienteRepo;
        this.citaRepo    = citaRepo;
        this.profesionalRepo = profesionalRepo;
        this.accessControlService = accessControlService;
    }

    // ── Crear ────────────────────────────────────────────────────────────────

    @Transactional
    public RegistroEvolucionResponse create(Long pacienteId,
                                             CreateRegistroEvolucionRequest req) {
        Paciente paciente = pacienteRepo.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + pacienteId));
        Profesional profesional = resolveCurrentProfessional();

        RegistroEvolucion r = new RegistroEvolucion();
        r.setPaciente(paciente);
        r.setProfesional(profesional);
        r.setTipoIndicador(req.getTipoIndicador());
        r.setEtiqueta(req.getEtiqueta());
        r.setValor(req.getValor());
        r.setFechaRegistro(req.getFechaRegistro());
        r.setObservacion(req.getObservacion());

        // Unidad: usar la enviada o la default del indicador
        String unidad = (req.getUnidad() != null && !req.getUnidad().isBlank())
                ? req.getUnidad()
                : UNIDADES_DEFAULT.getOrDefault(req.getTipoIndicador(), "");
        r.setUnidad(unidad);

        // Asociar cita si se proporciona
        if (req.getCitaId() != null) {
            Cita cita = citaRepo.findById(req.getCitaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + req.getCitaId()));
            if (!cita.getProfesional().getId().equals(profesional.getId())) {
                throw new ConflictException("La cita no pertenece al profesional autenticado");
            }
            if (!cita.getPaciente().getId().equals(paciente.getId())) {
                throw new ConflictException("La cita no pertenece al paciente indicado");
            }
            r.setCita(cita);
        }

        return RegistroEvolucionResponse.from(repo.save(r));
    }

    // ── Listar todos los registros de un paciente ────────────────────────────

    public List<RegistroEvolucionResponse> findByPaciente(Long pacienteId) {
        if (!pacienteRepo.existsById(pacienteId)) {
            throw new ResourceNotFoundException("Paciente no encontrado: " + pacienteId);
        }
        return repo.findByPacienteIdAndProfesionalIdOrderByFechaRegistroDesc(pacienteId, resolveCurrentProfessionalId())
                .stream().map(RegistroEvolucionResponse::from).toList();
    }

    // ── Serie temporal de un indicador (para gráficos) ───────────────────────

    public List<RegistroEvolucionResponse> findSerie(Long pacienteId,
                                                      TipoIndicador tipo,
                                                      LocalDate desde,
                                                      LocalDate hasta) {
        if (!pacienteRepo.existsById(pacienteId)) {
            throw new ResourceNotFoundException("Paciente no encontrado: " + pacienteId);
        }
        LocalDate d = (desde != null) ? desde : LocalDate.of(1900, 1, 1);
        LocalDate h = (hasta != null) ? hasta : LocalDate.now();
        return repo.findByPacienteAndIndicadorEnRango(pacienteId, resolveCurrentProfessionalId(), tipo, d, h)
                .stream().map(RegistroEvolucionResponse::from).toList();
    }

    // ── Registros por cita ───────────────────────────────────────────────────

    public List<RegistroEvolucionResponse> findByCita(Long citaId) {
        return repo.findByCitaIdAndProfesionalIdOrderByTipoIndicadorAsc(citaId, resolveCurrentProfessionalId())
                .stream().map(RegistroEvolucionResponse::from).toList();
    }

    // ── Último valor por indicador (resumen ficha) ───────────────────────────

    public List<RegistroEvolucionResponse> findUltimoPorIndicador(Long pacienteId) {
        if (!pacienteRepo.existsById(pacienteId)) {
            throw new ResourceNotFoundException("Paciente no encontrado: " + pacienteId);
        }
        return repo.findUltimoPorIndicador(pacienteId, resolveCurrentProfessionalId())
                .stream().map(RegistroEvolucionResponse::from).toList();
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        RegistroEvolucion registro = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de evolución no encontrado: " + id));

        if (!registro.getProfesional().getId().equals(resolveCurrentProfessionalId())) {
            throw new org.springframework.security.access.AccessDeniedException("No puedes eliminar registros creados por otro profesional");
        }

        repo.delete(registro);
    }

    private Profesional resolveCurrentProfessional() {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() != UserRole.PROFESSIONAL) {
            throw new org.springframework.security.access.AccessDeniedException("Solo el profesional autenticado puede acceder a evolución clínica");
        }

        return profesionalRepo.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
    }

    private Long resolveCurrentProfessionalId() {
        return resolveCurrentProfessional().getId();
    }
}
