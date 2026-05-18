package com.tbtha.gespa_backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "profesionales")
public class Profesional {

    @Id
    private Long id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "id")
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(nullable = false, length = 150)
    private String specialty;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 255)
    private String institucion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getInstitucion() {
        return institucion;
    }

    public void setInstitucion(String institucion) {
        this.institucion = institucion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
