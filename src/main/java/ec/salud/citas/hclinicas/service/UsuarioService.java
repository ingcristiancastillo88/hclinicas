package ec.salud.citas.hclinicas.service;

import ec.salud.citas.hclinicas.dto.ActualizarUsuarioRequest;
import ec.salud.citas.hclinicas.dto.CrearUsuarioRequest;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.dto.UsuarioResponse;

/**
 * Servicio de gestión de usuarios (HU-004, HU-005, HU-003).
 */
public interface UsuarioService {

    UsuarioResponse crear(CrearUsuarioRequest request, String ipOrigen);

    UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request, String ipOrigen);

    UsuarioResponse obtenerPorId(Long id);

    PageResponse<UsuarioResponse> listar(String busqueda, int pagina, int tamano);

    void desactivar(Long id, String ipOrigen);

    void activar(Long id, String ipOrigen);
}
