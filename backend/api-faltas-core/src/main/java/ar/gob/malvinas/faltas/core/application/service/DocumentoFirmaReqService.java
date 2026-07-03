package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.MaterializarFirmaReqDocumentoCommand;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoFirmaReqYaMaterializadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaReqRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de materializacion y consulta de requisitos de firma de documentos.
 *
 * Genera el snapshot de FalDocumentoFirmaReq desde FalDocumentoPlantillaFirmaReq
 * al momento de materializar requisitos de un documento concreto.
 *
 * Guardrails:
 * - No cambia estadoDocu del documento.
 * - No crea FalDocumentoFirma.
 * - No firma.
 * - No valida firmantes habilitados.
 * - No numera documento.
 * - No consume talonario.
 *
 * Slice 8C-4.
 */
@Service
public class DocumentoFirmaReqService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoPlantillaRepository documentoPlantillaRepository;
    private final DocumentoFirmaReqRepository documentoFirmaReqRepository;

    public DocumentoFirmaReqService(
            DocumentoRepository documentoRepository,
            DocumentoPlantillaRepository documentoPlantillaRepository,
            DocumentoFirmaReqRepository documentoFirmaReqRepository) {
        this.documentoRepository = documentoRepository;
        this.documentoPlantillaRepository = documentoPlantillaRepository;
        this.documentoFirmaReqRepository = documentoFirmaReqRepository;
    }

    public List<FalDocumentoFirmaReq> materializarDesdePlantilla(MaterializarFirmaReqDocumentoCommand cmd) {
        if (cmd.documentoId() == null) {
            throw new PrecondicionVioladaException("documentoId es obligatorio");
        }
        if (cmd.idUserAlta() == null || cmd.idUserAlta().isBlank()) {
            throw new PrecondicionVioladaException("idUserAlta es obligatorio y no puede estar en blanco");
        }

        FalDocumento doc = documentoRepository.buscarPorId(cmd.documentoId())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.documentoId()));

        if (doc.getPlantillaId() == null) {
            throw new PrecondicionVioladaException(
                    "El documento no tiene plantillaId. Solo se pueden materializar requisitos "
                    + "para documentos generados desde plantilla. documentoId=" + cmd.documentoId());
        }

        FalDocumentoPlantilla plantilla = documentoPlantillaRepository.buscarPorId(doc.getPlantillaId())
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(doc.getPlantillaId()));

        if (documentoFirmaReqRepository.existePorDocumento(cmd.documentoId())) {
            throw new DocumentoFirmaReqYaMaterializadaException(cmd.documentoId());
        }

        List<FalDocumentoPlantillaFirmaReq> requisitosPlantilla =
                documentoPlantillaRepository.listarFirmaReqPorPlantilla(plantilla.getId());

        List<FalDocumentoPlantillaFirmaReq> activosObligatorios = requisitosPlantilla.stream()
                .filter(r -> r.isSiActiva() && r.isSiObligatoria())
                .toList();

        TipoFirmaReq tipoFirmaReq = doc.getTipoFirmaReq();

        if (tipoFirmaReq == TipoFirmaReq.NO_REQUIERE) {
            if (!activosObligatorios.isEmpty()) {
                throw new PrecondicionVioladaException(
                        "El documento tiene tipoFirmaReq=NO_REQUIERE pero la plantilla tiene "
                        + activosObligatorios.size()
                        + " requisito(s) obligatorio(s) activo(s). Inconsistencia de plantilla/documento.");
            }
            return List.of();
        }

        if (tipoFirmaReq == TipoFirmaReq.FIRMA_MULTIPLE) {
            if (activosObligatorios.size() < 2) {
                throw new PrecondicionVioladaException(
                        "El documento tiene tipoFirmaReq=FIRMA_MULTIPLE pero la plantilla tiene solo "
                        + activosObligatorios.size()
                        + " requisito(s) obligatorio(s) activo(s). Se requieren al menos dos.");
            }
        } else {
            if (activosObligatorios.isEmpty()) {
                throw new PrecondicionVioladaException(
                        "El documento tiene tipoFirmaReq=" + tipoFirmaReq
                        + " pero la plantilla no tiene requisitos obligatorios activos.");
            }
        }

        List<FalDocumentoPlantillaFirmaReq> todosActivos = requisitosPlantilla.stream()
                .filter(FalDocumentoPlantillaFirmaReq::isSiActiva)
                .toList();

        LocalDateTime ahora = LocalDateTime.now();
        List<FalDocumentoFirmaReq> materializados = new ArrayList<>();

        for (FalDocumentoPlantillaFirmaReq req : todosActivos) {
            Long id = documentoFirmaReqRepository.nextId();
            FalDocumentoFirmaReq firmaReq = new FalDocumentoFirmaReq(
                    id,
                    cmd.documentoId(),
                    req.getSeqFirmaReq(),
                    req.getRolFirmaReq(),
                    req.getMecanismoFirmaReq(),
                    null,
                    req.isSiObligatoria(),
                    true,
                    ahora,
                    cmd.idUserAlta()
            );
            documentoFirmaReqRepository.guardar(firmaReq);
            materializados.add(firmaReq);
        }

        return materializados;
    }

    public List<FalDocumentoFirmaReq> listarPorDocumento(Long documentoId) {
        documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNoEncontradoException(documentoId));
        return documentoFirmaReqRepository.listarPorDocumento(documentoId);
    }

    public FalDocumentoFirmaReq obtener(Long firmaReqId) {
        return documentoFirmaReqRepository.buscarPorId(firmaReqId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Requisito de firma no encontrado: " + firmaReqId));
    }
}
