# ESTADO ACTUAL — PROTOTIPO FALTAS

## Nivel de avance

El prototipo backend se encuentra funcional y ejecutable.

Incluye:

- dataset mock inicial (10 actas)
- store en memoria (PrototipoStore)
- endpoints de lectura completos
- endpoints de acciones principales

## Acciones implementadas

1. pasar-a-notificacion
   - D3 → D4
   - genera evento FIRMA_COMPLETADA
   - crea notificación inicial

2. registrar-notificacion-positiva
   - D4 → D5
   - evento NOTIFICACION_ENTREGADA
   - marca notificación como ENTREGADA

3. cerrar-acta
   - D5 → CERRADA
   - evento CIERRE_ANALISIS

4. archivar-acta
   - D5 → ARCHIVO
   - evento ARCHIVADO_DESDE_ANALISIS

## Estado del flujo

Cubierto parcialmente:

D3 → D4 → D5 → (CERRADA | ARCHIVO)

## Lo que falta

Para considerar el sistema como simulador integral:

### 1. Bandejas faltantes
- completar universo real (≈21 bandejas definidas en spec)

### 2. Acciones faltantes
- flujo documental completo
- generación de documentos
- múltiples estados de firma
- notificación:
  - negativa
  - vencida
  - reintentos
- reingreso desde archivo
- derivaciones

### 3. Simulación rica
- documentos dinámicos
- múltiples notificaciones por acta
- estados intermedios

### 4. Coherencia global
- matriz bandeja → acciones → destino
- cobertura de todos los caminos

---

# PRÓXIMO PASO

Construir MATRIZ DEL SIMULADOR:

Por cada bandeja:

- qué muestra
- acciones disponibles
- evento generado
- cambios en ActaMock
- impacto en documentos
- impacto en notificaciones
- bandeja destino

Luego:

Implementar acciones faltantes de forma incremental,
manteniendo patrón actual:

- enum + record
- método en store
- endpoint controller

---

# OBJETIVO FINAL

Simulador completo que permita:

- recorrer TODO el sistema
- ejecutar TODAS las acciones
- validar TODOS los caminos del negocio

Base para:

- validación funcional con usuarios
- diseño de UI real
- posterior implementación productiva