package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.ValorizacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryActaValorizacionRepository
        implements ActaValorizacionRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaValorizacion> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public synchronized FalActaValorizacion save(FalActaValorizacion valorizacion) {
        // Guard: siVigente=true solo permitido para CONFIRMADA
        if (valorizacion.isSiVigente()
                && valorizacion.getEstadoValorizacion() != EstadoValorizacion.CONFIRMADA) {
            throw new PrecondicionVioladaException(
                    "Solo una valorizacion CONFIRMADA puede ser vigente. Estado: "
                            + valorizacion.getEstadoValorizacion());
        }

        FalActaValorizacion existing = store.get(valorizacion.getId());

        // Guard: no insertar una segunda vigente para el mismo acta+tipo
        if (valorizacion.isSiVigente()) {
            boolean otraVigenteExiste = store.values().stream()
                    .anyMatch(v -> !v.getId().equals(valorizacion.getId())
                            && valorizacion.getActaId().equals(v.getActaId())
                            && valorizacion.getTipoValorizacionActa() == v.getTipoValorizacionActa()
                            && v.isSiVigente());
            if (otraVigenteExiste) {
                throw new PrecondicionVioladaException(
                        "Ya existe una vigente para actaId=" + valorizacion.getActaId()
                                + " tipo=" + valorizacion.getTipoValorizacionActa()
                                + ". Usar confirmarVigenteAtomico para reemplazar.");
            }
        }

        if (existing == null) {
            FalActaValorizacion copia = valorizacion.copia();
            copia.setVersionRow(0);
            store.put(copia.getId(), copia);
            return copia;
        }
        if (existing.getVersionRow() != valorizacion.getVersionRow()) {
            throw new ConcurrenciaConflictoException("FalActaValorizacion", valorizacion.getId(),
                    existing.getVersionRow(), valorizacion.getVersionRow());
        }
        FalActaValorizacion copia = valorizacion.copia();
        copia.setVersionRow(existing.getVersionRow() + 1);
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaValorizacion> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaValorizacion::copia);
    }

    @Override
    public List<FalActaValorizacion> findByActaId(Long actaId) {
        return store.values().stream()
                .filter(v -> actaId.equals(v.getActaId()))
                .map(FalActaValorizacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaValorizacion> findByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo) {
        return store.values().stream()
                .filter(v -> actaId.equals(v.getActaId()) && tipo == v.getTipoValorizacionActa())
                .map(FalActaValorizacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaValorizacion> findVigenteByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo) {
        List<FalActaValorizacion> vigentes = store.values().stream()
                .filter(v -> actaId.equals(v.getActaId())
                        && tipo == v.getTipoValorizacionActa()
                        && v.isSiVigente()
                        && v.getEstadoValorizacion() == EstadoValorizacion.CONFIRMADA)
                .map(FalActaValorizacion::copia)
                .collect(Collectors.toList());
        if (vigentes.size() > 1) {
            throw new IllegalStateException(
                    "Invariante rota: " + vigentes.size()
                            + " vigentes CONFIRMADAS para actaId=" + actaId + " tipo=" + tipo);
        }
        return vigentes.isEmpty() ? Optional.empty() : Optional.of(vigentes.get(0));
    }

    @Override
    public List<FalActaValorizacion> findAll() {
        return store.values().stream().map(FalActaValorizacion::copia).collect(Collectors.toList());
    }

    /**
     * Operacion atomica de confirmacion y activacion de vigente.
     * Garantiza una unica vigente CONFIRMADA por acta+tipo al finalizar.
     * Implementa semantica compare-and-set sobre el slot vigente.
     */
    @Override
    public synchronized FalActaValorizacion confirmarVigenteAtomico(
            Long candidataId,
            int versionRowCandidataEsperada,
            Long vigenteAnteriorId,
            Integer versionRowVigenteEsperada,
            LocalDateTime fhConfirmacion,
            String idUserConfirmacion) {

        // 1. Verificar candidata
        FalActaValorizacion candidata = store.get(candidataId);
        if (candidata == null) throw new ValorizacionNoEncontradaException(candidataId);
        if (candidata.getVersionRow() != versionRowCandidataEsperada) {
            throw new ConcurrenciaConflictoException("FalActaValorizacion(candidata)",
                    candidataId, candidata.getVersionRow(), versionRowCandidataEsperada);
        }
        if (candidata.getEstadoValorizacion() != EstadoValorizacion.PRELIMINAR) {
            throw new PrecondicionVioladaException(
                    "La candidata id=" + candidataId + " no es PRELIMINAR. Estado: "
                            + candidata.getEstadoValorizacion());
        }

        // 2. Verificar slot vigente actual (CAS semantics)
        List<FalActaValorizacion> vigentesSlotsActuales = store.values().stream()
                .filter(v -> candidata.getActaId().equals(v.getActaId())
                        && candidata.getTipoValorizacionActa() == v.getTipoValorizacionActa()
                        && v.isSiVigente())
                .collect(Collectors.toList());

        if (vigentesSlotsActuales.size() > 1) {
            throw new IllegalStateException(
                    "Invariante interna rota: " + vigentesSlotsActuales.size()
                            + " filas vigentes para actaId=" + candidata.getActaId()
                            + " tipo=" + candidata.getTipoValorizacionActa());
        }

        FalActaValorizacion vigenteActual = vigentesSlotsActuales.isEmpty()
                ? null : vigentesSlotsActuales.get(0);

        if (vigenteAnteriorId == null) {
            // Se esperaba no-vigente. Si hay una, otro confirmo concurrentemente.
            if (vigenteActual != null) {
                throw new ConcurrenciaConflictoException(
                        "FalActaValorizacion(vigente-slot)",
                        candidata.getActaId() + "/" + candidata.getTipoValorizacionActa(),
                        vigenteActual.getVersionRow(), -1);
            }
        } else {
            // Se esperaba una vigente especifica.
            if (vigenteActual == null) {
                throw new ConcurrenciaConflictoException(
                        "FalActaValorizacion(vigente-anterior)", vigenteAnteriorId,
                        -1, versionRowVigenteEsperada);
            }
            if (!vigenteAnteriorId.equals(vigenteActual.getId())) {
                throw new ConcurrenciaConflictoException(
                        "FalActaValorizacion(vigente-anterior-id)", vigenteAnteriorId,
                        (int) (long) vigenteActual.getId(), versionRowVigenteEsperada);
            }
            if (vigenteActual.getVersionRow() != versionRowVigenteEsperada) {
                throw new ConcurrenciaConflictoException(
                        "FalActaValorizacion(vigente-anterior)", vigenteAnteriorId,
                        vigenteActual.getVersionRow(), versionRowVigenteEsperada);
            }
        }

        // 3. Reemplazar vigente anterior si existe
        if (vigenteActual != null) {
            FalActaValorizacion anteriorActualizada = vigenteActual.copia();
            anteriorActualizada.marcarReemplazada();
            anteriorActualizada.setVersionRow(vigenteActual.getVersionRow() + 1);
            store.put(anteriorActualizada.getId(), anteriorActualizada);
        }

        // 4. Confirmar candidata
        FalActaValorizacion candidataConfirmada = candidata.copia();
        candidataConfirmada.marcarPagada(fhConfirmacion, idUserConfirmacion);
        candidataConfirmada.setVersionRow(candidata.getVersionRow() + 1);
        store.put(candidataConfirmada.getId(), candidataConfirmada);

        // 5. Validar invariante final: exactamente una vigente por acta+tipo
        long finalVigentes = store.values().stream()
                .filter(v -> candidata.getActaId().equals(v.getActaId())
                        && candidata.getTipoValorizacionActa() == v.getTipoValorizacionActa()
                        && v.isSiVigente())
                .count();
        if (finalVigentes != 1) {
            throw new IllegalStateException(
                    "Invariante rota post-confirmacion: " + finalVigentes + " vigentes");
        }

        return candidataConfirmada.copia();
    }

    public void cargarSeed(List<FalActaValorizacion> lista) {
        long maxId = 0;
        for (FalActaValorizacion v : lista) {
            store.put(v.getId(), v.copia());
            if (v.getId() > maxId) maxId = v.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "valorizaciones"; }

    @Override
    public int size() { return store.size(); }
}
