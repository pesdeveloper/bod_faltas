# Paquete de Continuidad — Slice 8F-11D

> Este paquete es evidencia de continuidad. No reemplaza el repositorio ni el modelo MariaDB final.

---

## Identificación del repositorio

| Campo | Valor |
|---|---|
| Repositorio | `S:\Source\Repos\Bod-Faltas` |
| Módulo | `backend/api-faltas-core` |
| Branch | `main` |
| Commit (referencia) | `39e2a76663d859563f9acd2545210d681d30dc15` — "chore: quitar logs locales de test 2026-07-02" |
| Fecha/hora de generación | 2026-07-05 18:26 (UTC-3) |

---

## Estado de 8F-11D

**Estado: `CERRADO`**

Las seis tablas del slice fueron implementadas y alineadas:

| Tabla MariaDB | Clase Java | Estado |
|---|---|---|
| `fal_tarifario_unidad_faltas` | FalTarifarioUnidadFaltas | ALINEADO |
| `fal_medida_preventiva` | FalMedidaPreventiva | ALINEADO |
| `fal_articulo_medida_preventiva` | FalArticuloMedidaPreventiva + ArticuloMedidaPreventivaId | ALINEADO |
| `fal_acta_articulo_infringido` | FalActaArticuloInfringido | ALINEADO |
| `fal_acta_valorizacion` | FalActaValorizacion | ALINEADO |
| `fal_acta_valorizacion_item` | FalActaValorizacionItem | ALINEADO |

---

## Build

| Indicador | Valor |
|---|---|
| Comando | `mvn test` desde `backend/api-faltas-core` |
| Resultado | **BUILD SUCCESS** |
| Tests totales | **1755** |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Duración | 8.835 s |
| Fecha/hora build | 2026-07-05 18:26:23 (UTC-3) |

---

## Baseline y delta

| Indicador | Valor |
|---|---|
| Baseline pre-8F-11D | 1660 tests (al cierre de 8F-11C) |
| Tests post-8F-11D | **1755 tests** |
| Tests nuevos en 8F-11D | ~95 tests |

---

## Conteo de tablas FALTA_EN_INMEMORY

| Momento | FALTA_EN_INMEMORY |
|---|---|
| Antes de 8F-11D (post 8F-11C) | 27 |
| Después de 8F-11D | **21** |
| Tablas cerradas por 8F-11D | 6 |

---

## Objetivo estratégico

8F-11D implementa el modelo de cálculo económico del acta:
- **Tarifario:** catálogo de valores unitarios por tipo, con control de vigencia y no-superposición.
- **Medidas preventivas:** catálogo con versiones y relación artículo-medida (PK compuesta).
- **Artículos infringidos:** imputación de artículos a actas, sin montos, solo relación normativa.
- **Valorizaciones:** historial con optimistic locking (`versionRow`); una vigente por acta+tipo; estados PRELIMINAR/CONFIRMADA/REEMPLAZADA/ANULADA.
- **Items de valorización:** cálculo congelado por artículo, inmutables post-confirmación.
- **Snapshot:** extendido con valorizacionOperativaId, estadoValorizacionOperativa, tipoValorizacionOperativa, montoOperativoVigente, siMontoConfirmado.

---

## Próximo slice sugerido

**8F-11E — Satélites de acta y catálogos**
- FalActaTransito, FalActaTransitoAlcoholemia, FalActaVehiculo
- FalActaContravencion, FalActaSustanciasAlimenticias, FalActaMedidaPreventiva
- Catálogos: FalVehiculoMarca, FalVehiculoModelo, FalRubroVersion
- Guardrail: tipo_acta determina qué satélite existe

Alternativa: **8F-11F — Fallo, firmeza y apelación** (si se prioriza eje económico).

---

## Instrucciones para usar este paquete

1. Verificar commit `39e2a76663d859563f9acd2545210d681d30dc15` como referencia del punto base.
2. Los archivos nuevos de 8F-11D están en el working tree (sin commitear).
3. Ejecutar `mvn test` desde `backend/api-faltas-core` — debe dar 1755 tests, BUILD SUCCESS.
4. Consultar `02-REPORTE-8F-11D.md` para el reporte completo del slice.
5. Consultar `09-PARIDAD-Y-GAPS-RESTANTES.md` para el estado de paridad y próximos slices.
6. Los archivos fuente relevantes están copiados en la subcarpeta `files/`.

---

## ZIP del paquete

_| Campo | Valor |
|---|---|
| Ruta | `backend/api-faltas-core/docs/spec-as-source/handoff/8F-11D/CONTINUIDAD_8F-11D.zip` |
| Tamaño | 258.043 bytes (~252 KB) |
| SHA-256 | `0786680E4637AD2AC7DB29C7AD49215B2BF9F7F28AF67DDF5D061F879C6CC2E8` |
| Entradas | 59 archivos |
| Generado | 2026-07-05 18:31 (UTC-3) |_

