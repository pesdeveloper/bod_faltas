# [BACKEND] Implementación base Spring Boot + Spring JDBC

## Finalidad

Este documento fija las reglas base de implementación backend para el proyecto Faltas.

Su objetivo es evitar ambigüedades al momento de generar código con Cursor u otra herramienta, y asegurar coherencia con la spec del repo.

---

## Principios obligatorios

- El backend debe implementarse con **Spring Boot moderno**.
- El proyecto es **code-first**.
- No usar XML.
- No usar configuraciones mágicas o declarativas ajenas al estilo estándar de Spring Boot.
- Toda la configuración debe resolverse con:
  - Java
  - anotaciones
  - Java Config
  - `application.yml`

---

## Persistencia

- La persistencia se implementa con **Spring JDBC**.
- La API preferida de acceso a datos es **`JdbcClient`**.
- El acceso a datos debe hacerse con **SQL explícito**.
- Usar siempre **parámetros nombrados**.
- No usar ORM como mecanismo principal.
- No usar JPA/Hibernate.
- No usar `JpaRepository`, `CrudRepository` ni abstracciones equivalentes.
- No generar queries implícitas ni derivadas por convención.

---

## Estilo de SQL

- Escribir SQL nativo orientado a Informix.
- Preferir `Java Text Blocks` para SQL multilínea.
- Para consultas grandes o reutilizables, permitir archivo `.sql` externo en `resources/sql/...`.
- Mantener el SQL visible, revisable y auditable.
- No ocultar joins, filtros ni updates detrás de abstracciones automáticas.

---

## Mapeo

- Preferir **Java Records** para DTOs, comandos, queries y proyecciones.
- Usar `.query(MiRecord.class)` cuando el shape sea directo y claro.
- Para mapeos complejos, usar `RowMapper<T>`.
- Preferir alias SQL explícitos cuando mejoren la legibilidad o eviten ambigüedad.

---

## Repositories

- Los repositories son clases concretas.
- Su responsabilidad es:
  - ejecutar SQL
  - bindear parámetros
  - mapear resultados
  - persistir cambios
- No deben contener reglas de negocio del dominio.
- No deben reconstruir workflows completos.
- No deben tomar decisiones de negocio que correspondan a casos de uso o servicios de aplicación.

---

## Transaccionalidad

- La transaccionalidad se maneja en la capa de **caso de uso / servicio de aplicación**.
- Usar `@Transactional` allí.
- No usar `@Transactional` en controllers.
- No usar `@Transactional` en repositories salvo excepción muy justificada.

---

## Capas esperadas

La implementación backend debe tender a esta separación:

- `web` o `infrastructure/web`
- `application`
- `domain`
- `infrastructure/persistence/jdbc`
- `infrastructure/config`

---

## Criterio general

El código debe ser:

- explícito
- legible
- predecible
- fácil de auditar
- fácil de depurar
- coherente con la spec

Si una herramienta propone una solución más automática pero menos explícita, se debe priorizar siempre la alternativa más clara y controlable.

---

## Restricciones explícitas

### Prohibido

- usar XML
- usar `@Entity`, `@Table`, `@Id`
- usar Hibernate/JPA como base de persistencia
- usar `JpaRepository`, `CrudRepository`
- usar JPQL/HQL
- introducir magia que oculte el acceso real a datos

### Obligatorio

- usar Spring Boot moderno
- usar Java Config + anotaciones
- usar Spring JDBC
- usar `JdbcClient` como API preferida
- usar SQL explícito
- usar parámetros nombrados
- mantener consistencia con la spec del repo

---

## Regla final

Si una decisión de implementación contradice la spec, se prioriza la spec.