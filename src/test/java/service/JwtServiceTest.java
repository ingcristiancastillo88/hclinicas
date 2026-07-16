package service;

import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.enumerado.RolNombre;
import ec.salud.citas.hclinicas.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para JwtService.
 * Al depender de valores inyectados con @Value (jwtSecret, jwtExpiration),
 * se utiliza ReflectionTestUtils para simular la configuración de
 * application.properties sin necesidad de levantar el contexto de Spring,
 * manteniendo la prueba realmente aislada (unitaria) y rápida de ejecutar.
 */
@DisplayName("JwtService - Pruebas unitarias de generación y validación de token")
class JwtServiceTest {

    // Clave de al menos 256 bits (32 caracteres) requerida por el algoritmo HS256
    private static final String SECRET_TEST = "claveSecretaDePruebasParaJwtHClinicas2026Test";
    private static final long EXPIRATION_TEST = 3_600_000L; // 1 hora

    private JwtService jwtService;
    private UserDetails usuarioDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET_TEST);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_TEST);

        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(RolNombre.ROLE_MEDICO_ESPECIALISTA); // Ajustar al valor real del enum

        usuarioDetails = Usuario.builder()
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

    @Test
    @DisplayName("Genera un token JWT no nulo ni vacío con formato válido (3 segmentos)")
    void generarTokenGeneraTokenValido() {
        String token = jwtService.generarToken(usuarioDetails);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("El token generado contiene como subject el correo del usuario")
    void extraerUsernameDevuelveCorreoDelUsuario() {
        String token = jwtService.generarToken(usuarioDetails);

        String username = jwtService.extraerUsername(token);

        assertThat(username).isEqualTo("dra.leon@klinixmed.org");
    }

    @Test
    @DisplayName("Un token recién generado es válido para el mismo usuario")
    void esTokenValidoConMismoUsuarioRetornaTrue() {
        String token = jwtService.generarToken(usuarioDetails);

        boolean esValido = jwtService.esTokenValido(token, usuarioDetails);

        assertThat(esValido).isTrue();
    }

    @Test
    @DisplayName("Un token es inválido si el username no coincide con el del UserDetails")
    void esTokenValidoConUsuarioDistintoRetornaFalse() {
        String token = jwtService.generarToken(usuarioDetails);

        Rol otroRol = mock(Rol.class);
        when(otroRol.getNombre()).thenReturn(RolNombre.ROLE_MEDICO_ESPECIALISTA);
        UserDetails otroUsuario = Usuario.builder()
                .id(2L)
                .nombres("Otro")
                .apellidos("Usuario")
                .correo("otro.usuario@klinixmed.org")
                .contrasena("hash")
                .rol(otroRol)
                .estado(EstadoUsuario.ACTIVO)
                .passwordTemporal(false)
                .build();

        boolean esValido = jwtService.esTokenValido(token, otroUsuario);

        assertThat(esValido).isFalse();
    }

    @Test
    @DisplayName("Un token recién generado no está expirado")
    void esTokenExpiradoConTokenReciénGeneradoRetornaFalse() {
        String token = jwtService.generarToken(usuarioDetails);

        assertThat(jwtService.esTokenExpirado(token)).isFalse();
    }

    @Test
    @DisplayName("getJwtExpiration retorna el valor configurado en application.properties")
    void getJwtExpirationRetornaValorConfigurado() {
        assertThat(jwtService.getJwtExpiration()).isEqualTo(EXPIRATION_TEST);
    }
}