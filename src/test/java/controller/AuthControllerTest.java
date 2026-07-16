package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.salud.citas.hclinicas.controller.AuthController;
import ec.salud.citas.hclinicas.dto.LoginRequest;
import ec.salud.citas.hclinicas.dto.LoginResponse;
import ec.salud.citas.hclinicas.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias para AuthController usando MockMvc en modo standalone.
 * No levanta el contexto completo de Spring (a diferencia de las pruebas de
 * implementación con @SpringBootTest), por lo que AuthService se simula con
 * Mockito para validar exclusivamente la capa de presentación: mapeo de la
 * petición HTTP, código de respuesta y estructura del ApiResponse.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Pruebas unitarias del endpoint /auth/login")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("POST /auth/login con credenciales válidas retorna 200 y el token JWT")
    void loginConCredencialesValidasRetorna200ConToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setCorreo("dra.leon@klinixmed.org");
        request.setContrasena("Password123*");

        LoginResponse response = LoginResponse.builder()
                .token("token.jwt.simulado")
                .tipo("Bearer")
                .expiracionMs(3_600_000L)
                .usuarioId(1L)
                .nombreCompleto("Alexandra León")
                .correo("dra.leon@klinixmed.org")
                .rol("MEDICO")
                .build();

        when(authService.login(any(LoginRequest.class), any(String.class)))
                .thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Inicio de sesión exitoso"))
                .andExpect(jsonPath("$.data.token").value("token.jwt.simulado"))
                .andExpect(jsonPath("$.data.correo").value("dra.leon@klinixmed.org"));
    }

    @Test
    @DisplayName("Toma la IP del header X-Forwarded-For cuando está presente")
    void loginUsaIpDeHeaderXForwardedForCuandoExiste() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setCorreo("dra.leon@klinixmed.org");
        request.setContrasena("Password123*");

        when(authService.login(any(LoginRequest.class), eq("200.10.10.5")))
                .thenReturn(LoginResponse.builder().token("t").tipo("Bearer").build());

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", "200.10.10.5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).login(any(LoginRequest.class), eq("200.10.10.5"));
    }

    @Test
    @DisplayName("Con body inválido (correo vacío) retorna 400 por validación @Valid")
    void loginConCorreoVacioRetorna400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setCorreo("");
        request.setContrasena("Password123*");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}