package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.AlcoholemiaMedicionItem;
import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.ContravencionActaItem;
import ar.gob.malvinas.faltas.core.application.command.DomicilioActaItem;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.MedidaPreventivaActaItem;
import ar.gob.malvinas.faltas.core.application.command.SustanciasAlimenticiasActaItem;
import ar.gob.malvinas.faltas.core.application.command.TransitoActaItem;
import ar.gob.malvinas.faltas.core.application.command.VehiculoActaItem;
import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.FiltroActa;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSustanciasAlimenticias;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransito;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransitoAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.model.FalActaVehiculo;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaContravencionRepository;
import ar.gob.malvinas.faltas.core.repository.ActaMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSustanciasAlimenticiasRepository;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoAlcoholemiaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaVehiculoRepository;
import ar.gob.malvinas.faltas.core.repository.MedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActaService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final ActaEvidenciaRepository evidenciaRepository;
    private final FaltasClock faltasClock;
    private final QrActaService qrActaService;
    private final ObservacionRepository observacionRepository;
    private PersonaRepository personaRepository;

    /**
     * Satélites obligatorios del alta integral (Wave 4 PARTE-3A, {@code DECISION-ACTA-DEPS-01}).
     * Antes de PARTE-3A estos cuatro colaboradores eran campos
     * {@code @Autowired(required = false)} para no romper la construcción directa de
     * {@code ActaService} en tests unitarios preexistentes. El prompt de PARTE-3A §3.5 prohíbe
     * esa opcionalidad para participantes del alta: la opcionalidad correcta es la presencia o
     * aplicabilidad del bloque de datos en el comando, no la existencia del bean. Un bean
     * ausente ya no es un camino posible: el bean siempre existe; el bloque de datos en el
     * comando decide si se persiste algo.
     */
    private final PersonaDomicilioRepository personaDomicilioRepository;

    private final ActaArticuloInfringidoRepository articuloInfringidoRepository;

    /**
     * Calculador/persistidor de valorizacion (Wave 4 PARTE-2, DECISION-ACTA-VALORIZACION-REUTILIZABLE-01).
     * Se invoca solo si hubo articulos informados: sin articulos no hay nada que valorizar.
     */
    private final ValorizacionService valorizacionService;

    /**
     * Numeracion electronica del acta (Wave 4 PARTE-2 S8). Se invoca al FINAL de la transaccion,
     * justo antes de ACTLAB y el snapshot (seccion critica breve: solo bloquea la fila del
     * talonario). Desde Wave 4 Parte 3C ({@code DECISION-ACTA-TALONARIO-REQUIRED-01}), si no
     * hay exactamente un talonario ACTA ELECTRONICO activo compatible con la dependencia/
     * tipoActa, el alta completa se rechaza (0, 2+ o agotado): no existe alta canonica sin
     * numero por falta de configuracion electronica.
     */
    private final TalonarioService talonarioService;

    /**
     * Seis satélites completos del alta integral (Wave 4 PARTE-3A §11). Mismo régimen de
     * obligatoriedad que los cuatro colaboradores de arriba: el bean siempre existe; la
     * opcionalidad es del bloque de datos correspondiente en el comando.
     */
    private final ActaVehiculoRepository actaVehiculoRepository;
    private final ActaTransitoRepository actaTransitoRepository;
    private final ActaTransitoAlcoholemiaRepository actaTransitoAlcoholemiaRepository;
    private final ActaContravencionRepository actaContravencionRepository;
    private final ActaSustanciasAlimenticiasRepository actaSustanciasAlimenticiasRepository;
    private final ActaMedidaPreventivaRepository actaMedidaPreventivaRepository;

    /**
     * Catalogo de medidas preventivas ({@code fal_medida_preventiva}). Reemplaza a
     * {@code ArticuloMedidaPreventivaRepository} desde Wave 4 Parte 3B §7
     * (DECISION-MEDIDA-PREVENTIVA-LIBRE-01): la medida ya no se valida contra la relacion
     * articulo-medida, sino directamente contra su propia existencia/vigencia en el catalogo.
     */
    private final MedidaPreventivaRepository medidaPreventivaRepository;

    /**
     * Coordinador de fotografias del alta (Wave 4 Parte 3D). Obligatorio como bean (siempre
     * presente); la opcionalidad es del bloque {@code fotografias} del comando, no del bean,
     * mismo regimen que los satelites de PARTE-3A.
     */
    private final FotografiaEvidenciaService fotografiaEvidenciaService;

    /**
     * @param qrActaService              unico productor del token QR canonico; obligatorio, porque
     *                                   el alta no puede construir un acta sin {@code codigo_qr}
     * @param observacionRepository      puerto de {@code fal_observacion}; obligatorio, porque el
     *                                   alta no puede quedar sin la observacion inicial informada
     * @param personaDomicilioRepository puerto de domicilios de la persona infractora; obligatorio
     *                                   desde PARTE-3A (bean siempre presente; la opcionalidad es
     *                                   del bloque {@code domicilios} del comando, no del bean)
     * @param articuloInfringidoRepository puerto de artículos infringidos; idem
     * @param valorizacionService        calculador/persistidor de valorización; idem
     * @param talonarioService           numeración electrónica; idem
     * @param actaVehiculoRepository     satélite vehículo; idem
     * @param actaTransitoRepository     satélite tránsito; idem
     * @param actaTransitoAlcoholemiaRepository mediciones de alcoholemia; idem
     * @param actaContravencionRepository satélite contravención/comercio; idem
     * @param actaSustanciasAlimenticiasRepository satélite sustancias alimenticias; idem
     * @param actaMedidaPreventivaRepository medidas preventivas aplicadas; idem
     * @param medidaPreventivaRepository  catálogo de medidas preventivas; idem
     */
    public ActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository,
            FaltasClock faltasClock,
            QrActaService qrActaService,
            ObservacionRepository observacionRepository,
            PersonaDomicilioRepository personaDomicilioRepository,
            ActaArticuloInfringidoRepository articuloInfringidoRepository,
            ValorizacionService valorizacionService,
            TalonarioService talonarioService,
            ActaVehiculoRepository actaVehiculoRepository,
            ActaTransitoRepository actaTransitoRepository,
            ActaTransitoAlcoholemiaRepository actaTransitoAlcoholemiaRepository,
            ActaContravencionRepository actaContravencionRepository,
            ActaSustanciasAlimenticiasRepository actaSustanciasAlimenticiasRepository,
            ActaMedidaPreventivaRepository actaMedidaPreventivaRepository,
            MedidaPreventivaRepository medidaPreventivaRepository,
            FotografiaEvidenciaService fotografiaEvidenciaService) {
        if (qrActaService == null) {
            throw new IllegalArgumentException(
                    "qrActaService es obligatorio: el alta del acta exige codigo_qr canonico.");
        }
        if (observacionRepository == null) {
            throw new IllegalArgumentException(
                    "observacionRepository es obligatorio: el alta del acta persiste la"
                            + " observacion inicial en fal_observacion.");
        }
        if (personaDomicilioRepository == null) {
            throw new IllegalArgumentException(
                    "personaDomicilioRepository es obligatorio: participante del alta integral"
                            + " (DECISION-ACTA-DEPS-01). Su bloque en el comando puede estar vacio;"
                            + " el bean no puede ser nulo.");
        }
        if (articuloInfringidoRepository == null) {
            throw new IllegalArgumentException(
                    "articuloInfringidoRepository es obligatorio: participante del alta integral"
                            + " (DECISION-ACTA-DEPS-01).");
        }
        if (valorizacionService == null) {
            throw new IllegalArgumentException(
                    "valorizacionService es obligatorio: participante del alta integral"
                            + " (DECISION-ACTA-DEPS-01).");
        }
        if (talonarioService == null) {
            throw new IllegalArgumentException(
                    "talonarioService es obligatorio: participante del alta integral"
                            + " (DECISION-ACTA-DEPS-01).");
        }
        if (actaVehiculoRepository == null) {
            throw new IllegalArgumentException(
                    "actaVehiculoRepository es obligatorio: satelite del alta integral (PARTE-3A).");
        }
        if (actaTransitoRepository == null) {
            throw new IllegalArgumentException(
                    "actaTransitoRepository es obligatorio: satelite del alta integral (PARTE-3A).");
        }
        if (actaTransitoAlcoholemiaRepository == null) {
            throw new IllegalArgumentException(
                    "actaTransitoAlcoholemiaRepository es obligatorio: satelite del alta integral (PARTE-3A).");
        }
        if (actaContravencionRepository == null) {
            throw new IllegalArgumentException(
                    "actaContravencionRepository es obligatorio: satelite del alta integral (PARTE-3A).");
        }
        if (actaSustanciasAlimenticiasRepository == null) {
            throw new IllegalArgumentException(
                    "actaSustanciasAlimenticiasRepository es obligatorio: satelite del alta integral (PARTE-3A).");
        }
        if (actaMedidaPreventivaRepository == null) {
            throw new IllegalArgumentException(
                    "actaMedidaPreventivaRepository es obligatorio: satelite del alta integral (PARTE-3A).");
        }
        if (medidaPreventivaRepository == null) {
            throw new IllegalArgumentException(
                    "medidaPreventivaRepository es obligatorio: catalogo del alta integral (PARTE-3A/3B).");
        }
        if (fotografiaEvidenciaService == null) {
            throw new IllegalArgumentException(
                    "fotografiaEvidenciaService es obligatorio: coordinador de fotografias del alta (Wave 4 Parte 3D).");
        }
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.evidenciaRepository = evidenciaRepository;
        this.faltasClock = faltasClock;
        this.qrActaService = qrActaService;
        this.observacionRepository = observacionRepository;
        this.personaDomicilioRepository = personaDomicilioRepository;
        this.articuloInfringidoRepository = articuloInfringidoRepository;
        this.valorizacionService = valorizacionService;
        this.talonarioService = talonarioService;
        this.actaVehiculoRepository = actaVehiculoRepository;
        this.actaTransitoRepository = actaTransitoRepository;
        this.actaTransitoAlcoholemiaRepository = actaTransitoAlcoholemiaRepository;
        this.actaContravencionRepository = actaContravencionRepository;
        this.actaSustanciasAlimenticiasRepository = actaSustanciasAlimenticiasRepository;
        this.actaMedidaPreventivaRepository = actaMedidaPreventivaRepository;
        this.medidaPreventivaRepository = medidaPreventivaRepository;
        this.fotografiaEvidenciaService = fotografiaEvidenciaService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository,
            PersonaRepository personaRepository,
            FaltasClock faltasClock,
            QrActaService qrActaService,
            ObservacionRepository observacionRepository,
            PersonaDomicilioRepository personaDomicilioRepository,
            ActaArticuloInfringidoRepository articuloInfringidoRepository,
            ValorizacionService valorizacionService,
            TalonarioService talonarioService,
            ActaVehiculoRepository actaVehiculoRepository,
            ActaTransitoRepository actaTransitoRepository,
            ActaTransitoAlcoholemiaRepository actaTransitoAlcoholemiaRepository,
            ActaContravencionRepository actaContravencionRepository,
            ActaSustanciasAlimenticiasRepository actaSustanciasAlimenticiasRepository,
            ActaMedidaPreventivaRepository actaMedidaPreventivaRepository,
            MedidaPreventivaRepository medidaPreventivaRepository,
            FotografiaEvidenciaService fotografiaEvidenciaService) {
        this(actaRepository, eventoRepository, snapshotRepository, snapshotRecalculador,
                evidenciaRepository, faltasClock, qrActaService, observacionRepository,
                personaDomicilioRepository, articuloInfringidoRepository, valorizacionService,
                talonarioService, actaVehiculoRepository, actaTransitoRepository,
                actaTransitoAlcoholemiaRepository, actaContravencionRepository,
                actaSustanciasAlimenticiasRepository, actaMedidaPreventivaRepository,
                medidaPreventivaRepository, fotografiaEvidenciaService);
        this.personaRepository = personaRepository;
    }

    /**
     * Alta completa del acta.
     *
     * <p>Materializa la secuencia observable de {@code 20-application/command-contracts.md}:
     *
     * <pre>
     * validaciones previas a toda mutacion
     * 1. capturar actor e instante una sola vez
     * 2. resolver o crear persona si la situacion lo exige
     * 3. generar id_tecnico
     * 4. generar codigo_qr desde id_tecnico
     * 5. construir FalActa con actor, QR y version informados
     * 6. guardar FalActa y obtener la PK
     * 7. insertar la observacion inicial si el texto es no blanco
     * 8. persistir evidencias, ACTLAB y snapshot con el mismo instante
     * </pre>
     *
     * <p>Todas las precondiciones se verifican antes de la primera mutacion. La persona
     * infractora se resuelve o se crea antes de construir {@link FalActa}, de modo que el
     * agregado nunca existe con una combinacion situacion/persona imposible, ni siquiera
     * transitoriamente.
     *
     * <p>{@code id_user_alta} es el {@code sub} real del actor autenticado: no viaja en el
     * comando ni en el request y no se sustituye por {@code SYS}, {@code SISTEMA} ni
     * {@code null}. {@code codigo_qr} se deriva del {@code id_tecnico} y nunca del
     * {@code id_acta}, que todavia no existe cuando se emite.
     *
     * <h2>Frontera transaccional</h2>
     *
     * <p><b>Implementa {@code DECISION_OBS-TX-01}.</b> Este metodo publico es la frontera
     * transaccional del alta: una transaccion por comando. Todas las precondiciones se verifican
     * <b>antes</b> de abrir cualquier mutacion, y las seis escrituras del caso de uso ocurren
     * dentro de una unica transaccion con <b>rollback total</b> ante cualquier fallo.
     *
     * <p><b>{@code isolation = READ_COMMITTED}</b> (Wave 4 PARTE-3A §12, hallazgo material): la
     * numeracion electronica resuelve el talonario compatible (lectura simple de
     * {@code num_talonario}/{@code num_talonario_ambito}) y, mas tarde en la misma transaccion,
     * bloquea esa misma fila con {@code SELECT ... FOR UPDATE} para incrementar el contador
     * (prompt §12.1: "resolver politica y talonario elegible sin bloquear todavia" seguido de
     * "bloquear la fila del talonario" al final). Bajo {@code REPEATABLE READ} (nivel por
     * defecto de InnoDB) y MariaDB 12.x con {@code innodb_snapshot_isolation=ON} (activo por
     * defecto desde MariaDB 11.8), esa misma fila leida dos veces con proposito distinto dentro
     * de la misma transaccion, si otra transaccion concurrente la modifico entre ambas lecturas,
     * dispara {@code ERROR 1020 Record has changed since last read} con rollback total de la
     * transaccion completa -- documentado por MariaDB como equivalente a un deadlock, a tratar
     * reintentando la transaccion. {@code READ_COMMITTED} da una vista fresca por statement y
     * elimina ese conflicto sin debilitar la exclusion mutua real, que la sigue dando el
     * {@code FOR UPDATE} de fila (bloqueo de motor, no de nivel de aislamiento). No es un ajuste
     * de configuracion global del servidor: es el isolation level de esta transaccion Spring.
     *
     * <p>Orden de escritura unico y determinista, respetado en todos los caminos equivalentes.
     * Lo imponen las claves ajenas reales: evidencia, observacion, evento y snapshot referencian
     * {@code fal_acta.id}, y el acta referencia {@code fal_persona.id}, de modo que ninguna otra
     * secuencia es posible sin violar integridad.
     *
     * <pre>
     * 1. resolver o crear fal_persona cuando la situacion lo exige
     * 2. generar id_tecnico y QR0 sin efectos persistentes externos
     * 3. insertar fal_acta y obtener la PK
     * 4. insertar la observacion inicial si el texto es no blanco
     * 5. insertar las evidencias
     * 6. insertar el evento ACTLAB
     * 7. insertar o actualizar el snapshot
     * 8. commit total
     * </pre>
     *
     * <p>Un mismo orden de acceso a tablas en todos los caminos es lo que evita el deadlock por
     * orden inverso de bloqueos: dos altas concurrentes toman los bloqueos en la misma
     * secuencia.
     *
     * <p>Cualquier {@code RuntimeException} de persistencia se propaga sin capturar y provoca
     * rollback. No hay {@code noRollbackFor}, no hay compensaciones manuales, no hay
     * {@code TransactionSynchronization}, no hay callbacks poscommit, no hay reintento
     * silencioso y no se captura ninguna excepcion para devolver exito.
     *
     * <p>Ningun participante del alta produce un efecto externo irreversible. La emision del QR
     * es una funcion determinista sobre el {@code id_tecnico}, calculada en memoria: no escribe,
     * no publica y no llama a nada externo, de modo que un rollback la deja sin rastro. No hay
     * publicacion a broker, HTTP, archivo ni storage dentro de la frontera.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ComandoResultado labrar(LabrarActaCommand cmd) {
        String actor = exigirActor();

        // Idempotencia real por id_tecnico (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S5): un reintento
        // con el mismo id_tecnico (tipico de sincronizacion de captura movil offline) devuelve
        // la misma acta, numero y QR ya persistidos, sin reintentar ninguna mutacion (no crea
        // una segunda persona, no reconsume numeracion, no reemite QR). Se verifica antes de
        // cualquier otra validacion o mutacion: es el primer paso de la secuencia del alta.
        if (cmd.idTecnico() != null && !cmd.idTecnico().isBlank()) {
            Optional<FalActa> actaExistente = actaRepository.buscarPorUuidTecnico(cmd.idTecnico().trim());
            if (actaExistente.isPresent()) {
                FalActa acta = actaExistente.get();
                return ComandoResultado.de(acta.getId(), acta.getId().toString(), TipoEventoActa.ACTLAB.codigo(),
                        "Acta ya labrada (idempotencia por id_tecnico). Bloque: " + acta.getBloqueActual().codigo());
            }
        }

        // Matriz de aplicabilidad de satelites por tipoActa (Wave 4 Parte 3B §6, prompt
        // "DECISION-ACTA-SATELITES-COMPLETOS-01" cerrada): debe validarse antes de adquirir
        // locks de numeracion, incrementar el contador o persistir el acta o cualquier
        // satelite (prompt §6.2). Un acta TRANSITO exige transito y prohibe
        // contravencion/sustancias; CONTRAVENCION/COMERCIO exigen contravencion y prohiben
        // transito/sustancias; SUSTANCIAS_ALIMENTICIAS exige sustancias y prohibe
        // transito/contravencion. Vehiculo y medidasPreventivas son siempre opcionales
        // (condicionales) en los cuatro tipos: no hay discriminador funcional confiable en el
        // contrato actual mas alla de la presencia/ausencia del bloque (prompt §6, ultimo
        // parrafo).
        validarAplicabilidadSatelitesPorTipoActa(cmd);

        if (cmd.resultadoFirmaInfractor() == null) {
            throw new PrecondicionVioladaException("resultadoFirmaInfractor es obligatorio.");
        }

        if (cmd.resultadoFirmaInfractor() == ResultadoFirmaInfractor.FIRMADA) {
            boolean tieneEvidenciaFirma = cmd.evidenciasActa() != null
                    && cmd.evidenciasActa().stream()
                    .anyMatch(e -> e.tipoEvid() == TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR);
            if (!tieneEvidenciaFirma) {
                throw new PrecondicionVioladaException(
                        "resultadoFirmaInfractor=FIRMADA requiere evidencia FIRMA_OLOGRAFA_INFRACTOR.");
            }
        }

        SituacionIdentificacionInfractor situacion = cmd.situacionIdentificacionInfractor();
        DocumentoPersona documentoAportado = exigirIdentificacionCoherente(cmd, situacion);

        LocalDateTime ahora = faltasClock.now();
        LocalDate hoy = ahora.toLocalDate();

        Long idPersonaInfractor = resolverPersonaInfractora(cmd, situacion, documentoAportado, actor, ahora);

        // Domicilios informados (DECISION-ACTA-DOMICILIOS-OPCIONALES-01, Wave 4): se persisten
        // antes del acta raiz porque referencian id_persona_infractor, ya resuelto arriba, y no
        // referencian id_acta. Sin persona resuelta no hay a que persona_id anclarlos: se
        // ignoran silenciosamente, nunca se inventa una persona para satisfacerlos.
        persistirDomiciliosInformados(cmd, idPersonaInfractor, actor, ahora);

        // id_tecnico es opcional (DECISION-ACTA-ID-TECNICO-QR-01): ausente, se genera un
        // UUID nuevo; presente, se valida y se usa exactamente, sin regenerarlo. El QR se
        // deriva del id_tecnico resuelto y se emite antes de la primera escritura del acta:
        // la PK todavia no existe y el token debe permanecer estable cuando se asigne
        // (DECISION_DDL-ACTA-QR-01). El puerto devuelve token y version juntos para que
        // qr_payload_version no pueda divergir del segmento firmado dentro del token.
        String uuidTecnico = resolverIdTecnico(cmd.idTecnico());
        QrTokenEmitido qr = qrActaService.emitirTokenPara(uuidTecnico);

        FalActa actaNueva = new FalActa(
                null,
                uuidTecnico,
                cmd.tipoActa(),
                cmd.idDependencia() != null ? cmd.idDependencia() : 1L,
                cmd.idInspector() != null ? cmd.idInspector() : 1L,
                cmd.fechaActa() != null ? cmd.fechaActa() : hoy,
                ahora,
                cmd.domicilioHecho(),
                cmd.observaciones(),
                cmd.latInfr(),
                cmd.lonInfr(),
                cmd.resultadoFirmaInfractor(),
                situacion,
                idPersonaInfractor,
                ahora,
                actor
        );
        // La version del payload se informa antes del token: una vez emitido el QR el
        // agregado congela ambos campos (DECISION-QR-INMUTABLE-01).
        actaNueva.setQrPayloadVersion(qr.version());
        actaNueva.setCodigoQr(qr.token());
        if (cmd.infractorDocumento() != null) actaNueva.setInfractorDocumento(cmd.infractorDocumento());
        if (cmd.infractorNombre() != null) actaNueva.setInfractorNombre(cmd.infractorNombre());

        // La PK la asigna la persistencia: evidencias, evento, snapshot y respuesta
        // deben usar la copia persistida, no la entidad sin identidad.
        //
        // Idempotencia CONCURRENTE por id_tecnico (Wave 4 PARTE-3A S12.5): el pre-check de
        // arriba no alcanza por si solo cuando dos transacciones concurrentes con el mismo
        // id_tecnico pasan el pre-check antes de que cualquiera confirme. La restriccion
        // UNIQUE real de MariaDB (uq_fal_acta_id_tecnico) es la autoridad final: la
        // transaccion perdedora choca aqui. Esa violacion NO se oculta continuando dentro de
        // una transaccion invalida: se marca rollback-only explicitamente (revierte todo lo
        // especulativo de ESTA transaccion -- persona/domicilios ya insertados, si los hubo
        // -- sin necesidad de REQUIRES_NEW ni NESTED, prohibidos por DECISION_OBS-TX-01) y se
        // devuelve la acta ganadora, leida con FOR UPDATE para ver la fila ya comprometida
        // por la otra transaccion incluso si el snapshot no bloqueante de esta transaccion
        // todavia no la veria.
        FalActa acta;
        try {
            acta = actaRepository.guardar(actaNueva);
        } catch (org.springframework.dao.DataIntegrityViolationException colisionUnique) {
            if (uuidTecnico == null || uuidTecnico.isBlank()) {
                throw colisionUnique;
            }
            org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus().setRollbackOnly();
            FalActa ganadora = actaRepository.buscarPorUuidTecnicoConLock(uuidTecnico)
                    .orElseThrow(() -> colisionUnique);
            return ComandoResultado.de(ganadora.getId(), ganadora.getId().toString(), TipoEventoActa.ACTLAB.codigo(),
                    "Acta ya labrada (idempotencia concurrente por id_tecnico, UNIQUE de MariaDB)."
                            + " Bloque: " + ganadora.getBloqueActual().codigo());
        }
        Long id = acta.getId();

        // Articulos infringidos informados (DECISION-ACTA-SATELITES-ATOMICIDAD-01, Wave 4):
        // requieren id_acta ya asignado, por eso se persisten aqui y no junto a los domicilios.
        // Desde Wave 4 Parte 3B §7, la medida preventiva ya no referencia el articulo
        // infringido (decision funcional final: medida libre del catalogo, sin FK obligatoria
        // a fal_acta_articulo_infringido), por lo que el mapa de retorno ya no se consume aqui;
        // se conserva el metodo por si un consumidor futuro de articulos por clave natural lo
        // necesita.
        persistirArticulosInfringidosInformados(cmd, id, actor, ahora);

        // Seis satelites completos del alta integral (Wave 4 PARTE-3A §11), en el orden
        // recomendado por el prompt: vehiculo -> transito (con alcoholemia) -> contravencion ->
        // sustancias alimenticias -> medida preventiva (ultima, porque depende de los articulos
        // infringidos ya persistidos arriba).
        persistirVehiculoInformado(cmd.vehiculo(), id, actor, ahora);
        persistirTransitoInformado(cmd.transito(), id, actor, ahora);
        persistirContravencionInformada(cmd.contravencion(), id, actor, ahora);
        persistirSustanciasAlimenticiasInformadas(cmd.sustanciasAlimenticias(), id, actor, ahora);
        persistirMedidasPreventivasInformadas(cmd, id, actor, ahora);

        // Valorizacion inicial (Wave 4 PARTE-2, DECISION-ACTA-VALORIZACION-REUTILIZABLE-01):
        // calcula automaticamente el total ordinario y de pago voluntario de los articulos recien
        // imputados. No confirma (estado PRELIMINAR): valorizar no equivale a crear obligacion de
        // pago. Ausencia de articulos: no hay nada que valorizar, nunca un fallo del alta.
        valorizacionService.calcularInicial(id, actor, ahora);

        registrarObservacionInicial(cmd.observaciones(), id, actor, ahora);

        if (cmd.evidenciasActa() != null) {
            for (EvidenciaActaItem item : cmd.evidenciasActa()) {
                // Wave 4 Parte 3D (DECISION_DDL-EVID-03): FOTO exige el pipeline dedicado
                // (fotografiaService.incorporarLoteAlta, mas abajo), con SHA-256, dimensiones,
                // orden y demas metadata obligatoria. El contrato generico evidenciasActa no
                // transporta ese binario ni esa metadata: seguirlo aceptando para FOTO
                // construiria una evidencia invalida (el dominio la rechaza) o, peor, una
                // evidencia sin ningun archivo fisico real detras.
                if (item.tipoEvid() == TipoEvidenciaActa.FOTO) {
                    throw new PrecondicionVioladaException(
                            "tipoEvid=FOTO no se acepta en evidenciasActa (contrato generico). Usar el"
                                    + " contrato de fotografias del alta (multipart) o el endpoint de carga"
                                    + " posterior (DECISION_DDL-EVID-03, Wave 4 Parte 3D).");
                }
                FalActaEvidencia evidencia = new FalActaEvidencia(
                        evidenciaRepository.nextId(),
                        acta.getId(),
                        item.tipoEvid(),
                        item.storageKey(),
                        ahora,
                        ahora,
                        actor,
                        null
                );
                evidenciaRepository.guardar(evidencia);
            }
        }

        if (cmd.fotografiasSeguro() != null && !cmd.fotografiasSeguro().isEmpty()) {
            fotografiaEvidenciaService.incorporarLoteAlta(
                    acta.getId(), cmd.fotografiasSeguro(), actor, ahora, acta.getBloqueActual());
        }

        // Validacion de dependencia e inspector (Wave 4 PARTE-3A S10): debe ocurrir antes de
        // resolver/bloquear el talonario. Una dependencia inexistente/inactiva/sin version
        // vigente, o un inspector inexistente/inactivo/no habilitado para esa dependencia,
        // aborta el alta ANTES de tocar el contador (rollback total: 0 actas, 0 satelites,
        // 0 valorizaciones, contador sin cambios).
        // idInspector se valida con el valor CRUDO del comando (cmd.idInspector()), no con
        // acta.getIdInspector(): la entidad completa el campo con un valor tecnico por defecto
        // (1L) cuando el comando no lo informa (decision preexistente, fuera de alcance de
        // PARTE-3A), pero "no informado" debe seguir significando "no validar inspector", no
        // "validar contra el valor tecnico por defecto".
        talonarioService.validarDependenciaEInspectorParaActa(
                acta.getIdDependencia(), cmd.idInspector(), cmd.tipoActa(), hoy);

        // Numeracion electronica (Wave 4 PARTE-2 S8.3, reglas finales PARTE-3C §7): seccion
        // critica breve, al final, antes de ACTLAB y el snapshot. Bloquea unicamente la fila
        // del talonario (FOR UPDATE), nunca la dependencia/inspector/catalogos ya resueltos
        // arriba. Un rollback posterior revierte el contador junto con el resto: el numero
        // queda libre y reutilizable.
        //
        // DECISION-ACTA-TALONARIO-REQUIRED-01 (Wave 4 Parte 3C, prompt §7): 0 talonarios
        // ACTA ELECTRONICO activos, 2+ activos ambiguos y talonario agotado SIEMPRE rechazan el
        // alta completa. Ninguno de los tres casos se captura aqui: los tres se propagan y
        // revierten toda la transaccion (cero acta, cero satelites, cero valorizacion, contador
        // sin cambios). El acotamiento previo que toleraba "0 activos" como alta sin numerar
        // (DECISION-NUM-FINAL-ALTA-RECHAZADA-01, Parte 3B) queda eliminado: no existe alta
        // canonica con nro_acta NULL por falta de configuracion electronica.
        TalonarioService.TalonarioResuelto resuelto = talonarioService.resolverTalonarioActaElectronico(
                acta.getIdDependencia(), cmd.tipoActa() == null ? null : cmd.tipoActa().codigo(), hoy);
        TalonarioService.NumeroActaReclamado numero = talonarioService.reclamarNumeroActaElectronico(resuelto);
        acta.setNroActa(numero.nroActa());
        acta.setIdTalonario(numero.idTalonario());
        acta.setNroTalonarioUsado(numero.nroTalonarioUsado());
        acta = actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.ACTLAB, null, null,
                actor, ahora, "Acta labrada - tipo: " + cmd.tipoActa().name());

        snapshotRecalculador.recalcularYGuardar(acta, ahora);

        return ComandoResultado.de(id, id.toString(), TipoEventoActa.ACTLAB.codigo(),
                "Acta labrada correctamente. Bloque: ENRI");
    }

    /**
     * Resuelve {@code id_tecnico} segun {@code DECISION-ACTA-ID-TECNICO-QR-01}.
     *
     * <p>Ausente o en blanco: se genera un UUID nuevo. Informado: se valida (no blanco,
     * hasta 36 caracteres, coherente con {@code CHAR(36)}) y se usa exactamente, sin
     * regenerarlo. No es una segunda clave de idempotencia.
     */
    private String resolverIdTecnico(String idTecnicoInformado) {
        if (idTecnicoInformado == null || idTecnicoInformado.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String candidato = idTecnicoInformado.trim();
        if (candidato.length() > 36) {
            throw new PrecondicionVioladaException(
                    "idTecnico no puede exceder 36 caracteres: '" + candidato + "'.");
        }
        return candidato;
    }

    /**
     * Enriquece el acta con observaciones adicionales dentro del bloque {@code ENRI}.
     *
     * <h2>Frontera transaccional</h2>
     *
     * <p>Implementa {@code POST-B2-TX-CONSUMERS-01}. La ausencia de actualizacion directa de
     * {@code fal_acta} no elimina la necesidad de atomicidad entre el evento {@code ACTENR} y el
     * snapshot: las dos escrituras ocurren dentro de una unica transaccion con rollback total.
     */
    @Transactional
    public ComandoResultado enriquecer(EnriquecerActaCommand cmd) {
        String actor = exigirActor();
        FalActa acta = actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede enriquecer.");
        }
        if (acta.getBloqueActual() != BloqueActual.ENRI) {
            throw new PrecondicionVioladaException(
                    "Enriquecer requiere bloque ENRI. Bloque actual: "
                            + acta.getBloqueActual().codigo());
        }

        LocalDateTime ahora = faltasClock.now();
        registrarEvento(acta.getId(), TipoEventoActa.ACTENR, null, null,
                actor, ahora, cmd.observaciones());

        snapshotRecalculador.recalcularYGuardar(acta, ahora);

        return ComandoResultado.de(acta.getId(), acta.getId().toString(),
                TipoEventoActa.ACTENR.codigo(),
                "Acta enriquecida. Bloque: ENRI");
    }

    public FalActa obtenerActa(Long id) {
        return actaRepository.buscarPorId(id)
                .orElseThrow(() -> new ActaNoEncontradaException(id));
    }

    public FalActaSnapshot obtenerSnapshot(Long idActa) {
        return snapshotRepository.buscarPorActa(idActa)
                .orElseThrow(() -> new ActaNoEncontradaException("snapshot de acta: " + idActa));
    }

    public List<FalActaEvidencia> listarEvidencias(Long idActa) {
        return evidenciaRepository.listarPorActa(idActa);
    }

    /**
     * Caso de uso de consulta paginada de actas ({@code DECISION-LIST-PAGE-SORT-01}).
     *
     * <p>Es la unica lectura de coleccion de actas que la aplicacion ofrece. No existe una
     * variante ilimitada: {@code ActaRepository.listarTodas} es una operacion interna del puerto
     * y este servicio no la expone.
     *
     * <p>Argumentos nulos significan "sin filtro" y "primera pagina con los valores por
     * defecto": {@code page = 0}, {@code size = 50} y el orden predeterminado de Acta. La
     * ausencia de parametros nunca significa consulta ilimitada.
     *
     * <p>No lleva {@code @Transactional}: es una lectura sin mutaciones y el adaptador la
     * resuelve con las consultas que el motor necesita. Envolverla en una transaccion no
     * agregaria ninguna garantia y extenderia la vida de una conexion sin motivo.
     *
     * @throws ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException
     *         si la pagina o el orden solicitados violan el contrato
     */
    public PaginaResultado<FalActa> listarActas(FiltroActa filtro,
                                                PaginaSolicitada<CampoOrdenActa> pagina) {
        return actaRepository.listarPagina(
                filtro == null ? FiltroActa.ninguno() : filtro,
                pagina == null ? PaginaSolicitada.primeraPagina() : pagina);
    }

    /**
     * Crea la persona minima del infractor cuando el alta no aporta idPersonaInfractor.
     *
     * <p>El labrado recibe el documento como texto libre, sin tipo documental. Ese texto
     * **no** se estructura como documento de `fal_persona`: inferir `DNI` porque parece
     * numerico seria inventar identificacion, y `DECISION_DDL-PERS-01` lo prohibe. El
     * texto aportado permanece en `FalActa.infractorDocumento`, que es su registro
     * literal, y la persona queda sin documento.
     *
     * <p>Sin ninguna forma real de identificacion no se crea persona: la ausencia de
     * sujeto identificado se expresa con `id_persona_infractor` NULL.
     *
     * @return la persona persistida, u {@code Optional.empty()} si no hay datos suficientes
     */
    /**
     * Precondiciones de la matriz situacion/persona, todas antes de la primera
     * mutacion ({@code DECISION_DDL-ACTA-INFR-01}).
     *
     * @return el documento aportado ya en forma canonica, o
     *         {@link DocumentoPersona#ausente()} si no se aporto ninguno
     */
    private DocumentoPersona exigirIdentificacionCoherente(
            LabrarActaCommand cmd, SituacionIdentificacionInfractor situacion) {
        if (situacion == null) {
            throw new PrecondicionVioladaException(
                    "situacionIdentificacionInfractor es obligatoria.");
        }

        DocumentoPersona documento = documentoCanonico(cmd);

        if (!situacion.exigePersonaInfractora()) {
            if (cmd.idPersonaInfractor() != null) {
                throw new PrecondicionVioladaException(
                        "situacionIdentificacionInfractor=" + situacion.name()
                                + " no admite idPersonaInfractor: no hay sujeto identificado.");
            }
            return documento;
        }

        boolean hayPersonaResoluble = cmd.idPersonaInfractor() != null
                || !documento.estaAusente()
                || (cmd.infractorNombre() != null && !cmd.infractorNombre().isBlank());
        if (!hayPersonaResoluble) {
            throw new PrecondicionVioladaException(
                    "situacionIdentificacionInfractor=" + situacion.name()
                            + " exige una persona infractora resoluble: id, documento canonico o nombre.");
        }

        // IDENTIFICADO_COMPLETO se define por documento informado en fal_persona. Sin
        // id de persona previa y sin par (tipo, numero), el unico camino disponible
        // seria crear una persona por nombre, que es exactamente IDENTIFICADO_PARCIAL.
        if (situacion == SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO
                && cmd.idPersonaInfractor() == null && documento.estaAusente()) {
            throw new PrecondicionVioladaException(
                    "IDENTIFICADO_COMPLETO exige documento canonico (tipo y numero) o una"
                            + " persona ya identificada; un texto documental sin tipo no lo constituye.");
        }

        if (personaRepository == null && cmd.idPersonaInfractor() == null) {
            throw new PrecondicionVioladaException(
                    "situacionIdentificacionInfractor=" + situacion.name()
                            + " exige persona y no hay repositorio de personas disponible.");
        }

        exigirPersonaInternaUtilizable(cmd, situacion);

        return documento;
    }

    /**
     * {@code idPersonaInfractor} es una via interna: no se expone por HTTP y el request no
     * lo transporta. Cuando un llamador interno lo aporta, el alta no puede confiar en el
     * sin verificarlo, porque {@code fk_acta_persona_infractor} es RESTRICT y porque
     * {@code IDENTIFICADO_COMPLETO} afirma un hecho juridico sobre la identidad del sujeto.
     *
     * <p>Se verifica antes de cualquier mutacion:
     *
     * <pre>
     * la persona referenciada existe;
     * para IDENTIFICADO_COMPLETO, esa persona tiene documento canonico.
     * </pre>
     *
     * <p>No se altera la persona existente para hacerla encajar en la situacion declarada y
     * no se aplica ninguna heuristica de fusion ({@code DECISION_DDL-PERS-01}).
     */
    private void exigirPersonaInternaUtilizable(
            LabrarActaCommand cmd, SituacionIdentificacionInfractor situacion) {
        Long idPersona = cmd.idPersonaInfractor();
        if (idPersona == null) {
            return;
        }
        if (personaRepository == null) {
            throw new PrecondicionVioladaException(
                    "No hay repositorio de personas disponible para verificar"
                            + " idPersonaInfractor=" + idPersona + ".");
        }
        FalPersona persona = personaRepository.buscarPorId(idPersona)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "idPersonaInfractor=" + idPersona + " no corresponde a ninguna persona."));

        if (situacion == SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO
                && persona.getDocumento().estaAusente()) {
            throw new PrecondicionVioladaException(
                    "IDENTIFICADO_COMPLETO no puede apoyarse en la persona " + idPersona
                            + ", que no tiene documento canonico. Corresponde IDENTIFICADO_PARCIAL"
                            + " o aportar el documento de esa persona por su propio caso de uso.");
        }
    }

    /**
     * Inserta la observacion inicial del acta cuando el texto es no blanco
     * ({@code DECISION_OBS-01}, {@code 40-domain/observaciones.md}).
     *
     * <p>La fuente canonica persistente de la observacion es {@code fal_observacion}, con el
     * vinculo propietario del acta:
     *
     * <pre>
     * entidadTipo      = ACTA
     * entidadId        = PK real del acta
     * idActaContexto   = null, porque el propietario es el acta y no un hijo suyo
     * origen           = USUARIO, nota ingresada por un operador humano
     * fhAlta           = el mismo instante del alta
     * idUserAlta       = el actor autenticado del alta
     * </pre>
     *
     * <p>{@code tipoObs} no se informa: es un campo deprecado que no se persiste.
     * {@code FalActa.observaciones} conserva el texto solo como compatibilidad del agregado
     * en memoria y no es una segunda fuente de verdad.
     *
     * <p>Texto nulo o en blanco: cero filas.
     */
    private void registrarObservacionInicial(String texto, Long idActa,
                                             String actor, LocalDateTime ahora) {
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }
        observacionRepository.guardar(new FalObservacion(
                null,
                EntidadTipoObservada.ACTA,
                idActa,
                null,
                texto,
                OrigenObservacion.USUARIO,
                null,
                ahora,
                actor));
    }

    /**
     * Persiste los domicilios informados en el alta, anclados a la persona infractora ya
     * resuelta ({@code DECISION-ACTA-DOMICILIOS-OPCIONALES-01}).
     *
     * <p>No hace nada si no hay domicilios informados o la situación de identificación no
     * resolvió una persona. Ninguno de esos dos casos es un error del alta.
     */
    private void persistirDomiciliosInformados(
            LabrarActaCommand cmd, Long idPersonaInfractor, String actor, LocalDateTime ahora) {
        List<DomicilioActaItem> items = cmd.domiciliosSeguro();
        if (items.isEmpty() || idPersonaInfractor == null) {
            return;
        }
        for (DomicilioActaItem item : items) {
            FalPersonaDomicilio domicilio = new FalPersonaDomicilio(
                    personaDomicilioRepository.nextId(),
                    idPersonaInfractor,
                    null,
                    item.tipoDomicilio(),
                    item.origenDomicilio(),
                    item.modoDomicilio(),
                    true,
                    item.siNotificable(),
                    item.siPrincipal(),
                    item.idProvincia(),
                    item.unidadTerritorialTipo(),
                    item.idUnidadTerritorial(),
                    item.idLocalidad(),
                    item.idCalle(),
                    item.idLocMalvinas(),
                    item.localidadMalvinasVersionId(),
                    item.idTcaMalvinas(),
                    item.calleMalvinasVersionId(),
                    item.calleTxt(),
                    item.altura(),
                    item.siSinAltura(),
                    item.unidadFuncional(),
                    item.codigoPostal(),
                    item.domicilioTxt(),
                    false,
                    item.lat(),
                    item.lon(),
                    item.origenUbicacion(),
                    ahora,
                    actor);
            personaDomicilioRepository.guardar(domicilio);
        }
    }

    /**
     * Persiste los artículos infringidos informados en el alta
     * ({@code DECISION-ACTA-SATELITES-ATOMICIDAD-01}). No hace nada si no hay artículos
     * informados.
     *
     * @return el mapa {@code articuloId (catálogo) -> id de la fila fal_acta_articulo_infringido}
     *         recién creada, usado por {@link #persistirMedidasPreventivasInformadas} para
     *         resolver la medida preventiva por clave natural (nunca por posición de lista).
     */
    private java.util.Map<Long, Long> persistirArticulosInfringidosInformados(
            LabrarActaCommand cmd, Long idActa, String actor, LocalDateTime ahora) {
        List<ArticuloInfringidoItem> items = cmd.articulosInfringidosSeguro();
        java.util.Map<Long, Long> actaArticuloIdPorArticulo = new java.util.HashMap<>();
        if (items.isEmpty()) {
            return actaArticuloIdPorArticulo;
        }
        for (ArticuloInfringidoItem item : items) {
            FalActaArticuloInfringido articulo = new FalActaArticuloInfringido(
                    articuloInfringidoRepository.nextId(),
                    idActa,
                    item.normativaId(),
                    item.articuloId(),
                    ahora,
                    actor);
            // El id real lo asigna la persistencia (JDBC: AUTO_INCREMENT real, distinto del
            // provisional de nextId()); el mapa debe usar el id devuelto por save(), no el
            // provisional, para que la medida preventiva referencie la fila real.
            FalActaArticuloInfringido guardado = articuloInfringidoRepository.save(articulo);
            actaArticuloIdPorArticulo.put(item.articuloId(), guardado.getId());
        }
        return actaArticuloIdPorArticulo;
    }

    /**
     * Matriz autoritativa de aplicabilidad de satélites por {@code tipoActa}
     * ({@code DECISION-ACTA-SATELITES-COMPLETOS-01}, Wave 4 Parte 3B §6):
     *
     * <pre>
     * TRANSITO                 -&gt; transito OBLIGATORIO; contravencion y sustancias PROHIBIDOS
     * CONTRAVENCION            -&gt; contravencion OBLIGATORIO; transito y sustancias PROHIBIDOS
     * SUSTANCIAS_ALIMENTICIAS  -&gt; sustancias OBLIGATORIO; transito y contravencion PROHIBIDOS
     * COMERCIO                 -&gt; contravencion OBLIGATORIO (comparte bloque); transito y
     *                             sustancias PROHIBIDOS
     * </pre>
     *
     * <p>{@code vehiculo} y {@code medidasPreventivas} son siempre condicionales/opcionales en
     * los cuatro tipos: el contrato actual no posee un discriminador funcional confiable más
     * allá de la presencia del bloque (prompt §6, último párrafo: "no inventar un indicador
     * artificial"), así que se permiten en cualquier tipo sin exigir ni prohibir.
     */
    private void validarAplicabilidadSatelitesPorTipoActa(LabrarActaCommand cmd) {
        ar.gob.malvinas.faltas.core.domain.enums.TipoActa tipo = cmd.tipoActa();
        boolean tieneTransito = cmd.transito() != null;
        boolean tieneContravencion = cmd.contravencion() != null;
        boolean tieneSustancias = cmd.sustanciasAlimenticias() != null;

        // DECISION-ACTA-SATELITES-STRICT-01 (Wave 4 Parte 3C, prompt §5): el bloque obligatorio
        // de cada tipoActa se exige SIEMPRE, sin opt-in. El acotamiento previo
        // (DECISION-APLICABILIDAD-SATELITES-COMPATIBILIDAD-01, Parte 3B) queda eliminado: no
        // existe ningun comando -- ni siquiera uno que no informe ningun otro satelite tipado --
        // que pueda omitir el bloque obligatorio de su tipo. Las prohibiciones se validan
        // siempre, igual que antes.
        switch (tipo) {
            case TRANSITO -> {
                exigirBloque(tieneTransito, tipo, "transito");
                prohibirBloque(tieneContravencion, tipo, "contravencion");
                prohibirBloque(tieneSustancias, tipo, "sustanciasAlimenticias");
            }
            case CONTRAVENCION -> {
                exigirBloque(tieneContravencion, tipo, "contravencion");
                prohibirBloque(tieneTransito, tipo, "transito");
                prohibirBloque(tieneSustancias, tipo, "sustanciasAlimenticias");
            }
            case SUSTANCIAS_ALIMENTICIAS -> {
                exigirBloque(tieneSustancias, tipo, "sustanciasAlimenticias");
                prohibirBloque(tieneTransito, tipo, "transito");
                prohibirBloque(tieneContravencion, tipo, "contravencion");
            }
            case COMERCIO -> {
                exigirBloque(tieneContravencion, tipo, "contravencion (contravencion/comercio comparten bloque)");
                prohibirBloque(tieneTransito, tipo, "transito");
                prohibirBloque(tieneSustancias, tipo, "sustanciasAlimenticias");
            }
        }

        // Alcoholemia (prompt §6.1): solo tiene sentido dentro del bloque transito, ya validado
        // arriba como el unico camino posible (prohibido en los demas tipos).
        if (tieneTransito) {
            validarAlcoholemiaCoherenteConControl(cmd.transito());
        }
    }

    private static void exigirBloque(boolean presente, ar.gob.malvinas.faltas.core.domain.enums.TipoActa tipo, String bloque) {
        if (!presente) {
            throw new PrecondicionVioladaException(
                    "tipoActa=" + tipo + " exige informar el bloque '" + bloque + "' (obligatorio segun la matriz"
                            + " de aplicabilidad de satelites, DECISION-ACTA-SATELITES-COMPLETOS-01).");
        }
    }

    private static void prohibirBloque(boolean presente, ar.gob.malvinas.faltas.core.domain.enums.TipoActa tipo, String bloque) {
        if (presente) {
            throw new PrecondicionVioladaException(
                    "tipoActa=" + tipo + " no admite el bloque '" + bloque + "' (prohibido segun la matriz de"
                            + " aplicabilidad de satelites, DECISION-ACTA-SATELITES-COMPLETOS-01).");
        }
    }

    /**
     * {@code siControlAlcoholemia=true} exige al menos una medicion; {@code false} exige que
     * la lista este vacia (prompt §6.1). Ambos lados se validan explicitamente: un control
     * declarado sin mediciones es tan incoherente como mediciones sin control declarado.
     */
    private static void validarAlcoholemiaCoherenteConControl(TransitoActaItem transito) {
        List<AlcoholemiaMedicionItem> mediciones = transito.medicionesSeguro();
        if (transito.siControlAlcoholemia()) {
            if (mediciones.isEmpty()) {
                throw new PrecondicionVioladaException(
                        "siControlAlcoholemia=true exige informar al menos una medicion de alcoholemia.");
            }
        } else if (!mediciones.isEmpty()) {
            throw new PrecondicionVioladaException(
                    "siControlAlcoholemia=false no admite mediciones de alcoholemia informadas.");
        }
    }

    /**
     * Persiste el satélite vehículo del acta ({@code fal_acta_vehiculo}, 1:1). No hace nada si
     * el bloque no fue informado (satélite no aplicable a este alta).
     */
    private void persistirVehiculoInformado(
            VehiculoActaItem item, Long idActa, String actor, LocalDateTime ahora) {
        if (item == null) return;
        FalActaVehiculo vehiculo = new FalActaVehiculo(idActa, ahora, actor);
        vehiculo.setDominioVehiculo(item.dominioVehiculo());
        vehiculo.setTipoVehiculo(item.tipoVehiculo());
        vehiculo.setMarca(item.marcaVehiculoId(), item.marcaVehiculoTxt());
        vehiculo.setModelo(item.modeloVehiculoId(), item.modeloVehiculoTxt());
        vehiculo.setAnioVehiculo(item.anioVehiculo());
        vehiculo.setColorVehiculo(item.colorVehiculo());
        vehiculo.setEstadoGeneralVehiculo(item.estadoGeneralVehiculo());
        actaVehiculoRepository.guardar(vehiculo);
    }

    /**
     * Persiste el satélite tránsito del acta ({@code fal_acta_transito}, 1:1) y, si
     * corresponde, sus mediciones de alcoholemia ({@code fal_acta_transito_alcoholemia}, N).
     * No hace nada si el bloque {@code transito} no fue informado.
     */
    private void persistirTransitoInformado(
            TransitoActaItem item, Long idActa, String actor, LocalDateTime ahora) {
        if (item == null) return;
        FalActaTransito transito = new FalActaTransito(idActa);
        transito.setNroLicencia(item.nroLicencia());
        transito.setLicenciaTerritorio(
                item.idProvLic(), item.unidadTerritorialLicTipo(), item.idUnidadTerritorialLic());
        transito.setSiRetLicencia(item.siRetLicencia());
        transito.setSiRetVehiculo(item.siRetVehiculo());
        transito.setSiControlAlcoholemia(item.siControlAlcoholemia());
        actaTransitoRepository.guardar(transito);

        for (AlcoholemiaMedicionItem medicion : item.medicionesSeguro()) {
            FalActaTransitoAlcoholemia entidad = new FalActaTransitoAlcoholemia(
                    actaTransitoAlcoholemiaRepository.nextId(),
                    idActa,
                    medicion.ordenMedicion(),
                    medicion.tipoPrueba(),
                    ahora,
                    actor);
            entidad.setResultadoCualitativo(medicion.resultadoCualitativo());
            entidad.setResultadoNumerico(medicion.resultadoNumerico(), medicion.unidadMedida());
            entidad.setAlcoholimetro(medicion.idAlcoholimetro(), medicion.verAlcoholimetro());
            entidad.setFhMedicion(medicion.fhMedicion());
            FalActaTransitoAlcoholemia guardada = actaTransitoAlcoholemiaRepository.guardar(entidad);
            if (medicion.siResultadoFinal()) {
                actaTransitoAlcoholemiaRepository.marcarResultadoFinalAtomicamente(idActa, guardada.getId());
            }
        }
    }

    /**
     * Persiste el satélite contravención/comercio del acta ({@code fal_acta_contravencion},
     * 1:1). No hace nada si el bloque no fue informado.
     */
    private void persistirContravencionInformada(
            ContravencionActaItem item, Long idActa, String actor, LocalDateTime ahora) {
        if (item == null) return;
        FalActaContravencion contravencion = new FalActaContravencion(
                idActa, item.origenNomencl(), item.siNomenclaturaManual(), ahora, actor);
        contravencion.setSujetoInmueble(item.idSujI(), item.idBieI());
        contravencion.setSujetoComercio(item.idSujC(), item.idBieC());
        contravencion.setCirc(item.circ());
        contravencion.setSecc(item.secc());
        contravencion.setFrac(item.frac());
        contravencion.setMza(item.mza());
        contravencion.setParc(item.parc());
        contravencion.setUfun(item.ufun());
        contravencion.setUcomp(item.ucomp());
        if (item.siNomenclaturaManual()) {
            contravencion.setMotivoNomenclaturaManual(item.motivoNomenclaturaManual());
        }
        contravencion.setRubro(item.rubroId(), item.idRub());
        contravencion.setAmbito(item.ambitoCtv(), item.ambitoCtvTxt());
        actaContravencionRepository.guardar(contravencion);
    }

    /**
     * Persiste el satélite sustancias alimenticias/bromatología del acta
     * ({@code fal_acta_sustancias_alimenticias}, 1:1). No hace nada si el bloque no fue
     * informado.
     */
    private void persistirSustanciasAlimenticiasInformadas(
            SustanciasAlimenticiasActaItem item, Long idActa, String actor, LocalDateTime ahora) {
        if (item == null) return;
        FalActaSustanciasAlimenticias sustancias =
                new FalActaSustanciasAlimenticias(idActa, ahora, actor);
        sustancias.setRubro(item.rubroId(), item.idRub());
        sustancias.setAmbito(item.ambitoCtv(), item.ambitoCtvTxt());
        sustancias.setDescripcionSustancias(item.descripcionSustancias());
        actaSustanciasAlimenticiasRepository.guardar(sustancias);
    }

    /**
     * Persiste las medidas preventivas efectivamente aplicadas
     * ({@code fal_acta_medida_preventiva}, N). Decision funcional final (Wave 4 Parte 3B §7,
     * reemplaza {@code DECISION-ACTA-MEDIDA-PREVENTIVA-01}): <b>el inspector puede elegir
     * cualquier medida preventiva vigente del catalogo</b>, sin exigir compatibilidad previa
     * con el tipo de acta, el articulo infringido, {@code fal_articulo_medida_preventiva} ni
     * {@code si_admite_medida_preventiva}. La medida ya NO referencia un articulo infringido
     * de la acta ({@code actaArticuloId} queda {@code null}: la FK a
     * {@code fal_acta_articulo_infringido} dejo de ser obligatoria).
     *
     * <p>Se valida unicamente: existencia y vigencia/actividad de la medida en el catalogo
     * {@code fal_medida_preventiva}, y ausencia de duplicados exactos
     * ({@code acta_id + medida_preventiva_id}) dentro de la misma acta (dominio; la restriccion
     * fisica {@code UNIQUE(acta_id, medida_preventiva_id)} es la autoridad final ante
     * concurrencia). El estado inicial es siempre {@code APLICADA}
     * ({@link FalActaMedidaPreventiva} no admite otro estado en su constructor).
     */
    private void persistirMedidasPreventivasInformadas(
            LabrarActaCommand cmd, Long idActa, String actor, LocalDateTime ahora) {
        List<MedidaPreventivaActaItem> items = cmd.medidasPreventivasSeguro();
        if (items.isEmpty()) return;
        java.util.Set<Long> medidasYaInformadas = new java.util.HashSet<>();
        for (MedidaPreventivaActaItem item : items) {
            if (!medidasYaInformadas.add(item.medidaPreventivaId())) {
                throw new PrecondicionVioladaException(
                        "Medida preventiva duplicada dentro de la misma acta: medidaPreventivaId="
                                + item.medidaPreventivaId() + " (duplicado exacto acta_id+medida_preventiva_id,"
                                + " prompt Wave 4 Parte 3B §7.2).");
            }
            ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva medida = medidaPreventivaRepository
                    .findById(item.medidaPreventivaId())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "La medida preventiva id=" + item.medidaPreventivaId()
                                    + " no existe en el catalogo fal_medida_preventiva."));
            if (!medida.isSiActiva()) {
                throw new PrecondicionVioladaException(
                        "La medida preventiva id=" + item.medidaPreventivaId()
                                + " (codigo=" + medida.getCodigo() + ") no esta activa/vigente.");
            }
            FalActaMedidaPreventiva medidaAplicada = new FalActaMedidaPreventiva(
                    actaMedidaPreventivaRepository.nextId(),
                    idActa,
                    null,
                    item.medidaPreventivaId(),
                    item.siGeneraBloqueante(),
                    ahora,
                    actor);
            if (item.medPrevTxt() != null) {
                medidaAplicada.setMedPrevTxt(item.medPrevTxt());
            }
            actaMedidaPreventivaRepository.guardar(medidaAplicada);
        }
    }

    /**
     * Canoniza el par {@code (tipoDocumentoInfractor, infractorDocumento)} con la unica
     * autoridad documental del dominio. Un texto sin tipo no es un documento: queda
     * ausente y permanece literal en el campo de compatibilidad del acta, porque
     * inferir el tipo esta prohibido por {@code DECISION_DDL-PERS-01}.
     */
    private static DocumentoPersona documentoCanonico(LabrarActaCommand cmd) {
        if (cmd.tipoDocumentoInfractor() == null) {
            return DocumentoPersona.ausente();
        }
        try {
            return DocumentoPersona.normalizar(cmd.tipoDocumentoInfractor(), cmd.infractorDocumento());
        } catch (IllegalArgumentException noCanonizable) {
            throw new PrecondicionVioladaException(
                    "El documento del infractor no es canonizable: " + noCanonizable.getMessage());
        }
    }

    /**
     * Devuelve la PK de la persona infractora, o {@code null} cuando la situacion no
     * admite sujeto identificado. En las cuatro situaciones sin persona no se busca, no
     * se crea y no se asigna: nunca una centinela.
     */
    private Long resolverPersonaInfractora(LabrarActaCommand cmd,
                                           SituacionIdentificacionInfractor situacion,
                                           DocumentoPersona documento,
                                           String actor, LocalDateTime ahora) {
        if (!situacion.exigePersonaInfractora()) {
            return null;
        }
        if (cmd.idPersonaInfractor() != null) {
            return cmd.idPersonaInfractor();
        }
        if (!documento.estaAusente()) {
            return reutilizarOCrearPorDocumento(cmd, documento, actor, ahora);
        }
        return crearPersonaMinimal(cmd.infractorNombre(), actor, ahora)
                .map(FalPersona::getId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "situacionIdentificacionInfractor=" + situacion.name()
                                + " exige persona y los datos aportados no constituyen una identificacion."));
    }

    /**
     * Reutiliza la persona que el documento ya identifica en lugar de duplicarla
     * ({@code 20-application/command-contracts.md}). La busqueda usa la misma forma
     * canonica que se persiste.
     */
    private Long reutilizarOCrearPorDocumento(LabrarActaCommand cmd, DocumentoPersona documento,
                                              String actor, LocalDateTime ahora) {
        List<FalPersona> existentes =
                personaRepository.buscarPorTipoDocYNroDoc(documento.tipoDoc(), documento.nroDoc());
        if (!existentes.isEmpty()) {
            return existentes.get(0).getId();
        }
        FalPersona persona = new FalPersona(null, TipoPersona.FISICA, ahora, actor);
        persona.setDocumento(documento);
        aplicarNombreMostrar(persona, cmd.infractorNombre());
        return personaRepository.guardar(persona).getId();
    }

    /**
     * Persona sin documento canonizable, con los datos realmente aportados. Es el
     * camino de {@code IDENTIFICADO_PARCIAL}: nunca produce identificacion completa.
     */
    private Optional<FalPersona> crearPersonaMinimal(String nombreMostrar,
                                                     String actor, LocalDateTime ahora) {
        FalPersona persona = new FalPersona(null, TipoPersona.FISICA, ahora, actor);
        aplicarNombreMostrar(persona, nombreMostrar);
        if (!persona.tieneIdentificacionMinima()) {
            return Optional.empty();
        }
        return Optional.of(personaRepository.guardar(persona));
    }

    /** {@code nombre_mostrar} es VARCHAR(64) y es texto de presentacion, no identidad. */
    private static void aplicarNombreMostrar(FalPersona persona, String nombreMostrar) {
        if (nombreMostrar == null || nombreMostrar.isBlank()) {
            return;
        }
        String nm = nombreMostrar.strip();
        persona.setNombreMostrar(nm.length() > 64 ? nm.substring(0, 64) : nm);
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idDocuRel, Long idNotifRel,
                                  String idUserEvt, LocalDateTime fhEvt,
                                  String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(idUserEvt != null ? OrigenEvento.USUARIO_WEB : OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(fhEvt)
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUserEvt)
                .actorTipo(idUserEvt != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }

    private String exigirActor() {
        ActorContext ctx = ActorContextHolder.get();
        if (ctx == null) {
            throw new PrecondicionVioladaException("Operacion humana requiere actor autenticado (JWT sub).");
        }
        return ctx.sub();
    }
}
