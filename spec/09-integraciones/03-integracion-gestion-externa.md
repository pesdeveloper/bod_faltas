# Integración con gestión externa

## Finalidad

Este archivo define la integración entre el sistema de faltas y circuitos o sistemas externos que reciben, procesan o devuelven resultados con efecto material sobre el expediente.

---

## Alcance

Este bloque cubre:

- derivaciones externas
- recepción de resultados externos
- reingreso con efecto material
- correlación entre expediente interno y referencia externa
- tratamiento de estados o resultados externos relevantes

No cubre:

- implementación técnica detallada de conectores
- infraestructura concreta de mensajería
- contratos físicos finales

---

## Regla base

La integración externa no debe redefinir el dominio interno.

Su función es:

- derivar información o actuaciones fuera del sistema
- recibir resultados externos
- registrar efecto material sobre el expediente
- preservar trazabilidad y correlación

---

## Casos principales

La integración debe contemplar, según el caso:

- envío o derivación externa de actuaciones
- confirmación de recepción externa
- resultados externos con efecto sobre el expediente
- reingreso al sistema con actualización material
- incidencias o rechazos de integración
- reprocesos o reintentos, cuando corresponda

---

## Reglas de integración

- Toda integración externa debe correlacionarse con un expediente interno.
- La referencia externa no reemplaza la identidad interna del expediente.
- Los resultados externos relevantes deben impactar en trazabilidad, snapshot o estado operativo cuando corresponda.
- La integración debe poder registrar reingreso con efecto material.
- El detalle técnico del sistema externo no debe contaminar el núcleo del dominio.

---

## Persistencia esperada

La persistencia debe permitir, según corresponda:

- referencia al expediente interno
- referencia externa correlativa
- tipo de integración o gestión externa
- estado principal de la interacción
- fechas relevantes
- resultado principal
- incidencias o rechazos relevantes
- metadatos mínimos para reproceso o conciliación

---

## Relación con el expediente

La gestión externa puede:

- modificar el curso administrativo del expediente
- producir reingreso
- generar trazabilidad relevante
- impactar en snapshot y bandejas
- requerir documentos o notificaciones asociados

Pero no reemplaza la unidad principal de gestión del sistema.

---

## Relaciones clave

Este bloque se relaciona con:

- `Acta`
- `ActaEvento`
- `Documento`
- `Notificacion`
- snapshot operativo
- jobs, workers y procesos
- tablas auxiliares de integración

---

## Resultado esperado

Este bloque debe dejar resuelto que la gestión externa se integra de forma desacoplada, correlacionada y con efecto material trazable sobre el expediente.