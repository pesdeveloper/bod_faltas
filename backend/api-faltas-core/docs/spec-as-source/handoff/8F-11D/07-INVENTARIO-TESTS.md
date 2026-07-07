# 07 — Inventario de Tests

Rutas relativas al repositorio. Total post-8F-11D: **1755 tests**, 0 failures.

---

## Tests nuevos de 8F-11D

### TarifarioValorizacionTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/TarifarioValorizacionTest.java`
- **Estado:** NUEVO (`??`)
- **Casos aprox.:** ~60-80 tests
- **Invariantes cubiertas:**
  - Creación de tarifario con control de no-superposición
  - findVigentes y findUltimoVigente por tipo y fecha
  - Desactivar tarifario
  - Alta de medida preventiva con versiones
  - Asociación y desasociación artículo-medida
  - Imputación de artículos a acta y baja lógica
  - calcularBasePreliminar: items generados automáticamente
  - confirmar: garantía de una-vigente por acta+tipo; reemplaza anterior
  - anular valorización
  - agregarItemManual con motivo
  - seleccionarOperativa: prioridad CONDENA > PAGO_VOLUNTARIO > INFRACCION_BASE
  - ciclo completo: imputar → calcular → confirmar

### OptimisticLockingTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/OptimisticLockingTest.java`
- **Estado:** NUEVO (`??`)
- **Casos aprox.:** ~10-15 tests
- **Invariantes cubiertas:**
  - versionRow se incrementa en cada save de FalActaValorizacion
  - ConcurrenciaConflictoException al intentar guardar con versionRow desactualizado
  - Segunda lectura siempre obtiene versionRow actualizado

---

## Tests nuevos de slices anteriores (en el mismo working tree)

### PersonaIntegracionTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/PersonaIntegracionTest.java`
- **Estado:** NUEVO (`??`) — 8F-11C

### FalPersonaTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/FalPersonaTest.java`
- **Estado:** NUEVO (`??`) — 8F-11C

### FalPersonaDomicilioTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/FalPersonaDomicilioTest.java`
- **Estado:** NUEVO (`??`) — 8F-11C

### IdentidadesLongTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/IdentidadesLongTest.java`
- **Estado:** NUEVO (`??`) — 8F-11B

### OrigenFirmezaCondenaCodigoTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/OrigenFirmezaCondenaCodigoTest.java`
- **Estado:** NUEVO (`??`) — 8F-11B

### ResultadoFinalActaCodigoTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/ResultadoFinalActaCodigoTest.java`
- **Estado:** NUEVO (`??`) — 8F-11B

### TipoActaCodigoTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/TipoActaCodigoTest.java`
- **Estado:** NUEVO (`??`) — 8F-11B

### AuditoriaTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/AuditoriaTest.java`
- **Estado:** NUEVO (`??`) — 8F-11B

### DevInMemoryResetServiceTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/DevInMemoryResetServiceTest.java`
- **Estado:** NUEVO (`??`) — 8F-11 (reset/dev)

### DevResetGuardrailTest
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/DevResetGuardrailTest.java`
- **Estado:** NUEVO (`??`)

---

## Tests modificados relevantes

### Tests funcionales de flujo (modificados en ` M`)
- ActaFlujoDocumentalFuncionalTest.java
- ActaFlujoGestionExternaFuncionalTest.java
- ActaFlujoPagoCondenaFuncionalTest.java
- ActaFlujoPagoVoluntarioFuncionalTest.java
- ActaResultadoFirmaInfractorTest.java
- (varios otros flujos en ` M`)

Estos tests cubren el flujo completo del expediente y fueron actualizados para adaptarse a los cambios de 8F-11B/C/D.

---

## Tests de contracts/demo (nuevos)

### DemoActaDetalleContractTest (~35 tests)
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/web/DemoActaDetalleContractTest.java`
- **Estado:** NUEVO (`??`)

### DemoContractTest (~14 tests)
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/web/DemoContractTest.java`
- **Estado:** NUEVO (`??`)

### DemoHealthContractTest (~16 tests)
- **Ruta:** `backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/web/DemoHealthContractTest.java`
- **Estado:** NUEVO (`??`)

### DevResetControllerIT / DevResetDisabledIT
- **Estado:** NUEVO (`??`)

---

## Resumen de conteo de tests

| Baseline | Post-8F-11D |
|---|---|
| 1660 (post 8F-11C) | **1755** |
| Tests de 8F-11D | ~95 |
