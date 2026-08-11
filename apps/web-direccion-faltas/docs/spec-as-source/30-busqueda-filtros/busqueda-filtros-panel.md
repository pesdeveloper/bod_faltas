# Búsqueda, Filtros y Panel Resumen

> **Estado documental:** NORMATIVE

## 1. Tipos de búsqueda

### 1.1 Búsqueda global (cross-bandeja)

- Disponible desde el header; accesible desde cualquier pantalla.
- Busca actas en todas las bandejas simultáneamente.
- Criterios mínimos de búsqueda:
  - Número de acta (exacto o parcial).
  - DNI del infractor.
  - Apellido y nombre del infractor.
- Los resultados se muestran en una vista de lista neutral (sin bandeja activa en sidebar).
- Cada resultado muestra la bandeja donde reside el acta (badge de bandeja).
- Al seleccionar un resultado, se carga el detalle del acta con el contexto de su bandeja real.
- El parámetro `busqueda` se refleja en la URL.

### 1.2 Búsqueda local (dentro de la bandeja activa)

- Disponible en el sidebar o en el encabezado de la lista de actas.
- Filtra la lista visible sin cambiar de bandeja.
- Criterios: número de acta, nombre del infractor (por defecto).
- La búsqueda local es inmediata (filtro en cliente cuando el dataset es pequeño, o request al backend con debounce cuando es grande).
- El campo de búsqueda local se limpia al cambiar de bandeja.

## 2. Panel de filtros

### 2.1 Ubicación y apertura

- En desktop: panel lateral colapsable a la derecha del sidebar, o panel deslizante que se superpone sobre la lista.
- En mobile: modal o bottom sheet que cubre la lista.
- Se activa mediante un botón "Filtrar" en el encabezado de la lista.
- El botón muestra un badge con el número de filtros activos.

### 2.2 Filtros disponibles

Los filtros que el backend soporte deben exponerse en este panel. Los filtros mínimos esperados son:

| Filtro | Tipo | Descripción |
|--------|------|-------------|
| Fecha del hecho (desde/hasta) | Rango de fechas | Periodo en que ocurrió la infracción |
| Tipo de infracción | Selector (catálogo backend) | Categoría del hecho |
| Bandeja (solo en búsqueda global) | Selector múltiple | Limitar resultados a ciertas bandejas |
| Acción pendiente | Selector (catálogo `AccionPendiente`) | Filtrar por acción requerida |
| Estado de bloqueantes | Booleano | "Solo actas con bloqueantes activos" |

### 2.3 Aplicación de filtros

- Los filtros se aplican al presionar "Aplicar" o de forma reactiva (a definir en implementación).
- El botón "Limpiar filtros" restablece todos los filtros a su valor por defecto.
- Los filtros activos se muestran como chips/tags debajo del encabezado de la lista para que el operador vea qué está filtrando.

### 2.4 Persistencia de filtros

- Los filtros no persisten entre sesiones ni entre bandejas. Se limpian al cambiar de bandeja.
- Excepción: si el parámetro de filtro está en la URL, se restaura al cargar la página.

## 3. Panel de resumen / sumario

### 3.1 Propósito

El panel de resumen muestra estadísticas rápidas del estado operativo de las bandejas.
Es informativo y no es una fuente de verdad; los datos provienen de los contadores del backend.

### 3.2 Contenido

| Campo | Descripción |
|-------|-------------|
| Total de actas activas | Suma de actas en bandejas operativas (excluye CERRADAS y ARCHIVO) |
| Actas urgentes | Actas con `accionPendiente != NINGUNA` |
| Por bandeja | Contador de cada bandeja (mismo dato que el sidebar) |
| Últimas actualizaciones | Timestamp de la última sincronización de contadores |

### 3.3 Ubicación

- En desktop: puede desplegarse como panel lateral o como overlay al presionar un botón "Resumen" en el header.
- En mobile: accesible desde un menú o botón dedicado.

## 4. Orden de la lista

### 4.1 Orden por defecto

- Fecha del evento más reciente descendente.

### 4.2 Opciones de orden alternativas

| Opción | Criterio |
|--------|----------|
| Fecha del hecho (ascendente) | Más antiguas primero |
| Fecha del hecho (descendente) | Más recientes primero |
| Apellido del infractor (A-Z) | Orden alfabético |
| Número de acta (ascendente) | Orden numérico |

### 4.3 Persistencia del orden

- El orden seleccionado persiste durante la sesión del usuario en `sessionStorage`.
- Se restablece al cerrar la sesión.

## 5. Fila mínima de la lista

Cada fila de la lista debe mostrar como mínimo:

| Campo | Descripción |
|-------|-------------|
| Número de acta | Identificador primario |
| Apellido y nombre del infractor | Identificación de la persona |
| Tipo de infracción o descripción breve | Contexto operativo |
| Fecha del hecho | Referencia temporal |
| Badge de acción pendiente | Muestra `accionPendiente` en texto corto; si es `NINGUNA`, no se muestra o se muestra con estilo neutro |
| Icono "seguida" | Marcador visual si el operador sigue esta acta (ver `20-bandejas/bandejas-agrupamiento-routing.md` sección 7) |

## 6. Estados de la lista

| Estado | Visualización |
|--------|---------------|
| Cargando | Skeleton de filas |
| Sin resultados en bandeja | Mensaje: "No hay actas en esta bandeja" |
| Sin resultados de búsqueda | Mensaje: "No se encontraron actas para la búsqueda" |
| Error de carga | Mensaje de error con botón de reintentar |
| Resultado de búsqueda global | Lista con badge de bandeja por fila |

## 7. Seguir acta — detalle de la preferencia

### 7.1 Activación

El operador activa "seguir acta" desde el encabezado del panel de detalle del acta (ícono de marcador/estrella).

### 7.2 Almacenamiento

- Clave en `localStorage`: `faltas.seguirActa.<actaId>` con valor `true`.
- Alternativa (si se soporta múltiples activas simultáneamente): `faltas.actasSeguidas` como array de IDs.

### 7.3 Comportamiento al cambiar de bandeja

Ver reglas completas en `20-bandejas/bandejas-agrupamiento-routing.md` sección 7.

### 7.4 Indicador en la fila

Si el acta está en la lista visible y está marcada como seguida, la fila muestra un ícono de marcador.
El ícono se muestra sin importar si el acta está seleccionada o no.
