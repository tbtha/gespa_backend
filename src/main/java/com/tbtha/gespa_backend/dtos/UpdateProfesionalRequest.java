package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.validation.ValidRut;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfesionalRequest(
        @NotBlank String displayName,
        @NotBlank String specialty,
        @NotBlank @ValidRut String rut,
        String phone,
        String address,
        String institucion,
        String descripcion
) {
}
