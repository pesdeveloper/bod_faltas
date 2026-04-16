# Storage documental

## Finalidad

Este archivo define cómo se resuelve el storage físico o lógico de archivos documentales sin acoplarlo a la identidad lógica del documento.

---

## Regla base

La ubicación real del archivo no debe formar parte canónica de `Documento`.

La persistencia debe resolver el storage mediante `StorageKey` o mecanismo equivalente.

Debe existir un bloque explícito de storage documental, capaz de resolver:

- backend físico o lógico
- política de resolución por sistema/familia/tipo
- objeto almacenado con `StorageKey`, ruta relativa y metadata técnica

---

## Estructura esperada

Debe existir una estructura o bloque equivalente para resolver:

- backend de storage
- política de storage
- objeto almacenado
- `StorageKey`
- ubicación física o lógica
- estado
- metadatos técnicos mínimos

Las referencias documentales deben apoyarse en `StorageKey` y no en rutas físicas embebidas.

---

## Reglas de persistencia

- `Documento` conserva identidad lógica.
- La resolución de storage conserva la ubicación real o intercambiable del archivo.
- El backend no debe depender de una ruta rígida embebida en la identidad documental.
- Debe poder migrarse o intercambiarse el storage sin rediseñar el modelo documental.
- La resolución concreta de storage debe contemplar backends configurables y una política de fallback, sin acoplar el dominio a una unidad, ruta absoluta o proveedor único.

---

## Qué puede guardar

La resolución de storage puede guardar, según corresponda:

- `StorageKey`
- proveedor o tipo de storage
- ubicación física o lógica
- hash o referencia técnica
- estado del objeto almacenado
- metadatos técnicos mínimos

---

## Qué no debe guardar

Este bloque no debe absorber:

- identidad lógica del documento
- numeración documental
- estados administrativos del expediente
- trazabilidad del caso
- semántica documental que pertenece a `Documento`

---

## Relaciones clave

Este bloque se relaciona con:

- persistencia documental, como soporte de archivos
- evidencias del expediente
- acuses u otros soportes técnicos asociados
- backend de lectura o descarga documental, cuando corresponda

---

## Resultado esperado

Este bloque debe dejar resuelto que el storage documental es intercambiable y que su resolución queda desacoplada del documento lógico mediante `StorageKey`.