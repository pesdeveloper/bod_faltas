package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Formula economica de un articulo normativo: cantidad de unidades (o monto fijo)
 * ordinaria y de pago voluntario. Mapeada a {@code fal_tarifario_unidad_faltas}.
 *
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.3: el tarifario es POR ARTICULO
 * (antes era global por {@link TipoUnidadFaltas}, sin persistencia JDBC real —
 * incompatibilidad documentada en CHECKPOINT.md de Wave 4 §3). La valorizacion
 * ordinaria y el pago voluntario usan el MISMO {@code tipoUnidad}; lo que cambia
 * es la cantidad (o, para MONTO, el monto fijo).
 *
 * Reglas:
 * <pre>
 * SALARIO / UNIDAD_FIJA: cantidadUnidadesOrdinaria obligatoria; montosFijos nulos.
 * MONTO: montoFijoOrdinario obligatorio; cantidades nulas.
 * siAdmitePagoVoluntario=true: exige tambien la contraparte de pago voluntario
 * (cantidad o monto segun tipoUnidad).
 * </pre>
 *
 * Cuando cambia la formula del articulo: se cierra la vigencia de esta fila y
 * se crea una nueva activa (nunca se edita in-place una fila ya vigente).
 */
public class FalTarifarioUnidadFaltas {

    private final Long id;
    private final Long articuloId;
    private final TipoUnidadFaltas tipoUnidad;
    private final BigDecimal cantidadUnidadesOrdinaria;
    private BigDecimal cantidadUnidadesPagoVoluntario;
    private final BigDecimal montoFijoOrdinario;
    private BigDecimal montoFijoPagoVoluntario;
    private final boolean siAdmitePagoVoluntario;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalTarifarioUnidadFaltas(
            Long id,
            Long articuloId,
            TipoUnidadFaltas tipoUnidad,
            BigDecimal cantidadUnidadesOrdinaria,
            BigDecimal montoFijoOrdinario,
            boolean siAdmitePagoVoluntario,
            BigDecimal cantidadUnidadesPagoVoluntario,
            BigDecimal montoFijoPagoVoluntario,
            LocalDate fhVigDesde,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (articuloId == null) throw new IllegalArgumentException("articuloId es obligatorio");
        if (tipoUnidad == null) throw new IllegalArgumentException("tipoUnidad es obligatorio");
        if (fhVigDesde == null) throw new IllegalArgumentException("fhVigDesde es obligatoria");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");

        if (tipoUnidad == TipoUnidadFaltas.MONTO) {
            if (montoFijoOrdinario == null || montoFijoOrdinario.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("montoFijoOrdinario debe ser mayor a cero para tipoUnidad=MONTO");
            if (cantidadUnidadesOrdinaria != null)
                throw new IllegalArgumentException("cantidadUnidadesOrdinaria debe ser nula para tipoUnidad=MONTO");
        } else {
            if (cantidadUnidadesOrdinaria == null || cantidadUnidadesOrdinaria.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("cantidadUnidadesOrdinaria debe ser mayor a cero para tipoUnidad=" + tipoUnidad);
            if (montoFijoOrdinario != null)
                throw new IllegalArgumentException("montoFijoOrdinario debe ser nulo para tipoUnidad=" + tipoUnidad);
        }

        if (siAdmitePagoVoluntario) {
            if (tipoUnidad == TipoUnidadFaltas.MONTO) {
                if (montoFijoPagoVoluntario == null || montoFijoPagoVoluntario.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("montoFijoPagoVoluntario es obligatorio cuando siAdmitePagoVoluntario=true y tipoUnidad=MONTO");
            } else {
                if (cantidadUnidadesPagoVoluntario == null || cantidadUnidadesPagoVoluntario.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("cantidadUnidadesPagoVoluntario es obligatoria cuando siAdmitePagoVoluntario=true y tipoUnidad=" + tipoUnidad);
            }
        } else if (cantidadUnidadesPagoVoluntario != null || montoFijoPagoVoluntario != null) {
            throw new IllegalArgumentException("no puede informarse pago voluntario cuando siAdmitePagoVoluntario=false");
        }

        this.id = id;
        this.articuloId = articuloId;
        this.tipoUnidad = tipoUnidad;
        this.cantidadUnidadesOrdinaria = escala4(cantidadUnidadesOrdinaria);
        this.montoFijoOrdinario = escala2(montoFijoOrdinario);
        this.siAdmitePagoVoluntario = siAdmitePagoVoluntario;
        this.cantidadUnidadesPagoVoluntario = escala4(cantidadUnidadesPagoVoluntario);
        this.montoFijoPagoVoluntario = escala2(montoFijoPagoVoluntario);
        this.fhVigDesde = fhVigDesde;
        this.siActiva = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    private static BigDecimal escala4(BigDecimal v) { return v == null ? null : v.setScale(4, RoundingMode.HALF_UP); }
    private static BigDecimal escala2(BigDecimal v) { return v == null ? null : v.setScale(2, RoundingMode.HALF_UP); }

    public Long getId() { return id; }
    public Long getArticuloId() { return articuloId; }
    public TipoUnidadFaltas getTipoUnidad() { return tipoUnidad; }
    public BigDecimal getCantidadUnidadesOrdinaria() { return cantidadUnidadesOrdinaria; }
    public BigDecimal getMontoFijoOrdinario() { return montoFijoOrdinario; }
    public boolean isSiAdmitePagoVoluntario() { return siAdmitePagoVoluntario; }
    public BigDecimal getCantidadUnidadesPagoVoluntario() { return cantidadUnidadesPagoVoluntario; }
    public BigDecimal getMontoFijoPagoVoluntario() { return montoFijoPagoVoluntario; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }

    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate fhVigHasta) {
        if (fhVigHasta != null && fhVigHasta.isBefore(fhVigDesde))
            throw new IllegalArgumentException("fhVigHasta no puede ser anterior a fhVigDesde");
        this.fhVigHasta = fhVigHasta;
    }

    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    /**
     * Vigencia HISTORICA por rango de fechas, independiente de {@code siActiva}.
     * Para el valor OPERATIVO actual usar el repositorio (lookup por {@code si_activa}).
     */
    public boolean esVigenteEn(LocalDate fecha) {
        if (fhVigDesde.isAfter(fecha)) return false;
        return fhVigHasta == null || fecha.isBefore(fhVigHasta);
    }

    /** Cantidad o monto fijo ordinario efectivo segun tipoUnidad, para el calculo automatico. */
    public BigDecimal valorBaseOrdinario() {
        return tipoUnidad == TipoUnidadFaltas.MONTO ? montoFijoOrdinario : cantidadUnidadesOrdinaria;
    }

    /** Cantidad o monto fijo de pago voluntario efectivo segun tipoUnidad; null si no admite. */
    public BigDecimal valorBasePagoVoluntario() {
        if (!siAdmitePagoVoluntario) return null;
        return tipoUnidad == TipoUnidadFaltas.MONTO ? montoFijoPagoVoluntario : cantidadUnidadesPagoVoluntario;
    }

    public FalTarifarioUnidadFaltas copia() {
        FalTarifarioUnidadFaltas c = new FalTarifarioUnidadFaltas(
                id, articuloId, tipoUnidad, cantidadUnidadesOrdinaria, montoFijoOrdinario,
                siAdmitePagoVoluntario, cantidadUnidadesPagoVoluntario, montoFijoPagoVoluntario,
                fhVigDesde, fhAlta, idUserAlta);
        c.fhVigHasta = this.fhVigHasta;
        c.siActiva = this.siActiva;
        return c;
    }
}
