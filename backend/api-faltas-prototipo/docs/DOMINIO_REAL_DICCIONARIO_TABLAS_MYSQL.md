# Diccionario compacto de tablas MySQL — Dominio real Faltas

**Fecha:** 2026-06-13
**Estado:** PROPUESTA REVISABLE — no es dominio cerrado ni verdad final
**Fuente:** `DOMINIO_REAL_MODELO_TABLAS_MYSQL.md`
**Destino:** MySQL 8.x / InnoDB

---

## 1. Criterios minimos

- Motor: `InnoDB` obligatorio en todas las tablas de dominio.
- Charset/collation: `utf8mb4` — collation pendiente (`utf8mb4_0900_ai_ci` recomendado para MySQL 8.0+).
- PKs: `BIGINT AUTO_INCREMENT` por tabla. Excepcion: `stor_objeto` usa `storage_key VARCHAR(64)` como PK natural.
- Fecha/hora: `DATETIME(6)`. Fecha sola: `DATE`.
- Importes: `DECIMAL(14,2)`. Coordenadas GPS: `DECIMAL(12,8)`.
- Flags: `BOOLEAN` (`TINYINT(1)`).
- Observaciones largas: `TEXT`.
- `ENUM`: evitar. Preferir `SMALLINT` con catalogo documentado.
- `JSON`: solo para `payload_json` en `fal_acta_evento` si se adopta estrategia hibrida de TipoEvt.
- Nombres: `snake_case` en minusculas.
- Auditoria minima estandar: `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.

---

## 2. Mapa de tablas

| Tabla | Proposito | Tipo | Prioridad | Estado |
|---|---|---|---|---|
| `fal_acta` | Agregado raiz del expediente | principal | obligatoria | casi segura |
| `fal_acta_evento` | Historial append-only | historica | obligatoria | casi segura — decision TipoEvt |
| `fal_acta_snapshot` | Proyeccion operativa cache | snapshot/cache | obligatoria | casi segura |
| `fal_acta_evidencia` | Evidencias digitales | hija | probable | casi segura |
| `fal_observacion` | Observaciones polimorficas | hija | probable | casi segura |
| `fal_documento` | Pieza documental firmable | principal | obligatoria | casi segura |
| `fal_acta_documento` | Relacion acta-documento con rol | hija | obligatoria | casi segura |
| `fal_documento_firma` | Registro de firma por pieza | hija | obligatoria | casi segura |
| `fal_notificacion` | Proceso de notificacion al infractor | principal | obligatoria | casi segura |
| `fal_notificacion_intento` | Intento individual por canal | hija | obligatoria | casi segura |
| `fal_notificacion_acuse` | Acuse de recibo | hija | obligatoria | casi segura |
| `fal_acta_pago_voluntario` | Circuito pago voluntario pre-fallo | hija | probable | requiere veredicto |
| `fal_acta_pago_condena` | Pago de condena firme post-fallo | hija | probable | requiere veredicto |
| `fal_acta_fallo` | Decision juridica con monto | hija | probable | requiere veredicto |
| `fal_acta_apelacion` | Apelacion con resultado y canal | hija | probable | requiere veredicto |
| `fal_acta_bloqueante_cierre_material` | Tres origenes de bloqueo con 2 pasos | hija | probable | requiere veredicto |
| `fal_acta_paralizacion` | Historial de paralizaciones | historica | probable | requiere veredicto |
| `fal_acta_archivo` | Historial de archivos con causales | historica | probable | requiere veredicto |
| `fal_acta_gestion_externa` | Derivaciones con resultado | hija | probable | requiere veredicto |
| `fal_lote_correo` | Agrupacion de envios postales | integracion | diferible | diferible |
| `fal_acta_qr_acceso` | Acceso QR portal infractor | integracion | diferible | diferible |
| `fal_dependencia` | Dependencia organizacional | catalogo | obligatoria | casi segura |
| `fal_dependencia_version` | Version historica de dependencia | historica | obligatoria | casi segura |
| `fal_inspector` | Inspector actor del labrado | catalogo | obligatoria | casi segura |
| `fal_inspector_version` | Version historica de inspector | historica | obligatoria | casi segura |
| `fal_medida_preventiva` | Tipos de medida preventiva | catalogo | probable | casi segura |
| `fal_acta_medida_preventiva` | Medidas preventivas del acta | hija | probable | casi segura |
| `fal_normativa_faltas` | Normativa de faltas vigente | catalogo | probable | casi segura |
| `fal_articulo_normativa_faltas` | Articulos de la normativa | catalogo | probable | casi segura |
| `fal_tarifario_unidad_faltas` | Tarifario de unidades de multa | catalogo | probable | casi segura |
| `fal_acta_articulo_infringido` | Articulos infringidos en el acta | hija | probable | casi segura |
| `fal_acta_articulo_auditoria` | Auditoria de cambios en articulos | historica | probable | casi segura |
| `num_politica` | Politica de numeracion de talonarios | catalogo | obligatoria | casi segura |
| `num_talonario` | Talonario de numeracion | catalogo | obligatoria | casi segura |
| `num_talonario_dependencia` | Asignacion talonario-dependencia | hija | obligatoria | casi segura |
| `num_talonario_inspector` | Asignacion talonario-inspector | hija | obligatoria | casi segura |
| `num_talonario_movimiento` | Movimientos de uso de talonario | historica | obligatoria | casi segura |
| `stor_backend` | Backend de storage documental | catalogo | obligatoria | casi segura |
| `stor_politica` | Politica de storage por tipo | catalogo | obligatoria | casi segura |
| `stor_objeto` | Objeto fisico en storage | integracion | obligatoria | casi segura |
| `fal_acta_transito` | Datos de acta de transito | satelite | diferible | diferible |
| `fal_acta_vehiculo` | Datos del vehiculo infractor | satelite | diferible | diferible |
| `fal_acta_contravencion` | Datos contravencionales | satelite | diferible | diferible |
| `fal_acta_alcoholemia` | Datos de alcoholemia | satelite | diferible | diferible |

---

## 3. Relaciones principales

| Tabla origen | Relacion | Tabla destino | Cardinalidad | Observacion |
|---|---|---|---|---|
| `fal_acta` | → | `fal_acta_evento` | 1:N | append-only; fuente de verdad del historial |
| `fal_acta` | → | `fal_acta_snapshot` | 1:1 | cache regenerable; no editar manualmente |
| `fal_acta` | → via `fal_acta_documento` | `fal_documento` | N:M | con rol funcional por fila |
| `fal_acta` | → | `fal_notificacion` | 1:N | — |
| `fal_acta` | → | `fal_acta_pago_voluntario` | 1:0..N | historial de intentos por acta |
| `fal_acta` | → | `fal_acta_pago_condena` | 1:0..N | solo con CONDENA_FIRME |
| `fal_acta` | → | `fal_acta_fallo` | 1:0..N | N posible post gestion externa |
| `fal_acta` | → | `fal_acta_apelacion` | 1:0..1 activa | — |
| `fal_acta` | → | `fal_acta_bloqueante_cierre_material` | 1:N | hasta 3 origenes distintos |
| `fal_acta` | → | `fal_acta_paralizacion` | 1:N | historial completo |
| `fal_acta` | → | `fal_acta_archivo` | 1:N | reingreso permite N archivos |
| `fal_acta` | → | `fal_acta_gestion_externa` | 1:N | historial de derivaciones |
| `fal_documento` | → | `fal_documento_firma` | 1:N | una firma por firmante |
| `fal_notificacion` | → | `fal_notificacion_intento` | 1:N | — |
| `fal_notificacion` | → | `fal_notificacion_acuse` | 1:0..1 | — |

---

## 4. Diccionario por tabla

### `fal_acta`

**Proposito:** Agregado raiz del expediente de faltas. Fuente de verdad primaria junto con `fal_acta_evento`.
**Tipo:** principal | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `fal_dependencia.id`, `fal_inspector.id`

| Campo | Tipo MySQL | Null | Clave/indice | Relacion | Uso |
|---|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — | Identificador interno |
| `id_tecnico` | `CHAR(36)` | NO | UNIQUE | — | UUID externo mobile/APIs |
| `nro_acta` | `VARCHAR(30)` | SI | IDX | — | Numero visible (talonario) |
>>CAMBIO: Tipo de dato VARCHAR(20), MOT: Es muy largo, nunca se va llegar a 30.

| `tipo_acta` | `SMALLINT` | NO | IDX | — | Tipo (transito, bromatologia, etc.) |
| `origen_captura` | `SMALLINT` | NO | — | — | Canal de labrado |
| `fh_acta` | `DATETIME(6)` | NO | IDX | — | Fecha y hora del labrado |
| `id_dep` | `BIGINT` | NO | FK+IDX | `fal_dependencia.id` | Dependencia del acta |
| `ver_dep` | `SMALLINT` | NO | — | — | Version dependencia al labrar |
| `id_insp` | `BIGINT` | SI | FK+IDX | `fal_inspector.id` | Inspector del acta |
| `ver_insp` | `SMALLINT` | SI | — | — | Version inspector al labrar |
| `nom_infct` | `VARCHAR(120)` | SI | IDX | — | Nombre del infractor |
| `doc_pref_infct` | `SMALLINT` | SI | — | — | Prefijo documento (DNI/CUIT/etc.) |
| `doc_nro_infct` | `BIGINT` | SI | IDX | — | Numero documento infractor |
| `tipo_pers_infct` | `SMALLINT` | SI | — | — | Tipo persona (fisica/juridica) |
| `email_infct` | `VARCHAR(120)` | SI | — | — | Email para notificacion electronica |
| `resumen_hecho` | `TEXT` | SI | — | — | Descripcion narrativa del hecho |
| `bloque_actual` | `SMALLINT` | NO | IDX | — | Bloque juridico actual |
| `est_proc_act` | `SMALLINT` | NO | IDX | — | Estado del proceso actual |
| `sit_adm_act` | `SMALLINT` | NO | IDX | — | Situacion administrativa actual |
| `resultado_final` | `SMALLINT` | SI | IDX | — | Resultado juridico (0-4) |
| `esta_cerrada` | `BOOLEAN` | NO | IDX | — | Flag cierre definitivo |
| `motivo_archivo` | `SMALLINT` | SI | — | — | Motivo del archivo |
| `permite_reingreso` | `BOOLEAN` | SI | — | — | Habilitacion de reingreso |
| `id_tipo_gestion_ext_act` | `SMALLINT` | SI | — | — | Tipo de gestion externa activa |
>> CAMBIO: eliminar `id_tipo_gestion_ext_act` de `fal_acta`.
>> MOT: la gestión externa activa debe salir de `fal_acta_gestion_externa`; si hace falta para bandejas, cachearla en `fal_acta_snapshot`.

| *(domicilio infractor)* | ~15 campos | — | — | refs geo + texto libre | Domicilio del infractor |
>> CAMBIO: reemplazar el bloque embebido de domicilio infractor por referencias a `fal_persona` y `fal_persona_domicilio`.
>> MOT: una persona puede tener más de un domicilio; además, durante reintentos de notificación puede descubrirse un nuevo domicilio y usarse como destino.

>> CAMBIO: agregar en `fal_acta`: `id_persona_infractor BIGINT NOT NULL`, `id_domicilio_infractor_act BIGINT NULL`, `id_domicilio_notif_act BIGINT NULL`.
>> MOT: el acta debe relacionarse con una persona técnica aunque no exista documento; y debe saber cuál domicilio se tomó como domicilio del infractor y cuál se usa para notificaciones postales.

| `nom_infct` / `doc_pref_infct` / `doc_nro_infct` / `tipo_pers_infct` | ... |
>> CAMBIO: mover estos datos a `fal_persona`.
>> MOT: son datos de identificación de la persona, no atributos propios del acta. En `fal_acta` debe quedar la FK `id_persona_infractor`.

| *(domicilio infraccion)* | ~8 campos | — | — | refs geo + `dom_txt_infr` | Lugar del hecho |
| `lat_infr` | `DECIMAL(12,8)` | SI | — | — | GPS latitud |
| `lon_infr` | `DECIMAL(12,8)` | SI | — | — | GPS longitud |
| *(auditoria)* | `fh_alta`, `id_user_alta`, `fh_ult_mod`, `id_user_ult_mod`, `fh_cierre`, `fh_archivo` | — | — | — | Trazabilidad |
>> CAMBIO: separar auditoría técnica de fechas de negocio.
>> MOT: `fh_alta/id_user_alta/fh_ult_mod/id_user_ult_mod` son auditoría técnica del registro; `fh_cierre` y `fh_archivo` son estado/eventos de negocio.
>> CAMBIO: quitar `fh_cierre` y `fh_archivo` del bloque auditoría.
>> MOT: cierre y archivo deben surgir de eventos (`fal_acta_evento`) y/o tablas específicas (`fal_acta_archivo`); si quedan en `fal_acta`, son campos de estado actual, no auditoría.
| *(auditoria tecnica)* | `fh_alta`, `id_user_alta`, `fh_ult_mod`, `id_user_ult_mod` | — | — | — | Control técnico del registro |
| `fh_cierre` | `DATETIME(6)` | SI | IDX | — | Fecha de cierre actual |
| `fh_archivo` | `DATETIME(6)` | SI | IDX | — | Fecha de archivo actual |

> **Dudas:** confirmar si `bloque_actual`/`est_proc_act` viven en acta o solo en snapshot; alinear valores de `motivo_archivo` (DDL 5 vals vs prototipo 3 distintos); confirmar si `esta_cerrada` es campo bool o se deriva de `est_proc_act=17`.
>> CAMBIO: resolver duda. `bloque_actual`, `est_proc_act`, `sit_adm_act`, `resultado_final` y `esta_cerrada` viven en `fal_acta` como estado actual del expediente. `fal_acta_snapshot` solo los replica como cache/proyección para bandejas y filtros.
>> MOT: el snapshot no debe ser verdad primaria; el acta necesita estado actual propio.

>> CAMBIO: `motivo_archivo` debe usar catálogo real de dominio, no copiar sin revisar los valores del DDL viejo.
>> MOT: archivo no es cierre y sus motivos deben ser legibles para API/QA.

>> CAMBIO: mantener `esta_cerrada BOOLEAN` como campo explícito en `fal_acta`.
>> MOT: el cierre es una acción explícita, no una derivación opaca de `est_proc_act`.

## Ajuste en `fal_acta`

| Campo | Tipo MySQL | Null | Clave/índice | Descripción |
|---|---:|---:|---|---|
| `id_persona_infractor` | `BIGINT` | NO | FK+IDX | Persona infractora asociada al acta |
| `id_domicilio_infractor_act` | `BIGINT` | SI | FK+IDX | Domicilio tomado como domicilio del infractor al labrar/normalizar el acta |
| `id_domicilio_notif_act` | `BIGINT` | SI | FK+IDX | Domicilio actualmente seleccionado para notificaciones postales del acta |

Reglas:
- `id_persona_infractor` es obligatorio porque toda acta debe tener un sujeto/infractor técnico, aunque sus datos estén incompletos.
- `id_domicilio_infractor_act` conserva el domicilio tomado como domicilio del infractor para esa acta.
- `id_domicilio_notif_act` indica qué domicilio se usa actualmente como destino postal/notificable del acta.
- Ambos domicilios pueden coincidir, pero no tienen por qué coincidir.

---

## Tabla `fal_persona`

Representa a la persona física o jurídica vinculada al acta como infractor/destinatario.

| Campo | Tipo MySQL | Null | Clave/índice | Descripción |
|---|---:|---:|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador técnico de persona |
| `tipo_persona` | `SMALLINT` | SI | IDX | Física / jurídica / desconocida |
| `tipo_doc` | `SMALLINT` | SI | IDX | Tipo de documento: DNI, CUIT, pasaporte, etc. |
| `nro_doc` | `VARCHAR(20)` | SI | IDX | Número de documento |
| `nombre_razon` | `VARCHAR(160)` | SI | IDX | Nombre completo o razón social |
| `email_principal` | `VARCHAR(160)` | SI | — | Email principal conocido, si existe |
| `telefono_principal` | `VARCHAR(40)` | SI | — | Teléfono principal conocido, si existe |
| `observacion` | `TEXT` | SI | — | Observaciones sobre identificación |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta técnica |
| `fh_ult_mod` | `DATETIME(6)` | NO | — | Última modificación |
| `id_user_ult_mod` | `CHAR(36)` | SI | — | Usuario última modificación |

Reglas:
- La PK real es `id`.
- `tipo_doc` + `nro_doc` NO pueden ser PK porque pueden faltar.
- Si existen `tipo_doc` + `nro_doc`, deben usarse para detectar duplicados funcionales.
- Puede existir persona sin documento si el acta fue labrada con datos incompletos.

>> CAMBIO: agregar índice funcional sobre `tipo_doc`, `nro_doc`.
>> MOT: documento + tipo documento identifican funcionalmente a la persona cuando existen, pero no siempre están disponibles; por eso la PK debe ser `id BIGINT`.

---

## Tabla `fal_persona_domicilio`

Representa domicilios conocidos de una persona. Puede haber más de un domicilio por persona.

| Campo | Tipo MySQL | Null | Clave/índice | Descripción |
|---|---:|---:|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del domicilio |
| `persona_id` | `BIGINT` | NO | FK+IDX | Persona a la que pertenece |
| `acta_origen_id` | `BIGINT` | SI | FK+IDX | Acta desde la cual se originó/cargó este domicilio |
| `tipo_domicilio` | `SMALLINT` | NO | IDX | Real, legal, fiscal, constituido, hallado, otro |
| `origen_domicilio` | `SMALLINT` | NO | IDX | Labrado, investigación, reintento notificación, portal, sistema externo, operador |
| `si_activo` | `BOOLEAN` | NO | IDX | Domicilio activo |
| `si_notificable` | `BOOLEAN` | NO | IDX | Puede usarse como destino postal/notificación |
| `si_principal` | `BOOLEAN` | NO | IDX | Domicilio principal conocido de la persona |
| `id_provincia` | `BIGINT` | SI | IDX | Provincia normalizada |
| `id_departamento` | `BIGINT` | SI | IDX | Departamento/partido |
| `id_municipio` | `BIGINT` | SI | IDX | Municipio |
| `id_localidad` | `BIGINT` | SI | IDX | Localidad |
| `id_calle` | `BIGINT` | SI | IDX | Calle normalizada |
| `calle_txt` | `VARCHAR(120)` | SI | — | Calle en texto libre si no está normalizada |
| `altura` | `VARCHAR(20)` | SI | — | Altura/número |
| `piso` | `VARCHAR(10)` | SI | — | Piso |
| `depto` | `VARCHAR(10)` | SI | — | Departamento |
| `codigo_postal` | `VARCHAR(12)` | SI | — | Código postal |
| `observacion` | `TEXT` | SI | — | Observaciones |
| `domicilio_txt` | `VARCHAR(255)` | SI | — | Domicilio textual completo/snapshot legible |
| `si_normalizado_parcial` | `BOOLEAN` | NO | — | Tiene normalización parcial |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta técnica |
| `fh_ult_mod` | `DATETIME(6)` | NO | — | Última modificación |
| `id_user_ult_mod` | `CHAR(36)` | SI | — | Usuario última modificación |

Reglas:
- Una persona puede tener N domicilios.
- Un domicilio puede haberse originado en el labrado del acta o en una investigación posterior.
- Un reintento de notificación puede usar un domicilio nuevo hallado durante la gestión.
- No borrar domicilios usados por notificaciones; desactivar si corresponde.

---

### `fal_acta_evento`

**Proposito:** Historial inmutable append-only de hechos del expediente.
**Tipo:** historica | **Prioridad:** obligatoria | **Estado:** casi segura — decision TipoEvt pendiente
**Relaciones:** `fal_acta.id`, `fal_documento.id`, `fal_notificacion.id`

| Campo | Tipo MySQL | Null | Clave/indice | Relacion | Uso |
|---|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — | Identificador del evento |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` | Acta asociada |
| `tipo_evt` | `SMALLINT` | NO | IDX | — | Tipo de evento (catalogo) |
| `tipo_evt` | `SMALLINT` | NO | IDX | — | Tipo de evento (catálogo) |
>> CAMBIO: definir `tipo_evt` desde los 62 eventos candidatos del prototipo validado.
>> MOT: el prototipo fue iterado y validado funcionalmente; esos eventos representan los hechos reales que necesita el dominio. El DDL histórico de 19 eventos queda corto y no debe limitar el modelo real.

| `origen_evt` | `SMALLINT` | NO | — | — | Origen del actor (5 vals) |
| `fh_evt` | `DATETIME(6)` | NO | IDX | — | Fecha/hora del evento |
| `bloque_func` | `SMALLINT` | NO | — | — | Bloque al momento del evento |
| `est_proc_ant` | `SMALLINT` | SI | — | — | Estado proceso antes |
| `est_proc_nvo` | `SMALLINT` | SI | — | — | Estado proceso despues |
| `sit_adm_ant` | `SMALLINT` | SI | — | — | Situacion administrativa antes |
| `sit_adm_nva` | `SMALLINT` | SI | — | — | Situacion administrativa despues |
| `actor_tipo` | `SMALLINT` | SI | — | — | Tipo de actor ejecutor |
>> CAMBIO: documentar catálogo inicial de `actor_tipo`: USUARIO_INTERNO, INSPECTOR, INFRACTOR, SISTEMA, INTEGRACION_EXTERNA, NOTIFICADOR_MUNICIPAL.
>> MOT: identifica el origen humano/sistema del evento; el usuario exacto queda en `id_user_evt` cuando aplica.

| `actor_id` | `VARCHAR(60)` | SI | — | — | Identificador del actor |
>> CAMBIO: eliminar `actor_id`.
>> MOT: no queda claro su uso y se superpone con `id_user_evt`; el usuario autenticado se registra con el subject del IDP en `id_user_evt VARCHAR(36)`.

| `id_docu_rel` | `BIGINT` | SI | FK | `fal_documento.id` | Documento relacionado |
| `id_notif_rel` | `BIGINT` | SI | FK | `fal_notificacion.id` | Notificacion relacionada |
| `id_user_evt` | `CHAR(36)` | SI | — | — | Usuario ejecutor |
| `si_evt_cierre` | `BOOLEAN` | NO | — | — | Flag evento de cierre |
| `si_evt_ext` | `BOOLEAN` | NO | — | — | Flag evento externo |
| `si_permite_reing` | `BOOLEAN` | NO | — | — | Flag permite reingreso |
| `descripcion_legible` | `VARCHAR(255)` | SI | — | — | Descripcion legible para log/UI |
| `payload_json` | `JSON` | SI | — | — | Payload si estrategia hibrida |
| `payload_json` | `JSON` | SI | — | — | Payload si estrategia híbrida |
>> CAMBIO: eliminar `payload_json`.
>> MOT: no tiene uso concreto actual; evitar un campo genérico que esconda reglas de dominio.

> **Restricción:** append-only. Ningún evento se edita ni borra.
> **Criterio `tipo_evt`:** el catálogo inicial debe basarse en los eventos del prototipo validado. El DDL histórico de 19 tipos queda como referencia, pero no limita el modelo real. No usar `payload_json` por ahora.
>> CAMBIO: eliminar esta nota.
>> MOT: el criterio ya queda documentado debajo del campo `tipo_evt`; evitar duplicación.

---

### `fal_acta_snapshot`

**Proposito:** Proyeccion operativa regenerable. Cache derivado de `fal_acta` + `fal_acta_evento` + satelites.
**Tipo:** snapshot/cache | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `fal_acta.id` (1:1)

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK | FK a `fal_acta`; clave unica 1:1 |
| `bloque_actual` | `SMALLINT` | NO | IDX | Cache bloque juridico |
| `est_proc_act` | `SMALLINT` | NO | IDX | Cache estado proceso |
| `sit_adm_act` | `SMALLINT` | NO | IDX | Cache situacion administrativa |
| `cod_bandeja` | `VARCHAR(50)` | SI | IDX | Discriminador de bandeja operativa |
| `si_visible_bandeja` | `BOOLEAN` | NO | IDX | Visibilidad en bandeja |
| `prioridad` | `SMALLINT` | SI | IDX | Orden en bandeja |
| `resultado_final` | `SMALLINT` | SI | IDX | Cache resultado juridico |
| `motivo_paralizacion_act` | `SMALLINT` | SI | IDX | **Gap critico D10** — motivo activo |
| `accion_pendiente` | `SMALLINT` | SI | IDX | Discriminador de sub-bandeja |
| `si_bloqueante_med_prev` | `BOOLEAN` | NO | IDX | **Gap D11** — medida preventiva activa |
| `si_bloqueante_rodado` | `BOOLEAN` | NO | IDX | **Gap D11** — rodado secuestrado |
| `si_bloqueante_doc_ret` | `BOOLEAN` | NO | IDX | **Gap D11** — documentacion retenida |
| *(bloque notificaciones)* | 9 flags `si_notif_*` + `cant_reint_notif SMALLINT` | — | — | Cache estado notificaciones |
| *(bloque pago voluntario)* | `si_pago_volunt BOOLEAN`, `fh_pago_volunt DATE`, `monto_acta DECIMAL(14,2)`, `est_pago_act SMALLINT` | — | IDX | Cache pago voluntario |
| *(bloque pago condena)* | `si_pago_total BOOLEAN`, `si_plan_pago BOOLEAN`, `cant_cuotas_plan`, `valor_cuota_plan`, `cant_caidas_plan` | — | — | Cache pago condena |
| *(bloque gestion externa)* | `si_gestion_ext BOOLEAN`, `tipo_gestion_ext SMALLINT`, `si_reingreso_gestion_ext BOOLEAN`, `resultado_gestion_ext SMALLINT` | — | IDX | Cache gestion externa |
| *(bloque vencimientos)* | `fh_vto_presentacion DATE`, `fh_vto_apelacion DATE`, `fh_vto_apremio DATE` | — | IDX | Fechas clave para bandejas |
| *(bloque punteros)* | `id_evt_ult BIGINT`, `id_docu_ult BIGINT`, `id_notif_ult BIGINT` | — | FK | Punteros de control |
| `fh_ult_mod` | `DATETIME(6)` | NO | — | Ultima actualizacion del snapshot |
| `fh_snapshot` | `DATETIME(6)` | NO | — | Fecha de generacion |
| `rebuild_id` | `BIGINT` | SI | — | ID del ultimo ciclo de rebuild global |
>> CAMBIO: mantener `rebuild_id`, pero aclarar que es control técnico de reproceso masivo de snapshot.
>> MOT: sirve para verificar qué snapshots fueron recalculados después de cambiar reglas de bandeja/cerrabilidad.

> **Regla critica:** ningun campo del snapshot se edita manualmente. Todo es proyeccion regenerable desde `fal_acta` + `fal_acta_evento` + satelites.

---

### `fal_acta_evidencia`

**Proposito:** Evidencias digitales (fotos, videos) adjuntas al acta.
**Tipo:** hija | **Prioridad:** probable | **Estado:** casi segura
**Relaciones:** `fal_acta.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `tipo_evid` | `SMALLINT` | NO | — | Foto / video / documento |
| `storage_key` | `VARCHAR(255)` | NO | IDX | Referencia al storage externo |
| `nom_archivo` | `VARCHAR(120)` | SI | — | Nombre original del archivo |
| `mime_type` | `VARCHAR(80)` | SI | — | MIME type |
| `hash_evid` | `VARCHAR(128)` | SI | — | Hash de integridad |
| `orden_evid` | `SMALLINT` | NO | — | Orden de presentacion |
| `fh_captura` | `DATETIME(6)` | SI | — | Fecha de captura |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

---

### `fal_observacion`

**Proposito:** Observaciones polimorficas sobre cualquier entidad via discriminador.
**Tipo:** hija | **Prioridad:** probable | **Estado:** casi segura

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `id_ref` | `SMALLINT` | NO | IDX | Discriminador de entidad referenciada |
| `id_fk` | `BIGINT` | NO | IDX | FK logica a la entidad |
| `obs` | `VARCHAR(255)` | NO | — | Texto de la observacion |
| *(auditoria)* | `fh_alta`, `id_user` | NO | — | Alta estandar |

---

### `fal_documento`

**Proposito:** Pieza documental firmable (acta, notificacion, fallo, anexo).
**Tipo:** principal | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `num_talonario.id`, `stor_objeto.storage_key`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `tipo_docu` | `SMALLINT` | NO | IDX | Tipo documento (8 vals) |
| `estado_docu` | `SMALLINT` | NO | IDX | EMITIDO / PENDIENTE_FIRMA / FIRMADO / ADJUNTO |
| `nro_docu` | `VARCHAR(30)` | SI | IDX | Numero administrativo |
| `nro_docu` | `VARCHAR(30)` | SI | IDX | Numero administrativo |
>> CAMBIO: cambiar a `VARCHAR(20)`.
>> MOT: el número de documento se obtiene desde talonarios de numeración, igual que el acta; con 20 caracteres alcanza.

| `id_talonario` | `BIGINT` | SI | FK | `num_talonario.id` |
| `tipo_firma_req` | `SMALLINT` | NO | — | Tipo de firma requerida |
| `storage_key` | `VARCHAR(255)` | SI | IDX | Referencia al archivo en storage |
| `hash_docu` | `VARCHAR(128)` | SI | — | Hash de integridad |
| `fh_generacion` | `DATETIME(6)` | SI | IDX | Fecha de generacion |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |
>> CAMBIO: aclarar que `id_user_alta` es auditoría técnica del registro, no auditoría funcional del acto documental.
>> MOT: el usuario que generó el documento debe quedar en `fal_acta_evento.id_user_evt` del evento `DOCUMENTO_GENERADO`, vinculado por `id_docu_rel`.

---

### `fal_acta_documento`

**Proposito:** Relacion acta-documento con rol funcional.
**Tipo:** hija | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `fal_acta.id`, `fal_documento.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK comp. + FK | `fal_acta.id` |
| `documento_id` | `BIGINT` | NO | PK comp. + FK | `fal_documento.id` |
| `rol_docu_acta` | `SMALLINT` | NO | IDX | Rol funcional (7 vals) |
| `si_principal` | `BOOLEAN` | NO | — | Documento principal del acta |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

---

### `fal_documento_firma`

**Proposito:** Registro de firma individual por pieza documental.
**Tipo:** hija | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `fal_documento.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `documento_id` | `BIGINT` | NO | FK+IDX | `fal_documento.id` |
| `tipo_firma` | `SMALLINT` | NO | — | Tipo de firma aplicada |
| `estado_firma` | `SMALLINT` | NO | IDX | Estado del proceso de firma |
| `id_user_firma` | `CHAR(36)` | SI | — | Usuario firmante |
| `fh_solicitud` | `DATETIME(6)` | SI | — | Fecha de solicitud |
| `fh_firma` | `DATETIME(6)` | SI | — | Fecha efectiva de firma |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

---

### `fal_notificacion`

**Proposito:** Proceso de notificacion al infractor con canal, estado y acuse.
**Tipo:** principal | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `fal_acta.id`, `fal_documento.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `documento_id` | `BIGINT` | SI | FK+IDX | `fal_documento.id` |
| `tipo_notif` | `SMALLINT` | NO | IDX | Tipo (4 vals) |
>> CAMBIO: reemplazar "Tipo (4 vals)" por catálogo funcional del prototipo validado.
>> MOT: la fuente de verdad funcional es el prototipo validado; documentar valores legibles, no solo cantidad.

| `estado_notif` | `SMALLINT` | NO | IDX | Estado (9 vals) |
>> CAMBIO: reemplazar "Estado (9 vals)" por catálogo funcional del prototipo validado.
>> MOT: preservar estados reales del circuito de notificación validado.

| `canal_notif` | `SMALLINT` | NO | IDX | Canal (7 vals) |
>> CAMBIO: reemplazar "Canal (7 vals)" por catálogo funcional del prototipo validado.
>> MOT: incluir explícitamente `PORTAL_INFRACTOR`; el DDL histórico no debe limitar el modelo real.

| `si_requiere_acuse` | `BOOLEAN` | NO | — | Flag requiere acuse |
| `si_acuse_recibido` | `BOOLEAN` | NO | IDX | Flag acuse ya recibido |
| `fh_generacion` | `DATETIME(6)` | NO | IDX | Fecha de generacion |
| `fh_emision` | `DATETIME(6)` | SI | IDX | Fecha de emision |
| `destinatario_nombre` | `VARCHAR(120)` | SI | — | Nombre del destinatario |
| `domicilio_texto` | `VARCHAR(255)` | SI | — | Domicilio texto libre |
| `referencia_externa` | `VARCHAR(120)` | SI | — | Ref. en sistema externo |
| `lote_id` | `VARCHAR(50)` | SI | IDX | ID lote postal (placeholder; FK si se crea tabla) |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

>> CAMBIO: agregar nota: los catálogos de `tipo_notif`, `estado_notif` y `canal_notif` deben definirse desde el prototipo validado y exponerse como strings legibles en API/QA.
>> MOT: en base pueden ser `SMALLINT`, pero los contratos/control deben usar nombres entendibles.

## Ajuste en `fal_notificacion`

| Campo | Tipo MySQL | Null | Clave/índice | Descripción |
|---|---:|---:|---|---|
| `persona_destinatario_id` | `BIGINT` | NO | FK+IDX | Persona destinataria de la notificación |
| `domicilio_notif_id` | `BIGINT` | SI | FK+IDX | Domicilio seleccionado para la notificación, si el canal lo requiere |
| `destinatario_snapshot` | `VARCHAR(160)` | SI | — | Nombre/razón social al momento de generar la notificación |
| `domicilio_snapshot` | `VARCHAR(255)` | SI | — | Texto del domicilio al momento de generar la notificación |

Reglas:
- `persona_destinatario_id` normalmente será la misma persona infractora del acta.
- `domicilio_notif_id` se usa para canales postales/municipales/presenciales cuando corresponda.
- Los snapshots preservan lo efectivamente emitido aunque luego cambien los datos de persona/domicilio.

| `destinatario_nombre` | `VARCHAR(120)` | SI | — | Nombre del destinatario |
>> CAMBIO: reemplazar por `persona_destinatario_id BIGINT NOT NULL` + `destinatario_snapshot VARCHAR(160) NULL`.
>> MOT: la notificación debe apuntar a la persona destinataria, pero conservar el nombre emitido como snapshot histórico.

| `domicilio_texto` | `VARCHAR(255)` | SI | — | Domicilio en texto libre |
>> CAMBIO: reemplazar por `domicilio_notif_id BIGINT NULL` + `domicilio_snapshot VARCHAR(255) NULL`.
>> MOT: el domicilio debe ser una referencia estructurada si aplica; el texto queda como snapshot histórico de lo emitido.

---

### `fal_notificacion_intento` @FIX

**Proposito:** Intento individual de notificacion por canal.
**Tipo:** hija | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `fal_notificacion.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `notificacion_id` | `BIGINT` | NO | FK+IDX | `fal_notificacion.id` |
| `nro_intento` | `SMALLINT` | NO | — | Numero de intento |
| `canal_notif` | `SMALLINT` | NO | IDX | Canal utilizado |
| `dest_notif` | `VARCHAR(255)` | SI | — | Direccion / email / domicilio destino |
| `estado_intento` | `SMALLINT` | NO | IDX | Estado (7 vals) |
| `fh_intento` | `DATETIME(6)` | NO | IDX | Fecha del intento |
| `resultado_intento` | `SMALLINT` | SI | IDX | Resultado del intento |
| `referencia_externa` | `VARCHAR(120)` | SI | — | Ref. en sistema de envio externo |
| `lote_id` | `VARCHAR(50)` | SI | IDX | ID lote postal (placeholder) |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

## Ajuste en `fal_notificacion_intento`

| Campo | Tipo MySQL | Null | Clave/índice | Descripción |
|---|---:|---:|---|---|
| `domicilio_notif_id` | `BIGINT` | SI | FK+IDX | Domicilio usado en este intento |
| `destino_snapshot` | `VARCHAR(255)` | SI | — | Texto exacto del destino usado en este intento |
| `id_lote_notif` | `BIGINT` | SI | FK+IDX | Lote de notificación/correo si aplica |

Reglas:
- Si el intento fue postal/municipal/presencial con domicilio, `domicilio_notif_id` apunta al domicilio efectivamente usado.
- Si se descubre un nuevo domicilio, se da de alta en `fal_persona_domicilio` y el reintento apunta a ese nuevo domicilio.
- `destino_snapshot` conserva el texto exacto enviado.
- Si el intento no pertenece a lote, `id_lote_notif` queda `NULL`.

| `dest_notif` | `VARCHAR(255)` | SI | — | Direccion/email/domicilio de destino |
>> CAMBIO: reemplazar por `domicilio_notif_id BIGINT NULL` y `destino_snapshot VARCHAR(255) NULL`.
>> MOT: el intento debe referenciar el domicilio efectivamente utilizado; el texto queda como evidencia histórica del destino usado.

| `lote_id` | `VARCHAR(50)` | SI | IDX | ID del lote postal asociado |
>> CAMBIO: reemplazar por `id_lote_notif BIGINT NULL`.
>> MOT: debe ser FK al lote real. Si no pertenece a lote, se usa `NULL`, no `0`.

---

### `fal_notificacion_acuse`

**Proposito:** Acuse de recibo de notificacion.
**Tipo:** hija | **Prioridad:** obligatoria | **Estado:** casi segura
**Relaciones:** `fal_notificacion.id`, `fal_notificacion_intento.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `notificacion_id` | `BIGINT` | NO | FK+IDX | `fal_notificacion.id` |
| `intento_id` | `BIGINT` | SI | FK | `fal_notificacion_intento.id` |
| `tipo_acuse` | `SMALLINT` | NO | — | Tipo de acuse (6 vals) |
| `estado_acuse` | `SMALLINT` | NO | IDX | Estado del acuse (5 vals) |
| `fh_acuse` | `DATETIME(6)` | NO | IDX | Fecha del acuse |
| `storage_key_acuse` | `VARCHAR(255)` | SI | — | Constancia documental del acuse |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

---

### `fal_acta_pago_voluntario`

**Proposito:** Circuito de pago voluntario previo a fallo con historial de intentos.
**Tipo:** hija | **Prioridad:** probable | **Estado:** requiere veredicto
**Relaciones:** `fal_acta.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `situacion_pago` | `SMALLINT` | NO | IDX | SIN_PAGO / SOLICITADO / CONFIRMADO / VENCIDO / ... |
| `monto_pago_voluntario` | `DECIMAL(14,2)` | SI | — | Monto fijado por operador |
| `fecha_solicitud` | `DATETIME(6)` | SI | IDX | Fecha de solicitud |
| `fecha_vencimiento` | `DATE` | SI | IDX | Vencimiento del plazo |
| `fecha_confirmacion` | `DATETIME(6)` | SI | — | Fecha de confirmacion |
| `comprobante_ref` | `VARCHAR(100)` | SI | — | Ref. comprobante de pago |
| `tipo_comprobante` | `SMALLINT` | SI | — | Tipo de comprobante |
| `canal_solicitud` | `SMALLINT` | SI | — | Canal (INTERNO / PORTAL) |
| `observacion` | `TEXT` | SI | — | Observacion del operador |
| *(auditoria)* | `fh_alta`, `id_user_alta`, `id_user_solicitud` | NO/SI | — | Alta estandar |

---

### `fal_acta_pago_condena`

**Proposito:** Pago de condena firme post-fallo. Circuito separado del pago voluntario.
**Tipo:** hija | **Prioridad:** probable | **Estado:** requiere veredicto (casi obligatoria)
**Relaciones:** `fal_acta.id`, `fal_acta_fallo.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `fallo_id` | `BIGINT` | SI | FK | `fal_acta_fallo.id` |
| `situacion_pago_condena` | `SMALLINT` | NO | IDX | NO_APLICA / PENDIENTE / CONFIRMADO / ... |
| `monto_condena` | `DECIMAL(14,2)` | SI | — | Monto fijado al dictar fallo |
| `fecha_informe` | `DATETIME(6)` | SI | — | Fecha informe de pago |
| `fecha_confirmacion` | `DATETIME(6)` | SI | — | Habilita cierre del expediente |
| `comprobante_ref` | `VARCHAR(100)` | SI | — | Ref. comprobante de pago |
| `consentimiento_registrado` | `BOOLEAN` | NO | — | Infractor consintio (portal) |
| `canal_informado` | `SMALLINT` | SI | — | Canal del informe |
| `observacion` | `TEXT` | SI | — | Observacion del operador |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

---

### `fal_acta_fallo`

**Proposito:** Decision juridica del expediente con monto y consecuencias.
**Tipo:** hija | **Prioridad:** probable | **Estado:** requiere veredicto
**Relaciones:** `fal_acta.id`, `fal_documento.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `tipo_fallo` | `SMALLINT` | NO | IDX | ABSOLUTORIO=1 / CONDENATORIO=2 |
| `monto_condena` | `DECIMAL(14,2)` | SI | — | Solo si condenatorio |
| `fecha_dictado` | `DATETIME(6)` | NO | IDX | Fecha y hora del fallo |
| `documento_id` | `BIGINT` | SI | FK | `fal_documento.id` (TipoDocu=4) |
| `fh_vto_apelacion` | `DATE` | SI | IDX | Vencimiento plazo apelacion |
| *(auditoria)* | `fh_alta`, `id_user_alta`, `id_user_dictado` | NO | — | Alta estandar |

> **Veredicto pendiente:** A (solo doc TipoDocu=4) / B (tabla propia) / C hibrido (recomendado).

---

### `fal_acta_apelacion`

**Proposito:** Apelacion del infractor con canal, estado y resultado de resolucion.
**Tipo:** hija | **Prioridad:** probable | **Estado:** requiere veredicto
**Relaciones:** `fal_acta.id`, `fal_acta_fallo.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `fallo_id` | `BIGINT` | SI | FK | `fal_acta_fallo.id` |
| `canal` | `SMALLINT` | NO | — | PRESENCIAL=1 / PORTAL=2 |
| `fecha_registro` | `DATETIME(6)` | NO | IDX | Fecha de registro |
| `estado` | `SMALLINT` | NO | IDX | PRESENTADA=1 / RESUELTA=2 |
| `resultado_resolucion` | `SMALLINT` | SI | — | RECHAZADA=1 / ACEPTADA_ABSUELVE=2 |
| `fecha_resolucion` | `DATETIME(6)` | SI | — | Fecha de resolucion |
| *(auditoria)* | `fh_alta`, `id_user_alta`, `id_user_resolucion` | NO/SI | — | Alta estandar |

---

### `fal_acta_bloqueante_cierre_material`

**Proposito:** Tres origenes de bloqueo de cierre (medida preventiva / rodado / documentacion) con dos pasos cada uno.
**Tipo:** hija | **Prioridad:** probable | **Estado:** requiere veredicto
**Relaciones:** `fal_acta.id`, `fal_documento.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `origen` | `SMALLINT` | NO | IDX | MED_PREV=1 / RODADO=2 / DOC_RET=3 |
| `estado_resolucion_documental` | `SMALLINT` | NO | IDX | PENDIENTE=1 / RESUELTO=2 |
| `estado_cumplimiento_material` | `SMALLINT` | NO | IDX | PENDIENTE=1 / CUMPLIDO=2 |
| `id_documento_resolvent` | `BIGINT` | SI | FK | `fal_documento.id` |
| `fecha_constatacion` | `DATETIME(6)` | NO | — | Fecha del hecho bloqueante |
| `fecha_resolucion_documental` | `DATETIME(6)` | SI | — | Fecha resolucion documental |
| `fecha_cumplimiento_material` | `DATETIME(6)` | SI | — | Fecha cumplimiento material |
| `observacion` | `TEXT` | SI | — | Descripcion del bloqueo |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — | Alta estandar |

> **Regla:** cierre requiere ambos pasos resueltos para cada bloqueante activo.

---

### `fal_acta_paralizacion`

**Proposito:** Historial de paralizaciones con motivo y contexto de bandeja previa.
**Tipo:** historica | **Prioridad:** probable | **Estado:** requiere veredicto
**Relaciones:** `fal_acta.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `motivo_paralizacion` | `SMALLINT` | NO | IDX | 5 vals (espera documental, externa, etc.) |
| `observacion` | `TEXT` | SI | — | Texto libre |
| `fecha_paralizacion` | `DATETIME(6)` | NO | IDX | Fecha de la paralizacion |
| `bandeja_previa` | `VARCHAR(50)` | SI | — | Bandeja antes de paralizar |
| `fecha_reactivacion` | `DATETIME(6)` | SI | — | NULL si aun paralizada |
| *(auditoria)* | `fh_alta`, `id_user_alta`, `id_user_paralizacion`, `id_user_reactivacion` | — | — | Trazabilidad |

> `fal_acta_snapshot.motivo_paralizacion_act` deriva del registro activo (sin `fecha_reactivacion`).

---

### `fal_acta_archivo`

**Proposito:** Historial de archivos con causales estructuradas y soporte de reingreso.
**Tipo:** historica | **Prioridad:** probable | **Estado:** requiere veredicto
**Relaciones:** `fal_acta.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `motivo_archivo` | `SMALLINT` | NO | IDX | Motivo (alinear DDL 5 vals vs prototipo 3) |
| `permite_reingreso` | `BOOLEAN` | NO | — | Nulidad siempre permite |
| `causal_bloqueante_med_prev` | `BOOLEAN` | NO | — | **Gap D11** — medida preventiva |
| `causal_bloqueante_rodado` | `BOOLEAN` | NO | — | **Gap D11** — rodado secuestrado |
| `causal_bloqueante_doc_ret` | `BOOLEAN` | NO | — | **Gap D11** — documentacion retenida |
| `fecha_archivo` | `DATETIME(6)` | NO | IDX | Fecha del archivo |
| `fecha_reingreso` | `DATETIME(6)` | SI | — | NULL si no reingresado |
| `observacion` | `TEXT` | SI | — | Observacion del archivo |
| *(auditoria)* | `fh_alta`, `id_user_alta`, `id_user_archivo`, `id_user_reingreso` | — | — | Trazabilidad |

---

### `fal_acta_gestion_externa`

**Proposito:** Derivaciones a gestion externa (apremio, juzgado de paz) con historial.
**Tipo:** hija | **Prioridad:** probable | **Estado:** requiere veredicto
**Relaciones:** `fal_acta.id`

| Campo | Tipo MySQL | Null | Clave/indice | Uso |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | `fal_acta.id` |
| `tipo` | `SMALLINT` | NO | IDX | APREMIO=1 / JUZGADO_DE_PAZ=2 |
| `fecha_derivacion` | `DATETIME(6)` | NO | IDX | Fecha de la derivacion |
| `estado` | `SMALLINT` | NO | IDX | ACTIVA=1 / CERRADA=2 |
| `resultado_externo` | `SMALLINT` | SI | — | MODIFICA_MONTO=1 / ABSUELVE=2 |
| `monto_sugerido_post_gestion` | `DECIMAL(14,2)` | SI | — | Solo si MODIFICA_MONTO |
| `fecha_reingreso` | `DATETIME(6)` | SI | — | Fecha de reingreso |
| `observacion` | `TEXT` | SI | — | Observacion |
| *(auditoria)* | `fh_alta`, `id_user_alta`, `id_user_derivacion`, `id_user_reingreso` | — | — | Trazabilidad |

---

### `fal_lote_correo` *(diferible)*

**Proposito:** Agrupacion de envios postales. Diferir hasta integracion postal real definida.
**Tipo:** integracion | **Prioridad:** diferible | **Estado:** diferible

Mientras no exista: usar `lote_id VARCHAR(50)` en `fal_notificacion` e `fal_notificacion_intento` como placeholder.

| Campo | Tipo MySQL | Null | Uso |
|---|---|---|---|
| `id` | `BIGINT` | NO | PK |
| `lote_codigo` | `VARCHAR(50)` | NO (UNIQUE) | Codigo unico del lote |
| `fecha_generacion` | `DATETIME(6)` | NO | — |
| `estado` | `SMALLINT` | NO | ACTIVO=1 / ANULADO=2 |
| `cantidad_items` | `SMALLINT` | NO | — |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — |

---

### `fal_acta_qr_acceso` *(diferible)*

**Proposito:** Trazabilidad de accesos via QR al portal. Diferir hasta diseno del mecanismo QR.
**Tipo:** integracion | **Prioridad:** diferible | **Estado:** diferible

Mientras no exista: campo `codigo_qr VARCHAR(64) UNIQUE` en `fal_acta` o `fal_documento`.

| Campo | Tipo MySQL | Null | Uso |
|---|---|---|---|
| `id` | `BIGINT` | NO | PK |
| `acta_id` | `BIGINT` | NO | FK+IDX `fal_acta.id` |
| `codigo_qr` | `VARCHAR(64)` | NO (UNIQUE) | Codigo QR |
| `estado` | `SMALLINT` | NO | ACTIVO=1 / EXPIRADO=2 |
| `conteo_accesos` | `INT` | NO | Cantidad de accesos |
| *(auditoria)* | `fh_alta`, `id_user_alta` | NO | — |

---

### Satelites de acta por tipo

Incluir segun `tipo_acta`. Una acta puede tener 0..1 satelite de cada tipo. Prioridad diferible hasta implementar ese tipo de acta.

| Tabla | Proposito | FK a `fal_acta` | Campos principales |
|---|---|---|---|
| `fal_acta_transito` | Datos especificos de acta de transito | `acta_id` | `tipo_via`, `nom_via`, `lugar_txt`, campos de licencia |
| `fal_acta_vehiculo` | Datos del vehiculo infractor | `acta_id` | `dominio`, `marca`, `modelo`, `anio`, `id_tipo_vehiculo` |
| `fal_acta_contravencion` | Datos contravencionales | `acta_id` | `tipo_contravencion`, `descripcion` |
| `fal_acta_alcoholemia` | Datos de test de alcoholemia | `acta_id` | `valor_alcoholemia`, `tipo_instrumento`, `resultado` |
| `fal_medida_preventiva` | Catalogo de tipos de medida preventiva | — (catalogo) | `id`, `cod_med_prev`, `descripcion`, `si_activa` |
| `fal_acta_medida_preventiva` | Medidas preventivas aplicadas al acta | `acta_id`, `id_med_prev → fal_medida_preventiva.id` | `estado`, `fh_aplicacion`, `fh_levantamiento` |

---

### Catalogos y referenciales

| Tabla | Tipo | Campos clave |
|---|---|---|
| `fal_dependencia` | catalogo | `id BIGINT`, `cod_dep VARCHAR(20)`, `nom_dep VARCHAR(120)`, `id_dep_padre BIGINT`, `si_activa BOOLEAN` |
| `fal_dependencia_version` | historica | PK `(id_dep, ver_dep)`, `nom_dep`, `fh_vig_desde DATE`, `fh_vig_hasta DATE`, `si_activa` |
| `fal_inspector` | catalogo | `id BIGINT`, `id_user BIGINT`, `legajo_insp INT`, `nom_insp VARCHAR(120)`, `si_activo BOOLEAN` |
| `fal_inspector_version` | historica | PK `(id_insp, ver_insp)`, `legajo_insp`, `nom_insp`, `id_dep BIGINT`, `fh_vig_desde`, `fh_vig_hasta` |
| `fal_normativa_faltas` | catalogo | `id`, `codigo`, `descripcion`, `fh_vigencia_desde DATE`, `si_activa BOOLEAN` |
| `fal_articulo_normativa_faltas` | catalogo | `id`, `normativa_id FK`, `nro_articulo`, `descripcion`, `unidades_multa DECIMAL(14,2)`, `si_activo` |
| `fal_tarifario_unidad_faltas` | catalogo | `id`, `valor_unidad DECIMAL(14,2)`, `fh_desde DATE`, `fh_hasta DATE` |
| `fal_acta_articulo_infringido` | hija | `id`, `acta_id FK`, `articulo_id FK`, `cantidad_unidades DECIMAL(14,2)` |
| `fal_acta_articulo_auditoria` | historica | `id`, `acta_id FK`, `articulo_id FK`, `accion SMALLINT`, `fh_evt DATETIME(6)`, `id_user CHAR(36)` |
| `num_politica` | catalogo | `id`, `cod_politica`, `descripcion`, `tipo_objeto`, `prefijo` |
| `num_talonario` | catalogo | `id`, `politica_id FK`, `nro_desde`, `nro_hasta`, `nro_actual`, `estado SMALLINT`, `fh_desde DATE` |
| `num_talonario_dependencia` | hija | PK `(talonario_id, id_dep)`, `fh_asignacion`, `si_activo BOOLEAN` |
| `num_talonario_inspector` | hija | PK `(talonario_id, id_insp)`, `fh_asignacion`, `si_activo BOOLEAN` |
| `num_talonario_movimiento` | historica | `id`, `talonario_id FK`, `nro_asignado`, `tipo_movimiento SMALLINT`, `fh_mov DATETIME(6)` |
| `stor_backend` | catalogo | `id`, `nombre`, `tipo_backend SMALLINT`, `url_base VARCHAR(255)`, `si_activo BOOLEAN` |
| `stor_politica` | catalogo | `id`, `sistema VARCHAR(20)`, `familia VARCHAR(30)`, `tipo_objeto VARCHAR(30)`, `id_backend FK`, `bucket VARCHAR(60)` |
| `stor_objeto` | integracion | PK natural `storage_key VARCHAR(64)`, `id_storage_backend INT`, `sistema`, `familia`, `tipo_objeto`, `bucket`, `nom_archivo`, `mime_type`, `tam_bytes BIGINT`, `hash_archivo VARCHAR(128)`, `estado_storage SMALLINT` |

---

## 5. Tablas obligatorias primera version

| Tabla | Por que es obligatoria |
|---|---|
| `fal_acta` | Agregado raiz; sin ella no hay expediente |
| `fal_acta_evento` | Fuente de verdad del historial; append-only |
| `fal_acta_snapshot` | Cache operativo para bandejas y filtros |
| `fal_documento` | Soporte documental de todo el circuito |
| `fal_acta_documento` | Vinculacion acta-documento con rol |
| `fal_documento_firma` | Firma digital del circuito documental |
| `fal_notificacion` | Sin notificacion no hay circuito operativo |
| `fal_notificacion_intento` | Historial de intentos por canal |
| `fal_notificacion_acuse` | Acuse de recibo requerido por dominio |
| `fal_dependencia` | Referencial organizacional obligatorio |
| `fal_dependencia_version` | Historial de cambios de dependencia |
| `fal_inspector` | Referencial de inspector |
| `fal_inspector_version` | Historial de cambios de inspector |
| `num_politica` | Politica de numeracion de talonarios |
| `num_talonario` | Talonario para numeracion de actas |
| `num_talonario_dependencia` | Asignacion talonario-dependencia |
| `num_talonario_inspector` | Asignacion talonario-inspector |
| `num_talonario_movimiento` | Movimientos de uso de talonario |
| `stor_backend` | Backend de storage documental |
| `stor_politica` | Politica de storage por tipo de objeto |
| `stor_objeto` | Objeto fisico almacenado en storage |

---

## 6. Tablas con veredicto pendiente

| Tabla | Decision pendiente | Recomendacion actual | Impacto |
|---|---|---|---|
| `fal_acta_evento` | Estrategia TipoEvt: ampliar SMALLINT / catalogo tabla / hibrido+payload | Hibrido C (~35 tipos + `payload_json`) | Alto — bloquea DDL de acta_evento |
| `fal_acta_fallo` | Tabla propia vs doc enriquecido vs hibrido | Hibrido C: doc firma + entidad fallo | Alto — afecta firma, notif, pago condena |
| `fal_acta_pago_voluntario` | Tabla propia vs campos en acta/snapshot | Tabla propia (Opcion B) | Alto — bandeja D3, cerrabilidad |
| `fal_acta_pago_condena` | Tabla separada vs campos en acta | Tabla separada — casi obligatoria | Alto — separacion de circuitos es regla de dominio |
| `fal_acta_apelacion` | Tabla propia vs solo evento TipoEvt=13 | Tabla propia (Opcion B) | Medio — bandeja D8, condena firme |
| `fal_acta_gestion_externa` | Tabla propia vs flags en snapshot | Tabla propia si historial N es requisito | Medio — bandeja D9, resultado externo |
| `fal_acta_paralizacion` | Tabla historica + campo snapshot vs solo snapshot | Ambas — gap critico D10 | Alto — motivo no persiste sin tabla |
| `fal_acta_archivo` | Tabla historica + campos acta vs solo acta | Tabla historica + campos acta (Opcion B) | Medio — historial N archivos, reingreso |
| `fal_acta_bloqueante_cierre_material` | Tabla propia vs extension medida preventiva | Tabla propia (Opcion B) | Alto — gap D11/D12, flags snapshot |
| `accion_pendiente` (campo snapshot) | Cache derivado en snapshot vs campo en acta vs calculado | Cache derivado en snapshot (Opcion C) | Medio — sub-bandejas operativas |
| Charset/collation | `utf8mb4_unicode_ci` vs `utf8mb4_0900_ai_ci` | `utf8mb4_0900_ai_ci` (MySQL 8.0+) | Global — definir antes de crear tablas |
| Estrategia PKs | `BIGINT AUTO_INCREMENT` vs UUID vs ULID | `BIGINT AUTO_INCREMENT` por tabla | Global — todos los DDL |

---

## 7. Tablas diferibles

| Tabla | Motivo para diferir | Condicion para retomar |
|---|---|---|
| `fal_lote_correo` | Sin integracion postal real; `lote_id VARCHAR` alcanza como placeholder | Cuando haya integracion con sistema postal externo real |
| `fal_acta_qr_acceso` | Mecanismo QR pendiente de diseno; campo en acta o doc alcanza | Cuando se defina generacion y trazabilidad de accesos |
| Satelites por tipo de acta | Dependen de `tipo_acta` implementado; innecesarios en MVP general | Al implementar cada tipo de acta en produccion |

---

*Documento generado: Jun 2026. Sin cambios de codigo. Sin modificacion de specs. Sin DDL fisico. Propuesta revisable.*
