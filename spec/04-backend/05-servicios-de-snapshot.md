# 05-servicios-de-snapshot.md

## Finalidad

Este archivo define las responsabilidades del backend respecto de la proyección operativa del expediente.

Su objetivo es dejar claro cómo se construye, actualiza y expone `ActaSnapshot` como resumen operativo principal del expediente.

---

## Regla principal

La estrategia actual privilegia una única proyección operativa principal del expediente: `ActaSnapshot`.

Esta proyección debe ser:

- derivada
- regenerable
- no fuente primaria de verdad
- orientada a bandejas, plazos, notificaciones, gestión externa, situación económica resumida y filtros rápidos

No deben incorporarse estructuras auxiliares de snapshot o control si no aportan valor operativo concreto al sistema.

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- construir `ActaSnapshot`
- refrescarlo cuando cambian hechos relevantes del expediente
- exponerlo para consultas operativas
- usarlo como base principal de bandejas y filtros
- regenerarlo desde la fuente primaria cuando sea necesario

---

## Qué debe resumir

`ActaSnapshot` debe poder reflejar, según el modelo ya consolidado:

- fecha del acta
- dependencia e inspector
- bloque actual
- estado procesal actual
- situación administrativa actual
- código de bandeja y visibilidad
- notificaciones relevantes y su estado resumido
- existencia de reintentos
- gestión externa y su resultado resumido
- pago voluntario, pago total y plan de pagos si aplica
- plazos operativos relevantes

---

## Qué no debe hacer

El snapshot no debe:

- reemplazar el expediente
- reemplazar eventos
- reemplazar documentos
- reemplazar notificaciones
- contener historia completa
- convertirse en una segunda verdad del sistema

---

## Relación con otros servicios

### Con expediente
El expediente produce hechos que obligan a reproyectar snapshot.

### Con notificación
Las notificaciones alimentan estado resumido, pendientes y reintentos visibles.

### Con gestión externa
La gestión externa debe reflejarse resumidamente en snapshot.

### Con bandejas
Las bandejas deben apoyarse prioritariamente en snapshot como proyección operativa única del expediente.

---

## Idea clave

El backend debe tratar `ActaSnapshot` como una vista operativa central, útil para decisión y consulta rápida, pero siempre subordinada a la fuente primaria del dominio.