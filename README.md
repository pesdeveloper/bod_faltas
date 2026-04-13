# repo-faltas

## Qué es

Repositorio canónico del ecosistema del sistema de faltas municipal.

El sistema se entiende como un **gestor documental orientado al expediente**:
- la unidad principal de gestión es el expediente / acta
- los documentos, firmas, notificaciones, acuses y efectos materiales modifican su estado operativo
- las bandejas muestran la situación actual del expediente

---

## Qué incluye

- `spec/` → fuente de verdad canónica vigente
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
- spec fragmentada en archivos chicos

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

La fuente de verdad vigente del proyecto vive en:

- `spec/`

Todo desarrollo, tasklist o generación asistida debe tomar `spec/` como base principal.

---

## Archivos relacionados

- `ruta-relativa.md`
- `../otra-carpeta/archivo.md`

---
## Cómo empezar a leer

1. [spec/README.md](spec/README.md)
2. [Proyecto y objetivo](spec/00-overview/00-proyecto-y-objetivo.md)
3. [Stack tecnológico](spec/00-overview/01-stack-tecnologico.md)
4. [Decisiones arquitectónicas](spec/00-overview/02-decisiones-arquitectonicas.md)
5. [Mapa de dominio](spec/01-dominio/00-mapa-dominio.md)
6. [Índice maestro de bandejas](spec/03-bandejas/00-indice-maestro-bandejas.md)

## Continuidad rápida

Para recuperar rápidamente el estado actual del trabajo:

- [Estado actual y próximo paso](spec/00-overview/99-estado-actual-y-proximo-paso.md)
- [Contexto mínimo para agentes](spec/11-prompts-y-skills/prompts/00-contexto-minimo-para-agentes.md)


