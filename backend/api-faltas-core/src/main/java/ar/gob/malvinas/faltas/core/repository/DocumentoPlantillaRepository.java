package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de plantillas documentales y sus requisitos de firma.
 */
public interface DocumentoPlantillaRepository {

    Long nextPlantillaId();

    Long nextFirmaReqId();

    FalDocumentoPlantilla guardar(FalDocumentoPlantilla plantilla);

    Optional<FalDocumentoPlantilla> buscarPorId(Long id);

    Optional<FalDocumentoPlantilla> buscarPorCodigo(String codigo);

    List<FalDocumentoPlantilla> listar();

    List<FalDocumentoPlantilla> buscarPorAccion(AccionDocumental accionDocumental);

    List<FalDocumentoPlantilla> buscarActivasPorAccion(AccionDocumental accionDocumental);

    FalDocumentoPlantillaFirmaReq guardarFirmaReq(FalDocumentoPlantillaFirmaReq req);

    List<FalDocumentoPlantillaFirmaReq> listarFirmaReqPorPlantilla(Long plantillaId);

    Optional<FalDocumentoPlantillaFirmaReq> buscarFirmaReqPorId(Long id);
}
