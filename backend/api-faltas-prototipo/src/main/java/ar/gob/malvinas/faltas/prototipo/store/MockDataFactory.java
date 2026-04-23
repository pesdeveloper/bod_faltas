package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
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
}
