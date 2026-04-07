# [CANONICO] INTEGRACIÓN EXTERNA

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir los catálogos canónicos vinculados a la gestión e integración externa del sistema, estableciendo cómo clasificar la naturaleza de una gestión externa y cómo clasificar el resultado externo que impacta sobre el caso.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/04-snapshot/`
- `spec/05-queries/`
- `spec/06-contratos/06-contratos-integracion-externa.md`

---

# 1. Definición general

La integración externa forma parte del recorrido posible del caso cuando el trámite:

- sale del circuito interno principal
- interactúa con un organismo o instancia externa
- recibe actuaciones o resultados desde fuera del sistema
- vuelve al circuito interno con impacto procesal relevante

La integración externa no se modela como un agregado central autónomo del dominio.

Se resuelve mediante:

- eventos
- documentos
- referencias externas
- snapshot
- contratos de integración
- y estos catálogos canónicos

---

# 2. Catálogos incluidos en este documento

Este documento define:

1. `TipoGestionExterna`
2. `ResultadoExterno`

---

# 3. Catálogo canónico `TipoGestionExterna`

Los valores canónicos iniciales son:

- `APREMIO`
- `JUZGADO`
- `ORGANISMO_EXTERNO`
- `OTRA`

---

# 4. Semántica de cada tipo de gestión externa

## 4.1 APREMIO
Gestión externa vinculada a instancia de apremio, ejecución o circuito equivalente de cobro o exigibilidad externa.

Debe utilizarse cuando el caso sale del circuito principal hacia esa clase de gestión formal.

---

## 4.2 JUZGADO
Gestión externa vinculada a juzgado, instancia judicial o circuito formal judicial equivalente.

Debe usarse cuando el caso ingresa, se eleva o interactúa con una instancia judicial claramente identificable como tal.

---

## 4.3 ORGANISMO_EXTERNO
Gestión externa vinculada a organismo, dependencia o actor institucional externo al sistema principal, distinto de apremio o juzgado, pero con intervención formal sobre el caso.

Ejemplos posibles:
- otra dependencia con sistema propio
- organismo provincial
- organismo municipal externo al circuito principal
- entidad con intercambio formal de actuaciones

---

## 4.4 OTRA
Clasificación residual y excepcional para gestiones externas formalmente relevantes que no encajan razonablemente en una categoría más precisa.

Su uso debe revisarse periódicamente.

---

# 5. Regla general sobre `TipoGestionExterna`

`TipoGestionExterna` expresa la naturaleza principal del circuito externo involucrado.

No expresa por sí solo:

- el estado actual del caso
- el resultado de la gestión
- el evento concreto ocurrido
- el canal técnico de integración
- el protocolo de intercambio

Es una clasificación de naturaleza funcional, no de detalle técnico.

---

# 6. Catálogo canónico `ResultadoExterno`

Los valores canónicos iniciales son:

- `SIN_RESULTADO`
- `PENDIENTE`
- `PAGO`
- `CONFIRMACION`
- `REVOCACION`
- `MODIFICACION`
- `ANULACION`
- `ARCHIVO`
- `OTRO`

---

# 7. Semántica de cada resultado externo

## 7.1 SIN_RESULTADO
No existe todavía un resultado externo consolidado o reconocible para el caso.

Sirve para expresar ausencia actual de desenlace externo.

---

## 7.2 PENDIENTE
La gestión externa existe, pero su resultado todavía está pendiente, en curso o no consolidado.

---

## 7.3 PAGO
La gestión externa produjo o confirmó un resultado de pago con relevancia para el trámite.

No reemplaza el detalle económico profundo; solo expresa el efecto externo principal.

---

## 7.4 CONFIRMACION
La gestión externa devolvió una confirmación del criterio, decisión o situación previa relevante del caso.

Debe usarse cuando el resultado confirma lo actuado o sostenido previamente.

---

## 7.5 REVOCACION
La gestión externa produjo un resultado revocatorio respecto de una decisión, situación o estado anterior del caso.

---

## 7.6 MODIFICACION
La gestión externa produjo un cambio parcial o una alteración relevante del resultado previo, sin llegar necesariamente a confirmar o revocar por completo.

---

## 7.7 ANULACION
La gestión externa produjo un resultado invalidante que anula o deja sin efecto lo actuado o parte relevante del caso.

---

## 7.8 ARCHIVO
La gestión externa concluyó en archivo o cierre por criterio equivalente de archivo.

---

## 7.9 OTRO
Resultado residual y excepcional para casos externamente relevantes que no encajan razonablemente en una categoría más precisa.

Debe revisarse periódicamente.

---

# 8. Regla general sobre `ResultadoExterno`

`ResultadoExterno` expresa el desenlace principal reconocido desde la gestión externa.

No reemplaza:

- el tipo de evento
- el estado de acta
- el documento recibido
- la narrativa detallada del caso
- el contenido jurídico completo del resultado

Es una clasificación resumida del efecto externo principal.

---

# 9. Relación entre gestión externa y evento

La gestión externa no se modela solo con catálogo.

Los hechos relevantes del recorrido deben seguir expresándose mediante eventos como:

- `DERIVACION_EXTERNA_ENVIADA`
- `DERIVACION_EXTERNA_ADMITIDA`
- `ACTUACION_EXTERNA_RECIBIDA`
- `RESULTADO_EXTERNO_REGISTRADO`
- `REINGRESO_DESDE_GESTION_EXTERNA`

Los catálogos de este documento no reemplazan a esos eventos; los complementan.

---

# 10. Relación entre integración externa y estado de acta

La existencia de gestión externa o resultado externo puede impactar en `EstadoActa`, por ejemplo:

- `EN_GESTION_EXTERNA`
- `FINALIZADA`
- `ARCHIVADA`
- `ANULADA`

Pero `TipoGestionExterna` y `ResultadoExterno` no deben confundirse con el estado global del caso.

---

# 11. Relación con snapshot

El snapshot debe poder derivar y exponer, cuando corresponda:

- si existe gestión externa activa
- tipo principal de gestión externa
- existencia de resultado externo
- clase principal del resultado externo
- fecha del último hito externo relevante
- necesidad de reingreso o seguimiento

Esto mejora bandejas, filtros y diagnósticos operativos.

---

# 12. Relación con documentos

La gestión externa puede venir acompañada por:

- actuaciones
- constancias
- resoluciones
- notificaciones
- otros documentos

Los catálogos de este documento no reemplazan la clasificación documental.

El documento sigue resolviéndose mediante `TipoDocumento` y su circuito documental propio.

---

# 13. Relación con frontend y móvil

Frontend y móvil pueden mostrar nombres más amigables para estos valores, pero deben basarse en el catálogo canónico.

Pueden variar:
- labels
- colores
- agrupaciones
- ayudas contextuales

No debe variar:
- el valor canónico
- la semántica base
- el contrato de intercambio

---

# 14. Regla de evolución del catálogo

Solo deben agregarse nuevos valores a `TipoGestionExterna` o `ResultadoExterno` si:

- representan una categoría verdaderamente distinta
- no pueden resolverse con metadata adicional
- su uso será transversal
- mejoran claridad operativa
- no fragmentan innecesariamente el vocabulario

La proliferación de categorías externas debe evitarse.

---

# 15. Recomendación de implementación

Estos catálogos deben compartirse entre:

- backend
- frontend
- snapshot
- contratos de integración externa
- queries operativas
- reportes y trazabilidad

No deben existir reinterpretaciones locales no documentadas.

---

# 16. Resumen ejecutivo

La integración externa se clasifica canónicamente mediante:

- `TipoGestionExterna`
- `ResultadoExterno`

Estos catálogos permiten describir:
- qué clase de circuito externo intervino
- qué resultado principal devolvió

No reemplazan:
- eventos
- documentos
- estado de acta
- snapshot
- contratos

Su función es aportar semántica estable y transversal al tramo externo del recorrido del caso.
