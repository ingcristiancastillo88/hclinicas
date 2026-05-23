package ec.salud.citas.hclinicas.controller;


import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.dto.LoginRequest;
import ec.salud.citas.hclinicas.dto.LoginResponse;
import ec.salud.citas.hclinicas.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación.
 * Endpoint público - no requiere JWT (CU-001 / HU-001).
 *
 * POST /api/auth/login   → iniciar sesión
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Autentica al usuario y retorna el token JWT.
     * Responde con HTTP 401 en menos de 200ms ante credenciales inválidas (RNF-006).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipOrigen = obtenerIpCliente(httpRequest);
        LoginResponse response = authService.login(request, ipOrigen);

        return ResponseEntity.ok(
                ApiResponse.ok("Inicio de sesión exitoso", response)
        );
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String obtenerIpCliente(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
