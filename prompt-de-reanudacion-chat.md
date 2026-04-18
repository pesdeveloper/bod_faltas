# Prompt de reanudación del chat — Proyecto Faltas

Retomamos el proyecto del sistema de faltas municipal de Malvinas Argentinas.

## Regla de arranque

Antes de continuar:

1. leer `prompt-recuperacion-faltas.md`
2. leer `estado-actual-y-proximo-paso.md`
3. tomar el repo y la spec como fuente principal de verdad
4. no rediseñar desde cero
5. continuar desde el próximo paso real pendiente

---

## Estado conceptual a asumir al retomar

El proyecto ya tiene una spec bastante madura.

No estamos en etapa de rediscutir el dominio general desde cero.

Ya quedaron bastante consolidados:

- enfoque `spec-as-source`
- núcleo orientado a `Acta`
- modelo event-driven
- `ActaEvento` append-only
- `ActaSnapshot` como proyección operativa
- flujo D1–D8
- D2 = Enriquecimiento
- procesos transversales:
  - P1 documental
  - P2 económico
  - P3 notificación
- bandejas como vistas operativas
- convención física `Fal`, `Num`, `Stor`
- bloque `spec/13-ddl` maduro
- bloque `spec/14-sql-operativo` maduro

---

## Decisiones nuevas ya cerradas

### 1. Implementación backend real

Para backend productivo futuro quedó definida una línea base:

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

Esto quedó documentado en:

- `spec/04-backend/10-implementacion-base-spring-jdbc.md`

### 2. Prototipo de validación

Antes de bajar a implementación productiva real, se definió construir un prototipo para validar con la Dirección de Faltas.

Ese prototipo quedó definido como:

- simulador funcional integral del negocio
- de punta a punta
- con datos mockeados
- con estado en memoria
- sin base real
- sin Informix
- sin integraciones reales
- con toda la lógica operativa visible
- con bandejas, eventos, documentos, firma simulada, notificaciones, reintentos, archivo, cerrado, etc.

Importante:

- el prototipo es **descartable**
- no es base evolutiva del backend real
- cuando termine su objetivo, debe eliminarse del repo por completo

Esto quedó documentado en:

- `spec/03-bandejas/99-prototipo-validacion-direccion.md`

### 3. Orquestación de agentes

También quedó formalizado el esquema de trabajo:

- Byte = arquitecto
- Cursor = implementa
- Gemini = auditor crítico
- la spec manda
- no se aceptan propuestas automáticamente sin evaluación arquitectónica

Esto quedó documentado en:

- `spec/00-overview/06-orquestacion-de-agentes-y-flujo-sdd.md`

---

## Punto real en el que quedó el trabajo

El siguiente paso ya no es seguir ampliando la spec en abstracto.

El siguiente paso real es bajar a código el **prototipo descartable** para validación con la Dirección.

La idea es que mañana se empiece con Cursor sobre:

- `backend/api-faltas-prototipo/`

con implementación simple, sin sobrearquitectura, priorizando velocidad, claridad y capacidad de validación.

---

## Criterio de implementación inmediata

Para el prototipo:

- priorizar simplicidad
- usar estado en memoria
- usar dataset mock precargado
- evitar infraestructura real
- exponer todas las acciones importantes del negocio
- permitir recorrer el sistema completo
- no preocuparse por reusabilidad futura
- no contaminar el backend real con decisiones del prototipo

---

## Qué debería encararse primero

Orden sugerido de arranque para el prototipo:

1. crear `backend/api-faltas-prototipo`
2. montar estructura mínima Spring Boot
3. definir store en memoria
4. cargar escenario mock inicial
5. exponer primeras bandejas
6. exponer detalle de acta
7. exponer historial de eventos
8. exponer acciones mock
9. agregar documentos simulados
10. agregar notificaciones y reintentos simulados

---

## Regla de continuidad

Al retomar:

- no volver a discutir si el prototipo debe existir
- no volver a discutir si usa base real
- no volver a discutir si debe evolucionar al backend real
- esas decisiones ya quedaron cerradas

Se debe avanzar directamente en:

- estructura del prototipo
- implementación con Cursor
- validación rápida del recorrido
- ajuste fino a partir de lo que vaya mostrando la demo

---

## Regla final

Si hay contradicción entre recuerdos vagos, chat y repo, priorizar:

1. `spec/`
2. `estado-actual-y-proximo-paso.md`
3. este archivo
4. el chat actual

No inventar memoria. Continuar desde el punto real documentado.