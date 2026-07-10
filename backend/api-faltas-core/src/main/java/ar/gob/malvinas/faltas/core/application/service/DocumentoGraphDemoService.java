package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarRedaccionYGenerarDocumentoMockCommand;
import ar.gob.malvinas.faltas.core.application.demo.GraphDemoActaFactory;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGeneracionMockResponse;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGraphDemoCasoResultado;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGraphDemoResultado;
import ar.gob.malvinas.faltas.core.application.result.DocumentoRedaccionResponse;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestador del graph demo documental completo.
 *
 * Ejecuta los 8 casos operativos principales de punta a punta:
 *   acta demo -> contexto -> redaccion BORRADOR -> confirmacion -> generacion mock final.
 *
 * Cada ejecucion crea una acta demo fresca (id generado por nextId()).
 * Dos ejecuciones son independientes entre si.
 *
 * Guardrails:
 *   - No emite documentos automaticamente.
 *   - No firma.
 *   - No notifica.
 *   - Toda generacion usa mock PDF deterministico.
 *   - storageKey con esquema mock://
 *   - hashDocu con prefijo sha256-mock-
 *
 * Slice 8F-4.
 */
@Service
public class DocumentoGraphDemoService {

    private record CasoDefinicion(
            String codigo,
            String descripcion,
            AccionDocumental accion,
            TipoDocu tipoDocu) {}

    private static final List<CasoDefinicion> CASOS = List.of(
            new CasoDefinicion(
                    "CASO-01", "Fallo condenatorio - acto administrativo",
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO),
            new CasoDefinicion(
                    "CASO-02", "Notificacion de acta",
                    AccionDocumental.EMITIR_NOTIFICACION_ACTA, TipoDocu.NOTIFICACION_ACTA),
            new CasoDefinicion(
                    "CASO-03", "Notificacion de fallo",
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO),
            new CasoDefinicion(
                    "CASO-04", "Intimacion de pago",
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO),
            new CasoDefinicion(
                    "CASO-05", "Medida preventiva",
                    AccionDocumental.EMITIR_MEDIDA_PREVENTIVA, TipoDocu.MEDIDA_PREVENTIVA),
            new CasoDefinicion(
                    "CASO-06", "Constancia",
                    AccionDocumental.EMITIR_CONSTANCIA, TipoDocu.CONSTANCIA),
            new CasoDefinicion(
                    "CASO-07", "Anexo",
                    AccionDocumental.EMITIR_ANEXO, TipoDocu.ANEXO),
            new CasoDefinicion(
                    "CASO-08", "Resolutorio bloqueante",
                    AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE, TipoDocu.RESOLUTORIO_BLOQUEANTE)
    );

    private final ActaRepository actaRepository;
    private final FalloActaRepository falloRepository;
    private final PagoVoluntarioRepository pagoRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoRedaccionService redaccionService;
    private final DocumentoGeneracionMockService generacionService;
    private final FaltasClock faltasClock;

    public DocumentoGraphDemoService(
            ActaRepository actaRepository,
            FalloActaRepository falloRepository,
            PagoVoluntarioRepository pagoRepository,
            DocumentoRepository documentoRepository,
            DocumentoRedaccionService redaccionService,
            DocumentoGeneracionMockService generacionService,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.falloRepository = falloRepository;
        this.pagoRepository = pagoRepository;
        this.documentoRepository = documentoRepository;
        this.redaccionService = redaccionService;
        this.generacionService = generacionService;
    }

    public DocumentoGraphDemoResultado ejecutar() {
        LocalDateTime fhInicio = faltasClock.now();

        FalActa acta = actaRepository.guardar(
                GraphDemoActaFactory.crearActaDemo(actaRepository.nextId()));
        falloRepository.guardar(GraphDemoActaFactory.crearFalloCondenatorioDemo(acta.getId(), falloRepository.nextId()));
        pagoRepository.guardar(GraphDemoActaFactory.crearPagoVoluntarioDemo(acta.getId()));

        List<DocumentoGraphDemoCasoResultado> resultados = new ArrayList<>();
        for (CasoDefinicion caso : CASOS) {
            resultados.add(ejecutarCaso(caso, acta.getId(), fhInicio));
        }

        return DocumentoGraphDemoResultado.de(resultados, fhInicio);
    }

    private DocumentoGraphDemoCasoResultado ejecutarCaso(CasoDefinicion caso, Long actaId, LocalDateTime ahora) {
        try {
            Long docId = documentoRepository.nextId();
            FalDocumento doc = GraphDemoActaFactory.crearDocumentoDemo(docId, actaId, caso.tipoDocu(), ahora);
            documentoRepository.guardar(doc);

            DocumentoRedaccionResponse borrador = redaccionService.crearRedaccionConContextoActa(
                    actaId, docId, caso.accion(), null, null, null, "usr-demo");

            DocumentoGeneracionMockResponse generacion = generacionService.confirmarYGenerarMockPdf(
                    new ConfirmarRedaccionYGenerarDocumentoMockCommand(
                            borrador.id(), "usr-demo-confirm", null));

            return DocumentoGraphDemoCasoResultado.exitoso(
                    caso.codigo(), caso.descripcion(), caso.accion(), caso.tipoDocu(),
                    actaId, docId, borrador.id(),
                    generacion.estadoRedaccion(), borrador.completo(),
                    generacion.storageKey(), generacion.hashDocu(), generacion.fhGeneracion(),
                    generacion.mock());

        } catch (Exception e) {
            return DocumentoGraphDemoCasoResultado.fallido(
                    caso.codigo(), caso.descripcion(), caso.accion(), caso.tipoDocu(),
                    actaId, e.getMessage());
        }
    }
}