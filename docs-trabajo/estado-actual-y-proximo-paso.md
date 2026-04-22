# ESTADO ACTUAL - PROTOTIPO FALTAS

> Marco metodologico, reglas de trabajo y criterio de contexto: ver `docs-trabajo/prompt-de-reanudacion-chat.md`, `.cursor/rules/contexto-minimo.mdc` y `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`.

## Nivel de avance

El prototipo backend modela de forma fiel:

- produccion de multiples piezas no-fallo
- permanencia en bandeja hasta completar piezas requeridas
- firma individual por documento
- salida de bandeja segun completitud real de documentos firmables
- paso a notificacion por firma completa de documentos
- retorno a analisis por resultados alternativos de notificacion
- separacion operativa dentro de `PENDIENTE_ANALISIS` mediante `accionPendiente`
- cierre y archivo desde analisis en casos demo concretos

## Endpoints de lectura vigentes

- `GET /api/prototipo/health`
- `GET /api/prototipo/bandejas`
- `GET /api/prototipo/bandejas/{codigo}/actas`
- `GET /api/prototipo/actas/{id}`
- `GET /api/prototipo/actas/{id}/eventos`
- `GET /api/prototipo/actas/{id}/documentos`
- `GET /api/prototipo/actas/{id}/notificaciones`

## Acciones implementadas

- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-positiva`
- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-negativa`
- `POST /api/prototipo/actas/{id}/acciones/reintentar-notificacion`
- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-vencida`
- `POST /api/prototipo/actas/{id}/acciones/cerrar-acta`
- `POST /api/prototipo/actas/{id}/acciones/archivar-acta`
- `POST /api/prototipo/actas/{id}/acciones/generar-medida-preventiva`
- `POST /api/prototipo/actas/{id}/acciones/generar-notificacion-acta`
- `POST /api/prototipo/actas/{id}/acciones/firmar-documento/{documentoId}`

## Endpoint de utilidad

- `POST /api/prototipo/reset` -- reinicializa el escenario mock al estado inicial

## Refactors y decisiones cerradas

### Bandeja nueva
- `PENDIENTES_RESOLUCION_REDACCION`

### Multiples piezas
- existen `piezasRequeridas` y `piezasGeneradas`
- si faltan piezas, permanece en `PENDIENTES_RESOLUCION_REDACCION`
- si todas estan producidas, pasa a `PENDIENTE_FIRMA`

### Estados corregidos
- `PENDIENTE_PRODUCCION_PIEZAS`
- `PENDIENTE_FIRMA_PIEZAS`
- ya no depende de la primera pieza pendiente ni de la ultima generada

### Firma correcta
- documentos con `documentoId`
- nacen en `PENDIENTE_FIRMA`
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los firmables estan en `FIRMADO`

### Circuito viejo eliminado
- se elimino `pasarActaANotificacion`
- existe un unico modelo de firma / transicion a notificacion

### Resultados de notificacion ya modelados
- positiva
- negativa
- reintento
- vencida

### Marcas operativas ya modeladas
- `REINTENTAR_NOTIFICACION`
- `EVALUAR_NOTIFICACION_VENCIDA`

## Casos demo validados

### ACTA-0013
Caso canonico actual. Requiere `NOTIFICACION_ACTA` y `MEDIDA_PREVENTIVA`.

Recorrido validado:
1. generar medida preventiva
2. generar notificacion del acta
3. pasar a `PENDIENTE_FIRMA`
4. firmar primer documento -> seguir en `PENDIENTE_FIRMA`
5. firmar segundo documento -> pasar a `PENDIENTE_NOTIFICACION`
6. registrar notificacion positiva -> pasar a `PENDIENTE_ANALISIS`
7. cerrar acta desde analisis -> pasar a `CERRADAS`

Expresa:
- multiples piezas
- multiples documentos
- firma individual
- transicion realista a notificacion
- retorno a analisis
- cierre administrativo coherente

### ACTA-0006
Caso corto validado para archivo desde analisis.

Recorrido validado:
1. iniciar en `PENDIENTE_ANALISIS`
2. archivar acta
3. pasar a `ARCHIVO`

Expresa:
- archivo desde analisis
- evento explicito de archivado
- posibilidad de reingreso preservada

### ACTA-0004
Caso validado para notificacion negativa y reintento.

Recorrido validado:
1. iniciar en `PENDIENTE_NOTIFICACION`
2. registrar notificacion negativa
3. pasar a `PENDIENTE_ANALISIS`
4. quedar con `accionPendiente = REINTENTAR_NOTIFICACION`
5. aparecer filtrada en `PENDIENTE_ANALISIS?accionPendiente=REINTENTAR_NOTIFICACION`
6. reintentar notificacion
7. volver a `PENDIENTE_NOTIFICACION`
8. limpiar `accionPendiente`

Expresa:
- resultado negativo de notificacion
- separacion operativa dentro de analisis
- filtro por `accionPendiente`
- reintento como salida puntual del caso

### ACTA-0005
Caso validado para notificacion vencida.

Recorrido validado:
1. iniciar en `EN_NOTIFICACION`
2. registrar notificacion vencida
3. pasar a `PENDIENTE_ANALISIS`
4. quedar con `accionPendiente = EVALUAR_NOTIFICACION_VENCIDA`
5. notificacion en estado `VENCIDA`
6. evento `NOTIFICACION_VENCIDA`
7. aparecer filtrada en `PENDIENTE_ANALISIS?accionPendiente=EVALUAR_NOTIFICACION_VENCIDA`

Expresa:
- vencimiento de notificacion
- diferenciacion respecto de no entrega
- clasificacion operativa en analisis para decision posterior

## Que falta (funcionalidad aun no modelada)

- decision posterior sobre notificacion vencida
- reintento especifico para casos vencidos, si el modelo lo requiere
- ampliar mas piezas no-fallo
- ampliar mas circuitos de analisis y decisiones
- ampliar cobertura de bandejas del universo completo
- decidir si `accionPendiente` permanece como campo paralelo o sube luego a una estructura mas explicita
- limpiar temas de encoding/comentarios en algunos archivos Java

## Proximo paso

**El proximo slice funcional aun no esta elegido.**

Opciones candidatas actuales:
1. decision posterior sobre notificacion vencida
2. reintento especifico para casos vencidos
3. mas piezas no-fallo
4. otros circuitos de analisis y decisiones
5. micro-slice tecnico de limpieza de encoding/comentarios
6. otro caso demo fuerte

Elegir una opcion y redactar el slice antes de abrir codigo.

## Instruccion al retomar

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir dos modelos contradictorios.  
No abrir contexto de mas.  
No permitir que Cursor lea o edite continuidad sin autorizacion explicita.  
Si aparece una simplificacion que rompe el modelo final, corregirla ahora.