# Modelo lógico — Documento

## Finalidad

`Documento` representa una pieza documental formal del expediente.

Puede originarse en el sistema, incorporarse desde fuera, requerir numeración, firma, emisión, reemplazo o incorporación posterior de una versión firmada.

---

## Tabla principal

### `Documento`

Debe contener, como mínimo a nivel lógico:

- `Id`
- `IdTecnico`, si se usa identidad técnica estable
- referencia al expediente
- tipo de documento
- estado documental principal
- número de documento, cuando exista
- referencia al contexto de numeración, cuando corresponda
- referencia al archivo o versión material vigente
- metadatos básicos de creación, emisión, firma o incorporación

---

## Qué guarda

`Documento` debe guardar la identidad y metadata principal de la pieza documental, incluyendo cuando corresponda:

- tipo documental
- relación con el expediente
- estado principal
- número visible
- condición de pre-numerado o numerado definitivo
- si requiere firma o no
- si fue firmado digitalmente, ológrafamente o no firmado
- referencia a la versión material vigente del documento
- contexto básico de emisión, incorporación o reemplazo

---

## Qué no guarda

`Documento` no debe usarse para guardar:

- toda la trazabilidad del expediente
- todos los eventos documentales como texto libre
- la lógica completa del motor de firma
- la lógica completa del motor de numeración
- el snapshot del expediente
- detalle operativo completo de notificaciones

Tampoco debe confundirse documento con archivo binario puro.

---

## Reglas principales

- Todo `Documento` pertenece a un expediente.
- El `Id` interno no reemplaza al número visible del documento.
- `NumeroDocumento` puede no existir todavía en etapas tempranas.
- La numeración puede ocurrir al firmar o previamente como pre-numeración.
- La numeración visible depende del mecanismo de talonarios y políticas de numeración aplicables.
- Un documento puede existir sin estar firmado.
- Un documento puede imprimirse, firmarse físicamente/ológrafamente y luego incorporarse o reemplazarse por su versión firmada.
- La firma digital o electrónica se resuelve por integración externa; el sistema conserva el resultado documental.
- El documento puede cambiar de versión material sin perder su identidad lógica principal, según las reglas del caso.

---

## Numeración documental

`NumeroDocumento` es la identidad administrativa visible del documento.

No reemplaza:

- el `Id` interno
- la identidad técnica del documento, si existe

La numeración puede provenir de talonario y responder a una política de formato.

Debe contemplarse al menos estas situaciones:

- documento aún sin numerar
- documento pre-numerado
- documento numerado al firmar
- documento numerado dentro de un circuito manual/preimpreso, si aplica

La definición exacta del submodelo de numeración se desarrollará en el bloque correspondiente.

---

## Firma y soporte material

El documento puede atravesar distintas situaciones de firma y materialización, por ejemplo:

- generado y pendiente de firma
- pre-numerado y pendiente de firma
- firmado digitalmente
- impreso para firma física/ológrafa
- re-subido o reemplazado por versión firmada
- incorporado desde fuera ya firmado

Por lo tanto, el modelo debe separar con claridad:

- identidad lógica del documento
- numeración visible
- condición de firma
- versión material o archivo vigente

---

## Relaciones clave

`Documento` se relaciona con:

- `Acta`, como expediente al que pertenece
- `ActaEvento`, cuando existan hechos documentales relevantes
- `Notificacion`, cuando el documento sea objeto notificable o pieza vinculada al proceso de notificación
- talonarios y numeración, cuando el documento requiera número visible
- integración de firma, cuando exista firma digital o electrónica
- storage documental, para resolver el soporte material de sus versiones

---

## Criterio de compactación

`Documento` debe mantenerse como entidad documental principal.

Cuando crezcan demasiado los detalles de archivo, firma, versiones, observaciones o integración, deben separarse en anexos o entidades auxiliares en lugar de sobrecargar la tabla principal.