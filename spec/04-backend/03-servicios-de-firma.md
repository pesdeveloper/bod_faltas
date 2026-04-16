# 03-servicios-de-firma.md

## Finalidad

Este archivo define las responsabilidades del backend respecto de la firma de documentos del expediente.

Su objetivo es dejar claro cómo el backend interactúa con el motor externo de firma y qué parte de la trazabilidad de firma debe persistir el sistema de faltas.

---

## Regla principal

La firma es una integración externa al sistema de faltas.

El backend debe:

- preparar la solicitud de firma cuando corresponda
- reaccionar al resultado de firma
- actualizar el documento y su estado
- persistir trazabilidad mínima suficiente para el dominio

No debe trasladar innecesariamente al dominio toda la complejidad interna del motor de firma.

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- identificar documentos pendientes de firma
- exponerlos para el circuito de firma
- recibir o registrar el resultado de firma
- reflejar el nuevo estado documental
- dejar trazabilidad mínima del acto de firma

---

## Responsabilidades principales

### 1. Preparación de firma
Debe permitir:

- identificar documento firmable
- conocer su tipo de firma requerida
- exponerlo al circuito o integración de firma
- garantizar que el documento tenga el estado adecuado para firmarse

---

### 2. Registro del resultado
Debe permitir registrar, al menos:

- si el documento fue firmado
- cuándo fue firmado
- por qué usuario se produjo la firma, si corresponde
- qué efecto tuvo sobre el estado documental

---

### 3. Impacto sobre el documento vigente
El resultado de la firma debe impactar sobre el documento vigente.

Cuando corresponda:

- el archivo firmado reemplaza al archivo previo no firmado
- la referencia técnica del documento debe seguir resolviéndose por `StorageKey`

---

## Qué no debe hacer

Este bloque no debe absorber:

- la lógica interna del motor de firma
- modelado innecesario de roles de firmante
- ids externos complejos que no aporten valor al dominio
- coexistencia forzada de múltiples archivos del mismo documento si el circuito no la necesita

---

## Relación con otros servicios

### Con documental
La firma afecta estado documental y materialización del documento vigente.

### Con expediente
La firma puede modificar el estado operativo del expediente y habilitar nuevas acciones o bandejas.

### Con notificación
La firma puede ser condición previa para que un documento entre al circuito de notificación.

---

## Idea clave

El sistema de faltas no implementa la firma.

Solo integra la firma, registra su efecto y conserva la trazabilidad mínima necesaria para el expediente y el documento.