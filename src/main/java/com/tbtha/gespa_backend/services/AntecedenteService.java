package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.AntecedentesResponse;
import com.tbtha.gespa_backend.dtos.UpsertAntecedentesRequest;
import com.tbtha.gespa_backend.entities.Antecedente;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.AntecedenteRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AntecedenteService {

    private final AntecedenteRepository antecedenteRepository;
    private final PacienteRepository pacienteRepository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    public AntecedenteService(AntecedenteRepository antecedenteRepository,
                              PacienteRepository pacienteRepository,
                              AccessControlService accessControlService,
                              AuditService auditService) {
        this.antecedenteRepository = antecedenteRepository;
        this.pacienteRepository = pacienteRepository;
        this.accessControlService = accessControlService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AntecedentesResponse findByPaciente(Long pacienteId) {
        accessControlService.assertCanAccessPaciente(pacienteId);

        return antecedenteRepository.findFirstByPacienteIdOrderByUpdatedAtDesc(pacienteId)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(pacienteId));
    }

    @Transactional
    public AntecedentesResponse upsert(Long pacienteId, UpsertAntecedentesRequest request) {
        if (accessControlService.currentUserRole() == UserRole.PATIENT) {
            throw new org.springframework.security.access.AccessDeniedException("El paciente no puede editar antecedentes clínicos");
        }

        accessControlService.assertCanAccessPaciente(pacienteId);

        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Antecedente antecedente = antecedenteRepository.findFirstByPacienteIdOrderByUpdatedAtDesc(pacienteId)
                .orElseGet(() -> {
                    Antecedente nuevo = new Antecedente();
                    nuevo.setPaciente(paciente);
                    nuevo.setProfesional(null);
                    return nuevo;
                });

        antecedente.setEnfermedadesBase(request.enfermedadesBase());
        antecedente.setEnfermedadesOtros(request.enfermedadesOtros());
        antecedente.setConsumoAlcohol(request.consumoAlcohol());
        antecedente.setConsumoTabaco(request.consumoTabaco());
        antecedente.setConsumoDrogas(request.consumoDrogas());
        antecedente.setActividadFisica(request.actividadFisica());
        antecedente.setOperacionesPrevias(request.operacionesPrevias());
        antecedente.setMedicamentosRegulares(request.medicamentosRegulares());
        antecedente.setOtrosAntecedentes(request.otrosAntecedentes());

        Antecedente saved = antecedenteRepository.save(antecedente);
        auditService.register("UPSERT_ANTECEDENTE", "antecedentes", saved.getId(), "pacienteId=" + pacienteId);
        return toResponse(saved);
    }

    private AntecedentesResponse toResponse(Antecedente antecedente) {
        return new AntecedentesResponse(
                antecedente.getId(),
                antecedente.getPaciente().getId(),
                antecedente.getEnfermedadesBase(),
                antecedente.getEnfermedadesOtros(),
                antecedente.getConsumoAlcohol(),
                antecedente.getConsumoTabaco(),
                antecedente.getConsumoDrogas(),
                antecedente.getActividadFisica(),
                antecedente.getOperacionesPrevias(),
                antecedente.getMedicamentosRegulares(),
                antecedente.getOtrosAntecedentes(),
                antecedente.getUpdatedAt()
        );
    }

    private AntecedentesResponse emptyResponse(Long pacienteId) {
        return new AntecedentesResponse(
                null,
                pacienteId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

}
