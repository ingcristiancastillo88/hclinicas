package ec.salud.citas.hclinicas.service.impl;


import ec.salud.citas.hclinicas.dto.ActualizarUsuarioRequest;
import ec.salud.citas.hclinicas.dto.CrearUsuarioRequest;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.dto.UsuarioResponse;
import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.RolRepository;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implementación del servicio de usuarios.
 * Aplica BCrypt en contraseñas, valida unicidad de correo
 * y registra auditoría en cada operación (HU-004, HU-005, HU-006).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request, String ipOrigen) {

        // Validar correo único (CU-002 flujo alternativo: "Correo ya existente")
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ReglaNegocioException("Ya existe un usuario con el correo: "
                    + request.getCorreo());
        }

        // Validar cédula única si se proporciona
        if (StringUtils.hasText(request.getCedula())
                && usuarioRepository.existsByCedula(request.getCedula())) {
            throw new ReglaNegocioException("Ya existe un usuario con la cédula: "
                    + request.getCedula());
        }

        // Obtener rol
        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Rol no encontrado con ID: " + request.getRolId()));

        // Construir entidad
        Usuario usuario = Usuario.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .cedula(request.getCedula())
                .correo(request.getCorreo())
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .telefono(request.getTelefono())
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        usuario = usuarioRepository.save(usuario);

        // Auditoría (HU-006)
        auditoriaService.registrar(
                "CREATE", "USUARIOS",
                "Usuario creado: " + usuario.getNombreCompleto() +
                        " - Correo: " + usuario.getCorreo() +
                        " - Rol: " + rol.getNombre().name(),
                ipOrigen
        );

        log.info("Usuario creado: {} - Rol: {}", usuario.getCorreo(), rol.getNombre());
        return toResponse(usuario);
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request,
                                      String ipOrigen) {

        Usuario usuario = obtenerEntidad(id);

        // Validar correo único excluyendo al propio usuario
        if (usuarioRepository.existsByCorreoAndIdNot(request.getCorreo(), id)) {
            throw new ReglaNegocioException("Ya existe un usuario con el correo: "
                    + request.getCorreo());
        }

        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Rol no encontrado con ID: " + request.getRolId()));

        // Actualizar campos
        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setCedula(request.getCedula());
        usuario.setCorreo(request.getCorreo());
        usuario.setTelefono(request.getTelefono());
        usuario.setRol(rol);

        // Actualizar contraseña solo si viene en el request
        if (StringUtils.hasText(request.getContrasena())) {
            usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        }

        usuario = usuarioRepository.save(usuario);

        // Auditoría
        auditoriaService.registrar(
                "UPDATE", "USUARIOS",
                "Usuario actualizado: " + usuario.getNombreCompleto() +
                        " - Correo: " + usuario.getCorreo(),
                ipOrigen
        );

        return toResponse(usuario);
    }

    // ── Obtener ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    // ── Listar con búsqueda paginada (HU-005) ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String busqueda, int pagina, int tamano) {
        PageRequest pageRequest = PageRequest.of(pagina, tamano,
                Sort.by("apellidos").ascending());

        Page<UsuarioResponse> page = usuarioRepository
                .buscarUsuarios(busqueda, EstadoUsuario.ELIMINADO, pageRequest)
                .map(this::toResponse);

        return PageResponse.of(page);
    }

    // ── Desactivar (eliminación lógica) ───────────────────────────────────────

    @Override
    @Transactional
    public void desactivar(Long id, String ipOrigen) {
        Usuario usuario = obtenerEntidad(id);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                "DEACTIVATE", "USUARIOS",
                "Usuario desactivado: " + usuario.getNombreCompleto(), ipOrigen
        );
    }

    // ── Activar ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void activar(Long id, String ipOrigen) {
        Usuario usuario = obtenerEntidad(id);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                "ACTIVATE", "USUARIOS",
                "Usuario activado: " + usuario.getNombreCompleto(), ipOrigen
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Usuario obtenerEntidad(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getEstado() != EstadoUsuario.ELIMINADO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .nombreCompleto(u.getNombreCompleto())
                .cedula(u.getCedula())
                .correo(u.getCorreo())
                .telefono(u.getTelefono())
                .rol(u.getRol().getNombre().name())
                .estado(u.getEstado())
                .fechaCreacion(u.getFechaCreacion())
                .fechaActualizacion(u.getFechaActualizacion())
                .creadoPor(u.getCreadoPor())
                .build();
    }
}
