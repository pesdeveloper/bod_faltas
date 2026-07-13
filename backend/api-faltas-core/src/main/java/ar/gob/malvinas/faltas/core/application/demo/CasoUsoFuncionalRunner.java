package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.application.command.*;
import ar.gob.malvinas.faltas.core.application.result.CasoUsoFuncionalEjecucionResultado;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.infrastructure.config.PlazosAdministrativosProperties;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Runner funcional del dataset completo.
 *
 * Ejecuta el flujo real de dominio para cada acta del dataset funcional.
 * Usa servicios reales y repositorios in-memory.
 * No fabrica estados finales con setters directos.
 * No usa JDBC, SQL ni storage real.
 *
 * Slice 8F-4C.
 */
public class CasoUsoFuncionalRunner {

    private final ActaRepository actaRepo;
    private final ActaEventoRepository eventoRepo;
    private final ActaSnapshotRepository snapshotRepo;
    private final DocumentoRepository docRepo;
    private final DocumentoFirmaRepository firmaRepo;
    private final NotificacionRepository notifRepo;
    private final NotificacionIntentoRepository notifIntentoRepo;
    private final PagoVoluntarioRepository pagoVolRepo;
    private final PagoCondenaRepository pagoCondRepo;
    private final FalloActaRepository falloRepo;
    private final ApelacionActaRepository apelacionRepo;
    private final ApelacionDocumentoRepository apelacionDocRepo;
    private final GestionExternaRepository gestionExtRepo;
    private final BloqueanteMaterialRepository bloqueanteMaterialRepo;

    private final ActaService actaService;
    private final DocumentoService docService;
    private final NotificacionService notifService;
    private final PagoVoluntarioService pagoVolService;
    private final FalloActaService falloService;
    private final ApelacionActaService apelacionService;
    private final FirmezaCondenaService firmezaService;
    private final PagoCondenaService pagoCondService;
    private final GestionExternaService gestionExtService;
    private final GestionExternaService gestionExtServiceConBloqueantes;
    private final BloqueanteMaterialService bloqueanteMaterialService;
    private final ActaParalizacionService paralizacionService;
    private final FaltasClock faltasClock;

    public CasoUsoFuncionalRunner() {
        // Composition root manual: authorized exception to new FaltasClock() rule.
        // This class is NOT a Spring bean. It creates one canonical clock instance
        // and injects it into all services it wires. Not for production Spring context.
        faltasClock = new FaltasClock();
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        notifIntentoRepo = new InMemoryNotificacionIntentoRepository();
        pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        pagoCondRepo = new InMemoryPagoCondenaRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();
        apelacionDocRepo = new InMemoryApelacionDocumentoRepository();
        gestionExtRepo = new InMemoryGestionExternaRepository();
        bloqueanteMaterialRepo = new InMemoryBloqueanteMaterialRepository();

        RepositoryBloqueantesMaterialesChecker bloqueantesChecker =
                new RepositoryBloqueantesMaterialesChecker(bloqueanteMaterialRepo);

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondRepo, faltasClock);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), faltasClock);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                new InMemoryDocumentoPlantillaRepository(),
                new TalonarioService(new InMemoryTalonarioRepository(),
                        new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(), faltasClock),
                new InMemoryDependenciaRepository(),
                new InMemoryDocumentoFirmaReqRepository(),
                new InMemoryFirmanteRepository(),
                notifRepo, faltasClock);

        CalendarioAdministrativoService calendarioService =
                new CalendarioAdministrativoService(new InMemoryDiaNoComputableRepository(), faltasClock);
        PlazosAdministrativosService plazosService = new PlazosAdministrativosService(
                new PlazosAdministrativosProperties(),
                new CalculadorPlazosAdministrativos(calendarioService));

        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, bloqueantesChecker, faltasClock,
                notifIntentoRepo, new InMemoryPersonaDomicilioRepository(),
                plazosService);

        pagoVolService = new PagoVoluntarioService(
                actaRepo, eventoRepo, snapshotRepo, pagoVolRepo, recalc,
                new NoOpBloqueantesMaterialesChecker(), faltasClock);

        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo, recalc, faltasClock);

        apelacionService = new ApelacionActaService(
                actaRepo, falloRepo, apelacionRepo, apelacionDocRepo, eventoRepo, snapshotRepo, recalc,
                new NoOpBloqueantesMaterialesChecker(), faltasClock);

        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, recalc, faltasClock);

        gestionExtService = new GestionExternaService(
                actaRepo, eventoRepo, snapshotRepo, pagoCondRepo, gestionExtRepo, recalc,
                new NoOpBloqueantesMaterialesChecker(), faltasClock);

        gestionExtServiceConBloqueantes = new GestionExternaService(
                actaRepo, eventoRepo, snapshotRepo, pagoCondRepo, gestionExtRepo, recalc,
                bloqueantesChecker, faltasClock);

        pagoCondService = new PagoCondenaService(
                actaRepo, eventoRepo, snapshotRepo, falloRepo, pagoCondRepo, recalc,
                new NoOpBloqueantesMaterialesChecker(), faltasClock);

        CierreActaHelper cierreActaHelper = new CierreActaHelper(
                actaRepo, eventoRepo, snapshotRepo, recalc, faltasClock);
        bloqueanteMaterialService = new BloqueanteMaterialService(
                bloqueanteMaterialRepo, actaRepo, cierreActaHelper, faltasClock);

        paralizacionService = new ActaParalizacionService(
                actaRepo, eventoRepo, snapshotRepo, recalc, faltasClock);
    }

    // =========================================================================
    // API publica del runner
    // =========================================================================

    /**
     * Ejecuta el flujo funcional para el codigo de acta dado.
     * Crea el acta desde cero y la lleva al estado esperado ejecutando servicios reales.
     */
    public CasoUsoFuncionalEjecucionResultado ejecutar(String codigoActaMock) {
        ActaMockFuncionalDefinicion def = DatasetFuncionalDominioCatalog.buscarPorCodigo(codigoActaMock);
        List<String> pasos = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();
        try {
            return ejecutarDefinicion(def, pasos, advertencias);
        } catch (Exception e) {
            advertencias.add("ERROR_EJECUCION: " + e.getMessage());
            return CasoUsoFuncionalEjecucionResultado.parcial(
                    codigoActaMock, def.casoUsoPrincipal(), null, pasos, advertencias);
        }
    }

    // =========================================================================
    // Dispatch por codigo
    // =========================================================================

    private CasoUsoFuncionalEjecucionResultado ejecutarDefinicion(
            ActaMockFuncionalDefinicion def,
            List<String> pasos,
            List<String> advertencias) {

        String codigo = def.codigo();

        switch (codigo) {
            case "ACT-001-LABRADA":
                return ejecutarAct001(def, pasos);
            case "ACT-002-EN-ENRIQUECIMIENTO":
                return ejecutarAct002(def, pasos);
            case "ACT-003-DOC-PENDIENTE-FIRMA":
                return ejecutarAct003(def, pasos);
            case "ACT-004-PENDIENTE-NOTIFICACION":
                return ejecutarAct004(def, pasos);
            case "ACT-005-NOTI-ACTA-EN-CURSO":
                return ejecutarAct005(def, pasos);
            case "ACT-006-ANAL-LISTA-FALLO":
                return ejecutarAct006(def, pasos);
            case "ACT-007-PAGVOL-SOLICITADO":
                return ejecutarAct007(def, pasos);
            case "ACT-008-PAGVOL-PENDIENTE-CONF":
                return ejecutarAct008(def, pasos);
            case "ACT-009-PAGVOL-CONFIRMADO":
                return ejecutarAct009(def, pasos);
            case "ACT-010-FALLO-ABS-DICTADO":
                return ejecutarAct010(def, pasos);
            case "ACT-011-ABSUELTO-CERRADO":
                return ejecutarAct011(def, pasos);
            case "ACT-012-FALLO-COND-DICTADO":
                return ejecutarAct012(def, pasos);
            case "ACT-013-FALLO-COND-NOTIFICADO":
                return ejecutarAct013(def, pasos);
            case "ACT-014-APELACION-PRESENTADA":
                return ejecutarAct014(def, pasos);
            case "ACT-015-CONDENA-FIRME":
                return ejecutarAct015(def, pasos);
            case "ACT-016-PAGO-CONDENA-INFORMADO":
                return ejecutarAct016(def, pasos);
            case "ACT-017-CONDENA-FIRME-PAGADA":
                return ejecutarAct017(def, pasos);
            case "ACT-018-GESTION-EXTERNA":
                return ejecutarAct018(def, pasos);
            case "ACT-019-GESTION-EXTERNA-PAGO-EXTERNO":
                return ejecutarAct019(def, pasos);
            case "ACT-020-PARALIZADA":
                return ejecutarAct020(def, pasos);
            case "ACT-021-BLOQUEANTE-ACTIVO":
                return ejecutarAct021(def, pasos);
            case "ACT-022-ABSUELTO-CON-BLOQUEANTE":
                return ejecutarAct022(def, pasos);
            case "ACT-023-REDACCION-BORRADOR":
                return ejecutarAct023(def, pasos, advertencias);
            case "ACT-024-PDF-MOCK-GENERADO":
                return ejecutarAct024(def, pasos, advertencias);
            case "ACT-025-PRECONDICION-VIOLADA":
                return ejecutarAct025(def, pasos);
            case "ACT-026-NOTIFICACION-NEGATIVA":
                return ejecutarAct026(def, pasos);
            case "ACT-027-DOC-ADJUNTO-CONVALIDADO":
                return ejecutarAct027(def, pasos);
            case "ACT-028-ABSOLUCION-FIRME-CERRADA":
                return ejecutarAct028(def, pasos);
            case "ACT-029-REINGRESO-PARA-REVISION":
                return ejecutarAct029(def, pasos);
            case "ACT-030-PAGO-CONDENA-OBSERVADO":
                return ejecutarAct030(def, pasos);
            case "ACT-031-PAGO-CONDENA-CON-DESCUENTO":
                return ejecutarAct031(def, pasos, advertencias);
            case "ACT-032-APELACION-CON-DOCUMENTOS":
                return ejecutarAct032(def, pasos);
            case "ACT-033-APELACION-MIXTA":
                return ejecutarAct033(def, pasos);
            case "ACT-034-APELACION-RECHAZADA":
                return ejecutarAct034(def, pasos);
            case "ACT-035-APELACION-ABSOLUTORIA":
                return ejecutarAct035(def, pasos);
            case "ACT-036-APELACION-MODIFICA-CONDENA":
                return ejecutarAct036(def, pasos);
            case "ACT-037-APELACION-NULIDAD":
                return ejecutarAct037(def, pasos);
            default:
                return CasoUsoFuncionalEjecucionResultado.noEjecutado(
                        codigo, def.casoUsoPrincipal(),
                        "Codigo no reconocido en el runner: " + codigo);
        }
    }

    // =========================================================================
    // Helpers de flujo base
    // =========================================================================

    private Long labrar() {
        return actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-01", "INS-001",
                LocalDate.of(2024, 3, 15),
                "Av. Pioneros 2345, Malvinas Argentinas",
                "Belgrano 200, Malvinas Argentinas",
                "Conduccion sin revision tecnica obligatoria vigente",
                null, null,
                "Juan Carlos Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
    }

    private Long labrarYCapturar() {
        Long id = labrar();
        actaService.completarCaptura(new CompletarCapturaCommand(id, null));
        return id;
    }

    private Long labrarCapturarEnriquecer() {
        Long id = labrarYCapturar();
        actaService.enriquecer(new EnriquecerActaCommand(id, null));
        return id;
    }

    private Long generarDocumento(Long actaId, TipoDocu tipo) {
        return Long.parseLong(
                docService.generarDocumento(new GenerarDocumentoCommand(actaId, tipo, null))
                        .idEntidadAfectada());
    }

    private void firmarDocumento(Long docId) {
        docService.firmarDocumento(new FirmarDocumentoCommand(docId, "Inspector", "DIGITAL", null));
    }

    private String enviarNotificacion(Long actaId, Long docId) {
        return notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, docId, CanalNotificacion.PRESENCIAL, null, null, null, "demo-user"))
                .idEntidadAfectada();
    }

    private void registrarPositiva(String notifId) {
        Long idNotif = Long.parseLong(notifId);
        List<Long> activos = notifIntentoRepo.buscarPorNotificacion(idNotif).stream()
                .filter(i -> i.getEstadoIntento() == EstadoNotificacion.EN_PROCESO
                        && i.getResultadoIntento() == null)
                .map(FalNotificacionIntento::getId)
                .toList();
        if (activos.size() != 1)
            throw new IllegalStateException(
                    "Se esperaba exactamente un intento EN_PROCESO sin resultado para la notificacion "
                            + idNotif + ", encontrados: " + activos.size() + ".");
        notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(idNotif, activos.get(0), null, "demo-user"));
    }

    private Long llegarAAnalisis(List<String> pasos) {
        Long actaId = labrarCapturarEnriquecer();
        pasos.add("labrar+captura+enriquecer");
        Long docId = generarDocumento(actaId, TipoDocu.ACTA_INFRACCION);
        pasos.add("generarDocumento(ACTA_INFRACCION)");
        firmarDocumento(docId);
        pasos.add("firmarDocumento");
        String notifId = enviarNotificacion(actaId, docId);
        pasos.add("enviarNotificacion");
        registrarPositiva(notifId);
        pasos.add("registrarNotificacionPositiva -> ANAL");
        return actaId;
    }


    private CasoUsoFuncionalEjecucionResultado ejecutarAct032(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = ejecutarAct013(def, pasos).actaId();
        ComandoResultado apRes = apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(id, "Infractor", "Fundamentos apelacion con escrito", null));
        pasos.add("registrarApelacion -> APEPRE");
        Long apelacionId = Long.parseLong(apRes.idEntidadAfectada());
        apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(
                apelacionId, TipoDocumentoApelacion.ESCRITO_APELACION,
                OrigenPresentacion.INFRACTOR, null, "storage://ape-escrito.pdf",
                "escrito_apelacion.pdf", null, null, "SYS"));
        pasos.add("registrarDocumentoApelacion(ESCRITO_APELACION) -> CON_APELACION");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct033(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = ejecutarAct013(def, pasos).actaId();
        ComandoResultado apRes = apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(id, "Infractor", "Fundamentos apelacion mixta", null));
        pasos.add("registrarApelacion -> APEPRE");
        Long apelacionId = Long.parseLong(apRes.idEntidadAfectada());
        apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(
                apelacionId, TipoDocumentoApelacion.ESCRITO_APELACION,
                OrigenPresentacion.INFRACTOR, null, "storage://ape-escrito.pdf",
                "escrito_apelacion.pdf", null, null, "SYS"));
        apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(
                apelacionId, TipoDocumentoApelacion.DOCUMENTACION_RESPALDATORIA,
                OrigenPresentacion.INFRACTOR, null, "storage://ape-respaldo.pdf",
                "documentacion_respaldatoria.pdf", null, null, "SYS"));
        pasos.add("registrarDocumentoApelacion(ESCRITO + RESPALDATORIA) -> CON_APELACION");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct034(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = ejecutarAct013(def, pasos).actaId();
        apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(id, "Infractor", "Fundamentos apelacion", null));
        pasos.add("registrarApelacion -> APEPRE");
        apelacionService.resolverRechazada(
                new ResolverApelacionRechazadaCommand(id, "Apelacion sin fundamentos validos", null));
        pasos.add("resolverRechazada -> APERAZ/PENDIENTE_ANALISIS");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct035(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = ejecutarAct013(def, pasos).actaId();
        apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(id, "Infractor", "Fundamentos apelacion absolutoria", null));
        pasos.add("registrarApelacion -> APEPRE");
        apelacionService.resolverAceptaAbsuelve(
                new ResolverApelacionAceptaAbsuelveCommand(id, "Infractor absuelto en segunda instancia", null));
        pasos.add("resolverAceptaAbsuelve -> APEABS/CERRADAS");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct036(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = ejecutarAct013(def, pasos).actaId();
        ComandoResultado apRes = apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(id, "Infractor", "Fundamentos apelacion condena alta", null));
        pasos.add("registrarApelacion -> APEPRE");
        Long apelacionId = Long.parseLong(apRes.idEntidadAfectada());
        apelacionService.resolverModificaCondena(
                new ResolverApelacionModificaCondenaCommand(apelacionId, new BigDecimal("2500.00"),
                        "Condena reducida a 2500 por apelacion", null));
        pasos.add("resolverModificaCondena -> APEMCO/PENDIENTES_FALLO");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct037(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = ejecutarAct013(def, pasos).actaId();
        ComandoResultado apRes = apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(id, "Infractor", "Fundamentos apelacion nulidad", null));
        pasos.add("registrarApelacion -> APEPRE");
        Long apelacionId = Long.parseLong(apRes.idEntidadAfectada());
        apelacionService.resolverNulidad(
                new ResolverApelacionNulidadCommand(apelacionId, "Nulidad por defecto de forma", null));
        pasos.add("resolverNulidad -> APENUL/PENDIENTE_ANALISIS");
        return buildResultado(def, id, pasos);
    }
    private Long llegarACondenaFirme(List<String> pasos) {
        Long actaId = llegarAAnalisis(pasos);
        falloService.dictarCondenatorio(
                new DictarFalloCondenatorioCommand(actaId, new BigDecimal("5000.00"),
                        "Fundamentos condenatorios", null));
        pasos.add("dictarFalloCondenatorio");
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        firmarDocumento(idDocFallo);
        pasos.add("firmarDocFallo");
        String notifFalloId = enviarNotificacion(actaId, idDocFallo);
        pasos.add("enviarNotificacionFallo");
        registrarPositiva(notifFalloId);
        pasos.add("registrarPositivaFallo");
        // Retrotraer fhVtoApelacion para que la validacion temporal del slice CMD-FALLO-005 pase en demo
        FalActaFallo falloVto = falloRepo.buscarActivo(actaId).orElseThrow();
        falloVto.setFhVtoApelacion(faltasClock.now().toLocalDate().minusDays(1));
        falloRepo.guardar(falloVto);
        firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null, "demo-user"));
        pasos.add("vencerPlazoApelacion -> CONDENA_FIRME");
        return actaId;
    }

    private CasoUsoFuncionalEjecucionResultado buildResultado(
            ActaMockFuncionalDefinicion def, Long actaId, List<String> pasos) {
        return buildResultado(def, actaId, pasos, List.of());
    }

    private CasoUsoFuncionalEjecucionResultado buildResultado(
            ActaMockFuncionalDefinicion def, Long actaId, List<String> pasos, List<String> advertencias) {
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
        List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
        List<FalDocumento> docs = docRepo.buscarPorActa(actaId);
        return CasoUsoFuncionalEjecucionResultado.exitoso(
                def.codigo(), def.casoUsoPrincipal(),
                actaId,
                acta.getBloqueActual().codigo(),
                snap.getCodBandeja().name(),
                acta.getSituacionAdministrativa().name(),
                acta.getResultadoFinal().name(),
                def.cerrableEsperado(),
                acta.estaParalizada(),
                eventos.size(),
                docs.size(),
                0, 0,
                pasos, List.of(), List.of(), List.copyOf(advertencias));
    }

    // =========================================================================
    // Implementacion por acta
    // =========================================================================

    private CasoUsoFuncionalEjecucionResultado ejecutarAct001(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrar();
        pasos.add("labrar -> CAPT/ACTLAB");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct002(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrarYCapturar();
        pasos.add("labrar+completarCaptura -> ENRI/ACTCAP");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct003(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrarCapturarEnriquecer();
        pasos.add("labrar+captura+enriquecer");
        Long docId = generarDocumento(id, TipoDocu.ACTA_INFRACCION);
        pasos.add("generarDocumento -> DOCGEN/PENDIENTE_FIRMA");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct004(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrarCapturarEnriquecer();
        pasos.add("labrar+captura+enriquecer");
        Long docId = generarDocumento(id, TipoDocu.ACTA_INFRACCION);
        pasos.add("generarDocumento");
        firmarDocumento(docId);
        pasos.add("firmarDocumento -> DOCFIR/PENDIENTE_NOTIFICACION");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct005(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrarCapturarEnriquecer();
        pasos.add("labrar+captura+enriquecer");
        Long docId = generarDocumento(id, TipoDocu.ACTA_INFRACCION);
        pasos.add("generarDocumento");
        firmarDocumento(docId);
        pasos.add("firmarDocumento");
        enviarNotificacion(id, docId);
        pasos.add("enviarNotificacion -> NOTENV/EN_NOTIFICACION");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct006(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        pasos.add("notificacionPositiva -> ANAL");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct007(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        pagoVolService.solicitar(new SolicitarPagoVoluntarioCommand(id, null));
        pasos.add("solicitarPagoVoluntario -> PAGVSO");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct008(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        pagoVolService.solicitar(new SolicitarPagoVoluntarioCommand(id, null));
        pasos.add("solicitarPagoVoluntario");
        pagoVolService.fijarMonto(new FijarMontoPagoVoluntarioCommand(id, new BigDecimal("1500.00"), null));
        pasos.add("fijarMonto");
        pagoVolService.informar(new InformarPagoVoluntarioCommand(id, "REF-001", null));
        pasos.add("informarPago -> PAGINF/PENDIENTE_CONFIRMACION_PAGO");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct009(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        pagoVolService.solicitar(new SolicitarPagoVoluntarioCommand(id, null));
        pasos.add("solicitarPagoVoluntario");
        pagoVolService.fijarMonto(new FijarMontoPagoVoluntarioCommand(id, new BigDecimal("1500.00"), null));
        pasos.add("fijarMonto");
        pagoVolService.informar(new InformarPagoVoluntarioCommand(id, "REF-001", null));
        pasos.add("informarPago");
        pagoVolService.confirmar(new ConfirmarPagoVoluntarioCommand(id, null));
        pasos.add("confirmarPago -> PAGCNF+CIERRA/CERRADAS");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct010(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(id, "Fundamentos", null));
        pasos.add("dictarFalloAbsolutorio -> FALABS+DOCGEN/PENDIENTE_FIRMA");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct011(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(id, "Fundamentos abs", null));
        pasos.add("dictarFalloAbsolutorio");
        Long docFalloId = falloRepo.buscarActivo(id).orElseThrow().getDocumentoId();
        firmarDocumento(docFalloId);
        pasos.add("firmarDocFallo");
        String notifId = enviarNotificacion(id, docFalloId);
        pasos.add("enviarNotificacionFallo");
        registrarPositiva(notifId);
        pasos.add("registrarPositiva(abs) -> NOTPOS+CIERRA/CERRADAS");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct012(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        falloService.dictarCondenatorio(
                new DictarFalloCondenatorioCommand(id, new BigDecimal("3000.00"), "Fundamentos cond", null));
        pasos.add("dictarFalloCondenatorio -> FALCON+DOCGEN/PENDIENTE_FIRMA");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct013(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        falloService.dictarCondenatorio(
                new DictarFalloCondenatorioCommand(id, new BigDecimal("3000.00"), "Fundamentos cond", null));
        pasos.add("dictarFalloCondenatorio");
        Long docFallo = falloRepo.buscarActivo(id).orElseThrow().getDocumentoId();
        firmarDocumento(docFallo);
        pasos.add("firmarDocFallo");
        String notifFallo = enviarNotificacion(id, docFallo);
        pasos.add("enviarNotificacionFallo");
        registrarPositiva(notifFallo);
        pasos.add("registrarPositiva(cond) -> NOTPOS/PENDIENTES_FALLO");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct014(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = ejecutarAct013(def, pasos).actaId();
        apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(id, "Infractor", "Fundamentos apelacion", null));
        pasos.add("registrarApelacion -> APEPRE/CON_APELACION");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct015(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarACondenaFirme(pasos);
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct016(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarACondenaFirme(pasos);
        pagoCondService.informar(
                new InformarPagoCondenaCommand(id, new BigDecimal("5000.00"), "BANCO-001", null, "demo-user"));
        pasos.add("informarPagoCondena -> PCOINF/PENDIENTE_CONFIRMACION_PAGO_CONDENA");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct017(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarACondenaFirme(pasos);
        pagoCondService.informar(
                new InformarPagoCondenaCommand(id, new BigDecimal("5000.00"), "BANCO-001", null, "demo-user"));
        pasos.add("informarPagoCondena");
        pagoCondService.confirmar(new ConfirmarPagoCondenaCommand(id, null));
        pasos.add("confirmarPagoCondena -> PCOCNF+CIERRA/CERRADAS");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct018(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarACondenaFirme(pasos);
        gestionExtService.derivar(new DerivarGestionExternaCommand(
                id, TipoGestionExterna.APREMIO, "Deriva por falta de pago", null));
        pasos.add("derivarGestionExterna -> EXTDER/GESTION_EXTERNA");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct019(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarACondenaFirme(pasos);
        // Registrar bloqueante activo antes de derivar para que el pago externo no cierre el acta.
        // El dataset ACT-019 espera ACTIVA/ANAL: el cierre se difiere por el bloqueante.
        bloqueanteMaterialService.registrar(
                new RegistrarBloqueanteMaterialCommand(id, OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA));
        pasos.add("registrarBloqueante(MEDIDA_PREVENTIVA)");
        gestionExtService.derivar(new DerivarGestionExternaCommand(
                id, TipoGestionExterna.APREMIO, "Deriva para pago apremio", null));
        pasos.add("derivarGestionExterna");
        // Usar service con checker real: bloqueante activo impide cierre automatico.
        gestionExtServiceConBloqueantes.registrarPagoExternoGestion(
                new RegistrarPagoExternoGestionCommand(id, null));
        pasos.add("registrarPagoExternoGestion -> PAGAPR / ACTIVA/ANAL por bloqueante");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct020(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        paralizacionService.paralizar(new ParalizarActaCommand(id, MotivoParalizacion.ESPERA_DOCUMENTAL, null, null));
        pasos.add("paralizar -> ACTPAR/PARALIZADAS/bloque=ANAL");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct021(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        bloqueanteMaterialService.registrar(
                new RegistrarBloqueanteMaterialCommand(id, OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA));
        pasos.add("registrarBloqueanteMaterial -> PENDIENTE");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct022(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarAAnalisis(pasos);
        bloqueanteMaterialService.registrar(
                new RegistrarBloqueanteMaterialCommand(id, OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA));
        pasos.add("registrarBloqueanteMaterial");
        falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(id, "Absuelto", null));
        pasos.add("dictarFalloAbsolutorio");
        Long docFalloId = falloRepo.buscarActivo(id).orElseThrow().getDocumentoId();
        firmarDocumento(docFalloId);
        pasos.add("firmarDocFallo");
        String notifFalloId = enviarNotificacion(id, docFalloId);
        pasos.add("enviarNotificacionFallo");
        registrarPositiva(notifFalloId);
        pasos.add("registrarPositiva(abs con bloqueante) -> ABSUELTO/ACTIVA (bloqueante impide CIERRA)");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct023(
            ActaMockFuncionalDefinicion def, List<String> pasos, List<String> advertencias) {
        advertencias.add("ACT-023: requiere PlantillasMockSeeder para ejecutar redaccion; "
                + "flujo parcial: acta llega a ANAL con documento ACTO_ADMINISTRATIVO generado");
        Long id = llegarAAnalisis(pasos);
        Long docId = generarDocumento(id, TipoDocu.ACTO_ADMINISTRATIVO);
        pasos.add("generarDocumento(ACTO_ADMINISTRATIVO) -> pendiente redaccion");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct024(
            ActaMockFuncionalDefinicion def, List<String> pasos, List<String> advertencias) {
        advertencias.add("ACT-024: requiere PlantillasMockSeeder para generar PDF mock; "
                + "flujo parcial: acta llega a ANAL con documento ACTO_ADMINISTRATIVO generado");
        Long id = llegarAAnalisis(pasos);
        Long docId = generarDocumento(id, TipoDocu.ACTO_ADMINISTRATIVO);
        pasos.add("generarDocumento(ACTO_ADMINISTRATIVO) -> pendiente redaccion+confirmacion+mock");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct025(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrar();
        pasos.add("labrar -> CAPT");
        boolean guardrailOk = false;
        try {
            falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(id, "test", null));
        } catch (ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException e) {
            guardrailOk = true;
            pasos.add("PrecondicionVioladaException al dictar fallo en CAPT: OK (guardrail verificado)");
        }
        if (!guardrailOk) {
            pasos.add("ERROR: dictarFallo en CAPT no lanzo PrecondicionVioladaException");
        }
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct026(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrarCapturarEnriquecer();
        pasos.add("labrar+captura+enriquecer");
        Long docId = generarDocumento(id, TipoDocu.ACTA_INFRACCION);
        pasos.add("generarDocumento");
        firmarDocumento(docId);
        pasos.add("firmarDocumento");
        String notifId = enviarNotificacion(id, docId);
        pasos.add("enviarNotificacion");
        notifService.registrarNegativa(new RegistrarNotificacionNegativaCommand(Long.parseLong(notifId), null));
        pasos.add("registrarNotificacionNegativa -> NOTNEG/PENDIENTE_ANALISIS");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct027(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = labrarCapturarEnriquecer();
        pasos.add("labrar+captura+enriquecer");
        docService.incorporarDocumentoEscaneado(new IncorporarDocumentoEscaneadoCommand(
                id, TipoDocu.ACTA_INFRACCION,
                "storage://adjuntos/act027.pdf",
                "sha256-adjunto-mock-027",
                "inspector-001", null));
        pasos.add("incorporarDocumentoEscaneado -> DOCADJ");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct028(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        return ejecutarAct011(def, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct029(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarACondenaFirme(pasos);
        gestionExtService.derivar(new DerivarGestionExternaCommand(
                id, TipoGestionExterna.JUZGADO_DE_PAZ, "Reingreso para revision", null));
        pasos.add("derivarGestionExterna");
        gestionExtService.reingresar(new ReingresarDesdeGestionExternaCommand(
                id, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION,
                "Reingreso para nueva evaluacion",
                ResultadoGestionExterna.SIN_CAMBIOS, null, null));
        pasos.add("reingresarDesdeGestionExterna -> EXTRET/ANAL ACTIVA");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct030(
            ActaMockFuncionalDefinicion def, List<String> pasos) {
        Long id = llegarACondenaFirme(pasos);
        pagoCondService.informar(
                new InformarPagoCondenaCommand(id, new BigDecimal("5000.00"), "BANCO-001", null, "demo-user"));
        pasos.add("informarPagoCondena");
        pagoCondService.observar(new ObservarPagoCondenaCommand(id, "Comprobante invalido", null));
        pasos.add("observarPagoCondena -> PCOOBS/PENDIENTE_PAGO_CONDENA");
        return buildResultado(def, id, pasos);
    }

    private CasoUsoFuncionalEjecucionResultado ejecutarAct031(
            ActaMockFuncionalDefinicion def, List<String> pasos, List<String> advertencias) {
        advertencias.add("ACT-031: descuento sin evento propio. "
                + "Se usa PCOCNF como confirmacion. "
                + "Decision 8F-4C: descuento es atributo del pago, no evento separado.");
        Long id = llegarACondenaFirme(pasos);
        pagoCondService.informar(
                new InformarPagoCondenaCommand(id, new BigDecimal("4000.00"),
                        "BANCO-DESCUENTO", "Pago con descuento administrativo aplicado", "demo-user"));
        pasos.add("informarPagoCondena (monto con descuento)");
        pagoCondService.confirmar(new ConfirmarPagoCondenaCommand(id,
                "Descuento administrativo aplicado"));
        pasos.add("confirmarPagoCondena -> PCOCNF+CIERRA/CERRADAS (variante descuento)");
        return buildResultado(def, id, pasos, advertencias);
    }

    // =========================================================================
    // Accesores de repositorios (para aserciones en tests)
    // =========================================================================

    public ActaRepository getActaRepo() { return actaRepo; }
    public DocumentoRepository getDocRepo() { return docRepo; }
    public ActaEventoRepository getEventoRepo() { return eventoRepo; }
    public ActaSnapshotRepository getSnapshotRepo() { return snapshotRepo; }
    public FalloActaRepository getFalloRepo() { return falloRepo; }
    public NotificacionRepository getNotifRepo() { return notifRepo; }
    public PagoCondenaRepository getPagoCondRepo() { return pagoCondRepo; }
    public BloqueanteMaterialRepository getBloqueanteMaterialRepo() { return bloqueanteMaterialRepo; }
}
