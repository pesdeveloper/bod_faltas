package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaContenido;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentoPlantillaContenidoRepository {
    Long nextId();
    FalDocumentoPlantillaContenido guardar(FalDocumentoPlantillaContenido c);
    Optional<FalDocumentoPlantillaContenido> buscarPorId(Long id);
    List<FalDocumentoPlantillaContenido> buscarContenidoVigente(Long plantillaId, LocalDateTime en);
    List<FalDocumentoPlantillaContenido> listarPorPlantilla(Long plantillaId);
}