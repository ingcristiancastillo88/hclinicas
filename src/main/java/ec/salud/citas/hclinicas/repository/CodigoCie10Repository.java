package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.CodigoCie10;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoCie10Repository extends JpaRepository<CodigoCie10, Long> {

    /**
     * Busca códigos que empiecen con el texto (para buscar por código)
     * O cuya descripción contenga el texto (para buscar por nombre).
     * Limitado a 10 resultados — códigos primero, luego por descripción.
     */
    @Query("""
        SELECT c FROM CodigoCie10 c
        WHERE UPPER(c.codigo) LIKE UPPER(CONCAT(:texto, '%'))
           OR UPPER(c.descripcion) LIKE UPPER(CONCAT('%', :texto, '%'))
        ORDER BY
            CASE WHEN UPPER(c.codigo) LIKE UPPER(CONCAT(:texto, '%')) THEN 0 ELSE 1 END,
            c.codigo
        """)
    List<CodigoCie10> buscarSugerencias(@Param("texto") String texto);
}