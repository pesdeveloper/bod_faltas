package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaAsignacion;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaClasificador;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaCodigo;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaContexto;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Soporte funcional del área consulta/listado de bandejas del prototipo.
 * Extraído de {@link PrototipoStore} para bajar su tamaño y acoplamiento sin
 * cambiar comportamiento observable: mismos endpoints, mismo orden de bandejas,
 * mismos conteos, mismos filtros, misma clasificación de sub-bandejas.
 *
 * <p>Concentra:
 * <ul>
 *   <li>listado de bandejas con conteo y sub-bandejas dinámicas
 *       ({@code listarBandejasConConteoOrdenadas},
 *       {@code listarBandejasConResumenOperativo}),</li>
 *   <li>construcción del contexto de sub-bandeja para clasificación
 *       ({@code construirContextoSubBandeja}),</li>
 *   <li>clasificación de sub-bandeja operativa ({@code clasificarSubBandeja}),</li>
 *   <li>predicado de sub-bandeja válida ({@code esSubBandejaValidaParaBandeja}),</li>
 *   <li>listado de actas por bandeja con todos sus filtros opcionales
 *       ({@code listarActasPorBandeja}).</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo y los supports de área que exponen la información necesaria
 * ({@code cerrabilidad}, {@code archivoReingreso}, {@code gestionExterna},
 * {@code piezasFirma}).
 */
final class BandejaConsultaSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, String> accionPendientePorActa;
    private final Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa;
    private final Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa;
    private final Map<String, ActaPiezasRequeridasMock> piezasRequeridasPorActa;
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa;
    private final CerrabilidadSupport cerrabilidad;
    private final ArchivoReingresoSupport archivoReingreso;
    private final GestionExternaSupport gestionExterna;
    private final PiezasFirmaSupport piezasFirma;
    private final SubBandejaClasificador subBandejaClasificador;
    private final List<String> ordenBandejas;

    BandejaConsultaSupport(
            Map<String, ActaMock> actas,
            Map<String, String> accionPendientePorActa,
            Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa,
            Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa,
            Map<String, ActaPiezasRequeridasMock> piezasRequeridasPorActa,
            Map<String, List<ActaNotificacionMock>> notificacionesPorActa,
            CerrabilidadSupport cerrabilidad,
            ArchivoReingresoSupport archivoReingreso,
            GestionExternaSupport gestionExterna,
            PiezasFirmaSupport piezasFirma,
            SubBandejaClasificador subBandejaClasificador,
            List<String> ordenBandejas) {
        this.actas = actas;
        this.accionPendientePorActa = accionPendientePorActa;
        this.situacionPagoPorActa = situacionPagoPorActa;
        this.situacionPagoCondenaPorActa = situacionPagoCondenaPorActa;
        this.piezasRequeridasPorActa = piezasRequeridasPorActa;
        this.notificacionesPorActa = notificacionesPorActa;
        this.cerrabilidad = cerrabilidad;
        this.archivoReingreso = archivoReingreso;
        this.gestionExterna = gestionExterna;
        this.piezasFirma = piezasFirma;
        this.subBandejaClasificador = subBandejaClasificador;
        this.ordenBandejas = ordenBandejas;
    }

    List<PrototipoStore.BandejaConteo> listarBandejasConConteoOrdenadas() {
        return listarBandejasConResumenOperativo().stream()
                .map(r -> new PrototipoStore.BandejaConteo(r.codigo(), r.cantidadActas()))
                .toList();
    }

    /**
     * Resumen de bandejas con conteo total y sub-bandejas dinámicas
     * (solo cantidad &gt; 0).
     */
    List<PrototipoStore.BandejaResumenOperativo> listarBandejasConResumenOperativo() {
        Map<String, Integer> conteoBandejas = new HashMap<>();
        Map<String, Map<String, Integer>> conteoSubBandejas = new HashMap<>();
        for (ActaMock acta : new ArrayList<>(actas.values())) {
            String bandeja = acta.bandejaActual();
            conteoBandejas.put(bandeja, conteoBandejas.getOrDefault(bandeja, 0) + 1);
            SubBandejaAsignacion asignacion = clasificarSubBandeja(acta.id());
            conteoSubBandejas
                    .computeIfAbsent(bandeja, k -> new HashMap<>())
                    .merge(asignacion.subBandeja(), 1, Integer::sum);
        }
        if (conteoBandejas.isEmpty()) {
            return List.of();
        }
        List<PrototipoStore.BandejaResumenOperativo> resultado = new ArrayList<>();
        for (String codigo : ordenBandejas) {
            Integer n = conteoBandejas.remove(codigo);
            if (n != null) {
                resultado.add(new PrototipoStore.BandejaResumenOperativo(
                        codigo, n, construirSubBandejasVisibles(codigo, conteoSubBandejas)));
            }
        }
        conteoBandejas.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> resultado.add(new PrototipoStore.BandejaResumenOperativo(
                        e.getKey(),
                        e.getValue(),
                        construirSubBandejasVisibles(e.getKey(), conteoSubBandejas))));
        return resultado;
    }

    SubBandejaContexto construirContextoSubBandeja(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null) {
            return null;
        }
        PrototipoStore.CerrabilidadActaVista cerrabilidadVista = cerrabilidad.getVistaCerrabilidad(acta);
        PrototipoStore.ResultadoFinalCierreMock resultadoFinal = cerrabilidadVista != null
                ? cerrabilidadVista.resultadoFinal()
                : PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL;
        boolean cerrable = cerrabilidadVista != null && cerrabilidadVista.cerrable();
        List<String> pendientes = cerrabilidadVista != null && cerrabilidadVista.pendientesBloqueantes() != null
                ? cerrabilidadVista.pendientesBloqueantes().stream().map(Enum::name).toList()
                : List.of();

        PrototipoStore.SituacionPagoMock situacionPago = situacionPagoPorActa.get(actaId);
        if (situacionPago == null) {
            situacionPago = PrototipoStore.SituacionPagoMock.SIN_PAGO;
        }

        PrototipoStore.SituacionPagoCondena situacionPagoCondena;
        if (cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            situacionPagoCondena = PrototipoStore.SituacionPagoCondena.NO_APLICA;
        } else {
            situacionPagoCondena = situacionPagoCondenaPorActa.getOrDefault(
                    actaId, PrototipoStore.SituacionPagoCondena.PENDIENTE);
        }

        List<ActaNotificacionMock> notificaciones = notificacionesPorActa.get(actaId);
        List<ActaNotificacionMock> notificacionesVista = (notificaciones == null || notificaciones.isEmpty())
                ? List.of()
                : List.copyOf(notificaciones);

        ActaPiezasRequeridasMock piezas = piezasRequeridasPorActa.get(actaId);
        return new SubBandejaContexto(
                acta,
                accionPendientePorActa.get(actaId),
                situacionPago,
                situacionPagoCondena,
                archivoReingreso.getMotivoArchivo(actaId),
                gestionExterna.getTipoGestionExterna(actaId),
                resultadoFinal,
                cerrable,
                pendientes,
                piezasFirma.listarDocumentosPorActa(actaId),
                notificacionesVista,
                piezas != null ? piezas.piezasRequeridas() : List.of(),
                piezas != null ? piezas.piezasGeneradas() : List.of());
    }

    SubBandejaAsignacion clasificarSubBandeja(String actaId) {
        SubBandejaContexto ctx = construirContextoSubBandeja(actaId);
        if (ctx == null) {
            return SubBandejaAsignacion.de(SubBandejaCodigo.ANALISIS_REVISION_GENERAL);
        }
        return subBandejaClasificador.clasificar(ctx);
    }

    boolean esSubBandejaValidaParaBandeja(String codigoBandeja, String subBandeja) {
        return SubBandejaCodigo.perteneceABandeja(codigoBandeja, subBandeja);
    }

    List<ActaMock> listarActasPorBandeja(String codigoBandeja) {
        return listarActasPorBandeja(codigoBandeja, null);
    }

    /**
     * Listado de la bandeja con filtro opcional por acción pendiente. Si
     * {@code accionPendiente} es {@code null} o en blanco, no se filtra.
     */
    List<ActaMock> listarActasPorBandeja(String codigoBandeja, String accionPendiente) {
        return listarActasPorBandeja(codigoBandeja, accionPendiente, null);
    }

    /**
     * Listado de la bandeja con filtros opcionales por acción pendiente y por
     * situación de pago mock. Si un filtro es {@code null} o en blanco, no se
     * aplica.
     */
    List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            PrototipoStore.SituacionPagoMock situacionPago) {
        return listarActasPorBandeja(
                codigoBandeja, accionPendiente, situacionPago, null, null, null);
    }

    /**
     * Filtros opcionales de cerrabilidad (además de bandeja, acción y pago). Si
     * un filtro es {@code null}, no aplica.
     */
    List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            PrototipoStore.SituacionPagoMock situacionPago,
            PrototipoStore.ResultadoFinalCierreMock filtroResultadoFinal,
            Boolean filtroCerrable,
            PrototipoStore.PendienteBloqueanteCierreMock filtroPendienteBloqueante) {
        return listarActasPorBandeja(
                codigoBandeja,
                accionPendiente,
                situacionPago,
                filtroResultadoFinal,
                filtroCerrable,
                filtroPendienteBloqueante,
                null);
    }

    /**
     * Listado de bandeja con filtros existentes y filtro opcional por
     * sub-bandeja operativa. Si {@code subBandeja} no es válida para la
     * bandeja, devuelve lista vacía.
     */
    List<ActaMock> listarActasPorBandeja(
            String codigoBandeja,
            String accionPendiente,
            PrototipoStore.SituacionPagoMock situacionPago,
            PrototipoStore.ResultadoFinalCierreMock filtroResultadoFinal,
            Boolean filtroCerrable,
            PrototipoStore.PendienteBloqueanteCierreMock filtroPendienteBloqueante,
            String subBandeja) {
        if (codigoBandeja == null) {
            return List.of();
        }
        String subBandejaFiltro = (subBandeja != null && !subBandeja.isBlank()) ? subBandeja.trim() : null;
        if (subBandejaFiltro != null && !esSubBandejaValidaParaBandeja(codigoBandeja, subBandejaFiltro)) {
            return List.of();
        }
        String accionFiltro = (accionPendiente != null && !accionPendiente.isBlank()) ? accionPendiente : null;
        return new ArrayList<>(actas.values()).stream()
                .filter(a -> codigoBandeja.equals(a.bandejaActual()))
                .filter(a -> accionFiltro == null || accionFiltro.equals(accionPendientePorActa.get(a.id())))
                .filter(a -> situacionPago == null || situacionPago.equals(resolverSituacionPago(a.id())))
                .filter(a -> filtroResultadoFinal == null
                        || cerrabilidad.getResultadoFinal(a.id()) == filtroResultadoFinal)
                .filter(a -> filtroCerrable == null
                        || cerrabilidad.coincideFiltroCerrable(a, filtroCerrable))
                .filter(a -> filtroPendienteBloqueante == null
                        || cerrabilidad.coincideFiltroPendiente(a, filtroPendienteBloqueante))
                .filter(a -> subBandejaFiltro == null
                        || subBandejaFiltro.equals(clasificarSubBandeja(a.id()).subBandeja()))
                .sorted(Comparator.comparing(ActaMock::id))
                .toList();
    }

    private PrototipoStore.SituacionPagoMock resolverSituacionPago(String actaId) {
        PrototipoStore.SituacionPagoMock v = situacionPagoPorActa.get(actaId);
        return v != null ? v : PrototipoStore.SituacionPagoMock.SIN_PAGO;
    }

    private List<PrototipoStore.SubBandejaConteo> construirSubBandejasVisibles(
            String bandeja, Map<String, Map<String, Integer>> conteoSubBandejas) {
        Map<String, Integer> conteos = conteoSubBandejas.getOrDefault(bandeja, Map.of());
        return SubBandejaCodigo.deBandeja(bandeja).stream()
                .map(SubBandejaCodigo::codigo)
                .filter(c -> conteos.getOrDefault(c, 0) > 0)
                .map(c -> new PrototipoStore.SubBandejaConteo(c, conteos.get(c)))
                .toList();
    }
}
