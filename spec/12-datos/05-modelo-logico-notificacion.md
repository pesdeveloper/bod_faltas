# Modelo lógico — Notificación

## Finalidad

`Notificación` representa el proceso notificatorio transversal del expediente.

Su función es modelar la comunicación formal de documentos del expediente, sus intentos, acuses y efecto operativo posterior.

---

## Qué guarda

El modelo de notificación debe poder guardar, según corresponda:

- documento notificado
- acta a la que pertenece
- tipo de notificación
- estado actual
- canal principal
- si requiere acuse
- si existe acuse
- estado resumido del acuse
- fecha del acuse, si aplica
- intentos de notificación
- resultado de cada intento
- constancia de acuse, si corresponde

---

## Qué no guarda

`Notificación` no debe usarse para guardar:

- contenido completo del documento notificado
- rutas físicas absolutas
- duplicación innecesaria del expediente
- múltiples destinos estructurados separados si cada intento ya refleja su destino efectivo

---

## Reglas principales

- En este modelo, toda notificación recae sobre un documento del expediente.
- Toda notificación pertenece a una acta.
- Una notificación puede tener múltiples intentos.
- Cada intento se dirige a un único destino efectivo.
- El acuse, si existe, debe quedar trazado como pieza propia del circuito notificatorio.
- El resultado notificatorio debe poder proyectarse en snapshot.

---

## Relación con intentos

Los intentos deben poder registrar, al menos:

- canal utilizado
- destino efectivo
- resultado del intento
- fecha/hora del intento
- fecha/hora del resultado si existe

---

## Relación con acuse

El sistema debe poder distinguir entre:

- notificación emitida
- notificación en proceso
- notificación con acuse pendiente
- notificación con acuse recibido
- notificación fallida o reintentable

El acuse puede tener constancia técnica propia cuando corresponda.

---

## Relación con el expediente

La notificación puede:

- habilitar nuevos pasos
- bloquear avance
- disparar plazos
- motivar reencauce
- alimentar snapshot y bandejas

Pero no reemplaza al expediente ni al documento notificado.

---

## Idea clave

La notificación es un proceso transversal del expediente, siempre apoyado en un documento notificable y trazado mediante intentos y acuses con impacto operativo claro.