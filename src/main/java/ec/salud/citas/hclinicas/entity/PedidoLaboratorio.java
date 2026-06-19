package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Pedido de exámenes de laboratorio e imagenología asociado a una consulta.
 * Tabla: pedidos_laboratorio
 */
@Entity
@Table(name = "pedidos_laboratorio")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    /**
     * Tipo de pedido: LABORATORIO o IMAGENOLOGIA
     */
    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo;

    /**
     * JSON con todos los exámenes seleccionados y sus valores.
     * Para laboratorio: { hematologia: [...], quimicaSanguinea: [...], ... }
     * Para imagenología: { tipoEstudio: "ECOGRAFIA", descripcion: "...", ... }
     */
    @Column(name = "examenes_json", columnDefinition = "TEXT", nullable = false)
    private String examenesJson;

    /** Resumen clínico del paciente */
    @Column(name = "resumen_clinico", columnDefinition = "TEXT")
    private String resumenClinico;

    /** Diagnóstico principal para el pedido */
    @Column(name = "diagnostico", length = 500)
    private String diagnostico;

    /** Código CIE-10 */
    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    /** Observaciones adicionales */
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "creado_por", length = 100)
    private String creadoPor;
}