# Contratos de comandos: firma, notificacion, firmeza y pago

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Ante contradiccion sobre CMD-FALLO-001..007 entre este documento y cualquier otro
> documento de la spec, este documento es normativo (ver introduccion y README).

## Introduccion

Este documento aplica el estandar definido en `00-governance/command-contract-standard.md`
a los siete comandos de aplicacion de la familia fallo.

Cada contrato describe la operacion de aplicacion completa, no metodos internos
aislados. Una operacion de dominio interna se documenta dentro del comando que
la invoca. Los setters y helpers privados no son comandos.

Los nombres canonicos son conceptuales. Los nombres Java y los endpoints se
indican como mapeo tecnico.

Ante contradiccion sobre estos siete comandos entre este documento y cualquier
otro documento de la spec, este documento es normativo.

---

## CMD-FALLO-001 Confirmar firma documental real

**ID normativo:** CMD-FALLO-001

**Nombre canonico:** Confirmar firma documental real

**Proposito:** Registrar la firma real de un documento por un firmante autenticado via
callback de la aplicacion de Firmas. Cuando se completan todas las firmas obligatorias
del documento de fallo, desencadena el avance del lifecycle del documento y del fallo.

**Canales de entrada:**
`POST /api/faltas/documentos/{documentoId}/firmar-real`
Servicio: `DocumentoService.registrarFirmaDocumental`

**Actor y autorizacion:**
- actor autenticado: sub del JWT, extraido exclusivamente de `ActorContextHolder.get().sub()`.
- idFirmante: persona cuya firma se registra; no es el mismo concepto que el actor autenticado.
- Requiere JWT Bearer obligatorio. Sin Bearer: HTTP 401 (Spring Security, antes del dominio).
- El actor del body esta prohibido (`idUserFirma` en body no existe en el request).
- Ruta marcada como `authenticated()` en SecurityConfig.
- JWT con firma invalida, vencido, issuer/audience incorrectos o `alg=none`: HTTP 401.

**Entrada:**
- `documentoId` (path, Long): identificador del documento a firmar.
- `seqFirmaReq` (body, positivo): posicion en la secuencia de firmantes requeridos.
- `idFirmante` (body, no nulo): identificador del firmante.
- `tipoFirma` (body, no nulo): tipo de firma (enum `TipoFirma`).
- `referenciaFirmaExt` (body, obligatoria, @NotBlank): clave de idempotencia emitida por la app Firmas.
- `hashDocumento` (body, opcional): hash del documento firmado.
- `storageKey` (body, opcional): clave de storage del documento.

**Clave de idempotencia:** `referenciaFirmaExt`

**Agregado propietario:** `FalDocumento`; tambien `FalActaFallo` si el documento pertenece al fallo activo.

**Entidades leidas:**
- `FalDocumento`
- `FalDocumentoPlantilla`
- `FalDocumentoFirma` (para verificar idempotencia)
- `FalDocumentoFirmaReq`
- `FalFirmante`
- `FalFirmanteVersion`
- `FalFirmanteVersionHabilitacion`
- `FalActa`
- `FalActaFallo`, si el documento pertenece al fallo activo
- `FalNotificacion` activa del documento, solo al completar la ultima firma
- Si la numeracion ya fue realizada, tambien se lee la informacion documental persistida; el callback no numera.

**Orden de validaciones:**
1. JWT Bearer presente y valido (Spring Security, antes del dominio). Sin token: HTTP 401.
2. Bean Validation del request. `referenciaFirmaExt` @NotBlank y demas campos requeridos. Violation: HTTP 400 VALIDACION_REQUEST.
3. Busqueda idempotente temprana por `referenciaFirmaExt`.
4. Si existe:
   a. Datos compatibles (documentoId, seqFirmaReq, idFirmante, tipoFirma, hash, storageKey coinciden): retornar firma existente. HTTP 200. Sin efectos adicionales.
   b. Datos incompatibles: `PrecondicionVioladaException` (HTTP 422 PRECONDICION_VIOLADA).
5. Documento existe. No existe: `DocumentoNoEncontradoException` (HTTP 404).
6. `estadoDocu = PENDIENTE_FIRMA`. Si no: `PrecondicionVioladaException` (HTTP 422).
7. Plantilla existe. No existe: `DocumentoPlantillaNoEncontradaException` (HTTP 404).
8. Requisito `seqFirmaReq` existe. Si no: `PrecondicionVioladaException` (HTTP 422).
9. Requisito activo. Si no: `PrecondicionVioladaException` (HTTP 422).
10. Requisito en `PENDIENTE`. Si no: `PrecondicionVioladaException` (HTTP 422).
11. Firmante existe. No existe: `FirmanteNoEncontradoException` (HTTP 404).
12. Orden de firma respetado. Si no: `PrecondicionVioladaException` (HTTP 422).
13. Firma DIGITAL exige hash no vacio. Si vacio: `PrecondicionVioladaException` (HTTP 422).
14. Numeracion previa presente cuando la plantilla usa AL_FIRMAR. Si falta: `PrecondicionVioladaException` (HTTP 422).
15. Acta propietaria existente. No existe: `ActaNoEncontradaException` (HTTP 404).
16. Cargar todos los requisitos para determinar si la firma completa el documento.
17. Cargar y validar el fallo activo, si el cierre afecta al fallo. Si fallo en estado incompatible: `PrecondicionVioladaException` (HTTP 422).
18. Consultar la notificacion activa, si el cierre corresponde a una plantilla notificable.
19. Capturar `ahora` una sola vez.
20. Resolver `FalFirmanteVersion` vigente en `ahora.toLocalDate()`. Si no: `PrecondicionVioladaException` (HTTP 422).
21. Resolver `FalFirmanteVersionHabilitacion` activa para esa version, tipo de documento y rol. Si no: `PrecondicionVioladaException` (HTTP 422).
22. Validar que el mecanismo habilitado sea compatible con el requisito y con `tipoFirma`. Si no: `PrecondicionVioladaException` (HTTP 422).

**Precondiciones** (solo si primera vez):
`estadoDocu = PENDIENTE_FIRMA`; plantilla existente; requisito `seqFirmaReq` existente, activo y en `PENDIENTE`; firmante existente; orden respetado; hash presente si DIGITAL; numeracion previa presente si requerida; acta propietaria existente; fallo en estado compatible si el documento es del fallo activo; version vigente del firmante en `ahora.toLocalDate()` y habilitacion activa para esa version con mecanismo compatible (resueltas despues de capturar `ahora`).

**Instante canonico:** `FaltasClock.now()` capturado una sola vez (`ahora`) en el paso 19
del orden de validaciones, despues de todas las validaciones no temporales.
Se usa para:
- `ahora.toLocalDate()` para resolver FalFirmanteVersion vigente (paso 20);
- `ahora` para la firma (`FalDocumentoFirma`);
- `ahora` para marcar el requisito como firmado (`FalDocumentoFirmaReq`);
- `ahora` para el cierre del documento y del fallo (`fhFirma`);
- `ahora` para preparar la notificacion;
- `ahora` para el evento `DOCFIR`.

**Regla de numeracion:** Cuando la plantilla requiere numeracion AL_FIRMAR, la numeracion
se obtiene antes de renderizar, calcular el hash y firmar, mediante el comando especifico
de numeracion para Firmas. El orden obligatorio es:

```text
numerar
renderizar contenido definitivo
calcular hash
firmar
confirmar firma real
```

El callback `firmar-real`:
- exige que el documento ya este numerado;
- nunca asigna un numero;
- nunca consume talonario;
- rechaza si la numeracion requerida falta.

**Orden de efectos** (solo si primera vez; si es retry idempotente, ninguno):

Puerta atomica inicial (antes de cualquier mutacion):
1. Construir la firma candidata con `ahora`.
2. Ejecutar `DocumentoFirmaRepository.guardarSiAusentePorReferencia(firmaCandidata)`.
3. Si el repositorio devuelve una firma preexistente:
   - datos compatibles: devolver esa firma con `yaExistia = true`;
   - datos incompatibles: `PrecondicionVioladaException`;
   - no mutar requisito, documento, fallo, notificacion, eventos ni snapshot.
4. Solo si la insercion atomica fue ganadora:
   - continuar con la transicion del requisito;
   - aplicar los efectos de firma parcial o ultima firma.

Firma parcial (no completa todas las firmas obligatorias):
1. Marcar `FalDocumentoFirmaReq` como `FIRMADO` con `ahora`.
2. No marcar documento FIRMADO. No cambiar fallo. No crear notificacion. No emitir DOCFIR. No recalcular snapshot por cierre de firma.

Ultima firma obligatoria:
1. Marcar `FalDocumentoFirmaReq` como `FIRMADO` con `ahora`.
2. Invocar `completarFirmaDocumento`:
   a. `FalDocumento.estadoDocu = FIRMADO`.
   b. `FalActaFallo.marcarPendienteNotificacion(ahora)` si el documento pertenece al fallo activo.
      Efecto: `fhFirma = ahora`, `estadoFallo = PENDIENTE_NOTIFICACION`.
   c. Preparar `FalNotificacion` en `PENDIENTE_ENVIO` si la plantilla es notificable (`siNotificable = true`)
      y no existe notificacion activa para el documento.
3. Registrar evento `DOCFIR` con `ahora`.
4. Recalcular snapshot.

**Persistencia** (solo si primera vez):

Firma parcial:
- `FalDocumentoFirma` (guardar);
- `FalDocumentoFirmaReq` (guardar como FIRMADO).

Ultima firma obligatoria:
- `FalDocumentoFirma` (guardar);
- `FalDocumentoFirmaReq` (guardar como FIRMADO);
- `FalDocumento` (guardar como FIRMADO);
- `FalActaFallo` (guardar si marcarPendienteNotificacion);
- `FalNotificacion` (guardar si preparar notificacion);
- `ActaEvento` (registrar DOCFIR);
- `FalActaSnapshot` (guardar).

Orden normativo de persistencia:
1. guardar atomicamente FalDocumentoFirma por referenciaFirmaExt;
2. guardar FalDocumentoFirmaReq como FIRMADO;
3. si es ultima firma:
   a. guardar FalDocumento;
   b. guardar FalActaFallo, cuando corresponda;
   c. guardar FalNotificacion, cuando corresponda;
   d. registrar DOCFIR;
   e. guardar snapshot.

MariaDB ejecuta toda la unidad dentro de una transaccion.
La operacion atomica por referencia decide el ganador, pero no sustituye la
proteccion concurrente adicional del requisito frente a referencias externas
distintas.

**Creacion de identificadores:**
- El ID de la firma candidata se obtiene despues de capturar `ahora` y antes de
  la insercion atomica. Una implementacion puede asignarlo dentro del repositorio.
- Una ejecucion que pierde la carrera por `referenciaFirmaExt` no persiste una
  segunda firma. Un valor de secuencia descartado no constituye una entidad ni un
  efecto de dominio.
- ID de `FalNotificacion`: solo si se prepara notificacion en este comando.
- El callback no crea ni consume numeracion documental.
- La operacion atomica por `referenciaFirmaExt` determina un unico ganador; solo el ganador aplica efectos posteriores.

**Eventos:** `DOCFIR`. Solo al completar todas las firmas obligatorias. No se emite en retry.

**Snapshot/proyecciones:**
Si se completa el documento del fallo activo, el snapshot queda en bandeja `PENDIENTE_NOTIFICACION`; accion `ENVIAR_NOTIFICACION`.
Esto aplica aunque la plantilla no genere automaticamente una cabecera notificatoria. `siNotificable = true` controla la preparacion automatica de `FalNotificacion.PENDIENTE_ENVIO`, no el estado alcanzado por el fallo.
Para una firma parcial no se recalcula snapshot por cierre de firma.

**Resultado:** `RegistrarFirmaDocumentalResultado(firma, yaExistia: boolean)`.
HTTP 201 si primera vez (`yaExistia = false`). HTTP 200 si retry idempotente (`yaExistia = true`).

**Errores:**

| Condicion | Excepcion | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|
| JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| Campo invalido o `referenciaFirmaExt` blank | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| Documento no encontrado | `DocumentoNoEncontradoException` | 404 | Ninguno | No |
| Datos de retry incompatibles | `PrecondicionVioladaException` | 422 | Ninguno | No sin correccion |
| `estadoDocu != PENDIENTE_FIRMA` | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Plantilla no encontrada | `DocumentoPlantillaNoEncontradaException` | 404 | Ninguno | No |
| Requisito inexistente, inactivo o no en PENDIENTE | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Firmante no encontrado | `FirmanteNoEncontradoException` | 404 | Ninguno | No |
| Version del firmante no vigente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Habilitacion inexistente o inactiva | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Mecanismo incompatible | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Orden de firma invalido | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Hash DIGITAL faltante | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Numeracion previa faltante | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Acta propietaria no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| Fallo en estado incompatible | `PrecondicionVioladaException` | 422 | Ninguno | No |

**Semantica de reintento:** Idempotencia por referencia externa (`referenciaFirmaExt`).
Retry con misma clave y datos compatibles: HTTP 200, `yaExistia = true`, sin efectos adicionales.
Retry con datos incompatibles: HTTP 422 PRECONDICION_VIOLADA.

**Concurrencia:**
La exclusion concurrente por referencia externa se garantiza en
`DocumentoFirmaRepository.guardarSiAusentePorReferencia`.
MariaDB: unicidad sobre `referenciaFirmaExt` garantiza un unico resultado logico.
La unicidad de `referenciaFirmaExt` evita duplicar la firma externa, pero el
comando tambien debe impedir que dos referencias externas distintas firmen
concurrentemente el mismo `FalDocumentoFirmaReq`.
La persistencia debe garantizar que un requisito solo pueda pasar una vez de
`PENDIENTE` a `FIRMADO`, mediante OCC, condicion atomica o restriccion equivalente.

**Efectos prohibidos:**
- No registrar `DOCFIR` si es retry idempotente.
- No duplicar `FalNotificacion`.
- No actualizar `fhFirma` ni `estadoFallo` si no se completaron todas las firmas obligatorias.
- No aceptar `idUserFirma` del body.

**Tests de conformidad:**
`DocumentoFirmaRealTest`, `FirmaFalloNotificacionCanonicaTest`,
`DocumentoFirmaCallbackControllerTest`, `DocumentoFirmaCallbackSecurityIT`, `JwtActorFilterTest`

---

## CMD-FALLO-002 Iniciar envio notificatorio directo

**ID normativo:** CMD-FALLO-002

**Nombre canonico:** Iniciar envio notificatorio directo

**Proposito:** Iniciar el proceso de notificacion de un documento firmado. Si existe una
notificacion en cola (`PENDIENTE_ENVIO`), la reutiliza sin crear una nueva cabecera.

**Canales de entrada:**
`POST /api/faltas/actas/{idActa}/notificaciones/enviar`
Servicio: `NotificacionService.enviarNotificacion`

**Actor y autorizacion:**
- Requiere JWT Bearer obligatorio. Sin Bearer: HTTP 401 (Spring Security).
- El actor se extrae exclusivamente de `ActorContextHolder.get().sub()` (campo `sub` del JWT).
- El actor del body esta prohibido.
- JWT con firma invalida, vencido, issuer/audience incorrectos o sin `sub`: HTTP 401.
- El actor autenticado se registra en `idUserAlta`/`idUserUltMod` de la cabecera, en `idUserAlta` del intento y en `NOTENV.idUserEvt`.

**Entrada:**
- `idActa` (path, Long): identificador del acta.
- `idDocumento` (body, Long, requerido): identificador del documento a notificar.
- `canal` (body, CanalNotificacion, requerido): canal de notificacion. El adaptador HTTP recibe su representacion textual y la convierte al enum antes del dominio.
- `destinoDigital` (body, String, opcional): destino para canales digitales; obligatorio, no blanco y maximo 120 caracteres cuando `CanalNotificacion.esDigital() = true`.
- `referenciaExterna` (body, String, opcional).
- `observaciones` (body, opcional).

**Clave de idempotencia:** No hay clave de idempotencia explicita.

**Agregado propietario:** `FalNotificacion`, `FalActa` (bloque).

**Entidades leidas:** `FalActa`, `FalDocumento`, `FalNotificacion` (buscarActivaPorDocumento), `FalPersonaDomicilio` (para canal fisico).

**Orden de validaciones:**
1. JWT Bearer presente y valido (Spring Security). Sin token: HTTP 401.
2. Bean Validation del request:
   - `idDocumento` requerido;
   - `canal` requerido.
   Una violacion produce `MethodArgumentNotValidException`, HTTP 400 `VALIDACION_REQUEST`.
3. Acta existe. No existe: `ActaNoEncontradaException` (HTTP 404).
4. Acta no esta cerrada. Si cerrada: `PrecondicionVioladaException` (HTTP 422).
5. Documento existe. No existe: `DocumentoNoEncontradoException` (HTTP 404).
6. Documento firmado (`estadoDocu = FIRMADO`). Si no firmado: `PrecondicionVioladaException` (HTTP 422).
7. Documento pertenece al acta. Si no: `PrecondicionVioladaException` (HTTP 422).
8. Validar canal y combinacion canal/destino:
   - `CORREO_POSTAL` y `NOTIFICADOR_MUNICIPAL`: requieren `acta.idDomicilioNotifAct` no nulo; `FalPersonaDomicilio` referenciado existente; `domicilioNotifId = acta.idDomicilioNotifAct`; `destinoDigital = null`.
   - `EMAIL`: `destinoDigital` requerido, trim, longitud 1..120; `domicilioNotifId = null`.
   - `PRESENCIAL`: `domicilioNotifId = null`; `destinoDigital = null`; `referenciaExterna` opcional para identificar la constancia externa.
   - `PORTAL_INFRACTOR`: no esta permitido en CMD-FALLO-002; se procesa exclusivamente mediante la variante portal de CMD-FALLO-004. Rechazar con `PrecondicionVioladaException` (HTTP 422).
9. Consultar una sola vez la cabecera activa del documento:
   a. no existe: seleccionar rama CREAR;
   b. existe en `PENDIENTE_ENVIO`: seleccionar rama REUTILIZAR;
   c. existe en cualquier otro estado activo: rechazar con `PrecondicionVioladaException` (HTTP 422).
10. Validar `referenciaExterna`, si fue informada: trim; resultado no vacio (una cadena formada solo
   por espacios es invalida, no null); longitud maxima 80; debe ser unica entre intentos;
   una repeticion se rechaza (no se devuelve como retry idempotente).
11. Capturar `ahora` una sola vez.

**Precondiciones:** Acta no cerrada (validado mediante `acta.estaCerrada()`);
documento firmado y perteneciente al acta; notificacion activa compatible (si existe).

**Nota normativa:** Las restricciones adicionales por situacion administrativa deben
definirse en el contrato transversal de operatividad del acta.

**Instante canonico:** `FaltasClock.now()` capturado una sola vez (`ahora`) en el paso 11
del orden de validaciones, despues de completar todas las validaciones. El mismo `ahora` se usa
para la notificacion (cabecera), el intento (`FalNotificacionIntento`), el evento `NOTENV`
y el snapshot de la frontera.

**Orden de efectos:**

Rama CREAR (sin cabecera activa):
Despues de capturar `ahora`:
- notificacionId = siguiente identificador de FalNotificacion
- intentoId = siguiente identificador de FalNotificacionIntento
1. Crear cabecera `FalNotificacion`:
   `id = notificacionId`;
   `idActa = acta.id`;
   `idDocumento = documento.id`;
   `estado = EN_PROCESO`;
   `resultado = null`;
   `canal = canal.name()`;
   `fechaEnvio = ahora`;
   `intentos = 1`;
   `observaciones`: si el comando informa un valor, persistir ese valor; si es null, persistir null;
   `fhAlta = ahora`;
   `fhUltMod = ahora`;
   `idUserAlta = actor`;
   `idUserUltMod = actor`.
2. Crear `FalNotificacionIntento`:
   `id = intentoId`;
   `notificacionId = cabecera.id`;
   `nroIntento = 1`;
   `canalNotif = canal`;
   CORREO_POSTAL: `domicilioNotifId = acta.idDomicilioNotifAct`; `destinoDigital = null`.
   NOTIFICADOR_MUNICIPAL: `domicilioNotifId = acta.idDomicilioNotifAct`; `destinoDigital = null`.
   EMAIL: `domicilioNotifId = null`; `destinoDigital = destinoDigital normalizado`.
   PRESENCIAL: `domicilioNotifId = null`; `destinoDigital = null`.
   PORTAL_INFRACTOR: no llega a Orden de efectos porque fue rechazado.
   `referenciaExterna = valor normalizado o null`;
   `estadoIntento = EN_PROCESO`;
   `resultadoIntento = null`;
   `fhIntento = ahora`;
   `fhAlta = ahora`;
   `idUserAlta = actor`.
3. Guardar cabecera e intento.
4. `FalActa.bloqueActual = NOTI`; guardar acta.
5. Registrar evento `NOTENV` con actor autenticado y `ahora`.
6. Recalcular snapshot.

Rama REUTILIZAR (cabecera en `PENDIENTE_ENVIO`):
Despues de capturar `ahora`:
- intentoId = siguiente identificador de FalNotificacionIntento
1. Crear `FalNotificacionIntento`:
   `id = intentoId`;
   `notificacionId = cabecera.id`;
   `nroIntento = maximo existente + 1` (asignacion atomica; unicidad `(notificacionId, nroIntento)`);
   `canalNotif = canal`;
   CORREO_POSTAL: `domicilioNotifId = acta.idDomicilioNotifAct`; `destinoDigital = null`.
   NOTIFICADOR_MUNICIPAL: `domicilioNotifId = acta.idDomicilioNotifAct`; `destinoDigital = null`.
   EMAIL: `domicilioNotifId = null`; `destinoDigital = destinoDigital normalizado`.
   PRESENCIAL: `domicilioNotifId = null`; `destinoDigital = null`.
   PORTAL_INFRACTOR: no llega a Orden de efectos porque fue rechazado.
   `referenciaExterna = valor normalizado o null`;
   `estadoIntento = EN_PROCESO`;
   `resultadoIntento = null`;
   `fhIntento = ahora`;
   `fhAlta = ahora`;
   `idUserAlta = actor`.
2. Ejecutar `iniciarEnvio` sobre la misma cabecera:
   `estado = EN_PROCESO`;
   `resultado = null`;
   `canal = canal.name()`;
   `fechaEnvio = ahora`;
   `fhUltMod = ahora`;
   `idUserUltMod = actor`;
   `observaciones`: si el comando informa un valor, reemplazar; si es null, preservar el valor existente;
   `iniciarEnvio` incrementa `intentos` exactamente una vez.
3. Guardar intento y cabecera.
4. `FalActa.bloqueActual = NOTI`; guardar acta.
5. Registrar evento `NOTENV` con actor autenticado y `ahora`.
6. Recalcular snapshot.

El mismo `ahora` y actor se usan en todos los efectos.

Cabecera en otro estado activo: rechazar con `PrecondicionVioladaException` sin crear intento.

**Persistencia:** `FalNotificacion` (guardar), `FalNotificacionIntento` (guardar),
`FalActa` (guardar), `ActaEvento` (registrar NOTENV), `FalActaSnapshot` (guardar).

**Eventos:** `NOTENV`. Solo tras exito.

**Snapshot/proyecciones:** Bandeja `EN_NOTIFICACION`; accion `EVALUAR_NOTIFICACION`.

**Resultado:** `ComandoResultado` con actaId, `idEntidadAfectada` = ID de la cabecera
`FalNotificacion` (no el ID del intento), codigo `NOTENV`, mensaje.
HTTP 201.

**Errores:**

| Condicion | Excepcion | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|
| JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| `idDocumento` ausente | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| `canal` ausente | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| Documento no encontrado | `DocumentoNoEncontradoException` | 404 | Ninguno | No |
| Acta cerrada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Documento no firmado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Documento no pertenece al acta | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Canal no admitido (`PORTAL_INFRACTOR`) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Combinacion canal/destino invalida | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Domicilio activo no informado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `FalPersonaDomicilio` no encontrado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `destinoDigital` invalido | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `referenciaExterna` formada solo por espacios | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `referenciaExterna` duplicada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Notificacion activa en estado incompatible | `PrecondicionVioladaException` | 422 | Ninguno | No |

**Semantica de reintento:** Rechazo seguro de repeticion. Si ya existe notificacion en
estado distinto de `PENDIENTE_ENVIO`, el reintento devuelve 422. Si existe `PENDIENTE_ENVIO`,
la reutiliza (el canal e instante pueden cambiar; no es idempotencia plena por referencia).

**Concurrencia:** Rechazo seguro de repeticion secuencial. El retry concurrente no esta
garantizado sin OCC, transaccion o exclusion mutua. Unicidad de notificacion activa por
documento garantizada por `NotificacionRepository.guardar` (rechaza si ya existe otra activa distinta).

**Efectos prohibidos:**
- No crear segunda notificacion activa para el mismo documento.
- No emitir `NOTENV` ni modificar bloque ni snapshot si la operacion es rechazada.

**Tests de conformidad:**
`FirmaFalloNotificacionCanonicaTest`, `ActaFlujoNotificacionFuncionalTest`

---

## CMD-FALLO-003 Generar lote postal desde notificaciones pendientes

**ID normativo:** CMD-FALLO-003

**Nombre canonico:** Generar lote postal desde notificaciones pendientes

**Proposito:** Crear un `FalLoteCorreo` agrupando todas las notificaciones en estado
`PENDIENTE_ENVIO` para envio postal masivo. Valida todas las condiciones antes de
aplicar cualquier cambio de estado.

**Canales de entrada:**
Canal de entrada: proceso tecnico autenticado.
Endpoint HTTP: no aplica.
Servicio: `LoteCorreoService.generarLoteDesdePendientes`.

**Actor y autorizacion:** Identidad tecnica autenticada del proceso que invoca el lote.
No aplica JWT de usuario final. La forma concreta de autenticacion tecnica se
definira en la spec de seguridad, pero el contrato exige una identidad autenticada y trazable.
Todo evento `LOTGEN` registra el actorTecnico autenticado.

**Entrada:**
- `loteCodigo` (String, unico en repositorio): codigo identificador del lote; trim; longitud 1..30.
- `referenciaExterna` (opcional): si se informa, trim; longitud 1..60.
- `guidLoteExt` (opcional): si se informa, trim; longitud exacta 36; formato UUID canonico.

**Clave de idempotencia:** No hay clave idempotente explicita.
`loteCodigo` duplicado produce `LoteCodigoDuplicadoException` (excepcion de aplicacion).

**Agregado propietario:** `FalLoteCorreo`.

**Entidades leidas:** notificaciones con `buscarPorEstado(PENDIENTE_ENVIO)`;
`FalActa` por cada notificacion; `FalPersonaDomicilio` asociado a `FalActa.idDomicilioNotifAct`.

**Orden de validaciones:**

1. Validar la identidad tecnica autenticada del proceso.
2. Validacion estructural del lote antes de consultar repositorios de dominio:
   - `loteCodigo`: trim; requerido; longitud 1..30;
   - `referenciaExterna`: opcional; si se informa, trim y longitud 1..60;
   - `guidLoteExt`: opcional; si se informa, trim, longitud exacta 36 y formato UUID canonico.
   Los valores normalizados son los que se persisten.
   Datos invalidos se rechazan mediante validacion controlada antes de construir `FalLoteCorreo`;
   no deben depender de `IllegalArgumentException`.
3. Verificar que `loteCodigo` no exista. Si existe: `LoteCodigoDuplicadoException`.
4. Consultar todas las notificaciones en `PENDIENTE_ENVIO`.
5. Ordenarlas de forma ascendente por `id`.
6. Si no existe ninguna notificacion:
   - volver a consultar `loteCodigo`;
   - si ya existe: `LoteCodigoDuplicadoException`;
   - si continua inexistente: `PrecondicionVioladaException`.
   Este recheck preserva la prioridad del codigo frente a una lectura inicial obsoleta.
7. Derivar la lista interna de IDs y verificar defensivamente que no contenga duplicados.
8. Para cada notificacion:
   - existe en repositorio;
   - continua en `PENDIENTE_ENVIO`;
   - `resultado = null`;
   - acta asociada existente;
   - `acta.idDomicilioNotifAct` no nulo;
   - `FalPersonaDomicilio` referenciado existente.
9. No reservar ni asignar `loteId`, `intentoId` o `nroIntento` durante la
   validacion.
10. Solo despues de completar todas las validaciones y lecturas, capturar `ahora` una unica vez.


**Precondiciones:** Al menos una notificacion en `PENDIENTE_ENVIO`; `loteCodigo` unico;
cada notificacion con acta existente, `idDomicilioNotifAct` informado y `FalPersonaDomicilio` referenciado existente.

**Instante canonico:** `FaltasClock.now()` se captura una sola vez despues de validar identidad
tecnica, entrada, unicidad y recheck de `loteCodigo`, conjunto completo de
notificaciones, actas y domicilios.
Mismo instante para lote, intentos, transiciones a `EN_PROCESO`, canal/fecha y eventos `LOTGEN`.
El metodo `registrarEvento` debe recibir ese instante; no debe capturar un nuevo
`FaltasClock.now()` independiente.

Orden determinista: notificaciones seleccionadas en orden ascendente por id; intentos
creados en ese orden; actas afectadas procesadas por el orden de la primera notificacion
asociada; un `LOTGEN` por acta. El mismo instante se usa para lote, intentos, cabeceras
y `LOTGEN`.

**Orden de efectos** (solo si todas las validaciones pasan):

Despues de capturar `ahora`:

1. Obtener o asignar `loteId`.
2. Construir el candidato `FalLoteCorreo`.
3. Ejecutar
   `LoteCorreoRepository.guardarSiAusentePorCodigo(candidato)`.
4. Si la operacion devuelve un lote preexistente:
   `LoteCodigoDuplicadoException`; no asignar `nroIntento` ni `intentoId` y no
   aplicar ningun otro efecto.
5. Solo si el candidato fue el ganador, para cada notificacion ordenada:
   - asignar atomicamente `nroIntento`;
   - obtener o asignar `intentoId`;
   - construir y persistir el intento;
   - transicionar y persistir la cabecera.
6. Procesar las actas distintas, eventos y snapshots.

Campos y efectos detallados:

1. Crear `FalLoteCorreo`:
   `id = loteId`;
   `loteCodigo = loteCodigo normalizado`;
   `estadoLote = GENERADO`;
   `referenciaExterna = valor normalizado o null`;
   `guidLoteExt = valor normalizado o null`;
   `fhGeneracion = ahora`;
   `fhAlta = ahora`;
   `idUserAlta = actorTecnico`.
2. Para cada notificacion (en orden ascendente por id):
   a. Crear `FalNotificacionIntento` con:
      `id = intentoId`;
      `notificacionId = notif.id`;
      `nroIntento = maximo existente + 1` (asignacion atomica; unicidad `(notificacionId, nroIntento)`);
      `canalNotif = CORREO_POSTAL`;
      `estadoIntento = EN_PROCESO`; `resultadoIntento = null`;
      `domicilioNotifId = acta.idDomicilioNotifAct`; `destinoDigital = null`;
      `referenciaExterna = null` (la referencia externa del lote no se copia a los intentos);
      `loteId = lote.id`; `fhIntento = ahora`; `fhAlta = ahora`; `idUserAlta = actorTecnico`.
      Nunca crear un intento `CORREO_POSTAL` con `domicilioNotifId = null`.
   b. Aplicar sobre `notif`:
      `estado = EN_PROCESO`;
      `resultado = null`;
      `canal = CORREO_POSTAL.name()`;
      `fechaEnvio = ahora`;
      `intentos = intentos anteriores + 1`;
      `fhUltMod = ahora`;
      `idUserUltMod = actorTecnico`.
      El contador se incrementa exactamente una vez por intento postal creado.
      No modificar: `id`, `idActa`, `idDocumento`, `fhAlta`, `idUserAlta`, `observaciones`.
   c. Guardar intento.
   d. Guardar notificacion.
3. Por cada acta afectada distinta (en orden de primera notificacion asociada):
   `acta.bloqueActual = NOTI`; guardar acta una sola vez;
   registrar `LOTGEN` una sola vez; recalcular snapshot.

**Persistencia:** `FalLoteCorreo` (guardar), `FalNotificacionIntento` (guardar por notificacion),
`FalNotificacion` (guardar por notificacion), `FalActa` (guardar por acta afectada),
`ActaEvento` (LOTGEN por acta afectada), `FalActaSnapshot` (guardar por acta afectada).

Orden normativo de persistencia:
1. guardar FalLoteCorreo mediante guardarSiAusentePorCodigo;
   solo el ganador continua
2. por cada notificacion ordenada:
   a. guardar FalNotificacionIntento;
   b. guardar FalNotificacion en EN_PROCESO;
3. por cada acta distinta:
   a. guardar FalActa con bloque NOTI;
   b. registrar LOTGEN;
   c. guardar snapshot.

Toda la unidad se ejecuta en una transaccion MariaDB.

**Eventos:** `LOTGEN` por cada acta afectada (uno por acta, no uno por notificacion).

**Snapshot/proyecciones:** Actualizado por acta afectada. Resultado canonico:
bandeja `EN_NOTIFICACION`; accion `EVALUAR_NOTIFICACION`.

**Resultado:** `FalLoteCorreo` generado.

**Errores:**

| Condicion | Excepcion | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|
| Ninguna notificacion en `PENDIENTE_ENVIO` | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| Duplicado en lista interna derivada | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| identidad tecnica ausente o invalida | rechazo del adaptador tecnico | No aplica | Ninguno | No |
| `loteCodigo` nulo o blanco | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| `loteCodigo` excede 30 caracteres | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| `referenciaExterna` vacia o excede 60 caracteres | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| `guidLoteExt` invalido (longitud o formato UUID) | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| `loteCodigo` duplicado | `LoteCodigoDuplicadoException` | No aplica | Ninguno | No sin cambiar clave |
| Notificacion no encontrada | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| Notificacion no en `PENDIENTE_ENVIO` | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| Notificacion con resultado previo | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| Acta asociada inexistente | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| `idDomicilioNotifAct` nulo | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| `FalPersonaDomicilio` inexistente | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| Conflicto al asignar `nroIntento` | `PrecondicionVioladaException` | No aplica | Ninguno | No |

**Semantica de reintento:** Comando no idempotente. `loteCodigo` es una clave unica de
rechazo de repeticion, no una clave de idempotencia plena.

La existencia del codigo se valida antes de consultar si quedan notificaciones pendientes.
Por lo tanto, un reintento con el mismo `loteCodigo` despues de una respuesta incierta
se rechaza siempre con `LoteCodigoDuplicadoException`, aun cuando las notificaciones
originales ya hayan avanzado a `EN_PROCESO`.

El invocador puede consultar el lote por `loteCodigo` para resolver una respuesta incierta.

Esto tambien aplica cuando el primer precheck fue obsoleto y ya no quedan notificaciones
pendientes: la ausencia de pendientes activa un recheck de `loteCodigo`; si el codigo ya
existe, el resultado sigue siendo `LoteCodigoDuplicadoException`.

**Concurrencia:**
InMemory:

- `synchronized (loteGeneracionMonitor)` serializa el comando completo dentro de
  una instancia de `LoteCorreoService`;
- evita que dos codigos diferentes procesen simultaneamente el mismo conjunto
  de notificaciones pendientes;
- `guardarSiAusentePorCodigo` agrega la garantia atomica de unicidad para el
  mismo `loteCodigo`;
- no se afirma rollback transaccional entre repositorios.

MariaDB:

- una transaccion debe bloquear o reclamar las notificaciones pendientes;
- debe garantizar unicidad de `loteCodigo`;
- lote, intentos, cabeceras, actas, eventos y snapshots forman una sola unidad
  atomica.

**Efectos prohibidos:**
- No crear lote si cualquier precondicion falla.
- No mutar notificaciones si la validacion previa falla.

**Tests de conformidad:**
`NotificacionAcuseLoteServiceTest`, `FirmaFalloNotificacionCanonicaTest`,
`LoteCorreoIntentosCanonicoTest`

---

## CMD-FALLO-004 Registrar resultado notificatorio positivo

**ID normativo:** CMD-FALLO-004

**Nombre canonico:** Registrar resultado notificatorio positivo

**Proposito:** Registrar que la notificacion fue exitosa. El comando cubre tres variantes:
pieza previa al fallo (solo avanza acta a ANAL), fallo condenatorio (avanza fallo a NOTIFICADO
y calcula fhVtoApelacion) y fallo absolutorio (avanza fallo a NOTIFICADO y puede cerrar el
acta si no hay bloqueantes). En todos los casos registra el resultado positivo sobre el
intento concreto y la cabecera.

**Canales de entrada:**
`POST /api/faltas/notificaciones/{id}/positiva`
Servicio: `NotificacionService.registrarPositiva`

**Actor y autorizacion:**
- Requiere JWT Bearer obligatorio. Sin Bearer: HTTP 401 (Spring Security).
- El actor se extrae exclusivamente de `ActorContextHolder.get().sub()` (campo `sub` del JWT).
- El actor del body esta prohibido.
- JWT con firma invalida, vencido, issuer/audience incorrectos o sin `sub`: HTTP 401.
- El evento notificatorio registra el actor autenticado.

**Entrada:**
- `id` (path, Long): identificador de la notificacion.
- `intentoId` (body, Long, requerido): identificador del intento notificatorio concreto.
- `observaciones` (body, opcional).

**Clave de idempotencia:** No hay. Segundo intento con resultado ya registrado -> rechazo.

**Agregado propietario:** `FalNotificacion`, `FalActaFallo` (si notificacion es del fallo activo).

**Entidades leidas:** `FalNotificacion`, `FalNotificacionIntento`, `FalActa`,
`FalActaFallo` (buscarActivo, si aplica),
`FalDocumento` (cuando la notificacion corresponde a un documento del fallo).

**Orden de validaciones:**
1. JWT Bearer presente y valido (Spring Security). Sin token: HTTP 401.
2. Bean Validation del request. `intentoId` requerido. Violation: HTTP 400 VALIDACION_REQUEST.
3. Notificacion existe. No existe: `NotificacionNoEncontradaException` (HTTP 404).
4. Intento existe. No existe: `NotificacionIntentoNoEncontradoException` (HTTP 404).
5. Intento pertenece a la notificacion. Si no: `PrecondicionVioladaException` (HTTP 422).
6. Intento en estado `EN_PROCESO` y sin resultado previo (`estadoIntento = EN_PROCESO` y `resultadoIntento = null`). Si no: `PrecondicionVioladaException` (HTTP 422).
7. Cabecera en estado `EN_PROCESO` y sin resultado previo (`notificacion.estado = EN_PROCESO` y `notificacion.resultado = null`). Si tiene resultado o estado distinto: `PrecondicionVioladaException` (HTTP 422).
8. Acta cargada desde `notif.idActa`. No encontrada: `ActaNoEncontradaException` (HTTP 404).
9. Acta no esta cerrada. Si esta cerrada: `PrecondicionVioladaException` (HTTP 422).
10. Si la notificacion corresponde al documento del fallo activo: cargar y validar el documento y el fallo.
    La distincion absolutorio/condenatorio vive en `FalActaFallo.tipoFallo`, no en `TipoDocu`.
11. Estado e hitos del fallo compatibles con la variante que corresponda; entre ellos,
    `fhVtoApelacion == null` antes del resultado positivo (invariante comun a ambos tipos).
12. Capturar `ahora` una sola vez.

**Precondiciones por variante:**

Invariante comun del fallo (antes de clasificar CONDENATORIO/ABSOLUTORIO):
- `fhVtoApelacion` debe ser null antes del resultado positivo. Para condenatorio se calcula desde
  `ahoraPositiva` recien despues del positivo; para absolutorio permanece null. Un valor previo es
  inconsistente (ocultaria un vencimiento calculado antes de la notificacion) y se rechaza antes del
  reloj y antes de toda mutacion: `PrecondicionVioladaException` (HTTP 422).

Pieza previa al fallo:
- Notificacion e intento sin resultado previo; acta existente.

Fallo condenatorio:
- Ademas: fallo activo existente con documento coincidente; fallo en estado `PENDIENTE_NOTIFICACION`;
  `fhFirma` presente en el fallo; `fallo.tipoFallo = CONDENATORIO` y `fallo.resultadoFallo = CONDENA`.
  La coherencia tipo/resultado se valida antes del reloj y antes de toda mutacion; si falla:
  `PrecondicionVioladaException` (HTTP 422).

Fallo absolutorio:
- Ademas: fallo activo existente; fallo en estado `PENDIENTE_NOTIFICACION`;
  `fhFirma != null`; `fallo.tipoFallo = ABSOLUTORIO` y `fallo.resultadoFallo = ABSUELVE`.
  La coherencia tipo/resultado se valida antes del reloj y antes de toda mutacion; si falla:
  `PrecondicionVioladaException` (HTTP 422). Un `tipoFallo` no reconocido tambien se rechaza.

**Instante canonico:** `FaltasClock.now()` capturado una vez (`ahoraPositiva`) en el paso 12,
despues de completar todas las precondiciones de entrada y dominio; para condenatorio, a continuacion
se invoca y valida el contrato interno del calculo, todavia antes de la primera mutacion. El mismo
`ahoraPositiva` se usa para: fechaResultado del intento, fechaResultado de la cabecera,
`fhNotificacion` del fallo, base de `fhVtoApelacion` y evento notificatorio.

**Orden de efectos por variante:**

Pieza previa al fallo:
- Actualizar `FalNotificacionIntento`:
  `resultadoIntento = POSITIVO`;
  `estadoIntento = CON_ACUSE_POSITIVO`;
  `fhResultado = ahoraPositiva`;
  `fhUltMod = ahoraPositiva`;
  `idUserUltMod = actor`.
- Actualizar `FalNotificacion`:
  `estado = CON_ACUSE_POSITIVO`;
  `resultado = POSITIVO`;
  `fechaResultado = ahoraPositiva`;
  `fhUltMod = ahoraPositiva`;
  `idUserUltMod = actor`.
- `observaciones`: si el comando informa un valor, reemplazar `FalNotificacion.observaciones`;
  si es null, preservar el valor existente.
- La descripcion de `NOTPOS` incorpora las observaciones informadas sin persistirlas en otro campo.
- `FalActa.bloqueActual = ANAL`; guardar acta.
- Registrar evento `NOTPOS` con ahoraPositiva y actor autenticado.
- Recalcular snapshot.
- No modifica `FalActaFallo`.

Fallo condenatorio:
- Actualizar `FalNotificacionIntento`:
  `resultadoIntento = POSITIVO`;
  `estadoIntento = CON_ACUSE_POSITIVO`;
  `fhResultado = ahoraPositiva`;
  `fhUltMod = ahoraPositiva`;
  `idUserUltMod = actor`.
- Actualizar `FalNotificacion`:
  `estado = CON_ACUSE_POSITIVO`;
  `resultado = POSITIVO`;
  `fechaResultado = ahoraPositiva`;
  `fhUltMod = ahoraPositiva`;
  `idUserUltMod = actor`.
- `observaciones`: si el comando informa un valor, reemplazar `FalNotificacion.observaciones`;
  si es null, preservar el valor existente.
- La descripcion de `NOTPOS` incorpora las observaciones informadas sin persistirlas en otro campo.
- `FalActaFallo.marcarNotificado(ahoraPositiva)` -> `fhNotificacion = ahoraPositiva`; `estadoFallo = NOTIFICADO`.
  Calcular y persistir `fhVtoApelacion` (tipo `LocalDate`) mediante `PlazosAdministrativosService.calcularVencimientoApelacion(ahoraPositiva.toLocalDate())`:
  tipo = `APELACION_FALLO`; fechaOrigen = `ahoraPositiva.toLocalDate()`; cantidad leida desde configuracion global (`faltas.plazos.apelacion-dias-computables`);
  el dia de notificacion no se cuenta; sabado computa; domingo, 1 de enero, 1 de mayo y excepciones locales activas no computan;
  resultado LocalDate = ultimo dia completo apelable; equivalente a:
  la politica de plazo devuelve un `LocalDate`; el dia almacenado es el ultimo dia habil completo en que
  puede presentarse la apelacion; ese dia todavia es apelable; el plazo se considera vencido cuando
  `ahora.toLocalDate().isAfter(fhVtoApelacion)`; no comparar directamente `LocalDateTime` con `LocalDate`.
- Contrato del calculo: `NotificacionService` valida el resultado antes de mutar: no null;
  `tipo = APELACION_FALLO`; `fechaOrigen = ahoraPositiva.toLocalDate()`; `fechaVencimiento` no null.
  El calculo se invoca una sola vez. Una violacion del contrato es un error interno (no del usuario):
  `IllegalStateException`, sin efectos (ni intento, ni cabecera, ni fallo, ni acta, ni eventos, ni snapshot).
- `FalActa.bloqueActual = ANAL`; guardar acta.
- Persistir intento, notificacion, fallo y acta (misma transaccion en MariaDB).
- Registrar evento `NOTPOS` con ahoraPositiva y actor autenticado.
- Recalcular snapshot.
- No cerrar el acta.

Fallo absolutorio:
- Actualizar `FalNotificacionIntento`:
  `resultadoIntento = POSITIVO`;
  `estadoIntento = CON_ACUSE_POSITIVO`;
  `fhResultado = ahoraPositiva`;
  `fhUltMod = ahoraPositiva`;
  `idUserUltMod = actor`.
- Actualizar `FalNotificacion`:
  `estado = CON_ACUSE_POSITIVO`;
  `resultado = POSITIVO`;
  `fechaResultado = ahoraPositiva`;
  `fhUltMod = ahoraPositiva`;
  `idUserUltMod = actor`.
- `observaciones`: si el comando informa un valor, reemplazar `FalNotificacion.observaciones`;
  si es null, preservar el valor existente.
- La descripcion de `NOTPOS` incorpora las observaciones informadas sin persistirlas en otro campo.
- `FalActaFallo.marcarNotificado(ahoraPositiva)` -> `fhNotificacion = ahoraPositiva`; `estadoFallo = NOTIFICADO`.
  Preservar `fallo.tipoFallo = ABSOLUTORIO` y `fallo.resultadoFallo = ABSUELVE`; esos valores
  pertenecen al fallo ya dictado y no son decididos por la notificacion.
  `acta.resultadoFinal = ResultadoFinalActa.ABSUELTO`.
  No confundir la preservacion del resultado del fallo con el nuevo resultado final del acta.
- Si no hay bloqueantes: `situacionAdministrativa = CERRADA`; `bloqueActual = CERR`; registrar `CIERRA`.
- Si hay bloqueantes: `situacionAdministrativa` permanece operativa; `bloqueActual = ANAL`; sin `CIERRA`.
- `FalActa` actualizada en ambos casos.
- Registrar evento `NOTPOS`.
- Recalcular snapshot.

**Regla de atomicidad:** Ninguna notificacion positiva puede quedar persistida si la
transicion del fallo falla. MariaDB debe ejecutar este comando dentro de una unica
transaccion.

**Persistencia:** `FalNotificacionIntento` (actualizar), `FalNotificacion` (guardar),
`FalActaFallo` (guardar con `fhVtoApelacion` si fallo condenatorio),
`FalActa` (guardar), `ActaEvento` (registrar NOTPOS; CIERRA si absolutorio sin bloqueantes),
`FalActaSnapshot` (guardar).

**Eventos:** Orden para resultado ordinario: NOTPOS antes de CIERRA.
`CIERRA` solo para fallo absolutorio sin bloqueantes.
Para la variante portal: `NOTSUP` (solo si existian intentos superados), `PORPOS`; `CIERRA` solo si el fallo absolutorio queda sin bloqueantes. No se emite `NOTPOS` por la variante portal.

**Snapshot/proyecciones:**

Pieza previa al fallo: bloque `ANAL`; bandeja `PENDIENTE_ANALISIS`; accion `DICTAR_FALLO`.

Fallo condenatorio notificado: bloque `ANAL`; bandeja `PENDIENTES_FALLO`; accion `NINGUNA`.

Fallo absolutorio sin bloqueantes: `situacionAdministrativa = CERRADA`; bloque `CERR`; bandeja `CERRADAS`; accion `NINGUNA`.

Fallo absolutorio con bloqueantes: `situacionAdministrativa` permanece operativa; bloque `ANAL`; bandeja `PENDIENTE_ANALISIS`; accion `NINGUNA`.

La variante portal produce la misma proyeccion correspondiente al tipo de pieza o fallo.

**Resultado:** `ComandoResultado` con actaId, idNotificacion, codigo `NOTPOS`, mensaje. HTTP 200.

**Errores:**

| Condicion | Excepcion | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|
| JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| `intentoId` ausente | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| Notificacion no encontrada | `NotificacionNoEncontradaException` | 404 | Ninguno | No |
| Intento no encontrado | `NotificacionIntentoNoEncontradoException` | 404 | Ninguno | No |
| Intento no pertenece a notificacion | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Intento ya resuelto | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Cabecera ya tiene resultado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| Acta cerrada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Fallo en estado incompatible | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Fallo sin fhFirma | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Fallo con `fhVtoApelacion` previamente informado | `PrecondicionVioladaException` | 422 | Ninguno | No |

**Semantica de reintento:** Rechazo seguro de repeticion. Segundo intento -> 422 PRECONDICION_VIOLADA.

**Concurrencia:**

Ambas variantes (ordinaria y portal) sincronizan sobre el mismo monitor InMemory compartido
(`ResultadoPositivoInMemoryMonitor.INSTANCE`), lo que garantiza dentro de una instancia JVM:

- ordinario vs ordinario sobre la misma notificacion: exactamente una ejecucion tiene exito;
  las restantes se rechazan con `PrecondicionVioladaException` antes de capturar el reloj y sin
  efectos secundarios en intento, cabecera, fallo, acta ni snapshot.
- portal vs portal sobre la misma notificacion: idem anterior; sin duplicar `PORPOS`, `NOTSUP`,
  `CIERRA` ni snapshot.
- ordinario vs portal sobre la misma notificacion: exactamente un ganador; el perdedor detecta
  el resultado ya persistido y rechaza antes de su reloj, sin producir `NOTPOS` o `PORPOS`
  adicionales ni snapshot duplicado.

Garantia exclusiva de una instancia JVM. Multiples nodos o MariaDB requieren:

- transaccion con OCC o bloqueo de la cabecera de notificacion;
- restriccion unica a nivel de base de datos sobre (notificacionId, nroIntento) para el intento
  portal.

**Portal positivo:**

### Canal de entrada

`QrActaService.registrarAccesoConNotificacion`
    -> `NotificacionIntentoService.registrarPortalPositivo`

No existe endpoint HTTP directo para esta variante.

### Entrada

- `notificacionId`: identificador de la notificacion.
- `actaIdQrEsperada`: identificador interno obtenido al resolver el token QR; no proviene
  del cliente como autoridad; es la unica fuente valida para la asociacion QR-acta-notificacion.
- `destinoPortal`: requerido, no blanco, maximo 120 caracteres.
- `actor` autenticado.

Cuando `notificacionId` esta informado, la colaboracion con `NotificacionIntentoService`
es obligatoria; no existe bypass silencioso por servicio null.
La validacion de servicio ocurre antes del acceso QR para que un cableado erroneo
falle precozmente sin registrar accesos de auditoria.

### Validaciones

- notificacion existente;
- `actaIdQrEsperada` no null;
- invariante de asociacion: `notificacion.idActa = actaIdQrEsperada`;
  mismatch -> `PrecondicionVioladaException` antes del reloj portal y antes de cualquier
  mutacion notificatoria;
- `destinoPortal` valido: trim; longitud 1..120;
- cabecera sin resultado POSITIVO previo;
- acta existente;
- documento y fallo cargados antes de mutar, cuando corresponda;
- estado e hitos del fallo compatibles;
- intentos activos identificados.

### Efectos canonicos

- Validar todo antes de mutar.
- Por cada intento activo previo superado por portal:
  `resultadoIntento = SUPERADA_POR_PORTAL`;
  `estadoIntento = SIN_EFECTO`;
  `fhResultado = ahora`;
  `fhUltMod = ahora`;
  `idUserUltMod = actor`.
  Guardar cada intento actualizado.
  No modificar campos de identidad, canal, destino, lote, referencia externa, fecha de intento ni auditoria de alta.
`intentoId` se obtiene o asigna despues de capturar `ahora` y antes de construir el intento.
- Crear intento portal con todos los campos determinados:
  `id = intentoId`;
  `notificacionId = notificacion.id`;
  `nroIntento = maximo existente + 1` (asignado atomicamente; unicidad `(notificacionId, nroIntento)`);
  `canalNotif = PORTAL_INFRACTOR`;
  `domicilioNotifId = null`;
  `destinoDigital = destinoPortal normalizado`;
  `loteId = null`;
  `referenciaExterna = null`;
  `fhIntento = ahora`;
  `fhAlta = ahora`;
  `idUserAlta = actor`;
  `resultadoIntento = POSITIVO`;
  `estadoIntento = CON_ACUSE_POSITIVO`;
  `fhResultado = ahora`;
  `fhUltMod = ahora`;
  `idUserUltMod = actor`.
- Actualizar `FalNotificacion`:
  `estado = CON_ACUSE_POSITIVO`;
  `resultado = POSITIVO`;
  `fechaResultado = ahora`;
  `fhUltMod = ahora`;
  `idUserUltMod = actor`.
- Aplicar la variante de pieza/fallo correspondiente (condenatorio o absolutorio).
- Si es fallo condenatorio: `marcarNotificado`; `fhNotificacion`; `fhVtoApelacion`.
- Si es fallo absolutorio: `marcarNotificado`; preservar `fallo.tipoFallo = ABSOLUTORIO`
  y `fallo.resultadoFallo = ABSUELVE` (pertenecen al fallo ya dictado); aplicar cierre/bloqueantes.
- Cardinalidad de eventos:
  NOTSUP: cero eventos si no existian intentos activos previos; exactamente un evento por la notificacion procesada si existia al menos un intento activo previo; no un evento por cada intento superado.
  PORPOS: exactamente un evento por la ejecucion portal exitosa.
  CIERRA: exactamente uno unicamente si el fallo absolutorio queda cerrado sin bloqueantes.
  Orden: NOTSUP (si aplica), PORPOS, CIERRA (si aplica). Todos con el mismo `ahora` y actor.
- Recalcular snapshot.
- El mismo instante debe usarse en todos esos efectos.

### Resultado

El invocador interno recibe el `FalNotificacionIntento` portal creado. El resultado contiene:
- `id = intentoId`;
- `notificacionId = notificacion.id`;
- `nroIntento` asignado;
- `canalNotif = PORTAL_INFRACTOR`;
- `resultadoIntento = POSITIVO`;
- `estadoIntento = CON_ACUSE_POSITIVO`.

La variante portal no tiene endpoint HTTP directo. Aplica las mismas reglas de fallo,
cierre y snapshot que la variante ordinaria correspondiente al tipo de pieza o fallo.
Usa un unico instante para intentos superados, intento portal, cabecera, fallo, acta,
eventos y snapshot.

### Errores especificos de la variante portal

| Condicion | Excepcion |
|---|---|
| `actaIdQrEsperada` null (cableado invalido) | `PrecondicionVioladaException` |
| Mismatch QR-acta-notificacion: `notificacion.idActa != actaIdQrEsperada` | `PrecondicionVioladaException` |
| `notificacionId` informado y `notifService` null (cableado invalido) | `IllegalArgumentException` |
| `destinoPortal` invalido (null, blanco o excede 120) | `PrecondicionVioladaException` |
| Conflicto al asignar `nroIntento` (unicidad violada) | `PrecondicionVioladaException` |

### Reintento

- rechazo seguro si la cabecera ya es positiva;
- retry concurrente dentro de una JVM: garantizado por el monitor compartido
  `ResultadoPositivoInMemoryMonitor.INSTANCE` (serializa ordinario/ordinario, portal/portal y
  ordinario/portal); el perdedor rechaza antes de su reloj y sin efectos secundarios;
- multiples nodos o MariaDB: requiere transaccion con OCC o bloqueo de la cabecera.

### Persistencia

Orden normativo:
1. guardar intentos previos superados, si existen;
2. guardar intento portal;
3. guardar cabecera FalNotificacion positiva;
4. guardar FalActaFallo, cuando corresponda;
5. guardar FalActa;
6. registrar eventos en el orden definido;
7. guardar snapshot.

### Atomicidad

Intentos superados, intento portal, cabecera, fallo, acta, eventos y snapshot
forman una sola unidad transaccional en MariaDB.

**Efectos prohibidos:**
- No procesar si ya tiene resultado.
- No duplicar `marcarNotificado`.
- No cerrar el acta para fallo condenatorio (el cierre es posterior al pago de condena).

**Tests de conformidad:**
`NotificacionPositivaCanonicaTest`, `NotificacionPortalPositivaCanonicaTest`,
`NotificacionPositivaControllerTest`,
`NotificacionPositivaSecurityTest`, `FirmaFalloNotificacionCanonicaTest`,
`ActaFlujoNotificacionFuncionalTest`, `QrActaServiceTest` (clase anidada `IntegracionPortal`)

---

## CMD-FALLO-005 Declarar firmeza por vencimiento del plazo de apelacion

**ID normativo:** CMD-FALLO-005

**Nombre canonico:** Declarar firmeza por vencimiento del plazo de apelacion

**Proposito:** Declarar que la condena es firme porque el plazo de apelacion vencio
sin que se presentara apelacion. Avanza el fallo a `FIRME` y el acta a `CONDENA_FIRME`.

**Canales de entrada:**
`POST /api/faltas/actas/{id}/firmeza/vencer-plazo-apelacion`
Servicio: `FirmezaCondenaService.vencerPlazoApelacion`

**Actor y autorizacion:**
- Requiere JWT Bearer obligatorio. Sin Bearer: HTTP 401 (Spring Security).
- El actor se extrae exclusivamente de `ActorContextHolder.get().sub()` (campo `sub` del JWT).
- JWT con firma invalida, vencido, issuer/audience incorrectos o sin `sub`: HTTP 401.
- El endpoint esta explicitamente declarado en `SecurityConfig` y reconocido en `JwtActorFilter`.

**Entrada:**
- `id` (path, Long): identificador del acta.
- `observaciones` (body, opcional).

El `actor` del comando se obtiene en el controlador desde `ActorContextHolder.get().sub()`.
No proviene del body. `VencerPlazoApelacionCommand` transporta el campo `actor` (String, max 36).

**Clave de idempotencia:** No hay. `CONDENA_FIRME` previa -> rechazo.

**Agregado propietario:** `FalActaFallo`, `FalActa`.

**Entidades leidas:** `FalActa`, `FalActaFallo` (buscarActivo), `FalActaApelacion`
via `ApelacionActaRepository.buscarPorFallo(fallo.id)` (busca exclusivamente por `falloId`).

Una apelacion historica de otro fallo no bloquea el vencimiento del fallo activo.
La mera consulta `buscarUltima(actaId)` no es suficiente para CMD-FALLO-005.

**Orden de validaciones:**
1. JWT Bearer presente y valido (Spring Security). Sin token: HTTP 401.
2. Acta existe. No existe: `ActaNoEncontradaException` (HTTP 404).
3. Acta operativa (no CERRADA, ANULADA, ARCHIVADA, PARALIZADA). Si no: `PrecondicionVioladaException` (HTTP 422).
4. Fallo activo existe. Si no: `PrecondicionVioladaException` (HTTP 422).
5. Fallo de tipo CONDENATORIO. Si no: `PrecondicionVioladaException` (HTTP 422).
6. Fallo en estado NOTIFICADO. Si no: `PrecondicionVioladaException` (HTTP 422).
7. No firmeza previa: `estadoFallo = NOTIFICADO`; `tipoFallo = CONDENATORIO`; `fhFirma != null`; `fhNotificacion != null`; `siFirme = false`; `fhFirmeza = null`; `origenFirmeza = null`; `resultadoFinal != CONDENA_FIRME`. Si cualquier condicion falla: `PrecondicionVioladaException` (HTTP 422).
8. Ausencia de apelacion asociada al fallo activo:
   - `FalActaApelacion` posee `falloId`; la ausencia se evalua respecto de `fallo.id`;
   - si el repositorio devuelve la ultima apelacion por acta, el servicio debe comprobar
     `apelacion.falloId` antes de usarla; una apelacion historica vinculada a otro fallo
     no bloquea el vencimiento del fallo activo;
   - si no existe apelacion asociada a `fallo.id`: continuar;
   - si existe una apelacion rechazada correspondiente al fallo activo, usar `CMD-FALLO-006`;
   - cualquier otra apelacion existente asociada al fallo activo impide este comando
     y produce `PrecondicionVioladaException`.
9. `fhVtoApelacion != null`. Si es null: `PrecondicionVioladaException` (HTTP 422).
10. Capturar `ahora` una sola vez.
11. `ahora.toLocalDate().isAfter(fhVtoApelacion)`. Si no (`fechaActual <= fhVtoApelacion`): `PrecondicionVioladaException` (HTTP 422).

**Precondiciones:** Acta operativa; fallo CONDENATORIO en NOTIFICADO; `fhFirma` y `fhNotificacion` presentes; `siFirme = false`, `fhFirmeza = null`, `origenFirmeza = null` y `resultadoFinal != CONDENA_FIRME`; no existe ninguna apelacion asociada al fallo activo (`apelacion.falloId = fallo.id`); `fhVtoApelacion` calculado y vencido.

**Instante canonico:** `FaltasClock.now()` capturado una sola vez (`ahora`) en el paso 10
del orden de validaciones, despues de validar `fhVtoApelacion != null` y antes de la primera mutacion.
El mismo `ahora` se usa para `ahora.toLocalDate().isAfter(fhVtoApelacion)`, `fhFirmeza` del fallo
y eventos `PLAVNC` y `CONFIR`.
`fhVtoApelacion` es de tipo `LocalDate`; el dia almacenado es el ultimo dia habil completo apelable;
el plazo se considera vencido cuando `ahora.toLocalDate().isAfter(fhVtoApelacion)`;
no comparar directamente `LocalDateTime` con `LocalDate`.

**Orden de efectos:**
1. Completar todas las validaciones no temporales, incluida:
   - ausencia de apelacion asociada al fallo activo;
   - `fhVtoApelacion != null`.
2. Capturar `ahora` una sola vez.
3. Validar `ahora.toLocalDate().isAfter(fhVtoApelacion)`.
4. Aplicar `fallo.declararFirmeza(ahora, VENCIMIENTO_PLAZO_APELACION)` ->
   `siFirme=true`, `fhFirmeza=ahora`, `origenFirmeza=VENCIMIENTO_PLAZO_APELACION`,
   `estadoFallo=FIRME`.
5. Guardar fallo.
6. Asignar y guardar `acta.resultadoFinal = CONDENA_FIRME`.
7. Registrar `PLAVNC` con `ahora` y actor autenticado.
   Las observaciones opcionales se incorporan a la descripcion de `PLAVNC`.
8. Registrar `CONFIR` con `ahora` y actor autenticado.
   `CONFIR` conserva su descripcion canonica sin repetir las observaciones.
9. Recalcular y guardar snapshot.

`PLAVNC` se registra antes de `CONFIR`; ambos con el mismo instante. No capturar otro
reloj despues del paso 2.

**Persistencia:** `FalActaFallo` (guardar), `FalActa` (guardar),
`ActaEvento` (registrar PLAVNC y CONFIR), `FalActaSnapshot` (guardar).

**Eventos:** `PLAVNC` y `CONFIR` (en ese orden, mismo instante).

**Snapshot/proyecciones:** Fallo FIRME, CONDENA_FIRME, sin pago:
bandeja `PENDIENTE_PAGO_CONDENA`; accion `GESTIONAR_PAGO_CONDENA`.

**Resultado:** `ComandoResultado` con actaId, falloId, codigo `CONFIR`, mensaje. HTTP 200.

**Errores:**

| Condicion | Excepcion | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|
| JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| Acta no operativa (cerrada/anulada/archivada/paralizada) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Sin fallo activo | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Fallo no CONDENATORIO | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Fallo no NOTIFICADO | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Firmeza ya declarada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `fhFirma` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `fhNotificacion` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Marcadores de firmeza inconsistentes | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Cualquier apelacion existente distinta de rechazada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Apelacion rechazada: usar CMD-FALLO-006 | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `fhVtoApelacion` no calculado (null) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Plazo de apelacion no vencido (`!ahora.toLocalDate().isAfter(fhVtoApelacion)`) | `PrecondicionVioladaException` | 422 | Ninguno | No |

**Semantica de reintento:** Rechazo seguro de repeticion. Segundo intento -> 422.

**Concurrencia:** CMD-FALLO-005 y CMD-FALLO-006 comparten `private final Object firmezaMonitor = new Object()`
en `FirmezaCondenaService`. El monitor es por instancia de servicio, no estatico.
Dentro de la misma instancia de FirmezaCondenaService: exactamente un ganador; el perdedor rechaza
antes de su reloj y sin efectos en fallo, acta, eventos ni snapshot.
Otra instancia del mismo servicio, otra JVM/nodo o MariaDB: no garantizado sin transaccion + OCC/bloqueo.

**Efectos prohibidos:**
- No emitir `PLAVNC` si existe cualquier apelacion asociada al fallo activo.
  Una apelacion rechazada debe procesarse exclusivamente mediante CMD-FALLO-006.
- No cerrar automaticamente el acta.
- No iniciar pago condena.
- No emitir `CIERRA`.

**Tests de conformidad:**
`FirmezaCondenaTest`, `FirmezaVencimientoCanonicaTest`, `FirmezaVencimientoControllerTest`,
`FirmezaVencimientoSecurityTest`, `ActaFlujoFalloFuncionalTest`

---

## CMD-FALLO-006 Declarar firmeza por apelacion rechazada

**ID normativo:** CMD-FALLO-006

**Nombre canonico:** Declarar firmeza por apelacion rechazada

**Proposito:** Declarar que la condena es firme porque la apelacion fue rechazada.
Avanza el fallo a `FIRME` y el acta a `CONDENA_FIRME`. No emite `PLAVNC`.

**Canales de entrada:**
`POST /api/faltas/actas/{id}/firmeza/por-apelacion-rechazada`
Servicio: `FirmezaCondenaService.declararFirmePorApelacionRechazada`

**Actor y autorizacion:**
- Requiere JWT Bearer obligatorio. Sin Bearer: HTTP 401 (Spring Security).
- El actor se extrae exclusivamente de `ActorContextHolder.get().sub()` (campo `sub` del JWT).
- JWT con firma invalida, vencido, issuer/audience incorrectos o sin `sub`: HTTP 401.

**Entrada:**
- `id` (path, Long): identificador del acta.
- `observaciones` (body, opcional).

**Clave de idempotencia:** No hay. `CONDENA_FIRME` previa -> rechazo.

**Agregado propietario:** `FalActaFallo`, `FalActa`.

**Entidades leidas:** `FalActa`, `FalActaFallo` (buscarActivo), `FalActaApelacion` asociada al fallo activo (`falloId = fallo.id`).

La implementacion debe consultar la apelacion asociada al fallo activo o recorrer el resultado
disponible hasta identificarla. La mera consulta de la ultima apelacion por acta no demuestra
existencia ni ausencia para el fallo activo.

**Orden de validaciones:**
1. JWT Bearer presente y valido (Spring Security). Sin token: HTTP 401.
2. Acta existe. No existe: `ActaNoEncontradaException` (HTTP 404).
3. Acta operativa. Si no: `PrecondicionVioladaException` (HTTP 422).
4. Fallo activo CONDENATORIO en NOTIFICADO. Si no: `PrecondicionVioladaException` (HTTP 422).
5. No firmeza previa: `fhFirma != null`; `fhNotificacion != null`; `siFirme = false`; `fhFirmeza = null`; `origenFirmeza = null`; `resultadoFinal != CONDENA_FIRME`. Si cualquier condicion falla: `PrecondicionVioladaException` (HTTP 422).
6. Existe apelacion asociada al fallo activo (`apelacion.falloId = fallo.id`). Si no existe apelacion
   asociada a `fallo.id`: `PrecondicionVioladaException` (HTTP 422). Una apelacion historica de otro
   fallo no satisface esta precondicion.
7. Verificar que `apelacion.actaId = acta.id` y `apelacion.falloId = fallo.id`.
8. Estado de apelacion es RECHAZADA, o RESUELTA con resultado RECHAZADA.
   Si esta PRESENTADA: `PrecondicionVioladaException` (HTTP 422, debe resolverse primero).
   Si tiene otro resultado no rechazado: `PrecondicionVioladaException` (HTTP 422).
9. Capturar `ahora` una sola vez despues de completar todas las validaciones.

**Precondiciones:** Acta operativa; fallo CONDENATORIO en NOTIFICADO; `fhFirma` y `fhNotificacion` presentes; `siFirme = false`, `fhFirmeza = null`, `origenFirmeza = null` y `resultadoFinal != CONDENA_FIRME`; apelacion existente con `apelacion.actaId = acta.id` y `apelacion.falloId = fallo.id`, con resultado rechazado firme.

**Instante canonico:** `FaltasClock.now()` capturado una sola vez (`ahora`) en el paso 9
del orden de validaciones, despues de completar todas las validaciones.
Usado para: `fhFirmeza` del fallo y evento `CONFIR`.
En `Orden de efectos`, no volver a consultar el reloj.

**Orden de efectos:**
1. `fallo.declararFirmeza(ahora, APELACION_RECHAZADA)` -> `siFirme=true`, `fhFirmeza=ahora`,
   `origenFirmeza=APELACION_RECHAZADA`, `estadoFallo=FIRME`. Guardar fallo.
2. `acta.resultadoFinal = CONDENA_FIRME`. Guardar acta.
3. Registrar `CONFIR` con `ahora` y actor autenticado.
   Las observaciones opcionales se incorporan a la descripcion de `CONFIR`.
4. Recalcular snapshot.

No se emite `PLAVNC` en este camino.

**Persistencia:** `FalActaFallo` (guardar), `FalActa` (guardar),
`ActaEvento` (registrar CONFIR), `FalActaSnapshot` (guardar).

**Eventos:** `CONFIR` (solo este; no `PLAVNC`).

**Snapshot/proyecciones:** Identico a CMD-FALLO-005:
bandeja `PENDIENTE_PAGO_CONDENA`; accion `GESTIONAR_PAGO_CONDENA`.

**Resultado:** `ComandoResultado` con actaId, falloId, codigo `CONFIR`, mensaje. HTTP 200.

**Errores:**

| Condicion | Excepcion | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|
| JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| Acta no operativa | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Sin fallo activo / no CONDENATORIO / no NOTIFICADO | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Firmeza ya declarada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `fhFirma` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| `fhNotificacion` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Marcadores de firmeza inconsistentes | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Sin apelacion asociada al fallo activo | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Apelacion en PRESENTADA (no resuelta) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Apelacion no rechazada | `PrecondicionVioladaException` | 422 | Ninguno | No |

**Semantica de reintento:** Rechazo seguro de repeticion. Segundo intento -> 422.

**Concurrencia:** Un retry secuencial posterior a un exito visible se rechaza con 422.
Dentro de la misma instancia de FirmezaCondenaService: serializado por el mismo `firmezaMonitor`
compartido con CMD-FALLO-005.
Otra instancia del mismo servicio, otra JVM/nodo o MariaDB: no garantizado sin transaccion + OCC/bloqueo.

**Efectos prohibidos:**
- No emitir `PLAVNC`.
- No cerrar automaticamente el acta.
- No iniciar pago condena.
- No emitir `CIERRA`.

**Tests de conformidad:**
`FirmezaCondenaTest`, `FirmezaApelacionRechazadaCanonicaTest`, `FirmezaApelacionRechazadaControllerTest`,
`FirmezaApelacionRechazadaSecurityTest`, `ActaFlujoFalloFuncionalTest`, `ResolucionApelacionEfectosTest`

---

## CMD-FALLO-007 Informar pago de condena

**ID normativo:** CMD-FALLO-007

**Nombre canonico:** Informar pago de condena

**Proposito:** Registrar que el infractor ha efectuado el pago de la condena.
Crea o actualiza `FalPagoCondena` en estado `INFORMADO`.
No cierra el acta ni emite `CIERRA`.

**Canales de entrada:**
`POST /api/faltas/actas/{id}/pago-condena/informar`
Servicio: `PagoCondenaService.informar`

**Actor y autorizacion:**
- Requiere JWT Bearer obligatorio. Sin Bearer: HTTP 401 (Spring Security).
- El actor se extrae exclusivamente de `ActorContextHolder.get().sub()` (campo `sub` del JWT).
- JWT con firma invalida, vencido, issuer/audience incorrectos o sin `sub`: HTTP 401.
- El evento `PCOINF` registra el actor autenticado.

**Entrada:**
- `id` (path, Long): identificador del acta.
- `monto` (body, BigDecimal, requerido): monto abonado; debe ser mayor a cero.
- `referenciaPago` (body, String, requerido): referencia del comprobante; no puede ser vacia.
- `observaciones` (body, String, opcional).

**Clave de idempotencia:** No hay clave de idempotencia externa.
`referenciaPago` no constituye una clave de idempotencia mientras no exista una regla de unicidad expresa.
Si el pago existe en `INFORMADO`, re-informar actualiza montos y referencia.
Si el pago existe en `CONFIRMADO`, se rechaza.

**Nota sobre ResultadoFinalActa:** `ResultadoFinalActa.CONDENA_FIRME` no sustituye
la firmeza de `FalActaFallo`. El servicio verifica ambas condiciones de forma
independiente: primero el resultado del acta y luego el estado del fallo.

**Agregado propietario:** `FalPagoCondena`, `FalActa`.

**Entidades leidas:** `FalActa`, `FalActaFallo` (buscarActivo), `FalPagoCondena` (buscarPorActa).

**Orden de validaciones:**
1. JWT Bearer presente y valido (Spring Security). Sin token: HTTP 401. `sub` presente.
2. Validacion estructural del request (Bean Validation `@Valid`): `monto` requerido (`@NotNull`);
   `referenciaPago` no vacia (`@NotBlank`). Violation: HTTP 400.
3. Validacion estructural del comando (servicio, antes de `synchronized`):
   `actaId` no null; `actor` no null, no blank, `<= 36` caracteres (defensivo, sub del JWT);
   `monto` no null y `> 0`; `referenciaPago` no null y no blank.
   Si viola: `PrecondicionVioladaException` (HTTP 422) o `IllegalArgumentException` (comando null).
4. Acta existe. No existe: `ActaNoEncontradaException` (HTTP 404).
5. Acta operativa (no CERRADA, ANULADA, ARCHIVADA, PARALIZADA). Si no: `PrecondicionVioladaException` (HTTP 422).
6. `acta.resultadoFinal == CONDENA_FIRME`. Si no: `PrecondicionVioladaException` (HTTP 422).
7. Fallo activo existe y es CONDENATORIO con `resultadoFallo == CONDENA` en estado FIRME,
   con `siFirme=true`, `fhFirma`, `fhNotificacion`, `fhFirmeza` y `origenFirmeza` presentes.
   Si cualquier condicion falla: `PrecondicionVioladaException` (HTTP 422).
8. Si existe pago previo y esta `CONFIRMADO`: `PrecondicionVioladaException` (HTTP 422).

**Precondiciones:** Acta operativa; `CONDENA_FIRME`; fallo CONDENATORIO FIRME con todos
los hitos de firmeza presentes; pago previo no confirmado (si existe). Actor proviene exclusivamente de `sub`.

**Instante canonico:** `FaltasClock.now()` capturado una sola vez (`ahora`). El mismo instante
se usa para `fechaInforme`, el evento `PCOINF` y el snapshot. Actor = sub del JWT.

**Creacion de ID:** Si no existe pago previo: `UUID.randomUUID().toString()`.
Si ya existe pago en INFORMADO: se actualiza el existente (mismo ID).

**Orden de efectos:**

Sin pago previo:
1. Crear una unica `FalPagoCondena` para el acta con ID UUID.
2. Asignar `monto`, `referenciaPago`, `estadoPagoCondena = INFORMADO`, `fechaInforme = ahora`.
   Si `cmd.observaciones != null`: asignar `observaciones`.
3. Guardar pago.
4. Registrar evento `PCOINF` con `ahora` y actor autenticado.
5. Recalcular snapshot.

Pago en `INFORMADO`:
1. Reutilizar mismo ID.
2. Reemplazar `monto` y `referenciaPago`. Actualizar `fechaInforme = ahora`.
   Actualizar `observaciones` solo si se informo un nuevo valor.
3. Guardar pago.
4. Emitir nuevo `PCOINF` con `ahora` y actor autenticado.
5. Recalcular snapshot.

Pago en `OBSERVADO`:
1. Reutilizar mismo ID. Volver a `INFORMADO`.
2. Reemplazar `monto` y `referenciaPago`. Actualizar `fechaInforme = ahora`.
   Preservar `motivoObservacion` y `fechaObservacion` como evidencia historica.
   Actualizar `observaciones` solo si se informo un nuevo valor.
3. Guardar pago.
4. Emitir nuevo `PCOINF` con `ahora` y actor autenticado.
5. Recalcular snapshot.

Pago en `CONFIRMADO`: rechazar sin efectos.

El acta no cambia de `resultadoFinal`, `situacionAdministrativa` ni `bloqueActual`
en este comando. No se emite `CIERRA`.

**Persistencia:** `FalPagoCondena` (guardar),
`ActaEvento` (registrar PCOINF), `FalActaSnapshot` (guardar).

**Eventos:** `PCOINF`.

**Snapshot/proyecciones:**
Despues de una ejecucion valida de informar, el pago queda `INFORMADO`:
bandeja `PENDIENTE_CONFIRMACION_PAGO_CONDENA`; accion `CONFIRMAR_PAGO_CONDENA`.

`PENDIENTE_PAGO_CONDENA` / `GESTIONAR_PAGO_CONDENA` corresponde a condena firme sin pago informado.
`PENDIENTE_PAGO_CONDENA` / `CORREGIR_PAGO_CONDENA` corresponde a pago `OBSERVADO`.

**Resultado:** `ComandoResultado` con actaId, pagoId, codigo `PCOINF`, mensaje. HTTP 200.

**Errores:**

| Condicion | Excepcion | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|
| JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| body: `monto` ausente | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| body: `referenciaPago` ausente o blank | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| command directo: `actaId` null | `PrecondicionVioladaException` | 422 | Ninguno | No sin correccion |
| command directo: `actor` null/blank/>36 | `PrecondicionVioladaException` | 422 | Ninguno | No sin correccion |
| command directo: `monto` null o no positivo | `PrecondicionVioladaException` | 422 | Ninguno | No sin correccion |
| command directo: `referenciaPago` null o blank | `PrecondicionVioladaException` | 422 | Ninguno | No sin correccion |
| Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| Acta no operativa | `PrecondicionVioladaException` | 422 | Ninguno | No |
| resultadoFinal != CONDENA_FIRME | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Sin fallo activo | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Fallo no CONDENATORIO o no FIRME | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Firmeza inconsistente (hitos faltantes) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| Pago ya confirmado | `PrecondicionVioladaException` | 422 | Ninguno | No |

**Semantica de reintento:** Comando no idempotente con actualizacion controlada de una
identidad natural por acta.

Retry secuencial:
- `PENDIENTE/INFORMADO/OBSERVADO`: valido; mismo `pago.id`; nuevo `PCOINF` por ejecucion.
- `CONFIRMADO`: rechazo con `PrecondicionVioladaException` (HTTP 422).

Retry concurrente en misma instancia:
- Serializado por `pagoCondenaMonitor`.
- Una sola entidad de pago por acta.
- Cada ejecucion valida emite un `PCOINF`.

Otra instancia/JVM/nodo/MariaDB:
- No garantizado sin unicidad fisica por acta mas transaccion, OCC o bloqueo.

No llamarlo idempotente. `referenciaPago` no es clave de idempotencia mientras no exista
una regla de unicidad expresa.

**Concurrencia:** Dentro de la misma instancia de `PagoCondenaService`, `informar`,
`confirmar` y `observar` se serializan mediante `pagoCondenaMonitor` (monitor privado
de instancia). Dos informes iniciales validos reutilizan una sola entidad por acta y
cada ejecucion emite su `PCOINF`.

Otra instancia, otra JVM/nodo o MariaDB no tiene todavia esa garantia y requiere unicidad
fisica por acta mas transaccion/OCC o bloqueo.

**Efectos prohibidos:**
- No cerrar el acta.
- No emitir `CIERRA`.
- No emitir `PCOCNF`.
- No modificar `FalActaFallo` ni `FalActa`.

**Tests de conformidad:**
`PagoCondenaTest`, `PagoCondenaInformarCanonicaTest` (canonical, CountingClock),
`PagoCondenaInformarControllerTest` (adaptador HTTP), `PagoCondenaInformarSecurityTest` (seguridad JWT),
`ActaFlujoFalloFuncionalTest`

---

## Matriz de errores por comando

| Comando | Condicion | Excepcion exacta | HTTP | Efectos parciales | Reintentable |
|---|---|---|---|---|---|
| CMD-FALLO-001 | Documento no encontrado | `DocumentoNoEncontradoException` | 404 | Ninguno | No |
| CMD-FALLO-001 | JWT invalido o sin `sub` | `AuthenticationException` | 401 | Ninguno | No |
| CMD-FALLO-001 | Firma previa con `referenciaFirmaExt` identica pero datos incompatibles | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Documento ya firmado (firmas completas) y retry compatible | - | 200 (idempotente) | Ninguno | Si |
| CMD-FALLO-001 | Plantilla no encontrada | `DocumentoPlantillaNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-001 | Requisito inexistente, inactivo o no en PENDIENTE | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Firmante no encontrado | `FirmanteNoEncontradoException` | 404 | Ninguno | No |
| CMD-FALLO-001 | Version del firmante no vigente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Habilitacion inexistente o inactiva | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Mecanismo incompatible | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Orden de firma invalido | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Hash DIGITAL faltante | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Numeracion previa faltante | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-001 | Acta propietaria no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-002 | JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| CMD-FALLO-002 | `idDocumento` ausente | `MethodArgumentNotValidException` | 400 | Ninguno | No |
| CMD-FALLO-002 | `canal` ausente | `MethodArgumentNotValidException` | 400 | Ninguno | No |
| CMD-FALLO-002 | Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-002 | Acta cerrada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | Documento no firmado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | Canal no admitido (`PORTAL_INFRACTOR`) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | Combinacion canal/destino invalida | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | Domicilio activo no informado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | `FalPersonaDomicilio` no encontrado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | `destinoDigital` invalido | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | `referenciaExterna` formada solo por espacios | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | `referenciaExterna` duplicada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-002 | Notificacion activa en estado incompatible | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-003 | identidad tecnica ausente o invalida | rechazo del adaptador tecnico | No aplica | Ninguno | No |
| CMD-FALLO-003 | `loteCodigo` nulo o blanco | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | `loteCodigo` excede 30 caracteres | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | `referenciaExterna` vacia o excede 60 caracteres | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | `guidLoteExt` invalido (longitud o formato UUID) | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | Ninguna notificacion en `PENDIENTE_ENVIO` | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | Duplicado en lista interna derivada | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | ID de notificacion no encontrado | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | Notificacion no en `PENDIENTE_ENVIO` | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | `loteCodigo` duplicado | `LoteCodigoDuplicadoException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | Acta asociada inexistente | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | `idDomicilioNotifAct` nulo | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | `FalPersonaDomicilio` inexistente | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-003 | Conflicto al asignar `nroIntento` | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-004 | JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| CMD-FALLO-004 | Notificacion no encontrada | `NotificacionNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-004 | Intento no encontrado | `NotificacionIntentoNoEncontradoException` | 404 | Ninguno | No |
| CMD-FALLO-004 | Intento no pertenece a notificacion | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-004 | Intento ya resuelto | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-004 | Cabecera ya tiene resultado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-004 | Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-004 | Fallo en estado incompatible | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-004 | Fallo sin fhFirma | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-004 (portal) | `destinoPortal` invalido | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-004 (portal) | Conflicto al asignar `nroIntento` | `PrecondicionVioladaException` | No aplica | Ninguno | No |
| CMD-FALLO-005 | JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| CMD-FALLO-005 | Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-005 | Acta no operativa | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | Sin fallo activo o no CONDENATORIO o no NOTIFICADO | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | Firmeza ya declarada (`CONDENA_FIRME`) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | `fhFirma` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | `fhNotificacion` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | Marcadores de firmeza inconsistentes | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | Cualquier apelacion existente distinta de rechazada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | Apelacion rechazada: usar CMD-FALLO-006 | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | `fhVtoApelacion` no calculado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-005 | Plazo no vencido | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| CMD-FALLO-006 | Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-006 | Acta no operativa | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | Sin fallo activo condenatorio notificado | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | Firmeza ya declarada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | `fhFirma` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | `fhNotificacion` ausente | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | Marcadores de firmeza inconsistentes | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | Sin apelacion asociada al fallo activo | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | Apelacion en PRESENTADA (no resuelta) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-006 | Apelacion no rechazada | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-007 | JWT ausente o invalido | (Spring Security) | 401 | Ninguno | No |
| CMD-FALLO-007 | Acta no encontrada | `ActaNoEncontradaException` | 404 | Ninguno | No |
| CMD-FALLO-007 | Acta no operativa | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-007 | `resultadoFinal != CONDENA_FIRME` | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-007 | Sin fallo activo / no CONDENATORIO / no FIRME | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-007 | Firmeza inconsistente (hitos faltantes) | `PrecondicionVioladaException` | 422 | Ninguno | No |
| CMD-FALLO-007 | body: `monto` ausente/null | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| CMD-FALLO-007 | body: `referenciaPago` ausente o blank | `MethodArgumentNotValidException` | 400 | Ninguno | No sin correccion |
| CMD-FALLO-007 | command directo: `monto` null o no positivo | `PrecondicionVioladaException` | 422 | Ninguno | No sin correccion |
| CMD-FALLO-007 | command directo: `referenciaPago` null o blank | `PrecondicionVioladaException` | 422 | Ninguno | No sin correccion |
| CMD-FALLO-007 | Pago ya confirmado | `PrecondicionVioladaException` | 422 | Ninguno | No |

---

## Matriz de idempotencia por comando

| Comando | Categoria | Clave | Retry tras exito | Retry concurrente | Eventos duplicados |
|---|---|---|---|---|---|
| CMD-FALLO-001 | Idempotencia por referencia externa | `referenciaFirmaExt` | HTTP 200, mismo resultado logico, sin duplicar entidades ni eventos | La busqueda temprana no es la garantia concurrente. La garantia para la misma referencia es `guardarSiAusentePorReferencia`. Solo su ganador aplica efectos posteriores. Sin duplicar firma ni `DOCFIR`. | Misma referencia externa: no duplica firma ni `DOCFIR`. Referencias externas distintas sobre el mismo requisito: no garantizado hasta que `FalDocumentoFirmaReq` posea OCC, transicion atomica o restriccion equivalente. |
| CMD-FALLO-002 | Rechazo seguro de repeticion | - | 422 PRECONDICION_VIOLADA si notificacion ya esta en estado activo incompatible | No garantizado sin OCC, transaccion o exclusion mutua | No garantizado concurrentemente; el retry secuencial rechazado no duplica eventos. |
| CMD-FALLO-003 | Comando no idempotente | `loteCodigo` como clave unica de rechazo de repeticion | `LoteCodigoDuplicadoException`, incluso si ya no quedan pendientes; consultar lote por `loteCodigo` | La unicidad de `loteCodigo` debe elegir un solo ganador; la unidad completa todavia exige transaccion para lote, intentos, notificaciones, actas, eventos y snapshots | El mismo `loteCodigo` no produce nuevos `LOTGEN` despues de consolidarse la unicidad; otro `loteCodigo` representa otra ejecucion. |
| CMD-FALLO-004 | Rechazo seguro de repeticion | `(notificacionId, intentoId)` | 422 PRECONDICION_VIOLADA (resultado ya registrado) | Garantizado dentro de la misma JVM por `ResultadoPositivoInMemoryMonitor.INSTANCE` (serializa ordinario/ordinario, portal/portal y ordinario/portal; exactamente un ganador; el perdedor rechaza antes de su reloj y sin efectos). Multiples JVM/nodos o MariaDB: no garantizado sin transaccion + OCC/bloqueo. | No se duplican dentro de una JVM bajo el monitor compartido; multinodo requiere garantia fisica/transaccional. |
| CMD-FALLO-005 | Rechazo seguro de repeticion | - | 422 PRECONDICION_VIOLADA (`CONDENA_FIRME` ya asignado) | Garantizado dentro de la misma instancia de FirmezaCondenaService por `firmezaMonitor` (exactamente un ganador; el perdedor rechaza antes de su reloj y sin efectos). Otra instancia, otra JVM/nodo o MariaDB: no garantizado sin transaccion + OCC/bloqueo. | No se duplican dentro de la misma instancia protegida; fuera de esa frontera requieren garantia fisica/transaccional. |
| CMD-FALLO-006 | Rechazo seguro de repeticion | - | 422 PRECONDICION_VIOLADA (`CONDENA_FIRME` ya asignado) | Serializado por el mismo `firmezaMonitor` compartido con CMD-FALLO-005 dentro de la misma instancia de FirmezaCondenaService. Otra instancia, otra JVM/nodo o MariaDB: no garantizado sin transaccion + OCC/bloqueo. | No se duplican dentro de la misma instancia protegida; fuera de esa frontera requieren garantia fisica/transaccional. |
| CMD-FALLO-007 | Comando no idempotente con actualizacion controlada de una identidad natural por acta | - | PENDIENTE/INFORMADO/OBSERVADO: actualiza datos, emite nuevo `PCOINF`, mismo `pago.id`. CONFIRMADO: 422. Retry tras timeout puede emitir otro `PCOINF`. | Serializado por `pagoCondenaMonitor` dentro de la misma instancia de PagoCondenaService. Una sola entidad por acta; cada ejecucion valida emite `PCOINF`. Otra instancia/JVM/nodo/MariaDB: no garantizado sin unicidad fisica mas transaccion/OCC o bloqueo. | Cada ejecucion valida emite un nuevo `PCOINF`. No es idempotente. `referenciaPago` no es clave de idempotencia. |
