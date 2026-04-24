# UX Demo — Layout, bandejas y listado

## Objetivo

Definir pantalla principal: header, bandejas, listado y selección de acta.

## Layout

Zonas:

1. Header
2. Sidebar de bandejas
3. Lista central de actas
4. Panel derecho de detalle

Esquema:

Header: sistema, búsqueda, crear acta demo, reset demo.  
Sidebar: bandejas y alertas.  
Centro: listado, filtros, orden.  
Derecha: detalle y acciones.

## Header

Mostrar:

- nombre sistema
- búsqueda por número de acta
- botón `Crear acta demo`
- botón `Reset demo`
- badge `Modo demo`
- badge `Datos mock`

## Bandejas macro

| Código | Label |
|---|---|
| `ACTAS_EN_ENRIQUECIMIENTO` | Actas en enriquecimiento |
| `PENDIENTE_ANALISIS` | Pendientes de análisis |
| `PENDIENTE_FIRMA` | Pendientes de firma |
| `PENDIENTE_NOTIFICACION` | Pendientes de notificación |
| `EN_NOTIFICACION` | En notificación |
| `GESTION_EXTERNA` | Gestión externa |
| `ARCHIVO` | Archivo |
| `CERRADAS` | Cerradas |

Cada bandeja muestra contador.

## Alertas transversales

No son necesariamente bandejas. Pueden ser filtros rápidos.

- Pagos informados
- Bloqueantes materiales
- Medidas activas
- Documentos pendientes de firma
- Notificaciones vencidas
- Cerrables

## Filtros por bandeja

Usar chips con contador.

### Dependencia

- Tránsito
- Inspecciones
- Fiscalización
- Bromatología

### Condición

- Con bloqueantes
- Sin bloqueantes
- Con pago informado
- Con pago confirmado
- Con medida activa
- Con rodado retenido
- Con documentación retenida
- Con decomiso
- Pendiente de firma
- Cerrable
- No cerrable

## Lista de actas

Fila mínima:

- número acta
- dependencia
- bandeja
- resumen operativo
- badges
- último evento

Ejemplo:

ACTA-0024  
Tránsito · Actas en enriquecimiento  
Rodado retenido · Documentación retenida · Pago confirmado  
Badges: `PAGO_CONFIRMADO`, `3 BLOQUEANTES`, `NO CERRABLE`

## Badges

Máximo 3 a 5 visibles por fila.

Prioridad:

1. cerrabilidad
2. bloqueantes
3. pago/resultado
4. firma/notificación
5. dependencia material relevante

Badges posibles:

- `PAGO_INFORMADO`
- `PAGO_CONFIRMADO`
- `ABSUELTO`
- `NO_CERRABLE`
- `CERRABLE`
- `CON_BLOQUEANTES`
- `MEDIDA_ACTIVA`
- `RODADO_RETENIDO`
- `DOCUMENTACION_RETENIDA`
- `DECOMISO`
- `PENDIENTE_FIRMA`
- `NOTIFICACION_VENCIDA`
- `GESTION_EXTERNA`
- `ARCHIVADA`

## Orden default

Prioridad sugerida:

1. con acción urgente
2. con bloqueantes
3. con pago informado
4. con notificación vencida
5. fecha/número

## Selección

Al seleccionar fila:

- marcar fila activa
- cargar detalle
- cargar acciones
- no navegar de pantalla salvo necesidad

## Refresco post-acción

Después de acción:

- refrescar detalle
- refrescar lista
- refrescar contadores
- si cambió bandeja, mostrar aviso

Ejemplo:

ACTA-0024 pasó de Actas en enriquecimiento a Pendientes de análisis.

## Estados vacíos

Sin actas:

- No hay actas en esta bandeja.

Sin filtro:

- No hay actas que coincidan con este filtro.

## Criterio de cierre

Listo cuando:

- hay sidebar con bandejas
- hay contadores
- hay filtros
- hay lista
- hay selección de acta
- hay badges útiles
- refresca luego de acciones