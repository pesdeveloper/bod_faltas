# Frontera prototipo - dominio real

**Fecha:** 2026-06-12
**Scope:** `backend/api-faltas-prototipo`
**Objetivo:** guia tecnica para la futura integracion con dominio real, separando
que reglas deben conservarse, que infraestructura debe descartarse, que contratos
no deben romperse y que riesgos evitar al reemplazar el store en memoria por
persistencia y servicios reales.

---

## 1. Resumen ejecutivo

### Que es el prototipo

`api-faltas-prototipo` es un simulador navegable en memoria, descartable, creado
para validar el modelo operativo de faltas antes de construir el sistema real.
Expone la misma superficie de API que usara el sistema final y recorre circuitos
completos de dominio usando datos mock precargados en memoria.

No usa base de datos, ni JPA, ni Flyway, ni seguridad real. Su estado desaparece
con cada reinicio salvo por la precarga de MockDataFactory.

### Que debe sobrevivir al reemplazo

- Las **reglas de dominio** codificadas en los supports y en PrototipoStore:
  condiciones de cierre, secuencia fallo-firma-notificacion-firmeza, bloqueantes
  materiales, distincion pago voluntario vs. pago condena, archivo/reingreso,
  paralizacion/reactivacion, gestion externa, portal infractor sin estados
  internos sensibles.
- Los **contratos publicos**: rutas, DTOs, estructura de responses y strings
  legibles mientras Angular dependa de ellos.
- Los **eventos de auditoria** (append-only): todo cambio de estado relevante
  debe quedar registrado como evento en el dominio real.

### Que debe desaparecer

- PrototipoStore como store en memoria (mapas compartidos).
- MockDataFactory y todo el dataset mock.
- crearActaMockDemo y los endpoints de reset/creacion demo
  (POST /reset, POST /actas/mock).
- Los contadores mock (AtomicInteger de correlativo demo).
- Marcas operativas mock no semanticas (p. ej., accionPendientePorActa como
  mapa volatil en lugar de estado persistido).

### Que no debe romper Angular

Mientras el frontend Angular este desplegado contra el prototipo o el backend
real recien integrado:

- Las rutas /api/prototipo/... no deben cambiar de firma.
- Los campos de los DTOs de response no deben renombrarse ni eliminarse sin
  coordinacion con frontend.
- Los strings legibles en campos como bandeja, estadoProceso, resultadoFinal
  y lectura no deben convertirse en codigos opacos.
- Los status codes HTTP actuales (200 / 400 / 404 / 409) deben preservarse
  por condicion semantica equivalente.

---

## 2. Contratos publicos a preservar

### 2.1 Prefijo base

Todas las rutas tienen prefijo /api/prototipo. No cambiarlo salvo migracion
coordinada con frontend.

### 2.2 Rutas por controller

| Controller | Rutas representativas | Sensibilidad Angular |
|---|---|---|
| PrototipoConsultaActasController | GET /bandejas, GET /bandejas/{codigo}/actas, GET /actas/{id}, GET /actas/buscar, GET /actas/{id}/eventos, GET /actas/{id}/documentos, GET /actas/{id}/notificaciones | **Alta** - bandeja principal, detalle de acta |
| PrototipoFalloApelacionController | POST /actas/{id}/acciones/dictar-fallo-*, registrar-vencimiento-plazo-apelacion, registrar-apelacion, resolver-apelacion, marcar-resultado-final-absuelto | **Alta** |
| PrototipoPagosController | POST /actas/{id}/acciones/registrar-solicitud-pago-voluntario, fijar-monto-pago-voluntario, registrar-vencimiento-pago-voluntario, registrar-pago-informado, adjuntar-comprobante-pago-informado, confirmar-pago-informado, confirmar-pago-voluntario-externo, observar-pago-informado, informar-pago-condena, confirmar-pago-condena, observar-pago-condena, consentir-condena-y-registrar-pago | **Alta** |
| PrototipoDocumentosFirmaController | POST /actas/{id}/acciones/generar-medida-preventiva, generar-notificacion-acta, generar-nulidad, generar-resolucion, generar-rectificacion, firmar-documento/{documentoId} | **Alta** |
| PrototipoNotificacionesInternasController | POST /actas/{id}/acciones/registrar-notificacion-positiva, registrar-notificacion-negativa, registrar-notificacion-vencida, reintentar-notificacion, reintentar-notificacion-vencida | **Alta** |
| PrototipoMaterialesController | POST /actas/{id}/acciones/resolver-pendiente-bloqueante-cierre, registrar-constatacion-material-temprana, registrar-medida-preventiva-posterior, reconocer-origen-bloqueo-cierre-material, registrar-resolucion-bloqueo-cierre, registrar-cumplimiento-material-bloqueo-cierre | **Alta** |
| PrototipoArchivoParalizacionController | POST /actas/{id}/acciones/cerrar-acta, archivar-acta, archivar-por-vencimiento, reingresar-acta, reactivar-acta, paralizar-acta, enviar-a-notificacion, anular-acta | **Alta** |
| PrototipoGestionExternaController | POST /actas/{id}/acciones/derivar-a-apremio, derivar-a-juzgado-de-paz, reingresar-desde-gestion-externa, y variantes de apremio/juzgado | **Media** |
| PrototipoPortalInfractorController | GET /infractor/actas/{codigoQr}, POST /infractor/actas/{codigoQr}/acciones/* | **Alta** - canal ciudadano |
| PrototipoNotificacionesExternasController | GET/POST /notificaciones/notificador-municipal/*, /notificaciones/correo/* | **Media** |
| PrototipoAdminController | GET /health, POST /reset, POST /actas/mock | **Baja / transitoria** - solo demo |

### 2.3 DTOs y responses criticos

Los siguientes contratos no deben cambiar sin coordinacion con frontend:

- ActaResumenResponse - campos que alimentan bandejas.
- ActaDetalleResponse - campos que alimentan el detalle de acta.
- ActaEventoResponse - campos de auditoria visible.
- ActaDocumentoResponse - campos del catalogo de documentos.
- ActaNotificacionResponse - campos del historial de notificaciones.
- Responses de acciones: campos estado, mensaje, lectura usados por Angular
  para decisiones de UI.

### 2.4 Strings legibles de control

Los siguientes strings son consumidos por Angular para logica de display y
habilitacion de acciones. No convertirlos a codigos opacos:

- Valores de bandeja: PENDIENTE_FIRMA, PENDIENTE_ANALISIS, EN_NOTIFICACION,
  CERRADAS, ARCHIVO, etc.
- Valores de resultadoFinal: ABSUELTO, CONDENADO, CONDENA_FIRME,
  PAGO_CONFIRMADO, etc.
- Valores de estadoProceso: PENDIENTE_FIRMA, PENDIENTE_REVISION,
  PENDIENTE_ENVIO, etc.
- Campo lectura en responses de cerrabilidad y bloqueantes: texto legible para
  QA y Direccion; no reemplazar por codigo.

---

## 3. Reglas de dominio que deben sobrevivir

Estas reglas estan implementadas en el prototipo y representan decisiones de
modelo que deben trasladarse fielmente al dominio real. No son simplificaciones
demo: son el modelo operativo.

### 3.1 La bandeja no es verdad primaria

La bandeja es una **proyeccion operativa derivada** del estado real del acta.
No es una entidad con ciclo de vida propio. En el dominio real debe calcularse
a partir del estado del expediente, no guardarse como campo mutable independiente.
Si se guarda como optimizacion de lectura, debe recalcularse ante cualquier
transicion de estado.

### 3.2 Cierre explicito

El expediente no se cierra automaticamente al confirmar el resultado final.
CierreSupport valida condiciones antes de materializar el cierre. El operador
ejecuta cerrar-acta de forma deliberada. Esta separacion entre "resultado final
fijado" y "expediente cerrado" debe preservarse.

### 3.3 Pago confirmado no cierra si hay bloqueantes

PAGO_CONFIRMADO como resultado final no es suficiente para cerrar. Si existen
pendientes bloqueantes de cierre (materiales o documentales), el cierre se
rechaza hasta que se resuelvan. Esta logica vive en CerrabilidadSupport y
CierreSupport de forma encadenada.

### 3.4 Pago condena no se convierte en pago voluntario

Son dos circuitos distintos con semantica diferente:

- Pago voluntario: el infractor paga antes del fallo.
- Pago condena: el infractor paga la multa fijada en el fallo condenatorio
  despues de que la condena queda firme.

No mezclarlos. No permitir que un pago condena registrado se reinterprete como
pago voluntario ni viceversa.

### 3.5 Fallo condenatorio requiere firma -> notificacion -> firmeza

La secuencia es estricta:

1. Dictado del fallo: documento en PENDIENTE_FIRMA.
2. Firma del documento: acta pasa a PENDIENTE_NOTIFICACION.
3. Notificacion positiva del fallo: resultado fijado, plazo de apelacion abierto.
4. Vencimiento del plazo sin apelacion: CONDENA_FIRME.

No saltear pasos. No fijar resultadoFinal = CONDENADO antes de la notificacion.
No fijar CONDENA_FIRME antes del vencimiento del plazo.

### 3.6 Apelacion y vencimiento de plazo

La presentacion de apelacion registra el recurso pero no modifica el resultado
final hasta que se resuelve. La resolucion del recurso puede dar:

- absolucion (resultado -> ABSUELTO),
- confirmacion de condena con monto modificado,
- confirmacion de condena original.

El plazo de apelacion es explicito: se abre con la notificacion positiva y se
cierra con la accion registrar-vencimiento-plazo-apelacion. Sin ese registro
explicito, el portal del infractor puede seguir habilitando la presentacion de
apelacion.

### 3.7 Bloqueantes materiales

Los bloqueantes de cierre tienen dos dimensiones complementarias:

- Documental: se requiere un documento resolutorio firmado.
- Material: el cumplimiento fisico efectivo (devolucion de documentacion,
  liberacion de rodado, etc.) debe registrarse por separado despues del documento.

Un documento resolutorio firmado **no implica** cumplimiento material. Ambos
deben registrarse explicitamente antes de que el bloqueante desaparezca.

### 3.8 Archivo y reingreso

El archivo no equivale al cierre. Un acta archivada puede reingresarse. El
reingreso es un evento, no una edicion del estado: el expediente vuelve a un
estado operativo por evento, no por rollback. Los eventos anteriores al archivo
se conservan.

### 3.9 Gestion externa

La derivacion a apremio o juzgado de paz no borra el estado interno del
expediente: lo suspende operativamente. Al reingresar desde gestion externa,
el resultado (absuelto, condena confirmada, monto modificado) se aplica sobre
el expediente ya existente, no sobre uno nuevo.

### 3.10 Paralizacion y reactivacion

Paralizar suspende el tramite sin archivarlo ni cerrarlo. Reactivar lo devuelve
al estado activo. Ambas son operaciones explicitas con evento. No existe
auto-paralizacion implicita.

### 3.11 Portal infractor sin estados internos sensibles

El endpoint GET /infractor/actas/{codigoQr} proyecta una vista reducida del
acta orientada al ciudadano. No debe exponer:

- estados de bandeja interna operativa,
- documentos internos no dirigidos al infractor,
- marcas de proceso interno (p. ej., accionPendiente, situacionAdministrativa
  en su forma cruda).

La regla de que acciones habilitar al infractor (pagar voluntariamente, consentir
condena, apelar, ver documentos) debe derivarse de reglas de dominio, no de
mapas de estado internos arbitrarios.

---

## 4. Partes transitorias del prototipo

Estas partes deben desaparecer o reemplazarse al integrar el dominio real.
No invertir esfuerzo en refactorizarlas salvo bug real.

### 4.1 PrototipoStore como store en memoria

PrototipoStore mantiene el estado completo de todas las actas en 22 mapas
HashMap + 1 AtomicInteger en memoria JVM. El estado desaparece con cada reinicio.
En el dominio real este estado vive en la base de datos y se accede a traves de
repositorios y servicios.

Los tipos publicos declarados como records/enums dentro de PrototipoStore
(resultados de acciones, estados mock) tambien son transitorios: en el dominio
real seran reemplazados por tipos de dominio reales o responses directos.

### 4.2 Mapas compartidos

Los 22 mapas (actas, eventosPorActa, documentosPorActa, notificacionesPorActa,
situacionPagoCondenaPorActa, etc.) son la "base de datos" en memoria del
prototipo. Todos los supports reciben referencias a estos mapas en su constructor.
Esta es la unica razon por la que los supports no son stateless: en el dominio
real los supports se convertiran en servicios que lean/escriban contra
persistencia real.

### 4.3 MockDataFactory

Clase de ~4 730 lineas que precarga el dataset mock al inicio de la aplicacion.
Genera actas de distintos tipos (transito, bromatologia, documental), con
documentos, eventos, notificaciones y estados variados. No tiene contraparte en
el dominio real. Debe desaparecer completamente al integrar datos reales.

### 4.4 crearActaMockDemo

El metodo crearActaMockDemo en PrototipoStore y el endpoint
POST /api/prototipo/actas/mock permiten crear actas de prueba ad-hoc en tiempo
de ejecucion. Son infraestructura de demo pura. No tienen equivalente operativo
en el dominio real (las actas reales son generadas por el sistema de labrado).
No refactorizarlos: desapareceran al integrar.

### 4.5 Endpoint POST /api/prototipo/reset

Reinicia el estado en memoria a la precarga inicial. Infraestructura de demo.
No tiene equivalente en produccion. Retirar al integrar dominio real.

### 4.6 Contadores mock

contadorCorrelativoDemo (AtomicInteger) genera identificadores demo
incrementales. En el dominio real los identificadores provienen de secuencias de
base de datos o del sistema de labrado de actas.

### 4.7 Marcas operativas transitorias

accionPendientePorActa (mapa String-String) es un mecanismo de marcas operativas
volatiles en memoria que indica que accion esta pendiente para un acta
(p. ej., REINTENTAR_NOTIFICACION, EVALUAR_NOTIFICACION_VENCIDA). En el dominio
real esta informacion es estado persistido del expediente, no un mapa JVM volatil.

---

## 5. Supports y su rol en la transicion

Clasificacion de cada support segun su relevancia para el dominio real.

| Support | Aprox. lineas | Clasificacion | Observaciones |
|---|---|---|---|
| CerrabilidadSupport | ~1 426 | **Regla reusable / candidato a servicio real** | Concentra la logica de condiciones de cierre, bloqueantes materiales y documentales, sincronizacion de origenes. La logica es de dominio puro; la implementacion depende del store en memoria pero el modelo es trasladable. |
| FalloPlazoApelacionSupport | ~1 017 | **Regla reusable / candidato a servicio real** | Implementa el circuito juridico minimo: dictado de fallo, deteccion de fallo pendiente de notificacion, materializacion post-notificacion, apertura y vencimiento de plazo de apelacion. Logica de dominio trasladable. |
| CorreoPostalNotificacionSupport | ~1 119 | **Infraestructura mock externa** | Simula el circuito de lotes de correo postal (generacion, anulacion, respuestas). Candidato a reemplazar por integracion real con el proveedor postal. |
| PiezasFirmaSupport | ~697 | **Regla reusable + orquestacion prototipo** | La logica de produccion de piezas y firma es de dominio. La orquestacion hacia NotificacionSupport y la manipulacion directa de documentosPorActa son infraestructura mock. Al integrar, la parte de dominio debe preservarse; la orquestacion interna debe reescribirse contra servicios reales. |
| GestionExternaSupport | ~713 | **Regla reusable + orquestacion prototipo** | Circuitos de derivacion a apremio y juzgado de paz, y sus variantes de reingreso. La logica de transiciones es de dominio. La integracion real con apremio/juzgado requerira adaptar la orquestacion. |
| NotificacionSupport | ~548 | **Regla reusable + infraestructura mock** | Las transiciones de notificacion (positiva, negativa, vencida, reintento) son de dominio. La materializacion de destinatarios demo y la gestion de notificacionesPorActa en memoria son mock. |
| ArchivoReingresoSupport | ~528 | **Regla reusable + infraestructura mock** | La logica de archivo (por archivar, por vencimiento) y reingreso es de dominio. La gestion directa del mapa actas es infraestructura mock. |
| PagoVoluntarioSupport | ~485 | **Regla reusable / candidato a servicio real** | El circuito de pago voluntario (solicitud, fijacion de monto, vencimiento, confirmacion externa, confirmacion informada) es de dominio. Reusable como modelo de transiciones. |
| PagoInformadoSupport | ~331 | **Regla reusable** | Circuito de pago informado (registro, adjuntar comprobante, confirmar, observar). Logica de dominio trasladable. |
| PrototipoConstantes | ~299 | **Vocabulario de dominio + constantes mock** | Concentra nombres de bandejas, tipos de documento, estados, y predicados. El vocabulario de dominio debe trasladarse; las constantes mock (p. ej., nombres de archivo demo) deben descartarse. |
| NotificadorMunicipalSupport | ~257 | **Infraestructura mock externa** | Simula el protocolo de acuses del notificador municipal. Candidato a reemplazar por integracion real con el servicio de notificacion municipal. |
| ParalizacionReactivacionSupport | ~257 | **Regla reusable** | Paralizacion y reactivacion como eventos explicitos. Logica simple y trasladable. |
| BandejaConsultaSupport | ~257 | **Proyeccion UX - no verdad primaria** | Calcula las vistas de bandejas y conteos a partir del estado en memoria. En el dominio real sera una capa de proyeccion/consulta sobre datos reales. No debe convertirse en fuente de verdad del estado del expediente. |
| PagoCondenaSupport | ~171 | **Regla reusable** | Circuito de pago de condena (informar, confirmar, observar, consentir). Separado del pago voluntario por diseno; preservar esa separacion. |
| CierreSupport | ~138 | **Regla reusable** | Materializa el cierre efectivo una vez validadas las condiciones. Pequeno pero critico: es el unico lugar donde el expediente pasa a CERRADAS. |
| PortalInfractorSupport | ~420 | **Regla de proyeccion ciudadana** | Proyecta la vista del infractor (que ve, que puede hacer). Logica de filtrado y habilitacion de acciones ciudadanas que debe preservarse al integrar, adaptada para leer del dominio real. |
| PrototipoStoreUtil | ~30 | **Infraestructura prototipo** | Utilidades internas del store mock. No tiene contraparte directa en dominio real. |

---

## 6. Riesgos al conectar el dominio real

### 6.1 No modelar por bandeja

La bandeja es una vista derivada. Si al integrar el dominio real se toma la
bandeja como entidad con estado propio (campo bandeja editable), se introduce
un circuito inconsistente con el estado real del expediente.

### 6.2 No duplicar reglas en Angular

Las reglas de habilitacion de acciones (que se puede hacer en cada estado) deben
vivir en el backend. Si Angular implementa sus propias reglas de habilitacion
basadas en campos del response, cualquier cambio de modelo requerira coordinacion
frontend-backend. El backend debe exponer explicitamente que acciones estan
disponibles por acta.

### 6.3 No perder eventos de auditoria

En el prototipo, eventosPorActa es append-only: cada transicion agrega un evento
sin borrar los anteriores. Al integrar con dominio real, esta propiedad debe
preservarse. No actualizar el estado sin registrar el evento correspondiente.
No sobrescribir el historial. Los eventos son la fuente de auditoria.

### 6.4 No convertir strings legibles a codigos opacos en responses de control

Los campos bandeja, resultadoFinal, estadoProceso y lectura son strings legibles
por humanos (QA, Direccion, operadores). Si al integrar se reemplazan por codigos
numericos o identificadores tecnicos en los responses de la API, Angular y los
procesos de QA dejaran de funcionar sin una migracion coordinada.

### 6.5 No cerrar automaticamente

El cierre siempre es una accion explicita del operador. No introducir logica de
auto-cierre basada en eventos (p. ej., "si confirmo el pago, cerrar
automaticamente"). El prototipo valida esta separacion de forma deliberada.

### 6.6 No mezclar pago voluntario con pago condena

Son circuitos distintos con estados distintos y semantica distinta. No
unificarlos bajo una abstraccion generica de "pago" que pierda la separacion.
El momento del proceso en que ocurre cada uno (antes vs. despues del fallo firme)
es parte de la semantica de dominio.

### 6.7 No eliminar bloqueantes por documento firmado sin cumplimiento material

Un bloqueante de cierre tiene dos condiciones independientes: documental y
material. El documento firmado resuelve la condicion documental. El cumplimiento
material efectivo (devolucion fisica, liberacion real) se registra por separado.
Al integrar, no colapsar ambas condiciones en una sola: el dominio real tambien
tiene verificacion material independiente.

### 6.8 No eliminar la distincion archivo vs. cierre

Un expediente archivado no es un expediente cerrado. Tienen ciclos de vida
distintos y la reversibilidad (reingreso) solo aplica al archivo. Si al integrar
se unifica en un solo estado "finalizado", se pierde la capacidad de reingresar
expedientes archivados.

### 6.9 No exponer estado interno sensible en el portal infractor

El portal del infractor es un canal publico. Al reemplazar PortalInfractorSupport
por una proyeccion real, verificar que no se filtren marcas operativas internas,
bandejas internas, o estados de tramitacion interna no destinados al ciudadano.

---

## 7. Orden sugerido de integracion futura

La integracion debe ser incremental, manteniendo el prototipo funcional durante
la transicion. Se sugiere el siguiente orden de etapas:

### Etapa 1 - Leer actas reales

Conectar GET /actas/{id} y GET /actas/buscar contra datos reales. Mantener el
resto del prototipo en memoria. Validar que el detalle de acta real es compatible
con ActaDetalleResponse.

### Etapa 2 - Proyectar detalle compatible

Asegurar que los campos de ActaDetalleResponse se alimentan correctamente desde
el dominio real para todos los tipos de acta (transito, bromatologia, documental).
Validar con Angular antes de continuar.

### Etapa 3 - Proyectar bandejas reales

Conectar GET /bandejas y GET /bandejas/{codigo}/actas contra datos reales.
BandejaConsultaSupport se convierte en capa de proyeccion/consulta sobre
repositorios reales. Validar conteos y filtros con QA.

### Etapa 4 - Ejecutar acciones contra dominio real (una por una)

Reemplazar cada accion mock por su equivalente real en orden de criticidad:

1. Firma de documentos.
2. Notificaciones internas.
3. Fallo y plazo de apelacion.
4. Cierre.
5. Pagos (voluntario, informado, condena).
6. Bloqueantes materiales.
7. Archivo y reingreso.
8. Gestion externa.
9. Paralizacion y reactivacion.
10. Portal infractor.
11. Notificaciones externas (notificador municipal, correo postal).

Cada accion reemplazada debe registrar el evento correspondiente en el dominio
real antes de darse por integrada.

### Etapa 5 - Reemplazar mocks por persistencia/servicios reales

Una vez que todas las acciones estan integradas, reemplazar:

- MockDataFactory por datos reales.
- Mapas en memoria por repositorios reales.
- Marcas operativas volatiles por estado persistido.

### Etapa 6 - Retirar infraestructura demo

Ultima etapa. Solo cuando el sistema real es estable:

- Retirar POST /reset y POST /actas/mock.
- Retirar PrototipoAdminController o vaciar su contenido operativo.
- Retirar MockDataFactory.
- Retirar crearActaMockDemo.
- Limpiar contadores mock y marcas transitorias.

No retirar infraestructura demo antes de que las etapas anteriores esten
validadas. El reset y la creacion mock son herramientas de QA valiosas durante
la transicion.

---

## 8. Relacion con documentos existentes

| Documento | Relacion |
|---|---|
| docs/DIAGNOSTICO_PROTOTIPO_STORE.md | Diagnostico tecnico del store: inventario de tamanios, mapas, dependencias. Complementa este documento con detalle de implementacion. |
| docs/ARQUITECTURA_PROTOTIPO_CONTROLLERS.md | Arquitectura de controllers: describe la descomposicion de PrototipoApiController en controllers especializados. |
| docs/MATRIZ_REGLAS_ACTA.md | Matriz de reglas de dominio por estado y accion. Fuente de verdad para las reglas del dominio. |
| spec/ | Fuente primaria de verdad del modelo. Ante contradiccion entre este documento y la spec, prevalece la spec. |