package ar.gob.malvinas.faltas.prototipo.bandeja;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Etiquetas operativas de card de bandeja (chip y acción principal). Los códigos
 * internos (D1, sub-bandeja, bloque) no se muestran al usuario.
 */
public final class ActaBandejaUxPresenter {

    private static final Set<String> PENDIENTES_GESTION_BLOQUEOS = Set.of(
            "LEVANTAMIENTO_MEDIDA_PREVENTIVA",
            "LIBERACION_RODADO",
            "ENTREGA_DOCUMENTACION");

    private ActaBandejaUxPresenter() {}

    public static String chipVisible(SubBandejaCodigo codigo, SubBandejaContexto ctx) {
        if (ctx != null && ctx.acta() != null && tienePendientesBloqueantes(ctx)) {
            String bandeja = ctx.acta().bandejaActual();
            if ("ACTAS_EN_ENRIQUECIMIENTO".equals(bandeja)
                    && (codigo == SubBandejaCodigo.CAPTURA_INICIAL
                            || codigo == SubBandejaCodigo.ENRIQUECIMIENTO_GENERAL)) {
                return "Pendientes materiales";
            }
        }
        String desdeBloque = chipDesdeBloque(ctx != null && ctx.acta() != null ? ctx.acta().bloqueActual() : null);
        if (desdeBloque != null && chipEsTecnico(codigo.chip())) {
            return desdeBloque;
        }
        return humanizarChip(codigo.chip());
    }

    public static String accionPrincipalVisible(SubBandejaCodigo codigo, SubBandejaContexto ctx) {
        if (ctx != null && tienePendientesBloqueantes(ctx)) {
            if (tienePendienteGestionMaterial(ctx)) {
                return "Gestionar bloqueos";
            }
            return "Resolver pendientes";
        }
        return refinarAccion(codigo, ctx);
    }

    public static boolean chipPrimarioCubreBloqueoMaterial(SubBandejaCodigo codigo, SubBandejaContexto ctx) {
        return "Pendientes materiales".equals(chipVisible(codigo, ctx));
    }

    private static boolean tienePendientesBloqueantes(SubBandejaContexto ctx) {
        return ctx.pendientesBloqueantes() != null && !ctx.pendientesBloqueantes().isEmpty();
    }

    private static boolean tienePendienteGestionMaterial(SubBandejaContexto ctx) {
        List<String> pendientes = ctx.pendientesBloqueantes();
        if (pendientes == null) {
            return false;
        }
        return pendientes.stream().anyMatch(PENDIENTES_GESTION_BLOQUEOS::contains);
    }

    private static String refinarAccion(SubBandejaCodigo codigo, SubBandejaContexto ctx) {
        return switch (codigo) {
            case CAPTURA_INICIAL, ENRIQUECIMIENTO_GENERAL -> "Completar datos";
            case GENERACION_ACTA_PENDIENTE, GENERACION_PIEZAS_PENDIENTE,
                    REDACCION_NULIDAD, REDACCION_MEDIDA, REDACCION_RECTIFICACION,
                    REDACCION_RESOLUCION, REDACCION_GENERAL -> "Generar pieza";
            case FIRMA_FALLO_CONDENATORIO, FIRMA_FALLO_ABSOLUTORIO, FIRMA_ACTA_INICIAL, FIRMA_OTRAS_PIEZAS ->
                    "Firmar documento";
            case NOTIF_FALLO_CONDENATORIO_LISTA, NOTIF_FALLO_ABSOLUTORIO_LISTA, NOTIF_ACTA_LISTA_ENVIO,
                    NOTIF_LISTA_OTRO -> "Gestionar notificación";
            case ANALISIS_BLOQUEO_OPERATIVO -> "Resolver pendientes";
            case ANALISIS_REVISION_GENERAL, ANALISIS_NOTIF_POSITIVA, ANALISIS_PENDIENTE_FALLO,
                    ANALISIS_LISTO_DERIVAR_EXTERNA, ANALISIS_NOTIF_VENCIDA, ANALISIS_NOTIF_NEGATIVA,
                    ANALISIS_POST_GESTION_EXTERNA, ANALISIS_POST_REINGRESO, ANALISIS_PAGO_INFORMADO,
                    ANALISIS_PAGO_SOLICITADO, CONDENA_LISTO_CIERRE, CONDENA_PAGO_CONFIRMADO,
                    CONDENA_PAGO_OBSERVADO, CONDENA_PAGO_INFORMADO, CONDENA_PAGO_PENDIENTE_INFORMAR ->
                    "Analizar expediente";
            case FALLO_TRAS_PAGO_INFORMADO, FALLO_LISTO_ABSOLUTORIO, FALLO_LISTO_CONDENATORIO -> "Emitir fallo";
            default -> humanizarAccion(codigo.accionPrincipal(), ctx);
        };
    }

    private static String humanizarAccion(String accion, SubBandejaContexto ctx) {
        if (accion == null || accion.isBlank()) {
            return accion;
        }
        return switch (accion) {
            case "Completar acta" -> "Completar datos";
            case "Dictar fallo", "Firmar fallo" -> codigoEsFirma(ctx) ? "Firmar documento" : "Emitir fallo";
            case "Firmar acta" -> "Firmar documento";
            case "Preparar notificación" -> "Gestionar notificación";
            case "Resolver bloqueo" -> "Resolver pendientes";
            case "Generar acta", "Generar piezas", "Generar nulidad", "Generar medida", "Generar rectificación",
                    "Generar resolución" -> "Generar pieza";
            default -> accion;
        };
    }

    private static boolean codigoEsFirma(SubBandejaContexto ctx) {
        return ctx != null
                && ctx.acta() != null
                && "PENDIENTE_FIRMA".equals(ctx.acta().bandejaActual());
    }

    private static String chipDesdeBloque(String bloque) {
        if (bloque == null || bloque.isBlank()) {
            return null;
        }
        return switch (bloque) {
            case "D1_CAPTURA" -> "Captura inicial";
            case "D2_ENRIQUECIMIENTO" -> "Enriquecimiento";
            case "D3_PREPARACION_DOCUMENTAL", "D3_DOCUMENTAL", "D3" -> "Completitud documental";
            case "D5_ANALISIS", "D5" -> "Análisis";
            default -> {
                if (bloque.startsWith("D5")) {
                    yield "Análisis";
                }
                if (bloque.contains("PREPARACION") || bloque.contains("DOCUMENTAL")) {
                    yield "Completitud documental";
                }
                yield null;
            }
        };
    }

    private static boolean chipEsTecnico(String chip) {
        if (chip == null || chip.isBlank()) {
            return false;
        }
        String t = chip.trim();
        if (t.matches("^D[0-9]$")) {
            return true;
        }
        return t.equals("D1") || t.equals("D2") || t.equals("D3") || t.equals("D5");
    }

    private static String humanizarChip(String chip) {
        if (chip == null || chip.isBlank()) {
            return chip;
        }
        if (chipEsTecnico(chip)) {
            return chipDesdeBloque(chip + "_CAPTURA");
        }
        String lower = chip.toLowerCase(Locale.ROOT);
        if (lower.contains("fallo")) {
            return "Fallo";
        }
        if (lower.contains("apelacion") || lower.contains("apelación")) {
            return "Apelación";
        }
        if (lower.contains("paraliz")) {
            return "Paralizada";
        }
        return chip;
    }
}
