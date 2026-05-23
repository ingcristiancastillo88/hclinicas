package ec.salud.citas.hclinicas.service.impl;


import ec.salud.citas.hclinicas.dto.*;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.entity.Paciente;
import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.PacienteRepository;
import ec.salud.citas.hclinicas.service.AuditoriaService;
import ec.salud.citas.hclinicas.service.PacienteService;
import ec.salud.citas.hclinicas.util.CedulaEcuatorianaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;


/**
 * Implementación del servicio de pacientes.
 * Aplica el algoritmo de validación de cédula ecuatoriana (requisito especial CU-003).
 * Registra auditoría en cada operación (HU-006).
 * Usa eliminación lógica mediante cambio de estado (no borrado físico).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;
    private final AuditoriaService auditoriaService;

    private static final String MODULO = "PACIENTES";

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PacienteResponse crear(CrearPacienteRequest request, String ipOrigen) {

        // 1. Validar cédula ecuatoriana con algoritmo oficial (requisito especial CU-003)
        if (!CedulaEcuatorianaValidator.esValida(request.getCedula())) {
            throw new ReglaNegocioException(
                    "La cédula ingresada no es válida: " + request.getCedula());
        }

        // 2. Validar unicidad de cédula (CU-003 flujo alternativo)
        if (pacienteRepository.existsByCedula(request.getCedula())) {
            throw new ReglaNegocioException(
                    "Ya existe un paciente registrado con la cédula: " + request.getCedula());
        }

        // 3. Validar unicidad de número de historia si se proporciona
        if (request.getHistoriaNúmero() != null && !request.getHistoriaNúmero().isBlank()
                && pacienteRepository.existsByHistoriaNúmero(request.getHistoriaNúmero())) {
            throw new ReglaNegocioException(
                    "Ya existe un paciente con el número de historia: "
                            + request.getHistoriaNúmero());
        }

        // 4. Construir y persistir entidad
        Paciente paciente = Paciente.builder()
                .cedula(request.getCedula())
                .historiaNúmero(request.getHistoriaNúmero())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .fechaNacimiento(request.getFechaNacimiento())
                .lugarNacimiento(request.getLugarNacimiento())
                .nacionalidad(request.getNacionalidad())
                .estadoCivil(request.getEstadoCivil())
                .grupoSanguineo(request.getGrupoSanguineo())
                .instruccion(request.getInstruccion())
                .ocupacion(request.getOcupacion())
                .religion(request.getReligion())
                .correo(request.getCorreo())
                .telefono(request.getTelefono())
                .celular(request.getCelular())
                .direccion(request.getDireccion())
                .ciudad(request.getCiudad())
                .provincia(request.getProvincia())
                .contactoEmergenciaNombre(request.getContactoEmergenciaNombre())
                .contactoEmergenciaParentesco(request.getContactoEmergenciaParentesco())
                .contactoEmergenciaTelefono(request.getContactoEmergenciaTelefono())
                .alergias(request.getAlergias())
                .antecedentesPersonales(request.getAntecedentesPersonales())
                .antecedentesFamiliares(request.getAntecedentesFamiliares())
                .medicacionActual(request.getMedicacionActual())
                .observacionesGenerales(request.getObservacionesGenerales())
                .estado(EstadoPaciente.ACTIVO)
                .build();

        paciente = pacienteRepository.save(paciente);

        // 5. Registrar auditoría (HU-006)
        auditoriaService.registrar(
                "CREATE", MODULO,
                "Paciente creado: " + paciente.getNombreCompleto()
                        + " - Cédula: " + paciente.getCedula(),
                ipOrigen
        );

        log.info("Paciente creado: {} - Cédula: {}", paciente.getNombreCompleto(),
                paciente.getCedula());
        return toFullResponse(paciente);
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PacienteResponse actualizar(Long id, ActualizarPacienteRequest request,
                                       String ipOrigen) {

        Paciente paciente = obtenerEntidad(id);

        // Validar unicidad de número de historia si cambia
        if (request.getHistoriaNúmero() != null
                && !request.getHistoriaNúmero().isBlank()
                && !request.getHistoriaNúmero().equals(paciente.getHistoriaNúmero())
                && pacienteRepository.existsByHistoriaNúmero(request.getHistoriaNúmero())) {
            throw new ReglaNegocioException(
                    "Ya existe un paciente con el número de historia: "
                            + request.getHistoriaNúmero());
        }

        // Actualizar campos (la cédula NO se puede modificar)
        paciente.setHistoriaNúmero(request.getHistoriaNúmero());
        paciente.setNombres(request.getNombres());
        paciente.setApellidos(request.getApellidos());
        paciente.setFechaNacimiento(request.getFechaNacimiento());
        paciente.setLugarNacimiento(request.getLugarNacimiento());
        paciente.setNacionalidad(request.getNacionalidad());
        paciente.setEstadoCivil(request.getEstadoCivil());
        paciente.setGrupoSanguineo(request.getGrupoSanguineo());
        paciente.setInstruccion(request.getInstruccion());
        paciente.setOcupacion(request.getOcupacion());
        paciente.setReligion(request.getReligion());
        paciente.setCorreo(request.getCorreo());
        paciente.setTelefono(request.getTelefono());
        paciente.setCelular(request.getCelular());
        paciente.setDireccion(request.getDireccion());
        paciente.setCiudad(request.getCiudad());
        paciente.setProvincia(request.getProvincia());
        paciente.setContactoEmergenciaNombre(request.getContactoEmergenciaNombre());
        paciente.setContactoEmergenciaParentesco(request.getContactoEmergenciaParentesco());
        paciente.setContactoEmergenciaTelefono(request.getContactoEmergenciaTelefono());
        paciente.setAlergias(request.getAlergias());
        paciente.setAntecedentesPersonales(request.getAntecedentesPersonales());
        paciente.setAntecedentesFamiliares(request.getAntecedentesFamiliares());
        paciente.setMedicacionActual(request.getMedicacionActual());
        paciente.setObservacionesGenerales(request.getObservacionesGenerales());

        paciente = pacienteRepository.save(paciente);

        auditoriaService.registrar(
                "UPDATE", MODULO,
                "Paciente actualizado: " + paciente.getNombreCompleto()
                        + " - Cédula: " + paciente.getCedula(),
                ipOrigen
        );

        return toFullResponse(paciente);
    }

    // ── Obtener por ID ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse obtenerPorId(Long id) {
        return toFullResponse(obtenerEntidad(id));
    }

    // ── Obtener por Cédula ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse obtenerPorCedula(String cedula) {
        Paciente paciente = pacienteRepository.findByCedula(cedula)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Paciente no encontrado con cédula: " + cedula));
        return toFullResponse(paciente);
    }

    // ── Listar con búsqueda paginada (HU-009) ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PacienteResumenResponse> listar(String busqueda, int pagina,
                                                        int tamano, boolean soloActivos) {
        PageRequest pageRequest = PageRequest.of(pagina, tamano,
                Sort.by("apellidos").ascending());

        Page<PacienteResumenResponse> page;

        if (soloActivos) {
            page = pacienteRepository
                    .buscarPacientes(busqueda, EstadoPaciente.ACTIVO, pageRequest)
                    .map(this::toResumenResponse);
        } else {
            page = pacienteRepository
                    .buscarTodos(busqueda, pageRequest)
                    .map(this::toResumenResponse);
        }

        return PageResponse.of(page);
    }

    // ── Desactivar (eliminación lógica) ───────────────────────────────────────

    @Override
    @Transactional
    public void desactivar(Long id, String ipOrigen) {
        Paciente paciente = obtenerEntidad(id);
        paciente.setEstado(EstadoPaciente.INACTIVO);
        pacienteRepository.save(paciente);

        auditoriaService.registrar(
                "DEACTIVATE", MODULO,
                "Paciente desactivado: " + paciente.getNombreCompleto()
                        + " - Cédula: " + paciente.getCedula(),
                ipOrigen
        );
        log.info("Paciente desactivado: {}", paciente.getCedula());
    }

    // ── Activar ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void activar(Long id, String ipOrigen) {
        Paciente paciente = obtenerEntidad(id);
        paciente.setEstado(EstadoPaciente.ACTIVO);
        pacienteRepository.save(paciente);

        auditoriaService.registrar(
                "ACTIVATE", MODULO,
                "Paciente activado: " + paciente.getNombreCompleto()
                        + " - Cédula: " + paciente.getCedula(),
                ipOrigen
        );
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Paciente obtenerEntidad(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Paciente no encontrado con ID: " + id));
    }

    private Integer calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) return null;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    private PacienteResponse toFullResponse(Paciente p) {
        return PacienteResponse.builder()
                .id(p.getId())
                .cedula(p.getCedula())
                .historiaNúmero(p.getHistoriaNúmero())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .nombreCompleto(p.getNombreCompleto())
                .fechaNacimiento(p.getFechaNacimiento())
                .edad(calcularEdad(p.getFechaNacimiento()))
                .lugarNacimiento(p.getLugarNacimiento())
                .nacionalidad(p.getNacionalidad())
                .estadoCivil(p.getEstadoCivil())
                .grupoSanguineo(p.getGrupoSanguineo())
                .instruccion(p.getInstruccion())
                .ocupacion(p.getOcupacion())
                .religion(p.getReligion())
                .correo(p.getCorreo())
                .telefono(p.getTelefono())
                .celular(p.getCelular())
                .direccion(p.getDireccion())
                .ciudad(p.getCiudad())
                .provincia(p.getProvincia())
                .contactoEmergenciaNombre(p.getContactoEmergenciaNombre())
                .contactoEmergenciaParentesco(p.getContactoEmergenciaParentesco())
                .contactoEmergenciaTelefono(p.getContactoEmergenciaTelefono())
                .alergias(p.getAlergias())
                .antecedentesPersonales(p.getAntecedentesPersonales())
                .antecedentesFamiliares(p.getAntecedentesFamiliares())
                .medicacionActual(p.getMedicacionActual())
                .observacionesGenerales(p.getObservacionesGenerales())
                .estado(p.getEstado())
                .fechaCreacion(p.getFechaCreacion())
                .fechaActualizacion(p.getFechaActualizacion())
                .creadoPor(p.getCreadoPor())
                .actualizadoPor(p.getActualizadoPor())
                .build();
    }

    private PacienteResumenResponse toResumenResponse(Paciente p) {
        return PacienteResumenResponse.builder()
                .id(p.getId())
                .cedula(p.getCedula())
                .historiaNúmero(p.getHistoriaNúmero())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .nombreCompleto(p.getNombreCompleto())
                .fechaNacimiento(p.getFechaNacimiento())
                .edad(calcularEdad(p.getFechaNacimiento()))
                .celular(p.getCelular())
                .correo(p.getCorreo())
                .grupoSanguineo(p.getGrupoSanguineo())
                .estado(p.getEstado())
                .build();
    }
}
