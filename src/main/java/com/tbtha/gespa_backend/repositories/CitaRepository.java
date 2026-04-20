package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByProfesionalIdOrderByStartsAtAsc(Long profesionalId);
    List<Cita> findByPacienteIdOrderByStartsAtAsc(Long pacienteId);

    List<Cita> findByProfesionalIdAndStartsAtBetweenOrderByStartsAtAsc(
        Long profesionalId,
        OffsetDateTime desde,
        OffsetDateTime hasta
    );

    boolean existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThan(
        Long profesionalId,
        AppointmentStatus status,
        OffsetDateTime endsAt,
        OffsetDateTime startsAt
    );

    boolean existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThanAndIdNot(
        Long profesionalId,
        AppointmentStatus status,
        OffsetDateTime endsAt,
        OffsetDateTime startsAt,
        Long id
    );
}
