# PROMPT DE REANUDACIÓN — PROTOTIPO BACKEND FALTAS

Estamos retomando el prototipo backend del sistema de faltas municipal.

## Proyecto

- repo: `backend/api-faltas-prototipo`
- stack: Java 21 + Spring Boot 3.5.x
- sin base de datos real
- sin seguridad real
- store en memoria
- prototipo descartable en lo técnico, pero funcionalmente fiel al sistema real

## Norte del prototipo

Este prototipo no debe quedar como una demo de happy path.

La meta es que represente de forma navegable, accionable y coherente el comportamiento real del sistema, con simplificaciones técnicas controladas pero sin romper el modelo conceptual.

### Simplificaciones permitidas

- estado en memoria
- dataset mock
- sin DB real
- sin seguridad real
- sin integraciones reales
- acciones mock
- DTOs simples
- documentos mock
- circuitos operativos simplificados si mantienen fidelidad funcional

### Simplificaciones no permitidas

- happy paths irreales
- atajos conceptuales que contradigan el dominio
- convivencia de circuitos contradictorios
- cerrar automáticamente cuando el modelo real exige más condiciones
- confundir documento emitido con hecho material cumplido
- confundir resolución generada con resolución firmada
- confundir resolución firmada con resolución notificada
- confundir notificación con cumplimiento material
- deformar estados, transiciones, firma, notificación, archivo, gestión externa, reingreso o cierre por comodidad técnica

## Regla principal

La demo no debe ser un atajo conceptual.

Debe ser una base realista del sistema final.

Eso implica:

- un solo circuito verdadero por comportamiento
- estados agregadores correctos
- detalle fino en estructuras específicas
- transiciones coherentes con el modelo final
- fidelidad entre resultado final del expediente y cerrabilidad real
- separación estricta entre documento, acto, firma, notificación, cumplimiento material y cierre

## Estado de continuidad

Para inventario operativo vigente del prototipo, usar:

- `docs-trabajo/estado-actual-y-proximo-paso.md`

Ese archivo es la verdad volátil del prototipo.

Este prompt es el marco metodológico y estructural.

## Decisiones estructurales ya consolidadas

### Firma

- el circuito viejo `pasar-a-notificacion` fue eliminado
- la única verdad es `firmarDocumento(actaId, documentoId)`
- los documentos tienen identidad propia
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los documentos firmables están en `FIRMADO`

### Nulidad

- nulidad se trata como pieza no-fallo
- no existe bandeja terminal autónoma `NULAS`
- vive dentro del circuito documental/resolutivo
- el caso demo de nulidad se genera como documento `NULIDAD` pendiente de firma
- cuando el último documento firmado es de tipo `NULIDAD`, la acta pasa a `CERRADAS`
- nulidad post-firma no entra al circuito común de notificación
- la salida post-firma de nulidad es terminal/invalidante dentro del prototipo actual

### Piezas y agregadores

- existen `piezasRequeridas` y `piezasGeneradas`
- mientras falten piezas, la acta permanece en resolución/redacción
- si todas están producidas, pasa a `PENDIENTE_FIRMA`
- no usar el primer pendiente ni la última pieza como “estado”
- estados agregadores correctos:
  - `PENDIENTE_PRODUCCION_PIEZAS`
  - `PENDIENTE_FIRMA_PIEZAS`

### Notificación

- existen notificación positiva, negativa, vencida y reintentos
- los resultados alternativos pueden devolver a `PENDIENTE_ANALISIS`
- la separación operativa dentro de análisis se resuelve con `accionPendiente`
- no abrir bandejas nuevas por microcaso si alcanza con macro-bandeja + marca operativa visible y filtrable

### Archivo / reingreso

- `ARCHIVO` sigue siendo macro-bandeja
- existe semántica mínima de archivo mediante `motivoArchivo`
- existe reingreso explícito desde archivo
- el reingreso vuelve a `PENDIENTE_ANALISIS`
- deja marca `REVISION_POST_REINGRESO`

### Gestión externa

- existe macro-bandeja `GESTION_EXTERNA`
- tipos mínimos vigentes:
  - `APREMIO`
  - `JUZGADO_DE_PAZ`
- existe derivación efectiva
- existe retorno a análisis
- existe re-derivación
- el tipo de gestión externa se conserva como trazabilidad sintética

### Pago voluntario / circuito de pago

El prototipo ya distingue entre:

- solicitud de pago voluntario
- pago informado
- comprobante adjunto mock
- pago pendiente de confirmación
- pago confirmado
- pago observado

Reglas:

- el pago no se considera real por el solo hecho de ser informado
- el comprobante no basta por sí solo
- la confirmación mock de pago deja `resultadoFinal = PAGO_CONFIRMADO`
- `PAGO_CONFIRMADO` no cierra automáticamente el expediente

### Resultado final y cerrabilidad

Solo son compatibles con cierre:

- `ABSUELTO`
- `PAGO_CONFIRMADO`

Pero eso no significa cierre automático.

La regla consolidada es:

Cerrable = (`ABSUELTO` o `PAGO_CONFIRMADO`) y sin pendientes materiales/documentales activos.

### Pendientes materiales / documentales

Pendientes bloqueantes mínimos del prototipo:

- `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- `LIBERACION_RODADO`
- `ENTREGA_DOCUMENTACION`

Reglas:

- un documento resolutorio no equivale por sí solo al cumplimiento material
- la firma de un documento resolutorio no equivale al cumplimiento material
- la notificación de un documento resolutorio no equivale al cumplimiento material
- debe distinguirse entre:
  - origen material
  - documento resolutorio emitido
  - documento pendiente de firma si aplica
  - documento firmado si aplica
  - documento pendiente/listo para notificación si aplica
  - documento notificado si aplica
  - cumplimiento material efectivo
- el bloqueante desaparece por el cumplimiento material efectivo, no por la mera existencia del documento, firma o notificación

### Hechos materiales vs expediente documental

El prototipo separa la lectura entre:

- plano documental del expediente
- plano de hechos materiales
- plano de cerrabilidad

Se expone una vista de hechos materiales por eje, para distinguir:

- sin origen
- situación pendiente de resolutorio
- resolutorio en expediente sin hecho material
- cumplimiento material verificado

La cerrabilidad sigue dependiendo de la lógica consolidada, no del solo expediente documental.

### Condiciones materiales según origen

No todas las condiciones materiales nacen igual.

#### Tránsito

En actas de tránsito, la retención documental y la retención/secuestro de rodado deben tratarse como datos propios del acta de tránsito o de su satélite mock.

No deben modelarse como acciones disponibles genéricas del circuito.

Pueden proyectar:

- `hechosMateriales`
- `pendientesBloqueantesCierre`
- `lecturaOperativa`
- cerrabilidad

El caso `ACTA-0024` representa este criterio con `ActaTransitoMock`.

#### Contravención

En actas de contravención, una medida preventiva puede nacer:

- en el labrado del acta
- durante el trámite administrativo
- por inspección posterior
- por noticia administrativa o nuevo hecho vinculado

Ejemplo:

- rotura de faja de clausura

Según criterio, puede terminar generando otra acta, pero el modelo no debe impedir que una medida preventiva posterior nazca dentro del proceso administrativo de una contravención existente.

El caso `ACTA-0026` representa una medida preventiva posterior durante trámite.

#### Bromatología

En actas de bromatología, el decomiso de sustancias alimenticias debe tratarse como dato propio del acta o de su satélite mock.

No debe modelarse automáticamente como medida preventiva genérica.

Regla vigente:

- `decomisoSustanciasAlimenticias` es dato propio
- no genera `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- no genera `LIBERACION_RODADO`
- no genera `ENTREGA_DOCUMENTACION`
- un posible eje futuro podría ser `LIBERACION_DECOMISO`, pero no está implementado todavía

### Endpoint de constatación material temprana

El endpoint de constatación material temprana puede seguir existiendo como herramienta demo/técnica/regresión.

Pero no debe ser la verdad principal para datos constitutivos del acta de tránsito.

Reglas:

- debe estar restringido a D1/D2 o etapa temprana válida
- debe controlar duplicados
- no debe crear doble verdad con datos propios del acta
- no debe exponerse como “acciones disponibles” genéricas para tránsito

`ACTA-0025` puede usarse como caso de regresión del endpoint.

### Alta mock mínima de acta demo

El prototipo permite crear actas mock mínimas en vivo para la demo funcional-operativa.

Objetivo:

- permitir que usuarios del Tribunal creen casos durante la reunión
- validar circuitos y movimientos operativos con botones de acciones de negocio
- no depender únicamente de actas precargadas
- no reemplazar el labrado real final

Endpoint vigente:

- `POST /api/prototipo/actas/mock`

Reglas:

- genera numeración demo automática `ACTA-DEMO-0001+`
- nace en `ACTAS_EN_ENRIQUECIMIENTO`
- queda visible en detalle
- queda visible en listado de bandeja
- permite dependencias demo:
  - `TRANSITO`
  - `INSPECCIONES`
  - `FISCALIZACION`
  - `BROMATOLOGIA`
- permite flags mínimos por dependencia
- no carga personas, artículos, normativa, domicilios completos, documentos reales, PDFs ni integraciones

### Resoluciones desde instancias operativas

Desde `ENRIQUECIMIENTO` ya debe ser posible dictar resoluciones sobre el acta.

En general, las resoluciones sobre el acta pueden dictarse en instancias operativas, salvo:

- `GESTION_EXTERNA`
- `ARCHIVO`
- `CERRADAS`

No debe limitarse por defecto la generación de resoluciones a `PENDIENTE_ANALISIS`.

Regla vigente del prototipo:

- se admiten en `ACTAS_EN_ENRIQUECIMIENTO`
- se admiten en `PENDIENTE_ANALISIS`
- se admiten en `PENDIENTE_FIRMA`
- se rechazan en `GESTION_EXTERNA`
- se rechazan en `ARCHIVO`
- se rechazan en `CERRADAS`

### Resolución, firma, notificación y cumplimiento material

No asumir equivalencias falsas:

- dictar/generar resolución no equivale a firmarla
- firmar una resolución no equivale a notificarla
- notificar una resolución no equivale a cumplimiento material efectivo

Si el documento resolutivo requiere firma:

- debe quedar pendiente de firma
- debe pasar por el circuito/motor de firma vigente
- no debe tratarse como firmado automáticamente

Si además requiere notificación:

- no debe quedar notificable antes de firmarse
- recién luego de firmado puede quedar pendiente/listo para notificación, según el modelo vigente

Si la resolución se vincula con un bloqueo material:

- generar el documento no libera el bloqueo
- firmar el documento no libera el bloqueo
- notificar el documento no libera el bloqueo
- solo el cumplimiento material efectivo libera el bloqueo

Firma/notificación son propiedades o estados del documento resolutivo asociado, no nuevos ejes materiales de cierre.

Ejemplo correcto:

- eje material: `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- tipo documental posible: `DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF`

El tipo documental no debe convertirse en un bloqueante material separado.

### Bandeja operativa y documentos pendientes

El acta puede estar en una bandeja operativa determinada y, al mismo tiempo, tener documentos resolutivos pendientes de firma o pendientes/listos para notificación.

No asumir que todo el expediente debe moverse a una única bandeja global solo porque existe un documento pendiente de firma.

Esto aplica especialmente a:

- resoluciones sobre el acta
- resoluciones de medidas preventivas
- documentos de levantamiento de medidas
- documentos que requieren firma y notificación

## Criterio de diseño

Mantener:

- store simple
- supports funcionales por área
- controllers directos
- records / enums simples
- sin framework genérico
- sin DB
- sin sobrearquitectura

Pero priorizando siempre:

- coherencia con el modelo final
- un solo circuito verdadero por comportamiento
- consistencia entre condiciones iniciales, circuito operativo y cierre
- separación correcta entre documento, resultado final, firma, notificación y hecho material
- no convertir variantes documentales en nuevos ejes materiales falsos

## Regla de trabajo para slices

Cada slice debe definir explícitamente:

1. objetivo puntual
2. fuente de verdad
3. alcance técnico
4. exclusiones
5. caso demo
6. criterio de cierre

### Reglas obligatorias

- no reintroducir circuitos viejos
- no dejar convivir dos modelos contradictorios
- priorizar precisión quirúrgica
- no tocar más archivos de los necesarios
- no mezclar varios problemas conceptuales distintos en el mismo slice
- si una simplificación rompe el modelo final, corregirla ahora

## Fuente de verdad

- usar `spec/` como fuente principal de dominio
- usar solo la parte mínima necesaria
- usar `docs-trabajo/estado-actual-y-proximo-paso.md` como inventario vivo del prototipo
- usar este `prompt-de-reanudacion-chat.md` como marco metodológico y estructural
- usar `.cursor/rules/contexto-minimo.mdc` como norma de manejo de contexto
- usar `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc` para proteger continuidad

## Regla de contexto para Cursor

La fuente normativa de manejo de contexto es:

- `.cursor/rules/contexto-minimo.mdc`

Y la protección de continuidad está en:

- `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`

Resumen operativo mínimo:

- cargar primero lo mínimo indispensable
- no leer continuidad completa por defecto en slices funcionales
- no editar continuidad salvo autorización explícita
- cargar solo la parte mínima necesaria de `spec/`
- cargar solo los archivos del prototipo implicados en el slice

## Regla para refactors

Ante un problema, distinguir:

### A. Error de implementación puntual

Acción:

- corregir código
- no necesariamente tocar la spec

### B. Error de interpretación del modelo

Acción:

- corregir código
- y además fortalecer la fuente correcta

## Regla de aprendizaje persistente

Si una corrección importante revela ambigüedad o falta de precisión en la fuente, la lección no debe quedar solo en el código.

Debe subirse a la capa correcta:

- `spec/01-dominio/...` si era dominio
- `spec/02-reglas-transversales/...` si era regla transversal
- `spec/03-bandejas/...` si era flujo/bandeja/transición
- `spec/04-backend/...` si era implementación futura
- `prompt-de-reanudacion-chat.md` si era método o decisión estructural estable
- `estado-actual-y-proximo-paso.md` si era inventario vigente del prototipo
- `.cursor/rules/contexto-minimo.mdc` si era disciplina de contexto
- `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc` si era protección de continuidad

## Regla de continuidad mínima

Todo nuevo slice debe poder plantearse usando como base:

- `prompt-de-reanudacion-chat.md`
- `estado-actual-y-proximo-paso.md`
- `.cursor/rules/contexto-minimo.mdc`
- `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`
- un subconjunto pequeño y explícito de `spec/`

Si para arrancar hace falta abrir demasiada historia o demasiados archivos, el slice está mal recortado o el estado actual no está suficientemente consolidado.

## Puntos de entrada de código

Los puntos de entrada habituales del prototipo son:

- controller del prototipo
- `PrototipoStore`
- supports funcionales por área
- `MockDataFactory`
- tests de integración del prototipo

No asumir que todo debe tocarse.

Cada slice debe abrir solo la mínima superficie necesaria.

## Próximo paso sugerido

Después de esta consolidación, el siguiente paso natural debe elegirse sobre base real del prototipo actual y del inventario vigente.

Priorizar:

- cerrar backend demo con diff limpio
- asegurar archivos nuevos incorporados
- ejecutar `mvn test`
- revisar nombres muy confusos si afectan demo
- commitear estado consistente
- preparar guion funcional de demo

No priorizar ahora:

- DB real
- seguridad real
- integraciones reales
- firma digital real
- PDFs reales
- tesorería real
- notificación real

## Instrucción final

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir modelos contradictorios.  
No abrir contexto de más.  
No permitir que Cursor toque continuidad sin autorización explícita.  
Separar por área funcional cuando haga falta, no por entusiasmo de refactor.  
Si aparece una simplificación que rompe el modelo final, corregirla ahora.