# 02-integracion-notificaciones.md

## Finalidad

Este archivo describe cómo debe entenderse la integración de notificaciones dentro del ecosistema del sistema de faltas.

No define todavía contratos técnicos detallados ni proveedores finales.

Su objetivo es dejar claro qué parte pertenece al sistema de faltas y qué parte pertenece al mecanismo o proveedor externo de notificación.

---

## Regla principal

La notificación es un proceso transversal del expediente.

En el modelo actual del sistema:

- toda notificación recae sobre un documento del expediente
- toda notificación pertenece a una acta
- cada intento de notificación se dirige a un único destino efectivo
- el sistema debe registrar canal, resultado y acuse cuando corresponda

La integración externa no reemplaza a la entidad `Notificacion`, sino que materializa o informa su ejecución.

---

## Alcance dentro del repo de faltas

Este repositorio debe modelar:

- qué documento se notifica
- a qué expediente pertenece
- por qué canal se intenta notificar
- cuál fue el destino efectivo del intento
- cuál fue el resultado del intento
- si existió acuse y cuál fue su estado resumido
- qué efecto operativo produce la notificación sobre expediente y snapshot

---

## Qué queda fuera de este repo

Queda fuera del repositorio de faltas, según el caso:

- implementación técnica específica del proveedor
- mensajería concreta del canal
- infraestructura de correo, postal o tercero especializado
- UI propia del proveedor externo
- detalles internos de tracking no relevantes para el dominio

---

## Canales previstos

Los canales previstos en el sistema son, al menos:

- domicilio electrónico
- email
- postal
- bluemail
- notificador municipal
- portal ciudadano
- otro canal formal que luego se defina

---

## Regla de integración

La integración debe pensarse mediante puertos o adaptadores según canal o mecanismo.

El sistema de faltas necesita poder:

- emitir o registrar la notificación
- registrar intentos
- registrar destino efectivo
- registrar resultado del intento
- registrar acuse si corresponde
- reflejar el efecto operativo sobre expediente y snapshot

---

## Regla de acuse

El sistema debe distinguir entre:

- canales o circuitos que requieren acuse
- canales o circuitos donde el acuse adicional no es necesario

Si existe acuse, debe poder registrarse:

- su estado
- fecha/hora
- constancia asociada si existe
- efecto resumido sobre la notificación

---

## Relación con el expediente

La notificación puede:

- habilitar nuevos pasos
- bloquear avance hasta resultado o acuse
- provocar reencauce del expediente
- alimentar snapshot y bandejas
- generar reintentos o nuevas actuaciones

---

## Relación con snapshot

El snapshot debe poder reflejar, al menos:

- si existe notificación de acta
- si existe notificación de medida preventiva
- si existe notificación de fallo o acto
- si alguna está en proceso
- si alguna tiene acuse pendiente
- cantidad de reintentos relevantes

---

## Relación con la UI

Las superficies del sistema pueden necesitar:

- iniciar o registrar notificación
- consultar estado resumido
- visualizar intentos
- visualizar acuse
- decidir próximos pasos según resultado notificatorio

---

## Idea clave

La integración de notificaciones no reemplaza la lógica del expediente.

Solo materializa por canal un proceso notificatorio ya modelado por el sistema de faltas, con impacto directo en estado operativo, snapshot y bandejas.

---

## Archivos relacionados

- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
- [Bandeja de notificaciones](../03-bandejas/07-bandeja-notificaciones.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Mapa backend](../04-backend/00-mapa-backend.md)
- [Mapa app notificador](../07-mobile-notificador/00-mapa-app-notificador.md)