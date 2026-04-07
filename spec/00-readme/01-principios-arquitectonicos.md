# [CANONICO] PRINCIPIOS ARQUITECTÓNICOS

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir los principios arquitectónicos obligatorios del sistema de faltas municipal, de forma que todas las decisiones de modelado, implementación, integración y generación asistida por IA se mantengan coherentes con una misma línea rectora.

# Relación con otros documentos
- `spec/00-readme/00-vision-general.md`
- `spec/00-readme/02-stack-por-bloque.md`
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/`
- `spec/04-snapshot/`
- `spec/06-contratos/`

---

# 1. Principio de simplicidad deliberada

La arquitectura del sistema debe mantener la menor complejidad posible compatible con los requisitos reales del dominio.

No se deben incorporar patrones, entidades, capas o mecanismos que no aporten valor concreto al problema.

La simplicidad no es una limitación accidental: es una decisión arquitectónica central.

---

# 2. Principio de implementabilidad directa

Todo lo que se modele en la especificación debe poder implementarse de forma razonable, trazable y operativa.

No se debe diseñar una arquitectura “bonita en papel” pero costosa de construir, mantener o entender.

El sistema debe estar pensado para ejecución real en contexto municipal, con equipos, tiempos, infraestructuras y restricciones reales.

---

# 3. Principio de Acta como agregado raíz

`Acta` es el agregado raíz del dominio central.

Toda la narrativa principal del sistema se organiza alrededor de la acta.

No existe una entidad superior abstracta separada que reemplace a la acta como centro del modelo.

Esto implica que:

- la identidad administrativa principal vive en `Acta`
- el viaje procesal se reconstruye a partir de la acta
- los eventos relevantes se vinculan a la acta
- los documentos relevantes se vinculan a la acta
- el snapshot operativo se proyecta por acta

---

# 4. Principio de narrativa procesal explícita

El sistema debe poder responder con claridad:

- qué pasó
- cuándo pasó
- quién intervino
- cuál fue el efecto
- qué documento lo respaldó, si corresponde

Para ello, `ActaEvento` debe modelar los hechos procesales reales del trámite.

La narrativa del caso no debe quedar escondida en estados opacos ni en documentos sueltos.

---

# 5. Principio de separación entre proceso y técnica

El sistema debe distinguir de manera estricta entre:

## 5.1 Hechos procesales reales
Son hechos administrativos, jurídicos u operativos del caso.

Ejemplos:
- acta labrada
- acuse recibido
- apelación interpuesta
- archivo dispuesto

Estos hechos sí integran `ActaEvento`.

## 5.2 Eventos técnicos
Son hechos de infraestructura, soporte o integración interna.

Ejemplos:
- timeout
- reintento de job
- webhook recibido
- archivo copiado
- error de red

Estos hechos no integran `ActaEvento`.

Pueden existir en logs o subsistemas técnicos, pero no forman parte de la narrativa procesal de la acta.

---

# 6. Principio de soporte documental unificado

El sistema utiliza un modelo documental único y transversal.

No se deben construir múltiples submodelos documentales especializados por cada tramo del proceso si pueden resolverse mediante:

- tipo documental
- estado documental
- vínculo con acta
- vínculo con evento
- firma asociada, si corresponde

La semántica del documento debe emerger del catálogo y del contexto, no de una proliferación de entidades específicas.

---

# 7. Principio de snapshot derivado

`ActaSnapshotOperativo` es una proyección derivada y regenerable.

No es fuente de verdad.

La fuente de verdad está compuesta por:

- el estado válido de `Acta`
- los eventos procesales registrados
- los documentos relevantes
- las notificaciones y vínculos correspondientes

El snapshot existe para resolver:

- bandejas
- consultas rápidas
- filtros
- búsquedas
- indicadores operativos

Si el snapshot se pierde, debe poder reconstruirse.

---

# 8. Principio de notificación como satélite específico

La notificación es el único satélite persistente específico que se mantiene como entidad propia fuera del núcleo mínimo.

Esto se debe a que la gestión de notificación tiene suficiente complejidad operativa como para justificar tratamiento especializado:

- canales
- envíos
- reenvíos
- acuses
- rechazos
- estado operativo de la gestión

No debe extrapolarse este criterio para crear nuevos satélites si no existe una justificación igual de fuerte.

---

# 9. Principio de absorción de complejidad

Cuando una necesidad pueda modelarse razonablemente mediante:

- evento
- documento
- snapshot
- catálogo
- contrato

debe evitarse crear una nueva entidad satélite o subdominio específico.

La carga de prueba está del lado de quien proponga más complejidad.

---

# 10. Principio de integración, no duplicación, para el económico

El componente económico no forma parte del dominio central de faltas.

El sistema solo debe modelar lo necesario para:

- relacionarse con el sistema económico
- reflejar estados o efectos externos relevantes
- conservar trazabilidad mínima de interoperabilidad

No debe replicarse dentro de faltas un submodelo contable o de deuda completo si ya existe un sistema económico municipal responsable de ello.

---

# 11. Principio de canales múltiples con semántica única

El sistema debe poder operar desde más de un canal de entrada, pero manteniendo una semántica única de dominio.

En particular, D1 debe poder ejecutarse desde:

- frontend web
- aplicación móvil / PDA

Ambos canales deben converger en los mismos contratos y reglas del dominio, aunque la experiencia de usuario, persistencia local o forma de captura sean diferentes.

---

# 12. Principio offline-first para operación de campo

La operación móvil de campo debe diseñarse bajo enfoque offline-first.

Esto implica:

- capacidad de capturar actas sin conectividad
- persistencia local operativa
- cola explícita de sincronización
- reintento posterior
- trazabilidad clara del estado de sincronización

La falta de conectividad no debe impedir el funcionamiento básico del trabajo de campo.

---

# 13. Principio de contratos explícitos

Los límites entre bloques del sistema deben definirse mediante contratos claros.

Esto aplica a:

- API principal
- integración económica
- integración externa
- proceso documental
- notificación
- snapshot
- frontend web
- móvil

No deben existir acoplamientos implícitos sostenidos solo por interpretación humana.

---

# 14. Principio de SQL explícito y controlado

La persistencia del sistema debe priorizar SQL explícito y controlado.

Esto responde a:

- uso de Informix
- necesidad de queries operativas concretas
- control fino sobre bandejas y filtros
- trazabilidad en persistencia
- simplicidad de ejecución

No se adopta ORM pesado como núcleo de persistencia del sistema.

---

# 15. Principio de modularidad funcional

La arquitectura debe organizarse por bloques funcionales o módulos con responsabilidad clara, no solo por capas técnicas genéricas.

Esto favorece:

- comprensión del dominio
- evolución incremental
- testing más preciso
- generación asistida con IA
- menor riesgo de mezcla semántica

---

# 16. Principio de canon único

La verdad vigente del sistema vive en la carpeta `spec/`.

Los documentos ubicados en `historico/` se conservan por trazabilidad, pero no deben prevalecer frente al canon actual.

Si existe contradicción entre ambos, manda `spec/`.

---

# 17. Principio de vocabulario congelado

Una vez definido un término canónico, debe sostenerse consistentemente en:

- documentos
- contratos
- queries
- snapshots
- código
- generación asistida

No deben coexistir múltiples nombres para una misma idea si eso introduce ambigüedad.

---

# 18. Principio IA-first

La especificación debe estar escrita para ser comprendida y utilizada tanto por personas como por asistentes de implementación.

Por ello, los documentos canónicos deben:

- evitar ambigüedad
- evitar duplicación
- expresar reglas con precisión
- separar claramente canon e histórico
- ser estructurados
- ser suficientemente concretos para convertirse en código

---

# 19. Principio de trazabilidad de decisiones

Las decisiones arquitectónicas importantes deben poder rastrearse.

Por ello, el sistema debe conservar:

- documentos canónicos
- ADRs
- contratos
- catálogos
- estructuras de snapshot
- lineamientos de implementación

La arquitectura no debe depender de memoria oral o contexto implícito.

---

# 20. Regla operativa final

Ante cualquier duda de diseño, prevalecen en este orden:

1. simplicidad
2. implementabilidad directa
3. narrativa procesal clara
4. contratos explícitos
5. compatibilidad con el canon
6. compatibilidad con generación asistida por IA
