# 02-bandeja-enriquecimiento.md

## Finalidad

Esta bandeja reúne expedientes en revisión, validación y completitud documental inicial.

Es una de las principales bandejas de preparación del expediente para su continuidad operativa.

---

## Qué contiene

Contiene expedientes que ya fueron labrados y que todavía no tienen completo el conjunto mínimo de definiciones y documentos necesarios para continuar.

El expediente permanece aquí mientras falte generar alguna pieza necesaria para el siguiente paso.

---

## Qué no contiene

No contiene expedientes recién labrados sin decisión inicial mínima.

No contiene expedientes cuyo problema actual sea exclusivamente:

- análisis de presentaciones o pagos
- pendientes de firma
- notificación
- archivo
- cierre

---

## Función operativa

Su función es permitir que el expediente:

- sea revisado
- sea validado
- complete información faltante
- determine qué piezas documentales necesita
- genere los documentos mínimos necesarios para poder avanzar

Esta bandeja no existe para “mandar el expediente a otro lado a ver qué se hace”.

Existe para completar su preparación documental y operativa inicial.

---

## Regla de entrada

Un expediente entra a esta bandeja cuando ya salió de labradas y requiere revisión o completitud documental inicial.

También puede reingresar si una actuación posterior obliga a volver a una etapa de revisión temprana.

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras no estén generados todos los documentos o definiciones mínimas necesarias para continuar.

Ejemplos típicos:

- falta generar una medida preventiva
- falta generar una resolución inicial
- falta completar documentación previa
- falta definir el encuadre documental mínimo del expediente

---

## Regla de salida

La salida de esta bandeja se habilita cuando ya está completo el conjunto mínimo requerido para el siguiente paso operativo.

La salida no es única. Depende de la situación concreta del expediente.

Destinos típicos:

- pendientes de firma, si existen documentos generados que requieren firma
- notificaciones, si ya existen piezas notificables firmadas
- pendientes de fallo, si el expediente ya queda en condición de requerir fallo
- pendientes de resolución / redacción, si requiere una pieza no-fallo posterior
- análisis / presentaciones / pagos, si surge una actuación que exige tratamiento material
- archivo, si el trámite principal quedó resuelto pero no puede cerrarse
- cerradas, si ya cumple completamente las reglas de cierre

---

## Acciones típicas

- completar datos
- validar expediente
- revisar consistencia inicial
- definir piezas necesarias
- generar documentos requeridos
- enviar a análisis
- enviar a pendientes de resolución / redacción
- enviar a pendientes de fallo
- enviar a pendientes de firma
- enviar a notificaciones
- enviar a archivo
- enviar a cerradas
- paralizar

---

## Relación con documentos requeridos

Esta bandeja es especialmente sensible a la completitud documental.

Si el expediente necesita uno o más documentos para continuar, debe permanecer aquí hasta que esas piezas estén generadas.

Ejemplo típico:
- si el expediente requiere una o varias medidas preventivas, debe seguir aquí hasta que dichas piezas existan
- recién después, si corresponde firma, pasa a pendientes de firma

---

## Relación con snapshot

El snapshot debe permitir identificar que el expediente:

- está en revisión inicial
- aún no completó el conjunto documental mínimo
- todavía no está listo para pasar a la siguiente situación operativa

---

## Relación con la UI

La UI de esta bandeja debe permitir ver con claridad:

- qué le falta al expediente para avanzar
- qué documentos o definiciones faltan
- qué acciones están habilitadas
- cuándo ya se habilitó el pase a la siguiente bandeja

---

## Idea clave

El expediente no sale de esta bandeja por tiempo ni por simple movimiento manual.

Sale cuando su preparación documental y operativa mínima ya quedó suficientemente completa para el siguiente paso.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de labradas](01-bandeja-labradas.md)
- [Bandeja de análisis / presentaciones / pagos](03-bandeja-analisis-presentaciones-pagos.md)
- [Bandeja de pendientes de firma](06-bandeja-pendientes-firma.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Medidas y liberaciones](../01-dominio/06-medidas-y-liberaciones.md)