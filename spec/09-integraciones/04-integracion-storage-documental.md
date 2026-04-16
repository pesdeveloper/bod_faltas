# 04-integracion-storage-documental.md

## Finalidad

Este archivo define la integración entre el sistema de faltas y el storage físico o lógico utilizado para archivos documentales.

---

## Alcance

Este bloque cubre:

- resolución de `StorageKey`
- almacenamiento y recuperación de archivos
- desacople entre documento lógico y storage físico
- soporte para backends distintos:
  - disco local
  - unidad de red
  - S3
  - Azure Blob
- política de resolución de backend
- fallback por política
- soporte para lectura, descarga, reemplazo o incorporación de archivos

No cubre:

- implementación física concreta del proveedor
- detalles de infraestructura de red o filesystem
- contratos técnicos finales de bajo nivel

---

## Regla base

El storage documental debe quedar desacoplado de la identidad lógica del documento.

La ubicación real del archivo no debe formar parte canónica de `Documento`.

La integración debe resolverse mediante `StorageKey` y un bloque explícito de storage capaz de determinar:

- backend
- política
- ruta relativa
- metadata técnica del objeto almacenado

---

## Reglas de integración

- El documento lógico no conoce la ubicación física final del archivo.
- El expediente y sus satélites deben guardar solo `StorageKey`.
- El storage resuelve la ubicación real o lógica del archivo.
- El backend debe poder cambiar de storage sin rediseñar el modelo documental.
- La integración debe permitir almacenamiento, recuperación, verificación y reemplazo de archivos cuando corresponda.

---

## Política de resolución

La resolución del backend de storage debe permitir, al menos:

1. buscar política específica por:
   - sistema
   - familia
   - tipo de objeto
2. si no existe:
   - usar política general del sistema
3. si tampoco existe:
   - usar backend default

---

## Política de distribución física

La ruta relativa recomendada debe seguir una estructura equivalente a:

`/{sistema}/{familia}/{tipo}/{anio}/{mes}/{bucket}/{ref_negocio}/{storage_key}.{ext}`

Esto debe permitir:

- distribución técnica razonable
- evitar carpetas gigantes
- mantener interpretabilidad humana razonable
- desacoplar dominio de ruta física absoluta

---

## Persistencia esperada

La persistencia debe distinguir entre:

- backend de storage
- política de storage
- objeto almacenado
- `StorageKey`

Debe poder guardar, según corresponda:

- backend resuelto
- sistema, familia y tipo
- año y mes lógicos
- bucket
- referencia de negocio
- nombre lógico
- extensión
- tipo MIME
- tamaño
- hash
- ruta relativa
- estado del objeto almacenado

---

## Relación con backend

La integración con storage debe ser consumida principalmente por:

- servicios documentales
- servicios de firma, cuando haya reemplazo del archivo firmado
- evidencias del acta
- acuses u otros soportes técnicos asociados
- backend de lectura o descarga documental

No debe contaminar el dominio con detalles de infraestructura.

---

## Resultado esperado

Este bloque debe dejar resuelto que el storage documental es configurable, intercambiable y desacoplado del dominio mediante `StorageKey`, política de resolución y metadata técnica del objeto almacenado.