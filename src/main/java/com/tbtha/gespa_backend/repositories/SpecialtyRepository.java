package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    Optional<Specialty> findByNameIgnoreCase(String name);
    List<Specialty> findAllByActiveTrueOrderByNameAsc();
}
