package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentoPlantillaDefaultRepository {
    Long nextId();
    FalDocumentoPlantillaDefault guardar(FalDocumentoPlantillaDefault d);
    Optional<FalDocumentoPlantillaDefault> buscarPorId(Long id);
    List<FalDocumentoPlantillaDefault> buscarDefaultsVigentes(
            AccionDocumental accionDocumental, TipoActa tipoActa,
            Long idDependencia, LocalDateTime en);
    List<FalDocumentoPlantillaDefault> listar();
}