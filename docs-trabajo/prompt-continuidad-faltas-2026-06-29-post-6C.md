# Prompt de continuidad - Direccion de Faltas

**Fecha:** 2026-06-29
**Para pegar en:** chat nuevo o agente nuevo
**Repo:** S:\Source\Repos\Bod-Faltas
**Modulo activo:** backend\api-faltas-core (productivo en memoria)
**Build actual:** 264/264 tests passing, 0 failures

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
| 2 | Pago voluntario completo (PAGVSO/PAGVMF/PAGINF/PAGCMP/PAGCNF/PAGOBS/PAGVVN) | ok |
| 3A | Fallo absolutorio y condenatorio minimo | ok |
| 3B | Apelacion presentada (APEPRE) | ok |
| 3C | Resolucion de apelacion (APERAZ / APEABS) | ok |
| 4 | Firmeza de condena (PLAVNC + CONFIR / CONDENA_FIRME) | ok |
| Micro | Cierre semantico: guardrails, eliminacion PAGCON, prohibicion D3 | ok |
| 5 | Pago de condena (PCOINF / PCOCNF / PCOOBS / CONDENA_FIRME_PAGADA) | ok |
| 6A | Gestion externa: derivacion (EXTDER) | ok |
| 6B | Gestion externa: reingreso (EXTRET) | ok |
| 6C | Gestion externa: pago externo (PAGAPR) | ok |

**Total: 264 tests passing. BUILD SUCCESS.**

---

## Gestion externa completa hasta Slice 6C

El circuito de gestion externa implementa tres eventos sobre el acta:

### EXTDER (Slice 6A)

- Comando: `DerivarGestionExternaCommand`
- Endpoint: `POST /api/faltas/actas/{id}/gestion-externa/derivar`
- Precondicion: `resultadoFinal == CONDENA_FIRME`, acta ACTIVA, sin gestion activa, sin pago condena CONFIRMADO
- Efectos: `FalActa` pasa a `EN_GESTION_EXTERNA / GEXT`
- `FalGestionExterna` creado con `siActiva=true`, `estadoGestionExterna=DERIVADA`, `modoReingresoGestionExterna=NO_APLICA`

### EXTRET (Slice 6B)

- Comando: `ReingresarDesdeGestionExternaCommand`
- Endpoint: `POST /api/faltas/actas/{id}/gestion-externa/reingresar`
- Precondicion: acta `EN_GESTION_EXTERNA / GEXT`, gestion activa en `DERIVADA` o `EN_CURSO`
- Efectos: `FalGestionExterna[siActiva=false, estadoGestionExterna=REINGRESADA, fechaReingreso, motivoReingreso]`
- `FalActa` pasa a `ACTIVA / ANAL`
- Modos habilitados: `REINGRESAR_A_ANALISIS`, `REINGRESAR_A_PAGO_CONDENA`
- Modos bloqueados (reservados): `REINGRESAR_A_CIERRE`, `REINGRESAR_A_ARCHIVO`

### PAGAPR (Slice 6C)

- Comando: `RegistrarPagoExternoGestionCommand`
- Endpoint: `POST /api/faltas/actas/{id}/gestion-externa/pago-externo`
- Precondicion: acta `EN_GESTION_EXTERNA / GEXT`, gestion activa en `DERIVADA` o `EN_CURSO`, `resultadoFinal == CONDENA_FIRME`, sin pago condena `CONFIRMADO`
- Efectos sobre `FalGestionExterna`: `siActiva=false`, `estadoGestionExterna=CERRADA_EXTERNA`, `resultadoGestionExterna=PAGO_EXTERNO_INFORMADO`, `fechaCierreGestionExterna=now()`
- Efectos sobre `FalActa`: `resultadoFinal=CONDENA_FIRME_PAGADA` (siempre)
- Condicion CIERRA: se emite solo si no hay bloqueantes activos
  - Sin bloqueantes: PAGAPR + CIERRA, acta `CERRADA / CERR`
  - Con bloqueantes: solo PAGAPR, acta `ACTIVA / ANAL` (transitorio hasta Slice 7)

---

## Reglas de exclusion EXTRET vs PAGAPR

| Regla | Detalle |
|-------|---------|
| Son mutuamente excluyentes por ciclo | Solo uno puede ocurrir por cada ciclo de gestion externa |
| EXTRET cierra con REINGRESADA | Sin pago externo; vuelve al circuito interno |
| PAGAPR cierra con CERRADA_EXTERNA | Con pago externo; no vuelve; resultadoFinal=CONDENA_FIRME_PAGADA |
| EXTRET no toca fechaCierreGestionExterna | Ni estadoGestionExterna=CERRADA_EXTERNA |
| PAGAPR no toca fechaReingreso, motivoReingreso, observacionesReingreso | Campos exclusivos de EXTRET |
| Ambos dejan siActiva=false | Invariante compartido de fin de ciclo |

---

## Comportamiento PAGAPR con y sin bloqueantes

### Sin bloqueantes activos (NoOpBloqueantesMaterialesChecker / Slice 7 pendiente)

```
PAGAPR registrado
gestion.siActiva = false
gestion.estadoGestionExterna = CERRADA_EXTERNA
gestion.resultadoGestionExterna = PAGO_EXTERNO_INFORMADO
gestion.fechaCierreGestionExterna = now()
acta.resultadoFinal = CONDENA_FIRME_PAGADA
CIERRA registrado
acta.situacionAdministrativa = CERRADA
acta.bloqueActual = CERR
Snapshot: CERRADAS / NINGUNA
```

### Con bloqueantes activos (transitorio hasta Slice 7)

```
PAGAPR registrado
gestion.siActiva = false
gestion.estadoGestionExterna = CERRADA_EXTERNA
gestion.resultadoGestionExterna = PAGO_EXTERNO_INFORMADO
gestion.fechaCierreGestionExterna = now()
acta.resultadoFinal = CONDENA_FIRME_PAGADA
[sin CIERRA]
acta.situacionAdministrativa = ACTIVA
acta.bloqueActual = ANAL
Snapshot: PENDIENTE_ANALISIS / NINGUNA
```

SnapshotRecalculador tiene routing explicito:
`resultadoFinal == CONDENA_FIRME_PAGADA && situacionAdministrativa == ACTIVA`
-> bandeja PENDIENTE_ANALISIS, accion NINGUNA

---

## Campo nuevo en FalGestionExterna (Slice 6C)

| Campo Java | Tipo | Nullable | Camino de cierre | Columna fisica candidata |
|-----------|------|----------|-----------------|--------------------------|
| fechaCierreGestionExterna | LocalDateTime | si | Solo PAGAPR | fecha_cierre_gestion_externa |

Campos de cierre por EXTRET (no tocados por PAGAPR):

| Campo Java | Camino de cierre |
|-----------|-----------------|
| fechaReingreso | Solo EXTRET |
| motivoReingreso | Solo EXTRET |
| observacionesReingreso | Solo EXTRET |

Columnas que NO existen ni deben crearse en FalGestionExterna:
- observacionesCierreGestionExterna: prohibida (texto libre en tabla de dominio)
- motivoCierreGestionExterna: prohibida (si estructurado en futuro: enum, no columna libre)

---

## Deuda tecnica documentada: FalObservacion / fal_observacion

Las observaciones de RegistrarPagoExternoGestionCommand.observaciones viajan transitoriamente en FalActaEvento.descripcion.

Estado de la deuda:
- fal_observacion ya definida en el modelo MariaDB base con entidad_tipo = 10 = GESTION_EXTERNA
- La tabla existe en el modelo MariaDB final, pero NO hay implementacion en el modulo in-memory
- En Slice 9/JDBC: implementar repositorio FalObservacionRepository y derivar las observaciones ahi

No crear tabla ni repositorio antes de Slice 9/JDBC.

---

## Slice 6C - Detalle (ultimo slice cerrado, 2026-06-29)

### Que se implemento

- RegistrarPagoExternoGestionCommand (record: actaId, observaciones nullable)
- RegistrarPagoExternoGestionRequest DTO
- GestionExternaService.registrarPagoExternoGestion() con 5 validadores privados
- GestionExternaController: endpoint POST /api/faltas/actas/{id}/gestion-externa/pago-externo
- FalGestionExterna.fechaCierreGestionExterna campo nuevo
- SnapshotRecalculador: routing CONDENA_FIRME_PAGADA + ACTIVA -> PENDIENTE_ANALISIS / NINGUNA

### Interaccion con FalPagoCondena

| Caso | Comportamiento |
|------|---------------|
| Sin pago condena | PAGAPR procede normalmente |
| FalPagoCondena INFORMADO | PAGAPR procede; FalPagoCondena queda INFORMADO (no se modifica) |
| FalPagoCondena OBSERVADO | PAGAPR procede; FalPagoCondena queda OBSERVADO (no se modifica) |
| FalPagoCondena CONFIRMADO | PAGAPR rechaza con PrecondicionVioladaException |
| Tras PAGAPR: InformarPagoCondena | Falla (resultadoFinal ya no es CONDENA_FIRME) |
| Tras PAGAPR: ConfirmarPagoCondena | Falla (acta cerrada o resultadoFinal no es CONDENA_FIRME) |
| PCOCNF no se emite | Invariante: el flujo interno de pago condena esta bloqueado post-PAGAPR |

### Tests nuevos Slice 6C (37 tests)

| Nested class | Tests | Que cubre |
|-------------|-------|-----------|
| EventosPagoExterno | 5 | PAGAPR/CIERRA emitidos, orden, EXTRET no emitido, observaciones en evento |
| CamposCierreGestionExterna | 5 | siActiva, estadoGestionExterna, resultadoGestionExterna, fechaCierre, campos EXTRET no tocados |
| MutacionActaPagoExterno | 7 | resultadoFinal, situacion/bloque con y sin bloqueantes, snapshot x2 |
| InteraccionFalPagoCondena | 5 | rechaza CONFIRMADO, permite INFORMADO, no modifica, flujo interno bloqueado, no doble CIERRA |
| PrecondicionesPagoExterno | 8 | todos los rechazos de precondicion |
| GuardrailsSlice6C | 7 | PAGAPR en enum, CERRADA_EXTERNA, PAGO_EXTERNO_INFORMADO, campos prohibidos, EXTDER no re-emitido, escenario CONDENA_FIRME+PAGAPR+ConfirmarFalla |

Total acumulado: 264 tests passing.

### Correcciones de auditoria aplicadas post-slice (sin cambio funcional)

1. GestionExternaTest.java: fix @DisplayName 6C-21 ("ConfirmarPagoCondena" -> "InformarPagoCondena")
2. GestionExternaTest.java: fix typo metodo 6C-02 (Clierra/Desues -> Cierra/Despues)
3. GestionExternaTest.java: agrega 6C-36 (EXTDER no re-emitido durante PAGAPR)
4. GestionExternaTest.java: agrega 6C-37 (escenario completo CONDENA_FIRME + PAGAPR + ConfirmarFalla)
5. 06-tests-core.md: sincronizado con los mismos cambios

---

## Estado de spec-as-source viva

Carpeta activa: `backend/api-faltas-core/docs/spec-as-source/`

| Archivo | Estado |
|---------|--------|
| 02-estados-bloques-eventos.md | Actualizado Slice 6C: PAGAPR activo, CERRADA_EXTERNA, PAGO_EXTERNO_INFORMADO, reglas de transicion |
| 03-comandos-precondiciones-efectos.md | Actualizado Slice 6C: RegistrarPagoExternoGestionCommand completo |
| 04-snapshot-bandejas-acciones.md | Actualizado Slice 6C: routing post-PAGAPR con y sin bloqueantes |
| 05-api-core-endpoints.md | Actualizado Slice 6C: endpoint pago-externo documentado |
| 06-tests-core.md | Actualizado Slice 6C: 37 tests documentados, total 264 |
| 99-pendientes-siguientes-slices.md | Actualizado: Slice 6C completado, pendientes 6D+ explicitados |

IMPORTANTE: La carpeta `docs/spec-as-source/` es baseline historico Slice 1B / solo lectura; no es fuente viva para nuevos slices.
La carpeta `backend/api-faltas-core/docs/spec-as-source/` es la fuente viva.

Delta MariaDB: `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`
Actualizado con: columna fecha_cierre_gestion_externa, columnas rechazadas, deuda FalObservacion, routing SnapshotRecalculador.

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
| resultadoGestionExterna nullable intencional en EXTRET | No hacer obligatorio |
| REINGRESAR_A_CIERRE y REINGRESAR_A_ARCHIVO bloqueados | Reservados, no implementar sin definicion |
| Snapshot es derivado, no fuente de verdad | Invariante del modelo |
| PAGAPR implementado en Slice 6C | Activo. Aplica a apremio, juzgado de paz u otra gestion externa |
| observacionesCierreGestionExterna NO existe en FalGestionExterna | Texto libre prohibido en tablas de dominio |
| motivoCierreGestionExterna NO existe en FalGestionExterna | Si en futuro se necesita: enum/catalogo, no texto libre |
| Observaciones PAGAPR van a FalActaEvento.descripcion transitoriamente | Hasta FalObservacion en Slice 9/JDBC |
| FALLO_CONDENATORIO_GESTION_EXTERNA NO se usa en Slice 6C | Reservado para resultado sin pago externo (Slice 6D+) |

---

## Pendientes explicitos para Slice 6D+ y posteriores

### Slice 6D+: cierre externo sin pago -- NO implementar antes de definicion

Casos pendientes de definicion:
- Gestion externa termina SIN_PAGO o DEVUELTA_PARA_REVISION
- Posible FALLO_CONDENATORIO_GESTION_EXTERNA como resultado final del acta
- No esta claro si requiere nuevo evento o si reutiliza EXTRET con un resultado especifico
- No implementar hasta definicion de dominio aprobada

### Slice 6D+: modos de reingreso reservados -- NO implementar antes de definicion

- REINGRESAR_A_CIERRE: bloqueado con PrecondicionVioladaException en GestionExternaService
- REINGRESAR_A_ARCHIVO: bloqueado con PrecondicionVioladaException en GestionExternaService
- Existen en ModoReingresoGestionExterna pero sin logica de dominio
- Requiere definir bloque, situacion, snapshot y efectos sobre FalActa en cada caso

### Slice 7: motor real de bloqueantes

- Reemplazar NoOpBloqueantesMaterialesChecker por implementacion real
- El contrato BloqueantesMaterialesChecker (application/port/) no cambia
- No tocar servicios de aplicacion al hacerlo (DI limpia, solo cambia la implementacion)
- Los tests de aplicacion siguen usando NoOpBloqueantesMaterialesChecker
- IMPORTANTE: una vez implementado, el estado CONDENA_FIRME_PAGADA + ACTIVA / ANAL
  de PAGAPR-con-bloqueantes puede materializarse en produccion;
  el routing del SnapshotRecalculador ya esta preparado para ese caso

### Slice 9: persistencia MariaDB/JDBC

- Reemplazar todos los InMemory*Repository por implementaciones con JdbcClient
- Los servicios de aplicacion NO deben cambiar al hacer el reemplazo
- Los tests de aplicacion siguen usando InMemory
- Antes de implementar: reconciliar delta del modelo completo (ver AGENTS.md)
- Incluir: implementar FalObservacionRepository con entidad_tipo = 10 = GESTION_EXTERNA
  para cerrar la deuda de observaciones PAGAPR
- Agregar columna fecha_cierre_gestion_externa a DDL de fal_acta_gestion_externa

### Slice 10: Angular

- Conectar frontend Angular con endpoints de api-faltas-core
- No tocar antes de tener endpoints estables y persistencia definida

---

## Advertencias criticas para el agente

1. NO implementar MariaDB/JDBC hasta Slice 9 y reconciliacion del delta del modelo.
2. NO conectar Angular hasta Slice 10.
3. NO implementar REINGRESAR_A_CIERRE ni REINGRESAR_A_ARCHIVO sin definicion de dominio.
4. NO implementar cierre externo sin pago (Slice 6D+) sin definicion aprobada.
5. NO usar strings libres para dominio: todos los estados, bloques y eventos son enums.
6. NO reintroducir D3_DOCUMENTAL como bloque.
7. NO usar PAGCON, ACTCER, APELAC, DRVEXT: prohibidos.
8. NO usar FALLO_CONDENATORIO_GESTION_EXTERNA sin definicion para Slice 6D+.
9. NO agregar columnas de texto libre a tablas de dominio.
10. NO mezclar circuitos: cada comportamiento tiene un unico circuito verdadero.
11. NO cambiar la semantica de buscarActiva vs buscarPorHistorico: separacion intencional.
12. NO sobrescribir archivos de continuidad (docs-trabajo/) sin autorizacion explicita del usuario.
13. NO usar la carpeta historica docs/spec-as-source/ como fuente viva.
    Usar solo backend/api-faltas-core/docs/spec-as-source/.
14. NO editar AGENTS.md, .cursor/rules/ ni docs-trabajo/ por iniciativa propia.

---

## Fuentes obligatorias antes de tocar codigo

Leer en este orden antes de cualquier slice nuevo:

1. backend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md
2. backend/api-faltas-core/docs/spec-as-source/02-estados-bloques-eventos.md
3. backend/api-faltas-core/docs/spec-as-source/03-comandos-precondiciones-efectos.md
4. backend/api-faltas-core/docs/spec-as-source/04-snapshot-bandejas-acciones.md
5. backend/api-faltas-core/docs/spec-as-source/06-tests-core.md

Si el slice toca persistencia, leer ademas:
- AGENTS.md (raiz del repo)
- docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md

Si el slice toca gestion externa, leer ademas:
- backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalGestionExterna.java
- backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/service/GestionExternaService.java

---

## Archivos clave del modulo (post Slice 6C)

backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/
  application/command/DerivarGestionExternaCommand.java
  application/command/ReingresarDesdeGestionExternaCommand.java
  application/command/RegistrarPagoExternoGestionCommand.java
  application/port/BloqueantesMaterialesChecker.java
  application/service/GestionExternaService.java
  domain/enums/EstadoGestionExterna.java
  domain/enums/ModoReingresoGestionExterna.java
  domain/enums/ResultadoGestionExterna.java
  domain/enums/TipoEventoActa.java
  domain/model/FalGestionExterna.java
  repository/GestionExternaRepository.java
  repository/memory/InMemoryGestionExternaRepository.java
  snapshot/SnapshotRecalculador.java
  web/GestionExternaController.java
  web/dto/DerivarGestionExternaRequest.java
  web/dto/ReingresarDesdeGestionExternaRequest.java
  web/dto/RegistrarPagoExternoGestionRequest.java

backend/api-faltas-core/src/test/java/ar/gob/malvinas/faltas/core/application/GestionExternaTest.java

---

## Comando de verificacion

cd backend/api-faltas-core
mvn test

Resultado esperado: Tests run: 264, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS