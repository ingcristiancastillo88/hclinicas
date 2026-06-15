package ec.salud.citas.hclinicas.service;

import ec.salud.citas.hclinicas.dto.request.ActualizarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CancelarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearCitaRequest;
import ec.salud.citas.hclinicas.dto.response.CitaMedicaResumenResponse;
import ec.salud.citas.hclinicas.dto.response.CitaMedicaResponse;
import ec.salud.citas.hclinicas.dto.response.DisponibilidadResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato del servicio de citas médicas.
 * HU-016 Registro · HU-017 Disponibilidad · HU-018 Edición/Cancelación · HU-019 Calendario
 */
public interface CitaMedicaService {

    // CRUD principal
    CitaMedicaResponse crear(CrearCitaRequest request, String ipOrigen);

    CitaMedicaResponse actualizar(Long id, ActualizarCitaRequest request, String ipOrigen);

    CitaMedicaResponse obtener(Long id);

    PageResponse<CitaMedicaResumenResponse> listar(
            EstadoCita estado, LocalDate fecha,
            String busqueda, int pagina, int tamano);

    // Cancelar y cambios de estado
    void cancelar(Long id, CancelarCitaRequest request, String ipOrigen);

    void marcarAtendida(Long id, String ipOrigen);

    void marcarNoAsistio(Long id, String ipOrigen);

    // Calendario (HU-019)
    List<CitaMedicaResumenResponse> obtenerPorRangoFechas(
            LocalDate inicio, LocalDate fin);

    List<CitaMedicaResumenResponse> obtenerPorFecha(LocalDate fecha);

    // Disponibilidad (HU-017)
    DisponibilidadResponse verificarDisponibilidad(
            LocalDate fecha, String horaInicio, int duracionMinutos);

    // Citas de un paciente (HU-020)
    PageResponse<CitaMedicaResumenResponse> citasPorPaciente(
            Long pacienteId, int pagina, int tamano);
}