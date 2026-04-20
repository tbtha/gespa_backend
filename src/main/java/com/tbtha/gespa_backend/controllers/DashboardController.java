package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.dashboard.DashboardProfesionalResponse;
import com.tbtha.gespa_backend.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/profesional/{profesionalId}")
    public ResponseEntity<DashboardProfesionalResponse> getProfesionalDashboard(
            @PathVariable Long profesionalId,
            @RequestParam(defaultValue = "12") int semanas,
            @RequestParam(defaultValue = "20") int limitProximas) {
        return ResponseEntity.ok(dashboardService.getProfesionalDashboard(profesionalId, semanas, limitProximas));
    }
}
