package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.port.CalendarioAdministrativo;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

/**
 * Servicio de calendario administrativo local.
 *
 * Implementa CalendarioAdministrativo consultando exclusivamente el repositorio local.
 * No consulta calendarios externos durante el calculo de dias computables.
 *
 * Reglas fijas (no persisten en el repositorio):
 *   - Domingo: no computable.
 *   - 1 de enero: no computable.
 *   - 1 de mayo: no computable.
 *
 * El sabado es computable salvo excepcion activa registrada en el repositorio.
 */
@Service
public class CalendarioAdministrativoService implements CalendarioAdministrativo {

    private final DiaNoComputableRepository repository;
    private final FaltasClock clock;

    public CalendarioAdministrativoService(DiaNoComputableRepository repository, FaltasClock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public boolean esDiaComputable(LocalDate fecha) {
        if (fecha == null) throw new IllegalArgumentException("fecha es obligatoria");
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        if (fecha.getMonth() == Month.JANUARY && fecha.getDayOfMonth() == 1) return false;
        if (fecha.getMonth() == Month.MAY && fecha.getDayOfMonth() == 1) return false;
        return repository.buscarActivoPorFecha(fecha).isEmpty();
    }

    /**
     * Registra un dia como no computable en el calendario local.
     *
     * Se rechaza si la fecha ya es no computable por regla fija (domingo, 1-ene, 1-may),
     * ya que registrarlo seria redundante.
     *
     * Para SINCRONIZACION_EXTERNA con referenciaExterna ya registrada para el mismo origen:
     * lanza PrecondicionVioladaException (rechazo explicito sin duplicar el registro).
     *
     * Si la fecha ya tiene un registro activo (duplicado secuencial o concurrente):
     * lanza PrecondicionVioladaException.
     */
    public FalDiaNoComputable registrarDiaNoComputable(
            LocalDate fecha,
            TipoDiaNoComputable tipo,
            String descripcion,
            OrigenDiaNoComputable origen,
            String referenciaExterna,
            String actor) {

        // Validacion y normalizacion estructural completa ANTES de reglas fijas,
        // repositorio, reloj e ID. Ninguna entrada invalida debe consultar el reloj,
        // consumir nextId ni tocar el repositorio.
        if (fecha == null) throw new IllegalArgumentException("fecha es obligatoria");
        if (tipo == null) throw new IllegalArgumentException("tipo es obligatorio");
        if (origen == null) throw new IllegalArgumentException("origen es obligatorio");

        String descripcionNorm = normalizarDescripcion(descripcion);
        String actorNorm = normalizarActor(actor);
        String referenciaNorm = normalizarReferenciaExterna(origen, referenciaExterna);

        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY)
            throw new PrecondicionVioladaException(
                    "Registro redundante: el domingo nunca es computable y no se persiste");
        if (fecha.getMonth() == Month.JANUARY && fecha.getDayOfMonth() == 1)
            throw new PrecondicionVioladaException(
                    "Registro redundante: el 1 de enero nunca es computable y no se persiste");
        if (fecha.getMonth() == Month.MAY && fecha.getDayOfMonth() == 1)
            throw new PrecondicionVioladaException(
                    "Registro redundante: el 1 de mayo nunca es computable y no se persiste");

        repository.buscarActivoPorFecha(fecha).ifPresent(existente ->
                { throw new PrecondicionVioladaException(
                        "Ya existe un registro activo para la fecha: " + fecha); });

        if (origen == OrigenDiaNoComputable.SINCRONIZACION_EXTERNA) {
            repository.buscarPorOrigenYReferenciaExterna(origen, referenciaNorm)
                    .ifPresent(existente -> {
                        throw new PrecondicionVioladaException(
                                "Ya existe un registro para origen=" + origen
                                        + " referenciaExterna=" + referenciaNorm);
                    });
        }

        var ahora = clock.now();
        Long id = repository.nextId();

        var candidato = new FalDiaNoComputable(
                id, fecha, tipo, descripcionNorm, origen, referenciaNorm, ahora, actorNorm);

        var resultado = repository.guardarActivoSiAusentePorFecha(candidato);

        if (!resultado.getId().equals(candidato.getId())) {
            throw new PrecondicionVioladaException(
                    "Alta concurrente: ya existe un registro activo para la fecha: " + fecha);
        }

        return resultado;
    }

    /**
     * Desactiva (baja logica) un dia no computable por id.
     */
    public FalDiaNoComputable desactivarDiaNoComputable(Long id, String actor) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        String actorNorm = normalizarActor(actor);

        FalDiaNoComputable dia = repository.buscarPorId(id)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Dia no computable no encontrado: id=" + id));

        if (!dia.isSiActivo())
            throw new PrecondicionVioladaException(
                    "El dia no computable ya esta inactivo: id=" + id);

        var ahora = clock.now();
        dia.desactivar(ahora, actorNorm);
        return repository.guardar(dia);
    }

    public List<FalDiaNoComputable> listarActivos() {
        return repository.listarActivosOrdenados();
    }

    private static String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank())
            throw new IllegalArgumentException("descripcion es obligatoria");
        String norm = descripcion.trim();
        if (norm.length() > 160)
            throw new IllegalArgumentException("descripcion max 160 caracteres");
        return norm;
    }

    private static String normalizarActor(String actor) {
        if (actor == null || actor.isBlank())
            throw new IllegalArgumentException("actor es obligatorio");
        String norm = actor.trim();
        if (norm.length() > 36)
            throw new IllegalArgumentException("actor max 36 caracteres");
        return norm;
    }

    private static String normalizarReferenciaExterna(OrigenDiaNoComputable origen, String referenciaExterna) {
        if (origen == OrigenDiaNoComputable.MANUAL) {
            if (referenciaExterna != null)
                throw new IllegalArgumentException("referenciaExterna debe ser null para origen MANUAL");
            return null;
        }
        if (referenciaExterna == null || referenciaExterna.isBlank())
            throw new IllegalArgumentException("referenciaExterna es obligatoria para SINCRONIZACION_EXTERNA");
        String norm = referenciaExterna.trim();
        if (norm.length() > 200)
            throw new IllegalArgumentException("referenciaExterna max 200 caracteres");
        return norm;
    }
}
