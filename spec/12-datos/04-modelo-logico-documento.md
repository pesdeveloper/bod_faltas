# Modelo lógico — Documento

## Finalidad

`Documento` representa la pieza documental lógica del expediente.

Su función es modelar el documento como entidad del caso, separada de:

- su soporte físico concreto
- la firma
- la notificación
- el storage real del archivo

---

## Qué guarda

El modelo de `Documento` debe poder guardar, según corresponda:

- tipo documental
- estado documental
- número visible, si corresponde
- talonario, si corresponde
- tipo de firma requerida
- referencia técnica al archivo vigente mediante `StorageKey`
- hash del archivo vigente
- fecha de generación
- relación con el expediente

---

## Qué no guarda

`Documento` no debe usarse para guardar:

- rutas físicas absolutas
- detalle completo del backend de storage
- lógica interna del motor de firma
- múltiples versiones materiales coexistentes como regla operativa normal
- historia completa del expediente
- acuses o intentos de notificación

---

## Reglas principales

- El documento es una entidad lógica del expediente.
- Su soporte físico se resuelve por `StorageKey`.
- La política de numeración no vive dentro del documento; se resuelve por talonario cuando corresponda.
- El documento puede existir antes de la firma.
- Cuando el documento se firma, el archivo firmado reemplaza al archivo previo no firmado dentro del circuito documental estándar.
- La firma se integra externamente y su trazabilidad mínima vive en entidad separada.

---

## Relación con firma

`Documento` puede requerir firma digital u ológrafa.

La lógica de firma no vive dentro del documento, pero el estado documental sí debe reflejar el efecto de la firma.

---

## Relación con storage

La ubicación real del archivo no forma parte del dominio principal del documento.

Debe resolverse mediante un bloque de storage documental desacoplado, referenciado por `StorageKey`.

---

## Relación con el expediente

Todo documento pertenece a un expediente / acta y puede cumplir un rol específico dentro de él, por ejemplo:

- documento principal
- medida preventiva
- acto administrativo
- notificación del acto
- anexo
- constancia

---

## Idea clave

El sistema no gestiona “archivos sueltos”.

Gestiona documentos lógicos del expediente, cuyo soporte material se resuelve por storage desacoplado y cuya firma se integra externamente cuando corresponde.