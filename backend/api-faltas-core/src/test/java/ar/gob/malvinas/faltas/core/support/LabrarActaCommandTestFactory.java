package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.ContravencionActaItem;
import ar.gob.malvinas.faltas.core.application.command.DomicilioActaItem;
import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.application.command.FotografiaEntradaAlta;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.MedidaPreventivaActaItem;
import ar.gob.malvinas.faltas.core.application.command.SustanciasAlimenticiasActaItem;
import ar.gob.malvinas.faltas.core.application.command.TransitoActaItem;
import ar.gob.malvinas.faltas.core.application.command.VehiculoActaItem;
import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;

import java.time.LocalDate;
import java.util.List;

/**
 * Factory/builder central de {@link LabrarActaCommand} para tests (Wave 4 Parte 3C,
 * {@code DECISION-ACTA-SATELITES-STRICT-01}).
 *
 * <p>Desde que se eliminó el opt-in {@code optaPorSatelitesTipados}
 * ({@code DECISION-APLICABILIDAD-SATELITES-COMPATIBILIDAD-01}, revertida en esta parte), todo
 * comando que llega a {@code ActaService.labrar} debe informar el bloque de satélite
 * obligatorio de su {@code tipoActa} para no ser rechazado. Los ~37 archivos de test
 * históricos que usan el alta como precondición genérica (para llegar a un estado de
 * notificación, pago, apelación, fallo, etc.) no son "sobre satélites": esta factory les da un
 * único punto central para construir un alta mínima y válida, sin que cada archivo tenga que
 * conocer la matriz de aplicabilidad completa.
 *
 * <p>Los cuatro métodos {@code altaXxxValida()} son alta con datos deterministas
 * mínimos y el bloque obligatorio de la matriz de {@code ActaService
 * .validarAplicabilidadSatelitesPorTipoActa}. {@link Builder} permite sobrescribir cualquier
 * campo (fecha, domicilio, identificación del infractor, situación, resultado de firma,
 * idTecnico, evidencias, etc.) sin perder la validez base del bloque de satélite: los
 * overrides de campos ajenos al satélite nunca quitan el bloque obligatorio ya cargado.
 *
 * <p>Los tests que verifican intencionalmente un comando inválido (sin bloque obligatorio, con
 * bloque prohibido, alcoholemia incoherente, etc.) deben construirlo de forma explícita y
 * visible -- por ejemplo, con {@link Builder#sinSatelites()} o pasando {@code null} al bloque
 * correspondiente -- nunca a través de un bypass productivo.
 */
public final class LabrarActaCommandTestFactory {

    private LabrarActaCommandTestFactory() {}

    // -------------------------------------------------------------------------
    // Bloques de satélite mínimos y válidos, reutilizables por cualquier test.
    // -------------------------------------------------------------------------

    /** Tránsito mínimo, sin control de alcoholemia (coherente: siControlAlcoholemia=false, sin mediciones). */
    public static TransitoActaItem transitoMinimo() {
        return new TransitoActaItem(null, null, null, null, false, false, false, List.of());
    }

    /** Contravención/comercio mínima, con nomenclatura manual justificada y ámbito COMERCIO. */
    public static ContravencionActaItem contravencionMinima() {
        return new ContravencionActaItem(
                OrigenNomenclatura.MANUAL_EXCEPCIONAL, true,
                MotivoNomenclaturaManual.CONTINGENCIA_OPERATIVA,
                /* idSujI */ null, /* idBieI */ null, /* idSujC */ null, /* idBieC */ null,
                /* circ */ null, /* secc */ null, /* frac */ null, /* mza */ null,
                /* parc */ null, /* ufun */ null, /* ucomp */ null,
                /* rubroId */ null, /* idRub */ null,
                AmbitoCtv.COMERCIO, /* ambitoCtvTxt */ null);
    }

    /** Sustancias alimenticias/bromatología mínima, con descripción determinista. */
    public static SustanciasAlimenticiasActaItem sustanciasMinimas() {
        return new SustanciasAlimenticiasActaItem(null, null, AmbitoCtv.LOCAL, null, "sustancias de test");
    }

    /** Vehículo mínimo válido (satélite siempre condicional en los cuatro tipos). */
    public static VehiculoActaItem vehiculoValido() {
        return new VehiculoActaItem(
                "AB123CD", ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo.AUTO,
                null, "MARCA TEST", null, "MODELO TEST", (short) 2020, "Blanco",
                ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo.BUENO);
    }

    // -------------------------------------------------------------------------
    // Altas mínimas válidas por tipoActa (prompt Wave 4 Parte 3C §6.1).
    // -------------------------------------------------------------------------

    public static LabrarActaCommand altaTransitoValida() {
        return builder(TipoActa.TRANSITO).transito(transitoMinimo()).build();
    }

    public static LabrarActaCommand altaContravencionValida() {
        return builder(TipoActa.CONTRAVENCION).contravencion(contravencionMinima()).build();
    }

    public static LabrarActaCommand altaSustanciasValida() {
        return builder(TipoActa.SUSTANCIAS_ALIMENTICIAS).sustanciasAlimenticias(sustanciasMinimas()).build();
    }

    public static LabrarActaCommand altaComercioValida() {
        return builder(TipoActa.COMERCIO).contravencion(contravencionMinima()).build();
    }

    /** Punto de entrada del builder overridable, ya con el bloque obligatorio del tipo cargado. */
    public static Builder builder(TipoActa tipoActa) {
        Builder b = new Builder().tipoActa(tipoActa);
        switch (tipoActa) {
            case TRANSITO -> b.transito(transitoMinimo());
            case CONTRAVENCION, COMERCIO -> b.contravencion(contravencionMinima());
            case SUSTANCIAS_ALIMENTICIAS -> b.sustanciasAlimenticias(sustanciasMinimas());
        }
        return b;
    }

    /** Builder mutable y overridable de {@link LabrarActaCommand}. Ver clase contenedora. */
    public static final class Builder {
        private TipoActa tipoActa = TipoActa.TRANSITO;
        private Long idDependencia = 1L;
        private Long idInspector = 1L;
        private LocalDate fechaActa;
        private String domicilioHecho = "Dom. hecho de test";
        private String domicilioInfractor;
        private String observaciones;
        private Double latInfr;
        private Double lonInfr;
        private String infractorNombre;
        private TipoDocumentoPersona tipoDocumentoInfractor;
        private String infractorDocumento;
        private Long idPersonaInfractor;
        private ResultadoFirmaInfractor resultadoFirmaInfractor = ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR;
        private SituacionIdentificacionInfractor situacionIdentificacionInfractor =
                SituacionIdentificacionInfractor.DATOS_INSUFICIENTES;
        private List<EvidenciaActaItem> evidenciasActa;
        private String idTecnico;
        private List<DomicilioActaItem> domicilios;
        private List<ArticuloInfringidoItem> articulosInfringidos;
        private VehiculoActaItem vehiculo;
        private TransitoActaItem transito;
        private ContravencionActaItem contravencion;
        private SustanciasAlimenticiasActaItem sustanciasAlimenticias;
        private List<MedidaPreventivaActaItem> medidasPreventivas;
        private List<FotografiaEntradaAlta> fotografias;

        public Builder tipoActa(TipoActa v) { this.tipoActa = v; return this; }
        public Builder idDependencia(Long v) { this.idDependencia = v; return this; }
        public Builder idInspector(Long v) { this.idInspector = v; return this; }
        public Builder fechaActa(LocalDate v) { this.fechaActa = v; return this; }
        public Builder domicilioHecho(String v) { this.domicilioHecho = v; return this; }
        public Builder domicilioInfractor(String v) { this.domicilioInfractor = v; return this; }
        public Builder observaciones(String v) { this.observaciones = v; return this; }
        public Builder latInfr(Double v) { this.latInfr = v; return this; }
        public Builder lonInfr(Double v) { this.lonInfr = v; return this; }
        public Builder infractorNombre(String v) { this.infractorNombre = v; return this; }
        public Builder tipoDocumentoInfractor(TipoDocumentoPersona v) { this.tipoDocumentoInfractor = v; return this; }
        public Builder infractorDocumento(String v) { this.infractorDocumento = v; return this; }
        public Builder idPersonaInfractor(Long v) { this.idPersonaInfractor = v; return this; }
        public Builder resultadoFirmaInfractor(ResultadoFirmaInfractor v) { this.resultadoFirmaInfractor = v; return this; }
        public Builder situacionIdentificacionInfractor(SituacionIdentificacionInfractor v) { this.situacionIdentificacionInfractor = v; return this; }
        public Builder evidenciasActa(List<EvidenciaActaItem> v) { this.evidenciasActa = v; return this; }
        public Builder idTecnico(String v) { this.idTecnico = v; return this; }
        public Builder domicilios(List<DomicilioActaItem> v) { this.domicilios = v; return this; }
        public Builder articulosInfringidos(List<ArticuloInfringidoItem> v) { this.articulosInfringidos = v; return this; }
        public Builder vehiculo(VehiculoActaItem v) { this.vehiculo = v; return this; }
        public Builder transito(TransitoActaItem v) { this.transito = v; return this; }
        public Builder contravencion(ContravencionActaItem v) { this.contravencion = v; return this; }
        public Builder sustanciasAlimenticias(SustanciasAlimenticiasActaItem v) { this.sustanciasAlimenticias = v; return this; }
        public Builder medidasPreventivas(List<MedidaPreventivaActaItem> v) { this.medidasPreventivas = v; return this; }
        public Builder fotografias(List<FotografiaEntradaAlta> v) { this.fotografias = v; return this; }

        /**
         * Quita explícitamente los cuatro bloques de satélite tipados (deja el comando sin
         * ningún bloque informado). Uso exclusivo de tests que verifican intencionalmente el
         * rechazo por ausencia del bloque obligatorio -- nunca para "simplificar" un alta que
         * pretende ser válida.
         */
        public Builder sinSatelites() {
            this.vehiculo = null;
            this.transito = null;
            this.contravencion = null;
            this.sustanciasAlimenticias = null;
            this.medidasPreventivas = null;
            return this;
        }

        public LabrarActaCommand build() {
            return new LabrarActaCommand(
                    tipoActa, idDependencia, idInspector, fechaActa, domicilioHecho, domicilioInfractor,
                    observaciones, latInfr, lonInfr, infractorNombre, tipoDocumentoInfractor,
                    infractorDocumento, idPersonaInfractor, resultadoFirmaInfractor,
                    situacionIdentificacionInfractor, evidenciasActa, idTecnico, domicilios,
                    articulosInfringidos, vehiculo, transito, contravencion, sustanciasAlimenticias,
                    medidasPreventivas, fotografias);
        }
    }
}
