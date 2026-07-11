# Dimensiones de estado y lifecycle

## Principio rector

> Una misma acta puede tener simultaneamente un bloque, un estado procesal, una
> situacion administrativa, un resultado final, estados propios de sus subagregados
> y proyecciones operativas. Ninguna de esas dimensiones reemplaza a las demas.

---

## Matriz de dimensiones

| Dimension | Naturaleza | Persistencia | Propietario | Puede disparar transicion | Ejemplo |
|---|---|---|---|---|---|
| `BloqueActual` | Estado macro del acta | Persistida en acta | `FalActa` | No por si misma; puede ser precondicion o resultado de una transicion | `ANAL` |
| `EstadoProcesalActa` | Estado juridico-procesal | Persistida en acta | `FalActa` | No por si misma; puede ser precondicion o resultado de una transicion | `EN_TRAMITE` |
| `SituacionAdministrativaActa` | Condicion operativa transversal | Persistida en acta | `FalActa` | No por si misma; puede ser precondicion o resultado de una transicion | `ACTIVA` |
| `ResultadoFinalActa` | Desenlace sustantivo | Persistida en acta | `FalActa` | No por si misma; puede ser precondicion o resultado de una transicion | `CONDENA_FIRME` |
| Estado de fallo (`EstadoFalloActa`) | Estado del subagregado fallo | Persistida en `FalActaFallo` | `FalActaFallo` | No por si misma; puede ser precondicion o resultado de una transicion | `PENDIENTE_NOTIFICACION` |
| Estado documental (`EstadoDocu`) | Estado del subagregado documento | Persistida en `FalDocumento` | `FalDocumento` | No por si misma; puede ser precondicion o resultado de una transicion | `FIRMADO` |
| Estado notificatorio (`EstadoNotificacion`) | Estado del subagregado notificacion | Persistida en `FalNotificacion` | `FalNotificacion` | No por si misma; puede ser precondicion o resultado de una transicion | `PENDIENTE_ENVIO` |
| `TipoEventoActa` | Evento historico inmutable | Persistida append-only | `FalActaEvento` | No; registra una transicion ya ocurrida | `DOCFIR` |
| Hitos temporales (`fhDictado`, `fhFirma`, etc.) | Datos historicos | Persistidos en subagregado | `FalActaFallo` | No por si mismos; prueban una frontera ya alcanzada | `fhFirma != null` |
| `FalActaSnapshot` | Proyeccion operativa | Derivada, regenerable | `FalActaSnapshot` | No; son derivados y no habilitan dominio | bloque + bandeja calculada |
| `CodigoBandeja` | Agrupacion operativa derivada | Derivada en snapshot | `FalActaSnapshot` | No; son derivados y no habilitan dominio | `PENDIENTE_FIRMA` |
| Subbandeja | Clasificacion secundaria derivada | Derivada en snapshot | `FalActaSnapshot` | No; son derivados y no habilitan dominio | refinamiento visual |
| `AccionPendiente` | Recomendacion derivada | Derivada en snapshot | `FalActaSnapshot` | No; son derivados y no habilitan dominio | `FIRMAR_DOCUMENTO` |

---

## Dimensiones persistidas del acta

La condicion principal del acta se expresa mediante cuatro dimensiones
independientes, cada una con semantica propia. Ninguna resume a las otras.

### `bloqueActual` -- `BloqueActual`

Etapa macro del circuito. Valores exactos del enum Java vigente:

| Valor | Codigo | Descripcion |
|---|---|---|
| `CAPT` | `CAPT` | Captura/labrado inicial |
| `ENRI` | `ENRI` | Enriquecimiento/completitud del acta |
| `NOTI` | `NOTI` | Notificacion del acta, fallo u otra pieza |
| `ANAL` | `ANAL` | Analisis, resolucion, fallo, pagos y apelacion |
| `GEXT` | `GEXT` | Gestion externa: apremio / juzgado de paz |
| `ARCH` | `ARCH` | Archivo administrativo/procesal |
| `CERR` | `CERR` | Cierre definitivo del circuito |

> `D3_DOCUMENTAL` no es un bloque. Se representa mediante bandeja documental,
> `FalDocumento` y estados de firma.

### `estadoProcesal` -- `EstadoProcesalActa`

Estado juridico-procesal del tramite. Valores exactos:

| Valor | Codigo | Descripcion |
|---|---|---|
| `EN_TRAMITE` | `TRAM` | Expediente en curso |
| `CONCLUIDO` | `CONC` | Expediente concluido |
| `PRESCRIPTO` | `PRSC` | Expediente prescripto |

### `situacionAdministrativa` -- `SituacionAdministrativaActa`

Condicion operativa transversal. Valores exactos:

| Valor | Codigo | Descripcion |
|---|---|---|
| `ACTIVA` | `ACTV` | Acta activa en circuito normal |
| `PARALIZADA` | `PARA` | Acta paralizada administrativamente |
| `EN_GESTION_EXTERNA` | `GEXT` | En gestion externa (apremio/juzgado) |
| `ARCHIVADA` | `ARCH` | Archivada administrativamente |
| `CERRADA` | `CERR` | Cerrada definitivamente |
| `ANULADA` | `ANUL` | Anulada |

### `resultadoFinal` -- `ResultadoFinalActa`

Desenlace sustantivo del expediente. Valores exactos:

| Valor | Codigo numerico | Descripcion |
|---|---|---|
| `SIN_RESULTADO_FINAL` | 0 | Sin resultado aun |
| `PAGO_VOLUNTARIO_PAGADO` | 1 | Resuelto por pago voluntario |
| `ABSUELTO` | 2 | Fallo absolutorio |
| `CONDENA_FIRME` | 3 | Condena firme |
| `CONDENA_FIRME_PAGADA` | 4 | Condena firme pagada |
| `FALLO_CONDENATORIO_PAGADO` | 5 | Fallo condenatorio pagado antes de firmeza |
| `FALLO_CONDENATORIO_GESTION_EXTERNA` | 6 | Fallo condenatorio derivado a gestion externa |
| `PRESCRIPTO` | 7 | Prescripcion |
| `ANULADO` | 8 | Anulacion |
| `NULIDAD` | 9 | Nulidad declarada |

---

## Estados de subagregados

Los subagregados fallo, documento, notificacion, pago, apelacion y otros tienen
lifecycle propio. Ese lifecycle es independiente de las cuatro dimensiones del
acta descritas arriba.

En este documento se documenta exhaustivamente unicamente `EstadoFalloActa`, por
estar formalmente aprobado. Los demas subagregados se documentaran en documentos
posteriores cuando sus reglas sean absorbidas tematicamente.

---

## Lifecycle del fallo

[FALLO-STATE-001] El enum `EstadoFalloActa` contiene exactamente estos valores:

| Valor | Descripcion |
|---|---|
| `PENDIENTE_FIRMA` | Fallo dictado; documento generado; pendiente de firma obligatoria |
| `PENDIENTE_NOTIFICACION` | Ultima firma obligatoria confirmada; `fhFirma` registrado; fallo listo para continuar el circuito notificatorio. La cabecera `FalNotificacion` se prepara cuando la plantilla es notificable. |
| `NOTIFICADO` | Resultado notificatorio positivo registrado; `fhNotificacion` registrado |
| `FIRME` | Firmeza de condena declarada; `fhFirmeza` registrado |
| `REEMPLAZADO` | Fallo sustituido por otro (estado lateral terminal) |
| `SIN_EFECTO` | Fallo invalidado o dejado sin efecto (estado lateral terminal) |

> `DICTADO` y `FIRMADO` no son estados del fallo. Son hechos persistidos en
> `fhDictado` y `fhFirma` respectivamente. No deben reintroducirse como valores
> del enum ni como aliases en codigo o documentacion.

[FALLO-TRANS-001] Transiciones validas:

```
PENDIENTE_FIRMA
    --ultima firma obligatoria confirmada-->
PENDIENTE_NOTIFICACION
    --resultado notificatorio positivo-->
NOTIFICADO
    --declaracion de firmeza (solo para fallo CONDENATORIO)-->
FIRME
```

Estados laterales:

- `REEMPLAZADO`: fallo sustituido por otro; no continua el flujo principal.
- `SIN_EFECTO`: fallo invalidado o dejado sin efecto; no continua el flujo
  principal.

Reglas:

- No se permite volver hacia atras en el lifecycle principal.
- No se permite saltear estados dentro del lifecycle principal PENDIENTE_FIRMA -> PENDIENTE_NOTIFICACION -> NOTIFICADO -> FIRME.
- Las transiciones hacia REEMPLAZADO o SIN_EFECTO son laterales y se rigen por sus propios comandos, precondiciones e invariantes. No constituyen un salto dentro del lifecycle principal.
- `PENDIENTE_FIRMA -> PENDIENTE_NOTIFICACION` requiere `fhFirma`.
- `PENDIENTE_NOTIFICACION -> NOTIFICADO` requiere `fhNotificacion`.
- `NOTIFICADO -> FIRME` requiere `fhFirmeza` y solo aplica a fallos
  `CONDENATORIO`.
- Las transiciones laterales hacia `REEMPLAZADO` o `SIN_EFECTO` conservan la
  historia ya registrada y se rigen por sus propios contratos. Este documento
  no inventa hitos adicionales para ellas.
- Las operaciones de dominio validan el estado de origen antes de aplicar la
  transicion.

[FALLO-HITO-001] Hitos del fallo:

| Campo | Tipo | Hito correspondiente |
|---|---|---|
| `fhDictado` | `LocalDateTime` | Momento en que se dicto el fallo |
| `fhFirma` | `LocalDateTime` | Momento en que se completaron todas las firmas obligatorias |
| `fhNotificacion` | `LocalDateTime` | Momento en que se registro el resultado notificatorio positivo |
| `fhFirmeza` | `LocalDateTime` | Momento en que se declaro la firmeza de condena |

---

## Hechos, eventos e hitos

Estado, evento e hito son formas complementarias de representar el avance del
circuito:

- el estado expresa la situacion vigente;
- el evento registra de manera inmutable un hecho ocurrido;
- el hito registra la fecha y hora de una frontera de negocio.

Una transicion puede tener evento e hito asociados cuando el contrato del
circuito los define expresamente. No debe inventarse un evento o un campo
temporal por analogia.

En la transicion de firma del fallo estan definidos los tres elementos:

| Aspecto | Valor |
|---|---|
| Evento (`TipoEventoActa`) | `DOCFIR` |
| Hito en el fallo | `fhFirma != null` |
| Estado del fallo | `PENDIENTE_NOTIFICACION` |

En este avance concreto, los tres representan aspectos diferentes del mismo
hecho. Consultar solo `fhFirma` no informa el estado; consultar solo el estado
no prueba cuando ocurrio.

---

## Snapshot, bandejas y acciones

El snapshot (`FalActaSnapshot`) es una proyeccion operativa derivada y regenerable.
No es fuente normativa del lifecycle ni propietario del estado de dominio.

Reglas:

- El snapshot se recalcula desde el estado persistido mediante
  `SnapshotRecalculador`.
- Una bandeja agrupa actas para facilitar el trabajo operativo.
- Una accion pendiente indica la proxima actuacion sugerida al operador.
- Bandeja y accion se recalculan desde el estado persistido; no lo modifican.
- `subBandeja` es clasificacion visual/operativa secundaria.

> No se persiste un estado de dominio unicamente para reproducir el nombre de una
> bandeja.

---

## Invariantes transversales

1. Ningun estado derivado --snapshot, bandeja o accion-- puede contradecir las
   dimensiones persistidas del agregado.
2. Si `estadoFallo = PENDIENTE_FIRMA`, `fhFirma` debe ser nulo.
3. Si `estadoFallo` es `PENDIENTE_NOTIFICACION`, `NOTIFICADO` o `FIRME`,
   `fhFirma` debe estar registrado.
4. Si `estadoFallo` es `NOTIFICADO` o `FIRME`, `fhNotificacion` debe estar
   registrado.
5. Si `estadoFallo = FIRME`:
   - el fallo debe ser `CONDENATORIO`;
   - `fhFirmeza` debe estar registrado;
   - `siFirme` debe ser verdadero;
   - `origenFirmeza` debe estar registrado.
6. `REEMPLAZADO` y `SIN_EFECTO` preservan los hitos historicos que ya existian.
   Su compatibilidad temporal depende del estado desde el cual se ejecuto la
   transicion lateral.
7. Una transicion no puede borrar historia: los campos `fh*` y los eventos son
   inmutables una vez registrados.
8. Las operaciones sensibles usan `FaltasClock` como unica fuente de tiempo.
9. Los reintentos externos deben ser idempotentes: una misma `referenciaFirmaExt`
   no puede crear firmas duplicadas.
10. MariaDB debera preservar las mismas transiciones e invariantes que la
    implementacion InMemory.
11. Los aliases de compatibilidad (`@Deprecated`) no son estados canonicos.
12. Una bandeja no habilita por si sola una transicion de dominio.

---

## Consultas conceptuales

Las siguientes expresiones describen como consultar el estado del sistema en
terminos del modelo persistido:

```
fallos que fueron firmados:
    fhFirma != null

fallos actualmente pendientes de notificacion:
    estadoFallo = PENDIENTE_NOTIFICACION

trabajo notificatorio pendiente:
    estadoNotificacion = PENDIENTE_ENVIO

actas visibles en una bandeja:
    snapshot.codigoBandeja = <valor>
```

La ultima consulta es operativa y derivada. Para verificar el estado real del
acta, consultar las dimensiones persistidas del agregado, no el snapshot.

---

## Relacion con documentos transitorios

Los documentos numerados `02-estados-bloques-eventos.md` y
`04-snapshot-bandejas-acciones.md` continuan como material de extraccion y
trazabilidad mientras sus reglas se absorben tematicamente.

Ante una contradiccion sobre dimensiones, lifecycle o derivacion operativa,
este documento es normativo.
