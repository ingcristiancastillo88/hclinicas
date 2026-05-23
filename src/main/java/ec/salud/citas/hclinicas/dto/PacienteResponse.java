package ec.salud.citas.hclinicas.dto;

import ec.salud.citas.hclinicas.enumerado.EstadoCivil;
import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import ec.salud.citas.hclinicas.enumerado.GrupoSanguineo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * DTO de respuesta para Paciente.
 * Incluye todos los datos del paciente más campos calculados.
 */
@Data
@Builder
public class PacienteResponse {

    private Long id;

    // ── Datos de Identificación ───────────────────────────────────────────────
    private String cedula;
    private String historiaNúmero;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private Integer edad;                 // Calculado desde fechaNacimiento
    private String lugarNacimiento;
    private String nacionalidad;
    private EstadoCivil estadoCivil;
    private GrupoSanguineo grupoSanguineo;
    private String instruccion;
    private String ocupacion;
    private String religion;

    // ── Datos de Contacto ─────────────────────────────────────────────────────
    private String correo;
    private String telefono;
    private String celular;
    private String direccion;
    private String ciudad;
    private String provincia;

    // ── Contacto de Emergencia ────────────────────────────────────────────────
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaParentesco;
    private String contactoEmergenciaTelefono;

    // ── Datos Médicos Generales ───────────────────────────────────────────────
    private String alergias;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String medicacionActual;
    private String observacionesGenerales;

    // ── Estado y Auditoría ────────────────────────────────────────────────────
    private EstadoPaciente estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String creadoPor;
    private String actualizadoPor;
}
