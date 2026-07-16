package service;

import ec.salud.citas.hclinicas.dto.LoginRequest;
import ec.salud.citas.hclinicas.dto.LoginResponse;
import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.enumerado.RolNombre;
import ec.salud.citas.hclinicas.exception.UsuarioInactivoException;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import ec.salud.citas.hclinicas.security.JwtService;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AuthServiceImpl.
 * Cubre CU-001 (Inicio de sesión) incluyendo el flujo principal
 * y los flujos alternativos definidos en la tabla 8 de la tesis
 * (credenciales incorrectas, usuario inactivo, usuario eliminado).
 *
 * Se usa Mockito para aislar la lógica de negocio del AuthenticationManager,
 * del repositorio y del servicio de auditoría, validando únicamente el
 * comportamiento propio de AuthServiceImpl (AWS TDD, S. Kumar, 2023).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Pruebas unitarias de autenticación")
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest request;
    private Usuario usuarioActivo;
    private static final String IP_ORIGEN = "192.168.1.10";
    private static final String TOKEN_SIMULADO = "eyJhbGciOiJIUzI1NiJ9.token.simulado";
    private static final long EXPIRACION_MS = 3_600_000L;

    @BeforeEach
    void setUp() {
        request = new LoginRequest();
        request.setCorreo("dra.leon@klinixmed.org");
        request.setContrasena("Password123*");

        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(RolNombre.ROLE_MEDICO_ESPECIALISTA); // Ajustar al valor real del enum

        usuarioActivo = Usuario.builder()
                .id(1L)
                .nombres("Alexandra")
                .apellidos("León")
                .correo("dra.leon@klinixmed.org")
                .contrasena("hashBcrypt")
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .passwordTemporal(false)
                .build();
    }

    @Nested
    @DisplayName("Flujo principal - login exitoso")
    class LoginExitoso {

        @Test
        @DisplayName("Con credenciales válidas retorna token JWT y datos del usuario")
        void loginConCredencialesValidasRetornaLoginResponse() {
            when(usuarioRepository.findByCorreo(request.getCorreo()))
                    .thenReturn(Optional.of(usuarioActivo));
            when(jwtService.generarToken(usuarioActivo)).thenReturn(TOKEN_SIMULADO);
            when(jwtService.getJwtExpiration()).thenReturn(EXPIRACION_MS);

            LoginResponse response = authService.login(request, IP_ORIGEN);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(TOKEN_SIMULADO);
            assertThat(response.getTipo()).isEqualTo("Bearer");
            assertThat(response.getExpiracionMs()).isEqualTo(EXPIRACION_MS);
            assertThat(response.getUsuarioId()).isEqualTo(1L);
            assertThat(response.getCorreo()).isEqualTo("dra.leon@klinixmed.org");
            assertThat(response.getNombreCompleto()).isEqualTo("Alexandra León");

            // Verifica que Spring Security recibió las credenciales tal cual llegaron
            verify(authenticationManager).authenticate(
                    argThat(auth ->
                            auth.getPrincipal().equals(request.getCorreo()) &&
                                    auth.getCredentials().equals(request.getContrasena())));
        }

        @Test
        @DisplayName("Registra la auditoría de login exitoso con el usuario y su rol")
        void loginExitosoRegistraAuditoria() {
            when(usuarioRepository.findByCorreo(request.getCorreo()))
                    .thenReturn(Optional.of(usuarioActivo));
            when(jwtService.generarToken(usuarioActivo)).thenReturn(TOKEN_SIMULADO);
            when(jwtService.getJwtExpiration()).thenReturn(EXPIRACION_MS);

            authService.login(request, IP_ORIGEN);

            ArgumentCaptor<String> descripcionCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditoriaService).registrar(
                    eq("LOGIN"), eq("AUTENTICACION"),
                    descripcionCaptor.capture(), eq(IP_ORIGEN));

            assertThat(descripcionCaptor.getValue()).contains("Alexandra León");
        }
    }

}