package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;

import java.util.List;
import java.util.Optional;

public interface DocumentoRedaccionRepository {
    Long nextId();
    FalDocumentoRedaccion guardar(FalDocumentoRedaccion r);
    Optional<FalDocumentoRedaccion> buscarPorId(Long id);
    List<FalDocumentoRedaccion> buscarPorDocumento(Long idDocumento);
}
