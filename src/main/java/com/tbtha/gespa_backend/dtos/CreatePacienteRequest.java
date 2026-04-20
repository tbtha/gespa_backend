package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.EstadoCivil;
import com.tbtha.gespa_backend.entities.enums.Gender;
import com.tbtha.gespa_backend.entities.enums.Prevision;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePacienteRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank String displayName,
        @NotNull Long professionalId,
        @NotBlank String rut,
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
