package ec.salud.citas.hclinicas.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * DTO para crear un pedido de laboratorio o imagenología.
 * El campo examenesSeleccionados contiene el mapa de todos los exámenes marcados.
 */
@Data
public class PedidoLaboratorioRequest {

    /** LABORATORIO o IMAGENOLOGIA */
    private String tipo;

    /**
     * Mapa de categoría → lista de exámenes seleccionados.
     * Ej: { "hematologia": ["Biometria Hematica", "Hematocrito"], ... }
     */
    private Map<String, List<String>> examenesSeleccionados;

    /** Para imagenología: tipo de estudio */
    private String tipoEstudio;

    /** Descripción del estudio (imagenología) */
    private String descripcion;

    /** Motivo de solicitud */
    private String motivoSolicitud;

    /** Resumen clínico */
    private String resumenClinico;

    /** Diagnóstico */
    private String diagnostico;

    /** CIE-10 */
    private String codigoCie10;

    /** Observaciones */
    private String observaciones;

    // Campos específicos imagenología
    private Boolean monitoreoFetal;
    private String fum;
    private String eg;

    // Campos específicos laboratorio
    private Boolean embarazo;
    private String semGestacion;
}