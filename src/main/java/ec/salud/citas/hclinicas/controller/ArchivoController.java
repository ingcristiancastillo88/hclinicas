package ec.salud.citas.hclinicas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sirve los archivos almacenados para descarga y previsualización.
 * GET /api/archivos/consulta_{id}/{nombre} → retorna el archivo.
 */
@RestController
@RequestMapping("/archivos")
public class ArchivoController {

    @Value("${app.archivos.ruta-base:uploads/archivos}")
    private String rutaBase;

    @GetMapping("/consulta_{consultaId}/{nombreArchivo:.+}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<Resource> descargar(
            @PathVariable Long   consultaId,
            @PathVariable String nombreArchivo,
            @RequestParam(defaultValue = "false") boolean inline) {

        try {
            Path ruta = Paths.get(rutaBase)
                    .resolve("consulta_" + consultaId)
                    .resolve(nombreArchivo)
                    .normalize();

            Resource resource = new UrlResource(ruta.toUri());

            if (!resource.exists() || !resource.isReadable())
                return ResponseEntity.notFound().build();

            String disposition = inline
                    ? "inline; filename=\""     + nombreArchivo + "\""
                    : "attachment; filename=\"" + nombreArchivo + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}