package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.enumerado.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(RolNombre nombre);
    boolean existsByNombre(RolNombre nombre);
}
