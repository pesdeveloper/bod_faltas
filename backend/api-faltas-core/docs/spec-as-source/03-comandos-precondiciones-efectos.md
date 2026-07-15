# 03 - Comandos, Precondiciones y Efectos

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Ante contradiccion con un documento tematico de `00-governance/` o `10-domain/`, ese documento tematico prevalece en lo que respecta a definiciones, dimensiones y lifecycle (ver README, seccion 4.0). Ante contradiccion sobre CMD-FALLO-001..007, prevalece `20-application/fallo-command-contracts.md`.

## Ciclo base

### LabrarActa
- **Precondiciones**: ninguna (inicio de flujo)
- **Efectos**: crea FalActa (CAPT), evento ACTLAB, snapshot inicial

### CompletarCaptura
- **Precondiciones**: acta en CAPT, no cerrada
- **Efectos**: bloque -> ENRI, evento ACTCAP

### EnriquecerActa
- **Precondiciones**: acta en ENRI, no cerrada
- **Efectos**: evento ACTENR (sin transicion de bloque)

### GenerarDocumento
- **Precondiciones**: acta no cerrada
- **Efectos**: crea FalDocumento PENDIENTE_FIRMA, evento DOCGEN

### FirmarDocumento

Flujo naive de compatibilidad (`DocumentoService.firmarDocumento`). El circuito canonico vigente de firma
es `RegistrarFirmaDocumental` (callback de Firmas), documentado en
[`10-domain/firma-notificacion-fallo.md`](10-domain/firma-notificacion-fallo.md) y en la seccion
"RegistrarFirmaDocumental (callback de Firmas)" mas abajo. Ante contradiccion, ese documento prevalece.

- **Precondiciones**: documento existe y no esta firmado
- **Efectos**: FalDocumento -> FIRMADO, crea FalDocumentoFirma, evento DOCFIR
- **Efecto adicional**: si el documento es el asociado al fallo activo del acta (FALLO_ABSOLUTORIO o
  FALLO_CONDENATORIO), delega en `completarFirmaDocumento`, que llama
  `FalActaFallo.marcarPendienteNotificacion(ahora)` -- el fallo pasa a `PENDIENTE_NOTIFICACION`, nunca a un
  hipotetico `FIRMADO` (`FIRMADO` no es un valor de `EstadoFalloActa`).
- NO cierra el acta en ningun caso (ni fallo ni pieza inicial)

### EnviarNotificacion
- **Precondiciones**: acta no cerrada, documento firmado
- **Efectos**: crea FalNotificacion EN_PROCESO, bloque -> NOTI, evento NOTENV

### RegistrarNotificacionPositiva
- **Precondiciones**: notificacion existe, sin resultado previo
- **Efectos segun tipo de documento notificado**:
  1. Si notifica pieza inicial u otra pieza NO fallo:
     - notificacion -> CON_ACUSE_POSITIVO
     - bloque -> ANAL
     - evento NOTPOS
  2. Si notifica FALLO_ABSOLUTORIO:
     - notificacion -> CON_ACUSE_POSITIVO
     - FalActaFallo.estadoFallo -> NOTIFICADO
     - acta.resultadoFinal = ABSUELTO
     - evento NOTPOS
     - Si BloqueantesMaterialesChecker.tieneBloqueantesActivos() == false:
       - acta.situacionAdministrativa = CERRADA, bloque = CERR
       - evento CIERRA (inmediatamente despues de NOTPOS)
     - Si hay bloqueantes activos: NO cierra (ABSUELTO asignado, acta sigue activa)
  3. Si notifica FALLO_CONDENATORIO:
     - notificacion -> CON_ACUSE_POSITIVO
     - FalActaFallo.estadoFallo -> NOTIFICADO
     - evento NOTPOS
     - NO cierra
     - NO asigna CONDENA_FIRME
     - Acta queda preparada para el circuito de apelacion/firmeza/pago de condena

### RegistrarNotificacionNegativa
- **Precondiciones**: notificacion existe, sin resultado previo
- **Efectos**: notificacion -> CON_ACUSE_NEGATIVO, evento NOTNEG

### RegistrarNotificacionVencida
- **Precondiciones**: notificacion existe, sin resultado previo
- **Efectos**: notificacion -> VENCIDA, evento NOTVNC

---

## Pago Voluntario

Pago voluntario es un FLUJO, no una accion unica.
PAGVOL no existe. PAGO_INFORMADO no existe.

### SolicitarPagoVoluntario
- **Command**: `SolicitarPagoVoluntarioCommand(actaId, observaciones)`
- **Precondiciones**:
  - Acta existe y no esta cerrada/anulada/archivada
  - Bloque debe ser ENRI o ANAL
  - No existe pago voluntario confirmado previo
- **Efectos**:
  - Crea o actualiza FalPagoVoluntario en estado SOLICITADO
  - Si bloque era ENRI, transiciona a ANAL
  - Evento PAGVSO

### FijarMontoPagoVoluntario
- **Command**: `FijarMontoPagoVoluntarioCommand(actaId, monto, observaciones)`
- **Precondiciones**:
  - Pago voluntario existe en estado SOLICITADO o MONTO_FIJADO
  - Monto mayor a cero
  - No confirmado, no vencido
- **Efectos**:
  - FalPagoVoluntario -> MONTO_FIJADO, guarda monto
  - Evento PAGVMF

### InformarPagoVoluntario
- **Command**: `InformarPagoVoluntarioCommand(actaId, referenciaPago, observaciones)`
- **Precondiciones**:
  - Pago voluntario en SOLICITADO, MONTO_FIJADO u OBSERVADO
  - referenciaPago obligatoria y no vacia
  - No confirmado, no vencido
- **Efectos**:
  - FalPagoVoluntario -> PENDIENTE_CONFIRMACION, guarda referenciaPago
  - Evento PAGINF
  - NO emite PAGCMP: no hay adjunto/evidencia real del comprobante

### ConfirmarPagoVoluntario
- **Command**: `ConfirmarPagoVoluntarioCommand(actaId, observaciones)`
- **Precondiciones**:
  - Pago voluntario en PENDIENTE_CONFIRMACION
  - Acta no cerrada
  - BloqueantesMaterialesChecker.tieneBloqueantesActivos() == false
    (Si hay bloqueantes activos: lanza PrecondicionVioladaException, NO registra ningun evento)
- **Efectos** (solo si sin bloqueantes):
  - FalPagoVoluntario -> CONFIRMADO
  - acta.resultadoFinal = PAGO_VOLUNTARIO_PAGADO
  - acta.situacionAdministrativa = CERRADA
  - acta.bloqueActual = CERR
  - Evento PAGCNF (pago confirmado)
  - Evento CIERRA (acta cerrada), inmediatamente despues de PAGCNF
  - Snapshot: CERRADAS / NINGUNA

### ObservarPagoVoluntario
- **Command**: `ObservarPagoVoluntarioCommand(actaId, motivoObservacion, observaciones)`
- **Precondiciones**:
  - Pago voluntario en PENDIENTE_CONFIRMACION
  - motivoObservacion obligatorio
  - No confirmado
- **Efectos**:
  - FalPagoVoluntario -> OBSERVADO, guarda motivoObservacion
  - Evento PAGOBS
  - Snapshot: PENDIENTE_ANALISIS / CORREGIR_PAGO

### VencerPagoVoluntario
- **Command**: `VencerPagoVoluntarioCommand(actaId, observaciones)`
- **Precondiciones**:
  - Pago voluntario en SOLICITADO, MONTO_FIJADO, PENDIENTE_CONFIRMACION u OBSERVADO
  - No confirmado, acta no cerrada
- **Efectos**:
  - FalPagoVoluntario -> VENCIDO
  - acta.bloqueActual = ANAL
  - Evento PAGVVN
  - Snapshot: PENDIENTE_ANALISIS / DICTAR_FALLO

---

## Puerto BloqueantesMaterialesChecker

Interfaz: `ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker`
Metodo: `boolean tieneBloqueantesActivos(String actaId)`
Implementacion productiva: `RepositoryBloqueantesMaterialesChecker` (@Component)

Semantica del puerto:
- `true` = existen bloqueantes activos, accion bloqueada
- `false` = sin bloqueantes, accion permitida

Uso: ConfirmarPagoVoluntario, RegistrarNotificacionPositiva para fallo absolutorio.
Para tests: lambda `actaId -> true` para simular bloqueantes activos.

Ver seccion "Motor real de bloqueantes materiales" mas abajo.

---

## Fallo absolutorio y condenatorio minimo

### DictarFalloAbsolutorioCommand
- **Command**: `DictarFalloAbsolutorioCommand(actaId, fundamentos, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Acta esta en bloque ANAL
  - No hay pago voluntario confirmado
  - No hay fallo activo ya dictado sobre la misma acta
- **Efectos**:
  - Crea FalActaFallo con tipoFallo=ABSOLUTORIO, estadoFallo=PENDIENTE_FIRMA
  - Crea documento FALLO_ABSOLUTORIO (estadoDocumento=PENDIENTE_FIRMA)
  - Registra evento FALABS
  - Registra evento DOCGEN
  - FalActaFallo.documentoId = id del documento creado
  - Snapshot: PENDIENTE_FIRMA / FIRMAR_DOCUMENTO
- **No cierra el acta**
- **No asigna resultadoFinal**

### DictarFalloCondenatorioCommand
- **Command**: `DictarFalloCondenatorioCommand(actaId, montoCondena, fundamentos, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Acta esta en bloque ANAL
  - No hay pago voluntario confirmado
  - No hay fallo activo ya dictado sobre la misma acta
  - montoCondena > 0
- **Efectos**:
  - Crea FalActaFallo con tipoFallo=CONDENATORIO, estadoFallo=PENDIENTE_FIRMA, montoCondena guardado
  - Crea documento FALLO_CONDENATORIO (estadoDocumento=PENDIENTE_FIRMA)
  - Registra evento FALCON
  - Registra evento DOCGEN
  - FalActaFallo.documentoId = id del documento creado
  - Snapshot: PENDIENTE_FIRMA / FIRMAR_DOCUMENTO
- **No cierra el acta**
- **No asigna resultadoFinal**
- **No asigna CONDENA_FIRME**

---

## Deudas tecnicas documentadas

### referenciaPago
Dato temporal. En produccion sera reemplazado por integracion real
con el sistema de Ingresos/Tesoreria (Cmte_PG / Pref_PG / Nro_PG). Ver `99-pendientes-siguientes-slices.md`.

### PAGCMP (comprobante adjunto)
El evento PAGCMP existe en el enum pero no se emite en `InformarPagoVoluntario`.
Se reserva para cuando exista un circuito de adjuntos/comprobantes reales.
Emitirlo sin adjunto real seria semanticamente incorrecto.

### Fallo condenatorio post-notificacion
Luego de registrar notificacion positiva de FALLO_CONDENATORIO, el acta queda
con resultadoFinal=SIN_RESULTADO_FINAL y fallo NOTIFICADO.
Los pasos siguientes (plazo de apelacion, apelacion, firmeza, pago condena) estan documentados en las
secciones de apelacion, firmeza y pago de condena de este mismo documento.

---

## Apelacion presentada

### RegistrarApelacionCommand
- **Command**: `RegistrarApelacionCommand(actaId, presentante, fundamentos, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Existe fallo activo sobre el acta
  - El fallo activo es CONDENATORIO
  - El fallo condenatorio esta en estado NOTIFICADO
  - No existe apelacion activa previa sobre la misma acta
  - No existe CONDENA_FIRME
- **Efectos**:
  - Crea FalActaApelacion con estadoApelacion=PRESENTADA, siActiva=true
  - Registra evento APEPRE
  - Snapshot: CON_APELACION / RESOLVER_APELACION
- **No cierra el acta**
- **No asigna resultadoFinal**
- **No genera CONDENA_FIRME**
- **No inicia pago condena**
- **No resuelve la apelacion**

### Correccion obligatoria: APELAC no existe
- `APELAC` NO ES evento productivo. Fue reemplazado por `APEPRE`.
- `TipoEventoActa.deCodigo("APELAC")` debe lanzar `IllegalArgumentException`.
- `TipoEventoActa.deCodigo("APEPRE")` resuelve correctamente.
- `APERAZ` y `APEABS` son eventos productivos vigentes de resolucion de apelacion.
- `TipoEventoActa.deCodigo("APERAZ")` resuelve como apelacion rechazada.
- `TipoEventoActa.deCodigo("APEABS")` resuelve como apelacion aceptada absuelve.

### Calculo de plazo
El calculo de vencimiento del plazo de apelacion esta gobernado por
[`10-domain/calendario-plazos-administrativos.md`](10-domain/calendario-plazos-administrativos.md).

### Resolucion de apelacion (APERAZ / APEABS)
Ver seccion "Resolucion de apelacion" mas abajo.

---

## Resolucion de apelacion

### ResolverApelacionRechazadaCommand
- **Command**: `ResolverApelacionRechazadaCommand(actaId, fundamentosResolucion, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Existe apelacion activa sobre el acta
  - La apelacion esta en estado PRESENTADA (o EN_ANALISIS, segun el circuito interno de analisis)
  - Existe fallo activo
  - El fallo activo es CONDENATORIO
  - El fallo condenatorio esta en estado NOTIFICADO
  - No existe condena firme (ResultadoFinalActa != CONDENA_FIRME)
  - No existe pago de condena confirmado
  - No existe gestion externa activa
- **Efectos**:
  - FalActaApelacion.estadoApelacion -> RECHAZADA
  - FalActaApelacion.siActiva -> false
  - FalActaApelacion.fechaResolucion, fundamentosResolucion, observacionesResolucion asignados
  - Registra evento APERAZ
  - Snapshot: PENDIENTE_ANALISIS / DECLARAR_CONDENA_FIRME
- **No cierra el acta**
- **No registra CIERRA**
- **No registra CONFIR**
- **No asigna CONDENA_FIRME**
- **No inicia pago condena**

### ResolverApelacionAceptaAbsuelveCommand
- **Command**: `ResolverApelacionAceptaAbsuelveCommand(actaId, fundamentosResolucion, observaciones)`
- **Precondiciones**: identicas a ResolverApelacionRechazadaCommand
- **Efectos**:
  - FalActaApelacion.estadoApelacion -> ACEPTADA_ABSUELVE
  - FalActaApelacion.siActiva -> false
  - FalActaApelacion.fechaResolucion, fundamentosResolucion, observacionesResolucion asignados
  - FalActa.resultadoFinal -> ABSUELTO
  - Registra evento APEABS
  - Si BloqueantesMaterialesChecker.tieneBloqueantesActivos == false:
    - FalActa.situacionAdministrativa -> CERRADA
    - FalActa.bloqueActual -> CERR
    - FalActa.estadoProcesal -> CONCLUIDO
    - Registra evento CIERRA (despues de APEABS)
    - Snapshot: CERRADAS / NINGUNA
  - Si hay bloqueantes activos:
    - No cierra
    - No registra CIERRA
    - Snapshot: PENDIENTE_ANALISIS / NINGUNA (pendiente operativo por bloqueantes)

### Notas de alcance de apelacion

- `EstadoApelacionActa` tambien incluye `EN_ANALISIS` y `RESUELTA` (ver `02-estados-bloques-eventos.md`).
  Estos valores gobiernan el circuito interno de analisis de la apelacion antes de su resolucion final.
- Pago de condena: ver seccion "Pago de condena" de este documento.
- Gestion externa: ver seccion "Reingreso desde gestion externa" y "Pago externo de gestion externa".

---

## Familia canonica de comandos de fallo

Los contratos completos y autoritativos de CMD-FALLO-001..007 viven
exclusivamente en [`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md).

Esta seccion no duplica firmas, precondiciones, orden de efectos ni concurrencia.

| ID | Nombre canonico | Servicio | Heading propietario |
|----|------------------|----------|----------------------|
| CMD-FALLO-001 | Confirmar firma documental real | `DocumentoService.registrarFirmaDocumental` | [`20-application/fallo-command-contracts.md#cmd-fallo-001-confirmar-firma-documental-real`](20-application/fallo-command-contracts.md) |
| CMD-FALLO-002 | Iniciar envio notificatorio directo | `NotificacionService.enviarNotificacion` | [`20-application/fallo-command-contracts.md#cmd-fallo-002-iniciar-envio-notificatorio-directo`](20-application/fallo-command-contracts.md) |
| CMD-FALLO-003 | Generar lote postal desde notificaciones pendientes | `LoteCorreoService.generarLoteDesdePendientes` | [`20-application/fallo-command-contracts.md#cmd-fallo-003-generar-lote-postal-desde-notificaciones-pendientes`](20-application/fallo-command-contracts.md) |
| CMD-FALLO-004 | Registrar resultado notificatorio positivo | `NotificacionService.registrarPositiva` | [`20-application/fallo-command-contracts.md#cmd-fallo-004-registrar-resultado-notificatorio-positivo`](20-application/fallo-command-contracts.md) |
| CMD-FALLO-005 | Declarar firmeza por vencimiento del plazo de apelacion | `FirmezaCondenaService.vencerPlazoApelacion` | [`20-application/fallo-command-contracts.md#cmd-fallo-005-declarar-firmeza-por-vencimiento-del-plazo-de-apelacion`](20-application/fallo-command-contracts.md) |
| CMD-FALLO-006 | Declarar firmeza por apelacion rechazada | `FirmezaCondenaService.declararFirmePorApelacionRechazada` | [`20-application/fallo-command-contracts.md#cmd-fallo-006-declarar-firmeza-por-apelacion-rechazada`](20-application/fallo-command-contracts.md) |
| CMD-FALLO-007 | Informar pago de condena | `PagoCondenaService.informar` | [`20-application/fallo-command-contracts.md#cmd-fallo-007-informar-pago-de-condena`](20-application/fallo-command-contracts.md) |

## Firmeza de condena

La firmeza de condena se declara exclusivamente mediante CMD-FALLO-005
(vencimiento del plazo de apelacion) o CMD-FALLO-006 (apelacion rechazada).
Ver la tabla de la "Familia canonica de comandos de fallo" para los enlaces
propietarios. Este documento no mantiene una segunda version de sus
precondiciones, efectos, orden de concurrencia ni modelo de persistencia.

- La firmeza es inline en `FalActaFallo`; no existe un agregado ni un
  repositorio dedicados a la persistencia de firmeza (ver `110`, decision P2,
  para el detalle de la eliminacion de codigo confirmada).
- La apelacion relevante para CMD-FALLO-006 se consulta por `fallo.id`, no por
  "ultima apelacion del acta"; una apelacion historica de otro fallo no
  bloquea ni satisface la precondicion.
- Ambos comandos declaran actor (JWT `sub` via `ActorContextHolder`); no
  existen firmas legacy de estos comandos sin actor.

### Dependencias de firmeza y pago de condena

- Pago de condena (PCOINF/PCOCNF/PCOOBS): ver seccion "Pago de condena".
- Gestion externa (EXTDER/EXTRET/PAGAPR): ver secciones "Reingreso desde gestion externa" y "Pago externo de
  gestion externa". `DRVEXT` sigue prohibido.
- Calculo real de plazos de apelacion: ver `10-domain/calendario-plazos-administrativos.md`.

---

## Pago de condena - Comandos, precondiciones y efectos

Informar pago de condena pertenece a CMD-FALLO-007 y su contrato completo
vive exclusivamente en [`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md#cmd-fallo-007-informar-pago-de-condena).
Este documento no mantiene una segunda lista de campos, precondiciones,
efectos, estado del fallo, actor ni concurrencia para ese comando; ver la
tabla de la "Familia canonica de comandos de fallo".

### ConfirmarPagoCondenaCommand

`ConfirmarPagoCondenaCommand` es un comando general de pago de condena, fuera
de los siete contratos canonicos CMD-FALLO-001..007. Su contrato completo
vive en este documento.

**Campos:** actaId, observaciones

**Precondiciones:**
- Pago de condena existe y estado es `INFORMADO`
- Acta no esta cerrada
- `resultadoFinal == CONDENA_FIRME`

La presencia de bloqueantes materiales activos NO es una precondicion de este
comando: `ConfirmarPagoCondena` confirma el pago exista o no un bloqueante
activo. Los bloqueantes solo condicionan si se registra `CIERRA` (ver Efectos).

**Efectos:**
- Estado pago condena -> `CONFIRMADO` (siempre que se cumplan las precondiciones)
- `acta.resultadoFinal = CONDENA_FIRME_PAGADA`
- Registra `PCOCNF` (siempre, primer evento)

Sin bloqueantes activos:
- `acta.situacionAdministrativa = CERRADA`
- `acta.bloqueActual = CERR`
- Registra `CIERRA` (despues de `PCOCNF`)
- Snapshot: `CERRADAS / NINGUNA`

Con bloqueantes activos:
- `acta.situacionAdministrativa = ACTIVA`
- `acta.bloqueActual = ANAL`
- NO registra `CIERRA`
- Snapshot: `PENDIENTE_ANALISIS / NINGUNA`

El motor real de bloqueantes (`RepositoryBloqueantesMaterialesChecker`) determina unicamente si se emite
`CIERRA`; `PCOCNF` y la asignacion de `CONDENA_FIRME_PAGADA` ocurren siempre que se cumplen las
precondiciones. Este comportamiento sigue el mismo patron que `RegistrarPagoExternoGestionCommand` (PAGAPR).

### ObservarPagoCondenaCommand

**Campos:** actaId, motivoObservacion, observaciones

**Precondiciones:**
- Pago de condena existe y estado es `INFORMADO`
- No esta confirmado
- `motivoObservacion` obligatorio y no vacio

**Efectos:**
- Estado pago condena -> `OBSERVADO`
- `motivoObservacion` registrado
- Registra `PCOOBS`
- NO cierra el acta
- NO registra `CIERRA`
- Snapshot: `PENDIENTE_PAGO_CONDENA / CORREGIR_PAGO_CONDENA`

---

## Reingreso desde gestion externa - Comandos, precondiciones y efectos

### ReingresarDesdeGestionExternaCommand

**Campos:**
- `actaId` -- obligatorio
- `modoReingresoGestionExterna` -- obligatorio; debe ser un valor habilitado (ver tabla de pares mas abajo)
- `motivoReingreso` -- obligatorio, no vacio
- `resultadoGestionExterna` -- opcional (nullable); si se informa se persiste en `FalGestionExterna` y debe
  formar un par valido con `modoReingresoGestionExterna`
- `observaciones` -- opcional (nullable)

**Precondiciones comunes:**
- Acta existe
- `modoReingresoGestionExterna` no nulo
- `motivoReingreso` no nulo ni vacio
- `acta.situacionAdministrativa == EN_GESTION_EXTERNA`
- `acta.bloqueActual == GEXT`
- `modoReingresoGestionExterna` es un valor habilitado (no `REINGRESO_PARA_CIERRE`, no `REINGRESO_CON_PAGO`)
- Existe `FalGestionExterna` activa (`siActiva == true`)
- `FalGestionExterna.estadoGestionExterna in [DERIVADA, EN_CURSO]`
- `resultadoFinal == CONDENA_FIRME` para los modos que lo requieren (`REINGRESO_SIN_PAGO`,
  `REINGRESO_PARA_NUEVO_FALLO`, `REINGRESO_CON_DICTAMEN`)
- Acta no cerrada, no anulada, no archivada

**Pares habilitados de `resultadoGestionExterna` + `modoReingresoGestionExterna`:**

| resultadoGestionExterna | modoReingresoGestionExterna | Descripcion |
|------------------------|----------------------------|-------------|
| `SIN_PAGO`             | `REINGRESO_SIN_PAGO`        | Vuelve sin pago; continua circuito interno de cobro. Requiere CONDENA_FIRME |
| `SIN_CAMBIOS`          | `REINGRESO_PARA_REVISION`   | Vuelve sin cambios sustantivos; revision interna |
| `ABSUELVE`             | `REINGRESO_PARA_NUEVO_FALLO`| El externo propone absolver. Requiere CONDENA_FIRME |
| `CONFIRMA_CONDENA`     | `REINGRESO_CON_DICTAMEN`    | El externo confirma condena. Requiere CONDENA_FIRME |
| `MODIFICA_MONTO`       | `REINGRESO_CON_DICTAMEN`    | El externo modifica monto. Requiere `montoResultado > 0` |

**Pares invalidos rechazados:**
- Cualquier combinacion que no figure en la tabla de pares habilitados (ejemplo: `SIN_PAGO` +
  `REINGRESO_PARA_REVISION`, `SIN_CAMBIOS` + `REINGRESO_SIN_PAGO`).
- `resultadoGestionExterna == PAGO_REGISTRADO` (asignado automaticamente por PAGAPR, no valido para reingreso).
- `resultadoGestionExterna == SIN_RESULTADO` (estado inicial al derivar, no valido para reingreso).

**Modos reservados (no habilitados):**
- `REINGRESO_PARA_CIERRE`: reservado; requiere decision de cierre definitivo.
- `REINGRESO_CON_PAGO`: reservado/no aplicable al flujo `PAGAPR` actual. `PAGAPR` cierra la gestion con
  `estadoGestionExterna = CERRADA_EXTERNA` sin usar modo de reingreso.

**Campo adicional para dictamen externo:**

| Campo | Tipo | Obligatorio | Descripcion |
|-------|------|-------------|-------------|
| `montoResultado` | `BigDecimal` | Solo para MODIFICA_MONTO (> 0) | Monto externo informado. Persiste en `FalGestionExterna.montoResultado` (`monto_resultado` en MariaDB). |

**Efectos comunes:**
- `FalGestionExterna.siActiva = false`
- `FalGestionExterna.estadoGestionExterna = REINGRESADA`
- `FalGestionExterna.modoReingresoGestionExterna` = modo indicado
- `FalGestionExterna.motivoReingreso` registrado
- `FalGestionExterna.fechaReingreso` registrado
- `FalGestionExterna.observacionesReingreso` registrado (si presente)
- `FalGestionExterna.resultadoGestionExterna` registrado (si presente)
- `acta.situacionAdministrativa = ACTIVA`
- `acta.bloqueActual = ANAL`
- Registra evento `EXTRET`
- Recalcula snapshot segun estado real del acta
- **No registra** `PAGAPR`
- **No registra** `CIERRA`
- **No registra** `EXTDER` adicional
- `fechaCierreGestionExterna` no se toca (se usa exclusivamente en PAGAPR)

**Efectos especificos por caso:**

- **Caso ABSUELVE + REINGRESO_PARA_NUEVO_FALLO:** `FalActa.resultadoFinal` no cambia automaticamente; no
  genera fallo absolutorio automatico; la acta vuelve a ANAL para que internamente se dicte nuevo fallo si
  corresponde.
- **Caso CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN:** `FalActa.resultadoFinal = CONDENA_FIRME` (confirmado
  explicitamente); permite continuar circuito interno de pago u otras acciones en ANAL.
- **Caso MODIFICA_MONTO + REINGRESO_CON_DICTAMEN:** `FalGestionExterna.montoResultado = montoResultado` (del
  comando); `FalActa.resultadoFinal` no cambia automaticamente; no dicta nuevo fallo; no pasa a pago
  automaticamente.

**Errores esperados:**
- `PrecondicionVioladaException` si modo nulo
- `PrecondicionVioladaException` si motivo nulo o vacio
- `PrecondicionVioladaException` si acta no esta en `EN_GESTION_EXTERNA`
- `PrecondicionVioladaException` si `bloqueActual != GEXT`
- `PrecondicionVioladaException` si modo es `REINGRESO_PARA_CIERRE` (reservado)
- `PrecondicionVioladaException` si modo es `REINGRESO_CON_PAGO` (reservado/no aplicable)
- `PrecondicionVioladaException` si no existe gestion externa activa
- `PrecondicionVioladaException` si gestion no esta en `DERIVADA` ni `EN_CURSO`
- `PrecondicionVioladaException` si el par resultado/modo no figura en la tabla de pares habilitados
- `PrecondicionVioladaException` si `resultadoGestionExterna in [PAGO_REGISTRADO, SIN_RESULTADO]`
- `PrecondicionVioladaException` si el modo requiere `CONDENA_FIRME` y `resultadoFinal != CONDENA_FIRME`
- `PrecondicionVioladaException` si MODIFICA_MONTO con `montoResultado` nulo o <= 0
- `ActaNoEncontradaException` si acta no existe

**Pendiente para trabajo posterior (ver `99-pendientes-siguientes-slices.md`):**
- Nuevo fallo interno post-ABSUELVE (dictarFalloAbsolutorio o dictarFalloCondenatorio desde ANAL).
- Actualizacion definitiva de monto condena post-MODIFICA_MONTO (comando dedicado).
- Documentos externos (fal_documento): pendiente hasta JDBC.
- Fundamentos/observaciones (fal_observacion): pendiente hasta JDBC.

---

## Pago externo de gestion externa - Comandos, precondiciones y efectos

### RegistrarPagoExternoGestionCommand

**Campos:**
- actaId - obligatorio
- observaciones - opcional (nullable); si se informa, se incluye en FalActaEvento.descripcion
  como puente transitorio hasta implementar FalObservacion via JDBC.
  Las observaciones textuales NO se persisten como columna en fal_acta_gestion_externa.
  El modelo MariaDB base ya define fal_observacion con entidad_tipo=10=GESTION_EXTERNA.

**Precondiciones:**
1. Acta existe
2. acta.situacionAdministrativa == EN_GESTION_EXTERNA
3. acta.bloqueActual == GEXT
4. Existe FalGestionExterna con siActiva == true
5. FalGestionExterna.estadoGestionExterna in [DERIVADA, EN_CURSO]
6. acta.resultadoFinal == CONDENA_FIRME
7. No existe FalPagoCondena en estado CONFIRMADO
8. Acta no esta cerrada, anulada, archivada

**Efectos sobre FalGestionExterna:**
- siActiva = false
- estadoGestionExterna = CERRADA_EXTERNA
- resultadoGestionExterna = PAGO_REGISTRADO
- fechaCierreGestionExterna = LocalDateTime.now()
- fechaReingreso / motivoReingreso / observacionesReingreso no se tocan (quedan null)

**Efectos sobre FalActa:**
- acta.resultadoFinal = CONDENA_FIRME_PAGADA (siempre, independiente de bloqueantes)
- Si no hay bloqueantes:
  - acta.situacionAdministrativa = CERRADA
  - acta.bloqueActual = CERR
  - Registra CIERRA (despues de PAGAPR)
  - Snapshot: CERRADAS / NINGUNA
- Si hay bloqueantes:
  - acta.situacionAdministrativa = ACTIVA
  - acta.bloqueActual = ANAL
  - No registra CIERRA
  - Snapshot: PENDIENTE_ANALISIS / NINGUNA
  - El motor real de bloqueantes (ver seccion "Motor real de bloqueantes materiales") determina esta condicion.

**Efectos sobre FalPagoCondena:**
- No se toca. PAGAPR es un camino de pago externo independiente del flujo interno PCOINF/PCOCNF.
- Si existe FalPagoCondena INFORMADO/OBSERVADO: queda como historico, no se modifica.
- Despues de PAGAPR, resultadoFinal = CONDENA_FIRME_PAGADA, por lo que InformarPagoCondena
  y ConfirmarPagoCondena fallan (requieren resultadoFinal == CONDENA_FIRME).

**Eventos:**
- PAGAPR: siempre, si pasan precondiciones. Primer evento registrado.
- CIERRA: solo si no hay bloqueantes activos. Registrado despues de PAGAPR.
- No se emite EXTRET, EXTDER ni ningun otro evento.

**Errores esperados:**
- PrecondicionVioladaException si acta no en EN_GESTION_EXTERNA
- PrecondicionVioladaException si bloqueActual != GEXT
- PrecondicionVioladaException si no existe gestion externa activa
- PrecondicionVioladaException si estadoGestionExterna no es DERIVADA ni EN_CURSO
- PrecondicionVioladaException si resultadoFinal != CONDENA_FIRME
- PrecondicionVioladaException si existe FalPagoCondena CONFIRMADO
- PrecondicionVioladaException si acta cerrada, anulada o archivada
- ActaNoEncontradaException si acta no existe

---

## Motor real de bloqueantes materiales

### ConfirmarPagoCondena - comportamiento vigente

PCOCNF se registra SIEMPRE cuando las precondiciones se cumplen.
Los bloqueantes solo determinan si se emite o no CIERRA.

Precondiciones:
- Pago de condena existe y estado es INFORMADO
- Acta no esta cerrada
- resultadoFinal == CONDENA_FIRME

Efectos:
- Estado pago condena -> CONFIRMADO (siempre)
- acta.resultadoFinal = CONDENA_FIRME_PAGADA (siempre)
- Registra PCOCNF (siempre, primer evento)

Sin bloqueantes activos:
- acta.situacionAdministrativa = CERRADA
- acta.bloqueActual = CERR
- Registra CIERRA (despues de PCOCNF)
- Snapshot: CERRADAS / NINGUNA

Con bloqueantes activos:
- acta.situacionAdministrativa = ACTIVA
- acta.bloqueActual = ANAL
- NO registra CIERRA
- Snapshot: PENDIENTE_ANALISIS / NINGUNA

Este comportamiento sigue el mismo patron que `RegistrarPagoExternoGestionCommand` (PAGAPR).

### Puerto BloqueantesMaterialesChecker - motor real

Implementacion productiva: RepositoryBloqueantesMaterialesChecker (@Component)
Implementacion de test: NoOpBloqueantesMaterialesChecker (sin @Component, instanciar con new)

RepositoryBloqueantesMaterialesChecker delega en BloqueanteMaterialRepository.existsActivoByActaId(actaId).
NoOpBloqueantesMaterialesChecker ya no es @Component. Solo usable en tests directamente.

---

## Gestion minima de bloqueantes materiales - Comandos, precondiciones y efectos

### RegistrarBloqueanteMaterialCommand

**Campos:** actaId, origen (OrigenBloqueanteMaterial)

**Precondiciones:**
- actaId obligatorio y no vacio
- origen obligatorio (no nulo)

**Efectos:**
- Crea FalBloqueanteMaterial con estado=PENDIENTE, siActivo=true, fechaAlta=now, fechaCierre=null
- Persiste en BloqueanteMaterialRepository
- NO emite evento de FalActa
- NO modifica el snapshot del acta
- existsActivoByActaId(actaId) devuelve true inmediatamente despues

**Errores:**
- PrecondicionVioladaException si actaId nulo o vacio
- PrecondicionVioladaException si origen nulo

---

### CumplirBloqueanteMaterialCommand

**Campos:** bloqueanteId

**Precondiciones:**
- El bloqueante debe existir (BloqueanteMaterialNoEncontradoException si no existe)
- Si ya esta CUMPLIDO: operacion idempotente, devuelve el bloqueante sin modificar
- Si esta ANULADO: lanza PrecondicionVioladaException

**Efectos (si PENDIENTE):**
- estado = CUMPLIDO
- siActivo = false
- fechaCierre = now
- Persiste en BloqueanteMaterialRepository
- existsActivoByActaId(actaId) pasa a false si era el unico bloqueante activo
- Se evalua cierre diferido (ver "Cierre diferido automatico")

**Efectos (si ya CUMPLIDO - idempotente):**
- Devuelve el bloqueante sin modificaciones

**Errores:**
- BloqueanteMaterialNoEncontradoException si bloqueanteId no existe
- PrecondicionVioladaException si estado = ANULADO

---

### AnularBloqueanteMaterialCommand

**Campos:** bloqueanteId

**Precondiciones:**
- El bloqueante debe existir (BloqueanteMaterialNoEncontradoException si no existe)
- Si ya esta ANULADO: operacion idempotente, devuelve el bloqueante sin modificar
- Si esta CUMPLIDO: lanza PrecondicionVioladaException

**Efectos (si PENDIENTE):**
- estado = ANULADO
- siActivo = false
- fechaCierre = now
- Persiste en BloqueanteMaterialRepository
- existsActivoByActaId(actaId) pasa a false si era el unico bloqueante activo
- Se evalua cierre diferido (ver "Cierre diferido automatico")

**Efectos (si ya ANULADO - idempotente):**
- Devuelve el bloqueante sin modificaciones

**Errores:**
- BloqueanteMaterialNoEncontradoException si bloqueanteId no existe
- PrecondicionVioladaException si estado = CUMPLIDO

---

### Regla de cierre

- existsActivoByActaId(actaId) consulta solo bloqueantes con siActivo=true.
- Bloqueantes CUMPLIDO o ANULADO tienen siActivo=false y NO impiden CIERRA.
- Solo bloqueantes con estado PENDIENTE y siActivo=true impiden el cierre.

---

## Cierre diferido en CumplirBloqueante y AnularBloqueante

### CumplirBloqueanteMaterial - cierre diferido

Despues de resolver el estado a CUMPLIDO, se ejecuta `intentarCierreDiferido`:

- **Precondicion de cierre diferido:**
  - No quedan bloqueantes activos para el acta.
  - El acta existe en el repositorio.
  - El acta NO esta cerrada/anulada.
  - `resultadoFinal` es cerrable (PAGO_VOLUNTARIO_PAGADO, ABSUELTO, CONDENA_FIRME_PAGADA).
  - No existe ya un evento CIERRA en la historia del acta.

- **Efectos si se cumplen todas las precondiciones:**
  - Acta: situacionAdministrativa = CERRADA, bloqueActual = CERR.
  - Evento CIERRA registrado.
  - Snapshot recalculado.

- **Si alguna precondicion no se cumple:** no se emite CIERRA, sin efecto adicional, sin error.

### AnularBloqueanteMaterial - cierre diferido

Identico al de CumplirBloqueanteMaterial en cuanto a cierre diferido.

### Invariantes de cierre diferido

- No existe un evento propio de cumplimiento/anulacion de bloqueante (no se inventa).
- El unico evento emitido por el camino diferido es CIERRA.
- Idempotencia: si la operacion cumplir/anular ya fue aplicada (bloqueante ya en estado final),
  el retorno temprano impide que se llame `intentarCierreDiferido`, evitando duplicados.
- Guard adicional: `yaTieneCierre()` verifica la historia de eventos ante race conditions.

---

## Numeracion documental para integracion con Firmas

### NumerarDocumentoParaFirmas

**Operacion:** `NumerarDocumentoParaFirmas`
**Comando:** `NumerarDocumentoCommand(documentoId, actor)`
**Servicio:** `DocumentoService.numerarDocumentoParaFirmas`

**Clasificacion:**
- Capacidad documental central reutilizable.
- Invocada internamente por los flujos documentales del sistema.
- Endpoint de integracion controlada para la aplicacion de Firmas.
- No es API generica para obtener correlativos de talonario.

**Precondiciones:**
- Documento existente (`DocumentoNoEncontradoException` si no existe).
- La plantilla del documento requiere numeracion (`si_requiere_numeracion = true`).
- Documento no anulado y en estado compatible con la operacion.
- Actor autenticado obtenido del token Bearer (JWT `sub`) via `ActorContextHolder`.
- `MomentoNumeracionDocu.AL_FIRMAR` para una primera numeracion solicitada por la aplicacion de Firmas.
- Documentos ya numerados en momentos anteriores se devuelven idempotentemente.
- Politica, ambito y talonario vigentes resolubles para el documento.

**Efectos:**
- Si el documento ya esta numerado (`nroDocu != null`): devuelve el mismo numero sin consumir correlativo ni duplicar movimiento de talonario ni evento; `yaEstabaNumerado = true`.
- Si corresponde numerar: delega en la capacidad central de numeracion; asigna `nroDocu`, `idTalonario` y `nroTalonarioUsado`; registra el movimiento de talonario correspondiente.
- No genera un segundo evento principal independiente de la operacion documental envolvente.
- Preserva el orden obligatorio: numerar -> contenido definitivo/hash -> firma.

**Concurrencia:**
- InMemory actual: metodo de instancia `synchronized` (monitor `this`) en `numerarDocumentoParaFirmas`; serializa llamadas concurrentes sobre la misma instancia de `DocumentoService`, no un monitor estatico JVM-wide. No constituye un mecanismo de concurrencia multiinstancia.
- MariaDB (adapter futuro): `SEQUENCE` asociada al talonario para obtener el correlativo electronico; transaccion que vincule el numero al documento y registre el movimiento de talonario; control optimista mediante `fal_documento.version_row`; constraints unicas del modelo; recarga posterior a conflicto OCC. Garantia observable: un unico numero definitivo por documento, valido en multiples instancias de la API.

**Errores reales existentes:**
- `DocumentoNoEncontradoException` -> HTTP 404.
- `PrecondicionVioladaException` -> HTTP 422 (plantilla sin numeracion, momento incompatible, estado incompatible, talonario no vigente).

## Tiempo determinista
- Toda operacion productiva obtiene fecha/hora desde FaltasClock inyectado.
- Una operacion atomica usa un unico LocalDateTime ahora = faltasClock.now() compartido entre entidad, evento, snapshot y auditoria cuando corresponde.
- Prohibido LocalDateTime.now() / LocalDate.now() / Instant.now() directo en src/main/java fuera de allowlist.

---

## Firma y cola notificatoria del fallo

Ver especificacion completa en `10-domain/firma-notificacion-fallo.md`.

### RegistrarFirmaDocumental (callback de Firmas)

**Operacion:** `RegistrarFirmaDocumental`
**Endpoint:** `POST /api/faltas/documentos/{documentoId}/firmar-real`
**Actor:** extraido de JWT Bearer (`ActorContextHolder.get().sub()`). No acepta `idUserFirma` en body.
**Idempotencia:** por `referenciaFirmaExt` (obligatoria, @NotBlank).

**Precondiciones:**
- Documento existente (`DocumentoNoEncontradoException` si no).
- `referenciaFirmaExt` no nula ni blank.
- Si ya existe firma con esa referencia y los datos son incompatibles: `PrecondicionVioladaException`.

**Efectos (primera vez - HTTP 201):**
- Crea `FalDocumentoFirma`.
- Si todas las firmas obligatorias quedan completas: llama `completarFirmaDocumento`.
  - `FalDocumento.estadoDocu = FIRMADO`.
  - `FalActaFallo.marcarPendienteNotificacion(ahora)` si el documento pertenece al fallo activo.
  - Prepara `FalNotificacion` en `PENDIENTE_ENVIO` si la plantilla es notificable y no hay notificacion activa.
  - Evento `DOCFIR`.
  - Recalculo de snapshot.

**Efectos (idempotente - HTTP 200):**
- Ninguno. Devuelve la firma existente sin crear registros ni disparar eventos.

### EnviarNotificacion (reutilizacion de cola)

**Comportamiento vigente:**
- Antes de crear una nueva `FalNotificacion`, busca una existente en `PENDIENTE_ENVIO` para el documento.
- Si existe: la reutiliza mediante `notif.iniciarEnvio(canal, ahora, ahora, "SISTEMA")`.
- Si no existe: crea una nueva en `EN_PROCESO` (flujo directo, sin cola previa).
- No se duplican notificaciones para el mismo documento.

### GenerarLoteDesdePendientes

**Metodo:** `LoteCorreoService.generarLoteDesdePendientes(loteCodigo, referenciaExterna, guidLoteExt, idUser)`

**Precondiciones:**
- Debe existir al menos una notificacion en `PENDIENTE_ENVIO` (`PrecondicionVioladaException` si no).
- `loteCodigo` no duplicado.

**Efectos:**
- Crea `FalLoteCorreo` en estado `GENERADO`.
- Para cada notificacion en `PENDIENTE_ENVIO`: crea `FalNotificacionIntento` y avanza notificacion a `EN_PROCESO`.
- Emite evento `LOTGEN` por acta afectada.
