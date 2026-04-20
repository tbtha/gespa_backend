package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PacienteRepository extends JpaRepository<Paciente, Long>, JpaSpecificationExecutor<Paciente> {
    boolean existsByRut(String rut);
    List<Paciente> findByProfesionalId(Long profesionalId);
}
