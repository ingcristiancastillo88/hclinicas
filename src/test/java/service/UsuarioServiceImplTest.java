package service;

import ec.salud.citas.hclinicas.dto.ActualizarUsuarioRequest;
import ec.salud.citas.hclinicas.dto.CrearUsuarioRequest;
import ec.salud.citas.hclinicas.dto.UsuarioResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.enumerado.RolNombre;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.RolRepository;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.impl.EmailService;
import ec.salud.citas.hclinicas.service.impl.UsuarioServiceImpl;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para UsuarioServiceImpl.
 * Cubre HU-004 (creación), HU-005 (listado/edición) y HU-006 (auditoría),
 * aislando la lógica de negocio de los repositorios mediante Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl - Pruebas unitarias de gestión de usuarios")
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private static final String IP_ORIGEN = "192.168.1.10";

    private Rol rolMedico;
    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        rolMedico = Rol.builder()
                .id(1L)
                .nombre(RolNombre.ROLE_MEDICO_ESPECIALISTA)
                .descripcion("Médico especialista")
                .build();

        usuarioExistente = Usuario.builder()
                .id(1L)
                .nombres("Alexandra")
                .apellidos("León")
                .cedula("1710034065")
                .correo("dra.leon@klinixmed.org")
                .contrasena("hashActual")
                .telefono("0999999999")
                .rol(rolMedico)
                .estado(EstadoUsuario.ACTIVO)
                .passwordTemporal(false)
                .build();
    }

    private CrearUsuarioRequest buildCrearRequest(String cedula) {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombres("Alexandra");
        request.setApellidos("León");
        request.setCedula(cedula);
        request.setCorreo("dra.leon@klinixmed.org");
        request.setContrasena("RequestPasswordIgnorada123");
        request.setTelefono("0999999999");
        request.setRolId(1L);
        return request;
    }

    private ActualizarUsuarioRequest buildActualizarRequest(String contrasena) {
        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest();
        request.setNombres("Alexandra");
        request.setApellidos("León");
        request.setCedula("1710034065");
        request.setCorreo("dra.leon@klinixmed.org");
        request.setContrasena(contrasena);
        request.setTelefono("0999999999");
        request.setRolId(1L);
        return request;
    }

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("Con correo ya existente lanza ReglaNegocioException y no guarda")
        void crearConCorreoExistenteLanzaExcepcion() {
            CrearUsuarioRequest request = buildCrearRequest(null);
            when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crear(request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Con cédula proporcionada y ya existente lanza ReglaNegocioException")
        void crearConCedulaExistenteLanzaExcepcion() {
            CrearUsuarioRequest request = buildCrearRequest("1710034065");
            when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
            when(usuarioRepository.existsByCedula("1710034065")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crear(request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Con cédula en blanco/null omite por completo la validación de unicidad de cédula")
        void crearConCedulaEnBlancoOmiteValidacion() {
            CrearUsuarioRequest request = buildCrearRequest(null);
            when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
            when(rolRepository.findById(1L)).thenReturn(Optional.of(rolMedico));
            when(passwordEncoder.encode(anyString())).thenReturn("hashedTemp");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.crear(request, IP_ORIGEN);

            verify(usuarioRepository, never()).existsByCedula(anyString());
        }

        @Test
        @DisplayName("Con rol inexistente lanza RecursoNoEncontradoException")
        void crearConRolInexistenteLanzaExcepcion() {
            CrearUsuarioRequest request = buildCrearRequest(null);
            when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
            when(rolRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.crear(request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Con datos válidos crea el usuario ACTIVO con passwordTemporal=false, "
                + "ignora la contraseña del request y registra auditoría CREATE con el rol")
        void crearConDatosValidosCreaUsuarioYRegistraAuditoria() {
            CrearUsuarioRequest request = buildCrearRequest(null);
            when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
            when(rolRepository.findById(1L)).thenReturn(Optional.of(rolMedico));
            when(passwordEncoder.encode(anyString())).thenReturn("hashedTemp");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(10L);
                return u;
            });

            UsuarioResponse response = usuarioService.crear(request, IP_ORIGEN);

            assertThat(response).isNotNull();
            assertThat(response.getCorreo()).isEqualTo(request.getCorreo());
            assertThat(response.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);

            // La entidad guardada debe quedar ACTIVO y sin flag de contraseña temporal
            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(usuarioCaptor.capture());
            Usuario guardado = usuarioCaptor.getValue();
            assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
            assertThat(guardado.getPasswordTemporal()).isFalse();
            assertThat(guardado.getContrasena()).isEqualTo("hashedTemp");

            // La contraseña que se codifica es la generada aleatoriamente, no la del request
            ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
            verify(passwordEncoder).encode(passwordCaptor.capture());
            assertThat(passwordCaptor.getValue())
                    .isNotEqualTo(request.getContrasena())
                    .matches("[A-Za-z0-9]{10}");

            // Auditoría con la acción CREATE y el nombre del rol en la descripción
            ArgumentCaptor<String> descripcionCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditoriaService).registrar(
                    eq("CREATE"), eq("USUARIOS"), descripcionCaptor.capture(), eq(IP_ORIGEN));
            assertThat(descripcionCaptor.getValue()).contains("ROLE_MEDICO_ESPECIALISTA");

            // El bloque de envío de correo de bienvenida está comentado: no debe invocarse
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("Con id inexistente lanza RecursoNoEncontradoException")
        void actualizarConIdInexistenteLanzaExcepcion() {
            ActualizarUsuarioRequest request = buildActualizarRequest(null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.actualizar(1L, request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("Con correo en colisión (excluyendo el propio id) lanza ReglaNegocioException")
        void actualizarConCorreoEnColisionLanzaExcepcion() {
            ActualizarUsuarioRequest request = buildActualizarRequest(null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.existsByCorreoAndIdNot(request.getCorreo(), 1L)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.actualizar(1L, request, IP_ORIGEN))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Con rol inexistente lanza RecursoNoEncontradoException")
        void actualizarConRolInexistenteLanzaExcepcion() {
            ActualizarUsuarioRequest request = buildActualizarRequest(null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.existsByCorreoAndIdNot(request.getCorreo(), 1L)).thenReturn(false);
            when(rolRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.actualizar(1L, request, IP_ORIGEN))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Con contraseña en blanco/null no la re-codifica ni la modifica")
        void actualizarConContrasenaEnBlancoNoLaModifica() {
            ActualizarUsuarioRequest request = buildActualizarRequest(null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.existsByCorreoAndIdNot(request.getCorreo(), 1L)).thenReturn(false);
            when(rolRepository.findById(1L)).thenReturn(Optional.of(rolMedico));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.actualizar(1L, request, IP_ORIGEN);

            verify(passwordEncoder, never()).encode(anyString());

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getContrasena()).isEqualTo("hashActual");
        }

        @Test
        @DisplayName("Con contraseña no vacía la codifica y actualiza la entidad")
        void actualizarConContrasenaNoVaciaLaCodificaYActualiza() {
            ActualizarUsuarioRequest request = buildActualizarRequest("NuevaPassword123");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.existsByCorreoAndIdNot(request.getCorreo(), 1L)).thenReturn(false);
            when(rolRepository.findById(1L)).thenReturn(Optional.of(rolMedico));
            when(passwordEncoder.encode("NuevaPassword123")).thenReturn("hashNuevo");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.actualizar(1L, request, IP_ORIGEN);

            verify(passwordEncoder).encode("NuevaPassword123");

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getContrasena()).isEqualTo("hashNuevo");
        }

        @Test
        @DisplayName("Registra auditoría UPDATE al actualizar exitosamente")
        void actualizarRegistraAuditoria() {
            ActualizarUsuarioRequest request = buildActualizarRequest(null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.existsByCorreoAndIdNot(request.getCorreo(), 1L)).thenReturn(false);
            when(rolRepository.findById(1L)).thenReturn(Optional.of(rolMedico));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.actualizar(1L, request, IP_ORIGEN);

            verify(auditoriaService).registrar(
                    eq("UPDATE"), eq("USUARIOS"), anyString(), eq(IP_ORIGEN));
        }
    }

    @Nested
    @DisplayName("obtenerPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("Con usuario en estado ELIMINADO lo trata como no encontrado")
        void obtenerPorIdConUsuarioEliminadoLanzaExcepcion() {
            Usuario eliminado = Usuario.builder()
                    .id(1L)
                    .nombres("Alexandra")
                    .apellidos("León")
                    .correo("dra.leon@klinixmed.org")
                    .contrasena("hashActual")
                    .rol(rolMedico)
                    .estado(EstadoUsuario.ELIMINADO)
                    .passwordTemporal(false)
                    .build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(eliminado));

            assertThatThrownBy(() -> usuarioService.obtenerPorId(1L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("Con usuario ACTIVO retorna el UsuarioResponse correspondiente")
        void obtenerPorIdConUsuarioActivoRetornaResponse() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));

            UsuarioResponse response = usuarioService.obtenerPorId(1L);

            assertThat(response.getCorreo()).isEqualTo("dra.leon@klinixmed.org");
        }
    }

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("Invoca buscarUsuarios excluyendo el estado ELIMINADO del resultado")
        void listarExcluyeEstadoEliminado() {
            Page<Usuario> pagina = new PageImpl<>(List.of(usuarioExistente));
            when(usuarioRepository.buscarUsuarios(eq("leon"), eq(EstadoUsuario.ELIMINADO), any(Pageable.class)))
                    .thenReturn(pagina);

            PageResponse<UsuarioResponse> response = usuarioService.listar("leon", 0, 10);

            assertThat(response.getContenido()).hasSize(1);
            verify(usuarioRepository).buscarUsuarios(eq("leon"), eq(EstadoUsuario.ELIMINADO), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("desactivar() / activar()")
    class CambiarEstado {

        @Test
        @DisplayName("desactivar() establece estado INACTIVO y audita DEACTIVATE")
        void desactivarEstableceInactivoYAudita() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.desactivar(1L, IP_ORIGEN);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsuario.INACTIVO);

            verify(auditoriaService).registrar(
                    eq("DEACTIVATE"), eq("USUARIOS"), anyString(), eq(IP_ORIGEN));
        }

        @Test
        @DisplayName("activar() establece estado ACTIVO y audita ACTIVATE")
        void activarEstableceActivoYAudita() {
            usuarioExistente.setEstado(EstadoUsuario.INACTIVO);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.activar(1L, IP_ORIGEN);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);

            verify(auditoriaService).registrar(
                    eq("ACTIVATE"), eq("USUARIOS"), anyString(), eq(IP_ORIGEN));
        }
    }

    @Nested
    @DisplayName("generarPasswordTemporal()")
    class GenerarPasswordTemporal {

        @Test
        @DisplayName("Genera una contraseña de 10 caracteres alfanuméricos")
        void generaPasswordDeDiezCaracteresAlfanumericos() {
            String password = UsuarioServiceImpl.generarPasswordTemporal();

            assertThat(password).hasSize(10);
            assertThat(password).matches("[A-Za-z0-9]{10}");
        }

        @Test
        @DisplayName("Dos invocaciones sucesivas producen valores distintos con probabilidad abrumadora")
        void dosInvocacionesProducenValoresDistintos() {
            String password1 = UsuarioServiceImpl.generarPasswordTemporal();
            String password2 = UsuarioServiceImpl.generarPasswordTemporal();

            assertThat(password1).matches("[A-Za-z0-9]{10}");
            assertThat(password2).matches("[A-Za-z0-9]{10}");
        }
    }
}
