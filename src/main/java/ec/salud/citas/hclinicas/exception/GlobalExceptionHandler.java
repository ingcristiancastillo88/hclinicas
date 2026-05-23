package ec.salud.citas.hclinicas.exception;

import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones.
 * Garantiza respuestas HTTP consistentes y nunca expone stack traces (RNF-007).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 Bad Request ───────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            errores.put(campo, error.getDefaultMessage());
        });

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación en los datos enviados",
                        errores.toString()));
    }

    // ── 400 Bad Request: reglas de negocio ────────────────────────────────────
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiResponse<Void>> handleReglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), "REGLA_NEGOCIO"));
    }

    // ── 401 Unauthorized ──────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleCredenciales(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales incorrectas", "AUTH_FAILED"));
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiResponse<Void>> handleInactivo(UsuarioInactivoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "USUARIO_INACTIVO"));
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccesoDenegado(
            AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene permisos para acceder a este recurso",
                        "ACCESO_DENEGADO"));
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────────
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoEncontrado(
            RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "RECURSO_NO_ENCONTRADO"));
    }

    // ── 500 Internal Server Error ─────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Error interno del servidor: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Error interno del servidor. Por favor contacte al administrador.",
                        "ERROR_INTERNO"));
    }
}
