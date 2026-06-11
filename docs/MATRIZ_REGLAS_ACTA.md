# CICLO DE VIDA Y MATRIZ DE REGLAS DEL ACTA

---

## 1. Propósito

Este documento describe el ciclo de vida completo del acta dentro del sistema de faltas.

Su función es responder, para cada momento posible de la vida del expediente, las siguientes preguntas:

- ¿En qué bandeja/estado está el acta en este momento?
- ¿Qué puede pasar ahora?
- ¿Quién realiza la acción o registra el hecho?
- ¿Qué condición debe cumplirse para que eso ocurra?
- ¿A dónde va el acta después?
- ¿Qué está prohibido en este momento?
- ¿Qué bloquea el avance?

Es el documento de referencia para validar con Dirección cualquier cambio de regla funcional antes de trasladarlo a implementación técnica.

---

## 2. Lectura rápida del ciclo de vida

El recorrido típico de un acta transcurre por estas etapas en secuencia:


Labrado
  -> Enriquecimiento / Captura inicial
  -> Preparación documental
  -> Pendiente de firma (acta u otras piezas)
  -> Notificación (pendiente envío -> en trámite)
  -> Análisis / Presentaciones / Pagos
      -> Pago voluntario -> Cierre
      -> Pendiente de fallo -> Firma fallo -> Notificación fallo
          -> Apelación (si se presenta) -> Condena firme
          -> Vencimiento plazo apelación -> Condena firme
              -> Pago condena -> Cierre
              -> Gestión externa -> Reingreso -> ...
          -> Absolución -> Cierre
      -> Resolución / nulidad / pieza no-fallo -> Firma -> Notificación -> ...

Caminos especiales que pueden originarse en casi cualquier etapa:

- **Paralización**: suspende el circuito por causal fundada; se reanuda con reactivación explícita.
- **Archivo**: el trámite principal está resuelto pero subsiste un bloqueante de cierre (medida activa, material pendiente).
- **Resolución de bloqueantes materiales**: genera y firma documentos de levantamiento/liberación; al completarse el cumplimiento material habilita el cierre.

---

## 3. Nacimiento del acta

| Aspecto | Descripción |
|---|---|
| **Quién la crea** | Inspector, desde la app móvil de inspectores u otro canal formal de labrado admitido |
| **Qué ocurre** | El acta queda registrada en el sistema con identidad interna, identidad técnica de origen e identidad administrativa visible (número de talonario) |
| **Bandeja inicial** | `ACTAS_EN_ENRIQUECIMIENTO` |
| **Estado proceso inicial** | `LABRADA` |
| **Sub-bandeja inicial** | `CAPTURA_INICIAL` (datos básicos aún incompletos) |
| **Qué contiene en este momento** | Datos básicos del labrado: infractor, inspector, dependencia, hecho, ubicación, fecha/hora, evidencias iniciales |
| **Qué le falta** | Completar datos, generar documentos iniciales requeridos, definir piezas necesarias |
| **Primera decisión operativa** | ¿Pasa a revisión/enriquecimiento? ¿Ya puede ir a notificación? ¿Se origina pago voluntario? ¿Requiere nulidad desde el inicio? |

---

## 4. Flujo principal de vida del acta

> **Referencia de formato**: la columna *Bandeja* usa el código de bandeja operativa (`bandejaActual`) y la columna *Estado proceso* usa el valor de `estadoProcesoActual`.

| Momento del ciclo | Bandeja origen | Estado proceso origen | Acción / evento | Condición | Bandeja destino | Estado proceso destino | Resultado / efecto |
|---|---|---|---|---|---|---|---|
| **Labrado del acta** | — | — | Inspector labra el acta y la sincroniza | Acta creada en canal válido | `ACTAS_EN_ENRIQUECIMIENTO` | `LABRADA` | Acta ingresa al circuito operativo |
| **Captura inicial** | `ACTAS_EN_ENRIQUECIMIENTO` | `LABRADA` | Completar datos mínimos de labrado en sitio | Datos básicos registrados | `ACTAS_EN_ENRIQUECIMIENTO` | `LABRADA` | Sub-bandeja `CAPTURA_INICIAL`; datos disponibles para revisión |
| **Completitud documental (enriquecimiento)** | `ACTAS_EN_ENRIQUECIMIENTO` | `LABRADA` | Revisar, validar y completar el expediente | Revisión inicial realizada por operador | `ACTAS_EN_ENRIQUECIMIENTO` | `EN_ENRIQUECIMIENTO` | Expediente en revisión activa; se identifican piezas faltantes |
| **Generación de piezas iniciales** | `ACTAS_EN_ENRIQUECIMIENTO` | `EN_ENRIQUECIMIENTO` | Generar acta formal y/o medidas preventivas requeridas | Piezas identificadas como necesarias | `PENDIENTE_PREPARACION_DOCUMENTAL` | `PENDIENTE_PREPARACION_DOCUMENTAL` | Piezas documentales creadas; pendientes de firma |
| **Firma de acta inicial y otras piezas** | `PENDIENTE_PREPARACION_DOCUMENTAL` | `PENDIENTE_PREPARACION_DOCUMENTAL` | Firmar acta y/o piezas requeridas | Documentos generados; firmante identificado | `PENDIENTE_NOTIFICACION` | `PENDIENTE_ENVIO` | Piezas firmadas; expediente listo para notificación |
| **Envío a notificación** | `PENDIENTE_NOTIFICACION` | `PENDIENTE_ENVIO` | Iniciar diligencia de notificación | Pieza firmada disponible; canal definido | `EN_NOTIFICACION` | `EN_NOTIFICACION` | Notificación enviada al canal; se aguarda resultado |
| **Notificación positiva** | `EN_NOTIFICACION` | `EN_NOTIFICACION` | Registrar acuse positivo | Acuse fehaciente recibido | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Expediente ingresa a análisis; abre instancia de análisis jurídico |
| **Notificación negativa** | `EN_NOTIFICACION` | `EN_NOTIFICACION` | Registrar acuse negativo o vencimiento | Sin acuse en plazo aplicable, o acuse negativo | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Sub-bandeja notif. negativa/vencida; se evalúa reintento u otro canal de notificación; no habilita tratamiento de fondo como notificación positiva |
| **Análisis general** | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Analizar expediente; determinar próximo paso | Notificación produjo resultado; o reencauce previo | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Permanece hasta que se define acción concreta |
| **Solicitud de pago voluntario** | Cualquier bandeja interna operable | Variable | Infractor solicita acogerse a pago voluntario | Acta no cerrada; bandeja operable (no GESTION_EXTERNA, no ARCHIVO, no PARALIZADAS, no CERRADAS); estadoProcesoActual distinto de CONDENA_FIRME (en ese caso corresponde pago de condena) | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Sub-bandeja `ANALISIS_PAGO_SOLICITADO`; solicitud registrada |
| **Pago voluntario informado** | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Infractor informa haber realizado el pago | Pago voluntario en curso; comprobante presentado | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Sub-bandeja `ANALISIS_PAGO_INFORMADO`; pago pendiente de verificación |
| **Pago voluntario confirmado** | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Confirmar y verificar pago voluntario | Pago verificado; sin bloqueantes de cierre | `CERRADAS` | `CERRADA` | `resultadoFinal=PAGO_CONFIRMADO`; expediente cerrado |
| **Fallo absolutorio dictado** | `PENDIENTES_FALLO` | `PENDIENTE_FALLO` | Dictar fallo absolutorio | Expediente en condición de fallo; análisis completado | `PENDIENTE_FIRMA` | `PENDIENTE_FIRMA` | Fallo absolutorio generado (`FALLO_ABSOLUTORIO`); pendiente de firma |
| **Fallo condenatorio dictado** | `PENDIENTES_FALLO` | `PENDIENTE_FALLO` | Dictar fallo condenatorio | Expediente en condición de fallo; análisis completado | `PENDIENTE_FIRMA` | `PENDIENTE_FIRMA` | Fallo condenatorio generado (`FALLO_CONDENATORIO`); pendiente de firma |
| **Firma de fallo** | `PENDIENTE_FIRMA` | `PENDIENTE_FIRMA` | Firmar fallo (absolutorio o condenatorio) | Fallo generado; firmante disponible | `PENDIENTE_NOTIFICACION` | `PENDIENTE_ENVIO` | Fallo firmado; listo para notificación |
| **Notificación de fallo absolutorio positiva** | `EN_NOTIFICACION` | `EN_NOTIFICACION` | Registrar acuse positivo sobre `FALLO_ABSOLUTORIO` | Acuse fehaciente sobre pieza de tipo fallo | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | `resultadoFinal=ABSUELTO`; habilita cierre si sin bloqueantes |
| **Notificación de fallo condenatorio positiva** | `EN_NOTIFICACION` | `EN_NOTIFICACION` | Registrar acuse positivo sobre `FALLO_CONDENATORIO` | Acuse fehaciente; 5 días de espera antes del destino natural | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | `resultadoFinal=CONDENADO`; abre plazo de apelación |
| **Apelación presentada** | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Registrar apelación del infractor | Dentro del plazo; `resultadoFinal=CONDENADO`; sin apelación activa | `CON_APELACION` | `CON_APELACION` | Expediente entra en tramo recursivo |
| **Resolución de apelación — condena confirmada** | `CON_APELACION` | `CON_APELACION` | Resolver apelación rechazada | Análisis recursivo completado; apelación no admitida | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | `resultadoFinal=CONDENA_FIRME`; condena queda firme |
| **Resolución de apelación — con lugar** | `CON_APELACION` | `CON_APELACION` | Resolver apelación con lugar | Apelación admitida; requiere nueva decisión | `PENDIENTES_FALLO` o `PENDIENTES_RESOLUCION_REDACCION` | `PENDIENTE_FALLO` | Expediente requiere nuevo fallo o nueva pieza |
| **Vencimiento de plazo de apelación** | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Registrar vencimiento sin apelación | Plazo transcurrido; sin apelación; `resultadoFinal=CONDENADO` | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | `resultadoFinal=CONDENA_FIRME`; condena firme |
| **Condena firme (estado)** | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | — (estado resultante) | `resultadoFinal=CONDENA_FIRME` activo | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | Infractor debe pagar o el expediente se deriva a gestión externa |
| **Pago de condena informado** | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | Infractor informa pago de condena | Condena firme; comprobante presentado | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | Sub-bandeja `CONDENA_PAGO_INFORMADO`; pago pendiente de confirmación |
| **Pago de condena confirmado** | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | Confirmar pago de condena | Pago verificado; sin bloqueantes de cierre | `CERRADAS` | `CERRADA` | `resultadoFinal=CONDENA_FIRME`; expediente cerrado por pago de condena |
| **Derivación a gestión externa** | `PENDIENTE_ANALISIS` | `CONDENA_FIRME` | Derivar a gestión externa | Condena firme; sin pago; plazo vencido o decisión administrativa | `GESTION_EXTERNA` | `EN_GESTION_EXTERNA` | Expediente sale del circuito interno; conserva trazabilidad |
| **Reingreso desde gestión externa** | `GESTION_EXTERNA` | `EN_GESTION_EXTERNA` | Reingresar expediente con resultado externo | Resultado externo produce efecto material | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Expediente vuelve al circuito; se define nueva acción |
| **Archivo** | Cualquier bandeja interna activa | Variable | Archivar expediente | Trámite resuelto/avanzado; subsiste bloqueante de cierre | `ARCHIVO` | `EN_ARCHIVO` | Expediente en archivo operativo; no puede cerrarse todavía |
| **Reingreso desde archivo** | `ARCHIVO` | `EN_ARCHIVO` | Reingresar expediente | Novedad con efecto material que obliga a retomar trámite | `PENDIENTE_ANALISIS` | `PENDIENTE_REVISION` | Expediente vuelve al circuito; se asigna siguiente bandeja |
| **Paralización** | Cualquier bandeja interna | Variable | Paralizar expediente por causal fundada | Causal operativa válida documentada y registrada | `PARALIZADAS` | `PARALIZADA` | Circuito suspendido; no aplican acciones operativas normales |
| **Reactivación** | `PARALIZADAS` | `PARALIZADA` | Levantar paralización | Causal levantada; decisión formal de reanudación | Bandeja correspondiente a la situación real | Estado correspondiente | Expediente vuelve al circuito; destino según situación previa |
| **Resolución de bloqueante material** | Cualquier bandeja interna operable (no GESTION_EXTERNA, ARCHIVO, PARALIZADAS, CERRADAS) | Variable | Generar resolutorio (levantamiento medida / liberación rodado / restitución documental) | Bloqueante material identificado; decisión tomada | Misma bandeja | Estado actual conservado | Resolutorio generado; pendiente de firma si requiere firma |
| **Firma de resolutorio material** | `PENDIENTE_FIRMA` | `PENDIENTE_FIRMA` | Firmar resolutorio de bloqueante | Resolutorio con circuito de firma generado | Retorna a bandeja correspondiente | Estado correspondiente | Resolutorio firmado; habilita registro de cumplimiento material |
| **Cumplimiento material efectivo** | Bandeja con bloqueante activo | Variable | Registrar entrega, restitución o liberación material | Resolutorio firmado o emitido; hecho material realizado | `ARCHIVO` (si quedan bloqueantes) o `CERRADAS` (sin bloqueantes) | `EN_ARCHIVO` o `CERRADA` | Bloqueante eliminado; si no quedan bloqueantes, habilita cierre |
| **Cierre del acta** | `PENDIENTE_ANALISIS`, `ARCHIVO`, u otra bandeja con resultado habilitante | Variable | Cerrar acta | Sin bloqueantes; `resultadoFinal` en valor habilitante | `CERRADAS` | `CERRADA` | Expediente cerrado definitivamente |

---

## 5. Caminos alternativos y estados suspendidos

### 5.1 Caminos alternativos dentro del flujo

| Situación | Desde | Descripción |
|---|---|---|
| **Pago voluntario antes del fallo** | Cualquier bandeja interna operable | El infractor puede pagar antes de que se dicte fallo. El circuito cierra sin condena. |
| **Nulidad** | `PENDIENTES_RESOLUCION_REDACCION` | Defecto subsanable motiva nulidad; se genera pieza, se firma, se notifica y puede cerrar sin fallo. |
| **Fallo absolutorio** | `PENDIENTES_FALLO` | Sin condena. Se firma, notifica y habilita cierre si no hay bloqueantes. |
| **Apelación con lugar** | `CON_APELACION` | Puede requerir nuevo fallo, rectificación o nueva pieza no-fallo; el recorrido se bifurca. |
| **Pago externo en gestión externa** | `GESTION_EXTERNA` | El resultado externo puede incluir pago; al reingresar puede habilitarse cierre sin nuevo trámite completo. |
| **Cierre directo** | Desde cualquier bandeja interna | Si todas las condiciones de cierre se cumplen simultáneamente, el expediente puede pasar directamente a `CERRADAS` sin pasar por `ARCHIVO`. |
| **Consentimiento de condena desde portal** | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | El infractor consiente la condena voluntariamente. Efecto: `resultadoFinal=CONDENA_FIRME` (igual que vencimiento sin apelación). |

### 5.2 Estados que suspenden el circuito

| Estado | Bandeja | Descripción | Cómo se levanta |
|---|---|---|---|
| **Paralización** | `PARALIZADAS` | Trámite detenido por causal fundada y documentada | Reactivación con decisión formal y fundamento registrado |
| **Gestión externa activa** | `GESTION_EXTERNA` | Expediente fuera del circuito interno; en trámite externo | Reingreso con resultado formal que produce efecto material |
| **Archivo con bloqueante** | `ARCHIVO` | Trámite resuelto pero hay medida activa, liberación o restitución pendiente | Reingreso al circuito operativo; la resolución de bloqueantes se ejecuta en la bandeja correspondiente tras el reingreso |

### 5.3 Estado terminal

| Estado | Bandeja | Descripción | Permite reingreso |
|---|---|---|---|
| **Cerrada** | `CERRADAS` | Expediente concluido operativamente; sin bloqueantes | Solo de forma excepcional, con habilitación formal y trazabilidad |

---

## 6. Matriz de transición por acción

| Acción | Bandeja / estado origen | Condición requerida | Bandeja / estado destino | Efecto sobre `resultadoFinal` | Actor responsable |
|---|---|---|---|---|---|
| Labrar acta | — | Inspector en sitio | `ACTAS_EN_ENRIQUECIMIENTO / LABRADA` | — | Inspector |
| Completar enriquecimiento | `ACTAS_EN_ENRIQUECIMIENTO` | Revisión inicial realizada | Permanece `ACTAS_EN_ENRIQUECIMIENTO / EN_ENRIQUECIMIENTO` | — | Operador administrativo |
| Generar piezas documentales iniciales | `ACTAS_EN_ENRIQUECIMIENTO` o `PENDIENTE_PREPARACION_DOCUMENTAL` | Piezas identificadas como requeridas | `PENDIENTE_PREPARACION_DOCUMENTAL` → `PENDIENTE_FIRMA` | — | Operador administrativo |
| Firmar piezas iniciales | `PENDIENTE_FIRMA / PENDIENTE_FIRMA` | Documentos generados; firmante disponible | `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO` | — | Firmante autorizado |
| Iniciar notificación | `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO` | Canal definido; pieza firmada | `EN_NOTIFICACION / EN_NOTIFICACION` | — | Operador / sistema |
| Registrar acuse positivo (acta) | `EN_NOTIFICACION / EN_NOTIFICACION` | Acuse fehaciente sobre pieza de tipo acta | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | — | Operador / notificador |
| Registrar acuse negativo / vencimiento | `EN_NOTIFICACION / EN_NOTIFICACION` | Sin acuse en plazo o negativa confirmada | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | — | Operador / sistema |
| Solicitar pago voluntario | Cualquier bandeja interna operable | Acta no cerrada; no en GESTION_EXTERNA, ARCHIVO, PARALIZADAS, CERRADAS | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | — | Infractor (portal) / operador |
| Informar pago voluntario | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Pago solicitado; comprobante presentado | Permanece `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | — | Infractor / operador |
| Confirmar pago voluntario | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Pago verificado; sin bloqueantes | `CERRADAS / CERRADA` | `PAGO_CONFIRMADO` | Tesorería / operador |
| Enviar a pendiente de fallo | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Condición de fallo cumplida | `PENDIENTES_FALLO / PENDIENTE_FALLO` | — | Operador jurídico |
| Dictar fallo absolutorio | `PENDIENTES_FALLO / PENDIENTE_FALLO` | Análisis completado; decisión absolutoria | `PENDIENTE_FIRMA / PENDIENTE_FIRMA` | → `ABSUELTO` al notificar | Juez / autoridad competente |
| Dictar fallo condenatorio | `PENDIENTES_FALLO / PENDIENTE_FALLO` | Análisis completado; decisión condenatoria | `PENDIENTE_FIRMA / PENDIENTE_FIRMA` | → `CONDENADO` al notificar | Juez / autoridad competente |
| Firmar fallo absolutorio | `PENDIENTE_FIRMA / PENDIENTE_FIRMA` | Fallo absolutorio generado; firmante disponible | `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO` | — | Firmante autorizado |
| Firmar fallo condenatorio | `PENDIENTE_FIRMA / PENDIENTE_FIRMA` | Fallo condenatorio generado; firmante disponible | `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO` | — | Firmante autorizado |
| Notificación positiva de fallo absolutorio | `EN_NOTIFICACION / EN_NOTIFICACION` | Acuse fehaciente sobre `FALLO_ABSOLUTORIO` | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | `ABSUELTO` | Operador / notificador |
| Notificación positiva de fallo condenatorio | `EN_NOTIFICACION / EN_NOTIFICACION` | Acuse fehaciente sobre `FALLO_CONDENATORIO`; 5 días de espera | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | `CONDENADO`; abre plazo de apelación | Operador / notificador |
| Registrar apelación | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Dentro del plazo; `resultadoFinal=CONDENADO`; sin apelación activa | `CON_APELACION / CON_APELACION` | — | Infractor (portal QR) / operador |
| Resolver apelación — rechazada | `CON_APELACION / CON_APELACION` | Apelación no admitida | `PENDIENTE_ANALISIS / CONDENA_FIRME` | `CONDENA_FIRME` | Autoridad competente |
| Resolver apelación — con lugar | `CON_APELACION / CON_APELACION` | Apelación admitida; requiere nueva decisión | `PENDIENTES_FALLO / PENDIENTE_FALLO` o `PENDIENTES_RESOLUCION_REDACCION` | — | Autoridad competente |
| Registrar vencimiento de apelación | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Plazo vencido sin apelación; `resultadoFinal=CONDENADO` | `PENDIENTE_ANALISIS / CONDENA_FIRME` | `CONDENA_FIRME` | Operador / sistema |
| Consentir condena desde portal | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | `resultadoFinal=CONDENADO`; sin apelación pendiente | `PENDIENTE_ANALISIS / CONDENA_FIRME` | `CONDENA_FIRME` | Infractor (portal) |
| Informar pago de condena | `PENDIENTE_ANALISIS / CONDENA_FIRME` | `resultadoFinal=CONDENA_FIRME`; comprobante | Permanece `PENDIENTE_ANALISIS / CONDENA_FIRME` | — | Infractor / operador |
| Confirmar pago de condena | `PENDIENTE_ANALISIS / CONDENA_FIRME` | Pago verificado; sin bloqueantes | `CERRADAS / CERRADA` | `CONDENA_FIRME` (cerrado por pago) | Tesorería / operador |
| Derivar a gestión externa | `PENDIENTE_ANALISIS / CONDENA_FIRME` | Sin pago; plazo vencido o decisión administrativa | `GESTION_EXTERNA / EN_GESTION_EXTERNA` | — | Operador administrativo |
| Reingresar desde gestión externa | `GESTION_EXTERNA / EN_GESTION_EXTERNA` | Resultado externo con efecto material | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Según resultado externo | Operador administrativo |
| Archivar expediente | Cualquier bandeja interna activa | Trámite resuelto/avanzado; bloqueante de cierre activo | `ARCHIVO / EN_ARCHIVO` | Sin cambio | Operador administrativo |
| Reingresar desde archivo | `ARCHIVO / EN_ARCHIVO` | Novedad con efecto material | `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Sin cambio | Operador administrativo |
| Paralizar | Cualquier bandeja interna activa | Causal fundada registrada | `PARALIZADAS / PARALIZADA` | Sin cambio | Operador administrativo |
| Reactivar (levantar paralización) | `PARALIZADAS / PARALIZADA` | Causal levantada; decisión formal | Bandeja correspondiente a situación real | Sin cambio | Operador administrativo |
| Generar resolutorio de bloqueante material | Cualquier bandeja interna operable | Bloqueante material identificado; decisión tomada | Permanece en bandeja actual | Sin cambio | Operador / jurídico |
| Firmar resolutorio de bloqueante (con circuito firma) | `PENDIENTE_FIRMA / PENDIENTE_FIRMA` | Resolutorio con circuito firma generado | Retorna a bandeja anterior | Sin cambio | Firmante autorizado |
| Registrar cumplimiento material efectivo | Bandeja con bloqueante activo | Resolutorio firmado o emitido; hecho material realizado | `ARCHIVO / EN_ARCHIVO` o `CERRADAS / CERRADA` | Sin cambio | Operador / mesa de entrada |
| Cerrar acta | `PENDIENTE_ANALISIS` u otra bandeja interna operable con resultado habilitante | `resultadoFinal` cerrable; sin bloqueantes | `CERRADAS / CERRADA` | Sin cambio | Operador administrativo |

---

## 7. Matriz por bandeja / estado

| Bandeja / estado | Cómo llega el acta | Qué representa | Acciones posibles | Acción lleva a | Acciones prohibidas | Condiciones / bloqueos |
|---|---|---|---|---|---|---|
| **`ACTAS_EN_ENRIQUECIMIENTO`** `LABRADA` / `EN_ENRIQUECIMIENTO` | Labrado inicial por inspector | Primer ingreso al circuito; completitud de datos y documentos iniciales | Completar datos · Generar piezas iniciales · Originar pago voluntario · Paralizar · Enviar a preparación documental | Piezas generadas → `PENDIENTE_PREPARACION_DOCUMENTAL` · Pago voluntario → `PENDIENTE_ANALISIS` | Dictar fallo · Cerrar sin revisión previa · Notificar sin firma | Permanece mientras falten datos mínimos o piezas requeridas |
| **`PENDIENTE_PREPARACION_DOCUMENTAL`** | Desde enriquecimiento cuando se identificaron las piezas a generar | Generación de piezas documentales necesarias antes de firma | Generar acta formal · Generar medida preventiva · Generar resolución no-fallo · Completar piezas · Paralizar | Piezas generadas → `PENDIENTE_FIRMA` | Firmar sin piezas generadas · Dictar fallo desde aquí · Notificar sin firma | Permanece hasta que todas las piezas necesarias estén generadas |
| **`PENDIENTE_FIRMA`** `PENDIENTE_FIRMA` | Desde cualquier bandeja cuando hay documentos generados pendientes de firma | Uno o más documentos del expediente esperan firma (acta, fallo, resolución, medida, resolutorio de bloqueante) | Firmar fallo condenatorio → `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO` · Firmar fallo absolutorio → `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO` · Firmar acta inicial → `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO` · Firmar resolutorio de bloqueante → retorna a bandeja previa · Paralizar | Según tipo de pieza firmada | Cerrar sin firma completa · Notificar sin firma | Si múltiples documentos requieren firma, permanece hasta completar el conjunto necesario |
| **`PENDIENTE_NOTIFICACION`** `PENDIENTE_ENVIO` | Desde `PENDIENTE_FIRMA` al completarse firmas requeridas sobre pieza notificable | Pieza lista para envío al canal; diligencia aún no iniciada | Iniciar diligencia → `EN_NOTIFICACION / EN_NOTIFICACION` · Cambiar canal · Paralizar | Iniciar diligencia → `EN_NOTIFICACION` | Cerrar sin notificar · Saltar a fallo sin notificar el anterior | Canal debe estar definido; pieza debe estar firmada |
| **`EN_NOTIFICACION`** `EN_NOTIFICACION` | Desde `PENDIENTE_NOTIFICACION` al iniciarse la diligencia | Notificación en trámite; se aguarda resultado, acuse o vencimiento | Registrar acuse positivo → `PENDIENTE_ANALISIS / PENDIENTE_REVISION` · Registrar acuse negativo → `PENDIENTE_ANALISIS / PENDIENTE_REVISION` · Registrar vencimiento → `PENDIENTE_ANALISIS / PENDIENTE_REVISION` · Reintentar · Dejar a decisión manual · Paralizar | Según resultado | Cerrar sin resultado · Saltar a fallo sin esperar resultado | Notificación electrónica: hasta 7 días · Fallo notificado: 5 días antes del destino natural |
| **`PENDIENTE_ANALISIS`** `PENDIENTE_REVISION` | Desde notificación (positiva, negativa o vencida) · reingreso de archivo · gestión externa · resolución de apelación · reactivación · pago voluntario | Expediente con actuación o novedad que requiere análisis y definición del próximo paso | Analizar expediente · Enviar a fallo → `PENDIENTES_FALLO / PENDIENTE_FALLO` · Enviar a resolución/redacción → `PENDIENTES_RESOLUCION_REDACCION` · Confirmar pago voluntario → `CERRADAS / CERRADA` · Registrar apelación (si `resultadoFinal=CONDENADO` en plazo) → `CON_APELACION / CON_APELACION` · Registrar vencimiento apelación → `PENDIENTE_ANALISIS / CONDENA_FIRME` · Archivar → `ARCHIVO / EN_ARCHIVO` · Cerrar (si condiciones se cumplen) → `CERRADAS / CERRADA` · Paralizar · Generar resolutorio de bloqueante | Según resultado del análisis | Cerrar sin `resultadoFinal` habilitante · Firmar desde aquí (debe ir a `PENDIENTE_FIRMA`) | Sin `resultadoFinal` habilitante no puede cerrarse |
| **`PENDIENTE_ANALISIS`** `CONDENA_FIRME` | Desde vencimiento de apelación sin apelación · desde resolución de apelación rechazada · desde consentimiento del infractor | Condena firme; infractor debe pagar o el expediente se deriva a gestión externa | Informar pago de condena → permanece aquí (sub: `CONDENA_PAGO_INFORMADO`) · Confirmar pago de condena → `CERRADAS / CERRADA` · Derivar a gestión externa → `GESTION_EXTERNA / EN_GESTION_EXTERNA` · Archivar si hay bloqueante → `ARCHIVO / EN_ARCHIVO` · Paralizar · Generar resolutorio de bloqueante | Según situación | Nueva apelación · Cerrar sin pago confirmado | No puede cerrarse sin pago de condena confirmado o resolución equivalente |
| **`PENDIENTES_RESOLUCION_REDACCION`** | Desde enriquecimiento, análisis o apelación cuando se necesita una pieza no-fallo | Producción de pieza administrativa no-fallo: resolución, nulidad, medida preventiva, rectificación | Redactar nulidad · Generar medida preventiva · Generar resolución · Generar rectificación · Paralizar — todo lleva a → `PENDIENTE_FIRMA / PENDIENTE_FIRMA` | Pieza generada → `PENDIENTE_FIRMA` | Dictar fallo (debe ir a `PENDIENTES_FALLO`) · Cerrar sin pieza generada | Permanece hasta que la pieza requerida esté generada |
| **`PENDIENTES_FALLO`** `PENDIENTE_FALLO` | Desde análisis (condición de fallo cumplida) · notificación (plazo vencido) · apelación con nuevo fallo | Expediente en condición concreta de requerir fallo; decisión judicial pendiente | Dictar fallo absolutorio → `PENDIENTE_FIRMA / PENDIENTE_FIRMA` · Dictar fallo condenatorio → `PENDIENTE_FIRMA / PENDIENTE_FIRMA` · Reencauzar a análisis · Paralizar | Fallo dictado → `PENDIENTE_FIRMA` | Cerrar sin fallo · Firmar desde aquí | Permanece hasta que el fallo esté generado |
| **`CON_APELACION`** `CON_APELACION` | Desde `PENDIENTE_ANALISIS / PENDIENTE_REVISION` cuando el infractor presenta apelación con `resultadoFinal=CONDENADO` | Expediente en tramo recursivo; apelación activa altera el recorrido | Resolver apelación rechazada → `PENDIENTE_ANALISIS / CONDENA_FIRME` · Resolver apelación con lugar → `PENDIENTES_FALLO` o `PENDIENTES_RESOLUCION_REDACCION` · Paralizar | Según resultado de apelación | Cerrar directamente · Derivar a gestión externa antes de resolver apelación | Permanece mientras la apelación no produzca decisión suficiente |
| **`GESTION_EXTERNA`** `EN_GESTION_EXTERNA` | Desde `PENDIENTE_ANALISIS / CONDENA_FIRME` por decisión de derivación | Expediente fuera del circuito interno; en trámite externo | Consultar/seguir la gestión externa · Registrar resultado externo si el prototipo lo contempla · Reingresar con resultado externo → `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Reingreso → `PENDIENTE_ANALISIS` (el cierre, archivo o cualquier acción interna se define en la bandeja correspondiente tras el reingreso) | Pago voluntario normal · Dictar fallo directamente · Notificación interna normal · Cerrar sin reingreso previo · Paralizar · Archivar por vía normal · Cumplimiento material interno · Acciones operativas internas normales | Permanece hasta que el resultado externo produzca efecto material |
| **`PARALIZADAS`** `PARALIZADA` | Desde cualquier bandeja interna al registrar causal fundada | Expediente detenido; la causal impide la continuidad normal del trámite | Visualizar motivo · Visualizar observación · Reactivar/levantar paralización → bandeja correspondiente según situación real | Reactivación → bandeja correspondiente según situación real al momento de levantar | Pago voluntario · Dictar fallo · Notificar · Firmar · Cerrar · Archivar · Derivar a gestión externa · Cumplimiento material · Avanzar el circuito sin levantar la paralización · Generar piezas procesales · Nueva paralización sin reactivar primero | Permanece mientras subsista la causal de paralización |
| **`ARCHIVO`** `EN_ARCHIVO` | Desde cualquier bandeja activa cuando el trámite está resuelto pero hay bloqueante | Expediente archivado operativamente; el trámite está resuelto o suficientemente avanzado pero subsiste al menos un bloqueante de cierre | Consultar detalle del expediente · Consultar historial y documentos · Reingresar al circuito (si `permiteReingreso=true`) → `PENDIENTE_ANALISIS / PENDIENTE_REVISION` | Reingreso → `PENDIENTE_ANALISIS` (la resolución de bloqueantes y el cierre se ejecutan en la bandeja correspondiente tras el reingreso) | Pago voluntario · Dictar fallo · Notificar piezas del trámite principal · Firmar · Cerrar directamente · Paralizar · Derivar a gestión externa · Registrar cumplimiento material · Cualquier acción interna normal del circuito | Permanece mientras subsista al menos un bloqueante de cierre. Presentación visual: mostrar `Archivada recuperable` si `permiteReingreso=true`; mostrar `Archivada` si no. No mostrar chips operativos como `Cerrable` o `Condena firme pendiente`. |
| **`CERRADAS`** `CERRADA` | Desde análisis, archivo, o cualquier bandeja con resultado habilitante y sin bloqueantes | Expediente concluido operativamente; final del recorrido | Visualizar expediente · Consultar trazabilidad · Reingreso excepcional (solo con habilitación formal) | Solo reingreso excepcional formal | Todo avance operativo normal · Nuevas notificaciones · Nuevos fallos | Estado terminal; sin reingreso ordinario |

---

## 8. Reglas transversales

### 8.1 Regla de bandeja

Las bandejas son una **proyección operativa** del estado real del expediente. La fuente de verdad es el expediente, sus eventos, documentos y notificaciones. Las bandejas no deben editarse manualmente como fuente primaria ni quedar desacopladas de la realidad del expediente. Son regenerables desde la fuente primaria.

### 8.2 Regla de transición

Una transición de bandeja debe ser siempre **consecuencia de un hecho del expediente**: validación, generación documental, firma, notificación, acuse, presentación, pago, reingreso, derivación, archivo o cierre. No existe transición por tiempo solo, ni por movimiento administrativo manual sin hecho registrado.

### 8.3 Regla de firma

El sistema de faltas no firma documentos por sí mismo. Solo refleja que un documento requiere firma y reacciona al resultado informado por el motor externo o por el modo sandbox. Un expediente no avanza a notificación si falta firma requerida. Si múltiples piezas requieren firma, el expediente permanece en `PENDIENTE_FIRMA` hasta completar el conjunto necesario para el siguiente paso.

### 8.4 Regla de notificación

La notificación es un proceso transversal único. Aplica a cualquier pieza notificable del expediente (acta, fallo, resolución, medida, liberación con circuito). El resultado de la notificación modifica la situación operativa del expediente.

Plazos especiales:
- Notificación electrónica: espera de hasta **7 días** antes de resolver resultado como vencido.
- Fallo notificado positivamente: espera de **5 días** antes del siguiente destino natural.

### 8.5 Regla de archivo vs. cerrada

- **Archivo** ≠ **Cerrada**.
- **Archivo**: trámite principal resuelto o suficientemente avanzado, pero subsiste al menos un bloqueante de cierre.
- **Cerrada**: expediente completamente concluido operativamente, sin ningún bloqueante de cierre.

Que el expediente esté resuelto no implica automáticamente que pueda cerrarse.

### 8.6 Regla de bloqueantes de cierre

Un expediente **no puede pasar a cerrada** mientras exista:
- Una o más medidas preventivas activas.
- Una o más liberaciones materiales pendientes (rodado, documentación, licencia).
- Cualquier otra causal operativa definida por catálogo que impida cierre.

### 8.7 Regla de pago voluntario

El pago voluntario es amplio, pero no absoluto. Está disponible mientras el expediente esté en circuito interno operable y antes de que el estado jurídico lo vuelva incompatible.

El infractor puede originar pago voluntario desde cualquier bandeja interna operable. Queda excluido en:

- `GESTION_EXTERNA` (estado externo, no operable internamente)
- `ARCHIVO` (estado archivado; requiere reingreso primero)
- `PARALIZADAS` (estado suspendido; requiere reactivación primero)
- `CERRADAS` (estado terminal)
- `estadoProcesoActual=CONDENA_FIRME` (en ese caso corresponde pago de condena, no pago voluntario)

Si ya existe condena firme, el pago que hace el infractor es pago de condena. El mecanismo operativo es idéntico (informar → confirmar → cerrar), pero el tipo y el campo `resultadoFinal` son distintos.

### 8.8 Regla de reingreso

El reingreso no crea un expediente nuevo ni borra historia previa. Se modela como un nuevo evento del expediente. La bandeja destino depende de la situación real y del resultado que motiva el reingreso; no es automática a una bandeja genérica.

### 8.9 Regla de paralización

La paralización no es sinónimo de inactividad ni de archivo. Es una situación excepcional y fundada que suspende el circuito. Debe tener trazabilidad suficiente y no puede usarse como espera informal sin causal documentada.

### 8.10 Regla de materiales

El documento de levantamiento/liberación/restitución **no equivale** por sí solo al cumplimiento material. El sistema distingue tres instancias:

1. Documento generado.
2. Documento firmado.
3. Hecho material efectivamente registrado.

Solo el tercer paso elimina el bloqueante de cierre.

### 8.11 Regla de `resultadoFinal`

El campo `resultadoFinal` sintetiza la disposición del expediente y condiciona las acciones habilitadas:

| Valor | Significado | Habilita cierre directo |
|---|---|---|
| `SIN_RESULTADO_FINAL` | Sin resolución aún | No |
| `ABSUELTO` | Fallo absolutorio notificado positivamente | Sí, si no hay bloqueantes materiales |
| `CONDENADO` | Fallo condenatorio notificado; plazo de apelación abierto | No (puede apelar aún) |
| `CONDENA_FIRME` | Plazo de apelación vencido, apelación rechazada, o consentimiento del infractor | Solo si pago de condena confirmado y sin bloqueantes |
| `PAGO_CONFIRMADO` | Pago voluntario verificado | Sí, si no hay bloqueantes materiales |

---

## 9. Registro de cambios de regla

**Instrucción operativa obligatoria**: antes de implementar cualquier cambio solicitado por Dirección, actualizar este documento en la sección correspondiente.

Actualizar este documento si cambia:

- Una acción permitida en alguna bandeja.
- Una acción prohibida en alguna bandeja.
- Una condición de entrada, permanencia o salida de cualquier bandeja.
- Se agrega un paso previo antes de una acción existente.
- Se elimina o simplifica un paso del circuito.
- Una acción debe llevar a otra bandeja distinta a la documentada aquí.
- El actor responsable de una acción.
- La regla de cierre.
- El tratamiento de pago, notificación, fallo, apelación, materiales, archivo o paralización.

Luego usar el cambio documentado para generar el slice técnico correspondiente.

| Fecha | Sección afectada | Cambio | Solicitado por | Motivo |
|---|---|---|---|---|
| 2026-06-11 | Todo | Versión inicial documentada | — | Creación del documento de referencia funcional |

---

## Sección A: Pago voluntario

### Quién puede originarlo
El infractor, desde el portal infractor (lectura de código QR o acceso directo), o un operador administrativo en nombre del infractor.

### Cuándo está habilitado
En cualquier bandeja interna operable: desde `ACTAS_EN_ENRIQUECIMIENTO` hasta `PENDIENTE_ANALISIS`, incluyendo `PENDIENTES_FALLO`, `PENDIENTE_FIRMA`, `PENDIENTE_NOTIFICACION`, `EN_NOTIFICACION`, `PENDIENTES_RESOLUCION_REDACCION`, `CON_APELACION`.

**No está habilitado en**: `GESTION_EXTERNA`, `ARCHIVO`, `PARALIZADAS`, `CERRADAS`, ni cuando `estadoProcesoActual=CONDENA_FIRME` (en ese caso el pago es pago de condena, no pago voluntario).

El pago voluntario es amplio, pero no absoluto. Está disponible mientras el expediente esté en circuito interno operable y antes de que el estado jurídico lo vuelva incompatible.

### Flujo del pago voluntario

1. Infractor solicita acogerse a pago voluntario → expediente pasa o se mantiene en `PENDIENTE_ANALISIS / PENDIENTE_REVISION` con sub-bandeja `ANALISIS_PAGO_SOLICITADO`.
2. Infractor informa pago realizado con comprobante → sub-bandeja `ANALISIS_PAGO_INFORMADO`.
3. Tesorería u operador autorizado confirma el pago:
   - Sin bloqueantes de cierre: → `CERRADAS / CERRADA` con `resultadoFinal=PAGO_CONFIRMADO`.
   - Con bloqueantes de cierre activos: → `ARCHIVO / EN_ARCHIVO`.
4. Si el pago resulta insuficiente u observado → sub-bandeja `CONDENA_PAGO_OBSERVADO`; se requiere nueva actuación.

### Regla sobre condena firme
Si el expediente ya tiene `resultadoFinal=CONDENA_FIRME`, el pago que hace el infractor es pago de condena (no pago voluntario). El mecanismo es idéntico: informar → confirmar → cerrar.

### Regla sobre bloqueantes
El pago confirmado no cierra el expediente si existen bloqueantes de cierre activos. El expediente va a `ARCHIVO` hasta resolver el bloqueante.

---

## Sección B: Fallo, condena y apelación

### Cuándo se dicta fallo
El expediente llega a `PENDIENTES_FALLO` cuando ya existe una condición concreta:
- Notificación del acta con plazo vencido sin comparecencia ni pago suficiente.
- Reingreso desde gestión externa con necesidad de nuevo fallo.
- Resultado de apelación que requiere nuevo dictado.
- Decisión administrativa directa de encauzar a fallo.

### Tipos de fallo

| Tipo | Documento | Efecto tras notificación positiva |
|---|---|---|
| **Absolutorio** | `FALLO_ABSOLUTORIO` | `resultadoFinal=ABSUELTO`; habilita cierre si no hay bloqueantes |
| **Condenatorio** | `FALLO_CONDENATORIO` | `resultadoFinal=CONDENADO`; abre plazo de apelación |

### Circuito del fallo

1. Fallo dictado en `PENDIENTES_FALLO` → documento en estado `PENDIENTE_FIRMA`.
2. Expediente pasa a `PENDIENTE_FIRMA`.
3. Fallo firmado → `PENDIENTE_NOTIFICACION / PENDIENTE_ENVIO`.
4. Diligencia de notificación iniciada → `EN_NOTIFICACION`.
5. Acuse positivo del fallo → `PENDIENTE_ANALISIS / PENDIENTE_REVISION` con `resultadoFinal` actualizado.
6. Si fallo condenatorio: plazo de apelación abierto (5 días de referencia post notificación positiva).

### Plazo de apelación
Mientras `resultadoFinal=CONDENADO` y el plazo no venció, es posible registrar apelación. Al vencer sin apelación: "Registrar vencimiento de apelación" → `resultadoFinal=CONDENA_FIRME`.

### Apelación
- Solo existe cuando el infractor la presenta efectivamente.
- No es paso obligatorio ni lineal.
- Al presentarse: `CON_APELACION / CON_APELACION`.
- Al resolverse: rechazada → `CONDENA_FIRME`; con lugar → nuevo fallo o nueva pieza.
- El infractor puede consentir la condena desde el portal sin esperar el vencimiento del plazo. Efecto: `resultadoFinal=CONDENA_FIRME`.

### Condena firme
`resultadoFinal=CONDENA_FIRME` habilita dos caminos:
1. Infractor paga → informar → confirmar → cerrar.
2. Infractor no paga → derivar a gestión externa (apremio, Juzgado de Paz, otro).

---

## Sección C: Materiales y bloqueantes de cierre

### Tipos de bloqueantes de cierre

| Bloqueante | Pieza de origen | Documento de resolución | Requiere cumplimiento material |
|---|---|---|---|
| Medida preventiva activa | Acto/pieza de medida preventiva | Levantamiento de medida preventiva | Sí |
| Rodado secuestrado | Acta de retención de vehículo | Liberación de rodado | Sí (entrega física) |
| Documentación retenida | Constatación de retención documental | Restitución de documentación | Sí (entrega material) |
| Otros pendientes materiales | Según catálogo operativo vigente | Resolutorio correspondiente | Según tipo |

### Circuito de resolución de bloqueante

1. Identificar el bloqueante activo.
2. Generar resolutorio de bloqueante desde la bandeja actual (cualquier bandeja interna operable):
   - Con firma directa (sin notificación): va a `PENDIENTE_FIRMA`, se firma, retorna a la bandeja previa.
   - Con circuito completo firma y notificación (`DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF`): sigue firma → notificación.
3. Firma del resolutorio.
4. Registrar el cumplimiento material efectivo.
5. Bloqueante eliminado del expediente.
6. Si ya no quedan bloqueantes: habilitar cierre.

### Regla clave
El documento firmado **no equivale** al cumplimiento material. Solo el hecho material registrado elimina el bloqueante y habilita el cierre.

### Efecto en archivo y cierre

| Situación | Destino |
|---|---|
| Trámite resuelto + bloqueante activo | `ARCHIVO` |
| Trámite resuelto + bloqueante resuelto materialmente | `CERRADAS` (si no quedan otros bloqueantes) |
| En `ARCHIVO` + se resuelve el último bloqueante | Puede pasar a `CERRADAS` |

---

## Sección D: Notificaciones

### Alcance
Transversal a todo el expediente. Aplica a: acta, fallo absolutorio, fallo condenatorio, resolución, medida preventiva, documento de liberación con circuito, y otras piezas notificables según catálogo.

### Canales previstos
Domicilio electrónico · Email · Postal / carta documento · Bluemail · Notificador municipal · Portal ciudadano · Otro canal formal.

### Estados internos de una notificación
`PENDIENTE_ENVIO` → `EN_NOTIFICACION` → resultado: `ACUSE_POSITIVO` | `ACUSE_NEGATIVO` | `VENCIDA_SIN_ACUSE` → decisión sobre siguiente paso del expediente.

### Plazos especiales

| Canal / Situación | Plazo |
|---|---|
| Domicilio electrónico | Hasta **7 días** de espera antes de considerar vencida |
| Fallo notificado positivamente | **5 días** antes del siguiente destino natural |

### Efecto del resultado de notificación sobre el expediente

| Resultado | Pieza notificada | Efecto sobre el expediente |
|---|---|---|
| Acuse positivo | Acta | `PENDIENTE_ANALISIS / PENDIENTE_REVISION`; abre análisis |
| Acuse negativo / vencida | Acta | `PENDIENTE_ANALISIS / PENDIENTE_REVISION`; se evalúa reintento u otro canal de notificación; no habilita tratamiento de fondo como notificación positiva |
| Acuse positivo | Fallo absolutorio | `resultadoFinal=ABSUELTO`; habilita cierre si sin bloqueantes |
| Acuse positivo | Fallo condenatorio | `resultadoFinal=CONDENADO`; abre plazo de apelación |
| Acuse negativo / vencida | Fallo (cualquier tipo) | Requiere decisión: reintento, reencauce o gestión externa |

### Notificador municipal
Puede registrar acuse en el mismo acto con firma dibujada en dispositivo y evidencia complementaria. El resultado queda trazado en el intento.

---

## Sección E: Portal infractor

### Qué puede hacer el infractor desde el portal

| Acción | Condición de habilitación | Efecto |
|---|---|---|
| Consultar estado del expediente | Expediente existente | Solo consulta |
| Solicitar acogerse a pago voluntario | Bandeja interna operable; acta no cerrada | Sub-bandeja `ANALISIS_PAGO_SOLICITADO` |
| Informar pago realizado | Pago voluntario solicitado; comprobante disponible | Sub-bandeja `ANALISIS_PAGO_INFORMADO` |
| Registrar apelación | `resultadoFinal=CONDENADO`; dentro del plazo; sin apelación activa | `CON_APELACION / CON_APELACION` |
| Consentir condena voluntariamente | `resultadoFinal=CONDENADO`; sin apelación pendiente | `resultadoFinal=CONDENA_FIRME`; igual que vencimiento sin apelación |

### Regla de consentimiento de condena
El consentimiento queda trazado como evento con origen `PORTAL_INFRACTOR`. El sistema no expone esta opción si ya existe apelación pendiente activa.

### Regla de consulta en portal
El portal solo expone información y acciones coherentes con el estado actual. Ejemplo: tipo de pago de condena solo visible si `resultadoFinal=CONDENA_FIRME`.

---

## Sección F: Gestión externa

### Cuándo se deriva
Cuando el expediente tiene `resultadoFinal=CONDENA_FIRME` y el infractor no paga en el plazo operativo, o por decisión administrativa formal de derivación.

### Destinos externos previstos
Apremio · Juzgado de Paz · Otro ente externo formal admitido.

### Qué puede ocurrir durante la gestión externa

| Resultado | Acción al reingresar |
|---|---|
| Pago externo realizado | Reingreso → `PENDIENTE_ANALISIS`; puede habilitar cierre |
| Necesidad de nuevo fallo | Reingreso → `PENDIENTE_ANALISIS` → `PENDIENTES_FALLO` |
| Necesidad de rectificación | Reingreso → `PENDIENTE_ANALISIS` → `PENDIENTES_RESOLUCION_REDACCION` |
| Sin novedad material | Permanece en `GESTION_EXTERNA` |

### Reingreso desde gestión externa
Al recibir resultado con efecto material: reingreso → `PENDIENTE_ANALISIS / PENDIENTE_REVISION`. El expediente conserva identidad, historial, documentos y notificaciones previas.

### Acciones no habilitadas en gestión externa
Pago voluntario normal · Dictar fallo directamente · Notificación interna normal · Cerrar sin reingreso previo · Paralizar · Archivar por vía normal · Cumplimiento material interno · Ejecutar acciones operativas internas del circuito principal.

Si el resultado externo habilita cierre o archivo, primero debe existir reingreso formal al circuito; luego se define la acción en la bandeja correspondiente.

---

## Sección G: Archivo

### Cuándo se archiva
El expediente va a archivo cuando:
- El trámite principal ya está resuelto o suficientemente avanzado.
- Persiste al menos un bloqueante de cierre activo.

### Ejemplos típicos

| Situación | Bloqueante |
|---|---|
| Multa paga | Medida preventiva activa |
| Fallo absolutorio notificado | Rodado secuestrado pendiente de liberación |
| Fallo condenatorio + pago confirmado | Documentación retenida pendiente de restitución |
| Nulidad dictada y notificada | Otro pendiente material por catálogo |

### Qué puede ocurrir estando en archivo
Solo están habilitadas las siguientes acciones:

- Consultar detalle del expediente.
- Consultar historial y documentos.
- Reingresar al circuito operativo si `permiteReingreso=true` → `PENDIENTE_ANALISIS / PENDIENTE_REVISION`.

La resolución de bloqueantes (levantamiento de medida, liberación, restitución material) y el cierre no se ejecutan desde `ARCHIVO`. Primero debe reingresar el acta al circuito; luego se procesa la novedad en la bandeja que corresponda.

### Acciones no habilitadas en archivo
Pago voluntario · Dictar fallo · Notificar piezas del trámite principal · Firmar · Cerrar directamente · Paralizar · Derivar a gestión externa · Registrar cumplimiento material · Registrar levantamiento de medida · Registrar liberación/restitución · Cualquier acción interna del circuito normal.

---

## Sección H: Paralización

### Cuándo se paraliza
Cuando existe una causal operativa fundada y documentada que impide que el expediente continúe su curso normal. Ejemplos: espera de documentación externa; trámite administrativo no imputable al expediente; causa administrativa formal.

### Lo que paralización no es
No es espera pasiva informal · No equivale a archivo · No cierra el expediente.

### Qué puede ocurrir estando paralizada
Solo se puede visualizar el motivo y ejecutar la reactivación. No aplican acciones operativas normales: no se puede dictar fallo, notificar, generar piezas procesales ni cerrar.

### Reactivación
Al levantar la paralización, el expediente se redirige a la bandeja que corresponde según su situación real en el momento de la reactivación. El destino no es automático a una bandeja fija.

---

## Sección I: Cierre

### Condiciones necesarias para cerrar (todas deben cumplirse)

1. `resultadoFinal` en valor habilitante: `ABSUELTO`, `PAGO_CONFIRMADO`, o `CONDENA_FIRME` con pago de condena confirmado.
2. Sin medidas preventivas activas.
3. Sin liberaciones materiales pendientes.
4. Sin restituciones de documentación pendientes.
5. Sin ningún otro bloqueante de cierre según catálogo vigente.

### Tipos de cierre y sub-bandejas destino

| Tipo de cierre | Sub-bandeja en `CERRADAS` | `resultadoFinal` requerido |
|---|---|---|
| Pago voluntario confirmado | `CERRADA_PAGO_VOLUNTARIO` | `PAGO_CONFIRMADO` |
| Pago de condena confirmado | `CERRADA_PAGO_CONDENA` | `CONDENA_FIRME` |
| Absolución sin bloqueantes | `CERRADA_ABSOLUCION` | `ABSUELTO` |
| Nulidad sin bloqueantes | `CERRADA_NULIDAD` | Variable (según decisión jurídica) |
| Cierre posterior a reingreso desde archivo | `CERRADA_ARCHIVO_DEFINITIVO` | Variable |
| Otra causa válida | `CERRADA_OTRA_CAUSA` | Según catálogo operativo |

### Regla de cierre por tramos
El expediente puede cerrar directamente desde cualquier bandeja activa si se cumplen simultáneamente todas las condiciones. No es obligatorio pasar por `ARCHIVO` antes de `CERRADAS`.

### Reingreso desde cerradas
`CERRADAS` es estado terminal. Solo admite reingreso excepcional cuando una regla formal válida lo habilite expresamente. El reingreso debe ser trazable y documentado.
