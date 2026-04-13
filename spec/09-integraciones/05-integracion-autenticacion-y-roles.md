# Integración con autenticación y roles

## Finalidad

Este archivo define la integración entre el sistema de faltas y el servicio de identidad, autenticación y autorización del ecosistema.

---

## Alcance

Este bloque cubre:

- autenticación de usuarios del sistema
- consumo de identidad y claims
- integración con roles y permisos
- separación entre identidad externa y usuario/actor del dominio
- relación entre autenticación, backend y apps consumidoras

No cubre:

- implementación interna completa del IdP
- detalle físico de tokens, certificados o storage del IdP
- definición exhaustiva de pantallas de login de clientes

---

## Regla base

La autenticación del sistema de faltas debe apoyarse en un IdP externo al sistema de faltas, aunque forme parte del mismo ecosistema general.

La identidad autenticada no reemplaza automáticamente a las entidades del dominio.

En particular:

- usuario autenticado
- actor del dominio
- inspector
- dependencia
- permisos de operación

no deben confundirse entre sí.

---

## IdP del ecosistema

El sistema de faltas se integra con un IdP propio del ecosistema, implementado con OpenIddict.

Por lo tanto, esta integración debe pensarse contra un proveedor de identidad propio y controlado, no como una dependencia genérica de terceros.

Esto implica que:

- la autenticación local del sistema de faltas depende de ese IdP
- los clientes web y móviles consumen identidad emitida por ese IdP
- los roles, claims y permisos deben alinearse con las necesidades del backend de faltas

---

## Reglas de integración

- El backend confía en identidad emitida por el IdP del ecosistema.
- La autenticación no reemplaza la lógica de permisos del sistema.
- Los roles o claims no deben confundirse con `Inspector`, `Dependencia` ni otras entidades del dominio.
- Debe existir una forma clara de correlacionar identidad autenticada con usuario o actor interno cuando corresponda.
- La integración debe servir tanto para web como para apps móviles del ecosistema.

---

## Autorización y permisos

La integración debe permitir, según corresponda:

- validar identidad autenticada
- consumir roles y claims
- aplicar permisos de operación
- restringir accesos por superficie funcional
- distinguir perfiles de backoffice, inspectores, notificadores, liberaciones u otros actores del ecosistema

La autorización efectiva del backend debe mantenerse explícita y no quedar implícita solo por la autenticación.

---

## Relación con el dominio

La identidad autenticada puede necesitar correlación con:

- usuario interno
- inspector
- dependencia
- perfil funcional
- superficie consumidora

Pero esas relaciones deben resolverse de forma controlada y no automática por nombre, rol o claim aislado.

---

## Persistencia esperada

La persistencia local del sistema, si se requiere, debe guardar solo lo necesario para:

- correlación con identidad externa
- perfil o rol interno
- asignación funcional
- auditoría administrativa mínima
- control de permisos o superficies habilitadas, si luego se justifica

La fuente principal de autenticación sigue estando en el IdP del ecosistema.

---

## Relaciones clave

Este bloque se relaciona con:

- backend común del sistema
- apps web y móviles consumidoras
- `Inspector` y `Dependencia`, cuando haya correlación funcional
- autenticación y roles del ecosistema general
- servicios de seguridad y autorización del backend

---

## Resultado esperado

Este bloque debe dejar resuelto que la autenticación del sistema de faltas se apoya en un IdP propio del ecosistema, mientras que la autorización y la correlación con actores del dominio se resuelven de forma explícita dentro del backend.