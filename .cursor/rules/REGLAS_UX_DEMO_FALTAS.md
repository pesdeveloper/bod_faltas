---
description: UX estable aceptada para la demo Angular de Dirección de Faltas. Define lateral, filtros operativos, operación de notificaciones, panel derecho, redondeos y reglas visuales que no deben modificarse salvo pedido explícito.
globs:
  - apps/web-direccion-faltas/angular/src/app/features/demo/**
  - apps/web-direccion-faltas/angular/src/app/core/models/prototipo-faltas.models.ts
  - apps/web-direccion-faltas/angular/src/app/core/services/prototipo-faltas-api.service.ts
alwaysApply: false
---

# UX estable aceptada — Dirección de Faltas

Esta regla debe aplicarse cuando se modifique la demo Angular de Dirección de Faltas.

No modificar esta UX salvo pedido explícito.

## Lateral

- Mostrar solo bandejas principales.
- No mostrar sub-bandejas en el lateral.
- Cada bandeja tiene ícono, label y badge de cantidad.
- Los labels pueden ocupar hasta 2 líneas.
- La densidad vertical debe ser compacta.
- Los badges deben ser visibles y legibles.
- El lateral debe conservar buena legibilidad en modo expandido.
- En modo colapsado deben verse solo íconos, sin textos cortados.

### Bandejas laterales UX vigentes

El lateral NO debe reflejar 1 a 1 las bandejas backend.

El lateral debe mostrar únicamente estas bandejas UX:

1. Actas en enriquecimiento
2. Análisis / presentaciones / pagos
3. Pendientes de resolución / redacción
4. Pendientes de fallo
5. Pendientes de firma
6. Notificaciones
7. Con apelación
8. Gestión externa
9. Paralizadas
10. Archivo
11. Cerradas

Estos códigos backend NO deben mostrarse como entradas laterales independientes:

- LABRADAS
- PENDIENTE_PREPARACION_DOCUMENTAL
- PENDIENTE_NOTIFICACION
- EN_NOTIFICACION

Mapping visual obligatorio:

- LABRADAS -> Actas en enriquecimiento
- PENDIENTE_PREPARACION_DOCUMENTAL -> Actas en enriquecimiento
- PENDIENTE_NOTIFICACION -> Notificaciones
- EN_NOTIFICACION -> Notificaciones

La granularidad interna debe ir por Filtro operativo, no por más entradas laterales.

## Filtro operativo

- Las sub-bandejas/filtros operativos se muestran en el área principal mediante select.
- El select debe llamarse “Filtro operativo”.
- Debe incluir opción “Todos”.
- Debe tener una X compacta para limpiar y volver a “Todos”.
- No usar chips grandes para estos filtros.
- No volver a mostrar sub-bandejas en el lateral.

## Redondeo visual

- El redondeo de menú, badges y botones debe ser consistente con inputs/selects.
- No usar estilo pill/cápsula.
- Mantener un estilo sobrio de backoffice.

## Operación de notificaciones

- Correo postal es opción operativa real del menú.
- Debe estar separada por divider bajo “Operación de notificaciones”.
- No debe aparecer bajo “Herramientas demo”.
- Notificador municipal queda como acceso demo secundario/temporal.
- No eliminar Notificador municipal todavía salvo pedido explícito.
- No darle a Notificador municipal el mismo peso visual que Correo postal.

## Panel derecho

- Si no hay acta seleccionada, puede mostrar resumen simple de bandeja.
- Si hay acta seleccionada, debe mostrar solo el detalle del acta.
- No usar accordion/resumen colapsable por ahora.
- No superponer resumen de bandeja sobre detalle del acta.

## Mojibake

- Evitar mojibake en Angular.
- No deben aparecer caracteres corruptos como:
  - Ã
  - Â
  - �
  - âº
- Textos esperados:
  - Número de acta
  - Seleccioná un acta para ver su detalle operativo.
  - notificación
  - resolución
  - análisis
  - jurídico
  - situación

## Regla fuerte

Si un cambio toca el shell visual, lateral, filtros operativos o panel derecho, preservar esta UX estable.
Ante duda, hacer el cambio mínimo y no rediseñar el shell completo.