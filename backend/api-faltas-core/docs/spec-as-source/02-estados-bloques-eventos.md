# 02 - Estados, Bloques y Eventos

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Ante contradiccion con un documento tematico de `00-governance/` o `10-domain/`, ese documento tematico prevalece en lo que respecta a definiciones, dimensiones y lifecycle (ver README, seccion 4.0).

## Bloques productivos (BloqueActual)

| Codigo | Descripcion |
|--------|-------------|
| CAPT   | Captura/labrado inicial |
| ENRI   | Enriquecimiento del acta |
| NOTI   | Notificacion del acta, fallo u otra pieza |
| ANAL   | Analisis, resolucion, fallo, pagos y apelacion |
| GEXT   | Gestion externa: apremio / juzgado de paz |
| ARCH   | Archivo administrativo/procesal |
| CERR   | Cierre definitivo del circuito |

`D3_DOCUMENTAL` NO es un bloque. No existe como bloque productivo. No se reemplaza por otro bloque documental.
La documentalidad se modela exclusivamente mediante `FalDocumento`, `FalDocumentoFirma`, `EstadoDocumento`, `EstadoFirmaDocumento`, bandejas y snapshot.

---

## EstadoProcesalActa

| Valor | Descripcion |
|-------|-------------|
| EN_TRAMITE | Acta en tramitacion activa |
| CONCLUIDO  | Tramite concluido |
| PRESCRIPTO | Prescripto por vencimiento |

---

## SituacionAdministrativaActa

| Valor | Descripcion |
|-------|-------------|
| ACTIVA            | Acta activa, en tramite normal |
| PARALIZADA        | Paralizada temporalmente |
| EN_GESTION_EXTERNA| Derivada a gestion externa |
| ARCHIVADA         | Archivada administrativamente |
| CERRADA           | Cerrada definitivamente |
| ANULADA           | Anulada |

---

## ResultadoFinalActa

| Valor | Descripcion |
|-------|-------------|
| SIN_RESULTADO_FINAL               | Valor inicial, toda acta comienza aqui |
| PAGO_VOLUNTARIO_PAGADO            | Cerrada por pago voluntario confirmado |
| ABSUELTO                          | Cerrada por fallo absolutorio notificado |
| CONDENA_FIRME                     | Condena firme declarada por vencimiento de plazo o apelacion rechazada |
| CONDENA_FIRME_PAGADA              | Condena firme pagada (via PCOCNF o PAGAPR) |
| FALLO_CONDENATORIO_PAGADO         | LEGACY_RESERVED (codigo 5): valor compilado sin productor canonico vigente; ningun comando actual lo asigna; prohibido para nuevas escrituras funcionales; el resultado canonico del pago de condena confirmado es CONDENA_FIRME_PAGADA |
| FALLO_CONDENATORIO_GESTION_EXTERNA| Fallo condenatorio derivado a gestion externa (reservado; no asignado por el circuito PAGAPR vigente) |
| PRESCRIPTO                        | Cerrada por prescripcion |
| ANULADO                           | Anulada |
| NULIDAD                           | Nulidad declarada |

Prohibido: PAGO_VOLUNTARIO (ambiguo), PAGO_INFORMADO (no es resultado final), PAGO_VOLUNTARIO_CONFIRMADO (nombre incorrecto; el valor correcto es PAGO_VOLUNTARIO_PAGADO).
Prohibido: FALLO_ABSOLUTORIO como valor de ResultadoFinalActa. El valor correcto es ABSUELTO.

---

## EstadoPagoVoluntario

| Valor | Descripcion |
|-------|-------------|
| SIN_PAGO              | Estado inicial, sin pago iniciado |
| SOLICITADO            | Infractor solicita pagar voluntariamente |
| MONTO_FIJADO          | Organismo fija el monto a pagar |
| PENDIENTE_CONFIRMACION| Infractor informa pago, esperando confirmacion |
| CONFIRMADO            | Pago confirmado -> cierra el acta |
| OBSERVADO             | Pago rechazado/observado, requiere correccion |
| VENCIDO               | Plazo vencido, habilita analisis/fallo |

Prohibido: PAGO_INFORMADO como estado productivo.

---

## TipoFalloActa

| Valor | Descripcion |
|-------|-------------|
| ABSOLUTORIO  | Fallo que absuelve al infractor |
| CONDENATORIO | Fallo que condena al infractor |

---

## ResultadoFalloActa

El enum `ResultadoFalloActa` contiene exactamente estos 4 valores:

| Valor | Codigo | Descripcion |
|-------|--------|-------------|
| ABSUELVE | 1 | El fallo absuelve al infractor |
| CONDENA | 2 | El fallo condena al infractor |
| DECLARA_NULIDAD | 3 | El fallo declara la nulidad del acta o de una pieza |
| OTRO | 9 | Otro resultado no clasificado en los anteriores |

`ResultadoFalloActa` es el resultado concreto de un fallo dictado o de una
resolucion de apelacion; no debe confundirse con `TipoFalloActa` (clasificacion
previa al dictado) ni con `ResultadoFinalActa` (desenlace sustantivo del acta).

---

## EstadoFalloActa

El enum `EstadoFalloActa` contiene exactamente estos 6 valores (fuente normativa completa: [`10-domain/lifecycle-states.md`](10-domain/lifecycle-states.md), seccion "Lifecycle del fallo"):

| Valor | Descripcion |
|-------|-------------|
| PENDIENTE_FIRMA        | Fallo dictado; documento generado; pendiente de firma obligatoria |
| PENDIENTE_NOTIFICACION | Ultima firma obligatoria confirmada; `fhFirma` registrado |
| NOTIFICADO             | Notificacion del fallo registrada con acuse positivo; `fhNotificacion` registrado |
| FIRME                  | Firmeza de condena declarada; `fhFirmeza` registrado |
| REEMPLAZADO            | Fallo sustituido por otro (estado lateral terminal) |
| SIN_EFECTO             | Fallo anulado o dejado sin efecto (estado lateral terminal) |

Prohibido: `DICTADO` y `FIRMADO` no son valores del enum. Son hechos historicos persistidos en `fhDictado` y `fhFirma` respectivamente. No deben reintroducirse como valores de enum ni como aliases de compatibilidad.

---

## EstadoApelacionActa

El enum `EstadoApelacionActa` contiene exactamente estos 6 valores:

| Valor | Descripcion |
|-------|-------------|
| PRESENTADA        | Apelacion presentada por el infractor |
| EN_ANALISIS       | Apelacion en analisis (via `PasarApelacionAAnalisisCommand`) |
| RECHAZADA         | Apelacion rechazada/resuelta con resultado RECHAZADA; no declara firmeza por si misma; habilita la ejecucion posterior de CMD-FALLO-006 |
| ACEPTADA_ABSUELVE | Apelacion aceptada - absolucion en segunda instancia |
| RESUELTA          | Apelacion resuelta con modificacion de condena o nulidad, sin absolver |
| SIN_EFECTO        | Apelacion anulada o sin efecto |

---

## ResultadoResolucionApelacion

El enum `ResultadoResolucionApelacion` contiene exactamente estos 4 valores:

| Valor | Codigo | Descripcion |
|-------|--------|-------------|
| RECHAZADA | 1 | La apelacion se rechaza; no declara firmeza por si misma; deja pendiente CMD-FALLO-006 (Declarar condena firme por apelacion rechazada) |
| ACEPTADA_ABSUELVE | 2 | La apelacion se acepta y absuelve al infractor |
| MODIFICA_CONDENA | 3 | La apelacion se acepta y modifica la condena sin absolver |
| NULIDAD | 4 | La apelacion resulta en nulidad declarada |

Este es el resultado concreto que se informa al resolver una `FalApelacionActa`
mediante el comando de resolucion de apelacion; determina el evento emitido
(`APERAZ`, `APEABS`, `APEMCO` o `APENUL`) y el `EstadoApelacionActa` final.

---

## Actores y origenes de evento (ActorTipoEvento, OrigenEvento)

Todo `FalActaEvento` persiste dos dimensiones adicionales al actor autenticado
(`sub` del JWT): el tipo de actor y el origen tecnico del evento. Ninguna de las
dos sustituye al actor autenticado; son metadatos de trazabilidad.

### ActorTipoEvento

| Valor | Codigo | Descripcion |
|-------|--------|-------------|
| USUARIO_INTERNO | 1 | Agente/operador interno del organismo |
| INSPECTOR | 2 | Inspector de faltas |
| INFRACTOR | 3 | El infractor actuando directamente (p. ej. portal) |
| SISTEMA | 4 | Proceso automatico interno sin actor humano |
| INTEGRACION | 5 | Sistema externo integrado |
| NOTIFICADOR | 6 | Servicio o agente de notificacion |

### OrigenEvento

| Valor | Codigo | Descripcion |
|-------|--------|-------------|
| USUARIO_WEB | 1 | Interfaz web interna |
| DISPOSITIVO_MOBILE | 2 | Dispositivo movil de inspeccion |
| PROCESO_AUTOMATICO | 3 | Proceso automatico del sistema |
| INTEGRACION | 4 | Integracion con sistema externo |
| PORTAL_INFRACTOR | 5 | Portal publico del infractor |
| SERVICIO_NOTIFICACION | 6 | Servicio de notificacion |
| LOTE_CORREO | 7 | Procesamiento de lote de correo |
| SISTEMA_QR | 8 | Acceso o generacion via codigo QR |

---

## Eventos (TipoEventoActa)

`TipoEventoActa` contiene un catalogo mas amplio de eventos productivos (incluye documentacion, numeracion, acuses, QR y economia). Esta seccion documenta los eventos exigidos por el circuito de fallo, notificacion, apelacion, firmeza, pago de condena y gestion externa. El catalogo completo, codigo por codigo, esta en la seccion "Catalogo completo de TipoEventoActa" mas abajo. El catalogo fuente definitivo es evidencia de conformidad en `TipoEventoActa.java`.

### Ciclo inicial
| Codigo | Descripcion |
|--------|-------------|
| ACTLAB | Acta labrada/creada |
| ACTCAP | Captura completada (CAPT->ENRI) |
| ACTENR | Acta enriquecida |

### Documentacion y firma
| Codigo | Descripcion |
|--------|-------------|
| DOCGEN | Documento generado |
| DOCFIR | Documento firmado |

### Notificacion
| Codigo | Descripcion |
|--------|-------------|
| NOTENV | Notificacion enviada |
| NOTPOS | Notificacion con acuse positivo |
| PORPOS | Notificacion positiva por portal infractor |
| NOTNEG | Notificacion con acuse negativo |
| NOTVNC | Notificacion vencida sin acuse |

### Pago voluntario
| Codigo | Descripcion |
|--------|-------------|
| PAGVSO | Pago voluntario solicitado |
| PAGVMF | Pago voluntario monto fijado |
| PAGINF | Pago voluntario informado por el infractor |
| PAGCMP | Comprobante de pago adjuntado (pendiente: no se emite sin adjunto real) |
| PAGCNF | Pago voluntario confirmado |
| PAGOBS | Pago voluntario observado/rechazado |
| PAGVVN | Pago voluntario vencido sin confirmacion |

### Fallo
| Codigo | Descripcion |
|--------|-------------|
| FALABS | Fallo absolutorio dictado |
| FALCON | Fallo condenatorio dictado |

### Apelacion
| Codigo | Descripcion |
|--------|-------------|
| APEPRE | Apelacion presentada |
| APERAZ | Apelacion rechazada/resuelta con resultado RECHAZADA; no declara firmeza; habilita CMD-FALLO-006 |
| APEABS | Apelacion aceptada - absolucion en segunda instancia |

### Firmeza de condena
| Codigo | Descripcion |
|--------|-------------|
| PLAVNC | Plazo de apelacion vencido sin apelacion presentada |
| CONFIR | Condena firme declarada |

### Pago de condena

Ver catalogo completo, estado y transiciones en la seccion "Pago de condena" mas abajo.

| Codigo | Descripcion |
|--------|-------------|
| PCOINF | Pago de condena informado por el infractor |
| PCOCNF | Pago de condena confirmado |
| PCOOBS | Pago de condena observado/rechazado |

### Cierre y otros
| Codigo | Descripcion |
|--------|-------------|
| CIERRA | Acta cerrada definitivamente |
| ACTPAR | Acta paralizada |
| ACTREA | Acta reactivada |
| ACTARC | Acta archivada |
| ACTREI | Acta reingresada desde archivo |
| EXTDER | Derivar a gestion externa - apremio/juzgado de paz |
| EXTRET | Reingresar desde gestion externa |
| PAGAPR | Pago externo por apremio registrado |

---

## Catalogo completo de TipoEventoActa (verificacion de completitud)

`TipoEventoActa.java` es la fuente de codigo para el catalogo completo. Esta
tabla enumera los 65 valores vigentes uno por uno para trazabilidad completa
codigo <-> spec. Los eventos del circuito de fallo, notificacion, apelacion,
firmeza, pago de condena y gestion externa tienen matriz de comando completa
en `20-application/fallo-command-contracts.md` (columna "Matriz completa").
Los eventos de documentos, numeracion, acuses/lotes de notificacion y QR
pertenecen a subsistemas ya cerrados en slices anteriores a este circuito y no
se reabren en esta auditoria; su evidencia de conformidad es el codigo Java y
los tests de esos subsistemas.

| Codigo | Descripcion | Area | Matriz completa |
|--------|-------------|------|-------------------|
| ACTLAB | Acta labrada/creada | Ciclo inicial | `02` (esta seccion) |
| ACTCAP | Captura completada (CAPT->ENRI) | Ciclo inicial | `02` (esta seccion) |
| ACTENR | Acta enriquecida | Ciclo inicial | `02` (esta seccion) |
| DOCGEN | Documento generado | Documentos | Codigo/tests del subsistema documental |
| DOCFIR | Documento firmado | Documentos / Fallo | `20-application/fallo-command-contracts.md` (CMD-FALLO-001) |
| DOCEMI | Documento emitido formalmente | Documentos | Codigo/tests del subsistema documental |
| NOTENV | Notificacion enviada | Notificacion | Codigo/tests del subsistema documental |
| NOTPOS | Notificacion con acuse positivo | Notificacion / Fallo | `20-application/fallo-command-contracts.md` (CMD-FALLO-004) |
| NOTNEG | Notificacion con acuse negativo | Notificacion | Codigo/tests del subsistema documental |
| NOTVNC | Notificacion vencida sin acuse | Notificacion | Codigo/tests del subsistema documental |
| ACTPAR | Acta paralizada | Ciclo inicial | `02` (esta seccion) |
| ACTREA | Acta reactivada | Ciclo inicial | `02` (esta seccion) |
| ACTARC | Acta archivada | Ciclo inicial | `02` (esta seccion) |
| ACTREI | Acta reingresada desde archivo | Ciclo inicial | `02` (esta seccion) |
| FALABS | Fallo absolutorio dictado | Fallo | `02` (esta seccion) |
| FALCON | Fallo condenatorio dictado | Fallo | `02` (esta seccion) |
| PAGVSO | Pago voluntario solicitado | Pago voluntario | `02` (esta seccion) |
| PAGVMF | Pago voluntario monto fijado | Pago voluntario | `02` (esta seccion) |
| PAGINF | Pago voluntario informado por el infractor | Pago voluntario | `02` (esta seccion) |
| PAGCMP | Comprobante de pago adjuntado | Pago voluntario | `02` (esta seccion) |
| PAGCNF | Pago voluntario confirmado | Pago voluntario | `02` (esta seccion) |
| PAGOBS | Pago voluntario observado/rechazado | Pago voluntario | `02` (esta seccion) |
| PAGVVN | Pago voluntario vencido sin confirmacion | Pago voluntario | `02` (esta seccion) |
| APEPRE | Apelacion presentada | Apelacion | `02` (esta seccion) |
| APERAZ | Apelacion rechazada/resuelta con resultado RECHAZADA; no declara firmeza | Apelacion | `02` (esta seccion); habilita `20-application/fallo-command-contracts.md` (CMD-FALLO-006) |
| APEABS | Apelacion aceptada - absolucion en segunda instancia | Apelacion | `02` (esta seccion) |
| PLAVNC | Plazo de apelacion vencido sin apelacion presentada | Firmeza | `20-application/fallo-command-contracts.md` (CMD-FALLO-005) |
| CONFIR | Condena firme declarada | Firmeza | `20-application/fallo-command-contracts.md` (CMD-FALLO-005/006) |
| PCOINF | Pago de condena informado por el infractor | Pago de condena | `20-application/fallo-command-contracts.md` (CMD-FALLO-007) |
| PCOCNF | Pago de condena confirmado | Pago de condena | `02` (esta seccion) |
| PCOOBS | Pago de condena observado/rechazado | Pago de condena | `02` (esta seccion) |
| EXTDER | Derivar a gestion externa | Gestion externa | `02` (esta seccion) |
| EXTRET | Reingresar desde gestion externa | Gestion externa | `02` (esta seccion) |
| PAGAPR | Pago externo por apremio registrado | Gestion externa | `02` (esta seccion) |
| APEANL | Apelacion pasada a EN_ANALISIS | Apelacion | Codigo/tests del subsistema de apelacion |
| APEMCO | Apelacion aceptada - condena modificada | Apelacion | Codigo/tests del subsistema de apelacion |
| APENUL | Apelacion resuelta - nulidad declarada | Apelacion | Codigo/tests del subsistema de apelacion |
| FALRMP | Fallo reemplazado por decision en apelacion | Fallo / Apelacion | Codigo/tests del subsistema de apelacion |
| DOCADJ | Documento adjuntado/incorporado al expediente | Documentos | Codigo/tests del subsistema documental |
| CIERRA | Acta cerrada definitivamente | Cierre | `20-application/fallo-command-contracts.md` (CMD-FALLO-005/006/007) |
| OBLDET | Obligacion de pago determinada | Economia | `02` (seccion "Eventos economicos canonicos") |
| OBLSFE | Obligacion de pago dejada sin efecto | Economia | `02` (seccion "Eventos economicos canonicos") |
| OBLREP | Obligacion de pago reemplazada | Economia | `02` (seccion "Eventos economicos canonicos") |
| RCBGEN | Recibo al cobro generado | Economia | `02` (seccion "Eventos economicos canonicos") |
| PLNGEN | Plan de pago generado | Economia | `02` (seccion "Eventos economicos canonicos") |
| PLNREF | Plan de pago refinanciado | Economia | `02` (seccion "Eventos economicos canonicos") |
| PLNANU | Plan de pago anulado | Economia | `02` (seccion "Eventos economicos canonicos") |
| PAGREV | Pago revertido/contracargado | Economia | `02` (seccion "Eventos economicos canonicos") |
| EMIANU | Emision Ingresos anulada | Economia | `02` (seccion "Eventos economicos canonicos") |
| PAGANT | Pago aplicado a obligacion anterior | Economia | `02` (seccion "Eventos economicos canonicos") |
| PAGRES | Pago anterior resuelto administrativamente | Economia | `02` (seccion "Eventos economicos canonicos") |
| NOTINT | Intento de notificacion registrado | Notificacion | Codigo/tests del subsistema documental |
| NOTREI | Reintento de notificacion registrado | Notificacion | Codigo/tests del subsistema documental |
| NOTRVE | Reintento de notificacion post vencimiento | Notificacion | Codigo/tests del subsistema documental |
| ACUGEN | Acuse de notificacion registrado | Notificacion | Codigo/tests del subsistema documental |
| ACUVAL | Acuse de notificacion validado | Notificacion | Codigo/tests del subsistema documental |
| LOTGEN | Lote de correo generado | Notificacion / Fallo | `20-application/fallo-command-contracts.md` (CMD-FALLO-003) |
| LOTEMI | Lote de correo emitido | Notificacion | Codigo/tests del subsistema documental |
| LOTPRC | Lote de correo procesado | Notificacion | Codigo/tests del subsistema documental |
| LOTANU | Lote de correo anulado | Notificacion | Codigo/tests del subsistema documental |
| PORPOS | Notificacion positiva por portal infractor | Notificacion / Fallo | `20-application/fallo-command-contracts.md` (CMD-FALLO-004, variante portal) |
| NOTSUP | Intento de notificacion superado por portal | Notificacion / Fallo | `20-application/fallo-command-contracts.md` (CMD-FALLO-004, variante portal) |
| DOCAMP | Documento adjuntado a apelacion | Documentos / Apelacion | Codigo/tests del subsistema documental |
| QRGENA | Codigo QR de acceso generado para el acta | QR/Portal | Codigo/tests del subsistema QR |
| QRACCA | Acceso valido al acta registrado via codigo QR | QR/Portal | Codigo/tests del subsistema QR |

---

## Prohibiciones explicitas

### Bloques prohibidos / historicos

- `D3_DOCUMENTAL` NO existe como bloque productivo. No se migra a otro nombre. No se reemplaza por bloque documental alguno.
- `D3` NO existe como bloque.
- `DOCUMENTAL` NO existe como bloque.
- `D1_CAPTURA`, `D2_ENRIQUECIMIENTO`, `D4_NOTIFICACION`, `D5_ANALISIS` NO existen como bloques productivos.

### Eventos prohibidos

- `PAGCON` NO existe como evento productivo. Los eventos correctos de pago de condena son: `PCOINF`, `PCOCNF`, `PCOOBS`.
- `PAGVOL` NO existe como evento productivo. Usar los 7 eventos de pago voluntario.
- `ACTCER` NO existe como evento. El evento de cierre es `CIERRA`.
- `PASE_BANDEJA` NO es evento del dominio (es proyeccion operativa).
- `PASE_DEMO` NO existe como evento.
- `APELAC` NO EXISTE como evento productivo. El evento correcto es `APEPRE`.
- `FIRMEZA` NO existe como evento generico. Usar `CONFIR`.
- `FALLO` NO existe como evento generico. Usar `FALABS` o `FALCON`.
- `APELACION` NO existe como evento generico. Usar `APEPRE`, `APERAZ` o `APEABS`.
- `DRVEXT` NO existe como evento productivo. Los eventos correctos de gestion externa son `EXTDER`, `EXTRET`, `PAGAPR`.

### Estados prohibidos

- `PAGO_INFORMADO` NO existe como estado productivo de pago voluntario.
- `PAGO_VOLUNTARIO` NO existe en ResultadoFinalActa. Usar `PAGO_VOLUNTARIO_PAGADO`.
- `PAGCMP` NO se emite sin adjunto/evidencia real del comprobante.
- `FALLO_ABSOLUTORIO` NO existe en ResultadoFinalActa. Usar `ABSUELTO`.
- `DICTADO` y `FIRMADO` NO existen en `EstadoFalloActa`. Son hechos persistidos en `fhDictado` y `fhFirma`.
- `REINGRESAR_A_ANALISIS` y `REINGRESAR_A_PAGO_CONDENA` NO existen. Los valores vigentes de `ModoReingresoGestionExterna` son `REINGRESO_PARA_REVISION` y `REINGRESO_SIN_PAGO` respectivamente.

### Reglas de transicion

- Dictar fallo NO cierra el acta.
- Firmar documento de fallo NO cierra el acta.
- Fallo condenatorio notificado NO genera CONDENA_FIRME automaticamente.
- Registrar apelacion NO cierra el acta.
- Registrar apelacion NO genera CONDENA_FIRME.
- Rechazar apelacion (APERAZ) NO cierra el acta.
- Rechazar apelacion (APERAZ) NO genera CONFIR.
- Rechazar apelacion (APERAZ) NO habilita pago condena todavia.
- Rechazar apelacion (APERAZ) deja pendiente la ejecucion de CMD-FALLO-006 (Declarar condena firme por apelacion rechazada); la firmeza no es automatica.
- Aceptar apelacion que absuelve (APEABS) asigna resultadoFinal=ABSUELTO.
- Aceptar apelacion que absuelve cierra el acta solo si no hay bloqueantes activos.
- PLAVNC y CONFIR son eventos productivos de firmeza de condena. No mezclar con APELAC (evento inexistente).
- Vencimiento de plazo (PLAVNC+CONFIR) solo aplica si no existe apelacion alguna.
- Firmeza por apelacion rechazada registra solo CONFIR (sin PLAVNC).
- CONDENA_FIRME no cierra el acta automaticamente.
- CONDENA_FIRME no inicia pago condena automaticamente.
- Pago de condena: PCOINF, PCOCNF, PCOOBS son eventos productivos vigentes.

---

## Pago de condena

### Eventos de pago de condena

| Codigo | Nombre                     | Descripcion                                        |
|--------|----------------------------|----------------------------------------------------|
| PCOINF | PAGO_CONDENA_INFORMADO     | Pago de condena informado por el infractor         |
| PCOCNF | PAGO_CONDENA_CONFIRMADO    | Pago de condena confirmado por el organismo        |
| PCOOBS | PAGO_CONDENA_OBSERVADO     | Pago de condena observado/rechazado por el organismo |

### Evento prohibido: PAGCON

`PAGCON` NO existe como evento productivo.
Los eventos correctos de pago condena son: `PCOINF`, `PCOCNF`, `PCOOBS`.

### Estado: EstadoPagoCondena

| Valor      | Descripcion                                                        |
|------------|---------------------------------------------------------------------|
| NO_APLICA  | Valor inicial antes de condena firme                                |
| PENDIENTE  | Condena firme declarada, pago aun no informado                      |
| INFORMADO  | Infractor informo pago (via PCOINF)                                 |
| CONFIRMADO | Pago confirmado via PCOCNF. No implica cierre del acta por si solo. |
| OBSERVADO  | Pago observado/rechazado (via PCOOBS)                               |

### ResultadoFinalActa: CONDENA_FIRME_PAGADA

`PCOCNF` (ConfirmarPagoCondena) asigna `resultadoFinal = CONDENA_FIRME_PAGADA` siempre
que la confirmacion supera sus precondiciones, exista o no un bloqueante material activo.
`CIERRA` es un efecto adicional e independiente: solo se registra cuando no hay
bloqueantes materiales activos en el momento de la confirmacion.

### Transiciones de pago de condena

- `CONDENA_FIRME` sin pago -> infractor informa pago (PCOINF) -> estado `INFORMADO`.
- `INFORMADO` -> organismo confirma -> `CONFIRMADO` + `resultadoFinal = CONDENA_FIRME_PAGADA` (`PCOCNF` se
  registra siempre que la confirmacion es valida). A partir de aqui se bifurca segun bloqueantes:
  - **Sin bloqueantes materiales activos:** se registra tambien `CIERRA`; acta `CERRADA` / `CERR`.
  - **Con bloqueantes materiales activos:** no se registra `CIERRA`; acta permanece `ACTIVA` / `ANAL`
    con `PCOCNF` ya registrado y el pago en `CONFIRMADO`. El cierre queda diferido hasta que se
    resuelvan los bloqueantes (ver "Cierre diferido en CumplirBloqueante y AnularBloqueante" en
    `03-comandos-precondiciones-efectos.md`).
- `INFORMADO` -> organismo observa (PCOOBS) -> `OBSERVADO`.
- `OBSERVADO` -> infractor puede reinformar (PCOINF) -> `INFORMADO`.

### Reglas criticas de pago de condena

- `PAGCON` NO existe como evento productivo.
- `PCOCNF` se registra siempre que ConfirmarPagoCondena supera sus precondiciones (acta operativa,
  `CONDENA_FIRME`, pago existente en `INFORMADO`), exista o no un bloqueante material activo.
- Los bloqueantes materiales activos NO rechazan la confirmacion de pago: `ConfirmarPagoCondena`
  siempre completa su efecto principal (`CONFIRMADO` + `PCOCNF` + `CONDENA_FIRME_PAGADA`).
- Los bloqueantes materiales activos unicamente impiden que se registre `CIERRA` en esa misma
  ejecucion. No es una precondicion de la confirmacion: es una condicion del efecto de cierre.
- Informar pago NO cierra el acta.
- Observar pago NO cierra el acta.
- La integracion real con Ingresos/Tesoreria es una dependencia externa no bloqueante para DDL (ver `99-pendientes-siguientes-slices.md`).
- Los comprobantes reales de pago quedan como dependencia externa no bloqueante para DDL (ver `99-pendientes-siguientes-slices.md`).

---

## Reingreso desde gestion externa

### Evento: EXTRET

| Codigo | Nombre | Descripcion |
|--------|--------|-------------|
| EXTRET | REINGRESAR_DESDE_GESTION_EXTERNA | Reingresar desde gestion externa |

### ModoReingresoGestionExterna - catalogo productivo

Catalogo productivo modo_reingreso_gestion_ext. El campo es nullable: NULL antes del reingreso.

| Valor | Estado | Descripcion |
|-------|--------|-------------|
| REINGRESO_PARA_REVISION | Habilitado | Retorna a ANAL / ACTIVA para revision interna. Par valido: SIN_CAMBIOS. Reemplaza el nombre historico REINGRESAR_A_ANALISIS. |
| REINGRESO_SIN_PAGO | Habilitado | Retorna a ANAL / ACTIVA para circuito interno de cobro. Par valido: SIN_PAGO. Requiere CONDENA_FIRME. Reemplaza el nombre historico REINGRESAR_A_PAGO_CONDENA. |
| REINGRESO_PARA_CIERRE | Reservado | Requiere decision de cierre definitivo; no habilitado. |
| REINGRESO_PARA_NUEVO_FALLO | Habilitado | Par valido: ABSUELVE. Requiere CONDENA_FIRME. Vuelve a ANAL sin generar fallo automatico. |
| REINGRESO_CON_PAGO | Reservado / No aplicable a PAGAPR | PAGAPR cierra con CERRADA_EXTERNA sin modo de reingreso. Reservado; no habilitado. |
| REINGRESO_CON_DICTAMEN | Habilitado | Pares validos: CONFIRMA_CONDENA o MODIFICA_MONTO. Requiere CONDENA_FIRME. Vuelve a ANAL. |

El campo modoReingresoGestionExterna es null mientras la gestion no se reingresa (estado DERIVADA o EN_CURSO).
SIN_CAMBIOS empareja naturalmente con REINGRESO_PARA_REVISION.

### EstadoGestionExterna - estados de reingreso

| Valor | Descripcion |
|-------|-------------|
| REINGRESADA | Gestion externa cerrada por reingreso al circuito |

### Evento: PAGAPR

`PAGAPR` existe en `TipoEventoActa` y se emite al registrar pago externo desde gestion externa:
`Pago externo registrado en gestion externa (apremio / juzgado de paz / otra).`

### Reglas de transicion - reingreso

- Reingreso NO cierra el acta.
- Reingreso NO registra CIERRA.
- Reingreso NO registra PAGAPR.
- Reingreso NO registra EXTDER adicional.
- Reingreso registra exactamente un EXTRET.
- Ambos modos habilitados retornan a `bloqueActual=ANAL` y `situacionAdministrativa=ACTIVA`.
- El snapshot se recalcula segun el estado real del acta post-reingreso.
- La gestion externa `siActiva` pasa a `false` tras el reingreso.
- El ciclo externo se conserva (trazabilidad: `FalGestionExterna` no se borra).

---

## Pago externo de gestion externa

### Comando: RegistrarPagoExternoGestionCommand

Aplica a cualquier tipo de gestion externa: apremio, juzgado de paz u otra.
El evento sigue siendo PAGAPR (codigo de 6 caracteres cerrado).

### Evento: PAGAPR

| Codigo | Nombre | Descripcion |
|--------|--------|-------------|
| PAGAPR | PAGO_EXTERNO_GESTION | Pago externo registrado en gestion externa |

Se emite al registrar pago externo desde gestion externa.

Nota: PAGAPR y EXTRET son caminos mutuamente excluyentes por ciclo de gestion externa.
- EXTRET = reingreso sin pago externo confirmado.
- PAGAPR = cierre de la gestion con pago externo registrado.
Ambos dejan FalGestionExterna.siActiva = false. Solo uno puede ocurrir por ciclo.

### EstadoGestionExterna: CERRADA_EXTERNA

| Valor | Descripcion |
|-------|-------------|
| CERRADA_EXTERNA | Gestion externa cerrada por pago externo registrado |

### ResultadoGestionExterna - catalogo productivo

Catalogo productivo `resultado_gestion_ext`.

| Valor | Estado | Descripcion |
|-------|--------|-------------|
| SIN_RESULTADO | Implementado | Estado inicial al derivar. |
| PAGO_REGISTRADO | Implementado | Pago externo registrado. Reemplaza el nombre historico PAGO_EXTERNO_INFORMADO. |
| SIN_CAMBIOS | Habilitado | Reingresa sin cambios sustantivos. Par obligatorio: REINGRESO_PARA_REVISION. |
| SIN_PAGO | Habilitado | Reingresa sin pago. Par obligatorio: REINGRESO_SIN_PAGO. Requiere CONDENA_FIRME. |
| ABSUELVE | Habilitado | El externo propone absolver. Par obligatorio: REINGRESO_PARA_NUEVO_FALLO. |
| CONFIRMA_CONDENA | Habilitado | El externo confirma condena. Par obligatorio: REINGRESO_CON_DICTAMEN. |
| MODIFICA_MONTO | Habilitado | El externo modifica monto. Par obligatorio: REINGRESO_CON_DICTAMEN. Requiere montoResultado > 0. |

No usar strings compuestos para mezclar tipo, resultado y modo de reingreso.
Documentos externos recibidos: por fal_documento/adjuntos (pendiente).
Fundamentos y comentarios: por fal_observacion (pendiente hasta JDBC).

### ResultadoFinalActa: CONDENA_FIRME_PAGADA (via PAGAPR)

PAGAPR asigna resultadoFinal = CONDENA_FIRME_PAGADA siempre (identico al pago interno PCOCNF).
La ruta es diferente (externa vs interna) pero el resultado juridico final es el mismo.

FALLO_CONDENATORIO_GESTION_EXTERNA existe en el enum pero no se usa en el circuito de PAGAPR.
Queda reservado. SIN_PAGO y SIN_CAMBIOS son rutas de reingreso (EXTRET), no rutas de cierre PAGAPR.
Los pares SIN_PAGO/REINGRESO_SIN_PAGO y SIN_CAMBIOS/REINGRESO_PARA_REVISION son pares productivos vigentes.

### Reglas de transicion - pago externo de gestion externa

- PAGAPR NO falla por bloqueantes materiales activos.
- Los bloqueantes solo determinan si se emite o no CIERRA.
- Sin bloqueantes: PAGAPR + CIERRA. Acta: resultadoFinal=CONDENA_FIRME_PAGADA, CERRADA/CERR.
- Con bloqueantes: solo PAGAPR. Acta: resultadoFinal=CONDENA_FIRME_PAGADA, ACTIVA/ANAL.
  Este comportamiento se mantiene mientras existan bloqueantes materiales activos (ver "Motor real de bloqueantes materiales").
- PAGAPR no toca FalPagoCondena (flujo interno de pago condena es independiente).
- Si existe FalPagoCondena INFORMADO/OBSERVADO, queda como historico sin modificar.
- Si existe FalPagoCondena CONFIRMADO: PAGAPR lanza PrecondicionVioladaException.
- Despues de PAGAPR, confirmar pago condena interno falla: resultadoFinal ya no es CONDENA_FIRME.
- EXTRET no se emite al registrar PAGAPR.
- EXTDER no se emite al registrar PAGAPR.
- DRVEXT sigue prohibido.

---

## Motor real de bloqueantes materiales

### OrigenBloqueanteMaterial

| Valor | Descripcion |
|-------|-------------|
| MEDIDA_PREVENTIVA | Medida preventiva activa sobre el infractor |
| RODADO | Rodado retenido en deposito |
| DOCUMENTACION_RETENIDA | Documentacion del vehiculo o infractor retenida |
| OTRO | Otro tipo de bloqueante material |

### EstadoBloqueanteMaterial

| Valor | Descripcion |
|-------|-------------|
| PENDIENTE | Bloqueante activo, impide el cierre del acta |
| CUMPLIDO | Bloqueante resuelto, ya no impide el cierre |
| ANULADO | Bloqueante anulado administrativamente |

Un bloqueante impide el cierre unicamente cuando `siActivo == true` (estado PENDIENTE).
Un bloqueante con `siActivo == false` (CUMPLIDO o ANULADO) no impide el cierre.

### FalBloqueanteMaterial (entidad)

Campos: id (String), actaId (String), origen (OrigenBloqueanteMaterial), estado (EstadoBloqueanteMaterial), siActivo (boolean), descripcion (String), fechaAlta (LocalDateTime), fechaCierre (LocalDateTime nullable).

### BloqueanteMaterialRepository

Contrato:
- `guardar(FalBloqueanteMaterial)`: persiste o actualiza el bloqueante.
- `findByActaId(String actaId)`: lista todos los bloqueantes del acta.
- `existsActivoByActaId(String actaId)`: true si existe al menos uno con siActivo==true.

Implementacion actual: `InMemoryBloqueanteMaterialRepository`.
Pendiente: reemplazar por implementacion JDBC sin tocar servicios (ver roadmap de DDL/JDBC en `99-pendientes-siguientes-slices.md`).

### RepositoryBloqueantesMaterialesChecker

Implementacion real de `BloqueantesMaterialesChecker`.
Delega en `BloqueanteMaterialRepository.existsActivoByActaId(actaId)`.
`@Component` - inyectado por Spring en todos los servicios.

`NoOpBloqueantesMaterialesChecker` ya no es `@Component`. Usable solo en tests directamente.

### Regla central de bloqueantes materiales

Un acta con resultado final cerrable NO se cierra si existen bloqueantes activos.

Caminos afectados:
- `PCOCNF` (ConfirmarPagoCondena): PCOCNF se registra siempre. CIERRA solo si sin bloqueantes.
  Con bloqueantes: acta queda CONDENA_FIRME_PAGADA / ACTIVA / ANAL.
- `PAGAPR` (RegistrarPagoExternoGestion): mismo patron.
- `PAGCNF` (ConfirmarPagoVoluntario): lanza PrecondicionVioladaException si hay bloqueantes (sin mutacion).
- `NOTPOS` absolutorio y `APEABS` absolutorio: no cierran si hay bloqueantes (sin mutacion).

---

## Gestion minima in-memory de bloqueantes materiales

### Operaciones de gestion (no generan eventos de acta)

| Operacion | Comando | Estado resultante | siActivo |
|-----------|---------|-------------------|----------|
| registrar | RegistrarBloqueanteMaterialCommand | PENDIENTE | true |
| cumplir   | CumplirBloqueanteMaterialCommand   | CUMPLIDO  | false |
| anular    | AnularBloqueanteMaterialCommand    | ANULADO   | false |

Estas operaciones no emiten eventos de FalActa: no existe evento de dominio definido para gestion de bloqueantes.
El snapshot del acta no cambia por estas operaciones.
El impacto en el cierre se produce cuando el acta llega a un punto de cierre (PCOCNF, PAGAPR, PAGCNF, NOTPOS, APEABS).

### Reglas de transicion de bloqueantes

- PENDIENTE -> CUMPLIDO: via cumplir. Idempotente si ya CUMPLIDO. No permitido si ANULADO.
- PENDIENTE -> ANULADO:  via anular.  Idempotente si ya ANULADO.  No permitido si CUMPLIDO.
- CUMPLIDO: terminal. No puede pasar a ANULADO.
- ANULADO:  terminal. No puede pasar a CUMPLIDO.
- existsActivoByActaId: solo considera siActivo=true (PENDIENTE activo). CUMPLIDO y ANULADO no impiden cierre.

### BloqueanteMaterialNoEncontradoException

Lanzada por cumplir() y anular() si el bloqueanteId no existe en el repositorio.

## Cierre diferido automatico

Al resolver el ultimo bloqueante activo (cumplir o anular), el sistema evalua si el acta puede cerrarse:

- Si no quedan bloqueantes activos Y el acta esta ACTIVA Y el resultado es cerrable Y no existe CIERRA:
  - Se emite CIERRA.
  - Acta pasa a CERRADA/CERR.

Resultados cerrables habilitados para cierre diferido:
  - PAGO_VOLUNTARIO_PAGADO
  - ABSUELTO
  - CONDENA_FIRME_PAGADA

CONDENA_FIRME no habilita cierre diferido: requiere confirmacion de pago o gestion externa.

No se crea ningun evento nuevo para cumplir/anular bloqueante.
El cierre diferido solo emite CIERRA (evento ya existente).

---

## Eventos economicos canonicos

Catalogo vigente en `TipoEventoActa` para el circuito economico MariaDB/InMemory.
No usar en spec activa los alias descartados: `DEBEMI`, `PAGPRC`, `PAGCFT`, `MOVPAG`, `PLNCAI`, `PAGANU`, ni sustitutos parciales (`OBLAUL`, `FPCGEN`, `FPPGEN`, `FPREFN`, `PLNCAN`).

| Codigo | Descripcion |
|--------|-------------|
| OBLDET | Obligacion de pago determinada |
| OBLSFE | Obligacion de pago dejada sin efecto |
| OBLREP | Obligacion de pago reemplazada |
| RCBGEN | Recibo al cobro generado |
| PLNGEN | Plan de pago generado |
| PLNREF | Plan de pago refinanciado |
| PLNANU | Plan de pago anulado |
| PAGREV | Pago revertido/contracargado |
| EMIANU | Emision Ingresos anulada |
| PAGANT | Pago aplicado a obligacion anterior |
| PAGRES | Pago anterior resuelto administrativamente |

Reglas:
- `fal_acta_evento` permanece append-only; los hechos economicos se registran con estos codigos.
- La proyeccion operativa (`fal_acta_economia_proyeccion`) y el snapshot leen la proyeccion economica; no duplican reglas de calculo en campos paralelos.

### Cierre de economia InMemory

- `FalActaPagoMovimiento` es append-only con un unico vinculo canonico al original (`movimientoOrigenId`); un reverso (`PAGO_REVERTIDO`) o una anulacion (`EMISION_ANULADA`) referencian el movimiento origen por ese campo.
- Un reverso que revive saldo hace que la obligacion deje de estar `CANCELADA_POR_PAGO` (vuelve a `CON_FORMA_PAGO_VIGENTE` si hay forma vigente o `PENDIENTE_FORMA_PAGO` si no) y que la forma deje de estar `PAGADA`.
- `siPagoConfirmado` representa pago vigente: reverso total -> false; dos pagos con reverso de uno -> sigue true.
- El plan finaliza solo mediante transicion atomica a `FINALIZADO_POR_PAGO` (`siVigente=false`, `fhFinalizacionPago`). No se usan `CUMPLIDO`, `CAIDO`, `fhCaida` ni `PLNCAI`.

### Pago aplicado a obligacion anterior: PAGANT y PAGRES

Un pago recibido no se rechaza ni se pierde aunque refiera a una
`FalActaObligacionPago` que ya no es `siVigente` (anterior, reemplazada,
cancelada o dejada sin efecto). El circuito completo (comando, precondiciones
y efectos) vive en
[`03-comandos-precondiciones-efectos.md`](03-comandos-precondiciones-efectos.md#pago-aplicado-a-obligacion-anterior---comandos-precondiciones-y-efectos);
esta seccion solo fija el significado de los dos codigos de evento.

- `PAGANT`: constancia de que un `PAGO_CONFIRMADO` recibido referencia una
  obligacion que ya no es vigente. Se emite al notificar el movimiento
  (clasificacion `ClasificacionPago.OBLIGACION_ANTERIOR`), en lugar de
  `PAGCNF`/`PCOCNF`. Todavia requiere asociacion/resolucion administrativa;
  no implica por si mismo ningun cambio de estado en la obligacion vigente
  del acta. `OBLIGACION_ANTERIOR` es una clasificacion derivada del estado
  objetivo de la obligacion (no vigente), nunca una eleccion del actor: un
  `PAGO_CONFIRMADO` contra una obligacion vigente no puede declararla.
- `PAGRES`: resolucion efectiva y auditada de la aplicacion de ese pago
  contra la obligacion vigente, mediante `ResolverPagoObligacionAnteriorCommand`.
  No existe una tabla propia de resolucion (`fal_acta_pago_resolucion`) ni se
  crea una obligacion nueva por la diferencia: el efecto es un unico
  movimiento de aplicacion (`PAGO_CONFIRMADO`, `clasificacionPago=NORMAL`)
  contra la obligacion vigente, con `movimientoOrigenId` apuntando al
  movimiento `PAGANT` original, mas este evento. El saldo de la obligacion
  se deriva siempre de la suma de sus movimientos (`monto - aplicado`); si el
  pago resuelto supera el saldo pendiente, el excedente queda como dato
  informativo (`importeExcedente`), no como una nueva obligacion ni una
  devolucion.
- `PAGANT` y `PAGRES` son eventos distintos y no intercambiables: un
  movimiento `PAGANT` admite a lo sumo una aplicacion (`PAGRES`); la
  unicidad se garantiza sin tabla propia, exigiendo que a lo sumo un
  movimiento declare `movimientoOrigenId` igual al id del `PAGANT`. La
  ausencia de `PAGRES` para un `PAGANT` dado significa que el pago sigue
  pendiente de resolucion administrativa.
- La resolucion registrada por `PAGRES` usa un unico instante real de
  `FaltasClock.now()` por ejecucion (no una igualdad parcial entre varias
  lecturas): ese mismo instante es compartido por el movimiento de
  aplicacion (`fhMovimiento`, `fhAlta`), la cancelacion de la obligacion si
  corresponde (`fhCancelacion`), el propio `PAGRES.fhEvt` y la proyeccion
  economica recalculada (`fhCorteEconomico`, `fhUltMod`). El motivo de la
  resolucion vive estructurado en
  `FalActaPagoMovimiento.motivoAplicacionPagoAnterior` (campo del
  movimiento de aplicacion), no parseado desde
  `descripcionLegible`: `descripcionLegible` puede mostrar el motivo en
  texto para lectura humana, pero nunca es la fuente de verdad que el
  sistema consulta para decidir idempotencia o conflicto.
