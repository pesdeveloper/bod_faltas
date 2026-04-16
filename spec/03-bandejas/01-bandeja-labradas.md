# 01-bandeja-labradas.md

## Finalidad

Esta bandeja reúne actas recién labradas, todavía en etapa inicial.

Representa el punto de entrada operativo del expediente al circuito del sistema.

---

## Qué contiene

No incluye borradores técnicos previos al labrado.

El expediente ingresa a esta bandeja cuando el acta ya fue efectivamente labrada y pasó a integrar el circuito administrativo del sistema.

Contiene expedientes / actas recién creados a partir del labrado inicial.

Todavía no se consideran listos para continuar el circuito completo hasta que se realice la revisión o decisión inicial correspondiente.

---

## Qué no contiene

No contiene expedientes que ya pasaron por revisión inicial.

No contiene expedientes cuya situación actual ya sea de análisis, firma, notificación, archivo o cierre.

---

## Función operativa

Su función es permitir la primera lectura administrativa del expediente y definir su primer destino operativo.

Desde aquí se decide si el expediente:

- pasa a revisión / enriquecimiento
- requiere análisis
- puede encauzarse a nulidad, si correspondiera
- admite una solicitud inicial de pago voluntario
- u otra acción inicial permitida por la regla de negocio

---

## Regla de entrada

Un expediente entra a esta bandeja cuando se produce el labrado inicial del acta.

Este ingreso puede provenir, según la superficie operativa, por ejemplo desde:

- app móvil de inspectores
- otro canal formal de labrado admitido por el sistema

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras no se haya realizado la revisión o decisión inicial mínima que determine su siguiente situación operativa.

---

## Regla de salida

La salida de esta bandeja se habilita cuando ya existe una decisión inicial suficiente sobre el destino del expediente.

No existe una única salida obligatoria.

El destino depende de la situación concreta del expediente y de las reglas aplicables.

---

## Acciones típicas

- enviar a enriquecimiento / revisión inicial
- enviar a análisis / presentaciones / pagos
- pasar a nulidad, si corresponde
- iniciar solicitud de pago voluntario
- registrar una observación o decisión inicial relevante
- paralizar, si existiera una causal excepcional fundada

---

## Relación con snapshot

El snapshot debe permitir identificar rápidamente que el expediente:

- se encuentra recién labrado
- todavía no completó revisión inicial
- todavía no definió su siguiente situación operativa

---

## Relación con la UI

La UI de esta bandeja debe permitir ver con claridad:

- identificación básica del expediente
- fecha y origen de labrado
- datos mínimos necesarios para decidir el primer destino
- acciones iniciales habilitadas

No debería recargarse con lógica documental que todavía no corresponde a esta etapa.

---

## Idea clave

Esta bandeja representa el ingreso inicial del expediente al circuito operativo.

Su función no es resolver el caso, sino permitir la primera decisión administrativa sobre su encauzamiento.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de enriquecimiento](02-bandeja-enriquecimiento.md)
- [Bandeja de análisis / presentaciones / pagos](03-bandeja-analisis-presentaciones-pagos.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)