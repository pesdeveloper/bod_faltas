package ar.gob.malvinas.faltas.core.web.dto;

import java.util.List;

/**
 * Respuesta completa del endpoint GET /demo/actas/{codigo}.
 *
 * Contiene:
 * - datos de presentacion del caso (codigo, titulo, descripcion, casoUsoPrincipal)
 * - bloque "dataset": definicion declarativa del caso
 * - bloque "acta": instancia real materializada por CasoUsoFuncionalRunner
 * - timeline: eventos append-only reales del acta
 * - documentos: documentos reales del acta (con guardrails mock)
 * - demo: metadata de la respuesta demo
 * - links: navegacion entre endpoints demo
 *
 * Uso exclusivo en perfil de desarrollo y demo funcional.
 * No expone objetos de dominio directamente.
 *
 * Slice 8F-7.
 */
public record DemoActaDetalleResponse(
        String codigo,
        String titulo,
        String descripcion,
        String casoUsoPrincipal,
        DemoActaDetalleDatasetDto dataset,
        DemoActaDetalleActaDto acta,
        List<DemoTimelineEventoDto> timeline,
        List<DemoDocumentoDetalleDto> documentos,
        DemoActaDetalleMetaDto demo,
        DemoActaDetalleLinksDto links
) {}
