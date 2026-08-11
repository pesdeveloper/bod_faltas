# Pantalla Operativa: Correo Postal

> **Estado documental:** NORMATIVE
>
> Comando backend: CMD-FALLO-003 (`LoteCorreoService.generarLoteDesdePendientes`)
> Referencia: `backend/api-faltas-core/docs/spec-as-source/20-application/fallo-command-contracts.md#cmd-fallo-003`

## 1. Propósito

La pantalla de correo postal permite generar y gestionar lotes de notificaciones enviadas por correo postal.
Es una pantalla operativa separada de la vista de bandejas, accesible desde el menú de navegación principal.

Esta pantalla opera sobre `FalLoteCorreo` y las notificaciones en estado `PENDIENTE_ENVIO` que se agruparán en un lote postal.

## 2. Acceso

- Accesible desde el menú principal o desde un acceso directo en el header.
- Solo para usuarios con rol habilitado para gestión de correo postal.

## 3. Vista principal: Lista de lotes

### 3.1 Encabezado

| Elemento | Descripción |
|----------|-------------|
| Título | "Correo Postal" |
| Contador | "X notificaciones pendientes de envío postal" |
| Botón "Generar lote" | Habilitado solo si hay notificaciones en `PENDIENTE_ENVIO` |

### 3.2 Lista de lotes existentes

| Columna | Descripción |
|---------|-------------|
| Código de lote | `loteCodigo` |
| Fecha de generación | `fhGeneracion` |
| Estado del lote | `estadoLote`: GENERADO, EN_PROCESO, ENVIADO, etc. |
| Referencia externa | `referenciaExterna` si existe |
| Cantidad de notificaciones | Total de notificaciones en el lote |
| Acciones | Ver detalle del lote |

## 4. Formulario: Generar lote (CMD-FALLO-003)

El botón "Generar lote" abre un formulario modal:

| Campo UI | Mapeo backend | Requerido | Validaciones |
|----------|--------------|-----------|--------------|
| Código de lote | `loteCodigo` (body) | sí | Trim; 1–30 chars; único en el sistema |
| Referencia externa | `referenciaExterna` (body) | no | Trim; 1–60 chars si se informa |
| GUID de lote externo | `guidLoteExt` (body) | no | Trim; exactamente 36 chars; formato UUID si se informa |

**Comportamiento al confirmar:**

- El frontend llama al endpoint de generación de lote.
- Si exitoso: muestra el lote generado en la lista.
- Si `LoteCodigoDuplicadoException` (409 equivalente): mostrar error "El código de lote ya existe".
- Si no hay notificaciones pendientes: el botón "Generar lote" está deshabilitado y se muestra un aviso.

## 5. Vista de detalle de un lote

Al seleccionar un lote de la lista:

| Sección | Contenido |
|---------|-----------|
| Datos del lote | `loteCodigo`, `fhGeneracion`, `estadoLote`, `referenciaExterna`, `guidLoteExt` |
| Lista de notificaciones | Cada notificación del lote con: acta, domicilio, estado del intento |
| Actas afectadas | Lista de actas cuyo bloque pasó a NOTI por este lote |

## 6. Requisito previo: Domicilio notificatorio

Para que una notificación pueda incluirse en un lote postal, el acta debe tener:
- `idDomicilioNotifAct` configurado.
- `FalPersonaDomicilio` referenciado existente.

Si una notificación en `PENDIENTE_ENVIO` no cumple este requisito, el backend rechazará el lote que la contenga.
La pantalla puede mostrar un aviso preventivo listando las notificaciones con domicilio faltante antes de generar el lote.

## 7. Comportamiento mobile

- La lista de lotes se muestra como lista simple.
- El formulario de generación de lote se muestra como bottom sheet o modal completo.
- La vista de detalle del lote ocupa la pantalla completa.
