package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.bandejaPermitePagoVoluntario;

/**
 * Soporte funcional del área presentaciones / pagos. Alcance del slice:
 * registrar la solicitud de pago voluntario originada por el infractor en
 * cualquier etapa interna operable del expediente.
 *
 * <p>Respaldo de dominio:
 * <ul>
 *   <li>{@code spec/03-bandejas/01-bandeja-labradas.md} admite "iniciar
 *       solicitud de pago voluntario" como acción inicial temprana;</li>
 *   <li>{@code spec/03-bandejas/02-bandeja-enriquecimiento.md} contempla
 *       como salida típica hacia "análisis / presentaciones / pagos" si
 *       surge una actuación que exige tratamiento material;</li>
 *   <li>{@code spec/03-bandejas/03-bandeja-analisis-presentaciones-pagos.md}
 *       es la bandeja principal candidata para agrupar expedientes que
 *       solicitaron o registraron pago voluntario.</li>
 * </ul>
 *
 * <p>Decisión funcional (único circuito verdadero): el infractor siempre
 * puede pagar mientras el expediente esté en una bandeja interna
 * operable. La precondición se delega en
 * {@link PrototipoConstantes#bandejaPermitePagoVoluntario}, que excluye
 * {@code ARCHIVO}, {@code CERRADAS} y {@code GESTION_EXTERNA}. Tras
 * registrar la solicitud, el acta queda en {@code PENDIENTE_ANALISIS}
 * con marca operativa
 * {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}, alineada con la
 * spec que centraliza la evaluación en análisis. No se modela cierre
 * automático por pago: la resolución posterior queda en análisis y puede
 * cerrarse con la acción genérica ya existente ({@link CierreSupport}).
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}).
 */
final class PagoVoluntarioSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa;
    private final Map<String, PrototipoStore.TipoPago> tipoPagoPorActa;
    private final Map<String, BigDecimal> montoPagoVoluntarioPorActa;

    PagoVoluntarioSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa,
            Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa,
            Map<String, PrototipoStore.TipoPago> tipoPagoPorActa,
            Map<String, BigDecimal> montoPagoVoluntarioPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.situacionPagoPorActa = situacionPagoPorActa;
        this.tipoPagoPorActa = tipoPagoPorActa;
        this.montoPagoVoluntarioPorActa = montoPagoVoluntarioPorActa;
    }

    /**
     * Acción administrativa "Pago voluntario": Dirección de Faltas fija el
     * monto del acta y deja el pago voluntario habilitado. El portal del
     * infractor todavía no existe en este prototipo; cuando exista, deberá
     * mostrar "Pagar" en lugar de "Solicitar pago voluntario" en cuanto
     * detecte que hay monto fijado.
     *
     * <p>Lleva al acta a {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}, dentro del
     * bloque {@code D5_ANALISIS} y estado {@code PENDIENTE_REVISION}.
     * Persiste {@code montoPagoVoluntario} y genera evento
     * {@code PAGO_VOLUNTARIO_SOLICITADO} (se reutiliza el tipo existente
     * para no impactar otros consumidores; la descripción aclara que
     * Dirección habilitó el pago con el monto fijado).
     *
     * <p>Reglas fuertes del prototipo: la acción NO genera referencias
     * de comprobantes (sin EM, sin RC, sin Cmte/Pref/Nro). La
     * materialización de comprobantes vive en el proceso externo de
     * pago, que se modelará en un slice posterior.
     *
     * <p>Devuelve {@code CONFLICT} si el acta está cerrada, en bandeja
     * terminal/externa ({@code ARCHIVO}, {@code CERRADAS},
     * {@code GESTION_EXTERNA}) o si la situación de pago no es
     * {@code SIN_PAGO}. La validación del monto (no nulo y &gt; 0) la
     * aplica el controller antes de invocar este handler.
     */
    PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado registrarSolicitudPagoVoluntario(
            String actaId, BigDecimal monto) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND,
                    null, null, null, null, null);
        }
        if (!bandejaPermitePagoVoluntario(actual.estaCerrada(), actual.bandejaActual())) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null, null);
        }
        PrototipoStore.SituacionPagoMock situacionActual = situacionPagoPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        if (situacionActual != PrototipoStore.SituacionPagoMock.SIN_PAGO) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null, null);
        }

        String bloqueOrigen = actual.bloqueActual();
        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO);
        situacionPagoPorActa.put(actaId, PrototipoStore.SituacionPagoMock.SOLICITADO);
        tipoPagoPorActa.put(actaId, PrototipoStore.TipoPago.VOLUNTARIO);
        montoPagoVoluntarioPorActa.put(actaId, monto);

        registrarEvento(
                actaId,
                "PAGO_VOLUNTARIO_SOLICITADO",
                bloqueOrigen,
                BLOQUE_D5,
                "Dirección de Faltas habilitó el pago voluntario con monto fijado "
                        + monto.toPlainString()
                        + "; expediente pasa a análisis. Sin generación de "
                        + "comprobantes (sin EM, sin RC, sin Cmte/Pref/Nro).");

        return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO,
                monto);
    }
    /**
     * Acción de Dirección de Faltas para fijar el monto de pago voluntario
     * cuando el infractor solicitó el pago pero todavía no hay monto fijado.
     *
     * <p>Precondiciones:
     * <ul>
     *   <li>Acta existe.</li>
     *   <li>Bandeja operable para pago voluntario.</li>
     *   <li>Situación de pago es {@code SOLICITADO} (el infractor solicitó
     *       pero Dirección aún no fijó monto).</li>
     *   <li>Monto estrictamente mayor a cero (validado en el controller).</li>
     * </ul>
     *
     * <p>Efectos:
     * <ul>
     *   <li>Persiste {@code montoPagoVoluntario}.</li>
     *   <li>Limpia {@code accionPendiente} si era
     *       {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}: la
     *       evaluación está hecha; el siguiente paso es del infractor.</li>
     *   <li>Registra evento {@code PAGO_VOLUNTARIO_MONTO_FIJADO}.</li>
     * </ul>
     *
     * <p>Devuelve {@code CONFLICT} si el acta está en bandeja terminal o si
     * la situación de pago no es {@code SOLICITADO}.
     */
    PrototipoStore.FijarMontoPagoVoluntarioResultado fijarMontoPagoVoluntario(
            String actaId, BigDecimal monto) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.FijarMontoPagoVoluntarioResultado(
                    PrototipoStore.FijarMontoPagoVoluntarioEstado.NOT_FOUND,
                    null, null, null, null);
        }
        if (!bandejaPermitePagoVoluntario(actual.estaCerrada(), actual.bandejaActual())) {
            return new PrototipoStore.FijarMontoPagoVoluntarioResultado(
                    PrototipoStore.FijarMontoPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null);
        }
        PrototipoStore.SituacionPagoMock situacionActual = situacionPagoPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        if (situacionActual != PrototipoStore.SituacionPagoMock.SOLICITADO) {
            return new PrototipoStore.FijarMontoPagoVoluntarioResultado(
                    PrototipoStore.FijarMontoPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null);
        }

        montoPagoVoluntarioPorActa.put(actaId, monto);

        // La evaluación está hecha: Dirección fijó el monto.
        // Se limpia accionPendiente para que el expediente quede sin marca operativa
        // hasta que el infractor informe el pago.
        String accionActual = accionPendientePorActa.get(actaId);
        if (PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO.equals(accionActual)) {
            accionPendientePorActa.remove(actaId);
        }

        registrarEvento(
                actaId,
                "PAGO_VOLUNTARIO_MONTO_FIJADO",
                actual.bloqueActual(),
                actual.bloqueActual(),
                "Direccion de Faltas fijo monto de pago voluntario: "
                        + monto.toPlainString()
                        + ". El infractor puede ahora informar el pago.");

        return new PrototipoStore.FijarMontoPagoVoluntarioResultado(
                PrototipoStore.FijarMontoPagoVoluntarioEstado.OK,
                actual.id(),
                actual.bandejaActual(),
                actual.estadoProcesoActual(),
                monto);
    }

    /**
     * Solicitud de pago voluntario iniciada por el infractor desde el portal.
     * Sin monto: Direccion de Faltas evaluara y, si corresponde, lo fijara.
     *
     * <p>Genera evento {@code PAGO_VOLUNTARIO_SOLICITADO} indicando el canal
     * {@code PORTAL_INFRACTOR}. El acta pasa a {@code PENDIENTE_ANALISIS} con
     * marca {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}. El monto
     * queda {@code null} hasta que Direccion lo fije.
     *
     * <p>Devuelve {@code CONFLICT} si el acta esta cerrada, en bandeja
     * terminal/externa o si la situacion de pago no es {@code SIN_PAGO}.
     */
    PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado solicitarPagoVoluntarioDesdePortal(
            String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND,
                    null, null, null, null, null);
        }
        if (!bandejaPermitePagoVoluntario(actual.estaCerrada(), actual.bandejaActual())) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null, null);
        }
        PrototipoStore.SituacionPagoMock situacionActual = situacionPagoPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        if (situacionActual != PrototipoStore.SituacionPagoMock.SIN_PAGO) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null, null);
        }

        String bloqueOrigen = actual.bloqueActual();
        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO);
        situacionPagoPorActa.put(actaId, PrototipoStore.SituacionPagoMock.SOLICITADO);
        tipoPagoPorActa.put(actaId, PrototipoStore.TipoPago.VOLUNTARIO);
        // Monto no fijado: el infractor solo expresa intencion de pago;
        // Direccion de Faltas evaluara y fijara el monto si corresponde.

        registrarEvento(
                actaId,
                "PAGO_VOLUNTARIO_SOLICITADO",
                bloqueOrigen,
                BLOQUE_D5,
                "Infractor solicito pago voluntario desde PORTAL_INFRACTOR; "
                        + "expediente pasa a analisis. Monto pendiente de fijacion "
                        + "por Direccion de Faltas.");

        return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO,
                null);
    }

    /**
     * Dirección de Faltas registra que el plazo/oportunidad de pago voluntario
     * venció sin que el infractor pagara.
     *
     * <p>Precondiciones:
     * <ul>
     *   <li>Acta existe.</li>
     *   <li>Bandeja operable para pago voluntario (excluye ARCHIVO, CERRADAS,
     *       GESTION_EXTERNA).</li>
     *   <li>Situación de pago es {@code SOLICITADO}: hay una solicitud activa
     *       sin pago confirmado.</li>
     *   <li>No hay resultado final distinto de {@code SIN_RESULTADO_FINAL}.</li>
     * </ul>
     *
     * <p>Efectos:
     * <ul>
     *   <li>Cambia {@code situacionPago} a {@link PrototipoStore.SituacionPagoMock#VENCIDO}.</li>
     *   <li>Limpia {@code accionPendiente} si era
     *       {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}: la evaluación
     *       terminó con vencimiento.</li>
     *   <li>Conserva {@code montoPagoVoluntario} como dato histórico.</li>
     *   <li>Registra evento {@code PAGO_VOLUNTARIO_VENCIDO}.</li>
     * </ul>
     *
     * <p>Devuelve {@code CONFLICT} si el acta está cerrada, en bandeja terminal,
     * si la situación de pago no es {@code SOLICITADO}, o si el resultado final
     * es distinto de {@code SIN_RESULTADO_FINAL} (fallo ya dictado/notificado).
     */
    PrototipoStore.RegistrarVencimientoPagoVoluntarioResultado registrarVencimientoPagoVoluntario(
            String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarVencimientoPagoVoluntarioResultado(
                    PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.NOT_FOUND,
                    null, null, null, null, null);
        }
        if (!bandejaPermitePagoVoluntario(actual.estaCerrada(), actual.bandejaActual())) {
            return new PrototipoStore.RegistrarVencimientoPagoVoluntarioResultado(
                    PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null, null);
        }
        PrototipoStore.SituacionPagoMock situacionActual = situacionPagoPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        if (situacionActual != PrototipoStore.SituacionPagoMock.SOLICITADO) {
            return new PrototipoStore.RegistrarVencimientoPagoVoluntarioResultado(
                    PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.CONFLICT,
                    null, null, null, situacionActual, null);
        }

        situacionPagoPorActa.put(actaId, PrototipoStore.SituacionPagoMock.VENCIDO);

        String accionActual = accionPendientePorActa.get(actaId);
        if (PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO.equals(accionActual)) {
            accionPendientePorActa.remove(actaId);
        }

        java.math.BigDecimal monto = montoPagoVoluntarioPorActa.get(actaId);
        String descripcion = "Direccion de Faltas registro vencimiento de pago voluntario sin pago."
                + (monto != null ? " Monto historico: " + monto.toPlainString() + "." : "")
                + " El tramite puede continuar a fallo de fondo.";

        registrarEvento(
                actaId,
                "PAGO_VOLUNTARIO_VENCIDO",
                actual.bloqueActual(),
                actual.bloqueActual(),
                descripcion);

        return new PrototipoStore.RegistrarVencimientoPagoVoluntarioResultado(
                PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.OK,
                actual.id(),
                actual.bandejaActual(),
                actual.estadoProcesoActual(),
                PrototipoStore.SituacionPagoMock.VENCIDO,
                monto);
    }

    /**
     * El infractor confirma su intención de pago desde el portal.
     * Precondiciones: acta operable, monto fijado por Dirección,
     * situacionPago == SOLICITADO u OBSERVADO.
     * Efecto: situacionPago = PENDIENTE_CONFIRMACION + tarea operativa
     * VERIFICAR_PAGO_INFORMADO para que Dirección pueda confirmar u observar.
     */
    PrototipoStore.InformarPagoVoluntarioDesdePortalResultado informarPagoVoluntarioDesdePortal(
            String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.InformarPagoVoluntarioDesdePortalResultado(
                    PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.NOT_FOUND,
                    null,
                    PrototipoStore.SituacionPagoMock.SIN_PAGO);
        }
        if (!bandejaPermitePagoVoluntario(actual.estaCerrada(), actual.bandejaActual())) {
            return new PrototipoStore.InformarPagoVoluntarioDesdePortalResultado(
                    PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT,
                    null,
                    situacionPagoPorActa.getOrDefault(actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO));
        }
        BigDecimal monto = montoPagoVoluntarioPorActa.get(actaId);
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return new PrototipoStore.InformarPagoVoluntarioDesdePortalResultado(
                    PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_SIN_MONTO,
                    null,
                    situacionPagoPorActa.getOrDefault(actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO));
        }
        PrototipoStore.SituacionPagoMock situacionActual = situacionPagoPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        if (situacionActual == PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION) {
            return new PrototipoStore.InformarPagoVoluntarioDesdePortalResultado(
                    PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_YA_INFORMADO,
                    null,
                    situacionActual);
        }
        if (situacionActual == PrototipoStore.SituacionPagoMock.CONFIRMADO) {
            return new PrototipoStore.InformarPagoVoluntarioDesdePortalResultado(
                    PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_YA_CONFIRMADO,
                    null,
                    situacionActual);
        }
        if (situacionActual != PrototipoStore.SituacionPagoMock.SOLICITADO
                && situacionActual != PrototipoStore.SituacionPagoMock.OBSERVADO) {
            return new PrototipoStore.InformarPagoVoluntarioDesdePortalResultado(
                    PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT,
                    null,
                    situacionActual);
        }

        situacionPagoPorActa.put(actaId, PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION);
        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_VERIFICAR_PAGO_INFORMADO);

        registrarEvento(
                actaId,
                "PAGO_VOLUNTARIO_INFORMADO_PORTAL",
                actual.bloqueActual(),
                actual.bloqueActual(),
                "Infractor informo pago voluntario desde PORTAL_INFRACTOR con monto "
                        + monto.toPlainString()
                        + ". Pendiente de confirmacion por Direccion de Faltas.");

        return new PrototipoStore.InformarPagoVoluntarioDesdePortalResultado(
                PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.OK,
                actaId,
                PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION);
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
