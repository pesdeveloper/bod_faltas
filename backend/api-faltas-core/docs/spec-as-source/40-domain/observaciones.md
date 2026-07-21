# Observaciones — Spec transversal

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING

> **Slice:** SPEC-MODEL-DDL-CLOSURE-001 / DDL-MARIADB-MANUAL-001-FULL-R1.2
> No contiene "pendiente", "por definir" ni tokens de trabajo.

---

## 1. Regla principal

Toda nota libre, humana, acumulable y auditable se persiste en `fal_observacion`.

Las entidades observables **no** contienen un único campo `observacion` que se
sobrescribe. Puede haber N observaciones por cada registro observable.

No confundir con campos estructurales que permanecen en su entidad:
- fundamentos
- resumen del hecho
- motivo tipificado
- resultado técnico
- contenido jurídico
- detalle de acuse
- contenido documental

---

## 2. Tabla `fal_observacion` — definición física

### 2.1 Propósito

Registro polimórfico append-only de notas libres vinculadas a cualquier entidad
observable del dominio. La corrección de una observación se realiza mediante
baja lógica de la anterior e inserción de una nueva. **No se edita**.

### 2.2 Columnas

| Columna              | Tipo                    | Nulable | Default | Descripción |
|----------------------|-------------------------|---------|---------|-------------|
| `id`                 | `BIGINT AUTO_INCREMENT` | NO      | —       | PK técnica |
| `entidad_tipo`       | `SMALLINT`              | NO      | —       | Código de `EntidadTipoObservada`; identifica qué tipo de objeto se observa |
| `entidad_id`         | `BIGINT`                | NO      | —       | PK del objeto observado (polimórfico) |
| `id_acta_contexto`   | `BIGINT`                | SÍ      | NULL    | FK a `fal_acta(id)`. Contexto del expediente cuando la entidad es un satélite del acta. NULL para entidades maestro |
| `origen_observacion` | `SMALLINT`              | NO      | —       | Código de `OrigenObservacion`: 1=USUARIO, 2=SISTEMA, 3=INTEGRACION |
| `observacion`        | `VARCHAR(512)`          | NO      | —       | Texto de la nota. CHAR_LENGTH(TRIM(observacion)) BETWEEN 1 AND 512 |
| `si_activa`          | `BOOLEAN`               | NO      | `TRUE`  | TRUE = vigente; FALSE = dada de baja lógicamente |
| `fh_alta`            | `DATETIME(6)`           | NO      | —       | Instante de creación |
| `id_user_alta`       | `CHAR(36)`              | NO      | —       | UUID del actor que creó la observación |
| `fh_baja`            | `DATETIME(6)`           | SÍ      | NULL    | Instante de baja lógica. NULL si si_activa=TRUE |
| `id_user_baja`       | `CHAR(36)`              | SÍ      | NULL    | Actor que realizó la baja. NULL si si_activa=TRUE |

### 2.3 Constraints

```sql
PRIMARY KEY (id),

CONSTRAINT chk_obs_texto
  CHECK (CHAR_LENGTH(TRIM(observacion)) BETWEEN 1 AND 512),

CONSTRAINT chk_obs_baja_coherente
  CHECK (
    (si_activa = TRUE  AND fh_baja IS NULL  AND id_user_baja IS NULL) OR
    (si_activa = FALSE AND fh_baja IS NOT NULL AND id_user_baja IS NOT NULL)
  ),

INDEX idx_obs_entidad   (entidad_tipo, entidad_id, si_activa, fh_alta),
INDEX idx_obs_ctx_acta  (id_acta_contexto, fh_alta),

CONSTRAINT fk_obs_acta_ctx FOREIGN KEY (id_acta_contexto)
  REFERENCES fal_acta(id) ON DELETE CASCADE ON UPDATE RESTRICT
```

### 2.4 Notas de diseño

- **No tiene** `version_row`: la observación no se edita; OCC no aplica.
- `id_acta_contexto → fal_acta(id) ON DELETE CASCADE`: al eliminar físicamente un
  acta (operación no ordinaria), todas sus observaciones contextuales se limpian
  automáticamente. Las observaciones de entidades maestro no tienen este contexto
  y permanecen.
- El patrón de corrección: baja lógica (`si_activa=FALSE`, `fh_baja`, `id_user_baja`)
  + inserción de una nueva observación.

---

## 3. Enum `EntidadTipoObservada`

Enum cerrado. Codificado como `SMALLINT` en `fal_observacion.entidad_tipo`.
No guardar nombres de tabla como strings.

### 3.1 Tabla de valores (22 códigos canónicos)

| Código | Nombre Java                   | Tabla física                          | PK columna       | Puede tener `id_acta_contexto` | Trigger defensivo |
|--------|-------------------------------|---------------------------------------|------------------|-------------------------------|-------------------|
| 1      | `ACTA`                        | `fal_acta`                            | `id`             | NO (es el acta misma)         | NO — ON DELETE CASCADE ya limpia observaciones con `id_acta_contexto` |
| 2      | `PERSONA`                     | `fal_persona`                         | `id`             | NO                            | SÍ — trigger `trg_fal_persona_ad_observaciones` |
| 3      | `DOMICILIO`                   | `fal_persona_domicilio`               | `id`             | SÍ                            | SÍ — trigger `trg_fal_persona_domicilio_ad_observaciones` |
| 4      | `DOCUMENTO`                   | `fal_documento`                       | `id`             | SÍ                            | SÍ — trigger `trg_fal_documento_ad_observaciones` |
| 5      | `EVIDENCIA`                   | `fal_acta_evidencia`                  | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_evidencia_ad_observaciones` |
| 6      | `NOTIFICACION`                | `fal_notificacion`                    | `id`             | SÍ                            | SÍ — trigger `trg_fal_notificacion_ad_observaciones` |
| 7      | `NOTIFICACION_INTENTO`        | `fal_notificacion_intento`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_notificacion_intento_ad_observaciones` |
| 8      | `FALLO`                       | `fal_acta_fallo`                      | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_fallo_ad_observaciones` |
| 9      | `APELACION`                   | `fal_acta_apelacion`                  | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_apelacion_ad_observaciones` |
| 10     | `GESTION_EXTERNA`             | `fal_acta_gestion_externa`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_gestion_externa_ad_observaciones` |
| 11     | `PARALIZACION`                | `fal_acta_paralizacion`               | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_paralizacion_ad_observaciones` |
| 12     | `ARCHIVO`                     | `fal_acta_archivo`                    | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_archivo_ad_observaciones` |
| 13     | `MEDIDA_PREVENTIVA`           | `fal_acta_medida_preventiva`          | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_medida_preventiva_ad_observaciones` |
| 14     | `BLOQUEANTE_CIERRE_MATERIAL`  | `fal_acta_bloqueante_cierre_material` | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_bloqueante_cierre_material_ad_observaciones` |
| 15     | `ARTICULO_INFRINGIDO`         | `fal_acta_articulo_infringido`        | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_articulo_infringido_ad_observaciones` |
| 16     | `VALORIZACION`                | `fal_acta_valorizacion`               | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_valorizacion_ad_observaciones` |
| 17     | `OBLIGACION_PAGO`             | `fal_acta_obligacion_pago`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_obligacion_pago_ad_observaciones` |
| 18     | `FORMA_PAGO`                  | `fal_acta_forma_pago`                 | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_forma_pago_ad_observaciones` |
| 19     | `PLAN_PAGO`                   | `fal_acta_plan_pago_ref`              | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_plan_pago_ref_ad_observaciones` |
| 20     | `MOVIMIENTO_PAGO`             | `fal_acta_pago_movimiento`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_pago_movimiento_ad_observaciones` |
| 21     | `TALONARIO`                   | `num_talonario`                       | `id`             | NO                            | SÍ — trigger `trg_num_talonario_ad_observaciones` |
| 22     | `MOVIMIENTO_TALONARIO`        | `num_talonario_movimiento`            | `id`             | NO                            | SÍ — trigger `trg_num_talonario_movimiento_ad_observaciones` |

### 3.2 Reglas del enum

- Valores consecutivos 1–22. No renumerar sin actualizar Java y tests.
- No inventar nuevos valores en DDL ni Java sin actualizar esta tabla y el enum.
- No guardar nombres de tabla como strings en `entidad_tipo`.
- La clase Java es `ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada`.

---

## 4. Enum `OrigenObservacion`

Enum cerrado. Codificado como `SMALLINT` en `fal_observacion.origen_observacion`.

| Código | Nombre Java    | Significado |
|--------|----------------|-------------|
| 1      | `USUARIO`      | Nota ingresada directamente por un operador humano |
| 2      | `SISTEMA`      | Nota generada automáticamente por un proceso del sistema |
| 3      | `INTEGRACION`  | Nota proveniente de un sistema externo integrado |

La clase Java es `ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion`.

---

## 5. Triggers defensivos AFTER DELETE

### 5.1 Propósito

La eliminación física no es operación normal de producción. Las bajas lógicas
conservan observaciones. Los triggers son una red de seguridad para limpiar
observaciones polimórficas cuando se borre físicamente la entidad padre.

### 5.2 Convención de nombres

```
trg_<tabla>_ad_observaciones
```

### 5.3 Patrón único

```sql
CREATE TRIGGER trg_<tabla>_ad_observaciones
AFTER DELETE ON <tabla>
FOR EACH ROW
DELETE FROM fal_observacion
WHERE entidad_tipo = <CODIGO_FIJO>
  AND entidad_id = OLD.<PK>;
```

### 5.4 Reglas

- Un trigger por tabla física observable aplicable.
- Código de entidad fijo (literal numérico en el cuerpo del trigger).
- Sin SQL dinámico.
- Sin procedimientos almacenados.
- Sin condicionales complejos.
- Sin auditoría adicional.
- Sin actualización de otras entidades.
- Sin lógica de negocio.

Para entidades bajo acta:
- El trigger permite borrar individualmente el hijo y limpiar sus observaciones.
- `ON DELETE CASCADE` de `id_acta_contexto` limpia **todas** las observaciones
  contextuales al borrar el acta completa, sin necesidad de trigger en `fal_acta`.

No crear trigger para `ACTA` (código 1): el CASCADE ya limpia observaciones
contextuales al eliminar `fal_acta`.

### 5.5 Listado completo de triggers

| Trigger | Tabla | Código fijo | PK columna |
|---------|-------|-------------|------------|
| `trg_fal_persona_ad_observaciones`                        | `fal_persona`                         | 2  | `id` |
| `trg_fal_persona_domicilio_ad_observaciones`              | `fal_persona_domicilio`               | 3  | `id` |
| `trg_fal_documento_ad_observaciones`                      | `fal_documento`                       | 4  | `id` |
| `trg_fal_acta_evidencia_ad_observaciones`                 | `fal_acta_evidencia`                  | 5  | `id` |
| `trg_fal_notificacion_ad_observaciones`                   | `fal_notificacion`                    | 6  | `id` |
| `trg_fal_notificacion_intento_ad_observaciones`           | `fal_notificacion_intento`            | 7  | `id` |
| `trg_fal_acta_fallo_ad_observaciones`                     | `fal_acta_fallo`                      | 8  | `id` |
| `trg_fal_acta_apelacion_ad_observaciones`                 | `fal_acta_apelacion`                  | 9  | `id` |
| `trg_fal_acta_gestion_externa_ad_observaciones`           | `fal_acta_gestion_externa`            | 10 | `id` |
| `trg_fal_acta_paralizacion_ad_observaciones`              | `fal_acta_paralizacion`               | 11 | `id` |
| `trg_fal_acta_archivo_ad_observaciones`                   | `fal_acta_archivo`                    | 12 | `id` |
| `trg_fal_acta_medida_preventiva_ad_observaciones`         | `fal_acta_medida_preventiva`          | 13 | `id` |
| `trg_fal_acta_bloqueante_cierre_material_ad_observaciones`| `fal_acta_bloqueante_cierre_material` | 14 | `id` |
| `trg_fal_acta_articulo_infringido_ad_observaciones`       | `fal_acta_articulo_infringido`        | 15 | `id` |
| `trg_fal_acta_valorizacion_ad_observaciones`              | `fal_acta_valorizacion`               | 16 | `id` |
| `trg_fal_acta_obligacion_pago_ad_observaciones`           | `fal_acta_obligacion_pago`            | 17 | `id` |
| `trg_fal_acta_forma_pago_ad_observaciones`                | `fal_acta_forma_pago`                 | 18 | `id` |
| `trg_fal_acta_plan_pago_ref_ad_observaciones`             | `fal_acta_plan_pago_ref`              | 19 | `id` |
| `trg_fal_acta_pago_movimiento_ad_observaciones`           | `fal_acta_pago_movimiento`            | 20 | `id` |
| `trg_num_talonario_ad_observaciones`                      | `num_talonario`                       | 21 | `id` |
| `trg_num_talonario_movimiento_ad_observaciones`           | `num_talonario_movimiento`            | 22 | `id` |

Total: **21 triggers** (todos los códigos 2–22; código 1/ACTA usa CASCADE).

---

## 6. Validación al insertar observaciones

Implementada en el futuro `ObservacionService` (JDBC no implementado en este slice).

Contrato:

```text
switch cerrado por EntidadTipoObservada;
repositorio existe(id) por cada tipo;
sin nombres de tabla recibidos por request;
sin SQL dinámico.
```

La existencia de la entidad observada se valida en aplicación, no en DDL
(no hay FK polimórfica en MariaDB para este patrón).

---

## 7. Matriz de clasificación de campos libres

Clasificación de todos los campos con patrones `observacion`, `obs_`, `descripcion`,
`detalle`, `motivo`, `fundamento`, `resumen` en el modelo de dominio.

| Tabla / Entidad Java                    | Campo actual                              | Clasificación                         | Motivo |
|-----------------------------------------|-------------------------------------------|---------------------------------------|--------|
| `fal_observacion`                       | `observacion`                             | MANTENER_ESTRUCTURAL                  | Es la columna central de almacenamiento |
| `fal_acta`                              | `resumen_hecho`                           | MANTENER_ESTRUCTURAL                  | Descripción funcional del hecho, no nota libre |
| `fal_acta`                              | `dom_txt_infr`                            | MANTENER_ESTRUCTURAL                  | Domicilio del hecho, no nota libre |
| `fal_acta_paralizacion`                 | `descripcion`                             | MANTENER_ESTRUCTURAL                  | Descripción funcional de la paralización |
| `fal_acta_archivo`                      | `descripcion`                             | MANTENER_ESTRUCTURAL                  | Descripción funcional del archivo |
| `fal_acta_apelacion`                    | `texto_apelacion`                         | MANTENER_ESTRUCTURAL                  | Contenido jurídico de la apelación |
| `fal_acta_apelacion`                    | `fundamentos`                             | MANTENER_ESTRUCTURAL                  | Contenido jurídico; base legal de la apelación |
| `fal_acta_apelacion`                    | `fundamentos_resolucion`                  | MANTENER_ESTRUCTURAL                  | Contenido jurídico; base de la resolución |
| `fal_acta_fallo`                        | `fundamentos`                             | MANTENER_ESTRUCTURAL                  | Contenido jurídico del fallo |
| `fal_acta_bloqueante_cierre_material`   | `descripcion`                             | MANTENER_ESTRUCTURAL                  | Descripción funcional del bloqueante |
| `fal_acta_medida_preventiva`            | `med_prev_txt`                            | MANTENER_ESTRUCTURAL                  | Texto descriptivo de la medida aplicada |
| `fal_acta_sustancias_alimenticias`      | `descripcion_sustancias`                  | MANTENER_ESTRUCTURAL                  | Descripción funcional de las sustancias |
| `fal_acta_contravencion`                | `ambito_ctv_txt`                          | MANTENER_ESTRUCTURAL                  | Texto libre solo cuando ambito_ctv=OTRO |
| `fal_acta_sustancias_alimenticias`      | `ambito_ctv_txt`                          | MANTENER_ESTRUCTURAL                  | Texto libre solo cuando ambito_ctv=OTRO |
| `fal_documento_plantilla_contenido`     | `variables_declaradas_json`               | MANTENER_ESTRUCTURAL                  | Metadatos estructurados, no nota libre |
| `fal_documento_redaccion`               | `contenido_editable_markdown`             | MANTENER_ESTRUCTURAL                  | Contenido documental editable |
| `fal_documento_redaccion`               | `variables_snapshot_json`                 | MANTENER_ESTRUCTURAL                  | Snapshot estructurado de valores |
| `num_talonario`                         | `obs_talonario`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(TALONARIO) |
| `num_talonario_movimiento`              | `observacion`                             | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(MOVIMIENTO_TALONARIO) |
| `fal_acta_articulo_infringido`          | `observaciones`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(ARTICULO_INFRINGIDO) |
| `fal_notificacion`                      | `observaciones`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(NOTIFICACION) |
| `fal_acta_apelacion`                    | `observaciones`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(APELACION) |
| `fal_acta_apelacion`                    | `observaciones_resolucion`                | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(APELACION) |
| `fal_acta_pago_movimiento`              | `motivo_aplicacion_pago_anterior VARCHAR` | CENTRALIZAR_EN_FAL_OBSERVACION        | Explicación libre; idempotencia estructural via FK movimiento_origen_id |
| `fal_documento_redaccion`               | `variables_faltantes_json`                | ELIMINAR_DERIVABLE                    | Calculable en aplicación; no persistir diagnóstico de estado |
| `fal_documento_redaccion`               | `diagnostico_json`                        | ELIMINAR_DERIVABLE                    | Calculable en aplicación; no persistir estado intermedio |
| `fal_acta_evento`                       | `descripcion_legible`                     | ELIMINAR_DERIVABLE                    | Reconstruible desde tipo_evento + actor + timestamp |
| `fal_documento`                         | `descripcion`                             | ELIMINAR_DERIVABLE                    | El nombre lo comunica la plantilla; no duplicar en documento |
| `fal_documento_plantilla`               | `descripcion`                             | ELIMINAR_DERIVABLE                    | nombre VARCHAR(64) debe comunicar uso e intención |
| `fal_persona`                           | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_persona_domicilio`                 | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_acta_evidencia`                    | `obs_evid` (si existiese)                 | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_documento`                         | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_acta_gestion_externa`              | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_acta_articulo_infringido`          | `observacion_baja` (si existiese)         | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |

---

## 8. Campos embebidos eliminados (HUMAN_DECISION_CLOSED)

Los siguientes campos son eliminados del DDL y del modelo Java en este slice:

| Campo eliminado                                        | Entidad Java              | Motivo |
|--------------------------------------------------------|---------------------------|--------|
| `num_talonario.obs_talonario`                          | `NumTalonario`            | Centralizar en fal_observacion |
| `num_talonario_movimiento.observacion`                 | `NumTalonarioMovimiento`  | Centralizar en fal_observacion |
| `fal_acta_articulo_infringido.observaciones`           | `FalActaArticuloInfringido` | Centralizar en fal_observacion |
| `fal_notificacion.observaciones`                       | `FalNotificacion`         | Centralizar en fal_observacion |
| `fal_acta_apelacion.observaciones`                     | `FalActaApelacion`        | Centralizar en fal_observacion |
| `fal_acta_apelacion.observaciones_resolucion`          | `FalActaApelacion`        | Centralizar en fal_observacion |
| `fal_acta_pago_movimiento.motivo_aplicacion_pago_anterior` | `FalActaPagoMovimiento` | Texto libre no estructurado; usar fal_observacion + movimiento_origen_id para trazabilidad |
| `fal_documento_redaccion.variables_faltantes_json`     | `FalDocumentoRedaccion`   | Derivable; no persistir |
| `fal_documento_redaccion.diagnostico_json`             | `FalDocumentoRedaccion`   | Derivable; no persistir |
| `fal_acta_evento.descripcion_legible`                  | `FalActaEvento`           | Derivable desde tipo_evento + actor + timestamp |
| `fal_documento.descripcion`                            | `FalDocumento`            | Derivable desde plantilla |
| `fal_documento_plantilla.descripcion`                  | `FalDocumentoPlantilla`   | nombre debe comunicar uso; descripcion redundante |
