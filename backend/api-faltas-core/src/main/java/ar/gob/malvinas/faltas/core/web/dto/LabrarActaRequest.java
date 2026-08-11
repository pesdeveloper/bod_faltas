package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.ContravencionActaItem;
import ar.gob.malvinas.faltas.core.application.command.DomicilioActaItem;
import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.application.command.MedidaPreventivaActaItem;
import ar.gob.malvinas.faltas.core.application.command.SustanciasAlimenticiasActaItem;
import ar.gob.malvinas.faltas.core.application.command.TransitoActaItem;
import ar.gob.malvinas.faltas.core.application.command.VehiculoActaItem;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Body de {@code POST /api/faltas/actas/labrar}.
 *
 * <p>{@code situacionIdentificacionInfractor} es obligatoria y sin default: el alta la
 * informa siempre ({@code DECISION_DDL-ACTA-INFR-01}).
 *
 * <p>{@code infractorDocumento} dejo de ser obligatorio. Exigirlo siempre hacia
 * imposible representar {@code PRESENTE_SE_NIEGA}, {@code PRESENTE_NO_APORTA_DATOS},
 * {@code AUSENTE} y {@code DATOS_INSUFICIENTES} sin inventar un documento ficticio. La
 * obligatoriedad pasa a ser condicional segun la matriz y la verifica
 * {@code ActaService.labrar} antes de la primera mutacion, con las excepciones de
 * dominio que el proyecto ya usa; no se introduce infraestructura de validacion nueva.
 *
 * <p>{@code tipoDocumentoInfractor} es el tipo documental canonico que acompana a
 * {@code infractorDocumento}. Sin el, el texto no es canonizable y no puede constituir
 * una identificacion completa.
 *
 * <p>{@code idTecnico} es opcional ({@code DECISION-ACTA-ID-TECNICO-QR-01}): el caso de
 * uso web habitualmente lo omite y el servidor genera un UUID nuevo; el caso de uso
 * movil offline lo informa con el UUID generado en el dispositivo al crear el borrador
 * local, y el servidor lo valida y lo conserva exactamente.
 *
 * <p>{@code domicilios} y {@code articulosInfringidos} son los bloques tipados de satélites
 * agregados en Wave 4 (alta integral JDBC): opcionales, se persisten dentro de la misma
 * transacción del alta cuando están informados. Ver Javadoc de {@code LabrarActaCommand}.
 *
 * <p>{@code vehiculo}, {@code transito}, {@code contravencion}, {@code sustanciasAlimenticias}
 * y {@code medidasPreventivas} son los seis satélites completos de Wave 4 Parte 3A, expuestos
 * por HTTP desde Wave 4 Parte 3C: sin ellos el contrato web nunca podía informar el bloque
 * obligatorio que {@code ActaService.validarAplicabilidadSatelitesPorTipoActa} exige siempre
 * ({@code DECISION-ACTA-SATELITES-STRICT-01}), aunque el comando de aplicación ya los soportaba
 * desde Parte 3A.
 */
public record LabrarActaRequest(
        @NotBlank String tipoActa,
        @NotBlank String idDependencia,
        @NotBlank String idInspector,
        LocalDate fechaActa,
        String domicilioHecho,
        String domicilioInfractor,
        String observaciones,
        Double latInfr,
        Double lonInfr,
        String infractorNombre,
        TipoDocumentoPersona tipoDocumentoInfractor,
        String infractorDocumento,
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
        List<MedidaPreventivaActaItem> medidasPreventivas
) {}
