package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByPacienteIdOrderByCreatedAtDesc(Long pacienteId);
    List<Nota> findByPacienteIdAndProfesionalIdOrderByCreatedAtDesc(Long pacienteId, Long profesionalId);
    java.util.Optional<Nota> findByIdAndProfesionalId(Long id, Long profesionalId);
}
