package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Regla pendiente: resolutorio material expreso habilita solo el material
 * correspondiente, no todos.
 *
 * <p>Ejemplo esperado:
 * <ul>
 *   <li>Registrar resolutorio para {@code LIBERACION_RODADO}.</li>
 *   <li>Verificar que puede cumplirse/liberarse rodado.</li>
 *   <li>Verificar que {@code ENTREGA_DOCUMENTACION} sigue no ejecutable
 *       si no tiene resolutorio propio ni resultado habilitante general.</li>
 * </ul>
 *
 * <p>Condicion de activacion: el modelo debe permitir representar un
 * resolutorio material expreso emitido por autoridad competente, conceptualmente
 * separado del reingreso externo. Eventos candidatos:
 * {@code RESOLUTORIO_MATERIAL_LIBERACION_RODADO},
 * {@code RESOLUTORIO_MATERIAL_ENTREGA_DOCUMENTACION}, etc.
 *
 * <p>Pendiente de implementacion: se requiere un endpoint o mecanismo mock
 * para registrar el resolutorio material expreso por tipo puntual antes de
 * implementar este test.
 */
@Disabled("Pendiente: requiere modelar resolutorio material expreso por tipo individual (ACTA-0024 slice)")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ResolutorioMaterialExpresoHabilitaSoloMaterialCorrespondienteIT {

    @Test
    void resolutorioRodado_habilitaSoloRodado_noDocumentacion() {
        // TODO: registrar RESOLUTORIO_MATERIAL_LIBERACION_RODADO para el acta de prueba
        // TODO: verificar que cumplimiento de LIBERACION_RODADO devuelve OK
        // TODO: verificar que ENTREGA_DOCUMENTACION sigue devolviendo 409 sin resolutorio ni habilitante
        throw new UnsupportedOperationException("Test pendiente de implementacion");
    }
}
