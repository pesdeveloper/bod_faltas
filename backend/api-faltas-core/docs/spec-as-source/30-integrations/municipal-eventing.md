# Modelo conceptual del bus de eventos municipal (`bus_evento` + Outbox/checkpoint/Inbox locales)

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Arquitectura conceptual de transporte de eventos entre aplicaciones
> municipales (Faltas, Firmas y futuras aplicaciones). El modelo físico
> completo (`bus_evento`, `fal_outbox`, `fal_bus_checkpoint`, `fal_inbox`,
> `fal_inbox_error`) y su DDL están cerrados y listos (`DEC-EDA-022`,
> `../50-persistence/firma-eda-physical-model.md`); `DDL_NOT_EXECUTED` hasta
> ejecución manual autorizada. El primer consumo concreto de este modelo es
> la integración Faltas↔Firmas descrita en
> `../40-api/firma-event-contracts.md`; su operación concreta (retención,
> códigos de error) está cerrada en `eventing-operations.md`.
>
> **Definición normativa:** el Bus de Eventos Municipal es una tabla
> MariaDB append-only. Cada aplicación publica mediante su Outbox local y
> consume mediante un lector, checkpoint e Inbox locales. El bus no
> administra suscripciones, estados de entrega, reintentos ni workflows. Un
> evento puede no tener consumidores. Los códigos de productor, evento y
> agregado son breves y se modelan como enums o constantes en cada dominio.

## 1. Por qué Outbox local + bus central, y no acoplamiento directo

Cada aplicación (Faltas, Firmas) conserva su propia autoridad de datos y su
propio ciclo de despliegue. Ninguna aplicación llama directamente a la base
de datos ni a la API interna de otra para propagar un hecho de dominio. El
hecho de dominio ya ocurrió y ya fue persistido de forma transaccional por
la aplicación de origen (ej. `FalDocumentoFirmaReq` pasa a `PENDIENTE`); la
publicación del evento correspondiente debe ser una consecuencia atómica de
esa misma transacción, nunca una llamada de red adicional que pueda fallar
por separado.

## 2. Componentes del modelo

```text
Cambio de dominio + Outbox local
              │
              ▼
        relay de la aplicación
              │
              ▼
      bus_evento append-only
              │
              ▼
 lector local + checkpoint
              │
              ▼
         Inbox local
              │
              ▼
   worker de procesamiento local
```

- **Outbox local por aplicación** (`fal_outbox`, `fir_outbox`): tabla propia
  de cada aplicación productora. Se escribe en la misma transacción que el
  cambio de dominio que la origina. Un relay lee la Outbox e inserta en el
  bus central, reutilizando el mismo `evento_id` en cada reintento.
- **`bus_evento`:** única tabla central, append-only, inmutable tras el
  `INSERT`. Es transporte técnico, no es el hecho de dominio en sí — el
  hecho de dominio ya vive en la tabla de la aplicación de origen
  (`FalDocumentoFirmaReq`, `FalActaEvento`, etc.). No registra estados de
  consumo, entregas ni suscriptores.
- **Checkpoint local por aplicación** (`fal_bus_checkpoint`,
  `fir_bus_checkpoint`): registra hasta qué `bus_evento_id` cada aplicación
  ya examinó/copió a su Inbox local. El bus central no sabe qué aplicación
  leyó qué evento; cada consumidor administra su propio progreso.
- **Inbox local por aplicación consumidora** (`fal_inbox`, `fir_inbox`):
  registro de los eventos relevantes ya copiados desde el bus (`PK =
  bus_evento_id`), usado para idempotencia (no procesar dos veces el mismo
  evento) y para desacoplar la lectura del bus del procesamiento funcional.
- **Tabla local de errores de Inbox** (`fal_inbox_error`, `fir_inbox_error`):
  historial de fallos de procesamiento de cada consumidor, separado de la
  fila principal de Inbox.

## 3. Relación entre `bus_evento` (transporte) y evento de dominio (Faltas)

`bus_evento` transporta el hecho ya ocurrido; nunca es la fuente de verdad
del hecho de dominio. En Faltas, la fuente de verdad de un hecho de dominio
es siempre `FalActaEvento` (append-only) o la tabla de la entidad afectada
(`FalDocumentoFirmaReq`, `FalDocumentoFirma`, `FalDocumento`), nunca
`bus_evento`. La reconstrucción de un expediente completo nunca depende de
leer `bus_evento`; ese registro es puramente de transporte/observabilidad
de la integración.

## 4. Envelope estándar de evento

Todo evento publicado en el bus, de cualquier aplicación y cualquier
dominio, comparte el mismo envelope. La representación persistida usa
códigos compactos (`../50-persistence/firma-eda-physical-model.md` §6.1);
el envelope lógico conserva nombres legibles para el payload/contrato:

```json
{
  "eventoId": "uuid",
  "schemaVersion": 1,
  "tipoEvento": "DocumentoFirmado",
  "sistemaOrigen": "firmas",
  "fhEvento": "2026-07-31T22:15:00Z",
  "correlationId": "uuid",
  "causationId": "uuid",
  "agregado": { "tipo": "FalDocumentoFirmaReq", "id": "1234-1" },
  "payload": { }
}
```

- `eventoId`: clave de idempotencia de publicación, única por evento
  (`UNIQUE(evento_id)` físico en `bus_evento`).
- `schemaVersion`: versión del contrato de `payload` para ese `tipoEvento`.
- `correlationId`: agrupa todos los eventos de un mismo flujo de negocio de
  extremo a extremo (ej. una solicitud de firma completa).
- `causationId`: referencia al evento que causó este evento, cuando
  corresponda.
- `agregado`: identifica de forma agnóstica a qué entidad de dominio se
  refiere el evento, sin exponer detalles físicos.
- `payload`: contrato específico de cada `tipoEvento`, definido en el
  documento de dominio correspondiente (ej. `../40-api/firma-event-contracts.md`).

## 5. Seguridad

- El bus central (`bus_evento`, única tabla) es una tabla MariaDB, no una
  API HTTP: cada aplicación productora/consumidora accede a ella por
  **JDBC/SQL** con un usuario MariaDB de mínimo privilegio propio
  (`bus_publisher_*`: `INSERT` column-level, excluyendo `bus_evento_id`;
  `bus_consumer_*`: `SELECT` de tabla completa, `DEC-EDA-027`), nunca
  mediante la identidad del usuario humano que originó el hecho de dominio
  (`eventing-operations.md` §2bis,
  `../50-persistence/firma-eda-physical-model.md` §10). No hay rutinas
  almacenadas ni vistas intermediarias: la aplicación ejecuta sus propias
  sentencias SQL.
- El bus central no reenvía ni almacena información de autenticación de
  usuarios humanos; el envelope solo transporta identificadores funcionales
  de dominio, nunca tokens ni credenciales.
- `X-Api-Key` (`DEC-EDA-018`) autentica exclusivamente los tres endpoints
  HTTP de transferencia binaria Faltas↔Firmas
  (`firma-binary-transfer.md`); nunca el bus central de eventos. Ver
  `eventing-operations.md` §1-§2bis para la separación completa entre
  transporte HTTP y transporte SQL.

## 6. Operación

Retención y códigos de observabilidad locales están cerrados en
`eventing-operations.md` (`DEC-EDA-019`). No existe un procedimiento de
purga central (`sp_bus_purgar`): la retención de `bus_evento` es una tarea
operativa externa, ejecutada por la cuenta de mantenimiento (`SELECT`,
`DELETE`), fuera de la ruta de publicación/consumo.

## 7. Relación con el DDL

El modelo físico completo de `bus_evento`, `fal_outbox`,
`fal_bus_checkpoint`, `fal_inbox` y `fal_inbox_error` está definido en
`../50-persistence/firma-eda-physical-model.md` y materializado en el DDL
de este cierre (`database/ddl/`), con sus `DECISION_DDL-*` correspondientes
en `../50-persistence/ddl-decisions.md`. `fir_outbox`/`fir_bus_checkpoint`/
`fir_inbox`/`fir_inbox_error` pertenecen al bounded context Firmas
(aplicación/repositorio distinto) y su propio paquete DDL **ejecutable**
vive en `database/ddl/firmas/` (`firma-eda-physical-model.md` §8).
`DDL_READY` / `DDL_NOT_EXECUTED`: el esquema no ha sido creado; la
ejecución manual controlada requiere autorización humana explícita
posterior a este cierre documental.
