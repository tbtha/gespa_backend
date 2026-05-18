package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckEmailRequest(
        @Email @NotBlank String email
) {
}
