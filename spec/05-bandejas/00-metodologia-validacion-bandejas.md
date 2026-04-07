# [CANONICO] METODOLOGÍA DE VALIDACIÓN DE BANDEJAS

# Estado
Canónico en elaboración funcional

# Última actualización
2026-04-06

# Propósito
Definir el formato y objetivo de los documentos de validación de bandejas operativas, para discutir con el área de Faltas qué se muestra en cada bandeja, qué acciones humanas son posibles y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/04-snapshot/00-snapshot-engine.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`

---

# 1. Finalidad de esta fase

Esta fase no busca todavía diseñar queries SQL ni contratos técnicos.

Su objetivo es validar operativamente:

- qué bandejas existen realmente
- qué casos entra cada una
- qué información se muestra
- qué acciones humanas se pueden ejecutar
- qué evento o proceso dispara cada acción
- a qué bandeja o etapa va el caso después

Esto permite depurar y ajustar:

- estados
- eventos
- acciones
- procesos
- transiciones
- criterios de snapshot

antes de bajar a diseño más técnico.

---

# 2. Qué es una bandeja

Una bandeja es una vista operativa de casos que comparten una situación de trabajo similar.

Normalmente una bandeja se apoya en:

- `EstadoActa`
- flags del snapshot
- situación notificatoria
- situación documental
- contexto operativo actual

La bandeja no reemplaza al estado, pero lo traduce a una vista de trabajo concreta para el usuario.

---

# 3. Qué debe responder cada documento de bandeja

Cada documento de bandeja debe responder, de forma simple:

1. qué muestra la bandeja
2. qué casos entran
3. qué se espera resolver ahí
4. qué acciones humanas se pueden hacer
5. qué evento o proceso dispara cada acción
6. a dónde va el caso después
7. qué excepciones o desvíos pueden existir

---

# 4. Regla de simplicidad

Cada bandeja debe explicarse de forma breve, concreta y conversable con el área.

No debe convertirse en:
- un documento técnico de backend
- una especificación SQL
- una lista de casos de prueba detallados
- una explicación jurídica extensa

La idea es validar operación real, no infraestructura.

---

# 5. Estructura fija de cada bandeja

Cada documento de bandeja debe tener esta estructura:

## 1. Nombre de la bandeja
## 2. Objetivo
## 3. Qué se muestra
## 4. Qué casos entran
## 5. Qué se espera resolver aquí
## 6. Acciones posibles
## 7. Excepciones
## 8. Mini diagrama simple

---

# 6. Formato de las acciones

Cada acción debe explicarse con este formato:

- **Acción**
- **Evento o proceso que dispara**
- **Resultado esperado**
- **Destino**

El objetivo es que cualquier persona del área pueda leerla y decir:
- sí, esto es correcto
- no, esto falta
- esto no debería poder hacerse
- esto debería llevar a otra bandeja

---

# 7. Regla sobre destinos

El destino de una acción debe expresarse de manera simple, por ejemplo:

- nombre de la bandeja siguiente
- D{n} si ayuda a ubicar la etapa
- sigue en la misma bandeja
- pasa a paralizadas
- pasa a cerradas

No hace falta todavía expresar lógica técnica fina.

---

# 8. Regla sobre excepciones

Cada bandeja puede tener excepciones o caminos especiales, por ejemplo:

- paralizar
- archivar
- anular
- derivar externamente
- volver a revisión
- reabrir

Esas excepciones deben mostrarse si son operativamente relevantes.

---

# 9. Regla sobre mini diagramas

Cada bandeja puede incluir un diagrama Mermaid muy corto y simple.

El diagrama debe mostrar solo:

- bandeja actual
- acciones principales
- destino de cada acción

No debe intentar representar todo el sistema.

---

# 10. Resultado esperado de esta fase

Al finalizar esta fase, el sistema debería tener validado:

- el conjunto real de bandejas
- las acciones permitidas en cada una
- los eventos o procesos que disparan
- las transiciones operativas principales
- los puntos donde pueden requerirse ajustes de estados o catálogos

Esta fase es una herramienta de cierre funcional del diseño antes de pasar a queries, contratos e implementación.