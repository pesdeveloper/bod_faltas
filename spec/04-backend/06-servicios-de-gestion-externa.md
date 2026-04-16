# 06-servicios-de-gestion-externa.md

## Finalidad

Este archivo define las responsabilidades del backend respecto de la gestión externa del expediente.

Su objetivo es dejar claro cómo se trata la derivación externa, su resultado y el eventual reingreso del expediente al circuito interno.

---

## Regla principal

La gestión externa es una salida controlada del expediente hacia un circuito u organismo externo, sin pérdida de identidad ni historia.

El backend debe poder:

- registrar la derivación
- reflejar el estado externo relevante
- registrar el resultado resumido
- procesar el eventual reingreso

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- registrar envío o derivación externa
- identificar el tipo de gestión externa
- registrar resultado resumido
- registrar reingreso posterior
- impactar sobre snapshot y bandejas

---

## Responsabilidades principales

### 1. Derivación externa
Debe permitir dejar trazado:

- que el expediente salió a gestión externa
- qué tipo de gestión externa corresponde
- cuándo ocurrió
- qué efecto tiene en la situación operativa

### 2. Resultado externo
Debe permitir reflejar:

- si la gestión sigue pendiente
- si está en trámite
- si finalizó
- si no produjo resultado
- si derivó en reingreso

### 3. Reingreso
Debe permitir:

- registrar reingreso desde gestión externa
- mantener la identidad del expediente
- conservar historia previa
- recalcular snapshot y bandeja correspondiente

---

## Relación con snapshot

Los efectos de la gestión externa deben proyectarse operativamente en snapshot, permitiendo conocer si el expediente está o estuvo en gestión externa, su tipo, su resultado resumido y si existió reingreso posterior.

---

## Qué no debe hacer

Este bloque no debe absorber:

- la lógica completa del sistema externo
- modelado detallado del organismo tercero
- reconstrucción técnica del circuito externo más allá de lo que impacta en el expediente

---

## Idea clave

La gestión externa debe tratarse como una dimensión operativa trazable del expediente, con impacto claro en estado, snapshot, bandeja y reingreso.