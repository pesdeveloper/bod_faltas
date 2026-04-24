# PROMPT DE REANUDACIÓN — PROTOTIPO FALTAS

Estamos retomando el prototipo del sistema de faltas municipal.

## Proyecto

Backend demo:

- `backend/api-faltas-prototipo`
- Java 21 + Spring Boot 3.5.x
- sin DB real
- sin seguridad real
- store en memoria
- prototipo descartable técnicamente, pero funcionalmente fiel

Frontend demo:

- `apps/web-direccion-faltas`
- Angular + Angular Material
- UX demo documentada en:
  - `apps/web-direccion-faltas/docs/ux-demo/`

## Norte

El prototipo no debe ser happy path.

Debe representar de forma navegable, accionable y coherente el comportamiento real del sistema, con simplificaciones técnicas controladas.

La demo valida el espíritu operativo antes de construir el sistema real.

## Simplificaciones permitidas

- estado en memoria
- dataset mock
- acciones mock
- documentos mock
- firma mock
- notificación mock
- pago mock
- alta mock mínima
- UI demo sin seguridad real
- circuitos simplificados si mantienen fidelidad funcional

## Simplificaciones no permitidas

- atajos conceptuales que contradigan el dominio
- circuitos contradictorios
- cierre automático cuando el modelo exige condiciones
- confundir documento emitido con hecho material cumplido
- confundir resolución generada con resolución firmada
- confundir resolución firmada con resolución notificada
- confundir notificación con cumplimiento material
- convertir variantes documentales en ejes materiales falsos
- esconder reglas importantes en UI

## Estado de continuidad

Inventario vivo:

- `docs-trabajo/estado-actual-y-proximo-paso.md`

UX demo:

- `apps/web-direccion-faltas/docs/ux-demo/00-ux-demo-overview.md`

Este prompt es marco metodológico y estructural.

---

## Decisiones backend consolidadas

### Firma

- el circuito viejo `pasar-a-notificacion` fue eliminado
- la verdad es `firmarDocumento(actaId, documentoId)`
- documentos con identidad propia
- firma individual
- salida de `PENDIENTE_FIRMA` solo cuando todos los firmables están firmados

### Nulidad

- nulidad como pieza no-fallo
- no existe bandeja `NULAS`
- documento `NULIDAD` firmado lleva a `CERRADAS`
- no entra a notificación común
- salida terminal invalidante en prototipo

### Pago

- solicitud de pago voluntario
- pago informado
- comprobante mock
- pendiente confirmación
- pago confirmado
- pago observado

Regla:

- pago confirmado no cierra automáticamente

### Cerrabilidad

Cerrable = (`ABSUELTO` o `PAGO_CONFIRMADO`) y sin pendientes materiales/documentales activos.

Cierre siempre explícito.

### Bloqueantes materiales

Vigentes:

- `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- `LIBERACION_RODADO`
- `ENTREGA_DOCUMENTACION`

Regla:

- resolutorio no libera solo
- firma no libera
- notificación no libera
- solo cumplimiento material efectivo libera

### Hechos materiales

Separar:

- expediente documental
- hecho material
- cerrabilidad

Fases:

- sin origen/no aplica
- pendiente de resolutorio
- resolutorio sin cumplimiento
- cumplimiento verificado

### Tránsito

Retención documental y rodado retenido/secuestrado son datos propios del acta de tránsito mock.

- usar `ActaTransitoMock`
- no usar acciones disponibles genéricas
- proyectan bloqueantes y hechos materiales

### Contravención

Medida preventiva puede nacer:

- en labrado
- durante trámite
- por inspección posterior
- por noticia administrativa

`ACTA-0026` representa medida posterior.

### Bromatología

Decomiso de sustancias alimenticias es dato propio del acta/satélite mock.

- usar `ActaBromatologiaMock`
- no es medida preventiva genérica
- no genera `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- posible eje futuro a validar: `LIBERACION_DECOMISO` o `DISPOSICION_DECOMISO`

### Resoluciones

Desde `ENRIQUECIMIENTO` ya se puede dictar resolución.

En general, permitidas en instancias operativas salvo:

- `GESTION_EXTERNA`
- `ARCHIVO`
- `CERRADAS`

Probado en:

- `ACTAS_EN_ENRIQUECIMIENTO`
- `PENDIENTE_ANALISIS`
- `PENDIENTE_FIRMA`

### Resolución, firma, notificación y cumplimiento

No asumir equivalencias falsas:

- dictar resolución no equivale a firmar
- firmar no equivale a notificar
- notificar no equivale a cumplir materialmente

Si requiere firma:

- queda pendiente de firma

Si requiere firma y notificación:

- no queda notificable antes de firmarse

Si se vincula a bloqueo material:

- solo cumplimiento material libera

### Archivo / gestión externa

Archivo:

- bloquea resoluciones internas
- requiere reingreso

Gestión externa:

- bloquea resoluciones internas
- permite retorno y re-derivación

---

## Alta mock mínima

Endpoint:

- `POST /api/prototipo/actas/mock`

Dependencias:

- `TRANSITO`
- `INSPECCIONES`
- `FISCALIZACION`
- `BROMATOLOGIA`

Genera:

- `ACTA-DEMO-0001+`
- bandeja inicial `ACTAS_EN_ENRIQUECIMIENTO`
- visible en detalle/listado

No reemplaza labrado real final.

---

## UX demo

Carpeta:

- `apps/web-direccion-faltas/docs/ux-demo/`

Documentos:

- `00-ux-demo-overview.md`
- `01-layout-bandejas-y-listado.md`
- `02-panel-detalle-acta.md`
- `03-acciones-por-bandeja.md`
- `04-alta-mock-acta-demo.md`
- `05-hechos-materiales-y-cerrabilidad.md`
- `06-escenarios-demo-tribunal.md`
- `07-checklist-validacion-post-demo.md`

Modelo UX:

Bandeja → lista de actas → detalle → acciones → resultado/movimiento.

Reglas UX:

- tipo cliente de correo
- acciones importantes deshabilitadas con motivo
- backend fuente de verdad operativa
- frontend no duplica reglas complejas
- si falta backend, hacer micro-slice backend

---

## Regla UI ↔ Backend

Si durante UI aparece necesidad no cubierta por backend:

1. no simular permanentemente en Angular
2. revisar `PrototipoApiController`
3. si falta, frenar slice UI
4. crear micro-slice backend
5. agregar test
6. ejecutar `mvn test`
7. retomar UI consumiendo endpoint real

Angular puede resolver:

- layout
- filtros simples
- labels
- badges
- toasts
- estados visuales

Backend debe resolver:

- reglas de negocio
- transiciones
- cerrabilidad
- bloqueantes
- rechazos
- eventos
- movimientos de bandeja

---

## Regla de trabajo para slices

Cada slice debe definir:

1. objetivo
2. fuente de verdad
3. alcance
4. exclusiones
5. caso demo
6. criterio de cierre

Reglas:

- slices mínimos
- no mezclar problemas conceptuales
- no tocar más archivos de los necesarios
- no reintroducir circuitos viejos
- no dejar modelos contradictorios
- si una simplificación rompe modelo final, corregirla

---

## Fuente de verdad

- dominio: `spec/`
- inventario backend/estado: `docs-trabajo/estado-actual-y-proximo-paso.md`
- UX demo: `apps/web-direccion-faltas/docs/ux-demo/`
- reglas Cursor:
  - `.cursor/rules/contexto-minimo.mdc`
  - `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`

No tocar continuidad salvo autorización explícita.

---

## Regla de contexto para Cursor

Cargar solo contexto mínimo.

Ejemplos UX:

- layout: `00` + `01`
- detalle: `00` + `02` + `05`
- acciones: `00` + `03` + `05`
- alta mock: `00` + `04`
- guion: `00` + `06` + `07`

No cargar toda la carpeta salvo slice transversal.

---

## Próximo paso sugerido

Mañana iniciar UX demo en chat nuevo.

Primer slice recomendado:

- shell Angular demo
- sidebar bandejas
- listado por bandeja
- selección de acta
- panel detalle placeholder
- consumo real de backend mock
- sin intentar implementar todas las acciones todavía

No priorizar ahora:

- DB real
- seguridad real
- firma real
- PDFs reales
- notificación real
- tesorería real

---

## Instrucción final

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir modelos contradictorios.  
No abrir contexto de más.  
No permitir que Cursor toque continuidad sin autorización.  
Separar documento, acto, firma, notificación, cumplimiento material y cierre.  
La demo valida el espíritu operativo; la spec consolidada definirá el sistema real.