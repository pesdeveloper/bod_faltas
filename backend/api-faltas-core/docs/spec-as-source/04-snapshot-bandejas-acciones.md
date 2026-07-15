# 04 - Snapshot, Bandejas y Acciones

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Ante contradiccion con un documento tematico de `00-governance/` o `10-domain/`, ese documento tematico prevalece en lo que respecta a definiciones, dimensiones y lifecycle (ver README, seccion 4.0).

## Principio

El snapshot es una PROYECCION derivada y regenerable. No es fuente de verdad.
Se recalcula en cada transicion de dominio.

## Routing de bandeja

### 1. Situacion administrativa (transversal al bloque)

| SituacionAdministrativa | Bandeja    | Accion  |
|-------------------------|------------|---------|
| CERRADA / ANULADA       | CERRADAS   | NINGUNA |
| ARCHIVADA               | ARCHIVO    | NINGUNA |
| PARALIZADA              | PARALIZADAS| NINGUNA |
| EN_GESTION_EXTERNA      | GESTION_EXTERNA | NINGUNA |

### 2. Bloque (cuando situacion = ACTIVA)

| Bloque | Condicion adicional | Bandeja | Accion |
|--------|---------------------|---------|--------|
| CERR   | -                   | CERRADAS | NINGUNA |
| ARCH   | -                   | ARCHIVO  | NINGUNA |
| GEXT   | -                   | GESTION_EXTERNA | NINGUNA |
| ANAL   | pago PENDIENTE_CONFIRMACION | PENDIENTE_CONFIRMACION_PAGO | CONFIRMAR_PAGO |
| ANAL   | pago OBSERVADO      | PENDIENTE_ANALISIS | CORREGIR_PAGO |
| ANAL   | apelacion activa    | CON_APELACION | RESOLVER_APELACION |
| ANAL   | fallo PENDIENTE_FIRMA | PENDIENTE_FIRMA | FIRMAR_DOCUMENTO |
| ANAL   | fallo PENDIENTE_NOTIFICACION | PENDIENTE_NOTIFICACION | ENVIAR_NOTIFICACION |
| ANAL   | fallo NOTIFICADO    | PENDIENTES_FALLO | NINGUNA |
| ANAL   | pago VENCIDO        | PENDIENTE_ANALISIS | DICTAR_FALLO |
| ANAL   | sin condicion especial | PENDIENTE_ANALISIS | DICTAR_FALLO |
| NOTI   | notif en curso      | EN_NOTIFICACION | EVALUAR_NOTIFICACION |
| NOTI   | notif positiva      | PENDIENTE_ANALISIS | DICTAR_FALLO |
| NOTI   | notif sin acuse     | PENDIENTE_ANALISIS | DECIDIR_REINTENTO_O_GESTION |
| NOTI   | sin notif           | PENDIENTE_NOTIFICACION | ENVIAR_NOTIFICACION |
| CAPT   | sin docs            | ACTAS_EN_ENRIQUECIMIENTO | COMPLETAR_CAPTURA |
| ENRI   | sin docs            | ACTAS_EN_ENRIQUECIMIENTO | GENERAR_DOCUMENTO |
| CAPT/ENRI | docs pendientes firma | PENDIENTE_FIRMA | FIRMAR_DOCUMENTO |
| CAPT/ENRI | todos docs firmados   | PENDIENTE_NOTIFICACION | ENVIAR_NOTIFICACION |
| CAPT/ENRI | docs sin estado claro | PENDIENTE_PREPARACION_DOCUMENTAL | GENERAR_DOCUMENTO |

### Routing especial: resultadoFinal ABSUELTO con acta ACTIVA

Si ``acta.resultadoFinal == ABSUELTO`` y ``situacionAdministrativa == ACTIVA``:
- Solo puede ocurrir tras APEABS con bloqueantes materiales activos.
- Snapshot: PENDIENTE_ANALISIS / NINGUNA.
- No usar CON_APELACION en este caso.

### Prioridades de evaluacion en bloque ANAL

1. Pago voluntario PENDIENTE_CONFIRMACION (mayor prioridad)
2. Pago voluntario OBSERVADO
3. Apelacion activa (PRESENTADA) -> CON_APELACION / RESOLVER_APELACION
   Apelacion resuelta (RECHAZADA) -> PENDIENTE_ANALISIS / DECLARAR_CONDENA_FIRME
4. Fallo activo (segun EstadoFalloActa: PENDIENTE_FIRMA -> PENDIENTE_NOTIFICACION -> NOTIFICADO)
5. Pago voluntario VENCIDO
6. Sin condicion especial: PENDIENTE_ANALISIS / DICTAR_FALLO

## Bandejas (CodigoBandeja)

| Valor | Label |
|-------|-------|
| ACTAS_EN_ENRIQUECIMIENTO | Actas en captura/enriquecimiento |
| PENDIENTE_PREPARACION_DOCUMENTAL | Pendiente de preparacion documental |
| PENDIENTE_FIRMA | Pendiente de firma |
| PENDIENTE_NOTIFICACION | Pendiente de notificacion |
| EN_NOTIFICACION | En notificacion esperando resultado |
| PENDIENTE_ANALISIS | Pendiente de analisis juridico |
| PENDIENTES_RESOLUCION_REDACCION | Pendiente de redaccion resolutoria |
| PENDIENTES_FALLO | Fallo notificado, pendiente de paso siguiente |
| CON_APELACION | Con apelacion presentada |
| GESTION_EXTERNA | En gestion externa |
| PARALIZADAS | Paralizadas |
| ARCHIVO | Archivo administrativo |
| PENDIENTE_CONFIRMACION_PAGO | Pendiente de confirmacion de pago voluntario |
| PENDIENTE_PAGO_CONDENA | Pendiente de pago de condena (sin informar o observado) |
| PENDIENTE_CONFIRMACION_PAGO_CONDENA | Pendiente de confirmacion de pago de condena |
| CERRADAS | Cerradas definitivamente |

## Acciones pendientes (AccionPendiente)

| Valor | Descripcion |
|-------|-------------|
| COMPLETAR_CAPTURA | Completar datos de captura |
| ENRIQUECER | Enriquecer acta |
| GENERAR_DOCUMENTO | Generar documento |
| FIRMAR_DOCUMENTO | Firmar documento |
| ENVIAR_NOTIFICACION | Enviar notificacion |
| EVALUAR_NOTIFICACION | Evaluar resultado notificacion |
| DECIDIR_REINTENTO_O_GESTION | Decidir reintento o derivar a gestion |
| DICTAR_FALLO | Dictar fallo (absolutorio o condenatorio) |
| RESOLVER_APELACION | Resolver apelacion presentada |
| DECLARAR_CONDENA_FIRME | Declarar firmeza de condena (post-rechazo apelacion) |
| GESTIONAR_PAGO_CONDENA  | Condena firme declarada - gestionar pago de condena |
| CONFIRMAR_PAGO_CONDENA  | Confirmar pago de condena informado |
| CORREGIR_PAGO_CONDENA   | Corregir pago de condena observado |
| REGISTRAR_PAGO | Registrar pago de condena |
| DERIVAR_GESTION_EXTERNA | Derivar a gestion externa |
| CONFIRMAR_PAGO | Confirmar pago voluntario |
| CORREGIR_PAGO | Corregir pago observado |
| NINGUNA | Sin accion pendiente |

Prohibido: `GESTIONAR_CONDENA_FIRME` no existe en `AccionPendiente`. Fue eliminado; el valor vigente es `GESTIONAR_PAGO_CONDENA`.

---

## Snapshot - routing de pago de condena

### Bandejas de pago de condena

| Bandeja                          | Descripcion                                    |
|----------------------------------|------------------------------------------------|
| PENDIENTE_PAGO_CONDENA           | Pendiente de pago de condena (sin informar o observado) |
| PENDIENTE_CONFIRMACION_PAGO_CONDENA | Pendiente de confirmacion de pago de condena |

### Acciones de pago de condena

| Accion                  | Descripcion                              |
|-------------------------|-------------------------------------------|
| GESTIONAR_PAGO_CONDENA  | Gestionar pago de condena (informar)     |
| CONFIRMAR_PAGO_CONDENA  | Confirmar pago de condena informado      |
| CORREGIR_PAGO_CONDENA   | Corregir pago de condena observado       |

### Routing de CONDENA_FIRME

| Estado pago condena | Bandeja                         | Accion                   |
|--------------------|---------------------------------|--------------------------|
| Sin pago (PENDIENTE) | PENDIENTE_PAGO_CONDENA        | GESTIONAR_PAGO_CONDENA   |
| INFORMADO           | PENDIENTE_CONFIRMACION_PAGO_CONDENA | CONFIRMAR_PAGO_CONDENA |
| OBSERVADO           | PENDIENTE_PAGO_CONDENA        | CORREGIR_PAGO_CONDENA    |
| CONFIRMADO + cerrada | CERRADAS                     | NINGUNA                  |

---

## Snapshot - efecto del reingreso desde gestion externa

### Snapshot tras reingreso

Despues del reingreso, el acta queda con `bloqueActual=ANAL` y `situacionAdministrativa=ACTIVA`.
El snapshot se recalcula segun el estado real del acta.

#### REINGRESO_PARA_REVISION (resultadoFinal = CONDENA_FIRME, sin pago condena)

| Estado pago condena | Bandeja                  | Accion                   |
|---------------------|--------------------------|--------------------------|
| Sin pago (PENDIENTE) | PENDIENTE_PAGO_CONDENA  | GESTIONAR_PAGO_CONDENA   |

> Nota: el snapshot llega a PENDIENTE_PAGO_CONDENA porque el routing del SnapshotRecalculador
> para `resultadoFinal=CONDENA_FIRME` con `situacion=ACTIVA` va primero por estado pago condena.

#### REINGRESO_SIN_PAGO (requiere resultadoFinal = CONDENA_FIRME)

| Estado pago condena | Bandeja                  | Accion                   |
|---------------------|--------------------------|--------------------------|
| Sin pago (PENDIENTE) | PENDIENTE_PAGO_CONDENA  | GESTIONAR_PAGO_CONDENA   |

El snapshot es identico a REINGRESO_PARA_REVISION cuando no hay pago condena informado.
La diferencia es semantica (razon del reingreso), no tecnica.

#### REINGRESO_SIN_PAGO y REINGRESO_PARA_REVISION - casos SIN_PAGO y SIN_CAMBIOS

Los casos `SIN_PAGO + REINGRESO_SIN_PAGO` y `SIN_CAMBIOS + REINGRESO_PARA_REVISION` dejan el acta en:
- `bloqueActual = ANAL`
- `situacionAdministrativa = ACTIVA`
- `resultadoFinal = CONDENA_FIRME` (sin cambiar)

El snapshot resultante es identico al de REINGRESO_PARA_REVISION / REINGRESO_SIN_PAGO documentado arriba:

| Resultado | Estado pago condena | Bandeja | Accion |
|-----------|---------------------|---------|--------|
| CONDENA_FIRME | Sin pago | PENDIENTE_PAGO_CONDENA | GESTIONAR_PAGO_CONDENA |

### Invariante del snapshot tras reingreso

- `situacionAdministrativa = ACTIVA` (ya no `EN_GESTION_EXTERNA`)
- `bloqueActual = ANAL`
- Bandeja derivada del `resultadoFinal` y `estadoPagoCondena`
- `GESTION_EXTERNA` desaparece del snapshot tras el reingreso

---

## Snapshot - pago externo de gestion externa (PAGAPR)

### Routing post-PAGAPR

| Caso | Bandeja | Accion |
|------|---------|--------|
| Sin bloqueantes | CERRADAS | NINGUNA |
| Con bloqueantes activos | PENDIENTE_ANALISIS | NINGUNA |

### Invariante de snapshot post-PAGAPR

- `resultadoFinal = CONDENA_FIRME_PAGADA` en ambos casos.
- Sin bloqueantes: `situacionAdministrativa = CERRADA`, `bloqueActual = CERR`.
- Con bloqueantes: `situacionAdministrativa = ACTIVA`, `bloqueActual = ANAL` (hasta que se resuelvan los bloqueantes).

### Routing en SnapshotRecalculador para CONDENA_FIRME_PAGADA + ACTIVA

Cuando `resultadoFinal == CONDENA_FIRME_PAGADA` y `situacionAdministrativa == ACTIVA`:
- Bandeja: PENDIENTE_ANALISIS
- Accion: NINGUNA
- Precedencia: se evalua despues de ABSUELTO y antes de CONDENA_FIRME.
- Es el routing definitivo mientras existan bloqueantes materiales activos (ver "Motor real de
  bloqueantes - impacto en snapshot").

---

## Motor real de bloqueantes - impacto en snapshot

### Routing CONDENA_FIRME_PAGADA + ACTIVA

El routing CONDENA_FIRME_PAGADA + ACTIVA -> PENDIENTE_ANALISIS / NINGUNA es el comportamiento definitivo.

Aplica a:
- PAGAPR con bloqueantes activos.
- PCOCNF con bloqueantes activos.

Ambos casos dejan resultadoFinal = CONDENA_FIRME_PAGADA y situacionAdministrativa = ACTIVA.
El SnapshotRecalculador los maneja uniformemente: PENDIENTE_ANALISIS / NINGUNA.

El cierre se completa cuando los bloqueantes se resuelvan (siActivo = false) y se ejecute la operacion de
cierre correspondiente (PCOCNF, PAGAPR o el cierre diferido automatico al resolver el ultimo bloqueante;
ver `03-comandos-precondiciones-efectos.md`, seccion "Cierre diferido en CumplirBloqueante y
AnularBloqueante").

---

## Gestion de bloqueantes y snapshot

Las operaciones de gestion de bloqueantes (registrar/cumplir/anular via BloqueanteMaterialService)
NO modifican el snapshot del acta directamente.

El snapshot cambia solo cuando una operacion de cierre consulta existsActivoByActaId() y determina:
- Sin bloqueantes activos: acta -> CERRADA/CERR -> snapshot CERRADAS/NINGUNA.
- Con bloqueantes activos: acta queda ACTIVA/ANAL -> snapshot PENDIENTE_ANALISIS/NINGUNA.

Gestionar bloqueantes resuelve el estado transitorio:
Acta CONDENA_FIRME_PAGADA/ACTIVA/ANAL -> cumplir/anular todos los bloqueantes activos ->
el acta queda lista para cerrar en la siguiente operacion de cierre (PCOCNF, PAGAPR, etc.), o se cierra
automaticamente por el cierre diferido si al resolver el ultimo bloqueante ya existe un resultado cerrable.

---

## Snapshot y bandeja en cierre diferido

Cuando el cierre diferido se dispara al resolver el ultimo bloqueante:

- El acta pasa a `CERRADA / CERR`.
- `CierreActaHelper.emitirCierre()` llama `SnapshotRecalculador.recalcular(acta)`.
- El snapshot resultante queda con `codBandeja = CERRADAS`, `accionPendiente = NINGUNA`.

No hay bandeja transitoria especifica para "cierre pendiente por bloqueantes": el acta queda en
`PENDIENTE_ANALISIS` mientras tiene bloqueantes activos y resultado cerrable pendiente de cierre.

---

## Snapshot y proyeccion economica

- El snapshot activo NO transporta datos economicos: estado/monto de obligacion, estado de forma, estado de plan, cuotas, mora, saldo, importes procesados/confirmados/aplicados, conciliacion, flags de pago ni plan caido.
- `SnapshotRecalculador.proyectarPagos` es un no-op: las lecturas economicas se resuelven directamente desde `FalActaEconomiaProyeccion`.
- La proyeccion economica no es fuente primaria juridica; los movimientos y eventos append-only si.
- `montoOperativoVigente` puede permanecer solo como valorizacion UX, no como dato de pagos.
