# Dominio: Firma y cola notificatoria del fallo

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Fuente propietaria del lifecycle de `EstadoFalloActa`, el callback de firma real
> y la cola notificatoria del fallo (ver README).

---

## 1. Estado canonico de `EstadoFalloActa`

El enum `EstadoFalloActa` contiene exactamente estos valores:

| Valor                   | Descripcion                                                              |
|-------------------------|--------------------------------------------------------------------------|
| `PENDIENTE_FIRMA`       | Fallo dictado; documento generado; pendiente de firma obligatoria.       |
| `PENDIENTE_NOTIFICACION`| Ultima firma obligatoria confirmada; `fhFirma` registrado; fallo listo para continuar el circuito notificatorio. La cabecera `FalNotificacion` se prepara cuando la plantilla es notificable. |
| `NOTIFICADO`            | Resultado notificatorio positivo registrado; `fhNotificacion` registrado.|
| `FIRME`                 | Firmeza de condena declarada; `fhFirmeza` registrado.                    |
| `REEMPLAZADO`           | Fallo reemplazado por uno nuevo (estado lateral).                        |
| `SIN_EFECTO`            | Fallo invalidado o dejado sin efecto (estado lateral terminal).          |

Prohibicion: `DICTADO` y `FIRMADO` no son estados del enum. Son hechos historicos
persistidos en los campos `fhDictado` y `fhFirma` respectivamente. No deben
reintroducirse como valores de enum ni como aliases en codigo o documentacion.

---

## 2. Hitos persistentes en `FalActaFallo`

| Campo           | Tipo            | Significado                                                     |
|-----------------|-----------------|-----------------------------------------------------------------|
| `fhDictado`     | `LocalDateTime` | Momento en que se dicto el fallo. Inmutable desde el dictado.  |
| `fhFirma`       | `LocalDateTime` | Momento en que se completaron todas las firmas obligatorias.   |
| `fhNotificacion`| `LocalDateTime` | Momento en que se registro el resultado notificatorio positivo.|
| `fhFirmeza`     | `LocalDateTime` | Momento en que se declaro la firmeza de condena.               |

Estos campos son hechos de negocio. No deben usarse para inferir el estado:
el estado canonico siempre esta en `estadoFallo`.

---

## 3. Ciclo de vida del fallo: transiciones de estado

Lifecycle principal:

```text
PENDIENTE_FIRMA
    --ultima firma obligatoria confirmada-->
PENDIENTE_NOTIFICACION
    --resultado notificatorio positivo-->
NOTIFICADO
    --declaracion valida de firmeza; solo fallo CONDENATORIO-->
FIRME
```

Estados laterales terminales:

- `REEMPLAZADO`: el fallo fue sustituido por otro y deja de ser vigente.
- `SIN_EFECTO`: el fallo fue invalidado o dejado sin efecto.

Este documento no define por analogia todos los estados de origen permitidos
para las transiciones laterales. Cada transicion hacia `REEMPLAZADO` o
`SIN_EFECTO` debe estar autorizada por su comando, precondiciones e invariantes
especificos.

No se permite retroceder ni saltear estados dentro del lifecycle principal.

---

## 4. Operaciones de dominio en `FalActaFallo`

### `marcarPendienteNotificacion(LocalDateTime fhFirma)`
- Precondiciones: `estadoFallo = PENDIENTE_FIRMA`; `fhFirma` no nulo.
- Efecto: `this.fhFirma = fhFirma; this.estadoFallo = PENDIENTE_NOTIFICACION`.
- Quien llama: `DocumentoService.completarFirmaDocumento` cuando se completan todas
  las firmas obligatorias activas del documento de fallo.

### `marcarNotificado(LocalDateTime fhNotificacion)`
- Precondiciones: `estadoFallo = PENDIENTE_NOTIFICACION`; `fhFirma` ya registrado; `fhNotificacion` no nulo.
- Efecto: `this.fhNotificacion = fhNotificacion; this.estadoFallo = NOTIFICADO`.
- Quien llama: `NotificacionService.actualizarFalloNotificado` al registrar resultado positivo.

### `declararFirmeza(LocalDateTime fhFirmeza, OrigenFirmezaCondena origen)`

Regla normativa:

- solo fallo CONDENATORIO;
- `estadoFallo = NOTIFICADO`;
- `fhFirmeza` no nulo;
- `origen` no nulo.

Efectos:

- `siFirme = true`;
- `fhFirmeza = valor recibido`;
- `origenFirmeza = origen`;
- `estadoFallo = FIRME`.

---

## 5. Callback de Firmas: `RegistrarFirmaDocumental`

Endpoint: `POST /api/faltas/documentos/{documentoId}/firmar-real`

### Autenticacion

- Requiere Bearer JWT obligatorio.
- Sin Bearer: HTTP 401. No se procesa la request.
- El actor se extrae exclusivamente de `ActorContextHolder.get().sub()` (campo `sub` del JWT).
- No se acepta `idUserFirma` en el body. El actor del body esta prohibido.

### Seguridad declarada

- `SecurityConfig`: ruta `/api/faltas/documentos/*/firmar-real` marcada como `authenticated()`.
- `JwtActorFilter`: URI terminada en `/firmar-real` es ruta protegida; sin token devuelve 401.

### Request

```
seqFirmaReq        (positivo)    -- posicion en la secuencia de firmantes requeridos
idFirmante         (no nulo)     -- identificador del firmante
tipoFirma          (no nulo)     -- tipo de firma (enum TipoFirma)
referenciaFirmaExt (obligatorio) -- clave de idempotencia emitida por la app Firmas
hashDocumento      (opcional)    -- hash de entrada: hash del PDF vigente exacto que Firmas recibio y firmo, calculado antes de aplicar esta firma (ver "Integracion EDA con Firmas" mas abajo)
storageKey         (opcional)    -- clave de storage del documento
```

### Codigos de respuesta

| Condicion                                 | HTTP |
|-------------------------------------------|------|
| Sin Bearer                                | 401  |
| Primera ejecucion exitosa                 | 201  |
| Reintento con misma `referenciaFirmaExt` (identico) | 200 |
| Datos incompatibles con firma ya existente | 422 |

### Comportamiento idempotente

- Si ya existe una `FalDocumentoFirma` con la misma `referenciaFirmaExt`:
  - devuelve HTTP 200 con los datos de la firma existente.
  - no crea registros nuevos ni dispara efectos secundarios.
  - rechaza con `PrecondicionVioladaException` si los datos del reintento son incompatibles
    (documento, seqFirmaReq, firmante, tipoFirma, hash o storageKey distintos).
- Si es la primera vez: crea la firma, aplica efectos, devuelve HTTP 201.

### Resultado: `RegistrarFirmaDocumentalResultado`

```java
record RegistrarFirmaDocumentalResultado(FalDocumentoFirma firma, boolean yaExistia)
```

---

## 6. Efectos al completar la ultima firma obligatoria

El helper privado `DocumentoService.completarFirmaDocumento` agrupa estos efectos
como una unica unidad de aplicacion cuando se completan todas las firmas
obligatorias activas del documento.

La implementacion InMemory no proporciona rollback transaccional conjunto.
La persistencia MariaDB debe ejecutar estos efectos dentro de una única
transacción para garantizar atomicidad.

1. `FalDocumento.estadoDocu = FIRMADO` -- guardado en DocumentoRepository.
2. `FalActaFallo.marcarPendienteNotificacion(ahora)` -- si el documento es el asociado
   al fallo activo del acta.
3. Preparacion de `FalNotificacion` en `PENDIENTE_ENVIO` -- solo si la plantilla del
   documento es notificable (siNotificable = true) y no existe ya una notificacion
   activa para el documento (buscarActivaPorDocumento retorna vacio).
4. Evento `DOCFIR` registrado en ActaEventoRepository.
5. Recalculo de snapshot: SnapshotRecalculador.recalcular y guardado.

---

## 7. Preparacion de `FalNotificacion` (estado inicial: PENDIENTE_ENVIO)

La factory `FalNotificacion.preparar(...)` crea una notificacion lista para cola:

| Campo        | Valor inicial  |
|--------------|----------------|
| `estado`     | `PENDIENTE_ENVIO` |
| `resultado`  | null           |
| `intentos`   | 0              |
| `canal`      | null           |
| `fechaEnvio` | null           |

Esta preparacion no emite el evento NOTENV. Es la antesala del envio real.

### Invariante de unicidad

Por documento debe existir como maximo una notificacion activa (estado != `SIN_EFECTO`)
a la vez. El metodo `NotificacionRepository.buscarActivaPorDocumento(idDocumento)` verifica
este invariante antes de preparar una nueva.

`InMemoryNotificacionRepository.guardar` rechaza con `PrecondicionVioladaException`
si se intenta guardar una notificacion activa cuando ya existe otra activa distinta
para el mismo documento.

---

## 8. `NotificacionService.enviarNotificacion`

La operacion debe consultar una unica vez la notificacion activa del documento
mediante `buscarActivaPorDocumento`.

Reglas:

1. Verificar que el acta no este cerrada y que el documento este firmado.
2. Si no existe ninguna notificacion activa:
   - crear una nueva cabecera en `EN_PROCESO`;
   - completar canal, fecha de envio y primer intento.
3. Si existe una notificacion activa en `PENDIENTE_ENVIO`:
   - reutilizar la misma cabecera;
   - ejecutar `iniciarEnvio`;
   - pasarla a `EN_PROCESO`;
   - no cambiar su identidad.
4. Si existe una notificacion activa en cualquier otro estado:
   - rechazar con `PrecondicionVioladaException`;
   - no crear otra cabecera;
   - no emitir eventos;
   - no modificar bloque ni snapshot.
5. Solo despues de completar correctamente el inicio del envio:
   - emitir `NOTENV`;
   - recalcular el snapshot.

Por documento puede existir como maximo una notificacion activa.

---

## 9. `LoteCorreoService`

### `generarLoteDesdePendientes`

```
generarLoteDesdePendientes(loteCodigo, referenciaExterna, guidLoteExt, idUser)
```

1. Consulta `NotificacionRepository.buscarPorEstado(PENDIENTE_ENVIO)`.
2. Si la lista esta vacia: lanza `PrecondicionVioladaException` (mensaje incluye "PENDIENTE_ENVIO").
3. Extrae los IDs y delega en `generarLoteConIntentos`.

`generarLoteConIntentos` emite el evento `LOTGEN` por cada acta afectada.

### `generarLoteConIntentos` - Validacion total

Precondiciones validadas ANTES de cualquier cambio de estado:

1. `notificacionIds` no nulo y no vacio.
2. Sin IDs duplicados en `notificacionIds`.
3. `loteCodigo` no existe ya en `LoteCorreoRepository`.
4. Cada notificacion existe en el repositorio.
5. Cada notificacion esta en estado `PENDIENTE_ENVIO`.
6. Ninguna notificacion tiene resultado previo (`resultado == null`).

Si alguna precondicion falla: `PrecondicionVioladaException`; sin cambios de estado.

### Canal, fecha e intentos al incorporar al lote

Cuando una notificacion se incorpora al lote, se llama `notif.iniciarEnvio(canal, fechaEnvio, fhUltMod, actor)`:

- `canal`: `"CORREO_POSTAL"` (nombre del enum `CanalNotificacion.CORREO_POSTAL`).
- `fechaEnvio`: instante unico capturado al inicio de la operacion.
- `intentos`: incrementado en 1 por `iniciarEnvio`.
- `estado`: avanza a `EN_PROCESO`.

`iniciarEnvio` requiere que el estado sea `PENDIENTE_ENVIO`; de lo contrario lanza
`PrecondicionVioladaException`.

---

## 10. Cobertura de tests requerida

| Escenario                                                               | Clase de test                        |
|-------------------------------------------------------------------------|--------------------------------------|
| EstadoFalloActa tiene exactamente 6 valores canonicos                   | FirmaFalloNotificacionCanonicaTest   |
| DICTADO y FIRMADO no existen en el enum                                 | FirmaFalloNotificacionCanonicaTest   |
| Firma completa de plantilla notificable: fallo=PENDIENTE_NOTIFICACION, notif=PENDIENTE_ENVIO     | FirmaFalloNotificacionCanonicaTest   |
| enviarNotificacion reutiliza PENDIENTE_ENVIO (no duplica)               | FirmaFalloNotificacionCanonicaTest   |
| generarLoteDesdePendientes genera lote desde PENDIENTE_ENVIO            | FirmaFalloNotificacionCanonicaTest   |
| Resultado positivo: fallo=NOTIFICADO via marcarNotificado               | FirmaFalloNotificacionCanonicaTest   |
| Reintento identico (referenciaFirmaExt): yaExistia=true, sin registros nuevos | DocumentoFirmaRealTest         |
| Payload incompatible: PrecondicionVioladaException                      | DocumentoFirmaRealTest               |
| POST /firmar-real sin Bearer -> HTTP 401                                | JwtActorFilterTest, DocumentoFirmaCallbackSecurityIT |
| POST /firmar-real con Bearer valido -> no es 401                        | DocumentoFirmaCallbackSecurityIT     |
| Controller: yaExistia=false -> HTTP 201                                 | DocumentoFirmaCallbackControllerTest |
| Controller: yaExistia=true -> HTTP 200                                  | DocumentoFirmaCallbackControllerTest |
| Controller: idUserFirma viene del sub del JWT, no del body              | DocumentoFirmaCallbackControllerTest |
| Request no contiene idUserFirma                                         | DocumentoFirmaCallbackControllerTest |
| Idempotencia concurrente: dos callbacks, una sola firma y un solo DOCFIR  | DocumentoFirmaRealTest                                                   |
| Transicion invalida PENDIENTE_FIRMA -> NOTIFICADO rechazada               | FirmaFalloNotificacionCanonicaTest                                       |
| Segunda transicion a PENDIENTE_NOTIFICACION rechazada                     | FirmaFalloNotificacionCanonicaTest                                       |
| Segundo iniciarEnvio desde EN_PROCESO rechazado                           | NotificacionAcuseLoteServiceTest                                         |
| Lote rechaza ID inexistente                                               | NotificacionAcuseLoteServiceTest                                         |
| Lote rechaza IDs duplicados                                               | NotificacionAcuseLoteServiceTest                                         |
| Lote rechaza notificacion EN_PROCESO                                      | NotificacionAcuseLoteServiceTest                                         |
| Lote no muta repositorios cuando falla la validacion previa               | NotificacionAcuseLoteServiceTest                                         |
| JWT con firma invalida, vencido, issuer/audience incorrectos o alg=none -> 401 | JwtActorFilterTest, DocumentoFirmaCallbackSecurityIT              |

---

## 11. Seguridad JWT

### Validacion en desarrollo/test

- JWT firmado con clave local ficticia (`HS256`).
- No se acepta `alg=none`.
- El secreto se obtiene de `faltas.security.jwt.dev-secret`; nunca se hardcodea en codigo Java productivo.
- Se validan firma, algoritmo, `exp`, `nbf` cuando esta presente, issuer y audience.
- El campo `sub` del token es el actor de dominio; `idUserFirma` en el body esta prohibido.

### Staging y produccion

- Los JWT deben validarse contra el JWKS publicado por
  TokenServer/.NET/OpenIddict.
- Deben validarse firma, algoritmo permitido, `exp`, `nbf` cuando este
  presente, issuer, audience y `sub`.
- El secreto simetrico ficticio de desarrollo/test no puede utilizarse en
  staging ni produccion.
- La validacion mediante JWKS no cambia el contrato HTTP ni el origen del actor
  de dominio: el actor continua siendo el `sub` autenticado.
- No se exponen secretos reales ni URLs productivas en esta spec.

---

## 11bis. Integración EDA con Firmas

Esta sección es normativa para la integración por eventos entre Faltas y
Firmas (Outbox/Bus/Inbox municipal). El callback síncrono `firmar-real` (§5)
y el consumidor de eventos comparten exactamente el mismo caso de uso de
aplicación (`DocumentoService.registrarFirmaDocumental`,
`RegistrarFirmaDocumentalCommand`, CMD-FALLO-001); ninguno de los dos
duplica lógica de dominio.

### Ownership Faltas / Firmas

- **Faltas** es autoridad de contenido legal, Markdown fuente, plantillas,
  numeración, renderizado/consolidación del PDF, documento y su ciclo de
  vida, requisito concreto/rol/mecanismo/obligatoriedad/orden de firma,
  asignación al requisito, y ubicación física final del PDF vigente
  (`storageKey`, nunca expuesto a Firmas).
- **Firmas** es el maestro global de identidad, credenciales y PKI de
  firmantes; nunca interpreta ni modifica el contenido legal; nunca
  renderiza Markdown; siempre firma un PDF definitivo (`application/pdf`)
  que Faltas preparó o expuso; valida el hash antes de firmar; aplica la
  firma y devuelve el resultado y la evidencia.
- Faltas mantiene `FalFirmante`/`FalFirmanteVersion`/`FalFirmanteVersionHabilitacion`
  como **proyección local versionada** de la identidad que Firmas gobierna
  globalmente (no una autoridad originaria de Faltas). Los eventos de
  firmante (`FirmanteCreado`, `FirmanteVersionCreada`,
  `FirmanteHabilitacionModificada`, `DelegacionFirmanteModificada`, ver
  `40-api/firma-event-contracts.md` §9) actualizan esa proyección de forma
  idempotente (bootstrap, incremental, versionado, reconciliación,
  detección de huecos), sin invocar nunca `RegistrarFirmaDocumental` ni
  mover documento/fallo/notificación.

### Lifecycle único de solicitud/requisito de firma (`DEC-EDA-016`)

`fal_documento_firma_req` es la única fuente de verdad del ciclo de vida de
un requisito de firma. El catálogo `EstadoFirmaReq` (columna
`estado_firma_req`) se extiende de forma aditiva (sin renombrar ni alterar
los cinco valores ya cerrados en 8C-4) para cubrir el lifecycle completo de
la integración EDA:

| Código | Valor | Naturaleza | Origen |
|---|---|---|---|
| 1 | `PENDIENTE` | Transitorio (cubre `PENDIENTE_ENVIO`/`SOLICITADA`/`ACEPTADA`/`EN_FIRMA`) | Existente, sin cambios |
| 2 | `FIRMADO` | Terminal (`FIRMADA`) | Existente, sin cambios |
| 3 | `ANULADO` | Terminal (`CANCELADA`, motivo `DOCUMENTO_ANULADO`) | Existente, sin cambios |
| 4 | `VENCIDO` | Terminal (`VENCIDA`) | Existente, sin cambios |
| 5 | `REEMPLAZADO` | Terminal (`CANCELADA`, motivo `REQUISITO_REEMPLAZADO`) | Existente, sin cambios |
| 6 | `CANCELACION_SOLICITADA` | Transitorio | Nuevo (`DEC-EDA-016`) |
| 7 | `CANCELADO` | Terminal (`CANCELADA`, motivos `FLUJO_REVERTIDO`/`CANCELACION_AUTORIZADA`/`ERROR_FUNCIONAL_IRRECUPERABLE`) | Nuevo (`DEC-EDA-016`) |
| 8 | `RECHAZADO` | Terminal (`RECHAZADA`) | Nuevo (`DEC-EDA-016`) |
| 9 | `RECHAZADO_CONTRATO` | Terminal (`RECHAZADA_CONTRATO`) | Nuevo (`DEC-EDA-016`) |

**Decisión física única (sin alternativa):** las cuatro sub-fases
pre-terminales del vocabulario conceptual (`PENDIENTE_ENVIO`, `SOLICITADA`,
`ACEPTADA`, `EN_FIRMA`) no reciben cada una su propio código; todas
colapsan en el código físico `1=PENDIENTE` ya existente, y se distinguen
operativamente mediante los timestamps nuevos `fh_solicitud`,
`fh_aceptacion`, `fh_inicio_firma` (`DEC-EDA-020`) y la traza del bus, no
mediante proliferación de códigos de estado. Los códigos `ANULADO`(3) y
`REEMPLAZADO`(5) existentes se reutilizan tal cual para sus dos motivos de
cancelación específicos; el código nuevo `CANCELADO`(7) cubre exclusivamente
los tres motivos de cancelación que no tenían representación previa.

Motivo estructurado de cancelación (columna nueva `motivo_terminal_firma_req`,
significado condicionado por `estado_firma_req`; `DEC-EDA-020`), aplicable
cuando `estado_firma_req = CANCELADO`:

| Código | Motivo |
|---|---|
| 3 | `FLUJO_REVERTIDO` |
| 4 | `CANCELACION_AUTORIZADA` |
| 5 | `ERROR_FUNCIONAL_IRRECUPERABLE` |

(Los códigos 1=`DOCUMENTO_ANULADO` y 2=`REQUISITO_REEMPLAZADO` del mismo
catálogo de motivos no requieren poblar esta columna: ya están representados
sin ambigüedad por los estados `ANULADO`/`REEMPLAZADO` respectivamente; la
columna queda `NULL` en esos dos casos.)

Motivo estructurado de rechazo humano, aplicable cuando
`estado_firma_req = RECHAZADO` (misma columna, catálogo distinto):

| Código | Motivo |
|---|---|
| 1 | `NO_CONFORME` |
| 2 | `DATOS_INCORRECTOS` |
| 3 | `DOCUMENTO_INCOMPLETO` |
| 4 | `COMPETENCIA_INSUFICIENTE` |
| 5 | `OTRO_JUSTIFICADO` |

Cuando `estado_firma_req = RECHAZADO_CONTRATO`, la misma columna almacena un
código de `CodigoRechazoContrato` (`1=SCHEMA_NO_SOPORTADO`,
`2=CAMPO_OBLIGATORIO_AUSENTE`, `3=REFERENCIA_INCOMPATIBLE`,
`4=HASH_O_REVISION_INCONSISTENTE`, `5=REQUISITO_NO_PROCESABLE`). No se usa
texto libre como única clasificación en ningún caso; `detalle_terminal_firma_req`
(`VARCHAR(255) NULL`) admite un complemento descriptivo opcional y nunca
sustituye al código.

**Delegación aplicada:** no requiere columna nueva en
`fal_documento_firma_req`. Se deriva comparando `id_firmante_asig`/
`ver_firmante_asig` (firmante asignado al requisito) contra `id_firmante`/
`ver_firmante` de la fila resultante en `fal_documento_firma` (firmante que
efectivamente firmó): si difieren, hubo delegación. Es el único mapeo
definitivo; no existe columna `si_delegacion_aplicada` redundante.

**Regla de carrera firma/cancelación (`DEC-EDA-016` §6.2):** mientras
`estado_firma_req = CANCELACION_SOLICITADA`, puede llegar todavía un
`DocumentoFirmado` para la misma revisión. El primer evento terminal
aplicado válidamente gana: si `DocumentoFirmado` se aplica primero, la
`SolicitudFirmaCanceladaConfirmada` posterior se registra como obsoleta sin
alterar `FIRMADO`; si la confirmación de cancelación se aplica primero, un
`DocumentoFirmado` posterior para la misma `revisionSolicitud` se rechaza
como evento obsoleto. La resolución usa `estado_firma_req` persistido,
`revision_solicitud` y `version_row` (OCC) — nunca únicamente el orden de
llegada cuando las revisiones no coinciden.

Modelo físico completo de `fal_documento_firma_req`
(identidad/columnas/claves/índices/checks): `50-persistence/mariadb-logical-model.md`
y `50-persistence/firma-eda-physical-model.md`.

### Único PDF vigente y encadenamiento de hashes (`DEC-EDA-004`/`013`/`014`/`015`)

Faltas conserva únicamente el PDF vigente más reciente de cada documento;
no se conservan binarios intermedios por firma. `fal_documento.storage_key`/
`hash_docu` son la fuente de verdad de ese único PDF vigente en todo
momento (no existe ni se crea una tabla 1:1 adicional para historial de
artefactos). Para firmas múltiples:

```text
PDF vigente sin firma
→ PDF vigente con firma 1
→ PDF vigente con firmas 1 + 2
→ ...
→ PDF vigente final
```

El reemplazo del PDF vigente debe ser seguro: recibir el resultado en una
ubicación temporal (`fal_documento_firma_resultado`,
`50-persistence/firma-eda-physical-model.md` §4 — nunca escribir sobre el
PDF vigente desde el `PUT` HTTP), validar
solicitud/requisito/hashes/identidad/firma/PKI **al consumir el evento
`DocumentoFirmado`** (único punto de aplicación de dominio,
`30-integrations/firma-binary-transfer.md` §0bis), persistir los efectos
de dominio, cambiar la referencia al nuevo PDF vigente e incrementar
`fal_documento.revision_artefacto` (`50-persistence/firma-eda-physical-model.md`
§3), eliminar el PDF anterior solo después del éxito, y limpiar
temporales. Si falla antes del reemplazo, el PDF anterior permanece
vigente. No se reutiliza `marcarEmitido(...)` para congelar el PDF antes de
la primera firma si eso fuerza prematuramente `EstadoDocu.EMITIDO`.

Por cada firma se registra `hashEntrada` (hash del PDF vigente exacto que
Firmas recibió y firmó, antes de aplicar la firma) y `hashSalida` (hash del
PDF vigente resultante después de aplicarla), con la regla de encadenamiento
`firmaN.hashSalida == firmaN+1.hashEntrada`.
`RegistrarFirmaDocumentalCommand.hashDocumento`/`fal_documento_firma.hash_firma`
representa `hashEntrada`. `hash_salida VARCHAR(128) NULL` es la columna
física nueva y definitiva para `hashSalida` (`DEC-EDA-021`; definida en
`50-persistence/mariadb-logical-model.md` y en el DDL de este cierre;
`DDL_NOT_EXECUTED` hasta ejecución manual autorizada).
`fal_documento_firma.storage_key` no es fuente de verdad de ninguna
ubicación física por firma; queda `NULL`/sin uso en el circuito EDA
(`DEC-EDA-015`).

### `AL_FIRMAR`: orden normativo numeración → render → firma

Orden normativo cuando la plantilla requiere numeración `AL_FIRMAR`:

```text
1. Faltas numera.
2. Faltas renderiza/consolida el PDF vigente.
3. Faltas calcula hashEntrada.
4. Firmas recupera y verifica el PDF (preparar-artefacto + descarga).
5. Firmas aplica la firma.
6. Firmas calcula hashSalida.
7. Faltas registra la firma y reemplaza el PDF vigente.
```

Faltas es quien renderiza/consolida el PDF y calcula `hashEntrada` en todo
momento; Firmas nunca renderiza contenido ni calcula el hash de entrada — su
única responsabilidad de hash es `hashSalida`, tras aplicar la firma.

**Tarea de implementación pendiente (exacta, sin alternativa):** modificar
`DocumentoService.registrarFirmaDocumental` para que, cuando
`plantilla.getMomentoNumeracionDocu() == AL_FIRMAR` y `doc.getNroDocu() == null`,
lance `PrecondicionVioladaException` (HTTP 422) en vez de invocar
`numerarDocumento(...)` automáticamente; actualizar `test28` en
`DocumentoFirmaRealTest.java` para assertar ese rechazo (`test29` no cambia
de intención). Esta regla ya es `NORMATIVE` en
`20-application/fallo-command-contracts.md` (CMD-FALLO-001, "Regla de
numeración", "Orden de validaciones" paso 14); la tarea es exclusivamente de
código — `IMPLEMENTATION_NOT_STARTED` (ver `90-roadmap/current-roadmap.md`).

### Transferencia binaria y autenticación M2M (`DEC-EDA-017`/`018`)

El PDF vigente y el resultado firmado se transfieren exclusivamente por API
HTTP autenticada con streaming — nunca por URL temporal ni acceso directo al
storage. Los tres contratos normativos (preparación, descarga, carga de
resultado) y la autenticación M2M por `X-Api-Key` viven en
`30-integrations/firma-binary-transfer.md` y
`30-integrations/eventing-operations.md`. Ningún endpoint de esta
integración expone `storageKey`, bucket ni ruta física.

### Actor humano vs. actor técnico (`DEC-EDA-007`/`021`)

El consumidor de eventos `DocumentoFirmado` está autenticado como una
aplicación (Firmas), no como el funcionario que firmó. `idUserFirma`/
`firmanteEfectivo.idUser` (identidad humana) nunca se deriva del subject
técnico M2M del consumidor; el evento transporta esa identidad
explícitamente y Faltas la valida contra firmante, versión, habilitación,
delegación y requisito antes de aceptarla. El actor técnico (aplicación
Firmas autenticada M2M) se persiste por separado en `fal_documento_firma`
(`actor_tecnico_tipo`, `actor_tecnico_id`, `actor_tecnico_app`,
`actor_tecnico_key_fingerprint`, `correlation_id`, `event_id`,
`received_at`; `DEC-EDA-021`), obligatorio para resultados que llegan por
evento desde Firmas, nulo únicamente para firmas anteriores a esta
migración (compatibilidad histórica documentada en `ddl-decisions.md`), y
nunca sustituye a `id_user_firma`.

### Firma digital y evidencia PKI (`DEC-EDA-023`/`035`/`036`)

Firmas es la autoridad técnica de PKI. Perfil normativo único, sin
excepción: `PAdES Baseline-LT` (`PAdES-B-LT`; `50-persistence/firma-eda-physical-model.md`
§2.4bis) — un PDF `PAdES-B-T` no satisface el perfil obligatorio, sin
importar el resto de la evidencia. Algoritmo de hash mínimo `SHA-256`;
validación de cadena de certificación y de revocación (OCSP o CRL según
disponibilidad de la autoridad certificante); sello de tiempo confiable;
múltiples firmas preservan las firmas previas; no se re-renderiza el PDF
después de la primera firma. Faltas persiste la metadata mínima de
evidencia (perfil, algoritmo, serial/thumbprint/subject/issuer/vigencia
del certificado, resultado de cadena, fuente/resultado de revocación,
autoridad y momento de sello de tiempo, referencia de evidencia, resultado
y versión del validador) en `fal_documento_firma`
(`50-persistence/firma-eda-physical-model.md` §2.3-§2.4). Faltas valida el
recibo y la coherencia contractual; Firmas conserva la evidencia
criptográfica completa fuera de Faltas.

**Decisión final, sin alternativa: una PKI inválida no satisface el
requisito.** Al consumir `DocumentoFirmado`
(`40-api/firma-event-contracts.md` §4), Faltas exige simultáneamente:

```text
resultadoValidacion IN (VALIDO, VALIDO_CON_OBSERVACIONES_PERMITIDAS)
resultadoCadena = VALIDA
resultadoRevocacion = NO_REVOCADO
tsaInstante informado y no futuro respecto del reloj de Faltas
certificado_vigencia_desde <= tsaInstante <= certificado_vigencia_hasta
perfilFirma = 'PAdES-B-LT'
```

**Resolución local exacta cuando cualquiera de estas seis condiciones no
se cumple** (certificado revocado, cadena inválida, certificado fuera de
vigencia al `tsaInstante`, sello de tiempo inválido/ausente, perfil
distinto de `PAdES-B-LT`, o `resultadoValidacion = INVALIDO`) — una única
transacción local de procesamiento
(`50-persistence/firma-eda-physical-model.md` §5bis, paso 1a;
`30-integrations/eventing-operations.md` §3bis.2), sobre la fila de
`fal_inbox` ya insertada durante la ingestión (§3bis.1, PK
`bus_evento_id`; identidad lógica `evento_id`,
`uq_fal_inbox_evento_id`):

1. bloquear la fila de `fal_inbox` por `bus_evento_id`
   (`SELECT ... FOR UPDATE`; la fila ya existe desde la ingestión — esta
   transacción nunca la crea);
2. bloquear la fila de `fal_documento_firma_resultado` por
   `resultado_firma_id` (`SELECT ... FOR UPDATE`);
3. transicionar esa fila a `RECHAZADO` (`estado_resultado = 5`), con
   `event_id` = el `eventoId` de este `DocumentoFirmado`;
4. registrar un código estructurado de rechazo PKI en `last_error_code`
   (ej. `PKI_CADENA_INVALIDA`, `PKI_CERTIFICADO_REVOCADO`,
   `PKI_PERFIL_NO_PADES_B_LT`, `PKI_SELLO_TIEMPO_INVALIDO`) y el detalle en
   `last_error_detail`;
5. **no** crear `fal_documento_firma`, **no** satisfacer
   `fal_documento_firma_req` (permanece en su estado previo, sin
   transicionar a `FIRMADO`) y **no** reemplazar el PDF vigente de
   `fal_documento`;
6. cerrar `fal_inbox.status = PROCESADO`, `fal_inbox.si_error = FALSE`,
   `fal_inbox.procesado_en = UTC_TIMESTAMP(6)` — **igual que la rama
   válida**. El rechazo por PKI inválida es un **rechazo funcional de
   dominio**, no un error técnico reintentable: el evento fue procesado
   correctamente y su resultado de negocio fue "rechazar el candidato".
   Por eso **no** se inserta ninguna fila en `fal_inbox_error` (esa tabla
   es exclusiva de fallos técnicos de procesamiento,
   `50-persistence/firma-eda-physical-model.md` §6.6): el rechazo ya queda
   registrado, con su código y detalle, en
   `fal_documento_firma_resultado.last_error_code`/`last_error_detail`.

Esta transacción local es idempotente: un reintento de lectura del mismo
`bus_evento_id` (relay reintentando tras un crash antes de avanzar el
checkpoint local) encuentra `fal_inbox` ya cerrada en `PROCESADO` y no
repite el rechazo ni lo revierte.

**Es un rechazo funcional de dominio, no un error transitorio de
infraestructura ni un error técnico de procesamiento:** `bus_evento` no
registra ningún estado de consumo (no hay entrega central ni cuarentena
central que resolver, `50-persistence/firma-eda-physical-model.md` §6.0);
la resolución completa vive en `fal_inbox`/`fal_documento_firma_resultado`,
locales a Faltas, sin fila en `fal_inbox_error`. Una investigación
posterior (por qué Firmas produjo una PKI inválida) es responsabilidad
operativa fuera del bus, apoyada en el código estructurado persistido en
`fal_documento_firma_resultado`, nunca un reintento automático sobre esta
fila de `fal_inbox` (que ya quedó `PROCESADO`).

**No reuso de `resultadoFirmaId` (`DEC-EDA-036`):** una vez `RECHAZADO`,
ese `resultado_firma_id` queda agotado permanentemente. Firmas debe
producir un **nuevo** resultado (`PUT` con un **nuevo**
`resultadoFirmaId`) y un **nuevo** `evento_id` para `DocumentoFirmado`
(nunca el mismo `evento_id` reutilizado: un nuevo hecho de dominio siempre
recibe un nuevo `evento_id`, `eventing-operations.md` §5), después de
corregir la causa de la PKI inválida. El PDF vigente anterior de
`fal_documento` no cambia mientras esto ocurre. No se agrega ningún evento
de dominio nuevo para representar el rechazo (no existe hoy un contrato
normativo que lo requiera): un fallo de validación PKI se observa desde
Faltas por el código estructurado persistido en
`fal_documento_firma_resultado`.

Este contrato HTTP de carga (`30-integrations/firma-binary-transfer.md`
§3) no transporta metadata PKI; por lo tanto, una PKI inválida **nunca**
se detecta en el `PUT` — siempre se detecta al consumir el evento.

**Catálogo `ResultadoValidacionFirma` (`1=VALIDO, 2=INVALIDO,
3=VALIDO_CON_OBSERVACIONES_PERMITIDAS`).** `VALIDO_CON_OBSERVACIONES_PERMITIDAS`
es una lista cerrada de exactamente dos códigos, no un comodín; Firmas
solo puede reportar ese valor cuando la observación es exactamente una de
estas dos, y las seis condiciones de arriba (cadena `VALIDA`, revocación
`NO_REVOCADO`, sello de tiempo válido, certificado vigente al
`tsaInstante`, perfil `PAdES-B-LT`) siguen cumplidas sin excepción —
ninguna observación permitida rebaja el perfil ni ninguna de las cinco
condiciones de seguridad:

| Código de observación | Significado |
|---|---|
| `TSA_NO_CUALIFICADA_CONFIABLE` | El sello de tiempo proviene de una autoridad declarada confiable por Firmas pero no cualificada formalmente. |
| `REVOCACION_POR_CRL_SIN_OCSP` | La verificación de revocación se resolvió por CRL en lugar de OCSP por indisponibilidad temporal del servicio OCSP, sin que eso cambie el resultado `NO_REVOCADO`. |

Un PDF `PAdES-B-T` (sin material de validación a largo plazo embebido) no
satisface el perfil obligatorio (`DEC-EDA-035`) y se trata como
`INVALIDO`, nunca como una observación tolerada. Ninguna otra observación
habilita `VALIDO_CON_OBSERVACIONES_PERMITIDAS`; cualquier otra situación
no cubierta por esta lista que Firmas quisiera reportar como observación
se trata como `INVALIDO` a los efectos de esta integración.

### Storage encapsulado

Ningún contrato de la integración EDA expone `storageKey`, bucket ni ruta
física de Faltas hacia Firmas. Los identificadores funcionales permitidos en
los contratos de eventos son: `solicitudFirmaId`, `documentoId`,
`seqFirmaReq`, revisiones, `resultadoFirmaId`, `referenciaFirmaExt`,
hashes, referencia de evidencia.

### Eventos de integración: 12/12 `NORMATIVE`

El conjunto completo de contratos de eventos de la integración EDA
Faltas↔Firmas vive en `40-api/firma-event-contracts.md`: los 12 eventos
(`FirmaSolicitada`, `SolicitudFirmaAceptada`, `DocumentoFirmado`,
`SolicitudFirmaRechazadaPorContrato`, `FirmanteCreado`,
`FirmanteVersionCreada`, `FirmanteHabilitacionModificada`,
`DelegacionFirmanteModificada`, `SolicitudFirmaCancelada`,
`SolicitudFirmaCanceladaConfirmada`, `FirmaRechazada`,
`SolicitudFirmaVencida`) tienen contrato de dominio completo y cerrado
(`DEC-EDA-016` habilita el comando de dominio que faltaba para los cuatro
últimos). El modelo de transporte (Outbox/Bus/Inbox) vive en
`30-integrations/municipal-eventing.md`; su operación concreta
(`DEC-EDA-019`) en `30-integrations/eventing-operations.md`.

`DocumentoFirmado` es el único evento que puede invocar
`DocumentoService.registrarFirmaDocumental`; nunca el circuito legacy
`DocumentoService.firmarDocumento`. `SolicitudFirmaAceptada` es únicamente
ack técnico: nunca registra una firma, nunca satisface
`fal_documento_firma_req`, nunca genera `DOCFIR`, nunca mueve documento,
fallo o notificación.

## 12. Prohibiciones vigentes

- No usar EstadoFalloActa.DICTADO -- eliminado.
- No usar EstadoFalloActa.FIRMADO -- eliminado.
- No usar fallo.setEstadoFallo(NOTIFICADO) directamente -- usar marcarNotificado().
- No usar fallo.setFechaNotificacion() directamente -- usar marcarNotificado().
- No usar idUserFirma en el request del callback.
- No crear una nueva `FalNotificacion` en `enviarNotificacion` si ya existe
  cualquier cabecera activa para el documento. Si esta en `PENDIENTE_ENVIO`,
  se reutiliza; si esta en otro estado activo, la operacion se rechaza.
