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
Ninguno existe todavia. Se implementan bloque a bloque segun el roadmap de 99-pendientes.

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
| GET    | /talonarios/{id}/siguiente-numero | Obtener y reservar siguiente numero |
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
