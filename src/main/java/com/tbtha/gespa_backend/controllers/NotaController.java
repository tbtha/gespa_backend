package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.CreateNotaRequest;
import com.tbtha.gespa_backend.dtos.NotaResponse;
import com.tbtha.gespa_backend.dtos.UpdateNotaRequest;
import com.tbtha.gespa_backend.services.NotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NotaController {

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    @PostMapping("/pacientes/{pacienteId}/notas")
    public ResponseEntity<NotaResponse> create(@PathVariable Long pacienteId,
                                               @Valid @RequestBody CreateNotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notaService.create(pacienteId, request));
    }

    @GetMapping("/pacientes/{pacienteId}/notas")
    public ResponseEntity<List<NotaResponse>> findByPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(notaService.findByPaciente(pacienteId));
    }

    @GetMapping("/notas/{id}")
    public ResponseEntity<NotaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(notaService.findById(id));
    }

    @PutMapping("/notas/{id}")
    public ResponseEntity<NotaResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateNotaRequest request) {
        return ResponseEntity.ok(notaService.update(id, request));
    }

    @DeleteMapping("/notas/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
