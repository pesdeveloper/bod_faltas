# [ANEXO] DDL LÓGICO — CAPA 07 — TRAMO RECURSIVO / ELEVACIÓN / RESULTADO EXTERNO

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 07.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) FINALIDAD DE LA CAPA

La Capa 07 modela el tramo recursivo de la causa.

Debe permitir registrar, de forma estructurada:

- interposición del recurso
- improcedencia
- concesión
- elevación
- recepción de resultado externo
- cierre del tramo recursivo

La capa NO debe transformarse en:

- una capa de actos
- una capa documental
- una capa de notificación
- una capa judicial completa
- una máquina de estados compleja

---

# 2) CRITERIOS DE DISEÑO CONSOLIDADOS

## 2.1 Entidad única

Se adopta una única entidad base:

- `ActaRecurso`

No se crearán tablas separadas para:

- apelación
- concesión
- elevación
- resultado
- cierre

---

## 2.2 No duplicación de capas existentes

La capa no duplica:

- actos (`ActaActo`)
- documentos (`Documento`)
- notificaciones (`Notificacion`)
- eventos (`ActaEvento`)
- presentaciones (`ActaPresentacion`)

El tramo recursivo se apoya en ellas, pero no las reemplaza.

---

## 2.3 Estado mínimo y operativo

La capa necesita un estado mínimo para lectura operativa rápida.

No pretende reemplazar el historial ni modelar workflow judicial detallado.

---

## 2.4 El recurso no es el acto

La existencia de un recurso o de un resultado recursivo puede dar lugar a actos posteriores, pero el recurso no es un acto en sí mismo dentro de esta capa.

---

## 2.5 El recurso no es el documento

Los escritos, resoluciones, constancias o resultados documentales asociados al tramo recursivo viven en Capa 02.

---

# 3) ENTIDAD DE LA CAPA

La Capa 07 queda compuesta, en principio, por una única entidad principal:

- `ActaRecurso`

---

# 4) TABLA: ActaRecurso

## 4.1 Finalidad

Representa el tramo recursivo asociado a una causa.

Ejemplos:

- apelación interpuesta
- apelación improcedente
- apelación concedida y elevada
- recurso con resultado externo recibido
- tramo recursivo cerrado

---

## 4.2 PK

- `PK_ActaRecurso (Id)`

---

## 4.3 FK

- `FK_ActaRecurso_Acta (IdActa -> Acta.Id)`

---

## 4.4 Índices sugeridos

- `IX_ActaRecurso_IdActa`
- `IX_ActaRecurso_TipoRecurso`
- `IX_ActaRecurso_EstadoRecurso`
- `IX_ActaRecurso_FechaInterposicion`
- `IX_ActaRecurso_FechaResultado`
- `IX_ActaRecurso_FechaCierre`
- `IX_ActaRecurso_IdActa_EstadoRecurso`

---

## 4.5 Campos

### 4.5.1 Identidad y relación base

- `Id` → INT8 NOT NULL  
  Identificador único del tramo recursivo.

- `IdActa` → INT8 NOT NULL  
  Acta a la que pertenece el recurso.

---

### 4.5.2 Tipo de recurso

- `TipoRecurso` → SMALLINT NOT NULL  
  Tipo de recurso.

  Valores lógicos admitidos:

  - 1 = APELACION

---

### 4.5.3 Estado del recurso

- `EstadoRecurso` → SMALLINT NOT NULL  
  Estado operativo actual del tramo recursivo.

  Valores lógicos admitidos:

  - 1 = INTERPUESTO
  - 2 = IMPROCEDENTE
  - 3 = CONCEDIDO
  - 4 = ELEVADO
  - 5 = RESULTADO_RECIBIDO
  - 6 = CERRADO

---

### 4.5.4 Fechas

- `FechaInterposicion` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora en que se registra la interposición del recurso.

- `FechaResultado` → DATETIME YEAR TO SECOND NULL  
  Fecha/hora en que se registra la recepción del resultado externo, si corresponde.

- `FechaCierre` → DATETIME YEAR TO SECOND NULL  
  Fecha/hora de cierre del tramo recursivo, si corresponde.

---

### 4.5.5 Contenido operativo breve

- `ObservacionRecurso` → VARCHAR(255) NULL  
  Observación breve y operativa del tramo recursivo.

---

### 4.5.6 Trazabilidad operativa mínima

- `UsuarioRegistro` → VARCHAR(36) NOT NULL  
  Subject del usuario o identidad operativa que registró el recurso.

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
Toda fila de `ActaRecurso` pertenece a una única `Acta`.

### Regla 2
Toda fila debe tener un `TipoRecurso` válido.

### Regla 3
Toda fila debe tener un `EstadoRecurso` válido.

### Regla 4
Todo tramo recursivo debe tener `FechaInterposicion`.

### Regla 5
`FechaResultado` solo debería informarse cuando exista recepción de resultado externo o equivalente.

### Regla 6
`FechaCierre` solo debería informarse cuando el tramo recursivo haya finalizado operativamente.

### Regla 7
La capa no modela reemplazos entre recursos.

### Regla 8
La capa no modela el organismo externo como subdominio completo.

---

## 4.7 Observaciones de diseño

### Observación A
Se adopta una sola entidad (`ActaRecurso`) para mantener la capa simple y consistente con el resto del modelo.

### Observación B
`TipoRecurso` queda deliberadamente mínimo: solo `APELACION`, hasta que exista otro tipo real que justifique ampliarlo.

### Observación C
El estado del recurso no reemplaza el historial transversal; solo aporta lectura operativa rápida.

### Observación D
No se agregan referencias obligatorias a actos, documentos o notificaciones porque esas relaciones se resuelven desde sus propias capas.

---

# 5) VALIDACIONES LÓGICAS SUGERIDAS PARA ActaRecurso

## 5.1 Validación de cierre

Si `EstadoRecurso = CERRADO`, normalmente debería existir `FechaCierre`.

## 5.2 Validación de resultado

Si `EstadoRecurso = RESULTADO_RECIBIDO`, normalmente debería existir `FechaResultado`.

## 5.3 Validación de secuencia

La lógica de negocio debería evitar secuencias inconsistentes, por ejemplo:

- `RESULTADO_RECIBIDO` sin haber existido antes un tramo recursivo registrado
- `ELEVADO` sin previa admisión/concesión, salvo que el proceso operativo defina otro camino

Estas validaciones no necesariamente deben quedar como constraints físicas en esta etapa.

---

# 6) INTEGRIDAD CRUZADA DE LA CAPA

## 6.1 Coherencia con Acta

- `ActaRecurso.IdActa` debe referir a una `Acta` existente.
- una `Acta` puede tener más de un recurso a lo largo del tiempo

---

## 6.2 Coherencia con Documento

Los documentos asociados al tramo recursivo viven en Capa 02.

Ejemplos:

- escrito de apelación
- resolución de improcedencia
- resolución de concesión
- constancia de elevación
- resultado externo recibido

La capa 07 no los duplica ni los modela como tabla propia.

---

## 6.3 Coherencia con ActaActo

El tramo recursivo puede dar lugar a actos posteriores, pero no depende estructuralmente de ellos dentro de esta capa.

---

## 6.4 Coherencia con Notificación

La notificación del recurso o de su resultado se resuelve en Capa 03.

La Capa 07 no modela destinatarios, canales, intentos ni resultados de notificación.

---

## 6.5 Coherencia con ActaEvento

La capa no obliga físicamente la creación de `ActaEvento`, pero funcionalmente debe integrarse con el historial general.

Eventos típicos que podrían reflejarse desde lógica de negocio:

- RECURSO_INTERPUESTO
- RECURSO_IMPROCEDENTE
- RECURSO_CONCEDIDO
- RECURSO_ELEVADO
- RESULTADO_RECIBIDO
- TRAMO_RECURSIVO_CERRADO

---

# 7) CATÁLOGOS / ENUMS LÓGICOS DE LA CAPA

## 7.1 TipoRecurso

- 1 = APELACION

---

## 7.2 EstadoRecurso

- 1 = INTERPUESTO
- 2 = IMPROCEDENTE
- 3 = CONCEDIDO
- 4 = ELEVADO
- 5 = RESULTADO_RECIBIDO
- 6 = CERRADO

---

## 7.3 OrigenRegistro

- 1 = MANUAL
- 2 = SISTEMA
- 3 = PROCESO
- 4 = INTEGRACION

---

# 8) REGLAS DE NEGOCIO TRANSVERSALES SUGERIDAS

## Regla A
Todo tramo recursivo debe estar asociado a una `Acta`.

## Regla B
La interposición del recurso puede ingresar por presentación previa, pero Capa 07 comienza cuando el sistema lo registra como trámite recursivo formal.

## Regla C
La existencia de un recurso no reemplaza la necesidad de actos, documentos o notificaciones posteriores cuando correspondan.

## Regla D
El resultado del tramo recursivo puede dar lugar a actos posteriores, pero no se modela aquí como acto.

## Regla E
La capa 07 no modela el organismo externo en detalle; solo el seguimiento del tramo recursivo.

---

# 9) CONSULTAS OPERATIVAS QUE LA CAPA DEBE HABILITAR

La capa debe permitir consultas como:

- recursos de una causa
- último recurso de una causa
- recursos interpuestos
- recursos concedidos
- recursos elevados
- recursos con resultado recibido
- recursos cerrados
- recursos registrados por usuario
- recursos en rango de fechas

---

# 10) CIERRE DEL ANEXO

La Capa 07 queda lógicamente definida como una capa simple de seguimiento del tramo recursivo, basada en una única entidad:

- `ActaRecurso`

Su función es concreta:

- registrar la existencia del recurso
- permitir lectura operativa de su estado
- acompañar su elevación y su resultado
- integrarse con actos, documentos, notificaciones y eventos sin duplicarlos

👉 Capa 07 no modela el sistema judicial: **modela el seguimiento recursivo de la causa**.