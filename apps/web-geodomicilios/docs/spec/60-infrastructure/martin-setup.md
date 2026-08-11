# Martin Tile Server - Instalacion y Configuracion

> **Estado documental:** SUPPORTING_CURRENT
> **Guardrail**: este documento no incluye contrasenas, tokens, ni cadenas de conexion con credenciales.

---

## 1. Que es Martin

Martin es un servidor de vector tiles (MVT) y fuentes tipograficas (glyphs PBF) basado en Rust.
Es open-source y se distribuye como un binario unico sin dependencias de runtime.
Repo oficial: https://github.com/maplibre/martin

---

## 2. Requisitos

| Componente | Requisito |
|---|---|
| Sistema operativo | Windows Server 2019 o superior |
| PostgreSQL | 14+ con extension PostGIS habilitada |
| Puerto | 3000 (configurable) |

---

## 3. Instalacion

### 3.1 Descargar el binario

1. Ir a: https://github.com/maplibre/martin/releases
2. Descargar `martin-x86_64-pc-windows-msvc.zip` de la ultima release estable.
3. Extraer: el resultado es `martin.exe`.
4. Copiar a `C:\tools\martin\`.

### 3.2 Estructura de directorios

`
C:\tools\martin\
  martin.exe             binario principal
  martin.yaml            configuracion
  start-martin.ps1       script de inicio
  fonts\
    Open Sans Regular.ttf
    Open Sans Semibold.ttf
`

---

## 4. Configuracion (martin.yaml)

Configuracion minima para este proyecto:

`yaml
fonts:
  paths:
    - C:/tools/martin/fonts
`

La conexion a PostgreSQL se pasa por variable de entorno `DATABASE_URL`.
**No incluir la cadena de conexion en el archivo YAML.**

---

## 5. Variables de entorno requeridas

Martin necesita la variable `DATABASE_URL` para conectarse a PostgreSQL/PostGIS.

Formato de la variable:
`
DATABASE_URL=postgresql://usuario:contrasena@host:puerto/base_de_datos
`

El archivo de configuracion local (`geodomicilios.local.ps1`) define y exporta esta variable.
Cargar ese archivo antes de iniciar Martin:

`powershell
. 'geodomicilios.local.ps1'
& 'C:\tools\martin\start-martin.ps1'
`

---

## 6. Script de inicio (start-martin.ps1)

Ejemplo de script de inicio sin credenciales:

`powershell
  = 'C:\tools\martin\martin.exe'
 = 'C:\tools\martin\martin.yaml'
 = 3000

if (-not (Test-Path ))   { throw 'martin.exe no encontrado' }
if (-not (Test-Path ))  { throw 'martin.yaml no encontrado' }

 = Get-NetTCPConnection -LocalPort  -State Listen -ErrorAction SilentlyContinue
if () { Write-Warning 'Puerto ya en uso'; exit 1 }

# DATABASE_URL debe estar en el entorno antes de ejecutar este script
&  --config 
`

---

## 7. Fuentes tipograficas para MapLibre

### 7.1 Fuentes requeridas

| Nombre en el style | Archivo TTF |
|---|---|
| Open Sans Regular | Open Sans Regular.ttf |
| Open Sans Semibold | Open Sans Semibold.ttf |

Disponibles en Google Fonts (Apache 2.0): https://fonts.google.com/specimen/Open+Sans

### 7.2 Instalacion

1. Descargar los TTF.
2. Copiar a `C:\tools\martin\fonts\`.
3. Reiniciar Martin.

### 7.3 Endpoint de glyphs

`
GET /font/{fontstack}/{range}
`

Ejemplos validos:
- `/font/Open Sans Regular/0-255`
- `/font/Open Sans Semibold/0-255`

**Importante**: Martin no soporta stacks combinados (`Font A,Font B`).
Cada request debe pedir un unico font name. El frontend resuelve esto truncando los
arrays de `text-font` a su primer elemento al inicializar el mapa.

---

## 8. Funcion MVT: comercios_parcela

Martin detecta automaticamente las funciones SQL con la firma:
`f(z int, x int, y int) RETURNS bytea`.

La funcion `comercios_parcela` debe existir en PostgreSQL antes de iniciar Martin.
El DDL ejecutable debe estar en `db/postgis/functions/comercios_parcela_mvt.sql`.

Despues de crear la funcion, reiniciar Martin y verificar en `/catalog`.

---

## 9. Verificacion post-inicio

| Check | URL | Resultado esperado |
|---|---|---|
| Catalog | `GET /catalog` | JSON con lista de fuentes |
| Glyph Regular | `GET /font/Open Sans Regular/0-255` | 200 con datos PBF |
| Glyph Semibold | `GET /font/Open Sans Semibold/0-255` | 200 con datos PBF |
| Parcelas tile | `GET /geo_qmat_parcela/17/{x}/{y}` | 200 con datos MVT |
| Comercios tile | `GET /comercios_parcela/19/{x}/{y}` | 200 con datos MVT |

Usar la URL LAN del servidor (no localhost) desde la computadora de desarrollo.
La variable `MARTIN_BASE_URL` en `geodomicilios.local.ps1` contiene la URL correcta.

---

## 10. Version de Martin

| Componente | Regla |
|---|---|
| Martin | Ultima version estable disponible y compatible con la configuracion documentada del proyecto |

Para instalar o actualizar Martin: descargar la ultima release estable desde
https://github.com/maplibre/martin/releases y reemplazar el binario en `C:\tools\martin\martin.exe`.

El comando `martin --version` puede usarse opcionalmente como diagnostico, pero no es un
requisito ni un criterio de cierre de ningun GAP o slice.

