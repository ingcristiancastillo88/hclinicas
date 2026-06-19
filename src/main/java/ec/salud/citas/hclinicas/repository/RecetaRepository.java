package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    /** Obtiene todas las recetas de una consulta */
    List<Receta> findByConsultaIdOrderByFechaCreacionDesc(Long consultaId);

    /** Obtiene la última receta de una consulta */
    Optional<Receta> findTopByConsultaIdOrderByFechaCreacionDesc(Long consultaId);
}