package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaBromatologiaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaTransitoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;
import ar.gob.malvinas.faltas.prototipo.web.dto.CrearActaMockDemoRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D1_CAPTURA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_ADJUNTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_EMITIDO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_ANCLA_MEDIDA_PREVENTIVA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_ACUSE_RETENCION_VEHICULO;

@Component
public class PrototipoStore {

    /**
     * Orden fijo para demo: flujo operativo típico, luego archivo y cierre.
     */
    private static final List<String> ORDEN_BANDEJAS_DEMO = List.of(
            "ACTAS_EN_ENRIQUECIMIENTO",
            "PENDIENTE_PREPARACION_DOCUMENTAL",
            "PENDIENTE_FIRMA",
            "PENDIENTE_NOTIFICACION",
            "EN_NOTIFICACION",
            "PENDIENTE_ANALISIS",
            "PENDIENTES_RESOLUCION_REDACCION",
            "GESTION_EXTERNA",
            "ARCHIVO",
            "CERRADAS");

    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvi�� a
     * anǭlisis por notificaci��n fallida y requiere decisi��n posterior. El
     * campo es filtrable por API y sobrevive en el store aparte del record
     * inmutable {@link ActaMock}.
     */
    public static final String ACCION_REINTENTAR_NOTIFICACION = "REINTENTAR_NOTIFICACION";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis porque la notificación quedó vencida sin entrega ni rechazo
     * explícito. Debe quedar distinguible del reintento por no entrega: acá
     * no se asume todavía próximo paso (nuevo intento, archivo, apremio u
     * otra salida), sólo que requiere decisión posterior.
     */
    public static final String ACCION_EVALUAR_NOTIFICACION_VENCIDA = "EVALUAR_NOTIFICACION_VENCIDA";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis por reingreso desde la macro-bandeja {@code ARCHIVO}. Permite
     * distinguir y filtrar los casos reingresados dentro de la bandeja de
     * análisis, de los que llegaron por el circuito operativo normal o por
     * notificación fallida/vencida. Mantiene coherencia con la política del
     * prototipo de exponer el motivo de retorno como accionPendiente.
     */
    public static final String ACCION_REVISION_POST_REINGRESO = "REVISION_POST_REINGRESO";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso ya
     * atravesó fallo + notificación de fallo y la ventana de espera
     * posterior (regla especial: 5 días, ver
     * {@code spec/02-reglas-transversales/02-reglas-de-notificacion.md}) se
     * cumplió sin novedad que altere el recorrido, por lo que quedó en
     * condición de derivación a gestión externa.
     *
     * <p>En este slice el caso todavía permanece en análisis: sólo se
     * expone la condición de "listo para derivación" como marca filtrable,
     * sin materializar la bandeja {@code GESTION_EXTERNA} ni la derivación
     * efectiva. Distinguible del resto de marcas de análisis porque no
     * proviene de notificación fallida/vencida ni de reingreso desde archivo.
     */
    public static final String ACCION_DERIVAR_GESTION_EXTERNA = "DERIVAR_GESTION_EXTERNA";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis por retorno explícito desde la macro-bandeja
     * {@code GESTION_EXTERNA}. Permite distinguir y filtrar dentro de
     * análisis los casos que acaban de reingresar desde gestión externa, de
     * los que vinieron por el circuito operativo normal, por notificación
     * fallida/vencida o por reingreso desde archivo. La trazabilidad del
     * tipo de gestión externa del que provino el caso se preserva aparte
     * vía {@link GestionExternaSupport}, no se sobrescribe con esta marca.
     */
    public static final String ACCION_REVISION_POST_GESTION_EXTERNA = "REVISION_POST_GESTION_EXTERNA";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso ingresó a
     * análisis porque en una etapa temprana del expediente se registró una
     * solicitud de pago voluntario, y esa solicitud debe ser evaluada
     * materialmente antes de definir la siguiente situación operativa
     * (cierre por pago cumplido, continuación del circuito sancionatorio u
     * otra salida). La spec admite originar la solicitud desde labradas o
     * enriquecimiento (ver spec/03-bandejas/01-bandeja-labradas.md y
     * spec/03-bandejas/02-bandeja-enriquecimiento.md), pero centraliza la
     * evaluación en la bandeja de análisis / presentaciones / pagos
     * (spec/03-bandejas/03-bandeja-analisis-presentaciones-pagos.md). Esta
     * marca distingue el caso del resto de motivos por los que un
     * expediente puede aparecer en análisis.
     */
    public static final String ACCION_EVALUAR_PAGO_VOLUNTARIO = "EVALUAR_PAGO_VOLUNTARIO";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: se informó un pago
     * voluntario y se adjuntó comprobante mock; el caso requiere verificación
     * interna (mock) para confirmarlo u observarlo. Se usa sólo como "tarea
     * actual" (operativa) y NO colapsa la situación de pago, que vive aparte
     * en {@link #getSituacionPago(String)}.
     */
    public static final String ACCION_VERIFICAR_PAGO_INFORMADO = "VERIFICAR_PAGO_INFORMADO";
    /**
     * Motivo de archivo asignado cuando una acta es archivada directamente
     * desde análisis por decisión administrativa (acción genérica de archivo
     * sin paso previo por evaluación de vencimiento). Permite distinguir
     * dentro de la macro-bandeja {@code ARCHIVO} los archivos de origen
     * directo de otros orígenes semánticamente diferentes.
     */
    public static final String MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO = "ARCHIVO_DESDE_ANALISIS_DIRECTO";
    /**
     * Motivo de archivo asignado cuando la decisión posterior a una
     * notificación vencida es archivar el caso (en lugar de reintentar la
     * notificación u otra salida futura no modelada todavía). Distingue este
     * archivo del archivo directo sin pasar por evaluación de vencimiento.
     */
    public static final String MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO = "ARCHIVO_POST_EVALUACION_VENCIMIENTO";
    /**
     * Tipo vigente de gestión externa: apremio. Se asigna al expediente
     * al materializar la derivación efectiva hacia la macro-bandeja
     * {@code GESTION_EXTERNA} vía la acción dedicada, para que la salida
     * del circuito interno no quede muda y se pueda distinguir por API del
     * tipo {@link #TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ}.
     */
    public static final String TIPO_GESTION_EXTERNA_APREMIO = "APREMIO";
    /**
     * Tipo vigente de gestión externa: Juzgado de Paz. Alternativa al tipo
     * {@link #TIPO_GESTION_EXTERNA_APREMIO} dentro de la macro-bandeja
     * {@code GESTION_EXTERNA}: ambos tipos comparten bloque/estado/situación
     * y política de reingreso, y sólo se diferencian por el tipo vigente
     * expuesto por API. El operador elige la salida usando la acción
     * dedicada correspondiente.
     */
    public static final String TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ = "JUZGADO_DE_PAZ";

    public enum RegistrarNotificacionPositivaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarNotificacionPositivaResultado(
            RegistrarNotificacionPositivaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum RegistrarNotificacionNegativaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarNotificacionNegativaResultado(
            RegistrarNotificacionNegativaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente) {
    }

    public enum ReintentarNotificacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReintentarNotificacionResultado(
            ReintentarNotificacionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum RegistrarNotificacionVencidaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarNotificacionVencidaResultado(
            RegistrarNotificacionVencidaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente) {
    }

    public enum ReintentarNotificacionVencidaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReintentarNotificacionVencidaResultado(
            ReintentarNotificacionVencidaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum CerrarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record CerrarActaResultado(
            CerrarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum CrearActaMockDemoEstado {
        OK,
        BAD_REQUEST
    }

    public record CrearActaMockDemoResultado(
            CrearActaMockDemoEstado estado, String mensaje, ActaMock acta) {
    }

    public enum DependenciaActaDemo {
        TRANSITO,
        INSPECCIONES,
        FISCALIZACION,
        BROMATOLOGIA
    }

    public enum TipoActaDemo {
        TRANSITO,
        CONTRAVENCION,
        BROMATOLOGIA
    }

    public enum ArchivarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ArchivarActaResultado(
            ArchivarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String motivoArchivo) {
    }

    public enum ArchivarPorVencimientoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ArchivarPorVencimientoResultado(
            ArchivarPorVencimientoEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String motivoArchivo) {
    }

    public enum ReingresarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReingresarActaResultado(
            ReingresarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente,
            String motivoArchivoPrevio) {
    }

    public enum DerivarAGestionExternaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record DerivarAGestionExternaResultado(
            DerivarAGestionExternaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String tipoGestionExterna) {
    }

    public enum ReingresarDesdeGestionExternaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReingresarDesdeGestionExternaResultado(
            ReingresarDesdeGestionExternaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente,
            String tipoGestionExternaPrevia) {
    }

    public enum GenerarMedidaPreventivaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarMedidaPreventivaResultado(
            GenerarMedidaPreventivaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum GenerarNotificacionActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarNotificacionActaResultado(
            GenerarNotificacionActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum GenerarNulidadEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarNulidadResultado(
            GenerarNulidadEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    /**
     * Condición de material a registrar desde D1/D2: mismos
     * {@code tipoDocumento} que anclan orígenes bloqueantes y hechos
     * materiales (un solo modelo de expediente).
     */
    public enum TipoConstatacionMaterialTemprana {
        SECUESTRO_RODADO,
        RETENCION_DOCUMENTAL,
        MEDIDA_PREVENTIVA_APLICABLE
    }

    public enum RegistrarConstatacionMaterialTempranaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    /**
     * Detalle de CONFLICT: tipo duplicado, expediente no en D1/D2 enriquecimiento,
     * o falta al menos un evento previo en trazabilidad.
     */
    public enum MotivoConflictoConstatacionMaterialTemprana {
        TIPO_YA_EN_EXPEDIENTE,
        FUERA_ETAPA_LABRADO_ENRIQUECIMIENTO,
        SIN_TRAZA_PREVIA
    }

    public record RegistrarConstatacionMaterialTempranaResultado(
            RegistrarConstatacionMaterialTempranaEstado estado,
            String actaId,
            String documentoId,
            String tipoDocumento,
            String bandejaActual,
            String estadoProcesoActual,
            MotivoConflictoConstatacionMaterialTemprana motivoConflicto) {
    }

    /**
     * Medida preventiva nacida durante trámite (p. ej. inspección posterior a
     * labrado en contravención), no como constatación temprana D1/D2; incorpora
     * el mismo ancla documental que el circuito de
     * {@link #reconocerOrigenBloqueanteMedidaPreventiva} / cierre.
     */
    public enum RegistrarMedidaPreventivaPosteriorEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public enum MotivoConflictoRegistroMedidaPreventivaPosterior {
        ACTA_CERRADA,
        ACTA_EN_ARCHIVO,
        FUERA_PENDIENTE_ANALISIS,
        MEDIDA_YA_EN_EXPEDIENTE
    }

    public record RegistrarMedidaPreventivaPosteriorResultado(
            RegistrarMedidaPreventivaPosteriorEstado estado,
            String actaId,
            String documentoId,
            String tipoDocumento,
            String bandejaActual,
            String estadoProcesoActual,
            MotivoConflictoRegistroMedidaPreventivaPosterior motivoConflicto) {
    }

    public enum RegistrarSolicitudPagoVoluntarioEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarSolicitudPagoVoluntarioResultado(
            RegistrarSolicitudPagoVoluntarioEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente) {
    }

    /**
     * Situación de pago mock, visible por API y separada de {@code accionPendiente}.
     * Modela en mínimo: solicitud, pago informado, pendiente de confirmación,
     * confirmado y observado.
     */
    public enum SituacionPagoMock {
        SIN_PAGO,
        SOLICITADO,
        PAGO_INFORMADO,
        PENDIENTE_CONFIRMACION,
        CONFIRMADO,
        OBSERVADO
    }

    /**
     * Resultado final relevante para evaluar si el expediente puede quedar en
     * condición de cierre (independiente de la bandeja y de los pendientes).
     */
    public enum ResultadoFinalCierreMock {
        SIN_RESULTADO_FINAL,
        ABSUELTO,
        PAGO_CONFIRMADO
    }

    /**
     * Pendiente material/documental que bloquea cierre mientras esté activo.
     */
    public enum PendienteBloqueanteCierreMock {
        LEVANTAMIENTO_MEDIDA_PREVENTIVA,
        LIBERACION_RODADO,
        ENTREGA_DOCUMENTACION
    }

    /**
     * Hecho operativo del expediente (precarga demo o circuito previo, p. ej.
     * medida preventiva generada) que exige, antes del cierre, el documento
     * resolutorio mock y además el registro de cumplimiento material efectivo
     * (ver spec 06 medidas, §§3–4). Si la ancla ya figura en el expediente, el
     * origen se refleja en cerrabilidad sin depender del POST de reconocimiento.
     */
    public enum OrigenBloqueanteCierreMaterialMock {
        /**
         * Requiere documento de levantamiento; el origen está implicado por la
         * ancla {@code MEDIDA_PREVENTIVA} en el expediente (p. ej. la pieza
         * agregada por {@link #generarMedidaPreventiva} o precarga demo
         * coherente); {@link #reconocerOrigenBloqueanteMedidaPreventiva} es
         * opcional e idempotente, mismo patrón que rodado y documentación.
         */
        MEDIDA_PREVENTIVA_ACTIVA,
        /**
         * Requiere acta / documento de liberación de rodado; origen implicado por
         * ancla de secuestro/retención de vehículo ({@code ACTA_RETENCION});
         * {@link #reconocerOrigenBloqueanteSecuestroRodado} es opcional.
         */
        RODADO_SECUESTRADO,
        /**
         * Requiere constancia de entrega o restitución de documentación; origen
         * implicado por constatación de retención documental
         * ({@code CONSTATACION_RETENCION_DOCUMENTACION});
         * {@link #reconocerOrigenBloqueanteRetencionDocumental} es opcional.
         */
        DOCUMENTACION_RETENIDA
    }

    public record CerrabilidadActaVista(
            ResultadoFinalCierreMock resultadoFinal,
            boolean cerrable,
            java.util.List<PendienteBloqueanteCierreMock> pendientesBloqueantes,
            String motivoNoCerrable) {
    }

    /**
     * Plano de hechos materiales explícito, separado del expediente documental:
     * la fase {@link FaseEjeHechoMaterial#RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL}
     * indica que existe documento resolutorio en el expediente pero aún no el
     * registro de cumplimiento material efectivo.
     */
    public enum FaseEjeHechoMaterial {
        /** No hay origen material reconocido para este eje en el acta. */
        NO_APLICA,
        /** Origen activo: falta incorporar el documento resolutorio mock. */
        SITUACION_PENDIENTE_DE_RESOLUTORIO,
        /**
         * El resolutorio figura en expediente; el hecho material efectivo (p. ej.
         * medida levantada, rodado liberado) aún no está verificado en la capa material.
         */
        RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL,
        /** Hecho material efectivo registrado (mock); no sustituye la trazabilidad documental. */
        CUMPLIMIENTO_MATERIAL_VERIFICADO
    }

    /**
     * @param clave identificador estable para API (p. ej. {@code MEDIDA_PREVENTIVA})
     * @param bloqueaCierre {@code true} solo en bandeja de análisis, cuando el eje
     *     seguiría afectando el cierre; en etapas anteriores la fase y la
     *     descripción reflejan ya la condición del caso sin anticipar el cómputo
     *     de cierre.
     * @param descripcion texto de apoyo a la fase
     * @param ejeBloqueanteCierre pendiente de cierre vinculado a este eje
     *     (p. ej. {@code LIBERACION_RODADO}) cuando el origen material activo exige
     *     cierre de circuito; {@code null} si no aplica aún o el hecho quedó
     *     verificado materialmente.
     */
    public record EjeHechoMaterialVista(
            String clave,
            String etiqueta,
            FaseEjeHechoMaterial fase,
            boolean bloqueaCierre,
            String descripcion,
            String ejeBloqueanteCierre) {
    }

    /**
     * @param lecturaOperativa señal operativa para demo/UI: pendiente de tratamiento
     *     documental o material, o {@code null} cuando no aplica.
     */
    public record HechosMaterialesActaVista(
            java.util.List<EjeHechoMaterialVista> ejes, String lecturaOperativa) {
    }

    public enum MarcarResultadoAbsueltoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record MarcarResultadoAbsueltoResultado(
            MarcarResultadoAbsueltoEstado estado,
            String actaId,
            String bandejaActual) {
    }

    public enum ResolverPendienteBloqueanteEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ResolverPendienteBloqueanteResultado(
            ResolverPendienteBloqueanteEstado estado,
            String actaId,
            String pendienteResuelto,
            ResultadoFinalCierreMock resultadoFinal,
            boolean cerrable,
            java.util.List<String> pendientesBloqueantesCierreRestantes,
            String motivoNoCerrable) {
    }

    public enum RegistrarResolucionBloqueoCierreEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    /**
     * Incorpora el documento resolutorio mock; no reemplaza el registro de
     * {@linkplain #registrarCumplimientoMaterialBloqueoCierre cumplimiento
     * material efectivo}. {@code pendienteAsociado} es el
     * {@link PendienteBloqueanteCierreMock} de referencia, no implica cierre
     * del bloqueo en sentido material.
     */
    public record RegistrarResolucionBloqueoCierreResultado(
            RegistrarResolucionBloqueoCierreEstado estado,
            String actaId,
            String documentoId,
            String tipoDocumento,
            String pendienteAsociado,
            ResultadoFinalCierreMock resultadoFinal,
            boolean cerrable,
            java.util.List<String> pendientesBloqueantesCierreRestantes,
            String motivoNoCerrable) {
    }

    public enum RegistrarCumplimientoMaterialBloqueoCierreEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    /**
     * Registro mock de que el hecho material quedó cumplido (medida levantada,
     * rodado liberado, documentación entregada). Requiere documento
     * resolutorio previo y el origen material correspondiente.
     */
    public record RegistrarCumplimientoMaterialBloqueoCierreResultado(
            RegistrarCumplimientoMaterialBloqueoCierreEstado estado,
            String actaId,
            String pendienteCumplido,
            ResultadoFinalCierreMock resultadoFinal,
            boolean cerrable,
            java.util.List<String> pendientesBloqueantesCierreRestantes,
            String motivoNoCerrable) {
    }

    /**
     * Reconocimiento explícito (mock) del hecho material que origina un
     * bloqueante de cierre, anclado a documentación del expediente (ver
     * {@link #reconocerOrigenBloqueanteMedidaPreventiva},
     * {@link #reconocerOrigenBloqueanteSecuestroRodado} y
     * {@link #reconocerOrigenBloqueanteRetencionDocumental}).
     */
    public enum ReconocerOrigenBloqueanteMaterialEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    /**
     * @param origenBloqueante origen incorporado si {@code estado == OK};
     *     {@code null} si no aplica
     */
    public record ReconocerOrigenBloqueanteMaterialResultado(
            ReconocerOrigenBloqueanteMaterialEstado estado,
            String actaId,
            OrigenBloqueanteCierreMaterialMock origenBloqueante,
            CerrabilidadActaVista cerrabilidad) {
    }

    /**
     * Hecho informado por el administrado (mock): existe un pago informado y,
     * opcionalmente, un comprobante adjunto (mock). La confirmación/observación
     * se modela en {@link SituacionPagoMock}.
     */
    public record PagoInformadoMock(
            java.time.LocalDateTime fechaInformado,
            String comprobanteId,
            String comprobanteNombreArchivo) {
    }

    public enum RegistrarPagoInformadoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarPagoInformadoResultado(
            RegistrarPagoInformadoEstado estado,
            String actaId,
            SituacionPagoMock situacionPago) {
    }

    public enum AdjuntarComprobantePagoInformadoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record AdjuntarComprobantePagoInformadoResultado(
            AdjuntarComprobantePagoInformadoEstado estado,
            String actaId,
            SituacionPagoMock situacionPago,
            String accionPendiente,
            PagoInformadoMock pagoInformado) {
    }

    public enum ConfirmarPagoInformadoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ConfirmarPagoInformadoResultado(
            ConfirmarPagoInformadoEstado estado,
            String actaId,
            SituacionPagoMock situacionPago) {
    }

    public enum ObservarPagoInformadoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ObservarPagoInformadoResultado(
            ObservarPagoInformadoEstado estado,
            String actaId,
            SituacionPagoMock situacionPago) {
    }

    public enum FirmarDocumentoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record FirmarDocumentoResultado(
            FirmarDocumentoEstado estado,
            String actaId,
            String documentoId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public record BandejaConteo(String codigo, int cantidadActas) {
    }

    private final Map<String, ActaMock> actas = new LinkedHashMap<>();
    private final Map<String, List<ActaEventoMock>> eventosPorActa = new LinkedHashMap<>();
    private final Map<String, List<ActaDocumentoMock>> documentosPorActa = new LinkedHashMap<>();
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa = new LinkedHashMap<>();
    private final Map<String, ActaPiezasRequeridasMock> piezasRequeridasPorActa = new LinkedHashMap<>();
    /**
     * Marca operativa actual dentro de la bandeja. Se mantiene en un mapa
     * paralelo para no cambiar el record {@link ActaMock} ni obligar a
     * reescribir todas las transiciones previas. Si una acta no tiene marca,
     * simplemente no aparece en el mapa.
     */
    private final Map<String, String> accionPendientePorActa = new LinkedHashMap<>();

    /**
     * Situación de pago mock por acta. Si una acta no está en el mapa, se
     * interpreta como {@link SituacionPagoMock#SIN_PAGO}.
     */
    private final Map<String, SituacionPagoMock> situacionPagoPorActa = new LinkedHashMap<>();

    /**
     * Hecho informado (mock) de pago voluntario por acta. La existencia del
     * registro no implica confirmación: la confirmación vive en
     * {@link #situacionPagoPorActa}.
     */
    private final Map<String, PagoInformadoMock> pagoInformadoPorActa = new LinkedHashMap<>();
    /**
     * Mock de datos de tránsito por acta. Solo se puebla donde el escenario
     * demo asigna flags explícitos (p. ej. nacimiento de condiciones
     * materiales desde dato, no como acción de circuito).
     */
    private final Map<String, ActaTransitoMock> actaTransitoMockPorActa = new LinkedHashMap<>();

    private final Map<String, ActaBromatologiaMock> actaBromatologiaMockPorActa = new LinkedHashMap<>();
    private final Map<String, String> dependenciaDemoPorActa = new LinkedHashMap<>();
    private final Map<String, String> tipoActaDemoPorActa = new LinkedHashMap<>();
    private final AtomicInteger contadorActaLabradoMockDemo = new AtomicInteger(0);

    /**
     * Soporte funcional del área archivo/reingreso: archivo directo desde
     * análisis, archivo post evaluación de vencimiento, reingreso desde
     * archivo, motivoArchivo y eventos asociados. Extraído del store para
     * reducir su tamaño/acoplamiento sin cambiar comportamiento observable.
     * El store mantiene una fachada pública que delega aquí.
     */
    private final ArchivoReingresoSupport archivoReingreso =
            new ArchivoReingresoSupport(actas, eventosPorActa, accionPendientePorActa);

    /**
     * Soporte funcional del área notificación: notificación positiva,
     * negativa, vencida, reintento por no entrega y reintento
     * post-vencimiento, junto con sus estados internos, resumen de
     * destinatario demo y eventos asociados. Extraído del store para bajar
     * su tamaño/acoplamiento sin cambiar comportamiento observable. El
     * store mantiene una fachada pública que delega aquí.
     */
    private final NotificacionSupport notificacion =
            new NotificacionSupport(actas, eventosPorActa, notificacionesPorActa, accionPendientePorActa);

    /**
     * Resultado final para cierre, pendientes, cumplimientos materiales y
     * cómputo de cerrabilidad. Debe declararse antes de
     * {@link PiezasFirmaSupport} para enganchar el origen de medida activa
     * tras {@code generarMedidaPreventiva}.
     */
    private final CerrabilidadSupport cerrabilidad =
            new CerrabilidadSupport(actas, eventosPorActa, documentosPorActa);

    /**
     * Soporte funcional del área piezas/firma: consulta de piezas,
     * producción de medida preventiva, producción de notificación del acta,
     * producción de nulidad, firma individual de documentos y transición
     * a {@code PENDIENTE_NOTIFICACION} cuando se cierra la firma, o a
     * {@code CERRADAS} si entre los documentos firmados hay nulidad
     * (salida invalidante). Extraído del store para bajar su
     * tamaño/acoplamiento sin cambiar comportamiento observable. El store
     * mantiene una fachada pública que delega aquí. Frontera con
     * notificación: al cerrarse la firma por vía no-nulidad, delega en
     * {@link NotificacionSupport} la materialización inicial de la
     * notificación (única interacción cruzada entre ambas áreas).
     */
    private final PiezasFirmaSupport piezasFirma =
            new PiezasFirmaSupport(
                    actas,
                    eventosPorActa,
                    documentosPorActa,
                    piezasRequeridasPorActa,
                    notificacion,
                    cerrabilidad);

    /**
     * Soporte funcional del área gestión externa: derivación efectiva de
     * casos desde análisis hacia la macro-bandeja {@code GESTION_EXTERNA}
     * post fallo + notificación de fallo + ventana de espera posterior
     * cumplida sin novedad. Mantiene el tipo mínimo de gestión externa
     * ({@link #TIPO_GESTION_EXTERNA_APREMIO}) asociado a cada acta derivada.
     * Extraído del store para preservar la descompresión por área funcional
     * sin cambiar comportamiento observable. El store mantiene una fachada
     * pública que delega aquí.
     */
    private final GestionExternaSupport gestionExterna =
            new GestionExternaSupport(actas, eventosPorActa, accionPendientePorActa);

    /**
     * Soporte funcional del área presentaciones / pagos (alcance mínimo):
     * registra la solicitud de pago voluntario temprano originada en
     * {@code ACTAS_EN_ENRIQUECIMIENTO} y mueve el caso a
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link #ACCION_EVALUAR_PAGO_VOLUNTARIO}, respetando que la spec
     * centraliza la evaluación material del pago en la bandeja de análisis
     * / presentaciones / pagos
     * ({@code spec/03-bandejas/03-bandeja-analisis-presentaciones-pagos.md}).
     * Extraído del store para mantener la descompresión por área funcional.
     */
    private final PagoVoluntarioSupport pagoVoluntario =
            new PagoVoluntarioSupport(actas, eventosPorActa, accionPendientePorActa, situacionPagoPorActa);

    /**
     * Soporte funcional del circuito mínimo de pago informado + comprobante +
     * confirmación/observación mock, separado del registro de solicitud.
     */
    private final PagoInformadoSupport pagoInformado =
            new PagoInformadoSupport(actas, eventosPorActa, accionPendientePorActa, situacionPagoPorActa, pagoInformadoPorActa);

    private final CierreSupport cierre =
            new CierreSupport(actas, eventosPorActa, accionPendientePorActa, cerrabilidad);

    public Map<String, ActaMock> getActas() {
        return actas;
    }

    /**
     * Satélite de tránsito precargado en carga demo; vacío si el acta no
     * modela flags (no implica “sin tránsito” en el producto, sólo no demo).
     */
    public Optional<ActaTransitoMock> getActaTransitoMock(String actaId) {
        return Optional.ofNullable(actaTransitoMockPorActa.get(actaId));
    }

    /** Carga demo: fija flags y documentos coherentes en {@link MockDataFactory}. */
    public void putActaTransitoMock(String actaId, ActaTransitoMock datos) {
        if (actaId == null) {
            return;
        }
        if (datos == null) {
            actaTransitoMockPorActa.remove(actaId);
        } else {
            actaTransitoMockPorActa.put(actaId, datos);
        }
    }

    public Optional<ActaBromatologiaMock> getActaBromatologiaMock(String actaId) {
        return Optional.ofNullable(actaBromatologiaMockPorActa.get(actaId));
    }

    public void putActaBromatologiaMock(String actaId, ActaBromatologiaMock datos) {
        if (actaId == null) {
            return;
        }
        if (datos == null) {
            actaBromatologiaMockPorActa.remove(actaId);
        } else {
            actaBromatologiaMockPorActa.put(actaId, datos);
        }
    }

    public Optional<String> getDependenciaDemo(String actaId) {
        return Optional.ofNullable(dependenciaDemoPorActa.get(actaId));
    }

    public Optional<String> getTipoActaAltaDemo(String actaId) {
        return Optional.ofNullable(tipoActaDemoPorActa.get(actaId));
    }

    public Map<String, List<ActaEventoMock>> getEventosPorActa() {
        return eventosPorActa;
    }

    public Map<String, List<ActaDocumentoMock>> getDocumentosPorActa() {
        return documentosPorActa;
    }

    public Map<String, List<ActaNotificacionMock>> getNotificacionesPorActa() {
        return notificacionesPorActa;
    }

    public Map<String, ActaPiezasRequeridasMock> getPiezasRequeridasPorActa() {
        return piezasRequeridasPorActa;
    }

    public void clearAll() {
        actas.clear();
        eventosPorActa.clear();
        documentosPorActa.clear();
        notificacionesPorActa.clear();
        piezasRequeridasPorActa.clear();
        accionPendientePorActa.clear();
        situacionPagoPorActa.clear();
        pagoInformadoPorActa.clear();
        actaTransitoMockPorActa.clear();
        actaBromatologiaMockPorActa.clear();
        dependenciaDemoPorActa.clear();
        tipoActaDemoPorActa.clear();
        contadorActaLabradoMockDemo.set(0);
        cerrabilidad.clear();
        archivoReingreso.clear();
        gestionExterna.clear();
    }

    /**
     * Marca operativa vigente de la acta dentro de su bandeja, o {@code null}
     * si la acta no tiene acci��n pendiente marcada.
     */
    public String getAccionPendiente(String actaId) {
        return accionPendientePorActa.get(actaId);
    }

    /**
     * Motivo vigente de archivo para la acta dentro de la macro-bandeja
     * {@code ARCHIVO}, o {@code null} si la acta no está archivada o fue
     * archivada antes de modelar esta semántica. Fachada: el estado y la
     * semántica viven en {@link ArchivoReingresoSupport}.
     */
    public String getMotivoArchivo(String actaId) {
        return archivoReingreso.getMotivoArchivo(actaId);
    }

    /**
     * Tipo de gestión externa vigente para la acta dentro de la macro-bandeja
     * {@code GESTION_EXTERNA}, o {@code null} si la acta no fue derivada.
     * Fachada: el estado y la semántica viven en
     * {@link GestionExternaSupport}.
     */
    public String getTipoGestionExterna(String actaId) {
        return gestionExterna.getTipoGestionExterna(actaId);
    }

    /**
     * Situación de pago mock visible por API. Si no hay valor cargado para la
     * acta, se interpreta como {@link SituacionPagoMock#SIN_PAGO}.
     */
    public SituacionPagoMock getSituacionPago(String actaId) {
        SituacionPagoMock v = situacionPagoPorActa.get(actaId);
        return v != null ? v : SituacionPagoMock.SIN_PAGO;
    }

    /**
     * Hecho informado por el administrado (mock): pago informado y/o
     * comprobante adjunto. Puede ser {@code null} si no hay pago informado.
     */
    public PagoInformadoMock getPagoInformado(String actaId) {
        return pagoInformadoPorActa.get(actaId);
    }

    public CerrabilidadActaVista getCerrabilidadActa(String actaId) {
        ActaMock a = actas.get(actaId);
        return cerrabilidad.getVistaCerrabilidad(a);
    }

    /**
     * Hechos materiales efectivos por eje (medida, rodado, documentación), con
     * fase explícita frente al plano documental.
     */
    public HechosMaterialesActaVista getHechosMaterialesActa(String actaId) {
        return cerrabilidad.hechosMaterialesActa(actaId);
    }

    public MarcarResultadoAbsueltoResultado marcarResultadoAbsuelto(String actaId) {
        return cerrabilidad.marcarResultadoAbsuelto(actaId);
    }

    public ResolverPendienteBloqueanteResultado resolverPendienteBloqueanteCierre(
            String actaId,
            PendienteBloqueanteCierreMock tipo) {
        return cerrabilidad.resolverPendienteBloqueante(actaId, tipo);
    }

    /**
     * Incorpora el documento resolutorio mock. El pendiente de cierre material
     * (p. ej. {@link PendienteBloqueanteCierreMock#LEVANTAMIENTO_MEDIDA_PREVENTIVA})
     * no se duplica: variaciones documentales (firma/notif) se indican con
     * {@code documentoConCircuitoFirmaNotif} sobre el mismo eje, no con otro
     * bloqueante.
     */
    public RegistrarResolucionBloqueoCierreResultado registrarResolucionBloqueoCierreDocumental(
            String actaId, PendienteBloqueanteCierreMock tipoPendiente) {
        return cerrabilidad.registrarResolucionBloqueoCierreDocumental(
                actaId, tipoPendiente, false);
    }

    public RegistrarResolucionBloqueoCierreResultado registrarResolucionBloqueoCierreDocumental(
            String actaId,
            PendienteBloqueanteCierreMock tipoPendiente,
            boolean documentoConCircuitoFirmaNotif) {
        return cerrabilidad.registrarResolucionBloqueoCierreDocumental(
                actaId, tipoPendiente, documentoConCircuitoFirmaNotif);
    }

    public RegistrarCumplimientoMaterialBloqueoCierreResultado registrarCumplimientoMaterialBloqueoCierre(
            String actaId, PendienteBloqueanteCierreMock tipoPendiente) {
        return cerrabilidad.registrarCumplimientoMaterialEfectivoBloqueoCierre(actaId, tipoPendiente);
    }

    /**
     * Demo: incorpora el origen material {@link OrigenBloqueanteCierreMaterialMock#RODADO_SECUESTRADO}
     * solo si el expediente incluye el ancla documental de retención/secuestro de
     * vehículo ({@code ACTA_RETENCION}). Idempotente si el origen ya estaba.
     */
    public ReconocerOrigenBloqueanteMaterialResultado reconocerOrigenBloqueanteSecuestroRodado(String actaId) {
        return cerrabilidad.reconocerOrigenBloqueanteSecuestroRodado(actaId);
    }

    /**
     * Demo: incorpora el origen {@link OrigenBloqueanteCierreMaterialMock#MEDIDA_PREVENTIVA_ACTIVA}
     * solo si el expediente incluye un documento con tipo {@code MEDIDA_PREVENTIVA}
     * (p. ej. al producir la pieza vía {@link #generarMedidaPreventiva}). Idempotente
     * si el origen ya constaba.
     */
    public ReconocerOrigenBloqueanteMaterialResultado reconocerOrigenBloqueanteMedidaPreventiva(String actaId) {
        return cerrabilidad.reconocerOrigenBloqueanteMedidaPreventiva(actaId);
    }

    /**
     * Demo: incorpora el origen material
     * {@link OrigenBloqueanteCierreMaterialMock#DOCUMENTACION_RETENIDA} solo si el
     * expediente incluye la constatación de retención documental
     * ({@code CONSTATACION_RETENCION_DOCUMENTACION}). Idempotente si el origen ya estaba.
     */
    public ReconocerOrigenBloqueanteMaterialResultado reconocerOrigenBloqueanteRetencionDocumental(String actaId) {
        return cerrabilidad.reconocerOrigenBloqueanteRetencionDocumental(actaId);
    }

    /**
     * Incorpora al expediente el ancla de material correspondiente
     * (constatación en labrado o enriquecimiento). Alimenta el mismo circuito
     * que anclas precargadas y recepción posterior.
     */
    public RegistrarConstatacionMaterialTempranaResultado registrarConstatacionMaterialTemprana(
            String actaId, TipoConstatacionMaterialTemprana tipo) {
        return cerrabilidad.registrarConstatacionMaterialTemprana(actaId, tipo);
    }

    /**
     * Demo: constata una medida preventiva aplicable durante el trámite (p. ej.
     * contravención en análisis por inspección o noticia posterior al
     * labrado), sin usar
     * {@link #registrarConstatacionMaterialTemprana(constatación D1/D2)} ni
     * satélite de tránsito. Requiere
     * {@code PENDIENTE_ANALISIS}; el ancla documental
     * {@code MEDIDA_PREVENTIVA} alinea con el cierre. Fachada; la lógica vive
     * en el soporte de cerrabilidad.
     */
    public RegistrarMedidaPreventivaPosteriorResultado registrarMedidaPreventivaPosterior(
            String actaId) {
        return cerrabilidad.registrarMedidaPreventivaPosterior(actaId);
    }

    /**
     * Mock interno: resultado final (demo / precarga). Uso: fábrica de datos.
     */
    public void setResultadoFinalCierreDemo(String actaId, ResultadoFinalCierreMock resultado) {
        cerrabilidad.setResultadoFinalDemo(actaId, resultado);
    }

    /**
     * Mock interno: orígenes materiales que condicionan pendientes
     * bloqueantes (demo / precarga). Uso: fábrica de datos.
     */
    public void setOrigenesBloqueantesCierreMaterialDemo(
            String actaId, java.util.Set<OrigenBloqueanteCierreMaterialMock> origenes) {
        cerrabilidad.setOrigenesBloqueantesMaterialDemo(actaId, origenes);
    }

    /**
     * Mock interno: hecho de pago informado precargado. Uso: fábrica de datos.
     */
    public void setPagoInformadoDemo(String actaId, PagoInformadoMock pago) {
        if (actaId == null) {
            return;
        }
        if (pago == null) {
            pagoInformadoPorActa.remove(actaId);
            return;
        }
        pagoInformadoPorActa.put(actaId, pago);
    }

    /**
     * Helper interno de mocks: asigna situación de pago de una acta precargada.
     * Uso restringido a la fábrica de datos demo.
     */
    public void setSituacionPago(String actaId, SituacionPagoMock situacionPago) {
        if (actaId == null) {
            return;
        }
        if (situacionPago == null || situacionPago == SituacionPagoMock.SIN_PAGO) {
            situacionPagoPorActa.remove(actaId);
            return;
        }
        situacionPagoPorActa.put(actaId, situacionPago);
    }

    /**
     * Helper interno de mocks: asigna el motivo de archivo vigente para una
     * acta precargada. Uso restringido a la fábrica de datos demo. Fachada:
     * delega en {@link ArchivoReingresoSupport}.
     */
    public void setMotivoArchivo(String actaId, String motivo) {
        archivoReingreso.setMotivoArchivo(actaId, motivo);
    }

    /**
     * Helper interno de mocks: asigna la acción pendiente vigente para una
     * acta precargada, sin ejecutar la transición operativa asociada. Uso
     * restringido a la fábrica de datos demo para poder representar estados
     * intermedios (por ejemplo, un caso que ya quedó en condición de
     * derivación a gestión externa) sin tener que simular todo el recorrido
     * que los supports materializan en runtime.
     */
    public void setAccionPendiente(String actaId, String accion) {
        if (actaId == null) {
            return;
        }
        if (accion == null || accion.isBlank()) {
            accionPendientePorActa.remove(actaId);
            return;
        }
        accionPendientePorActa.put(actaId, accion);
    }

    public List<BandejaConteo> listarBandejasConConteoOrdenadas() {
        Map<String, Integer> conteo = new HashMap<>();
        for (ActaMock acta : actas.values()) {
            String bandeja = acta.bandejaActual();
            conteo.put(bandeja, conteo.getOrDefault(bandeja, 0) + 1);
        }
        if (conteo.isEmpty()) {
            return List.of();
        }
        List<BandejaConteo> resultado = new ArrayList<>();
        for (String codigo : ORDEN_BANDEJAS_DEMO) {
            Integer n = conteo.remove(codigo);
            if (n != null) {
                resultado.add(new BandejaConteo(codigo, n));
            }
        }
        conteo.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> resultado.add(new BandejaConteo(e.getKey(), e.getValue())));
        return resultado;
    }

    public List<ActaMock> listarActasPorBandeja(String codigoBandeja) {
        return listarActasPorBandeja(codigoBandeja, null);
    }

    /**
     * Listado de la bandeja con filtro opcional por acci��n pendiente. Si
     * {@code accionPendiente} es {@code null} o en blanco, no se filtra.
     */
    public List<ActaMock> listarActasPorBandeja(String codigoBandeja, String accionPendiente) {
        return listarActasPorBandeja(codigoBandeja, accionPendiente, null);
    }

    /**
     * Listado de la bandeja con filtros opcionales por acción pendiente y por
     * situación de pago mock. Si un filtro es {@code null} o en blanco, no se
     * aplica.
     */
    public List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            SituacionPagoMock situacionPago) {
        return listarActasPorBandeja(
                codigoBandeja, accionPendiente, situacionPago, null, null, null);
    }

    /**
     * Filtros opcionales de cerrabilidad (además de bandeja, acción y pago). Si
     * un filtro es {@code null}, no aplica.
     */
    public List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            SituacionPagoMock situacionPago,
            ResultadoFinalCierreMock filtroResultadoFinal,
            Boolean filtroCerrable,
            PendienteBloqueanteCierreMock filtroPendienteBloqueante) {
        if (codigoBandeja == null) {
            return List.of();
        }
        String accionFiltro = (accionPendiente != null && !accionPendiente.isBlank()) ? accionPendiente : null;
        return actas.values().stream()
                .filter(a -> codigoBandeja.equals(a.bandejaActual()))
                .filter(a -> accionFiltro == null || accionFiltro.equals(accionPendientePorActa.get(a.id())))
                .filter(a -> situacionPago == null || situacionPago.equals(getSituacionPago(a.id())))
                .filter(a -> filtroResultadoFinal == null
                        || filtroResultadoFinal.equals(cerrabilidad.getResultadoFinal(a.id())))
                .filter(a -> filtroCerrable == null
                        || cerrabilidad.coincideFiltroCerrable(a, filtroCerrable))
                .filter(a -> filtroPendienteBloqueante == null
                        || cerrabilidad.coincideFiltroPendiente(a, filtroPendienteBloqueante))
                .sorted(Comparator.comparing(ActaMock::id))
                .toList();
    }

    public Optional<ActaMock> findActa(String id) {
        return Optional.ofNullable(actas.get(id));
    }

    public boolean existeActa(String id) {
        return actas.containsKey(id);
    }

    /**
     * Historial cronológico. Si la acta no tiene eventos cargados, lista vacía.
     */
    public List<ActaEventoMock> listarEventosActaOrdenados(String actaId) {
        List<ActaEventoMock> lista = eventosPorActa.get(actaId);
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        return lista.stream()
                .sorted(Comparator.comparing(ActaEventoMock::fechaHora))
                .toList();
    }

    /**
     * Si la acta no tiene documentos cargados, lista vacía. Fachada
     * pública; la lógica vive en {@link PiezasFirmaSupport}.
     */
    public List<ActaDocumentoMock> listarDocumentosPorActa(String actaId) {
        return piezasFirma.listarDocumentosPorActa(actaId);
    }

    /**
     * Si la acta no tiene notificaciones cargadas, lista vacía.
     */
    public List<ActaNotificacionMock> listarNotificacionesPorActa(String actaId) {
        List<ActaNotificacionMock> lista = notificacionesPorActa.get(actaId);
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        return List.copyOf(lista);
    }

    /**
     * Si la acta no declara piezas requeridas (caso típico fuera de
     * PENDIENTES_RESOLUCION_REDACCION), devuelve {@link Optional#empty()}.
     * Fachada pública; la lógica vive en {@link PiezasFirmaSupport}.
     */
    public Optional<ActaPiezasRequeridasMock> findPiezasRequeridas(String actaId) {
        return piezasFirma.findPiezasRequeridas(actaId);
    }

    /**
     * Catálogo de piezas requeridas; lista vacía si la acta no declara
     * piezas. Fachada pública; la lógica vive en {@link PiezasFirmaSupport}.
     */
    public List<String> listarPiezasRequeridas(String actaId) {
        return piezasFirma.listarPiezasRequeridas(actaId);
    }

    /**
     * Piezas ya producidas; lista vacía si la acta no declara piezas o no
     * se produjo ninguna todavía. Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public List<String> listarPiezasGeneradas(String actaId) {
        return piezasFirma.listarPiezasGeneradas(actaId);
    }

    /**
     * Piezas requeridas que todavía no fueron producidas. Si la acta no
     * declara piezas requeridas, lista vacía. Fachada pública; la lógica
     * vive en {@link PiezasFirmaSupport}.
     */
    public List<String> listarPiezasPendientes(String actaId) {
        return piezasFirma.listarPiezasPendientes(actaId);
    }

    /**
     * {@code true} si la acta declara piezas requeridas y todas están ya
     * producidas. {@code false} si aún falta alguna, o si la acta no
     * declara piezas. Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public boolean todasLasPiezasProducidas(String actaId) {
        return piezasFirma.todasLasPiezasProducidas(actaId);
    }

    /**
     * Demo: notificación entregada positivamente → bandeja análisis (solo
     * desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION). Fachada pública; la
     * lógica vive en {@link NotificacionSupport}.
     */
    public RegistrarNotificacionPositivaResultado registrarNotificacionPositiva(String actaId) {
        return notificacion.registrarNotificacionPositiva(actaId);
    }

    /**
     * Demo: notificación no entregada → acta retorna a PENDIENTE_ANALISIS con
     * marca {@link #ACCION_REINTENTAR_NOTIFICACION}. Fachada pública; la
     * lógica vive en {@link NotificacionSupport}.
     */
    public RegistrarNotificacionNegativaResultado registrarNotificacionNegativa(String actaId) {
        return notificacion.registrarNotificacionNegativa(actaId);
    }

    /**
     * Demo: reintento de notificación desde análisis. Fachada pública; la
     * lógica vive en {@link NotificacionSupport}.
     */
    public ReintentarNotificacionResultado reintentarNotificacion(String actaId) {
        return notificacion.reintentarNotificacion(actaId);
    }

    /**
     * Demo: notificación vencida → acta retorna a PENDIENTE_ANALISIS con
     * marca {@link #ACCION_EVALUAR_NOTIFICACION_VENCIDA}. Fachada pública;
     * la lógica vive en {@link NotificacionSupport}.
     */
    public RegistrarNotificacionVencidaResultado registrarNotificacionVencida(String actaId) {
        return notificacion.registrarNotificacionVencida(actaId);
    }

    /**
     * Demo: registrar que el administrado informó un pago (hecho informado),
     * sin asumir confirmación. La verificación posterior se materializa
     * separadamente con adjunto de comprobante + confirmación/observación.
     */
    public RegistrarPagoInformadoResultado registrarPagoInformado(String actaId) {
        return pagoInformado.registrarPagoInformado(actaId);
    }

    /**
     * Demo: adjuntar comprobante mock al pago informado. Deja el caso como
     * pendiente de confirmación y marca la tarea operativa.
     */
    public AdjuntarComprobantePagoInformadoResultado adjuntarComprobantePagoInformado(
            String actaId,
            String comprobanteNombreArchivo) {
        return pagoInformado.adjuntarComprobantePagoInformado(actaId, comprobanteNombreArchivo);
    }

    /**
     * Demo: confirmar el pago informado (acción mock interna). Si el resultado
     * final es ABSUELTO, no aplica. En caso contrario, deja
     * {@link ResultadoFinalCierreMock#PAGO_CONFIRMADO} (no cierra el expediente).
     */
    public ConfirmarPagoInformadoResultado confirmarPagoInformado(String actaId) {
        if (cerrabilidad.getResultadoFinal(actaId) == ResultadoFinalCierreMock.ABSUELTO) {
            return new ConfirmarPagoInformadoResultado(
                    ConfirmarPagoInformadoEstado.CONFLICT, actaId, getSituacionPago(actaId));
        }
        ConfirmarPagoInformadoResultado r = pagoInformado.confirmarPagoInformado(actaId);
        if (r.estado() == ConfirmarPagoInformadoEstado.OK) {
            cerrabilidad.setResultadoFinalDemo(actaId, ResultadoFinalCierreMock.PAGO_CONFIRMADO);
        }
        return r;
    }

    /**
     * Demo: observar/rechazar/desconocer el pago informado (acción mock interna).
     */
    public ObservarPagoInformadoResultado observarPagoInformado(String actaId) {
        return pagoInformado.observarPagoInformado(actaId);
    }

    /**
     * Demo: decisión posterior mínima sobre un caso vencido: reintentar la
     * notificación. Fachada pública; la lógica vive en
     * {@link NotificacionSupport}.
     */
    public ReintentarNotificacionVencidaResultado reintentarNotificacionVencida(String actaId) {
        return notificacion.reintentarNotificacionVencida(actaId);
    }

    /**
     * Demo: cierre desde análisis → bandeja CERRADAS (solo desde
     * PENDIENTE_ANALISIS). Fachada pública; la lógica vive en
     * {@link CierreSupport}.
     */
    public CerrarActaResultado cerrarActaDesdeAnalisis(String actaId) {
        return cierre.cerrarActaDesdeAnalisis(actaId);
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link #TIPO_GESTION_EXTERNA_APREMIO} → macro-bandeja
     * {@code GESTION_EXTERNA}. Aplica a actas en PENDIENTE_ANALISIS con
     * marca {@link #ACCION_DERIVAR_GESTION_EXTERNA} (derivación inicial) o
     * con marca {@link #ACCION_REVISION_POST_GESTION_EXTERNA}
     * (re-derivación del expediente que ya volvió desde gestión externa).
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public DerivarAGestionExternaResultado derivarAApremio(String actaId) {
        return gestionExterna.derivarAApremio(actaId);
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link #TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ} → macro-bandeja
     * {@code GESTION_EXTERNA}. Aplica a actas en PENDIENTE_ANALISIS con
     * marca {@link #ACCION_DERIVAR_GESTION_EXTERNA} (derivación inicial) o
     * con marca {@link #ACCION_REVISION_POST_GESTION_EXTERNA}
     * (re-derivación del expediente que ya volvió desde gestión externa).
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public DerivarAGestionExternaResultado derivarAJuzgadoDePaz(String actaId) {
        return gestionExterna.derivarAJuzgadoDePaz(actaId);
    }

    /**
     * Demo: retorno efectivo desde la macro-bandeja {@code GESTION_EXTERNA} →
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link #ACCION_REVISION_POST_GESTION_EXTERNA}. Solo aplica a actas en
     * {@code GESTION_EXTERNA} con {@code permiteReingreso = true}. Preserva
     * el {@code tipoGestionExterna} como trazabilidad sintética de la
     * gestión externa de la que provino. Fachada pública; la lógica vive en
     * {@link GestionExternaSupport}.
     */
    public ReingresarDesdeGestionExternaResultado reingresarActaDesdeGestionExterna(String actaId) {
        return gestionExterna.reingresarActaDesdeGestionExterna(actaId);
    }

    /**
     * Helper interno de mocks: asigna el tipo de gestión externa vigente
     * para una acta precargada directamente en {@code GESTION_EXTERNA}, sin
     * ejecutar la transición operativa asociada. Uso restringido a la
     * fábrica de datos demo. Fachada: delega en {@link GestionExternaSupport}.
     */
    public void setTipoGestionExterna(String actaId, String tipo) {
        gestionExterna.setTipoGestionExterna(actaId, tipo);
    }

    /**
     * Demo: archivo directo desde análisis → macro-bandeja ARCHIVO. Fachada
     * pública; la lógica vive en {@link ArchivoReingresoSupport}.
     */
    public ArchivarActaResultado archivarActaDesdeAnalisis(String actaId) {
        return archivoReingreso.archivarActaDesdeAnalisis(actaId);
    }

    /**
     * Demo: archivo posterior a evaluación de vencimiento → macro-bandeja
     * ARCHIVO. Fachada pública; la lógica vive en
     * {@link ArchivoReingresoSupport}.
     */
    public ArchivarPorVencimientoResultado archivarPorVencimiento(String actaId) {
        return archivoReingreso.archivarPorVencimiento(actaId);
    }

    /**
     * Demo: reingreso explícito desde la macro-bandeja ARCHIVO → vuelve a
     * PENDIENTE_ANALISIS con marca {@link #ACCION_REVISION_POST_REINGRESO} y
     * {@code motivoArchivo} previo preservado. Fachada pública; la lógica vive
     * en {@link ArchivoReingresoSupport}.
     */
    public ReingresarActaResultado reingresarActaDesdeArchivo(String actaId) {
        return archivoReingreso.reingresarActaDesdeArchivo(actaId);
    }

    /**
     * Demo: produce la pieza MEDIDA_PREVENTIVA (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarMedidaPreventivaResultado generarMedidaPreventiva(String actaId) {
        return piezasFirma.generarMedidaPreventiva(actaId);
    }

    /**
     * Demo: produce la pieza NOTIFICACION_ACTA (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarNotificacionActaResultado generarNotificacionActa(String actaId) {
        return piezasFirma.generarNotificacionActa(actaId);
    }

    /**
     * Demo: produce la pieza NULIDAD como pieza no-fallo dentro del
     * circuito documental/resolutivo (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Comparte agregador con las otras piezas: si quedan
     * piezas pendientes, la acta permanece en la misma bandeja; si no,
     * pasa a PENDIENTE_FIRMA. Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarNulidadResultado generarNulidad(String actaId) {
        return piezasFirma.generarNulidad(actaId);
    }

    /**
     * Demo: firma individual de un documento puntual de la acta. La acta
     * sólo abandona la bandeja PENDIENTE_FIRMA cuando todos los documentos
     * firmables pasan a estado FIRMADO; al firmarse el último, pasa a
     * PENDIENTE_NOTIFICACION, salvo que el documento recién firmado sea
     * de tipo {@code NULIDAD}: en ese caso la acta pasa directamente a
     * {@code CERRADAS} como salida invalidante del expediente, sin
     * arrancar circuito de notificación. Fachada pública; la lógica vive
     * en {@link PiezasFirmaSupport}, que delega en
     * {@link NotificacionSupport} la materialización inicial de la
     * notificación cuando corresponde.
     */
    public FirmarDocumentoResultado firmarDocumento(String actaId, String documentoId) {
        return piezasFirma.firmarDocumento(actaId, documentoId);
    }

    /**
     * Demo: registra solicitud de pago voluntario en etapa temprana del
     * expediente (antes de análisis formal) y mueve el caso a
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link #ACCION_EVALUAR_PAGO_VOLUNTARIO}. Fachada pública; la lógica
     * vive en {@link PagoVoluntarioSupport}.
     */
    public RegistrarSolicitudPagoVoluntarioResultado registrarSolicitudPagoVoluntario(String actaId) {
        return pagoVoluntario.registrarSolicitudPagoVoluntario(actaId);
    }

    /**
     * Crea un acta mock mínima para demo (numeración {@code ACTA-DEMO-nnnn}),
     * con anclas/documentación coherente y satélites {@link ActaTransitoMock} /
     * {@link ActaBromatologiaMock} según dependencia. Sin DB ni numeración
     * real.
     */
    public CrearActaMockDemoResultado crearActaMockDemo(CrearActaMockDemoRequest r) {
        if (r == null) {
            return new CrearActaMockDemoResultado(CrearActaMockDemoEstado.BAD_REQUEST, "body requerido", null);
        }
        if (r.dependencia() == null || r.dependencia().isBlank()) {
            return new CrearActaMockDemoResultado(
                    CrearActaMockDemoEstado.BAD_REQUEST, "dependencia requerida", null);
        }
        final DependenciaActaDemo dep;
        try {
            dep = DependenciaActaDemo.valueOf(r.dependencia().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return new CrearActaMockDemoResultado(
                    CrearActaMockDemoEstado.BAD_REQUEST, "dependencia desconocida: " + r.dependencia(), null);
        }
        boolean ejeUrbano = Boolean.TRUE.equals(r.ejeUrbano());
        boolean rodado = Boolean.TRUE.equals(r.rodadoRetenidoOSecuestrado());
        boolean doc = Boolean.TRUE.equals(r.documentacionRetenida());
        boolean claus = Boolean.TRUE.equals(r.medidaPreventivaClausura());
        boolean paral = Boolean.TRUE.equals(r.medidaPreventivaParalizacionObra());
        boolean decom = Boolean.TRUE.equals(r.decomisoSustanciasAlimenticias());
        TipoActaDemo tipo;
        if (r.tipoActaDemo() != null && !r.tipoActaDemo().isBlank()) {
            try {
                tipo = TipoActaDemo.valueOf(r.tipoActaDemo().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return new CrearActaMockDemoResultado(
                        CrearActaMockDemoEstado.BAD_REQUEST, "tipoActaDemo inválido: " + r.tipoActaDemo(), null);
            }
        } else {
            tipo =
                    switch (dep) {
                        case TRANSITO -> TipoActaDemo.TRANSITO;
                        case BROMATOLOGIA -> TipoActaDemo.BROMATOLOGIA;
                        case INSPECCIONES, FISCALIZACION -> TipoActaDemo.CONTRAVENCION;
                    };
        }
        if (!validarTipoConDependencia(dep, tipo)) {
            return new CrearActaMockDemoResultado(
                    CrearActaMockDemoEstado.BAD_REQUEST, "dependencia y tipoActaDemo incompatibles (demo).", null);
        }
        if (!validarBanderasConDependencia(dep, ejeUrbano, rodado, doc, claus, paral, decom)) {
            return new CrearActaMockDemoResultado(
                    CrearActaMockDemoEstado.BAD_REQUEST, "banderas inválidas o incoherentes con la dependencia (demo).", null);
        }

        int n = contadorActaLabradoMockDemo.incrementAndGet();
        String id = "ACTA-DEMO-" + String.format("%04d", n);
        String numeracion = id;
        while (actas.containsKey(id)) {
            n = contadorActaLabradoMockDemo.incrementAndGet();
            id = "ACTA-DEMO-" + String.format("%04d", n);
            numeracion = id;
        }
        String sufijoDoc = "DEMO" + n;

        dependenciaDemoPorActa.put(id, dep.name());
        tipoActaDemoPorActa.put(id, tipo.name());
        if (dep == DependenciaActaDemo.BROMATOLOGIA) {
            actaBromatologiaMockPorActa.put(id, new ActaBromatologiaMock(decom));
        }
        if (dep == DependenciaActaDemo.TRANSITO) {
            ActaTransitoMock tr = new ActaTransitoMock(ejeUrbano, rodado, doc, false);
            actaTransitoMockPorActa.put(id, tr);
            agregarAnclasMaterialesDesdeTransito(id, tr, sufijoDoc);
        } else if (dep == DependenciaActaDemo.INSPECCIONES) {
            anclaSoloMedida(id, claus, sufijoDoc, "ancla_clausura_inspeccion");
        } else if (dep == DependenciaActaDemo.FISCALIZACION) {
            anclaSoloMedida(id, paral, sufijoDoc, "ancla_paralizacion_obra");
        }

        List<ActaDocumentoMock> docsCreados = documentosPorActa.getOrDefault(id, List.of());
        String dominio;
        if (dep == DependenciaActaDemo.TRANSITO) {
            dominio = ejeUrbano ? "TRANSITO_URBANO" : "TRANSITO";
        } else if (dep == DependenciaActaDemo.BROMATOLOGIA) {
            dominio = "BROMATOLOGIA";
        } else {
            dominio = dep == DependenciaActaDemo.INSPECCIONES ? "INSPECCIONES" : "FISCALIZACION_OBRA";
        }
        String res =
                "Alta acta mock demo: dependencia "
                        + dep
                        + ", tipo "
                        + tipo
                        + (dep == DependenciaActaDemo.BROMATOLOGIA && decom
                                ? "; decomiso (sust. alimenticias) en expediente (mock, no medida)."
                                : ".");
        ActaMock acta =
                new ActaMock(
                        id,
                        numeracion,
                        dominio,
                        BLOQUE_D1_CAPTURA,
                        "EN_CURSO",
                        "ACTIVA",
                        false,
                        true,
                        !docsCreados.isEmpty(),
                        false,
                        LocalDateTime.now(),
                        "Infractor pendiente (demo)",
                        "—",
                        "Sistema (labrado demo)",
                        res,
                        BANDEJA_ACTAS_EN_ENRIQUECIMIENTO);
        actas.put(id, acta);
        int idxEvt = 1;
        agregarEventoCrear(
                id,
                "EVT-" + id + "-" + String.format("%02d", idxEvt),
                "LABRADO_MOCK",
                "Alta acta mock mínima (prototipo in-memory) para prueba de circuito (demo " + n + ").");
        return new CrearActaMockDemoResultado(CrearActaMockDemoEstado.OK, null, acta);
    }

    private void agregarEventoCrear(String actaId, String eventoId, String tipo, String descripcion) {
        List<ActaEventoMock> lista = eventosPorActa.get(actaId);
        if (lista == null) {
            lista = new ArrayList<>();
            eventosPorActa.put(actaId, lista);
        }
        lista.add(
                new ActaEventoMock(
                        eventoId,
                        actaId,
                        LocalDateTime.now(),
                        tipo,
                        BLOQUE_D1_CAPTURA,
                        BLOQUE_D1_CAPTURA,
                        descripcion));
    }

    private void agregarAnclasMaterialesDesdeTransito(
            String id, ActaTransitoMock tr, String sf) {
        int seq = 1;
        if (tr.rodadoRetenidoOSecuestrado()) {
            agregarOAncla(
                    id,
                    "DOC-" + sf + "-" + seq,
                    TIPO_DOC_ACUSE_RETENCION_VEHICULO,
                    "mock_retencion_rodado_" + seq + ".pdf");
            seq++;
        }
        if (tr.documentacionRetenida()) {
            agregarOAncla(
                    id,
                    "DOC-" + sf + "-" + seq,
                    TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL,
                    "mock_constatacion_doc_" + seq + ".pdf");
            seq++;
        }
        if (tr.medidaPreventivaAplicable()) {
            anclaMedida(
                    id, "DOC-" + sf + "-" + seq, "mock_constatacion_medida_" + seq + ".pdf");
        }
    }

    private void agregarOAncla(
            String id, String docId, String tipo, String nombre) {
        List<ActaDocumentoMock> docs = documentosPorActa.get(id);
        if (docs == null) {
            docs = new ArrayList<>();
            documentosPorActa.put(id, docs);
        }
        docs.add(new ActaDocumentoMock(docId, id, tipo, ESTADO_DOC_ADJUNTO, nombre));
    }

    private void anclaMedida(String actaId, String docId, String nombre) {
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null) {
            docs = new ArrayList<>();
            documentosPorActa.put(actaId, docs);
        }
        docs.add(
                new ActaDocumentoMock(
                        docId, actaId, TIPO_ANCLA_MEDIDA_PREVENTIVA, ESTADO_DOC_EMITIDO, nombre));
    }

    private void anclaSoloMedida(
            String actaId, boolean requiere, String sf, String baseNombre) {
        if (!requiere) {
            return;
        }
        anclaMedida(actaId, "DOC-" + sf + "-1", baseNombre + ".pdf");
    }

    private static boolean validarBanderasConDependencia(
            DependenciaActaDemo d,
            boolean ejeU,
            boolean rod,
            boolean docR,
            boolean claus,
            boolean paral,
            boolean decom) {
        return switch (d) {
            case TRANSITO -> !decom && !claus && !paral;
            case INSPECCIONES -> !ejeU && !rod && !docR && !decom && !paral;
            case FISCALIZACION -> !ejeU && !rod && !docR && !decom && !claus;
            case BROMATOLOGIA -> !ejeU && !rod && !docR && !claus && !paral;
        };
    }

    private static boolean validarTipoConDependencia(DependenciaActaDemo d, TipoActaDemo t) {
        return switch (d) {
            case TRANSITO -> t == TipoActaDemo.TRANSITO;
            case BROMATOLOGIA -> t == TipoActaDemo.BROMATOLOGIA;
            case INSPECCIONES, FISCALIZACION -> t == TipoActaDemo.CONTRAVENCION;
        };
    }
}
