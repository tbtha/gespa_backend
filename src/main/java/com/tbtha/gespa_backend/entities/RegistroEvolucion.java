package com.tbtha.gespa_backend.entities;

import com.tbtha.gespa_backend.entities.enums.TipoIndicador;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Registro de un indicador clínico en la evolución de un paciente.
 * El diseño es extensible: cada fila almacena UN indicador con su valor numérico
 * y unidad. Para agregar nuevos indicadores basta con añadir un valor al enum
 * TipoIndicador (o usar OTRO + etiqueta).
 */
@Entity
@Table(name = "registros_evolucion", indexes = {
    @Index(name = "idx_evol_paciente_profesional_tipo_fecha", columnList = "paciente_id, professional_id, tipo_indicador, fecha_registro")
})
public class RegistroEvolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private Profesional profesional;

    /** Cita a la que pertenece este registro (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_indicador", nullable = false, length = 40)
    private TipoIndicador tipoIndicador;

    /**
     * Cuando tipoIndicador = OTRO, este campo describe qué se midió.
     * Para los demás tipos es opcional (puede usarse como sub-etiqueta).
     */
    @Column(name = "etiqueta", length = 100)
    private String etiqueta;

    /** Valor numérico del indicador (ej. 75.5 para peso en kg). */
    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /** Unidad de medida (ej. "kg", "mmHg", "mg/dL", "bpm"). */
    @Column(name = "unidad", length = 20)
    private String unidad;

    /** Fecha en que se tomó la medición (puede diferir del día de registro). */
    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    /** Observaciones o contexto libre del profesional. */
    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ── Constructores ────────────────────────────────────────────────────────

    public RegistroEvolucion() {}

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Profesional getProfesional() { return profesional; }
    public void setProfesional(Profesional profesional) { this.profesional = profesional; }

    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }

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

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Instant getCreatedAt() { return createdAt; }
}
