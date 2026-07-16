package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.salud.citas.hclinicas.controller.DocumentoController;
import ec.salud.citas.hclinicas.dto.request.PedidoLaboratorioRequest;
import ec.salud.citas.hclinicas.dto.request.RecetaRequest;
import ec.salud.citas.hclinicas.entity.PedidoLaboratorio;
import ec.salud.citas.hclinicas.entity.Receta;
import ec.salud.citas.hclinicas.service.PdfService;
import ec.salud.citas.hclinicas.service.impl.PdfConsultaServiceImpl;
import ec.salud.citas.hclinicas.service.impl.PedidoLaboratorioServiceImpl;
import ec.salud.citas.hclinicas.service.impl.RecetaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias para DocumentoController usando MockMvc en modo standalone.
 * Todas las dependencias (PdfService, RecetaServiceImpl, PedidoLaboratorioServiceImpl,
 * PdfConsultaServiceImpl) son clases concretas simuladas con Mockito para validar
 * exclusivamente la capa de presentación: mapeo de la petición HTTP, cabeceras de
 * descarga y estructura del ApiResponse.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoController - Pruebas unitarias de endpoints de documentos PDF")
class DocumentoControllerTest {

    @Mock
    private PdfService pdfService;

    @Mock
    private RecetaServiceImpl recetaService;

    @Mock
    private PedidoLaboratorioServiceImpl pedidoService;

    @Mock
    private PdfConsultaServiceImpl pdfConsultaService;

    @InjectMocks
    private DocumentoController documentoController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(documentoController).build();
    }

    // ── PDF Consulta / Historia ──────────────────────────────────────────

    @Test
    @DisplayName("GET /documentos/consulta/{id} retorna 200 con el PDF de la consulta")
    void descargarConsultaRetorna200ConPdf() throws Exception {
        byte[] pdf = "PDF-CONSULTA".getBytes(StandardCharsets.UTF_8);
        when(pdfConsultaService.generarPdf(1L)).thenReturn(pdf);

        MvcResult result = mockMvc.perform(get("/documentos/consulta/{consultaId}", 1L))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("GET /documentos/historia/{id} retorna 200 con el PDF de la historia clínica")
    void descargarHistoriaRetorna200ConPdf() throws Exception {
        byte[] pdf = "PDF-HISTORIA".getBytes(StandardCharsets.UTF_8);
        when(pdfService.generarPdfHistoria(7L)).thenReturn(pdf);

        MvcResult result = mockMvc.perform(get("/documentos/historia/{historiaId}", 7L))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    // ── RECETA ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /documentos/receta/{id}/guardar guarda la receta y retorna su id")
    void guardarRecetaRetorna200ConId() throws Exception {
        RecetaRequest req = new RecetaRequest();
        req.setPrescripcion("Reposo relativo por 3 días");

        Receta recetaGuardada = Receta.builder().id(99L).build();
        when(recetaService.guardarReceta(eq(1L), any(RecetaRequest.class)))
                .thenReturn(recetaGuardada);

        mockMvc.perform(post("/documentos/receta/{consultaId}/guardar", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Receta guardada correctamente"))
                .andExpect(jsonPath("$.data").value(99));
    }

    @Test
    @DisplayName("POST /documentos/receta/{id}/pdf retorna 200 con el PDF generado")
    void generarPdfRecetaRetorna200ConPdf() throws Exception {
        RecetaRequest req = new RecetaRequest();
        req.setPrescripcion("Reposo relativo");

        byte[] pdf = "PDF-RECETA".getBytes(StandardCharsets.UTF_8);
        when(recetaService.generarPdf(eq(1L), any(RecetaRequest.class))).thenReturn(pdf);

        MvcResult result = mockMvc.perform(post("/documentos/receta/{consultaId}/pdf", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("GET /documentos/receta/{id} sin receta previa retorna mensaje 'Sin receta registrada'")
    void obtenerRecetaSinRecetaRegistrada() throws Exception {
        when(recetaService.obtenerUltimaReceta(1L)).thenReturn(null);

        mockMvc.perform(get("/documentos/receta/{consultaId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sin receta registrada"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GET /documentos/receta/{id} con receta previa retorna la receta encontrada")
    void obtenerRecetaConRecetaEncontrada() throws Exception {
        RecetaRequest receta = new RecetaRequest();
        receta.setPrescripcion("Tomar reposo");
        receta.setMedicamentos(List.of());
        when(recetaService.obtenerUltimaReceta(1L)).thenReturn(receta);

        mockMvc.perform(get("/documentos/receta/{consultaId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Receta encontrada"))
                .andExpect(jsonPath("$.data.prescripcion").value("Tomar reposo"));
    }

    // ── PEDIDO LABORATORIO / IMAGENOLOGÍA ─────────────────────────────────

    @Test
    @DisplayName("POST /documentos/pedido/{id}/guardar guarda el pedido y retorna su id")
    void guardarPedidoRetorna200ConId() throws Exception {
        PedidoLaboratorioRequest req = new PedidoLaboratorioRequest();
        req.setTipo("LABORATORIO");

        PedidoLaboratorio pedidoGuardado = PedidoLaboratorio.builder().id(55L).build();
        when(pedidoService.guardarPedido(eq(1L), any(PedidoLaboratorioRequest.class)))
                .thenReturn(pedidoGuardado);

        mockMvc.perform(post("/documentos/pedido/{consultaId}/guardar", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Pedido guardado correctamente"))
                .andExpect(jsonPath("$.data").value(55));
    }

    @Test
    @DisplayName("POST /documentos/pedido/{id}/pdf con tipo IMAGENOLOGIA (case-insensitive) invoca generarPdfImagenologia")
    void generarPdfPedidoConTipoImagenologiaInvocaServicioCorrecto() throws Exception {
        PedidoLaboratorioRequest req = new PedidoLaboratorioRequest();
        req.setTipo("imagenologia");

        byte[] pdf = "PDF-IMAGENOLOGIA".getBytes(StandardCharsets.UTF_8);
        when(pedidoService.generarPdfImagenologia(eq(1L), any(PedidoLaboratorioRequest.class)))
                .thenReturn(pdf);

        MvcResult result = mockMvc.perform(post("/documentos/pedido/{consultaId}/pdf", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
        verify(pedidoService).generarPdfImagenologia(eq(1L), any(PedidoLaboratorioRequest.class));
        verify(pedidoService, never()).generarPdfLaboratorio(any(), any());
    }

    @Test
    @DisplayName("POST /documentos/pedido/{id}/pdf con tipo distinto de IMAGENOLOGIA invoca generarPdfLaboratorio")
    void generarPdfPedidoConTipoLaboratorioInvocaServicioCorrecto() throws Exception {
        PedidoLaboratorioRequest req = new PedidoLaboratorioRequest();
        req.setTipo("LABORATORIO");

        byte[] pdf = "PDF-LABORATORIO".getBytes(StandardCharsets.UTF_8);
        when(pedidoService.generarPdfLaboratorio(eq(1L), any(PedidoLaboratorioRequest.class)))
                .thenReturn(pdf);

        MvcResult result = mockMvc.perform(post("/documentos/pedido/{consultaId}/pdf", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
        verify(pedidoService).generarPdfLaboratorio(eq(1L), any(PedidoLaboratorioRequest.class));
        verify(pedidoService, never()).generarPdfImagenologia(any(), any());
    }

    @Test
    @DisplayName("GET /documentos/pedido/{id} sin pedido previo retorna mensaje 'Sin pedido registrado'")
    void obtenerPedidoSinPedidoRegistrado() throws Exception {
        when(pedidoService.obtenerUltimoPedido(1L, "LABORATORIO")).thenReturn(null);

        mockMvc.perform(get("/documentos/pedido/{consultaId}", 1L)
                        .param("tipo", "LABORATORIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sin pedido registrado"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GET /documentos/pedido/{id} con pedido previo retorna el pedido encontrado")
    void obtenerPedidoConPedidoEncontrado() throws Exception {
        PedidoLaboratorioRequest pedido = new PedidoLaboratorioRequest();
        pedido.setTipo("LABORATORIO");
        pedido.setResumenClinico("dolor abdominal");
        when(pedidoService.obtenerUltimoPedido(1L, "LABORATORIO")).thenReturn(pedido);

        mockMvc.perform(get("/documentos/pedido/{consultaId}", 1L)
                        .param("tipo", "LABORATORIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Pedido encontrado"))
                .andExpect(jsonPath("$.data.resumenClinico").value("dolor abdominal"));
    }
}
