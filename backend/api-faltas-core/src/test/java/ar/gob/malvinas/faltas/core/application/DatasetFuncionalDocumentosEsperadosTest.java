package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.ActaMockFuncionalDefinicion;
import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.demo.DocumentoEsperadoPorActaMock;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de los documentos esperados por acta mock.
 *
 * Valida que cada documento esperado usa enums validos del dominio
 * y que las actas con fallo/notificacion/pago declaran los documentos correctos.
 *
 * Slice 8F-4B.
 */
@DisplayName("8F-4B: DatasetFuncionalDocumentosEsperados - validaciones documentales")
class DatasetFuncionalDocumentosEsperadosTest {

    private static final Set<AccionDocumental> ACCIONES_CONOCIDAS =
            Set.of(AccionDocumental.values());

    private static final Set<TipoDocu> TIPOS_CONOCIDOS =
            Set.of(TipoDocu.values());

    // Acciones con plantilla en PlantillasMockSeeder
    private static final Set<AccionDocumental> ACCIONES_CON_PLANTILLA_MOCK = Set.of(
            AccionDocumental.EMITIR_FALLO,
            AccionDocumental.EMITIR_NOTIFICACION_ACTA,
            AccionDocumental.EMITIR_NOTIFICACION_FALLO,
            AccionDocumental.EMITIR_INTIMACION_PAGO,
            AccionDocumental.EMITIR_MEDIDA_PREVENTIVA,
            AccionDocumental.EMITIR_CONSTANCIA,
            AccionDocumental.EMITIR_ANEXO,
            AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE
    );

    @Test
    @DisplayName("1. Ningun documento esperado tiene AccionDocumental nula")
    void no_hay_documento_con_accion_null() {
        todosLosDocumentosEsperados().forEach(doc ->
            assertThat(doc.accionDocumental())
                .as("accionDocumental no puede ser null")
                .isNotNull()
        );
    }

    @Test
    @DisplayName("2. Ningun documento esperado tiene TipoDocu nulo")
    void no_hay_documento_con_tipo_docu_null() {
        todosLosDocumentosEsperados().forEach(doc ->
            assertThat(doc.tipoDocu())
                .as("tipoDocu no puede ser null")
                .isNotNull()
        );
    }

    @Test
    @DisplayName("3. Cada documento esperado usa una AccionDocumental conocida del dominio")
    void cada_documento_usa_accion_documental_conocida() {
        todosLosDocumentosEsperados().forEach(doc ->
            assertThat(ACCIONES_CONOCIDAS)
                .as("accionDocumental %s debe ser conocida", doc.accionDocumental())
                .contains(doc.accionDocumental())
        );
    }

    @Test
    @DisplayName("4. Cada documento esperado usa un TipoDocu conocido del dominio")
    void cada_documento_usa_tipo_docu_conocido() {
        todosLosDocumentosEsperados().forEach(doc ->
            assertThat(TIPOS_CONOCIDOS)
                .as("tipoDocu %s debe ser conocido", doc.tipoDocu())
                .contains(doc.tipoDocu())
        );
    }

    @Test
    @DisplayName("5. Actas con requiereFallo=true declaran al menos un EMITIR_FALLO")
    void actas_con_fallo_declaran_documento_fallo() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().stream()
                .filter(ActaMockFuncionalDefinicion::requiereFallo)
                .forEach(def -> {
                    boolean tieneFallo = def.documentosEsperados().stream()
                            .anyMatch(d -> d.accionDocumental() == AccionDocumental.EMITIR_FALLO);
                    assertThat(tieneFallo)
                        .as("acta %s requiereFallo=true debe declarar EMITIR_FALLO", def.codigo())
                        .isTrue();
                });
    }

    @Test
    @DisplayName("6. Actas con requiereNotificacion=true declaran al menos un documento de notificacion")
    void actas_con_notificacion_declaran_documento_notificacion() {
        Set<AccionDocumental> accionesNotificacion = Set.of(
                AccionDocumental.EMITIR_NOTIFICACION_ACTA,
                AccionDocumental.EMITIR_NOTIFICACION_FALLO
        );
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().stream()
                .filter(ActaMockFuncionalDefinicion::requiereNotificacion)
                .forEach(def -> {
                    boolean tieneNotif = def.documentosEsperados().stream()
                            .anyMatch(d -> accionesNotificacion.contains(d.accionDocumental()));
                    assertThat(tieneNotif)
                        .as("acta %s requiereNotificacion=true debe declarar documento de notificacion", def.codigo())
                        .isTrue();
                });
    }

    @Test
    @DisplayName("7. Actas con condena (requiereFallo=true Y requierePago=true) declaran EMITIR_INTIMACION_PAGO")
    void actas_con_condena_y_pago_declaran_intimacion() {
        // Solo los escenarios de pago de CONDENA requieren EMITIR_INTIMACION_PAGO.
        // Pago VOLUNTARIO (requiereFallo=false, requierePago=true) no lo requiere.
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().stream()
                .filter(def -> def.requierePago() && def.requiereFallo() && !def.documentosEsperados().isEmpty())
                .forEach(def -> {
                    boolean tieneIntimacion = def.documentosEsperados().stream()
                            .anyMatch(d -> d.accionDocumental() == AccionDocumental.EMITIR_INTIMACION_PAGO);
                    assertThat(tieneIntimacion)
                        .as("acta %s condena+pago debe declarar EMITIR_INTIMACION_PAGO", def.codigo())
                        .isTrue();
                });
    }

    @Test
    @DisplayName("8. Actas con requiereResolutorioBloqueante=true declaran EMITIR_RESOLUTORIO_BLOQUEANTE")
    void actas_con_resolutorio_bloqueante_declaran_documento() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().stream()
                .filter(ActaMockFuncionalDefinicion::requiereResolutorioBloqueante)
                .forEach(def -> {
                    boolean tieneResolutorio = def.documentosEsperados().stream()
                            .anyMatch(d -> d.accionDocumental() == AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE);
                    assertThat(tieneResolutorio)
                        .as("acta %s requiereResolutorioBloqueante=true debe declarar EMITIR_RESOLUTORIO_BLOQUEANTE", def.codigo())
                        .isTrue();
                });
    }

    @Test
    @DisplayName("9. Documentos con requiereRedaccion=true usan AccionDocumental con plantilla en PlantillasMockSeeder")
    void documentos_con_redaccion_tienen_plantilla_disponible() {
        todosLosDocumentosEsperados().stream()
                .filter(DocumentoEsperadoPorActaMock::requiereRedaccion)
                .forEach(doc ->
                    assertThat(ACCIONES_CON_PLANTILLA_MOCK)
                        .as("accion %s debe tener plantilla mock disponible", doc.accionDocumental())
                        .contains(doc.accionDocumental())
                );
    }

    @Test
    @DisplayName("10. Todos los momentoFuncional declarados en documentos esperados son no vacios")
    void momentos_funcionales_no_vacios() {
        todosLosDocumentosEsperados().forEach(doc ->
            assertThat(doc.momentoFuncional())
                .as("momentoFuncional de %s/%s no puede ser vacio", doc.accionDocumental(), doc.tipoDocu())
                .isNotNull().isNotBlank()
        );
    }

    @Test
    @DisplayName("11. Todas las observaciones de documentos esperados son no vacias")
    void observaciones_documentos_no_vacias() {
        todosLosDocumentosEsperados().forEach(doc ->
            assertThat(doc.observacion())
                .as("observacion de %s/%s no puede ser vacia", doc.accionDocumental(), doc.tipoDocu())
                .isNotNull().isNotBlank()
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static List<DocumentoEsperadoPorActaMock> todosLosDocumentosEsperados() {
        return DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().stream()
                .flatMap(def -> def.documentosEsperados().stream())
                .toList();
    }
}
