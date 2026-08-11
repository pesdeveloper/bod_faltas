# Acciones — Botones y Habilitación

> **Estado documental:** NORMATIVE
>
> Contratos de comandos: `backend/api-faltas-core/docs/spec-as-source/20-application/command-contracts.md`
> Contratos de fallo: `backend/api-faltas-core/docs/spec-as-source/20-application/fallo-command-contracts.md`
> Bandejas y acciones: `backend/api-faltas-core/docs/spec-as-source/30-projections/snapshot-bandejas-acciones.md`

## 1. Principio de backend-driven UI para acciones

El frontend nunca decide qué acciones están disponibles por su propia lógica.
La fuente de verdad es siempre el snapshot del backend:

- `accionPendiente`: acción primaria que el backend indica que debe ejecutarse.
- `accionesUi`: conjunto de acciones secundarias habilitadas por el backend (a definir en el contrato HTTP).

El botón de una acción se habilita solo si el backend lo indica. Nunca se habilita
por inferencia del estado del acta en el frontend.

## 2. Tipos de interacción de botón

| Tipo | Descripción | UX |
|------|-------------|-----|
| **Externo** | La acción ocurre en otra aplicación; el backend recibe un callback. | Botón informativo o enlace; no ejecuta el backend directamente. |
| **Formulario** | Requiere datos del operador antes de ejecutar el backend. | Abre modal o pantalla con campos; envía al confirmar. |
| **Confirmación simple** | No requiere datos adicionales; solo confirmación del operador. | Diálogo de confirmación "¿Estás seguro?"; ejecuta al confirmar. |
| **Destructivo** | Acción irreversible o de alto impacto. | Diálogo de confirmación con texto explícito de consecuencias; botón rojo. |
| **Informativo** | No ejecuta ningún comando; muestra información o estado. | No hay botón de submit; solo lectura. |

## 3. Fuente de las acciones canónicas

### 3.1 Acciones pendientes canónicas (AccionPendiente)

Definidas en `backend/.../30-projections/snapshot-bandejas-acciones.md`:

| Valor | Tipo de interacción UX | Descripción |
|-------|------------------------|-------------|
| `COMPLETAR_CAPTURA` | Formulario | Completar datos de captura del acta |
| `ENRIQUECER` | Formulario | Enriquecer el acta con documentación adicional |
| `GENERAR_DOCUMENTO` | Confirmación simple | Generar el documento procesal correspondiente |
| `FIRMAR_DOCUMENTO` | **Externo** | Redirige a la App de Firmas; NO es botón de acción directa |
| `ENVIAR_NOTIFICACION` | Formulario | Iniciar el envío de notificación (canal, destino) |
| `EVALUAR_NOTIFICACION` | Formulario | Registrar el resultado del intento notificatorio |
| `DECIDIR_REINTENTO_O_GESTION` | Formulario | Decidir si reintentar notificación o derivar a gestión externa |
| `DICTAR_FALLO` | Formulario | Dictar fallo absolutorio o condenatorio |
| `RESOLVER_APELACION` | Formulario | Resolver la apelación presentada |
| `DECLARAR_CONDENA_FIRME` | Confirmación simple | Declarar firmeza (CMD-FALLO-006; apelación rechazada) |
| `GESTIONAR_PAGO_CONDENA` | Formulario | Informar el pago de condena (CMD-FALLO-007) |
| `CONFIRMAR_PAGO_CONDENA` | Confirmación simple | Confirmar el pago de condena informado |
| `CORREGIR_PAGO_CONDENA` | Formulario | Corregir pago de condena observado (reinformar) |
| `REGISTRAR_PAGO` | Formulario | Registrar pago (contexto pago voluntario) |
| `DERIVAR_GESTION_EXTERNA` | Formulario | Derivar el acta a gestión externa |
| `CONFIRMAR_PAGO` | Confirmación simple | Confirmar el pago voluntario informado |
| `CORREGIR_PAGO` | Formulario | Corregir pago voluntario observado |
| `NINGUNA` | — | Sin acción primaria disponible; no se muestra botón primario |

## 4. Tratamiento especial: FIRMAR_DOCUMENTO

`FIRMAR_DOCUMENTO` NO es una acción que ejecute `web-direccion-faltas` directamente.

El circuito canónico de firma es:

1. El documento tiene `estadoDocu = PENDIENTE_FIRMA`.
2. La App de Firmas toma el documento, lo numera, lo renderiza, calcula el hash y registra la firma real via `POST /api/faltas/documentos/{documentoId}/firmar-real` (CMD-FALLO-001).
3. El backend recibe el callback de Firmas y actualiza el estado del documento y del acta.

**Comportamiento UX en `web-direccion-faltas` cuando `accionPendiente = FIRMAR_DOCUMENTO`:**

- Se muestra un aviso informativo en la zona de acciones: "Este documento debe firmarse en la aplicación de Firmas."
- Se puede mostrar un enlace o botón que dirija al operador a la App de Firmas (si existe URL de integración).
- NO se muestra un botón de "Firmar" que llame al backend directamente desde esta aplicación.
- La zona de acciones permanece en estado informativo hasta que el callback de Firmas actualice el snapshot.

## 5. Acciones contextuales (sin accionPendiente)

Estas acciones aparecen en la zona de acciones por condición del estado del acta,
incluso cuando `accionPendiente = NINGUNA`:

### 5.1 Declarar Condena Firme (por vencimiento de plazo)

**Condición:** Acta en bandeja `PENDIENTES_FALLO` con `accionPendiente = NINGUNA`
y `hoy > fhVtoApelacion`.

- Botón: "Declarar Condena Firme" (Confirmación simple).
- Comando backend: CMD-FALLO-005 (`POST /api/faltas/actas/{id}/firmeza/vencer-plazo-apelacion`).
- Este botón es un guardrail UX; el backend también puede declarar la firmeza vía proceso nocturno.
- Ver `90-gaps-pendientes/gaps-backend-spec.md` para la decisión sobre el proceso nocturno.

### 5.2 Gestionar bloqueantes materiales

**Condición:** El acta tiene bloqueantes en estado `PENDIENTE` (activos).

- Botones "Cumplir" y "Anular" por cada bloqueante activo.
- Disponibles en el bloque 8 del detalle del acta.
- El cierre del acta es diferido automático al resolver el último bloqueante; no hay botón de cierre manual.

### 5.3 Solicitar pago voluntario

**Condición:** Acta en bloque ENRI o ANAL sin pago voluntario confirmado.

El flujo de pago voluntario tiene múltiples pasos; los botones específicos se muestran según el sub-estado del `FalPagoVoluntario`. Ver `50-acciones/acciones-formularios-datos.md`.

## 6. Habilitación y deshabilitación de botones

### 6.1 Regla general

Un botón de acción está habilitado solo si el snapshot del backend lo indica (`accionPendiente` o `accionesUi`).

### 6.2 Reglas de deshabilitación adicionales (defensivas en frontend)

El frontend puede deshabilitar adicionalmente un botón si:

- La sesión del usuario no tiene el rol requerido para esa acción (según configuración de roles).
- La aplicación está en estado de carga o hay una operación en curso.
- El acta está en situación administrativa CERRADA, ANULADA, ARCHIVADA o PARALIZADA.

El frontend nunca habilita un botón que el backend no haya indicado como disponible.

### 6.3 Botones siempre disponibles (de consulta)

Independientemente del snapshot, los siguientes no son acciones de comando y siempre están disponibles:

- Ver historial de eventos (expandir bloque timeline).
- Descargar documentos disponibles.
- Activar/desactivar "Seguir acta".

## 7. Post-acción: comportamiento de la UI

Después de ejecutar exitosamente una acción:

1. Cerrar el modal o formulario (si aplicaba).
2. Recargar el snapshot del acta desde el backend (para reflejar la nueva bandeja y accionPendiente).
3. Si el acta cambió de bandeja y el operador no la sigue: la acta puede desaparecer de la lista visible (si la lista muestra solo la bandeja anterior).
4. Si el operador sigue el acta: ver regla de "seguir acta" en `20-bandejas/bandejas-agrupamiento-routing.md` sección 7.
5. Actualizar el contador de la bandeja en el sidebar.
6. Mostrar un toast de confirmación de la acción ejecutada.

### 7.1 Error en la acción

Si el backend devuelve error (4xx/5xx):

- Mostrar el mensaje de error del backend en el modal o como toast.
- No cerrar el modal si el error es recuperable (ej: campo inválido).
- Si el error es de estado (422): mostrar el mensaje e indicar que el snapshot puede estar desactualizado; recargar el snapshot.

## 8. Agrupamiento visual de botones

| Zona | Contenido | Posición |
|------|-----------|----------|
| Botón primario | Acción derivada de `accionPendiente` | Botón prominente (filled); primero a la derecha o fijo en la barra inferior |
| Botones secundarios | Acciones de `accionesUi` | Botones de menor jerarquía; agrupados junto al primario |
| Acciones contextuales | Según condición (plazo vencido, etc.) | Misma zona; misma jerarquía que secundarios |
| Acciones destructivas | Anular, Paralizar, etc. | Separadas visualmente; botón con estilo de riesgo |

En mobile, todos los botones se apilan verticalmente en la barra de acciones fija en el pie de pantalla.
Si hay más de 3 botones, los secundarios se colapsan en un menú "Más acciones".
