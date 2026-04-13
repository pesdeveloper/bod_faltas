# 00-proyecto-y-objetivo.md

## Finalidad

Este archivo fija el propósito general del ecosistema del sistema de faltas y el criterio con el que debe leerse su spec canónica.

---

## Qué es este sistema

El sistema de faltas debe entenderse como un **gestor documental orientado al expediente**.

La unidad principal de gestión es el **expediente / acta**.

Los documentos, sus firmas, sus notificaciones, sus acuses, sus actuaciones y sus efectos materiales modifican el estado operativo del expediente y determinan su visibilidad en bandejas.

---

## Objetivo general

Soportar la gestión integral de faltas municipales desde el labrado inicial del acta hasta su resolución, notificación, gestión externa, archivo o cierre, incluyendo:

- documentos
- firmas
- notificaciones
- medidas preventivas
- liberaciones
- pagos
- apelaciones
- reingresos
- snapshot operativo
- trabajo por bandejas

---

## Alcance del ecosistema

El sistema no se limita a una sola aplicación.

Se lo concibe como un ecosistema multiproyecto compuesto, al menos, por:

- aplicación web interna para Dirección de Faltas
- aplicación móvil para inspectores
- aplicación móvil para notificador municipal
- aplicación móvil para entrega material y liberaciones
- backend y procesos compartidos
- integraciones externas relevantes

---

## Núcleo común

El núcleo común del sistema está compuesto por:

- acta / expediente
- evento de expediente
- documento
- notificación
- snapshot operativo
- medidas y liberaciones
- bandejas
- reglas transversales
- integraciones externas

---

## Enfoque spec-as-source

La estrategia del proyecto es **spec-as-source**.

La spec canónica debe servir como base para:

- arquitectura
- dominio
- reglas
- vistas
- contratos
- tasklists
- generación asistida de artefactos

No se busca documentación decorativa.  
Se busca una base de conocimiento pequeña, precisa y navegable.

---

## Principios ya adoptados

- el sistema se entiende como gestor documental orientado al expediente
- el expediente / acta es la unidad principal de gestión
- las bandejas muestran situación operativa actual del expediente
- la notificación se trata como proceso transversal único
- el snapshot operativo es derivado y regenerable
- archivo y cierre no son equivalentes
- el motor de firma es externo al sistema de faltas
- la spec debe mantenerse chica, fragmentada y utilizable por agentes

---

## Regla de partición

La spec no debe crecer como un documento único grande.

Debe fragmentarse en archivos chicos, específicos y enlazables.

Regla práctica:
- un archivo = una responsabilidad principal
- un archivo = una pregunta principal

---

## Regla de vigencia

La verdad vigente del proyecto vive en:

- `spec/`

Los borradores, notas y materiales históricos no deben competir con ella como fuente principal.

---

## Archivos relacionados

- [Stack tecnológico](01-stack-tecnologico.md)
- [Decisiones arquitectónicas](02-decisiones-arquitectonicas.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Regla del sistema como gestor documental](../02-reglas-transversales/00-regla-sistema-como-gestor-documental.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)