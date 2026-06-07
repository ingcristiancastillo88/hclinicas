package ec.salud.citas.hclinicas.service.impl;

import ec.salud.citas.hclinicas.dto.request.ActualizarConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearHistoriaClinicaRequest;
import ec.salud.citas.hclinicas.dto.response.*;
import ec.salud.citas.hclinicas.entity.*;
import ec.salud.citas.hclinicas.enumerado.TipoArchivo;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.*;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.HistoriaClinicaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de historias clínicas.
 * HU-010 · HU-011 · HU-012 · HU-020
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {

    private final HistoriaClinicaRepository historiaRepo;
    private final ConsultaRepository        consultaRepo;
    private final ArchivoAdjuntoRepository  archivoRepo;
    private final PacienteRepository        pacienteRepo;
    private final AuditoriaService          auditoriaService;

    @Value("${app.archivos.ruta-base:uploads/archivos}")
    private String rutaBase;

    @Value("${app.archivos.url-base:http://localhost:8080/api/archivos}")
    private String urlBase;

    private static final Set<String> EXTENSIONES_OK =
            Set.of("jpg", "jpeg", "png", "pdf", "docx", "doc", "xlsx");
    private static final long MAX_BYTES = 10 * 1024 * 1024L; // 10 MB

    private static final String MOD_HISTORIA  = "HISTORIAS_CLINICAS";
    private static final String MOD_CONSULTA  = "CONSULTAS";
    private static final String MOD_ARCHIVO   = "ARCHIVOS";

    // ── Historia Clínica ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public HistoriaClinicaResponse crearOActualizar(
            CrearHistoriaClinicaRequest req, String ip) {

        Paciente paciente = pacienteRepo.findById(req.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Paciente no encontrado: " + req.getPacienteId()));

        // Si ya existe la historia la actualizamos; si no, la creamos
        HistoriaClinica historia = historiaRepo
                .findByPacienteId(req.getPacienteId())
                .orElse(HistoriaClinica.builder().paciente(paciente).build());

        historia.setMenarquia(req.getMenarquia());
        historia.setCicloMenstrual(req.getCicloMenstrual());
        historia.setFechaUltimaMenstruacion(req.getFechaUltimaMenstruacion());
        historia.setGestas(req.getGestas());
        historia.setPartos(req.getPartos());
        historia.setCesareas(req.getCesareas());
        historia.setAbortos(req.getAbortos());
        historia.setHijosVivos(req.getHijosVivos());
        historia.setMetodoAnticonceptivo(req.getMetodoAnticonceptivo());
        historia.setUltimoPapanicolau(req.getUltimoPapanicolau());
        historia.setUltimaMamografia(req.getUltimaMamografia());
        historia.setObservacionesGenerales(req.getObservacionesGenerales());

        historia = historiaRepo.save(historia);

        auditoriaService.registrar("CREATE_UPDATE", MOD_HISTORIA,
                "Historia clínica de: " + paciente.getNombreCompleto(), ip);

        return toHistoriaResponse(historia);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinicaResponse obtenerPorId(Long id) {
        HistoriaClinica h = historiaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clínica no encontrada: " + id));
        return toHistoriaResponse(h);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinicaResponse obtenerPorPaciente(Long pacienteId) {
        HistoriaClinica h = historiaRepo.findByPacienteId(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe historia clínica para el paciente: " + pacienteId));
        return toHistoriaResponse(h);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConsultaResponse crearConsulta(CrearConsultaRequest req, String ip) {

        HistoriaClinica historia = historiaRepo.findById(req.getHistoriaClinicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clínica no encontrada: " + req.getHistoriaClinicaId()));

        Consulta consulta = Consulta.builder()
                .historiaClinica(historia)
                .fechaConsulta(req.getFechaConsulta())
                .motivoConsulta(req.getMotivoConsulta())
                .peso(req.getPeso())
                .talla(req.getTalla())
                .presionArterial(req.getPresionArterial())
                .frecuenciaCardiaca(req.getFrecuenciaCardiaca())
                .temperatura(req.getTemperatura())
                .saturacionOxigeno(req.getSaturacionOxigeno())
                .semanasGestacion(req.getSemanasGestacion())
                .examenFisico(req.getExamenFisico())
                .diagnosticoPrincipal(req.getDiagnosticoPrincipal())
                .diagnosticoSecundario(req.getDiagnosticoSecundario())
                .codigoCie10(req.getCodigoCie10())
                .tratamiento(req.getTratamiento())
                .medicacion(req.getMedicacion())
                .indicaciones(req.getIndicaciones())
                .proximaCita(req.getProximaCita())
                .observaciones(req.getObservaciones())
                .build();

        consulta = consultaRepo.save(consulta);

        auditoriaService.registrar("CREATE", MOD_CONSULTA,
                "Consulta creada ID: " + consulta.getId()
                        + " | Paciente: " + historia.getPaciente().getNombreCompleto(), ip);

        return toConsultaResponse(consulta);
    }

    @Override
    @Transactional
    public ConsultaResponse actualizarConsulta(
            Long id, ActualizarConsultaRequest req, String ip) {

        Consulta c = getConsultaActiva(id);

        c.setFechaConsulta(req.getFechaConsulta());
        c.setMotivoConsulta(req.getMotivoConsulta());
        c.setPeso(req.getPeso());
        c.setTalla(req.getTalla());
        c.setPresionArterial(req.getPresionArterial());
        c.setFrecuenciaCardiaca(req.getFrecuenciaCardiaca());
        c.setTemperatura(req.getTemperatura());
        c.setSaturacionOxigeno(req.getSaturacionOxigeno());
        c.setSemanasGestacion(req.getSemanasGestacion());
        c.setExamenFisico(req.getExamenFisico());
        c.setDiagnosticoPrincipal(req.getDiagnosticoPrincipal());
        c.setDiagnosticoSecundario(req.getDiagnosticoSecundario());
        c.setCodigoCie10(req.getCodigoCie10());
        c.setTratamiento(req.getTratamiento());
        c.setMedicacion(req.getMedicacion());
        c.setIndicaciones(req.getIndicaciones());
        c.setProximaCita(req.getProximaCita());
        c.setObservaciones(req.getObservaciones());

        c = consultaRepo.save(c);
        auditoriaService.registrar("UPDATE", MOD_CONSULTA,
                "Consulta actualizada ID: " + c.getId(), ip);

        return toConsultaResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultaResponse obtenerConsulta(Long id) {
        Consulta c = consultaRepo.findByIdConArchivos(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + id));
        return toConsultaResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConsultaResumenResponse> listarConsultas(
            Long historiaId, int pagina, int tamano) {

        PageRequest pr = PageRequest.of(pagina, tamano,
                Sort.by("fechaConsulta").descending());

        Page<ConsultaResumenResponse> page = consultaRepo
                .findActivasByHistoriaId(historiaId, pr)
                .map(this::toResumen);

        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public void eliminarConsulta(Long id, String ip) {
        Consulta c = getConsultaActiva(id);
        c.setActiva(false);
        consultaRepo.save(c);
        auditoriaService.registrar("DELETE", MOD_CONSULTA,
                "Consulta eliminada ID: " + id, ip);
    }

    // ── Archivos ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void subirArchivo(Long consultaId, MultipartFile file,
                             String tipoArchivo, String descripcion, String ip) {

        Consulta consulta = getConsultaActiva(consultaId);

        String ext = getExtension(file.getOriginalFilename());
        if (!EXTENSIONES_OK.contains(ext.toLowerCase()))
            throw new ReglaNegocioException(
                    "Extensión no permitida: " + ext
                            + ". Permitidas: " + EXTENSIONES_OK);

        if (file.getSize() > MAX_BYTES)
            throw new ReglaNegocioException("El archivo supera el máximo de 10 MB");

        String nombreAlmacenado = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(rutaBase, "consulta_" + consultaId);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(),
                    dir.resolve(nombreAlmacenado),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ReglaNegocioException("Error al guardar el archivo: " + e.getMessage());
        }

        ArchivoAdjunto archivo = ArchivoAdjunto.builder()
                .consulta(consulta)
                .nombreOriginal(StringUtils.cleanPath(
                        file.getOriginalFilename() != null
                                ? file.getOriginalFilename() : "archivo"))
                .nombreAlmacenado(nombreAlmacenado)
                .rutaArchivo("consulta_" + consultaId + "/" + nombreAlmacenado)
                .tipoMime(file.getContentType())
                .tamanoBytes(file.getSize())
                .tipoArchivo(parseTipo(tipoArchivo))
                .descripcion(descripcion)
                .build();

        archivoRepo.save(archivo);

        auditoriaService.registrar("CREATE", MOD_ARCHIVO,
                "Archivo subido: " + file.getOriginalFilename()
                        + " | Consulta: " + consultaId, ip);
    }

    @Override
    @Transactional
    public void eliminarArchivo(Long archivoId, String ip) {
        ArchivoAdjunto a = archivoRepo.findById(archivoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Archivo no encontrado: " + archivoId));
        try {
            Files.deleteIfExists(Paths.get(rutaBase, a.getRutaArchivo()));
        } catch (IOException e) {
            log.warn("No se pudo eliminar archivo del disco: {}", e.getMessage());
        }
        archivoRepo.delete(a);
        auditoriaService.registrar("DELETE", MOD_ARCHIVO,
                "Archivo eliminado: " + a.getNombreOriginal(), ip);
    }

    // ── Helpers de mapeo ──────────────────────────────────────────────────────

    private HistoriaClinicaResponse toHistoriaResponse(HistoriaClinica h) {
        Paciente p = h.getPaciente();
        Integer edad = p.getFechaNacimiento() != null
                ? Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears()
                : null;

        long total = consultaRepo.countByHistoriaClinicaIdAndActivaTrue(h.getId());

        return HistoriaClinicaResponse.builder()
                .id(h.getId())
                .pacienteId(p.getId())
                .pacienteCedula(p.getCedula())
                .pacienteNombreCompleto(p.getNombreCompleto())
                .pacienteEdad(edad)
                .menarquia(h.getMenarquia())
                .cicloMenstrual(h.getCicloMenstrual())
                .fechaUltimaMenstruacion(h.getFechaUltimaMenstruacion())
                .gestas(h.getGestas())
                .partos(h.getPartos())
                .cesareas(h.getCesareas())
                .abortos(h.getAbortos())
                .hijosVivos(h.getHijosVivos())
                .metodoAnticonceptivo(h.getMetodoAnticonceptivo())
                .ultimoPapanicolau(h.getUltimoPapanicolau())
                .ultimaMamografia(h.getUltimaMamografia())
                .observacionesGenerales(h.getObservacionesGenerales())
                .totalConsultas(total)
                .fechaCreacion(h.getFechaCreacion())
                .fechaActualizacion(h.getFechaActualizacion())
                .creadoPor(h.getCreadoPor())
                .actualizadoPor(h.getActualizadoPor())
                .build();
    }

    private ConsultaResponse toConsultaResponse(Consulta c) {
        Double imc = calcularImc(c.getPeso(), c.getTalla());

        List<ArchivoAdjuntoResponse> archivos = c.getArchivos() == null
                ? List.of()
                : c.getArchivos().stream()
                .map(this::toArchivoResponse)
                .collect(Collectors.toList());

        return ConsultaResponse.builder()
                .id(c.getId())
                .historiaClinicaId(c.getHistoriaClinica().getId())
                .fechaConsulta(c.getFechaConsulta())
                .motivoConsulta(c.getMotivoConsulta())
                .peso(c.getPeso())
                .talla(c.getTalla())
                .imc(imc)
                .presionArterial(c.getPresionArterial())
                .frecuenciaCardiaca(c.getFrecuenciaCardiaca())
                .temperatura(c.getTemperatura())
                .saturacionOxigeno(c.getSaturacionOxigeno())
                .semanasGestacion(c.getSemanasGestacion())
                .examenFisico(c.getExamenFisico())
                .diagnosticoPrincipal(c.getDiagnosticoPrincipal())
                .diagnosticoSecundario(c.getDiagnosticoSecundario())
                .codigoCie10(c.getCodigoCie10())
                .tratamiento(c.getTratamiento())
                .medicacion(c.getMedicacion())
                .indicaciones(c.getIndicaciones())
                .proximaCita(c.getProximaCita())
                .observaciones(c.getObservaciones())
                .archivos(archivos)
                .totalArchivos(archivos.size())
                .fechaCreacion(c.getFechaCreacion())
                .fechaActualizacion(c.getFechaActualizacion())
                .creadoPor(c.getCreadoPor())
                .actualizadoPor(c.getActualizadoPor())
                .build();
    }

    private ConsultaResumenResponse toResumen(Consulta c) {
        int nArchivos = c.getArchivos() == null ? 0 : c.getArchivos().size();
        return ConsultaResumenResponse.builder()
                .id(c.getId())
                .fechaConsulta(c.getFechaConsulta())
                .motivoConsulta(c.getMotivoConsulta())
                .diagnosticoPrincipal(c.getDiagnosticoPrincipal())
                .codigoCie10(c.getCodigoCie10())
                .peso(c.getPeso())
                .presionArterial(c.getPresionArterial())
                .semanasGestacion(c.getSemanasGestacion())
                .totalArchivos(nArchivos)
                .fechaCreacion(c.getFechaCreacion())
                .creadoPor(c.getCreadoPor())
                .build();
    }

    private ArchivoAdjuntoResponse toArchivoResponse(ArchivoAdjunto a) {
        return ArchivoAdjuntoResponse.builder()
                .id(a.getId())
                .nombreOriginal(a.getNombreOriginal())
                .tipoMime(a.getTipoMime())
                .tamanoBytes(a.getTamanoBytes())
                .tipoArchivo(a.getTipoArchivo())
                .descripcion(a.getDescripcion())
                .urlDescarga(urlBase + "/" + a.getRutaArchivo())
                .fechaCreacion(a.getFechaCreacion())
                .creadoPor(a.getCreadoPor())
                .build();
    }

    private Consulta getConsultaActiva(Long id) {
        return consultaRepo.findById(id)
                .filter(c -> Boolean.TRUE.equals(c.getActiva()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + id));
    }

    private Double calcularImc(Double peso, Double talla) {
        if (peso == null || talla == null || talla == 0) return null;
        double m = talla / 100.0;
        return Math.round((peso / (m * m)) * 100.0) / 100.0;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private TipoArchivo parseTipo(String tipo) {
        try { return TipoArchivo.valueOf(tipo.toUpperCase()); }
        catch (Exception e) { return TipoArchivo.OTRO; }
    }
}