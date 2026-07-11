# Glosario canonico del dominio de Faltas

## Introduccion

Este glosario define los terminos canonicos del dominio de Faltas.
Las siguientes reglas son de aplicacion obligatoria:

- Un termino canonico tiene un unico significado; no admite sinonimos que
  pertenezcan a dimensiones diferentes.
- No deben usarse como equivalentes terminos que nombran realidades distintas
  (estado, hito, evento, proyeccion, bandeja, accion).
- Los nombres Java pueden citarse como mapeo tecnico; la definicion debe
  entenderse sin necesidad de leer el codigo fuente.
- Ante una contradiccion entre la spec y el codigo, se corrige primero la spec.

[GLO-TERM-001] Cada termino a continuacion es normativo en su dimension.

---

## Terminos canonicos

### Acta

**Dimension:** Agregado raiz.
**Naturaleza:** Persistida.
**Propietario funcional:** Sistema de Faltas.

Unidad principal del dominio. Un acta representa el expediente de la infraccion
desde su labrado hasta su cierre definitivo. Contiene subagregados propios (fallo,
documentos, notificaciones, pagos, apelaciones) y proyecciones operativas
(snapshot).

No confundir con: expediente como sinonimo libre, ni con un documento en
particular.

---

### Expediente

**Dimension:** Sinonimo operativo de Acta.
**Naturaleza:** Persistida.

Termino de uso coloquial equivalente a Acta. En la spec canonica, Acta es el
termino preciso.

---

### Agregado

**Dimension:** Diseno de dominio.
**Naturaleza:** Conceptual.

Unidad de consistencia transaccional. En este dominio, el Acta es el agregado
raiz. Un agregado encapsula invariantes y garantiza que sus transiciones sean
validas.

---

### Subagregado

**Dimension:** Diseno de dominio.
**Naturaleza:** Conceptual.

Entidad con lifecycle propio que vive dentro del Acta. Ejemplos: fallo
(`FalActaFallo`), documento (`FalDocumento`), notificacion (`FalNotificacion`),
pago, apelacion. Cada subagregado tiene su propio conjunto de estados y
transiciones.

No confundir con: el estado del Acta como un todo.

---

### Invariante

**Dimension:** Regla de dominio.
**Naturaleza:** Conceptual.

Condicion que debe ser verdadera en todo momento del ciclo de vida. Las
operaciones de dominio verifican invariantes antes de aplicar cambios. Una
transicion que viola un invariante debe ser rechazada.

---

### Precondicion

**Dimension:** Regla de dominio.
**Naturaleza:** Conceptual.

Condicion que debe ser satisfecha antes de ejecutar un comando. Distinta del
invariante: la precondicion puede no aplicarse en todos los estados.

Ejemplo: `marcarPendienteNotificacion` requiere `estadoFallo = PENDIENTE_FIRMA`.

---

### Comando

**Dimension:** Intencion ejecutable.
**Naturaleza:** Efimera (no persistida como tal).

Solicitud de ejecucion de una operacion sobre el agregado. Los comandos tienen
forma de verbo imperativo (`registrarFirma`, `marcarPendienteNotificacion`,
`declararFirmeza`). Un comando puede ser rechazado si no se cumplen sus
precondiciones.

No confundir con: accion pendiente (que es una recomendacion derivada).

---

### Evento

**Dimension:** Registro historico inmutable.
**Naturaleza:** Persistida (append-only).
**Propietario funcional:** `ActaEvento`.

Hecho ocurrido en el circuito del acta, registrado en forma inmutable. Los
eventos son de tipo `TipoEventoActa` (codigo de 6 caracteres). Ejemplos:
`DOCFIR`, `NOTENV`, `FALCON`.

No confundir con: estado (que es la situacion vigente, no el hecho ocurrido).

---

### Hito

**Dimension:** Dato historico temporal.
**Naturaleza:** Persistida como campo de fecha/hora.

Fecha o momento que prueba que ocurrio una frontera del circuito. Ejemplos:
`fhDictado`, `fhFirma`, `fhNotificacion`, `fhFirmeza`. Un hito no determina el
estado; el estado canonico es el que persiste en la dimension correspondiente.

No confundir con: estado del fallo (que es `estadoFallo`, no `fhFirma`).

---

### Estado actual

**Dimension:** Situacion vigente persistida.
**Naturaleza:** Persistida.

Valor de una dimension de estado en el momento presente. Un acta tiene multiples
estados actuales simultaneos: uno por dimension (`bloqueActual`, `estadoProcesal`,
`situacionAdministrativa`, `resultadoFinal`). Un subagregado tiene su propio
estado actual. Un valor de estado no ejecuta ni dispara por si mismo una transicion.

---

### Lifecycle

**Dimension:** Diseno de dominio.
**Naturaleza:** Conceptual.

Secuencia ordenada de estados por los que puede transitar un agregado o
subagregado. Un lifecycle define que transiciones son validas, cuales son estados
terminales y cuales son estados laterales.

---

### Transicion

**Dimension:** Diseno de dominio.
**Naturaleza:** Conceptual.

Cambio de un estado a otro dentro de un lifecycle. Toda transicion requiere que
se cumpla su precondicion y produce efectos definidos.

---

### Bloque actual

**Dimension:** Etapa macro persistida del acta.
**Naturaleza:** Persistida.
**Propietario funcional:** `BloqueActual` (enum).

Representa la etapa principal del circuito en la que se encuentra el acta. Es
una dimension del acta, no del subagregado. Valores: `CAPT`, `ENRI`, `NOTI`,
`ANAL`, `GEXT`, `ARCH`, `CERR`.

No confundir con: bandeja (que es derivada y operativa).

---

### Estado procesal

**Dimension:** Estado juridico-procesal del acta.
**Naturaleza:** Persistida.
**Propietario funcional:** `EstadoProcesalActa` (enum).

Situacion del tramite desde el punto de vista procesal. Valores: `EN_TRAMITE`
(`TRAM`), `CONCLUIDO` (`CONC`), `PRESCRIPTO` (`PRSC`).

---

### Situacion administrativa

**Dimension:** Estado operativo/administrativo del acta.
**Naturaleza:** Persistida.
**Propietario funcional:** `SituacionAdministrativaActa` (enum).

Condicion transversal al bloque procesal. Permite identificar actas paralizadas,
archivadas, en gestion externa, cerradas o anuladas. Valores: `ACTIVA` (`ACTV`),
`PARALIZADA` (`PARA`), `EN_GESTION_EXTERNA` (`GEXT`), `ARCHIVADA` (`ARCH`),
`CERRADA` (`CERR`), `ANULADA` (`ANUL`).

No confundir con: resultado final (que es el desenlace sustantivo del
expediente).

---

### Resultado final

**Dimension:** Resultado sustantivo del expediente.
**Naturaleza:** Persistida.
**Propietario funcional:** `ResultadoFinalActa` (enum).

Desenlace del circuito. Indica que ocurrio en terminos de resolucion del
expediente. Valores: `SIN_RESULTADO_FINAL` (0), `PAGO_VOLUNTARIO_PAGADO` (1),
`ABSUELTO` (2), `CONDENA_FIRME` (3), `CONDENA_FIRME_PAGADA` (4),
`FALLO_CONDENATORIO_PAGADO` (5), `FALLO_CONDENATORIO_GESTION_EXTERNA` (6),
`PRESCRIPTO` (7), `ANULADO` (8), `NULIDAD` (9).

No confundir con: situacion administrativa (que es el estado operativo
transversal, no el desenlace sustantivo).

---

### Estado de subagregado

**Dimension:** Estado propio de un subagregado.
**Naturaleza:** Persistida en el subagregado.

Estado especifico de un fallo, documento, notificacion, pago o apelacion. Cada
subagregado tiene su propio lifecycle independiente del bloque y del estado
procesal del acta.

---

### Snapshot

**Dimension:** Proyeccion operativa.
**Naturaleza:** Derivada, regenerable.
**Propietario funcional:** `FalActaSnapshot`.

Vista calculada del estado consolidado del acta para uso operativo: bandeja,
subbandeja y accion pendiente. El snapshot se regenera desde los datos
persistidos mediante `SnapshotRecalculador`. No es fuente normativa ni define
el estado del dominio.

No confundir con: el agregado Acta (que es la fuente persistida de verdad).

---

### Proyeccion

**Dimension:** Vista derivada.
**Naturaleza:** Derivada.

Calculo a partir de datos persistidos. Las bandejas, subbandejas y acciones son
proyecciones. Si la fuente persistida cambia, la proyeccion debe regenerarse.

---

### Bandeja

**Dimension:** Agrupacion operativa derivada.
**Naturaleza:** Derivada (calculada en el snapshot).
**Propietario funcional:** `CodigoBandeja` (enum).

Vista que agrupa actas segun su estado operativo para facilitar el trabajo de
los operadores. Es una clasificacion de snapshot, no una fuente de verdad del
estado del dominio. Ejemplos: `PENDIENTE_FIRMA`, `PENDIENTE_NOTIFICACION`,
`EN_NOTIFICACION`.

No confundir con: bloque actual (que es la etapa macro persistida), ni con
estado de dominio.

---

### Subbandeja

**Dimension:** Clasificacion visual/operativa secundaria.
**Naturaleza:** Derivada.

Refinamiento dentro de una bandeja para organizacion visual u operativa. Es
secundaria respecto a la bandeja principal.

---

### Accion pendiente

**Dimension:** Recomendacion operativa derivada.
**Naturaleza:** Derivada (calculada en el snapshot).
**Propietario funcional:** `AccionPendiente` (enum).

Indica la proxima actuacion que el operador deberia realizar. Es una sugerencia
derivada del estado persistido; no equivale a un comando ni lo reemplaza.

Valores: `COMPLETAR_CAPTURA`, `ENRIQUECER`, `GENERAR_DOCUMENTO`,
`FIRMAR_DOCUMENTO`, `ENVIAR_NOTIFICACION`, `EVALUAR_NOTIFICACION`,
`DECIDIR_REINTENTO_O_GESTION`, `DICTAR_FALLO`, `RESOLVER_APELACION`,
`REGISTRAR_PAGO`, `DERIVAR_GESTION_EXTERNA`, `CONFIRMAR_PAGO`, `CORREGIR_PAGO`,
`DECLARAR_CONDENA_FIRME`, `GESTIONAR_PAGO_CONDENA`, `CONFIRMAR_PAGO_CONDENA`,
`CORREGIR_PAGO_CONDENA`, `NINGUNA`.

No confundir con: comando (que es la intencion ejecutable real).

---

### Fallo

**Dimension:** Subagregado.
**Naturaleza:** Persistida.
**Propietario funcional:** `FalActaFallo`.

Resolucion dictada sobre el acta. Un acta puede tener historial de fallos; solo
uno es vigente (`siVigente = true`). Tiene lifecycle propio expresado en
`EstadoFalloActa`. Puede ser absolutorio (`ABSOLUTORIO`) o condenatorio
(`CONDENATORIO`).

---

### Documento

**Dimension:** Subagregado.
**Naturaleza:** Persistida.
**Propietario funcional:** `FalDocumento`.

Pieza documental del expediente generada para el circuito. Tiene lifecycle
propio expresado en `EstadoDocu`. El estado documental y el estado del fallo
son dimensiones independientes.

---

### Requisito de firma

**Dimension:** Subagregado del documento.
**Naturaleza:** Persistida.
**Propietario funcional:** `FalDocumentoFirmaReq`.

Obligacion de firma registrada sobre un documento. Un documento puede tener
multiples requisitos de firma. El cumplimiento de todos los requisitos activos
desencadena el avance del lifecycle del documento y, si corresponde, del fallo.

No confundir con: firma (que es la evidencia de cumplimiento del requisito).

---

### Firma

**Dimension:** Evidencia de cumplimiento.
**Naturaleza:** Persistida.
**Propietario funcional:** `FalDocumentoFirma`.

Registro de que un requisito de firma fue satisfecho. Incluye: firmante, tipo
de firma, referencia externa de idempotencia (`referenciaFirmaExt`), hash y
clave de storage.

No confundir con: requisito de firma (que es la obligacion previa).

---

### Documento firmado

**Dimension:** Estado documental.
**Naturaleza:** Valor del enum `EstadoDocu`.

`FalDocumento.estadoDocu = FIRMADO` indica que el documento completo sus firmas
obligatorias. Es un estado del documento, no del fallo.

No confundir con: fallo firmado (concepto coloquial) ni con el estado
`PENDIENTE_NOTIFICACION` del fallo.

---

### Fallo firmado

**Dimension:** Hito del fallo.
**Naturaleza:** Hecho coloquial; no es un estado del enum `EstadoFalloActa`.

Expresion coloquial que designa el momento en que `fhFirma` quedo registrado en
el fallo. El estado canonico del fallo en ese momento es
`PENDIENTE_NOTIFICACION`, no un hipotetico `FIRMADO`.

No usar `FIRMADO` como valor del enum `EstadoFalloActa`.

La creacion de `FalNotificacion.PENDIENTE_ENVIO` depende de que la plantilla sea notificable (`siNotificable = true`).

---

### Notificacion

**Dimension:** Subagregado.
**Naturaleza:** Persistida.
**Propietario funcional:** `FalNotificacion`.

Cabecera del proceso notificatorio para un documento. Tiene lifecycle propio
expresado en `EstadoNotificacion`. Una notificacion no es lo mismo que un
intento de notificacion.

Por documento puede existir como maximo una cabecera notificatoria activa. Los
reintentos y canales se representan mediante intentos, no mediante nuevas
cabeceras.

---

### Notificacion pendiente

**Dimension:** Estado del subagregado notificacion.
**Naturaleza:** Persistida.

`FalNotificacion.estado = PENDIENTE_ENVIO` indica trabajo notificatorio concreto
a realizar. Es trabajo persistido, no una vista operativa.

No es consecuencia obligatoria de toda transicion a `PENDIENTE_NOTIFICACION`: se
crea solo cuando la plantilla del documento es notificable (`siNotificable = true`).

No confundir con: bandeja pendiente de notificacion
(`CodigoBandeja.PENDIENTE_NOTIFICACION`), que es una proyeccion operativa
derivada.

---

### Intento de notificacion

**Dimension:** Ejecucion por canal.
**Naturaleza:** Persistida.

Cada ejecucion concreta del proceso de notificacion. Una notificacion puede
tener multiples intentos.

Un nuevo intento no crea una nueva `FalNotificacion`.

---

### Resultado notificatorio

**Dimension:** Desenlace del proceso notificatorio.
**Naturaleza:** Persistida en `FalNotificacion.resultado`.

Valor que indica si la notificacion tuvo resultado positivo, negativo o si
vencio sin acuse. El registro valido de un resultado positivo ejecuta la transicion
`PENDIENTE_NOTIFICACION -> NOTIFICADO` del fallo asociado.

---

### Portal

**Dimension:** Canal de notificacion.
**Naturaleza:** Conceptual.

Canal de notificacion que permite al infractor acceder al acta. Una notificacion
positiva por portal (`PORPOS`) puede avanzar el estado del fallo.

---

### Lote de correo

**Dimension:** Agrupacion de envios.
**Naturaleza:** Persistida.
**Propietario funcional:** `FalLoteCorreo`.

Agrupacion de notificaciones para envio postal masivo. Se genera desde
notificaciones en estado `PENDIENTE_ENVIO`.

---

### Actor

**Dimension:** Identidad ejecutora.
**Naturaleza:** Contextual y autenticada.

Identidad del usuario, funcionario o sistema tecnico que ejecuta una operacion.
El actor se obtiene del contexto autenticado correspondiente al canal de
entrada. En el callback de firma proviene exclusivamente del `sub` del JWT y
nunca del body.

---

### Reloj canonico

**Dimension:** Fuente de tiempo del dominio.
**Naturaleza:** Infraestructura.
**Propietario funcional:** `FaltasClock`.

Fuente de tiempo utilizada por todas las operaciones sensibles. Garantiza
testabilidad y coherencia temporal.

---

### Idempotencia

**Dimension:** Propiedad de operacion.
**Naturaleza:** Contractual.

Propiedad de una operacion que permite ejecutarla mas de una vez con el mismo
resultado. Las firmas se identifican por `referenciaFirmaExt`; un reintento
identico devuelve los datos existentes sin crear registros nuevos.

---

### Concurrencia optimista

**Dimension:** Estrategia de persistencia.
**Naturaleza:** Infraestructura.

Mecanismo que detecta escrituras concurrentes sobre una version desactualizada.
La operacion debe rechazar el conflicto o resolverlo mediante una regla
idempotente explicita; nunca debe sobrescribir silenciosamente.

---

### Alias de compatibilidad

**Dimension:** Deuda tecnica.
**Naturaleza:** Codigo marcado como `@Deprecated`.

Nombre alternativo de un campo o metodo preservado temporalmente por
compatibilidad. No es un estado canonico. No debe usarse en codigo nuevo ni en
documentacion normativa.

Ejemplo: `getFechaDictado()` es alias de `getFhDictado()`.

---

### Dato historico

**Dimension:** Dato del pasado.
**Naturaleza:** Persistida como campo de fecha/hora o evento append-only.

Campo o registro que documenta que algo ocurrio. No se modifica. No debe usarse
para inferir el estado canonico vigente.

---

### Fuente normativa

**Dimension:** Autoridad de la spec.
**Naturaleza:** Documental.

Documento cuyo contenido define reglas de dominio vigentes. En este proyecto,
la fuente normativa es `backend/api-faltas-core/docs/spec-as-source/`. No son
fuentes normativas: el codigo Java, los tests, los handoffs ni la historia Git.

---

### Evidencia de conformidad

**Dimension:** Auditoria.
**Naturaleza:** Documental o ejecutable.

Material que demuestra que el sistema se comporta segun la spec. Los tests, el
codigo Java y los eventos registrados son evidencia de conformidad. No pueden
redefinir silenciosamente la spec.

---

## Terminos que no son sinonimos

[GLO-DIST-001] Las siguientes distinciones son obligatorias.

| Termino A | Termino B | Distincion |
|---|---|---|
| fallo dictado (`fhDictado != null`) | `PENDIENTE_FIRMA` | hecho ocurrido vs. estado actual del fallo |
| documento firmado (`estadoDocu = FIRMADO`) | `PENDIENTE_NOTIFICACION` | estado documental vs. estado actual del fallo |
| notificacion pendiente (`estadoNotificacion = PENDIENTE_ENVIO`) | bandeja pendiente de notificacion (`CodigoBandeja.PENDIENTE_NOTIFICACION`) | trabajo persistente vs. proyeccion operativa |
| evento (`TipoEventoActa`) | estado (`BloqueActual`, `EstadoProcesalActa`, etc.) | hecho inmutable registrado vs. situacion vigente |
| bloque (`BloqueActual`) | bandeja (`CodigoBandeja`) | etapa macro persistida vs. agrupacion operativa derivada |
| resultado final (`ResultadoFinalActa`) | situacion administrativa (`SituacionAdministrativaActa`) | resultado sustantivo del expediente vs. condicion operativa transversal |
| snapshot (`FalActaSnapshot`) | agregado (Acta) | proyeccion regenerable vs. fuente persistida de verdad |
| accion pendiente (`AccionPendiente`) | comando | recomendacion derivada vs. intencion ejecutable |
| firma (`FalDocumentoFirma`) | requisito de firma (`FalDocumentoFirmaReq`) | evidencia realizada vs. obligacion previa |
| notificacion (`FalNotificacion`) | intento de notificacion | cabecera del proceso notificatorio vs. ejecucion por canal |

---

## Convencion de nombres

[GLO-NAME-001] Las siguientes convenciones son obligatorias para la spec y el
codigo:

- **Estados** se expresan en presente o como situacion vigente: `PENDIENTE_FIRMA`,
  `EN_TRAMITE`, `ACTIVA`.
- **Eventos** se nombran como hechos ocurridos, en forma compacta: `DOCFIR`,
  `NOTENV`, `FALCON`.
- **Comandos** se nombran como verbos imperativos: `registrarFirma`,
  `marcarPendienteNotificacion`, `declararFirmeza`.
- **Hitos** se almacenan como campo con prefijo `fh`: `fhDictado`, `fhFirma`,
  `fhNotificacion`, `fhFirmeza`.
- **Bandejas** se nombran como clasificacion operativa: `PENDIENTE_FIRMA`,
  `EN_NOTIFICACION`.
- **No usar participios historicos como estado** cuando existe una situacion
  posterior mas precisa. `DICTADO` y `FIRMADO` no son estados del fallo;
  `PENDIENTE_FIRMA` y `PENDIENTE_NOTIFICACION` son los estados canonicos
  correspondientes.
