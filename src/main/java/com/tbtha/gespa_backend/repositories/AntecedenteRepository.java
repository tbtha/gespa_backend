package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.Antecedente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AntecedenteRepository extends JpaRepository<Antecedente, Long> {
    Optional<Antecedente> findByPacienteId(Long pacienteId);
}
