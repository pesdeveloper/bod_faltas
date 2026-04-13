# 01-integracion-motor-firma.md

## Finalidad

Este archivo describe cómo debe entenderse la integración entre el sistema de faltas y el motor de firma.

El motor de firma se considera un sistema externo transversal, con repositorio propio.

En el ecosistema de faltas solo se modela la integración y sus efectos sobre expediente y documentos.

---

## Regla principal

El sistema de faltas no firma documentos por sí mismo.

El sistema de faltas:

- genera o administra documentos del expediente
- solicita o refleja su firma
- reacciona al resultado de firma
- actualiza estado documental y situación operativa del expediente

---

## Alcance dentro del repo de faltas

Este repositorio solo debe modelar:

- qué documentos requieren firma
- qué estado documental tienen antes y después de la firma
- qué efecto produce una firma sobre el expediente
- cómo se representa la integración externa
- cómo funciona un modo sandbox / testing

No debe implementar el motor real completo.

---

## Qué queda fuera de este repo

Quedan fuera del repositorio de faltas, por corresponder al sistema externo de firma:

- cola general del motor de firma
- numeración documental transversal
- estampado PDF
- integración con token, HSM o firma remota
- agente o programa firmador
- UI propia del firmador
- lógica técnica específica del motor

---

## Regla de integración

La integración debe pensarse mediante un puerto o adaptador externo.

A nivel conceptual, el sistema de faltas necesita poder:

- solicitar firma
- consultar o reflejar estado de firma
- recibir confirmación de firmado
- recibir rechazo o error
- actualizar documento, expediente y snapshot según el resultado

---

## Regla de estados mínimos esperados

La integración debe contemplar, al menos, estados o resultados equivalentes a:

- pendiente de firma
- firmado
- rechazado
- error
- cancelado

El detalle final podrá ajustarse cuando se modele el contrato técnico concreto con el motor de firma.

---

## Regla de efecto sobre el expediente

Cuando un documento del expediente pasa a firmado, el sistema de faltas debe poder:

- actualizar el estado del documento
- registrar trazabilidad del hecho
- recalcular o actualizar snapshot
- habilitar el próximo paso operativo, si corresponde
- mover visibilidad del expediente a la bandeja siguiente cuando aplique

---

## Relación con la bandeja de pendientes de firma

La bandeja de pendientes de firma no es una cola del motor de firma.

Es una bandeja de expedientes del sistema de faltas que tienen documentos generados pendientes de firma.

El resultado informado por el motor de firma o por el sandbox es lo que modifica la situación del expediente dentro del sistema de faltas.

---

## Regla de sandbox / testing

Durante las primeras etapas de desarrollo, el sistema debe permitir un modo sandbox o testing para emular la firma sin depender del motor real.

Ese modo debe permitir, al menos:

- marcar un documento como firmado
- disparar los mismos efectos operativos que produciría una firma válida
- probar bandejas, snapshot, notificaciones y flujo documental

El sandbox no debe confundirse con la integración definitiva.

---

## Relación con la UI

La UI del sistema de faltas puede ofrecer, en entorno sandbox o testing, acciones del tipo:

- simular firma
- marcar como firmado en sandbox

Estas acciones deben estar claramente separadas de la operación real.

---

## Relación con backend

El backend de faltas debe concentrarse en:

- exponer el estado documental esperado
- actualizar expediente y snapshot ante resultado de firma
- encapsular la integración con el motor externo
- permitir implementación sandbox y real sin rediseñar el dominio

---

## Idea clave

El motor de firma es externo.  
El sistema de faltas solo administra el efecto documental y operativo de la firma sobre el expediente.

---

## Archivos relacionados

- [Stack tecnológico](../00-overview/01-stack-tecnologico.md)
- [Decisiones arquitectónicas](../00-overview/02-decisiones-arquitectonicas.md)
- [Bandeja de pendientes de firma](../03-bandejas/06-bandeja-pendientes-firma.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Reglas de firma](../02-reglas-transversales/01-reglas-de-firma.md)