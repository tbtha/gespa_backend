package com.tbtha.gespa_backend.dtos.dashboard;

import java.util.List;

public record DashboardProfesionalResponse(
        DashboardKpisResponse kpis,
        List<DashboardCitaItemResponse> citasProximas,
        List<DashboardSerieItemResponse> citasPorSemana,
        List<DashboardTipoAtencionItemResponse> tiposAtencion,
        List<DashboardSerieItemResponse> citasPorAnio
) {
}
