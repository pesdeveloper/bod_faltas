# 07-catalogos-y-estados.md

## Finalidad

Este archivo resume los principales catálogos y grupos de estados que el sistema de faltas debe contemplar a nivel conceptual.

No define todavía tablas ni el listado físico definitivo de todos los valores.

---

## Regla general

Los catálogos y estados del sistema deben mantenerse:

- explícitos
- controlados
- consistentes con la operatoria real
- utilizables por reglas, snapshot, UI y backend

No deben quedar dispersos como textos libres cuando impactan en comportamiento del sistema.

---

## Grupos principales

### 1. Estados del expediente / acta
Describen la situación operativa resumida del expediente.

Ejemplos conceptuales:
- labrada
- en revisión
- en análisis
- pendiente de resolución
- pendiente de fallo
- pendiente de firma
- en notificación
- con apelación
- en gestión externa
- paralizada
- en archivo
- cerrada

Deben alinearse con el modelo vigente de bandejas.

---

### 2. Tipos de evento del expediente
Clasifican hechos relevantes registrados sobre la Acta.

Ejemplos conceptuales:
- generación documental
- firma
- notificación
- acuse
- presentación
- pago
- medida aplicada
- medida levantada
- liberación material
- archivo
- cierre
- paralización
- reanudación
- reingreso

No deben usarse para logs técnicos.

---

### 3. Tipos de documento
Clasifican las piezas documentales del sistema.

Ejemplos conceptuales:
- acta
- resolución
- fallo
- medida preventiva
- documento de notificación
- documento de liberación
- sentencia
- constancia
- otra pieza administrativa

Deben servir para reglas, firma, notificación, filtros y UI.

---

### 4. Estados del documento
Describen la situación documental de una pieza.

Ejemplos conceptuales:
- generado
- pendiente de firma
- firmado
- pendiente de notificación
- en notificación
- notificado
- observado
- anulado
- reemplazado

---

### 5. Canales de notificación
Describen por qué canal se realiza una notificación.

Canales previstos:
- electrónica
- correo / carta documento
- notificador municipal
- comparecencia / retiro presencial
- otro canal formal definido posteriormente

---

### 6. Estados de notificación
Describen la situación de una notificación.

Ejemplos conceptuales:
- pendiente de notificar
- en proceso
- acuse pendiente
- acuse positivo
- acuse negativo
- vencida sin acuse
- reintento pendiente
- decisión manual pendiente
- finalizada

---

### 7. Tipos de medida preventiva
Clasifican las medidas preventivas aplicables.

El catálogo definitivo dependerá de la operatoria real y del alcance jurídico-administrativo acordado.

---

### 8. Estados de medida preventiva
Describen la situación de una medida.

Ejemplos conceptuales:
- pendiente de generar
- generada
- pendiente de firma
- firmada
- activa
- en levantamiento
- levantada

---

### 9. Tipos de pendientes materiales
Clasifican situaciones materiales que impiden el cierre aunque no siempre sean medidas preventivas formales.

Ejemplos:
- liberación de rodado pendiente
- restitución de documentación pendiente
- devolución de licencia pendiente
- otra restitución material pendiente

---

### 10. Motivos de archivo
Explican por qué un expediente se encuentra en archivo.

Ejemplos conceptuales:
- medida preventiva activa
- liberación pendiente
- restitución pendiente
- bloqueo operativo de cierre
- otro motivo definido por catálogo

---

### 11. Motivos de cierre
Clasifican la causa por la cual un expediente pasa a cerrada.

Ejemplos conceptuales:
- pago cumplido sin bloqueos
- absolución sin bloqueos
- nulidad sin bloqueos
- otra causal válida de cierre

---

### 12. Resultados de gestión externa
Clasifican el resultado de una derivación externa.

Ejemplos conceptuales:
- sin novedad
- requiere reingreso
- requiere nuevo fallo
- pago externo
- cierre externo
- otro resultado relevante

---

### 13. Origen de actuación o evento
Clasifican desde dónde se originó una actuación relevante.

Ejemplos conceptuales:
- web Dirección de Faltas
- mobile inspectores
- mobile notificador
- mobile liberaciones
- integración externa
- proceso interno del sistema

---

## Regla de uso

No todos los catálogos se muestran completos al usuario final.

Pero deben existir de forma controlada para permitir:

- reglas consistentes
- filtros
- badges
- etiquetas
- explicaciones operativas
- trazabilidad

---

## Regla de evolución

Si se modifica un catálogo relevante, debe verificarse su impacto en:

- snapshot
- bandejas
- reglas transversales
- UI
- backend
- integraciones

---

## Archivos relacionados

- [Mapa de dominio](00-mapa-dominio.md)
- [Snapshot operativo](05-snapshot-operativo.md)
- [Medidas y liberaciones](06-medidas-y-liberaciones.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)