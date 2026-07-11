# Dominio: Firma y cola notificatoria del fallo

---

## 1. Estado canonico de `EstadoFalloActa`

El enum `EstadoFalloActa` contiene exactamente estos valores:

| Valor                   | Descripcion                                                              |
|-------------------------|--------------------------------------------------------------------------|
| `PENDIENTE_FIRMA`       | Fallo dictado; documento generado; pendiente de firma obligatoria.       |
| `PENDIENTE_NOTIFICACION`| Ultima firma completada; `fhFirma` registrado; cola notificatoria lista. |
| `NOTIFICADO`            | Resultado notificatorio positivo registrado; `fhNotificacion` registrado.|
| `FIRME`                 | Firmeza de condena declarada; `fhFirmeza` registrado.                    |
| `REEMPLAZADO`           | Fallo reemplazado por uno nuevo (estado lateral).                        |
| `SIN_EFECTO`            | Fallo anulado sin efecto (estado lateral).                               |

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

PENDIENTE_FIRMA
    -> PENDIENTE_NOTIFICACION   (marcarPendienteNotificacion: ultima firma obligatoria completada)
    -> REEMPLAZADO              (fallo reemplazado por uno nuevo)
    -> SIN_EFECTO               (fallo anulado)

PENDIENTE_NOTIFICACION
    -> NOTIFICADO               (marcarNotificado: resultado notificatorio positivo)
    -> REEMPLAZADO

NOTIFICADO
    -> FIRME                    (declararFirmeza: solo condenatorios)
    -> REEMPLAZADO

FIRME                           (estado terminal para condenatorios)
REEMPLAZADO                     (estado terminal lateral)
SIN_EFECTO                      (estado terminal lateral)

---

## 4. Operaciones de dominio en `FalActaFallo`

### `marcarPendienteNotificacion(LocalDateTime fhFirma)`
- Precondicion: `fhFirma` no nulo.
- Efecto: `this.fhFirma = fhFirma; this.estadoFallo = PENDIENTE_NOTIFICACION`.
- Quien llama: `DocumentoService.completarFirmaDocumento` cuando se completan todas
  las firmas obligatorias activas del documento de fallo.

### `marcarNotificado(LocalDateTime fhNotificacion)`
- Precondicion: `fhNotificacion` no nulo.
- Efecto: `this.fhNotificacion = fhNotificacion; this.estadoFallo = NOTIFICADO`.
- Quien llama: `NotificacionService.actualizarFalloNotificado` al registrar resultado positivo.

### `declararFirmeza(LocalDateTime fhFirmeza, OrigenFirmezaCondena origen)`
- Precondicion: ambos campos no nulos.
- Efecto: `siFirme = true; this.fhFirmeza = fhFirmeza; this.origenFirmeza = origen; estadoFallo = FIRME`.

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
La futura persistencia MariaDB debera ejecutar estos efectos dentro de una
transaccion para garantizar atomicidad.

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

Comportamiento canonico (FIX-FALLO-NOTI-01):

1. Verificar que el acta no este cerrada y que el documento este firmado.
2. Buscar notificacion activa en estado `PENDIENTE_ENVIO` para el documento.
3. Si existe: llamar `notif.iniciarEnvio(canal, ahora, ahora, "SISTEMA")` -- reutiliza la
   notificacion preparada por el callback de firma; no crea una nueva.
4. Si no existe: crear una nueva `FalNotificacion` en estado EN_PROCESO (flujo directo
   sin cola previa).
5. Emitir evento NOTENV y recalcular snapshot.

---

## 9. `LoteCorreoService`

### `generarLoteDesdePendientes`

```
generarLoteDesdePendientes(loteCodigo, referenciaExterna, guidLoteExt, idUser)
```

1. Consulta `NotificacionRepository.buscarPorEstado(PENDIENTE_ENVIO)`.
2. Si la lista esta vacia: lanza `PrecondicionVioladaException` (mensaje incluye "PENDIENTE_ENVIO").
3. Extrae los IDs y delega en `generarLoteConIntentos`.

Evento `LOTGEN` emitido por acta afectada (ya implementado en `generarLoteConIntentos`).

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
| Firma completa: fallo=PENDIENTE_NOTIFICACION, notif=PENDIENTE_ENVIO     | FirmaFalloNotificacionCanonicaTest   |
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

## 10b. Seguridad JWT

### Validacion en desarrollo/test

- JWT firmado con clave local ficticia (`HS256`).
- No se acepta `alg=none`.
- El secreto se obtiene de `faltas.security.jwt.dev-secret`; nunca se hardcodea en codigo Java productivo.
- Se validan firma, algoritmo, `exp`, `nbf` cuando esta presente, issuer y audience.
- El campo `sub` del token es el actor de dominio; `idUserFirma` en el body esta prohibido.

### Staging y produccion (pendiente)

- Pendiente activar validacion contra JWKS de TokenServer/.NET/OpenIddict.
- La activacion no cambia el contrato HTTP ni el actor de dominio.
- No exponer secretos reales ni URLs productivas en esta spec.

---

## 11. Prohibiciones vigentes

- No usar EstadoFalloActa.DICTADO -- eliminado.
- No usar EstadoFalloActa.FIRMADO -- eliminado.
- No usar fallo.setEstadoFallo(NOTIFICADO) directamente -- usar marcarNotificado().
- No usar fallo.setFechaNotificacion() directamente -- usar marcarNotificado().
- No usar idUserFirma en el request del callback.
- No crear FalNotificacion nueva en enviarNotificacion si ya existe una en PENDIENTE_ENVIO.
- No implementar MariaDB/JDBC en este slice.
