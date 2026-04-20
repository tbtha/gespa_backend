package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.TipoIndicador;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateRegistroEvolucionRequest {

    @NotNull
    private TipoIndicador tipoIndicador;

    /** Obligatorio cuando tipoIndicador = OTRO. */
    private String etiqueta;

    @NotNull
    private BigDecimal valor;

    /** Si se omite, se usa la unidad estándar del indicador (ver servicio). */
    private String unidad;

    @NotNull
    private LocalDate fechaRegistro;

    /** ID de cita asociada (opcional). */
    private Long citaId;

    private String observacion;

    // ── Getters / Setters ────────────────────────────────────────────────────

    public TipoIndicador getTipoIndicador() { return tipoIndicador; }
    public void setTipoIndicador(TipoIndicador tipoIndicador) { this.tipoIndicador = tipoIndicador; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Long getCitaId() { return citaId; }
    public void setCitaId(Long citaId) { this.citaId = citaId; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
