# Spec GeoDomicilios - Visor de Mapas Malvinas Argentinas

> **Estado documental:** NORMATIVE
> **Alcance:** frontend `apps/web-geodomicilios` + APIs geo consumidas del backend Faltas Core.
> Esta spec cubre arquitectura Angular, mapa MapLibre, estilos visuales, geocodificacion,
> contratos de API y guias de interfaz. No define reglas de dominio de Faltas;
> esas reglas pertenecen a `backend/api-faltas-core/docs/spec-as-source/`.

---

## 1. Proposito

Esta carpeta es la spec canonica del modulo `web-geodomicilios`. Define:

- la arquitectura del frontend Angular;
- el catalogo de capas y fuentes de datos MapLibre;
- los tokens de estilos visuales (temas dia/noche);
- los contratos de geocodificacion;
- los contratos de los endpoints geo del backend;
- las guias de interfaz de usuario;
- la configuracion de infraestructura local (Martin, PostgreSQL/PostGIS).

El codigo, los tests, los handoffs y la historia Git son fuentes de evidencia.
No pueden redefinir silenciosamente esta spec.

---

## 2. Relacion con la spec del backend

La spec de dominio de Faltas vive en:

```
backend/api-faltas-core/docs/spec-as-source/
```

Esta spec puede **consumir** endpoints del backend y **referenciar** modelos de datos
geograficos (parcelas, inmuebles, comercios), pero no puede:

- redefinir estados, eventos, bloques ni transiciones del dominio de Faltas;
- alterar contratos HTTP del backend sin decision explicita en la spec del backend;
- declarar reglas de negocio de actas o faltas.

Ante contradiccion, la spec del backend es normativa sobre el dominio de Faltas.

---

## 3. Estructura tematica

| Carpeta | Contenido |
|---|---|
| `00-governance/` | Registro documental y vocabulario canonico |
| `10-architecture/` | Arbol de componentes, servicios, NgZone, routing |
| `20-map/` | Catalogo de capas, fuentes tipograficas, tokens de color |
| `30-geocoding/` | Modos de busqueda, precision, validacion de alturas |
| `40-api/` | 11 endpoints, DTOs, reglas de matching catastral |
| `50-ux/` | Guias de interfaz: buscador, mapa, comercios, inspector |
| `60-infrastructure/` | Martin, PostgreSQL/PostGIS, restore, objetos geo requeridos |

---

## 4. Orden de lectura

Para reproducir o extender la feature:

1. este `README.md`;
2. `00-governance/glossary.md` - vocabulario;
3. `10-architecture/application-architecture.md` - estructura general;
4. `60-infrastructure/postgis-local-setup.md` - levantar la base de datos;
5. `60-infrastructure/martin-setup.md` - levantar el tile server;
6. `20-map/layer-catalog.md` - capas del mapa;
7. `20-map/theme-tokens.md` - estilos visuales;
8. `30-geocoding/geocoding-contracts.md` - busqueda y geocodificacion;
9. `40-api/geo-api-contracts.md` - endpoints consumidos;
10. `50-ux/interface-guidelines.md` - guias de interfaz.

---

## 5. Regla de precedencia

Ante una contradiccion en esta spec:

1. no elegir una version por fecha, nombre de archivo ni cercania con el codigo;
2. no inventar una conciliacion;
3. registrar y reportar el gap;
4. obtener una decision humana explicita;
5. actualizar primero esta spec canonica;
6. alinear despues codigo, tests y documentos de referencia.

Precedencia entre documentos (de mayor a menor):

1. `00-governance/` (glosario, registro);
2. `10-architecture/` y `40-api/` (contratos estructurales);
3. `20-map/`, `30-geocoding/` (contratos visuales y de busqueda);
4. `50-ux/` (guias de interfaz);
5. `60-infrastructure/` (guias de entorno - SUPPORTING_CURRENT).

---

## 6. Guardrails

- No hardcodear IPs, puertos ni URLs de produccion en codigo versionado. Usar `GEO_CONSTANTS` y variables de entorno.
- No incluir contrasenas, tokens, API keys ni cadenas de conexion completas en ninguna parte de esta spec ni en codigo versionado.
- No mezclar reglas de dominio de Faltas con logica de visualizacion del mapa.
- No inventar capas sin fuente PostGIS confirmada en el catalogo de Martin.
- No declarar `text-font` implicito en capas dinamicas de MapLibre; siempre especificar un unico font name.
- No modificar la spec del backend desde aqui.
- Las credenciales se gestionan exclusivamente a traves del archivo local de configuracion no versionado.
