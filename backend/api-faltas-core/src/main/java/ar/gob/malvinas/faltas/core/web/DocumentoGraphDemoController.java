package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.result.DocumentoGraphDemoResultado;
import ar.gob.malvinas.faltas.core.application.service.DocumentoGraphDemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dev/demo del graph demo documental completo.
 *
 * GET /demo/documentos/graph ejecuta los 8 casos operativos de punta a punta
 * y devuelve el resultado navegable con metadatos mock.
 *
 * Uso exclusivo en perfil de desarrollo y demo funcional.
 * No disponible en produccion.
 *
 * Slice 8F-4.
 */
@RestController
@RequestMapping("/demo")
public class DocumentoGraphDemoController {

    private final DocumentoGraphDemoService graphDemoService;

    public DocumentoGraphDemoController(DocumentoGraphDemoService graphDemoService) {
        this.graphDemoService = graphDemoService;
    }

    @GetMapping("/documentos/graph")
    public ResponseEntity<DocumentoGraphDemoResultado> ejecutarGraphDemo() {
        return ResponseEntity.ok(graphDemoService.ejecutar());
    }
}
