package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.CitaResponse;
import com.tbtha.gespa_backend.dtos.CreateCitaRequest;
import com.tbtha.gespa_backend.dtos.UpdateCitaRequest;
import com.tbtha.gespa_backend.dtos.UpdateEstadoCitaRequest;
import com.tbtha.gespa_backend.services.CitaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    public ResponseEntity<CitaResponse> create(@Valid @RequestBody CreateCitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CitaResponse>> findByProfesional(@RequestParam Long profesionalId,
                                                                @RequestParam(required = false)
                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                OffsetDateTime desde,
                                                                @RequestParam(required = false)
                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                OffsetDateTime hasta) {
        return ResponseEntity.ok(citaService.findAgendaByProfesional(profesionalId, desde, hasta));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<CitaResponse>> findByPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(citaService.findByPaciente(pacienteId));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CitaResponse> updateEstado(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateEstadoCitaRequest request) {
        return ResponseEntity.ok(citaService.updateEstado(id, request.status()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CitaResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateCitaRequest request) {
        return ResponseEntity.ok(citaService.update(id, request));
    }
}
