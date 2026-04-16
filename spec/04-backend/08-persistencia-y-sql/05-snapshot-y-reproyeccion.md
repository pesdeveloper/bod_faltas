# Snapshot y reproyección

## Finalidad

Este archivo define cómo se persiste `ActaSnapshot` y cómo se resuelve su regeneración o reproyección.

---

## Regla base

El snapshot es:

- derivado
- regenerable
- no fuente primaria de verdad

Su persistencia existe por razones operativas, no como reemplazo del núcleo transaccional.

La estrategia actual privilegia una única proyección operativa del expediente (`ActaSnapshot`), evitando incorporar estructuras auxiliares de control si no aportan valor operativo concreto.

---

## Estructura esperada

Debe existir:

- `ActaSnapshot` o equivalente

No deben incorporarse tablas técnicas adicionales de snapshot si no existe una necesidad práctica real y estable del sistema.

---

## Qué debe persistir

El snapshot debe persistir solo información derivada útil para:

- bandejas
- filtros
- búsqueda operativa
- lectura rápida del expediente
- indicadores visibles de situación actual

No debe persistir detalle histórico completo.

---

## Reglas de persistencia

- Debe existir una única proyección operativa vigente por expediente, salvo justificación fuerte en contrario.
- El snapshot debe poder recalcularse desde el núcleo.
- Su contenido debe poder explicarse desde `Acta`, `ActaEvento`, `Documento`, `Notificacion` y demás piezas primarias.
- Puede quedar transitoriamente desactualizado, pero debe poder reproyectarse.
- No debe contener semántica imposible de recomponer.

---

## Reproyección

La reproyección debe contemplar al menos:

- regeneración por cambio del expediente
- regeneración por corrección o reproceso
- regeneración masiva, si luego se requiere
- consistencia entre proyección vigente y núcleo transaccional

La estrategia concreta de jobs o workers se desarrollará fuera de este archivo.

---

## Consultas

Las bandejas y listados operativos deberían apoyarse preferentemente en snapshot o proyecciones equivalentes.

La reconstrucción histórica no debe apoyarse en snapshot.

---

## Relaciones clave

Este bloque se relaciona con:

- `Acta`, como expediente proyectado
- `ActaEvento`, como fuente principal de reconstrucción
- `Documento`, `Notificacion`, medidas y demás piezas que impactan en la situación actual
- bandejas y consultas operativas, como principal consumidor funcional

---

## Resultado esperado

Este bloque debe dejar resuelto que el snapshot se persiste como proyección operativa del expediente y que su regeneración forma parte normal del backend.