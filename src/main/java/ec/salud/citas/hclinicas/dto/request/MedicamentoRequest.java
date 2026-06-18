package ec.salud.citas.hclinicas.dto.request;

import lombok.Data;

/**
 * Representa un medicamento dentro de la receta médica.
 * Enviado desde el frontend al generar la receta.
 */
@Data
public class MedicamentoRequest {

    /** Nombre comercial o genérico del medicamento. */
    private String nombre;

    /** Dosis por toma. Ej: "500 mg", "1 tableta". */
    private String dosis;

    /** Cantidad total a dispensar. Ej: "10 tabletas", "1 frasco". */
    private String cantidad;

    /** Forma de administración. Ej: "1 tableta cada 8 horas por 7 días". */
    private String indicaciones;
}

