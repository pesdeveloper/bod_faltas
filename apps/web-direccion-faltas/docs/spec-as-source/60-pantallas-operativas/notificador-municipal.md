# Pantalla Operativa: Notificador Municipal

> **Estado documental:** NORMATIVE
>
> Comando backend: CMD-FALLO-002 con `canal = NOTIFICADOR_MUNICIPAL`
> Referencia: `backend/api-faltas-core/docs/spec-as-source/20-application/fallo-command-contracts.md#cmd-fallo-002`

## 1. Propósito

La pantalla de notificador municipal permite gestionar notificaciones enviadas a través del
notificador municipal del municipio. A diferencia del correo postal, estas notificaciones se
envían individualmente por acta, no en lotes.

## 2. Acceso

- Accesible desde el menú principal o desde el panel de detalle del acta cuando la acción pendiente es `ENVIAR_NOTIFICACION` y el canal seleccionado es `NOTIFICADOR_MUNICIPAL`.
- Solo para usuarios con rol habilitado para gestión de notificaciones.

## 3. Flujo de notificación vía notificador municipal

### 3.1 Inicio desde el panel de detalle del acta

Cuando el operador ejecuta la acción `ENVIAR_NOTIFICACION` (CMD-FALLO-002) seleccionando el canal `NOTIFICADOR_MUNICIPAL`:

1. Verificar que el acta tiene `idDomicilioNotifAct` configurado. Si no: mostrar error y bloquear la acción.
2. El formulario de envío de notificación muestra el domicilio notificatorio del acta como destino (solo lectura; no se ingresa `destinoDigital` ya que es un canal físico).
3. Al confirmar, el backend crea `FalNotificacion` en `EN_PROCESO` y genera `FalNotificacionIntento` con `canalNotif = NOTIFICADOR_MUNICIPAL`.

### 3.2 Vista de gestión de notificaciones municipales

Pantalla independiente que lista las notificaciones con canal `NOTIFICADOR_MUNICIPAL` en estado activo:

| Columna | Descripción |
|---------|-------------|
| Número de acta | Identificador del acta |
| Infractor | Nombre y DNI |
| Domicilio de notificación | Dirección del domicilio notificatorio |
| Estado del intento | `estadoIntento`: EN_PROCESO |
| Fecha de envío | `fhIntento` |
| Acciones | Registrar resultado (positivo / negativo / vencido) |

## 4. Registrar resultado de la notificación municipal

Desde la lista o desde el detalle del acta, el operador registra el resultado del intento.

### 4.1 Resultado positivo (CMD-FALLO-004)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Intento | `intentoId` (body) | sí | Identificador del intento; obtenido del contexto |
| Observaciones | `observaciones` (body) | no | Texto libre |

### 4.2 Resultado negativo (RegistrarNotificacionNegativa)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Intento | (según contrato HTTP) | sí | Identificador del intento |
| Observaciones | `observaciones` (body) | no | Texto libre |

### 4.3 Notificación vencida (RegistrarNotificacionVencida)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Intento | (según contrato HTTP) | sí | Identificador del intento |
| Observaciones | `observaciones` (body) | no | Texto libre |

## 5. Requisito previo: Domicilio notificatorio

Al igual que el correo postal, el canal `NOTIFICADOR_MUNICIPAL` requiere:
- `idDomicilioNotifAct` configurado en el acta.
- `FalPersonaDomicilio` referenciado existente.

Si el domicilio no está configurado, el sistema muestra:
> "El acta no tiene domicilio notificatorio configurado. No es posible enviar notificación municipal."

Y deshabilita la selección de este canal en el formulario de `ENVIAR_NOTIFICACION`.

## 6. Diferencias con correo postal

| Aspecto | Correo Postal | Notificador Municipal |
|---------|---------------|-----------------------|
| Agrupamiento | En lotes (`FalLoteCorreo`) | Individual por acta |
| Generación | Batch: todas las notificaciones `PENDIENTE_ENVIO` | Una por vez desde el acta |
| Pantalla de gestión | Lista de lotes | Lista de intentos individuales activos |
| Proceso backend | CMD-FALLO-003 | CMD-FALLO-002 con canal NOTIFICADOR_MUNICIPAL |

## 7. Comportamiento mobile

- La pantalla de gestión de notificaciones municipales activas ocupa la pantalla completa.
- El formulario de resultado se muestra como bottom sheet.
- La lista de notificaciones activas tiene filtro por estado y por fecha.
