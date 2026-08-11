# Bandejas — Agrupamiento visual y routing

> **Estado documental:** NORMATIVE
>
> Fuente canónica de bandejas: `backend/api-faltas-core/docs/spec-as-source/30-projections/snapshot-bandejas-acciones.md`

## 1. Principio

Las bandejas son proyecciones operativas derivadas del snapshot del backend.
El frontend nunca calcula a qué bandeja pertenece un acta; lee `codBandeja` del snapshot.

Los agrupamientos visuales del sidebar son decisiones de presentación que NO modifican las bandejas del dominio.
El backend devuelve siempre los 16 códigos canónicos; el frontend decide cómo organizarlos visualmente.

## 2. Las 16 bandejas canónicas del backend

| Código backend | Label canónico backend |
|----------------|------------------------|
| `ACTAS_EN_ENRIQUECIMIENTO` | Actas en captura/enriquecimiento |
| `PENDIENTE_PREPARACION_DOCUMENTAL` | Pendiente de preparación documental |
| `PENDIENTE_FIRMA` | Pendiente de firma |
| `PENDIENTE_NOTIFICACION` | Pendiente de notificación |
| `EN_NOTIFICACION` | En notificación esperando resultado |
| `PENDIENTE_ANALISIS` | Pendiente de análisis jurídico |
| `PENDIENTES_RESOLUCION_REDACCION` | Pendiente de redacción resolutoria |
| `PENDIENTES_FALLO` | Fallo notificado, pendiente de paso siguiente |
| `CON_APELACION` | Con apelación presentada |
| `GESTION_EXTERNA` | En gestión externa |
| `PARALIZADAS` | Paralizadas |
| `ARCHIVO` | Archivo administrativo |
| `PENDIENTE_CONFIRMACION_PAGO` | Pendiente de confirmación de pago voluntario |
| `PENDIENTE_PAGO_CONDENA` | Pendiente de pago de condena |
| `PENDIENTE_CONFIRMACION_PAGO_CONDENA` | Pendiente de confirmación de pago de condena |
| `CERRADAS` | Cerradas definitivamente |

## 3. Las 13 entradas del sidebar y su mapeo

El sidebar presenta 13 entradas navegables. Las reglas de agrupamiento son decisiones de presentación.

| # | Entrada sidebar | Código(s) backend cubiertos | Rationale del agrupamiento |
|---|-----------------|-----------------------------|-----------------------------|
| 1 | Captura y enriquecimiento | `ACTAS_EN_ENRIQUECIMIENTO` + `PENDIENTE_PREPARACION_DOCUMENTAL` | Ambos representan actas incompletas en proceso de preparación documental; el operador necesita completar o generar documentos |
| 2 | Pendiente de firma | `PENDIENTE_FIRMA` | Bandeja independiente; acción bien definida (`FIRMAR_DOCUMENTO`); entrada única |
| 3 | Pendiente de notificación | `PENDIENTE_NOTIFICACION` | Sub-entrada dentro de la sección "Notificaciones"; acción `ENVIAR_NOTIFICACION` |
| 4 | En notificación | `EN_NOTIFICACION` | Sub-entrada dentro de la sección "Notificaciones"; acción `EVALUAR_NOTIFICACION` |
| 5 | Pendiente de análisis | `PENDIENTE_ANALISIS` | Bandeja central del flujo jurídico; múltiples acciones posibles según sub-estado |
| 6 | Redacci&#243;n resolutoria | `PENDIENTES_RESOLUCION_REDACCION` | Bloque ANAL: FalDocumento(tipoDocu=ACTO_ADMINISTRATIVO, estadoDocu=PENDIENTE_FIRMA) sin FalActaFallo activo; accionPendiente=`FIRMAR_DOCUMENTO` |
| 7 | Pendiente de fallo | `PENDIENTES_FALLO` | Fallo notificado; accionPendiente = NINGUNA; espera vencimiento de plazo de apelación |
| 8 | Con apelación | `CON_APELACION` | Apelación activa presentada; acción `RESOLVER_APELACION` |
| 9 | Pagos | `PENDIENTE_CONFIRMACION_PAGO` + `PENDIENTE_PAGO_CONDENA` + `PENDIENTE_CONFIRMACION_PAGO_CONDENA` | Agrupamiento visual de tres bandejas de gestión de pagos; contador unificado y sub-entradas expandibles |
| 10 | Gestión externa | `GESTION_EXTERNA` | Actas derivadas a gestión externa; acción `NINGUNA` en general |
| 11 | Paralizadas | `PARALIZADAS` | Actas detenidas por causa administrativa; acción `NINGUNA` |
| 12 | Archivo | `ARCHIVO` | Actas archivadas administrativamente; acción `NINGUNA` |
| 13 | Cerradas | `CERRADAS` | Actas cerradas definitivamente; solo consulta |

## 4. Detalle de agrupamientos visuales compuestos

### 4.1 Entrada 1: Captura y enriquecimiento

Cubre `ACTAS_EN_ENRIQUECIMIENTO` y `PENDIENTE_PREPARACION_DOCUMENTAL`.

- Contador: suma de actas en ambas bandejas backend.
- La lista muestra actas de ambas bandejas sin distinguirlas visualmente a nivel de fila (es transparente para el operador).
- Si se requiere distinción operativa, se puede agregar un badge diferenciador en la fila del acta.
- Cuando el operador accede a esta entrada del sidebar, la lista muestra todas las actas de `ACTAS_EN_ENRIQUECIMIENTO` + `PENDIENTE_PREPARACION_DOCUMENTAL`.

### 4.2 Entradas 3 y 4: Sección "Notificaciones"

`PENDIENTE_NOTIFICACION` y `EN_NOTIFICACION` son entradas separadas dentro de una sección colapsable "Notificaciones" en el sidebar.

- La sección tiene un contador total (suma de ambas bandejas).
- Cada sub-entrada tiene su propio contador.
- El operador puede navegar independientemente a cada una.
- La sección se muestra expandida por defecto si alguna de las dos bandejas tiene actas.

### 4.3 Entrada 9: Sección "Pagos"

Cubre tres bandejas: `PENDIENTE_CONFIRMACION_PAGO`, `PENDIENTE_PAGO_CONDENA`, `PENDIENTE_CONFIRMACION_PAGO_CONDENA`.

- La sección tiene un contador total (suma de las tres).
- Cada sub-entrada tiene su propio contador.
- El operador puede navegar independientemente a cada sub-entrada.
- Al navegar a una sub-entrada, la lista muestra solo las actas de esa bandeja backend.

Mapeo de sub-entradas:

| Sub-entrada | Código backend | Acción backend |
|-------------|----------------|----------------|
| Confirmar pago voluntario | `PENDIENTE_CONFIRMACION_PAGO` | `CONFIRMAR_PAGO` |
| Pago de condena pendiente | `PENDIENTE_PAGO_CONDENA` | `GESTIONAR_PAGO_CONDENA` o `CORREGIR_PAGO_CONDENA` |
| Confirmar pago de condena | `PENDIENTE_CONFIRMACION_PAGO_CONDENA` | `CONFIRMAR_PAGO_CONDENA` |

## 5. Orden de las entradas en el sidebar

El orden del sidebar refleja el flujo operativo cronológico del acta:

1. Captura y enriquecimiento (inicio del ciclo)
2. Pendiente de firma
3. Notificaciones (Pendiente de notificación / En notificación)
4. Pendiente de análisis
5. Redacción resolutoria
6. Pendiente de fallo
7. Con apelación
8. Pagos (Pago voluntario / Pago condena pendiente / Confirmar pago condena)
9. Gestión externa
10. Paralizadas
11. Archivo
12. Cerradas

## 6. Contadores

### 6.1 Fuente

Los contadores de cada bandeja provienen del backend. El frontend no los calcula.
El endpoint de contadores se consume periódicamente (polling) o por suscripción push.

### 6.2 Visualización

- Badge numérico en la entrada del sidebar.
- Si el contador es 0: el badge no se muestra o se muestra "0" con estilo tenue.
- Para entradas agrupadas: el badge muestra la suma de los backend codes cubiertos.
- Para secciones con sub-entradas: el badge de la sección es la suma de sus sub-entradas.

### 6.3 Actualización

- Se actualiza como mínimo al cambiar de bandeja.
- Recomendado: polling cada 30–60 segundos en segundo plano o SSE/WebSocket si el backend lo soporta.
- Tras ejecutar una acción sobre un acta, se recarga el contador de la bandeja activa.

## 7. Comportamiento al cambiar de bandeja

### 7.1 Regla de "seguir acta"

El operador puede marcar un acta como "seguida" (icono de marcador en el encabezado del detalle).
Esta preferencia se persiste en `localStorage` bajo la clave `faltas.seguirActa.<actaId>`.

Cuando el operador cambia de bandeja:

- Si tiene una acta seguida y esa acta **existe en la nueva bandeja**: se mantiene seleccionada automáticamente.
- Si tiene una acta seguida pero esa acta **no existe en la nueva bandeja**: no se selecciona ningún acta. El icono de seguimiento permanece activo en `localStorage` (no se borra).
- Si no tiene acta seguida: no se selecciona ningún acta al cambiar de bandeja.

### 7.2 Acta seguida entre sesiones

La preferencia de "seguir acta" persiste entre recargas de página (localStorage).
Para limpiarla, el operador desactiva el marcador en el encabezado del detalle del acta.

## 8. Invariante de cobertura total

Toda acta con `codBandeja` devuelto por el backend tiene una entrada visible en el sidebar.
No existe código de bandeja canónico sin representación en el UI.

Los 16 códigos canónicos del backend quedan cubiertos por las 13 entradas del sidebar
según la tabla del apartado 3. Ningún código queda sin entrada de navegación.

## 9. Nota sobre PENDIENTES_RESOLUCION_REDACCION

`PENDIENTES_RESOLUCION_REDACCION` aparece en el catálogo de bandejas del backend pero
no tiene routing definido actualmente (no hay fila en la tabla de routing de bloques que
lleve a esta bandeja). Ver `90-gaps-pendientes/gaps-backend-spec.md`.

La entrada de sidebar para esta bandeja debe mostrarse. Su contador será 0 hasta que el
backend defina el routing y empiece a colocar actas en ella.
