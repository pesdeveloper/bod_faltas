# Operación del bus de eventos municipal y autenticación M2M (`DEC-EDA-018`/`019`/`027`)

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Valores operativos concretos, mecanismo de autenticación
> servicio-a-servicio para la integración EDA Faltas↔Firmas y transporte
> físico del bus central. Complementa la arquitectura conceptual de
> `municipal-eventing.md` y los contratos de `../40-api/firma-event-contracts.md`
> y `firma-binary-transfer.md`. `IMPLEMENTATION_NOT_STARTED`.

## 1. Autenticación M2M — únicamente HTTP (`DEC-EDA-018`/`027`)

**Decisión única, sin alternativa:** `X-Api-Key` autentica exclusivamente
los tres endpoints HTTP de transferencia binaria Faltas↔Firmas
(`firma-binary-transfer.md`). El bus central de eventos (`bus_evento`,
única tabla) **no es una API HTTP**: es una tabla MariaDB accedida por
JDBC/SQL con credenciales de base de datos de mínimo privilegio (§2bis).
`X-Api-Key` nunca autentica una operación sobre el bus.

```http
X-Api-Key: <credencial de aplicación>
```

**Reglas (alcance: los tres endpoints HTTP de `firma-binary-transfer.md`):**

- una credencial distinta por bounded context y por ambiente (Faltas-prod,
  Faltas-staging, Firmas-prod, Firmas-staging, etc.);
- Faltas y Firmas nunca comparten clave;
- permisos por operación (matriz en §2);
- los secretos nunca se versionan en el repositorio; se resuelven desde
  configuración externa o secret manager de la plataforma
  (`faltas.integraciones.firmas.api-key`, análogo al patrón ya usado por
  credenciales JDBC/JWKS de este backend);
- almacenamiento cifrado o secret manager de la plataforma, nunca en texto
  plano en `application.yml`;
- rotación con solapamiento controlado (dos claves válidas simultáneamente
  durante la ventana de rotación);
- se persiste únicamente el fingerprint (hash) de la clave usada en cada
  llamada, nunca la clave completa (`actor_tecnico_key_fingerprint`,
  `DEC-EDA-021`);
- la clave nunca se registra en logs;
- rate limiting por credencial;
- revocación inmediata (invalidar la clave sin esperar expiración natural);
- HTTPS obligatorio en todos los endpoints de esta integración;
- el subject técnico (`actor_tecnico_app`, `actor_tecnico_id`) se resuelve
  desde la credencial, nunca desde un campo de negocio;
- la identidad humana del firmante (`firmanteEfectivo.idUser`) viaja
  separada en el contrato de evento/HTTP firmado por Firmas; nunca se
  deriva de `X-Api-Key`.

## 2. Matriz de permisos HTTP por operación (alcance: transferencia binaria)

| Operación | Faltas | Firmas |
|---|---|---|
| `POST preparar-artefacto`, `GET artefacto` | Expone | Invoca |
| `PUT resultados/{resultadoFirmaId}` | Expone | Invoca |
| Endpoints administrativos de Faltas ajenos a esta integración | N/A | Sin acceso (credencial de integración no habilita administración) |

Esta matriz cubre exclusivamente los tres endpoints HTTP. La publicación y
el consumo de los 12 eventos de dominio (`FirmaSolicitada`,
`SolicitudFirmaAceptada`, etc.) nunca pasan por HTTP ni por `X-Api-Key`:
ocurren siempre a través del bus central por JDBC/SQL (§2bis).

## 2bis. Transporte y seguridad del bus central: JDBC/SQL directo (`DEC-EDA-027`)

**Arquitectura:**

- cada aplicación (Faltas, Firmas) escribe su Outbox en su propia base de
  datos local (`fal_outbox`/`fir_outbox`), en la misma transacción que el
  cambio de dominio que la origina;
- cada relay lee su Outbox local con el datasource/usuario de aplicación
  ya existente (no una cuenta `bus_*`);
- el relay publica en la base central del bus (`bus_evento`, hoy en
  `sb_faltas_db`) mediante un **datasource JDBC dedicado**, autenticado con
  un usuario MariaDB de mínimo privilegio (`bus_publisher_faltas` o
  `bus_publisher_firmas`), ejecutando un `INSERT` directo sobre
  `bus_evento` (`../50-persistence/firma-eda-physical-model.md` §10 y
  `database/ddl/grants-firmas-eda-bus.sql`) — reutilizando el mismo
  `evento_id` en cada reintento (idempotencia por `UNIQUE(evento_id)`, sin
  fingerprint calculado en SQL);
- cada aplicación consumidora lee `bus_evento` con su propio usuario
  (`bus_consumer_faltas`/`bus_consumer_firmas`), ejecutando un `SELECT`
  directo filtrado por `bus_evento_id > ultimo_evento_id` (su propio
  checkpoint local), copia los eventos relevantes a su Inbox local y
  avanza su checkpoint, todo en una transacción local — sin ningún
  `UPDATE`/`DELETE` sobre `bus_evento` desde estas cuentas;
- **no XA/2PC** en ningún punto: la atomicidad publicación↔dominio se
  logra exclusivamente vía Outbox local en la misma transacción del
  cambio de dominio (nunca una transacción distribuida entre la base local
  y la base central);
- duplicados resueltos por `UNIQUE(evento_id)` en `bus_evento` y por
  `PK(bus_evento_id)` en `fal_inbox`/`fir_inbox`, nunca por coordinación
  transaccional entre bases.

Sin rutinas almacenadas ni vistas intermediarias: cada aplicación ejecuta
sus propias sentencias SQL contra `bus_evento` con el grant mínimo que le
corresponde. Roles SQL exactos y permisos mínimos por cuenta:
`../50-persistence/firma-eda-physical-model.md` §10,
`database/ddl/grants-firmas-eda-bus.sql`.

## 3. Semántica de entrega (`DEC-EDA-019`)

- entrega `at-least-once`; todo consumidor debe ser idempotente por
  `bus_evento_id` (Inbox, `PK`);
- orden garantizado únicamente dentro de `agregado_codigo + agregado_id +
  agregado_version`; no se promete orden global entre agregados distintos;
- no 2PC, no XA: la atomicidad publicación↔dominio se logra exclusivamente
  vía Outbox local en la misma transacción del cambio de dominio;
- todos los timestamps del bus y de sus tablas se interpretan y persisten
  en UTC.

## 3bis. Dos transacciones locales independientes: ingestión y procesamiento (`DEC-EDA-019`)

**Regla única, sin alternativa:** la ingestión del lote (Inbox + avance de
checkpoint) y el procesamiento funcional de cada fila (efecto de dominio +
Outbox de respuesta + cierre del Inbox) son **dos transacciones locales
independientes**. Ningún error de un handler de dominio revierte ni
bloquea la ingestión del lote, y un error en una fila nunca impide
procesar las demás:

```text
Transacción 1 (ingestión, por lote):
  INSERT en Inbox de los eventos relevantes + UPDATE del checkpoint

Transacción 2 (procesamiento, por fila, una por cada fila de Inbox):
  efecto de dominio + Outbox de respuesta, si corresponde + Inbox.status = PROCESADO
```

### 3bis.1 Transacción de ingestión (lote completo, corta, sin efectos de dominio)

**Secuencia normativa:**

1. leer un lote de `bus_evento` (`bus_evento_id > ultimo_evento_id`,
   `ORDER BY bus_evento_id LIMIT :batchSize`, con `LOCK IN SHARE MODE`
   dentro de una transacción corta de solo lectura, §4), fuera de la
   transacción local de ingestión (solo lectura contra la base central);
2. identificar por `evento_codigo` cuáles eventos del lote interesan a la
   aplicación;
3. comenzar la transacción local de ingestión (base de datos propia del
   consumidor, ej. `sb_faltas_db` para Faltas);
4. insertar en `fal_inbox`/`fir_inbox` (`PK = bus_evento_id`) los eventos
   relevantes identificados en el paso 2, copiando el envelope completo
   (`evento_id`, `productor_codigo`, `schema_version`, `correlation_id`,
   `causation_id`, `ocurrido_en`; `../50-persistence/firma-eda-physical-model.md`
   §6.5), con `status = 0` (`PENDIENTE`), `recibido_en = UTC_TIMESTAMP(6)`;
   si la fila ya existe (relectura tras un crash), no-op — no se duplica
   ni se reejecuta ningún efecto;
5. avanzar `fal_bus_checkpoint`/`fir_bus_checkpoint.ultimo_evento_id` al
   último `bus_evento_id` examinado del lote (misma transacción local que
   el paso 4, para que nunca se avance el checkpoint antes de copiar el
   evento relevante);
6. commit de la transacción de ingestión.

Esta transacción **no** ejecuta ningún efecto de dominio, **no** crea
Outbox de respuesta y **no** marca ninguna fila `PROCESADO`: solo copia el
envelope/payload relevante y avanza el checkpoint. Si falla, hace
`rollback` completo — ni las filas del lote quedan en Inbox ni el
checkpoint avanza; el siguiente ciclo de lectura repite el mismo lote sin
pérdida ni duplicado.

### 3bis.2 Transacción de procesamiento por fila de Inbox

Cada fila `PENDIENTE`/`REINTENTO` de `fal_inbox`/`fir_inbox` se procesa en
su propia transacción local, independiente de la ingestión y de cualquier
otra fila:

1. bloquear la fila de Inbox por `bus_evento_id` (`SELECT ... FOR UPDATE`;
   la fila ya existe desde la ingestión — esta transacción nunca la crea);
2. validar versión/orden (`agregado_version`, §0.4 de
   `../40-api/firma-event-contracts.md`);
3. aplicar el efecto de dominio (ej. los 9 pasos de
   `firma-binary-transfer.md` §0bis para `DocumentoFirmado`); incluye el
   caso de rechazo funcional (ej. PKI inválida,
   `../10-domain/firma-notificacion-fallo.md` §11bis), que también cierra
   en `PROCESADO`, nunca en `REINTENTO`/`ERROR_DEFINITIVO`;
4. insertar Outbox de respuesta si corresponde (mismo commit que el
   paso 3);
5. marcar el Inbox `status = 2` (`PROCESADO`), `si_error = FALSE`,
   `procesado_en = UTC_TIMESTAMP(6)`;
6. commit de la transacción de procesamiento.

Si el efecto de dominio falla por un error técnico (no un rechazo
funcional):

1. `rollback` completo de esta transacción de procesamiento (el efecto de
   dominio no queda aplicado a medias, ni el Outbox de respuesta, ni el
   cierre del Inbox);
2. en una segunda transacción local breve e independiente: incrementar
   `attempts`, fijar `si_error = TRUE`, dejar `status` en `3`
   (`REINTENTO`) o `4` (`ERROR_DEFINITIVO`), e insertar la fila
   correspondiente en `fal_inbox_error`/`fir_inbox_error`
   (`../50-persistence/firma-eda-physical-model.md` §6.6).

Un error de procesamiento en una fila **nunca** revierte la transacción de
ingestión del lote ni impide procesar otras filas
`PENDIENTE`/`REINTENTO` del mismo lote o de lotes anteriores. Un rechazo
funcional de dominio (ej. PKI inválida) **no** es un error técnico: cierra
en `PROCESADO` con `si_error = FALSE` y **no** inserta fila en
`fal_inbox_error` (§0.5, `../10-domain/firma-notificacion-fallo.md`
§11bis).

## 4. Lector local: sin claim ni lease central, lectura con `LOCK IN SHARE MODE` (`DEC-EDA-019`)

**Versión real de destino:** MariaDB 12.3.2 (`sb_faltas_db`, ver
`../50-persistence/jdbc-strategy.md`).

**Decisión única:** no hay claim transaccional contra el bus central, no
hay lease, no hay `locked_by`/`locked_until` en `bus_evento`. Cada
aplicación tiene su propio lector local, que consulta `bus_evento` en una
transacción corta de solo lectura (`START TRANSACTION READ ONLY`) usando
su checkpoint local como cursor; no hay coordinación entre lectores de
distintas aplicaciones porque cada una lee la misma tabla central de forma
completamente independiente.

**Lectura obligatoria con `LOCK IN SHARE MODE`:**

```sql
START TRANSACTION READ ONLY;
SELECT bus_evento_id, evento_id, productor_codigo, evento_codigo,
       schema_version, agregado_codigo, agregado_id, agregado_version,
       correlation_id, causation_id, payload, ocurrido_en, publicado_en
FROM bus_evento
WHERE bus_evento_id > :ultimo_evento_id
ORDER BY bus_evento_id
LIMIT :batchSize
LOCK IN SHARE MODE;
COMMIT;
```

**Motivo:** dos publicaciones concurrentes pueden obtener `bus_evento_id`
consecutivos (ej. 5 y 6) y confirmar en orden inverso (la transacción del
id 6 hace `commit` antes que la del id 5, que sigue en curso). Un
`SELECT` de lectura consistente sin bloqueo nunca ve la fila 5 mientras
esa transacción no confirme, pero tampoco espera por ella: si el lector
toma la fila 6 y avanza el checkpoint hasta 6, la fila 5 queda
permanentemente salteada al confirmar más tarde (el lector nunca vuelve a
mirar `bus_evento_id <= 6`). `LOCK IN SHARE MODE` obliga al `SELECT` a
tomar un bloqueo compartido sobre cada fila que efectivamente lee: si esa
fila fue insertada por una transacción todavía no confirmada, InnoDB
bloquea el `SELECT` hasta que esa transacción confirma o revierte, en
lugar de saltarla silenciosamente. Esto mantiene la transacción de lectura
corta (solo espera si hay una inserción concurrente en curso dentro del
rango leído) y evita que el checkpoint avance más allá de un hueco todavía
no confirmado.

**Defaults:**

```text
poll interval: 5 segundos
batch size: 100
workers por aplicación: 1 (un único lector local avanza el checkpoint; no
    hay reparto de un mismo checkpoint entre múltiples workers)
```

## 5. Reintentos locales de Inbox (`DEC-EDA-019`)

Los reintentos ocurren únicamente dentro de la Inbox local de cada
aplicación, nunca contra el bus central (que no tiene estado de entrega
que reintentar):

```text
intentos máximos automáticos por fila de Inbox: 10
backoff: 30s, 2m, 10m, 30m, 2h, 6h, 12h, 24h, 24h, 24h
```

`backoff[attempts]` se indexa por el valor de `attempts` **después** de
incrementarlo en el fallo actual (el primer fallo deja `attempts = 1` y
usa `backoff[1] = 30s`; el décimo fallo deja `attempts = 10` y transiciona
`status = 4` (`ERROR_DEFINITIVO`), sin programar un `next_attempt_at`
número 11).

Una fila `ERROR_DEFINITIVO` de Inbox no se reintenta automáticamente; la
intervención humana auditable ocurre íntegramente sobre las tablas locales
del consumidor (`fal_inbox`/`fal_inbox_error`), nunca sobre `bus_evento`:
`bus_evento` es inmutable y no participa en la resolución de un fallo de
procesamiento local. Si corresponde reprocesar, la aplicación reinicia
`fal_inbox.status`/`attempts` de esa fila localmente (operación de
mantenimiento propia de cada aplicación, fuera del alcance de este bus).

`fal_outbox`/`fir_outbox` siguen el mismo esquema de reintentos, local al
productor: `status = 3` (`ERROR_DEFINITIVO`) tras el décimo intento
fallido de publicación. El reintento manual de una fila
`ERROR_DEFINITIVO` (intervención humana auditable, fuera del backoff
automático de 10 intentos) **conserva el mismo `evento_id` y el mismo
`payload`** de la fila original: reinicia `attempts`/`status`/
`next_attempt_at` para que el relay vuelva a intentar el mismo `INSERT`
sobre `bus_evento` con idéntica identidad lógica (`UNIQUE(evento_id)`,
§2bis); nunca reescribe el `payload` ni genera un `evento_id` distinto
para el mismo hecho de dominio ya ocurrido. Un **nuevo** `evento_id` solo
se genera cuando existe un **nuevo** hecho de dominio (una fila nueva de
Outbox), nunca como mecanismo de reintento de una fila existente.

## 6. Retención

```text
fal_outbox (status=PUBLICADO): 90 días
fal_inbox (status=PROCESADO): 365 días
bus_evento: 365 días
fal_inbox_error: igual retención que la fila de fal_inbox referenciada
Candidatos no consumidos (fal_documento_firma_resultado, EXPIRADO): 24 horas desde received_at
Storage anterior tras swap (fal_documento_firma_resultado, cleanup_status=COMPLETADO): inmediato tras confirmar la eliminación fisica
```

No existe `sp_bus_purgar` ni ningún procedimiento almacenado de purga
central: la retención de `bus_evento` es una tarea operativa externa y
simple, ejecutada por la cuenta `bus_maintenance` (única con `DELETE`
sobre `bus_evento`, `../50-persistence/firma-eda-physical-model.md` §10),
fuera de la ruta de publicación/consumo. El único criterio de purga es la
antigüedad de `publicado_en` frente a la ventana de retención anterior
(`publicado_en < UTC_TIMESTAMP() - INTERVAL 365 DAY`): `bus_maintenance`
no tiene ni necesita ningún privilegio sobre `fal_bus_checkpoint`/
`fir_bus_checkpoint` (tablas locales de cada aplicación consumidora, fuera
de su alcance de permisos, §10 de `firma-eda-physical-model.md`) — la
purga nunca consulta ni compara contra el progreso de lectura de ningún
consumidor.

Jobs de purga locales por aplicación: `PurgarOutboxPublicadoJob`,
`PurgarInboxProcesadoJob`, `PurgarCandidatosNoConsumidosJob`,
`PurgarStorageAnteriorJob` (nombres de diseño; implementación pendiente).
Todos son idempotentes (repetir la purga sobre filas ya purgadas no
produce error ni efecto adicional) y auditables (registran cantidad de
filas purgadas y rango de fechas). `PurgarCandidatosNoConsumidosJob` opera
sobre `fal_documento_firma_resultado`
(`../50-persistence/firma-eda-physical-model.md` §4): marca
`estado_resultado = EXPIRADO` y limpia `storage_key_candidato` de los
candidatos `VALIDADO` cuyo `expires_at` ya pasó; nunca toca filas
`CONSUMIDO`/`RECHAZADO` (ya terminales). `PurgarStorageAnteriorJob` es el
job de limpieza asíncrona del swap de storage
(`../50-persistence/firma-eda-physical-model.md` §5bis paso 7): elimina el
PDF referenciado por `storage_key_anterior` en filas `CONSUMIDO` con
`cleanup_status IN (PENDIENTE, RETRY)`, con el mismo backoff de 10
intentos que §5, y limpia `storage_key_anterior`/`storage_key_candidato`
al confirmar `COMPLETADO`. La evidencia jurídica de una firma **no
depende del bus**: se conserva en las tablas de dominio
(`fal_documento_firma`, `fal_documento_firma_req`) según la política
documental vigente, ajena a la retención del bus.

## 7. Reconciliación (`DEC-EDA-019`)

- **Incremental:** cada hora. Detecta eventos de `fal_outbox`/`fir_outbox`
  con `status = PENDIENTE`/`REINTENTO` cuyo `next_attempt_at` ya venció
  sin que exista todavía la fila correspondiente en `bus_evento` (huecos
  de publicación); alerta si persiste más de 15 minutos.
- **Completa:** diaria. Recorre `agregado_codigo + agregado_id` buscando
  huecos de `agregado_version` (versión saltada) y candidatos huérfanos en
  `fal_documento_firma_resultado` (`VALIDADO` sin consumo dentro del TTL de
  24 horas, ver §6).
- Ninguna reconciliación reejecuta efectos de dominio ya aplicados
  (idempotencia por `bus_evento_id`/`PK` en Inbox es la garantía de no
  repetición).
- Métricas y umbrales: `huecos_publicacion_detectados` (alerta si > 0
  durante más de 15 minutos), `filas_inbox_error_definitivo` (alerta si >
  0; requiere intervención local, nunca automática),
  `resultados_binarios_huerfanos` (alerta si > 0 tras la reconciliación
  diaria), `storage_cleanup_quarantined` (alerta si > 0; PDFs anteriores
  sin poder eliminarse tras 10 intentos, §5bis/§6).
- Diagnóstico técnico: endpoint interno
  `GET /internal/diagnostics/firmas-eda/reconciliacion` (diseño; no
  implementado), que expone el resultado de la última corrida de cada
  reconciliación sin datos personales ni secretos.

## 8. Observabilidad (`DEC-EDA-019`)

Campos obligatorios en toda fila de `fal_outbox`/`bus_evento`/`fal_inbox`
relevantes para diagnóstico:

`evento_id`, `evento_codigo`, `schema_version`, `productor_codigo`,
`ocurrido_en`, `correlation_id`, `causation_id`, `agregado_codigo`,
`agregado_id`, `agregado_version`, `attempts` (`fal_outbox`/`fal_inbox`),
`si_error` (`fal_outbox`/`fal_inbox`; boolean simple, sin código/detalle
en la fila principal). El detalle de cada fallo (código, resumen breve)
vive en `fal_inbox_error`, nunca en `bus_evento`: la tabla central no
tiene columnas de observabilidad de consumo, porque no registra estados de
consumo.

No se almacenan secretos ni el PDF en ningún `payload` de evento.
