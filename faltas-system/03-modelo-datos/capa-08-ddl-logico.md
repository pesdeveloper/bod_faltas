# [ANEXO] DDL LÓGICO — CAPA 08 — APREMIO / DERIVACIÓN EXTERNA / RESULTADO EXTERNO

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 08.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) FINALIDAD DE LA CAPA

La Capa 08 modela el tramo externo de la causa cuando esta es derivada a apremio o al Juzgado de Paz.

Debe permitir registrar, de forma estructurada:

- derivación externa
- gestión externa en curso
- recepción de resultado externo
- tipo de resultado recibido
- cierre del tramo externo

La capa NO debe transformarse en:

- una capa judicial externa completa
- una capa de actos
- una capa documental
- una capa de notificación
- una máquina de estados compleja

---

# 2) CRITERIOS DE DISEÑO CONSOLIDADOS

## 2.1 Entidad única

Se adopta una única entidad base:

- `ActaDerivacionExterna`

No se crearán tablas separadas para:

- apremio
- juzgado
- resultado externo
- cierre externo

---

## 2.2 No duplicación de capas existentes

La capa no duplica:

- actos (`ActaActo`)
- documentos (`Documento`)
- notificaciones (`Notificacion`)
- eventos (`ActaEvento`)
- recurso (`ActaRecurso`)

---

## 2.3 Estado mínimo y operativo

La capa necesita un estado mínimo para lectura operativa rápida.

No pretende modelar el proceso externo completo.

---

## 2.4 Resultado externo tipificado

La capa debe poder distinguir no solo que hubo resultado, sino también cuál fue ese resultado.

Esto aporta lectura operativa real sin obligar a modelar el sistema externo completo.

---

## 2.5 La derivación no es el acto

La derivación o su resultado pueden dar lugar a actos posteriores, pero no son actos en sí mismos dentro de esta capa.

---

# 3) ENTIDAD DE LA CAPA

La Capa 08 queda compuesta, en principio, por una única entidad principal:

- `ActaDerivacionExterna`

---

# 4) TABLA: ActaDerivacionExterna

## 4.1 Finalidad

Representa el tramo externo asociado a una causa.

Ejemplos:

- derivación a apremio
- derivación a Juzgado de Paz
- resultado externo recibido
- tramo externo cerrado

---

## 4.2 PK

- `PK_ActaDerivacionExterna (Id)`

---

## 4.3 FK

- `FK_ActaDerivacionExterna_Acta (IdActa -> Acta.Id)`

---

## 4.4 Índices sugeridos

- `IX_ActaDerivacionExterna_IdActa`
- `IX_ActaDerivacionExterna_TipoDerivacionExterna`
- `IX_ActaDerivacionExterna_EstadoDerivacionExterna`
- `IX_ActaDerivacionExterna_ResultadoDerivacionExterna`
- `IX_ActaDerivacionExterna_FechaDerivacion`
- `IX_ActaDerivacionExterna_FechaResultado`
- `IX_ActaDerivacionExterna_FechaCierre`
- `IX_ActaDerivacionExterna_IdActa_EstadoDerivacionExterna`

---

## 4.5 Campos

### 4.5.1 Identidad y relación base

- `Id` → INT8 NOT NULL  
  Identificador único del tramo externo.

- `IdActa` → INT8 NOT NULL  
  Acta a la que pertenece la derivación externa.

---

### 4.5.2 Tipo de derivación externa

- `TipoDerivacionExterna` → SMALLINT NOT NULL  
  Tipo de derivación.

  Valores lógicos admitidos:

  - 1 = APREMIO
  - 2 = JUZGADO_PAZ

---

### 4.5.3 Estado de la derivación externa

- `EstadoDerivacionExterna` → SMALLINT NOT NULL  
  Estado operativo actual del tramo externo.

  Valores lógicos admitidos:

  - 1 = DERIVADO
  - 2 = EN_GESTION_EXTERNA
  - 3 = RESULTADO_RECIBIDO
  - 4 = CERRADO

---

### 4.5.4 Resultado de la derivación externa

- `ResultadoDerivacionExterna` → SMALLINT NOT NULL DEFAULT 0  
  Resultado externo recibido o snapshot del resultado actual.

  Valores lógicos admitidos:

  - 0 = SIN_RESULTADO_AUN
  - 1 = PAGO_EN_APREMIO
  - 2 = CONFIRMA_FALLO
  - 3 = MODIFICA_FALLO
  - 4 = ANULA_FALLO
  - 99 = OTRO_RESULTADO_EXTERNO

---

### 4.5.5 Fechas

- `FechaDerivacion` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora en que se registra la derivación externa.

- `FechaResultado` → DATETIME YEAR TO SECOND NULL  
  Fecha/hora en que se registra la recepción del resultado externo, si corresponde.

- `FechaCierre` → DATETIME YEAR TO SECOND NULL  
  Fecha/hora en que se registra el cierre del tramo externo, si corresponde.

---

### 4.5.6 Contenido operativo breve

- `ObservacionDerivacion` → VARCHAR(255) NULL  
  Observación breve y operativa de la derivación.

---

### 4.5.7 Trazabilidad operativa mínima

- `UsuarioRegistro` → VARCHAR(36) NOT NULL  
  Subject del usuario o identidad operativa que registró la derivación.

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
Toda fila de `ActaDerivacionExterna` pertenece a una única `Acta`.

### Regla 2
Toda fila debe tener un `TipoDerivacionExterna` válido.

### Regla 3
Toda fila debe tener un `EstadoDerivacionExterna` válido.

### Regla 4
Toda derivación externa debe tener `FechaDerivacion`.

### Regla 5
Si `EstadoDerivacionExterna = RESULTADO_RECIBIDO`, normalmente debería existir `FechaResultado`.

### Regla 6
Si `EstadoDerivacionExterna = CERRADO`, normalmente debería existir `FechaCierre`.

### Regla 7
`ResultadoDerivacionExterna` no reemplaza actos, documentos ni eventos; solo aporta clasificación operativa del resultado externo.

### Regla 8
La capa no modela el organismo externo como subdominio completo.

---

## 4.7 Observaciones de diseño

### Observación A
Se adopta una sola entidad (`ActaDerivacionExterna`) para mantener la capa simple y consistente con el resto del modelo.

### Observación B
Los tipos de derivación quedan acotados a destinos externos reales ya identificados:
- APREMIO
- JUZGADO_PAZ

### Observación C
El resultado externo queda tipificado porque aporta valor operativo real.

### Observación D
No se agregan referencias obligatorias a actos, documentos o notificaciones porque esas relaciones se resuelven desde sus propias capas.

---

# 5) VALIDACIONES LÓGICAS SUGERIDAS PARA ActaDerivacionExterna

## 5.1 Validación de cierre

Si `EstadoDerivacionExterna = CERRADO`, normalmente debería existir `FechaCierre`.

## 5.2 Validación de resultado

Si `EstadoDerivacionExterna = RESULTADO_RECIBIDO`, normalmente debería existir:

- `FechaResultado`
- y un `ResultadoDerivacionExterna` distinto de `SIN_RESULTADO_AUN`

## 5.3 Validación de secuencia

La lógica de negocio debería evitar secuencias inconsistentes, por ejemplo:

- `RESULTADO_RECIBIDO` sin derivación previa registrada
- `EN_GESTION_EXTERNA` sin `DERIVADO` previo, salvo que el proceso operativo defina otro camino

Estas validaciones no necesariamente deben quedar como constraints físicas en esta etapa.

---

# 6) INTEGRIDAD CRUZADA DE LA CAPA

## 6.1 Coherencia con Acta

- `ActaDerivacionExterna.IdActa` debe referir a una `Acta` existente.
- una `Acta` puede tener más de una derivación externa a lo largo del tiempo

---

## 6.2 Coherencia con Documento

Los documentos asociados al tramo externo viven en Capa 02.

Ejemplos:

- constancia de derivación
- oficio
- remisión
- resultado externo recibido
- constancia de cierre

La capa 08 no los duplica ni los modela como tabla propia.

---

## 6.3 Coherencia con ActaActo

La derivación externa puede dar lugar a actos previos o posteriores, pero no depende estructuralmente de ellos dentro de esta capa.

---

## 6.4 Coherencia con Notificación

La notificación de la derivación o de su resultado se resuelve en Capa 03.

La Capa 08 no modela destinatarios, canales, intentos ni resultados de notificación.

---

## 6.5 Coherencia con ActaEvento

La capa no obliga físicamente la creación de `ActaEvento`, pero funcionalmente debe integrarse con el historial general.

Eventos típicos que podrían reflejarse desde lógica de negocio:

- DERIVACION_EXTERNA_REGISTRADA
- APREMIO_INICIADO
- DERIVACION_JUZGADO_PAZ
- RESULTADO_EXTERNO_RECIBIDO
- PAGO_EN_APREMIO_REGISTRADO
- FALLO_CONFIRMADO_EXTERNAMENTE
- FALLO_MODIFICADO_EXTERNAMENTE
- FALLO_ANULADO_EXTERNAMENTE
- DERIVACION_EXTERNA_CERRADA

---

# 7) CATÁLOGOS / ENUMS LÓGICOS DE LA CAPA

## 7.1 TipoDerivacionExterna

- 1 = APREMIO
- 2 = JUZGADO_PAZ

---

## 7.2 EstadoDerivacionExterna

- 1 = DERIVADO
- 2 = EN_GESTION_EXTERNA
- 3 = RESULTADO_RECIBIDO
- 4 = CERRADO

---

## 7.3 ResultadoDerivacionExterna

- 0 = SIN_RESULTADO_AUN
- 1 = PAGO_EN_APREMIO
- 2 = CONFIRMA_FALLO
- 3 = MODIFICA_FALLO
- 4 = ANULA_FALLO
- 99 = OTRO_RESULTADO_EXTERNO

---

## 7.4 OrigenRegistro

- 1 = MANUAL
- 2 = SISTEMA
- 3 = PROCESO
- 4 = INTEGRACION

---

# 8) REGLAS DE NEGOCIO TRANSVERSALES SUGERIDAS

## Regla A
Toda derivación externa debe estar asociada a una `Acta`.

## Regla B
La derivación externa puede apoyarse en actos, documentos o notificaciones, pero no los reemplaza.

## Regla C
El resultado externo puede dar lugar a actos posteriores, pero no se modela aquí como acto.

## Regla D
La capa 08 no modela el organismo externo en detalle; solo el seguimiento estructurado de la derivación y del resultado recibido.

---

# 9) CONSULTAS OPERATIVAS QUE LA CAPA DEBE HABILITAR

La capa debe permitir consultas como:

- derivaciones externas de una causa
- última derivación externa de una causa
- causas en apremio
- causas derivadas al Juzgado de Paz
- derivaciones con resultado recibido
- pagos en apremio informados
- fallos confirmados externamente
- fallos modificados externamente
- fallos anulados externamente
- derivaciones cerradas

---

# 10) CIERRE DEL ANEXO

La Capa 08 queda lógicamente definida como una capa simple de seguimiento del tramo externo, basada en una única entidad:

- `ActaDerivacionExterna`

Su función es concreta:

- registrar la existencia de la derivación externa
- permitir lectura operativa de su estado
- clasificar el resultado externo recibido
- acompañar su cierre
- integrarse con actos, documentos, notificaciones y eventos sin duplicarlos

👉 Capa 08 no modela el sistema externo: **modela el seguimiento de la salida externa de la causa y del resultado que regresa**.
