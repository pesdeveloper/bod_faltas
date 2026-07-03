package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.result.DatasetFuncionalCoberturaResultado;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dev/demo del dataset funcional completo del dominio in-memory.
 *
 * GET /demo/actas/dataset-funcional devuelve la matriz de cobertura funcional:
 * - total de actas mock
 * - casos de uso cubiertos
 * - documentos esperados
 * - casos pendientes
 * - advertencias
 *
 * Uso exclusivo en perfil de desarrollo y demo funcional.
 * No modifica estado. No ejecuta flujos. Solo lectura del catalogo.
 *
 * Slice 8F-4B.
 */
@RestController
@RequestMapping("/demo")
public class DatasetFuncionalDemoController {

    @GetMapping("/actas/dataset-funcional")
    public ResponseEntity<DatasetFuncionalCoberturaResultado> obtenerDatasetFuncional() {
        return ResponseEntity.ok(DatasetFuncionalDominioCatalog.calcularCobertura());
    }
}
