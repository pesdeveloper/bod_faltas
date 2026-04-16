# Persistencia documental

## Finalidad

Este archivo define criterios de persistencia y SQL para el bloque documental del sistema.

Su objetivo es dejar claro cómo se persisten documentos, su relación con el expediente, su estado y su referencia técnica de almacenamiento.

---

## Regla base

El documento se persiste como entidad lógica del expediente.

Su soporte material se resuelve de forma desacoplada mediante `StorageKey`.

La persistencia documental debe asumir que, en el circuito estándar, el archivo firmado reemplaza al archivo no firmado previamente almacenado, manteniendo la identidad lógica del documento y su referencia técnica.

---

## Estructura esperada

La persistencia documental debe contemplar, al menos:

- documento lógico
- relación documento-expediente
- metadata mínima de firma
- referencia de storage
- estado documental
- rol del documento dentro del expediente

---

## Reglas de persistencia

- La identidad lógica del documento es distinta de su soporte físico.
- La persistencia no debe depender de rutas absolutas embebidas.
- La referencia técnica del archivo se resuelve por `StorageKey`.
- El archivo firmado reemplaza al archivo previo no firmado cuando el circuito así lo determine.
- La metadata de firma persistida debe ser mínima y suficiente para el dominio.
- La numeración documental, cuando exista, debe resolverse mediante talonario y no por una política embebida en el documento.

---

## Qué no debe absorber

Este bloque no debe absorber:

- lógica interna del motor de firma
- lógica de notificación
- reconstrucción histórica completa del expediente
- resolución física completa del backend de storage

---

## Resultado esperado

La persistencia documental debe sostener documentos lógicos del expediente, desacoplados de su storage físico, con estado documental claro y trazabilidad mínima de firma cuando corresponda.