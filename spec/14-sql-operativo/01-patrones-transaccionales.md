# [14-SQL-OPERATIVO] 01 - PATRONES TRANSACCIONALES

## Finalidad

Este archivo fija los patrones base de persistencia para:

- secuencias Informix
- altas compuestas
- padre/hijos
- trazabilidad
- proyección
- parte en línea vs parte diferida

No define todo el SQL final.  
Define la forma obligatoria de pensar las operaciones de escritura.

---

## Reglas base

- La transacción sigue el caso de uso real.
- La escritura debe ser explícita, secuenciada y auditable.
- El uso de secuencias es obligatorio y visible.
- Lo diferido debe documentarse expresamente.
- Si falla una parte obligatoria, falla la transacción.
- Lo histórico append-only no se reescribe.
- La proyección mínima puede actualizarse en línea.
- La recomputación total puede diferirse.

---

## Patrón 01 - Alta simple

### Orden base
1. obtener secuencia
2. insertar principal
3. opcionalmente registrar traza
4. confirmar

### Ejemplos
- `FalObservacion`
- entidad maestra simple nueva

---

## Patrón 02 - Alta padre con hijos obligatorios

### Orden base
1. obtener ID del padre
2. insertar padre
3. insertar hijos obligatorios
4. insertar evento o traza si corresponde
5. actualizar o insertar proyección si corresponde
6. confirmar

### Ejemplos
- `FalNotificacion` + `FalNotificacionIntento`
- `FalDocumento` + `FalActaDocumento`
- alta de entidad versionada con su primera versión

---

## Patrón 03 - Alta padre con hijos opcionales

### Ejemplos
- `FalActa` con o sin `FalActaEvidencia`
- `FalDocumento` con o sin `FalDocumentoFirma`
- `FalActa` con o sin satélites según tipo

---

## Patrón 04 - Alta con evento append-only

### Tablas típicas
- entidad principal afectada
- `FalActaEvento`
- opcionalmente `FalActaSnapshot`

### Regla
Si el hecho tiene relevancia procesal o administrativa, no debe resolverse solo con un `UPDATE` silencioso.

---

## Patrón 05 - Alta con observación transversal

### Tabla principal
- `FalObservacion`

### Regla
La observación no reemplaza al evento ni al cambio de estado.

---

## Patrón 06 - Asociación simple entre entidades existentes

### Ejemplos
- vínculo `FalActaDocumento`
- asociación de `StorObjeto` a `FalDocumento`
- asignación de `NumTalonario` a dependencia o inspector

---

## Patrón 07 - Alta documental completa

### Tablas típicas
- `FalDocumento`
- `FalActaDocumento`
- opcionalmente `FalDocumentoFirma`
- opcionalmente `StorObjeto`
- opcionalmente `FalObservacion`
- opcionalmente `FalActaEvento`

### Orden base
1. obtener ID de documento
2. insertar `FalDocumento`
3. insertar `FalActaDocumento`
4. insertar metadatos adicionales presentes
5. registrar observación o evento si corresponde
6. confirmar

---

## Patrón 08 - Alta de notificación con intento inicial

### Tablas típicas
- `FalNotificacion`
- `FalNotificacionIntento`
- opcionalmente `FalNotificacionAcuse`
- opcionalmente `FalActaEvento`

### Regla
Si el caso exige intento inicial, no debe quedar una notificación vacía operativamente.

---

## Patrón 09 - Alta seguida de reproyección resumida

### Tabla principal de proyección
- `FalActaSnapshot`

### Regla
Se actualiza solo la porción mínima necesaria. La recomputación total puede diferirse.

---

## Patrón 10 - Operación append-only con cierre resumido

Registrar un nuevo hecho histórico sin modificar el historial previo, pero actualizar una marca resumida vigente.

---

## Patrón 11 - Corrección controlada de dato mutable

Modificar un dato permitido sin violar el carácter histórico del sistema.

---

## Patrón 12 - Versionado con cierre y nueva versión

### Ejemplos
- `FalDependenciaVersion`
- `FalInspectorVersion`
- `FalAlcoholimetroVersion`
- `RubroComVersion`

### Orden base
1. localizar versión vigente
2. cerrar vigencia previa
3. insertar nueva versión
4. confirmar

---

## Patrón 13 - Inserción con idempotencia técnica controlada

### Casos típicos
- altas con `IdTecnico`
- sincronización móvil
- reintentos de integración

---

## Patrón 14 - Transacción compuesta con parte externa diferida

### Ejemplos
- documento listo para firma externa
- notificación lista para despacho externo
- archivo listo para persistirse en `Stor*`
- solicitud de número o asignación en capa `Num*`

---

## Regla de aplicación

Cada archivo CRUD o de operación concreta debe indicar, al menos:

- tablas involucradas
- secuencias usadas
- orden de ejecución
- parte en línea
- parte diferida
- invariantes protegidas

---
