# [CANONICO] TRANSICIONES DEL SNAPSHOT

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir las transiciones operativas principales que puede reflejar `ActaSnapshotOperativo`, estableciendo cómo evoluciona la lectura resumida del caso frente a los eventos y cambios relevantes del dominio.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/03-catalogos/07-integracion-externa.md`
- `spec/03-catalogos/08-motivos-cierre.md`
- `spec/04-snapshot/00-snapshot-engine.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/05-queries/00-bandejas-operativas.md`

---

# 1. Definición general

Las transiciones del snapshot describen cómo cambia la proyección operativa del caso cuando ocurre un hecho del dominio con impacto relevante.

No describen el detalle completo del dominio ni reemplazan la lógica procesal completa del expediente.

Su función es establecer, de manera clara y canónica:

- qué cambios del dominio alteran la lectura operativa
- qué campos del snapshot suelen verse afectados
- qué prioridades semánticas deben respetarse
- cómo debe evolucionar la visión resumida del caso

---

# 2. Regla general

Toda transición del snapshot debe responder a un hecho del dominio válido.

Ese hecho puede provenir de:

- cambio válido en `Acta`
- alta de `ActaEvento`
- cambio relevante en `Documento`
- cambio relevante en `Notificacion`
- ingreso o actualización de integración externa
- reproceso autorizado

El snapshot no debe transicionar por razones puramente visuales o técnicas.

---

# 3. Naturaleza de una transición

Una transición del snapshot puede afectar uno o más de estos niveles:

- `EstadoActa`
- flags operativos
- contexto documental
- contexto notificatorio
- contexto externo
- motivo de cierre
- último hito relevante
- fechas operativas

No todas las transiciones modifican todos los niveles.

---

# 4. Regla de precedencia de transición

Si varios hechos compiten por definir la situación actual, el snapshot debe respetar esta precedencia general:

1. cierres válidos
2. anulaciones válidas
3. paralizaciones vigentes
4. gestión externa vigente
5. apelación vigente
6. notificación/acto en curso
7. análisis/enriquecimiento
8. estados previos no vigentes

Esta precedencia se aplica cuando haya conflicto semántico entre señales del dominio.

---

# 5. Transiciones principales del snapshot

## 5.1 Inicio del caso

### Disparadores típicos
- `ACTA_LABRADA`
- `ACTA_IMPORTADA`

### Efectos esperables
- se crea o actualiza snapshot para la acta
- `EstadoActa` pasa a `LABRADA` si no existe estado posterior dominante
- `EstaActiva = true`
- `EstaCerrada = false`
- se fija `FechaHoraCreacionActa`
- se fija `UltimoTipoEventoActa`
- se fija `UltimoOrigenEvento`
- se fija `UltimoEventoFechaHora`

### Observación
Esta transición inaugura la existencia operativa del snapshot.

---

## 5.2 Inicio de enriquecimiento

### Disparadores típicos
- `ACTA_ENRIQUECIMIENTO_INICIADO`

### Efectos esperables
- `EstadoActa = EN_ENRIQUECIMIENTO`
- `RequiereRevisionAdministrativa = true`
- `RequiereAccionOperativa = true`
- actualización de último hito relevante

---

## 5.3 Acta enriquecida

### Disparadores típicos
- `ACTA_ENRIQUECIDA`

### Efectos esperables
- `EstadoActa = ENRIQUECIDA`
- puede apagarse `RequiereRevisionAdministrativa`, según el flujo
- se actualiza último hito
- el caso queda disponible para transición posterior a notificación del acta

---

## 5.4 Acta observada

### Disparadores típicos
- `ACTA_OBSERVADA`

### Efectos esperables
- el snapshot debe reflejar necesidad de tratamiento
- `RequiereRevisionAdministrativa = true`
- `RequiereAccionOperativa = true`
- actualización del último hito
- el `EstadoActa` puede mantenerse o quedar en situación coherente con observación, según la implementación del dominio

### Regla
La observación no debe cerrar el caso por sí sola.

---

## 5.5 Acta subsanada

### Disparadores típicos
- `ACTA_SUBSANADA`

### Efectos esperables
- se remueve o reduce la condición de observación vigente
- el snapshot vuelve a una lectura coherente con el punto del flujo alcanzado
- se actualiza el último hito
- puede disminuir `RequiereRevisionAdministrativa`, si ya no existe pendiente equivalente

---

# 6. Transiciones de notificación del acta

## 6.1 Pendiente de notificación del acta

### Disparadores típicos
- acta enriquecida y habilitada para notificación
- transición válida desde el flujo

### Efectos esperables
- `EstadoActa = PENDIENTE_NOTIFICACION_ACTA`
- `TieneNotificacionActiva = false`
- `RequiereAccionOperativa = true`

---

## 6.2 Notificación del acta en curso

### Disparadores típicos
- `NOTIFICACION_ENVIADA`
- alta o activación de `Notificacion`

### Efectos esperables
- `EstadoActa = NOTIFICACION_ACTA_EN_PROCESO`
- `TieneNotificacionActiva = true`
- `UltimoEstadoNotificacion = ENVIADA` o equivalente vigente
- `UltimoCanalNotificacion` se actualiza
- `EtapaNotificatoriaActual` refleja notificación del acta
- se actualiza último hito

---

## 6.3 Acuse recibido para acta

### Disparadores típicos
- `ACUSE_RECIBIDO`

### Efectos esperables
- `TieneAcuseRecibidoVigente = true`
- `UltimoEstadoNotificacion = ACUSE_RECIBIDO`
- `TieneNotificacionActiva` puede pasar a `false`, si la gestión ya no requiere seguimiento
- `EstadoActa = NOTIFICADA_ACTA`, si no existe otra regla dominante
- se actualiza último hito

---

## 6.4 Acuse rechazado para acta

### Disparadores típicos
- `ACUSE_RECHAZADO`

### Efectos esperables
- `UltimoEstadoNotificacion = ACUSE_RECHAZADO`
- `RequiereReenvioNotificacion = true`
- `TieneNotificacionActiva = true`
- `RequiereAccionOperativa = true`
- el `EstadoActa` normalmente permanece en `NOTIFICACION_ACTA_EN_PROCESO` o equivalente coherente

---

## 6.5 Reenvío de notificación del acta

### Disparadores típicos
- `NOTIFICACION_REENVIADA`

### Efectos esperables
- `UltimoEstadoNotificacion = REENVIADA`
- `RequiereReenvioNotificacion = false` o disminuye, según la regla
- sigue activa la situación notificatoria
- actualización de último hito

---

# 7. Transiciones de análisis

## 7.1 Ingreso a análisis

### Disparadores típicos
- `ANALISIS_INICIADO`
- `DESCARGO_PRESENTADO`
- `COMPARECENCIA_REGISTRADA`
- otras presentaciones relevantes

### Efectos esperables
- `EstadoActa = EN_ANALISIS`
- `RequiereRevisionAdministrativa = true`
- `RequiereAccionOperativa = true`
- si hay presentación vigente: `TienePresentacionPendiente = true`
- actualización de último hito

---

## 7.2 Presentación incorporada

### Disparadores típicos
- `PRESENTACION_INCORPORADA`
- `ACLARACION_PRESENTADA`
- `DESISTIMIENTO_PRESENTADO`
- `SOLICITUD_LIBERACION_PRESENTADA`
- `SOLICITUD_PAGO_VOLUNTARIO`

### Efectos esperables
- `TienePresentacionPendiente = true`, si todavía requiere tratamiento
- actualización de último hito
- puede mantenerse `EN_ANALISIS` o evolucionar según reglas del caso

---

## 7.3 Análisis completado

### Disparadores típicos
- `ANALISIS_COMPLETADO`

### Efectos esperables
- disminuye o desaparece `RequiereRevisionAdministrativa`
- el caso queda en situación compatible con transición a acto administrativo o siguiente etapa
- actualización del último hito

---

# 8. Transiciones documentales

## 8.1 Documento relevante generado

### Disparadores típicos
- documento relevante en estado `GENERADO`
- `DOCUMENTO_INCORPORADO`
- `ACTO_ADMINISTRATIVO_GENERADO`

### Efectos esperables
- actualización de:
  - `UltimoTipoDocumentoRelevante`
  - `UltimoEstadoDocumentoRelevante`
  - `UltimoDocumentoFechaHora`
- el snapshot documental refleja la nueva pieza dominante

---

## 8.2 Documento pendiente de firma

### Disparadores típicos
- documento relevante en `PENDIENTE_FIRMA`
- `RESOLUCION_PENDIENTE_DE_FIRMA`

### Efectos esperables
- `TieneDocumentoPendienteFirma = true`
- `UltimoEstadoDocumentoRelevante = PENDIENTE_FIRMA`
- el caso puede permanecer o pasar a `ACTO_ADMINISTRATIVO_EN_PROCESO`
- `RequiereAccionOperativa = true`, si corresponde

---

## 8.3 Documento firmado

### Disparadores típicos
- documento relevante pasa a `FIRMADO`
- `ACTO_ADMINISTRATIVO_FIRMADO`

### Efectos esperables
- `TieneDocumentoPendienteFirma = false`
- `UltimoEstadoDocumentoRelevante = FIRMADO`
- `UltimoFirmaTipoRelevante` se actualiza
- puede habilitar transición a `PENDIENTE_NOTIFICACION_ACTO`
- actualización de último hito

---

## 8.4 Documento observado

### Disparadores típicos
- documento relevante pasa a `OBSERVADO`

### Efectos esperables
- `TieneDocumentoObservado = true`
- `RequiereAccionOperativa = true`
- posible impacto en revisión administrativa
- actualización del último hito

---

## 8.5 Documento invalidado o reemplazado

### Disparadores típicos
- `DOCUMENTO_INVALIDADO`
- `DOCUMENTO_REEMPLAZADO`
- cambio de estado documental a `INVALIDADO` o `REEMPLAZADO`

### Efectos esperables
- el snapshot debe reflejar la nueva vigencia documental dominante
- puede activarse `TieneDocumentoInvalidadoVigente` si sigue siendo relevante
- el documento activo principal puede cambiar
- actualización del último hito

---

# 9. Transiciones del acto administrativo

## 9.1 Pendiente de acto administrativo

### Disparadores típicos
- fin de análisis con condición de avanzar
- reglas del flujo

### Efectos esperables
- `EstadoActa = PENDIENTE_ACTO_ADMINISTRATIVO`
- `RequiereAccionOperativa = true`

---

## 9.2 Acto administrativo en proceso

### Disparadores típicos
- `ACTO_ADMINISTRATIVO_GENERADO`
- `RESOLUCION_PENDIENTE_DE_FIRMA`
- documento resolutivo en preparación

### Efectos esperables
- `EstadoActa = ACTO_ADMINISTRATIVO_EN_PROCESO`
- contexto documental actualizado
- posible `TieneDocumentoPendienteFirma = true`
- actualización del último hito

---

## 9.3 Acto firmado y listo para notificación

### Disparadores típicos
- `ACTO_ADMINISTRATIVO_FIRMADO`

### Efectos esperables
- `EstadoActa = PENDIENTE_NOTIFICACION_ACTO`
- `TieneDocumentoPendienteFirma = false`
- `UltimoEstadoDocumentoRelevante = FIRMADO`
- actualización de último hito

---

# 10. Transiciones de notificación del acto

## 10.1 Inicio de notificación del acto

### Disparadores típicos
- `NOTIFICACION_ENVIADA` sobre acto administrativo
- alta de notificación correspondiente

### Efectos esperables
- `EstadoActa = NOTIFICACION_ACTO_EN_PROCESO`
- `TieneNotificacionActiva = true`
- `EtapaNotificatoriaActual` refleja notificación del acto
- actualización de datos notificatorios
- actualización del último hito

---

## 10.2 Acuse recibido del acto

### Disparadores típicos
- `ACUSE_RECIBIDO` sobre la notificación del acto

### Efectos esperables
- `UltimoEstadoNotificacion = ACUSE_RECIBIDO`
- `TieneAcuseRecibidoVigente = true`
- `TieneNotificacionActiva = false`, si ya no hay gestión abierta
- `EstadoActa = NOTIFICADA_RESOLUCION`, si no existe otra regla dominante
- actualización del último hito

---

## 10.3 Acuse rechazado del acto

### Disparadores típicos
- `ACUSE_RECHAZADO` sobre la notificación del acto

### Efectos esperables
- `UltimoEstadoNotificacion = ACUSE_RECHAZADO`
- `RequiereReenvioNotificacion = true`
- `TieneNotificacionActiva = true`
- el estado se mantiene en notificación del acto en proceso o equivalente coherente
- actualización del último hito

---

# 11. Transiciones de apelación

## 11.1 Ingreso a apelación

### Disparadores típicos
- `APELACION_INTERPUESTA`

### Efectos esperables
- `EstadoActa = EN_APELACION`
- `TieneApelacionActiva = true`
- `RequiereAccionOperativa = true` o seguimiento equivalente
- actualización del último hito

---

## 11.2 Apelación admitida o elevada

### Disparadores típicos
- `APELACION_ADMITIDA`
- `APELACION_ELEVADA`

### Efectos esperables
- se mantiene `EN_APELACION`
- el snapshot refleja continuidad del tramo recursivo
- actualización del último hito

---

## 11.3 Resultado de apelación recibido

### Disparadores típicos
- `RESULTADO_APELACION_RECIBIDO`
- `ACTO_REVISOR_INCORPORADO`

### Efectos esperables
- el snapshot debe reflejar la nueva situación dominante del caso
- `TieneApelacionActiva` puede pasar a `false`
- puede abrir transición a nuevo acto, cierre o situación externa
- actualización del último hito

---

# 12. Transiciones de gestión externa

## 12.1 Derivación externa iniciada

### Disparadores típicos
- `DERIVACION_EXTERNA_ENVIADA`

### Efectos esperables
- `EstadoActa = EN_GESTION_EXTERNA`
- `TieneGestionExternaActiva = true`
- `RequiereSeguimientoExterno = true`
- `TipoGestionExternaActual` se actualiza
- actualización del último hito

---

## 12.2 Gestión externa admitida

### Disparadores típicos
- `DERIVACION_EXTERNA_ADMITIDA`

### Efectos esperables
- se consolida el tramo externo
- puede mantenerse `EN_GESTION_EXTERNA`
- actualización de fecha de último movimiento externo
- actualización del último hito

---

## 12.3 Actuación externa recibida

### Disparadores típicos
- `ACTUACION_EXTERNA_RECIBIDA`

### Efectos esperables
- actualización de contexto externo
- posible actualización documental
- posible mantenimiento de `RequiereSeguimientoExterno`
- actualización del último hito

---

## 12.4 Resultado externo registrado

### Disparadores típicos
- `RESULTADO_EXTERNO_REGISTRADO`

### Efectos esperables
- `ResultadoExternoActual` se actualiza
- puede mantenerse o cerrarse la gestión externa
- puede abrir transición a cierre, finalización, archivo o nueva revisión
- actualización del último hito

---

## 12.5 Reingreso desde gestión externa

### Disparadores típicos
- `REINGRESO_DESDE_GESTION_EXTERNA`

### Efectos esperables
- `TieneGestionExternaActiva = false`, si ya cesó la gestión externa
- `RequiereSeguimientoExterno = false` o disminuye
- el caso vuelve a estado coherente con el flujo reingresado
- actualización del último hito

---

# 13. Transiciones de paralización

## 13.1 Paralización dispuesta

### Disparadores típicos
- `PARALIZACION_DISPUESTA`

### Efectos esperables
- `EstadoActa = PARALIZADA`
- `EstaParalizada = true`
- `RequiereAccionOperativa` puede cambiar según el sentido del bloqueo
- actualización del último hito

---

## 13.2 Paralización levantada

### Disparadores típicos
- `PARALIZACION_LEVANTADA`

### Efectos esperables
- `EstaParalizada = false`
- el snapshot retorna al estado dominante vigente posterior a la paralización
- actualización del último hito

### Regla
No debe “volver” automáticamente al estado anterior sin evaluar si hubo hechos posteriores o contexto que cambie la situación dominante.

---

# 14. Transiciones de cierre

## 14.1 Archivo

### Disparadores típicos
- `ARCHIVO_DISPUESTO`

### Efectos esperables
- `EstadoActa = ARCHIVADA`
- `EstaCerrada = true`
- `EstaActiva = false`
- `MotivoCierreActa = ARCHIVO_ADMINISTRATIVO`
- `TipoEventoCierre = ARCHIVO_DISPUESTO`
- `FechaHoraCierre` se fija
- actualización del último hito

---

## 14.2 Anulación

### Disparadores típicos
- `ACTA_ANULADA`

### Efectos esperables
- `EstadoActa = ANULADA`
- `EstaCerrada = true`
- `EstaActiva = false`
- `MotivoCierreActa = ANULACION`
- `TipoEventoCierre = ACTA_ANULADA`
- `FechaHoraCierre` se fija
- actualización del último hito

---

## 14.3 Cierre por cumplimiento o pago

### Disparadores típicos
- `PAGO_CONFIRMADO`
- `CUMPLIMIENTO_TOTAL_REGISTRADO`
- `CIERRE_DISPUESTO`, según reglas

### Efectos esperables
- `EstadoActa = FINALIZADA`
- `EstaCerrada = true`
- `EstaActiva = false`
- `MotivoCierreActa` se deriva según el hecho dominante
- `FechaHoraCierre` se fija
- `TieneCierreFirme = true` si corresponde
- actualización del último hito

---

## 14.4 Cierre por resultado externo firme

### Disparadores típicos
- `RESULTADO_EXTERNO_REGISTRADO` con desenlace conclusivo

### Efectos esperables
- `EstadoActa = FINALIZADA`, si la regla del dominio así lo determina
- `MotivoCierreActa = RESULTADO_EXTERNO_FIRME`
- cierre y fechas acordes
- actualización del último hito

---

# 15. Transiciones por reproceso

## 15.1 Reproceso parcial
Cuando el sistema recalcula snapshot por una acta específica:

- debe recomponer todos los campos afectados
- no debe mantener residuos semánticos viejos
- debe respetar las mismas reglas canónicas que una reconstrucción completa

## 15.2 Reproceso masivo
Cuando el sistema recalcula múltiples actas:

- debe preservar trazabilidad de versión
- debe actualizar `FechaHoraUltimaActualizacionSnapshot`
- debe respetar consistencia y precedencia vigentes

---

# 16. Regla sobre transiciones no lineales

El snapshot debe tolerar recorridos no lineales del caso.

No todos los expedientes seguirán siempre el mismo orden ideal.

Por eso, las transiciones deben aceptar que un caso pueda:

- volver desde gestión externa
- reabrirse
- recibir nuevas presentaciones
- paralizarse y luego continuar
- cerrar por distintos caminos válidos

La lectura operativa debe adaptarse a la situación vigente, no a una secuencia rígida idealizada.

---

# 17. Regla sobre transiciones inválidas o superadas

Si un hecho:

- fue invalidado
- quedó superado
- perdió vigencia
- fue reemplazado por otro de mayor precedencia

no debe seguir dominando el snapshot.

La mera existencia histórica del hecho no basta para imponer la transición.

---

# 18. Regla de consistencia final

Tras cada transición, el snapshot debe quedar:

- coherente
- explicable
- compatible con el canon
- consistente con el dominio
- apto para queries operativas

Si una transición deja combinación contradictoria de campos, debe corregirse en el mismo cálculo o en reproceso inmediato controlado.

---

# 19. Recomendación de implementación

Conviene implementar las transiciones en capas de reglas:

## 19.1 Transiciones de estado principal
Definen `EstadoActa`, cierre, actividad y paralización.

## 19.2 Transiciones de contexto
Definen notificación, documento y gestión externa.

## 19.3 Transiciones de flags y trazabilidad
Definen pendientes, prioridad, último hito y fechas operativas.

Esto simplifica testing y evolución.

---

# 20. Resumen ejecutivo

Las transiciones del snapshot definen cómo cambia la lectura operativa actual del caso frente a hechos válidos del dominio.

Deben permitir que el snapshot:

- refleje la situación vigente real
- tolere recorridos no lineales
- mantenga consistencia interna
- sea útil para bandejas y queries
- siga siendo regenerable y explicable

Su función no es reemplazar el dominio, sino traducirlo en una proyección operativa clara.
