package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.application.result.DatasetFuncionalCoberturaResultado;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Catalogo canonico del dataset funcional completo del dominio in-memory.
 *
 * Declara todas las actas mock funcionales necesarias para cubrir los casos de uso
 * del sistema de faltas actualmente implementados (Slice 1 hasta 8F-4).
 *
 * Cada acta mock:
 * - tiene un codigo estable y unico
 * - declara el caso de uso que cubre
 * - declara el estado inicial esperado (bloque, situacion, resultadoFinal, bandeja)
 * - declara los documentos esperados
 * - puede construirse de forma deterministica via construirActa()
 *
 * No usa JDBC, SQL, JPA/Hibernate, ni escribe archivos fisicos.
 * No depende de frontend, storage real ni librerias PDF.
 *
 * Slice 8F-4B.
 */
public final class DatasetFuncionalDominioCatalog {

    // --- Datos base reutilizables para construir actas mock ---

    private static final LocalDate FECHA_ACTA = LocalDate.of(2024, 3, 15);
    private static final LocalDateTime FH_LABRADO = LocalDateTime.of(2024, 3, 15, 10, 30);
    private static final ar.gob.malvinas.faltas.core.domain.enums.TipoActa TIPO_ACTA = ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO;
    private static final Long DEPENDENCIA = 1L;
    private static final Long INSPECTOR = 1L;
    private static final String DOM_HECHO = "Avenida Pioneros 2345, Malvinas Argentinas";
    private static final String DOM_INFRACTOR = "Belgrano 200, Malvinas Argentinas";
    private static final String OBSERVACIONES = "Conduccion sin revision tecnica obligatoria vigente";
    private static final String INFRACTOR_NOMBRE = "Juan Carlos Perez";
    private static final String INFRACTOR_DOC = "12345678";

    private DatasetFuncionalDominioCatalog() {}

    // =========================================================================
    // DEFINICIONES DEL DATASET FUNCIONAL COMPLETO
    // =========================================================================

    private static final List<ActaMockFuncionalDefinicion> DEFINICIONES = List.of(

        // =====================================================================
        // ACT-001 - Slice 1: Acta recien labrada (CAPT)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-001-LABRADA",
            "Acta recien labrada en captura",
            "Acta de transito recien creada. Estado inicial del circuito. Inspector registra la infraccion.",
            "Slice 1 - LabrarActa",
            List.of("Slice 1 - LabrarActa", "Estado inicial CAPT", "Bandeja ACTAS_EN_ENRIQUECIMIENTO"),
            BloqueActual.CAPT,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO,
            false, false, false, false, false, false, false, false, false,
            List.of(),
            List.of(TipoEventoActa.ACTLAB),
            List.of("POST /api/faltas/actas/labrar", "ActaService.labrar()"),
            List.of("Estado inicial puro. Bloque CAPT. Ningun documento esperado todavia.")
        ),

        // =====================================================================
        // ACT-002 - Slice 1: Captura completada, en enriquecimiento (ENRI)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-002-EN-ENRIQUECIMIENTO",
            "Acta con captura completada, en enriquecimiento",
            "Captura completada por el inspector. Acta en bloque ENRI para completar datos adicionales.",
            "Slice 1 - CompletarCaptura (CAPT->ENRI)",
            List.of("Slice 1 - CompletarCaptura", "Transicion CAPT->ENRI", "EnriquecerActa"),
            BloqueActual.ENRI,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO,
            false, false, false, false, false, false, false, false, false,
            List.of(),
            List.of(TipoEventoActa.ACTLAB, TipoEventoActa.ACTCAP),
            List.of("POST /api/faltas/actas/{id}/completar-captura", "ActaService.completarCaptura()"),
            List.of("Bloque ENRI. Accion esperada: GENERAR_DOCUMENTO o ENRIQUECER.")
        ),

        // =====================================================================
        // ACT-003 - Slice 1: En enriquecimiento con documento pendiente de firma
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-003-DOC-PENDIENTE-FIRMA",
            "Acta en enriquecimiento con documento pendiente de firma",
            "Acta en ENRI con documento de acta de infraccion generado y pendiente de firma institucional.",
            "Slice 1 - GenerarDocumento -> PENDIENTE_FIRMA",
            List.of("Slice 1 - GenerarDocumento", "FirmarDocumento", "Bandeja PENDIENTE_FIRMA"),
            BloqueActual.ENRI,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_FIRMA,
            false, false, false, false, false, true, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.GENERAR_ACTA_INFRACCION, TipoDocu.ACTA_INFRACCION,
                    false, false, true, false, false,
                    "ENRI", "Documento inicial del expediente, requiere firma antes de notificar")
            ),
            List.of(TipoEventoActa.ACTLAB, TipoEventoActa.ACTCAP, TipoEventoActa.ACTENR, TipoEventoActa.DOCGEN),
            List.of("POST /api/faltas/actas/{id}/documentos/generar", "DocumentoService.generar()"),
            List.of("Bandeja PENDIENTE_FIRMA. Accion: FIRMAR_DOCUMENTO.")
        ),

        // =====================================================================
        // ACT-004 - Slice 1: Documento firmado, pendiente de notificacion
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-004-PENDIENTE-NOTIFICACION",
            "Acta con documento firmado, pendiente de notificacion",
            "Documento de acta de infraccion firmado institucionalmente. Listo para enviar notificacion al infractor.",
            "Slice 1 - FirmarDocumento -> EnviarNotificacion",
            List.of("Slice 1 - FirmarDocumento", "EnviarNotificacion", "Bandeja PENDIENTE_NOTIFICACION"),
            BloqueActual.ENRI,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_NOTIFICACION,
            false, false, false, false, true, true, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_ACTA, TipoDocu.NOTIFICACION_ACTA,
                    true, true, false, false, true,
                    "ENRI->NOTI", "Notificacion del acta al infractor")
            ),
            List.of(TipoEventoActa.ACTLAB, TipoEventoActa.ACTCAP, TipoEventoActa.ACTENR,
                    TipoEventoActa.DOCGEN, TipoEventoActa.DOCFIR),
            List.of("POST /api/faltas/actas/{id}/documentos/{docId}/firmar", "DocumentoService.firmar()"),
            List.of("Accion: ENVIAR_NOTIFICACION.")
        ),

        // =====================================================================
        // ACT-005 - Slice 1: Notificacion de acta en curso
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-005-NOTI-ACTA-EN-CURSO",
            "Acta con notificacion de acta enviada, esperando resultado",
            "Notificacion del acta enviada al infractor. El sistema espera el resultado.",
            "Slice 1 - EnviarNotificacion -> RegistrarResultadoNotificacion",
            List.of("Slice 1 - EnviarNotificacion", "RegistrarNotificacionPositiva",
                    "RegistrarNotificacionNegativa", "RegistrarNotificacionVencida",
                    "Bloque NOTI", "Bandeja EN_NOTIFICACION"),
            BloqueActual.NOTI,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.EN_NOTIFICACION,
            false, false, false, false, true, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_ACTA, TipoDocu.NOTIFICACION_ACTA,
                    true, true, false, false, true,
                    "NOTI", "Notificacion enviada, pendiente de acuse")
            ),
            List.of(TipoEventoActa.ACTLAB, TipoEventoActa.ACTCAP, TipoEventoActa.ACTENR,
                    TipoEventoActa.DOCGEN, TipoEventoActa.DOCFIR, TipoEventoActa.NOTENV),
            List.of("POST /api/faltas/actas/{id}/notificaciones/enviar",
                    "POST /api/faltas/actas/{id}/notificaciones/{notifId}/positiva",
                    "POST /api/faltas/actas/{id}/notificaciones/{notifId}/negativa",
                    "NotificacionService"),
            List.of("Bloque NOTI. Accion: EVALUAR_NOTIFICACION.")
        ),

        // =====================================================================
        // ACT-006 - Slice 3A: En analisis lista para dictado de fallo
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-006-ANAL-LISTA-FALLO",
            "Acta en analisis, lista para dictado de fallo",
            "Notificacion con resultado positivo. Acta en bloque ANAL. Espera dictado de fallo.",
            "Slice 3A - DictarFalloAbsolutorio / DictarFalloCondenatorio",
            List.of("Slice 1 - RegistrarNotificacionPositiva -> ANAL",
                    "Slice 3A - DictarFalloAbsolutorio", "Slice 3A - DictarFalloCondenatorio",
                    "Bloque ANAL", "Bandeja PENDIENTE_ANALISIS"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Acto administrativo de fallo, obligatorio para cerrar el circuito")
            ),
            List.of(TipoEventoActa.ACTLAB, TipoEventoActa.ACTCAP, TipoEventoActa.ACTENR,
                    TipoEventoActa.DOCGEN, TipoEventoActa.DOCFIR, TipoEventoActa.NOTENV,
                    TipoEventoActa.NOTPOS),
            List.of("POST /api/faltas/actas/{id}/fallo/absolutorio",
                    "POST /api/faltas/actas/{id}/fallo/condenatorio", "FalloActaService"),
            List.of("Bloque ANAL. Accion: DICTAR_FALLO.")
        ),

        // =====================================================================
        // ACT-007 - Slice 2: Pago voluntario solicitado
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-007-PAGVOL-SOLICITADO",
            "Acta con pago voluntario solicitado",
            "Infractor solicita pagar voluntariamente la multa. El sistema espera fijacion de monto.",
            "Slice 2 - SolicitarPagoVoluntario",
            List.of("Slice 2 - SolicitarPagoVoluntario", "Slice 2 - FijarMontoPagoVoluntario",
                    "EstadoPagoVoluntario.SOLICITADO", "Bandeja PENDIENTE_ANALISIS"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, false, true, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, true, false, false, true,
                    "ANAL", "Intimacion de pago, se genera tras fijar el monto")
            ),
            List.of(TipoEventoActa.PAGVSO),
            List.of("POST /api/faltas/actas/{id}/pago-voluntario/solicitar",
                    "POST /api/faltas/actas/{id}/pago-voluntario/fijar-monto",
                    "PagoVoluntarioService"),
            List.of("EstadoPagoVoluntario: SOLICITADO. Siguiente: FijarMonto -> InformarPago.")
        ),

        // =====================================================================
        // ACT-008 - Slice 2: Pago voluntario pendiente de confirmacion
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-008-PAGVOL-PENDIENTE-CONF",
            "Acta con pago voluntario informado, pendiente de confirmacion",
            "Infractor informo el pago voluntario. El sistema espera confirmacion del organismo.",
            "Slice 2 - ConfirmarPagoVoluntario / ObservarPagoVoluntario",
            List.of("Slice 2 - InformarPagoVoluntario", "Slice 2 - ConfirmarPagoVoluntario",
                    "Slice 2 - ObservarPagoVoluntario",
                    "EstadoPagoVoluntario.PENDIENTE_CONFIRMACION",
                    "Bandeja PENDIENTE_CONFIRMACION_PAGO"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO,
            false, false, false, true, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, true, false, false, true,
                    "ANAL", "Intimacion de pago referenciada en tramite de confirmacion")
            ),
            List.of(TipoEventoActa.PAGVSO, TipoEventoActa.PAGVMF, TipoEventoActa.PAGINF),
            List.of("POST /api/faltas/actas/{id}/pago-voluntario/confirmar",
                    "POST /api/faltas/actas/{id}/pago-voluntario/observar"),
            List.of("Bandeja PENDIENTE_CONFIRMACION_PAGO. Accion: CONFIRMAR_PAGO.")
        ),

        // =====================================================================
        // ACT-009 - Slice 2: Acta cerrada por pago voluntario confirmado
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-009-PAGVOL-CONFIRMADO",
            "Acta cerrada por pago voluntario confirmado",
            "Pago voluntario confirmado por el organismo. Acta cerrada definitivamente.",
            "Slice 2 - ConfirmarPagoVoluntario -> PAGO_VOLUNTARIO_PAGADO -> CERRADA",
            List.of("Slice 2 - ConfirmarPagoVoluntario",
                    "ResultadoFinal.PAGO_VOLUNTARIO_PAGADO",
                    "Bandeja CERRADAS", "Bloque CERR", "SituacionAdministrativa.CERRADA"),
            BloqueActual.CERR,
            SituacionAdministrativaActa.CERRADA,
            ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO,
            CodigoBandeja.CERRADAS,
            true, false, false, true, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_CONSTANCIA, TipoDocu.CONSTANCIA,
                    true, true, false, false, false,
                    "CERR", "Constancia de pago voluntario, opcional al cierre")
            ),
            List.of(TipoEventoActa.PAGVSO, TipoEventoActa.PAGVMF, TipoEventoActa.PAGINF,
                    TipoEventoActa.PAGCNF, TipoEventoActa.CIERRA),
            List.of("POST /api/faltas/actas/{id}/pago-voluntario/confirmar"),
            List.of("ResultadoFinal: PAGO_VOLUNTARIO_PAGADO. SituacionAdministrativa: CERRADA. Bloque: CERR.")
        ),

        // =====================================================================
        // ACT-010 - Slice 3A: Fallo absolutorio dictado, pendiente de firma
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-010-FALLO-ABS-DICTADO",
            "Acta con fallo absolutorio dictado, pendiente de firma",
            "Fallo absolutorio dictado. Documento de fallo en estado PENDIENTE_FIRMA.",
            "Slice 3A - DictarFalloAbsolutorio",
            List.of("Slice 3A - DictarFalloAbsolutorio", "EstadoFalloActa.DICTADO",
                    "Bandeja PENDIENTE_FIRMA", "TipoFalloActa.ABSOLUTORIO"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_FIRMA,
            false, false, true, false, false, true, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo absolutorio, requiere firma antes de notificar"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO,
                    true, true, false, false, true,
                    "ANAL->CERR", "Notificacion del fallo absolutorio al infractor")
            ),
            List.of(TipoEventoActa.FALABS, TipoEventoActa.DOCGEN),
            List.of("POST /api/faltas/actas/{id}/fallo/absolutorio", "FalloActaService.dictarAbsolutorio()"),
            List.of("Bandeja PENDIENTE_FIRMA. EstadoFallo: DICTADO.")
        ),

        // =====================================================================
        // ACT-011 - Slice 3A: Acta absuelta y cerrada
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-011-ABSUELTO-CERRADO",
            "Acta absuelta y cerrada definitivamente",
            "Fallo absolutorio notificado. Acta cerrada con resultado ABSUELTO.",
            "Slice 3A - RegistrarNotificacionPositiva (fallo absolutorio) -> ABSUELTO -> CERRADA",
            List.of("Slice 3A - FalloAbsolutorio NOTIFICADO -> CERRADA",
                    "ResultadoFinal.ABSUELTO",
                    "Bandeja CERRADAS", "SituacionAdministrativa.CERRADA"),
            BloqueActual.CERR,
            SituacionAdministrativaActa.CERRADA,
            ResultadoFinalActa.ABSUELTO,
            CodigoBandeja.CERRADAS,
            true, false, true, false, true, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo absolutorio firmado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO,
                    true, true, false, false, true,
                    "ANAL->CERR", "Notificacion del fallo absolutorio")
            ),
            List.of(TipoEventoActa.FALABS, TipoEventoActa.DOCGEN, TipoEventoActa.DOCFIR,
                    TipoEventoActa.NOTENV, TipoEventoActa.NOTPOS, TipoEventoActa.CIERRA),
            List.of("POST /api/faltas/actas/{id}/notificaciones/{notifId}/positiva"),
            List.of("ResultadoFinal: ABSUELTO. Ciclo completo para fallo absolutorio sin bloqueantes.")
        ),

        // =====================================================================
        // ACT-012 - Slice 3A: Fallo condenatorio dictado, pendiente de firma
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-012-FALLO-COND-DICTADO",
            "Acta con fallo condenatorio dictado, pendiente de firma",
            "Fallo condenatorio dictado con monto de condena. Documento de fallo pendiente de firma.",
            "Slice 3A - DictarFalloCondenatorio",
            List.of("Slice 3A - DictarFalloCondenatorio", "EstadoFalloActa.DICTADO",
                    "TipoFalloActa.CONDENATORIO", "Bandeja PENDIENTE_FIRMA"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_FIRMA,
            false, false, true, false, false, true, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio, requiere firma"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO,
                    true, true, false, false, true,
                    "ANAL", "Notificacion del fallo condenatorio al infractor"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, true, false, false, true,
                    "ANAL->pago", "Intimacion de pago de condena, post-firmeza")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.DOCGEN),
            List.of("POST /api/faltas/actas/{id}/fallo/condenatorio", "FalloActaService.dictarCondenatorio()"),
            List.of("Bandeja PENDIENTE_FIRMA. EstadoFallo: DICTADO. MontoCondena seteado.")
        ),

        // =====================================================================
        // ACT-013 - Slice 3A: Fallo condenatorio notificado
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-013-FALLO-COND-NOTIFICADO",
            "Acta con fallo condenatorio notificado",
            "Fallo condenatorio firmado y notificado. Bandeja PENDIENTES_FALLO, esperando apelacion o firmeza.",
            "Slice 3A - FalloCondenatorio NOTIFICADO",
            List.of("Slice 3A - FalloCondenatorio NOTIFICADO",
                    "Slice 3B - RegistrarApelacion", "Slice 4 - DeclararCondenaFirme",
                    "Bandeja PENDIENTES_FALLO"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTES_FALLO,
            false, false, true, false, true, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio firmado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO,
                    true, true, false, false, true,
                    "ANAL", "Notificacion del fallo realizada"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, true, false, false, true,
                    "ANAL->pago", "Intimacion de pago de condena")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.DOCGEN, TipoEventoActa.DOCFIR,
                    TipoEventoActa.NOTENV, TipoEventoActa.NOTPOS),
            List.of("NotificacionService", "ApelacionActaService", "FirmezaCondenaService"),
            List.of("EstadoFallo: NOTIFICADO. Listo para apelacion (APEPRE) o plazo (PLAVNC).")
        ),

        // =====================================================================
        // ACT-014 - Slice 3B: Apelacion presentada
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-014-APELACION-PRESENTADA",
            "Acta con apelacion presentada",
            "Infractor presento recurso de apelacion contra el fallo condenatorio.",
            "Slice 3B - RegistrarApelacion",
            List.of("Slice 3B - RegistrarApelacion", "EstadoApelacionActa.PRESENTADA",
                    "Bandeja CON_APELACION", "Slice 3C - ResolverApelacion"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.CON_APELACION,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio ya emitido previo a la apelacion"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO,
                    true, true, false, false, true,
                    "ANAL", "Notificacion del fallo condenatorio ya realizada"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_CONSTANCIA, TipoDocu.CONSTANCIA,
                    true, true, false, false, false,
                    "ANAL", "Constancia de presentacion de apelacion")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.APEPRE),
            List.of("POST /api/faltas/actas/{id}/apelacion/registrar", "ApelacionActaService"),
            List.of("Bandeja CON_APELACION. Accion: RESOLVER_APELACION.")
        ),

        // =====================================================================
        // ACT-015 - Slice 4: Condena firme declarada
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-015-CONDENA-FIRME",
            "Acta con condena firme declarada",
            "Condena firme declarada por vencimiento de plazo de apelacion o apelacion rechazada.",
            "Slice 4 - DeclararCondenaFirmePorApelacionRechazada / VencerPlazoApelacion",
            List.of("Slice 4 - DeclararCondenaFirme", "Slice 3C - ResolverApelacionRechazada",
                    "ResultadoFinal.CONDENA_FIRME",
                    "Bandeja PENDIENTE_PAGO_CONDENA",
                    "Slice 5 - InformarPagoCondena"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.CONDENA_FIRME,
            CodigoBandeja.PENDIENTE_PAGO_CONDENA,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio emitido y firmado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, true, false, false, true,
                    "ANAL", "Intimacion de pago de condena firme")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.PLAVNC, TipoEventoActa.CONFIR),
            List.of("POST /api/faltas/actas/{id}/firmeza/declarar", "FirmezaCondenaService"),
            List.of("ResultadoFinal: CONDENA_FIRME. Acta ACTIVA/ANAL. Siguiente: InformarPagoCondena.")
        ),

        // =====================================================================
        // ACT-016 - Slice 5: Pago de condena informado
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-016-PAGO-CONDENA-INFORMADO",
            "Acta con pago de condena informado, pendiente de confirmacion",
            "Infractor informo el pago de condena. El sistema espera confirmacion del organismo.",
            "Slice 5 - InformarPagoCondena -> ConfirmarPagoCondena",
            List.of("Slice 5 - InformarPagoCondena", "Slice 5 - ConfirmarPagoCondena",
                    "Slice 5 - ObservarPagoCondena",
                    "EstadoPagoCondena.PENDIENTE_CONFIRMACION",
                    "Bandeja PENDIENTE_CONFIRMACION_PAGO_CONDENA"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.CONDENA_FIRME,
            CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA,
            false, false, true, true, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio emitido y firmado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, true, false, false, true,
                    "ANAL", "Intimacion de pago emitida previamente")
            ),
            List.of(TipoEventoActa.CONFIR, TipoEventoActa.PCOINF),
            List.of("POST /api/faltas/actas/{id}/pago-condena/informar",
                    "POST /api/faltas/actas/{id}/pago-condena/confirmar", "PagoCondenaService"),
            List.of("EstadoPagoCondena: PENDIENTE_CONFIRMACION. Accion: CONFIRMAR_PAGO_CONDENA.")
        ),

        // =====================================================================
        // ACT-017 - Slice 5: Condena firme pagada
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-017-CONDENA-FIRME-PAGADA",
            "Acta cerrada por condena firme pagada",
            "Pago de condena confirmado. Acta cerrada con resultado CONDENA_FIRME_PAGADA.",
            "Slice 5 - ConfirmarPagoCondena -> CONDENA_FIRME_PAGADA -> CERRADA",
            List.of("Slice 5 - ConfirmarPagoCondena",
                    "ResultadoFinal.CONDENA_FIRME_PAGADA",
                    "Bandeja CERRADAS", "SituacionAdministrativa.CERRADA"),
            BloqueActual.CERR,
            SituacionAdministrativaActa.CERRADA,
            ResultadoFinalActa.CONDENA_FIRME_PAGADA,
            CodigoBandeja.CERRADAS,
            true, false, true, true, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio emitido y firmado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, true, true, false, true,
                    "PAGCON", "Intimacion de pago de condena emitida"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_CONSTANCIA, TipoDocu.CONSTANCIA,
                    true, true, false, false, false,
                    "CERR", "Constancia de pago de condena")
            ),
            List.of(TipoEventoActa.CONFIR, TipoEventoActa.PCOINF, TipoEventoActa.PCOCNF, TipoEventoActa.CIERRA),
            List.of("POST /api/faltas/actas/{id}/pago-condena/confirmar"),
            List.of("ResultadoFinal: CONDENA_FIRME_PAGADA. Ciclo cerrado para pago de condena.")
        ),

        // =====================================================================
        // ACT-018 - Slice 6A: Derivada a gestion externa
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-018-GESTION-EXTERNA",
            "Acta derivada a gestion externa",
            "Acta con condena firme derivada a gestion externa (apremio o juzgado de paz).",
            "Slice 6A - DerivarGestionExterna",
            List.of("Slice 6A - DerivarGestionExterna",
                    "SituacionAdministrativa.EN_GESTION_EXTERNA",
                    "Bandeja GESTION_EXTERNA",
                    "Slice 6B - ReingresarDesdeGestionExterna",
                    "Slice 6C - RegistrarPagoExternoGestion",
                    "Slice 6D-1 - REINGRESO_SIN_PAGO / REINGRESO_PARA_REVISION",
                    "Slice 6D-2 - REINGRESO_CON_DICTAMEN"),
            BloqueActual.GEXT,
            SituacionAdministrativaActa.EN_GESTION_EXTERNA,
            ResultadoFinalActa.CONDENA_FIRME,
            CodigoBandeja.GESTION_EXTERNA,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "GEXT", "Fallo condenatorio emitido previo a la derivacion externa")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.CONFIR, TipoEventoActa.EXTDER),
            List.of("POST /api/faltas/actas/{id}/gestion-externa/derivar",
                    "POST /api/faltas/actas/{id}/gestion-externa/reingresar",
                    "POST /api/faltas/actas/{id}/gestion-externa/pago-externo",
                    "GestionExternaService"),
            List.of("SituacionAdministrativa: EN_GESTION_EXTERNA. Bloque: GEXT.")
        ),

        // =====================================================================
        // ACT-019 - Slice 6C: Pago externo por apremio registrado
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-019-GESTION-EXTERNA-PAGO-EXTERNO",
            "Acta con pago externo por apremio registrado",
            "Gestion externa con pago registrado via PAGAPR. Acta con CONDENA_FIRME_PAGADA.",
            "Slice 6C - RegistrarPagoExternoGestion (PAGAPR)",
            List.of("Slice 6C - RegistrarPagoExternoGestion", "Evento PAGAPR",
                    "ResultadoGestionExterna.PAGO_REGISTRADO",
                    "ResultadoFinal.CONDENA_FIRME_PAGADA"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.CONDENA_FIRME_PAGADA,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio emitido previo a la gestion externa")
            ),
            List.of(TipoEventoActa.EXTDER, TipoEventoActa.PAGAPR),
            List.of("POST /api/faltas/actas/{id}/gestion-externa/pago-externo"),
            List.of("CONDENA_FIRME_PAGADA via PAGAPR. Acta retorno a ANAL ACTIVA. Cierre puede diferirse por bloqueantes.")
        ),

        // =====================================================================
        // ACT-020 - Transversal: Acta paralizada
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-020-PARALIZADA",
            "Acta paralizada temporalmente",
            "Acta paralizada por causa administrativa. Acciones de dominio bloqueadas.",
            "Transversal - Paralizacion/reactivacion",
            List.of("Paralizacion - SituacionAdministrativa.PARALIZADA",
                    "Bandeja PARALIZADAS",
                    "Reactivacion - ACTPAR/ACTREA"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.PARALIZADA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PARALIZADAS,
            false, true, false, false, false, false, false, false, false,
            List.of(),
            List.of(TipoEventoActa.ACTPAR),
            List.of("ActaService - paralizar/reactivar"),
            List.of("SituacionAdministrativa: PARALIZADA. Acciones del dominio bloqueadas.")
        ),

        // =====================================================================
        // ACT-021 - Slice 7B: Bloqueante material activo
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-021-BLOQUEANTE-ACTIVO",
            "Acta con bloqueante material activo",
            "Acta con bloqueante material activo. El cierre esta bloqueado aunque tenga resultado cerrable.",
            "Slice 7B - RegistrarBloqueanteMaterial",
            List.of("Slice 7B - RegistrarBloqueanteMaterial",
                    "EstadoBloqueanteMaterial.PENDIENTE",
                    "Slice 7A - RepositoryBloqueantesMaterialesChecker",
                    "Slice 7C - Cierre diferido al resolver el bloqueante"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, false, false, false, false, false, true, true,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE, TipoDocu.RESOLUTORIO_BLOQUEANTE,
                    true, true, true, true, false,
                    "ANAL", "Resolutorio de la traba bloqueante material")
            ),
            List.of(),
            List.of("BloqueanteMaterialService",
                    "POST /api/faltas/bloqueantes/registrar",
                    "POST /api/faltas/bloqueantes/{id}/cumplir",
                    "POST /api/faltas/bloqueantes/{id}/anular"),
            List.of("BloqueantesMaterialesChecker bloquea el cierre. Slice 7C: cierre diferido al resolverse.")
        ),

        // =====================================================================
        // ACT-022 - Slice 7C: Absuelto con bloqueante activo (cierre diferido)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-022-ABSUELTO-CON-BLOQUEANTE",
            "Acta absuelta con bloqueante activo (cierre pendiente por Slice 7C)",
            "ResultadoFinal ABSUELTO asignado pero bloqueante activo impide el cierre. Pendiente cierre diferido.",
            "Slice 7C - Cierre diferido por resolucion del ultimo bloqueante",
            List.of("Slice 7C - Cierre diferido",
                    "ResultadoFinal.ABSUELTO con bloqueante activo",
                    "CierreActaHelper.esResultadoCerrable()",
                    "Al cumplir/anular bloqueante: emite CIERRA automatico"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.ABSUELTO,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, true, false, false, false, false, true, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo absolutorio firmado y notificado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE, TipoDocu.RESOLUTORIO_BLOQUEANTE,
                    true, true, true, true, false,
                    "ANAL", "Resolutorio de la traba bloqueante")
            ),
            List.of(TipoEventoActa.FALABS, TipoEventoActa.NOTPOS),
            List.of("BloqueanteMaterialService.cumplir()",
                    "BloqueanteMaterialService.anular()", "CierreActaHelper"),
            List.of("ABSUELTO pero ACTIVA por bloqueante. Al resolverse bloqueante se emite CIERRA.")
        ),

        // =====================================================================
        // ACT-023 - Slice 8F-2: Redaccion en BORRADOR
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-023-REDACCION-BORRADOR",
            "Acta con redaccion de documento en BORRADOR",
            "Acta en ANAL con redaccion de fallo en BORRADOR. Pendiente de confirmacion y generacion PDF.",
            "Slice 8F-2 - CrearRedaccionConContextoActa",
            List.of("Slice 8F-2 - CrearRedaccionConContextoActa",
                    "EstadoRedaccionDocumento.BORRADOR",
                    "DocumentoVariableContextBuilder",
                    "DocumentoCombinacionService",
                    "PlantillasMockSeeder"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, true, false, false, true, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, false, true, true, false,
                    "ANAL", "Redaccion BORRADOR del fallo, sin PDF generado. storageKey/hashDocu/fhGeneracion son null.")
            ),
            List.of(),
            List.of("DocumentoRedaccionService.crearRedaccionConContextoActa()",
                    "DocumentoCombinacionService"),
            List.of("EstadoRedaccion: BORRADOR. storageKey/hashDocu/fhGeneracion son null en BORRADOR.")
        ),

        // =====================================================================
        // ACT-024 - Slice 8F-3: PDF mock generado (redaccion CONFIRMADA)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-024-PDF-MOCK-GENERADO",
            "Acta con PDF mock generado (redaccion CONFIRMADA)",
            "Redaccion confirmada y PDF mock generado. StorageKey mock://, hashDocu sha256-mock-.",
            "Slice 8F-3 - ConfirmarRedaccionYGenerarDocumentoMock",
            List.of("Slice 8F-3 - ConfirmarRedaccionYGenerarDocumentoMock",
                    "EstadoRedaccionDocumento.CONFIRMADA",
                    "DocumentoPdfMockRenderer",
                    "storageKey mock://",
                    "hashDocu sha256-mock-",
                    "Slice 8F-4 - DocumentoGraphDemoService"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, true, false, false, true, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo con PDF mock generado: storageKey mock://, hashDocu sha256-mock-")
            ),
            List.of(),
            List.of("DocumentoGeneracionMockService.confirmarYGenerarMockPdf()",
                    "GET /demo/documentos/graph"),
            List.of("EstadoRedaccion: CONFIRMADA. storageKey/hashDocu/fhGeneracion seteados con valores mock.")
        ),

        // =====================================================================
        // ACT-025 - Guardrail: Precondicion violada (caso negativo)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-025-PRECONDICION-VIOLADA",
            "Acta para validar precondicion violada (caso negativo)",
            "Acta en bloque incorrecto. Prueba guardrails de PrecondicionVioladaException.",
            "Guardrail - PrecondicionVioladaException en estado incorrecto",
            List.of("Guardrail - PrecondicionVioladaException",
                    "Bloque CAPT incompatible con DictarFallo",
                    "Acta CERRADA incompatible con todas las operaciones"),
            BloqueActual.CAPT,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO,
            false, false, false, false, false, false, false, false, false,
            List.of(),
            List.of(TipoEventoActa.ACTLAB),
            List.of("PrecondicionVioladaException"),
            List.of("Caso negativo. DictarFallo en CAPT debe fallar. Usado para guardrails funcionales.")
        ),

        // =====================================================================
        // ACT-026 - Notificacion negativa
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-026-NOTIFICACION-NEGATIVA",
            "Acta con notificacion negativa fallida",
            "Notificacion del acta intentada con resultado negativo. Infractor no encontrado o domicilio incorrecto.",
            "Slice 1 - RegistrarNotificacionNegativa (NOTNEG)",
            List.of("Slice 1 - RegistrarNotificacionNegativa", "TipoEventoActa.NOTNEG",
                    "Bandeja PENDIENTE_ANALISIS post-notificacion-negativa",
                    "RegistrarNotificacionNegativaCommand"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, false, false, true, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_ACTA, TipoDocu.NOTIFICACION_ACTA,
                    true, false, false, false, true,
                    "NOTI", "Notificacion enviada con resultado negativo (NOTNEG). Requiere nueva accion.")
            ),
            List.of(TipoEventoActa.NOTENV, TipoEventoActa.NOTNEG),
            List.of("POST /api/faltas/actas/{id}/notificaciones/registrar-negativa",
                    "RegistrarNotificacionNegativaCommand"),
            List.of("NOTNEG: infractor no encontrado o domicilio incorrecto. El acta vuelve a analisis.")
        ),

        // =====================================================================
        // ACT-027 - Documento adjunto escaneado con firma convalidada
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-027-DOC-ADJUNTO-CONVALIDADO",
            "Acta con documento adjunto escaneado y firma convalidada",
            "Acta con documento fisico escaneado incorporado al expediente. Firma olografa del infractor convalidada.",
            "Slice 1 - IncorporarDocumentoEscaneado + ConvalidarFirmaEscaneada (DOCADJ)",
            List.of("Slice 1 - IncorporarDocumentoEscaneado", "Slice 1 - ConvalidarFirmaEscaneada",
                    "TipoEventoActa.DOCADJ",
                    "Firma olografa infractor vs firma documental institucional: conceptos separados",
                    "storageKey/hash/fhGeneracion obligatorios para adjunto"),
            BloqueActual.ENRI,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO,
            false, false, false, false, false, false, true, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.GENERAR_ACTA_INFRACCION, TipoDocu.ACTA_INFRACCION,
                    false, false, false, false, false,
                    "ENRI", "Acta de infraccion original como base para el adjunto escaneado"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_CONSTANCIA, TipoDocu.CONSTANCIA,
                    false, false, false, false, false,
                    "ENRI", "Constancia de convalidacion de firma escaneada del infractor si corresponde")
            ),
            List.of(TipoEventoActa.DOCADJ),
            List.of("POST /api/faltas/actas/{id}/documentos/adjunto/incorporar",
                    "POST /api/faltas/actas/{id}/documentos/adjunto/convalidar-firma",
                    "IncorporarDocumentoEscaneadoCommand", "ConvalidarFirmaEscaneadaCommand"),
            List.of("Firma olografa del infractor (ResultadoFirmaInfractor) distinta de firma documental institucional.",
                    "storageKey/hashDocu/fhGeneracion obligatorios para el adjunto escaneado.")
        ),

        // =====================================================================
        // ACT-028 - Absolucion firme cerrada
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-028-ABSOLUCION-FIRME-CERRADA",
            "Acta cerrada por absolucion firme",
            "Fallo absolutorio emitido, firmado, notificado y firme. Acta cerrada con resultado ABSUELTO.",
            "Slice 3A - DictarFalloAbsolutorio -> ABSUELTO -> CERRADA",
            List.of("Slice 3A - DictarFalloAbsolutorio", "ResultadoFinal.ABSUELTO",
                    "BloqueActual.CERR", "SituacionAdministrativa.CERRADA",
                    "Bandeja CERRADAS", "Cierre por absolucion firme"),
            BloqueActual.CERR,
            SituacionAdministrativaActa.CERRADA,
            ResultadoFinalActa.ABSUELTO,
            CodigoBandeja.CERRADAS,
            true, false, true, false, true, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, false, false,
                    "ANAL", "Fallo absolutorio emitido, firmado y notificado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO,
                    true, false, false, false, true,
                    "ANAL", "Notificacion del fallo absolutorio con resultado positivo (NOTPOS)")
            ),
            List.of(TipoEventoActa.FALABS, TipoEventoActa.DOCFIR, TipoEventoActa.NOTPOS, TipoEventoActa.CIERRA),
            List.of("POST /api/faltas/actas/{id}/fallo/absolutorio", "FalloActaService"),
            List.of("ResultadoFinal: ABSUELTO. Cierre definitivo post absolucion firme y notificacion positiva.")
        ),

        // =====================================================================
        // ACT-029 - Reingreso para revision desde gestion externa
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-029-REINGRESO-PARA-REVISION",
            "Acta reingresada desde gestion externa para revision",
            "Acta con condena firme derivada a gestion externa y reingresada sin pago. Vuelve a ANAL ACTIVA.",
            "Slice 6D-1 - EXTRET + REINGRESO_PARA_REVISION",
            List.of("Slice 6D-1 - ReingresarDesdeGestionExterna", "Evento EXTRET",
                    "ModoReingresoGestionExterna.REINGRESO_PARA_REVISION",
                    "Bandeja PENDIENTE_ANALISIS", "ANAL ACTIVA post-reingreso"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.CONDENA_FIRME,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, false, false,
                    "ANAL", "Fallo condenatorio emitido que origino la derivacion a gestion externa"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_CONSTANCIA, TipoDocu.CONSTANCIA,
                    false, false, false, false, false,
                    "ANAL", "Constancia de reingreso desde gestion externa si corresponde")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.EXTDER, TipoEventoActa.EXTRET),
            List.of("POST /api/faltas/actas/{id}/gestion-externa/reingresar",
                    "ReingresarDesdeGestionExternaCommand", "ReingresarDesdeGestionExternaService"),
            List.of("CONDENA_FIRME conservada post-reingreso. Modo REINGRESO_PARA_REVISION. Nueva decision pendiente.")
        ),

        // =====================================================================
        // ACT-030 - Pago de condena observado/rechazado
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-030-PAGO-CONDENA-OBSERVADO",
            "Acta con pago de condena observado/rechazado",
            "Pago de condena informado y luego observado/rechazado (PCOOBS). Condena firme continua pendiente de pago.",
            "Slice 5 - ObservarPagoCondena (PCOOBS)",
            List.of("Slice 5 - ObservarPagoCondena", "TipoEventoActa.PCOOBS",
                    "EstadoPagoCondena post-observacion",
                    "Bandeja PENDIENTE_PAGO_CONDENA"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.CONDENA_FIRME,
            CodigoBandeja.PENDIENTE_PAGO_CONDENA,
            false, false, true, true, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, false, false,
                    "ANAL", "Fallo condenatorio emitido y firmado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, false, false, false, true,
                    "ANAL", "Intimacion de pago de condena emitida previamente")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.CONFIR, TipoEventoActa.PCOINF, TipoEventoActa.PCOOBS),
            List.of("POST /api/faltas/actas/{id}/pago-condena/observar", "PagoCondenaService"),
            List.of("CONDENA_FIRME pendiente de pago. PCOOBS: pago observado/rechazado. Requiere nuevo PCOINF.")
        ),

        // =====================================================================
        // ACT-031 - Pago de condena cerrado con descuento/acuerdo
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-031-PAGO-CONDENA-CON-DESCUENTO",
            "Acta cerrada con pago de condena con descuento aplicado",
            "Condena firme pagada con descuento administrativo o acuerdo. Cerrada con CONDENA_FIRME_PAGADA. GAP: dominio actual no distingue PCOCNF con/sin descuento.",
            "Slice 5 - ConfirmarPagoCondena (variante con descuento) - GAP documentado",
            List.of("Slice 5 - ConfirmarPagoCondena (variante descuento)",
                    "ResultadoFinal.CONDENA_FIRME_PAGADA",
                    "GAP: el dominio actual usa PCOCNF sin parametro de descuento separado",
                    "El descuento es atributo del pago, no evento separado en dominio actual"),
            BloqueActual.CERR,
            SituacionAdministrativaActa.CERRADA,
            ResultadoFinalActa.CONDENA_FIRME_PAGADA,
            CodigoBandeja.CERRADAS,
            true, false, true, true, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, false, false,
                    "ANAL", "Fallo condenatorio emitido y firmado"),
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO,
                    true, false, false, false, true,
                    "ANAL", "Intimacion de pago con monto original de la condena"),
                DocumentoEsperadoPorActaMock.opcional(
                    AccionDocumental.EMITIR_CONSTANCIA, TipoDocu.CONSTANCIA,
                    true, false, false, false, false,
                    "CERR", "Constancia de cierre con descuento aplicado")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.CONFIR, TipoEventoActa.PCOINF, TipoEventoActa.PCOCNF, TipoEventoActa.CIERRA),
            List.of("POST /api/faltas/actas/{id}/pago-condena/confirmar", "PagoCondenaService"),
            List.of("GAP: PCOCNF no distingue descuento en dominio actual. Variante documentada para 8F-4C.",
                    "ResultadoFinal: CONDENA_FIRME_PAGADA. Cierre definitivo.")
        ),

        // =====================================================================
        // ACT-032 - Slice 8F-11F: Apelacion presentada con documento adjunto
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-032-APELACION-CON-DOCUMENTOS",
            "Acta con apelacion presentada con escrito adjunto",
            "Infractor presento recurso de apelacion con ESCRITO_APELACION adjunto.",
            "Slice 8F-11F - RegistrarDocumentoApelacion",
            List.of("Slice 8F-11F - RegistrarDocumentoApelacion", "TipoDocumentoApelacion.ESCRITO_APELACION",
                    "Bandeja CON_APELACION"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.CON_APELACION,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio ya emitido")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.APEPRE),
            List.of("POST /api/faltas/apelacion/{id}/documentos", "ApelacionActaService.registrarDocumento()"),
            List.of("Apelacion PRESENTADA con ESCRITO_APELACION adjunto. Bandeja CON_APELACION.")
        ),

        // =====================================================================
        // ACT-033 - Slice 8F-11F: Apelacion mixta (texto + documentacion respaldatoria)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-033-APELACION-MIXTA",
            "Acta con apelacion mixta: texto y documentacion respaldatoria",
            "Infractor presento recurso con texto de fundamentos y DOCUMENTACION_RESPALDATORIA adjunta.",
            "Slice 8F-11F - RegistrarDocumentoApelacion (mixta)",
            List.of("Slice 8F-11F - RegistrarDocumentoApelacion", "TipoDocumentoApelacion.DOCUMENTACION_RESPALDATORIA",
                    "Bandeja CON_APELACION"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.CON_APELACION,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio ya emitido")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.APEPRE),
            List.of("POST /api/faltas/apelacion/{id}/documentos", "ApelacionActaService.registrarDocumento()"),
            List.of("Apelacion PRESENTADA con ESCRITO_APELACION + DOCUMENTACION_RESPALDATORIA. Bandeja CON_APELACION.")
        ),

        // =====================================================================
        // ACT-034 - Slice 8F-11F: Apelacion rechazada (APERAZ)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-034-APELACION-RECHAZADA",
            "Acta con apelacion rechazada, condena pendiente de firmeza",
            "Apelacion resuelta con resultado RECHAZADA (APERAZ). Condena original queda pendiente de firmeza.",
            "Slice 8F-11F - ResolverApelacionRechazada",
            List.of("Slice 8F-11F - ResolverApelacionRechazada", "EstadoApelacionActa.RECHAZADA",
                    "Bandeja PENDIENTE_ANALISIS (declarar firmeza)"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio original subsiste")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.APEPRE, TipoEventoActa.APERAZ),
            List.of("POST /api/faltas/apelacion/rechazar", "ApelacionActaService.resolverRechazada()"),
            List.of("Apelacion RECHAZADA. Condena original subsiste. Pendiente declarar firmeza.")
        ),

        // =====================================================================
        // ACT-035 - Slice 8F-11F: Apelacion absolutoria aceptada (APEABS)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-035-APELACION-ABSOLUTORIA",
            "Acta cerrada por absolucion en apelacion",
            "Apelacion aceptada con resultado ACEPTADA_ABSUELVE (APEABS). Infractor absuelto. Acta CERRADA.",
            "Slice 8F-11F - ResolverApelacionAceptaAbsuelve",
            List.of("Slice 8F-11F - ResolverApelacionAceptaAbsuelve", "EstadoApelacionActa.ACEPTADA_ABSUELVE",
                    "ResultadoFinal.ABSUELTO", "Bandeja CERRADAS"),
            BloqueActual.CERR,
            SituacionAdministrativaActa.CERRADA,
            ResultadoFinalActa.ABSUELTO,
            CodigoBandeja.CERRADAS,
            true, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo condenatorio previo a la absolucion")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.APEPRE, TipoEventoActa.APEABS, TipoEventoActa.CIERRA),
            List.of("POST /api/faltas/apelacion/absolver", "ApelacionActaService.resolverAceptaAbsuelve()"),
            List.of("ACEPTADA_ABSUELVE. Acta CERRADA. ResultadoFinal: ABSUELTO.")
        ),

        // =====================================================================
        // ACT-036 - Slice 8F-11F: Apelacion que modifica la condena (APEMCO)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-036-APELACION-MODIFICA-CONDENA",
            "Acta con apelacion que modifica el monto de la condena",
            "Apelacion resuelta con MODIFICA_CONDENA (APEMCO). Nuevo fallo vigente con monto 2500.",
            "Slice 8F-11F - ResolverApelacionModificaCondena",
            List.of("Slice 8F-11F - ResolverApelacionModificaCondena", "ResultadoResolucionApelacion.MODIFICA_CONDENA",
                    "Nuevo fallo vigente 2500", "Bandeja PENDIENTES_FALLO"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.SIN_RESULTADO_FINAL,
            CodigoBandeja.PENDIENTES_FALLO,
            false, false, true, false, false, false, false, false, false,
            List.of(
                DocumentoEsperadoPorActaMock.obligatorio(
                    AccionDocumental.EMITIR_FALLO, TipoDocu.ACTO_ADMINISTRATIVO,
                    true, true, true, true, false,
                    "ANAL", "Fallo original reemplazado por nuevo fallo con monto modificado")
            ),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.APEPRE, TipoEventoActa.FALRMP, TipoEventoActa.APEMCO),
            List.of("POST /api/faltas/apelacion/modificar-condena", "ApelacionActaService.resolverModificaCondena()"),
            List.of("MODIFICA_CONDENA. Nuevo fallo NOTIFICADO con monto 2500. Bandeja PENDIENTES_FALLO.")
        ),

        // =====================================================================
        // ACT-037 - Slice 8F-11F: Apelacion con resultado nulidad (APENUL)
        // =====================================================================
        new ActaMockFuncionalDefinicion(
            "ACT-037-APELACION-NULIDAD",
            "Acta con nulidad declarada en apelacion",
            "Apelacion resuelta con NULIDAD (APENUL). Fallo desactivado. Acta activa pendiente nuevo fallo.",
            "Slice 8F-11F - ResolverApelacionNulidad",
            List.of("Slice 8F-11F - ResolverApelacionNulidad", "ResultadoResolucionApelacion.NULIDAD",
                    "Fallo desactivado", "Bandeja PENDIENTE_ANALISIS"),
            BloqueActual.ANAL,
            SituacionAdministrativaActa.ACTIVA,
            ResultadoFinalActa.NULIDAD,
            CodigoBandeja.PENDIENTE_ANALISIS,
            false, false, false, false, false, false, false, false, false,
            List.of(),
            List.of(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS, TipoEventoActa.APEPRE, TipoEventoActa.APENUL),
            List.of("POST /api/faltas/apelacion/nulidad", "ApelacionActaService.resolverNulidad()"),
            List.of("NULIDAD declarada. Fallo desactivado. Acta activa. ResultadoFinal: NULIDAD. Bandeja PENDIENTE_ANALISIS.")
        )
    );

    // =========================================================================
    // API publica del catalogo
    // =========================================================================

    /** Devuelve todas las definiciones del dataset funcional. */
    public static List<ActaMockFuncionalDefinicion> obtenerTodasLasDefiniciones() {
        return DEFINICIONES;
    }

    /** Devuelve una definicion por codigo, o lanza excepcion si no existe. */
    public static ActaMockFuncionalDefinicion buscarPorCodigo(String codigo) {
        return DEFINICIONES.stream()
                .filter(d -> d.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro definicion de acta mock con codigo: " + codigo));
    }

    /**
     * Construye una FalActa en el estado inicial esperado para el codigo dado.
     *
     * El acta tiene datos base representativos del dominio.
     * El bloque, situacion y resultadoFinal se preestablecen segun la definicion.
     *
     * @param codigo codigo de la definicion
     * @param id     id tecnico a asignar al acta
     */
    public static FalActa construirActa(String codigo, Long id) {
        ActaMockFuncionalDefinicion def = buscarPorCodigo(codigo);
        return construirActa(def, id);
    }

    /**
     * Construye una FalActa directamente desde una definicion.
     */
    public static FalActa construirActa(ActaMockFuncionalDefinicion def, Long id) {
        FalActa acta = new FalActa(
                id,
                "uuid-mock-" + def.codigo().toLowerCase() + "-" + id,
                TIPO_ACTA,
                DEPENDENCIA,
                INSPECTOR,
                FECHA_ACTA,
                FH_LABRADO,
                DOM_HECHO,
                DOM_INFRACTOR,
                -34.5678,
                -58.1234,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                null,
                null,
                null);
        acta.setInfractorNombre(INFRACTOR_NOMBRE);
        acta.setInfractorDocumento(INFRACTOR_DOC);
        acta.setNroActa("ACT-MOCK-" + def.codigo());
        acta.setBloqueActual(def.bloqueEsperado());
        acta.setSituacionAdministrativa(def.situacionEsperada());
        acta.setResultadoFinal(def.resultadoFinalEsperado());
        return acta;
    }

    /**
     * Calcula la matriz de cobertura funcional del dataset.
     */
    public static DatasetFuncionalCoberturaResultado calcularCobertura() {
        List<String> pendientes = pendientesDocumentados();
        List<String> advertencias = advertenciasDocumentadas();
        return DatasetFuncionalCoberturaResultado.calcular(DEFINICIONES, pendientes, advertencias);
    }

    // =========================================================================
    // Pendientes documentados (casos de uso no implementados todavia)
    // =========================================================================

    private static List<String> pendientesDocumentados() {
        return List.of(
            "Slice 8C - FirmaReq completa (FalDocumentoFirmaReq): firma institucional con firmante asignado pendiente",
            "Slice 8C - Emision formal (FalDocumento.marcarEmitido): estado EMITIDO no implementado todavia",

            "Medida preventiva con flujo propio: hoy solo disponible como documento (EMITIR_MEDIDA_PREVENTIVA)",
            "Acta archivada (SituacionAdministrativa.ARCHIVADA / BloqueActual.ARCH): sin acta mock especifica",
            "Slice 6D-3+ - REINGRESO_PARA_CIERRE: bloqueado, reservado para slice futuro",
            "Slice 9 - Persistencia JDBC/MariaDB: no implementada todavia"
        );
    }

    // =========================================================================
    // Advertencias documentadas
    // =========================================================================

    private static List<String> advertenciasDocumentadas() {
        return List.of(
            "ACT-019 (GESTION_EXTERNA_PAGO_EXTERNO): resultado CONDENA_FIRME_PAGADA via PAGAPR; cierre puede diferirse por bloqueantes activos",
            "ACT-022 (ABSUELTO_CON_BLOQUEANTE): requiere flujo en dos pasos; no cerrable directamente",
            "Documentos con requiereRedaccion=true necesitan plantilla disponible en PlantillasMockSeeder para AccionDocumental correspondiente"
        );
    }
}
