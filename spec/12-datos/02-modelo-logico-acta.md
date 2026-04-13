# Modelo lógico — Acta

## Finalidad

`Acta` es la entidad principal del modelo lógico.

Representa la identidad central del expediente administrativo y funciona como ancla de las demás piezas del sistema: eventos, documentos, notificaciones, medidas, presentaciones, resultados e integraciones vinculadas al caso.

---

## Rol dentro del modelo

`Acta` concentra los datos estructurales y de identidad principal del expediente.

Su función no es almacenar toda la historia del caso ni todos los detalles operativos, sino ofrecer la referencia central sobre la cual se vinculan las demás entidades del modelo.

Por lo tanto, `Acta` debe mantenerse estable, clara y relativamente compacta.

---

## Tabla principal

### `Acta`

Entidad principal del expediente.

Debe contener, como mínimo a nivel lógico:

- `Id`
- `IdTecnico`
- `NumeroActa`, cuando exista numeración administrativa asignada
- datos estructurales de apertura o incorporación
- datos principales de clasificación del caso
- referencias a estado actual principal cuando tengan valor operativo directo
- referencias mínimas necesarias para consulta e integración
- campos de trazabilidad administrativa básicos de la entidad

---

## Qué tipo de datos vive en Acta

En `Acta` deben vivir los datos que identifican o clasifican estructuralmente al expediente, por ejemplo:

- identidad técnica primaria interna
- identidad técnica distribuida de origen
- identidad administrativa visible
- fecha relevante de labrado, creación o incorporación
- tipo o clase principal del expediente
- origen principal
- dependencia, competencia o sector responsable, cuando corresponda
- referencias principales al estado operativo actual, si el modelo decide persistirlas
- observaciones estructurales mínimas solo si forman parte estable de la cabecera

La lógica general es que `Acta` conserve la identidad del expediente, no el detalle completo de su evolución.

---

## Qué no debe vivir en Acta

No deben vivir directamente en `Acta`, salvo justificación muy fuerte:

- historial detallado del expediente
- secuencia de transiciones
- detalle documental completo
- detalle de notificaciones
- resultados múltiples de procesos externos
- detalle extenso de medidas, liberaciones o presentaciones
- snapshot operativo completo
- duplicación de datos derivables desde eventos, documentos o notificaciones

Tampoco debe usarse `Acta` como tabla “contenedora universal” del expediente.

---

## Criterio sobre estado actual

`Acta` puede conservar una referencia compacta al estado actual principal del expediente cuando tenga valor operativo directo y simplifique lectura, filtros o vínculos con bandejas.

Pero esa referencia:

- no reemplaza la trazabilidad
- no reemplaza el snapshot
- no debe absorber semántica que pertenece a eventos o resultados específicos

En caso de duda, se debe preferir un estado principal compacto y dejar el detalle al resto del modelo.

---

## Identidades del expediente

La entidad `Acta` debe contemplar al menos tres planos de identidad distintos:

### Clave interna del sistema

- `Id`

Es la clave primaria técnica interna del sistema y de la base de datos central.

### Identidad técnica distribuida

- `IdTecnico`

Es un identificador técnico estable, generado en origen, independiente de la base central y útil para captura offline, sincronización, correlación técnica y prevención de duplicados.

A nivel lógico puede pensarse como un identificador tipo UUID, y en antecedentes del modelo ya aparece como `CHAR(36)`.

### Identidad administrativa visible

- `NumeroActa`

Es el número operativo/administrativo visible del acta.

No reemplaza ni a `Id` ni a `IdTecnico`, sino que convive con ambos como identidad administrativa del expediente para búsqueda, referencia, soporte papel y operación cotidiana.

---

## IdTecnico

`IdTecnico` debe existir desde el momento de captura del acta, incluso antes de que el expediente sea sincronizado o consolidado en la base central.

Su función es dar soporte a:

- independencia respecto de la base central al momento de creación
- operación offline-first
- sincronización posterior
- correlación con evidencia, adjuntos y datos capturados en origen
- deduplicación y reconciliación técnica
- trazabilidad técnica del expediente desde su nacimiento material

Este identificador técnico de origen:

- no reemplaza la clave primaria interna `Id`
- no reemplaza la numeración administrativa del acta
- no reemplaza otros identificadores de negocio que el expediente pueda requerir

Debe convivir con ellos como pieza de identidad técnica distribuida del modelo.

---

## NumeroActa

`NumeroActa` es la identidad administrativa visible del expediente.

Debe modelarse como valor textual y no como entero puro, para respetar políticas de numeración y formateo.

En antecedentes del modelo ya aparece como:

- `NumeroActa` → `VARCHAR(20)`

Esto resulta consistente con una numeración administrativa que puede incluir:

- prefijo opcional
- año
- número correlativo
- separadores
- otras reglas de formateo definidas por política

`NumeroActa` puede ser `NULL` mientras el expediente aún no tenga número administrativo asignado, si la operatoria concreta lo permite.

---

## Relación con talonarios de actas

La numeración del acta debe poder provenir de un talonario de actas.

El submodelo de talonarios, su asociación a dependencias y las modalidades de numeración se definen en el bloque de catálogos y maestros.

Ver: [Modelo lógico — Catálogos y maestros](08-modelo-logico-catalogos-y-maestros.md)

---

## Política de numeración

La política de numeración debe definirse en la configuración del talonario.

Esa política puede incluir, entre otros elementos:

- prefijo opcional
- año / AAAA
- número correlativo
- separadores
- longitud máxima
- formato visible final

Por lo tanto, el número visible del acta no debe pensarse como una simple secuencia numérica aislada, sino como el resultado de una política de numeración asociada al talonario.

---

## Regla de unicidad operativa

No debe permitirse registrar dos veces el mismo número de acta dentro del mismo esquema de numeración aplicable.

En particular, cuando exista talonario manual o preimpreso, el sistema debe impedir reutilizar dos veces el mismo número del mismo talonario, aunque esos números no se hayan materializado previamente uno por uno en la base.

Esto implica que la unicidad administrativa del acta no debe resolverse solo sobre el valor textual aislado de `NumeroActa`, sino también sobre el contexto de numeración que lo origina.

La definición física exacta de constraints y claves alternativas se resolverá en la etapa posterior de persistencia/SQL.

---

## Relación con motor de numeración

La asignación de `NumeroActa` no se resuelve dentro de la entidad `Acta` misma, sino a través de un mecanismo transversal de numeración.

Ese motor de numeración debe poder entregar números para distintos objetos administrativos, por ejemplo:

- actas
- documentos
- notificaciones
- otros elementos numerables del sistema

También puede requerirse la asignación por unidad o por rango, según la operatoria.

La definición completa del submodelo de numeración y talonarios se desarrollará fuera de este archivo, pero la dependencia conceptual de `NumeroActa` respecto de ese mecanismo queda aquí establecida.

---

## Clasificación principal del expediente

La entidad `Acta` debe permitir reflejar la clasificación principal del caso, por ejemplo mediante referencias a catálogos o maestros para:

- tipo de acta
- materia o ámbito principal
- estado principal actual
- origen
- dependencia o área competente
- motivo general de cierre o situación terminal, si el modelo decide persistirlo de forma compacta

No conviene embutir clasificaciones complejas como texto libre cuando tengan valor transversal en el sistema.

---

## Trazabilidad administrativa básica

Aunque la trazabilidad fuerte vive en eventos, la entidad `Acta` puede conservar metadatos mínimos de trazabilidad administrativa de la propia fila, por ejemplo:

- fecha de creación en sistema
- fecha de última actualización relevante
- usuario o proceso de alta
- usuario o proceso de última modificación estructural

Estos datos no reemplazan la trazabilidad funcional del expediente.

---

## Anexos o extensiones razonables

Según el nivel de detalle final del modelo, `Acta` puede complementarse con anexos o satélites para evitar sobrecargar su tabla principal.

Ejemplos posibles:

- datos complementarios específicos por tipo de acta
- datos de localización o contexto material
- vínculos con sujetos, dominios u objetos relacionados
- metadatos adicionales de origen
- datos especializados de tránsito, contravención u otro subtipo

La regla general es:

- lo común y estructural vive en `Acta`
- lo especializado o voluminoso vive fuera de `Acta`

---

## Relación con eventos

La relación entre `Acta` y `ActaEvento` es central.

- `Acta` representa la identidad principal del expediente
- `ActaEvento` representa los hechos relevantes de su evolución

Por lo tanto, `Acta` no debe duplicar la historia detallada del caso.

---

## Relación con documentos

`Acta` es el ancla principal de las piezas documentales del expediente.

Los documentos se vinculan al expediente a través de relaciones explícitas y no deben absorberse como simples campos dentro de `Acta`, salvo referencias excepcionales de conveniencia operativa claramente justificadas.

---

## Relación con notificaciones

Las notificaciones también se vinculan al expediente a partir de su identidad principal.

`Acta` no debe almacenar el detalle del proceso de notificación, pero sí puede servir como punto de referencia para:

- búsquedas
- filtros
- lecturas operativas
- correlación con el estado general del caso

---

## Relación con snapshot

El snapshot operativo puede derivarse total o parcialmente a partir de la situación actual del expediente y de sus piezas asociadas.

Por eso:

- `Acta` no reemplaza al snapshot
- el snapshot no reemplaza a `Acta`
- ambos cumplen funciones distintas dentro del modelo

---

## Relación con satélites del expediente

Además de eventos, documentos y notificaciones, `Acta` puede vincularse con satélites como:

- medidas y liberaciones
- presentaciones
- derivaciones
- resultados externos
- observaciones relevantes
- datos especializados por subtipo

Todos estos elementos deben entenderse como subordinados a la unidad principal del expediente.

---

## Criterio de compactación

La entidad `Acta` debe mantenerse compacta.

No conviene convertirla en una tabla gigante con decenas de columnas heterogéneas que mezclen:

- identidad
- clasificación
- trazabilidad
- snapshot
- documentos
- notificación
- resultados de procesos externos

Cuando aparezca esa tendencia, debe separarse en anexos o satélites.

---

## Resultado esperado del modelado

Al cerrar la bajada lógica de `Acta`, el modelo debe dejar claro:

- qué constituye la identidad central del expediente
- qué datos estructurales pertenecen a la entidad principal
- qué información debe separarse fuera de `Acta`
- qué relaciones principales deberán resolverse en las entidades posteriores del bloque
- que la identidad del expediente combina clave interna, identidad técnica distribuida y, cuando corresponda, numeración administrativa visible
- que la numeración administrativa del acta depende de talonarios y de una política de numeración asociada