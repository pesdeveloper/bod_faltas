package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.ModoValorizacionItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Version del historial economico de un articulo infringido.
 * Mapeada a {@code fal_acta_valorizacion_item} en MariaDB.
 *
 * Hijo de {@link FalActaArticuloInfringido} (no de la cabecera
 * {@link FalActaValorizacion}): {@code fal_acta -> fal_acta_articulo_infringido ->
 * fal_acta_valorizacion_item} (version 1, version 2, ..., version actual).
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.5.
 *
 * Inmutable una vez creado: cada cambio manual o automatico crea una fila
 * nueva con {@code versionItem+1} y {@code siActual=true}; la anterior pasa a
 * {@code siActual=false} en el mismo movimiento transaccional (a cargo del
 * repositorio/servicio, no de esta clase).
 *
 * No duplica obligatoriamente el valor monetario automatico: la
 * reconstruccion usa {@code idTarifarioArticuloUsado + idValorUnidadUsado}.
 */
public class FalActaValorizacionItem {

    private final Long id;
    private final Long actaArticuloId;
    private final int versionItem;
    private boolean siActual;
    private final Long tarifarioArticuloUsadoId;
    private final Long valorUnidadUsadoId;
    private final ModoValorizacionItem modoOrdinaria;
    private final BigDecimal cantidadManualOrdinaria;
    private final BigDecimal montoManualOrdinario;
    private final ModoValorizacionItem modoPagoVoluntario;
    private final BigDecimal cantidadManualPagoVoluntario;
    private final BigDecimal montoManualPagoVoluntario;
    private final String motivoCambio;
    private final String observacionCambio;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaValorizacionItem(
            Long id,
            Long actaArticuloId,
            int versionItem,
            boolean siActual,
            Long tarifarioArticuloUsadoId,
            Long valorUnidadUsadoId,
            ModoValorizacionItem modoOrdinaria,
            BigDecimal cantidadManualOrdinaria,
            BigDecimal montoManualOrdinario,
            ModoValorizacionItem modoPagoVoluntario,
            BigDecimal cantidadManualPagoVoluntario,
            BigDecimal montoManualPagoVoluntario,
            String motivoCambio,
            String observacionCambio,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaArticuloId == null) throw new IllegalArgumentException("actaArticuloId es obligatorio");
        if (versionItem < 1) throw new IllegalArgumentException("versionItem debe ser >= 1");
        if (tarifarioArticuloUsadoId == null) throw new IllegalArgumentException("tarifarioArticuloUsadoId es obligatorio");
        if (modoOrdinaria == null) throw new IllegalArgumentException("modoOrdinaria es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");

        validarModo(modoOrdinaria, cantidadManualOrdinaria, montoManualOrdinario, "ordinaria");
        if (modoPagoVoluntario != null) {
            validarModo(modoPagoVoluntario, cantidadManualPagoVoluntario, montoManualPagoVoluntario, "pagoVoluntario");
        } else if (cantidadManualPagoVoluntario != null || montoManualPagoVoluntario != null) {
            throw new IllegalArgumentException("no puede informarse cantidad/monto de pago voluntario sin modoPagoVoluntario");
        }

        boolean esManual = modoOrdinaria.esManual() || (modoPagoVoluntario != null && modoPagoVoluntario.esManual());
        if (esManual && (motivoCambio == null || motivoCambio.isBlank())) {
            throw new IllegalArgumentException("motivoCambio es obligatorio cuando algun modo es manual");
        }

        this.id = id;
        this.actaArticuloId = actaArticuloId;
        this.versionItem = versionItem;
        this.siActual = siActual;
        this.tarifarioArticuloUsadoId = tarifarioArticuloUsadoId;
        this.valorUnidadUsadoId = valorUnidadUsadoId;
        this.modoOrdinaria = modoOrdinaria;
        this.cantidadManualOrdinaria = escala4(cantidadManualOrdinaria);
        this.montoManualOrdinario = escala2(montoManualOrdinario);
        this.modoPagoVoluntario = modoPagoVoluntario;
        this.cantidadManualPagoVoluntario = escala4(cantidadManualPagoVoluntario);
        this.montoManualPagoVoluntario = escala2(montoManualPagoVoluntario);
        this.motivoCambio = motivoCambio;
        this.observacionCambio = observacionCambio;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    private static void validarModo(ModoValorizacionItem modo, BigDecimal cantidadManual, BigDecimal montoManual, String dimension) {
        switch (modo) {
            case AUTOMATICA -> {
                if (cantidadManual != null || montoManual != null)
                    throw new IllegalArgumentException("modo AUTOMATICA de " + dimension + " no admite cantidad/monto manual");
            }
            case MANUAL_UNIDADES -> {
                if (cantidadManual == null || cantidadManual.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("cantidadManual" + capitalize(dimension) + " es obligatoria y positiva para MANUAL_UNIDADES");
                if (montoManual != null)
                    throw new IllegalArgumentException("montoManual" + capitalize(dimension) + " debe ser nulo para MANUAL_UNIDADES");
            }
            case MANUAL_MONTO -> {
                if (montoManual == null || montoManual.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("montoManual" + capitalize(dimension) + " es obligatorio y positivo para MANUAL_MONTO");
                if (cantidadManual != null)
                    throw new IllegalArgumentException("cantidadManual" + capitalize(dimension) + " debe ser nula para MANUAL_MONTO");
            }
        }
    }

    private static String capitalize(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static BigDecimal escala4(BigDecimal v) { return v == null ? null : v.setScale(4, RoundingMode.HALF_UP); }
    private static BigDecimal escala2(BigDecimal v) { return v == null ? null : v.setScale(2, RoundingMode.HALF_UP); }

    public Long getId() { return id; }
    public Long getActaArticuloId() { return actaArticuloId; }
    public int getVersionItem() { return versionItem; }
    public boolean isSiActual() { return siActual; }

    /**
     * Marca esta version como no-actual (bookkeeping puro: la versión que reemplaza
     * pasa a ser la actual). No modifica ningun otro campo: el historial economico
     * de esta fila permanece inmutable.
     */
    public void marcarNoActual() { this.siActual = false; }
    public Long getTarifarioArticuloUsadoId() { return tarifarioArticuloUsadoId; }
    public Long getValorUnidadUsadoId() { return valorUnidadUsadoId; }
    public ModoValorizacionItem getModoOrdinaria() { return modoOrdinaria; }
    public BigDecimal getCantidadManualOrdinaria() { return cantidadManualOrdinaria; }
    public BigDecimal getMontoManualOrdinario() { return montoManualOrdinario; }
    public ModoValorizacionItem getModoPagoVoluntario() { return modoPagoVoluntario; }
    public BigDecimal getCantidadManualPagoVoluntario() { return cantidadManualPagoVoluntario; }
    public BigDecimal getMontoManualPagoVoluntario() { return montoManualPagoVoluntario; }
    public String getMotivoCambio() { return motivoCambio; }
    public String getObservacionCambio() { return observacionCambio; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    /** Crea la siguiente version (actual) a partir de esta, marcando esta como no-actual la responsabilidad del llamador/repositorio. */
    public FalActaValorizacionItem siguienteVersion(
            Long nuevoId,
            Long tarifarioArticuloUsadoId,
            Long valorUnidadUsadoId,
            ModoValorizacionItem modoOrdinaria,
            BigDecimal cantidadManualOrdinaria,
            BigDecimal montoManualOrdinario,
            ModoValorizacionItem modoPagoVoluntario,
            BigDecimal cantidadManualPagoVoluntario,
            BigDecimal montoManualPagoVoluntario,
            String motivoCambio,
            String observacionCambio,
            LocalDateTime fhAlta,
            String idUserAlta) {
        return new FalActaValorizacionItem(
                nuevoId, actaArticuloId, versionItem + 1, true,
                tarifarioArticuloUsadoId, valorUnidadUsadoId,
                modoOrdinaria, cantidadManualOrdinaria, montoManualOrdinario,
                modoPagoVoluntario, cantidadManualPagoVoluntario, montoManualPagoVoluntario,
                motivoCambio, observacionCambio, fhAlta, idUserAlta);
    }
}
