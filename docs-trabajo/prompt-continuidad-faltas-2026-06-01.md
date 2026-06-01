# Prompt de continuidad — Direccion de Faltas

**Fecha:** 2026-06-01  
**Para pegar en:** chat nuevo o agente nuevo  
**Repo:** S:\Source\Repos\Bod-Faltas  
**Backend:** S:\Source\Repos\Bod-Faltas\backend\api-faltas-prototipo  
**Angular:** S:\Source\Repos\Bod-Faltas\apps\web-direccion-faltas\angular  

---

## Modelo recomendado para proximos slices

- Cambios funcionales importantes: Sonnet 4.6, Thinking ON, Effort High, Context 200K
- Validaciones y documentacion: Sonnet 4.6, Thinking ON, Effort Medium, Context 200K
- No usar Composer/Fast para cambios funcionales grandes

---

## Reglas de trabajo con Cursor

- Trabajar con slices minimos y objetivo definido.
- Primero diagnosticar. Clasificar hallazgos: critico / importante / bajo / requiere criterio / no accion / cubierto.
- Proponer slice minimo antes de implementar.
- Ejecutar build/tests cuando corresponda.
- No tocar UX estable sin pedido explicito.
- Para cambios criticos pedir Keep All.
- No reescribir artefactos de continuidad (docs-trabajo/) sin autorizacion explicita.
- Contexto minimo: cargar solo los archivos del modulo afectado; no abrir todo el repo.

---

## Estado final al 2026-06-01

| Aspecto | Estado |
|---|---|
| Restauracion funcional | CERRADA |
| Backend | Estable, en memoria, sin DB real |
| Dataset mock | 42 actas canonicas |
| Matriz de sanidad | 42/42 sanas, 0 warnings, 0 errores |
| Angular | Conectado con backend para circuitos principales |
| Circuito punta a punta | Validado |
| Bug PARALIZADAS (Slice 15A) | Corregido y validado (Slice 15B) |
| Build Angular | OK, solo warnings preexistentes de budget |
| Documentacion de cierre (Slice 16A) | Creada |
| Indices y prompt de continuidad (Slice 16B) | Creados |
| Slice 17A — Reactivacion de PARALIZADAS | Implementado y validado |
| Slice 17B — Documentacion actualizada | Actualizada |
| Slice 18A - Validacion funcional completa | 14/15 OK; H1 detectado (ACTA-0029 ausente) |
| Slice 19A - ACTA-0029 agregada al dataset | Implementado; dataset 42 actas; test 6/6 OK |
| Slice 19B - Revalidacion ACTA-0029 | Revalidado; demo funcional completa SI |
| Slice 20A - Validacion visual UX + guia presentacion | Angular levantado OK; validacion visual en browser PENDIENTE revision manual; guia ejecutiva creada |

---

## Reglas UX fuertes — NO ROMPER sin slice explicito

- Lateral izquierdo: bandejas principales solamente; sin sub-bandejas en el lateral
- Iconos visibles en bandejas del lateral
- Cantidades como badges en las bandejas del lateral
- Labels de bandeja legibles hasta dos lineas; densidad vertical compacta
- Filtro operativo en area principal con opcion "Todos" y X compacta para limpiar
- Filtro de dependencia transversal separado del operativo
- Filtro dependencia y filtro operativo trabajan en AND
- Redondeos consistentes con inputs/selects; sin estilo pill/capsula
- Correo postal como opcion operativa real bajo divider "Operacion de notificaciones"
- Notificador municipal demo: secundario/temporal, no como opcion principal
- Panel derecho sin acta seleccionada: resumen simple de bandeja; sin accordion/resumen colapsable arriba
- Panel derecho con acta seleccionada: solo detalle del acta; sin resumen colapsable encima
- Responsive menor a 960px: detalle como modal/overlay con X; resumen oculto por defecto y accesible por icono/info
- Sin mojibake (caracteres mal codificados) en ningun texto visible

---

## Circuitos funcionales validados

- NOTIFICACION_ACTA — redaccion, firma, notificacion
- NULIDAD — redaccion, firma
- MEDIDA_PREVENTIVA — redaccion, firma
- RESOLUCION — redaccion, firma
- RECTIFICACION — redaccion, firma
- Firma documental — circuito de firma de piezas generadas
- Notificacion positiva / negativa / vencida
- Reintento de notificacion desde negativa o vencida
- Correo postal — lotes CSV de salida y procesamiento de respuesta
- Pago voluntario — solicitar, informar, adjuntar, confirmar, observar
- Fallo absolutorio — dictado, firma, notificacion, cierre
- Fallo condenatorio — dictado, firma, notificacion
- Apelacion — presentacion, resolucion (aceptada / rechazada / absuelve)
- Vencimiento de plazo de apelacion — condena firme por vencimiento
- Condena firme — por vencimiento o por resolucion de apelacion
- Pago de condena — informar, adjuntar, confirmar; circuito separado del pago voluntario
- Bloqueantes materiales — ejes medida preventiva / rodado / documentacion; resolucion documental + cumplimiento material por eje; bloqueo cierre hasta completar
- Archivo / reingreso
- Gestion externa / retorno (Juzgado de Paz, APREMIO)
- Cierre — por pago voluntario confirmado o fallo absolutorio con cumplimiento material
- Paralizadas — estado protegido; sin acciones internas; reactivacion controlada (Slice 17A); destino PENDIENTE_ANALISIS / PENDIENTE_REVISION; accionPendiente REVISION_POST_REACTIVACION

---

## Actas clave para demo

| Acta | Circuito |
|---|---|
| ACTA-0001 | NOTIFICACION_ACTA: generar, firmar, notificar |
| ACTA-0011 | RESOLUCION: generar, firmar |
| ACTA-0014 | RECTIFICACION: generar, firmar |
| ACTA-0012 | NULIDAD: generar, firmar, cerrar si corresponde |
| ACTA-0013 | MEDIDA_PREVENTIVA + NOTIFICACION_ACTA: piezas multiples, firmar, notificar |
| ACTA-0006 | Fallo absolutorio: dictar, firmar, notificar, cerrar |
| ACTA-0027 / ACTA-0028 | Fallo condenatorio + apelacion: presentar, resolver |
| ACTA-0029 | Condena firme + pago condena + cierre |
| ACTA-0030 | Gestion externa APREMIO + reingreso + pago condena |
| ACTA-0019 | Bloqueantes materiales (3 ejes): resolver documental + material + cerrar |
| ACTA-0122 | Condena firme + informar pago + confirmar + cerrar |
| ACTA-0007 | Archivo + reingreso |
| ACTA-0017 | Derivar a gestion externa + retorno |
| ACTA-0114 | PARALIZADAS protegida: sin acciones internas; "Reactivar acta" disponible; destino PENDIENTE_ANALISIS / PENDIENTE_REVISION; accionPendiente REVISION_POST_REACTIVACION |

---

## Bug PARALIZADAS — documentado y cerrado

**Sintoma:** ACTA-0114 mostraba incorrectamente el boton "Solicitar pago voluntario" estando en bandeja PARALIZADAS.

**Causa:** La constante BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS en demo-shell.component.ts no incluia 'PARALIZADAS'. La funcion bandejaActaSinAccionesInternasOperativas() no bloqueaba las actas en esa bandeja.

**Fix (Slice 15A):**

Archivo: apps/web-direccion-faltas/angular/src/app/features/demo/demo-shell.component.ts

    const BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS: ReadonlyArray<BandejaCodigo> = [
      'CERRADAS',
      'ARCHIVO',
      'GESTION_EXTERNA',
      'PARALIZADAS',   // agregado en Slice 15A
    ];

**Validacion (Slice 15B):** OK, cero regresiones.

**Mejora funcional (Slice 17A):** Reactivacion de PARALIZADAS implementada.
- Endpoint: POST /api/prototipo/actas/{id}/acciones/reactivar-acta
- Evento registrado: ACTA_REACTIVADA_DESDE_PARALIZADAS
- Destino: PENDIENTE_ANALISIS / estadoProceso PENDIENTE_REVISION / accionPendiente REVISION_POST_REACTIVACION
- Tests: ReactivacionActa0114IT 9/9 OK
- Matriz: 41/0/0

**Regla activa:** PARALIZADAS es estado protegido. Sin acciones internas operativas. Con reactivacion controlada disponible.

---

## Pendientes no criticos — no implementar sin definicion

| Pendiente | Observacion |
|---|---|
| Circuito material para actas dinamicas nuevas | Requiere criterio sobre reconocer-origen-bloqueo-cierre-material para actas creadas en runtime |
| ACTA-0010 y ACTA-0023 | Casos opcionales futuros; fuera del dataset canonico actual |
| Warnings de budget Angular | Preexistentes; bundle 688 KB (limite 500 KB), scss 12.66 KB (limite 8 KB); no bloquean |

---

## Comandos utiles

### Backend

    cd S:\Source\Repos\Bod-Faltas\backend\api-faltas-prototipo
    mvn spring-boot:run

### Backend compilar (verificacion rapida)

    cd S:\Source\Repos\Bod-Faltas\backend\api-faltas-prototipo
    mvn -q compile

### Matriz de sanidad

    cd S:\Source\Repos\Bod-Faltas\backend\api-faltas-prototipo
    mvn test -Dtest="MatrizSanidadActasDemoIT"

Resultado esperado: BUILD SUCCESS, 42 actas sanas, 0 warnings, 0 errores.

### Angular dev server

    cd S:\Source\Repos\Bod-Faltas\apps\web-direccion-faltas\angular
    npm start

### Angular build

    cd S:\Source\Repos\Bod-Faltas\apps\web-direccion-faltas\angular
    npm run build

Resultado esperado: exit code 0. Solo warnings preexistentes de budget.

### Reset demo

    # Via backend directo
    Invoke-RestMethod -Uri "http://localhost:8087/api/prototipo/reset" -Method Post

    # Via proxy Angular
    Invoke-RestMethod -Uri "http://localhost:4200/api/prototipo/reset" -Method Post

Respuesta esperada: { "resultado": "OK", "cantidadActas": 42 }

### Health

    Invoke-RestMethod -Uri "http://localhost:8087/api/prototipo/health" -Method Get

Respuesta esperada: { "status": "UP", "cantidadActas": 42 }

### Bandejas via proxy Angular

    Invoke-RestMethod -Uri "http://localhost:4200/api/prototipo/bandejas" -Method Get

---

## Documentacion de referencia

Todos los archivos estan en docs-trabajo/:

- docs-trabajo/README.md — indice de documentacion de trabajo
- docs-trabajo/cierre-restauracion-funcional-faltas-2026-06-01.md — cierre formal del proceso de restauracion
- docs-trabajo/checklist-guiado-validacion-integral-faltas-2026-06-01.md - checklist guiado de validacion integral (22 casos, 42 actas)
- docs-trabajo/prompt-continuidad-faltas-2026-06-01.md — este archivo

---

## Forma de trabajo futura

1. Siempre empezar con reset del demo antes de validar o presentar.
2. Para un nuevo slice:
   a. Describir el objetivo en una linea.
   b. Diagnosticar: leer solo los archivos del modulo afectado.
   c. Clasificar hallazgos: critico / importante / bajo / requiere criterio / no accion / cubierto.
   d. Proponer slice minimo antes de tocar codigo.
   e. Ejecutar build y/o matriz de sanidad si el slice toco backend o Angular.
   f. Documentar resultado.
3. No mezclar en un mismo slice: refactor + ampliacion funcional + limpieza.
4. No tocar UX estable sin pedido explicito y sin justificacion funcional.
5. Para cambios importantes: Sonnet 4.6, Thinking ON, Effort High, Context 200K.
6. Para validaciones y documentacion: Sonnet 4.6, Thinking ON, Effort Medium.
7. Pedir Keep All solo en slices criticos con muchos archivos tocados.
8. No usar "por las dudas" — abrir solo lo estrictamente necesario.
