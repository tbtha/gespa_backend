package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.CreatePacienteRequest;
import com.tbtha.gespa_backend.dtos.PacienteResponse;
import com.tbtha.gespa_backend.dtos.PagedResponse;
import com.tbtha.gespa_backend.dtos.UpdatePacienteRequest;
import com.tbtha.gespa_backend.entities.enums.EstadoCivil;
import com.tbtha.gespa_backend.entities.enums.Prevision;
import com.tbtha.gespa_backend.services.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> create(@Valid @RequestBody CreatePacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.create(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PacienteResponse>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long professionalId,
            @RequestParam(required = false) Prevision prevision,
            @RequestParam(required = false) EstadoCivil estadoCivil,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(
                pacienteService.findAll(q, professionalId, prevision, estadoCivil, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdatePacienteRequest request) {
        return ResponseEntity.ok(pacienteService.update(id, request));
    }
}
