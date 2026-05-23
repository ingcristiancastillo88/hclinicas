package ec.salud.citas.hclinicas.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO de respuesta paginada para listados (HU-005, HU-009).
 */
@Data
@Builder
public class PageResponse<T> {

    private List<T> contenido;
    private int paginaActual;
    private int totalPaginas;
    private long totalElementos;
    private int tamanioPagina;
    private boolean primera;
    private boolean ultima;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .contenido(page.getContent())
                .paginaActual(page.getNumber())
                .totalPaginas(page.getTotalPages())
                .totalElementos(page.getTotalElements())
                .tamanioPagina(page.getSize())
                .primera(page.isFirst())
                .ultima(page.isLast())
                .build();
    }
}
