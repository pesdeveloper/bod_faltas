 06 - Tests del Core

## Estado actual: 329 tests passing

---

### FlujoCompletoTest (29 tests)

Tests del flujo base CAPT -> ENRI -> DOCGEN -> DOCFIR -> NOTI -> ANAL.

| Grupo | Tests |
|-------|-------|
| LabrarActaTests | 2 |
| CompletarCapturaTests | 1 |
| GenerarDocumentoTests | 1 |
| FirmarDocumentoTests | 1 |
| EnviarNotificacionTests | 1 |
| RegistrarResultadoNotificacionTests | 3 |
| ErroresTests | 8 |
| TimelineTests | 1 |
| SeparacionEnumsTests | 11 |

---

### PagoVoluntarioTest (23 tests) - Slice 2A + 2B

**Casos felices (9):**
1. Solicitar pago voluntario registra PAGVSO
2. Fijar monto registra PAGVMF
3. Informar pago registra PAGINF y NO registra PAGCMP
4. Confirmar pago sin bloqueantes: PAGCNF y CIERRA en ese orden, acta cerrada
5. Observar pago registra PAGOBS y no cierra
6. Vencer pago registra PAGVVN y habilita analisis/fallo
7. Snapshot con PENDIENTE_CONFIRMACION muestra PENDIENTE_CONFIRMACION_PAGO / CONFIRMAR_PAGO
8. Flujo completo: solicitar -> monto -> informar -> confirmar = CERRADA con CIERRA en timeline
9. Informar despues de observar

**Casos invalidos (14):**
1-13. (identicos a Slice 2B)

---

### FalloActaTest (17 tests) - Slice 3A

**Absolutorio (4):** dictar, firmar, notificar sin bloqueantes, timeline completo.
**Condenatorio (4):** dictar, notificar positivo, negativo, vencido.
**Invalidos (9):** no en ANAL, cerrada, doble fallo, monto cero, negativo, bloqueantes, no CONDENA_FIRME, firma no cierra, dictar no cierra.

---

### ApelacionActaTest (12 tests) - Slice 3B

**Correccion evento apelacion (3):**
1. APEPRE existe y resuelve como APELACION_PRESENTADA
2. APELAC no existe: deCodigo lanza excepcion
3. TipoEventoActa incluye APERAZ y APEABS como reservados

**Casos felices (2):**
3. Registrar apelacion sobre fallo condenatorio notificado: crea apelacion, APEPRE, snapshot CON_APELACION
4. Timeline: conserva eventos previos y agrega APEPRE al final, sin eventos de resolucion

**Casos invalidos (7):**
5. No permitir apelacion si no hay fallo activo
6. No permitir apelacion si el fallo es absolutorio
7. No permitir apelacion si fallo condenatorio no esta NOTIFICADO
8. No permitir doble apelacion activa
9. No permitir apelacion si el acta esta cerrada
13. Registrar apelacion no genera CONDENA_FIRME
14. Registrar apelacion no cierra el acta ni registra CIERRA

---

## Invariantes verificados en tests

### Pago voluntario
- `PAGVOL` no existe en `TipoEventoActa`
- `ACTCER` no existe (reemplazado por `CIERRA`)
- `PAGO_INFORMADO` no existe en `EstadoPagoVoluntario`
- `PAGO_VOLUNTARIO` no existe en `ResultadoFinalActa`
- `PAGCMP` no se emite sin adjunto real
- Confirmar con bloqueantes no muta estado ni registra eventos

### Fallo (Slice 3A)
- `TipoEventoActa` incluye `FALABS` y `FALCON`
- `ResultadoFinalActa` incluye `ABSUELTO` (no `FALLO_ABSOLUTORIO`)
- Dictar fallo no cierra
- Firmar fallo no cierra
- Absolutorio notificado sin bloqueantes cierra con ABSUELTO + CIERRA
- Condenatorio notificado no cierra ni asigna CONDENA_FIRME

### Apelacion (Slice 3B)
- `APELAC` NO EXISTE en `TipoEventoActa`: `deCodigo("APELAC")` lanza `IllegalArgumentException`
- `APEPRE` existe y resuelve correctamente
- `APERAZ` y `APEABS` existen como reservados con codigos correctos
- Apelacion solo sobre fallo CONDENATORIO NOTIFICADO
- No se puede apelar si no hay fallo, si el fallo es absolutorio, o si no esta notificado
- No se puede apelar si acta cerrada
- No doble apelacion activa
- Registrar apelacion no cierra el acta
- Registrar apelacion no genera CONDENA_FIRME

## Reglas del test suite

- No usar mocks de repositorios: usar implementaciones InMemory
- No testear snapshot como fuente de verdad
- Cada test verifica eventos, estado del acta y estado de la entidad secundaria
- Para tests de bloqueantes: usar lambda `actaId -> true`

---

## Slice 3C - Tests de resolucion de apelacion (nuevos en Slice 3C)

### Eventos
1. APERAZ existe y resuelve como apelacion rechazada.
2. APEABS existe y resuelve como apelacion aceptada absuelve.
3. APELAC no existe: deCodigo lanza excepcion.

### Casos felices
4. Rechazar apelacion presentada: registra APERAZ, estado RECHAZADA, no cierra, snapshot DECLARAR_CONDENA_FIRME.
5. Rechazar apelacion: no registra CIERRA.
6. Rechazar apelacion: no genera CONDENA_FIRME (resultadoFinal = SIN_RESULTADO_FINAL).
7. Timeline APEPRE -> APERAZ, sin APEABS ni CIERRA.
8. Aceptar apelacion sin bloqueantes: APEABS, ABSUELTO, CIERRA, snapshot CERRADAS.
9. Aceptar apelacion con bloqueantes: APEABS, ABSUELTO, sin CIERRA, snapshot PENDIENTE_ANALISIS.
10. Aceptar apelacion: no genera CONDENA_FIRME.

### Casos invalidos
11. No permitir rechazar si no existe apelacion activa.
12. No permitir aceptar si no existe apelacion activa.
13. No permitir resolver apelacion dos veces.
14. No permitir resolver si el acta esta cerrada.
15. No permitir rechazar si el acta esta paralizada.
16. No permitir rechazar si no hay fallo condenatorio notificado.
17. No permitir aceptar apelacion absolutoria sobre fallo absolutorio.
18. Rechazar apelacion no genera CIERRA.

**Total acumulado al cierre de Slice 3C: 97 tests** _(acumulado hist?rico)_

## Slice 4 - Tests de firmeza de condena (nuevos en Slice 4)

### Eventos y enums
1. PLAVNC existe y resuelve correctamente.
2. CONFIR existe y resuelve correctamente.
3. ResultadoFinalActa.CONDENA_FIRME existe.
4. No existe evento generico FIRMEZA: deCodigo lanza IllegalArgumentException.

### Vencimiento de plazo sin apelacion (camino 1)
5. Vencer plazo sobre fallo condenatorio notificado sin apelacion: registra PLAVNC, registra CONFIR, asigna CONDENA_FIRME, acta ACTIVA, no CIERRA, snapshot PENDIENTE_PAGO_CONDENA / GESTIONAR_PAGO_CONDENA.
6. Timeline: PLAVNC aparece antes que CONFIR.

### Firmeza por apelacion rechazada (camino 2)
7. Declarar firme por apelacion rechazada: registra CONFIR, asigna CONDENA_FIRME, no registra PLAVNC, no CIERRA, snapshot PENDIENTE_PAGO_CONDENA / GESTIONAR_PAGO_CONDENA.
8. Timeline: APEPRE, APERAZ, CONFIR presentes; sin PLAVNC.

### Casos invalidos
9. No firmeza sin fallo activo.
10. No firmeza si fallo es absolutorio.
11. No firmeza si fallo condenatorio no esta NOTIFICADO.
12. No vencer plazo si existe apelacion PRESENTADA.
13. No vencer plazo si existe apelacion RECHAZADA (usar otro comando).
14. No declarar firme por apelacion si no hay apelacion.
15. No declarar firme si apelacion sigue PRESENTADA.
16. No declarar firme si apelacion fue ACEPTADA_ABSUELVE.
17. No doble firmeza.
18. No firmeza si acta cerrada.
19. No firmeza si acta archivada.
20. No firmeza si acta anulada.
21. No firmeza si acta paralizada.
22. Firmeza no genera pago condena (no PAGCON en timeline, resultadoFinal != FALLO_CONDENATORIO_PAGADO).
23. Firmeza no cierra el acta (situacion != CERRADA).
24. Firmeza no registra CIERRA.

**Total acumulado al cierre de Slice 4: 121 tests** _(acumulado hist?rico)_

## Micro-slice de cierre semantico - Guardrails de enum

Clase: EnumGuardrailTest (nueva)

### Pago de condena - eventos correctos para Slice 5
1. PAGCON no existe como evento productivo.
2. PCOINF existe y resuelve correctamente.
3. PCOCNF existe y resuelve correctamente.
4. PCOOBS existe y resuelve correctamente.

### BloqueActual - valores exactos y prohibiciones
5. BloqueActual contiene exactamente los 7 bloques productivos (CAPT/ENRI/NOTI/ANAL/GEXT/ARCH/CERR).
6. DOCUMENTAL rechazado como codigo de bloque.
7. D2_ENRIQUECIMIENTO rechazado.
8. D4_NOTIFICACION rechazado.
9. D5_ANALISIS rechazado.

### Eventos prohibidos
10. PAGVOL no existe.
11. ACTCER no existe.
12. FALLO no existe como evento generico.
13. APELACION no existe como evento generico.
14. PASE_BANDEJA no existe.
15. PASE_DEMO no existe.
16. ACTCER no aparece en ningun valor del enum.
17. PAGCON no aparece en ningun valor del enum.

### Eventos obligatorios vigentes
18. CIERRA existe.
19. APEPRE existe.
20. APERAZ existe.
21. APEABS existe.

**Total acumulado al cierre del micro-slice de guardrails: 142 tests** _(acumulado hist?rico)_

---

## Slice 5: Tests de pago de condena (PagoCondenaTest.java)

### Guardrails de eventos

- Test 1: PCOINF, PCOCNF, PCOOBS existen en TipoEventoActa
- Test 2: PAGCON no existe (evento prohibido)

### Informar pago condena

- Test 3: Informar pago condena registra PCOINF
- Test 4: Informar pago condena no cierra el acta ni registra CIERRA

### Confirmar pago condena

- Test 5: Confirmar pago condena registra PCOCNF y luego CIERRA (en ese orden)
- Test 6: Confirmar pago condena sin bloqueantes cierra el acta (CERRADA/CERR/CONDENA_FIRME_PAGADA)
- Test 7: Confirmar pago condena con bloqueantes activos falla sin mutar ni registrar eventos

### Observar pago condena

- Test 8: Observar pago condena registra PCOOBS
- Test 9: Observar pago condena no cierra el acta ni registra CIERRA

### Precondiciones

- Test 10: No permitir pago condena si resultadoFinal != CONDENA_FIRME
- Test 11: No permitir pago condena sin fallo condenatorio notificado
- Test 12: No permitir monto cero o negativo
- Test 13: No permitir doble confirmacion
- Test 14: No permitir confirmar sin pago informado previo
- Test 15: No permitir observar pago ya confirmado
- Test 16: No permitir PAGCON (evento prohibido)
- Test 17: No reintroducir D3_DOCUMENTAL como bloque
- Test 18: No reintroducir ACTCER como evento

### Snapshot routing

- Snapshot: CONDENA_FIRME sin pago -> PENDIENTE_PAGO_CONDENA / GESTIONAR_PAGO_CONDENA
- Snapshot: pago INFORMADO -> PENDIENTE_CONFIRMACION_PAGO_CONDENA / CONFIRMAR_PAGO_CONDENA
- Snapshot: pago OBSERVADO -> PENDIENTE_PAGO_CONDENA / CORREGIR_PAGO_CONDENA
- Snapshot: pago CONFIRMADO + acta cerrada -> CERRADAS / NINGUNA


---

## Slice 6B: Tests de reingreso desde gestion externa

### GestionExternaTest (nuevos nested classes)

#### ReingresoFeliz
- Test 6B-01: Reingreso feliz a analisis
- Test 6B-02: Reingreso feliz a pago condena
- Test 6B-03: Reingreso acepta resultadoGestionExterna informado

#### EventosReingreso
- Test 6B-04: Registra EXTRET al reingresar
- Test 6B-05: No registra PAGAPR en Slice 6B
- Test 6B-06: No registra CIERRA al reingresar
- Test 6B-07: No registra EXTDER adicional

#### EstadoGestionExternaTrasReingreso
- Test 6B-08: Cierra gestion externa activa (siActiva = false)
- Test 6B-09: Deja estadoGestionExterna = REINGRESADA
- Test 6B-10: Persiste modoReingresoGestionExterna en la gestion externa
- Test 6B-11: Persiste motivoReingreso en la gestion externa
- Test 6B-12: Persiste fechaReingreso en la gestion externa

#### MutacionActaReingreso
- Test 6B-13: Deja situacionAdministrativa = ACTIVA
- Test 6B-14: Deja bloqueActual = ANAL
- Test 6B-15: Recalcula snapshot - REINGRESO_PARA_REVISION con CONDENA_FIRME
- Test 6B-16: Recalcula snapshot - REINGRESO_SIN_PAGO

#### PrecondicionesReingreso
- Test 6B-17: Rechaza sin gestion externa activa
- Test 6B-18: Rechaza acta no en bloque GEXT
- Test 6B-19: Rechaza acta no en situacion EN_GESTION_EXTERNA
- Test 6B-20: Rechaza modo nulo
- Test 6B-21: Rechaza motivo nulo
- Test 6B-22: Rechaza motivo vacio
- Test 6B-23: Rechaza REINGRESO_PARA_CIERRE (reservado para slice futuro)
- Test 6B-24: Rechaza REINGRESO_PARA_NUEVO_FALLO (reservado para slice futuro)
- Test 6B-25: Rechaza reingreso doble (segunda llamada falla)
- Test 6B-26: Rechaza REINGRESO_SIN_PAGO si resultadoFinal no es CONDENA_FIRME

#### GuardrailsSlice6B
- Test 6B-27: PAGAPR existe en enum pero no se emite en Slice 6B
- Test 6B-28: EXTRET existe y es el evento correcto de reingreso
- Test 6B-29: DRVEXT no existe como evento productivo
- Test 6B-30: InMemoryGestionExternaRepository no usa JDBC
- Test 6B-31: Ciclo completo derivar -> reingresar preserva trazabilidad

**Total Slice 6B: 31 tests nuevos** (196 previos + 31 = 227 passing)


---

## Slice 6C: Tests de pago externo de gestion externa (GestionExternaTest)

**Total Slice 6C: 37 tests nuevos** (227 previos + 37 = 264 passing)

### EventosPagoExterno (5)
- Test 6C-01: PAGAPR se registra al ejecutar el comando
- Test 6C-02: Sin bloqueantes, CIERRA se registra despues de PAGAPR (orden verificado)
- Test 6C-03: Con bloqueantes, solo PAGAPR, no CIERRA
- Test 6C-04: EXTRET no se emite al registrar pago externo
- Test 6C-05: Observaciones quedan en la descripcion del evento PAGAPR

### CamposCierreGestionExterna (5)
- Test 6C-06: siActiva = false tras pago externo
- Test 6C-07: estadoGestionExterna = CERRADA_EXTERNA
- Test 6C-08: resultadoGestionExterna = PAGO_REGISTRADO
- Test 6C-09: fechaCierreGestionExterna no es nula
- Test 6C-10: fechaReingreso, motivoReingreso y observacionesReingreso no se tocan por PAGAPR

### MutacionActaPagoExterno (7)
- Test 6C-11: resultadoFinal = CONDENA_FIRME_PAGADA siempre
- Test 6C-12: Sin bloqueantes: situacionAdministrativa = CERRADA
- Test 6C-13: Sin bloqueantes: bloqueActual = CERR
- Test 6C-14: Con bloqueantes: situacionAdministrativa = ACTIVA
- Test 6C-15: Con bloqueantes: bloqueActual = ANAL
- Test 6C-16: Snapshot sin bloqueantes: CERRADAS / NINGUNA
- Test 6C-17: Snapshot con bloqueantes: PENDIENTE_ANALISIS / NINGUNA

### InteraccionFalPagoCondena (5)
- Test 6C-18: PAGAPR rechaza si existe FalPagoCondena CONFIRMADO
- Test 6C-19: PAGAPR procede si existe FalPagoCondena INFORMADO
- Test 6C-20: Tras PAGAPR, FalPagoCondena INFORMADO sigue INFORMADO (no modificado)
- Test 6C-21: Despues de PAGAPR, InformarPagoCondena lanza PrecondicionVioladaException (resultadoFinal ya no es CONDENA_FIRME)
- Test 6C-22: No hay doble CIERRA: acta cerrada por PAGAPR rechaza derivar de nuevo

### PrecondicionesPagoExterno (8)
- Test 6C-23: Rechaza si acta no en EN_GESTION_EXTERNA
- Test 6C-24: Rechaza si bloqueActual != GEXT
- Test 6C-25: Rechaza si no existe gestion externa activa
- Test 6C-26: Rechaza si resultadoFinal != CONDENA_FIRME
- Test 6C-27: Rechaza segundo PAGAPR por inexistencia de gestion externa activa
- Test 6C-28: Rechaza acta cerrada
- Test 6C-29: Rechaza acta anulada
- Test 6C-30: Rechaza acta archivada

### GuardrailsSlice6C (7)
- Test 6C-31: PAGAPR existe en TipoEventoActa con codigo "PAGAPR"
- Test 6C-32: CERRADA_EXTERNA existe en EstadoGestionExterna
- Test 6C-33: PAGO_REGISTRADO existe en ResultadoGestionExterna
- Test 6C-34: FalGestionExterna no tiene campo observacionesCierreGestionExterna
- Test 6C-35: FalGestionExterna no tiene campo motivoCierreGestionExterna
- Test 6C-36: EXTDER no se re-emite durante PAGAPR (exactamente un EXTDER por ciclo completo)
- Test 6C-37: CONDENA_FIRME + PAGAPR + intento ConfirmarPagoCondena: falla (acta cerrada), sin PCOCNF ni CIERRA extra

---

## Estado actual del build (Slice 6C cerrado)

| Slice | Tests acumulados |
|-------|-----------------|
| Slice 3C (cierre) | 97 _(hist?rico)_ |
| Slice 4 (cierre) | 121 _(hist?rico)_ |
| Micro-slice guardrails | 142 _(hist?rico)_ |
| Slice 5 (cierre) | 164 _(hist?rico)_ |
| Slice 6A (cierre) | 196 _(hist?rico)_ |
| Slice 6B (cierre) | 227 _(hist?rico)_ |
| **Slice 6C (cierre ? estado actual)** | **264 tests passing** |

**Total actual: 270/270 tests passing.**
Build verde. Spec viva: backend/api-faltas-core/docs/spec-as-source/.


---

## Slice 7A: Tests del motor real de bloqueantes materiales (BloqueantesMaterialesTest)

**Clase:** BloqueantesMaterialesTest
**Checker usado:** RepositoryBloqueantesMaterialesChecker + InMemoryBloqueanteMaterialRepository

### PagoCondenaConBloqueantesReales (3 tests)

- Test 7A-1: Pago condena sin bloqueantes: PCOCNF + CIERRA, acta CERRADA/CERR, snapshot CERRADAS.
- Test 7A-2: Pago condena con bloqueante activo: PCOCNF emitido, CIERRA NO emitido, acta ACTIVA/ANAL, snapshot PENDIENTE_ANALISIS / NINGUNA.
- Test 7A-5: Bloqueante inactivo (siActivo=false) no impide cierre por PCOCNF: CIERRA emitido.

### PagoExternoConBloqueantesReales (2 tests)

- Test 7A-3: PAGAPR sin bloqueantes: PAGAPR + CIERRA, acta CERRADA/CERR, snapshot CERRADAS.
- Test 7A-4: PAGAPR con bloqueante activo: PAGAPR emitido, CIERRA NO emitido, acta ACTIVA/ANAL, snapshot PENDIENTE_ANALISIS / NINGUNA.

### GuardrailsSlice7A (1 test)

- Test 7A-6: No aparecen PAGCON, ACTCER, APELAC, DRVEXT en ningun camino del Slice 7A.

**Tests nuevos Slice 7A: 6**

### Nota: actualizacion de test existente en PagoCondenaTest

Test 7 de PagoCondenaTest fue actualizado para reflejar el nuevo comportamiento (Slice 7A):
- Antes: "falla sin mutar ni registrar eventos" (lanzaba PrecondicionVioladaException).
- Desde Slice 7A: "emite PCOCNF, no emite CIERRA, acta ACTIVA/ANAL" (patron PAGAPR).
El test sigue existiendo, solo cambio su comportamiento esperado. No se elimino ni se agrego un test.

---

## Estado actual del build (Slice 7A cerrado)

| Slice | Tests acumulados |
|-------|-----------------|
| Slice 3C (cierre) | 97 (historico) |
| Slice 4 (cierre) | 121 (historico) |
| Micro-slice guardrails | 142 (historico) |
| Slice 5 (cierre) | 164 (historico) |
| Slice 6A (cierre) | 196 (historico) |
| Slice 6B (cierre) | 227 (historico) |
| Slice 6C (cierre) | 264 (historico) |
| **Slice 7A (cierre - estado actual)** | **270 tests passing** |

**Total actual: 270/270 tests passing.**
Build verde. Spec viva: backend/api-faltas-core/docs/spec-as-source/.

---

## Slice 7B: Tests de gestion minima de bloqueantes materiales (BloqueantesMaterialesTest)

**Tests nuevos Slice 7B: 13**

### RegistrarBloqueante (3 tests)

- Test 7B-01: registrar bloqueante � estado PENDIENTE, siActivo=true, fechaAlta not null, fechaCierre null, existsActivoByActaId=true.
- Test 7B-02: registrar sin actaId � lanza PrecondicionVioladaException.
- Test 7B-03: registrar sin origen � lanza PrecondicionVioladaException.

### CumplirBloqueante (3 tests)

- Test 7B-04: cumplir bloqueante PENDIENTE � estado CUMPLIDO, siActivo=false, fechaCierre not null, existsActivoByActaId=false.
- Test 7B-05: cumplir bloqueante ya CUMPLIDO � idempotente, sin error.
- Test 7B-06: cumplir bloqueante ANULADO � lanza PrecondicionVioladaException.

### AnularBloqueante (3 tests)

- Test 7B-07: anular bloqueante PENDIENTE � estado ANULADO, siActivo=false, fechaCierre not null, existsActivoByActaId=false.
- Test 7B-08: anular bloqueante ya ANULADO � idempotente, sin error.
- Test 7B-09: anular bloqueante CUMPLIDO � lanza PrecondicionVioladaException.

### CierreConBloqueantesResueltos (3 tests)

- Test 7B-10: bloqueante cumplido no impide cierre por PCOCNF � CIERRA emitido, acta CERRADA/CERR.
- Test 7B-11: bloqueante anulado no impide cierre por PAGAPR � CIERRA emitido, acta CERRADA/CERR.
- Test 7B-12: bloqueante activo si impide cierre por PCOCNF (refuerzo 7A-2) � PCOCNF emitido, CIERRA NO emitido, acta ACTIVA/ANAL.

### GuardrailsSlice7B (1 test)

- Test 7B-13: ciclo registrar+cumplir+anular+PCOCNF � sin eventos prohibidos (PAGCON, ACTCER, APELAC, DRVEXT, D3_DOCUMENTAL), acta CERRADA.

---

## Slice 7C: Cierre diferido por resolucion del ultimo bloqueante activo

Clase: `BloqueantesMaterialesTest` (seccion `CierreDiferido`, 8 tests nuevos)

### CierreDiferido (8 tests)

- Test 7C-01: cumplir ultimo bloqueante activo con CONDENA_FIRME_PAGADA -> CIERRA emitido, acta CERRADA/CERR.
- Test 7C-02: anular ultimo bloqueante activo con CONDENA_FIRME_PAGADA -> CIERRA emitido, acta CERRADA/CERR.
- Test 7C-03: dos bloqueantes activos, cumplir uno -> no CIERRA, acta sigue ACTIVA/ANAL.
- Test 7C-04: acta con CONDENA_FIRME (no cerrable) -> cumplir bloqueante -> no CIERRA.
- Test 7C-05: acta ya cerrada -> cumplir bloqueante sobre cerrada -> no CIERRA duplicado.
- Test 7C-06: cumplir idempotente dos veces -> exactamente 1 CIERRA en eventos.
- Test 7C-07: anular idempotente dos veces -> exactamente 1 CIERRA en eventos.
- Test 7C-08: guardrail -> ciclo completo sin eventos/bloques/estados prohibidos.
---

## Estado actual del build (Slice 7B cerrado)

| Slice | Tests acumulados |
|-------|-----------------|
| Slice 3C (cierre) | 97 (historico) |
| Slice 4 (cierre) | 121 (historico) |
| Micro-slice guardrails | 142 (historico) |
| Slice 5 (cierre) | 164 (historico) |
| Slice 6A (cierre) | 196 (historico) |
| Slice 6B (cierre) | 227 (historico) |
| Slice 6C (cierre) | 264 (historico) |
| Slice 7A (cierre) | 270 (historico) |
| Slice 7B (cierre) | 283 (historico) |
| **Slice 7C (cierre - estado actual)** | **291 tests passing** |

**Total actual: 291/291 tests passing.**
Build verde. Spec viva: backend/api-faltas-core/docs/spec-as-source/.

---

## Slice 6D-1: Tests de reingreso sin pago y sin cambios (GestionExternaTest)

**Total Slice 6D-1: 15 tests nuevos** (300 previos + 15 = 315 passing)

### Slice6D1 (15 tests)

#### Casos felices (8)
- Test 6D1-01: SIN_PAGO + REINGRESO_SIN_PAGO reingresa por EXTRET; verifica EXTRET, siActiva=false, estadoGestionExterna=REINGRESADA, resultadoFinal=CONDENA_FIRME, ACTIVA/ANAL
- Test 6D1-02: SIN_CAMBIOS + REINGRESO_PARA_REVISION reingresa por EXTRET; mismos asserts de estado
- Test 6D1-03: SIN_PAGO no toca campos PAGAPR (fechaCierre null, no CERRADA_EXTERNA, no CONDENA_FIRME_PAGADA)
- Test 6D1-04: SIN_CAMBIOS no toca campos PAGAPR
- Test 6D1-05: Despues de SIN_PAGO no se puede PAGAPR en mismo ciclo
- Test 6D1-06: Despues de SIN_CAMBIOS no se puede PAGAPR en mismo ciclo
- Test 6D1-07: SIN_PAGO permite seguir circuito interno de pago condena (acta CONDENA_FIRME, informar procede)
- Test 6D1-08: SIN_CAMBIOS deja snapshot coherente en analisis (PENDIENTE_PAGO_CONDENA / GESTIONAR_PAGO_CONDENA)

#### Precondiciones rechazadas (5)
- Test 6D1-09: Rechaza SIN_PAGO si acta no esta en GEXT
- Test 6D1-10: Rechaza SIN_CAMBIOS si no hay gestion externa activa
- Test 6D1-11: Rechaza par incoherente SIN_PAGO + REINGRESO_PARA_REVISION
- Test 6D1-12: Rechaza par incoherente SIN_CAMBIOS + REINGRESO_SIN_PAGO
- Test 6D1-13: Rechaza resultado reservado ABSUELVE (requiere REINGRESO_CON_DICTAMEN, slice futuro)

#### Guardrails (2)
- Test 6D1-14: Rechaza modo reservado REINGRESO_PARA_NUEVO_FALLO
- Test 6D1-15: Guardrail - EXTRET es unico evento de reingreso; PAGCON/ACTCER/APELAC/DRVEXT no existen; REINGRESO_PARA_CIERRE y REINGRESO_PARA_NUEVO_FALLO bloqueados

### Cambios a tests existentes (Slice 6D-1)
- Test 6B-03 actualizado: par corregido a `SIN_CAMBIOS + REINGRESO_PARA_REVISION` (el par anterior `SIN_PAGO + REINGRESO_PARA_REVISION` era incoherente per 6D-1)

### Tabla acumulada tras Slice 6D-1

| Slice | Tests passing |
|-------|--------------|
| Slice 6A (cierre) | 196 (historico) |
| Slice 6B (cierre) | 227 (historico) |
| Slice 6C (cierre) | 264 (historico) |
| Slice 7A (cierre) | 270 (historico) |
| Slice 7B (cierre) | 283 (historico) |
| Slice 7C (cierre) | 291 (historico) |
| Slice 6D-0 (cierre) | 300 (historico) |
| **Slice 6D-1 (cierre - estado actual)** | **315 tests passing** |

**Total actual: 315/315 tests passing.**
Build verde. Spec viva: backend/api-faltas-core/docs/spec-as-source/.

## Slice 6D-2: Tests de reingreso con dictamen externo (GestionExternaTest$Slice6D2)

14 tests en clase `Slice6D2` (dentro de `GestionExternaTest`):

- 6D2-01: ABSUELVE + REINGRESO_PARA_NUEVO_FALLO reingresa por EXTRET (resultadoFinal no cambia)
- 6D2-02: CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN reingresa por EXTRET (CONDENA_FIRME mantenida)
- 6D2-03: MODIFICA_MONTO + REINGRESO_CON_DICTAMEN registra monto y vuelve a analisis
- 6D2-04: MODIFICA_MONTO rechaza montoResultado null
- 6D2-05: MODIFICA_MONTO rechaza monto cero o negativo
- 6D2-06: ABSUELVE rechaza modo REINGRESO_CON_DICTAMEN
- 6D2-07: CONFIRMA_CONDENA rechaza modo REINGRESO_PARA_NUEVO_FALLO
- 6D2-08: MODIFICA_MONTO rechaza modo REINGRESO_PARA_NUEVO_FALLO
- 6D2-09: REINGRESO_PARA_CIERRE sigue bloqueado
- 6D2-10: Post dictamen ABSUELVE no permite PAGAPR en mismo ciclo
- 6D2-11: Post MODIFICA_MONTO no pasa directo a pago ni cierre
- 6D2-12: PAGO_REGISTRADO sigue exclusivo de PAGAPR
- 6D2-13: SIN_RESULTADO no es aceptado como resultado de reingreso
- 6D2-14: Guardrail integral

**Total acumulado al cierre de Slice 6D-2: 329 tests**