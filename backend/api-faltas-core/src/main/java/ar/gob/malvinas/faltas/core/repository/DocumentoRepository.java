package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de documentos del expediente.
 */
public interface DocumentoRepository {
    FalDocumento guardar(FalDocumento documento);
    Optional<FalDocumento> buscarPorId(Long id);
    List<FalDocumento> buscarPorActa(Long idActa);
    Long nextId();
}
