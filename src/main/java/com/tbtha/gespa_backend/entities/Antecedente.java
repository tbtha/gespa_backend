package com.tbtha.gespa_backend.entities;

import com.tbtha.gespa_backend.entities.enums.ActividadFisica;
import com.tbtha.gespa_backend.entities.enums.ConsumoNivel;
import com.tbtha.gespa_backend.entities.enums.ConsumoTabaco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "antecedentes")
public class Antecedente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    private Paciente paciente;

    @Column(name = "enfermedades_base", columnDefinition = "TEXT")
    private String enfermedadesBase;

    @Column(name = "enfermedades_otros", length = 500)
    private String enfermedadesOtros;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumo_alcohol", length = 20)
    private ConsumoNivel consumoAlcohol;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumo_tabaco", length = 20)
    private ConsumoTabaco consumoTabaco;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumo_drogas", length = 20)
    private ConsumoNivel consumoDrogas;

    @Enumerated(EnumType.STRING)
    @Column(name = "actividad_fisica", length = 50)
    private ActividadFisica actividadFisica;

    @Column(name = "operaciones_previas", columnDefinition = "TEXT")
    private String operacionesPrevias;

    @Column(name = "medicamentos_regulares", columnDefinition = "TEXT")
    private String medicamentosRegulares;

    @Column(name = "otros_antecedentes", columnDefinition = "TEXT")
    private String otrosAntecedentes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public String getEnfermedadesBase() {
        return enfermedadesBase;
    }

    public void setEnfermedadesBase(String enfermedadesBase) {
        this.enfermedadesBase = enfermedadesBase;
    }

    public String getEnfermedadesOtros() {
        return enfermedadesOtros;
    }

    public void setEnfermedadesOtros(String enfermedadesOtros) {
        this.enfermedadesOtros = enfermedadesOtros;
    }

    public ConsumoNivel getConsumoAlcohol() {
        return consumoAlcohol;
    }

    public void setConsumoAlcohol(ConsumoNivel consumoAlcohol) {
        this.consumoAlcohol = consumoAlcohol;
    }

    public ConsumoTabaco getConsumoTabaco() {
        return consumoTabaco;
    }

    public void setConsumoTabaco(ConsumoTabaco consumoTabaco) {
        this.consumoTabaco = consumoTabaco;
    }

    public ConsumoNivel getConsumoDrogas() {
        return consumoDrogas;
    }

    public void setConsumoDrogas(ConsumoNivel consumoDrogas) {
        this.consumoDrogas = consumoDrogas;
    }

    public ActividadFisica getActividadFisica() {
        return actividadFisica;
    }

    public void setActividadFisica(ActividadFisica actividadFisica) {
        this.actividadFisica = actividadFisica;
    }

    public String getOperacionesPrevias() {
        return operacionesPrevias;
    }

    public void setOperacionesPrevias(String operacionesPrevias) {
        this.operacionesPrevias = operacionesPrevias;
    }

    public String getMedicamentosRegulares() {
        return medicamentosRegulares;
    }

    public void setMedicamentosRegulares(String medicamentosRegulares) {
        this.medicamentosRegulares = medicamentosRegulares;
    }

    public String getOtrosAntecedentes() {
        return otrosAntecedentes;
    }

    public void setOtrosAntecedentes(String otrosAntecedentes) {
        this.otrosAntecedentes = otrosAntecedentes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
