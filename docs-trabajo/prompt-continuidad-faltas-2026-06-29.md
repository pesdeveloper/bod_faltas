# Prompt de continuidad - Direccion de Faltas

**Fecha:** 2026-06-29
**Para pegar en:** chat nuevo o agente nuevo
**Repo:** S:\Source\Repos\Bod-Faltas
**Modulo activo:** backend\api-faltas-core (productivo en memoria)
**Build actual:** 227/227 tests passing, 0 failures

---

## Modelo recomendado

- Cambios funcionales importantes: Sonnet 4.6, Thinking ON, Effort High, Context 200K
- Auditorias, validaciones, documentacion: Sonnet 4.6, Thinking ON, Effort Medium
- No usar Composer/Fast para cambios funcionales grandes

---

## Reglas de trabajo con Cursor

- Trabajar con slices minimos y objetivo definido.
- Primero diagnosticar. Clasificar hallazgos antes de proponer cambios.
- Proponer slice minimo antes de implementar.
- Ejecutar build/tests al cerrar cada slice.
- No reescribir artefactos de continuidad sin autorizacion explicita.
- Contexto minimo: cargar solo los archivos del modulo afectado.
- No abrir todo el repo.

---

## Estado del modulo productivo: api-faltas-core

Ruta: `backend/api-faltas-core`

### Slices completados (en orden)

| Slice | Descripcion | Tests |
|-------|-------------|-------|
| 1 | Ciclo base: labrar, captura, enriquecer, documento, firma, notificacion | ok |
| 2 | Pago voluntario completo (PVOLS/PVOINF/PVOCNF/PVOOBS/PVOVNC) | ok |
| 3A | Fallo absolutorio y condenatorio minimo | ok |
| 3B | Apelacion presentada (APEPRE) | ok |
| 3C | Resolucion de apelacion (APERAZ / APEABS) | ok |
| 4 | Firmeza de condena (PLAVNC + CONFIR / CONDENA_FIRME) | ok |
| Micro | Cierre semantico: guardrails, eliminacion PAGCON, prohibicion D3 | ok |
| 5 | Pago de condena (PCOINF / PCOCNF / PCOOBS / CONDENA_FIRME_PAGADA) | ok |
| 6A | Gestion externa: derivacion (EXTDER) | ok |
| 6B | Gestion externa: reingreso (EXTRET) | ok |

**Total: 227 tests passing. BUILD SUCCESS.**

---

## Slice 6B - Detalle (ultimo slice cerrado, 2026-06-29)

### Que se implemento

- `ReingresarDesdeGestionExternaCommand` (record, campo nullable `resultadoGestionExterna`)
- `GestionExternaService.reingresar()` con validaciones completas
- `GestionExternaRepository.buscarPorHistorico(actaId)` agregado al contrato
- `InMemoryGestionExternaRepository` implementa `buscarPorHistorico`
- `GestionExternaController`: endpoint `POST /api/faltas/actas/{id}/gestion-externa/reingresar`
- `ReingresarDesdeGestionExternaRequest` DTO en web/dto

### Flujo implementado

`FalActa[bloqueActual=GEXT, situacion=EN_GESTION_EXTERNA]`
`+ FalGestionExterna[siActiva=true, estado=DERIVADA]`
`-- reingresar(modo, motivo, resultadoOpcional) -->`
`EXTRET registrado`
`FalGestionExterna[siActiva=false, estado=REINGRESADA, fechaReingreso, modo, motivo]`
`FalActa[bloqueActual=ANAL, situacion=ACTIVA]`
`Snapshot recalculado`

### Modos habilitados en Slice 6B

| Modo | Estado |
|------|--------|
| `REINGRESAR_A_ANALISIS` | Habilitado |
| `REINGRESAR_A_PAGO_CONDENA` | Habilitado (requiere resultadoFinal == CONDENA_FIRME) |
| `REINGRESAR_A_CIERRE` | Bloqueado -- PrecondicionVioladaException -- reservado |
| `REINGRESAR_A_ARCHIVO` | Bloqueado -- PrecondicionVioladaException -- reservado |
| `NO_APLICA` | Bloqueado -- invalido en reingreso |

### Separacion semantica del repositorio

- `buscarActiva(actaId)`: uso exclusivo del servicio para logica de dominio
- `buscarPorHistorico(actaId)`: solo para auditoria, tests y consulta post-ciclo; nunca en logica de dominio

### resultadoGestionExterna -- campo nullable intencional

`resultadoGestionExterna` en `ReingresarDesdeGestionExternaCommand` es nullable por diseno:
- No todos los reingresos tienen resultado externo documentado
- Si presente, se persiste en `FalGestionExterna`
- Si ausente (null), el campo queda con valor anterior (SIN_RESULTADO del constructor)
- No produce ambiguedad de dominio
- Documentado en Javadoc del command y en 03-comandos-precondiciones-efectos.md

### Tests nuevos Slice 6B (31 tests)

Nested classes en `GestionExternaTest`:

- `ReingresoFeliz` (3): a analisis, a pago condena, con resultado informado
- `EventosReingreso` (4): EXTRET registrado; PAGAPR, CIERRA, EXTDER adicional NO registrados
- `EstadoGestionExternaTrasReingreso` (5): siActiva=false, REINGRESADA, modo, motivo, fecha
- `MutacionActaReingreso` (4): situacion=ACTIVA, bloque=ANAL, snapshot recalculado x2
- `PrecondicionesReingreso` (10): todos los rechazos de precondicion
- `GuardrailsSlice6B` (5): PAGAPR no emitido, EXTRET correcto, DRVEXT prohibido, in-memory, trazabilidad

### Correcciones de auditoria aplicadas post-slice (sin cambio funcional)

1. `GestionExternaTest.java`: typo `EstadoGestionExternaTraReingreso` -> `EstadoGestionExternaTrasReingreso`
2. `GestionExternaTest.java`: Javadoc de clase actualizado (ya no dice 'No implementa reingreso')
3. `ModoReingresoGestionExterna.java`: Javadoc enum actualizado -- 6B implemento REINGRESAR_A_ANALISIS y REINGRESAR_A_PAGO_CONDENA
4. `docs/spec-as-source/06-tests-core.md`: sincronizado con el mismo typo fix

---

## Decisiones cerradas (no reabrir sin justificacion explicita)

| Decision | Razon |
|----------|-------|
| DRVEXT NO existe como evento | Prohibido, eliminado |
| PAGCON NO existe | Prohibido; usar PCOINF/PCOCNF/PCOOBS |
| ACTCER NO existe | Prohibido; el cierre usa CIERRA |
| APELAC NO existe como evento | Usar APEPRE |
| D3_DOCUMENTAL NO existe como bloque | Eliminado, prohibido |
| Ambos modos de reingreso retornan a ANAL/ACTIVA | Definicion de dominio cerrada |
| buscarPorHistorico solo para consulta post-ciclo | No invertir semantica |
| resultadoGestionExterna nullable intencional | No hacer obligatorio |
| REINGRESAR_A_CIERRE y REINGRESAR_A_ARCHIVO bloqueados | Reservados, no implementar sin definicion |
| PAGAPR existe en enum pero no se emite en 6B | Reservado Slice 6C+ |
| Snapshot es derivado, no fuente de verdad | Invariante del modelo |

---

## Pendientes explicitos para Slice 6C y posteriores

### Slice 6C+: pago apremio -- NO implementar antes de definicion

- `RegistrarPagoApremioCommand` -> evento `PAGAPR`
- Requiere definicion: quien registra, como confirma, si cierra o no, si genera resultado externo
- `PAGAPR` existe en `TipoEventoActa` pero NO se emite todavia
- NO implementar sin slice bien definido y autorizado

### Slice 6D+: reingreso a cierre / archivo -- NO implementar antes de definicion

- `REINGRESAR_A_CIERRE`: bloqueado con PrecondicionVioladaException
- `REINGRESAR_A_ARCHIVO`: bloqueado con PrecondicionVioladaException
- Ambos modos existen en `ModoReingresoGestionExterna` pero sin logica
- Requiere definir que bloque, situacion y snapshot corresponde en cada caso

### Slice 7: motor real de bloqueantes

- Reemplazar `NoOpBloqueantesMaterialesChecker` por implementacion real
- El contrato `BloqueantesMaterialesChecker` no cambia
- No tocar servicios de aplicacion al hacerlo

### Slice 9: persistencia MariaDB/JDBC

- Reemplazar todos los `InMemory*Repository` por implementaciones con `JdbcClient`
- Los servicios de aplicacion NO deben cambiar al hacer el reemplazo
- Los tests de aplicacion siguen usando InMemory
- Antes de implementar: reconciliar delta del modelo (ver AGENTS.md y DELTA_MODELO_MARIADB...)

### Slice 10: Angular

- Conectar frontend Angular con endpoints de `api-faltas-core`
- No tocar antes de tener endpoints estables y persistencia definida

---

## Advertencias criticas para el agente

1. NO implementar MariaDB/JDBC hasta Slice 9 y reconciliacion del delta del modelo.
2. NO conectar Angular hasta Slice 10.
3. NO emitir `PAGAPR` -- existe en enum pero esta reservado para Slice 6C+.
4. NO implementar cierre automatico desde reingreso (`REINGRESAR_A_CIERRE`) -- sin definicion.
5. NO usar strings libres para dominio -- todos los estados, bloques y eventos son enums.
6. NO reintroducir `D3_DOCUMENTAL` como bloque.
7. NO usar `PAGCON`, `ACTCER`, `APELAC`, `DRVEXT` -- prohibidos.
8. NO mezclar circuitos -- cada comportamiento tiene un unico circuito verdadero.
9. NO cambiar la semantica de `buscarActiva` vs `buscarPorHistorico` -- separacion intencional.
10. NO sobrescribir archivos de continuidad (docs-trabajo/) sin autorizacion explicita del usuario.

---

## Fuentes obligatorias antes de tocar codigo

Leer en este orden antes de cualquier slice nuevo:

1. `backend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md`
2. `backend/api-faltas-core/docs/spec-as-source/02-estados-bloques-eventos.md`
3. `backend/api-faltas-core/docs/spec-as-source/03-comandos-precondiciones-efectos.md`
4. `backend/api-faltas-core/docs/spec-as-source/04-snapshot-bandejas-acciones.md`
5. `backend/api-faltas-core/docs/spec-as-source/06-tests-core.md`

Si el slice toca persistencia, leer ademas:
- `AGENTS.md` (raiz del repo)
- `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`

---

## Archivos clave del modulo (Slice 6B)

`backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/`
  `application/command/ReingresarDesdeGestionExternaCommand.java`
  `application/command/DerivarGestionExternaCommand.java`
  `application/service/GestionExternaService.java`
  `domain/enums/ModoReingresoGestionExterna.java`
  `domain/enums/EstadoGestionExterna.java`
  `domain/enums/ResultadoGestionExterna.java`
  `domain/enums/TipoEventoActa.java`
  `domain/model/FalGestionExterna.java`
  `repository/GestionExternaRepository.java`
  `repository/memory/InMemoryGestionExternaRepository.java`
  `web/GestionExternaController.java`
  `web/dto/ReingresarDesdeGestionExternaRequest.java`

`backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/GestionExternaTest.java`

---

## Comando de verificacion

`cd backend/api-faltas-core`
`mvn test`

Resultado esperado: Tests run: 227, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS
