package service;

import ec.salud.citas.hclinicas.dto.request.RecetaRequest;
import ec.salud.citas.hclinicas.dto.request.RecetaRequest.MedicamentoRequest;
import ec.salud.citas.hclinicas.entity.Consulta;
import ec.salud.citas.hclinicas.entity.HistoriaClinica;
import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.entity.Receta;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.repository.RecetaRepository;
import ec.salud.citas.hclinicas.service.impl.RecetaServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para RecetaServiceImpl.
 * RecetaServiceImpl no implementa ninguna interfaz, por lo que se construye
 * manualmente con el constructor generado por @RequiredArgsConstructor,
 * usando Mockito para los repositorios y una instancia real de ObjectMapper
 * (tools.jackson.databind.ObjectMapper) ya que su serialización/deserialización
 * es simple y no aporta valor simularla.
 *
 * Se cubren únicamente los métodos de persistencia (guardarReceta,
 * obtenerUltimaReceta) y un smoke test de generarPdf que valida que el
 * resultado es un PDF válido, sin inspeccionar el contenido/layout interno.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecetaServiceImpl - Pruebas unitarias")
class RecetaServiceImplTest {

    @Mock
    private ConsultaRepository consultaRepo;

    @Mock
    private RecetaRepository recetaRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    private RecetaServiceImpl recetaService;

    @BeforeEach
    void setUp() {
        recetaService = new RecetaServiceImpl(consultaRepo, recetaRepo, mapper);
        // Campos @Value sin contexto de Spring: se fijan manualmente los valores
        // por defecto (igual que los de application.properties) para que el
        // PDF (iText7) no reciba textos nulos.
        ReflectionTestUtils.setField(recetaService, "clinicaNombre", "Consultorio Gineco-Obstetrico");
        ReflectionTestUtils.setField(recetaService, "especialista", "Dra. Alexandra Leon");
        ReflectionTestUtils.setField(recetaService, "telefono", "096 044 0040 - 099 146 3226");
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private RecetaRequest crearRecetaRequest() {
        MedicamentoRequest med = new MedicamentoRequest();
        med.setNombreGenerico("Ibuprofeno 400 mg");
        med.setNombreComercial("Buprex Flash");
        med.setPresentacion("Tabletas #10 (diez)");
        med.setIndicaciones("1 tableta cada 8 horas por 3 días");

        RecetaRequest req = new RecetaRequest();
        req.setMedicamentos(List.of(med));
        req.setPrescripcion("Reposo relativo por 3 días");
        req.setProximaCita("En 15 días");
        return req;
    }

    // ── guardarReceta ─────────────────────────────────────────────────────

    @Test
    @DisplayName("guardarReceta con usuario autenticado usa su nombre como creadoPor")
    void guardarRecetaConUsuarioAutenticadoUsaSuNombre() {
        Consulta consulta = Consulta.builder().id(1L).build();
        when(consultaRepo.findById(1L)).thenReturn(Optional.of(consulta));
        when(recetaRepo.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("dra.leon@klinixmed.org");
        SecurityContextHolder.getContext().setAuthentication(auth);

        RecetaRequest req = crearRecetaRequest();

        Receta resultado = recetaService.guardarReceta(1L, req);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getConsulta()).isEqualTo(consulta);
        assertThat(resultado.getPrescripcion()).isEqualTo("Reposo relativo por 3 días");
        assertThat(resultado.getProximaCita()).isEqualTo("En 15 días");
        assertThat(resultado.getCreadoPor()).isEqualTo("dra.leon@klinixmed.org");
        assertThat(resultado.getMedicamentosJson()).contains("Ibuprofeno 400 mg");
        verify(recetaRepo).save(any(Receta.class));
    }

    @Test
    @DisplayName("guardarReceta sin autenticación asigna 'sistema' como creadoPor")
    void guardarRecetaSinAutenticacionUsaSistema() {
        Consulta consulta = Consulta.builder().id(1L).build();
        when(consultaRepo.findById(1L)).thenReturn(Optional.of(consulta));
        when(recetaRepo.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(null);

        RecetaRequest req = crearRecetaRequest();

        Receta resultado = recetaService.guardarReceta(1L, req);

        assertThat(resultado.getCreadoPor()).isEqualTo("sistema");
    }

    @Test
    @DisplayName("guardarReceta con consulta inexistente lanza RecursoNoEncontradoException")
    void guardarRecetaConConsultaInexistenteLanzaExcepcion() {
        when(consultaRepo.findById(99L)).thenReturn(Optional.empty());

        RecetaRequest req = crearRecetaRequest();

        assertThatThrownBy(() -> recetaService.guardarReceta(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(recetaRepo, never()).save(any());
    }

    // ── obtenerUltimaReceta ───────────────────────────────────────────────

    @Test
    @DisplayName("obtenerUltimaReceta sin registros retorna null")
    void obtenerUltimaRecetaSinRegistrosRetornaNull() {
        when(recetaRepo.findTopByConsultaIdOrderByFechaCreacionDesc(1L))
                .thenReturn(Optional.empty());

        RecetaRequest resultado = recetaService.obtenerUltimaReceta(1L);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("obtenerUltimaReceta con JSON válido retorna los medicamentos deserializados")
    void obtenerUltimaRecetaConJsonValidoRetornaMedicamentos() {
        MedicamentoRequest med = new MedicamentoRequest();
        med.setNombreGenerico("Ibuprofeno 400 mg");
        med.setNombreComercial("Buprex Flash");
        String json = mapper.writeValueAsString(List.of(med));

        Receta receta = Receta.builder()
                .prescripcion("Reposo")
                .proximaCita("En 10 días")
                .medicamentosJson(json)
                .build();
        when(recetaRepo.findTopByConsultaIdOrderByFechaCreacionDesc(1L))
                .thenReturn(Optional.of(receta));

        RecetaRequest resultado = recetaService.obtenerUltimaReceta(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getPrescripcion()).isEqualTo("Reposo");
        assertThat(resultado.getProximaCita()).isEqualTo("En 10 días");
        assertThat(resultado.getMedicamentos()).hasSize(1);
        assertThat(resultado.getMedicamentos().get(0).getNombreGenerico())
                .isEqualTo("Ibuprofeno 400 mg");
    }

    @Test
    @DisplayName("obtenerUltimaReceta con JSON malformado retorna lista de medicamentos vacía sin lanzar excepción")
    void obtenerUltimaRecetaConJsonMalformadoRetornaListaVacia() {
        Receta receta = Receta.builder()
                .prescripcion("Reposo")
                .medicamentosJson("{esto no es un json valido")
                .build();
        when(recetaRepo.findTopByConsultaIdOrderByFechaCreacionDesc(1L))
                .thenReturn(Optional.of(receta));

        RecetaRequest resultado = recetaService.obtenerUltimaReceta(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getMedicamentos()).isEmpty();
    }

    // ── generarPdf ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generarPdf con consulta inexistente lanza RecursoNoEncontradoException")
    void generarPdfConConsultaInexistenteLanzaExcepcion() {
        when(consultaRepo.findById(99L)).thenReturn(Optional.empty());

        RecetaRequest req = crearRecetaRequest();

        assertThatThrownBy(() -> recetaService.generarPdf(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("generarPdf con consulta válida retorna un PDF no vacío")
    void generarPdfConConsultaValidaRetornaPdfValido() {
        Paciente paciente = Paciente.builder()
                .nombres("María")
                .apellidos("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .build();
        HistoriaClinica historia = HistoriaClinica.builder()
                .paciente(paciente)
                .build();
        Consulta consulta = Consulta.builder()
                .id(1L)
                .historiaClinica(historia)
                .build();
        when(consultaRepo.findById(1L)).thenReturn(Optional.of(consulta));

        RecetaRequest req = crearRecetaRequest();

        byte[] pdf = recetaService.generarPdf(1L, req);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
