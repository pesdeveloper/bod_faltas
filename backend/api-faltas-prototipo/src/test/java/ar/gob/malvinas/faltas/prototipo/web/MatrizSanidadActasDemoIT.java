package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de sanidad funcional de todas las actas demo.
 *
 * <p>No ejecuta flujos completos. Detecta actas muertas, inconsistentes o con estado
 * incoherente tras el reset inicial.
 *
 * <p>Resultado esperado post-reset: 44 actas, 0 advertencias, 0 errores.
 *
 * <p>Reglas cubiertas:
 * <ol>
 *   <li>Toda acta tiene bandejaActual, estadoProcesoActual y situacionAdministrativaActual.</li>
 *   <li>Actas cerradas (CERRADAS o estaCerrada) no tienen documentos PENDIENTE_FIRMA.</li>
 *   <li>Actas en PENDIENTE_FIRMA tienen al menos un documento PENDIENTE_FIRMA y no están cerradas.</li>
 *   <li>Actas en PENDIENTE_NOTIFICACION o EN_NOTIFICACION tienen notificaciones y al menos un doc FIRMADO.</li>
 *   <li>Actas en PENDIENTES_RESOLUCION_REDACCION tienen piezasRequeridas o piezasGeneradas.</li>
 *   <li>Actas en ACTAS_EN_ENRIQUECIMIENTO o PENDIENTE_PREPARACION_DOCUMENTAL sin piezas ni
 *       accionPendiente: error (acta sin camino operativo visible).</li>
 *   <li>Actas en GESTION_EXTERNA tienen tipoGestionExterna informado.</li>
 *   <li>Actas en ARCHIVO tienen motivoArchivo informado.</li>
 *   <li>Actas en PARALIZADAS tienen accionPendiente informado.</li>
 * </ol>
 *
 * <p>Actas con accionPendiente explícita para cumplir regla 6:
 * <ul>
 *   <li>ACTA-0002 (PENDIENTE_PREPARACION_DOCUMENTAL): GENERAR_BORRADOR_ACTA — hito D3 post-enriquecimiento.</li>
 *   <li>ACTA-0024 (ACTAS_EN_ENRIQUECIMIENTO/D1): COMPLETAR_ENRIQUECIMIENTO — constatación material temprana con anclas.</li>
 *   <li>ACTA-0123 (ACTAS_EN_ENRIQUECIMIENTO/D2): COMPLETAR_ENRIQUECIMIENTO — captura inicial Inspecciones.</li>
 *   <li>ACTA-0124 (ACTAS_EN_ENRIQUECIMIENTO/D2): COMPLETAR_ENRIQUECIMIENTO — captura inicial Fiscalización.</li>
 *   <li>ACTA-0125 (ACTAS_EN_ENRIQUECIMIENTO/D2): COMPLETAR_ENRIQUECIMIENTO — captura inicial Bromatología.</li>
 * </ul>
 */
@SuppressWarnings({"null", "unchecked"})
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class MatrizSanidadActasDemoIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Test principal ────────────────────────────────────────────────────────

    @Test
    void matrizSanidadFuncional_todasLasActasDemo() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        Set<String> actaIds = descubrirTodasLasActas();

        List<FilaMatriz> matriz = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        for (String actaId : actaIds) {
            FilaMatriz fila = buildFila(actaId, errores, advertencias);
            matriz.add(fila);
        }

        imprimirMatriz(matriz, errores, advertencias);

        if (!advertencias.isEmpty()) {
            fail("Matriz de sanidad detectó " + advertencias.size() + " advertencia(s) en "
                    + matriz.size() + " actas:\n" + String.join("\n", advertencias)
                    + "\nTodas las actas deben tener camino operativo visible (accionPendiente, piezasRequeridas o piezasGeneradas).");
        }
        if (!errores.isEmpty()) {
            fail("Matriz de sanidad detectó " + errores.size() + " error(es) en "
                    + matriz.size() + " actas:\n" + String.join("\n", errores));
        }
    }

    // ── Descubrimiento de actas ───────────────────────────────────────────────

    private Set<String> descubrirTodasLasActas() throws Exception {
        String bandejasJson = mvc.perform(get(B + "/bandejas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> bandejas = objectMapper.readValue(
                bandejasJson, new TypeReference<>() {});

        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> bandeja : bandejas) {
            String codigo = str(bandeja, "codigo");
            if (codigo == null) continue;
            String actasJson = mvc.perform(get(B + "/bandejas/" + codigo + "/actas"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<Map<String, Object>> items = objectMapper.readValue(
                    actasJson, new TypeReference<>() {});
            for (Map<String, Object> item : items) {
                String id = str(item, "id");
                if (id != null) ids.add(id);
            }
        }
        return ids;
    }

    // ── Construcción de fila ──────────────────────────────────────────────────

    private FilaMatriz buildFila(
            String actaId, List<String> errores, List<String> advertencias) throws Exception {

        String detalleJson = mvc.perform(get(B + "/actas/" + actaId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> d = objectMapper.readValue(detalleJson, new TypeReference<>() {});

        String docsJson = mvc.perform(get(B + "/actas/" + actaId + "/documentos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> docs = objectMapper.readValue(docsJson, new TypeReference<>() {});

        String numeroActa        = str(d, "numeroActa");
        String bandeja           = str(d, "bandejaActual");
        String estadoProceso     = str(d, "estadoProcesoActual");
        String situacionAdmin    = str(d, "situacionAdministrativaActual");
        String bloque            = str(d, "bloqueActual");
        boolean estaCerrada      = bool(d, "estaCerrada");
        boolean permiteReingreso = bool(d, "permiteReingreso");
        String accionPendiente   = str(d, "accionPendiente");
        String motivoArchivo     = str(d, "motivoArchivo");
        String tipoGestionExterna = str(d, "tipoGestionExterna");

        List<String> piezasRequeridas  = strList(d, "piezasRequeridas");
        List<String> piezasGeneradas   = strList(d, "piezasGeneradas");
        List<Map<String, Object>> notificaciones = mapList(d, "notificaciones");

        Map<String, Object> cerrabilidad = (Map<String, Object>) d.get("cerrabilidad");
        boolean cerrable      = cerrabilidad != null && Boolean.TRUE.equals(cerrabilidad.get("cerrable"));
        String resultadoFinal = cerrabilidad != null ? str(cerrabilidad, "resultadoFinal") : null;

        aplicarReglas(actaId, numeroActa, bandeja, estadoProceso, situacionAdmin,
                estaCerrada, piezasRequeridas, piezasGeneradas,
                docs, notificaciones, accionPendiente, motivoArchivo, tipoGestionExterna,
                errores, advertencias);

        long docsPF  = docs.stream().filter(doc -> "PENDIENTE_FIRMA".equals(doc.get("estadoDocumento"))).count();
        long docsFrm = docs.stream().filter(doc -> "FIRMADO".equals(doc.get("estadoDocumento"))).count();

        String accionesResumen = resumirAcciones(
                d, bandeja, piezasRequeridas, piezasGeneradas, docs, notificaciones);

        return new FilaMatriz(
                actaId, numeroActa, bandeja, estadoProceso, bloque, situacionAdmin,
                docs.size(), (int) docsPF, (int) docsFrm,
                piezasRequeridas.size(), piezasGeneradas.size(), notificaciones.size(),
                accionPendiente, tipoGestionExterna, motivoArchivo,
                permiteReingreso, cerrable, resultadoFinal, accionesResumen);
    }

    // ── Reglas de sanidad ─────────────────────────────────────────────────────

    private void aplicarReglas(
            String actaId, String numeroActa, String bandeja, String estadoProceso,
            String situacionAdmin, boolean estaCerrada,
            List<String> piezasRequeridas, List<String> piezasGeneradas,
            List<Map<String, Object>> docs, List<Map<String, Object>> notificaciones,
            String accionPendiente, String motivoArchivo, String tipoGestionExterna,
            List<String> errores, List<String> advertencias) {

        String ctx = actaId + " (bandeja=" + bandeja + ")";

        // Regla 1: campos básicos obligatorios
        if (isBlank(bandeja)) {
            errores.add(ctx + ": bandejaActual vacía o nula");
        }
        if (isBlank(estadoProceso)) {
            errores.add(ctx + ": estadoProcesoActual vacío o nulo");
        }
        if (isBlank(situacionAdmin)) {
            errores.add(ctx + ": situacionAdministrativaActual vacía o nula");
        }
        if (isBlank(bandeja)) return; // sin bandeja las demás reglas no aplican

        boolean tieneDocPF  = docs.stream().anyMatch(doc -> "PENDIENTE_FIRMA".equals(doc.get("estadoDocumento")));
        boolean tieneDocFrm = docs.stream().anyMatch(doc -> "FIRMADO".equals(doc.get("estadoDocumento")));

        // Regla 2: cerradas no deben tener docs PENDIENTE_FIRMA
        if (estaCerrada || "CERRADAS".equals(bandeja)) {
            if (tieneDocPF) {
                errores.add(ctx + ": cerrada pero tiene documentos PENDIENTE_FIRMA");
            }
            if (!piezasRequeridas.isEmpty()) {
                advertencias.add(ctx + ": cerrada con piezasRequeridas no vacías: " + piezasRequeridas);
            }
        }

        // Regla 3: PENDIENTE_FIRMA debe tener al menos 1 doc PENDIENTE_FIRMA
        if ("PENDIENTE_FIRMA".equals(bandeja)) {
            if (!tieneDocPF) {
                errores.add(ctx + ": en PENDIENTE_FIRMA sin documentos PENDIENTE_FIRMA"
                        + " — estados actuales: " + estadosDocumentos(docs));
            }
            if (estaCerrada) {
                errores.add(ctx + ": en PENDIENTE_FIRMA pero estaCerrada=true");
            }
        }

        // Regla 4: PENDIENTE_NOTIFICACION / EN_NOTIFICACION
        if ("PENDIENTE_NOTIFICACION".equals(bandeja) || "EN_NOTIFICACION".equals(bandeja)) {
            if (notificaciones.isEmpty()) {
                errores.add(ctx + ": en " + bandeja + " sin notificaciones");
            }
            if (!tieneDocFrm) {
                errores.add(ctx + ": en " + bandeja + " sin documentos FIRMADO"
                        + " — estados actuales: " + estadosDocumentos(docs));
            }
            if (estaCerrada) {
                errores.add(ctx + ": en " + bandeja + " pero estaCerrada=true");
            }
        }

        // Regla 5: PENDIENTES_RESOLUCION_REDACCION debe tener piezas
        if ("PENDIENTES_RESOLUCION_REDACCION".equals(bandeja)) {
            if (piezasRequeridas.isEmpty() && piezasGeneradas.isEmpty()) {
                errores.add(ctx + ": en PENDIENTES_RESOLUCION_REDACCION sin piezasRequeridas ni piezasGeneradas"
                        + " (acta sin camino documental)");
            }
        }

        // Regla 6: enriquecimiento sin camino operativo → advertencia
        if ("ACTAS_EN_ENRIQUECIMIENTO".equals(bandeja) || "PENDIENTE_PREPARACION_DOCUMENTAL".equals(bandeja)) {
            if (piezasRequeridas.isEmpty() && piezasGeneradas.isEmpty() && isBlank(accionPendiente)) {
                advertencias.add(ctx + ": sin piezasRequeridas, piezasGeneradas ni accionPendiente"
                        + " — posible acta sin camino operativo visible");
            }
        }

        // Regla 7: GESTION_EXTERNA debe tener tipoGestionExterna
        if ("GESTION_EXTERNA".equals(bandeja)) {
            if (isBlank(tipoGestionExterna)) {
                errores.add(ctx + ": en GESTION_EXTERNA sin tipoGestionExterna");
            }
        }

        // Regla 8: ARCHIVO debe tener motivoArchivo
        if ("ARCHIVO".equals(bandeja)) {
            if (isBlank(motivoArchivo)) {
                errores.add(ctx + ": en ARCHIVO sin motivoArchivo");
            }
        }

        // Regla 9: PARALIZADAS debe tener accionPendiente o motivo explícito
        if ("PARALIZADAS".equals(bandeja)) {
            if (isBlank(accionPendiente)) {
                errores.add(ctx + ": en PARALIZADAS sin accionPendiente ni motivo de paralización");
            }
        }
    }

    // ── Resumen de acciones ───────────────────────────────────────────────────

    private String resumirAcciones(
            Map<String, Object> d, String bandeja,
            List<String> piezasReq, List<String> piezasGen,
            List<Map<String, Object>> docs, List<Map<String, Object>> notificaciones) {

        List<String> acc = new ArrayList<>();
        if ("PENDIENTES_RESOLUCION_REDACCION".equals(bandeja) && (!piezasReq.isEmpty() || !piezasGen.isEmpty())) {
            acc.add("redaccion");
        }
        if ("PENDIENTE_FIRMA".equals(bandeja)) {
            acc.add("firmaPendiente");
        }
        if ("PENDIENTE_NOTIFICACION".equals(bandeja) || "EN_NOTIFICACION".equals(bandeja)) {
            acc.add("notificacion");
        }
        if ("GESTION_EXTERNA".equals(bandeja)) {
            acc.add("gestionExterna");
        }
        if ("ARCHIVO".equals(bandeja)) {
            acc.add("archivoReingreso");
        }
        if ("PARALIZADAS".equals(bandeja)) {
            acc.add("paralizada");
        }
        if ("PENDIENTES_FALLO".equals(bandeja)) {
            acc.add("falloFondo");
        }
        if ("CON_APELACION".equals(bandeja)) {
            acc.add("apelacion");
        }
        List<String> accionesPago = strList(d, "accionesPagoVoluntarioDisponibles");
        if (!accionesPago.isEmpty()) {
            acc.add("pago=" + String.join("+", accionesPago));
        }
        return acc.isEmpty() ? "-" : String.join(",", acc);
    }

    // ── Impresión ─────────────────────────────────────────────────────────────

    private void imprimirMatriz(
            List<FilaMatriz> matriz, List<String> errores, List<String> advertencias) {

        int sanas      = (int) matriz.stream().filter(f -> !esObservada(f.actaId, errores, advertencias)).count();
        int conError   = contarConPrefijo(errores, matriz);
        int conWarn    = contarConAdvertencia(advertencias, matriz, errores);

        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════╗");
        System.out.printf( "║       MATRIZ SANIDAD FUNCIONAL — ACTAS DEMO POST-RESET                       ║%n");
        System.out.printf( "║  Actas: %-4d   Sanas: %-4d   Con advertencia: %-4d   Con error: %-4d          ║%n",
                matriz.size(), sanas, conWarn, conError);
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        String fmt = "%-14s | %-36s | %-26s | docs=%d | pFrm=%d | frmD=%d | pReq=%d | pGen=%d | notif=%d | %s";
        System.out.printf("%-14s | %-36s | %-26s | %-6s | %-6s | %-6s | %-6s | %-6s | %-7s | %s%n",
                "ActaId", "Bandeja", "EstadoProceso",
                "docs", "pFrm", "frmD", "pReq", "pGen", "notif", "acciones");
        System.out.println("-".repeat(165));

        for (FilaMatriz f : matriz) {
            System.out.printf((fmt + "%n"),
                    f.actaId, nvl(f.bandeja, "?"), nvl(f.estadoProceso, "?"),
                    f.numDocs, f.numDocsPendienteFirma, f.numDocsFirmados,
                    f.numPiezasReq, f.numPiezasGen, f.numNotifs,
                    nvl(f.acciones, "-"));
        }
        System.out.println();

        if (!advertencias.isEmpty()) {
            System.out.println("=== ADVERTENCIAS (" + advertencias.size() + ") ===");
            advertencias.forEach(w -> System.out.println("  WARN : " + w));
            System.out.println();
        }
        if (!errores.isEmpty()) {
            System.out.println("=== ERRORES (" + errores.size() + ") ===");
            errores.forEach(e -> System.out.println("  ERROR: " + e));
            System.out.println();
        }
        if (errores.isEmpty()) {
            System.out.println("=== RESULTADO: TODAS LAS ACTAS SUPERAN LAS REGLAS DE SANIDAD ===");
        }
        System.out.println();
    }

    private boolean esObservada(String actaId, List<String> errores, List<String> advertencias) {
        return errores.stream().anyMatch(e -> e.startsWith(actaId + " "))
                || advertencias.stream().anyMatch(w -> w.startsWith(actaId + " "));
    }

    private int contarConPrefijo(List<String> errores, List<FilaMatriz> matriz) {
        return (int) matriz.stream()
                .filter(f -> errores.stream().anyMatch(e -> e.startsWith(f.actaId + " ")))
                .count();
    }

    private int contarConAdvertencia(List<String> advertencias, List<FilaMatriz> matriz, List<String> errores) {
        return (int) matriz.stream()
                .filter(f -> advertencias.stream().anyMatch(w -> w.startsWith(f.actaId + " "))
                        && errores.stream().noneMatch(e -> e.startsWith(f.actaId + " ")))
                .count();
    }

    // ── Modelo de fila ────────────────────────────────────────────────────────

    record FilaMatriz(
            String actaId, String numeroActa, String bandeja, String estadoProceso,
            String bloque, String situacionAdmin,
            int numDocs, int numDocsPendienteFirma, int numDocsFirmados,
            int numPiezasReq, int numPiezasGen, int numNotifs,
            String accionPendiente, String tipoGestionExterna, String motivoArchivo,
            boolean permiteReingreso, boolean cerrable, String resultadoFinal,
            String acciones) {}

    // ── Utilidades ────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String nvl(String s, String fallback) {
        return s != null ? s : fallback;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof String s ? s : null;
    }

    private boolean bool(Map<String, Object> m, String key) {
        return Boolean.TRUE.equals(m.get(key));
    }

    private List<String> strList(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List<?> list) {
            return list.stream()
                    .filter(e -> e instanceof String)
                    .map(e -> (String) e)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<Map<String, Object>> mapList(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List<?> list) {
            return list.stream()
                    .filter(e -> e instanceof Map)
                    .map(e -> (Map<String, Object>) e)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<Object> estadosDocumentos(List<Map<String, Object>> docs) {
        return docs.stream().map(doc -> doc.get("estadoDocumento")).collect(Collectors.toList());
    }
}
