package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {
    boolean existsByRut(String rut);
    boolean existsByRutAndIdNot(String rut, Long id);
}
