# Convenciones generales del modelo lógico

## Finalidad

Este archivo define las convenciones comunes del bloque `spec/12-datos/` para asegurar consistencia entre las entidades, relaciones y criterios de persistencia del sistema.

Su objetivo es evitar ambigüedades al momento de bajar el dominio a tablas, referencias, anexos, satélites y proyecciones derivadas.

---

## Alcance

Estas convenciones aplican a todo el modelo lógico del sistema, salvo que un archivo específico indique explícitamente una excepción justificada.

No reemplazan las reglas del dominio ni las reglas transversales.  
Solo fijan criterios estables para la bajada lógica a datos.

---

## Unidad principal de gestión

La unidad principal de gestión del sistema es el Acta / Expediente.

Por lo tanto:

- las entidades principales del modelo deben referenciar directa o indirectamente al Acta cuando formen parte del expediente
- las piezas satélite no deben desplazar al Acta como eje del modelo
- los datos derivados o proyectados no deben redefinir la identidad central del expediente

---

## Convención de claves

### Clave primaria

La clave primaria de cada entidad se denomina:

- `Id`

### Claves foráneas

Las claves foráneas se denominan:

- `Id[EntidadReferenciada]`

Ejemplos:

- `IdActa`
- `IdDocumento`
- `IdNotificacion`
- `IdTipoDocumento`

Esta convención busca claridad, uniformidad y fácil lectura transversal del modelo.

---

## Identificadores de negocio

Además de la clave técnica `Id`, una entidad puede tener identificadores de negocio propios cuando el dominio lo requiera.

Ejemplos posibles:

- número de acta
- número de documento
- código externo
- identificador de integración

Estos identificadores no reemplazan la clave primaria técnica, aunque puedan tener restricciones de unicidad o uso operativo relevante.

---

## Separación entre núcleo, anexo, satélite y derivado

### Núcleo

Se considera núcleo a la entidad principal sin la cual una pieza del dominio no tiene sentido autónomo dentro del sistema.

Ejemplos:

- Acta
- ActaEvento
- Documento
- Notificacion

### Anexo

Se considera anexo a una entidad estrechamente dependiente de otra principal, utilizada para ampliar detalle sin recargar la tabla base.

Un anexo no redefine la identidad del objeto principal.

### Satélite

Se considera satélite a una entidad especializada vinculada al expediente o a una pieza principal para resolver una necesidad funcional concreta del dominio.

El satélite puede tener más autonomía que un anexo, pero sigue subordinado al modelo central del expediente.

### Derivado

Se considera derivado a toda proyección, vista materializable, snapshot o estructura reconstruible a partir del núcleo persistido y sus piezas asociadas.

Los derivados no constituyen fuente primaria de verdad.

---

## Regla sobre fuente de verdad

La fuente primaria de verdad del sistema está en:

- las entidades principales persistidas
- sus relaciones formales
- los eventos relevantes
- los documentos y notificaciones efectivamente registrados

Los snapshots, vistas operativas y demás estructuras derivadas no reemplazan a la fuente primaria.

---

## Regla sobre snapshot

El snapshot operativo es derivado y regenerable.

Por lo tanto:

- no debe modelarse como origen de verdad del expediente
- puede persistirse por razones operativas si luego se justifica
- su existencia no debe duplicar sin control la semántica del núcleo
- siempre debe poder explicarse a partir de las piezas primarias del expediente

---

## Regla sobre estados

Los estados persistidos deben utilizarse con criterio conservador.

En general:

- los estados principales del expediente, documento o notificación pueden persistirse cuando tengan valor operativo directo
- no debe persistirse como “estado” todo lo que pueda resolverse mejor como evento, relación o resultado específico
- no debe abusarse de estados redundantes que dupliquen semántica ya expresada por otras piezas del modelo

El modelo debe preferir claridad semántica antes que proliferación artificial de estados.

---

## Regla sobre eventos

Los eventos representan hechos relevantes del expediente y forman parte del núcleo de trazabilidad.

Por lo tanto:

- deben preservarse como registro formal del historial relevante
- no deben reemplazarse por simples marcas de estado actual
- no todo cambio técnico o interno debe transformarse en evento de negocio
- deben registrarse solo los hechos con valor real de trazabilidad, reconstrucción o auditoría funcional

---

## Regla sobre documentos

Los documentos son piezas formales del expediente y deben modelarse de forma explícita.

Por lo tanto:

- el archivo binario o soporte físico no agota la noción de documento
- la metadata documental relevante forma parte del modelo
- la firma, numeración, emisión, incorporación, observación o resultado documental deben poder reflejarse sin confundir documento con evento o con notificación

---

## Regla sobre notificaciones

La notificación es un proceso transversal único y debe modelarse separadamente del documento y del expediente, aunque vinculada a ambos.

Por lo tanto:

- no debe reducirse a una simple marca de “enviado”
- debe permitir reflejar objeto notificable, destinos, canal, resultado e incidencias
- sus resultados pueden impactar en el estado operativo del expediente, pero no deben absorberse de forma confusa dentro de otras entidades

---

## Catálogos y maestros

Los valores de referencia estables del sistema deben concentrarse en catálogos y maestros cuando tengan reutilización transversal o valor semántico propio.

No conviene crear catálogos innecesarios para valores efímeros, hiperlocales o puramente técnicos.

El detalle de qué se cataloga y qué no se desarrolla en el archivo específico de catálogos y maestros.

---

## Referencias externas e integraciones

Las referencias a sistemas externos, resultados de integración o identificadores ajenos al sistema pueden persistirse cuando sean necesarias para:

- trazabilidad
- conciliación
- auditoría
- reintentos
- correlación entre sistemas

Pero esas referencias no deben desplazar la identidad interna del modelo.

---

## Nulabilidad y obligatoriedad

La obligatoriedad de un dato debe responder a necesidad semántica real, no a comodidad técnica.

Por lo tanto:

- un campo obligatorio debe serlo porque la entidad no tiene sentido sin él
- un campo opcional debe permitirse cuando el dato pueda no existir legítimamente en ese momento del expediente
- no debe forzarse completitud artificial en etapas donde el dominio admite incorporación progresiva de información

---

## Fechas y trazabilidad temporal

Las entidades del modelo pueden requerir distintas fechas según su naturaleza, por ejemplo:

- creación
- ocurrencia
- emisión
- recepción
- firma
- resultado
- cierre

No debe asumirse una única fecha genérica para todo.

Cada modelo lógico deberá distinguir con claridad las fechas que tienen valor de negocio real para esa pieza.

---

## Borrado y conservación

Dado el carácter administrativo y trazable del sistema, el modelo debe favorecer conservación antes que borrado destructivo.

En principio:

- no debe asumirse borrado físico como operación normal del dominio
- las anulaciones, cierres, archivos, observaciones o invalidaciones deben resolverse semánticamente según corresponda
- cualquier excepción a este criterio deberá justificarse explícitamente

---

## Compactación del modelo

El modelo lógico debe mantenerse compacto.

Por lo tanto:

- no deben crearse entidades separadas si una extensión razonable puede resolverse como anexo o satélite simple
- no deben duplicarse estructuras solo por comodidad de lectura
- no debe sobre-normalizarse si eso complica innecesariamente el entendimiento y la operación
- tampoco debe sub-modelarse de manera que se pierda semántica importante

El criterio rector es equilibrio entre claridad, trazabilidad y simplicidad.

---

## Paso siguiente

A partir de estas convenciones, los archivos siguientes del bloque desarrollan la bajada lógica de las piezas principales del sistema:

- Acta
- ActaEvento
- Documento
- Notificacion
- Snapshot
- satélites específicos
- catálogos y maestros