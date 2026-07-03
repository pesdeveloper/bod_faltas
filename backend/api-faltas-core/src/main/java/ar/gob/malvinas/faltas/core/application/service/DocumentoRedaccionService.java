package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionResultado;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableContextBuilder;
import ar.gob.malvinas.faltas.core.application.command.CrearRedaccionDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.result.DocumentoRedaccionResponse;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaContenidoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaContenido;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de creacion y gestion de redacciones documentales editables.
 *
 * Flujo de crearRedaccionDesdePlantilla:
 *   1. Validar command.
 *   2. Verificar que el documento existe.
 *   3. Resolver plantilla default.
 *   4. Resolver contenido vigente de la plantilla.
 *   5. Combinar template con variablesContexto del command.
 *   6. Si hay faltantes requeridos o desconocidos: PrecondicionVioladaException.
 *   7. Crear FalDocumentoRedaccion en BORRADOR y guardar.
 *
 * Flujo de crearRedaccionConContextoActa (slice 8F-2):
 *   1. Cargar acta del repositorio.
 *   2. Cargar documento del repositorio.
 *   3. Cargar fallo activo (opcional).
 *   4. Cargar pago voluntario (opcional).
 *   5. Construir contexto via DocumentoVariableContextBuilder.
 *   6. Delegar a crearRedaccionDesdePlantilla.
 *
 * Guardrails:
 *   - No genera PDF.
 *   - No emite documento.
 *   - No envia a firma.
 *   - FalDocumento.storageKey/hashDocu/fhGeneracion permanecen null.
 *
 * Slice 8F-1.
 * Slice 8F-2: agrega crearRedaccionConContextoActa y repos de acta/fallo/pago.
 */
@Service
public class DocumentoRedaccionService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoPlantillaDefaultService defaultService;
    private final DocumentoPlantillaContenidoRepository contenidoRepository;
    private final DocumentoRedaccionRepository redaccionRepository;
    private final DocumentoCombinacionService combinacionService;
    private final ActaRepository actaRepository;
    private final FalloActaRepository falloRepository;
    private final PagoVoluntarioRepository pagoRepository;

    @Autowired
    public DocumentoRedaccionService(
            DocumentoRepository documentoRepository,
            DocumentoPlantillaDefaultService defaultService,
            DocumentoPlantillaContenidoRepository contenidoRepository,
            DocumentoRedaccionRepository redaccionRepository,
            DocumentoCombinacionService combinacionService,
            ActaRepository actaRepository,
            FalloActaRepository falloRepository,
            PagoVoluntarioRepository pagoRepository) {
        this.documentoRepository = documentoRepository;
        this.defaultService = defaultService;
        this.contenidoRepository = contenidoRepository;
        this.redaccionRepository = redaccionRepository;
        this.combinacionService = combinacionService;
        this.actaRepository = actaRepository;
        this.falloRepository = falloRepository;
        this.pagoRepository = pagoRepository;
    }

    /**
     * Constructor de compatibilidad para tests unitarios de slice 8F-1
     * que no requieren contexto automatico de acta.
     */
    public DocumentoRedaccionService(
            DocumentoRepository documentoRepository,
            DocumentoPlantillaDefaultService defaultService,
            DocumentoPlantillaContenidoRepository contenidoRepository,
            DocumentoRedaccionRepository redaccionRepository,
            DocumentoCombinacionService combinacionService) {
        this(documentoRepository, defaultService, contenidoRepository,
                redaccionRepository, combinacionService, null, null, null);
    }

    public DocumentoRedaccionResponse crearRedaccionDesdePlantilla(CrearRedaccionDocumentoCommand cmd) {
        validarCommand(cmd);

        documentoRepository.buscarPorId(cmd.idDocumento())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.idDocumento()));

        LocalDateTime ahora = LocalDateTime.now();

        FalDocumentoPlantillaDefault plantillaDefault = defaultService.resolverDefault(
                cmd.accionDocumental(), cmd.tipoActa(), cmd.idDependencia(), ahora);

        FalDocumentoPlantillaContenido contenido =
                resolverContenidoVigente(plantillaDefault.getPlantillaId(), ahora);

        Map<String, Object> contexto = cmd.variablesContexto();
        DocumentoCombinacionResultado resultado =
                combinacionService.combinar(contenido.getCuerpoTemplate(), contexto);

        if (!resultado.variablesFaltantes().isEmpty()) {
            throw new PrecondicionVioladaException(
                    "Variable(s) requerida(s) faltante(s): " + resultado.variablesFaltantes());
        }
        if (!resultado.variablesDesconocidas().isEmpty()) {
            throw new PrecondicionVioladaException(
                    "Variable(s) desconocida(s) en el template: " + resultado.variablesDesconocidas());
        }

        String snapshotJson = buildSnapshotJson(contexto);
        String diagnosticoJson = resultado.variablesUsadas().isEmpty()
                ? null
                : "{\"variablesUsadas\":" + toJsonArray(resultado.variablesUsadas()) + "}";

        Long id = redaccionRepository.nextId();
        FalDocumentoRedaccion redaccion = new FalDocumentoRedaccion(
                id, cmd.idDocumento(), contenido.getId(),
                EstadoRedaccionDocumento.BORRADOR,
                resultado.contenidoCombinado(),
                snapshotJson, null, diagnosticoJson,
                ahora, cmd.idUserOperacion(),
                ahora, cmd.idUserOperacion(),
                null, null);

        redaccionRepository.guardar(redaccion);

        return new DocumentoRedaccionResponse(
                redaccion.getId(), redaccion.getIdDocumento(),
                redaccion.getPlantillaContenidoId(), redaccion.getEstadoRedaccion(),
                redaccion.getContenidoEditable(),
                resultado.variablesUsadas(), resultado.variablesFaltantes(),
                resultado.variablesDesconocidas(), resultado.completo());
    }

    /**
     * Crea una redaccion construyendo el contexto automaticamente desde el dominio in-memory.
     *
     * Carga la acta, el documento, el fallo activo (si existe) y el pago voluntario (si existe).
     * Construye el contexto via DocumentoVariableContextBuilder y delega a crearRedaccionDesdePlantilla.
     *
     * Requiere que actaRepository este inicializado.
     *
     * Guardrails:
     *   - No genera PDF.
     *   - No emite documento.
     *   - No envia a firma.
     *   - storageKey/hashDocu/fhGeneracion permanecen null.
     *
     * Slice 8F-2.
     */
    public DocumentoRedaccionResponse crearRedaccionConContextoActa(
            Long idActa,
            Long idDocumento,
            AccionDocumental accionDocumental,
            TipoActa tipoActa,
            Long idDependencia,
            Short verDependencia,
            String idUserOperacion) {

        if (actaRepository == null)
            throw new IllegalStateException("actaRepository requerido para crearRedaccionConContextoActa");

        FalActa acta = actaRepository.buscarPorId(idActa)
                .orElseThrow(() -> new ActaNoEncontradaException(idActa));

        FalDocumento documento = documentoRepository.buscarPorId(idDocumento)
                .orElseThrow(() -> new DocumentoNoEncontradoException(idDocumento));

        FalActaFallo fallo = falloRepository != null
                ? falloRepository.buscarActivo(idActa).orElse(null)
                : null;

        FalPagoVoluntario pago = pagoRepository != null
                ? pagoRepository.buscarPorActa(idActa).orElse(null)
                : null;

        Map<String, Object> contexto =
                DocumentoVariableContextBuilder.buildDesdeActa(acta, documento, fallo, pago, null);

        CrearRedaccionDocumentoCommand cmd = new CrearRedaccionDocumentoCommand(
                idDocumento, idActa, accionDocumental,
                tipoActa, idDependencia, verDependencia,
                idUserOperacion, contexto);

        return crearRedaccionDesdePlantilla(cmd);
    }

    private FalDocumentoPlantillaContenido resolverContenidoVigente(Long plantillaId, LocalDateTime en) {
        List<FalDocumentoPlantillaContenido> contenidos =
                contenidoRepository.buscarContenidoVigente(plantillaId, en);
        if (contenidos.isEmpty()) {
            throw new PlantillaContenidoNoEncontradaException(plantillaId);
        }
        if (contenidos.size() == 1) return contenidos.get(0);
        return contenidos.stream()
                .max(java.util.Comparator.comparingInt(FalDocumentoPlantillaContenido::getVersionContenido))
                .orElseThrow();
    }

    private void validarCommand(CrearRedaccionDocumentoCommand cmd) {
        if (cmd.idDocumento() == null)
            throw new PrecondicionVioladaException("idDocumento es obligatorio");
        if (cmd.idActa() == null)
            throw new PrecondicionVioladaException("idActa es obligatorio");
        if (cmd.accionDocumental() == null)
            throw new PrecondicionVioladaException("accionDocumental es obligatoria");
        if (cmd.idUserOperacion() == null || cmd.idUserOperacion().isBlank())
            throw new PrecondicionVioladaException("idUserOperacion es obligatorio");
    }

    private String buildSnapshotJson(Map<String, Object> variables) {
        if (variables.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : variables.entrySet()) {
            if (!first) sb.append(",");
            sb.append('"').append(e.getKey()).append('"').append(':').append('"')
              .append(e.getValue() == null ? "" : e.getValue().toString().replace("\"", "\\\""))
              .append('"');
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String toJsonArray(Set<String> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String item : items) {
            if (!first) sb.append(",");
            sb.append('"').append(item).append('"');
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}