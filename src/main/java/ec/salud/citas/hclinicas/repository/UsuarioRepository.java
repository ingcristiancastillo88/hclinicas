package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCedula(String cedula);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    // HU-005: Búsqueda por nombre o cédula con paginación
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.estado != :excluirEstado
            AND (:busqueda IS NULL OR :busqueda = ''
                 OR LOWER(u.nombres) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                 OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                 OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                 OR u.cedula LIKE CONCAT('%', :busqueda, '%'))
            """)
    Page<Usuario> buscarUsuarios(
            @Param("busqueda") String busqueda,
            @Param("excluirEstado") EstadoUsuario excluirEstado,
            Pageable pageable
    );
}
