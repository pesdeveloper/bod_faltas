# [CANONICO] ESTADOS DE ACTA

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el catálogo canónico `EstadoActa`, su semántica, su uso correcto y sus límites, de forma que el sistema pueda expresar de manera consistente la situación operativa y procesal actual de cada acta.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/04-snapshot/`
- `spec/05-queries/`
- `spec/06-contratos/`

---

# 1. Definición general

`EstadoActa` representa la situación operativa/procesal actual consolidada de una acta dentro del sistema.

No describe:

- logs técnicos
- estados de infraestructura
- pasos internos de UI
- microestados efímeros del frontend
- estados de sincronización local del móvil

Su finalidad es expresar, de manera comprensible y estable, dónde se encuentra el caso dentro de su recorrido vigente.

---

# 2. Regla general de diseño

Los estados de acta deben ser:

- pocos
- claros
- mutuamente distinguibles
- útiles para snapshot y bandejas
- semánticamente estables

No deben reemplazar la narrativa procesal.

El historial de lo ocurrido vive en `ActaEvento`.  
`EstadoActa` solo resume la situación actual principal.

---

# 3. Catálogo canónico `EstadoActa`

Los valores canónicos iniciales del estado de acta son:

LABRADA
EN_ENRIQUECIMIENTO
ENRIQUECIDA
PENDIENTE_NOTIFICACION_ACTA
NOTIFICACION_ACTA_EN_PROCESO
NOTIFICADA_ACTA
EN_ANALISIS
PENDIENTE_ACTO_ADMINISTRATIVO
ACTO_ADMINISTRATIVO_EN_PROCESO
PENDIENTE_NOTIFICACION_ACTO
NOTIFICACION_ACTO_EN_PROCESO
NOTIFICADA_RESOLUCION
EN_APELACION
EN_GESTION_EXTERNA
PARALIZADA
ARCHIVADA
ANULADA
FINALIZADA

# 4. Semántica de cada estado

## 4.1 LABRADA
La acta fue creada o registrada y existe formalmente en el sistema, pero todavía no ingresó en trabajo de enriquecimiento administrativo posterior.

Es el estado de entrada natural de D1.

---

## 4.2 EN_ENRIQUECIMIENTO
La acta está siendo completada, revisada o enriquecida administrativamente luego de su labrado inicial.

Implica que todavía no se considera lista para avanzar al circuito posterior.

---

## 4.3 ENRIQUECIDA
La acta ya fue enriquecida y quedó suficientemente consolidada para avanzar a pasos posteriores del trámite.

No implica que ya haya sido notificada.

---

## 4.4 PENDIENTE_NOTIFICACION_ACTA
La acta ya se encuentra en condiciones de ser notificada, pero todavía no se inició la gestión operativa concreta de notificación.

---

## 4.5 NOTIFICACION_ACTA_EN_PROCESO
Existe una gestión de notificación activa asociada a la acta inicial.

Este estado se usa mientras la notificación está operativamente en curso.

---

## 4.6 NOTIFICADA_ACTA
La notificación de la acta alcanzó un resultado suficiente para que el trámite pueda avanzar a la siguiente etapa del proceso, según las reglas vigentes del sistema.

No significa necesariamente que el caso esté cerrado ni resuelto.

---

## 4.7 EN_ANALISIS
La acta se encuentra en etapa de análisis, evaluación, presentaciones, actuaciones intermedias, pagos con impacto procesal u otras intervenciones previas al acto administrativo principal.

---

## 4.8 PENDIENTE_ACTO_ADMINISTRATIVO
El caso ya se encuentra en condiciones de avanzar hacia la generación o formalización del acto administrativo correspondiente, pero dicha gestión aún no comenzó efectivamente.

---

## 4.9 ACTO_ADMINISTRATIVO_EN_PROCESO
Existe una gestión activa de preparación, generación, revisión o firma del acto administrativo.

Este estado cubre el trabajo operativo previo a la consolidación del acto.

---

## 4.10 PENDIENTE_NOTIFICACION_ACTO
El acto administrativo ya existe en forma suficiente como para iniciar su notificación, pero la gestión operativa de notificación todavía no comenzó.

---

## 4.11 NOTIFICACION_ACTO_EN_PROCESO
Existe una notificación activa en curso vinculada al acto administrativo.

---

## 4.12 NOTIFICADA_RESOLUCION
La resolución, fallo o acto principal ya fue notificado en forma suficiente como para considerar cumplida esa etapa del proceso.

No implica automáticamente archivo ni cierre final.

---

## 4.13 EN_APELACION
El caso se encuentra atravesando un tramo de recurso, revisión o apelación con impacto vigente sobre el trámite.

---

## 4.14 EN_GESTION_EXTERNA
El caso fue derivado o se encuentra involucrado en una gestión externa relevante para su recorrido, como por ejemplo apremio, juzgado u otro circuito formal externo.

---

## 4.15 PARALIZADA
El caso fue paralizado por decisión administrativa o jurídica y no debe continuar su curso normal hasta que dicha paralización sea levantada o reemplazada por otra decisión.

No equivale a cierre.

---

## 4.16 ARCHIVADA
El caso fue archivado por decisión administrativa válida.

Implica cierre administrativo del trámite bajo criterio de archivo.

---

## 4.17 ANULADA
La acta o el trámite fueron invalidados de manera tal que no corresponde continuar el proceso normal.

Es un cierre por invalidez, no por resolución favorable ni por cumplimiento.

---

## 4.18 FINALIZADA
El caso concluyó de manera efectiva por una causa distinta del archivo o la anulación, como por ejemplo cumplimiento total, pago confirmado con efecto final, resolución firme consolidada o resultado externo firme.

Es un cierre material/operativo del recorrido.

---

# 5. Reglas de uso

## 5.1 Un solo estado principal vigente
Cada acta debe tener un único `EstadoActa` principal vigente en cada momento.

## 5.2 El estado no reemplaza al historial
El estado actual resume la situación actual, pero no narra el recorrido completo del caso.

## 5.3 El estado debe poder derivarse
La semántica del estado debe ser coherente con los eventos, documentos y notificaciones registrados.

## 5.4 El estado debe ser útil
No deben existir estados que no sirvan para:
- bandejas
- filtros
- snapshot
- lectura operativa

---

# 6. Estados finales y no finales

## 6.1 Estados finales
Se consideran estados finales del recorrido principal:

- `ARCHIVADA`
- `ANULADA`
- `FINALIZADA`

## 6.2 Estados no finales
Todos los demás estados son estados no finales y pueden, en principio, evolucionar hacia otros estados válidos.

---

# 7. Diferencia entre ARCHIVADA, ANULADA y FINALIZADA

## 7.1 ARCHIVADA
Cierre administrativo por archivo.

## 7.2 ANULADA
Cierre por invalidez del acta o del trámite.

## 7.3 FINALIZADA
Cierre efectivo del caso por cumplimiento, resolución firme u otro resultado conclusivo distinto de archivo o anulación.

Estos tres estados no deben fusionarse.

---

# 8. Diferencia entre estado de acta y estado de notificación

`EstadoActa` no reemplaza el estado de una notificación específica.

Por ejemplo:

- una acta puede estar en `NOTIFICACION_ACTA_EN_PROCESO`
- mientras una notificación concreta está en `ENVIADA`, `ACUSE_RECHAZADO` o `REENVIADA`

La acta expresa la situación global del caso.  
La notificación expresa la situación operativa de una gestión puntual.

---

# 9. Diferencia entre estado de acta y estado documental

`EstadoActa` no reemplaza la situación documental.

Por ejemplo:
- el caso puede estar en `ACTO_ADMINISTRATIVO_EN_PROCESO`
- mientras el documento correspondiente está en `BORRADOR`, `GENERADO` o `PENDIENTE_FIRMA`

Cada capa semántica debe conservar su responsabilidad.

---

# 10. Relación con snapshot

`EstadoActa` es uno de los ejes principales del snapshot operativo.

Las bandejas, métricas y filtros deben usarlo como clasificación central, complementado con:

- flags derivados
- estado de notificación
- vencimientos
- prioridad operativa
- datos de contexto

No debe sobrecargarse `EstadoActa` con detalles que pertenecen a flags o campos derivados.

---

# 11. Relación con móvil offline-first

La aplicación móvil puede manejar estados locales de sincronización, por ejemplo:

- pendiente de sincronizar
- sincronizando
- error de sincronización

Esos estados no forman parte de `EstadoActa`.

Son estados técnicos locales del canal móvil y deben modelarse por separado.

---

# 12. Regla de evolución del catálogo

Solo deben agregarse nuevos estados si:

- no pueden resolverse como eventos
- no pueden resolverse como flags del snapshot
- no pueden resolverse como estado de notificación o de documento
- existe una necesidad operativa real y transversal

La incorporación de estados nuevos debe ser excepcional.

---

# 13. Recomendación de implementación

En implementación, `EstadoActa` debe ser tratado como catálogo canónico compartido entre:

- backend
- frontend web
- móvil
- snapshot
- contratos
- procesos de integración

No debe haber traducciones semánticas distintas entre canales.

---

# 14. Resumen ejecutivo

`EstadoActa` expresa la situación actual principal del caso.

Debe ser:
- único por acta
- estable
- útil
- derivable
- consistente con los eventos reales del trámite

La narrativa vive en `ActaEvento`.  
La proyección vive en `ActaSnapshotOperativo`.  
El catálogo `EstadoActa` solo fija la ubicación actual del caso dentro del recorrido canónico.
