# 07-servicios-de-bandejas.md

## Finalidad

Este archivo resume los servicios de backend vinculados a las bandejas operativas del sistema de faltas.

Su objetivo es identificar las responsabilidades del backend respecto de la construcción, consulta y filtrado de expedientes visibles en bandejas.

No define todavía endpoints, DTOs ni detalles técnicos de implementación.

---

## Regla principal

Las bandejas muestran **expedientes / actas**, no documentos sueltos.

El backend debe exponer las bandejas como vistas operativas construidas principalmente a partir de:

- snapshot operativo
- estado resumido del expediente
- bloqueos
- pendientes
- reglas de visibilidad
- filtros de trabajo

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- listar expedientes por bandeja
- aplicar filtros operativos
- ordenar resultados
- exponer badges e indicadores resumidos
- explicar por qué un expediente aparece en una bandeja
- reflejar acciones habilitadas según la situación operativa

---

## Responsabilidades principales

### 1. Consulta por bandeja
Debe permitir obtener los expedientes visibles en una bandeja determinada, por ejemplo:

- labradas
- enriquecimiento
- análisis
- pendientes de resolución / redacción
- pendientes de fallo
- pendientes de firma
- notificaciones
- con apelación
- gestión externa
- paralizadas
- archivo
- cerradas

---

### 2. Aplicación de filtros operativos
Debe permitir aplicar filtros relevantes para el trabajo diario, por ejemplo:

- estado operativo
- tipo de pieza notificable
- canal de notificación
- existencia de acuse
- plazo o vencimiento
- existencia de medidas activas
- existencia de pendientes materiales
- motivo de archivo
- destino externo
- operador o notificador, cuando corresponda

---

### 3. Exposición de indicadores resumidos
Debe permitir exponer, al menos:

- badges o etiquetas operativas
- bloqueos relevantes
- motivo resumido de archivo
- existencia de pendientes de firma
- existencia de piezas en notificación
- situación de medidas o pendientes materiales

Estos indicadores deben apoyarse principalmente en snapshot.

---

### 4. Habilitación de acciones
Debe permitir que la UI determine qué acciones están habilitadas para cada expediente según su situación operativa.

Ejemplos:

- pasar a otra bandeja
- generar una pieza
- registrar firma
- iniciar notificación
- registrar resultado
- enviar a archivo
- enviar a cerrada
- paralizar
- reingresar al circuito

La validación final de negocio no debe depender solo de la UI.

---

### 5. Explicación de visibilidad
Debe poder exponer, al menos de forma resumida, por qué un expediente aparece en la bandeja actual.

Esto es importante para evitar que la UI deba reconstruir por sí sola toda la lógica de negocio.

---

## Qué no debe hacer

Este bloque no debe:

- recalcular por su cuenta toda la historia del expediente en cada consulta
- reemplazar la fuente de verdad del dominio
- tratar las bandejas como workflows rígidos independientes del expediente
- duplicar en la UI la lógica central de visibilidad y bloqueos

Debe apoyarse principalmente en snapshot y en reglas operativas ya consolidadas.

---

## Relación con otros servicios

### Con servicios de expediente
El servicio de expediente coordina la situación general del caso, pero el servicio de bandejas expone la lectura operativa de múltiples expedientes en conjunto.

### Con servicios de snapshot
El servicio de bandejas depende principalmente de snapshot para construir listas rápidas, filtros y badges.

### Con servicios documentales
El estado documental impacta en la visibilidad de bandejas, pero la gestión documental sigue viviendo en su bloque propio.

### Con servicios de firma
Las firmas pendientes o completadas pueden modificar bandejas, pero la lógica de firma sigue en su bloque específico.

### Con servicios de notificación
La situación de notificación impacta fuertemente en bandejas, especialmente en notificaciones, fallo, archivo y gestión externa.

### Con servicios de gestión externa
La derivación y el resultado externo modifican la visibilidad del expediente en bandejas y sus acciones habilitadas.

---

## Operaciones conceptuales típicas

Este bloque debería poder sostener operaciones conceptuales como:

- listar expedientes de una bandeja
- contar expedientes por bandeja
- aplicar filtros sobre una bandeja
- obtener badges o indicadores resumidos
- obtener acciones habilitadas por expediente
- explicar motivo de visibilidad o bloqueo
- ordenar expedientes según criterios operativos

No implica que estas operaciones deban exponerse una a una como endpoints directos.

---

## Relación con snapshot

Snapshot es la principal base operativa de este bloque.

El servicio de bandejas debe consumir especialmente información resumida como:

- bandeja visible actual
- estado operativo general
- pendientes documentales
- pendientes de firma
- situación de notificación
- bloqueos de avance
- bloqueos de cierre
- medidas activas
- pendientes materiales
- condición de archivo o cerrada

---

## Relación con la UI

La UI de bandejas debe poder consumir este bloque para:

- mostrar listados
- aplicar filtros
- mostrar badges
- explicar bloqueos
- renderizar acciones habilitadas
- navegar al detalle del expediente

La UI no debería reconstruir reglas operativas centrales que ya existen en backend y snapshot.

---

## Idea clave

Las bandejas no son solo pantallas.

Son vistas operativas del expediente soportadas por backend, snapshot y reglas de negocio consistentes.

---

## Archivos relacionados

- [Mapa backend](00-mapa-backend.md)
- [Servicios de expediente](01-servicios-de-expediente.md)
- [Servicios de snapshot](05-servicios-de-snapshot.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)