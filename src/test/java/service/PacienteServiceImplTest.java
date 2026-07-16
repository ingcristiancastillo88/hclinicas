package service;

import ec.salud.citas.hclinicas.dto.ActualizarPacienteRequest;
import ec.salud.citas.hclinicas.dto.CrearPacienteRequest;
import ec.salud.citas.hclinicas.dto.PacienteResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.PacienteRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.impl.PacienteServiceImpl;
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
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PacienteServiceImpl.
 * Cubre CU-003 (Gestión de pacientes): HU-007 (registro), HU-008 (edición)
 * y HU-009 (búsqueda paginada), incluyendo el flujo principal y los flujos
 * alternativos de cada operación.
 *
 * Se usa Mockito para aislar la lógica de negocio del repositorio y del
 * servicio de auditoría, validando únicamente el comportamiento propio
 * de PacienteServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PacienteServiceImpl - Pruebas unitarias de gestión de pacientes")
class PacienteServiceImplTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PacienteServiceImpl pacienteService;

    private static final String IP_ORIGEN = "192.168.1.10";
    private static final String MODULO = "PACIENTES";

    private CrearPacienteRequest crearRequestValido() {
        CrearPacienteRequest request = new CrearPacienteRequest();
        request.setCedula("1712345678");
        request.setHistoriaNumero("HC-001");
        request.setNombres("Juan Carlos");
        request.setApellidos("Pérez Mora");
        request.setFechaNacimiento(LocalDate.of(1990, 3, 10));
        request.setCorreo("juan.perez@correo.com");
        request.setCelular("0991234567");
        return request;
    }

    private Paciente pacienteExistente() {
        return Paciente.builder()
                .id(1L)
                .cedula("1712345678")
                .historiaNumero("HC-001")
                .nombres("Juan Carlos")
                .apellidos("Pérez Mora")
                .fechaNacimiento(LocalDate.of(1990, 3, 10))
                .correo("juan.perez@correo.com")
                .celular("0991234567")
                .estado(EstadoPaciente.ACTIVO)
                .build();
    }

    @Nested
    @DisplayName("Crear paciente - flujo principal")
    class CrearPacienteFlujoPrincipal {

        @Test
        @DisplayName("Con datos válidos crea el paciente, lo persiste con estado ACTIVO y retorna el response mapeado")
        void crearPacienteConDatosValidosRetornaPacienteResponse() {
            CrearPacienteRequest request = crearRequestValido();

            when(pacienteRepository.existsByCedula(request.getCedula())).thenReturn(false);
            when(pacienteRepository.existsByHistoriaNumero(request.getHistoriaNumero()))
                    .thenReturn(false);
            when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
                Paciente guardado = invocation.getArgument(0);
                guardado.setId(10L);
                return guardado;
            });

            PacienteResponse response = pacienteService.crear(request, IP_ORIGEN);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getCedula()).isEqualTo("1712345678");
            assertThat(response.getNombreCompleto()).isEqualTo("Juan Carlos Pérez Mora");
            assertThat(response.getEstado()).isEqualTo(EstadoPaciente.ACTIVO);
            assertThat(response.getEdad())
                    .isEqualTo(Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears());

            verify(pacienteRepository).save(any(Paciente.class));
        }

        @Test
        @DisplayName("Registra la auditoría de creación con la acción CREATE y los datos del paciente")
        void crearPacienteRegistraAuditoriaConAccionCreate() {
            CrearPacienteRequest request = crearRequestValido();

            when(pacienteRepository.existsByCedula(request.getCedula())).thenReturn(false);
            when(pacienteRepository.existsByHistoriaNumero(request.getHistoriaNumero()))
                    .thenReturn(false);
            when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
                Paciente guardado = invocation.getArgument(0);
                guardado.setId(10L);
                return guardado;
            });

            pacienteService.crear(request, IP_ORIGEN);

            ArgumentCaptor<String> accionCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> moduloCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> descripcionCaptor = ArgumentCaptor.forClass(String.class);

            verify(auditoriaService).registrar(
                    accionCaptor.capture(), moduloCaptor.capture(),
                    descripcionCaptor.capture(), eq(IP_ORIGEN));

            assertThat(accionCaptor.getValue()).isEqualTo("CREATE");
            assertThat(moduloCaptor.getValue()).isEqualTo(MODULO);
            assertThat(descripcionCaptor.getValue())
                    .contains("Juan Carlos Pérez Mora")
                    .contains("1712345678");
        }
    }

    @Nested
    @DisplayName("Crear paciente - flujos alternativos")
    class CrearPacienteFlujosAlternativos {

        @Test
        @DisplayName("Si la cédula ya existe lanza ReglaNegocioException y no persiste el paciente")
        void crearPacienteConCedulaExistenteLanzaReglaNegocioException() {
            CrearPacienteRequest request = crearRequestValido();
            when(pacienteRepository.existsByCedula(request.getCedula())).thenReturn(true);

            assertThatThrownBy(() -> pacienteService.crear(request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(request.getCedula());

            verify(pacienteRepository, never()).save(any());
            verifyNoInteractions(auditoriaService);
        }

        @Test
        @DisplayName("Si el número de historia ya existe lanza ReglaNegocioException y no persiste el paciente")
        void crearPacienteConHistoriaNumeroExistenteLanzaReglaNegocioException() {
            CrearPacienteRequest request = crearRequestValido();
            when(pacienteRepository.existsByCedula(request.getCedula())).thenReturn(false);
            when(pacienteRepository.existsByHistoriaNumero(request.getHistoriaNumero()))
                    .thenReturn(true);

            assertThatThrownBy(() -> pacienteService.crear(request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(request.getHistoriaNumero());

            verify(pacienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si no se proporciona número de historia, no valida su unicidad")
        void crearPacienteSinHistoriaNumeroNoValidaUnicidad() {
            CrearPacienteRequest request = crearRequestValido();
            request.setHistoriaNumero(null);

            when(pacienteRepository.existsByCedula(request.getCedula())).thenReturn(false);
            when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
                Paciente guardado = invocation.getArgument(0);
                guardado.setId(11L);
                return guardado;
            });

            pacienteService.crear(request, IP_ORIGEN);

            verify(pacienteRepository, never()).existsByHistoriaNumero(any());
        }
    }

    @Nested
    @DisplayName("Actualizar paciente")
    class ActualizarPaciente {

        @Test
        @DisplayName("Actualiza los campos del paciente y no modifica la cédula original")
        void actualizarPacienteExistenteNoModificaCedula() {
            Paciente existente = pacienteExistente();
            ActualizarPacienteRequest request = new ActualizarPacienteRequest();
            request.setNombres("Juan Carlos");
            request.setApellidos("Pérez Mora Actualizado");
            request.setHistoriaNumero("HC-001");
            request.setCorreo("nuevo.correo@correo.com");

            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepository.save(any(Paciente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PacienteResponse response = pacienteService.actualizar(1L, request, IP_ORIGEN);

            assertThat(response.getCedula()).isEqualTo("1712345678");
            assertThat(response.getApellidos()).isEqualTo("Pérez Mora Actualizado");
            assertThat(response.getCorreo()).isEqualTo("nuevo.correo@correo.com");

            ArgumentCaptor<String> accionCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditoriaService).registrar(
                    accionCaptor.capture(), eq(MODULO), anyString(), eq(IP_ORIGEN));
            assertThat(accionCaptor.getValue()).isEqualTo("UPDATE");
        }

        @Test
        @DisplayName("Si el número de historia no cambia, no valida su unicidad")
        void actualizarConHistoriaNumeroIgualAlActualNoValidaUnicidad() {
            Paciente existente = pacienteExistente();
            ActualizarPacienteRequest request = new ActualizarPacienteRequest();
            request.setNombres("Juan Carlos");
            request.setApellidos("Pérez Mora");
            request.setHistoriaNumero("HC-001");

            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepository.save(any(Paciente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            pacienteService.actualizar(1L, request, IP_ORIGEN);

            verify(pacienteRepository, never()).existsByHistoriaNumero(any());
        }

        @Test
        @DisplayName("Si el número de historia cambia y ya existe en otro paciente lanza ReglaNegocioException")
        void actualizarConHistoriaNumeroDiferenteYExistenteLanzaReglaNegocioException() {
            Paciente existente = pacienteExistente();
            ActualizarPacienteRequest request = new ActualizarPacienteRequest();
            request.setNombres("Juan Carlos");
            request.setApellidos("Pérez Mora");
            request.setHistoriaNumero("HC-002");

            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepository.existsByHistoriaNumero("HC-002")).thenReturn(true);

            assertThatThrownBy(() -> pacienteService.actualizar(1L, request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("HC-002");

            verify(pacienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si el paciente no existe lanza RecursoNoEncontradoException y no persiste cambios")
        void actualizarConIdInexistenteLanzaRecursoNoEncontradoException() {
            ActualizarPacienteRequest request = new ActualizarPacienteRequest();
            request.setNombres("Juan Carlos");
            request.setApellidos("Pérez Mora");

            when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.actualizar(99L, request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(pacienteRepository, never()).save(any());
            verifyNoInteractions(auditoriaService);
        }
    }

    @Nested
    @DisplayName("Obtener paciente")
    class ObtenerPaciente {

        @Test
        @DisplayName("obtenerPorId retorna el response del paciente cuando existe")
        void obtenerPorIdExistenteRetornaResponse() {
            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(pacienteExistente()));

            PacienteResponse response = pacienteService.obtenerPorId(1L);

            assertThat(response.getCedula()).isEqualTo("1712345678");
        }

        @Test
        @DisplayName("obtenerPorId lanza RecursoNoEncontradoException con el id en el mensaje")
        void obtenerPorIdInexistenteLanzaExcepcion() {
            when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.obtenerPorId(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("obtenerPorCedula retorna el response del paciente cuando existe")
        void obtenerPorCedulaExistenteRetornaResponse() {
            when(pacienteRepository.findByCedula("1712345678"))
                    .thenReturn(Optional.of(pacienteExistente()));

            PacienteResponse response = pacienteService.obtenerPorCedula("1712345678");

            assertThat(response.getNombreCompleto()).isEqualTo("Juan Carlos Pérez Mora");
        }

        @Test
        @DisplayName("obtenerPorCedula lanza RecursoNoEncontradoException con la cédula en el mensaje")
        void obtenerPorCedulaInexistenteLanzaExcepcion() {
            when(pacienteRepository.findByCedula("0000000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.obtenerPorCedula("0000000000"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("0000000000");
        }
    }

    @Nested
    @DisplayName("Listar pacientes")
    class ListarPacientes {

        @Test
        @DisplayName("Con soloActivos=true consulta buscarPacientes filtrando por estado ACTIVO")
        void listarConSoloActivosTrueLlamaBuscarPacientes() {
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("apellidos").ascending());
            Page<Paciente> pagina = new PageImpl<>(List.of(pacienteExistente()), pageRequest, 1);

            when(pacienteRepository.buscarPacientes(eq(""), eq(EstadoPaciente.ACTIVO), eq(pageRequest)))
                    .thenReturn(pagina);

            PageResponse<?> response = pacienteService.listar("", 0, 10, true);

            assertThat(response.getContenido()).hasSize(1);
            verify(pacienteRepository).buscarPacientes(eq(""), eq(EstadoPaciente.ACTIVO), eq(pageRequest));
            verify(pacienteRepository, never()).buscarTodos(anyString(), any());
        }

        @Test
        @DisplayName("Con soloActivos=false consulta buscarTodos sin filtrar por estado")
        void listarConSoloActivosFalseLlamaBuscarTodos() {
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("apellidos").ascending());
            Page<Paciente> pagina = new PageImpl<>(List.of(pacienteExistente()), pageRequest, 1);

            when(pacienteRepository.buscarTodos(eq(""), eq(pageRequest))).thenReturn(pagina);

            PageResponse<?> response = pacienteService.listar("", 0, 10, false);

            assertThat(response.getContenido()).hasSize(1);
            verify(pacienteRepository).buscarTodos(eq(""), eq(pageRequest));
            verify(pacienteRepository, never())
                    .buscarPacientes(anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("Activar / Desactivar paciente")
    class ActivarDesactivarPaciente {

        @Test
        @DisplayName("desactivar cambia el estado a INACTIVO, persiste y registra auditoría DEACTIVATE")
        void desactivarPacienteExistenteCambiaEstadoYRegistraAuditoria() {
            Paciente existente = pacienteExistente();
            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepository.save(any(Paciente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            pacienteService.desactivar(1L, IP_ORIGEN);

            ArgumentCaptor<Paciente> pacienteCaptor = ArgumentCaptor.forClass(Paciente.class);
            verify(pacienteRepository).save(pacienteCaptor.capture());
            assertThat(pacienteCaptor.getValue().getEstado()).isEqualTo(EstadoPaciente.INACTIVO);

            ArgumentCaptor<String> accionCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditoriaService).registrar(
                    accionCaptor.capture(), eq(MODULO), anyString(), eq(IP_ORIGEN));
            assertThat(accionCaptor.getValue()).isEqualTo("DEACTIVATE");
        }

        @Test
        @DisplayName("activar cambia el estado a ACTIVO, persiste y registra auditoría ACTIVATE")
        void activarPacienteExistenteCambiaEstadoYRegistraAuditoria() {
            Paciente existente = pacienteExistente();
            existente.setEstado(EstadoPaciente.INACTIVO);
            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepository.save(any(Paciente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            pacienteService.activar(1L, IP_ORIGEN);

            ArgumentCaptor<Paciente> pacienteCaptor = ArgumentCaptor.forClass(Paciente.class);
            verify(pacienteRepository).save(pacienteCaptor.capture());
            assertThat(pacienteCaptor.getValue().getEstado()).isEqualTo(EstadoPaciente.ACTIVO);

            ArgumentCaptor<String> accionCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditoriaService).registrar(
                    accionCaptor.capture(), eq(MODULO), anyString(), eq(IP_ORIGEN));
            assertThat(accionCaptor.getValue()).isEqualTo("ACTIVATE");
        }

        @Test
        @DisplayName("desactivar con id inexistente lanza RecursoNoEncontradoException y no persiste cambios")
        void desactivarConIdInexistenteLanzaExcepcion() {
            when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.desactivar(99L, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(pacienteRepository, never()).save(any());
            verifyNoInteractions(auditoriaService);
        }
    }
}
