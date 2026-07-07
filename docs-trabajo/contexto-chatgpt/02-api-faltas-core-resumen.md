# 02 — api-faltas-core: Resumen de arquitectura

**Módulo:** `backend/api-faltas-core`
**Generado:** 2026-07-03

---

## Tipo de módulo

Prototipo de validación funcional in-memory. Spring Boot moderno sin XML, sin ORM, sin base de datos real.
No usar MariaDB/JDBC todavía. No usar JPA/Hibernate.

---

## Paquetes principales

```
ar.gob.malvinas.faltas.core
├── domain/
│   ├── model/          → entidades del dominio (FalActa, FalDocumento, FalFallo, etc.)
│   ├── enums/          → enums de dominio (BloqueActual, TipoEventoActa, CodigoBandeja, etc.)
│   └── exception/      → excepciones de dominio (PrecondicionVioladaException, etc.)
├── application/
│   ├── demo/           → GraphDemoActaFactory, DatasetFuncionalDominioCatalog, DevInMemoryResetService, etc.
│   ├── service/        → servicios de dominio (ActaService, FalloActaService, PagoCondenaService, etc.)
│   ├── command/        → commands de entrada (LabrarActaCommand, DictarFalloCommand, etc.)
│   └── result/         → resultados de servicio
├── repository/
│   ├── (interfaces)    → ActaRepository, DocumentoRepository, etc.
│   └── memory/         → InMemoryActaRepository, InMemoryDocumentoRepository, etc.
└── web/
    ├── (controllers)   → ActaApiController, DocumentoApiController, DevResetController, etc.
    └── dto/            → DTOs de request/response
```

---

## Servicios principales

| Servicio | Responsabilidad |
|----------|-----------------|
| `ActaService` | Labrar, completar captura, enriquecer, cerrar acta |
| `FalloActaService` | Dictar fallo absolutorio / condenatorio |
| `NotificacionService` | Enviar notificación, registrar resultado positivo/negativo |
| `PagoVoluntarioService` | Solicitar, fijar monto, informar, confirmar, observar pago voluntario |
| `PagoCondenaService` | Informar, confirmar, observar pago de condena |
| `FirmezaCondenaService` | Declarar condena firme (PLAVNC + CONFIR) |
| `ApelacionActaService` | Registrar apelación (APEPRE), resolver (APERAZ / APEABS) |
| `GestionExternaService` | Derivar, reingresar, registrar pago externo |
| `BloqueanteMaterialService` | Registrar, cumplir, anular bloqueante material |
| `DocumentoService` | Generar, firmar, incorporar adjunto, convalidar |
| `DocumentoRedaccionService` | Crear redacción con contexto de acta (BORRADOR → CONFIRMADA) |
| `DocumentoCombinacionService` | Combinar plantilla + contexto → texto final |
| `DocumentoGeneracionMockService` | Confirmar redacción y generar PDF mock |
| `DocumentoGraphDemoService` | Graph demo documental (8 casos de punta a punta) |
| `DevInMemoryResetService` | Reset completo in-memory + resembrado plantillas (8F-5) |

---

## Repositorios InMemory activos

Todos los repositorios son in-memory. Implementan `ResettableInMemoryRepository` (desde 8F-5).

- `InMemoryActaRepository`
- `InMemoryActaEventoRepository`
- `InMemoryActaSnapshotRepository`
- `InMemoryActaEvidenciaRepository`
- `InMemoryDocumentoRepository`
- `InMemoryDocumentoFirmaRepository`
- `InMemoryDocumentoFirmaReqRepository`
- `InMemoryDocumentoPlantillaRepository`
- `InMemoryDocumentoPlantillaDefaultRepository`
- `InMemoryDocumentoPlantillaContenidoRepository`
- `InMemoryDocumentoRedaccionRepository`
- `InMemoryFalloActaRepository`
- `InMemoryApelacionActaRepository`
- `InMemoryFirmezaCondenaRepository`
- `InMemoryPagoVoluntarioRepository`
- `InMemoryPagoCondenaRepository`
- `InMemoryGestionExternaRepository`
- `InMemoryBloqueanteMaterialRepository`
- `InMemoryFirmanteRepository`
- `InMemoryNotificacionRepository`
- `InMemoryDependenciaRepository`
- `InMemoryNormativaRepository`
- `InMemoryTalonarioRepository`
- `InMemoryInspectorRepository`

---

## Endpoints principales

### API de dominio

| Método | Path | Descripción |
|--------|------|-------------|
| POST | `/api/faltas/actas/labrar` | Labrar nueva acta |
| POST | `/api/faltas/actas/{id}/completar-captura` | Completar captura |
| POST | `/api/faltas/actas/{id}/fallo/absolutorio` | Dictar fallo absolutorio |
| POST | `/api/faltas/actas/{id}/fallo/condenatorio` | Dictar fallo condenatorio |
| POST | `/api/faltas/actas/{id}/notificaciones/enviar` | Enviar notificación |
| POST | `/api/faltas/actas/{id}/firmeza/declarar` | Declarar condena firme |
| POST | `/api/faltas/actas/{id}/pago-voluntario/solicitar` | Solicitar pago voluntario |
| POST | `/api/faltas/actas/{id}/pago-condena/informar` | Informar pago de condena |
| POST | `/api/faltas/actas/{id}/gestion-externa/derivar` | Derivar a gestión externa |
| POST | `/api/faltas/actas/{id}/gestion-externa/reingresar` | Reingresar desde gestión externa |
| POST | `/api/faltas/bloqueantes/registrar` | Registrar bloqueante material |

### Endpoints demo / dev

| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/demo/documentos/graph` | Graph demo documental (8 flujos completos) |
| GET | `/demo/actas/dataset-funcional` | Dataset funcional completo (31 actas mock) |
| POST | `/demo/dev/reset` | Reset in-memory (solo si `faltas.demo.reset.enabled=true`) |

---

## Motor documental

El motor documental usa el siguiente flujo:

```
PlantillasMockSeeder → PlantillasDisponibles
DocumentoVariableContextBuilder → contexto (namespace ACTA, INSPECTOR, INFRACTOR, etc.)
DocumentoCombinacionService → texto combinado
DocumentoRedaccionService → FalDocumentoRedaccion (BORRADOR)
DocumentoGeneracionMockService → confirmar + PDF mock (CONFIRMADA)
DocumentoPdfMockRenderer → storageKey mock://, hashDocu sha256-mock-
```

---

## Documentación viva

`backend/api-faltas-core/docs/spec-as-source/`

| Archivo | Contenido |
|---------|-----------|
| `02-estados-bloques-eventos.md` | Estados, bloques y eventos del dominio |
| `03-comandos-precondiciones-efectos.md` | Comandos y precondiciones |
| `04-snapshot-bandejas-acciones.md` | Snapshot, bandejas y acciones pendientes |
| `05-api-core-endpoints.md` | Endpoints de la API |
| `06-tests-core.md` | Estrategia de tests |
| `99-pendientes-siguientes-slices.md` | Historial de slices y próximos pasos |
| `100-etapa-8-plan-maestro-api-multi-app.md` | Plan etapa 8F |
| `104-plantillas-redaccion-combinacion-documentos.md` | Motor documental |

---

## Advertencia

**No avanzar con JDBC/MariaDB todavía.**
El módulo es in-memory puro. La infraestructura JDBC existe bajo perfil `jdbc` pero ningún repositorio de dominio la usa aún.
Próximo paso JDBC: después de cerrar 8F-6 y posteriores slices funcionales.
