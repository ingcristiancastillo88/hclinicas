package ec.salud.citas.hclinicas.service;

/**
 * Servicio de auditoría - registra todas las acciones del sistema (HU-006).
 */
public interface AuditoriaService {

    void registrar(String accion, String modulo, String descripcion,
                   String ipOrigen, boolean exitoso, String detalleError);

    void registrar(String accion, String modulo, String descripcion, String ipOrigen);
}
