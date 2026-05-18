package com.tbtha.gespa_backend.dtos;

public record CheckEmailResponse(
        boolean exists,
        boolean hasPatientProfile,
        boolean hasProfessionalProfile
) {
}
