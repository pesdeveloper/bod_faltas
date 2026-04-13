# 02-decisiones-arquitectonicas.md

## Finalidad

Este archivo resume las decisiones arquitectónicas base ya adoptadas para el ecosistema del sistema de faltas.

No describe la implementación detallada.  
Fija criterios que deben mantenerse estables en la spec, el backend, las apps y la generación asistida.

---

## Decisiones principales

### 1. Sistema como gestor documental orientado al expediente
El sistema se entiende operativamente como un **gestor documental orientado al expediente**.

- la unidad principal de gestión es el expediente / acta
- las bandejas son bandejas de expedientes
- los documentos, firmas, notificaciones y actuaciones modifican la situación operativa del expediente

---

### 2. `spec/` como fuente principal de verdad
La carpeta `spec/` es la fuente principal de verdad del proyecto.

Debe prevalecer sobre:
- chats
- notas
- borradores
- prompts
- skills
- tasklists

---

### 3. Spec chica, fragmentada y navegable
No se adopta una spec monolítica.

Se priorizan archivos:
- chicos
- específicos
- enlazables
- con responsabilidad clara

Regla práctica:
- un archivo = una responsabilidad principal
- un archivo = una pregunta principal

---

### 4. Repo multiproyecto
El ecosistema se organiza como un **repo multiproyecto** con núcleo común y aplicaciones separadas por superficie operativa.

Superficies previstas:
- web Dirección de Faltas
- mobile inspectores
- mobile notificador
- mobile liberaciones
- backend y procesos compartidos

---

### 5. Backend con servicios explícitos y SQL controlado
Se adopta:
- Java
- Spring Boot
- Spring JDBC
- `NamedParameterJdbcTemplate`
- HikariCP
- SQL explícito

No se adopta ORM pesado como núcleo del sistema.

---

### 6. Documento como pieza central del sistema
Los documentos no son accesorios del flujo.

El sistema debe dar entidad fuerte a:
- documento
- firma
- notificación
- acuse
- efectos documentales sobre el expediente

---

### 7. Bandejas como situación operativa actual
Las bandejas no son lugares a donde primero se envía el expediente para recién producir documentos.

La lógica es:
- el expediente permanece en una bandeja mientras falten documentos o condiciones
- la generación o incorporación de documentos modifica su situación
- cuando se completa el conjunto requerido, se habilita el pase a la siguiente bandeja

---

### 8. Notificación como proceso transversal único
La notificación se trata como una **bandeja transversal única**, con filtros por:
- tipo de pieza
- canal
- estado
- acuse
- vencimiento
- reintento
- resultado

---

### 9. Snapshot operativo derivado y regenerable
El snapshot operativo:
- no es fuente de verdad
- resume situación operativa relevante
- sirve para bandejas, filtros, badges, bloqueos y habilitación de acciones
- debe poder regenerarse

---

### 10. Archivo distinto de cerrada
Archivo y cerrada no son equivalentes.

- archivo = situación operativa de expediente ya resuelto o avanzado, pero no cerrable todavía
- cerrada = expediente completamente concluido

---

### 11. Medidas y liberaciones con impacto real
Las medidas preventivas y los pendientes materiales de liberación/restitución impactan en:
- bloqueos de notificación
- bloqueos de cierre
- bandejas
- snapshot
- generación documental

---

### 12. Motor de firma externo
El motor de firma se trata como un sistema externo, con repositorio propio.

En este repositorio solo se modela:
- integración
- estados esperados
- efectos sobre expediente y documento
- sandbox/testing para primera etapa

---

### 13. Offline en apps de campo
Las apps que operan en terreno deben contemplar conectividad variable o ausente.

Aplica especialmente a:
- inspectores
- notificador
- liberaciones

---

### 14. Contexto histórico fuera de la spec canónica
El material histórico puede conservarse, pero no debe competir con `spec/` como verdad vigente.

---

### 15. Implementación asistida con contexto acotado
La spec y el repo deben permitir que asistentes trabajen leyendo solo lo necesario para cada tarea.

Se priorizan:
- índices maestros
- archivos cortos
- referencias explícitas
- tasklists por frente y por módulo

---

## Resumen operativo

### Núcleo conceptual
- sistema como gestor documental orientado al expediente
- expediente / acta como unidad principal
- documentos como disparadores de cambio operativo

### Organización
- repo multiproyecto
- spec canónica única
- archivos chicos y navegables

### Operatoria
- bandejas como situación actual del expediente
- notificación como proceso transversal único
- snapshot derivado y regenerable
- archivo distinto de cerrada

### Integraciones
- motor de firma externo
- integración sandbox en primera etapa

### Apps móviles
- Flutter
- soporte offline para operación en campo

---

## Archivos relacionados

- [Proyecto y objetivo](00-proyecto-y-objetivo.md)
- [Stack tecnológico](01-stack-tecnologico.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Regla del sistema como gestor documental](../02-reglas-transversales/00-regla-sistema-como-gestor-documental.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)