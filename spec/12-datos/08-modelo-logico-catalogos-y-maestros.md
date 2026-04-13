# Modelo lógico — Catálogos y maestros

## Finalidad

`Catálogos y maestros` representa los valores de referencia estables reutilizados por el modelo lógico.

Su función es concentrar clasificaciones, tipos, estados y configuraciones estructurales del sistema sin mezclar esos valores con datos transaccionales del expediente.

---

## Alcance

Este bloque debe cubrir, según corresponda:

- tipos de acta
- tipos de evento
- tipos de documento
- estados principales
- canales de notificación
- tipos de medida
- motivos o resultados estables
- dependencias, inspectores y sus referencias estructurales
- talonarios y políticas de numeración
- otros maestros con valor semántico transversal

No todo valor del sistema merece catálogo propio.

---

## Qué guarda

Los catálogos y maestros deben guardar valores con estabilidad relativa y reutilización transversal, por ejemplo:

- identificadores semánticos
- descripciones
- clasificaciones principales
- vigencia o estado de uso, cuando corresponda
- relaciones estructurales entre maestros
- configuración base que otros elementos del modelo consumen

---

## Qué no guarda

`Catálogos y maestros` no debe usarse para guardar:

- historial del expediente
- eventos del caso
- snapshot operativo
- configuraciones efímeras de bajo nivel
- texto libre casual sin valor transversal
- detalle transaccional que pertenece al núcleo del expediente

Tampoco conviene crear catálogos innecesarios para valores hiperlocales o descartables.

---

## Reglas principales

- Un catálogo o maestro debe existir solo si agrega reutilización, claridad semántica o control estructural real.
- Los catálogos no deben absorber lógica transaccional del expediente.
- Los estados principales del sistema deben apoyarse en catálogos cuando tengan valor transversal.
- Dependencia e Inspector deben modelarse como maestros o entidades referenciales del dominio, no como simples textos libres.
- Talonario y política de numeración deben modelarse como referencias estructurales reutilizables.
- Cuando corresponda preservar contexto histórico, los maestros referenciales podrán requerir versionado.

---

## Dependencia e Inspector

Dentro de este bloque deben contemplarse las referencias estructurales para:

- `Dependencia`, como unidad organizacional del expediente
- `Inspector`, como actor operativo referencial

Ambos forman parte del dominio y pueden requerir versión aplicable para congelar correctamente el contexto del expediente sin duplicar snapshots textuales.

---

## Talonarios y numeración

Dentro de este bloque debe contemplarse el submodelo referencial de:

- talonarios
- políticas de numeración
- tipos de objeto numerable
- asignación de talonarios
- reglas de formato y alcance

En el caso de las actas, los talonarios deben quedar asociados a dependencias.

La numeración visible no debe confundirse con la identidad interna ni con la identidad técnica de los objetos numerados.

---

## Relaciones clave

`Catálogos y maestros` se relaciona con:

- `Acta`, para clasificación, contexto organizacional y numeración
- `ActaEvento`, para tipos de evento y motivos
- `Documento`, para tipo, estado y numeración
- `Notificacion`, para canales, estados y resultados
- `Medidas y liberaciones`, para tipos y estados aplicables
- versionado de entidades referenciales, cuando corresponda congelar contexto histórico

---

## Criterio de compactación

Este bloque debe mantenerse útil y controlado.

Cuando un maestro crezca demasiado en complejidad o comportamiento, debe evaluarse si sigue siendo un catálogo/maestro o si pasó a ser una entidad de dominio con tratamiento propio dentro del modelo.