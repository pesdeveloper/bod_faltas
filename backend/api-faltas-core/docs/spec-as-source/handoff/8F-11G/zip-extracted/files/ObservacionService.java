package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de observaciones con registry de validadores por tipo de entidad.
 *
 * El registry valida existencia para entidades implementadas.
 * Los tipos no disponibles aun son rechazados explicitamente.
 * Permite que paralizar/archivar creen la observacion dentro de la misma operacion atomica.
 */
@Service
public class ObservacionService {

    private final ObservacionRepository repo;
    private final Map<EntidadTipoObservada, EntidadExistenciaValidator> validadores;

    private static final Set<EntidadTipoObservada> TIPOS_NO_DISPONIBLES = EnumSet.of(
            EntidadTipoObservada.PERSONA,
            EntidadTipoObservada.DOMICILIO,
            EntidadTipoObservada.NOTIFICACION_INTENTO,
            EntidadTipoObservada.OBLIGACION_PAGO,
            EntidadTipoObservada.FORMA_PAGO,
            EntidadTipoObservada.PLAN_PAGO,
            EntidadTipoObservada.MOVIMIENTO_PAGO,
            EntidadTipoObservada.TALONARIO,
            EntidadTipoObservada.MOVIMIENTO_TALONARIO
    );

    public ObservacionService(ObservacionRepository repo, ActaRepository actaRepo) {
        this.repo = repo;
        this.validadores = new EnumMap<>(EntidadTipoObservada.class);
        registrarValidador(EntidadTipoObservada.ACTA,
                id -> actaRepo.buscarPorId(id)
                        .orElseThrow(() -> new PrecondicionVioladaException("Acta no encontrada: " + id)));
        registrarValidador(EntidadTipoObservada.PARALIZACION,
                id -> { /* existencia no verificable sin paralizacion repo aqui; se valida en ParalizacionActaService */ });
        registrarValidador(EntidadTipoObservada.ARCHIVO,
                id -> { /* existencia no verificable sin archivo repo aqui; se valida en ArchivoActaService */ });
        registrarValidador(EntidadTipoObservada.DOCUMENTO, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.EVIDENCIA, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.NOTIFICACION, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.FALLO, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.APELACION, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.GESTION_EXTERNA, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.MEDIDA_PREVENTIVA, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.BLOQUEANTE_CIERRE_MATERIAL, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.ARTICULO_INFRINGIDO, id -> { /* validado externamente */ });
        registrarValidador(EntidadTipoObservada.VALORIZACION, id -> { /* validado externamente */ });
    }

    private void registrarValidador(EntidadTipoObservada tipo, EntidadExistenciaValidator v) {
        validadores.put(tipo, v);
    }

    /**
     * Crea una observacion para la entidad indicada.
     * Valida texto, tipo disponible y existencia de entidad.
     */
    public FalObservacion agregar(
            EntidadTipoObservada tipo,
            Long entidadId,
            String texto,
            OrigenObservacion origen,
            String idUserAlta) {
        validarTipoDisponible(tipo);
        validarTexto(texto);
        validadores.get(tipo).validar(entidadId);
        Long id = repo.nextId();
        FalObservacion obs = new FalObservacion(id, tipo, entidadId, null, texto.trim(), origen,
                LocalDateTime.now(), idUserAlta);
        return repo.guardar(obs);
    }

    /**
     * Crea una observacion sin validar existencia de la entidad.
     * Para uso interno dentro de operaciones atomicas donde la entidad acaba de crearse.
     */
    public FalObservacion agregarSinValidarExistencia(
            EntidadTipoObservada tipo,
            Long entidadId,
            String texto,
            OrigenObservacion origen,
            String idUserAlta) {
        validarTipoDisponible(tipo);
        validarTexto(texto);
        Long id = repo.nextId();
        FalObservacion obs = new FalObservacion(id, tipo, entidadId, null, texto.trim(), origen,
                LocalDateTime.now(), idUserAlta);
        return repo.guardar(obs);
    }

    public List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return repo.listarPorEntidad(tipo, entidadId);
    }

    public List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return repo.listarActivasPorEntidad(tipo, entidadId);
    }

    public void desactivar(Long id) {
        repo.buscarPorId(id).orElseThrow(() ->
                new PrecondicionVioladaException("Observacion no encontrada: " + id));
        repo.desactivar(id);
    }

    private void validarTipoDisponible(EntidadTipoObservada tipo) {
        if (TIPOS_NO_DISPONIBLES.contains(tipo)) {
            throw new PrecondicionVioladaException(
                    "Tipo de entidad no disponible aun en InMemory: " + tipo
                    + ". Implementacion prevista en slices posteriores.");
        }
        if (!validadores.containsKey(tipo)) {
            throw new PrecondicionVioladaException(
                    "Tipo de entidad sin validador registrado: " + tipo);
        }
    }

    private void validarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new PrecondicionVioladaException("El texto de observacion no puede ser vacio");
        }
        if (texto.trim().length() > 1000) {
            throw new PrecondicionVioladaException(
                    "El texto de observacion supera 1000 caracteres: " + texto.trim().length());
        }
    }

    @FunctionalInterface
    public interface EntidadExistenciaValidator {
        void validar(Long entidadId);
    }
}
