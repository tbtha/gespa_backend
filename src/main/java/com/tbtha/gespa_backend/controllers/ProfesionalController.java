package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.CreateHorarioRequest;
import com.tbtha.gespa_backend.dtos.CreateProfesionalRequest;
import com.tbtha.gespa_backend.dtos.HorarioDisponibleResponse;
import com.tbtha.gespa_backend.dtos.ProfesionalResponse;
import com.tbtha.gespa_backend.dtos.SlotDisponibleResponse;
import com.tbtha.gespa_backend.dtos.SpecialtyResponse;
import com.tbtha.gespa_backend.dtos.UpdateProfesionalRequest;
import com.tbtha.gespa_backend.services.HorarioService;
import com.tbtha.gespa_backend.services.ProfesionalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalController {

    private final ProfesionalService profesionalService;
    private final HorarioService horarioService;

    public ProfesionalController(ProfesionalService profesionalService, HorarioService horarioService) {
        this.profesionalService = profesionalService;
        this.horarioService = horarioService;
    }

    @PostMapping
    public ResponseEntity<ProfesionalResponse> create(@Valid @RequestBody CreateProfesionalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesionalService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProfesionalResponse>> findAll() {
        return ResponseEntity.ok(profesionalService.findAll());
    }

    @GetMapping("/specialties")
    public ResponseEntity<List<SpecialtyResponse>> listSpecialties() {
        return ResponseEntity.ok(profesionalService.listSpecialties());
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

    // --- Horarios disponibles ---

    @PostMapping("/{id}/horarios")
    public ResponseEntity<HorarioDisponibleResponse> createHorario(
            @PathVariable Long id,
            @Valid @RequestBody CreateHorarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.create(id, request));
    }

    @GetMapping("/{id}/horarios")
    public ResponseEntity<List<HorarioDisponibleResponse>> listHorarios(@PathVariable Long id) {
        return ResponseEntity.ok(horarioService.listByProfesional(id));
    }

    @DeleteMapping("/{id}/horarios/{horarioId}")
    public ResponseEntity<Void> deleteHorario(@PathVariable Long id, @PathVariable Long horarioId) {
        horarioService.delete(id, horarioId);
        return ResponseEntity.noContent().build();
    }

    /** Public endpoint: available slots for a professional on a date (YYYY-MM-DD) */
    @GetMapping("/{id}/slots")
    public ResponseEntity<List<SlotDisponibleResponse>> getSlots(
            @PathVariable Long id,
            @RequestParam String fecha) {
        return ResponseEntity.ok(horarioService.getSlots(id, fecha));
    }

    /** Public endpoint: available slots across ALL professionals on a date */
    @GetMapping("/slots")
    public ResponseEntity<List<SlotDisponibleResponse>> getAllSlots(@RequestParam String fecha) {
        return ResponseEntity.ok(horarioService.getAllSlots(fecha));
    }
}
