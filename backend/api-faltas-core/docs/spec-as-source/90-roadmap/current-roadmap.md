# Roadmap vigente posterior a spec-as-source

> **Estado documental:** SUPPORTING_CURRENT
> **Autoridad DDL:** SUPPORTING
> Este documento describe unicamente el trabajo pendiente vigente despues del
> cierre formal de la etapa spec-as-source (ver
> [`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md)).
> La evolucion historica se conserva en Git y no forma parte de la spec-as-source
> activa.

**Fecha de actualización:** 2026-07-24
**Estado:** spec-as-source auditada transversalmente; puerta `READY_FOR_DDL` evaluada en
[`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md);
todas las `DECISION_DDL-*` cerradas (ver
[`../50-persistence/ddl-decisions.md`](../50-persistence/ddl-decisions.md));
script DDL canónico completo (64 tablas) aprobado para ejecución manual controlada
(trabajo DDL-MARIADB-MANUAL-001-FULL-R1; ver `database/ddl/create-bod-faltas-domain.sql`);
esquema en MariaDB aún no creado.

## 1. Baseline vigente

El modelo funcional InMemory actúa como oráculo de paridad para la persistencia.

Estado verificado:

- modelo de dominio y lifecycle estabilizados;
- contratos funcionales de los siete comandos canónicos de fallo (CMD-FALLO-001..007) y del resto
  del circuito cerrados y verificados contra el código vigente;
- modelo conceptual de tablas objetivo MariaDB listo (ver
  [`../50-persistence/mariadb-logical-model.md`](../50-persistence/mariadb-logical-model.md));
  todas las `DECISION_DDL-*` están cerradas (ver
  [`../50-persistence/ddl-decisions.md`](../50-persistence/ddl-decisions.md)); 0 enums
  0 enums `NO_EXPLICIT_CODE` vigentes;
- `FaltasClock` como abstracción temporal canónica;
- concurrencia optimista InMemory estabilizada;
- contrato global de errores HTTP cerrado mediante `ErrorResponse` y `GlobalFaltasControllerAdvice`;
- spec-as-source activa auditada transversalmente y saneada en UTF-8;
- infraestructura JDBC base incorporada: dependencia Spring JDBC, driver MariaDB, perfil `jdbc`,
  configuración de `DataSource` y prueba condicionada de infraestructura (ver
  [`../50-persistence/jdbc-infrastructure.md`](../50-persistence/jdbc-infrastructure.md));
- script DDL canónico aprobado para ejecución manual controlada: `database/ddl/create-bod-faltas-domain.sql`,
  64 tablas TO_CREATE + adopción de `fal_rubro_version`; guardrails estáticos en 3 clases de test
  (`DdlInventoryContractTest`, `DdlScriptContractTest`, `DdlDiagnosticsSafetyTest`); verdes en CI;
  esquema en MariaDB aún no creado;
- sin repositorios JDBC de dominio ni JPA/Hibernate todavía; sin Flyway/Liquibase por DECISION_DDL-EXEC-01 (no se incorporarán); sin Testcontainers ni DDL ejecutado todavía.

Las definiciones activas de paridad y estrategia de persistencia se encuentran en:

- [`../50-persistence/mariadb-logical-model.md`](../50-persistence/mariadb-logical-model.md)
- [`../50-persistence/ddl-decisions.md`](../50-persistence/ddl-decisions.md)
- [`../50-persistence/inmemory-mariadb-deltas.md`](../50-persistence/inmemory-mariadb-deltas.md)
- [`../50-persistence/jdbc-strategy.md`](../50-persistence/jdbc-strategy.md)

## 2. Siguiente secuencia de trabajo

### 2.1 Slice DDL cerrado y auditado — COMPLETADO

Slice `DDL-MARIADB-MANUAL-001-FULL-R1` cerrado y auditado externamente.
Reauditoría externa independiente declaró: `DDL APTO PARA EJECUCIÓN MANUAL CONTROLADA`.
Script aprobado con release gate formal; historia de auditoría preservada en Git.

### 2.2 Ejecutar script DDL manualmente

**[APROBADO — pendiente ejecución]**

Script canónico `database/ddl/create-bod-faltas-domain.sql` (64 tablas) aprobado para ejecución.
Ejecutar desde HeidiSQL contra MariaDB 12.3.2.
Verificar post-ejecución con `database/diagnostics/verify-domain-schema.sql` y
`database/diagnostics/verify-protected-baseline.sql`.

### 2.4 Implementar seeder

Ver contrato en `../50-persistence/ddl-execution-and-test-seeding.md`
(operaciones VERIFY/SEED/RESET_TEST_DATA/RESET_AND_SEED, allowlists, baseline
protegido).

### 2.5 Implementar adapters JDBC

Implementar `JdbcXxxRepository` para cada puerto de repositorio de dominio,
sin modificar las interfaces existentes ni los servicios de aplicación.
Tests de integración JDBC separados; no sustituir los tests InMemory.

### 2.6 Validar backend real contra MariaDB

- Suite existente completamente verde sin cambios funcionales.
- Tests de integración MariaDB verdes.
- OCC/bloqueos multinodo verificados (ver `inmemory-mariadb-deltas.md`,
  sección 12).

### 2.7 UX discovery/redesign (fase posterior)

Hoy existe una demo navegable in-memory. El frontend productivo y el
discovery de UX son fases posteriores a la validación con MariaDB real.

### 2.8 GeoDomicilios — slices de implementación

El modelo canónico completo está definido en
[`../60-geo/geodomicilios.md`](../60-geo/geodomicilios.md).

**Decisiones cerradas en GEO-SPEC-002-CORRECCION-02-R1:**
- La unidad principal de geocodificación es el **segmento físico** (`gis_calle_tramo_altura`), no el lado de cuadra.
- Validacion oficial Style Spec con @maplibre/maplibre-gl-style-spec (bloqueo GEO-SPEC-003-CORRECCION-01; requiere herramienta disponible en E1).
- Calibracion visual de jerarquia vial (anchos, colores, opacidades por zoom).
- Evaluacion de capas contextuales (capas-contextuales-pendientes: agua, espacios verdes, ferrocarril, edificios, contexto exterior).
- Definir fuente productiva de glyphs/tipografia (glyph-productivo-pendiente: Noto Sans o Inter desde Martin o CDN controlado).
- MariaDB está fuera del baseline del geocodificador geográfico.
- `id` (PK de `geo_gmat_calle`) es el identificador estable del segmento. `id_tramo` tiene 14 duplicados y no es confiable.
- La columna de calle lógica en la tabla física es `ccalle` (no `id_tca`).
- Cobertura diagnóstica por segmento: 87.6 % con un único candidato; 91.9 % con al menos uno.
- Ambigüedad lejana (> 100 m): 0.5 % del total de domicilios geocodificables.
- La paridad mejora significativamente (63.7 % de los casos ambiguos), pero es opcional en el baseline.

#### GEO-MODEL-002-A — Delta de modelo Java/DDL/tests

Alcance:

- `fal_acta_contravencion` (DDL): agregar `quinta VARCHAR(7)`, ajustar `secc VARCHAR(2)` →
  `VARCHAR(7)`, ajustar `ucomp VARCHAR(20)` → `VARCHAR(3)`, agregar campos de comercio no
  registrado (`si_comercio_no_registrado`, `comercio_no_reg_nombre_fantasia`,
  `comercio_no_reg_titular`).
- `FalActaContravencion.java`: actualizar validaciones de longitud máxima; agregar `quinta`
  con getter/setter; agregar campos de comercio no registrado con reglas de negocio.
- Commands/DTOs: incorporar `quinta` y campos de comercio no registrado.
- HTTP contracts (`40-api/http-contracts.md`): actualizar requests/responses afectados.
- `FalActaSnapshot.java` y `snapshot-bandejas-acciones.md`: evaluar proyección de nuevos campos.
- Guardrails (`MariaDbLogicalModelParityGuardrailTest`): actualizar tokens de verificación.
- Tests: actualizar tests de `FalActaContravencion`, `EnriquecimientoContravencionService` y
  demás tests afectados.
- Sin Flyway ni Liquibase; la política vigente los prohíbe.

#### GEO-MODEL-002-B — Acceso real a MariaDB (datasource MariaDB)

Alcance: configurar datasource MariaDB para GeoDomicilios; implementar repositorios JDBC
de lookups de catálogos IGN/INDEC/BAHRA y catálogos locales.
MariaDB no participa en el geocodificador de calle-altura (ver §16 de geodomicilios.md).

#### GEO-MODEL-002-C — Diagnóstico e infraestructura PostGIS

**[COMPLETADO CON CORRECCION TECNICA — GEO-MODEL-002-C-POSTGIS-02-CORRECCION-01]**

Resultados: PostgreSQL 15.2 / PostGIS 3.3.2; sandbox `sb_malvinas_geo`; 79.169 parcelas (SRID 22195).
Objetos auditados: `geo_qmat_parcela`, `geo_gmat_localidad` (9 loc.), `geo_gmat_barrio` (93),
`fal_inmueble` (85.167), `fal_comercio` (13.024), `fal_comercio_inmueble` (12.737).
Vista `gis_parcela_web`: VIEW NORMAL, geom nativa (MultiPolygon 22195 y 4326 + Point 4326), filtro por `geom_22195` con GiST, etiqueta canónica Q/F/M/P.
Ver handoff `GEO-MODEL-002-C-POSTGIS-02-CORRECCION-01.md`.

**Diagnóstico ampliado (GEO-SPEC-002-CORRECCION-02-R1):** cobertura por segmento físico, identificador estable, paridad y separación espacial documentados en `geodomicilios.md` §15.4 y handoff `GEO-SPEC-002-CORRECCION-02-R1.md`.

#### GEO-MODEL-002-D1 — Backend PostGIS: servicios base

Alcance **exclusivo** de servicios base; **sin geocodificador final ni reverse de altura**:

- `LocalidadCacheService`: caché inmutable de las nueve localidades.
- Validación municipal: punto dentro del polígono municipal.
- Resolución punto → localidad (PostGIS).
- Resolución punto → parcela (PostGIS).
- Endpoints de inmuebles y comercios.
- Configuración datasource PostGIS.
- Sin InMemory Geo. Sin proxy de Google en backend.
- Las capas vectoriales se sirven por Martin (MVT), no por endpoint GeoJSON Spring.

#### GEO-SPEC-003-MAPLIBRE-STYLE-UX-01 — Style Malvinas Claro y requisitos visuales

**[CERRADO — 2026-07-24]**

Resultados:

- Filosofía visual Malvinas Claro canonicalizada (Apple Maps Muted; familiaridad Google Maps).
- Paleta de 9 localidades aprobada y validada contra PostGIS.
- Matriz zoom/capa definida (z10–z19+).
- Doble trazo MapLibre para calles; corredor PostGIS reservado como fallback.
- Manzanas z15–z16; parcelas desde z17 aproximadamente.
- Etiquetas parcelarias jerarquizadas (manzana→corta→completa).
- Autocomplete territorial y buscador flotante definidos.
- Accesibilidad y rendimiento documentados.
- Martin inventariado: 5 fuentes, TileJSON completo, gaps identificados.
- Style draft JSON válido: `malvinas-claro.draft.style.json` (20 capas, 5 fuentes Martin).
- Tokens visuales centralizados: `malvinas-claro.tokens.json`.
- Ver handoff `GEO-SPEC-003-MAPLIBRE-STYLE-UX-01.md`.
- Correccion GEO-SPEC-003-CORRECCION-01: style a 25 capas (jerarquia vial por zoom); maxzoom de localidades corregido; peatonales/semipeatonales alineados; tokens sin drift; glyphs verificados (fonts.openmaptiles.org, Open Sans confirmados); 5 TileJSON capturados; UTF-8 sin BOM. Ver handoff `GEO-SPEC-003-CORRECCION-01.md`.
- Correccion GEO-SPEC-003-CORRECCION-02: generador -Generate implementado (Build-MalvinasClaroStyle.ps1); style construido deterministicamente desde tokens; idempotente (SHA256 estable); font arrays simplificados a stack exacto unico (Open Sans Semibold, Open Sans Regular; sin Arial Unicode MS); stacks verificados HTTP 200 (rango 0-255 y 256-511); Unicode inventariado (47 chars, max U+00D1); validacion 108 checks / 0 ERR; prueba negativa 3 drifts detectados / exit 1. Ver handoff GEO-SPEC-003-CORRECCION-02.md.

#### GEO-MODEL-002-E1 — Benchmark Martin/MapLibre y decisión de zooms productivos

Alcance:

- Benchmark concurrente MVT: tiles zona densa/media/periférica, varios zooms.
- Métricas P50/P95 de tamaño y latencia; cache hit/miss; features y atributos por nivel.
- Zoom mínimo, simplificación y atributos MVT productivos.
- Candidato `gis_parcela_web` como source de Martin.
- Calibración visual de tokens de zoom definidos en GEO-SPEC-003-MAPLIBRE-STYLE-UX-01 (§20.4 de geodomicilios.md).
- **Sin lógica de geocodificación en el cliente.**

#### GEO-MODEL-002-E2 — Modelo `gis_calle_tramo_altura` y gate 85 %

**[PRE-DIAGNÓSTICO COMPLETADO — GEO-SPEC-002-CORRECCION-02-R1]**

Alcance:

- Diseñar vista/query `gis_calle_tramo_altura` (segmento físico como unidad principal).
- Ejecutar diagnóstico completo mediante CTE/consultas read-only.
- Medir cobertura por segmento (no por lado).
- Medir ambigüedad espacial (cercana/lejana).
- Evaluar paridad como mejora opcional.
- Ejecutar holdout estratificado (§19 de geodomicilios.md).
- Cerrar gate 85 %: misma calle + mismo segmento/cuadra.
- Producir DDL propuesto de `gis_calle_tramo_altura`, no ejecutado.
- **No crear objetos persistentes en este slice.**

Fuentes exclusivas: PostgreSQL/PostGIS. MariaDB no participa.

#### GEO-MODEL-002-D2 — Backend geocodificador y reverse

**Solo después de aprobar E2 (gate 85 %).**

Alcance:

- Autocomplete territorial: calle + localidad.
- Geocodificación: calle + localidad + altura → segmento, calidad, punto sugerido.
- Validación territorial con `ccalle + id_loc`.
- Reverse local: punto → calle, segmento, altura estimada (si orientación confiable).
- Confirmación obligatoria del inspector para todo punto sugerido.

#### GEO-MODEL-002-E — Prototipo Angular 20 con MapLibre GL JS

Alcance exclusivo Angular 20:

- MapLibre GL JS como visor principal; capas MVT desde Martin.
- Autocomplete de calles con localidad obligatoria (`ccalle + id_loc`, §1.4 de geodomicilios.md).
- Selección y corrección de punto y altura por el inspector.
- Consume el backend (D1 y D2); **no ejecuta reglas de geocodificación en el cliente.**
- No usa Google Maps JavaScript API, Google Places ni Google reverse geocoding en el baseline.

#### GEO-MODEL-002-F — Android MapLibre Native

Alcance exclusivo Android Kotlin:

- MapLibre Native para Android como visor nativo; MVT desde Martin.
- Ubicación nativa (`FusedLocationProviderClient`).
- Consume el backend (D1 y D2).
- Sin Google Maps SDK ni Places SDK en el baseline.
- Fase posterior al prototipo Angular 20.

#### GEO-MODEL-002-G — DDL y objetos aprobados

**Único slice autorizado para crear objetos persistentes aprobados.**

Alcance: ejecución del DDL aprobado de `gis_calle_tramo_altura` y otros objetos geográficos;
verificación post-ejecución; cierre formal del modelo georreferenciado.

## 3. Integraciones externas

Estas integraciones dependen de sistemas externos y no bloquean el diseño ni la
generación del DDL de MariaDB:

- Integración real con la aplicación de Firmas (más allá del callback `firmar-real` ya implementado; posible ampliación de flujo).
- Integración real con el sistema de Notificaciones (envío postal/electrónico real; hoy simulado con lotes y acuses mock).
- Integración real con Ingresos/Tesorería (hoy la conciliación económica usa un input mock absoluto de Tesorería).
- Storage y comprobantes reales (hoy `storageKey` usa esquemas `mock://`; sin `file://` ni `s3://` reales).
- Sincronización real de calendario administrativo con un proveedor externo (hoy sin integración; ver `../10-domain/calendario-plazos-administrativos.md`, sección 14).
- Frontend productivo, cuando corresponda (hoy existe una demo navegable in-memory; ver `../40-api/http-contracts.md`, endpoints `/demo/**`).

## 4. Mejoras no bloqueantes

- Revisión del mapeo `INSPECTOR_NO_ENCONTRADO` para `FirmanteNoEncontradoException` (ver `../00-governance/command-contract-standard.md`, tabla de errores); requiere decisión explícita, no se corrige silenciosamente.
- Decisión funcional pendiente sobre la seguridad de `pago-condena/confirmar`, `pago-condena/observar` y
  `GET pago-condena` (hoy `permitAll` en `SecurityConfig`; ver `../40-api/http-contracts.md`).

## 5. Regla de mantenimiento de este documento

Este documento contiene únicamente el trabajo pendiente vigente. No agregar
cronologías de bloques cerrados, resultados históricos extensos ni copias de
handoffs. Eliminar cada ítem de este documento en cuanto quede cerrado,
trasladando el cierre a `../00-governance/ready-for-ddl-gate.md` o al historial de Git.
