# Integración con storage documental

## Finalidad

Este archivo define la integración entre el sistema de faltas y el storage físico o lógico utilizado para archivos documentales.

---

## Alcance

Este bloque cubre:

- resolución de `StorageKey`
- almacenamiento y recuperación de archivos
- desacople entre documento lógico y storage físico
- intercambio futuro de tecnología de storage
- soporte para lectura, descarga, reemplazo o incorporación de versiones materiales

No cubre:

- implementación física concreta del proveedor
- detalles de infraestructura de red o filesystem
- contratos técnicos finales de bajo nivel

---

## Regla base

El storage documental debe quedar desacoplado de la identidad lógica del documento.

La ubicación real del archivo no debe formar parte canónica de `Documento`.

La integración debe resolverse mediante `StorageKey` o mecanismo equivalente.

---

## Reglas de integración

- El documento lógico no conoce la ubicación física final del archivo.
- La versión material del documento referencia `StorageKey`.
- El storage resuelve la ubicación real o lógica del archivo.
- El backend debe poder cambiar de storage sin rediseñar el modelo documental.
- La integración debe permitir almacenamiento, recuperación, verificación y reemplazo de archivos cuando corresponda.

---

## Capacidades mínimas esperadas

La integración debe contemplar, según el caso:

- guardar archivo
- recuperar archivo
- resolver metadata técnica mínima
- validar existencia o disponibilidad
- reemplazar o agregar nueva versión material
- permitir lectura o descarga controlada
- soportar migración o cambio de backend de storage, si luego se requiere

---

## Persistencia esperada

La persistencia debe distinguir entre:

- `Documento`
- versión material documental
- resolución de `StorageKey`

Debe poder guardar, según corresponda:

- `StorageKey`
- tipo o proveedor de storage
- ubicación física o lógica
- hash
- tamaño
- tipo de contenido
- estado del objeto almacenado
- metadatos técnicos mínimos

---

## Relación con backend

La integración con storage debe ser consumida principalmente por:

- servicios documentales
- servicios de firma, cuando haya reemplazo o incorporación de versión firmada
- procesos de reproceso o consolidación documental
- backend de lectura o descarga documental

No debe contaminar el dominio con detalles de infraestructura.

---

## Relaciones clave

Este bloque se relaciona con:

- `Documento`
- persistencia documental
- jobs y procesos de consolidación documental
- integraciones auxiliares, cuando el storage deba interoperar con servicios externos

---

## Resultado esperado

Este bloque debe dejar resuelto que el storage documental es intercambiable y que su resolución queda desacoplada del documento lógico mediante `StorageKey`.