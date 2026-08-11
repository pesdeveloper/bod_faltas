# Visualización del Acta — Desktop y Mobile

> **Estado documental:** NORMATIVE
>
> Dimensiones de estado canónicas: `backend/api-faltas-core/docs/spec-as-source/10-domain/lifecycle-states.md`
> Snapshot y bandejas: `backend/api-faltas-core/docs/spec-as-source/30-projections/snapshot-bandejas-acciones.md`

## 1. Estructura general del panel de detalle

El panel de detalle del acta se organiza en bloques verticales secuenciales.
Cada bloque agrupa información relacionada. En desktop el panel es una columna;
en mobile es una pantalla completa scrolleable.

### 1.1 Orden de bloques

| # | Bloque | Descripción |
|---|--------|-------------|
| 1 | Encabezado del acta | Número, estado operativo, acción pendiente, botón "seguir" |
| 2 | Datos del hecho | Fecha, lugar, tipo de infracción, descripción |
| 3 | Datos del infractor | Nombre, DNI, domicilio notificatorio |
| 4 | Estado procesal | Dimensiones de estado del acta |
| 5 | Documentos | Lista de documentos asociados al acta y su estado |
| 6 | Notificaciones | Historial de notificaciones y su resultado |
| 7 | Sección económica | Pago voluntario, condena, pago de condena |
| 8 | Bloqueantes materiales | Lista de bloqueantes activos y resueltos |
| 9 | Apelación | Si existe apelación activa o resuelta |
| 10 | Gestión externa | Si el acta fue derivada o está en gestión externa |
| 11 | Historial de eventos | Timeline de eventos `ActaEvento` append-only |
| 12 | Zona de acciones | Botones de acción habilitados (ver `50-acciones/`) |

## 2. Bloque 1: Encabezado del acta

### 2.1 Contenido

| Campo | Origen | Descripción |
|-------|--------|-------------|
| Número de acta | Snapshot | Identificador principal; tipografía grande y prominente |
| Badge de bandeja | `codBandeja` del snapshot | Label visual de la bandeja actual |
| Acción pendiente | `accionPendiente` del snapshot | Badge o chip con la acción requerida; si es `NINGUNA`, no se muestra o se muestra neutro |
| Botón "Seguir acta" | localStorage | Toggle; activado = icono lleno; desactivado = icono vacío |
| Timestamp de última actualización | Snapshot `fhUltMod` | "Actualizado: hace X minutos" |

### 2.2 Reglas de visualización

- El número de acta es siempre visible incluso en mobile.
- El badge de bandeja usa el label legible (no el código backend).
- Si `accionPendiente = NINGUNA` y el acta no tiene situación especial, no se muestra badge de acción.

## 3. Bloque 2: Datos del hecho

| Campo | Descripción |
|-------|-------------|
| Fecha del hecho | Fecha y hora en que ocurrió la infracción |
| Lugar del hecho | Dirección o descripción geográfica |
| Tipo de infracción | Categoría normativa |
| Descripción del hecho | Texto libre del agente |
| Inspector / Agente interviniente | Quién labró el acta |

Este bloque es siempre visible. En mobile puede colapsar.

## 4. Bloque 3: Datos del infractor

| Campo | Descripción |
|-------|-------------|
| Apellido y nombre | Nombre completo |
| DNI | Número de documento |
| Fecha de nacimiento | Calculado como edad al momento del hecho |
| Domicilio notificatorio activo | Dirección usada para notificaciones; campo `idDomicilioNotifAct` del acta |
| Estado del domicilio | Indicador de si el domicilio está configurado (requerido para notificaciones postales/municipales) |

### 4.1 Alerta de domicilio faltante

Si el acta no tiene `idDomicilioNotifAct` configurado y la bandeja actual requiere notificación postal o vía notificador municipal, se muestra un aviso en este bloque:

> "El acta no tiene domicilio notificatorio configurado. Las opciones de notificación postal y municipal no estarán disponibles."

## 5. Bloque 4: Estado procesal

Este bloque distingue explícitamente las cuatro dimensiones de estado del dominio.
Es crítico para el operador entender la situación del acta. El frontend no infiere ni calcula estas dimensiones; las lee del snapshot.

| Dimensión | Campo snapshot | Descripción |
|-----------|---------------|-------------|
| Bloque actual | `bloqueActual` | Etapa del ciclo de vida: CAPT, ENRI, NOTI, ANAL, GEXT, CERR, ARCH |
| Estado procesal | `estadoProcesal` | Estado procesal del acta (EN_TRAMITE, CONCLUIDO, etc.) |
| Situación administrativa | `situacionAdministrativa` | ACTIVA, EN_GESTION_EXTERNA, CERRADA, ANULADA, ARCHIVADA, PARALIZADA |
| Resultado final | `resultadoFinal` | SIN_RESULTADO_FINAL, PAGO_VOLUNTARIO_PAGADO, ABSUELTO, CONDENA_FIRME, CONDENA_FIRME_PAGADA |

### 5.1 Reglas de visualización de dimensiones

- Cada dimensión se muestra con su label legible, no con el código.
- Si `resultadoFinal = SIN_RESULTADO_FINAL`: no se muestra o se muestra como "Sin resultado final" en estilo neutro/secundario.
- Si `situacionAdministrativa = CERRADA`: todo el encabezado del detalle se muestra con estilo "cerrado" (badge especial, fondo diferenciado).

### 5.2 Sub-bloque: Fallo activo

Si existe fallo activo (`FalActaFallo`), se muestra dentro del bloque de estado procesal:

| Campo | Descripción |
|-------|-------------|
| Tipo de fallo | Absolutorio / Condenatorio |
| Estado del fallo | `estadoFallo`: PENDIENTE_FIRMA, PENDIENTE_NOTIFICACION, NOTIFICADO, FIRME |
| Monto de condena | Solo si tipo = Condenatorio |
| Fecha de firma | `fhFirma` |
| Fecha de notificación | `fhNotificacion` |
| Fecha vencimiento apelación | `fhVtoApelacion` (LocalDate) |
| Días hábiles restantes | Calculado por el frontend: días entre hoy y `fhVtoApelacion`; solo si fallo NOTIFICADO y plazo no vencido |
| Indicador de plazo vencido | Si `hoy > fhVtoApelacion`: muestra "Plazo de apelación vencido el [fecha]" con estilo de alerta |

#### 5.2.1 Aviso contextual de plazo de apelación vencido

Cuando el acta está en bandeja `PENDIENTES_FALLO` (accionPendiente = NINGUNA) y `hoy > fhVtoApelacion`:

- Se muestra en el bloque de estado procesal: "Plazo de apelación vencido. Puede declararse la condena firme."
- Un botón "Declarar Condena Firme" aparece en la zona de acciones (bloque 12), anclado a CMD-FALLO-005.
- Este botón es un guardrail de la UI; el proceso nocturno del backend puede ejecutar CMD-FALLO-005 automáticamente. Ver `90-gaps-pendientes/gaps-backend-spec.md`.

## 6. Bloque 5: Documentos

| Campo por documento | Descripción |
|--------------------|-------------|
| Tipo de documento | `tipoDocu` con label legible |
| Estado | `estadoDocu`: PENDIENTE_FIRMA, FIRMADO |
| Número de documento | `nroDocu` si fue numerado |
| Fecha de firma | Si firmado |
| Acciones de descarga | Enlace al documento si está disponible |

### 6.1 Aviso de documento en firma externa

Si hay documentos en estado `PENDIENTE_FIRMA`:

> "Este documento está pendiente de firma. La firma se realiza en la aplicación de Firmas."

No se muestra botón de firma en esta aplicación; la acción `FIRMAR_DOCUMENTO` es externa.
Ver `50-acciones/acciones-botones-habilitacion.md` sección sobre FIRMAR_DOCUMENTO.

## 7. Bloque 6: Notificaciones

| Campo | Descripción |
|-------|-------------|
| Estado de la notificación | `estado`: EN_PROCESO, CON_ACUSE_POSITIVO, CON_ACUSE_NEGATIVO, VENCIDA |
| Canal de notificación | CORREO_POSTAL, NOTIFICADOR_MUNICIPAL, EMAIL, PRESENCIAL, PORTAL_INFRACTOR |
| Fecha de envío | `fechaEnvio` |
| Resultado | Positivo / Negativo / Sin resultado |
| Número de intentos | `intentos` |

Este bloque muestra el historial completo de notificaciones del acta.

## 8. Bloque 7: Sección económica

### 8.1 Sub-bloque: Pago voluntario

Visible si existe `FalPagoVoluntario` para el acta:

| Campo | Descripción |
|-------|-------------|
| Estado del pago voluntario | SOLICITADO, MONTO_FIJADO, PENDIENTE_CONFIRMACION, CONFIRMADO, OBSERVADO, VENCIDO |
| Monto fijado | Si `estado >= MONTO_FIJADO` |
| Referencia de pago | Si el infractor informó el pago |
| Motivo de observación | Si `estado = OBSERVADO` |
| Fecha de vencimiento | Si aplica |

#### 8.1.1 Botón contextual de "Pago voluntario"

El flujo de pago voluntario tiene múltiples sub-comandos (SolicitarPagoVoluntario, FijarMontoPagoVoluntario, InformarPagoVoluntario, etc.). Los botones correspondientes se muestran en este sub-bloque cuando el estado del pago y la accionPendiente lo habilitan. Ver detalle en `50-acciones/acciones-botones-habilitacion.md`.

### 8.2 Sub-bloque: Pago de condena

Visible si `resultadoFinal = CONDENA_FIRME` o existe `FalPagoCondena`:

| Campo | Descripción |
|-------|-------------|
| Estado del pago de condena | PENDIENTE, INFORMADO, OBSERVADO, CONFIRMADO |
| Monto de condena | Del fallo condenatorio |
| Monto abonado informado | `monto` del FalPagoCondena |
| Referencia de pago | `referenciaPago` |
| Motivo de observación | Si `estado = OBSERVADO` |

### 8.3 Sub-bloque: PAGANT (pago aplicado a obligación anterior)

Si existe un evento `PAGANT` sin `PAGRES` asociado, se muestra en la sección económica:

> "Existe un pago informado contra una obligación anterior pendiente de resolución administrativa."

Este aviso es solo informativo (no hay accionPendiente dedicada en el snapshot para este caso).
El operador puede ver el detalle en el historial de eventos.

## 9. Bloque 8: Bloqueantes materiales

Visible si el acta tiene bloqueantes materiales registrados:

| Campo | Descripción |
|-------|-------------|
| Estado del bloqueante | PENDIENTE (activo) / CUMPLIDO / ANULADO |
| Origen del bloqueante | `OrigenBloqueanteMaterial` |
| Fecha de alta | `fechaAlta` |
| Fecha de cierre | `fechaCierre` si fue resuelto |
| Botón "Cumplir" | Solo si estado = PENDIENTE; acción CumplirBloqueanteMaterial |
| Botón "Anular" | Solo si estado = PENDIENTE; acción AnularBloqueanteMaterial |

### 9.1 Aviso cuando no puede cerrar

Si el acta tiene `resultadoFinal` cerrable (PAGO_VOLUNTARIO_PAGADO, ABSUELTO, CONDENA_FIRME_PAGADA)
y aún hay bloqueantes activos, se muestra en este bloque:

> "El acta no puede cerrarse hasta resolver todos los bloqueantes. Resolución pendiente: [N bloqueantes]."

No hay botón de cierre manual; el cierre es diferido automático al resolver el último bloqueante.

## 10. Bloque 9: Apelación

Visible si existe `FalActaApelacion`:

| Campo | Descripción |
|-------|-------------|
| Estado de la apelación | PRESENTADA, EN_ANALISIS, RECHAZADA, ACEPTADA_ABSUELVE, RESUELTA |
| Presentante | Quién presentó la apelación |
| Fundamentos | Texto de la apelación |
| Fecha de resolución | Si fue resuelta |
| Fundamentos de resolución | Si fue resuelta |

## 11. Bloque 10: Gestión externa

Visible si existe `FalGestionExterna` con `siActiva = true` o historial:

| Campo | Descripción |
|-------|-------------|
| Estado de gestión | DERIVADA, EN_CURSO, REINGRESADA, CERRADA_EXTERNA |
| Fecha de derivación | `fhAlta` |
| Modo de reingreso | Si fue reingresada |
| Motivo de reingreso | Si fue reingresada |
| Resultado de gestión | Si fue reingresada con resultado |
| Monto resultado | Si `modoReingreso = MODIFICA_MONTO` |

## 12. Bloque 11: Historial de eventos

Timeline cronológico de todos los eventos `ActaEvento` del acta (append-only).

| Campo por evento | Descripción |
|-----------------|-------------|
| Tipo de evento | Label legible del `TipoEventoActa` |
| Fecha y hora | `fhEvt` |
| Actor | `idUserEvt` |
| Descripción | `descripcionLegible` |

### 12.1 Reglas de visualización del timeline

- Orden: más reciente primero (descendente).
- Paginado o con "Mostrar más" si hay muchos eventos.
- Cada tipo de evento tiene su ícono o color diferenciador.
- En mobile, el timeline puede estar colapsado por defecto.

## 13. Bloque 12: Zona de acciones

Ver especificación completa en `50-acciones/acciones-botones-habilitacion.md`.

### 13.1 Posición

- En desktop: fija en la parte inferior del panel de detalle, siempre visible.
- En mobile: barra de acciones fija en la parte inferior de la pantalla.

### 13.2 Jerarquía de botones

1. **Acción primaria**: derivada de `accionPendiente`; botón prominente (filled).
2. **Acciones secundarias**: derivadas de `accionesUi`; botones de menor jerarquía visual (outlined o text).
3. **Acciones contextuales**: aparecen por condición específica (plazo vencido, bloqueantes, etc.); mismo nivel visual que secundarias.

## 14. Reglas de rendering mobile vs desktop

| Aspecto | Desktop | Mobile |
|---------|---------|--------|
| Panel de detalle | Columna derecha, 50%–60% | Pantalla completa |
| Bloques colapsables | Opcionales; expandidos por defecto | Recomendado colapsar bloques secundarios |
| Zona de acciones | Fija en pie del panel | Barra fija en pie de pantalla |
| Timeline de eventos | Visible por defecto | Colapsado por defecto |
| Sección económica | Siempre visible | Puede estar colapsada |

## 15. Estados de carga del detalle

| Estado | Comportamiento |
|--------|----------------|
| Cargando | Skeleton del panel completo |
| Acta no encontrada | Mensaje de error en el panel |
| Error de red | Mensaje con botón reintentar |
| Acta cerrada | Panel completo con estilo "cerrado"; acciones deshabilitadas excepto consulta |
