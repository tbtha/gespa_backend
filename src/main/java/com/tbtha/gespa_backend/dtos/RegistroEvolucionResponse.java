package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.TipoIndicador;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class RegistroEvolucionResponse {

    private Long id;
    private Long pacienteId;
    private Long citaId;
    private TipoIndicador tipoIndicador;
    private String etiqueta;
    private BigDecimal valor;
    private String unidad;
    private LocalDate fechaRegistro;
    private String observacion;
    private Instant createdAt;

    // ── Constructor estático de mapeo ────────────────────────────────────────

    public static RegistroEvolucionResponse from(
            com.tbtha.gespa_backend.entities.RegistroEvolucion r) {
        RegistroEvolucionResponse dto = new RegistroEvolucionResponse();
        dto.id              = r.getId();
        dto.pacienteId      = r.getPaciente().getId();
        dto.citaId          = r.getCita() != null ? r.getCita().getId() : null;
        dto.tipoIndicador   = r.getTipoIndicador();
        dto.etiqueta        = r.getEtiqueta();
        dto.valor           = r.getValor();
        dto.unidad          = r.getUnidad();
        dto.fechaRegistro   = r.getFechaRegistro();
        dto.observacion     = r.getObservacion();
        dto.createdAt       = r.getCreatedAt();
        return dto;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public Long getCitaId() { return citaId; }
    public TipoIndicador getTipoIndicador() { return tipoIndicador; }
    public String getEtiqueta() { return etiqueta; }
    public BigDecimal getValor() { return valor; }
    public String getUnidad() { return unidad; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public String getObservacion() { return observacion; }
    public Instant getCreatedAt() { return createdAt; }
}
