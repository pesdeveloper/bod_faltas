 04 - Snapshot, Bandejas y Acciones

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
| ANAL   | fallo DICTADO/PENDIENTE_FIRMA | PENDIENTE_FIRMA | FIRMAR_DOCUMENTO |
| ANAL   | fallo FIRMADO/PENDIENTE_NOTIFICACION | PENDIENTE_NOTIFICACION | ENVIAR_NOTIFICACION |
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
3. Apelacion activa (PRESENTADA) -> CON_APELACION / RESOLVER_APELACION (Slice 3B)
   Apelacion resuelta (RECHAZADA) -> PENDIENTE_ANALISIS / DECLARAR_CONDENA_FIRME (Slice 3C)
4. Fallo activo (segun EstadoFalloActa: DICTADO/PENDIENTE_FIRMA -> FIRMADO/PENDIENTE_NOTIFICACION -> NOTIFICADO)
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
| CON_APELACION | Con apelacion presentada (Slice 3B) |
| GESTION_EXTERNA | En gestion externa |
| PARALIZADAS | Paralizadas |
| ARCHIVO | Archivo administrativo |
| PENDIENTE_CONFIRMACION_PAGO | Pendiente de confirmacion de pago voluntario |
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
| DECLARAR_CONDENA_FIRME | Declarar firmeza de condena (post-rechazo apelacion) - antes de Slice 3D |
| GESTIONAR_PAGO_CONDENA  | Condena firme declarada - gestionar pago de condena (Slice 5)             |
| REGISTRAR_PAGO | Registrar pago de condena (Slice 3E) |
| DERIVAR_GESTION_EXTERNA | Derivar a gestion externa |
| CONFIRMAR_PAGO | Confirmar pago voluntario |
| CORREGIR_PAGO | Corregir pago observado |
| NINGUNA | Sin accion pendiente |

---

## Slice 5: Snapshot - routing de pago de condena

### Bandejas nuevas (Slice 5)

| Bandeja                          | Descripcion                                    |
|----------------------------------|------------------------------------------------|
| PENDIENTE_PAGO_CONDENA           | Pendiente de pago de condena (sin informar o observado) |
| PENDIENTE_CONFIRMACION_PAGO_CONDENA | Pendiente de confirmacion de pago de condena |

### Acciones nuevas (Slice 5)

| Accion                  | Descripcion                              |
|-------------------------|------------------------------------------|
| GESTIONAR_PAGO_CONDENA  | Gestionar pago de condena (informar)     |
| CONFIRMAR_PAGO_CONDENA  | Confirmar pago de condena informado      |
| CORREGIR_PAGO_CONDENA   | Corregir pago de condena observado       |

### Routing de CONDENA_FIRME (implementado en Slice 5)

| Estado pago condena | Bandeja                         | Accion                   |
|--------------------|---------------------------------|--------------------------|
| Sin pago (PENDIENTE) | PENDIENTE_PAGO_CONDENA        | GESTIONAR_PAGO_CONDENA   |
| INFORMADO           | PENDIENTE_CONFIRMACION_PAGO_CONDENA | CONFIRMAR_PAGO_CONDENA |
| OBSERVADO           | PENDIENTE_PAGO_CONDENA        | CORREGIR_PAGO_CONDENA    |
| CONFIRMADO + cerrada | CERRADAS                     | NINGUNA                  |

**Nota:** `GESTIONAR_CONDENA_FIRME` fue eliminado del enum `AccionPendiente` (micro-slice pre-Slice 6). El valor vigente es `GESTIONAR_PAGO_CONDENA`.


---

## Slice 6B: Snapshot - efecto del reingreso

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

### Invariante del snapshot tras reingreso

- `situacionAdministrativa = ACTIVA` (ya no `EN_GESTION_EXTERNA`)
- `bloqueActual = ANAL`
- Bandeja derivada del `resultadoFinal` y `estadoPagoCondena`
- `GESTION_EXTERNA` desaparece del snapshot tras el reingreso


---

## Slice 6C: Snapshot - pago externo de gestion externa (PAGAPR)

### Routing post-PAGAPR

| Caso | Bandeja | Accion |
|------|---------|--------|
| Sin bloqueantes | CERRADAS | NINGUNA |
| Con bloqueantes activos | PENDIENTE_ANALISIS | NINGUNA |

### Invariante de snapshot post-PAGAPR

- esultadoFinal = CONDENA_FIRME_PAGADA en ambos casos.
- Sin bloqueantes: situacionAdministrativa = CERRADA, loqueActual = CERR.
- Con bloqueantes: situacionAdministrativa = ACTIVA, loqueActual = ANAL (transitorio).

### Routing en SnapshotRecalculador para CONDENA_FIRME_PAGADA + ACTIVA

Cuando esultadoFinal == CONDENA_FIRME_PAGADA y situacionAdministrativa == ACTIVA:
- Bandeja: PENDIENTE_ANALISIS
- Accion: NINGUNA
- Precedencia: se evalua despues de ABSUELTO y antes de CONDENA_FIRME.
- Estado transitorio hasta Slice 7 (motor real de bloqueantes y cerrabilidad).



---

## Slice 6D-1: Snapshot - reingreso SIN_PAGO y SIN_CAMBIOS

### Routing identico a Slice 6B tras reingreso

Los casos `SIN_PAGO + REINGRESO_SIN_PAGO` y `SIN_CAMBIOS + REINGRESO_PARA_REVISION` dejan el acta en:
- `bloqueActual = ANAL`
- `situacionAdministrativa = ACTIVA`
- `resultadoFinal = CONDENA_FIRME` (sin cambiar)

Por lo tanto el snapshot es identico al de Slice 6B:

| Resultado | Estado pago condena | Bandeja | Accion |
|-----------|---------------------|---------|--------|
| CONDENA_FIRME | Sin pago | PENDIENTE_PAGO_CONDENA | GESTIONAR_PAGO_CONDENA |

No se agrega ningun nuevo routing al SnapshotRecalculador en Slice 6D-1.
---

## Slice 7A: Motor real de bloqueantes - impacto en snapshot

### Routing CONDENA_FIRME_PAGADA + ACTIVA (definitivo desde Slice 7A)

La nota "Estado transitorio hasta Slice 7" queda obsoleta.
El routing CONDENA_FIRME_PAGADA + ACTIVA -> PENDIENTE_ANALISIS / NINGUNA es el comportamiento definitivo.

Aplica a:
- PAGAPR con bloqueantes activos (implementado en Slice 6C).
- PCOCNF con bloqueantes activos (implementado en Slice 7A).

Ambos casos dejan resultadoFinal = CONDENA_FIRME_PAGADA y situacionAdministrativa = ACTIVA.
El SnapshotRecalculador los maneja uniformemente: PENDIENTE_ANALISIS / NINGUNA.

El cierre se completa cuando los bloqueantes se resuelvan (siActivo = false) y se ejecute
una accion futura de cierre manual o automatico (pendiente de implementacion en slice posterior).

---

## Nota Slice 7B: Gestion de bloqueantes y snapshot

Las operaciones de gestion de bloqueantes (registrar/cumplir/anular via BloqueanteMaterialService)
NO modifican el snapshot del acta directamente.

El snapshot cambia solo cuando una operacion de cierre consulta existsActivoByActaId() y determina:
- Sin bloqueantes activos: acta -> CERRADA/CERR -> snapshot CERRADAS/NINGUNA.
- Con bloqueantes activos: acta queda ACTIVA/ANAL -> snapshot PENDIENTE_ANALISIS/NINGUNA.

Gestionar bloqueantes resuelve el estado transitorio:
Acta CONDENA_FIRME_PAGADA/ACTIVA/ANAL -> cumplir/anular todos los bloqueantes activos ->
el acta queda lista para cerrar en la siguiente operacion de cierre (PCOCNF, PAGAPR, etc.).

No existe actualmente una operacion de "reintentar cierre" pendiente de bloqueantes.
Esa operacion (o un worker programado) queda para slices posteriores.

---

## Slice 7C: Snapshot y bandeja en cierre diferido (implementado)

Cuando el cierre diferido se dispara al resolver el ultimo bloqueante:

- El acta pasa a `CERRADA / CERR`.
- `CierreActaHelper.emitirCierre()` llama `SnapshotRecalculador.recalcular(acta)`.
- El snapshot resultante queda con `codBandeja = CERRADAS`, `accionPendiente = NINGUNA`.

No hay bandeja transitoria especifica para "cierre pendiente por bloqueantes": el acta queda en
`PENDIENTE_ANALISIS` mientras tiene bloqueantes activos y resultado cerrable pendiente de cierre.
