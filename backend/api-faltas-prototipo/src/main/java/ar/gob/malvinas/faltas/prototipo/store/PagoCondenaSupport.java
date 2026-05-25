package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Circuito mock de pago de condena posterior a CONDENA_FIRME.
 * No comparte monto, situacion ni acciones con pago voluntario.
 */
final class PagoCondenaSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa;
    private final Map<String, BigDecimal> montoCondenaPorActa;
    private final CerrabilidadSupport cerrabilidad;

    PagoCondenaSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa,
            Map<String, BigDecimal> montoCondenaPorActa,
            CerrabilidadSupport cerrabilidad) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.situacionPagoCondenaPorActa = situacionPagoCondenaPorActa;
        this.montoCondenaPorActa = montoCondenaPorActa;
        this.cerrabilidad = cerrabilidad;
    }

    PrototipoStore.PagoCondenaResultado informarPagoCondena(String actaId) {
        PrototipoStore.PagoCondenaPrecondicion pre = validarPrecondicionComun(actaId);
        if (pre.estado() != PrototipoStore.PagoCondenaEstado.OK) {
            return pre.resultado();
        }
        PrototipoStore.SituacionPagoCondena situacion = pre.situacion();
        if (situacion != PrototipoStore.SituacionPagoCondena.PENDIENTE
                && situacion != PrototipoStore.SituacionPagoCondena.OBSERVADO) {
            return resultado(PrototipoStore.PagoCondenaEstado.CONFLICT, actaId, situacion);
        }
        situacionPagoCondenaPorActa.put(actaId, PrototipoStore.SituacionPagoCondena.INFORMADO);
        registrarEvento(
                actaId,
                "PAGO_CONDENA_INFORMADO",
                "Pago de condena informado/iniciado en demo; sin comprobante real ni EM/RC/Cmte/Pref/Nro.");
        return resultado(PrototipoStore.PagoCondenaEstado.OK, actaId, PrototipoStore.SituacionPagoCondena.INFORMADO);
    }

    PrototipoStore.PagoCondenaResultado confirmarPagoCondena(String actaId) {
        PrototipoStore.PagoCondenaPrecondicion pre = validarPrecondicionComun(actaId);
        if (pre.estado() != PrototipoStore.PagoCondenaEstado.OK) {
            return pre.resultado();
        }
        if (pre.situacion() != PrototipoStore.SituacionPagoCondena.INFORMADO) {
            return resultado(PrototipoStore.PagoCondenaEstado.CONFLICT, actaId, pre.situacion());
        }
        situacionPagoCondenaPorActa.put(actaId, PrototipoStore.SituacionPagoCondena.CONFIRMADO);
        registrarEvento(
                actaId,
                "PAGO_CONDENA_CONFIRMADO",
                "Pago de condena confirmado en demo; habilita cerrabilidad si no hay bloqueantes materiales.");
        return resultado(PrototipoStore.PagoCondenaEstado.OK, actaId, PrototipoStore.SituacionPagoCondena.CONFIRMADO);
    }

    PrototipoStore.PagoCondenaResultado observarPagoCondena(String actaId) {
        PrototipoStore.PagoCondenaPrecondicion pre = validarPrecondicionComun(actaId);
        if (pre.estado() != PrototipoStore.PagoCondenaEstado.OK) {
            return pre.resultado();
        }
        if (pre.situacion() != PrototipoStore.SituacionPagoCondena.INFORMADO) {
            return resultado(PrototipoStore.PagoCondenaEstado.CONFLICT, actaId, pre.situacion());
        }
        situacionPagoCondenaPorActa.put(actaId, PrototipoStore.SituacionPagoCondena.OBSERVADO);
        registrarEvento(
                actaId,
                "PAGO_CONDENA_OBSERVADO",
                "Pago de condena observado en demo; vuelve a admitir nuevo informe o derivacion externa.");
        return resultado(PrototipoStore.PagoCondenaEstado.OK, actaId, PrototipoStore.SituacionPagoCondena.OBSERVADO);
    }

    private PrototipoStore.PagoCondenaPrecondicion validarPrecondicionComun(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null) {
            return new PrototipoStore.PagoCondenaPrecondicion(
                    PrototipoStore.PagoCondenaEstado.NOT_FOUND,
                    PrototipoStore.SituacionPagoCondena.NO_APLICA);
        }
        if (acta.estaCerrada()
                || cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new PrototipoStore.PagoCondenaPrecondicion(
                    PrototipoStore.PagoCondenaEstado.CONFLICT,
                    PrototipoStore.SituacionPagoCondena.NO_APLICA);
        }
        BigDecimal monto = montoCondenaPorActa.get(actaId);
        if (monto == null || monto.signum() <= 0) {
            return new PrototipoStore.PagoCondenaPrecondicion(
                    PrototipoStore.PagoCondenaEstado.CONFLICT,
                    situacionPagoCondenaPorActa.getOrDefault(
                            actaId, PrototipoStore.SituacionPagoCondena.PENDIENTE));
        }
        return new PrototipoStore.PagoCondenaPrecondicion(
                PrototipoStore.PagoCondenaEstado.OK,
                situacionPagoCondenaPorActa.getOrDefault(
                        actaId, PrototipoStore.SituacionPagoCondena.PENDIENTE));
    }

    private PrototipoStore.PagoCondenaResultado resultado(
            PrototipoStore.PagoCondenaEstado estado,
            String actaId,
            PrototipoStore.SituacionPagoCondena situacion) {
        return new PrototipoStore.PagoCondenaResultado(estado, actaId, situacion);
    }

    private void registrarEvento(String actaId, String tipoEvento, String descripcion) {
        ActaMock acta = actas.get(actaId);
        String bloque = acta != null ? acta.bloqueActual() : null;
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(idEvento, actaId, fechaEvento, tipoEvento, bloque, bloque, descripcion));
    }
}
