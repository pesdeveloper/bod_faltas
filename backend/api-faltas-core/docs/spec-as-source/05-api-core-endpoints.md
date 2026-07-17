# 05 - API Core Endpoints

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Ante contradiccion con un documento tematico de `00-governance/` o `10-domain/`, ese documento tematico prevalece en lo que respecta a definiciones, dimensiones y lifecycle (ver README, seccion 4.0).

Base path: `/api/faltas`

## Actas

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/labrar | Labrar nueva acta |
| POST | /actas/{id}/completar-captura | Completar captura |
| POST | /actas/{id}/enriquecer | Enriquecer acta |
| GET  | /actas/{id} | Obtener acta |
| GET  | /actas/{id}/snapshot | Obtener snapshot operativo |
| GET  | /actas/{id}/timeline | Obtener eventos del acta |

## Documentos

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{idActa}/documentos | Generar documento |
| POST | /documentos/desde-plantilla | Generar documento a partir de una plantilla |
| POST | /documentos/{idDocumento}/firmar | Firmar documento (acta o fallo; flujo naive de compatibilidad) |
| POST | /documentos/{documentoId}/firmar-real | Callback de firma real (Firmas); requiere JWT Bearer; ver `10-domain/firma-notificacion-fallo.md` |
| POST | /documentos/{documentoId}/enviar-a-firma | Enviar documento a firma |
| POST | /documentos/{documentoId}/emitir | Emitir formalmente un documento |
| POST | /actas/{idActa}/documentos/escaneados | Incorporar documento escaneado/adjunto externo |
| POST | /documentos/{documentoId}/convalidar-firma-escaneada | Convalidar firma escaneada/olografa |
| POST | /documentos/{documentoId}/numerar | Numerar documento para integracion con Firmas; requiere JWT Bearer |
| GET  | /actas/{idActa}/documentos | Listar documentos del acta |

## Notificaciones

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/notificaciones | Enviar notificacion |
| POST | /notificaciones/{id}/positiva | Registrar acuse positivo |
| POST | /notificaciones/{id}/negativa | Registrar acuse negativo |
| POST | /notificaciones/{id}/vencida | Registrar vencida |
| GET  | /actas/{id}/notificaciones | Listar notificaciones del acta |

## Pago Voluntario

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/pago-voluntario/solicitar | Solicitar pago voluntario |
| POST | /actas/{id}/pago-voluntario/fijar-monto | Fijar monto a pagar |
| POST | /actas/{id}/pago-voluntario/informar | Infractor informa pago realizado |
| POST | /actas/{id}/pago-voluntario/confirmar | Confirmar pago (cierra si sin bloqueantes) |
| POST | /actas/{id}/pago-voluntario/observar | Observar/rechazar pago informado |
| POST | /actas/{id}/pago-voluntario/vencer | Vencer pago, habilitar analisis |
| GET  | /actas/{id}/pago-voluntario | Obtener estado del pago voluntario |

## Fallo

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/fallos/absolutorio | Dictar fallo absolutorio |
| POST | /actas/{id}/fallos/condenatorio | Dictar fallo condenatorio |
| GET  | /actas/{id}/fallo | Obtener fallo activo del acta |

Nota: la firma del documento de fallo usa los endpoints de documentos (`/firmar` o `/firmar-real`); la
notificacion usa los endpoints de notificaciones. Apelacion, pago de condena, firmeza y gestion externa
tienen sus propios endpoints, documentados en las secciones siguientes.

## Respuestas comunes

- **200 OK**: comando ejecutado correctamente
- **201 Created**: acta labrada
- **404 Not Found**: entidad no encontrada
- **422 Unprocessable Entity**: precondicion violada

## Estructura ComandoResultado

```json
{
  "idActa": "uuid-del-acta",
  "idEntidadAfectada": "uuid-de-la-entidad",
  "tipoEvento": "FALABS",
  "descripcion": "Fallo absolutorio dictado."
}
```

## Apelacion

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/apelaciones | Registrar apelacion sobre fallo condenatorio notificado (HTTP 201) |
| POST | /actas/{id}/apelaciones/rechazar | Resolver apelacion como rechazada (evento APERAZ) |
| POST | /actas/{id}/apelaciones/aceptar-absuelve | Resolver apelacion aceptando y absolviendo (evento APEABS) |
| GET  | /actas/{id}/apelacion | Obtener apelacion activa del acta |

## Firmeza de condena

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/firmeza/vencer-plazo-apelacion | Vencer plazo sin apelacion - genera PLAVNC+CONFIR |
| POST | /actas/{id}/firmeza/por-apelacion-rechazada | Declarar firmeza por apelacion rechazada - genera CONFIR |

DTO: DeclararFirmezaRequest { observaciones }

## Gestion externa

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/gestion-externa/derivar | Derivar acta a gestion externa |
| POST | /actas/{id}/gestion-externa/reingresar | Reingresar desde gestion externa (ver detalle abajo) |
| POST | /actas/{id}/gestion-externa/pago-externo | Registrar pago externo de una gestion activa (ver detalle abajo) |
| GET  | /actas/{id}/gestion-externa | Obtener gestion externa activa del acta |

---

## Endpoints de pago de condena

### POST /api/faltas/actas/{id}/pago-condena/informar

Informa un pago de condena para el acta. Pertenece a CMD-FALLO-007; contrato
canonico completo (precondiciones, orden de efectos, concurrencia) en
[`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md#cmd-fallo-007-informar-pago-de-condena).

**Seguridad:** JWT Bearer obligatorio (`SecurityConfig`: `POST /api/faltas/actas/*/pago-condena/informar` -> `authenticated()`).
El actor se extrae exclusivamente de `sub` (JWT); el body NO incluye actor.

**Body:**
```json
{
  "monto": 3000.00,
  "referenciaPago": "REF-BAN-001",
  "observaciones": "opcional"
}
```

**Respuesta exitosa:** `200 OK` con `ComandoResultado` (evento `PCOINF`)

**Errores:**
- `401` si el Bearer/JWT es invalido, ausente, vencido o sin `sub`
- `400` si el body es ausente o invalido (Bean Validation): `monto` ausente/null, `referenciaPago` ausente/blank
- `404` si acta no encontrada
- `422` si el command o una precondicion de dominio es invalida (acta no operativa, `resultadoFinal != CONDENA_FIRME`,
  sin fallo activo/no CONDENATORIO/no FIRME, firmeza inconsistente, pago ya `CONFIRMADO`)

### POST /api/faltas/actas/{id}/pago-condena/confirmar

Confirma el pago de condena informado (`ConfirmarPagoCondenaCommand`; comando general
fuera de los siete contratos canonicos CMD-FALLO-001..007).

`PCOCNF` se registra siempre que la confirmacion supere sus precondiciones (pago `INFORMADO`,
acta operativa, `resultadoFinal == CONDENA_FIRME`). Los bloqueantes materiales activos NO
rechazan la confirmacion: solo determinan si tambien se registra `CIERRA`.
- Sin bloqueantes activos: se registra `CIERRA`; acta `CERRADA`/`CERR`.
- Con bloqueantes activos: NO se registra `CIERRA`; acta permanece `ACTIVA`/`ANAL` con
  `resultadoFinal = CONDENA_FIRME_PAGADA` y el pago en `CONFIRMADO`.

**Body:** `{}` (observaciones opcionales)

**Respuesta exitosa:** `200 OK` con `ComandoResultado` (evento `PCOCNF`)

**Errores:**
- `404` si acta no encontrada
- `422` si pago no informado (no existe pago `INFORMADO` previo), acta no operativa,
  o `resultadoFinal != CONDENA_FIRME`

Los bloqueantes materiales activos NO producen `422`: la confirmacion completa su efecto
principal y solo difiere el cierre.

### POST /api/faltas/actas/{id}/pago-condena/observar

Observa/rechaza el pago de condena informado.

**Body:**
```json
{
  "motivoObservacion": "Referencia de pago invalida",
  "observaciones": "opcional"
}
```

**Respuesta exitosa:** `200 OK` con `ComandoResultado` (evento `PCOOBS`)

**Errores:**
- `404` si acta no encontrada
- `422` si precondicion violada

### GET /api/faltas/actas/{id}/pago-condena

Obtiene el pago de condena actual del acta.

**Respuesta exitosa:** `200 OK` con `FalPagoCondena`
**Respuesta:** `404` si no existe pago condena para el acta

### Seguridad de confirmar / observar / obtener

`SecurityConfig` vigente solo declara `authenticated()` explicito para
`POST /api/faltas/actas/*/pago-condena/informar` (CMD-FALLO-007) y para el resto de
`/api/faltas/pagos/**`. Las rutas `pago-condena/confirmar`, `pago-condena/observar` y
`GET pago-condena` no tienen un `requestMatcher` propio: caen en la regla general
`.anyRequest().permitAll()` y por lo tanto son de acceso publico (sin JWT) en el estado
actual del codigo. Esto es una decision funcional pendiente fuera del alcance de
CMD-FALLO-007; no se corrige aqui ni se modifica `SecurityConfig` en este documento.

Contrato canonico completo (CMD-FALLO-007): ver [`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md).

---

## Endpoint de reingreso desde gestion externa

### POST /api/faltas/actas/{id}/gestion-externa/reingresar

Reingresa el expediente desde gestion externa al circuito interno.

**Precondicion:** acta en `bloqueActual=GEXT` / `situacion=EN_GESTION_EXTERNA` con gestion activa en `DERIVADA` o `EN_CURSO`.

**Body:**
```json
{
  "modoReingresoGestionExterna": "REINGRESO_PARA_REVISION",
  "motivoReingreso": "No se logro ejecutar el apremio",
  "resultadoGestionExterna": "SIN_PAGO",
  "observaciones": "opcional"
}
```

Valores habilitados de `modoReingresoGestionExterna`:
- `REINGRESO_PARA_REVISION`
- `REINGRESO_SIN_PAGO`
- `REINGRESO_PARA_NUEVO_FALLO`
- `REINGRESO_CON_DICTAMEN`

Valores reservados (fallan con 422):
- `REINGRESO_PARA_CIERRE`
- `REINGRESO_CON_PAGO`

**Respuesta exitosa:** `200 OK` con `ComandoResultado` (evento `EXTRET`)

**Errores:**
- `404` si acta no encontrada
- `422` si precondicion violada (modo no habilitado, motivo vacio, sin gestion activa, par resultado/modo incoherente, etc.)

**Efectos del reingreso:**
- `FalGestionExterna.siActiva = false`
- `FalGestionExterna.estadoGestionExterna = REINGRESADA`
- `acta.situacionAdministrativa = ACTIVA`
- `acta.bloqueActual = ANAL`
- Registra `EXTRET`
- Recalcula snapshot

### Validacion de pares resultado/modo habilitados en /reingresar

Cuando `resultadoGestionExterna` es informado (no null), se valida el par resultado/modo.

**Pares permitidos:**

| resultadoGestionExterna | modoReingresoGestionExterna |
|------------------------|-----------------------------|
| `SIN_PAGO`             | `REINGRESO_SIN_PAGO`        |
| `SIN_CAMBIOS`          | `REINGRESO_PARA_REVISION`   |
| `ABSUELVE`             | `REINGRESO_PARA_NUEVO_FALLO`|
| `CONFIRMA_CONDENA`     | `REINGRESO_CON_DICTAMEN`    |
| `MODIFICA_MONTO`       | `REINGRESO_CON_DICTAMEN`    |

**Ejemplos validos:**

```json
{
  "modoReingresoGestionExterna": "REINGRESO_SIN_PAGO",
  "motivoReingreso": "No se logro ejecutar el apremio. Sin pago.",
  "resultadoGestionExterna": "SIN_PAGO"
}
```

```json
{
  "modoReingresoGestionExterna": "REINGRESO_PARA_REVISION",
  "motivoReingreso": "Sin cambios sustantivos tras gestion.",
  "resultadoGestionExterna": "SIN_CAMBIOS"
}
```

**Combinaciones incoherentes que fallan con 422:**
- `SIN_PAGO` + `REINGRESO_PARA_REVISION`
- `SIN_CAMBIOS` + `REINGRESO_SIN_PAGO`
- `PAGO_REGISTRADO` via reingreso (asignado solo por PAGAPR; nunca via /reingresar)
- `SIN_RESULTADO` como resultado de reingreso (es el estado inicial, no un resultado informable)

### Pares con dictamen externo habilitados en /reingresar

**Campo adicional en el request:**

| Campo | Tipo | Obligatorio | Descripcion |
|-------|------|-------------|-------------|
| `montoResultado` | `BigDecimal` | Solo si resultado == `MODIFICA_MONTO` (> 0) | Monto externo informado. |

**Pares con dictamen externo:**

| resultadoGestionExterna | modoReingresoGestionExterna | Requiere |
|------------------------|-----------------------------|---------|
| `ABSUELVE`             | `REINGRESO_PARA_NUEVO_FALLO` | `resultadoFinal == CONDENA_FIRME` |
| `CONFIRMA_CONDENA`     | `REINGRESO_CON_DICTAMEN`   | `resultadoFinal == CONDENA_FIRME` |
| `MODIFICA_MONTO`       | `REINGRESO_CON_DICTAMEN`   | `resultadoFinal == CONDENA_FIRME`; `montoResultado > 0` |

**Ejemplo `ABSUELVE`:**
```json
{
  "modoReingresoGestionExterna": "REINGRESO_PARA_NUEVO_FALLO",
  "motivoReingreso": "Juzgado de Paz propone absolucion.",
  "resultadoGestionExterna": "ABSUELVE"
}
```

**Ejemplo `CONFIRMA_CONDENA`:**
```json
{
  "modoReingresoGestionExterna": "REINGRESO_CON_DICTAMEN",
  "motivoReingreso": "Juzgado de Paz confirma condena.",
  "resultadoGestionExterna": "CONFIRMA_CONDENA"
}
```

**Ejemplo `MODIFICA_MONTO`:**
```json
{
  "modoReingresoGestionExterna": "REINGRESO_CON_DICTAMEN",
  "motivoReingreso": "Juzgado de Paz modifica monto.",
  "resultadoGestionExterna": "MODIFICA_MONTO",
  "montoResultado": 125000.00
}
```

**Efectos comunes:**
- Evento `EXTRET` emitido.
- `FalGestionExterna.siActiva = false`, `estadoGestionExterna = REINGRESADA`.
- `acta.situacionAdministrativa = ACTIVA`, `acta.bloqueActual = ANAL`.
- No emite `CIERRA`, `PAGAPR` ni `PCOCNF`.
- `fechaCierreGestionExterna` no se toca.

**Combinaciones prohibidas (fallan con 422):**
- `ABSUELVE` + `REINGRESO_CON_DICTAMEN` (par incoherente)
- `CONFIRMA_CONDENA` + `REINGRESO_PARA_NUEVO_FALLO` (par incoherente)
- `MODIFICA_MONTO` + `REINGRESO_PARA_NUEVO_FALLO` (par incoherente)
- `REINGRESO_PARA_CIERRE` (reservado, siempre bloqueado)
- `REINGRESO_CON_PAGO` (reservado, siempre bloqueado fuera de PAGAPR)
- `REINGRESO_PARA_NUEVO_FALLO` o `REINGRESO_CON_DICTAMEN` sin resultado explicito (resultado null)
- `MODIFICA_MONTO` con `montoResultado` null, cero o negativo

---

## Endpoint pago externo de gestion externa

### POST /api/faltas/actas/{id}/gestion-externa/pago-externo

Registra el pago externo de una gestion externa activa (PAGAPR).

**Request body:**
```json
{
  "observaciones": "string (nullable)"
}
```

**Response:** ComandoResultado con evento PAGAPR (y CIERRA si no hay bloqueantes).

**Errores:**
- 422 UNPROCESSABLE_ENTITY: PrecondicionVioladaException
- 404 NOT_FOUND: ActaNoEncontradaException

---

## Endpoints planificados multi-app (in-memory, sin MariaDB)

Esta seccion documenta grupos de endpoints planificados para administracion base, talonarios/numeracion,
la app de firmas, observaciones/adjuntos, el notificador municipal, el corralon y el portal ciudadano.
El roadmap de implementacion vigente esta en `99-pendientes-siguientes-slices.md`.
Algunos de estos grupos ya cuentan con endpoints productivos implementados (por ejemplo, la numeracion
documental para Firmas: `POST /api/faltas/documentos/{id}/numerar`); ver la seccion correspondiente para el
contrato vigente.

### Administracion base

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST   | /dependencias | Crear dependencia |
| GET    | /dependencias | Listar dependencias activas |
| GET    | /dependencias/{id} | Obtener dependencia |
| PUT    | /dependencias/{id}/versionar | Crear nueva version de dependencia |
| POST   | /inspectores | Crear inspector |
| GET    | /inspectores | Listar inspectores activos |
| GET    | /inspectores/{id} | Obtener inspector |
| POST   | /firmantes | Crear firmante / autoridad |
| GET    | /firmantes | Listar firmantes activos |
| GET    | /firmantes/{id} | Obtener firmante |

### Talonarios y numeracion

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST   | /talonarios | Crear talonario |
| GET    | /talonarios | Listar talonarios activos |
| POST   | /talonarios/{id}/asignar-dependencia | Asignar talonario a dependencia |
| POST   | /talonarios/{id}/asignar-inspector | Asignar talonario a inspector |
| GET    | /talonarios/{id}/siguiente-numero | SUPERADO / NO IMPLEMENTAR: enfoque generico de correlativo reemplazado por operacion sobre el objeto de negocio concreto. Ver `POST /api/faltas/documentos/{id}/numerar`. No existe en codigo. |
| POST   | /talonarios/{id}/anular-numero | Anular y justificar numero |

### App de firmas

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET    | /firmas/pendientes | Bandeja documentos pendientes por firmante autenticado |
| GET    | /firmas/pendientes/{firmante_id} | Bandeja por firmante especifico |
| POST   | /actas/{idActa}/documentos/{idDoc}/firmar | Firmar documento (ampliado con firmante estructurado) |

### Observaciones y adjuntos

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST   | /observaciones | Registrar observacion polimorfica |
| GET    | /actas/{id}/observaciones | Listar observaciones del acta |
| POST   | /adjuntos | Subir adjunto mock |
| GET    | /actas/{id}/adjuntos | Listar adjuntos del acta |

### App notificador municipal

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET    | /notificaciones/pendientes | Bandeja notificador por agente autenticado |
| POST   | /notificaciones/{id}/intento | Registrar intento de notificacion |
| POST   | /notificaciones/{id}/intento/{idIntento}/resultado | Registrar resultado del intento |

### App corralon

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST   | /rodados | Registrar rodado retenido |
| GET    | /rodados/{id} | Consultar rodado |
| GET    | /actas/{id}/rodados | Listar rodados del acta |
| POST   | /rodados/{id}/autorizar-retiro | Autorizar retiro |
| POST   | /rodados/{id}/registrar-retiro | Registrar entrega / retiro |

### Portal ciudadano

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET    | /portal/actas/qr/{codigo_qr} | Consulta publica por QR (token protegido) |
| GET    | /portal/actas/{codigo} | Consulta publica por codigo visible |
| GET    | /portal/actas/{id}/documentos | Documentos visibles al ciudadano |
| POST   | /portal/actas/{id}/pago-voluntario/informar | Informar pago desde portal |

### Nota de implementacion

Todos estos endpoints se implementan sobre estado in-memory.
No se usa MariaDB/JDBC.
Los repositorios son reemplazables por JDBC sin tocar servicios (ver roadmap de DDL/JDBC).

## Endpoints dev/test/demo

Disponibles solo cuando `faltas.demo.enabled=true` (default: `false`).
No son endpoints productivos. No tocan JDBC ni MariaDB.
Con la propiedad ausente o `false`, los controllers no se instancian y las rutas devuelven `404`.

**Property general:** `faltas.demo.enabled=false` (default seguro).

| Metodo | Endpoint | Descripcion | Habilitacion |
|--------|----------|-------------|-------------|
| POST | /demo/dev/reset | Reset in-memory: limpia repos y resembrada plantillas mock | `faltas.demo.reset.enabled=true` |
| GET | /demo/documentos/graph | Graph demo documental (8 casos operativos) | `faltas.demo.enabled=true` |
| GET | /demo/actas/dataset-funcional | Catalogo de cobertura del dataset funcional | `faltas.demo.enabled=true` |
| GET | /demo/actas/{codigo} | Detalle frontend-ready de un acta del dataset funcional | `faltas.demo.enabled=true` |
| GET | /demo/health | Estado de salud del modulo demo | siempre disponible mientras el modulo este iniciado |

### POST /demo/dev/reset

- **Property de habilitacion**: `faltas.demo.reset.enabled=true` (default: `false`)
- **Si deshabilitado**: responde `404` (cuerpo vacio)
- **Que hace**: limpia los 24 repositorios in-memory y resembrada las 8 plantillas mock
- **Campos clave del response (DevResetResponse):**
  - `ejecutado=true`, `modo="memory"`, `fhReset` (timestamp)
  - `repositoriosReseteados` (>= 24), `plantillasRecreadas=8`
  - `casosDatasetFuncional=31`, `errores=0`
  - `repositorios[]`, `acciones[]`, `advertencias[]`
- **Idempotente**: ejecutar N veces produce el mismo estado
- **No toca**: JDBC, MariaDB, SQL, archivos, datos reales

### GET /demo/documentos/graph

- **Sin request body ni query params.**
- **Response:** `DocumentoGraphDemoResultado` con 8 casos operativos documentales.
- **Campos clave para frontend:**
  - `totalCasos`, `casosExitosos`, `casosFallidos`, `completo`, `fhEjecucion`
  - `casos[].codigoCaso`, `casos[].descripcionCaso`, `casos[].accionDocumental`, `casos[].tipoDocu`
  - `casos[].actaId`, `casos[].documentoId`, `casos[].redaccionId` (IDs de navegacion)
  - `casos[].estadoRedaccion`, `casos[].redaccionCompleta`, `casos[].exitoso`
  - `casos[].storageKey` (esquema `mock://`), `casos[].hashDocu` (prefijo `sha256-mock-`)
  - `casos[].errorMensaje` (null si exitoso, presente si fallido)
- **Disponible solo con** `faltas.demo.enabled=true`. Sin la propiedad o con `false`, responde `404`.
- **No modifica estado.** Cada llamada crea actas demo frescas independientes.

### GET /demo/actas/dataset-funcional

- **Sin request body ni query params.**
- **Response:** `DatasetFuncionalCoberturaResultado` con catalogo de 31 actas declarativas.
- **Campos clave para frontend:**
  - `totalActasMock`, `totalCasosUsoCubiertos`, `totalDocumentosEsperados`
  - `coberturaCompletaSegunDominioActual`, `advertencias`
  - `actas[].codigo` (clave estable), `actas[].titulo`, `actas[].descripcion`
  - `actas[].bloqueEsperado`, `actas[].situacionEsperada`, `actas[].bandejaEsperada`
  - `actas[].cerrableEsperado`, `actas[].requiereFallo`, `actas[].requierePago`
  - `actas[].detallePath` (ejemplo: `"/demo/actas/ACT-001-LABRADA"`)
  - `casosUsoCubiertos[]`, `casosUsoPendientes[]`
- **Disponible solo con** `faltas.demo.enabled=true`. Sin la propiedad o con `false`, responde `404`. Solo lectura del catalogo estatico.
- **totalActasMock == actas.length** garantizado.

### GET /demo/actas/{codigo}

- **Path**: `/demo/actas/{codigo}` (bajo `/demo`, no productivo)
- **Codigos validos**: cualquier codigo del `DatasetFuncionalDominioCatalog` (`ACT-001-LABRADA` ... `ACT-031-PAGO-CONDENA-CON-DESCUENTO`)
- **HTTP 200**: para codigo existente -- respuesta frontend-ready completa
- **HTTP 404**: para codigo inexistente -- sin stacktrace expuesto
- **Materializacion real**: ejecuta `CasoUsoFuncionalRunner` aislado, devuelve instancia real (no declarativa)
- **Idempotente**: mismo codigo produce el mismo estado final, mismos conteos de timeline/documentos
- **Campos clave del response (DemoActaDetalleResponse):**
  - `codigo`, `titulo`, `descripcion`, `casoUsoPrincipal`
  - `dataset`: `{bloqueEsperado, situacionEsperada, bandejaEsperada, cerrableEsperado}`
  - `acta`: `{actaId, numeroActa, codigoActa, bloqueActual, estadoProcesal, situacionAdministrativa, resultadoFinal, bandeja, cerrable}`
  - `timeline[]`: eventos append-only reales `{orden, eventoId, tipoEvento, descripcion, fhEvento}` -- ordenados por `ordenLogico`
  - `documentos[]`: documentos reales `{documentoId, tipoDocu, estadoDocu, storageKey, hashDocu, mock, fhGeneracion, plantillaId, descripcion}`
  - `demo`: `{mock=true, materializada, source="DATASET_FUNCIONAL", warnings[]}`
  - `links`: `{self, dataset, graph}`
- **Guardrails**: `storageKey` nunca `file://` ni `s3://`; `hashDocu` nunca hash real.

### Contrato de errores demo

| Caso | HTTP |
|------|------|
| Reset deshabilitado | 404 (cuerpo vacio) |
| Metodo incorrecto en reset (GET) | 405 |
| Codigo de acta inexistente | 404 |
| GET /demo/documentos/graph | 200 siempre |
| GET /demo/actas/dataset-funcional | 200 siempre |
| GET /demo/actas/{codigo valido} | 200 siempre |

### CORS demo

Configurado via `DemoCorsConfig` (WebMvcConfigurer):
- Cubre `/demo/**` y `/api/**`
- Propiedad: `faltas.demo.cors.allowed-origins` (default `*`)
- Para produccion: configurar con URL real del frontend Angular

### GET /demo/health

- **Path**: `/demo/health` (bajo `/demo`, no productivo)
- **HTTP 200**: siempre que el modulo este iniciado.
- **Sin efectos**: no ejecuta reset, no genera documentos, no llama HTTP contra si mismo.
- **Checks internos realizados**:
  - Dataset: `totalActasMock == 31`, `detalleDisponible` (via `detallePath()`).
  - Documentos: `totalPlantillasMock == 8` via `DocumentoPlantillaRepository.listar()`.
  - Reset: informa si `faltas.demo.reset.enabled` esta activo o no.
  - Endpoints: lista estatica de 5 endpoints demo conocidos.
- **Campos clave del response (DemoHealthResponse)**:
  - `status`: `"UP"`
  - `demoReady`: boolean (true si `dataset.ready && documentos.ready`)
  - `fhEjecucion`: timestamp de evaluacion
  - `versionDemo`: identificador de version del modulo demo
  - `dataset`: `{ready, totalActasMock, coberturaCompleta, detalleDisponible}`
  - `documentos`: `{ready, totalPlantillasMock, graphDisponible, storageReal=false}`
  - `reset`: `{endpoint, enabled, defaultSeguro}`
  - `endpoints[]`: lista de `{method, path, ready, descripcion}`
  - `warnings[]`: advertencias si algo no esta completo (no es error fatal)
- **Uso en frontend Angular demo**: consultar `/demo/health` al iniciar la SPA; si `demoReady=true` habilitar navegacion completa; si hay warnings mostrarlos en banner informativo.
- **No valida todavia**: base real, storage real, PDF real, autenticacion productiva.
- **Guardrails**: `storageReal=false` siempre; `resetEnabled=false` por defecto; no expone `s3://` ni `file://`.

GET /demo/health esta cubierto por 16 tests de contrato (ver `06-tests-core.md`).

## Pagos / integracion economica

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /pagos/notificar-movimiento | Notificacion normal de movimiento (tipos: DEUDA_EMITIDA, PAGO_PROCESADO, PAGO_CONFIRMADO, EMISION_ANULADA). Registra movimiento append-only, recalcula proyeccion economica. NO acepta PAGO_REVERTIDO (usar endpoint especifico de reverso). Si el `PAGO_CONFIRMADO` referencia una obligacion no vigente, se clasifica `OBLIGACION_ANTERIOR` y emite `PAGANT` (ver mas abajo). |
| POST | /pagos/revertir-movimiento | Operacion especifica de reverso atomico: crea 1 movimiento PAGO_REVERTIDO, emite exactamente 1 evento PAGREV, recalcula estados. Rechaza reverso por endpoint generico. Idempotente por referenciaExterna. |
| POST | /pagos/resolver-pago-anterior | Resuelve administrativamente un `PAGANT` contra la obligacion vigente del acta: crea un unico movimiento de aplicacion (sin obligacion nueva ni tabla de resolucion propia). Emite `PAGRES`. Idempotente por `movimientoPagoId`. |

Base path completo: `POST /api/faltas/pagos/notificar-movimiento`.

Request: `NotificarMovimientoPagoRequest` (obligacion, forma, plan, tipo/origen/clasificacion, importes, referencia externa, fechas).

Respuestas: 200 movimiento creado; 409 duplicado (`MovimientoPagoDuplicadoException`); 422 precondicion, incluyendo terna `cmtePG`/`prefPG`/`nroPG` ausente o parcial en un `PAGO_CONFIRMADO` original (ver "Deduplicacion de recibo" mas abajo).

### POST /api/faltas/pagos/resolver-pago-anterior

**Finalidad:** resolver administrativamente un movimiento `PAGO_CONFIRMADO`
ya registrado con `clasificacionPago = OBLIGACION_ANTERIOR` (evento `PAGANT`
emitido al notificarse), aplicandolo contra la obligacion vigente del acta.
Contrato de dominio completo en
[`03-comandos-precondiciones-efectos.md`](03-comandos-precondiciones-efectos.md#pago-aplicado-a-obligacion-anterior---comandos-precondiciones-y-efectos).

**Autenticacion:** requerida, misma regla que el resto de
`/api/faltas/pagos/**` (ver "Reglas de cierre de la integracion economica").
Actor obtenido del JWT `sub` via `ActorContextHolder`; el body no declara
actor.

**Request:** `ResolverPagoObligacionAnteriorRequest`
- `actaId`: Long, obligatorio (`@NotNull`)
- `movimientoPagoId`: Long, obligatorio (`@NotNull`)
- `motivo`: String, opcional

**Response:** `ResolverPagoObligacionAnteriorResultado` (200 OK) -- record
no persistido, no existe una tabla `fal_acta_pago_resolucion`:
- `movimientoOriginal`: el `PAGANT` resuelto (`FalActaPagoMovimiento`).
- `movimientoAplicado`: el movimiento de aplicacion creado
  (`PAGO_CONFIRMADO`, `clasificacionPago=NORMAL`,
  `movimientoOrigenId=movimientoOriginal.id`).
- `obligacionOrigenId` / `obligacionAplicadaId`.
- `importeAplicado`: importe total del movimiento original, sin recortar.
- `saldoResultante` / `importeExcedente`: derivados de
  `FalActaEconomiaProyeccion` al momento de la respuesta (`>= 0` siempre;
  a lo sumo uno de los dos es positivo).
- `motivo`, `actor`, `fhResolucion`.

En un reintento idempotente compatible, `movimientoOriginal` /
`movimientoAplicado` / `importeAplicado` son exactos (el movimiento de
aplicacion es inmutable); `saldoResultante` / `importeExcedente` se
recalculan contra el estado economico actual, no contra un snapshot
historico (no existe tal snapshot persistido). `motivo`, `actor` y
`fhResolucion` reportan los datos **historicos** de la primera ejecucion
(no el actor ni el motivo de la solicitud de reintento).

**Idempotencia:** un reintento con el mismo `movimientoPagoId`, la misma
obligacion vigente que recibio la aplicacion previa y un `motivo`
normalizado equivalente devuelve **200** con el mismo resultado, sin crear
un segundo movimiento de aplicacion ni un segundo `PAGRES`. Un reintento
con la obligacion vigente cambiada o un motivo distinto es un conflicto.

**Errores:**
| Caso | HTTP | Excepcion |
|------|------|-----------|
| Sin Bearer valido | 401 | (Spring Security) |
| `actaId` o `movimientoPagoId` ausentes en el body | 400 | Bean Validation |
| Acta inexistente | 404 | `ActaNoEncontradaException` |
| Movimiento inexistente | 404 | `PagoMovimientoNoEncontradoException` |
| Obligacion origen o vigente inexistente | 404 | `ObligacionPagoNoEncontradaException` |
| Actor resuelto nulo o en blanco | 422 | `PrecondicionVioladaException` |
| Movimiento no `PAGO_CONFIRMADO`, `clasificacionPago != OBLIGACION_ANTERIOR`, movimiento ya derivado, obligacion origen de otra acta, `importeTotal` invalido, u obligacion origen vigente | 422 | `PrecondicionVioladaException` |
| Reintento incompatible (distinta obligacion aplicada o motivo distinto) | 409 | `ResolucionPagoAnteriorConflictoException` |

### Reglas de cierre de la integracion economica

- **Autenticacion obligatoria**: `/api/faltas/pagos/**` exige `Authorization: Bearer <jwt>` con `sub` valido (no vacio, hasta 36 caracteres). Sin Bearer valido responde **401**; no hay fallback anonimo `integracion-externa`.
- **Idempotencia**: clave `origenMovimiento + referenciaExterna`. Reintento identico -> `ALREADY_EXISTS` (no crea otro movimiento ni evento); mismo origen/referencia con importe u obligacion distintos -> **409**. Un reintento sin `fhMovimiento` informada no falla por avance del Clock.
- **Conciliacion**: no crea movimiento ni evento ni muta el movimiento original; reclasifica importes agregados usando el input mock absoluto de Tesoreria; es idempotente; payload incompatible -> **409**.
- **Reversos**: solo tipos reversibles (`PAGO_CONFIRMADO`); reverso identico es idempotente; segundo reverso distinto sobre un original ya revertido se rechaza.
- **Pago aplicado a obligacion anterior**: ver `POST /api/faltas/pagos/resolver-pago-anterior` arriba para el contrato completo de `PAGANT`/`PAGRES`. `OBLIGACION_ANTERIOR` es una clasificacion derivada, nunca declarable contra una obligacion vigente (422 si se intenta).
- **Deduplicacion de recibo**: `origenMovimiento + cmtePG + prefPG + nroPG` identifica un recibo fisico; reintentar el mismo recibo con una `referenciaExterna` distinta es **409** (`MovimientoPagoDuplicadoException`). La unicidad se verifica de forma atomica dentro de la escritura del movimiento (no solo con una verificacion previa), por lo que dos solicitudes concurrentes con el mismo recibo nunca insertan ambas. La terna `cmtePG/prefPG/nroPG` es obligatoria (completa, nunca parcial) en todo `PAGO_CONFIRMADO` original; ausente o parcial es **422** (`PrecondicionVioladaException`). El movimiento de aplicacion generado por `resolver-pago-anterior` (`movimientoOrigenId != null`) queda exento: su recibo se recupera navegando al movimiento original.

---

## Endpoint de numeracion documental para Firmas

### POST /api/faltas/documentos/{documentoId}/numerar

**Finalidad:** integracion controlada con la aplicacion de Firmas para asignar un numero de talonario electronico a un documento antes de que Firmas renderice su contenido definitivo y calcule el hash.

**Autenticacion:** requerida. Actor obtenido del JWT `sub` via `ActorContextHolder`. Sin Bearer valido: HTTP 401.

**Request:** sin body. El actor, la politica y el talonario los resuelve el sistema internamente. El cliente no elige correlativo, politica ni talonario.

**Response:** `NumerarDocumentoParaFirmasResponse`
  - `documentoId`: Long
  - `yaEstabaNumerado`: boolean
  - `nroDocu`: String (numero asignado)
  - `idTalonario`: Long
  - `nroTalonarioUsado`: Integer
  - `momentoAplicado`: MomentoNumeracionDocu
  - `estadoDocu`: EstadoDocu

**HTTP 201 CREATED:** si numera por primera vez.
**HTTP 200 OK:** si devuelve idempotentemente el numero ya asignado (`yaEstabaNumerado = true`).

**Momento elegible inicial:** `AL_FIRMAR` para la primera numeracion desde Firmas.
**Momentos anteriores ya numerados:** se devuelven idempotentemente (200 OK).

**Orden obligatorio:** numerar -> contenido definitivo/hash -> firma.

**No es:** administracion de talonarios. No entrega correlativos libres. No es API generica.

**Errores:**
| Caso | HTTP |
|------|------|
| Documento no encontrado | 404 |
| Plantilla no requiere numeracion | 422 |
| Momento incompatible | 422 |
| Estado del documento incompatible | 422 |
| Talonario no vigente / sin politica | 422 |
| Sin Bearer valido | 401 |

---

## Contrato global de errores HTTP

### GlobalFaltasControllerAdvice

Todos los endpoints del modulo `api-faltas-core` comparten un contrato de errores canonico gestionado por `GlobalFaltasControllerAdvice`.

#### DTO de respuesta de error: ErrorResponse

```json
{
  "codigoError": "ACTA_NO_ENCONTRADA",
  "mensaje": "Recurso no encontrado",
  "detalle": null,
  "correlacionId": null
}
```

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| `codigoError` | String | Codigo semantico del error. Nunca nulo ni vacio. |
| `mensaje` | String | Descripcion legible. Sin info interna. |
| `detalle` | String | Opcional. Detalles de validacion o contexto adicional. |
| `correlacionId` | String | Reservado para uso futuro (trazabilidad). |

#### Tabla de HTTP status por categoria de excepcion

| HTTP | Categoria | Excepciones cubiertas |
|------|-----------|----------------------|
| 404 | Recurso no encontrado | Excepciones `*NoEncontrad*Exception` del dominio + `NoResourceFoundException` (ruta sin handler / demo deshabilitado) |
| 422 | Precondicion violada | `PrecondicionVioladaException` |
| 409 | Conflicto OCC | `ConcurrenciaConflictoException` |
| 409 | Pago duplicado | `MovimientoPagoDuplicadoException` |
| 409 | Acuse/conciliacion duplicada | `AcuseDuplicadoException`, `ConciliacionIncompatibleException` |
| 400 | Token QR invalido | `QrTokenInvalidoException` |
| 400 | Bean Validation | `MethodArgumentNotValidException`, `ConstraintViolationException` |
| 400 | JSON invalido | `HttpMessageNotReadableException` |
| 400 | Parametro faltante/invalido | `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` |
| 405 | Metodo no soportado | `HttpRequestMethodNotSupportedException` |
| 415 | Content-Type no soportado | `HttpMediaTypeNotSupportedException` |
| 4xx-5xx | ResponseStatusException | Pass-through del status code de la excepcion Spring |
| 500 | Error interno (fallback) | Cualquier excepcion no cubierta (se logea como ERROR) |

#### Contrato de seguridad

- **401**: gestionado por `SecurityConfig.authenticationEntryPoint` (filtro de seguridad). NO interceptado por el advice.
- **403**: gestionado por Spring Security. NO interceptado por el advice.
- `Throwable` y `Error` de JVM no son capturados.
- Stack traces y mensajes internos nunca se exponen al cliente.

#### Handlers locales

Los controladores del modulo no implementan handlers locales `@ExceptionHandler` propios. El contrato canonico `ErrorResponse` gestionado por `GlobalFaltasControllerAdvice` es el unico mecanismo de respuesta de error.

#### Tests del contrato

| Test | Clase | Cobertura |
|------|-------|-----------|
| Unitario por handler | `GlobalFaltasControllerAdviceTest` | Todos los handlers cubiertos |
| Cobertura arquitectural | `GlobalFaltasControllerAdviceCoverageTest` | Excepciones de dominio, HTTP status y `codigoError` validados |
| Contrato HTTP integrado | `FlujoCoreIT`, `DemoDeshabilitadoTest`, `DemoActaDetalleContractTest` | Suite completa (ver `06-tests-core.md` para el conteo vigente) |
