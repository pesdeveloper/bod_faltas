package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.NumerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.result.NumerarDocumentoParaFirmasResponse;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.application.command.EnviarAFirmaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarFirmaDocumentalCommand;
import ar.gob.malvinas.faltas.core.web.dto.NumerarDocumentoRequest;
import ar.gob.malvinas.faltas.core.web.dto.EnviarAFirmaRequest;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoDesdePlantillaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.FirmanteNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.application.result.RegistrarFirmaDocumentalResultado;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;
import ar.gob.malvinas.faltas.core.web.dto.DocumentoFirmaResponse;
import ar.gob.malvinas.faltas.core.web.dto.DocumentoResponse;
import ar.gob.malvinas.faltas.core.web.dto.FirmarDocumentoRequest;
import ar.gob.malvinas.faltas.core.web.dto.GenerarDocumentoDesdePlantillaRequest;
import ar.gob.malvinas.faltas.core.application.command.EmitirDocumentoCommand;
import ar.gob.malvinas.faltas.core.web.dto.EmitirDocumentoRequest;
import ar.gob.malvinas.faltas.core.web.dto.GenerarDocumentoRequest;
import ar.gob.malvinas.faltas.core.web.dto.RegistrarFirmaDocumentalRequest;
import ar.gob.malvinas.faltas.core.application.command.ConvalidarFirmaEscaneadaCommand;
import ar.gob.malvinas.faltas.core.application.command.IncorporarDocumentoEscaneadoCommand;
import ar.gob.malvinas.faltas.core.application.result.ConvalidacionEscaneadaResultado;
import ar.gob.malvinas.faltas.core.web.dto.ConvalidacionFirmaEscaneadaResponse;
import ar.gob.malvinas.faltas.core.web.dto.ConvalidarFirmaEscaneadaRequest;
import ar.gob.malvinas.faltas.core.web.dto.IncorporarDocumentoEscaneadoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faltas")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping("/actas/{idActa}/documentos")
    public ResponseEntity<ComandoResultado> generar(
            @PathVariable Long idActa,
            @Valid @RequestBody GenerarDocumentoRequest req) {
        GenerarDocumentoCommand cmd = new GenerarDocumentoCommand(
                idActa, req.tipoDocumento());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoService.generarDocumento(cmd));
    }

    @PostMapping("/documentos/desde-plantilla")
    public ResponseEntity<DocumentoResponse> generarDesdePlantilla(
            @Valid @RequestBody GenerarDocumentoDesdePlantillaRequest req) {
        GenerarDocumentoDesdePlantillaCommand cmd = new GenerarDocumentoDesdePlantillaCommand(
                req.idActa(), req.plantillaId(), req.idUserAlta());
        FalDocumento doc = documentoService.generarDesdePlantilla(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentoResponse.from(doc));
    }

    @PostMapping("/documentos/{idDocumento}/firmar")
    public ResponseEntity<ComandoResultado> firmar(
            @PathVariable Long idDocumento,
            @Valid @RequestBody FirmarDocumentoRequest req) {
        FirmarDocumentoCommand cmd = new FirmarDocumentoCommand(
                idDocumento, req.firmante(), req.tipoFirma(), req.observaciones());
        return ResponseEntity.ok(documentoService.firmarDocumento(cmd));
    }

    /**
     * Callback de la aplicacion de Firmas para registrar firma documental real.
     * POST /api/faltas/documentos/{documentoId}/firmar-real
     * El actor se extrae del JWT Bearer (sub). Idempotente por referenciaFirmaExt.
     * FIX-FALLO-NOTI-01.
     */
    @PostMapping("/documentos/{documentoId}/firmar-real")
    public ResponseEntity<DocumentoFirmaResponse> firmarReal(
            @PathVariable Long documentoId,
            @Valid @RequestBody RegistrarFirmaDocumentalRequest req) {
        String actor = ActorContextHolder.get().sub();
        RegistrarFirmaDocumentalCommand cmd = new RegistrarFirmaDocumentalCommand(
                documentoId,
                req.seqFirmaReq(),
                req.idFirmante(),
                req.tipoFirma(),
                actor,
                req.hashDocumento(),
                req.referenciaFirmaExt(),
                req.storageKey()
        );
        RegistrarFirmaDocumentalResultado resultado = documentoService.registrarFirmaDocumental(cmd);
        HttpStatus status = resultado.yaExistia() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(DocumentoFirmaResponse.from(resultado.firma()));
    }

    @PostMapping("/documentos/{documentoId}/enviar-a-firma")
    public ResponseEntity<DocumentoResponse> enviarAFirma(
            @PathVariable Long documentoId,
            @Valid @RequestBody EnviarAFirmaRequest req) {
        EnviarAFirmaCommand cmd = new EnviarAFirmaCommand(documentoId, req.idUserOperacion());
        FalDocumento doc = documentoService.enviarAFirma(cmd);
        return ResponseEntity.ok(DocumentoResponse.from(doc));
    }

    /**
     * Emite formalmente un documento.
     * POST /api/faltas/documentos/{documentoId}/emitir
     * Slice 8C-6C-1.
     */
    @PostMapping("/documentos/{documentoId}/emitir")
    public ResponseEntity<DocumentoResponse> emitir(
            @PathVariable Long documentoId,
            @Valid @RequestBody EmitirDocumentoRequest req) {
        EmitirDocumentoCommand cmd = new EmitirDocumentoCommand(
                documentoId,
                req.idUserOperacion(),
                req.storageKey(),
                req.hashDocu());
        FalDocumento doc = documentoService.emitirDocumento(cmd);
        return ResponseEntity.ok(DocumentoResponse.from(doc));
    }

    /**
     * Incorpora un documento escaneado/adjunto externo al expediente.
     * POST /api/faltas/actas/{idActa}/documentos/escaneados
     * Slice 8C-6D-1.
     */
    @PostMapping("/actas/{idActa}/documentos/escaneados")
    public ResponseEntity<DocumentoResponse> incorporarEscaneado(
            @PathVariable Long idActa,
            @Valid @RequestBody IncorporarDocumentoEscaneadoRequest req) {
        IncorporarDocumentoEscaneadoCommand cmd = new IncorporarDocumentoEscaneadoCommand(
                idActa, req.tipoDocu(), req.storageKey(), req.hashDocu(), req.idUserAlta(), req.plantillaId());
        FalDocumento doc = documentoService.incorporarDocumentoEscaneado(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoResponse.from(doc));
    }

    /**
     * Convalida la firma escaneada/olografa de un documento adjunto.
     * POST /api/faltas/documentos/{documentoId}/convalidar-firma-escaneada
     * Slice 8C-6D-1.
     */
    @PostMapping("/documentos/{documentoId}/convalidar-firma-escaneada")
    public ResponseEntity<?> convalidarFirmaEscaneada(
            @PathVariable Long documentoId,
            @Valid @RequestBody ConvalidarFirmaEscaneadaRequest req) {
        ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                documentoId, req.seqFirmaReq(), req.idFirmante(), req.idUserFirma(), req.referenciaFirmaExt());
        ConvalidacionEscaneadaResultado resultado = documentoService.convalidarFirmaEscaneada(cmd);
        if (resultado.firma() != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(DocumentoFirmaResponse.from(resultado.firma()));
        }
        return ResponseEntity.ok(new ConvalidacionFirmaEscaneadaResponse(
                resultado.documento().getId(),
                resultado.documento().getEstadoDocu(),
                "Convalidacion simple registrada. Documento permanece en ADJUNTO."));
    }

    /**
     * Numera un documento concreto via integracion controlada con la aplicacion de Firmas.
     *
     * El actor se extrae del token Bearer (sub del JWT); no se acepta actor en el body.
     * Protegido por autenticacion JWT obligatoria.
     * Idempotente: si el documento ya esta numerado, devuelve el numero existente (HTTP 200).
     * Si numera por primera vez, devuelve HTTP 201.
     *
     * Flujo AL_FIRMAR obligatorio:
     *   1. Firmas solicita POST /documentos/{id}/numerar  <- este endpoint
     *   2. Faltas asigna y persiste el numero.
     *   3. Firmas renderiza el contenido definitivo incluyendo el numero.
     *   4. Firmas calcula el hash del contenido definitivo.
     *   5. Firmas firma ese contenido (POST /documentos/{id}/firmar-real).
     *
     * Slice D-18.
     */
    @PostMapping("/documentos/{documentoId}/numerar")
    public ResponseEntity<NumerarDocumentoParaFirmasResponse> numerar(
            @PathVariable Long documentoId) {
        String actor = ActorContextHolder.get().sub();
        NumerarDocumentoCommand cmd = new NumerarDocumentoCommand(documentoId, actor);
        NumerarDocumentoParaFirmasResponse resp = documentoService.numerarDocumentoParaFirmas(cmd);
        return resp.yaEstabaNumerado()
                ? ResponseEntity.ok(resp)
                : ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
    @GetMapping("/actas/{idActa}/documentos")
    public ResponseEntity<List<FalDocumento>> listar(@PathVariable Long idActa) {
        return ResponseEntity.ok(documentoService.obtenerDocumentos(idActa));
    }

}
