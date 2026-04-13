# Storage documental

## Finalidad

Este archivo define cómo se resuelve el storage físico o lógico de archivos documentales sin acoplarlo a la identidad lógica del documento.

---

## Regla base

La ubicación real del archivo no debe formar parte canónica de `Documento`.

La persistencia debe resolver el storage mediante `StorageKey` o mecanismo equivalente.

---

## Estructura esperada

Debe existir una tabla o mecanismo equivalente para resolver:

- `StorageKey`
- tipo o proveedor de storage
- ubicación física o lógica
- estado
- metadatos técnicos mínimos

Las versiones materiales documentales deben referenciar `StorageKey` y no una ruta física embebida.

---

## Reglas de persistencia

- `Documento` conserva identidad lógica.
- La versión material conserva referencia a `StorageKey`.
- La resolución de storage conserva la ubicación real o intercambiable del archivo.
- El backend no debe depender de una ruta rígida embebida en la identidad documental.
- Debe poder migrarse o intercambiarse el storage sin rediseñar el modelo documental.

---

## Qué puede guardar

La resolución de storage puede guardar, según corresponda:

- `StorageKey`
- proveedor o tipo de storage
- ubicación física o lógica
- hash o referencia técnica
- estado del objeto almacenado
- metadatos técnicos mínimos
- marcas de disponibilidad o verificación, si luego se justifican

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

- persistencia documental, como soporte de versiones materiales
- integraciones auxiliares, cuando el storage deba interoperar con servicios externos
- backend de lectura o descarga documental, cuando corresponda

---

## Resultado esperado

Este bloque debe dejar resuelto que el storage documental es intercambiable y que su resolución queda desacoplada del documento lógico mediante `StorageKey`.