# 09 — Paridad y Gaps Restantes

Fuentes consultadas:
- `backend/api-faltas-core/docs/spec-as-source/110-matriz-maestra-paridad-mariadb-inmemory.md`
- `backend/api-faltas-core/docs/spec-as-source/109-delta-modelo-mariadb-inmemory.md`
- `backend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md`
- Código Java verificado en el working tree

---

## Resumen de paridad post-8F-11D

| Categoría | Cantidad |
|---|---|
| Tablas MariaDB auditadas | **62** |
| Entidades InMemory persistibles | ~37 (31 base + 6 nuevas de 8F-11D) |
| ALINEADO | 26 base + 6 nuevas = **32** |
| FALTA_EN_INMEMORY | **21** (reducido de 27 en 8F-11C) |
| FALTA_EN_MARIADB | 0 |
| SEMANTICA_INCOMPATIBLE | 3 (decisiones cerradas) |
| RELACION_INCOMPLETA | 3 |

---

## Tablas cerradas por 8F-11D (GAP-7)

| Tabla | Estado anterior | Estado post-8F-11D |
|---|---|---|
| `fal_tarifario_unidad_faltas` | FALTA_EN_INMEMORY | **ALINEADO** |
| `fal_medida_preventiva` | FALTA_EN_INMEMORY | **ALINEADO** |
| `fal_articulo_medida_preventiva` | FALTA_EN_INMEMORY | **ALINEADO** |
| `fal_acta_articulo_infringido` | FALTA_EN_INMEMORY | **ALINEADO** |
| `fal_acta_valorizacion` | FALTA_EN_INMEMORY | **ALINEADO** |
| `fal_acta_valorizacion_item` | FALTA_EN_INMEMORY | **ALINEADO** |

---

## Lista exacta de tablas FALTA_EN_INMEMORY restantes (21)

### Satélites de acta — Slice 8F-11E
1. `fal_acta_transito`
2. `fal_acta_transito_alcoholemia`
3. `fal_acta_vehiculo`
4. `fal_acta_contravencion`
5. `fal_acta_sustancias_alimenticias`
6. `fal_acta_medida_preventiva`

### Paralizacion y Archivo — Slice 8F-11G
7. `fal_acta_paralizacion`
8. `fal_acta_archivo`

### Observaciones — Slice 8F-11G
9. `fal_observacion`

### Pagos reales — Slice 8F-11H
10. `fal_acta_obligacion_pago` (FalPagoVoluntario y FalPagoCondena son semántica incompatible/D2)
11. `fal_acta_forma_pago`
12. `fal_acta_plan_pago_ref`
13. `fal_acta_pago_movimiento`

### Notificaciones ciclo completo — Slice 8F-11I
14. `fal_notificacion_intento`
15. `fal_notificacion_acuse`
16. `fal_lote_correo`

### Documentos y relaciones — Slice 8F-11J
17. `fal_acta_documento`

### Apelación complementaria — Slice 8F-11F
18. `fal_acta_apelacion_documento`

### QR portal — Slice 8F-11K
19. `fal_acta_qr_acceso`

### Pendientes parciales (campos en entidades existentes)
20-21. Campos en `fal_acta_fallo` y `fal_acta_apelacion` — Slice 8F-11F (GAP-9, GAP-10)

---

## Gaps parciales/incompatibles restantes

| GAP | Descripción | Slice asignado |
|---|---|---|
| GAP-3 (D2) | FalPagoVoluntario/FalPagoCondena semánticamente incompatibles con fal_acta_obligacion_pago | 8F-11H |
| GAP-4 (D3) | FalActaParalizacion faltante | 8F-11G |
| GAP-5 | FalActaArchivo faltante | 8F-11G |
| GAP-9 | FalActaFallo: faltan valorizacionId, resultadoFallo, fhFirma, fhVtoApelacion, falloReemplazadoId, siFirme, fhFirmeza, origenFirmeza, versionRow, auditoría | 8F-11F |
| GAP-10 | FalActaApelacion: faltan canalApelacion, tipoPresentacion, textoApelacion, documentoResolucionId, versionRow; falloId debe ser Long | 8F-11F |
| GAP-11 | fal_observacion no implementado (PAGAPR usa evento transitoriamente) | 8F-11G |
| GAP-12 | fal_acta_documento (pivot) faltante | 8F-11J |
| GAP-17 | Ciclo notificaciones completo | 8F-11I |
| GAP-18 | QR portal | 8F-11K |
| GAP-16 | Satélites de acta (6) | 8F-11E |

---

## Contradicción documental detectada

En el documento 109, sección final "8F-11D", el conteo aparece como:
> "Conteo: 27 → 21 FALTA_EN_INMEMORY"

El documento 110 tiene como baseline post-8F-11C: **29 FALTA_EN_INMEMORY** (cifra corregida en 8F-11A-R1).

La diferencia (29 vs 27) se explica porque:
- 8F-11B cerró algunas identidades incompatibles (FalActaFallo.id, etc.) que ya no figuran como FALTA_EN_INMEMORY.
- 8F-11C cerró FalPersona y FalPersonaDomicilio (2 tablas).
- 29 - 2 (persona) - otros ya cerrados en 8F-11B = 27 antes de 8F-11D.
- 27 - 6 (8F-11D) = **21 FALTA_EN_INMEMORY al cierre de 8F-11D**.

El número correcto post-8F-11D es **21 FALTA_EN_INMEMORY** (verificado contra la lista detallada arriba).

---

## Roadmap 8F-11E a 8F-11K

| Slice | Objetivo | Tablas/Entidades |
|---|---|---|
| **8F-11E** | Satélites de acta y catálogos | FalActaTransito, Alcoholemia, Vehiculo, Contravencion, SustanciasAlimenticias, FalActaMedidaPreventiva; Marcas, Modelos, Rubros |
| **8F-11F** | Fallo, firmeza y apelación completos | FalActaFallo (completar), FalActaApelacion (completar + Long), FalActaApelacionDocumento; eliminar FalActaFirmezaCondena |
| **8F-11G** | Paralizacion, archivo, motivos, observaciones | FalActaParalizacion, FalActaArchivo, FalMotivoArchivo, FalObservacion |
| **8F-11H** | Pagos reales unificados (D2) | FalActaObligacionPago, FalActaFormaPago, FalActaPlanPagoRef, FalActaPagoMovimiento |
| **8F-11I** | Notificaciones completas | FalNotificacionIntento, FalNotificacionAcuse, FalLoteCorreo |
| **8F-11J** | Documentos y relaciones | FalActaDocumento pivot; FalDocumento campos faltantes |
| **8F-11K** | Portal QR + auditoría final paridad cero | FalActaQrAcceso; verificar 62 tablas a 0 gaps |
