package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    Optional<HistoriaClinica> findByPacienteId(Long pacienteId);

    boolean existsByPacienteId(Long pacienteId);

    @Query("""
            SELECT h FROM HistoriaClinica h
            LEFT JOIN FETCH h.consultas c
            WHERE h.paciente.id = :pacienteId
            AND h.activa = true
            """)
    Optional<HistoriaClinica> findActivaByPacienteId(@Param("pacienteId") Long pacienteId);
}