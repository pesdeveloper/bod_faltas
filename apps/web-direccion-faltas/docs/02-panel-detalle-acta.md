# UX Demo — Panel detalle de acta

## Objetivo

Definir el panel derecho al seleccionar una acta.

Debe responder:

- dónde está
- qué tiene pendiente
- qué pasó
- qué puede hacerse
- por qué no puede hacerse algo

## Bloques

Orden sugerido:

1. Encabezado
2. Resumen operativo
3. Datos del acta
4. Hechos materiales
5. Cerrabilidad
6. Documentos/firma
7. Notificación
8. Pago
9. Gestión externa / archivo
10. Historial
11. Acciones

## Encabezado

Mostrar:

- número acta
- dependencia
- tipo operativo
- bandeja
- resultado final
- cerrabilidad
- badges principales

Ejemplo:

ACTA-0024  
Tránsito · Actas en enriquecimiento  
Resultado: Pago confirmado  
Cerrable: No

## Resumen operativo

Texto corto generado por UI/backend.

Debe decir:

- qué necesita
- qué bloquea
- último evento relevante

Ejemplo:

Pago confirmado, pero no puede cerrarse porque existen bloqueantes materiales: liberar rodado, entregar documentación, levantar medida.

## Datos del acta

Campos base:

- número
- dependencia
- tipo demo
- bandeja
- acción pendiente
- resultado final

Por dependencia:

| Dependencia | Datos visibles |
|---|---|
| Tránsito | eje urbano, rodado retenido, documentación retenida |
| Inspecciones | clausura / medida preventiva |
| Fiscalización | paralización de obra |
| Bromatología | decomiso sustancias alimenticias |

## Hechos materiales

Mostrar componente dedicado.

Resumen por eje:

- eje
- fase
- bloquea cierre
- acción sugerida

Ver reglas en `05`.

## Cerrabilidad

Mostrar componente dedicado.

Debe incluir:

- cerrable sí/no
- resultado final compatible o faltante
- pendientes activos
- botón `Cerrar acta`
- motivo si está deshabilitado

Ver reglas en `05`.

## Documentos y firma

Campos:

- id documento
- tipo
- estado
- requiere firma
- requiere notificación
- acción disponible

Estados:

- `PENDIENTE_FIRMA`
- `FIRMADO`
- `EMITIDO`

Mensaje fijo:

Firmar no verifica cumplimiento material.

## Notificación

Campos:

- estado
- canal
- resultado
- reintentos
- acciones

Acciones:

- acuse positivo
- acuse negativo
- vencimiento
- reintento
- volver a análisis

## Pago

Mostrar:

- solicitud pago voluntario
- pago informado
- comprobante mock
- pendiente confirmación
- confirmado
- observado

Mensaje fijo:

Pago confirmado no cierra automáticamente.

## Gestión externa / archivo

Si gestión externa:

- tipo gestión
- trazabilidad
- retorno / re-derivación

Si archivo:

- motivo
- acción reingresar

## Historial

Mostrar solo eventos funcionales.

Eventos útiles:

- alta/labrado
- cambio bandeja
- pago
- resolución
- firma
- notificación
- medida posterior
- cumplimiento
- archivo
- reingreso
- cierre

## Acciones

Agrupar:

- primarias
- secundarias
- transición
- demo/técnicas

Acciones importantes deshabilitadas con motivo.

Ejemplo:

Cerrar acta: deshabilitado.  
Motivo: existen pendientes materiales activos.

## Post-acción

Después de acción:

1. refrescar detalle
2. refrescar lista
3. refrescar contadores
4. mostrar toast
5. indicar movimiento si cambió bandeja

## Criterio de cierre

Listo cuando el panel muestra:

- resumen
- datos mínimos
- hechos materiales
- cerrabilidad
- documentos/firma
- pago/notificación si aplica
- historial
- acciones contextuales
- motivos de bloqueo