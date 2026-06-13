# Plan de integracion al dominio real

## 1. Resumen ejecutivo

### Objetivo

Reemplazar, de forma incremental y sin romper el frontend Angular, los
subsistemas mock del prototipo (`PrototipoStore`, `MockDataFactory` y sus
`*Support`) por implementaciones que lean y persistan datos reales, respetando
el modelo de dominio ya consolidado en `spec/`.

### Alcance

El plan cubre el modulo `backend/api-faltas-prototipo` exclusivamente.
No afecta el frontend Angular ni los modulos backend productivos existentes
hasta que una etapa concrete su integracion.

### Que se integra primero

1. Lectura de actas reales (busqueda y detalle).
2. Proyeccion real de bandejas.
3. Eventos y documentos reales.
4. Acciones nucleo, una por una, en el orden de la Etapa 4.

### Que queda mock temporalmente

- `MockDataFactory` y su dataset de actas de demo.
- `/reset` y `/actas/mock` (infraestructura de demo).
- `contadorActaLabradoMockDemo` y los mapas paralelos de estado mock.
- Cualquier accion nucleo cuya entidad/servicio real aun no este disenada.

### Principio rector

Angular siempre debe funcionar. Cada paso de integracion reemplaza un
subsistema mock por un subsistema real, manteniendo contratos de respuesta
identicos. No se mezclan circuitos.

---

## 2. Estado actual del prototipo

### Controllers especializados (al cierre del ultimo slice)

| Controller | Ruta base | Responsabilidad |
|---|---|---|
| `PrototipoConsultaActasController` | `/api/prototipo` | Bandejas, detalle, busqueda, eventos, documentos, notificaciones |
| `PrototipoDocumentosFirmaController` | `/api/prototipo` | Generacion de piezas y firma documental |
| `PrototipoNotificacionesInternasController` | `/api/prototipo` | Notificaciones internas (positiva, negativa, vencida, reintento) |
| `PrototipoFalloApelacionController` | `/api/prototipo` | Fallo, apelacion, plazo de apelacion |
| `PrototipoPagosController` | `/api/prototipo` | Pago voluntario, pago informado, pago condena |
| `PrototipoMaterialesController` | `/api/prototipo` | Constatacion material, medida preventiva, bloqueantes de cierre |
| `PrototipoArchivoParalizacionController` | `/api/prototipo` | Cierre, archivo, reingreso, paralizacion, reactivacion, nulidad, envio a notificacion |
| `PrototipoGestionExternaController` | `/api/prototipo` | Derivacion a apremio/juzgado y retorno desde gestion externa |
| `PrototipoNotificacionesExternasController` | `/api/prototipo` | Notificador municipal y correo postal |
| `PrototipoPortalInfractorController` | `/api/prototipo/infractor` | Portal ciudadano: visualizacion, apelacion, pago |
| `PrototipoAdminController` | `/api/prototipo` | Health, reset, alta mock demo |

### Supports principales

| Clase | Responsabilidad |
|---|---|
| `ArchivoReingresoSupport` | Archivo directo/vencimiento, reingreso, motivoArchivo |
| `BandejaConsultaSupport` | Proyeccion de bandejas y filtros operativos |
| `CerrabilidadSupport` | Computo de cerrabilidad, pendientes, cumplimientos materiales |
| `CierreSupport` | Cierre efectivo del expediente |
| `CorreoPostalNotificacionSupport` | Generacion de lotes de correo y trazabilidad postal |
| `FalloPlazoApelacionSupport` | Fallo absolutorio/condenatorio, notificacion de fallo, plazo de apelacion, condena firme |
| `GestionExternaSupport` | Derivacion a apremio/juzgado, retorno y resultado externo |
| `NotificacionSupport` | Notificacion positiva, negativa, vencida, reintento |
| `NotificadorMunicipalSupport` | Notificacion via notificador municipal y acuse |
| `PagoCondenaSupport` | Pago de condena firme |
| `PagoInformadoSupport` | Registro, comprobante, confirmacion/observacion de pago informado |
| `PagoVoluntarioSupport` | Solicitud de pago voluntario temprano |
| `ParalizacionReactivacionSupport` | Paralizacion administrativa transversal y reactivacion |
| `PiezasFirmaSupport` | Produccion de piezas documentales y firma individual |
| `PortalInfractorSupport` | Predicado de revision, documentos visibles, confirmacion portal |

### Store en memoria

`PrototipoStore` es el nucleo mock. Sus mapas principales son:

| Campo | Tipo | Contenido |
|---|---|---|
| `actas` | `Map<String, ActaMock>` | Dataset principal de actas mock |
| `eventosPorActa` | `Map<String, List<ActaEventoMock>>` | Historial de eventos por acta |
| `documentosPorActa` | `Map<String, List<ActaDocumentoMock>>` | Documentos por acta |
| `notificacionesPorActa` | `Map<String, List<ActaNotificacionMock>>` | Notificaciones por acta |
| `piezasRequeridasPorActa` | `Map<String, ActaPiezasRequeridasMock>` | Piezas documentales requeridas |
| `accionPendientePorActa` | `Map<String, String>` | Marca operativa activa (accion pendiente) |
| `observacionParalizacionPorActa` | `Map<String, String>` | Observacion de paralizacion |
| `situacionPagoPorActa` | `Map<String, SituacionPagoMock>` | Situacion de pago voluntario/informado |
| `pagoInformadoPorActa` | `Map<String, PagoInformadoMock>` | Hecho informado de pago voluntario |
| `montoPagoVoluntarioPorActa` | `Map<String, BigDecimal>` | Monto habilitado para pago voluntario |
| `montoCondenaPorActa` | `Map<String, BigDecimal>` | Monto de condena fijado al fallar |
| `situacionPagoCondenaPorActa` | `Map<String, SituacionPagoCondena>` | Situacion de pago de condena firme |
| `tipoPagoPorActa` | `Map<String, TipoPago>` | Tipo de pago registrado |
| `resultadoExternoPostGestionPorActa` | `Map<String, ResultadoExternoPostGestion>` | Resultado de gestion externa |
| `montoCondenaSugeridoPostGestionExternaPorActa` | `Map<String, BigDecimal>` | Monto sugerido post gestion externa |
| `actaTransitoMockPorActa` | `Map<String, ActaTransitoMock>` | Datos de transito por acta |
| `actaBromatologiaMockPorActa` | `Map<String, ActaBromatologiaMock>` | Datos de bromatologia por acta |
| `dependenciaDemoPorActa` | `Map<String, String>` | Dependencia demo por acta |
| `tipoActaDemoPorActa` | `Map<String, String>` | Tipo de acta demo por acta |
| `patenteVehiculoPorActa` | `Map<String, String>` | Patente vehicular por acta |
| `contadorActaLabradoMockDemo` | `AtomicInteger` | Contador de actas mock creadas en vivo |

### Contratos publicos actuales (responses usados por Angular)

- `ActaDetalleResponse` — detalle completo de un acta
- `ActaBandejaItemResponse` — item en listado de bandeja
- `BandejaResponse` — resumen operativo de bandeja con sub-bandejas
- `ActaEventoResponse` — evento del historial
- `ActaDocumentoResponse` — documento del acta
- `ActaNotificacionResponse` — notificacion del acta
- `PrototipoActaBusquedaResponse` — resultado de busqueda global

### Documentos relacionados

- `docs/FRONTERA_PROTOTIPO_DOMINIO_REAL.md` — frontera tecnica por area funcional
- `docs/DIAGNOSTICO_PROTOTIPO_STORE.md` — diagnostico interno del store y sus deudas
- `docs/ARQUITECTURA_PROTOTIPO_CONTROLLERS.md` — arquitectura de controllers especializados
- `docs/MATRIZ_REGLAS_ACTA.md` — reglas de negocio por estado y accion

---

## 3. Principios de integracion

1. **El backend mantiene reglas de dominio.** Angular no duplica reglas de
   habilitacion, transicion ni validacion. Solo consume el resultado de las
   acciones.

2. **Angular no duplica reglas.** Los campos `cerrabilidad`, `accionPendiente`,
   `situacionPago` y similares son calculados en backend y entregados como
   proyeccion. Angular los consume como strings legibles, no los recomputa.

3. **Bandeja es proyeccion, no verdad primaria.** La bandeja actual de un acta
   es consecuencia de sus eventos. No debe persistirse como verdad independiente
   salvo que sea un cache/proyeccion controlada con invalidacion explicita.

4. **Responses siguen con strings legibles.** Los campos enum se exponen como
   strings en los responses (por ejemplo `"PENDIENTE_FIRMA"`, `"CONDENADO"`,
   `"SIN_PAGO"`). No se cambia ese contrato durante la integracion.

5. **Cierre explicito.** El cierre de un expediente es una accion de dominio
   explicitamente invocada. No ocurre automaticamente por ninguna condicion
   calculada. El endpoint `/cerrar-acta` sigue siendo la unica puerta de cierre.

6. **Eventos/auditoria append-only.** Ningun evento se edita ni se borra.
   El historial del expediente es inmutable. La integracion real debe respetar
   este principio.

7. **No mezclar pago voluntario con pago condena.** Son circuitos separados con
   DTOs, estados y flujos distintos. La integracion debe preservar esa
   separacion.

8. **No exponer estados internos en portal ciudadano.** El portal (`/infractor/`)
   solo expone lo que el ciudadano puede ver: documentos notificados, posibilidad
   de pago y posibilidad de apelacion. Estados internos de bandeja, accionPendiente
   y marcas operativas no se exponen.

---

## 4. Etapas de integracion

### Etapa 1 — Lectura real de actas

**Objetivo:** Angular puede ver actas reales en detalle y busqueda.
Las acciones siguen siendo mock.

**Que hacer:**
- Conectar `GET /actas/{id}` contra la fuente de datos real.
- Conectar `GET /actas/buscar` contra la fuente de datos real.
- Mapear acta real a `ActaDetalleResponse`.
- Verificar que todos los campos criticos del response tengan equivalente real.

**Campos criticos a validar en `ActaDetalleResponse`:**
- `id` (actaId persistido)
- `numeroActa` (numero visible para QA y ciudadano)
- `infractorNombre`
- `bloqueActual` (estado juridico/procesal)
- `estadoProcesoActual`
- `situacionAdministrativaActual`
- `bandejaActual`

**Dependencias mock que se mantienen en esta etapa:**
- Todo lo demas: bandejas, eventos, documentos, notificaciones, acciones.

**Riesgos:**
- Campos que en mock siempre tienen valor pueden estar nulos en datos reales.
- El formato de `numeroActa` puede diferir entre mock y real.

---

### Etapa 2 — Proyeccion real de bandejas

**Objetivo:** Angular puede navegar bandejas con datos reales.

**Que hacer:**
- Conectar `GET /bandejas` contra proyeccion real.
- Conectar `GET /bandejas/{codigo}/actas` contra proyeccion real.
- Validar conteos, filtros por `accionPendiente`, `situacionPago`,
  `resultadoFinal`, `cerrable`, `pendienteBloqueante` y `subBandeja`.
- Validar sub-bandejas y sus labels.

**Principio:** La bandeja de un acta se calcula desde su estado real, no se
persiste como campo independiente salvo que sea cache controlado con
invalidacion por evento.

**Dependencias mock que se mantienen en esta etapa:**
- Eventos, documentos, notificaciones, acciones.

**Riesgos:**
- Los filtros compuestos dependen de que los estados secundarios (situacionPago,
  accionPendiente) tambien esten disponibles desde datos reales.
- Las sub-bandejas necesitan la logica de `SubBandejaClasificador`, que hoy
  depende de accionPendiente mock.

---

### Etapa 3 — Eventos y documentos reales

**Objetivo:** El historial visible en Angular refleja eventos y documentos reales.

**Que hacer:**
- Reemplazar `eventosPorActa` por consulta real al historial de eventos.
- Reemplazar `documentosPorActa` por consulta real al repositorio de documentos.
- Mantener auditoria: cada accion ya registrada debe seguir teniendo su evento.
- Validar historial visible en Angular.

**Dependencias mock que se mantienen en esta etapa:**
- Notificaciones, acciones.

**Riesgos:**
- Los tipos de evento usados en mock (`tipoEvento` como string) deben coincidir
  con los valores reales. Si difieren, Angular puede mostrar etiquetas vacias.
- La ordenacion de eventos debe ser consistente con lo que muestra mock hoy
  (por `fechaHora` ascendente).

---

### Etapa 4 — Acciones nucleo una por una

Cada accion debe integrarse de forma independiente, sin afectar las demas.
El orden sugerido refleja la dependencia natural del flujo operativo:

#### Orden sugerido

1. Firma documental
2. Notificaciones internas
3. Fallo / apelacion / plazo
4. Cierre
5. Pagos (voluntario, informado, condena)
6. Bloqueantes materiales
7. Archivo / reingreso
8. Gestion externa
9. Paralizacion / reactivacion
10. Portal infractor
11. Notificaciones externas (notificador municipal, correo postal)

#### Estructura de documentacion por accion

Para cada accion a integrar se debe documentar:

| Campo | Descripcion |
|---|---|
| Endpoint actual | Ruta HTTP del controller |
| Support prototipo actual | Clase `*Support` que lo implementa |
| Regla de dominio asociada | Referencia a `spec/` |
| Entidad/servicio real probable | Pendiente de diseno si no esta definido |
| Evento esperado | Tipo de evento que debe registrarse al ejecutar |
| Precondiciones | Estados o condiciones requeridas para habilitar la accion |
| Postcondiciones | Estado resultante tras la accion exitosa |
| Tests minimos | Casos de prueba recomendados |
| Riesgos | Riesgos especificos de la integracion |

#### Detalle por accion nucleo

**1. Firma documental**

| Campo | Valor |
|---|---|
| Endpoints | `POST /actas/{id}/acciones/generar-medida-preventiva`, `generar-notificacion-acta`, `generar-nulidad`, `generar-resolucion`, `generar-rectificacion`, `firmar-documento/{documentoId}` |
| Support | `PiezasFirmaSupport` |
| Regla dominio | `spec/02-reglas-transversales/` — circuito de firma |
| Entidad real probable | Pendiente de diseno (repositorio de documentos, servicio de firma) |
| Evento esperado | `DOCUMENTO_GENERADO`, `DOCUMENTO_FIRMADO`, `FIRMA_CERRADA` |
| Precondiciones | Acta en `PENDIENTE_PREPARACION_DOCUMENTAL` o `PENDIENTE_FIRMA` segun la pieza |
| Postcondiciones | Documento firmado en repositorio; transicion a `PENDIENTE_NOTIFICACION` o `CERRADAS` por nulidad |
| Tests minimos | Generacion de cada tipo de pieza; firma individual; firma de nulidad cierra por invalidacion |
| Riesgos | La firma de nulidad cierra el expediente: precondicion critica |

**2. Notificaciones internas**

| Campo | Valor |
|---|---|
| Endpoints | `POST /actas/{id}/acciones/registrar-notificacion-positiva`, `registrar-notificacion-negativa`, `registrar-notificacion-vencida`, `reintentar-notificacion`, `reintentar-notificacion-vencida` |
| Support | `NotificacionSupport` |
| Regla dominio | `spec/02-reglas-transversales/02-reglas-de-notificacion.md` |
| Entidad real probable | Pendiente de diseno (entidad notificacion con canal y resultado) |
| Evento esperado | `NOTIFICACION_POSITIVA`, `NOTIFICACION_NEGATIVA`, `NOTIFICACION_VENCIDA`, `REINTENTO_NOTIFICACION` |
| Precondiciones | Acta en `EN_NOTIFICACION` o `PENDIENTE_NOTIFICACION` |
| Postcondiciones | Segun resultado: analisis, nuevo intento o evaluacion vencida |
| Tests minimos | Cada tipo de resultado; caso de reintento post vencida |
| Riesgos | La notificacion de fallo tiene logica especial en `FalloPlazoApelacionSupport`; debe coordinarse |

**3. Fallo / apelacion / plazo**

| Campo | Valor |
|---|---|
| Endpoints | `marcar-resultado-final-absuelto`, `dictar-fallo-absolutorio`, `dictar-fallo-condenatorio`, `registrar-vencimiento-plazo-apelacion`, `registrar-apelacion`, `resolver-apelacion` |
| Support | `FalloPlazoApelacionSupport` |
| Regla dominio | `spec/` — circuito de fallo y apelacion |
| Entidad real probable | Pendiente de diseno (fallo, resolucion de apelacion) |
| Evento esperado | `FALLO_ABSOLUTORIO`, `FALLO_CONDENATORIO`, `APELACION_REGISTRADA`, `APELACION_RESUELTA`, `CONDENA_FIRME` |
| Precondiciones | Acta en `PENDIENTE_ANALISIS`; existencia de fallo para habilitar plazo |
| Postcondiciones | Resultado final fijado; plazo abierto si condenatorio; condena firme si vence plazo sin apelacion |
| Tests minimos | Circuito absolutorio completo; circuito condenatorio con vencimiento; circuito con apelacion |
| Riesgos | `resultadoFinal` es calculado en el store; si hay persistencia real debe asegurarse consistencia |

**4. Cierre**

| Campo | Valor |
|---|---|
| Endpoints | `POST /actas/{id}/acciones/cerrar-acta` |
| Support | `CierreSupport`, `CerrabilidadSupport` |
| Regla dominio | `spec/` — condiciones de cierre |
| Entidad real probable | Pendiente de diseno (evento de cierre persistido) |
| Evento esperado | `ACTA_CERRADA` |
| Precondiciones | `CerrabilidadSupport.getCerrabilidadActa` retorna `CERRABLE` |
| Postcondiciones | Acta en `CERRADAS`; sin marcas operativas activas |
| Tests minimos | Cierre con condena firme pagada; cierre con absolucion; intento de cierre sin cumplir precondiciones |
| Riesgos | No debe ocurrir cierre automatico; el endpoint debe ser la unica puerta |

**5. Pagos**

| Campo | Valor |
|---|---|
| Endpoints | `registrar-solicitud-pago-voluntario`, `fijar-monto-pago-voluntario`, `registrar-vencimiento-pago-voluntario`, `registrar-pago-informado`, `adjuntar-comprobante-pago-informado`, `confirmar-pago-informado`, `confirmar-pago-voluntario-externo`, `observar-pago-informado`, `informar-pago-condena`, `confirmar-pago-condena`, `observar-pago-condena`, `consentir-condena-y-registrar-pago` |
| Supports | `PagoVoluntarioSupport`, `PagoInformadoSupport`, `PagoCondenaSupport` |
| Regla dominio | `spec/` — circuito de pagos, separacion voluntario/condena |
| Entidad real probable | Pendiente de diseno (entidades de pago por tipo) |
| Evento esperado | Evento especifico por tipo de pago y resultado |
| Precondiciones | Distintas segun tipo: pago voluntario requiere monto habilitado; pago condena requiere condena firme |
| Postcondiciones | Estado de pago actualizado; habilitacion de cierre si corresponde |
| Tests minimos | Circuito voluntario completo; circuito condena completo; intento de mezclar circuitos (debe fallar) |
| Riesgos | No mezclar `situacionPagoPorActa` con `situacionPagoCondenaPorActa`; son estados independientes |

**6. Bloqueantes materiales**

| Campo | Valor |
|---|---|
| Endpoints | `resolver-pendiente-bloqueante-cierre`, `registrar-constatacion-material-temprana`, `registrar-medida-preventiva-posterior`, `reconocer-origen-bloqueo-cierre-material`, `registrar-resolucion-bloqueo-cierre`, `registrar-cumplimiento-material-bloqueo-cierre` |
| Support | `CerrabilidadSupport` (logica de bloqueantes) |
| Regla dominio | `spec/` — condiciones materiales bloqueantes de cierre |
| Entidad real probable | Pendiente de diseno |
| Evento esperado | Eventos de constatacion, medida, bloqueo, resolucion y cumplimiento |
| Precondiciones | Segun tipo de bloqueante y estado del acta |
| Postcondiciones | Bloqueante resuelto o registrado; impacto en cerrabilidad |
| Tests minimos | Bloqueo por medida preventiva; resolucion de bloqueo; intento de cierre con bloqueante activo |
| Riesgos | Los bloqueantes materiales y documentales son distintos; no colapsar en una sola entidad |

**7. Archivo / reingreso**

| Campo | Valor |
|---|---|
| Endpoints | `archivar-acta`, `archivar-por-vencimiento`, `reingresar-acta` |
| Support | `ArchivoReingresoSupport` |
| Regla dominio | `spec/` — circuito de archivo y reingreso |
| Entidad real probable | Pendiente de diseno |
| Evento esperado | `ACTA_ARCHIVADA`, `ACTA_ARCHIVADA_POR_VENCIMIENTO`, `ACTA_REINGRESADA` |
| Precondiciones | Acta en `PENDIENTE_ANALISIS` para archivar; en `ARCHIVO` para reingresar |
| Postcondiciones | Bandeja cambia a `ARCHIVO` o retorno a `PENDIENTE_ANALISIS` con marca `REVISION_POST_REINGRESO` |
| Tests minimos | Archivo directo; archivo por vencimiento; reingreso desde archivo |
| Riesgos | El reingreso tiene marca operativa especial; debe preservarse en integracion real |

**8. Gestion externa**

| Campo | Valor |
|---|---|
| Endpoints | `derivar-a-apremio`, `derivar-a-juzgado-de-paz`, `reingresar-desde-gestion-externa`, `apremio-reingresar-sin-pago`, `apremio-registrar-pago`, `apremio-reingresar-monto-modificado`, `apremio-reingresar-absuelto`, `juzgado-reingresar-absuelto`, `juzgado-reingresar-condena-confirmada`, `juzgado-reingresar-monto-modificado` |
| Support | `GestionExternaSupport` |
| Regla dominio | `spec/` — circuito de gestion externa |
| Entidad real probable | Pendiente de diseno (tipo de gestion, resultado externo) |
| Evento esperado | `DERIVADO_A_APREMIO`, `DERIVADO_A_JUZGADO`, `REINGRESO_DESDE_GESTION_EXTERNA` y variantes |
| Precondiciones | Marca `DERIVAR_GESTION_EXTERNA` activa para derivar; acta en `GESTION_EXTERNA` para retornar |
| Postcondiciones | Acta en `GESTION_EXTERNA` o retorno a `PENDIENTE_ANALISIS` con marca correspondiente |
| Tests minimos | Circuito apremio completo; circuito juzgado completo; intento de derivar sin precondicion |
| Riesgos | El resultado externo puede modificar montoCondena; la integracion real debe preservar trazabilidad |

**9. Paralizacion / reactivacion**

| Campo | Valor |
|---|---|
| Endpoints | `paralizar-acta`, `reactivar-acta` |
| Support | `ParalizacionReactivacionSupport` |
| Regla dominio | `spec/` — paralizacion administrativa transversal |
| Entidad real probable | Pendiente de diseno |
| Evento esperado | `ACTA_PARALIZADA`, `ACTA_REACTIVADA` |
| Precondiciones | Acta elegible para paralizacion (predicado en `ParalizacionReactivacionSupport`) |
| Postcondiciones | Acta en `PARALIZADAS`; reactivacion retorna a bandeja anterior con observacion preservada |
| Tests minimos | Paralizacion desde distintas bandejas; reactivacion con observacion |
| Riesgos | La observacion de paralizacion se guarda en `observacionParalizacionPorActa`; debe persistirse |

**10. Portal infractor**

| Campo | Valor |
|---|---|
| Endpoints | `GET /infractor/actas/{codigoQr}`, `registrar-apelacion`, `confirmar-visualizacion-notificacion`, `solicitar-pago-voluntario`, `pagar-voluntario`, `documentos/{tipoDocumento}/ver`, `pagar-condena`, `consentir-condena` |
| Support | `PortalInfractorSupport` |
| Regla dominio | `spec/` — portal ciudadano |
| Entidad real probable | Pendiente de diseno (lookup por codigoQr, documentos visibles por canal) |
| Evento esperado | `NOTIFICACION_PORTAL_CONFIRMADA`, `APELACION_PORTAL`, `PAGO_VOLUNTARIO_PORTAL` |
| Precondiciones | `codigoQr` valido y resolvible a un actaId; notificacion pendiente de visualizacion |
| Postcondiciones | Estado de notificacion actualizado; pago registrado si corresponde |
| Tests minimos | Visualizacion por QR; confirmacion de notificacion; pago desde portal |
| Riesgos | No exponer estados internos (`bandejaActual`, `accionPendiente`) en respuestas del portal |

**11. Notificaciones externas**

| Campo | Valor |
|---|---|
| Endpoints | `GET /notificaciones/notificador-municipal`, `POST .../acuse`, `GET /notificaciones/correo/listas-para-lote`, `GET .../lotes`, `POST .../lotes/{loteId}/anular`, `POST .../correo/{notificacionId}/enviar-individual`, `POST .../lotes/generar`, `GET .../trazabilidad`, `POST .../respuestas/procesar-demo` |
| Supports | `NotificadorMunicipalSupport`, `CorreoPostalNotificacionSupport` |
| Regla dominio | `spec/02-reglas-transversales/02-reglas-de-notificacion.md` |
| Entidad real probable | Pendiente de diseno (integracion con sistema de notificaciones externas) |
| Evento esperado | `ACUSE_NOTIFICADOR_MUNICIPAL`, `CORREO_ENVIADO`, `LOTE_GENERADO` |
| Precondiciones | Notificaciones en estado que habilita envio externo |
| Postcondiciones | Estado de notificacion actualizado con resultado externo |
| Tests minimos | Generacion de lote; anulacion de lote; procesamiento de respuesta |
| Riesgos | Esta area tiene mas dependencias externas; integrar al ultimo |

---

## 5. Matriz endpoint / integracion

### PrototipoConsultaActasController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `GET /bandejas` | Listar bandejas con resumen | `BandejaConsultaSupport` | `actas` en memoria | Proyeccion real de bandejas | Alta | Medio | Depende de Etapa 2 |
| `GET /bandejas/{codigo}/actas` | Listar actas por bandeja + filtros | `BandejaConsultaSupport` | `actas`, `accionPendientePorActa`, `situacionPagoPorActa` | Consulta real con filtros | Alta | Alto | Filtros compuestos; depende de Etapa 2 |
| `GET /actas/{id}` | Detalle de acta | `ActaDetalleMapper` | `ActaMock` | Entidad real mapeada a `ActaDetalleResponse` | Alta | Medio | Etapa 1 |
| `GET /actas/buscar` | Busqueda global | `ActaBusquedaHelper` | `actas` en memoria | Busqueda real | Alta | Bajo | Etapa 1 |
| `GET /actas/{id}/eventos` | Historial de eventos | directo al store | `eventosPorActa` | Consulta real de historial | Media | Bajo | Etapa 3 |
| `GET /actas/{id}/documentos` | Documentos del acta | directo al store | `documentosPorActa` | Consulta real de documentos | Media | Bajo | Etapa 3 |
| `GET /actas/{id}/notificaciones` | Notificaciones del acta | directo al store | `notificacionesPorActa` | Consulta real de notificaciones | Media | Medio | Etapa 3 o con Etapa 4-notificaciones |

### PrototipoAdminController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `GET /health` | Estado del servicio | directo al store | `store.getActas().size()` | Pendiente de diseno (metrica real) | Baja | Bajo | Infraestructura demo; no bloquea integracion |
| `POST /reset` | Reinicializar dataset mock | `MockDataFactory` | Todo el store | No aplica en produccion | Baja | — | Retirar en criterio de retiro |
| `POST /actas/mock` | Alta mock en vivo | `MockDataFactory` | `contadorActaLabradoMockDemo` | No aplica en produccion | Baja | — | Retirar en criterio de retiro |

### PrototipoDocumentosFirmaController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `POST /actas/{id}/acciones/generar-medida-preventiva` | Generacion de medida preventiva | `PiezasFirmaSupport` | `documentosPorActa` | Servicio de documentos real | Alta | Alto | Primera accion nucleo sugerida (Etapa 4) |
| `POST /actas/{id}/acciones/generar-notificacion-acta` | Generacion de pieza de notificacion | `PiezasFirmaSupport` | `documentosPorActa` | Servicio de documentos real | Alta | Alto | Coordinar con NotificacionSupport |
| `POST /actas/{id}/acciones/generar-nulidad` | Generacion de nulidad | `PiezasFirmaSupport` | `documentosPorActa` | Servicio de documentos real | Alta | Critico | Firma de nulidad cierra expediente |
| `POST /actas/{id}/acciones/generar-resolucion` | Generacion de resolucion | `PiezasFirmaSupport` | `documentosPorActa` | Servicio de documentos real | Media | Medio | — |
| `POST /actas/{id}/acciones/generar-rectificacion` | Generacion de rectificacion | `PiezasFirmaSupport` | `documentosPorActa` | Servicio de documentos real | Media | Medio | — |
| `POST /actas/{id}/acciones/firmar-documento/{documentoId}` | Firma individual de documento | `PiezasFirmaSupport` | `documentosPorActa` | Servicio de firma real | Alta | Alto | Puede cerrar firma y transicionar bandeja |

### PrototipoNotificacionesInternasController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `POST /actas/{id}/acciones/registrar-notificacion-positiva` | Notificacion positiva | `NotificacionSupport` | `notificacionesPorActa` | Entidad notificacion real | Alta | Alto | Puede ser notificacion de fallo (logica en `FalloPlazoApelacionSupport`) |
| `POST /actas/{id}/acciones/registrar-notificacion-negativa` | Notificacion negativa | `NotificacionSupport` | `notificacionesPorActa` | Entidad notificacion real | Alta | Medio | — |
| `POST /actas/{id}/acciones/registrar-notificacion-vencida` | Notificacion vencida | `NotificacionSupport` | `notificacionesPorActa` | Entidad notificacion real | Alta | Medio | Marca `EVALUAR_NOTIFICACION_VENCIDA` |
| `POST /actas/{id}/acciones/reintentar-notificacion` | Reintento por no entrega | `NotificacionSupport` | `notificacionesPorActa` | Entidad notificacion real | Media | Medio | — |
| `POST /actas/{id}/acciones/reintentar-notificacion-vencida` | Reintento post vencimiento | `NotificacionSupport` | `notificacionesPorActa` | Entidad notificacion real | Media | Medio | — |

### PrototipoFalloApelacionController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `POST /actas/{id}/acciones/marcar-resultado-final-absuelto` | Marca absolucion directa | `FalloPlazoApelacionSupport` | `actas`, `accionPendientePorActa` | Servicio de dominio real | Media | Medio | — |
| `POST /actas/{id}/acciones/dictar-fallo-absolutorio` | Fallo absolutorio | `FalloPlazoApelacionSupport` | `documentosPorActa` | Servicio de dominio real | Alta | Alto | Genera documento; coordinar con firma |
| `POST /actas/{id}/acciones/dictar-fallo-condenatorio` | Fallo condenatorio | `FalloPlazoApelacionSupport` | `documentosPorActa`, `montoCondenaPorActa` | Servicio de dominio real | Alta | Alto | Fija monto condena; coordinar con firma y pagos |
| `POST /actas/{id}/acciones/registrar-vencimiento-plazo-apelacion` | Vencimiento plazo | `FalloPlazoApelacionSupport` | `accionPendientePorActa` | Servicio de dominio real | Media | Medio | — |
| `POST /actas/{id}/acciones/registrar-apelacion` | Registro de apelacion | `FalloPlazoApelacionSupport` | `accionPendientePorActa` | Servicio de dominio real | Media | Medio | — |
| `POST /actas/{id}/acciones/resolver-apelacion` | Resolucion de apelacion | `FalloPlazoApelacionSupport` | `accionPendientePorActa` | Servicio de dominio real | Media | Medio | — |

### PrototipoPagosController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `POST /actas/{id}/acciones/registrar-solicitud-pago-voluntario` | Solicitud pago voluntario | `PagoVoluntarioSupport` | `situacionPagoPorActa` | Entidad pago voluntario real | Alta | Alto | — |
| `POST /actas/{id}/acciones/fijar-monto-pago-voluntario` | Fijacion de monto | `PagoVoluntarioSupport` | `montoPagoVoluntarioPorActa` | Entidad pago voluntario real | Alta | Medio | — |
| `POST /actas/{id}/acciones/registrar-vencimiento-pago-voluntario` | Vencimiento pago voluntario | `PagoVoluntarioSupport` | `situacionPagoPorActa` | Entidad pago voluntario real | Media | Bajo | — |
| `POST /actas/{id}/acciones/registrar-pago-informado` | Registro pago informado | `PagoInformadoSupport` | `pagoInformadoPorActa` | Entidad pago informado real | Alta | Alto | — |
| `POST /actas/{id}/acciones/adjuntar-comprobante-pago-informado` | Adjuntar comprobante | `PagoInformadoSupport` | `pagoInformadoPorActa` | Entidad pago informado real | Alta | Medio | — |
| `POST /actas/{id}/acciones/confirmar-pago-informado` | Confirmacion pago informado | `PagoInformadoSupport` | `situacionPagoPorActa` | Entidad pago informado real | Alta | Medio | — |
| `POST /actas/{id}/acciones/confirmar-pago-voluntario-externo` | Confirmacion pago voluntario externo | `PagoVoluntarioSupport` | `situacionPagoPorActa` | Pendiente de diseno | Media | Alto | Integracion con sistema externo |
| `POST /actas/{id}/acciones/observar-pago-informado` | Observacion pago informado | `PagoInformadoSupport` | `pagoInformadoPorActa` | Entidad pago informado real | Media | Bajo | — |
| `POST /actas/{id}/acciones/informar-pago-condena` | Informar pago condena | `PagoCondenaSupport` | `situacionPagoCondenaPorActa` | Entidad pago condena real | Media | Alto | No mezclar con pago voluntario |
| `POST /actas/{id}/acciones/confirmar-pago-condena` | Confirmar pago condena | `PagoCondenaSupport` | `situacionPagoCondenaPorActa` | Entidad pago condena real | Media | Alto | — |
| `POST /actas/{id}/acciones/observar-pago-condena` | Observar pago condena | `PagoCondenaSupport` | `situacionPagoCondenaPorActa` | Entidad pago condena real | Media | Bajo | — |
| `POST /actas/{id}/acciones/consentir-condena-y-registrar-pago` | Consentimiento de condena con pago | `PagoCondenaSupport` | `situacionPagoCondenaPorActa` | Entidad pago condena real | Media | Alto | — |

### PrototipoMaterialesController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `POST /actas/{id}/acciones/resolver-pendiente-bloqueante-cierre` | Resolucion de bloqueante | `CerrabilidadSupport` | `documentosPorActa` | Pendiente de diseno | Media | Alto | Bloqueantes documentales y materiales son distintos |
| `POST /actas/{id}/acciones/registrar-constatacion-material-temprana` | Constatacion material | `CerrabilidadSupport` | `actas` | Pendiente de diseno | Media | Medio | — |
| `POST /actas/{id}/acciones/registrar-medida-preventiva-posterior` | Medida preventiva posterior | `CerrabilidadSupport` | `actas` | Pendiente de diseno | Media | Alto | Genera condicion material bloqueante |
| `POST /actas/{id}/acciones/reconocer-origen-bloqueo-cierre-material` | Reconocimiento de origen | `CerrabilidadSupport` | `actas` | Pendiente de diseno | Baja | Bajo | — |
| `POST /actas/{id}/acciones/registrar-resolucion-bloqueo-cierre` | Resolucion de bloqueo cierre | `CerrabilidadSupport` | `actas` | Pendiente de diseno | Media | Medio | — |
| `POST /actas/{id}/acciones/registrar-cumplimiento-material-bloqueo-cierre` | Cumplimiento material | `CerrabilidadSupport` | `actas` | Pendiente de diseno | Media | Medio | — |

### PrototipoArchivoParalizacionController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `POST /actas/{id}/acciones/cerrar-acta` | Cierre de acta | `CierreSupport` | `actas`, `cerrabilidad` | Servicio de cierre real | Alta | Critico | Unica puerta de cierre |
| `POST /actas/{id}/acciones/archivar-acta` | Archivo de acta | `ArchivoReingresoSupport` | `actas` | Servicio de archivo real | Media | Medio | — |
| `POST /actas/{id}/acciones/archivar-por-vencimiento` | Archivo por vencimiento | `ArchivoReingresoSupport` | `actas` | Servicio de archivo real | Media | Medio | — |
| `POST /actas/{id}/acciones/reingresar-acta` | Reingreso desde archivo | `ArchivoReingresoSupport` | `actas`, `accionPendientePorActa` | Servicio de archivo real | Media | Medio | Marca `REVISION_POST_REINGRESO` |
| `POST /actas/{id}/acciones/reactivar-acta` | Reactivacion desde PARALIZADAS | `ParalizacionReactivacionSupport` | `actas`, `observacionParalizacionPorActa` | Servicio de paralizacion real | Media | Medio | — |
| `POST /actas/{id}/acciones/paralizar-acta` | Paralizacion administrativa | `ParalizacionReactivacionSupport` | `actas`, `observacionParalizacionPorActa` | Servicio de paralizacion real | Media | Medio | — |
| `POST /actas/{id}/acciones/enviar-a-notificacion` | Envio manual a notificacion | directo al store | `actas` | Pendiente de diseno | Baja | Bajo | — |
| `POST /actas/{id}/acciones/anular-acta` | Anulacion por nulidad | directo al store | `actas` | Pendiente de diseno | Media | Critico | Similar a cierre; no debe ocurrir automaticamente |

### PrototipoGestionExternaController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `POST /actas/{id}/acciones/derivar-a-apremio` | Derivacion a apremio | `GestionExternaSupport` | `accionPendientePorActa` | Servicio de gestion externa real | Baja | Alto | Depende de fallo notificado |
| `POST /actas/{id}/acciones/derivar-a-juzgado-de-paz` | Derivacion a juzgado | `GestionExternaSupport` | `accionPendientePorActa` | Servicio de gestion externa real | Baja | Alto | Pendiente de diseno |
| `POST /actas/{id}/acciones/reingresar-desde-gestion-externa` | Reingreso desde gestion | `GestionExternaSupport` | `actas` | Servicio de gestion externa real | Baja | Medio | — |
| Endpoints apremio/juzgado retorno | Variantes de retorno | `GestionExternaSupport` | `resultadoExternoPostGestionPorActa` | Servicio de gestion externa real | Baja | Alto | Pueden modificar monto condena |

### PrototipoNotificacionesExternasController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| Notificador municipal (varios) | Acuse y envio | `NotificadorMunicipalSupport` | `notificacionesPorActa` | Integracion con sistema externo | Baja | Alto | Pendiente de diseno |
| Correo postal (varios) | Lotes y trazabilidad | `CorreoPostalNotificacionSupport` | `notificacionesPorActa` | Integracion con sistema externo | Baja | Alto | Pendiente de diseno |

### PrototipoPortalInfractorController

| Endpoint | Operacion | Support actual | Dependencia mock | Reemplazo real esperado | Prioridad | Riesgo | Observaciones |
|---|---|---|---|---|---|---|---|
| `GET /infractor/actas/{codigoQr}` | Detalle para ciudadano | `PortalInfractorSupport` | `actas` | Lookup real por codigoQr | Baja | Medio | No exponer estados internos |
| Acciones del infractor (varios) | Pago, apelacion, visualizacion | `PortalInfractorSupport` | `notificacionesPorActa`, `situacionPagoPorActa` | Pendiente de diseno | Baja | Alto | — |

---

## 6. Datos reales minimos necesarios

Para comenzar la Etapa 1 se necesita al menos:

| Dato | Descripcion | Notas |
|---|---|---|
| Identificador de acta | `actaId` tecnico persistido | Clave unica, estable, no cambia |
| Numero visible | `numeroActa` legible para operadores y QA | Formato pendiente de definicion |
| Dependencia | Dependencia municipal o reparticion | Necesario para filtros y vistas |
| Tipo de acta | Tipo: transito, bromatologia, general, etc. | Pendiente de catalogo real |
| Infractor | Nombre y datos minimos del infractor | Al menos nombre completo |
| Estado juridico/procesal | Bloque actual, estado de proceso, situacion administrativa | Equivalente a `bloqueActual` + `estadoProcesoActual` + `situacionAdministrativaActual` del mock |
| Bandeja actual | A que bandeja pertenece el acta | Calculado desde estado o proyectado |
| Documentos | Lista de documentos asociados con tipo, estado y nombre | Necesario para Etapa 3 |
| Notificaciones | Lista de notificaciones con canal, estado y resultado | Necesario para Etapa 3 o Etapa 4 notificaciones |
| Pagos | Situacion de pago voluntario e informado; situacion de pago condena | Necesario para Etapa 4 pagos |
| Bloqueantes materiales | Existencia y tipo de bloqueantes activos | Necesario para Etapa 4 materiales |
| Eventos | Historial de eventos con tipo, fechaHora, bloques origen/destino | Necesario para Etapa 3 |
| Gestion externa | Tipo de gestion externa si el acta esta derivada | Solo si el acta tiene gestion activa |

---

## 7. Tests de aceptacion por etapa

### Etapa 1 — Lectura real de actas

**Backend tests:**
- `GET /actas/{id}` retorna `200` con response completo para acta existente.
- `GET /actas/{id}` retorna `404` para id inexistente.
- `GET /actas/buscar?q=...` retorna lista no vacia para terminos conocidos.
- `GET /actas/buscar?q=` retorna lista vacia.
- Todos los campos de `ActaDetalleResponse` tienen valor no nulo para actas reales validas.

**Validacion manual Angular:**
- La vista de detalle de acta carga sin errores de consola.
- Los campos visibles coinciden con los datos reales del acta.
- La busqueda global retorna resultados esperados.

**Casos QA representativos:**
- Acta en cada estado de bandeja representativo.
- Acta con infractor con caracteres especiales.
- Acta con numero de formato largo.

**Comparacion con matriz de reglas:**
- Verificar que el estado expuesto coincide con el estado real segun la especificacion.

---

### Etapa 2 — Proyeccion real de bandejas

**Backend tests:**
- `GET /bandejas` retorna todas las bandejas del orden fijo con conteos correctos.
- `GET /bandejas/{codigo}/actas` retorna solo actas de esa bandeja.
- Filtro por `accionPendiente` funciona correctamente.
- Filtro por `situacionPago` funciona correctamente.
- Filtro por `cerrable=true` retorna solo actas cerrables.
- Sub-bandejas tienen labels legibles.

**Validacion manual Angular:**
- Las bandejas muestran conteos correctos.
- Al hacer clic en una bandeja, la lista de actas corresponde.
- Los filtros de sub-bandeja funcionan.

**Casos QA representativos:**
- Bandeja con cero actas.
- Bandeja con actas en distintas sub-bandejas.
- Filtro por accionPendiente con valor inexistente retorna lista vacia (no error).

---

### Etapa 3 — Eventos y documentos reales

**Backend tests:**
- `GET /actas/{id}/eventos` retorna lista ordenada por `fechaHora`.
- `GET /actas/{id}/documentos` retorna documentos con tipos legibles.
- Los tipos de evento son consistentes con los valores esperados por Angular.
- Un acta sin eventos retorna lista vacia (no error).

**Validacion manual Angular:**
- El historial de eventos se muestra completo y ordenado.
- Los documentos tienen nombres y tipos legibles.

**Casos QA representativos:**
- Acta con muchos eventos (al menos 5).
- Acta sin documentos.
- Acta con documento en estado pendiente de firma.

---

### Etapa 4 — Acciones nucleo (por accion)

**Backend tests minimos por accion:**
- Accion exitosa: `200` con estado posterior correcto.
- Accion con precondicion no cumplida: `409 Conflict` o `400 Bad Request`.
- Accion sobre acta inexistente: `404 Not Found`.
- El evento correspondiente queda registrado en el historial.

**Validacion manual Angular:**
- La accion ejecutada desde la UI produce el cambio de estado esperado.
- El historial de eventos refleja la accion ejecutada.
- La bandeja del acta cambia si corresponde.

**Comparacion con matriz de reglas:**
- Verificar que cada accion respeta las precondiciones documentadas en `docs/MATRIZ_REGLAS_ACTA.md`.

---

## 8. Riesgos y mitigaciones

| Riesgo | Descripcion | Mitigacion |
|---|---|---|
| Romper contratos Angular | Un campo renombrado o eliminado en un response rompe la UI | No cambiar nombres de campos existentes; solo agregar campos nuevos si es necesario |
| Perder legibilidad QA | Reemplazar strings legibles por codigos internos | Los responses siguen exponiendo strings legibles; nunca exponer ids internos como unica informacion |
| Modelar por bandeja | Persistir `bandejaActual` como campo independiente sin invalidacion por evento | La bandeja se calcula desde el estado; si se persiste como cache, debe invalidarse en cada evento relevante |
| Cerrar automaticamente | Agregar logica que cierre el expediente sin invocacion explicita | El endpoint `cerrar-acta` es la unica puerta de cierre; no agregar cierre en hooks ni listeners |
| Perder eventos | Migrar a entidad real sin preservar historial de eventos del mock | Los eventos son append-only; la entidad real debe respetar ese principio desde el inicio |
| Mezclar circuitos de pago | Confundir `situacionPago` (voluntario/informado) con `situacionPagoCondena` | Son mapas y DTOs separados; al integrar, usar entidades distintas para cada circuito |
| Colapsar bloqueantes | Unificar bloqueantes documentales y materiales en una sola entidad | Son categorias distintas con reglas distintas; mantener separacion en la entidad real |
| Exponer estados internos en portal | Incluir `bandejaActual`, `accionPendiente` en respuestas del portal ciudadano | El portal solo expone datos del infractor; las respuestas de `/infractor/` no incluyen estados operativos internos |
| Retirar mocks demasiado pronto | Eliminar `MockDataFactory` o `/reset` antes de que la integracion real este estable | Ver criterio de retiro en la seccion 9; no retirar hasta que cada etapa este validada |
| Inconsistencia entre prototipo y dominio real | Un campo que en mock siempre tiene valor puede estar nulo en datos reales | Validar campos criticos con datos reales antes de desplegar cada etapa |

---

## 9. Criterio para retirar infraestructura demo

### `MockDataFactory`

Se puede retirar cuando:
- Las Etapas 1, 2 y 3 esten integradas y validadas.
- Al menos las acciones nucleo de la Etapa 4 con mayor uso esten integradas.
- El equipo de QA confirma que no necesita el dataset mock para pruebas.
- Se dispone de un mecanismo alternativo para poblar datos de prueba en
  entorno de QA (fixture, seed o script SQL real).

### `PrototipoStore`

Se puede retirar cuando:
- Todos sus mapas in-memory tengan equivalente en persistencia real.
- Todos sus supports tengan equivalente en servicios/repositorios reales.
- Los controllers esten conectados a los servicios reales.
- No quedan tests ni integraciones que dependan de `PrototipoStore`.
- Se ejecuto al menos un ciclo completo de QA sobre datos reales sin mock activo.

### `POST /reset`

Se puede retirar cuando:
- `MockDataFactory` fue retirado.
- El dataset demo ya no se usa en ningun flujo de prueba.

### `POST /actas/mock`

Se puede retirar cuando:
- Existe un endpoint real de alta de acta en el backend productivo.
- `contadorActaLabradoMockDemo` ya no se usa.

### Contadores demo (`contadorActaLabradoMockDemo`)

Se puede retirar junto con `MockDataFactory` y el endpoint `/actas/mock`.
Son infraestructura exclusiva del flujo de demo.

### Mapas mock de estado parallel (`accionPendientePorActa`,
`situacionPagoPorActa`, etc.)

Se pueden retirar cuando el campo equivalente vive en la entidad real
y los supports correspondientes ya no dependen del mapa in-memory.
No retirar por area hasta que todos los endpoints que dependen de ese mapa
esten integrados con la fuente real.

---

## Notas y pendientes de diseno

- El mecanismo de resolucion de `codigoQr` a `actaId` no esta definido.
  Pendiente de diseno en la integracion del portal infractor.
- El catalogo de tipos de acta (transito, bromatologia, general) no esta
  definido como entidad real. Pendiente de diseno antes de Etapa 1.
- La estructura de la entidad real de notificacion (canal, estado, resultado)
  no esta definida. Pendiente de diseno antes de Etapa 4-notificaciones.
- El servicio de firma documental (real) no esta definido. Pendiente de diseno
  antes de Etapa 4-firma.
- El criterio de invalidacion de cache de bandeja no esta definido.
  Pendiente de decision antes de Etapa 2.
- Los comprobantes de pago externo (EM, RC, Cmte/Pref/Nro) no estan modelados
  en el prototipo. Seran parte del diseno del circuito de pago real.
