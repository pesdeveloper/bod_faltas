package ar.gob.malvinas.faltas.prototipo.domain;

import java.time.LocalDateTime;

public record ActaNotificacionMock(
        String id,
        String actaId,
        String canal,
        String estadoNotificacion,
        String destinatarioResumen,
        TipoNotificacion tipo,
        CanalNotificacion canalTipificado,
        EstadoNotificacion estado,
        ResultadoNotificacion resultado,
        String referencia,
        String eventoRelacionado,
        String loteId,
        String referenciaExterna,
        LocalDateTime fechaPreparacion,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaResultado,
        String observacion,
        String destinatarioNombre,
        String destinatarioEmail,
        String domicilioTexto,
        Boolean domicilioElectronicoVerificado,
        Integer diasPlazoNotificacionElectronica) {

    public ActaNotificacionMock(
            String id,
            String actaId,
            String canal,
            String estadoNotificacion,
            String destinatarioResumen) {
        this(
                id,
                actaId,
                canal,
                estadoNotificacion,
                destinatarioResumen,
                TipoNotificacion.ACTA_INFRACCION,
                canalTipificadoDesdeLegacy(canal),
                estadoDesdeLegacy(estadoNotificacion),
                resultadoDesdeLegacy(estadoNotificacion),
                null,
                null,
                null,
                null,
                null,
                null,
                resultadoDesdeLegacy(estadoNotificacion) == ResultadoNotificacion.SIN_RESULTADO
                        ? null
                        : LocalDateTime.now(),
                null,
                destinatarioResumen,
                null,
                null,
                null,
                null);
    }

    public static ActaNotificacionMock preparada(
            String id,
            String actaId,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String descripcion,
            String destinatarioNombre,
            String domicilioTexto,
            LocalDateTime fechaPreparacion,
            String eventoRelacionado) {
        return new ActaNotificacionMock(
                id,
                actaId,
                canalLegacy(canal),
                "PENDIENTE_ENVIO",
                descripcion,
                tipo,
                canal,
                EstadoNotificacion.LISTA_PARA_ENVIO,
                ResultadoNotificacion.SIN_RESULTADO,
                descripcion,
                eventoRelacionado,
                null,
                null,
                fechaPreparacion,
                null,
                null,
                null,
                destinatarioNombre,
                null,
                domicilioTexto,
                null,
                null);
    }

    public ActaNotificacionMock conResultado(
            EstadoNotificacion nuevoEstado,
            ResultadoNotificacion nuevoResultado,
            String nuevoEstadoLegacy,
            String nuevaDescripcion,
            LocalDateTime fecha,
            String eventoRelacionado) {
        return new ActaNotificacionMock(
                id,
                actaId,
                canal,
                nuevoEstadoLegacy,
                nuevaDescripcion,
                tipo,
                canalTipificado,
                nuevoEstado,
                nuevoResultado,
                nuevaDescripcion,
                eventoRelacionado,
                loteId,
                referenciaExterna,
                fechaPreparacion,
                fechaEnvio,
                fecha,
                observacion,
                destinatarioNombre,
                destinatarioEmail,
                domicilioTexto,
                domicilioElectronicoVerificado,
                diasPlazoNotificacionElectronica);
    }

    public ActaNotificacionMock conLoteCorreo(String nuevoLoteId, String nuevaReferenciaExterna, LocalDateTime fecha) {
        return new ActaNotificacionMock(
                id,
                actaId,
                canal,
                "EN_TRAMITE",
                destinatarioResumen,
                tipo,
                canalTipificado,
                EstadoNotificacion.ENVIADA,
                ResultadoNotificacion.SIN_RESULTADO,
                referencia,
                eventoRelacionado,
                nuevoLoteId,
                nuevaReferenciaExterna,
                fechaPreparacion,
                fecha,
                fechaResultado,
                observacion,
                destinatarioNombre,
                destinatarioEmail,
                domicilioTexto,
                domicilioElectronicoVerificado,
                diasPlazoNotificacionElectronica);
    }

    public ActaNotificacionMock conResultadoCorreo(
            EstadoNotificacion nuevoEstado,
            ResultadoNotificacion nuevoResultado,
            String nuevoEstadoLegacy,
            LocalDateTime fecha,
            String nuevaObservacion,
            String nuevoEventoRelacionado) {
        return new ActaNotificacionMock(
                id,
                actaId,
                canal,
                nuevoEstadoLegacy,
                destinatarioResumen,
                tipo,
                canalTipificado,
                nuevoEstado,
                nuevoResultado,
                referencia,
                nuevoEventoRelacionado,
                loteId,
                referenciaExterna,
                fechaPreparacion,
                fechaEnvio,
                fecha,
                nuevaObservacion,
                destinatarioNombre,
                destinatarioEmail,
                domicilioTexto,
                domicilioElectronicoVerificado,
                diasPlazoNotificacionElectronica);
    }

    public ActaNotificacionMock conVisualizacionPortal(LocalDateTime fecha, String nuevaObservacion) {
        return new ActaNotificacionMock(
                id,
                actaId,
                canal,
                "ENTREGADA",
                destinatarioResumen,
                tipo,
                canalTipificado,
                EstadoNotificacion.ENTREGADA,
                ResultadoNotificacion.POSITIVA,
                referencia,
                "NOTIFICACION_PORTAL_VISUALIZADA",
                loteId,
                referenciaExterna,
                fechaPreparacion,
                fechaEnvio,
                fecha,
                nuevaObservacion,
                destinatarioNombre,
                destinatarioEmail,
                domicilioTexto,
                Boolean.TRUE,
                diasPlazoNotificacionElectronica);
    }

    public ActaNotificacionMock conPreparacion(
            EstadoNotificacion nuevoEstado,
            String nuevoEstadoLegacy,
            String nuevaDescripcion,
            LocalDateTime fecha,
            String eventoRelacionado) {
        return new ActaNotificacionMock(
                id,
                actaId,
                canal,
                nuevoEstadoLegacy,
                nuevaDescripcion,
                tipo,
                canalTipificado,
                nuevoEstado,
                ResultadoNotificacion.SIN_RESULTADO,
                nuevaDescripcion,
                eventoRelacionado,
                loteId,
                referenciaExterna,
                fecha,
                null,
                null,
                observacion,
                destinatarioNombre,
                destinatarioEmail,
                domicilioTexto,
                domicilioElectronicoVerificado,
                diasPlazoNotificacionElectronica);
    }

    private static CanalNotificacion canalTipificadoDesdeLegacy(String canal) {
        if (canal == null) {
            return CanalNotificacion.CORREO_POSTAL;
        }
        return switch (canal) {
            case "EMAIL" -> CanalNotificacion.EMAIL;
            case "PRESENCIAL" -> CanalNotificacion.PRESENCIAL;
            case "DOMICILIO_ELECTRONICO" -> CanalNotificacion.DOMICILIO_ELECTRONICO;
            case "NOTIFICADOR_MUNICIPAL" -> CanalNotificacion.NOTIFICADOR_MUNICIPAL;
            default -> CanalNotificacion.CORREO_POSTAL;
        };
    }

    private static EstadoNotificacion estadoDesdeLegacy(String estado) {
        if (estado == null) {
            return EstadoNotificacion.PENDIENTE_PREPARACION;
        }
        return switch (estado) {
            case "PENDIENTE_ENVIO" -> EstadoNotificacion.LISTA_PARA_ENVIO;
            case "EN_TRAMITE" -> EstadoNotificacion.ENVIADA;
            case "ENTREGADA" -> EstadoNotificacion.ENTREGADA;
            case "NO_ENTREGADA" -> EstadoNotificacion.NEGATIVA;
            case "VENCIDA" -> EstadoNotificacion.VENCIDA;
            default -> EstadoNotificacion.PENDIENTE_PREPARACION;
        };
    }

    private static ResultadoNotificacion resultadoDesdeLegacy(String estado) {
        if ("ENTREGADA".equals(estado)) {
            return ResultadoNotificacion.POSITIVA;
        }
        if ("NO_ENTREGADA".equals(estado)) {
            return ResultadoNotificacion.NEGATIVA;
        }
        if ("VENCIDA".equals(estado)) {
            return ResultadoNotificacion.VENCIDA;
        }
        return ResultadoNotificacion.SIN_RESULTADO;
    }

    private static String canalLegacy(CanalNotificacion canal) {
        return canal == CanalNotificacion.CORREO_POSTAL ? "POSTAL" : canal.name();
    }
}
