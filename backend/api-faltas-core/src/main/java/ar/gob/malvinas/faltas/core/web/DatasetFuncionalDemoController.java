package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.result.DatasetFuncionalCoberturaResultado;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dev/demo del dataset funcional completo del dominio.
 *
 * GET /demo/actas/dataset-funcional devuelve la matriz de cobertura funcional
 * ({@link DatasetFuncionalDominioCatalog}, catálogo estático, sin persistencia).
 *
 * <p>Wave 4 Parte 3E-B: {@code GET /demo/actas/{codigo}} (drill-down de un acta mock vía
 * materialización real con {@code CasoUsoFuncionalRunner}) se retiró de este controller y de
 * todo {@code src/main}. Esa ruta dependía de un composition root manual con repositorios
 * InMemory propios, ajeno al artefacto productivo; {@code CasoUsoFuncionalRunner} y su
 * materializador quedan preservados exclusivamente como fixture de test
 * (`src/test/.../application/demo/CasoUsoFuncionalRunner.java`). Si en el futuro se necesita un
 * dataset mock materializado para el frontend, debe generarse como fixture estático o utilidad
 * de test/dev fuera del artefacto productivo, no mediante un servicio Spring de {@code src/main}.
 *
 * Slice 8F-4B.
 */
@ConditionalOnProperty(name = "faltas.demo.enabled", havingValue = "true")
@RestController
@RequestMapping("/demo")
public class DatasetFuncionalDemoController {

    @GetMapping("/actas/dataset-funcional")
    public ResponseEntity<DatasetFuncionalCoberturaResultado> obtenerDatasetFuncional() {
        return ResponseEntity.ok(DatasetFuncionalDominioCatalog.calcularCobertura());
    }
}
