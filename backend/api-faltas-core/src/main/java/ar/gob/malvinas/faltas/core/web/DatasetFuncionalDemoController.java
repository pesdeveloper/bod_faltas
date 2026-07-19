package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.demo.DemoActaMaterializadorService;
import ar.gob.malvinas.faltas.core.application.result.DatasetFuncionalCoberturaResultado;
import ar.gob.malvinas.faltas.core.web.dto.DemoActaDetalleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dev/demo del dataset funcional completo del dominio in-memory.
 *
 * GET /demo/actas/dataset-funcional devuelve la matriz de cobertura funcional.
 * GET /demo/actas/{codigo} devuelve el drill-down de un acta mock especifica.
 *
 * Slice 8F-4B / Slice 8F-7.
 */
@ConditionalOnProperty(name = "faltas.demo.enabled", havingValue = "true")
@RestController
@RequestMapping("/demo")
public class DatasetFuncionalDemoController {

    private final DemoActaMaterializadorService materializadorService;

    public DatasetFuncionalDemoController(DemoActaMaterializadorService materializadorService) {
        this.materializadorService = materializadorService;
    }

    @GetMapping("/actas/dataset-funcional")
    public ResponseEntity<DatasetFuncionalCoberturaResultado> obtenerDatasetFuncional() {
        return ResponseEntity.ok(DatasetFuncionalDominioCatalog.calcularCobertura());
    }

    @GetMapping("/actas/{codigo}")
    public ResponseEntity<DemoActaDetalleResponse> obtenerActaDetalle(@PathVariable String codigo) {
        return ResponseEntity.ok(materializadorService.materializar(codigo));
    }
}
