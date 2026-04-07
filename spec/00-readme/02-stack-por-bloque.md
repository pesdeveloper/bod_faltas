# [CANONICO] STACK TECNOLÓGICO POR BLOQUE DEL SISTEMA

# Estado
Canónico

# Última actualización
2026-04-06

# Objetivo
Definir el stack tecnológico oficial por bloque del sistema de faltas municipal, en función de su implementabilidad real, el contexto técnico existente y el enfoque spec-as-source.

---

# 1. Principios generales

- El stack se define **por bloque del sistema**, no por capa conceptual.
- Se prioriza implementabilidad real sobre complejidad innecesaria.
- Se evita introducir tecnologías que no aporten ventaja concreta al problema.
- Se favorece arquitectura clara, trazable y compatible con generación asistida por IA.
- La simplicidad operativa es un criterio arquitectónico principal.

---

# 2. Base de datos transaccional principal

## Tecnología
- Informix 12.10

## Acceso desde backend
- Spring JDBC
- NamedParameterJdbcTemplate
- HikariCP

## Criterio
- SQL explícito como estrategia principal de persistencia
- evitar ORM pesado como núcleo del sistema

---

# 3. Backend principal del sistema de faltas

## Tecnología
- Java
- Spring Boot

## Criterios de implementación
- arquitectura modular por bloques funcionales
- API HTTP JSON
- transacciones clásicas orientadas a casos de uso
- persistencia con Spring JDBC
- pool de conexiones con HikariCP

## Decisión importante
- WebFlux se descarta como stack base del sistema
- Netty se descarta como decisión principal del backend core
- se prioriza backend transaccional clásico por simplicidad, trazabilidad y mejor encaje con Informix + JDBC

---

# 4. Frontend web interno del sistema

## Tecnología
- Angular (última versión estable)
- TypeScript
- RxJS
- Angular Material

## Criterios
- formularios reactivos
- componentes reutilizables enterprise
- soporte completo para operación administrativa
- D1 (alta de acta) debe poder realizarse también desde web

## Despliegue
- frontend compilado como sitio estático
- publicado detrás de Nginx o Apache como reverse proxy

---

# 5. Aplicación móvil / PDA para inspectores

## Tecnología
- Flutter
- dio
- freezed
- json_serializable
- flutter_bloc (Cubit + Bloc)
- isar
- flutter_secure_storage
- connectivity_plus
- geolocator
- esc_pos_printer
- flutter_appauth
- image_picker
- file_picker
- path_provider
- uuid

## Testing
- mockito

## Criterios
- enfoque offline-first
- persistencia local operativa
- almacenamiento de borradores
- cola de sincronización
- captura de evidencias
- geolocalización
- impresión en campo cuando aplique
- autenticación segura
- soporte de creación de actas D1 desde dispositivos móviles

---

# 6. Storage documental

## Tecnología / infraestructura
- fileserver local
- unidad de red / storage compartido

## Criterios
- los archivos no se guardan dentro de la base de datos
- el backend accede al storage mediante un servicio abstracto
- los metadatos documentales se persisten en Informix

## Metadatos mínimos esperables
- StorageKey
- nombre físico
- hash
- tamaño
- mime type
- ubicación lógica
- fecha de carga

---

# 7. Snapshot engine

## Ubicación arquitectónica
- módulo interno del backend principal

## Tecnología
- Java
- Spring Boot
- Spring JDBC
- Informix

## Criterios
- snapshot derivado a partir de eventos procesales
- no es fuente de verdad
- sirve para bandejas, queries y lecturas rápidas
- debe ser regenerable

---

# 8. Integración económica

## Ubicación arquitectónica
- módulo de integración desacoplado del dominio central de faltas

## Tecnología
- Java
- Spring Boot
- Spring JDBC
- Informix

## Criterios
- el económico no forma parte del dominio central
- se resuelve por integración
- se usan contratos explícitos con el sistema económico municipal
- la persistencia o consulta depende del mecanismo real disponible en entorno

---

# 9. Exposición web e infraestructura

## Tecnología
- Nginx o Apache como reverse proxy

## Criterios
- publicación del frontend web
- proxy hacia backend
- control de TLS, headers, rutas y exposición segura

---

# 10. Tecnologías descartadas o no adoptadas como base

## Descartadas como stack principal
- Spring WebFlux
- Netty como base obligatoria del core
- ORM pesado como fuente principal de persistencia

## Motivo
- agregan complejidad innecesaria para un sistema principalmente transaccional, documental, administrativo y orientado a SQL explícito

---

# 11. Resumen ejecutivo

## Core transaccional
- Java
- Spring Boot
- Spring JDBC
- HikariCP
- Informix 12.10

## Frontend web interno
- Angular
- Angular Material
- TypeScript
- RxJS

## Móvil / PDA
- Flutter
- dio
- freezed + json_serializable
- flutter_bloc
- isar
- secure storage
- conectividad
- geolocalización
- impresión
- auth OIDC
- captura de archivos e imágenes

## Storage documental
- fileserver local / unidad de red

## Snapshot engine
- módulo interno del backend

## Integración económica
- módulo de integración desacoplado
