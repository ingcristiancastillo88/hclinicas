package ec.salud.citas.hclinicas.dto.response;

import ec.salud.citas.hclinicas.enumerado.TipoArchivo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ArchivoAdjuntoResponse {
    private Long id;
    private String nombreOriginal;
    private String tipoMime;
    private Long tamanoBytes;
    private TipoArchivo tipoArchivo;
    private String descripcion;
    private String urlDescarga;
    private LocalDateTime fechaCreacion;
    private String creadoPor;
}