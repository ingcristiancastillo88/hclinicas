package controller;

import tools.jackson.databind.ObjectMapper;
import ec.salud.citas.hclinicas.controller.HistoriaClinicaController;
import ec.salud.citas.hclinicas.dto.request.ActualizarConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearHistoriaClinicaRequest;
import ec.salud.citas.hclinicas.dto.response.ConsultaResponse;
import ec.salud.citas.hclinicas.dto.response.ConsultaResumenResponse;
import ec.salud.citas.hclinicas.dto.response.HistoriaClinicaResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.service.HistoriaClinicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias para HistoriaClinicaController usando MockMvc en modo
 * standalone. Se simula HistoriaClinicaService con Mockito para validar
 * exclusivamente la capa de presentación: mapeo de la petición HTTP,
 * código de respuesta y estructura del ApiResponse (campo "data").
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HistoriaClinicaController - Pruebas unitarias de los endpoints /historias")
class HistoriaClinicaControllerTest {

    @Mock
    private HistoriaClinicaService service;

    @InjectMocks
    private HistoriaClinicaController historiaClinicaController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(historiaClinicaController).build();
    }

    // ── Historia Clínica ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Historia clínica")
    class HistoriaClinicaEndpoints {

        @Test
        @DisplayName("POST /historias con datos válidos retorna 200 y la historia guardada")
        void crearOActualizarRetorna200() throws Exception {
            CrearHistoriaClinicaRequest request = new CrearHistoriaClinicaRequest();
            request.setPacienteId(1L);
            request.setGestas(2);
            request.setPartos(1);

            HistoriaClinicaResponse response = HistoriaClinicaResponse.builder()
                    .id(5L)
                    .pacienteId(1L)
                    .pacienteNombreCompleto("María Pérez")
                    .gestas(2)
                    .partos(1)
                    .totalConsultas(0L)
                    .build();

            when(service.crearOActualizar(any(CrearHistoriaClinicaRequest.class), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/historias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Historia clínica guardada exitosamente"))
                    .andExpect(jsonPath("$.data.id").value(5))
                    .andExpect(jsonPath("$.data.pacienteId").value(1));
        }

        @Test
        @DisplayName("Con pacienteId nulo retorna 400 por validación @Valid")
        void crearOActualizarConPacienteIdNuloRetorna400() throws Exception {
            CrearHistoriaClinicaRequest request = new CrearHistoriaClinicaRequest();
            request.setPacienteId(null);

            mockMvc.perform(post("/historias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /historias/{id} retorna 200 con la historia solicitada")
        void obtenerPorIdRetorna200() throws Exception {
            HistoriaClinicaResponse response = HistoriaClinicaResponse.builder()
                    .id(5L)
                    .pacienteId(1L)
                    .build();

            when(service.obtenerPorId(5L)).thenReturn(response);

            mockMvc.perform(get("/historias/{id}", 5L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Historia clínica obtenida"))
                    .andExpect(jsonPath("$.data.id").value(5));
        }

        @Test
        @DisplayName("GET /historias/paciente/{pacienteId} retorna 200 con la historia del paciente")
        void obtenerPorPacienteRetorna200() throws Exception {
            HistoriaClinicaResponse response = HistoriaClinicaResponse.builder()
                    .id(5L)
                    .pacienteId(1L)
                    .build();

            when(service.obtenerPorPaciente(1L)).thenReturn(response);

            mockMvc.perform(get("/historias/paciente/{pacienteId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pacienteId").value(1));
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Consultas")
    class ConsultasEndpoints {

        @Test
        @DisplayName("GET /historias/{historiaId}/consultas retorna 200 con página de consultas")
        void listarConsultasRetorna200() throws Exception {
            ConsultaResumenResponse resumen = ConsultaResumenResponse.builder()
                    .id(1L)
                    .fechaConsulta(LocalDate.of(2026, 7, 1))
                    .motivoConsulta("Control prenatal")
                    .build();

            PageResponse<ConsultaResumenResponse> page = PageResponse.<ConsultaResumenResponse>builder()
                    .contenido(List.of(resumen))
                    .paginaActual(0)
                    .totalPaginas(1)
                    .totalElementos(1L)
                    .tamanioPagina(10)
                    .primera(true)
                    .ultima(true)
                    .build();

            when(service.listarConsultas(eq(5L), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/historias/{historiaId}/consultas", 5L)
                            .param("pagina", "0")
                            .param("tamano", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contenido[0].id").value(1))
                    .andExpect(jsonPath("$.data.totalElementos").value(1));

            verify(service).listarConsultas(5L, 0, 10);
        }

        @Test
        @DisplayName("POST /historias/consultas con datos válidos retorna 201")
        void crearConsultaRetorna201() throws Exception {
            CrearConsultaRequest request = new CrearConsultaRequest();
            request.setHistoriaClinicaId(5L);
            request.setFechaConsulta(LocalDate.of(2026, 7, 10));
            request.setMotivoConsulta("Dolor abdominal");
            request.setDiagnosticoPrincipal("Gastritis");

            ConsultaResponse response = ConsultaResponse.builder()
                    .id(20L)
                    .historiaClinicaId(5L)
                    .motivoConsulta("Dolor abdominal")
                    .diagnosticoPrincipal("Gastritis")
                    .build();

            when(service.crearConsulta(any(CrearConsultaRequest.class), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/historias/consultas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mensaje").value("Consulta registrada exitosamente"))
                    .andExpect(jsonPath("$.data.id").value(20));
        }

        @Test
        @DisplayName("Con motivoConsulta vacío retorna 400 por validación @Valid")
        void crearConsultaConMotivoVacioRetorna400() throws Exception {
            CrearConsultaRequest request = new CrearConsultaRequest();
            request.setHistoriaClinicaId(5L);
            request.setFechaConsulta(LocalDate.of(2026, 7, 10));
            request.setMotivoConsulta("");
            request.setDiagnosticoPrincipal("Gastritis");

            mockMvc.perform(post("/historias/consultas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /historias/consultas/{id} retorna 200 con el detalle de la consulta")
        void obtenerConsultaRetorna200() throws Exception {
            ConsultaResponse response = ConsultaResponse.builder()
                    .id(20L)
                    .historiaClinicaId(5L)
                    .build();

            when(service.obtenerConsulta(20L)).thenReturn(response);

            mockMvc.perform(get("/historias/consultas/{id}", 20L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(20));
        }

        @Test
        @DisplayName("PUT /historias/consultas/{id} retorna 200 con la consulta actualizada")
        void actualizarConsultaRetorna200() throws Exception {
            ActualizarConsultaRequest request = new ActualizarConsultaRequest();
            request.setFechaConsulta(LocalDate.of(2026, 7, 12));
            request.setMotivoConsulta("Control de seguimiento");
            request.setDiagnosticoPrincipal("Gastritis crónica");

            ConsultaResponse response = ConsultaResponse.builder()
                    .id(20L)
                    .motivoConsulta("Control de seguimiento")
                    .build();

            when(service.actualizarConsulta(eq(20L), any(ActualizarConsultaRequest.class), anyString()))
                    .thenReturn(response);

            mockMvc.perform(put("/historias/consultas/{id}", 20L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.motivoConsulta").value("Control de seguimiento"));
        }

        @Test
        @DisplayName("DELETE /historias/consultas/{id} retorna 200 y delega en el servicio")
        void eliminarConsultaRetorna200() throws Exception {
            mockMvc.perform(delete("/historias/consultas/{id}", 20L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Consulta eliminada exitosamente"));

            verify(service).eliminarConsulta(eq(20L), anyString());
        }
    }

    // ── Archivos ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Archivos")
    class ArchivosEndpoints {

        @Test
        @DisplayName("POST /historias/consultas/{id}/archivos (multipart) retorna 201")
        void subirArchivoRetorna201() throws Exception {
            MockMultipartFile archivo = new MockMultipartFile(
                    "archivo", "receta.pdf", "application/pdf", "contenido".getBytes());

            mockMvc.perform(multipart("/historias/consultas/{id}/archivos", 20L)
                            .file(archivo)
                            .param("tipoArchivo", "RECETA")
                            .param("descripcion", "Receta médica"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mensaje").value("Archivo subido exitosamente"));

            verify(service).subirArchivo(eq(20L), any(), eq("RECETA"), eq("Receta médica"), anyString());
        }

        @Test
        @DisplayName("DELETE /historias/archivos/{archivoId} retorna 200 y delega en el servicio")
        void eliminarArchivoRetorna200() throws Exception {
            mockMvc.perform(delete("/historias/archivos/{archivoId}", 30L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Archivo eliminado exitosamente"));

            verify(service).eliminarArchivo(eq(30L), anyString());
        }
    }
}
