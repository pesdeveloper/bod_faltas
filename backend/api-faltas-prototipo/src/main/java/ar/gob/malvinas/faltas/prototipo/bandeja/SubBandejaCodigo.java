package ar.gob.malvinas.faltas.prototipo.bandeja;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Taxonomía operativa de sub-bandejas dinámicas por bandeja principal.
 * Prioridad ascendente: menor número = mayor prioridad al clasificar.
 */
public enum SubBandejaCodigo {
    CAPTURA_INICIAL(
            "ACTAS_EN_ENRIQUECIMIENTO",
            10,
            "Captura inicial",
            "Captura inicial",
            "Completar datos",
            "Completar datos de labrado"),
    PAGO_VOLUNTARIO_ORIGINADO(
            "ACTAS_EN_ENRIQUECIMIENTO",
            20,
            "Pago voluntario originado",
            "Pago solicitado",
            "Evaluar pago",
            "Pago voluntario en curso"),
    ENRIQUECIMIENTO_GENERAL(
            "ACTAS_EN_ENRIQUECIMIENTO",
            90,
            "Enriquecimiento general",
            "Enriquecimiento",
            "Completar datos",
            "Completar enriquecimiento"),

    GENERACION_ACTA_PENDIENTE(
            "PENDIENTE_PREPARACION_DOCUMENTAL",
            10,
            "Generación acta pendiente",
            "Generación acta",
            "Generar acta",
            "Pendiente generación del acta"),
    GENERACION_PIEZAS_PENDIENTE(
            "PENDIENTE_PREPARACION_DOCUMENTAL",
            20,
            "Generación piezas pendiente",
            "Piezas pendientes",
            "Generar piezas",
            "Piezas documentales pendientes"),
    REVISION_DOCUMENTAL(
            "PENDIENTE_PREPARACION_DOCUMENTAL",
            90,
            "Revisión documental",
            "Revisión",
            "Revisar expediente",
            "Revisión documental general"),

    FIRMA_FALLO_CONDENATORIO(
            "PENDIENTE_FIRMA",
            10,
            "Firma fallo condenatorio",
            "Fallo condenatorio",
            "Firmar fallo",
            "Fallo condenatorio pendiente de firma"),
    FIRMA_FALLO_ABSOLUTORIO(
            "PENDIENTE_FIRMA",
            15,
            "Firma fallo absolutorio",
            "Fallo absolutorio",
            "Firmar fallo",
            "Fallo absolutorio pendiente de firma"),
    FIRMA_ACTA_INICIAL(
            "PENDIENTE_FIRMA",
            20,
            "Firma acta inicial",
            "Acta inicial",
            "Firmar acta",
            "Acta inicial pendiente de firma"),
    FIRMA_OTRAS_PIEZAS(
            "PENDIENTE_FIRMA",
            90,
            "Firma otras piezas",
            "Otras piezas",
            "Firmar documento",
            "Otras piezas pendientes de firma"),

    NOTIF_FALLO_CONDENATORIO_LISTA(
            "PENDIENTE_NOTIFICACION",
            10,
            "Fallo condenatorio listo envío",
            "Fallo condenatorio",
            "Preparar notificación",
            "Fallo condenatorio listo para envío"),
    NOTIF_FALLO_ABSOLUTORIO_LISTA(
            "PENDIENTE_NOTIFICACION",
            15,
            "Fallo absolutorio listo envío",
            "Fallo absolutorio",
            "Preparar notificación",
            "Fallo absolutorio listo para envío"),
    NOTIF_ACTA_LISTA_ENVIO(
            "PENDIENTE_NOTIFICACION",
            20,
            "Acta lista para envío",
            "Acta",
            "Preparar notificación",
            "Acta lista para envío"),
    NOTIF_LISTA_OTRO(
            "PENDIENTE_NOTIFICACION",
            90,
            "Otra notificación lista",
            "Notificación",
            "Preparar notificación",
            "Otra notificación pendiente de envío"),

    NOTIF_NEGATIVA_PENDIENTE_DECISION(
            "EN_NOTIFICACION",
            10,
            "Notificación negativa pendiente decisión",
            "Negativa",
            "Evaluar resultado",
            "Resultado negativo pendiente de decisión"),
    NOTIF_VENCIDA_PENDIENTE_DECISION(
            "EN_NOTIFICACION",
            11,
            "Notificación vencida pendiente decisión",
            "Vencida",
            "Evaluar vencimiento",
            "Notificación vencida pendiente de decisión"),
    NOTIF_EN_CORREO_POSTAL(
            "EN_NOTIFICACION",
            20,
            "En correo postal",
            "Correo postal",
            "Seguir envío",
            "Notificación en trámite por correo postal"),
    NOTIF_EN_NOTIFICADOR_MUNICIPAL(
            "EN_NOTIFICACION",
            21,
            "En notificador municipal",
            "Notificador municipal",
            "Seguir envío",
            "Notificación en trámite por notificador municipal"),
    NOTIF_EN_DOMICILIO_ELECTRONICO(
            "EN_NOTIFICACION",
            22,
            "En domicilio electrónico",
            "Domicilio electrónico",
            "Seguir envío",
            "Notificación en trámite por domicilio electrónico"),
    NOTIF_EN_OTRO_CANAL(
            "EN_NOTIFICACION",
            90,
            "En otro canal",
            "Otro canal",
            "Seguir envío",
            "Notificación en trámite por otro canal"),

    ANALISIS_BLOQUEO_OPERATIVO(
            "PENDIENTE_ANALISIS",
            5,
            "Bloqueo operativo",
            "Bloqueo material",
            "Resolver bloqueo",
            "Pendiente bloqueante de cierre"),
    CONDENA_LISTO_CIERRE(
            "PENDIENTE_ANALISIS",
            10,
            "Listo para cierre",
            "Cerrable",
            "Cerrar acta",
            "Expediente cerrable"),
    CONDENA_PAGO_CONFIRMADO(
            "PENDIENTE_ANALISIS",
            15,
            "Pago de condena confirmado",
            "Pago confirmado",
            "Cerrar o continuar",
            "Pago de condena confirmado"),
    CONDENA_PAGO_OBSERVADO(
            "PENDIENTE_ANALISIS",
            20,
            "Pago de condena observado",
            "Pago observado",
            "Revisar pago",
            "Pago de condena observado"),
    CONDENA_PAGO_INFORMADO(
            "PENDIENTE_ANALISIS",
            25,
            "Pago de condena informado",
            "Pago informado",
            "Confirmar pago",
            "Pago de condena informado"),
    CONDENA_PAGO_PENDIENTE_INFORMAR(
            "PENDIENTE_ANALISIS",
            30,
            "Pago de condena pendiente informar",
            "Pago pendiente",
            "Informar pago",
            "Pago de condena pendiente de informar"),
    ANALISIS_LISTO_DERIVAR_EXTERNA(
            "PENDIENTE_ANALISIS",
            35,
            "Listo derivar gestión externa",
            "Derivación externa",
            "Derivar expediente",
            "Listo para derivación externa"),
    ANALISIS_NOTIF_VENCIDA(
            "PENDIENTE_ANALISIS",
            40,
            "Notificación vencida",
            "Notif. vencida",
            "Evaluar vencimiento",
            "Evaluar notificación vencida"),
    ANALISIS_NOTIF_NEGATIVA(
            "PENDIENTE_ANALISIS",
            41,
            "Notificación negativa",
            "Notif. negativa",
            "Reintentar notificación",
            "Evaluar notificación negativa"),
    ANALISIS_POST_GESTION_EXTERNA(
            "PENDIENTE_ANALISIS",
            42,
            "Post gestión externa",
            "Post externa",
            "Revisar reingreso",
            "Revisión post gestión externa"),
    ANALISIS_POST_REINGRESO(
            "PENDIENTE_ANALISIS",
            43,
            "Post reingreso desde archivo",
            "Post reingreso",
            "Revisar reingreso",
            "Revisión post reingreso desde archivo"),
    ANALISIS_PAGO_INFORMADO(
            "PENDIENTE_ANALISIS",
            44,
            "Pago informado pendiente verificación",
            "Pago informado",
            "Verificar pago",
            "Verificar pago informado"),
    ANALISIS_PAGO_SOLICITADO(
            "PENDIENTE_ANALISIS",
            45,
            "Pago voluntario solicitado",
            "Pago solicitado",
            "Evaluar pago",
            "Evaluar solicitud de pago voluntario"),
    ANALISIS_PENDIENTE_FALLO(
            "PENDIENTE_ANALISIS",
            52,
            "Pendiente de fallo",
            "Sin fallo",
            "Dictar fallo",
            "Expediente pendiente de fallo"),
    ANALISIS_NOTIF_POSITIVA(
            "PENDIENTE_ANALISIS",
            50,
            "Notificación fehaciente positiva",
            "Notificada positiva",
            "Analizar expediente",
            "Notificación positiva — análisis jurídico"),
    ANALISIS_REVISION_GENERAL(
            "PENDIENTE_ANALISIS",
            90,
            "Revisión general",
            "Análisis",
            "Analizar expediente",
            "Revisión general de análisis"),

    REDACCION_NULIDAD(
            "PENDIENTES_RESOLUCION_REDACCION",
            10,
            "Redacción nulidad",
            "Nulidad",
            "Generar nulidad",
            "Redacción de nulidad"),
    REDACCION_MEDIDA(
            "PENDIENTES_RESOLUCION_REDACCION",
            15,
            "Redacción medida preventiva",
            "Medida preventiva",
            "Generar medida",
            "Redacción de medida preventiva"),
    REDACCION_RECTIFICACION(
            "PENDIENTES_RESOLUCION_REDACCION",
            20,
            "Redacción rectificación",
            "Rectificación",
            "Generar rectificación",
            "Redacción de rectificación"),
    REDACCION_RESOLUCION(
            "PENDIENTES_RESOLUCION_REDACCION",
            25,
            "Redacción resolución",
            "Resolución",
            "Generar resolución",
            "Redacción de resolución"),
    REDACCION_GENERAL(
            "PENDIENTES_RESOLUCION_REDACCION",
            90,
            "Redacción general",
            "Redacción",
            "Generar pieza",
            "Redacción general"),

    FALLO_TRAS_PAGO_INFORMADO(
            "PENDIENTES_FALLO",
            10,
            "Fallo tras pago informado",
            "Pago informado",
            "Dictar fallo",
            "Pago informado — expediente derivado a fallo"),
    FALLO_LISTO_ABSOLUTORIO(
            "PENDIENTES_FALLO",
            15,
            "Fallo absolutorio pendiente",
            "Absolutorio",
            "Dictar fallo",
            "Lista para fallo absolutorio"),
    FALLO_LISTO_CONDENATORIO(
            "PENDIENTES_FALLO",
            90,
            "Fallo condenatorio pendiente",
            "Condenatorio",
            "Dictar fallo",
            "Lista para fallo condenatorio"),

    APELACION_EN_ANALISIS(
            "CON_APELACION",
            10,
            "Apelación en análisis",
            "En análisis",
            "Resolver apelación",
            "Apelación presentada — revisión jurídica"),
    APELACION_RESUELTA(
            "CON_APELACION",
            15,
            "Apelación resuelta",
            "Resuelta",
            "Definir siguiente paso",
            "Apelación resuelta — acción posterior pendiente"),
    APELACION_PENDIENTE_RESOLUCION(
            "CON_APELACION",
            30,
            "Apelación pendiente de resolución",
            "Pendiente",
            "Resolver apelación",
            "Apelación presentada — pendiente de resolución"),

    PARALIZ_ESPERA_DOCUMENTAL(
            "PARALIZADAS",
            10,
            "Espera documental",
            "Documental",
            "Levantar paralización",
            "Paralizada por espera de documentación"),
    PARALIZ_TRAMITE_EXTERNO(
            "PARALIZADAS",
            15,
            "Trámite externo / informe",
            "Trámite externo",
            "Levantar paralización",
            "Paralizada por trámite externo o informe"),
    PARALIZ_CAUSA_ADMINISTRATIVA(
            "PARALIZADAS",
            20,
            "Causa administrativa",
            "Administrativa",
            "Levantar paralización",
            "Paralizada por causa administrativa fundada"),

    EXT_APREMIO("GESTION_EXTERNA", 10, "Apremio", "Apremio", "Seguir gestión", "Gestión externa — apremio"),
    EXT_JUZGADO_PAZ(
            "GESTION_EXTERNA",
            10,
            "Juzgado de Paz",
            "Juzgado de Paz",
            "Seguir gestión",
            "Gestión externa — Juzgado de Paz"),
    EXT_PENDIENTE_REINGRESO(
            "GESTION_EXTERNA",
            20,
            "Pendiente reingreso",
            "Reingreso",
            "Reingresar acta",
            "Pendiente de reingreso desde gestión externa"),
    EXT_SEGUIMIENTO(
            "GESTION_EXTERNA",
            90,
            "Seguimiento externo",
            "Seguimiento",
            "Seguir gestión",
            "Seguimiento en gestión externa"),

    ARCHIVO_POST_VENCIMIENTO(
            "ARCHIVO",
            10,
            "Archivo post vencimiento",
            "Post vencimiento",
            "Reingresar o cerrar",
            "Archivado tras evaluación de vencimiento"),
    ARCHIVO_DESDE_ANALISIS(
            "ARCHIVO",
            15,
            "Archivo desde análisis",
            "Desde análisis",
            "Reingresar acta",
            "Archivado directamente desde análisis"),
    ARCHIVO_JURIDICO("ARCHIVO", 20, "Archivo jurídico", "Jurídico", "Reingresar acta", "Archivo por decisión jurídica"),
    ARCHIVO_REINGRESO_PERMITIDO(
            "ARCHIVO",
            25,
            "Archivo con reingreso permitido",
            "Reingreso permitido",
            "Reingresar acta",
            "Archivo operativo con reingreso permitido"),
    ARCHIVO_DEFINITIVO(
            "ARCHIVO",
            30,
            "Archivo definitivo",
            "Definitivo",
            "Consultar expediente",
            "Archivo definitivo sin reingreso"),
    ARCHIVO_OPERATIVO(
            "ARCHIVO",
            90,
            "Archivo operativo",
            "Archivo",
            "Reingresar acta",
            "Archivo operativo general"),

    CERRADA_PAGO_VOLUNTARIO(
            "CERRADAS",
            10,
            "Cerrada por pago voluntario",
            "Pago voluntario",
            "Consultar",
            "Cerrada por pago voluntario"),
    CERRADA_PAGO_CONDENA(
            "CERRADAS",
            15,
            "Cerrada por pago de condena",
            "Pago condena",
            "Consultar",
            "Cerrada por pago de condena"),
    CERRADA_ABSOLUCION(
            "CERRADAS",
            20,
            "Cerrada por absolución",
            "Absolución",
            "Consultar",
            "Cerrada por absolución"),
    CERRADA_NULIDAD("CERRADAS", 25, "Cerrada por nulidad", "Nulidad", "Consultar", "Cerrada por nulidad"),
    CERRADA_ARCHIVO_DEFINITIVO(
            "CERRADAS",
            30,
            "Cerrada por archivo definitivo",
            "Archivo definitivo",
            "Consultar",
            "Cerrada tras archivo definitivo"),
    CERRADA_OTRA_CAUSA(
            "CERRADAS",
            90,
            "Cerrada otra causa",
            "Cerrada",
            "Consultar",
            "Cerrada por otra causa");

    private final String bandejaPadre;
    private final int prioridad;
    private final String label;
    private final String chip;
    private final String accionPrincipal;
    private final String descripcion;

    SubBandejaCodigo(
            String bandejaPadre,
            int prioridad,
            String label,
            String chip,
            String accionPrincipal,
            String descripcion) {
        this.bandejaPadre = bandejaPadre;
        this.prioridad = prioridad;
        this.label = label;
        this.chip = chip;
        this.accionPrincipal = accionPrincipal;
        this.descripcion = descripcion;
    }

    public String codigo() {
        return name();
    }

    public String bandejaPadre() {
        return bandejaPadre;
    }

    public int prioridad() {
        return prioridad;
    }

    public String label() {
        return label;
    }

    public String chip() {
        return chip;
    }

    public String accionPrincipal() {
        return accionPrincipal;
    }

    public String descripcion() {
        return descripcion;
    }

    public static Optional<SubBandejaCodigo> porCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(SubBandejaCodigo.valueOf(codigo.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static List<SubBandejaCodigo> deBandeja(String bandejaPadre) {
        return Arrays.stream(values())
                .filter(s -> s.bandejaPadre.equals(bandejaPadre))
                .sorted(Comparator.comparingInt(SubBandejaCodigo::prioridad))
                .toList();
    }

    public static boolean perteneceABandeja(String bandejaPadre, String subBandejaCodigo) {
        return porCodigo(subBandejaCodigo).map(s -> s.bandejaPadre.equals(bandejaPadre)).orElse(false);
    }
}
