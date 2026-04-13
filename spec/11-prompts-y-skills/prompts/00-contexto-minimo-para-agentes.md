# 00-contexto-minimo-para-agentes.md

## Finalidad

Este archivo resume el contexto mínimo que un agente necesita para retomar rápidamente el trabajo del sistema de faltas sin cargar una ventana de contexto excesiva.

Debe usarse como punto de entrada corto y operativo.

---

## Qué es este sistema

El sistema de faltas se entiende como un **gestor documental orientado al expediente**.

La unidad principal de gestión es la **Acta / Expediente**.

Los documentos, firmas, notificaciones, acuses y efectos materiales modifican la situación operativa del expediente y determinan su visibilidad en bandejas.

---

## Organización general

El proyecto se organiza como un **repo multiproyecto** con:

- aplicación web para Dirección de Faltas
- aplicación móvil de inspectores
- aplicación móvil de notificador municipal
- aplicación móvil de liberaciones / entregas materiales
- backend compartido
- spec canónica en `spec/`

La carpeta `spec/` es la fuente principal de verdad.

---

## Reglas arquitectónicas más importantes

- `spec/` prevalece sobre chats, notas, borradores, prompts y tasklists
- la spec debe mantenerse chica, fragmentada y navegable
- la notificación se trata como proceso transversal único
- el snapshot operativo es derivado y regenerable
- archivo y cerrada no son equivalentes
- el motor de firma es externo al sistema de faltas
- en esta etapa existe integración sandbox para firma

---

## Bandejas vigentes

1. labradas
2. enriquecimiento / revisión inicial
3. análisis / presentaciones / pagos
4. pendientes de resolución / redacción
5. pendientes de fallo
6. pendientes de firma
7. notificaciones
8. con apelación
9. gestión externa
10. paralizadas
11. archivo
12. cerradas

---

## Qué leer primero

Orden sugerido de lectura mínima:

1. [Proyecto y objetivo](../../00-overview/00-proyecto-y-objetivo.md)
2. [Decisiones arquitectónicas](../../00-overview/02-decisiones-arquitectonicas.md)
3. [Mapa de dominio](../../01-dominio/00-mapa-dominio.md)
4. [Snapshot operativo](../../01-dominio/05-snapshot-operativo.md)
5. [Medidas y liberaciones](../../01-dominio/06-medidas-y-liberaciones.md)
6. [Regla del sistema como gestor documental](../../02-reglas-transversales/00-regla-sistema-como-gestor-documental.md)
7. [Reglas de notificación](../../02-reglas-transversales/02-reglas-de-notificacion.md)
8. [Reglas de cierre y archivo](../../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
9. [Índice maestro de bandejas](../../03-bandejas/00-indice-maestro-bandejas.md)

---

## Qué archivos ya existen y son especialmente importantes

- `spec/00-overview/00-proyecto-y-objetivo.md`
- `spec/00-overview/99-estado-actual-y-proximo-paso.md`
- `spec/00-overview/01-stack-tecnologico.md`
- `spec/00-overview/02-decisiones-arquitectonicas.md`
- `spec/01-dominio/00-mapa-dominio.md`
- `spec/11-prompts-y-skills/prompts/00-contexto-minimo-para-agentes.md`
- `spec/01-dominio/05-snapshot-operativo.md`
- `spec/01-dominio/06-medidas-y-liberaciones.md`
- `spec/01-dominio/07-catalogos-y-estados.md`
- `spec/02-reglas-transversales/00-regla-sistema-como-gestor-documental.md`
- `spec/02-reglas-transversales/02-reglas-de-notificacion.md`
- `spec/02-reglas-transversales/03-reglas-de-cierre-y-archivo.md`
- `spec/03-bandejas/00-indice-maestro-bandejas.md`
- `spec/03-bandejas/06-bandeja-pendientes-firma.md`
- `spec/03-bandejas/07-bandeja-notificaciones.md`
- `spec/03-bandejas/11-bandeja-archivo.md`
- `spec/09-integraciones/01-integracion-motor-firma.md`

---

## Próximo trabajo recomendado

Antes de seguir expandiendo la spec, revisar lo ya creado para detectar:

- contradicciones
- huecos
- nombres mejorables
- oportunidades de compactación
- referencias rotas

Luego continuar con:

- bandejas individuales faltantes
- reglas de firma
- mapa backend
- tasklists
- vistas por aplicación

---

## Regla de uso para agentes

No cargar todo el repo en contexto de una vez.

Leer solo:
- este archivo
- el estado actual
- los archivos directamente relacionados con la tarea

La estrategia correcta es contexto pequeño + navegación por referencias.