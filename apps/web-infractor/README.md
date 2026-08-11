# web-infractor — Portal del Infractor

## Propósito

`web-infractor` es la aplicación web pública del infractor. Permite al infractor:

- acceder al estado de su acta mediante un código QR o token seguro;
- visualizar el detalle del acta y la notificación recibida;
- confirmar la recepción de la notificación (resultado positivo vía portal);
- consultar y gestionar el pago de su obligación.

Esta aplicación es independiente de `web-direccion-faltas` (la aplicación del operador interno).

## Separación de aplicaciones

| Aplicación | Usuarios | Propósito |
|------------|----------|-----------|
| `web-direccion-faltas` | Operadores internos (dirección de faltas) | Gestión operativa de actas y bandejas |
| `web-infractor` | Ciudadanos / infractores | Acceso al estado del propio acta y acciones permitidas |

## Stack

A definir en la implementación. Recomendaciones:

- Aplicación web responsive (mobile-first), accesible desde cualquier dispositivo.
- Sin autenticación tradicional de usuario; acceso vía token QR seguro y de un solo uso o con expiración.
- Integración con backend `api-faltas-core` exclusivamente (no acceso directo a base de datos ni lógica de dominio en el frontend).

## Spec

La especificación de comportamiento del portal del infractor vive en:

```
apps/web-infractor/docs/spec-as-source/portal-infractor.md
```

## Relación con el backend

El backend ya contempla el canal `PORTAL_INFRACTOR` en la variante portal de CMD-FALLO-004.
El acceso QR es gestionado por `QrActaService.registrarAccesoConNotificacion` del backend.
Esta aplicación consume las APIs del backend como cualquier cliente HTTP autenticado con token QR.
