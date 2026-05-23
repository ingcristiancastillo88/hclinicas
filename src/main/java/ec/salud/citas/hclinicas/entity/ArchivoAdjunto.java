package ec.salud.citas.hclinicas.entity;

import ec.salud.citas.hclinicas.enumerado.TipoArchivo;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad ArchivoAdjunto — archivos vinculados a una consulta médica.
 * Se almacena en el servidor de archivos y se referencia con su ruta.
 * <p>
 * HU-012 Adjuntar archivos multimedia
 * Tabla: archivos_adjuntos
 */
@Entity
@Table(name = "archivos_adjuntos")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;          // Nombre del archivo al subir

    @Column(name = "nombre_almacenado", nullable = false, length = 255)
    private String nombreAlmacenado;        // UUID + extensión en el servidor

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;             // Ruta relativa en el servidor

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;                // image/jpeg, application/pdf, etc.

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_archivo", length = 30)
    private TipoArchivo tipoArchivo;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    // ── Auditoría ─────────────────────────────────────────────────────────────
    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @CreatedBy
    @Column(name = "creado_por", updatable = false, length = 100)
    private String creadoPor;
}
