# Orden de dependencias — DAG topológico BOD Faltas

> **Trabajo:** DDL-MARIADB-MANUAL-001-FULL-R1
> **Fuente normativa:** `50-persistence/mariadb-logical-model.md`
>
> Este documento establece el orden real de creación de las 64 tablas nuevas,
> derivado del grafo de dependencias (DAG). Los ciclos de FK se resuelven
> mediante `ALTER TABLE` post-creación.

## Notación

- `A → B`: la tabla A tiene FK hacia B (A depende de B; B debe existir primero).
- `(self)`: FK autorreferencial (agregar vía `ALTER TABLE` después de crear la tabla).
- `(ciclica)`: FK que forma ciclo con otra tabla (resolver vía `ALTER TABLE`).
- `[baseline]`: referencia a un objeto del baseline protegido.

---

## Grupo G1 — Catálogos y raíces sin dependencias externas

**Dependencias previas:** ninguna (cero FK a tablas TO_CREATE).
**Dependencia al baseline:** `fal_rubro_version` adoptada pero no creada.

| Orden dentro del grupo | Tabla | Razón del orden |
|---|---|---|
| G1-01 | `fal_dependencia` | Sin FK externas; self-ref agrega post-create |
| G1-02 | `fal_inspector` | Sin FK externas |
| G1-03 | `fal_firmante` | Sin FK externas |
| G1-04 | `fal_persona` | Sin FK externas |
| G1-05 | `fal_vehiculo_marca` | Sin FK externas |
| G1-06 | `num_politica` | Sin FK externas |
| G1-07 | `fal_dia_no_computable` | Sin FK externas |
| G1-08 | `fal_motivo_archivo` | Sin FK externas |
| G1-09 | `fal_normativa_faltas` | Sin FK externas |
| G1-10 | `fal_medida_preventiva` | Sin FK externas |
| G1-11 | `fal_lote_correo` | Sin FK externas |
| G1-12 | `fal_documento_plantilla` | Sin FK externas |

**FK autorreferencial a agregar post-G1:**
```sql
-- Dependencia jerárquica (nullable, no bloquea la creación)
ALTER TABLE fal_dependencia
    ADD CONSTRAINT fk_dep_padre
    FOREIGN KEY (id_dep_padre) REFERENCES fal_dependencia(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;
```

**Riesgo circular G1:** ninguno entre tablas del grupo.

---

## Grupo G2 — Versiones y catálogos secundarios

**Dependencias previas:** solo G1.

| Orden | Tabla | FK salientes | Razón |
|---|---|---|---|
| G2-01 | `fal_dependencia_version` | `→ fal_dependencia` | Versión de G1-01 |
| G2-02 | `fal_inspector_version` | `→ fal_inspector` | Versión de G1-02 |
| G2-03 | `fal_firmante_version` | `→ fal_firmante`, `→ fal_dependencia` (nullable) | Versión de G1-03 |
| G2-04 | `fal_vehiculo_modelo` | `→ fal_vehiculo_marca` | Catálogo secundario de G1-05 |
| G2-05 | `num_talonario` | `→ num_politica` | Instancia de G1-06 |
| G2-06 | `fal_dependencia_normativa` | `→ fal_dependencia`, `→ fal_normativa_faltas` | Pivote G1-01 × G1-09 |
| G2-07 | `fal_articulo_normativa_faltas` | `→ fal_normativa_faltas` | Detalle de G1-09 |
| G2-08 | `fal_documento_plantilla_firma_req` | `→ fal_documento_plantilla` | Detalle de G1-12 |
| G2-09 | `fal_documento_plantilla_contenido` | `→ fal_documento_plantilla` | Detalle de G1-12 |
| G2-10 | `fal_documento_plantilla_default` | `→ fal_documento_plantilla`, `→ fal_dependencia` (nullable) | Detalle de G1-12 |

---

## Grupo G3 — Habilitaciones y talonarios asignados

**Dependencias previas:** G1 + G2.

| Orden | Tabla | FK salientes | Razón |
|---|---|---|---|
| G3-01 | `fal_firmante_version_habilitacion` | `→ fal_firmante_version` | Detalle de G2-03 |
| G3-02 | `fal_tarifario_unidad_faltas` | `→ fal_articulo_normativa_faltas` | Detalle de G2-07 |
| G3-03 | `fal_articulo_medida_preventiva` | `→ fal_medida_preventiva`, `→ fal_articulo_normativa_faltas` | Pivote G1-10 × G2-07 |
| G3-04 | `num_talonario_ambito` | `→ num_talonario`, `→ fal_dependencia` (nullable) | Alcance de G2-05 |
| G3-05 | `num_talonario_inspector` | `→ num_talonario` | Asignación G2-05 |

---

## Grupo G4 — Personas/domicilios y núcleo del acta

**Dependencias previas:** G1 + G2 + G3.
**Ciclo identificado:** `fal_acta ↔ fal_persona_domicilio`.

### Resolución del ciclo G4

```
fal_persona_domicilio.persona_id → fal_persona   (G1)
fal_persona_domicilio.acta_origen_id → fal_acta  ← CICLO

fal_acta.id_persona_infractor → fal_persona      (G1)
fal_acta.id_domicilio_infractor_act → fal_persona_domicilio ← CICLO
fal_acta.id_domicilio_notif_act → fal_persona_domicilio     ← CICLO
```

**Estrategia:**
1. Crear `fal_persona_domicilio` sin FK a `fal_acta` (acta_origen_id nullable).
2. Crear `fal_acta` con FKs a `fal_persona_domicilio` (domicilios nullable).
3. `ALTER TABLE fal_persona_domicilio ADD FK acta_origen_id → fal_acta`.
4. `ALTER TABLE fal_dependencia ADD FK id_dep_padre → fal_dependencia` (self-ref G1).

| Orden | Tabla | FK salientes (en CREATE) | FK diferidas (ALTER) |
|---|---|---|---|
| G4-01 | `fal_persona_domicilio` | `→ fal_persona`, `→ geo_malv_localidad_version` [baseline], `→ geo_malv_calle_version` [baseline] | `acta_origen_id → fal_acta` |
| G4-02 | `fal_acta` | `→ fal_persona`, `→ fal_persona_domicilio` (x2, nullable), `→ fal_motivo_archivo` (nullable), `→ num_talonario` (nullable), `→ geo_malv_localidad_version` [baseline], `→ geo_malv_calle_version` [baseline] | — |

---

## Grupo G5 — Satélites del acta (depende de G4)

**Dependencias previas:** G1..G4.

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G5-01 | `fal_acta_evidencia` | `→ fal_acta` |
| G5-02 | `fal_observacion` | `→ fal_acta` |
| G5-03 | `fal_acta_transito` | `→ fal_acta` |
| G5-04 | `fal_acta_transito_alcoholemia` | `→ fal_acta` |
| G5-05 | `fal_acta_vehiculo` | `→ fal_acta`, `→ fal_vehiculo_marca`, `→ fal_vehiculo_modelo` |
| G5-06 | `fal_acta_contravencion` | `→ fal_acta`, `→ fal_rubro_version` [baseline] |
| G5-07 | `fal_acta_sustancias_alimenticias` | `→ fal_acta`, `→ fal_rubro_version` [baseline] |
| G5-08 | `fal_acta_paralizacion` | `→ fal_acta` |
| G5-09 | `fal_acta_archivo` | `→ fal_acta` |
| G5-10 | `fal_acta_articulo_infringido` | `→ fal_acta`, `→ fal_articulo_normativa_faltas` |
| G5-11 | `fal_acta_qr_acceso` | `→ fal_acta` |
| G5-12 | `fal_acta_gestion_externa` | `→ fal_acta` |
| G5-13 | `fal_acta_valorizacion` | `→ fal_acta` |
| G5-14 | `fal_acta_bloqueante_cierre_material` | `→ fal_acta` |

---

## Grupo G6 — Medidas preventivas y documentos base (depende de G5)

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G6-01 | `fal_acta_medida_preventiva` | `→ fal_acta`, `→ fal_acta_articulo_infringido`, `→ fal_medida_preventiva` |
| G6-02 | `fal_acta_valorizacion_item` | `→ fal_acta_valorizacion` |
| G6-03 | `fal_documento` | `→ fal_acta`, `→ fal_documento_plantilla` |

---

## Grupo G7 — Documentos relacionados y notificaciones (depende de G6)

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G7-01 | `fal_acta_documento` | `→ fal_acta`, `→ fal_documento` |
| G7-02 | `fal_documento_firma_req` | `→ fal_documento` |
| G7-03 | `fal_documento_redaccion` | `→ fal_documento`, `→ fal_documento_plantilla_contenido`, self |
| G7-04 | `fal_notificacion` | `→ fal_acta`, `→ fal_documento` |
| G7-05 | `num_talonario_movimiento` | `→ num_talonario`, `→ fal_acta` (nullable), `→ fal_documento` (nullable) |
| G7-06 | `fal_acta_fallo` | `→ fal_acta`, `→ fal_acta_valorizacion` (nullable), `→ fal_documento` (nullable), self |

> **Nota:** `num_talonario_movimiento` va después de `fal_documento` porque tiene FK nullable a `fal_documento`.

---

## Grupo G8 — Firma, eventos, apelaciones y pagos base (depende de G7)

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G8-01 | `fal_documento_firma` | `→ fal_documento`, `→ fal_firmante_version` |
| G8-02 | `fal_notificacion_intento` | `→ fal_notificacion` |
| G8-03 | `fal_notificacion_acuse` | `→ fal_notificacion` |
| G8-04 | `fal_acta_evento` | `→ fal_acta`, `→ fal_documento` (nullable), `→ fal_notificacion` (nullable) |
| G8-05 | `fal_acta_apelacion` | `→ fal_acta`, `→ fal_acta_fallo`, `→ fal_documento` (nullable), self |
| G8-06 | `fal_acta_obligacion_pago` | `→ fal_acta`, `→ fal_persona`, `→ fal_acta_valorizacion` (nullable), `→ fal_acta_fallo` (nullable), self; FK `forma_pago_vigente_id` diferida |

### Ciclo G8 — `fal_acta_obligacion_pago ↔ fal_acta_forma_pago`

```
fal_acta_obligacion_pago.forma_pago_vigente_id → fal_acta_forma_pago  ← CICLO
fal_acta_forma_pago.obligacion_pago_id → fal_acta_obligacion_pago     ← CICLO
```

**Estrategia:** crear `fal_acta_obligacion_pago` sin la FK `forma_pago_vigente_id`,
crear `fal_acta_forma_pago` con FK a `fal_acta_obligacion_pago`, luego
`ALTER TABLE fal_acta_obligacion_pago ADD FK forma_pago_vigente_id`.

---

## Grupo G9 — Snapshot, apelación documentos y formas de pago (depende de G8)

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G9-01 | `fal_acta_snapshot` | `→ fal_acta`, `→ fal_documento` (nullable), `→ fal_acta_valorizacion` (nullable) |
| G9-02 | `fal_acta_apelacion_documento` | `→ fal_acta_apelacion`, `→ fal_documento` |
| G9-03 | `fal_acta_forma_pago` | `→ fal_acta_obligacion_pago`, self |

**FK diferida a agregar post-G9:**
```sql
ALTER TABLE fal_acta_obligacion_pago
    ADD CONSTRAINT fk_oblig_forma_pago_vigente
    FOREIGN KEY (forma_pago_vigente_id) REFERENCES fal_acta_forma_pago(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;
```

---

## Grupo G10 — Plan de pagos (depende de G9)

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G10-01 | `fal_acta_plan_pago_ref` | `→ fal_acta_forma_pago`, `→ fal_acta_obligacion_pago`, self |

---

## Grupo G11 — Movimientos de pago (depende de G10)

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G11-01 | `fal_acta_pago_movimiento` | `→ fal_acta_obligacion_pago`, `→ fal_acta_forma_pago` (nullable), `→ fal_acta_plan_pago_ref` (nullable), self |

---

## Grupo G12 — Proyección económica (depende de G11)

| Orden | Tabla | FK salientes clave |
|---|---|---|
| G12-01 | `fal_acta_economia_proyeccion` | `→ fal_acta`, `→ fal_acta_obligacion_pago` (nullable), `→ fal_acta_forma_pago` (nullable), `→ fal_acta_plan_pago_ref` (nullable), `→ fal_acta_pago_movimiento` (nullable) |

---

## Resumen de ciclos y resolución

| Ciclo | Tablas involucradas | Resolución |
|---|---|---|
| G1-self | `fal_dependencia.id_dep_padre → fal_dependencia` | `ALTER TABLE` post-G1 |
| G4-cross | `fal_acta ↔ fal_persona_domicilio` | Crear sin FK; `ALTER TABLE` post-G4 |
| G8-cross | `fal_acta_obligacion_pago ↔ fal_acta_forma_pago` | Crear sin FK `forma_pago_vigente_id`; `ALTER TABLE` post-G9 |

No se resuelven ciclos eliminando FK ni degradando integridad referencial.

---

## Grupos de creación en FULL-R1

El trabajo **FULL-R1** del slice DDL-MARIADB-MANUAL-001-FULL-R1 materializa
las 64 tablas en un único script siguiendo el orden topológico descrito arriba.

No existe "R2+": todas las tablas se crean en este script.
Las decisiones sobre `fal_persona`, `fal_inspector` y `fal_dependencia` han
sido corregidas y reconciliadas (ver `ddl-full-scope.md`).

