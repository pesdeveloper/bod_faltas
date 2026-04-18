# [PROTOTIPO] Validación operativa integral con Dirección de Faltas

## Finalidad

Este prototipo tiene como objetivo validar con usuarios reales de la Dirección de Faltas la lógica operativa integral del sistema antes de bajar a implementación productiva real.

No es una maqueta estática ni una demo visual aislada.

Es un **simulador funcional del negocio**, navegable de punta a punta, con datos mock, capaz de recorrer el circuito completo de trabajo.

---

## Objetivo principal

Validar que:

- las bandejas tienen sentido operativo
- las acciones visibles son correctas
- los cambios de estado se entienden
- los eventos del caso están bien representados
- los documentos que aparecen son los esperados
- la lógica de notificación y reintentos tiene sentido
- la secuencia general del circuito cierra con la práctica real
- la navegación resulta natural para la Dirección
- la lógica de negocio general fue correctamente interpretada

---

## Naturaleza del prototipo

Este prototipo **no** es la implementación productiva definitiva.

Es un artefacto de validación funcional y operativa.

Debe entenderse como un **simulador funcional integral del negocio**, capaz de:

- exponer estado mock actual
- ejecutar acciones operativas simuladas
- registrar eventos simulados
- reflejar cambios de bandeja, estado, documentos y notificaciones
- permitir recorrer el circuito completo de punta a punta

Una vez validado el modelo con la Dirección de Faltas, este prototipo debe poder eliminarse por completo del repo.

No debe considerarse base evolutiva del backend productivo posterior.

---

## Alcance

El prototipo debe simular el sistema completo de punta a punta, con datos mockeados.

Debe incluir:

- bandejas
- transiciones
- eventos
- documentos simulados
- firma simulada
- notificaciones
- reintentos
- resoluciones / fallos
- archivo
- cerrado
- cambios de situación
- reingresos por evento cuando corresponda
- navegación completa del circuito

No es necesario implementar todavía:

- formularios reales completos
- persistencia productiva
- base real
- Informix
- motor de firma real
- generación documental real
- integraciones reales
- seguridad final
- infraestructura definitiva

---

## Decisión técnica para esta etapa

Para esta etapa se prioriza la implementación más simple posible.

### Opción elegida

- **estado en memoria**
- **dataset mock precargado al iniciar**
- **sin base de datos real**
- **sin Informix**
- **sin integraciones reales**

### Criterio

El objetivo del prototipo no es validar infraestructura ni persistencia productiva.

El objetivo es validar la lógica operativa integral del sistema con la Dirección de Faltas.

Por lo tanto, se prioriza una implementación:

- simple
- rápida de iterar
- fácil de ajustar
- desacoplada de infraestructura definitiva
- fácil de descartar cuando termine su objetivo

---

## Actas mock

Se debe contar con un conjunto de actas mock representativas.

No deben ser solo registros estáticos; deben poder evolucionar dentro del prototipo según acciones simuladas.

### Cantidad sugerida

Preparar entre **8 y 12 actas mock**.

### Casos sugeridos

Incluir al menos:

- acta recién labrada
- acta en enriquecimiento
- acta con faltantes tolerables
- acta lista para notificación
- acta con notificación pendiente
- acta con reintento de notificación
- acta con documento pendiente de firma
- acta con documento marcado como firmado
- acta con presentación espontánea
- acta con pago confirmado
- acta archivada
- acta con medida preventiva o situación especial

---

## Comportamiento esperado

El prototipo debe permitir:

- navegar bandejas
- abrir detalle de acta
- visualizar historial de eventos
- visualizar documentos simulados
- visualizar notificaciones simuladas
- ejecutar acciones operativas mock
- recorrer el circuito completo
- reflejar cambios de estado, bloque, situación y bandeja

Aunque no existan todavía formularios reales completos, deben estar presentes todos los botones de acción relevantes y todo el comportamiento lógico esperado.

---

## Historial de eventos

Cada acta mock debe exponer una vista simple de eventos registrados.

El prototipo debe permitir:

- ver los eventos relevantes de la acta en orden
- entender qué pasó y por qué la acta llegó a su situación actual
- relacionar eventos con cambios de estado, documentos, firma simulada y notificaciones

No se busca auditoría técnica exhaustiva.

Sí se busca trazabilidad operativa clara y entendible.

---

## Documentos simulados

Cuando una acción del circuito requiera generar un documento:

- no hace falta documento real
- no hace falta contenido real
- no hace falta PDF final

Sí debe generarse una representación simulada que permita ver:

- tipo de documento
- nombre asignado
- acta asociada
- estado documental
- si requiere firma o no
- impacto operativo en el circuito

El nombre visible puede construirse en forma simple a partir del tipo documental y la acta.

---

## Firma simulada

El proceso de firma real no forma parte todavía del prototipo.

Todo documento que requiera firma debe permitir una acción de prueba, por ejemplo:

- `Marcar como firmado (test)`

Esta acción debe producir el mismo efecto operativo esperado que tendría la firma real sobre:

- estado del documento
- habilitación del siguiente paso
- avance del circuito
- cambio de bandeja o situación

---

## Notificaciones simuladas

El prototipo debe simular la lógica operativa completa de notificación, incluyendo:

- preparación
- intentos
- resultados
- reintentos
- acuses
- vencimientos
- impacto operativo sobre la acta
- cambio de bandeja o situación

No se requiere envío real ni integración con canales reales.

Sí se requiere que la lógica visible del proceso quede completamente simulada y navegable.

---

## Formularios

No es obligatorio implementar formularios reales completos en esta etapa.

Cuando una acción requiera entrada de datos, puede resolverse inicialmente con:

- botón directo con resultado mock
- modal mínima
- selección predefinida de escenario
- datos de prueba precargados

El foco está en validar la lógica operativa y el recorrido del sistema, no en cerrar todavía la UX final de captura.

---

## Resultado esperado

Luego de mostrar este prototipo, la Dirección debería poder validar con claridad:

- si el circuito completo cierra
- si las bandejas son correctas
- si las acciones visibles son adecuadas
- si los eventos del expediente están bien representados
- si los documentos visibles son los correctos
- si la lógica de notificación y reintentos tiene sentido
- si la secuencia operativa resulta natural
- si los nombres usados son correctos
- si la lógica de negocio general fue correctamente interpretada

Solo después de esta validación se recomienda bajar a implementación productiva real.

---

## Regla final

Este prototipo debe priorizar siempre:

- simplicidad de implementación
- claridad operativa
- capacidad de validación con usuarios reales
- facilidad para iterar cambios rápidos
- mínima complejidad técnica necesaria

Una vez cumplido su objetivo, debe poder eliminarse por completo del repo sin arrastrar decisiones técnicas innecesarias a la implementación productiva posterior.