# Modelo lógico — Snapshot operativo

## Finalidad

`Snapshot operativo` representa la proyección derivada de la situación actual del expediente para lectura rápida, operación y bandejas.

Su función es facilitar consultas operativas sin reemplazar al núcleo transaccional del sistema.

---

## Naturaleza del snapshot

El snapshot es derivado y regenerable.

No constituye fuente primaria de verdad.

Debe poder explicarse a partir de:

- `Acta`
- `ActaEvento`
- `Documento`
- `Notificacion`
- medidas, presentaciones y demás piezas relevantes del expediente

---

## Tabla o proyección principal

### `ActaSnapshot` o equivalente

El modelo puede materializar un snapshot operativo por expediente cuando resulte útil para operación.

Debe contener, como mínimo a nivel lógico:

- referencia única a `Acta`
- estado operativo actual proyectado
- indicadores principales para bandejas y lectura rápida
- referencias resumidas al contexto vigente del expediente
- marcas de actualización o regeneración

La forma física final podrá resolverse como tabla, vista materializada u otra estrategia equivalente.

---

## Qué guarda

El snapshot debe guardar solo información derivada útil para:

- bandejas
- filtros
- búsqueda operativa
- lectura rápida del estado actual
- indicadores visibles de situación del expediente

Puede incluir, según el caso:

- estado actual proyectado
- etapa o bandeja actual
- indicadores de notificación
- indicadores documentales
- indicadores de medidas o presentaciones
- referencias resumidas a dependencia, inspector u otros contextos vigentes
- fechas operativas relevantes derivadas

---

## Qué no guarda

El snapshot no debe usarse para guardar:

- la historia del expediente
- la fuente primaria del estado
- duplicación extensa del contenido documental
- detalle completo de eventos o notificaciones
- reglas de negocio autónomas desligadas del núcleo
- información que no pueda regenerarse o justificarse desde las entidades primarias

Tampoco debe convertirse en una “segunda verdad” del expediente.

---

## Reglas principales

- El snapshot es derivado.
- El snapshot es regenerable.
- El snapshot no reemplaza a `Acta`.
- El snapshot no reemplaza a `ActaEvento`.
- El snapshot no reemplaza a `Documento` ni a `Notificacion`.
- Puede persistirse por razones operativas.
- Su contenido debe mantenerse compacto y enfocado en lectura actual del expediente.

---

## Relación con bandejas

El snapshot puede ser la base principal de lectura para bandejas y vistas operativas.

Por eso debe poder resumir, de forma confiable, la situación actual del expediente sin exigir reconstrucción completa en cada consulta.

Sin embargo, las bandejas no deben redefinir el dominio: deben apoyarse en el snapshot y en el núcleo del modelo.

---

## Relación con regeneración

El snapshot debe poder recalcularse cuando cambie el expediente o cuando sea necesario reconstruir consistencia operativa.

El modelo debe asumir que:

- puede quedar desactualizado transitoriamente
- puede requerir reproyección
- no debe contener información imposible de recomponer desde la fuente primaria

---

## Relaciones clave

`Snapshot operativo` se relaciona con:

- `Acta`, como expediente proyectado
- `ActaEvento`, como fuente principal de trazabilidad
- `Documento`, `Notificacion` y demás piezas del expediente, como fuentes de estado derivado
- bandejas y consultas operativas, como principal consumidor funcional de la proyección

---

## Criterio de compactación

El snapshot debe mantenerse como una proyección operativa resumida.

Cuando una necesidad requiera demasiado detalle histórico o semántico, esa necesidad debe resolverse en el núcleo del modelo o en consultas especializadas, no inflando el snapshot.