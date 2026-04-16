# 00-mapa-backend.md

## Finalidad

Este archivo resume el mapa lógico del backend del ecosistema de faltas.

Ubica los bloques principales de responsabilidad y su relación con expediente, documentos, firmas, notificaciones, snapshot, bandejas, gestión externa y aplicaciones consumidoras.

No define endpoints, paquetes ni contratos detallados.

---

## Regla general

El backend debe construirse como un núcleo compartido del ecosistema de faltas.

No debe modelarse como un backend distinto por cada aplicación.

Las distintas superficies operativas consumen un backend común, con responsabilidades separadas por dominio y servicio.

---

## Bloques principales del backend

- **Gestión de expediente**: alta, lectura, trazabilidad y evolución del expediente.
- **Medidas y liberaciones**: tratamiento coordinado por el servicio de expediente, con impacto en documentación, snapshot, archivo y cierre.
- **Gestión documental**: producción, consulta y estado de documentos del expediente.
- **Gestión de firma**: integración con motor externo de firma y reacción a resultados.
- **Gestión de notificación**: proceso transversal de comunicación formal y sus efectos.
- **Gestión de snapshot**: construcción y exposición de proyección operativa actual.
- **Gestión de bandejas**: listados, filtros e indicadores operativos apoyados en snapshot.
- **Gestión externa**: derivaciones, resultados externos y reingreso con efecto material.
- **Resolución de storage documental**: determinación de backend, construcción de rutas y acceso desacoplado por `StorageKey`.

---

## Consumidores del backend

El backend será consumido, al menos, por:

- aplicación web de Dirección de Faltas
- aplicación móvil de inspectores
- aplicación móvil de notificador municipal
- aplicación móvil de liberaciones / entregas materiales

Todos estos clientes deben trabajar sobre el mismo núcleo de reglas y contratos.

---

## Relación con integraciones externas

El backend actúa como punto de integración con sistemas externos relevantes, por ejemplo:

- motor de firma
- mecanismos de notificación
- gestión externa
- backend o infraestructura de storage documental
- autenticación y roles

Estas integraciones no deben contaminar el núcleo del dominio con detalles técnicos innecesarios.

---

## Estilo arquitectónico recomendado

El backend debe organizarse por responsabilidades claras, privilegiando:

- servicios explícitos
- SQL controlado
- separación de lógica de dominio e integración
- trazabilidad
- bajo acoplamiento
- facilidad de lectura para implementación asistida

---

## Relación con persistencia

La persistencia debe sostener:

- trazabilidad del expediente
- producción documental
- notificaciones
- snapshot operativo
- medidas y liberaciones
- búsquedas y filtros de bandejas

El detalle se desarrolla en `08-persistencia-y-sql.md` y en la subcarpeta `08-persistencia-y-sql/`.

---

## Qué no define este archivo

Este archivo no define todavía:

- paquetes Java
- endpoints HTTP
- DTOs
- comandos y queries específicos
- workers detallados
- SQL por caso de uso
- seguridad exacta
- estructura física de base de datos

---

## Siguiente nivel de detalle sugerido

A partir de este mapa, los siguientes archivos naturales del bloque backend son:

- `01-servicios-de-expediente.md`
- `02-servicios-documentales.md`
- `03-servicios-de-firma.md`
- `04-servicios-de-notificacion.md`
- `05-servicios-de-snapshot.md`
- `06-servicios-de-gestion-externa.md`
- `07-servicios-de-bandejas.md`
- `08-persistencia-y-sql.md`
- subbloque `08-persistencia-y-sql/`
- `09-jobs-workers-y-procesos.md`

---

## Archivos relacionados

- [Stack tecnológico](../00-overview/01-stack-tecnologico.md)
- [Decisiones arquitectónicas](../00-overview/02-decisiones-arquitectonicas.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Persistencia y SQL](08-persistencia-y-sql.md)
- [Integración con motor de firma](../09-integraciones/01-integracion-motor-firma.md)