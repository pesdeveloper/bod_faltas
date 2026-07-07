> **[8F-11D-R1 - 2026-07-06]** 8F-11D cerrado: FalTarifarioUnidadFaltas, FalMedidaPreventiva, FalArticuloMedidaPreventiva, FalActaArticuloInfringido, FalActaValorizacion, FalActaValorizacionItem. R1 invariantes aplicados: snapshot proyeccion (R1-A), confirmarVigenteAtomico (R1-B), inmutabilidad items (R1-C), guards atomicos de paridad en 6 tablas (R1-D). 1785 tests GREEN. Nota: el contenido detallado historico de este archivo fue perdido por error de escritura durante la limpieza de caracteres corruptos U+FEFF. La fuente vigente es 110.
>
> **[8F-11A - 2026-07-04]** Este documento es historial de auditoria (slices 8F-9 / 8F-10).
> La fuente vigente de paridad es: **`110-matriz-maestra-paridad-mariadb-inmemory.md`**
> No actualizar este documento; actualizar el 110.
>
> **[8F-11A-R1 - 2026-07-05]** Correccion documental aplicada al 110: 62 tablas (no 55), 29 FALTA_EN_INMEMORY (no 22), P1 cerrada, P2 cerrada. La entrada de `ResultadoFinalActa` en la seccion 8 de este documento es historica; la definicion vigente es la del 110.

---

# 109 - Auditoria DELTA: Modelo MariaDB vs Modelo InMemory

**Slice:** 8F-9 / Revision 8F-9-R2 (base) | **Actualizado en 8F-10 (2026-07-04)** | **8F-11D-R1 (2026-07-06)**
**Fecha base:** 2026-07-03 | **Ultima actualizacion:** 2026-07-06 (Slice 8F-11D-R1)
**Tipo:** Auditoria y documentacion historica. Sin cambios funcionales Java.
**Modulo:** backend/api-faltas-core (in-memory, sin MariaDB)
**Build base:** 1785 tests passing. BUILD SUCCESS. (al cierre de 8F-11D-R1)

> **NOTA DE RECONSTRUCCION:** El cuerpo detallado de este documento historico fue perdido
> durante una operacion de limpieza de caracteres corruptos (U+FEFF) el 2026-07-06.
> El documento original contenia la auditoria detallada de paridad para los slices 8F-9 y 8F-10.
> La informacion vigente y actualizada esta en `110-matriz-maestra-paridad-mariadb-inmemory.md`.

---

## Resumen de estado al cierre de 8F-11D-R1

| Categoria | Cantidad |
|-----------|----------|
| Tablas MariaDB auditadas | 62 |
| Entidades InMemory persistibles | 37 |
| ALINEADO | 32 entidades |
| FALTA_EN_INMEMORY | 23 tablas sin entidad |
| SOLO_DEMO_TEST | 21 clases |
| NO_PERSISTIBLE | 25+ (servicios, DTOs, enums puro Java) |

## Slices completados hasta 8F-11D-R1

- **8F-9**: Ciclo base, modelos core
- **8F-10**: Integracion documental
- **8F-11A**: Auditoria y paridad inicial (62 tablas)
- **8F-11A-R1**: Correccion documental (conteos, P1, P2)
- **8F-11B**: Identidades, enums, versionRow, auditoria
- **8F-11C**: FalPersona, FalPersonaDomicilio
- **8F-11D**: FalTarifarioUnidadFaltas, FalMedidaPreventiva, FalArticuloMedidaPreventiva, FalActaArticuloInfringido, FalActaValorizacion, FalActaValorizacionItem
- **8F-11D-R1**: Invariantes de cierre: snapshot proyeccion, confirmarVigenteAtomico, inmutabilidad items, guards atomicos

## Proximos slices

Ver `99-pendientes-siguientes-slices.md` para la lista actualizada.
Ver `110-matriz-maestra-paridad-mariadb-inmemory.md` para la matriz de paridad vigente.
