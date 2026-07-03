# CONTEXTO ACTUAL — Faltas backend api-faltas-core

**Fecha de generación:** 2026-07-02 19:06  
**Generado por:** mini-slice ZIP de contexto

---

## 1. Build actual

**954/954 tests passing. BUILD SUCCESS.**

Historial reciente de builds:

| Slice               | Tests   |
|---------------------|---------|
| Inicio Etapa 8      | 330/330 |
| 8B-4                | 561/561 |
| 8A final            | 454/454 |
| 8C-5A               | 746/746 |
| 8C-5B               | 766/766 |
| 8C-6B-1             | 816/816 |
| 8C-6C-1             | 855/855 |
| 8C-6D-1             | 908/908 |
| 8C-6E (solo docs)   | 908/908 |
| 9-0 (solo docs)     | 908/908 |
| 9-1 (JDBC infra)    | 908/908 |
| **8F-1 (actual)**   | **954/954** |

---

## 2. Estado actual de slices

### Etapa 8C — Documentos y firma — CERRADO

Todos los micro-slices 8C cerrados:
- 8C-0D: Base documental
- 8C-0A: Alineación FalDocumento contra modelo MariaDB
- 8C-5A: Bandeja firma por firmante
- 8C-5B: Bandeja firma — consolidación
- 8C-6A: Firma documental — diagnóstico
- 8C-6B-0: Firma documental — requisito
- 8C-6B-1: Registrar firma real documental
- 8C-6C-0: Diagnóstico contrato emisión formal
- 8C-6C-1: Emisión formal in-memory con storage/hash simulado
- 8C-6D-0: Diagnóstico documento escaneado/ADJUNTO/convalidación
- 8C-6D-1: Documento escaneado y convalidación de firma ológrafa
- **8C-6E: Auditoría pre-JDBC/MariaDB — CERRADO (solo documentación)**

**Build final Etapa 8C: 908/908 tests passing. BUILD SUCCESS.**

### Slice 9-0 — Estrategia JDBC/MariaDB — CERRADO

- Solo documentación. Sin cambios Java.
- Plan incremental Slice 9 (9-1 a 9-9) documentado.
- **Build: 908/908 tests passing. BUILD SUCCESS.**

### Slice 9-1 — Infraestructura JDBC base — CERRADO

- Dependencias Spring JDBC y driver MariaDB agregadas en pom.xml.
- Configuración de perfiles: default (in-memory), jdbc (MariaDB real).
- `JdbcConfig.java` creado bajo perfil `jdbc`.
- `JdbcInfrastructureIT` condicionado a `FALTAS_DB_URL` — no rompe build normal.
- Todos los repositorios InMemory siguen activos sin modificar.
- **Build: 908/908 tests passing (JdbcInfrastructureIT omitido sin MariaDB). BUILD SUCCESS.**

### Slice 8F-1 — Plantillas operativas, variables documentales y motor de combinación — CERRADO

**Estado: CERRADO** | Build: 954/954 tests | Fecha: 2026-07-02

Artefactos Java entregados:
- 4 enums: `FormatoPlantillaContenido`, `EstadoRedaccionDocumento`, `DocumentoVariableNamespace`, `TipoDatoVariableDocumento`
- 4 entidades: `FalDocumentoPlantillaDefault`, `FalDocumentoPlantillaContenido`, `FalDocumentoRedaccion`, `DocumentoVariableDefinicion`
- 4 excepciones de dominio
- 6 repositorios (3 interfaces + 3 in-memory)
- 4 clases de combinación: Registry, Service, Resultado, ContextBuilder
- 2 servicios: `DocumentoPlantillaDefaultService`, `DocumentoRedaccionService`
- 1 command + 1 response
- 46 tests nuevos
- Documentación: `spec-as-source/104-plantillas-redaccion-combinacion-documentos.md`

### Estado general

- **JDBC real: PAUSADO** — no hay repositorios JDBC de dominio todavía.
- **MariaDB real: NO activa** — solo infraestructura disponible bajo perfil `jdbc`.
- **Angular: no activo** en el ciclo actual.
- **Etapa actual: 8F** (plantillas, redacción, combinación, corralón).

---

## 3. Próximo slice pendiente

**8F-2 — Context builder completo + plantillas mock por caso de uso**

Objetivos de 8F-2:
- Completar `DocumentoVariableContextBuilder` con todos los namespaces reales.
- Crear plantillas mock operativas por tipo de documento / caso de uso.
- Cubrir al menos los casos del graph demo.

Pendientes obligatorios antes de avanzar a JDBC de dominio (desde 9-2):
1. 8F-2: Context builder completo + plantillas mock
2. 8F-3: Confirmación de redacción + PDF final (stub/mock)
3. 8F-4: Plantillas default para todos los casos del graph demo
4. Actas mock completas con snapshots reales
5. Reset/recreación in-memory dev/test

Solo después de cerrar todo lo anterior avanzar con:
- 9-2: `JdbcDocumentoPlantillaRepository`
- 9-N: Repositorios JDBC del resto del dominio

---

## 4. Guardrails activos

### JDBC / persistencia

- **No repositorios JDBC todavía** — InMemory* permanecen activos.
- **No MariaDB real en tests normales** — requiere `FALTAS_DB_URL` definida.
- **No scripts SQL** de tablas ni seeds en este ciclo.
- **No tablas ni seeds para enums cerrados** — los enums ya están en Java y no necesitan DDL separado.
- **No Flyway / Liquibase** — decisión pendiente para slices posteriores.

### Tecnología

- **No JPA / Hibernate** — prohibidos.
- **No Angular** en este ciclo.
- **No PDF / storage real en 8F** — se usa stub/mock.
- **No UX editor** de plantillas en este ciclo.
- **No `D3_DOCUMENTAL`** — no existe como bloque.
- **No `PAGCON`** — prohibido, evento obsoleto.
- **No `APELAC`** — usar `APEPRE`.
- **No `ACTCER`** — el cierre usa `CIERRA`.
- **No `DRVEXT`** — prohibido.

### Dominio

- No inventar eventos.
- No inventar bloques.
- No inventar estados centrales.
- No usar strings libres para dominio.
- `Acta` es la unidad principal (no `IdCausa`).
- `ActaEvento` es append-only.
- `ActaSnapshot` es proyección operativa derivada.
- No se reabre editando estado: se reingresa por evento.

---

## 5. Decisiones de documentos (cerradas)

- Plantilla no genera PDF directo.
- Se genera redacción editable primero.
- PDF solo al confirmar/enviar.
- No guardar PDFs intermedios.
- `storageKey` / `hashDocu` / `fhGeneracion` solo al PDF final.
- `FalDocumentoRedaccion` es la entidad de trabajo editable antes de emisión.
- `FalDocumentoPlantillaDefault` define la plantilla por defecto para cada tipo de documento.

---

## 6. Archivos incluidos en este ZIP

### Documentación

- `docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`
- `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`
- `backend/api-faltas-core/docs/spec-as-source/` (carpeta completa)

### Configuración backend

- `backend/api-faltas-core/pom.xml`
- `backend/api-faltas-core/src/main/resources/application.yml`
- `backend/api-faltas-core/src/main/resources/application-jdbc.yml`

### Código Java

- `backend/api-faltas-core/src/main/java/` (carpeta completa)

### Tests

- `backend/api-faltas-core/src/test/java/` (carpeta completa)

---

## 7. Instrucción para el próximo chat

Subir este ZIP junto con el reporte final del slice 8F-1 (ya cerrado, ver sección 2).

Al iniciar el chat, decir:

> Retomo el backend Faltas. Build actual: 954/954 tests passing.
> Slices cerrados: 8C, 8C-6E, 9-0, 9-1, 8F-1.
> JDBC real pausado. Próximo slice: 8F-2.

---

## 8. Reporte final Slice 8F-1

**Estado: CERRADO** | Build: 954 tests OK | Fecha: 2026-07-02

Implementada la capa de plantillas operativas, variables documentales y motor de combinación
necesaria antes de avanzar con JDBC/MariaDB real.

Entidades principales:
- `FalDocumentoPlantillaDefault` — plantilla por defecto por tipo de documento
- `FalDocumentoPlantillaContenido` — contenido versionado de la plantilla
- `FalDocumentoRedaccion` — redacción editable generada para un acta
- `DocumentoVariableDefinicion` — catálogo de variables disponibles por namespace

Motor de combinación:
- `DocumentoVariableRegistry` — registro de variables disponibles
- `DocumentoVariableContextBuilder` — construye el contexto de variables para una plantilla
- `DocumentoCombinacionService` — combina plantilla + contexto → redacción
- `DocumentoCombinacionResultado` — resultado de la combinación

46 tests nuevos. BUILD SUCCESS.
