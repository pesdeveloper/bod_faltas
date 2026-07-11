package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaSaveResult;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementacion in-memory de DocumentoFirmaRepository.
 *
 * Slice 8C-6B-1: Long id, sin idActa, con metodos alineados al modelo.
 * FIX-FALLO-NOTI-01-R2: indice secundario byReference para idempotencia atomica.
 */
@Repository
public class InMemoryDocumentoFirmaRepository implements DocumentoFirmaRepository {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<Long, FalDocumentoFirma> store = new ConcurrentHashMap<>();
    private final Map<String, FalDocumentoFirma> byReference = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return sequence.getAndIncrement();
    }

    @Override
    public FalDocumentoFirma guardar(FalDocumentoFirma firma) {
        store.put(firma.getId(), firma);
        if (firma.getReferenciaFirmaExt() != null && !firma.getReferenciaFirmaExt().isBlank()) {
            byReference.put(firma.getReferenciaFirmaExt(), firma);
        }
        return firma;
    }

    @Override
    public Optional<FalDocumentoFirma> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalDocumentoFirma> buscarPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(f -> idDocumento.equals(f.getIdDocumento()))
                .toList();
    }

    @Override
    public Optional<FalDocumentoFirma> buscarPorDocumentoYSeq(Long idDocumento, short seqFirmaReq) {
        return store.values().stream()
                .filter(f -> idDocumento.equals(f.getIdDocumento()) && f.getSeqFirmaReq() == seqFirmaReq)
                .findFirst();
    }

    @Override
    public Optional<FalDocumentoFirma> buscarPorReferenciaFirmaExt(String referenciaFirmaExt) {
        if (referenciaFirmaExt == null || referenciaFirmaExt.isBlank()) return Optional.empty();
        return Optional.ofNullable(byReference.get(referenciaFirmaExt));
    }

    /**
     * Guarda la firma atomicamente solo si no existe ninguna con la misma referenciaFirmaExt.
     *
     * La operacion compute en ConcurrentHashMap garantiza atomicidad por clave:
     * dos llamadas concurrentes con la misma referencia producen exactamente una firma persistida.
     */
    @Override
    public DocumentoFirmaSaveResult guardarSiAusentePorReferencia(FalDocumentoFirma firma) {
        if (firma.getReferenciaFirmaExt() == null || firma.getReferenciaFirmaExt().isBlank()) {
            throw new IllegalArgumentException("referenciaFirmaExt requerida para guardar idempotente");
        }
        FalDocumentoFirma[] holder = new FalDocumentoFirma[1];
        boolean[] isNew = {false};

        byReference.compute(firma.getReferenciaFirmaExt(), (ref, existing) -> {
            if (existing != null) {
                holder[0] = existing;
                return existing;
            }
            store.put(firma.getId(), firma);
            holder[0] = firma;
            isNew[0] = true;
            return firma;
        });

        return new DocumentoFirmaSaveResult(holder[0], !isNew[0]);
    }
}
