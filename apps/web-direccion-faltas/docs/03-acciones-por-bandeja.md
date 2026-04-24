# UX Demo — Acciones por bandeja

## Objetivo

Definir botones visibles por bandeja y motivos de bloqueo.

## Regla principal

Acciones importantes: mostrar deshabilitadas con motivo.  
Acciones menores: pueden ocultarse si no aplican.

## Tipos de acciones

| Tipo | Acciones |
|---|---|
| Primarias | cerrar, firmar, confirmar pago, dictar resolución, registrar cumplimiento |
| Secundarias | ver documentos, historial, notificaciones, hechos materiales |
| Transición | derivar, retornar, archivar, reingresar |
| Demo/técnicas | crear acta demo, reset, comprobante mock, medida posterior |

## ACTAS_EN_ENRIQUECIMIENTO

Mostrar:

- Ver detalle
- Ver hechos materiales
- Ver cerrabilidad
- Solicitar pago voluntario
- Dictar resolución
- Generar pieza/documento mock
- Firmar documento pendiente si existe
- Archivar

Deshabilitado útil:

| Acción | Motivo |
|---|---|
| Cerrar acta | Falta resultado final o existen bloqueantes |

## PENDIENTE_ANALISIS

Mostrar:

- Ver detalle
- Registrar descargo mock
- Solicitar pago voluntario
- Registrar pago informado
- Adjuntar comprobante mock
- Confirmar pago
- Observar pago
- Dictar resolución
- Registrar medida preventiva posterior
- Derivar a gestión externa
- Archivar
- Cerrar acta

Deshabilitado útil:

| Acción | Motivo |
|---|---|
| Cerrar acta | Falta resultado final compatible o hay pendientes |

## PENDIENTE_FIRMA

Mostrar:

- Ver documentos pendientes
- Firmar documento
- Ver hechos materiales
- Ver cerrabilidad
- Dictar resolución si corresponde
- Ver historial

Deshabilitado útil:

| Acción | Motivo |
|---|---|
| Cerrar acta | Documento pendiente de firma o bloqueantes activos |
| Firmar documento | No hay documentos pendientes de firma |

## PENDIENTE_NOTIFICACION

Mostrar:

- Ver documentos notificables
- Preparar notificación
- Enviar a notificación mock
- Ver hechos materiales
- Ver cerrabilidad

Deshabilitado útil:

| Acción | Motivo |
|---|---|
| Cerrar acta | Notificación pendiente o bloqueantes activos |

## EN_NOTIFICACION

Mostrar:

- Ver notificaciones
- Registrar acuse positivo
- Registrar acuse negativo
- Registrar vencimiento
- Generar reintento
- Volver a análisis si corresponde

Ocultar o deshabilitar:

| Acción | Motivo |
|---|---|
| Cerrar directamente | Acta en notificación |
| Dictar resolución nueva | Requiere volver a instancia operativa si aplica |

## GESTION_EXTERNA

Mostrar:

- Ver detalle
- Ver gestión externa
- Registrar retorno
- Re-derivar
- Ver historial

Bloquear:

| Acción | Motivo |
|---|---|
| Dictar resolución interna | Acta en gestión externa |
| Cerrar directamente | Acta en gestión externa |

## ARCHIVO

Mostrar:

- Ver detalle
- Ver motivo archivo
- Reingresar desde archivo
- Ver historial

Bloquear:

| Acción | Motivo |
|---|---|
| Dictar resolución | Acta archivada |
| Cerrar directamente | Debe reingresar antes de operar |
| Registrar pago/cumplimiento | Acta archivada |

## CERRADAS

Mostrar:

- Ver detalle
- Ver historial
- Ver documentos
- Ver notificaciones

No mostrar acciones de fondo.

## Acción cerrar acta

Habilitar solo si:

- resultado final compatible
- sin pendientes materiales/documentales
- no gestión externa
- no archivo
- no cerrada

Motivos:

| Caso | Motivo visible |
|---|---|
| Sin resultado | Falta resultado final compatible |
| Con bloqueantes | Existen pendientes materiales |
| Gestión externa | El acta está en gestión externa |
| Archivo | Debe reingresar antes de operar |
| Cerrada | El acta ya está cerrada |

## Acción dictar resolución

Permitida en instancias operativas salvo:

- `GESTION_EXTERNA`
- `ARCHIVO`
- `CERRADAS`

Debe estar permitida en:

- `ACTAS_EN_ENRIQUECIMIENTO`
- `PENDIENTE_ANALISIS`
- `PENDIENTE_FIRMA`

## Acción registrar cumplimiento material

Habilitar si:

- hay bloqueante material activo
- acta no está en gestión externa
- acta no está en archivo
- acta no está cerrada

Ejes actuales:

- levantar medida preventiva
- liberar rodado
- entregar documentación

No incluir decomiso todavía como eje.

## Acción medida preventiva posterior

Principalmente en:

- `PENDIENTE_ANALISIS`

No mezclar con:

- rodado/documentación de tránsito
- decomiso de bromatología

## Acción constatación temprana

Solo demo/técnica/regresión.

No presentarla como flujo principal de tránsito.

## Criterio de cierre

Listo cuando:

- cada bandeja tiene acciones
- acciones importantes tienen motivos
- cierre es explícito
- firma/notificación/cumplimiento no se mezclan
- gestión externa, archivo y cerradas bloquean correctamente