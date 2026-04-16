# Documento

## Finalidad

`Documento` representa una pieza documental formal del expediente.

Puede originarse en el sistema, incorporarse desde fuera, requerir numeración, firma, emisión, reemplazo o incorporación posterior de una versión firmada.

---

## Qué representa en el sistema

`Documento` es una unidad documental individual asociada al expediente.

Puede corresponder, según el caso, a:

- actas complementarias
- notificaciones
- resoluciones
- fallos
- constancias
- actuaciones
- presentaciones
- notas u otras piezas admitidas por el sistema

Su identidad no se agota en el archivo material: el documento conserva identidad lógica propia dentro del expediente.

---

## Relación con el expediente

Todo `Documento` pertenece a un expediente.

Puede:

- originarse dentro del sistema
- incorporarse desde fuera
- emitirse
- recibirse
- producir efectos sobre la evolución del caso

---

## Identidad del documento

El documento puede convivir con distintos planos de identidad:

- identidad interna del sistema
- identidad técnica, si se requiere para correlación o integración
- identidad administrativa visible, cuando exista número de documento

Estas identidades no se reemplazan entre sí.

---

## Numeración documental

La numeración visible del documento depende del mecanismo de numeración y talonarios aplicable.

Un documento puede estar:

- sin numerar
- pre-numerado
- numerado al firmar
- numerado bajo modalidad manual o preimpresa, si corresponde

La política de numeración no vive dentro del documento.

---

## Firma y soporte material

El documento puede:

- no requerir firma
- firmarse digitalmente por integración externa
- imprimirse para firma física u ológrafa
- reemplazar el archivo borrador por la versión efectivamente firmada

Por eso, la identidad lógica del documento no debe confundirse con una única representación material.

En esta versión del sistema, cuando el documento se firma, el archivo firmado reemplaza al archivo previo no firmado, sin necesidad de conservar ambas versiones dentro del dominio documental principal.

---

## Reglas principales

- El documento es una pieza formal del expediente.
- La firma afecta al documento y no al expediente en abstracto.
- La numeración documental pertenece al documento o al proceso documental aplicable, no al evento.
- Un documento puede generar eventos relevantes, pero no se reemplazan entre sí.
- Documento y notificación no son equivalentes.
- El motor de firma es externo al sistema de faltas.
- La identidad lógica del documento no debe confundirse con la versión material concreta que lo representa.

---

## Qué no es

`Documento` no es:

- el expediente completo
- la trazabilidad del caso
- la notificación en sí misma
- el motor de firma
- un simple archivo binario sin semántica administrativa

---

## Relaciones clave

`Documento` se relaciona conceptualmente con:

- `Acta`, como expediente al que pertenece
- `ActaEvento`, cuando su generación, numeración, firma, emisión, recepción o reemplazo produce trazabilidad relevante
- `Notificacion`, cuando deba comunicarse formalmente
- `Talonario y numeración`, cuando requiera número visible
- storage documental, como soporte técnico desacoplado mediante `StorageKey`