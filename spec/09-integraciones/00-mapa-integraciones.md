# 00-mapa-integraciones.md

## Finalidad

Este archivo resume el mapa general de integraciones externas relevantes para el ecosistema del sistema de faltas.

Su objetivo es identificar:

- qué integraciones existen o se prevén
- qué papel cumplen
- qué parte pertenece al dominio de faltas
- qué parte debe quedar desacoplada como integración externa

No define todavía contratos técnicos detallados ni proveedores concretos.

---

## Regla general

El sistema de faltas debe integrarse con servicios externos sin contaminar el núcleo del dominio con detalles técnicos innecesarios.

La lógica del expediente debe seguir siendo propia del sistema de faltas.

Las integraciones deben aportar:

- resultados
- estados
- evidencias
- confirmaciones
- datos auxiliares

que impactan sobre el expediente, sus documentos y su situación operativa.

---

## Integraciones principales previstas

### 1. Motor de firma
Sistema externo transversal encargado del proceso real de firma documental.

Dentro del ecosistema de faltas solo se modela:

- la integración
- el estado de firma esperado
- el efecto de la firma sobre documento y expediente
- el modo sandbox / testing para etapas iniciales

Archivo relacionado:
- [Integración con motor de firma](01-integracion-motor-firma.md)

---

### 2. Notificaciones
Conjunto de integraciones o mecanismos que permiten materializar la notificación de piezas del expediente por distintos canales.

Dentro del ecosistema de faltas se modela:

- qué pieza se notifica
- por qué canal
- estado, acuse y resultado
- efecto operativo sobre el expediente

Archivo relacionado:
- [Integración de notificaciones](02-integracion-notificaciones.md)

---

### 3. Gestión externa
Integración o interacción con circuitos externos al flujo interno principal del sistema de faltas.

Ejemplos:
- apremios
- Juzgado de Paz
- otra gestión externa formal

Dentro del ecosistema de faltas se modela:

- derivación externa
- trazabilidad
- resultados externos
- reingreso con efecto material

Archivo relacionado:
- [Integración de gestión externa](03-integracion-gestion-externa.md)

---

### 4. Storage documental
Mecanismo de almacenamiento físico de documentos del sistema.

Debe mantenerse desacoplado del dominio mediante una abstracción de acceso documental.

Dentro del ecosistema de faltas se modela:

- metadata documental
- relación entre expediente y documento
- localización lógica
- acceso controlado desde backend

Archivo relacionado:
- [Integración de storage documental](04-integracion-storage-documental.md)

---

### 5. Autenticación y roles
Integración con el sistema de autenticación, identidad y autorización del ecosistema institucional.

Dentro del sistema de faltas se modela:

- perfiles y permisos necesarios
- impacto operativo de roles
- asociación entre identidad y acciones del expediente

Archivo relacionado:
- [Integración de autenticación y roles](05-integracion-autenticacion-y-roles.md)

---

## Relación con el dominio

Las integraciones no reemplazan el dominio de faltas.

El dominio sigue definiendo:

- expediente
- documento
- notificación
- snapshot
- medidas y liberaciones
- bandejas
- reglas operativas

Las integraciones solo aportan capacidades externas o resultados que impactan en esas piezas.

---

## Relación con backend

El backend de faltas debe actuar como capa de integración y orquestación entre:

- el núcleo del dominio
- las aplicaciones consumidoras
- los servicios externos

No debe mezclar el detalle técnico de cada integración con las reglas centrales del expediente.

---

## Relación con las apps

Las aplicaciones consumidoras pueden interactuar con integraciones de forma directa o indirecta según el caso.

Ejemplos:

- la web interna puede disparar solicitudes de firma o consultar resultados
- la app del notificador puede alimentar la integración de notificaciones
- la app de liberaciones puede validar documentos o registrar actos materiales
- la app de inspectores puede generar información que luego impacta en documentos o notificaciones

En todos los casos, el expediente sigue siendo el centro.

---

## Regla de desacople

Cada integración debe modelarse de forma que el sistema de faltas pueda:

- cambiar proveedor o mecanismo técnico
- mantener su dominio estable
- soportar sandbox o simulación cuando corresponda
- minimizar el impacto de cambios externos sobre la lógica propia del expediente

---

## Idea clave

El ecosistema de faltas no debe absorber como lógica interna lo que pertenece a sistemas externos.

Debe integrarse con ellos de forma explícita, controlada y trazable.

---

## Archivos relacionados

- [Mapa backend](../04-backend/00-mapa-backend.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Integración con motor de firma](01-integracion-motor-firma.md)
- [Integración de notificaciones](02-integracion-notificaciones.md)
- [Integración de gestión externa](03-integracion-gestion-externa.md)
- [Integración de storage documental](04-integracion-storage-documental.md)
- [Integración de autenticación y roles](05-integracion-autenticacion-y-roles.md)