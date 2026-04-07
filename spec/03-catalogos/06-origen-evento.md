# [CANONICO] ORIGEN DE EVENTO

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el catálogo canónico `OrigenEvento`, estableciendo la procedencia funcional de un evento procesal dentro del sistema y su uso correcto en dominio, trazabilidad, snapshot, queries y contratos.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/04-snapshot/`
- `spec/05-queries/`
- `spec/06-contratos/`

---

# 1. Definición general

`OrigenEvento` expresa la procedencia funcional inmediata de un evento registrado en `ActaEvento`.

Su finalidad es responder, de manera simple y consistente:

- desde dónde se originó el hecho
- quién o qué tipo de mecanismo lo provocó
- si el evento fue humano, automático, integrado o procesado internamente

`OrigenEvento` no describe el detalle técnico fino de infraestructura, ni reemplaza la trazabilidad operativa completa del sistema.

---

# 2. Regla general de diseño

`OrigenEvento` debe ser:

- corto
- estable
- transversal
- funcionalmente comprensible
- útil para trazabilidad y auditoría operativa

No debe capturar:
- tecnología concreta
- nombre de servicio
- nombre de job
- host o equipo
- detalle de red
- detalle de cola o worker
- detalle de canal UI específico

---

# 3. Catálogo canónico `OrigenEvento`

Los valores canónicos iniciales son:

- `MANUAL`
- `SISTEMA`
- `PROCESO`
- `INTEGRACION`

---

# 4. Semántica de cada origen

## 4.1 MANUAL
El evento fue provocado por una acción humana directa con efecto procesal sobre el caso.

Ejemplos típicos:
- operador que observa un acta
- usuario que registra una comparecencia
- agente que dispone un archivo
- personal administrativo que incorpora una presentación
- inspector que labra un acta

No implica necesariamente que la acción haya ocurrido por web o por móvil; solo indica que el origen funcional fue humano directo.

---

## 4.2 SISTEMA
El evento fue generado automáticamente por el sistema como consecuencia funcional de reglas de negocio, validaciones o automatismos visibles del dominio.

Ejemplos típicos:
- el sistema deriva automáticamente un estado luego de una condición cumplida
- el sistema registra un evento funcional al consolidar un paso sin intervención humana directa en ese instante
- el sistema genera un evento documental o de avance procesal previsto por reglas del dominio

No debe usarse para logs técnicos invisibles al dominio.

---

## 4.3 PROCESO
El evento fue originado por un proceso interno del sistema que opera en segundo plano o mediante ejecución diferida, pero con efecto funcional sobre el caso.

Ejemplos típicos:
- un reproceso funcional
- una consolidación posterior
- una tarea batch que actualiza situaciones válidas del expediente
- un job que incorpora un resultado operativo ya reconocido por el dominio

La diferencia respecto de `SISTEMA` es que aquí el origen inmediato es un proceso interno diferido o programado, y no una reacción instantánea del sistema frente a una operación interactiva.

---

## 4.4 INTEGRACION
El evento fue originado a partir de una interacción con un sistema externo o una fuente externa reconocida por el dominio.

Ejemplos típicos:
- recepción de resultado desde gestión externa
- recepción de confirmación de pago desde sistema económico
- incorporación de una actuación desde una integración formal
- retorno desde juzgado u otro circuito externo

No describe el protocolo técnico; solo expresa que el origen funcional provino del exterior del sistema principal.

---

# 5. Regla de interpretación

`OrigenEvento` no responde “qué pasó”.  
Eso lo responde `TipoEventoActa`.

`OrigenEvento` responde “desde qué clase de origen funcional se produjo el hecho”.

Por ejemplo:
- `ACTA_LABRADA` responde qué pasó
- `MANUAL` responde cómo se originó funcionalmente

Ambas dimensiones deben convivir sin mezclarse.

---

# 6. Regla sobre canal y origen

`OrigenEvento` no debe confundirse con:

- canal de notificación
- canal UI
- web
- móvil
- API
- batch
- endpoint
- servicio técnico

Esos detalles pueden existir en auditoría técnica o metadata, pero no forman parte del catálogo canónico de origen.

---

# 7. Regla sobre manualidad

Si el hecho fue disparado directamente por una persona con intención operativa sobre el expediente, el origen debe ser `MANUAL`, incluso si técnicamente lo hizo desde:

- frontend web
- aplicación móvil
- terminal interna
- puesto administrativo

La herramienta usada no cambia el origen funcional.

---

# 8. Regla sobre automatismos del sistema

Si el evento surge por una regla de negocio que el sistema aplica automáticamente como parte de una interacción normal y visible del dominio, corresponde `SISTEMA`.

Si el evento surge por ejecución diferida, proceso de consolidación o tarea interna posterior, corresponde evaluar `PROCESO`.

---

# 9. Regla sobre integración

Cuando el hecho procesal relevante aparece porque un sistema externo informa, devuelve o confirma una situación con impacto en el caso, el origen correcto es `INTEGRACION`.

No importa si técnicamente fue:
- API
- archivo
- importación
- webhook
- intercambio batch

El catálogo solo expresa la procedencia funcional.

---

# 10. Relación con snapshot y queries

`OrigenEvento` puede servir para:

- auditoría funcional
- filtros de historial
- análisis de trazabilidad
- indicadores de intervención manual vs automática
- diagnóstico operativo

No debe ser el eje principal del snapshot, pero sí puede formar parte de sus campos complementarios o de consultas analíticas.

---

# 11. Relación con frontend y móvil

Frontend web y móvil pueden mostrar labels amigables para el origen, por ejemplo:
- manual
- automático
- integración
- proceso interno

Pero deben basarse siempre en el valor canónico del catálogo.

No deben inventar variantes locales.

---

# 12. Regla de evolución del catálogo

Solo deben agregarse nuevos valores a `OrigenEvento` si:

- expresan una procedencia funcional genuinamente distinta
- no pueden resolverse con metadata adicional
- su uso sería transversal
- mejoran la comprensión del sistema
- no fragmentan innecesariamente el vocabulario

La proliferación de orígenes debe evitarse.

---

# 13. Recomendación de implementación

En implementación, `OrigenEvento` debe ser compartido entre:

- backend
- frontend
- móvil
- snapshot
- contratos
- timeline o historial del expediente

No deben existir traducciones semánticas no documentadas.

---

# 14. Resumen ejecutivo

`OrigenEvento` clasifica la procedencia funcional de un evento procesal.

Debe ser:
- simple
- estable
- transversal
- comprensible
- útil para trazabilidad

No reemplaza:
- el tipo de evento
- la auditoría técnica
- la metadata operativa
- el canal ni la tecnología concreta

Su función es dar contexto funcional canónico a los eventos del sistema.
