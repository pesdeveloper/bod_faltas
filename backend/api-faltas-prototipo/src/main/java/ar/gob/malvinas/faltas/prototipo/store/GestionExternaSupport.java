package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_EN_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;

/**
 * Soporte funcional del área gestión externa del prototipo. Concentra la
 * derivación efectiva de expedientes desde análisis hacia la macro-bandeja
 * {@code GESTION_EXTERNA}, consumiendo la marca operativa
 * {@link PrototipoStore#ACCION_DERIVAR_GESTION_EXTERNA} que el caso ya trae
 * por haber atravesado fallo + notificación de fallo + ventana de espera
 * posterior cumplida sin novedad.
 *
 * <p>Alcance funcional:
 * <ul>
 *   <li>transición efectiva análisis → gestión externa, parametrizada por
 *       tipo vigente ({@link PrototipoStore#TIPO_GESTION_EXTERNA_APREMIO} o
 *       {@link PrototipoStore#TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ});</li>
 *   <li>retorno efectivo gestión externa → análisis, único destino en este
 *       slice: {@code PENDIENTE_ANALISIS} con marca operativa
 *       {@link PrototipoStore#ACCION_REVISION_POST_GESTION_EXTERNA},
 *       preservando el {@code tipoGestionExterna} original como
 *       trazabilidad sintética;</li>
 *   <li>sin integración con sistemas externos;</li>
 *   <li>sin subestados internos ricos dentro de la nueva bandeja;</li>
 *   <li>sin políticas de retorno diferenciadas por tipo todavía.</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}) y mantiene como propio únicamente el mapa
 * de {@code tipoGestionExterna}, que pertenece semánticamente a esta área.
 */
final class GestionExternaSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final CerrabilidadSupport cerrabilidad;
    private final Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa;
    private final Map<String, BigDecimal> montoCondenaPorActa;

    /**
     * Tipo de gestión externa vigente para la acta dentro de la macro-bandeja
     * {@code GESTION_EXTERNA}. Específico del área: las actas que no fueron
     * derivadas simplemente no aparecen en el mapa.
     */
    private final Map<String, String> tipoGestionExternaPorActa = new LinkedHashMap<>();

    GestionExternaSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa,
            CerrabilidadSupport cerrabilidad,
            Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa,
            Map<String, BigDecimal> montoCondenaPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.cerrabilidad = cerrabilidad;
        this.situacionPagoCondenaPorActa = situacionPagoCondenaPorActa;
        this.montoCondenaPorActa = montoCondenaPorActa;
    }

    String getTipoGestionExterna(String actaId) {
        return tipoGestionExternaPorActa.get(actaId);
    }

    /**
     * Helper interno de mocks: asigna el tipo de gestión externa vigente para
     * una acta ya precargada en la macro-bandeja {@code GESTION_EXTERNA} sin
     * ejecutar la transición. Uso restringido a la fábrica de datos demo,
     * para poder representar desde el reset casos ya derivados (por
     * ejemplo, listos para el futuro retorno efectivo).
     */
    void setTipoGestionExterna(String actaId, String tipo) {
        if (actaId == null || tipo == null) {
            return;
        }
        tipoGestionExternaPorActa.put(actaId, tipo);
    }

    void clear() {
        tipoGestionExternaPorActa.clear();
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link PrototipoStore#TIPO_GESTION_EXTERNA_APREMIO}. Delega en el
     * helper compartido; misma precondición y mismo efecto observable que
     * {@link #derivarAJuzgadoDePaz(String)} salvo por el tipo asignado.
     */
    PrototipoStore.DerivarAGestionExternaResultado derivarAApremio(String actaId) {
        return derivar(actaId, PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO);
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link PrototipoStore#TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ}. Delega en
     * el helper compartido; misma precondición y mismo efecto observable
     * que {@link #derivarAApremio(String)} salvo por el tipo asignado.
     */
    PrototipoStore.DerivarAGestionExternaResultado derivarAJuzgadoDePaz(String actaId) {
        return derivar(actaId, PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ);
    }

    /**
     * Transición compartida por las derivaciones tipadas a gestión externa.
     * Solo aplica si la acta está en PENDIENTE_ANALISIS y trae una marca
     * operativa válida como disparador de derivación efectiva:
     * <ul>
     *   <li>{@link PrototipoStore#ACCION_DERIVAR_GESTION_EXTERNA}: primera
     *       derivación, casos post fallo + notificación de fallo + ventana
     *       de espera posterior cumplida sin novedad;</li>
     *   <li>{@link PrototipoStore#ACCION_REVISION_POST_GESTION_EXTERNA}:
     *       re-derivación de un expediente que ya volvió desde gestión
     *       externa y el análisis decide mandarlo otra vez. La posibilidad
     *       de re-derivar no se consume por ida y vuelta: mientras el caso
     *       no entre en una salida terminal (nulidad, cierre, archivo
     *       efectivo), cada retorno habilita una nueva derivación.</li>
     * </ul>
     * Deja el acta con bloque/estado/situación {@code GESTION_EXTERNA},
     * reingreso habilitado para habilitar el retorno efectivo, y limpia la
     * marca operativa previa. Genera un evento
     * {@code DERIVACION_GESTION_EXTERNA} con origen D5_ANALISIS y destino
     * GESTION_EXTERNA, explicitando el tipo asignado y, si corresponde, el
     * tipo previo preservado como trazabilidad de la gestión externa de la
     * que venía el expediente.
     */
    private PrototipoStore.DerivarAGestionExternaResultado derivar(String actaId, String tipo) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.DerivarAGestionExternaResultado(
                    PrototipoStore.DerivarAGestionExternaEstado.NOT_FOUND, null, null, null, null, null);
        }
        // Defensa explícita: situación de pago impide derivación.
        PrototipoStore.SituacionPagoCondena situacionPago = situacionPagoCondenaPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoCondena.PENDIENTE);
        if (situacionPago == PrototipoStore.SituacionPagoCondena.CONFIRMADO) {
            return new PrototipoStore.DerivarAGestionExternaResultado(
                    PrototipoStore.DerivarAGestionExternaEstado.CONFLICT, null, null, null, null,
                    "No se puede derivar a gestión externa una condena con pago confirmado.");
        }
        if (situacionPago == PrototipoStore.SituacionPagoCondena.INFORMADO) {
            return new PrototipoStore.DerivarAGestionExternaResultado(
                    PrototipoStore.DerivarAGestionExternaEstado.CONFLICT, null, null, null, null,
                    "No se puede derivar a gestión externa mientras el pago de condena informado está pendiente de confirmación.");
        }
        String marcaActual = accionPendientePorActa.get(actaId);
        boolean marcaHabilitaDerivacion =
                PrototipoStore.ACCION_DERIVAR_GESTION_EXTERNA.equals(marcaActual)
                        || PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA.equals(marcaActual);
        boolean condenaFirmeDerivable = condenaFirmeDerivable(actaId);
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())
                || (!marcaHabilitaDerivacion && !condenaFirmeDerivable)
                || tipoGestionExternaPorActa.containsKey(actaId)) {
            return new PrototipoStore.DerivarAGestionExternaResultado(
                    PrototipoStore.DerivarAGestionExternaEstado.CONFLICT, null, null, null, null, null);
        }

        boolean esRederivacion =
                PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA.equals(marcaActual);
        String tipoPrevio = tipoGestionExternaPorActa.get(actaId);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_GESTION_EXTERNA,
                ESTADO_EN_GESTION_EXTERNA,
                BLOQUE_GESTION_EXTERNA,
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_GESTION_EXTERNA);
        actas.put(actaId, actualizada);

        String descripcion;
        if (esRederivacion && tipoPrevio != null) {
            descripcion = "Análisis jurídico re-deriva el caso a gestión externa (tipo previo "
                    + tipoPrevio + ", nuevo tipo " + tipo + ").";
        } else if (esRederivacion) {
            descripcion = "Análisis jurídico re-deriva el caso a gestión externa; tipo " + tipo + ".";
        } else {
            descripcion = "Análisis jurídico deriva efectivamente el caso a gestión externa; tipo "
                    + tipo + ".";
        }
        registrarEvento(
                actaId,
                "DERIVACION_GESTION_EXTERNA",
                BLOQUE_D5,
                BLOQUE_GESTION_EXTERNA,
                descripcion);

        accionPendientePorActa.remove(actaId);
        tipoGestionExternaPorActa.put(actaId, tipo);

        return new PrototipoStore.DerivarAGestionExternaResultado(
                PrototipoStore.DerivarAGestionExternaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                tipo,
                null);
    }

    private boolean condenaFirmeDerivable(String actaId) {
        if (cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return false;
        }
        PrototipoStore.SituacionPagoCondena situacion = situacionPagoCondenaPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoCondena.PENDIENTE);
        return situacion == PrototipoStore.SituacionPagoCondena.PENDIENTE
                || situacion == PrototipoStore.SituacionPagoCondena.OBSERVADO;
    }

    /**
     * Demo: retorno efectivo desde la macro-bandeja {@code GESTION_EXTERNA}
     * al circuito operativo. Solo aplica si la acta está en
     * {@code GESTION_EXTERNA} y el modelo declara
     * {@code permiteReingreso = true}. Destino único en este slice:
     * {@code PENDIENTE_ANALISIS} con bloque {@code D5_ANALISIS}, estado
     * {@code PENDIENTE_REVISION} y situación {@code ACTIVA}; el reingreso
     * no se consume: {@code permiteReingreso} se preserva en {@code true}
     * porque la bandera representa la naturaleza reingresable del
     * expediente frente a gestión externa, no un uso único del retorno.
     * La marca operativa pasa a
     * {@link PrototipoStore#ACCION_REVISION_POST_GESTION_EXTERNA}. El
     * {@code tipoGestionExterna} original se preserva intacto para que el
     * caso retorne, pero siga siendo visible de qué tipo de gestión externa
     * venía. Genera un evento
     * {@code ACTA_REINGRESADA_DESDE_GESTION_EXTERNA} con origen
     * {@code GESTION_EXTERNA} y destino {@code D5_ANALISIS}, explicitando
     * el tipo previo como trazabilidad.
     */
    PrototipoStore.ReingresarDesdeGestionExternaResultado reingresarActaDesdeGestionExterna(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReingresarDesdeGestionExternaResultado(
                    PrototipoStore.ReingresarDesdeGestionExternaEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (!BANDEJA_GESTION_EXTERNA.equals(actual.bandejaActual()) || !actual.permiteReingreso()) {
            return new PrototipoStore.ReingresarDesdeGestionExternaResultado(
                    PrototipoStore.ReingresarDesdeGestionExternaEstado.CONFLICT, null, null, null, null, null);
        }

        String tipoPrevio = tipoGestionExternaPorActa.get(actaId);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA);

        String descripcion = tipoPrevio == null
                ? "Retorno desde gestión externa; acta vuelve a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA + "."
                : "Retorno desde gestión externa (tipo previo " + tipoPrevio
                        + "); acta vuelve a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA + ".";
        registrarEvento(
                actaId,
                "ACTA_REINGRESADA_DESDE_GESTION_EXTERNA",
                BLOQUE_GESTION_EXTERNA,
                BLOQUE_D5,
                descripcion);

        return new PrototipoStore.ReingresarDesdeGestionExternaResultado(
                PrototipoStore.ReingresarDesdeGestionExternaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA,
                tipoPrevio);
    }

    /**
     * APREMIO: retorno administrativo sin pago. Solo aplica si la acta está
     * en {@code GESTION_EXTERNA} con {@code tipoGestionExterna = APREMIO} y
     * {@code permiteReingreso = true}. Deja el acta en
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link PrototipoStore#ACCION_REVISION_POST_GESTION_EXTERNA}, condena
     * firme y situación de pago PENDIENTE. Limpia {@code tipoGestionExterna}.
     */
    PrototipoStore.ReingresarDesdeApremioSinPagoResultado reingresarDesdeApremioSinPago(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReingresarDesdeApremioSinPagoResultado(
                    PrototipoStore.ReingresarDesdeApremioSinPagoEstado.NOT_FOUND, null, null, null, null);
        }
        if (!BANDEJA_GESTION_EXTERNA.equals(actual.bandejaActual())
                || !actual.permiteReingreso()
                || !PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO.equals(tipoGestionExternaPorActa.get(actaId))) {
            return new PrototipoStore.ReingresarDesdeApremioSinPagoResultado(
                    PrototipoStore.ReingresarDesdeApremioSinPagoEstado.CONFLICT, null, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA);
        tipoGestionExternaPorActa.remove(actaId);

        registrarEvento(
                actaId,
                "ACTA_REINGRESADA_DESDE_APREMIO_SIN_PAGO",
                BLOQUE_GESTION_EXTERNA,
                BLOQUE_D5,
                "Retorno administrativo desde Apremio sin pago; condena firme pendiente. "
                        + "Acta vuelve a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA + ".");

        return new PrototipoStore.ReingresarDesdeApremioSinPagoResultado(
                PrototipoStore.ReingresarDesdeApremioSinPagoEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA);
    }

    /**
     * APREMIO: registra pago efectuado en el proceso de apremio. Solo aplica
     * si la acta está en {@code GESTION_EXTERNA} con
     * {@code tipoGestionExterna = APREMIO} y {@code permiteReingreso = true}.
     * Confirma la situación de pago, limpia {@code tipoGestionExterna} y deja
     * el acta en {@code PENDIENTE_ANALISIS} cerrable (sin acción pendiente).
     */
    PrototipoStore.RegistrarPagoEnApremioResultado registrarPagoEnApremio(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarPagoEnApremioResultado(
                    PrototipoStore.RegistrarPagoEnApremioEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_GESTION_EXTERNA.equals(actual.bandejaActual())
                || !actual.permiteReingreso()
                || !PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO.equals(tipoGestionExternaPorActa.get(actaId))) {
            return new PrototipoStore.RegistrarPagoEnApremioResultado(
                    PrototipoStore.RegistrarPagoEnApremioEstado.CONFLICT, null, null, null);
        }

        situacionPagoCondenaPorActa.put(actaId, PrototipoStore.SituacionPagoCondena.CONFIRMADO);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.remove(actaId);
        tipoGestionExternaPorActa.remove(actaId);

        registrarEvento(
                actaId,
                "PAGO_EN_APREMIO_REGISTRADO",
                BLOQUE_GESTION_EXTERNA,
                BLOQUE_D5,
                "Pago en apremio registrado; condena confirmada. "
                        + "Acta vuelve a análisis en condición de cierre.");

        return new PrototipoStore.RegistrarPagoEnApremioResultado(
                PrototipoStore.RegistrarPagoEnApremioEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial absolutoria. Solo aplica
     * si la acta está en {@code GESTION_EXTERNA} con
     * {@code tipoGestionExterna = JUZGADO_DE_PAZ} y
     * {@code permiteReingreso = true}. Establece resultado final ABSUELTO,
     * limpia {@code tipoGestionExterna} y deja el acta cerrable.
     */
    PrototipoStore.ReingresarDesdeJuzgadoResultado reingresarDesdeJuzgadoAbsuelto(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                    PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (!BANDEJA_GESTION_EXTERNA.equals(actual.bandejaActual())
                || !actual.permiteReingreso()
                || !PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ.equals(
                        tipoGestionExternaPorActa.get(actaId))) {
            return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                    PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT, null, null, null, null, null);
        }

        cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.remove(actaId);
        tipoGestionExternaPorActa.remove(actaId);

        registrarEvento(
                actaId,
                "RESOLUCION_JUZGADO_ABSUELVE",
                BLOQUE_GESTION_EXTERNA,
                BLOQUE_D5,
                "Resolución de Juzgado de Paz: absuelve. "
                        + "Acta vuelve a análisis con resultado ABSUELTO; en condición de cierre.");

        return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                PrototipoStore.ReingresarDesdeJuzgadoEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                null,
                "ABSUELVE");
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que confirma la condena
     * original. Solo aplica si la acta está en {@code GESTION_EXTERNA} con
     * {@code tipoGestionExterna = JUZGADO_DE_PAZ} y
     * {@code permiteReingreso = true}. Mantiene CONDENA_FIRME, limpia
     * {@code tipoGestionExterna} y deja el acta con pago de condena pendiente.
     */
    PrototipoStore.ReingresarDesdeJuzgadoResultado reingresarDesdeJuzgadoCondenaConfirmada(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                    PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (!BANDEJA_GESTION_EXTERNA.equals(actual.bandejaActual())
                || !actual.permiteReingreso()
                || !PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ.equals(
                        tipoGestionExternaPorActa.get(actaId))) {
            return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                    PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT, null, null, null, null, null);
        }

        cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA);
        tipoGestionExternaPorActa.remove(actaId);

        registrarEvento(
                actaId,
                "RESOLUCION_JUZGADO_CONFIRMA_CONDENA",
                BLOQUE_GESTION_EXTERNA,
                BLOQUE_D5,
                "Resolución de Juzgado de Paz: confirma condena. "
                        + "Acta vuelve a análisis con CONDENA_FIRME; pago de condena pendiente.");

        return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                PrototipoStore.ReingresarDesdeJuzgadoEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA,
                "CONFIRMA_CONDENA");
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que modifica el monto de
     * condena. Solo aplica si la acta está en {@code GESTION_EXTERNA} con
     * {@code tipoGestionExterna = JUZGADO_DE_PAZ}, {@code permiteReingreso = true}
     * y {@code nuevoMonto > 0}. Actualiza monto, mantiene CONDENA_FIRME,
     * limpia {@code tipoGestionExterna} y deja el acta con pago de condena
     * pendiente por el nuevo monto.
     */
    PrototipoStore.ReingresarDesdeJuzgadoResultado reingresarDesdeJuzgadoMontoModificado(
            String actaId, BigDecimal nuevoMonto) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                    PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (!BANDEJA_GESTION_EXTERNA.equals(actual.bandejaActual())
                || !actual.permiteReingreso()
                || !PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ.equals(
                        tipoGestionExternaPorActa.get(actaId))
                || nuevoMonto == null
                || nuevoMonto.signum() <= 0) {
            return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                    PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT, null, null, null, null, null);
        }

        montoCondenaPorActa.put(actaId, nuevoMonto);
        cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA);
        tipoGestionExternaPorActa.remove(actaId);

        registrarEvento(
                actaId,
                "RESOLUCION_JUZGADO_MODIFICA_MONTO",
                BLOQUE_GESTION_EXTERNA,
                BLOQUE_D5,
                "Resolución de Juzgado de Paz: modifica monto de condena a " + nuevoMonto + ". "
                        + "Acta vuelve a análisis con CONDENA_FIRME; pago de condena pendiente por nuevo monto.");

        return new PrototipoStore.ReingresarDesdeJuzgadoResultado(
                PrototipoStore.ReingresarDesdeJuzgadoEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA,
                "MODIFICA_MONTO");
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
