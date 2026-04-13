# Mapa DDL físico

## Finalidad

Este bloque baja el modelo consolidado a estructura física de base de datos para Informix 12.10.

Prepara:

- tablas físicas
- PK y FK
- constraints
- secuencias
- diagrama relacional
- DDL de creación

---

## Alcance

Incluye:

- convenciones DDL
- tablas principales
- referencias y versionado
- tablas documentales
- notificaciones
- numeración y talonarios
- snapshot y auxiliares
- diagrama relacional
- SQL inicial

No incluye todavía:

- tuning fino
- índices secundarios definitivos
- migraciones avanzadas

---

## Regla base

Este bloque no redefine dominio ni modelo lógico.

Solo baja a físico lo ya consolidado en:

- `spec/01-dominio/`
- `spec/12-datos/`
- `spec/04-backend/`
- `spec/09-integraciones/`

---

## Motor objetivo

- **Informix 12.10**

---

## Estructura esperada

Este bloque se desarrollará en archivos para:

- convenciones DDL Informix
- tablas núcleo
- tablas referenciales y versionadas
- tablas documentales
- tablas de notificación
- tablas de numeración
- tablas derivadas
- diagrama relacional
- SQL de creación

---

## Resultado esperado

Al cerrar este bloque debe existir:

- definición física consistente de tablas
- convenciones DDL estables
- relaciones claras
- diagrama relacional
- SQL inicial de creación