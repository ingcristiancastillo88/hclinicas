package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.request.CambiarPasswordRequest;
import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.service.EmailService;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint para cambio de contraseña temporal.
 * POST /api/auth/cambiar-password
 * El usuario debe estar autenticado (tiene token JWT con passwordTemporal=true).
 * Tras el cambio exitoso, el flag passwordTemporal se pone en false.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class CambiarPasswordController {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;
    private final EmailService emailService;

    @PostMapping("/cambiar-password")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest req) {

        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Usuario usuario = usuarioRepo.findByCorreo(email)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));

        // Validar contraseña actual
        if (!encoder.matches(req.getPasswordActual(), usuario.getPassword())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }

        // Validar que nueva y confirmación coincidan
        if (!req.getPasswordNueva().equals(req.getConfirmarPassword())) {
            throw new ReglaNegocioException(
                    "La nueva contraseña y la confirmación no coinciden");
        }

        // Validar que no sea igual a la temporal
        if (encoder.matches(req.getPasswordNueva(), usuario.getPassword())) {
            throw new ReglaNegocioException(
                    "La nueva contraseña no puede ser igual a la contraseña temporal");
        }

        // Actualizar contraseña y quitar flag temporal
        usuario.setContrasena(encoder.encode(req.getPasswordNueva()));
        usuario.setPasswordTemporal(false);
        usuarioRepo.save(usuario);

        // Notificar por correo (asíncrono)
        emailService.enviarConfirmacionCambioPassword(
                email, usuario.getNombreCompleto());

        log.info("Contraseña actualizada para usuario: {}", email);
        return ResponseEntity.ok(
                ApiResponse.ok("Contraseña actualizada correctamente", null));
    }
}