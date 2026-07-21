package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.RolDocuActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaDocumentoYaExisteException;
import ar.gob.malvinas.faltas.core.domain.model.ActaDocumentoId;
import ar.gob.malvinas.faltas.core.domain.model.FalActaDocumento;
import ar.gob.malvinas.faltas.core.repository.ActaDocumentoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementacion InMemory del pivot acta-documento.
 *
 * Thread-safety:
 * - ConcurrentHashMap para lecturas concurrentes seguras.
 * - principalLock para operaciones de principalidad (unicidad invariante).
 * - guardar usa putIfAbsent semantico via synchronized para detectar duplicados.
 */
@Repository
public class InMemoryActaDocumentoRepository
        implements ActaDocumentoRepository, ResettableInMemoryRepository {

    /** Clave: ActaDocumentoId -> FalActaDocumento */
    private final Map<ActaDocumentoId, FalActaDocumento> store = new ConcurrentHashMap<>();

    /** Lock para operaciones de principalidad y guardado con unicidad. */
    private final Object principalLock = new Object();

    @Override
    public FalActaDocumento guardar(FalActaDocumento relacion) {
        synchronized (principalLock) {
            ActaDocumentoId key = relacion.getId();
            if (store.containsKey(key)) {
                throw new ActaDocumentoYaExisteException(key.actaId(), key.documentoId());
            }
            FalActaDocumento copia = relacion.copia();
            store.put(key, copia);
            return copia.copia();
        }
    }

    /** Guarda o actualiza sin verificar duplicado (para uso interno de operaciones atomicas). */
    private FalActaDocumento guardarInterno(FalActaDocumento relacion) {
        FalActaDocumento copia = relacion.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaDocumento> buscarPorIdCompuesto(ActaDocumentoId id) {
        return Optional.ofNullable(store.get(id)).map(FalActaDocumento::copia);
    }

    @Override
    public boolean existe(Long actaId, Long documentoId) {
        return store.containsKey(new ActaDocumentoId(actaId, documentoId));
    }

    @Override
    public List<FalActaDocumento> listarPorActa(Long actaId) {
        return store.values().stream()
                .filter(r -> r.getActaId().equals(actaId))
                .map(FalActaDocumento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaDocumento> listarPorDocumento(Long documentoId) {
        return store.values().stream()
                .filter(r -> r.getDocumentoId().equals(documentoId))
                .map(FalActaDocumento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaDocumento> listarPorActaYRol(Long actaId, RolDocuActa rol) {
        return store.values().stream()
                .filter(r -> r.getActaId().equals(actaId) && r.getRolDocuActa() == rol)
                .map(FalActaDocumento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaDocumento> buscarPrincipalPorActaYRol(Long actaId, RolDocuActa rol) {
        return store.values().stream()
                .filter(r -> r.getActaId().equals(actaId)
                        && r.getRolDocuActa() == rol
                        && r.isSiPrincipal())
                .map(FalActaDocumento::copia)
                .findFirst();
    }

    @Override
    public FalActaDocumento asociarComoPrincipalAtomico(Long actaId, Long documentoId,
                                                         RolDocuActa rol,
                                                         String idUserAlta, LocalDateTime fhAlta) {
        synchronized (principalLock) {
            ActaDocumentoId key = new ActaDocumentoId(actaId, documentoId);

            // Bajar principal anterior del mismo (acta, rol)
            if (rol.exigeUnicidadPrincipal()) {
                store.values().stream()
                        .filter(r -> r.getActaId().equals(actaId)
                                && r.getRolDocuActa() == rol
                                && r.isSiPrincipal()
                                && !r.getDocumentoId().equals(documentoId))
                        .forEach(r -> r.setSiPrincipalInterno(false));
            }

            FalActaDocumento relacion;
            if (store.containsKey(key)) {
                relacion = store.get(key);
                relacion.setSiPrincipalInterno(true);
            } else {
                relacion = new FalActaDocumento(actaId, documentoId, rol, true, fhAlta, idUserAlta);
                store.put(key, relacion);
            }
            return relacion.copia();
        }
    }

    @Override
    public FalActaDocumento reemplazarPrincipalAtomico(Long actaId, Long documentoIdNuevo,
                                                        RolDocuActa rol,
                                                        String idUserAlta, LocalDateTime fhAlta) {
        synchronized (principalLock) {
            // Bajar principal actual
            store.values().stream()
                    .filter(r -> r.getActaId().equals(actaId)
                            && r.getRolDocuActa() == rol
                            && r.isSiPrincipal())
                    .forEach(r -> r.setSiPrincipalInterno(false));

            // Crear o actualizar el nuevo principal
            ActaDocumentoId key = new ActaDocumentoId(actaId, documentoIdNuevo);
            FalActaDocumento relacion;
            if (store.containsKey(key)) {
                relacion = store.get(key);
                relacion.setSiPrincipalInterno(true);
            } else {
                relacion = new FalActaDocumento(actaId, documentoIdNuevo, rol, true, fhAlta, idUserAlta);
                store.put(key, relacion);
            }
            return relacion.copia();
        }
    }

    @Override
    public void reset() {
        synchronized (principalLock) {
            store.clear();
        }
    }

    @Override
    public String nombre() { return "acta-documentos"; }

    @Override
    public int size() { return store.size(); }
}
