package ec.salud.citas.hclinicas.entity;

import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entidad Usuario - implementa UserDetails para integración con Spring Security.
 * Contempla HU-001, HU-002, HU-003, HU-004.
 * Tabla: usuarios
 */
@Entity
@Table(name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "correo", name = "uk_usuario_correo"),
                @UniqueConstraint(columnNames = "cedula", name = "uk_usuario_cedula")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "cedula", length = 13)
    private String cedula;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "contrasena", nullable = false)
    private String contrasena;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    // ── Spring Security UserDetails ────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.getNombre().name()));
    }

    @Override
    public String getPassword() {
        return contrasena;
    }

    @Override
    public String getUsername() {
        return correo;   // El login se realiza con correo electrónico
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return estado != EstadoUsuario.ELIMINADO;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return estado == EstadoUsuario.ACTIVO;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
