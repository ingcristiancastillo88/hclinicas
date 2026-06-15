package ec.salud.citas.hclinicas.service;

/**
 * Contrato del servicio de generación de documentos PDF.
 * HU-022 — Generar PDF de consulta médica con código QR.
 * RF-BE-007 / RF-BE-008
 */
public interface PdfService {

    /**
     * Genera el PDF completo de una consulta médica.
     * Incluye: datos del paciente, historia clínica, signos vitales,
     * diagnóstico, tratamiento y código QR de verificación.
     *
     * @param consultaId ID de la consulta a generar.
     * @return           Bytes del PDF generado.
     */
    byte[] generarPdfConsulta(Long consultaId);

    /**
     * Genera el PDF de un resumen de historia clínica.
     * Incluye antecedentes gineco-obstétricos y listado de consultas.
     *
     * @param historiaId ID de la historia clínica.
     * @return           Bytes del PDF generado.
     */
    byte[] generarPdfHistoria(Long historiaId);
}