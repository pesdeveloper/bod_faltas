package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.AsignarTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.DevolverTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.EmitirNumeroActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EmitirNumeroDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.result.NumeroDocumentoEmitidoResponse;
import ar.gob.malvinas.faltas.core.application.result.NumeroActaEmitidoResponse;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNumeroTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioAmbito;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;
import ar.gob.malvinas.faltas.core.application.command.AnularNumeroTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.JustificarNumeroTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.DevolverNumeroSinUsarCommand;
import ar.gob.malvinas.faltas.core.application.command.CerrarAsignacionTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.web.dto.CierreAsignacionTalonarioResponse;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import ar.gob.malvinas.faltas.core.repository.TalonarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Motor de administracion de politicas de numeracion, talonarios, ambitos,
 * asignaciones de talonarios manuales a inspectores y emision de numeros.
 *
 * Reglas de emision (Slice 8B-4):
 * - Solo talonarios con claseTalonario = ACTA.
 * - Talonario debe estar activo y no bloqueado.
 * - Ambito debe estar activo y vigente en la fecha.
 * - Ambito debe ser compatible con idDep/verDep (GLOBAL o DEPENDENCIA).
 * - tipoActa filtra ambitos si esta informado.
 * - Menor prioridad = mayor prioridad.
 * - Empate real de prioridad entre talonarios distintos: falla por ambiguedad.
 * - El primer numero emitido es nroDesde.
 * - Si nroHasta se supera, falla con error controlado.
 * - Registra movimiento USADO por cada numero emitido.
 *
 * No emite numeros para DOCUMENTO (8B-5).
 * No implementa rendicion, huecos, anulacion (8B-6).
 *
 * Slice 8B-2/8B-3: implementacion in-memory. Slice 9: reemplazar repositorios por JDBC.
 */
@Service
public class TalonarioService {

    private final TalonarioRepository talonarioRepository;
    private final DependenciaRepository dependenciaRepository;
    private final InspectorRepository inspectorRepository;

    public TalonarioService(TalonarioRepository talonarioRepository,
                             DependenciaRepository dependenciaRepository,
                             InspectorRepository inspectorRepository) {
        this.talonarioRepository = talonarioRepository;
        this.dependenciaRepository = dependenciaRepository;
        this.inspectorRepository = inspectorRepository;
    }

    // -------------------------------------------------------------------------
    // Politicas
    // -------------------------------------------------------------------------

    public NumPolitica crearPolitica(CrearPoliticaNumeracionCommand cmd) {
        validarCampoObligatorio(cmd.codigo(), "codigo");
        validarCampoObligatorio(cmd.descripcion(), "descripcion");
        if (cmd.claseNumeracion() == null) {
            throw new PrecondicionVioladaException("claseNumeracion es obligatoria.");
        }
        validarCampoObligatorio(cmd.formatoVisible(), "formatoVisible");
        if (cmd.fhVigDesde() == null) {
            throw new PrecondicionVioladaException("fhVigDesde es obligatoria.");
        }
        if (talonarioRepository.existePoliticaCodigo(cmd.codigo())) {
            throw new PrecondicionVioladaException("Ya existe una politica con codigo: " + cmd.codigo());
        }
        if (cmd.siIncluyePrefijo() && esNuloOVacio(cmd.prefijo())) {
            throw new PrecondicionVioladaException("prefijo es obligatorio cuando siIncluyePrefijo = true.");
        }
        if (cmd.siIncluyeAnio() && cmd.formatoAnio() == null) {
            throw new PrecondicionVioladaException("formatoAnio es obligatorio cuando siIncluyeAnio = true.");
        }
        if (cmd.longitudNro() != null && cmd.longitudNro() <= 0) {
            throw new PrecondicionVioladaException("longitudNro debe ser mayor a cero.");
        }

        long id = nextPoliticaId();
        LocalDateTime fhAlta = LocalDateTime.now();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";

        NumPolitica politica = new NumPolitica(
                id,
                cmd.codigo(),
                cmd.descripcion(),
                cmd.claseNumeracion(),
                cmd.siReinicioAnual(),
                cmd.siIncluyePrefijo(),
                cmd.prefijo(),
                cmd.siIncluyeAnio(),
                cmd.formatoAnio(),
                cmd.siIncluyeSerie(),
                cmd.longitudNro(),
                cmd.formatoVisible(),
                cmd.siActiva(),
                cmd.fhVigDesde(),
                cmd.fhVigHasta(),
                fhAlta,
                idUserAlta);

        return talonarioRepository.guardarPolitica(politica);
    }

    public NumPolitica obtenerPolitica(Long id) {
        return talonarioRepository.buscarPoliticaPorId(id)
                .orElseThrow(() -> new PrecondicionVioladaException("Politica no encontrada: " + id));
    }

    public List<NumPolitica> listarPoliticasActivas() {
        return talonarioRepository.listarPoliticasActivas();
    }

    // -------------------------------------------------------------------------
    // Talonarios
    // -------------------------------------------------------------------------

    public NumTalonario crearTalonario(CrearTalonarioCommand cmd) {
        if (cmd.politicaId() == null) {
            throw new PrecondicionVioladaException("politicaId es obligatorio.");
        }
        NumPolitica politica = talonarioRepository.buscarPoliticaPorId(cmd.politicaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Politica no encontrada: " + cmd.politicaId()));

        validarCampoObligatorio(cmd.codigo(), "codigo");
        validarCampoObligatorio(cmd.descripcion(), "descripcion");
        if (cmd.tipoTalonario() == null) {
            throw new PrecondicionVioladaException("tipoTalonario es obligatorio.");
        }
        if (cmd.claseTalonario() == null) {
            throw new PrecondicionVioladaException("claseTalonario es obligatoria.");
        }
        if (cmd.claseTalonario() != politica.getClaseNumeracion()) {
            throw new PrecondicionVioladaException(
                    "claseTalonario (" + cmd.claseTalonario() + ") debe coincidir con la politica ("
                            + politica.getClaseNumeracion() + ").");
        }
        if (talonarioRepository.existeTalonarioCodigo(cmd.codigo())) {
            throw new PrecondicionVioladaException(
                    "Ya existe un talonario con codigo: " + cmd.codigo());
        }
        validarCampoObligatorio(cmd.nombreSecuencia(), "nombreSecuencia");
        if (talonarioRepository.existeNombreSecuencia(cmd.nombreSecuencia())) {
            throw new PrecondicionVioladaException(
                    "Ya existe un talonario con nombreSecuencia: " + cmd.nombreSecuencia());
        }
        if (politica.isSiReinicioAnual() && cmd.anio() == null) {
            throw new PrecondicionVioladaException(
                    "anio es obligatorio porque la politica tiene siReinicioAnual = true.");
        }
        if (politica.isSiIncluyeSerie() && esNuloOVacio(cmd.serie())) {
            throw new PrecondicionVioladaException(
                    "serie es obligatoria porque la politica tiene siIncluyeSerie = true.");
        }
        if (cmd.nroDesde() <= 0) {
            throw new PrecondicionVioladaException("nroDesde debe ser mayor a cero.");
        }
        if (cmd.nroHasta() != null && cmd.nroHasta() < cmd.nroDesde()) {
            throw new PrecondicionVioladaException(
                    "nroHasta debe ser mayor o igual a nroDesde.");
        }
        if (cmd.siBloqueado() && esNuloOVacio(cmd.codDesbloqueo())) {
            throw new PrecondicionVioladaException(
                    "codDesbloqueo es obligatorio cuando siBloqueado = true.");
        }

        long id = nextTalonarioId();
        LocalDateTime fhAlta = LocalDateTime.now();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";

        NumTalonario talonario = new NumTalonario(
                id,
                1,
                cmd.politicaId(),
                cmd.codigo(),
                cmd.descripcion(),
                cmd.tipoTalonario(),
                cmd.claseTalonario(),
                cmd.anio(),
                cmd.serie(),
                cmd.nroDesde(),
                cmd.nroHasta(),
                cmd.nombreSecuencia(),
                cmd.siActivo(),
                cmd.siBloqueado(),
                cmd.codDesbloqueo(),
                cmd.obsTalonario(),
                fhAlta,
                idUserAlta);

        return talonarioRepository.guardarTalonario(talonario);
    }

    public NumTalonario obtenerTalonario(Long id) {
        return talonarioRepository.buscarTalonarioPorId(id)
                .orElseThrow(() -> new PrecondicionVioladaException("Talonario no encontrado: " + id));
    }

    public List<NumTalonario> listarTalonariosActivos() {
        return talonarioRepository.listarTalonariosActivos();
    }

    // -------------------------------------------------------------------------
    // Ambitos
    // -------------------------------------------------------------------------

    public NumTalonarioAmbito crearAmbito(CrearTalonarioAmbitoCommand cmd) {
        if (cmd.talonarioId() == null) {
            throw new PrecondicionVioladaException("talonarioId es obligatorio.");
        }
        NumTalonario talonario = talonarioRepository.buscarTalonarioPorId(cmd.talonarioId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Talonario no encontrado: " + cmd.talonarioId()));

        if (cmd.claseTalonario() == null) {
            throw new PrecondicionVioladaException("claseTalonario es obligatoria.");
        }
        if (cmd.claseTalonario() != talonario.getClaseTalonario()) {
            throw new PrecondicionVioladaException(
                    "La claseTalonario del ambito debe coincidir con la del talonario.");
        }
        if (cmd.alcance() == null) {
            throw new PrecondicionVioladaException("alcance es obligatorio.");
        }
        if (cmd.fhDesde() == null) {
            throw new PrecondicionVioladaException("fhDesde es obligatoria.");
        }
        if (cmd.fhHasta() != null && !cmd.fhHasta().isAfter(cmd.fhDesde())) {
            throw new PrecondicionVioladaException("fhHasta debe ser posterior a fhDesde.");
        }

        if (cmd.prioridad() <= 0) {
            throw new PrecondicionVioladaException("La prioridad debe ser un valor positivo.");
        }

        if (cmd.alcance() == AlcanceTalonario.DEPENDENCIA) {
            if (cmd.idDep() == null) {
                throw new PrecondicionVioladaException(
                        "Alcance DEPENDENCIA requiere idDep y verDep.");
            }
        } else if (cmd.alcance() == AlcanceTalonario.GLOBAL && cmd.idDep() != null) {
            throw new PrecondicionVioladaException(
                    "Alcance GLOBAL no debe tener idDep. Use alcance DEPENDENCIA si quiere restringir por dependencia.");
        } else if (cmd.alcance() == AlcanceTalonario.TRANSVERSAL_DOCUMENTO) {
            if (cmd.tipoDocu() == null) {
                throw new PrecondicionVioladaException(
                        "Alcance TRANSVERSAL_DOCUMENTO requiere tipoDocu.");
            }
        }

        if (cmd.idDep() != null && cmd.verDep() == null) {
            throw new PrecondicionVioladaException("Si se informa idDep, verDep es obligatorio.");
        }
        if (cmd.verDep() != null && cmd.idDep() == null) {
            throw new PrecondicionVioladaException("Si se informa verDep, idDep es obligatorio.");
        }

        if (cmd.idDep() != null && cmd.verDep() != null) {
            dependenciaRepository.findById(cmd.idDep())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "Dependencia no encontrada: " + cmd.idDep()));
            LocalDate fechaRef = cmd.fhDesde();
            dependenciaRepository.findVersionVigente(cmd.idDep(), fechaRef)
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "No existe version vigente para la dependencia: " + cmd.idDep()
                                    + " en fecha: " + fechaRef));
        }

        long id = nextAmbitoId();
        LocalDateTime fhAlta = LocalDateTime.now();

        NumTalonarioAmbito ambito = new NumTalonarioAmbito(
                id,
                cmd.talonarioId(),
                cmd.claseTalonario(),
                cmd.tipoDocu(),
                cmd.tipoActa(),
                cmd.idDep(),
                cmd.verDep(),
                cmd.alcance(),
                cmd.prioridad(),
                cmd.fhDesde(),
                cmd.fhHasta(),
                cmd.siActivo(),
                fhAlta,
                cmd.idUserAlta());

        return talonarioRepository.guardarAmbito(ambito);
    }

    public NumTalonarioAmbito obtenerAmbito(Long id) {
        return talonarioRepository.buscarAmbitoPorId(id)
                .orElseThrow(() -> new PrecondicionVioladaException("Ambito no encontrado: " + id));
    }

    public List<NumTalonarioAmbito> listarAmbitosPorTalonario(Long talonarioId) {
        talonarioRepository.buscarTalonarioPorId(talonarioId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Talonario no encontrado: " + talonarioId));
        return talonarioRepository.listarAmbitosPorTalonario(talonarioId);
    }

    // -------------------------------------------------------------------------
    // Asignaciones inspector (Slice 8B-3)
    // -------------------------------------------------------------------------

    public NumTalonarioInspector asignarTalonarioInspector(AsignarTalonarioInspectorCommand cmd) {
        if (cmd.idTalonario() == null) {
            throw new PrecondicionVioladaException("idTalonario es obligatorio.");
        }
        NumTalonario talonario = talonarioRepository.buscarTalonarioPorId(cmd.idTalonario())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Talonario no encontrado: " + cmd.idTalonario()));

        if (!talonario.isSiActivo()) {
            throw new PrecondicionVioladaException("El talonario no esta activo.");
        }
        if (talonario.isSiBloqueado()) {
            throw new PrecondicionVioladaException("El talonario esta bloqueado.");
        }
        if (talonario.getTipoTalonario() != TipoTalonario.MANUAL_FISICO) {
            throw new PrecondicionVioladaException(
                    "Solo se pueden asignar talonarios MANUAL_FISICO a inspectores. " +
                    "Tipo actual: " + talonario.getTipoTalonario());
        }

        if (cmd.idInsp() == null) {
            throw new PrecondicionVioladaException("idInsp es obligatorio.");
        }
        inspectorRepository.findById(cmd.idInsp())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Inspector no encontrado: " + cmd.idInsp()));

        List<FalInspectorVersion> versiones = inspectorRepository.findVersionesByInsp(cmd.idInsp());
        versiones.stream()
                .filter(v -> v.getVerInsp() == cmd.verInsp())
                .findFirst()
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Version " + cmd.verInsp() + " del inspector " + cmd.idInsp() + " no encontrada."));

        if (cmd.fhEntrega() == null) {
            throw new PrecondicionVioladaException("fhEntrega es obligatoria.");
        }
        if (esNuloOVacio(cmd.idUserEntrega())) {
            throw new PrecondicionVioladaException("idUserEntrega es obligatorio.");
        }

        talonarioRepository.buscarAsignacionActivaPorTalonario(cmd.idTalonario())
                .ifPresent(a -> {
                    throw new PrecondicionVioladaException(
                            "El talonario ya tiene una asignacion activa (id=" + a.getId() + ").");
                });

        long id = nextAsignacionInspectorId();
        NumTalonarioInspector asignacion = new NumTalonarioInspector(
                id,
                cmd.idTalonario(),
                cmd.idInsp(),
                cmd.verInsp(),
                cmd.fhEntrega(),
                cmd.idUserEntrega(),
                null,
                null,
                EstadoAsignacionTalonario.ENTREGADO,
                true,
                cmd.idTalonario());

        return talonarioRepository.guardarAsignacionInspector(asignacion);
    }

    public NumTalonarioInspector devolverTalonarioInspector(DevolverTalonarioInspectorCommand cmd) {
        if (cmd.idAsignacion() == null) {
            throw new PrecondicionVioladaException("idAsignacion es obligatorio.");
        }
        NumTalonarioInspector asignacion = talonarioRepository
                .buscarAsignacionInspectorPorId(cmd.idAsignacion())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Asignacion no encontrada: " + cmd.idAsignacion()));

        if (!asignacion.isSiActiva()) {
            throw new PrecondicionVioladaException(
                    "La asignacion ya no esta activa (estado=" + asignacion.getEstadoAsignacion() + ").");
        }

        if (cmd.fhDevolucion() == null) {
            throw new PrecondicionVioladaException("fhDevolucion es obligatoria.");
        }
        if (esNuloOVacio(cmd.idUserDevolucion())) {
            throw new PrecondicionVioladaException("idUserDevolucion es obligatorio.");
        }

        asignacion.setFhDevolucion(cmd.fhDevolucion());
        asignacion.setIdUserDevolucion(cmd.idUserDevolucion());
        asignacion.setEstadoAsignacion(EstadoAsignacionTalonario.DEVUELTO);
        asignacion.setSiActiva(false);
        asignacion.setTalonarioIdActivo(null);

        return talonarioRepository.guardarAsignacionInspector(asignacion);
    }

    public NumTalonarioInspector obtenerAsignacionInspector(Long id) {
        return talonarioRepository.buscarAsignacionInspectorPorId(id)
                .orElseThrow(() -> new PrecondicionVioladaException("Asignacion no encontrada: " + id));
    }

    public List<NumTalonarioInspector> listarAsignacionesActivas() {
        return talonarioRepository.listarAsignacionesActivas();
    }

    public List<NumTalonarioInspector> listarAsignacionesPorTalonario(Long idTalonario) {
        talonarioRepository.buscarTalonarioPorId(idTalonario)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Talonario no encontrado: " + idTalonario));
        return talonarioRepository.listarAsignacionesPorTalonario(idTalonario);
    }

    public List<NumTalonarioInspector> listarAsignacionesPorInspector(Long idInsp) {
        return talonarioRepository.listarAsignacionesPorInspector(idInsp);
    }

    public List<NumTalonarioInspector> listarAsignacionesPorInspectorVersion(Long idInsp, short verInsp) {
        return talonarioRepository.listarAsignacionesPorInspector(idInsp).stream()
                .filter(a -> a.getVerInsp() == verInsp)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Emision de numeros de acta (Slice 8B-4)
    // -------------------------------------------------------------------------

    /**
     * Emite un numero de acta desde el talonario ACTA compatible con el contexto dado.
     *
     * Algoritmo de seleccion de talonario:
     * 1. Buscar talonarios ACTA, activos, no bloqueados.
     * 2. Para cada talonario, encontrar ambitos compatibles con idDep/tipoActa.
     * 3. Un ambito es compatible si: siActivo=true, vigente en fecha, alcance != TRANSVERSAL_DOCUMENTO,
     *    y si alcance=DEPENDENCIA entonces ambito.idDep == idDep.
     * 4. Si tipoActa != null, el ambito.tipoActa debe ser null o igual a tipoActa.
     * 5. La prioridad efectiva de un talonario es el minimo de prioridad de sus ambitos compatibles.
     * 6. Menor prioridad = mayor prioridad (menor numero gana).
     * 7. Si dos o mas talonarios distintos empatan en prioridad: falla por ambiguedad.
     * 8. El primer numero emitido es nroDesde. Si nroHasta se supera: falla.
     * 9. Registra movimiento USADO.
     */
    public NumeroActaEmitidoResponse emitirNumeroActa(EmitirNumeroActaCommand cmd) {
        if (cmd.idDep() == null) {
            throw new PrecondicionVioladaException("idDep es obligatorio.");
        }
        if (cmd.verDep() == null) {
            throw new PrecondicionVioladaException("verDep es obligatorio.");
        }
        if (cmd.fhMovimiento() == null) {
            throw new PrecondicionVioladaException("fhMovimiento es obligatorio.");
        }
        if (esNuloOVacio(cmd.idUserMovimiento())) {
            throw new PrecondicionVioladaException("idUserMovimiento es obligatorio.");
        }
        if (cmd.idInsp() != null && cmd.verInsp() == null) {
            throw new PrecondicionVioladaException("Si se informa idInsp, verInsp es obligatorio.");
        }
        if (cmd.verInsp() != null && cmd.idInsp() == null) {
            throw new PrecondicionVioladaException("Si se informa verInsp, idInsp es obligatorio.");
        }

        LocalDate fechaEmision = cmd.fhMovimiento().toLocalDate();

        List<NumTalonario> talonariosActa = talonarioRepository.listarTalonariosActivos().stream()
                .filter(t -> t.getClaseTalonario() == ClaseNumeracion.ACTA)
                .filter(t -> !t.isSiBloqueado())
                .toList();

        // Por cada talonario, calcular prioridad efectiva segun ambitos compatibles
        record TalonarioPrioridad(NumTalonario talonario, short prioridad) {}

        List<TalonarioPrioridad> candidatos = talonariosActa.stream()
                .map(t -> {
                    List<NumTalonarioAmbito> ambitosCompatibles = talonarioRepository
                            .listarAmbitosPorTalonario(t.getId()).stream()
                            .filter(a -> a.isSiActivo())
                            .filter(a -> a.esVigenteEn(fechaEmision))
                            .filter(a -> a.getAlcance() != AlcanceTalonario.TRANSVERSAL_DOCUMENTO)
                            .filter(a -> {
                                if (a.getAlcance() == AlcanceTalonario.DEPENDENCIA) {
                                    return cmd.idDep().equals(a.getIdDep());
                                }
                                return true; // GLOBAL
                            })
                            .filter(a -> {
                                if (cmd.tipoActa() != null && a.getTipoActa() != null) {
                                    return cmd.tipoActa().equals(a.getTipoActa());
                                }
                                return true;
                            })
                            .toList();

                    if (ambitosCompatibles.isEmpty()) return Optional.<TalonarioPrioridad>empty();

                    short mejorPrioridad = (short) ambitosCompatibles.stream()
                            .mapToInt(NumTalonarioAmbito::getPrioridad)
                            .min().getAsInt();

                    return Optional.of(new TalonarioPrioridad(t, mejorPrioridad));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (candidatos.isEmpty()) {
            throw new PrecondicionVioladaException(
                    "No hay talonario ACTA compatible con el contexto dado (idDep=" + cmd.idDep() + ").");
        }

        short mejorPrioridad = (short) candidatos.stream()
                .mapToInt(TalonarioPrioridad::prioridad)
                .min().orElseThrow();

        List<TalonarioPrioridad> conMejorPrioridad = candidatos.stream()
                .filter(c -> c.prioridad() == mejorPrioridad)
                .toList();

        if (conMejorPrioridad.size() > 1) {
            throw new PrecondicionVioladaException(
                    "Ambiguedad: " + conMejorPrioridad.size() + " talonarios tienen la misma prioridad "
                    + mejorPrioridad + ". No se puede determinar talonario sin ambiguedad.");
        }

        NumTalonario talonario = conMejorPrioridad.get(0).talonario();

        // Emitir siguiente numero
        int nroUsado = nextNumeroTalonario(talonario.getId(), talonario.getNroDesde(), talonario.getNroHasta());

        // Validar UNIQUE logico
        if (talonarioRepository.existeMovimientoTalonarioNumero(talonario.getId(), nroUsado)) {
            throw new PrecondicionVioladaException(
                    "Duplicado: ya existe un movimiento para talonario=" + talonario.getId()
                    + " nro=" + nroUsado + ".");
        }

        // Obtener politica para construir nroActa
        NumPolitica politica = talonarioRepository.buscarPoliticaPorId(talonario.getPoliticaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Politica no encontrada para talonario: " + talonario.getId()));

        String nroActa = construirNroActa(politica, talonario, nroUsado);

        // Registrar movimiento USADO
        long movimientoId = nextMovimientoId();
        NumTalonarioMovimiento movimiento = new NumTalonarioMovimiento(
                movimientoId,
                talonario.getId(),
                nroUsado,
                EstadoNumeroTalonario.USADO,
                null,
                null,
                cmd.actaId(),
                null,
                cmd.idDep(),
                cmd.verDep(),
                cmd.idInsp(),
                cmd.verInsp(),
                cmd.fhMovimiento(),
                cmd.idUserMovimiento()
        );
        talonarioRepository.guardarMovimiento(movimiento);

        return new NumeroActaEmitidoResponse(movimientoId, talonario.getId(), nroUsado, nroActa);
    }

    public List<NumTalonarioMovimiento> listarMovimientosPorTalonario(Long idTalonario) {
        talonarioRepository.buscarTalonarioPorId(idTalonario)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Talonario no encontrado: " + idTalonario));
        return talonarioRepository.listarMovimientosPorTalonario(idTalonario);
    }


    // -------------------------------------------------------------------------
    // Emision de numeros de documento (Slice 8C-5A)
    // -------------------------------------------------------------------------

    /**
     * Emite un numero de documento desde el talonario DOCUMENTO compatible con el contexto dado.
     *
     * Algoritmo analogo a emitirNumeroActa pero para clase DOCUMENTO:
     * 1. Buscar talonarios DOCUMENTO, activos, no bloqueados.
     * 2. Para cada talonario, encontrar ambitos compatibles:
     *    - siActivo=true, vigente en fecha.
     *    - tipoDocu del ambito debe coincidir con cmd.tipoDocu (obligatorio).
     *    - Si alcance=DEPENDENCIA: ambito.idDep == cmd.idDep.
     *    - Si alcance=GLOBAL: sin restriccion de dep.
     *    - Si alcance=TRANSVERSAL_DOCUMENTO: aplica si tipoDocu coincide.
     * 3. La prioridad efectiva es el minimo de prioridad de sus ambitos compatibles.
     * 4. Menor prioridad = mayor prioridad.
     * 5. Empate real entre talonarios distintos: falla por ambiguedad.
     * 6. Emite correlativo, construye nroDocu y registra movimiento con documentoId.
     */
    public NumeroDocumentoEmitidoResponse emitirNumeroDocumento(EmitirNumeroDocumentoCommand cmd) {
        if (cmd.idDep() == null) {
            throw new PrecondicionVioladaException("idDep es obligatorio para numeracion documental.");
        }
        if (cmd.verDep() == null) {
            throw new PrecondicionVioladaException("verDep es obligatorio para numeracion documental.");
        }
        if (cmd.tipoDocu() == null) {
            throw new PrecondicionVioladaException("tipoDocu es obligatorio para numeracion documental.");
        }
        if (cmd.documentoId() == null) {
            throw new PrecondicionVioladaException("documentoId es obligatorio para numeracion documental.");
        }
        if (cmd.fhMovimiento() == null) {
            throw new PrecondicionVioladaException("fhMovimiento es obligatorio para numeracion documental.");
        }
        if (esNuloOVacio(cmd.idUserMovimiento())) {
            throw new PrecondicionVioladaException("idUserMovimiento es obligatorio para numeracion documental.");
        }

        LocalDate fechaEmision = cmd.fhMovimiento().toLocalDate();

        List<NumTalonario> talonariosDoc = talonarioRepository.listarTalonariosActivos().stream()
                .filter(t -> t.getClaseTalonario() == ClaseNumeracion.DOCUMENTO)
                .filter(t -> !t.isSiBloqueado())
                .toList();

        record TalonarioPrioridad(NumTalonario talonario, short prioridad) {}

        List<TalonarioPrioridad> candidatos = talonariosDoc.stream()
                .map(t -> {
                    List<NumTalonarioAmbito> ambitosCompatibles = talonarioRepository
                            .listarAmbitosPorTalonario(t.getId()).stream()
                            .filter(a -> a.isSiActivo())
                            .filter(a -> a.esVigenteEn(fechaEmision))
                            .filter(a -> cmd.tipoDocu().equals(a.getTipoDocu()))
                            .filter(a -> {
                                if (a.getAlcance() == AlcanceTalonario.DEPENDENCIA) {
                                    return cmd.idDep().equals(a.getIdDep());
                                }
                                // GLOBAL y TRANSVERSAL_DOCUMENTO aplican sin restriccion de dep
                                return true;
                            })
                            .toList();

                    if (ambitosCompatibles.isEmpty()) return Optional.<TalonarioPrioridad>empty();

                    short mejorPrioridad = (short) ambitosCompatibles.stream()
                            .mapToInt(NumTalonarioAmbito::getPrioridad)
                            .min().getAsInt();

                    return Optional.of(new TalonarioPrioridad(t, mejorPrioridad));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (candidatos.isEmpty()) {
            throw new PrecondicionVioladaException(
                    "No hay talonario DOCUMENTO compatible con el contexto dado " +
                    "(tipoDocu=" + cmd.tipoDocu() + ", idDep=" + cmd.idDep() + ").");
        }

        short mejorPrioridad = (short) candidatos.stream()
                .mapToInt(TalonarioPrioridad::prioridad)
                .min().orElseThrow();

        List<TalonarioPrioridad> conMejorPrioridad = candidatos.stream()
                .filter(c -> c.prioridad() == mejorPrioridad)
                .toList();

        if (conMejorPrioridad.size() > 1) {
            throw new PrecondicionVioladaException(
                    "Ambiguedad: " + conMejorPrioridad.size() + " talonarios DOCUMENTO tienen la misma prioridad "
                    + mejorPrioridad + ". No se puede determinar talonario sin ambiguedad.");
        }

        NumTalonario talonario = conMejorPrioridad.get(0).talonario();

        int nroUsado = nextNumeroTalonario(talonario.getId(), talonario.getNroDesde(), talonario.getNroHasta());

        if (talonarioRepository.existeMovimientoTalonarioNumero(talonario.getId(), nroUsado)) {
            throw new PrecondicionVioladaException(
                    "Duplicado: ya existe un movimiento para talonario=" + talonario.getId()
                    + " nro=" + nroUsado + ".");
        }

        NumPolitica politica = talonarioRepository.buscarPoliticaPorId(talonario.getPoliticaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Politica no encontrada para talonario: " + talonario.getId()));

        String nroDocu = construirNroActa(politica, talonario, nroUsado);

        long movimientoId = nextMovimientoId();
        NumTalonarioMovimiento movimiento = new NumTalonarioMovimiento(
                movimientoId,
                talonario.getId(),
                nroUsado,
                EstadoNumeroTalonario.USADO,
                null,
                null,
                null,
                cmd.documentoId(),
                cmd.idDep(),
                cmd.verDep(),
                null,
                null,
                cmd.fhMovimiento(),
                cmd.idUserMovimiento()
        );
        talonarioRepository.guardarMovimiento(movimiento);

        return new NumeroDocumentoEmitidoResponse(movimientoId, talonario.getId(), nroUsado, nroDocu);
    }
    // -------------------------------------------------------------------------
    // Control de talonarios manuales fisicos (Slice 8B-6)
    // -------------------------------------------------------------------------

    public NumTalonarioMovimiento anularNumeroTalonario(AnularNumeroTalonarioCommand cmd) {
        if (cmd.idTalonario() == null) throw new PrecondicionVioladaException("idTalonario es obligatorio.");
        if (cmd.motivoAnulacion() == null) throw new PrecondicionVioladaException("motivoAnulacion es obligatorio.");
        if (cmd.fhMovimiento() == null) throw new PrecondicionVioladaException("fhMovimiento es obligatorio.");
        if (esNuloOVacio(cmd.idUserMovimiento())) throw new PrecondicionVioladaException("idUserMovimiento es obligatorio.");
        if (cmd.motivoAnulacion() == ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario.OTRO
                && esNuloOVacio(cmd.observacion())) {
            throw new PrecondicionVioladaException("observacion es obligatoria cuando motivoAnulacion = OTRO.");
        }

        NumTalonario talonario = talonarioRepository.buscarTalonarioPorId(cmd.idTalonario())
                .orElseThrow(() -> new PrecondicionVioladaException("Talonario no encontrado: " + cmd.idTalonario()));
        if (talonario.getTipoTalonario() != TipoTalonario.MANUAL_FISICO) {
            throw new PrecondicionVioladaException("Solo se pueden anular numeros de talonarios MANUAL_FISICO.");
        }
        if (!talonario.isSiActivo()) {
            throw new PrecondicionVioladaException("El talonario no esta activo: " + cmd.idTalonario());
        }
        validarNroEnRango(talonario, cmd.nroTalonario());
        if (talonarioRepository.existeMovimientoTalonarioNumero(cmd.idTalonario(), cmd.nroTalonario())) {
            throw new PrecondicionVioladaException(
                    "Ya existe un movimiento para talonario=" + cmd.idTalonario() + " nro=" + cmd.nroTalonario() + ".");
        }

        long movimientoId = nextMovimientoId();
        NumTalonarioMovimiento movimiento = new NumTalonarioMovimiento(
                movimientoId, cmd.idTalonario(), cmd.nroTalonario(),
                EstadoNumeroTalonario.ANULADO, cmd.motivoAnulacion(),
                cmd.observacion(), null, null,
                cmd.idDep(), cmd.verDep(), cmd.idInsp(), cmd.verInsp(),
                cmd.fhMovimiento(), cmd.idUserMovimiento());
        return talonarioRepository.guardarMovimiento(movimiento);
    }

    public NumTalonarioMovimiento justificarNumeroTalonario(JustificarNumeroTalonarioCommand cmd) {
        if (cmd.idTalonario() == null) throw new PrecondicionVioladaException("idTalonario es obligatorio.");
        if (esNuloOVacio(cmd.observacion())) throw new PrecondicionVioladaException("observacion es obligatoria para justificacion.");
        if (cmd.fhMovimiento() == null) throw new PrecondicionVioladaException("fhMovimiento es obligatorio.");
        if (esNuloOVacio(cmd.idUserMovimiento())) throw new PrecondicionVioladaException("idUserMovimiento es obligatorio.");

        NumTalonario talonario = talonarioRepository.buscarTalonarioPorId(cmd.idTalonario())
                .orElseThrow(() -> new PrecondicionVioladaException("Talonario no encontrado: " + cmd.idTalonario()));
        if (talonario.getTipoTalonario() != TipoTalonario.MANUAL_FISICO) {
            throw new PrecondicionVioladaException("Solo se pueden justificar numeros de talonarios MANUAL_FISICO.");
        }
        if (!talonario.isSiActivo()) {
            throw new PrecondicionVioladaException("El talonario no esta activo: " + cmd.idTalonario());
        }
        validarNroEnRango(talonario, cmd.nroTalonario());
        if (talonarioRepository.existeMovimientoTalonarioNumero(cmd.idTalonario(), cmd.nroTalonario())) {
            throw new PrecondicionVioladaException(
                    "Ya existe un movimiento para talonario=" + cmd.idTalonario() + " nro=" + cmd.nroTalonario() + ".");
        }

        long movimientoId = nextMovimientoId();
        NumTalonarioMovimiento movimiento = new NumTalonarioMovimiento(
                movimientoId, cmd.idTalonario(), cmd.nroTalonario(),
                EstadoNumeroTalonario.JUSTIFICADO, null,
                cmd.observacion(), null, null,
                cmd.idDep(), cmd.verDep(), cmd.idInsp(), cmd.verInsp(),
                cmd.fhMovimiento(), cmd.idUserMovimiento());
        return talonarioRepository.guardarMovimiento(movimiento);
    }

    public NumTalonarioMovimiento devolverNumeroSinUsar(DevolverNumeroSinUsarCommand cmd) {
        if (cmd.idTalonario() == null) throw new PrecondicionVioladaException("idTalonario es obligatorio.");
        if (cmd.fhMovimiento() == null) throw new PrecondicionVioladaException("fhMovimiento es obligatorio.");
        if (esNuloOVacio(cmd.idUserMovimiento())) throw new PrecondicionVioladaException("idUserMovimiento es obligatorio.");

        NumTalonario talonario = talonarioRepository.buscarTalonarioPorId(cmd.idTalonario())
                .orElseThrow(() -> new PrecondicionVioladaException("Talonario no encontrado: " + cmd.idTalonario()));
        if (talonario.getTipoTalonario() != TipoTalonario.MANUAL_FISICO) {
            throw new PrecondicionVioladaException("Solo se pueden devolver numeros de talonarios MANUAL_FISICO.");
        }
        validarNroEnRango(talonario, cmd.nroTalonario());
        if (talonarioRepository.existeMovimientoTalonarioNumero(cmd.idTalonario(), cmd.nroTalonario())) {
            throw new PrecondicionVioladaException(
                    "Ya existe un movimiento para talonario=" + cmd.idTalonario() + " nro=" + cmd.nroTalonario() + ".");
        }

        long movimientoId = nextMovimientoId();
        NumTalonarioMovimiento movimiento = new NumTalonarioMovimiento(
                movimientoId, cmd.idTalonario(), cmd.nroTalonario(),
                EstadoNumeroTalonario.DEVUELTO_SIN_USAR, null,
                cmd.observacion(), null, null,
                null, null, cmd.idInsp(), cmd.verInsp(),
                cmd.fhMovimiento(), cmd.idUserMovimiento());
        return talonarioRepository.guardarMovimiento(movimiento);
    }

    public CierreAsignacionTalonarioResponse cerrarAsignacionTalonarioInspector(CerrarAsignacionTalonarioInspectorCommand cmd) {
        if (cmd.idAsignacion() == null) throw new PrecondicionVioladaException("idAsignacion es obligatorio.");
        if (cmd.fhCierre() == null) throw new PrecondicionVioladaException("fhCierre es obligatorio.");
        if (esNuloOVacio(cmd.idUserCierre())) throw new PrecondicionVioladaException("idUserCierre es obligatorio.");

        NumTalonarioInspector asignacion = talonarioRepository.buscarAsignacionInspectorPorId(cmd.idAsignacion())
                .orElseThrow(() -> new PrecondicionVioladaException("Asignacion no encontrada: " + cmd.idAsignacion()));
        if (!asignacion.isSiActiva()) {
            throw new PrecondicionVioladaException("La asignacion ya no esta activa: " + cmd.idAsignacion());
        }

        NumTalonario talonario = talonarioRepository.buscarTalonarioPorId(asignacion.getIdTalonario())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Talonario no encontrado: " + asignacion.getIdTalonario()));
        if (talonario.getTipoTalonario() != TipoTalonario.MANUAL_FISICO) {
            throw new PrecondicionVioladaException("Solo se puede cerrar asignacion de talonario MANUAL_FISICO.");
        }
        if (talonario.getNroHasta() == null) {
            throw new PrecondicionVioladaException(
                    "No se puede hacer cierre completo con control de huecos: nroHasta es null para talonario " + talonario.getId() + ".");
        }

        List<Integer> faltantes = talonarioRepository.buscarNumerosFaltantesEnRango(
                talonario.getId(), talonario.getNroDesde(), talonario.getNroHasta());

        if (!faltantes.isEmpty()) {
            return new CierreAsignacionTalonarioResponse(
                    asignacion.getId(),
                    talonario.getId(),
                    asignacion.getEstadoAsignacion(),
                    asignacion.isSiActiva(),
                    faltantes,
                    false,
                    cmd.observacion());
        }

        asignacion.setEstadoAsignacion(EstadoAsignacionTalonario.CERRADO);
        asignacion.setSiActiva(false);
        asignacion.setTalonarioIdActivo(null);
        asignacion.setFhDevolucion(cmd.fhCierre());
        asignacion.setIdUserDevolucion(cmd.idUserCierre());
        talonarioRepository.actualizarAsignacionInspector(asignacion);

        return new CierreAsignacionTalonarioResponse(
                asignacion.getId(),
                talonario.getId(),
                EstadoAsignacionTalonario.CERRADO,
                false,
                List.of(),
                true,
                cmd.observacion());
    }

    public List<Integer> listarNumerosFaltantes(Long idTalonario) {
        NumTalonario talonario = talonarioRepository.buscarTalonarioPorId(idTalonario)
                .orElseThrow(() -> new PrecondicionVioladaException("Talonario no encontrado: " + idTalonario));
        if (talonario.getNroHasta() == null) {
            return List.of();
        }
        return talonarioRepository.buscarNumerosFaltantesEnRango(
                idTalonario, talonario.getNroDesde(), talonario.getNroHasta());
    }

    // -------------------------------------------------------------------------
    // Formato visible de nroActa segun politica
    // -------------------------------------------------------------------------

    /**
     * Construye el numero de acta visible (nroActa) segun la politica de numeracion.
     *
     * Formato in-memory:
     *   [PREFIJO-][ANIO-][SERIE-]NUMERO_CON_CEROS
     *
     * Cada seccion solo se incluye si la politica lo indica.
     * El numero se rellena con ceros a la izquierda segun longitudNro.
     */
    String construirNroActa(NumPolitica politica, NumTalonario talonario, int nroTalonario) {
        StringBuilder sb = new StringBuilder();

        if (politica.isSiIncluyePrefijo() && !esNuloOVacio(politica.getPrefijo())) {
            sb.append(politica.getPrefijo());
        }

        if (politica.isSiIncluyeAnio() && talonario.getAnio() != null) {
            String anioFull = String.valueOf(talonario.getAnio());
            String anioStr;
            Short formatoAnio = politica.getFormatoAnio();
            if (formatoAnio != null && formatoAnio == 2 && anioFull.length() >= 2) {
                anioStr = anioFull.substring(anioFull.length() - 2);
            } else {
                anioStr = anioFull;
            }
            if (sb.length() > 0) sb.append("-");
            sb.append(anioStr);
        }

        if (politica.isSiIncluyeSerie() && !esNuloOVacio(talonario.getSerie())) {
            if (sb.length() > 0) sb.append("-");
            sb.append(talonario.getSerie());
        }

        String numStr;
        if (politica.getLongitudNro() != null && politica.getLongitudNro() > 0) {
            numStr = String.format("%0" + politica.getLongitudNro() + "d", nroTalonario);
        } else {
            numStr = String.valueOf(nroTalonario);
        }
        if (sb.length() > 0) sb.append("-");
        sb.append(numStr);

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private long nextPoliticaId() {
        if (talonarioRepository instanceof InMemoryTalonarioRepository r) {
            return r.nextPoliticaId();
        }
        throw new IllegalStateException("nextPoliticaId no disponible en esta implementacion.");
    }

    private long nextTalonarioId() {
        if (talonarioRepository instanceof InMemoryTalonarioRepository r) {
            return r.nextTalonarioId();
        }
        throw new IllegalStateException("nextTalonarioId no disponible en esta implementacion.");
    }

    private long nextAmbitoId() {
        if (talonarioRepository instanceof InMemoryTalonarioRepository r) {
            return r.nextAmbitoId();
        }
        throw new IllegalStateException("nextAmbitoId no disponible en esta implementacion.");
    }

    private long nextAsignacionInspectorId() {
        if (talonarioRepository instanceof InMemoryTalonarioRepository r) {
            return r.nextAsignacionInspectorId();
        }
        throw new IllegalStateException("nextAsignacionInspectorId no disponible en esta implementacion.");
    }

    private long nextMovimientoId() {
        if (talonarioRepository instanceof InMemoryTalonarioRepository r) {
            return r.nextMovimientoId();
        }
        throw new IllegalStateException("nextMovimientoId no disponible en esta implementacion.");
    }

    private int nextNumeroTalonario(Long idTalonario, int nroDesde, Integer nroHasta) {
        if (talonarioRepository instanceof InMemoryTalonarioRepository r) {
            return r.nextNumeroTalonario(idTalonario, nroDesde, nroHasta);
        }
        throw new IllegalStateException("nextNumeroTalonario no disponible en esta implementacion.");
    }

    private void validarNroEnRango(NumTalonario talonario, int nroTalonario) {
        if (nroTalonario < talonario.getNroDesde()) {
            throw new PrecondicionVioladaException(
                    "nroTalonario " + nroTalonario + " es menor que nroDesde " + talonario.getNroDesde() + ".");
        }
        if (talonario.getNroHasta() != null && nroTalonario > talonario.getNroHasta()) {
            throw new PrecondicionVioladaException(
                    "nroTalonario " + nroTalonario + " supera nroHasta " + talonario.getNroHasta() + ".");
        }
    }

    private void validarCampoObligatorio(String valor, String nombre) {
        if (esNuloOVacio(valor)) {
            throw new PrecondicionVioladaException(nombre + " es obligatorio.");
        }
    }

    private boolean esNuloOVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}



