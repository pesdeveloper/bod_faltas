# [CANONICO] REGLAS DE DERIVACIÓN DEL SNAPSHOT

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir las reglas canónicas mediante las cuales `ActaSnapshotOperativo` se deriva a partir de la información fuente del dominio, estableciendo criterios de precedencia, consistencia y actualización para sus campos principales y flags operativos.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/03-catalogos/06-origen-evento.md`
- `spec/03-catalogos/07-integracion-externa.md`
- `spec/03-catalogos/08-motivos-cierre.md`
- `spec/03-catalogos/09-firma-tipo.md`
- `spec/04-snapshot/00-snapshot-engine.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/05-queries/`
- `spec/06-contratos/04-contratos-snapshot.md`

---

# 1. Definición general

Las reglas de derivación son el conjunto de criterios explícitos mediante los cuales el sistema transforma hechos del dominio en una proyección operativa coherente y útil.

Estas reglas deben permitir que el snapshot:

- sea consistente
- sea regenerable
- sea explicable
- sea estable
- no dependa de interpretación informal

Toda derivación del snapshot debe poder justificarse contra estas reglas o sus ampliaciones posteriores.

---

# 2. Regla madre de derivación

El snapshot se deriva siempre desde la fuente de verdad del dominio.

La precedencia general es:

1. `Acta`
2. `ActaEvento`
3. `Documento`
4. `Notificacion`
5. relaciones y metadata válidas del dominio

Si existiera contradicción entre snapshot y fuente de verdad, prevalece siempre la fuente de verdad.

---

# 3. Regla de precedencia por tipo de información

Cada campo del snapshot debe derivarse desde la fuente más adecuada según su naturaleza.

## 3.1 Estado global del caso
Se deriva prioritariamente desde:
- estado vigente de `Acta`
- eventos procesales válidos
- reglas de cierre o transición

## 3.2 Hitos narrativos
Se derivan prioritariamente desde:
- `ActaEvento`

## 3.3 Situación documental
Se deriva prioritariamente desde:
- `Documento`
- `DocumentoFirma`
- relaciones documentales válidas

## 3.4 Situación de notificación
Se deriva prioritariamente desde:
- `Notificacion`
- eventos de notificación válidos

## 3.5 Gestión externa
Se deriva prioritariamente desde:
- eventos externos
- metadata de integración válida
- documentos externos si aplican

---

# 4. Regla de recálculo

El snapshot debe recalcularse siempre que un hecho del dominio pueda afectar alguno de sus campos relevantes.

Como mínimo, debe dispararse recálculo cuando ocurra:

- alta o modificación relevante de `Acta`
- alta de `ActaEvento`
- incorporación o cambio relevante de `Documento`
- cambio relevante en `Notificacion`
- reproceso manual o batch autorizado
- corrección de consistencia

No hace falta recalcular campos que no puedan verse afectados, pero la semántica final debe ser equivalente a una reconstrucción completa.

---

# 5. Regla de derivación del EstadoActa

`EstadoActa` del snapshot debe derivarse a partir del estado vigente del caso y de sus hechos dominiales válidos.

Como criterio general:

- si existe un estado actual persistido y válido en `Acta`, ese valor es la base principal
- si el modelo usa eventos como disparadores de transición, el snapshot debe reflejar el resultado vigente consolidado
- si hay un evento de cierre posterior, el estado final debe prevalecer sobre estados previos

## 5.1 Prioridad de estados finales
Si el caso alcanzó una condición final válida, deben prevalecer estos estados finales sobre cualquier estado operativo previo:

- `ARCHIVADA`
- `ANULADA`
- `FINALIZADA`

## 5.2 Prioridad de paralización
Si existe paralización vigente, el snapshot debe reflejar `PARALIZADA`, salvo que exista un cierre final posterior válido que la supere.

---

# 6. Regla de derivación de cierre

## 6.1 EstaCerrada
Debe derivarse como `true` cuando el `EstadoActa` vigente sea uno de los estados finales del caso.

## 6.2 MotivoCierreActa
Debe derivarse únicamente cuando el caso tenga cierre válido o suficientemente consolidado.

La derivación debe basarse en:
- evento de cierre relevante
- estado final alcanzado
- documento resolutivo, si aplica
- resultado externo, si aplica

## 6.3 TipoEventoCierre
Debe derivarse a partir del evento que consolidó el cierre principal del caso.

## 6.4 FechaHoraCierre
Debe derivarse a partir del momento del evento o hecho dominante que marcó el cierre efectivo.

---

# 7. Regla de derivación del último hito relevante

El snapshot debe conservar un último hito relevante resumido.

Este hito se deriva a partir del último evento procesal válido con impacto operativo.

## 7.1 Criterios del último hito
El último hito debe priorizar:
- eventos procesales relevantes
- eventos con impacto de estado
- eventos de cierre
- eventos de notificación significativos
- eventos de gestión externa relevantes

## 7.2 No deben considerarse
No deben computarse como último hito:
- logs técnicos
- reintentos de infraestructura
- actividad local del móvil sin impacto dominial
- cambios irrelevantes de soporte

---

# 8. Regla de derivación de notificación

## 8.1 TieneNotificacionActiva
Debe ser `true` cuando exista una notificación vigente cuya situación requiera seguimiento o cuyo ciclo no esté cerrado operativamente.

## 8.2 UltimoEstadoNotificacion
Debe derivarse de la notificación relevante más reciente con impacto operativo actual.

## 8.3 UltimoCanalNotificacion
Debe derivarse del canal de la última notificación relevante vigente.

## 8.4 RequiereReenvioNotificacion
Debe ser `true` cuando exista una notificación cuyo estado o secuencia evidencie necesidad operativa de reenvío.

Ejemplos típicos:
- último acuse rechazado
- secuencia incompleta con política de reenvío
- situación equivalente definida por reglas del proceso

## 8.5 TieneAcuseRecibidoVigente
Debe ser `true` cuando exista acuse válido y útil para la etapa notificatoria actual.

No basta con cualquier acuse histórico; debe ser relevante para la situación actual.

---

# 9. Regla de derivación de etapa notificatoria

`EtapaNotificatoriaActual` debe derivarse a partir del contexto dominante de notificación.

Como criterio general:
- si el caso está transitando notificación del acta, debe reflejar esa etapa
- si el caso está transitando notificación del acto administrativo, debe reflejar esa etapa
- si no hay notificación activa, puede ser nulo o reflejar la última etapa notificatoria cerrada, según lo que defina el contrato de lectura

La semántica debe ser clara y no ambigua.

---

# 10. Regla de derivación documental

## 10.1 UltimoTipoDocumentoRelevante
Debe derivarse del documento más reciente con relevancia operativa real para el caso.

No debe elegirse solo por fecha si existe un documento más significativo funcionalmente.

## 10.2 UltimoEstadoDocumentoRelevante
Debe derivarse del estado del documento relevante dominante.

## 10.3 UltimoFirmaTipoRelevante
Debe derivarse de la modalidad de firma del documento relevante dominante, si aplica.

## 10.4 TieneDocumentoPendienteFirma
Debe ser `true` cuando exista al menos un documento relevante cuyo estado actual sea `PENDIENTE_FIRMA`.

## 10.5 TieneDocumentoObservado
Debe ser `true` cuando exista un documento observado cuya observación siga teniendo relevancia operativa vigente.

## 10.6 TieneDocumentoInvalidadoVigente
Solo debe ser `true` cuando la invalidación documental siga teniendo valor contextual u operativo actual.

No debe marcarse solo porque exista algún documento invalidado histórico ya irrelevante.

---

# 11. Regla de derivación de gestión externa

## 11.1 TieneGestionExternaActiva
Debe ser `true` cuando el caso esté actualmente bajo una gestión externa no concluida o aún relevante operativamente.

## 11.2 TipoGestionExternaActual
Debe derivarse de la gestión externa vigente dominante.

## 11.3 ResultadoExternoActual
Debe derivarse del resultado externo más reciente y vigente con impacto sobre el caso.

## 11.4 RequiereSeguimientoExterno
Debe ser `true` cuando la situación externa exija intervención, control o espera activa.

## 11.5 UltimoMovimientoExternoFechaHora
Debe derivarse del último evento o actuación externa válida reconocida por el dominio.

---

# 12. Regla de derivación de flags de actividad

## 12.1 EstaActiva
Debe ser `true` mientras el caso no se encuentre en un estado final y siga requiriendo pertenencia al circuito operativo principal o secundario.

## 12.2 RequiereAccionOperativa
Debe ser `true` cuando del snapshot resulte que existe un pendiente, bloqueo, revisión o paso humano necesario.

Este flag no debe activarse por mera existencia histórica de eventos pasados.

## 12.3 RequiereRevisionAdministrativa
Debe ser `true` cuando el caso se encuentre en análisis, observación, subsanación, tratamiento o revisión administrativa equivalente.

## 12.4 TienePresentacionPendiente
Debe ser `true` cuando exista presentación o intervención incorporada aún no absorbida operativamente por el flujo.

## 12.5 TieneApelacionActiva
Debe ser `true` mientras exista apelación o revisión vigente con impacto actual.

## 12.6 TieneMedidaPreventivaVigente
Debe ser `true` cuando exista medida preventiva vigente según la lógica del caso.

## 12.7 EstaParalizada
Debe derivarse de manera directa y consistente con el estado vigente o condición equivalente.

---

# 13. Regla de derivación temporal

## 13.1 FechaHoraCreacionActa
Debe derivarse de la fecha/hora real de creación o labrado del caso.

## 13.2 FechaHoraUltimaActualizacionDominio
Debe reflejar la fecha/hora del último hecho del dominio que cambió efectivamente la lectura del caso.

## 13.3 FechaHoraUltimaActualizacionSnapshot
Debe reflejar cuándo se recalculó o persistió la proyección.

## 13.4 FechaHoraUltimoPendienteOperativo
Debe reflejar la marca temporal del último pendiente que todavía requiere atención, si ese concepto se usa.

No debe confundirse con el último evento histórico sin pendiente actual.

---

# 14. Regla de derivación de consistencia interna

Los campos del snapshot deben ser coherentes entre sí.

Ejemplos mínimos de coherencia:

- si `EstaCerrada = true`, `EstadoActa` debe ser final
- si `EstaParalizada = true`, el estado debe ser compatible o la regla debe justificar la coexistencia
- si `TieneGestionExternaActiva = true`, no debería haber cierre externo firme incompatible
- si `TieneDocumentoPendienteFirma = true`, debe existir respaldo documental válido
- si `RequiereReenvioNotificacion = true`, debe existir contexto notificatorio compatible

Toda inconsistencia debe poder reprocesarse.

---

# 15. Regla de prevalencia entre hechos contradictorios

Si existen hechos históricos que parecen contradictorios, el snapshot debe derivarse según:

1. validez del hecho
2. vigencia actual
3. precedencia temporal
4. precedencia semántica del proceso
5. reglas explícitas del dominio

No debe ganar automáticamente el último registro si semánticamente fue superado o invalidado.

---

# 16. Regla de vigencia

No todo hecho histórico sigue siendo vigente para snapshot.

Una condición solo debe impactar el snapshot si:
- sigue vigente
- sigue siendo operativamente relevante
- no fue superada por un hecho posterior válido
- no fue invalidada por el dominio

Esta regla es clave para evitar snapshots contaminados con historia vieja.

---

# 17. Regla de minimalismo derivativo

El snapshot no debe derivar campos innecesarios solo porque la información esté disponible.

Si un campo no se usa en:
- bandejas
- filtros
- prioridades
- visualización frecuente
- indicadores

no debería derivarse por defecto.

---

# 18. Regla de regeneración completa

Debe existir siempre la posibilidad de reconstruir el snapshot completo de una acta desde cero, utilizando:

- estado base de `Acta`
- eventos válidos
- documentos válidos
- notificaciones válidas
- reglas canónicas vigentes

Esto es obligatorio para:
- migraciones
- auditorías
- corrección de errores
- cambios de reglas

---

# 19. Regla de versionado de reglas

Cuando cambien las reglas de derivación, debe poder identificarse la versión aplicada al snapshot recalculado.

Esto es importante para:
- auditoría
- debugging
- reprocesos controlados
- migraciones de semántica operativa

---

# 20. Regla de trazabilidad explicable

Toda derivación importante del snapshot debería poder explicarse al menos con:

- el hecho dominante que la originó
- la fecha del hecho
- la regla aplicada
- la fuente de verdad utilizada

No hace falta que el snapshot contenga toda esta explicación, pero el sistema debe poder reconstruirla.

---

# 21. Recomendación de implementación

En implementación, conviene expresar estas reglas en tres niveles:

## 21.1 Reglas base
Las que derivan estado, cierre, hito y vigencia general.

## 21.2 Reglas complementarias
Las que derivan flags, contexto documental, notificación y gestión externa.

## 21.3 Reglas de consistencia y reproceso
Las que validan, corrigen y reconstruyen el snapshot.

Esto mejora mantenibilidad y claridad.

---

# 22. Resumen ejecutivo

Las reglas de derivación del snapshot definen cómo convertir hechos del dominio en lectura operativa actual.

Deben garantizar que el snapshot sea:

- consistente
- regenerable
- útil
- explicable
- estable

El snapshot no debe depender de intuición ni de heurística opaca.

Debe responder siempre a reglas canónicas explícitas y revisables.