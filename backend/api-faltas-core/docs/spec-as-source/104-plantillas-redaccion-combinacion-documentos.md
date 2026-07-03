# 104 - Plantillas operativas, variables documentales y motor de combinacion de documentos

**Slice: 8F-1**
**Estado: CERRADO**
**Build base: 908 tests OK -> post-slice: 954 tests OK**

---

## 1. Problema

El sistema tenia entidades de plantilla funcional (`FalDocumentoPlantilla`) y documento (`FalDocumento`),
pero faltaba la capa de:

- Plantilla operativa por defecto (que plantilla usar automaticamente).
- Contenido versionado de plantilla (el texto/template real).
- Variables documentales (reemplazos dinamicos en el texto).
- Motor de combinacion (reemplaza variables por valores).
- Redaccion documental editable (el documento en borrador antes de PDF).

---

## 2. Flujo de redaccion

```
1. Resolver plantilla default operativa (DocumentoPlantillaDefaultService)
2. Resolver contenido vigente de la plantilla (FalDocumentoPlantillaContenido)
3. Construir contexto de variables (responsabilidad del llamador en 8F-1; 8F-2 agrega builder)
4. Combinar template con variables (DocumentoCombinacionService)
5. Crear FalDocumentoRedaccion en estado BORRADOR
6. Guardar redaccion
7. Devolver DocumentoRedaccionResponse con diagnostico
```

---

## 3. Plantilla funcional vs plantilla contenido

| Entidad                      | Responsabilidad                                      |
|------------------------------|------------------------------------------------------|
| `FalDocumentoPlantilla`      | Contrato funcional: tipo, accion, firma, numeracion  |
| `FalDocumentoPlantillaContenido` | Texto/template versionado con variables          |
| `FalDocumentoPlantillaDefault`   | Resolucion automatica: que plantilla usar        |

Una `FalDocumentoPlantilla` puede tener multiples versiones de `FalDocumentoPlantillaContenido`.

---

## 4. Plantilla default operativa

Entidad: `FalDocumentoPlantillaDefault`

Campos clave: accionDocumental, tipoActa (nullable), idDependencia (nullable), plantillaId, prioridad, vigencia.

### Regla de resolucion

1. Buscar defaults activos y vigentes para la accionDocumental.
2. Filtrar: `d.tipoActa == null || d.tipoActa == tipoActa`.
3. Filtrar: `d.idDependencia == null || d.idDependencia == idDependencia`.
4. Ordenar por prioridad DESC (mayor = mas especifico).
5. Tomar el de mayor prioridad.
6. Empate de prioridad -> `PlantillaDefaultAmbiguaException`.
7. Ninguno -> `PlantillaDefaultNoEncontradaException`.

---

## 5. Variables documentales

ConvenciÃƒÂ³n de nombres:
```
{{namespace.nombreCampo}}
```

- namespace en lowerCamelCase (acta, infractor, domicilioInfraccion, ...).
- campo en lowerCamelCase (nroActa, nombreCompleto, ...).
- Solo puntos como separador de segmentos.
- No se permiten: espacios obligatorios, expresiones, metodos, scripts, SpEL, eval.
- Regex de validacion: `[a-z][a-zA-Z0-9]*(?:\.[a-z][a-zA-Z0-9]*)+`

### Variables minimas registradas (8F-1)

| Variable                      | Namespace       | Tipo       | Requerida |
|-------------------------------|-----------------|------------|-----------|
| acta.nroActa                  | ACTA            | TEXTO      | No        |
| acta.fechaLabrado             | ACTA            | FECHA_HORA | **Si**    |
| infractor.nombreCompleto      | INFRACTOR       | TEXTO      | **Si**    |
| infractor.documento           | INFRACTOR       | TEXTO      | **Si**    |
| domicilioInfractor.texto      | DOMICILIO_INFRACTOR | TEXTO  | No        |
| domicilioInfraccion.texto     | DOMICILIO_INFRACCION | TEXTO | No       |
| ubicacion.lat                 | UBICACION       | NUMERO     | No        |
| ubicacion.lon                 | UBICACION       | NUMERO     | No        |
| nomenclatura.manzana          | NOMENCLATURA    | TEXTO      | No        |
| nomenclatura.parcela          | NOMENCLATURA    | TEXTO      | No        |
| licencia.municipioEmisor      | LICENCIA        | TEXTO      | No        |
| infraccion.descripcion        | INFRACCION      | TEXTO      | No        |
| documento.nroDocu             | DOCUMENTO       | TEXTO      | No        |
| sistema.municipioNombre       | SISTEMA         | TEXTO      | No        |
| sistema.fechaActual           | SISTEMA         | FECHA_HORA | No        |

---

## 6. Motor de combinacion

Servicio: `DocumentoCombinacionService`

Metodo: `combinar(String template, Map<String, Object> contexto)`

### Politica de faltantes

- Variable desconocida (no en registry): registrada en `variablesDesconocidas`, NO reemplazada.
- Variable requerida faltante: registrada en `variablesFaltantes`, NO reemplazada.
- Variable opcional faltante: reemplazada por cadena vacia, diagnostico informativo.
- `completo = variablesFaltantes.isEmpty() && variablesDesconocidas.isEmpty()`

### Formato de valores

- `LocalDate` -> `dd/MM/yyyy`
- `LocalDateTime` -> `dd/MM/yyyy HH:mm`
- Cualquier otro tipo -> `toString()`
- null requerido -> faltante
- null opcional -> cadena vacia

### Guardrails de seguridad

- No usa SpEL, ScriptEngine, Groovy, MVEL ni JavaScript.
- No permite expresiones arbitrarias ni llamadas a metodos.
- Solo reemplaza variables con el patron exacto `{{namespace.campo}}`.
- Sintaxis invalida (sin punto, con parentesis, etc.) es ignorada.

---

## 7. Redaccion editable

Entidad: `FalDocumentoRedaccion`

Estados: `BORRADOR | CONFIRMADA | REABIERTA | ANULADA`

### Regla de PDF/storage

Mientras `estadoRedaccion = BORRADOR`:

- `FalDocumento.storageKey = null`
- `FalDocumento.hashDocu = null`
- `FalDocumento.fhGeneracion = null`

Al confirmar/enviar (slice futuro):

- Se genera PDF final.
- Se calcula hash.
- Se guarda en storage.
- Se setean `storageKey/hashDocu/fhGeneracion`.
- **No se guardan PDFs intermedios.**

---

## 8. Servicio de redaccion

Servicio: `DocumentoRedaccionService`

Metodo principal: `crearRedaccionDesdePlantilla(CrearRedaccionDocumentoCommand cmd)`

Command: `idDocumento, idActa, accionDocumental, tipoActa, idDependencia, verDependencia, idUserOperacion, variablesContexto`

Respuesta: `DocumentoRedaccionResponse` con contenidoEditable, variablesUsadas, diagnostico.

### Nota de evolucion

En 8F-1 el llamador es responsable de proveer `variablesContexto`.
En 8F-2 se agrega `DocumentoVariableContextBuilder` para construir el contexto desde `FalActa`/snapshots.

---

## 9. Nuevos enums Java

| Enum                         | Valores                              |
|------------------------------|--------------------------------------|
| `FormatoPlantillaContenido`  | TEXTO_PLANO, HTML_SIMPLE, MARKDOWN_SIMPLE |
| `EstadoRedaccionDocumento`   | BORRADOR, CONFIRMADA, REABIERTA, ANULADA |
| `DocumentoVariableNamespace` | ACTA, INFRACTOR, DOMICILIO_INFRACTOR, ... (18 valores) |
| `TipoDatoVariableDocumento`  | TEXTO, NUMERO, FECHA, FECHA_HORA, MONEDA, BOOLEANO |

No se crean tablas fisicas para estos enums. No se crean seeds.

---

## 10. Fuera de alcance

- PDF real, storage real, firma digital real.
- Frontend Angular, editor visual, UX de edicion.
- Repositorios JDBC.
- Scripts MariaDB / DDL.
- Tablas/seeds para enums cerrados.
- OCR/IA.
- Motor de templates complejo (Word/docx).
- Notificacion automatica.

---

## 11. Proximos slices

| Slice | Descripcion                                                               |
|-------|---------------------------------------------------------------------------|
| 8F-2  | Context builder desde acta/snapshots + plantillas mock por caso de uso    |
| 8F-3  | Confirmacion de redaccion + generacion de PDF final (stub/mock)           |
| 8F-4  | Plantillas default para todos los casos de uso del graph demo             |
| 9-2   | Piloto JdbcDocumentoPlantillaRepository (despues de dominio completo)     |

---

## 12. Archivos creados

### Enums
- `domain/enums/FormatoPlantillaContenido.java`
- `domain/enums/EstadoRedaccionDocumento.java`
- `domain/enums/DocumentoVariableNamespace.java`
- `domain/enums/TipoDatoVariableDocumento.java`

### Entidades de dominio
- `domain/model/FalDocumentoPlantillaDefault.java`
- `domain/model/FalDocumentoPlantillaContenido.java`
- `domain/model/FalDocumentoRedaccion.java`
- `domain/model/DocumentoVariableDefinicion.java` (record)

### Excepciones
- `domain/exception/PlantillaDefaultNoEncontradaException.java`
- `domain/exception/PlantillaDefaultAmbiguaException.java`
- `domain/exception/PlantillaContenidoNoEncontradaException.java`
- `domain/exception/DocumentoRedaccionNoEncontradaException.java`

### Repositorios (interfaces + in-memory)
- `repository/DocumentoPlantillaDefaultRepository.java`
- `repository/DocumentoPlantillaContenidoRepository.java`
- `repository/DocumentoRedaccionRepository.java`
- `repository/memory/InMemoryDocumentoPlantillaDefaultRepository.java`
- `repository/memory/InMemoryDocumentoPlantillaContenidoRepository.java`
- `repository/memory/InMemoryDocumentoRedaccionRepository.java`

### Combinacion
- `application/combinacion/DocumentoVariableRegistry.java`
- `application/combinacion/DocumentoCombinacionService.java`
- `application/combinacion/DocumentoCombinacionResultado.java`
- `application/combinacion/DocumentoVariableContextBuilder.java`

### Command / Response
- `application/command/CrearRedaccionDocumentoCommand.java`
- `application/result/DocumentoRedaccionResponse.java`

### Servicios
- `application/service/DocumentoPlantillaDefaultService.java`
- `application/service/DocumentoRedaccionService.java`

### Tests
- `test/.../DocumentoCombinacionServiceTest.java` (17 casos)
- `test/.../DocumentoPlantillaDefaultTest.java` (10 casos)
- `test/.../DocumentoRedaccionServiceTest.java` (19 casos)

---

## Slice 8F-2 Ã¢â‚¬â€ Context builder desde acta/snapshots + plantillas mock

**Estado: CERRADO**
**Build: 1040/1040 tests OK**
**Fecha: 2026-07-02**

### Objetivo

Implementar el armado real/deterministico del contexto documental desde acta/snapshots/mocks del
graph demo y crear plantillas default mock para los 8 casos operativos principales.

### Origen de variables

Las variables se construyen desde objetos del dominio in-memory mediante `DocumentoVariableContextBuilder`:

| Namespace      | Fuente                                      | Observaciones                          |
|----------------|---------------------------------------------|----------------------------------------|
| `acta.*`       | `FalActa.getNroActa()`, `getFechaLabrado()` | Opcionales si son null                 |
| `infractor.*`  | `FalActa.getInfractorNombre/Documento()`    | Requeridas                             |
| `domicilioInfractor.*` | `FalActa.getDomicilioInfractor()`   | Opcional                               |
| `domicilioInfraccion.*`| `FalActa.getDomicilioHecho()`       | Opcional                               |
| `ubicacion.*`  | `FalActa.getLatInfr/LonInfr()`              | Opcionales si son null                 |
| `infraccion.*` | `FalActa.getObservaciones()`                | Opcional                               |
| `documento.*`  | `FalDocumento.getNroDocu()`                 | Opcional si documento es null o sin nro|
| `fallo.*`      | `FalActaFallo` (opcional)                   | Absent si fallo es null                |
| `pago.*`       | `FalPagoVoluntario` (opcional)              | Absent si pago es null                 |
| `notificacion.*`| `FalNotificacion` (opcional)               | Absent si notificacion es null         |
| `licencia.*`   | Mock deterministico                         | `licencia.municipioEmisor` fijo        |
| `nomenclatura.*`| Mock deterministico                        | `manzana=12, parcela=4B`               |
| `sistema.*`    | Constantes + `LocalDateTime.now()`          | Siempre presentes                      |

### Namespaces cubiertos (total 13)

acta, infractor, domicilioInfractor, domicilioInfraccion, ubicacion, infraccion,
documento, fallo, pago, notificacion, licencia, nomenclatura, sistema.

### Variables en el registry (total 26)

**Requeridas (3):** `acta.fechaLabrado`, `infractor.nombreCompleto`, `infractor.documento`

**Opcionales (23):** todas las demas incluyendo fallo.*, pago.*, notificacion.*, nomenclatura.*, etc.

### Plantillas mock creadas (8 casos)

| ID    | Codigo               | AccionDocumental               | TipoDocu                        |
|-------|----------------------|--------------------------------|---------------------------------|
| 1001  | TMPL-FALLO-001       | EMITIR_FALLO                   | ACTO_ADMINISTRATIVO             |
| 1002  | TMPL-NOTIF-ACTA-001  | EMITIR_NOTIFICACION_ACTA       | NOTIFICACION_ACTA               |
| 1003  | TMPL-NOTIF-FALLO-001 | EMITIR_NOTIFICACION_FALLO      | NOTIFICACION_ACTO_ADMINISTRATIVO|
| 1004  | TMPL-INTIM-PAGO-001  | EMITIR_INTIMACION_PAGO         | INTIMACION_PAGO                 |
| 1005  | TMPL-MED-PREV-001    | EMITIR_MEDIDA_PREVENTIVA       | MEDIDA_PREVENTIVA               |
| 1006  | TMPL-CONST-001       | EMITIR_CONSTANCIA              | CONSTANCIA                      |
| 1007  | TMPL-ANEXO-001       | EMITIR_ANEXO                   | ANEXO                           |
| 1008  | TMPL-RES-BLOQ-001    | EMITIR_RESOLUTORIO_BLOQUEANTE  | RESOLUTORIO_BLOQUEANTE          |

Cada plantilla tiene:
- `FalDocumentoPlantillaDefault` con prioridad 10, vigente desde 2024-01-01, sin fecha de vencimiento
- `FalDocumentoPlantillaContenido` version 1, formato TEXTO_PLANO, vigente
- Template con variables reales del registry, usando solo variables registradas
- Todas las plantillas producen `completo=true` con el contexto demo completo

### Clases nuevas / modificadas

**Nuevas:**
- `application/combinacion/DocumentoVariableContextBuilder.java` (expandido)
- `application/combinacion/DocumentoVariableRegistry.java` (expandido, 26 variables)
- `application/demo/GraphDemoActaFactory.java`
- `application/demo/PlantillasMockSeeder.java`
- `application/service/DocumentoRedaccionService.java` (expandido)

**Tests nuevos (86 casos adicionales):**
- `test/.../DocumentoVariableContextBuilderTest.java` (25 casos)
- `test/.../DocumentoPlantillasMockTest.java` (45+ casos parametrizados)
- `test/.../DocumentoRedaccionGraphDemoTest.java` (16 casos)

### Limitaciones documentadas

- `FalNotificacion` no se carga automaticamente en `crearRedaccionConContextoActa` (se puede pasar null).
  Razon: no existe un repositorio de "ultima notificacion por acta" determinado aun.
  Pendiente: slice 8F-3 puede agregar este mecanismo si se necesita.
- `FalActaSnapshot` no se usa para el contexto documental.
  Razon: el snapshot contiene flags operativos de bandeja, no datos de infractor/domicilio.
  Los datos de infractor/domicilio estan en `FalActa` directamente.
- `nomenclatura.*` y `licencia.*` son valores mock deterministicos.
  Pendiente: en produccion estos valores vendran de catalogos reales.

### Garantias

- No genera PDF.
- No setea storageKey, hashDocu ni fhGeneracion durante BORRADOR.
- No emite documentos.
- No envia a firma.
- No notifica.
- No usa JPA/Hibernate.
- No usa JDBC/MariaDB.
- No usa reflection ni SpEL.

### Siguiente slice recomendado

**8F-3 Ã¢â‚¬â€ Mock PDF renderer / generacion final simulada de documentos.**

---

## 8F-3 Ã¢â‚¬â€ Mock PDF renderer y generacion final simulada de documentos

### Proposito del renderer mock

DocumentoPdfMockRenderer es un servicio que materializa el contenido de una redaccion confirmada
en una representacion textual simulada, sin usar librerias PDF reales (no iText, no PDFBox, no OpenPDF).

El objetivo es cerrar el ciclo documental en el prototipo:

`
Redaccion BORRADOR Ã¢â€ â€™ confirmar Ã¢â€ â€™ generacion mock Ã¢â€ â€™ metadatos en FalDocumento
`

### Diferencia entre PDF mock y PDF real

| Aspecto                | PDF Mock (8F-3)                              | PDF Real (futuro)             |
|------------------------|----------------------------------------------|-------------------------------|
| Contenido              | Texto plano con marcas de diagnostico        | Binario PDF estructurado      |
| MIME type              | application/x-faltas-pdf-mock               | application/pdf               |
| Storage                | mock:// (sin storage real)                   | S3, filesystem, etc.          |
| Hash                   | SHA-256 sobre contenido textual              | SHA-256 sobre bytes PDF       |
| Librerias externas     | Ninguna                                      | iText, PDFBox, etc.           |
| Archivos fisicos       | No                                           | Si                            |
| Uso                    | Validacion funcional / prototipo             | Produccion                    |

### Por que no hay storage real todavia

El prototipo es un modulo descartable in-memory. El objetivo es validar:
- el modelo de estados y transiciones
- el circuito redaccion Ã¢â€ â€™ confirmacion Ã¢â€ â€™ generacion
- los metadatos que el sistema necesita setear

No tiene sentido conectar storage real hasta que el modelo sea estable.

### Cuando se setean storageKey / hashDocu / fhGeneracion

Estos tres campos en FalDocumento permanecen null **mientras la redaccion esta en BORRADOR**.

Se setean **solo al confirmar y generar mock**, en DocumentoGeneracionMockService.confirmarYGenerarMockPdf():

- storageKey Ã¢â€ Â mock://documentos/{id}/redacciones/{id}/documento-final.pdf
- hashDocu Ã¢â€ Â sha256-mock-{hex-sha256-del-contenido-mock}
- hGeneracion Ã¢â€ Â timestamp del momento de renderizado

### Regla de BORRADOR sin PDF

`
Mientras redaccion.estadoRedaccion == BORRADOR:
  FalDocumento.storageKey == null
  FalDocumento.hashDocu == null
  FalDocumento.fhGeneracion == null
`

Ninguna operacion anterior a la confirmacion puede violar esta regla.

### Regla de CONFIRMADA con generacion mock

`
Al confirmar y generar:
  redaccion.estadoRedaccion Ã¢â€ â€™ CONFIRMADA
  redaccion.fhConfirmacion Ã¢â€ â€™ seteado
  redaccion.idUserConfirmacion Ã¢â€ â€™ seteado
  FalDocumento.storageKey Ã¢â€ â€™ mock://...
  FalDocumento.hashDocu Ã¢â€ â€™ sha256-mock-...
  FalDocumento.fhGeneracion Ã¢â€ â€™ seteado
`

### Separacion entre generacion, emision, firma y notificacion

- **Generacion mock (8F-3)**: confirmar redaccion + setear metadatos simulados. No emite.
- **Emision formal (8C)**: transicion BORRADOR/FIRMADO Ã¢â€ â€™ EMITIDO con storageKey/hashDocu reales.
- **Firma (8C)**: proceso de firma digital o escaneada. Requiere PENDIENTE_FIRMA o ADJUNTO.
- **Notificacion**: flujo separado post-emision.

La generacion mock NO invoca emision, NO envia a firma, NO notifica.

### Clases implementadas en 8F-3

| Clase | Paquete | Rol |
|-------|---------|-----|
| DocumentoRenderizadoMock | pplication/result/ | Record con resultado del render |
| DocumentoGeneracionMockResponse | pplication/result/ | Response del servicio |
| ConfirmarRedaccionYGenerarDocumentoMockCommand | pplication/command/ | Command de entrada |
| DocumentoPdfMockRenderer | pplication/service/ | Renderer puro (no modifica FalDocumento) |
| DocumentoGeneracionMockService | pplication/service/ | Orquestador del flujo |
| FalDocumentoRedaccion.confirmar() | domain/model/ | Metodo de dominio |

### Limitaciones

- El contenido mock es texto plano, no PDF estructurado.
- El storageKey es ficticio (esquema mock://).
- El hash es de contenido textual, no de bytes PDF.
- No hay paginacion, estilos, firmas digitales ni metadatos PDF.

### Que queda para storage/PDF real futuro

- Integrar libreria PDF (iText o similar) en modulo productivo.
- Conectar storage real (S3 o equivalente).
- Migrar confirmar() del dominio a un flujo de emision formal real.
- Ajustar FalDocumento.marcarEmitido() como punto de entrada para storage real.
- El hash real sera SHA-256 sobre los bytes del PDF generado.
---

## 8F-4B - Dataset funcional completo del dominio en memoria

### Por que se creo este slice

El grafo demo documental (8F-4) cubria solo una acta demo base generica.
Para poder implementar pruebas funcionales completas por caso de uso (8F-4C)
se necesitaba primero el dataset funcional: un universo canonico de actas
mock que declarase todos los casos de uso del sistema, sus estados iniciales,
documentos esperados y cobertura funcional.

### Diferencia entre graph demo y dataset funcional

| | Graph demo (8F-4) | Dataset funcional (8F-4B) |
|--|-------------------|---------------------------|
| Proposito | Flujo documental 8 tipos | Universo de casos de uso |
| Actas | 1 acta demo base | 25 actas mock funcionales |
| Documentos | 8 casos ejecutados | Documentos esperados declarados |
| Tests | 16 tests de graph | 50 tests del dataset |
| Mutacion | Crea redacciones/documentos | Catalogo read-only + endpoint |

### Clases implementadas en 8F-4B

| Clase | Paquete | Rol |
|-------|---------|-----|
| ActaMockFuncionalDefinicion | application/demo/ | Record declarativo con 22 campos |
| DocumentoEsperadoPorActaMock | application/demo/ | Record de documento esperado |
| DatasetFuncionalDominioCatalog | application/demo/ | 25 actas mock + construirActa() |
| DatasetFuncionalCoberturaResultado | application/result/ | Cobertura: totales, listas, flags |
| DatasetFuncionalDemoController | web/ | GET /demo/actas/dataset-funcional |

### Estructura de ActaMockFuncionalDefinicion

Campos clave:
- codigo: identificador estable (ACT-NNN-DESCRIPCION)
- casoUsoPrincipal, casosUsoCubiertos
- bloqueEsperado (BloqueActual), situacionEsperada, resultadoFinalEsperado, bandejaEsperada
- Flags booleanos: requiereFallo, requierePago, requiereNotificacion, requiereFirmaDocumental,
  requiereMedidaPreventiva, requiereResolutorioBloqueante, etc.
- documentosEsperados: List<DocumentoEsperadoPorActaMock>
- eventosEsperados: List<TipoEventoActa>
- endpointsServiciosCubiertos, observaciones

### Estructura de DocumentoEsperadoPorActaMock

Campos:
- accionDocumental (AccionDocumental), tipoDocu (TipoDocu)
- obligatorio, requiereRedaccion, requiereGeneracionMock, requiereFirma, requiereEmision, requiereNotificacion
- momentoFuncional, observacion

### Matriz acta mock -> documentos esperados

Cada acta mock declara sus documentos esperados. Reglas de consistencia:
- Actas con requiereFallo=true deben declarar EMITIR_FALLO
- Actas con requiereNotificacion=true deben declarar EMITIR_NOTIFICACION_ACTA o EMITIR_NOTIFICACION_FALLO
- Actas con requiereFallo=true Y requierePago=true deben declarar EMITIR_INTIMACION_PAGO
- Actas con requiereMedidaPreventiva=true deben declarar EMITIR_MEDIDA_PREVENTIVA
- Actas con requiereResolutorioBloqueante=true deben declarar EMITIR_RESOLUTORIO_BLOQUEANTE

Estas reglas son validadas por DatasetFuncionalDocumentosEsperadosTest.

### Endpoint demo read-only

GET /demo/actas/dataset-funcional

Devuelve DatasetFuncionalCoberturaResultado con:
- totalActasMock: 25
- totalCasosUsoCubiertos: numero de casos unicos cubiertos
- totalDocumentosEsperados: suma de documentos declarados
- actas: lista completa de ActaMockFuncionalDefinicion
- casosUsoCubiertos: lista de strings
- casosUsoPendientes: lista de pendientes documentados
- coberturaCompletaSegunDominioActual: flag booleano
- advertencias: lista de advertencias

### Guardrails cumplidos

- No JDBC, no JPA/Hibernate, no tablas, no seeds.
- No PDF/storage real, no librerÃ­as PDF, no filesystem documental.
- No reflection, SpEL, eval, ScriptEngine.
- No Angular, no frontend.
- No endpoint reset todavia.
- docs/spec-as-source en raiz: False (confirmado).
- No actas genericas sin proposito: cada acta declara caso de uso.

### Relacion con 8F-4C

8F-4B construye el dataset y la matriz declarativa.
8F-4C usara el dataset para pruebas funcionales completas de punta a punta:
  caso de uso -> crear acta desde cero -> ejecutar flujo real
  -> documentos -> eventos -> estado final -> asserts.
### 8F-4B-R1 - Cierre de pendientes del dataset funcional

Por que no se aceptan pendientes funcionales en el dataset:
El dataset funcional debe ser completo. Cada caso de uso del sistema
debe tener una acta mock que lo represente. No deben quedar casos
conocidos fuera del dataset, porque el dataset es la fuente canonica
del universo funcional del sistema. Los gaps tecnicos de ejecucion
(donde no hay implementacion todavia) se documentan como observaciones
en la acta, no como "pendientes del dataset".

Diferencia entre caso cubierto por dataset y flujo ejecutable en 8F-4C:
- Dataset (8F-4B): declara que el caso existe, cual es su estado esperado
  y que documentos deberia generar.
- 8F-4C: intenta ejecutar el flujo de punta a punta. Si el flujo no es
  ejecutable todavia (gap tecnico), lo reporta pero no elimina la acta del dataset.

Actas nuevas agregadas en 8F-4B-R1:

| Codigo | Caso de uso | Bloque | ResultadoFinal |
|--------|-------------|--------|----------------|
| ACT-026-NOTIFICACION-NEGATIVA | RegistrarNotificacionNegativa (NOTNEG) | ANAL | SIN_RESULTADO_FINAL |
| ACT-027-DOC-ADJUNTO-CONVALIDADO | IncorporarDocumentoEscaneado + ConvalidarFirmaEscaneada | ENRI | SIN_RESULTADO_FINAL |
| ACT-028-ABSOLUCION-FIRME-CERRADA | DictarFalloAbsolutorio -> ABSUELTO -> CERRADA | CERR | ABSUELTO |
| ACT-029-REINGRESO-PARA-REVISION | EXTRET + REINGRESO_PARA_REVISION (Slice 6D-1) | ANAL | CONDENA_FIRME |
| ACT-030-PAGO-CONDENA-OBSERVADO | ObservarPagoCondena (PCOOBS) | ANAL | CONDENA_FIRME |
| ACT-031-PAGO-CONDENA-CON-DESCUENTO | ConfirmarPagoCondena variante descuento (GAP documentado) | CERR | CONDENA_FIRME_PAGADA |

Cobertura de notificacion negativa:
- NOTNEG: evento productivo TipoEventoActa.NOTNEG
- Acta vuelve a analisis post-notificacion-negativa
- Documento EMITIR_NOTIFICACION_ACTA declarado

Cobertura de adjunto convalidado:
- DOCADJ: evento productivo TipoEventoActa.DOCADJ
- IncorporarDocumentoEscaneadoCommand + ConvalidarFirmaEscaneadaCommand
- Firma olografa del infractor separada de firma documental institucional
- Eliminado de pendientesDocumentados() en el catalogo

Cobertura de absolucion firme cerrada:
- ResultadoFinalActa.ABSUELTO: enum existente
- Cierre definitivo post-fallo-absolutorio + notificacion positiva
- BloqueActual.CERR, SituacionAdministrativaActa.CERRADA

Cobertura de reingreso para revision:
- TipoEventoActa.EXTRET: evento existente (Slice 6)
- ModoReingresoGestionExterna.REINGRESO_PARA_REVISION: slice 6D-1
- Acta vuelve a ANAL ACTIVA con CONDENA_FIRME

Cobertura de pago observado:
- TipoEventoActa.PCOOBS: evento existente (Slice 5)
- Pago rechazado, CONDENA_FIRME pendiente de nuevo PCOINF

Cobertura de pago con descuento:
- GAP: el dominio actual usa PCOCNF sin parametro de descuento separado
- El descuento es atributo del pago, no un evento distinto en el dominio
- ACT-031 declara la expectativa funcional con gap documentado
- 8F-4C validara si el flujo es ejecutable tal cual o requiere ajuste

Estado final del dataset en 8F-4B-R1:
- 31 actas mock funcionales
- 0 casos pendientes de los 5 originales (todos cubiertos)
- Tests run: 1173, Failures: 0, BUILD SUCCESS

---

## 8F-4C � Pruebas funcionales completas por caso de uso

### Diferencia entre dataset funcional y prueba funcional

- **Dataset funcional (8F-4B/8F-4B-R1)**: Declara las 31 actas, sus documentos esperados y la matriz de cobertura. No ejecuta servicios reales.
- **Prueba funcional (8F-4C)**: Crea cada acta desde cero, invoca servicios reales, recorre el ciclo de vida, valida eventos/estados/documentos. No acepta parciales por falta de servicio.

### Regla central

- Cada test funcional crea el acta desde cero (sin reutilizar estado global).
- No se fabrican estados finales con setters directos.
- Si falta un servicio de dominio in-memory necesario, se implementa en el slice.
- Los �nicos gaps permitidos son de infraestructura futura expl�cita (JDBC/MariaDB real).

### Servicios nuevos creados en 8F-4C

| Servicio | Motivo |
|---|---|
| ActaParalizacionService | Gap detectado: no hab�a servicio para ACTPAR/ACTREA |
| ParalizarActaCommand | Comando nuevo |
| ReactivarActaCommand | Comando nuevo |
| CasoUsoFuncionalRunner | Runner funcional del dataset completo |
| CasoUsoFuncionalEjecucionResultado | Modelo de resultado de ejecuci�n |
| PasoFuncionalResultado | Modelo de paso individual |

### Suites funcionales creadas (11 + 1 cobertura)

| Suite | Actas cubiertas |
|---|---|
| ActaFlujoCapturaFuncionalTest | ACT-001, ACT-002, ACT-004 |
| ActaFlujoDocumentalFuncionalTest | ACT-003, ACT-023, ACT-024, ACT-025, ACT-027 |
| ActaFlujoNotificacionFuncionalTest | ACT-005, ACT-006, ACT-013, ACT-026 |
| ActaFlujoPagoVoluntarioFuncionalTest | ACT-007, ACT-008, ACT-009 |
| ActaFlujoFalloFuncionalTest | ACT-010, ACT-011, ACT-012, ACT-015, ACT-028 |
| ActaFlujoApelacionFuncionalTest | ACT-014 |
| ActaFlujoPagoCondenaFuncionalTest | ACT-016, ACT-017, ACT-030, ACT-031 |
| ActaFlujoGestionExternaFuncionalTest | ACT-018, ACT-019 |
| ActaFlujoParalizacionFuncionalTest | ACT-020 |
| ActaFlujoBloqueanteFuncionalTest | ACT-021, ACT-022 |
| ActaFlujoReingresoFuncionalTest | ACT-029 |
| DatasetFuncionalFlujosCoberturaTest | Cobertura total 31/31 |

### Decisiones de dominio

**Descuento en ACT-031**: El descuento no tiene evento propio (DESCT no existe en TipoEventoActa). El pago con descuento queda representado como atributo/observaci�n del PCOCNF. Raz�n: el descuento es una variante del pago confirmado, no una transici�n de estado distinta.

**FirmaReq completa**: El ciclo de firma v�a DocumentoFirmaReqService funciona correctamente en el prototipo in-memory. Los tests funcionales usan irmarDocumento() directamente que genera el evento DOCFIR.

**Emisi�n formal**: No requerida para cerrar los casos del dataset actual. Pendiente en 8F-5/8F-6 cuando se precise el endpoint formal de numeraci�n.

**Acta archivada / REINGRESO_PARA_CIERRE**: No pertenecen al ciclo funcional del dataset actual. Quedan documentados como pendientes para slice 8F-5+.

**ACT-022 (Absuelto con bloqueante)**: El fallo absolutorio con bloqueante material activo no genera CIERRA. La NotificacionService usa RepositoryBloqueantesMaterialesChecker que bloquea el cierre si hay bloqueantes activos. El cierre se produce cuando BloqueanteMaterialService resuelve el �ltimo bloqueante.

**ACT-025 (PrecondicionVioladaException)**: Los guardrails de precondici�n se testean verificando que PrecondicionVioladaException se lanza al invocar comandos fuera de orden.

### Eventos validados

ACTLAB, ACTCAP, ACTENR, DOCGEN, DOCFIR, NOTENV, NOTPOS, NOTNEG, DOCADJ, PAGVSO, PAGVMF, PAGINF, PAGCNF, FALCON, FALABS, APEPRE, PLAVNC, CONFIR, PCOINF, PCOCNF, PCOOBS, EXTDER, EXTRET, PAGAPR, ACTPAR, ACTREA, CIERRA

### Estados finales validados

Bloque: CAPT, ENRI, ANAL, CERR, GEXT
Bandeja: todas las CodigoBandeja operativas
SituacionAdministrativa: ACTIVA, PARALIZADA, CERRADA, EN_GESTION_EXTERNA
ResultadoFinal: ABSUELTO, CONDENA_FIRME, CONDENA_FIRME_PAGADA, PAGO_VOLUNTARIO_CONFIRMADO, NOTIFICACION_NEGATIVA_SIN_COMPARENCIA

### Relaci�n con slices siguientes

- **8F-5**: Endpoint dev/test de reset y recreaci�n in-memory.
- **8F-6**: Frontend-ready: bandejas y acciones desde Angular.
