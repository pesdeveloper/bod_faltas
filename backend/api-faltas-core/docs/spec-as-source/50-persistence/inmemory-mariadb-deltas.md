# Delta transversal: modelo InMemory vigente vs. MariaDB objetivo

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> No contiene el DDL definitivo. Describe unicamente los deltas transversales de
> naturaleza fisica/infraestructural entre la implementacion InMemory vigente y el
> objetivo MariaDB; no repite el inventario campo-por-campo, que vive en
> [`mariadb-logical-model.md`](mariadb-logical-model.md).

## 1. Alcance de este documento

Este documento no describe deltas de entidades o campos faltantes: ese cierre
funcional (contrato funcional completo, sin entidades ni campos pendientes por
incorporar) esta registrado como decision cerrada en
[`mariadb-logical-model.md`](mariadb-logical-model.md). Este documento
(`inmemory-mariadb-deltas.md`) y `mariadb-logical-model.md` son complementarios:
este describe los deltas fisicos transversales; `mariadb-logical-model.md` es
la matriz vigente por agregado/puerto para el diseno de DDL/JDBC. Ninguno de
los dos reemplaza al otro.

Este documento cubre exclusivamente los deltas **transversales de naturaleza
fisica** que existen porque InMemory es un prototipo funcional de una sola JVM y
MariaDB es una base relacional multi-nodo. Estos deltas no son gaps funcionales:
son decisiones de diseno fisico que el bloque de DDL/JDBC debe resolver sin
alterar el comportamiento de dominio ya validado.

## 2. Identidad

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Generacion de identificador | Contador `AtomicLong`/campo `nextId` en memoria por repositorio | `AUTO_INCREMENT` o secuencia equivalente por tabla | El objetivo debe garantizar unicidad e monotonicidad bajo escritura concurrente multi-nodo; InMemory solo la garantiza dentro de una JVM. |
| Tipo de identificador | `Long` en las entidades ya migradas (ver `110`, inventario de identidad por agregado) | `BIGINT` | Sin delta de tipo pendiente; confirmar mapeo 1:1 en el DDL. |
| Identidad natural (claves de negocio) | Validada en memoria antes de insertar (p. ej. `loteCodigo`, `referenciaFirmaExt`, `referenciaExterna`) | Debe reforzarse con restriccion `UNIQUE` fisica | Ver seccion 3. |

## 3. Unicidades

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Mecanismo | Operaciones atomicas de repositorio in-memory (`guardarSiAusentePorReferencia`, `guardarActivoSiAusentePorFecha`, verificacion previa en `LoteCorreoRepository`, etc.) sincronizadas dentro de una sola JVM | Restricciones `UNIQUE` a nivel de columna o indice compuesto | InMemory nunca delega la unicidad al motor de persistencia; MariaDB debe hacerlo para sostener la garantia con multiples instancias de aplicacion. |
| Claves de idempotencia conocidas | `referenciaFirmaExt` (firma), `referenciaExterna` + `origenMovimiento` (pagos), `origenMovimiento` + `cmtePG` + `prefPG` + `nroPG` (recibo real de pago), `loteCodigo` (lote de correo), fecha (dia no computable activo), `movimientoOrigenId` en `fal_acta_pago_movimiento` (unicidad de aplicacion de un pago anterior resuelto) | Deben expresarse como restriccion `UNIQUE` (simple o parcial/condicional segun el motor) | Decision fisica pendiente para el bloque de DDL: marcar `DECISION_DDL` por clave, especialmente donde la unicidad es solo sobre el subconjunto "activo" (p. ej. una excepcion de calendario activa por fecha, una gestion externa activa por acta). No existe `fal_acta_pago_resolucion`: la unicidad de la resolucion de `ResolverPagoObligacionAnteriorCommand` es la unicidad de `movimientoOrigenId` en el propio `fal_acta_pago_movimiento` (a lo sumo un movimiento de aplicacion por movimiento `PAGANT` original); ver `110` para el detalle conceptual del portador de esa unicidad y `DECISION_DDL-PAGO-MOV-01`. La clave de recibo (`origenMovimiento` + `cmtePG` + `prefPG` + `nroPG`) se verifica de forma **atomica** dentro del mismo bloque `synchronized` de `InMemoryPagoMovimientoRepository.append` que ya protege `id` y `origenMovimiento` + `referenciaExterna` (no es solo una verificacion previa no atomica); el equivalente fisico en MariaDB es la restriccion `UNIQUE` condicional descrita en `DECISION_DDL-PAGO-MOV-01`. El motivo de la resolucion de un pago anterior se persiste en el propio movimiento de aplicacion (`fal_acta_pago_movimiento.motivo_aplicacion_pago_anterior`, columna nueva nullable), no en el texto del evento `PAGRES`; ver `110` para el detalle de columna. |

## 4. Ordenacion determinista

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Orden de eventos (`ActaEvento`) | Orden de inserccion en la lista in-memory (equivalente a orden de alta) | Requiere `ORDER BY` explicito con desambate deterministico (p. ej. `id` autoincremental como desempate de `fhEvento` igual) | El campo de desempate debe declararse explicitamente en el DDL; no asumir que el orden fisico de almacenamiento en disco coincide con el orden de alta. |
| Orden de listados (documentos, notificaciones, intentos) | Orden de inserccion en memoria | Requiere `ORDER BY` explicito por columna de auditoria o id | Mismo criterio que arriba. |

## 5. Seleccion de registro activo/vigente

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Mecanismo | Recorrido en memoria filtrando por flags booleanos (`siVigente`, `siActiva`, `siActivo`, estado `!= SIN_EFECTO`, etc.) | Consulta indexada `WHERE si_vigente = 1` (o equivalente) | El DDL debe indexar las columnas de vigencia usadas en consultas frecuentes (bandejas, snapshot, callback de firmas). |
| Invariante de unicidad de "activo" | Verificada en el repositorio antes de guardar (p. ej. como maximo una `FalNotificacion` activa por documento; como maximo un fallo `siVigente = true` por acta) | Debe reforzarse con restriccion fisica (indice unico parcial/condicional o verificacion transaccional) | Ver `DECISION_DDL` en seccion 3; el motor MariaDB elegido determina si soporta indices unicos parciales. |

## 6. Atomicidad y transacciones

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Garantia actual | `CONC-STD-001` (`00-governance/command-contract-standard.md`) declara explicitamente que la implementacion InMemory **no** ofrece rollback transaccional conjunto entre multiples repositorios; una mutacion parcial antes de una falla puede dejar el estado inconsistente | Debe ejecutar los efectos de cada comando dentro de una unica transaccion | Delta critico: ningun comando puede asumir en MariaDB la misma tolerancia a fallos parciales que tiene hoy en memoria. Todo comando multi-entidad (por ejemplo, completar firma: documento + fallo + notificacion + evento + snapshot) debe mapearse a una transaccion unica. |
| Evento y transicion en la misma transaccion | Garantizado por ejecucion secuencial sincrona en memoria | Debe garantizarse explicitamente: un evento no puede persistir si la transicion que representa no persistio en la misma transaccion | Ver `CONC-STD-001`. |

## 7. Reloj (`FaltasClock`)

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Captura | `FaltasClock.now()` se invoca exactamente una vez por operacion exitosa, antes de la primera mutacion; el mismo instante se reutiliza para todos los hitos y eventos de esa frontera de negocio (`CMD-ORDER-002`) | Debe preservarse la misma disciplina: un unico instante de aplicacion por comando, escrito en todas las columnas de fecha/hora que representen la misma frontera | Sin delta de comportamiento; el delta es puramente de mecanismo: en JDBC, el instante debe capturarse en la capa de aplicacion (Java) antes de construir la sentencia SQL, no mediante funciones de fecha del motor (`NOW()`, `CURRENT_TIMESTAMP` de MariaDB), para preservar testabilidad y evitar drift entre columnas de la misma transaccion. |

## 8. Auditoría

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Campos | `fhAlta`, `idUserAlta`, `fhMod`/`fhBaja`, `idUserMod`/`idUserBaja` ya incorporados en las entidades auditables (ver `110`, inventario de auditoria por agregado) | Columnas equivalentes `NOT NULL` donde el dominio los exige | Sin delta funcional pendiente; el DDL debe respetar la nulabilidad exacta documentada por entidad en `110`. |

## 9. Nulos

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Regla general | La nulabilidad de cada campo esta definida por las precondiciones e invariantes de dominio (ver contratos de comando y `10-domain/lifecycle-states.md`), no por el tipo Java | El DDL debe declarar `NOT NULL` exactamente donde el dominio lo exige, campo por campo, segun la matriz `110` | No inventar nulabilidad por analogia con otras columnas del mismo tipo. |

## 10. Longitudes

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Regla general | Las longitudes maximas validadas en servicios (p. ej. `idUserAlta`/`idUserBaja` hasta 36 caracteres en `CalendarioAdministrativoService`; `referenciaExterna` hasta 200 caracteres en excepciones de calendario sincronizadas; `sub` del JWT hasta 36 caracteres en la integracion de pagos) son la fuente de longitud para el DDL | `VARCHAR(N)` debe usar exactamente la longitud validada por el servicio de aplicacion, no un valor generico | Marcar `DECISION_DDL` por columna donde la spec no documenta explicitamente una longitud maxima y deba inferirse de Bean Validation o de un valor por defecto conservador. |

## 11. Enums y catálogos

No todo enum de dominio con relevancia de persistencia expone un codigo explicito. Se
distinguen tres categorias (ver [`ddl-decisions.md`](ddl-decisions.md), `DECISION_DDL-ENUM-01`, y
[`jdbc-strategy.md`](jdbc-strategy.md), seccion 6):

| Categoria | Definicion | Persistencia candidata |
|---|---|---|
| `EXPLICIT_NUMERIC_CODE` | El enum expone `codigo()` numerico (`short`) | Columna `SMALLINT` con el codigo explicito |
| `EXPLICIT_STRING_CODE` | El enum expone `codigo()` de tipo `String` estable | Columna `CHAR`/`VARCHAR` exacta con constraint |
| `NO_EXPLICIT_CODE` | El enum no expone `codigo()` | Pendiente de `DECISION_DDL-ENUM-01`; prohibido `ordinal()`; prohibido `name()` sin decision explicita |

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Enums `EXPLICIT_NUMERIC_CODE`/`EXPLICIT_STRING_CODE` | Codigo explicito verificado en el enum Java (ver `../00-governance/glossary.md` y [`mariadb-logical-model.md`](mariadb-logical-model.md)) | Columna `SMALLINT` (o `CHAR`/`VARCHAR` para codigo `String`) con el codigo explicito del enum Java | Sin delta de estrategia pendiente para estos enums. `ResultadoFinalActa` tiene 10 codigos vigentes (0-9); el codigo 5 (`FALLO_CONDENATORIO_PAGADO`) es LEGACY_RESERVED (ver [`ddl-decisions.md`](ddl-decisions.md), `DECISION_DDL-RF-005`). |
| Enums `NO_EXPLICIT_CODE` (`EstadoFalloActa`, `EstadoApelacionActa`, `EstadoPagoCondena`) | Sin `codigo()`; comparacion y persistencia in-memory por referencia de enum Java, sin columna fisica | Representacion fisica pendiente: `DECISION_DDL-ENUM-01` (agregar `codigo()` estable antes del adapter JDBC, o aprobar codigo `String` con constraint) | Delta abierto. No inferir un `SMALLINT` por posicion (`ordinal()`) ni persistir `name()` sin la decision aprobada. |

## 12. Concurrencia

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Fronteras vigentes de exclusion | `guardarSiAusentePorReferencia` (repositorios); `ResultadoPositivoInMemoryMonitor.INSTANCE`, monitor estatico: su garantia alcanza a toda la JVM/classloader donde esa clase esta cargada, no solo a un objeto; `firmezaMonitor` (campo de instancia de `FirmezaCondenaService`), `pagoCondenaMonitor` (campo de instancia de `PagoCondenaService`) y el metodo `synchronized resolver()` de `ResolverPagoObligacionAnteriorService`: su garantia alcanza unicamente a llamadas serializadas sobre esa misma instancia del servicio, no a otras instancias del mismo bean ni a otra JVM | OCC (`version_row`) y/o restricciones `UNIQUE` a nivel de fila; ningun monitor en memoria de Java (de campo, de metodo `synchronized` sobre `this`, o estatico) tiene efecto entre distintas JVM/nodos | Delta critico de escala. Un monitor de campo o un metodo `synchronized` sobre la instancia (`firmezaMonitor`, `pagoCondenaMonitor`, `ResolverPagoObligacionAnteriorService.resolver()`) solo garantiza exclusion dentro de la misma instancia del servicio; no debe describirse como garantia "de toda la JVM". Un monitor estatico (`ResultadoPositivoInMemoryMonitor.INSTANCE`) si alcanza a toda la JVM/classloader, pero ninguno de los tres tipos se extiende a un despliegue multi-nodo. El DDL/JDBC debe reemplazar cada monitor en memoria por la garantia fisica equivalente (OCC vía `versionRow` en `FalActaObligacionPago`, unicidad de `movimientoOrigenId` en `fal_acta_pago_movimiento` para la aplicacion de un pago anterior, o bloqueo a nivel de fila) antes de operar con mas de una instancia de aplicacion contra la misma base. |
| OCC | Version en memoria (`versionRow` en el agregado) verificada antes de cada mutacion; conflicto lanza `ConcurrenciaConflictoException` (ver `CONC-STD-001`) | `version_row` fisico con `UPDATE ... WHERE version_row = :actual`; cero filas afectadas ⇒ `ConcurrenciaConflictoException` | Sin delta de contrato de excepcion; el delta es de mecanismo de deteccion (comparacion en memoria vs. `UPDATE` condicional). |

## 13. Deltas ya resueltos funcionalmente (fuera de alcance de este documento)

Los siguientes temas ya estan cerrados funcionalmente y su detalle vigente vive
en [`mariadb-logical-model.md`](mariadb-logical-model.md) (inventario por agregado) y
no se repite aqui: cobertura de entidades en InMemory (paralización, archivo,
persona/domicilio, valorización, satélites de acta, notificaciones de ciclo
completo, pivot documento-acta, QR/portal), identidades `String` migradas a
`Long`, enums con código numérico explícito, y campos de auditoría
incorporados. La historia de como se llegó a este cierre permanece en Git.

## 14. Entrada al siguiente bloque

Este documento, junto con [`mariadb-logical-model.md`](mariadb-logical-model.md), es la
base de entrada para diseñar el DDL versionado de MariaDB (ver
[`../90-roadmap/current-roadmap.md`](../90-roadmap/current-roadmap.md) y
[`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md)). No
contiene nombres de tabla nuevos ni tipos SQL definitivos más allá de los ya
usados en `mariadb-logical-model.md`.
