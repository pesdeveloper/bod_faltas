# [OVERVIEW] Orquestación de agentes y flujo SDD

## Finalidad

Este archivo define el esquema de trabajo con agentes para este proyecto.

Su objetivo es:

- mantener coherencia global
- reducir desvíos
- evitar rediseños innecesarios
- ordenar la colaboración entre arquitectura, implementación y revisión
- sostener un enfoque **spec-as-source / SDD**

Este archivo no reemplaza la spec funcional ni técnica del proyecto.

Define cómo se trabaja sobre ella.

---

## Enfoque general

Este proyecto se trabaja con enfoque **Spec-Driven Development (SDD)**.

La fuente principal de verdad es el repo, especialmente:

- `spec/`
- archivos de estado y continuidad
- decisiones explícitas ya cerradas en el proyecto

La memoria conversacional o las inferencias nunca deben prevalecer sobre la spec vigente.

---

## Roles

### ChatGPT / Byte

Rol principal:

- arquitecto del proyecto
- mantiene coherencia global
- interpreta la spec
- baja decisiones a lineamientos implementables
- filtra propuestas de otros agentes
- define rumbo y prioridades

Responsabilidades:

- diseñar
- consolidar decisiones
- detectar contradicciones
- proponer estructura
- definir criterios de implementación
- evaluar observaciones críticas

### Cursor

Rol principal:

- implementador
- generador de código
- ejecutor técnico sobre lineamientos ya definidos

Responsabilidades:

- generar código
- respetar la spec y las reglas del repo
- implementar siguiendo las `.cursor/rules`
- no rediseñar por cuenta propia
- no introducir frameworks, patrones o decisiones no pedidas

### Gemini

Rol principal:

- revisor crítico
- auditor de inconsistencias, ambigüedades o grietas

Responsabilidades:

- revisar decisiones y entregables
- detectar contradicciones
- proponer mejoras puntuales
- señalar riesgos o ambigüedades

Regla importante:

- sus propuestas **no se aceptan automáticamente**
- toda observación de Gemini debe ser evaluada por Byte antes de adoptarse

---

## Regla de autoridad

Orden de prioridad:

1. **spec vigente del repo**
2. **archivos de estado/reanudación**
3. **decisiones explícitas del chat actual**
4. **propuestas de agentes**
5. **inferencias o memoria vaga**

Si algo contradice la spec, se prioriza la spec.

---

## Flujo de trabajo acordado

### Paso 1 — Byte interpreta y diseña

Byte:

- lee el estado vigente
- identifica el punto actual real
- define el siguiente paso
- baja lineamientos concretos
- evita rediseño innecesario

### Paso 2 — Cursor implementa

Cursor:

- implementa sobre reglas claras
- genera código o artefactos técnicos
- no redefine arquitectura
- no inventa dominio
- no se aparta de la spec

### Paso 3 — Gemini revisa

Gemini:

- analiza inconsistencias
- detecta grietas
- propone ajustes si realmente mejoran el resultado

### Paso 4 — Byte decide

Byte:

- evalúa observaciones
- acepta o rechaza cambios
- consolida la versión que sigue
- mantiene coherencia del proyecto

---

## Regla de continuidad

No rediseñar desde cero.

Siempre retomar así:

1. revisar `spec/`
2. revisar archivos de estado y continuidad
3. identificar el punto exacto donde quedó el trabajo
4. continuar desde el siguiente paso pendiente
5. tocar solo lo necesario

Priorizar siempre:

- continuidad
- correcciones quirúrgicas
- consistencia
- entregables reutilizables

---

## Regla de uso de la spec

La spec no es documentación decorativa.

La spec es:

- contrato de trabajo
- marco de diseño
- guía de implementación
- referencia para revisión
- base para generación asistida por IA

Toda implementación debe poder justificarse contra la spec.

Si la implementación contradice la spec, debe corregirse la implementación o ajustarse la spec explícitamente, pero nunca dejar la contradicción tácita.

---

## Regla sobre propuestas automáticas

Ningún agente debe introducir por su cuenta:

- rediseños amplios
- nuevas capas no pedidas
- frameworks no acordados
- cambios de stack
- simplificaciones que rompan el dominio
- reinterpretaciones fuertes sin evidencia en la spec

Toda propuesta de cambio relevante debe pasar por evaluación arquitectónica.

---

## Regla de simplicidad

Cuando haya varias formas de avanzar, priorizar:

- la más clara
- la más explícita
- la más controlable
- la que mejor respete la spec
- la que consuma menos contexto y menos complejidad accidental

Evitar sobreingeniería.

---

## Regla para prototipos

Cuando se trabaje en modo prototipo o simulador funcional:

- priorizar validación de lógica de negocio
- no forzar infraestructura definitiva
- usar la implementación más simple posible si cumple el objetivo
- separar claramente prototipo de implementación productiva real

---

## Regla para implementación productiva

Cuando se baje a implementación real:

- usar solo decisiones ya validadas
- evitar arrastrar hacks del prototipo
- consolidar primero reglas técnicas y de arquitectura
- mantener trazabilidad entre spec, implementación y revisión

---

## Resultado esperado de este esquema

Este flujo busca que el proyecto avance con:

- coherencia
- continuidad
- menor desperdicio
- menor desvío arquitectónico
- mejor aprovechamiento de IA
- mejor control humano sobre decisiones importantes

---

## Regla final

Este proyecto no se trabaja con agentes autónomos descontrolados.

Se trabaja con **orquestación guiada por spec**, donde:

- Byte diseña y decide
- Cursor implementa
- Gemini audita
- el repo conserva la verdad del proyecto