 03 - Comandos, Precondiciones y Efectos

## Slice 1 - Ciclo base

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
- **Precondiciones**: documento existe y no esta firmado
- **Efectos**: FalDocumento -> FIRMADO, crea FalDocumentoFirma, evento DOCFIR
- **Efecto adicional Slice 3A**: si el documento es FALLO_ABSOLUTORIO o FALLO_CONDENATORIO, actualiza FalActaFallo.estadoFallo -> FIRMADO
- NO cierra el acta en ningun caso (ni fallo ni pieza inicial)

### EnviarNotificacion
- **Precondiciones**: acta no cerrada, documento firmado
- **Efectos**: crea FalNotificacion EN_PROCESO, bloque -> NOTI, evento NOTENV

### RegistrarNotificacionPositiva
- **Precondiciones**: notificacion existe, sin resultado previo
- **Efectos segun tipo de documento notificado** (Slice 3A):
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
     - Acta queda preparada para slice de apelacion/firmeza/pago condena

### RegistrarNotificacionNegativa
- **Precondiciones**: notificacion existe, sin resultado previo
- **Efectos**: notificacion -> CON_ACUSE_NEGATIVO, evento NOTNEG

### RegistrarNotificacionVencida
- **Precondiciones**: notificacion existe, sin resultado previo
- **Efectos**: notificacion -> VENCIDA, evento NOTVNC

---

## Slice 2A/2B - Pago Voluntario

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
  - NO emite PAGCMP: no hay adjunto/evidencia real del comprobante en este slice

### ConfirmarPagoVoluntario
- **Command**: `ConfirmarPagoVoluntarioCommand(actaId, observaciones)`
- **Precondiciones**:
  - Pago voluntario en PENDIENTE_CONFIRMACION
  - Acta no cerrada
  - BloqueantesMaterialesChecker.tieneBloqueantesActivos() == false
    (Si hay bloqueantes activos: lanza PrecondicionVioladaException, NO registra ningun evento)
- **Efectos** (solo si sin bloqueantes):
  - FalPagoVoluntario -> CONFIRMADO
  - acta.resultadoFinal = PAGO_VOLUNTARIO_CONFIRMADO
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

## Puerto BloqueantesMaterialesChecker (Slice 2B)

Interfaz: `ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker`
Metodo: `boolean tieneBloqueantesActivos(String actaId)`
Implementacion productiva (Slice 7A): `RepositoryBloqueantesMaterialesChecker` (@Component)

Semantica del puerto:
- `true` = existen bloqueantes activos, accion bloqueada
- `false` = sin bloqueantes, accion permitida

Uso: ConfirmarPagoVoluntario, RegistrarNotificacionPositiva para fallo absolutorio.
Para tests: lambda `actaId -> true` para simular bloqueantes activos.

Motor real activo desde Slice 7A. Ver seccion Slice 7A de este archivo.

---

## Slice 3A - Fallo absolutorio y condenatorio minimo

### DictarFalloAbsolutorioCommand
- **Command**: `DictarFalloAbsolutorioCommand(actaId, fundamentos, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Acta esta en bloque ANAL
  - No hay pago voluntario confirmado
  - No hay fallo activo ya dictado sobre la misma acta
- **Efectos**:
  - Crea FalActaFallo con tipoFallo=ABSOLUTORIO, estadoFallo=DICTADO
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
  - Crea FalActaFallo con tipoFallo=CONDENATORIO, estadoFallo=DICTADO, montoCondena guardado
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
Dato temporal del slice. En produccion sera reemplazado por integracion real
con el sistema de Ingresos/Tesoreria (Cmte_PG / Pref_PG / Nro_PG).

### PAGCMP (comprobante adjunto)
El evento PAGCMP existe en el enum pero no se emite en `InformarPagoVoluntario`.
Se reserva para slice posterior de adjuntos/comprobantes reales.
Emitirlo sin adjunto real seria semanticamente incorrecto.

### Fallo condenatorio post-notificacion
Luego de registrar notificacion positiva de FALLO_CONDENATORIO, el acta queda
con resultadoFinal=SIN_RESULTADO_FINAL y fallo NOTIFICADO.
Esta es la situacion minima correcta para Slice 3A.
Los pasos siguientes (plazo de apelacion, apelacion, firmeza, pago condena) se implementan en slices posteriores.

### CONDENA_FIRME
No existe todavia como valor productivo de ResultadoFinalActa.
Se reserva para el slice de firmeza/apelacion (post-3A).
---

## Slice 3B - Apelacion presentada

### RegistrarApelacionCommand
- **Command**: `RegistrarApelacionCommand(actaId, presentante, fundamentos, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Existe fallo activo sobre el acta
  - El fallo activo es CONDENATORIO
  - El fallo condenatorio esta en estado NOTIFICADO
  - No existe apelacion activa previa sobre la misma acta
  - No existe CONDENA_FIRME (no aplica en Slice 3B, pero validado para slices futuros)
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
- `APERAZ` y `APEABS` son eventos productivos desde Slice 3C.
- `TipoEventoActa.deCodigo("APERAZ")` resuelve como apelacion rechazada.
- `TipoEventoActa.deCodigo("APEABS")` resuelve como apelacion aceptada absuelve.

### Calculo de plazo
No implementado en Slice 3B. Se reserva para Slice 3C.

### Resolucion de apelacion (APERAZ / APEABS)
Implementado en Slice 3C. Ver seccion Slice 3C mas abajo.

---

## Slice 3C - Resolucion de apelacion

### ResolverApelacionRechazadaCommand
- **Command**: `ResolverApelacionRechazadaCommand(actaId, fundamentosResolucion, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Existe apelacion activa sobre el acta
  - La apelacion esta en estado PRESENTADA
  - Existe fallo activo
  - El fallo activo es CONDENATORIO
  - El fallo condenatorio esta en estado NOTIFICADO
  - No existe condena firme (ResultadoFinalActa != CONDENA_FIRME, no existe todavia)
  - No existe pago de condena confirmado (no implementado todavia)
  - No existe gestion externa activa (no implementado todavia)
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

### Deudas tecnicas Slice 3C

- PLAVNC (vencimiento de plazo de apelacion): Slice 3D
- CONFIR (confirmacion de condena firme): Slice 3D
- CONDENA_FIRME como ResultadoFinalActa: Slice 3D
- Pago de condena: Slice 5
- Gestion externa: Slice 3F
---

## Slice 4 - Firmeza de condena

### VencerPlazoApelacionCommand
- **Command**: `VencerPlazoApelacionCommand(actaId, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Existe fallo activo
  - El fallo activo es CONDENATORIO
  - El fallo condenatorio esta en estado NOTIFICADO
  - No existe apelacion (ninguna ultima apelacion: ni PRESENTADA ni RECHAZADA ni otra)
  - No existe CONDENA_FIRME (resultadoFinal != CONDENA_FIRME)
- **Efectos**:
  - Registra evento PLAVNC (plazo de apelacion vencido)
  - Registra evento CONFIR (condena firme) despues de PLAVNC
  - FalActa.resultadoFinal -> CONDENA_FIRME
  - Crea FalActaFirmezaCondena con origenFirmeza=VENCIMIENTO_PLAZO_APELACION
  - Snapshot: PENDIENTE_PAGO_CONDENA / GESTIONAR_PAGO_CONDENA
- **No cierra el acta**
- **No registra CIERRA**
- **No inicia pago condena**
- **No crea obligacion de pago**

### DeclararCondenaFirmePorApelacionRechazadaCommand
- **Command**: `DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, observaciones)`
- **Precondiciones**:
  - Acta existe
  - Acta no esta cerrada, anulada, archivada ni paralizada
  - Existe fallo activo
  - El fallo activo es CONDENATORIO
  - El fallo condenatorio esta en estado NOTIFICADO
  - Existe ultima apelacion sobre el acta
  - La ultima apelacion esta en estado RECHAZADA
  - No existe apelacion en estado PRESENTADA
  - No existe CONDENA_FIRME (resultadoFinal != CONDENA_FIRME)
- **Efectos**:
  - Registra evento CONFIR (condena firme)
  - No registra PLAVNC (la firmeza viene por apelacion rechazada, no por vencimiento de plazo)
  - FalActa.resultadoFinal -> CONDENA_FIRME
  - Crea FalActaFirmezaCondena con origenFirmeza=APELACION_RECHAZADA, apelacionId asignado
  - Snapshot: PENDIENTE_PAGO_CONDENA / GESTIONAR_PAGO_CONDENA
- **No cierra el acta**
- **No registra CIERRA**
- **No inicia pago condena**
- **No crea obligacion de pago**

### Deudas tecnicas Slice 4

- Pago de condena (PCOINF/PCOCNF/PCOOBS): Slice 5
- Informar pago condena (PCOINF): Slice 5
- Confirmar pago condena (PCOCNF): Slice 5
- Gestion externa (EXTDER/EXTRET/PAGAPR): Slice 6 - NO usar DRVEXT
- Calculo real de plazos de apelacion: Slice futuro de integracion
- Cierre definitivo por condena firme pagada: Slice 5/posterior

---

## Slice 5: Pago de condena - Comandos, precondiciones y efectos

### InformarPagoCondenaCommand

**Campos:** actaId, monto, referenciaPago, observaciones

**Precondiciones:**
- Acta existe y no esta cerrada, anulada, archivada ni paralizada
- `resultadoFinal == CONDENA_FIRME`
- Existe fallo condenatorio con estado `NOTIFICADO`
- `monto > 0`
- `referenciaPago` obligatoria y no vacia
- No existe pago de condena `CONFIRMADO` previo

**Efectos:**
- Crea o actualiza `FalPagoCondena` con estado `INFORMADO`
- Registra evento `PCOINF`
- NO cierra el acta
- NO registra `CIERRA`
- Recalcula snapshot

### ConfirmarPagoCondenaCommand

**Campos:** actaId, observaciones

**Precondiciones:**
- Pago de condena existe y estado es `INFORMADO`
- Acta no esta cerrada
- `resultadoFinal == CONDENA_FIRME`
- No hay bloqueantes materiales activos (`BloqueantesMaterialesChecker`)

**Efectos:**
- Estado pago condena -> `CONFIRMADO`
- `acta.resultadoFinal = CONDENA_FIRME_PAGADA`
- `acta.situacionAdministrativa = CERRADA`
- `acta.bloqueActual = CERR`
- Registra `PCOCNF` (primero)
- Registra `CIERRA` (despues)
- Snapshot: `CERRADAS / NINGUNA`

**Si hay bloqueantes activos:**
- Lanza `PrecondicionVioladaException`
- NO registra ningun evento
- NO cierra el acta
- NO modifica el pago

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

## Slice 6B: Reingreso desde gestion externa - Comandos, precondiciones y efectos

### ReingresarDesdeGestionExternaCommand

**Campos:**
- `actaId` — obligatorio
- `modoReingresoGestionExterna` — obligatorio; debe ser `REINGRESO_PARA_REVISION` o `REINGRESO_SIN_PAGO`
- `motivoReingreso` — obligatorio, no vacio
- `resultadoGestionExterna` — opcional (nullable); si se informa se persiste en `FalGestionExterna`
- `observaciones` — opcional (nullable)

**Precondiciones:**
- Acta existe
- `modoReingresoGestionExterna` no nulo
- `motivoReingreso` no nulo ni vacio
- `acta.situacionAdministrativa == EN_GESTION_EXTERNA`
- `acta.bloqueActual == GEXT`
- `modoReingresoGestionExterna` es habilitado (no `REINGRESO_PARA_CIERRE`, no `REINGRESO_PARA_NUEVO_FALLO`, no nulo)
- Existe `FalGestionExterna` activa (`siActiva == true`)
- `FalGestionExterna.estadoGestionExterna in [DERIVADA, EN_CURSO]`
- Si `modo == REINGRESO_SIN_PAGO`: `acta.resultadoFinal == CONDENA_FIRME`
- Acta no cerrada, no anulada, no archivada

**Efectos:**
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

**Errores esperados:**
- `PrecondicionVioladaException` si modo nulo
- `PrecondicionVioladaException` si motivo nulo o vacio
- `PrecondicionVioladaException` si acta no esta en `EN_GESTION_EXTERNA`
- `PrecondicionVioladaException` si `bloqueActual != GEXT`
- `PrecondicionVioladaException` si modo es `REINGRESO_PARA_CIERRE` (reservado)
- `PrecondicionVioladaException` si modo es `REINGRESO_PARA_NUEVO_FALLO` (reservado)
- `PrecondicionVioladaException` si no existe gestion externa activa
- `PrecondicionVioladaException` si gestion no esta en `DERIVADA` ni `EN_CURSO`
- `PrecondicionVioladaException` si `REINGRESO_SIN_PAGO` y `resultadoFinal != CONDENA_FIRME`
- `ActaNoEncontradaException` si acta no existe

**Modos prohibidos temporalmente:**
- `REINGRESO_PARA_CIERRE`: reservado para slice futuro
- `REINGRESO_PARA_NUEVO_FALLO`: reservado para slice futuro


---

## Slice 6C: Pago externo de gestion externa - Comandos, precondiciones y efectos

### RegistrarPagoExternoGestionCommand

**Campos:**
- actaId - obligatorio
- observaciones - opcional (nullable); si se informa, se incluye en FalActaEvento.descripcion
  como puente transitorio hasta implementar FalObservacion en Slice 9/JDBC.
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
  - Motor real activo desde Slice 7A.

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
- PrecondicionVioladaException si 
esultadoFinal != CONDENA_FIRME
- PrecondicionVioladaException si existe FalPagoCondena CONFIRMADO
- PrecondicionVioladaException si acta cerrada, anulada o archivada
- ActaNoEncontradaException si acta no existe



---

## Slice 6D-1: Reingreso sin pago y sin cambios - Validacion de pares resultado/modo

### ReingresarDesdeGestionExternaCommand - validacion de pares (Slice 6D-1)

Extiende Slice 6B. Agrega validacion estricta del par `resultadoGestionExterna` + `modoReingresoGestionExterna`
cuando `resultadoGestionExterna` es no nulo.

**Pares habilitados en Slice 6D-1:**

| resultadoGestionExterna | modoReingresoGestionExterna | Descripcion |
|------------------------|----------------------------|-------------|
| `SIN_PAGO`             | `REINGRESO_SIN_PAGO`        | Vuelve sin pago; continua circuito interno de cobro |
| `SIN_CAMBIOS`          | `REINGRESO_PARA_REVISION`   | Vuelve sin cambios sustantivos; revision interna |

**Pares habilitados en Slice 6D-2 (ver seccion siguiente):**

| resultadoGestionExterna | modoReingresoGestionExterna |
|------------------------|-----------------------------|
| `ABSUELVE`             | `REINGRESO_PARA_NUEVO_FALLO`|
| `CONFIRMA_CONDENA`     | `REINGRESO_CON_DICTAMEN`    |
| `MODIFICA_MONTO`       | `REINGRESO_CON_DICTAMEN`    |

**Pares todavia reservados (fallan con PrecondicionVioladaException):**

| resultadoGestionExterna | modoReingresoGestionExterna |
|------------------------|-----------------------------|
| cualquiera             | `REINGRESO_PARA_CIERRE`     |

**Nota sobre REINGRESO_CON_PAGO:**
`REINGRESO_CON_PAGO` queda reservado/no aplicable al flujo `PAGAPR` actual.
`PAGAPR` cierra la gestion con `estadoGestionExterna = CERRADA_EXTERNA` sin usar modo de reingreso.
`REINGRESO_CON_PAGO` queda bloqueado y reservado para un slice futuro.

**Precondiciones adicionales (Slice 6D-1):**
- Si `resultadoGestionExterna` es no nulo:
  - Desde Slice 6D-2: puede ser `ABSUELVE`, `CONFIRMA_CONDENA` o `MODIFICA_MONTO` (habilitados con par correcto)
  - No puede ser `PAGO_REGISTRADO` (asignado automaticamente por PAGAPR)
  - No puede ser `SIN_RESULTADO` (estado inicial al derivar)
  - Si resultado es `SIN_PAGO`: modo debe ser `REINGRESO_SIN_PAGO`
  - Si resultado es `SIN_CAMBIOS`: modo debe ser `REINGRESO_PARA_REVISION`

**Caso A — SIN_PAGO / REINGRESO_SIN_PAGO:**
- Precondiciones: Caso base + `resultadoFinal == CONDENA_FIRME` (ya existia en 6B)
- Efectos: EXTRET emitido; `FalGestionExterna.resultadoGestionExterna = SIN_PAGO`;
  `modoReingresoGestionExterna = REINGRESO_SIN_PAGO`; `fechaReingreso = now`;
  NO `fechaCierreGestionExterna`; NO `estadoGestionExterna = CERRADA_EXTERNA`;
  `FalActa.resultadoFinal` permanece `CONDENA_FIRME`; `situacionAdministrativa = ACTIVA`; `bloqueActual = ANAL`
- No emite PAGAPR, CIERRA ni PCOCNF

**Caso B — SIN_CAMBIOS / REINGRESO_PARA_REVISION:**
- Precondiciones: Caso base
- Efectos: EXTRET emitido; `FalGestionExterna.resultadoGestionExterna = SIN_CAMBIOS`;
  `modoReingresoGestionExterna = REINGRESO_PARA_REVISION`; `fechaReingreso = now`;
  NO `fechaCierreGestionExterna`; NO `estadoGestionExterna = CERRADA_EXTERNA`;
  `FalActa.resultadoFinal` permanece `CONDENA_FIRME`; `situacionAdministrativa = ACTIVA`; `bloqueActual = ANAL`
- No emite PAGAPR, CIERRA ni PCOCNF

**Errores adicionales (Slice 6D-1):**
- `PrecondicionVioladaException` si `resultadoGestionExterna in [ABSUELVE, CONFIRMA_CONDENA, MODIFICA_MONTO]`
- `PrecondicionVioladaException` si `resultadoGestionExterna == PAGO_REGISTRADO`
- `PrecondicionVioladaException` si `resultadoGestionExterna == SIN_RESULTADO`
- `PrecondicionVioladaException` si par incoherente: `SIN_PAGO + REINGRESO_PARA_REVISION`
- `PrecondicionVioladaException` si par incoherente: `SIN_CAMBIOS + REINGRESO_SIN_PAGO`
---

## Slice 7A: Actualizacion de ConfirmarPagoCondena y motor de bloqueantes

### ConfirmarPagoCondena - comportamiento actualizado (Slice 7A)

PCOCNF se registra SIEMPRE cuando las precondiciones se cumplen.
Los bloqueantes solo determinan si se emite o no CIERRA.

Precondiciones (sin cambios):
- Pago de condena existe y estado es INFORMADO
- Acta no esta cerrada
- resultadoFinal == CONDENA_FIRME

Efectos (actualizados en Slice 7A):
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

Nota: antes de Slice 7A, el comportamiento era lanzar PrecondicionVioladaException sin mutar.
Desde Slice 7A, se alinea con el patron de PAGAPR (Slice 6C).

### Puerto BloqueantesMaterialesChecker - motor real implementado (Slice 7A)

Implementacion productiva: RepositoryBloqueantesMaterialesChecker (@Component)
Implementacion de test: NoOpBloqueantesMaterialesChecker (sin @Component, instanciar con new)

RepositoryBloqueantesMaterialesChecker delega en BloqueanteMaterialRepository.existsActivoByActaId(actaId).
NoOpBloqueantesMaterialesChecker ya no es @Component. Solo usable en tests directamente.

Motor real: activo desde Slice 7A. No es mas deuda tecnica.

### PAGAPR - nota actualizada

La nota "Estado transitorio hasta Slice 7" en la seccion de PAGAPR queda obsoleta.
El motor real de bloqueantes esta implementado desde Slice 7A.
El comportamiento de PAGAPR con bloqueantes es el definitivo: PAGAPR siempre, CIERRA solo si sin bloqueantes.

---

## Slice 7B: Gestion minima de bloqueantes materiales - Comandos, precondiciones y efectos

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

**Efectos (si ya ANULADO - idempotente):**
- Devuelve el bloqueante sin modificaciones

**Errores:**
- BloqueanteMaterialNoEncontradoException si bloqueanteId no existe
- PrecondicionVioladaException si estado = CUMPLIDO

---

### Regla de cierre (confirmada Slice 7B)

- existsActivoByActaId(actaId) consulta solo bloqueantes con siActivo=true.
- Bloqueantes CUMPLIDO o ANULADO tienen siActivo=false y NO impiden CIERRA.
- Solo bloqueantes con estado PENDIENTE y siActivo=true impiden el cierre.

---

## Slice 7C: Cierre diferido en CumplirBloqueante y AnularBloqueante (implementado)

### CumplirBloqueanteMaterial (comportamiento extendido en Slice 7C)

Despues de resolver el estado a CUMPLIDO, se ejecuta `intentarCierreDiferido`:

- **Precondicion de cierre diferido:**
  - No quedan bloqueantes activos para el acta.
  - El acta existe en el repositorio.
  - El acta NO esta cerrada/anulada.
  - `resultadoFinal` es cerrable (PAGO_VOLUNTARIO_CONFIRMADO, ABSUELTO, CONDENA_FIRME_PAGADA).
  - No existe ya un evento CIERRA en la historia del acta.

- **Efectos si se cumplen todas las precondiciones:**
  - Acta: situacionAdministrativa = CERRADA, bloqueActual = CERR.
  - Evento CIERRA registrado.
  - Snapshot recalculado.

- **Si alguna precondicion no se cumple:** no se emite CIERRA, sin efecto adicional, sin error.

### AnularBloqueanteMaterial (comportamiento extendido en Slice 7C)

Identico al de CumplirBloqueanteMaterial en cuanto a cierre diferido.

### Invariantes de cierre diferido

- No existe un evento propio de cumplimiento/anulacion de bloqueante (no se inventa).
- El unico evento emitido por el camino diferido es CIERRA.
- Idempotencia: si la operacion cumplir/anular ya fue aplicada (bloqueante ya en estado final),
  el retorno temprano impide que se llame `intentarCierreDiferido`, evitando duplicados.
- Guard adicional: `yaTieneCierre()` verifica la historia de eventos ante race conditions.


---

## Slice 6D-2: Reingreso con dictamen externo

### ReingresarDesdeGestionExternaCommand - dictamen externo (Slice 6D-2)

Extiende Slice 6D-1. Habilita los tres casos de reingreso con dictamen externo desde instancia externa
(juzgado de paz, apremio, otro organismo).

**Nuevo campo en comando y DTO:**

| Campo | Tipo | Obligatorio | Descripcion |
|-------|------|-------------|-------------|
| `montoResultado` | `BigDecimal` | Solo para MODIFICA_MONTO (> 0) | Monto externo informado. Persiste en `FalGestionExterna.montoResultado` (monto_resultado en MariaDB). |

**Pares habilitados en Slice 6D-2:**

| resultadoGestionExterna | modoReingresoGestionExterna | Efecto sobre resultadoFinal |
|------------------------|----------------------------|-----------------------------|
| `ABSUELVE`             | `REINGRESO_PARA_NUEVO_FALLO` | No cambia (queda CONDENA_FIRME); nuevo fallo posterior |
| `CONFIRMA_CONDENA`     | `REINGRESO_CON_DICTAMEN`   | Se confirma CONDENA_FIRME (ya era precondicion) |
| `MODIFICA_MONTO`       | `REINGRESO_CON_DICTAMEN`   | No cambia resultadoFinal; registra montoResultado |

**Precondiciones comunes (adicionales a Slice 6B):**
- `resultadoFinal == CONDENA_FIRME` (para REINGRESO_PARA_NUEVO_FALLO y REINGRESO_CON_DICTAMEN)
- `resultadoGestionExterna` no nulo cuando modo es REINGRESO_PARA_NUEVO_FALLO o REINGRESO_CON_DICTAMEN
- Para MODIFICA_MONTO: `montoResultado` no nulo y > 0

**Efectos comunes (Slice 6D-2):**
- Evento `EXTRET` emitido
- `FalGestionExterna.siActiva = false`
- `FalGestionExterna.estadoGestionExterna = REINGRESADA`
- `FalGestionExterna.fechaReingreso = now`
- `FalGestionExterna.resultadoGestionExterna` = resultado indicado
- `FalGestionExterna.modoReingresoGestionExterna` = modo indicado
- `FalActa.situacionAdministrativa = ACTIVA`
- `FalActa.bloqueActual = ANAL`
- No emite `CIERRA`, `PAGAPR` ni `PCOCNF`
- `fechaCierreGestionExterna` no se toca (se usa en PAGAPR)

**Efectos especificos:**

**Caso ABSUELVE + REINGRESO_PARA_NUEVO_FALLO:**
- `FalActa.resultadoFinal` no cambia automaticamente
- No genera fallo absolutorio automatico
- La acta vuelve a ANAL para que internamente se dicte nuevo fallo si corresponde

**Caso CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN:**
- `FalActa.resultadoFinal = CONDENA_FIRME` (confirmado explicitamente)
- Permite continuar circuito interno de pago u otras acciones en ANAL

**Caso MODIFICA_MONTO + REINGRESO_CON_DICTAMEN:**
- `FalGestionExterna.montoResultado = montoResultado` (del comando)
- `FalActa.resultadoFinal` no cambia automaticamente
- No dicta nuevo fallo
- No pasa a pago automaticamente
- Internamente, luego del reingreso, se decidira si corresponde nuevo fallo o actualizacion de monto para continuar a pago

**Errores esperados (Slice 6D-2):**
- `PrecondicionVioladaException` si modo REINGRESO_PARA_NUEVO_FALLO o REINGRESO_CON_DICTAMEN con resultado null
- `PrecondicionVioladaException` si resultado CONDENA_FIRME no se cumple para estos modos
- `PrecondicionVioladaException` si MODIFICA_MONTO con montoResultado null
- `PrecondicionVioladaException` si MODIFICA_MONTO con montoResultado <= 0
- `PrecondicionVioladaException` si par incoherente (ABSUELVE + DICTAMEN, CONFIRMA_CONDENA + NUEVO_FALLO, etc.)
- `PrecondicionVioladaException` si REINGRESO_PARA_CIERRE (sigue reservado)

**Pendiente para slices posteriores:**
- Nuevo fallo interno post-ABSUELVE (dictarFalloAbsolutorio o dictarFalloCondenatorio desde ANAL)
- Actualizacion definitiva de monto condena post-MODIFICA_MONTO (comando dedicado)
- Documentos externos (fal_documento): pendiente hasta JDBC
- Fundamentos/observaciones (fal_observacion): pendiente hasta JDBC

---

## CIERRE-D14-D18 (2026-07-09) - Numeracion documental para integracion Firmas

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
- InMemory actual: `synchronized` en `numerarDocumentoParaFirmas`; serializa llamadas dentro de una unica instancia JVM. No constituye un mecanismo de concurrencia multiinstancia.
- MariaDB (adapter futuro): `SEQUENCE` asociada al talonario para obtener el correlativo electronico; transaccion que vincule el numero al documento y registre el movimiento de talonario; control optimista mediante `fal_documento.version_row`; constraints unicas del modelo; recarga posterior a conflicto OCC. Garantia observable: un unico numero definitivo por documento, valido en multiples instancias de la API.

**Errores reales existentes:**
- `DocumentoNoEncontradoException` -> HTTP 404.
- `PrecondicionVioladaException` -> HTTP 422 (plantilla sin numeracion, momento incompatible, estado incompatible, talonario no vigente).

## R-08 — Tiempo determinista (cerrado 2026-07-09)
- Toda operacion productiva obtiene fecha/hora desde FaltasClock inyectado.
- Una operacion atomica usa un unico LocalDateTime ahora = faltasClock.now() compartido entre entidad, evento, snapshot y auditoria cuando corresponde.
- Prohibido LocalDateTime.now() / LocalDate.now() / Instant.now() directo en src/main/java fuera de allowlist.


---

## FIX-FALLO-NOTI-01 (2026-07-10) - Firma y cola notificatoria del fallo

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

**Cambio FIX-FALLO-NOTI-01:**
- Antes de crear una nueva `FalNotificacion`, busca una existente en `PENDIENTE_ENVIO` para el documento.
- Si existe: la reutiliza mediante `notif.iniciarEnvio(canal, ahora, ahora, "SISTEMA")`.
- Si no existe: crea una nueva en `EN_PROCESO` (flujo directo, sin cola previa).
- No se duplican notificaciones para el mismo documento.

### GenerarLoteDesdePendientes (nuevo metodo canonico)

**Metodo:** `LoteCorreoService.generarLoteDesdePendientes(loteCodigo, referenciaExterna, guidLoteExt, idUser)`

**Precondiciones:**
- Debe existir al menos una notificacion en `PENDIENTE_ENVIO` (`PrecondicionVioladaException` si no).
- `loteCodigo` no duplicado.

**Efectos:**
- Crea `FalLoteCorreo` en estado `GENERADO`.
- Para cada notificacion en `PENDIENTE_ENVIO`: crea `FalNotificacionIntento` y avanza notificacion a `EN_PROCESO`.
- Emite evento `LOTGEN` por acta afectada.

---
