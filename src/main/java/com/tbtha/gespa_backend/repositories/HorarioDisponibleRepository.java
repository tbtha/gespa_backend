package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.HorarioDisponible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioDisponibleRepository extends JpaRepository<HorarioDisponible, Long> {
    List<HorarioDisponible> findByProfesionalIdAndActiveTrue(Long profesionalId);
    List<HorarioDisponible> findByProfesionalIdAndDiaSemanaAndActiveTrue(Long profesionalId, int diaSemana);
    List<HorarioDisponible> findByProfesionalId(Long profesionalId);
}
