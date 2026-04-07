# [CANONICO] SNAPSHOT ENGINE

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el concepto, responsabilidad, límites y reglas generales del `Snapshot Engine`, que deriva y mantiene la proyección operativa actual de cada acta a partir de los hechos válidos del dominio.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/03-catalogos/08-motivos-cierre.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/05-queries/`
- `spec/06-contratos/04-contratos-snapshot.md`

---

# 1. Definición general

El `Snapshot Engine` es el componente responsable de construir y mantener una proyección operativa actual de cada acta.

Esa proyección se denomina `ActaSnapshotOperativo`.

Su función es transformar la información dispersa en el dominio en una vista rápida, utilizable y consistente para operación diaria.

El snapshot debe permitir responder con rapidez preguntas como:

- en qué estado operativo está la acta
- qué acción o seguimiento requiere
- si tiene pendientes de notificación
- si tiene documentos pendientes
- si está cerrada o activa
- si está en gestión externa
- cuál fue su último hito relevante
- qué bandeja la debe mostrar

---

# 2. Fuente de verdad y naturaleza del snapshot

El snapshot no es fuente de verdad.

La fuente de verdad del sistema sigue estando en:

- `Acta`
- `ActaEvento`
- `Documento`
- `Notificacion`
- relaciones y metadata válidas del dominio

El snapshot es una proyección derivada.

Esto implica que:

- puede regenerarse
- no debe introducir semántica nueva
- no debe reemplazar al historial
- no debe usarse para justificar inconsistencias del dominio
- debe derivarse de reglas explícitas y revisables

---

# 3. Finalidad del snapshot

El snapshot existe para resolver necesidades operativas y de lectura rápida, especialmente:

- bandejas
- filtros
- búsquedas
- priorización
- indicadores
- listados masivos
- consultas de uso frecuente
- visualización de estado actual

Sin snapshot, muchas de esas lecturas exigirían reconstrucción costosa del caso en tiempo real a partir del historial completo.

---

# 4. Regla rectora del snapshot

La regla central es:

**el dominio produce hechos; el snapshot produce lectura operativa**

Eso significa:

- el dominio registra lo que ocurrió
- el snapshot resume dónde quedó la acta
- el snapshot no inventa hechos
- el snapshot no sustituye el dominio
- el snapshot no redefine la semántica del sistema

---

# 5. Qué debe contener el snapshot

El snapshot debe contener solamente información operativa de alto valor y lectura frecuente.

En particular debe poder resumir:

- estado actual principal del caso
- motivo de cierre, si aplica
- situación de notificación relevante
- situación documental relevante
- hitos recientes
- flags operativos
- información de clasificación y seguimiento
- referencias necesarias para bandejas y consultas

No debe convertirse en una copia completa del dominio.

---

# 6. Qué NO debe hacer el snapshot

El snapshot no debe:

- reemplazar a `ActaEvento`
- almacenar la narrativa completa del caso
- convertirse en fuente jurídica del expediente
- guardar cada detalle documental histórico
- resolver reglas ambiguas por intuición
- contener semántica que no exista en el canon
- convertirse en una segunda base de verdad autónoma

Tampoco debe utilizarse como excusa para evitar modelado correcto en el dominio principal.

---

# 7. Momento de actualización del snapshot

Como principio general, el snapshot debe actualizarse cuando ocurre un hecho del dominio con impacto operativo relevante.

En particular, debe considerarse actualización al menos cuando ocurra alguna de estas situaciones:

- se registra un evento procesal relevante
- se incorpora o cambia un documento relevante
- cambia una notificación relevante
- cambia el estado principal del caso
- cambia una situación de cierre
- cambia una relación externa significativa

La actualización debe ser lo más cercana posible al hecho que la provoca, sin perder consistencia.

---

# 8. Estrategias de actualización admitidas

El sistema puede implementar el snapshot mediante una o varias de estas estrategias, siempre respetando el canon:

## 8.1 Actualización inmediata
Se recalcula el snapshot al momento de persistir el hecho relevante.

Es la estrategia preferida cuando el impacto operativo debe reflejarse rápidamente.

## 8.2 Actualización diferida controlada
Se recalcula mediante proceso posterior controlado cuando por razones técnicas u operativas convenga desacoplar la proyección del write path principal.

## 8.3 Reproceso completo o parcial
Se reconstruye el snapshot desde el historial para una acta o conjunto de actas cuando sea necesario:

- reparar inconsistencias
- migrar reglas
- recalcular proyecciones
- auditar resultados

Estas estrategias pueden coexistir, pero la semántica del snapshot debe ser siempre la misma.

---

# 9. Regla de regenerabilidad

Todo snapshot debe poder regenerarse desde la información fuente válida.

Esto implica que:

- no debe depender de datos invisibles o no persistidos
- no debe depender de estados efímeros del frontend
- no debe depender exclusivamente de memoria de proceso
- sus reglas de derivación deben estar documentadas
- debe existir una forma controlada de reconstrucción

La regenerabilidad es una condición obligatoria del diseño.

---

# 10. Relación entre snapshot y EstadoActa

`EstadoActa` es uno de los ejes principales del snapshot.

Sin embargo, el snapshot no se agota en el estado.

El snapshot debe complementar el estado con información como:

- flags de atención
- pendientes
- contexto documental
- contexto de notificación
- situación externa
- motivo de cierre
- último hito relevante

El estado ordena la lectura principal.  
El snapshot agrega contexto operativo.

---

# 11. Relación entre snapshot y eventos

Los eventos son una fuente principal para derivar el snapshot.

Pero el snapshot no debe copiar el historial completo.

Debe seleccionar y resumir, por ejemplo:

- último evento relevante
- fecha de último hito
- existencia de eventos de cierre
- existencia de eventos de apelación
- existencia de eventos de gestión externa
- existencia de eventos pendientes de seguimiento

Los eventos narran.  
El snapshot resume.

---

# 12. Relación entre snapshot y documentos

Los documentos pueden afectar el snapshot cuando tengan relevancia operativa, por ejemplo:

- documento pendiente de firma
- último acto administrativo firmado
- existencia de documento observado
- existencia de documento invalidado
- existencia de documento formal pendiente de tratamiento

El snapshot no debe ser un repositorio documental paralelo.  
Solo debe reflejar la información documental útil para operación.

---

# 13. Relación entre snapshot y notificación

La notificación es una fuente crítica del snapshot.

El snapshot debe poder reflejar, cuando corresponda:

- si existe notificación activa
- estado de la última notificación relevante
- último canal utilizado
- si hubo rechazo de acuse
- si se requiere reenvío
- si la etapa notificatoria quedó cumplida

No debe replicar la entidad `Notificacion`, pero sí proyectar lo necesario para lectura operativa.

---

# 14. Relación entre snapshot y cierres

Cuando una acta llega a estado final, el snapshot debe reflejar de manera clara:

- que el caso está cerrado
- qué estado final tiene
- cuál fue el motivo principal de cierre
- cuál fue el último hito relevante
- si hubo resolución firme, pago, archivo, anulación o resultado externo

Esto es fundamental para reportes, búsquedas y análisis posterior.

---

# 15. Relación entre snapshot y gestión externa

El snapshot debe poder expresar la situación externa resumida del caso, por ejemplo:

- si está en gestión externa
- tipo principal de gestión externa
- si existe resultado externo
- resultado externo principal
- fecha del último movimiento externo relevante

Esto permite construir bandejas y filtros útiles sin releer toda la historia externa del expediente.

---

# 16. Relación entre snapshot y móvil

El snapshot operativo del dominio no debe mezclarse con estados técnicos locales del móvil.

Por ejemplo, el snapshot no debe incluir como semántica canónica cosas como:

- pendiente de sincronizar
- error de sync
- sincronizando
- borrador local no enviado

Esos conceptos pertenecen al canal móvil y a su operación offline-first, no al snapshot canónico del expediente.

---

# 17. Regla sobre flags operativos

Muchos aspectos del snapshot deben modelarse como flags o campos derivados, no como nuevos estados globales.

Ejemplos de esto pueden ser:

- requiere reenvío
- tiene documento pendiente de firma
- tiene presentación pendiente de revisión
- tiene gestión externa activa
- tiene cierre firme
- tiene última notificación rechazada

Esto evita inflar `EstadoActa` con demasiadas variantes y mantiene claridad semántica.

---

# 18. Regla de consistencia

El snapshot debe ser consistente con el dominio.

Si existe contradicción entre snapshot y fuente de verdad, debe prevalecer siempre la fuente de verdad.

Esto implica que el sistema debe prever:

- detección de inconsistencias
- recalculo o reproceso
- no basar decisiones críticas únicamente en snapshot si hay sospecha de divergencia

---

# 19. Regla de trazabilidad

Aunque el snapshot sea una proyección resumida, debe conservar suficiente trazabilidad para explicar de dónde sale.

Por eso conviene que pueda vincularse, directa o indirectamente, con datos como:

- último evento relevante
- fecha de último cambio
- documento principal vigente
- última notificación relevante
- causa principal de cierre

No para reconstruir todo desde el snapshot, sino para hacerlo interpretable.

---

# 20. Regla de minimalismo

El snapshot debe contener lo suficiente, no todo lo posible.

Cada campo del snapshot debe justificarse con al menos uno de estos motivos:

- se usa en bandejas
- se usa en filtros
- se usa en búsqueda
- se usa en indicadores
- se usa en prioridad operativa
- se usa en visualización frecuente del caso

Si un dato no aporta valor operativo real, no debería vivir en el snapshot.

---

# 21. Relación con queries y bandejas

El snapshot es la base principal sobre la cual deben construirse la mayoría de:

- bandejas operativas
- vistas resumidas
- filtros por estado
- filtros por pendientes
- búsquedas administrativas
- indicadores de gestión

Esto no impide que algunas queries especiales consulten tablas fuente, pero la operación diaria debe apoyarse principalmente en snapshot.

---

# 22. Relación con contratos

El snapshot debe exponer una semántica estable para ser consumido por:

- frontend web
- reportes
- módulos internos
- servicios de consulta
- posibles integraciones de lectura

Por eso, sus campos y reglas deben quedar definidos en documentos específicos posteriores.

---

# 23. Recomendación de implementación

En implementación, el snapshot debe tratarse como:

- proyección persistida
- regenerable
- actualizable por reglas controladas
- desacoplada semánticamente del dominio, pero dependiente de él
- optimizada para lectura operativa

No debe convertirse en una estructura improvisada o mutable sin reglas.

---

# 24. Resumen ejecutivo

El `Snapshot Engine` es el componente que construye y mantiene la proyección operativa actual del caso.

Su misión es:

- resumir el estado real del expediente
- facilitar operación diaria
- alimentar bandejas y queries
- evitar lecturas costosas del historial completo

El snapshot:

- no es fuente de verdad
- no reemplaza eventos ni documentos
- debe ser regenerable
- debe seguir reglas explícitas
- debe ser mínimo pero útil

Su valor principal está en transformar la complejidad del dominio en lectura operativa clara y consistente.
