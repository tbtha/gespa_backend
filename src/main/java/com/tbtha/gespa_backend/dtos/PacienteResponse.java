package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.EstadoCivil;
import com.tbtha.gespa_backend.entities.enums.Gender;
import com.tbtha.gespa_backend.entities.enums.Prevision;

import java.time.LocalDate;

public record PacienteResponse(
        Long id,
        String email,
        String displayName,
        Long professionalId,
        String professionalName,
        String professionalSpecialty,
        String rut,
        LocalDate birthdate,
        Gender gender,
        Prevision prevision,
        EstadoCivil estadoCivil,
        String ocupacion,
        String phone,
        String address,
        String emergencyContactName,
        String emergencyContactPhone
) {
}
