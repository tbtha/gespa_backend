package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.RegistroEvolucion;
import com.tbtha.gespa_backend.entities.enums.TipoIndicador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RegistroEvolucionRepository extends JpaRepository<RegistroEvolucion, Long> {

    /** Todos los registros de un paciente ordenados por fecha descendente. */
    List<RegistroEvolucion> findByPacienteIdOrderByFechaRegistroDesc(Long pacienteId);

    /**
     * Registros de un paciente filtrando por tipo de indicador y rango de fechas.
     * Usado para alimentar gráficos de evolución.
     */
    @Query("""
            SELECT r FROM RegistroEvolucion r
            WHERE r.paciente.id = :pacienteId
              AND r.tipoIndicador = :tipo
              AND r.fechaRegistro BETWEEN :desde AND :hasta
            ORDER BY r.fechaRegistro ASC
            """)
    List<RegistroEvolucion> findByPacienteAndIndicadorEnRango(
            @Param("pacienteId") Long pacienteId,
            @Param("tipo") TipoIndicador tipo,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /** Registros de una cita específica. */
    List<RegistroEvolucion> findByCitaIdOrderByTipoIndicadorAsc(Long citaId);

    /**
     * Último registro de cada indicador para un paciente.
     * Útil para mostrar el resumen de valores actuales en la ficha.
     */
    @Query("""
            SELECT r FROM RegistroEvolucion r
            WHERE r.paciente.id = :pacienteId
              AND r.fechaRegistro = (
                  SELECT MAX(r2.fechaRegistro) FROM RegistroEvolucion r2
                  WHERE r2.paciente.id = :pacienteId
                    AND r2.tipoIndicador = r.tipoIndicador
              )
            ORDER BY r.tipoIndicador ASC
            """)
    List<RegistroEvolucion> findUltimoPorIndicador(@Param("pacienteId") Long pacienteId);
}
