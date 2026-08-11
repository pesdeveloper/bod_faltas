# Contratos de eventos de dominio — integración EDA Faltas ↔ Firmas

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Los 12 eventos de esta integración son `NORMATIVE` y `EVENT_CONTRACTS_COMPLETE = 12/12`.
> Transporte: `../30-integrations/municipal-eventing.md` (arquitectura) y
> `eventing-operations.md` (operación, `DEC-EDA-019`). Ownership, lifecycle,
> hashes y PKI: `../10-domain/firma-notificacion-fallo.md`. Modelo físico:
> `../50-persistence/firma-eda-physical-model.md` y
> `../50-persistence/mariadb-logical-model.md`. `IMPLEMENTATION_NOT_STARTED`:
> ningún consumidor/productor de estos eventos existe en código.

## 0. Políticas comunes a los 12 eventos

Estas políticas aplican uniformemente; cada evento (§1-§12) solo documenta
lo que le es específico.

### 0.1 Envelope

Todo evento usa el envelope de `../30-integrations/municipal-eventing.md` §4:
`eventoId` (UUID, clave de idempotencia de transporte), `schemaVersion`,
`tipoEvento`, `sistemaOrigen` (`faltas` | `firmas`), `fhEvento` (UTC),
`correlationId`, `causationId` (nullable), `agregado` (`{tipo, id}`),
`payload`.

### 0.2 Schema version inicial

Todos los eventos de este documento inician en `schemaVersion = 1`.

### 0.3 Idempotencia y duplicados (transporte)

Todo consumidor es idempotente por `evento_id` vía su Inbox
(`fal_inbox`/`fir_inbox`, `UNIQUE KEY uq_fal_inbox_evento_id (evento_id)` /
`uq_fir_inbox_evento_id`, identidad lógica estable copiada del envelope del
evento — distinta del cursor técnico `bus_evento_id`, que es la `PK`
operativa de la misma tabla; `../50-persistence/firma-eda-physical-model.md`
§6.5). Un duplicado detectado en el Inbox se descarta sin reejecutar el
efecto de dominio y sin error.

### 0.4 Orden y fuera de orden

Orden garantizado únicamente dentro de `aggregate_type + aggregate_id +
aggregate_version` (`../30-integrations/eventing-operations.md` §3). Un
evento que llega con `aggregateVersion` menor o igual a la ya aplicada para
ese agregado se descarta como obsoleto (no se reejecuta ni se reporta como
error, salvo la excepción explícita de la regla de carrera del §3 de este
documento). Un evento con `aggregateVersion` mayor a la esperada (hueco)
queda pendiente y se resuelve por reconciliación
(`eventing-operations.md` §7), nunca reordenando localmente sin evidencia.

### 0.5 Errores transitorios vs. permanentes

- **Transitorios** (reintentable automáticamente, política de backoff de
  `eventing-operations.md` §5): timeout de red, indisponibilidad temporal
  del consumidor, error de infraestructura, deadlock/lock wait.
- **Permanentes** (cierran el Inbox local directo en `status = 4`
  (`ERROR_DEFINITIVO`) sin agotar los 10 intentos automáticos de
  `eventing-operations.md` §5, o se resuelven con un evento explícito de
  rechazo de contrato — `SolicitudFirmaRechazadaPorContrato`, §4): violación
  de schema, campo obligatorio ausente, referencia a una entidad que no
  existe, hash/revisión inconsistente detectado antes de aplicar cualquier
  efecto de dominio. Un error permanente es siempre un fallo **técnico** de
  contrato (inserta fila en `fal_inbox_error`/`fir_inbox_error`); se
  distingue de un **rechazo funcional** de dominio (§0.5bis), que cierra en
  `PROCESADO` sin fila de error.

### 0.5bis Rechazo funcional de dominio vs. error técnico

Un rechazo funcional de dominio (ej. PKI inválida al procesar
`DocumentoFirmado`, §7bis) **no** es un error técnico: la fila de
`fal_inbox`/`fir_inbox` cierra en `status = PROCESADO`, `si_error = FALSE`,
y **no** se inserta ninguna fila en `fal_inbox_error`/`fir_inbox_error`
(`eventing-operations.md` §3bis.2,
`../10-domain/firma-notificacion-fallo.md` §11bis). El detalle del rechazo
funcional vive en la tabla de dominio afectada (ej.
`fal_documento_firma_resultado.last_error_code`/`last_error_detail`), nunca
en `fal_inbox_error` (esa tabla es exclusiva de fallos técnicos
reintentables de procesamiento).

### 0.6 Seguridad

Todo evento de dominio (§1-§12) se publica/consume exclusivamente por el
bus central `bus_evento` vía JDBC/SQL con credenciales de base de datos de
mínimo privilegio (`bus_publisher_*`/`bus_consumer_*`,
`eventing-operations.md` §2bis); ningún evento de dominio pasa por HTTP ni
por `X-Api-Key`. `X-Api-Key` autentica exclusivamente los tres endpoints
HTTP de transferencia binaria de `firma-binary-transfer.md`
(`eventing-operations.md` §1), fuera del alcance de este documento. Ningún
evento transporta la credencial ni el token de un actor humano.

### 0.7 Datos sensibles

Ningún evento transporta `storageKey`, bucket, ruta física, secretos,
credenciales ni el binario del PDF. Los eventos de firmante (§9-§12)
transportan datos de identidad profesional (nombre, rol, vigencia), no
datos personales sensibles adicionales.

### 0.8 Observabilidad

Todo evento publicado/entregado expone los campos de
`eventing-operations.md` §8 (`evento_id`, `evento_codigo`,
`schema_version`, `productor_codigo`, `ocurrido_en`, `correlation_id`,
`causation_id`, `agregado_codigo`, `agregado_id`, `agregado_version`,
`attempts` en `fal_outbox`/`fal_inbox`, `si_error` booleano simple en
`fal_outbox`/`fal_inbox`). El detalle de cada fallo técnico (código,
resumen breve) vive en `fal_inbox_error`, nunca en `bus_evento`: la tabla
central no tiene columnas de observabilidad de consumo.

### 0.9 Compatibilidad de versión

Cambios aditivos (nuevo campo opcional) no incrementan `schemaVersion`.
Cualquier cambio incompatible (campo obligatorio nuevo, cambio de tipo,
eliminación de campo) incrementa `schemaVersion` y requiere que el
productor soporte ambas versiones durante una ventana de convivencia
documentada en el momento de ese cambio (no aplica todavía: los 12 eventos
están en su versión inicial). Un consumidor que recibe un `schemaVersion`
que no soporta responde con `SolicitudFirmaRechazadaPorContrato` cuando el
evento en cuestión es `FirmaSolicitada`; para el resto de los eventos, el
consumidor rechaza el mensaje como error permanente de contrato (Inbox
local en `status = ERROR_DEFINITIVO` inmediato, sin agotar los 10 intentos
automáticos) ya que no existe un evento de rechazo de contrato en sentido
inverso (Firmas → Faltas ya cubierto; Faltas → Firmas queda cubierto por el
mismo evento en la dirección que corresponda).

### 0.10 Tests de contrato — matriz general

Aplica a los 12 eventos (además de los tests específicos de cada uno,
§1-§12): payload válido mínimo, payload con todos los campos opcionales,
rechazo por campo obligatorio ausente, rechazo por tipo de campo inválido,
duplicado exacto (mismo `eventoId`) no reejecuta efecto, evento con
`aggregateVersion` ya aplicada se descarta, evento con `aggregateVersion`
en hueco queda pendiente sin error, publicación falla después de commit de
dominio y antes de publicar en Outbox (recuperación por relay tras
reinicio), consumo falla después de aplicar efecto y antes de marcar Inbox
(reintento no debe duplicar efecto).

---

## 1. `FirmaSolicitada`

**Productor:** `faltas`. **Consumidores:** `firmas`.

**Trigger:** `FalDocumentoFirmaReq` pasa a `estado_firma_req = PENDIENTE`
(alta del requisito) — `DocumentoService.enviarAFirma` →
`materializarFirmaReqDesdeInternal`, misma transacción que la escritura en
`fal_outbox`.

**Precondiciones:** el requisito existe, está activo (`si_activa = TRUE`) y
en `PENDIENTE`; el documento referenciado existe.

**Aggregate:** `aggregate_type = FalDocumentoFirmaReq`,
`aggregate_id = "{documentoId}-{seqFirmaReq}"`,
`aggregate_version = version_row` en el momento de la solicitud.

**Idempotency key (negocio):** `documentoId + seqFirmaReq` (un único
requisito no publica dos `FirmaSolicitada` activas simultáneas: mientras
exista una solicitud transitoria abierta, `enviarAFirma` es idempotente,
`DocumentoService.existePorDocumento`).

**Payload:**

```json
{
  "solicitudFirmaId": "uuid",
  "sistemaOrigen": "faltas",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionSolicitud": 0,
  "versionDocumento": 3,
  "tipoDocumento": "RESOLUCION",
  "rolFirmaReq": 10,
  "mecanismoFirmaReq": 1,
  "ordenFirma": 1,
  "siObligatoria": true,
  "firmanteSugerido": { "firmanteId": 55, "verFirmante": 4 },
  "numeracion": { "momentoNumeracionDocu": "AL_FIRMAR", "yaNumerado": false },
  "fhSolicitud": "2026-07-31T20:00:00Z"
}
```

| Campo | Obligatorio | Tipo/enum |
|---|---|---|
| `solicitudFirmaId` | Sí | UUID, generado por Faltas al publicar |
| `documentoId` | Sí | Long |
| `seqFirmaReq` | Sí | Short |
| `revisionSolicitud` | Sí | Int, inicia en 0 |
| `versionDocumento` | Sí | Int (`fal_documento.version_row`) |
| `tipoDocumento` | Sí | `TipoDocu` |
| `rolFirmaReq` | Sí | `RolFirmaReq` |
| `mecanismoFirmaReq` | No | `MecanismoFirmaReq`; ausente = sin restricción |
| `ordenFirma` | No | Short; ausente = sin orden obligatorio |
| `siObligatoria` | Sí | Boolean |
| `firmanteSugerido` | No | `{firmanteId, verFirmante}`; ausente = sin asignación previa |
| `numeracion.momentoNumeracionDocu` | Sí | `MomentoNumeracionDocu` |
| `numeracion.yaNumerado` | Sí | Boolean |
| `fhSolicitud` | Sí | UTC ISO-8601 |

**No viaja:** URL de numeración, bucket, storage key, ruta física,
Markdown/PDF completo, certificado, datos extensos del expediente.

**Efecto en el consumidor (Firmas):** crea/actualiza su tarea local de
trabajo humano en `estado` conceptual `SOLICITADA` (fuera del alcance físico
de Faltas). Firmas no descarga el artefacto en este punto; lo solicita bajo
demanda vía `preparar-artefacto`/`artefacto` (`firma-binary-transfer.md`)
cuando un firmante confirma la acción.

**Estados válidos:** se publica exactamente una vez por transición a
`PENDIENTE`; no se republica mientras el requisito permanezca en
`PENDIENTE`/`CANCELACION_SOLICITADA` sin haber vuelto a `PENDIENTE` desde un
estado previo.

**Errores permanentes específicos:** `rolFirmaReq`/`mecanismoFirmaReq` con
código fuera de catálogo → `SolicitudFirmaRechazadaPorContrato` (§4, código
`REFERENCIA_INCOMPATIBLE`).

**Tests de contrato específicos:** `firmanteSugerido` ausente vs. presente;
`mecanismoFirmaReq`/`ordenFirma` ausentes (sin restricción) vs. presentes;
republicación idempotente al reintentar `enviarAFirma` sobre un requisito ya
`PENDIENTE`.

---

## 2. `SolicitudFirmaAceptada`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** Firmas recibió y aceptó `FirmaSolicitada` para su workflow
humano (ack técnico).

**Precondiciones:** existe una `FirmaSolicitada` previa con el mismo
`solicitudFirmaId`.

**Aggregate:** mismo `aggregate_type`/`aggregate_id` que `FirmaSolicitada`;
`aggregate_version` = la vigente al momento de aceptar.

**Idempotency key:** `solicitudFirmaId` (aceptar dos veces la misma
solicitud es un no-op para Faltas).

**Payload:**

```json
{ "solicitudFirmaId": "uuid", "documentoId": 1234, "seqFirmaReq": 1, "revisionSolicitud": 0, "fhAceptacion": "2026-07-31T20:05:00Z" }
```

Todos los campos obligatorios.

**Efecto en el consumidor (Faltas):** actualiza únicamente
`fal_documento_firma_req.fh_aceptacion` (metadata operativa; no cambia
`estado_firma_req`, que permanece en `PENDIENTE`). **Prohibido, sin
excepción:** este evento nunca registra una firma, nunca satisface el
requisito, nunca invoca `RegistrarFirmaDocumental`, nunca genera `DOCFIR`,
nunca mueve documento, fallo o notificación (`DEC-EDA-008`).

**Estados válidos:** requisito en `PENDIENTE` con `fh_aceptacion IS NULL`.
Si llega con `fh_aceptacion` ya seteada, se aplica idempotentemente (no
error).

**Errores permanentes específicos:** `solicitudFirmaId` inexistente para
Faltas → se descarta como evento huérfano y se reporta en observabilidad
(no hay evento de rechazo de contrato en este sentido; Faltas no publica
eventos de vuelta para un ack).

**Tests de contrato específicos:** aceptación duplicada no altera
`fh_aceptacion` ya seteada; aceptación de una solicitud ya `FIRMADA`/
terminal se descarta sin error (evento obsoleto, §0.4).

---

## 3. `SolicitudFirmaRechazadaPorContrato`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** error permanente de contrato al procesar `FirmaSolicitada` (no
error transitorio, no rechazo humano).

**Precondiciones:** ninguna sobre el estado de dominio — puede ocurrir antes
de que Firmas materialice ninguna tarea local.

**Aggregate:** mismo `aggregate_type`/`aggregate_id` que `FirmaSolicitada`.

**Idempotency key:** `solicitudFirmaId` + `codigoRechazo`.

**Payload:**

```json
{
  "solicitudFirmaId": "uuid",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionSolicitud": 0,
  "codigoRechazo": "SCHEMA_NO_SOPORTADO",
  "detalle": "schemaVersion 2 no soportado por esta version de Firmas",
  "fhRechazo": "2026-07-31T20:06:00Z"
}
```

**Enum `codigoRechazo` (`CodigoRechazoContrato`):** `SCHEMA_NO_SOPORTADO`,
`CAMPO_OBLIGATORIO_AUSENTE`, `REFERENCIA_INCOMPATIBLE`,
`HASH_O_REVISION_INCONSISTENTE`, `REQUISITO_NO_PROCESABLE`.

**No usar para:** errores transitorios, timeout, indisponibilidad, rechazo
humano (usar `FirmaRechazada`, §7).

**Efecto en el consumidor (Faltas):** `estado_firma_req` → `RECHAZADO_CONTRATO`
(código 9); `motivo_terminal_firma_req` = código de `codigoRechazo`;
`detalle_terminal_firma_req` = `detalle`; `fh_terminal` = `fhRechazo`.
Requiere intervención humana o una nueva solicitud corregida (nueva
`FirmaSolicitada` sobre un requisito reingresado, nunca reabriendo el mismo
registro).

**Estados válidos:** requisito en `PENDIENTE`.

**Tests de contrato específicos:** cada valor de `codigoRechazo` transiciona
correctamente; un `RECHAZADO_CONTRATO` no puede volver a `PENDIENTE` sin un
nuevo requisito/solicitud.

---

## 4. `DocumentoFirmado`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** Firmas completó la firma criptográfica del artefacto
descargado vía `firma-binary-transfer.md`, tras cargar el resultado en
`PUT .../resultados/{resultadoFirmaId}`.

**Precondiciones:** existe un resultado cargado y validado
(`hashEntrada` coincide con el vigente) para `resultadoFirmaId`.

**Aggregate:** mismo `aggregate_type`/`aggregate_id` que `FirmaSolicitada`;
`aggregate_version` = `revisionSolicitud` recibida.

**Idempotency key (negocio):** `referenciaFirmaExt`
(`DocumentoFirmaRepository.guardarSiAusentePorReferencia`), delegada
íntegramente al comando real; el Inbox agrega idempotencia de *transporte*
por `eventoId` sin reemplazarla.

**Mapeo evento → `RegistrarFirmaDocumentalCommand` real:**

| Campo del evento | Campo del comando |
|---|---|
| `documentoId` | `documentoId` |
| `seqFirmaReq` | `seqFirmaReq` |
| `firmanteEfectivo.firmanteId` | `idFirmante` |
| `mecanismoFirma` | `tipoFirma` (`TipoFirma`) |
| `firmanteEfectivo.idUser` | `idUserFirma` |
| `hashEntrada` | `hashDocumento` |
| `referenciaFirmaExt` | `referenciaFirmaExt` |

**Payload:**

```json
{
  "solicitudFirmaId": "uuid",
  "resultadoFirmaId": "uuid",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionSolicitud": 0,
  "revisionEntrada": 3,
  "revisionSalida": 4,
  "referenciaFirmaExt": "FIR-2026-000123",
  "firmanteSugerido": { "firmanteId": 55, "verFirmante": 4 },
  "firmanteEfectivo": { "firmanteId": 55, "verFirmante": 4, "idUser": "func-4821", "esDelegado": false },
  "actorTecnico": { "tipo": "APLICACION", "id": "firmas-consumer-01", "app": "firmas", "keyFingerprint": "sha256:ab12..." },
  "mecanismoFirma": "DIGITAL",
  "fhFirma": "2026-07-31T22:15:00Z",
  "hashAlgoritmo": "SHA-256",
  "hashEntrada": "sha256:...",
  "hashSalida": "sha256:...",
  "evidenciaId": "FIR-EVID-2026-000123",
  "pki": {
    "perfil": "PAdES-B-LT",
    "certificadoSerial": "0A1B2C",
    "certificadoThumbprint": "sha256:...",
    "certificadoSubject": "CN=Funcionario, O=Municipio",
    "certificadoIssuer": "CN=AC Firmas Municipal",
    "certificadoVigenciaDesde": "2025-01-01T00:00:00Z",
    "certificadoVigenciaHasta": "2027-01-01T00:00:00Z",
    "resultadoCadena": "VALIDA",
    "fuenteRevocacion": "OCSP",
    "resultadoRevocacion": "NO_REVOCADO",
    "tsaNombre": "TSA Municipal",
    "tsaInstante": "2026-07-31T22:15:03Z",
    "resultadoValidacion": "VALIDO",
    "versionValidador": "1.0.0"
  }
}
```

| Campo | Obligatorio |
|---|---|
| `solicitudFirmaId`, `resultadoFirmaId`, `documentoId`, `seqFirmaReq`, `revisionSolicitud` | Sí |
| `revisionEntrada`, `revisionSalida` | Sí |
| `referenciaFirmaExt` | Sí |
| `firmanteSugerido` | No |
| `firmanteEfectivo.{firmanteId,verFirmante,idUser}` | Sí |
| `firmanteEfectivo.esDelegado` | Sí |
| `actorTecnico.*` | Sí |
| `mecanismoFirma` | Sí (`TipoFirma`) |
| `fhFirma` | Sí |
| `hashAlgoritmo` | Sí (`SHA-256` mínimo) |
| `hashEntrada`, `hashSalida` | Sí |
| `evidenciaId` | Sí |
| `pki.*` | Sí (metadata mínima `DEC-EDA-023`) |

**Precondición de metadata (`DEC-EDA-033`, previa a la de PKI, mismo
paso):** `referenciaFirmaExt` y `evidenciaId` del evento deben coincidir
exactamente con los mismos campos ya persistidos en
`fal_documento_firma_resultado` por el `PUT` de
`firma-binary-transfer.md` §3 para este `resultadoFirmaId`; igual para
`solicitudFirmaId`, `documentoId`, `seqFirmaReq`, las revisiones y los
hashes. Cualquier divergencia es metadata divergente y sigue la misma
resolución que una PKI inválida (ver abajo).

**Precondición de PKI (`DEC-EDA-023`/`035`, obligatoria antes de aplicar
cualquier efecto de dominio):**

```text
pki.resultadoValidacion IN (VALIDO, VALIDO_CON_OBSERVACIONES_PERMITIDAS)
pki.resultadoCadena = VALIDA
pki.resultadoRevocacion = NO_REVOCADO
pki.tsaInstante informado y no futuro
pki.certificadoVigenciaDesde <= pki.tsaInstante <= pki.certificadoVigenciaHasta
pki.perfil = 'PAdES-B-LT'
```

**Resolución local exacta si la metadata o cualquiera de estas seis
condiciones no se cumple (`DEC-EDA-036`):** en la misma transacción local
de procesamiento del consumidor (`eventing-operations.md` §3bis.2),
**antes** de invocar el comando de dominio, `fal_documento_firma_resultado`
transiciona a `RECHAZADO` con un código estructurado de rechazo (ver
`../10-domain/firma-notificacion-fallo.md`, "Firma digital y evidencia
PKI"); `fal_inbox` cierra en `status = PROCESADO`, `si_error = FALSE`
(rechazo funcional de dominio, no error técnico: **no** se inserta fila en
`fal_inbox_error`, §0.5bis); **no** se crea `fal_documento_firma`, **no**
se satisface el requisito y **no** se reemplaza el PDF vigente. Ese
`resultadoFirmaId` queda agotado: Firmas debe producir un nuevo resultado y
un nuevo evento tras corregir la causa. Solo si la
metadata coincide y las seis condiciones de PKI se cumplen, el consumidor
invoca exclusivamente
`DocumentoService.registrarFirmaDocumental`/`RegistrarFirmaDocumentalCommand`
(nunca el circuito legacy `firmarDocumento`, `DEC-EDA-009`), copiando el
actor técnico validado desde el recibo HTTP persistido (nunca
directamente del payload de este evento, `DEC-EDA-033`). Persiste
`hash_salida`, `revision_entrada`, `revision_salida`, columnas de actor
técnico y metadata PKI en `fal_documento_firma`
(`../50-persistence/firma-eda-physical-model.md` §2). Tras persistir,
reemplaza el PDF vigente e incrementa `fal_documento.revision_artefacto`
(secuencia atómica en `firma-binary-transfer.md` §0bis y
`../50-persistence/firma-eda-physical-model.md` §5bis, nunca en el
`PUT`). Requiere numeración `AL_FIRMAR` previa; rechaza (422) si
falta — ver tarea de implementación en
`../10-domain/firma-notificacion-fallo.md`, "`AL_FIRMAR`: orden normativo".

**Estados válidos:** requisito en `PENDIENTE` o `CANCELACION_SOLICITADA`
(regla de carrera, §0.4 y `../10-domain/firma-notificacion-fallo.md`
"Regla de carrera firma/cancelación"). Si el requisito ya está en
`CANCELACION_SOLICITADA` y este evento llega primero, se aplica: pasa a
`FIRMADO` y cualquier `SolicitudFirmaCanceladaConfirmada` posterior para la
misma `revisionSolicitud` se descarta como obsoleta.

**Errores permanentes específicos:** `hashEntrada` del evento no coincide
con el `hash_firma`/`hashEntrada` ya validado en la carga HTTP → resolución
local exacta de rechazo descrita arriba (nunca se aplica un efecto de
dominio con hashes inconsistentes); `referenciaFirmaExt`/`evidenciaId` no
coinciden con el recibo persistido → misma resolución (`DEC-EDA-033`);
cualquiera de las seis condiciones de PKI de arriba incumplida → misma
resolución (nunca se aplica un efecto de dominio con PKI inválida).

**Tests de contrato específicos:** carrera firma/cancelación en ambos
órdenes (§0.4); numeración `AL_FIRMAR` ausente → 422; múltiples firmas
consecutivas preservan `hash_salida` anterior como `hash_entrada` siguiente
(cadena); certificado revocado → resolución local de rechazo, sin efecto
de dominio; certificado fuera de vigencia al `tsaInstante` → resolución
local de rechazo, sin efecto de dominio; `resultadoCadena = INVALIDA` →
resolución local de rechazo, sin efecto de dominio; `tsaInstante` ausente
o futuro → resolución local de rechazo, sin efecto de dominio;
`pki.perfil` distinto de `PAdES-B-LT` (incluido `PAdES-B-T`) → resolución
local de rechazo, sin efecto de dominio (`DEC-EDA-035`);
`referenciaFirmaExt`/`evidenciaId` divergentes del recibo → resolución
local de rechazo (`DEC-EDA-033`); reintento del mismo `resultadoFirmaId`
ya `RECHAZADO` → rechazado de nuevo de forma idempotente, nunca reaplica
ni revierte (`DEC-EDA-036`); `resultadoValidacion =
VALIDO_CON_OBSERVACIONES_PERMITIDAS` con observación
`TSA_NO_CUALIFICADA_CONFIABLE` o `REVOCACION_POR_CRL_SIN_OCSP` y las demás
condiciones satisfechas → efecto de dominio aplicado normalmente;
`resultadoValidacion = INVALIDO` → resolución local de rechazo sin efecto
de dominio, aunque el resto de las condiciones sea válido.

---

## 5. `SolicitudFirmaCancelada`

**Productor:** `faltas`. **Consumidores:** `firmas`.

**Trigger:** Faltas determina que un requisito en curso ya no debe
procesarse (documento anulado, requisito reemplazado, flujo revertido,
cancelación autorizada, error funcional irrecuperable).

**Precondiciones:** el requisito no está en un estado terminal
(`FIRMADO`, `ANULADO`, `VENCIDO`, `REEMPLAZADO`, `CANCELADO`, `RECHAZADO`,
`RECHAZADO_CONTRATO`).

**Aggregate:** mismo `aggregate_type`/`aggregate_id`;
`aggregate_version` = `revision_solicitud` vigente al momento de cancelar.

**Idempotency key:** `solicitudFirmaId` + `revisionSolicitud`.

**Payload:**

```json
{
  "solicitudFirmaId": "uuid",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionSolicitud": 0,
  "motivoCancelacion": "DOCUMENTO_ANULADO",
  "detalle": "Documento anulado por resolucion complementaria",
  "actor": { "idUser": "func-1122", "esProceso": false },
  "fhSolicitudCancelacion": "2026-07-31T21:00:00Z"
}
```

**Enum `motivoCancelacion` (`MotivoCancelacionFirmaReq`):**
`DOCUMENTO_ANULADO`, `REQUISITO_REEMPLAZADO`, `FLUJO_REVERTIDO`,
`CANCELACION_AUTORIZADA`, `ERROR_FUNCIONAL_IRRECUPERABLE`. No se admite
texto libre como única clasificación; `detalle` es un complemento opcional
limitado.

**Efecto local en Faltas (antes de publicar):** valida que el requisito no
esté en estado terminal; registra `estado_firma_req = CANCELACION_SOLICITADA`
(código 6); persiste `motivo_terminal_firma_req` provisional y `detalle`;
escribe el evento en `fal_outbox` en la misma transacción.

**Efecto en el consumidor (Firmas):** cierra su tarea local de trabajo
humano si aún está abierta, y responde con
`SolicitudFirmaCanceladaConfirmada` (§6).

**Estados válidos:** transición únicamente desde `PENDIENTE`.

**Tests de contrato específicos:** cada uno de los 5 motivos; intento de
cancelar un requisito ya terminal se rechaza en el propio comando de Faltas
antes de publicar (no llega a generarse el evento).

---

## 6. `SolicitudFirmaCanceladaConfirmada`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** Firmas cerró la tarea local sin producir una firma, en
respuesta a `SolicitudFirmaCancelada`.

**Precondiciones:** existe una `SolicitudFirmaCancelada` previa con el mismo
`solicitudFirmaId` + `revisionSolicitud`.

**Aggregate:** mismo `aggregate_type`/`aggregate_id`;
`aggregate_version` = `revisionSolicitud` que se confirma.

**Idempotency key:** `solicitudFirmaId` + `revisionSolicitud`.

**Payload:**

```json
{ "solicitudFirmaId": "uuid", "documentoId": 1234, "seqFirmaReq": 1, "revisionSolicitud": 0, "fhConfirmacion": "2026-07-31T21:05:00Z" }
```

**Efecto en el consumidor (Faltas), regla de carrera (`DEC-EDA-016` §6.2):**

1. si `estado_firma_req = CANCELACION_SOLICITADA` con la misma
   `revisionSolicitud`: transiciona a `CANCELADO` (código 7), copia el
   `motivo_terminal_firma_req` provisional a definitivo, `fh_terminal` =
   `fhConfirmacion`;
2. si `estado_firma_req` ya es `FIRMADO` (un `DocumentoFirmado` se aplicó
   primero para la misma revisión): esta confirmación se registra como
   evento obsoleto en observabilidad y **no altera** `FIRMADO`;
3. si la `revisionSolicitud` no coincide con la vigente: se descarta como
   evento fuera de orden (§0.4), sin error.

**Estados válidos:** `CANCELACION_SOLICITADA` (caso 1) o `FIRMADO` (caso 2,
no-op) para la misma `revisionSolicitud`.

**Tests de contrato específicos:** los tres casos de la regla de carrera
anteriores, exactamente como están numerados.

---

## 7. `FirmaRechazada`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** rechazo humano explícito por parte del firmante (no error
técnico, no rechazo de contrato).

**Precondiciones:** requisito en `PENDIENTE`, con una tarea local abierta en
Firmas.

**Aggregate:** mismo `aggregate_type`/`aggregate_id`;
`aggregate_version` = `revisionSolicitud` vigente.

**Idempotency key:** `solicitudFirmaId` + `revisionSolicitud`.

**Payload:**

```json
{
  "solicitudFirmaId": "uuid",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionSolicitud": 0,
  "firmanteEfectivo": { "firmanteId": 55, "verFirmante": 4, "idUser": "func-4821" },
  "motivoRechazo": "DOCUMENTO_INCOMPLETO",
  "detalle": "Falta el anexo referenciado en el articulo 3",
  "fhRechazo": "2026-07-31T21:10:00Z",
  "actorTecnico": { "tipo": "APLICACION", "id": "firmas-consumer-01", "app": "firmas", "keyFingerprint": "sha256:ab12..." },
  "correlationId": "uuid"
}
```

**Enum `motivoRechazo` (`MotivoRechazoFirma`):** `NO_CONFORME`,
`DATOS_INCORRECTOS`, `DOCUMENTO_INCOMPLETO`, `COMPETENCIA_INSUFICIENTE`,
`OTRO_JUSTIFICADO`.

**Efecto en el consumidor (Faltas):** `estado_firma_req` → `RECHAZADO`
(código 8); `motivo_terminal_firma_req` = código de `motivoRechazo`;
`detalle_terminal_firma_req` = `detalle`; `fh_terminal` = `fhRechazo`;
registra auditoría del actor técnico igual que `DocumentoFirmado` (§4). No
satisface el requisito y no genera `DOCFIR`.

**Estados válidos:** `PENDIENTE`.

**Tests de contrato específicos:** cada uno de los 5 motivos; rechazo sobre
un requisito ya `FIRMADO` se descarta como evento obsoleto (§0.4).

---

## 8. `SolicitudFirmaVencida`

**Productor:** `faltas`. **Consumidores:** `firmas`.

**Trigger:** Faltas determina el vencimiento mediante un proceso idempotente
que evalúa `vence_en` persistido contra el reloj actual.

**Precondiciones:** requisito en `PENDIENTE`, con `vence_en` no nulo y ya
pasado.

**Aggregate:** mismo `aggregate_type`/`aggregate_id`;
`aggregate_version` = `revisionSolicitud` vigente.

**Idempotency key:** `solicitudFirmaId` + `revisionSolicitud`.

**Payload:**

```json
{ "solicitudFirmaId": "uuid", "documentoId": 1234, "seqFirmaReq": 1, "revisionSolicitud": 0, "venceEn": "2026-07-30T00:00:00Z", "fhVencimiento": "2026-07-31T00:05:00Z" }
```

**Autoridad del vencimiento:** exclusivamente Faltas. Firmas nunca declara
un requisito vencido por su cuenta; solo cierra su tarea local al recibir
este evento (`DEC-EDA-016` §6.4). Faltas y Firmas nunca son simultáneamente
autoridad de vencimiento.

**Efecto local en Faltas (antes de publicar):** cambia
`estado_firma_req = VENCIDO` (código 4, existente); escribe el evento en
`fal_outbox` en la misma transacción que ese cambio.

**Efecto en el consumidor (Firmas):** cierra la tarea local si todavía está
abierta.

**Hora:** `venceEn`/`fhVencimiento` siempre UTC, en persistencia y en el
contrato.

**Estados válidos:** transición únicamente desde `PENDIENTE`.

**Tests de contrato específicos:** proceso de detección de vencimiento
idempotente (correrlo dos veces sobre el mismo requisito no duplica el
evento ni cambia el estado dos veces); vencimiento de un requisito que se
firmó en el mismo instante se resuelve por `aggregate_version` (§0.4), no
por orden de reloj.

---

## 9. `FirmanteCreado`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** alta de un firmante en el maestro global de Firmas.

**Precondiciones:** ninguna sobre el dominio de Faltas (Firmas es la
autoridad de origen).

**Aggregate:** `aggregate_type = Firmante`, `aggregate_id = firmanteId`,
`aggregate_version` = 1 (primera versión).

**Idempotency key:** `firmanteId`.

**Payload:**

```json
{ "firmanteId": 55, "nombreFirmante": "Nombre Apellido", "fhAlta": "2026-01-10T10:00:00Z" }
```

**Efecto en el consumidor (Faltas):** crea la fila espejo en `fal_firmante`
(proyección local versionada, `DEC-EDA-006`).

**Prohibido, sin excepción:** este evento y los de §10-§12 nunca invocan
`RegistrarFirmaDocumental`, nunca satisfacen `fal_documento_firma_req`,
nunca mueven `FalDocumento`/`FalActaFallo`/`FalNotificacion`, ni tocan
`FalDocumentoFirmaReq.idFirmanteAsig`.

**Estados válidos:** `firmanteId` no existe todavía en `fal_firmante`.

**Tests de contrato específicos:** alta duplicada (mismo `firmanteId`) es
idempotente (no crea una segunda fila); bootstrap inicial procesa un lote
de `FirmanteCreado` en cualquier orden relativo entre sí sin duplicar.

---

## 10. `FirmanteVersionCreada`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** nueva versión de datos de un firmante existente en Firmas.

**Precondiciones:** el `firmanteId` ya existe en la proyección local (si no
existe, se aplica primero un bootstrap implícito creando también la fila
base — reconciliación, `eventing-operations.md` §7).

**Aggregate:** `aggregate_type = FirmanteVersion`,
`aggregate_id = "{firmanteId}-{verFirmante}"`, `aggregate_version = verFirmante`.

**Idempotency key:** `firmanteId` + `verFirmante`.

**Payload:**

```json
{ "firmanteId": 55, "verFirmante": 4, "nombreFirmante": "Nombre Apellido", "rolProfesional": "SECRETARIO", "fhVersion": "2026-06-01T00:00:00Z" }
```

**Efecto en el consumidor (Faltas):** inserta fila en `fal_firmante_version`
(append-only por versión, nunca actualiza una versión existente).

**Estados válidos:** `(firmanteId, verFirmante)` no existe todavía.

**Tests de contrato específicos:** versión fuera de orden (llega `verFirmante=4`
antes que `verFirmante=3`) queda pendiente por hueco de versión
(`eventing-operations.md` §7), no se aplica adelantada.

---

## 11. `FirmanteHabilitacionModificada`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** cambio de habilitación (alta/baja/vigencia) de un firmante para
firmar.

**Precondiciones:** `(firmanteId, verFirmante)` existe en la proyección
local.

**Aggregate:** `aggregate_type = FirmanteHabilitacion`,
`aggregate_id = "{firmanteId}-{verFirmante}"`,
`aggregate_version` = contador incremental de modificaciones de
habilitación para esa versión de firmante.

**Idempotency key:** `firmanteId` + `verFirmante` + `aggregateVersion`.

**Payload:**

```json
{ "firmanteId": 55, "verFirmante": 4, "siHabilitado": true, "fhVigenciaDesde": "2026-06-01T00:00:00Z", "fhVigenciaHasta": null, "fhModificacion": "2026-06-01T00:00:00Z" }
```

**Efecto en el consumidor (Faltas):** actualiza/inserta fila en
`fal_firmante_version_habilitacion` reflejando la vigencia informada.

**Estados válidos:** cualquier estado previo de habilitación para esa
versión de firmante (la modificación siempre es aplicable; es un cambio de
vigencia, no una transición con precondición de estado).

**Tests de contrato específicos:** habilitar y luego deshabilitar en
sucesión respeta el orden por `aggregateVersion`; una modificación con
`aggregateVersion` ya aplicada se descarta (§0.4).

---

## 12. `DelegacionFirmanteModificada`

**Productor:** `firmas`. **Consumidores:** `faltas`.

**Trigger:** cambio de delegación de firma (alta, baja o modificación de
alcance) en Firmas.

**Precondiciones:** el firmante delegante y el delegado existen en la
proyección local (bootstrap/reconciliación si no).

**Aggregate:** `aggregate_type = DelegacionFirmante`,
`aggregate_id = "{firmanteDelegante}-{firmanteDelegado}"`,
`aggregate_version` = contador incremental de esa relación de delegación.

**Idempotency key:** `firmanteDelegante` + `firmanteDelegado` +
`aggregateVersion`.

**Payload:**

```json
{
  "firmanteDelegante": 55,
  "firmanteDelegado": 61,
  "siActiva": true,
  "alcance": "TODOS_LOS_ROLES",
  "fhVigenciaDesde": "2026-06-01T00:00:00Z",
  "fhVigenciaHasta": null,
  "fhModificacion": "2026-06-01T00:00:00Z"
}
```

**Efecto en el consumidor (Faltas):** actualiza la proyección local de
delegaciones vigentes, usada únicamente para validar
`firmanteEfectivo.esDelegado` al consumir `DocumentoFirmado` (§4); nunca
para asignar automáticamente un requisito al delegado.

**Estados válidos:** cualquier estado previo de la relación de delegación.

**Tests de contrato específicos:** delegación activa vigente valida
correctamente un `DocumentoFirmado` con `esDelegado = true`; delegación
desactivada rechaza (motivo `COMPETENCIA_INSUFICIENTE` a nivel de
validación de Faltas, previo a aplicar el comando) un intento de firma
delegada posterior a la baja.
