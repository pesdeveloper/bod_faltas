# Transferencia binaria definitiva Faltas ↔ Firmas (`DEC-EDA-017`/`024`)

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Mecanismo único y cerrado de transferencia del PDF vigente y de su
> resultado firmado entre Faltas y Firmas. `IMPLEMENTATION_NOT_STARTED`:
> ninguno de los tres endpoints existe en código. Autenticación M2M en
> `eventing-operations.md`. Ownership y ciclo del PDF vigente en
> `../10-domain/firma-notificacion-fallo.md`. Tabla de staging del resultado
> temporal en `../50-persistence/firma-eda-physical-model.md` §5
> (`fal_documento_firma_resultado`).

## 0. Decisión de mecanismo (sin alternativa)

**Transferencia mediante API HTTP autenticada con streaming.** No se usan
URLs temporales de storage ni acceso directo de Firmas al storage de
Faltas. Los tres contratos de esta sección son los únicos autorizados.

## 0bis. Único punto de aplicación del efecto de dominio (`DEC-EDA-024`)

**Decisión final, sin alternativa:** el endpoint `PUT
.../resultados/{resultadoFirmaId}` (§3) recibe, valida y **registra
exclusivamente un candidato firmado temporal** en
`fal_documento_firma_resultado`. El `PUT` **no invoca**
`RegistrarFirmaDocumentalCommand`, **no crea** `fal_documento_firma`, **no
cambia** `estado_firma_req` y **no reemplaza** `fal_documento.storage_key`.

El único punto de aplicación del efecto de dominio de una firma es el
**consumidor del evento `DocumentoFirmado`**
(`../40-api/firma-event-contracts.md` §4). El `PUT` HTTP y el consumo del
evento son dos operaciones distintas, en dos momentos distintos, sobre dos
tablas distintas (`fal_documento_firma_resultado` vs.
`fal_documento_firma`/`fal_documento`).

### Secuencia normativa completa (staging → aplicación)

```text
1. Firmas carga el candidato firmado por PUT, en una clave de storage
   interna, inmutable y ya definitiva (storage_key_candidato); no es una
   ruta temporal que requiera rename/copy posterior.
2. Faltas valida binario, MIME, tamaño, hash y metadata (incluida la
   igualdad exacta con fal_documento_firma_req.hash_entrada_preparado y
   revision_artefacto_preparada, DEC-EDA-030).
3. Faltas persiste un recibo técnico idempotente en
   fal_documento_firma_resultado (estado_resultado = VALIDADO), incluida
   la metadata de vinculación (referencia_firma_ext, evidencia_id,
   upload_actor_id/app/key_fingerprint derivados de la X-Api-Key
   autenticada, DEC-EDA-033).
4. Faltas responde 200/201 con el recibo técnico: el candidato fue
   aceptado para staging (no significa que la firma ya fue aplicada).
5. Firmas publica DocumentoFirmado desde fir_outbox (evento independiente
   del PUT anterior).
6. Faltas consume DocumentoFirmado.
7. En una única transacción local (ver ../50-persistence/firma-eda-physical-model.md
   §5/§5bis y eventing-operations.md §3bis):
   - valida Inbox/idempotencia por event_id;
   - bloquea requisito, documento y fila de fal_documento_firma_resultado;
   - valida metadata (referenciaFirmaExt/evidenciaId contra el recibo,
     DEC-EDA-033) y PKI (../10-domain/firma-notificacion-fallo.md, "Firma
     digital y evidencia PKI");
   - rama inválida: marca fal_documento_firma_resultado RECHAZADO y
     termina la transacción aquí (paso 8 directo; ver más abajo);
   - rama válida: crea fal_documento_firma copiando el actor técnico
     validado desde el recibo HTTP (nunca del payload del evento);
   - satisface fal_documento_firma_req (estado_firma_req = FIRMADO);
   - swap atómico de fal_documento al nuevo PDF vigente
     (storage_key = storage_key_candidato, hash_docu, revision_artefacto;
     firma-eda-physical-model.md §5bis paso 2);
   - marca fal_documento_firma_resultado.estado_resultado = CONSUMIDO,
     copia storage_key_anterior, fija cleanup_status;
   - inserta fal_inbox;
   - inserta cualquier evento de respuesta en fal_outbox.
8. Commit local (rama válida o rama inválida).
9. Recién después del commit (solo rama válida, solo si
   cleanup_status = PENDIENTE):
   - un job asíncrono elimina el PDF anterior referenciado por
     storage_key_anterior (firma-eda-physical-model.md §5bis paso 7);
   - al confirmar la eliminación, limpia storage_key_anterior y
     storage_key_candidato de fal_documento_firma_resultado
     (cleanup_status = COMPLETADO); el resto de la metadata permanece
     como evidencia;
   - fallas de limpieza reintentan con backoff y luego quedan
     QUARANTINED para reconciliación manual, sin revertir el dominio ya
     aplicado en el paso 7-8 ni afectar el PDF vigente ya swapeado.
```

Si la transacción local del paso 7 falla por un error técnico (no por PKI
inválida — ver rama inválida arriba):

- el PDF anterior sigue vigente;
- el candidato en `fal_documento_firma_resultado` permanece en `VALIDADO`
  (no pasa a `CONSUMIDO` ni a `RECHAZADO`);
- el evento se reintenta (`eventing-operations.md` §5);
- no se duplica ningún efecto (Inbox por `event_id`).

Si la PKI reportada en el evento es inválida, o la metadata del evento
(`referenciaFirmaExt`/`evidenciaId`) no coincide con el recibo persistido
(`../10-domain/firma-notificacion-fallo.md`, "Firma digital y evidencia
PKI"), la rama inválida del paso 7 se ejecuta dentro de la misma
transacción local de procesamiento
(`eventing-operations.md` §3bis.2): `fal_documento_firma_resultado` pasa a
`RECHAZADO` de forma idempotente, y **no** se crea `fal_documento_firma`,
**no** se satisface el requisito y **no** se reemplaza el PDF vigente.
`fal_inbox` cierra en `status = PROCESADO`, `si_error = FALSE` — es un
**rechazo funcional de dominio**, no un error técnico: por eso **no** se
inserta ninguna fila en `fal_inbox_error`
(`../10-domain/firma-notificacion-fallo.md`, "Firma digital y evidencia
PKI"). `bus_evento` no tiene ningún estado de entrega ni cuarentena
central que registrar: la resolución completa vive en
`fal_inbox`/`fal_documento_firma_resultado`, locales a Faltas. Ese
`resultadoFirmaId` queda agotado: Firmas debe producir un nuevo resultado
y un nuevo `evento_id` tras corregir la causa.

## 1. Preparación del artefacto

```http
POST /api/faltas/integraciones/firmas/solicitudes/{solicitudFirmaId}/preparar-artefacto
```

Límite `60 req/min` por credencial, burst `10`
(`../50-persistence/firma-eda-physical-model.md` §15); excedido responde
`429 Too Many Requests` con `Retry-After`.

**Responsabilidad del endpoint:**

1. validar que `solicitudFirmaId` existe y corresponde a un
   `FalDocumentoFirmaReq` en `estado_firma_req = PENDIENTE` (nunca
   `CANCELACION_SOLICITADA` ni un estado terminal — ver §1bis);
2. exigir numeración previa cuando la plantilla usa `AL_FIRMAR`; si falta,
   `422 Unprocessable Entity` (misma semántica que CMD-FALLO-001, "Orden de
   validaciones");
3. si el requisito ya tiene una preparación persistida
   (`fal_documento_firma_req.revision_artefacto_preparada` informado,
   `DEC-EDA-030`) para la misma `solicitudFirmaId` + `revisionSolicitud`,
   devolver exactamente esos cuatro valores persistidos (idempotencia
   real, verificada contra base, no contra memoria del proceso) — nunca
   re-renderizar ni volver a copiar el PDF;
4. si no hay preparación persistida para este requisito: renderizar el
   PDF vigente (primera creación física, Faltas, nunca Firmas) solo si
   `fal_documento.revision_artefacto = 0` (documento sin ningún artefacto
   todavía); si el documento ya tiene un PDF vigente por otra firma
   aplicada a otro requisito, **no** re-renderiza ni copia el PDF: solo
   vincula la `revision_artefacto` vigente al requisito;
5. persistir en el requisito (`../50-persistence/firma-eda-physical-model.md`
   §1.1bis) `revision_artefacto_preparada`, `hash_entrada_preparado`,
   `content_length_preparado` y `fh_preparacion` — los mismos cuatro
   valores que la respuesta devuelve;
6. si hubo render físico nuevo, actualizar además
   `fal_documento.storage_key`/`hash_docu` e incrementar
   `fal_documento.revision_artefacto` exactamente en 1 respecto de su
   valor previo (`0 -> 1` en la primera creación; nunca en preparaciones
   posteriores sobre un PDF ya existente);
7. calcular `hashEntrada` (hash del PDF exacto que se va a entregar,
   correspondiente a la `revisionArtefacto` vigente al momento de esta
   preparación, ya sea recién renderizada o preexistente);
8. devolver metadata — nunca el binario. `201 Created` únicamente en la
   primera creación física del PDF; `200 OK` en toda repetición
   idempotente y en toda preparación que solo vincula un PDF vigente ya
   existente sin re-renderizarlo.

### 1bis. Restricción de estado (`DEC-EDA-024`, regla anti-carrera de trabajo nuevo)

- `preparar-artefacto` **solo se permite** con el requisito en
  `PENDIENTE`. Si el requisito está en `CANCELACION_SOLICITADA` o en
  cualquier estado terminal, responde `409 Conflict`
  (`codigoError = REQUISITO_NO_ACEPTA_PREPARACION`).
- Ninguna preparación ni descarga de un artefacto **nuevo** se inicia
  después de que Faltas registró `CANCELACION_SOLICITADA` para ese
  requisito. "Nuevo" significa una `revisionArtefacto` que todavía no fue
  fijada antes de la cancelación.
- Esta restricción no afecta la aceptación de un `DocumentoFirmado` que
  complete criptográficamente una firma sobre una `revisionArtefacto` que
  **ya estaba preparada antes** de la cancelación: esa aceptación se
  resuelve exclusivamente por la regla de carrera del consumidor de
  eventos (`../10-domain/firma-notificacion-fallo.md`, "Regla de carrera
  firma/cancelación"), nunca por este endpoint.

**Respuesta 200/201 (metadata):**

```json
{
  "solicitudFirmaId": "uuid",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionSolicitud": 0,
  "revisionArtefacto": 3,
  "contentType": "application/pdf",
  "contentLength": 245678,
  "hashAlgoritmo": "SHA-256",
  "hashEntrada": "sha256:..."
}
```

**Idempotencia:**

| Caso | Resultado |
|---|---|
| Misma `solicitudFirmaId` + misma `revisionSolicitud` | Mismos cuatro valores persistidos en el requisito (idempotente, verificado contra base); no vuelve a renderizar ni a incrementar `revisionArtefacto` |
| `revisionSolicitud` incompatible con el estado actual | `409 Conflict` |
| Requisito en `CANCELACION_SOLICITADA` o terminal | `409 Conflict` (`REQUISITO_NO_ACEPTA_PREPARACION`) |
| Documento no numerado bajo `AL_FIRMAR` | `422 Unprocessable Entity` |
| `solicitudFirmaId` no existe | `404 Not Found` |
| Sin `X-Api-Key` válida | `401 Unauthorized` |

## 2. Descarga del artefacto

```http
GET /api/faltas/integraciones/firmas/solicitudes/{solicitudFirmaId}/artefacto
```

- streaming `application/pdf`, sin cargar el binario completo en memoria;
- autorización M2M (`X-Api-Key`, ver `eventing-operations.md`);
- descarga exclusivamente la revisión persistida en
  `fal_documento_firma_req.revision_artefacto_preparada`/
  `hash_entrada_preparado`/`content_length_preparado` (`DEC-EDA-030`),
  nunca una noción genérica de "documento vigente" resuelta de otra
  forma — si otra firma concurrente sobre otro requisito del mismo
  documento ya avanzó `fal_documento.revision_artefacto` más allá de la
  preparación de este requisito, la descarga sigue sirviendo exactamente
  el PDF de la revisión preparada (todavía físicamente accesible; el
  storage no se sobrescribe entre revisiones);
- **solo se permite la descarga de una `revisionArtefacto` ya preparada
  antes de una eventual `CANCELACION_SOLICITADA`** (§1bis); una descarga
  de una revisión que todavía no fue preparada cuando el requisito ya está
  en `CANCELACION_SOLICITADA` o terminal responde `409 Conflict`
  (`REQUISITO_NO_ACEPTA_PREPARACION`);
- valida que la `revisionArtefacto` solicitada (query param `revision`,
  opcional; por defecto la persistida en el requisito) coincida con la
  persistida — si no, `409`;
- devuelve `Content-Length` exacto;
- devuelve header `Digest: sha-256=<base64>` (RFC 3230) con el mismo valor
  que `hashEntrada` de la preparación;
- nunca revela `storageKey`, bucket ni ruta física;
- nunca acepta ni redirige a una URL externa;
- límite `120 req/min` por credencial, burst `20`
  (`../50-persistence/firma-eda-physical-model.md` §15); excedido responde
  `429 Too Many Requests` con `Retry-After`.

**Errores:** `404` (solicitud inexistente), `409` (revisión ya no vigente
o requisito no acepta preparación/descarga nueva), `401` (sin `X-Api-Key`
válida), `403` (aplicación sin permiso de descarga).

## 3. Carga del candidato firmado (staging temporal — sin efecto de dominio)

```http
PUT /api/faltas/integraciones/firmas/solicitudes/{solicitudFirmaId}/resultados/{resultadoFirmaId}
Content-Type: multipart/form-data
```

Límite `20 req/min` por credencial, burst `5`
(`../50-persistence/firma-eda-physical-model.md` §15); excedido responde
`429 Too Many Requests` con `Retry-After`.

**Partes obligatorias:**

- `metadata`: JSON (ver esquema abajo);
- `archivo`: `application/pdf`.

**Metadata:**

```json
{
  "resultadoFirmaId": "uuid",
  "solicitudFirmaId": "uuid",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionSolicitud": 0,
  "revisionEntrada": 3,
  "revisionSalida": 4,
  "hashAlgoritmo": "SHA-256",
  "hashEntrada": "sha256:...",
  "hashSalida": "sha256:...",
  "referenciaFirmaExt": "FIR-2026-000123",
  "evidenciaId": "FIR-EVID-2026-000123"
}
```

**Reglas de procesamiento (staging únicamente; ver §0bis para el efecto de
dominio real):**

1. la carga se escribe en `storage_key_candidato`: una clave interna,
   inmutable y ya definitiva desde el momento del `PUT` (nunca se escribe
   sobre el PDF vigente ni requiere un `rename`/`copy` posterior; el PDF
   vigente solo cambia de puntero en el paso 7 de §0bis, ejecutado al
   consumir `DocumentoFirmado`, nunca en este endpoint);
2. valida MIME real (`application/pdf` verificado por contenido, no solo
   por header), estructura PDF válida, tamaño máximo `104857600` bytes
   (100 MiB — valor de configuración
   `faltas.integraciones.firmas.resultado.max-bytes`), que `hashEntrada`
   coincida exactamente con
   `fal_documento_firma_req.hash_entrada_preparado`, y que
   `revisionEntrada` coincida exactamente con
   `fal_documento_firma_req.revision_artefacto_preparada` de esa
   `solicitudFirmaId`/requisito (`DEC-EDA-030`; nunca contra
   `fal_documento.revision_artefacto`/`hash_docu` directamente, que
   pueden haber avanzado por otra firma concurrente sobre otro requisito
   del mismo documento);
3. deriva `upload_actor_id`/`upload_actor_app`/`upload_key_fingerprint`
   exclusivamente de la `X-Api-Key` autenticada de esta petición, nunca
   del JSON enviado por Firmas (`DEC-EDA-033`);
4. persiste (o recupera, si es idempotente) una fila en
   `fal_documento_firma_resultado`
   (`../50-persistence/firma-eda-physical-model.md` §4) con
   `estado_resultado = VALIDADO`, incluidos `referencia_firma_ext`,
   `evidencia_id` y los tres campos de actor derivados de la credencial;
   **no invoca** `RegistrarFirmaDocumentalCommand`, **no persiste** nada
   en `fal_documento_firma` ni en `fal_documento`;
5. no cambia `estado_firma_req`, no reemplaza `fal_documento.storage_key`,
   no elimina el PDF vigente anterior;
6. el candidato permanece disponible (inactivo, sin ser referenciado por
   `fal_documento`) para que el consumidor de `DocumentoFirmado` lo
   aplique (§0bis); la limpieza del candidato ocurre únicamente si el
   candidato expira sin consumirse (§4) — si se consume, el candidato se
   convierte en el PDF vigente (swap de puntero, nunca una copia), y lo
   que se limpia de forma asíncrona es el PDF **anterior**
   (`storage_key_anterior`, `../50-persistence/firma-eda-physical-model.md`
   §5bis).

Este endpoint nunca puede fallar "a mitad de la aplicación del dominio"
porque nunca aplica dominio: solo persiste o rechaza el recibo técnico.

**Idempotencia:**

| Caso | Resultado |
|---|---|
| Mismo `resultadoFirmaId` + mismo `hashSalida` | `200 OK` idempotente, mismo recibo (mismo estado de `fal_documento_firma_resultado`) |
| Mismo `resultadoFirmaId` + `hashSalida` distinto | `409 Conflict` |
| `hashEntrada` no coincide con el vigente | `409 Conflict` |
| `revisionEntrada` no corresponde a una preparación previa válida | `409 Conflict` |
| Archivo no es un PDF válido / excede el tamaño máximo | `422 Unprocessable Entity` |
| Sin `X-Api-Key` válida | `401 Unauthorized` |

**Nota sobre validación PKI en esta carga:** este contrato HTTP no
transporta metadata PKI (`pki.*`); esa evidencia viaja exclusivamente en
el evento `DocumentoFirmado` (`../40-api/firma-event-contracts.md` §4).
Por lo tanto, una PKI inválida nunca se detecta en este `PUT` — se detecta
y se resuelve exclusivamente al consumir el evento (cuarentena, sin
aplicar dominio; `../10-domain/firma-notificacion-fallo.md`, "Firma
digital y evidencia PKI").

**Respuesta exitosa (recibo técnico) — campos exactos, sin placeholders:**

```json
{
  "resultadoFirmaId": "uuid",
  "solicitudFirmaId": "uuid",
  "documentoId": 1234,
  "seqFirmaReq": 1,
  "revisionEntrada": 3,
  "revisionSalida": 4,
  "hashSalida": "sha256:...",
  "estadoResultado": "VALIDADO",
  "recibidoEn": "2026-08-01T15:04:00Z",
  "expiraEn": "2026-08-02T15:04:00Z"
}
```

Ningún campo de este recibo expone `storageKey`, bucket ni ruta física, ni
ningún otro campo no listado arriba. El binario **no viaja** en el evento
`DocumentoFirmado` (`../40-api/firma-event-contracts.md` §4); el evento
solo referencia `resultadoFirmaId`.

## 4. Retención de resultados no consumidos

TTL: **24 horas** desde la carga (`fal_documento_firma_resultado.expires_at`).
Job de limpieza idempotente (ver `eventing-operations.md` §4) marca
`estado_resultado = EXPIRADO` y elimina el objeto de storage referenciado
por `storage_key_candidato` (que pasa a `NULL`) de los candidatos no
consumidos tras el TTL; el recibo técnico y su metadata persistida en
`fal_documento_firma_resultado` no se eliminan (quedan como evidencia de
la carga, aun si el candidato ya fue purgado). Este job es distinto del
job de limpieza de `storage_key_anterior` sobre candidatos ya `CONSUMIDO`
(`../50-persistence/firma-eda-physical-model.md` §5bis paso 7): uno purga
candidatos nunca aplicados; el otro purga PDFs reemplazados tras aplicar
un candidato.
