# [CANONICO] TIPO DE FIRMA

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el catálogo canónico `FirmaTipo`, estableciendo las modalidades de firma o formalización documental admitidas por el sistema y su uso consistente dentro del circuito documental.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/06-contratos/03-contratos-documentales.md`
- `spec/08-ddl-logico/02-documentos-y-firmas.md`

---

# 1. Definición general

`FirmaTipo` clasifica la modalidad de firma o formalización aplicada a un documento dentro del sistema.

Su finalidad es expresar, de manera clara y canónica:

- si el documento carece de firma
- si fue firmado digitalmente
- si fue firmado ológrafamente y luego re-subido
- si recibió sello técnico de inmutabilidad

`FirmaTipo` no reemplaza:

- el estado documental
- la validez jurídica completa del documento
- la evidencia técnica detallada de firma
- la metadata criptográfica
- la trazabilidad completa del motor de firma

Es una clasificación semántica de la forma principal de formalización.

---

# 2. Regla general de diseño

El catálogo de firma debe ser:

- simple
- estable
- jurídicamente comprensible
- operativo
- compatible con el modelo documental unificado

No debe capturar en este nivel:

- detalles criptográficos
- proveedor de firma
- algoritmo
- certificado específico
- datos del token o HSM
- detalles de infraestructura del firmador

Esos detalles pertenecen a metadata documental, evidencia de firma o al motor de firma.

---

# 3. Catálogo canónico `FirmaTipo`

Los valores canónicos iniciales son:

- `NINGUNA`
- `DIGITAL`
- `OLOGRAFA_RESUBIDA`
- `SELLO_TECNICO_INMUTABILIDAD`

---

# 4. Semántica de cada tipo de firma

## 4.1 NINGUNA
El documento no cuenta con firma ni formalización específica de las contempladas por el sistema.

Puede tratarse de:
- borradores
- documentos en preparación
- piezas informativas
- documentos que aún no pasaron por su circuito formal

No implica por sí mismo invalidez, pero sí ausencia de firma o sellado formal dentro del alcance de este catálogo.

---

## 4.2 DIGITAL
El documento fue firmado digitalmente mediante el mecanismo formal previsto por el sistema o por el circuito institucional aplicable.

Este valor expresa una firma digital ya aplicada al documento, sin entrar aquí en detalles sobre:
- certificado
- prestador
- token
- HSM
- agente local
- motor de firma

Todos esos detalles deben resolverse fuera del catálogo.

---

## 4.3 OLOGRAFA_RESUBIDA
El documento fue firmado ológrafamente fuera del sistema y luego re-subido o reincorporado como documento firmado.

Este valor permite reconocer situaciones en las que el documento ya llega firmado materialmente por fuera del motor de firma digital del sistema.

La firma ológrafa re-subida no debe confundirse con un borrador ni con una firma digital.

---

## 4.4 SELLO_TECNICO_INMUTABILIDAD
El documento recibió un sello técnico de inmutabilidad o mecanismo equivalente de aseguramiento técnico posterior, orientado a preservar integridad o trazabilidad.

Este valor no reemplaza a una firma digital de autoría.

Su función es expresar que el sistema aplicó un mecanismo técnico de resguardo o sellado, útil para integridad, conservación o trazabilidad documental.

---

# 5. Relación entre firma y estado documental

`FirmaTipo` no reemplaza a `EstadoDocumento`.

Por ejemplo:
- un documento puede estar en `PENDIENTE_FIRMA` con `FirmaTipo = NINGUNA`
- un documento puede estar en `FIRMADO` con `FirmaTipo = DIGITAL`
- un documento puede estar en `INCORPORADO` con `FirmaTipo = OLOGRAFA_RESUBIDA`

Ambas dimensiones deben mantenerse separadas.

---

# 6. Relación entre firma y tipo documental

`FirmaTipo` no reemplaza a `TipoDocumento`.

Por ejemplo:
- un documento puede ser `RESOLUCION`
- y tener `FirmaTipo = DIGITAL`
- o `FirmaTipo = OLOGRAFA_RESUBIDA`

La naturaleza del documento y la modalidad de firma son dimensiones distintas.

---

# 7. Regla sobre sello técnico

`SELLO_TECNICO_INMUTABILIDAD` no debe interpretarse automáticamente como firma digital autoral.

Expresa una formalización o aseguramiento técnico, no necesariamente autoría personal en el mismo sentido que `DIGITAL`.

Por lo tanto, no deben fusionarse ambos valores.

---

# 8. Regla sobre ológrafa re-subida

`OLOGRAFA_RESUBIDA` debe utilizarse cuando el documento ya fue firmado fuera del circuito digital del sistema y luego se incorpora como pieza firmada.

No debe utilizarse para:
- simples imágenes no firmadas
- documentos sin firma
- digitalizaciones sin valor formal reconocido
- documentos que aún esperan firma

Debe existir consistencia entre este valor y la realidad documental del caso.

---

# 9. Regla sobre coexistencia de firma y metadata

El catálogo `FirmaTipo` resume la modalidad principal de firma, pero puede coexistir con metadata adicional como:

- fecha de firma
- firmante
- cargo o rol
- identificador de operación
- hash
- evidencia técnica
- proveedor o mecanismo técnico

El catálogo no elimina la necesidad de metadata más rica cuando el sistema lo requiera.

---

# 10. Relación con motor de firma

El motor de firma o proceso documental puede manejar múltiples complejidades técnicas, pero el dominio canónico solo necesita clasificar el resultado principal mediante este catálogo.

Esto permite desacoplar:

- semántica del documento
- operación técnica de firma
- infraestructura de firmado

---

# 11. Relación con snapshot y queries

`FirmaTipo` puede ser útil para snapshot y queries en casos como:

- detectar documentos pendientes de firma
- distinguir documentos firmados digitalmente
- identificar documentos re-subidos firmados en papel
- construir filtros documentales
- apoyar auditorías o revisiones

No debe usarse en exceso como reemplazo del estado documental.

---

# 12. Relación con frontend y móvil

Frontend y móvil pueden mostrar labels más amigables para el tipo de firma, pero deben basarse en este catálogo canónico.

Puede variar:
- texto visible
- color
- ícono
- ayuda contextual

No debe variar:
- el valor canónico
- la semántica base
- el contrato compartido

---

# 13. Regla de evolución del catálogo

Solo deben agregarse nuevos valores a `FirmaTipo` si:

- representan una modalidad realmente distinta de formalización
- no pueden resolverse con metadata adicional
- su uso será transversal
- aportan claridad al circuito documental
- no fragmentan innecesariamente el canon

La proliferación de tipos de firma debe evitarse.

---

# 14. Recomendación de implementación

`FirmaTipo` debe compartirse entre:

- backend
- frontend
- móvil
- contratos documentales
- snapshot
- procesos documentales
- motor de firma o integración documental

No deben existir reinterpretaciones locales no documentadas.

---

# 15. Resumen ejecutivo

`FirmaTipo` clasifica la modalidad principal de firma o formalización documental.

Debe ser:
- simple
- estable
- operativo
- compatible con el modelo documental unificado

No reemplaza:
- el estado documental
- el tipo documental
- la metadata técnica de firma
- la validez jurídica completa del documento

Su función es aportar una semántica canónica y transversal sobre la forma principal de formalización del documento.