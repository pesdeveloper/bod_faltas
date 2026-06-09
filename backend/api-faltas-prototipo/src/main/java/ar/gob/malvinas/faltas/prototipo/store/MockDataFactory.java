package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaTransitoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Carga un escenario demo fijo en {@link PrototipoStore}.
 */
@Component
public class MockDataFactory {

    private static final String BANDEJA_ACTAS_EN_ENRIQUECIMIENTO = "ACTAS_EN_ENRIQUECIMIENTO";
    private static final String BANDEJA_PENDIENTE_PREPARACION_DOCUMENTAL = "PENDIENTE_PREPARACION_DOCUMENTAL";
    private static final String BANDEJA_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String BANDEJA_PENDIENTE_NOTIFICACION = "PENDIENTE_NOTIFICACION";
    private static final String BANDEJA_EN_NOTIFICACION = "EN_NOTIFICACION";
    private static final String BANDEJA_PENDIENTE_ANALISIS = "PENDIENTE_ANALISIS";
    private static final String BANDEJA_PENDIENTES_RESOLUCION_REDACCION = "PENDIENTES_RESOLUCION_REDACCION";
    private static final String BANDEJA_GESTION_EXTERNA = "GESTION_EXTERNA";
    private static final String BANDEJA_ARCHIVO = "ARCHIVO";
    private static final String BANDEJA_CERRADAS = "CERRADAS";
    private static final String BANDEJA_PENDIENTES_FALLO = "PENDIENTES_FALLO";
    private static final String BANDEJA_CON_APELACION = "CON_APELACION";
    private static final String BANDEJA_PARALIZADAS = "PARALIZADAS";

    private static final String ACCION_PARALIZACION_ESPERA_DOCUMENTAL = "PARALIZACION_ESPERA_DOCUMENTAL";
    private static final String ACCION_PARALIZACION_TRAMITE_EXTERNO = "PARALIZACION_TRAMITE_EXTERNO";
    private static final String ACCION_PARALIZACION_CAUSA_ADMINISTRATIVA = "PARALIZACION_CAUSA_ADMINISTRATIVA";
    private static final String ACCION_REVISION_APELACION = "REVISION_APELACION";

    public void loadInitialData(PrototipoStore store) {
        store.clearAll();

        // --- Flujo Tránsito: hitos 1–7 + cerrada ---
        cargarActa0001(store);
        cargarActa0002(store);
        cargarActa0003(store);
        cargarActa0004(store);
        cargarActa0005(store);
        cargarActa0006(store);
        cargarActa0007(store);
        cargarActa0008(store);

        // --- Mocks con dependencias de tests ---
        cargarActa0011(store);
        cargarActa0012(store);
        cargarActa0013(store);
        cargarActa0014(store);
        cargarActa0015(store);
        cargarActa0016(store);
        cargarActa0017(store);
        cargarActa0019CerrabilidadMaterialesDemo(store);
        cargarActa0020OrigenMedidaDesdeGenerarMedidaDemo(store);
        cargarActa0021CerrabilidadMaterialesPagoConfirmadoDemo(store);
        cargarActa0024NacimientoCondicionesMaterialesPorConstatacionTempranaDemo(store);
        cargarActa0025SoloD1TrazasSinAnclasMaterialesDemo(store);
        cargarActa0026MedidaPreventivaPosteriorContravencionDemo(store);

        // --- Fallo, apelación, correo postal ---
        cargarActa0027FalloApelacionPortalDemo(store);
        cargarActa0028FalloApelacionPresencialDemo(store);
        cargarActa0029FalloVencimientoPlazoDemo(store);
        cargarActa0030ApelacionAceptadaAbsuelveDemo(store);
        cargarActasNotificadorMunicipalDemo(store);
        cargarActasPortalDomicilioElectronicoDemo(store);
        cargarActasCorreoPostalDemo(store);

        // --- Bandejas UX: fallo, apelación, paralizadas ---
        cargarActasBandejasUxDemo(store);

        // --- Nuevos mocks canónicos para hitos faltantes ---
        cargarActa0120PagoVoluntarioInformadoDemo(store);
        cargarActa0121FalloPendienteFirmaDemo(store);
        cargarActa0122CondenaFirmePagoCondenaDemo(store);
        cargarActa0123InspeccionesInicialDemo(store);
        cargarActa0124FiscalizacionInicialDemo(store);
        cargarActa0125BromatologiaInicialDemo(store);
        cargarActa0130GestionExternaApremioDemo(store);

        cargarActa0131AbsueltoArchivoReingresoContinuidadDemo(store);
        cargarActa0132CondenaFirmeConfirmadoArchivoReingresoContinuidadDemo(store);
        cargarActa0133CondenaFirmePendienteArchivoReingresoContinuidadDemo(store);
        cargarActa0134CondenaFirmeInformadoArchivoReingresoContinuidadDemo(store);

        completarDependenciasDemo(store);
    }

    private void cargarActa0001(PrototipoStore store) {
        String id = "ACTA-0001";
        String bandeja = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0001",
                "TRANSITO_URBANO",
                "D3_DOCUMENTAL",
                "PENDIENTE_PRODUCCION_PIEZAS",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 1, 10, 9, 15),
                "García, Laura",
                "DNI 28.441.992",
                "Oficial Pérez",
                "Estacionamiento prohibido en zona escolar.",
                bandeja);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);
        store.registrarPatenteVehiculo(id, "ABC123");

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0001-01",
                id,
                LocalDateTime.of(2026, 1, 10, 9, 20),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada y asignada a enriquecimiento."));
        eventos.add(new ActaEventoMock(
                "EVT-0001-02",
                id,
                LocalDateTime.of(2026, 1, 11, 14, 30),
                "ASIGNACION",
                "D2_ENRIQUECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "Operador asignado para completar datos del infractor."));
        eventos.add(new ActaEventoMock(
                "EVT-0001-03",
                id,
                LocalDateTime.of(2026, 1, 12, 11, 5),
                "ACTUALIZACION_DATOS",
                "D2_ENRIQUECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "Verificado domicilio y contacto del infractor."));
        eventos.add(new ActaEventoMock(
                "EVT-0001-04",
                id,
                LocalDateTime.of(2026, 1, 13, 9, 0),
                "PASE_BANDEJA",
                "D2_ENRIQUECIMIENTO",
                "D3_DOCUMENTAL",
                "Enriquecimiento completo; pendiente produccion de notificacion del acta."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0001-01",
                id,
                "FOTO_INFRACCION",
                "ADJUNTO",
                "foto_infraccion_0001.jpg"));
        store.getDocumentosPorActa().put(id, docs);

        store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                id,
                new ArrayList<>(List.of("NOTIFICACION_ACTA")),
                new ArrayList<>()));
    }

    private void cargarActa0002(PrototipoStore store) {
        String id = "ACTA-0002";
        String bandeja = BANDEJA_PENDIENTE_PREPARACION_DOCUMENTAL;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0002",
                "SEGURIDAD_VIAL",
                "D3_DOCUMENTAL",
                "PENDIENTE_GENERACION",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 1, 8, 16, 40),
                "Martínez, Juan Carlos",
                "DNI 32.102.554",
                "Oficial Ruiz",
                "Circular en sentido contrario en calle de mano única.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0002-01",
                id,
                LocalDateTime.of(2026, 1, 8, 16, 45),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta registrada en campo."));
        eventos.add(new ActaEventoMock(
                "EVT-0002-02",
                id,
                LocalDateTime.of(2026, 1, 9, 10, 0),
                "PASE_BANDEJA",
                "D2_ENRIQUECIMIENTO",
                "D3_DOCUMENTAL",
                "Enriquecimiento cerrado; pasa a preparación documental."));
        store.getEventosPorActa().put(id, eventos);
        store.setAccionPendiente(id, "GENERAR_BORRADOR_ACTA");
    }

    private void cargarActa0003(PrototipoStore store) {
        String id = "ACTA-0003";
        String bandeja = BANDEJA_PENDIENTE_FIRMA;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0003",
                "TRANSITO_URBANO",
                "D3_DOCUMENTAL",
                "PENDIENTE_FIRMA",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 1, 5, 11, 0),
                "López, Ana",
                "DNI 35.889.001",
                "Oficial Díaz",
                "Exceso de velocidad en avenida principal (medición radar).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0003-01",
                id,
                LocalDateTime.of(2026, 1, 5, 11, 10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta cargada con lectura de dominio."));
        eventos.add(new ActaEventoMock(
                "EVT-0003-02",
                id,
                LocalDateTime.of(2026, 1, 6, 9, 0),
                "PASE_BANDEJA",
                "D2_ENRIQUECIMIENTO",
                "D3_DOCUMENTAL",
                "Datos completos; generación de borrador acta."));
        eventos.add(new ActaEventoMock(
                "EVT-0003-03",
                id,
                LocalDateTime.of(2026, 1, 7, 15, 20),
                "DOCUMENTO_GENERADO",
                "D3_DOCUMENTAL",
                "D3_DOCUMENTAL",
                "Borrador PDF generado y listo para firma."));
        eventos.add(new ActaEventoMock(
                "EVT-0003-04",
                id,
                LocalDateTime.of(2026, 1, 8, 8, 30),
                "PENDIENTE_FIRMA",
                "D3_DOCUMENTAL",
                "D3_DOCUMENTAL",
                "En cola de firma del inspector titular."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0003-01",
                id,
                "BORRADOR_ACTA",
                "PENDIENTE_FIRMA",
                "borrador_acta_0003.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0003-02",
                id,
                "CONSTANCIA_RADAR",
                "ADJUNTO",
                "constancia_radar_0003.pdf"));
        store.getDocumentosPorActa().put(id, docs);
    }

    private void cargarActa0004(PrototipoStore store) {
        String id = "ACTA-0004";
        String bandeja = BANDEJA_PENDIENTE_NOTIFICACION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0004",
                "ESTACIONAMIENTO",
                "D4_NOTIFICACION",
                "PENDIENTE_ENVIO",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2026, 1, 3, 13, 25),
                "Fernández, Roberto",
                "DNI 29.300.112",
                "Oficial Costa",
                "Estacionamiento en cordón amarillo frente a hospital.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0004-01",
                id,
                LocalDateTime.of(2026, 1, 3, 13, 30),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta iniciada en dispositivo móvil."));
        eventos.add(new ActaEventoMock(
                "EVT-0004-02",
                id,
                LocalDateTime.of(2026, 1, 4, 10, 15),
                "FIRMA_COMPLETADA",
                "D3_DOCUMENTAL",
                "D4_NOTIFICACION",
                "Acta firmada; pasa a notificación."));
        eventos.add(new ActaEventoMock(
                "EVT-0004-03",
                id,
                LocalDateTime.of(2026, 1, 5, 16, 0),
                "PENDIENTE_NOTIFICACION",
                "D4_NOTIFICACION",
                "D4_NOTIFICACION",
                "Pendiente armado de lote de notificación postal."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0004-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_0004.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0004-01",
                id,
                "POSTAL",
                "PENDIENTE_ENVIO",
                "Roberto Fernández — domicilio constancia Río Negro 1450"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    private void cargarActa0005(PrototipoStore store) {
        String id = "ACTA-0005";
        String bandeja = BANDEJA_EN_NOTIFICACION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0005",
                "TRANSITO_URBANO",
                "D4_NOTIFICACION",
                "EN_ENVIO",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 12, 20, 8, 50),
                "Silva, Mariana",
                "DNI 31.556.778",
                "Oficial Acosta",
                "No respetar semáforo en rojo (cruce peatonal ocupado).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0005-01",
                id,
                LocalDateTime.of(2025, 12, 20, 9, 0),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta labrada en intersección Av. Libertad / Sarmiento."));
        eventos.add(new ActaEventoMock(
                "EVT-0005-02",
                id,
                LocalDateTime.of(2025, 12, 22, 12, 0),
                "FIRMA_COMPLETADA",
                "D3_DOCUMENTAL",
                "D4_NOTIFICACION",
                "Documentación formalizada."));
        eventos.add(new ActaEventoMock(
                "EVT-0005-03",
                id,
                LocalDateTime.of(2026, 1, 2, 9, 45),
                "NOTIFICACION_EN_CURSO",
                "D4_NOTIFICACION",
                "D4_NOTIFICACION",
                "Carta documento en trámite en correo oficial."));
        eventos.add(new ActaEventoMock(
                "EVT-0005-04",
                id,
                LocalDateTime.of(2026, 1, 6, 14, 10),
                "SEGUIMIENTO",
                "D4_NOTIFICACION",
                "D4_NOTIFICACION",
                "Primer intento de entrega programado para el 08/01."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0005-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_0005.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0005-01",
                id,
                "POSTAL",
                "EN_TRAMITE",
                "Mariana Silva — Av. Patagonia 220, 3º B"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    private void cargarActa0006(PrototipoStore store) {
        String id = "ACTA-0006";
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0006",
                "SEGURIDAD_VIAL",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 11, 28, 17, 5),
                "Ramos, Diego",
                "DNI 27.901.003",
                "Oficial Vega",
                "Conducir bajo efectos de alcohol (alcohotest positivo).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0006-01",
                id,
                LocalDateTime.of(2025, 11, 28, 17, 20),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Retención de licencia registrada."));
        eventos.add(new ActaEventoMock(
                "EVT-0006-02",
                id,
                LocalDateTime.of(2025, 12, 10, 11, 30),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación fehaciente; acta pasa a análisis jurídico."));
        eventos.add(new ActaEventoMock(
                "EVT-0006-03",
                id,
                LocalDateTime.of(2026, 1, 4, 9, 0),
                "ASIGNACION_ANALISTA",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Asignada a analista para dictamen preliminar."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0006-01",
                id,
                "INFORME_ALCOHOTEST",
                "ADJUNTO",
                "informe_alcotest_0006.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0006-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Diego Ramos — constancia AR 445566"));
        notifs.add(new ActaNotificacionMock(
                "NOT-0006-02",
                id,
                "EMAIL",
                "NO_APLICA",
                "Reserva — solo postal por normativa interna"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    private void cargarActa0007(PrototipoStore store) {
        String id = "ACTA-0007";
        String bandeja = BANDEJA_ARCHIVO;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0007",
                "TRANSITO_URBANO",
                "ARCHIVO",
                "ARCHIVADA_OPERATIVA",
                "ARCHIVO",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 10, 15, 12, 0),
                "Torres, Patricia",
                "DNI 34.200.901",
                "Oficial Molina",
                "Uso de celular al conducir (fotografía probatoria).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0007-01",
                id,
                LocalDateTime.of(2025, 10, 15, 12, 10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta generada desde cámara fija."));
        eventos.add(new ActaEventoMock(
                "EVT-0007-02",
                id,
                LocalDateTime.of(2025, 11, 1, 10, 0),
                "RESOLUCION",
                "D5_ANALISIS",
                "ARCHIVO",
                "Resolución administrativa de archivo por prescripción operativa."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0007-01",
                id,
                "RESOLUCION_ARCHIVO",
                "FIRMADO",
                "resolucion_archivo_0007.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0007-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Patricia Torres — notificación cumplida 20/10/2025"));
        store.getNotificacionesPorActa().put(id, notifs);

        // Archivo directo histórico por resolución administrativa; no vino
        // por evaluación de vencimiento.
        store.setMotivoArchivo(id, PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO);
    }

    private void cargarActa0008(PrototipoStore store) {
        String id = "ACTA-0008";
        String bandeja = BANDEJA_CERRADAS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0008",
                "ESTACIONAMIENTO",
                "CERRADA",
                "CERRADA",
                "CERRADA",
                true,
                false,
                true,
                true,
                LocalDateTime.of(2025, 9, 5, 7, 30),
                "Benítez, Carlos",
                "DNI 26.778.334",
                "Oficial Romero",
                "Obstrucción de rampa de accesibilidad.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0008-01",
                id,
                LocalDateTime.of(2025, 9, 5, 7, 45),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta labrada con testigos identificados."));
        eventos.add(new ActaEventoMock(
                "EVT-0008-02",
                id,
                LocalDateTime.of(2025, 10, 12, 16, 0),
                "CIERRE",
                "D5_ANALISIS",
                "CERRADA",
                "Cierre administrativo por pago y conformidad."));
        eventos.add(new ActaEventoMock(
                "EVT-0008-03",
                id,
                LocalDateTime.of(2025, 10, 12, 16, 5),
                "CIERRE_DEFINITIVO",
                "CERRADA",
                "CERRADA",
                "Acta cerrada sin posibilidad de reingreso automático."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0008-01",
                id,
                "COMPROBANTE_PAGO",
                "ADJUNTO",
                "comprobante_pago_0008.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0008-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Carlos Benítez — AR 778899"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO);
    }

    private void cargarActa0009(PrototipoStore store) {
        String id = "ACTA-0009";
        String bandeja = BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0009",
                "TRANSITO_URBANO",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 1, 14, 15, 40),
                "Acosta, Verónica",
                "DNI 33.445.120",
                "Oficial Núñez",
                "Giros indebidos en rotonda (corte de senda).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0009-01",
                id,
                LocalDateTime.of(2026, 1, 14, 15, 50),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada; falta domicilio legal del infractor."));
        eventos.add(new ActaEventoMock(
                "EVT-0009-02",
                id,
                LocalDateTime.of(2026, 1, 15, 9, 10),
                "OBSERVACION",
                "D2_ENRIQUECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "Solicitud de cédula verde para cruzar titularidad."));
        eventos.add(new ActaEventoMock(
                "EVT-0009-03",
                id,
                LocalDateTime.of(2026, 1, 16, 11, 25),
                "SEGUIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "A la espera de respuesta del registro automotor."));
        eventos.add(new ActaEventoMock(
                "EVT-0009-04",
                id,
                LocalDateTime.of(2026, 1, 17, 8, 0),
                "RECORDATORIO",
                "D2_ENRIQUECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "Recordatorio interno: vencer SLA enriquecimiento 5 días."));
        store.getEventosPorActa().put(id, eventos);
    }

    private void cargarActa0010(PrototipoStore store) {
        String id = "ACTA-0010";
        String bandeja = BANDEJA_ARCHIVO;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0010",
                "SEGURIDAD_VIAL",
                "ARCHIVO",
                "ARCHIVADA_OPERATIVA",
                "ARCHIVO",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2025, 12, 1, 10, 20),
                "Paredes, Luis",
                "DNI 30.112.889",
                "Oficial Soto",
                "Falta de cinturón de seguridad (vía rápida).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0010-01",
                id,
                LocalDateTime.of(2025, 12, 1, 10, 30),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta generada desde control móvil."));
        eventos.add(new ActaEventoMock(
                "EVT-0010-02",
                id,
                LocalDateTime.of(2025, 12, 18, 13, 0),
                "ARCHIVO_OPERATIVO",
                "D5_ANALISIS",
                "ARCHIVO",
                "Archivo por duplicidad de expediente (acta previa ACTA-2025-8891)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0010-01",
                id,
                "NOTA_INTERNA_DUPLICIDAD",
                "ADJUNTO",
                "nota_duplicidad_0010.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        // Archivo directo histórico por duplicidad; decisión tomada sin paso
        // previo por evaluación de vencimiento.
        store.setMotivoArchivo(id, PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO);
    }

    private void cargarActa0011(PrototipoStore store) {
        String id = "ACTA-0011";
        String bandeja = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0011",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_RESOLUCION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 12, 5, 10, 15),
                "Herrera, Marta",
                "DNI 28.991.445",
                "Oficial Cabrera",
                "Conducir con licencia vencida (corresponde resolución administrativa).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0011-01",
                id,
                LocalDateTime.of(2025, 12, 5, 10, 25),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta labrada en control vehicular."));
        eventos.add(new ActaEventoMock(
                "EVT-0011-02",
                id,
                LocalDateTime.of(2025, 12, 20, 9, 0),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación cumplida; pasa a análisis."));
        eventos.add(new ActaEventoMock(
                "EVT-0011-03",
                id,
                LocalDateTime.of(2026, 1, 12, 11, 30),
                "DERIVACION_RESOLUCION",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Analista deriva a redacción de resolución; pieza no-fallo aún no producida."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0011-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_0011.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0011-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Marta Herrera — AR 334455"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                id,
                new ArrayList<>(List.of("RESOLUCION")),
                new ArrayList<>()));
    }

    private void cargarActa0012(PrototipoStore store) {
        String id = "ACTA-0012";
        String bandeja = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0012",
                "SEGURIDAD_VIAL",
                "D5_ANALISIS",
                "PENDIENTE_NULIDAD",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 11, 18, 14, 50),
                "Morales, Hernán",
                "DNI 31.004.776",
                "Oficial Pereyra",
                "Acta con vicio formal detectado; corresponde redactar nulidad.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0012-01",
                id,
                LocalDateTime.of(2025, 11, 18, 15, 0),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada desde control móvil."));
        eventos.add(new ActaEventoMock(
                "EVT-0012-02",
                id,
                LocalDateTime.of(2025, 12, 10, 10, 20),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación fehaciente registrada."));
        eventos.add(new ActaEventoMock(
                "EVT-0012-03",
                id,
                LocalDateTime.of(2026, 1, 8, 16, 45),
                "DERIVACION_NULIDAD",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Analista detecta vicio formal; deriva a redacción de nulidad."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0012-01",
                id,
                "INFORME_VICIO_FORMAL",
                "ADJUNTO",
                "informe_vicio_0012.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0012-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Hernán Morales — AR 556677"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                id,
                new ArrayList<>(List.of("NULIDAD")),
                new ArrayList<>()));
    }

    private void cargarActa0013(PrototipoStore store) {
        String id = "ACTA-0013";
        String bandeja = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0013",
                "SEGURIDAD_VIAL",
                "D5_ANALISIS",
                "PENDIENTE_MEDIDA_PREVENTIVA",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 1, 6, 22, 10),
                "Quiroga, Sergio",
                "DNI 26.884.230",
                "Oficial Godoy",
                "Retención preventiva de vehículo; corresponde generar medida preventiva.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0013-01",
                id,
                LocalDateTime.of(2026, 1, 6, 22, 20),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada con retención preventiva del vehículo."));
        eventos.add(new ActaEventoMock(
                "EVT-0013-02",
                id,
                LocalDateTime.of(2026, 1, 7, 9, 30),
                "OBSERVACION",
                "D2_ENRIQUECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "Revisión inicial identifica necesidad de medida preventiva formal."));
        eventos.add(new ActaEventoMock(
                "EVT-0013-03",
                id,
                LocalDateTime.of(2026, 1, 8, 11, 0),
                "DERIVACION_MEDIDA_PREVENTIVA",
                "D2_ENRIQUECIMIENTO",
                "D5_ANALISIS",
                "Expediente derivado a redacción de medida preventiva; pieza no-fallo aún no producida."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0013-01",
                id,
                "ACTA_RETENCION",
                "ADJUNTO",
                "acta_retencion_0013.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        // Caso demo con múltiples piezas requeridas: el acta debe permanecer en
        // PENDIENTES_RESOLUCION_REDACCION hasta que NOTIFICACION_ACTA y
        // MEDIDA_PREVENTIVA estén ambas producidas.
        store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                id,
                new ArrayList<>(List.of("NOTIFICACION_ACTA", "MEDIDA_PREVENTIVA")),
                new ArrayList<>()));
    }

    private void cargarActa0014(PrototipoStore store) {
        String id = "ACTA-0014";
        String bandeja = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0014",
                "ESTACIONAMIENTO",
                "D5_ANALISIS",
                "PENDIENTE_RECTIFICACION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 12, 22, 8, 40),
                "Ibarra, Lucía",
                "DNI 33.770.118",
                "Oficial Maldonado",
                "Error material en datos del infractor; corresponde rectificación.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0014-01",
                id,
                LocalDateTime.of(2025, 12, 22, 8, 50),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta cargada desde dispositivo móvil."));
        eventos.add(new ActaEventoMock(
                "EVT-0014-02",
                id,
                LocalDateTime.of(2026, 1, 5, 12, 15),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación entregada; pasa a análisis."));
        eventos.add(new ActaEventoMock(
                "EVT-0014-03",
                id,
                LocalDateTime.of(2026, 1, 14, 10, 5),
                "DERIVACION_RECTIFICACION",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Analista detecta error material; deriva a redacción de rectificación."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0014-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_0014.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0014-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Lucía Ibarra — AR 889900"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                id,
                new ArrayList<>(List.of("RECTIFICACION")),
                new ArrayList<>()));
    }

    /**
     * Caso demo "listo para derivación a gestión externa": el expediente ya
     * atravesó fallo + notificación de fallo y la ventana de espera posterior
     * (5 días por regla especial de notificación de fallo, ver
     * {@code spec/02-reglas-transversales/02-reglas-de-notificacion.md}) se
     * cumplió sin novedad. Queda en análisis pero marcado con
     * {@link PrototipoStore#ACCION_DERIVAR_GESTION_EXTERNA}. La derivación
     * efectiva a la bandeja {@code GESTION_EXTERNA} todavía no está modelada
     * en el prototipo: en este slice sólo se expone la condición.
     */
    private void cargarActa0015(PrototipoStore store) {
        String id = "ACTA-0015";
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0015",
                "SEGURIDAD_VIAL",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 10, 2, 9, 0),
                "Vargas, Elena",
                "DNI 29.884.551",
                "Oficial Luna",
                "Exceso de velocidad en ruta nacional; fallo firme sin novedad posterior.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0015-01",
                id,
                LocalDateTime.of(2025, 10, 2, 9, 15),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada desde control de ruta."));
        eventos.add(new ActaEventoMock(
                "EVT-0015-02",
                id,
                LocalDateTime.of(2025, 11, 10, 10, 30),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación fehaciente del acta; pasa a análisis."));
        eventos.add(new ActaEventoMock(
                "EVT-0015-03",
                id,
                LocalDateTime.of(2025, 12, 5, 11, 0),
                "FALLO_EMITIDO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Fallo dictado por autoridad competente."));
        eventos.add(new ActaEventoMock(
                "EVT-0015-04",
                id,
                LocalDateTime.of(2025, 12, 12, 15, 45),
                "NOTIFICACION_FALLO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Fallo notificado al infractor; inicia ventana de espera de 5 días."));
        eventos.add(new ActaEventoMock(
                "EVT-0015-05",
                id,
                LocalDateTime.of(2025, 12, 18, 9, 0),
                "SEGUIMIENTO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Ventana de espera post notificación de fallo cumplida sin novedad; "
                        + "caso en condición de derivación a gestión externa."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0015-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_0015.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0015-02",
                id,
                "FALLO",
                "FIRMADO",
                "fallo_0015.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0015-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Elena Vargas — AR 991122 (notificación del acta)"));
        notifs.add(new ActaNotificacionMock(
                "NOT-0015-02",
                id,
                "POSTAL",
                "ENTREGADA",
                "Elena Vargas — AR 991133 (notificación del fallo)"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.setAccionPendiente(id, PrototipoStore.ACCION_DERIVAR_GESTION_EXTERNA);
    }

    /**
     * Caso demo "no derivable todavía": expediente post fallo, con fallo
     * notificado recientemente, cuya ventana de espera de 5 días todavía no
     * se cumplió. Aunque comparte el mismo bloque D5_ANALISIS que el caso
     * anterior, NO debe marcarse para derivación a gestión externa. Permite
     * comprobar que el filtro por
     * {@link PrototipoStore#ACCION_DERIVAR_GESTION_EXTERNA} diferencia este
     * caso del "listo para derivación".
     */
    private void cargarActa0016(PrototipoStore store) {
        String id = "ACTA-0016";
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0016",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 11, 20, 10, 0),
                "Duarte, Sofía",
                "DNI 35.112.007",
                "Oficial Herrera",
                "Circulación sin seguro obligatorio; fallo notificado recientemente.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0016-01",
                id,
                LocalDateTime.of(2025, 11, 20, 10, 10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada desde control vehicular."));
        eventos.add(new ActaEventoMock(
                "EVT-0016-02",
                id,
                LocalDateTime.of(2025, 12, 15, 11, 0),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación del acta entregada; pasa a análisis."));
        eventos.add(new ActaEventoMock(
                "EVT-0016-03",
                id,
                LocalDateTime.of(2026, 1, 10, 9, 30),
                "FALLO_EMITIDO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Fallo dictado por autoridad competente."));
        eventos.add(new ActaEventoMock(
                "EVT-0016-04",
                id,
                LocalDateTime.of(2026, 1, 16, 14, 20),
                "NOTIFICACION_FALLO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Fallo notificado al infractor; ventana de espera de 5 días en curso."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0016-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_0016.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0016-02",
                id,
                "FALLO",
                "FIRMADO",
                "fallo_0016.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0016-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Sofía Duarte — AR 223344 (notificación del acta)"));
        notifs.add(new ActaNotificacionMock(
                "NOT-0016-02",
                id,
                "POSTAL",
                "ENTREGADA",
                "Sofía Duarte — AR 223355 (notificación del fallo)"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    /**
     * Caso demo "ya derivado a Juzgado de Paz": el expediente atravesó fallo
     * + notificación de fallo + ventana de espera posterior cumplida sin
     * novedad, y fue efectivamente derivado a la macro-bandeja
     * {@code GESTION_EXTERNA} con tipo
     * {@link PrototipoStore#TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ}. Permite
     * ver desde el reset un caso ya presente en la bandeja de gestión
     * externa, distinguible por tipo del caso que se deriva a APREMIO vía
     * la acción dedicada sobre ACTA-0015. Queda con
     * {@code permiteReingreso = true} para habilitar el retorno efectivo
     * en un slice posterior.
     */
    private void cargarActa0017(PrototipoStore store) {
        String id = "ACTA-0017";
        String bandeja = BANDEJA_GESTION_EXTERNA;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0017",
                "TRANSITO_URBANO",
                "GESTION_EXTERNA",
                "EN_GESTION_EXTERNA",
                "GESTION_EXTERNA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2025, 8, 18, 10, 30),
                "Ocampo, Ricardo",
                "DNI 27.115.443",
                "Oficial Villalba",
                "Conducir con VTV vencida; fallo firme derivado a Juzgado de Paz.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0017-01",
                id,
                LocalDateTime.of(2025, 8, 18, 10, 45),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada desde control vehicular."));
        eventos.add(new ActaEventoMock(
                "EVT-0017-02",
                id,
                LocalDateTime.of(2025, 9, 25, 11, 15),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación fehaciente del acta; pasa a análisis."));
        eventos.add(new ActaEventoMock(
                "EVT-0017-03",
                id,
                LocalDateTime.of(2025, 10, 20, 9, 30),
                "FALLO_EMITIDO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Fallo dictado por autoridad competente."));
        eventos.add(new ActaEventoMock(
                "EVT-0017-04",
                id,
                LocalDateTime.of(2025, 10, 28, 14, 0),
                "NOTIFICACION_FALLO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Fallo notificado al infractor; inicia ventana de espera de 5 días."));
        eventos.add(new ActaEventoMock(
                "EVT-0017-05",
                id,
                LocalDateTime.of(2025, 11, 5, 9, 0),
                "SEGUIMIENTO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Ventana de espera post notificación de fallo cumplida sin novedad; "
                        + "caso en condición de derivación a gestión externa."));
        eventos.add(new ActaEventoMock(
                "EVT-0017-06",
                id,
                LocalDateTime.of(2025, 11, 6, 10, 30),
                "DERIVACION_GESTION_EXTERNA",
                "D5_ANALISIS",
                "GESTION_EXTERNA",
                "Análisis jurídico deriva efectivamente el caso a gestión externa; tipo "
                        + PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ + "."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0017-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_0017.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0017-02",
                id,
                "FALLO_CONDENATORIO",
                "FIRMADO",
                "fallo_condenatorio_0017.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0017-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Ricardo Ocampo — AR 667788 (notificación del acta)"));
        notifs.add(new ActaNotificacionMock(
                "NOT-0017-02",
                id,
                "POSTAL",
                "ENTREGADA",
                "Ricardo Ocampo — AR 667799 (notificación del fallo)"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.setTipoGestionExterna(id, PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ);
        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        store.setMontoCondenaDemo(id, java.math.BigDecimal.valueOf(75000));
    }

    /**
     * Caso demo canónico para el slice de gestión externa diferenciada:
     * acta en {@code GESTION_EXTERNA} con tipo APREMIO, condena firme y monto
     * informado. Permite probar las acciones diferenciadas de APREMIO
     * (reingreso sin pago y pago en apremio) sin depender de una derivación
     * efectiva en el mismo test.
     */
    private void cargarActa0130GestionExternaApremioDemo(PrototipoStore store) {
        String id = "ACTA-0130";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0130",
                "TRANSITO_URBANO",
                "GESTION_EXTERNA",
                "EN_GESTION_EXTERNA",
                "GESTION_EXTERNA",
                false,
                true,
                true,
                true,
                java.time.LocalDateTime.of(2026, 1, 10, 9, 0),
                "Torres, Marcelo",
                "DNI 38.001.130",
                "Oficial Demo",
                "Caso demo ACTA-0130: condena firme derivada a Apremio; monto 75000.",
                BANDEJA_GESTION_EXTERNA);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0130-01", id,
                java.time.LocalDateTime.of(2026, 1, 10, 9, 10),
                "ALTA", "D1_CAPTURA", "D2_ENRIQUECIMIENTO",
                "Acta ingresada."));
        eventos.add(new ActaEventoMock(
                "EVT-0130-02", id,
                java.time.LocalDateTime.of(2026, 1, 18, 11, 0),
                "NOTIFICACION_ENTREGADA", "D4_NOTIFICACION", "D5_ANALISIS",
                "Notificación fehaciente."));
        eventos.add(new ActaEventoMock(
                "EVT-0130-03", id,
                java.time.LocalDateTime.of(2026, 2, 5, 10, 0),
                "FALLO_CONDENATORIO_DICTADO", "D5_ANALISIS", "D5_ANALISIS",
                "Fallo condenatorio dictado."));
        eventos.add(new ActaEventoMock(
                "EVT-0130-04", id,
                java.time.LocalDateTime.of(2026, 2, 10, 14, 0),
                "CONDENA_FIRME", "D5_ANALISIS", "D5_ANALISIS",
                "Condena firme establecida; plazo de apelación vencido."));
        eventos.add(new ActaEventoMock(
                "EVT-0130-05", id,
                java.time.LocalDateTime.of(2026, 2, 12, 9, 0),
                "DERIVACION_GESTION_EXTERNA", "D5_ANALISIS", "GESTION_EXTERNA",
                "Derivado a gestión externa tipo "
                        + PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO + "."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0130-01", id, "ACTA_FIRMADA", "FIRMADO", "acta_firmada_0130.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0130-02", id, "FALLO_CONDENATORIO", "FIRMADO",
                "fallo_condenatorio_0130.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0130-01", id, "POSTAL", "ENTREGADA",
                "Marcelo Torres — notificación del acta"));
        notifs.add(new ActaNotificacionMock(
                "NOT-0130-02", id, "POSTAL", "ENTREGADA",
                "Marcelo Torres — notificación del fallo"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.setTipoGestionExterna(id, PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO);
        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        store.setMontoCondenaDemo(id, java.math.BigDecimal.valueOf(75000));
    }

    /**
     * Caso demo del slice "pago informado + pendiente de confirmación +
     * confirmación/observación mock". Parte en enriquecimiento para poder
     * registrar solicitud de pago voluntario temprana y luego simular el pago
     * informado + adjunto de comprobante + confirmación por API sin depender
     * de integraciones externas. Sin anclas de bloqueo material: para
     * {@code PAGO_CONFIRMADO} + bloqueos + cierre, ver {@code ACTA-0022}.
     */
    private void cargarActa0018PagoVoluntarioDemo(PrototipoStore store) {
        String id = "ACTA-0018";
        String bandeja = BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0018",
                "TRANSITO_URBANO",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 2, 3, 10, 10),
                "Demo Pago Voluntario",
                "DNI 11.111.111",
                "Oficial Demo",
                "Caso demo: pago voluntario informado y confirmación mock.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0018-01",
                id,
                LocalDateTime.of(2026, 2, 3, 10, 15),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta demo creada para circuito de pago voluntario."));
        store.getEventosPorActa().put(id, eventos);

        // Situación de pago inicial: SIN_PAGO (default). Se recorre por API.
    }

    /**
     * Caso demo principal (cerrabilidad + rama de absolución): {@code ACTA-0019},
     * subtipo/caso {@code TRANSITO_URBANO} con estado
     * {@code PENDIENTE_CIERRE_MATERIAL} en análisis, {@code resultadoFinal} mock
     * {@code ABSUELTO}. En tránsito, RODADO y DOCUMENTACION son ejes materiales
     * propios; el secuestro/retiro de rodado y la retención de documentación no
     * son medidas preventivas. No existe {@code LEVANTAMIENTO_MEDIDA_PREVENTIVA}:
     * {@code MEDIDA_PREVENTIVA} queda {@code NO_APLICA}. Solo dos orígenes activos:
     * {@link PrototipoStore#reconocerOrigenBloqueanteSecuestroRodado} y
     * {@link PrototipoStore#reconocerOrigenBloqueanteRetencionDocumental}. La rama
     * simétrica con {@code PAGO_CONFIRMADO} precargado es {@code ACTA-0021};
     * el mismo criterio con {@code PAGO_CONFIRMADO} viniendo del flujo de
     * pago informado + confirmación es {@code ACTA-0022}. Ver {@code ACTA-0020}
     * para el reconocimiento de medida nacido junto a
     * {@code generarMedidaPreventiva} (misma ancla {@code MEDIDA_PREVENTIVA}).
     * El circuito sólo pago voluntario / informado sin material de cierre
     * sigue en {@code ACTA-0018}.
     */
    private void cargarActa0019CerrabilidadMaterialesDemo(PrototipoStore store) {
        String id = "ACTA-0019";
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0019",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_CIERRE_MATERIAL",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 3, 1, 9, 0),
                "Demo Cerrabilidad",
                "DNI 22.222.222",
                "Oficial Demo",
                "Caso demo ACTA-0019: absolución; rodado y documentación (tránsito — ejes materiales propios; sin medida preventiva).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0019-01",
                id,
                LocalDateTime.of(2026, 3, 1, 9, 30),
                "ALTA_DEMO_CERRABILIDAD",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Precarga demo: resultado ABSUELTO. Dos orígenes vía reconocimiento (rodado + documentación); sin medida preventiva."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0019-01",
                        id,
                        "ACTA_RETENCION",
                        "ADJUNTO",
                        "acta_retencion_vehiculo_0019.pdf"));
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0019-02",
                        id,
                        "CONSTATACION_RETENCION_DOCUMENTACION",
                        "ADJUNTO",
                        "constatacion_retencion_doc_0019.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);

        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado rRodado =
                store.reconocerOrigenBloqueanteSecuestroRodado(id);
        if (rRodado.estado() != PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.OK) {
            throw new IllegalStateException(
                    "Carga demo ACTA-0019: reconocer secuestro rodado, esperado OK, obtuvo: " + rRodado.estado());
        }
        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado rDoc =
                store.reconocerOrigenBloqueanteRetencionDocumental(id);
        if (rDoc.estado() != PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.OK) {
            throw new IllegalStateException(
                    "Carga demo ACTA-0019: reconocer retención documental, esperado OK, obtuvo: " + rDoc.estado());
        }
    }

    /**
     * Demostración mínima: al producir medida preventiva con
     * {@code generarMedidaPreventiva} se reconoce el origen
     * {@code MEDIDA_PREVENTIVA_ACTIVA} (ancla {@code MEDIDA_PREVENTIVA} en
     * expediente), con el mismo tronco que
     * {@code reconocerOrigenBloqueanteMedidaPreventiva}. La acta queda en
     * resolución / firma según el tramo del prototipo; no es el caso e2e de
     * {@code ACTA-0019}, solo prueba el enganche con el circuito de piezas.
     */
    private void cargarActa0020OrigenMedidaDesdeGenerarMedidaDemo(PrototipoStore store) {
        String id = "ACTA-0020";
        String bandeja = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0020",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_PRODUCCION_PIEZAS",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 3, 2, 10, 0),
                "Origen medida (demo)",
                "DNI 11.111.111",
                "Oficial Demo",
                "Caso auxiliar: medida requerida; la generación de la pieza aporta el origen de medida activa al modelo de cierre.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(
                new ActaEventoMock(
                        "EVT-0020-01",
                        id,
                        LocalDateTime.of(2026, 3, 2, 10, 5),
                        "ALTA_DEMO_MEDIDA",
                        "D5_ANALISIS",
                        "D5_ANALISIS",
                        "Acta mínima para invocar generarMedidaPreventiva al cargar datos."));
        store.getEventosPorActa().put(id, eventos);

        store.getPiezasRequeridasPorActa()
                .put(
                        id,
                        new ActaPiezasRequeridasMock(
                                id, new ArrayList<>(List.of("MEDIDA_PREVENTIVA")), new ArrayList<>()));

        PrototipoStore.GenerarMedidaPreventivaResultado r = store.generarMedidaPreventiva(id);
        if (r.estado() != PrototipoStore.GenerarMedidaPreventivaEstado.OK) {
            throw new IllegalStateException(
                    "Carga demo ACTA-0020: se esperaba generarMedidaPreventiva OK, obtuvo: " + r.estado());
        }
    }

    /**
     * Caso demo explícito (cerrabilidad + rama de pago confirmado): {@code ACTA-0021},
     * tránsito urbano con {@code resultadoFinal} mock {@code PAGO_CONFIRMADO} precargado.
     * Anclas: {@code ACTA_RETENCION} y {@code CONSTATACION_RETENCION_DOCUMENTACION};
     * pendientes: {@code LIBERACION_RODADO} y {@code ENTREGA_DOCUMENTACION}.
     * Tránsito puede tener medidas preventivas excepcionales si están justificadas por
     * ordenanza/artículo; este mock no representa ese caso — la retención de rodado
     * y documentación no son medidas preventivas. El recorrido integrado con pago
     * real es {@code ACTA-0022}; el circuito de levantamiento de medida preventiva
     * (CFN) se testea con {@code ACTA-0022} que sí tiene ancla {@code MEDIDA_PREVENTIVA}.
     */
    private void cargarActa0021CerrabilidadMaterialesPagoConfirmadoDemo(PrototipoStore store) {
        String id = "ACTA-0021";
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0021",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_CIERRE_MATERIAL",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 3, 2, 9, 0),
                "Demo Pago confirmado (cerrabilidad)",
                "DNI 33.333.333",
                "Oficial Demo",
                "Caso demo ACTA-0021: pago confirmado; rodado retenido y documentación retenida (anclas + reconocimiento; mock). "
                        + "Tránsito puede tener medidas preventivas excepcionales si están justificadas por ordenanza/artículo; "
                        + "este mock no representa ese caso.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0021-01",
                id,
                LocalDateTime.of(2026, 3, 2, 9, 30),
                "ALTA_DEMO_CERRABILIDAD_PAGO_CONFIRMADO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Precarga demo: resultado PAGO_CONFIRMADO. Dos orígenes vía reconocimiento: rodado retenido y documentación retenida (mock)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0021-01",
                        id,
                        "ACTA_RETENCION",
                        "ADJUNTO",
                        "acta_retencion_vehiculo_0021.pdf"));
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0021-02",
                        id,
                        "CONSTATACION_RETENCION_DOCUMENTACION",
                        "ADJUNTO",
                        "constatacion_retencion_doc_0021.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO);
        store.setSituacionPago(id, PrototipoStore.SituacionPagoMock.CONFIRMADO);
        store.setMontoPagoVoluntario(id, new BigDecimal("8750.00"));

        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado rRodado =
                store.reconocerOrigenBloqueanteSecuestroRodado(id);
        if (rRodado.estado() != PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.OK) {
            throw new IllegalStateException(
                    "Carga demo ACTA-0021: reconocer secuestro rodado, esperado OK, obtuvo: " + rRodado.estado());
        }
        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado rDoc =
                store.reconocerOrigenBloqueanteRetencionDocumental(id);
        if (rDoc.estado() != PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.OK) {
            throw new IllegalStateException(
                    "Carga demo ACTA-0021: reconocer retención documental, esperado OK, obtuvo: " + rDoc.estado());
        }
    }

    /**
     * Caso demo e2e: {@code ACTA-0022} — tránsito urbano con tres anclas:
     * {@code MEDIDA_PREVENTIVA}, {@code ACTA_RETENCION} y
     * {@code CONSTATACION_RETENCION_DOCUMENTACION}. A diferencia de
     * {@link #cargarActa0021CerrabilidadMaterialesPagoConfirmadoDemo(PrototipoStore)}
     * (que solo tiene retención de rodado y documentación, sin medida preventiva),
     * {@code ACTA-0022} incluye el ancla {@code MEDIDA_PREVENTIVA} con justificación
     * de campo (evento {@code MEDIDA_PREVENTIVA_APLICABLE}); es el mock de referencia
     * para testear el circuito de levantamiento (CFN). Partida en
     * {@code ACTAS_EN_ENRIQUECIMIENTO} como {@link #cargarActa0018PagoVoluntarioDemo(PrototipoStore)}.
     * No fija {@code resultadoFinal} ni invoca reconocimientos: el
     * {@code PAGO_CONFIRMADO} nace solo al confirmar el pago informado, y
     * entra a la misma {@link CerrabilidadSupport} que el resto. Las
     * condiciones materiales iniciales se expresan con las anclas de
     * expediente (misma fuente de verdad que el origen y que los
     * bloqueos en análisis; sin segundo modelo); el POST de
     * reconocimiento es opcional (idempotente).
     * Secuencia API sugerida: {@code registrar-solicitud-pago-voluntario} →
     * {@code registrar-pago-informado} → {@code adjuntar-comprobante-pago-informado}
     * → {@code confirmar-pago-informado} (queda {@code PAGO_CONFIRMADO} y no
     * cerrable mientras haya bloqueos) → por cada pendiente
     * {@code registrar-resolucion-bloqueo-cierre} y
     * {@code registrar-cumplimiento-material-bloqueo-cierre} →
     * {@code cerrar-acta}.
     */
    private void cargarActa0022PagoRealYCerrabilidadMaterialDemo(PrototipoStore store) {
        String id = "ACTA-0022";
        String bandeja = BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0022",
                "TRANSITO_URBANO",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 3, 4, 9, 0),
                "Demo pago real + cerrabilidad (0022)",
                "DNI 44.444.444",
                "Oficial Demo",
                "Caso en campo: rodado bajo retención, licencia/cedula retenida y medida preventiva aplicable; "
                        + "sigue por pago y cierre material vía API sin precargar resultado final.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0022-01",
                id,
                LocalDateTime.of(2026, 3, 4, 8, 50),
                "ALTA",
                "D1_CAPTURA",
                "D1_CAPTURA",
                "Alta: infracción y acta; inspección en sitio (mock)."));
        eventos.add(new ActaEventoMock(
                "EVT-0022-02",
                id,
                LocalDateTime.of(2026, 3, 4, 8, 52),
                "CONSTATACION_RODADO",
                "D1_CAPTURA",
                "D1_CAPTURA",
                "Acta/constatación: rodado bajo retención o secuestro; refuerza ancla de expediente (mock)."));
        eventos.add(new ActaEventoMock(
                "EVT-0022-03",
                id,
                LocalDateTime.of(2026, 3, 4, 8, 53),
                "CONSTATACION_DOCUMENTACION",
                "D1_CAPTURA",
                "D1_CAPTURA",
                "Documentación retenida en la intervención; constatación vinculada a expediente (mock)."));
        eventos.add(new ActaEventoMock(
                "EVT-0022-04",
                id,
                LocalDateTime.of(2026, 3, 4, 8, 54),
                "MEDIDA_PREVENTIVA_APLICABLE",
                "D1_CAPTURA",
                "D1_CAPTURA",
                "Situación de medida preventiva aplicable según criterio de campo; ancla al circuito (mock)."));
        eventos.add(new ActaEventoMock(
                "EVT-0022-05",
                id,
                LocalDateTime.of(2026, 3, 4, 9, 15),
                "PASE_DEMO",
                "D2_ENRIQUECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "Precarga: anclas en expediente; PAGO_CONFIRMADO y etapa de cierre vía API."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0022-00",
                        id,
                        "MEDIDA_PREVENTIVA",
                        "PENDIENTE_FIRMA",
                        "medida_preventiva_0022.pdf"));
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0022-01",
                        id,
                        "ACTA_RETENCION",
                        "ADJUNTO",
                        "acta_retencion_vehiculo_0022.pdf"));
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0022-02",
                        id,
                        "CONSTATACION_RETENCION_DOCUMENTACION",
                        "ADJUNTO",
                        "constatacion_retencion_doc_0022.pdf"));
        store.getDocumentosPorActa().put(id, docs);
    }

    /**
     * Caso mínimo de separación expediente / hecho material: {@code ACTA-0023}.
     * <ol>
     *   <li>Origen material: ancla {@code MEDIDA_PREVENTIVA} en el expediente (sincroniza
     *       el origen vía {@link CerrabilidadSupport}).</li>
     *   <li>Resultado: {@code ABSUELTO} (cerrabilidad requiere además cierre de bloqueos).</li>
     *   <li>Documento resolutorio {@code DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA} ya
     *       en expediente: el bloqueo de cierre sigue activo (no basta el PDF).</li>
     *   <li>En API detalle, {@code hechosMateriales} muestra
     *       {@code RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL} en el eje medida;
     *       {@code cerrable} es {@code false}.</li>
     *   <li>Tras {@code POST registrar-cumplimiento-material-bloqueo-cierre} con
     *       {@code LEVANTAMIENTO_MEDIDA_PREVENTIVA}, el eje pasa a
     *       {@code CUMPLIMIENTO_MATERIAL_VERIFICADO} y, sin otros bloqueos, el
     *       expediente queda en condición de cierre (misma lógica que
     *       {@link #cargarActa0019CerrabilidadMaterialesDemo(PrototipoStore)} con
     *       resolución explícita por API).</li>
     * </ol>
     */
    private void cargarActa0023HechoMaterialVsDocumentoResolutorioDemo(PrototipoStore store) {
        String id = "ACTA-0023";
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0023",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_CIERRE_MATERIAL",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 3, 5, 9, 0),
                "Demo hecho material vs. documento (0023)",
                "DNI 55.555.555",
                "Oficial Demo",
                "Caso ACTA-0023: ancla + resolutorio de medida en expediente; falta hecho material para cerrar.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0023-01",
                id,
                LocalDateTime.of(2026, 3, 5, 9, 20),
                "ALTA_DEMO_HECHO_MATERIAL",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Demo: ancla y levantamiento documental precargados; cierre operativo requiere capa material."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0023-00",
                        id,
                        "MEDIDA_PREVENTIVA",
                        "PENDIENTE_FIRMA",
                        "medida_preventiva_0023.pdf"));
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0023-01",
                        id,
                        "DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA",
                        "EMITIDO",
                        "levantamiento_medida_0023.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);
    }

    /**
     * <p>Recorrido demo e2e (tránsito + pago + cierre): {@code ACTA-0024}.
     *
     * <p>Modelo principal: retención de rodado y documentación nacen como
     * <b>datos de tránsito</b> (satélite
     * {@link ar.gob.malvinas.faltas.prototipo.domain.ActaTransitoMock} + anclas
     * en expediente coherentes con
     * {@link CerrabilidadSupport#ensureOrigenesSincronizados}. No replican en
     * el producto un “botón de constatación” del operador: el
     * {@code POST /acciones/registrar-constatacion-material-temprana} del
     * prototipo es mutación demo/técnica y pruebas (duplicado, D1/D2), no el
     * camino conceptual de este acta.
     *
     * <p>{@code medidaPreventivaAplicable = false} (Slice 24A). En tránsito puede
     * existir medida preventiva real por ordenanza/artículo; lo que no corresponde
     * es usar {@code MEDIDA_PREVENTIVA} para representar automáticamente la
     * retención de rodado/documentación. Dos bloqueantes de cierre:
     * {@code LIBERACION_RODADO} y {@code ENTREGA_DOCUMENTACION}.
     * El eje {@code MEDIDA_PREVENTIVA} queda {@code NO_APLICA}.
     *
     * <p>Base de URL: {@code /api/prototipo}. Antes de la demo: {@code POST /reset}.
     *
     * <p>Secuencia (tras reset: condiciones de material ya visibles en
     * {@code GET /actas/ACTA-0024}):
     *
     * <ol>
     *   <li><b>Transición a análisis.</b>
     *       {@code POST /actas/ACTA-0024/acciones/registrar-solicitud-pago-voluntario}.</li>
     *   <li><b>Pago informado + comprobante + confirmación.</b>
     *       {@code POST .../registrar-pago-informado} &rarr;
     *       {@code POST .../adjuntar-comprobante-pago-informado} &rarr;
     *       {@code POST .../confirmar-pago-informado}.</li>
     *   <li><b>Resolutorios + cumplimiento + cierre explícito</b> (igual que
     *       en la referencia de pago y material, ver
     *       {@link #cargarActa0022PagoRealYCerrabilidadMaterialDemo(PrototipoStore)}).</li>
     * </ol>
     */
    private void cargarActa0024NacimientoCondicionesMaterialesPorConstatacionTempranaDemo(PrototipoStore store) {
        String id = "ACTA-0024";
        String bandeja = BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
        ActaTransitoMock tr = new ActaTransitoMock(true, true, true, false);
        store.putActaTransitoMock(id, tr);
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0024",
                "TRANSITO_URBANO",
                "D1_CAPTURA",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 3, 6, 8, 30),
                "Demo constatación material temprana (0024)",
                "DNI 66.666.666",
                "Oficial Demo",
                "Caso ACTA-0024: tránsito con retención de rodado y documentación (flags mock + anclas al nacimiento; sin medida preventiva artificial).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0024-01",
                id,
                LocalDateTime.of(2026, 3, 6, 8, 32),
                "ALTA",
                "D1_CAPTURA",
                "D1_CAPTURA",
                "Alta en sitio: condiciones de tránsito y anclas de material coherente con dato (mock)."));
        store.getEventosPorActa().put(id, eventos);

        // Mismas anclas que generaría el POST de constatación temprana (proyección desde flags)
        String n = "0024";
        List<ActaDocumentoMock> docs = new ArrayList<>();
        if (tr.rodadoRetenidoOSecuestrado()) {
            docs.add(
                    new ActaDocumentoMock(
                            "DOC-" + n + "-01",
                            id,
                            PrototipoConstantes.TIPO_DOC_ACUSE_RETENCION_VEHICULO,
                            PrototipoConstantes.ESTADO_DOC_ADJUNTO,
                            "acta_retencion_vehiculo_" + n + ".pdf"));
        }
        if (tr.documentacionRetenida()) {
            docs.add(
                    new ActaDocumentoMock(
                            "DOC-" + n + "-02",
                            id,
                            PrototipoConstantes.TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL,
                            PrototipoConstantes.ESTADO_DOC_ADJUNTO,
                            "constatacion_retencion_doc_" + n + ".pdf"));
        }
        if (tr.medidaPreventivaAplicable()) {
            docs.add(
                    new ActaDocumentoMock(
                            "DOC-" + n + "-03",
                            id,
                            PrototipoConstantes.TIPO_ANCLA_MEDIDA_PREVENTIVA,
                            PrototipoConstantes.ESTADO_DOC_EMITIDO,
                            "constatacion_medida_preventiva_" + n + ".pdf"));
        }
        store.getDocumentosPorActa().put(id, docs);
        store.setAccionPendiente(id, "COMPLETAR_ENRIQUECIMIENTO");
    }

    /**
     * D1/D2, trazas mínimas, sin anclas de material. Solo para
     * {@code ConstatacionMaterialTempranaEtapaIT} y pruebas del
     * {@code POST /acciones/registrar-constatacion-material-temprana}
     * (herramienta demo, no el modelo principal de 0024).
     */
    private void cargarActa0025SoloD1TrazasSinAnclasMaterialesDemo(PrototipoStore store) {
        String id = "ACTA-0025";
        String bandeja = BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0025",
                "TRANSITO_URBANO",
                "D1_CAPTURA",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 3, 6, 8, 30),
                "Caso 0025 constatación API (regresión)",
                "DNI 77.777.777",
                "Oficial Demo",
                "ACTA-0025: solo para POST constatación temprana; sin datos de tránsito material al nacimiento.",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0025-01",
                id,
                LocalDateTime.of(2026, 3, 6, 8, 32),
                "ALTA",
                "D1_CAPTURA",
                "D1_CAPTURA",
                "Alta en sitio: expediente sin anclas de material (mock, regresión API)."));
        store.getEventosPorActa().put(id, eventos);

        store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                id,
                new ArrayList<>(List.of("NOTIFICACION_ACTA")),
                new ArrayList<>()));
    }

    /**
     * ACTA-0026: contravención en local/comercio, sin tránsito, sin ancla
     * {@code MEDIDA_PREVENTIVA} al labrado; trámite ya en
     * {@code PENDIENTE_ANALISIS} con resultado final compatible. La medida
     * preventiva nace en trámite vía
     * {@link PrototipoStore#registrarMedidaPreventivaPosterior} (no D1/D2, no
     * {@code ActaTransitoMock}).
     */
    private void cargarActa0026MedidaPreventivaPosteriorContravencionDemo(PrototipoStore store) {
        String id = "ACTA-0026";
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0026",
                "ESTABLECIMIENTO",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 2, 1, 10, 0),
                "Comercio — demo 0026",
                "CUIT 30-70000000-0",
                "Inspección municipal",
                "Contravención en actividad comercial: sin medida al labrado; expediente en análisis (demo"
                        + " ACTA-0026, medida posible en trámite).",
                bandeja);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0026-01",
                id,
                LocalDateTime.of(2026, 2, 1, 10, 5),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Alta: contravención sin retención documental, sin rodado, sin medida preventiva inicial (mock)."));
        eventos.add(new ActaEventoMock(
                "EVT-0026-02",
                id,
                LocalDateTime.of(2026, 3, 1, 9, 0),
                "PASE_BANDEJA",
                "D2_ENRIQUECIMIENTO",
                "D5_ANALISIS",
                "Trámite administrativo: acta en análisis, susceptibles de novedades posteriores (mock)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0026-01", id, "ACTA_FIRMADA", "FIRMADO", "acta_contravencion_0026.pdf"));
        store.getDocumentosPorActa().put(id, docs);
        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO);
    }

    /**
     * Actas en {@code PENDIENTE_ANALISIS} listas para recorridos demo de
     * fallo/apelación sin estado mezclado (mismo patrón que {@code ACTA-0006}).
     * {@code ACTA-0006} cubre fallo absolutorio; estas cuatro cubren escenarios
     * condenatorio separados para demo manual en paralelo.
     */
    private void cargarActaFalloApelacionAnalisisLimpioDemo(
            PrototipoStore store,
            String id,
            String numeroActa,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            LocalDateTime fechaCreacion) {
        String n = id.substring(5);
        String bandeja = BANDEJA_PENDIENTE_ANALISIS;
        ActaMock acta = new ActaMock(
                id,
                numeroActa,
                "SEGURIDAD_VIAL",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Oficial Demo",
                resumenHecho,
                bandeja);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + n + "-01",
                id,
                fechaCreacion.plusMinutes(15),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada; expediente en análisis jurídico (demo fallo/apelación)."));
        eventos.add(new ActaEventoMock(
                "EVT-" + n + "-02",
                id,
                fechaCreacion.plusDays(5),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificación fehaciente del acta; pasa a análisis."));
        eventos.add(new ActaEventoMock(
                "EVT-" + n + "-03",
                id,
                fechaCreacion.plusDays(12),
                "ASIGNACION_ANALISTA",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Asignada a analista para dictamen preliminar."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + n + "-01",
                id,
                "INFORME_ALCOHOTEST",
                "ADJUNTO",
                "informe_alcotest_" + n + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-" + n + "-01",
                id,
                "POSTAL",
                "ENTREGADA",
                infractorNombre + " — constancia AR demo " + n));
        notifs.add(new ActaNotificacionMock(
                "NOT-" + n + "-02",
                id,
                "EMAIL",
                "NO_APLICA",
                "Reserva — solo postal por normativa interna"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    /** Demo: fallo condenatorio + apelación portal ({@code PORTAL_INFRACTOR}). */
    private void cargarActa0027FalloApelacionPortalDemo(PrototipoStore store) {
        cargarActaFalloApelacionAnalisisLimpioDemo(
                store,
                "ACTA-0027",
                "A-2026-0027",
                "Demo Apelación Portal",
                "DNI 40.001.027",
                "Caso demo ACTA-0027: fallo condenatorio y apelación desde portal infractor.",
                LocalDateTime.of(2026, 3, 10, 9, 0));
    }

    /** Demo: fallo condenatorio + apelación presencial ({@code PRESENCIAL_DIRECCION}). */
    private void cargarActa0028FalloApelacionPresencialDemo(PrototipoStore store) {
        cargarActaFalloApelacionAnalisisLimpioDemo(
                store,
                "ACTA-0028",
                "A-2026-0028",
                "Demo Apelación Presencial",
                "DNI 40.001.028",
                "Caso demo ACTA-0028: fallo condenatorio y apelación presencial en Dirección.",
                LocalDateTime.of(2026, 3, 11, 9, 0));
    }

    /** Demo: fallo condenatorio + vencimiento de plazo → {@code CONDENA_FIRME}. */
    private void cargarActa0029FalloVencimientoPlazoDemo(PrototipoStore store) {
        cargarActaFalloApelacionAnalisisLimpioDemo(
                store,
                "ACTA-0029",
                "A-2026-0029",
                "Demo Vencimiento Plazo",
                "DNI 40.001.029",
                "Caso demo ACTA-0029: fallo condenatorio y vencimiento mock del plazo de apelación.",
                LocalDateTime.of(2026, 3, 12, 9, 0));
    }

    /** Demo: fallo condenatorio + apelación → resolver {@code ACEPTADA_ABSUELVE}. */
    private void cargarActa0030ApelacionAceptadaAbsuelveDemo(PrototipoStore store) {
        cargarActaFalloApelacionAnalisisLimpioDemo(
                store,
                "ACTA-0030",
                "A-2026-0030",
                "Demo Apelación Absuelve",
                "DNI 40.001.030",
                "Caso demo ACTA-0030: fallo condenatorio, apelación y resolución ACEPTADA_ABSUELVE.",
                LocalDateTime.of(2026, 3, 13, 9, 0));
    }

    private void cargarActasNotificadorMunicipalDemo(PrototipoStore store) {
        cargarActaNotificadorMunicipalDemo(
                store,
                "ACTA-0031",
                "A-2026-0031",
                "Demo Notificador Condenatorio",
                "DNI 41.001.031",
                "Caso demo ACTA-0031: fallo condenatorio en manos del notificador municipal.",
                TipoNotificacion.FALLO_CONDENATORIO,
                "FALLO_CONDENATORIO",
                "fallo_condenatorio_0031.pdf",
                LocalDateTime.of(2026, 3, 18, 9, 0));
        cargarActaNotificadorMunicipalDemo(
                store,
                "ACTA-0032",
                "A-2026-0032",
                "Demo Notificador Absolutorio",
                "DNI 41.001.032",
                "Caso demo ACTA-0032: fallo absolutorio en manos del notificador municipal.",
                TipoNotificacion.FALLO_ABSOLUTORIO,
                "FALLO_ABSOLUTORIO",
                "fallo_absolutorio_0032.pdf",
                LocalDateTime.of(2026, 3, 19, 9, 0));
    }

    private void cargarActaNotificadorMunicipalDemo(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            TipoNotificacion tipoNotificacion,
            String tipoDocumento,
            String nombreArchivo,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "DEMO_NOTIFICADOR_MUNICIPAL",
                "D4_NOTIFICACION",
                "PENDIENTE_ENVIO",
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Dirección de Faltas",
                resumenHecho,
                BANDEJA_PENDIENTE_NOTIFICACION);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta demo preparada para validación de notificador municipal."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-02",
                id,
                fechaCreacion.plusDays(1),
                "PENDIENTE_NOTIFICACION_MUNICIPAL",
                "D4_NOTIFICACION",
                "D4_NOTIFICACION",
                "Notificación tipificada asignada al canal NOTIFICADOR_MUNICIPAL."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                tipoDocumento,
                "FIRMADO",
                nombreArchivo));
        store.getDocumentosPorActa().put(id, docs);

        String descripcion = switch (tipoNotificacion) {
            case FALLO_CONDENATORIO -> infractorNombre + " — fallo condenatorio para acuse municipal";
            case FALLO_ABSOLUTORIO -> infractorNombre + " — fallo absolutorio para acuse municipal";
            case ACTA_INFRACCION -> infractorNombre + " — acta de infracción para acuse municipal";
        };
        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(ActaNotificacionMock.preparada(
                "NOT-" + sufijo + "-01",
                id,
                tipoNotificacion,
                CanalNotificacion.NOTIFICADOR_MUNICIPAL,
                descripcion,
                infractorNombre,
                "Domicilio demo municipal " + sufijo,
                fechaCreacion.plusDays(1),
                "ASIGNACION_NOTIFICADOR_MUNICIPAL"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    private void cargarActasPortalDomicilioElectronicoDemo(PrototipoStore store) {
        cargarActaPortalDomicilioElectronicoDemo(
                store,
                "ACTA-0034",
                "A-2026-0034",
                "Demo Portal Acta",
                "DNI 42.001.034",
                "Caso demo ACTA-0034: notificación inicial del acta por portal infractor.",
                TipoNotificacion.ACTA_INFRACCION,
                "ACTA_FIRMADA",
                "acta_firmada_0034.pdf",
                LocalDateTime.of(2026, 3, 21, 9, 0));
        cargarActaPortalDomicilioElectronicoDemo(
                store,
                "ACTA-0035",
                "A-2026-0035",
                "Demo Portal Condenatorio",
                "DNI 42.001.035",
                "Caso demo ACTA-0035: fallo condenatorio pendiente de visualización en portal infractor.",
                TipoNotificacion.FALLO_CONDENATORIO,
                "FALLO_CONDENATORIO",
                "fallo_condenatorio_0035.pdf",
                LocalDateTime.of(2026, 3, 22, 9, 0));
        cargarActaPortalDomicilioElectronicoDemo(
                store,
                "ACTA-0036",
                "A-2026-0036",
                "Demo Portal Absolutorio",
                "DNI 42.001.036",
                "Caso demo ACTA-0036: fallo absolutorio pendiente de visualización en portal infractor.",
                TipoNotificacion.FALLO_ABSOLUTORIO,
                "FALLO_ABSOLUTORIO",
                "fallo_absolutorio_0036.pdf",
                LocalDateTime.of(2026, 3, 23, 9, 0));
    }

    private void cargarActaPortalDomicilioElectronicoDemo(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            TipoNotificacion tipoNotificacion,
            String tipoDocumento,
            String nombreArchivo,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "DEMO_PORTAL_INFRACTOR",
                "D4_NOTIFICACION",
                "PENDIENTE_ENVIO",
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Dirección de Faltas",
                resumenHecho,
                BANDEJA_PENDIENTE_NOTIFICACION);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta demo preparada para validación de domicilio electrónico en portal."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-02",
                id,
                fechaCreacion.plusDays(1),
                "NOTIFICACION_PORTAL_PENDIENTE",
                "D4_NOTIFICACION",
                "D4_NOTIFICACION",
                "Notificación tipificada puesta a disposición por DOMICILIO_ELECTRONICO."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                tipoDocumento,
                "FIRMADO",
                nombreArchivo));
        store.getDocumentosPorActa().put(id, docs);

        String descripcion = switch (tipoNotificacion) {
            case FALLO_CONDENATORIO -> infractorNombre + " — fallo condenatorio disponible en portal infractor";
            case FALLO_ABSOLUTORIO -> infractorNombre + " — fallo absolutorio disponible en portal infractor";
            case ACTA_INFRACCION -> infractorNombre + " — acta de infracción disponible en portal infractor";
        };
        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(ActaNotificacionMock.preparada(
                "NOT-" + sufijo + "-01",
                id,
                tipoNotificacion,
                CanalNotificacion.DOMICILIO_ELECTRONICO,
                descripcion,
                infractorNombre,
                "Domicilio electrónico demo " + infractorDocumento,
                fechaCreacion.plusDays(1),
                "PUESTA_DISPOSICION_PORTAL"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    private void cargarActasCorreoPostalDemo(PrototipoStore store) {
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0037",
                "A-2026-0037",
                "Demo Correo Fallo Condenatorio 1",
                "DNI 43.001.037",
                "Caso demo ACTA-0037: fallo condenatorio listo para lote de correo postal.",
                TipoNotificacion.FALLO_CONDENATORIO,
                LocalDateTime.of(2026, 3, 24, 9, 0));
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0038",
                "A-2026-0038",
                "Demo Correo Acta Inicial 1",
                "DNI 43.001.038",
                "Caso demo ACTA-0038: acta de infracción lista para lote de correo postal.",
                TipoNotificacion.ACTA_INFRACCION,
                LocalDateTime.of(2026, 3, 25, 9, 0));
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0039",
                "A-2026-0039",
                "Demo Correo Fallo Condenatorio 2",
                "DNI 43.001.039",
                "Caso demo ACTA-0039: segundo fallo condenatorio listo para selección parcial en correo postal.",
                TipoNotificacion.FALLO_CONDENATORIO,
                LocalDateTime.of(2026, 3, 26, 9, 0));
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0040",
                "A-2026-0040",
                "Demo Correo Fallo Absolutorio",
                "DNI 43.001.040",
                "Caso demo ACTA-0040: fallo absolutorio listo para lote de correo postal.",
                TipoNotificacion.FALLO_ABSOLUTORIO,
                LocalDateTime.of(2026, 3, 27, 9, 0));
        cargarActaCorreoPostalNoCandidataDemo(
                store,
                "ACTA-0044",
                "A-2026-0044",
                "Demo Correo Negativa",
                "DNI 43.001.044",
                "Caso demo ACTA-0044: notificación postal con resultado negativo.",
                TipoNotificacion.ACTA_INFRACCION,
                EstadoNotificacion.NEGATIVA,
                ResultadoNotificacion.NEGATIVA,
                "NO_ENTREGADA",
                LocalDateTime.of(2026, 3, 31, 9, 0));
        cargarActaCorreoPostalNoCandidataDemo(
                store,
                "ACTA-0045",
                "A-2026-0045",
                "Demo Correo Vencida",
                "DNI 43.001.045",
                "Caso demo ACTA-0045: notificación postal vencida sin respuesta.",
                TipoNotificacion.ACTA_INFRACCION,
                EstadoNotificacion.VENCIDA,
                ResultadoNotificacion.VENCIDA,
                "VENCIDA",
                LocalDateTime.of(2026, 4, 1, 9, 0));
    }

    private void cargarActaCorreoPostalCandidataDemo(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            TipoNotificacion tipoNotificacion,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "DEMO_CORREO_POSTAL",
                "D4_NOTIFICACION",
                "PENDIENTE_ENVIO",
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Dirección de Faltas",
                resumenHecho,
                BANDEJA_PENDIENTE_NOTIFICACION);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta demo preparada para validación de correo postal."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-02",
                id,
                fechaCreacion.plusDays(1),
                "PENDIENTE_NOTIFICACION",
                "D4_NOTIFICACION",
                "D4_NOTIFICACION",
                "Notificación tipificada lista para armado de lote postal."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        String tipoDocumento = switch (tipoNotificacion) {
            case FALLO_CONDENATORIO -> "FALLO_CONDENATORIO";
            case FALLO_ABSOLUTORIO -> "FALLO_ABSOLUTORIO";
            default -> "ACTA_FIRMADA";
        };
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                tipoDocumento,
                "FIRMADO",
                tipoDocumento.toLowerCase(Locale.ROOT) + "_" + sufijo + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);

        String descripcion = switch (tipoNotificacion) {
            case FALLO_CONDENATORIO -> infractorNombre + " — fallo condenatorio listo para correo postal";
            case FALLO_ABSOLUTORIO -> infractorNombre + " — fallo absolutorio listo para correo postal";
            case ACTA_INFRACCION -> infractorNombre + " — acta de infracción lista para correo postal";
        };
        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(ActaNotificacionMock.preparada(
                "NOT-" + sufijo + "-01",
                id,
                tipoNotificacion,
                CanalNotificacion.CORREO_POSTAL,
                descripcion,
                infractorNombre,
                "Domicilio demo postal " + sufijo,
                fechaCreacion.plusDays(1),
                "PREPARACION_CORREO_POSTAL"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    private void cargarActaCorreoPostalNoCandidataDemo(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            TipoNotificacion tipoNotificacion,
            EstadoNotificacion estado,
            ResultadoNotificacion resultado,
            String estadoLegacy,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "DEMO_CORREO_POSTAL",
                "D4_NOTIFICACION",
                "EN_ENVIO",
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Dirección de Faltas",
                resumenHecho,
                BANDEJA_EN_NOTIFICACION);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta demo de trazabilidad postal fuera de listas para lote."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_" + sufijo + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);

        String descripcion = infractorNombre + " — correo postal " + estado.name().toLowerCase(Locale.ROOT) + " demo";
        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-" + sufijo + "-01",
                id,
                "POSTAL",
                estadoLegacy,
                descripcion,
                tipoNotificacion,
                CanalNotificacion.CORREO_POSTAL,
                estado,
                resultado,
                descripcion,
                "CORREO_POSTAL_DEMO",
                null,
                null,
                fechaCreacion.plusDays(1),
                fechaCreacion.plusDays(2),
                resultado == ResultadoNotificacion.SIN_RESULTADO ? null : fechaCreacion.plusDays(3),
                "Observación demo correo postal " + estado.name(),
                infractorNombre,
                null,
                "Domicilio demo postal " + sufijo,
                null,
                null));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    /**
     * Segunda tanda de actas demo para probar volumen en bandejas (~10 por macro-bandeja).
     * Usa ids libres desde {@code ACTA-0046}; no altera casos funcionales existentes.
     */
    private void cargarActasVolumenDemo(PrototipoStore store) {
        cargarActasVolumenEnriquecimientoDemo(store);
        cargarActasVolumenPreparacionDocumentalDemo(store);
        cargarActasVolumenFirmaDemo(store);
        cargarActasVolumenCorreoPostalDemo(store);
        cargarActasVolumenEnNotificacionDemo(store);
        cargarActasVolumenRedaccionDemo(store);
        cargarActasVolumenGestionExternaDemo(store);
        cargarActasVolumenArchivoDemo(store);
        cargarActasVolumenCerradasDemo(store);
    }

    private void cargarActasVolumenEnriquecimientoDemo(PrototipoStore store) {
        cargarActaVolumenEnriquecimiento(
                store,
                "ACTA-0046",
                "A-2026-0046",
                "D1_CAPTURA",
                "EN_CURSO",
                "TRANSITO_URBANO",
                "Rossi, Camila",
                "DNI 44.001.046",
                "Oficial Bracco",
                "Volumen demo: captura inicial pendiente de datos de labrado.",
                LocalDateTime.of(2026, 4, 2, 8, 30));
        cargarActaVolumenEnriquecimiento(
                store,
                "ACTA-0047",
                "A-2026-0047",
                "D1_CAPTURA",
                "EN_CURSO",
                "SEGURIDAD_VIAL",
                "Ponce, Esteban",
                "DNI 44.001.047",
                "Oficial Bracco",
                "Volumen demo: captura inicial con lectura parcial de dominio.",
                LocalDateTime.of(2026, 4, 3, 9, 15));
        cargarActaVolumenEnriquecimiento(
                store,
                "ACTA-0048",
                "A-2026-0048",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "ESTACIONAMIENTO",
                "Salinas, Nora",
                "DNI 44.001.048",
                "Oficial Bracco",
                "Volumen demo: enriquecimiento general por domicilio pendiente.",
                LocalDateTime.of(2026, 4, 4, 10, 0));
        cargarActaVolumenEnriquecimiento(
                store,
                "ACTA-0049",
                "A-2026-0049",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "TRANSITO_URBANO",
                "Vega, Tomas",
                "DNI 44.001.049",
                "Oficial Bracco",
                "Volumen demo: enriquecimiento con pago voluntario solicitado.",
                LocalDateTime.of(2026, 4, 5, 11, 20));
        store.setSituacionPago("ACTA-0049", PrototipoStore.SituacionPagoMock.SOLICITADO);
    }

    private void cargarActaVolumenEnriquecimiento(
            PrototipoStore store,
            String id,
            String numero,
            String bloque,
            String estado,
            String dominio,
            String infractorNombre,
            String infractorDocumento,
            String inspector,
            String resumenHecho,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                dominio,
                bloque,
                estado,
                "ACTIVA",
                false,
                true,
                "D1_CAPTURA".equals(bloque),
                false,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                inspector,
                resumenHecho,
                BANDEJA_ACTAS_EN_ENRIQUECIMIENTO);
        store.getActas().put(id, acta);
        if ("TRANSITO_URBANO".equals(dominio) || "SEGURIDAD_VIAL".equals(dominio)) {
            store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);
        }

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ALTA",
                "D1_CAPTURA",
                bloque,
                "Acta volumen demo ingresada a enriquecimiento."));
        store.getEventosPorActa().put(id, eventos);
    }

    private void cargarActasVolumenPreparacionDocumentalDemo(PrototipoStore store) {
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0050",
                "A-2026-0050",
                "PENDIENTE_GENERACION",
                "SEGURIDAD_VIAL",
                "Cabrera, Luis",
                "DNI 44.001.050",
                "Generacion acta pendiente tras enriquecimiento completo.",
                null,
                LocalDateTime.of(2026, 4, 6, 9, 0));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0051",
                "A-2026-0051",
                "PENDIENTE_GENERACION",
                "TRANSITO_URBANO",
                "Dominguez, Paula",
                "DNI 44.001.051",
                "Generacion acta pendiente con constancia radar adjunta.",
                null,
                LocalDateTime.of(2026, 4, 7, 10, 30));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0052",
                "A-2026-0052",
                "PENDIENTE_GENERACION",
                "ESTACIONAMIENTO",
                "Franco, Mario",
                "DNI 44.001.052",
                "Generacion acta pendiente por estacionamiento en doble fila.",
                null,
                LocalDateTime.of(2026, 4, 8, 11, 45));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0053",
                "A-2026-0053",
                "PENDIENTE_PIEZAS",
                "SEGURIDAD_VIAL",
                "Gimenez, Clara",
                "DNI 44.001.053",
                "Piezas documentales pendientes: notificacion de acta.",
                List.of("NOTIFICACION_ACTA"),
                LocalDateTime.of(2026, 4, 9, 8, 15));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0054",
                "A-2026-0054",
                "PENDIENTE_PIEZAS",
                "TRANSITO_URBANO",
                "Herrera, Diego",
                "DNI 44.001.054",
                "Piezas documentales pendientes: medida preventiva.",
                List.of("MEDIDA_PREVENTIVA"),
                LocalDateTime.of(2026, 4, 10, 9, 30));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0055",
                "A-2026-0055",
                "PENDIENTE_PIEZAS",
                "SEGURIDAD_VIAL",
                "Ibarra, Sofia",
                "DNI 44.001.055",
                "Piezas documentales pendientes: notificacion y medida.",
                List.of("NOTIFICACION_ACTA", "MEDIDA_PREVENTIVA"),
                LocalDateTime.of(2026, 4, 11, 10, 0));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0056",
                "A-2026-0056",
                "REVISION_DOCUMENTAL",
                "ESTACIONAMIENTO",
                "Juarez, Ana",
                "DNI 44.001.056",
                "Revision documental general antes de pasar a firma.",
                null,
                LocalDateTime.of(2026, 4, 12, 14, 0));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0057",
                "A-2026-0057",
                "REVISION_DOCUMENTAL",
                "TRANSITO_URBANO",
                "Klein, Bruno",
                "DNI 44.001.057",
                "Revision documental con borrador generado y observaciones menores.",
                null,
                LocalDateTime.of(2026, 4, 13, 15, 20));
        cargarActaVolumenPreparacionDocumental(
                store,
                "ACTA-0058",
                "A-2026-0058",
                "REVISION_DOCUMENTAL",
                "SEGURIDAD_VIAL",
                "Luna, Carla",
                "DNI 44.001.058",
                "Revision documental con adjuntos probatorios completos.",
                null,
                LocalDateTime.of(2026, 4, 14, 16, 40));
    }

    private void cargarActaVolumenPreparacionDocumental(
            PrototipoStore store,
            String id,
            String numero,
            String estado,
            String dominio,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            List<String> piezasPendientes,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        boolean tieneDocs = piezasPendientes != null || "REVISION_DOCUMENTAL".equals(estado);
        ActaMock acta = new ActaMock(
                id,
                numero,
                dominio,
                "D3_DOCUMENTAL",
                estado,
                "ACTIVA",
                false,
                true,
                tieneDocs,
                false,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Oficial Volumen Demo",
                resumenHecho,
                BANDEJA_PENDIENTE_PREPARACION_DOCUMENTAL);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(5),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta volumen demo en preparacion documental."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-02",
                id,
                fechaCreacion.plusDays(1),
                "PASE_BANDEJA",
                "D2_ENRIQUECIMIENTO",
                "D3_DOCUMENTAL",
                "Enriquecimiento cerrado; pasa a preparacion documental."));
        store.getEventosPorActa().put(id, eventos);

        if ("REVISION_DOCUMENTAL".equals(estado)) {
            List<ActaDocumentoMock> docs = new ArrayList<>();
            docs.add(new ActaDocumentoMock(
                    "DOC-" + sufijo + "-01",
                    id,
                    "BORRADOR_ACTA",
                    "BORRADOR",
                    "borrador_acta_" + sufijo + ".pdf"));
            store.getDocumentosPorActa().put(id, docs);
        }

        if (piezasPendientes != null && !piezasPendientes.isEmpty()) {
            store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                    id,
                    new ArrayList<>(piezasPendientes),
                    new ArrayList<>()));
        }
    }

    private void cargarActasVolumenFirmaDemo(PrototipoStore store) {
        cargarActaVolumenFirma(
                store,
                "ACTA-0059",
                "A-2026-0059",
                "BORRADOR_ACTA",
                "PENDIENTE_FIRMA",
                "Acta inicial pendiente de firma del inspector.",
                LocalDateTime.of(2026, 4, 15, 9, 0));
        cargarActaVolumenFirma(
                store,
                "ACTA-0060",
                "A-2026-0060",
                "BORRADOR_ACTA",
                "PENDIENTE_FIRMA",
                "Segunda acta inicial en cola de firma.",
                LocalDateTime.of(2026, 4, 16, 10, 15));
        cargarActaVolumenFirma(
                store,
                "ACTA-0061",
                "A-2026-0061",
                "BORRADOR_ACTA",
                "PENDIENTE_FIRMA",
                "Acta inicial con constancia probatoria adjunta.",
                LocalDateTime.of(2026, 4, 17, 11, 30));
        cargarActaVolumenFirma(
                store,
                "ACTA-0062",
                "A-2026-0062",
                "FALLO_CONDENATORIO",
                "PENDIENTE_FIRMA",
                "Fallo condenatorio pendiente de firma.",
                LocalDateTime.of(2026, 4, 18, 12, 0));
        cargarActaVolumenFirma(
                store,
                "ACTA-0063",
                "A-2026-0063",
                "FALLO_CONDENATORIO",
                "PENDIENTE_FIRMA",
                "Fallo condenatorio con monto de condena cargado.",
                LocalDateTime.of(2026, 4, 19, 13, 45));
        cargarActaVolumenFirma(
                store,
                "ACTA-0064",
                "A-2026-0064",
                "FALLO_ABSOLUTORIO",
                "PENDIENTE_FIRMA",
                "Fallo absolutorio pendiente de firma.",
                LocalDateTime.of(2026, 4, 20, 14, 30));
        cargarActaVolumenFirma(
                store,
                "ACTA-0065",
                "A-2026-0065",
                "FALLO_ABSOLUTORIO",
                "PENDIENTE_FIRMA",
                "Fallo absolutorio con informe juridico adjunto.",
                LocalDateTime.of(2026, 4, 21, 15, 0));
        cargarActaVolumenFirma(
                store,
                "ACTA-0066",
                "A-2026-0066",
                "RESOLUCION",
                "PENDIENTE_FIRMA",
                "Resolucion administrativa pendiente de firma.",
                LocalDateTime.of(2026, 4, 22, 16, 10));
        cargarActaVolumenFirma(
                store,
                "ACTA-0067",
                "A-2026-0067",
                "NULIDAD",
                "PENDIENTE_FIRMA",
                "Pieza de nulidad pendiente de firma.",
                LocalDateTime.of(2026, 4, 23, 17, 20));
    }

    private void cargarActaVolumenFirma(
            PrototipoStore store,
            String id,
            String numero,
            String tipoDocumento,
            String estadoDocumento,
            String resumenHecho,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "SEGURIDAD_VIAL",
                "D3_DOCUMENTAL",
                "PENDIENTE_FIRMA",
                "ACTIVA",
                false,
                true,
                true,
                false,
                fechaCreacion,
                "Infractor Volumen " + sufijo,
                "DNI 44.001." + sufijo,
                "Oficial Volumen Demo",
                resumenHecho,
                BANDEJA_PENDIENTE_FIRMA);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "DOCUMENTO_GENERADO",
                "D3_DOCUMENTAL",
                "D3_DOCUMENTAL",
                "Documento generado y en cola de firma (volumen demo)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                tipoDocumento,
                estadoDocumento,
                tipoDocumento.toLowerCase(Locale.ROOT) + "_" + sufijo + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);
    }

    private void cargarActasVolumenCorreoPostalDemo(PrototipoStore store) {
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0068",
                "A-2026-0068",
                "Volumen Correo Acta 3",
                "DNI 44.001.068",
                "Volumen demo ACTA-0068: acta de infraccion lista para correo postal.",
                TipoNotificacion.ACTA_INFRACCION,
                LocalDateTime.of(2026, 4, 24, 9, 0));
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0069",
                "A-2026-0069",
                "Volumen Correo Acta 4",
                "DNI 44.001.069",
                "Volumen demo ACTA-0069: segunda acta de infraccion para lote postal.",
                TipoNotificacion.ACTA_INFRACCION,
                LocalDateTime.of(2026, 4, 25, 9, 30));
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0070",
                "A-2026-0070",
                "Volumen Correo Absolutorio 2",
                "DNI 44.001.070",
                "Volumen demo ACTA-0070: fallo absolutorio listo para correo postal.",
                TipoNotificacion.FALLO_ABSOLUTORIO,
                LocalDateTime.of(2026, 4, 26, 10, 0));
        cargarActaCorreoPostalCandidataDemo(
                store,
                "ACTA-0071",
                "A-2026-0071",
                "Volumen Correo Acta 5",
                "DNI 44.001.071",
                "Volumen demo ACTA-0071: acta de infraccion con domicilio verificado.",
                TipoNotificacion.ACTA_INFRACCION,
                LocalDateTime.of(2026, 4, 27, 10, 30));
    }

    private void cargarActasVolumenEnNotificacionDemo(PrototipoStore store) {
        cargarActaCorreoPostalNoCandidataDemo(
                store,
                "ACTA-0072",
                "A-2026-0072",
                "Volumen Correo Enviada 2",
                "DNI 44.001.072",
                "Volumen demo ACTA-0072: correo postal enviado pendiente de respuesta.",
                TipoNotificacion.ACTA_INFRACCION,
                EstadoNotificacion.ENVIADA,
                ResultadoNotificacion.SIN_RESULTADO,
                "EN_TRAMITE",
                LocalDateTime.of(2026, 4, 28, 9, 0));
        cargarActaCorreoPostalNoCandidataDemo(
                store,
                "ACTA-0073",
                "A-2026-0073",
                "Volumen Correo Enviada 3",
                "DNI 44.001.073",
                "Volumen demo ACTA-0073: fallo condenatorio en trámite postal.",
                TipoNotificacion.FALLO_CONDENATORIO,
                EstadoNotificacion.ENVIADA,
                ResultadoNotificacion.SIN_RESULTADO,
                "EN_TRAMITE",
                LocalDateTime.of(2026, 4, 29, 9, 30));
        cargarActaCorreoPostalNoCandidataDemo(
                store,
                "ACTA-0074",
                "A-2026-0074",
                "Volumen Correo Positiva 2",
                "DNI 44.001.074",
                "Volumen demo ACTA-0074: notificacion postal entregada con resultado positivo.",
                TipoNotificacion.ACTA_INFRACCION,
                EstadoNotificacion.ENTREGADA,
                ResultadoNotificacion.POSITIVA,
                "ENTREGADA",
                LocalDateTime.of(2026, 4, 30, 10, 0));
        cargarActaCorreoPostalNoCandidataDemo(
                store,
                "ACTA-0075",
                "A-2026-0075",
                "Volumen Correo Negativa 2",
                "DNI 44.001.075",
                "Volumen demo ACTA-0075: notificacion postal con resultado negativo.",
                TipoNotificacion.ACTA_INFRACCION,
                EstadoNotificacion.NEGATIVA,
                ResultadoNotificacion.NEGATIVA,
                "NO_ENTREGADA",
                LocalDateTime.of(2026, 5, 1, 10, 30));
        cargarActaCorreoPostalNoCandidataDemo(
                store,
                "ACTA-0076",
                "A-2026-0076",
                "Volumen Correo Vencida 2",
                "DNI 44.001.076",
                "Volumen demo ACTA-0076: notificacion postal vencida sin respuesta.",
                TipoNotificacion.ACTA_INFRACCION,
                EstadoNotificacion.VENCIDA,
                ResultadoNotificacion.VENCIDA,
                "VENCIDA",
                LocalDateTime.of(2026, 5, 2, 11, 0));
    }

    private void cargarActasVolumenRedaccionDemo(PrototipoStore store) {
        cargarActaVolumenRedaccion(
                store,
                "ACTA-0077",
                "A-2026-0077",
                "PENDIENTE_RESOLUCION",
                "Resolucion administrativa pendiente de redaccion.",
                "RESOLUCION",
                LocalDateTime.of(2026, 5, 3, 9, 0));
        cargarActaVolumenRedaccion(
                store,
                "ACTA-0078",
                "A-2026-0078",
                "PENDIENTE_RESOLUCION",
                "Segunda resolucion en bandeja de redaccion.",
                "RESOLUCION",
                LocalDateTime.of(2026, 5, 4, 10, 0));
        cargarActaVolumenRedaccion(
                store,
                "ACTA-0079",
                "A-2026-0079",
                "PENDIENTE_NULIDAD",
                "Vicio formal detectado; redaccion de nulidad.",
                "NULIDAD",
                LocalDateTime.of(2026, 5, 5, 11, 0));
        cargarActaVolumenRedaccion(
                store,
                "ACTA-0080",
                "A-2026-0080",
                "PENDIENTE_MEDIDA_PREVENTIVA",
                "Retencion preventiva; redaccion de medida.",
                "MEDIDA_PREVENTIVA",
                LocalDateTime.of(2026, 5, 6, 12, 0));
        cargarActaVolumenRedaccion(
                store,
                "ACTA-0081",
                "A-2026-0081",
                "PENDIENTE_RECTIFICACION",
                "Error material en datos; redaccion de rectificacion.",
                "RECTIFICACION",
                LocalDateTime.of(2026, 5, 7, 13, 0));
    }

    private void cargarActaVolumenRedaccion(
            PrototipoStore store,
            String id,
            String numero,
            String estado,
            String resumenHecho,
            String pieza,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                estado,
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                "Infractor Redaccion " + sufijo,
                "DNI 44.001." + sufijo,
                "Oficial Volumen Demo",
                resumenHecho,
                BANDEJA_PENDIENTES_RESOLUCION_REDACCION);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "DERIVACION_REDACCION",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Derivado a redaccion de pieza " + pieza + " (volumen demo)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_" + sufijo + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-" + sufijo + "-01",
                id,
                "POSTAL",
                "ENTREGADA",
                "Infractor Redaccion " + sufijo + " — notificacion positiva demo"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.getPiezasRequeridasPorActa().put(id, new ActaPiezasRequeridasMock(
                id,
                new ArrayList<>(List.of(pieza)),
                new ArrayList<>()));
    }

    private void cargarActasVolumenGestionExternaDemo(PrototipoStore store) {
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0082",
                "A-2026-0082",
                "Volumen Apremio 1",
                "DNI 44.001.082",
                "Gestion externa en apremio por deuda firme.",
                PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO,
                true,
                LocalDateTime.of(2026, 5, 8, 9, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0083",
                "A-2026-0083",
                "Volumen Apremio 2",
                "DNI 44.001.083",
                "Seguimiento de apremio con reingreso habilitado.",
                PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO,
                true,
                LocalDateTime.of(2026, 5, 9, 10, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0084",
                "A-2026-0084",
                "Volumen Apremio 3",
                "DNI 44.001.084",
                "Apremio iniciado tras condena firme.",
                PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO,
                false,
                LocalDateTime.of(2026, 5, 10, 11, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0085",
                "A-2026-0085",
                "Volumen Apremio 4",
                "DNI 44.001.085",
                "Apremio con gestion en curso.",
                PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO,
                true,
                LocalDateTime.of(2026, 5, 11, 12, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0086",
                "A-2026-0086",
                "Volumen Juzgado 1",
                "DNI 44.001.086",
                "Derivado a Juzgado de Paz por fallo firme.",
                PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ,
                true,
                LocalDateTime.of(2026, 5, 12, 13, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0087",
                "A-2026-0087",
                "Volumen Juzgado 2",
                "DNI 44.001.087",
                "Seguimiento en Juzgado de Paz.",
                PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ,
                true,
                LocalDateTime.of(2026, 5, 13, 14, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0088",
                "A-2026-0088",
                "Volumen Juzgado 3",
                "DNI 44.001.088",
                "Juzgado de Paz con reingreso pendiente.",
                PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ,
                true,
                LocalDateTime.of(2026, 5, 14, 15, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0089",
                "A-2026-0089",
                "Volumen Juzgado 4",
                "DNI 44.001.089",
                "Expediente en Juzgado de Paz sin novedad.",
                PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ,
                false,
                LocalDateTime.of(2026, 5, 15, 16, 0));
        cargarActaVolumenGestionExterna(
                store,
                "ACTA-0090",
                "A-2026-0090",
                "Volumen Externa Seguimiento",
                "DNI 44.001.090",
                "Seguimiento externo generico en gestion.",
                PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO,
                true,
                LocalDateTime.of(2026, 5, 16, 17, 0));
    }

    private void cargarActaVolumenGestionExterna(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            String tipoGestion,
            boolean permiteReingreso,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "TRANSITO_URBANO",
                "GESTION_EXTERNA",
                "EN_GESTION_EXTERNA",
                "GESTION_EXTERNA",
                false,
                permiteReingreso,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Direccion de Faltas",
                resumenHecho,
                BANDEJA_GESTION_EXTERNA);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "DERIVACION_GESTION_EXTERNA",
                "D5_ANALISIS",
                "GESTION_EXTERNA",
                "Derivado a gestion externa tipo " + tipoGestion + " (volumen demo)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                "FALLO",
                "FIRMADO",
                "fallo_" + sufijo + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setTipoGestionExterna(id, tipoGestion);
    }

    private void cargarActasVolumenArchivoDemo(PrototipoStore store) {
        cargarActaVolumenArchivo(
                store,
                "ACTA-0091",
                "A-2026-0091",
                "ARCHIVADA_OPERATIVA",
                "Archivo operativo por duplicidad.",
                PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO,
                true,
                LocalDateTime.of(2026, 5, 17, 9, 0));
        cargarActaVolumenArchivo(
                store,
                "ACTA-0092",
                "A-2026-0092",
                "ARCHIVADA_OPERATIVA",
                "Archivo operativo con reingreso permitido.",
                PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO,
                true,
                LocalDateTime.of(2026, 5, 18, 10, 0));
        cargarActaVolumenArchivo(
                store,
                "ACTA-0093",
                "A-2026-0093",
                "ARCHIVADA_OPERATIVA",
                "Archivo definitivo sin reingreso.",
                PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO,
                false,
                LocalDateTime.of(2026, 5, 19, 11, 0));
        cargarActaVolumenArchivo(
                store,
                "ACTA-0094",
                "A-2026-0094",
                "ARCHIVADA_JURIDICA",
                "Archivo juridico por decision administrativa.",
                null,
                true,
                LocalDateTime.of(2026, 5, 20, 12, 0));
        cargarActaVolumenArchivo(
                store,
                "ACTA-0095",
                "A-2026-0095",
                "ARCHIVADA_JURIDICA",
                "Archivo juridico con resolucion firmada.",
                null,
                false,
                LocalDateTime.of(2026, 5, 21, 13, 0));
        cargarActaVolumenArchivo(
                store,
                "ACTA-0096",
                "A-2026-0096",
                "ARCHIVADA_OPERATIVA",
                "Archivo post evaluacion de vencimiento.",
                PrototipoStore.MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO,
                true,
                LocalDateTime.of(2026, 5, 22, 14, 0));
        cargarActaVolumenArchivo(
                store,
                "ACTA-0097",
                "A-2026-0097",
                "ARCHIVADA_OPERATIVA",
                "Archivo post vencimiento sin reingreso.",
                PrototipoStore.MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO,
                false,
                LocalDateTime.of(2026, 5, 23, 15, 0));
        cargarActaVolumenArchivo(
                store,
                "ACTA-0098",
                "A-2026-0098",
                "ARCHIVADA_OPERATIVA",
                "Archivo operativo general.",
                null,
                true,
                LocalDateTime.of(2026, 5, 24, 16, 0));
    }

    private void cargarActaVolumenArchivo(
            PrototipoStore store,
            String id,
            String numero,
            String estadoProceso,
            String resumenHecho,
            String motivoArchivo,
            boolean permiteReingreso,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "SEGURIDAD_VIAL",
                "ARCHIVO",
                estadoProceso,
                "ARCHIVO",
                false,
                permiteReingreso,
                true,
                false,
                fechaCreacion,
                "Infractor Archivo " + sufijo,
                "DNI 44.001." + sufijo,
                "Oficial Volumen Demo",
                resumenHecho,
                BANDEJA_ARCHIVO);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ARCHIVO_OPERATIVO",
                "D5_ANALISIS",
                "ARCHIVO",
                "Archivo volumen demo."));
        store.getEventosPorActa().put(id, eventos);

        if (motivoArchivo != null) {
            store.setMotivoArchivo(id, motivoArchivo);
        }
    }

    private void cargarActasVolumenCerradasDemo(PrototipoStore store) {
        cargarActaVolumenCerrada(
                store,
                "ACTA-0099",
                "A-2026-0099",
                "Cerrada por pago voluntario confirmado.",
                PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO,
                false,
                false,
                LocalDateTime.of(2026, 5, 25, 9, 0));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0100",
                "A-2026-0100",
                "Cerrada por pago de condena firme.",
                PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME,
                false,
                true,
                LocalDateTime.of(2026, 5, 26, 10, 0));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0101",
                "A-2026-0101",
                "Cerrada por absolucion.",
                PrototipoStore.ResultadoFinalCierreMock.ABSUELTO,
                false,
                true,
                LocalDateTime.of(2026, 5, 27, 11, 0));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0102",
                "A-2026-0102",
                "Cerrada por nulidad.",
                PrototipoStore.ResultadoFinalCierreMock.ABSUELTO,
                false,
                true,
                LocalDateTime.of(2026, 5, 28, 12, 0));
        store.getDocumentosPorActa().put("ACTA-0102", new ArrayList<>(List.of(
                new ActaDocumentoMock(
                        "DOC-0102-01",
                        "ACTA-0102",
                        "NULIDAD",
                        "FIRMADO",
                        "nulidad_0102.pdf"))));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0103",
                "A-2026-0103",
                "Cerrada tras archivo definitivo.",
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                false,
                LocalDateTime.of(2026, 5, 29, 13, 0));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0104",
                "A-2026-0104",
                "Cerrada por otra causa administrativa.",
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                true,
                LocalDateTime.of(2026, 5, 30, 14, 0));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0105",
                "A-2026-0105",
                "Cerrada por pago voluntario temprano.",
                PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO,
                true,
                false,
                LocalDateTime.of(2026, 5, 31, 15, 0));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0106",
                "A-2026-0106",
                "Cerrada por condena con comprobante.",
                PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME,
                true,
                true,
                LocalDateTime.of(2026, 6, 1, 16, 0));
        cargarActaVolumenCerrada(
                store,
                "ACTA-0107",
                "A-2026-0107",
                "Cerrada por absolucion en segunda instancia.",
                PrototipoStore.ResultadoFinalCierreMock.ABSUELTO,
                true,
                true,
                LocalDateTime.of(2026, 6, 2, 17, 0));
    }

    private void cargarActaVolumenCerrada(
            PrototipoStore store,
            String id,
            String numero,
            String resumenHecho,
            PrototipoStore.ResultadoFinalCierreMock resultadoFinal,
            boolean estaCerradaFlag,
            boolean permiteReingreso,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "ESTACIONAMIENTO",
                "CERRADA",
                "CERRADA",
                "CERRADA",
                estaCerradaFlag,
                permiteReingreso,
                "ACTA-0102".equals(id),
                false,
                fechaCreacion,
                "Infractor Cerrada " + sufijo,
                "DNI 44.001." + sufijo,
                "Oficial Volumen Demo",
                resumenHecho,
                BANDEJA_CERRADAS);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "CIERRE",
                "D5_ANALISIS",
                "CERRADA",
                "Cierre volumen demo."));
        store.getEventosPorActa().put(id, eventos);

        if (resultadoFinal != PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL) {
            store.setResultadoFinalCierreDemo(id, resultadoFinal);
        }
    }

    /**
     * Actas demo para bandejas laterales UX que arrancaban vacías
     * ({@code PENDIENTES_FALLO}, {@code CON_APELACION}, {@code PARALIZADAS}).
     * Ids libres desde {@code ACTA-0108}; no altera casos funcionales previos.
     */
    private void cargarActasBandejasUxDemo(PrototipoStore store) {
        cargarActasParalizadasUxDemo(store);
    }

    private void cargarActasPendientesFalloUxDemo(PrototipoStore store) {
        cargarActaPendienteFalloUxDemo(
                store,
                "ACTA-0108",
                "A-2026-0108",
                "Demo Fallo Condenatorio Pendiente",
                "DNI 45.001.108",
                "Caso demo ACTA-0108: notificacion positiva; lista para fallo condenatorio.",
                false,
                LocalDateTime.of(2026, 6, 10, 9, 0));
        cargarActaPendienteFalloUxDemo(
                store,
                "ACTA-0109",
                "A-2026-0109",
                "Demo Fallo Absolutorio Pendiente",
                "DNI 45.001.109",
                "Caso demo ACTA-0109: notificacion positiva; lista para fallo absolutorio.",
                true,
                LocalDateTime.of(2026, 6, 11, 10, 0));
        cargarActaPendienteFalloUxDemo(
                store,
                "ACTA-0110",
                "A-2026-0110",
                "Demo Fallo Tras Pago",
                "DNI 45.001.110",
                "Caso demo ACTA-0110: pago informado; expediente derivado a fallo.",
                false,
                LocalDateTime.of(2026, 6, 12, 11, 0));
        store.setSituacionPago("ACTA-0110", PrototipoStore.SituacionPagoMock.PAGO_INFORMADO);
        store.setPagoInformadoDemo(
                "ACTA-0110",
                new PrototipoStore.PagoInformadoMock(
                        LocalDateTime.of(2026, 6, 12, 11, 30),
                        "COMP-0110",
                        "comprobante_pago_0110.pdf"));
        store.setAccionPendiente("ACTA-0110", PrototipoStore.ACCION_VERIFICAR_PAGO_INFORMADO);
    }

    private void cargarActaPendienteFalloUxDemo(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            boolean perfilAbsolutorio,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "SEGURIDAD_VIAL",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Oficial Demo",
                resumenHecho,
                BANDEJA_PENDIENTES_FALLO);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(15),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta ingresada; expediente en condicion de fallo (demo UX)."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-02",
                id,
                fechaCreacion.plusDays(5),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificacion fehaciente del acta; pasa a pendientes de fallo."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-03",
                id,
                fechaCreacion.plusDays(8),
                "DERIVACION_PENDIENTE_FALLO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                perfilAbsolutorio
                        ? "Expediente habilitado para fallo absolutorio."
                        : "Expediente habilitado para fallo condenatorio."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_" + sufijo + ".pdf"));
        if (perfilAbsolutorio) {
            docs.add(new ActaDocumentoMock(
                    "DOC-" + sufijo + "-02",
                    id,
                    "INFORME_ALCOHOTEST",
                    "ADJUNTO",
                    "informe_alcotest_" + sufijo + ".pdf"));
        }
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-" + sufijo + "-01",
                id,
                "POSTAL",
                "ENTREGADA",
                infractorNombre + " — notificacion positiva demo " + sufijo));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    private void cargarActasConApelacionUxDemo(PrototipoStore store) {
        cargarActaConApelacionUxDemo(
                store,
                "ACTA-0111",
                "A-2026-0111",
                "Demo Apelacion Pendiente Resolucion",
                "DNI 45.001.111",
                "Caso demo ACTA-0111: apelacion presentada; pendiente de resolucion.",
                false,
                null,
                null,
                LocalDateTime.of(2026, 6, 13, 9, 0));
        cargarActaConApelacionUxDemo(
                store,
                "ACTA-0112",
                "A-2026-0112",
                "Demo Apelacion En Analisis",
                "DNI 45.001.112",
                "Caso demo ACTA-0112: apelacion presentada; en revision juridica.",
                false,
                null,
                ACCION_REVISION_APELACION,
                LocalDateTime.of(2026, 6, 14, 10, 0));
        cargarActaConApelacionUxDemo(
                store,
                "ACTA-0113",
                "A-2026-0113",
                "Demo Apelacion Resuelta Absuelve",
                "DNI 45.001.113",
                "Caso demo ACTA-0113: apelacion resuelta ACEPTADA_ABSUELVE; lista para cierre o archivo.",
                true,
                PrototipoStore.ResultadoResolucionApelacionMock.ACEPTADA_ABSUELVE,
                null,
                LocalDateTime.of(2026, 6, 15, 11, 0));
    }

    private void cargarActaConApelacionUxDemo(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            boolean apelacionResuelta,
            PrototipoStore.ResultadoResolucionApelacionMock resultadoResolucion,
            String accionPendiente,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "SEGURIDAD_VIAL",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Oficial Demo",
                resumenHecho,
                BANDEJA_CON_APELACION);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);
        store.setMontoCondenaDemo(id, java.math.BigDecimal.valueOf(85000));

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta demo con fallo condenatorio notificado."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-02",
                id,
                fechaCreacion.plusDays(3),
                "FALLO_CONDENATORIO_DICTADO",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Fallo condenatorio dictado (precarga demo)."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-03",
                id,
                fechaCreacion.plusDays(10),
                "NOTIFICACION_FALLO_POSITIVA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificacion positiva del fallo condenatorio."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-04",
                id,
                fechaCreacion.plusDays(15),
                "APELACION_PRESENTADA",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Apelacion presentada por portal infractor (precarga demo)."));
        if (apelacionResuelta && resultadoResolucion != null) {
            eventos.add(new ActaEventoMock(
                    "EVT-" + sufijo + "-05",
                    id,
                    fechaCreacion.plusDays(25),
                    "APELACION_ACEPTADA_ABSUELVE",
                    "D5_ANALISIS",
                    "D5_ANALISIS",
                    "Resolucion de apelacion: ACEPTADA_ABSUELVE (precarga demo)."));
        }
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_" + sufijo + ".pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-02",
                id,
                "FALLO_CONDENATORIO",
                "FIRMADO",
                "fallo_condenatorio_" + sufijo + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-" + sufijo + "-01",
                id,
                "POSTAL",
                "ENTREGADA",
                infractorNombre + " — notificacion de fallo condenatorio demo"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.precargarApelacionDemo(
                id,
                PrototipoStore.CanalPresentacionApelacionMock.PORTAL_INFRACTOR,
                apelacionResuelta,
                resultadoResolucion);
        if (accionPendiente != null) {
            store.setAccionPendiente(id, accionPendiente);
        }
    }

    private void cargarActasParalizadasUxDemo(PrototipoStore store) {
        cargarActaParalizadaUxDemo(
                store,
                "ACTA-0114",
                "A-2026-0114",
                "Demo Paralizada Espera Documental",
                "DNI 45.001.114",
                "Paralizada por espera de documentacion probatoria del infractor.",
                ACCION_PARALIZACION_ESPERA_DOCUMENTAL,
                "Espera de documentacion probatoria",
                LocalDateTime.of(2026, 6, 16, 9, 0));
    }

    private void cargarActaParalizadaUxDemo(
            PrototipoStore store,
            String id,
            String numero,
            String infractorNombre,
            String infractorDocumento,
            String resumenHecho,
            String accionParalizacion,
            String motivoParalizacion,
            LocalDateTime fechaCreacion) {
        String sufijo = id.substring("ACTA-".length());
        ActaMock acta = new ActaMock(
                id,
                numero,
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PARALIZADA",
                "PARALIZADA",
                false,
                true,
                true,
                true,
                fechaCreacion,
                infractorNombre,
                infractorDocumento,
                "Oficial Demo",
                resumenHecho,
                BANDEJA_PARALIZADAS);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);
        store.setAccionPendiente(id, accionParalizacion);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-01",
                id,
                fechaCreacion.plusMinutes(10),
                "ALTA",
                "D1_CAPTURA",
                "D2_ENRIQUECIMIENTO",
                "Acta demo ingresada."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-02",
                id,
                fechaCreacion.plusDays(2),
                "NOTIFICACION_ENTREGADA",
                "D4_NOTIFICACION",
                "D5_ANALISIS",
                "Notificacion del acta cumplida."));
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijo + "-03",
                id,
                fechaCreacion.plusDays(5),
                "PARALIZACION",
                "D5_ANALISIS",
                "D5_ANALISIS",
                "Paralizacion vigente: " + motivoParalizacion + "."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-" + sufijo + "-01",
                id,
                "ACTA_FIRMADA",
                "FIRMADO",
                "acta_firmada_" + sufijo + ".pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-" + sufijo + "-01",
                id,
                "POSTAL",
                "ENTREGADA",
                infractorNombre + " — notificacion positiva previa a paralizacion"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    /**
     * Asigna dependencia demo coherente a actas precargadas que aun no la tienen.
     * No sobrescribe registros explicitos (p. ej. fallo/apelacion, UX 0108+).
     */
    private void completarDependenciasDemo(PrototipoStore store) {
        for (var entry : store.getActas().entrySet()) {
            String id = entry.getKey();
            if (store.getDependenciaDemo(id).isPresent()) {
                continue;
            }
            ActaMock acta = entry.getValue();
            PrototipoStore.DependenciaActaDemo dep =
                    inferirDependenciaDemo(id, acta.dominioReferencia());
            store.registrarDependenciaDemo(id, dep);
        }
    }

    private static PrototipoStore.DependenciaActaDemo inferirDependenciaDemo(
            String actaId, String dominioReferencia) {
        if ("ESTABLECIMIENTO".equals(dominioReferencia)) {
            return PrototipoStore.DependenciaActaDemo.BROMATOLOGIA;
        }
        PrototipoStore.DependenciaActaDemo base = dependenciaBaseDesdeDominio(dominioReferencia);
        int n = numeroSecuencialActa(actaId);
        if (n < 0) {
            return base;
        }
        if (n >= 31 && n <= 45) {
            return base;
        }
        if (n >= 1 && n <= 17) {
            return switch (n) {
                case 7 -> PrototipoStore.DependenciaActaDemo.INSPECCIONES;
                case 14 -> PrototipoStore.DependenciaActaDemo.FISCALIZACION;
                default -> base;
            };
        }
        if (n >= 46) {
            return switch (n % 8) {
                case 0 -> PrototipoStore.DependenciaActaDemo.INSPECCIONES;
                case 1 -> PrototipoStore.DependenciaActaDemo.FISCALIZACION;
                case 2 -> PrototipoStore.DependenciaActaDemo.BROMATOLOGIA;
                default -> base;
            };
        }
        return base;
    }

    private static PrototipoStore.DependenciaActaDemo dependenciaBaseDesdeDominio(String dominioReferencia) {
        return switch (dominioReferencia) {
            case "ESTABLECIMIENTO" -> PrototipoStore.DependenciaActaDemo.BROMATOLOGIA;
            default -> PrototipoStore.DependenciaActaDemo.TRANSITO;
        };
    }

    private static int numeroSecuencialActa(String actaId) {
        if (actaId == null || !actaId.startsWith("ACTA-")) {
            return -1;
        }
        try {
            return Integer.parseInt(actaId.substring(5));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** Hito 8: pago voluntario informado, pendiente de confirmación. */
    private void cargarActa0120PagoVoluntarioInformadoDemo(PrototipoStore store) {
        String id = "ACTA-0120";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0120",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2026, 7, 1, 9, 0),
                "Herrera, Julián",
                "DNI 41.001.120",
                "Oficial Demo",
                "Caso demo ACTA-0120: pago voluntario informado pendiente de confirmación.",
                BANDEJA_PENDIENTE_ANALISIS);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0120-01", id,
                LocalDateTime.of(2026, 7, 1, 9, 10),
                "ALTA", "D1_CAPTURA", "D2_ENRIQUECIMIENTO",
                "Acta ingresada; datos completos."));
        eventos.add(new ActaEventoMock(
                "EVT-0120-02", id,
                LocalDateTime.of(2026, 7, 6, 10, 0),
                "NOTIFICACION_ENTREGADA", "D4_NOTIFICACION", "D5_ANALISIS",
                "Notificación fehaciente del acta; pasa a análisis."));
        eventos.add(new ActaEventoMock(
                "EVT-0120-03", id,
                LocalDateTime.of(2026, 7, 10, 11, 0),
                "PAGO_INFORMADO", "D5_ANALISIS", "D5_ANALISIS",
                "Infractor informa pago voluntario; comprobante adjunto pendiente de verificación."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0120-01", id, "ACTA_FIRMADA", "FIRMADO", "acta_firmada_0120.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0120-02", id, "COMPROBANTE_PAGO", "ADJUNTO", "comprobante_pago_0120.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0120-01", id, "POSTAL", "ENTREGADA",
                "Julián Herrera — notificación positiva del acta"));
        store.getNotificacionesPorActa().put(id, notifs);

        // El comprobante ya fue adjuntado: la situación es PENDIENTE_CONFIRMACION
        // (no PAGO_INFORMADO, que es el estado previo al adjunto del comprobante).
        // El monto fue fijado por Dirección de Faltas antes del pago informado.
        store.setMontoPagoVoluntario(id, java.math.BigDecimal.valueOf(8500));
        store.setSituacionPago(id, PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION);
        store.setPagoInformadoDemo(
                id,
                new PrototipoStore.PagoInformadoMock(
                        LocalDateTime.of(2026, 7, 10, 11, 30),
                        "COMP-0120",
                        "comprobante_pago_0120.pdf"));
        store.setAccionPendiente(id, PrototipoStore.ACCION_VERIFICAR_PAGO_INFORMADO);
    }

    /** Hito 10: fallo condenatorio dictado pendiente de firma. */
    private void cargarActa0121FalloPendienteFirmaDemo(PrototipoStore store) {
        String id = "ACTA-0121";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0121",
                "SEGURIDAD_VIAL",
                "D3_DOCUMENTAL",
                "PENDIENTE_FIRMA",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2026, 7, 2, 10, 0),
                "Medina, Sofía",
                "DNI 42.001.121",
                "Oficial Demo",
                "Caso demo ACTA-0121: fallo condenatorio dictado pendiente de firma.",
                BANDEJA_PENDIENTE_FIRMA);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0121-01", id,
                LocalDateTime.of(2026, 7, 2, 10, 10),
                "ALTA", "D1_CAPTURA", "D2_ENRIQUECIMIENTO",
                "Acta ingresada; datos completos."));
        eventos.add(new ActaEventoMock(
                "EVT-0121-02", id,
                LocalDateTime.of(2026, 7, 7, 14, 0),
                "NOTIFICACION_ENTREGADA", "D4_NOTIFICACION", "D5_ANALISIS",
                "Notificación fehaciente del acta."));
        eventos.add(new ActaEventoMock(
                "EVT-0121-03", id,
                LocalDateTime.of(2026, 7, 15, 9, 0),
                "FALLO_CONDENATORIO_DICTADO", "D5_ANALISIS", "D5_ANALISIS",
                "Fallo condenatorio dictado; pendiente firma de autoridad competente."));
        eventos.add(new ActaEventoMock(
                "EVT-0121-04", id,
                LocalDateTime.of(2026, 7, 15, 9, 30),
                "DERIVACION_FIRMA", "D5_ANALISIS", "D3_DOCUMENTAL",
                "Fallo derivado a firma; queda en bandeja PENDIENTE_FIRMA."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0121-01", id, "ACTA_FIRMADA", "FIRMADO", "acta_firmada_0121.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0121-02", id, "FALLO_CONDENATORIO", "PENDIENTE_FIRMA",
                "fallo_condenatorio_0121.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0121-01", id, "POSTAL", "ENTREGADA",
                "Sofía Medina — notificación positiva del acta"));
        store.getNotificacionesPorActa().put(id, notifs);
    }

    /** Hito 13: condena firme establecida; pago de condena pendiente de informar. */
    private void cargarActa0122CondenaFirmePagoCondenaDemo(PrototipoStore store) {
        String id = "ACTA-0122";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0122",
                "TRANSITO_URBANO",
                "D5_ANALISIS",
                "PENDIENTE_REVISION",
                "ACTIVA",
                false,
                true,
                true,
                true,
                LocalDateTime.of(2026, 7, 3, 8, 0),
                "Varela, Diego",
                "DNI 43.001.122",
                "Oficial Demo",
                "Caso demo ACTA-0122: condena firme; pago de condena pendiente de informar.",
                BANDEJA_PENDIENTE_ANALISIS);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.TRANSITO);
        store.setMontoCondenaDemo(id, java.math.BigDecimal.valueOf(95000));

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0122-01", id,
                LocalDateTime.of(2026, 7, 3, 8, 10),
                "ALTA", "D1_CAPTURA", "D2_ENRIQUECIMIENTO",
                "Acta ingresada."));
        eventos.add(new ActaEventoMock(
                "EVT-0122-02", id,
                LocalDateTime.of(2026, 7, 8, 11, 0),
                "NOTIFICACION_ENTREGADA", "D4_NOTIFICACION", "D5_ANALISIS",
                "Notificación fehaciente del acta."));
        eventos.add(new ActaEventoMock(
                "EVT-0122-03", id,
                LocalDateTime.of(2026, 7, 16, 10, 0),
                "FALLO_CONDENATORIO_DICTADO", "D5_ANALISIS", "D5_ANALISIS",
                "Fallo condenatorio dictado."));
        eventos.add(new ActaEventoMock(
                "EVT-0122-04", id,
                LocalDateTime.of(2026, 7, 20, 14, 0),
                "NOTIFICACION_FALLO_POSITIVA", "D4_NOTIFICACION", "D5_ANALISIS",
                "Fallo notificado positivamente; inicia plazo de apelación."));
        eventos.add(new ActaEventoMock(
                "EVT-0122-05", id,
                LocalDateTime.of(2026, 7, 30, 9, 0),
                "CONDENA_FIRME", "D5_ANALISIS", "D5_ANALISIS",
                "Plazo de apelación vencido sin presentación; condena firme establecida."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0122-01", id, "ACTA_FIRMADA", "FIRMADO", "acta_firmada_0122.pdf"));
        docs.add(new ActaDocumentoMock(
                "DOC-0122-02", id, "FALLO_CONDENATORIO", "FIRMADO",
                "fallo_condenatorio_0122.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        List<ActaNotificacionMock> notifs = new ArrayList<>();
        notifs.add(new ActaNotificacionMock(
                "NOT-0122-01", id, "POSTAL", "ENTREGADA",
                "Diego Varela — notificación del acta"));
        notifs.add(new ActaNotificacionMock(
                "NOT-0122-02", id, "POSTAL", "ENTREGADA",
                "Diego Varela — notificación del fallo"));
        store.getNotificacionesPorActa().put(id, notifs);

        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
    }

    /** Hito 18: caso inicial dependencia INSPECCIONES, sin documentos avanzados. */
    private void cargarActa0123InspeccionesInicialDemo(PrototipoStore store) {
        String id = "ACTA-0123";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0123",
                "TRANSITO_URBANO",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 7, 4, 10, 0),
                "Rojas, Carmen",
                "DNI 44.001.123",
                "Inspector Demo",
                "Caso demo ACTA-0123: captura inicial del circuito de Inspecciones.",
                BANDEJA_ACTAS_EN_ENRIQUECIMIENTO);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.INSPECCIONES);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0123-01", id,
                LocalDateTime.of(2026, 7, 4, 10, 10),
                "ALTA", "D1_CAPTURA", "D2_ENRIQUECIMIENTO",
                "Acta de inspección ingresada; pendiente completar datos del establecimiento."));
        store.getEventosPorActa().put(id, eventos);
        store.setAccionPendiente(id, "COMPLETAR_ENRIQUECIMIENTO");
    }

    /** Hito 19: caso inicial dependencia FISCALIZACION, sin documentos avanzados. */
    private void cargarActa0124FiscalizacionInicialDemo(PrototipoStore store) {
        String id = "ACTA-0124";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0124",
                "TRANSITO_URBANO",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 7, 5, 9, 0),
                "Mendoza, Pablo",
                "DNI 45.001.124",
                "Fiscalizador Demo",
                "Caso demo ACTA-0124: captura inicial del circuito de Fiscalización.",
                BANDEJA_ACTAS_EN_ENRIQUECIMIENTO);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.FISCALIZACION);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0124-01", id,
                LocalDateTime.of(2026, 7, 5, 9, 10),
                "ALTA", "D1_CAPTURA", "D2_ENRIQUECIMIENTO",
                "Acta de fiscalización ingresada; pendiente datos del local."));
        store.getEventosPorActa().put(id, eventos);
        store.setAccionPendiente(id, "COMPLETAR_ENRIQUECIMIENTO");
    }

    /** Hito 20: caso inicial dependencia BROMATOLOGIA, sin documentos avanzados. */
    private void cargarActa0125BromatologiaInicialDemo(PrototipoStore store) {
        String id = "ACTA-0125";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0125",
                "ESTABLECIMIENTO",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.of(2026, 7, 6, 8, 30),
                "Distribuidora El Progreso",
                "CUIT 30-00001125-7",
                "Inspector Bromatología Demo",
                "Caso demo ACTA-0125: captura inicial del circuito de Bromatología.",
                BANDEJA_ACTAS_EN_ENRIQUECIMIENTO);
        store.getActas().put(id, acta);
        store.registrarDependenciaDemo(id, PrototipoStore.DependenciaActaDemo.BROMATOLOGIA);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0125-01", id,
                LocalDateTime.of(2026, 7, 6, 8, 40),
                "ALTA", "D1_CAPTURA", "D2_ENRIQUECIMIENTO",
                "Acta de bromatología ingresada; pendiente identificación del establecimiento."));
        store.getEventosPorActa().put(id, eventos);
        store.setAccionPendiente(id, "COMPLETAR_ENRIQUECIMIENTO");
    }
    /**
     * Caso continuidad de reingreso D: acta con resultadoFinal ABSUELTO en ARCHIVO.
     * Precargada en ARCHIVO para cubrir el caso de reingreso desde un estado absuelto.
     * Tras reingresar, la acta debe quedar cerrable=true sin pendientes bloqueantes.
     */
    private void cargarActa0131AbsueltoArchivoReingresoContinuidadDemo(PrototipoStore store) {
        String id = "ACTA-0131";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0131",
                "SEGURIDAD_VIAL",
                "ARCHIVO",
                "PENDIENTE_REVISION",
                "ARCHIVO",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                "Sosa, Andrea",
                "DNI 40.001.131",
                "Oficial Demo",
                "Caso demo ACTA-0131: absuelto en archivo; reingreso y continuidad caso D.",
                BANDEJA_ARCHIVO);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0131-01", id,
                LocalDateTime.of(2026, 5, 1, 9, 10),
                "ARCHIVADO_DESDE_ANALISIS_DIRECTO",
                "D5_ANALISIS", "ARCHIVO",
                "Archivado manualmente para demo de continuidad post-reingreso."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0131-01", id, "FALLO_ABSOLUTORIO", "FIRMADO",
                "fallo_absolutorio_0131.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setMotivoArchivo(id, "ARCHIVO_DESDE_ANALISIS_DIRECTO");
        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);
    }

    /**
     * Caso continuidad de reingreso G: acta con resultadoFinal CONDENA_FIRME y
     * situacionPagoCondena CONFIRMADO en ARCHIVO.
     * Tras reingresar, la acta debe quedar cerrable=true y sin gestion externa disponible.
     */
    private void cargarActa0132CondenaFirmeConfirmadoArchivoReingresoContinuidadDemo(PrototipoStore store) {
        String id = "ACTA-0132";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0132",
                "TRANSITO_URBANO",
                "ARCHIVO",
                "PENDIENTE_REVISION",
                "ARCHIVO",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                "Paz, Miguel",
                "DNI 41.001.132",
                "Oficial Demo",
                "Caso demo ACTA-0132: condena firme pago confirmado en archivo; reingreso caso G.",
                BANDEJA_ARCHIVO);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0132-01", id,
                LocalDateTime.of(2026, 5, 10, 10, 10),
                "ARCHIVADO_DESDE_ANALISIS_DIRECTO",
                "D5_ANALISIS", "ARCHIVO",
                "Archivado manualmente para demo de continuidad post-reingreso."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0132-01", id, "FALLO_CONDENATORIO", "FIRMADO",
                "fallo_condenatorio_0132.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setMotivoArchivo(id, "ARCHIVO_DESDE_ANALISIS_DIRECTO");
        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        store.setMontoCondenaDemo(id, java.math.BigDecimal.valueOf(75000));
        store.setSituacionPagoCondenaDemo(id, PrototipoStore.SituacionPagoCondena.CONFIRMADO);
    }

    /**
     * Caso continuidad de reingreso E: acta con resultadoFinal CONDENA_FIRME y
     * situacionPagoCondena PENDIENTE en ARCHIVO. Precargada directamente en ARCHIVO
     * porque el circuito normal bloquea archivar actas con CONDENA_FIRME (deben
     * resolverse por pago/gestión externa). Tras reingresar, gestión externa
     * debe estar disponible.
     */
    private void cargarActa0133CondenaFirmePendienteArchivoReingresoContinuidadDemo(PrototipoStore store) {
        String id = "ACTA-0133";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0133",
                "TRANSITO_URBANO",
                "ARCHIVO",
                "PENDIENTE_REVISION",
                "ARCHIVO",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 5, 12, 10, 0),
                "Ruiz, Laura",
                "DNI 42.001.133",
                "Oficial Demo",
                "Caso demo ACTA-0133: condena firme pago pendiente en archivo; reingreso caso E.",
                BANDEJA_ARCHIVO);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0133-01", id,
                LocalDateTime.of(2026, 5, 12, 10, 10),
                "ARCHIVADO_DESDE_ANALISIS_DIRECTO",
                "D5_ANALISIS", "ARCHIVO",
                "Archivado manualmente para demo de continuidad post-reingreso."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0133-01", id, "FALLO_CONDENATORIO", "FIRMADO",
                "fallo_condenatorio_0133.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setMotivoArchivo(id, "ARCHIVO_DESDE_ANALISIS_DIRECTO");
        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        store.setMontoCondenaDemo(id, java.math.BigDecimal.valueOf(60000));
        store.setSituacionPagoCondenaDemo(id, PrototipoStore.SituacionPagoCondena.PENDIENTE);
    }

    /**
     * Caso continuidad de reingreso F: acta con resultadoFinal CONDENA_FIRME y
     * situacionPagoCondena INFORMADO en ARCHIVO. Precargada directamente en ARCHIVO
     * porque el circuito normal bloquea archivar actas con CONDENA_FIRME. Tras
     * reingresar, deben estar disponibles confirmar y observar pago; gestión
     * externa no disponible.
     */
    private void cargarActa0134CondenaFirmeInformadoArchivoReingresoContinuidadDemo(PrototipoStore store) {
        String id = "ACTA-0134";
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0134",
                "TRANSITO_URBANO",
                "ARCHIVO",
                "PENDIENTE_REVISION",
                "ARCHIVO",
                false,
                true,
                true,
                false,
                LocalDateTime.of(2026, 5, 14, 11, 0),
                "Vega, Carlos",
                "DNI 43.001.134",
                "Oficial Demo",
                "Caso demo ACTA-0134: condena firme pago informado en archivo; reingreso caso F.",
                BANDEJA_ARCHIVO);
        store.getActas().put(id, acta);

        List<ActaEventoMock> eventos = new ArrayList<>();
        eventos.add(new ActaEventoMock(
                "EVT-0134-01", id,
                LocalDateTime.of(2026, 5, 14, 11, 10),
                "ARCHIVADO_DESDE_ANALISIS_DIRECTO",
                "D5_ANALISIS", "ARCHIVO",
                "Archivado manualmente para demo de continuidad post-reingreso."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0134-01", id, "FALLO_CONDENATORIO", "FIRMADO",
                "fallo_condenatorio_0134.pdf"));
        store.getDocumentosPorActa().put(id, docs);

        store.setMotivoArchivo(id, "ARCHIVO_DESDE_ANALISIS_DIRECTO");
        store.setResultadoFinalCierreDemo(id, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        store.setMontoCondenaDemo(id, java.math.BigDecimal.valueOf(55000));
        store.setSituacionPagoCondenaDemo(id, PrototipoStore.SituacionPagoCondena.INFORMADO);
    }
}
