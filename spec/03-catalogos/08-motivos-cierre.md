# [CANONICO] MOTIVOS DE CIERRE DE ACTA

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el catálogo canónico `MotivoCierreActa`, estableciendo las causas principales por las cuales un caso puede considerarse cerrado, finalizado, archivado o concluido operativamente dentro del sistema.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/07-integracion-externa.md`
- `spec/04-snapshot/`
- `spec/05-queries/`
- `spec/06-contratos/`

---

# 1. Definición general

`MotivoCierreActa` expresa la causa principal por la cual un caso deja de estar abierto dentro del recorrido operativo normal del sistema.

Su finalidad es responder, de manera canónica y resumida:

- por qué se cerró el caso
- por qué dejó de seguir avanzando
- cuál fue la razón principal de conclusión administrativa, material o jurídica

No reemplaza:

- el `EstadoActa`
- el historial de eventos
- el contenido documental
- el detalle jurídico completo del expediente

Es una clasificación de cierre, no una reconstrucción completa del caso.

---

# 2. Regla general de diseño

Los motivos de cierre deben ser:

- pocos
- claros
- mutuamente distinguibles
- útiles para snapshot, queries, reportes y trazabilidad
- compatibles con los estados finales del caso

No deben capturar micro-causas excesivamente específicas si la necesidad puede resolverse con:

- eventos
- observaciones
- documentos
- metadata
- detalle de integración externa

---

# 3. Catálogo canónico `MotivoCierreActa`

Los valores canónicos iniciales son:

- `ARCHIVO_ADMINISTRATIVO`
- `ANULACION`
- `PAGO_CONFIRMADO`
- `CUMPLIMIENTO_TOTAL`
- `RESOLUCION_FIRME`
- `RESULTADO_EXTERNO_FIRME`
- `DESISTIMIENTO_ACEPTADO`
- `OTRO`

---

# 4. Semántica de cada motivo de cierre

## 4.1 ARCHIVO_ADMINISTRATIVO
El caso fue cerrado por una decisión administrativa de archivo.

Debe utilizarse cuando el cierre responda principalmente a una decisión de archivar el trámite, más allá de los fundamentos concretos que puedan constar en el expediente.

Este motivo se vincula naturalmente con estado final `ARCHIVADA`.

---

## 4.2 ANULACION
El caso fue cerrado porque el acta o el trámite resultó anulado, invalidado o dejado sin efecto de manera tal que no corresponde continuar su recorrido normal.

Este motivo se vincula naturalmente con estado final `ANULADA`.

---

## 4.3 PAGO_CONFIRMADO
El caso fue cerrado principalmente porque el pago quedó confirmado con efecto suficiente para concluir operativamente el trámite.

Debe utilizarse cuando el hecho decisivo de cierre sea la confirmación del pago, sin necesidad de modelar aquí el detalle económico fino.

Normalmente se vincula con estado final `FINALIZADA`.

---

## 4.4 CUMPLIMIENTO_TOTAL
El caso fue cerrado porque se verificó un cumplimiento total suficiente de las exigencias o condiciones relevantes del trámite.

Este motivo es más amplio que `PAGO_CONFIRMADO`, ya que puede cubrir cierres por cumplimiento material completo aunque no se agoten en el pago.

Normalmente se vincula con estado final `FINALIZADA`.

---

## 4.5 RESOLUCION_FIRME
El caso fue cerrado porque se consolidó una resolución, fallo o decisión firme que agotó el recorrido principal del trámite y dejó una situación conclusiva suficiente.

Debe utilizarse cuando la razón principal de cierre no sea pago ni archivo, sino la firmeza del acto resolutivo.

Normalmente se vincula con estado final `FINALIZADA`.

---

## 4.6 RESULTADO_EXTERNO_FIRME
El caso fue cerrado porque una gestión o instancia externa devolvió un resultado firme, concluyente o suficientemente consolidado como para dar por terminado el recorrido principal dentro del sistema.

Debe utilizarse cuando el cierre se explica principalmente por el desenlace externo y no por una decisión autónoma puramente interna.

Normalmente se vincula con estado final `FINALIZADA`, aunque según el caso también puede acompañar otros cierres derivados.

---

## 4.7 DESISTIMIENTO_ACEPTADO
El caso fue cerrado porque un desistimiento fue aceptado con efecto suficiente para concluir el trámite.

Debe utilizarse cuando el desistimiento no sea solo un antecedente documental, sino la razón principal y reconocida del cierre del caso.

Usualmente se vincula con estado final `FINALIZADA` o eventualmente `ARCHIVADA`, según la lógica que adopte el proceso.

---

## 4.8 OTRO
Motivo residual y excepcional.

Debe utilizarse solo cuando:
- el caso efectivamente se cerró
- existe una causa principal identificable
- ninguna de las categorías anteriores resulta razonablemente adecuada

Su uso debe revisarse periódicamente para detectar si corresponde incorporar un motivo nuevo al canon.

---

# 5. Relación entre motivo de cierre y estado de acta

`MotivoCierreActa` no reemplaza al `EstadoActa`.

El estado expresa la situación final o vigente del caso, por ejemplo:
- `ARCHIVADA`
- `ANULADA`
- `FINALIZADA`

El motivo expresa por qué se llegó a ese cierre.

Por ejemplo:
- estado: `FINALIZADA`
- motivo: `PAGO_CONFIRMADO`

Ambos conceptos deben coexistir y no confundirse.

---

# 6. Relación entre motivo de cierre y evento

El motivo de cierre no reemplaza el evento que produjo o consolidó el cierre.

Ejemplos:
- `ARCHIVO_DISPUESTO`
- `ACTA_ANULADA`
- `CUMPLIMIENTO_TOTAL_REGISTRADO`
- `RESULTADO_EXTERNO_REGISTRADO`

Los eventos narran lo ocurrido.  
El motivo resume la causa principal del cierre.

---

# 7. Relación entre motivo de cierre y documento

El motivo de cierre tampoco reemplaza la base documental del expediente.

Puede existir:
- resolución
- fallo
- constancia
- actuación
- documento externo
- presentación aceptada

que fundamente el cierre, pero el motivo sigue siendo una clasificación resumida y transversal.

---

# 8. Relación con snapshot

`MotivoCierreActa` es un dato muy valioso para el snapshot cuando el caso ya alcanzó estado final o casi final.

Puede servir para:

- filtros de casos cerrados
- reportes de cierre
- estadísticas
- vistas históricas
- análisis de productividad y desenlace
- trazabilidad operativa

No debería usarse como campo obligatorio de un caso aún abierto, salvo en estados transicionales muy controlados.

---

# 9. Relación con queries y reportes

Este catálogo debe permitir consultas como:

- cuántos casos se archivaron
- cuántos cerraron por pago
- cuántos cerraron por anulación
- cuántos cerraron por resultado externo
- cuántos cerraron por resolución firme

Por eso debe mantenerse simple y consistente.

---

# 10. Regla sobre uso de OTRO

`OTRO` debe ser estrictamente excepcional.

No debe transformarse en:
- categoría cómoda
- categoría genérica por omisión
- lugar donde termina todo lo mal clasificado

Todo uso de `OTRO` debería ser auditable y revisable.

---

# 11. Relación con frontend y móvil

Frontend y móvil pueden mostrar labels más amigables para el motivo de cierre, pero deben basarse en este catálogo canónico.

Puede cambiar:
- label visible
- color
- agrupación de UI
- descripción contextual

No debe cambiar:
- valor canónico
- semántica
- contrato compartido

---

# 12. Regla de evolución del catálogo

Solo deben agregarse nuevos motivos de cierre si:

- representan una causa principal genuinamente distinta
- no pueden resolverse con eventos + metadata
- tienen uso transversal
- aportan claridad real a snapshot, reportes y contratos
- no fragmentan innecesariamente la clasificación final

La proliferación de motivos debe evitarse.

---

# 13. Recomendación de implementación

`MotivoCierreActa` debe compartirse entre:

- backend
- snapshot
- queries
- reportes
- frontend
- contratos

Su uso debe estar controlado por reglas del dominio y no quedar librado a interpretaciones locales inconsistentes.

---

# 14. Resumen ejecutivo

`MotivoCierreActa` clasifica la razón principal por la cual un caso se cerró.

Debe ser:
- claro
- estable
- útil para reporting y snapshot
- compatible con estados finales
- consistente con la narrativa del expediente

No reemplaza:
- el estado de acta
- los eventos
- los documentos
- la fundamentación completa del caso

Su función es aportar una semántica final resumida y transversal sobre la causa principal del cierre.