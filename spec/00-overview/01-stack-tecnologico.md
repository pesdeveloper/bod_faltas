# 01-stack-tecnologico.md

## Finalidad

Este archivo resume el stack tecnológico canónico del ecosistema del sistema de faltas.

Su objetivo es dejar explícitas las decisiones base de tecnología para:

- backend
- frontend web
- aplicaciones móviles
- base de datos
- storage documental
- infraestructura cercana
- integraciones relevantes

No describe aún el detalle fino de implementación de cada módulo.

---

## Principio general

El stack debe privilegiar:

- simplicidad operativa
- claridad arquitectónica
- control explícito de reglas y consultas
- bajo acoplamiento innecesario
- buena trazabilidad entre spec, código y operación real
- compatibilidad con implementación incremental asistida

---

## Base de datos transaccional

### Tecnología
- Informix 12.10

### Criterio
La base transaccional del sistema se apoya en Informix, respetando las restricciones, convenciones y posibilidades reales del entorno municipal existente.

La persistencia debe modelarse con especial cuidado en:

- trazabilidad
- consultas explícitas
- operaciones transaccionales
- compatibilidad con SQL controlado
- performance razonable para bandejas, snapshots y búsquedas operativas

---

## Backend principal

### Lenguaje
- Java

### Framework base
- Spring Boot

### Acceso a datos
- Spring JDBC
- `NamedParameterJdbcTemplate`

### Pool de conexiones
- HikariCP

### Estilo de persistencia
- SQL explícito
- sin ORM pesado como núcleo del sistema

### Decisiones explícitas
- NO WebFlux como base del core
- NO Netty como decisión principal
- NO ORM pesado como núcleo de negocio

### Criterio
El backend debe priorizar:

- servicios claros
- control explícito de SQL
- transacciones entendibles
- bajo nivel de magia implícita
- facilidad de trazado y depuración
- implementación compatible con el dominio y sus reglas documentales

---

## Frontend web interno

### Tecnología base
- Angular
- TypeScript
- RxJS
- Angular Material

### Despliegue
- detrás de Nginx o Apache como reverse proxy

### Criterio
La aplicación web interna debe enfocarse en:

- bandejas
- detalle de expediente
- gestión documental
- pendientes de firma
- notificaciones
- filtros operativos
- acciones por expediente

Se prioriza una UI clara, orientada a operación administrativa real.

---

## Aplicación móvil de inspectores

### Tecnología base
- Flutter

### Paquetes base previstos
- `dio`
- `freezed`
- `json_serializable`
- `flutter_bloc`
- `isar`
- `flutter_secure_storage`
- `connectivity_plus`
- `geolocator`
- `esc_pos_printer`
- `flutter_appauth`
- `image_picker`
- `file_picker`
- `path_provider`
- `uuid`
- `mockito`

### Criterio
La app de inspectores debe soportar especialmente:

- labrado de actas
- captura de evidencia
- operación offline
- sincronización
- emisión de comprobantes
- uso en campo con conectividad variable

---

## Aplicación móvil de notificador municipal

### Tecnología base
- Flutter

### Paquetes base previstos
Se parte del mismo criterio general que las apps móviles del ecosistema, ajustando luego según necesidades específicas.

### Criterio
La app de notificador debe soportar especialmente:

- cola de diligencias
- notificación en campo
- captura de acuse
- firma dibujada en dispositivo
- evidencia complementaria
- operación offline
- sincronización posterior

---

## Aplicación móvil de entrega material / liberaciones

### Tecnología base
- Flutter

### Paquetes base previstos
Se parte del mismo criterio general que las apps móviles del ecosistema, ajustando luego según necesidades específicas.

### Criterio
La app de liberaciones debe soportar especialmente:

- lectura de QR
- validación de documento de liberación
- entrega efectiva de rodado o restitución documental
- registro del acto material de liberación
- operación offline cuando corresponda
- sincronización posterior

---

## Storage documental

### Estrategia base
- fileserver local
- unidad de red

### Metadata documental
- almacenada en base de datos

### Acceso
- abstracto desde backend

### Criterio
Los documentos no deben acoplarse rígidamente a una única forma física de almacenamiento en el código de negocio.

El backend debe trabajar contra una abstracción de acceso documental, dejando desacoplado:

- storage físico
- metadata
- reglas documentales
- estados del documento

---

## Motor de firma

### Naturaleza
El motor de firma se considera un sistema externo transversal.

### Estado en este repositorio
En esta etapa, el ecosistema de faltas solo modelará:

- integración con el motor de firma
- estados esperados
- efectos sobre expediente y documento
- modo sandbox / testing para emular firma

### Criterio
La implementación real del motor de firma no forma parte del núcleo de este repositorio.

---

## Infraestructura cercana

### Reverse proxy
- Nginx
- Apache

### Scripts e infraestructura complementaria
Se documentarán dentro del repo según necesidades de despliegue, integración y operación.

---

## Integraciones previstas

El ecosistema contempla, al menos, estas integraciones:

- motor de firma
- mecanismos de notificación
- gestión externa
- storage documental
- autenticación y roles

El detalle de cada integración se desarrolla en `spec/09-integraciones/`.

---

## Criterios de consistencia técnica

A nivel general, el stack elegido debe sostener estas decisiones:

- SQL explícito en backend
- dominio controlado desde spec
- separación entre núcleo común y aplicaciones
- integración desacoplada con sistemas externos
- soporte de operación offline en apps móviles que lo requieran
- preparación para trabajo incremental asistido por IA

---

## Qué no define este archivo

Este archivo no define aún:

- estructura de paquetes Java
- diseño de módulos Angular
- arquitectura fina de Flutter por app
- contratos HTTP concretos
- modelo físico de base de datos
- workers concretos
- deployment detallado

Todo eso se desarrolla en los archivos especializados correspondientes.

---

## Archivos relacionados

- [Proyecto y objetivo](00-proyecto-y-objetivo.md)
- [Decisiones arquitectónicas](02-decisiones-arquitectonicas.md)
- [Mapa backend](../04-backend/00-mapa-backend.md)
- [Mapa de integraciones](../09-integraciones/00-mapa-integraciones.md)
- [Integración con motor de firma](../09-integraciones/01-integracion-motor-firma.md)