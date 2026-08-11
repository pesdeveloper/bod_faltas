# Acciones — Formularios y Datos Requeridos

> **Estado documental:** NORMATIVE
>
> Fuente de contratos: `backend/api-faltas-core/docs/spec-as-source/20-application/command-contracts.md`
> Fuente de contratos de fallo: `backend/api-faltas-core/docs/spec-as-source/20-application/fallo-command-contracts.md`

## Principio

Este documento define los campos que debe mostrar cada formulario de acción.
Los campos se anclan a las precondiciones y campos del contrato de comando del backend.
No se agregan campos no presentes en el contrato.

---

## Acción: COMPLETAR_CAPTURA (CompletarCaptura)

**Tipo UX:** Formulario

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| (según spec del acta) | actaId (path) | sí | Identificado desde el contexto |

> La spec de campos específicos de captura se definirá cuando el contrato HTTP correspondiente esté documentado en `40-api/http-contracts.md`.

---

## Acción: GENERAR_DOCUMENTO (GenerarDocumento)

**Tipo UX:** Confirmación simple

No requiere datos adicionales del operador. Solo confirmar la generación.

---

## Acción: ENVIAR_NOTIFICACION (EnviarNotificacion — CMD-FALLO-002)

**Tipo UX:** Formulario

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Documento a notificar | `idDocumento` (body) | sí | Selector del documento firmado a notificar |
| Canal de notificación | `canal` (body) | sí | Selector: CORREO_POSTAL, NOTIFICADOR_MUNICIPAL, EMAIL, PRESENCIAL |
| Destino digital | `destinoDigital` (body) | Condicional | Obligatorio y max 120 chars cuando canal es EMAIL. Vacío para otros canales |
| Referencia externa | `referenciaExterna` (body) | no | Max 80 chars; trim; no solo espacios |
| Observaciones | `observaciones` (body) | no | Texto libre |

**Validaciones en UI:**

- Si `canal = CORREO_POSTAL` o `NOTIFICADOR_MUNICIPAL`: verificar que el acta tenga `idDomicilioNotifAct` configurado; si no, mostrar aviso antes de habilitar el botón.
- `PORTAL_INFRACTOR` no es un canal seleccionable desde esta pantalla (backend lo rechaza; el canal portal es exclusivo de la variante portal de CMD-FALLO-004).

---

## Acción: EVALUAR_NOTIFICACION (registrar resultado notificatorio)

**Tipo UX:** Formulario con dos variantes (positivo / negativo)

### Variante: Resultado positivo (CMD-FALLO-004)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Intento notificatorio | `intentoId` (body) | sí | Selector del intento activo en EN_PROCESO |
| Observaciones | `observaciones` (body) | no | Texto libre |

### Variante: Resultado negativo

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Intento notificatorio | (según contrato HTTP) | sí | Selector del intento activo |
| Observaciones | `observaciones` (body) | no | Texto libre |

### Variante: Notificación vencida

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Intento notificatorio | (según contrato HTTP) | sí | Selector del intento activo |
| Observaciones | `observaciones` (body) | no | Texto libre |

---

## Acción: DECIDIR_REINTENTO_O_GESTION

**Tipo UX:** Formulario con dos opciones

El operador elige entre:

1. Reintentar notificación → flujo de ENVIAR_NOTIFICACION.
2. Derivar a gestión externa → flujo de DERIVAR_GESTION_EXTERNA.

---

## Acción: DICTAR_FALLO

**Tipo UX:** Formulario con dos variantes (absolutorio / condenatorio)

### Variante: Fallo absolutorio (DictarFalloAbsolutorioCommand)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Fundamentos | `fundamentos` (body) | sí | Texto de los fundamentos del fallo |
| Observaciones | `observaciones` (body) | no | Texto libre adicional |

### Variante: Fallo condenatorio (DictarFalloCondenatorioCommand)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Monto de condena | `montoCondena` (body) | sí | Importe mayor a cero; numérico |
| Fundamentos | `fundamentos` (body) | sí | Texto de los fundamentos del fallo |
| Observaciones | `observaciones` (body) | no | Texto libre adicional |

**Validaciones en UI:**

- El selector de tipo de fallo (absolutorio/condenatorio) es el primer campo.
- Si condenatorio: mostrar campo de monto condena con validación > 0.
- Confirmar antes de enviar (acción de alto impacto).

---

## Acción: RESOLVER_APELACION

**Tipo UX:** Formulario con dos variantes (rechazar / aceptar absuelve)

### Variante: Rechazar apelación (ResolverApelacionRechazadaCommand)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Fundamentos de resolución | `fundamentosResolucion` (body) | sí | Texto de los fundamentos del rechazo |
| Observaciones | `observaciones` (body) | no | Texto libre |

### Variante: Aceptar apelación absuelve (ResolverApelacionAceptaAbsuelveCommand)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Fundamentos de resolución | `fundamentosResolucion` (body) | sí | Texto de los fundamentos de la aceptación |
| Observaciones | `observaciones` (body) | no | Texto libre |

---

## Acción: DECLARAR_CONDENA_FIRME (CMD-FALLO-006 — apelación rechazada)

**Tipo UX:** Confirmación simple

Solo requiere confirmación del operador. Operacionalmente se llama cuando la apelación fue rechazada.

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Observaciones | `observaciones` (body) | no | Texto libre |

---

## Acción contextual: DECLARAR_CONDENA_FIRME por vencimiento (CMD-FALLO-005)

**Tipo UX:** Confirmación simple (aparece solo cuando `hoy > fhVtoApelacion`)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Observaciones | `observaciones` (body) | no | Texto libre |

**Nota:** Este botón solo aparece como guardrail. El backend puede ejecutar CMD-FALLO-005 automáticamente vía proceso nocturno. Ver `90-gaps-pendientes/gaps-backend-spec.md`.

---

## Acción: GESTIONAR_PAGO_CONDENA (CMD-FALLO-007 — Informar pago de condena)

**Tipo UX:** Formulario

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Monto abonado | `monto` (body) | sí | Importe mayor a cero; numérico |
| Referencia de pago | `referenciaPago` (body) | sí | Identificador del comprobante; @NotBlank |
| Observaciones | `observaciones` (body) | no | Texto libre |

**Nota temporal:** `referenciaPago` es un dato provisional. En producción será reemplazado por la integración con el sistema de Ingresos/Tesorería (`cmtePG`/`prefPG`/`nroPG`). Ver deuda técnica en el backend spec.

---

## Acción: CONFIRMAR_PAGO_CONDENA (ConfirmarPagoCondenaCommand)

**Tipo UX:** Confirmación simple

No requiere datos adicionales.

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Observaciones | `observaciones` (body) | no | Texto libre |

---

## Acción: CORREGIR_PAGO_CONDENA (reinformar pago — CMD-FALLO-007)

**Tipo UX:** Formulario (mismo formulario que GESTIONAR_PAGO_CONDENA pero pre-cargado con datos existentes)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Monto abonado | `monto` (body) | sí | Importe mayor a cero; pre-cargado con el valor anterior |
| Referencia de pago | `referenciaPago` (body) | sí | Pre-cargado con el valor anterior |
| Motivo de corrección | `observaciones` (body) | recomendado | El backend no lo requiere pero es buena práctica |

---

## Acción: CONFIRMAR_PAGO (ConfirmarPagoVoluntario)

**Tipo UX:** Confirmación simple

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Observaciones | `observaciones` (body) | no | Texto libre |

---

## Acción: CORREGIR_PAGO (ObservarPagoVoluntario → re-informar)

El backend tiene el comando `ObservarPagoVoluntario` para observar el pago (requiere `motivoObservacion`).
La corrección es re-informar el pago después de la observación, usando `InformarPagoVoluntario`.

### Observar pago voluntario (acción del verificador)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Motivo de observación | `motivoObservacion` (body) | sí | Texto explicando por qué se observa |
| Observaciones | `observaciones` (body) | no | Texto libre adicional |

### Informar pago voluntario (InformarPagoVoluntario)

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Referencia de pago | `referenciaPago` (body) | sí | Identificador del pago; @NotBlank |
| Observaciones | `observaciones` (body) | no | Texto libre |

---

## Flujo de pago voluntario (multi-paso)

El pago voluntario es un flujo de múltiples comandos. Los botones disponibles
dependen del estado de `FalPagoVoluntario`:

| Estado PagoVoluntario | Botón disponible | Comando |
|-----------------------|------------------|---------|
| No existe | "Solicitar pago voluntario" | SolicitarPagoVoluntario |
| SOLICITADO | "Fijar monto" | FijarMontoPagoVoluntario |
| MONTO_FIJADO | "Informar pago" / "Vencer" | InformarPagoVoluntario / VencerPagoVoluntario |
| PENDIENTE_CONFIRMACION | "Confirmar pago" / "Observar pago" | ConfirmarPagoVoluntario / ObservarPagoVoluntario |
| OBSERVADO | "Informar pago" (corrección) | InformarPagoVoluntario |
| VENCIDO | (sin botones de pago voluntario; el flujo derivó a DICTAR_FALLO) | — |
| CONFIRMADO | (pago cerrado; acta cerrada) | — |

---

## Acción: DERIVAR_GESTION_EXTERNA

**Tipo UX:** Formulario

| Campo UI | Mapeo backend | Requerido | Descripción |
|----------|--------------|-----------|-------------|
| Motivo de derivación | (según contrato HTTP) | sí | Texto explicando la derivación |
| Observaciones | `observaciones` (body) | no | Texto libre |

---

## Bloqueantes materiales

### Cumplir bloqueante (CumplirBloqueanteMaterial)

**Tipo UX:** Confirmación simple por bloqueante.

No requiere campos adicionales. El `bloqueanteId` se obtiene del contexto (bloqueante seleccionado).

### Anular bloqueante (AnularBloqueanteMaterial)

**Tipo UX:** Confirmación simple por bloqueante.

No requiere campos adicionales. El `bloqueanteId` se obtiene del contexto.
