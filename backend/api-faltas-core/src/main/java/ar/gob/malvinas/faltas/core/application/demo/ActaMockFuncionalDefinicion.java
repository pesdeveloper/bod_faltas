package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;

import java.util.List;

/**
 * Definicion declarativa de un acta mock funcional.
 *
 * Cada instancia describe:
 * - que caso de uso cubre
 * - en que estado inicial se encuentra el acta
 * - que documentos se esperan
 * - que eventos del dominio deben haberse registrado
 * - que endpoints/servicios cubre
 *
 * No ejecuta flujos. Es la fuente canonica del dataset funcional in-memory.
 *
 * Slice 8F-4B.
 */
public record ActaMockFuncionalDefinicion(
        String codigo,
        String titulo,
        String descripcion,
        String casoUsoPrincipal,
        List<String> casosUsoCubiertos,
        BloqueActual bloqueEsperado,
        SituacionAdministrativaActa situacionEsperada,
        ResultadoFinalActa resultadoFinalEsperado,
        CodigoBandeja bandejaEsperada,
        boolean cerrableEsperado,
        boolean paralizadaEsperada,
        boolean requiereFallo,
        boolean requierePago,
        boolean requiereNotificacion,
        boolean requiereFirmaDocumental,
        boolean requiereAdjunto,
        boolean requiereMedidaPreventiva,
        boolean requiereResolutorioBloqueante,
        List<DocumentoEsperadoPorActaMock> documentosEsperados,
        List<TipoEventoActa> eventosEsperados,
        List<String> endpointsServiciosCubiertos,
        List<String> observaciones
) {}
