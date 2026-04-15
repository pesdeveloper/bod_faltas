**Las notas al respecto estan en llamadas como estas:**
*(a)* es : Si pero que guaradmos ahi y como se alimenta este dato e interpreta?, donde se actualiza esto y como ?

# ACTA
NumeroActa VARCHAR(20) Alcanza : ejemplos: 2026-001-000002 o AB-000001 o A120001, segun politica de numeracion

> CORRECCION: El domicilio es dentro del distrito de malvinas argentinas.
>   - La calle debe ser obtenida desde el catalogo de calles, se ubica por busqueda o se obtiene por georeferenciacion
>   - El numero debe ser un valor numerico
CalleInfraccion VARCHAR(120): Se descompone en IdTca CHAR(5) que apunta al catalogo de calles !
NumeroInfraccion VARCHAR(10): Debe ser un INT

> CORRECCION en EntreCalles 
>   - Se obtiene por consulta georeferenciacion o por calle y altura desde catalogo de calles por la altura 
>   - Se decompoene en IdTcaE1 y IdTcaE2 ambos son CHAR(5).
EntreCalles VARCHAR(150): 

LocalidadInfraccion VARCHAR(80):  *ELIMINARLO*, Se obtiene de otra forma, abajo te explico esto !
PartidoInfraccion VARCHAR(80): *ELIMINARLO*, son siempre en el mismo partidp!

> CORRECCION en LocalidadInfraccion y PartidoInfraccion
>   - Se obtiene de georeferenciacion o por calle y altura ingresada, se obtiene localidad
>   - El partido es siempre el mismo, Malvinas Argentinas, deberia ser un dato que no se pide y es asignado desde catalogo de partidos, en confguracion en todo caso se deberia poder asignar codigo de partido por defecto para infracciones. No se hacen infracciones fuera del partido de echo este campo es candidato o mejor dicho hay que eliminarlo !

 **Notas de implementacion de domicilio de la infraccion**
Los domicilios de la infraccion se obtiene en en primera instancia utilizando el GPS y una consulta a base de datos espacial posgress/potgis
de todas formas esa consulta es solo para saber cual es la calle y altura, pero si el acta se esta precargando por ejemplo y esta en modo borrador o se finaliza luego , la calle y la altura se ingresaran en el firmulario, la busqueda de la direccion devolvera del catalogo de calles existentes que abajo de voy a mostrar, el Id_Tca que es el Id de calle junto a su descripcion, luego hay una tabla 
con los id de calles y numeraciones que a su vez tienen la localidad , e inclusive el barrio. esto es muy util !
Los campos que forman el domicilio de la infraccion son:
IdTcaInfraccion (ID de calle) CHAR(5) , desde la tabla calles que es el catalogo mantenido por catastro. asimismo tambien tenemos calles_snapshot como lo queramos crear, ya que vamos a versionar esa tabla, entonces tendremos IdTcaVersion un numero de version.
La altura es un numero y formata parte del domicilio de la infraccion: CalleNumeroInfraccion INT.

Luego tenemos la localidad IDLocInfraccion CHAR(2) y por ultimo IdBarInfraccion (ID de barrio) SMALLINT, estos datos se llenan con el resultado 
de la consulta sobre geo_calle_alturas_barrio que mas abajo se muestra.
*IMPORTANTE* No tenemos domicilios textuales en la infraccion salvo que los catalogos no esten actualizados, en ese caso debe ser 
solicitado en la UX aprentado algun boton que pondra en TRUE un campo, SiDomiTextual SMALLINT y ahi si me permitira 
ingresar el domicilio en formato texto libre VARCHAR(150) , pero preferimos que el domicilio siempre sea normalizado.

> CORRECCION en DentroEjidoUrbano es DentroEjeUrbano y FueraEjidoUrbano no es necesario, es o no es eje urbano, no puede ser ambos !
DentroEjidoUrbano SMALLINT: Este dato se debe obtener desde catalogo, ya sabemos que IdTca y Altura son o no Son eje Urbano asi que se asigna desde la consulta de calle y altura, no es necesario completarlo manual, se pre asigna valor !
FueraEjidoUrbano SMALLINT: Error conceptual, Se elimina este campo.
- DentroEjidoUrbano: Esta dato se obtiene desde catalogo, ya sabemos que IdTca y Altura son o no Son eje Urbano.
  **NOTAS Y COMO SE OBTIENE**
  1. Se obteiene la calle y la altura ya sea por GPS o busqueda en el formulario, luego con el codigo de calle y la altura 
  se busca en la tabla que te muestro aca ! geo_calle_alturas_barrio (
    id_tca CHAR(5) not null,
    alt_desde INT not null,
    alt_hasta INT not null,
    calle_nombre VARCHAR(64) not null,
    localidad_geo VARCHAR(64) not null,
    id_bar SMALLINT default 0 not null,
    barrio_geo VARCHAR(64) not null,
    SiEjidoUrbano SMALLINT default 0 not null,
    id_loc CHAR(2) default '' not null
) , como podras observar hay un campo *SiEjidoUrbano* y bueno ese es el campo que indica si calle y esa altura es o no es EjidoUrbano.

NombreInfractor VARCHAR(120): Pasa a VARCHAR(64), alcanza y sobra

> CORRECCION en DocumentoInfractor, se descompone mas alla de como se ingresa si se usan expresiones regulares , ideal para esto.
>   - Descomposicion en : DocPref SMALLINT, DocNro INT, DigVerif SMALLINT
DocumentoInfractor VARCHAR(20): Es un Numero de DNI o como maximo un CUIT o CUIL, este dato se debe descomponer en DocPref, DocNro, DocDigVerif, pero el ingreso se hace en un campo de texto, al hacer el parser o regex sobre ese campo se debera descomponer y asignar.

TipoPersona SMALLINT: SI y *(a)*


DomicilioInfractor VARCHAR(150): Bueno aca es caso similar al del domicilio de la infraccion, pues preferimos datos normalizados, la normalizacion como primer camino se realiza utlizando las tablas pre cargadas disponibles en el INDEC, lo que se predente es :
El formulario solicita únicamente:

1. **Provincia**
2. **Municipio** (unidad administrativa lógica)
3. **Localidad**
4. **Calle**
5. **Altura**
6. *(Opcional)* Piso / Depto / Observaciones

❌ No se solicita:
- Departamento
- Localidad censal
- Barrio
- Asentamiento
- Conceptos técnicos del modelo de datos

---

## 🧠 Concepto clave: "Municipio lógico"

En la interfaz, siempre se presenta el campo **Municipio**, que representa:

- ✅ un municipio real (cuando existe)
- ✅ o un departamento usado como *fallback*, mostrado **como si fuera municipio**

El usuario **no distingue** la diferencia.  
El sistema sí la conoce y la registra.

---

## 🔁 Flujo de búsqueda y selección (orden jerárquico)

### 1️⃣ Provincia
- Selección obligatoria
- Fuente: `geo_ign_provincia`
- Se guarda: `provincia_id`

---

### 2️⃣ Municipio (unidad administrativa lógica)
Política de búsqueda:

1. Buscar coincidencias en **municipios reales**
   - Fuente: `geo_ign_municipio`
   - Filtrado por `provincia_id`
2. Si no hay resultados o son insuficientes:
   - Usar **departamentos como fallback**
   - Fuente: `geo_ign_departamento`
   - Se presentan igualmente como “Municipio”

Resultado interno:
- `municipio_id` → puede ser `NULL`
- `departamento_id` → **siempre queda definido**

---

### 3️⃣ Localidad
Política de resolución:

SI hay `municipio_id`:
- buscar localidades por **(provincia_id + municipio_id)**

SI NO hay `municipio_id`:
- buscar localidades por **(provincia_id + departamento_id)**

Fuente:
- `geo_indec_localidad`

Esto garantiza cobertura correcta incluso en provincias sin municipios formales.

---

### 4️⃣ Calle
Política de búsqueda:

1. Autocompletar por **localidad censal**
   - Fuente: `geo_indec_calles`
   - Búsqueda por prefijo (`LIKE 'SAN%'`)
2. Validación opcional por tramos (si está disponible)
   - Fuente: `geo_calle_alturas_barrio`
   - Regla:
     - `altura BETWEEN alt_desde AND alt_hasta`

Si existe calle pero no hay coincidencia de tramo para la altura:
- el domicilio se guarda igual
- se marca como **validación parcial** (sin barrio o sin tramo)

---

### 5️⃣ Altura
- Campo numérico obligatorio
- Validación mínima:
  - `altura > 0`

La altura se utiliza luego para:
- validación contra tramos
- resolución de barrio (si corresponde)

---

## 🔄 Política general de fallback

| Nivel     | Si no hay coincidencia | Acción del sistema |
|----------|-------------------------|--------------------|
| Municipio | No existe municipio     | Usar departamento (fallback) |
| Localidad | No hay municipio        | Usar departamento |
| Calle     | No existe en catálogo   | Permitir carga manual (opcional) |
| Altura    | No entra en tramo       | Guardar sin barrio (validación parcial) |

El usuario **no toma decisiones técnicas** en ningún caso.

---

## 🗄️ Datos persistidos (backend)

Aunque la UI es simple, el sistema almacena información completa:

- `provincia_id`               NOT NULL
- `municipio_id`               NULL
- `departamento_id`            NOT NULL
- `localidad_id`               NOT NULL
- `localidad_censal_id`        NOT NULL
- `calle_id`                   NOT NULL
- `altura`                     NOT NULL

## ✅ Ventajas de este enfoque

- UX simple y sin ambigüedades
- Modelo compatible con INDEC / IGN
- Funciona para toda la Argentina
- Escalable y mantenible
- Integrable con GIS y QGIS
- Alineado con sistemas municipales reales

## 📌 Nota final

Este diseño desacopla completamente:
- **la experiencia del usuario**
del
- **modelo territorial real**



> CORRECCION en ProvinciaInfractor *Se Elimina como esta arriba se eplico ya !*
ProvinciaInfractor VARCHAR(80): Debe ser un codigo normalizado desde catalogo de partidos normalizado argentina !

> CORRECCION en PartidoInfractor *Se Elimina como esta arriba se eplico ya !*
PartidoInfractor VARCHAR(80): Debe ser un codigo normalizado desde catalogo de partidos normalizado argentina !

> CORRECCION en LocalidadInfractor *Se Elimina como esta arriba se eplico ya !*
LocalidadInfractor VARCHAR(80): Debe ser un codigo normalizado desde catalogo de partidos normalizado argentina !

CodigoPostalInfractor VARCHAR(10): Se puede leer desde catalogo, pero es posible modificarlo.

ATENCION: Las tablas IGN deberan llevar versionado, todavi no lo implementan, pero lo deben implementar, como un numero pequeño, smallint o byte
ya que estos catalogos no se cambia seguido, poner un smallint quiza sea mucho , pero andaria bien. esto lo veremos mas adelante de todas
formas ten en cuenta el campo de version para los datos que salgan de las tablas INDEC IGN !


ObservacionesActa LVARCHAR(4096): SI
FechaHoraAlta DATETIME YEAR TO SECOND: SI
IdUserAlta CHAR(36): SI
FechaHoraUltMod DATETIME YEAR TO SECOND: SI
IdUserUltMod CHAR(36): SI pero como dato de performance, deberia estar en el snapshot, no aca esto es auditortia proyectada para performance!

# ActaSnapshot

IdActa INT8: SI
BloqueActual SMALLINT: *(a)*
EstadoProcesoActual SMALLINT: *(a)*
SituacionAdministrativaActual: *(a)*
EstaCerrada SMALLINT:
PermiteReingreso SMALLINT:
TipoCierreActual SMALLINT:
EstadoPagoActual SMALLINT:
TieneDescargo SMALLINT:
TieneMedidaPreventiva SMALLINT:
EstadoDocumentalActual SMALLINT:
EstadoNotificacionActual SMALLINT:
CantidadDocsPendFirma SMALLINT:
CantidadDocsFirmados SMALLINT:
IdActaEventoUltimo INT8:
IdDocumentoUltimo INT8:
IdNotificacionUltima INT8:
FechaHoraSnapshot DATETIME YEAR TO SECOND: SI

# ActaEvento

Id INT8: SI
IdActa INT8: SI
FechaHoraEvento DATETIME YEAR TO SECOND: SI
TipoEvento SMALLINT: SI
OrigenEvento SMALLINT: SI
BloqueFuncional SMALLINT: SI y *(a)*
EstadoProcesoAnterior SMALLINT:
EstadoProcesoNuevo SMALLINT:
SituacionAdminAnterior SMALLINT:
SituacionAdminNueva SMALLINT:
IdDocumentoRelacionado INT8:
IdNotificacionRelacionada INT8:
IdPresentacionRelacionada INT8:
IdUserEvento CHAR(36): SI
EsEventoDeCierre SMALLINT: SI y *(a)*
EsEventoExterno SMALLINT: SI y *(a)*
PermiteReingreso SMALLINT: SI y *(a)*

# Observacion

Id INT8: SI
IdRef SMALLINT: SI
IdFk INT8: SI
Observacion VARCHAR(255): SI
FechaHoraAlta DATETIME YEAR TO SECOND: SI
IdUserAlta CHAR(36): SI y deberia llamarse IdUserEvento o IdUser a secas
*Observaciones sobre esta tabla: En caso de ser necesario editarla, solo la puede editar la persona que la creo, y en caso de realizar una edicion esta debe generar un evento en el cual si se pondria el idUser, para darle mas significando al idUserAlta lo vamos a dejar como IdUser asi sirve para los dos propositos.*

# ActaTransito

IdActa INT8: SI
LicenciaNumero VARCHAR(30): *CORRECCION: CHAR(8)*

> CORRECCION en LicenciaMunicipioEmisor: El municipio emisor debe ser capturado utilizando las tablas IGN/INDEC con logica de busqueda en UX
por provincia + Municipio, no hay datos textuales libres aca!
se debera persistir (El nombnre de los campos es un ejemplo, pon como debe ir segun el caso ):
- `provincia_id`               NOT NULL
- `municipio_id`               NULL
- `departamento_id`            NOT NULL
> VER: **QR mi argentina o OCR sobre licencia** 


LicenciaMunicipioEmisor VARCHAR(80): *(Queda eliminado como esta)*

> CORRECCION en DominioVehiculo:
| Tipo de patente | Período / Uso | Ejemplo | Longitud | Regex de validación |
|-----------------|---------------|---------|----------|---------------------|
| Auto – Mercosur | Vigente (2016–actualidad) | AA123BB | 7 | ^[A-Z]{2}[0-9]{3}[A-Z]{2}$ |
| Moto – Mercosur | Vigente (2016–actualidad) | A123BCD | 7 | ^[A-Z][0-9]{3}[A-Z]{3}$ |
| Auto – sistema anterior | 1995–2016 | ABC123 | 6 | ^[A-Z]{3}[0-9]{3}$ |
| Provincial antiguo | 1964–1994 | B123456 | 7 | ^[A-Z][0-9]{5,6}$ |
| Oficial / Especial (Mercosur) | Vigente | AF123CD | 7 | ^[A-Z]{2}[0-9]{3}[A-Z]{2}$ |
| Diplomático / Consular | Vigente | CD123AB | 7 | ^[A-Z]{2}[0-9]{3}[A-Z]{2}$ |
| Remolque / Acoplado | Vigente | RT123AB | 7 | ^[A-Z]{2}[0-9]{3}[A-Z]{2}$ |
| Provisoria (papel) | Temporal | AG123EF | 7 | ^[A-Z]{2}[0-9]{3}[A-Z]{2}$ |

> Regex “unificadora” (si necesitás una sola)
^([A-Z]{2}[0-9]{3}[A-Z]{2}|[A-Z][0-9]{3}[A-Z]{3}|[A-Z]{3}[0-9]{3}|[A-Z][0-9]{5,6})$

DominioVehiculo VARCHAR(15): *Cambiar a longitud de 10 para tener espacio extra*


>CORRECCION EN : TipoVehiculo VARCHAR(50) y MarcaVehiculo VARCHAR(50) y ModeloVehiculo VARCHAR(50)
Estos datos en la UX vendran de catalogos, pero si en el ingreso no figuran simplemente se acepta el texto ingresado.
tipo_vehiculo_id (enum/FK) a tabla de 
tipo_vehiculo_texto (nullable, solo si enum = OTRO)

Sugerencia pensada para actas/multas y captura desde cédula o ingreso manual:
- `AUTOMOVIL`
- `PICKUP_CAMIONETA`
- `UTILITARIO_FURGON`
- `CAMION`
- `OMNIBUS_MINIBUS`
- `MOTOCICLETA`
- `CICLOMOTOR`
- `TRICICLO_CUATRICICLO`
- `ACOPLADO_REMOLQUE`
- `MAQUINARIA_VIAL_ESPECIAL`
- `MAQUINARIA_AGRICOLA`
- `CASA_RODANTE`
- `OTRO` (fallback controlado)

### Reglas de uso recomendadas
- El valor `OTRO` debe usarse **solo** si no matchea con los anteriores y guardando el texto original en un campo auxiliar (`TipoVehiculoTexto`).

## Ejemplo de implementación (conceptual)

### Opción A (recomendada): guardar como INT + tabla de referencia
- `TipoVehiculoId SMALLINT` (FK a tabla `tipo_vehiculo`)

Campos:
- `TipoVehiculoId SMALLINT NOT NULL`
- `MarcaVehiculo  VARCHAR(64) NOT NULL`
- `ModeloVehiculo VARCHAR(128) NOT NULL`

## Checklist de normalización (recomendado)
Al persistir:
- `MarcaVehiculo` y `ModeloVehiculo`: `TRIM`, colapsar espacios, opcional `UPPER`
- Guardar también el texto original (opcional) si vas a hacer matching con catálogo
- `TipoVehiculoEnum`: mapear por reglas (y si no, `OTRO` + `TipoVehiculoTexto`)

# ACA SE PONE JODIDO, pues hay datos que la verdad son chotos, pero le vamos a buscar la vuelta.
ALCOHOLEMIA !
SiControlAlcoholemia: SMALLINT 0/1

Alcoholemia_TipoPrueba: SMALLINT enum  de Tipo de prueba realizada Alómetro (tamizaje)  o  Alcoholímetro (medición)
- ALOMETRO        → prueba de tamizaje (presuntiva) 
- ALCOHOLIMETRO   → prueba de medición (cuantitativa)
Tipo de dato para esto: AlcoholemiaTipoPrueba TipoPrueba SMALLINT NUM('ALOMETRO', 'ALCOHOLIMETRO')

AlcoholemiaResultado DECIMAL(4,2)  -- rango típico: 0.00 a 9.99
Valores posibles
Ejemplos: Mediciones: [0.92, 0.88] si hay mas una

Decimal ≥ 0.00
Ejemplos reales:

0.00 → negativo
0.32 → positivo (tolerancia cero)
0.87 → positivo general
1.45 → alcoholemia grave



⚠️ Regla importante:

Si TipoPrueba = ALOMETRO → NO debe existir valor numérico
Si TipoPrueba = ALCOHOLIMETRO → OBLIGATORIO

Alcoholemia_UnidadMedida CHAR(3) CHECK (UnidadMedida = 'G/L')
Alcoholemia_NumeroSerieEquipo VARCHAR(64)


Alcoholemia_EquipoCalibrado SMALLINT 0/1
Alcoholemia_CalibracionVigente SMALLINT 0/1
Alcoholemia_FechaUltimaCalibracion DATE NULL

Alcoholemia_CantidadMediciones SMALLINT
Alcoholemia_ResultadoFinal = 0.81

Resultado numérico del test (g/l de alcohol en sangre)
Unidad de medida (g/l)
Número de serie / identificación del alcoholímetro
Constancia de calibración / homologación del equipo (según protocolo)
Cantidad de mediciones realizadas (si hubo repetición)
Resultado final válido

> Cuando se realiza la prueba de alcoholimetro, el numero de serie del alcoholimetro se seleccionara de los alcoholimetros ingresados 
en la aplicacion para no tener que cargarlo en cada medicion positiva, ademas se podra obtener el numero de serie marca etc
escanenado un codigo qr presente en el dispositivo !

> Resumen estructurado (ideal para tu modelo de datos)
TipoPrueba:              ENUM / SMALLINT
ResultadoAlcoholemia:    DECIMAL(4,2)
UnidadMedida:            CHAR(3) = 'G/L'
NumeroSerieEquipo:       VARCHAR(64)
CantidadMediciones:      SMALLINT
ResultadoFinal:          DECIMAL(4,2)

ALGO MAS :
Control vehicular
   ↓
ALÓMETRO (tamizaje)
   ↓
¿POSITIVO?
   ↓ sí
ALCOHOLÍMETRO (medición)
   ↓
Acta de infracción
 Asi que debemos guardar el resultado del alometro si se realiza !, puede que no se realice !

Ejemplos:
TipoPrueba = ALCOHOLIMETRO
Resultado = NEGATIVA
ResultadoAlcoholemia = NULL
ResultadoFinal = NULL

Reglas de consistencia (MUY importantes)
> Regla 1
Si TipoPrueba = ALOMETRO
→ ResultadoAlcoholemia = NULL
→ UnidadMedida = NULL
→ ResultadoFinal = NULL
> Regla 2
Si TipoPrueba = ALCOHOLIMETRO
→ ResultadoFinal OBLIGATORIO
→ UnidadMedida = 'G/L'
→ NºSerieEquipo OBLIGATORIO
> Regla 3
CantidadMediciones >= 1
ResultadoFinal debe coincidir con:
- única medición
- o regla de reducción (menor / promedio)

✅ No hay “otra medición” válida en el control vial
✅ Alómetro = detección
✅ Alcoholímetro = prueba legal
✅ Solo el alcoholímetro llena el bloque crítico
✅ Alómetro y alcoholímetro NO compiten, se complementan
✅ El bloque crítico solo tiene sentido con alcoholímetro


>*CORRECCION REF-1* Esto es una tabla normalizada
ArticuloInfringido VARCHAR(30):
DescripcionInfraccion LVARCHAR:
NormaInfringida VARCHAR(120):

RetencionLicencia SMALLINT: SI
RetencionVehiculo SMALLINT: SI

# ActaContravencion

IdActa INT8: SI

> CORRECCION en NomenclaturaCatastral:
NomenclaturaCatastral VARCHAR(60): La noemclatura catastral es un data compuesto.
**Composicion de la Nomenclatura Catastral**
    circ SMALLINT not null,
    secc CHAR(2),
    frac CHAR(7),
    mza CHAR(7),
    parc CHAR(7),
    ufun CHAR(7),
    ucomp CHAR(3)    
**Forma de obtencion de la Nomenclatura Catastral**
La nomenclatura catastral puede ser ingresada manualmente si y so si no esta presente el nro de cuenta municipal 
de comercio o de inmueble, salnado la excepcion de que el comercio no tenga la referencia a inmueble , en ese caso 
si puede ser de contenido textual y validada que exista en los maestros, via una consulta en la capa de persistencia que 
devolvera si es valida o no.


> CORRECCION en RubroActividad: debe ser id_rub SMALLINT not null el cual depende del catalogo de rubros actuales 
que esta en la tabla rubrocom (
    id_rub SMALLINT not null,
    deno CHAR(64) not null ) la que pasara a ser una tabla versionada.

AmbitoContravencion: Debe ser un SMALLINT enumerable con los valores BALDIO/COMERCIO/INDUSTRIA/VIVIENDA/LOCAL/OTRO)
- En case de que sea otro es AmbitoContravencionTexto controlada , solo si es OTRO.

>*CORRECCION REF-1* Esto es una tabla normalizada
ArticuloInfringido VARCHAR(30):
DescripcionInfraccion LVARCHAR:
NormaInfringida VARCHAR(120):

**CORRECCION REF-1** (Esto es una tabla normalizada)
La estrctura deinitorial de los articulos es asi:
Tabla de Normativas -> Tabla de Articulos por Normativa 

Se propone Tabla de Normativas: NormativaFaltas
    id_norma VARCHAR(8) not null,
    nombre VARCHAR(64),
    siTransito SMALLINT 0/1,
    siActiva,
    versionNorma SMALLINT,

Se propone Tabla de Articulos de las Normativas: ArticuloNormativaFaltas
    id_norma VARCHAR(8) not null,
    id_art_norma VARCHAR(8) not null,
    nombre VARCHAR(64),
    versionArtNorma SMALLINT,
    unidadDeMedida: es la unidad de medisa de aplicacion del articulo, es un SMALLINT que exprese Enum (UNIDAD FIJA o UF , SALAIO, IMPORTE FIJO )
    Valor : Es el valor a ser aplicado, es el cuantificador de la unidad DECIMAL(16,2)

Tabla de configuracion de valor de UNIDAD FIJA , SALARIO
    IdTarifario : INT , es un ID que da unicidad a la tabla
    VigenciaDesde: FECHA, que indica la vigencia desde de esta configuracion de valor
    VigenciaHasta: FECHA, que indica la vigencia hasta de esta configuracion de valor, es opcional si debe estar si es sacada de vigencia para documentar las fechas entre las cuales esa unidad era valida.
    SiActiva SMALLINT, indica si la unidad esta vigente o no, este dato es un dato para acceso rapido, ya que buscamos siempre de la unidad 
    cual es la vigente, la idea es tener un hisorico en donde cada articulo de cada acta tenga la referencia a los valores 
    orignales de los cuales se calculo , y cuando se modifique el valor de la unidad solo se aplique a actas nuevas.
    tambien puede ser una tabla versionada, pero siempre debe estar la fecha de vigencia hasta el dia que se modifico.
    En caso de que sea necesario actualizar valores de actas ya eralizadas esto se realizara con un proceso especifico.
    Igual esto esta a definirse ahun !

En las actas se debe instanciar en una tabla de Articulos infringidos los siguientes datos
    id_norma VARCHAR(8) not null,
    versionNorma SMALLINT,
    id_art_norma VARCHAR(8) not null,
    versionArtNorma SMALLINT,
    unidadDeMedida: SMALLINT que exprese Enum (UNIDAD FIJA o UF , SALAIO, IMPORTE FIJO )
    IdTarifario que se utilizo
    Valor : El valor hace referencia a la unidad de medida pero en ocasiones excepcionales 
    este valor puede ser editado manualmente por la directora de faltas o inclusive puede ser asignado un importe manualmente 
    y de forma discrcional por eso, tambien vamos a tener un campo que sea unidadDeMedidaEfectiva o aplicada, que al principio 
    va a ser la unidad de medida por defecto ya definida, pero puede ser editada y modificada si asi es necesario tanto en el tipo 
    como en su cuantificacion o valor.
    siActiva: Esto es necesario ya que puede que en una sentencia , fallo o resolucion se dictamine que esa que ese articulo fue sonreseido y no debe aplicarse, o que la directora de faltas determine que no es valido y no se aplicara en esta acta.
    NOTAS ESPECIALES: En caso de que se realice modificaciones se debera asentar en la tabla de observaciones opcionalmete 
    razones que justifiquen lo que se hizo. Ademas se debe llevar una tabla de audotria con el usuario , la accion, que valor cambio y cual es el nuevo valor, a modod de auditoria con fecha y hora.    


# ActaSustanciasAlimenticias

IdActa INT8: SI
RubroActividad VARCHAR(120): **(Ya se eplico antes en ActaContravencion)**
AmbitoActividad SMALLINT:  **(Ya se eplico antes en ActaContravencion)**
DescripcionAmbitoOtro VARCHAR(80): Se elimina, **(Ya se eplico antes en ActaContravencion)**

**Los siguientes campos sobre el vehiculo ya me explicaron antes en ActaTransito , asi que como estan deben desaparecer** 
- DominioVehiculo VARCHAR(15): Debe ser igual que ActaTransito
- TipoVehiculo VARCHAR(50): Debe ser igual que ActaTransito
- MarcaVehiculo VARCHAR(50): Debe ser igual que ActaTransito
- ModeloVehiculo VARCHAR(50): Debe ser igual que ActaTransito

YA se eplico en **CORRECCION REF-1**
ArticuloInfringido VARCHAR(30): CORRECCION REF-1
DescripcionInfraccion LVARCHAR: CORRECCION REF-1
NormaInfringida VARCHAR(120): CORRECCION REF-1

# ActaMedidaPreventiva

Id INT8: SI
IdActa INT8: SI
TipoMedidaPreventiva SMALLINT: SI y *(a)*

> CORRECCION en DescripcionMedida:
DescripcionMedida VARCHAR(255): La medida preventiva debe ser obtenida de catalogo el cual debe estar organizado 
por dependencia que realiza el acta. Debe ser una tabla de medidas preventivas.
MedidaPreventivaTexto o bien el significado real de DescripcionMedida: VARCHAR(255): SI, es opcional 
para describir o aportar informacion relacionada con la medida preventiva, ya que desde el catalogo solo obtenmos
el nombre de la medida.

FechaHoraAlta DATETIME YEAR TO SECOND:
IdUserAlta CHAR(36): SI

# Tabla de medidas preventivas organizada por dependencia
Debe ser una tabla con el catalogo de medidas preventivas, mas una descripcion opcional de la medida preventiva.
Debe estar organizada por Dependencia como el acta, ya que un inspector de una dependencia tiene medidas preventivas 
distintas al de otra dependencia.
como minimo debe tener:
IdMedidaPreventiva INT
VersionMedidaPreventiva SMALLINT
NombreMedidaPreventiva VARCHAR(120): SI
DescripcionMedida VARCHAR(255): SI, es opcional 
SiActiva SMALLINT: 0/1
IdDependencia INT: SI
VersionDependencia SMALLINT: SI

*IMPORTANTE*
Nos esta faltando la tabla de depenencias que ademas es fundamental para el acta y la tabla de inspectores relacionados al acta creo.
La tabla de dependencias debe ser una tabla con un concepto organizacional de gerarquico donde una dependencia puede ser 
padre o hija de otra dependencia.

Por Ejemplo: Servicios -> Bromatologia -> otra Direccion
Debemos tener un ParentId si es hija, pero ojo y muy importante el versionado debe ser pensado para 
poder reconstruir el organigrama hacia atras en caso de ser necesario.

Bueno byte, antes de pasar a generar mas spec yo diria que armes una previa de como se veria esto, lo veo 
lo estudio, si algo me parece que debemos cambia lo vambiamos lo ajustamos hasta que estemos confirmes y luego hacemos 
las tablas nucleos as spec. 




----------------------------------------------------------------------------------------------------------------------



16.6 EstadoProcesoActual: Puede ser que se este generando se grabe un previa del acta en fiscalizaciones por ejemplo
y luego se dirija al domicilio de la infraccion y termine el acta ahi, por ejemplo paralizacion de obra. entonces 
el acta se creo y se prepara en oficina, luego inspector va al lugar termina el labrado y se entrega el acta.
Este seria un estado que hemos eliminado del proceso hace tiempo que es BORRADOR ! ahu no numerada pero existe en borrador 
y puede ser descartado en caso de que no se finalice la emision del acta. Quiza un flag BORRADOR sirva para esto.


16.11 TipoEvento. Deberiamos incluir BORRADOR, igual esto no cambia el flujo operativo, ya que el acta ingresa 
al proceso administrativo una vez que esta labrada !

17) Lo que todavía veo abierto

Esto no lo considero cerrado todavía:

catálogo exacto de EstadoProcesoActual: Para mi esta bien asi, si aparece algun otro lo agregamos pero ahora me parece que asi esta bien !
catálogo exacto de SituacionAdministrativaActual: Esto no deberia reflejar los ya conocidos D1 a D8 o bandeja ? si hay que definirlo ahora !
catálogo exacto y definitivo de TipoEvento: Si, a ver. Esto esta direcmante relacionado con el paso de D1 a D9 o bandejas, yo creo que 
esta mas relacionado con el paso entre bandejas, son los eventos que hacen que el expediente de acta cambie de lugar o de estado. si deberiamos ya definirlos de echo creo que seria importante tenerlos ahora. como esta esta bien. Seria buena opcion que sea de catalogo, mi duda con respecto a que sean de catalogo es que si forman parte en algu momento de la logica de negocio entonces no deberian ser de catalogo prefiero 
que sean de enumeraciones constantes, es preferible en todo caso refactorizar y agregar nuevas. 
si TipoPersona necesita más opciones o puede quedar solo en 2 + otro: Si asi esta bien , en realidad solo hay dos FISICA o JURIDICA nada mas!
si IdInspector será siempre CHAR(36) o luego querés un INT interno adicional: Podriamos dejarlo como INT, y ademas no esta mal
ya que el IDP que va a usar le gente de faltas y los inspectrores, apunta a una tabla de usuarios con 
Id de usuario INT, Legajo INT, nombre INT y ademas son empleados del municipio, con usuario. Lo que si podriamos hacer 
y creo que es la mejor forma es que utilicen esa tenant para autenticarse, y guardemos en una tabla de forma automatica 
el snapshot de version del inspector, es decir: Inicia con el usuario, este usuario tiene la CLAIM inspector, entonces es inspector
pero si hay cambios en los datos del usuario inspector, se genera una nueva version del registro de ese usuario, la tabla de usuarios no lleva versionado, pero nosotros no vamos a leer los datos de la tabla maestra de usuarios, siempre vamos a leer la ultima version cuando hacemos el 
alta que lo que tiene es el snahpshot actual del usuario inspector. me explico ?, y si el tipo de dato del inspector 
va a ser siempre un int o smallint mas version ! el IDP resolvera y devolvera la claim de versionado junto con el id del inspector.
de echo esta afectara a todo ese tenant ! Solo tengo que tener la tabla y modificar el proceso de alta , modifiacion de datos del usuario para guardar el versionado cuando algo cambie, no si esta activbo o no, si si cambia legajo, dependencia o nombre o datos personales del usuario. Eso lo hago yo en el sistema que administra los usuarios.

si los IDs IGN/INDEC que pusimos como CHAR(8) deben ajustarse por fuente real exacta, los datos de IGN INDEC 
ahi te paso la estrctura real de hoy, falta el versionado de esas tablas.

geo_bahra_asentamiento (
    categoria CHAR(36) not null,
    departamento_id INT,
    departamento_nombre VARCHAR(48),
    fuente VARCHAR(36) not null,
    id VARCHAR(20) not null,
    localidad_censal_id INT8 not null,
    localidad_censal_nombre VARCHAR(64),
    municipio_id INT,
    municipio_nombre VARCHAR(64),
    nombre VARCHAR(84) not null,
    provincia_id SMALLINT not null,
    provincia_nombre VARCHAR(64) not null
)

geo_ign_departamento (
    id INT not null,
    fuente VARCHAR(12) not null,
    categoria CHAR(36) not null,
    nombre VARCHAR(48) not null,
    nombre_completo VARCHAR(64) not null,
    provincia_id SMALLINT not null,
    provincia_nombre VARCHAR(64) not null
)

geo_ign_municipio (
    id INT not null,
    categoria CHAR(26) not null,
    fuente VARCHAR(12) not null,
    nombre VARCHAR(64) not null,
    nombre_completo VARCHAR(64) not null,
    provincia_id SMALLINT not null,
    provincia_nombre VARCHAR(64) not null
)

geo_ign_provincia (
    id SMALLINT not null,
    categoria CHAR(26) not null,
    fuente VARCHAR(12) not null,
    iso_id VARCHAR(4) not null,
    iso_nombre VARCHAR(48) not null,
    nombre VARCHAR(64) not null,
    nombre_completo VARCHAR(84) not null
)

geo_indec_calles (
    categoria CHAR(36) not null,
    departamento_id INT,
    departamento_nombre VARCHAR(48),
    fuente VARCHAR(12) not null,
    id INT8 not null,
    localidad_censal_id INT8 not null,
    localidad_censal_nombre VARCHAR(64),
    nombre VARCHAR(64) not null,
    provincia_id SMALLINT not null,
    provincia_nombre VARCHAR(64) not null
)

geo_indec_localidad (
    id INT8,
    categoria CHAR(36) not null,
    departamento_id INT,
    departamento_nombre VARCHAR(48),
    fuente VARCHAR(12) not null,
    localidad_censal_id INT8 not null,
    localidad_censal_nombre VARCHAR(64),
    municipio_id INT,
    municipio_nombre VARCHAR(64),
    nombre VARCHAR(64) not null,
    provincia_id SMALLINT not null,
    provincia_nombre VARCHAR(64) not null
)

geo_indec_localidad_censal (
    id INT8 not null,
    categoria CHAR(36) not null,
    departamento_id INT,
    departamento_nombre VARCHAR(48),
    fuente VARCHAR(12) not null,
    municipio_id INT,
    municipio_nombre VARCHAR(64),
    nombre VARCHAR(64) not null,
    provincia_id SMALLINT not null,
    provincia_nombre VARCHAR(64) not null
)

geo_calle_alturas_barrio (
    id_tca CHAR(5) not null,
    alt_desde INT not null,
    alt_hasta INT not null,
    calle_nombre VARCHAR(64) not null,
    localidad_geo VARCHAR(64) not null,
    id_bar SMALLINT default 0 not null,
    barrio_geo VARCHAR(64) not null,
    SiEjidoUrbano SMALLINT default 0 not null,
    id_loc CHAR(2) default '' not null
)



----------------------------------------------------------------------------------------------------------------------


Yo agregaría al menos: SI MUY BIEN !

0 = ACTA_CREADA_EN_BORRADOR
1 = ACTA_LABRADA

Y además sumaría otro que seguramente te va a servir:

2 = BORRADOR_DESCARTADO

Inspector: Aca se me termina de acurrir algo y creo que es muy bueno, el inspector versionado si.
No modificams el IDP, solo incluirmos el ClaimInspectorId e inspectorVersion, que se obtiene de la tabla 
de inspectores. y en la tabla de inspectores le decimos cual el el usuario que tiene asignado para la aplicacion.
Que logramos con esto: Separamos de la autenticacion los inspectores.
Aislamos de la aurorizacion el versionado y lo dejamos en manos de claim constructor por decirlo del IDP que 
se fijata en ese tenant si es inspector y listo. unimos los dos mundos sin generar depenencias !

UN DATO con repecto a como nomnramos los campos.
ejemplos:
IdProvinciaInfractor -> IdProvInf
VersionDepartamentoInfractor -> VerDptoInf
IdLocalidadInfractor -> IdLocInf
IdCalleInfractor -> IdTca o Id_Tca
DeptoInfractor -> DeptoInf
ObservacionesDomicilioInfractor -> ObsDomInf
IdMunicipioLicencia -> IdMuni o IdMunLic
IdDepartamentoLicencia -> idDptoLic 
VersionDepartamentoLicencia -> VerDptoLic
etc...

Como puedes observar te doy estos ejemplos para que evalues una politica de nombramiento de campos, ademas 
en la base de datos actual en prod intentamos siempre que los campos no pierdan significicado pero que tampoco
sean tan grandes , es mas facil para escribir sql despues, es mas complicado para entender auiza al principio 
pero es mejor para despues me parece a mi. Campos tan grandes ademas son dificles de leer cuando tenes muchos juntos 
es mas facil leer a primera vista si estan mas resumidos y es mas cuando corresponda seprara las partes esas pequeñas con  un _
entonces es facil de leer en general. Lo que si es muy necesario para alguien que no esta acostumbrado o conoce 
la tabla que tenga en comenatrio u observaciones el texto minimo necesario que expresa el significado del nombre.

que opinas de esto, me parece importante para que empecemos bien con esto!


---------------------------------------------------------------------------------------------

Política que te propongo
1) Prefijos base
Id = identificador
Ver = versión
Fh = fecha/hora
Si = flag booleano lógico
Nro = número visible o administrativo
Obs = observación
Cant = cantidad
Cod = código
Nom = nombre
Desc = descripción... EXELENTE BYTE ESTO, pero ten en cuenta que van a aparecer campos que no entren dentro de esa politica
, entonces hay que hacerlos cortos con criterio !

2) Abreviaturas estables de entidades / conceptos: SI
3) Regla de composición: Perfecto

Para evitar eso yo haría:

...Infra = infracción , quiza Infr pues se distingue mejor al leerlo y no confunde con por ejemplo inf , de info, eso info no esta y seria de informacion , hay que agregar esa
...Inf = infractor, quiza ahi podria ser Infct ya que visualmente es mas declarativo

-----------------------------------------------------------------------------------------------

Regla fina que agregaría

Para que no se descontrole con el tiempo, yo cerraría también esto:

Orden recomendado del nombre

Prefijo + concepto + contexto
EXACTO byte 

esto lo debemos persistir en una regla general para todo el proyecto ya mismo , y dime donde la ponemos ademas si pudes 

----------------------------------------------------------------------------------------------




