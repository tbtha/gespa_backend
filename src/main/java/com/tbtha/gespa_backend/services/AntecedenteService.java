package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.AntecedentesResponse;
import com.tbtha.gespa_backend.dtos.UpsertAntecedentesRequest;
import com.tbtha.gespa_backend.entities.Antecedente;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.AntecedenteRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AntecedenteService {

    private final AntecedenteRepository antecedenteRepository;
    private final PacienteRepository pacienteRepository;

    public AntecedenteService(AntecedenteRepository antecedenteRepository,
                              PacienteRepository pacienteRepository) {
        this.antecedenteRepository = antecedenteRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @Transactional(readOnly = true)
    public AntecedentesResponse findByPaciente(Long pacienteId) {
        Antecedente antecedente = antecedenteRepository.findByPacienteId(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Antecedentes no encontrados para el paciente"));
        return toResponse(antecedente);
    }

    @Transactional
    public AntecedentesResponse upsert(Long pacienteId, UpsertAntecedentesRequest request) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Antecedente antecedente = antecedenteRepository.findByPacienteId(pacienteId)
                .orElseGet(() -> {
                    Antecedente nuevo = new Antecedente();
                    nuevo.setPaciente(paciente);
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
}
