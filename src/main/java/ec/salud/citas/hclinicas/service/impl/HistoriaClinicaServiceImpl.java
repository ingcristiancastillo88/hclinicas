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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de historias clínicas.
 * HU-010 Registro · HU-011 Visualización · HU-012 Archivos · HU-020 Paciente
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

    // Extensiones permitidas (RNF-007 / CU-006)
    private static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of("jpg", "jpeg", "png", "pdf", "docx", "doc", "xlsx");

    private static final long MAX_TAMANO_BYTES = 10 * 1024 * 1024L; // 10 MB

    private static final String MODULO_HISTORIA  = "HISTORIAS_CLINICAS";
    private static final String MODULO_CONSULTA  = "CONSULTAS";
    private static final String MODULO_ARCHIVO   = "ARCHIVOS";

    // ── Historia Clínica ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public HistoriaClinicaResponse crearOActualizar(
            CrearHistoriaClinicaRequest request, String ipOrigen) {

        Paciente paciente = pacienteRepo.findById(request.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Paciente no encontrado con ID: " + request.getPacienteId()));

        // Si ya existe la historia la actualizamos, si no la creamos
        HistoriaClinica historia = historiaRepo
                .findByPacienteId(request.getPacienteId())
                .orElse(HistoriaClinica.builder().paciente(paciente).build());

        historia.setMenarquia(request.getMenarquia());
        historia.setCicloMenstrual(request.getCicloMenstrual());
        historia.setFechaUltimaMenstruacion(request.getFechaUltimaMenstruacion());
        historia.setGestas(request.getGestas());
        historia.setPartos(request.getPartos());
        historia.setCesareas(request.getCesareas());
        historia.setAbortos(request.getAbortos());
        historia.setHijosVivos(request.getHijosVivos());
        historia.setMetodoAnticonceptivo(request.getMetodoAnticonceptivo());
        historia.setUltimoPapanicolau(request.getUltimoPapanicolau());
        historia.setUltimaMamografia(request.getUltimaMamografia());
        historia.setObservacionesGenerales(request.getObservacionesGenerales());

        historia = historiaRepo.save(historia);

        String accion = historia.getId() != null ? "UPDATE" : "CREATE";
        auditoriaService.registrar(accion, MODULO_HISTORIA,
                "Historia clínica de: " + paciente.getNombreCompleto(), ipOrigen);

        return toHistoriaResponse(historia);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinicaResponse obtenerPorPaciente(Long pacienteId) {
        HistoriaClinica historia = historiaRepo.findByPacienteId(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe historia clínica para el paciente ID: " + pacienteId));
        return toHistoriaResponse(historia);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinicaResponse obtenerPorId(Long id) {
        HistoriaClinica historia = historiaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clínica no encontrada: " + id));
        return toHistoriaResponse(historia);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConsultaResponse crearConsulta(
            CrearConsultaRequest request, String ipOrigen) {

        HistoriaClinica historia = historiaRepo.findById(request.getHistoriaClinicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clínica no encontrada: " + request.getHistoriaClinicaId()));

        Consulta consulta = Consulta.builder()
                .historiaClinica(historia)
                .fechaConsulta(request.getFechaConsulta())
                .motivoConsulta(request.getMotivoConsulta())
                .peso(request.getPeso())
                .talla(request.getTalla())
                .presionArterial(request.getPresionArterial())
                .frecuenciaCardiaca(request.getFrecuenciaCardiaca())
                .temperatura(request.getTemperatura())
                .saturacionOxigeno(request.getSaturacionOxigeno())
                .semanasGestacion(request.getSemanasGestacion())
                .examenFisico(request.getExamenFisico())
                .diagnosticoPrincipal(request.getDiagnosticoPrincipal())
                .diagnosticoSecundario(request.getDiagnosticoSecundario())
                .codigoCie10(request.getCodigoCie10())
                .tratamiento(request.getTratamiento())
                .medicacion(request.getMedicacion())
                .indicaciones(request.getIndicaciones())
                .proximaCita(request.getProximaCita())
                .observaciones(request.getObservaciones())
                .build();

        consulta = consultaRepo.save(consulta);

        auditoriaService.registrar("CREATE", MODULO_CONSULTA,
                "Consulta registrada ID: " + consulta.getId()
                        + " | Paciente: " + historia.getPaciente().getNombreCompleto(),
                ipOrigen);

        return toConsultaResponse(consulta);
    }

    @Override
    @Transactional
    public ConsultaResponse actualizarConsulta(
            Long consultaId, ActualizarConsultaRequest request, String ipOrigen) {

        Consulta consulta = getConsultaActiva(consultaId);

        consulta.setFechaConsulta(request.getFechaConsulta());
        consulta.setMotivoConsulta(request.getMotivoConsulta());
        consulta.setPeso(request.getPeso());
        consulta.setTalla(request.getTalla());
        consulta.setPresionArterial(request.getPresionArterial());
        consulta.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        consulta.setTemperatura(request.getTemperatura());
        consulta.setSaturacionOxigeno(request.getSaturacionOxigeno());
        consulta.setSemanasGestacion(request.getSemanasGestacion());
        consulta.setExamenFisico(request.getExamenFisico());
        consulta.setDiagnosticoPrincipal(request.getDiagnosticoPrincipal());
        consulta.setDiagnosticoSecundario(request.getDiagnosticoSecundario());
        consulta.setCodigoCie10(request.getCodigoCie10());
        consulta.setTratamiento(request.getTratamiento());
        consulta.setMedicacion(request.getMedicacion());
        consulta.setIndicaciones(request.getIndicaciones());
        consulta.setProximaCita(request.getProximaCita());
        consulta.setObservaciones(request.getObservaciones());

        consulta = consultaRepo.save(consulta);

        auditoriaService.registrar("UPDATE", MODULO_CONSULTA,
                "Consulta actualizada ID: " + consulta.getId(), ipOrigen);

        return toConsultaResponse(consulta);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultaResponse obtenerConsulta(Long consultaId) {
        Consulta consulta = consultaRepo.findByIdWithArchivos(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));
        return toConsultaResponse(consulta);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConsultaResumenResponse> listarConsultas(
            Long historiaClinicaId, int pagina, int tamano) {

        PageRequest pageRequest = PageRequest.of(pagina, tamano,
                Sort.by("fechaConsulta").descending());

        Page<ConsultaResumenResponse> page = consultaRepo
                .findByHistoriaClinicaId(historiaClinicaId, pageRequest)
                .map(this::toConsultaResumen);

        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public void eliminarConsulta(Long consultaId, String ipOrigen) {
        Consulta consulta = getConsultaActiva(consultaId);
        consulta.setActiva(false);
        consultaRepo.save(consulta);
        auditoriaService.registrar("DELETE", MODULO_CONSULTA,
                "Consulta eliminada ID: " + consultaId, ipOrigen);
    }

    // ── Archivos ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void subirArchivo(Long consultaId, MultipartFile file,
                             String tipoArchivo, String descripcion, String ipOrigen) {

        Consulta consulta = getConsultaActiva(consultaId);

        // Validar extensión (CU-006)
        String extension = getExtension(file.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase())) {
            throw new ReglaNegocioException(
                    "Extensión de archivo no permitida: " + extension
                            + ". Permitidas: " + EXTENSIONES_PERMITIDAS);
        }

        // Validar tamaño
        if (file.getSize() > MAX_TAMANO_BYTES) {
            throw new ReglaNegocioException(
                    "El archivo supera el tamaño máximo permitido de 10 MB");
        }

        // Guardar archivo en disco
        String nombreAlmacenado = UUID.randomUUID() + "." + extension;
        Path directorioConsulta = Paths.get(rutaBase, "consulta_" + consultaId);

        try {
            Files.createDirectories(directorioConsulta);
            Path destino = directorioConsulta.resolve(nombreAlmacenado);
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ReglaNegocioException("Error al guardar el archivo: " + e.getMessage());
        }

        // Persistir referencia en BD
        ArchivoAdjunto archivo = ArchivoAdjunto.builder()
                .consulta(consulta)
                .nombreOriginal(StringUtils.cleanPath(file.getOriginalFilename()))
                .nombreAlmacenado(nombreAlmacenado)
                .rutaArchivo("consulta_" + consultaId + "/" + nombreAlmacenado)
                .tipoMime(file.getContentType())
                .tamanoBytes(file.getSize())
                .tipoArchivo(parseTipoArchivo(tipoArchivo))
                .descripcion(descripcion)
                .build();

        archivoRepo.save(archivo);

        auditoriaService.registrar("CREATE", MODULO_ARCHIVO,
                "Archivo subido: " + file.getOriginalFilename()
                        + " | Consulta ID: " + consultaId, ipOrigen);
    }

    @Override
    @Transactional
    public void eliminarArchivo(Long archivoId, String ipOrigen) {
        ArchivoAdjunto archivo = archivoRepo.findById(archivoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Archivo no encontrado: " + archivoId));

        // Eliminar del disco
        try {
            Path ruta = Paths.get(rutaBase, archivo.getRutaArchivo());
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo del disco: {}", e.getMessage());
        }

        archivoRepo.delete(archivo);

        auditoriaService.registrar("DELETE", MODULO_ARCHIVO,
                "Archivo eliminado: " + archivo.getNombreOriginal(), ipOrigen);
    }

    // ── Helpers de mapeo ──────────────────────────────────────────────────────

    private HistoriaClinicaResponse toHistoriaResponse(HistoriaClinica h) {
        Paciente p = h.getPaciente();
        Integer edad = p.getFechaNacimiento() != null
                ? Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears()
                : null;
        long totalConsultas = consultaRepo
                .countByHistoriaClinicaIdAndActivaTrue(h.getId());

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
                .totalConsultas(totalConsultas)
                .fechaCreacion(h.getFechaCreacion())
                .fechaActualizacion(h.getFechaActualizacion())
                .creadoPor(h.getCreadoPor())
                .build();
    }

    private ConsultaResponse toConsultaResponse(Consulta c) {
        Double imc = calcularImc(c.getPeso(), c.getTalla());
        List<ArchivoAdjuntoResponse> archivos = c.getArchivos() == null
                ? List.of()
                : c.getArchivos().stream().map(this::toArchivoResponse)
                .collect(Collectors.toList());

        return ConsultaResponse.builder()
                .id(c.getId())
                .historiaClinicaId(c.getHistoriaClinica().getId())
                .fechaConsulta(c.getFechaConsulta())
                .motivoConsulta(c.getMotivoConsulta())
                .peso(c.getPeso())
                .talla(c.getTalla())
                .presionArterial(c.getPresionArterial())
                .frecuenciaCardiaca(c.getFrecuenciaCardiaca())
                .temperatura(c.getTemperatura())
                .saturacionOxigeno(c.getSaturacionOxigeno())
                .semanasGestacion(c.getSemanasGestacion())
                .imc(imc)
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

    private ConsultaResumenResponse toConsultaResumen(Consulta c) {
        int archivos = c.getArchivos() == null ? 0 : c.getArchivos().size();
        return ConsultaResumenResponse.builder()
                .id(c.getId())
                .fechaConsulta(c.getFechaConsulta())
                .motivoConsulta(c.getMotivoConsulta())
                .diagnosticoPrincipal(c.getDiagnosticoPrincipal())
                .codigoCie10(c.getCodigoCie10())
                .peso(c.getPeso())
                .presionArterial(c.getPresionArterial())
                .semanasGestacion(c.getSemanasGestacion())
                .totalArchivos(archivos)
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
        double tallaM = talla / 100.0;
        return Math.round((peso / (tallaM * tallaM)) * 100.0) / 100.0;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private TipoArchivo parseTipoArchivo(String tipo) {
        try { return TipoArchivo.valueOf(tipo.toUpperCase()); }
        catch (Exception e) { return TipoArchivo.OTRO; }
    }
}