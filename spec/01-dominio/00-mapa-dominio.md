# 00-mapa-dominio.md

## Finalidad

Este archivo resume el mapa conceptual del dominio del sistema de faltas.

Identifica las piezas principales del núcleo común, su función y su relación general.

No define todavía tablas, tipos de dato ni detalle físico de persistencia.

---

## Unidad principal de gestión

La unidad principal de gestión del sistema es la **Acta / Expediente**.

Todo el sistema se organiza alrededor de su situación documental y procesal.

Las demás piezas del dominio existen para:

- describir su estado
- registrar lo ocurrido
- producir documentación
- reflejar pendientes
- habilitar o bloquear acciones
- determinar su visibilidad en bandejas

---

## Piezas principales del dominio

### 1. Acta
Es la entidad central del sistema.

Representa el expediente principal sobre el cual se registran:

- hechos relevantes
- documentos
- notificaciones
- medidas
- pagos
- presentaciones
- apelaciones
- resultados externos
- bloqueos y condiciones de cierre

La Acta es la unidad que aparece en bandejas y sobre la que operan los usuarios.

---

### 2. ActaEvento
Registra hechos relevantes del expediente.

Debe contener hechos procesales, documentales o materiales con impacto real sobre la Acta.

No debe usarse para logs técnicos ni eventos de infraestructura.

---

### 3. Documento
Representa toda pieza documental relevante del sistema.

Ejemplos:
- acta
- resolución
- fallo
- medida preventiva
- documento de notificación
- documento de liberación
- sentencia
- otras piezas administrativas

Los documentos materializan decisiones y habilitan cambios de situación del expediente.

---

### 4. Notificacion
Representa el proceso de notificación de una pieza documental.

Debe permitir modelar:

- qué se notifica
- por qué canal
- en qué estado se encuentra
- si existe acuse
- si el resultado fue positivo o negativo
- si requiere reintento o decisión manual

La notificación se trata como un proceso transversal.

---

### 5. SnapshotOperativo
Es un resumen derivado y regenerable de la situación operativa de la Acta.

Sirve para:

- bandejas
- filtros
- badges
- bloqueos
- habilitación de acciones
- lectura rápida

No es fuente de verdad.

---

### 6. Medidas y liberaciones
Resume todo lo vinculado a:

- medidas preventivas
- levantamientos
- liberaciones
- restituciones materiales
- pendientes que impiden continuidad o cierre

Impacta especialmente en:

- documentación
- snapshot
- notificación
- archivo
- cierre

---

### 7. Catálogos y estados
El sistema utiliza catálogos y estados controlados para describir:

- situación de la Acta
- tipo de evento
- tipo de documento
- estado documental
- estado de notificación
- canal de notificación
- tipos de medida
- motivos de archivo o cierre
- otros conceptos operativos relevantes

---

## Relación general entre las piezas

La relación principal del dominio puede leerse así:

- la **Acta** es el centro
- la **ActaEvento** registra lo que ocurre sobre ella
- el **Documento** materializa decisiones o actuaciones
- la **Notificacion** gestiona la comunicación formal de documentos
- el **SnapshotOperativo** resume la situación vigente para operar
- las **Medidas y liberaciones** afectan continuidad, archivo y cierre
- los **Catálogos y estados** permiten describir y clasificar todo lo anterior

---

## Regla de operación del dominio

El sistema no debe pensarse como una cadena rígida de pasos abstractos.

Debe pensarse como un expediente cuya situación cambia cuando ocurren hechos tales como:

- se genera un documento
- se firma un documento
- se notifica una pieza
- se registra un acuse
- se incorpora una presentación
- se registra un pago
- se aplica o se levanta una medida
- se registra una liberación material
- se produce un resultado externo
- se resuelve una apelación

Cada uno de esos hechos modifica la situación operativa de la Acta.

---

## Regla de visibilidad en bandejas

Las bandejas muestran **Actas**, no documentos sueltos.

Una Acta aparece en una bandeja según su situación documental y procesal actual, por ejemplo:

- requiere revisión
- requiere producir una pieza
- tiene documentos pendientes de firma
- tiene piezas en notificación
- está pendiente de fallo
- está en archivo
- ya está cerrada

---

## Qué no define este archivo

Este archivo no define todavía:

- tablas
- campos
- tipos de dato
- claves
- índices
- SQL
- DDL
- contratos API
- diseño de vistas

Eso se desarrollará en archivos especializados posteriores.

---

## Archivos relacionados

- [Proyecto y objetivo](../00-overview/00-proyecto-y-objetivo.md)
- [Decisiones arquitectónicas](../00-overview/02-decisiones-arquitectonicas.md)
- [Regla del sistema como gestor documental](../02-reglas-transversales/00-regla-sistema-como-gestor-documental.md)
- [Snapshot operativo](05-snapshot-operativo.md)
- [Medidas y liberaciones](06-medidas-y-liberaciones.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)