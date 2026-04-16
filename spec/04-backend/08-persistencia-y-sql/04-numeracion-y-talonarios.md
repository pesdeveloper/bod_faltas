# Numeración y talonarios

## Finalidad

Este archivo define criterios de persistencia y SQL para la numeración visible y el uso de talonarios en el sistema.

---

## Regla base

La numeración visible pertenece al contexto administrativo de numeración y no reemplaza:

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
- una estructura de movimiento o estado del número manual, cuando el proceso lo requiera

---

## Reglas de persistencia

- La numeración visible no reemplaza la identidad interna del objeto.
- La política de numeración no vive dentro del objeto numerado.
- Un objeto puede nacer sin número visible.
- Un objeto puede pre-numerarse.
- Un objeto puede numerarse al firmar.
- La numeración puede responder a circuito automático o manual/preimpreso.
- La política de numeración debe definirse por componentes habilitados, como prefijo, año, serie y número, con separadores controlados entre componentes, evitando máscaras libres difíciles de validar y mantener.

---

## Actas

Para actas:

- el número visible puede depender de talonarios asociados a dependencias
- el talonario no debe depender del inspector como titular
- el inspector participa del expediente, no de la titularidad del talonario

---

## Talonarios globales y por dependencia

Los talonarios no deben asumirse siempre como dependientes de una dependencia.

Pueden existir talonarios globales para otros objetos numerables, mientras que para actas la asignación a dependencia sigue siendo la regla principal.

---

## Numeración manual física

En talonarios manuales físicos, el estado de cada número debe quedar trazado mediante `TalonarioMovimiento`, sin necesidad de una tabla separada de anulados si el estado ya queda resuelto allí.

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