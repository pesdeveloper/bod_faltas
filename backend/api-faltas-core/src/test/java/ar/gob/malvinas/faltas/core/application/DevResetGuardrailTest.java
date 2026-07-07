package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.demo.DevInMemoryResetService;
import ar.gob.malvinas.faltas.core.web.DevResetController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails estaticos del slice 8F-5.
 *
 * Verifica:
 *  - el endpoint reset esta bajo /demo (no productivo);
 *  - el servicio no extiende clases JDBC/JPA ni implementa interfaces de persistencia real;
 *  - el dataset funcional estatico sigue devolviendo 37 actas;
 *  - el service no contiene SQL ni DROP ni ALTER en su nombre de clase.
 *
 * Slice 8F-5.
 */
@DisplayName("Guardrails 8F-5: endpoint reset dev/test correcto y sin JDBC")
class DevResetGuardrailTest {

    @Test
    @DisplayName("DevResetController esta mapeado bajo /demo")
    void controller_mapeado_bajo_demo() {
        RequestMapping rm = DevResetController.class.getAnnotation(RequestMapping.class);
        assertThat(rm).isNotNull();
        assertThat(rm.value()).hasSize(1);
        assertThat(rm.value()[0]).startsWith("/demo");
    }

    @Test
    @DisplayName("DevInMemoryResetService no extiende clase base (no hay herencia JDBC/JPA)")
    void service_no_extiende_clase_jdbc() {
        assertThat(DevInMemoryResetService.class.getSuperclass()).isEqualTo(Object.class);
    }

    @Test
    @DisplayName("DevInMemoryResetService no implementa interfaces de persistencia real")
    void service_no_implementa_interfaces_repositorio() {
        Class<?>[] interfaces = DevInMemoryResetService.class.getInterfaces();
        for (Class<?> iface : interfaces) {
            assertThat(iface.getName().toLowerCase())
                    .as("El servicio no debe implementar interfaces JDBC/JPA: " + iface.getName())
                    .doesNotContain("repository")
                    .doesNotContain("jdbc")
                    .doesNotContain("jpa");
        }
    }

    @Test
    @DisplayName("DatasetFuncionalDominioCatalog sigue devolviendo 37 actas post-8F-5")
    void dataset_funcional_tiene_37_actas() {
        assertThat(DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones()).hasSize(37);
    }

    @Test
    @DisplayName("El nombre del servicio no contiene terminos SQL prohibidos")
    void service_nombre_no_contiene_sql() {
        String nombre = DevInMemoryResetService.class.getSimpleName().toLowerCase();
        assertThat(nombre).doesNotContain("jdbc");
        assertThat(nombre).doesNotContain("sql");
        assertThat(nombre).doesNotContain("mariadb");
        assertThat(nombre).doesNotContain("jpa");
    }
}
