# Tokens de Tema Visual (Dia / Noche)

> **Estado documental:** NORMATIVE
> **Alcance:** paleta de colores y reglas de auto-tema del mapa.
> Ante contradiccion sobre valores de color o logica de auto-tema, este documento prevalece.

---

## 1. Modos disponibles

| Modo | Valor localStorage | Descripcion |
|---|---|---|
| auto | `auto` | Calcula dia/noche segun hora actual en zona America/Argentina/Buenos_Aires |
| dia | `day` | Fuerza tema claro |
| noche | `night` | Fuerza tema oscuro |

**Clave localStorage**: `malvinas-map-theme`.

**Logica auto-tema**: hora 07:00-18:59 (Argentina) = dia; fuera de ese rango = noche.
Polling cada 5 minutos. Fallback si la API de hora no esta disponible: `prefers-color-scheme`.

---

## 2. Tokens de color

| Token | Dia | Noche | Uso |
|---|---|---|---|
| `bgGeneral` | `#F2F1EC` | `#344A63` | Fondo del mapa |
| `extLine` | `#7E8990` | `#8EA3B5` | Linea de borde del municipio |
| `extLineOpacity` | 0.65 | 0.60 | Opacidad del borde |
| `localityFill` | match por id_loc (ver paleta) | `#3B526B` | Relleno de localidad |
| `localityOutline` | `#A89C90` | `#4A6278` | Borde de localidad |
| `localityLabel` | `#444444` | `#7A9CC0` | Etiqueta de localidad |
| `localityLabelHalo` | `#FFFFFF` | `#1C3248` | Halo de etiqueta de localidad |
| `greenFill` | `#CFE3C4` | `#365E4A` | Espacios verdes |
| `greenFillOpacity` | 0.92 | 0.97 | Opacidad espacios verdes |
| `greenLine` | `#5F8A58` | `#77B98A` | Borde espacios verdes |
| `greenLabel` | `#315B36` | `#D7F2DD` | Etiqueta espacios verdes |
| `greenLabelHalo` | `#F4F5ED` | `#1E362A` | Halo etiqueta espacios verdes |
| `barrioFill` | `#DDDAD4` | `#2E4560` | Relleno de barrio |
| `barrioFillOpacity` | 0.05 | 0.20 | Opacidad barrio |
| `barrioOutline` | `#9A7446` | `#D0A96E` | Borde de barrio |
| `barrioLabel` | `#654722` | `#FFD28A` | Etiqueta de barrio |
| `barrioLabelHalo` | `#F8F7F2` | `#1A2E42` | Halo etiqueta barrio |
| `labelHaloWidth` | 1.7 | 0.5 | Ancho halo etiquetas de calle |
| `blockFill` | `#E6E3DA` | `#415A73` | Relleno de manzana |
| `blockOutline` | `#A8A49C` | `#6A8AA0` | Borde de manzana |
| `blockLabel` | `#222B33` | `#F5F7F9` | Etiqueta de manzana |
| `blockLabelHalo` | `#F8F7F2` | `#243345` | Halo etiqueta manzana |
| `parcelFill` | `#E6E3DA` | `#415A73` | Relleno de parcela |
| `parcelLine` | `#718896` | `#A7B5C3` | Borde de parcela |
| `parcelLabel` | `#455563` | `#D7E1EA` | Etiqueta parcela corta |
| `parcelLabelHalo` | `#F8F7F2` | `#26374B` | Halo etiqueta parcela corta |
| `parcelFullLabel` | `#25313A` | `#F2F6FA` | Etiqueta parcela completa |
| `parcelFullLabelHalo` | `#FFFFFF` | `#16212D` | Halo etiqueta parcela completa |
| `parcelSelFill` | `#2563EB` | `#FFB000` | Relleno de parcela seleccionada (preexistente en style) |
| `parcelSelFillOp` | 0.18 | 0.25 | Opacidad de parcela seleccionada |
| `parcelSelLine` | `#1D4ED8` | `#FFB000` | Borde de parcela seleccionada |
| `casingPrimary` | `#B5B0A8` | `#1A2A3B` | Casing calle primaria |
| `fillPrimary` | `#EDE5CE` | `#26364A` | Relleno calle primaria |
| `casingSecondary` | `#BFBAB2` | `#1A2A3B` | Casing calle secundaria |
| `fillSecondary` | `#F0EBE3` | `#26364A` | Relleno calle secundaria |
| `casingLocal` | `#EDE9E4` | `#1A2A3B` | Casing calle local |
| `fillLocal` | `#FCFBF8` | `#26364A` | Relleno calle local |
| `pedestrianFill` | `#E8E4DC` | `#344A63` | Relleno calle peatonal |
| `roadLabel` | `#25313A` | `#F2F6FA` | Etiqueta de calle |
| `roadLabelHalo` | `#FFFFFF` | `#1C3248` | Halo etiqueta de calle |
| `roadLabelPassage` | `#484848` | `#8FA8C0` | Etiqueta pasaje |
| `railCasing` | `#585A60` | `#AAA5B0` | Casing ferrocarril |
| `railFill` | `#F5F2EE` | `#243345` | Relleno ferrocarril |
| `railTicks` | `#777A80` | `#D0CBD4` | Ticks ferrocarril |
| `stationFill` | `#6D4DB3` | `#B58DFF` | Icono estacion ferrocarril |
| `stationStroke` | `#FFFFFF` | `#21182F` | Borde icono estacion |
| `stationLabel` | `#3E2E5A` | `#C8A0FF` | Etiqueta estacion |
| `stationLabelHalo` | `#FFFFFF` | `#14082A` | Halo etiqueta estacion |

---

## 3. Paleta de localidades (modo dia)  [GEO-STYLE-001]

En modo dia, el relleno de cada localidad usa un color distintivo segun `id_loc`:

| id_loc | Nombre localidad | Color dia |
|---|---|---|
| AP | Grand Bourg / La Pastora | `#F3DFC1` |
| AS | Pablo Nogues | `#D8E7F5` |
| GB | Grand Bourg | `#E7D9F3` |
| LP | Los Polvorines | `#D8ECD9` |
| MA | Malvinas Argentinas | `#F3E8B9` |
| PN | Pablo Nogues | `#D2E9E7` |
| TA | Tortuguitas / Area Norte | `#EED8CD` |
| TO | Tortuguitas | `#DCE0F4` |
| VM | Villa de Mayo | `#F0D9E0` |
| (otros) | - | `#E8E8E8` |

En modo noche, todas las localidades usan el color unico `#3B526B`.

---

## 4. Highlight de parcela seleccionada (runtime)

Las capas de highlight `parcela-highlight-fill` y `parcela-highlight-outline` usan colores fijos:

| Propiedad | Valor |
|---|---|
| fill-color | `#2563EB` |
| fill-opacity | 0.22 |
| line-color | `#1D4ED8` |
| line-width | 2.5 |

Estos colores no cambian con el tema (son identicos en dia y noche).

---

## 5. Comercios: colores por tema  [GEO-STYLE-002]

La capa `comercios-parcela-label` ajusta sus colores dinamicamente en `applyTheme()`:

| Propiedad | Dia | Noche |
|---|---|---|
| text-color | `#1a3a5c` | `#7CC8F8` |
| text-halo-color | `rgba(242,241,236,0.9)` | `rgba(52,74,99,0.9)` |
| text-halo-width | 1 | 1 |

El color del halo coincide con el fondo del mapa en cada tema para que no sea visible como caja.

El icono de comercio es un canvas 40x40px con circulo naranja (`#F57C00`) y silueta de local comercial blanca.
