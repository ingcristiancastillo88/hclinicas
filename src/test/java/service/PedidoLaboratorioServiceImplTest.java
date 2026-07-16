package service;

import ec.salud.citas.hclinicas.dto.request.PedidoLaboratorioRequest;
import ec.salud.citas.hclinicas.entity.Consulta;
import ec.salud.citas.hclinicas.entity.HistoriaClinica;
import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.entity.PedidoLaboratorio;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.repository.PedidoLaboratorioRepository;
import ec.salud.citas.hclinicas.service.impl.PedidoLaboratorioServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para PedidoLaboratorioServiceImpl.
 * Al igual que RecetaServiceImpl, esta clase no implementa ninguna interfaz,
 * por lo que se construye manualmente con el constructor generado por
 * @RequiredArgsConstructor, usando Mockito para los repositorios y una
 * instancia real de ObjectMapper (tools.jackson.databind.ObjectMapper).
 *
 * Se cubren los métodos de persistencia (guardarPedido, obtenerUltimoPedido)
 * y un smoke test de generarPdfLaboratorio/generarPdfImagenologia que valida
 * que el resultado es un PDF válido, sin inspeccionar el contenido/layout interno.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoLaboratorioServiceImpl - Pruebas unitarias")
class PedidoLaboratorioServiceImplTest {

    @Mock
    private ConsultaRepository consultaRepo;

    @Mock
    private PedidoLaboratorioRepository pedidoRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    private PedidoLaboratorioServiceImpl pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoLaboratorioServiceImpl(consultaRepo, pedidoRepo, mapper);
        // Campos @Value sin contexto de Spring: se fijan manualmente los valores
        // por defecto para que el PDF (iText7) no reciba textos nulos.
        ReflectionTestUtils.setField(pedidoService, "especialista", "Dra. Alexandra Leon");
        ReflectionTestUtils.setField(pedidoService, "telefono", "096 044 0040 - 099 146 3226");
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private PedidoLaboratorioRequest crearPedidoLaboratorioRequest() {
        PedidoLaboratorioRequest req = new PedidoLaboratorioRequest();
        req.setTipo("LABORATORIO");
        req.setExamenesSeleccionados(Map.of("hematologia", List.of("Biometría Hemática")));
        req.setResumenClinico("Paciente asintomática");
        req.setDiagnostico("Control prenatal");
        req.setCodigoCie10("Z34.9");
        req.setObservaciones("Sin observaciones");
        req.setEmbarazo(true);
        req.setSemGestacion("12 semanas");
        return req;
    }

    // ── guardarPedido ─────────────────────────────────────────────────────

    @Test
    @DisplayName("guardarPedido con usuario autenticado usa su nombre como creadoPor")
    void guardarPedidoConUsuarioAutenticadoUsaSuNombre() {
        Consulta consulta = Consulta.builder().id(1L).build();
        when(consultaRepo.findById(1L)).thenReturn(Optional.of(consulta));
        when(pedidoRepo.save(any(PedidoLaboratorio.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("dra.leon@klinixmed.org");
        SecurityContextHolder.getContext().setAuthentication(auth);

        PedidoLaboratorioRequest req = crearPedidoLaboratorioRequest();

        PedidoLaboratorio resultado = pedidoService.guardarPedido(1L, req);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getConsulta()).isEqualTo(consulta);
        assertThat(resultado.getTipo()).isEqualTo("LABORATORIO");
        assertThat(resultado.getResumenClinico()).isEqualTo("Paciente asintomática");
        assertThat(resultado.getCreadoPor()).isEqualTo("dra.leon@klinixmed.org");
        assertThat(resultado.getExamenesJson()).contains("hematologia");
        verify(pedidoRepo).save(any(PedidoLaboratorio.class));
    }

    @Test
    @DisplayName("guardarPedido sin autenticación asigna 'sistema' como creadoPor")
    void guardarPedidoSinAutenticacionUsaSistema() {
        Consulta consulta = Consulta.builder().id(1L).build();
        when(consultaRepo.findById(1L)).thenReturn(Optional.of(consulta));
        when(pedidoRepo.save(any(PedidoLaboratorio.class))).thenAnswer(inv -> inv.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(null);

        PedidoLaboratorioRequest req = crearPedidoLaboratorioRequest();

        PedidoLaboratorio resultado = pedidoService.guardarPedido(1L, req);

        assertThat(resultado.getCreadoPor()).isEqualTo("sistema");
    }

    @Test
    @DisplayName("guardarPedido con consulta inexistente lanza RecursoNoEncontradoException")
    void guardarPedidoConConsultaInexistenteLanzaExcepcion() {
        when(consultaRepo.findById(99L)).thenReturn(Optional.empty());

        PedidoLaboratorioRequest req = crearPedidoLaboratorioRequest();

        assertThatThrownBy(() -> pedidoService.guardarPedido(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(pedidoRepo, never()).save(any());
    }

    // ── obtenerUltimoPedido ───────────────────────────────────────────────

    @Test
    @DisplayName("obtenerUltimoPedido sin registros retorna null")
    void obtenerUltimoPedidoSinRegistrosRetornaNull() {
        when(pedidoRepo.findByConsultaIdAndTipoOrderByFechaCreacionDesc(1L, "LABORATORIO"))
                .thenReturn(List.of());

        PedidoLaboratorioRequest resultado = pedidoService.obtenerUltimoPedido(1L, "LABORATORIO");

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("obtenerUltimoPedido con JSON válido retorna los exámenes deserializados")
    void obtenerUltimoPedidoConJsonValidoRetornaExamenes() {
        Map<String, List<String>> examenes = Map.of(
                "hematologia", List.of("Biometría Hemática", "Hematocrito"));
        String json = mapper.writeValueAsString(examenes);

        PedidoLaboratorio pedido = PedidoLaboratorio.builder()
                .tipo("LABORATORIO")
                .resumenClinico("Paciente asintomática")
                .diagnostico("Control prenatal")
                .codigoCie10("Z34.9")
                .observaciones("Ninguna")
                .examenesJson(json)
                .build();
        when(pedidoRepo.findByConsultaIdAndTipoOrderByFechaCreacionDesc(1L, "LABORATORIO"))
                .thenReturn(List.of(pedido));

        PedidoLaboratorioRequest resultado = pedidoService.obtenerUltimoPedido(1L, "LABORATORIO");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipo()).isEqualTo("LABORATORIO");
        assertThat(resultado.getResumenClinico()).isEqualTo("Paciente asintomática");
        assertThat(resultado.getExamenesSeleccionados()).containsKey("hematologia");
        assertThat(resultado.getExamenesSeleccionados().get("hematologia"))
                .containsExactly("Biometría Hemática", "Hematocrito");
    }

    @Test
    @DisplayName("obtenerUltimoPedido con JSON malformado retorna Map.of() sin lanzar excepción")
    void obtenerUltimoPedidoConJsonMalformadoRetornaMapVacio() {
        PedidoLaboratorio pedido = PedidoLaboratorio.builder()
                .tipo("LABORATORIO")
                .examenesJson("{esto no es un json valido")
                .build();
        when(pedidoRepo.findByConsultaIdAndTipoOrderByFechaCreacionDesc(1L, "LABORATORIO"))
                .thenReturn(List.of(pedido));

        PedidoLaboratorioRequest resultado = pedidoService.obtenerUltimoPedido(1L, "LABORATORIO");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getExamenesSeleccionados()).isEmpty();
    }

    // ── generarPdfLaboratorio ─────────────────────────────────────────────

    private Consulta crearConsultaConPacienteCompleto() {
        Paciente paciente = Paciente.builder()
                .nombres("Ana")
                .apellidos("Torres")
                .cedula("1723456789")
                .fechaNacimiento(LocalDate.of(1985, 3, 10))
                .build();
        HistoriaClinica historia = HistoriaClinica.builder()
                .paciente(paciente)
                .build();
        return Consulta.builder()
                .id(1L)
                .historiaClinica(historia)
                .diagnosticoPrincipal("Control prenatal")
                .codigoCie10("Z34.9")
                .indicaciones("Reposo relativo")
                .build();
    }

    @Test
    @DisplayName("generarPdfLaboratorio con consulta inexistente lanza RecursoNoEncontradoException")
    void generarPdfLaboratorioConConsultaInexistenteLanzaExcepcion() {
        when(consultaRepo.findById(99L)).thenReturn(Optional.empty());

        PedidoLaboratorioRequest req = crearPedidoLaboratorioRequest();

        assertThatThrownBy(() -> pedidoService.generarPdfLaboratorio(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("generarPdfLaboratorio con consulta válida retorna un PDF no vacío")
    void generarPdfLaboratorioConConsultaValidaRetornaPdfValido() {
        Consulta consulta = crearConsultaConPacienteCompleto();
        when(consultaRepo.findById(1L)).thenReturn(Optional.of(consulta));

        PedidoLaboratorioRequest req = crearPedidoLaboratorioRequest();

        byte[] pdf = pedidoService.generarPdfLaboratorio(1L, req);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    // ── generarPdfImagenologia ────────────────────────────────────────────

    @Test
    @DisplayName("generarPdfImagenologia con consulta inexistente lanza RecursoNoEncontradoException")
    void generarPdfImagenologiaConConsultaInexistenteLanzaExcepcion() {
        when(consultaRepo.findById(99L)).thenReturn(Optional.empty());

        PedidoLaboratorioRequest req = crearPedidoLaboratorioRequest();
        req.setTipo("IMAGENOLOGIA");

        assertThatThrownBy(() -> pedidoService.generarPdfImagenologia(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("generarPdfImagenologia con consulta válida retorna un PDF no vacío")
    void generarPdfImagenologiaConConsultaValidaRetornaPdfValido() {
        Consulta consulta = crearConsultaConPacienteCompleto();
        when(consultaRepo.findById(1L)).thenReturn(Optional.of(consulta));

        PedidoLaboratorioRequest req = new PedidoLaboratorioRequest();
        req.setTipo("IMAGENOLOGIA");
        req.setTipoEstudio("ECOGRAFÍA");
        req.setDescripcion("Ecografía obstétrica");
        req.setMotivoSolicitud("Control de embarazo");
        req.setResumenClinico("Paciente asintomática");
        req.setDiagnostico("Control prenatal");
        req.setCodigoCie10("Z34.9");
        req.setMonitoreoFetal(true);
        req.setFum("01/01/2025");
        req.setEg("12 semanas");
        req.setExamenesSeleccionados(Map.of());

        byte[] pdf = pedidoService.generarPdfImagenologia(1L, req);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
