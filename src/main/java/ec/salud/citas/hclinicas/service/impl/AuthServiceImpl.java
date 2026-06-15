package ec.salud.citas.hclinicas.service.impl;

import ec.salud.citas.hclinicas.dto.LoginRequest;
import ec.salud.citas.hclinicas.dto.LoginResponse;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.exception.UsuarioInactivoException;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.security.JwtService;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de autenticación.
 * Flujo: validar credenciales → verificar estado → generar JWT → auditar (CU-001).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    @Override
    public LoginResponse login(LoginRequest request, String ipOrigen) {

        // 1. Autenticar credenciales con Spring Security (BCrypt)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getCorreo(),
                            request.getContrasena()
                    )
            );
        } catch (AuthenticationException ex) {
            // Registrar intento fallido en auditoría
            auditoriaService.registrar(
                    "LOGIN_FALLIDO", "AUTENTICACION",
                    "Intento de login fallido para: " + request.getCorreo(),
                    ipOrigen, false, ex.getMessage()
            );
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // 2. Cargar el usuario desde la base de datos
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        // 3. Verificar estado del usuario (CU-001 flujo alternativo)
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            auditoriaService.registrar(
                    "LOGIN_BLOQUEADO", "AUTENTICACION",
                    "Intento de login de usuario inactivo: " + request.getCorreo(),
                    ipOrigen, false, "Usuario INACTIVO"
            );
            throw new UsuarioInactivoException(
                    "El usuario se encuentra inactivo. Contacte al administrador.");
        }
        if (usuario.getEstado() == EstadoUsuario.ELIMINADO) {
            auditoriaService.registrar(
                    "LOGIN_BLOQUEADO", "AUTENTICACION",
                    "Intento de login de usuario eliminado: " + request.getCorreo(),
                    ipOrigen, false, "Usuario ELIMINADO"
            );
            throw new UsuarioInactivoException(
                    "El usuario no existe en el sistema. Contacte al administrador.");
        }

        // 4. Generar token JWT
        String token = jwtService.generarToken(usuario);

        // 5. Registrar login exitoso en auditoría (HU-006)
        auditoriaService.registrar(
                "LOGIN", "AUTENTICACION",
                "Login exitoso: " + usuario.getNombreCompleto() +
                        " - Rol: " + usuario.getRol().getNombre().name(),
                ipOrigen
        );

        log.info("Login exitoso: {} - Rol: {}", usuario.getCorreo(),
                usuario.getRol().getNombre());

        // 6. Construir y retornar respuesta
        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .expiracionMs(jwtService.getJwtExpiration())
                .usuarioId(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().getNombre().name())
                .build();
    }
}
