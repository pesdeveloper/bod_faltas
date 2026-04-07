# [P1] PROCESO DOCUMENTAL FORMAL

---

## Finalidad del proceso

Este proceso transversal tiene como objetivo generar un **documento formal del sistema**, asegurando:

- generación del documento base
- asignación de numeración (si corresponde)
- autorización de pre-numeración cuando aplique
- firma mediante sistema externo
- obtención de documento final inmutable
- trazabilidad mínima

Este proceso es reutilizable desde distintos bloques del sistema:

- D3 (notificación)
- D4 (liberación, constancias)
- D5 (resoluciones, fallos)
- otros documentos administrativos

---

## Regla transversal

La lógica documental es única y reutilizable.

Ningún bloque funcional debe reimplementar:

- numeración
- autorización
- firma

Todos deben utilizar este proceso.

---

## Entrada del proceso

El proceso recibe un **contexto documental**, que incluye:

- OrigenProceso (ej: D3, D4, D5)
- IdActa
- TipoDocumento
- RequiereDocumento (bool)
- RequiereFirma (bool)
- RequiereNumeracion (bool)
- ModalidadNumeracion:
  - NUMERACION_EN_FIRMA
  - PRE_NUMERACION
- RequiereAutorizacionPreNumeracion (bool)
- FirmanteEsperado (rol)
- PlantillaDocumento / datos base

---

## Regla clave de entrada

Si:

RequiereDocumento = false

→ no se ejecuta el proceso  
→ salida inmediata según origen  

---

## Salidas del proceso

Las salidas son explícitas y dependen del resultado:

### Documento generado correctamente
→ DocumentoFormalListo  
→ volver al OrigenProceso  

---

### Documento pendiente de firma
→ DocumentoPendienteFirma  
→ volver al OrigenProceso  

---

### Error en autorización
→ ErrorAutorizacion  
→ volver al OrigenProceso  

---

### Error en firma
→ ErrorFirma  
→ volver al OrigenProceso  

---

### No aplica documento
→ SinDocumento  
→ volver al OrigenProceso  

---

## Flujo del proceso

---

### 1. Evaluar si requiere documento

Si no requiere:

→ salida: SinDocumento  
→ volver al origen  

---

### 2. Generar documento borrador

- se construye el documento
- aún no es formal
- no es inmutable

---

### 3. Evaluar numeración

Si RequiereNumeracion = false:

→ continuar sin numerar  

Si RequiereNumeracion = true:

→ evaluar modalidad  

---

### 4. Modalidad de numeración

---

#### A. Numeración en firma

- no se asigna número aún
- se asignará al firmar

---

#### B. Pre-numeración

---

##### ¿Requiere autorización?

Si sí:

→ verificar rol del operador  

Si no tiene rol:

→ solicitar autorización (PIN / superior)

---

##### Resultado

Si autorización rechazada:

→ salida: ErrorAutorizacion  
→ volver al origen  

Si autorizada:

→ asignar número de talonario  

---

## Regla

Si el documento ya tiene número asignado:

→ la firma no debe reasignarlo  

---

### 5. Evaluar firma

Si RequiereFirma = false:

→ documento final sin firma  
→ salida: DocumentoFormalListo  

---

### 6. Enviar a firmador

- se envía documento
- proceso asincrónico

---

### 7. Esperar resultado de firma

---

#### Firma exitosa

- documento queda firmado
- si numeración era en firma → se asigna número
- documento pasa a ser inmutable

→ salida: DocumentoFormalListo  

---

#### Firma fallida

→ salida: ErrorFirma  

---

#### Firma pendiente

→ salida: DocumentoPendienteFirma  

---

## Reglas adicionales

---

### Inmutabilidad

Un documento firmado:

- no puede modificarse
- no puede regenerarse sin crear uno nuevo

---

### Reutilización

Un documento ya generado puede reutilizarse si:

- no cambió su contenido
- sigue siendo válido

---

### Reintentos

En caso de error:

- se puede reintentar firma
- no se debe regenerar documento sin necesidad

---

## Relación con el sistema

---

### DocumentoActa

Debe registrar:

- tipo
- estado
- numeración
- firma
- firmante
- storage

---

### ActaEvento

No se generan eventos internos del proceso salvo que el negocio lo requiera.

---

## Origen y retorno

El proceso siempre debe:

- conocer su OrigenProceso
- devolver control explícitamente a ese origen

Ejemplo:

- D3 → generar documento de notificación → volver a D3
- D4 → generar acta de liberación → volver a D4
- D5 → generar fallo → volver a D5

---

## Regla clave

Este proceso no decide el flujo del negocio.

Solo produce documentos formales.

---

## Resumen

P1 centraliza la lógica de generación documental, numeración, autorización y firma, permitiendo que el resto del sistema reutilice este comportamiento sin duplicar lógica.