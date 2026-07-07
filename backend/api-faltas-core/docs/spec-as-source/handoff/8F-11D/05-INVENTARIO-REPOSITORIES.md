# 05 — Inventario de Repositories

Rutas relativas al repositorio. Separados en ports (interfaces) e implementaciones InMemory.

---

## Repositories de 8F-11D — Ports (interfaces)

### TarifarioUnidadFaltasRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/TarifarioUnidadFaltasRepository.java`
- **Estado:** NUEVO (`??`)
- **Entidad:** FalTarifarioUnidadFaltas
- **PK:** Long id
- **Queries:** nextId(), save(), findById(Long), findAll(), findByTipoUnidad(TipoUnidadFaltas), findVigentes(tipo, fecha), findUltimoVigente(tipo, fecha)
- **Unicidades:** no superposición de rangos activos por tipo (enforced en TarifarioService, no en repo)
- **Aislamiento:** InMemory devuelve copias defensivas

### MedidaPreventivaRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/MedidaPreventivaRepository.java`
- **Estado:** NUEVO (`??`)
- **Entidad:** FalMedidaPreventiva
- **PK:** Long id
- **Queries:** nextId(), save(), findById(Long), findByCodigo(String), findAll(), findActivos()

### ArticuloMedidaPreventivaRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/ArticuloMedidaPreventivaRepository.java`
- **Estado:** NUEVO (`??`)
- **Entidad:** FalArticuloMedidaPreventiva
- **PK:** ArticuloMedidaPreventivaId (compuesta: articuloId + medidaPreventivaId)
- **Queries:** save(), findById(ArticuloMedidaPreventivaId), findByArticuloId(Long), findByMedidaPreventivaId(Long), findAll()
- **Unicidades:** PK compuesta, sin ID sintético

### ActaArticuloInfringidoRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/ActaArticuloInfringidoRepository.java`
- **Estado:** NUEVO (`??`)
- **Entidad:** FalActaArticuloInfringido
- **PK:** Long id
- **Queries:** nextId(), save(), findById(Long), findByActaId(Long), findActivosByActaId(Long), findAll()

### ActaValorizacionRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/ActaValorizacionRepository.java`
- **Estado:** NUEVO (`??`)
- **Entidad:** FalActaValorizacion
- **PK:** Long id
- **Queries:** nextId(), save() (con optimistic locking), findById(Long), findByActaId(Long), findByActaIdAndTipo(Long, TipoValorizacionActa), findVigenteByActaIdAndTipo(Long, TipoValorizacionActa), findAll()
- **Optimistic locking:** save() en InMemory: si id ya existe, compara versionRow; si no coincide lanza ConcurrenciaConflictoException; si coincide incrementa versionRow.

### ActaValorizacionItemRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/ActaValorizacionItemRepository.java`
- **Estado:** NUEVO (`??`)
- **Entidad:** FalActaValorizacionItem
- **PK:** Long id
- **Queries:** nextId(), save(), findById(Long), findByValorizacionId(Long), findAll()

---

## Repositories de 8F-11D — Implementaciones InMemory

### InMemoryTarifarioUnidadFaltasRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/memory/InMemoryTarifarioUnidadFaltasRepository.java`
- **Estado:** NUEVO (`??`)
- **Implementa:** TarifarioUnidadFaltasRepository
- **Store:** ConcurrentHashMap<Long, FalTarifarioUnidadFaltas> + AtomicLong
- **Aislamiento:** devuelve copias (.copia())

### InMemoryMedidaPreventivaRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/memory/InMemoryMedidaPreventivaRepository.java`
- **Estado:** NUEVO (`??`)
- **Implementa:** MedidaPreventivaRepository

### InMemoryArticuloMedidaPreventivaRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/memory/InMemoryArticuloMedidaPreventivaRepository.java`
- **Estado:** NUEVO (`??`)
- **Implementa:** ArticuloMedidaPreventivaRepository
- **PK:** ArticuloMedidaPreventivaId (key compuesta en HashMap)

### InMemoryActaArticuloInfringidoRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/memory/InMemoryActaArticuloInfringidoRepository.java`
- **Estado:** NUEVO (`??`)
- **Implementa:** ActaArticuloInfringidoRepository

### InMemoryActaValorizacionRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/memory/InMemoryActaValorizacionRepository.java`
- **Estado:** NUEVO (`??`)
- **Implementa:** ActaValorizacionRepository
- **Optimistic locking real:** sincronizado, compara versionRow, incrementa en update

### InMemoryActaValorizacionItemRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/memory/InMemoryActaValorizacionItemRepository.java`
- **Estado:** NUEVO (`??`)
- **Implementa:** ActaValorizacionItemRepository

---

## ResettableInMemoryRepository
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/repository/memory/ResettableInMemoryRepository.java`
- **Estado:** NUEVO (`??`) — solo demo/dev
- **Uso:** Todos los InMemory repos que implementan esta interfaz pueden ser reseteados por DevInMemoryResetService.
