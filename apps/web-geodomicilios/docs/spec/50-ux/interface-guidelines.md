# Guias de Interfaz de Usuario

> **Estado documental:** NORMATIVE
> **Alcance:** comportamiento de todos los elementos interactivos de la feature GeoDomicilios.

---

## 1. Layout general

La aplicacion consiste en tres pestanas dentro de un `mat-tab-group`:

| Pestana | Componente | Descripcion |
|---|---|---|
| Domicilio del Infractor | `DomicilioInfractorTabComponent` | Captura la direccion del infractor |
| Municipio de la Licencia | `MunicipioLicenciaTabComponent` | Captura la jurisdiccion de la licencia |
| Ubicacion en el Mapa | `UbicacionInfraccionTabComponent` | Mapa interactivo con buscador de domicilios |

---

## 2. Buscador de domicilios  [GEO-UX-001]

### Posicion y layout
- Input flotante en la parte superior izquierda del mapa.
- Ancho: 40% del ancho del panel.
- Icono de lupa a la derecha del input.
- Boton clear centrado verticalmente cuando hay texto.
- Los controles de tema (auto/dia/noche) y el nivel de zoom se muestran debajo del buscador.

### Comportamiento
- Debounce: 280ms desde la ultima pulsacion.
- Minimo de caracteres: 2.
- Maximo de sugerencias: 5.
- Al detectar modo `calle-altura`: el numero se envia como parametro `altura` para filtrar sugerencias invalidas.
- Navegacion por teclado: flechas arriba/abajo navegan las sugerencias; Enter selecciona.
- Al seleccionar una sugerencia: el mapa navega al punto geocodificado con zoom 18-19.

### Panel de sugerencias
- Maximo 5 items.
- Cada item muestra: nombre de calle, localidad, y distancia estimada.
- Icono indicativo del modo detectado (calle, esquina, entre calles).
- Al hacer click fuera del panel, se cierra.

---

## 3. Marcador  [GEO-UX-002]

- Marcador rojo en las coordenadas geocodificadas.
- Es draggable: al soltarlo en una nueva posicion, se emite el nuevo punto.
- Boton de navegacion en el panel de punto actual para hacer flyTo al marcador.

### Panel de punto actual
- Muestra la descripcion del domicilio seleccionado.
- Mismo ancho que el buscador.
- No muestra el campo de origen/fuente (`GEOCODIFICADO_CALLE_ALTURA`) al usuario.
- Alineado a la izquierda para no interferir con la navegacion del mapa.

---

## 4. Modo colocar punto  [GEO-UX-003]

Al activar el modo colocar:
- El cursor del mapa cambia a crosshair.
- El proximo click en el mapa coloca el marcador en esa posicion.
- El modo se desactiva automaticamente al colocar el punto.
- El origen queda como `MANUAL_MAPA`.

---

## 5. Interactividad del mapa  [GEO-UX-004]

**El mapa debe ser siempre navegable** (pan y zoom).

Reglas:
- Los overlays sobre el mapa (buscador, panel de punto actual) NO deben bloquear
  el scroll del mapa. Los listeners de wheel se agregan con `{ passive: true }`.
- Despues de geocodificar o seleccionar un punto, el mapa sigue siendo navegable.
- Todo lo relacionado con MapLibre corre fuera de NgZone para no degradar la interactividad.

---

## 6. Selector de tema  [GEO-UX-005]

Tres opciones: Auto, Dia, Noche.
- Posicion: debajo del buscador, alineado a la izquierda.
- La seleccion se persiste en `localStorage` con la clave `malvinas-map-theme`.
- En modo Auto: el tema se recalcula automaticamente segun la hora (07:00-18:59 = dia).

---

## 7. Indicador de zoom  [GEO-UX-006]

Muestra el nivel de zoom actual del mapa en tiempo real.
Posicion: junto al selector de tema, debajo del buscador.
Formato: un decimal (`19.0`, `18.5`, etc.).

---

## 8. Capa de comercios  [GEO-UX-007]

La capa de comercios es **siempre visible**. No tiene toggle.
Solo se muestra a partir de zoom 18.

Cada comercio se representa con:
- Icono: circulo naranja con silueta de local comercial blanca.
- Etiqueta: nombre de fantasia del comercio, fuente 9pt, debajo del icono.

Reglas de posicionamiento:
- El icono se coloca dentro de la parcela (28% por debajo del centroide).
- La etiqueta de calle tiene prioridad sobre la etiqueta de comercio.
- La etiqueta de comercio es opcional (`text-optional: true`).

---

## 9. Inspector de parcelas  [GEO-UX-008]

Al hacer click sobre una parcela:

1. La parcela se resalta con un borde azul y relleno semitransparente.
2. El panel de detalle de parcela se muestra debajo del buscador.

El cursor cambia a pointer al pasar sobre una parcela.

### Panel de detalle de parcela

Muestra (de arriba a abajo):
1. Direccion estimada de la parcela.
2. Nomenclatura catastral completa (omitiendo Q0 y F0).
3. Lista de cuentas de inmuebles (numero de cuenta).
4. Lista scrollable de comercios: numero de cuenta + nombre de fantasia.

**Este panel es de inspeccion visual, no de captura de acta.**
No escribe ningun dato en el formulario de actas.

### Highlight de parcela

El highlight usa la **geometria completa** de la parcela obtenida del backend,
no la geometria clipada del tile MVT. Esto garantiza que toda la parcela se resalte,
sin importar cuantos tiles la atraviesen.

---

## 10. Zoom ladder  [GEO-UX-009]

El zoom del mapa es discreto con los siguientes niveles recomendados:

| Nivel | Que se ve |
|---|---|
| 12 | Municipio completo |
| 14 | Barrios y localidades |
| 16 | Manzanas y calles principales |
| 17 | Parcelas |
| 18 | Comercios (aparecen) |
| 18.5 | Comercios con etiquetas bien legibles |
| 19 | Detalle de parcela y numeracion |
| 19.5 | Maximo detalle |

El zoom objetivo para geocodificacion es 18-19.

---

## 11. Autocomplete de formularios  [GEO-UX-010]

Los campos de texto con autocomplete (Provincia, Municipio, Localidad, Calle) comparten comportamiento:

- Filtran a medida que se escribe.
- La busqueda es insensible a acentos.
- Al seleccionar una opcion, el valor seleccionado queda visible en el campo.
- No muestran dropdown fijo; el dropdown aparece al escribir.
- Todos usan `mat-autocomplete` de Angular Material.
