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
| Generacion de identificador | Contador `AtomicLong` en memoria por repositorio; 35 puertos todavia exponen `nextId()` | `AUTO_INCREMENT` por tabla, asignado por MariaDB | **Delta resuelto normativamente** (`DECISION_ID-01`, seccion 2.1). Migracion gradual puerto por puerto dentro de su wave JDBC. |
| Tipo de identificador | `Long` en las entidades ya migradas (ver `110`, inventario de identidad por agregado) | `BIGINT` | Sin delta de tipo pendiente; confirmar mapeo 1:1 en el DDL. |
| Identidad natural (claves de negocio) | Validada en memoria antes de insertar (p. ej. `loteCodigo`, `referenciaFirmaExt`, `referenciaExterna`) | Debe reforzarse con restriccion `UNIQUE` fisica | Ver seccion 3. |

### 2.1 `DECISION_ID-01` - Generacion de identificadores (CERRADA)

**Regla global.** Las PK `BIGINT AUTO_INCREMENT` las asigna MariaDB. Ningun
componente productivo preasigna identificadores.

Queda prohibido en produccion:

    MAX(id) + 1
    lectura de information_schema.AUTO_INCREMENT
    AtomicLong como generador productivo
    reservas locales de rango
    UUID improvisado en reemplazo de BIGINT
    reintento de duplicate key como estrategia normal de asignacion

`AtomicLong` puede permanecer unicamente dentro de adaptadores InMemory,
simulando el identificador que asignaria la persistencia.

**Contrato.** La operacion de alta recibe la entidad sin id y devuelve la entidad
persistida con el id asignado. El modelo expone `conId(Long)` (inmutable) o
`setId(Long)` (mutable) segun su naturaleza.

**Familias de patron.** Cada puerto se migra segun la forma de su operacion:

| Patron | Forma | Ejemplo aprobado |
|---|---|---|
| P1 INSERT simple | `guardar(entidad sin id)` devuelve entidad con id | `ActaEventoRepository`, `MotivoArchivoRepository` |
| P2 INSERT-if-absent concurrente | devuelve resultado explicito con la entidad y un booleano de preexistencia | `DocumentoFirmaRepository.guardarSiAusentePorReferencia` -> `DocumentoFirmaSaveResult`; `DiaNoComputableRepository.guardarActivoSiAusentePorFecha` -> `DiaNoComputableSaveResult` |
| P3 UPSERT explicito | `guardar(entidad)`: id nulo inserta, id presente actualiza. Sin `REPLACE` | `MotivoArchivoRepository`, `DiaNoComputableRepository` |
| P4 UPDATE/OCC | `actualizar(entidad con id y version)`. No genera id | puertos con `version_row` |
| P5 Agregado raiz con hijos | INSERT raiz, generated key, INSERT hijos con esa key | pendiente de wave |
| P6 Numeracion funcional previa | PK tecnica `AUTO_INCREMENT` separada del numero de negocio (talonario/secuencia) | pendiente de wave |
| P7 Append-only | INSERT que devuelve la entidad con generated key | `ActaEventoRepository` |
| P8 Catalogo de solo lectura | sin alta productiva: no requiere generacion de id | pendiente de wave |

En P2 la preexistencia se informa **directamente** en el resultado. Queda
prohibido inferirla comparando identificadores preasignados: con `AUTO_INCREMENT`
esa comparacion no es expresable.

**Migracion gradual.** No se elimina `nextId()` de todos los puertos en bloque.
Cada puerto se migra dentro de su propia wave JDBC, en este orden:

    1. cambiar el contrato del puerto
    2. adaptar servicios y callsites directos
    3. adaptar el adaptador InMemory
    4. agregar el adaptador JDBC
    5. probar paridad
    6. eliminar nextId() de ese puerto

**Estado.** Puertos ya sin `nextId()`: `ActaEventoRepository` (WAVE 0B),
`MotivoArchivoRepository` y `DiaNoComputableRepository` (WAVE 1A0-D),
`ActaRepository` (WAVE 1A2-A1), `ObservacionRepository` (WAVE 1A2-B0). Los 35
restantes conservan `nextId()` hasta su wave.

### 2.2 `DECISION_ID-01` aplicada a `ObservacionRepository` (CERRADA)

Aplicada en WAVE 1A2-B0. `fal_observacion` no tiene `version_row` y su fila no se
edita, de modo que el patron es P7 (append-only) y no P3: el alta es la unica
escritura que crea filas y la unica escritura sobre una fila existente es la baja
logica.

**Alta.** La observacion nueva llega con `id` nulo. `guardar` inserta sin informar
la PK, la recupera del motor y devuelve una copia persistida mediante
`FalObservacion.conId(Long)`. El argumento no se muta.

**Id informado.** `guardar` lo rechaza siempre con excepcion funcional, en ambos
adaptadores y con el mismo mensaje. Las dos lecturas posibles son ilegitimas: con
un id inexistente seria preasignar la PK, prohibido por esta decision; con un id
existente seria reescribir una fila append-only, prohibido por `DECISION_OBS-01`.
Esta es la diferencia con P3, donde un id presente significa actualizar.

**Baja logica.** `desactivar(id, fhBaja, idUserBaja)` recibe el actor y el instante
de la capa de aplicacion. El adaptador JDBC no usa `NOW()`, `CURRENT_TIMESTAMP` ni
un usuario por defecto, y el CHECK fisico `chk_obs_baja_coherente` refuerza la
combinacion. Cero filas afectadas **no** es un unico caso: se desambigua con una
lectura por id que solo se paga en el camino de fallo, igual que en
`DECISION_OCC-01/B`, y distingue un id inexistente de una observacion ya inactiva.

**Paridad.** El adaptador InMemory es el oraculo: mismo orden canonico, mismo
filtro de vigencia y mismos tipos y mensajes de excepcion. Su `AtomicLong` es
simulacion interna del `AUTO_INCREMENT` y ningun llamador lo ve. La secuencia no se
promete sin huecos.

**Divergencia residual aceptada.** Con `id` nulo, el adaptador InMemory lanza
`NullPointerException` en `buscarPorId` (`ConcurrentHashMap` no admite claves
nulas) y el JDBC devuelve `Optional.empty()`. Es la misma divergencia ya registrada
para los catalogos de WAVE 1A1: ningun contrato ni callsite ejerce esa forma.

### 2.3 `DECISION_ID-01` aplicada a `PersonaRepository` (CERRADA, sin implementar)

Cerrada normativamente en
`JDBC-DOMINIO-EDA-WAVE-1A2-B1.5A-PERSONA-DOCUMENTO-NORMATIVO-002`. `fal_persona` no
tiene `version_row`, de modo que no hay OCC, pero **su fila si se edita**: el patron
es P3 (alta y actualizacion en la misma operacion) y no P7.

**Alta.** La persona nueva llega con `id` nulo. `guardar` inserta sin informar la PK,
la recupera del motor y devuelve una copia persistida mediante
`FalPersona.conId(Long)`. El argumento no se muta y todo llamador continua con la
copia devuelta.

**Id informado.** `guardar` **actualiza** la fila existente. Es la diferencia con
`ObservacionRepository`: `PersonaService.modificar` y
`PersonaService.actualizarVinculoIngresos` ya usan hoy esa semantica y el contrato
del puerto la conserva de forma explicita, sin `REPLACE` y sin
`ON DUPLICATE KEY UPDATE`. El `UPDATE` lista todas las columnas mutables, de modo que
llevar una columna nullable a NULL se persiste.

**Id informado inexistente.** No se inserta nunca: insertar con una PK elegida por
el llamador esta prohibido por esta decision. La excepcion funcional canonica es
`PersonaNoEncontradaException`, que ya existe en el dominio y ya la produce
`PersonaService.obtener`; ambos adaptadores deben producirla con el mismo tipo y el
mismo mensaje. No se crea ninguna excepcion nueva.

**Upsert incondicional: prohibido como contrato futuro.**
`InMemoryPersonaRepository.guardar` hace hoy `store.put(copia.getId(), copia)`, de
modo que un id inexistente se inserta en silencio. Esa permisividad existe solo
porque `nextId()` entrega un id antes de guardar y contradice esta decision. Al
eliminar `nextId()` desaparece la forma de obtener un id previo, y el adaptador
InMemory debe pasar a distinguir alta de actualizacion como lo hara el adaptador
JDBC.

**Identidad interna del oraculo.** El `AtomicLong` de `InMemoryPersonaRepository` es
simulacion interna del `AUTO_INCREMENT` y ningun llamador lo ve. La secuencia no se
promete sin huecos.

**Transicion de `nextId()`: los tres callsites autorizados.** `nextId()` tiene
exactamente tres callsites productivos y ninguno en tests. Los tres consumen el id
**despues** de persistir, de modo que el orden de sentencias no cambia:

```text
PersonaService.crear
PersonaService.crearMinimal
ActaService.crearPersonaMinimal
```

En los tres, la forma correcta es construir con `new FalPersona(null, ...)`, invocar
`guardar` y continuar con la entidad devuelta. La modificacion de
`ActaService.crearPersonaMinimal` queda **expresamente autorizada** para el slice de
implementacion y es unicamente la transicion de identidad de Persona:
no reabre las reglas de alta del acta, ni el `codigo_qr`, ni el `id_user_alta`, ni
`DECISION_OBS-TX-01`.

**Orden.** El puerto declara `listarTodas`, `buscarPorTipoDocYNroDoc` y
`buscarPorIdSujBie` devolviendo listas, y promete orden por `id` ascendente, que es el
mismo criterio ya aplicado a `ActaRepository.listarTodas`. Antes de esta transicion el
adaptador InMemory recorria `ConcurrentHashMap.values()` sin ordenar y el orden
observable era indefinido. No es una armonizacion silenciosa: se documenta como
correccion de una divergencia preexistente y se prueba contra el oraculo.

**Normalizacion documental: una sola autoridad.** El value object
`DocumentoPersona` es el unico normalizador del documento en Java. Agrupa los cuatro
componentes fisicos, valida sus invariantes en el constructor y expone
`normalizar(TipoDocumentoPersona, String)`. `FalPersona` no tiene setters individuales
de tipo, numero, prefijo ni digito: la unica via de mutacion es
`setDocumento(DocumentoPersona)`, de modo que no puede construirse un par incoherente
en dos pasos. `PersonaService` y `ActaService` no contienen ninguna expresion regular
de documento.

Una busqueda por documento normaliza con la misma funcion que persiste. Cuando el
valor recibido no es canonizable para el tipo indicado, la busqueda devuelve lista
vacia en lugar de fallar: ninguna fila persistida puede coincidir, porque todas
guardan la forma canonica. El alta y la modificacion si rechazan ese mismo valor.

**Estado de implementacion (WAVE 1A2-B1.5B0-A).** La parte Java esta implementada:
`PersonaRepository` sin `nextId()`, `FalPersona` con `id` nulo y `conId(Long)`,
`DocumentoPersona` como normalizador unico, `InMemoryPersonaRepository` con identidad
interna, alta y actualizacion diferenciadas y orden por `id` ascendente, y los tres
callsites migrados.

**Estado de implementacion (WAVE 1A2-B1.5B0-B).** La divergencia que la version
anterior de este parrafo declaraba **quedo cerrada**. El `ALTER` de `fal_persona` se
aplico sobre `sb_faltas_db` el 2026-08-03
(`database/ddl/alter-fal-persona-documento-opcional-001-forward.sql`): el esquema
ejecutado coincide con el modelo objetivo de `DECISION_DDL-PERS-01` y una persona sin
documento ya es persistible. `JdbcPersonaRepository` está activado desde `WAVE
1A2-B2`, verificado por `JdbcPersonaRepositoryIT` contra la base real.

Ya es un bean productivo: el puerto `PersonaRepository` lo resuelve
`JdbcPersonaRepository` bajo el perfil `jdbc` (`@Profile("jdbc")`) y
`InMemoryPersonaRepository` en el resto de los perfiles (`@Profile("!jdbc")`), de forma
mutuamente excluyente. La activacion de `WAVE 1A2-B2` quedó cerrada.

**Divergencia residual del adaptador JDBC.** `fal_persona` no tiene `version_row` y
por lo tanto no hay OCC: dos actualizaciones concurrentes de la misma fila no se
detectan en ninguno de los dos adaptadores. Es la consecuencia de que la decision
fisica no incorpore la columna, no una omision del adaptador.

## 3. Unicidades

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Mecanismo | Operaciones atomicas de repositorio in-memory (`guardarSiAusentePorReferencia`, `guardarActivoSiAusentePorFecha`, verificacion previa en `LoteCorreoRepository`, etc.) sincronizadas dentro de una sola JVM | Restricciones `UNIQUE` a nivel de columna o indice compuesto | InMemory nunca delega la unicidad al motor de persistencia; MariaDB debe hacerlo para sostener la garantia con multiples instancias de aplicacion. |
| Claves de idempotencia conocidas | `referenciaFirmaExt` (firma), `referenciaExterna` + `origenMovimiento` (pagos), `origenMovimiento` + `cmtePG` + `prefPG` + `nroPG` (recibo real de pago), `loteCodigo` (lote de correo), fecha (dia no computable activo), `movimientoOrigenId` en `fal_acta_pago_movimiento` (unicidad de aplicacion de un pago anterior resuelto) | Deben expresarse como restriccion `UNIQUE` (simple o parcial/condicional segun el motor) | `DECISION_DDL-PAGO-MOV-01` CERRADA: unicidad fisica de `movimientoOrigenId` mediante columna generada nullable + `UNIQUE`; `DECISION_DDL-PERS-01`: unicidad de persona por `(tipo_documento, doc_key)`, con `doc_key` `STORED GENERATED` y NULL cuando no hay documento, de modo que las personas identificadas parcialmente sin documento coexisten sin colisionar. El nombre `tipo_doc` esta prohibido como columna fisica y la redaccion anterior `UNIQUE(tipo_doc, nro_doc)` queda obsoleta. No existe `fal_acta_pago_resolucion`. La clave de recibo (`origenMovimiento` + `cmtePG` + `prefPG` + `nroPG`) se verifica de forma **atomica** dentro del mismo bloque `synchronized` de `InMemoryPagoMovimientoRepository.append` que ya protege `id` y `origenMovimiento` + `referenciaExterna`. El motivo de la resolucion de un pago anterior se persiste en el propio movimiento de aplicacion (`fal_acta_pago_movimiento.motivo_aplicacion_pago_anterior`, columna nullable), no en el texto del evento `PAGRES`. |

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
| `NO_EXPLICIT_CODE` | El enum no expone `codigo()` — **0 enums en esta categoría** tras el cierre de `DECISION_DDL-ENUM-01` | N/A |

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Enums `EXPLICIT_NUMERIC_CODE`/`EXPLICIT_STRING_CODE` | Codigo explicito verificado en el enum Java (ver `../00-governance/glossary.md` y [`mariadb-logical-model.md`](mariadb-logical-model.md)) | Columna `SMALLINT` (o `CHAR`/`VARCHAR` para codigo `String`) con el codigo explicito del enum Java | Sin delta pendiente. `ResultadoFinalActa` tiene 10 codigos vigentes (0-9); el codigo 5 (`FALLO_CONDENATORIO_PAGADO`) es LEGACY_RESERVED (`DECISION_DDL-RF-005` CERRADA). |
| Enums promovidos por `DECISION_DDL-ENUM-01` (`EstadoFalloActa`, `EstadoApelacionActa`, `EstadoPagoCondena`, `TipoDiaNoComputable`, `OrigenDiaNoComputable`) | `codigo()` numerico expuesto; verificado en guardrail `SpecAsSourceGuardrailTest.enums_decision_ddl_enum01_tienen_codigo_numerico` | `SMALLINT NOT NULL` con el codigo explicito | Delta resuelto: `DECISION_DDL-ENUM-01` CERRADA. |

## 12. Concurrencia

| Aspecto | InMemory vigente | MariaDB objetivo | Delta |
|---|---|---|---|
| Fronteras vigentes de exclusion | `guardarSiAusentePorReferencia` (repositorios); `ResultadoPositivoInMemoryMonitor.INSTANCE`, monitor estatico: su garantia alcanza a toda la JVM/classloader donde esa clase esta cargada, no solo a un objeto; `firmezaMonitor` (campo de instancia de `FirmezaCondenaService`), `pagoCondenaMonitor` (campo de instancia de `PagoCondenaService`) y el metodo `synchronized resolver()` de `ResolverPagoObligacionAnteriorService`: su garantia alcanza unicamente a llamadas serializadas sobre esa misma instancia del servicio, no a otras instancias del mismo bean ni a otra JVM | OCC (`version_row`) y/o restricciones `UNIQUE` a nivel de fila; ningun monitor en memoria de Java (de campo, de metodo `synchronized` sobre `this`, o estatico) tiene efecto entre distintas JVM/nodos | Delta critico de escala. Un monitor de campo o un metodo `synchronized` sobre la instancia (`firmezaMonitor`, `pagoCondenaMonitor`, `ResolverPagoObligacionAnteriorService.resolver()`) solo garantiza exclusion dentro de la misma instancia del servicio; no debe describirse como garantia "de toda la JVM". Un monitor estatico (`ResultadoPositivoInMemoryMonitor.INSTANCE`) si alcanza a toda la JVM/classloader, pero ninguno de los tres tipos se extiende a un despliegue multi-nodo. El DDL/JDBC debe reemplazar cada monitor en memoria por la garantia fisica equivalente (OCC vía `versionRow` en `FalActaObligacionPago`, unicidad de `movimientoOrigenId` en `fal_acta_pago_movimiento` para la aplicacion de un pago anterior, o bloqueo a nivel de fila) antes de operar con mas de una instancia de aplicacion contra la misma base. |
| OCC | Version en memoria (`versionRow` en el agregado) verificada antes de cada mutacion; conflicto lanza `ConcurrenciaConflictoException` (ver `CONC-STD-001`); implementado en todos los repositorios con OCC: `InMemoryActaRepository`, `InMemoryDocumentoRepository`, `InMemoryNotificacionRepository`, `InMemoryGestionExternaRepository`, `InMemoryBloqueanteMaterialRepository`, `InMemoryActaObligacionPagoRepository`, `InMemoryActaFormaPagoRepository`, `InMemoryActaMovimientoRepository`, `InMemoryEconomiaProyeccionRepository`, `InMemoryActaSnapshotRepository` (regenera en lugar de OCC directo) | `version_row` fisico con `UPDATE ... SET version_row = version_row + 1 WHERE id = :id AND version_row = :versionEsperada`; una fila afectada es exito; cero filas NO es un unico caso (ver `DECISION_OCC-01/B`) | Sin delta de contrato de excepcion; el delta es de mecanismo de deteccion (comparacion en memoria vs. `UPDATE` condicional) y de desambiguacion del resultado cero (`DECISION_OCC-01/B`). |

### 12.1 `DECISION_OCC-01` - Alta y version inicial bajo OCC (CERRADA)

Aplica a los agregados con `version_row`. Ratificada en WAVE 1A2-A1 y aplicada a
`ActaRepository`.

**Alta.** La entidad nueva llega con `id` nulo y `versionRow` 0. La PK la asigna la
persistencia (`DECISION_ID-01`); la version inicial se conserva en 0.

**Resultado.** `guardar` devuelve una copia persistida con el id y la version
efectivos, y no muta la instancia recibida. Todo llamador debe seguir trabajando
con la entidad devuelta.

**Incremento.** Una actualizacion exitosa avanza la version exactamente uno.

**Huecos.** La version OCC no tiene huecos, pero la PK `AUTO_INCREMENT` si puede
tenerlos: un INSERT rechazado consume un valor sin dejar fila. No debe afirmarse
una secuencia de PK sin huecos como contrato.

Nota de alcance: la semantica de instancia queda registrada como excepcion de
diseno. `InMemoryActaRepository` devuelve copia; `InMemoryDocumentoRepository`,
`InMemoryNotificacionRepository`, `InMemoryGestionExternaRepository` y
`InMemoryBloqueanteMaterialRepository` todavia mutan la entidad recibida. Esta
decision no unifica esos cuatro puertos.

### 12.2 `DECISION_OCC-01/B` - Semantica de cero filas afectadas (CERRADA)

La formula normativa de la actualizacion es:

    UPDATE <tabla>
       SET ...,
           version_row = version_row + 1
     WHERE id = :id
       AND version_row = :versionEsperada

Resultado:

    1 fila  -> exito; devuelve la copia persistida con version_row + 1

    0 filas -> NO significa siempre conflicto. Debe desambiguarse por id:

               id inexistente                    -> excepcion funcional de no
                                                    encontrado (para el acta,
                                                    ActaNoEncontradaException),
                                                    HTTP 404, codigo
                                                    ACTA_NO_ENCONTRADA

               id existente con version distinta -> ConcurrenciaConflictoException,
                                                    HTTP 409, codigo
                                                    CONFLICTO_CONCURRENCIA

Un 404 no debe degradarse a 409, y un 409 no debe usarse como "no encontrado":
decirle al cliente que reintente un id que no existe le miente. Esto alinea la
seccion 12 con `D-1A1-01`, que ya exigia una excepcion funcional de no encontrado
para un id inexistente.

En JDBC la desambiguacion requiere una lectura por id **solo en el camino de
fallo**. El camino de exito no paga esa consulta.

Un id presente que no existe nunca se inserta: insertar con una PK elegida por el
llamador esta prohibido por `DECISION_ID-01`.
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
base de entrada para construir el script DDL manual de MariaDB (ver
[`../90-roadmap/current-roadmap.md`](../90-roadmap/current-roadmap.md) y
[`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md)). No
contiene nombres de tabla nuevos ni tipos SQL definitivos más allá de los ya
usados en `mariadb-logical-model.md`.
