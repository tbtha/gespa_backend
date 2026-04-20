package com.tbtha.gespa_backend.dtos.dashboard;

public record DashboardKpisResponse(
        long pacientesActivos,
        long citasHoy,
        double porcentajeCompletadas,
        double porcentajeAusentismo
) {
}
