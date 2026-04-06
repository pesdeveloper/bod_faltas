# [ANEXO] DDL LÓGICO — CAPA 04 — PRESENTACIONES E INTERACCIÓN ADMINISTRATIVA

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 04.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) FINALIDAD DE LA CAPA

La Capa 04 modela el ingreso administrativo estructurado de interacciones vinculadas a un acta.

Debe resolver, de forma simple y práctica:

- comparecencia espontánea
- descargo
- constitución, ratificación o rectificación de domicilio
- aporte documental
- solicitudes
- notas administrativas
- manifestaciones
- presentaciones de terceros

La capa NO debe transformarse en:

- un mini workflow paralelo
- una capa resolutiva
- una capa económica
- una capa documental duplicada
- una capa de notificación
- un expediente interno paralelo

---

# 2) CRITERIOS DE DISEÑO CONSOLIDADOS

## 2.1 Entidad base única

Se adopta una única entidad base:

- `ActaPresentacion`

No se crearán tablas separadas para:

- descargos
- comparecencias
- solicitudes
- constituciones de domicilio
- aportes documentales
- notas
- presentaciones de terceros

La diferenciación se resolverá con un único campo tipificado:

- `TipoPresentacion`

---

## 2.2 Sin workflow propio

La presentación NO tendrá:

- estado de tratamiento
- resultado administrativo
- fechas de tratamiento
- fechas de cierre
- derivaciones internas complejas

La evolución real de lo presentado se reflejará, cuando corresponda, mediante:

- `ActaEvento`
- `Documento`
- `ActaPresentacionObservacion`
- lógica del flujo D1–D8
- capas posteriores

---

## 2.3 Documento se reutiliza

Los escritos, adjuntos, constancias, escaneos o PDFs asociados a una presentación viven en:

- `Documento`

La capa 04 únicamente los vincula.

---

## 2.4 Domicilio se reutiliza

Cuando una presentación se refiera a domicilio:

- el domicilio utilizable sigue viviendo en `ActaDomicilio`
- `ActaPresentacion` solo podrá referenciarlo

---

## 2.5 Historial transversal no se reemplaza

La presentación registra el ingreso administrativo.

Los efectos transversales relevantes deben seguir reflejándose en:

- `ActaEvento`

---

# 3) ENTIDADES DE LA CAPA

La Capa 04 queda compuesta por:

- `ActaPresentacion`
- `ActaPresentacionDocumento`
- `ActaPresentacionObservacion`

---

# 4) TABLA: ActaPresentacion

## 4.1 Finalidad

Representa una presentación o interacción administrativa concreta vinculada a un acta.

Ejemplos:

- comparecencia espontánea
- descargo
- constitución de domicilio
- rectificación de domicilio
- ratificación de domicilio
- aporte documental
- solicitud de pago voluntario
- solicitud de prórroga
- solicitud de vista
- solicitud de copia
- nota administrativa
- manifestación
- presentación de tercero

---

## 4.2 PK

- `PK_ActaPresentacion (Id)`

---

## 4.3 FK

- `FK_ActaPresentacion_Acta (IdActa -> Acta.Id)`
- `FK_ActaPresentacion_ActaDomicilio (IdActaDomicilio -> ActaDomicilio.Id)` nullable

---

## 4.4 Índices sugeridos

- `IX_ActaPresentacion_IdActa`
- `IX_ActaPresentacion_TipoPresentacion`
- `IX_ActaPresentacion_FechaRegistro`
- `IX_ActaPresentacion_CanalPresentacion`
- `IX_ActaPresentacion_IdActa_TipoPresentacion`

---

## 4.5 Campos

### 4.5.1 Identidad y relación base

- `Id` → INT8 NOT NULL  
  Identificador único de la presentación.

- `IdActa` → INT8 NOT NULL  
  Acta a la que pertenece la presentación.

- `IdActaDomicilio` → INT8 NULL  
  Domicilio vinculado, solo si la presentación refiere a domicilio.

---

### 4.5.2 Tipo de presentación

- `TipoPresentacion` → SMALLINT NOT NULL  
  Tipo único de presentación, resuelto por catálogo cerrado.

  Valores lógicos sugeridos:

  - 1 = COMPARECENCIA_ESPONTANEA
  - 2 = DESCARGO
  - 3 = CONSTITUCION_DOMICILIO
  - 4 = RATIFICACION_DOMICILIO
  - 5 = RECTIFICACION_DOMICILIO
  - 6 = APORTE_DOCUMENTAL
  - 7 = SOLICITUD_PAGO_VOLUNTARIO
  - 8 = SOLICITUD_PRORROGA
  - 9 = SOLICITUD_VISTA
  - 10 = SOLICITUD_COPIA
  - 11 = NOTA_ADMINISTRATIVA
  - 12 = MANIFESTACION
  - 13 = PRESENTACION_TERCERO
  - 99 = OTRO_CONTROLADO

---

### 4.5.3 Canal

- `CanalPresentacion` → SMALLINT NOT NULL  
  Canal real de ingreso.

  Valores previstos:

  - 1 = PRESENCIAL
  - 2 = DIGITAL

---

### 4.5.4 Presentante

- `CaracterPresentante` → SMALLINT NOT NULL  
  Carácter del presentante, tipificado.

  Valores lógicos sugeridos:

  - 1 = INFRACTOR
  - 2 = TITULAR
  - 3 = APODERADO
  - 4 = ABOGADO
  - 5 = TERCERO_INTERESADO
  - 6 = AGENTE_MUNICIPAL
  - 7 = AUTORIDAD
  - 99 = OTRO_CONTROLADO

- `PresentanteNombre` → VARCHAR(150) NULL  
  Nombre visible o identificador operativo del presentante.

- `PresentanteTipoDocumento` → SMALLINT NULL  
  Tipo documental básico, si corresponde.

- `PresentanteNumeroDocumento` → VARCHAR(20) NULL  
  Número documental del presentante, si corresponde.

- `PresentanteContacto` → VARCHAR(100) NULL  
  Dato de contacto resumido, si se requiere registrarlo.

---

### 4.5.5 Fecha

- `FechaRegistro` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora en que la presentación fue registrada en el sistema.

---

### 4.5.6 Contenido operativo

- `ResumenPresentacion` → VARCHAR(255) NULL  
  Resumen corto y operativo de la presentación.

---

### 4.5.7 Trazabilidad operativa mínima

- `UsuarioRegistro` → VARCHAR(36) NOT NULL  
  Subject del usuario o identidad operativa que registró la presentación.

- `OrigenRegistro` → SMALLINT NOT NULL  
  Origen técnico/operativo del alta.

  Valores previstos:

  - 1 = MANUAL
  - 2 = SISTEMA
  - 3 = PROCESO
  - 4 = INTEGRACION

---

### 4.5.8 Snapshots operativos simples

- `TieneDocumentos` → SMALLINT NOT NULL DEFAULT 0  
  Snapshot operativo.

  Valores:
  - 0 = NO
  - 1 = SI

- `TieneDomicilioVinculado` → SMALLINT NOT NULL DEFAULT 0  
  Snapshot operativo.

  Valores:
  - 0 = NO
  - 1 = SI

---

## 4.6 Reglas lógicas

### Regla 1
Toda fila de `ActaPresentacion` pertenece a una única `Acta`.

### Regla 2
Toda fila debe tener:

- `TipoPresentacion`
- `CanalPresentacion`
- `CaracterPresentante`
- `FechaRegistro`
- `UsuarioRegistro`

### Regla 3
`IdActaDomicilio` es nullable y solo se usa cuando la presentación refiere a domicilio.

### Regla 4
`TieneDomicilioVinculado = 1` implica que `IdActaDomicilio` no debe ser null.

### Regla 5
`TieneDocumentos = 1` implica la existencia de al menos una fila relacionada en `ActaPresentacionDocumento`.

### Regla 6
La presentación puede existir sin documentos.

### Regla 7
La presentación puede existir sin domicilio vinculado.

### Regla 8
La presentación no modela por sí misma admisión, rechazo, subsanación, resolución ni cierre de trámite.

---

## 4.7 Observaciones de diseño

### Observación A
Se eliminó el esquema `TipoPresentacion + SubtipoPresentacion` por complejidad innecesaria.

### Observación B
Se eliminó `CanalPresentacion` ampliado y se dejó solo lo realmente útil para esta capa: `PRESENCIAL` y `DIGITAL`.

### Observación C
Se eliminaron `NivelFormalidad`, `EstadoTratamiento`, `ResultadoPresentacion`, derivaciones y fechas de tratamiento/cierre para evitar workflow paralelo.

### Observación D
`UsuarioRegistro` se modela como subject (`VARCHAR(36)`) para mantener consistencia con el resto del sistema.

### Observación E
`ResumenPresentacion` se reduce a `VARCHAR(255)` porque esta capa requiere una síntesis corta y operativa, no texto extenso.

---

# 5) VALIDACIONES LÓGICAS SUGERIDAS PARA ActaPresentacion

## 5.1 Validación de domicilio

Si `TipoPresentacion` es alguno de:

- CONSTITUCION_DOMICILIO
- RATIFICACION_DOMICILIO
- RECTIFICACION_DOMICILIO

entonces normalmente debería existir `IdActaDomicilio`, salvo cargas preliminares pendientes de normalización.

## 5.2 Validación de presentante

`CaracterPresentante` debe existir siempre, aunque `PresentanteNombre` pueda ser null en algunos escenarios de registración interna o integración.

## 5.3 Validación documental

La existencia o no de documentos asociados no debe condicionar la validez básica de la presentación, salvo reglas particulares de negocio que puedan imponerse más adelante para ciertos tipos.

## 5.4 Validación de snapshot documental

Si existe al menos una fila relacionada en `ActaPresentacionDocumento`, el sistema debería reflejar:

- `TieneDocumentos = 1`

## 5.5 Validación de snapshot de domicilio

Si `IdActaDomicilio` no es null, el sistema debería reflejar:

- `TieneDomicilioVinculado = 1`

---

# 6) TABLA: ActaPresentacionDocumento

## 6.1 Finalidad

Tabla puente para vincular una presentación con uno o varios documentos de Capa 02.

Se utiliza para resolver escenarios como:

- descargo con escrito y anexos
- aporte documental con múltiples piezas
- solicitud con formulario y constancia
- presentación con documentación complementaria
- nota administrativa con constancia generada por el sistema

---

## 6.2 PK

- `PK_ActaPresentacionDocumento (Id)`

---

## 6.3 FK

- `FK_ActaPresentacionDocumento_ActaPresentacion (IdActaPresentacion -> ActaPresentacion.Id)`
- `FK_ActaPresentacionDocumento_Documento (IdDocumento -> Documento.Id)`

---

## 6.4 Unique sugerida

- `UQ_ActaPresentacionDocumento (IdActaPresentacion, IdDocumento)`

Esta restricción evita duplicar la misma vinculación documental dentro de una misma presentación.

---

## 6.5 Índices sugeridos

- `IX_ActaPresentacionDocumento_IdActaPresentacion`
- `IX_ActaPresentacionDocumento_IdDocumento`
- `IX_ActaPresentacionDocumento_TipoVinculoDocumento`

---

## 6.6 Campos

### 6.6.1 Identidad y relaciones

- `Id` → INT8 NOT NULL  
  Identificador único de la relación.

- `IdActaPresentacion` → INT8 NOT NULL  
  Presentación a la que pertenece la relación documental.

- `IdDocumento` → INT8 NOT NULL  
  Documento vinculado.

---

### 6.6.2 Semántica de la vinculación

- `TipoVinculoDocumento` → SMALLINT NOT NULL  
  Tipo de vínculo funcional del documento respecto de la presentación.

  Valores lógicos sugeridos:

  - 1 = PRINCIPAL
  - 2 = ANEXO
  - 3 = RESPALDO
  - 4 = CONSTANCIA
  - 99 = OTRO_CONTROLADO

- `Orden` → SMALLINT NOT NULL DEFAULT 1  
  Orden lógico del documento dentro del conjunto documental de la presentación.

- `EsPrincipal` → SMALLINT NOT NULL DEFAULT 0  
  Snapshot operativo.

  Valores:
  - 0 = NO
  - 1 = SI

---

### 6.6.3 Trazabilidad mínima

- `FechaRegistro` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora en que se registró la vinculación del documento.

- `UsuarioRegistro` → VARCHAR(36) NOT NULL  
  Subject del usuario o identidad operativa que realizó la vinculación.

- `OrigenRegistro` → SMALLINT NOT NULL  
  Origen técnico/operativo del alta.

  Valores previstos:

  - 1 = MANUAL
  - 2 = SISTEMA
  - 3 = PROCESO
  - 4 = INTEGRACION

---

## 6.7 Reglas lógicas

### Regla 1
Toda fila de `ActaPresentacionDocumento` pertenece a una única `ActaPresentacion`.

### Regla 2
Toda fila vincula exactamente un `Documento`.

### Regla 3
No debe repetirse el mismo `IdDocumento` dentro de la misma `ActaPresentacion`.

### Regla 4
Una presentación puede tener múltiples documentos.

### Regla 5
Una presentación puede no tener documentos y seguir siendo válida a nivel de negocio.

### Regla 6
`EsPrincipal = 1` debería existir como máximo una vez por cada `IdActaPresentacion`, salvo que en el futuro se defina lo contrario.

---

## 6.8 Observaciones de diseño

### Observación A
Se mantiene `TipoVinculoDocumento` porque sí aporta valor práctico para distinguir documento principal, anexo, respaldo o constancia.

### Observación B
No se duplican aquí metadatos documentales como hash, firma, storage, numeración o estado del documento, porque eso ya pertenece a `Documento`.

### Observación C
La tabla se mantiene liviana y estrictamente relacional.

---

# 7) TABLA: ActaPresentacionObservacion

## 7.1 Finalidad

Registra observaciones o notas administrativas breves propias de la presentación.

No reemplaza:

- `ActaObservacion` de Capa 01
- `ActaEvento`
- documentos formales
- resoluciones posteriores

Sirve para anotar seguimiento corto y específico de una presentación.

Ejemplos:

- faltan anexos
- se acompaña documentación complementaria
- se deja constancia de revisión administrativa
- se aclara contexto de registración
- se deja una nota breve sobre la presentación

---

## 7.2 PK

- `PK_ActaPresentacionObservacion (Id)`

---

## 7.3 FK

- `FK_ActaPresentacionObservacion_ActaPresentacion (IdActaPresentacion -> ActaPresentacion.Id)`

---

## 7.4 Índices sugeridos

- `IX_ActaPresentacionObservacion_IdActaPresentacion`
- `IX_ActaPresentacionObservacion_TipoObservacion`
- `IX_ActaPresentacionObservacion_FechaRegistro`

---

## 7.5 Campos

### 7.5.1 Identidad y relación

- `Id` → INT8 NOT NULL  
  Identificador único de la observación.

- `IdActaPresentacion` → INT8 NOT NULL  
  Presentación a la que pertenece la observación.

---

### 7.5.2 Clasificación

- `TipoObservacion` → SMALLINT NOT NULL  
  Tipo de observación breve.

  Valores lógicos sugeridos:

  - 1 = INTERNA
  - 2 = ACLARACION
  - 3 = REQUERIMIENTO
  - 4 = CONSTANCIA
  - 99 = OTRO_CONTROLADO

---

### 7.5.3 Contenido y trazabilidad

- `Observacion` → VARCHAR(255) NOT NULL  
  Texto breve de la observación.

- `FechaRegistro` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora de registro de la observación.

- `UsuarioRegistro` → VARCHAR(36) NOT NULL  
  Subject del usuario o identidad operativa que registró la observación.

- `OrigenRegistro` → SMALLINT NOT NULL  
  Origen técnico/operativo del alta.

  Valores previstos:

  - 1 = MANUAL
  - 2 = SISTEMA
  - 3 = PROCESO
  - 4 = INTEGRACION

---

## 7.6 Reglas lógicas

### Regla 1
Toda observación de presentación pertenece a una única `ActaPresentacion`.

### Regla 2
Una presentación puede tener múltiples observaciones.

### Regla 3
La observación de presentación no reemplaza ni duplica el historial transversal del sistema.

### Regla 4
La observación de presentación debe mantenerse corta y operativa.

---

## 7.7 Observaciones de diseño

### Observación A
Se simplifica fuertemente respecto de la versión anterior: no hay cierre, estado activo ni motivo de cierre.

### Observación B
Se usa `VARCHAR(255)` porque esta tabla debe contener notas breves y prácticas, no desarrollos largos.

### Observación C
Se mantiene separada de `ActaObservacion` porque aquí el foco es la presentación específica, no el acta general.

---

# 8) INTEGRIDAD CRUZADA DE LA CAPA

## 8.1 Coherencia con Acta

- `ActaPresentacion.IdActa` debe referir a una `Acta` existente.
- Toda observación y toda relación documental de la capa dependen indirectamente de esa misma `Acta` a través de `ActaPresentacion`.

---

## 8.2 Coherencia con ActaDomicilio

Si `ActaPresentacion.IdActaDomicilio` no es null:

- el domicilio debe existir
- debería ser coherente con la misma causa o con la misma `Acta`, según las reglas de integridad de Capa 01

---

## 8.3 Coherencia con Documento

Todo `IdDocumento` de `ActaPresentacionDocumento` debe existir en `Documento`.

La vigencia, estado, firma, hash, storage o anulación del documento se gobierna exclusivamente en Capa 02.

---

## 8.4 Coherencia con eventos transversales

La capa no obliga físicamente la creación de `ActaEvento`, pero funcionalmente debe integrarse con el historial general.

Eventos típicos que podrían reflejarse desde lógica de negocio:

- PRESENTACION_REGISTRADA
- COMPARECENCIA_ESPONTANEA_REGISTRADA
- DESCARGO_REGISTRADO
- DOMICILIO_CONSTITUIDO
- DOMICILIO_RATIFICADO
- DOMICILIO_RECTIFICADO
- APORTE_DOCUMENTAL_REGISTRADO
- SOLICITUD_PAGO_VOLUNTARIO_REGISTRADA
- SOLICITUD_PRORROGA_REGISTRADA
- SOLICITUD_VISTA_REGISTRADA
- SOLICITUD_COPIA_REGISTRADA
- NOTA_ADMINISTRATIVA_REGISTRADA
- PRESENTACION_TERCERO_REGISTRADA

---

# 9) CATÁLOGOS / ENUMS LÓGICOS DE LA CAPA

> Estos catálogos se expresan aquí como referencia lógica.  
> La implementación física final podrá resolverse mediante:
>
> - constantes de aplicación
> - tablas de dominio
> - enum documentados
>
> según la política general del sistema.

---

## 9.1 TipoPresentacion

- 1 = COMPARECENCIA_ESPONTANEA
- 2 = DESCARGO
- 3 = CONSTITUCION_DOMICILIO
- 4 = RATIFICACION_DOMICILIO
- 5 = RECTIFICACION_DOMICILIO
- 6 = APORTE_DOCUMENTAL
- 7 = SOLICITUD_PAGO_VOLUNTARIO
- 8 = SOLICITUD_PRORROGA
- 9 = SOLICITUD_VISTA
- 10 = SOLICITUD_COPIA
- 11 = NOTA_ADMINISTRATIVA
- 12 = MANIFESTACION
- 13 = PRESENTACION_TERCERO
- 99 = OTRO_CONTROLADO

---

## 9.2 CanalPresentacion

- 1 = PRESENCIAL
- 2 = DIGITAL

---

## 9.3 CaracterPresentante

- 1 = INFRACTOR
- 2 = TITULAR
- 3 = APODERADO
- 4 = ABOGADO
- 5 = TERCERO_INTERESADO
- 6 = AGENTE_MUNICIPAL
- 7 = AUTORIDAD
- 99 = OTRO_CONTROLADO

---

## 9.4 TipoVinculoDocumento

- 1 = PRINCIPAL
- 2 = ANEXO
- 3 = RESPALDO
- 4 = CONSTANCIA
- 99 = OTRO_CONTROLADO

---

## 9.5 TipoObservacion

- 1 = INTERNA
- 2 = ACLARACION
- 3 = REQUERIMIENTO
- 4 = CONSTANCIA
- 99 = OTRO_CONTROLADO

---

## 9.6 OrigenRegistro

- 1 = MANUAL
- 2 = SISTEMA
- 3 = PROCESO
- 4 = INTEGRACION

---

# 10) REGLAS DE NEGOCIO TRANSVERSALES SUGERIDAS

## Regla A
Una comparecencia espontánea puede existir sin documentos asociados.

## Regla B
Un descargo puede existir con o sin documento, según el canal y la modalidad operativa.

## Regla C
Una constitución, ratificación o rectificación de domicilio debería tender a vincular `IdActaDomicilio`, salvo carga preliminar pendiente de normalización.

## Regla D
Una solicitud registrada no implica concesión automática.

## Regla E
La existencia de documentos asociados no reemplaza la necesidad de registrar la presentación cuando el ingreso administrativo tenga entidad propia.

## Regla F
La capa 04 no reemplaza el historial event-driven.

## Regla G
La capa 04 no reemplaza el proceso documental ni el proceso de notificación.

---

# 11) CIERRE DEL ANEXO

La Capa 04 queda lógicamente definida como una capa simple de ingreso administrativo estructurado, basada en una entidad genérica (`ActaPresentacion`) y apoyada en relaciones reutilizables con:

- `Documento`
- `ActaDomicilio`

y con trazabilidad específica mínima mediante:

- `ActaPresentacionObservacion`

El diseño evita:

- proliferación de tablas por subtipo
- duplicación documental
- duplicación de domicilio
- workflow paralelo innecesario
- invasión sobre la lógica resolutiva, económica o de notificación
- pérdida de integración con el historial event-driven
