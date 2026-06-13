package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaBromatologiaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaTransitoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaAsignacion;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaClasificador;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaContexto;
import ar.gob.malvinas.faltas.prototipo.web.dto.CrearActaMockDemoRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_EN_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D1_CAPTURA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.bandejaPermitePagoVoluntario;
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
            "PENDIENTES_FALLO",
            "CON_APELACION",
            "GESTION_EXTERNA",
            "PARALIZADAS",
            "ARCHIVO",
            "CERRADAS");

    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis por notificación fallida y requiere decisión posterior. El
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
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: la gestión
     * externa devolvió un resultado sustantivo que debe formalizarse con un
     * nuevo fallo/resolución interno antes de alterar el fondo del expediente.
     */
    public static final String ACCION_DICTAR_FALLO_POST_GESTION_EXTERNA = "DICTAR_FALLO_POST_GESTION_EXTERNA";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis por reactivación explícita desde la macro-bandeja
     * {@code PARALIZADAS}. Permite distinguir y filtrar dentro de análisis
     * los casos recién reactivados de los que vinieron por el circuito
     * operativo normal o por reingreso desde archivo/gestión externa. La
     * información histórica de la paralización queda en los eventos como
     * trazabilidad; no bloquea acciones luego de reactivar.
     */
    public static final String ACCION_REVISION_POST_REACTIVACION = "REVISION_POST_REACTIVACION";
    public static final String ACCION_PARALIZACION_ESPERA_DOCUMENTAL = "PARALIZACION_ESPERA_DOCUMENTAL";
    public static final String ACCION_PARALIZACION_ESPERA_INFORME_EXTERNO =
            "PARALIZACION_ESPERA_INFORME_EXTERNO";
    public static final String ACCION_PARALIZACION_ESPERA_OTRA_DEPENDENCIA =
            "PARALIZACION_ESPERA_OTRA_DEPENDENCIA";
    public static final String ACCION_PARALIZACION_ESPERA_RESOLUCION_RELACIONADA =
            "PARALIZACION_ESPERA_RESOLUCION_RELACIONADA";
    public static final String ACCION_PARALIZACION_OTRO = "PARALIZACION_OTRO";
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

    /**
     * Motivo de archivo asignado cuando un acta es anulada administrativamente
     * por nulidad desde la etapa de enriquecimiento. El archivo por nulidad
     * conserva {@code permiteReingreso=true}: puede reingresarse si la
     * decisión fue incorrecta.
     */
    public static final String MOTIVO_ARCHIVO_NULIDAD = "NULIDAD";

    /**
     * Marca operativa de actas en {@code ACTAS_EN_ENRIQUECIMIENTO}: el
     * expediente requiere completar datos de enriquecimiento antes de
     * avanzar. Se asigna al cargar datos demo (D1/D2) y al reingresar
     * desde un archivo por nulidad originado en enriquecimiento.
     */
    public static final String ACCION_COMPLETAR_ENRIQUECIMIENTO = "COMPLETAR_ENRIQUECIMIENTO";

    public enum EnviarActaANotificacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record EnviarActaANotificacionResultado(
            EnviarActaANotificacionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum AnularActaPorNulidadEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record AnularActaPorNulidadResultado(
            AnularActaPorNulidadEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String motivoArchivo) {
    }

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

    public enum PagoCondenaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record PagoCondenaResultado(
            PagoCondenaEstado estado,
            String actaId,
            SituacionPagoCondena situacionPagoCondena) {
    }

    record PagoCondenaPrecondicion(
            PagoCondenaEstado estado,
            SituacionPagoCondena situacion) {

        PagoCondenaResultado resultado() {
            return new PagoCondenaResultado(estado, null, situacion);
        }
    }

    public enum ConsentirCondenaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ConsentirCondenaResultado(
            ConsentirCondenaEstado estado,
            String actaId,
            ResultadoFinalCierreMock resultadoFinal,
            SituacionPagoCondena situacionPagoCondena) {
    }

    public enum ConsentirCondenaYRegistrarPagoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ConsentirCondenaYRegistrarPagoResultado(
            ConsentirCondenaYRegistrarPagoEstado estado,
            String actaId,
            ResultadoFinalCierreMock resultadoFinal,
            SituacionPagoCondena situacionPagoCondena) {
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
            String tipoGestionExterna,
            String motivo) {
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

    public enum ReingresarDesdeApremioSinPagoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReingresarDesdeApremioSinPagoResultado(
            ReingresarDesdeApremioSinPagoEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente) {
    }

    public enum RegistrarPagoEnApremioEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarPagoEnApremioResultado(
            RegistrarPagoEnApremioEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum ReingresarDesdeJuzgadoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReingresarDesdeJuzgadoResultado(
            ReingresarDesdeJuzgadoEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente,
            String resolucion) {
    }

    public enum ResultadoExternoPostGestion {
        MODIFICA_MONTO,
        ABSUELVE
    }

    public enum ReactivarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReactivarActaResultado(
            ReactivarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente) {
    }

    public enum MotivoParalizacionActa {
        ESPERA_DOCUMENTAL,
        ESPERA_INFORME_EXTERNO,
        ESPERA_OTRA_DEPENDENCIA,
        ESPERA_RESOLUCION_RELACIONADA,
        OTRO
    }

    public enum ParalizarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ParalizarActaResultado(
            ParalizarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String situacionAdministrativa,
            String accionPendiente) {
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

    public enum GenerarResolucionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarResolucionResultado(
            GenerarResolucionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum GenerarRectificacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarRectificacionResultado(
            GenerarRectificacionEstado estado,
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
        CONFLICT,
        /**
         * El acta está en revisión (D1/D2, aún no validada como
         * notificable): el portal no admite acciones sustantivas.
         */
        CONFLICT_EN_REVISION,
        /**
         * Existe un fallo condenatorio dictado (pendiente de firma o ya
         * firmado): el portal no puede solicitar pago voluntario mientras
         * haya una resolución en curso.
         */
        CONFLICT_FALLO_DICTADO
    }

    public record RegistrarSolicitudPagoVoluntarioResultado(
            RegistrarSolicitudPagoVoluntarioEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente,
            BigDecimal montoPagoVoluntario) {
    }

    public enum FijarMontoPagoVoluntarioEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record FijarMontoPagoVoluntarioResultado(
            FijarMontoPagoVoluntarioEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            BigDecimal montoPagoVoluntario) {
    }

    /**
     * Situación de pago mock, visible por API y separada de {@code accionPendiente}.
     * Modela en mínimo: solicitud, pago informado, pendiente de confirmación,
     * confirmado, observado y vencido.
     */
    public enum SituacionPagoMock {
        SIN_PAGO,
        SOLICITADO,
        PAGO_INFORMADO,
        PENDIENTE_CONFIRMACION,
        CONFIRMADO,
        OBSERVADO,
        /** El plazo/oportunidad de pago voluntario venció sin pago; el trámite puede continuar a fallo. */
        VENCIDO
    }

    /**
     * Situación de pago de condena firme. Es independiente de
     * {@link SituacionPagoMock}: no usa montoPagoVoluntario ni acciones del
     * pago voluntario.
     */
    public enum SituacionPagoCondena {
        NO_APLICA,
        PENDIENTE,
        INFORMADO,
        CONFIRMADO,
        OBSERVADO
    }

    /**
     * Resultado final relevante para evaluar si el expediente puede quedar en
     * condición de cierre (independiente de la bandeja y de los pendientes).
     *
     * <p>Valores que habilitan cerrabilidad operativa: {@link #ABSUELTO} y
     * {@link #PAGO_CONFIRMADO}. {@link #CONDENADO} marca el caso post fallo
     * condenatorio notificado mientras el plazo de apelación esté abierto;
     * {@link #CONDENA_FIRME} marca el caso una vez vencido ese plazo sin
     * apelación. Ninguno de los dos últimos cierra automáticamente: dejan
     * el expediente listo para slices futuros (pago post condena firme o
     * gestión externa).
     */
    public enum ResultadoFinalCierreMock {
        SIN_RESULTADO_FINAL,
        ABSUELTO,
        PAGO_CONFIRMADO,
        CONDENADO,
        CONDENA_FIRME
    }

    /**
     * Circuito/origen del pago confirmado o en curso.
     * Especializa {@link SituacionPagoMock} sin reemplazarla.
     */
    public enum TipoPago {
        NO_APLICA,
        VOLUNTARIO,
        CONDENA
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
        CONFLICT,
        /** Monto de pago voluntario nulo o cero: Dirección aún no lo fijó. */
        CONFLICT_SIN_MONTO
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

    public enum InformarPagoVoluntarioDesdePortalEstado {
        OK,
        NOT_FOUND,
        /** Acta en bandeja no operable o condición general no compatible. */
        CONFLICT,
        /** Monto no fijado todavía por Dirección de Faltas. */
        CONFLICT_SIN_MONTO,
        /** Pago ya informado o pendiente de confirmación: no se puede duplicar. */
        CONFLICT_YA_INFORMADO,
        /** Pago ya confirmado: no aplica. */
        CONFLICT_YA_CONFIRMADO,
        /**
         * Existe un fallo condenatorio dictado (pendiente de firma o ya
         * firmado): el portal no puede pagar voluntario mientras haya una
         * resolución en curso.
         */
        CONFLICT_FALLO_DICTADO
    }

    public record InformarPagoVoluntarioDesdePortalResultado(
            InformarPagoVoluntarioDesdePortalEstado estado,
            String actaId,
            SituacionPagoMock situacionPago) {
    }

    public enum ConfirmarPagoVoluntarioExternoEstado {
        OK,
        NOT_FOUND,
        /** Acta en condición no compatible (cerrada, archivo, gestión externa). */
        CONFLICT,
        /** No existe pago informado/pendiente de confirmación previo. */
        CONFLICT_SIN_PAGO_PENDIENTE,
        /** Pago ya confirmado: idempotencia rechazada. */
        CONFLICT_YA_CONFIRMADO,
        /** El monto recibido no coincide con el monto fijado. */
        CONFLICT_MONTO_DISTINTO
    }

    public record ConfirmarPagoVoluntarioExternoResultado(
            ConfirmarPagoVoluntarioExternoEstado estado,
            String actaId,
            SituacionPagoMock situacionPago) {
    }

    public enum FirmarDocumentoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    /**
     * Acciones de dictado de fallo (absolutorio o condenatorio) en análisis
     * jurídico. Comparten estructura de resultado: producen el documento
     * resolutorio mock en {@link PrototipoConstantes#ESTADO_DOC_PENDIENTE_FIRMA}
     * y mueven el acta a {@code PENDIENTE_FIRMA}, sin cambiar
     * {@code resultadoFinal} todavía (el resultado se materializa al
     * notificarse positivamente el fallo).
     */
    public enum DictarFalloEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record DictarFalloResultado(
            DictarFalloEstado estado,
            String actaId,
            String documentoId,
            String tipoDocumento,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum RegistrarVencimientoPagoVoluntarioEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarVencimientoPagoVoluntarioResultado(
            RegistrarVencimientoPagoVoluntarioEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            SituacionPagoMock situacionPago,
            java.math.BigDecimal montoPagoVoluntario) {
    }

    public enum RegistrarVencimientoPlazoApelacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarVencimientoPlazoApelacionResultado(
            RegistrarVencimientoPlazoApelacionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String resultadoFinal) {
    }

    /**
     * Canales válidos para registrar la presentación de apelación/recurso
     * mientras el plazo está abierto.
     */
    public enum CanalPresentacionApelacionMock {
        PORTAL_INFRACTOR,
        PRESENCIAL_DIRECCION
    }

    public enum RegistrarApelacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarApelacionResultado(
            RegistrarApelacionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String resultadoFinal,
            String canal) {
    }

    /**
     * Resultados válidos para resolver mock de apelación/recurso ya
     * presentado.
     */
    public enum ResultadoResolucionApelacionMock {
        RECHAZADA,
        ACEPTADA_ABSUELVE
    }

    public enum ResolverApelacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ResolverApelacionResultado(
            ResolverApelacionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String resultadoFinal,
            String resultadoResolucion) {
    }

    public record FirmarDocumentoResultado(
            FirmarDocumentoEstado estado,
            String actaId,
            String documentoId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public record NotificacionCorreoLoteItem(
            String notificacionId,
            String actaId,
            String acta,
            String tipo,
            String canal,
            String referenciaExterna,
            String estadoNotificacion,
            String resultadoNotificacion,
            String destinatario,
            String domicilio,
            String observacion) {
    }

    public record CorreoPostalTrazabilidadItem(
            String acta,
            String actaId,
            String notificacionId,
            String tipo,
            String canal,
            String estadoNotificacion,
            String resultadoNotificacion,
            String loteId,
            String estadoLote,
            LocalDateTime fechaGeneracion,
            LocalDateTime fechaProcesamiento,
            String observacion,
            String referenciaExterna) {
    }

    public record GenerarLoteCorreoResultado(
            String loteId,
            int cantidad,
            String nombreArchivo,
            String rutaArchivo,
            List<NotificacionCorreoLoteItem> notificaciones) {
    }

    public record ProcesarRespuestaCorreoResultado(
            int total,
            int positivas,
            int negativas,
            int vencidas,
            int errores,
            List<String> detalleErrores,
            String nombreArchivo,
            String rutaArchivoProcesado) {
    }

    public record CorreoPostalNotificacionListaItem(
            String notificacionId,
            String actaId,
            String acta,
            String tipo,
            String canal,
            String estado,
            String resultado,
            String destinatario,
            String domicilio,
            String observacion) {
    }

    public record CorreoLoteResumen(
            String loteId,
            int cantidad,
            String nombreArchivo,
            String rutaArchivo,
            String estado,
            LocalDateTime fechaGeneracion,
            List<String> tiposIncluidos,
            String tipoDominante,
            int positivas,
            int negativas,
            int vencidas,
            List<NotificacionCorreoLoteItem> notificaciones) {
    }

    public record AnularLoteCorreoResultado(
            String estado,
            String mensaje,
            String loteId) {

        public static AnularLoteCorreoResultado ok(String loteId, String mensaje) {
            return new AnularLoteCorreoResultado("OK", mensaje, loteId);
        }

        public static AnularLoteCorreoResultado notFound(String loteId) {
            return new AnularLoteCorreoResultado("NOT_FOUND", "Lote no encontrado: " + loteId, loteId);
        }

        public static AnularLoteCorreoResultado conflict(String loteId, String mensaje) {
            return new AnularLoteCorreoResultado("CONFLICT", mensaje, loteId);
        }
    }

    public record EnviarIndividualCorreoResultado(
            String estado,
            String mensaje,
            String notificacionId,
            NotificacionCorreoLoteItem notificacion) {

        public static EnviarIndividualCorreoResultado ok(
                String notificacionId,
                String mensaje,
                NotificacionCorreoLoteItem notificacion) {
            return new EnviarIndividualCorreoResultado("OK", mensaje, notificacionId, notificacion);
        }

        public static EnviarIndividualCorreoResultado notFound(String notificacionId) {
            return new EnviarIndividualCorreoResultado(
                    "NOT_FOUND",
                    "Notificacion no encontrada: " + notificacionId,
                    notificacionId,
                    null);
        }

        public static EnviarIndividualCorreoResultado conflict(String notificacionId, String mensaje) {
            return new EnviarIndividualCorreoResultado("CONFLICT", mensaje, notificacionId, null);
        }
    }

    public record NotificacionMunicipalVista(
            String notificacionId,
            String actaId,
            String acta,
            String tipo,
            String canal,
            String estado,
            String resultado,
            String destinatario,
            String domicilio,
            String observacion,
            String qrNotificacion,
            LocalDateTime fechaPreparacion,
            LocalDateTime fechaEnvio) {
    }

    public enum ConfirmarVisualizacionNotificacionPortalEstado {
        OK,
        NOT_FOUND,
        SIN_NOTIFICACION_PENDIENTE,
        CONFLICT
    }

    public record ConfirmarVisualizacionNotificacionPortalResultado(
            ConfirmarVisualizacionNotificacionPortalEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            ActaNotificacionMock notificacion) {
    }

    /**
     * Documento del expediente formalmente visible para el infractor desde
     * el portal: firmado y en etapa de notificación
     * ({@code pendienteNotificacion=true}) o ya notificado
     * ({@code notificado=true}). Documentos internos, adjuntos o piezas sin
     * firma nunca se listan acá.
     */
    public record DocumentoPortalVista(
            String tipoDocumento,
            String titulo,
            String estadoDocumento,
            boolean notificado,
            boolean pendienteNotificacion) {
    }

    public enum VerDocumentoPortalEstado {
        OK_NOTIFICADO,
        OK_YA_NOTIFICADO,
        NOT_FOUND,
        CONFLICT
    }

    public record VerDocumentoPortalResultado(
            VerDocumentoPortalEstado estado,
            String actaId,
            String tipoDocumento) {
    }

    public enum RegistrarAcuseNotificadorMunicipalEstado {
        OK,
        NOT_FOUND,
        WRONG_CHANNEL,
        BAD_REQUEST,
        CONFLICT
    }

    public record RegistrarAcuseNotificadorMunicipalResultado(
            RegistrarAcuseNotificadorMunicipalEstado estado,
            String actaId,
            String acta,
            String bandejaActual,
            String estadoProcesoActual,
            ActaNotificacionMock notificacion,
            NotificacionMunicipalVista vista) {

        static RegistrarAcuseNotificadorMunicipalResultado notFound() {
            return new RegistrarAcuseNotificadorMunicipalResultado(
                    RegistrarAcuseNotificadorMunicipalEstado.NOT_FOUND, null, null, null, null, null, null);
        }

        static RegistrarAcuseNotificadorMunicipalResultado wrongChannel(String actaId) {
            return new RegistrarAcuseNotificadorMunicipalResultado(
                    RegistrarAcuseNotificadorMunicipalEstado.WRONG_CHANNEL, actaId, null, null, null, null, null);
        }

        static RegistrarAcuseNotificadorMunicipalResultado badRequest(String actaId) {
            return new RegistrarAcuseNotificadorMunicipalResultado(
                    RegistrarAcuseNotificadorMunicipalEstado.BAD_REQUEST, actaId, null, null, null, null, null);
        }

        static RegistrarAcuseNotificadorMunicipalResultado conflict(String actaId) {
            return new RegistrarAcuseNotificadorMunicipalResultado(
                    RegistrarAcuseNotificadorMunicipalEstado.CONFLICT, actaId, null, null, null, null, null);
        }
    }

    public record BandejaConteo(String codigo, int cantidadActas) {
    }

    public record SubBandejaConteo(String codigo, int cantidad) {
    }

    public record BandejaResumenOperativo(
            String codigo, int cantidadActas, List<SubBandejaConteo> subBandejas) {
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
     * Observaci&oacute;n de paralizaci&oacute;n por acta. Se guarda al paralizar y se
     * expone en el detalle mientras el acta permanece en PARALIZADAS.
     * Valor {@code null} si no se ingres&oacute; observaci&oacute;n.
     */
    private final Map<String, String> observacionParalizacionPorActa = new LinkedHashMap<>();

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
     * Monto del acta fijado por Dirección de Faltas al habilitar el pago
     * voluntario (acción administrativa "Pago voluntario"). Si una acta no
     * está en el mapa, no hay monto fijado: el futuro portal del infractor
     * deberá mostrar "Solicitar pago voluntario"; con monto fijado y pago
     * voluntario habilitado deberá mostrar "Pagar".
     *
     * <p>Este monto NO genera comprobantes (sin EM, sin RC, sin Cmte/Pref/Nro):
     * los comprobantes sólo se materializan en el proceso externo de pago,
     * que se modelará en un slice posterior.
     */
    private final Map<String, BigDecimal> montoPagoVoluntarioPorActa = new LinkedHashMap<>();
    /**
     * Monto de condena fijado al dictar fallo condenatorio. Distinto de
     * {@link #montoPagoVoluntarioPorActa}: no genera comprobantes ni
     * habilita pago post condena en este slice.
     */
    private final Map<String, BigDecimal> montoCondenaPorActa = new LinkedHashMap<>();
    private final Map<String, ResultadoExternoPostGestion> resultadoExternoPostGestionPorActa = new LinkedHashMap<>();
    private final Map<String, BigDecimal> montoCondenaSugeridoPostGestionExternaPorActa = new LinkedHashMap<>();
    /**
     * Situación operativa del pago de condena. Solo aplica cuando el
     * resultado final es CONDENA_FIRME; fuera de ese caso se expone NO_APLICA.
     */
    private final Map<String, SituacionPagoCondena> situacionPagoCondenaPorActa = new LinkedHashMap<>();
    private final Map<String, TipoPago> tipoPagoPorActa = new LinkedHashMap<>();
    /**
     * Mock de datos de tránsito por acta. Solo se puebla donde el escenario
     * demo asigna flags explícitos (p. ej. nacimiento de condiciones
     * materiales desde dato, no como acción de circuito).
     */
    private final Map<String, ActaTransitoMock> actaTransitoMockPorActa = new LinkedHashMap<>();

    private final Map<String, ActaBromatologiaMock> actaBromatologiaMockPorActa = new LinkedHashMap<>();
    private final Map<String, String> dependenciaDemoPorActa = new LinkedHashMap<>();
    private final Map<String, String> tipoActaDemoPorActa = new LinkedHashMap<>();

    /** Patente/matrícula vehicular por actaId; solo actas con dato de rodado. */
    private final Map<String, String> patenteVehiculoPorActa = new LinkedHashMap<>();
    private final AtomicInteger contadorActaLabradoMockDemo = new AtomicInteger(0);

    /**
     * Soporte funcional del área archivo/reingreso: archivo directo desde
     * análisis, archivo post evaluación de vencimiento, reingreso desde
     * archivo, motivoArchivo y eventos asociados. Extraído del store para
     * reducir su tamaño/acoplamiento sin cambiar comportamiento observable.
     * El store mantiene una fachada pública que delega aquí.
     */
    private final ArchivoReingresoSupport archivoReingreso =
            new ArchivoReingresoSupport(actas, eventosPorActa, accionPendientePorActa, documentosPorActa);

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
            new CerrabilidadSupport(actas, eventosPorActa, documentosPorActa, situacionPagoCondenaPorActa);

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
            new GestionExternaSupport(
                    actas,
                    eventosPorActa,
                    accionPendientePorActa,
                    cerrabilidad,
                    situacionPagoCondenaPorActa,
                    situacionPagoPorActa,
                    tipoPagoPorActa,
                    montoCondenaPorActa,
                    resultadoExternoPostGestionPorActa,
                    montoCondenaSugeridoPostGestionExternaPorActa);

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
            new PagoVoluntarioSupport(
                    actas,
                    eventosPorActa,
                    accionPendientePorActa,
                    situacionPagoPorActa,
                    tipoPagoPorActa,
                    montoPagoVoluntarioPorActa);

    /**
     * Soporte funcional del circuito mínimo de pago informado + comprobante +
     * confirmación/observación mock, separado del registro de solicitud.
     */
    private final PagoInformadoSupport pagoInformado =
            new PagoInformadoSupport(actas, eventosPorActa, accionPendientePorActa, situacionPagoPorActa, pagoInformadoPorActa, montoPagoVoluntarioPorActa);

    private final PagoCondenaSupport pagoCondena =
            new PagoCondenaSupport(
                    actas,
                    eventosPorActa,
                    situacionPagoCondenaPorActa,
                    situacionPagoPorActa,
                    tipoPagoPorActa,
                    montoCondenaPorActa,
                    cerrabilidad);

    private final CierreSupport cierre =
            new CierreSupport(actas, eventosPorActa, accionPendientePorActa, cerrabilidad);

    /**
     * Circuito jurídico mínimo de fallo y plazo de apelación. Encapsula:
     * <ul>
     *   <li>dictado de fallo absolutorio / condenatorio en análisis (genera
     *       documento {@code FALLO_*} en {@code PENDIENTE_FIRMA} y mueve la
     *       acta a la bandeja {@code PENDIENTE_FIRMA}; no cambia
     *       {@code resultadoFinal});</li>
     *   <li>detección de "fallo pendiente de notificación" para que el
     *       endpoint genérico {@code registrar-notificacion-positiva} se
     *       interprete como notificación de fallo cuando corresponda;</li>
     *   <li>materialización de la notificación de fallo (fija
     *       {@code resultadoFinal} {@code ABSUELTO} o {@code CONDENADO},
     *       abre plazo de apelación en el caso condenatorio);</li>
     *   <li>registro mock del vencimiento del plazo de apelación
     *       ({@link ResultadoFinalCierreMock#CONDENA_FIRME}).</li>
     * </ul>
     * Reglas funcionales explícitas en {@link FalloPlazoApelacionSupport}.
     */
    private final FalloPlazoApelacionSupport falloPlazoApelacion =
            new FalloPlazoApelacionSupport(
                    actas,
                    eventosPorActa,
                    documentosPorActa,
                    notificacionesPorActa,
                    accionPendientePorActa,
                    cerrabilidad,
                    montoCondenaPorActa,
                    situacionPagoCondenaPorActa,
                    situacionPagoPorActa,
                    tipoPagoPorActa,
                    resultadoExternoPostGestionPorActa,
                    montoCondenaSugeridoPostGestionExternaPorActa);

    private final CorreoPostalNotificacionSupport correoPostal =
            new CorreoPostalNotificacionSupport(
                    actas,
                    eventosPorActa,
                    notificacionesPorActa,
                    accionPendientePorActa,
                    this::registrarNotificacionPositiva);

    /**
     * Soporte funcional del área paralización/reactivación: paralización
     * administrativa transversal, reactivación explícita desde PARALIZADAS,
     * predicado de elegibilidad y eventos asociados. Extraído del store para
     * reducir su tamaño/acoplamiento sin cambiar comportamiento observable.
     * El store mantiene una fachada pública que delega aquí.
     */
    private final ParalizacionReactivacionSupport paralizacionReactivacion =
            new ParalizacionReactivacionSupport(
                    actas,
                    eventosPorActa,
                    accionPendientePorActa,
                    observacionParalizacionPorActa);

    private final SubBandejaClasificador subBandejaClasificador = new SubBandejaClasificador();

    private final NotificadorMunicipalSupport notificadorMunicipal =
            new NotificadorMunicipalSupport(
                    actas,
                    eventosPorActa,
                    notificacionesPorActa,
                    accionPendientePorActa,
                    this::registrarNotificacionPositiva);

    /**
     * Soporte funcional del área portal infractor: predicado de revisión,
     * búsqueda de notificación pendiente, listado de documentos visibles,
     * visualización de documento notificable y confirmación de notificación
     * por portal. Extraído del store para reducir su tamaño/acoplamiento sin
     * cambiar comportamiento observable. El store mantiene una fachada pública
     * que delega aquí.
     */
    private final PortalInfractorSupport portalInfractor =
            new PortalInfractorSupport(
                    actas,
                    eventosPorActa,
                    notificacionesPorActa,
                    accionPendientePorActa,
                    piezasFirma,
                    falloPlazoApelacion);

    /**
     * Soporte funcional del área consulta/listado de bandejas: resumen con
     * conteos, sub-bandejas dinámicas, clasificación de sub-bandeja y listado
     * filtrable de actas por bandeja. Extraído del store para reducir su
     * tamaño/acoplamiento sin cambiar comportamiento observable. El store
     * mantiene una fachada pública que delega aquí.
     */
    private final BandejaConsultaSupport bandejaConsulta =
            new BandejaConsultaSupport(
                    actas,
                    accionPendientePorActa,
                    situacionPagoPorActa,
                    situacionPagoCondenaPorActa,
                    piezasRequeridasPorActa,
                    notificacionesPorActa,
                    cerrabilidad,
                    archivoReingreso,
                    gestionExterna,
                    piezasFirma,
                    subBandejaClasificador,
                    ORDEN_BANDEJAS_DEMO);

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


    /** Bootstrap {@link MockDataFactory}: dependencia visible en detalle demo. */
    void registrarDependenciaDemo(String actaId, DependenciaActaDemo dependencia) {
        dependenciaDemoPorActa.put(actaId, dependencia.name());
    }

    public Optional<String> getDependenciaDemo(String actaId) {
        return Optional.ofNullable(dependenciaDemoPorActa.get(actaId));
    }

    public Optional<String> getTipoActaAltaDemo(String actaId) {
        return Optional.ofNullable(tipoActaDemoPorActa.get(actaId));
    }

    void registrarPatenteVehiculo(String actaId, String patente) {
        patenteVehiculoPorActa.put(actaId, patente);
    }

    public Optional<String> getPatenteVehiculo(String actaId) {
        return Optional.ofNullable(patenteVehiculoPorActa.get(actaId));
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
        observacionParalizacionPorActa.clear();
        situacionPagoPorActa.clear();
        pagoInformadoPorActa.clear();
        montoPagoVoluntarioPorActa.clear();
        montoCondenaPorActa.clear();
        resultadoExternoPostGestionPorActa.clear();
        montoCondenaSugeridoPostGestionExternaPorActa.clear();
        situacionPagoCondenaPorActa.clear();
        tipoPagoPorActa.clear();
        actaTransitoMockPorActa.clear();
        actaBromatologiaMockPorActa.clear();
        dependenciaDemoPorActa.clear();
        tipoActaDemoPorActa.clear();
        patenteVehiculoPorActa.clear();
        contadorActaLabradoMockDemo.set(0);
        cerrabilidad.clear();
        archivoReingreso.clear();
        gestionExterna.clear();
        falloPlazoApelacion.clear();
        correoPostal.reiniciarDemo();
    }

    /**
     * Marca operativa vigente de la acta dentro de su bandeja, o {@code null}
     * si la acta no tiene acci��n pendiente marcada.
     */
    public String getAccionPendiente(String actaId) {
        return accionPendientePorActa.get(actaId);
    }

    /**
     * Observaci&oacute;n de paralizaci&oacute;n registrada al momento de paralizar el
     * acta. {@code null} si no se ingres&oacute; o si el acta no est&aacute; paralizada.
     */
    public String getObservacionParalizacion(String actaId) {
        return observacionParalizacionPorActa.get(actaId);
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

    public ResultadoExternoPostGestion getResultadoExternoPostGestion(String actaId) {
        return resultadoExternoPostGestionPorActa.get(actaId);
    }

    public BigDecimal getMontoCondenaSugeridoPostGestionExterna(String actaId) {
        return montoCondenaSugeridoPostGestionExternaPorActa.get(actaId);
    }

    public boolean hayResultadoExternoPostGestionPendiente(String actaId) {
        return resultadoExternoPostGestionPorActa.containsKey(actaId);
    }

    /**
     * Situación de pago mock visible por API. Si no hay valor cargado para la
     * acta, se interpreta como {@link SituacionPagoMock#SIN_PAGO}.
     */
    public SituacionPagoMock getSituacionPago(String actaId) {
        SituacionPagoMock v = situacionPagoPorActa.get(actaId);
        return v != null ? v : SituacionPagoMock.SIN_PAGO;
    }

    public SituacionPagoCondena getSituacionPagoCondena(String actaId) {
        if (cerrabilidad.getResultadoFinal(actaId) != ResultadoFinalCierreMock.CONDENA_FIRME) {
            return SituacionPagoCondena.NO_APLICA;
        }
        return situacionPagoCondenaPorActa.getOrDefault(actaId, SituacionPagoCondena.PENDIENTE);
    }

    public TipoPago getTipoPago(String actaId) {
        TipoPago v = tipoPagoPorActa.get(actaId);
        return v != null ? v : TipoPago.NO_APLICA;
    }

    /**
     * Hecho informado por el administrado (mock): pago informado y/o
     * comprobante adjunto. Puede ser {@code null} si no hay pago informado.
     */
    public PagoInformadoMock getPagoInformado(String actaId) {
        return pagoInformadoPorActa.get(actaId);
    }

    /**
     * Acciones del flujo de pago voluntario realmente disponibles para la
     * acta, en el mismo orden en que la UI suele ofrecerlas. Refleja
     * exactamente las precondiciones que aplican los handlers asociados;
     * intentar invocar una acción que no figure en esta lista termina en
     * 409.
     *
     * <p>Precondiciones reflejadas (alineadas con
     * {@link PagoVoluntarioSupport} y {@link PagoInformadoSupport}):
     * <ul>
     *   <li>{@code SOLICITAR}: situación {@code SIN_PAGO} y acta en
     *       bandeja interna operable (helper
     *       {@link PrototipoConstantes#bandejaPermitePagoVoluntario}).
     *       Decisión funcional: el infractor siempre puede pagar mientras
     *       el expediente esté en una etapa interna operable; el origen
     *       no se restringe a {@code ACTAS_EN_ENRIQUECIMIENTO}.</li>
     *   <li>{@code INFORMAR}: situación {@code SOLICITADO} u
     *       {@code OBSERVADO} (administrado puede (re)informar pago).</li>
     *   <li>{@code ADJUNTAR_COMPROBANTE}: situación
     *       {@code PENDIENTE_CONFIRMACION} sin comprobante adjunto aún.</li>
     *   <li>{@code CONFIRMAR} y {@code OBSERVAR}: situación
     *       {@code PENDIENTE_CONFIRMACION} (Dirección puede confirmar u observar
     *       en cualquier subestado de esa situación).</li>
     * </ul>
     *
     * <p>Lista vacía si el acta no existe, está cerrada, o se encuentra en
     * una bandeja terminal/externa ({@code ARCHIVO}, {@code CERRADAS},
     * {@code GESTION_EXTERNA}): sobre esos estados no aplica ninguna
     * acción del flujo de pago voluntario.
     */
    public List<String> getAccionesPagoVoluntarioDisponibles(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null) {
            return List.of();
        }
        if (!bandejaPermitePagoVoluntario(acta.estaCerrada(), acta.bandejaActual())) {
            return List.of();
        }
        ResultadoFinalCierreMock rf = cerrabilidad.getResultadoFinal(actaId);
        if (rf != ResultadoFinalCierreMock.SIN_RESULTADO_FINAL) {
            return List.of();
        }
        SituacionPagoMock situacion = getSituacionPago(actaId);
        List<String> acciones = new ArrayList<>();
        switch (situacion) {
            case SIN_PAGO -> acciones.add("SOLICITAR");
            case SOLICITADO, OBSERVADO -> {
                BigDecimal monto = getMontoPagoVoluntario(actaId);
                if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
                    // Monto fijado: el infractor puede informar pago.
                    acciones.add("INFORMAR");
                } else {
                    // Monto no fijado: Dirección debe evaluarlo y fijarlo.
                    acciones.add("FIJAR_MONTO");
                }
            }
            case PENDIENTE_CONFIRMACION -> {
                PagoInformadoMock pago = pagoInformadoPorActa.get(actaId);
                if (pago != null && pago.comprobanteId() == null) {
                    acciones.add("ADJUNTAR_COMPROBANTE");
                }
                acciones.add("CONFIRMAR");
                acciones.add("OBSERVAR");
            }
            case CONFIRMADO, VENCIDO -> {
                // Terminal de pago: sin nuevas acciones de pago voluntario.
            }
        }
        return acciones;
    }

    public List<String> getAccionesPagoCondenaDisponibles(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null || acta.estaCerrada() || !BANDEJA_PENDIENTE_ANALISIS.equals(acta.bandejaActual())) {
            return List.of();
        }
        if (ACCION_DICTAR_FALLO_POST_GESTION_EXTERNA.equals(getAccionPendiente(actaId))) {
            return List.of();
        }
        if (cerrabilidad.getResultadoFinal(actaId) != ResultadoFinalCierreMock.CONDENA_FIRME) {
            return List.of();
        }
        SituacionPagoCondena situacion = getSituacionPagoCondena(actaId);
        return switch (situacion) {
            case PENDIENTE, OBSERVADO -> List.of("INFORMAR");
            case INFORMADO -> List.of("CONFIRMAR", "OBSERVAR");
            case CONFIRMADO, NO_APLICA -> List.of();
        };
    }

    public List<String> getAccionesGestionExternaDisponibles(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null || acta.estaCerrada() || !BANDEJA_PENDIENTE_ANALISIS.equals(acta.bandejaActual())) {
            return List.of();
        }
        if (ACCION_DICTAR_FALLO_POST_GESTION_EXTERNA.equals(getAccionPendiente(actaId))) {
            return List.of();
        }
        SituacionPagoCondena situacion = getSituacionPagoCondena(actaId);
        // Ninguna marca habilita gestión externa cuando el pago ya fue informado o confirmado.
        if (situacion == SituacionPagoCondena.INFORMADO
                || situacion == SituacionPagoCondena.CONFIRMADO) {
            return List.of();
        }
        boolean condenaFirmeDerivable =
                cerrabilidad.getResultadoFinal(actaId) == ResultadoFinalCierreMock.CONDENA_FIRME
                        && (situacion == SituacionPagoCondena.PENDIENTE
                                || situacion == SituacionPagoCondena.OBSERVADO);
        boolean marcaDerivable = ACCION_DERIVAR_GESTION_EXTERNA.equals(getAccionPendiente(actaId))
                || ACCION_REVISION_POST_GESTION_EXTERNA.equals(getAccionPendiente(actaId));
        if (!condenaFirmeDerivable && !marcaDerivable) {
            return List.of();
        }
        if (getTipoGestionExterna(actaId) != null) {
            return List.of();
        }
        return List.of(TIPO_GESTION_EXTERNA_APREMIO, TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ);
    }

    public CerrabilidadActaVista getCerrabilidadActa(String actaId) {
        ActaMock a = actas.get(actaId);
        return cerrabilidad.getVistaCerrabilidad(a);
    }

    /**
     * {@code true} si el acta puede cerrarse ahora desde análisis: bandeja
     * PENDIENTE_ANALISIS, resultado compatible y sin pendientes bloqueantes.
     */
    public boolean puedeCerrarDesdeAnalisis(String actaId) {
        return cerrabilidad.puedeCerrarDesdeAnalisis(actaId);
    }

    /**
     * {@code true} si al menos un bloqueante pendiente no tiene aún documento
     * resolutorio generado. Indica que la acción "Generar resolución" está disponible.
     */
    public boolean hayPendientesSinResolutorio(String actaId) {
        return cerrabilidad.hayPendientesSinResolutorio(actaId);
    }

    /**
     * {@code true} si al menos un bloqueante pendiente tiene resolutorio FIRMADO
     * pero el cumplimiento material no fue registrado. Indica que la acción
     * "Registrar cumplimiento" está disponible.
     */
    public boolean hayPendientesConResolutorioFirmado(String actaId) {
        return cerrabilidad.hayPendientesConResolutorioFirmado(actaId);
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
     * Helper interno de mocks: asigna el tipo de pago (circuito de origen) de
     * una acta precargada sin ejecutar el flujo operativo. Uso restringido a la
     * fábrica de datos demo para representar estados donde ya se conoce el
     * circuito (VOLUNTARIO / CONDENA) sin haber transitado por los supports.
     */
    public void setTipoPago(String actaId, TipoPago tipoPago) {
        if (actaId == null) {
            return;
        }
        if (tipoPago == null || tipoPago == TipoPago.NO_APLICA) {
            tipoPagoPorActa.remove(actaId);
            return;
        }
        tipoPagoPorActa.put(actaId, tipoPago);
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
     * Helper interno de mocks: fija el monto de pago voluntario para un acta
     * precargada sin ejecutar el flujo operativo de solicitud. Uso restringido
     * a la fábrica de datos demo para representar estados como CONFIRMADO con
     * monto ya fijado desde precarga.
     */
    public void setMontoPagoVoluntario(String actaId, BigDecimal monto) {
        if (actaId == null) {
            return;
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            montoPagoVoluntarioPorActa.remove(actaId);
            return;
        }
        montoPagoVoluntarioPorActa.put(actaId, monto);
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

    /**
     * Helper interno de mocks: precarga circuito de apelación sin recorrer API.
     */
    public void precargarApelacionDemo(
            String actaId,
            CanalPresentacionApelacionMock canal,
            boolean resuelta,
            ResultadoResolucionApelacionMock resultadoResolucion) {
        falloPlazoApelacion.precargarApelacionDemo(actaId, canal, resuelta, resultadoResolucion);
    }

    /**
     * Helper interno de mocks: monto de condena asociado a fallo condenatorio.
     */
    public void setMontoCondenaDemo(String actaId, java.math.BigDecimal monto) {
        if (actaId == null || monto == null) {
            return;
        }
        montoCondenaPorActa.put(actaId, monto);
    }

    /**
     * Helper interno de mocks: situación de pago de condena precargada (demo).
     * Solo usar en la fábrica de datos; nunca en código de circuito operativo.
     */
    public void setSituacionPagoCondenaDemo(String actaId, SituacionPagoCondena situacion) {
        if (actaId == null || situacion == null) {
            return;
        }
        situacionPagoCondenaPorActa.put(actaId, situacion);
    }

    /**
     * Fachada pública; la lógica vive en {@link BandejaConsultaSupport}.
     */
    public List<BandejaConteo> listarBandejasConConteoOrdenadas() {
        return bandejaConsulta.listarBandejasConConteoOrdenadas();
    }

    /**
     * Resumen de bandejas con conteo total y sub-bandejas dinámicas (solo
     * cantidad &gt; 0). Fachada pública; la lógica vive en
     * {@link BandejaConsultaSupport}.
     */
    public List<BandejaResumenOperativo> listarBandejasConResumenOperativo() {
        return bandejaConsulta.listarBandejasConResumenOperativo();
    }

    /**
     * Fachada pública; la lógica vive en {@link BandejaConsultaSupport}.
     */
    public SubBandejaContexto construirContextoSubBandeja(String actaId) {
        return bandejaConsulta.construirContextoSubBandeja(actaId);
    }

    /**
     * Fachada pública; la lógica vive en {@link BandejaConsultaSupport}.
     */
    public SubBandejaAsignacion clasificarSubBandeja(String actaId) {
        return bandejaConsulta.clasificarSubBandeja(actaId);
    }

    /**
     * Fachada pública; la lógica vive en {@link BandejaConsultaSupport}.
     */
    public boolean esSubBandejaValidaParaBandeja(String codigoBandeja, String subBandeja) {
        return bandejaConsulta.esSubBandejaValidaParaBandeja(codigoBandeja, subBandeja);
    }

    /**
     * Fachada pública; la lógica vive en {@link BandejaConsultaSupport}.
     */
    public List<ActaMock> listarActasPorBandeja(String codigoBandeja) {
        return bandejaConsulta.listarActasPorBandeja(codigoBandeja);
    }

    /**
     * Listado de la bandeja con filtro opcional por acción pendiente. Si
     * {@code accionPendiente} es {@code null} o en blanco, no se filtra.
     * Fachada pública; la lógica vive en {@link BandejaConsultaSupport}.
     */
    public List<ActaMock> listarActasPorBandeja(String codigoBandeja, String accionPendiente) {
        return bandejaConsulta.listarActasPorBandeja(codigoBandeja, accionPendiente);
    }

    /**
     * Listado de la bandeja con filtros opcionales por acción pendiente y por
     * situación de pago mock. Si un filtro es {@code null} o en blanco, no se
     * aplica. Fachada pública; la lógica vive en {@link BandejaConsultaSupport}.
     */
    public List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            SituacionPagoMock situacionPago) {
        return bandejaConsulta.listarActasPorBandeja(codigoBandeja, accionPendiente, situacionPago);
    }

    /**
     * Filtros opcionales de cerrabilidad (además de bandeja, acción y pago). Si
     * un filtro es {@code null}, no aplica. Fachada pública; la lógica vive en
     * {@link BandejaConsultaSupport}.
     */
    public List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            SituacionPagoMock situacionPago,
            ResultadoFinalCierreMock filtroResultadoFinal,
            Boolean filtroCerrable,
            PendienteBloqueanteCierreMock filtroPendienteBloqueante) {
        return bandejaConsulta.listarActasPorBandeja(
                codigoBandeja, accionPendiente, situacionPago,
                filtroResultadoFinal, filtroCerrable, filtroPendienteBloqueante);
    }

    /**
     * Listado de bandeja con filtros existentes y filtro opcional por
     * sub-bandeja operativa. Si {@code subBandeja} no es válida para la
     * bandeja, devuelve lista vacía. Fachada pública; la lógica vive en
     * {@link BandejaConsultaSupport}.
     */
    public List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            SituacionPagoMock situacionPago,
            ResultadoFinalCierreMock filtroResultadoFinal,
            Boolean filtroCerrable,
            PendienteBloqueanteCierreMock filtroPendienteBloqueante,
            String subBandeja) {
        return bandejaConsulta.listarActasPorBandeja(
                codigoBandeja, accionPendiente, situacionPago,
                filtroResultadoFinal, filtroCerrable, filtroPendienteBloqueante, subBandeja);
    }

    public Optional<ActaMock> findActa(String id) {
        return Optional.ofNullable(actas.get(id));
    }

    public boolean existeActa(String id) {
        return actas.containsKey(id);
    }

    /**
     * Código ciudadano/QR estable derivado del identificador interno del acta.
     *
     * <p>Es determinístico para demo, pero se expone con un formato opaco
     * ({@code QR-<actaId>-DEMO}) que el futuro portal del infractor podrá
     * usar como dato a imprimir en el QR o entregar al infractor, sin
     * exponer el {@code actaId} interno como input público. Devuelve
     * {@code null} si {@code actaId} es nulo o vacío.
     */
    public String codigoQrDeActa(String actaId) {
        if (actaId == null || actaId.isBlank()) {
            return null;
        }
        return "QR-" + actaId + "-DEMO";
    }

    /**
     * Búsqueda inversa por código ciudadano/QR: dado el código opaco emitido
     * por {@link #codigoQrDeActa(String)}, recupera el acta asociada si
     * existe. El portal del infractor consulta acá usando el código, sin
     * conocer el {@code actaId} interno. Si el código no coincide con
     * ningún acta cargada, devuelve {@link Optional#empty()}.
     */
    public Optional<ActaMock> findActaPorCodigoQr(String codigoQr) {
        if (codigoQr == null || codigoQr.isBlank()) {
            return Optional.empty();
        }
        for (ActaMock a : actas.values()) {
            if (codigoQr.equals(codigoQrDeActa(a.id()))) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
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
     * Fachada pública; la lógica vive en {@link PortalInfractorSupport}.
     */
    public Optional<ActaNotificacionMock> findNotificacionPortalPendiente(String actaId) {
        return portalInfractor.findNotificacionPortalPendiente(actaId);
    }

    /**
     * Fachada pública; la lógica vive en {@link PortalInfractorSupport}.
     */
    public ConfirmarVisualizacionNotificacionPortalResultado confirmarVisualizacionNotificacionPortal(
            String actaId) {
        return portalInfractor.confirmarVisualizacionNotificacionPortal(actaId);
    }

    /**
     * Regla de portal: acta ingresada pero todavía no validada como
     * notificable (D1/D2, bandeja {@code ACTAS_EN_ENRIQUECIMIENTO}). En ese
     * estado el portal solo reconoce el número de acta y muestra el mensaje
     * de revisión; no expone detalle, documentos ni acciones operativas.
     * Fachada pública; la lógica vive en {@link PortalInfractorSupport}.
     */
    public boolean actaEnRevisionParaPortal(String actaId) {
        return portalInfractor.actaEnRevisionParaPortal(actaId);
    }

    /**
     * Fachada pública de {@link FalloPlazoApelacionSupport}: {@code true} si
     * hay un documento de fallo firmado todavía sin notificar. Mientras eso
     * ocurre, el portal no debe ofrecer pago voluntario ni pago de condena.
     */
    public boolean hayFalloPendienteDeNotificacion(String actaId) {
        return falloPlazoApelacion.hayFalloPendienteDeNotificacion(actaId);
    }

    /**
     * {@code true} si la acta tiene específicamente un documento
     * {@code FALLO_CONDENATORIO} firmado y sin notificación positiva
     * registrada. Usado por la UI para habilitar la acción compuesta
     * {@code consentir-condena-y-registrar-pago} cuando el operador atiende
     * presencialmente al infractor antes de que se haya formalizado la
     * notificación.
     */
    public boolean hayFalloCondenatorioPendienteDeNotificacion(String actaId) {
        return falloPlazoApelacion.hayFalloCondenatorioPendienteDeNotificacion(actaId);
    }

    /**
     * {@code true} si existe un documento {@code FALLO_CONDENATORIO} en
     * estado {@code PENDIENTE_FIRMA} o {@code FIRMADO}: indica que ya se
     * dictó una resolución condenatoria, aunque todavía no esté firmada ni
     * notificada. En cualquiera de esos sub-estados el portal no debe
     * permitir solicitar ni pagar voluntariamente.
     */
    public boolean hayFalloCondenatorioDictado(String actaId) {
        return piezasFirma.listarDocumentosPorActa(actaId).stream()
                .anyMatch(d -> PrototipoConstantes.TIPO_DOC_FALLO_CONDENATORIO.equals(d.tipoDocumento())
                        && (PrototipoConstantes.ESTADO_DOC_PENDIENTE_FIRMA.equals(d.estadoDocumento())
                                || PrototipoConstantes.ESTADO_DOC_FIRMADO.equals(d.estadoDocumento())));
    }

    /**
     * {@code true} si existe cualquier fallo de fondo dictado, pendiente de firma
     * o ya firmado. Se usa para impedir volver a abrir decisiones de pago
     * voluntario una vez iniciado el circuito resolutorio.
     */
    public boolean hayFalloDictado(String actaId) {
        return piezasFirma.listarDocumentosPorActa(actaId).stream()
                .anyMatch(d -> PrototipoConstantes.esFallo(d.tipoDocumento())
                        && (PrototipoConstantes.ESTADO_DOC_PENDIENTE_FIRMA.equals(d.estadoDocumento())
                                || PrototipoConstantes.ESTADO_DOC_FIRMADO.equals(d.estadoDocumento())));
    }

    /**
     * Documentos del expediente formalmente visibles para el infractor.
     * Fachada pública; la lógica vive en {@link PortalInfractorSupport}.
     */
    public List<DocumentoPortalVista> listarDocumentosVisiblesPortal(String actaId) {
        return portalInfractor.listarDocumentosVisiblesPortal(actaId);
    }

    /**
     * Apertura/visualización de un documento notificable desde el portal.
     * Idempotente: si el documento ya estaba notificado devuelve
     * {@code OK_YA_NOTIFICADO} sin efectos. Fachada pública; la lógica vive
     * en {@link PortalInfractorSupport}.
     */
    public VerDocumentoPortalResultado verDocumentoPortal(String actaId, String tipoDocumento) {
        return portalInfractor.verDocumentoPortal(actaId, tipoDocumento);
    }

    public List<NotificacionMunicipalVista> listarNotificacionesNotificadorMunicipal() {
        return notificadorMunicipal.listarPendientes();
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
     * desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION). Fachada pública.
     *
     * <p>Si la acta tiene un documento de fallo firmado pendiente de
     * notificación (caso post {@code dictar-fallo-*} + firma), la
     * notificación positiva se interpreta como notificación de fallo y se
     * delega a {@link FalloPlazoApelacionSupport}. En ese caso se ajusta
     * {@code resultadoFinal} ({@code ABSUELTO} o {@code CONDENADO}) y, para
     * fallo condenatorio, se abre el plazo de apelación. En el resto de los
     * casos delega en {@link NotificacionSupport} (notificación del acta).
     */
    public RegistrarNotificacionPositivaResultado registrarNotificacionPositiva(String actaId) {
        if (falloPlazoApelacion.hayFalloPendienteDeNotificacion(actaId)) {
            return falloPlazoApelacion.registrarNotificacionPositivaDeFallo(actaId);
        }
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

    public RegistrarAcuseNotificadorMunicipalResultado registrarAcuseNotificadorMunicipal(
            String notificacionId,
            ResultadoNotificacion resultado,
            String observacion) {
        return notificadorMunicipal.registrarAcuse(notificacionId, resultado, observacion);
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
        ResultadoFinalCierreMock rf = cerrabilidad.getResultadoFinal(actaId);
        if (rf == ResultadoFinalCierreMock.ABSUELTO
                || rf == ResultadoFinalCierreMock.CONDENADO
                || rf == ResultadoFinalCierreMock.CONDENA_FIRME) {
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

    public PagoCondenaResultado informarPagoCondena(String actaId) {
        return pagoCondena.informarPagoCondena(actaId);
    }

    public PagoCondenaResultado informarPagoCondenaDesdePortal(String actaId) {
        return pagoCondena.informarPagoCondenaDesdePortal(actaId);
    }

    public PagoCondenaResultado confirmarPagoCondena(String actaId) {
        PagoCondenaResultado r = pagoCondena.confirmarPagoCondena(actaId);
        if (r.estado() == PagoCondenaEstado.OK
                && ACCION_REVISION_POST_GESTION_EXTERNA.equals(accionPendientePorActa.get(actaId))) {
            accionPendientePorActa.remove(actaId);
        }
        return r;
    }

    public PagoCondenaResultado observarPagoCondena(String actaId) {
        return pagoCondena.observarPagoCondena(actaId);
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
     * APREMIO: retorno administrativo sin pago. El acta vuelve a
     * {@code PENDIENTE_ANALISIS} con {@link #ACCION_REVISION_POST_GESTION_EXTERNA},
     * condena firme pendiente de pago, y {@code tipoGestionExterna} limpiado.
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public ReingresarDesdeApremioSinPagoResultado reingresarDesdeApremioSinPago(String actaId) {
        return gestionExterna.reingresarDesdeApremioSinPago(actaId);
    }

    /**
     * APREMIO: registra pago efectuado en el proceso de apremio. Confirma la
     * situación de pago de condena, limpia {@code tipoGestionExterna} y deja
     * el acta en {@code PENDIENTE_ANALISIS} con cerrabilidad habilitada.
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public RegistrarPagoEnApremioResultado registrarPagoEnApremio(String actaId) {
        return gestionExterna.registrarPagoEnApremio(actaId);
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial absolutoria. Establece
     * {@code resultadoFinal = ABSUELTO}, limpia {@code tipoGestionExterna} y
     * deja el acta cerrable desde análisis. Fachada pública; la lógica vive
     * en {@link GestionExternaSupport}.
     */
    public ReingresarDesdeJuzgadoResultado reingresarDesdeJuzgadoAbsuelto(String actaId) {
        return gestionExterna.reingresarDesdeJuzgadoAbsuelto(actaId);
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que confirma la condena
     * original. Mantiene {@code resultadoFinal = CONDENA_FIRME}, limpia
     * {@code tipoGestionExterna} y deja el acta con pago de condena pendiente.
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public ReingresarDesdeJuzgadoResultado reingresarDesdeJuzgadoCondenaConfirmada(String actaId) {
        return gestionExterna.reingresarDesdeJuzgadoCondenaConfirmada(actaId);
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que modifica el monto de
     * condena. Actualiza {@code montoCondena}, mantiene
     * {@code resultadoFinal = CONDENA_FIRME}, limpia {@code tipoGestionExterna}
     * y deja el acta con pago de condena pendiente por el nuevo monto.
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public ReingresarDesdeJuzgadoResultado reingresarDesdeJuzgadoMontoModificado(
            String actaId, java.math.BigDecimal nuevoMonto) {
        return gestionExterna.reingresarDesdeJuzgadoMontoModificado(actaId, nuevoMonto);
    }

    public ReingresarDesdeJuzgadoResultado reingresarDesdeApremioMontoModificado(
            String actaId, java.math.BigDecimal nuevoMonto) {
        return gestionExterna.reingresarDesdeApremioMontoModificado(actaId, nuevoMonto);
    }

    public ReingresarDesdeJuzgadoResultado reingresarDesdeApremioAbsuelto(String actaId) {
        return gestionExterna.reingresarDesdeApremioAbsuelto(actaId);
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
     *
     * <p>Regla de coexistencia con cierre: si la acta ya cumple condición de
     * cierre operativo desde análisis (resultado final compatible y sin
     * pendientes bloqueantes), no debe poder archivarse. Archivo es la salida
     * para casos que no pueden o no deben cerrarse todavía; si el expediente
     * está listo para cierre, corresponde cerrar explícitamente y no
     * archivar. Se rechaza con {@link ArchivarActaEstado#CONFLICT} para que
     * el controller responda 409 (no se delega al soporte).
     */
    public ArchivarActaResultado archivarActaDesdeAnalisis(String actaId) {
        if (cerrabilidad.puedeCerrarDesdeAnalisis(actaId)) {
            return new ArchivarActaResultado(
                    ArchivarActaEstado.CONFLICT, null, null, null, null);
        }
        ResultadoFinalCierreMock rf = cerrabilidad.getResultadoFinal(actaId);
        if (rf == ResultadoFinalCierreMock.CONDENADO || rf == ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new ArchivarActaResultado(
                    ArchivarActaEstado.CONFLICT, null, null, null, null);
        }
        // Bloquear archivo mientras el pago voluntario está en proceso de acreditación.
        SituacionPagoMock sp = getSituacionPago(actaId);
        if (sp == SituacionPagoMock.PENDIENTE_CONFIRMACION) {
            return new ArchivarActaResultado(
                    ArchivarActaEstado.CONFLICT, null, null, null, null);
        }
        return archivoReingreso.archivarActaDesdeAnalisis(actaId);
    }

    /**
     * Informa pago voluntario desde el portal del infractor. Requiere que
     * Direccion haya fijado el monto (situacionPago=SOLICITADO u OBSERVADO,
     * monto > 0). Mueve el acta a PENDIENTE_CONFIRMACION y genera tarea
     * operativa VERIFICAR_PAGO_INFORMADO para Direccion de Faltas.
     */
    public InformarPagoVoluntarioDesdePortalResultado informarPagoVoluntarioDesdePortal(String actaId) {
        if (hayFalloCondenatorioDictado(actaId)) {
            return new InformarPagoVoluntarioDesdePortalResultado(
                    InformarPagoVoluntarioDesdePortalEstado.CONFLICT_FALLO_DICTADO, actaId, getSituacionPago(actaId));
        }
        ResultadoFinalCierreMock rf = cerrabilidad.getResultadoFinal(actaId);
        if (rf != ResultadoFinalCierreMock.SIN_RESULTADO_FINAL) {
            return new InformarPagoVoluntarioDesdePortalResultado(
                    InformarPagoVoluntarioDesdePortalEstado.CONFLICT, actaId, getSituacionPago(actaId));
        }
        SituacionPagoCondena spc = situacionPagoCondenaPorActa.getOrDefault(actaId, SituacionPagoCondena.NO_APLICA);
        if (spc != SituacionPagoCondena.NO_APLICA) {
            return new InformarPagoVoluntarioDesdePortalResultado(
                    InformarPagoVoluntarioDesdePortalEstado.CONFLICT, actaId, getSituacionPago(actaId));
        }
        return pagoVoluntario.informarPagoVoluntarioDesdePortal(actaId);
    }

    /**
     * Confirma pago voluntario desde sistema externo de cobro. Requiere pago
     * previo informado/pendiente de confirmacion y monto coincidente. Genera
     * evento PAGO_VOLUNTARIO_CONFIRMADO_EXTERNO y fija resultadoFinal=PAGO_CONFIRMADO.
     */
    public ConfirmarPagoVoluntarioExternoResultado confirmarPagoVoluntarioExterno(
            String actaId,
            java.math.BigDecimal monto,
            String origen) {
        ResultadoFinalCierreMock rf = cerrabilidad.getResultadoFinal(actaId);
        if (rf == ResultadoFinalCierreMock.ABSUELTO
                || rf == ResultadoFinalCierreMock.CONDENADO
                || rf == ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new ConfirmarPagoVoluntarioExternoResultado(
                    ConfirmarPagoVoluntarioExternoEstado.CONFLICT, actaId, getSituacionPago(actaId));
        }
        ConfirmarPagoVoluntarioExternoResultado r = pagoInformado.confirmarPagoVoluntarioExterno(actaId, monto, origen);
        if (r.estado() == ConfirmarPagoVoluntarioExternoEstado.OK) {
            cerrabilidad.setResultadoFinalDemo(actaId, ResultadoFinalCierreMock.PAGO_CONFIRMADO);
        }
        return r;
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
     * Demo: reingreso explícito desde la macro-bandeja ARCHIVO; vuelve a
     * PENDIENTE_ANALISIS con {@code motivoArchivo} previo preservado. La
     * acción pendiente se infiere del estado documental y de los eventos
     * previos al archivo: si había un fallo condenatorio firmado cuya
     * notificación falló o venció, la marca resultante es
     * {@link #ACCION_REINTENTAR_NOTIFICACION} o
     * {@link #ACCION_EVALUAR_NOTIFICACION_VENCIDA} según corresponda; en
     * cualquier otro caso se usa {@link #ACCION_REVISION_POST_REINGRESO}
     * como marca informativa no bloqueante. Fachada pública; la lógica vive
     * en {@link ArchivoReingresoSupport}.
     */    public ReingresarActaResultado reingresarActaDesdeArchivo(String actaId) {
        return archivoReingreso.reingresarActaDesdeArchivo(actaId);
    }

    /**
     * Demo: envía el acta desde enriquecimiento (D2) a la bandeja de
     * notificación pendiente (D4). Solo aplica a actas en
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con situaciónAdministrativa
     * {@code ACTIVA} y estadoProceso {@code EN_CURSO}. Los bloqueantes
     * materiales pendientes no impiden esta transición. Fachada pública;
     * la lógica vive en {@link ArchivoReingresoSupport}.
     */
    public EnviarActaANotificacionResultado enviarActaANotificacion(String actaId) {
        return archivoReingreso.enviarActaANotificacion(actaId);
    }

    /**
     * Demo: anula el acta y la archiva por nulidad desde la etapa de
     * enriquecimiento. Solo aplica a actas en
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con situaciónAdministrativa
     * {@code ACTIVA}. El acta queda en ARCHIVO con
     * {@code motivoArchivo=NULIDAD} y {@code permiteReingreso=true} para
     * permitir correcciones administrativas. Fachada pública; la lógica
     * vive en {@link ArchivoReingresoSupport}.
     */
    public AnularActaPorNulidadResultado anularActaPorNulidad(String actaId) {
        return archivoReingreso.anularActaPorNulidad(actaId);
    }

    /**
     * Demo: predicado de elegibilidad para paralización administrativa.
     * Fachada pública; la lógica vive en
     * {@link ParalizacionReactivacionSupport}.
     */
    public boolean puedeParalizarActa(String actaId) {
        return paralizacionReactivacion.puedeParalizarActa(actaId);
    }

    /**
     * Demo: paralización administrativa transversal desde cualquier bandeja
     * interna operativa activa. Conserva expediente, pagos, montos y resultado
     * de fondo; sólo cambia la proyección operativa agregadora y deja el motivo
     * como acción pendiente trazable. Fachada pública; la lógica vive en
     * {@link ParalizacionReactivacionSupport}.
     */
    public ParalizarActaResultado paralizarActa(
            String actaId, MotivoParalizacionActa motivo, String observacion) {
        return paralizacionReactivacion.paralizarActa(actaId, motivo, observacion);
    }

    /**
     * Demo: reactivación explícita desde la macro-bandeja PARALIZADAS →
     * vuelve a PENDIENTE_ANALISIS con bloque D5_ANALISIS, estado
     * PENDIENTE_REVISION, situación ACTIVA y marca operativa
     * {@link #ACCION_REVISION_POST_REACTIVACION}. La información histórica
     * de la paralización queda en el log de eventos; no bloquea acciones
     * luego de reactivar. Solo aplica a actas cuya bandejaActual sea
     * {@code PARALIZADAS}. Fachada pública; la lógica vive en
     * {@link ParalizacionReactivacionSupport}.
     */
    public ReactivarActaResultado reactivarActa(String actaId) {
        return paralizacionReactivacion.reactivarActa(actaId);
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
     * Demo: produce la pieza RESOLUCION (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Fachada publica; la logica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarResolucionResultado generarResolucion(String actaId) {
        return piezasFirma.generarResolucion(actaId);
    }

    /**
     * Demo: produce la pieza RECTIFICACION (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Fachada publica; la logica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarRectificacionResultado generarRectificacion(String actaId) {
        return piezasFirma.generarRectificacion(actaId);
    }

    /**
     * Demo: dictar fallo absolutorio desde {@code PENDIENTE_ANALISIS}.
     * Genera documento mock {@code FALLO_ABSOLUTORIO} en
     * {@code PENDIENTE_FIRMA} y mueve la acta a la bandeja
     * {@code PENDIENTE_FIRMA}; no cambia {@code resultadoFinal} todavía. La
     * firma reutiliza el endpoint existente
     * {@code firmar-documento/{documentoId}}. Fachada pública; la lógica
     * vive en {@link FalloPlazoApelacionSupport}.
     */
    public DictarFalloResultado dictarFalloAbsolutorio(String actaId) {
        return falloPlazoApelacion.dictarFalloAbsolutorio(actaId);
    }

    /**
     * Demo: dictar fallo condenatorio desde {@code PENDIENTE_ANALISIS}.
     * Genera documento mock {@code FALLO_CONDENATORIO} en
     * {@code PENDIENTE_FIRMA} y mueve la acta a la bandeja
     * {@code PENDIENTE_FIRMA}; no cambia {@code resultadoFinal} todavía. La
     * apertura del plazo de apelación se materializa recién al notificarse
     * positivamente el fallo. Fachada pública; la lógica vive en
     * {@link FalloPlazoApelacionSupport}.
     */
    public DictarFalloResultado dictarFalloCondenatorio(String actaId, BigDecimal montoCondena) {
        return falloPlazoApelacion.dictarFalloCondenatorio(actaId, montoCondena);
    }

    /**
     * Demo: registra el vencimiento mock del plazo de apelación (sin
     * cálculo real de días). Solo aplica si {@code resultadoFinal} es
     * {@link ResultadoFinalCierreMock#CONDENADO} y el plazo está abierto.
     * Si vence sin apelación presentada, el resultado pasa a
     * {@link ResultadoFinalCierreMock#CONDENA_FIRME} y el portal/infractor
     * deja de habilitar la presentación de apelación. Fachada pública; la
     * lógica vive en {@link FalloPlazoApelacionSupport}.
     */
    public RegistrarVencimientoPlazoApelacionResultado registrarVencimientoPlazoApelacion(String actaId) {
        return falloPlazoApelacion.registrarVencimientoPlazoApelacion(actaId);
    }

    /**
     * Demo: registra la presentación de apelación/recurso mientras el plazo
     * está abierto. Cierra el plazo, conserva {@code resultadoFinal}
     * {@link ResultadoFinalCierreMock#CONDENADO} y no resuelve ni eleva el
     * recurso. Fachada pública; la lógica vive en
     * {@link FalloPlazoApelacionSupport}.
     */
    public RegistrarApelacionResultado registrarApelacion(
            String actaId, CanalPresentacionApelacionMock canal) {
        return falloPlazoApelacion.registrarApelacion(actaId, canal);
    }

    /**
     * Demo: resuelve mock de apelación/recurso ya presentado con plazo
     * cerrado y {@code resultadoFinal} {@link ResultadoFinalCierreMock#CONDENADO}.
     * {@link ResultadoResolucionApelacionMock#RECHAZADA} confirma la condena
     * ({@link ResultadoFinalCierreMock#CONDENA_FIRME});
     * {@link ResultadoResolucionApelacionMock#ACEPTADA_ABSUELVE} absuelve
     * ({@link ResultadoFinalCierreMock#ABSUELTO}). No cierra el acta ni
     * deriva a gestión externa. Fachada pública; la lógica vive en
     * {@link FalloPlazoApelacionSupport}.
     */
    public ResolverApelacionResultado resolverApelacion(
            String actaId, ResultadoResolucionApelacionMock resultado) {
        return falloPlazoApelacion.resolverApelacion(actaId, resultado);
    }

    /**
     * Regla de portal/infractor para
     * {@code GET /api/prototipo/infractor/actas/{codigoQr}}:
     * la acción de presentar apelación queda habilitada solo si el fallo
     * condenatorio fue notificado, el plazo está abierto, no hay apelación
     * presentada y la acta no está en bandeja terminal/externa ni cerrada.
     * Fachada pública; la lógica vive en {@link FalloPlazoApelacionSupport}.
     */
    public boolean puedePresentarApelacion(String actaId) {
        return falloPlazoApelacion.puedePresentarApelacion(actaId);
    }

    /**
     * {@code true} si el infractor ya presentó apelación/recurso para esta
     * acta (independientemente de si fue resuelto). Fachada pública; la
     * lógica vive en {@link FalloPlazoApelacionSupport}.
     */
    public boolean hayApelacionPresentada(String actaId) {
        return falloPlazoApelacion.apelacionPresentada(actaId);
    }

    /**
     * El infractor consiente la condena desde el portal, renunciando a
     * apelar. Precondición: {@code resultadoFinal=CONDENADO}, sin apelación
     * presentada, {@code situacionPagoCondena=NO_APLICA},
     * {@code montoCondena > 0}. Efecto: {@code resultadoFinal=CONDENA_FIRME},
     * {@code situacionPagoCondena=PENDIENTE}, evento {@code CONDENA_CONSENTIDA}.
     * Fachada pública; la lógica vive en {@link FalloPlazoApelacionSupport}.
     */
    public ConsentirCondenaResultado consentirCondena(String actaId) {
        return falloPlazoApelacion.consentirCondena(actaId);
    }

    /**
     * Dirección registra consentimiento presencial de condena y pago en un
     * único acto. Precondición: {@code resultadoFinal=CONDENADO}, sin
     * apelación presentada, {@code situacionPagoCondena=NO_APLICA},
     * {@code situacionPago=SIN_PAGO}, {@code montoCondena > 0}, bandeja
     * operativa interna (no ARCHIVO, CERRADAS, GESTION_EXTERNA). Efecto:
     * {@code resultadoFinal=CONDENA_FIRME},
     * {@code situacionPagoCondena=INFORMADO},
     * {@code situacionPago=PENDIENTE_CONFIRMACION},
     * {@code tipoPago=CONDENA}. Fachada pública; la lógica vive en
     * {@link FalloPlazoApelacionSupport}.
     */
    public ConsentirCondenaYRegistrarPagoResultado consentirCondenaYRegistrarPago(String actaId) {
        return falloPlazoApelacion.consentirCondenaYRegistrarPago(actaId);
    }

    public GenerarLoteCorreoResultado generarLoteCorreoPostalDemo(String tipo, List<String> notificacionIds)
            throws java.io.IOException {
        return correoPostal.generarLote(tipo, notificacionIds);
    }

    public ProcesarRespuestaCorreoResultado procesarRespuestaCorreoPostalDemo(String loteId)
            throws java.io.IOException {
        return correoPostal.procesarRespuestaDemo(loteId);
    }

    public List<CorreoPostalNotificacionListaItem> listarNotificacionesCorreoListasParaLote() {
        return correoPostal.listarNotificacionesListasParaLote();
    }

    public List<CorreoLoteResumen> listarLotesCorreoGenerados() {
        return correoPostal.listarLotesGenerados();
    }

    public AnularLoteCorreoResultado anularLoteCorreoPostalDemo(String loteId) {
        return correoPostal.anularLote(loteId);
    }

    public List<CorreoPostalTrazabilidadItem> buscarTrazabilidadCorreoPorActa(String consultaActa) {
        return correoPostal.buscarTrazabilidadPorActa(consultaActa);
    }

    public EnviarIndividualCorreoResultado enviarIndividualCorreoPostalDemo(String notificacionId) {
        return correoPostal.enviarIndividual(notificacionId);
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
     * Acción administrativa "Pago voluntario": Dirección de Faltas fija
     * {@code monto} y deja habilitado el pago voluntario. El acta pasa a
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link #ACCION_EVALUAR_PAGO_VOLUNTARIO}. No genera comprobantes
     * (sin EM, sin RC, sin Cmte/Pref/Nro). Fachada pública; la lógica
     * vive en {@link PagoVoluntarioSupport}.
     */
    public RegistrarSolicitudPagoVoluntarioResultado registrarSolicitudPagoVoluntario(
            String actaId, BigDecimal monto) {
        return pagoVoluntario.registrarSolicitudPagoVoluntario(actaId, monto);
    }
    /**
     * Solicitud de pago voluntario iniciada por el infractor desde el portal.
     * Sin monto: Direccion de Faltas evaluara y, si corresponde, lo fijara.
     * Fachada publica; la logica vive en {@link PagoVoluntarioSupport}.
     *
     * <p>Regla de portal: si el acta está en revisión (D1/D2, todavía no
     * validada como notificable; ver {@link #actaEnRevisionParaPortal}), el
     * portal no admite acciones sustantivas y la solicitud se rechaza con
     * {@link RegistrarSolicitudPagoVoluntarioEstado#CONFLICT_EN_REVISION}
     * sin modificar el expediente.
     */
    public RegistrarSolicitudPagoVoluntarioResultado solicitarPagoVoluntarioDesdePortal(
            String actaId) {
        if (actaEnRevisionParaPortal(actaId)) {
            return new RegistrarSolicitudPagoVoluntarioResultado(
                    RegistrarSolicitudPagoVoluntarioEstado.CONFLICT_EN_REVISION,
                    null, null, null, null, null);
        }
        if (hayFalloCondenatorioDictado(actaId)) {
            return new RegistrarSolicitudPagoVoluntarioResultado(
                    RegistrarSolicitudPagoVoluntarioEstado.CONFLICT_FALLO_DICTADO,
                    actaId, null, null, null, null);
        }
        return pagoVoluntario.solicitarPagoVoluntarioDesdePortal(actaId);
    }

    /**
     * Dirección de Faltas fija el monto del pago voluntario cuando el infractor
     * solicitó el pago pero aún no hay monto asignado. Fachada pública; la
     * lógica vive en {@link PagoVoluntarioSupport}.
     */
    public FijarMontoPagoVoluntarioResultado fijarMontoPagoVoluntario(
            String actaId, BigDecimal monto) {
        return pagoVoluntario.fijarMontoPagoVoluntario(actaId, monto);
    }

    /**
     * Dirección de Faltas registra que el plazo/oportunidad de pago voluntario
     * venció sin que el infractor pagara. El acta queda en situación
     * {@link SituacionPagoMock#VENCIDO} y puede continuar hacia fallo de fondo.
     * El {@code montoPagoVoluntario} se conserva como dato histórico.
     * Fachada pública; la lógica vive en {@link PagoVoluntarioSupport}.
     */
    public RegistrarVencimientoPagoVoluntarioResultado registrarVencimientoPagoVoluntario(
            String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new RegistrarVencimientoPagoVoluntarioResultado(
                    RegistrarVencimientoPagoVoluntarioEstado.NOT_FOUND,
                    null, null, null, null, null);
        }
        BigDecimal monto = getMontoPagoVoluntario(actaId);
        if (monto == null
                || monto.signum() <= 0
                || cerrabilidad.getResultadoFinal(actaId) != ResultadoFinalCierreMock.SIN_RESULTADO_FINAL
                || hayFalloDictado(actaId)) {
            return new RegistrarVencimientoPagoVoluntarioResultado(
                    RegistrarVencimientoPagoVoluntarioEstado.CONFLICT,
                    null, null, null, getSituacionPago(actaId), monto);
        }
        return pagoVoluntario.registrarVencimientoPagoVoluntario(actaId);
    }

    /**
     * Monto fijado por Dirección de Faltas al habilitar el pago voluntario,
     * o {@code null} si todavía no se fijó. Visible por API en el snapshot
     * {@code ActaDetalleResponse.montoPagoVoluntario}.
     */
    public BigDecimal getMontoPagoVoluntario(String actaId) {
        return montoPagoVoluntarioPorActa.get(actaId);
    }

    /**
     * Monto de condena fijado al dictar fallo condenatorio, o {@code null}
     * si todavía no se dictó. Visible por API en
     * {@code ActaDetalleResponse.montoCondena} y
     * {@code ActaInfractorResponse.montoCondena}.
     */
    public BigDecimal getMontoCondena(String actaId) {
        return montoCondenaPorActa.get(actaId);
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








