# Estandar canonico de contratos de comandos

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Ante contradiccion con un contrato de comando especifico, ese contrato es normativo en lo
> particular; este estandar es normativo en lo estructural y definitorio (ver README).

Este documento define el formato normativo de todo contrato de comando en el sistema de Faltas.
Es autosuficiente y no hace referencia a slices, fechas ni estado temporal de implementacion.

Todo contrato de aplicacion debe seguir este estandar. Ante contradiccion entre
un contrato especifico y este documento, el contrato especifico es normativo
en lo particular; este estandar es normativo en lo estructural y definitorio.

---

## CMD-STD-001 Definicion de terminos

### Comando

Intencion autenticada de modificar el dominio.

No es:
- una consulta;
- un evento;
- una accion pendiente;
- una llamada directa a repositorio;
- una transicion automatica causada por un valor de estado.

Los comandos se nombran como verbos imperativos. Pueden ser rechazados si no se cumplen sus precondiciones.

### Precondicion

Condicion de dominio que debe cumplirse antes de la primera mutacion.

Su incumplimiento:
- rechaza el comando;
- no produce efectos parciales;
- no crea eventos;
- no recalcula snapshot;
- no consume numeracion ni identificadores persistentes, salvo que el contrato especifico documente otra cosa.

### Validacion de entrada

Verificacion sintatica o estructural previa al acceso al dominio:
- campos requeridos;
- formatos;
- rangos;
- autenticacion;
- autorizacion;
- integridad del request.

Una validacion de entrada rechaza antes de acceder al dominio.
Una precondicion requiere haber cargado el agregado.
No son equivalentes ni intercambiables.

### Efecto

Cambio autorizado sobre agregados, subagregados o proyecciones persistidas.

### Evento

Registro inmutable de un hecho ya ocurrido.

No ejecuta la transicion; la registra.

### Resultado

Contrato devuelto por el comando al emisor:
- entidad afectada;
- estado final;
- flags de idempotencia;
- payload HTTP cuando corresponda.

---

## CMD-ORDER-001 Plantilla normativa por comando

Todo contrato de comando debe documentar exactamente los siguientes campos en
el orden indicado:

```text
ID normativo
Nombre canonico
Proposito
Canales de entrada
Actor y autorizacion
Entrada
Clave de idempotencia
Agregado propietario
Entidades leidas
Orden de validaciones
Precondiciones
Instante canonico
Orden de efectos
Persistencia
Eventos
Snapshot/proyecciones
Resultado
Errores
Semantica de reintento
Concurrencia
Efectos prohibidos
Tests de conformidad
```

Reglas:
- el orden es normativo;
- no se puede agrupar validaciones si el agrupamiento cambia la posibilidad de
  efectos parciales;
- toda llamada al reloj debe estar documentada;
- toda creacion de identificador persistente debe estar documentada;
- toda operacion idempotente debe indicar su clave y el resultado de un retry;
- toda operacion no idempotente debe describir que ocurre ante reintento.

---

## CMD-ORDER-002 Orden canonico de ejecucion

La secuencia de ejecucion de todo comando es:

```text
1. validar autenticacion y autorizacion
2. validar estructura de entrada
3. cargar entidades requeridas
4. validar precondiciones de dominio
5. capturar el instante canonico
6. aplicar mutaciones
7. persistir agregados y subagregados
8. registrar eventos
9. recalcular y persistir snapshot/proyecciones
10. construir resultado
```

Reglas:
- un comando especifico puede omitir pasos que no aplican;
- no puede alterar el orden sin declararlo explicitamente en su contrato;
- el instante se captura despues de todas las validaciones y antes de la primera
  mutacion;
- un comando exitoso usa el mismo instante en todos los hitos y eventos que
  representen la misma frontera de negocio;
- el reloj de dominio es `FaltasClock`; no se usa `LocalDateTime.now()`,
  `LocalDate.now()` ni `Instant.now()` directamente en codigo productivo;
- cuando una precondicion depende del tiempo, el comando completa primero las
  validaciones no temporales, captura una unica vez el instante canonico, usa ese
  mismo instante para las precondiciones temporales y para todos los efectos, hitos
  y eventos de la misma frontera, y no vuelve a consultar el reloj durante el
  comando; ejemplos: vigencia de una version de firmante, vencimiento del plazo de
  apelacion.

---

## ERR-STD-001 Taxonomia de errores

La siguiente tabla describe las clases de error transversales y las excepciones
especificas utilizadas por los contratos canonicos de esta familia.

Los valores de las columnas "HTTP canonico" y "Codigo canonico" forman parte del
contrato del adaptador HTTP. Las clases `AuthenticationException` y
`AccessDeniedException` no son interceptadas por el advice; son gestionadas
directamente por Spring Security antes de llegar al dominio.

| Clase de error | Significado | Momento | Mutacion permitida | HTTP canonico | Codigo canonico | Reintentable |
|---|---|---|---|---|---|---|
| `ActaNoEncontradaException` | Acta inexistente | Carga de entidad | Ninguna | 404 | `ACTA_NO_ENCONTRADA` | No |
| `DocumentoNoEncontradoException` | Documento inexistente | Carga de entidad | Ninguna | 404 | `DOCUMENTO_NO_ENCONTRADO` | No |
| `NotificacionNoEncontradaException` | Notificacion inexistente | Carga de entidad | Ninguna | 404 | `NOTIFICACION_NO_ENCONTRADA` | No |
| `LoteCorreoNoEncontradoException` | Lote de correo inexistente | Carga de entidad | Ninguna | 404 | `RECURSO_NO_ENCONTRADO` | No |
| `DocumentoPlantillaNoEncontradaException` | Plantilla de documento inexistente | Carga de entidad | Ninguna | 404 | `PLANTILLA_NO_ENCONTRADA` | No |
| `FirmanteNoEncontradoException` | Firmante inexistente | Carga de entidad | Ninguna | 404 | `INSPECTOR_NO_ENCONTRADO` | No |
| `NotificacionIntentoNoEncontradoException` | Intento de notificacion inexistente | Carga de entidad | Ninguna | 404 | `NOTIFICACION_NO_ENCONTRADA` | No |
| `PrecondicionVioladaException` | Precondicion de dominio o validacion de datos incumplida | Validacion de precondicion | Ninguna | 422 | `PRECONDICION_VIOLADA` | No sin correccion |
| `ConcurrenciaConflictoException` | Conflicto de version OCC | Persistencia | Ninguna | 409 | `CONFLICTO_CONCURRENCIA` | Si, tras recargar el agregado |
| `LoteCodigoDuplicadoException` | Codigo de lote ya existe | Creacion de lote | Ninguna | 409 | `CONFLICTO_DUPLICADO` | No sin cambiar la clave |
| `MethodArgumentNotValidException` | Campo requerido ausente o formato invalido | Antes del dominio | Ninguna | 400 | `VALIDACION_REQUEST` | No sin correccion |
| `ConstraintViolationException` | Violacion de Bean Validation | Antes del dominio | Ninguna | 400 | `VALIDACION_REQUEST` | No sin correccion |
| `HttpMessageNotReadableException` | Cuerpo JSON no parseable | Antes del dominio | Ninguna | 400 | `JSON_INVALIDO` | No sin correccion |
| `AuthenticationException` (Spring Security) | JWT ausente, invalido, vencido o con algoritmo no permitido | Antes del dominio | Ninguna | 401 | gobernado por el adaptador de seguridad | No sin token valido |
| `AccessDeniedException` (Spring Security) | Permiso insuficiente | Antes del dominio | Ninguna | 403 | gobernado por el adaptador de seguridad | No sin permisos |
| `Exception` (fallback generico) | Error interno no esperado | Cualquier momento | Indeterminada | 500 | `ERROR_INTERNO` | Posiblemente |

El codigo `INSPECTOR_NO_ENCONTRADO` es el mapeo vigente del adaptador HTTP para
`FirmanteNoEncontradoException`; su eventual revision exige una decision
explicita y no se corrige silenciosamente desde este documento.

Reglas de aplicacion:
- un error de precondicion no puede traducirse a exito silencioso;
- una excepcion interna no documentada en esta tabla no forma parte del
  contrato publico;
- cada contrato usa la excepcion especifica real de la entidad.

`IllegalArgumentException` representa una violacion interna de programacion o
de construccion de objetos y no es un error publico de dominio. Los adaptadores
de entrada deben rechazar datos invalidos mediante Bean Validation o
excepciones controladas antes de llegar a una operacion que pueda producir
`IllegalArgumentException`.

---

## IDEMP-STD-001 Idempotencia

Se distinguen cuatro categorias. La clasificacion de cada comando debe derivarse
del codigo y los tests vigentes; no se infiere por analogia.

### Idempotencia por referencia externa

Una clave externa unica identifica el mismo hecho.

Retry:
- devuelve el mismo resultado logico;
- no duplica entidad;
- no duplica evento;
- no duplica efectos.

El contrato debe declarar la clave de idempotencia y describir el resultado del
retry, incluyendo el status HTTP si aplica.

### Idempotencia por identidad natural

La combinacion de datos de dominio identifica un unico resultado permitido.
Un segundo intento con los mismos datos produce el mismo estado final, sin
duplicados.

### Rechazo seguro de repeticion

El segundo intento no repite efectos, pero devuelve precondicion violada o
conflicto porque el estado ya avanzo.

No se clasifica como idempotencia plena. El cliente puede distinguir el rechazo
del exito pero no debe interpretarlo como fallo de la operacion original.

### Comando no idempotente

Cada ejecucion valida crea un nuevo hecho.

El contrato debe documentar:
- como evitar duplicados accidentales;
- que clave de negocio diferencia las ejecuciones;
- que ocurre ante timeout del cliente.

---

## CONC-STD-001 Concurrencia y atomicidad

Reglas obligatorias:

- Cuando el dominio exige OCC, el campo de version del agregado actua como
  guardian de escrituras concurrentes. Un intento sobre version desactualizada
  lanza `ConcurrenciaConflictoException` (HTTP 409).
- La unicidad de claves idempotentes debe ser garantizada por el repositorio.
- La validacion completa ocurre antes de cualquier mutacion.
- No se sobrescribe silenciosamente un valor existente.
- La implementacion InMemory preserva invariantes observables dentro de una
  sola instancia JVM. No ofrece rollback transaccional conjunto entre multiples
  repositorios; si una mutacion parcial ocurre antes de una falla, el estado
  puede quedar inconsistente.
- MariaDB debe ejecutar los efectos de un comando dentro de una unica
  transaccion cuando el contrato exige atomicidad.
- Un evento no puede quedar persistido si la transicion que representa no
  quedo persistida en la misma transaccion.
- No se afirma que InMemory ofrece rollback transaccional conjunto cuando
  no lo ofrece.
- La implementacion debe garantizar la semantica concurrente declarada mediante
  unicidad, OCC, transaccion o exclusion mutua. Los tests secuenciales no
  prueban por si solos la seguridad concurrente.
- Una matriz de idempotencia no puede prometer el resultado de un retry
  concurrente si el comando no posee unicidad atomica, OCC, transaccion o
  exclusion mutua verificable.
