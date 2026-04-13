# Persistencia documental

## Finalidad

Este archivo define cómo se persiste `Documento`, sus versiones materiales y su resolución en storage.

---

## Regla base

La persistencia documental debe separar:

- identidad lógica del documento
- numeración visible, cuando exista
- estado documental principal
- condición de firma
- versión material o soporte asociado
- resolución de storage

El archivo no agota la identidad del documento.

---

## Estructura esperada

Debe existir al menos:

- `Documento`
- una tabla auxiliar de versiones materiales o soportes documentales
- una tabla o mecanismo equivalente para resolver `StorageKey`

Y podrán existir tablas auxiliares para:

- firma
- observaciones documentales
- resultados documentales específicos

si luego se justifican.

---

## Reglas de persistencia

- Todo `Documento` pertenece a un expediente.
- Un documento puede nacer sin número visible.
- Un documento puede pre-numerarse.
- Un documento puede numerarse al firmar.
- Un documento puede quedar pendiente de firma.
- Un documento puede firmarse digitalmente por integración externa.
- Un documento puede imprimirse para firma física u ológrafa.
- Un documento puede luego incorporar o reemplazar su versión material firmada sin perder identidad lógica.

---

## Versión material

La persistencia debe permitir distinguir entre:

- documento lógico
- una o más representaciones materiales o archivos asociados

La versión material debe poder guardar, según corresponda:

- `StorageKey`
- hash
- nombre original
- tipo de contenido
- tamaño
- metadatos técnicos mínimos

---

## Storage

La ubicación física o lógica del archivo no debe formar parte canónica de `Documento`.

La persistencia debe resolver el storage mediante `StorageKey`, permitiendo desacoplar:

- identidad documental
- versión material
- ubicación real del archivo

Esto permite intercambiar o migrar el storage sin rediseñar el modelo documental.

---

## Integridad

La persistencia debe asegurar, según corresponda:

- referencia obligatoria del documento al expediente
- coherencia entre documento y versión material asociada
- referencia válida desde versión material a `StorageKey`
- unicidad de identidad interna
- unicidad de identidad técnica, si existe
- coherencia entre numeración visible y contexto de numeración aplicable

---

## Resultado esperado

Este bloque debe dejar resuelto que la persistencia documental conserva la identidad lógica del documento separada de sus versiones materiales y separada también de la ubicación real del archivo en storage.