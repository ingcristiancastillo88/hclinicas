package service;

import ec.salud.citas.hclinicas.dto.request.ActualizarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CancelarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearCitaRequest;
import ec.salud.citas.hclinicas.dto.response.CitaMedicaResponse;
import ec.salud.citas.hclinicas.dto.response.CitaMedicaResumenResponse;
import ec.salud.citas.hclinicas.dto.response.DisponibilidadResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.entity.CitaMedica;
import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.enumerado.TipoCita;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.CitaMedicaRepository;
import ec.salud.citas.hclinicas.repository.PacienteRepository;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.impl.CitaMedicaServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CitaMedicaServiceImpl.
 * Cubre HU-016 (registro de citas), HU-017 (validación de disponibilidad),
 * HU-018 (edición/cancelación) y HU-019/HU-020 (calendario e historial del
 * paciente). Se aísla la lógica de negocio del repositorio, del contexto de
 * seguridad y del servicio de auditoría mediante Mockito (AWS TDD, S. Kumar, 2023).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CitaMedicaServiceImpl - Pruebas unitarias del módulo de citas médicas")
class CitaMedicaServiceImplTest {

    @Mock
    private CitaMedicaRepository citaRepo;

    @Mock
    private PacienteRepository pacienteRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private CitaMedicaServiceImpl citaService;

    private static final String IP_ORIGEN = "192.168.1.20";
    private static final String CORREO_MEDICO = "dra.leon@klinixmed.org";

    private Paciente paciente;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        paciente = Paciente.builder()
                .id(10L)
                .cedula("0102030405")
                .nombres("Ana")
                .apellidos("Pérez")
                .celular("0991234567")
                .build();

        usuario = Usuario.builder()
                .id(2L)
                .nombres("Alexandra")
                .apellidos("León")
                .correo(CORREO_MEDICO)
                .build();

        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(CORREO_MEDICO);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CitaMedica citaBuilder(Long id, EstadoCita estado) {
        return CitaMedica.builder()
                .id(id)
                .paciente(paciente)
                .usuario(usuario)
                .fechaCita(LocalDate.of(2026, 7, 20))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(8, 30))
                .duracionMinutos(30)
                .tipoCita(TipoCita.CONTROL)
                .motivoCita("Control prenatal")
                .estado(estado)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Crear cita
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Crear cita (HU-016)")
    class CrearCita {

        @Test
        @DisplayName("Con datos válidos y sin conflicto de horario, agenda la cita con duración por defecto")
        void crearExitosoConDuracionPorDefectoAgendaCitaProgramada() {
            CrearCitaRequest request = new CrearCitaRequest();
            request.setPacienteId(10L);
            request.setFechaCita(LocalDate.of(2026, 7, 20));
            request.setHoraInicio(LocalTime.of(8, 0));
            request.setDuracionMinutos(null);
            request.setTipoCita(TipoCita.CONTROL);
            request.setMotivoCita("Control prenatal");

            when(pacienteRepo.findById(10L)).thenReturn(Optional.of(paciente));
            when(usuarioRepo.findByCorreo(CORREO_MEDICO)).thenReturn(Optional.of(usuario));
            when(citaRepo.existeConflictoHorario(eq(LocalDate.of(2026, 7, 20)),
                    eq(LocalTime.of(8, 0)), eq(LocalTime.of(8, 30)), anyList()))
                    .thenReturn(false);
            when(citaRepo.save(any(CitaMedica.class))).thenAnswer(inv -> {
                CitaMedica c = inv.getArgument(0);
                c.setId(100L);
                return c;
            });

            CitaMedicaResponse response = citaService.crear(request, IP_ORIGEN);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getHoraFin()).isEqualTo(LocalTime.of(8, 30));
            assertThat(response.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
            assertThat(response.getPacienteNombreCompleto()).isEqualTo("Ana Pérez");
            assertThat(response.getUsuarioNombreCompleto()).isEqualTo("Alexandra León");

            ArgumentCaptor<CitaMedica> captor = ArgumentCaptor.forClass(CitaMedica.class);
            verify(citaRepo).save(captor.capture());
            assertThat(captor.getValue().getDuracionMinutos()).isEqualTo(30);
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        }

        @Test
        @DisplayName("Con duración explícita calcula la hora fin acorde y audita la creación")
        void crearExitosoConDuracionExplicitaCalculaHoraFinYAudita() {
            CrearCitaRequest request = new CrearCitaRequest();
            request.setPacienteId(10L);
            request.setFechaCita(LocalDate.of(2026, 7, 20));
            request.setHoraInicio(LocalTime.of(9, 0));
            request.setDuracionMinutos(45);

            when(pacienteRepo.findById(10L)).thenReturn(Optional.of(paciente));
            when(usuarioRepo.findByCorreo(CORREO_MEDICO)).thenReturn(Optional.of(usuario));
            when(citaRepo.existeConflictoHorario(any(), any(), any(), anyList())).thenReturn(false);
            when(citaRepo.save(any(CitaMedica.class))).thenAnswer(inv -> inv.getArgument(0));

            CitaMedicaResponse response = citaService.crear(request, IP_ORIGEN);

            assertThat(response.getHoraFin()).isEqualTo(LocalTime.of(9, 45));

            ArgumentCaptor<String> descripcionCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditoriaService).registrar(eq("CREATE"), eq("CITAS"),
                    descripcionCaptor.capture(), eq(IP_ORIGEN));
            assertThat(descripcionCaptor.getValue()).contains("Ana Pérez");
        }

        @Test
        @DisplayName("Con paciente no encontrado lanza RecursoNoEncontradoException y no guarda la cita")
        void crearPacienteNoEncontradoLanzaRecursoNoEncontradoException() {
            CrearCitaRequest request = new CrearCitaRequest();
            request.setPacienteId(999L);
            request.setFechaCita(LocalDate.of(2026, 7, 20));
            request.setHoraInicio(LocalTime.of(8, 0));

            when(pacienteRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.crear(request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(citaRepo, never()).save(any());
            verifyNoInteractions(auditoriaService);
        }

        @Test
        @DisplayName("Con conflicto de horario lanza ReglaNegocioException y no guarda la cita")
        void crearConConflictoHorarioLanzaReglaNegocioException() {
            CrearCitaRequest request = new CrearCitaRequest();
            request.setPacienteId(10L);
            request.setFechaCita(LocalDate.of(2026, 7, 20));
            request.setHoraInicio(LocalTime.of(8, 0));
            request.setDuracionMinutos(30);

            when(pacienteRepo.findById(10L)).thenReturn(Optional.of(paciente));
            when(usuarioRepo.findByCorreo(CORREO_MEDICO)).thenReturn(Optional.of(usuario));
            when(citaRepo.existeConflictoHorario(any(), any(), any(), anyList())).thenReturn(true);

            assertThatThrownBy(() -> citaService.crear(request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("Sin usuario autenticado en el contexto de seguridad lanza ReglaNegocioException")
        void crearSinUsuarioAutenticadoLanzaReglaNegocioException() {
            CrearCitaRequest request = new CrearCitaRequest();
            request.setPacienteId(10L);
            request.setFechaCita(LocalDate.of(2026, 7, 20));
            request.setHoraInicio(LocalTime.of(8, 0));

            when(pacienteRepo.findById(10L)).thenReturn(Optional.of(paciente));
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> citaService.crear(request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("No hay usuario autenticado");

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("Con usuario autenticado inexistente en el repositorio lanza RecursoNoEncontradoException")
        void crearUsuarioAutenticadoNoEncontradoLanzaRecursoNoEncontradoException() {
            CrearCitaRequest request = new CrearCitaRequest();
            request.setPacienteId(10L);
            request.setFechaCita(LocalDate.of(2026, 7, 20));
            request.setHoraInicio(LocalTime.of(8, 0));

            when(pacienteRepo.findById(10L)).thenReturn(Optional.of(paciente));
            when(usuarioRepo.findByCorreo(CORREO_MEDICO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.crear(request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(citaRepo, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Actualizar cita
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Actualizar cita (HU-018)")
    class ActualizarCita {

        @Test
        @DisplayName("Con datos válidos recalcula horaFin y valida disponibilidad excluyendo la propia cita")
        void actualizarExitosoRecalculaHoraFinYExcluyeCitaPropia() {
            CitaMedica citaExistente = citaBuilder(50L, EstadoCita.PROGRAMADA);

            ActualizarCitaRequest request = new ActualizarCitaRequest();
            request.setFechaCita(LocalDate.of(2026, 7, 21));
            request.setHoraInicio(LocalTime.of(9, 0));
            request.setDuracionMinutos(45);
            request.setTipoCita(TipoCita.URGENCIA);
            request.setMotivoCita("Reprogramada");

            when(citaRepo.findById(50L)).thenReturn(Optional.of(citaExistente));
            when(citaRepo.existeConflictoHorarioExcluyendo(eq(LocalDate.of(2026, 7, 21)),
                    eq(LocalTime.of(9, 0)), eq(LocalTime.of(9, 45)), anyList(), eq(50L)))
                    .thenReturn(false);
            when(citaRepo.save(any(CitaMedica.class))).thenAnswer(inv -> inv.getArgument(0));

            CitaMedicaResponse response = citaService.actualizar(50L, request, IP_ORIGEN);

            assertThat(response.getHoraFin()).isEqualTo(LocalTime.of(9, 45));
            assertThat(response.getFechaCita()).isEqualTo(LocalDate.of(2026, 7, 21));

            verify(citaRepo).existeConflictoHorarioExcluyendo(any(), any(), any(), anyList(), eq(50L));
            verify(citaRepo, never()).existeConflictoHorario(any(), any(), any(), anyList());

            verify(auditoriaService).registrar(eq("UPDATE"), eq("CITAS"), contains("ID: 50"), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("Con tipoCita, motivoCita y notasAdicionales nulos conserva los valores originales")
        void actualizarConCamposOpcionalesNulosConservaValoresOriginales() {
            CitaMedica citaExistente = citaBuilder(51L, EstadoCita.PROGRAMADA);

            ActualizarCitaRequest request = new ActualizarCitaRequest();
            request.setFechaCita(LocalDate.of(2026, 7, 22));
            request.setHoraInicio(LocalTime.of(10, 0));
            request.setDuracionMinutos(30);
            request.setTipoCita(null);
            request.setMotivoCita(null);
            request.setNotasAdicionales(null);

            when(citaRepo.findById(51L)).thenReturn(Optional.of(citaExistente));
            when(citaRepo.existeConflictoHorarioExcluyendo(any(), any(), any(), anyList(), eq(51L)))
                    .thenReturn(false);
            when(citaRepo.save(any(CitaMedica.class))).thenAnswer(inv -> inv.getArgument(0));

            CitaMedicaResponse response = citaService.actualizar(51L, request, IP_ORIGEN);

            assertThat(response.getTipoCita()).isEqualTo(TipoCita.CONTROL);
            assertThat(response.getMotivoCita()).isEqualTo("Control prenatal");
        }

        @Test
        @DisplayName("Con cita cancelada lanza ReglaNegocioException y no guarda cambios")
        void actualizarCitaCanceladaLanzaReglaNegocioException() {
            CitaMedica citaCancelada = citaBuilder(52L, EstadoCita.CANCELADA);
            ActualizarCitaRequest request = new ActualizarCitaRequest();
            request.setFechaCita(LocalDate.of(2026, 7, 22));
            request.setHoraInicio(LocalTime.of(10, 0));

            when(citaRepo.findById(52L)).thenReturn(Optional.of(citaCancelada));

            assertThatThrownBy(() -> citaService.actualizar(52L, request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("Con cita atendida lanza ReglaNegocioException y no guarda cambios")
        void actualizarCitaAtendidaLanzaReglaNegocioException() {
            CitaMedica citaAtendida = citaBuilder(53L, EstadoCita.ATENDIDA);
            ActualizarCitaRequest request = new ActualizarCitaRequest();
            request.setFechaCita(LocalDate.of(2026, 7, 22));
            request.setHoraInicio(LocalTime.of(10, 0));

            when(citaRepo.findById(53L)).thenReturn(Optional.of(citaAtendida));

            assertThatThrownBy(() -> citaService.actualizar(53L, request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("Con id inexistente lanza RecursoNoEncontradoException")
        void actualizarIdNoEncontradoLanzaRecursoNoEncontradoException() {
            ActualizarCitaRequest request = new ActualizarCitaRequest();
            request.setFechaCita(LocalDate.of(2026, 7, 22));
            request.setHoraInicio(LocalTime.of(10, 0));

            when(citaRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.actualizar(999L, request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("Con conflicto de horario al actualizar lanza ReglaNegocioException")
        void actualizarConConflictoHorarioLanzaReglaNegocioException() {
            CitaMedica citaExistente = citaBuilder(54L, EstadoCita.PROGRAMADA);
            ActualizarCitaRequest request = new ActualizarCitaRequest();
            request.setFechaCita(LocalDate.of(2026, 7, 22));
            request.setHoraInicio(LocalTime.of(10, 0));
            request.setDuracionMinutos(30);

            when(citaRepo.findById(54L)).thenReturn(Optional.of(citaExistente));
            when(citaRepo.existeConflictoHorarioExcluyendo(any(), any(), any(), anyList(), eq(54L)))
                    .thenReturn(true);

            assertThatThrownBy(() -> citaService.actualizar(54L, request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Obtener y listar citas
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Obtener y listar citas")
    class ObtenerYListar {

        @Test
        @DisplayName("obtener() con id existente retorna la cita mapeada")
        void obtenerExistenteRetornaCitaMapeada() {
            CitaMedica cita = citaBuilder(60L, EstadoCita.PROGRAMADA);
            when(citaRepo.findById(60L)).thenReturn(Optional.of(cita));

            CitaMedicaResponse response = citaService.obtener(60L);

            assertThat(response.getId()).isEqualTo(60L);
            assertThat(response.getPacienteNombreCompleto()).isEqualTo("Ana Pérez");
            assertThat(response.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        }

        @Test
        @DisplayName("obtener() con id inexistente lanza RecursoNoEncontradoException")
        void obtenerNoExistenteLanzaRecursoNoEncontradoException() {
            when(citaRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.obtener(999L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("listar() construye el PageRequest ordenado y normaliza busqueda nula a vacío")
        void listarConBusquedaNulaNormalizaAVacioYOrdenaDescendente() {
            CitaMedica cita = citaBuilder(61L, EstadoCita.PROGRAMADA);
            Page<CitaMedica> pageResultado = new PageImpl<>(List.of(cita));

            when(citaRepo.listarConFiltros(eq(EstadoCita.PROGRAMADA), eq(LocalDate.of(2026, 7, 20)),
                    eq(""), any(PageRequest.class))).thenReturn(pageResultado);

            PageResponse<CitaMedicaResumenResponse> response = citaService.listar(
                    EstadoCita.PROGRAMADA, LocalDate.of(2026, 7, 20), null, 0, 10);

            assertThat(response.getContenido()).hasSize(1);
            assertThat(response.getContenido().get(0).getId()).isEqualTo(61L);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(citaRepo).listarConFiltros(eq(EstadoCita.PROGRAMADA), eq(LocalDate.of(2026, 7, 20)),
                    eq(""), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort().getOrderFor("fechaCita").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
            assertThat(pageable.getSort().getOrderFor("horaInicio").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("listar() con busqueda no nula la utiliza tal cual")
        void listarConBusquedaNoNulaUsaValorTalCual() {
            Page<CitaMedica> pageVacia = new PageImpl<>(List.of());
            when(citaRepo.listarConFiltros(isNull(), isNull(), eq("lopez"), any(PageRequest.class)))
                    .thenReturn(pageVacia);

            citaService.listar(null, null, "lopez", 0, 10);

            verify(citaRepo).listarConFiltros(isNull(), isNull(), eq("lopez"), any(PageRequest.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Cancelar / cambios de estado
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cancelar / cambios de estado (HU-018)")
    class CancelarYCambiosEstado {

        @Test
        @DisplayName("cancelar() con cita modificable actualiza estado, motivo, fecha de cancelación y audita")
        void cancelarExitosoActualizaEstadoMotivoYAudita() {
            CitaMedica cita = citaBuilder(70L, EstadoCita.PROGRAMADA);
            CancelarCitaRequest request = new CancelarCitaRequest();
            request.setMotivoCancelacion("Paciente reprogramó");

            when(citaRepo.findById(70L)).thenReturn(Optional.of(cita));
            when(citaRepo.save(any(CitaMedica.class))).thenAnswer(inv -> inv.getArgument(0));

            citaService.cancelar(70L, request, IP_ORIGEN);

            ArgumentCaptor<CitaMedica> captor = ArgumentCaptor.forClass(CitaMedica.class);
            verify(citaRepo).save(captor.capture());
            CitaMedica guardada = captor.getValue();
            assertThat(guardada.getEstado()).isEqualTo(EstadoCita.CANCELADA);
            assertThat(guardada.getMotivoCancelacion()).isEqualTo("Paciente reprogramó");
            assertThat(guardada.getFechaCancelacion()).isNotNull();

            verify(auditoriaService).registrar(eq("CANCEL"), eq("CITAS"),
                    contains("Paciente reprogramó"), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("cancelar() sobre una cita ya cancelada lanza ReglaNegocioException")
        void cancelarCitaYaCanceladaLanzaReglaNegocioException() {
            CitaMedica cita = citaBuilder(71L, EstadoCita.CANCELADA);
            CancelarCitaRequest request = new CancelarCitaRequest();
            request.setMotivoCancelacion("Motivo");

            when(citaRepo.findById(71L)).thenReturn(Optional.of(cita));

            assertThatThrownBy(() -> citaService.cancelar(71L, request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("cancelar() sobre una cita atendida lanza ReglaNegocioException")
        void cancelarCitaAtendidaLanzaReglaNegocioException() {
            CitaMedica cita = citaBuilder(72L, EstadoCita.ATENDIDA);
            CancelarCitaRequest request = new CancelarCitaRequest();
            request.setMotivoCancelacion("Motivo");

            when(citaRepo.findById(72L)).thenReturn(Optional.of(cita));

            assertThatThrownBy(() -> citaService.cancelar(72L, request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("cancelar() con id inexistente lanza RecursoNoEncontradoException")
        void cancelarIdNoEncontradoLanzaRecursoNoEncontradoException() {
            CancelarCitaRequest request = new CancelarCitaRequest();
            request.setMotivoCancelacion("Motivo");

            when(citaRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.cancelar(999L, request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("marcarAtendida() sobre una cita activa cambia el estado a ATENDIDA y audita")
        void marcarAtendidaExitosoCambiaEstadoYAudita() {
            CitaMedica cita = citaBuilder(73L, EstadoCita.PROGRAMADA);
            when(citaRepo.findById(73L)).thenReturn(Optional.of(cita));
            when(citaRepo.save(any(CitaMedica.class))).thenAnswer(inv -> inv.getArgument(0));

            citaService.marcarAtendida(73L, IP_ORIGEN);

            ArgumentCaptor<CitaMedica> captor = ArgumentCaptor.forClass(CitaMedica.class);
            verify(citaRepo).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCita.ATENDIDA);

            verify(auditoriaService).registrar(eq("UPDATE"), eq("CITAS"),
                    contains("ATENDIDA"), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("marcarAtendida() sobre una cita ya cancelada lanza ReglaNegocioException")
        void marcarAtendidaCitaCanceladaLanzaReglaNegocioException() {
            CitaMedica cita = citaBuilder(74L, EstadoCita.CANCELADA);
            when(citaRepo.findById(74L)).thenReturn(Optional.of(cita));

            assertThatThrownBy(() -> citaService.marcarAtendida(74L, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }

        @Test
        @DisplayName("marcarNoAsistio() sobre una cita activa cambia el estado a NO_ASISTIO y audita")
        void marcarNoAsistioExitosoCambiaEstadoYAudita() {
            CitaMedica cita = citaBuilder(75L, EstadoCita.PROGRAMADA);
            when(citaRepo.findById(75L)).thenReturn(Optional.of(cita));
            when(citaRepo.save(any(CitaMedica.class))).thenAnswer(inv -> inv.getArgument(0));

            citaService.marcarNoAsistio(75L, IP_ORIGEN);

            ArgumentCaptor<CitaMedica> captor = ArgumentCaptor.forClass(CitaMedica.class);
            verify(citaRepo).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCita.NO_ASISTIO);

            verify(auditoriaService).registrar(eq("UPDATE"), eq("CITAS"),
                    contains("NO_ASISTIO"), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("marcarNoAsistio() sobre una cita ya cancelada lanza ReglaNegocioException")
        void marcarNoAsistioCitaCanceladaLanzaReglaNegocioException() {
            CitaMedica cita = citaBuilder(76L, EstadoCita.CANCELADA);
            when(citaRepo.findById(76L)).thenReturn(Optional.of(cita));

            assertThatThrownBy(() -> citaService.marcarNoAsistio(76L, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(citaRepo, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Disponibilidad
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Verificar disponibilidad (HU-017)")
    class Disponibilidad {

        @Test
        @DisplayName("Con conflicto de horario retorna disponible=false con mensaje descriptivo")
        void disponibilidadConConflictoRetornaNoDisponibleConMensaje() {
            LocalDate fecha = LocalDate.of(2026, 7, 20);

            when(citaRepo.existeConflictoHorario(eq(fecha), eq(LocalTime.of(8, 0)),
                    eq(LocalTime.of(8, 30)), anyList())).thenReturn(true);
            when(citaRepo.findByFecha(fecha)).thenReturn(List.of());

            DisponibilidadResponse response = citaService.verificarDisponibilidad(fecha, "08:00", 30);

            assertThat(response.isDisponible()).isFalse();
            assertThat(response.getMensaje()).contains("08:00").contains(fecha.toString());
        }

        @Test
        @DisplayName("Sin conflicto de horario retorna disponible=true con mensaje estándar")
        void disponibilidadSinConflictoRetornaDisponibleConMensajeEstandar() {
            LocalDate fecha = LocalDate.of(2026, 7, 20);

            when(citaRepo.existeConflictoHorario(any(), any(), any(), anyList())).thenReturn(false);
            when(citaRepo.findByFecha(fecha)).thenReturn(List.of());

            DisponibilidadResponse response = citaService.verificarDisponibilidad(fecha, "08:00", 30);

            assertThat(response.isDisponible()).isTrue();
            assertThat(response.getMensaje()).isEqualTo("Horario disponible");
        }

        @Test
        @DisplayName("Los slots ocupados excluyen citas CANCELADA y NO_ASISTIO")
        void disponibilidadExcluyeCitasLiberadasDeSlotsOcupados() {
            LocalDate fecha = LocalDate.of(2026, 7, 20);

            CitaMedica activa = citaBuilder(80L, EstadoCita.PROGRAMADA);
            CitaMedica cancelada = citaBuilder(81L, EstadoCita.CANCELADA);
            CitaMedica noAsistio = citaBuilder(82L, EstadoCita.NO_ASISTIO);

            when(citaRepo.existeConflictoHorario(any(), any(), any(), anyList())).thenReturn(false);
            when(citaRepo.findByFecha(fecha)).thenReturn(List.of(activa, cancelada, noAsistio));

            DisponibilidadResponse response = citaService.verificarDisponibilidad(fecha, "08:00", 30);

            assertThat(response.getSlotsOcupados()).hasSize(1);
            assertThat(response.getSlotsOcupados().get(0).getEstado()).isEqualTo("PROGRAMADA");
            assertThat(response.getSlotsOcupados().get(0).getPacienteNombre()).isEqualTo("Ana Pérez");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Consultas de calendario y paciente
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Consultas de calendario y paciente (HU-019 / HU-020)")
    class ConsultasCalendarioYPaciente {

        @Test
        @DisplayName("obtenerPorRangoFechas() mapea las citas del repositorio a resúmenes")
        void obtenerPorRangoFechasMapeaCitasAResumen() {
            CitaMedica cita = citaBuilder(90L, EstadoCita.PROGRAMADA);
            LocalDate inicio = LocalDate.of(2026, 7, 1);
            LocalDate fin = LocalDate.of(2026, 7, 31);

            when(citaRepo.findByRangoFechas(inicio, fin)).thenReturn(List.of(cita));

            List<CitaMedicaResumenResponse> response = citaService.obtenerPorRangoFechas(inicio, fin);

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getPacienteNombreCompleto()).isEqualTo("Ana Pérez");
            assertThat(response.get(0).getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        }

        @Test
        @DisplayName("obtenerPorFecha() mapea las citas del día a resúmenes")
        void obtenerPorFechaMapeaCitasAResumen() {
            CitaMedica cita = citaBuilder(91L, EstadoCita.CONFIRMADA);
            LocalDate fecha = LocalDate.of(2026, 7, 20);

            when(citaRepo.findByFecha(fecha)).thenReturn(List.of(cita));

            List<CitaMedicaResumenResponse> response = citaService.obtenerPorFecha(fecha);

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getId()).isEqualTo(91L);
            assertThat(response.get(0).getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        @DisplayName("citasPorPaciente() retorna un PageResponse mapeado desde el repositorio")
        void citasPorPacienteRetornaPageResponseMapeada() {
            CitaMedica cita = citaBuilder(92L, EstadoCita.ATENDIDA);
            Page<CitaMedica> page = new PageImpl<>(List.of(cita));

            when(citaRepo.findByPacienteIdOrderByFechaCitaDescHoraInicioDesc(eq(10L), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<CitaMedicaResumenResponse> response = citaService.citasPorPaciente(10L, 0, 10);

            assertThat(response.getContenido()).hasSize(1);
            assertThat(response.getContenido().get(0).getId()).isEqualTo(92L);
            assertThat(response.getTotalElementos()).isEqualTo(1);
        }
    }
}
