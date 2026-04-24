# UX Demo — Hechos materiales y cerrabilidad

## Objetivo

Visualizar por qué un acta puede o no cerrarse.

Separar:

- documento
- firma
- notificación
- cumplimiento material
- cerrabilidad
- cierre

## Regla central

Cerrable = resultado final compatible y sin pendientes materiales/documentales activos.

Resultados compatibles:

- `ABSUELTO`
- `PAGO_CONFIRMADO`

No hay cierre automático.

## Ejes actuales

| Eje | Bloqueante |
|---|---|
| Medida preventiva | `LEVANTAMIENTO_MEDIDA_PREVENTIVA` |
| Rodado retenido | `LIBERACION_RODADO` |
| Documentación retenida | `ENTREGA_DOCUMENTACION` |

Eje futuro posible:

- decomiso: `LIBERACION_DECOMISO` o `DISPOSICION_DECOMISO`, a validar

## Fases visibles

| Técnica | Label UI |
|---|---|
| `NO_APLICA` | No aplica |
| `SITUACION_PENDIENTE_DE_RESOLUTORIO` | Pendiente de resolución |
| `RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL` | Resolución registrada, falta cumplimiento |
| `CUMPLIMIENTO_MATERIAL_VERIFICADO` | Cumplimiento verificado |

## Vista de hechos materiales

Mostrar por eje:

- nombre
- fase
- bloquea cierre: sí/no
- acción sugerida
- motivo

Ejemplo:

Medida preventiva  
Fase: Resolución registrada, falta cumplimiento  
Bloquea cierre: Sí  
Acción: Registrar cumplimiento material

## Lectura operativa

Mensajes sugeridos:

| Caso | Mensaje |
|---|---|
| condición inicial | Hay condiciones materiales activas originadas en el acta |
| medida posterior | Hay una medida preventiva generada durante el trámite |
| resolutorio sin cumplimiento | Hay resolución registrada, falta cumplimiento material |
| todo cumplido | Sin pendientes materiales activos |

## Cerrabilidad no cerrable

Mostrar:

- estado: No cerrable
- resultado final: sí/no
- pendientes
- botón cerrar deshabilitado
- motivo

Ejemplo:

Resultado final: Pago confirmado  
Pendientes: Liberación de rodado, entrega documentación  
Cerrar acta: deshabilitado  
Motivo: existen pendientes materiales activos

## Cerrabilidad cerrable

Mostrar:

- estado: Cerrable
- resultado final compatible
- sin pendientes
- botón cerrar habilitado

## Motivos de bloqueo

| Motivo | Texto UI |
|---|---|
| sin resultado | Falta resultado final compatible |
| bloqueantes | Existen pendientes materiales |
| documentos | Existen documentos pendientes |
| gestión externa | Acta en gestión externa |
| archivo | Acta archivada |
| cerrada | Acta ya cerrada |

## Documento resolutivo

Dictar resolución:

- crea documento/acto
- puede requerir firma
- puede requerir notificación
- no cumple materialmente el hecho

Mensaje UI:

Resolución registrada. Falta cumplimiento material.

## Firma

Firmar documento:

- cambia estado documental
- no cumple materialmente el hecho
- no cierra acta

Mensaje UI:

Documento firmado. El cumplimiento material sigue pendiente.

## Notificación

Notificar:

- comunica acto
- no cumple materialmente el hecho
- no cierra acta por sí sola

Mensaje UI:

Notificación registrada. El cumplimiento material sigue pendiente.

## Cumplimiento material

Acciones actuales:

- levantar medida preventiva
- liberar rodado
- entregar documentación

Resultado:

- eje pasa a cumplido
- deja de bloquear cierre
- si no quedan pendientes y hay resultado compatible, acta queda cerrable

## Bromatología / decomiso

Actual:

- decomiso es dato del acta
- no genera bloqueante material vigente
- no genera medida preventiva genérica

A validar con Tribunal:

- si requiere liberación
- si requiere disposición
- si requiere destrucción
- si requiere restitución
- si necesita eje futuro

## Criterio de cierre

Listo cuando UI muestra:

- ejes
- fases
- pendientes
- motivo de no cierre
- cierre explícito
- separación documento/firma/notificación/cumplimiento