# [CANONICO] BANDEJA — ACTAS LABRADAS / REVISIÓN INICIAL

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de actas labradas en revisión inicial, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`

---

# 1. Nombre de la bandeja

**Actas Labradas / Revisión Inicial**

---

# 2. Objetivo

Mostrar las actas recién labradas que ya se encuentran disponibles en el sistema para su primera toma operativa por parte de la Dirección de Faltas.

Esta bandeja representa el primer punto de revisión administrativa real del caso dentro del circuito interno.

Aquí se decide el primer encuadre operativo del acta, por ejemplo:

- si debe pasar a enriquecimiento
- si puede pasar directamente a notificación
- si requiere observación o revisión inicial
- si excepcionalmente corresponde una anulación fundada
- si corresponde alguna otra medida excepcional

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- tipo o naturaleza principal del acta
- estado actual
- último hito relevante
- datos mínimos identificatorios del caso
- alertas básicas si existieran

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con:

- `EstadoActa = LABRADA`

y que todavía no tengan una transición posterior dominante como:

- enriquecimiento iniciado
- paso a notificación
- paralización
- anulación
- cierre
- derivación posterior relevante

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera que la Dirección de Faltas tome las actas labradas y resuelva su primer movimiento operativo.

Normalmente debería decidirse si:

- pasan a enriquecimiento
- pasan directamente a notificación del acta
- requieren observación o revisión inicial
- requieren incorporación de alguna evidencia o documentación adicional
- deben paralizarse, si excepcionalmente correspondiera
- deben anularse, de forma excepcional y fundada

---

# 6. Regla sobre el inspector

El inspector labra el acta, pero no opera esta bandeja como parte del flujo administrativo principal.

Una vez labrada, el acta queda disponible en esta bandeja para su tratamiento por la Dirección de Faltas.

El inspector puede eventualmente contar con una vista propia de consulta de las actas realizadas, pero no con acciones de avance sobre el flujo principal desde esa vista.

---

# 7. Regla sobre anulación

La anulación no debe entenderse como una acción ordinaria de esta bandeja.

Solo debería utilizarse de manera excepcional, cuando exista causa fundada, por ejemplo:

- error material relevante en el acta
- defecto grave detectado en la revisión
- exposición posterior del inspector ante Dirección de Faltas explicando el problema
- otra causa válida reconocida por el proceso

Por lo tanto, debe tratarse como una salida excepcional y no como acción normal de rutina.

---

# 8. Regla de competencia

La competencia del caso ya queda determinada desde el labrado del acta, en función de su naturaleza o tipo principal, por ejemplo:

- tránsito
- ordenanza contravencional
- comercio
- sustancias alimenticias
- otra materia competente

Por lo tanto, la bandeja no contempla una acción de reasignación operativa entre áreas como parte del flujo normal.

Lo que sí debe mostrarse es la competencia o materia principal del caso, ya definida desde origen.

---

# 9. Acciones posibles

## 9.1 Pasar a enriquecimiento
- **Evento o proceso que dispara:** `ACTA_ENRIQUECIMIENTO_INICIADO`
- **Resultado esperado:** el caso pasa a trabajo activo de enriquecimiento
- **Destino:** `Enriquecimiento` / D2

## 9.2 Pasar directamente a notificación del acta
- **Evento o proceso que dispara:** a validar con el área si corresponde modelarlo como evento explícito o como transición derivada del estado
- **Resultado esperado:** el caso queda listo para iniciar la notificación del acta sin etapa previa dominante de enriquecimiento
- **Destino:** `Pendiente de notificación del acta` / D3

## 9.3 Observar acta / marcar para revisión inicial
- **Evento o proceso que dispara:** `ACTA_OBSERVADA`
- **Resultado esperado:** el caso queda marcado para revisión, control o tratamiento inicial
- **Destino:** normalmente sigue en esta misma bandeja o en un subconjunto/filtro interno de revisión, a validar con el área

## 9.4 Corregir acta
- **Evento o proceso que dispara:** `ACTA_CORREGIDA`
- **Resultado esperado:** se ajustan datos del acta sin cambiar necesariamente de etapa
- **Destino:** sigue en `Actas Labradas / Revisión Inicial`, salvo nueva decisión posterior

## 9.5 Incorporar evidencia
- **Evento o proceso que dispara:** `EVIDENCIA_INCORPORADA`
- **Resultado esperado:** el caso suma evidencia relevante en esta etapa temprana
- **Destino:** sigue en la misma bandeja, salvo nueva decisión posterior

## 9.6 Incorporar documento inicial
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO`
- **Resultado esperado:** se agrega documentación relevante para la revisión inicial
- **Destino:** sigue en la misma bandeja, salvo nueva decisión posterior

## 9.7 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso deja el circuito activo normal
- **Destino:** `Paralizadas`

## 9.8 Anular excepcionalmente
- **Evento o proceso que dispara:** `ACTA_ANULADA`
- **Resultado esperado:** el caso queda invalidado y fuera del circuito normal
- **Destino:** `Cerradas`

## 9.9 Archivar excepcionalmente
- **Evento o proceso que dispara:** `ARCHIVO_DISPUESTO`
- **Resultado esperado:** el caso cierra pase a cerradas
- **Destino:** `Cerradas`
- **Observación:** validar con el área si realmente esto puede ocurrir desde esta bandeja o si debe eliminarse

---

# 10. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- un caso observado puede seguir visible aquí o requerir sub-bandeja/filtro específico
- algunas actas podrían pasar casi directamente a notificación
- otras podrían requerir enriquecimiento sí o sí
- debe validarse si archivar desde aquí es real o demasiado excepcional
- debe validarse si la corrección la hace directamente el operador o si solo deja asentada la necesidad de corrección
- debe validarse si “pasar directo a notificación” es normal para ciertos tipos de acta y excepcional para otros

---

# 11. Mini diagrama simple

```mermaid
flowchart TD
  B[Actas Labradas<br/>Revisión Inicial]

  A1[Pasar a enriquecimiento]
  A2[Pasar a notificación]
  A3[Observar / revisión]
  A4[Corregir]
  A5[Incorporar evidencia]
  A6[Incorporar documento]
  A7[Paralizar]
  A8[Anular]
  A9[Archivar]

  D2[Enriquecimiento]
  D3[Pendiente de notificación<br/>del acta]
  P[Paralizadas]
  C[Cerradas]

  B --> A1 --> D2
  B --> A2 --> D3
  B --> A3 --> B
  B --> A4 --> B
  B --> A5 --> B
  B --> A6 --> B
  B --> A7 --> P
  B --> A8 --> C
  B --> A9 --> C
  ```
  