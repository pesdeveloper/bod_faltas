# Estado actual y próximo paso

## Estado actual consolidado

El proyecto se encuentra en un punto de madurez alta a nivel de spec.

Ya quedaron bastante consolidados:

- enfoque `spec-as-source`
- dominio orientado a `Acta`
- modelo event-driven
- `ActaEvento` append-only
- `ActaSnapshot` como proyección operativa principal
- flujo D1–D8
- D2 = Enriquecimiento
- procesos transversales P1 documental, P2 económico y P3 notificación
- bandejas como vistas operativas
- convención física `Fal`, `Num`, `Stor`
- reglas de naming auxiliar corto
- GIS municipal `geo_gmat_*` con EPSG:22195
- shape de domicilios ya bastante cerrado
- `spec/13-ddl` madura
- `spec/14-sql-operativo` madura

En este punto, el proyecto ya no necesita más expansión general de spec como prioridad inmediata.

---

## Decisiones cerradas recientemente

### Backend productivo futuro

Se cerró la línea base de implementación backend real:

- Spring Boot moderno
- code-first
- sin XML
- Java Config + anotaciones
- `application.yml`
- Spring JDBC
- `JdbcClient` como API preferida
- SQL explícito
- parámetros nombrados
- sin JPA/Hibernate
- sin `JpaRepository` / `CrudRepository`

Archivo asociado:

- `spec/04-backend/10-implementacion-base-spring-jdbc.md`

### Orquestación de trabajo con IA

Se formalizó el flujo SDD / spec-driven con roles:

- Byte = arquitecto
- Cursor = implementador
- Gemini = auditor crítico

Archivo asociado:

- `spec/00-overview/06-orquestacion-de-agentes-y-flujo-sdd.md`

### Prototipo de validación

Se definió construir un prototipo previo a la implementación productiva real.

Características cerradas del prototipo:

- simulador funcional integral del negocio
- de punta a punta
- con datos mockeados
- con estado en memoria
- sin base real
- sin Informix
- sin integraciones reales
- con bandejas, eventos, documentos simulados, firma simulada, notificaciones, reintentos, archivo, cerrado y navegación completa del circuito

Regla importante:

- este prototipo es **descartable**
- no debe considerarse base evolutiva del backend real
- una vez validado con la Dirección, debe eliminarse por completo del repo

Archivo asociado:

- `spec/03-bandejas/99-prototipo-validacion-direccion.md`

---

## Lectura correcta del momento actual

El siguiente paso lógico ya no es profundizar más la spec en abstracto.

El siguiente paso correcto es:

- bajar a código el prototipo
- validarlo con la Dirección
- ajustar lógica operativa y nombres si hiciera falta
- recién después pasar a implementación productiva real

---

## Próximo paso concreto

### Crear el backend del prototipo

Ubicación prevista:

- `backend/api-faltas-prototipo/`

Naturaleza de este módulo:

- backend simple
- Spring Boot liviano
- estado en memoria
- dataset mock precargado
- sin persistencia real
- sin infraestructura definitiva
- orientado solo a validar negocio

### Objetivo inmediato del módulo

Permitir:

- listar bandejas
- ver actas mock
- abrir detalle de acta
- ver historial de eventos
- ver documentos simulados
- ver notificaciones simuladas
- ejecutar acciones mock
- recorrer el circuito completo de punta a punta

---

## Orden sugerido de implementación

1. crear estructura mínima de `backend/api-faltas-prototipo`
2. definir modelos mock básicos
3. definir store en memoria
4. cargar escenario inicial con 8 a 12 actas mock
5. exponer endpoints mínimos de salud y reset
6. exponer bandejas
7. exponer detalle de acta
8. exponer historial de eventos
9. exponer acciones operativas mock
10. agregar lógica documental simulada
11. agregar lógica de notificación y reintentos simulados

---

## Criterios para la implementación del prototipo

- priorizar simplicidad
- evitar sobrearquitectura
- evitar infraestructura real
- evitar decisiones de largo plazo innecesarias
- exponer toda la lógica visible del negocio
- permitir iteración rápida con Cursor
- hacer algo descartable, claro y útil para validación

---

## Qué no hacer ahora

- no implementar Informix todavía
- no conectar persistencia real
- no armar workers reales
- no meter seguridad real
- no sobrediseñar capas pensando en producción
- no reutilizar este prototipo como backend real futuro

---

## Resultado esperado del próximo tramo

Quedar con un primer backend de prototipo capaz de:

- levantar rápido
- cargar escenarios mock
- exponer bandejas y detalle
- registrar acciones simuladas
- mostrar eventos, documentos y notificaciones
- servir de base para una demo funcional con la Dirección

---

## Regla final

Hasta validar este prototipo con la Dirección, el foco no es producción.

El foco es:

- validar lógica de negocio
- validar recorrido operativo
- validar nombres, bandejas y acciones
- corregir rápido
- recién después bajar a implementación productiva seria