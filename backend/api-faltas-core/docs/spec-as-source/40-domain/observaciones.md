# Observaciones — Spec transversal

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING

> **Slice:** SPEC-MODEL-DDL-CLOSURE-001 / DDL-MARIADB-MANUAL-001-FULL-R1.2
> No contiene "pendiente", "por definir" ni tokens de trabajo.

---

## 1. Regla principal

Toda nota libre, humana, acumulable y auditable se persiste en `fal_observacion`.

Las entidades observables **no** contienen un único campo `observacion` que se
sobrescribe. Puede haber N observaciones por cada registro observable.

No confundir con campos estructurales que permanecen en su entidad:
- fundamentos
- resumen del hecho
- motivo tipificado
- resultado técnico
- contenido jurídico
- detalle de acuse
- contenido documental

---

## 2. Tabla `fal_observacion` — definición física

### 2.1 Propósito

Registro polimórfico append-only de notas libres vinculadas a cualquier entidad
observable del dominio. La corrección de una observación se realiza mediante
baja lógica de la anterior e inserción de una nueva. **No se edita**.

### 2.2 Columnas

| Columna              | Tipo                    | Nulable | Default | Descripción |
|----------------------|-------------------------|---------|---------|-------------|
| `id`                 | `BIGINT AUTO_INCREMENT` | NO      | —       | PK técnica |
| `entidad_tipo`       | `SMALLINT`              | NO      | —       | Código de `EntidadTipoObservada`; identifica qué tipo de objeto se observa |
| `entidad_id`         | `BIGINT`                | NO      | —       | PK del objeto observado (polimórfico) |
| `id_acta_contexto`   | `BIGINT`                | SÍ      | NULL    | FK a `fal_acta(id)`. Contexto del expediente cuando la entidad es un satélite del acta. NULL para entidades maestro |
| `origen_observacion` | `SMALLINT`              | NO      | —       | Código de `OrigenObservacion`: 1=USUARIO, 2=SISTEMA, 3=INTEGRACION |
| `observacion`        | `VARCHAR(512)`          | NO      | —       | Texto de la nota. CHAR_LENGTH(TRIM(observacion)) BETWEEN 1 AND 512 |
| `si_activa`          | `BOOLEAN`               | NO      | `TRUE`  | TRUE = vigente; FALSE = dada de baja lógicamente |
| `fh_alta`            | `DATETIME(6)`           | NO      | —       | Instante de creación |
| `id_user_alta`       | `CHAR(36)`              | NO      | —       | UUID del actor que creó la observación |
| `fh_baja`            | `DATETIME(6)`           | SÍ      | NULL    | Instante de baja lógica. NULL si si_activa=TRUE |
| `id_user_baja`       | `CHAR(36)`              | SÍ      | NULL    | Actor que realizó la baja. NULL si si_activa=TRUE |

### 2.3 Constraints

```sql
PRIMARY KEY (id),

CONSTRAINT chk_obs_texto
  CHECK (CHAR_LENGTH(TRIM(observacion)) BETWEEN 1 AND 512),

CONSTRAINT chk_obs_baja_coherente
  CHECK (
    (si_activa = TRUE  AND fh_baja IS NULL  AND id_user_baja IS NULL) OR
    (si_activa = FALSE AND fh_baja IS NOT NULL AND id_user_baja IS NOT NULL)
  ),

INDEX ix_obs_entidad        (entidad_tipo, entidad_id, si_activa, fh_alta),
INDEX ix_obs_acta_contexto  (id_acta_contexto, fh_alta),

CONSTRAINT fk_obs_acta_contexto FOREIGN KEY (id_acta_contexto)
  REFERENCES fal_acta (id) ON DELETE CASCADE ON UPDATE RESTRICT
```

Los nombres de los dos índices y de la clave ajena son los canónicos de
`DECISION_OBS-NAMES-01` (sección 2.7) y coinciden exactamente con los del esquema
ejecutado en `sb_faltas_db` y con `database/ddl/create-bod-faltas-domain.sql`.

### 2.4 Notas de diseño

- **No tiene** `version_row`: la observación no se edita; OCC no aplica.
- `id_acta_contexto → fal_acta(id) ON DELETE CASCADE`: al eliminar físicamente un
  acta (operación no ordinaria), todas sus observaciones contextuales se limpian
  automáticamente. Las observaciones de entidades maestro no tienen este contexto
  y permanecen.
- El patrón de corrección: baja lógica (`si_activa=FALSE`, `fh_baja`, `id_user_baja`)
  + inserción de una nueva observación.

### 2.5 `DECISION_OBS-01` — vínculo propietario polimórfico y ciclo de vida (CERRADA)

`fal_observacion` es una tabla central polimórfica. Una FK no puede apuntar
selectivamente a distintas tablas padre según `entidad_tipo`, de modo que la
integridad propietaria **no** se implementa con FK ni con `ON DELETE CASCADE`.

**Vínculo propietario.** El propietario canónico de cada observación se identifica
por el par `(entidad_tipo, entidad_id)`. `entidad_id` es el nombre físico de la
columna genérica de identidad de entidad; no existe FK física sobre ella.

Para una observación cuyo propietario es el acta:

```text
entidad_tipo      = ACTA (1)
entidad_id        = fal_acta.id
id_acta_contexto  = NULL
```

**`id_acta_contexto` no identifica al propietario.** Es únicamente un contexto
opcional para observaciones cuyo propietario real es otra entidad vinculada a un
acta:

```text
entidad_tipo      = DOCUMENTO (4)
entidad_id        = fal_documento.id
id_acta_contexto  = fal_acta.id
```

**Cardinalidad.** `fal_acta` 1 a 0..N `fal_observacion`. No existe una observación
única mutable por acta y no se define ninguna restricción `UNIQUE` que impida
múltiples observaciones.

**`FalActa.observaciones`.** Es la observación inicial capturada durante el
labrado. No se mapea a `fal_acta.resumen_hecho`: son conceptos distintos y no
comparten columna ni fuente de verdad.

```text
texto no blanco  tras insertar fal_acta y obtener la PK, se inserta una fila en
                 fal_observacion con el vinculo propietario de ACTA
nulo o blanco    no se crea fila
```

Las observaciones siguientes son filas nuevas append-only administradas por
`ObservacionRepository`. No sobrescriben ni concatenan la observación inicial.

**Lectura.** `FalActa.observaciones` se reconstruye desde `fal_observacion`, nunca
desde `resumen_hecho`. No existen dos fuentes de verdad.

**Puerto.** `ObservacionRepository` es el puerto canónico de `fal_observacion`.
Ningún adaptador de otra tabla duplica su SQL. La operación conjunta la coordina
la frontera transaccional del caso de uso o servicio de aplicación, que coordina
puertos y no adaptadores concretos.

**Alta atómica.**

```text
BEGIN
  insertar fal_acta
  obtener el id generado
  insertar la observacion inicial por ObservacionRepository si hay texto
  persistir los demas elementos del caso de uso
COMMIT
```

Ante cualquier fallo, rollback completo. No puede quedar un acta sin la
observación inicial informada, ni una observación sin acta.

**Ciclo de vida.** La operación normal sobre un acta no es un borrado físico:
anulación, archivo y baja jurídica conservan las observaciones.

**Purga física**, excepcional y controlada:

```text
BEGIN
  eliminar las observaciones propietarias
    (entidad_tipo = ACTA, entidad_id = :idActa)
  tratar segun politica las observaciones de otras entidades
    con id_acta_contexto = :idActa
  eliminar fal_acta
COMMIT
```

**Triggers.** La eliminación transaccional explícita es la forma normativa: es
visible, testeable y auditable, y distingue baja lógica de purga física. Los
triggers `AFTER DELETE` de la sección 5 dejan de ser comportamiento canónico:
introducen efectos ocultos, uno por cada tabla observable, pérdida del contexto de
actor y dificultades de auditoría, versionado, Outbox y restauración.

**Mitigación del borrado directo fuera de la aplicación:** permisos que impidan
`DELETE` directo a usuarios ordinarios, un único caso de uso o servicio de purga,
tests de rollback e integridad, un guardrail de detección de observaciones
huérfanas y procedimientos operativos controlados.

**Índice.** El acceso por propietario se sirve con `ix_obs_entidad`
`(entidad_tipo, entidad_id, si_activa, fh_alta)`, cuyo prefijo izquierdo es
exactamente `(entidad_tipo, entidad_id)`. No se requiere un índice adicional.

**Remediación física registrada.** El esquema ejecutado conserva dos elementos que
`DECISION_OBS-01` deja de considerar canónicos y cuya baja requiere DDL, que se
ejecuta con autorización expresa y evidencia propia:

```text
fk_obs_acta_contexto ... ON DELETE CASCADE sobre id_acta_contexto
21 triggers *_ad_observaciones
```

### 2.6 Aclaraciones derivadas de la implementacion del puerto (WAVE 1A2-B0)

Subordinadas a `DECISION_OBS-01`. No agregan reglas nuevas: fijan como se expresan
las ya cerradas en el contrato del puerto y verifican su comportamiento material
contra `sb_faltas_db`.

**Orden canonico de lectura por propietario.**

```text
fh_alta ASC, id ASC
```

El id desempata instantes `DATETIME(6)` identicos, de modo que el orden historico es
determinista y la primera fila es siempre la observacion mas antigua del
propietario. Cuando `entidad_tipo = ACTA`, esa primera fila es la observacion
inicial capturada durante el labrado. El filtro se sirve con el prefijo izquierdo
`(entidad_tipo, entidad_id)` de `ix_obs_entidad`. No existe listado descendente ni
endpoint HTTP de observaciones: este orden no invierte ninguna vista previa.

**El alta es la unica escritura que crea filas.** `ObservacionRepository.guardar`
recibe la observacion con `id` nulo y devuelve la copia persistida con la PK que
asigno la persistencia. Una observacion con `id` informado se rechaza siempre: es la
forma en que el puerto hace cumplir el caracter append-only. La correccion sigue
siendo baja logica de la fila anterior mas una fila nueva.

**La baja logica exige actor e instante.** `desactivar(id, fhBaja, idUserBaja)` los
recibe de la capa de aplicacion. La persistencia no los genera: no se usa `NOW()`,
`CURRENT_TIMESTAMP` ni un usuario por defecto. La operacion no reescribe
`observacion`, `entidad_tipo`, `entidad_id`, `origen_observacion`, `fh_alta` ni
`id_user_alta`.

**Alcance real de `chk_obs_texto`.** La rama superior del `BETWEEN 1 AND 512` es
estructuralmente inalcanzable: como la columna es `VARCHAR(512)`,
`CHAR_LENGTH(TRIM(observacion))` nunca supera 512. Un texto mas largo lo rechaza el
tipo de la columna (error 1406, `SQLSTATE 22001`) antes de que el CHECK se evalue. El
CHECK defiende efectivamente el limite inferior: un texto en blanco o compuesto solo
por espacios. La consecuencia practica es que no toda violacion de integridad de
esta tabla se manifiesta de la misma forma, y la validacion de longitud del dominio
sigue siendo la primera linea de defensa.

**`origen_observacion` es `NOT NULL` en la base y no lo valida el modelo Java.** Un
origen nulo llega al motor y produce una violacion de integridad; el adaptador no
inventa un valor por defecto. Queda registrado como gap de validacion de dominio,
no como delta fisico.

### 2.7 `DECISION_OBS-NAMES-01` — nombres físicos canónicos y aliases prohibidos (CERRADA)

Los nombres canónicos de los dos índices y de la clave ajena de `fal_observacion`
son exactamente los del esquema ejecutado:

```text
ix_obs_entidad
ix_obs_acta_contexto
fk_obs_acta_contexto
```

Verificados contra `information_schema.STATISTICS` e
`information_schema.REFERENTIAL_CONSTRAINTS` de `sb_faltas_db`, y coincidentes con
`database/ddl/create-bod-faltas-domain.sql`.

Las variantes `idx_obs_entidad`, `idx_obs_ctx_acta` y `fk_obs_acta_ctx` son nombres
obsoletos: no existen en el motor, no existen en el DDL fuente y quedan eliminados
de esta spec. Prohibido reintroducirlos como nombres vigentes.

No se renombra ningún objeto físico: la divergencia era exclusivamente documental y
se resuelve alineando el documento con el motor, nunca al revés.

### 2.8 `DECISION_OBS-TX-01` — frontera transaccional del alta del acta (CERRADA)

Cierra la pregunta que `DECISION_OBS-01` dejaba abierta al describir el alta atómica
sin nombrar quién la demarca.

**Frontera.** `ActaService.labrar` es la frontera transaccional del alta completa del
acta, incluida su observación inicial.

```text
@Transactional
ActaService.labrar(LabrarActaCommand):

validaciones previas a toda mutacion

luego, en una unica transaccion y en este orden:
1. resolver o crear fal_persona cuando la situacion lo exige
2. generar id_tecnico y QR0 sin efectos persistentes externos
3. guardar fal_acta y obtener la PK
4. insertar la observacion inicial si FalActa.observaciones tiene texto no blanco
5. persistir las evidencias
6. persistir el evento ACTLAB
7. persistir el snapshot
8. commit total

cualquier fallo -> rollback total de todas las mutaciones del caso de uso
```

**El orden es único y determinista.** No es una preferencia de estilo: lo imponen las
claves ajenas reales. Evidencia, observación, evento y snapshot referencian
`fal_acta.id`, y el acta referencia `fal_persona.id`, de modo que ninguna otra secuencia
es posible sin violar integridad. Ese mismo orden se aplica en todos los caminos
equivalentes, y es lo que evita el interbloqueo por orden inverso de bloqueos entre dos
altas concurrentes.

**Fundamento.** Es el único punto donde el caso de uso completo se expresa una sola
vez y ya coordina los puertos participantes. El controller no es frontera de
transacción. No se crea un coordinador vacío sólo para alojar la anotación. Los
adaptadores no abren transacciones propias.

**La transacción coordina puertos, no adaptadores.** Participan `ActaRepository`,
`ObservacionRepository` y los demás puertos que `labrar` ya utiliza. Ni el dominio ni
el servicio dependen de clases JDBC concretas, de modo que el comportamiento en modo
InMemory se conserva.

**Estado de implementación.** **Implementada y verificada.** `ActaService.labrar` declara
`@Transactional` sobre el método público, con rollback total demostrado por etapa. Las
cuatro condiciones que la bloqueaban están cumplidas:

```text
JdbcActaRepository            bean bajo perfil jdbc; unica implementacion del puerto
id_persona_infractor          SituacionIdentificacionInfractor y su matriz de
                              coherencia, en 10-domain/states-events-catalogs.md;
                              columna fisica y CHECK en DECISION_DDL-ACTA-INFR-01
codigo_qr                     formato canonico y algoritmo en DECISION_DDL-ACTA-QR-01
id_user_alta                  subject autenticado, en 20-application/command-contracts.md
```

**Todos los participantes mutantes escriben en el mismo recurso.** La promesa de rollback
total sólo es verdadera si ninguna escritura del caso de uso ocurre fuera de la
transacción. Los seis puertos que `labrar` muta se resuelven, bajo perfil `jdbc`, por un
adaptador JDBC sobre el único `DataSource` administrado por Spring, y por lo tanto
participan del único `JdbcTransactionManager`:

```text
PersonaRepository        JdbcPersonaRepository          fal_persona
ActaRepository           JdbcActaRepository             fal_acta
ObservacionRepository    JdbcObservacionRepository      fal_observacion
ActaEvidenciaRepository  JdbcActaEvidenciaRepository    fal_acta_evidencia
ActaEventoRepository     JdbcActaEventoRepository       fal_acta_evento
ActaSnapshotRepository   JdbcActaSnapshotRepository     fal_acta_snapshot
```

Los equivalentes InMemory de esos seis puertos quedan restringidos a `!jdbc`. Dejar
alguno activo bajo `jdbc` habría sido peor que no tener transacción: su escritura
sobreviviría al rollback y quedaría apuntando a un acta inexistente.

**Efectos no reversibles: ninguno.** La emisión del QR es una función determinista sobre
el `id_tecnico`, calculada en memoria; no escribe, no publica y no llama a nada externo,
de modo que un rollback la deja sin rastro. Dentro de la frontera no hay publicación a
broker, llamada HTTP, escritura de archivo ni storage externo. No se implementan
compensaciones manuales, sincronizaciones de transacción ni callbacks posteriores al
commit para simular atomicidad donde el motor ya la provee.

**Sin reintentos.** Una violación de unicidad o un interbloqueo se propagan; no se
reintenta el alta, no se regenera el UUID y no se captura ninguna excepción para devolver
éxito (`DECISION-ACTA-UNIQUE-01`).

**Modo InMemory.** Sin el perfil `jdbc` no hay `DataSource` ni transaction manager, de
modo que la anotación queda sin efecto y el alta se comporta como antes: ordenada, con un
único instante, pero sin promesa de rollback conjunto. Es una consecuencia declarada del
modo de prueba, y por eso no se agrega un transaction manager ficticio que simule ACID en
memoria.

Una `fal_persona` sin documento representa a una persona presente sin identificación
completa, y por sí sola no representa AUSENTE, SE_NIEGA, NO_APORTA_DATOS ni
DATOS_INSUFICIENTES: esos cuatro casos se expresan con `id_persona_infractor` en NULL
más la situación correspondiente. No se crea persona centinela.

### 2.9 `DECISION-OBS-BATCH-01` — lectura de varios propietarios en una sola consulta (CERRADA)

Subordinada a `DECISION_OBS-01`. No crea reglas de dominio: fija cómo el puerto expresa
la lectura de varios propietarios sin cambiar el orden canónico, el vínculo propietario
ni el carácter append-only ya cerrados.

**Problema que resuelve.** Un adaptador que necesita reconstruir la observación de varias
entidades a la vez sólo disponía de la lectura por propietario individual, de modo que
componer una lista de N entidades costaba una consulta de la entidad más N consultas de
`fal_observacion`. La cardinalidad crece con la cantidad de filas leídas y no con la
cantidad de conceptos consultados.

**Operación del puerto.**

```java
Map<Long, List<FalObservacion>> listarPorEntidades(
        EntidadTipoObservada tipo,
        Collection<Long> entidadIds);
```

**Contrato.**

```text
1.  tipo es obligatorio; nulo se rechaza
2.  la coleccion de ids es obligatoria; nula se rechaza
3.  ningun id de la coleccion puede ser nulo; un id nulo se rechaza
4.  una coleccion vacia devuelve un mapa vacio
5.  una coleccion vacia no ejecuta consulta alguna contra el motor
6.  ids repetidos en la entrada no duplican filas ni grupos en la salida
7.  solo se devuelven observaciones cuyo propietario coincida con el tipo pedido
    y con uno de los ids pedidos
8.  cada historial conserva el orden canonico fh_alta ASC, id ASC
9.  se incluyen activas e inactivas
10. un propietario sin filas puede estar ausente del mapa y el consumidor lo
    interpreta como historial vacio
```

**Por qué se incluyen las inactivas.** Es la misma razón por la que la lectura individual
del historial las incluye: cuando `entidad_tipo = ACTA`, la primera fila por el orden
canónico es la observación capturada durante el labrado, y sigue siendo la inicial aunque
después se haya dado de baja y corregido con una fila nueva. Filtrar por `si_activa`
convertiría la observación inicial en «la primera activa», que es otro concepto.

**Por qué la ausencia representa el historial vacío.** Sintetizar una clave con lista
vacía por cada id pedido obligaría al adaptador a inventar entradas que el motor no
devolvió, y dejaría dos representaciones del mismo hecho. La ausencia es la
representación única y el contrato la declara, de modo que no es un detalle de
implementación filtrado al consumidor.

**Forma de la consulta física.** Una sola sentencia con las once columnas explícitas,
parámetros nombrados y expansión de colección para el `IN`. Quedan prohibidos el
`SELECT *`, la concatenación de identificadores en el texto de la sentencia, el SQL
armado por condiciones y cualquier consulta ejecutada dentro del recorrido del resultado.

```sql
SELECT <11 columnas explícitas>
FROM fal_observacion
WHERE entidad_tipo = :entidadTipo
  AND entidad_id IN (:entidadIds)
ORDER BY entidad_id ASC, fh_alta ASC, id ASC
```

**Índice que la sirve.** El prefijo izquierdo `(entidad_tipo, entidad_id)` de
`ix_obs_entidad` sirve la igualdad del tipo y la pertenencia del identificador. **No se
crea ningún índice nuevo y no se ejecuta DDL por esta decisión.** El `ORDER BY` sobre el
conjunto ya acotado es una cuestión de plan de ejecución, no de corrección.

**Las lecturas individuales no se tocan.** `listarPorEntidad` y
`listarActivasPorEntidad` conservan su contrato y su semántica. La operación batch se
agrega, no las reemplaza: una lectura de un solo propietario no gana nada resolviéndose
con un mapa intermedio.

**Consecuencia Java.** `ObservacionRepository` pasa de cinco a seis operaciones. Las dos
implementaciones —el adaptador InMemory y el adaptador JDBC— la implementan, y el
adaptador InMemory devuelve copias defensivas como en el resto de sus lecturas. El
adaptador JDBC de `fal_acta` reconstruye la observación inicial de todas las actas
listadas con una sola invocación de esta operación, y deja de pedir el historial de cada
acta por separado. La composición sigue ocurriendo después de agotar el resultado, nunca
dentro del mapeo de filas. El orden observable de la lista de actas no cambia: se
preserva el de la consulta de actas y no el de las claves del mapa.

**Consecuencia DDL.** Ninguna. `fal_observacion` conserva sus once columnas, sus dos
`CHECK`, sus dos índices y su clave ajena. La remediación física que `DECISION_OBS-01`
dejó registrada —la baja de `fk_obs_acta_contexto ... ON DELETE CASCADE`, la de los 21
triggers `*_ad_observaciones` y la corrección del `COMMENT` de `origen_observacion`—
sigue pendiente y esta decisión no la adelanta ni la modifica.

---

## 3. Enum `EntidadTipoObservada`

Enum cerrado. Codificado como `SMALLINT` en `fal_observacion.entidad_tipo`.
No guardar nombres de tabla como strings.

### 3.1 Tabla de valores (22 códigos canónicos)

| Código | Nombre Java                   | Tabla física                          | PK columna       | Puede tener `id_acta_contexto` | Trigger defensivo |
|--------|-------------------------------|---------------------------------------|------------------|-------------------------------|-------------------|
| 1      | `ACTA`                        | `fal_acta`                            | `id`             | NO — el propietario es `(ACTA, entidad_id)` y `id_acta_contexto` queda NULL | NO — la limpieza es transaccional explícita (`DECISION_OBS-01`) |
| 2      | `PERSONA`                     | `fal_persona`                         | `id`             | NO                            | SÍ — trigger `trg_fal_persona_ad_observaciones` |
| 3      | `DOMICILIO`                   | `fal_persona_domicilio`               | `id`             | SÍ                            | SÍ — trigger `trg_fal_persona_domicilio_ad_observaciones` |
| 4      | `DOCUMENTO`                   | `fal_documento`                       | `id`             | SÍ                            | SÍ — trigger `trg_fal_documento_ad_observaciones` |
| 5      | `EVIDENCIA`                   | `fal_acta_evidencia`                  | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_evidencia_ad_observaciones` |
| 6      | `NOTIFICACION`                | `fal_notificacion`                    | `id`             | SÍ                            | SÍ — trigger `trg_fal_notificacion_ad_observaciones` |
| 7      | `NOTIFICACION_INTENTO`        | `fal_notificacion_intento`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_notificacion_intento_ad_observaciones` |
| 8      | `FALLO`                       | `fal_acta_fallo`                      | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_fallo_ad_observaciones` |
| 9      | `APELACION`                   | `fal_acta_apelacion`                  | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_apelacion_ad_observaciones` |
| 10     | `GESTION_EXTERNA`             | `fal_acta_gestion_externa`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_gestion_externa_ad_observaciones` |
| 11     | `PARALIZACION`                | `fal_acta_paralizacion`               | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_paralizacion_ad_observaciones` |
| 12     | `ARCHIVO`                     | `fal_acta_archivo`                    | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_archivo_ad_observaciones` |
| 13     | `MEDIDA_PREVENTIVA`           | `fal_acta_medida_preventiva`          | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_medida_preventiva_ad_observaciones` |
| 14     | `BLOQUEANTE_CIERRE_MATERIAL`  | `fal_acta_bloqueante_cierre_material` | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_bloqueante_cierre_material_ad_observaciones` |
| 15     | `ARTICULO_INFRINGIDO`         | `fal_acta_articulo_infringido`        | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_articulo_infringido_ad_observaciones` |
| 16     | `VALORIZACION`                | `fal_acta_valorizacion`               | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_valorizacion_ad_observaciones` |
| 17     | `OBLIGACION_PAGO`             | `fal_acta_obligacion_pago`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_obligacion_pago_ad_observaciones` |
| 18     | `FORMA_PAGO`                  | `fal_acta_forma_pago`                 | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_forma_pago_ad_observaciones` |
| 19     | `PLAN_PAGO`                   | `fal_acta_plan_pago_ref`              | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_plan_pago_ref_ad_observaciones` |
| 20     | `MOVIMIENTO_PAGO`             | `fal_acta_pago_movimiento`            | `id`             | SÍ                            | SÍ — trigger `trg_fal_acta_pago_movimiento_ad_observaciones` |
| 21     | `TALONARIO`                   | `num_talonario`                       | `id`             | NO                            | SÍ — trigger `trg_num_talonario_ad_observaciones` |
| 22     | `MOVIMIENTO_TALONARIO`        | `num_talonario_movimiento`            | `id`             | NO                            | SÍ — trigger `trg_num_talonario_movimiento_ad_observaciones` |

### 3.2 Reglas del enum

- Valores consecutivos 1–22. No renumerar sin actualizar Java y tests.
- No inventar nuevos valores en DDL ni Java sin actualizar esta tabla y el enum.
- No guardar nombres de tabla como strings en `entidad_tipo`.
- La clase Java es `ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada`.

---

## 4. Enum `OrigenObservacion`

Enum cerrado. Codificado como `SMALLINT` en `fal_observacion.origen_observacion`.

| Código | Nombre Java    | Significado |
|--------|----------------|-------------|
| 1      | `USUARIO`      | Nota ingresada directamente por un operador humano |
| 2      | `SISTEMA`      | Nota generada automáticamente por un proceso del sistema |
| 3      | `INTEGRACION`  | Nota proveniente de un sistema externo integrado |

La clase Java es `ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion`.

### 4.1 `DECISION_OBS-ENUM-01` — nombres semánticos canónicos (CERRADA)

Los nombres canónicos del dominio son exactamente los tres de la tabla anterior, con
esos códigos:

```text
1 = USUARIO
2 = SISTEMA
3 = INTEGRACION
```

El `COMMENT` de la columna `origen_observacion` en el esquema ejecutado y en
`database/ddl/create-bod-faltas-domain.sql` describe los mismos tres códigos con
etiquetas distintas:

```text
1 = OPERADOR
2 = SISTEMA
3 = EXTERNO
```

`OPERADOR` y `EXTERNO` son etiquetas obsoletas de ese comentario, no valores del
dominio. Los códigos coinciden, de modo que **no hay divergencia de datos ni de
mapeo**: la fila persistida con `1` es `USUARIO` lea quien lea el comentario.

Consecuencias:

```text
el enum Java no cambia
los codigos no cambian
el mapeo JDBC no cambia
no se ejecuta ALTER TABLE por esta causa
```

La corrección del texto del `COMMENT` es una tarea de DDL y está registrada como
`DECISION_DDL-OBS-COMMENT-01` en
[`../50-persistence/ddl-decisions.md`](../50-persistence/ddl-decisions.md), para
resolverse junto con la remediación física de los triggers y del `ON DELETE CASCADE`
descrita en la sección 2.5. Este documento no la duplica.

---

## 5. Triggers defensivos AFTER DELETE

> Subordinada a `DECISION_OBS-01` (sección 2.5): los triggers dejan de ser el
> mecanismo canónico de limpieza. La forma normativa es la eliminación
> transaccional explícita. Esta sección describe los 21 triggers que el esquema
> ejecutado conserva y cuya baja quedó registrada como remediación física.

### 5.1 Propósito

La eliminación física no es operación normal de producción. Las bajas lógicas
conservan observaciones. Los triggers son una red de seguridad para limpiar
observaciones polimórficas cuando se borre físicamente la entidad padre.

### 5.2 Convención de nombres

```
trg_<tabla>_ad_observaciones
```

### 5.3 Patrón único

```sql
CREATE TRIGGER trg_<tabla>_ad_observaciones
AFTER DELETE ON <tabla>
FOR EACH ROW
DELETE FROM fal_observacion
WHERE entidad_tipo = <CODIGO_FIJO>
  AND entidad_id = OLD.<PK>;
```

### 5.4 Reglas

- Un trigger por tabla física observable aplicable.
- Código de entidad fijo (literal numérico en el cuerpo del trigger).
- Sin SQL dinámico.
- Sin procedimientos almacenados.
- Sin condicionales complejos.
- Sin auditoría adicional.
- Sin actualización de otras entidades.
- Sin lógica de negocio.

Para entidades bajo acta:
- El trigger permite borrar individualmente el hijo y limpiar sus observaciones.
- `ON DELETE CASCADE` de `id_acta_contexto` alcanza únicamente a las observaciones
  **contextuales**, es decir aquellas que tienen `id_acta_contexto` informado.

No crear trigger para `ACTA` (código 1). El CASCADE tampoco alcanza a esas filas:
una observación cuyo propietario es el acta lleva `id_acta_contexto` NULL. Su
limpieza es transaccional explícita, según `DECISION_OBS-01`.

### 5.5 Listado completo de triggers

| Trigger | Tabla | Código fijo | PK columna |
|---------|-------|-------------|------------|
| `trg_fal_persona_ad_observaciones`                        | `fal_persona`                         | 2  | `id` |
| `trg_fal_persona_domicilio_ad_observaciones`              | `fal_persona_domicilio`               | 3  | `id` |
| `trg_fal_documento_ad_observaciones`                      | `fal_documento`                       | 4  | `id` |
| `trg_fal_acta_evidencia_ad_observaciones`                 | `fal_acta_evidencia`                  | 5  | `id` |
| `trg_fal_notificacion_ad_observaciones`                   | `fal_notificacion`                    | 6  | `id` |
| `trg_fal_notificacion_intento_ad_observaciones`           | `fal_notificacion_intento`            | 7  | `id` |
| `trg_fal_acta_fallo_ad_observaciones`                     | `fal_acta_fallo`                      | 8  | `id` |
| `trg_fal_acta_apelacion_ad_observaciones`                 | `fal_acta_apelacion`                  | 9  | `id` |
| `trg_fal_acta_gestion_externa_ad_observaciones`           | `fal_acta_gestion_externa`            | 10 | `id` |
| `trg_fal_acta_paralizacion_ad_observaciones`              | `fal_acta_paralizacion`               | 11 | `id` |
| `trg_fal_acta_archivo_ad_observaciones`                   | `fal_acta_archivo`                    | 12 | `id` |
| `trg_fal_acta_medida_preventiva_ad_observaciones`         | `fal_acta_medida_preventiva`          | 13 | `id` |
| `trg_fal_acta_bloqueante_cierre_material_ad_observaciones`| `fal_acta_bloqueante_cierre_material` | 14 | `id` |
| `trg_fal_acta_articulo_infringido_ad_observaciones`       | `fal_acta_articulo_infringido`        | 15 | `id` |
| `trg_fal_acta_valorizacion_ad_observaciones`              | `fal_acta_valorizacion`               | 16 | `id` |
| `trg_fal_acta_obligacion_pago_ad_observaciones`           | `fal_acta_obligacion_pago`            | 17 | `id` |
| `trg_fal_acta_forma_pago_ad_observaciones`                | `fal_acta_forma_pago`                 | 18 | `id` |
| `trg_fal_acta_plan_pago_ref_ad_observaciones`             | `fal_acta_plan_pago_ref`              | 19 | `id` |
| `trg_fal_acta_pago_movimiento_ad_observaciones`           | `fal_acta_pago_movimiento`            | 20 | `id` |
| `trg_num_talonario_ad_observaciones`                      | `num_talonario`                       | 21 | `id` |
| `trg_num_talonario_movimiento_ad_observaciones`           | `num_talonario_movimiento`            | 22 | `id` |

Total: **21 triggers** (todos los códigos 2–22; código 1/ACTA usa CASCADE).

---

## 6. Validación al insertar observaciones

Implementada en el futuro `ObservacionService` (JDBC no implementado en este slice).

Contrato:

```text
switch cerrado por EntidadTipoObservada;
repositorio existe(id) por cada tipo;
sin nombres de tabla recibidos por request;
sin SQL dinámico.
```

La existencia de la entidad observada se valida en aplicación, no en DDL
(no hay FK polimórfica en MariaDB para este patrón).

---

## 7. Matriz de clasificación de campos libres

Clasificación de todos los campos con patrones `observacion`, `obs_`, `descripcion`,
`detalle`, `motivo`, `fundamento`, `resumen` en el modelo de dominio.

| Tabla / Entidad Java                    | Campo actual                              | Clasificación                         | Motivo |
|-----------------------------------------|-------------------------------------------|---------------------------------------|--------|
| `fal_observacion`                       | `observacion`                             | MANTENER_ESTRUCTURAL                  | Es la columna central de almacenamiento |
| `fal_acta`                              | `resumen_hecho`                           | MANTENER_ESTRUCTURAL                  | Descripción funcional del hecho, no nota libre |
| `fal_acta`                              | `dom_txt_infr`                            | MANTENER_ESTRUCTURAL                  | Domicilio del hecho, no nota libre |
| `fal_acta_paralizacion`                 | `descripcion`                             | MANTENER_ESTRUCTURAL                  | Descripción funcional de la paralización |
| `fal_acta_archivo`                      | `descripcion`                             | MANTENER_ESTRUCTURAL                  | Descripción funcional del archivo |
| `fal_acta_apelacion`                    | `texto_apelacion`                         | MANTENER_ESTRUCTURAL                  | Contenido jurídico de la apelación |
| `fal_acta_apelacion`                    | `fundamentos`                             | MANTENER_ESTRUCTURAL                  | Contenido jurídico; base legal de la apelación |
| `fal_acta_apelacion`                    | `fundamentos_resolucion`                  | MANTENER_ESTRUCTURAL                  | Contenido jurídico; base de la resolución |
| `fal_acta_fallo`                        | `fundamentos`                             | MANTENER_ESTRUCTURAL                  | Contenido jurídico del fallo |
| `fal_acta_bloqueante_cierre_material`   | `descripcion`                             | MANTENER_ESTRUCTURAL                  | Descripción funcional del bloqueante |
| `fal_acta_medida_preventiva`            | `med_prev_txt`                            | MANTENER_ESTRUCTURAL                  | Texto descriptivo de la medida aplicada |
| `fal_acta_sustancias_alimenticias`      | `descripcion_sustancias`                  | MANTENER_ESTRUCTURAL                  | Descripción funcional de las sustancias |
| `fal_acta_contravencion`                | `ambito_ctv_txt`                          | MANTENER_ESTRUCTURAL                  | Texto libre solo cuando ambito_ctv=OTRO |
| `fal_acta_sustancias_alimenticias`      | `ambito_ctv_txt`                          | MANTENER_ESTRUCTURAL                  | Texto libre solo cuando ambito_ctv=OTRO |
| `fal_documento_plantilla_contenido`     | `variables_declaradas_json`               | MANTENER_ESTRUCTURAL                  | Metadatos estructurados, no nota libre |
| `fal_documento_redaccion`               | `contenido_editable_markdown`             | MANTENER_ESTRUCTURAL                  | Contenido documental editable |
| `fal_documento_redaccion`               | `variables_snapshot_json`                 | MANTENER_ESTRUCTURAL                  | Snapshot estructurado de valores |
| `num_talonario`                         | `obs_talonario`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(TALONARIO) |
| `num_talonario_movimiento`              | `observacion`                             | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(MOVIMIENTO_TALONARIO) |
| `fal_acta_articulo_infringido`          | `observaciones`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(ARTICULO_INFRINGIDO) |
| `fal_notificacion`                      | `observaciones`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(NOTIFICACION) |
| `fal_acta_apelacion`                    | `observaciones`                           | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(APELACION) |
| `fal_acta_apelacion`                    | `observaciones_resolucion`                | CENTRALIZAR_EN_FAL_OBSERVACION        | Nota libre humana; mover a fal_observacion(APELACION) |
| `fal_acta_pago_movimiento`              | `motivo_aplicacion_pago_anterior VARCHAR` | CENTRALIZAR_EN_FAL_OBSERVACION        | Explicación libre; idempotencia estructural via FK movimiento_origen_id |
| `fal_documento_redaccion`               | `variables_faltantes_json`                | ELIMINAR_DERIVABLE                    | Calculable en aplicación; no persistir diagnóstico de estado |
| `fal_documento_redaccion`               | `diagnostico_json`                        | ELIMINAR_DERIVABLE                    | Calculable en aplicación; no persistir estado intermedio |
| `fal_acta_evento`                       | `descripcion_legible`                     | ELIMINAR_DERIVABLE                    | Reconstruible desde tipo_evento + actor + timestamp |
| `fal_documento`                         | `descripcion`                             | ELIMINAR_DERIVABLE                    | El nombre lo comunica la plantilla; no duplicar en documento |
| `fal_documento_plantilla`               | `descripcion`                             | ELIMINAR_DERIVABLE                    | nombre VARCHAR(64) debe comunicar uso e intención |
| `fal_persona`                           | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_persona_domicilio`                 | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_acta_evidencia`                    | `obs_evid` (si existiese)                 | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_documento`                         | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_acta_gestion_externa`              | `observacion` (si existiese)              | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |
| `fal_acta_articulo_infringido`          | `observacion_baja` (si existiese)         | PROHIBIDO — no crear                  | Verificar ausencia; no debe existir |

---

## 8. Campos embebidos eliminados (HUMAN_DECISION_CLOSED)

Los siguientes campos son eliminados del DDL y del modelo Java en este slice:

| Campo eliminado                                        | Entidad Java              | Motivo |
|--------------------------------------------------------|---------------------------|--------|
| `num_talonario.obs_talonario`                          | `NumTalonario`            | Centralizar en fal_observacion |
| `num_talonario_movimiento.observacion`                 | `NumTalonarioMovimiento`  | Centralizar en fal_observacion |
| `fal_acta_articulo_infringido.observaciones`           | `FalActaArticuloInfringido` | Centralizar en fal_observacion |
| `fal_notificacion.observaciones`                       | `FalNotificacion`         | Centralizar en fal_observacion |
| `fal_acta_apelacion.observaciones`                     | `FalActaApelacion`        | Centralizar en fal_observacion |
| `fal_acta_apelacion.observaciones_resolucion`          | `FalActaApelacion`        | Centralizar en fal_observacion |
| `fal_acta_pago_movimiento.motivo_aplicacion_pago_anterior` | `FalActaPagoMovimiento` | Texto libre no estructurado; usar fal_observacion + movimiento_origen_id para trazabilidad |
| `fal_documento_redaccion.variables_faltantes_json`     | `FalDocumentoRedaccion`   | Derivable; no persistir |
| `fal_documento_redaccion.diagnostico_json`             | `FalDocumentoRedaccion`   | Derivable; no persistir |
| `fal_acta_evento.descripcion_legible`                  | `FalActaEvento`           | Derivable desde tipo_evento + actor + timestamp |
| `fal_documento.descripcion`                            | `FalDocumento`            | Derivable desde plantilla |
| `fal_documento_plantilla.descripcion`                  | `FalDocumentoPlantilla`   | nombre debe comunicar uso; descripcion redundante |
