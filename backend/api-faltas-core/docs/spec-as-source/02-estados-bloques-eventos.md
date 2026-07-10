 02 - Estados, Bloques y Eventos

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
| PAGO_VOLUNTARIO_CONFIRMADO        | Cerrada por pago voluntario confirmado (Slice 2) |
| ABSUELTO                          | Cerrada por fallo absolutorio notificado (Slice 3A) |
| CONDENA_FIRME                     | Condena firme declarada por vencimiento de plazo o apelacion rechazada (Slice 4) |
| FALLO_CONDENATORIO_PAGADO         | Fallo condenatorio pagado (Slice 5) |
| FALLO_CONDENATORIO_GESTION_EXTERNA| Fallo condenatorio en gestion externa (Slice 6) |
| PRESCRIPTO                        | Cerrada por prescripcion |
| ANULADO                           | Anulada |

Prohibido: PAGO_VOLUNTARIO (ambiguo), PAGO_INFORMADO (no es resultado final).
Prohibido: FALLO_ABSOLUTORIO como valor de ResultadoFinalActa. El valor correcto es ABSUELTO.

---

## EstadoPagoVoluntario (Slice 2)

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

## TipoFalloActa (Slice 3A)

| Valor | Descripcion |
|-------|-------------|
| ABSOLUTORIO  | Fallo que absuelve al infractor |
| CONDENATORIO | Fallo que condena al infractor |

---

## EstadoFalloActa (Slice 3A)

| Valor | Descripcion |
|-------|-------------|
| DICTADO              | Fallo dictado, pendiente de firma del documento |
| PENDIENTE_FIRMA      | Alias operativo de DICTADO |
| FIRMADO              | Documento de fallo firmado, pendiente de notificacion |
| PENDIENTE_NOTIFICACION | Alias operativo de FIRMADO |
| NOTIFICADO           | Notificacion del fallo registrada con acuse positivo |
| SIN_EFECTO           | Fallo anulado o sin efecto (reservado) |

---

## EstadoApelacionActa (Slice 3B)

| Valor | Descripcion |
|-------|-------------|
| PRESENTADA       | Apelacion presentada por el infractor (Slice 3B) |
| RECHAZADA        | Apelacion rechazada - condena queda firme (Slice 3C) |
| ACEPTADA_ABSUELVE| Apelacion aceptada - absolucion en segunda instancia (Slice 3C) |
| SIN_EFECTO       | Apelacion anulada o sin efecto |

---

## Eventos (TipoEventoActa)

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
| NOTNEG | Notificacion con acuse negativo |
| NOTVNC | Notificacion vencida sin acuse |

### Pago voluntario (Slice 2)
| Codigo | Descripcion |
|--------|-------------|
| PAGVSO | Pago voluntario solicitado |
| PAGVMF | Pago voluntario monto fijado |
| PAGINF | Pago voluntario informado por el infractor |
| PAGCMP | Comprobante de pago adjuntado (pendiente: no se emite sin adjunto real) |
| PAGCNF | Pago voluntario confirmado |
| PAGOBS | Pago voluntario observado/rechazado |
| PAGVVN | Pago voluntario vencido sin confirmacion |

### Fallo (Slice 3A)
| Codigo | Descripcion |
|--------|-------------|
| FALABS | Fallo absolutorio dictado |
| FALCON | Fallo condenatorio dictado |

### Apelacion (Slice 3B + 3C)
| Codigo | Descripcion |
|--------|-------------|
| APEPRE | Apelacion presentada |
| APERAZ | Apelacion rechazada - condena queda firme (Slice 3C) |
| APEABS | Apelacion aceptada - absolucion en segunda instancia (Slice 3C) |

### Firmeza de condena (Slice 4)
| Codigo | Descripcion |
|--------|-------------|
| PLAVNC | Plazo de apelacion vencido sin apelacion presentada |
| CONFIR | Condena firme declarada |

### Pago de condena (Slice 5 ? implementado)
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
| EXTDER | Derivar a gestion externa - apremio/juzgado de paz (Slice 6) |
| EXTRET | Reingresar desde gestion externa (Slice 6) |
| PAGAPR | Pago externo por apremio registrado (Slice 6) |

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

### Estados prohibidos

- `PAGO_INFORMADO` NO existe como estado productivo de pago voluntario.
- `PAGO_VOLUNTARIO` NO existe en ResultadoFinalActa. Usar `PAGO_VOLUNTARIO_CONFIRMADO`.
- `PAGCMP` NO se emite sin adjunto/evidencia real del comprobante.
- `FALLO_ABSOLUTORIO` NO existe en ResultadoFinalActa. Usar `ABSUELTO`.

### Reglas de transicion

- Dictar fallo NO cierra el acta.
- Firmar documento de fallo NO cierra el acta.
- Fallo condenatorio notificado NO genera CONDENA_FIRME automaticamente.
- Registrar apelacion NO cierra el acta.
- Registrar apelacion NO genera CONDENA_FIRME.
- Rechazar apelacion (APERAZ) NO cierra el acta.
- Rechazar apelacion (APERAZ) NO genera CONFIR.
- Rechazar apelacion (APERAZ) NO habilita pago condena todavia.
- Aceptar apelacion que absuelve (APEABS) asigna resultadoFinal=ABSUELTO.
- Aceptar apelacion que absuelve cierra el acta solo si no hay bloqueantes activos.
- PLAVNC y CONFIR son eventos productivos de firmeza de condena. No mezclar con APELAC.
- Vencimiento de plazo (PLAVNC+CONFIR) solo aplica si no existe apelacion alguna.
- Firmeza por apelacion rechazada registra solo CONFIR (sin PLAVNC).
- CONDENA_FIRME no cierra el acta automaticamente.
- CONDENA_FIRME no inicia pago condena automaticamente.
- Pago de condena: implementado en Slice 5 (PCOINF, PCOCNF, PCOOBS activos).
- PLAVNC y CONFIR son eventos productivos reales desde Slice 4.
- CONDENA_FIRME es un valor productivo real desde Slice 4.
- PCOINF, PCOCNF, PCOOBS: implementados en Slice 5. Activos.

---

## Slice 5: Pago de condena (implementado)

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

| Valor      | Descripcion                                      |
|------------|--------------------------------------------------|
| NO_APLICA  | Valor inicial antes de condena firme             |
| PENDIENTE  | Condena firme declarada, pago aun no informado   |
| INFORMADO  | Infractor informo pago (via PCOINF)              |
| CONFIRMADO | Pago confirmado (via PCOCNF) -> cierre del acta  |
| OBSERVADO  | Pago observado/rechazado (via PCOOBS)            |

### ResultadoFinalActa: CONDENA_FIRME_PAGADA

Cuando el pago de condena es confirmado, el resultado final del acta pasa a:
`CONDENA_FIRME_PAGADA` (via `PCOCNF` + `CIERRA`).

### Transiciones de pago de condena

- `CONDENA_FIRME` sin pago -> infractor informa pago (PCOINF) -> estado `INFORMADO`
- `INFORMADO` -> organismo confirma (PCOCNF + CIERRA) -> `CONFIRMADO` + acta CERRADA
- `INFORMADO` -> organismo observa (PCOOBS) -> `OBSERVADO`
- `OBSERVADO` -> infractor puede reinformar (PCOINF) -> `INFORMADO`

### Reglas criticas de pago de condena

- PAGCON NO existe.
- Confirmar pago solo si no hay bloqueantes materiales activos.
- Si hay bloqueantes: no registrar PCOCNF, no registrar CIERRA, no cerrar acta.
- Cuando confirma sin bloqueantes: PCOCNF se registra antes que CIERRA.
- Informar pago NO cierra el acta.
- Observar pago NO cierra el acta.
- Integracion real con Ingresos/Tesoreria queda para slice posterior.
- Comprobantes reales quedan para slice posterior.
- Gestion externa no implementada en Slice 5.


---

## Slice 6B: Reingreso desde gestion externa (implementado)

### Evento: EXTRET

| Codigo | Nombre | Descripcion |
|--------|--------|-------------|
| EXTRET | REINGRESAR_DESDE_GESTION_EXTERNA | Reingresar desde gestion externa (Slice 6B) |

**Estado:** implementado en Slice 6B.

### ModoReingresoGestionExterna — catálogo productivo (alineado Slice 6D-0)

Catalogo productivo modo_reingreso_gestion_ext. El campo es nullable: NULL antes del reingreso.

| Valor | Estado | Descripcion |
|-------|--------|-------------|
| REINGRESO_PARA_REVISION | Habilitado (Slice 6B / 6D-1) | Retorna a ANAL / ACTIVA para revision interna. Par valido: SIN_CAMBIOS. Reemplaza REINGRESAR_A_ANALISIS. |
| REINGRESO_SIN_PAGO | Habilitado (Slice 6B / 6D-1) | Retorna a ANAL / ACTIVA para circuito interno de cobro. Par valido: SIN_PAGO. Requiere CONDENA_FIRME. Reemplaza REINGRESAR_A_PAGO_CONDENA. |
| REINGRESO_PARA_CIERRE | Reservado | Pendiente para slice posterior. |
| REINGRESO_PARA_NUEVO_FALLO | Habilitado desde Slice 6D-2 | Par valido: ABSUELVE. Requiere CONDENA_FIRME. Vuelve a ANAL sin generar fallo automatico. |
| REINGRESO_CON_PAGO | Reservado / No aplicable a PAGAPR | PAGAPR cierra con CERRADA_EXTERNA sin modo de reingreso. Reservado para slice futuro. |
| REINGRESO_CON_DICTAMEN | Habilitado desde Slice 6D-2 | Pares validos: CONFIRMA_CONDENA o MODIFICA_MONTO. Requiere CONDENA_FIRME. Vuelve a ANAL. |

El campo modoReingresoGestionExterna es null mientras la gestion no se reingresia (estado DERIVADA o EN_CURSO).
SIN_CAMBIOS empareja naturalmente con REINGRESO_PARA_REVISION.

### EstadoGestionExterna — estados de reingreso

| Valor | Descripcion |
|-------|-------------|
| REINGRESADA | Gestion externa cerrada por reingreso al circuito (Slice 6B) |

### Evento: PAGAPR (Slice 6C)

`PAGAPR` existe en `TipoEventoActa` y **se emite desde Slice 6C**:
`Pago externo registrado en gestion externa (apremio / juzgado de paz / otra).`

### Reglas de transición — reingreso

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

## Slice 6C: Pago externo de gestion externa (implementado)

### Comando: RegistrarPagoExternoGestionCommand

Aplica a cualquier tipo de gestion externa: apremio, juzgado de paz u otra.
El evento sigue siendo PAGAPR (codigo de 6 caracteres cerrado).

### Evento: PAGAPR

| Codigo | Nombre | Descripcion |
|--------|--------|-------------|
| PAGAPR | PAGO_EXTERNO_GESTION | Pago externo registrado en gestion externa (Slice 6C) |

**Estado anterior a Slice 6C:** declarado en TipoEventoActa pero no emitido.
**Estado desde Slice 6C:** activo. Se emite al registrar pago externo desde gestion externa.

Nota: PAGAPR y EXTRET son caminos mutuamente excluyentes por ciclo de gestion externa.
- EXTRET = reingreso sin pago externo confirmado (Slice 6B).
- PAGAPR = cierre de la gestion con pago externo registrado (Slice 6C).
Ambos dejan FalGestionExterna.siActiva = false. Solo uno puede ocurrir por ciclo.

### EstadoGestionExterna: CERRADA_EXTERNA (activo desde Slice 6C)

| Valor | Descripcion |
|-------|-------------|
| CERRADA_EXTERNA | Gestion externa cerrada por pago externo registrado (Slice 6C) |

### ResultadoGestionExterna - catalogo productivo (alineado Slice 6D-0)

Catalogo productivo `resultado_gestion_ext`.

| Valor | Estado | Descripcion |
|-------|--------|-------------|
| SIN_RESULTADO | Implementado (Slice 6A) | Estado inicial al derivar. |
| PAGO_REGISTRADO | Implementado (Slice 6C) | Pago externo registrado. Reemplaza PAGO_EXTERNO_INFORMADO. |
| SIN_CAMBIOS | Habilitado (Slice 6D-1) | Reingresa sin cambios sustantivos. Par obligatorio: REINGRESO_PARA_REVISION. |
| SIN_PAGO | Habilitado (Slice 6D-1) | Reingresa sin pago. Par obligatorio: REINGRESO_SIN_PAGO. Requiere CONDENA_FIRME. |
| ABSUELVE | Habilitado (Slice 6D-2) | El externo propone absolver. Par obligatorio: REINGRESO_PARA_NUEVO_FALLO. |
| CONFIRMA_CONDENA | Habilitado (Slice 6D-2) | El externo confirma condena. Par obligatorio: REINGRESO_CON_DICTAMEN. |
| MODIFICA_MONTO | Habilitado (Slice 6D-2) | El externo modifica monto. Par obligatorio: REINGRESO_CON_DICTAMEN. Requiere montoResultado > 0. |

No usar strings compuestos para mezclar tipo, resultado y modo de reingreso.
Documentos externos recibidos: por fal_documento/adjuntos (pendiente).
Fundamentos y comentarios: por fal_observacion (pendiente hasta JDBC).

### ResultadoFinalActa: CONDENA_FIRME_PAGADA (via PAGAPR)

PAGAPR asigna
esultadoFinal = CONDENA_FIRME_PAGADA siempre (identico al pago interno PCOCNF).
La ruta es diferente (externa vs interna) pero el resultado juridico final es el mismo.

FALLO_CONDENATORIO_GESTION_EXTERNA existe en el enum pero NO se usa en Slice 6C.
Queda reservado. SIN_PAGO y SIN_CAMBIOS son rutas de reingreso (EXTRET), no rutas de cierre PAGAPR.
Par SIN_PAGO/REINGRESO_SIN_PAGO y SIN_CAMBIOS/REINGRESO_PARA_REVISION implementados en Slice 6D-1.

### Reglas de transicion - Slice 6C

- PAGAPR NO falla por bloqueantes materiales activos.
- Los bloqueantes solo determinan si se emite o no CIERRA.
- Sin bloqueantes: PAGAPR + CIERRA. Acta: resultadoFinal=CONDENA_FIRME_PAGADA, CERRADA/CERR.
- Con bloqueantes: solo PAGAPR. Acta: resultadoFinal=CONDENA_FIRME_PAGADA, ACTIVA/ANAL.
  Estado transitorio hasta Slice 7 (motor real de bloqueantes).
- PAGAPR no toca FalPagoCondena (flujo interno de pago condena es independiente).
- Si existe FalPagoCondena INFORMADO/OBSERVADO, queda como historico sin modificar.
- Si existe FalPagoCondena CONFIRMADO: PAGAPR lanza PrecondicionVioladaException.
- Despues de PAGAPR, confirmar pago condena interno falla: resultadoFinal ya no es CONDENA_FIRME.
- EXTRET no se emite al registrar PAGAPR.
- EXTDER no se emite al registrar PAGAPR.
- DRVEXT sigue prohibido.

---

## Slice 7A: Motor real de bloqueantes materiales (implementado)

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
Slice 9: reemplazar por implementacion JDBC sin tocar servicios.

### RepositoryBloqueantesMaterialesChecker

Implementacion real de `BloqueantesMaterialesChecker` (Slice 7A).
Delega en `BloqueanteMaterialRepository.existsActivoByActaId(actaId)`.
`@Component` - inyectado por Spring en todos los servicios.

`NoOpBloqueantesMaterialesChecker` ya no es `@Component`. Usable solo en tests directamente.

### Regla central Slice 7A

Un acta con resultado final cerrable NO se cierra si existen bloqueantes activos.

Caminos afectados:
- `PCOCNF` (ConfirmarPagoCondena): PCOCNF se registra siempre. CIERRA solo si sin bloqueantes.
  Con bloqueantes: acta queda CONDENA_FIRME_PAGADA / ACTIVA / ANAL.
- `PAGAPR` (RegistrarPagoExternoGestion): mismo patron (ya implementado en Slice 6C).
- `PAGCNF` (ConfirmarPagoVoluntario): lanza PrecondicionVioladaException si hay bloqueantes (sin mutacion).
- `NOTPOS` absolutorio y `APEABS` absolutorio: no cierran si hay bloqueantes (sin mutacion).

---

## Slice 7B: Gestion minima in-memory de bloqueantes materiales (implementado)

### Operaciones de gestion (no generan eventos de acta)

| Operacion | Comando | Estado resultante | siActivo |
|-----------|---------|-------------------|----------|
| registrar | RegistrarBloqueanteMaterialCommand | PENDIENTE | true |
| cumplir   | CumplirBloqueanteMaterialCommand   | CUMPLIDO  | false |
| anular    | AnularBloqueanteMaterialCommand    | ANULADO   | false |

Estas operaciones no emiten eventos de FalActa: no existe evento de dominio definido para gestion de bloqueantes.
El snapshot del acta no cambia por estas operaciones.
El impacto en el cierre se produce cuando el acta llega a un punto de cierre (PCOCNF, PAGAPR, PAGCNF, NOTPOS, APEABS).

### Reglas de transicion de bloqueantes (Slice 7B)

- PENDIENTE -> CUMPLIDO: via cumplir. Idempotente si ya CUMPLIDO. No permitido si ANULADO.
- PENDIENTE -> ANULADO:  via anular.  Idempotente si ya ANULADO.  No permitido si CUMPLIDO.
- CUMPLIDO: terminal. No puede pasar a ANULADO.
- ANULADO:  terminal. No puede pasar a CUMPLIDO.
- existsActivoByActaId: solo considera siActivo=true (PENDIENTE activo). CUMPLIDO y ANULADO no impiden cierre.

### BloqueanteMaterialNoEncontradoException (Slice 7B)

Lanzada por cumplir() y anular() si el bloqueanteId no existe en el repositorio.

## Slice 7C: Cierre diferido automatico (implementado)

Al resolver el ultimo bloqueante activo (cumplir o anular), el sistema evalua si el acta puede cerrarse:

- Si no quedan bloqueantes activos Y el acta esta ACTIVA Y el resultado es cerrable Y no existe CIERRA:
  - Se emite CIERRA.
  - Acta pasa a CERRADA/CERR.

Resultados cerrables habilitados para cierre diferido:
  - PAGO_VOLUNTARIO_CONFIRMADO
  - ABSUELTO
  - CONDENA_FIRME_PAGADA

CONDENA_FIRME no habilita cierre diferido: requiere confirmacion de pago o gestion externa.

No se crea ningun evento nuevo para cumplir/anular bloqueante.
El cierre diferido solo emite CIERRA (evento ya existente).

---

## Slice 8F-11M-B1: Eventos economicos canonicos (implementado)

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

### 8F-11M-B1-R2 (cierre economia InMemory)

- `FalActaPagoMovimiento` es append-only con un unico vinculo canonico al original (`movimientoOrigenId`); un reverso (`PAGO_REVERTIDO`) o una anulacion (`EMISION_ANULADA`) referencian el movimiento origen por ese campo.
- Un reverso que revive saldo hace que la obligacion deje de estar `CANCELADA_POR_PAGO` (vuelve a `CON_FORMA_PAGO_VIGENTE` si hay forma vigente o `PENDIENTE_FORMA_PAGO` si no) y que la forma deje de estar `PAGADA`.
- `siPagoConfirmado` representa pago vigente: reverso total -> false; dos pagos con reverso de uno -> sigue true.
- El plan finaliza solo mediante transicion atomica a `FINALIZADO_POR_PAGO` (`siVigente=false`, `fhFinalizacionPago`). No se usan `CUMPLIDO`, `CAIDO`, `fhCaida` ni `PLNCAI`.
