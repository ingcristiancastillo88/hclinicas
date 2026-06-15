package ec.salud.citas.hclinicas.service;


import ec.salud.citas.hclinicas.dto.*;
import ec.salud.citas.hclinicas.dto.response.PageResponse;

/**
 * Servicio de gestión de pacientes.
 * HU-007: Registro
 * HU-008: Edición
 * HU-009: Búsqueda paginada
 * CU-003: Casos de uso completo
 */
public interface PacienteService {

    PacienteResponse crear(CrearPacienteRequest request, String ipOrigen);

    PacienteResponse actualizar(Long id, ActualizarPacienteRequest request, String ipOrigen);

    PacienteResponse obtenerPorId(Long id);

    PacienteResponse obtenerPorCedula(String cedula);

    PageResponse<PacienteResumenResponse> listar(String busqueda, int pagina, int tamano, boolean soloActivos);

    void desactivar(Long id, String ipOrigen);

    void activar(Long id, String ipOrigen);
}
