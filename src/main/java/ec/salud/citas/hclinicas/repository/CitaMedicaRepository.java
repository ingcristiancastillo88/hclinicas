package ec.salud.citas.hclinicas.repository;


import ec.salud.citas.hclinicas.entity.CitaMedica;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CitaMedicaRepository extends JpaRepository<CitaMedica, Long> {

    // ── Validación de disponibilidad (HU-017) ─────────────────────────────────

    /**
     * Verifica si ya existe una cita activa en el mismo rango de tiempo.
     * Detecta solapamientos: nueva cita comienza antes de que termine la existente
     * y termina después de que comienza la existente.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM CitaMedica c
            WHERE c.fechaCita = :fecha
              AND c.estado NOT IN (:estadosExcluidos)
              AND (
                  (c.horaInicio < :horaFin AND c.horaFin > :horaInicio)
                  OR c.horaInicio = :horaInicio
              )
            """)
    boolean existeConflictoHorario(@Param("fecha") LocalDate fecha, @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin, @Param("estadosExcluidos") List<EstadoCita> estadosExcluidos);

    /**
     * Mismo chequeo excluyendo la propia cita (para edición).
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM CitaMedica c
            WHERE c.fechaCita = :fecha
              AND c.id != :excludeId
              AND c.estado NOT IN (:estadosExcluidos)
              AND (
                  (c.horaInicio < :horaFin AND c.horaFin > :horaInicio)
                  OR c.horaInicio = :horaInicio
              )
            """)
    boolean existeConflictoHorarioExcluyendo(@Param("fecha") LocalDate fecha, @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin, @Param("estadosExcluidos") List<EstadoCita> estadosExcluidos, @Param("excludeId") Long excludeId);

    // ── Consultas por rango de fechas (HU-019 — calendario) ──────────────────

    /**
     * Todas las citas de un mes para el calendario.
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE c.fechaCita BETWEEN :inicio AND :fin
            ORDER BY c.fechaCita ASC, c.horaInicio ASC
            """)
    List<CitaMedica> findByRangoFechas(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /**
     * Citas de un día específico.
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE c.fechaCita = :fecha
            ORDER BY c.horaInicio ASC
            """)
    List<CitaMedica> findByFecha(@Param("fecha") LocalDate fecha);

    // ── Consultas por paciente ────────────────────────────────────────────────

    Page<CitaMedica> findByPacienteIdOrderByFechaCitaDescHoraInicioDesc(Long pacienteId, Pageable pageable);

    // ── Listado general paginado con filtros ──────────────────────────────────

    @Query("""
            SELECT c FROM CitaMedica c
            WHERE (:estado IS NULL OR c.estado = :estado)
            AND (:fecha IS NULL OR c.fechaCita = :fecha)
            AND (
                CAST(:busqueda AS string) IS NULL
                OR CAST(:busqueda AS string) = ''
                OR LOWER(c.paciente.nombres)
                LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%'))
                OR LOWER(c.paciente.apellidos)
                LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%'))
                OR c.paciente.cedula
                LIKE CONCAT('%', CAST(:busqueda AS string), '%'))
            ORDER BY c.fechaCita DESC, c.horaInicio DESC
            """)
    Page<CitaMedica> listarConFiltros(@Param("estado") EstadoCita estado, @Param("fecha") LocalDate fecha, @Param("busqueda") String busqueda, Pageable pageable);

    // ── Estadísticas rápidas para dashboard ──────────────────────────────────

    long countByFechaCitaAndEstadoNot(LocalDate fecha, EstadoCita estado);

    long countByEstado(EstadoCita estado);
}