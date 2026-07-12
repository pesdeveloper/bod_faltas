package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoLote;
import ar.gob.malvinas.faltas.core.domain.model.FalLoteCorreo;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de lotes de correo postal.
 */
public interface LoteCorreoRepository {
    Long nextId();
    FalLoteCorreo guardar(FalLoteCorreo lote);
    Optional<FalLoteCorreo> buscarPorId(Long id);
    Optional<FalLoteCorreo> buscarPorCodigo(String loteCodigo);
    List<FalLoteCorreo> buscarPorEstado(EstadoLote estadoLote);
    Optional<FalLoteCorreo> buscarPorReferenciaExterna(String referenciaExterna);
    Optional<FalLoteCorreo> buscarPorGuid(String guidLoteExt);
    boolean existeCodigo(String loteCodigo);

    /**
     * Persiste el candidato solo si no existe un lote con el mismo loteCodigo.
     * La implementacion debe garantizar atomicidad bajo concurrencia.
     * Devuelve el candidato si fue el ganador, o el lote ya existente si habia uno.
     */
    FalLoteCorreo guardarSiAusentePorCodigo(FalLoteCorreo candidato);
}
