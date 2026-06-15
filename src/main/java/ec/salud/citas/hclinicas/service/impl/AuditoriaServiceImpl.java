package ec.salud.citas.hclinicas.service.impl;

import ec.salud.citas.hclinicas.entity.Auditoria;
import ec.salud.citas.hclinicas.repository.AuditoriaRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementación del servicio de auditoría.
 * Usa REQUIRES_NEW para que la auditoría siempre se persista,
 * incluso si la transacción principal hace rollback (RNF-011).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String modulo, String descripcion,
                          String ipOrigen, boolean exitoso, String detalleError) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String correo = "ANONIMO";
        String nombre = "ANONIMO";

        if (auth != null && auth.isAuthenticated()
                && !auth.getPrincipal().equals("anonymousUser")) {
            correo = auth.getName();
            if (auth.getPrincipal() instanceof UserDetails ud) {
                nombre = ud.getUsername();
            }
        }

        Auditoria auditoria = Auditoria.builder()
                .usuarioCorreo(correo)
                .nombreUsuario(nombre)
                .accion(accion)
                .modulo(modulo)
                .descripcion(descripcion)
                .ipOrigen(ipOrigen)
                .fechaAccion(LocalDateTime.now())
                .exitoso(exitoso)
                .detalleError(detalleError)
                .build();

        auditoriaRepository.save(auditoria);
        log.debug("Auditoría registrada: [{}] {} - {}", accion, modulo, descripcion);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String modulo, String descripcion, String ipOrigen) {
        registrar(accion, modulo, descripcion, ipOrigen, true, null);
    }
}
