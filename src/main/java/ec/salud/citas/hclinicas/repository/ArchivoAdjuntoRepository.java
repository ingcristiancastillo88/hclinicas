package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {

    List<ArchivoAdjunto> findByConsultaIdOrderByFechaCreacionDesc(Long consultaId);
}