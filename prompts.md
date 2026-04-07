>> QR, ese debe generarse en 01 !
>> Catalogos de calles
>> Tabla de obtencion de Calle x Altura de barrio , localidad etc
>> Tabla o servicio para obtener por GPS la ubicacion completa, calle, altura todo !!!!!

>> Tabla de municipios
>> Tabla de provincias , localidades, departamentos, etc ! o servicio web que lo devuelva !

# Observaciones de relevamiento

> VALIDADO:
> La bandeja existe como estación real.

> CORREGIR:
> “Archivar” no ocurre desde esta bandeja.

> PENDIENTE:
> Confirmar si el cambio de canal se hace aquí o en la siguiente.

> DESCARTADO:
> No existe acción de reasignación.
---

Estamos retomando el diseño/spec-as-source del sistema de faltas municipal de Malvinas Argentinas.

IMPORTANTE:
- Responder en español.
- No rediseñar desde cero.
- No volver a discutir fundamentos ya cerrados salvo contradicción real.
- Continuar exactamente desde la fase actual.
- Mantener el estilo de trabajo actual:
  - documentos `.md` canónicos
  - simples
  - claros
  - conversables con el área
  - sin sobrecarga técnica innecesaria
- Evitar diagramas gigantes.
- Si se usan gráficos, que sean mínimos, de alto nivel y solo cuando ayuden.
- Cuidar siempre los bloques markdown internos para que cierren correctamente.
- No mezclar cierre con archivo.
- No crear bandeja de desarchivo.
- No modelar apelación como paso lineal obligatorio.

==================================================
CONTEXTO GENERAL DEL PROYECTO
==================================================

El proyecto fue reorganizado en dos grandes zonas:

- `spec/` = verdad canónica vigente
- `historico/` = evolución del diseño, respaldo y trazabilidad

Objetivo final:
- ChatGPT = arquitecto
- Cursor = implementador
- usuario = supervisor
- spec-as-source = fuente de verdad para generar backend, frontend, móvil, contratos, queries, snapshot y diseño implementable

==================================================
STACK TECNOLÓGICO CANÓNICO
==================================================

Base de datos transaccional:
- Informix 12.10

Backend principal:
- Java
- Spring Boot
- Spring JDBC (`NamedParameterJdbcTemplate`)
- HikariCP
- SQL explícito
- NO WebFlux como base del core
- NO Netty como decisión principal
- NO ORM pesado como núcleo

Frontend web interno:
- Angular
- TypeScript
- RxJS
- Angular Material
- detrás de Nginx o Apache como reverse proxy

Aplicación móvil / PDA:
- Flutter
- dio
- freezed + json_serializable
- flutter_bloc
- isar
- flutter_secure_storage
- connectivity_plus
- geolocator
- esc_pos_printer
- flutter_appauth
- image_picker
- file_picker
- path_provider
- uuid
- mockito

Storage documental:
- fileserver local / unidad de red
- metadata documental en DB
- acceso abstracto desde backend

==================================================
PRINCIPIOS CANÓNICOS YA CERRADOS
==================================================

Centro del dominio:
- `Acta`
- `ActaEvento`
- `Documento`
- `ActaSnapshotOperativo`
- `Notificacion` como único satélite persistente específico

Regla base:
- evento + documento + snapshot

`ActaEvento`:
- solo hechos procesales reales
- no logs técnicos
- no eventos de infraestructura

`Documento`:
- modelo documental unificado
- sin proliferación de entidades documentales paralelas por etapa

`ActaSnapshotOperativo`:
- derivado
- regenerable
- no fuente de verdad
- sirve para bandejas, filtros y lectura rápida

Económico:
- no es subdominio central del sistema de faltas
- se resuelve por integración

Catálogos ya trabajados:
- `EstadoActa`
- `TipoEventoActa`
- `TipoDocumento`
- `EstadoDocumento`
- `CanalNotificacion`
- `EstadoNotificacion`
- `OrigenEvento`
- `TipoGestionExterna`
- `ResultadoExterno`
- `MotivoCierreActa`
- `FirmaTipo`

Snapshot ya definido en:
- `04-snapshot/00-snapshot-engine.md`
- `04-snapshot/01-campos-snapshot-operativo.md`
- `04-snapshot/02-reglas-derivacion.md`
- `04-snapshot/03-transiciones.md`

==================================================
FASE EN LA QUE ESTAMOS
==================================================

Estamos en fase de VALIDACIÓN FUNCIONAL POR BANDEJAS.

Decisión ya tomada:
- abandonar por ahora el diagrama gigante del viaje del acta
- validar una bandeja por documento, simple, clara y conversable con Dirección de Faltas
- todo esto vive en:
  - `spec/05-bandejas/`

Ya existe:
- `00-metodologia-validacion-bandejas.md`

Propósito de esta fase:
- depurar estados
- depurar eventos
- depurar acciones posibles
- validar si las bandejas existen realmente como estaciones operativas
- ajustar snapshot flags, procesos y catálogos si hace falta
- cerrar la última parte del diseño antes de bajar a queries, contratos y código

==================================================
DECISIONES FUNCIONALES IMPORTANTES YA CERRADAS
==================================================

1. Primera bandeja real del sistema:
- `01-bandeja-labradas.md`
- el inspector labra el acta, pero no opera el flujo principal

2. Competencia / materia:
- queda definida desde el labrado
- no existe “reasignación” entre áreas
- sigue todo dentro de Dirección de Faltas

3. Anulación:
- no es acción liviana
- es excepcional y fundada

4. Revisión inicial:
- desde la primera bandeja se puede decidir:
  - ir a enriquecimiento
  - o ir directo a preparación para notificación del acta

5. Tramo de notificación del acta quedó separado en:
- `03-bandeja-preparacion-para-notificacion-acta.md`
- `04-bandeja-actas-listas-para-notificar.md`
- `05-bandeja-notificacion-acta-en-proceso.md`

6. Regla cerrada:
- el acta no pasa a “listas para notificar” hasta que toda la documentación previa requerida esté generada y firmada

7. Luego del acuse positivo / notificación fehaciente del acto:
- pasa a `11-bandeja-actas-notificadas.md`
- desde la fecha de acuse positivo corre un plazo configurado
- vencido ese plazo pasa a `12-bandeja-pendiente-de-fallo.md`
- también puede pasar manualmente

8. En `12-bandeja-pendiente-de-fallo.md`:
- no existe acción “tomar para iniciar resolución”
- la acción fuerte es directamente:
  - generar fallo y documentación necesaria

9. Luego del fallo:
- `13-bandeja-fallo-pendiente-de-notificacion.md`
- `14-bandeja-fallos-listos-para-notificar.md`
- `15-bandeja-notificacion-fallo-en-proceso.md`
- `16-bandeja-fallos-notificados.md`

10. La apelación:
- NO es lineal
- NO es obligatoria
- solo existe si efectivamente se presenta
- aparece luego del fallo notificado
- vive en:
  - `17-bandeja-con-apelacion.md`

11. Si no hay apelación ni otra actuación que altere el recorrido:
- desde `16-bandeja-fallos-notificados.md`
- el destino natural al vencer el plazo es:
  - `18-bandeja-gestion-externa.md`

12. Gestión externa:
- no es simple salida sin retorno
- permite elegir destino externo, al menos:
  - apremios
  - Juzgado de Paz
- debe mantener trazabilidad del trámite externo
- permite reingreso con efectos materiales

13. Reingreso desde gestión externa:
- puede traer:
  - actuaciones
  - documentos
  - evidencias
  - resultados
  - pagos
  - decisiones externas
- puede requerir:
  - rectificar fallo
  - modificar fallo
  - emitir nuevo fallo
- si eso ocurre:
  - el expediente vuelve al circuito interno
  - punto de reingreso definido actualmente:
    - `12-bandeja-pendiente-de-fallo.md`
  - se genera nuevo fallo o fallo rectificado
  - se firma
  - se notifica nuevamente
  - y recién luego, si corresponde, podrá ir otra vez a apremios

14. Paralización:
- `19-bandeja-paralizadas.md`
- no es cierre
- no es simple espera
- es detención fundada

15. Cierre:
- `20-bandeja-cerradas.md`
- es la última bandeja operativa
- cierre ≠ archivo

16. Archivo:
- `21-proceso-archivo.md`
- proceso posterior al cierre
- no es una bandeja operativa
- puede ser manual, automático, por lote, por antigüedad, etc.
- no existe bandeja de desarchivo
- si se desarchiva:
  - el expediente vuelve a:
    - `06-bandeja-analisis-presentaciones-pagos.md`

==================================================
ARCHIVOS ACTUALES DE BANDEJAS / PROCESOS
==================================================

La serie conceptual actual quedó en 21 piezas:

- `00-metodologia-validacion-bandejas.md`
- `01-bandeja-labradas.md`
- `02-bandeja-enriquecimiento.md`
- `03-bandeja-preparacion-para-notificacion-acta.md`
- `04-bandeja-actas-listas-para-notificar.md`
- `05-bandeja-notificacion-acta-en-proceso.md`
- `06-bandeja-analisis-presentaciones-pagos.md`
- `07-bandeja-pendiente-acto-administrativo.md`
- `08-bandeja-acto-administrativo-en-proceso.md`
- `09-bandeja-pendiente-notificacion-acto.md`
- `10-bandeja-notificacion-acto-en-proceso.md`
- `11-bandeja-actas-notificadas.md`
- `12-bandeja-pendiente-de-fallo.md`
- `13-bandeja-fallo-pendiente-de-notificacion.md`
- `14-bandeja-fallos-listos-para-notificar.md`
- `15-bandeja-notificacion-fallo-en-proceso.md`
- `16-bandeja-fallos-notificados.md`
- `17-bandeja-con-apelacion.md`
- `18-bandeja-gestion-externa.md`
- `19-bandeja-paralizadas.md`
- `20-bandeja-cerradas.md`
- `21-proceso-archivo.md`

==================================================
MATERIAL NUEVO YA HECHO
==================================================

Ya quedó armado un:
- `00-indice-maestro-bandejas.md`

Con formato resumido por bandeja:
- función
- entra por
- acciones posibles
- acción → efecto / destino

Y también quedó elegido un gráfico resumido del índice maestro:
- la versión simplificada, no la técnica grande
- flujo principal + ramas no lineales + cierre + archivo

La versión elegida del gráfico fue la segunda, la resumida.

==================================================
OBSERVACIONES DE CONSISTENCIA YA DETECTADAS
==================================================

En la última revisión quedaron estos ajustes finos todavía a revisar/corregir:

1. `11-bandeja-actas-notificadas.md`
- sacar restos de apelación
- no debe sugerir que la apelación vive en esta etapa

2. `12-bandeja-pendiente-de-fallo.md`
- revisar una redacción heredada que aún sugiere lógica vieja de “tomar el caso”
- debe quedar consistente con:
  - desde aquí se genera fallo directamente

3. `16-bandeja-fallos-notificados.md`
- limpiar restos de texto provisorio como:
  - “o nombre equivalente si luego se renumera”

4. `18-bandeja-gestion-externa.md`
- limpiar restos de texto provisorio similares de renumeración

5. `20-bandeja-cerradas.md`
- evitar cualquier mezcla residual entre cierre y archivo

==================================================
ESTADO EXACTO AL RETOMAR
==================================================

Estamos justo en este punto:

- ya está armado el índice maestro resumido de bandejas
- ya está elegido el gráfico resumido del índice maestro
- el bloque 05-bandejas ya quedó muy maduro conceptualmente
- no hay que rediseñar
- no hay que volver atrás
- hay que retomar desde aquí y seguir con lo que resulte más útil

==================================================
PRÓXIMO PASO MÁS ÚTIL AL RETOMAR
==================================================

Al volver, el siguiente paso natural puede ser uno de estos tres, a decidir en el momento:

1. hacer una pasada final de consistencia sobre los archivos puntuales que aún tienen detalles menores
2. consolidar el `00-indice-maestro-bandejas.md` como documento canónico
3. armar una matriz implementable tipo:
   - evento → bandeja destino
   - por ejemplo:
     - `acuse_positivo_acto` → `11-bandeja-actas-notificadas.md`
     - `plazo_post_acto_vencido` → `12-bandeja-pendiente-de-fallo.md`
     - `apelacion_presentada_post_fallo` → `17-bandeja-con-apelacion.md`
     - `desarchivo` → `06-bandeja-analisis-presentaciones-pagos.md`

==================================================
INSTRUCCIÓN DE CONTINUIDAD
==================================================

No explicar de nuevo todo este contexto.
No rediseñar desde cero.
No discutir fundamentos ya cerrados salvo contradicción real.
Retomar exactamente desde aquí, con foco en consistencia final, utilidad práctica del spec y preparación para implementación asistida con IA.




