package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo local del circuito de correo postal: lote CSV de salida y respuesta CSV
 * prearmada. No integra servicios externos, no agenda procesos y no persiste
 * fuera del estado in-memory + archivos locales de la demo.
 */
final class CorreoPostalNotificacionSupport {

    private static final String BANDEJA_PENDIENTE_ANALISIS = "PENDIENTE_ANALISIS";
    private static final String BANDEJA_PENDIENTE_NOTIFICACION = "PENDIENTE_NOTIFICACION";
    private static final String BANDEJA_EN_NOTIFICACION = "EN_NOTIFICACION";
    private static final String BLOQUE_D4 = "D4_NOTIFICACION";
    private static final String BLOQUE_D5 = "D5_ANALISIS";
    private static final String ESTADO_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter CSV_TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final java.util.function.Function<String, PrototipoStore.RegistrarNotificacionPositivaResultado>
            registrarPositiva;
    private final AtomicInteger secuenciaLote = new AtomicInteger(0);
    private final List<CorreoLoteRegistro> lotesGenerados = new ArrayList<>();
    private final Path baseDir;

    enum EstadoLoteCorreo {
        PENDIENTE_RESPUESTA,
        PROCESADO,
        ANULADO
    }

    private record CorreoLoteRegistro(
            String loteId,
            int cantidad,
            String nombreArchivo,
            String rutaArchivo,
            EstadoLoteCorreo estado,
            LocalDateTime fechaGeneracion,
            List<PrototipoStore.NotificacionCorreoLoteItem> notificaciones) {
    }

    CorreoPostalNotificacionSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, List<ActaNotificacionMock>> notificacionesPorActa,
            Map<String, String> accionPendientePorActa,
            java.util.function.Function<String, PrototipoStore.RegistrarNotificacionPositivaResultado>
                    registrarPositiva) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.notificacionesPorActa = notificacionesPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.registrarPositiva = registrarPositiva;
        this.baseDir = Path.of(System.getProperty("user.dir"), "var", "demo", "notificaciones", "correo");
    }

    PrototipoStore.GenerarLoteCorreoResultado generarLote(String tipoFiltro, List<String> notificacionIds)
            throws IOException {
        List<ActaNotificacionMock> listas;
        if (notificacionIds != null && !notificacionIds.isEmpty()) {
            listas = resolverNotificacionesSeleccionadas(notificacionIds, tipoFiltro);
        } else {
            listas = notificacionesListasBase();
            if (tipoFiltro != null && !tipoFiltro.isBlank()) {
                TipoNotificacion tipo = TipoNotificacion.valueOf(tipoFiltro.trim().toUpperCase(Locale.ROOT));
                listas = listas.stream().filter(n -> n.tipo() == tipo).toList();
            }
        }
        if (listas.isEmpty()) {
            throw new IllegalArgumentException("No hay notificaciones validas para generar el lote.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        String loteId = "LOTE-CORREO-" + FILE_TS.format(ahora) + "-" + String.format("%03d", secuenciaLote.incrementAndGet());
        String nombreArchivo = loteId.toLowerCase(Locale.ROOT) + ".csv";
        Path salida = salidaDir().resolve(nombreArchivo);
        Files.createDirectories(salida.getParent());

        List<PrototipoStore.NotificacionCorreoLoteItem> incluidas = new ArrayList<>();
        List<String> lineas = new ArrayList<>();
        lineas.add("loteId,notificacionId,actaId,acta,tipo,canal,destinatario,domicilio,referenciaExterna,fechaGeneracion");
        for (ActaNotificacionMock n : listas) {
            ActaMock acta = actas.get(n.actaId());
            String referenciaExterna = n.referenciaExterna();
            if (referenciaExterna == null || referenciaExterna.isBlank()) {
                referenciaExterna = "CORREO-" + loteId + "-" + n.id();
            }
            ActaNotificacionMock enviada = n.conLoteCorreo(loteId, referenciaExterna, ahora);
            reemplazarNotificacion(enviada);
            if (acta != null && BANDEJA_PENDIENTE_NOTIFICACION.equals(acta.bandejaActual())) {
                actas.put(acta.id(), moverAEnNotificacion(acta));
            }
            registrarEvento(
                    n.actaId(),
                    "LOTE_CORREO_GENERADO",
                    BLOQUE_D4,
                    BLOQUE_D4,
                    "Notificación postal incorporada al lote demo " + loteId + ".");
            String actaNumero = acta != null ? acta.numeroActa() : n.actaId();
            lineas.add(csv(
                    loteId,
                    n.id(),
                    n.actaId(),
                    actaNumero,
                    n.tipo().name(),
                    n.canalTipificado().name(),
                    PrototipoStoreUtil.primerNoVacio(n.destinatarioNombre(), n.destinatarioResumen()),
                    n.domicilioTexto(),
                    referenciaExterna,
                    CSV_TS.format(ahora)));
            incluidas.add(itemDetalle(enviada, actaNumero));
        }

        Files.write(salida, lineas, StandardCharsets.UTF_8);
        CorreoLoteRegistro registro = new CorreoLoteRegistro(
                loteId,
                incluidas.size(),
                nombreArchivo,
                salida.toAbsolutePath().toString(),
                EstadoLoteCorreo.PENDIENTE_RESPUESTA,
                ahora,
                List.copyOf(incluidas));
        lotesGenerados.add(registro);
        return new PrototipoStore.GenerarLoteCorreoResultado(
                loteId,
                incluidas.size(),
                nombreArchivo,
                salida.toAbsolutePath().toString(),
                incluidas);
    }

    List<PrototipoStore.CorreoPostalNotificacionListaItem> listarNotificacionesListasParaLote() {
        return notificacionesListasBase().stream()
                .map(this::itemLista)
                .toList();
    }

    PrototipoStore.EnviarIndividualCorreoResultado enviarIndividual(String notificacionId) {
        if (notificacionId == null || notificacionId.isBlank()) {
            return PrototipoStore.EnviarIndividualCorreoResultado.conflict(
                    notificacionId, "Debe indicar la notificacion a enviar.");
        }
        String id = notificacionId.trim();
        Optional<ActaNotificacionMock> encontrada = buscarNotificacion(id, null);
        if (encontrada.isEmpty()) {
            return PrototipoStore.EnviarIndividualCorreoResultado.notFound(id);
        }
        ActaNotificacionMock notificacion = encontrada.get();
        if (notificacion.canalTipificado() != CanalNotificacion.CORREO_POSTAL) {
            return PrototipoStore.EnviarIndividualCorreoResultado.conflict(
                    id, "La notificacion " + id + " no es CORREO_POSTAL.");
        }
        if (notificacion.estado() != EstadoNotificacion.LISTA_PARA_ENVIO) {
            return PrototipoStore.EnviarIndividualCorreoResultado.conflict(
                    id, "La notificacion " + id + " no esta LISTA_PARA_ENVIO.");
        }
        if (notificacion.resultado() != ResultadoNotificacion.SIN_RESULTADO) {
            return PrototipoStore.EnviarIndividualCorreoResultado.conflict(
                    id, "La notificacion " + id + " no tiene SIN_RESULTADO.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        ActaNotificacionMock enviada = notificacion.conEnvioIndividualCorreo(ahora);
        reemplazarNotificacion(enviada);
        ActaMock acta = actas.get(notificacion.actaId());
        if (acta != null && BANDEJA_PENDIENTE_NOTIFICACION.equals(acta.bandejaActual())) {
            actas.put(acta.id(), moverAEnNotificacion(acta));
        }
        registrarEvento(
                notificacion.actaId(),
                "CORREO_ENVIO_INDIVIDUAL",
                BLOQUE_D4,
                BLOQUE_D4,
                "Notificacion postal enviada de forma individual demo sin lote.");
        String actaNumero = acta != null ? acta.numeroActa() : notificacion.actaId();
        return PrototipoStore.EnviarIndividualCorreoResultado.ok(
                id,
                "Notificacion " + id + " enviada individualmente sin lote.",
                itemDetalle(enviada, actaNumero));
    }

    List<PrototipoStore.CorreoPostalTrazabilidadItem> buscarTrazabilidadPorActa(String consultaActa) {
        if (consultaActa == null || consultaActa.isBlank()) {
            return List.of();
        }
        Set<String> actaIds = resolverActaIds(consultaActa.trim());
        if (actaIds.isEmpty()) {
            return List.of();
        }
        return notificacionesPorActa.entrySet().stream()
                .filter(e -> actaIds.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .filter(n -> n.canalTipificado() == CanalNotificacion.CORREO_POSTAL)
                .sorted(Comparator.comparing(ActaNotificacionMock::fechaEnvio, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ActaNotificacionMock::id))
                .map(this::itemTrazabilidad)
                .toList();
    }

    List<PrototipoStore.CorreoLoteResumen> listarLotesGenerados() {
        return lotesGenerados.stream()
                .sorted(Comparator.comparing(CorreoLoteRegistro::fechaGeneracion).reversed())
                .map(this::resumenLote)
                .toList();
    }

    Optional<PrototipoStore.CorreoLoteResumen> buscarLote(String loteId) {
        return lotesGenerados.stream()
                .filter(l -> l.loteId().equals(loteId))
                .findFirst()
                .map(this::resumenLote);
    }

    PrototipoStore.AnularLoteCorreoResultado anularLote(String loteId) {
        Optional<CorreoLoteRegistro> encontrado = lotesGenerados.stream()
                .filter(l -> l.loteId().equals(loteId))
                .findFirst();
        if (encontrado.isEmpty()) {
            return PrototipoStore.AnularLoteCorreoResultado.notFound(loteId);
        }
        CorreoLoteRegistro lote = encontrado.get();
        if (lote.estado() == EstadoLoteCorreo.PROCESADO) {
            return PrototipoStore.AnularLoteCorreoResultado.conflict(
                    loteId, "El lote ya fue procesado y no puede anularse.");
        }
        if (lote.estado() == EstadoLoteCorreo.ANULADO) {
            return PrototipoStore.AnularLoteCorreoResultado.conflict(
                    loteId, "El lote ya está anulado.");
        }
        for (PrototipoStore.NotificacionCorreoLoteItem item : lote.notificaciones()) {
            ActaNotificacionMock notificacion = buscarNotificacion(item.notificacionId(), loteId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró notificación " + item.notificacionId() + " en lote " + loteId + "."));
            if (notificacion.estado() != EstadoNotificacion.ENVIADA
                    || notificacion.resultado() != ResultadoNotificacion.SIN_RESULTADO) {
                return PrototipoStore.AnularLoteCorreoResultado.conflict(
                        loteId,
                        "La notificación " + item.notificacionId() + " ya tiene resultado y el lote no puede anularse.");
            }
            reemplazarNotificacion(revertirNotificacionLista(notificacion));
            revertirActaSiCorresponde(notificacion.actaId());
            registrarEvento(
                    notificacion.actaId(),
                    "LOTE_CORREO_ANULADO",
                    BLOQUE_D4,
                    BLOQUE_D4,
                    "Notificación postal devuelta a lista para envío por anulación del lote demo " + loteId + ".");
        }
        actualizarEstadoLote(loteId, EstadoLoteCorreo.ANULADO);
        return PrototipoStore.AnularLoteCorreoResultado.ok(
                loteId,
                "Lote " + loteId + " anulado. Las notificaciones volvieron a LISTA_PARA_ENVIO.");
    }

    PrototipoStore.ProcesarRespuestaCorreoResultado procesarRespuestaDemo(String loteId) throws IOException {
        if (loteId != null && !loteId.isBlank()) {
            return procesarRespuestaLote(loteId.trim());
        }
        Path respuesta = respuestaDemoPath();
        if (!Files.exists(respuesta)) {
            Optional<PrototipoStore.ProcesarRespuestaCorreoResultado> resultadoSinArchivo =
                    generarRespuestaDemoSiFalta(respuesta);
            if (resultadoSinArchivo.isPresent()) {
                return resultadoSinArchivo.get();
            }
        } else if (!csvCubreLotesPendientes(Files.readAllLines(respuesta, StandardCharsets.UTF_8))) {
            generarRespuestaDemoDesdeLotesPendientes(respuesta);
        }
        List<String> lineas = Files.readAllLines(respuesta, StandardCharsets.UTF_8);
        if (lineas.isEmpty()) {
            return resultadoVacioConError("Archivo CSV de respuesta vacío.");
        }
        Map<String, Integer> header = header(lineas.get(0));
        List<String> errores = new ArrayList<>();
        int total = 0;
        int positivas = 0;
        int negativas = 0;
        int vencidas = 0;

        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea == null || linea.isBlank()) {
                continue;
            }
            total++;
            List<String> columnas = parseCsvLine(linea);
            String filaLoteId = valor(columnas, header, "loteId");
            String notificacionId = valor(columnas, header, "notificacionId");
            String resultadoRaw = valor(columnas, header, "resultado");
            String fechaRaw = valor(columnas, header, "fechaResultado");
            String observacion = valor(columnas, header, "observacion");
            try {
                ResultadoNotificacion resultado = ResultadoNotificacion.valueOf(resultadoRaw.trim().toUpperCase(Locale.ROOT));
                if (resultado == ResultadoNotificacion.SIN_RESULTADO) {
                    throw new IllegalArgumentException("resultado sin efecto");
                }
                ActaNotificacionMock notificacion = buscarNotificacion(notificacionId, filaLoteId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No se encontró notificación " + notificacionId + " en lote " + filaLoteId + "."));
                LocalDateTime fechaResultado = parseFechaResultado(fechaRaw);
                aplicarResultado(notificacion, resultado, fechaResultado, observacion);
                if (resultado == ResultadoNotificacion.POSITIVA) {
                    positivas++;
                } else if (resultado == ResultadoNotificacion.NEGATIVA) {
                    negativas++;
                } else if (resultado == ResultadoNotificacion.VENCIDA) {
                    vencidas++;
                }
            } catch (RuntimeException ex) {
                errores.add("Fila " + (i + 1) + ": " + ex.getMessage());
            }
        }

        Path destino = (errores.isEmpty() ? procesadosDir() : errorDir())
                .resolve((errores.isEmpty() ? "procesado-" : "error-")
                        + FILE_TS.format(LocalDateTime.now()) + "-" + respuesta.getFileName());
        Files.createDirectories(destino.getParent());
        Files.copy(respuesta, destino, StandardCopyOption.REPLACE_EXISTING);
        if (errores.isEmpty()) {
            marcarLotesProcesadosPorCsv(lineas, header);
        }

        return new PrototipoStore.ProcesarRespuestaCorreoResultado(
                total,
                positivas,
                negativas,
                vencidas,
                errores.size(),
                errores,
                respuesta.getFileName().toString(),
                destino.toAbsolutePath().toString());
    }

    private PrototipoStore.ProcesarRespuestaCorreoResultado procesarRespuestaLote(String loteId) throws IOException {
        CorreoLoteRegistro lote = lotesGenerados.stream()
                .filter(item -> loteId.equals(item.loteId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado: " + loteId));
        if (lote.estado() == EstadoLoteCorreo.PROCESADO) {
            throw new IllegalStateException("El lote ya fue procesado.");
        }
        if (lote.estado() == EstadoLoteCorreo.ANULADO) {
            throw new IllegalStateException("El lote esta anulado y no puede procesarse.");
        }
        Path respuesta = respuestaDemoPath();
        generarRespuestaDemoParaLote(lote, respuesta);
        List<String> lineas = Files.readAllLines(respuesta, StandardCharsets.UTF_8);
        if (lineas.isEmpty()) {
            return resultadoVacioConError("Archivo CSV de respuesta vacio.");
        }
        Map<String, Integer> header = header(lineas.get(0));
        List<String> errores = new ArrayList<>();
        int total = 0;
        int positivas = 0;
        int negativas = 0;
        int vencidas = 0;

        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea == null || linea.isBlank()) {
                continue;
            }
            total++;
            List<String> columnas = parseCsvLine(linea);
            String filaLoteId = valor(columnas, header, "loteId");
            String notificacionId = valor(columnas, header, "notificacionId");
            String resultadoRaw = valor(columnas, header, "resultado");
            String fechaRaw = valor(columnas, header, "fechaResultado");
            String observacion = valor(columnas, header, "observacion");
            try {
                if (!loteId.equals(filaLoteId)) {
                    throw new IllegalArgumentException("La fila no pertenece al lote " + loteId + ".");
                }
                ResultadoNotificacion resultado = ResultadoNotificacion.valueOf(
                        resultadoRaw.trim().toUpperCase(Locale.ROOT));
                if (resultado == ResultadoNotificacion.SIN_RESULTADO) {
                    throw new IllegalArgumentException("resultado sin efecto");
                }
                ActaNotificacionMock notificacion = buscarNotificacion(notificacionId, filaLoteId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No se encontro notificacion " + notificacionId + " en lote " + filaLoteId + "."));
                LocalDateTime fechaResultado = parseFechaResultado(fechaRaw);
                aplicarResultado(notificacion, resultado, fechaResultado, observacion);
                if (resultado == ResultadoNotificacion.POSITIVA) {
                    positivas++;
                } else if (resultado == ResultadoNotificacion.NEGATIVA) {
                    negativas++;
                } else if (resultado == ResultadoNotificacion.VENCIDA) {
                    vencidas++;
                }
            } catch (RuntimeException ex) {
                errores.add("Fila " + (i + 1) + ": " + ex.getMessage());
            }
        }

        Path destino = (errores.isEmpty() ? procesadosDir() : errorDir())
                .resolve((errores.isEmpty() ? "procesado-" : "error-")
                        + FILE_TS.format(LocalDateTime.now()) + "-" + respuesta.getFileName());
        Files.createDirectories(destino.getParent());
        Files.copy(respuesta, destino, StandardCopyOption.REPLACE_EXISTING);
        if (errores.isEmpty()) {
            actualizarEstadoLote(loteId, EstadoLoteCorreo.PROCESADO);
        }

        return new PrototipoStore.ProcesarRespuestaCorreoResultado(
                total,
                positivas,
                negativas,
                vencidas,
                errores.size(),
                errores,
                respuesta.getFileName().toString(),
                destino.toAbsolutePath().toString());
    }

    private List<ActaNotificacionMock> resolverNotificacionesSeleccionadas(
            List<String> notificacionIds,
            String tipoFiltro) {
        TipoNotificacion tipo = null;
        if (tipoFiltro != null && !tipoFiltro.isBlank()) {
            tipo = TipoNotificacion.valueOf(tipoFiltro.trim().toUpperCase(Locale.ROOT));
        }
        List<String> errores = new ArrayList<>();
        List<ActaNotificacionMock> resultado = new ArrayList<>();
        Set<String> idsUnicos = new LinkedHashSet<>(notificacionIds);
        for (String id : idsUnicos) {
            if (id == null || id.isBlank()) {
                continue;
            }
            Optional<ActaNotificacionMock> encontrada = buscarNotificacion(id.trim(), null);
            if (encontrada.isEmpty()) {
                errores.add("Notificacion no encontrada: " + id);
                continue;
            }
            ActaNotificacionMock notificacion = encontrada.get();
            if (notificacion.canalTipificado() != CanalNotificacion.CORREO_POSTAL) {
                errores.add(id + " no es CORREO_POSTAL");
                continue;
            }
            if (notificacion.estado() != EstadoNotificacion.LISTA_PARA_ENVIO) {
                errores.add(id + " no esta LISTA_PARA_ENVIO");
                continue;
            }
            if (notificacion.resultado() != ResultadoNotificacion.SIN_RESULTADO) {
                errores.add(id + " no tiene SIN_RESULTADO");
                continue;
            }
            if (tipo != null && notificacion.tipo() != tipo) {
                errores.add(id + " no coincide con tipo " + tipo.name());
                continue;
            }
            resultado.add(notificacion);
        }
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errores));
        }
        if (resultado.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una notificacion valida.");
        }
        return resultado.stream()
                .sorted(Comparator.comparing(ActaNotificacionMock::actaId).thenComparing(ActaNotificacionMock::id))
                .toList();
    }

    private void generarRespuestaDemoParaLote(CorreoLoteRegistro lote, Path respuesta) throws IOException {
        Files.createDirectories(respuesta.getParent());
        List<String> lineas = new ArrayList<>();
        lineas.add("loteId,notificacionId,resultado,fechaResultado,observacion");
        LocalDateTime ahora = LocalDateTime.now();
        int indice = 0;
        for (PrototipoStore.NotificacionCorreoLoteItem item : lote.notificaciones()) {
            ResultadoNotificacion resultado = demoResultadoPara(item, indice);
            lineas.add(csv(
                    lote.loteId(),
                    item.notificacionId(),
                    resultado.name(),
                    CSV_TS.format(ahora.plusMinutes(indice)),
                    "Respuesta demo " + resultado.name() + " para " + item.tipo()));
            indice++;
        }
        Files.write(respuesta, lineas, StandardCharsets.UTF_8);
    }

    private void generarRespuestaDemoDesdeLotesPendientes(Path respuesta) throws IOException {
        List<CorreoLoteRegistro> pendientes = lotesGenerados.stream()
                .filter(lote -> lote.estado() == EstadoLoteCorreo.PENDIENTE_RESPUESTA)
                .sorted(Comparator.comparing(CorreoLoteRegistro::fechaGeneracion))
                .toList();
        if (pendientes.isEmpty()) {
            return;
        }
        Files.createDirectories(respuesta.getParent());
        List<String> lineas = new ArrayList<>();
        lineas.add("loteId,notificacionId,resultado,fechaResultado,observacion");
        LocalDateTime ahora = LocalDateTime.now();
        int indice = 0;
        for (CorreoLoteRegistro lote : pendientes) {
            for (PrototipoStore.NotificacionCorreoLoteItem item : lote.notificaciones()) {
                ResultadoNotificacion resultado = demoResultadoPara(item, indice);
                lineas.add(csv(
                        lote.loteId(),
                        item.notificacionId(),
                        resultado.name(),
                        CSV_TS.format(ahora.plusMinutes(indice)),
                        "Respuesta demo " + resultado.name() + " para " + item.tipo()));
                indice++;
            }
        }
        Files.write(respuesta, lineas, StandardCharsets.UTF_8);
    }

    private boolean csvCubreLotesPendientes(List<String> lineas) {
        Set<String> lotesPendientes = lotesGenerados.stream()
                .filter(lote -> lote.estado() == EstadoLoteCorreo.PENDIENTE_RESPUESTA)
                .map(CorreoLoteRegistro::loteId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (lotesPendientes.isEmpty()) {
            return true;
        }
        if (lineas.isEmpty()) {
            return false;
        }
        Map<String, Integer> header = header(lineas.get(0));
        Set<String> lotesEnCsv = new LinkedHashSet<>();
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea == null || linea.isBlank()) {
                continue;
            }
            List<String> columnas = parseCsvLine(linea);
            String loteId = valor(columnas, header, "loteId");
            if (loteId != null && !loteId.isBlank()) {
                lotesEnCsv.add(loteId);
            }
        }
        return lotesEnCsv.containsAll(lotesPendientes);
    }

    private static ResultadoNotificacion demoResultadoPara(
            PrototipoStore.NotificacionCorreoLoteItem item,
            int indice) {
        return switch (indice % 3) {
            case 1 -> ResultadoNotificacion.NEGATIVA;
            case 2 -> ResultadoNotificacion.VENCIDA;
            default -> ResultadoNotificacion.POSITIVA;
        };
    }

    private Optional<PrototipoStore.ProcesarRespuestaCorreoResultado> generarRespuestaDemoSiFalta(Path respuesta)
            throws IOException {
        List<ActaNotificacionMock> enviadasPendientes = notificacionesPorActa.values().stream()
                .flatMap(List::stream)
                .filter(n -> n.canalTipificado() == CanalNotificacion.CORREO_POSTAL)
                .filter(n -> n.estado() == EstadoNotificacion.ENVIADA)
                .filter(n -> n.resultado() == ResultadoNotificacion.SIN_RESULTADO)
                .filter(n -> n.loteId() != null && !n.loteId().isBlank())
                .sorted(Comparator.comparing(ActaNotificacionMock::loteId)
                        .thenComparing(ActaNotificacionMock::actaId)
                        .thenComparing(ActaNotificacionMock::id))
                .toList();
        if (enviadasPendientes.isEmpty()) {
            return Optional.of(resultadoVacioConError(
                    "No se encontró el archivo de respuesta demo en " + respuesta.toAbsolutePath()
                            + " y no hay notificaciones de correo postal enviadas pendientes para generar una respuesta automática."));
        }

        Files.createDirectories(respuesta.getParent());
        List<String> lineas = new ArrayList<>();
        lineas.add("loteId,notificacionId,resultado,fechaResultado,observacion");
        LocalDateTime ahora = LocalDateTime.now();
        for (ActaNotificacionMock n : enviadasPendientes) {
            lineas.add(csv(
                    n.loteId(),
                    n.id(),
                    ResultadoNotificacion.POSITIVA.name(),
                    CSV_TS.format(ahora),
                    "Respuesta positiva demo generada automáticamente para " + n.tipo().name()));
        }
        Files.write(respuesta, lineas, StandardCharsets.UTF_8);
        return Optional.empty();
    }

    private void aplicarResultado(
            ActaNotificacionMock notificacion,
            ResultadoNotificacion resultado,
            LocalDateTime fechaResultado,
            String observacion) {
        if (resultado == ResultadoNotificacion.POSITIVA) {
            PrototipoStore.RegistrarNotificacionPositivaResultado r = registrarPositiva.apply(notificacion.actaId());
            if (r.estado() != PrototipoStore.RegistrarNotificacionPositivaEstado.OK) {
                throw new IllegalStateException("No se pudo aplicar notificación positiva: " + r.estado());
            }
            ActaNotificacionMock actualizada = buscarNotificacion(notificacion.id(), notificacion.loteId())
                    .orElse(notificacion);
            reemplazarNotificacion(actualizada.conResultadoCorreo(
                    EstadoNotificacion.ENTREGADA,
                    ResultadoNotificacion.POSITIVA,
                    "ENTREGADA",
                    fechaResultado,
                    observacion,
                    eventoPositivo(notificacion.tipo())));
            return;
        }
        EstadoNotificacion estado = resultado == ResultadoNotificacion.NEGATIVA
                ? EstadoNotificacion.NEGATIVA
                : EstadoNotificacion.VENCIDA;
        String legacy = resultado == ResultadoNotificacion.NEGATIVA ? "NO_ENTREGADA" : "VENCIDA";
        String evento = resultado == ResultadoNotificacion.NEGATIVA ? "NOTIFICACION_NO_ENTREGADA" : "NOTIFICACION_VENCIDA";
        reemplazarNotificacion(notificacion.conResultadoCorreo(estado, resultado, legacy, fechaResultado, observacion, evento));
        ActaMock acta = actas.get(notificacion.actaId());
        if (acta != null && (BANDEJA_PENDIENTE_NOTIFICACION.equals(acta.bandejaActual())
                || BANDEJA_EN_NOTIFICACION.equals(acta.bandejaActual()))) {
            actas.put(acta.id(), moverAAnalisis(acta));
        }
        String accion = resultado == ResultadoNotificacion.NEGATIVA
                ? PrototipoStore.ACCION_REINTENTAR_NOTIFICACION
                : PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA;
        accionPendientePorActa.put(notificacion.actaId(), accion);
        registrarEvento(
                notificacion.actaId(),
                evento,
                BLOQUE_D4,
                BLOQUE_D5,
                "Respuesta de correo postal demo " + resultado.name() + " para "
                        + notificacion.tipo().name() + "; acción pendiente " + accion + ".");
    }

    private PrototipoStore.CorreoPostalNotificacionListaItem itemLista(ActaNotificacionMock n) {
        ActaMock acta = actas.get(n.actaId());
        String actaNumero = acta != null ? acta.numeroActa() : n.actaId();
        return new PrototipoStore.CorreoPostalNotificacionListaItem(
                n.id(),
                n.actaId(),
                actaNumero,
                n.tipo().name(),
                n.canalTipificado().name(),
                n.estado().name(),
                n.resultado().name(),
                PrototipoStoreUtil.primerNoVacio(n.destinatarioNombre(), n.destinatarioResumen()),
                n.domicilioTexto(),
                n.observacion());
    }

    private PrototipoStore.CorreoLoteResumen resumenLote(CorreoLoteRegistro lote) {
        List<PrototipoStore.NotificacionCorreoLoteItem> detalle = lote.notificaciones().stream()
                .map(item -> itemDetalle(
                        buscarNotificacion(item.notificacionId(), lote.loteId()).orElse(null),
                        item))
                .toList();
        List<String> tiposIncluidos = detalle.stream()
                .map(PrototipoStore.NotificacionCorreoLoteItem::tipo)
                .distinct()
                .sorted()
                .toList();
        int positivas = 0;
        int negativas = 0;
        int vencidas = 0;
        for (PrototipoStore.NotificacionCorreoLoteItem item : detalle) {
            if ("POSITIVA".equals(item.resultadoNotificacion())) {
                positivas++;
            } else if ("NEGATIVA".equals(item.resultadoNotificacion())) {
                negativas++;
            } else if ("VENCIDA".equals(item.resultadoNotificacion())) {
                vencidas++;
            }
        }
        return new PrototipoStore.CorreoLoteResumen(
                lote.loteId(),
                lote.cantidad(),
                lote.nombreArchivo(),
                lote.rutaArchivo(),
                lote.estado().name(),
                lote.fechaGeneracion(),
                tiposIncluidos,
                tipoDominante(tiposIncluidos),
                positivas,
                negativas,
                vencidas,
                detalle);
    }

    private PrototipoStore.NotificacionCorreoLoteItem itemDetalle(ActaNotificacionMock n, String actaNumero) {
        if (n == null) {
            throw new IllegalStateException("Notificación postal no encontrada para detalle de lote.");
        }
        return new PrototipoStore.NotificacionCorreoLoteItem(
                n.id(),
                n.actaId(),
                actaNumero,
                n.tipo().name(),
                n.canalTipificado().name(),
                n.referenciaExterna(),
                n.estado().name(),
                n.resultado().name(),
                PrototipoStoreUtil.primerNoVacio(n.destinatarioNombre(), n.destinatarioResumen()),
                n.domicilioTexto(),
                n.observacion());
    }

    private PrototipoStore.NotificacionCorreoLoteItem itemDetalle(
            ActaNotificacionMock actual,
            PrototipoStore.NotificacionCorreoLoteItem base) {
        if (actual != null) {
            ActaMock acta = actas.get(actual.actaId());
            String actaNumero = acta != null ? acta.numeroActa() : base.acta();
            return itemDetalle(actual, actaNumero);
        }
        return new PrototipoStore.NotificacionCorreoLoteItem(
                base.notificacionId(),
                base.actaId(),
                base.acta(),
                base.tipo(),
                base.canal(),
                base.referenciaExterna(),
                "ENVIADA",
                "SIN_RESULTADO",
                null,
                null,
                null);
    }

    private PrototipoStore.CorreoPostalTrazabilidadItem itemTrazabilidad(ActaNotificacionMock n) {
        ActaMock acta = actas.get(n.actaId());
        String actaNumero = acta != null ? acta.numeroActa() : n.actaId();
        String estadoLote = null;
        LocalDateTime fechaGeneracion = n.fechaEnvio();
        if (n.loteId() != null && !n.loteId().isBlank()) {
            Optional<CorreoLoteRegistro> lote = lotesGenerados.stream()
                    .filter(l -> l.loteId().equals(n.loteId()))
                    .findFirst();
            if (lote.isPresent()) {
                estadoLote = lote.get().estado().name();
                fechaGeneracion = lote.get().fechaGeneracion();
            }
        }
        return new PrototipoStore.CorreoPostalTrazabilidadItem(
                actaNumero,
                n.actaId(),
                n.id(),
                n.tipo().name(),
                n.canalTipificado().name(),
                n.estado().name(),
                n.resultado().name(),
                n.loteId(),
                estadoLote,
                fechaGeneracion,
                n.fechaResultado(),
                n.observacion(),
                n.referenciaExterna());
    }

    private List<ActaNotificacionMock> notificacionesListasBase() {
        return notificacionesPorActa.values().stream()
                .flatMap(List::stream)
                .filter(n -> n.canalTipificado() == CanalNotificacion.CORREO_POSTAL)
                .filter(n -> n.estado() == EstadoNotificacion.LISTA_PARA_ENVIO)
                .filter(n -> n.resultado() == ResultadoNotificacion.SIN_RESULTADO)
                .sorted(Comparator.comparing(ActaNotificacionMock::actaId).thenComparing(ActaNotificacionMock::id))
                .toList();
    }

    private Set<String> resolverActaIds(String consulta) {
        String normalizada = consulta.trim().toLowerCase(Locale.ROOT);
        Set<String> ids = new LinkedHashSet<>();
        if (normalizada.startsWith("acta-")) {
            if (actas.containsKey(consulta.trim().toUpperCase(Locale.ROOT))) {
                ids.add(consulta.trim().toUpperCase(Locale.ROOT));
            } else if (actas.containsKey(consulta.trim())) {
                ids.add(consulta.trim());
            }
        }
        for (Map.Entry<String, ActaMock> entry : actas.entrySet()) {
            ActaMock acta = entry.getValue();
            if (entry.getKey().equalsIgnoreCase(consulta)
                    || acta.numeroActa().equalsIgnoreCase(consulta)
                    || acta.numeroActa().toLowerCase(Locale.ROOT).contains(normalizada)
                    || entry.getKey().toLowerCase(Locale.ROOT).contains(normalizada)) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    private static String tipoDominante(List<String> tiposIncluidos) {
        if (tiposIncluidos.isEmpty()) {
            return null;
        }
        if (tiposIncluidos.size() == 1) {
            return tiposIncluidos.get(0);
        }
        return tiposIncluidos.get(0) + " +" + (tiposIncluidos.size() - 1);
    }

    private ActaNotificacionMock revertirNotificacionLista(ActaNotificacionMock notificacion) {
        return new ActaNotificacionMock(
                notificacion.id(),
                notificacion.actaId(),
                notificacion.canal(),
                "PENDIENTE_ENVIO",
                notificacion.destinatarioResumen(),
                notificacion.tipo(),
                notificacion.canalTipificado(),
                EstadoNotificacion.LISTA_PARA_ENVIO,
                ResultadoNotificacion.SIN_RESULTADO,
                notificacion.referencia(),
                notificacion.eventoRelacionado(),
                null,
                null,
                notificacion.fechaPreparacion(),
                null,
                null,
                notificacion.observacion(),
                notificacion.destinatarioNombre(),
                notificacion.destinatarioEmail(),
                notificacion.domicilioTexto(),
                notificacion.domicilioElectronicoVerificado(),
                notificacion.diasPlazoNotificacionElectronica());
    }

    private void revertirActaSiCorresponde(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null) {
            return;
        }
        boolean quedaEnviada = notificacionesPorActa.getOrDefault(actaId, List.of()).stream()
                .filter(n -> n.canalTipificado() == CanalNotificacion.CORREO_POSTAL)
                .anyMatch(n -> n.estado() == EstadoNotificacion.ENVIADA
                        && n.resultado() == ResultadoNotificacion.SIN_RESULTADO);
        if (quedaEnviada) {
            return;
        }
        if (BANDEJA_EN_NOTIFICACION.equals(acta.bandejaActual())
                || BANDEJA_PENDIENTE_NOTIFICACION.equals(acta.bandejaActual())) {
            actas.put(actaId, moverAPendienteNotificacion(acta));
        }
    }

    void reiniciarDemo() {
        lotesGenerados.clear();
        secuenciaLote.set(0);
    }

    private ActaMock moverAPendienteNotificacion(ActaMock actual) {
        return new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D4,
                "PENDIENTE_ENVIO",
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_NOTIFICACION);
    }

    private void actualizarEstadoLote(String loteId, EstadoLoteCorreo nuevoEstado) {
        for (int i = 0; i < lotesGenerados.size(); i++) {
            CorreoLoteRegistro actual = lotesGenerados.get(i);
            if (loteId.equals(actual.loteId())) {
                lotesGenerados.set(i, new CorreoLoteRegistro(
                        actual.loteId(),
                        actual.cantidad(),
                        actual.nombreArchivo(),
                        actual.rutaArchivo(),
                        nuevoEstado,
                        actual.fechaGeneracion(),
                        actual.notificaciones()));
                return;
            }
        }
    }

    private void marcarLotesProcesadosPorCsv(List<String> lineas, Map<String, Integer> header) {
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea == null || linea.isBlank()) {
                continue;
            }
            List<String> columnas = parseCsvLine(linea);
            String loteId = valor(columnas, header, "loteId");
            if (loteId == null || loteId.isBlank()) {
                continue;
            }
            actualizarEstadoLote(loteId, EstadoLoteCorreo.PROCESADO);
        }
    }

    private Optional<ActaNotificacionMock> buscarNotificacion(String notificacionId, String loteId) {
        if (notificacionId != null && !notificacionId.isBlank()) {
            return notificacionesPorActa.values().stream()
                    .flatMap(List::stream)
                    .filter(n -> notificacionId.equals(n.id()))
                    .findFirst();
        }
        return notificacionesPorActa.values().stream()
                .flatMap(List::stream)
                .filter(n -> loteId != null && loteId.equals(n.loteId()))
                .findFirst();
    }

    private void reemplazarNotificacion(ActaNotificacionMock actualizada) {
        List<ActaNotificacionMock> lista = notificacionesPorActa.get(actualizada.actaId());
        if (lista == null) {
            return;
        }
        for (int i = 0; i < lista.size(); i++) {
            if (Objects.equals(lista.get(i).id(), actualizada.id())) {
                lista.set(i, actualizada);
                return;
            }
        }
    }

    private ActaMock moverAEnNotificacion(ActaMock actual) {
        return new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                actual.bloqueActual(),
                "EN_ENVIO",
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                true,
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_EN_NOTIFICACION);
    }

    private ActaMock moverAAnalisis(ActaMock actual) {
        return new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                true,
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
    }

    private void registrarEvento(
            String actaId,
            String tipoEvento,
            String bloqueOrigen,
            String bloqueDestino,
            String descripcion) {
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = PrototipoStoreUtil.sufijoActa(actaId);
        int siguiente = eventos.size() + 1;
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente),
                actaId,
                fechaEvento,
                tipoEvento,
                bloqueOrigen,
                bloqueDestino,
                descripcion));
    }

    private PrototipoStore.ProcesarRespuestaCorreoResultado resultadoVacioConError(String error) {
        return new PrototipoStore.ProcesarRespuestaCorreoResultado(
                0, 0, 0, 0, 1, List.of(error), respuestaDemoPath().getFileName().toString(), null);
    }

    private Path salidaDir() {
        return baseDir.resolve("salida");
    }

    private Path respuestaDemoPath() {
        return baseDir.resolve("respuesta").resolve("respuesta-correo-demo.csv");
    }

    private Path procesadosDir() {
        return baseDir.resolve("procesados");
    }

    private Path errorDir() {
        return baseDir.resolve("error");
    }

    private static String eventoPositivo(TipoNotificacion tipo) {
        return switch (tipo) {
            case ACTA_INFRACCION -> "NOTIFICACION_ENTREGADA";
            case FALLO_ABSOLUTORIO -> "FALLO_ABSOLUTORIO_NOTIFICADO";
            case FALLO_CONDENATORIO -> "FALLO_CONDENATORIO_NOTIFICADO";
        };
    }

    private static LocalDateTime parseFechaResultado(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        String v = raw.trim();
        try {
            return LocalDateTime.parse(v, CSV_TS);
        } catch (DateTimeParseException ex) {
            return LocalDate.parse(v).atStartOfDay();
        }
    }

    private static Map<String, Integer> header(String linea) {
        List<String> columnas = parseCsvLine(linea);
        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (int i = 0; i < columnas.size(); i++) {
            resultado.put(columnas.get(i).trim(), i);
        }
        return resultado;
    }

    private static String valor(List<String> columnas, Map<String, Integer> header, String nombre) {
        Integer idx = header.get(nombre);
        if (idx == null || idx >= columnas.size()) {
            return "";
        }
        return columnas.get(idx);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String csv(String... values) {
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            escaped.add(escapeCsv(value));
        }
        return String.join(",", escaped);
    }

    private static String escapeCsv(String raw) {
        if (raw == null) {
            return "";
        }
        boolean quote = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r");
        String escaped = raw.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

}
