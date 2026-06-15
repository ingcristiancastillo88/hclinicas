package ec.salud.citas.hclinicas.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.service.QrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación del servicio QR usando la librería ZXing de Google.
 * HU-021 — Generación de códigos QR en documentos PDF.
 */
@Slf4j
@Service
public class QrServiceImpl implements QrService {

    @Value("${app.qr.url-base:http://localhost:4200/verificar}")
    private String urlBase;

    @Override
    public byte[] generarQr(String contenido, int ancho, int alto) {
        try {
            // Configuración del QR: corrección de errores alta, charset UTF-8
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(contenido, BarcodeFormat.QR_CODE,
                    ancho, alto, hints);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);

            log.debug("QR generado para contenido: {}", contenido);
            return baos.toByteArray();

        } catch (WriterException | IOException e) {
            log.error("Error generando código QR: {}", e.getMessage());
            throw new ReglaNegocioException(
                    "No se pudo generar el código QR: " + e.getMessage());
        }
    }

    @Override
    public String construirUrlVerificacion(String codigoUnico) {
        return urlBase + "/" + codigoUnico;
    }
}