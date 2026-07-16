package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.salud.citas.hclinicas.controller.UsuarioController;
import ec.salud.citas.hclinicas.dto.ActualizarUsuarioRequest;
import ec.salud.citas.hclinicas.dto.CrearUsuarioRequest;
import ec.salud.citas.hclinicas.dto.UsuarioResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
 * Pruebas unitarias para UsuarioController usando MockMvc en modo standalone.
 * No levanta el contexto completo de Spring, por lo que UsuarioService se
 * simula con Mockito para validar exclusivamente la capa de presentación:
 * mapeo de la petición HTTP, código de respuesta y estructura del ApiResponse
 * (HU-004, HU-005).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioController - Pruebas unitarias de gestión de usuarios")
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UsuarioResponse usuarioResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController).build();

        usuarioResponse = UsuarioResponse.builder()
                .id(1L)
                .nombres("Alexandra")
                .apellidos("León")
                .nombreCompleto("Alexandra León")
                .cedula("1710034065")
                .correo("dra.leon@klinixmed.org")
                .telefono("0999999999")
                .rol("ROLE_MEDICO_ESPECIALISTA")
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }

    @Nested
    @DisplayName("GET /usuarios")
    class Listar {

        @Test
        @DisplayName("Lista usuarios paginados y retorna 200")
        void listarRetorna200() throws Exception {
            PageResponse<UsuarioResponse> pageResponse = PageResponse.<UsuarioResponse>builder()
                    .contenido(List.of(usuarioResponse))
                    .paginaActual(0)
                    .totalPaginas(1)
                    .totalElementos(1L)
                    .tamanioPagina(10)
                    .primera(true)
                    .ultima(true)
                    .build();

            when(usuarioService.listar(anyString(), eq(0), eq(10))).thenReturn(pageResponse);

            mockMvc.perform(get("/usuarios")
                            .param("busqueda", "")
                            .param("pagina", "0")
                            .param("tamano", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Usuarios obtenidos exitosamente"))
                    .andExpect(jsonPath("$.data.contenido[0].correo").value("dra.leon@klinixmed.org"));
        }
    }

    @Nested
    @DisplayName("GET /usuarios/{id}")
    class Obtener {

        @Test
        @DisplayName("Obtiene un usuario por ID y retorna 200")
        void obtenerRetorna200() throws Exception {
            when(usuarioService.obtenerPorId(1L)).thenReturn(usuarioResponse);

            mockMvc.perform(get("/usuarios/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Usuario obtenido exitosamente"))
                    .andExpect(jsonPath("$.data.correo").value("dra.leon@klinixmed.org"));
        }
    }

    @Nested
    @DisplayName("POST /usuarios")
    class Crear {

        @Test
        @DisplayName("Crea un usuario con datos válidos y retorna 201")
        void crearConDatosValidosRetorna201() throws Exception {
            CrearUsuarioRequest request = new CrearUsuarioRequest();
            request.setNombres("Alexandra");
            request.setApellidos("León");
            request.setCorreo("dra.leon@klinixmed.org");
            request.setContrasena("Password123*");
            request.setRolId(1L);

            when(usuarioService.crear(any(CrearUsuarioRequest.class), anyString()))
                    .thenReturn(usuarioResponse);

            mockMvc.perform(post("/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mensaje").value("Usuario creado exitosamente"))
                    .andExpect(jsonPath("$.data.correo").value("dra.leon@klinixmed.org"));
        }

        @Test
        @DisplayName("Con correo en blanco retorna 400 por validación @Valid")
        void crearConCorreoEnBlancoRetorna400() throws Exception {
            CrearUsuarioRequest request = new CrearUsuarioRequest();
            request.setNombres("Alexandra");
            request.setApellidos("León");
            request.setCorreo("");
            request.setContrasena("Password123*");
            request.setRolId(1L);

            mockMvc.perform(post("/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Con contraseña menor a 8 caracteres retorna 400 por validación @Valid")
        void crearConContrasenaCortaRetorna400() throws Exception {
            CrearUsuarioRequest request = new CrearUsuarioRequest();
            request.setNombres("Alexandra");
            request.setApellidos("León");
            request.setCorreo("dra.leon@klinixmed.org");
            request.setContrasena("abc123");
            request.setRolId(1L);

            mockMvc.perform(post("/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /usuarios/{id}")
    class Actualizar {

        @Test
        @DisplayName("Actualiza un usuario y retorna 200")
        void actualizarRetorna200() throws Exception {
            ActualizarUsuarioRequest request = new ActualizarUsuarioRequest();
            request.setNombres("Alexandra");
            request.setApellidos("León");
            request.setCorreo("dra.leon@klinixmed.org");
            request.setRolId(1L);

            when(usuarioService.actualizar(eq(1L), any(ActualizarUsuarioRequest.class), anyString()))
                    .thenReturn(usuarioResponse);

            mockMvc.perform(put("/usuarios/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Usuario actualizado exitosamente"))
                    .andExpect(jsonPath("$.data.correo").value("dra.leon@klinixmed.org"));
        }
    }

    @Nested
    @DisplayName("PATCH /usuarios/{id}/desactivar y /activar")
    class CambiarEstado {

        @Test
        @DisplayName("Desactiva un usuario y retorna 200 con el mensaje correcto")
        void desactivarRetorna200() throws Exception {
            mockMvc.perform(patch("/usuarios/{id}/desactivar", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Usuario desactivado exitosamente"));

            verify(usuarioService).desactivar(eq(1L), anyString());
        }

        @Test
        @DisplayName("Activa un usuario y retorna 200 con el mensaje correcto")
        void activarRetorna200() throws Exception {
            mockMvc.perform(patch("/usuarios/{id}/activar", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Usuario activado exitosamente"));

            verify(usuarioService).activar(eq(1L), anyString());
        }
    }
}
