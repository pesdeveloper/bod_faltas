# 09-bandeja-gestion-externa.md

## Finalidad

Esta bandeja reúne expedientes derivados a una gestión externa formal.

Su función es dar visibilidad a los casos que salieron del circuito interno principal pero que conservan trazabilidad y posibilidad de producir efectos materiales sobre el expediente.

---

## Qué contiene

Contiene expedientes derivados, por ejemplo, a:

- apremios
- Juzgado de Paz
- otra gestión externa formal admitida por el sistema

La derivación externa no implica pérdida de seguimiento del expediente.

---

## Qué no contiene

No contiene expedientes que todavía deban resolverse íntegramente dentro del circuito interno.

No contiene expedientes solo por estar en espera de notificación o firma.

Tampoco contiene expedientes cerrados sin posibilidad de reingreso material.

---

## Función operativa

Su función es permitir:

- identificar el destino externo del expediente
- registrar actuaciones o resultados externos
- mantener trazabilidad del trámite fuera del circuito interno principal
- reingresar el expediente cuando el resultado externo tenga efecto material

---

## Regla de entrada

Un expediente entra a esta bandeja cuando se decide formalmente su derivación a una gestión externa.

Esto puede ocurrir, por ejemplo, después de:

- una etapa post fallo sin novedad que altere el recorrido
- una decisión administrativa de derivación externa
- una resolución o situación recursiva que determine salida del circuito interno

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras la gestión externa no haya producido todavía un resultado suficiente para definir su siguiente situación operativa.

---

## Regla de salida

La salida de esta bandeja depende del resultado externo.

Destinos típicos:

- pendientes de fallo, si el resultado exige nuevo fallo, rectificación o modificación
- análisis / presentaciones / pagos, si reingresa con novedad material que exige evaluación
- archivo, si el expediente quedó resuelto pero todavía no puede cerrarse
- cerradas, si ya cumple completamente las reglas de cierre
- paralizadas, si corresponde detención fundada

No existe una única salida obligatoria para todos los casos de gestión externa.

---

## Acciones típicas

- registrar derivación externa
- registrar resultado externo
- incorporar actuaciones o documentos externos
- reingresar a fallo
- reingresar a análisis
- enviar a archivo
- enviar a cerradas
- paralizar

---

## Relación con reingreso

La gestión externa no debe modelarse como simple salida sin retorno.

El sistema debe contemplar reingreso cuando el resultado externo produzca efectos materiales como, por ejemplo:

- necesidad de nuevo fallo
- rectificación de fallo
- modificación del estado del expediente
- pago externo
- cierre externo con efecto en el expediente
- otra novedad relevante

---

## Relación con snapshot

El snapshot operativo debe permitir identificar si el expediente está en gestión externa, de qué tipo es esa gestión, si existió reingreso desde ella y cuál es su resultado resumido actual.

El snapshot debe permitir identificar, al menos:

- que el expediente está en gestión externa
- cuál es el destino externo general
- si existe resultado externo pendiente o ya registrado
- si el expediente quedó listo para reingreso, archivo o cierre

---

## Relación con la UI

La UI de esta bandeja debe permitir ver con claridad:

- a qué gestión externa fue derivado el expediente
- en qué estado general se encuentra
- si ya existe resultado externo
- qué acciones están habilitadas
- si el expediente debe reingresar al circuito

---

## Idea clave

La gestión externa no saca al expediente del sistema.

Lo mantiene trazable y listo para reingresar cuando la actuación externa produzca efectos materiales sobre su situación operativa.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja con apelación](08-bandeja-con-apelacion.md)
- [Bandeja de pendientes de fallo](05-bandeja-pendientes-fallo.md)
- [Bandeja de análisis / presentaciones / pagos](03-bandeja-analisis-presentaciones-pagos.md)
- [Bandeja de archivo](11-bandeja-archivo.md)
- [Bandeja de cerradas](12-bandeja-cerradas.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)