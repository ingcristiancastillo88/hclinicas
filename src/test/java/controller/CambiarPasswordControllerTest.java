package controller;

import ec.salud.citas.hclinicas.controller.CambiarPasswordController;
import ec.salud.citas.hclinicas.dto.request.CambiarPasswordRequest;
import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.enumerado.RolNombre;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.service.impl.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para CambiarPasswordController.
 * Este controlador no delega en una capa de servicio independiente: toda la
 * lógica de negocio del cambio de contraseña temporal vive directamente en
 * el método del controlador, que además lee el usuario autenticado desde el
 * SecurityContextHolder. Por ello las pruebas invocan directamente el método
 * `cambiarPassword(request)` (llamada Java simple), simulando previamente la
 * Authentication en el contexto de seguridad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CambiarPasswordController - Pruebas unitarias de cambio de contraseña temporal")
class CambiarPasswordControllerTest {

    private static final String EMAIL = "dra.leon@klinixmed.org";
    private static final String HASH_ACTUAL = "hashActual";

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private EmailService emailService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CambiarPasswordController cambiarPasswordController;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn(EMAIL);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Rol rol = Rol.builder()
                .id(1L)
                .nombre(RolNombre.ROLE_MEDICO_ESPECIALISTA)
                .build();

        usuario = Usuario.builder()
                .id(1L)
                .nombres("Alexandra")
                .apellidos("León")
                .correo(EMAIL)
                .contrasena(HASH_ACTUAL)
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .passwordTemporal(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CambiarPasswordRequest buildRequest(String actual, String nueva, String confirmar) {
        CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual(actual);
        request.setPasswordNueva(nueva);
        request.setConfirmarPassword(confirmar);
        return request;
    }

    @Nested
    @DisplayName("Flujo principal - cambio exitoso")
    class CambioExitoso {

        @Test
        @DisplayName("Con contraseña actual correcta y nueva contraseña válida, "
                + "actualiza la contraseña, quita el flag temporal y notifica por correo")
        void cambiarPasswordExitosoActualizaYNotifica() {
            CambiarPasswordRequest request = buildRequest(
                    "TempPass123", "NuevaPass123", "NuevaPass123");

            when(usuarioRepo.findByCorreo(EMAIL)).thenReturn(Optional.of(usuario));
            when(encoder.matches(eq("TempPass123"), eq(HASH_ACTUAL))).thenReturn(true);
            when(encoder.matches(eq("NuevaPass123"), eq(HASH_ACTUAL))).thenReturn(false);
            when(encoder.encode("NuevaPass123")).thenReturn("hashNuevo");
            when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            ResponseEntity<ApiResponse<Void>> response =
                    cambiarPasswordController.cambiarPassword(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMensaje()).isEqualTo("Contraseña actualizada correctamente");

            assertThat(usuario.getContrasena()).isEqualTo("hashNuevo");
            assertThat(usuario.getPasswordTemporal()).isFalse();

            verify(usuarioRepo).save(usuario);
            verify(emailService).enviarConfirmacionCambioPassword(EMAIL, "Alexandra León");
        }
    }

    @Nested
    @DisplayName("Flujos alternativos - validaciones de negocio")
    class FlujosAlternativos {

        @Test
        @DisplayName("Con usuario no encontrado lanza ReglaNegocioException")
        void usuarioNoEncontradoLanzaExcepcion() {
            CambiarPasswordRequest request = buildRequest(
                    "TempPass123", "NuevaPass123", "NuevaPass123");

            when(usuarioRepo.findByCorreo(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cambiarPasswordController.cambiarPassword(request))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Usuario no encontrado");

            verify(usuarioRepo, never()).save(any());
        }

        @Test
        @DisplayName("Con contraseña actual incorrecta lanza ReglaNegocioException y no guarda")
        void passwordActualIncorrectaLanzaExcepcion() {
            CambiarPasswordRequest request = buildRequest(
                    "PasswordIncorrecta", "NuevaPass123", "NuevaPass123");

            when(usuarioRepo.findByCorreo(EMAIL)).thenReturn(Optional.of(usuario));
            when(encoder.matches(eq("PasswordIncorrecta"), eq(HASH_ACTUAL))).thenReturn(false);

            assertThatThrownBy(() -> cambiarPasswordController.cambiarPassword(request))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La contraseña actual no es correcta");

            verify(usuarioRepo, never()).save(any());
        }

        @Test
        @DisplayName("Con nueva contraseña distinta a la confirmación lanza ReglaNegocioException")
        void passwordNuevaDistintaDeConfirmacionLanzaExcepcion() {
            CambiarPasswordRequest request = buildRequest(
                    "TempPass123", "NuevaPass123", "OtraConfirmacion1");

            when(usuarioRepo.findByCorreo(EMAIL)).thenReturn(Optional.of(usuario));
            when(encoder.matches(eq("TempPass123"), eq(HASH_ACTUAL))).thenReturn(true);

            assertThatThrownBy(() -> cambiarPasswordController.cambiarPassword(request))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La nueva contraseña y la confirmación no coinciden");

            verify(usuarioRepo, never()).save(any());
        }

        @Test
        @DisplayName("Con nueva contraseña igual a la temporal/actual lanza ReglaNegocioException")
        void passwordNuevaIgualALaTemporalLanzaExcepcion() {
            // La nueva contraseña coincide con el hash actual (es la misma contraseña temporal)
            CambiarPasswordRequest request = buildRequest(
                    "TempPass123", "TempPass123", "TempPass123");

            when(usuarioRepo.findByCorreo(EMAIL)).thenReturn(Optional.of(usuario));
            when(encoder.matches(eq("TempPass123"), eq(HASH_ACTUAL))).thenReturn(true);

            assertThatThrownBy(() -> cambiarPasswordController.cambiarPassword(request))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La nueva contraseña no puede ser igual a la contraseña temporal");

            verify(usuarioRepo, never()).save(any());
        }
    }
}
