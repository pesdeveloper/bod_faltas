# 108 - Frontend-Ready Demo: AuditorÃ­a de Endpoints, Payloads y Flujo

**Slice:** 8F-6
**Fecha:** 2026-07-03
**MÃ³dulo:** backend/api-faltas-core (in-memory, sin MariaDB)

---

## Objetivo

AuditorÃ­a backend-only, frontend-ready.
Dejar el backend listo para que un frontend Angular consuma los endpoints demo sin adivinar contratos, estados, errores o pasos de navegaciÃ³n.

---

## Inventario real de endpoints demo

### Endpoints bajo /demo (frontend-consumibles)

| MÃ©todo | Path | Controller | Servicio | Guarda |
|--------|------|-----------|---------|--------|
| GET | /demo/documentos/graph | DocumentoGraphDemoController | DocumentoGraphDemoService | ninguna (siempre activo) |
| GET | /demo/actas/dataset-funcional | DatasetFuncionalDemoController | DatasetFuncionalDominioCatalog | ninguna (siempre activo) |
| POST | /demo/dev/reset | DevResetController | DevInMemoryResetService | faltas.demo.reset.enabled=true |

### Endpoint operativo de referencia (para contexto)

El frontend tambiÃ©n necesitarÃ¡ consumir los endpoints operativos bajo /api/faltas para acciones reales.
Estos estÃ¡n documentados en 05-api-core-endpoints.md.
La auditorÃ­a de este slice se focaliza en los 3 endpoints /demo.

---

## AnÃ¡lisis de payloads

### GET /demo/documentos/graph

**Response:** DocumentoGraphDemoResultado

```json
{
  "totalCasos": 8,
  "casosExitosos": 8,
  "casosFallidos": 0,
  "completo": true,
  "fhEjecucion": "2026-07-03T10:00:00",
  "casos": [
    {
      "codigoCaso": "CASO-01",
      "descripcionCaso": "Fallo condenatorio - acto administrativo",
      "accionDocumental": "EMITIR_FALLO",
      "tipoDocu": "ACTO_ADMINISTRATIVO",
      "actaId": 1,
      "documentoId": 1,
      "redaccionId": 1,
      "estadoRedaccion": "CONFIRMADA",
      "redaccionCompleta": true,
      "storageKey": "mock://...",
      "hashDocu": "sha256-mock-...",
      "fhGeneracion": "2026-07-03T10:00:00",
      "mock": true,
      "exitoso": true,
      "errorMensaje": null
    }
  ]
}
```

**EvaluaciÃ³n frontend:**
- [OK] IDs estables por ejecuciÃ³n: actaId, documentoId, redaccionId presentes
- [OK] CÃ³digo y descripciÃ³n legible para presentar en lista
- [OK] Estado tÃ©cnico: estadoRedaccion, redaccionCompleta, exitoso
- [OK] Metadatos mock: storageKey y hashDocu siempre mock:// / sha256-mock-
- [OK] errorMensaje null cuando exitoso, presente cuando fallido
- [OK] Conteos agregados en raÃ­z: totalCasos, casosExitosos, casosFallidos, completo
- [GAP-1] accionDocumental y tipoDocu son enums tÃ©cnicos sin label presentacional
- [GAP-2] No hay campo "label" o "nombre" derivado para mostrar en UI sin mapeo en frontend

### GET /demo/actas/dataset-funcional

**Response:** DatasetFuncionalCoberturaResultado

```json
{
  "totalActasMock": 31,
  "totalCasosUsoCubiertos": 25,
  "totalDocumentosEsperados": 45,
  "coberturaCompletaSegunDominioActual": false,
  "actas": [
    {
      "codigo": "ACT-001-LABRADA",
      "titulo": "Acta reciÃ©n labrada en captura",
      "descripcion": "...",
      "casoUsoPrincipal": "Slice 1 - LabrarActa",
      "casosUsoCubiertos": ["Slice 1 - LabrarActa", ...],
      "bloqueEsperado": "CAPT",
      "situacionEsperada": "ACTIVA",
      "resultadoFinalEsperado": "SIN_RESULTADO_FINAL",
      "bandejaEsperada": "ACTAS_EN_ENRIQUECIMIENTO",
      "cerrableEsperado": false,
      "paralizadaEsperada": false,
      "requiereFallo": false,
      "requierePago": false,
      "documentosEsperados": [],
      "eventosEsperados": ["ACTLAB"],
      "endpointsServiciosCubiertos": ["POST /api/faltas/actas/labrar"],
      "observaciones": ["Estado inicial puro..."]
    }
  ],
  "casosUsoCubiertos": ["Slice 1 - LabrarActa", ...],
  "casosUsoPendientes": [...],
  "advertencias": []
}
```

**EvaluaciÃ³n frontend:**
- [OK] CÃ³digos estables: codigo es clave estable y Ãºnica
- [OK] TÃ­tulos y descripciones legibles
- [OK] Estados tÃ©cnicos: bloqueEsperado, situacionEsperada, resultadoFinalEsperado, bandejaEsperada
- [OK] Flags booleanos para drill-down: cerrableEsperado, requiereFallo, requierePago, etc.
- [OK] Conteos en raÃ­z: totalActasMock, totalCasosUsoCubiertos, totalDocumentosEsperados
- [OK] coberturaCompletaSegunDominioActual para banner de estado
- [GAP-3] No hay endpoint GET /demo/actas/{codigo} para drill-down por acta individual
- [GAP-4] bloqueEsperado, situacionEsperada, bandejaEsperada son enums tÃ©cnicos sin labels presentacionales
- [GAP-5] Las actas del dataset son definiciones declarativas, no instancias reales en repositorio
  (un GET /actas/{id} real no existe para estas actas mock)

### POST /demo/dev/reset

**Response:** DevResetResponse

```json
{
  "ejecutado": true,
  "modo": "memory",
  "fhReset": "2026-07-03T10:00:00",
  "repositoriosReseteados": 24,
  "plantillasRecreadas": 8,
  "actasDemoDisponibles": 0,
  "casosDatasetFuncional": 31,
  "errores": 0,
  "repositorios": ["InMemoryActaRepository", ...],
  "acciones": ["Reset ejecutado", ...],
  "advertencias": []
}
```

**EvaluaciÃ³n frontend:**
- [OK] ejecutado=true confirma Ã©xito
- [OK] modo="memory" identifica perfil activo
- [OK] fhReset provee timestamp de la Ãºltima operaciÃ³n de reset
- [OK] Conteos de repos y plantillas para confirmar estado post-reset
- [OK] casosDatasetFuncional=31 confirma dataset de referencia intacto
- [OK] errores=0 para verificaciÃ³n de integridad
- [OK] 404 cuando disabled (faltas.demo.reset.enabled=false, default)
- [GAP-6] actasDemoDisponibles siempre 0 post-reset (las actas del dataset son declarativas, no instanciadas)

---

## Flujo demo frontend-ready recomendado

```
1. INICIO
   â””â”€â”€ GET /demo/actas/dataset-funcional
       â””â”€â”€ Obtener catÃ¡logo de 31 actas mock: cÃ³digos, tÃ­tulos, estados, bloques, bandejas
       â””â”€â”€ Mostrar resumen de cobertura

2. DEMO DOCUMENTAL
   â””â”€â”€ GET /demo/documentos/graph
       â””â”€â”€ Ejecutar los 8 casos documentales de punta a punta
       â””â”€â”€ Mostrar estado de cada caso: exitoso/fallido, storageKey mock, hash mock

3. RESET (solo si habilitado en entorno demo/dev)
   â””â”€â”€ POST /demo/dev/reset
       â””â”€â”€ Si responde 200: estado limpio, continuar con pasos 1 y 2
       â””â”€â”€ Si responde 404: reset no habilitado; continuar leyendo endpoints de solo lectura
       â””â”€â”€ NUNCA depender del reset para que los endpoints GET funcionen

4. EXPLORACIÃ“N OPERATIVA
   â””â”€â”€ Los endpoints /api/faltas/actas/* estÃ¡n disponibles para flujos reales
       â””â”€â”€ Ver 05-api-core-endpoints.md para detalle
```

### Reglas del flujo

- Los endpoints GET /demo/** funcionan siempre, sin necesidad de reset previo.
- El reset (POST) es opcional y solo disponible en demo/dev con property habilitada.
- El frontend NO debe requerir reset para renderizar datos iniciales.
- Si reset responde 404, el frontend debe continuar normalmente leyendo endpoints GET.

---

## Errores HTTP - contrato actual

| Caso | CÃ³digo | Body |
|------|--------|------|
| Reset deshabilitado | 404 | (cuerpo vacÃ­o) |
| Acta no encontrada (API) | 404 | {"error": "mensaje"} |
| PrecondiciÃ³n violada (API) | 422 | {"error": "mensaje"} |
| ValidaciÃ³n de request (API) | 400 | Spring default (BindingResult) |
| MÃ©todo incorrecto | 405 | Spring default |

**Notas:**
- No existe un `@ControllerAdvice` global. Cada controller operativo tiene sus propios `@ExceptionHandler`.
- El patrÃ³n de error es consistente: `Map<String, String>` con clave "error".
- Los endpoints /demo solo devuelven 200 o 404 (para reset disabled). No hay 422 en demo.
- [GAP-7] No existe un DTO global de error. El patrÃ³n Map<String,String> no estÃ¡ documentado como contrato.
- [GAP-8] No existe endpoint de health/demo-readiness para que el frontend verifique disponibilidad.

---

## CORS

Desde Slice 8F-6 se agregÃ³ `DemoCorsConfig` (WebMvcConfigurer):
- Cubre `/demo/**` y `/api/**`
- MÃ©todos: GET, POST, PUT, DELETE, OPTIONS
- Origins: configurable vÃ­a `faltas.demo.cors.allowed-origins` (default `*`)
- Para producciÃ³n: configurar con la URL real del frontend Angular

---

## Gap list frontend-ready

### Gaps bloqueantes para demo frontend completo

| ID | Gap | Impacto |
|----|-----|---------|
| GAP-3 | No hay `GET /demo/actas/{codigo}` para drill-down de acta individual | Un frontend que necesite mostrar detalle de una acta especÃ­fica no puede navegar a ella desde el dataset |
| GAP-5 | Las actas del dataset son definiciones declarativas, no instancias reales en repositorio | El frontend no puede mostrar la acta "real" correspondiente al cÃ³digo del dataset |
| GAP-8 | No hay endpoint de health/demo-readiness | El frontend no puede verificar si el backend estÃ¡ listo antes de mostrar la UI |

### Gaps no bloqueantes (funciona, pero UX mejorable)

| ID | Gap | Impacto |
|----|-----|---------|
| GAP-1 | accionDocumental y tipoDocu son enums tÃ©cnicos sin label | El frontend debe mapear en TypeScript para mostrar texto amigable |
| GAP-2 | No hay campo label/nombre derivado en DocumentoGraphDemoCasoResultado | El frontend usa descripcionCaso como fallback (disponible) |
| GAP-4 | bloqueEsperado, situacionEsperada, bandejaEsperada son enums sin labels | El frontend debe mapear para mostrar texto amigable |
| GAP-6 | actasDemoDisponibles=0 siempre (actas declarativas, no instanciadas en repo) | El conteo puede ser confuso; la doc debe aclarar que es por diseÃ±o |
| GAP-7 | No hay DTO global de error documentado como contrato | El frontend debe manejar tanto Map<String,String> como Spring defaults |

### Gaps de slice futuro

| ID | Gap | Slice sugerido |
|----|-----|----------------|
| GAP-9 | PaginaciÃ³n en GET /demo/actas/dataset-funcional (31 actas en un solo payload) | 8G o superior |
| GAP-10 | Labels/i18n de enums exportados desde el backend | 8G o superior |
| GAP-11 | Endpoint GET /demo/actas/{codigo} con instancia real + eventos + documentos | 8G o superior |
| GAP-12 | NormalizaciÃ³n de DTO de error global (@ControllerAdvice) | 9.x o superior |
| GAP-13 | CORS de producciÃ³n con origins configurados por ambiente | Etapa 9 / infra |

---

## Decisiones de no alcance

- No se creÃ³ Angular ni ningÃºn frontend dentro del mÃ³dulo backend.
- No se modificÃ³ semÃ¡ntica funcional de los 31 casos.
- No se introdujo JDBC, MariaDB, SQL, JPA ni storage real.
- No se creÃ³ docs/spec-as-source en raÃ­z del repo.
- No se inventaron enums ni eventos fuera del dominio.

---

## Guardrails confirmados

- faltas.demo.reset.enabled: false por defecto.
- POST /demo/dev/reset con reset disabled: 404.
- GET /demo/documentos/graph: siempre activo, 200.
- GET /demo/actas/dataset-funcional: siempre activo, 200.
- Todos los storageKey: mock:// (nunca s3:// ni file://).
- Todos los hashDocu: sha256-mock- (nunca hash real).
- Test-Path docs\spec-as-source: False.


---

## Slice 8F-7: Drill-down real de acta individual

**Slice:** 8F-7
**Fecha:** 2026-07-03

### Endpoint nuevo

| Metodo | Path | Controller | Servicio |
|--------|------|-----------|---------|
| GET | /demo/actas/{codigo} | DatasetFuncionalDemoController | DemoActaMaterializadorService |

### Materializacion real (GAP-5 cerrado)

- No se devuelve una ficha declarativa del catalog.
- Se crea un CasoUsoFuncionalRunner aislado por request.
- Se ejecuta el flujo real del dominio para el codigo dado.
- La instancia resultante (acta, snapshot, eventos, documentos) se proyecta al DTO.
- Idempotente: cada ejecucion produce el mismo estado final para el mismo codigo.

### GAP-3 cerrado

- GET /demo/actas/{codigo} disponible.
- HTTP 200 para codigo existente.
- HTTP 404 para codigo inexistente (sin stacktrace expuesto).
- No se implemento @ControllerAdvice global (GAP-7 sigue no bloqueante).

### GAP-5 cerrado

- El detalle devuelve una instancia real del acta.
- Timeline: eventos append-only reales, ordenados por ordenLogico.
- Documentos: FalDocumento reales del flujo (storageKey y hashDocu mock cuando existen).
- Bloque, situacion y bandeja: del snapshot real, no declarativo.

### Detalle aditivo en dataset-funcional

- GET /demo/actas/dataset-funcional ahora incluye detallePath por cada acta.
- Campo calculado en ActaMockFuncionalDefinicion.detallePath() via @JsonProperty.
- No rompe ningun test previo ni contrato existente.

### Shape del endpoint GET /demo/actas/{codigo}

`json
{
  "codigo": "ACT-001-LABRADA",
  "titulo": "...",
  "descripcion": "...",
  "casoUsoPrincipal": "...",
  "dataset": {
    "bloqueEsperado": "CAPT",
    "situacionEsperada": "ACTIVA",
    "bandejaEsperada": "ACTAS_EN_ENRIQUECIMIENTO",
    "cerrableEsperado": false
  },
  "acta": {
    "actaId": 1,
    "numeroActa": "ACT-MOCK-ACT-001-LABRADA",
    "codigoActa": "uuid-mock-...",
    "bloqueActual": "CAPT",
    "estadoProcesal": "EN_TRAMITE",
    "situacionAdministrativa": "ACTIVA",
    "resultadoFinal": "SIN_RESULTADO_FINAL",
    "bandeja": "ACTAS_EN_ENRIQUECIMIENTO",
    "cerrable": false
  },
  "timeline": [
    {
      "orden": 1,
      "eventoId": "...",
      "tipoEvento": "ACTLAB",
      "descripcion": null,
      "fhEvento": "2024-03-15T10:30:00"
    }
  ],
  "documentos": [],
  "demo": {
    "mock": true,
    "materializada": true,
    "source": "DATASET_FUNCIONAL",
    "warnings": []
  },
  "links": {
    "self": "/demo/actas/ACT-001-LABRADA",
    "dataset": "/demo/actas/dataset-funcional",
    "graph": "/demo/documentos/graph"
  }
}
`

### Gaps actualizados

| ID | Estado | Nota |
|----|--------|------|
| GAP-3 | CERRADO | GET /demo/actas/{codigo} implementado |
| GAP-5 | CERRADO | Instancia real via CasoUsoFuncionalRunner |
| GAP-7 | NO BLOQUEANTE | Error global sin @ControllerAdvice (Spring defaults) |
| GAP-8 | PENDIENTE 8F-8 | Health/demo-readiness no implementado |

### Tests agregados

- DemoActaDetalleContractTest.java: 35 tests, 0 failures.
- Cubre los 15 puntos de cobertura requeridos mas casos representativos adicionales.
- Total suite: 1486 tests (antes: 1451).

### Guardrails 8F-7

- No JDBC, no MariaDB, no SQL, no JPA, no storage real.
- storageKey: nunca file:// ni s3://.
- hashDocu: mock cuando existe.
- No se creo docs/spec-as-source en raiz del repo.
- faltas.demo.reset.enabled: false por defecto.
- No se inventaron TipoEventoActa ni BloqueActual nuevos.

---

## Slice 8F-8 — GET /demo/health (2026-07-03)

### Objetivo

Cierre de GAP-8. Endpoint de demo-readiness para que el frontend Angular verifique disponibilidad del backend antes de cargar la UI de navegacion.

### Endpoint

`
GET /demo/health
HTTP 200 — siempre que el modulo este iniciado
`

### Shape de respuesta (DemoHealthResponse)

`json
{
  "status": "UP",
  "demoReady": true,
  "fhEjecucion": "2026-07-03T12:42:...",
  "versionDemo": "8F-8",
  "dataset": {
    "ready": true,
    "totalActasMock": 31,
    "coberturaCompleta": true,
    "detalleDisponible": true
  },
  "documentos": {
    "ready": true,
    "totalPlantillasMock": 8,
    "graphDisponible": true,
    "storageReal": false
  },
  "reset": {
    "endpoint": "/demo/dev/reset",
    "enabled": false,
    "defaultSeguro": true
  },
  "endpoints": [
    { "method": "GET", "path": "/demo/documentos/graph", "ready": true, "descripcion": "..." },
    { "method": "GET", "path": "/demo/actas/dataset-funcional", "ready": true, "descripcion": "..." },
    { "method": "GET", "path": "/demo/actas/{codigo}", "ready": true, "descripcion": "..." },
    { "method": "POST", "path": "/demo/dev/reset", "ready": true, "descripcion": "..." },
    { "method": "GET", "path": "/demo/health", "ready": true, "descripcion": "..." }
  ],
  "warnings": []
}
`

### Uso en frontend Angular demo

1. Al iniciar la SPA, llamar GET /demo/health.
2. Si demoReady=true: habilitar navegacion completa (dataset → detalle → documentos).
3. Si demoReady=false: mostrar pantalla de error/espera.
4. Si warnings no vacio: mostrar banner informativo (no bloquea la demo).
5. Si eset.enabled=false: no mostrar boton de reset en la UI (es el caso default).

### Flujo navegacion completa demo

`
GET /demo/health
  → demoReady=true → habilitar UI
    → GET /demo/actas/dataset-funcional  (31 actas + detallePath)
      → GET /demo/actas/{codigo}         (drill-down real: timeline + documentos)
    → GET /demo/documentos/graph         (graph documental completo)
`

### Lo que NO valida todavia

- Base de datos real (MariaDB/Informix).
- Storage real (S3, filesystem).
- Generacion de PDF real.
- Autenticacion productiva.
- Integraciones externas.

### Checks realizados internamente

- Dataset: DatasetFuncionalDominioCatalog.calcularCobertura().
- Plantillas: DocumentoPlantillaRepository.listar().size().
- Reset: @Value("").
- Endpoints: lista estatica declarativa.

Sin llamadas HTTP contra si mismo. Sin efectos destructivos.

### Guardrails confirmados (8F-8)

- storageReal=false siempre.
- esetEnabled=false por defecto.
- No expone s3:// ni ile://.
- No ejecuta reset.
- No genera documentos.
- No usa JDBC/MariaDB/JPA.

### Tests nuevos

DemoHealthContractTest — 16 tests, 0 failures.

Build final: **1502 tests, BUILD SUCCESS**.

### GAP-8 CERRADO