# Matriz de proceso completa — Faltas / Dirección de Faltas
## Documento autocontenido — 2026-06-23

> Matriz productiva de proceso, estados, reglas de transición, eventos y derivaciones de snapshot.  
> Sirve como puente entre modelo de datos, dominio Java/Spring, spec productiva, backend, Angular y QA.

---

## 1. Principio de motor determinista

El sistema debe operar con un motor de proceso determinista.

Cada acción debe tener:

| Elemento | Descripción |
|---|---|
| Acción | Comando explícito del usuario/sistema/integración |
| Precondiciones | Estado requerido para poder ejecutar |
| Efectos | Cambios en tablas fuente de verdad |
| Eventos | Hechos reales append-only |
| Snapshot | Recalculo de bandeja/sub-bandeja/acción pendiente |
| Errores | Rechazos claros si no se cumplen precondiciones |

Prohibido:

- acciones que no hacen nada;
- acciones visibles sin transición real;
- acciones que ejecutan otra cosa;
- archivo automático sin decisión;
- fallo automático sin decisión;
- cambios de estado por historial accidental;
- eventos de bandeja/proyección;
- UI-only como catálogo productivo.

---

## 2. Motores/ejes de estado

| Motor | Fuente |
|---|---|
| Acta principal | `fal_acta` |
| Documentos/firma | `fal_documento`, `fal_documento_firma_req`, `fal_documento_firma` |
| Notificación | `fal_notificacion`, intentos/lotes si aplica |
| Pago voluntario | Pago/movimientos/eventos |
| Pago condena | Pago/movimientos/eventos |
| Fallo/resultado | `fal_acta_fallo`, `fal_acta.resultado_final` |
| Apelación | módulo/tablas de apelación si se implementa separado |
| Archivo/reingreso | `fal_acta_archivo`, `fal_motivo_archivo` |
| Gestión externa | `fal_acta_gestion_externa` |
| Bloqueantes materiales | tablas/movimientos de bloqueantes |
| Snapshot | `fal_acta_snapshot` |

Regla:

> Ningún eje debe ocultar al otro. Por ejemplo, documento firmado no equivale a bloqueante liberado, fallo dictado no equivale siempre a resultado final, bandeja no equivale a evento.

---


## Criterio final sobre `bloque_actual`

`bloque_actual` usa códigos compactos semánticos productivos en DB. No se usan como valores productivos los nombres `D1/D2/D4/D5` heredados del prototipo, porque arrastran numeración histórica y dejan huecos al eliminar `D3_DOCUMENTAL`.

| Código DB | Bloque productivo | Descripción |
|---|---|---|
| `CAPT` | `CAPTURA` | Captura/labrado inicial |
| `ENRI` | `ENRIQUECIMIENTO` | Enriquecimiento/completitud del acta |
| `NOTI` | `NOTIFICACION` | Notificación del acta, fallo u otra pieza |
| `ANAL` | `ANALISIS` | Análisis, resolución, fallo, pagos y apelación |
| `GEXT` | `GESTION_EXTERNA` | Gestión externa: apremio / juzgado de paz |
| `ARCH` | `ARCHIVO` | Archivo administrativo/procesal |
| `CERR` | `CERRADA` | Cierre definitivo del circuito |

Equivalencias históricas sólo para migración/lectura de mocks/compatibilidad temporal:

| Valor histórico/prototipo | Valor productivo |
|---|---|
| `D1_CAPTURA` | `CAPT` |
| `D2_ENRIQUECIMIENTO` | `ENRI` |
| `D3_DOCUMENTAL` | eliminado / no productivo |
| `D4_NOTIFICACION` | `NOTI` |
| `D5_ANALISIS` | `ANAL` |

Reglas:

- No persistir `D1_CAPTURA`, `D2_ENRIQUECIMIENTO`, `D4_NOTIFICACION` ni `D5_ANALISIS` como valores productivos.
- No crear bloque documental.
- La etapa documental se representa por `cod_bandeja = PENDIENTE_PREPARACION_DOCUMENTAL`, sub-bandejas documentales, `fal_documento` y firma documental.


---

## 4. Bandejas productivas

Bandejas reales:

| Bandeja |
|---|
| `ACTAS_EN_ENRIQUECIMIENTO` |
| `PENDIENTE_PREPARACION_DOCUMENTAL` |
| `PENDIENTE_FIRMA` |
| `PENDIENTE_NOTIFICACION` |
| `EN_NOTIFICACION` |
| `PENDIENTE_ANALISIS` |
| `PENDIENTES_RESOLUCION_REDACCION` |
| `PENDIENTES_FALLO` |
| `CON_APELACION` |
| `GESTION_EXTERNA` |
| `PARALIZADAS` |
| `ARCHIVO` |
| `CERRADAS` |

UI-only/no productivo:

| Valor | Motivo |
|---|---|
| `NOTIFICACIONES` | Agregador UI |
| `LABRADAS` | Legacy UI |
| `EN_TRABAJO` | Alias visual |
| `TODOS` | Filtro |
| `ENTREGA_DOC` | Copy/abreviatura |

---

## 5. Matriz principal de transición

| Acción/hecho | Precondiciones | Cambios fuente | Eventos | Snapshot esperado |
|---|---|---|---|---|
| Labrar acta | Talonario válido, dependencia/inspector vigente, tipo acta permitido | `bloque=CAPT`, `sit_adm=ACTIVA`, `resultado_final=SIN_RESULTADO_FINAL` | `ACTLAB` | Enriquecimiento/captura inicial |
| Completar captura | Datos mínimos válidos | Puede pasar a `ENRI` | Evento específico si aplica | `ACTAS_EN_ENRIQUECIMIENTO` |
| Enriquecer acta | Acta activa, bloque `ENRI` | Datos completos, constataciones/documentos si aplica | `CONS*`, `DOCGEN` | Sub-bandeja según pendiente |
| Generar piezas documentales | Pieza requerida | Documento emitido/pendiente firma | `DOCGEN` | Preparación documental/firma |
| Firmar documento | Documento pendiente firma | Documento firmado | `DOCFIR` | Según pieza: notificación/análisis/cierre |
| Enviar a notificación | Pieza notificable lista | `bloque=NOTI`, estado pendiente/envío | `NOTENV` | `PENDIENTE_NOTIFICACION` o `EN_NOTIFICACION` |
| Notificación positiva | Notificación enviada/lista | Resultado notificación positivo | `NOTPOS` | Avanza según pieza |
| Notificación negativa | Notificación enviada | Resultado negativo | `NOTNEG` | Evaluar/reintentar |
| Notificación vencida | Plazo vencido | Resultado vencido | `NOTVNC` | Evaluar vencimiento |
| Portal visualiza pieza | Pieza disponible en portal | Notificación positiva por portal; otras pueden quedar sin efecto | `NOTPVI`, opcional `NOTSUP` | Avanza como notificado |
| Solicitar pago voluntario | Estado permite pago | `situacion_pago=SOLICITADO`, monto si aplica | `PAGVSO`, `PAGVMF` | Espera informe/confirmación |
| Informar pago voluntario | Pago solicitado | `PENDIENTE_CONFIRMACION` | `PAGINF`, `PAGCMP` | Pendiente confirmar |
| Confirmar pago voluntario | Pago pendiente confirmación | `CONFIRMADO`, `resultado_final=PAGO_CONFIRMADO` | `PAGCNF` | Cerrable sólo si no hay bloqueantes |
| Observar pago voluntario | Pago pendiente confirmación | `OBSERVADO` | `PAGOBS` | Corregir/evaluar |
| Vencer pago voluntario | Plazo vencido | `VENCIDO` | `PAGVVN` | Habilita análisis/fallo |
| Dictar fallo absolutorio | Análisis habilitado | `fal_acta_fallo=ABSOLUTORIO`, documento fallo | `FALABS`, `DOCGEN` | Firma/notificación |
| Dictar fallo condenatorio | Análisis habilitado | `fal_acta_fallo=CONDENATORIO`, monto | `FALCON`, `DOCGEN` | Firma/notificación |
| Notificar fallo absolutorio | Fallo firmado/notificable | `resultado_final=ABSUELTO` | `NOTPOS` | Cerrable si no hay bloqueantes |
| Notificar fallo condenatorio | Fallo firmado/notificable | Resultado condena según reglas | `NOTPOS` | Plazo apelación/pago condena |
| Vencer plazo apelación | Condena notificada, sin apelación | `resultado_final=CONDENA_FIRME` | `PLAVNC`, `CONFIR` | Pago/cierre según reglas |
| Presentar apelación | Dentro de plazo | Apelación registrada | `APEPRE` | `CON_APELACION` |
| Resolver apelación rechazada | Apelación presentada | Condena firme | `APERAZ`, `CONFIR` | Pago/cierre |
| Resolver apelación aceptada | Apelación presentada | `resultado_final=ABSUELTO` | `APEABS` | Cerrable si no hay bloqueantes |
| Informar pago condena | Condena aplicable | `pago_condena=INFORMADO` | `PCOINF` | Pendiente confirmar |
| Confirmar pago condena | Pago condena informado | `pago_condena=CONFIRMADO` | `PCOCNF` | Cerrable si no hay bloqueantes |
| Observar pago condena | Pago condena informado | `pago_condena=OBSERVADO` | `PCOOBS` | Corregir/evaluar |
| Reconocer bloqueante material | Hecho material detectado | Bloqueante activo | `ORG*` / `CONS*` | No cerrable |
| Generar resolutorio material | Bloqueante requiere resolutorio | Documento generado/firmable | `DOCGEN` | Pendiente firma |
| Cumplir materialmente | Cumplimiento efectivo | Bloqueante verificado | `CUM*` | Recalcular cerrabilidad |
| Derivar gestión externa | Reglas permiten | `bloque=GEXT`, gestión activa | `EXTDER` | `GESTION_EXTERNA` |
| Reingresar gestión externa | Ciclo externo activo | Resultado/modo reingreso | `EXTRET` + resultado | Bandeja según modo |
| Registrar pago apremio | Gestión apremio activa | Pago externo registrado | `PAGAPR` | Reingreso/revisión/cierre |
| Archivar acta | Acción explícita, motivo válido | `bloque=ARCH`, ciclo archivo activo | `ARCHIV` o `ANUNUL` | `ARCHIVO` |
| Reingresar archivo | Archivo permite reingreso | Restaurar/encaminar según origen/motivo | `REINGR` | Bandeja determinista |
| Paralizar acta | Motivo válido | `sit_adm=PARALIZADA` | `PARALZ` | `PARALIZADAS` |
| Reactivar acta | Acta paralizada | Vuelve a activa y se encamina | `REACTV` | Según estado |
| Cerrar acta | Cerrable verdadero | `bloque=CERR`, `sit_adm=CERRADA` | `CIERRA` | `CERRADAS` |

---

## 6. Matriz de cerrabilidad

| Condición | Cerrable |
|---|---:|
| Sin resultado final | No |
| Resultado final válido + bloqueantes activos | No |
| Resultado final válido + documentos obligatorios pendientes | No |
| Resultado final válido + notificación obligatoria pendiente | No |
| Pago pendiente de confirmación | No |
| Gestión externa activa | No |
| Paralizada | No |
| Archivo activo | No, salvo regla explícita de cierre por archivo definitivo |
| Resultado final válido + sin bloqueantes + sin pendientes | Sí |

Resultado final compatible inicial:

| Resultado |
|---|
| `ABSUELTO` |
| `PAGO_CONFIRMADO` |
| `CONDENA_FIRME` |
| `CONDENADO`, sólo si la spec define que puede cerrar sin firmeza/apelación pendiente |

Regla:

> No confundir resultado final compatible con cerrabilidad. Cerrabilidad exige además no tener bloqueantes ni pendientes impeditivos.

---

## 7. Matriz de acciones disponibles

| Acción | Visible si | Prohibida si |
|---|---|---|
| Pago voluntario | Estado permite pago y no hay pago incompatible | No ejecutará transición real |
| Informar pago | Pago solicitado | Pago no iniciado |
| Confirmar pago | Pago pendiente confirmación | No hay pago pendiente |
| Observar pago | Pago pendiente confirmación | No hay pago pendiente |
| Vencer pago | Pago solicitado y plazo vencido | Pago confirmado |
| Dictar fallo | Análisis habilitado | Hay pago/acto pendiente incompatible |
| Firmar documento | Documento pendiente firma | Ya firmado |
| Notificar | Pieza notificable lista | No hay pieza notificable |
| Reintentar notificación | Negativa/vencida evaluable | Portal ya notificó positivamente |
| Archivar | Motivo válido y acción explícita | Automático/colateral |
| Reingresar archivo | Motivo permite reingreso | Archivo no permite reingreso |
| Derivar gestión externa | Estado permite | Ya hay gestión activa |
| Reingresar gestión externa | Gestión activa | No hay ciclo activo |
| Paralizar | Acta activa | Cerrada/archivo/gestión externa activa |
| Reactivar | Paralizada | No paralizada |
| Cerrar | Cerrable verdadero | Falla cualquier regla |

---

## 8. Motor documental

Estados:

| Estado |
|---|
| `EMITIDO` |
| `PENDIENTE_FIRMA` |
| `FIRMADO` |
| `ADJUNTO` |

Reglas:

- Documento generado no equivale a firma.
- Documento firmado no equivale a notificación.
- Resolutorio firmado no equivale a cumplimiento material.
- Firma externa real queda simulada por interfaz de prueba hasta integración.

Bandeja de firma:

- La bandeja `PENDIENTE_FIRMA` se resuelve desde `fal_documento_firma_req` (requisitos activos/pendientes).
- La compatibilidad se evalua via `fal_firmante_version_habilitacion` (tipo_docu + rol_firma_req + mecanismo).
- Al firmar: se crea `fal_documento_firma` y se actualiza `fal_documento_firma_req` (estado=FIRMADO).
- Ver Secciones 5.4, 5.5 y 5.6 del MODELO_MARIADB para el flujo completo.

---

## 9. Motor notificación

Canales:

| Canal |
|---|
| `CORREO_POSTAL` |
| `NOTIFICADOR_MUNICIPAL` |
| `PRESENCIAL` |
| `PORTAL_INFRACTOR` |
| `EMAIL` |

Estados:

| Estado |
|---|
| `PENDIENTE_PREPARACION` |
| `LISTA_PARA_ENVIO` |
| `ENVIADA` |
| `ENTREGADA` |
| `NEGATIVA` |
| `VENCIDA` |
| `SIN_EFECTO` |

Regla portal:

> Portal infractor puede producir notificación positiva. Otros canales pendientes pueden quedar sin efecto por `SUPERADA_POR_PORTAL`.

---

## 10. Motor pagos

Pago voluntario:

| Estado |
|---|
| `SIN_PAGO` |
| `SOLICITADO` |
| `PENDIENTE_CONFIRMACION` |
| `CONFIRMADO` |
| `OBSERVADO` |
| `VENCIDO` |

Pago condena:

| Estado |
|---|
| `NO_APLICA` |
| `PENDIENTE` |
| `INFORMADO` |
| `CONFIRMADO` |
| `OBSERVADO` |

Regla:

> `PAGO_INFORMADO` no es estado productivo de pago voluntario.

---

## 11. Motor archivo/reingreso

Archivo requiere:

| Dato |
|---|
| Motivo administrable |
| Flags de motivo |
| Estado origen |
| Situación origen |
| Bloque origen |
| Bandeja/sub-bandeja origen |
| Acción pendiente origen |
| Observación si corresponde |
| Evento de archivo |

Reglas:

- Archivo explícito.
- No automático.
- Reingreso determinista.
- No loop accidental.

---

## 12. Motor gestión externa

Dimensiones:

| Dimensión |
|---|
| `tipo_gestion_ext` |
| `estado_gestion_ext` |
| `resultado_gestion_ext` |
| `modo_reingreso_gestion_ext` |
| documentos externos |
| observaciones |
| eventos |

Regla:

> No string compuesto.

---

## 13. Bloqueantes materiales

Bloqueantes:

| Bloqueante |
|---|
| `MEDIDA_PREVENTIVA_ACTIVA` |
| `RODADO_SECUESTRADO` |
| `DOCUMENTACION_RETENIDA` |

Regla crítica:

> Cierre sólo si no existen bloqueantes activos. Firma de resolutorio no libera; cumplimiento material efectivo libera.

---

## 14. Eventos productivos y prohibidos

`tipo_evt` productivo:

- `CHAR(6)`;
- enum/constante Spring;
- hechos reales de dominio;
- `CONFIR` conservado.

Prohibidos:

- eventos de bandeja;
- eventos de estado;
- eventos demo;
- eventos genéricos que oculten semántica.

---

## 15. Reglas de determinismo QA

| Hallazgo QA | Regla de motor |
|---|---|
| Acción visible que no hace nada | Prohibido |
| Pago termina en fallo | Routing incorrecto |
| Archivo automático | Prohibido |
| Error 409 por estado inconsistente | Validación previa |
| Resultado final no actualizado | Consolidación explícita |
| Reingreso no determinista | Guardar origen/modo |
| Cierre bloqueado por materiales | Correcto |

---

## 16. Simulaciones temporales

| Integración | Simulación permitida |
|---|---|
| Firma | Botón UI prueba, endpoint real |
| Retiro rodado | Botón UI prueba, endpoint real |
| Notificador municipal | Botón UI prueba, endpoint real |
| Portal ciudadano | Usuario mock, endpoint real |

Regla:

> Interfaz de prueba sí; lógica mock no.

---

## 17. Conversión a spec/backend

La spec productiva debe derivar de esta matriz:

1. enums Java;
2. comandos;
3. precondiciones;
4. transiciones;
5. eventos;
6. efectos;
7. snapshot;
8. errores;
9. tests;
10. UX Angular.

---

## 18. Cierre

Esta matriz reemplaza cualquier matriz previa o parcial.

No volver a introducir:

- `D1/D2/D4/D5` productivos;
- `D3_DOCUMENTAL`;
- acción pendiente string libre;
- eventos de proyección;
- pagos como estados ambiguos;
- archivo automático;
- cierre con bloqueantes.
