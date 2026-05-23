package ec.salud.citas.hclinicas.dto;


import ec.salud.citas.hclinicas.enumerado.EstadoCivil;
import ec.salud.citas.hclinicas.enumerado.GrupoSanguineo;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO para la creación de un paciente (HU-007 / CU-003).
 * La cédula ecuatoriana se valida mediante el algoritmo
 * oficial en la capa de servicio (requisito especial CU-003).
 */
@Data
public class CrearPacienteRequest {

    // ── Datos de Identificación ───────────────────────────────────────────────

    @NotBlank(message = "La cédula es obligatoria")
    @Size(min = 10, max = 10, message = "La cédula debe tener exactamente 10 dígitos")
    @Pattern(regexp = "\\d{10}", message = "La cédula debe contener solo dígitos numéricos")
    private String cedula;

    @Size(max = 20, message = "El número de historia no puede superar 20 caracteres")
    private String historiaNúmero;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
    private String apellidos;

    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate fechaNacimiento;

    @Size(max = 100)
    private String lugarNacimiento;

    @Size(max = 80)
    private String nacionalidad;

    private EstadoCivil estadoCivil;

    private GrupoSanguineo grupoSanguineo;

    @Size(max = 50)
    private String instruccion;

    @Size(max = 100)
    private String ocupacion;

    @Size(max = 80)
    private String religion;

    // ── Datos de Contacto ─────────────────────────────────────────────────────

    @Email(message = "El formato del correo no es válido")
    @Size(max = 150)
    private String correo;

    @Size(max = 15)
    private String telefono;

    @Size(max = 15)
    private String celular;

    @Size(max = 250)
    private String direccion;

    @Size(max = 100)
    private String ciudad;

    @Size(max = 100)
    private String provincia;

    // ── Contacto de Emergencia ────────────────────────────────────────────────

    @Size(max = 200)
    private String contactoEmergenciaNombre;

    @Size(max = 80)
    private String contactoEmergenciaParentesco;

    @Size(max = 15)
    private String contactoEmergenciaTelefono;

    // ── Datos Médicos Generales ───────────────────────────────────────────────

    private String alergias;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String medicacionActual;
    private String observacionesGenerales;
}