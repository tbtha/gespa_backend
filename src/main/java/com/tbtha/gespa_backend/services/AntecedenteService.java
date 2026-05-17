package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.AntecedentesResponse;
import com.tbtha.gespa_backend.dtos.UpsertAntecedentesRequest;
import com.tbtha.gespa_backend.entities.Antecedente;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.AntecedenteRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AntecedenteService {

    private final AntecedenteRepository antecedenteRepository;
    private final PacienteRepository pacienteRepository;
        private final ProfesionalRepository profesionalRepository;
        private final AccessControlService accessControlService;

    public AntecedenteService(AntecedenteRepository antecedenteRepository,
                      PacienteRepository pacienteRepository,
                      ProfesionalRepository profesionalRepository,
                      AccessControlService accessControlService) {
        this.antecedenteRepository = antecedenteRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public AntecedentesResponse findByPaciente(Long pacienteId) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new ResourceNotFoundException("Paciente no encontrado");
        }
        Profesional profesional = resolveCurrentProfessional();

        return antecedenteRepository.findByPacienteIdAndProfesionalId(pacienteId, profesional.getId())
            .map(this::toResponse)
            .orElseGet(() -> emptyResponse(pacienteId));
    }

    @Transactional
    public AntecedentesResponse upsert(Long pacienteId, UpsertAntecedentesRequest request) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Profesional profesional = resolveCurrentProfessional();

        Antecedente antecedente = antecedenteRepository.findByPacienteIdAndProfesionalId(pacienteId, profesional.getId())
                .orElseGet(() -> {
                    Antecedente nuevo = new Antecedente();
                    nuevo.setPaciente(paciente);
                nuevo.setProfesional(profesional);
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

        return toResponse(antecedenteRepository.save(antecedente));
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

    private Profesional resolveCurrentProfessional() {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() != UserRole.PROFESSIONAL) {
            throw new org.springframework.security.access.AccessDeniedException("Solo el profesional autenticado puede acceder a antecedentes clínicos");
        }

        return profesionalRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
    }
}
