# Cierre de restauración funcional — Dirección de Faltas

**Fecha:** 2026-06-01  
**Proyecto:** Bod-Faltas — Prototipo descartable en memoria  
**Módulo:** `backend/api-faltas-prototipo` + `apps/web-direccion-faltas/angular`

---

## Resumen ejecutivo

El proceso de restauración funcional del prototipo de Dirección de Faltas quedó cerrado el 1 de junio de 2026 luego de una serie de slices funcionales que reestablecieron la coherencia entre el backend en memoria, el dataset mock canónico y el frontend Angular.

Se corrigieron errores de datos mock, se completaron circuitos faltantes, se normalizó el dataset a 42 actas sin errores, se validó el punta a punta del circuito operativo completo y se corrigió el bug de PARALIZADAS (Slice 15A / 15B).

---

## Estado final

| Aspecto | Estado |
|---|---|
| Backend | Restaurado y estable |
| Dataset mock | 42 actas, 0 warnings, 0 errores |
| Matriz de sanidad | 42/42 sanas, 0 errores |
| Angular | Conectado, build OK |
| Circuito punta a punta | Validado |
| Bug PARALIZADAS | Corregido y validado |
| Warnings de budget Angular | Preexistentes, sin cambios |
| Reactivacion de PARALIZADAS (Slice 17A) | Implementada y validada |
| ACTA-0029 en dataset (Slice 19A/19B) | Implementada y revalidada |

---

## Alcance restaurado

### Circuitos funcionales validados

- **Redaccion documental**
  - NOTIFICACION_ACTA
  - NULIDAD
  - MEDIDA_PREVENTIVA
  - RESOLUCION
  - RECTIFICACION
- **Firma documental** — circuito de firma de piezas generadas
- **Notificacion positiva / negativa / vencida**
- **Reintento de notificacion** — desde notificacion negativa o vencida
- **Correo postal** — operacion real bajo "Operacion de notificaciones"; lotes CSV de salida y procesamiento de respuesta
- **Pago voluntario** — solicitar, informar, adjuntar comprobante, confirmar, observar
- **Fallo absolutorio** — dictado, firma, notificacion, cierre
- **Fallo condenatorio** — dictado, firma, notificacion
- **Apelacion** — presentacion, resolucion (aceptada / rechazada / absuelve)
- **Vencimiento de plazo de apelacion** — condena firme por vencimiento
- **Condena firme** — por vencimiento de apelacion o resolucion de apelacion
- **Pago de condena** — informar, adjuntar comprobante, confirmar; circuito separado del pago voluntario
- **Bloqueantes materiales** — deteccion de ejes (medida preventiva / rodado / documentacion), resolucion documental por eje, cumplimiento material por eje, bloqueo de cierre hasta completar
- **Archivo / reingreso** — archivo por analisis; reingreso desde archivo
- **Gestion externa / retorno** — derivacion (Juzgado de Paz, APREMIO); retorno desde gestion externa
- **Cierre** — por pago voluntario confirmado o por fallo absolutorio con cumplimiento material
- **Paralizadas** — estado protegido; sin acciones internas; reactivacion controlada con destino PENDIENTE_ANALISIS (Slice 17A)

---

## Dataset demo actual

- **Cantidad de actas:** 42
- **Distribucion de bandejas al reset:**

| Bandeja | Cantidad |
|---|---|
| ACTAS_EN_ENRIQUECIMIENTO | 4 |
| PENDIENTE_PREPARACION_DOCUMENTAL | 1 |
| PENDIENTE_FIRMA | 3 |
| PENDIENTE_NOTIFICACION | 10 |
| EN_NOTIFICACION | 3 |
| PENDIENTE_ANALISIS | 12 |
| PENDIENTES_RESOLUCION_REDACCION | 5 |
| GESTION_EXTERNA | 1 |
| PARALIZADAS | 1 |
| ARCHIVO | 1 |
| CERRADAS | 1 |

---

## Slices ejecutados (resumen)

| Slice | Descripcion |
|---|---|
| Slices previos | Restauracion de mocks, circuitos base, normalizacion del dataset |
| Slice 14 | Correo postal real (lotes CSV + procesamiento de respuesta) |
| Slice 15A | Fix bug PARALIZADAS: agregar PARALIZADAS a BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS |
| Slice 15B | Validacion post-fix: sin acciones indebidas en ACTA-0114, sin regresiones |
| Slice 16A | Documentacion de cierre (este documento) |
| Slice 17A | Reactivacion de PARALIZADAS: endpoint reactivar-acta, evento ACTA_REACTIVADA_DESDE_PARALIZADAS, destino PENDIENTE_ANALISIS, accionPendiente REVISION_POST_REACTIVACION, tests ReactivacionActa0114IT 9/9, matriz 41/0/0 (42/0/0 post Slice 19A), build Angular OK |
| Slice 17B | Documentacion: actualizacion de cierre, checklist y prompt de continuidad |
| Slice 18A | Validacion funcional completa demo: 14/15 circuitos OK; detectado H1 (ACTA-0029 ausente del dataset) |
| Slice 19A | Fix H1: agregar llamada a cargarActa0029FalloVencimientoPlazoDemo en MockDataFactory; dataset 42 actas; test CondenaFirmePagoCondenaActa0029IT 6/6 OK |
| Slice 19B | Revalidacion ACTA-0029: circuito API OK, build Angular OK; demo funcional completa SÍ |

---

## Bugs detectados y corregidos

### Bug PARALIZADAS (Slice 15A)

**Sintoma:** ACTA-0114, en bandeja PARALIZADAS, mostraba incorrectamente el boton "Solicitar pago voluntario".

**Causa raiz:** La constante BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS en demo-shell.component.ts no incluia PARALIZADAS. La funcion bandejaActaSinAccionesInternasOperativas() — primera guarda de todas las acciones internas operativas — no bloqueaba las actas en esa bandeja.

**Fix aplicado:**

Archivo: apps/web-direccion-faltas/angular/src/app/features/demo/demo-shell.component.ts

    const BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS: ReadonlyArray<BandejaCodigo> = [
      'CERRADAS',
      'ARCHIVO',
      'GESTION_EXTERNA',
      'PARALIZADAS',   // agregado en Slice 15A
    ];

**Impacto del fix:** Cero regresiones. Todos los circuitos operativos tienen esta funcion como primera guarda.

---

## Validaciones finales (Slice 15B)

| Validacion | Resultado |
|---|---|
| ACTA-0114 visible en PARALIZADAS | OK |
| ACTA-0114 informacion de paralizacion visible | OK |
| ACTA-0114 sin boton "Solicitar pago voluntario" | OK |
| ACTA-0114 sin acciones internas operativas indebidas | OK |
| ACTA-0114 pantalla no rompe ni queda vacia | OK |
| ACTA-0120 pago voluntario no afectado | OK |
| ACTA-0019 bloqueantes materiales visibles | OK |
| ACTA-0019 acciones materiales/documentales visibles | OK |
| ACTA-0011 "Generar resolucion" visible | OK |
| ACTA-0014 "Generar rectificacion" visible | OK |
| npm run build | OK (exit code 0) |

---

## Warnings conocidos (preexistentes, no regresiones)

| Warning | Detalle |
|---|---|
| bundle initial exceeded maximum budget | 688 KB vs limite 500 KB — preexistente, no empeoro |
| demo-shell.component.scss exceeded maximum budget | 12.66 KB vs limite 8 KB — preexistente, no empeoro |

Ninguno de estos warnings impide el build ni afecta el funcionamiento del prototipo.

---

## Pendientes no criticos

| Pendiente | Prioridad |
|---|---|
| Circuito material para actas dinamicas nuevas | Media — requiere criterio sobre reconocer-origen-bloqueo-cierre-material |
| ACTA-0010 y ACTA-0023 como casos de demo opcionales | Baja — fuera del dataset canonico; incorporar en slice futuro si hace falta |
| Warnings de budget Angular | Informativo — no bloquean; resolver implicaria code splitting o ajuste de presupuesto |

---

## Reglas fuertes de no tocar UX estable

La siguiente estructura quedo validada y no debe modificarse sin un slice explicito de UX:

- **Lateral izquierdo:** bandejas principales solamente; sin sub-bandejas en el lateral
- **Area principal:** filtro operativo y filtro de dependencia transversal
- **Panel derecho (sin acta seleccionada):** resumen simple de bandeja
- **Panel derecho (con acta seleccionada):** solo detalle del acta; sin accordion/resumen colapsable superior
- **Estilo de controles:** redondeos consistentes con inputs/selects; sin estilo pill/capsula
- **Texto:** sin mojibake (caracteres mal codificados)

---

## Conclusion

La restauracion funcional del prototipo de Direccion de Faltas quedo **cerrada** al 2026-06-01.

El prototipo es apto para demostracion y validacion operativa de todos los circuitos descritos en este documento. PARALIZADAS ya no es callejon sin salida: cuenta con reactivacion controlada (Slice 17A). Dataset: 42 actas, 0 warnings, 0 errores (Slice 19A agrego ACTA-0029). Demo funcional completa: SI (Slice 19B).
