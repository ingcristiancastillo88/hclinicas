package ec.salud.citas.hclinicas.repository;

import ec.salud.citas.hclinicas.entity.PedidoLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoLaboratorioRepository extends JpaRepository<PedidoLaboratorio, Long> {

    /** Todos los pedidos de una consulta ordenados por fecha */
    List<PedidoLaboratorio> findByConsultaIdOrderByFechaCreacionDesc(Long consultaId);

    /** Pedidos por tipo (LABORATORIO o IMAGENOLOGIA) */
    List<PedidoLaboratorio> findByConsultaIdAndTipoOrderByFechaCreacionDesc(
            Long consultaId, String tipo);
}