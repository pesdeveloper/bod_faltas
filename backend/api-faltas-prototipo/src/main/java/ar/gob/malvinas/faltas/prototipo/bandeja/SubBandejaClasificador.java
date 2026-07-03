package ar.gob.malvinas.faltas.prototipo.bandeja;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.ACCION_DERIVAR_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.ACCION_REINTENTAR_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.ACCION_REVISION_POST_REINGRESO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.ACCION_VERIFICAR_PAGO_INFORMADO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ;

/**
 * Clasificador puro de sub-bandejas operativas. No modifica estado ni transiciones.
 */
public final class SubBandejaClasificador {

    private static final Set<PrototipoStore.SituacionPagoMock> PAGO_VOLUNTARIO_EN_CURSO =
            EnumSet.of(
                    PrototipoStore.SituacionPagoMock.SOLICITADO,
                    PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION,
                    PrototipoStore.SituacionPagoMock.OBSERVADO);

    public SubBandejaAsignacion clasificar(SubBandejaContexto ctx) {
        if (ctx == null || ctx.acta() == null || ctx.acta().bandejaActual() == null) {
            return SubBandejaAsignacion.de(SubBandejaCodigo.ANALISIS_REVISION_GENERAL);
        }
        String bandeja = ctx.acta().bandejaActual();
        List<SubBandejaCodigo> candidatos = new ArrayList<>();
        switch (bandeja) {
            case "ACTAS_EN_ENRIQUECIMIENTO" -> evaluarEnriquecimiento(ctx, candidatos);
            case "PENDIENTE_PREPARACION_DOCUMENTAL" -> evaluarPreparacionDocumental(ctx, candidatos);
            case "PENDIENTE_FIRMA" -> evaluarPendienteFirma(ctx, candidatos);
            case "PENDIENTE_NOTIFICACION" -> evaluarPendienteNotificacion(ctx, candidatos);
            case "EN_NOTIFICACION" -> evaluarEnNotificacion(ctx, candidatos);
            case "PENDIENTE_ANALISIS" -> evaluarPendienteAnalisis(ctx, candidatos);
            case "PENDIENTES_RESOLUCION_REDACCION" -> evaluarRedaccion(ctx, candidatos);
            case "PENDIENTES_FALLO" -> evaluarPendientesFallo(ctx, candidatos);
            case "CON_APELACION" -> evaluarConApelacion(ctx, candidatos);
            case "PARALIZADAS" -> evaluarParalizadas(ctx, candidatos);
            case "GESTION_EXTERNA" -> evaluarGestionExterna(ctx, candidatos);
            case "ARCHIVO" -> evaluarArchivo(ctx, candidatos);
            case "CERRADAS" -> evaluarCerradas(ctx, candidatos);
            default -> candidatos.add(SubBandejaCodigo.ANALISIS_REVISION_GENERAL);
        }
        SubBandejaCodigo elegido = candidatos.stream()
                .min(Comparator.comparingInt(SubBandejaCodigo::prioridad))
                .orElse(fallbackDeBandeja(bandeja));
        List<String> secundarios = chipsSecundarios(ctx, elegido);
        return new SubBandejaAsignacion(
                elegido.codigo(),
                elegido.label(),
                ActaBandejaUxPresenter.chipVisible(elegido, ctx),
                ActaBandejaUxPresenter.accionPrincipalVisible(elegido, ctx),
                elegido.prioridad(),
                secundarios);
    }

    private static SubBandejaCodigo fallbackDeBandeja(String bandeja) {
        return SubBandejaCodigo.deBandeja(bandeja).stream()
                .filter(s -> s != SubBandejaCodigo.NOTIF_EN_OTRO_CANAL)
                .min(Comparator.comparingInt(SubBandejaCodigo::prioridad))
                .orElse(SubBandejaCodigo.ANALISIS_REVISION_GENERAL);
    }

    private static void evaluarEnriquecimiento(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if ("CAPT".equals(ctx.acta().bloqueActual())) {
            out.add(SubBandejaCodigo.CAPTURA_INICIAL);
        }
        if (PAGO_VOLUNTARIO_EN_CURSO.contains(ctx.situacionPago())
                || ACCION_EVALUAR_PAGO_VOLUNTARIO.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.PAGO_VOLUNTARIO_ORIGINADO);
        }
        out.add(SubBandejaCodigo.ENRIQUECIMIENTO_GENERAL);
    }

    private static void evaluarPreparacionDocumental(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if ("PENDIENTE_GENERACION".equals(ctx.acta().estadoProcesoActual())) {
            out.add(SubBandejaCodigo.GENERACION_ACTA_PENDIENTE);
        }
        if (tienePiezasPendientes(ctx)) {
            out.add(SubBandejaCodigo.GENERACION_PIEZAS_PENDIENTE);
        }
        out.add(SubBandejaCodigo.REVISION_DOCUMENTAL);
    }

    private static void evaluarPendienteFirma(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if (tieneDocPendienteFirma(ctx, "FALLO_CONDENATORIO") || tieneDocPendienteFirma(ctx, "FALLO")) {
            out.add(SubBandejaCodigo.FIRMA_FALLO_CONDENATORIO);
        }
        if (tieneDocPendienteFirma(ctx, "FALLO_ABSOLUTORIO")) {
            out.add(SubBandejaCodigo.FIRMA_FALLO_ABSOLUTORIO);
        }
        if (tieneDocPendienteFirma(ctx, "BORRADOR_ACTA") || tieneDocPendienteFirma(ctx, "ACTA_FIRMADA")) {
            out.add(SubBandejaCodigo.FIRMA_ACTA_INICIAL);
        }
        if (ctx.documentos().stream().anyMatch(SubBandejaClasificador::esDocumentoPendienteFirma)) {
            out.add(SubBandejaCodigo.FIRMA_OTRAS_PIEZAS);
        }
        out.add(SubBandejaCodigo.FIRMA_ACTA_INICIAL);
    }

    private static void evaluarPendienteNotificacion(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        Optional<ActaNotificacionMock> pendiente = notificacionListaParaEnvio(ctx);
        if (pendiente.isPresent()) {
            clasificarNotificacionLista(pendiente.get(), out);
        } else {
            out.add(SubBandejaCodigo.NOTIF_ACTA_LISTA_ENVIO);
        }
        out.add(SubBandejaCodigo.NOTIF_LISTA_OTRO);
    }

    private static void evaluarEnNotificacion(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        Optional<ActaNotificacionMock> relevante = notificacionRelevanteEnNotificacion(ctx);
        if (relevante.isEmpty()) {
            relevante = notificacionMasReciente(ctx);
        }
        if (relevante.isEmpty()) {
            return;
        }
        ActaNotificacionMock n = relevante.get();
        if (n.resultado() == ResultadoNotificacion.NEGATIVA) {
            out.add(SubBandejaCodigo.NOTIF_NEGATIVA_PENDIENTE_DECISION);
            return;
        }
        if (n.resultado() == ResultadoNotificacion.VENCIDA) {
            out.add(SubBandejaCodigo.NOTIF_VENCIDA_PENDIENTE_DECISION);
            return;
        }
        clasificarCanalEnNotificacion(n, out);
    }

    private static void clasificarCanalEnNotificacion(ActaNotificacionMock n, List<SubBandejaCodigo> out) {
        CanalNotificacion canal = n.canalTipificado();
        if (canal == CanalNotificacion.CORREO_POSTAL || "POSTAL".equals(n.canal())) {
            out.add(SubBandejaCodigo.NOTIF_EN_CORREO_POSTAL);
        } else if (canal == CanalNotificacion.NOTIFICADOR_MUNICIPAL) {
            out.add(SubBandejaCodigo.NOTIF_EN_NOTIFICADOR_MUNICIPAL);
        } else if (canal == CanalNotificacion.DOMICILIO_ELECTRONICO) {
            out.add(SubBandejaCodigo.NOTIF_EN_DOMICILIO_ELECTRONICO);
        } else {
            out.add(SubBandejaCodigo.NOTIF_EN_OTRO_CANAL);
        }
    }

    private static void evaluarPendienteAnalisis(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if (ctx.pendientesBloqueantes() != null && !ctx.pendientesBloqueantes().isEmpty()) {
            out.add(SubBandejaCodigo.ANALISIS_BLOQUEO_OPERATIVO);
        }
        if (ctx.cerrable()) {
            out.add(SubBandejaCodigo.CONDENA_LISTO_CIERRE);
        }
        PrototipoStore.SituacionPagoCondena spc = ctx.situacionPagoCondena();
        if (spc == PrototipoStore.SituacionPagoCondena.CONFIRMADO) {
            out.add(SubBandejaCodigo.CONDENA_PAGO_CONFIRMADO);
        } else if (spc == PrototipoStore.SituacionPagoCondena.OBSERVADO) {
            out.add(SubBandejaCodigo.CONDENA_PAGO_OBSERVADO);
        } else if (spc == PrototipoStore.SituacionPagoCondena.INFORMADO) {
            out.add(SubBandejaCodigo.CONDENA_PAGO_INFORMADO);
        } else if (esCondenaInformable(ctx)) {
            out.add(SubBandejaCodigo.CONDENA_PAGO_PENDIENTE_INFORMAR);
        }
        if (ACCION_DERIVAR_GESTION_EXTERNA.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.ANALISIS_LISTO_DERIVAR_EXTERNA);
        }
        if (ACCION_EVALUAR_NOTIFICACION_VENCIDA.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.ANALISIS_NOTIF_VENCIDA);
        }
        if (ACCION_REINTENTAR_NOTIFICACION.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.ANALISIS_NOTIF_NEGATIVA);
        }
        if (ACCION_REVISION_POST_GESTION_EXTERNA.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.ANALISIS_POST_GESTION_EXTERNA);
        }
        if (ACCION_REVISION_POST_REINGRESO.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.ANALISIS_POST_REINGRESO);
        }
        if (ACCION_VERIFICAR_PAGO_INFORMADO.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.ANALISIS_PAGO_INFORMADO);
        }
        if (ACCION_EVALUAR_PAGO_VOLUNTARIO.equals(ctx.accionPendiente())
                || ctx.situacionPago() == PrototipoStore.SituacionPagoMock.SOLICITADO) {
            out.add(SubBandejaCodigo.ANALISIS_PAGO_SOLICITADO);
        }
        if (!tieneFalloEnExpediente(ctx) && requiereFallo(ctx)) {
            out.add(SubBandejaCodigo.ANALISIS_PENDIENTE_FALLO);
        }
        if (tieneNotificacionPositivaReciente(ctx)) {
            out.add(SubBandejaCodigo.ANALISIS_NOTIF_POSITIVA);
        }
        out.add(SubBandejaCodigo.ANALISIS_REVISION_GENERAL);
    }

    private static void evaluarPendientesFallo(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if (ACCION_VERIFICAR_PAGO_INFORMADO.equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.FALLO_TRAS_PAGO_INFORMADO);
        }
        if (ctx.documentos().stream().anyMatch(d -> "INFORME_ALCOHOTEST".equals(d.tipoDocumento()))) {
            out.add(SubBandejaCodigo.FALLO_LISTO_ABSOLUTORIO);
        }
        out.add(SubBandejaCodigo.FALLO_LISTO_CONDENATORIO);
    }

    private static void evaluarConApelacion(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if ("REVISION_APELACION".equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.APELACION_EN_ANALISIS);
        }
        if (ctx.resultadoFinal() == PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                || ctx.resultadoFinal() == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            out.add(SubBandejaCodigo.APELACION_RESUELTA);
        }
        out.add(SubBandejaCodigo.APELACION_PENDIENTE_RESOLUCION);
    }

    private static void evaluarParalizadas(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if ("PARALIZACION_ESPERA_DOCUMENTAL".equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.PARALIZ_ESPERA_DOCUMENTAL);
        } else if ("PARALIZACION_TRAMITE_EXTERNO".equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.PARALIZ_TRAMITE_EXTERNO);
        } else if ("PARALIZACION_CAUSA_ADMINISTRATIVA".equals(ctx.accionPendiente())) {
            out.add(SubBandejaCodigo.PARALIZ_CAUSA_ADMINISTRATIVA);
        }
    }

    private static void evaluarRedaccion(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        String estado = ctx.acta().estadoProcesoActual();
        if ("PENDIENTE_NULIDAD".equals(estado) || piezaPendiente(ctx, "NULIDAD")) {
            out.add(SubBandejaCodigo.REDACCION_NULIDAD);
        }
        if (piezaPendiente(ctx, "MEDIDA_PREVENTIVA")) {
            out.add(SubBandejaCodigo.REDACCION_MEDIDA);
        }
        if (piezaPendiente(ctx, "RECTIFICACION")) {
            out.add(SubBandejaCodigo.REDACCION_RECTIFICACION);
        }
        if (piezaPendiente(ctx, "RESOLUCION")) {
            out.add(SubBandejaCodigo.REDACCION_RESOLUCION);
        }
        out.add(SubBandejaCodigo.REDACCION_GENERAL);
    }

    private static void evaluarGestionExterna(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if (TIPO_GESTION_EXTERNA_APREMIO.equals(ctx.tipoGestionExterna())) {
            out.add(SubBandejaCodigo.EXT_APREMIO);
        }
        if (TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ.equals(ctx.tipoGestionExterna())) {
            out.add(SubBandejaCodigo.EXT_JUZGADO_PAZ);
        }
        if (ctx.acta().permiteReingreso()) {
            out.add(SubBandejaCodigo.EXT_PENDIENTE_REINGRESO);
        }
        out.add(SubBandejaCodigo.EXT_SEGUIMIENTO);
    }

    private static void evaluarArchivo(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        if (MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO.equals(ctx.motivoArchivo())) {
            out.add(SubBandejaCodigo.ARCHIVO_POST_VENCIMIENTO);
        }
        if (MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO.equals(ctx.motivoArchivo())) {
            out.add(SubBandejaCodigo.ARCHIVO_DESDE_ANALISIS);
        }
        if ("ARCHIVADA_JURIDICA".equals(ctx.acta().estadoProcesoActual())) {
            out.add(SubBandejaCodigo.ARCHIVO_JURIDICO);
        }
        if (ctx.acta().permiteReingreso()) {
            out.add(SubBandejaCodigo.ARCHIVO_REINGRESO_PERMITIDO);
        } else {
            out.add(SubBandejaCodigo.ARCHIVO_DEFINITIVO);
        }
        out.add(SubBandejaCodigo.ARCHIVO_OPERATIVO);
    }

    private static void evaluarCerradas(SubBandejaContexto ctx, List<SubBandejaCodigo> out) {
        PrototipoStore.ResultadoFinalCierreMock rf = ctx.resultadoFinal();
        if (rf == PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO) {
            out.add(SubBandejaCodigo.CERRADA_PAGO_VOLUNTARIO);
        }
        if (rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME
                || spcCerradaPorCondena(ctx)) {
            out.add(SubBandejaCodigo.CERRADA_PAGO_CONDENA);
        }
        if (rf == PrototipoStore.ResultadoFinalCierreMock.ABSUELTO) {
            out.add(SubBandejaCodigo.CERRADA_ABSOLUCION);
        }
        if (tieneDocumento(ctx, "NULIDAD") || "PENDIENTE_NULIDAD".equals(ctx.acta().estadoProcesoActual())) {
            out.add(SubBandejaCodigo.CERRADA_NULIDAD);
        }
        if (!ctx.acta().permiteReingreso()) {
            out.add(SubBandejaCodigo.CERRADA_ARCHIVO_DEFINITIVO);
        }
        out.add(SubBandejaCodigo.CERRADA_OTRA_CAUSA);
    }

    private static void clasificarNotificacionLista(ActaNotificacionMock n, List<SubBandejaCodigo> out) {
        TipoNotificacion tipo = n.tipo() != null ? n.tipo() : TipoNotificacion.ACTA_INFRACCION;
        if (tipo == TipoNotificacion.FALLO_CONDENATORIO) {
            out.add(SubBandejaCodigo.NOTIF_FALLO_CONDENATORIO_LISTA);
        } else if (tipo == TipoNotificacion.FALLO_ABSOLUTORIO) {
            out.add(SubBandejaCodigo.NOTIF_FALLO_ABSOLUTORIO_LISTA);
        } else if (tipo == TipoNotificacion.ACTA_INFRACCION) {
            out.add(SubBandejaCodigo.NOTIF_ACTA_LISTA_ENVIO);
        } else {
            out.add(SubBandejaCodigo.NOTIF_LISTA_OTRO);
        }
    }

    private static List<String> chipsSecundarios(SubBandejaContexto ctx, SubBandejaCodigo primaria) {
        List<String> secundarios = new ArrayList<>();
        if (primaria != SubBandejaCodigo.PAGO_VOLUNTARIO_ORIGINADO
                && PAGO_VOLUNTARIO_EN_CURSO.contains(ctx.situacionPago())) {
            secundarios.add("Pago voluntario");
        }
        if (primaria != SubBandejaCodigo.ANALISIS_BLOQUEO_OPERATIVO
                && ctx.pendientesBloqueantes() != null
                && !ctx.pendientesBloqueantes().isEmpty()
                && !ActaBandejaUxPresenter.chipPrimarioCubreBloqueoMaterial(primaria, ctx)) {
            secundarios.add("Bloqueo material");
        }
        return secundarios;
    }

    private static boolean esCondenaInformable(SubBandejaContexto ctx) {
        PrototipoStore.ResultadoFinalCierreMock rf = ctx.resultadoFinal();
        return rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME;
    }

    private static boolean spcCerradaPorCondena(SubBandejaContexto ctx) {
        return ctx.situacionPagoCondena() == PrototipoStore.SituacionPagoCondena.CONFIRMADO;
    }

    private static boolean requiereFallo(SubBandejaContexto ctx) {
        return ctx.acta().tieneNotificaciones()
                && ctx.resultadoFinal() != PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                && ctx.resultadoFinal() != PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO;
    }

    private static boolean tieneFalloEnExpediente(SubBandejaContexto ctx) {
        return ctx.documentos().stream()
                .anyMatch(d -> esTipoFallo(d.tipoDocumento()) || "FALLO".equals(d.tipoDocumento()));
    }

    private static boolean esTipoFallo(String tipoDocumento) {
        return "FALLO_CONDENATORIO".equals(tipoDocumento) || "FALLO_ABSOLUTORIO".equals(tipoDocumento);
    }

    private static boolean tieneNotificacionPositivaReciente(SubBandejaContexto ctx) {
        return ctx.notificaciones().stream()
                .anyMatch(n -> n.resultado() == ResultadoNotificacion.POSITIVA
                        || "ENTREGADA".equals(n.estadoNotificacion()));
    }

    private static boolean tienePiezasPendientes(SubBandejaContexto ctx) {
        if (ctx.piezasRequeridas() == null || ctx.piezasRequeridas().isEmpty()) {
            return false;
        }
        List<String> generadas = ctx.piezasGeneradas() != null ? ctx.piezasGeneradas() : List.of();
        return ctx.piezasRequeridas().stream().anyMatch(p -> !generadas.contains(p));
    }

    private static boolean piezaPendiente(SubBandejaContexto ctx, String pieza) {
        return ctx.piezasRequeridas() != null
                && ctx.piezasRequeridas().contains(pieza)
                && (ctx.piezasGeneradas() == null || !ctx.piezasGeneradas().contains(pieza));
    }

    private static boolean tieneDocPendienteFirma(SubBandejaContexto ctx, String tipoDocumento) {
        return ctx.documentos().stream()
                .anyMatch(d -> tipoDocumento.equals(d.tipoDocumento()) && esDocumentoPendienteFirma(d));
    }

    private static boolean tieneDocumento(SubBandejaContexto ctx, String tipoDocumento) {
        return ctx.documentos().stream().anyMatch(d -> tipoDocumento.equals(d.tipoDocumento()));
    }

    private static boolean esDocumentoPendienteFirma(ActaDocumentoMock d) {
        return "PENDIENTE_FIRMA".equals(d.estadoDocumento());
    }

    private static Optional<ActaNotificacionMock> notificacionListaParaEnvio(SubBandejaContexto ctx) {
        return ctx.notificaciones().stream()
                .filter(SubBandejaClasificador::esNotificacionListaParaEnvio)
                .findFirst();
    }

    private static Optional<ActaNotificacionMock> notificacionEnCurso(SubBandejaContexto ctx) {
        return ctx.notificaciones().stream()
                .filter(SubBandejaClasificador::esNotificacionEnCurso)
                .max(Comparator.comparing(
                        ActaNotificacionMock::fechaEnvio,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * Notificación operativa dentro de {@code EN_NOTIFICACION}: en trámite o con
     * resultado negativo/vencido pendiente de decisión (p. ej. correo postal
     * devuelto).
     */
    private static Optional<ActaNotificacionMock> notificacionRelevanteEnNotificacion(SubBandejaContexto ctx) {
        Optional<ActaNotificacionMock> enCurso = notificacionEnCurso(ctx);
        if (enCurso.isPresent()) {
            return enCurso;
        }
        return ctx.notificaciones().stream()
                .filter(SubBandejaClasificador::esNotificacionPendienteDecision)
                .max(Comparator.comparing(
                        ActaNotificacionMock::fechaEnvio,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static Optional<ActaNotificacionMock> notificacionMasReciente(SubBandejaContexto ctx) {
        return ctx.notificaciones().stream()
                .max(Comparator.comparing(
                        ActaNotificacionMock::fechaEnvio,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static boolean esNotificacionPendienteDecision(ActaNotificacionMock n) {
        ResultadoNotificacion resultado = n.resultado();
        return resultado == ResultadoNotificacion.NEGATIVA || resultado == ResultadoNotificacion.VENCIDA;
    }

    private static boolean esNotificacionListaParaEnvio(ActaNotificacionMock n) {
        if (n.estado() == EstadoNotificacion.LISTA_PARA_ENVIO) {
            return true;
        }
        return "PENDIENTE_ENVIO".equals(n.estadoNotificacion());
    }

    private static boolean esNotificacionEnCurso(ActaNotificacionMock n) {
        if (n.estado() == EstadoNotificacion.ENVIADA) {
            return n.resultado() == ResultadoNotificacion.SIN_RESULTADO;
        }
        return "EN_TRAMITE".equals(n.estadoNotificacion())
                || "EN_ENVIO".equals(n.estadoNotificacion());
    }
}
