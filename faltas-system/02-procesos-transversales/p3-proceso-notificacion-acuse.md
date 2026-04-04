# [P3] PROCESO DE NOTIFICACIÓN / ENVÍO / ACUSE

## Finalidad del proceso

Este proceso transversal tiene como objetivo gestionar la **comunicación formal** de un documento o acto del sistema, asegurando:

- determinación o confirmación del medio de notificación
- validación del medio utilizable
- creación del registro de notificación o envío
- ejecución del envío, despacho o diligenciamiento
- espera del resultado
- gestión de acuse, rechazo, falta de acuse o imposibilidad material
- gestión de reintentos
- trazabilidad mínima reutilizable

Este proceso es reutilizable desde distintos bloques del sistema, por ejemplo:

- D3 (notificación del acta)
- D6 (notificación del acto resolutivo)
- otros envíos o comunicaciones formales futuras

---

## Regla transversal

La lógica de comunicación formal es única y reutilizable.

Ningún bloque funcional debe reimplementar por su cuenta:

- validación del medio
- creación del registro de notificación
- ejecución del envío
- espera de acuse
- tratamiento básico de rechazo, falta de acuse o reintento

Todos deben utilizar este proceso cuando corresponda comunicar formalmente un documento o acto.

---

## Regla de alcance

P3 no produce documentos.

P3 no firma documentos.

P3 no numera documentos.

P3 no decide el efecto jurídico o administrativo final del negocio.

P3 solo se ocupa de:

- comunicar formalmente
- registrar intentos
- esperar resultado
- devolver el resultado al proceso origen

---

## Entrada del proceso

El proceso recibe un **contexto de notificación**, que incluye al menos:

- `OrigenProceso` (ej: D3, D6)
- `IdActa`
- `TipoObjetoANotificar`
  - ACTA
  - ACTO_RESOLUTIVO
  - DOCUMENTO_FORMAL
  - otro
- `IdDocumentoPrincipal`
- `IdDocumentoAccesorio` (nullable)
- `RequiereNotificacionFormal` (bool)
- `MediosPosibles`
  - ELECTRONICO
  - POSTAL
  - NOTIFICADOR
  - otros futuros
- `CanalPreferido` (nullable)
- `DomicilioElectronico` (nullable)
- `DomicilioPostal` (nullable)
- `ReceptorEsperado` / destinatario
- `PermiteCambioMedio` (bool)
- `PermiteReintento` (bool)
- `CantidadMaximaIntentos` (nullable o configurable)
- `PlazoEsperaAcuse`
- `ObservacionesOperativas`
- `Callbacks / hooks de retorno al origen`

---

## Regla clave de entrada

Si:

`RequiereNotificacionFormal = false`

→ no se ejecuta el proceso  
→ salida inmediata según origen

---

## Salidas del proceso

Las salidas son explícitas y siempre devuelven control al proceso origen.

### Notificación perfeccionada
→ `NotificacionPerfeccionada`  
→ volver al `OrigenProceso`

---

### Notificación pendiente
→ `NotificacionPendiente`  
→ volver al `OrigenProceso`

---

### Medio no disponible
→ `MedioNoDisponible`  
→ volver al `OrigenProceso`

---

### Notificación rechazada o no confirmada
→ `NotificacionNoConfirmada`  
→ volver al `OrigenProceso`

---

### Reintento preparado
→ `ReintentoPreparado`  
→ volver al `OrigenProceso`

---

### Archivo o cierre por imposibilidad
→ `ImposibilidadDeNotificacion`  
→ volver al `OrigenProceso`

---

### No aplica notificación
→ `SinNotificacion`  
→ volver al `OrigenProceso`

---

## Flujo del proceso

### 1. Evaluar si requiere notificación formal

Si no requiere:

→ salida: `SinNotificacion`  
→ volver al origen

---

### 2. Determinar o confirmar medio de notificación

El proceso debe:

- determinar el medio aplicable
- o validar el medio recibido desde el proceso origen

Debe evaluar:

- si existe canal electrónico válido
- si existe domicilio postal utilizable
- si existe posibilidad de diligenciamiento por notificador
- si hay restricciones materiales o jurídicas
- si hubo intentos previos
- si corresponde mantener o cambiar el medio

---

### 3. Validar disponibilidad del medio

Si no existe medio utilizable:

→ salida: `MedioNoDisponible`  
→ volver al origen

Si existe medio utilizable:

→ continuar

---

### 4. Crear registro de notificación

Se crea el registro de notificación/envío con datos mínimos:

- `IdNotificacion`
- `IdActa`
- `OrigenProceso`
- `TipoObjetoANotificar`
- `IdDocumentoPrincipal`
- `IdDocumentoAccesorio`
- `Medio`
- `Destino`
- `FechaHoraCreacion`
- `EstadoInicial`
- `NumeroIntento`

---

### 5. Ejecutar envío o diligenciamiento

Según el medio:

#### A. Electrónico
- preparar envío electrónico
- enviar por canal configurado

#### B. Postal
- preparar despacho postal
- registrar salida

#### C. Notificador
- asignar al notificador
- registrar diligenciamiento

---

### 6. Esperar resultado

Una vez emitido el envío o diligenciamiento, el proceso queda a la espera de resultado.

Resultados posibles:

- acuse válido
- pendiente
- rechazo explícito
- sin acuse
- imposibilidad material
- vencimiento del plazo

---

### 7. Resolver resultado del intento

#### A. Acuse válido

Resultado:

→ salida: `NotificacionPerfeccionada`

---

#### B. Pendiente

Resultado:

→ salida: `NotificacionPendiente`

---

#### C. Rechazo / no confirmación / sin acuse

Resultado:

→ salida: `NotificacionNoConfirmada`

---

#### D. Imposibilidad material o jurídica

Resultado:

→ salida: `ImposibilidadDeNotificacion`

---

### 8. Preparar reintento si corresponde

Si el origen o la política del proceso permiten reintentar:

- incrementar contador de intento
- mantener vínculo con el mismo objeto principal
- conservar historial
- cambiar medio si corresponde
- preparar nuevo intento

Resultado:

→ salida: `ReintentoPreparado`

---

## Reglas adicionales

### Reutilización del documento principal

P3 nunca debe regenerar el documento principal a notificar.

Siempre reutiliza:

- el acto
- el documento
- o la pieza formal ya producida por el proceso origen o por P1

---

### Documento accesorio

Si el proceso origen requiere documento accesorio para notificar:

- ese documento debe venir ya resuelto por P1
- o el origen debe invocar P1 antes de entrar a P3

P3 no lo produce.

---

### Trazabilidad completa

Debe quedar registrado:

- qué se notificó
- desde qué proceso origen
- por qué medio
- a qué destino
- cuántos intentos hubo
- cuál fue el resultado de cada intento
- si se cambió el medio
- si hubo cierre por imposibilidad

---

### Reintentos

Los reintentos:

- no deben perder historial
- no deben sobrescribir intentos previos
- pueden reutilizar el mismo documento principal
- pueden cambiar el medio si está permitido

---

### P3 no decide negocio

P3 no decide si el trámite:

- pasa a D4
- pasa a D7
- se archiva
- se paraliza

Eso lo decide el proceso origen al recibir el resultado de P3.

---

## Relación con el sistema

### Notificacion

Debe existir una entidad o registro equivalente que guarde al menos:

- medio
- destino
- estado
- resultado
- acuse
- cantidad de intentos
- timestamps
- vínculo con el documento/acto notificado

---

### ActaEvento

P3 no necesita generar automáticamente todos los eventos internos técnicos del envío.

Solo deben exponerse hacia negocio los eventos o resultados relevantes cuando el proceso origen lo necesite.

---

## Origen y retorno

El proceso siempre debe:

- conocer su `OrigenProceso`
- devolver control explícitamente a ese origen

Ejemplos:

- D3 → invoca P3 para notificar el acta → vuelve a D3
- D6 → invoca P3 para notificar el acto resolutivo → vuelve a D6

---

## Regla clave

Este proceso no decide el flujo del negocio.

Solo produce y administra el resultado de la comunicación formal.

---

## Resumen

P3 centraliza la lógica de:

- validación del medio
- registro de notificación
- ejecución del envío
- espera de acuse
- tratamiento básico de rechazo, falta de acuse o reintento

permitiendo que el resto del sistema reutilice este comportamiento sin duplicar lógica en D3, D6 u otros bloques.