# [CANONICO] CAMPOS DEL SNAPSHOT OPERATIVO

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir los campos canónicos mínimos y recomendados de `ActaSnapshotOperativo`, estableciendo qué información operativa resumida debe proyectarse para soportar bandejas, queries, filtros, vistas rápidas e indicadores.

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
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/05-queries/`
- `spec/06-contratos/04-contratos-snapshot.md`

---

# 1. Definición general

`ActaSnapshotOperativo` es la proyección resumida y persistida del estado operativo actual de una acta.

Sus campos deben ser:

- útiles
- estables
- derivables
- operativos
- consistentes con el canon

No deben copiar el dominio completo ni convertirse en una tabla paralela que intente reemplazar al expediente.

---

# 2. Regla general de diseño

Cada campo del snapshot debe justificarse por al menos uno de estos motivos:

- se usa en bandejas
- se usa en filtros
- se usa en búsquedas
- se usa en prioridades operativas
- se usa en indicadores
- se usa en lectura resumida frecuente

Si un campo no aporta valor operativo real, no debería formar parte del snapshot.

---

# 3. Criterio de organización de campos

Los campos del snapshot se agrupan en estos bloques conceptuales:

1. Identificación
2. Estado principal
3. Último hito relevante
4. Notificación
5. Documento
6. Gestión externa
7. Cierre
8. Flags operativos
9. Fechas operativas
10. Auditoría de snapshot

---

# 4. Campos mínimos obligatorios

## 4.1 Identificación mínima

### IdActa
Identificador principal de la acta a la que pertenece el snapshot.

Este campo es obligatorio.

### NumeroActa
Número visible o referencia principal del acta, cuando corresponda exponerla operativamente.

Debe existir si el sistema ya dispone de ese dato de manera consolidada.

### AnioActa
Año operativo o de referencia principal del caso, cuando aplique al modelo del acta.

### IdDependenciaActual
Identificador de la dependencia, área o unidad actual responsable del caso, si el dominio lo maneja.

### DependenciaActualNombre
Nombre amigable o descripción resumida de la dependencia actual, cuando convenga materializarlo para lectura rápida.

---

## 4.2 Estado principal

### EstadoActa
Estado operativo principal vigente del caso.

Debe usar exclusivamente el catálogo canónico `EstadoActa`.

Es uno de los campos más importantes del snapshot.

### MotivoCierreActa
Motivo principal de cierre, cuando el caso se encuentre en un estado final o equivalente.

Debe usar exclusivamente el catálogo canónico `MotivoCierreActa`.

Puede ser nulo mientras el caso siga abierto.

### EstaCerrada
Flag booleano que indica si el caso se encuentra efectivamente cerrado.

Normalmente se deriva del estado:
- `ARCHIVADA`
- `ANULADA`
- `FINALIZADA`

### EstaActiva
Flag booleano operativo complementario que indica si el caso sigue formando parte del trabajo activo.

No es lo mismo que “no cerrada” en todos los escenarios, pero normalmente se relaciona con ello.

---

## 4.3 Último hito relevante

### UltimoTipoEventoActa
Último tipo de evento relevante considerado por el snapshot para explicar la situación actual del caso.

Debe usar `TipoEventoActa`.

### UltimoOrigenEvento
Origen funcional del último evento relevante.

Debe usar `OrigenEvento`.

### UltimoEventoFechaHora
Fecha y hora del último evento relevante tomado en cuenta por el snapshot.

### UltimoEventoDescripcionCorta
Descripción breve, resumida y operativa del último hito, solo si realmente aporta valor de lectura.

Debe ser breve y derivada, no narrativa extensa.

---

# 5. Campos de notificación

## 5.1 Estado operativo de notificación

### TieneNotificacionActiva
Flag que indica si existe una notificación en curso o pendiente con relevancia operativa actual.

### UltimoEstadoNotificacion
Estado de la última notificación relevante para el caso.

Debe usar el catálogo `EstadoNotificacion`.

### UltimoCanalNotificacion
Canal de la última notificación relevante.

Debe usar el catálogo `CanalNotificacion`.

### UltimaNotificacionFechaHora
Fecha y hora del último hito relevante de notificación.

### RequiereReenvioNotificacion
Flag que indica si el caso requiere atención operativa por rechazo, falta de resultado o necesidad de reenvío.

### TieneAcuseRecibidoVigente
Flag que indica si existe un acuse vigente y útil para la etapa notificatoria correspondiente.

---

## 5.2 Contexto de etapa notificatoria

### EtapaNotificatoriaActual
Campo resumido que identifica si la situación notificatoria vigente se refiere principalmente a:
- notificación del acta
- notificación del acto administrativo
- otro tramo notificable

Este campo puede ser texto canónico controlado o derivación equivalente, según la implementación final.

---

# 6. Campos documentales

## 6.1 Documento principal relevante

### UltimoTipoDocumentoRelevante
Tipo del último documento relevante para la lectura operativa del caso.

Debe usar `TipoDocumento`.

### UltimoEstadoDocumentoRelevante
Estado del último documento relevante.

Debe usar `EstadoDocumento`.

### UltimoFirmaTipoRelevante
Tipo de firma o formalización del último documento relevante.

Debe usar `FirmaTipo`.

### UltimoDocumentoFechaHora
Fecha y hora del último hito documental relevante.

---

## 6.2 Pendientes documentales

### TieneDocumentoPendienteFirma
Flag que indica existencia de al menos un documento relevante en estado de pendiente de firma.

### TieneDocumentoObservado
Flag que indica existencia de documento observado con impacto operativo vigente.

### TieneDocumentoInvalidadoVigente
Flag que indica existencia de documento invalidado que todavía requiere atención operativa o contextual.

Este campo debe usarse con cuidado y solo si aporta valor real.

---

# 7. Campos de gestión externa

## 7.1 Situación externa actual

### TieneGestionExternaActiva
Flag que indica si el caso se encuentra actualmente en gestión externa activa.

### TipoGestionExternaActual
Tipo principal de gestión externa vigente.

Debe usar `TipoGestionExterna`.

### ResultadoExternoActual
Resultado externo principal reconocido, si existe.

Debe usar `ResultadoExterno`.

### UltimoMovimientoExternoFechaHora
Fecha y hora del último movimiento externo relevante.

### RequiereSeguimientoExterno
Flag que indica si la gestión externa aún requiere atención o seguimiento operativo.

---

# 8. Campos de cierre

## 8.1 Cierre y finalización

### FechaHoraCierre
Fecha y hora en la que el caso alcanzó su cierre efectivo, si corresponde.

### TipoEventoCierre
Tipo de evento que consolidó el cierre principal del caso.

Debe usar `TipoEventoActa`.

### TieneCierreFirme
Flag que indica si el cierre alcanzado puede considerarse firme o consolidado según la lógica del dominio y snapshot.

Este campo es útil para reporting y filtros, pero debe derivarse con reglas claras.

---

# 9. Flags operativos recomendados

## 9.1 Flags de atención y trabajo

### RequiereAccionOperativa
Flag general que indica si el caso requiere alguna acción operativa humana.

### RequiereRevisionAdministrativa
Flag que indica si el caso requiere revisión administrativa o análisis.

### TienePresentacionPendiente
Flag que indica existencia de presentación o intervención pendiente de tratamiento operativo.

### TieneApelacionActiva
Flag que indica si el caso atraviesa una apelación o revisión vigente.

### TieneMedidaPreventivaVigente
Flag que indica existencia de medida preventiva relevante aún vigente.

### EstaParalizada
Flag que indica si el caso se encuentra paralizado.

### EsPriorizable
Flag general orientado a bandejas o priorización, si el modelo operativo lo necesita.

Debe derivarse con reglas simples y explícitas.

---

# 10. Fechas operativas recomendadas

## 10.1 Fechas para orden, filtro y seguimiento

### FechaHoraCreacionActa
Fecha y hora de creación/labrado de la acta.

### FechaHoraUltimaActualizacionDominio
Fecha y hora del último cambio relevante del dominio que impactó el snapshot.

### FechaHoraUltimaActualizacionSnapshot
Fecha y hora en que el snapshot fue recalculado o persistido por última vez.

### FechaHoraUltimoPendienteOperativo
Fecha y hora asociada al último pendiente operativo relevante, si el sistema necesita priorizar por antigüedad del pendiente.

---

# 11. Campos de auditoría mínima del snapshot

## 11.1 Trazabilidad del cálculo

### VersionReglaSnapshot
Versión lógica de reglas con la que fue calculado el snapshot.

Este campo es muy útil para reprocesos, migraciones y auditoría.

### UltimaFuenteDetonante
Tipo resumido de fuente que detonó el último recálculo del snapshot, por ejemplo:
- evento
- documento
- notificación
- reproceso

No es obligatorio en todos los casos, pero es muy recomendable.

---

# 12. Campos opcionales útiles según contexto

Los siguientes campos pueden resultar útiles, pero no deben incorporarse si no aportan valor real inmediato:

- nombre corto del infractor o sujeto visible
- tipo principal de acta
- referencia territorial resumida
- indicador de antigüedad del trámite
- cantidad de intentos de notificación
- cantidad de documentos relevantes
- cantidad de eventos relevantes
- indicador de “sin movimiento hace X tiempo”

Estos campos deben evaluarse contra costo, utilidad y consistencia.

---

# 13. Campos que NO deberían vivir en el snapshot

No deberían formar parte del snapshot canónico:

- historial completo de eventos
- historial completo documental
- blobs o contenido documental
- textos extensos del expediente
- detalles técnicos de sincronización móvil
- errores internos de jobs
- metadata de infraestructura
- información irrelevante para operación diaria
- campos duplicados sin justificación operativa

---

# 14. Regla sobre duplicación controlada

El snapshot necesariamente duplica cierta información del dominio, pero esa duplicación debe ser:

- intencional
- controlada
- derivable
- útil
- mínima

No debe duplicar por comodidad aquello que no se usa operativamente.

---

# 15. Regla sobre nulabilidad

Muchos campos del snapshot pueden ser nulos según el momento del caso.

Ejemplos típicos:
- `MotivoCierreActa` antes del cierre
- `ResultadoExternoActual` si nunca hubo gestión externa
- `UltimoFirmaTipoRelevante` si aún no hubo firma relevante
- `FechaHoraCierre` si el caso sigue abierto

La nulabilidad debe responder al proceso real, no a completitud artificial.

---

# 16. Regla sobre consistencia entre campos

Los campos del snapshot deben ser coherentes entre sí.

Ejemplos:
- si `EstaCerrada = true`, normalmente debe existir `EstadoActa` final
- si `TieneGestionExternaActiva = true`, debería existir contexto externo consistente
- si `TieneDocumentoPendienteFirma = true`, debería existir sustento documental real
- si `RequiereReenvioNotificacion = true`, debería existir una situación notificatoria compatible

Estas coherencias se detallarán luego en reglas de derivación.

---

# 17. Recomendación de implementación

En implementación, conviene tratar estos campos con tres niveles:

## 17.1 Campos obligatorios
Los imprescindibles para operación básica.

## 17.2 Campos recomendados
Los de alto valor para bandejas, filtros y trazabilidad.

## 17.3 Campos opcionales
Los que se incorporan solo si el caso real los necesita.

Esta clasificación permite crecer sin sobrecargar el snapshot desde el inicio.

---

# 18. Resumen ejecutivo

`ActaSnapshotOperativo` debe contener una selección mínima y útil de datos operativos derivados del dominio.

Sus campos deben permitir:

- saber dónde está el caso
- saber qué requiere
- saber cuál fue su último hito
- saber si tiene pendientes
- saber si está cerrado o activo
- construir bandejas y queries rápidas

No debe copiar el expediente completo.

Su diseño debe mantenerse:
- mínimo
- útil
- estable
- derivable
- consistente con el canon.