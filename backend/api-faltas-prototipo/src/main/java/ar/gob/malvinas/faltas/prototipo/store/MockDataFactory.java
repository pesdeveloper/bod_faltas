package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaTransitoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public void loadInitialData(PrototipoStore store) {
        store.clearAll();

        cargarActa0001(store);
        cargarActa0002(store);
        cargarActa0003(store);
        cargarActa0004(store);
        cargarActa0005(store);
        cargarActa0006(store);
        cargarActa0007(store);
        cargarActa0008(store);
        cargarActa0009(store);
        cargarActa0010(store);
        cargarActa0011(store);
        cargarActa0012(store);
        cargarActa0013(store);
        cargarActa0014(store);
        cargarActa0015(store);
        cargarActa0016(store);
        cargarActa0017(store);
        cargarActa0018PagoVoluntarioDemo(store);
        cargarActa0019CerrabilidadMaterialesDemo(store);
        cargarActa0020OrigenMedidaDesdeGenerarMedidaDemo(store);
        cargarActa0021CerrabilidadMaterialesPagoConfirmadoDemo(store);
        cargarActa0022PagoRealYCerrabilidadMaterialDemo(store);
        cargarActa0023HechoMaterialVsDocumentoResolutorioDemo(store);
        cargarActa0024NacimientoCondicionesMaterialesPorConstatacionTempranaDemo(store);
        cargarActa0025SoloD1TrazasSinAnclasMaterialesDemo(store);
        cargarActa0026MedidaPreventivaPosteriorContravencionDemo(store);
    }

    private void cargarActa0001(PrototipoStore store) {
        String id = "ACTA-0001";
        String bandeja = BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
        ActaMock acta = new ActaMock(
                id,
                "A-2026-0001",
                "TRANSITO_URBANO",
                "D2_ENRIQUECIMIENTO",
                "EN_CURSO",
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
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(new ActaDocumentoMock(
                "DOC-0001-01",
                id,
                "FOTO_INFRACCION",
                "ADJUNTO",
                "foto_infraccion_0001.jpg"));
        store.getDocumentosPorActa().put(id, docs);
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
                "FALLO",
                "FIRMADO",
                "fallo_0017.pdf"));
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
     * {@code ABSUELTO} y tres orígenes con el mismo patrón: ancla documental en
     * el expediente + {@link PrototipoStore#reconocerOrigenBloqueanteMedidaPreventiva},
     * {@link PrototipoStore#reconocerOrigenBloqueanteSecuestroRodado} y
     * {@link PrototipoStore#reconocerOrigenBloqueanteRetencionDocumental}. Cada
     * bloqueo requiere documento resolutorio mock y luego cumplimiento material
     * vía API; la resolución documental no sustituye el hecho material. La rama
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
                "Caso demo ACTA-0019: absolución; medida, rodado y documentación (anclas + reconocimiento; mock).",
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
                "Precarga demo: resultado ABSUELTO. Tres orígenes vía reconocimiento sobre anclas de expediente (mock)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0019-00",
                        id,
                        "MEDIDA_PREVENTIVA",
                        "PENDIENTE_FIRMA",
                        "medida_preventiva_0019.pdf"));
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

        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado rMed =
                store.reconocerOrigenBloqueanteMedidaPreventiva(id);
        if (rMed.estado() != PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.OK) {
            throw new IllegalStateException(
                    "Carga demo ACTA-0019: reconocer medida preventiva, esperado OK, obtuvo: " + rMed.estado());
        }
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
     * mismo tronco operativo y mismas anclas/reconocimientos que
     * {@link #cargarActa0019CerrabilidadMaterialesDemo(PrototipoStore)}, con
     * {@code resultadoFinal} mock {@code PAGO_CONFIRMADO} precargado. Atajo
     * para probar material de cierre sin el circuito de pago; el recorrido
     * integrado pago real → {@code PAGO_CONFIRMADO} es {@code ACTA-0022}.
     * Misma API: pendientes → resolución documental → cumplimiento material →
     * {@code cerrable} → cierre vía {@code cerrarActaDesdeAnalisis}.
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
                "Caso demo ACTA-0021: pago confirmado; medida, rodado y documentación (anclas + reconocimiento; mock).",
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
                "Precarga demo: resultado PAGO_CONFIRMADO. Tres orígenes vía reconocimiento sobre anclas de expediente (mock)."));
        store.getEventosPorActa().put(id, eventos);

        List<ActaDocumentoMock> docs = new ArrayList<>();
        docs.add(
                new ActaDocumentoMock(
                        "DOC-0021-00",
                        id,
                        "MEDIDA_PREVENTIVA",
                        "PENDIENTE_FIRMA",
                        "medida_preventiva_0021.pdf"));
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

        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado rMed =
                store.reconocerOrigenBloqueanteMedidaPreventiva(id);
        if (rMed.estado() != PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.OK) {
            throw new IllegalStateException(
                    "Carga demo ACTA-0021: reconocer medida preventiva, esperado OK, obtuvo: " + rMed.estado());
        }
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
     * Caso demo e2e: {@code ACTA-0022} — mismas anclas de expediente que
     * {@link #cargarActa0021CerrabilidadMaterialesPagoConfirmadoDemo(PrototipoStore)}
     * ({@code MEDIDA_PREVENTIVA}, {@code ACTA_RETENCION},
     * {@code CONSTATACION_RETENCION_DOCUMENTACION}), partida en
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
     * <p>Modelo principal: retención de rodado, de documentación y medida
     * preventiva nacen como <b>datos de tránsito</b> (satélite
     * {@link ar.gob.malvinas.faltas.prototipo.domain.ActaTransitoMock} + anclas
     * en expediente coherentes con
     * {@link CerrabilidadSupport#ensureOrigenesSincronizados}. No replican en
     * el producto un “botón de constatación” del operador: el
     * {@code POST /acciones/registrar-constatacion-material-temprana} del
     * prototipo es mutación demo/técnica y pruebas (duplicado, D1/D2), no el
     * camino conceptual de este acta.
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
        ActaTransitoMock tr = new ActaTransitoMock(true, true, true, true);
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
                "Caso ACTA-0024: tránsito con retención rodado, documentación y medida (flags mock + anclas al nacimiento).",
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
}
