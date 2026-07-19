package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.CasoUsoFuncionalRunner;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que CasoUsoFuncionalRunner restaura el actor previo al terminar,
 * incluso cuando el flujo lanza una excepcion.
 *
 * El actor "runner-funcional" es tecnico del runner de pruebas, no un actor
 * humano ni un fallback productivo.
 */
@DisplayName("CasoUsoFuncionalRunner — restauracion de actor")
class CasoUsoFuncionalRunnerActorTest {

    private CasoUsoFuncionalRunner runner;

    @BeforeEach
    void setUp() {
        runner = new CasoUsoFuncionalRunner();
    }

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Test
    @DisplayName("01. Sin contexto previo -> ActorContextHolder queda limpio al terminar")
    void sin_contexto_previo_queda_limpio() {
        assertThat(ActorContextHolder.get()).isNull();

        runner.ejecutar("ACT-001-LABRADA");

        assertThat(ActorContextHolder.get())
                .as("Sin contexto previo, ActorContextHolder debe quedar null al terminar")
                .isNull();
    }

    @Test
    @DisplayName("02. Con contexto previo -> se restaura exactamente el mismo contexto")
    void con_contexto_previo_se_restaura() {
        ActorContext previo = new ActorContext("actor-pre-existente");
        ActorContextHolder.set(previo);

        runner.ejecutar("ACT-001-LABRADA");

        ActorContext restaurado = ActorContextHolder.get();
        assertThat(restaurado)
                .as("Debe restaurarse el contexto previo")
                .isNotNull();
        assertThat(restaurado.sub())
                .as("El sub del contexto restaurado debe ser el original")
                .isEqualTo("actor-pre-existente");
    }

    @Test
    @DisplayName("03. Flujo con excepcion -> contexto previo se restaura igualmente")
    void flujo_con_excepcion_restaura_contexto_previo() {
        ActorContext previo = new ActorContext("actor-antes-de-error");
        ActorContextHolder.set(previo);

        // Codigo inexistente: el runner captura la excepcion y retorna parcial,
        // pero debe restaurar el contexto en finally.
        runner.ejecutar("CODIGO-INEXISTENTE-FUERZA-ERROR");

        ActorContext restaurado = ActorContextHolder.get();
        assertThat(restaurado)
                .as("Ante excepcion interna, debe restaurarse el contexto previo")
                .isNotNull();
        assertThat(restaurado.sub())
                .as("El sub del contexto debe ser el actor previo al runner")
                .isEqualTo("actor-antes-de-error");
    }

    @Test
    @DisplayName("04. Sin contexto previo y excepcion interna -> queda limpio")
    void sin_contexto_previo_excepcion_queda_limpio() {
        assertThat(ActorContextHolder.get()).isNull();

        runner.ejecutar("CODIGO-INEXISTENTE-FUERZA-ERROR");

        assertThat(ActorContextHolder.get())
                .as("Sin contexto previo, incluso ante excepcion, debe quedar null")
                .isNull();
    }
}
