# Talonario y numeración

## Finalidad

`Talonario y numeración` representa el mecanismo administrativo mediante el cual el sistema asigna números visibles a objetos numerables del dominio, en particular al Acta y a otras piezas documentales cuando corresponda.

Su función es separar claramente la identidad administrativa visible de la identidad interna y de la identidad técnica distribuida de cada objeto.

---

## Qué representa en el sistema

Este bloque representa el conjunto de reglas y referencias necesarias para administrar numeración visible.

Incluye conceptualmente:

- talonarios
- políticas de numeración
- modalidades automáticas o manuales/preimpresas
- asignación operativa según el tipo de objeto numerable
- formato visible del número resultante

No se limita a una simple secuencia numérica.

---

## Relación con el acta

El número de acta constituye una identidad administrativa visible del expediente.

Ese número:

- no reemplaza la clave interna del sistema
- no reemplaza la identidad técnica de origen
- puede provenir de un talonario de actas
- responde a una política de numeración determinada

Por lo tanto, la numeración del acta debe entenderse como una pieza administrativa propia, separada de las demás identidades del expediente.

---

## Relación con documentos y otros objetos numerables

La numeración no es exclusiva del acta.

El sistema puede requerir numeración visible también para:

- documentos
- notificaciones
- otras piezas administrativas numerables

Por eso, el mecanismo de talonarios y numeración debe pensarse como transversal y reutilizable.

---

## Política de numeración

La política de numeración debe definirse en la configuración del talonario aplicable.

Puede incluir, entre otros elementos:

- prefijo opcional
- año
- número correlativo
- separadores
- longitud máxima
- formato visible final

El número resultante debe tratarse como valor textual y no como entero puro.

---

## Modalidades de numeración

La numeración puede responder a distintas modalidades operativas, por ejemplo:

- automática por sistema
- manual o preimpresa en papel

El sistema debe poder convivir con ambas cuando la operatoria lo requiera.

En particular, para actas puede existir talonario manual/preimpreso asociado a dependencia, sin necesidad de materializar previamente todos los números posibles en la base.

---

## Relación con dependencia

En el caso de las actas, el talonario debe entenderse asignado a una Dependencia.

La Dependencia condiciona qué numeración administrativa resulta válida para las actas emitidas o capturadas en su ámbito.

Esto no convierte al talonario en parte del expediente mismo, pero sí lo vuelve una referencia administrativa relevante para su identidad visible.

---

## Relación con inspector

El Inspector no es el titular del talonario de actas.

Puede estar asociado al expediente como actor del labrado o captura, pero la numeración administrativa del acta no debe depender conceptualmente del inspector sino de la dependencia y de la política de numeración aplicable.

---

## Regla de unicidad operativa

No debe permitirse reutilizar indebidamente la misma numeración administrativa dentro del mismo esquema o contexto de numeración aplicable.

En particular, cuando exista talonario manual o preimpreso, el sistema debe impedir registrar dos veces el mismo número del mismo talonario, aun cuando no se hayan generado previamente uno por uno todos los números posibles.

La unicidad administrativa no debe pensarse solo sobre el valor textual aislado del número, sino también sobre el contexto de numeración que lo origina.

---

## Relación con motor de numeración

La asignación del número visible no se resuelve en la entidad numerada en sí misma, sino mediante un mecanismo transversal de numeración.

Ese mecanismo puede entregar números para:

- una unidad individual
- rangos de numeración
- distintos tipos de objetos numerables

El detalle estructural de ese motor se resolverá en el bloque lógico de datos y luego en persistencia/SQL.

---

## Qué no es

`Talonario y numeración` no es:

- la identidad interna del expediente
- la identidad técnica distribuida
- la dependencia
- el inspector
- el documento en sí mismo
- un simple contador sin contexto administrativo

---

## Relaciones clave

`Talonario y numeración` se relaciona conceptualmente con:

- Acta, como objeto numerable principal del expediente
- Documento y otros objetos numerables, cuando corresponda
- Dependencia, como ámbito de asignación del talonario de actas
- modelo lógico de datos, donde luego se definirá la estructura concreta de talonarios, políticas y referencias