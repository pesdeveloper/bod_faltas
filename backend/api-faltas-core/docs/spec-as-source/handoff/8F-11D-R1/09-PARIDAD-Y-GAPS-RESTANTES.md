# 09 - Paridad y Gaps Restantes - Post-8F-11D-R1

## Estado al cierre de 8F-11D-R1

**Fecha:** 2026-07-06
**Build:** 1785 tests, 0 failures, 0 errors, BUILD SUCCESS
**Tablas MariaDB auditadas:** 62
**Fuente vigente de paridad:** `110-matriz-maestra-paridad-mariadb-inmemory.md`

---

## Distribucion de 62 tablas (categorias excluyentes)

| Categoria | Tablas | Suma |
|-----------|-------:|-----:|
| ALINEADO | 32 | 32 |
| FALTA_EN_INMEMORY | 21 | 53 |
| PARCIAL | 4 | 57 |
| SEMANTICA_INCOMPATIBLE | 1 | 58 |
| RELACION_INCOMPLETA | 1 | 59 |
| NO_PERSISTIBLE (stor_*) | 3 | 62 |
| **Total** | **62** | |

---

## Las 21 tablas FALTA_EN_INMEMORY (canonicas post-8F-11D-R1)

### Observaciones (1)
1. `fal_observacion` - Slice 8F-11G

### Satelites del acta (6)
2. `fal_acta_transito` - Slice 8F-11E
3. `fal_acta_transito_alcoholemia` - Slice 8F-11E
4. `fal_acta_vehiculo` - Slice 8F-11E
5. `fal_acta_contravencion` - Slice 8F-11E
6. `fal_acta_sustancias_alimenticias` - Slice 8F-11E
7. `fal_acta_medida_preventiva` - Slice 8F-11E

### Notificaciones ciclo completo (3)
8. `fal_notificacion_intento` - Slice 8F-11I
9. `fal_notificacion_acuse` - Slice 8F-11I
10. `fal_lote_correo` - Slice 8F-11I

### Apelacion, paralizacion y archivo (3)
11. `fal_acta_apelacion_documento` - Slice 8F-11F
12. `fal_acta_paralizacion` - Slice 8F-11G
13. `fal_acta_archivo` - Slice 8F-11G

### Pagos (3)
14. `fal_acta_forma_pago` - Slice 8F-11H
15. `fal_acta_plan_pago_ref` - Slice 8F-11H
16. `fal_acta_pago_movimiento` - Slice 8F-11H

### Portal (1)
17. `fal_acta_qr_acceso` - Slice 8F-11K

### Catalogos del dominio (4)
18. `fal_vehiculo_marca` - Slice 8F-11E
19. `fal_vehiculo_modelo` - Slice 8F-11E
20. `fal_rubro_version` - Slice 8F-11E
21. `fal_motivo_archivo` - Slice 8F-11G

---

## Las 4 tablas PARCIAL

| Tabla | Estado | Gaps principales | Slice |
|-------|--------|-----------------|-------|
| `fal_acta` | PARCIAL | tipo_acta ALINEADO; infractorXxx transitorio (D6); sin auditoria completa | multiple |
| `fal_acta_evento` | PARCIAL | id String; payload JSON no implementado | 8F-11F |
| `fal_acta_fallo` | PARCIAL | valorizacionId, resultadoFallo, siFirme, fhFirmeza, origenFirmeza, fhFirma, fhVtoApelacion, falloReemplazadoId, versionRow, auditoria | 8F-11F |
| `fal_acta_apelacion` | PARCIAL | canalApelacion, tipoPresentacion, textoApelacion, documentoResolucionId, versionRow; falloId->Long; auditoria | 8F-11F |

---

## La 1 tabla SEMANTICA_INCOMPATIBLE

| Tabla MariaDB | Entidades Java | Decision | Slice resolucion |
|---|---|---|---|
| `fal_acta_obligacion_pago` | FalPagoVoluntario (tipo=1) + FalPagoCondena (tipo=2) | D2 cerrada: unificar en FalActaObligacionPago | 8F-11H |

> **Nota:** FalActaFirmezaCondena es SEMANTICA_INCOMPATIBLE tambien, pero no tiene tabla propia - sus campos van en `fal_acta_fallo` (Decision D1/P2 cerradas). No se cuenta como tabla.

---

## La 1 tabla RELACION_INCOMPLETA

| Tabla | Estado | Descripcion | Slice |
|-------|--------|-------------|-------|
| `fal_acta_documento` | RELACION_INCOMPLETA | FalDocumento.idActa cubre caso simple; pivot completo pendiente | 8F-11J |

> **Nota:** `fal_acta_apelacion_documento` esta en FALTA_EN_INMEMORY (#11), no en RELACION_INCOMPLETA.

---

## Las 3 tablas NO_PERSISTIBLE

| Tabla | Razon |
|-------|-------|
| `stor_backend` | Infraestructura de storage, no es dominio Faltas |
| `stor_politica` | Infraestructura de storage, no es dominio Faltas |
| `stor_objeto` | Referencia de binarios via storage_key en entidades; no es dominio Faltas |

---

## Tablas resueltas en 8F-11D+R1 (ya ALINEADO)

| Tabla | Entidad Java | Slice |
|-------|-------------|-------|
| `fal_tarifario_unidad_faltas` | FalTarifarioUnidadFaltas | 8F-11D+R1-D |
| `fal_medida_preventiva` | FalMedidaPreventiva | 8F-11D+R1-D |
| `fal_articulo_medida_preventiva` | FalArticuloMedidaPreventiva | 8F-11D+R1-D |
| `fal_acta_articulo_infringido` | FalActaArticuloInfringido | 8F-11D+R1-D |
| `fal_acta_valorizacion` | FalActaValorizacion | 8F-11D+R1-B |
| `fal_acta_valorizacion_item` | FalActaValorizacionItem | 8F-11D+R1-C |

---

## Frase de cierre

> 8F-11D-R1 CERRADO - 1785 tests - 32 ALINEADO - 21 FALTA_EN_INMEMORY - 4 PARCIAL - 1 SEMANTICA_INCOMPATIBLE - 1 RELACION_INCOMPLETA - 3 NO_PERSISTIBLE stor_* - Total 62 tablas explicadas.