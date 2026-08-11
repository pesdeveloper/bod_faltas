# Portal del Infractor — Spec-as-source

> **Estado documental:** NORMATIVE
>
> Fuente de dominio: `backend/api-faltas-core/docs/spec-as-source/`
> Canal portal: CMD-FALLO-004 variante portal (`QrActaService.registrarAccesoConNotificacion`)

## 1. Propósito y alcance

El portal del infractor es una aplicación web pública que permite al ciudadano:

1. Acceder al estado de su acta mediante un link o código QR recibido en la notificación.
2. Confirmar la recepción de la notificación (resultado positivo vía canal PORTAL_INFRACTOR).
3. Consultar el detalle de su acta (datos del hecho, fallo, monto de condena si corresponde).
4. (Futuro) Gestionar el pago de su obligación.

## 2. Acceso: token QR y seguridad

### 2.1 Flujo de acceso

1. El infractor recibe una notificación con un código QR o un link único.
2. Al escanear el QR o acceder al link, el navegador del infractor llega al portal.
3. El portal envía el token QR al backend.
4. El backend resuelve el token vía `QrActaService` y determina el acta y notificación asociados.
5. Si el token es válido: el infractor accede a la vista del acta.
6. Si el token es inválido o expirado: se muestra un mensaje de error.

### 2.2 Características del token

- El token QR es de un solo uso o tiene expiración configurada por el backend.
- No existe autenticación de usuario con contraseña; el acceso es exclusivamente por token QR.
- El token identifica unívocamente: acta + notificación.

### 2.3 Sin estado de sesión persistente

- No hay login ni logout.
- La sesión expira con el token.
- El infractor no puede acceder a otras actas desde el portal.

## 3. Vista principal: Estado del acta (vista del infractor)

Cuando el acceso es exitoso, el infractor ve una vista simplificada del acta:

### 3.1 Datos del hecho

| Campo | Descripción |
|-------|-------------|
| Número de acta | Identificador visible |
| Fecha y lugar del hecho | Cuándo y dónde ocurrió |
| Tipo de infracción | Categoría de la infracción |
| Descripción del hecho | Texto legible |

### 3.2 Notificación (si la sesión corresponde a una notificación activa)

| Campo | Descripción |
|-------|-------------|
| Tipo de documento notificado | Fallo absolutorio / Fallo condenatorio / Pieza procesal |
| Fecha de envío de la notificación | Cuándo se envió |
| Estado de la notificación | En proceso / Notificado |

### 3.3 Resultado del fallo (si existe fallo)

| Campo | Descripción |
|-------|-------------|
| Tipo de fallo | Absolutorio / Condenatorio |
| Monto de condena | Si condenatorio y ya dictado |
| Fecha vencimiento de apelación | `fhVtoApelacion`; solo si fallo condenatorio notificado |
| Estado de la condena | Si fue declarada firme |

### 3.4 Estado del pago (si corresponde)

| Campo | Descripción |
|-------|-------------|
| Estado del pago de condena | Pendiente / Informado / Confirmado |
| Instrucciones de pago | Texto informativo sobre cómo pagar |

## 4. Acción: Confirmar recepción de notificación

Cuando el acceso corresponde a una notificación activa (`FalNotificacion` en `EN_PROCESO`),
el portal registra automáticamente el acceso como resultado positivo vía el canal PORTAL_INFRACTOR.

### 4.1 Flujo backend

El backend ejecuta la variante portal de CMD-FALLO-004:
- `QrActaService.registrarAccesoConNotificacion`
- → `NotificacionIntentoService.registrarPortalPositivo`
- Crea intento con `canalNotif = PORTAL_INFRACTOR`, resultado `POSITIVO`.
- Actualiza la notificación a `CON_ACUSE_POSITIVO`.
- Si el fallo era condenatorio: registra `fhNotificacion` y calcula `fhVtoApelacion`.

### 4.2 Comportamiento en el portal

- Al cargar la vista del acta (si hay notificación activa), el backend ya registró el acceso como positivo.
- La vista muestra una confirmación: "Su presencia ha sido registrada. La notificación fue recibida exitosamente."
- No se muestra un botón de "confirmar"; el acceso al portal ya constituye la confirmación.

### 4.3 Errores posibles

| Error | Mensaje al infractor |
|-------|----------------------|
| Token inválido | "El enlace no es válido o ya fue utilizado." |
| Token expirado | "El enlace ha expirado. Comuníquese con la Dirección de Faltas." |
| Notificación ya tiene resultado | "Esta notificación ya fue procesada." |
| Error de sistema | "Ocurrió un error. Por favor intente nuevamente." |

## 5. (Futuro) Gestión del pago

Esta funcionalidad está fuera del alcance inicial pero debe considerarse en el diseño:

- El infractor podría ver el monto de la condena e instrucciones de pago.
- Informar el pago directamente desde el portal (CMD-FALLO-007 o equivalente con token QR).
- Ver el estado del pago informado.

La implementación de pagos en el portal requiere decisión arquitectónica sobre autenticación de pago y referencia del comprobante.

## 6. Integración con el backend

### 6.1 Endpoints utilizados (a confirmar en `40-api/http-contracts.md`)

| Operación | Endpoint esperado | Descripción |
|-----------|-------------------|-------------|
| Resolver token QR + registrar acceso | Backend específico de QR | Devuelve datos del acta y notificación; registra acceso positivo |
| Consultar estado del acta | Endpoint de consulta por actaId o token | Vista read-only del acta para el infractor |

### 6.2 No se exponen bandejas ni acciones del operador

El portal del infractor consume únicamente datos de la vista del ciudadano.
No tiene acceso a las bandejas, acciones operativas, ni configuraciones del sistema.

## 7. UX y diseño

### 7.1 Mobile-first

El portal es principalmente mobile (el infractor escanea el QR desde su teléfono).
El diseño debe funcionar bien en pantallas de 360 px de ancho.

### 7.2 Sin sidebar ni navegación compleja

- Una sola pantalla: el estado del acta.
- Sin login, sin menú de usuario, sin navegación a otras secciones.
- Botón "Volver" solo si hay navegación de sub-páginas.

### 7.3 Lenguaje ciudadano

El portal usa lenguaje accesible para ciudadanos, no jerga jurídica.
Los estados y labels se muestran con términos simples:

| Término backend | Texto en portal |
|-----------------|----------------|
| FALLO_CONDENATORIO | "Usted fue condenado al pago de una multa" |
| CONDENA_FIRME | "La condena es definitiva" |
| PENDIENTE_PAGO_CONDENA | "Tiene una multa pendiente de pago" |
| ABSUELTO | "Usted fue absuelto" |

## 8. Límites del portal

El portal del infractor NO puede:

- Acceder a otras actas distintas a la identificada por el token.
- Modificar datos del acta o del infractor.
- Ejecutar acciones de operador (dictar fallo, enviar notificación, etc.).
- Cancelar o modificar la notificación.
- Acceder al sistema de bandejas del operador.
