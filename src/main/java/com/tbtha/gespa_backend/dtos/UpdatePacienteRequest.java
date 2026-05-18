package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.EstadoCivil;
import com.tbtha.gespa_backend.entities.enums.Gender;
import com.tbtha.gespa_backend.entities.enums.Prevision;

import java.time.LocalDate;

public record UpdatePacienteRequest(
        String email,
        String displayName,
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
