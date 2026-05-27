# Auditoría de dependencias — mocks demo Faltas

**Fecha:** 2026-05-26  
**Alcance:** `MockDataFactory.java` → mapa `dependenciaDemoPorActa` en `PrototipoStore` (campo API `dependenciaDemo`; en UX se muestra `—` si es `null`).  
**Regla del slice:** solo auditoría; **no** se modificaron mocks, UX ni backend funcional.

## Resumen

| Métrica | Valor |
|---------|------:|
| Actas mock cargadas | 116 |
| Con dependencia registrada | 14 |
| Sin dependencia (`—` / null en API) | 102 |

## Enum demo vigente (`DependenciaActaDemo`)

| Código API | Etiqueta operativa |
|------------|-------------------|
| `TRANSITO` | Tránsito |
| `INSPECCIONES` | Inspección general |
| `FISCALIZACION` | Obras particulares |
| `BROMATOLOGIA` | Bromatología |

No existen aún códigos demo para **Comercio**, **Ambiente** ni **Juzgado de faltas**; las filas que los sugieren indican ampliación de enum o mapeo UX en un slice posterior.

## Criterio de auditoría

Se considera dependencia **ausente** cuando la acta no tiene entrada en `dependenciaDemoPorActa` tras el bootstrap (equivalente a `dependenciaDemo: null` → pantalla `—`).

## Detalle por acta

| Acta | Bandeja actual | Dependencia actual | Dependencia sugerida | Motivo | ¿Corregir en próximo slice? |
|------|----------------|-------------------|----------------------|--------|:---------------------------:|
| ACTA-0001 | ACTAS_EN_ENRIQUECIMIENTO | TRANSITO_URBANO → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0002 | PENDIENTE_PREPARACION_DOCUMENTAL | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0003 | PENDIENTE_FIRMA | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0004 | PENDIENTE_NOTIFICACION | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0005 | EN_NOTIFICACION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0006 | PENDIENTE_ANALISIS | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0007 | ARCHIVO | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0008 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0009 | ACTAS_EN_ENRIQUECIMIENTO | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0010 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0011 | PENDIENTES_RESOLUCION_REDACCION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0012 | PENDIENTES_RESOLUCION_REDACCION | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0013 | PENDIENTES_RESOLUCION_REDACCION | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0014 | PENDIENTES_RESOLUCION_REDACCION | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0015 | PENDIENTE_ANALISIS | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0016 | PENDIENTE_ANALISIS | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0017 | GESTION_EXTERNA | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0018 | ACTAS_EN_ENRIQUECIMIENTO | TRANSITO_URBANO → — | Juzgado de faltas | Gestión externa tipo Juzgado de Paz. Fuera del enum demo actual. | Sí |
| ACTA-0019 | PENDIENTE_ANALISIS | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0020 | PENDIENTES_RESOLUCION_REDACCION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0021 | PENDIENTE_ANALISIS | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0022 | ACTAS_EN_ENRIQUECIMIENTO | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0023 | PENDIENTE_ANALISIS | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0024 | ACTAS_EN_ENRIQUECIMIENTO | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0025 | ACTAS_EN_ENRIQUECIMIENTO | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0026 | PENDIENTE_ANALISIS | ESTABLECIMIENTO → — | Comercio | Contravención comercial; enum demo más cercano: `INSPECCIONES`. Propuesta: `DependenciaActaDemo.INSPECCIONES`. | Sí |
| ACTA-0027 | PENDIENTE_ANALISIS | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0028 | PENDIENTE_ANALISIS | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0029 | PENDIENTE_ANALISIS | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0030 | PENDIENTE_ANALISIS | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0031 | PENDIENTE_NOTIFICACION | DEMO_NOTIFICADOR_MUNICIPAL → — | Juzgado de faltas | Notificación municipal de fallos. Fuera del enum demo actual. | Sí |
| ACTA-0032 | PENDIENTE_NOTIFICACION | DEMO_NOTIFICADOR_MUNICIPAL → — | Juzgado de faltas | Notificación municipal de fallos. Fuera del enum demo actual. | Sí |
| ACTA-0033 | PENDIENTE_NOTIFICACION | DEMO_NOTIFICADOR_MUNICIPAL → — | Juzgado de faltas | Notificación municipal de fallos. Fuera del enum demo actual. | Sí |
| ACTA-0034 | PENDIENTE_NOTIFICACION | DEMO_PORTAL_INFRACTOR → — | Juzgado de faltas | Canal portal infractor (expediente en Dirección de Faltas). Fuera del enum demo actual. | Sí |
| ACTA-0035 | PENDIENTE_NOTIFICACION | DEMO_PORTAL_INFRACTOR → — | Juzgado de faltas | Canal portal infractor (expediente en Dirección de Faltas). Fuera del enum demo actual. | Sí |
| ACTA-0036 | PENDIENTE_NOTIFICACION | DEMO_PORTAL_INFRACTOR → — | Juzgado de faltas | Canal portal infractor (expediente en Dirección de Faltas). Fuera del enum demo actual. | Sí |
| ACTA-0037 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0038 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0039 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0040 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0041 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0042 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0043 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0044 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0045 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0046 | ACTAS_EN_ENRIQUECIMIENTO | Rossi, Camila → — | Tránsito | Dominio `Rossi, Camila`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0047 | ACTAS_EN_ENRIQUECIMIENTO | Ponce, Esteban → — | Tránsito | Dominio `Ponce, Esteban`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0048 | ACTAS_EN_ENRIQUECIMIENTO | Salinas, Nora → — | Tránsito | Dominio `Salinas, Nora`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0049 | ACTAS_EN_ENRIQUECIMIENTO | Vega, Tomas → — | Tránsito | Dominio `Vega, Tomas`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0050 | PENDIENTE_PREPARACION_DOCUMENTAL | Cabrera, Luis → — | Tránsito | Dominio `Cabrera, Luis`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0051 | PENDIENTE_PREPARACION_DOCUMENTAL | Dominguez, Paula → — | Tránsito | Dominio `Dominguez, Paula`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0052 | PENDIENTE_PREPARACION_DOCUMENTAL | Franco, Mario → — | Tránsito | Dominio `Franco, Mario`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0053 | PENDIENTE_PREPARACION_DOCUMENTAL | Gimenez, Clara → — | Tránsito | Dominio `Gimenez, Clara`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0054 | PENDIENTE_PREPARACION_DOCUMENTAL | Herrera, Diego → — | Tránsito | Dominio `Herrera, Diego`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0055 | PENDIENTE_PREPARACION_DOCUMENTAL | Ibarra, Sofia → — | Tránsito | Dominio `Ibarra, Sofia`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0056 | PENDIENTE_PREPARACION_DOCUMENTAL | Juarez, Ana → — | Tránsito | Dominio `Juarez, Ana`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0057 | PENDIENTE_PREPARACION_DOCUMENTAL | Klein, Bruno → — | Tránsito | Dominio `Klein, Bruno`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0058 | PENDIENTE_PREPARACION_DOCUMENTAL | Luna, Carla → — | Tránsito | Dominio `Luna, Carla`; default alineado al grueso vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0059 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0060 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0061 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0062 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0063 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0064 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0065 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0066 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0067 | PENDIENTE_FIRMA | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0068 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0069 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0070 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0071 | PENDIENTE_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0072 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0073 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0074 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0075 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0076 | EN_NOTIFICACION | DEMO_CORREO_POSTAL → — | Juzgado de faltas | Canal correo postal sin dependencia de labrado registrada. Fuera del enum demo actual. | Sí |
| ACTA-0077 | PENDIENTES_RESOLUCION_REDACCION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0078 | PENDIENTES_RESOLUCION_REDACCION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0079 | PENDIENTES_RESOLUCION_REDACCION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0080 | PENDIENTES_RESOLUCION_REDACCION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0081 | PENDIENTES_RESOLUCION_REDACCION | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0082 | GESTION_EXTERNA | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0083 | GESTION_EXTERNA | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0084 | GESTION_EXTERNA | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0085 | GESTION_EXTERNA | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0086 | GESTION_EXTERNA | TRANSITO_URBANO → — | Juzgado de faltas | Gestión externa tipo Juzgado de Paz. Fuera del enum demo actual. | Sí |
| ACTA-0087 | GESTION_EXTERNA | TRANSITO_URBANO → — | Juzgado de faltas | Gestión externa tipo Juzgado de Paz. Fuera del enum demo actual. | Sí |
| ACTA-0088 | GESTION_EXTERNA | TRANSITO_URBANO → — | Juzgado de faltas | Gestión externa tipo Juzgado de Paz. Fuera del enum demo actual. | Sí |
| ACTA-0089 | GESTION_EXTERNA | TRANSITO_URBANO → — | Juzgado de faltas | Gestión externa tipo Juzgado de Paz. Fuera del enum demo actual. | Sí |
| ACTA-0090 | GESTION_EXTERNA | TRANSITO_URBANO → — | Tránsito | Dominio vial/urbano. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0091 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0092 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0093 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0094 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0095 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0096 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0097 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0098 | ARCHIVO | SEGURIDAD_VIAL → — | Tránsito | Seguridad vial; enum demo `TRANSITO`. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0099 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0100 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0101 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0102 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0103 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0104 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0105 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0106 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0107 | CERRADAS | ESTACIONAMIENTO → — | Tránsito | Estacionamiento en circuito vial demo. Propuesta: `DependenciaActaDemo.TRANSITO`. | Sí |
| ACTA-0108 | PENDIENTES_FALLO | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0109 | PENDIENTES_FALLO | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0110 | PENDIENTES_FALLO | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0111 | CON_APELACION | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0112 | CON_APELACION | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0113 | CON_APELACION | SEGURIDAD_VIAL → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0114 | PARALIZADAS | TRANSITO_URBANO → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0115 | PARALIZADAS | TRANSITO_URBANO → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |
| ACTA-0116 | PARALIZADAS | TRANSITO_URBANO → Tránsito | Tránsito | Ya tiene `registrarDependenciaDemo` → `TRANSITO`. | No |

## Actas con dependencia ya registrada

- **ACTA-0001**: `Tránsito` (ACTAS_EN_ENRIQUECIMIENTO, dominio `TRANSITO_URBANO`)
- **ACTA-0027**: `Tránsito` (PENDIENTE_ANALISIS, dominio `SEGURIDAD_VIAL`)
- **ACTA-0028**: `Tránsito` (PENDIENTE_ANALISIS, dominio `SEGURIDAD_VIAL`)
- **ACTA-0029**: `Tránsito` (PENDIENTE_ANALISIS, dominio `SEGURIDAD_VIAL`)
- **ACTA-0030**: `Tránsito` (PENDIENTE_ANALISIS, dominio `SEGURIDAD_VIAL`)
- **ACTA-0108**: `Tránsito` (PENDIENTES_FALLO, dominio `SEGURIDAD_VIAL`)
- **ACTA-0109**: `Tránsito` (PENDIENTES_FALLO, dominio `SEGURIDAD_VIAL`)
- **ACTA-0110**: `Tránsito` (PENDIENTES_FALLO, dominio `SEGURIDAD_VIAL`)
- **ACTA-0111**: `Tránsito` (CON_APELACION, dominio `SEGURIDAD_VIAL`)
- **ACTA-0112**: `Tránsito` (CON_APELACION, dominio `SEGURIDAD_VIAL`)
- **ACTA-0113**: `Tránsito` (CON_APELACION, dominio `SEGURIDAD_VIAL`)
- **ACTA-0114**: `Tránsito` (PARALIZADAS, dominio `TRANSITO_URBANO`)
- **ACTA-0115**: `Tránsito` (PARALIZADAS, dominio `TRANSITO_URBANO`)
- **ACTA-0116**: `Tránsito` (PARALIZADAS, dominio `TRANSITO_URBANO`)

## Prioridad sugerida para próximo slice (solo mocks)

1. **Casos núcleo** (`ACTA-0001`–`ACTA-0026`): registrar dependencia según dominio (mayoría Tránsito; `ACTA-0026` → Comercio / `INSPECCIONES`).
2. **Volumen enriquecimiento/preparación** (`ACTA-0046`–`ACTA-0058`): extender regla condicional existente en `cargarActaVolumenEnriquecimiento` (hoy solo `TRANSITO_URBANO` y `SEGURIDAD_VIAL`).
3. **Canales demo** (`ACTA-0031`–`0045`, `0068`–`0076`): definir si mostrar dependencia de labrado (Tránsito) o etiqueta Juzgado de faltas.
4. **Gestión externa Juzgado** (`ACTA-0017`, `0086`–`0089`): valorar `Juzgado de faltas` vs Tránsito según `tipoGestionExterna`.

## Validación observable

```http
GET /api/prototipo/actas/{id}
```

Tras corrección: `dependenciaDemo` no nulo.

## Fuente

- `backend/api-faltas-prototipo/.../MockDataFactory.java`
- `backend/api-faltas-prototipo/.../PrototipoStore.java`
