package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacionDocumento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de acceso a FalActaApelacionDocumento (fal_acta_apelacion_documento).
 */
public interface ApelacionDocumentoRepository {

    Long nextId();

    /** Guarda un documento de apelacion (append-only; no sobrescribir). */
    FalActaApelacionDocumento guardar(FalActaApelacionDocumento doc);

    Optional<FalActaApelacionDocumento> findById(Long id);

    /** Todos los documentos de una apelacion, ordenados por id asc. */
    List<FalActaApelacionDocumento> findByApelacionId(Long apelacionId);
}