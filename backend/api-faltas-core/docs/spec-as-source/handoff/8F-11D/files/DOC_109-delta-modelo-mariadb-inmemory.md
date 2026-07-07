> **[8F-11A — 2026-07-04]** Este documento es historial de auditoría (slices 8F-9 / 8F-10).
> La fuente vigente de paridad es: **`110-matriz-maestra-paridad-mariadb-inmemory.md`**
> No actualizar este documento; actualizar el 110.
>
> **[8F-11A-R1 — 2026-07-05]** Corrección documental aplicada al 110: 62 tablas (no 55), 29 FALTA_EN_INMEMORY (no 22), P1 cerrada (`resultado_final SMALLINT NOT NULL` 0–4), P2 cerrada (firmeza en fallo, refactor en 8F-11F), catálogos incorporados. La entrada de `ResultadoFinalActa` en la sección 8 de este documento es histórica; la definición vigente es la del 110.
---
﻿﻿﻿﻿﻿﻿# 109 — Auditoría DELTA: Modelo MariaDB vs Modelo InMemory

**Slice:** 8F-9 / Revisión 8F-9-R2 (base) → **Actualizado en 8F-10 (2026-07-04): tres tablas nuevas incorporadas al modelo MariaDB, decisiones D1+D5 aplicadas**
**Fecha base:** 2026-07-03 | **ltima actualización:** 2026-07-04 (Slice 8F-10)
**Tipo:** Auditoría y documentación. Sin cambios funcionales Java.
**Módulo:** backend/api-faltas-core (in-memory, sin MariaDB)
**Build base:** 1502 tests passing. BUILD SUCCESS. (al cierre de 8F-8)

---

## 1. Resumen ejecutivo

El modelo InMemory actual es **funcionalmente avanzado** pero **parcialmente desalineado** con el modelo MariaDB documentado.

Situación general:

| Categoría | Cantidad aproximada |
|-----------|-------------------|
| Entidades InMemory alineadas con MariaDB | ~20 |
| Entidades InMemory con gaps o deudas activas | ~8 |
| Entidades InMemory sin equivalente MariaDB directo | ~5 |
| Tablas MariaDB sin implementación InMemory | ~25 |
| Elementos solo demo/test/infra | ~20 |
| Entidades nuevas en 8F que requieren tabla MariaDB | 3 |
| Decisiones de Pablo aprobadas y cerradas (8F-9-R1) | 9 |

El bloque 8C-6E (auditoría previa a Slice 9) documentó exhaustivamente el modelo documental hasta
ese momento. Este documento extiende esa auditoría incorporando todo lo agregado en 8F y haciendo un
inventario completo del delta incluyendo las áreas aún no auditadas (pagos, fallo/apelación, persona).

---

## 2. Alcance y guardrails

### Alcance de este documento

- Auditoría del modelo InMemory actual vs modelo MariaDB documentado.
- Identificación de entidades, campos, enums y conceptos nuevos de 8F.
- Propuesta de tipos de dato MariaDB para revisión de Pablo.
- Separación clara entre dominio persistible y soporte demo/test/infra.

### Fuentes consultadas

1. `docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`
2. `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`
3. `backend/api-faltas-core/docs/spec-as-source/101-auditoria-pre-jdbc-mariadb.md`
4. Todo el código Java de `backend/api-faltas-core/src/main/java/`

### Guardrails estrictos de este slice

NO se implementa:
- JDBC / MariaDB real / JPA / Hibernate / `@Entity` / `JpaRepository`
- Scripts SQL ejecutables / Flyway / Liquibase
- Frontend Angular / PDF real / storage real
- Endpoints productivos nuevos

---

## 3. Estado actual del backend in-memory

| Indicador | Valor |
|-----------|-------|
| Build | BUILD SUCCESS |
| Tests | 1502 passing, 0 failures, 0 errors |
| Persistencia | In-memory (sin MariaDB) |
| Angular/frontend | No tocado |
| `docs/spec-as-source` en raíz | NO EXISTE (guardrail OK) |
| Slices cerrados | 1 al 8F-8 inclusive |

---

## 4. Inventario del modelo MariaDB documentado

### Sección 1 — Dependencias, inspectores, firmantes

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_dependencia` + `fal_dependencia_version` | Unidades administrativas versionadas | COMPLETO |
| `fal_inspector` + `fal_inspector_version` | Inspectores/agentes versionados | COMPLETO |
| `fal_firmante` + `fal_firmante_version` + `fal_firmante_version_habilitacion` | Firmantes habilitados para firma documental | COMPLETO (agregado en 8A-3D/8A-3D.1) |

### Sección 2 — Núcleo expediente

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_acta` | Entidad central del expediente | COMPLETO |
| `fal_persona` | Maestro de personas | PENDIENTE — no implementado in-memory (datos embedidos en FalActa) |
| `fal_persona_domicilio` | Domicilios de personas | PENDIENTE — no implementado in-memory |
| `fal_acta_evento` | Log append-only de eventos | COMPLETO |
| `fal_acta_snapshot` | Proyección operativa derivada | COMPLETO |
| `fal_acta_evidencia` | Evidencias del acta | COMPLETO |
| `fal_observacion` | Observaciones tipificadas por entidad | PENDIENTE — no implementado in-memory (PAGAPR usa evento transitoriamente) |

### Sección 3 — Normativa y valorización

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_dependencia_normativa` + `fal_normativa_faltas` + `fal_articulo_normativa_faltas` | Normativas | COMPLETO |
| `fal_tarifario_unidad_faltas` | Valores unitarios por artículo | PENDIENTE — no implementado in-memory |
| `fal_medida_preventiva` + `fal_articulo_medida_preventiva` | Medidas preventivas | PENDIENTE — no implementado in-memory |
| `fal_acta_articulo_infringido` | Artículos infringidos por acta concreta | PENDIENTE — no implementado in-memory |
| `fal_acta_valorizacion` + `fal_acta_valorizacion_item` | Valorización del acta | PENDIENTE — no implementado in-memory |

### Sección 4 — Satélites del acta

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_acta_transito` + `fal_acta_transito_alcoholemia` + `fal_acta_vehiculo` | Datos de tránsito | PENDIENTE — no implementado in-memory |
| `fal_acta_contravencion` + `fal_acta_sustancias_alimenticias` | Otros tipos de acta | PENDIENTE — no implementado in-memory |
| `fal_acta_medida_preventiva` | Medidas preventivas aplicadas al acta | PENDIENTE — no implementado in-memory |
| `fal_acta_bloqueante_cierre_material` | Bloqueantes de cierre | COMPLETO (id UUID String → evaluar Long para JDBC) |

### Sección 5 — Documentos y firma

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_documento` | Documento concreto del expediente | COMPLETO (auditado en 8C-6E) |
| `fal_acta_documento` | Relación acta-documento | PENDIENTE — no implementado in-memory |
| `fal_documento_firma` + `fal_documento_firma_req` | Firmas y requisitos | COMPLETO (8C-6B-1, 8C-6E) |
| `fal_documento_plantilla` + `fal_documento_plantilla_firma_req` | Plantillas base | COMPLETO |
| **`fal_documento_plantilla_contenido`** | Contenido versionado de plantilla | **FALTA EN MARIADB** — nueva entidad 8F-1 |
| **`fal_documento_plantilla_default`** | Resolución automática de plantilla por acción | **FALTA EN MARIADB** — nueva entidad 8F-1 |
| **`fal_documento_redaccion`** | Redacción editable previa al PDF | **FALTA EN MARIADB** — nueva entidad 8F-1 |

### Sección 6 — Notificaciones

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_notificacion` | Notificación asociada a un acta | PARCIAL (id UUID String, sin intento/acuse/lote) |
| `fal_notificacion_intento` + `fal_notificacion_acuse` + `fal_lote_correo` | Ciclo de notificación | PENDIENTE — no implementado in-memory |

### Sección 7 — Fallo, apelación, paralización, archivo, gestión externa

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_acta_fallo` | Fallo dictado sobre el acta | PARCIAL (id UUID String; faltan varios campos — ver sección 10) |
| `fal_acta_apelacion` + `fal_acta_apelacion_documento` | Apelación y documentos presentados | PARCIAL (id UUID String; campos ausentes — ver sección 10) |
| `fal_acta_paralizacion` | Ciclos de paralización con trazabilidad completa | SOLO_PARCIAL — InMemory solo maneja estado via FalActa + evento; sin entidad FalActaParalizacion |
| `fal_acta_archivo` | Ciclos de archivo y reingreso | FALTA_EN_INMEMORY — no hay entidad FalActaArchivo |
| `fal_acta_gestion_externa` | Ciclos de gestión externa | COMPLETO (id UUID String → evaluar Long) |

### Sección 8 — Pagos

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_acta_obligacion_pago` + `fal_acta_forma_pago` + `fal_acta_plan_pago_ref` + `fal_acta_pago_movimiento` | Modelo completo de pagos | FALTA_EN_INMEMORY — InMemory usa FalPagoVoluntario y FalPagoCondena como simplificación |

### Sección 9 — Talonarios y numeración

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `num_politica` + `num_talonario` + `num_talonario_ambito` + `num_talonario_inspector` + `num_talonario_movimiento` | Numeración completa | COMPLETO (deuda DDL fh_alta/id_user_alta en num_politica) |

### Sección 11 — Portal y QR

| Tabla | Propósito | Estado |
|-------|-----------|--------|
| `fal_acta_qr_acceso` | Acceso al portal por QR | DIFERIBLE — no implementado in-memory |

---

## 5. Inventario del modelo InMemory actual

### A. Modelo de dominio persistible

| Clase Java | Tabla MariaDB candidata | Estado delta |
|---|---|---|
| `FalActa` | `fal_acta` | ALINEADO (con deudas activas pre-JDBC) |
| `FalActaEvento` | `fal_acta_evento` | ALINEADO |
| `FalActaSnapshot` | `fal_acta_snapshot` | ALINEADO |
| `FalActaEvidencia` | `fal_acta_evidencia` | ALINEADO |
| `FalActaFallo` | `fal_acta_fallo` | PARCIAL — id String, faltan campos (ver sección 10) |
| `FalActaApelacion` | `fal_acta_apelacion` | PARCIAL — id String, faltan campos |
| `FalActaFirmezaCondena` | Sin tabla directa - firmeza en `fal_acta_fallo` + `origen_firmeza SMALLINT NULL` | APROBADA_D1 |
| `FalBloqueanteMaterial` | `fal_acta_bloqueante_cierre_material` | OK — id UUID String → evaluar Long |
| `FalDependencia` + `FalDependenciaVersion` | `fal_dependencia` + `fal_dependencia_version` | ALINEADO |
| `FalDocumento` | `fal_documento` | ALINEADO (auditado 8C-6E) |
| `FalDocumentoFirma` | `fal_documento_firma` | ALINEADO (refactor 8C-6B-1) |
| `FalDocumentoFirmaReq` | `fal_documento_firma_req` | ALINEADO |
| `FalDocumentoPlantilla` | `fal_documento_plantilla` | ALINEADO |
| `FalDocumentoPlantillaFirmaReq` | `fal_documento_plantilla_firma_req` | ALINEADO |
| **`FalDocumentoPlantillaContenido`** | **`fal_documento_plantilla_contenido`** (sección 5.11) | **INCORPORADA_8F-10** |
| **`FalDocumentoPlantillaDefault`** | **`fal_documento_plantilla_default`** (sección 5.12) | **INCORPORADA_8F-10** |
| **`FalDocumentoRedaccion`** | **`fal_documento_redaccion`** (sección 5.13) | **INCORPORADA_8F-10** |
| `FalFirmante` + `FalFirmanteVersion` + `FalFirmanteVersionHabilitacion` | `fal_firmante` + `fal_firmante_version` + `fal_firmante_version_habilitacion` | ALINEADO (8A-3D/8A-3D.1) |
| `FalGestionExterna` | `fal_acta_gestion_externa` | OK — id UUID String → evaluar Long |
| `FalInspector` + `FalInspectorVersion` | `fal_inspector` + `fal_inspector_version` | ALINEADO |
| `FalNormativaFaltas` + `FalArticuloNormativaFaltas` + `FalDependenciaNormativa` | tablas normativa | ALINEADO |
| `FalNotificacion` | `fal_notificacion` | PARCIAL — id UUID String; sin notificacion_intento/acuse/lote |
| `FalPagoVoluntario` | `fal_acta_obligacion_pago` con tipo PAGO_VOLUNTARIO | APROBADA_D2 |
| `FalPagoCondena` | `fal_acta_obligacion_pago` con tipo CONDENA | APROBADA_D2 |
| `NumPolitica` + `NumTalonario` + `NumTalonarioAmbito` + `NumTalonarioInspector` + `NumTalonarioMovimiento` | tablas num_* | ALINEADO |

### B. Proyecciones / snapshots

| Clase Java | Descripción | Acción |
|---|---|---|
| `FalActaSnapshot` | Proyección operativa derivada de eventos | PERSISTIBLE — upsert por id_acta |

### C. DTOs web / demo

| Clase Java | Categoría | Acción |
|---|---|---|
| `DemoActaDetalleResponse` + sub-DTOs (8F-7) | Demo-only | NO_PERSISTIBLE |
| `DemoHealthResponse` + sub-DTOs (8F-8) | Demo-only | NO_PERSISTIBLE |
| `DevResetResponse` (8F-5) | Demo/dev-only | NO_PERSISTIBLE |
| `DemoTimelineEventoDto` (8F-7) | Demo-only | NO_PERSISTIBLE |
| `DemoDocumentoDetalleDto` (8F-7) | Demo-only | NO_PERSISTIBLE |
| Demás `*Request` / `*Response` de `/api/faltas` | DTOs operativos | NO_PERSISTIBLE como tabla |

### D. Soporte técnico in-memory

| Clase Java | Descripción | Acción |
|---|---|---|
| Todos los `InMemory*Repository` | Implementaciones en memoria de interfaces | REEMPLAZAR por JDBC en Slice 9 |
| `ResettableInMemoryRepository` (8F-5) | Interfaz para repos reseteables | SOLO_INFRA_INMEMORY — no persistir |

### E. Soporte de test/demo/mock

| Clase Java | Descripción | Acción |
|---|---|---|
| `DatasetFuncionalDominioCatalog` | Catálogo declarativo 31 actas mock | SOLO_DEMO_TEST |
| `ActaMockFuncionalDefinicion` | Definición de un caso de uso mock | SOLO_DEMO_TEST |
| `DocumentoEsperadoPorActaMock` | Documento esperado para un caso mock | SOLO_DEMO_TEST |
| `CasoUsoFuncionalRunner` | Ejecutor de casos de uso sobre repos frescos | SOLO_DEMO_TEST |
| `PlantillasMockSeeder` | Sembrador de plantillas mock para demo | SOLO_DEMO |
| `GraphDemoActaFactory` | Factory de acta para graph demo | SOLO_DEMO |
| `DemoActaMaterializadorService` (8F-7) | Materializa acta por código demo | SOLO_DEMO |
| `DemoHealthService` (8F-8) | Calcula demo-readiness | SOLO_DEMO |
| `DevInMemoryResetService` (8F-5) | Resetea todos los repos in-memory | SOLO_DEMO_DEV |
| `DocumentoRenderizadoMock` | Resultado de rendering mock | SOLO_DEMO |
| `DocumentoGraphDemoResultado` + sub | Resultado de graph demo | SOLO_DEMO |
| `DatasetFuncionalCoberturaResultado` + sub | Resultado de cobertura dataset | SOLO_DEMO |

### F. Infraestructura dev-only

| Clase Java | Descripción | Acción |
|---|---|---|
| `DemoCorsConfig` (8F-6) | CORS abierto para demo/dev | SOLO_INFRA_DEMO — reemplazar por config productiva en Etapa 9 |

### G. Servicios de aplicación con doble rol

| Clase Java | Descripción | Clasificación |
|---|---|---|
| `DocumentoCombinacionService` | Motor de combinación de variables en template | Servicio de dominio real — no tabla |
| `DocumentoVariableContextBuilder` | Construye contexto de variables desde FalActa | Servicio de dominio real — no tabla |
| `DocumentoVariableRegistry` | Registro de variables permitidas | Singleton Java — no tabla |
| `DocumentoPlantillaDefaultService` | Resuelve plantilla default operativa | Servicio de dominio real |
| `DocumentoRedaccionService` | Crea y gestiona redacciones documentales | Servicio de dominio real |
| `DocumentoGeneracionMockService` + `DocumentoPdfMockRenderer` | Generación PDF simulada | DEMO_MOCK — reemplazar por real |
| `DocumentoGraphDemoService` | Graph demo documental | SOLO_DEMO |

---

## 6. Matriz DELTA MariaDB vs InMemory

### Área: Núcleo de acta y eventos

| Concepto InMemory | Existe en MariaDB | Tabla/campo MariaDB | Estado | Acción recomendada |
|---|---|---|---|---|
| `FalActa.id` (Long) | Sí | `fal_acta.id BIGINT AUTO_INCREMENT` | OK_ALINEADO | — |
| `FalActa.nroActa` (String) | Sí | `fal_acta.nro_acta VARCHAR(20)` | OK_ALINEADO | — |
| `FalActa.tipoActa` (String) | Sí | `fal_acta.tipo_acta SMALLINT` | TIPO_DATO_A_REVISAR | Convertir a `TipoActa` enum con código SMALLINT |
| `FalActa.resultadoFirmaInfractor` | Sí | `fal_acta.resultado_firma_infractor SMALLINT NOT NULL` | OK_ALINEADO (deuda DDL activa) | Agregar columna DDL |
| `FalActa.idTalonario` / `nroTalonarioUsado` | Sí | `fal_acta.id_talonario BIGINT / nro_talonario_usado INT` | OK_ALINEADO | — |
| `FalActaEvento` | Sí | `fal_acta_evento` | OK_ALINEADO | — |
| `FalActaSnapshot` | Sí | `fal_acta_snapshot` | OK_ALINEADO | — |
| `FalActaEvidencia` | Sí | `fal_acta_evidencia` | OK_ALINEADO | — |
| `fal_persona` / `fal_persona_domicilio` | Sí en MariaDB | Sin entidad InMemory; datos embedidos en FalActa | FALTA_EN_INMEMORY | Diseñar separación en Slice 9 |

### Área: Documentos, plantillas y firma

| Concepto InMemory | Existe en MariaDB | Estado | Acción |
|---|---|---|---|
| `FalDocumento` / `FalDocumentoFirma` / `FalDocumentoFirmaReq` | Sí | OK_ALINEADO | — |
| `FalDocumentoPlantilla` + `FalDocumentoPlantillaFirmaReq` | Sí | OK_ALINEADO | — |
| `FalDocumentoPlantillaContenido` | **NO** | **FALTA_EN_MARIADB** | Crear `fal_documento_plantilla_contenido` |
| `FalDocumentoPlantillaDefault` | **NO** | **FALTA_EN_MARIADB** | Crear `fal_documento_plantilla_default` |
| `FalDocumentoRedaccion` | **NO** | **FALTA_EN_MARIADB** | Crear `fal_documento_redaccion` |
| `fal_acta_documento` (relación) | Sí en MariaDB | FALTA_EN_INMEMORY | Diseñar en Slice 9 |
| `DocumentoVariableDefinicion` (record) | No | DTO_NO_PERSISTIBLE | Puro Java |

### Área: Firmantes y firma

| Concepto InMemory | Existe en MariaDB | Estado |
|---|---|---|
| `FalFirmante` + `FalFirmanteVersion` + `FalFirmanteVersionHabilitacion` | Sí (1.7, 1.8, 1.9) | OK_ALINEADO |


### Área: Notificaciones

| Concepto InMemory | Existe en MariaDB | Estado | Acción |
|---|---|---|---|
| `FalNotificacion` | Sí | PARCIAL — id UUID String | Evaluar BIGINT vs UUID para JDBC |
| `fal_notificacion_intento` + `fal_notificacion_acuse` + `fal_lote_correo` | Sí | FALTA_EN_INMEMORY | Implementar en slice posterior |

### Área: Pagos

| Concepto InMemory | Existe en MariaDB | Estado | Acción |
|---|---|---|---|
| `FalPagoVoluntario` | `fal_acta_obligacion_pago` con tipo PAGO_VOLUNTARIO | APROBADA_D2 |
| `FalPagoCondena` | `fal_acta_obligacion_pago` con tipo CONDENA | APROBADA_D2 |
| `fal_acta_obligacion_pago` + `fal_acta_forma_pago` + `fal_acta_plan_pago_ref` + `fal_acta_pago_movimiento` | Sí en MariaDB | FALTA_EN_INMEMORY | Diferir al slice de pagos JDBC |

### Área: Fallo, apelación y firmeza

| Concepto InMemory | Existe en MariaDB | Estado | Notas |
|---|---|---|---|
| `FalActaFallo.id` (String UUID) | `fal_acta_fallo.id BIGINT` | TIPO_DATO_A_REVISAR | Migrar a Long antes de JDBC |
| `FalActaFallo.fundamentos` (String libre) | No directamente | SEMANTICA_A_REVISAR | Posiblemente FK a `fal_observacion` |
| Campos ausentes en `FalActaFallo` | `valorizacion_id`, `resultado_fallo`, `fh_firma`, `fh_vto_apelacion`, `fallo_reemplazado_id` | FALTA_EN_INMEMORY | Agregar antes de JDBC |
| `FalActaApelacion.id` (String UUID) | `fal_acta_apelacion.id BIGINT` | TIPO_DATO_A_REVISAR | Migrar a Long |
| Campos ausentes en `FalActaApelacion` | `fallo_id`, `canal_apelacion`, `tipo_presentacion`, `texto_apelacion`, `documento_resolucion_id` | FALTA_EN_INMEMORY | Agregar antes de JDBC |
| `FalActaFirmezaCondena` | Firmeza en `fal_acta_fallo.fh_firmeza` + `si_firme` + nuevo `origen_firmeza SMALLINT NULL` | **APROBADA_D1** - sin tabla separada |

### Área: Gestión externa, paralización, archivo

| Concepto InMemory | Existe en MariaDB | Estado | Notas |
|---|---|---|---|
| `FalGestionExterna.id` (String UUID) | `fal_acta_gestion_externa.id BIGINT` | TIPO_DATO_A_REVISAR | Migrar a Long |
| `FalGestionExterna` campos | Alineados (ver DELTA 6A–6D-2) | OK_ALINEADO | — |
| Paralización via FalActa.situacionAdministrativa | `fal_acta_paralizacion` (tabla independiente) | SEMANTICA_A_REVISAR | InMemory sin FalActaParalizacion; modelo MariaDB tiene motivo + trazabilidad completa |
| Archivo de acta | `fal_acta_archivo` | FALTA_EN_INMEMORY | No hay FalActaArchivo in-memory |

### Área: Bloqueantes

| Concepto InMemory | Existe en MariaDB | Estado |
|---|---|---|
| `FalBloqueanteMaterial.id` (String UUID) | `fal_acta_bloqueante_cierre_material.id BIGINT` | TIPO_DATO_A_REVISAR — migrar a Long |
| `FalBloqueanteMaterial` resto de campos | Sí | OK_ALINEADO |

### Área: Talonarios y numeración

| Concepto InMemory | Existe en MariaDB | Estado |
|---|---|---|
| `NumPolitica` + `NumTalonario` + `NumTalonarioAmbito` + `NumTalonarioInspector` + `NumTalonarioMovimiento` | Sí — secciones 9.2–9.6 | OK_ALINEADO (deuda DDL fh_alta/id_user_alta en num_politica) |

---

## 7. Entidades y conceptos nuevos incorporados en 8F

### 7.1 FalDocumentoPlantillaContenido (8F-1)

**Tabla candidata:** `fal_documento_plantilla_contenido`
**Clasificación:** Dominio real, persistible

| Campo Java | Tipo Java | Tipo MariaDB propuesto | Null | Notas |
|---|---|---|---|---|
| `id` | Long | BIGINT AUTO_INCREMENT | NO | PK técnica |
| `plantillaId` | Long | BIGINT FK → fal_documento_plantilla.id | NO | — |
| `versionContenido` | short | SMALLINT | NO | Versión ordinal |
| ~~`formato`~~ | ~~FormatoPlantillaContenido~~ | ~~SMALLINT~~ | ~~NO~~ | **ELIMINADO EN 8F-10-R1** |
| `titulo` | String | VARCHAR(200) | NO | — |
| `cuerpoTemplate` | String | TEXT | NO | Template con variables `{{namespace.campo}}` |
| `encabezadoTemplate` | String | TEXT | SÍ | — |
| `pieTemplate` | String | TEXT | SÍ | — |
| `variablesDeclaradasJson` | String | **JSON** | SÍ | APROBADA_D4 - JSON nativo MariaDB 12.3.2 |
| `siActivo` | boolean | BOOLEAN | NO | — |
| `fhVigDesde` | LocalDateTime | DATETIME(6) | NO | — |
| `fhVigHasta` | LocalDateTime | DATETIME(6) | SÍ | — |
| `fhAlta` | LocalDateTime | DATETIME(6) | NO | Auditoría |
| `idUserAlta` | String | CHAR(36) | NO | Auditoría |

### 7.2 FalDocumentoPlantillaDefault (8F-1)

**Tabla candidata:** `fal_documento_plantilla_default`
**Clasificación:** Dominio real, persistible
**Índice propuesto:** (accion_documental, tipo_acta, id_dependencia)

| Campo Java | Tipo Java | Tipo MariaDB propuesto | Null | Notas |
|---|---|---|---|---|
| `id` | Long | BIGINT AUTO_INCREMENT | NO | PK técnica |
| `accionDocumental` | AccionDocumental | SMALLINT | NO | — |
| `tipoActa` | TipoActa | SMALLINT | SÍ | NULL = genérico |
| `tipoDocu` | TipoDocu | SMALLINT | NO | — |
| `idDependencia` | Long | BIGINT FK | SÍ | NULL = genérico |
| `verDependencia` | Short | SMALLINT | SÍ | FK condicional |
| `plantillaId` | Long | BIGINT FK → fal_documento_plantilla.id | NO | — |
| `prioridad` | int | **SMALLINT** | NO | APROBADA_D9 - rango 0..32767 |
| `fhVigDesde` | LocalDateTime | DATETIME(6) | NO | — |
| `fhVigHasta` | LocalDateTime | DATETIME(6) | SÍ | — |
| `siActivo` | boolean | BOOLEAN | NO | — |
| `fhAlta` | LocalDateTime | DATETIME(6) | NO | Auditoría |
| `idUserAlta` | String | CHAR(36) | NO | Auditoría |

### 7.3 FalDocumentoRedaccion (8F-1)

**Tabla candidata:** `fal_documento_redaccion`
**Clasificación:** Dominio real, persistible

| Campo Java | Tipo Java | Tipo MariaDB propuesto | Null | Notas |
|---|---|---|---|---|
| `id` | Long | BIGINT AUTO_INCREMENT | NO | PK técnica |
| `idDocumento` | Long | BIGINT FK → fal_documento.id | NO | — |
| `plantillaContenidoId` | Long | BIGINT FK → fal_documento_plantilla_contenido.id | NO | — |
| `estadoRedaccion` | EstadoRedaccionDocumento | SMALLINT | NO | BORRADOR=1, CONFIRMADA=2, ANULADA=3. **REABIERTA eliminada (8F-10-R1)** |
| `contenidoEditable` | String | TEXT | NO | Texto editable ya combinado |
| `variablesSnapshotJson` | String | **JSON** | SÍ | APROBADA_D4 - JSON nativo MariaDB 12.3.2 |
| `variablesFaltantesJson` | String | **JSON** | SÍ | APROBADA_D4 - JSON nativo MariaDB 12.3.2 |
| `diagnosticoJson` | String | **JSON** | SÍ | APROBADA_D4 - JSON nativo MariaDB 12.3.2 |
| `fhCreacion` | LocalDateTime | DATETIME(6) | NO | — |
| `idUserCreacion` | String | CHAR(36) | NO | — |
| `fhUltimaEdicion` | LocalDateTime | DATETIME(6) | SÍ | — |
| `idUserUltimaEdicion` | String | CHAR(36) | SÍ | — |
| `fhConfirmacion` | LocalDateTime | DATETIME(6) | SÍ | — |
| `idUserConfirmacion` | String | CHAR(36) | SÍ | — |

### 7.4 Nuevos enums de dominio en 8F

| Enum | Valores | Persistencia | Estado |
|---|---|---|---|
| ~~`FormatoPlantillaContenido`~~ | ~~TEXTO_PLANO, HTML_SIMPLE, MARKDOWN_SIMPLE~~ | **ELIMINADO EN 8F-10-R1**: Markdown exclusivo; campo `formato` eliminado del modelo MariaDB | Enum eliminado del modelo |
| `EstadoRedaccionDocumento` | BORRADOR=1, CONFIRMADA=2, ANULADA=3 | SMALLINT en `fal_documento_redaccion.estado_redaccion` | Enum cerrado - no tabla. **REABIERTA eliminada en 8F-10-R1** |
| `DocumentoVariableNamespace` | ACTA, INFRACTOR, DOMICILIO_INFRACTOR, ... (13+ valores) | **NO SE PERSISTE** | Puro Java registry |
| `TipoDatoVariableDocumento` | TEXTO, NUMERO, FECHA, FECHA_HORA, MONEDA, BOOLEANO | **NO SE PERSISTE** | Puro Java |

### 7.5 Nuevos servicios de dominio en 8F

| Clase | Slice | Persistible | Observaciones |
|---|---|---|---|
| `DocumentoCombinacionService` | 8F-1 | NO (servicio) | Motor de combinación de variables en template |
| `DocumentoVariableContextBuilder` | 8F-2 | NO (servicio) | Construye contexto desde FalActa |
| `DocumentoVariableRegistry` | 8F-1 | NO (singleton Java) | Registro de variables válidas |
| `DocumentoPlantillaDefaultService` | 8F-1 | NO (servicio) | Opera sobre `fal_documento_plantilla_default` |
| `DocumentoRedaccionService` | 8F-1 | NO (servicio) | Opera sobre `fal_documento_redaccion` |
| `DocumentoGeneracionMockService` | 8F-3 | NO (demo) | PDF mock — reemplazar por real |
| `DocumentoPdfMockRenderer` | 8F-3 | NO (demo) | Renderer mock |

### 7.6 Nuevos repos de dominio en 8F

| Interfaz | Impl InMemory | Tabla MariaDB candidata |
|---|---|---|
| `DocumentoPlantillaContenidoRepository` | `InMemoryDocumentoPlantillaContenidoRepository` | `fal_documento_plantilla_contenido` |
| `DocumentoPlantillaDefaultRepository` | `InMemoryDocumentoPlantillaDefaultRepository` | `fal_documento_plantilla_default` |
| `DocumentoRedaccionRepository` | `InMemoryDocumentoRedaccionRepository` | `fal_documento_redaccion` |

### 7.7 Elementos de 8F clasificados como solo demo/test/infra

| Clase | Slice | Clasificación |
|---|---|---|
| `DatasetFuncionalDominioCatalog` | 8F-4B | SOLO_DEMO_TEST |
| `ActaMockFuncionalDefinicion` | 8F-4B | SOLO_DEMO_TEST |
| `DocumentoEsperadoPorActaMock` | 8F-4B | SOLO_DEMO_TEST |
| `CasoUsoFuncionalRunner` | 8F-4C | SOLO_DEMO_TEST |
| `PlantillasMockSeeder` | 8F-2 | SOLO_DEMO |
| `GraphDemoActaFactory` | 8F-4 | SOLO_DEMO |
| `DemoActaMaterializadorService` | 8F-7 | SOLO_DEMO |
| `DemoHealthService` | 8F-8 | SOLO_DEMO |
| `DevInMemoryResetService` | 8F-5 | SOLO_DEMO_DEV |
| `ResettableInMemoryRepository` | 8F-5 | SOLO_INFRA_INMEMORY |
| `DemoCorsConfig` | 8F-6 | SOLO_INFRA_DEMO |
| `DemoActaDetalleResponse` + sub-DTOs | 8F-7 | SOLO_DEMO |
| `DemoHealthResponse` + sub-DTOs | 8F-8 | SOLO_DEMO |
| `DevResetResponse` | 8F-5 | SOLO_DEMO_DEV |
| `DemoTimelineEventoDto` | 8F-7 | SOLO_DEMO |
| `DemoDocumentoDetalleDto` | 8F-7 | SOLO_DEMO |
| `DemoHealthController` | 8F-8 | SOLO_DEMO |
| `DevResetController` | 8F-5 | SOLO_DEMO_DEV |
| `DocumentoGraphDemoController` | 8F-4 | SOLO_DEMO |
| `DocumentoRenderizadoMock` / `DocumentoGraphDemoResultado` | 8F-4 | SOLO_DEMO |

---

## 8. Tipos de datos MariaDB — decisiones cerradas y pendientes reales

### 8.1 Decisiones cerradas (D1–D9)

| Entidad | Campo | Tipo Java actual | Tipo MariaDB aprobado | Decisión |
|---|---|---|---|---|
| ~~`fal_documento_plantilla_contenido`~~ | ~~`formato`~~ | ~~FormatoPlantillaContenido~~ | **ELIMINADO EN 8F-10-R1** | Solo Markdown |
| `fal_documento_plantilla_contenido` | `cuerpo_template` / `encabezado` / `pie` | String | TEXT | NO |
| `fal_documento_plantilla_contenido` | `variables_declaradas_json` | String | **JSON** | NO — APROBADA_D4 |
| `fal_documento_plantilla_default` | `prioridad` | int | **SMALLINT** | NO — APROBADA_D9 |
| `fal_documento_plantilla_default` | `tipo_acta` | TipoActa | SMALLINT NULL | NO |
| `fal_documento_plantilla_default` | `id_dependencia` / `ver_dependencia` | Long / Short | BIGINT NULL / SMALLINT NULL | NO |
| `fal_documento_redaccion` | `estado_redaccion` | EstadoRedaccionDocumento | SMALLINT NOT NULL | NO |
| `fal_documento_redaccion` | `contenido_editable` | String | TEXT | NO |
| `fal_documento_redaccion` | `variables_snapshot_json` / `variables_faltantes_json` / `diagnostico_json` | String | **JSON** | NO — APROBADA_D4 |
| `FalActaFallo` | `id` | String UUID | BIGINT AUTO_INCREMENT | NO (ya decidido en Slice 9-0) |
| `FalActaFallo` | `fundamentos` | String libre | **TEXT NULL** | NO — APROBADA_D5 |
| `FalActaApelacion` | `id` | String UUID | BIGINT AUTO_INCREMENT | NO (ya decidido) |
| `FalActaFirmezaCondena` | toda la entidad | — sin tabla separada | **campos en `fal_acta_fallo`: `fh_firmeza`, `si_firme`, `origen_firmeza SMALLINT NULL`** | NO — APROBADA_D1 |
| `FalPagoVoluntario` | toda la entidad | String id + BigDecimal | **`fal_acta_obligacion_pago` con tipo PAGO_VOLUNTARIO** | NO — APROBADA_D2 |
| `FalPagoCondena` | toda la entidad | String id + BigDecimal | **`fal_acta_obligacion_pago` con tipo CONDENA** | NO — APROBADA_D2 |
| `FalBloqueanteMaterial` | `id` | String UUID | BIGINT AUTO_INCREMENT | NO (ya decidido) |
| `FalGestionExterna` | `id` | String UUID | BIGINT AUTO_INCREMENT | NO (ya decidido) |
| `FalNotificacion` | `id` | String UUID | **BIGINT AUTO_INCREMENT** | NO — APROBADA_D7 |
| `FalActa` | `tipoActa` | String | **SMALLINT** (enum TipoActa con código) | NO — APROBADA_D8 |

### 8.2 Pendiente real (fuera de D1–D9)

| Entidad | Campo | Tipo Java actual | Tipo MariaDB propuesto | Requiere aprobación |
|---|---|---|---|---|
| `FalActa` | `resultadoFinal` | ResultadoFinalActa | **VARCHAR(30) o SMALLINT** | **SÍ — pendiente** |

---

## 9. Cosas InMemory/demo/test que NO deben persistirse igual

### 9.1 Infra in-memory pura

- Todos los `InMemory*Repository` → reemplazar por `Jdbc*Repository` en Slice 9
- `ResettableInMemoryRepository` → no tiene equivalente JDBC
- Contadores `AtomicLong` en repos → reemplazar por `AUTO_INCREMENT` + `LAST_INSERT_ID()`
- `ConcurrentHashMap` stores → reemplazar por tablas MariaDB

### 9.2 Demo/dev-only

- `DevInMemoryResetService` / `DevResetController` / `DevResetResponse` → solo demo/dev; producción: inhabilitado
- `DemoCorsConfig` → CORS abierto solo en demo; en producción configurar origins reales
- `DemoActaMaterializadorService` → drill-down de demo; en producción no necesario
- `DemoHealthService` / `DemoHealthController` + DTOs → reemplazar por Spring Actuator
- `DatasetFuncionalDominioCatalog` / `ActaMockFuncionalDefinicion` / `CasoUsoFuncionalRunner` → no son datos reales
- `PlantillasMockSeeder` → siembra hardcoded; en producción administrar por backoffice
- `GraphDemoActaFactory` + `DocumentoEsperadoPorActaMock` → factories de datos mock
- `DocumentoGeneracionMockService` + `DocumentoPdfMockRenderer` → reemplazar por generador real
- Todos los DTOs de `/demo/**` → contratos de endpoints demo

### 9.3 Patrones mock no persistibles

| Patrón | Producción |
|--------|------------|
| `mock://...` (storageKey) | Clave real del sistema de storage |
| `sha256-mock-...` (hashDocu) | Hash real calculado sobre PDF real |

### 9.4 Variables documentales (puro Java, no tablas)

- `DocumentoVariableRegistry` (singleton Java)
- `DocumentoVariableDefinicion` (record descriptor)
- `DocumentoVariableNamespace` enum
- `TipoDatoVariableDocumento` enum

### 9.5 Textos libres transitoriamente en eventos

- `FalActaEvento.descripcion` usado por PAGAPR → en Slice 9: `fal_observacion` con entidad_tipo = GESTION_EXTERNA
- `FalActaFallo.fundamentos`  **APROBADA D5**: `fundamentos TEXT NULL` directamente en `fal_acta_fallo`.

---

## 10. Gaps reales del modelo MariaDB

### GAP-1 — Tres tablas nuevas de 8F - **RESUELTO EN 8F-10**

**CERRADO en Slice 8F-10.** Las tres tablas `fal_documento_plantilla_contenido`, `fal_documento_plantilla_default` y `fal_documento_redaccion` fueron incorporadas al modelo lógico MariaDB (secciones 5.11, 5.12, 5.13).

### GAP-2 — FalActaFirmezaCondena sin tabla directa en MariaDB

`FalActaFirmezaCondena` (con `origenFirmeza`, `fechaFirmeza`, `falloId`, `apelacionId`) no tiene tabla equivalente. El modelo MariaDB captura la firmeza en `fal_acta_fallo.fh_firmeza` + `si_firme` sin campo `origenFirmeza`.

**Decisión D1 cerrada. APLICADA EN 8F-10:** `origen_firmeza SMALLINT NULL` incorporado en `fal_acta_fallo` del modelo lógico MariaDB. Pendiente: alinear InMemory antes de JDBC.

### GAP-3 — Modelo de pagos simplificado en InMemory

`FalPagoVoluntario` y `FalPagoCondena` no tienen equivalencia directa en el modelo real de pagos MariaDB (`fal_acta_obligacion_pago`, etc.).

**Decisión D2 cerrada.** Falta implementar el mapeo real de FalPagoVoluntario y FalPagoCondena contra fal_acta_obligacion_pago en el slice de pagos/JDBC.

### GAP-4 — Paralización sin entidad en InMemory

`fal_acta_paralizacion` tiene trazabilidad completa (motivo, fh, usuario). InMemory solo tiene estado FalActa + eventos. Sin `FalActaParalizacion`, la riqueza del modelo no está implementada.

### GAP-5 — fal_acta_archivo no implementado

No hay `FalActaArchivo` in-memory.

### GAP-6 — fal_persona / fal_persona_domicilio

Datos de persona embedidos en FalActa. En MariaDB son entidades separadas.

### GAP-7 — fal_acta_articulo_infringido y valorización no implementados

Artículos infringidos y valorización del acta (monto de condena calculado) no están in-memory. Son centrales para el cálculo del monto real del fallo.

### GAP-8 - ELIMINADO (8F-11B)

Decision de diseno: `firma_storage_key`, `firma_hash`, `fh_firma_registrada` del inspector eliminados definitivamente del modelo. La firma maestra del inspector no se registra.

### GAP-9 — fal_acta_fallo: múltiples campos ausentes

Faltan: `valorizacion_id`, `resultado_fallo`, `fh_firma`, `fh_vto_apelacion`, `fallo_reemplazado_id`, `version_row`. (**`fundamentos TEXT NULL` y `origen_firmeza SMALLINT NULL` incorporados en 8F-10.**)

### GAP-10 — fal_acta_apelacion: múltiples campos ausentes

Faltan: `fallo_id`, `canal_apelacion`, `tipo_presentacion`, `texto_apelacion`, `documento_resolucion_id`, `fal_acta_apelacion_documento`, `version_row`.

### GAP-11 — fal_observacion no implementado in-memory

Observaciones tipificadas de cualquier entidad no están implementadas. PAGAPR usa evento transitoriamente.

### GAP-12 — fal_acta_documento (relación acta-documento) no implementado

Los documentos tienen `idActa` pero no hay tabla pivot explícita implementada.

---

## 11. Decisiones de Pablo — APROBADAS Y CERRADAS (8F-9-R1)

Todas las decisiones fueron aprobadas por Pablo en el Slice 8F-9-R1. Ya no figuran como pendientes.

### Decisión 1 — FalActaFirmezaCondena ✅ CERRADA

**Aprobado:** Opción A. No se crea tabla separada.

Agregar conceptualmente a fal_acta_fallo:
- origen_firmeza SMALLINT NULL — representa el origen de la firmeza de condena.
  - 1 = VENCIMIENTO_PLAZO_APELACION
  - 2 = APELACION_RECHAZADA

Impacto InMemory: FalActaFirmezaCondena existe con más campos que el modelo aprobado. Al migrar a JDBC, no tendrá tabla propia — sus datos relevantes se representan mediante fh_firmeza, si_firme y origen_firmeza en fal_acta_fallo. La entidad puede mantenerse como auxiliar de lógica o refactorizarse antes del Slice JDBC de firmeza.

---

### Decisión 2 — FalPagoVoluntario y FalPagoCondena ✅ CERRADA

**Aprobado:** Opción A. Ambos mapean a fal_acta_obligacion_pago con tipo:
- PAGO_VOLUNTARIO
- CONDENA

No se crean tablas transitorias fal_pago_voluntario / fal_pago_condena.

Impacto InMemory: FalPagoVoluntario y FalPagoCondena (id String) se mantienen para el prototipo. El JDBC implementará un repositorio único sobre fal_acta_obligacion_pago.

---

### Decisión 3 — FalActaParalizacion ✅ CERRADA

**Aprobado:** Opción A. Implementar FalActaParalizacion in-memory antes de JDBC.

Estado actual del gap:
- InMemory: solo gestión via FalActa.situacionAdministrativa = PARALIZADA + eventos.
- No existe clase FalActaParalizacion ni FalActaParalizacionRepository.
- No existe servicio de paralizacion con trazabilidad completa.

Próximo slice: implementar FalActaParalizacion in-memory antes del Slice JDBC de paralizacion.

---

### Decisión 4 — Campos JSON documentales ✅ CERRADA

**Aprobado:** JSON nativo MariaDB. Versión objetivo: **12.3.2-MariaDB** (soporta JSON nativo plenamente).

Campos afectados:
- variables_declaradas_json → JSON (no TEXT)
- variables_snapshot_json → JSON
- variables_faltantes_json → JSON
- diagnostico_json → JSON

En Java los campos siguen siendo String — se serializa/deserializa con Jackson antes de persistir. El tipo de columna MariaDB es JSON.

---

### Decisión 5 — FalActaFallo.fundamentos ✅ CERRADA

**Aprobado:** Opción A. fundamentos TEXT NULL directamente en fal_acta_fallo.

Justificación: el fundamento es parte del fallo y del documento. No debe modelarse como observación suelta.

Impacto InMemory: FalActaFallo.fundamentos es String en Java. Sin cambio requerido — mapea a TEXT NULL en MariaDB.

---

### Decisión 6 — FalPersona / FalPersonaDomicilio ✅ CERRADA

**Aprobado:** Opción A con aclaración fuerte. Implementar FalPersona y FalPersonaDomicilio in-memory antes de JDBC. Deben alinearse con fal_persona y fal_persona_domicilio.

**Auditoría de campos de persona actualmente embebidos en FalActa:**

| Campo Java en FalActa | Descripción | Tabla destino MariaDB |
|---|---|---|
| infractorNombre (String) | Nombre completo del infractor | fal_persona.nombre_completo |
| infractorDocumento (String) | DNI/CUIL del infractor | fal_persona.nro_documento |
| domicilioInfractor (String) | Domicilio texto libre embebido | fal_persona_domicilio.texto_libre o fal_acta.domicilio_infractor transitorio |
| domicilioHecho (String) | Domicilio del hecho | ffal_acta.domicilio_hecho (OK — no es dato de persona) |
| latInfr, lonInfr (Double) | Coordenadas del hecho | fal_acta.lat_infr, fal_acta.lon_infr (OK) |

**Campos identificados para fal_persona:** id BIGINT PK, nro_documento, tipo_documento SMALLINT, nombre_completo (o apellido+nombre), fh_nacimiento, sexo SMALLINT, si_identificado, si_extranjero, id_ign/id_indec/id_local, auditoría (fh_alta, id_user_alta, fh_mod, id_user_mod).

**Campos identificados para fal_persona_domicilio:** id BIGINT PK, id_persona FK, tipo_domicilio SMALLINT, id_calle_version FK, nro_puerta, piso, dpto, localidad_id, texto_libre, si_principal TINYINT(1), auditoría.

Impacto InMemory: FalPersona y FalPersonaDomicilio no existen como entidades separadas. Antes de JDBC se debe implementar su separación de FalActa. Se recomienda slice dedicado antes del Slice JDBC de actas.

---

### Decisión 7 — FalNotificacion PK ✅ CERRADA

**Aprobado:** BIGINT AUTO_INCREMENT PK. Sin UUID alternativo por ahora.

Estado actual InMemory: FalNotificacion.id es String (UUID) — **delta técnico activo** a corregir antes del Slice JDBC de notificaciones.

Acción requerida antes de JDBC:
- Cambiar FalNotificacion.id de String a Long.
- Actualizar InMemoryNotificacionRepository para usar AtomicLong.
- Actualizar todos los usos de id como String en DTOs, servicios y tests.

---

### Decisión 8 — FalActa.tipoActa ✅ CERRADA

**Aprobado:** Convertir FalActa.tipoActa a enum TipoActa. Alinear con SMALLINT en MariaDB.

Estado actual: FalActa.tipoActa es String (valor como "COMERCIO"). El enum TipoActa existe pero sin código numérico explícito.

Acciones antes de JDBC:
1. Agregar códigos SMALLINT explícitos a TipoActa (TRANSITO=1, CONTRAVENCION=2, SUSTANCIAS_ALIMENTICIAS=3, COMERCIO=4).
2. Cambiar FalActa.tipoActa de String a TipoActa.
3. Actualizar mocks, DTOs y tests afectados.

Se recomienda slice dedicado antes del JDBC de actas.

---

### Decisión 9 — fal_documento_plantilla_default.prioridad ✅ CERRADA

**Aprobado:** SMALLINT.

En Java prioridad sigue siendo int internamente (rango de SMALLINT: 0..32767 es suficiente). Al implementar el repositorio JDBC se validará que el valor esté en rango SMALLINT antes de persistir.

---

## 12. Recomendación de próximos slices

### Antes de iniciar cualquier JDBC

1. ~~**Resolver Decisiones 1–9** (Pablo)~~ **COMPLETADO en 8F-9-R1** — las 9 decisiones están aprobadas y cerradas.
2. **Actualizar modelo MariaDB** con las tres tablas de 8F (GAP-1) y los campos/tipos aprobados en 8F-9-R1 — próximo paso prioritario.
3. **Resolver deudas activas pre-JDBC** ya documentadas en 8C-6E y DELTA:
   - `num_politica`: agregar `fh_alta` + `id_user_alta` al DDL.
   - `fal_acta`: agregar `resultado_firma_infractor SMALLINT NOT NULL`.
   - `fal_acta_evidencia`: seed `tipo_evid = 6` (FIRMA_OLOGRAFA_INFRACTOR).
   - `fal_documento`: `nro_docu VARCHAR(30)`; renombrar `requisito_firma → tipo_firma_req`; agregar `plantilla_id`.
   - Crear DDL para `fal_documento_plantilla`, `fal_documento_plantilla_firma_req`, `fal_firmante*`, `fal_documento_firma_req`.
   - `fal_documento_firma`: agregar `id_firmante`, `ver_firmante`, `seq_firma_req`.
   - `fal_acta_gestion_externa`: verificar `monto_resultado DECIMAL(14,2) NULL`.

### Slice 8F-10 (pre-9, documental) - **COMPLETADO 2026-07-04**

- ✔ Tres tablas de 8F incorporadas al modelo MariaDB (secciones 5.11, 5.12, 5.13).
- ✔ Decisiones D1 (origen_firmeza) y D5 (fundamentos) aplicadas en `fal_acta_fallo`.
- ✔ Sección 0.8 con tabla de D1-D9 incorporada al modelo MariaDB.

### Slice 9 (JDBC) — orden recomendado (igual que 8C-6E)

9-1: Infraestructura JDBC base.
9-2: Piloto plantillas documentales (incluye las tres tablas nuevas de 8F).
9-3: Firmantes / habilitaciones.
9-4: Inspectores y dependencias.
9-5: Talonarios / numeración.
9-6: FalDocumento + firma req + firma real.
9-7: Emision formal + adjuntos + eventos.
9-8: Actas core.
9-9: Repositorios UUID/String + retiro de legacy + `FalDocumentoRedaccion` JDBC.

---

## 13. Checklist de validación

| Ítem | Estado |
|------|--------|
| Build ejecutado (resultado post-slice) | ver sección 3 |
| `Test-Path docs\spec-as-source` desde raíz | FALSE — guardrail OK |
| Sin `@Entity` nuevo | ✅ |
| Sin `JpaRepository` | ✅ |
| Sin `EntityManager` | ✅ |
| Sin SQL/migraciones nuevas | ✅ |
| Sin frontend Angular nuevo | ✅ |
| Sin storageKey `s3://` o `file://` real | ✅ |
| Sin hashDocu real | ✅ |
| Sin endpoints productivos nuevos en este slice | ✅ (slice documental) |
| Documento 109 creado | ✅ |
| 99-pendientes actualizado con 8F-9 / R2 | ✅ |
| Tres tablas nuevas de 8F identificadas | ✅ |
| 9 decisiones de Pablo cerradas y aplicadas consistentemente en todo el documento | ✅ |
| 0 apariciones de «Requiere decisión de Pablo» asociadas a D1-D9 | ✅ |
| 0 apariciones de «TEXT o JSON» en campos JSON aprobados | ✅ |
| 0 apariciones de «SMALLINT o INT» en prioridad aprobada | ✅ |
| 0 caracteres corruptos en nombres de tablas/campos | ✅ |
| Tipos de dato para aprobación tabulados (sección 8.1 + pendiente real 8.2) | ✅ |
| Elementos solo demo/test/infra clasificados | ✅ |
| Gaps reales del modelo MariaDB identificados (12) | ✅ |

---

## 14. Resumen final del delta

| Categoría | Cantidad |
|-----------|----------|
| Entidades InMemory alineadas con MariaDB | ~20 |
| Entidades InMemory con gaps parciales | 7 |
| Entidades InMemory que requieren tabla nueva en MariaDB | 3 |
| Tablas MariaDB sin implementación InMemory | ~25 |
| Enums solo Java (no persistir como tabla) | 4 (en 8F) |
| Elementos clasificados como solo demo/test/infra | 20+ |
| Gaps reales del modelo MariaDB documentados | 12 |
| Decisiones de Pablo aprobadas y cerradas | 9 |
| Deudas activas pre-JDBC conocidas (pre-8F) | ~15 (documentadas en 8C-6E + DELTA) |

**Las 9 decisiones de Pablo están aprobadas y cerradas (8F-9-R1). El próximo paso es actualizar el modelo MariaDB con las 3 tablas nuevas de 8F y los campos aprobados, antes de iniciar el Slice 9 (JDBC).**

---

## 15. Revisión 8F-10-R1 (2026-07-04)

El modelo MariaDB lógico fue revisado en 8F-10-R1 con las siguientes refinaciones en las secciones 5.11, 5.12 y 5.13:

### Cambios en 5.11 `fal_documento_plantilla_contenido`

- Eliminados: campo `formato` y enum `FormatoPlantillaContenido`. Todo el contenido es exclusivamente Markdown.
- Renombrados: `cuerpo_template` → `cuerpo_markdown`, `encabezado_template` → `encabezado_markdown`, `pie_template` → `pie_markdown`.
- `variables_declaradas_json`: ahora NOT NULL; usar `[]` cuando no haya variables.
- Assets solo mediante expresiones controladas (`{{asset.NOMBRE}}`).

### Cambios en 5.12 `fal_documento_plantilla_default`

- Algoritmo de resolución refinado con niveles de especificidad (Nivel 2 / 1 / 0).
- Pasos 12 y 13: resolución de la versión de contenido Markdown activa y vigente.
- La regla default apunta a `fal_documento_plantilla`; la versión concreta se congela en `fal_documento_redaccion`.

### Cambios en 5.13 `fal_documento_redaccion`

- Eliminado estado `REABIERTA`. Estados: BORRADOR=1, CONFIRMADA=2, ANULADA=3.
- Una redacción confirmada no vuelve a editable. Rehacerla crea nueva fila BORRADOR.
- Agregados: `nro_revision SMALLINT NOT NULL UK`, `redaccion_origen_id BIGINT NULL FK` (self FK).
- Desdoblado: `contenido_editable` → `contenido_base_markdown` (solo lectura) + `contenido_editable_markdown` (editable).
- JSON fields NOT NULL: `variables_snapshot_json` (`{}`), `variables_faltantes_json` (`[]`), `diagnostico_json` (`{}`).
- Agregado: `recursos_snapshot_json JSON NULL`.
- Agregados: `fh_ultima_regeneracion`, `id_user_ultima_regeneracion`, `fh_anulacion`, `id_user_anulacion`, `motivo_anulacion`.

### Nuevos deltas InMemory vs. modelo MariaDB

| Entidad Java | Delta | Resolución recomendada |
|---|---|---|
| `FalDocumentoPlantillaContenido` | Tiene campo `formato` / enum `FormatoPlantillaContenido` que se eliminan del modelo MariaDB | Eliminar antes del Slice JDBC documental |
| `FalDocumentoRedaccion` | Tiene estado `REABIERTA`; campo `contenidoEditable` en lugar de `contenidoBaseMarkdown` + `contenidoEditableMarkdown`; faltan `nroRevision`, `redaccionOrigenId`, `recursosSnapshotJson`, `fhAnulacion`, etc. | Alinear antes del Slice JDBC documental |
| `DocumentoPlantillaDefaultService` | Algoritmo de prioridad sin niveles de especificidad | Actualizar lógica antes del Slice JDBC documental |



---

## 8F-11D — Normativa, Tarifario, Medidas y Valorización (2026-07-05)

### CERRADO: 6 tablas alineadas

| Tabla MariaDB | Clase Java | Estado |
|---|---|---|
| al_tarifario_unidad_faltas | FalTarifarioUnidadFaltas | ALINEADO |
| al_medida_preventiva | FalMedidaPreventiva | ALINEADO |
| al_articulo_medida_preventiva | FalArticuloMedidaPreventiva + ArticuloMedidaPreventivaId | ALINEADO |
| al_acta_articulo_infringido | FalActaArticuloInfringido | ALINEADO |
| al_acta_valorizacion | FalActaValorizacion | ALINEADO |
| al_acta_valorizacion_item | FalActaValorizacionItem | ALINEADO |

### Decisiones relevantes
- TipoUnidadFaltas creado como enum nuevo con codigo() (no renombrar TipoUnidad existente)
- 	ipoBloqueanteDefault en FalMedidaPreventiva reutiliza OrigenBloqueanteMaterial
- FalActaValorizacion tiene ersionRow con optimistic locking real
- FalActaValorizacionItem sin ersionRow ni auditoría de usuario (inmutable post-confirmación)
- FalActaArticuloInfringido sin montos, sin tarifario, sin observación
- Una vigente por acta+tipo garantizada atómicamente en InMemory
- FalActaSnapshot extendido con alorizacionOperativaId, estadoValorizacionOperativa, 	ipoValorizacionOperativa, montoOperativoVigente, siMontoConfirmado

### Guardrails eliminados de NormativaTest
- 
o_existe_tarifario → eliminado (8F-11D implementa FalTarifarioUnidadFaltas)
- 
o_existe_tarifario_repository → eliminado (8F-11D implementa TarifarioUnidadFaltasRepository)

### Conteo: 27 → 21 FALTA_EN_INMEMORY