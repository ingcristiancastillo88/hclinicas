package controller;

import tools.jackson.databind.ObjectMapper;
import ec.salud.citas.hclinicas.controller.PacienteController;
import ec.salud.citas.hclinicas.dto.ActualizarPacienteRequest;
import ec.salud.citas.hclinicas.dto.CrearPacienteRequest;
import ec.salud.citas.hclinicas.dto.PacienteResponse;
import ec.salud.citas.hclinicas.dto.PacienteResumenResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import ec.salud.citas.hclinicas.service.PacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias para PacienteController usando MockMvc en modo standalone.
 * No levanta el contexto completo de Spring (a diferencia de las pruebas de
 * implementación con @SpringBootTest), por lo que PacienteService se simula
 * con Mockito para validar exclusivamente la capa de presentación: mapeo de
 * la petición HTTP, código de respuesta y estructura del ApiResponse.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PacienteController - Pruebas unitarias de gestión de pacientes (HU-007/HU-008/HU-009)")
class PacienteControllerTest {

    @Mock
    private PacienteService pacienteService;

    @InjectMocks
    private PacienteController pacienteController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pacienteController).build();
    }

    private CrearPacienteRequest crearRequestValido() {
        CrearPacienteRequest request = new CrearPacienteRequest();
        request.setCedula("1712345678");
        request.setNombres("Juan Carlos");
        request.setApellidos("Pérez Mora");
        return request;
    }

    private PacienteResponse pacienteResponseSimulado() {
        return PacienteResponse.builder()
                .id(1L)
                .cedula("1712345678")
                .nombres("Juan Carlos")
                .apellidos("Pérez Mora")
                .nombreCompleto("Juan Carlos Pérez Mora")
                .edad(35)
                .estado(EstadoPaciente.ACTIVO)
                .build();
    }

    // ── GET /pacientes ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /pacientes retorna 200 con los parámetros de búsqueda por defecto")
    void listarPacientesRetorna200ConParametrosPorDefecto() throws Exception {
        PacienteResumenResponse resumen = PacienteResumenResponse.builder()
                .id(1L)
                .cedula("1712345678")
                .nombreCompleto("Juan Carlos Pérez Mora")
                .estado(EstadoPaciente.ACTIVO)
                .build();

        PageResponse<PacienteResumenResponse> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(resumen), PageRequest.of(0, 10), 1));

        when(pacienteService.listar(eq(""), eq(0), eq(10), eq(true)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Pacientes obtenidos exitosamente"))
                .andExpect(jsonPath("$.data.contenido[0].nombreCompleto")
                        .value("Juan Carlos Pérez Mora"));

        verify(pacienteService).listar(eq(""), eq(0), eq(10), eq(true));
    }

    @Test
    @DisplayName("GET /pacientes propaga los parámetros de búsqueda, paginación y filtro de estado recibidos")
    void listarPacientesPropagaParametrosRecibidos() throws Exception {
        PageResponse<PacienteResumenResponse> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        when(pacienteService.listar(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/pacientes")
                        .param("busqueda", "Pérez")
                        .param("pagina", "2")
                        .param("tamano", "5")
                        .param("soloActivos", "false"))
                .andExpect(status().isOk());

        verify(pacienteService).listar(eq("Pérez"), eq(2), eq(5), eq(false));
    }

    // ── GET /pacientes/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /pacientes/{id} retorna 200 con el detalle del paciente")
    void obtenerPacientePorIdRetorna200() throws Exception {
        when(pacienteService.obtenerPorId(1L)).thenReturn(pacienteResponseSimulado());

        mockMvc.perform(get("/pacientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Paciente obtenido exitosamente"))
                .andExpect(jsonPath("$.data.cedula").value("1712345678"));
    }

    // ── GET /pacientes/cedula/{cedula} ────────────────────────────────────────

    @Test
    @DisplayName("GET /pacientes/cedula/{cedula} retorna 200 con el paciente encontrado")
    void obtenerPacientePorCedulaRetorna200() throws Exception {
        when(pacienteService.obtenerPorCedula("1712345678"))
                .thenReturn(pacienteResponseSimulado());

        mockMvc.perform(get("/pacientes/cedula/1712345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Paciente encontrado"))
                .andExpect(jsonPath("$.data.nombreCompleto").value("Juan Carlos Pérez Mora"));
    }

    // ── POST /pacientes ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /pacientes con datos válidos retorna 201 y el paciente creado")
    void crearPacienteConDatosValidosRetorna201() throws Exception {
        when(pacienteService.crear(any(CrearPacienteRequest.class), anyString()))
                .thenReturn(pacienteResponseSimulado());

        mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Paciente creado exitosamente"))
                .andExpect(jsonPath("$.data.cedula").value("1712345678"));
    }

    @Test
    @DisplayName("POST /pacientes toma la IP del header X-Forwarded-For cuando está presente")
    void crearPacienteUsaIpDeHeaderXForwardedForCuandoExiste() throws Exception {
        when(pacienteService.crear(any(CrearPacienteRequest.class), eq("200.10.10.5")))
                .thenReturn(pacienteResponseSimulado());

        mockMvc.perform(post("/pacientes")
                        .header("X-Forwarded-For", "200.10.10.5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequestValido())))
                .andExpect(status().isCreated());

        verify(pacienteService).crear(any(CrearPacienteRequest.class), eq("200.10.10.5"));
    }

    @Test
    @DisplayName("POST /pacientes con cédula y nombres vacíos retorna 400 por validación @Valid")
    void crearPacienteConCedulaYNombresVaciosRetorna400() throws Exception {
        CrearPacienteRequest request = new CrearPacienteRequest();
        request.setCedula("");
        request.setNombres("");
        request.setApellidos("");

        mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /pacientes/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /pacientes/{id} con datos válidos retorna 200 y el paciente actualizado")
    void actualizarPacienteConDatosValidosRetorna200() throws Exception {
        ActualizarPacienteRequest request = new ActualizarPacienteRequest();
        request.setNombres("Juan Carlos");
        request.setApellidos("Pérez Mora");

        when(pacienteService.actualizar(eq(1L), any(ActualizarPacienteRequest.class), anyString()))
                .thenReturn(pacienteResponseSimulado());

        mockMvc.perform(put("/pacientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Paciente actualizado exitosamente"))
                .andExpect(jsonPath("$.data.nombreCompleto").value("Juan Carlos Pérez Mora"));
    }

    @Test
    @DisplayName("PUT /pacientes/{id} con nombres y apellidos vacíos retorna 400 por validación @Valid")
    void actualizarPacienteConNombresVaciosRetorna400() throws Exception {
        ActualizarPacienteRequest request = new ActualizarPacienteRequest();
        request.setNombres("");
        request.setApellidos("");

        mockMvc.perform(put("/pacientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /pacientes/{id}/desactivar ──────────────────────────────────────

    @Test
    @DisplayName("PATCH /pacientes/{id}/desactivar retorna 200 y desactiva al paciente")
    void desactivarPacienteRetorna200() throws Exception {
        mockMvc.perform(patch("/pacientes/1/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Paciente desactivado exitosamente"));

        verify(pacienteService).desactivar(eq(1L), anyString());
    }

    // ── PATCH /pacientes/{id}/activar ─────────────────────────────────────────

    @Test
    @DisplayName("PATCH /pacientes/{id}/activar retorna 200 y activa al paciente")
    void activarPacienteRetorna200() throws Exception {
        mockMvc.perform(patch("/pacientes/1/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Paciente activado exitosamente"));

        verify(pacienteService).activar(eq(1L), anyString());
    }
}
