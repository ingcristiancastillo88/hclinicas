package service;

import ec.salud.citas.hclinicas.dto.request.ActualizarConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearHistoriaClinicaRequest;
import ec.salud.citas.hclinicas.dto.response.ConsultaResponse;
import ec.salud.citas.hclinicas.dto.response.ConsultaResumenResponse;
import ec.salud.citas.hclinicas.dto.response.HistoriaClinicaResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.entity.ArchivoAdjunto;
import ec.salud.citas.hclinicas.entity.Consulta;
import ec.salud.citas.hclinicas.entity.HistoriaClinica;
import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.enumerado.TipoArchivo;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.ArchivoAdjuntoRepository;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.repository.HistoriaClinicaRepository;
import ec.salud.citas.hclinicas.repository.PacienteRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.impl.HistoriaClinicaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para HistoriaClinicaServiceImpl.
 * Cubre historia clínica, consultas y archivos adjuntos (HU-010, HU-011, HU-012, HU-020).
 * Se usa Mockito para aislar la lógica de negocio de los repositorios y del
 * servicio de auditoría; los campos rutaBase/urlBase (@Value) se inyectan
 * manualmente hacia un directorio temporal para no afectar uploads/ real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HistoriaClinicaServiceImpl - Pruebas unitarias")
class HistoriaClinicaServiceImplTest {

    @Mock
    private HistoriaClinicaRepository historiaRepo;

    @Mock
    private ConsultaRepository consultaRepo;

    @Mock
    private ArchivoAdjuntoRepository archivoRepo;

    @Mock
    private PacienteRepository pacienteRepo;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private HistoriaClinicaServiceImpl service;

    @TempDir
    Path tempDir;

    private static final String IP_ORIGEN = "192.168.1.10";

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "rutaBase", tempDir.toString());
        ReflectionTestUtils.setField(service, "urlBase", "http://localhost:8080/api/archivos");

        paciente = Paciente.builder()
                .id(1L)
                .cedula("0102030405")
                .nombres("María")
                .apellidos("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Nested
    @DisplayName("Historia clínica")
    class HistoriaClinicaTests {

        @Test
        @DisplayName("crearOActualizar: paciente no encontrado lanza RecursoNoEncontradoException")
        void crearOActualizarPacienteNoEncontrado() {
            CrearHistoriaClinicaRequest req = new CrearHistoriaClinicaRequest();
            req.setPacienteId(99L);

            when(pacienteRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crearOActualizar(req, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(historiaRepo, never()).save(any());
        }

        @Test
        @DisplayName("crearOActualizar: sin historia previa crea una nueva vinculada al paciente")
        void crearOActualizarSinHistoriaPreviaCreaNueva() {
            CrearHistoriaClinicaRequest req = new CrearHistoriaClinicaRequest();
            req.setPacienteId(1L);
            req.setGestas(3);
            req.setPartos(2);
            req.setMenarquia("12 años");

            when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
            when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());
            when(historiaRepo.save(any(HistoriaClinica.class))).thenAnswer(inv -> {
                HistoriaClinica h = inv.getArgument(0);
                h.setId(5L);
                return h;
            });
            when(consultaRepo.countByHistoriaClinicaIdAndActivaTrue(5L)).thenReturn(0L);

            HistoriaClinicaResponse response = service.crearOActualizar(req, IP_ORIGEN);

            assertThat(response.getId()).isEqualTo(5L);
            assertThat(response.getPacienteId()).isEqualTo(1L);
            assertThat(response.getGestas()).isEqualTo(3);
            assertThat(response.getPartos()).isEqualTo(2);
            assertThat(response.getMenarquia()).isEqualTo("12 años");

            ArgumentCaptor<HistoriaClinica> captor = ArgumentCaptor.forClass(HistoriaClinica.class);
            verify(historiaRepo).save(captor.capture());
            assertThat(captor.getValue().getPaciente()).isEqualTo(paciente);

            verify(auditoriaService).registrar(eq("CREATE_UPDATE"), eq("HISTORIAS_CLINICAS"),
                    anyString(), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("crearOActualizar: con historia previa actualiza los campos conservando el ID")
        void crearOActualizarConHistoriaPreviaActualizaConservandoId() {
            HistoriaClinica existente = HistoriaClinica.builder()
                    .id(10L)
                    .paciente(paciente)
                    .gestas(1)
                    .partos(1)
                    .build();

            CrearHistoriaClinicaRequest req = new CrearHistoriaClinicaRequest();
            req.setPacienteId(1L);
            req.setGestas(4);
            req.setPartos(3);

            when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
            when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(existente));
            when(historiaRepo.save(any(HistoriaClinica.class))).thenAnswer(inv -> inv.getArgument(0));
            when(consultaRepo.countByHistoriaClinicaIdAndActivaTrue(10L)).thenReturn(2L);

            HistoriaClinicaResponse response = service.crearOActualizar(req, IP_ORIGEN);

            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getGestas()).isEqualTo(4);
            assertThat(response.getPartos()).isEqualTo(3);
            assertThat(response.getTotalConsultas()).isEqualTo(2L);
        }

        @Test
        @DisplayName("obtenerPorId: historia no encontrada lanza RecursoNoEncontradoException")
        void obtenerPorIdNoEncontrada() {
            when(historiaRepo.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerPorId(1L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("obtenerPorId: retorna la historia mapeada con los datos del paciente")
        void obtenerPorIdRetornaHistoria() {
            HistoriaClinica historia = HistoriaClinica.builder()
                    .id(7L)
                    .paciente(paciente)
                    .gestas(2)
                    .build();

            when(historiaRepo.findById(7L)).thenReturn(Optional.of(historia));
            when(consultaRepo.countByHistoriaClinicaIdAndActivaTrue(7L)).thenReturn(3L);

            HistoriaClinicaResponse response = service.obtenerPorId(7L);

            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.getPacienteCedula()).isEqualTo("0102030405");
            assertThat(response.getPacienteNombreCompleto()).isEqualTo("María Pérez");
            assertThat(response.getTotalConsultas()).isEqualTo(3L);
        }

        @Test
        @DisplayName("obtenerPorPaciente: sin historia para el paciente lanza RecursoNoEncontradoException")
        void obtenerPorPacienteNoEncontrada() {
            when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerPorPaciente(1L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("obtenerPorPaciente: retorna la historia del paciente")
        void obtenerPorPacienteRetornaHistoria() {
            HistoriaClinica historia = HistoriaClinica.builder()
                    .id(7L)
                    .paciente(paciente)
                    .build();

            when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));
            when(consultaRepo.countByHistoriaClinicaIdAndActivaTrue(7L)).thenReturn(0L);

            HistoriaClinicaResponse response = service.obtenerPorPaciente(1L);

            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.getPacienteId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class ConsultasTests {

        private HistoriaClinica historia;

        @BeforeEach
        void setUpHistoria() {
            historia = HistoriaClinica.builder()
                    .id(5L)
                    .paciente(paciente)
                    .build();
        }

        private CrearConsultaRequest crearConsultaRequestValido() {
            CrearConsultaRequest req = new CrearConsultaRequest();
            req.setHistoriaClinicaId(5L);
            req.setFechaConsulta(LocalDate.of(2026, 7, 10));
            req.setMotivoConsulta("Dolor abdominal");
            req.setDiagnosticoPrincipal("Gastritis");
            req.setPeso(60.0);
            req.setTalla(160.0);
            return req;
        }

        @Test
        @DisplayName("crearConsulta: historia no encontrada lanza RecursoNoEncontradoException")
        void crearConsultaHistoriaNoEncontrada() {
            CrearConsultaRequest req = crearConsultaRequestValido();
            when(historiaRepo.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crearConsulta(req, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(consultaRepo, never()).save(any());
        }

        @Test
        @DisplayName("crearConsulta: une los códigos CIE-10 secundarios con comas")
        void crearConsultaConCodigosSecundariosUneConComas() {
            CrearConsultaRequest req = crearConsultaRequestValido();
            req.setCodigosCie10Secundarios(List.of("N94.0", "Z34"));

            when(historiaRepo.findById(5L)).thenReturn(Optional.of(historia));
            when(consultaRepo.save(any(Consulta.class))).thenAnswer(inv -> {
                Consulta c = inv.getArgument(0);
                c.setId(20L);
                return c;
            });

            ConsultaResponse response = service.crearConsulta(req, IP_ORIGEN);

            ArgumentCaptor<Consulta> captor = ArgumentCaptor.forClass(Consulta.class);
            verify(consultaRepo).save(captor.capture());
            assertThat(captor.getValue().getCodigosCie10SecundariosJson()).isEqualTo("N94.0,Z34");

            assertThat(response.getId()).isEqualTo(20L);
            assertThat(response.getImc()).isNotNull();

            verify(auditoriaService).registrar(eq("CREATE"), eq("CONSULTAS"), anyString(), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("crearConsulta: con lista de códigos secundarios nula genera JSON nulo")
        void crearConsultaConCodigosSecundariosNuloGeneraJsonNulo() {
            CrearConsultaRequest req = crearConsultaRequestValido();
            req.setCodigosCie10Secundarios(null);

            when(historiaRepo.findById(5L)).thenReturn(Optional.of(historia));
            when(consultaRepo.save(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));

            service.crearConsulta(req, IP_ORIGEN);

            ArgumentCaptor<Consulta> captor = ArgumentCaptor.forClass(Consulta.class);
            verify(consultaRepo).save(captor.capture());
            assertThat(captor.getValue().getCodigosCie10SecundariosJson()).isNull();
        }

        @Test
        @DisplayName("actualizarConsulta: consulta no encontrada lanza RecursoNoEncontradoException")
        void actualizarConsultaNoEncontrada() {
            ActualizarConsultaRequest req = new ActualizarConsultaRequest();
            when(consultaRepo.findById(20L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizarConsulta(20L, req, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("actualizarConsulta: consulta inactiva lanza RecursoNoEncontradoException")
        void actualizarConsultaInactivaLanzaExcepcion() {
            Consulta inactiva = Consulta.builder()
                    .id(20L)
                    .historiaClinica(historia)
                    .activa(false)
                    .build();

            ActualizarConsultaRequest req = new ActualizarConsultaRequest();
            when(consultaRepo.findById(20L)).thenReturn(Optional.of(inactiva));

            assertThatThrownBy(() -> service.actualizarConsulta(20L, req, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(consultaRepo, never()).save(any());
        }

        @Test
        @DisplayName("actualizarConsulta: actualiza los campos y conserva el mismo ID")
        void actualizarConsultaActualizaCamposConservandoId() {
            Consulta existente = Consulta.builder()
                    .id(20L)
                    .historiaClinica(historia)
                    .activa(true)
                    .motivoConsulta("Motivo anterior")
                    .build();

            ActualizarConsultaRequest req = new ActualizarConsultaRequest();
            req.setFechaConsulta(LocalDate.of(2026, 7, 12));
            req.setMotivoConsulta("Control de seguimiento");
            req.setDiagnosticoPrincipal("Gastritis crónica");
            req.setCodigosCie10Secundarios(List.of("K29.7"));

            when(consultaRepo.findById(20L)).thenReturn(Optional.of(existente));
            when(consultaRepo.save(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));

            ConsultaResponse response = service.actualizarConsulta(20L, req, IP_ORIGEN);

            assertThat(response.getId()).isEqualTo(20L);
            assertThat(response.getMotivoConsulta()).isEqualTo("Control de seguimiento");
            assertThat(response.getDiagnosticoPrincipal()).isEqualTo("Gastritis crónica");
            assertThat(existente.getCodigosCie10SecundariosJson()).isEqualTo("K29.7");

            verify(auditoriaService).registrar(eq("UPDATE"), eq("CONSULTAS"), anyString(), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("obtenerConsulta: consulta no encontrada lanza RecursoNoEncontradoException")
        void obtenerConsultaNoEncontrada() {
            when(consultaRepo.findByIdConArchivos(20L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerConsulta(20L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("obtenerConsulta: retorna el detalle de la consulta con sus archivos")
        void obtenerConsultaRetornaDetalle() {
            Consulta consulta = Consulta.builder()
                    .id(20L)
                    .historiaClinica(historia)
                    .motivoConsulta("Dolor abdominal")
                    .activa(true)
                    .build();

            when(consultaRepo.findByIdConArchivos(20L)).thenReturn(Optional.of(consulta));

            ConsultaResponse response = service.obtenerConsulta(20L);

            assertThat(response.getId()).isEqualTo(20L);
            assertThat(response.getHistoriaClinicaId()).isEqualTo(5L);
            assertThat(response.getTotalArchivos()).isZero();
        }

        @Test
        @DisplayName("listarConsultas: invoca al repositorio con el PageRequest correcto")
        void listarConsultasInvocaConPageRequestCorrecto() {
            Consulta consulta = Consulta.builder()
                    .id(20L)
                    .historiaClinica(historia)
                    .fechaConsulta(LocalDate.of(2026, 7, 1))
                    .motivoConsulta("Control")
                    .build();

            Page<Consulta> pageMock = new PageImpl<>(List.of(consulta));

            when(consultaRepo.findActivasByHistoriaId(eq(5L), any(Pageable.class)))
                    .thenReturn(pageMock);

            PageResponse<ConsultaResumenResponse> response = service.listarConsultas(5L, 1, 15);

            assertThat(response.getContenido()).hasSize(1);
            assertThat(response.getContenido().get(0).getId()).isEqualTo(20L);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(consultaRepo).findActivasByHistoriaId(eq(5L), pageableCaptor.capture());

            Pageable usado = pageableCaptor.getValue();
            assertThat(usado.getPageNumber()).isEqualTo(1);
            assertThat(usado.getPageSize()).isEqualTo(15);
            assertThat(usado.getSort().getOrderFor("fechaConsulta")).isNotNull();
            assertThat(usado.getSort().getOrderFor("fechaConsulta").isDescending()).isTrue();
        }

        @Test
        @DisplayName("eliminarConsulta: marca la consulta como inactiva (borrado lógico)")
        void eliminarConsultaMarcaInactiva() {
            Consulta consulta = Consulta.builder()
                    .id(20L)
                    .historiaClinica(historia)
                    .activa(true)
                    .build();

            when(consultaRepo.findById(20L)).thenReturn(Optional.of(consulta));
            when(consultaRepo.save(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));

            service.eliminarConsulta(20L, IP_ORIGEN);

            assertThat(consulta.getActiva()).isFalse();
            verify(consultaRepo).save(consulta);
            verify(consultaRepo, never()).delete(any());
            verify(consultaRepo, never()).deleteById(any());
            verify(auditoriaService).registrar(eq("DELETE"), eq("CONSULTAS"), anyString(), eq(IP_ORIGEN));
        }
    }

    @Nested
    @DisplayName("Archivos")
    class ArchivosTests {

        private HistoriaClinica historia;
        private Consulta consultaActiva;

        @BeforeEach
        void setUpConsulta() {
            historia = HistoriaClinica.builder().id(5L).paciente(paciente).build();
            consultaActiva = Consulta.builder()
                    .id(7L)
                    .historiaClinica(historia)
                    .activa(true)
                    .build();
        }

        @Test
        @DisplayName("subirArchivo: consulta no encontrada lanza RecursoNoEncontradoException")
        void subirArchivoConsultaNoEncontrada() {
            MockMultipartFile archivo = new MockMultipartFile(
                    "archivo", "receta.pdf", "application/pdf", "contenido".getBytes());

            when(consultaRepo.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.subirArchivo(7L, archivo, "RECETA", "desc", IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(archivoRepo, never()).save(any());
        }

        @Test
        @DisplayName("subirArchivo: consulta inactiva lanza RecursoNoEncontradoException")
        void subirArchivoConsultaInactiva() {
            Consulta inactiva = Consulta.builder().id(7L).historiaClinica(historia).activa(false).build();
            MockMultipartFile archivo = new MockMultipartFile(
                    "archivo", "receta.pdf", "application/pdf", "contenido".getBytes());

            when(consultaRepo.findById(7L)).thenReturn(Optional.of(inactiva));

            assertThatThrownBy(() -> service.subirArchivo(7L, archivo, "RECETA", "desc", IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("subirArchivo: extensión no permitida lanza ReglaNegocioException")
        void subirArchivoExtensionNoPermitida() {
            MockMultipartFile archivo = new MockMultipartFile(
                    "archivo", "malware.exe", "application/octet-stream", "contenido".getBytes());

            when(consultaRepo.findById(7L)).thenReturn(Optional.of(consultaActiva));

            assertThatThrownBy(() -> service.subirArchivo(7L, archivo, "RECETA", "desc", IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Extensión no permitida")
                    .hasMessageContaining("exe");

            verify(archivoRepo, never()).save(any());
        }

        @Test
        @DisplayName("subirArchivo: archivo mayor a 10 MB lanza ReglaNegocioException")
        void subirArchivoSuperaTamanoMaximo() {
            MultipartFile archivoGrande = mock(MultipartFile.class);
            when(archivoGrande.getOriginalFilename()).thenReturn("receta.pdf");
            when(archivoGrande.getSize()).thenReturn(11L * 1024 * 1024);

            when(consultaRepo.findById(7L)).thenReturn(Optional.of(consultaActiva));

            assertThatThrownBy(() -> service.subirArchivo(7L, archivoGrande, "RECETA", "desc", IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("10 MB");

            verify(archivoRepo, never()).save(any());
        }

        @Test
        @DisplayName("subirArchivo: con extensión permitida guarda el archivo en disco y registra auditoría")
        void subirArchivoExitosoGuardaEnDiscoYRegistra() throws Exception {
            MockMultipartFile archivo = new MockMultipartFile(
                    "archivo", "receta.pdf", "application/pdf", "contenido".getBytes());

            when(consultaRepo.findById(7L)).thenReturn(Optional.of(consultaActiva));

            service.subirArchivo(7L, archivo, "RECETA", "Receta médica", IP_ORIGEN);

            ArgumentCaptor<ArchivoAdjunto> captor = ArgumentCaptor.forClass(ArchivoAdjunto.class);
            verify(archivoRepo).save(captor.capture());
            ArchivoAdjunto guardado = captor.getValue();

            assertThat(guardado.getConsulta()).isEqualTo(consultaActiva);
            assertThat(guardado.getNombreOriginal()).isEqualTo("receta.pdf");
            assertThat(guardado.getTipoArchivo()).isEqualTo(TipoArchivo.RECETA);
            assertThat(guardado.getDescripcion()).isEqualTo("Receta médica");
            assertThat(guardado.getTamanoBytes()).isEqualTo((long) "contenido".getBytes().length);

            Path archivoGuardado = tempDir.resolve(guardado.getRutaArchivo());
            assertThat(Files.exists(archivoGuardado)).isTrue();

            verify(auditoriaService).registrar(eq("CREATE"), eq("ARCHIVOS"), anyString(), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("eliminarArchivo: archivo no encontrado lanza RecursoNoEncontradoException")
        void eliminarArchivoNoEncontrado() {
            when(archivoRepo.findById(30L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminarArchivo(30L, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(archivoRepo, never()).delete(any());
        }

        @Test
        @DisplayName("eliminarArchivo: elimina la entidad y el archivo físico del disco")
        void eliminarArchivoEliminaEntidadYArchivoFisico() throws Exception {
            Path subdir = tempDir.resolve("consulta_7");
            Files.createDirectories(subdir);
            Path archivoFisico = subdir.resolve("archivo.pdf");
            Files.writeString(archivoFisico, "contenido");

            ArchivoAdjunto archivo = ArchivoAdjunto.builder()
                    .id(30L)
                    .consulta(consultaActiva)
                    .nombreOriginal("receta.pdf")
                    .rutaArchivo("consulta_7/archivo.pdf")
                    .build();

            when(archivoRepo.findById(30L)).thenReturn(Optional.of(archivo));

            service.eliminarArchivo(30L, IP_ORIGEN);

            verify(archivoRepo).delete(archivo);
            assertThat(Files.exists(archivoFisico)).isFalse();
            verify(auditoriaService).registrar(eq("DELETE"), eq("ARCHIVOS"), anyString(), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("eliminarArchivo: no propaga error si el archivo físico ya no existe en disco")
        void eliminarArchivoNoPropagaErrorSiArchivoFisicoNoExiste() {
            ArchivoAdjunto archivo = ArchivoAdjunto.builder()
                    .id(31L)
                    .consulta(consultaActiva)
                    .nombreOriginal("receta.pdf")
                    .rutaArchivo("consulta_7/no_existe.pdf")
                    .build();

            when(archivoRepo.findById(31L)).thenReturn(Optional.of(archivo));

            service.eliminarArchivo(31L, IP_ORIGEN);

            verify(archivoRepo).delete(archivo);
        }
    }
}
