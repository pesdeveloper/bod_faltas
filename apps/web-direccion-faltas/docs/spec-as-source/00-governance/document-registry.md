# Registro documental — web-direccion-faltas spec-as-source

> **Estado documental:** NORMATIVE
> Toda clasificación de documentos de esta spec UX vive aquí.

## 1. Clasificaciones posibles

| Clasificación | Significado |
|---------------|-------------|
| `NORMATIVE` | Documento de referencia obligatoria para implementación |
| `SUPPORTING` | Referencia de apoyo; no es fuente de verdad por sí sola |
| `GAP_PENDING` | Identifica una brecha que requiere resolución antes de implementar |

## 2. Documentos de esta spec

| Documento | Clasificación | Descripción |
|-----------|---------------|-------------|
| `README.md` | NORMATIVE | Índice, principios, alcance, precedencia |
| `00-governance/document-registry.md` | NORMATIVE | Este archivo |
| `10-layout/layout-y-navegacion.md` | NORMATIVE | Layout general, mobile-first, sidebar, overlays, navegación |
| `20-bandejas/bandejas-agrupamiento-routing.md` | NORMATIVE | Agrupamiento visual de 16 bandejas canónicas en 13 entradas sidebar |
| `30-busqueda-filtros/busqueda-filtros-panel.md` | NORMATIVE | Búsqueda, filtros, panel resumen, preferencias usuario |
| `40-acta/visualizacion-acta-desktop-mobile.md` | NORMATIVE | Detalle del acta: bloques, dimensiones, desktop/mobile |
| `50-acciones/acciones-botones-habilitacion.md` | NORMATIVE | Habilitación de acciones por snapshot, tipos de interacción, post-acción |
| `50-acciones/acciones-formularios-datos.md` | NORMATIVE | Campos de formulario por acción, anclados a contratos del backend |
| `60-pantallas-operativas/correo-postal.md` | NORMATIVE | Pantalla de gestión de lotes de correo postal |
| `60-pantallas-operativas/notificador-municipal.md` | NORMATIVE | Pantalla de gestión de notificador municipal |
| `90-gaps-pendientes/gaps-backend-spec.md` | GAP_PENDING | Brechas en la spec backend que bloquean o condicionan UX |

## 3. Referencias al backend spec

Los siguientes documentos del backend son referencias normativas directas para esta spec UX:

| Documento backend | Relevancia para UX |
|-------------------|--------------------|
| `backend/api-faltas-core/docs/spec-as-source/30-projections/snapshot-bandejas-acciones.md` | Define las 16 bandejas canónicas, el routing y las 20 acciones pendientes |
| `backend/api-faltas-core/docs/spec-as-source/20-application/command-contracts.md` | Define precondiciones y campos de todos los comandos generales |
| `backend/api-faltas-core/docs/spec-as-source/20-application/fallo-command-contracts.md` | Define precondiciones y campos de CMD-FALLO-001..007 |
| `backend/api-faltas-core/docs/spec-as-source/10-domain/lifecycle-states.md` | Define dimensiones de estado: BloqueActual, EstadoProcesalActa, SituacionAdministrativaActa, ResultadoFinalActa |
| `backend/api-faltas-core/docs/spec-as-source/10-domain/states-events-catalogs.md` | Catálogo completo de bloques, estados y eventos de dominio |
| `backend/api-faltas-core/docs/spec-as-source/10-domain/firma-notificacion-fallo.md` | Circuito de firma (externo via App de Firmas) y cola notificatoria |
| `backend/api-faltas-core/docs/spec-as-source/40-api/http-contracts.md` | Contratos HTTP de los endpoints que el frontend consume |

## 4. Restricciones de uso

- Esta spec no puede redefinir bandejas, estados, eventos ni transiciones del backend.
- Si una decisión UX requiere un cambio en la spec backend, debe documentarse en `90-gaps-pendientes/gaps-backend-spec.md` y requerir aprobación humana explícita antes de implementar.
- Los documentos de la carpeta `apps/web-direccion-faltas/docs/` (prototipo anterior) son fuentes históricas de referencia de UX; no son normativas y no pueden prevalecer sobre esta spec ni sobre la spec del backend.
