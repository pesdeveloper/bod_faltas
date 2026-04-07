# [CAPA 09] — SNAPSHOT OPERATIVO CONSOLIDADO

---

# 1. FINALIDAD DE LA CAPA

Esta capa define la **proyección del estado actual del Acta**.

Su objetivo es:

* facilitar consultas rápidas
* alimentar bandejas operativas
* evitar reconstrucción constante desde eventos
* representar la situación actual del trámite

---

# 2. PRINCIPIO CENTRAL

El snapshot es:

## una proyección derivada del modelo

No es:

* fuente primaria de verdad
* reemplazo del dominio
* almacenamiento de historia

---

# 3. ENTIDAD PRINCIPAL

## 3.1 ActaSnapshotOperativo

### Descripción

Representa el estado actual del Acta en forma simplificada.

---

### Responsabilidades

* reflejar la situación actual del trámite
* permitir consultas rápidas
* soportar lógica de bandejas

---

### Contiene

* IdActa
* EtapaOperativaActual
* EstadoGeneral (opcional)
* Banderas operativas
* Referencias rápidas

---

# 4. CONTENIDO DEL SNAPSHOT

## 4.1 EtapaOperativaActual

Representa en qué punto del recorrido se encuentra el Acta.

---

### Ejemplos

* LABRADA
* EN_ENRIQUECIMIENTO
* NOTIFICADA
* EN_ANALISIS
* EN_APELACION
* DERIVADA
* FINALIZADA

---

## 4.2 Banderas operativas

Indicadores booleanos o simples que facilitan consultas.

---

### Ejemplos

* TieneApelacionAbierta
* TieneDeuda
* NotificacionPendiente
* NotificacionFallida
* PagoRegistrado
* DerivadoAApremio
* DerivadoAJuzgado

---

## 4.3 EstadoGeneral (opcional)

Permite representar una síntesis del estado del trámite.

---

### Ejemplos

* ACTIVO
* EN_PROCESO
* FINALIZADO
* ARCHIVADO

---

## 4.4 Referencias rápidas

Permiten acceso directo a datos relevantes sin recorrer eventos.

---

### Ejemplos

* FechaUltimoEvento
* TipoUltimoEvento
* FechaUltimaNotificacion
* ResultadoUltimaNotificacion

---

# 5. ORIGEN DE LOS DATOS

Todos los datos del snapshot se derivan de:

* ActaEvento
* Documento
* Notificacion

---

## Regla

El snapshot:

* no introduce información nueva
* no genera lógica propia
* no almacena verdad independiente

---

# 6. ACTUALIZACIÓN DEL SNAPSHOT

## Modelo

El snapshot se actualiza en base a eventos.

---

### Flujo conceptual

1. ocurre un evento
2. el sistema interpreta el impacto
3. se actualiza el snapshot

---

## Importante

La reconstrucción del estado debe ser posible únicamente con eventos.

---

# 7. REGLAS DE DISEÑO

## 7.1 No duplicar lógica

Evitar:

* cálculos complejos dentro del snapshot
* reglas de negocio duplicadas

---

## 7.2 No almacenar historia

El snapshot no guarda:

* listas de eventos
* detalle documental
* trazabilidad

---

## 7.3 Mantener simplicidad

Debe ser:

* liviano
* rápido
* fácil de consultar

---

## 7.4 Consistencia

Debe poder regenerarse completamente a partir de:

* eventos
* documentos
* notificaciones

---

# 8. USOS PRINCIPALES

El snapshot se utiliza para:

* bandejas de trabajo
* dashboards
* filtros
* reportes operativos
* consultas rápidas

---

# 9. RESULTADO DE LA CAPA

Esta capa permite:

* obtener el estado actual sin recorrer eventos
* mejorar performance
* simplificar consultas
* separar lectura de escritura

---

## Resumen

El snapshot es:

## el cartel de estación actual del Acta

y permite saber:

* dónde está
* qué estado tiene
* qué condiciones aplica

sin leer toda la historia.

---
