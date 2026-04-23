# ESTADO ACTUAL - PROTOTIPO FALTAS

> Marco metodológico, reglas de trabajo y criterio de contexto: ver `docs-trabajo/prompt-de-reanudacion-chat.md`, `.cursor/rules/contexto-minimo.mdc` y `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`.

## Nivel de avance

El prototipo backend modela de forma fiel:

- producción de múltiples piezas no-fallo
- permanencia en bandeja hasta completar piezas requeridas
- firma individual por documento
- salida de bandeja según completitud real de documentos firmables
- paso a notificación por firma completa de documentos
- resultados alternativos de notificación
- retorno a análisis por resultados alternativos de notificación
- separación operativa dentro de `PENDIENTE_ANALISIS` mediante `accionPendiente`
- semántica mínima de archivo mediante `motivoArchivo`
- reingreso desde archivo
- cierre desde análisis
- gestión externa con ida, vuelta y re-derivación
- nulidad correctamente reencuadrada como pieza no-fallo
- store descompuesto por áreas funcionales

## Endpoints de lectura vigentes

- `GET /api/prototipo/health`
- `GET /api/prototipo/bandejas`
- `GET /api/prototipo/bandejas/{codigo}/actas`
- `GET /api/prototipo/actas/{id}`
- `GET /api/prototipo/actas/{id}/eventos`
- `GET /api/prototipo/actas/{id}/documentos`
- `GET /api/prototipo/actas/{id}/notificaciones`

## Acciones implementadas

### Notificación
- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-positiva`
- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-negativa`
- `POST /api/prototipo/actas/{id}/acciones/reintentar-notificacion`
- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-vencida`
- `POST /api/prototipo/actas/{id}/acciones/reintentar-notificacion-vencida`

### Archivo / reingreso
- `POST /api/prototipo/actas/{id}/acciones/archivar-acta`
- `POST /api/prototipo/actas/{id}/acciones/archivar-por-vencimiento`
- `POST /api/prototipo/actas/{id}/acciones/reingresar-acta`

### Cierre
- `POST /api/prototipo/actas/{id}/acciones/cerrar-acta`

### Gestión externa
- `POST /api/prototipo/actas/{id}/acciones/derivar-a-apremio`
- `POST /api/prototipo/actas/{id}/acciones/derivar-a-juzgado-de-paz`
- `POST /api/prototipo/actas/{id}/acciones/reingresar-desde-gestion-externa`

### Piezas / firma
- `POST /api/prototipo/actas/{id}/acciones/generar-medida-preventiva`
- `POST /api/prototipo/actas/{id}/acciones/generar-notificacion-acta`
- `POST /api/prototipo/actas/{id}/acciones/generar-nulidad`
- `POST /api/prototipo/actas/{id}/acciones/firmar-documento/{documentoId}`

## Endpoint de utilidad

- `POST /api/prototipo/reset` — reinicializa el escenario mock al estado inicial

## Refactors y decisiones cerradas

### Bandejas relevantes actualmente visibles
- `ACTAS_EN_ENRIQUECIMIENTO`
- `PENDIENTE_PREPARACION_DOCUMENTAL`
- `PENDIENTE_FIRMA`
- `PENDIENTE_NOTIFICACION`
- `EN_NOTIFICACION`
- `PENDIENTE_ANALISIS`
- `PENDIENTES_RESOLUCION_REDACCION`
- `GESTION_EXTERNA`
- `ARCHIVO`
- `CERRADAS`

### Múltiples piezas
- existen `piezasRequeridas` y `piezasGeneradas`
- si faltan piezas, permanece en `PENDIENTES_RESOLUCION_REDACCION`
- si todas están producidas, pasa a `PENDIENTE_FIRMA`

### Estados corregidos
- `PENDIENTE_PRODUCCION_PIEZAS`
- `PENDIENTE_FIRMA_PIEZAS`

### Firma correcta
- documentos con `documentoId`
- nacen en `PENDIENTE_FIRMA`
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los firmables están en `FIRMADO`

### Circuito viejo eliminado
- se eliminó `pasarActaANotificacion`
- existe un único modelo de firma / transición a notificación

### Resultados de notificación modelados
- positiva
- negativa
- reintento por no entrega
- vencida
- reintento post-vencimiento

### Marcas operativas ya modeladas
- `REINTENTAR_NOTIFICACION`
- `EVALUAR_NOTIFICACION_VENCIDA`
- `REVISION_POST_REINGRESO`
- `DERIVAR_GESTION_EXTERNA`
- `REVISION_POST_GESTION_EXTERNA`

### Archivo
- `ARCHIVO` sigue siendo macro-bandeja
- existe `motivoArchivo`
- motivos modelados:
  - `ARCHIVO_DESDE_ANALISIS_DIRECTO`
  - `ARCHIVO_POST_EVALUACION_VENCIMIENTO`

### Reingreso desde archivo
- vuelve a `PENDIENTE_ANALISIS`
- preserva `motivoArchivo`
- deja marca `REVISION_POST_REINGRESO`

### Gestión externa
- existe visibilidad previa en análisis de casos listos para derivación
- existe `GESTION_EXTERNA` como macro-bandeja
- tipos vigentes:
  - `APREMIO`
  - `JUZGADO_DE_PAZ`
- existe derivación efectiva a ambos tipos
- existe retorno efectivo a `PENDIENTE_ANALISIS`
- existe re-derivación efectiva desde `REVISION_POST_GESTION_EXTERNA`
- `tipoGestionExterna` se preserva como trazabilidad sintética
- `permiteReingreso` no se consume artificialmente al retornar
- la salida vuelve a quedar bloqueada naturalmente si el expediente abandona análisis por una salida terminal o equivalente

### Nulidad
- nulidad quedó reencuadrada correctamente como pieza no-fallo
- no existe bandeja terminal `NULAS`
- `ACTA-0012` es el caso demo alineado con spec
- ya existe acción `generar-nulidad`
- genera documento `NULIDAD` pendiente de firma
- usa el mismo agregador de piezas existente
- si completa piezas, pasa a `PENDIENTE_FIRMA`

### Cierre
- existe cierre explícito desde análisis
- lleva a `CERRADAS`
- limpia marcas operativas cuando corresponde
- genera evento `CIERRE_ANALISIS`

### Refactor táctico ya realizado
- archivo + reingreso separados por área funcional
- notificación separada por área funcional
- piezas + firma separadas por área funcional
- gestión externa separada por área funcional
- cierre separado por área funcional
- `PrototipoStore` quedó como fachada pública liviana
- existe `PrototipoConstantes` para frontera mínima entre áreas

## Casos demo validados

### ACTA-0013
Caso canónico actual. Requiere `NOTIFICACION_ACTA` y `MEDIDA_PREVENTIVA`.

Recorrido validado:
1. generar medida preventiva
2. generar notificación del acta
3. pasar a `PENDIENTE_FIRMA`
4. firmar primer documento -> seguir en `PENDIENTE_FIRMA`
5. firmar segundo documento -> pasar a `PENDIENTE_NOTIFICACION`
6. registrar notificación positiva -> pasar a `PENDIENTE_ANALISIS`
7. cerrar acta desde análisis -> pasar a `CERRADAS`

### ACTA-0006
Caso validado para archivo directo y reingreso.

Recorrido validado:
1. iniciar en `PENDIENTE_ANALISIS`
2. archivar acta
3. pasar a `ARCHIVO`
4. quedar con `motivoArchivo = ARCHIVO_DESDE_ANALISIS_DIRECTO`
5. reingresar
6. volver a `PENDIENTE_ANALISIS`
7. quedar con `accionPendiente = REVISION_POST_REINGRESO`

### ACTA-0004
Caso validado para notificación negativa y reintento por no entrega.

Recorrido validado:
1. iniciar en `PENDIENTE_NOTIFICACION`
2. registrar notificación negativa
3. pasar a `PENDIENTE_ANALISIS`
4. quedar con `accionPendiente = REINTENTAR_NOTIFICACION`
5. aparecer filtrada en `PENDIENTE_ANALISIS?accionPendiente=REINTENTAR_NOTIFICACION`
6. reintentar notificación
7. volver a `PENDIENTE_NOTIFICACION`
8. limpiar `accionPendiente`

### ACTA-0005
Caso validado para notificación vencida, reintento post-vencimiento y archivo post evaluación de vencimiento.

Recorrido validado:
1. iniciar en `EN_NOTIFICACION`
2. registrar notificación vencida
3. pasar a `PENDIENTE_ANALISIS`
4. quedar con `accionPendiente = EVALUAR_NOTIFICACION_VENCIDA`
5. notificación en estado `VENCIDA`
6. evento `NOTIFICACION_VENCIDA`
7. aparecer filtrada en `PENDIENTE_ANALISIS?accionPendiente=EVALUAR_NOTIFICACION_VENCIDA`
8. reintentar notificación vencida -> volver a `PENDIENTE_NOTIFICACION`
9. o archivar por vencimiento -> pasar a `ARCHIVO` con `ARCHIVO_POST_EVALUACION_VENCIMIENTO`

### Gestión externa
Recorrido mínimo ya cubierto:
1. caso listo para gestión externa en análisis (`DERIVAR_GESTION_EXTERNA`)
2. derivación efectiva a `APREMIO` o `JUZGADO_DE_PAZ`
3. permanencia en `GESTION_EXTERNA`
4. reingreso a análisis (`REVISION_POST_GESTION_EXTERNA`)
5. re-derivación efectiva desde esa marca

### Nulidad
Caso demo vigente:
1. reset
2. `ACTA-0012` en `PENDIENTES_RESOLUCION_REDACCION`
3. `piezasRequeridas = ["NULIDAD"]`
4. `POST generar-nulidad`
5. pieza `NULIDAD` generada
6. documento `NULIDAD` en `PENDIENTE_FIRMA`
7. paso a `PENDIENTE_FIRMA` por completitud de piezas

## Qué falta (funcionalidad aún no modelada)

- decidir el comportamiento correcto post-firma de nulidad si la spec exige salida distinta del circuito común post-firma
- ampliar otras decisiones posteriores desde análisis si todavía faltan salidas fuertes
- ampliar cobertura funcional de inicio y enriquecimiento
- evaluar si hacen falta más casos demo intermedios de piezas combinadas
- micro-slice técnico de higiene de repo (`.gitignore` + `target/`) si se decide
- actualizar continuidad y estado dentro del repo si el usuario lo autoriza explícitamente y el estado actual local quedó viejo

## Estado técnico del backend

- `PrototipoStore` ya no concentra toda la lógica del prototipo
- supports existentes:
  - `ArchivoReingresoSupport`
  - `NotificacionSupport`
  - `PiezasFirmaSupport`
  - `GestionExternaSupport`
  - `CierreSupport`
- working tree local ya fue limpiado de ruido regenerable bajo `target/`
- sigue pendiente, si se decide, formalizar la higiene de repo con `.gitignore` y limpieza del índice

## Próximo paso

**El próximo slice funcional aún no está elegido.**

Candidatos razonables:
1. decidir el destino post-firma de nulidad si la spec no acompaña el circuito común
2. ampliar otras decisiones fuertes desde análisis
3. ampliar inicio / enriquecimiento
4. micro-slice de higiene de repo
5. actualización explícita de continuidad/estado en archivos del repo

## Instrucción al retomar

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir modelos contradictorios.  
No abrir contexto de más.  
No permitir que Cursor lea o edite continuidad sin autorización explícita.  
Separar por área funcional cuando haga falta, no por bandeja.  
Si aparece una simplificación que rompe el modelo final, corregirla ahora.