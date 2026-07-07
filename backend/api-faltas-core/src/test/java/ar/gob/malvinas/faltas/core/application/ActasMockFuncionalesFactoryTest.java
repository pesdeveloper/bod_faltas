package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.ActaMockFuncionalDefinicion;
import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del factory de actas mock funcionales.
 *
 * Valida que cada acta mock del catalogo puede construirse correctamente
 * y que la construccion es deterministica.
 *
 * Slice 8F-4B.
 */
@DisplayName("8F-4B: ActasMockFuncionales - construccion deterministica")
class ActasMockFuncionalesFactoryTest {

    @Test
    @DisplayName("1. Construye todas las actas mock del catalogo sin excepcion")
    void construye_todas_las_actas_sin_excepcion() {
        List<ActaMockFuncionalDefinicion> defs = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        for (int i = 0; i < defs.size(); i++) {
            ActaMockFuncionalDefinicion def = defs.get(i);
            FalActa acta = DatasetFuncionalDominioCatalog.construirActa(def, (long) (100 + i));
            assertThat(acta).as("acta para %s", def.codigo()).isNotNull();
        }
    }

    @Test
    @DisplayName("2. Cada acta construida tiene id coherente con el parametro")
    void cada_acta_tiene_id_coherente() {
        List<ActaMockFuncionalDefinicion> defs = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        for (int i = 0; i < defs.size(); i++) {
            Long expectedId = (long) (200 + i);
            FalActa acta = DatasetFuncionalDominioCatalog.construirActa(defs.get(i), expectedId);
            assertThat(acta.getId())
                .as("id de acta para %s", defs.get(i).codigo())
                .isEqualTo(expectedId);
        }
    }

    @Test
    @DisplayName("3. Cada acta construida tiene nroActa con codigo de definicion")
    void cada_acta_tiene_nro_acta_con_codigo() {
        ActaMockFuncionalDefinicion def = DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-001-LABRADA");
        FalActa acta = DatasetFuncionalDominioCatalog.construirActa(def, 999L);
        assertThat(acta.getNroActa()).contains("ACT-001-LABRADA");
    }

    @Test
    @DisplayName("4. Cada acta construida refleja el bloque de la definicion")
    void cada_acta_refleja_bloque_de_definicion() {
        List<ActaMockFuncionalDefinicion> defs = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        for (int i = 0; i < defs.size(); i++) {
            ActaMockFuncionalDefinicion def = defs.get(i);
            FalActa acta = DatasetFuncionalDominioCatalog.construirActa(def, (long) (300 + i));
            assertThat(acta.getBloqueActual())
                .as("bloque de acta para %s", def.codigo())
                .isEqualTo(def.bloqueEsperado());
        }
    }

    @Test
    @DisplayName("5. Cada acta construida refleja la situacion de la definicion")
    void cada_acta_refleja_situacion_de_definicion() {
        List<ActaMockFuncionalDefinicion> defs = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        for (int i = 0; i < defs.size(); i++) {
            ActaMockFuncionalDefinicion def = defs.get(i);
            FalActa acta = DatasetFuncionalDominioCatalog.construirActa(def, (long) (400 + i));
            assertThat(acta.getSituacionAdministrativa())
                .as("situacion de acta para %s", def.codigo())
                .isEqualTo(def.situacionEsperada());
        }
    }

    @Test
    @DisplayName("6. Cada acta construida refleja el resultado final de la definicion")
    void cada_acta_refleja_resultado_final_de_definicion() {
        List<ActaMockFuncionalDefinicion> defs = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        for (int i = 0; i < defs.size(); i++) {
            ActaMockFuncionalDefinicion def = defs.get(i);
            FalActa acta = DatasetFuncionalDominioCatalog.construirActa(def, (long) (500 + i));
            assertThat(acta.getResultadoFinal())
                .as("resultadoFinal de acta para %s", def.codigo())
                .isEqualTo(def.resultadoFinalEsperado());
        }
    }

    @Test
    @DisplayName("7. La construccion es deterministica: dos construcciones producen actas equivalentes")
    void construccion_es_deterministica() {
        ActaMockFuncionalDefinicion def = DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-006-ANAL-LISTA-FALLO");
        FalActa acta1 = DatasetFuncionalDominioCatalog.construirActa(def, 777L);
        FalActa acta2 = DatasetFuncionalDominioCatalog.construirActa(def, 777L);
        assertThat(acta1.getId()).isEqualTo(acta2.getId());
        assertThat(acta1.getBloqueActual()).isEqualTo(acta2.getBloqueActual());
        assertThat(acta1.getSituacionAdministrativa()).isEqualTo(acta2.getSituacionAdministrativa());
        assertThat(acta1.getResultadoFinal()).isEqualTo(acta2.getResultadoFinal());
    }

    @Test
    @DisplayName("8. Dos construcciones con distinto id no se solapan")
    void dos_construcciones_con_distinto_id_son_independientes() {
        ActaMockFuncionalDefinicion def = DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-001-LABRADA");
        FalActa acta1 = DatasetFuncionalDominioCatalog.construirActa(def, 1L);
        FalActa acta2 = DatasetFuncionalDominioCatalog.construirActa(def, 2L);
        assertThat(acta1.getId()).isNotEqualTo(acta2.getId());
        assertThat(acta1.getUuidTecnico()).isNotEqualTo(acta2.getUuidTecnico());
    }

    @Test
    @DisplayName("9. Construccion por codigo funciona igual que por definicion")
    void construccion_por_codigo_igual_que_por_definicion() {
        FalActa actaPorCodigo = DatasetFuncionalDominioCatalog.construirActa("ACT-009-PAGVOL-CONFIRMADO", 50L);
        ActaMockFuncionalDefinicion def = DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-009-PAGVOL-CONFIRMADO");
        FalActa actaPorDef = DatasetFuncionalDominioCatalog.construirActa(def, 50L);
        assertThat(actaPorCodigo.getId()).isEqualTo(actaPorDef.getId());
        assertThat(actaPorCodigo.getBloqueActual()).isEqualTo(actaPorDef.getBloqueActual());
        assertThat(actaPorCodigo.getSituacionAdministrativa()).isEqualTo(actaPorDef.getSituacionAdministrativa());
        assertThat(actaPorCodigo.getResultadoFinal()).isEqualTo(actaPorDef.getResultadoFinal());
    }

    @Test
    @DisplayName("10. Acta de ACT-009-PAGVOL-CONFIRMADO tiene bloque CERR y situacion CERRADA")
    void acta_PAGO_VOLUNTARIO_PAGADO_esta_cerrada() {
        FalActa acta = DatasetFuncionalDominioCatalog.construirActa("ACT-009-PAGVOL-CONFIRMADO", 9L);
        assertThat(acta.getBloqueActual().codigo()).isEqualTo("CERR");
        assertThat(acta.getSituacionAdministrativa().name()).isEqualTo("CERRADA");
        assertThat(acta.getResultadoFinal().name()).isEqualTo("PAGO_VOLUNTARIO_PAGADO");
        assertThat(acta.estaCerrada()).isTrue();
    }

    @Test
    @DisplayName("11. Acta de ACT-020-PARALIZADA tiene situacion PARALIZADA")
    void acta_paralizada_tiene_situacion_paralizada() {
        FalActa acta = DatasetFuncionalDominioCatalog.construirActa("ACT-020-PARALIZADA", 20L);
        assertThat(acta.estaParalizada()).isTrue();
    }
}