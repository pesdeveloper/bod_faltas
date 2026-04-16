# spec/

## Finalidad

Esta carpeta contiene la **spec canónica vigente** del ecosistema del sistema de faltas.

Todo lo que se use como base para:

- arquitectura
- dominio
- bandejas
- reglas
- vistas
- backend
- tasklists
- generación asistida

debe tomar `spec/` como referencia principal.

---

## Cómo está organizada

- `00-overview/` → visión general, stack y decisiones base
- `01-dominio/` → entidades, estados, snapshot y relaciones
- `02-reglas-transversales/` → reglas que cruzan todo el sistema
- `03-bandejas/` → modelo operativo por bandejas
- `04-backend/` → responsabilidades del backend
- `05-web-direccion-faltas/` → spec funcional de la app web
- `06-mobile-inspectores/` → spec funcional de la app de inspectores
- `07-mobile-notificador/` → spec funcional de la app del notificador
- `08-mobile-liberaciones/` → spec funcional de la app de liberaciones
- `09-integraciones/` → integraciones externas
- `10-tasklists/` → hojas de trabajo de implementación
- `11-prompts-y-skills/` → prompts y skills subordinados a esta spec

---

## Regla de precedencia

Si existe contradicción entre esta spec y:

- notas
- chats
- borradores
- prompts
- skills
- tasklists

debe prevalecer `spec/`.

---

## Regla de partición

Cada archivo canónico debe contener solo la mínima información necesaria para responder su pregunta principal.

El contexto secundario, las ampliaciones y los detalles de otros temas deben moverse a archivos especializados y referenciarse, en lugar de repetirse.

La spec debe privilegiar:

- archivos chicos
- responsabilidad clara
- baja duplicación
- referencias explícitas entre archivos

---

## Regla de tamaño práctico

Los archivos de overview, índices y puertas de entrada deben mantenerse especialmente cortos y orientadores.

Los archivos más extensos solo se justifican cuando describen reglas operativas, dominio o comportamientos que no pueden dividirse razonablemente sin perder claridad.

---

## Orden sugerido de lectura inicial

1. [Proyecto y objetivo](00-overview/00-proyecto-y-objetivo.md)
2. [Stack tecnológico](00-overview/01-stack-tecnologico.md)
3. [Decisiones arquitectónicas](00-overview/02-decisiones-arquitectonicas.md)
4. [Mapa de dominio](01-dominio/00-mapa-dominio.md)
5. [Regla del sistema como gestor documental](02-reglas-transversales/00-regla-sistema-como-gestor-documental.md)
6. [Índice maestro de bandejas](03-bandejas/00-indice-maestro-bandejas.md)

---

## Regla de actualización

Cuando cambie una decisión importante, debe actualizarse primero el archivo canónico principal afectado y luego los archivos dependientes.

---

## Estado actual

La prioridad actual es consolidar:

- overview
- dominio
- snapshot
- medidas y liberaciones
- reglas transversales
- bandejas operativas vigentes

## Continuidad de trabajo

Para retomar rápidamente el estado actual de la spec:

- [Estado actual y próximo paso](./99-estado-actual-y-proximo-paso.md)
- [Contexto mínimo para agentes](11-prompts-y-skills/prompts/00-contexto-minimo-para-agentes.md)