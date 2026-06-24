package ec.salud.citas.hclinicas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correos electrónicos.
 * Se ejecuta de forma asíncrona para no bloquear el hilo principal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@klinixmed.org}")
    private String mailFrom;

    @Value("${app.mail.nombre-clinica:Consultorio Dra. Alexandra León}")
    private String nombreClinica;

    // ── Bienvenida con contraseña temporal ───────────────────────────────────

    /**
     * Envía correo de bienvenida con contraseña temporal al usuario recién creado.
     * Se llama automáticamente al crear cualquier usuario (médico, admin, paciente).
     *
     * @param destinatario correo del usuario (también es su username)
     * @param nombreCompleto nombre para personalizar el saludo
     * @param passwordTemporal contraseña generada aleatoriamente
     */
    @Async
    public void enviarBienvenida(String destinatario, String nombreCompleto,
                                 String passwordTemporal) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(mailFrom, nombreClinica);
            helper.setTo(destinatario);
            helper.setSubject("Bienvenido/a al sistema — " + nombreClinica);
            helper.setText(buildHtmlBienvenida(nombreCompleto, destinatario,
                    passwordTemporal), true);

            mailSender.send(msg);
            log.info("Correo de bienvenida enviado a: {}", destinatario);

        } catch (Exception e) {
            log.error("Error enviando correo de bienvenida a {}: {}", destinatario,
                    e.getMessage());
        }
    }

    // ── Notificación de cambio de contraseña ─────────────────────────────────

    @Async
    public void enviarConfirmacionCambioPassword(String destinatario,
                                                 String nombreCompleto) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(mailFrom, nombreClinica);
            helper.setTo(destinatario);
            helper.setSubject("Contraseña actualizada — " + nombreClinica);
            helper.setText(buildHtmlCambioPassword(nombreCompleto, destinatario),
                    true);

            mailSender.send(msg);
            log.info("Correo de cambio de password enviado a: {}", destinatario);

        } catch (Exception e) {
            log.error("Error enviando correo de cambio de password a {}: {}",
                    destinatario, e.getMessage());
        }
    }

    // ── Templates HTML ────────────────────────────────────────────────────────

    private String buildHtmlBienvenida(String nombre, String email,
                                       String password) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8">
            <style>
              body { font-family: Arial, sans-serif; background:#f8fafc; margin:0; padding:0; }
              .container { max-width:560px; margin:32px auto; background:white;
                           border-radius:16px; overflow:hidden;
                           box-shadow:0 4px 24px rgba(0,0,0,.10); }
              .header { background:linear-gradient(135deg,#0a2342,#1a4a7a);
                        padding:32px 24px; text-align:center; }
              .header h1 { color:white; margin:0; font-size:1.4rem; }
              .header p  { color:#93c5fd; margin:6px 0 0; font-size:.9rem; }
              .body { padding:32px 24px; }
              .body p { color:#475569; line-height:1.7; margin:0 0 16px; }
              .cred-box { background:#f1f5f9; border-radius:12px; padding:20px 24px;
                          margin:20px 0; border-left:4px solid #e91e8c; }
              .cred-box .label { font-size:.78rem; color:#94a3b8;
                                 font-weight:700; text-transform:uppercase;
                                 margin-bottom:4px; }
              .cred-box .value { font-size:1rem; color:#0a2342; font-weight:700;
                                 font-family:monospace; }
              .aviso { background:#fef3c7; border-radius:10px; padding:14px 18px;
                       margin:20px 0; font-size:.85rem; color:#92400e; }
              .aviso strong { color:#78350f; }
              .footer { background:#f8fafc; padding:16px 24px; text-align:center;
                        color:#94a3b8; font-size:.78rem; }
            </style>
            </head>
            <body>
            <div class="container">
              <div class="header">
                <h1>%s</h1>
                <p>Sistema de Gestión Clínica</p>
              </div>
              <div class="body">
                <p>Estimado/a <strong>%s</strong>,</p>
                <p>Tu cuenta ha sido creada exitosamente en nuestro sistema.
                   A continuación tus credenciales de acceso:</p>
                <div class="cred-box">
                  <div class="label">Usuario (correo)</div>
                  <div class="value">%s</div>
                </div>
                <div class="cred-box">
                  <div class="label">Contraseña Temporal</div>
                  <div class="value">%s</div>
                </div>
                <div class="aviso">
                  ⚠️ <strong>Importante:</strong> Al ingresar por primera vez
                  el sistema te solicitará cambiar tu contraseña temporal
                  por una de tu elección.
                </div>
                <p>Si tienes alguna duda, comunícate con el administrador del sistema.</p>
              </div>
              <div class="footer">
                %s · klinixmed.org<br>
                Este es un correo automático, por favor no responder.
              </div>
            </div>
            </body></html>
            """.formatted(nombreClinica, nombre, email, password, nombreClinica);
    }

    private String buildHtmlCambioPassword(String nombre, String email) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8">
            <style>
              body { font-family: Arial, sans-serif; background:#f8fafc; margin:0; }
              .container { max-width:560px; margin:32px auto; background:white;
                           border-radius:16px; overflow:hidden;
                           box-shadow:0 4px 24px rgba(0,0,0,.10); }
              .header { background:linear-gradient(135deg,#059669,#047857);
                        padding:32px 24px; text-align:center; }
              .header h1 { color:white; margin:0; font-size:1.4rem; }
              .body { padding:32px 24px; color:#475569; line-height:1.7; }
              .footer { background:#f8fafc; padding:16px 24px; text-align:center;
                        color:#94a3b8; font-size:.78rem; }
            </style>
            </head>
            <body>
            <div class="container">
              <div class="header">
                <h1>✓ Contraseña actualizada</h1>
              </div>
              <div class="body">
                <p>Hola <strong>%s</strong>,</p>
                <p>Tu contraseña para el usuario <strong>%s</strong>
                   ha sido actualizada correctamente.</p>
                <p>Ya puedes ingresar al sistema con tu nueva contraseña.
                   Si no realizaste este cambio, comunícate de inmediato
                   con el administrador.</p>
              </div>
              <div class="footer">%s · klinixmed.org</div>
            </div>
            </body></html>
            """.formatted(nombre, email, nombreClinica);
    }
}