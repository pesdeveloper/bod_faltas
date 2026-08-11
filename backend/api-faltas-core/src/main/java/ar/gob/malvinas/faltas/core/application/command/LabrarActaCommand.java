package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Comando canonico del alta del acta.
 *
 * <p>{@code situacionIdentificacionInfractor} es obligatoria y no admite valor por
 * defecto: es una calificacion juridica del acto ({@code DECISION_DDL-ACTA-INFR-01},
 * {@code 20-application/command-contracts.md}). No se infiere desde
 * {@code resultadoFirmaInfractor}, desde el nombre ni desde el documento.
 *
 * <p>{@code tipoDocumentoInfractor} acompana a {@code infractorDocumento} para que el
 * par pueda canonizarse con {@code DocumentoPersona.normalizar}. Sin tipo documental el
 * texto no es un documento canonizable y permanece literal en el campo de
 * compatibilidad del acta: inferir DNI porque el valor parece numerico esta prohibido
 * por {@code DECISION_DDL-PERS-01}.
 *
 * <p>{@code latInfr} y {@code lonInfr} son componentes independientes del record y viajan
 * juntas hasta {@code FalActa}. No tienen valor por defecto: una ubicacion no informada es
 * {@code null} en ambas, nunca {@code 0}, y no se sustituye una por la otra.
 *
 * <p>{@code idTecnico} es opcional ({@code DECISION-ACTA-ID-TECNICO-QR-01},
 * {@code 20-application/command-contracts.md}): ausente, el servidor genera un UUID
 * nuevo; presente, el servidor lo valida y lo usa exactamente, sin regenerarlo. No es
 * una segunda clave de idempotencia.
 *
 * <p>{@code domicilios} y {@code articulosInfringidos} son los primeros bloques tipados de
 * satélites del alta integral (Wave 4, {@code DECISION-ACTA-SATELITES-ATOMICIDAD-01}),
 * agregados en paralelo con sus adapters JDBC según la secuenciación decidida en Wave 3.
 * {@code vehiculo}, {@code transito}, {@code contravencion}, {@code sustanciasAlimenticias} y
 * {@code medidasPreventivas} son los seis satélites completos de Wave 4 Parte 3A. Todos son
 * opcionales (nulos o lista vacía por defecto vía los constructores de compatibilidad): su
 * ausencia nunca bloquea el alta, solo omite ese satélite. {@code domicilios} solo se persiste
 * cuando la situación de identificación resolvió una persona infractora; sin persona no hay
 * {@code persona_id} al que anclar el domicilio, y no se inventa una persona para satisfacerlo.
 * {@code medidasPreventivas} referencia artículos por su clave natural de catálogo
 * ({@code articuloId}), nunca por posición de lista.
 */
public record LabrarActaCommand(
        @NotNull TipoActa tipoActa,
        Long idDependencia,
        Long idInspector,
        LocalDate fechaActa,
        String domicilioHecho,
        String domicilioInfractor,
        String observaciones,
        Double latInfr,
        Double lonInfr,
        String infractorNombre,
        TipoDocumentoPersona tipoDocumentoInfractor,
        String infractorDocumento,
        Long idPersonaInfractor,
        @NotNull ResultadoFirmaInfractor resultadoFirmaInfractor,
        @NotNull SituacionIdentificacionInfractor situacionIdentificacionInfractor,
        List<EvidenciaActaItem> evidenciasActa,
        String idTecnico,
        List<DomicilioActaItem> domicilios,
        List<ArticuloInfringidoItem> articulosInfringidos,
        VehiculoActaItem vehiculo,
        TransitoActaItem transito,
        ContravencionActaItem contravencion,
        SustanciasAlimenticiasActaItem sustanciasAlimenticias,
        List<MedidaPreventivaActaItem> medidasPreventivas,
        List<FotografiaEntradaAlta> fotografias
) {
    /**
     * Constructor de compatibilidad previo a las fotografias del alta (Wave 4 Parte 3D,
     * 23 componentes). Delega con lista vacia: ningun llamador anterior a esta parte incorpora
     * fotografias durante el alta. {@code fotografias} viaja como bytes crudos (sin procesar);
     * el pipeline de normalizacion y persistencia lo ejecuta
     * {@code FotografiaEvidenciaService.incorporarLoteAlta}, invocado desde
     * {@code ActaService.labrar} dentro de la misma transaccion, nunca aca.
     */
    public LabrarActaCommand(
            TipoActa tipoActa,
            Long idDependencia,
            Long idInspector,
            LocalDate fechaActa,
            String domicilioHecho,
            String domicilioInfractor,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            String infractorNombre,
            TipoDocumentoPersona tipoDocumentoInfractor,
            String infractorDocumento,
            Long idPersonaInfractor,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            SituacionIdentificacionInfractor situacionIdentificacionInfractor,
            List<EvidenciaActaItem> evidenciasActa,
            String idTecnico,
            List<DomicilioActaItem> domicilios,
            List<ArticuloInfringidoItem> articulosInfringidos,
            VehiculoActaItem vehiculo,
            TransitoActaItem transito,
            ContravencionActaItem contravencion,
            SustanciasAlimenticiasActaItem sustanciasAlimenticias,
            List<MedidaPreventivaActaItem> medidasPreventivas) {
        this(tipoActa, idDependencia, idInspector, fechaActa, domicilioHecho, domicilioInfractor,
                observaciones, latInfr, lonInfr, infractorNombre, tipoDocumentoInfractor,
                infractorDocumento, idPersonaInfractor, resultadoFirmaInfractor,
                situacionIdentificacionInfractor, evidenciasActa, idTecnico, domicilios,
                articulosInfringidos, vehiculo, transito, contravencion, sustanciasAlimenticias,
                medidasPreventivas, List.of());
    }

    /**
     * Constructor de compatibilidad previo a los seis satélites completos de Wave 4 Parte 3A
     * (19 componentes). Delega con {@code null}/lista vacía: ningún llamador anterior a esta
     * parte persiste vehículo, tránsito, contravención, sustancias alimenticias ni medidas
     * preventivas desde el comando de alta.
     */
    public LabrarActaCommand(
            TipoActa tipoActa,
            Long idDependencia,
            Long idInspector,
            LocalDate fechaActa,
            String domicilioHecho,
            String domicilioInfractor,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            String infractorNombre,
            TipoDocumentoPersona tipoDocumentoInfractor,
            String infractorDocumento,
            Long idPersonaInfractor,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            SituacionIdentificacionInfractor situacionIdentificacionInfractor,
            List<EvidenciaActaItem> evidenciasActa,
            String idTecnico,
            List<DomicilioActaItem> domicilios,
            List<ArticuloInfringidoItem> articulosInfringidos) {
        this(tipoActa, idDependencia, idInspector, fechaActa, domicilioHecho, domicilioInfractor,
                observaciones, latInfr, lonInfr, infractorNombre, tipoDocumentoInfractor,
                infractorDocumento, idPersonaInfractor, resultadoFirmaInfractor,
                situacionIdentificacionInfractor, evidenciasActa, idTecnico, domicilios,
                articulosInfringidos, null, null, null, null, List.of());
    }

    /**
     * Constructor de compatibilidad previo a los bloques de satélites de Wave 4 (17
     * componentes). Delega con listas vacías: ningún llamador anterior a esta wave persiste
     * domicilios ni artículos infringidos desde el comando de alta.
     */
    public LabrarActaCommand(
            TipoActa tipoActa,
            Long idDependencia,
            Long idInspector,
            LocalDate fechaActa,
            String domicilioHecho,
            String domicilioInfractor,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            String infractorNombre,
            TipoDocumentoPersona tipoDocumentoInfractor,
            String infractorDocumento,
            Long idPersonaInfractor,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            SituacionIdentificacionInfractor situacionIdentificacionInfractor,
            List<EvidenciaActaItem> evidenciasActa,
            String idTecnico) {
        this(tipoActa, idDependencia, idInspector, fechaActa, domicilioHecho, domicilioInfractor,
                observaciones, latInfr, lonInfr, infractorNombre, tipoDocumentoInfractor,
                infractorDocumento, idPersonaInfractor, resultadoFirmaInfractor,
                situacionIdentificacionInfractor, evidenciasActa, idTecnico, List.of(), List.of());
    }

    /**
     * Constructor de compatibilidad con los llamadores tipados previos a
     * {@code idTecnico} (16 componentes). Delega con {@code idTecnico = null}: el
     * servidor genera un UUID nuevo, igual que el comportamiento anterior a esta
     * decision.
     */
    public LabrarActaCommand(
            TipoActa tipoActa,
            Long idDependencia,
            Long idInspector,
            LocalDate fechaActa,
            String domicilioHecho,
            String domicilioInfractor,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            String infractorNombre,
            TipoDocumentoPersona tipoDocumentoInfractor,
            String infractorDocumento,
            Long idPersonaInfractor,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            SituacionIdentificacionInfractor situacionIdentificacionInfractor,
            List<EvidenciaActaItem> evidenciasActa) {
        this(tipoActa, idDependencia, idInspector, fechaActa, domicilioHecho, domicilioInfractor,
                observaciones, latInfr, lonInfr, infractorNombre, tipoDocumentoInfractor,
                infractorDocumento, idPersonaInfractor, resultadoFirmaInfractor,
                situacionIdentificacionInfractor, evidenciasActa, null);
    }

    /**
     * Constructor de compatibilidad con los llamadores que expresan tipo de acta,
     * dependencia e inspector como String, sin {@code idTecnico} informado.
     *
     * <p>La situacion viaja explicita igual que en el record: no hay sobrecarga que la
     * complete por defecto. {@code latInfr} y {@code lonInfr} se delegan ambas, cada una a
     * su propio componente.
     */
    public LabrarActaCommand(
            String tipoActa,
            String idDependencia,
            String idInspector,
            LocalDate fechaActa,
            String domicilioHecho,
            String domicilioInfractor,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            String infractorNombre,
            TipoDocumentoPersona tipoDocumentoInfractor,
            String infractorDocumento,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            SituacionIdentificacionInfractor situacionIdentificacionInfractor,
            List<EvidenciaActaItem> evidenciasActa) {
        this(TipoActa.valueOf(tipoActa),
                parseLongId(idDependencia),
                parseLongId(idInspector),
                fechaActa, domicilioHecho, domicilioInfractor, observaciones, latInfr, lonInfr,
                infractorNombre, tipoDocumentoInfractor, infractorDocumento, null,
                resultadoFirmaInfractor, situacionIdentificacionInfractor, evidenciasActa, null);
    }

    /** {@code domicilios()} nunca es {@code null}: los constructores de compatibilidad delegan lista vacía. */
    public List<DomicilioActaItem> domiciliosSeguro() {
        return domicilios == null ? List.of() : domicilios;
    }

    /** {@code articulosInfringidos()} nunca es {@code null}: los constructores de compatibilidad delegan lista vacía. */
    public List<ArticuloInfringidoItem> articulosInfringidosSeguro() {
        return articulosInfringidos == null ? List.of() : articulosInfringidos;
    }

    /** {@code medidasPreventivas()} nunca es {@code null}: los constructores de compatibilidad delegan lista vacía. */
    public List<MedidaPreventivaActaItem> medidasPreventivasSeguro() {
        return medidasPreventivas == null ? List.of() : medidasPreventivas;
    }

    /** {@code fotografias()} nunca es {@code null}: los constructores de compatibilidad delegan lista vacía. */
    public List<FotografiaEntradaAlta> fotografiasSeguro() {
        return fotografias == null ? List.of() : fotografias;
    }

    /**
     * Copia este comando con {@code idTecnico} reemplazado, sin alterar ningún otro campo
     * (Wave 4 Parte 3E, {@code AltaActaRetryPolicy}). Existe para que el orquestador de
     * reintento del alta pueda fijar un {@code id_tecnico} único <b>antes</b> del primer
     * intento y reutilizar exactamente el mismo valor en los reintentos subsiguientes: sin
     * esta copia, un comando sin {@code idTecnico} informado generaría un UUID distinto en
     * cada intento dentro de {@code ActaService.labrar}, rompiendo la idempotencia del retry.
     */
    public LabrarActaCommand conIdTecnico(String nuevoIdTecnico) {
        return new LabrarActaCommand(
                tipoActa, idDependencia, idInspector, fechaActa, domicilioHecho, domicilioInfractor,
                observaciones, latInfr, lonInfr, infractorNombre, tipoDocumentoInfractor,
                infractorDocumento, idPersonaInfractor, resultadoFirmaInfractor,
                situacionIdentificacionInfractor, evidenciasActa, nuevoIdTecnico, domicilios,
                articulosInfringidos, vehiculo, transito, contravencion, sustanciasAlimenticias,
                medidasPreventivas, fotografias);
    }

    private static Long parseLongId(String s) {
        if (s == null) return 1L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 1L; }
    }
}
