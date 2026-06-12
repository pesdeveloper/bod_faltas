package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.CerrabilidadResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReconocerOrigenBloqueanteMaterialAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarConstatacionMaterialTempranaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarCumplimientoMaterialBloqueoCierreAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarMedidaPreventivaPosteriorAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarResolucionBloqueoCierreAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.mapper.ActaDetalleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoMaterialesController {

    private final PrototipoStore store;

    public PrototipoMaterialesController(PrototipoStore store) {
        this.store = store;
    }

    /**
     * Registra el cumplimiento material efectivo (alias de
     * {@code POST .../registrar-cumplimiento-material-bloqueo-cierre}).
     */
    @PostMapping("/actas/{id}/acciones/resolver-pendiente-bloqueante-cierre")
    public RegistrarCumplimientoMaterialBloqueoCierreAccionResponse resolverPendienteBloqueanteCierre(
            @PathVariable("id") String id,
            @RequestParam("tipo") String tipo) {
        return ejecutarRegistrarCumplimientoMaterialBloqueoCierre(id, tipo);
    }

    /**
     * Constatación mínima en D1/D2: incorpora al expediente el ancla
     * documental alineada con
     * {@code ACTA_RETENCION}, {@code CONSTATACION_RETENCION_DOCUMENTACION} o
     * {@code MEDIDA_PREVENTIVA}. Parámetro {@code tipo}:
     * {@code SECUESTRO_RODADO}, {@code RETENCION_DOCUMENTAL},
     * {@code MEDIDA_PREVENTIVA_APLICABLE}. Requiere
     * {@code ACTAS_EN_ENRIQUECIMIENTO} y bloque {@code D1_CAPTURA} o
     * {@code D2_ENRIQUECIMIENTO}, y al menos un evento de trazabilidad previo
     * en el expediente (p. ej. {@code ALTA} en D1).
     */
    @PostMapping("/actas/{id}/acciones/registrar-constatacion-material-temprana")
    public RegistrarConstatacionMaterialTempranaAccionResponse registrarConstatacionMaterialTemprana(
            @PathVariable("id") String id, @RequestParam("tipo") String tipo) {
        PrototipoStore.TipoConstatacionMaterialTemprana t;
        try {
            t = PrototipoStore.TipoConstatacionMaterialTemprana.valueOf(tipo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo inválido: " + tipo);
        }
        PrototipoStore.RegistrarConstatacionMaterialTempranaResultado r =
                store.registrarConstatacionMaterialTemprana(id, t);
        if (r.estado() == PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.CONFLICT) {
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.TIPO_YA_EN_EXPEDIENTE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe una constatación material temprana activa de este tipo para el acta (ancla"
                                + " documental ya en expediente).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.FUERA_ETAPA_LABRADO_ENRIQUECIMIENTO) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La constatación material temprana solo puede registrarse en etapa de labrado o"
                                + " enriquecimiento (D1/D2, bandeja de actas en enriquecimiento).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.SIN_TRAZA_PREVIA) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Se requiere al menos un evento de trazabilidad en el expediente para registrar"
                                + " constatación material temprana.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarConstatacionMaterialTempranaAccionResponse(
                "OK",
                "Constatación de material en expediente (D1/D2; mock; misma ancla que el circuito de cierre).",
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Incorpora ancla de medida preventiva nacida durante el trámite
     * administrativo (p. ej. inspección posterior a labrado en
     * contravención), en {@code PENDIENTE_ANALISIS}. No es constatación
     * temprana D1/D2. Reutiliza el mismo criterio documental
     * ({@code MEDIDA_PREVENTIVA}) que cierre y reconocimiento, sin datos de
     * tránsito.
     */
    @PostMapping("/actas/{id}/acciones/registrar-medida-preventiva-posterior")
    public RegistrarMedidaPreventivaPosteriorAccionResponse registrarMedidaPreventivaPosterior(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado r = store.registrarMedidaPreventivaPosterior(id);
        if (r.estado() == PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.CONFLICT) {
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .MEDIDA_YA_EN_EXPEDIENTE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El expediente ya contiene ancla de medida preventiva (MEDIDA_PREVENTIVA).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .FUERA_PENDIENTE_ANALISIS) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Registrar medida preventiva posterior solo aplica con acta en PENDIENTE_ANALISIS"
                                + " (mock).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .ACTA_EN_ARCHIVO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No aplica a acta en bandeja ARCHIVO (mock).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior.ACTA_CERRADA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No aplica a acta cerrada (mock).");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarMedidaPreventivaPosteriorAccionResponse(
                "OK",
                "Medida preventiva registrada desde trámite posterior al labrado (ancla al circuito de cierre;"
                        + " mock).",
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Reconoce el hecho material que origina un bloqueante de cierre, anclado
     * a documentación del expediente. Parámetro {@code tipo}:
     * {@code MEDIDA_ACTIVA} requiere documento de tipo
     * {@code MEDIDA_PREVENTIVA}; {@code SECUESTRO_RODADO} requiere
     * {@code ACTA_RETENCION}; {@code RETENCION_DOCUMENTAL} requiere
     * {@code CONSTATACION_RETENCION_DOCUMENTACION}. Idempotente si el origen
     * ya constaba.
     */
    @PostMapping("/actas/{id}/acciones/reconocer-origen-bloqueo-cierre-material")
    public ReconocerOrigenBloqueanteMaterialAccionResponse reconocerOrigenBloqueoCierreMaterial(
            @PathVariable("id") String id, @RequestParam("tipo") String tipo) {
        TipoReconocimientoOrigenBloqueo t = TipoReconocimientoOrigenBloqueo.parse(tipo);
        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado r;
        if (t == TipoReconocimientoOrigenBloqueo.MEDIDA_ACTIVA) {
            r = store.reconocerOrigenBloqueanteMedidaPreventiva(id);
        } else if (t == TipoReconocimientoOrigenBloqueo.SECUESTRO_RODADO) {
            r = store.reconocerOrigenBloqueanteSecuestroRodado(id);
        } else {
            r = store.reconocerOrigenBloqueanteRetencionDocumental(id);
        }
        if (r.estado() == PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        CerrabilidadResponse c = ActaDetalleMapper.mapCerrabilidad(r.cerrabilidad());
        return new ReconocerOrigenBloqueanteMaterialAccionResponse(
                "OK",
                "Origen material de bloqueo de cierre reconocido (mock).",
                r.actaId(),
                r.origenBloqueante().name(),
                c);
    }

    /**
     * Registra en el expediente el documento mock resolutorio (no sustituye el
     * cumplimiento material; usar
     * {@link #registrarCumplimientoMaterialBloqueoCierre(String, String)}).
     * Parámetro {@code tipo}: bloque de cierre material
     * ({@code LEVANTAMIENTO_MEDIDA_PREVENTIVA}, {@code LIBERACION_RODADO},
     * {@code ENTREGA_DOCUMENTACION}). Opcional {@code documentoConCircuitoFirmaNotif}
     * (por defecto {@code false}): si es {@code true} y el tipo es
     * levantamiento de medida, el documento se emite con circuito
     * firma+notif mock ({@code PENDIENTE_FIRMA} / tipo
     * {@code DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF}) sin sumar otro
     * bloqueante material. Requiere origen material; la acta no debe estar
     * cerrada ni en {@code GESTION_EXTERNA}, {@code ARCHIVO} ni
     * {@code CERRADAS} (tampoco limitado a {@code PENDIENTE_ANALISIS}).
     */
    @PostMapping("/actas/{id}/acciones/registrar-resolucion-bloqueo-cierre")
    public RegistrarResolucionBloqueoCierreAccionResponse registrarResolucionBloqueoCierreDocumental(
            @PathVariable("id") String id,
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "documentoConCircuitoFirmaNotif", defaultValue = "false")
                    boolean documentoConCircuitoFirmaNotif) {
        PrototipoStore.PendienteBloqueanteCierreMock t = parsePendienteBloqueanteRequired(tipo);
        PrototipoStore.RegistrarResolucionBloqueoCierreResultado r =
                store.registrarResolucionBloqueoCierreDocumental(id, t, documentoConCircuitoFirmaNotif);
        if (r.estado() == PrototipoStore.RegistrarResolucionBloqueoCierreEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarResolucionBloqueoCierreEstado.CONFLICT) {
            String msg = r.motivoNoCerrable();
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    msg != null && !msg.isBlank() ? msg : "Conflicto al registrar resolución documental.");
        }
        CerrabilidadResponse c = new CerrabilidadResponse(
                r.resultadoFinal().name(),
                r.cerrable(),
                r.pendientesBloqueantesCierreRestantes(),
                r.motivoNoCerrable());
        return new RegistrarResolucionBloqueoCierreAccionResponse(
                "OK",
                "Documento resolutorio mock incorporado; el bloqueo persiste hasta registrar cumplimiento material efectivo.",
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.pendienteAsociado(),
                c);
    }

    /**
     * Registro mock de cumplimiento material (medida levantada, rodado
     * liberado, documentación entregada). Requiere documento resolutorio y origen
     * coherentes. Parámetro {@code tipo} igual a
     * {@link #registrarResolucionBloqueoCierreDocumental(String, String, boolean)}.
     */
    @PostMapping("/actas/{id}/acciones/registrar-cumplimiento-material-bloqueo-cierre")
    public RegistrarCumplimientoMaterialBloqueoCierreAccionResponse registrarCumplimientoMaterialBloqueoCierre(
            @PathVariable("id") String id,
            @RequestParam("tipo") String tipo) {
        return ejecutarRegistrarCumplimientoMaterialBloqueoCierre(id, tipo);
    }

    private RegistrarCumplimientoMaterialBloqueoCierreAccionResponse ejecutarRegistrarCumplimientoMaterialBloqueoCierre(
            String id, String tipo) {
        PrototipoStore.PendienteBloqueanteCierreMock t = parsePendienteBloqueanteRequired(tipo);
        PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado r =
                store.registrarCumplimientoMaterialBloqueoCierre(id, t);
        if (r.estado() == PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.CONFLICT) {
            String msg = r.motivoNoCerrable();
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    msg != null && !msg.isBlank() ? msg : "Conflicto al registrar cumplimiento material.");
        }
        CerrabilidadResponse c = new CerrabilidadResponse(
                r.resultadoFinal().name(),
                r.cerrable(),
                r.pendientesBloqueantesCierreRestantes(),
                r.motivoNoCerrable());
        return new RegistrarCumplimientoMaterialBloqueoCierreAccionResponse(
                "OK",
                "Cumplimiento material efectivo registrado (mock).",
                r.actaId(),
                r.pendienteCumplido(),
                c);
    }

    private PrototipoStore.PendienteBloqueanteCierreMock parsePendienteBloqueanteRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo de pendiente requerido");
        }
        try {
            return PrototipoStore.PendienteBloqueanteCierreMock.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo de pendiente inválido: " + raw);
        }
    }

    private enum TipoReconocimientoOrigenBloqueo {
        /**
         * Ancla: documento de tipo {@code MEDIDA_PREVENTIVA} en el expediente
         * (origen {@code MEDIDA_PREVENTIVA_ACTIVA}).
         */
        MEDIDA_ACTIVA,
        SECUESTRO_RODADO,
        RETENCION_DOCUMENTAL;

        static TipoReconocimientoOrigenBloqueo parse(String raw) {
            if (raw == null || raw.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo requerido");
            }
            try {
                return TipoReconocimientoOrigenBloqueo.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo inválido: " + raw);
            }
        }
    }
}
