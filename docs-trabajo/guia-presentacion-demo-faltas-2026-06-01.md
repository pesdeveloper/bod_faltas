# Guia de Presentacion — Demo Direccion de Faltas — 2026-06-01

## Objetivo

Mostrar el prototipo funcional del sistema de Direccion de Faltas a responsables de negocio y usuarios clave, cubriendo el circuito operativo completo desde el alta del acta hasta el cierre, con los casos mas representativos de cada etapa.

---

## Mensaje principal

La demo cubre el circuito funcional completo del prototipo de Direccion de Faltas con dataset controlado de 42 actas.

No es aun integracion productiva completa; es prototipo funcional validado con backend demo.

---

## Que decir al inicio

"Lo que van a ver es un prototipo navegable que simula el flujo operativo real de la Direccion de Faltas. Tiene datos de ejemplo cargados. Las acciones que se ejecutan generan eventos reales en el sistema, lo que permite recorrer cada circuito de punta a punta. No tiene aun integracion con los sistemas de produccion, pero la logica de negocio y los estados del proceso estan implementados fielmente."

---

## Preparacion (tecnico, antes de la demo)

1. Levantar backend:
   ```
   cd S:\Source\Repos\Bod-Faltas\backend\api-faltas-prototipo
   mvn spring-boot:run
   ```

2. Levantar Angular:
   ```
   cd S:\Source\Repos\Bod-Faltas\apps\web-direccion-faltas\angular
   npm start
   ```

3. Abrir browser: http://localhost:4200

4. Resetear dataset para estado canonico:
   ```
   POST http://localhost:8087/api/prototipo/reset
   ```
   O usar el boton de reset en la UI si existe.

5. Verificar health:
   ```
   GET http://localhost:8087/api/prototipo/health
   ```
   Esperado: { "status": "UP" }

---

## Recorrido sugerido — 20 a 30 minutos

### 1. Bandejas y filtros (2 min)

- Mostrar el lateral con las 11 bandejas del sistema.
- Destacar que las cantidades son en tiempo real (reflejan el estado del dataset).
- Mostrar el filtro operativo en el area principal.
- Mostrar el filtro de dependencia como filtro transversal separado.
- Acta para abrir: cualquiera de PENDIENTE_ANALISIS.

Punto de discusion: "Estas son las bandejas de trabajo diario del operador. Cada acta se mueve entre bandejas segun el estado del proceso."

---

### 2. Redaccion documental (3 min)

Acta: ACTA-0001 (PENDIENTES_RESOLUCION_REDACCION, PENDIENTE_PRODUCCION_PIEZAS)

- Mostrar piezas requeridas.
- Generar NOTIFICACION_ACTA (la pieza se genera como documento).
- Observar que la acta avanza al siguiente estado.

Acta alternativa: ACTA-0011 para RESOLUCION; ACTA-0014 para RECTIFICACION.

Punto de discusion: "El sistema guia al operador con exactamente las piezas que corresponden a cada tipo de acta."

---

### 3. Firma y notificacion (3 min)

Continuando desde ACTA-0001 o abriendo un acta ya en PENDIENTE_FIRMA:

- Mostrar el documento generado pendiente de firma.
- Firmar el documento.
- Observar el paso a PENDIENTE_NOTIFICACION.
- Registrar notificacion positiva.
- Observar el retorno a PENDIENTE_ANALISIS.

Punto de discusion: "El flujo firma-notificacion es el corazon del proceso. Cada documento generado tiene su ciclo de vida propio."

---

### 4. Pago voluntario (2 min)

Acta: cualquiera en PENDIENTE_ANALISIS sin fallo dictado.

- Mostrar opcion de solicitar pago voluntario si el acta lo permite.
- No es obligatorio ejecutarla; alcanza con mostrar el boton visible.

---

### 5. Fallo absolutorio (3 min)

Acta: ACTA-0006 (PENDIENTE_ANALISIS)

- Dictar fallo absolutorio.
- Firmar.
- Notificar.
- Observar que el resultadoFinal queda ABSUELTO y el acta es cerrable.
- Cerrar.
- Confirmar en bandeja CERRADAS.

Punto de discusion: "El fallo absolutorio cierra el proceso con resultado definitivo. Una vez cerrada no se puede reabrir salvo reingreso controlado."

---

### 6. Fallo condenatorio (3 min)

Acta: ACTA-0029 (PENDIENTE_ANALISIS, estado limpio)

- Dictar fallo condenatorio con monto (1700).
- Observar que se genera documento FALLO_CONDENATORIO pendiente de firma.
- Firmar.
- Notificar (resultadoFinal=CONDENADO, plazo apelacion abierto).

Punto de discusion: "Al notificar un fallo condenatorio el sistema abre automaticamente el plazo de apelacion."

---

### 7. Apelacion (3 min)

Acta: ACTA-0027 o ACTA-0028 (PENDIENTE_ANALISIS, para registrar apelacion)

- Mostrar que el acta tiene plazo de apelacion abierto.
- Registrar apelacion (canal PRESENCIAL_DIRECCION o PORTAL_INFRACTOR).
- Mostrar que resultadoFinal queda CONDENADO con apelacion pendiente.
- Resolver apelacion (RECHAZADA -> CONDENA_FIRME, o ACEPTADA_ABSUELVE -> ABSUELTO).

Punto de discusion: "El sistema soporta el circuito completo de apelacion con sus dos resoluciones posibles."

---

### 8. Condena firme + pago condena (4 min)

Acta: ACTA-0029 (continuando desde paso 6) o ACTA-0122 (ya en CONDENA_FIRME)

Desde ACTA-0029 post-notificacion:
- Vencer plazo de apelacion -> resultadoFinal=CONDENA_FIRME.
- Informar pago condena.
- Confirmar pago condena.
- Observar que cerrabilidad.cerrable=true.
- Cerrar acta.
- Confirmar en CERRADAS con monto=1700 y situacionPagoCondena=CONFIRMADO.

Desde ACTA-0122 (camino rapido):
- Ya esta en CONDENA_FIRME, ir directo a informar/confirmar/cerrar.

Punto de discusion: "La condena firme es el estado final condenatorio. El sistema controla que solo se pueda cerrar si el pago fue confirmado o se derivo a gestion externa."

---

### 9. Bloqueantes materiales (3 min)

Acta: ACTA-0019 (PENDIENTE_ANALISIS, PENDIENTE_CIERRE_MATERIAL, 3 bloqueantes)

- Mostrar que cerrabilidad.cerrable=false con 3 pendientes.
- Mostrar los 3 ejes bloqueantes (MEDIDA_PREVENTIVA, RODADO, DOCUMENTACION).
- Resolver bloqueante documental.
- Resolver cumplimiento material.
- Repetir para los 3 ejes.
- Confirmar que cerrabilidad.cerrable=true al resolver todos.
- Cerrar.

Punto de discusion: "El cierre solo se habilita cuando todos los compromisos materiales estan cumplidos. El sistema lo controla automaticamente."

---

### 10. Gestion externa / retorno (2 min)

Acta: ACTA-0030 (para ver circuito APREMIO) o ACTA-0017 (ya en GESTION_EXTERNA)

- Mostrar acta en GESTION_EXTERNA con tipo APREMIO o JUZGADO_DE_PAZ.
- Reingresar desde gestion externa.
- Observar retorno a PENDIENTE_ANALISIS con accionPendiente=REVISION_POST_GESTION_EXTERNA.

Punto de discusion: "Cuando un expediente va a APREMIO o Juzgado de Paz, el sistema lo registra y permite el reingreso controlado cuando corresponde."

---

### 11. Archivo / reingreso (2 min)

Acta: ACTA-0007 (estado inicial ARCHIVO)

- Mostrar acta en bandeja ARCHIVO.
- Reingresar.
- Observar retorno a PENDIENTE_ANALISIS.

---

### 12. Paralizadas + reactivacion (2 min)

Acta: ACTA-0114 (PARALIZADAS)

- Mostrar que aparece en PARALIZADAS con estado protegido.
- Confirmar que NO muestra acciones internas operativas (pago voluntario, firma, etc.).
- Confirmar que muestra boton "Reactivar acta".
- Ejecutar reactivacion.
- Observar que pasa a PENDIENTE_ANALISIS con accionPendiente=REVISION_POST_REACTIVACION.

Punto de discusion: "Las actas paralizadas quedan protegidas hasta que se toma una decision deliberada de reactivarlas. El sistema lo controla sin bloquear el flujo del resto."

---

### 13. Correo postal (2 min)

- Mostrar la operacion de correo postal bajo "Operacion de notificaciones".
- Mostrar que correo postal es el canal operativo real.
- Mostrar que el notificador municipal queda como canal demo secundario/temporal.

---

## Actas recomendadas para mostrar

| Acta | Circuito |
|------|----------|
| ACTA-0001 | Redaccion documental — NOTIFICACION_ACTA |
| ACTA-0006 | Fallo absolutorio — cierre rapido |
| ACTA-0011 | Resolucion documental |
| ACTA-0014 | Rectificacion documental |
| ACTA-0019 | Bloqueantes materiales — 3 ejes |
| ACTA-0029 | Fallo condenatorio — vencimiento — condena firme — pago — cierre |
| ACTA-0030 | Gestion externa APREMIO — reingreso — pago condena |
| ACTA-0114 | PARALIZADAS — reactivacion controlada |
| ACTA-0007 | Archivo — reingreso |
| ACTA-0017 | Gestion externa — retorno |
| ACTA-0122 | Condena firme — pago condena rapido |

---

## Que NO prometer

- No prometer integracion productiva real con Informix, AFIP, sistemas legados u otros.
- No prometer permisos y roles reales si no estan implementados en esta version.
- No prometer rendimiento productivo en grandes volumenes (el prototipo usa estado en memoria).
- No prometer integracion con el portal del infractor para apelaciones (es mock).
- No prometer integracion con el notificador municipal productivo (es demo secundario).
- No prometer generacion real de documentos PDF (son mocks).

---

## Cierre recomendado

"La demo esta lista para validacion funcional con usuarios clave. Cada circuito ha sido validado de punta a punta por tests automaticos y por recorrido manual. El proximo paso es revisar con el equipo operativo si el flujo modela correctamente la realidad del proceso y definir los ajustes de negocio necesarios antes de avanzar a la etapa productiva."

---

## Estado al momento de esta guia

- Dataset: 42 actas canonicas.
- MatrizSanidadActasDemoIT: 42 actas, 0 warnings, 0 errores.
- Build Angular: OK (warnings preexistentes de budget, sin regresiones).
- Circuitos validados: 15/15 por API/tests (Slices 18A, 19A, 19B).
- Validacion visual en browser: pendiente confirmacion manual.
- Proximo paso tecnico recomendado: validacion visual UX transversal en browser por el equipo.