package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicamentoCatalogoRepository extends JpaRepository<Medicamento, Long> {

    Optional<Medicamento> findByNombreNormalizado(String nombreNormalizado);

    /**
     * Busca por nombre genérico normalizado — los más usados primero.
     */
    @Query("""
        SELECT m FROM Medicamento m
        WHERE m.nombreNormalizado LIKE CONCAT('%', :texto, '%')
        ORDER BY m.vecesUsado DESC, m.ultimaVezUsado DESC
        """)
    List<Medicamento> buscarSugerencias(@Param("texto") String texto);
}