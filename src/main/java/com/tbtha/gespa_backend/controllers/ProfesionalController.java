package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.CreateProfesionalRequest;
import com.tbtha.gespa_backend.dtos.ProfesionalResponse;
import com.tbtha.gespa_backend.dtos.UpdateProfesionalRequest;
import com.tbtha.gespa_backend.services.ProfesionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalController {

    private final ProfesionalService profesionalService;

    public ProfesionalController(ProfesionalService profesionalService) {
        this.profesionalService = profesionalService;
    }

    @PostMapping
    public ResponseEntity<ProfesionalResponse> create(@Valid @RequestBody CreateProfesionalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesionalService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProfesionalResponse>> findAll() {
        return ResponseEntity.ok(profesionalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfesionalResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(profesionalService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfesionalResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateProfesionalRequest request) {
        return ResponseEntity.ok(profesionalService.update(id, request));
    }
}
