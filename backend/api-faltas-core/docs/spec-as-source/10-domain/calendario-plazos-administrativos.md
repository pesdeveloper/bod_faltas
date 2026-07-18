# Calendario administrativo local y calculo de plazos

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Es la fuente unica de las reglas de dias computables y del algoritmo de calculo
> de vencimientos administrativos (ver README).

## 1. Proposito

Este documento gobierna el modelo local de dias no computables, las reglas fijas del calendario administrativo y el calculo reutilizable de vencimientos de plazos administrativos (apelacion, y otros futuros).

Es la unica fuente normativa para:
- que dias computan y cuales no;
- como se registran excepciones locales;
- como se calcula y persiste un vencimiento.

---

## 2. Vocabulario

| Termino | Significado |
|---|---|
| Dia computable | Dia que cuenta para el transcurso del plazo administrativo. |
| Dia no computable | Dia que no avanza el conteo: domingo, 1-ene, 1-may, o excepcion local activa. |
| Excepcion local | Registro activo en el repositorio local que marca una fecha como no computable. |
| Fecha de origen | Fecha desde la cual empieza a computarse el plazo (no se cuenta). |
| Vencimiento | Ultimo dia computable en que todavia puede ejercerse el derecho (e.g. apelar). |
| Plazo vencido | La fecha actual ya supero el vencimiento: fechaActual.isAfter(fhVtoApelacion). |

---

## 3. Reglas de dias computables

### 3.1 Reglas fijas (no se persisten)

Las siguientes reglas son invariantes del dominio y no se almacenan en el repositorio local:

1. **Domingo**: nunca computable.
2. **1 de enero**: nunca computable.
3. **1 de mayo**: nunca computable.

El sabado ES computable, salvo que exista una excepcion local activa para esa fecha.

### 3.2 Excepciones locales activas

Cualquier fecha registrada en el repositorio local con siActivo = true es no computable.

Las excepciones se crean via CalendarioAdministrativoService.registrarDiaNoComputable.

### 3.3 Orden de evaluacion en esDiaComputable

1. fecha no null.
2. si es domingo -> false.
3. si es 1 de enero -> false.
4. si es 1 de mayo -> false.
5. si existe una excepcion activa para la fecha -> false.
6. en otro caso -> true.

---

## 4. Fecha de origen no computable

La fecha de origen (fecha de notificacion) **no se cuenta**. El computo comienza al dia siguiente.

Esto es independiente de si la fecha de origen es o no computable.

---

## 5. Tipo de dato del vencimiento: LocalDate

fhVtoApelacion es de tipo LocalDate. Representa el **ultimo dia completo** en que todavia puede presentarse la apelacion.

- El dia almacenado en fhVtoApelacion todavia es apelable.
- El plazo esta vencido recien cuando fechaActual.isAfter(fhVtoApelacion).
- No usar LocalDateTime ni 23:59:59 en dominio ni persistencia.
- Comparacion correcta: ahora.toLocalDate().isAfter(fhVtoApelacion).

---

## 6. Configuracion global

| Propiedad YAML | Clase Java | Default | Rango |
|---|---|---|---|
| faltas.plazos.apelacion-dias-computables | PlazosAdministrativosProperties.apelacionDiasComputables | 30 | 1..3650 |

El valor 30 es el default canonico del dominio. Los unicos lugares productivos autorizados para contenerlo son PlazosAdministrativosProperties y application.yml.

No hardcodear 30 en ningun servicio, calculador, test de paridad ni contrato.

El valor es global y externalizable. No existe actualmente un endpoint HTTP de configuracion.

---

## 7. Fechas fijas

| Fecha | Regla |
|---|---|
| Domingo (semanal) | No computable. Regla fija. No se persiste. |
| 1 de enero | No computable. Regla fija. No se persiste. |
| 1 de mayo | No computable. Regla fija. No se persiste. |

Intentar registrar estas fechas como excepcion local es rechazado como registro redundante (PrecondicionVioladaException).

---

## 8. Excepciones locales activas

- Solo puede haber **un registro activo por fecha**.
- El historial inactivo puede conservar fechas repetidas.
- La unicidad activa se garantiza en guardarActivoSiAusentePorFecha (operacion atomica).

### 8.1 Tipos

| Valor | Descripcion |
|---|---|
| FERIADO | Feriado nacional, provincial o local. |
| ASUETO_ADMINISTRATIVO | Asueto decretado administrativamente. |
| OTRO | Cualquier otra excepcion. |

### 8.2 Origenes

| Valor | Descripcion |
|---|---|
| MANUAL | Ingresado directamente por un operador del sistema. referenciaExterna = null. |
| SINCRONIZACION_EXTERNA | Incorporado via sincronizacion con un proveedor externo. referenciaExterna obligatoria, max 200 caracteres. |

No existe un valor GOOGLE_CALENDAR ni ninguna referencia a un proveedor concreto en el dominio.

---

## 9. Auditoria de alta y baja

| Campo | Alta | Baja |
|---|---|---|
| siActivo | true | false |
| fhAlta | Obligatoria (timestamp de FaltasClock.now()) | Se conserva |
| idUserAlta | Obligatorio, trim, max 36 | Se conserva |
| fhBaja | null | Timestamp de FaltasClock.now() |
| idUserBaja | null | Actor que desactiva, obligatorio, trim, max 36 |

FaltasClock.now() se captura exactamente una vez por operacion exitosa.

El actor de alta (idUserAlta) es obligatorio: null, blanco tras trim o longitud mayor a 36 se rechazan con IllegalArgumentException. El actor de baja (idUserBaja) es obligatorio en la baja con las mismas reglas.

---

## 10. Unicidad activa por fecha

La operacion guardarActivoSiAusentePorFecha garantiza atomicidad: dos altas concurrentes para la misma fecha producen exactamente un registro activo. La alta perdedora resulta en PrecondicionVioladaException en el servicio.

---

## 11. Algoritmo de calculo de vencimiento

```text
fecha = fechaOrigen.plusDays(1)
diasContados = 0

mientras diasContados < cantidadDiasComputables:
    si calendario.esDiaComputable(fecha):
        diasContados++
    si todavia faltan dias:
        fecha = fecha.plusDays(1)

devolver fecha
```

La funcion es pura respecto del tiempo actual. No consulta el reloj.

---

## 12. Ejemplos

### 12.1 Viernes + 1 dia computable

- Fecha origen: viernes 2026-08-07 (no se cuenta)
- Dia siguiente: sabado 2026-08-08
- Sabado es computable: diasContados = 1
- Vencimiento: 2026-08-08

### 12.2 Sabado + 1 dia computable

- Fecha origen: sabado 2026-08-08 (no se cuenta)
- Dia siguiente: domingo 2026-08-09 (no computa)
- Siguiente: lunes 2026-08-10 (computa): diasContados = 1
- Vencimiento: 2026-08-10

### 12.3 Cruzando 1 de enero

- Fecha origen: 2026-12-30 (no se cuenta)
- 2026-12-31 (jueves): computa -> diasContados = 1
- 2027-01-01: no computa (regla fija)
- 2027-01-02 (sabado): computa -> diasContados = 2
- (con cantidad=2): vencimiento = 2027-01-02

### 12.4 Con feriado adicional

- Fecha origen: 2026-07-09 (jueves, no se cuenta)
- 2026-07-10 (viernes): si hay excepcion activa -> no computa
- 2026-07-11 (sabado): computa -> diasContados = 1
- (con cantidad=1): vencimiento = 2026-07-11

### 12.5 30 dias desde 2026-07-10

- Fecha origen: 2026-07-10 (viernes, no se cuenta)
- Contando 30 dias computables (sabado SI computa, domingo NO):
- Vencimiento: 2026-08-14 (viernes)

---

## 13. Invariantes historicas

Un vencimiento ya calculado y persistido en FalActaFallo.fhVtoApelacion **no se recalcula** automaticamente por:
- cambios posteriores en el calendario (nuevas excepciones o bajas de excepciones);
- cambios en la configuracion global (apelacion-dias-computables).

La fecha persistida es el resultado del calculo en el momento de la notificacion positiva.

---

## 14. Arquitectura de sincronizacion futura

La separacion arquitectonica actual prepara una futura sincronizacion sin modificar el calculador:

```text
Proveedor externo (e.g. Google Calendar u otro)
        |
        v  (sincronizacion programada - no implementada actualmente)
Sincronizador externo
        |
        v
Repositorio local de dias no computables (DiaNoComputableRepository)
        |
        v
CalendarioAdministrativoService (implementa CalendarioAdministrativo)
        |
        v
CalculadorPlazosAdministrativos (depende exclusivamente de CalendarioAdministrativo)
```

**Prohibicion**: el calculador no debe consultar ningun proveedor externo, API de calendario, OAuth o servicio HTTP durante el calculo de un vencimiento.

Google Calendar no esta integrado actualmente: sin OAuth, sin HTTP, sin scheduler. El calculador nunca consulta proveedores externos.

---

## 15. Alcance vigente y trabajo futuro

### Implementado (CALENDARIO-PLAZOS-001)

- Enums: TipoDiaNoComputable, OrigenDiaNoComputable, TipoPlazoAdministrativo.
- Modelo: FalDiaNoComputable (InMemory).
- Puertos: CalendarioAdministrativo, ConfiguracionPlazosAdministrativos.
- Repositorio: DiaNoComputableRepository + InMemoryDiaNoComputableRepository.
- Configuracion: PlazosAdministrativosProperties + faltas.plazos.apelacion-dias-computables.
- Servicios: CalendarioAdministrativoService, CalculadorPlazosAdministrativos, PlazosAdministrativosService.
- Resultado: CalculoPlazoAdministrativo (record).
- Spec: este documento, `README.md` y `20-application/fallo-command-contracts.md` (CMD-FALLO-004).
- Tests: 31 tests focales (ver seccion 17).

### Fuera de alcance vigente (roadmap posterior, ver `../90-roadmap/current-roadmap.md`)

- Integracion con Google Calendar ni ningun proveedor externo.
- OAuth, secretos o cliente HTTP.
- Scheduler de sincronizacion.
- Controller HTTP para excepciones.
- DDL MariaDB para FalDiaNoComputable.
- Adapter JDBC para DiaNoComputableRepository.

### Deuda pendiente de persistencia

FalDiaNoComputable es el modelo local InMemory del soporte de excepciones. Su tabla MariaDB y adapter JDBC se incorporaran en el bloque de DDL/JDBC correspondiente (ver `../50-persistence/inmemory-mariadb-deltas.md` y `../50-persistence/mariadb-logical-model.md`). Este documento no afirma paridad fisica con MariaDB.

---

## 16. Artefactos Java canonicos

| Artefacto | Paquete | Rol |
|---|---|---|
| TipoDiaNoComputable | domain.enums | Enum tipo de excepcion |
| OrigenDiaNoComputable | domain.enums | Enum origen del registro |
| TipoPlazoAdministrativo | domain.enums | Enum tipo de plazo |
| FalDiaNoComputable | domain.model | Modelo de excepcion local |
| CalendarioAdministrativo | application.port | Puerto de consulta del calendario |
| ConfiguracionPlazosAdministrativos | application.port | Puerto de configuracion global |
| DiaNoComputableRepository | repository | Repositorio de excepciones |
| InMemoryDiaNoComputableRepository | repository.memory | Implementacion InMemory |
| PlazosAdministrativosProperties | infrastructure.config | Binding de configuracion |
| CalendarioAdministrativoService | application.service | Servicio de calendario local |
| CalculadorPlazosAdministrativos | application.service | Calculador determinista |
| CalculoPlazoAdministrativo | application.model | Record de resultado |
| PlazosAdministrativosService | application.service | Servicio de plazos |

---

## 17. Tests de conformidad

| Test | Casos |
|---|---|
| CalendarioAdministrativoServiceTest | 13 casos: reglas fijas, excepciones, auditoria, concurrencia |
| CalculadorPlazosAdministrativosTest | 12 casos: algoritmo, sabado, excepciones, cruce de anio |
| PlazosAdministrativosServiceTest | 6 casos: configuracion, delegacion, trazabilidad |
