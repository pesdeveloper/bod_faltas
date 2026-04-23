# ESTADO ACTUAL Y PRÓXIMO PASO — PROTOTIPO BACKEND FALTAS

## Propósito del archivo

Este archivo registra el **estado operativo vigente** del prototipo backend.

No es fuente de verdad de dominio.  
No reemplaza la `spec/`.  
No reemplaza el `prompt-de-reanudacion-chat.md`.

Su función es dejar visible, de forma compacta y práctica:

- qué capacidades reales tiene hoy el prototipo
- qué decisiones ya quedaron consolidadas
- qué casos demo existen
- qué falta realmente
- cuál es el próximo paso natural

---

## 1. Estado actual resumido

El prototipo backend ya cubre un conjunto funcional importante del circuito demo.

### Capacidades hoy consolidadas

- piezas múltiples por expediente
- firma individual por documento
- salida correcta desde `PENDIENTE_FIRMA` solo cuando todas las piezas firmables están firmadas
- nulidad post-firma como salida terminal invalidante a `CERRADAS`
- notificación con variantes mínimas útiles para demo
- archivo con reingreso explícito
- gestión externa con derivación y retorno
- cierre desde análisis sujeto a regla de cerrabilidad
- pago voluntario temprano
- pago informado
- comprobante mock
- pago pendiente de confirmación
- pago confirmado
- pago observado
- resultado final compatible con cierre:
  - `ABSUELTO`
  - `PAGO_CONFIRMADO`
- cerrabilidad unificada
- bloqueantes materiales/documentales
- separación entre:
  - origen material
  - resolutorio documental
  - cumplimiento material efectivo
- vista de hechos materiales separada de la lectura de cerrabilidad
- condiciones materiales tempranas desde `D1` / `D2`
- recorrido demo reproducible de punta a punta

---

## 2. Decisiones funcionales y estructurales ya cerradas

### 2.1 Firma

- el circuito viejo `pasar-a-notificacion` ya no es la verdad del sistema
- la verdad vigente es la firma por documento
- la operación central es `firmarDocumento(actaId, documentoId)`
- la acta solo sale de `PENDIENTE_FIRMA` cuando todas las piezas firmables quedaron en `FIRMADO`

### 2.2 Nulidad

- la nulidad se modela como **pieza no-fallo**
- no existe bandeja terminal `NULAS`
- si el último documento firmado es de tipo `NULIDAD`, el expediente pasa a `CERRADAS`
- nulidad post-firma **no** entra al circuito común de notificación
- la nulidad post-firma es una salida terminal invalidante dentro del prototipo

### 2.3 Pago

El prototipo distingue entre:

- solicitud de pago voluntario
- pago informado
- comprobante adjunto mock
- pendiente de confirmación
- pago confirmado
- pago observado

Reglas vigentes:

- informar pago no equivale a pago confirmado
- adjuntar comprobante no equivale a pago confirmado
- la confirmación mock del pago produce `resultadoFinal = PAGO_CONFIRMADO`
- `PAGO_CONFIRMADO` **no** cierra automáticamente

### 2.4 Cerrabilidad

La regla consolidada es:

**Cerrable = (`ABSUELTO` o `PAGO_CONFIRMADO`) y sin pendientes materiales/documentales activos**

Eso implica:

- ni `ABSUELTO`
- ni `PAGO_CONFIRMADO`

cierran por sí solos el expediente.

El cierre sigue siendo una acción posterior y explícita.

### 2.5 Bloqueantes materiales/documentales

Bloqueantes mínimos vigentes:

- `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- `LIBERACION_RODADO`
- `ENTREGA_DOCUMENTACION`

Reglas vigentes:

- el resolutorio documental no basta por sí solo
- debe existir cumplimiento material efectivo
- el expediente no queda cerrable mientras exista al menos un bloqueante activo

### 2.6 Hechos materiales

El prototipo ya separa mejor:

- plano documental del expediente
- plano de hechos materiales
- plano de cerrabilidad

En la lectura del detalle de acta ya existe una vista separada de `hechosMateriales`, independiente de la vista de `cerrabilidad`.

### 2.7 Condiciones materiales tempranas

Ya existe soporte mínimo para registrar desde etapas tempranas (`D1` / `D2`):

- secuestro / retención de rodado
- retención documental
- medida preventiva aplicable

Esto permite que más circuitos nazcan operativamente desde etapas tempranas y no solo desde seeds precargados.

### 2.8 Archivo y reingreso

- existe macro-bandeja `ARCHIVO`
- existe motivo de archivo
- existe reingreso explícito
- el reingreso devuelve a `PENDIENTE_ANALISIS`
- queda marca operativa de revisión post reingreso

### 2.9 Gestión externa

- existe macro-bandeja `GESTION_EXTERNA`
- existen tipos mínimos útiles:
  - `APREMIO`
  - `JUZGADO_DE_PAZ`
- existe derivación
- existe retorno a análisis
- existe re-derivación

---

## 3. Acciones y lecturas hoy vigentes

## 3.1 Lecturas principales

El prototipo permite como mínimo:

- listar actas
- consultar detalle de una acta
- ver bloque actual, bandeja, situación operativa y acción pendiente
- ver estado documental y piezas
- ver cerrabilidad
- ver bloqueantes de cierre
- ver hechos materiales
- ver eventos relevantes del caso demo

## 3.2 Acciones principales hoy disponibles

### Documentales / resolutivas

- generar piezas/documentos según el circuito demo
- firmar documento individual
- generar nulidad en el caso demo correspondiente

### Notificación

- registrar resultado de notificación
- contemplar positivo / negativo / vencido / reintentos según el caso demo

### Archivo / reingreso

- archivar
- reingresar desde archivo

### Gestión externa

- derivar a gestión externa
- retornar a análisis
- re-derivar

### Pago

- solicitar pago voluntario
- registrar pago informado
- adjuntar comprobante mock
- confirmar pago informado
- observar pago informado

### Cerrabilidad / bloqueantes

- consultar cerrabilidad
- registrar resolutorio documental de bloqueante
- registrar cumplimiento material de bloqueante
- cerrar expediente si ya está en condición de cierre

### Acciones tempranas

- registrar constatación material temprana:
  - `SECUESTRO_RODADO`
  - `RETENCION_DOCUMENTAL`
  - `MEDIDA_PREVENTIVA_APLICABLE`

### Utilidad demo

- reset del dataset demo

> Nota: los nombres exactos de rutas/URIs pueden evolucionar.  
> La verdad inmediata está en el controller actual del prototipo.

---

## 4. Casos demo vigentes relevantes

## 4.1 `ACTA-0012` — nulidad post-firma

Caso demo útil para validar:

- generación de nulidad
- firma de documento `NULIDAD`
- salida terminal a `CERRADAS`
- no paso por notificación común

## 4.2 `ACTA-0015` — gestión externa

Caso demo útil para validar:

- derivación a gestión externa
- retorno a análisis
- re-derivación

## 4.3 `ACTA-0018` — pago confirmado sin bloqueantes

Caso demo útil para validar:

- flujo de pago sin complejidad material adicional
- recorrido económico simple dentro del prototipo

## 4.4 `ACTA-0019` — absolución + bloqueantes

Caso demo útil para validar:

- `resultadoFinal = ABSUELTO`
- presencia de bloqueantes materiales
- no cerrabilidad mientras existan
- resolutorio documental
- cumplimiento material
- paso a cerrable
- cierre posterior

## 4.5 `ACTA-0021` — pago confirmado precargado + bloqueantes

Caso demo útil para validar:

- `resultadoFinal = PAGO_CONFIRMADO`
- bloqueantes materiales
- cerrabilidad material
- sin depender del flujo real de pago

## 4.6 `ACTA-0022` — flujo real de pago + bloqueantes

Caso demo útil para validar:

- solicitud de pago voluntario
- pago informado
- comprobante
- confirmación de pago
- entrada efectiva a `PAGO_CONFIRMADO`
- bloqueantes materiales
- resolutorio + cumplimiento material
- cerrabilidad y cierre posterior

## 4.7 `ACTA-0023` — foco en vista de hechos materiales

Caso demo útil para validar:

- separación de lectura entre expediente documental y hecho material
- resolutorio en expediente sin hecho material cumplido
- lectura fina del eje material

## 4.8 `ACTA-0024` — demo reproducible punta a punta desde etapa temprana

Caso demo útil para validar:

- constataciones materiales tempranas
- paso por análisis
- circuito de pago
- cerrabilidad
- bloqueantes
- resolutorio
- cumplimiento material
- cierre final

Es hoy el mejor caso de recorrido integrado de punta a punta.

---

## 5. Recorrido demo integrado hoy disponible

El recorrido más completo y reproducible es el de `ACTA-0024`.

Secuencia funcional esperable:

1. reset del dataset demo
2. registrar constataciones materiales tempranas
3. avanzar hasta el punto de análisis aplicable
4. solicitar pago voluntario
5. registrar pago informado
6. adjuntar comprobante mock
7. confirmar pago
8. verificar que el expediente **no** sea cerrable si persisten bloqueantes
9. registrar resolutorio documental por cada eje bloqueante
10. registrar cumplimiento material efectivo por cada eje
11. verificar `cerrable = true`
12. ejecutar cierre final

Este recorrido ya tiene soporte tanto documental como de test/recorrido reproducible.

---

## 6. Refactors / limpiezas ya consolidadas

- eliminado el circuito conceptual viejo basado en “pasar a notificación” como verdad única
- consolidada firma individual por documento
- consolidado comportamiento correcto de nulidad post-firma
- unificada regla de cerrabilidad para:
  - `ABSUELTO`
  - `PAGO_CONFIRMADO`
- eliminado el supuesto falso de cierre automático por pago
- eliminada la idea de que el resolutorio documental basta por sí solo
- reducido el peso del reconocimiento manual de bloqueantes como requisito para existencia del bloqueo
- reforzada la lectura separada entre expediente documental y hecho material

---

## 7. Qué falta realmente

Lo pendiente real hoy, de forma resumida, es:

### 7.1 Profundizar etapas tempranas

- mejorar aún más `D1` / `D2`
- acercar más condiciones y acciones tempranas al caso real
- reforzar cómo nacen operativamente algunos circuitos desde labrado/enriquecimiento

### 7.2 Ampliar fidelidad documental/operativa

- enriquecer algunos circuitos documentales aún simplificados
- seguir conectando condiciones tempranas con actuaciones posteriores

### 7.3 Seguir endureciendo recorridos demo

- más tests/recorridos reproducibles
- más guardarraíles sobre regresiones

### 7.4 No pendiente inmediato

No están en foco inmediato:

- seguridad real
- base de datos real
- integraciones reales
- motor documental real
- PDFs reales
- firma digital real
- tesorería real

---

## 8. Riesgos / bordes conocidos

- el prototipo sigue siendo in-memory y demo-oriented
- varias representaciones siguen simplificadas para favorecer navegabilidad y velocidad de iteración
- no todo circuito temprano está todavía bajado con la misma profundidad
- algunos nombres, seeds y ayudas demo podrían seguir requiriendo limpieza fina futura

---

## 9. Próximo paso natural

El próximo paso natural, después de este estado consolidado, es **seguir profundizando etapas tempranas (`D1` / `D2`) con acciones operativas mínimas y coherentes**, reforzando la continuidad entre:

- condiciones iniciales del caso
- actuaciones tempranas
- consecuencias posteriores
- cerrabilidad final

La prioridad no debería ser abrir frentes técnicos grandes, sino seguir cerrando huecos operativos reales del prototipo con slices chicos y claros.

---

## 10. Regla operativa para continuar

Para próximos slices:

- usar `spec/` como fuente principal de dominio
- usar este archivo como inventario vivo
- usar `prompt-de-reanudacion-chat.md` como marco metodológico
- mantener slices mínimos
- no mezclar problemas conceptuales distintos
- tocar la menor cantidad posible de archivos
- no tocar archivos de continuidad salvo autorización explícita