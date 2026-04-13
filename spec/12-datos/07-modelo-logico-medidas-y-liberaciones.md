# Modelo lógico — Medidas y liberaciones

## Finalidad

`Medidas y liberaciones` representa las piezas del expediente vinculadas a imposición, mantenimiento, levantamiento, devolución o cierre de medidas aplicadas dentro del caso.

Su función es modelar estos elementos sin confundirlos con el estado general del expediente ni con la trazabilidad completa del caso.

---

## Tabla o entidades principales

El modelo debe contemplar al menos una entidad principal para medidas aplicadas al expediente y, cuando corresponda, una entidad o mecanismo equivalente para su liberación, levantamiento o resolución posterior.

Estas piezas deben vincularse obligatoriamente al expediente.

---

## Qué guarda

El modelo debe poder guardar, según corresponda:

- tipo de medida
- estado principal de la medida
- fecha de imposición
- fecha de levantamiento, liberación o cierre, si existe
- motivo o resultado principal
- referencias a documentos, actos o eventos vinculados
- observaciones estructuradas mínimas, cuando agreguen valor real
- contexto administrativo necesario para entender su situación actual

---

## Qué no guarda

`Medidas y liberaciones` no debe usarse para guardar:

- todo el historial del expediente
- snapshot completo del caso
- duplicación del contenido completo de documentos
- bitácora técnica de acciones menores
- semántica general de notificación o firma
- texto libre descontrolado sin estructura funcional

---

## Reglas principales

- Toda medida pertenece a un expediente.
- La medida no reemplaza al estado general del expediente.
- La liberación o levantamiento no reemplaza a la medida original: expresa su evolución o resolución posterior.
- Las medidas pueden generar documentos, notificaciones y eventos relevantes, pero no se confunden con ellos.
- La situación de una medida puede impactar en snapshot y bandejas, pero debe conservar identidad propia en el modelo.
- Cuando el dominio lo requiera, debe poder distinguirse entre imposición, vigencia, levantamiento, devolución, liberación o resultado equivalente.

---

## Relación con el expediente

Las medidas y sus liberaciones son piezas subordinadas al expediente.

Pueden afectar la evolución operativa del caso, pero no constituyen la unidad principal de gestión ni deben absorber semántica que pertenece al expediente completo.

---

## Relación con documentos y eventos

Las medidas pueden vincularse con:

- documentos que las disponen, respaldan o resuelven
- eventos que registran su imposición, modificación, levantamiento o resultado
- notificaciones, cuando su existencia o resolución deba comunicarse formalmente

Estas relaciones deben mantenerse explícitas y separadas.

---

## Relaciones clave

`Medidas y liberaciones` se relaciona con:

- `Acta`, como expediente al que pertenecen
- `ActaEvento`, cuando generen trazabilidad relevante
- `Documento`, cuando exista soporte documental asociado
- `Notificacion`, cuando corresponda comunicar su imposición o resolución
- `Snapshot operativo`, cuando su situación impacte en la lectura actual del expediente

---

## Criterio de compactación

El modelo de medidas debe mantenerse simple y explícito.

Cuando un subtipo de medida requiera demasiado detalle, debe resolverse con anexos o satélites específicos en lugar de inflar la entidad principal con columnas heterogéneas para todos los casos.