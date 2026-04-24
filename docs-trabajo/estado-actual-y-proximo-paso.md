# ESTADO ACTUAL Y PRÓXIMO PASO — PROTOTIPO BACKEND FALTAS

## Propósito del archivo

Este archivo registra el **estado operativo vigente** del prototipo backend.

No es fuente de verdad de dominio.  
No reemplaza la `spec/`.  
No reemplaza el `prompt-de-reanudacion-chat.md`.

Su función es dejar visible, de forma compacta y práctica:

- qué capacidades reales tiene hoy el prototipo
- qué decisiones ya quedaron consolidadas
- qué casos demo existen
- qué falta realmente
- cuál es el próximo paso natural

---

## 1. Estado actual resumido

El prototipo backend ya cubre un conjunto funcional importante del circuito demo.

### Capacidades hoy consolidadas

- piezas múltiples por expediente
- firma individual por documento
- salida correcta desde `PENDIENTE_FIRMA` solo cuando todas las piezas firmables están firmadas
- nulidad post-firma como salida terminal invalidante a `CERRADAS`
- notificación con variantes mínimas útiles para demo
- archivo con reingreso explícito
- gestión externa con derivación y retorno
- cierre desde análisis sujeto a regla de cerrabilidad
- pago voluntario temprano
- pago informado
- comprobante mock
- pago pendiente de confirmación
- pago confirmado
- pago observado
- resultado final compatible con cierre:
  - `ABSUELTO`
  - `PAGO_CONFIRMADO`
- cerrabilidad unificada
- bloqueantes materiales/documentales
- separación entre:
  - origen material
  - resolutorio documental
  - firma documental
  - notificación documental
  - cumplimiento material efectivo
- vista de hechos materiales separada de la lectura de cerrabilidad
- condiciones materiales de tránsito nacidas desde datos mock del acta
- medida preventiva posterior en contravención durante trámite administrativo
- resoluciones permitidas desde `ENRIQUECIMIENTO` y otras instancias operativas
- recorridos demo reproducibles de punta a punta

---

## 2. Decisiones funcionales y estructurales ya cerradas

### 2.1 Firma

- el circuito viejo `pasar-a-notificacion` ya no es la verdad del sistema
- la verdad vigente es la firma por documento
- la operación central es `firmarDocumento(actaId, documentoId)`
- los documentos tienen identidad propia
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todas las piezas firmables quedaron en `FIRMADO`
- un documento resolutivo puede estar pendiente de firma aunque el acta esté en una bandeja operativa distinta

### 2.2 Firma y notificación documental

Regla consolidada:

- generar/dictar una resolución no equivale a firmarla
- firmar una resolución no equivale a notificarla
- notificar una resolución no equivale a cumplimiento material efectivo

Si un documento resolutivo requiere firma:

- nace `PENDIENTE_FIRMA`
- no queda firmado automáticamente
- debe pasar por el circuito/mock de firma vigente

Si además requiere notificación:

- no debe quedar notificable antes de firmarse
- luego de firmado puede quedar listo/pendiente para notificación según el modelo mock vigente
- no se implementa notificación real en este prototipo

Regla material:

- ni generar el documento
- ni firmarlo
- ni notificarlo

liberan por sí solos un bloqueo material.

El bloqueo material se libera solo por cumplimiento material efectivo.

### 2.3 Nulidad

- la nulidad se modela como **pieza no-fallo**
- no existe bandeja terminal `NULAS`
- si el último documento firmado es de tipo `NULIDAD`, el expediente pasa a `CERRADAS`
- nulidad post-firma **no** entra al circuito común de notificación
- la nulidad post-firma es una salida terminal invalidante dentro del prototipo

### 2.4 Pago

El prototipo distingue entre:

- solicitud de pago voluntario
- pago informado
- comprobante adjunto mock
- pendiente de confirmación
- pago confirmado
- pago observado

Reglas vigentes:

- informar pago no equivale a pago confirmado
- adjuntar comprobante no equivale a pago confirmado
- la confirmación mock del pago produce `resultadoFinal = PAGO_CONFIRMADO`
- `PAGO_CONFIRMADO` **no** cierra automáticamente

### 2.5 Cerrabilidad

La regla consolidada es:

Cerrable = (`ABSUELTO` o `PAGO_CONFIRMADO`) y sin pendientes materiales/documentales activos.

Eso implica:

- ni `ABSUELTO`
- ni `PAGO_CONFIRMADO`

cierran por sí solos el expediente.

El cierre sigue siendo una acción posterior y explícita.

### 2.6 Bloqueantes materiales/documentales

Bloqueantes mínimos vigentes:

- `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- `LIBERACION_RODADO`
- `ENTREGA_DOCUMENTACION`

Reglas vigentes:

- el resolutorio documental no basta por sí solo
- la firma del resolutorio no basta por sí sola
- la notificación del resolutorio no basta por sí sola
- debe existir cumplimiento material efectivo
- el expediente no queda cerrable mientras exista al menos un bloqueante activo

### 2.7 Hechos materiales

El prototipo separa:

- plano documental del expediente
- plano de hechos materiales
- plano de cerrabilidad

En la lectura del detalle de acta existe una vista separada de `hechosMateriales`, independiente de la vista de `cerrabilidad`.

`hechosMateriales` expone:

- ejes materiales
- fase del eje
- `ejeBloqueanteCierre`
- `lecturaOperativa`

La lectura distingue:

- condiciones materiales tempranas/iniciales
- medida preventiva posterior durante trámite
- resolutorios existentes sin cumplimiento material efectivo

### 2.8 Tránsito: condiciones materiales desde datos del acta

Para tránsito, retención documental y retención/secuestro de rodado no se tratan como “botones” operativos del circuito.

Regla vigente:

- nacen como datos del acta de tránsito mock
- se modelan mediante `ActaTransitoMock`
- proyectan anclas documentales/orígenes materiales
- alimentan:
  - `hechosMateriales`
  - `pendientesBloqueantesCierre`
  - `lecturaOperativa`
  - cerrabilidad

`ACTA-0024` ya no depende de ejecutar acciones de constatación temprana para nacer con estas condiciones.

No existe `hechosMateriales.accionesDisponibles`.

### 2.9 Endpoint de constatación material temprana

Sigue existiendo:

- `registrar-constatacion-material-temprana`

Pero su rol vigente es de herramienta demo/técnica/regresión, no verdad principal de `ACTA-0024`.

Reglas vigentes:

- restringido a etapa válida D1/D2
- controla duplicados
- rechaza fuera de etapa válida
- no debe presentarse como mecanismo principal para datos constitutivos del acta de tránsito

`ACTA-0025` queda como caso de regresión para este endpoint.

### 2.10 Contravención: medida preventiva posterior

En contravenciones, una medida preventiva puede:

- nacer en el labrado
- nacer después durante trámite administrativo
- originarse por inspección posterior
- originarse por noticia administrativa o nuevo hecho vinculado

Ejemplo conceptual:

- rotura de faja de clausura

Esto puede terminar siendo otra acta según criterio, pero el modelo no debe impedir que, durante el proceso de una contravención existente, nazca una nueva medida preventiva.

`ACTA-0026` representa este caso.

Reglas vigentes:

- no se mezcla con `ActaTransitoMock`
- usa un endpoint específico:
  - `registrar-medida-preventiva-posterior`
- genera bloqueante:
  - `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- se ve en `hechosMateriales`
- afecta cerrabilidad
- requiere resolutorio documental
- requiere cumplimiento material efectivo

### 2.11 Resoluciones sobre el acta

Regla vigente:

Desde `ENRIQUECIMIENTO` ya debe ser posible dictar resoluciones sobre el acta.

En general, las resoluciones/resolutorios se admiten en instancias operativas, salvo:

- `GESTION_EXTERNA`
- `ARCHIVO`
- `CERRADAS`

No están limitadas rígidamente a `PENDIENTE_ANALISIS`.

Está probado que se admiten en:

- `ACTAS_EN_ENRIQUECIMIENTO`
- `PENDIENTE_ANALISIS`
- `PENDIENTE_FIRMA`

Regla documental:

- generar resolución no equivale a firmar
- firmar no equivale a notificar
- notificar no equivale a cumplimiento material

`DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF` existe como tipo documental, no como eje/bloqueante material.

El eje material sigue siendo:

- `LEVANTAMIENTO_MEDIDA_PREVENTIVA`

### 2.12 Archivo y reingreso

- existe macro-bandeja `ARCHIVO`
- existe motivo de archivo
- existe reingreso explícito
- el reingreso devuelve a `PENDIENTE_ANALISIS`
- queda marca operativa de revisión post reingreso
- mientras esté en `ARCHIVO`, no se admiten resoluciones internas

### 2.13 Gestión externa

- existe macro-bandeja `GESTION_EXTERNA`
- existen tipos mínimos útiles:
  - `APREMIO`
  - `JUZGADO_DE_PAZ`
- existe derivación
- existe retorno a análisis
- existe re-derivación
- el tipo de gestión externa se conserva como trazabilidad sintética
- mientras esté en `GESTION_EXTERNA`, no se admiten resoluciones internas

---

## 3. Acciones y lecturas hoy vigentes

## 3.1 Lecturas principales

El prototipo permite como mínimo:

- listar actas
- consultar detalle de una acta
- ver bloque actual, bandeja, situación operativa y acción pendiente
- ver estado documental y piezas
- ver documentos del expediente
- ver notificaciones mock asociadas
- ver cerrabilidad
- ver bloqueantes de cierre
- ver hechos materiales
- ver lectura operativa de hechos materiales
- ver eventos relevantes del caso demo

## 3.2 Acciones principales hoy disponibles

### Documentales / resolutivas

- generar piezas/documentos según el circuito demo
- firmar documento individual
- generar nulidad en el caso demo correspondiente
- registrar resolutorio documental de bloqueante
- registrar resolutorio con variante documental que requiere firma/notificación para levantamiento de medida

### Notificación

- registrar resultado de notificación
- contemplar positivo / negativo / vencido / reintentos según el caso demo
- no se implementa notificación real de resoluciones firmadas en este prototipo

### Archivo / reingreso

- archivar
- reingresar desde archivo

### Gestión externa

- derivar a gestión externa
- retornar a análisis
- re-derivar

### Pago

- solicitar pago voluntario
- registrar pago informado
- adjuntar comprobante mock
- confirmar pago informado
- observar pago informado

### Cerrabilidad / bloqueantes

- consultar cerrabilidad
- registrar resolutorio documental de bloqueante
- registrar cumplimiento material de bloqueante
- cerrar expediente si ya está en condición de cierre

### Condiciones materiales / medidas

- registrar constatación material temprana como herramienta demo/técnica/regresión
- registrar medida preventiva posterior en contravención

### Utilidad demo

- reset del dataset demo

> Nota: los nombres exactos de rutas/URIs pueden evolucionar.  
> La verdad inmediata está en el controller actual del prototipo.

---

## 4. Casos demo vigentes relevantes

## 4.1 `ACTA-0012` — nulidad post-firma

Caso demo útil para validar:

- generación de nulidad
- firma de documento `NULIDAD`
- salida terminal a `CERRADAS`
- no paso por notificación común

## 4.2 `ACTA-0015` — gestión externa

Caso demo útil para validar:

- derivación a gestión externa
- retorno a análisis
- re-derivación

## 4.3 `ACTA-0018` — pago confirmado sin bloqueantes

Caso demo útil para validar:

- flujo de pago sin complejidad material adicional
- recorrido económico simple dentro del prototipo

## 4.4 `ACTA-0019` — absolución + bloqueantes

Caso demo útil para validar:

- `resultadoFinal = ABSUELTO`
- presencia de bloqueantes materiales
- no cerrabilidad mientras existan
- resolutorio documental
- cumplimiento material
- paso a cerrable
- cierre posterior

## 4.5 `ACTA-0020` — resolución en `PENDIENTE_FIRMA`

Caso demo útil para validar:

- existencia de bloqueante activo en `PENDIENTE_FIRMA`
- posibilidad de registrar resolutorio desde una bandeja operativa distinta de `ENRIQUECIMIENTO` y `PENDIENTE_ANALISIS`
- el resolutorio no mueve artificialmente la bandeja
- el bloqueante sigue activo hasta cumplimiento material efectivo

## 4.6 `ACTA-0021` — pago confirmado precargado + bloqueantes

Caso demo útil para validar:

- `resultadoFinal = PAGO_CONFIRMADO`
- bloqueantes materiales
- cerrabilidad material
- sin depender del flujo real de pago

## 4.7 `ACTA-0022` — flujo real de pago + bloqueantes

Caso demo útil para validar:

- solicitud de pago voluntario
- pago informado
- comprobante
- confirmación de pago
- entrada efectiva a `PAGO_CONFIRMADO`
- bloqueantes materiales
- resolutorio + cumplimiento material
- cerrabilidad y cierre posterior

## 4.8 `ACTA-0023` — foco en vista de hechos materiales

Caso demo útil para validar:

- separación de lectura entre expediente documental y hecho material
- resolutorio en expediente sin hecho material cumplido
- lectura fina del eje material

## 4.9 `ACTA-0024` — tránsito con condiciones materiales desde `ActaTransitoMock`

Caso demo útil para validar:

- condiciones materiales de tránsito nacidas desde datos mock del acta
- retención documental
- rodado retenido/secuestrado
- medida preventiva aplicable
- proyección a hechos materiales
- proyección a bloqueantes
- lectura operativa
- circuito de pago
- resolutorios documentales
- resolutorio con circuito firma/notificación documental
- cumplimiento material efectivo
- cerrabilidad
- cierre explícito

Es hoy el mejor caso de recorrido integrado de punta a punta.

## 4.10 `ACTA-0025` — regresión de constatación temprana D1/D2

Caso demo/técnico útil para validar:

- endpoint `registrar-constatacion-material-temprana`
- restricción a D1/D2
- rechazo fuera de etapa válida
- control de duplicados
- no contaminación de `ACTA-0024`

## 4.11 `ACTA-0026` — contravención con medida preventiva posterior

Caso demo útil para validar:

- acta de contravención sin medida preventiva inicial
- medida preventiva nacida durante trámite administrativo
- ejemplo conceptual: rotura de faja de clausura
- bloqueante `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- hechos materiales
- lectura operativa de medida posterior
- resolutorio documental
- cumplimiento material efectivo
- cerrabilidad
- cierre explícito

---

## 5. Recorrido demo integrado hoy disponible

El recorrido más completo y reproducible sigue siendo `ACTA-0024`.

Secuencia funcional esperable:

1. reset del dataset demo
2. consultar `ACTA-0024`
3. verificar condiciones materiales nacidas desde `ActaTransitoMock`
4. verificar `hechosMateriales`
5. verificar `pendientesBloqueantesCierre`
6. verificar `lecturaOperativa`
7. solicitar pago voluntario
8. registrar pago informado
9. adjuntar comprobante mock
10. confirmar pago
11. verificar que el expediente no sea cerrable si persisten bloqueantes
12. registrar resolutorio documental por cada eje bloqueante
13. verificar que el expediente todavía no sea cerrable si falta cumplimiento material
14. opcionalmente registrar resolutorio de levantamiento de medida con documento que requiere firma/notificación
15. firmar el documento resolutivo si corresponde
16. verificar que la firma no libera cumplimiento material
17. registrar cumplimiento material efectivo por cada eje
18. verificar `cerrable = true`
19. ejecutar cierre final explícito

También existe recorrido complementario `ACTA-0026` para demostrar medida preventiva posterior en contravención.

---

## 6. Refactors / limpiezas ya consolidadas

- eliminado el circuito conceptual viejo basado en “pasar a notificación” como verdad única
- consolidada firma individual por documento
- consolidado comportamiento correcto de nulidad post-firma
- unificada regla de cerrabilidad para:
  - `ABSUELTO`
  - `PAGO_CONFIRMADO`
- eliminado el supuesto falso de cierre automático por pago
- eliminada la idea de que el resolutorio documental basta por sí solo
- separada la lectura entre expediente documental y hecho material
- eliminado `hechosMateriales.accionesDisponibles`
- corregido `ACTA-0024` para que tránsito nazca desde datos mock del acta, no desde botones operativos
- agregado `ActaTransitoMock`
- agregado caso `ACTA-0026` para medida preventiva posterior en contravención
- separado eje material de circuito firma/notificación documental
- evitado que `DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF` sea un bloqueante material
- blindada resolución desde bandeja operativa adicional `PENDIENTE_FIRMA`

---

## 7. Qué falta realmente

Lo pendiente real hoy, de forma resumida, es:

### 7.1 Consolidar y versionar cambios

Hay varios archivos modificados y archivos nuevos sin tracking respecto de `HEAD`.

Antes de cerrar el bloque backend demo, conviene:

- revisar diff final
- asegurar que los nuevos archivos entren al control de versiones
- commitear el estado consistente

### 7.2 Revisión final de nombres y narrativa demo

Hay nombres de endpoints o tests que pueden requerir aclaración en el guion demo:

- `registrar-resolucion-bloqueo-cierre`
- `registrar-constatacion-material-temprana`
- algún nombre de test con typo o redacción fea

No es urgente funcionalmente, pero puede mejorar claridad.

### 7.3 Bordes no cubiertos por test específico

La regla de resoluciones admite varias bandejas operativas.

Hoy hay cobertura explícita en:

- `ACTAS_EN_ENRIQUECIMIENTO`
- `PENDIENTE_ANALISIS`
- `PENDIENTE_FIRMA`

Y rechazo en:

- `GESTION_EXTERNA`
- `ARCHIVO`
- `CERRADAS`

No hay tests dedicados para todas las demás bandejas operativas posibles.

### 7.4 No pendiente inmediato

No están en foco inmediato:

- seguridad real
- base de datos real
- integraciones reales
- motor documental real
- PDFs reales
- firma digital real
- tesorería real
- notificación real de resoluciones

---

## 8. Riesgos / bordes conocidos

- el prototipo sigue siendo in-memory y demo-oriented
- varias representaciones siguen simplificadas para favorecer navegabilidad y velocidad de iteración
- algunos endpoints siguen siendo herramientas demo/técnicas y no contratos finales
- `registrar-constatacion-material-temprana` debe explicarse como regresión/demo, no como verdad principal de tránsito
- el modelo de notificación real de resoluciones firmadas no está implementado
- los documentos resolutivos con firma/notificación están representados solo a nivel mock
- algunas bandejas operativas adicionales admiten resoluciones por regla general, pero no tienen test específico dedicado

---

## 9. Próximo paso natural

El próximo paso natural es **cerrar el bloque backend demo**, no abrir funcionalidad grande.

Orden recomendado:

1. revisar diff final
2. asegurar que archivos nuevos estén incorporados
3. ejecutar `mvn test`
4. corregir nombres mínimos si hay alguno muy confuso
5. commitear estado backend demo
6. recién después evaluar si queda algún micro-slice o si se pasa a otra capa

No conviene abrir ahora frentes grandes como DB real, seguridad real, PDFs reales o integraciones.

---

## 10. Regla operativa para continuar

Para próximos slices:

- usar `spec/` como fuente principal de dominio
- usar este archivo como inventario vivo
- usar `prompt-de-reanudacion-chat.md` como marco metodológico
- mantener slices mínimos
- no mezclar problemas conceptuales distintos
- tocar la menor cantidad posible de archivos
- no tocar archivos de continuidad salvo autorización explícita
- distinguir siempre:
  - dato del acta
  - acción demo/técnica
  - documento resolutivo
  - firma
  - notificación
  - cumplimiento material
  - cierre