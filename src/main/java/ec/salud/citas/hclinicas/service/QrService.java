package ec.salud.citas.hclinicas.service;

/**
 * Contrato del servicio de códigos QR (HU-021 / CU-004).
 * Genera y valida los códigos QR que se imprimen en los documentos PDF.
 */
public interface QrService {

    /**
     * Genera una imagen PNG del código QR en bytes.
     *
     * @param contenido  Texto o URL que codificará el QR.
     * @param ancho      Ancho en píxeles.
     * @param alto       Alto en píxeles.
     * @return           Bytes de la imagen PNG.
     */
    byte[] generarQr(String contenido, int ancho, int alto);

    /**
     * Construye la URL pública de verificación del documento.
     * Ejemplo: https://hclinicas.salud.ec/verificar/{codigoUnico}
     *
     * @param codigoUnico Código UUID del documento.
     * @return            URL completa de verificación.
     */
    String construirUrlVerificacion(String codigoUnico);
}