# [CANONICO] TIPOS DE DOCUMENTO

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el catálogo canónico `TipoDocumento`, estableciendo cuáles son los tipos documentales formales admitidos por el sistema, cómo deben interpretarse y cuáles son las reglas para su uso consistente en dominio, contratos, snapshot, queries e implementación.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/06-contratos/03-contratos-documentales.md`
- `spec/08-ddl-logico/02-documentos-y-firmas.md`

---

# 1. Definición general

`TipoDocumento` clasifica la naturaleza formal de un documento dentro del sistema.

No define por sí solo:

- el estado del documento
- su validez vigente
- si está firmado o no
- si produjo efecto procesal
- si ya fue notificado
- si reemplaza a otro

Esas dimensiones se resuelven con otros conceptos del sistema, como por ejemplo:

- `EstadoDocumento`
- `FirmaTipo`
- `ActaEvento`
- relaciones documentales
- notificación asociada
- snapshot derivado

---

# 2. Regla general de diseño

Los tipos documentales deben ser:

- pocos
- claros
- administrativamente comprensibles
- jurídicamente razonables
- reutilizables entre etapas del proceso

No debe crearse un tipo documental nuevo si la diferencia puede resolverse mediante:

- metadata
- relación con el evento
- estado documental
- observaciones
- configuración de plantilla o formato
- subtipo parametrizable fuera del catálogo base

---

# 3. Catálogo canónico `TipoDocumento`

Los valores canónicos iniciales son los siguientes:

ACTA
ACTA_COMPLEMENTARIA
FALLO
RESOLUCION
RESOLUCION_CONCESION_APELACION
RESOLUCION_IMPROCEDENCIA_APELACION
NOTIFICACION
NOTIFICACION_RESULTADO_JUDICIAL
DDJJ_DOMICILIO_PRESENCIAL
ACTUACION
NOTA_ABOGADO
NOTA_INFRACTOR
PRESENTACION
CONSTANCIA
OTRO

# 4. Semántica de cada tipo documental

## 4.1 ACTA
Documento principal que representa el labrado o existencia formal inicial del caso.

Es el documento base del sistema y normalmente corresponde al inicio operativo del trámite.

---

## 4.2 ACTA_COMPLEMENTARIA
Documento complementario vinculado a una acta principal, utilizado para extender, complementar o registrar contenido accesorio relevante del caso.

Este tipo permite cubrir variantes complementarias sin crear nuevos tipos documentales específicos para cada una.

Los subtipos concretos o finalidades particulares podrán resolverse por metadata, contenido, plantilla o parametrización adicional.

---

## 4.3 FALLO
Documento que expresa una decisión resolutiva con naturaleza de fallo dentro del trámite.

Su existencia y uso dependen del proceso administrativo y jurídico vigente del sistema.

---

## 4.4 RESOLUCION
Documento que expresa una resolución administrativa formal dentro del caso.

Debe utilizarse cuando la pieza documental tenga naturaleza resolutiva, pero no corresponda clasificarla específicamente como fallo ni como una resolución especial ya tipificada por el catálogo.

---

## 4.5 RESOLUCION_CONCESION_APELACION
Documento que expresa formalmente la concesión de una apelación o recurso.

Se mantiene explícito en el catálogo porque tiene semántica administrativa suficientemente clara y diferenciable.

---

## 4.6 RESOLUCION_IMPROCEDENCIA_APELACION
Documento que expresa formalmente la improcedencia, rechazo o no concesión de una apelación o recurso.

Se mantiene explícito para evitar ambigüedad documental en el tramo recursivo.

---

## 4.7 NOTIFICACION
Documento formal de notificación emitido dentro del trámite, ya sea respecto del acta, del acto administrativo u otro hito notificable del expediente.

El contexto concreto surge de la relación con el caso, el evento, la notificación operativa asociada y el documento principal que se notifica.

---

## 4.8 NOTIFICACION_RESULTADO_JUDICIAL
Documento de notificación vinculado específicamente al resultado proveniente de una instancia judicial o equivalente externa, cuando dicho resultado debe ser formalmente notificado dentro del sistema.

---

## 4.9 DDJJ_DOMICILIO_PRESENCIAL
Documento que registra una declaración jurada de domicilio constituida o presentada presencialmente.

Se mantiene explícito porque tiene función administrativa específica y relevancia operativa clara.

---

## 4.10 ACTUACION
Documento formal de actuación administrativa incorporado al expediente.

Debe usarse como categoría amplia para piezas documentales de trámite interno o externo que no encajen mejor en un tipo más específico del catálogo.

---

## 4.11 NOTA_ABOGADO
Documento presentado o incorporado con naturaleza de nota proveniente de abogado o representante letrado.

Se mantiene explícito porque la procedencia y tipo de intervención pueden tener relevancia administrativa y jurídica.

---

## 4.12 NOTA_INFRACTOR
Documento presentado o incorporado con naturaleza de nota proveniente del infractor.

Se mantiene explícito para distinguirlo de otras presentaciones más genéricas cuando esa diferencia importe administrativamente.

---

## 4.13 PRESENTACION
Documento de presentación formal genérica incorporado al expediente.

Debe utilizarse cuando exista una presentación relevante, pero no corresponda clasificarla mejor como:
- nota de abogado
- nota de infractor
- declaración jurada
- resolución
- fallo
- constancia

---

## 4.14 CONSTANCIA
Documento que deja constancia formal de un hecho, actuación, recepción, verificación o circunstancia relevante dentro del trámite.

Es una categoría útil para piezas de soporte formal sin naturaleza resolutiva principal.

---

## 4.15 OTRO
Tipo documental residual y excepcional.

Debe utilizarse solo cuando:
- el documento sea formalmente relevante
- no exista un tipo documental adecuado en el catálogo actual
- no resulte razonable forzar su clasificación en una categoría existente

El uso de `OTRO` debe revisarse periódicamente para detectar si corresponde incorporar una categoría nueva al canon.

---

# 5. Regla sobre subtipos y especialización

El catálogo canónico de `TipoDocumento` no debe crecer innecesariamente para capturar cada variante documental menor.

Cuando sea necesario distinguir variantes adicionales, debe evaluarse si alcanza con:

- metadata documental
- plantilla
- nombre lógico
- relación con evento
- relación con etapa del trámite
- configuración externa
- subtipo parametrizable no canónico

Solo si la diferencia tiene relevancia transversal, estable y fuerte, corresponde crear un nuevo tipo documental canónico.

---

# 6. Regla sobre documentos y eventos

El tipo documental no reemplaza al tipo de evento.

Por ejemplo:

- `RESOLUCION` es un tipo documental
- `ACTO_ADMINISTRATIVO_GENERADO` es un evento

Ambos conceptos deben convivir de manera complementaria.

El documento expresa el soporte formal.  
El evento expresa el hecho procesal ocurrido.

---

# 7. Regla sobre documentos y estado documental

El tipo documental no reemplaza el estado documental.

Por ejemplo, un documento `RESOLUCION` podría encontrarse en estados distintos como:

- borrador
- generado
- pendiente de firma
- firmado
- incorporado
- invalidado

Por lo tanto, `TipoDocumento` y `EstadoDocumento` deben mantenerse siempre separados.

---

# 8. Regla sobre documentos y firma

El tipo documental no define automáticamente el tipo de firma.

La firma o formalización se resuelve mediante:

- `FirmaTipo`
- `DocumentoFirma`
- reglas documentales del proceso correspondiente

Esto permite que dos documentos del mismo tipo puedan atravesar circuitos formales distintos cuando la normativa o el proceso así lo requieran.

---

# 9. Relación con el modelo documental unificado

`TipoDocumento` forma parte del modelo documental unificado del sistema.

Eso implica que el sistema evita construir entidades documentales paralelas por etapa, utilizando en cambio:

- una entidad documental común
- este catálogo como clasificador semántico
- estados documentales
- firma
- relaciones con acta y evento

Este enfoque es central para mantener simplicidad e implementabilidad.

---

# 10. Relación con notificación

No todo documento `NOTIFICACION` implica por sí solo que la notificación haya producido efecto procesal suficiente.

La efectividad de la notificación depende también de:

- la entidad `Notificacion`
- su estado operativo
- el canal utilizado
- el acuse
- los eventos procesales asociados

El documento es soporte formal.  
La gestión operativa y el efecto procesal se resuelven en otras capas del modelo.

---

# 11. Relación con snapshot y queries

`TipoDocumento` puede ser utilizado por snapshot y queries para:

- identificar el último acto formal relevante
- identificar documentos pendientes
- clasificar documentación incorporada
- construir filtros documentales
- mostrar hitos principales del expediente

Sin embargo, snapshot y queries no deben inferir semántica excesiva solo desde el tipo documental si existen otros datos más precisos.

---

# 12. Relación con frontend y móvil

Frontend web y aplicación móvil deben compartir exactamente este catálogo como base canónica.

Pueden variar:
- labels visibles
- agrupaciones visuales
- íconos
- ayudas contextuales

pero no debe cambiar el valor canónico del tipo documental.

---

# 13. Regla de evolución del catálogo

Solo debe incorporarse un nuevo `TipoDocumento` si:

- representa una categoría documental realmente distinta
- la diferencia tiene valor administrativo o jurídico estable
- no alcanza con metadata o parametrización
- su uso será transversal y no local
- mejora claridad del sistema en lugar de fragmentarlo

La incorporación de nuevos tipos documentales debe ser excepcional y revisada contra todo el canon.

---

# 14. Recomendación de implementación

En implementación, `TipoDocumento` debe ser tratado como catálogo canónico compartido entre:

- backend
- frontend
- móvil
- snapshot
- contratos
- persistencia documental

No deben existir reinterpretaciones locales no documentadas.

---

# 15. Resumen ejecutivo

`TipoDocumento` clasifica la naturaleza formal del documento dentro del sistema.

Debe ser:
- estable
- simple
- jurídicamente razonable
- administrativamente útil
- compatible con el modelo documental unificado

No reemplaza:
- el evento
- el estado documental
- la firma
- la notificación operativa
- el snapshot

Su función es aportar semántica documental canónica y consistente a todo el sistema.
