# Jobs, workers y procesos

## Finalidad

Este archivo define los procesos de backend que no forman parte del request síncrono normal, pero que son necesarios para sostener consistencia operativa, reproyección, integración y ejecución diferida.

---

## Alcance

Este bloque cubre:

- jobs programados
- workers de procesamiento
- colas o pendientes internas
- reprocesos
- tareas de sincronización o consolidación
- procesos derivados de integraciones externas

No cubre:

- lógica principal de dominio síncrona
- SQL detallado por caso
- definición física final de infraestructura de colas

---

## Regla base

Los jobs y workers no deben redefinir el dominio.

Su función es ejecutar trabajo diferido, repetible o desacoplado, respetando siempre:

- `spec/01-dominio/`
- `spec/12-datos/`
- `08-persistencia-y-sql.md`
- el snapshot como derivado y regenerable
- las integraciones externas como procesos desacoplados del núcleo

---

## Tipos principales de procesos

### 1. Reproyección de snapshot

Procesos destinados a:

- recalcular `ActaSnapshot`
- corregir desfasajes
- reproyectar expedientes afectados por cambios relevantes
- ejecutar regeneraciones masivas cuando sea necesario

### 2. Procesos documentales diferidos

Procesos destinados a:

- completar tareas documentales no resueltas en línea
- registrar resultados de firma externa
- consolidar reemplazo o incorporación de versiones firmadas
- verificar consistencia documental, si luego se requiere

### 3. Procesos de notificación

Procesos destinados a:

- despachar notificaciones
- registrar resultados diferidos
- gestionar reintentos
- consolidar acuses o incidencias provenientes de integraciones externas

### 4. Procesos de numeración y control

Procesos destinados a:

- consolidar reservas o consumos de numeración, si el diseño final lo requiere
- controlar consistencia de talonarios
- detectar incidencias de numeración o duplicidad operativa

### 5. Procesos de integración externa

Procesos destinados a:

- recibir resultados externos
- consolidar derivaciones
- registrar reingresos con efecto material
- reprocesar integraciones fallidas o incompletas

### 6. Procesos de mantenimiento y consistencia

Procesos destinados a:

- detectar desvíos de integridad
- reprocesar pendientes
- reconstruir proyecciones
- ejecutar tareas administrativas de regularización

---

## Reglas de diseño

- Los procesos diferidos deben ser idempotentes cuando el caso lo requiera.
- Deben poder reintentarse sin corromper el expediente.
- Deben dejar trazabilidad suficiente en el backend.
- No deben depender de lógica implícita u oculta fuera de la spec.
- No deben convertir al snapshot en fuente primaria.
- No deben reemplazar validaciones del núcleo transaccional.

---

## Relación con persistencia

Estos procesos consumen y actualizan estructuras definidas en:

- núcleo transaccional
- snapshot y reproyección
- persistencia documental
- numeración y talonarios
- storage documental
- integraciones auxiliares

No deben rediseñar esas estructuras; solo operar sobre ellas.

---

## Relación con jobs síncronos vs diferidos

Debe preferirse procesamiento síncrono cuando:

- la operación sea corta
- el resultado sea necesario para responder al usuario
- no exista riesgo de acoplar demasiado el request

Debe preferirse procesamiento diferido cuando:

- la tarea sea pesada
- dependa de integración externa
- requiera reintentos
- impacte sobre múltiples expedientes
- pueda reprocesarse sin bloquear la operación principal

---

## Resultado esperado

Este bloque debe dejar resuelto qué tipos de trabajo asíncrono o diferido existen en el backend y bajo qué criterios se ejecutan, sin mezclar esa responsabilidad con dominio, persistencia o endpoints.