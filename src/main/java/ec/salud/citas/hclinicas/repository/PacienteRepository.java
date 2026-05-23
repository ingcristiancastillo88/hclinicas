package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de Paciente.
 * HU-009: búsqueda por nombre, apellido o cédula con paginación.
 * RNF-005: consultas optimizadas.
 */
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByCedula(String cedula);

    boolean existsByCedulaAndIdNot(String cedula, Long id);

    boolean existsByHistoriaNúmero(String historiaNúmero);

    Optional<Paciente> findByCedula(String cedula);

    /**
     * Búsqueda optimizada por nombre, apellido o cédula con filtro de estado.
     * RNF-005: el costo de ejecución no debe aumentar más de un 10%
     * respecto al crecimiento lineal del volumen de datos.
     */
    @Query("""
            SELECT p FROM Paciente p
            WHERE p.estado = :estado
            AND (
                :busqueda IS NULL OR :busqueda = ''
                OR LOWER(p.nombres)   LIKE LOWER(CONCAT('%', :busqueda, '%'))
                OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                OR p.cedula           LIKE CONCAT('%', :busqueda, '%')
                OR LOWER(p.correo)    LIKE LOWER(CONCAT('%', :busqueda, '%'))
                OR p.historiaNúmero   LIKE CONCAT('%', :busqueda, '%')
            )
            ORDER BY p.apellidos ASC, p.nombres ASC
            """)
    Page<Paciente> buscarPacientes(
            @Param("busqueda") String busqueda,
            @Param("estado") EstadoPaciente estado,
            Pageable pageable
    );

    /**
     * Busca todos los pacientes sin importar estado (solo superadmin).
     */
    @Query("""
            SELECT p FROM Paciente p
            WHERE (
                :busqueda IS NULL OR :busqueda = ''
                OR LOWER(p.nombres)   LIKE LOWER(CONCAT('%', :busqueda, '%'))
                OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                OR p.cedula           LIKE CONCAT('%', :busqueda, '%')
            )
            ORDER BY p.apellidos ASC
            """)
    Page<Paciente> buscarTodos(
            @Param("busqueda") String busqueda,
            Pageable pageable
    );
}
