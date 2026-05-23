package ec.salud.citas.hclinicas.service;

import ec.salud.citas.hclinicas.dto.request.ActualizarConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearHistoriaClinicaRequest;
import ec.salud.citas.hclinicas.dto.response.ConsultaResumenResponse;
import ec.salud.citas.hclinicas.dto.response.ConsultaResponse;
import ec.salud.citas.hclinicas.dto.response.HistoriaClinicaResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;


public interface HistoriaClinicaService {

    // ── Historia Clínica ──────────────────────────────────────────────────────
    HistoriaClinicaResponse crearOActualizar(
            CrearHistoriaClinicaRequest request, String ipOrigen);

    HistoriaClinicaResponse obtenerPorPaciente(Long pacienteId);

    HistoriaClinicaResponse obtenerPorId(Long id);

    // ── Consultas ─────────────────────────────────────────────────────────────
    ConsultaResponse crearConsulta(
            CrearConsultaRequest request, String ipOrigen);

    ConsultaResponse actualizarConsulta(
            Long consultaId, ActualizarConsultaRequest request, String ipOrigen);

    ConsultaResponse obtenerConsulta(Long consultaId);

    PageResponse<ConsultaResumenResponse> listarConsultas(
            Long historiaClinicaId, int pagina, int tamano);

    void eliminarConsulta(Long consultaId, String ipOrigen);

    // ── Archivos ──────────────────────────────────────────────────────────────
    void subirArchivo(Long consultaId, MultipartFile file,
                      String tipoArchivo, String descripcion, String ipOrigen);

    void eliminarArchivo(Long archivoId, String ipOrigen);
}