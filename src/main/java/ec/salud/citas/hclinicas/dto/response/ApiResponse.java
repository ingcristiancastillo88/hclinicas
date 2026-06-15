package ec.salud.citas.hclinicas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Wrapper estándar para todas las respuestas del API.
 * Garantiza un formato consistente en todos los endpoints (RNF-001).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean exitoso;
    private String mensaje;
    private T data;
    private String error;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Factories ─────────────────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(String mensaje, T data) {
        return ApiResponse.<T>builder()
                .exitoso(true)
                .mensaje(mensaje)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String mensaje) {
        return ApiResponse.<T>builder()
                .exitoso(true)
                .mensaje(mensaje)
                .build();
    }

    public static <T> ApiResponse<T> error(String mensaje, String error) {
        return ApiResponse.<T>builder()
                .exitoso(false)
                .mensaje(mensaje)
                .error(error)
                .build();
    }
}
