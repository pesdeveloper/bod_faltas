# 02-integracion-notificaciones.md

## Finalidad

Este archivo describe cómo debe entenderse la integración de notificaciones dentro del ecosistema del sistema de faltas.

No define todavía contratos técnicos detallados ni proveedores concretos.

Su objetivo es fijar:

- qué necesita el sistema de faltas del subsistema de notificaciones
- qué resultados espera recibir
- cómo impactan esos resultados sobre el expediente
- qué parte pertenece al dominio de faltas y qué parte a la integración

---

## Regla principal

El sistema de faltas no debe confundirse con un simple proveedor técnico de mensajería o envío.

El sistema de faltas:

- decide qué pieza debe notificarse
- registra el inicio del proceso de notificación
- refleja canal, estado, acuse y resultado
- reacciona al efecto operativo de la notificación sobre el expediente

Los mecanismos concretos de envío o diligenciamiento pueden apoyarse en integraciones externas o capacidades específicas por canal.

---

## Alcance dentro del ecosistema de faltas

Dentro del sistema de faltas debe modelarse, al menos:

- qué pieza se notifica
- por qué canal
- en qué estado general está la notificación
- si existe acuse
- cuál fue el resultado
- si requiere reintento
- si requiere decisión manual
- qué efecto produce sobre el expediente

---

## Qué queda fuera del núcleo de faltas

Quedan fuera del núcleo del dominio, salvo como integración o infraestructura:

- proveedores concretos de correo electrónico
- integraciones postales específicas
- servicios externos de mensajería
- detalle técnico de transporte o entrega electrónica
- lógica completa de agenda o despacho de notificador fuera del dominio propio
- detalles técnicos de colas, gateways o brokers

---

## Canales previstos

La integración debe contemplar, al menos, estos canales:

- notificación electrónica
- correo / carta documento
- notificador municipal
- comparecencia o retiro presencial
- otro canal formal que luego se defina

Cada canal puede requerir una forma distinta de evidenciar resultado o acuse.

---

## Regla de inicio de notificación

Cuando el expediente ya tiene una pieza notificable en condiciones, el sistema de faltas debe poder:

- registrar que la notificación se inicia
- indicar el canal
- dejar trazabilidad del intento o diligencia
- exponer el expediente en la bandeja de notificaciones

El inicio de la notificación no implica automáticamente resultado positivo ni cierre del trámite.

---

## Regla de acuse y resultado

La integración debe permitir que el sistema de faltas reciba o registre, según el canal:

- acuse pendiente
- acuse positivo
- acuse negativo
- vencimiento sin acuse suficiente
- reintento pendiente
- resultado final relevante

El expediente debe reaccionar a ese resultado sin perder trazabilidad de la notificación.

---

## Reglas por canal

### 1. Notificación electrónica
Debe permitir registrar:

- fecha de inicio
- dirección o medio utilizado, si corresponde
- estado general
- plazo de espera aplicable
- resultado final, cuando exista

El sistema debe contemplar la espera operativa correspondiente antes de determinar el efecto definitivo.

---

### 2. Correo / carta documento
Debe permitir registrar:

- despacho o inicio de envío
- estado general
- recepción o acuse externo
- resultado final cuando esté disponible

Mientras el resultado no sea suficiente, el expediente puede permanecer con acuse pendiente o situación equivalente.

---

### 3. Notificador municipal
Debe permitir registrar, al menos:

- diligencia asignada o iniciada
- fecha y hora
- resultado de la diligencia
- observaciones
- firma dibujada en dispositivo, si aplica
- evidencia complementaria
- acuse en acto, cuando corresponda

Este canal se relaciona directamente con la app móvil del notificador.

---

### 4. Comparecencia o retiro presencial
Debe permitir registrar:

- fecha del acto
- identidad o constancia del compareciente, si aplica
- pieza notificada
- resultado suficiente para el expediente

---

## Relación con la bandeja de notificaciones

La integración de notificaciones no reemplaza la bandeja de notificaciones.

La bandeja muestra expedientes en situación de notificación.  
La integración aporta o registra los resultados que permiten modificar esa situación.

---

## Relación con snapshot

El snapshot debe poder reflejar el efecto operativo resumido de la notificación, por ejemplo:

- expediente con piezas en notificación
- acuse pendiente
- resultado positivo o negativo relevante
- espera de plazo post notificación
- habilitación del siguiente paso

El detalle fino del trámite de notificación no vive solo en snapshot.

---

## Relación con la UI

La UI debe poder consumir esta integración o sus efectos para mostrar claramente:

- qué pieza se está notificando
- por qué canal
- en qué estado general se encuentra
- si existe acuse
- si el resultado fue positivo o negativo
- si requiere reintento
- si requiere decisión manual

---

## Relación con apps móviles

### App móvil del notificador
Debe consumir y/o alimentar esta integración para:

- recibir diligencias
- registrar entrega o intento
- capturar firma dibujada
- adjuntar evidencia
- registrar resultado

### Otras apps
Las demás apps pueden consumir resultados de notificación, pero no necesariamente gestionan el proceso completo de diligenciamiento.

---

## Regla de trazabilidad

Toda integración de notificación debe dejar trazabilidad suficiente sobre:

- pieza afectada
- canal
- intento o diligencia
- acuse
- resultado
- fechas relevantes
- observaciones
- reintentos

---

## Regla de desacople

El dominio del sistema de faltas no debe depender de detalles técnicos innecesarios del proveedor o mecanismo concreto de notificación.

La integración debe abstraer esos detalles y exponer al dominio solo la información necesaria para operar sobre el expediente.

---

## Idea clave

El sistema de faltas no “es” el sistema de mensajería.

El sistema de faltas decide, registra y utiliza el resultado de la notificación para modificar la situación operativa del expediente.

---

## Archivos relacionados

- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
- [Bandeja de notificaciones](../03-bandejas/07-bandeja-notificaciones.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Mapa backend](../04-backend/00-mapa-backend.md)
- [Mapa app notificador](../07-mobile-notificador/00-mapa-app-notificador.md)