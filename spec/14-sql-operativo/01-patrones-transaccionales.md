# [14-SQL-OPERATIVO] 01 - PATRONES TRANSACCIONALES

## Finalidad

Este archivo define los patrones transaccionales base que deben seguir las operaciones de escritura del sistema de faltas municipal.

Su objetivo es dejar explícito, de forma uniforme y reutilizable:

- cómo se estructuran las altas transaccionales
- cómo se usan las secuencias Informix
- cómo se propagan IDs a registros hijos
- cómo se resuelve la persistencia de agregados compuestos
- qué parte debe ocurrir dentro de una única transacción
- qué parte puede diferirse
- cómo se combinan escritura operativa, trazabilidad y proyección

Este archivo no define todo el SQL final.  
Define los **patrones fundamentales de persistencia** sobre los cuales luego se documentan los CRUD y operaciones concretas.

---

## Reglas base

- La transacción sigue el caso de uso real.
- La escritura debe ser explícita, secuenciada y auditable.
- El uso de secuencias es obligatorio y visible.
- Lo diferido debe documentarse expresamente.
- Si falla una parte obligatoria, falla la transacción.
- Lo histórico append-only no se reescribe.
- La proyección mínima operativa puede actualizarse en línea.
- La recomputación total puede diferirse.

---

## Regla general de composición

Las operaciones concretas del sistema no siempre coinciden con un único patrón puro.

En muchos casos se construyen como composición de:

- un patrón base de persistencia
- uno o más pasos comunes
- una eventual parte diferida

Por eso este archivo define primero los **patrones fundamentales** y luego los **pasos comunes reutilizables**.

---

## Patrones fundamentales

### 1. Alta simple

Inserción de un registro en una única tabla principal.

#### Orden base

1. obtener `Id` desde la secuencia
2. insertar registro principal
3. confirmar transacción

#### Ejemplos típicos

- `FalObservacion`
- alta de una entidad maestra simple
- marca técnica o registro auxiliar desacoplado

---

### 2. Alta de agregado padre/hijos

Creación de una entidad principal junto con uno o más registros dependientes.

Este patrón cubre tanto hijos:

- obligatorios
- opcionales
- o combinación de ambos

#### Orden base

1. obtener `Id` del padre desde la secuencia
2. insertar registro padre
3. insertar hijos obligatorios y opcionales presentes
4. ejecutar pasos comunes si corresponden
5. confirmar transacción

#### Ejemplos típicos

- alta de `FalActa` con satélites
- `FalDocumento` + `FalActaDocumento`
- `FalNotificacion` + `FalNotificacionIntento`
- alta de entidad versionada con primera versión

---

### 3. Asociación entre entidades existentes

Creación de un registro que vincula entidades ya persistidas.

#### Orden base

1. obtener IDs de entidades a vincular
2. insertar registro de asociación
3. ejecutar pasos comunes si corresponden
4. confirmar transacción

#### Ejemplos típicos

- vínculo `FalActaDocumento`
- asociación de `StorObjeto` a `FalDocumento`
- asignación de `NumTalonario` a dependencia o inspector

---

### 4. Corrección controlada de dato mutable

Modificación de un dato permitido sobre una entidad existente, sin violar el carácter histórico del sistema.

#### Orden base

1. localizar registro a corregir
2. ejecutar `UPDATE` controlado
3. ejecutar pasos comunes si corresponden
4. confirmar transacción

#### Regla

Este patrón no aplica a:

- historial append-only
- eventos ya consolidados
- intentos históricos
- trazabilidad que debe salir por nuevo registro

---

### 5. Versionado con cierre y nueva versión

Cierre de vigencia de una versión activa e inserción de una nueva versión.

#### Orden base

1. localizar versión vigente
2. cerrar vigencia previa
3. insertar nueva versión
4. confirmar transacción

#### Ejemplos típicos

- `FalDependenciaVersion`
- `FalInspectorVersion`
- `FalAlcoholimetroVersion`
- `RubroComVersion`

---

### 6. Inserción con idempotencia

Inserción protegida contra duplicación por clave técnica o funcional.

#### Orden base

1. verificar existencia por clave de idempotencia
2. si no existe, insertar usando el patrón que corresponda
3. si ya existe, devolver resultado coherente
4. confirmar transacción si hubo inserción

#### Casos típicos

- altas con `IdTecnico`
- sincronización móvil
- reintentos de integración

---

### 7. Operación con parte diferida

Operación donde la transacción principal consolida el estado interno y deja preparada una etapa posterior fuera de la transacción original.

#### Orden base

1. ejecutar transacción principal usando el patrón base correspondiente
2. registrar o dejar preparada la tarea diferida
3. confirmar transacción
4. procesar la parte diferida fuera de la transacción original

#### Ejemplos típicos

- documento listo para firma externa
- notificación lista para despacho externo
- archivo listo para persistirse en `Stor*`
- solicitud a numeración o servicio externo posterior

---

## Pasos comunes reutilizables

Los siguientes no se consideran patrones autónomos, sino pasos comunes que pueden aparecer dentro de otros patrones.

### A. Registrar evento append-only

Cuando el hecho tiene relevancia procesal o administrativa, debe registrarse un nuevo hecho histórico.

Tabla típica:
- `FalActaEvento`

Regla:
- no resolverlo con `UPDATE` silencioso si debe existir traza histórica

---

### B. Actualizar o insertar proyección resumida

Cuando una operación impacta un dato visible para operación inmediata, puede actualizarse la proyección mínima dentro de la misma transacción.

Tabla típica:
- `FalActaSnapshot`

Regla:
- actualizar solo la porción mínima necesaria
- la recomputación total puede diferirse

---

### C. Registrar observación transversal

Cuando una operación necesita dejar contexto adicional o comentario auxiliar, puede insertar observación.

Tabla típica:
- `FalObservacion`

Regla:
- la observación no reemplaza al evento ni al cambio de estado

---

## Regla de aplicación

Cada archivo CRUD o de operación concreta debe indicar, como mínimo:

- qué tablas intervienen
- qué secuencias se usan
- qué patrón fundamental aplica
- qué pasos comunes se ejecutan
- qué parte ocurre en línea
- qué parte se difiere
- qué invariantes protege

---

## Relación con otros archivos

Aplicación concreta de estos patrones:

- acta → [`04-sql-crud-acta.md`](./04-sql-crud-acta.md)
- documental → [`05-sql-crud-documental.md`](./05-sql-crud-documental.md)
- notificación → [`06-sql-crud-notificacion.md`](./06-sql-crud-notificacion.md)
- referenciales y transversales → [`07-sql-crud-referenciales-y-transversales.md`](./07-sql-crud-referenciales-y-transversales.md)
- snapshot y reproceso → [`08-sql-proyecciones-y-reproceso.md`](./08-sql-proyecciones-y-reproceso.md)

---