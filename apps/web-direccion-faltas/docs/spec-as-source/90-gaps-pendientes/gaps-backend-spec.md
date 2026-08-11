# Gaps del backend spec — Resolución pendiente

> **Estado documental:** GAP_PENDING
>
> Este documento registra brechas identificadas en la spec del backend (`backend/api-faltas-core/docs/spec-as-source/`)
> que afectan el diseño o la implementación de la UX de `web-direccion-faltas`.
>
> **REGLA:** Ninguno de los gaps aquí documentados debe resolverse automáticamente.
> Cada gap requiere decisión humana explícita y modificación de la spec del backend por el equipo autorizado.
> Este documento es solo de registro; no modifica la spec del backend.

---

## GAP-UX-001: Routing de PENDIENTES_RESOLUCION_REDACCION no definido

### Descripción del gap

La bandeja `PENDIENTES_RESOLUCION_REDACCION` aparece en el catálogo de bandejas del backend
(`30-projections/snapshot-bandejas-acciones.md`) pero no tiene ninguna fila en la tabla de routing
que lleve a ella. No existe actualmente ninguna combinación de bloque/condición que produzca
`codBandeja = PENDIENTES_RESOLUCION_REDACCION`.

### Impacto en UX

- La entrada del sidebar existe (entrada #6 según `20-bandejas/bandejas-agrupamiento-routing.md`).
- Su contador siempre será 0 hasta que el backend defina el routing.
- El operador verá la entrada vacía permanentemente.

### Qué se necesita del backend para resolver este gap

1. Definir en `30-projections/snapshot-bandejas-acciones.md` cuándo y bajo qué condición
   un acta se enruta a `PENDIENTES_RESOLUCION_REDACCION`.
2. Definir qué `accionPendiente` corresponde a esta bandeja.
3. Posiblemente definir qué `tipoDocu` representan "piezas resolutorias" en el catálogo de documentos.

### Opciones identificadas (para decisión humana)

- Opción A: Agregar una fila de routing en bloque ANAL cuando existe un documento de tipo resolutorio
  en estado PENDIENTE_FIRMA (distinto al fallo) y antes de enviarlo a notificación.
- Opción B: Eliminar la bandeja del catálogo si no se implementará en el ciclo actual.
- Opción C: Dejarla en el catálogo como "reservada para uso futuro" sin routing activo.

**Estado actual en UX:** La entrada de sidebar se muestra con contador 0. No bloquea la implementación de otras bandejas.

---

## GAP-UX-002: Proceso nocturno para CMD-FALLO-005 (firmeza por vencimiento de plazo)

### Descripción del gap

CMD-FALLO-005 (`FirmezaCondenaService.vencerPlazoApelacion`) puede ser ejecutado:
- Manualmente por el operador desde `web-direccion-faltas` (botón contextual cuando `hoy > fhVtoApelacion`).
- Por un proceso automatizado nocturno del backend (no definido formalmente en la spec).

La spec del backend define el endpoint `POST /api/faltas/actas/{id}/firmeza/vencer-plazo-apelacion`
pero no define un proceso automatizado que lo invoque.

### Impacto en UX

- Si no existe el proceso nocturno, las actas en `PENDIENTES_FALLO` con plazo vencido
  permanecen en esa bandeja hasta que el operador presione el botón contextual "Declarar Condena Firme".
- El botón contextual es el guardrail actual.

### Qué se necesita del backend para resolver este gap

Decidir y documentar en `backend/api-faltas-core/docs/spec-as-source/90-roadmap/current-roadmap.md`
si se implementará un proceso nocturno (job/scheduler) que:
1. Consulte actas en bandeja `PENDIENTES_FALLO` con `fhVtoApelacion` vencido.
2. Ejecute CMD-FALLO-005 por cada una.
3. Registre los eventos `PLAVNC` y `CONFIR` correspondientes.

### Estado actual en UX

El botón contextual "Declarar Condena Firme" ya está contemplado en la spec UX
(`40-acta/visualizacion-acta-desktop-mobile.md` sección 5.2.1 y `50-acciones/acciones-botones-habilitacion.md`
sección 5.1). Es el guardrail funcional hasta que el proceso nocturno se defina e implemente.

---

## GAP-UX-003: PAGANT sin PAGRES — visibilidad en el panel del acta

### Descripción del gap

Cuando existe un evento `PAGANT` sin `PAGRES` asociado (pago aplicado a obligación anterior
pendiente de resolución administrativa), la spec del backend establece:

> "No existe bandeja ni acción pendiente dedicada a 'pago anterior pendiente de resolución':
> la detección y priorización operativa de un PAGANT sin PAGRES asociado se resuelve
> por consulta directa de eventos/movimientos."

### Impacto en UX

- La UX de `web-direccion-faltas` incluye un aviso informativo en la sección económica
  del detalle del acta (bloque 7, sección 8.3), basado en la consulta de eventos.
- No hay `accionPendiente` dedicada; es una consulta directa.

### Qué se necesita del backend para resolver

Un endpoint de consulta económica que devuelva si existen movimientos `PAGANT` sin `PAGRES`
para el acta, o bien que incluya este dato en el snapshot o en la proyección económica.

### Estado actual en UX

El aviso está documentado como "si existe evento PAGANT sin PAGRES" pero el mecanismo
de detección en el frontend depende de qué exponga el backend en la API económica.
Se resolverá cuando se defina el contrato HTTP de `FalActaEconomiaProyeccion` en `40-api/http-contracts.md`.

---

## Registro de gaps resueltos

*(Cuando un gap sea resuelto mediante modificación de la spec del backend y decisión humana,
moverlo a esta sección con la fecha de resolución y el número de decisión.)*

| ID | Descripción | Estado | Fecha resolución |
|----|-------------|--------|-----------------|
| — | — | — | — |
