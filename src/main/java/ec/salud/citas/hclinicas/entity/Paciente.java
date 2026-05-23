package ec.salud.citas.hclinicas.entity;

import ec.salud.citas.hclinicas.enumerado.EstadoCivil;
import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import ec.salud.citas.hclinicas.enumerado.GrupoSanguineo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entidad Paciente - contiene toda la información personal y médica básica.
 * Relacionada con HistoriaClinica y CitaMedica en sprints posteriores.
 * Tabla: pacientes
 *
 * HU-007: Registro de pacientes
 * HU-008: Edición de pacientes
 * HU-009: Búsqueda de pacientes
 * CU-003: Gestión de pacientes
 */
@Entity
@Table(name = "pacientes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "cedula",       name = "uk_paciente_cedula"),
                @UniqueConstraint(columnNames = "historia_numero", name = "uk_paciente_historia")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Datos de Identificación ───────────────────────────────────────────────

    @Column(name = "cedula", nullable = false, length = 10)
    private String cedula;

    @Column(name = "historia_numero", length = 20)
    private String historiaNúmero;        // Número de historia clínica del consultorio

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "lugar_nacimiento", length = 100)
    private String lugarNacimiento;

    @Column(name = "nacionalidad", length = 80)
    private String nacionalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_civil", length = 20)
    private EstadoCivil estadoCivil;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_sanguineo", length = 15)
    private GrupoSanguineo grupoSanguineo;

    @Column(name = "instruccion", length = 50)
    private String instruccion;           // Primaria, Secundaria, Superior, etc.

    @Column(name = "ocupacion", length = 100)
    private String ocupacion;

    @Column(name = "religion", length = 80)
    private String religion;

    // ── Datos de Contacto ─────────────────────────────────────────────────────

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "celular", length = 15)
    private String celular;

    @Column(name = "direccion", length = 250)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "provincia", length = 100)
    private String provincia;

    // ── Contacto de Emergencia ────────────────────────────────────────────────

    @Column(name = "contacto_emergencia_nombre", length = 200)
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_parentesco", length = 80)
    private String contactoEmergenciaParentesco;

    @Column(name = "contacto_emergencia_telefono", length = 15)
    private String contactoEmergenciaTelefono;

    // ── Datos Médicos Generales ───────────────────────────────────────────────

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "antecedentes_personales", columnDefinition = "TEXT")
    private String antecedentesPersonales;

    @Column(name = "antecedentes_familiares", columnDefinition = "TEXT")
    private String antecedentesFamiliares;

    @Column(name = "medicacion_actual", columnDefinition = "TEXT")
    private String medicacionActual;

    // ── Estado del Registro ───────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoPaciente estado = EstadoPaciente.ACTIVO;

    @Column(name = "observaciones_generales", columnDefinition = "TEXT")
    private String observacionesGenerales;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
