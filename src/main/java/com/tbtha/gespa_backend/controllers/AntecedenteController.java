package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.AntecedentesResponse;
import com.tbtha.gespa_backend.dtos.UpsertAntecedentesRequest;
import com.tbtha.gespa_backend.services.AntecedenteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes/{pacienteId}/antecedentes")
public class AntecedenteController {

    private final AntecedenteService antecedenteService;

    public AntecedenteController(AntecedenteService antecedenteService) {
        this.antecedenteService = antecedenteService;
    }

    @GetMapping
    public ResponseEntity<AntecedentesResponse> findByPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(antecedenteService.findByPaciente(pacienteId));
    }

    @PutMapping
    public ResponseEntity<AntecedentesResponse> upsert(@PathVariable Long pacienteId,
                                                       @Valid @RequestBody UpsertAntecedentesRequest request) {
        return ResponseEntity.ok(antecedenteService.upsert(pacienteId, request));
    }
}
