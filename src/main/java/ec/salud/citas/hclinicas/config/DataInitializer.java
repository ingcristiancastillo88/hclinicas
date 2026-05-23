package ec.salud.citas.hclinicas.config;


import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.entity.Usuario;
import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import ec.salud.citas.hclinicas.enumerado.RolNombre;
import ec.salud.citas.hclinicas.repository.RolRepository;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicializador de datos del sistema.
 * Crea los 4 roles y el usuario superadministrador por defecto al arrancar.
 * Solo inserta si no existen (idempotente).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // ── 1. Crear roles si no existen ──────────────────────────────────
            crearRolSiNoExiste(RolNombre.ROLE_SUPERADMINISTRADOR,
                    "Acceso total al sistema - configuración técnica y base de datos");
            crearRolSiNoExiste(RolNombre.ROLE_ADMINISTRADOR,
                    "Supervisión del sistema y acceso a módulos específicos");
            crearRolSiNoExiste(RolNombre.ROLE_MEDICO_ESPECIALISTA,
                    "Gestión clínica completa: pacientes, historias, citas y documentos");
            crearRolSiNoExiste(RolNombre.ROLE_PACIENTE,
                    "Consulta de información personal, citas e historial propio");

            // ── 2. Crear superadministrador por defecto ────────────────────────
            if (!usuarioRepository.existsByCorreo("admin@hclinicas.com")) {
                Rol rolAdmin = rolRepository.findByNombre(RolNombre.ROLE_SUPERADMINISTRADOR)
                        .orElseThrow();

                Usuario superAdmin = Usuario.builder()
                        .nombres("Super")
                        .apellidos("Administrador")
                        .correo("admin@hclinicas.com")
                        .contrasena(passwordEncoder.encode("Admin@2026!"))
                        .rol(rolAdmin)
                        .estado(EstadoUsuario.ACTIVO)
                        .build();

                usuarioRepository.save(superAdmin);
                log.info("SuperAdministrador creado: admin@hclinicas.com / Admin@2026!");
            }

            log.info("Datos iniciales del sistema verificados correctamente");
        };
    }

    private void crearRolSiNoExiste(RolNombre nombre, String descripcion) {
        if (!rolRepository.existsByNombre(nombre)) {
            rolRepository.save(Rol.builder()
                    .nombre(nombre)
                    .descripcion(descripcion)
                    .build());
            log.info("Rol creado: {}", nombre);
        }
    }
}
