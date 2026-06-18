package ec.salud.citas.hclinicas.service.impl;

import ec.salud.citas.hclinicas.dto.request.ActualizarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CancelarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearCitaRequest;
import ec.salud.citas.hclinicas.dto.response.*;
import ec.salud.citas.hclinicas.entity.CitaMedica;
import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.CitaMedicaRepository;
import ec.salud.citas.hclinicas.repository.PacienteRepository;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.CitaMedicaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de citas médicas.
 * <p>
 * Lógica de validación de disponibilidad (HU-017):
 * - Se consideran CANCELADAS y NO_ASISTIO como slots liberados.
 * - Se detectan solapamientos de tiempo (no solo colisión exacta de hora).
 * - Al editar, se excluye la propia cita de la validación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitaMedicaServiceImpl implements CitaMedicaService {

    private final CitaMedicaRepository citaRepo;
    private final PacienteRepository pacienteRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuditoriaService auditoriaService;

    private static final String MODULO = "CITAS";

    // Estados que se consideran "liberados" para la validación de disponibilidad
    private static final List<EstadoCita> ESTADOS_LIBRES =
            List.of(EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO);

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CitaMedicaResponse crear(CrearCitaRequest req, String ip) {

        Paciente paciente = pacienteRepo.findById(req.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Paciente no encontrado: " + req.getPacienteId()));

        Usuario usuario = getUsuarioActual();

        LocalTime horaFin = req.getHoraInicio()
                .plusMinutes(req.getDuracionMinutos() != null
                        ? req.getDuracionMinutos() : 30);

        // Validar disponibilidad (HU-017)
        validarDisponibilidad(req.getFechaCita(), req.getHoraInicio(), horaFin, null);

        CitaMedica cita = CitaMedica.builder()
                .paciente(paciente)
                .usuario(usuario)
                .fechaCita(req.getFechaCita())
                .horaInicio(req.getHoraInicio())
                .horaFin(horaFin)
                .duracionMinutos(req.getDuracionMinutos() != null
                        ? req.getDuracionMinutos() : 30)
                .tipoCita(req.getTipoCita())
                .motivoCita(req.getMotivoCita())
                .notasAdicionales(req.getNotasAdicionales())
                .estado(EstadoCita.PROGRAMADA)
                .build();

        cita = citaRepo.save(cita);

        auditoriaService.registrar("CREATE", MODULO,
                "Cita agendada: " + paciente.getNombreCompleto()
                        + " | " + req.getFechaCita() + " " + req.getHoraInicio(), ip);

        log.info("Cita creada ID:{} | Paciente:{} | {}/{}",
                cita.getId(), paciente.getCedula(),
                req.getFechaCita(), req.getHoraInicio());

        return toResponse(cita);
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CitaMedicaResponse actualizar(Long id, ActualizarCitaRequest req, String ip) {

        CitaMedica cita = getCitaModificable(id);

        LocalTime horaFin = req.getHoraInicio()
                .plusMinutes(req.getDuracionMinutos() != null
                        ? req.getDuracionMinutos() : 30);

        // Validar disponibilidad excluyendo la propia cita
        validarDisponibilidad(req.getFechaCita(), req.getHoraInicio(), horaFin, id);

        cita.setFechaCita(req.getFechaCita());
        cita.setHoraInicio(req.getHoraInicio());
        cita.setHoraFin(horaFin);
        cita.setDuracionMinutos(req.getDuracionMinutos() != null
                ? req.getDuracionMinutos() : 30);
        if (req.getTipoCita() != null) cita.setTipoCita(req.getTipoCita());
        if (req.getMotivoCita() != null) cita.setMotivoCita(req.getMotivoCita());
        if (req.getNotasAdicionales() != null) cita.setNotasAdicionales(req.getNotasAdicionales());

        cita = citaRepo.save(cita);

        auditoriaService.registrar("UPDATE", MODULO,
                "Cita actualizada ID: " + id
                        + " | Nueva fecha: " + req.getFechaCita()
                        + " " + req.getHoraInicio(), ip);

        return toResponse(cita);
    }

    // ── Obtener ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CitaMedicaResponse obtener(Long id) {
        CitaMedica cita = citaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cita no encontrada: " + id));
        return toResponse(cita);
    }

    // ── Listar ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CitaMedicaResumenResponse> listar(
            EstadoCita estado, LocalDate fecha,
            String busqueda, int pagina, int tamano) {

        PageRequest pr = PageRequest.of(pagina, tamano,
                Sort.by("fechaCita").descending()
                        .and(Sort.by("horaInicio").descending()));

        String terminoBusqueda = (busqueda == null || busqueda.isBlank()) ? "" : busqueda;

        Page<CitaMedicaResumenResponse> page = citaRepo
                .listarConFiltros(estado, fecha, terminoBusqueda, pr)
                .map(this::toResumen);

        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public void cancelar(Long id, CancelarCitaRequest req, String ip) {

        CitaMedica cita = getCitaModificable(id);
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion(req.getMotivoCancelacion());
        cita.setFechaCancelacion(LocalDateTime.now());
        citaRepo.save(cita);

        auditoriaService.registrar("CANCEL", MODULO,
                "Cita cancelada ID: " + id
                        + " | Motivo: " + req.getMotivoCancelacion(), ip);
    }

    // ── Marcar atendida ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void marcarAtendida(Long id, String ip) {
        CitaMedica cita = getCitaActiva(id);
        cita.setEstado(EstadoCita.ATENDIDA);
        citaRepo.save(cita);
        auditoriaService.registrar("UPDATE", MODULO,
                "Cita marcada como ATENDIDA ID: " + id, ip);
    }

    // ── Marcar no asistió ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void marcarNoAsistio(Long id, String ip) {
        CitaMedica cita = getCitaActiva(id);
        cita.setEstado(EstadoCita.NO_ASISTIO);
        citaRepo.save(cita);
        auditoriaService.registrar("UPDATE", MODULO,
                "Cita marcada como NO_ASISTIO ID: " + id, ip);
    }

    // ── Calendario ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CitaMedicaResumenResponse> obtenerPorRangoFechas(
            LocalDate inicio, LocalDate fin) {
        return citaRepo.findByRangoFechas(inicio, fin)
                .stream().map(this::toResumen).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaMedicaResumenResponse> obtenerPorFecha(LocalDate fecha) {
        return citaRepo.findByFecha(fecha)
                .stream().map(this::toResumen).collect(Collectors.toList());
    }

    // ── Disponibilidad ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadResponse verificarDisponibilidad(
            LocalDate fecha, String horaInicioStr, int duracionMinutos) {

        LocalTime horaInicio = LocalTime.parse(horaInicioStr,
                DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime horaFin = horaInicio.plusMinutes(duracionMinutos);

        boolean hayConflicto = citaRepo.existeConflictoHorario(
                fecha, horaInicio, horaFin, ESTADOS_LIBRES);

        // Obtener todos los slots ocupados del día para el calendario
        List<DisponibilidadResponse.SlotOcupado> ocupados = citaRepo
                .findByFecha(fecha).stream()
                .filter(c -> !ESTADOS_LIBRES.contains(c.getEstado()))
                .map(c -> DisponibilidadResponse.SlotOcupado.builder()
                        .horaInicio(c.getHoraInicio())
                        .horaFin(c.getHoraFin())
                        .pacienteNombre(c.getPaciente().getNombreCompleto())
                        .tipoCita(c.getTipoCita().name())
                        .estado(c.getEstado().name())
                        .build())
                .collect(Collectors.toList());

        return DisponibilidadResponse.builder()
                .fecha(fecha)
                .disponible(!hayConflicto)
                .mensaje(hayConflicto
                        ? "El horario " + horaInicioStr + " ya está ocupado el "
                        + fecha + ". Selecciona otro horario."
                        : "Horario disponible")
                .slotsOcupados(ocupados)
                .build();
    }

    // ── Citas por paciente ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CitaMedicaResumenResponse> citasPorPaciente(
            Long pacienteId, int pagina, int tamano) {

        PageRequest pr = PageRequest.of(pagina, tamano);
        Page<CitaMedicaResumenResponse> page = citaRepo
                .findByPacienteIdOrderByFechaCitaDescHoraInicioDesc(pacienteId, pr)
                .map(this::toResumen);
        return PageResponse.of(page);
    }

    // ── Validación interna ────────────────────────────────────────────────────

    private void validarDisponibilidad(LocalDate fecha, LocalTime horaInicio,
                                       LocalTime horaFin, Long excludeId) {
        boolean conflicto;

        if (excludeId != null) {
            conflicto = citaRepo.existeConflictoHorarioExcluyendo(
                    fecha, horaInicio, horaFin, ESTADOS_LIBRES, excludeId);
        } else {
            conflicto = citaRepo.existeConflictoHorario(
                    fecha, horaInicio, horaFin, ESTADOS_LIBRES);
        }

        if (conflicto) {
            throw new ReglaNegocioException(
                    "El horario " + horaInicio + " del " + fecha
                            + " ya está ocupado. Por favor selecciona otro horario.");
        }
    }

    // ── Helpers de estado ─────────────────────────────────────────────────────

    private CitaMedica getCitaModificable(Long id) {
        CitaMedica cita = citaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cita no encontrada: " + id));

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException(
                    "No se puede modificar una cita cancelada.");
        }
        if (cita.getEstado() == EstadoCita.ATENDIDA) {
            throw new ReglaNegocioException(
                    "No se puede modificar una cita que ya fue atendida.");
        }
        return cita;
    }

    private CitaMedica getCitaActiva(Long id) {
        CitaMedica cita = citaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cita no encontrada: " + id));

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException(
                    "La cita ya está cancelada.");
        }
        return cita;
    }

    // ── Obtener usuario autenticado ───────────────────────────────────────────

    private Usuario getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ReglaNegocioException("No hay usuario autenticado.");
        return usuarioRepo.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + auth.getName()));
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private CitaMedicaResponse toResponse(CitaMedica c) {
        return CitaMedicaResponse.builder()
                .id(c.getId())
                .pacienteId(c.getPaciente().getId())
                .pacienteCedula(c.getPaciente().getCedula())
                .pacienteNombreCompleto(c.getPaciente().getNombreCompleto())
                .pacienteCelular(c.getPaciente().getCelular())
                .usuarioId(c.getUsuario().getId())
                .usuarioNombreCompleto(c.getUsuario().getNombreCompleto())
                .fechaCita(c.getFechaCita())
                .horaInicio(c.getHoraInicio())
                .horaFin(c.getHoraFin())
                .duracionMinutos(c.getDuracionMinutos())
                .tipoCita(c.getTipoCita())
                .motivoCita(c.getMotivoCita())
                .notasAdicionales(c.getNotasAdicionales())
                .estado(c.getEstado())
                .motivoCancelacion(c.getMotivoCancelacion())
                .fechaCancelacion(c.getFechaCancelacion())
                .fechaCreacion(c.getFechaCreacion())
                .fechaActualizacion(c.getFechaActualizacion())
                .creadoPor(c.getCreadoPor())
                .actualizadoPor(c.getActualizadoPor())
                .build();
    }

    private CitaMedicaResumenResponse toResumen(CitaMedica c) {
        return CitaMedicaResumenResponse.builder()
                .id(c.getId())
                .fechaCita(c.getFechaCita())
                .horaInicio(c.getHoraInicio() != null
                        ? c.getHoraInicio().toString().substring(0, 5) : null)
                .horaFin(c.getHoraFin() != null
                        ? c.getHoraFin().toString().substring(0, 5) : null)
                .duracionMinutos(c.getDuracionMinutos())
                .tipoCita(c.getTipoCita())
                .motivoCita(c.getMotivoCita())
                .pacienteId(c.getPaciente().getId())
                .pacienteNombreCompleto(c.getPaciente().getNombreCompleto())
                .pacienteCedula(c.getPaciente().getCedula())
                .pacienteCelular(c.getPaciente().getCelular())
                .estado(c.getEstado())
                .build();
    }
}