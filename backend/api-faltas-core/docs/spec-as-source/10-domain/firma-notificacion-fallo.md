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
hashDocumento      (opcional)    -- hash del documento firmado
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

## 12. Prohibiciones vigentes

- No usar EstadoFalloActa.DICTADO -- eliminado.
- No usar EstadoFalloActa.FIRMADO -- eliminado.
- No usar fallo.setEstadoFallo(NOTIFICADO) directamente -- usar marcarNotificado().
- No usar fallo.setFechaNotificacion() directamente -- usar marcarNotificado().
- No usar idUserFirma en el request del callback.
- No crear una nueva `FalNotificacion` en `enviarNotificacion` si ya existe
  cualquier cabecera activa para el documento. Si esta en `PENDIENTE_ENVIO`,
  se reutiliza; si esta en otro estado activo, la operacion se rechaza.
