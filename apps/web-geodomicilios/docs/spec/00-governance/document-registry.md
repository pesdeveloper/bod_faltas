# Registro documental

> **Estado documental:** NORMATIVE
> **Alcance:** spec GeoDomicilios completa.
> Este registro es la fuente unica de clasificacion de todos los documentos de esta spec.

---

## 1. Tabla de documentos

| Path | Clasificacion | Estado | Descripcion |
|---|---|---|---|
| `README.md` | NORMATIVE | VIGENTE | Entry point, autoridad, guardrails y orden de lectura |
| `00-governance/document-registry.md` | NORMATIVE | VIGENTE | Este registro |
| `00-governance/glossary.md` | NORMATIVE | VIGENTE | Vocabulario canonico del modulo geo |
| `10-architecture/application-architecture.md` | NORMATIVE | VIGENTE | Estructura de componentes, servicios y NgZone |
| `20-map/layer-catalog.md` | NORMATIVE | VIGENTE | Fuentes MapLibre, capas, zooms y fuentes tipograficas |
| `20-map/theme-tokens.md` | NORMATIVE | VIGENTE | Tokens de color dia/noche y reglas de auto-tema |
| `30-geocoding/geocoding-contracts.md` | NORMATIVE | VIGENTE | Modos de busqueda, precision y validacion de alturas |
| `40-api/geo-api-contracts.md` | NORMATIVE | VIGENTE | 12 endpoints del backend, DTOs y reglas de matching |
| `50-ux/interface-guidelines.md` | NORMATIVE | VIGENTE | Guias de interfaz: buscador, mapa, comercios, inspector |
| `60-infrastructure/martin-setup.md` | SUPPORTING_CURRENT | VIGENTE | Instalacion y configuracion de Martin tile server |
| `60-infrastructure/postgis-local-setup.md` | SUPPORTING_CURRENT | VIGENTE | PostgreSQL/PostGIS local, restore del backup, objetos geo |

---

## 2. Clasificaciones

| Clasificacion | Descripcion |
|---|---|
| NORMATIVE | Define contratos, arquitectura o reglas visuales vigentes. Ante contradiccion, prevalece sobre evidencia del codigo. |
| SUPPORTING_CURRENT | Documenta el entorno y la infraestructura de soporte. No es autoridad sobre contratos funcionales. |

---

## 3. Regla de actualizacion

Todo cambio funcional en la feature (nueva capa, nuevo endpoint, cambio de tema, nuevo modo de geocodificacion)
debe actualizar primero el documento normativo correspondiente de esta spec antes de considerarse vigente en el codigo.
