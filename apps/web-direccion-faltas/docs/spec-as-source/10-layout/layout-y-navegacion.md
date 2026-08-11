# Layout y Navegación — web-direccion-faltas

> **Estado documental:** NORMATIVE

## 1. Modelo de layout general

La aplicación usa un modelo de tres zonas en desktop y un modelo de una sola zona activa en mobile:

```
┌──────────────────────────────────────────────────────────┐
│ HEADER (fijo, 100% ancho)                                │
├──────────────┬───────────────────────────────────────────┤
│ SIDEBAR      │ ZONA PRINCIPAL                            │
│ (bandejas,   │ ┌─────────────────┬─────────────────────┐│
│  búsqueda    │ │ LISTA DE ACTAS  │ PANEL DE DETALLE    ││
│  global)     │ │                 │ (acta seleccionada) ││
│              │ │                 │                     ││
│              │ └─────────────────┴─────────────────────┘│
└──────────────┴───────────────────────────────────────────┘
```

En mobile, las tres zonas se apilan verticalmente y el detalle del acta se presenta como overlay o pantalla completa.

## 2. Header

### 2.1 Contenido

| Elemento | Posición | Descripción |
|----------|----------|-------------|
| Logo/nombre de la aplicación | Izquierda | Identidad visual |
| Búsqueda global | Centro | Campo de texto; ver `30-busqueda-filtros/` |
| Menú de usuario | Derecha | Nombre del operador, logout |
| Notificaciones del sistema | Derecha | Alertas o errores sistémicos |

### 2.2 Comportamiento

- El header es fijo (sticky) en scroll.
- En mobile, la búsqueda global puede contraerse a un ícono que despliega un campo al tocarlo.

## 3. Sidebar

### 3.1 Contenido

- Entradas de bandeja (ver `20-bandejas/bandejas-agrupamiento-routing.md`).
- Contador por bandeja: número de actas en esa bandeja. Se actualiza por polling o push desde el backend.
- Búsqueda local dentro de la bandeja activa (campo de texto rápido).

### 3.2 Comportamiento en desktop

- Ancho fijo: 240 px–280 px.
- Entrada activa resaltada visualmente.
- El sidebar no tiene scroll horizontal.

### 3.3 Comportamiento en mobile

- El sidebar se oculta por defecto y se abre mediante un menú hamburguesa en el header.
- Al seleccionar una bandeja, el sidebar se cierra automáticamente.
- El sidebar en mobile es un drawer que aparece sobre la zona principal (no desplaza el contenido).

## 4. Lista de actas

### 4.1 Contenido por fila mínima

Cada fila de la lista debe mostrar:

| Campo | Origen | Descripción |
|-------|--------|-------------|
| Número de acta | Snapshot | Identificador visible del acta |
| Apellido y nombre del infractor | Snapshot | Datos básicos de la persona |
| Tipo de infracción o descripción breve | Snapshot | Contexto operativo rápido |
| Fecha del hecho | Snapshot | Ayuda a identificar el acta |
| Acción pendiente | `accionPendiente` del snapshot | Badge visual del paso requerido |
| Indicador "seguida" | localStorage | Icono de marcador si el operador la sigue |

### 4.2 Comportamiento

- La fila es clickeable/tappable en su totalidad.
- Al seleccionar una fila en desktop: abre el panel de detalle a la derecha sin recargar la lista.
- Al seleccionar una fila en mobile: abre el detalle como overlay o navega a pantalla completa.
- La fila activa/seleccionada tiene highlight visual diferenciado.
- Orden por defecto: fecha del evento más reciente descendente. El operador puede cambiar el orden (ver `30-busqueda-filtros/`).

### 4.3 Paginación

- La lista pagina por scroll infinito o paginación explícita (a definir en la implementación).
- El total de actas en la bandeja se muestra en el encabezado de la lista.

## 5. Panel de detalle del acta

### 5.1 Desktop

- Columna derecha del layout principal, ancho 50%–60%.
- Si no hay acta seleccionada, muestra estado vacío ("Seleccioná un acta para ver el detalle").
- El panel tiene scroll interno independiente de la lista.
- El botón de cierre (X) vuelve al estado sin acta seleccionada.

### 5.2 Mobile

- El detalle se muestra como overlay de pantalla completa sobre la lista.
- La navegación de vuelta a la lista se hace con el botón de retroceso del navegador o un botón explícito "Volver".
- En mobile no coexisten la lista y el detalle visibles simultáneamente.

## 6. Navegación y query parameters

### 6.1 Parámetros de URL

La URL debe reflejar el estado de navegación para soportar recarga, compartir y navegación con botón atrás:

| Parámetro | Descripción | Ejemplo |
|-----------|-------------|---------|
| `bandeja` | Código de bandeja activa | `?bandeja=PENDIENTE_ANALISIS` |
| `acta` | ID del acta seleccionada | `?bandeja=PENDIENTE_ANALISIS&acta=12345` |
| `busqueda` | Término de búsqueda global | `?busqueda=garcia` |
| `pagina` | Página o cursor de paginación | `?pagina=2` |

Los filtros activos se expresan como parámetros adicionales o en el query string según la implementación.

### 6.2 Comportamiento del historial

- Seleccionar una bandeja diferente actualiza el parámetro `bandeja` sin recargar la página.
- Seleccionar un acta agrega el parámetro `acta`.
- El botón atrás del navegador debe restaurar el estado anterior (lista sin selección, o bandeja anterior).

### 6.3 Ruta inicial

- Sin parámetros: se carga la bandeja con más actas urgentes (a definir en configuración o la primera bandeja del sidebar con actas).

## 7. Breakpoints

| Nombre | Ancho mínimo | Comportamiento |
|--------|-------------|----------------|
| `mobile` | 360 px | Una columna, sidebar oculto, detalle como overlay |
| `tablet` | 768 px | Sidebar deslizante disponible, lista ocupa todo el ancho |
| `desktop` | 1024 px | Layout de tres columnas completo |
| `wide` | 1440 px | Margen lateral; las columnas se limitan en ancho máximo |

## 8. Accesibilidad y rendimiento

- Todas las regiones tienen `role` y `aria-label` apropiados.
- El foco del teclado debe moverse lógicamente: header → sidebar → lista → detalle.
- La lista utiliza virtualización cuando supera los 50 elementos visibles.
- El panel de detalle no bloquea la lista (no hay modal bloqueante en desktop).
- Los estados de carga muestran skeletons, no spinners bloqueantes de la interfaz.
