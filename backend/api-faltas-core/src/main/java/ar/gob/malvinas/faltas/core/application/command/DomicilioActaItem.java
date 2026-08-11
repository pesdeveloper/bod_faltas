package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.ModoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Bloque tipado de domicilio informado en el alta integral, agregado en Wave 4 (alta integral
 * JDBC), según {@code DECISION-ACTA-DOMICILIOS-OPCIONALES-01}.
 *
 * <p>Se persiste como {@code fal_persona_domicilio} de la persona infractora resuelta. Si la
 * situación de identificación no resuelve persona, el alta ignora los domicilios informados: no
 * existe fila de dominio sin {@code persona_id}, y no se inventa una persona para satisfacerlo.
 *
 * <p>Todos los campos geográficos son opcionales salvo {@code tipoDomicilio}, {@code
 * origenDomicilio} y {@code modoDomicilio}, que el DDL exige {@code NOT NULL}.
 */
public record DomicilioActaItem(
        @NotNull TipoDomicilio tipoDomicilio,
        @NotNull OrigenDomicilio origenDomicilio,
        @NotNull ModoDomicilio modoDomicilio,
        boolean siNotificable,
        boolean siPrincipal,
        Short idProvincia,
        UnidadTerritorialTipo unidadTerritorialTipo,
        Integer idUnidadTerritorial,
        Long idLocalidad,
        Long idCalle,
        String idLocMalvinas,
        Long localidadMalvinasVersionId,
        String idTcaMalvinas,
        Long calleMalvinasVersionId,
        String calleTxt,
        Integer altura,
        boolean siSinAltura,
        String unidadFuncional,
        String codigoPostal,
        String domicilioTxt,
        BigDecimal lat,
        BigDecimal lon,
        OrigenUbicacion origenUbicacion
) {}
