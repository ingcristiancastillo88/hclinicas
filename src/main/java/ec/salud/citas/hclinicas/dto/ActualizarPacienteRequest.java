package ec.salud.citas.hclinicas.dto;


import ec.salud.citas.hclinicas.enumerado.EstadoCivil;
import ec.salud.citas.hclinicas.enumerado.GrupoSanguineo;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO para la actualización de un paciente (HU-008 / CU-003).
 * La cédula no se puede modificar una vez creado el registro.
 */
@Data
public class ActualizarPacienteRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    private String apellidos;

    @Size(max = 20)
    private String historiaNúmero;

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

    @Size(max = 200)
    private String contactoEmergenciaNombre;

    @Size(max = 80)
    private String contactoEmergenciaParentesco;

    @Size(max = 15)
    private String contactoEmergenciaTelefono;

    private String alergias;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String medicacionActual;
    private String observacionesGenerales;
}
