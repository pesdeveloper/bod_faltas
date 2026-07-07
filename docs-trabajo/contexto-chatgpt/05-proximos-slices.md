# 05 — Próximos slices

**Generado:** 2026-07-03

---

## 8F-5 — Reset dev/test in-memory [EN PROGRESO, sin commitear]

**Estado en working tree:** Implementado. Pendiente de build y commit.
**Tests según spec:** 1437 (vs 1419 en HEAD main / 8F-4C).

### Objetivo

Endpoint dev/test para destruir y recrear todos los repositorios in-memory con el dataset mock cargado.
Útil para tests E2E y desarrollo frontend. Idempotente. No productivo.

### Implementación (en working tree)

**Nuevos archivos (untracked):**

| Clase | Paquete | Rol |
|-------|---------|-----|
| `ResettableInMemoryRepository` | `repository.memory` | Interfaz: `reset()`, `nombre()`, `size()` |
| `DevInMemoryResetService` | `application.demo` | Orquesta reset completo + resembrado plantillas |
| `DevResetController` | `web` | Expone `POST /demo/dev/reset` |
| `DevResetResponse` | `web.dto` | Record con resumen del reset |

**Archivos modificados:**
- Los 24 `InMemory*Repository` implementan `ResettableInMemoryRepository`
- `application.yml`: property `faltas.demo.reset.enabled` documentada

### Endpoint

```
POST /demo/dev/reset
```

- Disponible solo cuando `faltas.demo.reset.enabled=true`
- Devuelve 404 si está deshabilitado
- Property por defecto: `faltas.demo.reset.enabled=false`

### Response: DevResetResponse

Record con campos del resumen del reset:
- repositorios reseteados y su `size()` antes/después
- plantillas resembradas
- timestamp

### Tests creados (en working tree)

| Suite | Tests | Descripción |
|-------|-------|-------------|
| `DevInMemoryResetServiceTest` | 13 | Servicio de reset unitario |
| `DevResetControllerIT` | 14 | IT con `@TestPropertySource` reset habilitado |
| `DevResetDisabledIT` | 1 | IT: 404 cuando deshabilitado |
| `DevResetGuardrailTest` | 5 | Guardrails estáticos |

### Guardrails cumplidos

- No JDBC, no MariaDB, no SQL, no JPA/Hibernate
- No escritura de archivos, no storage real
- No endpoint reset productivo fuera de `/demo`
- Idempotente: ejecutar N veces produce el mismo estado
- `docs/spec-as-source` en raíz: False (confirmado)

---

## 8F-6 — Auditoría frontend-ready [PENDIENTE]

**Estado:** No iniciado. Próximo slice después de commitear 8F-5.

### Objetivo

Revisar y auditar que todos los endpoints core estén listos para ser consumidos desde Angular:

- Revisar payloads de respuesta: ¿son completos? ¿campos redundantes o faltantes?
- Errores bien tipados (tipos de error, mensajes legibles para UI)
- CORS si aplica
- Flujos end-to-end navegables desde el frontend en desarrollo
- Documentación de endpoints para equipo frontend
- Preparar integración UI sin necesariamente iniciar Angular todavía

### No incluye

- No implementar Angular en este slice
- No implementar JDBC/MariaDB
- No cambios de dominio

---

## Pendientes futuros (post 8F-6)

### Slice 9 — JDBC / MariaDB (cuando corresponda)

Antes de avanzar con JDBC real, cerrar toda la etapa 8F.

Próximos repositorios JDBC propuestos (en orden):
- 9-2: `JdbcDocumentoPlantillaRepository`
- 9-N: Repositorios JDBC del resto del dominio

**Infraestructura disponible:** perfil `jdbc` con `JdbcConfig.java` bajo `@Profile("jdbc")`.
No activar sin reconciliar el delta del modelo (ver `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`).

### Gaps técnicos conocidos (no bloqueantes para 8F-5/8F-6)

- FirmaReq completa (workflow de requisito de firma institucional con firmante asignado)
- Emisión formal numerada (`FalDocumento.marcarEmitido` / estado EMITIDO)
- Acta archivada (`SituacionAdministrativa.ARCHIVADA` / `BloqueActual.ARCH`)
- `REINGRESO_PARA_CIERRE` (bloqueado, reservado para slice futuro)
- `FalObservacion` / `fal_observacion` para observaciones de PAGAPR (Slice 9/JDBC)

---

## Secuencia recomendada

```
[ACTUAL] 8F-4C (main, 1419 tests) — CERRADO
    ↓
8F-5 (working tree, sin commitear) — build + commit pendiente
    ↓
8F-6 — auditoría frontend-ready
    ↓
(cuando corresponda) Slice 9 — JDBC/MariaDB incremental
```
