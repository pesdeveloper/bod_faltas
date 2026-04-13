# 04-servicios-de-notificacion.md

## Finalidad

Este archivo resume los servicios de backend vinculados al tratamiento de notificaciones dentro del sistema de faltas.

Su objetivo es identificar las responsabilidades del backend respecto del proceso de notificación como capacidad transversal del expediente.

No define todavía endpoints, DTOs ni contratos técnicos detallados.

---

## Regla principal

La notificación se trata como un proceso transversal único del sistema.

El backend debe ser capaz de:

- iniciar o reflejar notificaciones
- registrar diligencias, acuses y resultados
- coordinar reintentos o decisiones manuales
- producir el efecto operativo de la notificación sobre el expediente
- exponer el estado de notificación para bandejas, snapshot y UI

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- iniciar el tratamiento de notificación de una pieza
- registrar canal de notificación
- registrar acuse y resultado
- reflejar espera de plazo cuando corresponda
- sostener reintentos o decisiones manuales
- actualizar la situación operativa del expediente según el resultado

---

## Responsabilidades principales

### 1. Inicio de notificación
Debe permitir registrar que una pieza notificable del expediente entra al proceso de notificación.

Esto implica, al menos:

- identificar la pieza
- registrar el canal
- dejar trazabilidad del inicio
- reflejar su situación en notificaciones y snapshot

---

### 2. Gestión de estado de notificación
Debe permitir reflejar estados o situaciones equivalentes como:

- pendiente de notificar
- en proceso
- acuse pendiente
- acuse positivo
- acuse negativo
- vencida sin acuse suficiente
- reintento pendiente
- decisión manual pendiente
- finalizada

El listado definitivo deberá alinearse con el catálogo de estados de notificación.

---

### 3. Registro de acuse y resultado
Debe permitir registrar o reflejar, según el canal:

- acuse pendiente
- acuse positivo
- acuse negativo
- resultado suficiente para cerrar la etapa
- necesidad de reintento
- necesidad de reencauce

El expediente debe reaccionar a ese resultado sin perder trazabilidad del trámite.

---

### 4. Gestión de plazos
Debe permitir contemplar plazos operativos relevantes del proceso de notificación, por ejemplo:

- espera de hasta 7 días en notificación electrónica, según la regla aplicable
- espera de 5 días post fallo antes del siguiente destino natural

Estos plazos no deben obligar a crear microbandejas separadas, sino impactar en estado, filtros y habilitación del siguiente paso.

---

### 5. Gestión de reintentos y decisión manual
Debe permitir que, cuando una notificación no tenga resultado suficiente, el sistema pueda:

- reintentar
- mantener a decisión manual
- reencauzar el expediente
- derivar a archivo u otra salida válida

No todas las salidas deben automatizarse rígidamente.

---

### 6. Impacto sobre el expediente
Debe permitir que el resultado de la notificación modifique la situación del expediente cuando corresponda.

Ejemplos:

- habilitar continuidad
- abrir plazo
- derivar a fallo
- permitir gestión externa
- reencauzar a análisis
- mantener el expediente en notificación
- dejarlo a decisión manual

---

## Qué no debe hacer

Este bloque no debe absorber de forma innecesaria:

- detalle técnico del proveedor de correo
- detalle técnico del canal postal
- lógica propia del programa móvil del notificador
- lógica general del expediente fuera del efecto de la notificación
- cálculo completo de snapshot
- implementación técnica de brokers o gateways externos

Debe coordinar esas piezas sin contaminar el dominio.

---

## Relación con otros servicios

### Con servicios documentales
La notificación siempre se relaciona con una pieza documental del expediente, pero el documento sigue administrado por el bloque documental.

### Con servicios de expediente
El expediente cambia su situación operativa cuando cambia el resultado de una notificación, pero la coordinación general del caso vive en el bloque de expediente.

### Con servicios de snapshot
La notificación impacta en snapshot porque modifica bandeja, bloqueos, plazos y situación visible.

### Con integraciones
Los mecanismos concretos por canal se tratan como integraciones o capacidades específicas, no como lógica central del dominio.

---

## Operaciones conceptuales típicas

Este bloque debería poder sostener operaciones conceptuales como:

- iniciar notificación
- registrar diligencia
- registrar acuse
- registrar resultado
- marcar reintento pendiente
- dejar notificación a decisión manual
- consultar estado de notificación
- consultar piezas del expediente en notificación
- reflejar vencimiento de plazo relevante

No implica que estas operaciones deban exponerse una a una como endpoints directos.

---

## Relación con la bandeja de notificaciones

Este bloque es el principal sostén backend de:

- bandeja de notificaciones
- filtros por canal
- filtros por estado
- acuses
- reintentos
- decisiones manuales
- plazos post notificación

La bandeja no debería tener que reconstruir por sí sola la lógica de notificación.

---

## Relación con la UI

La UI debe poder usar este bloque para:

- ver qué expediente tiene piezas en notificación
- identificar qué se notifica
- saber por qué canal
- ver el estado general
- registrar o reflejar acuse
- registrar resultado
- gestionar reintentos
- entender el próximo efecto esperado sobre el expediente

---

## Idea clave

El backend no trata la notificación como simple envío de mensajes.

La trata como una capacidad del expediente con efecto real sobre su situación operativa.

---

## Archivos relacionados

- [Mapa backend](00-mapa-backend.md)
- [Servicios de expediente](01-servicios-de-expediente.md)
- [Servicios documentales](02-servicios-documentales.md)
- [Bandeja de notificaciones](../03-bandejas/07-bandeja-notificaciones.md)
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
- [Integración de notificaciones](../09-integraciones/02-integracion-notificaciones.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)