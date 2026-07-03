# repo-faltas

## Qué es

Repositorio canónico del ecosistema del sistema de faltas municipal.

El sistema se entiende como un **gestor documental orientado al expediente**:
- la unidad principal de gestión es el expediente / acta
- los documentos, firmas, notificaciones, acuses y efectos materiales modifican su estado operativo
- las bandejas muestran la situación actual del expediente

---

## Qué incluye

- `backend/api-faltas-core/docs/spec-as-source/` → spec viva operativa (estados, eventos, comandos, bandejas, API, tests)
- `docs/faltas/` → modelo MariaDB final, matriz de proceso y delta
- `apps/` → aplicaciones por superficie operativa
- `backend/` → backend y procesos compartidos
- `shared/` → contratos y recursos compartidos
- `infra/` → infraestructura y scripts
- `docs/` → documentación complementaria

---

## Superficies previstas

- web interna para Dirección de Faltas
- app móvil de inspectores
- app móvil de notificador municipal
- app móvil de liberaciones / entregas materiales

---

## Decisión arquitectónica base

Este repositorio se organiza como **repo multiproyecto** con:
- núcleo común de dominio y reglas
- varias aplicaciones consumidoras
- backend compartido
- spec viva fragmentada en archivos chicos bajo `backend/api-faltas-core/docs/spec-as-source/`

No se modela como un conjunto de soluciones inconexas.

---

## Motor de firma

El motor de firma se considera un **sistema externo** con repositorio propio.

En este repositorio solo se modela:
- la integración con el motor de firma
- los estados y efectos esperados
- un modo sandbox / testing para emular firma durante las primeras etapas

---

## Regla de precedencia

Las fuentes de verdad vigentes del proyecto son:

- `backend/api-faltas-core/docs/spec-as-source/` → spec viva: estados, bloques, eventos, comandos, bandejas, API, tests
- `docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md` → modelo de datos MariaDB final
- `docs/faltas/MATRIZ_PROCESO_FALTAS_CIERRE_COMPLETA_2026-06-23.md` → matriz de proceso completa
- `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md` → delta in-memory → MariaDB
- `AGENTS.md` → instrucciones para agentes
- `.cursor/rules/` → reglas de dominio y arquitectura

Todo desarrollo, tasklist o generación asistida debe tomar estas fuentes como base principal.

---

## Archivos relacionados

- `ruta-relativa.md`
- `../otra-carpeta/archivo.md`

---
## Cómo empezar a leer

1. [AGENTS.md](AGENTS.md)
2. [Spec viva — estados, bloques, eventos](backend/api-faltas-core/docs/spec-as-source/02-estados-bloques-eventos.md)
3. [Spec viva — comandos, precondiciones, efectos](backend/api-faltas-core/docs/spec-as-source/03-comandos-precondiciones-efectos.md)
4. [Spec viva — snapshot, bandejas, acciones](backend/api-faltas-core/docs/spec-as-source/04-snapshot-bandejas-acciones.md)
5. [Modelo MariaDB final](docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md)
6. [Matriz de proceso completa](docs/faltas/MATRIZ_PROCESO_FALTAS_CIERRE_COMPLETA_2026-06-23.md)

## Continuidad rápida

Para recuperar rápidamente el estado actual del trabajo:

- [Slices pendientes](backend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md)
- [Índice de documentación de trabajo](docs-trabajo/README.md)

### Direccion de Faltas - demo funcional completa lista 2026-06-01

Dataset: 42 actas. Incluye Slice 17A (reactivacion de PARALIZADAS), Slice 19A/19B (ACTA-0029 condena firme + pago condena).

- [Índice de documentación de trabajo](docs-trabajo/README.md)
- [Prompt de continuidad (2026-06-01)](docs-trabajo/prompt-continuidad-faltas-2026-06-01.md)
- [Cierre de restauración funcional](docs-trabajo/cierre-restauracion-funcional-faltas-2026-06-01.md)
- [Checklist guiado de validacion integral](docs-trabajo/checklist-guiado-validacion-integral-faltas-2026-06-01.md)


