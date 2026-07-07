# 01 — Estado actual del proyecto Faltas

**Generado:** 2026-07-03

---

## Repositorio

- **Path:** `S:\Source\Repos\Bod-Faltas`
- **Módulo activo:** `backend/api-faltas-core`
- **Spec viva:** `backend/api-faltas-core/docs/spec-as-source/`

### Nota sobre eliminaciones intencionales

- `spec/` (raíz del repo) — eliminado intencionalmente en 8F historia.
- `sql/informix/base/*` — eliminado intencionalmente. No tratar como error.

---

## Estado Git

| Campo | Valor |
|-------|-------|
| Rama | `main` |
| HEAD (committed) | `39e2a76` — `chore: quitar logs locales de test` |
| Sincronizado | `origin/main` OK |
| Working tree | 8F-5 implementado pero NO commiteado (ver abajo) |

### Commits recientes

```
39e2a76 (HEAD -> main, origin/main) chore: quitar logs locales de test
716c490 test: agregar pruebas funcionales completas 8F-4C
7036e5d test: agregar pruebas funcionales completas 8F-4C
11f755c chore: limpiar spec historica y sql informix obsoleto
```

### Working tree al 2026-07-03

Los archivos de 8F-5 están implementados en el working tree pero sin commitear:

**Archivos nuevos (untracked ??)**
- `DevInMemoryResetService.java`
- `ResettableInMemoryRepository.java`
- `DevResetController.java`
- `DevResetResponse.java`
- `DevInMemoryResetServiceTest.java`
- `DevResetControllerIT.java`
- `DevResetDisabledIT.java`
- `DevResetGuardrailTest.java`

**Archivos modificados (M)**
- 25 `InMemory*Repository` (implementan `ResettableInMemoryRepository`)
- 4 archivos de spec en `docs/spec-as-source/`
- `application.yml`

---

## Slices cerrados (commiteados en main)

| Slice | Descripción |
|-------|-------------|
| 8F-1 | Plantillas operativas, variables documentales, motor de combinación |
| 8F-2 | Context builder completo + plantillas mock por caso de uso |
| 8F-3 | Confirmación de redacción + PDF mock (DocumentoPdfMockRenderer) |
| 8F-4 | Graph demo documental (DocumentoGraphDemoService, GET /demo/documentos/graph) |
| 8F-4B | Dataset funcional: 25 actas mock del dominio |
| 8F-4B-R1 | Cierre de pendientes: 6 actas nuevas (ACT-026 a ACT-031) → 31 actas totales |
| 8F-4C | 11 suites funcionales completas + CasoUsoFuncionalRunner |

**Slices anteriores también cerrados (histórico):**
Slice 1 (ciclo base), Slice 2 (pago voluntario), Slice 3A (fallo), Slice 3B (apelación),
Slice 3C (resolución apelación), Slice 4 (firmeza condena), Slice 5 (pago condena),
Slice 6A/6B/6C/6D (gestión externa), Slice 7A/7B/7C (bloqueantes materiales),
Slice 8A/8B/8C (firmantes, documentos, firma), 8C-6E (auditoría pre-JDBC).

---

## Estado de tests (al último commit main)

- **Build:** 1419 tests, 0 failures, 0 errors, 0 skipped. **BUILD SUCCESS.**
- Suites funcionales del slice 8F-4C: 11 suites + 1 cobertura total.
- Todas las pruebas pasan sin MariaDB, sin JDBC real, in-memory puro.

---

## Estado del dataset funcional

- **31 actas mock funcionales** cubriendo todos los casos de uso implementados.
- Dataset canónico en `DatasetFuncionalDominioCatalog.java`.
- Cada acta declara: bloque, situación, resultado final, bandeja, documentos esperados, eventos esperados.
- Endpoint de consulta: `GET /demo/actas/dataset-funcional`

---

## Slice en progreso (working tree, NO commiteado)

**8F-5 — Endpoint dev/test de reset y recreación in-memory**

- Implementado en working tree. Pendiente de build y commit.
- Spec dice: 1437 tests al cerrar (vs 1419 en HEAD main).
- Ver `05-proximos-slices.md` para detalles.

---

## Próximo slice confirmado

**8F-6 — Auditoría frontend-ready de endpoints, payloads y flujo demo.**

---

## Estado de tecnologías

| Tecnología | Estado |
|------------|--------|
| In-memory (InMemory*Repository) | Activo y único mecanismo de persistencia |
| JDBC / MariaDB | Pausado — infraestructura disponible bajo perfil `jdbc` pero sin uso |
| JPA / Hibernate | Prohibido |
| Angular / frontend | No activo en este ciclo |
| PDF real / storage real | No activo — se usa mock (`DocumentoPdfMockRenderer`) |
| Flyway / Liquibase | No activo |
