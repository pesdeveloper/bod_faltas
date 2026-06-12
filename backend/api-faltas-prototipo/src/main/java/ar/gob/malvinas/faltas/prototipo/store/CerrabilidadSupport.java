package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_ARCHIVO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_FIRMA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D1_CAPTURA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D2_ENRIQUECIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_ADJUNTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_EMITIDO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_FIRMADO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_PENDIENTE_FIRMA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_ANCLA_MEDIDA_PREVENTIVA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_ACUSE_RETENCION_VEHICULO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_LIBERACION_RODADO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_RESTITUCION_DOCUMENTACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.bandejaHabilitaResolucionBloqueoCierre;

/**
 * Resultado final compatible con cierre (ABSUELTO / PAGO_CONFIRMADO), pendientes
 * que bloquean cierre, y cómputo de condición de cierre. {@code PAGO_CONFIRMADO}
 * se alimenta desde la confirmación mock de pago informado en
 * {@link PrototipoStore#confirmarPagoInformado} o, en pruebas puntuales, vía
 * {@link #setResultadoFinalDemo} en carga demo. No cierra el expediente por sí
 * solo; {@link CierreSupport} valida antes de materializar cierre.
 *
 * <p>Origen material: si el expediente ya incluye la ancla documental mock
 * correspondiente (misma comprobación que
 * {@link #reconocerOrigenBloqueanteMedidaPreventiva} y afines), el
 * {@link PrototipoStore.OrigenBloqueanteCierreMaterialMock} se sincroniza
 * automáticamente a partir de esas anclas, sin depender del orden en que el
 * operador invoque el POST de reconocimiento. Ese
 * reconocimiento queda idempotente y puede seguir aportando el evento de
 * trazabilidad la primera vez que aún no figuraba en el mapa. Un {@link
 * PrototipoStore.PendienteBloqueanteCierreMock} subsiste mientras no se
 * registre {@linkplain #registrarCumplimientoMaterialEfectivoBloqueoCierre
 * cumplimiento material efectivo}, luego de emitido el documento resolutorio
 * mock ({@link #registrarResolucionBloqueoCierreDocumental}).
 */
final class CerrabilidadSupport {

    private static final String LECTURA_OPERATIVA_CONDICIONES_TEMPRANAS_ACTIVAS =
            "Existen condiciones materiales tempranas activas. Deberán resolverse documentalmente y"
                    + " cumplirse materialmente antes del cierre.";
    /**
     * Ejes que aún exigen resolutorio: ancla vía
     * {@link #registrarMedidaPreventivaPosterior} (evento
     * {@link #TIPO_EVENTO_MEDIDA_PREVENTIVA_POSTERIOR_AL_LABRADO}).
     */
    private static final String LECTURA_OPERATIVA_MEDIDA_POSTERIOR_SIN_RESOLUTORIO =
            "Existe una medida preventiva activa generada en trámite, posterior al labrado. Requiere"
                    + " resolutorio documental y cumplimiento material efectivo antes del cierre (mock).";
    private static final String LECTURA_OPERATIVA_FALTA_CUMPLIMIENTO_MATERIAL_EFECTIVO =
            "Existen resolutorios documentales registrados, pero todavía falta cumplimiento material"
                    + " efectivo.";

    private static final String TIPO_EVENTO_MEDIDA_PREVENTIVA_POSTERIOR_AL_LABRADO =
            "MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO";

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, List<ActaDocumentoMock>> documentosPorActa;
    private final Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa;
    private final Map<String, PrototipoStore.ResultadoFinalCierreMock> resultadoFinalPorActa = new HashMap<>();
    /**
     * Hechos operativos que exigen cierre de circuito (medida activa, rodado
     * retenido, documentación retenida) antes de poder cerrar.
     */
    private final Map<String, EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock>>
            origenesBloqueantesPorActa = new HashMap<>();
    /**
     * Orígenes cuyo cumplimiento material ya fue registrado (mock) para esa
     * acta. El levantamiento documental y el hecho material son pasos
     * distintos.
     */
    private final Map<String, EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock>>
            cumplimientoMaterialEfectivoPorActa = new HashMap<>();

    CerrabilidadSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, List<ActaDocumentoMock>> documentosPorActa,
            Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.documentosPorActa = documentosPorActa;
        this.situacionPagoCondenaPorActa = situacionPagoCondenaPorActa;
    }

    void clear() {
        resultadoFinalPorActa.clear();
        origenesBloqueantesPorActa.clear();
        cumplimientoMaterialEfectivoPorActa.clear();
    }

    /**
     * Comprueba la capa material por eje: documento resolutorio en expediente no
     * implica {@link PrototipoStore.FaseEjeHechoMaterial#CUMPLIMIENTO_MATERIAL_VERIFICADO}.
     */
    PrototipoStore.HechosMaterialesActaVista hechosMaterialesActa(String actaId) {
        if (actaId == null || !actas.containsKey(actaId)) {
            return new PrototipoStore.HechosMaterialesActaVista(List.of(), null);
        }
        ActaMock a = actas.get(actaId);
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> ors =
                a != null && !a.estaCerrada()
                        ? origenesBloqueantesEfectivosParaLectura(actaId)
                        : origenesBloqueantesPorActa.getOrDefault(
                                actaId,
                                EnumSet.noneOf(PrototipoStore.OrigenBloqueanteCierreMaterialMock.class));
        boolean evaluaBloqueoCierre = a != null && evaluacionBloqueoCierreHechoMaterial(a);
        return new PrototipoStore.HechosMaterialesActaVista(
                List.of(
                        ejeHechoVista(
                                actaId,
                                PrototipoStore.OrigenBloqueanteCierreMaterialMock.MEDIDA_PREVENTIVA_ACTIVA,
                                "MEDIDA_PREVENTIVA",
                                "Medida preventiva",
                                ors,
                                evaluaBloqueoCierre),
                        ejeHechoVista(
                                actaId,
                                PrototipoStore.OrigenBloqueanteCierreMaterialMock.RODADO_SECUESTRADO,
                                "RODADO",
                                "Retención / secuestro de rodado",
                                ors,
                                evaluaBloqueoCierre),
                        ejeHechoVista(
                                actaId,
                                PrototipoStore.OrigenBloqueanteCierreMaterialMock.DOCUMENTACION_RETENIDA,
                                "DOCUMENTACION",
                                "Documentación retenida",
                                ors,
                                evaluaBloqueoCierre)),
                lecturaOperativaHechosMateriales(actaId));
    }

    /**
     * Lectura para demo: solo cuando hay al menos un bloqueo material todavía
     * activo. Si aún falta resolutorio documental en algún eje, el mensaje
     * distingue anclas de etapa temprana (D1/D2, tránsito, etc.) de una medida
     * preventiva incorporada en trámite
     * ({@link #TIPO_EVENTO_MEDIDA_PREVENTIVA_POSTERIOR_AL_LABRADO};
     * {@link #registrarMedidaPreventivaPosterior}). Si los resolutorios ya
     * constan, el mensaje enfatiza el cumplimiento material efectivo pendiente.
     */
    private String lecturaOperativaHechosMateriales(String actaId) {
        List<PrototipoStore.PendienteBloqueanteCierreMock> pend =
                listarPendientesBloqueantesOrdenados(actaId);
        if (pend.isEmpty()) {
            return null;
        }
        for (PrototipoStore.PendienteBloqueanteCierreMock p : pend) {
            if (!tieneDocumentoResolutorio(actaId, origenParaPendiente(p))) {
                if (faseResolutorioPendienteSoloMedidaPreventivaPosteriorAlLabrado(actaId, pend)) {
                    return LECTURA_OPERATIVA_MEDIDA_POSTERIOR_SIN_RESOLUTORIO;
                }
                return LECTURA_OPERATIVA_CONDICIONES_TEMPRANAS_ACTIVAS;
            }
        }
        return LECTURA_OPERATIVA_FALTA_CUMPLIMIENTO_MATERIAL_EFECTIVO;
    }

    /**
     * @return true si el expediente incorporó medida vía
     *     {@link #registrarMedidaPreventivaPosterior} (evento de trazabilidad)
     *     y, entre los bloqueos sin resolutorio, solo figura el
     *     de medida.
     */
    private boolean faseResolutorioPendienteSoloMedidaPreventivaPosteriorAlLabrado(
            String actaId, List<PrototipoStore.PendienteBloqueanteCierreMock> pend) {
        if (!expedienteIncluyeEventoMedidaPreventivaPosteriorAlLabrado(actaId)) {
            return false;
        }
        for (PrototipoStore.PendienteBloqueanteCierreMock p : pend) {
            if (!tieneDocumentoResolutorio(actaId, origenParaPendiente(p))
                    && p != PrototipoStore.PendienteBloqueanteCierreMock.LEVANTAMIENTO_MEDIDA_PREVENTIVA) {
                return false;
            }
        }
        return true;
    }

    private boolean expedienteIncluyeEventoMedidaPreventivaPosteriorAlLabrado(String actaId) {
        List<ActaEventoMock> evs = eventosPorActa.get(actaId);
        if (evs == null || evs.isEmpty()) {
            return false;
        }
        return evs.stream()
                .anyMatch(
                        e ->
                                TIPO_EVENTO_MEDIDA_PREVENTIVA_POSTERIOR_AL_LABRADO.equals(
                                        e.tipoEvento()));
    }

    /**
     * El flag {@code bloqueaCierre} en el eje solo aplica en análisis: antes se
     * exponen fase y descripción (anclas) sin duplicar la lista de pendientes
     * de cierre, que allí se arma aparte.
     */
    private static boolean evaluacionBloqueoCierreHechoMaterial(ActaMock a) {
        return a != null
                && !a.estaCerrada()
                && BANDEJA_PENDIENTE_ANALISIS.equals(a.bandejaActual());
    }

    private PrototipoStore.EjeHechoMaterialVista ejeHechoVista(
            String actaId,
            PrototipoStore.OrigenBloqueanteCierreMaterialMock origen,
            String clave,
            String etiqueta,
            EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> ors,
            boolean evaluaBloqueoCierre) {
        if (ors == null || !ors.contains(origen)) {
            return new PrototipoStore.EjeHechoMaterialVista(
                    clave,
                    etiqueta,
                    PrototipoStore.FaseEjeHechoMaterial.NO_APLICA,
                    false,
                    "Sin origen material activo en este eje (mock).",
                    null);
        }
        String ejeBloqActivo = pendienteParaOrigen(origen).name();
        boolean doc = tieneDocumentoResolutorio(actaId, origen);
        if (tieneCumplimientoMaterialEfectivo(actaId, origen)) {
            return new PrototipoStore.EjeHechoMaterialVista(
                    clave,
                    etiqueta,
                    PrototipoStore.FaseEjeHechoMaterial.CUMPLIMIENTO_MATERIAL_VERIFICADO,
                    false,
                    descripcionCumplimientoEje(etiqueta),
                    null);
        }
        if (!doc) {
            return new PrototipoStore.EjeHechoMaterialVista(
                    clave,
                    etiqueta,
                    PrototipoStore.FaseEjeHechoMaterial.SITUACION_PENDIENTE_DE_RESOLUTORIO,
                    evaluaBloqueoCierre,
                    "Situación material activa: falta el documento resolutorio de "
                            + etiqueta
                            + " en expediente (mock).",
                    ejeBloqActivo);
        }
        return new PrototipoStore.EjeHechoMaterialVista(
                clave,
                etiqueta,
                PrototipoStore.FaseEjeHechoMaterial.RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL,
                evaluaBloqueoCierre,
                "Documento resolutorio incorporado; pendiente registro de hecho material efectivo: "
                        + descripcionCortaEje(etiqueta)
                        + " (mock).",
                ejeBloqActivo);
    }

    private static String descripcionCumplimientoEje(String etiqueta) {
        return "Hecho material verificado (capa distinta al expediente documental) para: "
                + etiqueta
                + " (mock).";
    }

    private static String descripcionCortaEje(String etiqueta) {
        if (etiqueta.startsWith("Medida")) {
            return "medida efectivamente levantada";
        }
        if (etiqueta.contains("rodado")) {
            return "rodado efectivamente liberado";
        }
        return "documentación efectivamente entregada o restituida";
    }

    PrototipoStore.ResultadoFinalCierreMock getResultadoFinal(String actaId) {
        if (actaId == null) {
            return PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL;
        }
        return resultadoFinalPorActa.getOrDefault(
                actaId, PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL);
    }

    void setResultadoFinalDemo(String actaId, PrototipoStore.ResultadoFinalCierreMock r) {
        if (actaId == null || r == null) {
            return;
        }
        if (r == PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL) {
            resultadoFinalPorActa.remove(actaId);
            return;
        }
        resultadoFinalPorActa.put(actaId, r);
    }

    void setOrigenesBloqueantesMaterialDemo(
            String actaId, Set<PrototipoStore.OrigenBloqueanteCierreMaterialMock> origenes) {
        if (actaId == null || origenes == null || origenes.isEmpty()) {
            origenesBloqueantesPorActa.remove(actaId);
            return;
        }
        origenesBloqueantesPorActa.put(actaId, EnumSet.copyOf(origenes));
    }

    /**
     * Pendientes de cierre material (resolutorio + cumplimiento aún requeridos) para
     * cualquier acta no cerrada: incluye D1/D2, no solo
     * {@code PENDIENTE_ANALISIS}, de modo que las
     * anclas nacidas en constatación temprana sigan trazables como
     * {@code LIBERACION_RODADO} y afines vía
     * {@link PrototipoStore#getCerrabilidadActa} antes de llegar a análisis.
     */
    List<PrototipoStore.PendienteBloqueanteCierreMock> listarPendientesBloqueantesOrdenados(String actaId) {
        ActaMock v = actas.get(actaId);
        if (v == null || v.estaCerrada()) {
            return List.of();
        }
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> origenes =
                origenesBloqueantesEfectivosParaLectura(actaId);
        if (origenes.isEmpty()) {
            return List.of();
        }
        EnumSet<PrototipoStore.PendienteBloqueanteCierreMock> pend = EnumSet.noneOf(
                PrototipoStore.PendienteBloqueanteCierreMock.class);
        for (PrototipoStore.OrigenBloqueanteCierreMaterialMock o : origenes) {
            if (!tieneCumplimientoMaterialEfectivo(actaId, o)) {
                pend.add(pendienteParaOrigen(o));
            }
        }
        return pend.stream()
                .sorted(Comparator.comparing(PrototipoStore.PendienteBloqueanteCierreMock::name))
                .toList();
    }

    /**
     * Reconoce medida preventiva activa anclada al expediente: debe existir un
     * documento de tipo {@code MEDIDA_PREVENTIVA} (p. ej. el incorporado al
     * producir la pieza en {@link PiezasFirmaSupport#generarMedidaPreventiva}
     * o en precarga demo coherente). Mismo criterio de tronco común que
     * {@link #reconocerOrigenBloqueanteSecuestroRodado}.
     */
    PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado reconocerOrigenBloqueanteMedidaPreventiva(
            String actaId) {
        return reconocerOrigenBloqueanteConAnclaDocumental(
                actaId,
                TIPO_ANCLA_MEDIDA_PREVENTIVA,
                PrototipoStore.OrigenBloqueanteCierreMaterialMock.MEDIDA_PREVENTIVA_ACTIVA,
                "ORIGEN_BLOQUEO_MEDIDA_PREVENTIVA_RECONOCIDO",
                "Origen material reconocido: medida preventiva activa (ancla "
                        + TIPO_ANCLA_MEDIDA_PREVENTIVA
                        + " en expediente; mock).");
    }

    PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado reconocerOrigenBloqueanteSecuestroRodado(
            String actaId) {
        return reconocerOrigenBloqueanteConAnclaDocumental(
                actaId,
                TIPO_DOC_ACUSE_RETENCION_VEHICULO,
                PrototipoStore.OrigenBloqueanteCierreMaterialMock.RODADO_SECUESTRADO,
                "ORIGEN_BLOQUEO_LIBERACION_RODADO_RECONOCIDO",
                "Origen material reconocido: vehículo retenido o secuestrado (ancla "
                        + TIPO_DOC_ACUSE_RETENCION_VEHICULO
                        + " en expediente; mock).");
    }

    PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado reconocerOrigenBloqueanteRetencionDocumental(
            String actaId) {
        return reconocerOrigenBloqueanteConAnclaDocumental(
                actaId,
                TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL,
                PrototipoStore.OrigenBloqueanteCierreMaterialMock.DOCUMENTACION_RETENIDA,
                "ORIGEN_BLOQUEO_ENTREGA_DOCUMENTACION_RECONOCIDO",
                "Origen material reconocido: documentación retenida (ancla "
                        + TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL
                        + " en expediente; mock).");
    }

    private PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado
            reconocerOrigenBloqueanteConAnclaDocumental(
                    String actaId,
                    String tipoAncla,
                    PrototipoStore.OrigenBloqueanteCierreMaterialMock origen,
                    String tipoEvento,
                    String descripcionEvento) {
        if (actaId == null) {
            return new PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado(
                    PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.NOT_FOUND,
                    null,
                    null,
                    getVistaCerrabilidad(null));
        }
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado(
                    PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.NOT_FOUND,
                    null,
                    null,
                    getVistaCerrabilidad(null));
        }
        if (actual.estaCerrada()) {
            return new PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado(
                    PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.CONFLICT,
                    actaId,
                    null,
                    getVistaCerrabilidad(actual));
        }
        ensureOrigenesSincronizados(actaId);
        if (!expedienteIncluyeDocumentoConTipo(actaId, tipoAncla)) {
            return new PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado(
                    PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.CONFLICT,
                    actaId,
                    null,
                    getVistaCerrabilidad(actual));
        }
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> ors = origenesBloqueantesPorActa
                .computeIfAbsent(
                        actaId, k -> EnumSet.noneOf(PrototipoStore.OrigenBloqueanteCierreMaterialMock.class));
        boolean nuevo = !ors.contains(origen);
        ors.add(origen);
        if (nuevo) {
            String bloque = actual.bloqueActual();
            registrarEvento(actaId, tipoEvento, bloque, bloque, descripcionEvento);
        }
        ActaMock post = actas.get(actaId);
        return new PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado(
                PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.OK,
                actaId,
                origen,
                getVistaCerrabilidad(post));
    }

    private boolean expedienteIncluyeDocumentoConTipo(String actaId, String tipoDocumento) {
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null || docs.isEmpty()) {
            return false;
        }
        return docs.stream().anyMatch(d -> tipoDocumento.equals(d.tipoDocumento()));
    }

    /**
     * Orígenes bloqueantes efectivos para lectura (bandejas, cerrabilidad,
     * clasificación): fusiona lo persistido con anclas documentales del
     * expediente sin mutar {@link #origenesBloqueantesPorActa}.
     */
    private EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock>
            origenesBloqueantesEfectivosParaLectura(String actaId) {
        if (actaId == null) {
            return EnumSet.noneOf(PrototipoStore.OrigenBloqueanteCierreMaterialMock.class);
        }
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> stored =
                origenesBloqueantesPorActa.get(actaId);
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> efectivos =
                stored != null
                        ? EnumSet.copyOf(stored)
                        : EnumSet.noneOf(PrototipoStore.OrigenBloqueanteCierreMaterialMock.class);
        if (expedienteIncluyeDocumentoConTipo(actaId, TIPO_ANCLA_MEDIDA_PREVENTIVA)) {
            efectivos.add(PrototipoStore.OrigenBloqueanteCierreMaterialMock.MEDIDA_PREVENTIVA_ACTIVA);
        }
        if (expedienteIncluyeDocumentoConTipo(actaId, TIPO_DOC_ACUSE_RETENCION_VEHICULO)) {
            efectivos.add(PrototipoStore.OrigenBloqueanteCierreMaterialMock.RODADO_SECUESTRADO);
        }
        if (expedienteIncluyeDocumentoConTipo(actaId, TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL)) {
            efectivos.add(PrototipoStore.OrigenBloqueanteCierreMaterialMock.DOCUMENTACION_RETENIDA);
        }
        return efectivos;
    }

    /**
     * Alinea {@link #origenesBloqueantesPorActa} con anclas reales en el
     * expediente (medida preventiva, acuse de vehículo, constatación
     * documental). No genera eventos; el reconocimiento manual añade el
     * origen solo si faltaba y dispara trazabilidad en ese caso.
     */
    private void ensureOrigenesSincronizados(String actaId) {
        if (actaId == null) {
            return;
        }
        ActaMock a = actas.get(actaId);
        if (a == null || a.estaCerrada()) {
            return;
        }
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> ors =
                origenesBloqueantesPorActa.computeIfAbsent(
                        actaId, k -> EnumSet.noneOf(PrototipoStore.OrigenBloqueanteCierreMaterialMock.class));
        if (expedienteIncluyeDocumentoConTipo(actaId, TIPO_ANCLA_MEDIDA_PREVENTIVA)) {
            ors.add(PrototipoStore.OrigenBloqueanteCierreMaterialMock.MEDIDA_PREVENTIVA_ACTIVA);
        }
        if (expedienteIncluyeDocumentoConTipo(actaId, TIPO_DOC_ACUSE_RETENCION_VEHICULO)) {
            ors.add(PrototipoStore.OrigenBloqueanteCierreMaterialMock.RODADO_SECUESTRADO);
        }
        if (expedienteIncluyeDocumentoConTipo(actaId, TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL)) {
            ors.add(PrototipoStore.OrigenBloqueanteCierreMaterialMock.DOCUMENTACION_RETENIDA);
        }
        if (ors.isEmpty()) {
            origenesBloqueantesPorActa.remove(actaId);
        }
    }

    private static PrototipoStore.PendienteBloqueanteCierreMock pendienteParaOrigen(
            PrototipoStore.OrigenBloqueanteCierreMaterialMock o) {
        return switch (o) {
            case MEDIDA_PREVENTIVA_ACTIVA -> PrototipoStore.PendienteBloqueanteCierreMock
                    .LEVANTAMIENTO_MEDIDA_PREVENTIVA;
            case RODADO_SECUESTRADO -> PrototipoStore.PendienteBloqueanteCierreMock.LIBERACION_RODADO;
            case DOCUMENTACION_RETENIDA -> PrototipoStore.PendienteBloqueanteCierreMock.ENTREGA_DOCUMENTACION;
        };
    }

    private static PrototipoStore.OrigenBloqueanteCierreMaterialMock origenParaPendiente(
            PrototipoStore.PendienteBloqueanteCierreMock p) {
        return switch (p) {
            case LEVANTAMIENTO_MEDIDA_PREVENTIVA ->
                    PrototipoStore.OrigenBloqueanteCierreMaterialMock.MEDIDA_PREVENTIVA_ACTIVA;
            case LIBERACION_RODADO -> PrototipoStore.OrigenBloqueanteCierreMaterialMock.RODADO_SECUESTRADO;
            case ENTREGA_DOCUMENTACION -> PrototipoStore.OrigenBloqueanteCierreMaterialMock.DOCUMENTACION_RETENIDA;
        };
    }

    private static String tipoDocumentoResolutorioSegunOrigen(
            PrototipoStore.OrigenBloqueanteCierreMaterialMock o) {
        return switch (o) {
            case MEDIDA_PREVENTIVA_ACTIVA -> TIPO_DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA;
            case RODADO_SECUESTRADO -> TIPO_DOC_LIBERACION_RODADO;
            case DOCUMENTACION_RETENIDA -> TIPO_DOC_RESTITUCION_DOCUMENTACION;
        };
    }

    private static String prefijoArchivoResolutorio(
            PrototipoStore.OrigenBloqueanteCierreMaterialMock o) {
        return switch (o) {
            case MEDIDA_PREVENTIVA_ACTIVA -> "levantamiento_medida_";
            case RODADO_SECUESTRADO -> "liberacion_rodado_";
            case DOCUMENTACION_RETENIDA -> "restitucion_documentacion_";
        };
    }

    private static String eventoResolucion(
            PrototipoStore.OrigenBloqueanteCierreMaterialMock o) {
        return switch (o) {
            case MEDIDA_PREVENTIVA_ACTIVA -> "LEVANTAMIENTO_MEDIDA_PREVENTIVA_PENDIENTE_FIRMA";
            case RODADO_SECUESTRADO -> "LIBERACION_RODADO_PENDIENTE_FIRMA";
            case DOCUMENTACION_RETENIDA -> "RESTITUCION_DOCUMENTACION_PENDIENTE_FIRMA";
        };
    }

    private static String descripcionResolucion(
            PrototipoStore.OrigenBloqueanteCierreMaterialMock o) {
        return switch (o) {
            case MEDIDA_PREVENTIVA_ACTIVA ->
                    "Resolutorio de levantamiento de medida preventiva generado; pendiente de firma antes del cumplimiento material (mock).";
            case RODADO_SECUESTRADO ->
                    "Resolutorio de liberación de rodado generado; pendiente de firma antes del cumplimiento material (mock).";
            case DOCUMENTACION_RETENIDA ->
                    "Resolutorio de restitución de documentación generado; pendiente de firma antes del cumplimiento material (mock).";
        };
    }

    private boolean tieneDocumentoResolutorio(
            String actaId, PrototipoStore.OrigenBloqueanteCierreMaterialMock origen) {
        String tipoEstandar = tipoDocumentoResolutorioSegunOrigen(origen);
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null) {
            return false;
        }
        return docs.stream()
                .anyMatch(
                        d -> {
                            String t = d.tipoDocumento();
                            if (origen == PrototipoStore.OrigenBloqueanteCierreMaterialMock.MEDIDA_PREVENTIVA_ACTIVA) {
                                return TIPO_DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF.equals(t)
                                        || TIPO_DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA.equals(t);
                            }
                            return tipoEstandar.equals(t);
                        });
    }

    /** Variante estricta: solo cuenta el resolutorio si ya está firmado (FIRMADO). */
    private boolean tieneDocumentoResolutorioFirmado(
            String actaId, PrototipoStore.OrigenBloqueanteCierreMaterialMock origen) {
        String tipoEstandar = tipoDocumentoResolutorioSegunOrigen(origen);
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null) {
            return false;
        }
        return docs.stream()
                .anyMatch(
                        d -> {
                            if (!ESTADO_DOC_FIRMADO.equals(d.estadoDocumento())) {
                                return false;
                            }
                            String t = d.tipoDocumento();
                            if (origen == PrototipoStore.OrigenBloqueanteCierreMaterialMock.MEDIDA_PREVENTIVA_ACTIVA) {
                                return TIPO_DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF.equals(t)
                                        || TIPO_DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA.equals(t);
                            }
                            return tipoEstandar.equals(t);
                        });
    }

    /**
     * {@code true} si al menos un bloqueante pendiente no tiene aún ningún
     * documento resolutorio generado (en ningún estado). Informa que la acción
     * "Generar resolución" sigue disponible.
     */
    boolean hayPendientesSinResolutorio(String actaId) {
        ensureOrigenesSincronizados(actaId);
        List<PrototipoStore.PendienteBloqueanteCierreMock> pend = listarPendientesBloqueantesOrdenados(actaId);
        for (PrototipoStore.PendienteBloqueanteCierreMock p : pend) {
            if (!tieneDocumentoResolutorio(actaId, origenParaPendiente(p))
                    && !tieneCumplimientoMaterialEfectivo(actaId, origenParaPendiente(p))) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code true} si al menos un bloqueante pendiente tiene un documento
     * resolutorio ya firmado ({@code FIRMADO}) pero el cumplimiento material
     * aún no fue registrado. Informa que la acción "Registrar cumplimiento"
     * está disponible.
     */
    boolean hayPendientesConResolutorioFirmado(String actaId) {
        ensureOrigenesSincronizados(actaId);
        List<PrototipoStore.PendienteBloqueanteCierreMock> pend = listarPendientesBloqueantesOrdenados(actaId);
        for (PrototipoStore.PendienteBloqueanteCierreMock p : pend) {
            PrototipoStore.OrigenBloqueanteCierreMaterialMock origen = origenParaPendiente(p);
            if (tieneDocumentoResolutorioFirmado(actaId, origen)
                    && !tieneCumplimientoMaterialEfectivo(actaId, origen)) {
                return true;
            }
        }
        return false;
    }

    private boolean tieneCumplimientoMaterialEfectivo(
            String actaId, PrototipoStore.OrigenBloqueanteCierreMaterialMock origen) {
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> hechos =
                cumplimientoMaterialEfectivoPorActa.get(actaId);
        return hechos != null && hechos.contains(origen);
    }

    private static String eventoCumplimientoMaterial(PrototipoStore.OrigenBloqueanteCierreMaterialMock o) {
        return switch (o) {
            case MEDIDA_PREVENTIVA_ACTIVA -> "CUMPLIMIENTO_LEVANTAMIENTO_MEDIDA_PREVENTIVA";
            case RODADO_SECUESTRADO -> "CUMPLIMIENTO_LIBERACION_RODADO";
            case DOCUMENTACION_RETENIDA -> "CUMPLIMIENTO_RESTITUCION_DOCUMENTACION";
        };
    }

    private static String descripcionCumplimientoMaterial(PrototipoStore.OrigenBloqueanteCierreMaterialMock o) {
        return switch (o) {
            case MEDIDA_PREVENTIVA_ACTIVA -> "Cumplimiento material registrado: medida preventiva efectivamente levantada (mock).";
            case RODADO_SECUESTRADO -> "Retiro de rodado registrado (app). Retiro efectivo desde corralón (mock).";
            case DOCUMENTACION_RETENIDA -> "Cumplimiento material registrado: documentación retenida efectivamente entregada / restituida (mock).";
        };
    }

    PrototipoStore.RegistrarResolucionBloqueoCierreResultado registrarResolucionBloqueoCierreDocumental(
            String actaId,
            PrototipoStore.PendienteBloqueanteCierreMock tipoPendiente,
            boolean documentoConCircuitoFirmaNotif) {
        PrototipoStore.OrigenBloqueanteCierreMaterialMock origen = origenParaPendiente(tipoPendiente);
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return resultadoRegistroVacio(PrototipoStore.RegistrarResolucionBloqueoCierreEstado.NOT_FOUND);
        }
        if (actual.estaCerrada()) {
            return resultadoRegistroVacio(PrototipoStore.RegistrarResolucionBloqueoCierreEstado.CONFLICT);
        }
        if (!bandejaHabilitaResolucionBloqueoCierre(actual.estaCerrada(), actual.bandejaActual())) {
            return resultadoRegistroVacio(PrototipoStore.RegistrarResolucionBloqueoCierreEstado.CONFLICT);
        }
        ensureOrigenesSincronizados(actaId);
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> ors = origenesBloqueantesPorActa.get(actaId);
        if (ors == null || !ors.contains(origen)) {
            return resultadoRegistroVacio(PrototipoStore.RegistrarResolucionBloqueoCierreEstado.CONFLICT);
        }
        if (tieneDocumentoResolutorio(actaId, origen)) {
            return resultadoRegistroVacio(PrototipoStore.RegistrarResolucionBloqueoCierreEstado.CONFLICT);
        }
        if ((tipoPendiente == PrototipoStore.PendienteBloqueanteCierreMock.LIBERACION_RODADO
                        || tipoPendiente == PrototipoStore.PendienteBloqueanteCierreMock.ENTREGA_DOCUMENTACION)
                && !tieneResultadoHabilitanteMaterial(actaId)) {
            String descripcion = tipoPendiente == PrototipoStore.PendienteBloqueanteCierreMock.LIBERACION_RODADO
                    ? "la liberación del rodado"
                    : "la entrega de documentación";
            return resultadoRegistroConflictoConMensaje(
                    "No se puede emitir el resolutorio de " + descripcion
                            + ": requiere pago confirmado, absolución o condena firme con pago confirmado.");
        }

        String tipoDoc = tipoDocumentoResolutorioSegunOrigen(origen);
        List<ActaDocumentoMock> docs = documentosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguienteDoc = docs.size() + 1;
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        String idDoc = "DOC-" + sufijoActa + "-" + String.format("%02d", siguienteDoc);
        String nombreArchivo = prefijoArchivoResolutorio(origen) + sufijoActa + ".pdf";
        docs.add(new ActaDocumentoMock(idDoc, actaId, tipoDoc, ESTADO_DOC_PENDIENTE_FIRMA, nombreArchivo));

        String bloqueOrigen = actual.bloqueActual();
        // Mover acta a PENDIENTE_FIRMA si no está ya allí (la firma del resolutorio
        // es obligatoria antes de registrar cumplimiento material).
        ActaMock actualizada = moverAPendienteFirmaResolutorio(actual);
        if (actualizada != actual) {
            actas.put(actaId, actualizada);
        }

        registrarEvento(
                actaId,
                eventoResolucion(origen),
                bloqueOrigen,
                actualizada.bandejaActual(),
                descripcionResolucion(origen));

        ActaMock post = actas.get(actaId);
        PrototipoStore.CerrabilidadActaVista vista = getVistaCerrabilidad(post);
        return new PrototipoStore.RegistrarResolucionBloqueoCierreResultado(
                PrototipoStore.RegistrarResolucionBloqueoCierreEstado.OK,
                actaId,
                idDoc,
                tipoDoc,
                tipoPendiente.name(),
                vista.resultadoFinal(),
                vista.cerrable(),
                vista.pendientesBloqueantes().stream()
                        .map(PrototipoStore.PendienteBloqueanteCierreMock::name)
                        .toList(),
                vista.motivoNoCerrable());
    }

    private static final String ESTADO_PENDIENTE_FIRMA_RESOLUTORIO = "PENDIENTE_FIRMA_PIEZAS";

    private ActaMock moverAPendienteFirmaResolutorio(ActaMock actual) {
        if (BANDEJA_PENDIENTE_FIRMA.equals(actual.bandejaActual())) {
            // Ya está en cola de firma; solo asegurar tieneDocumentos=true
            if (!actual.tieneDocumentos()) {
                return new ActaMock(actual.id(), actual.numeroActa(), actual.dominioReferencia(),
                        actual.bloqueActual(), actual.estadoProcesoActual(), actual.situacionAdministrativaActual(),
                        actual.estaCerrada(), actual.permiteReingreso(), true, actual.tieneNotificaciones(),
                        actual.fechaCreacion(), actual.infractorNombre(), actual.infractorDocumento(),
                        actual.inspectorNombre(), actual.resumenHecho(), actual.bandejaActual());
            }
            return actual;
        }
        return new ActaMock(actual.id(), actual.numeroActa(), actual.dominioReferencia(),
                actual.bloqueActual(), ESTADO_PENDIENTE_FIRMA_RESOLUTORIO, actual.situacionAdministrativaActual(),
                actual.estaCerrada(), actual.permiteReingreso(), true, actual.tieneNotificaciones(),
                actual.fechaCreacion(), actual.infractorNombre(), actual.infractorDocumento(),
                actual.inspectorNombre(), actual.resumenHecho(), BANDEJA_PENDIENTE_FIRMA);
    }

    PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado
            registrarCumplimientoMaterialEfectivoBloqueoCierre(
            String actaId, PrototipoStore.PendienteBloqueanteCierreMock tipoPendiente) {
        PrototipoStore.OrigenBloqueanteCierreMaterialMock origen = origenParaPendiente(tipoPendiente);
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return resultadoCumplimientoVacio(PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado
                    .NOT_FOUND);
        }
        if (actual.estaCerrada()) {
            return resultadoCumplimientoVacio(PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado
                    .CONFLICT);
        }
        if (!bandejaHabilitaResolucionBloqueoCierre(actual.estaCerrada(), actual.bandejaActual())) {
            return resultadoCumplimientoVacio(PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado
                    .CONFLICT);
        }
        ensureOrigenesSincronizados(actaId);
        EnumSet<PrototipoStore.OrigenBloqueanteCierreMaterialMock> ors = origenesBloqueantesPorActa.get(actaId);
        if (ors == null || !ors.contains(origen)) {
            return resultadoCumplimientoVacio(PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado
                    .CONFLICT);
        }
        if ((origen == PrototipoStore.OrigenBloqueanteCierreMaterialMock.RODADO_SECUESTRADO
                        || origen == PrototipoStore.OrigenBloqueanteCierreMaterialMock.DOCUMENTACION_RETENIDA)
                && !tieneResultadoHabilitanteMaterial(actaId)) {
            String descripcion = origen == PrototipoStore.OrigenBloqueanteCierreMaterialMock.RODADO_SECUESTRADO
                    ? "el retiro/liberación del rodado"
                    : "la entrega de documentación retenida";
            return resultadoCumplimientoConflictoConMensaje(
                    "No se puede registrar " + descripcion
                            + ": requiere pago confirmado, absolución o condena firme con pago confirmado.");
        }
        if (!tieneDocumentoResolutorioFirmado(actaId, origen)) {
            return resultadoCumplimientoVacio(PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado
                    .CONFLICT);
        }
        if (tieneCumplimientoMaterialEfectivo(actaId, origen)) {
            return resultadoCumplimientoVacio(PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado
                    .CONFLICT);
        }

        cumplimientoMaterialEfectivoPorActa
                .computeIfAbsent(
                        actaId, k -> EnumSet.noneOf(PrototipoStore.OrigenBloqueanteCierreMaterialMock.class))
                .add(origen);

        String bloqueOrigen = actual.bloqueActual();
        registrarEvento(
                actaId,
                eventoCumplimientoMaterial(origen),
                bloqueOrigen,
                bloqueOrigen,
                descripcionCumplimientoMaterial(origen));

        ActaMock post = actas.get(actaId);
        PrototipoStore.CerrabilidadActaVista vista = getVistaCerrabilidad(post);
        return new PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado(
                PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.OK,
                actaId,
                tipoPendiente.name(),
                vista.resultadoFinal(),
                vista.cerrable(),
                vista.pendientesBloqueantes().stream()
                        .map(PrototipoStore.PendienteBloqueanteCierreMock::name)
                        .toList(),
                vista.motivoNoCerrable());
    }

    private PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado resultadoCumplimientoVacio(
            PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado estado) {
        return new PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado(
                estado,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                null);
    }

    private PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado resultadoCumplimientoConflictoConMensaje(
            String mensaje) {
        return new PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado(
                PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.CONFLICT,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                mensaje);
    }

    private PrototipoStore.RegistrarResolucionBloqueoCierreResultado resultadoRegistroVacio(
            PrototipoStore.RegistrarResolucionBloqueoCierreEstado estado) {
        return new PrototipoStore.RegistrarResolucionBloqueoCierreResultado(
                estado,
                null,
                null,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                null);
    }

    PrototipoStore.CerrabilidadActaVista getVistaCerrabilidad(ActaMock acta) {
        if (acta == null) {
            return new PrototipoStore.CerrabilidadActaVista(
                    PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                    false,
                    List.of(),
                    "Acta inexistente.");
        }
        PrototipoStore.ResultadoFinalCierreMock rf = getResultadoFinal(acta.id());
        List<PrototipoStore.PendienteBloqueanteCierreMock> pend = listarPendientesBloqueantesOrdenados(acta.id());
        boolean condicionCierre = condicionCierreSinCerrarExpediente(acta.id(), rf, pend);
        boolean cerrableApi = !acta.estaCerrada() && condicionCierre;
        String motivo = motivoNoCerrable(acta, rf, pend, condicionCierre);
        return new PrototipoStore.CerrabilidadActaVista(
                rf, cerrableApi, pend, motivoNuloSiCerrable(cerrableApi, motivo));
    }

    private static String motivoNuloSiCerrable(boolean cerrableApi, String motivo) {
        return cerrableApi ? null : motivo;
    }

    /**
     * {@code true} si el cierre operativo (endpoint) puede ejecutarse: análisis,
     * no cerrada, resultado compatible y sin pendientes bloqueantes.
     */
    boolean puedeCerrarDesdeAnalisis(String actaId) {
        ActaMock a = actas.get(actaId);
        if (a == null || a.estaCerrada()) {
            return false;
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(a.bandejaActual())) {
            return false;
        }
        PrototipoStore.ResultadoFinalCierreMock rf = getResultadoFinal(actaId);
        List<PrototipoStore.PendienteBloqueanteCierreMock> pend = listarPendientesBloqueantesOrdenados(actaId);
        return condicionCierreSinCerrarExpediente(actaId, rf, pend);
    }

    PrototipoStore.MarcarResultadoAbsueltoResultado marcarResultadoAbsuelto(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.MarcarResultadoAbsueltoResultado(
                    PrototipoStore.MarcarResultadoAbsueltoEstado.NOT_FOUND, null, null);
        }
        if (actual.estaCerrada()) {
            return new PrototipoStore.MarcarResultadoAbsueltoResultado(
                    PrototipoStore.MarcarResultadoAbsueltoEstado.CONFLICT, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())) {
            return new PrototipoStore.MarcarResultadoAbsueltoResultado(
                    PrototipoStore.MarcarResultadoAbsueltoEstado.CONFLICT, null, null);
        }
        PrototipoStore.ResultadoFinalCierreMock rf = getResultadoFinal(actaId);
        if (rf == PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new PrototipoStore.MarcarResultadoAbsueltoResultado(
                    PrototipoStore.MarcarResultadoAbsueltoEstado.CONFLICT, null, null);
        }
        resultadoFinalPorActa.put(actaId, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);
        registrarEvento(
                actaId,
                "RESULTADO_ABSUELTO_MARCADO",
                BLOQUE_D5,
                BLOQUE_D5,
                "Resultado final mock: ABSUELTO (no implica cierre automático; pueden subsistir pendientes materiales).");
        return new PrototipoStore.MarcarResultadoAbsueltoResultado(
                PrototipoStore.MarcarResultadoAbsueltoEstado.OK, actaId, actual.bandejaActual());
    }

    /**
     * Alias de {@link #registrarCumplimientoMaterialEfectivoBloqueoCierre} para
     * la ruta heredada {@code resolver-pendiente-bloqueante-cierre}.
     */
    PrototipoStore.ResolverPendienteBloqueanteResultado resolverPendienteBloqueante(
            String actaId, PrototipoStore.PendienteBloqueanteCierreMock tipo) {
        var r = registrarCumplimientoMaterialEfectivoBloqueoCierre(actaId, tipo);
        if (r.estado() == PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.NOT_FOUND) {
            return new PrototipoStore.ResolverPendienteBloqueanteResultado(
                    PrototipoStore.ResolverPendienteBloqueanteEstado.NOT_FOUND,
                    null,
                    null,
                    PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                    false,
                    List.of(),
                    null);
        }
        if (r.estado() == PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.CONFLICT) {
            return new PrototipoStore.ResolverPendienteBloqueanteResultado(
                    PrototipoStore.ResolverPendienteBloqueanteEstado.CONFLICT,
                    null,
                    null,
                    PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                    false,
                    List.of(),
                    null);
        }
        return new PrototipoStore.ResolverPendienteBloqueanteResultado(
                PrototipoStore.ResolverPendienteBloqueanteEstado.OK,
                r.actaId(),
                r.pendienteCumplido(),
                r.resultadoFinal(),
                r.cerrable(),
                r.pendientesBloqueantesCierreRestantes(),
                r.motivoNoCerrable());
    }

    boolean coincideFiltroResultado(String actaId, PrototipoStore.ResultadoFinalCierreMock filtro) {
        if (filtro == null) {
            return true;
        }
        return getResultadoFinal(actaId) == filtro;
    }

    boolean coincideFiltroCerrable(ActaMock a, Boolean cerrable) {
        if (cerrable == null) {
            return true;
        }
        return getVistaCerrabilidad(a).cerrable() == cerrable.booleanValue();
    }

    boolean coincideFiltroPendiente(ActaMock a, PrototipoStore.PendienteBloqueanteCierreMock filtro) {
        if (filtro == null) {
            return true;
        }
        return listarPendientesBloqueantesOrdenados(a.id()).contains(filtro);
    }

    private boolean condicionCierreSinCerrarExpediente(
            String actaId,
            PrototipoStore.ResultadoFinalCierreMock rf,
            List<PrototipoStore.PendienteBloqueanteCierreMock> pend) {
        boolean resultadoOk = rf == PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                || rf == PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO
                || (rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME
                        && getSituacionPagoCondena(actaId) == PrototipoStore.SituacionPagoCondena.CONFIRMADO);
        return resultadoOk && (pend == null || pend.isEmpty());
    }

    private String motivoNoCerrable(
            ActaMock acta,
            PrototipoStore.ResultadoFinalCierreMock rf,
            List<PrototipoStore.PendienteBloqueanteCierreMock> pend,
            boolean condicionCierre) {
        if (acta.estaCerrada()) {
            return "Expediente ya cerrado; no aplica condición de cierre operativa.";
        }
        if (condicionCierre) {
            return null;
        }
        if (rf == PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL) {
            return "Falta resultado final compatible con cierre (ABSUELTO o PAGO_CONFIRMADO).";
        }
        if (pend != null && !pend.isEmpty() && tieneResultadoHabilitanteMaterial(acta.id())) {
            return motivoPendientesMateriales(pend);
        }
        if (rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            PrototipoStore.SituacionPagoCondena situacion = getSituacionPagoCondena(acta.id());
            if (situacion == PrototipoStore.SituacionPagoCondena.INFORMADO) {
                return "Pago de condena informado pendiente de confirmación.";
            }
            return "Condena firme pendiente de pago o derivación externa.";
        }
        if (rf != PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                && rf != PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO) {
            return "Resultado final no habilita cierre en este slice.";
        }
        if (pend != null && !pend.isEmpty()) {
            return motivoPendientesMateriales(pend);
        }
        return "No cumple condición de cierre (ver resultado final y pendientes).";
    }

    private String motivoPendientesMateriales(List<PrototipoStore.PendienteBloqueanteCierreMock> pend) {
        return "Aún no se cumple la condición de cierre: "
                + resumenPendientes(pend)
                + " (cada eje: documento resolutorio en expediente y luego hecho material efectivo; el "
                + "documento no sustituye al hecho).";
    }

    private PrototipoStore.SituacionPagoCondena getSituacionPagoCondena(String actaId) {
        if (getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return PrototipoStore.SituacionPagoCondena.NO_APLICA;
        }
        return situacionPagoCondenaPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoCondena.PENDIENTE);
    }

    private static String resumenPendientes(List<PrototipoStore.PendienteBloqueanteCierreMock> pend) {
        return String.join(
                ", ",
                pend.stream().map(PrototipoStore.PendienteBloqueanteCierreMock::name).toList());
    }

    /**
     * Contravención (u otro trámite) en análisis: constata en expediente la
     * medida preventiva a partir de una novedad posterior al labrado
     * (p. ej. inspección que constata afectación de faja o clausura, sin
     * detalle normativo en este mock). Misma ancla
     * {@link PrototipoConstantes#TIPO_ANCLA_MEDIDA_PREVENTIVA} y circuito
     * que al producir la pieza; distinto a
     * {@link #registrarConstatacionMaterialTemprana} (D1/D2) y a datos de
     * tránsito.
     */
    PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado registrarMedidaPreventivaPosterior(
            String actaId) {
        if (actaId == null) {
            return new PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado(
                    PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.NOT_FOUND,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado(
                    PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.NOT_FOUND,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        if (actual.estaCerrada()) {
            return new PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado(
                    PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.CONFLICT,
                    actaId,
                    null,
                    null,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .ACTA_CERRADA);
        }
        if (BANDEJA_ARCHIVO.equals(actual.bandejaActual())) {
            return new PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado(
                    PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.CONFLICT,
                    actaId,
                    null,
                    null,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .ACTA_EN_ARCHIVO);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())) {
            return new PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado(
                    PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.CONFLICT,
                    actaId,
                    null,
                    null,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .FUERA_PENDIENTE_ANALISIS);
        }
        if (expedienteIncluyeDocumentoConTipo(actaId, TIPO_ANCLA_MEDIDA_PREVENTIVA)) {
            return new PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado(
                    PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.CONFLICT,
                    actaId,
                    null,
                    TIPO_ANCLA_MEDIDA_PREVENTIVA,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .MEDIDA_YA_EN_EXPEDIENTE);
        }

        List<ActaDocumentoMock> docs = documentosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguiente = docs.size() + 1;
        String sufijo = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        String idDoc = "DOC-" + sufijo + "-" + String.format("%02d", siguiente);
        String nombreArchivo = "medida_preventiva_posterior_tramite_" + sufijo.toLowerCase() + ".pdf";
        docs.add(
                new ActaDocumentoMock(
                        idDoc, actaId, TIPO_ANCLA_MEDIDA_PREVENTIVA, ESTADO_DOC_EMITIDO, nombreArchivo));

        if (!actual.tieneDocumentos()) {
            actas.put(
                    actaId,
                    new ActaMock(
                            actual.id(),
                            actual.numeroActa(),
                            actual.dominioReferencia(),
                            actual.bloqueActual(),
                            actual.estadoProcesoActual(),
                            actual.situacionAdministrativaActual(),
                            actual.estaCerrada(),
                            actual.permiteReingreso(),
                            true,
                            actual.tieneNotificaciones(),
                            actual.fechaCreacion(),
                            actual.infractorNombre(),
                            actual.infractorDocumento(),
                            actual.inspectorNombre(),
                            actual.resumenHecho(),
                            actual.bandejaActual()));
        }

        ensureOrigenesSincronizados(actaId);
        String bloque = BLOQUE_D5;
        registrarEvento(
                actaId,
                TIPO_EVENTO_MEDIDA_PREVENTIVA_POSTERIOR_AL_LABRADO,
                bloque,
                bloque,
                "Inspección o noticia en trámite, posterior al labrado, da lugar a ancla de medida"
                        + " preventiva (mock; no etapa D1/D2; mismo tipo documental que pieza o constatación"
                        + " coherente).");

        ActaMock post = actas.get(actaId);
        return new PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado(
                PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.OK,
                actaId,
                idDoc,
                TIPO_ANCLA_MEDIDA_PREVENTIVA,
                post != null ? post.bandejaActual() : null,
                post != null ? post.estadoProcesoActual() : null,
                null);
    }

    /**
     * Registra en expediente el mismo ancla documental que alimenta
     * {@link #ensureOrigenesSincronizados} y el circuito de cierre, pero
     * desde D1/D2 (labrado / enriquecimiento), con evento de trazabilidad
     * explícito. Requiere al menos un evento previo en el expediente (p. ej.
     * {@code ALTA} en D1) para vincular la constatación a un despliegue temprano
     * real, no a una marca aislada. Rechazo controlado: si el expediente ya
     * incluía el {@code tipoDocumento} de esa constatación, devuelve CONFLICT
     * (API 409) sin añadir documento ni evento, evitando doble ancla, doble
     * origen y lecturas duplicadas. Si el acta no está en bandeja
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con bloque {@code D1_CAPTURA} o
     * {@code D2_ENRIQUECIMIENTO}, el rechazo no modifica el expediente
     * (criterio de etapa D1/D2 en el prototipo). No reemplaza a {@code
     * reconocerOrigenBloqueante*}; el reconocimiento sigue siendo opcional.
     */
    PrototipoStore.RegistrarConstatacionMaterialTempranaResultado registrarConstatacionMaterialTemprana(
            String actaId, PrototipoStore.TipoConstatacionMaterialTemprana tipo) {
        if (actaId == null || tipo == null) {
            return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                    PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.NOT_FOUND,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                    PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.NOT_FOUND,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        if (actual.estaCerrada()) {
            return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                    PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.CONFLICT,
                    actaId,
                    null,
                    null,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.FUERA_ETAPA_LABRADO_ENRIQUECIMIENTO);
        }
        if (!BANDEJA_ACTAS_EN_ENRIQUECIMIENTO.equals(actual.bandejaActual())) {
            return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                    PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.CONFLICT,
                    actaId,
                    null,
                    null,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.FUERA_ETAPA_LABRADO_ENRIQUECIMIENTO);
        }
        String b = actual.bloqueActual();
        if (b == null
                || (!BLOQUE_D1_CAPTURA.equals(b) && !BLOQUE_D2_ENRIQUECIMIENTO.equals(b))) {
            return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                    PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.CONFLICT,
                    actaId,
                    null,
                    null,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.FUERA_ETAPA_LABRADO_ENRIQUECIMIENTO);
        }
        List<ActaEventoMock> eventosExistentes = eventosPorActa.get(actaId);
        if (eventosExistentes == null || eventosExistentes.isEmpty()) {
            return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                    PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.CONFLICT,
                    actaId,
                    null,
                    null,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.SIN_TRAZA_PREVIA);
        }
        String tipoDoc;
        String prefijoArchivo;
        String estadoDoc;
        String tipoEvento;
        String descEvento;
        switch (tipo) {
            case SECUESTRO_RODADO:
                tipoDoc = TIPO_DOC_ACUSE_RETENCION_VEHICULO;
                prefijoArchivo = "acta_retencion_vehiculo_";
                estadoDoc = ESTADO_DOC_ADJUNTO;
                tipoEvento = "CONSTATACION_SECUESTRO_RODADO_D1_D2";
                descEvento = "En expediente: ancla de retención o secuestro de rodado (labrado/enriquecimiento; mock).";
                break;
            case RETENCION_DOCUMENTAL:
                tipoDoc = TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL;
                prefijoArchivo = "constatacion_retencion_doc_";
                estadoDoc = ESTADO_DOC_ADJUNTO;
                tipoEvento = "CONSTATACION_RETENCION_DOCUMENTAL_D1_D2";
                descEvento = "En expediente: constatación de retención de documentación (labrado/enriquecimiento; mock).";
                break;
            case MEDIDA_PREVENTIVA_APLICABLE:
                tipoDoc = TIPO_ANCLA_MEDIDA_PREVENTIVA;
                prefijoArchivo = "constatacion_medida_preventiva_";
                estadoDoc = ESTADO_DOC_EMITIDO;
                tipoEvento = "CONSTATACION_MEDIDA_PREVENTIVA_D1_D2";
                descEvento = "En expediente: ancla de medida preventiva aplicable (mismo tipo que la pieza "
                        + TIPO_ANCLA_MEDIDA_PREVENTIVA
                        + "; etapa temprana; mock).";
                break;
            default:
                throw new IllegalStateException("tipo: " + tipo);
        }
        if (expedienteIncluyeDocumentoConTipo(actaId, tipoDoc)) {
            return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                    PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.CONFLICT,
                    actaId,
                    null,
                    tipoDoc,
                    null,
                    null,
                    PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.TIPO_YA_EN_EXPEDIENTE);
        }
        List<ActaDocumentoMock> docs = documentosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguiente = docs.size() + 1;
        String sufijo = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        String idDoc = "DOC-" + sufijo + "-" + String.format("%02d", siguiente);
        String nombreArchivo = prefijoArchivo + sufijo.toLowerCase() + ".pdf";
        docs.add(new ActaDocumentoMock(idDoc, actaId, tipoDoc, estadoDoc, nombreArchivo));

        if (!actual.tieneDocumentos()) {
            actas.put(
                    actaId,
                    new ActaMock(
                            actual.id(),
                            actual.numeroActa(),
                            actual.dominioReferencia(),
                            actual.bloqueActual(),
                            actual.estadoProcesoActual(),
                            actual.situacionAdministrativaActual(),
                            actual.estaCerrada(),
                            actual.permiteReingreso(),
                            true,
                            actual.tieneNotificaciones(),
                            actual.fechaCreacion(),
                            actual.infractorNombre(),
                            actual.infractorDocumento(),
                            actual.inspectorNombre(),
                            actual.resumenHecho(),
                            actual.bandejaActual()));
        }

        ensureOrigenesSincronizados(actaId);
        registrarEvento(actaId, tipoEvento, b, b, descEvento);

        ActaMock post = actas.get(actaId);
        return new PrototipoStore.RegistrarConstatacionMaterialTempranaResultado(
                PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.OK,
                actaId,
                idDoc,
                tipoDoc,
                post != null ? post.bandejaActual() : null,
                post != null ? post.estadoProcesoActual() : null,
                null);
    }

    /**
     * Crea automáticamente el documento resolutorio de restitución documental
     * cuando se invoca el cumplimiento material de DOCUMENTACION_RETENIDA sin
     * haber emitido el resolutorio previamente. Hace la acción atómica desde
     * el punto de vista del operador: un solo POST registra la constancia y el
     * cumplimiento material.
     */
    /**
     * {@code true} si el resultado final habilita la ejecucion de operaciones
     * materiales (entrega de documentacion, liberacion de rodado, levantamiento
     * de medida preventiva). Habilitantes validos: ABSUELTO, PAGO_CONFIRMADO
     * o CONDENA_FIRME con situacionPagoCondena=CONFIRMADO.
     */
    boolean tieneResultadoHabilitanteMaterial(String actaId) {
        PrototipoStore.ResultadoFinalCierreMock rf = getResultadoFinal(actaId);
        if (rf == PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                || rf == PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO) {
            return true;
        }
        if (rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return getSituacionPagoCondena(actaId) == PrototipoStore.SituacionPagoCondena.CONFIRMADO;
        }
        return false;
    }

    private PrototipoStore.RegistrarResolucionBloqueoCierreResultado resultadoRegistroConflictoConMensaje(
            String mensaje) {
        return new PrototipoStore.RegistrarResolucionBloqueoCierreResultado(
                PrototipoStore.RegistrarResolucionBloqueoCierreEstado.CONFLICT,
                null,
                null,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                mensaje);
    }

    private void registrarEvento(
            String actaId,
            String tipoEvento,
            String bloqueOrigen,
            String bloqueDestino,
            String descripcion) {
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                tipoEvento,
                bloqueOrigen,
                bloqueDestino,
                descripcion));
    }
}
