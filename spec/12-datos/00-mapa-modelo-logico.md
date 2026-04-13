# Mapa del modelo lógico de datos

## Finalidad del bloque

Este bloque define la bajada lógica del dominio del sistema a estructuras de datos.

Su objetivo es traducir las piezas conceptuales definidas en `spec/01-dominio/` a un modelo lógico consistente, compacto y apto para ser llevado posteriormente a persistencia, SQL, repositorios, queries y servicios de backend.

---

## Alcance

Este bloque define, a nivel lógico:

- entidades persistibles principales
- relaciones entre entidades
- criterios de separación entre núcleo, anexos, satélites y derivados
- reglas generales de identificación y referencia
- límites entre datos transaccionales, datos documentales, datos derivados y catálogos

No define todavía:

- SQL físico final
- índices físicos completos
- scripts de migración
- detalles de performance
- estrategias concretas de acceso a datos
- implementación de repositorios o servicios

---

## Relación con otros bloques

Este bloque se apoya principalmente en:

- `spec/01-dominio/`, como fuente conceptual del negocio
- `spec/02-reglas-transversales/`, como fuente de reglas operativas comunes
- `spec/03-bandejas/`, como validación operativa de lectura y evolución del expediente
- `spec/04-backend/`, como destino posterior de implementación
- `spec/09-integraciones/`, para los puntos donde existan referencias o resultados externos que deban persistirse

Este bloque no redefine el dominio: lo aterriza a estructuras de datos.

---

## Principio rector

La unidad principal de gestión sigue siendo el Acta / Expediente.

Por lo tanto, el modelo lógico debe organizarse alrededor de esa unidad central y de sus piezas asociadas:

- trazabilidad
- documentos
- notificaciones
- medidas y liberaciones
- catálogos
- proyecciones derivadas

---

## Criterios de modelado

El modelo lógico debe respetar estos criterios:

- compactación y claridad antes que sobre-diseño
- separación explícita entre dato principal y dato derivado
- trazabilidad conservada sin duplicación innecesaria
- soporte para operación documental orientada al expediente
- soporte para evolución no estrictamente lineal del caso
- posibilidad de reconstruir vistas operativas a partir del núcleo persistido
- integración con servicios externos sin absorberlos dentro del núcleo del modelo

---

## Estructura del bloque

Este bloque se organiza en los siguientes archivos:

- `01-convenciones-generales.md`
- `02-modelo-logico-acta.md`
- `03-modelo-logico-acta-evento.md`
- `04-modelo-logico-documento.md`
- `05-modelo-logico-notificacion.md`
- `06-modelo-logico-snapshot.md`
- `07-modelo-logico-medidas-y-liberaciones.md`
- `08-modelo-logico-catalogos-y-maestros.md`

Cada archivo desarrolla un sector lógico del modelo sin reemplazar la visión de conjunto del dominio.

---

## Orden de lectura sugerido

Para entender el bloque en forma progresiva, se recomienda leer en este orden:

1. mapa del modelo lógico
2. convenciones generales
3. núcleo del expediente
4. trazabilidad
5. piezas documentales
6. notificación
7. derivados operativos
8. satélites específicos
9. catálogos y maestros

---

## Resultado esperado

Al finalizar este bloque, el repo debe contar con una definición lógica suficientemente estable como para avanzar hacia:

- persistencia y SQL
- repositorios
- queries operativas
- proyecciones de backend
- validación de integridad estructural del modelo