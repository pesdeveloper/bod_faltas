# Numeración y talonarios

## Finalidad

Este archivo define cómo se persisten talonarios, políticas de numeración y control de numeración visible para actas, documentos y demás objetos numerables.

---

## Regla base

La numeración visible debe persistirse separada de:

- identidad interna
- identidad técnica, si existe
- lógica propia del objeto numerado

La numeración debe resolverse con su contexto de numeración aplicable.

---

## Estructura esperada

Debe existir al menos:

- `Talonario`
- una entidad o mecanismo equivalente para política de numeración
- una entidad o mecanismo equivalente para asignación de talonarios, si corresponde
- estructuras auxiliares de control, reserva o correlativo, si el diseño final lo requiere

---

## Reglas de persistencia

- La numeración visible no reemplaza la identidad interna del objeto.
- La política de numeración no vive dentro del objeto numerado.
- Un objeto puede nacer sin número visible.
- Un objeto puede pre-numerarse.
- Un objeto puede numerarse al firmar.
- La numeración puede responder a circuito automático o manual/preimpreso.
- La persistencia debe contemplar numeración por unidad o por rangos, si el proceso lo requiere.

---

## Actas

Para actas:

- el número visible puede depender de talonarios asociados a dependencias
- el talonario no debe depender del inspector como titular
- el inspector participa del expediente, no de la titularidad del talonario

---

## Unicidad operativa

La persistencia debe impedir reutilización indebida de numeración visible dentro del contexto de numeración aplicable.

Esto incluye, cuando corresponda:

- mismo número dentro del mismo talonario
- mismo número dentro del mismo esquema de numeración
- talonarios manuales o preimpresos sin materialización previa de todos los números posibles

La unicidad no debe resolverse solo sobre el valor textual aislado.

---

## Relaciones clave

Este bloque se relaciona con:

- `Acta`
- `Documento`
- otros objetos numerables, si luego aparecen
- `Dependencia`, para asignación de talonarios de actas
- catálogos y maestros, para tipo de objeto numerable y política aplicable

---

## Resultado esperado

Este bloque debe dejar resuelto que la numeración visible se persiste con su contexto administrativo y que los talonarios constituyen una estructura propia del backend.