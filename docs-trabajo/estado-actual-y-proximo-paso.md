# ESTADO ACTUAL Y PRÓXIMO PASO — PROTOTIPO BACKEND / UX DEMO FALTAS

## Propósito del archivo

Este archivo registra el estado operativo vigente del prototipo y el próximo paso real.

No reemplaza la `spec/`.  
No reemplaza el `prompt-de-reanudacion-chat.md`.

Su función es dejar visible:

- qué capacidades reales tiene hoy el prototipo backend
- qué decisiones quedaron consolidadas
- qué casos demo existen
- qué documentos UX quedaron preparados
- qué falta realmente
- cuál es el próximo paso natural

---

## 1. Estado actual resumido

El backend prototipo demo quedó funcionalmente cerrado para la demo operativa.

Ya cubre:

- piezas múltiples por expediente
- firma individual por documento
- salida desde `PENDIENTE_FIRMA` solo cuando las piezas firmables están firmadas
- nulidad post-firma como salida terminal invalidante a `CERRADAS`
- notificación mock con variantes mínimas
- archivo y reingreso
- gestión externa con derivación, retorno y re-derivación
- pago voluntario, pago informado, comprobante mock, confirmación y observación
- resultado final compatible con cierre:
  - `ABSUELTO`
  - `PAGO_CONFIRMADO`
- cerrabilidad unificada
- bloqueantes materiales/documentales
- hechos materiales separados de cerrabilidad
- tránsito con condiciones materiales desde datos mock del acta
- contravención con medida preventiva posterior
- resoluciones desde instancias operativas
- firma/notificación documental separada de cumplimiento material
- alta mock mínima de actas demo en vivo
- script `demo-prototipo-faltas.ps1` actualizado y ejecutado correctamente
- documentación UX demo dividida por área funcional

---

## 2. Decisiones funcionales consolidadas

### 2.1 Firma

- el circuito viejo `pasar-a-notificacion` ya no es la verdad
- la verdad vigente es `firmarDocumento(actaId, documentoId)`
- los documentos tienen identidad propia
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los documentos firmables están `FIRMADO`
- un documento resolutivo puede estar pendiente de firma aunque el acta siga en una bandeja operativa

### 2.2 Firma y notificación documental

Regla consolidada:

- generar/dictar resolución no equivale a firmarla
- firmar resolución no equivale a notificarla
- notificar resolución no equivale a cumplimiento material efectivo

Si un documento resolutivo requiere firma:

- nace `PENDIENTE_FIRMA`
- no queda firmado automáticamente
- debe pasar por circuito/mock de firma vigente

Si además requiere notificación:

- no queda notificable antes de firmarse
- luego de firmado puede quedar listo/pendiente para notificación según modelo vigente

Ni generar, ni firmar, ni notificar liberan por sí solos un bloqueo material.

El bloqueo material se libera solo por cumplimiento material efectivo.

### 2.3 Nulidad

- nulidad se modela como pieza no-fallo
- no existe bandeja terminal `NULAS`
- si el último documento firmado es `NULIDAD`, el expediente pasa a `CERRADAS`
- nulidad post-firma no entra al circuito común de notificación
- en este prototipo es una salida terminal invalidante

### 2.4 Pago

El prototipo distingue:

- solicitud de pago voluntario
- pago informado
- comprobante mock
- pago pendiente de confirmación
- pago confirmado
- pago observado

Reglas:

- informar pago no equivale a pago confirmado
- adjuntar comprobante no equivale a pago confirmado
- confirmar pago produce `resultadoFinal = PAGO_CONFIRMADO`
- `PAGO_CONFIRMADO` no cierra automáticamente

### 2.5 Cerrabilidad

Regla consolidada:

Cerrable = (`ABSUELTO` o `PAGO_CONFIRMADO`) y sin pendientes materiales/documentales activos.

Entonces:

- `ABSUELTO` no cierra solo
- `PAGO_CONFIRMADO` no cierra solo
- cierre sigue siendo acción explícita

### 2.6 Bloqueantes materiales/documentales

Bloqueantes vigentes:

- `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- `LIBERACION_RODADO`
- `ENTREGA_DOCUMENTACION`

Reglas:

- resolutorio documental no basta solo
- firma del resolutorio no basta sola
- notificación del resolutorio no basta sola
- debe existir cumplimiento material efectivo
- el expediente no queda cerrable mientras haya bloqueantes activos

### 2.7 Hechos materiales

La vista `hechosMateriales` separa:

- plano documental
- plano material
- cerrabilidad

Expone:

- ejes materiales
- fase del eje
- `ejeBloqueanteCierre`
- `lecturaOperativa`

La lectura distingue:

- condiciones materiales tempranas/iniciales
- medida preventiva posterior durante trámite
- resolutorios existentes sin cumplimiento material efectivo

### 2.8 Tránsito

Para tránsito:

- retención documental
- retención/secuestro de rodado
- eje urbano

son datos propios del acta de tránsito mock, no botones operativos.

Se modelan mediante `ActaTransitoMock`.

Proyectan:

- `hechosMateriales`
- `pendientesBloqueantesCierre`
- `lecturaOperativa`
- cerrabilidad

`ACTA-0024` ya no depende de ejecutar acciones de constatación temprana para nacer con estas condiciones.

No existe `hechosMateriales.accionesDisponibles`.

### 2.9 Endpoint de constatación material temprana

Sigue existiendo:

- `registrar-constatacion-material-temprana`

Rol vigente:

- herramienta demo/técnica/regresión
- no verdad principal de `ACTA-0024`

Reglas:

- restringido a D1/D2
- controla duplicados
- rechaza fuera de etapa válida

`ACTA-0025` queda como caso de regresión.

### 2.10 Contravención con medida preventiva posterior

En contravenciones, una medida preventiva puede nacer:

- en el labrado
- durante trámite administrativo
- por inspección posterior
- por noticia administrativa o nuevo hecho vinculado

Ejemplo:

- rotura de faja de clausura

Puede terminar generando otra acta según criterio, pero el modelo no debe impedir que nazca dentro del proceso administrativo de una contravención existente.

`ACTA-0026` representa este caso.

Reglas:

- no se mezcla con `ActaTransitoMock`
- usa `registrar-medida-preventiva-posterior`
- genera `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- se ve en `hechosMateriales`
- afecta cerrabilidad
- requiere resolutorio documental y cumplimiento material efectivo

### 2.11 Bromatología / decomiso

Para bromatología:

- `decomisoSustanciasAlimenticias` es dato propio del acta/satélite mock
- usa `ActaBromatologiaMock`
- no es medida preventiva genérica
- no genera `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- no genera `LIBERACION_RODADO`
- no genera `ENTREGA_DOCUMENTACION`

Posible eje futuro a validar:

- `LIBERACION_DECOMISO`
- `DISPOSICION_DECOMISO`

No implementado todavía.

### 2.12 Alta mock mínima de acta demo

El backend permite crear/labrar actas mock mínimas en vivo.

Endpoint:

- `POST /api/prototipo/actas/mock`

Reglas:

- genera `ACTA-DEMO-0001+`
- id y número coinciden en prototipo
- contador se reinicia con `POST /reset`
- nace en `ACTAS_EN_ENRIQUECIMIENTO`
- aparece en detalle
- aparece en listado de bandeja
- genera evento mínimo de alta/labrado mock

Dependencias demo:

- `TRANSITO`
- `INSPECCIONES`
- `FISCALIZACION`
- `BROMATOLOGIA`

Datos por dependencia:

- Tránsito:
  - `ejeUrbano`
  - `rodadoRetenidoOSecuestrado`
  - `documentacionRetenida`
- Inspecciones:
  - `medidaPreventivaClausura`
- Fiscalización:
  - `medidaPreventivaParalizacionObra`
- Bromatología:
  - `decomisoSustanciasAlimenticias`

### 2.13 Resoluciones sobre el acta

Desde `ENRIQUECIMIENTO` ya se puede dictar resolución.

En general, las resoluciones se admiten en instancias operativas salvo:

- `GESTION_EXTERNA`
- `ARCHIVO`
- `CERRADAS`

Probado en:

- `ACTAS_EN_ENRIQUECIMIENTO`
- `PENDIENTE_ANALISIS`
- `PENDIENTE_FIRMA`

`DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF` existe como tipo documental, no como eje/bloqueante material.

El eje sigue siendo:

- `LEVANTAMIENTO_MEDIDA_PREVENTIVA`

### 2.14 Archivo / reingreso

- existe `ARCHIVO`
- existe motivo de archivo
- existe reingreso explícito
- reingreso vuelve a `PENDIENTE_ANALISIS`
- queda marca `REVISION_POST_REINGRESO`
- en archivo no se admiten resoluciones internas

### 2.15 Gestión externa

- existe `GESTION_EXTERNA`
- tipos mínimos:
  - `APREMIO`
  - `JUZGADO_DE_PAZ`
- existe derivación
- existe retorno a análisis
- existe re-derivación
- conserva trazabilidad sintética
- en gestión externa no se admiten resoluciones internas

---

## 3. Casos demo relevantes

### `ACTA-0012` — nulidad post-firma

Valida:

- generación de nulidad
- firma de documento `NULIDAD`
- salida terminal a `CERRADAS`
- no notificación común

### `ACTA-0015` — gestión externa

Valida:

- derivación
- retorno
- re-derivación

### `ACTA-0018` — pago confirmado sin bloqueantes

Valida:

- flujo económico simple
- pago confirmado sin complejidad material

### `ACTA-0019` — absolución + bloqueantes

Valida:

- `resultadoFinal = ABSUELTO`
- bloqueantes
- no cerrabilidad hasta cumplimiento
- cierre posterior

### `ACTA-0020` — resolución en `PENDIENTE_FIRMA`

Valida:

- resolución desde bandeja operativa adicional
- bandeja no se mueve artificialmente
- bloqueo sigue hasta cumplimiento material

### `ACTA-0021` — pago confirmado precargado + bloqueantes

Valida:

- `PAGO_CONFIRMADO`
- bloqueantes materiales
- cerrabilidad material

### `ACTA-0022` — flujo real de pago + bloqueantes

Valida:

- solicitud pago
- pago informado
- comprobante
- confirmación
- bloqueantes
- resolutorio + cumplimiento
- cierre

### `ACTA-0023` — hechos materiales

Valida:

- separación expediente documental vs hecho material
- resolutorio sin cumplimiento

### `ACTA-0024` — tránsito integral

Caso más completo.

Valida:

- datos desde `ActaTransitoMock`
- rodado/documentación retenida
- medida preventiva aplicable
- pago
- bloqueantes
- resolutorios
- firma/notificación documental
- cumplimiento material
- cerrabilidad
- cierre explícito

### `ACTA-0025` — constatación temprana D1/D2

Valida:

- endpoint técnico/regresión
- duplicados
- rechazo fuera de etapa

### `ACTA-0026` — medida preventiva posterior

Valida:

- medida nacida durante trámite
- lectura operativa de medida posterior
- bloqueante
- resolutorio
- cumplimiento
- cierre

### `ACTA-DEMO-0001+` — actas creadas en vivo

Generadas por:

- `POST /api/prototipo/actas/mock`

Valida:

- alta demo
- numeración
- dependencia
- aparición en bandeja
- escenario creado por usuarios

---

## 4. UX demo preparada

Se creó carpeta:

- `apps/web-direccion-faltas/docs/ux-demo/`

Archivos:

- `00-ux-demo-overview.md`
- `01-layout-bandejas-y-listado.md`
- `02-panel-detalle-acta.md`
- `03-acciones-por-bandeja.md`
- `04-alta-mock-acta-demo.md`
- `05-hechos-materiales-y-cerrabilidad.md`
- `06-escenarios-demo-tribunal.md`
- `07-checklist-validacion-post-demo.md`

Criterio:

- documentos compactos
- navegables
- orientados a Cursor
- sin depender de `docs-trabajo/` como contexto directo
- conectados al backend demo
- pensados para slices de Angular

El `00-overview` contiene:

- índice navegable
- conexión con backend
- casos relevantes
- reglas Angular mínimas
- qué hacer si falta backend durante UI

---

## 5. Script demo

Se actualizó y validó:

- `demo-prototipo-faltas.ps1`

El script ejecuta correctamente contra backend actualizado.

Cubre:

- health
- reset
- alta mock
- casos precargados principales
- `ACTA-0024`
- `ACTA-0026`
- resoluciones desde bandejas operativas
- firma/cumplimiento
- gestión externa
- archivo/notificación según script vigente

Resultado reportado:

- demo del prototipo finalizada correctamente

---

## 6. Próximo paso natural

Mañana arrancar con UX demo en `apps/web-direccion-faltas`.

Orden recomendado:

1. abrir chat nuevo
2. pegar `prompt-de-reanudacion-chat.md`
3. pegar o resumir este estado
4. indicar que se empieza desde:
   - `apps/web-direccion-faltas/docs/ux-demo/00-ux-demo-overview.md`
5. primer slice UI:
   - layout shell de bandejas/listado/detalle
   - sin intentar implementar toda la UX de golpe

Primer slice sugerido:

- crear shell Angular demo
- sidebar de bandejas
- listado por bandeja
- selección de acta
- panel detalle placeholder
- consumo real de backend mock
- sin acciones todavía o con acciones mínimas de lectura

---

## 7. Qué falta realmente

### 7.1 Cierre técnico

Si no está hecho:

- revisar `git status`
- agregar archivos nuevos
- ejecutar tests
- commitear cambios backend + docs UX + script demo

### 7.2 UX demo

Pendiente:

- implementar UI Angular por slices
- conectar con backend mock
- mostrar bandejas/listados/detalle
- agregar alta mock
- agregar acciones contextuales
- agregar hechos materiales/cerrabilidad
- preparar guion demo

### 7.3 Post-demo

Después de validar con Tribunal:

- reconciliar feedback con spec
- ajustar dominio
- ajustar bandejas
- ajustar documentos
- ajustar firma/notificación/pago
- ajustar medidas/decomiso
- ajustar modelo de datos y DDL
- recién después pasar a sistema real

---

## 8. Riesgos / bordes conocidos

- backend sigue siendo in-memory y demo-oriented
- endpoints demo no son contratos finales productivos
- alta mock no reemplaza labrado real
- notificación real no está implementada
- firma real no está implementada
- decomiso todavía no tiene eje propio
- algunas bandejas operativas no tienen test específico individual
- UI debe evitar duplicar reglas complejas del backend
- si falta endpoint para UI, debe hacerse micro-slice backend y no simularlo permanentemente en Angular

---

## 9. Regla operativa para continuar

Para próximos slices:

- usar `spec/` como fuente de dominio
- usar este archivo como inventario vivo
- usar `prompt-de-reanudacion-chat.md` como marco
- usar `apps/web-direccion-faltas/docs/ux-demo/00-ux-demo-overview.md` como entrada UX
- mantener slices mínimos
- no mezclar problemas conceptuales
- no tocar continuidad salvo autorización
- distinguir siempre:
  - dato del acta
  - acción demo/técnica
  - documento resolutivo
  - firma
  - notificación
  - cumplimiento material
  - cierre