# Inspector

## Finalidad

`Inspector` representa al actor operativo que labra, captura o interviene sobre el acta dentro del circuito funcional del sistema.

Su función es vincular el expediente con la persona o agente que realizó la actuación correspondiente, preservando esa referencia como parte del contexto del caso.

---

## Qué representa en el sistema

`Inspector` representa un actor operativo del dominio y no un simple usuario técnico del sistema.

Tiene sentido funcional porque puede:

- labrar o capturar actas
- intervenir en actuaciones del expediente
- quedar asociado al origen material del caso
- formar parte del contexto administrativo visible o trazable del expediente

---

## Relación con el expediente

El Acta puede quedar asociada al Inspector que realizó el labrado, captura o intervención correspondiente.

Esa relación no reemplaza la trazabilidad por eventos, pero sí constituye parte del contexto principal del expediente.

La vinculación entre Acta e Inspector tiene valor administrativo y funcional propio.

---

## Relación con dependencia

El Inspector está asignado a una Dependencia.

Por lo tanto, no actúa de manera aislada: su intervención sobre el expediente ocurre dentro de un marco organizacional concreto.

Esa asignación permite que, al momento de la captura o labrado, el sistema ya conozca parte del contexto operativo inicial del acta a partir de la Dependencia asociada al Inspector, incluyendo si el circuito corresponde a tránsito u otro ámbito equivalente.

La Acta pertenece a una Dependencia y puede además estar asociada a un Inspector interviniente.

Estas referencias no son equivalentes y no deben confundirse.

---

## Regla de estabilidad histórica

Los datos del Inspector pueden cambiar con el tiempo.

Sin embargo, esos cambios no deben modificar indirectamente el contexto histórico de las actas ya emitidas o capturadas.

Por lo tanto, la relación entre Acta e Inspector debe contemplar una noción de versión aplicable al momento del expediente, de manera análoga a otras entidades referenciales cuyo contexto deba quedar congelado.

Esto evita tanto la mutación histórica indeseada como la necesidad de copiar snapshots textuales completos dentro del acta.

---

## Relación con numeración

El Inspector no debe entenderse como titular del talonario de actas.

La numeración administrativa del acta, cuando dependa de talonarios, se vincula a la Dependencia y a la política de numeración aplicable.

El Inspector se relaciona con el acta como actor de labrado o intervención, no como responsable de la asignación del número administrativo.

---

## Qué no es

`Inspector` no es:

- una dependencia
- el expediente
- el talonario de actas
- una simple firma técnica del sistema
- un rol abstracto sin vínculo con el caso

Tampoco debe confundirse con cualquier usuario genérico de backoffice.

---

## Relaciones clave

`Inspector` se relaciona conceptualmente con:

- Acta, como actor del labrado, captura o intervención
- Dependencia, como marco organizacional obligatorio de actuación
- versionado de entidades referenciales, para congelar correctamente el contexto aplicado al expediente
- eventos del expediente, cuando su actuación produzca hechos relevantes de trazabilidad