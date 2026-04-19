Estamos retomando el prototipo del sistema de faltas municipal (Malvinas Argentinas).

Contexto:

- Proyecto: backend/api-faltas-prototipo
- Stack: Java 21 + Spring Boot 3.5.x
- Sin base de datos (in-memory store)
- Sin seguridad
- Enfoque: prototipo descartable pero realista
- Arquitectura: PrototipoStore como único estado + Controller directo

Estado actual:

El prototipo ya implementa:

Lectura:
- /api/prototipo/health
- /api/prototipo/bandejas
- /api/prototipo/bandejas/{codigo}/actas
- /api/prototipo/actas/{id}
- /api/prototipo/actas/{id}/eventos
- /api/prototipo/actas/{id}/documentos
- /api/prototipo/actas/{id}/notificaciones

Acciones implementadas:
- pasar-a-notificacion (D3 → D4)
- registrar-notificacion-positiva (D4 → D5)
- cerrar-acta (D5 → CERRADA)
- archivar-acta (D5 → ARCHIVO)

El prototipo se prueba con script PowerShell que:
- hace reset
- recorre bandejas
- ejecuta acciones
- valida estados

Objetivo ahora:

Convertir el prototipo en un SIMULADOR INTEGRAL COMPLETO del sistema de faltas.

Esto implica:

1. Tener TODAS las bandejas relevantes
2. Tener TODAS las acciones posibles por bandeja
3. Cubrir TODOS los caminos del flujo:
   - flujo principal
   - alternativos
   - excepciones
4. Simular:
   - documentos
   - firma
   - notificaciones (positivo, negativo, vencido, reintento)
   - análisis
   - cierre
   - archivo
   - reingreso

Restricciones:
- mantener simpleza
- no agregar DB
- no sobrearquitectura
- mantener patrón actual (store + enum + record + controller)
- no framework genérico de acciones todavía

Rol:

Vos sos el arquitecto.
Definís qué falta, en qué orden y cómo implementarlo.
Yo ejecuto con Cursor.

Punto de arranque:

Necesitamos construir la MATRIZ COMPLETA del simulador:

- bandeja
- acciones disponibles
- evento generado
- cambios en acta
- documentos mock
- notificaciones mock
- bandeja destino

Arrancamos por ahí.