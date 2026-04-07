# [CANONICO] ESTADOS DE DOCUMENTO

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el catálogo canónico `EstadoDocumento`, estableciendo la situación operativa y formal de un documento dentro del sistema, su semántica, sus límites y sus reglas de uso.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/06-contratos/03-contratos-documentales.md`
- `spec/08-ddl-logico/02-documentos-y-firmas.md`

---

# 1. Definición general

`EstadoDocumento` representa la situación operativa/formal actual de un documento dentro de su ciclo de vida documental.

No expresa:

- el estado global de la acta
- el estado de notificación
- el tipo documental
- el efecto procesal completo del caso
- el estado técnico del storage

Su finalidad es indicar en qué situación documental concreta se encuentra la pieza.

---

# 2. Regla general de diseño

Los estados documentales deben ser:

- pocos
- claros
- reutilizables
- consistentes entre tipos documentales
- útiles para operaciones, queries y snapshot

No deben capturar cada micro-paso técnico del backend o del frontend.

---

# 3. Catálogo canónico `EstadoDocumento`

Los valores canónicos iniciales son los siguientes:

BORRADOR
GENERADO
PENDIENTE_FIRMA
FIRMADO
INCORPORADO
OBSERVADO
INVALIDADO
REEMPLAZADO

# 4. Semántica de cada estado documental

## 4.1 BORRADOR
Documento en elaboración o preparación, todavía no consolidado como pieza formal válida del trámite.

Puede existir para edición, revisión interna o generación previa.

No implica valor documental definitivo.

---

## 4.2 GENERADO
Documento ya generado materialmente por el sistema o incorporado en forma suficientemente estable como pieza lista para continuar su circuito documental.

Todavía puede no estar firmado ni formalizado.

---

## 4.3 PENDIENTE_FIRMA
Documento que ya alcanzó un grado de preparación suficiente y está esperando la firma o formalización correspondiente.

Este estado es especialmente relevante para resoluciones, fallos, constancias u otras piezas que requieren cierre formal.

---

## 4.4 FIRMADO
Documento que ya cuenta con la firma o formalización exigida por su circuito.

No implica necesariamente que ya haya sido notificado, incorporado definitivamente al expediente lógico o producido todo su efecto procesal.

---

## 4.5 INCORPORADO
Documento que ya forma parte válida e integrada del expediente o caso dentro del sistema.

Este estado expresa pertenencia efectiva al trámite como pieza vigente.

Puede haber documentos firmados que todavía no hayan sido tratados como incorporados formalmente, según el proceso.

---

## 4.6 OBSERVADO
Documento que presenta una observación formal, administrativa o material que impide considerarlo plenamente apto en su situación actual.

No equivale necesariamente a invalidación definitiva.

Puede admitir corrección, reemplazo o subsanación.

---

## 4.7 INVALIDADO
Documento que fue dejado sin efecto, perdió validez o no debe seguir considerándose documento vigente del trámite.

Su existencia histórica puede conservarse, pero ya no integra el circuito activo como pieza válida.

---

## 4.8 REEMPLAZADO
Documento que fue sustituido por otro documento válido que ocupa su lugar funcional dentro del trámite.

Se conserva por trazabilidad, pero dejó de ser la versión activa.

---

# 5. Relación entre estado documental y tipo documental

`EstadoDocumento` no reemplaza a `TipoDocumento`.

Por ejemplo:
- un documento puede ser `RESOLUCION`
- y al mismo tiempo estar en `BORRADOR`, `PENDIENTE_FIRMA`, `FIRMADO` o `INVALIDADO`

Ambas dimensiones deben mantenerse separadas.

---

# 6. Relación entre estado documental y evento

`EstadoDocumento` no reemplaza el hecho procesal asociado al documento.

Por ejemplo:
- `ACTO_ADMINISTRATIVO_FIRMADO` es un evento
- `FIRMADO` es un estado documental

El evento expresa el hito ocurrido.  
El estado documental expresa la situación formal vigente del documento.

---

# 7. Relación entre estado documental y estado de acta

El estado documental no reemplaza el estado de la acta.

Por ejemplo:
- una acta puede estar en `ACTO_ADMINISTRATIVO_EN_PROCESO`
- mientras el documento principal del acto está en `PENDIENTE_FIRMA`

Cada dimensión semántica debe conservar su propia responsabilidad.

---

# 8. Relación entre estado documental y notificación

Que un documento esté `FIRMADO` o `INCORPORADO` no significa automáticamente que esté notificado.

La notificación depende de:

- entidad `Notificacion`
- canal
- estado de notificación
- acuse
- eventos asociados

El documento puede estar completo formalmente, pero todavía no haber recorrido su circuito de notificación.

---

# 9. Regla de coexistencia entre FIRMADO e INCORPORADO

`FIRMADO` e `INCORPORADO` no son sinónimos.

## 9.1 FIRMADO
Pone el foco en la formalización del documento.

## 9.2 INCORPORADO
Pone el foco en su integración vigente al expediente o trámite.

Según el proceso, un documento puede:
- estar firmado pero aún no incorporado
- estar incorporado tras la firma
- o recorrer ambos estados en rápida sucesión

La implementación puede optimizar el flujo, pero la semántica debe seguir siendo distinguible.

---

# 10. Regla sobre OBSERVADO, INVALIDADO y REEMPLAZADO

Estos estados no deben confundirse.

## 10.1 OBSERVADO
El documento presenta problemas o reparos, pero no necesariamente fue excluido definitivamente del trámite.

## 10.2 INVALIDADO
El documento ya no tiene validez operativa/formal dentro del expediente.

## 10.3 REEMPLAZADO
El documento dejó de ser la versión activa porque otro lo sustituyó funcionalmente.

---

# 11. Regla de trazabilidad

Los documentos no deben desaparecer del historial solo porque cambien de estado.

Incluso si un documento queda:
- observado
- invalidado
- reemplazado

debe conservarse trazabilidad suficiente para entender:

- qué documento fue
- cuándo existió
- qué relación tuvo con el caso
- por qué dejó de estar vigente

---

# 12. Relación con snapshot y queries

`EstadoDocumento` puede ser utilizado por snapshot y queries para:

- detectar pendientes de firma
- detectar documentos observados
- identificar la última pieza documental válida
- construir bandejas documentales
- priorizar trabajo operativo

No obstante, snapshot y queries no deben inferir en exceso solo con este estado si hay datos más precisos disponibles.

---

# 13. Relación con frontend y móvil

Frontend web y móvil deben usar este catálogo como base canónica.

Pueden presentar:
- labels más amigables
- colores
- ayudas visuales
- agrupaciones operativas

pero no deben cambiar la semántica base del estado documental.

---

# 14. Regla de evolución del catálogo

Solo deben agregarse nuevos estados documentales si:

- representan una situación documental genuinamente distinta
- no pueden resolverse con metadata o flags
- no duplican un evento
- no duplican un estado técnico de infraestructura
- su uso es transversal y sostenido

La proliferación de estados documentales debe evitarse.

---

# 15. Recomendación de implementación

En implementación, `EstadoDocumento` debe compartirse entre:

- backend
- frontend
- móvil
- snapshot
- contratos documentales
- procesos de generación y firma

No deben existir versiones alternativas no documentadas.

---

# 16. Resumen ejecutivo

`EstadoDocumento` expresa la situación formal y operativa actual de un documento dentro de su ciclo de vida.

Debe ser:
- claro
- estable
- reutilizable
- compatible con múltiples tipos documentales
- útil para operación y trazabilidad

No reemplaza:
- el tipo documental
- el evento
- la firma
- la notificación
- el estado de la acta

Su función es dar semántica documental consistente al circuito formal del sistema.


