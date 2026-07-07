# Contexto para ChatGPT/Byte — Faltas backend

**Propósito:** archivos de contexto para retomar el trabajo en ChatGPT/Byte cuando el chat de Cursor está lento o agotado.

**Fecha de generación:** 2026-07-03 10:45

**Cómo usar:** copiar el contenido de `PROMPT_RETOMAR_CHATGPT.md` al inicio del chat nuevo. Adjuntar los archivos adicionales si el contexto lo requiere.

---

## Estado del repositorio al generar estos archivos

| Campo | Valor |
|-------|-------|
| Repo | `S:\Source\Repos\Bod-Faltas` |
| Rama | `main` |
| HEAD | `39e2a76` — `chore: quitar logs locales de test` |
| Sincronizado con | `origin/main` OK |
| Working tree | Archivos de 8F-5 presentes pero sin commitear (untracked) |

### Commits recientes (git log --oneline -8)

```
39e2a76 (HEAD -> main, origin/main) chore: quitar logs locales de test
716c490 test: agregar pruebas funcionales completas 8F-4C
7036e5d test: agregar pruebas funcionales completas 8F-4C
11f755c chore: limpiar spec historica y sql informix obsoleto
b4fcf0e docs(faltas-api): proponer modelo logico mysql del dominio real
8af7502 docs(faltas-api): preparar cruce decisional pre modelo mysql
```

### Working tree

- M (modificados): 25+ InMemory*Repository (implementan ResettableInMemoryRepository, parte de 8F-5)
- ?? (sin commitear): DevInMemoryResetService.java, ResettableInMemoryRepository.java, DevResetController.java, DevResetResponse.java
- ?? (sin commitear): DevInMemoryResetServiceTest, DevResetControllerIT, DevResetDisabledIT, DevResetGuardrailTest

---

## Archivos en esta carpeta

| Archivo | Contenido |
|---------|-----------|
| README.md | Este archivo |
| 01-estado-actual-faltas.md | Estado general, slices cerrados, tests |
| 02-api-faltas-core-resumen.md | Arquitectura, paquetes, servicios, endpoints |
| 03-dataset-funcional-31-actas.md | Las 31 actas mock funcionales del dataset |
| 04-tests-y-guardrails.md | Build actual, suites de test, guardrails activos |
| 05-proximos-slices.md | 8F-5 (en progreso uncommitted), 8F-6 y pendientes |
| PROMPT_RETOMAR_CHATGPT.md | Prompt listo para pegar en ChatGPT/Byte |

---

## Nota sobre CONTEXTO_ACTUAL_FALTAS.md

Existe en la raíz del repo pero está desactualizado (generado 2026-07-02, build 954 tests / 8F-1).
Build actual: 1419 tests. Usar los archivos de esta carpeta para retomar.
