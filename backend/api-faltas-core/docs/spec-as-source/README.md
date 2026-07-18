# Spec-as-source canónica — API Faltas Core

## 1. Propósito

Esta carpeta es la única spec-as-source canónica del backend productivo de
Faltas (`backend/api-faltas-core`). Las reglas funcionales, arquitectónicas y
de persistencia aprobadas deben quedar expresadas aquí antes de considerarse
vigentes.

El código Java, los tests, los modelos anteriores, las matrices, los deltas,
los handoffs y la historia Git son fuentes de evidencia, auditoría o
conformidad. No pueden redefinir silenciosamente la spec canónica.

La auditoría transversal final
([`00-governance/ready-for-ddl-gate.md`](00-governance/ready-for-ddl-gate.md))
cierra formalmente la etapa spec-as-source. Ver "Estado `READY_FOR_DDL`" más
abajo.

## 2. Estructura temática

Esta carpeta usa una estructura temática única, sin numeración residual de
slices ni directorio `handoff/`:

- [`00-governance/`](00-governance/) — vocabulario, estándar de contratos de comando, registro documental, gate `READY_FOR_DDL`.
- [`10-domain/`](10-domain/) — dimensiones de estado y lifecycle, catálogo de bloques/estados/eventos, calendario de plazos, firma y notificación del fallo.
- [`20-application/`](20-application/) — contratos de comandos de aplicación (catálogo general y familia de fallo).
- [`30-projections/`](30-projections/) — snapshot, bandejas y acciones pendientes derivadas.
- [`40-api/`](40-api/) — contratos HTTP de los endpoints productivos.
- [`50-persistence/`](50-persistence/) — estrategia e infraestructura JDBC, deltas InMemory/MariaDB, modelo lógico de MariaDB y decisiones DDL abiertas.
- [`90-roadmap/`](90-roadmap/) — roadmap vigente posterior al cierre de spec-as-source.

## 3. Registro documental

El [`00-governance/document-registry.md`](00-governance/document-registry.md)
es la fuente única de clasificación y autoridad DDL para cada `.md` de esta
carpeta. Cada archivo aparece allí exactamente una vez, con su clasificación
(`NORMATIVE`, `SUPPORTING_CURRENT` o `PRE_DDL_PLAN`) y su autoridad DDL (`YES`
o `SUPPORTING`).

## 4. Regla de precedencia

Ante una contradicción:

1. no elegir una versión por fecha, nombre de archivo, cantidad de tests o cercanía con el código;
2. no inventar una conciliación;
3. registrar y reportar el gap;
4. obtener una decisión humana explícita cuando afecte dominio, contratos o arquitectura;
5. actualizar primero la spec canónica;
6. alinear después código, tests, persistencia y documentos de referencia.

Una implementación existente demuestra comportamiento, pero no convierte
automáticamente ese comportamiento en regla normativa.

Precedencia mínima entre documentos de la spec, de mayor a menor autoridad:

1. `00-governance/` (glosario, estándar de contratos de comando, registro documental, gate `READY_FOR_DDL`).
2. `10-domain/` (dimensiones de estado, lifecycle, catálogo de bloques/estados/eventos, firma/notificación, calendario).
3. `20-application/` (contratos de comandos de aplicación).
4. `30-projections/` y `40-api/` (`NORMATIVE`).
5. `50-persistence/` y `90-roadmap/` (`SUPPORTING_CURRENT` / `PRE_DDL_PLAN`).

Ante contradicción entre un documento de `00-governance/` o `10-domain/` y un
documento de contrato funcional de otra carpeta, el documento de
`00-governance/`/`10-domain/` es normativo en lo que respecta a definiciones
de términos, dimensiones y lifecycle. Cuando
[`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md)
declare precedencia específica para `CMD-FALLO-001..007`, esa precedencia
específica se mantiene: ese documento prevalece sobre cualquier otro para esos
siete comandos.

## 5. Orden de lectura

Para cualquier trabajo en `backend/api-faltas-core`:

1. leer este `README.md`;
2. consultar el [registro documental](00-governance/document-registry.md) para identificar la clasificación de los documentos aplicables al alcance;
3. leer, en orden temático:
   1. [`00-governance/glossary.md`](00-governance/glossary.md) — vocabulario canónico;
   2. [`10-domain/lifecycle-states.md`](10-domain/lifecycle-states.md) — dimensiones de estado y lifecycle;
   3. [`10-domain/states-events-catalogs.md`](10-domain/states-events-catalogs.md) — catálogo de bloques, estados, catálogos y eventos;
   4. [`10-domain/calendario-plazos-administrativos.md`](10-domain/calendario-plazos-administrativos.md) — calendario y plazos;
   5. [`00-governance/command-contract-standard.md`](00-governance/command-contract-standard.md) — estándar de contratos de comando;
   6. [`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md) — contratos de CMD-FALLO-001..007;
   7. [`10-domain/firma-notificacion-fallo.md`](10-domain/firma-notificacion-fallo.md) — circuito de firma y cola notificatoria;
4. leer solamente el subconjunto adicional necesario para el alcance:
   [`20-application/command-contracts.md`](20-application/command-contracts.md),
   [`30-projections/snapshot-bandejas-acciones.md`](30-projections/snapshot-bandejas-acciones.md),
   [`40-api/http-contracts.md`](40-api/http-contracts.md);
5. consultar código y tests como evidencia de conformidad;
6. consultar documentación externa a esta spec (si existiera) únicamente como fuente de auditoría o diseño físico, nunca como autoridad normativa;
7. detenerse y reportar si existe una contradicción no resuelta.

## 6. Cómo se usa la spec para diseñar el DDL de MariaDB

El diseño del DDL versionado de MariaDB debe partir de:

1. los documentos `NORMATIVE` (estados, eventos, comandos, endpoints) para las reglas de dominio que las tablas y columnas deben reforzar, nunca redefinir;
2. [`50-persistence/inmemory-mariadb-deltas.md`](50-persistence/inmemory-mariadb-deltas.md) para los deltas vigentes de identidad, versionRow/OCC, unicidades, atomicidad, transacciones y enums/catálogos;
3. [`50-persistence/mariadb-logical-model.md`](50-persistence/mariadb-logical-model.md) como matriz de entrada: por cada agregado/repositorio, identidad primaria, clave natural, FKs conceptuales, unicidad física, índices, versionRow/OCC, frontera transaccional y fuente normativa;
4. [`50-persistence/ddl-decisions.md`](50-persistence/ddl-decisions.md) para las decisiones físicas todavía abiertas (`DECISION_DDL-*`);
5. [`50-persistence/jdbc-strategy.md`](50-persistence/jdbc-strategy.md) y [`50-persistence/jdbc-infrastructure.md`](50-persistence/jdbc-infrastructure.md) para la estrategia y la infraestructura JDBC ya incorporada.

El DDL no puede modificar reglas de dominio, estados, eventos, transiciones,
bandejas ni contratos HTTP vigentes. Toda decisión física que requiera
análisis durante el diseño del DDL se marca `DECISION_DDL` en
`50-persistence/ddl-decisions.md`; no se presenta como gap funcional.

## 7. Estado `READY_FOR_DDL`

La auditoría transversal final registrada en
[`00-governance/ready-for-ddl-gate.md`](00-governance/ready-for-ddl-gate.md)
declara el estado `READY_FOR_DDL`: la etapa spec-as-source queda formalmente
cerrada y la siguiente etapa autorizada es diseñar y generar el DDL
versionado de MariaDB, sin alterar los contratos funcionales vigentes.

## 8. Qué queda fuera de esta spec

Quedan explícitamente fuera del alcance de spec-as-source y de esta
auditoría:

- la generación del DDL/SQL definitivo de MariaDB;
- la implementación JDBC de los repositorios de dominio;
- las transacciones por comando, unicidades y FKs físicas, y OCC/bloqueos multinodo;
- la migración de datos existentes;
- las integraciones externas reales (Firmas, Notificaciones, Ingresos/Tesorería, storage/comprobantes reales, calendario administrativo externo);
- el frontend productivo.

Estos ítems son el roadmap vigente en
[`90-roadmap/current-roadmap.md`](90-roadmap/current-roadmap.md).

## 9. Fuentes externas a esta carpeta

Los tests ejecutables y la implementación InMemory constituyen la evidencia
principal para comprobar paridad durante la incorporación de MariaDB. Si
difieren de la spec, debe registrarse el gap y resolverse; no se debe
modificar la spec automáticamente para copiar el código.

El prototipo (`backend/api-faltas-prototipo`) y el frontend Angular son
evidencia de UX o integración. Nunca constituyen por sí solos una fuente
normativa de dominio.

## 10. Guardrails

- No inventar eventos, bloques, estados, bandejas, acciones ni catálogos.
- No confundir estado de dominio, estado documental, bandeja operativa, acción disponible o proyección de snapshot.
- No modificar reglas funcionales para facilitar la persistencia.
- No reintroducir conceptos explícitamente eliminados.
- No usar cronologías de slices como sustituto de una definición vigente consolidada en documentos `NORMATIVE`, `SUPPORTING_CURRENT` o `PRE_DDL_PLAN`.
- No eliminar documentación sin extracción y trazabilidad previas.
- No considerar cerrado un cambio documental si deja referencias rotas o fuentes de autoridad contradictorias.
- `SpecAsSourceGuardrailTest` (ver `backend/api-faltas-core/src/test/java/.../application/SpecAsSourceGuardrailTest.java`) automatiza la verificación del registro, los links relativos, los términos prohibidos y el gate `READY_FOR_DDL`.
