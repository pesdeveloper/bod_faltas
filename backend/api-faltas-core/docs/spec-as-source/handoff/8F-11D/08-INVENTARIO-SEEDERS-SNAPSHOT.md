# 08 — Inventario de Seeders y Snapshot

Rutas relativas al repositorio.

---

## Seeders nuevos en 8F-11D

### TarifarioMedidaMockSeeder
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/demo/TarifarioMedidaMockSeeder.java`
- **Estado:** NUEVO (`??`)
- **Tipo:** @Component con @PostConstruct
- **Finalidad:** Siembra tarifarios, medidas preventivas y relaciones artículo-medida para demo/test.
- **Datos que siembra:**
  - Tarifarios (IDs 1-7):
    - SALARIO histórico (inactivo, ID 1)
    - SALARIO vigente (activo, ID 2)
    - UNIDAD_FIJA histórico (inactivo, ID 3)
    - UNIDAD_FIJA vigente (activo, ID 4)
    - MONTO vigente (activo, ID 5)
    - SALARIO adicional sin superposición (solo test, inactivo: ID 6)
  - Medidas preventivas (IDs 1-7):
    - RETLIC v1 activa
    - RETROD v1 activa
    - SECVEH v1 activa
    - CLAUSURA v1 activa
    - CLAUSURA v2 activa
  - Relaciones artículo-medida: IDs 1 y 2 del seed de artículos normativos.
- **Dependencias:** TarifarioUnidadFaltasRepository, MedidaPreventivaRepository, ArticuloMedidaPreventivaRepository
- **Compatibilidad con pagos actuales:** No hay conflicto. Los pagos usan FalPagoVoluntario/FalPagoCondena (D2 cerrada).

---

## Seeders preexistentes (relevantes para el contexto)

### PlantillasMockSeeder
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/demo/PlantillasMockSeeder.java`
- **Estado:** MODIFICADO (` M`)
- **Finalidad:** Siembra plantillas documentales mock para demo.

---

## Factories de datos demo

### GraphDemoActaFactory
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/demo/GraphDemoActaFactory.java`
- **Estado:** MODIFICADO (` M`)
- **Finalidad:** Factory de acta para el endpoint graph demo.

### DatasetFuncionalDominioCatalog
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/demo/DatasetFuncionalDominioCatalog.java`
- **Estado:** MODIFICADO (` M`)
- **Finalidad:** Catálogo declarativo de 31 actas mock para tests funcionales.

---

## FalActaSnapshot — extensión por 8F-11D

- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalActaSnapshot.java`
- **Estado:** MODIFICADO (` M`)
- **Campos nuevos:**
  - `valorizacionOperativaId` Long NULL — ID de la valorización vigente operativa
  - `estadoValorizacionOperativa` EstadoValorizacion NULL
  - `tipoValorizacionOperativa` TipoValorizacionActa NULL
  - `montoOperativoVigente` BigDecimal NULL — monto económico del acta
  - `siMontoConfirmado` boolean — true si la valorización está confirmada

### SnapshotRecalculador (recalculación)
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/snapshot/SnapshotRecalculador.java`
- **Estado:** MODIFICADO (` M`)
- **Cambios en 8F-11D:** usa ValorizacionService.seleccionarOperativa(actaId) para proyectar la valorización operativa vigente al snapshot.

---

## Compatibilidad con pagos actuales

Los pagos (FalPagoVoluntario / FalPagoCondena) no se ven afectados por 8F-11D.
La decisión D2 (cerrada) los mapea a fal_acta_obligacion_pago — pendiente en Slice 8F-11H.
La valorización es la fuente económica; los pagos registran la ejecución, no el monto original.
