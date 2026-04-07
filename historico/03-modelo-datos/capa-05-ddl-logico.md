# [ANEXO] DDL LÓGICO — CAPA 05 — ACTOS / DECISIÓN ADMINISTRATIVA FORMAL

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 05.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) FINALIDAD DE LA CAPA

La Capa 05 modela el acto administrativo formal dictado sobre una causa.

Debe resolver, de forma simple y consistente:

- fallo
- resolución
- disposición

La capa NO debe transformarse en:

- una capa de análisis
- una capa documental
- una capa de notificación
- una capa de firma
- una capa económica
- una máquina de estados compleja
- una estructura de reemplazo entre actos

---

# 2) CRITERIOS DE DISEÑO CONSOLIDADOS

## 2.1 Entidad única

Se adopta una única entidad base:

- `ActaActo`

No se crearán tablas separadas para:

- `Fallo`
- `Resolucion`
- `Disposicion`

La diferenciación se resolverá mediante:

- `TipoActo`

---

## 2.2 TipoActo define naturaleza formal

`TipoActo` solo expresa la naturaleza formal del acto.

Valores admitidos:

- `FALLO`
- `RESOLUCION`
- `DISPOSICION`

No debe utilizarse para expresar:

- contexto recursivo
- materia sobre medida
- efecto económico
- confirmación o modificación
- etapa del flujo

Eso pertenece al contexto del negocio, no al tipo formal del acto.

---

## 2.3 El acto no es el documento

El acto y el documento son objetos distintos.

- el **acto** es la decisión formal
- el **documento** es la pieza documental que la materializa

Por eso Capa 05 se vincula con Capa 02, pero no la reemplaza ni la duplica.

---

## 2.4 La firma no vive en esta capa

La firma del acto se resuelve a través del documento asociado y de su información de firma.

Por lo tanto, Capa 05 no modela:

- firma
- certificado
- sello
- numeración
- hash
- estado de firma

Eso ya pertenece a la capa documental.

---

## 2.5 El acto no reemplaza el historial

La existencia del acto no reemplaza el historial transversal.

Los hechos relevantes vinculados al acto deben reflejarse también en:

- `ActaEvento`

---

## 2.6 Estado mínimo y austero

El acto puede tener un estado mínimo, solo para expresar su situación administrativa más básica.

Valores admitidos:

- `EMITIDO`
- `ANULADO`

No se modelan estados como:

- `BORRADOR`
- `REEMPLAZADO`
- `FIRMADO`
- `NO_FIRMADO`

La firma no se resuelve aquí.

---

## 2.7 Sin reemplazo entre actos

No se incorpora relación formal de reemplazo entre actos.

Puede haber más de un acto en una misma causa, pero esta capa no modela:

- acto reemplazado
- acto vigente
- flags de sustitución
- cadenas de reemplazo

---

# 3) ENTIDAD DE LA CAPA

La Capa 05 queda compuesta, en principio, por una única entidad principal:

- `ActaActo`

---

# 4) TABLA: ActaActo

## 4.1 Finalidad

Representa un acto administrativo formal dictado sobre una causa.

Ejemplos:

- un fallo
- una resolución
- una disposición

---

## 4.2 PK

- `PK_ActaActo (Id)`

---

## 4.3 FK

- `FK_ActaActo_Acta (IdActa -> Acta.Id)`
- `FK_ActaActo_Documento (IdDocumentoPrincipal -> Documento.Id)`

---

## 4.4 Índices sugeridos

- `IX_ActaActo_IdActa`
- `IX_ActaActo_TipoActo`
- `IX_ActaActo_EstadoActo`
- `IX_ActaActo_FechaRegistro`
- `IX_ActaActo_IdActa_TipoActo`
- `IX_ActaActo_IdDocumentoPrincipal`

---

## 4.5 Campos

### 4.5.1 Identidad y relación base

- `Id` → INT8 NOT NULL  
  Identificador único del acto.

- `IdActa` → INT8 NOT NULL  
  Acta a la que pertenece el acto.

- `IdDocumentoPrincipal` → INT8 NOT NULL  
  Documento principal que materializa formalmente el acto.

---

### 4.5.2 Tipo de acto

- `TipoActo` → SMALLINT NOT NULL  
  Tipo formal del acto.

  Valores lógicos admitidos:

  - 1 = FALLO
  - 2 = RESOLUCION
  - 3 = DISPOSICION

---

### 4.5.3 Estado mínimo del acto

- `EstadoActo` → SMALLINT NOT NULL  
  Situación administrativa básica del acto.

  Valores lógicos admitidos:

  - 1 = EMITIDO
  - 2 = ANULADO

> Observación:
> La validez formal del acto no depende de este campo, sino del documento firmado asociado.

---

### 4.5.4 Datos operativos opcionales

- `ResumenActo` → VARCHAR(255) NULL  
  Descripción breve y operativa del acto.

- `ObservacionActo` → VARCHAR(255) NULL  
  Observación breve asociada al acto, si se requiere una nota operativa corta.

---

### 4.5.5 Fecha

- `FechaRegistro` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora en que el acto fue registrado en el sistema.

---

### 4.5.6 Trazabilidad operativa mínima

- `UsuarioRegistro` → VARCHAR(36) NOT NULL  
  Subject del usuario o identidad operativa que registró el acto.

- `OrigenRegistro` → SMALLINT NOT NULL  
  Origen técnico/operativo del alta.

  Valores previstos:

  - 1 = MANUAL
  - 2 = SISTEMA
  - 3 = PROCESO
  - 4 = INTEGRACION

---

## 4.6 Reglas lógicas

### Regla 1
Toda fila de `ActaActo` pertenece a una única `Acta`.

### Regla 2
Todo acto debe tener un único `TipoActo` válido.

### Regla 3
Todo acto debe tener un único `EstadoActo` válido.

### Regla 4
Todo acto debe vincularse a un `Documento` principal existente.

### Regla 5
Todo acto debe tener `FechaRegistro` y `UsuarioRegistro`.

### Regla 6
La capa no modela reemplazo entre actos.

### Regla 7
La capa no modela vigencia específica del acto.

### Regla 8
La capa no modela firma, notificación, análisis ni efectos económicos.

---

## 4.7 Observaciones de diseño

### Observación A
Se adopta una sola entidad (`ActaActo`) para mantener el modelo simple y extensible.

### Observación B
`TipoActo` queda deliberadamente acotado a tres tipos reales y claros:
- FALLO
- RESOLUCION
- DISPOSICION

### Observación C
`EstadoActo` no expresa si el acto está firmado o no firmado.  
Eso se obtiene del documento y su capa de firma.

### Observación D
No se incorpora referencia a acto previo, reemplazado o vigente porque ese modelo fue descartado expresamente.

---

# 5) VALIDACIONES LÓGICAS SUGERIDAS PARA ActaActo

## 5.1 Validación documental

`IdDocumentoPrincipal` debe existir siempre y debe referir a un documento válido de Capa 02.

## 5.2 Validación de tipo

Solo deben admitirse los tipos de acto definidos para la capa.

## 5.3 Validación de estado

Solo deben admitirse los estados mínimos definidos para la capa.

## 5.4 Validación de anulación

Si `EstadoActo = ANULADO`, la lógica de negocio debería reflejar también el hecho en:

- `ActaEvento`

y, si corresponde, en la capa documental.

# 6) INTEGRIDAD CRUZADA DE LA CAPA

## 6.1 Coherencia con Acta

- `ActaActo.IdActa` debe referir a una `Acta` existente.
- una misma `Acta` puede tener múltiples actos a lo largo de su ciclo de vida

Esto permite escenarios como:

- disposición previa
- resolución posterior
- fallo posterior

sin forzar relaciones artificiales de reemplazo.

---

## 6.2 Coherencia con Documento

`ActaActo.IdDocumentoPrincipal` debe referir a un `Documento` existente.

La capa 05 asume que ese documento es la pieza formal principal del acto.

Metadatos como:

- firma
- hash
- storage
- numeración
- sello
- anulación documental

se gobiernan exclusivamente en Capa 02.

---

## 6.3 Coherencia con DocumentoFirma

La firma no forma parte de `ActaActo`, pero la validez formal del acto depende del documento firmado.

Por eso, funcionalmente, la capa debe convivir con:

- `Documento`
- `DocumentoFirma`

sin duplicar sus datos ni su lógica.

---

## 6.4 Coherencia con ActaEvento

La capa no obliga físicamente la creación de `ActaEvento`, pero funcionalmente debe integrarse con el historial general.

Eventos típicos que podrían reflejarse desde lógica de negocio:

- ACTO_EMITIDO
- FALLO_EMITIDO
- RESOLUCION_EMITIDA
- DISPOSICION_EMITIDA
- ACTO_ANULADO

---

## 6.5 Coherencia con Notificación

Si el acto debe notificarse, esa notificación se resuelve en Capa 03.

La Capa 05 no modela:

- destinatarios
- canales
- intentos
- resultados
- acuses

pero sí debe poder convivir con la notificación posterior del documento/acto.

---

# 7) CATÁLOGOS / ENUMS LÓGICOS DE LA CAPA

> Estos catálogos se expresan aquí como referencia lógica.  
> La implementación física final podrá resolverse mediante:
>
> - constantes de aplicación
> - tablas de dominio
> - enum documentados
>
> según la política general del sistema.

---

## 7.1 TipoActo

- 1 = FALLO
- 2 = RESOLUCION
- 3 = DISPOSICION

---

## 7.2 EstadoActo

- 1 = EMITIDO
- 2 = ANULADO

---

## 7.3 OrigenRegistro

- 1 = MANUAL
- 2 = SISTEMA
- 3 = PROCESO
- 4 = INTEGRACION

---

# 8) REGLAS DE NEGOCIO TRANSVERSALES SUGERIDAS

## Regla A
Un acto administrativo formal siempre debe estar asociado a una `Acta`.

## Regla B
Un acto administrativo formal siempre debe tener un documento principal que lo materialice.

## Regla C
La existencia del acto no implica por sí sola que esté firmado; la validez formal debe leerse desde el documento y su firma.

## Regla D
La anulación de un acto debe quedar reflejada también en el historial transversal.

## Regla E
La existencia de más de un acto en una misma causa no implica relación formal de reemplazo entre ellos.

## Regla F
La capa 05 no reemplaza la capa documental ni la capa de notificación.

## Regla G
La capa 05 expresa el acto formal resultante de la decisión administrativa, no el análisis que lo precede.

---

# 9) CONSULTAS OPERATIVAS QUE LA CAPA DEBE HABILITAR

La capa debe permitir consultas como:

- actos de una causa
- último acto registrado de una causa
- actos por tipo
- fallos emitidos
- resoluciones emitidas
- disposiciones emitidas
- actos anulados
- acto principal asociado a un documento
- actos registrados por usuario
- actos registrados en un rango de fechas

---

# 10) CIERRE DEL ANEXO

La Capa 05 queda lógicamente definida como una capa simple de acto administrativo formal, basada en una única entidad:

- `ActaActo`

y apoyada en entidades ya existentes para:

- `Acta`
- `Documento`
- `DocumentoFirma`
- `ActaEvento`

El diseño evita:

- una tabla por tipo de acto
- reemplazos artificiales entre actos
- modelado redundante de firma
- modelado redundante de notificación
- estados innecesarios
- sobre-diseño del tramo resolutivo

Su función es concreta:

👉 **capturar el acto administrativo formal dictado sobre la causa**

