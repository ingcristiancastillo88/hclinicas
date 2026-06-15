package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    Page<Auditoria> findByUsuarioCorreoOrderByFechaAccionDesc(String correo, Pageable pageable);

    Page<Auditoria> findByModuloOrderByFechaAccionDesc(String modulo, Pageable pageable);

    List<Auditoria> findByFechaAccionBetweenOrderByFechaAccionDesc(
            LocalDateTime inicio, LocalDateTime fin);
}
