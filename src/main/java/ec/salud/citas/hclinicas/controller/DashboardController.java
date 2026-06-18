package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.dto.response.CitaMedicaResumenResponse;
import ec.salud.citas.hclinicas.dto.response.StatsAdminResponse;
import ec.salud.citas.hclinicas.dto.response.StatsMedicoResponse;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import ec.salud.citas.hclinicas.repository.CitaMedicaRepository;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.repository.PacienteRepository;
import ec.salud.citas.hclinicas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints de estadísticas para el Dashboard.
 * Cada rol recibe datos específicos según sus permisos.
 *
 * GET /api/dashboard/stats      → estadísticas admin/superadmin
 * GET /api/dashboard/medico     → estadísticas del médico
 * GET /api/dashboard/citas-hoy  → citas del día actual
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PacienteRepository   pacienteRepo;
    private final CitaMedicaRepository citaRepo;
    private final ConsultaRepository   consultaRepo;
    private final UsuarioRepository    usuarioRepo;


    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ApiResponse<StatsAdminResponse> statsAdmin() {

        LocalDate hoy       = LocalDate.now();
        LocalDate inicioMes = YearMonth.now().atDay(1);
        LocalDate finMes    = YearMonth.now().atEndOfMonth();

        long totalPacientes = pacienteRepo.count();

        long pacientesActivos = pacienteRepo
                .countByEstado(EstadoPaciente.ACTIVO);

        long citasHoy = citaRepo
                .countByFechaCitaAndEstadoNot(hoy, EstadoCita.CANCELADA);

        long citasPendientes = citaRepo.countByEstado(EstadoCita.PROGRAMADA)
                + citaRepo.countByEstado(EstadoCita.CONFIRMADA);

        long citasAtendidas  = citaRepo.countByEstado(EstadoCita.ATENDIDA);
        long citasCanceladas = citaRepo.countByEstado(EstadoCita.CANCELADA);

        long citasMes = citaRepo
                .findByRangoFechas(inicioMes, finMes)
                .size();

        long consultasMes = consultaRepo
                .countByFechaConsultaBetweenAndActivaTrue(inicioMes, finMes);

        long totalUsuarios = usuarioRepo.count();

        return ApiResponse.ok("Estadisticas obtenidas",
                StatsAdminResponse.builder()
                        .totalPacientes(totalPacientes)
                        .pacientesActivos(pacientesActivos)
                        .citasHoy(citasHoy)
                        .citasMes(citasMes)
                        .citasPendientes(citasPendientes)
                        .citasAtendidas(citasAtendidas)
                        .citasCanceladas(citasCanceladas)
                        .consultasMes(consultasMes)
                        .totalUsuarios(totalUsuarios)
                        .build()
        );
    }

    // ── Estadísticas Médico Especialista ──────────────────────────────────

    @GetMapping("/medico")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ApiResponse<StatsMedicoResponse> statsMedico() {

        LocalDate hoy       = LocalDate.now();
        LocalDate finSemana = hoy.plusDays(7);
        LocalDate inicioMes = YearMonth.now().atDay(1);
        LocalDate finMes    = YearMonth.now().atEndOfMonth();

        long citasHoy = citaRepo
                .countByFechaCitaAndEstadoNot(hoy, EstadoCita.CANCELADA);

        long citasPendientesHoy = citaRepo.findByFecha(hoy).stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA
                        || c.getEstado() == EstadoCita.CONFIRMADA)
                .count();

        long citasAtendidasHoy = citaRepo.findByFecha(hoy).stream()
                .filter(c -> c.getEstado() == EstadoCita.ATENDIDA)
                .count();

        long citasPendientesSemana = citaRepo
                .findByRangoFechas(hoy, finSemana).stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA
                        || c.getEstado() == EstadoCita.CONFIRMADA)
                .count();

        long totalPacientes = pacienteRepo.count();

        long consultasMes = consultaRepo
                .countByFechaConsultaBetweenAndActivaTrue(inicioMes, finMes);

        return ApiResponse.ok("Estadisticas medico",
                StatsMedicoResponse.builder()
                        .totalPacientes(totalPacientes)
                        .citasHoy(citasHoy)
                        .citasPendientesHoy(citasPendientesHoy)
                        .citasAtendidasHoy(citasAtendidasHoy)
                        .citasPendientesSemana(citasPendientesSemana)
                        .consultasMes(consultasMes)
                        .build()
        );
    }

    // ── Citas del día (médico y admin) ────────────────────────────────────

    @GetMapping("/citas-hoy")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ApiResponse<List<CitaMedicaResumenResponse>> citasHoy(
            @RequestParam(defaultValue = "20") int tamano) {

        List<CitaMedicaResumenResponse> citas = citaRepo
                .findByFecha(LocalDate.now())
                .stream()
                .limit(tamano)
                .map(c -> CitaMedicaResumenResponse.builder()
                        .id(c.getId())
                        .fechaCita(c.getFechaCita())
                        .horaInicio(c.getHoraInicio() != null
                                ? c.getHoraInicio().toString().substring(0, 5) : null)
                        .horaFin(c.getHoraFin() != null
                                ? c.getHoraFin().toString().substring(0, 5) : null)
                        .duracionMinutos(c.getDuracionMinutos())
                        .tipoCita(c.getTipoCita())
                        .motivoCita(c.getMotivoCita())
                        .pacienteId(c.getPaciente().getId())
                        .pacienteNombreCompleto(c.getPaciente().getNombreCompleto())
                        .pacienteCedula(c.getPaciente().getCedula())
                        .pacienteCelular(c.getPaciente().getCelular())
                        .estado(c.getEstado())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.ok("Citas de hoy", citas);
    }
}