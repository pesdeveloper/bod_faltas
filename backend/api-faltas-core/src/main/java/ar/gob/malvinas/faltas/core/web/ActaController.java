package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.FotografiaEntradaAlta;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaAltaTransientRetryService;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.FotografiaEvidenciaService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ImagenEvidenciaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.web.dto.EnriquecerActaRequest;
import ar.gob.malvinas.faltas.core.web.dto.FotografiaMetadataEntradaRequest;
import ar.gob.malvinas.faltas.core.web.dto.FotografiaMetadataResponse;
import ar.gob.malvinas.faltas.core.web.dto.LabrarActaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/faltas/actas")
public class ActaController {

    private final ActaService actaService;
    private final ActaAltaTransientRetryService altaTransientRetryService;
    private final ActaEventoRepository eventoRepository;
    private final FotografiaEvidenciaService fotografiaEvidenciaService;

    public ActaController(ActaService actaService,
                          ActaAltaTransientRetryService altaTransientRetryService,
                          ActaEventoRepository eventoRepository,
                          FotografiaEvidenciaService fotografiaEvidenciaService) {
        this.actaService = actaService;
        this.altaTransientRetryService = altaTransientRetryService;
        this.eventoRepository = eventoRepository;
        this.fotografiaEvidenciaService = fotografiaEvidenciaService;
    }

    /**
     * Alta JSON, sin fotografias. Contrato anterior a la Parte 3D, sin cambios
     * de ruta, status codes ni serializacion (Parte 3D S12: compatibilidad).
     */
    @PostMapping(value = "/labrar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComandoResultado> labrar(@Valid @RequestBody LabrarActaRequest req) {
        return labrarInterno(req, List.of());
    }

    /**
     * Alta multipart con fotografias opcionales (Wave 4 Parte 3D S12).
     *
     * <p>Partes:
     * <ul>
     *   <li>{@code comando} (obligatoria): el mismo JSON que el contrato anterior;</li>
     *   <li>{@code fotografias} (0..10, opcional): partes de archivo;</li>
     *   <li>{@code fotografiasMetadata} (opcional): JSON array de
     *       {@link FotografiaMetadataEntradaRequest}, asociado por posicion a
     *       {@code fotografias}.</li>
     * </ul>
     *
     * <p>Invoca el mismo caso de uso de alta a traves del orquestador de reintento
     * transitorio (Wave 4 Parte 3E): no duplica logica de alta. Cero fotografias
     * produce semantica equivalente al JSON sin fotografias.
     */
    @PostMapping(value = "/labrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComandoResultado> labrarMultipart(
            @Valid @RequestPart("comando") LabrarActaRequest req,
            @RequestPart(value = "fotografias", required = false) List<MultipartFile> fotografias,
            @RequestPart(value = "fotografiasMetadata", required = false) List<FotografiaMetadataEntradaRequest> metadata) {
        return labrarInterno(req, construirEntradas(fotografias, metadata));
    }

    private ResponseEntity<ComandoResultado> labrarInterno(LabrarActaRequest req, List<FotografiaEntradaAlta> fotos) {
        ActorContext ctx = ActorContextHolder.get();
        if (ctx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LabrarActaCommand cmd = new LabrarActaCommand(
                parseTipoActa(req.tipoActa()),
                parseLongId(req.idDependencia()), parseLongId(req.idInspector()),
                req.fechaActa(), req.domicilioHecho(), req.domicilioInfractor(),
                req.observaciones(), req.latInfr(), req.lonInfr(),
                req.infractorNombre(), req.tipoDocumentoInfractor(), req.infractorDocumento(),
                null,
                req.resultadoFirmaInfractor(), req.situacionIdentificacionInfractor(),
                req.evidenciasActa(), req.idTecnico(), req.domicilios(), req.articulosInfringidos(),
                req.vehiculo(), req.transito(), req.contravencion(), req.sustanciasAlimenticias(),
                req.medidasPreventivas(), fotos
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(altaTransientRetryService.labrarConReintento(cmd));
    }

    /**
     * Construye las entradas crudas de fotografia a partir de las partes
     * multipart. La metadata (si viene) se asocia por posicion; si falta o es
     * mas corta que {@code fotografias}, las posiciones sin metadata quedan
     * sin descripcion (nunca un error).
     */
    private static List<FotografiaEntradaAlta> construirEntradas(
            List<MultipartFile> fotografias, List<FotografiaMetadataEntradaRequest> metadata) {
        if (fotografias == null || fotografias.isEmpty()) {
            return List.of();
        }
        List<FotografiaEntradaAlta> entradas = new java.util.ArrayList<>(fotografias.size());
        for (int i = 0; i < fotografias.size(); i++) {
            MultipartFile parte = fotografias.get(i);
            String descripcion = (metadata != null && i < metadata.size() && metadata.get(i) != null)
                    ? metadata.get(i).descripcion() : null;
            entradas.add(new FotografiaEntradaAlta(leerBytes(parte), parte.getOriginalFilename(), descripcion));
        }
        return entradas;
    }

    private static byte[] leerBytes(MultipartFile parte) {
        try {
            return parte.getBytes();
        } catch (IOException e) {
            throw new ImagenEvidenciaInvalidaException("No se pudo leer el contenido de la parte multipart.", e);
        }
    }

    /**
     * {@code tipoActa} llega como texto libre en el contrato HTTP (compatibilidad, ver
     * {@link LabrarActaRequest}). Un valor no reconocido es un error de precondicion del
     * cliente (422), no un error interno: sin esta conversion explicita,
     * {@code TipoActa.valueOf} propaga {@code IllegalArgumentException}, que no tiene
     * {@code @ExceptionHandler} dedicado y cae en el 500 generico de
     * {@code GlobalFaltasControllerAdvice} (Wave 4 Parte 3F, hardening de certificacion).
     */
    private static TipoActa parseTipoActa(String tipoActa) {
        try {
            return TipoActa.valueOf(tipoActa);
        } catch (IllegalArgumentException e) {
            throw new PrecondicionVioladaException("tipoActa invalido: '" + tipoActa + "'. Valores aceptados: "
                    + java.util.Arrays.toString(TipoActa.values()) + ".");
        }
    }

    private static Long parseLongId(String s) {
        if (s == null) return 1L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 1L; }
    }

    @PostMapping("/{id}/enriquecer")
    public ResponseEntity<ComandoResultado> enriquecer(
            @PathVariable Long id,
            @RequestBody(required = false) EnriquecerActaRequest req) {
        ActorContext ctx = ActorContextHolder.get();
        if (ctx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        EnriquecerActaCommand cmd = new EnriquecerActaCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(actaService.enriquecer(cmd));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FalActa> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(actaService.obtenerActa(id));
    }

    @GetMapping("/{id}/snapshot")
    public ResponseEntity<FalActaSnapshot> snapshot(@PathVariable Long id) {
        return ResponseEntity.ok(actaService.obtenerSnapshot(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<FalActaEvento>> timeline(@PathVariable Long id) {
        actaService.obtenerActa(id);
        return ResponseEntity.ok(eventoRepository.buscarPorActa(id));
    }

    @GetMapping("/{id}/evidencias")
    public ResponseEntity<List<FalActaEvidencia>> evidencias(@PathVariable Long id) {
        actaService.obtenerActa(id);
        return ResponseEntity.ok(actaService.listarEvidencias(id));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fotografias (Wave 4 Parte 3D)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Carga posterior de fotografias sobre una acta ya existente, en
     * cualquier etapa del ciclo de vida (Parte 3D S13). Requiere de 1 a 10
     * fotografias; el lote es atomico.
     */
    @PostMapping(value = "/{id}/fotografias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FotografiaMetadataResponse>> agregarFotografias(
            @PathVariable Long id,
            @RequestPart("fotografias") List<MultipartFile> fotografias,
            @RequestPart(value = "fotografiasMetadata", required = false) List<FotografiaMetadataEntradaRequest> metadata) {
        ActorContext ctx = ActorContextHolder.get();
        if (ctx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<FotografiaEntradaAlta> entradas = construirEntradas(fotografias, metadata);
        List<FalActaEvidencia> guardadas = fotografiaEvidenciaService.incorporarLotePosterior(id, entradas);
        List<FotografiaMetadataResponse> respuesta = guardadas.stream()
                .map(FotografiaMetadataResponse::de)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Listado ordenado de fotografias de la acta (Parte 3D S14). No expone
     * {@code storageKey}.
     */
    @GetMapping("/{id}/fotografias")
    public ResponseEntity<List<FotografiaMetadataResponse>> listarFotografias(@PathVariable Long id) {
        actaService.obtenerActa(id);
        List<FotografiaMetadataResponse> respuesta = fotografiaEvidenciaService.listarFotografias(id).stream()
                .map(FotografiaMetadataResponse::de)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Descarga/streaming autenticada de una fotografia (Parte 3D S14). Nunca
     * revela la ruta fisica ni el {@code storageKey}. 404 si la acta no tiene
     * esa fotografia.
     */
    @GetMapping("/{id}/fotografias/{fotoId}/archivo")
    public ResponseEntity<byte[]> descargarFotografia(@PathVariable Long id, @PathVariable Long fotoId) {
        FotografiaEvidenciaService.FotografiaArchivo archivo = fotografiaEvidenciaService.leerArchivo(id, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.mimeTipo()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.contenido());
    }
}
