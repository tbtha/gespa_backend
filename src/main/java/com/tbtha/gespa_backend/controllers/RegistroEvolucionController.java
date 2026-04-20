package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.CreateRegistroEvolucionRequest;
import com.tbtha.gespa_backend.dtos.RegistroEvolucionResponse;
import com.tbtha.gespa_backend.entities.enums.TipoIndicador;
import com.tbtha.gespa_backend.services.RegistroEvolucionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Evolución", description = "Registros de indicadores clínicos del paciente")
public class RegistroEvolucionController {

    private final RegistroEvolucionService service;

    public RegistroEvolucionController(RegistroEvolucionService service) {
        this.service = service;
    }

    // ── POST /api/pacientes/{id}/evolucion ───────────────────────────────────

    @PostMapping("/pacientes/{pacienteId}/evolucion")
    @Operation(summary = "Registrar un nuevo indicador clínico para un paciente")
    public ResponseEntity<RegistroEvolucionResponse> create(
            @PathVariable Long pacienteId,
            @Valid @RequestBody CreateRegistroEvolucionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(pacienteId, req));
    }

    // ── GET /api/pacientes/{id}/evolucion ────────────────────────────────────

    @GetMapping("/pacientes/{pacienteId}/evolucion")
    @Operation(summary = "Listar todos los registros de evolución de un paciente")
    public List<RegistroEvolucionResponse> findByPaciente(
            @PathVariable Long pacienteId) {
        return service.findByPaciente(pacienteId);
    }

    // ── GET /api/pacientes/{id}/evolucion/resumen ────────────────────────────

    @GetMapping("/pacientes/{pacienteId}/evolucion/resumen")
    @Operation(summary = "Último valor registrado por cada indicador (vista resumen ficha)")
    public List<RegistroEvolucionResponse> resumen(
            @PathVariable Long pacienteId) {
        return service.findUltimoPorIndicador(pacienteId);
    }

    // ── GET /api/pacientes/{id}/evolucion/serie ──────────────────────────────

    @GetMapping("/pacientes/{pacienteId}/evolucion/serie")
    @Operation(summary = "Serie temporal de un indicador para gráficos",
               description = "Parámetros: tipo (TipoIndicador), desde (yyyy-MM-dd), hasta (yyyy-MM-dd)")
    public List<RegistroEvolucionResponse> serie(
            @PathVariable Long pacienteId,
            @RequestParam TipoIndicador tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.findSerie(pacienteId, tipo, desde, hasta);
    }

    // ── GET /api/citas/{id}/evolucion ────────────────────────────────────────

    @GetMapping("/citas/{citaId}/evolucion")
    @Operation(summary = "Registros de evolución asociados a una cita específica")
    public List<RegistroEvolucionResponse> findByCita(
            @PathVariable Long citaId) {
        return service.findByCita(citaId);
    }

    // ── DELETE /api/evolucion/{id} ───────────────────────────────────────────

    @DeleteMapping("/evolucion/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un registro de evolución")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
