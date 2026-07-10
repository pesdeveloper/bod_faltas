# 05 - API Core Endpoints

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
| POST | /actas/{id}/documentos | Generar documento |
| POST | /actas/{idActa}/documentos/{idDoc}/firmar | Firmar documento (acta o fallo) |
| GET  | /actas/{id}/documentos | Listar documentos del acta |
| POST | /documentos/{documentoId}/numerar | Numerar documento para integracion con Firmas (D-18; requiere JWT Bearer) |

## Notificaciones

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/notificaciones | Enviar notificacion |
| POST | /notificaciones/{id}/positiva | Registrar acuse positivo |
| POST | /notificaciones/{id}/negativa | Registrar acuse negativo |
| POST | /notificaciones/{id}/vencida | Registrar vencida |
| GET  | /actas/{id}/notificaciones | Listar notificaciones del acta |

## Pago Voluntario (Slice 2A)

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/pago-voluntario/solicitar | Solicitar pago voluntario |
| POST | /actas/{id}/pago-voluntario/fijar-monto | Fijar monto a pagar |
| POST | /actas/{id}/pago-voluntario/informar | Infractor informa pago realizado |
| POST | /actas/{id}/pago-voluntario/confirmar | Confirmar pago (cierra si sin bloqueantes) |
| POST | /actas/{id}/pago-voluntario/observar | Observar/rechazar pago informado |
| POST | /actas/{id}/pago-voluntario/vencer | Vencer pago, habilitar analisis |
| GET  | /actas/{id}/pago-voluntario | Obtener estado del pago voluntario |

## Fallo (Slice 3A)

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/fallos/absolutorio | Dictar fallo absolutorio |
| POST | /actas/{id}/fallos/condenatorio | Dictar fallo condenatorio |
| GET  | /actas/{id}/fallo | Obtener fallo activo del acta |

Nota: firma y notificacion del fallo usan los endpoints existentes de documentos y notificaciones.
No se agregan endpoints de apelacion, pago condena, firmeza ni gestion externa en este slice.

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
## Apelacion (Slice 3B)

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/apelaciones | Registrar apelacion sobre fallo condenatorio notificado |
| GET  | /actas/{id}/apelacion | Obtener apelacion activa del acta |

Nota: resolucion de apelacion (APERAZ / APEABS) implementada en Slice 3C.

## Firmeza de condena (Slice 3D)

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /actas/{id}/firmeza/vencer-plazo-apelacion | Vencer plazo sin apelacion - genera PLAVNC+CONFIR |
| POST | /actas/{id}/firmeza/por-apelacion-rechazada | Declarar firmeza por apelacion rechazada - genera CONFIR |

DTO: DeclararFirmezaRequest { observaciones }

---

## Slice 5: Endpoints de pago de condena

### POST /api/faltas/actas/{id}/pago-condena/informar

Informa un pago de condena para el acta.

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
- `404` si acta no encontrada
- `422` si precondicion violada (resultadoFinal != CONDENA_FIRME, monto <= 0, etc.)

### POST /api/faltas/actas/{id}/pago-condena/confirmar

Confirma el pago de condena informado. Cierra el acta si no hay bloqueantes.

**Body:** `{}` (observaciones opcionales)

**Respuesta exitosa:** `200 OK` con `ComandoResultado` (evento `PCOCNF`)

**Errores:**
- `404` si acta no encontrada
- `422` si bloqueantes activos, pago no informado, o precondicion violada

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


---

## Slice 6B: Endpoint de reingreso

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

Valores validos de `modoReingresoGestionExterna` en Slice 6B:
- `REINGRESO_PARA_REVISION`
- `REINGRESO_SIN_PAGO`

Valores prohibidos en Slice 6B (fallan con 422):
- `REINGRESO_PARA_CIERRE`
- `REINGRESO_PARA_NUEVO_FALLO`

**Respuesta exitosa:** `200 OK` con `ComandoResultado` (evento `EXTRET`)

**Errores:**
- `404` si acta no encontrada
- `422` si precondicion violada (modo no habilitado, motivo vacio, sin gestion activa, etc.)

**Efectos del reingreso:**
- `FalGestionExterna.siActiva = false`
- `FalGestionExterna.estadoGestionExterna = REINGRESADA`
- `acta.situacionAdministrativa = ACTIVA`
- `acta.bloqueActual = ANAL`
- Registra `EXTRET`
- Recalcula snapshot



---

## Slice 6D-1: Pares resultado/modo habilitados en endpoint /reingresar

### Validacion de pares resultado/modo (activa desde Slice 6D-1)

Cuando `resultadoGestionExterna` es informado (no null), se valida el par resultado/modo.

**Pares permitidos:**

| resultadoGestionExterna | modoReingresoGestionExterna |
|------------------------|-----------------------------|
| `SIN_PAGO`             | `REINGRESO_SIN_PAGO`        |
| `SIN_CAMBIOS`          | `REINGRESO_PARA_REVISION`   |

**Ejemplos validos (Slice 6D-1):**

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

---

## Slice 6D-2: Pares con dictamen externo habilitados en endpoint /reingresar

### Validacion de pares dictamen externo (activa desde Slice 6D-2)

Extiende Slice 6D-1. Habilita tres casos de reingreso con dictamen externo.

**Nuevo campo en request:**

| Campo | Tipo | Obligatorio | Descripcion |
|-------|------|-------------|-------------|
| `montoResultado` | `BigDecimal` | Solo si resultado == `MODIFICA_MONTO` (> 0) | Monto externo informado. |

**Pares habilitados en Slice 6D-2:**

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

**Efectos comunes (Slice 6D-2):**
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

## Slice 6C: Endpoint pago externo de gestion externa

### POST /api/faltas/actas/{id}/gestion-externa/pago-externo

Registra el pago externo de una gestion externa activa (PAGAPR).

**Request body:**
`json
{
  "observaciones": "string (nullable)"
}
`

**Response:** ComandoResultado con evento PAGAPR (y CIERRA si no hay bloqueantes).

**Errores:**
- 422 UNPROCESSABLE_ENTITY: PrecondicionVioladaException
- 404 NOT_FOUND: ActaNoEncontradaException


---

## Etapa 8 — Endpoints planificados multi-app (in-memory, sin MariaDB)

Esta seccion documenta los grupos de endpoints que se implementaran en Etapa 8.
La mayoria se implementan bloque a bloque segun el roadmap de 99-pendientes.
Nota: algunos endpoints de Etapa 8 han sido implementados en slices posteriores (por ejemplo, D-18 implemento POST /api/faltas/documentos/{id}/numerar). Ver las secciones de cada slice para el contrato vigente.

### Bloque 8A — Administracion base

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

### Bloque 8B — Talonarios y numeracion

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST   | /talonarios | Crear talonario |
| GET    | /talonarios | Listar talonarios activos |
| POST   | /talonarios/{id}/asignar-dependencia | Asignar talonario a dependencia |
| POST   | /talonarios/{id}/asignar-inspector | Asignar talonario a inspector |
| GET    | /talonarios/{id}/siguiente-numero | SUPERADO / NO IMPLEMENTAR: enfoque generico de correlativo reemplazado por operacion sobre el objeto de negocio concreto. Ver POST /api/faltas/documentos/{id}/numerar (D-18). No existe en codigo. |
| POST   | /talonarios/{id}/anular-numero | Anular y justificar numero |

### Bloque 8C — App de firmas

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET    | /firmas/pendientes | Bandeja documentos pendientes por firmante autenticado |
| GET    | /firmas/pendientes/{firmante_id} | Bandeja por firmante especifico |
| POST   | /actas/{idActa}/documentos/{idDoc}/firmar | Firmar documento (ampliado con firmante estructurado) |

### Bloque 8D — Observaciones y adjuntos

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST   | /observaciones | Registrar observacion polimorfica |
| GET    | /actas/{id}/observaciones | Listar observaciones del acta |
| POST   | /adjuntos | Subir adjunto mock |
| GET    | /actas/{id}/adjuntos | Listar adjuntos del acta |

### Bloque 8E — App notificador municipal

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET    | /notificaciones/pendientes | Bandeja notificador por agente autenticado |
| POST   | /notificaciones/{id}/intento | Registrar intento de notificacion |
| POST   | /notificaciones/{id}/intento/{idIntento}/resultado | Registrar resultado del intento |

### Bloque 8F — App corralo'n

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST   | /rodados | Registrar rodado retenido |
| GET    | /rodados/{id} | Consultar rodado |
| GET    | /actas/{id}/rodados | Listar rodados del acta |
| POST   | /rodados/{id}/autorizar-retiro | Autorizar retiro |
| POST   | /rodados/{id}/registrar-retiro | Registrar entrega / retiro |

### Bloque 8G — Portal ciudadano

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET    | /portal/actas/qr/{codigo_qr} | Consulta publica por QR (token protegido) |
| GET    | /portal/actas/{codigo} | Consulta publica por codigo visible |
| GET    | /portal/actas/{id}/documentos | Documentos visibles al ciudadano |
| POST   | /portal/actas/{id}/pago-voluntario/informar | Informar pago desde portal |

### Nota de implementacion

Todos estos endpoints se implementan sobre estado in-memory.
No se usa MariaDB/JDBC en Etapa 8.
Los repositorios son reemplazables por JDBC en Etapa 9 sin tocar servicios.

## Endpoints dev/test/demo

Disponibles solo cuando `faltas.demo.enabled=true` (default: `false`).
No son endpoints productivos. No tocan JDBC ni MariaDB.
Con la propiedad ausente o `false`, los controllers no se instancian y las rutas devuelven `404`.

**Property general:** `faltas.demo.enabled=false` (default seguro).
**D-12 CERRADO** (2026-07-09).

| Metodo | Endpoint | Descripcion | Habilitacion |
|--------|----------|-------------|-------------|
| POST | /demo/dev/reset | Reset in-memory: limpia repos y resembrada plantillas mock | `faltas.demo.reset.enabled=true` |
| GET | /demo/documentos/graph | Graph demo documental (8 casos operativos) | ``faltas.demo.enabled=true`` |
| GET | /demo/actas/dataset-funcional | Catalogo de cobertura del dataset funcional | ``faltas.demo.enabled=true`` |

### POST /demo/dev/reset — Slice 8F-5

- **Property de habilitacion**: `faltas.demo.reset.enabled=true` (default: `false`)
- **Si deshabilitado**: responde `404`
- **Que hace**: limpia los 24 repositorios in-memory y resembrada las 8 plantillas mock
- **Response**: `DevResetResponse` con ejecutado, fhReset, repositoriosReseteados, plantillasRecreadas, casosDatasetFuncional, etc.
- **Idempotente**: si, ejecutar N veces produce el mismo estado
- **No toca**: JDBC, MariaDB, SQL, archivos, datos reales

### GET /demo/documentos/graph - Slice 8F-4

- **Sin request body ni query params.**
- **Response:** `DocumentoGraphDemoResultado` con 8 casos operativos documentales.
- **Campos clave para frontend:**
  - `totalCasos`, `casosExitosos`, `casosFallidos`, `completo`, `fhEjecucion`
  - `casos[].codigoCaso`, `casos[].descripcionCaso`, `casos[].accionDocumental`, `casos[].tipoDocu`
  - `casos[].actaId`, `casos[].documentoId`, `casos[].redaccionId` (IDs de navegación)
  - `casos[].estadoRedaccion`, `casos[].redaccionCompleta`, `casos[].exitoso`
  - `casos[].storageKey` (esquema `mock://`), `casos[].hashDocu` (prefijo `sha256-mock-`)
  - `casos[].errorMensaje` (null si exitoso, presente si fallido)
- **Disponible solo con** ``faltas.demo.enabled=true``. Sin la propiedad o con ``false``, responde ``404``.
- **No modifica estado.** Cada llamada crea actas demo frescas independientes.

### GET /demo/actas/dataset-funcional - Slice 8F-4B

- **Sin request body ni query params.**
- **Response:** `DatasetFuncionalCoberturaResultado` con catálogo de 31 actas declarativas.
- **Campos clave para frontend:**
  - `totalActasMock`, `totalCasosUsoCubiertos`, `totalDocumentosEsperados`
  - `coberturaCompletaSegunDominioActual`, `advertencias`
  - `actas[].codigo` (clave estable), `actas[].titulo`, `actas[].descripcion`
  - `actas[].bloqueEsperado`, `actas[].situacionEsperada`, `actas[].bandejaEsperada`
  - `actas[].cerrableEsperado`, `actas[].requiereFallo`, `actas[].requierePago`
  - `casosUsoCubiertos[]`, `casosUsoPendientes[]`
- **Disponible solo con** ``faltas.demo.enabled=true``. Sin la propiedad o con ``false``, responde ``404``. Solo lectura del catálogo estático.
- **totalActasMock == actas.length** garantizado.

### POST /demo/dev/reset - Slice 8F-5

- **Property de habilitación**: `faltas.demo.reset.enabled=true` (default: `false`)
- **Si deshabilitado**: responde `404` (cuerpo vacío)
- **Que hace**: limpia los 24 repositorios in-memory y resembrada las 8 plantillas mock
- **Campos clave del response (DevResetResponse):**
  - `ejecutado=true`, `modo="memory"`, `fhReset` (timestamp)
  - `repositoriosReseteados` (≥ 24), `plantillasRecreadas=8`
  - `casosDatasetFuncional=31`, `errores=0`
  - `repositorios[]`, `acciones[]`, `advertencias[]`
- **Idempotente**: ejecutar N veces produce el mismo estado
- **No toca**: JDBC, MariaDB, SQL, archivos, datos reales

### Contrato de errores demo

| Caso | HTTP |
|------|------|
| Reset deshabilitado | 404 (cuerpo vacío) |
| Método incorrecto en reset (GET) | 405 |
| GET /demo/documentos/graph | 200 siempre |
| GET /demo/actas/dataset-funcional | 200 siempre |

### CORS demo (Slice 8F-6)

Configurado via `DemoCorsConfig` (WebMvcConfigurer):
- Cubre `/demo/**` y `/api/**`
- Propiedad: `faltas.demo.cors.allowed-origins` (default `*`)
- Para producción: configurar con URL real del frontend Angular


### GET /demo/actas/{codigo} - Slice 8F-7 (NUEVO)

- **Path**: /demo/actas/{codigo} (bajo /demo, no productivo)
- **Codigos validos**: cualquier codigo del DatasetFuncionalDominioCatalog (ACT-001-LABRADA ... ACT-031-PAGO-CONDENA-CON-DESCUENTO)
- **HTTP 200**: para codigo existente — respuesta frontend-ready completa
- **HTTP 404**: para codigo inexistente — sin stacktrace expuesto
- **Materializacion real**: ejecuta CasoUsoFuncionalRunner aislado, devuelve instancia real (no declarativa)
- **Idempotente**: mismo codigo → mismo estado final, mismos conteos de timeline/documentos
- **Campos clave del response (DemoActaDetalleResponse):**
  - codigo, 	itulo, descripcion, casoUsoPrincipal
  - dataset: {bloqueEsperado, situacionEsperada, bandejaEsperada, cerrableEsperado}
  - cta: {actaId, numeroActa, codigoActa, bloqueActual, estadoProcesal, situacionAdministrativa, resultadoFinal, bandeja, cerrable}
  - 	imeline[]: eventos append-only reales {orden, eventoId, tipoEvento, descripcion, fhEvento} — ordenados por ordenLogico
  - documentos[]: documentos reales {documentoId, tipoDocu, estadoDocu, storageKey, hashDocu, mock, fhGeneracion, plantillaId, descripcion}
  - demo: {mock=true, materializada, source="DATASET_FUNCIONAL", warnings[]}
  - links: {self, dataset, graph}
- **Guardrails**: storageKey nunca
ile:// ni s3://; hashDocu nunca hash real

### GET /demo/actas/dataset-funcional (actualizado Slice 8F-7)

- **Campo nuevo aditivo**: detallePath por cada acta en el array ctas[]
- Ejemplo: "detallePath": "/demo/actas/ACT-001-LABRADA"
- No rompe ningun test previo ni contrato existente

### Contrato de errores demo (actualizado 8F-7)

| Caso | HTTP |
|------|------|
| Reset deshabilitado | 404 (cuerpo vacio) |
| Metodo incorrecto en reset (GET) | 405 |
| Codigo de acta inexistente | 404 |
| GET /demo/documentos/graph | 200 siempre |
| GET /demo/actas/dataset-funcional | 200 siempre |
| GET /demo/actas/{codigo valido} | 200 siempre |

### GET /demo/health - Slice 8F-8 (NUEVO)

- **Path**: /demo/health (bajo /demo, no productivo)
- **HTTP 200**: siempre que el modulo este iniciado.
- **Sin efectos**: no ejecuta reset, no genera documentos, no llama HTTP contra si mismo.
- **Checks internos realizados**:
  - Dataset: 	otalActasMock == 31, detalleDisponible (via detallePath()).
  - Documentos: 	otalPlantillasMock == 8 via DocumentoPlantillaRepository.listar().
  - Reset: informa si
altas.demo.reset.enabled esta activo o no.
  - Endpoints: lista estatica de 5 endpoints demo conocidos.
- **Campos clave del response (DemoHealthResponse)**:
  - status: "UP"
  - demoReady: boolean (true si dataset.ready && documentos.ready)
  -
hEjecucion: timestamp de evaluacion
  -
ersionDemo: "8F-8"
  - dataset: {ready, totalActasMock, coberturaCompleta, detalleDisponible}
  - documentos: {ready, totalPlantillasMock, graphDisponible, storageReal=false}
  -
eset: {endpoint, enabled, defaultSeguro}
  - endpoints[]: lista de {method, path, ready, descripcion}
  - warnings[]: advertencias si algo no esta completo (no es error fatal)
- **Uso en frontend Angular demo**: consultar /demo/health al iniciar la SPA; si demoReady=true habilitar navegacion completa; si hay warnings mostrarlos en banner informativo.
- **No valida todavia**: base real, storage real, PDF real, autenticacion productiva.
- **Guardrails**: storageReal=false siempre;
esetEnabled=false por defecto; no expone s3:// ni
ile://.

### GAP-8 — CERRADO (Slice 8F-8)

GET /demo/health implementado, testeado con 16 tests de contrato.

## Pagos / integracion (Slice 8F-11M-B1)

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /pagos/notificar-movimiento | Notificacion normal de movimiento (tipos: DEUDA_EMITIDA, PAGO_PROCESADO, PAGO_CONFIRMADO, EMISION_ANULADA). Registra movimiento append-only, recalcula proyeccion economica. NO acepta PAGO_REVERTIDO (usar endpoint especifico de reverso). |
| POST | /pagos/revertir-movimiento | Operacion especifica de reverso atomico: crea 1 movimiento PAGO_REVERTIDO, emite exactamente 1 evento PAGREV, recalcula estados. Rechaza reverso por endpoint generico. Idempotente por referenciaExterna. |

Base path completo: `POST /api/faltas/pagos/notificar-movimiento`.

Request: `NotificarMovimientoPagoRequest` (obligacion, forma, plan, tipo/origen/clasificacion, importes, referencia externa, fechas).

Respuestas: 200 movimiento creado; 409 duplicado (`MovimientoPagoDuplicadoException`); 422 precondicion.

### 8F-11M-B1-R2 (cierre)

- **Autenticacion obligatoria**: `/api/faltas/pagos/**` exige `Authorization: Bearer <jwt>` con `sub` valido (no vacio, hasta 36 caracteres). Sin Bearer valido responde **401**; no hay fallback anonimo `integracion-externa`.
- **Idempotencia**: clave `origenMovimiento + referenciaExterna`. Reintento identico -> `ALREADY_EXISTS` (no crea otro movimiento ni evento); mismo origen/referencia con importe u obligacion distintos -> **409**. Un reintento sin `fhMovimiento` informada no falla por avance del Clock.
- **Conciliacion**: no crea movimiento ni evento ni muta el movimiento original; reclasifica importes agregados usando el input mock absoluto de Tesoreria; es idempotente; payload incompatible -> **409**.
- **Reversos**: solo tipos reversibles (`PAGO_CONFIRMADO`); reverso identico es idempotente; segundo reverso distinto sobre un original ya revertido se rechaza.

---

## CIERRE-D14-D18 (2026-07-09) - Endpoint de numeracion documental para Firmas

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

## CIERRE-R11 (2026-07-09) - Contrato global de errores HTTP

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
| 404 | Recurso no encontrado | 29 excepciones `*NoEncontrad*Exception` del dominio + `NoResourceFoundException` (ruta sin handler / demo deshabilitado) |
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

#### Cobertura de handlers locales eliminados

Los 17 controladores del modulo tenian handlers locales `@ExceptionHandler` que devolvian `Map<String, String>{"error": "..."}`.
Fueron eliminados sistematicamente. El contrato canonico `ErrorResponse` es el unico mecanismo de respuesta de error.

#### Tests del contrato

| Test | Clase | Cobertura |
|------|-------|-----------|
| Unitario por handler | `GlobalFaltasControllerAdviceTest` | 14 tests, todos los handlers cubiertos |
| Cobertura arquitectural | `GlobalFaltasControllerAdviceCoverageTest` | 50 excepciones dominio, HTTP status y codigoError validados |
| Contrato HTTP integrado | `FlujoCoreIT`, `DemoDeshabilitadoTest`, `DemoActaDetalleContractTest` | Suite completa 2478 tests |
