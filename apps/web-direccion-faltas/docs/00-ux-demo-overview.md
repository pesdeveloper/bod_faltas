# UX Demo — Overview

## Propósito

Contexto mínimo para construir la UI demo de `apps/web-direccion-faltas`.

La UX valida con el Tribunal:

- bandejas operativas
- listado de actas
- detalle
- acciones por acta
- alta mock en vivo
- hechos materiales
- cerrabilidad

Backend demo asociado:

- `backend/api-faltas-prototipo`
- base API: `/api/prototipo`

No es spec final. Lo validado en demo debe reconciliarse luego con `spec/`.

---

## Índice

| Archivo | Uso |
|---|---|
| [01 — Layout, bandejas y listado](./01-layout-bandejas-y-listado.md) | Pantalla principal, sidebar, filtros, filas |
| [02 — Panel detalle de acta](./02-panel-detalle-acta.md) | Detalle, resumen, documentos, pago, historial |
| [03 — Acciones por bandeja](./03-acciones-por-bandeja.md) | Botones, habilitación, motivos de bloqueo |
| [04 — Alta mock acta demo](./04-alta-mock-acta-demo.md) | Formulario `POST /actas/mock` |
| [05 — Hechos materiales y cerrabilidad](./05-hechos-materiales-y-cerrabilidad.md) | Bloqueantes, cumplimiento, cierre |
| [06 — Escenarios demo Tribunal](./06-escenarios-demo-tribunal.md) | Guion funcional de demo |
| [07 — Checklist validación post-demo](./07-checklist-validacion-post-demo.md) | Feedback y consolidación post-demo |

---

## Contexto relacionado

| Recurso | Uso |
|---|---|
| `backend/api-faltas-prototipo` | Backend mock que alimenta esta UX |
| `docs-trabajo/estado-actual-y-proximo-paso.md` | Inventario vivo del backend prototipo |
| `docs-trabajo/prompt-de-reanudacion-chat.md` | Marco metodológico del prototipo |
| `.cursor/rules/contexto-minimo.mdc` | Regla de carga mínima |
| `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc` | Protección de continuidad |

Si `docs-trabajo/` está protegido para Cursor, no usarlo como insumo directo salvo autorización explícita.

---

## Principio UX

Modelo tipo cliente de correo:

Bandeja → lista de actas → detalle → acciones → resultado/movimiento

El operador debe ver:

- dónde está el acta
- por qué está ahí
- qué puede hacer
- qué no puede hacer y por qué
- qué cambió después de una acción

---

## Reglas fuertes

- No mostrar workflow gigante.
- Mostrar bandejas y pendientes.
- Las acciones importantes se muestran deshabilitadas con motivo.
- No llamar a todo “estado”.
- Separar bandeja, resultado, firma, notificación, cerrabilidad, hechos materiales y cierre.
- Backend = fuente de verdad operativa.
- Frontend = presentación, navegación, filtros, labels y experiencia.
- No duplicar reglas complejas de dominio en Angular.

---

## Conexión backend mínima

Endpoints principales esperados:

| Necesidad UI | Endpoint |
|---|---|
| Health | `GET /api/prototipo/health` |
| Reset demo | `POST /api/prototipo/reset` |
| Crear acta mock | `POST /api/prototipo/actas/mock` |
| Listar bandeja | `GET /api/prototipo/bandejas/{bandeja}/actas` |
| Ver detalle | `GET /api/prototipo/actas/{id}` |
| Ver documentos | `GET /api/prototipo/actas/{id}/documentos` |
| Ver notificaciones | `GET /api/prototipo/actas/{id}/notificaciones` |
| Firmar documento | `POST /api/prototipo/actas/{id}/acciones/firmar-documento/{documentoId}` |
| Registrar resolución | `POST /api/prototipo/actas/{id}/acciones/registrar-resolucion-bloqueo-cierre` |
| Registrar cumplimiento | `POST /api/prototipo/actas/{id}/acciones/registrar-cumplimiento-material-bloqueo-cierre` |
| Medida preventiva posterior | `POST /api/prototipo/actas/{id}/acciones/registrar-medida-preventiva-posterior` |

Para rutas no listadas, Cursor debe revisar `PrototipoApiController` y no inventar endpoints.

---

## Casos backend útiles

| Caso | Uso |
|---|---|
| `ACTA-0024` | Tránsito integral: pago, bloqueantes, firma, cumplimiento, cierre |
| `ACTA-0025` | Constatación material temprana D1/D2 |
| `ACTA-0026` | Medida preventiva posterior |
| `ACTA-0020` | Resolución desde `PENDIENTE_FIRMA` |
| `ACTA-0015` | Gestión externa |
| `ACTA-0004` / `ACTA-0005` | Notificación negativa/vencida |
| `ACTA-DEMO-0001+` | Actas creadas en vivo |

---

## Si falta backend durante UI

No tapar faltantes con lógica ficticia permanente en Angular.

Procedimiento:

1. identificar necesidad exacta
2. revisar `PrototipoApiController`
3. si no existe, frenar slice UI
4. crear micro-slice backend
5. agregar test backend
6. ejecutar `mvn test`
7. retomar UI consumiendo endpoint real

Angular puede resolver:

- layout
- filtros simples
- labels
- badges
- toasts
- visualización de botones
- estados de carga/error

Backend debe resolver:

- reglas de negocio
- transiciones
- rechazos
- cerrabilidad
- generación de eventos
- movimientos de bandeja
- validaciones de acciones

---

## Reglas Angular mínimas

Usar Angular + Angular Material.

Preferir:

- componentes pequeños
- servicios HTTP por recurso
- modelos TypeScript simples
- estado local para demo
- sin store global salvo necesidad real
- sin duplicar reglas complejas del backend

Componentes sugeridos:

- `FaltasDemoShellComponent`
- `BandejasSidebarComponent`
- `ActasListadoComponent`
- `ActaDetallePanelComponent`
- `ActaAccionesComponent`
- `AltaActaMockDialogComponent`
- `HechosMaterialesComponent`
- `CerrabilidadPanelComponent`

Servicios sugeridos:

- `PrototipoFaltasApiService`
- `BandejasDemoService`
- `ActasDemoService`
- `AccionesActaDemoService`

---

## Uso con Cursor

| Slice UI | Contexto mínimo |
|---|---|
| Layout principal | `00` + `01` |
| Panel detalle | `00` + `02` + `05` |
| Acciones por bandeja | `00` + `03` + `05` |
| Alta mock | `00` + `04` |
| Hechos/cerrabilidad | `00` + `05` |
| Guion demo | `00` + `06` + `07` |

No cargar toda la carpeta salvo slice transversal.

---

## Criterio de cierre UX demo

La UI demo queda lista cuando:

- se ven bandejas con cantidades
- se listan actas
- se abre detalle
- se ejecutan acciones
- se refresca la vista
- se explican acciones bloqueadas
- se crean actas mock
- se recorren escenarios principales