# Documentacion de trabajo - Bod-Faltas

Carpeta de artefactos de trabajo operativo: prompts de continuidad, cierres de slices, checklists de demo y estado de avance.

Los archivos de esta carpeta son mantenidos manualmente. No deben ser reescritos ni normalizados por agentes salvo instruccion explicita.

---

## Estado actual al 2026-06-01

- Demo funcional completa lista: SI
- Dataset demo: 42 actas canonicas
- MatrizSanidadActasDemoIT: 42 actas, 0 advertencias, 0 errores
- Build Angular: OK (solo warnings preexistentes de budget)
- Checklist principal: checklist guiado integral (22 casos, 42 actas)

---

## Documentos activos

### Validacion y presentacion

- [checklist-guiado-validacion-integral-faltas-2026-06-01.md](checklist-guiado-validacion-integral-faltas-2026-06-01.md)
  Checklist guiado de validacion integral. 22 casos cubiertos. Bandeja inicial por acta (segun dataset real post-reset). ACTA-0029 (condena firme + pago condena), ACTA-0114 (PARALIZADAS + reactivacion), ACTA-0019 (bloqueantes materiales), correo postal, guards negativos y plantilla para acompanamiento con asistente. Criterio de cierre.

- [guia-presentacion-demo-faltas-2026-06-01.md](guia-presentacion-demo-faltas-2026-06-01.md)
  Guia ejecutiva para presentacion de la demo. Guion sugerido de 20 a 30 minutos, actas recomendadas, que decir, que no prometer, cierre recomendado.

### Cierre y continuidad

- [cierre-restauracion-funcional-faltas-2026-06-01.md](cierre-restauracion-funcional-faltas-2026-06-01.md)
  Resumen ejecutivo del proceso de restauracion. Estado final, dataset canonico (42 actas), circuitos validados, bug PARALIZADAS corregido (Slice 15A/15B), reactivacion de PARALIZADAS implementada (Slice 17A), ACTA-0029 incorporada (Slice 19A), reglas UX estables.

- [prompt-continuidad-faltas-2026-06-01.md](prompt-continuidad-faltas-2026-06-01.md)
  Prompt de reanudacion para pegar en un chat o agente nuevo. Contiene estado actual, reglas UX fuertes, circuitos validados, actas clave, pendientes no criticos, comandos y forma de trabajo futura.

### Resultados de validacion

- [resultado-validacion-demo-funcional-faltas-2026-06-01.md](resultado-validacion-demo-funcional-faltas-2026-06-01.md)
  Resultado de la validacion funcional completa por API y tests (Slices 18A, 19A, 19B). 15/15 circuitos OK. Detalle de hallazgos H1 (ACTA-0029 resuelto) y H2 (informativo).

- [resultado-validacion-visual-ux-faltas-2026-06-01.md](resultado-validacion-visual-ux-faltas-2026-06-01.md)
  Resultado de la validacion visual UX (Slice 20A). Angular levantado OK, proxy OK, 11 bandejas OK. Validacion visual en browser pendiente de confirmacion manual por el equipo.

### Plantillas reutilizables

- [plantilla-prompt-slice.md](plantilla-prompt-slice.md)
  Plantilla para escribir el prompt de un nuevo slice.

- [plantilla-slice-cerrado.md](plantilla-slice-cerrado.md)
  Plantilla para documentar el cierre de un slice.

---

## Reglas de esta carpeta

- No reescribir artefactos de continuidad por iniciativa propia.
- No normalizar ni resumir archivos existentes.
- Agregar nuevos archivos con fecha en el nombre para trazabilidad.
- Usar Slice + fecha como convencion de nombres cuando aplique.

---

## Nota sobre archivos eliminados

Los siguientes archivos fueron eliminados en Slice 21B porque quedaron reemplazados por el checklist guiado integral y el prompt de continuidad actuales:

- checklist-demo-operativa-faltas-2026-06-01.md (reemplazado por checklist-guiado)
- checklist-validacion-integral-circuito-faltas.md (reemplazado por checklist-guiado)
- estado-actual-y-proximo-paso.md (reemplazado por prompt-continuidad)
- prompt-de-reanudacion-chat.md (reemplazado por prompt-continuidad)
- auditoria-dependencias-mocks-faltas.md (diagnostico historico, dataset superado)
- diagnostico-mocks-funcionales-faltas.md (diagnostico historico, dataset superado)
- mocks-funcionales-demo-faltas.md (estado anterior a 42 actas, superado)
- NOTAS.md (notas de diseno de BD historicas, sin relacion con demo actual)