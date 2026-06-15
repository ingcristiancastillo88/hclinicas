package ec.salud.citas.hclinicas.config;


import ec.salud.citas.hclinicas.security.JwtAuthenticationFilter;
import ec.salud.citas.hclinicas.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración principal de Spring Security.
 * Arquitectura: Stateless con JWT (sin sesiones HTTP).
 * Protección: CSRF deshabilitado (API REST), XSS y CORS configurados.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // Habilita @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // ── Rutas públicas (sin autenticación) ────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/auth/**",           // Login
            "/v3/api-docs/**",    // Swagger
            "/swagger-ui/**",
            "/qr/validar/**"      // Validación QR pública (Sprint 6)
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivar CSRF (API REST stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Configurar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Políticas de autorización
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // ── SUPERADMINISTRADOR: acceso total ──
                        .requestMatchers("/admin/**").hasRole("SUPERADMINISTRADOR")

                        // ── ADMINISTRADOR + SUPERADMIN: gestión de usuarios ──
                        .requestMatchers("/usuarios/**")
                        .hasAnyRole("SUPERADMINISTRADOR", "ADMINISTRADOR")

                        // ── MEDICO + SUPERADMIN + ADMIN: módulos clínicos ──
                        .requestMatchers("/pacientes/**")
                        .hasAnyRole("SUPERADMINISTRADOR", "MEDICO_ESPECIALISTA")
                        .requestMatchers("/historias/**")
                        .hasAnyRole("SUPERADMINISTRADOR", "ADMINISTRADOR", "MEDICO_ESPECIALISTA")
                        .requestMatchers("/citas/**")
                        .hasAnyRole("SUPERADMINISTRADOR", "MEDICO_ESPECIALISTA")

                        // ── PACIENTE: solo lectura de su propia información ──
                        .requestMatchers("/mi-perfil/**").hasRole("PACIENTE")

                        // El resto requiere autenticación
                        .anyRequest().authenticated()
                )

                // Stateless: no sesiones HTTP
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Proveedor de autenticación
                .authenticationProvider(authenticationProvider())

                // Agregar filtro JWT antes del filtro estándar
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── CORS ─────────────────────────────────────────────────────────────────
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── BCrypt (RNF-008 + CU-001: contraseñas cifradas) ──────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Authentication Provider ───────────────────────────────────────────────
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
