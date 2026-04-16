# Modelo lógico — Snapshot operativo

## Finalidad

`Snapshot operativo` representa la proyección resumida y regenerable del estado operativo actual del expediente.

Su función es permitir:

- bandejas
- filtros
- búsqueda rápida
- lectura resumida
- decisiones operativas
- estadísticas

sin exigir reconstrucción completa del expediente en cada consulta rutinaria.

---

## Qué guarda

El snapshot debe guardar solo información derivada útil para la operación actual, por ejemplo:

- fecha del acta
- dependencia e inspector proyectados
- bloque actual
- estado procesal actual
- situación administrativa actual
- código de bandeja y visibilidad
- si existe notificación de acta
- si existe notificación de medida preventiva
- si existe notificación de fallo o acto
- si alguna notificación está en proceso o pendiente de acuse
- cantidad de reintentos relevantes
- si existe solicitud de pago voluntario
- monto exigible actual
- si existe pago total
- si existe plan de pagos
- datos resumidos del plan
- si existe gestión externa
- tipo y resultado resumido de gestión externa
- plazos operativos relevantes
- referencias resumidas a último evento, documento o notificación

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
- Existe una única proyección operativa principal del expediente.
- El snapshot no reemplaza a `Acta`.
- El snapshot no reemplaza a `ActaEvento`.
- El snapshot no reemplaza a `Documento` ni a `Notificacion`.
- Puede persistirse por razones operativas.
- Su contenido debe mantenerse compacto, directo y enfocado en lectura actual del expediente.

---

## Relación con bandejas

El snapshot es la base principal de lectura para bandejas y vistas operativas.

Debe poder resumir, de forma confiable, la situación actual del expediente sin exigir reconstrucción completa en cada consulta.

---

## Relación con regeneración

El snapshot debe poder recalcularse cuando cambie el expediente o cuando sea necesario reconstruir consistencia operativa.

El modelo debe asumir que:

- puede quedar desactualizado transitoriamente
- puede requerir reproyección
- no debe contener información imposible de recomponer desde la fuente primaria

No se justifican estructuras técnicas adicionales si no aportan valor operativo concreto.

---

## Idea clave

El snapshot debe mantenerse como una proyección operativa resumida, simple y útil para decisión, no como un submodelo paralelo del expediente.