package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.Consulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    /**
     * Historial completo de consultas de una historia clínica,
     * ordenado de más reciente a más antigua (HU-011).
     */
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.historiaClinica.id = :historiaId
            AND c.activa = true
            ORDER BY c.fechaConsulta DESC, c.fechaCreacion DESC
            """)
    Page<Consulta> findByHistoriaClinicaId(
            @Param("historiaId") Long historiaId,
            Pageable pageable
    );

    /**
     * Consulta con sus archivos adjuntos (evita N+1).
     */
    @Query("""
            SELECT c FROM Consulta c
            LEFT JOIN FETCH c.archivos
            WHERE c.id = :id AND c.activa = true
            """)
    Optional<Consulta> findByIdWithArchivos(@Param("id") Long id);

    long countByHistoriaClinicaIdAndActivaTrue(Long historiaId);
}