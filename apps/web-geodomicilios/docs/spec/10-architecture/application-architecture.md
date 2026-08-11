# Arquitectura de la Aplicacion Angular

> **Estado documental:** NORMATIVE
> **Alcance:** estructura del frontend `apps/web-geodomicilios/angular`.
> Ante contradiccion sobre estructura de componentes, servicios o boundaries de NgZone, este documento prevalece.

---

## 1. Contexto tecnologico

| Elemento | Version |
|---|---|
| Angular | 20.x standalone, signals-first, OnPush |
| MapLibre GL JS | 5.x |
| Angular Material | 20.x |
| RxJS | 7.8.x |
| TypeScript | 5.9.x |

---

## 2. Arbol de componentes

`
AppComponent (router-outlet)
  GeoDomiciliosShellComponent  (ruta lazy '')
    mat-tab-group
      DomicilioInfractorTabComponent
      MunicipioLicenciaTabComponent
      UbicacionInfraccionTabComponent
        MalvinasMapComponent
        ParcelaDetailPanelComponent
`

### GeoDomiciliosShellComponent
Contenedor de las tres pestanas. Muestra badges de completitud derivados de `GeoDomiciliosStateService`.
Cargado de forma lazy desde la ruta raiz.

### DomicilioInfractorTabComponent
Captura el domicilio del infractor: provincia, municipio, localidad, calle, altura.
Modo EXTERNO_IGN_INDEC usa `GeoCatalogosService`; modo MALVINAS_LOCAL usa `GeoLocalMalvinasService`.

### MunicipioLicenciaTabComponent
Captura provincia y unidad territorial de la licencia. Usa `GeoCatalogosService` (IGN/INDEC).

### UbicacionInfraccionTabComponent  [GEO-ARCH-001]
Host de la interaccion mapa-geocoding. Responsabilidades:
- parsear el input y detectar modo (partial / calle-altura / esquina / entre-calles);
- solicitar sugerencias (debounce 280ms, min 2 chars, max 5);
- geocodificar la seleccion y coordinar la navegacion del mapa;
- recibir clicks de parcela y pasarlos al panel de detalle.

### MalvinasMapComponent  [GEO-ARCH-002]
Integra MapLibre GL JS. Corre completamente fuera de NgZone.

| Signal | Tipo | Descripcion |
|---|---|---|
| `markerLat` (input) | `number|null` | Latitud del marcador |
| `markerLng` (input) | `number|null` | Longitud del marcador |
| `colocarModoActivo` (input) | `boolean` | Cursor crosshair |
| `mapClick` (output) | `MapClickEvent` | Click en mapa (lat, lng) |
| `mapReady` (output) | void | Mapa inicializado |
| `zoomChange` (output) | `number` | Zoom actual |
| `parcelaSeleccionada` (output) | `ParcelaRef` | Parcela clickeada |

### ParcelaDetailPanelComponent  [GEO-ARCH-003]
Recibe solo un `ParcelaRef` como input y busca sus propios datos.
Componente de **inspeccion visual**, no de captura de acta. No escribe en el state service.

---

## 3. Servicios

| Servicio | Responsabilidad |
|---|---|
| `GeoDomiciliosStateService` | Estado compartido entre pestanas (signals). Unica fuente de verdad del formulario. |
| `GeoLocalMalvinasService` | Localidades, calles y geometrias de parcelas (modo MALVINAS_LOCAL). |
| `GeoCatalogosService` | Provincias, municipios, localidades y calles del catalogo IGN/INDEC. |
| `GisService` | Geocodificacion, reverse geocoding, esquinas, entre calles. |
| `GeoParcelaService` | Detalle de parcela (nomenclatura, inmuebles, comercios). |
| `MapThemeService` | Gestion del tema (auto/dia/noche). Persiste en localStorage. |

---

## 4. Configuracion  [GEO-ARCH-004]

`API_CONFIG`: InjectionToken que provee `baseUrl: /api` (proxy al backend) y `martinUrl`.

`GEO_CONSTANTS`: objeto const con todas las constantes geograficas.
**Regla**: `MARTIN_BASE_URL` no debe contener una IP hardcodeada en builds de produccion.
Usar environment files o reemplazo en tiempo de build.

---

## 5. NgZone boundary  [GEO-ARCH-005]

MapLibre GL JS corre **completamente fuera** del zone de Angular.

| Patron | Uso |
|---|---|
| `ngZone.runOutsideAngular()` | Todo lo de MapLibre: init, event handlers, animaciones |
| `ngZone.run()` | Re-entrada para emitir outputs Angular: `mapClick`, `parcelaSeleccionada`, etc. |
| `addEventListener(el, fn, {passive: true})` | Wheel event proxy al mapa (sin preventDefault) |

---

## 6. Routing

La aplicacion tiene una unica ruta. El shell component se carga de forma lazy:

`	ypescript
' path: '', loadComponent: () => import(GeoDomiciliosShellComponent) '
`

---

## 7. Flujo de datos principal

`mermaid
flowchart TD
    UserInput[Usuario escribe busqueda]
    ParseMode[Detecta modo de busqueda]
    FetchCalles[getCalles - debounce 280ms]
    Suggestions[Sugerencias max 5]
    SelectSug[Seleccion]
    Geocodify[geocodificar]
    StateSet[setPunto en state service]
    FlyTo[flyTo zoom 18-19]
    Marker[Marcador rojo draggable]
    ParcelClick[Click en parcela]
    FetchGeom[getParcelaGeom - geometria completa]
    Highlight[Highlight azul sin clipeo]
    EmitRef[parcelaSeleccionada emit]
    FetchDetail[getParcelaDetalle]
    ShowPanel[Panel de detalle]
    UserInput --> ParseMode --> FetchCalles --> Suggestions --> SelectSug
    SelectSug --> Geocodify --> StateSet --> FlyTo --> Marker
    ParcelClick --> FetchGeom --> Highlight
    ParcelClick --> EmitRef --> FetchDetail --> ShowPanel
`
