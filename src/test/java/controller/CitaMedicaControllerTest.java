package controller;

import tools.jackson.databind.ObjectMapper;
import ec.salud.citas.hclinicas.controller.CitaMedicaController;
import ec.salud.citas.hclinicas.dto.request.ActualizarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CancelarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearCitaRequest;
import ec.salud.citas.hclinicas.dto.response.CitaMedicaResponse;
import ec.salud.citas.hclinicas.dto.response.CitaMedicaResumenResponse;
import ec.salud.citas.hclinicas.dto.response.DisponibilidadResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.enumerado.TipoCita;
import ec.salud.citas.hclinicas.service.CitaMedicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias para CitaMedicaController usando MockMvc en modo standalone.
 * No levanta el contexto completo de Spring, por lo que CitaMedicaService se
 * simula con Mockito para validar exclusivamente la capa de presentación:
 * mapeo de parámetros/rutas, código de respuesta y estructura del ApiResponse.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CitaMedicaController - Pruebas unitarias del módulo de citas médicas (HU-016..HU-020)")
class CitaMedicaControllerTest {

    @Mock
    private CitaMedicaService citaService;

    @InjectMocks
    private CitaMedicaController citaMedicaController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(citaMedicaController).build();
    }

    private CitaMedicaResponse respuestaEjemplo() {
        return CitaMedicaResponse.builder()
                .id(1L)
                .pacienteId(10L)
                .pacienteCedula("0102030405")
                .pacienteNombreCompleto("Ana Pérez")
                .pacienteCelular("0991234567")
                .usuarioId(2L)
                .usuarioNombreCompleto("Alexandra León")
                .fechaCita(LocalDate.of(2026, 7, 20))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(8, 30))
                .duracionMinutos(30)
                .tipoCita(TipoCita.CONTROL)
                .motivoCita("Control prenatal")
                .estado(EstadoCita.PROGRAMADA)
                .build();
    }

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /citas con datos válidos retorna 201 y la cita creada")
    void crearCitaValidaRetorna201ConCitaCreada() throws Exception {
        CrearCitaRequest request = new CrearCitaRequest();
        request.setPacienteId(10L);
        request.setFechaCita(LocalDate.of(2026, 7, 20));
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setDuracionMinutos(30);
        request.setTipoCita(TipoCita.CONTROL);
        request.setMotivoCita("Control prenatal");

        when(citaService.crear(any(CrearCitaRequest.class), any(String.class))).thenReturn(respuestaEjemplo());

        mockMvc.perform(post("/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Cita agendada exitosamente"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.pacienteNombreCompleto").value("Ana Pérez"))
                .andExpect(jsonPath("$.data.estado").value("PROGRAMADA"));
    }

    @Test
    @DisplayName("Con pacienteId nulo retorna 400 por validación @Valid")
    void crearConPacienteIdNuloRetorna400() throws Exception {
        CrearCitaRequest request = new CrearCitaRequest();
        request.setPacienteId(null);
        request.setFechaCita(LocalDate.of(2026, 7, 20));
        request.setHoraInicio(LocalTime.of(8, 0));

        mockMvc.perform(post("/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Con fechaCita nula retorna 400 por validación @Valid")
    void crearConFechaCitaNulaRetorna400() throws Exception {
        CrearCitaRequest request = new CrearCitaRequest();
        request.setPacienteId(10L);
        request.setFechaCita(null);
        request.setHoraInicio(LocalTime.of(8, 0));

        mockMvc.perform(post("/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Con horaInicio nula retorna 400 por validación @Valid")
    void crearConHoraInicioNulaRetorna400() throws Exception {
        CrearCitaRequest request = new CrearCitaRequest();
        request.setPacienteId(10L);
        request.setFechaCita(LocalDate.of(2026, 7, 20));
        request.setHoraInicio(null);

        mockMvc.perform(post("/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── Listar ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /citas con filtros opcionales retorna 200 y delega los parámetros")
    void listarCitasConFiltrosRetorna200YDelegaParametros() throws Exception {
        CitaMedicaResumenResponse resumen = CitaMedicaResumenResponse.builder()
                .id(1L).pacienteNombreCompleto("Ana Pérez").estado(EstadoCita.PROGRAMADA).build();
        PageResponse<CitaMedicaResumenResponse> page = PageResponse.<CitaMedicaResumenResponse>builder()
                .contenido(List.of(resumen))
                .paginaActual(1).totalPaginas(1).totalElementos(1).tamanioPagina(5)
                .primera(true).ultima(true).build();

        when(citaService.listar(eq(EstadoCita.PROGRAMADA), eq(LocalDate.of(2026, 7, 15)),
                eq("perez"), eq(1), eq(5))).thenReturn(page);

        mockMvc.perform(get("/citas")
                        .param("estado", "PROGRAMADA")
                        .param("fecha", "2026-07-15")
                        .param("busqueda", "perez")
                        .param("pagina", "1")
                        .param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[0].id").value(1))
                .andExpect(jsonPath("$.data.totalElementos").value(1));

        verify(citaService).listar(eq(EstadoCita.PROGRAMADA), eq(LocalDate.of(2026, 7, 15)),
                eq("perez"), eq(1), eq(5));
    }

    @Test
    @DisplayName("GET /citas sin parámetros usa los valores por defecto")
    void listarCitasSinParametrosUsaValoresPorDefecto() throws Exception {
        PageResponse<CitaMedicaResumenResponse> page = PageResponse.<CitaMedicaResumenResponse>builder()
                .contenido(List.of()).paginaActual(0).totalPaginas(0).totalElementos(0)
                .tamanioPagina(10).primera(true).ultima(true).build();

        when(citaService.listar(isNull(), isNull(), eq(""), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/citas"))
                .andExpect(status().isOk());

        verify(citaService).listar(isNull(), isNull(), eq(""), eq(0), eq(10));
    }

    // ── Obtener ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /citas/{id} retorna 200 y el detalle de la cita")
    void obtenerCitaPorIdRetorna200() throws Exception {
        when(citaService.obtener(1L)).thenReturn(respuestaEjemplo());

        mockMvc.perform(get("/citas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Cita obtenida"));
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /citas/{id} con datos válidos retorna 200 y la cita actualizada")
    void actualizarCitaValidaRetorna200() throws Exception {
        ActualizarCitaRequest request = new ActualizarCitaRequest();
        request.setFechaCita(LocalDate.of(2026, 7, 21));
        request.setHoraInicio(LocalTime.of(9, 0));
        request.setDuracionMinutos(45);

        when(citaService.actualizar(eq(1L), any(ActualizarCitaRequest.class), any(String.class)))
                .thenReturn(respuestaEjemplo());

        mockMvc.perform(put("/citas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Cita actualizada exitosamente"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ── Cancelar / cambios de estado ──────────────────────────────────────────

    @Test
    @DisplayName("PATCH /citas/{id}/cancelar con motivo retorna 200")
    void cancelarCitaRetorna200() throws Exception {
        CancelarCitaRequest request = new CancelarCitaRequest();
        request.setMotivoCancelacion("Paciente reprogramó");

        mockMvc.perform(patch("/citas/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Cita cancelada exitosamente"));

        verify(citaService).cancelar(eq(1L), any(CancelarCitaRequest.class), any(String.class));
    }

    @Test
    @DisplayName("Con motivoCancelacion vacío retorna 400 por validación @Valid")
    void cancelarConMotivoVacioRetorna400() throws Exception {
        CancelarCitaRequest request = new CancelarCitaRequest();
        request.setMotivoCancelacion("");

        mockMvc.perform(patch("/citas/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /citas/{id}/atendida retorna 200")
    void marcarAtendidaRetorna200() throws Exception {
        mockMvc.perform(patch("/citas/1/atendida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Cita marcada como atendida"));

        verify(citaService).marcarAtendida(eq(1L), any(String.class));
    }

    @Test
    @DisplayName("PATCH /citas/{id}/no-asistio retorna 200")
    void marcarNoAsistioRetorna200() throws Exception {
        mockMvc.perform(patch("/citas/1/no-asistio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Cita marcada como no asistió"));

        verify(citaService).marcarNoAsistio(eq(1L), any(String.class));
    }

    // ── Calendario ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /citas/calendario con rango de fechas retorna 200")
    void calendarioRetorna200ConRangoFechas() throws Exception {
        CitaMedicaResumenResponse resumen = CitaMedicaResumenResponse.builder()
                .id(1L).estado(EstadoCita.PROGRAMADA).build();

        when(citaService.obtenerPorRangoFechas(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(resumen));

        mockMvc.perform(get("/citas/calendario")
                        .param("inicio", "2026-07-01")
                        .param("fin", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("GET /citas/dia con fecha retorna 200")
    void porDiaRetorna200() throws Exception {
        CitaMedicaResumenResponse resumen = CitaMedicaResumenResponse.builder()
                .id(2L).estado(EstadoCita.PROGRAMADA).build();

        when(citaService.obtenerPorFecha(LocalDate.of(2026, 7, 15)))
                .thenReturn(List.of(resumen));

        mockMvc.perform(get("/citas/dia").param("fecha", "2026-07-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2));
    }

    // ── Disponibilidad ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /citas/disponibilidad retorna 200 con el resultado de disponibilidad")
    void disponibilidadRetorna200() throws Exception {
        DisponibilidadResponse response = DisponibilidadResponse.builder()
                .fecha(LocalDate.of(2026, 7, 15))
                .disponible(true)
                .mensaje("Horario disponible")
                .slotsOcupados(List.of())
                .build();

        when(citaService.verificarDisponibilidad(LocalDate.of(2026, 7, 15), "08:00", 30))
                .thenReturn(response);

        mockMvc.perform(get("/citas/disponibilidad")
                        .param("fecha", "2026-07-15")
                        .param("hora", "08:00")
                        .param("duracion", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.disponible").value(true))
                .andExpect(jsonPath("$.data.mensaje").value("Horario disponible"));
    }

    // ── Historial del paciente ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /citas/paciente/{pacienteId} retorna 200 con el historial paginado")
    void porPacienteRetorna200ConPaginacion() throws Exception {
        CitaMedicaResumenResponse resumen = CitaMedicaResumenResponse.builder()
                .id(3L).pacienteId(10L).estado(EstadoCita.ATENDIDA).build();
        PageResponse<CitaMedicaResumenResponse> page = PageResponse.<CitaMedicaResumenResponse>builder()
                .contenido(List.of(resumen))
                .paginaActual(0).totalPaginas(1).totalElementos(1).tamanioPagina(10)
                .primera(true).ultima(true).build();

        when(citaService.citasPorPaciente(10L, 0, 10)).thenReturn(page);

        mockMvc.perform(get("/citas/paciente/10")
                        .param("pagina", "0")
                        .param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[0].id").value(3));

        verify(citaService).citasPorPaciente(10L, 0, 10);
    }
}
