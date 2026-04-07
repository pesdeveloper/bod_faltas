# [CANONICO] TIPOS DE EVENTO DE ACTA

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el catálogo canónico `TipoEventoActa`, estableciendo cuáles son los hechos procesales reales que pueden integrar la narrativa de `ActaEvento`, cómo deben interpretarse y cuáles son las reglas para su uso correcto dentro del sistema.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/05-queries/`
- `spec/04-snapshot/`
- `spec/06-contratos/`

---

# 1. Definición general

`TipoEventoActa` define los hechos procesales reales que pueden registrarse en la bitácora `ActaEvento`.

Su función es expresar de manera explícita la narrativa del caso, permitiendo responder:

- qué pasó
- cuándo pasó
- quién intervino
- cuál fue el efecto
- qué documento lo respaldó, si corresponde

`TipoEventoActa` no representa eventos técnicos, logs internos ni detalles de infraestructura.

---

# 2. Regla general de uso

Un hecho debe registrarse como `ActaEvento` si:

- altera el estado procesal del caso
- representa un hito real del trámite
- expresa una decisión administrativa o jurídica
- debe quedar visible en la narrativa del expediente
- requiere trazabilidad funcional explícita

No debe registrarse como `ActaEvento` si solo representa:

- errores de red
- reintentos técnicos
- detalles de sincronización
- webhooks
- logs de storage
- eventos internos de infraestructura
- pasos efímeros del frontend o del móvil

---

# 3. Regla de diseño del catálogo

El catálogo de tipos de evento debe ser:

- expresivo
- estable
- útil para snapshot y queries
- lo suficientemente preciso para el dominio
- lo suficientemente simple para no explotar en cientos de variantes

No deben crearse eventos duplicados por contexto si el contexto ya puede resolverse mediante:
- documento asociado
- notificación asociada
- estado de acta
- flags del snapshot
- tipo documental

---

# 4. Catálogo canónico `TipoEventoActa`

Los valores canónicos iniciales son los siguientes:

ACTA_LABRADA
ACTA_IMPORTADA
ACTA_REASIGNADA
ACTA_CORREGIDA
ACTA_ENRIQUECIMIENTO_INICIADO
ACTA_ENRIQUECIDA
ACTA_OBSERVADA
ACTA_SUBSANADA
ACTA_ANULADA
ACTA_REABIERTA

EVIDENCIA_INCORPORADA
EVIDENCIA_RECTIFICADA
EVIDENCIA_INVALIDADA

DOCUMENTO_INCORPORADO
DOCUMENTO_REEMPLAZADO
DOCUMENTO_INVALIDADO

NOTIFICACION_ENVIADA
ACUSE_RECIBIDO
ACUSE_RECHAZADO
NOTIFICACION_REENVIADA

PRESENTACION_INCORPORADA
DESCARGO_PRESENTADO
COMPARECENCIA_REGISTRADA
SOLICITUD_PRONTO_DESPACHO
SOLICITUD_LIBERACION_PRESENTADA
SOLICITUD_PAGO_VOLUNTARIO
ACLARACION_PRESENTADA
DESISTIMIENTO_PRESENTADO

ANALISIS_INICIADO
ANALISIS_REQUERIDO
ANALISIS_COMPLETADO
MEDIDA_PREVENTIVA_REGISTRADA
MEDIDA_PREVENTIVA_LEVANTADA
RESOLUCION_PENDIENTE_DE_FIRMA

ACTO_ADMINISTRATIVO_GENERADO
ACTO_ADMINISTRATIVO_FIRMADO
ACTO_ADMINISTRATIVO_INVALIDADO

INTENCION_PAGO_REGISTRADA
PAGO_PENDIENTE_CONFIRMACION
PAGO_CONFIRMADO
PAGO_RECHAZADO
PAGO_IMPUTADO_EXTERNAMENTE

APELACION_INTERPUESTA
APELACION_ADMITIDA
APELACION_RECHAZADA
APELACION_ELEVADA
RESULTADO_APELACION_RECIBIDO
ACTO_REVISOR_INCORPORADO

DERIVACION_EXTERNA_ENVIADA
DERIVACION_EXTERNA_ADMITIDA
ACTUACION_EXTERNA_RECIBIDA
RESULTADO_EXTERNO_REGISTRADO
REINGRESO_DESDE_GESTION_EXTERNA

PARALIZACION_DISPUESTA
PARALIZACION_LEVANTADA
ARCHIVO_DISPUESTO
CIERRE_DISPUESTO
CUMPLIMIENTO_TOTAL_REGISTRADO

# 5. Semántica de los eventos

## 5.1 Eventos de inicio, identidad y evolución básica del acta

### ACTA_LABRADA
Marca el nacimiento operativo normal de la acta dentro del sistema.

Es el evento de entrada natural del recorrido D1.

### ACTA_IMPORTADA
Indica que la acta fue incorporada desde una fuente externa, migración o integración, conservando validez operativa dentro del sistema.

### ACTA_REASIGNADA
Indica que la responsabilidad, dependencia, circuito o referencia administrativa principal asociada a la acta fue reasignada de manera relevante.

### ACTA_CORREGIDA
Indica que la acta recibió una corrección con impacto operativo o administrativo real.

No debe usarse para microajustes técnicos irrelevantes.

### ACTA_ENRIQUECIMIENTO_INICIADO
Indica el inicio efectivo del trabajo de enriquecimiento administrativo posterior al labrado.

### ACTA_ENRIQUECIDA
Indica que el enriquecimiento del acta fue completado en forma suficiente para avanzar a la siguiente etapa del trámite.

### ACTA_OBSERVADA
Indica que la acta fue observada por una razón administrativa, jurídica o material que requiere atención.

### ACTA_SUBSANADA
Indica que la observación previa fue corregida o subsanada en forma suficiente.

### ACTA_ANULADA
Indica que la acta fue invalidada y que no corresponde continuar el recorrido normal del trámite.

### ACTA_REABIERTA
Indica que una acta previamente cerrada, archivada o detenida reingresa al circuito activo bajo una decisión válida.

---

## 5.2 Eventos de evidencia

### EVIDENCIA_INCORPORADA
Indica que se agregó evidencia relevante al caso y que dicha incorporación tiene valor procesal u operativo.

### EVIDENCIA_RECTIFICADA
Indica que una evidencia previamente incorporada fue rectificada o sustituida con efecto relevante.

### EVIDENCIA_INVALIDADA
Indica que una evidencia fue dejada sin valor operativo o procesal.

---

## 5.3 Eventos documentales

### DOCUMENTO_INCORPORADO
Indica que un documento relevante fue agregado formalmente al caso.

### DOCUMENTO_REEMPLAZADO
Indica que un documento previo fue reemplazado por otro con efecto válido sobre el trámite.

### DOCUMENTO_INVALIDADO
Indica que un documento quedó invalidado, observado definitivamente o sin efecto dentro del trámite.

---

## 5.4 Eventos de notificación

### NOTIFICACION_ENVIADA
Indica que una notificación fue emitida y enviada por un canal válido.

Este evento se reutiliza tanto para la notificación del acta como para la notificación del acto administrativo. El contexto lo da la notificación o el documento relacionado.

### ACUSE_RECIBIDO
Indica que se recibió un acuse válido vinculado a una notificación emitida.

### ACUSE_RECHAZADO
Indica que el acuse fue rechazado, resultó inválido o no tuvo efecto suficiente para consolidar la notificación.

### NOTIFICACION_REENVIADA
Indica que una notificación volvió a emitirse operativamente dentro del mismo trámite de notificación.

---

# 6. Regla fuerte sobre notificación

No forman parte del catálogo canónico los siguientes eventos:

- `NOTIFICACION_PRACTICADA`
- `RESULTADO_NOTIFICACION_INCORPORADO`

La narrativa de notificación debe resolverse con:
- `NOTIFICACION_ENVIADA`
- `ACUSE_RECIBIDO`
- `ACUSE_RECHAZADO`
- `NOTIFICACION_REENVIADA`

y con el contexto provisto por `Notificacion`, `Documento` y `EstadoActa`.

---

# 7. Eventos de presentaciones e intervenciones de parte

### PRESENTACION_INCORPORADA
Indica que se incorporó formalmente una presentación al expediente.

### DESCARGO_PRESENTADO
Indica la presentación formal de un descargo.

### COMPARECENCIA_REGISTRADA
Indica que se registró una comparecencia relevante dentro del trámite.

### SOLICITUD_PRONTO_DESPACHO
Indica la presentación de una solicitud de pronto despacho.

### SOLICITUD_LIBERACION_PRESENTADA
Indica una solicitud formal de liberación vinculada al caso.

### SOLICITUD_PAGO_VOLUNTARIO
Indica la presentación de una solicitud o manifestación formal de pago voluntario.

### ACLARACION_PRESENTADA
Indica la incorporación de una aclaración relevante al expediente.

### DESISTIMIENTO_PRESENTADO
Indica la presentación formal de un desistimiento.

---

# 8. Eventos de análisis y trabajo interno

### ANALISIS_INICIADO
Indica el comienzo efectivo de la etapa de análisis del caso.

### ANALISIS_REQUERIDO
Indica que se requirió formalmente análisis, revisión o intervención administrativa adicional.

### ANALISIS_COMPLETADO
Indica que la etapa de análisis vigente fue completada en forma suficiente.

### MEDIDA_PREVENTIVA_REGISTRADA
Indica que se registró una medida preventiva relevante dentro del expediente.

### MEDIDA_PREVENTIVA_LEVANTADA
Indica que una medida preventiva vigente fue levantada o dejada sin efecto.

### RESOLUCION_PENDIENTE_DE_FIRMA
Indica que existe una resolución o acto en estado de preparación suficientemente avanzado como para quedar pendiente de firma.

---

# 9. Eventos del acto administrativo

### ACTO_ADMINISTRATIVO_GENERADO
Indica que el acto administrativo fue generado en forma suficiente dentro del circuito documental y administrativo.

### ACTO_ADMINISTRATIVO_FIRMADO
Indica que el acto administrativo obtuvo la firma o formalización correspondiente.

### ACTO_ADMINISTRATIVO_INVALIDADO
Indica que el acto administrativo quedó sin efecto, invalidado o reemplazado de manera relevante.

---

# 10. Regla de simplificación del acto administrativo

No se crean por ahora eventos específicos paralelos como:
- `FALLO_DICTADO`
- `RESOLUCION_DICTADA`

La naturaleza concreta del acto debe poder resolverse principalmente por:
- `TipoDocumento`
- `Documento`
- contexto del expediente

El evento canónico se mantiene en un nivel más simple:
- `ACTO_ADMINISTRATIVO_GENERADO`
- `ACTO_ADMINISTRATIVO_FIRMADO`
- `ACTO_ADMINISTRATIVO_INVALIDADO`

---

# 11. Eventos con impacto económico mínimo

### INTENCION_PAGO_REGISTRADA
Indica que se registró formalmente la intención de pago o manifestación equivalente con efecto procesal.

### PAGO_PENDIENTE_CONFIRMACION
Indica que el sistema recibió un antecedente de pago aún no consolidado como confirmado.

### PAGO_CONFIRMADO
Indica que el pago fue confirmado en grado suficiente para producir efecto procesal.

### PAGO_RECHAZADO
Indica que el antecedente de pago no fue aceptado o no produjo efecto válido.

### PAGO_IMPUTADO_EXTERNAMENTE
Indica que el pago fue reflejado o imputado desde un sistema externo con efecto relevante para el caso.

---

# 12. Eventos de apelación y revisión

### APELACION_INTERPUESTA
Indica la interposición formal de una apelación o recurso.

### APELACION_ADMITIDA
Indica que la apelación fue admitida o considerada procedente para continuar su trámite.

### APELACION_RECHAZADA
Indica que la apelación fue rechazada o declarada improcedente.

### APELACION_ELEVADA
Indica que la apelación fue elevada a la instancia externa o superior correspondiente.

### RESULTADO_APELACION_RECIBIDO
Indica que se recibió el resultado o pronunciamiento derivado del proceso de apelación.

### ACTO_REVISOR_INCORPORADO
Indica que se incorporó formalmente el acto o documento resultante de la revisión o apelación.

---

# 13. Eventos de gestión externa

### DERIVACION_EXTERNA_ENVIADA
Indica que el caso fue derivado formalmente a una gestión externa relevante.

### DERIVACION_EXTERNA_ADMITIDA
Indica que la instancia externa admitió o tomó formalmente el caso.

### ACTUACION_EXTERNA_RECIBIDA
Indica que se recibió una actuación, novedad o documento proveniente de la gestión externa.

### RESULTADO_EXTERNO_REGISTRADO
Indica que se consolidó un resultado externo relevante para el recorrido del caso.

### REINGRESO_DESDE_GESTION_EXTERNA
Indica que el caso volvió al circuito interno desde una instancia externa.

---

# 14. Eventos de cierre o excepción

### PARALIZACION_DISPUESTA
Indica que se dispuso formalmente la paralización del trámite.

### PARALIZACION_LEVANTADA
Indica que la paralización vigente fue levantada.

### ARCHIVO_DISPUESTO
Indica que se resolvió el archivo administrativo del caso.

### CIERRE_DISPUESTO
Indica que se dispuso formalmente el cierre del caso por una causa válida distinta o más amplia que el archivo.

### CUMPLIMIENTO_TOTAL_REGISTRADO
Indica que se registró un cumplimiento suficiente como para considerar concluido materialmente el caso.

---

# 15. Relación entre evento y estado de acta

Un `TipoEventoActa` no es lo mismo que un `EstadoActa`.

- El evento expresa lo ocurrido.
- El estado expresa dónde quedó situado el caso luego de lo ocurrido.

Por ejemplo:
- `ACTA_ENRIQUECIDA` es un evento
- `ENRIQUECIDA` es un estado

Esto debe mantenerse separado en todo el sistema.

---

# 16. Relación entre evento y documento

Un evento puede existir:
- con documento asociado
- sin documento asociado

Pero cuando el hecho solo existe jurídicamente a través de un documento, la relación con `Documento` debe ser explícita.

Ejemplos típicos:
- actos administrativos
- presentaciones
- resoluciones
- constancias
- notificaciones formales

---

# 17. Relación con snapshot

El snapshot debe consumir este catálogo como base narrativa para derivar:

- estado actual
- flags operativos
- bandejas
- prioridades
- hitos relevantes

El snapshot no debe inventar nuevos tipos de evento.

---

# 18. Relación con frontend y móvil

Frontend web y aplicación móvil deben utilizar exactamente estos tipos de evento cuando muestren historial, timelines, trazabilidad o acciones registradas.

Pueden cambiar:
- etiquetas visibles
- agrupación visual
- iconos
- orden de presentación

pero no el valor canónico del evento.

---

# 19. Regla de evolución del catálogo

Solo deben incorporarse nuevos tipos de evento si:

- representan un hecho procesal real
- no alcanzan los eventos actuales
- no puede resolverse la necesidad con documento + contexto
- no puede resolverse con flags o snapshot
- existe utilidad transversal para queries, historial o contratos

No deben agregarse eventos por conveniencia local o por detalle técnico.

---

# 20. Resumen ejecutivo

`TipoEventoActa` define los hechos procesales reales que integran la narrativa del caso.

Debe permitir un historial:
- claro
- trazable
- jurídicamente comprensible
- operativamente útil
- compatible con snapshot, queries y contratos

No incluye logs técnicos, ni detalles de infraestructura, ni duplicaciones semánticas innecesarias.