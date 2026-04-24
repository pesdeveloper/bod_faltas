# UX Demo — Alta mock acta demo

## Objetivo

Definir pantalla/modal para crear acta mock mínima en vivo durante la demo.

No reemplaza labrado real final.

## Endpoint

`POST /api/prototipo/actas/mock`

Resultado:

- crea acta mock
- genera `ACTA-DEMO-0001+`
- nace en `ACTAS_EN_ENRIQUECIMIENTO`
- aparece en listado
- abre detalle

## Botón

Ubicación: header.

Label:

- Crear acta demo

## Formulario

Campo obligatorio:

- dependencia

Valores:

- Tránsito
- Inspecciones
- Fiscalización
- Bromatología

## Campos por dependencia

| Dependencia | Campos |
|---|---|
| Tránsito | eje urbano, rodado retenido/secuestrado, documentación retenida |
| Inspecciones | medida preventiva: clausura |
| Fiscalización | medida preventiva: paralización de obra |
| Bromatología | decomiso de sustancias alimenticias |

## Reglas por dependencia

### Tránsito

- usa `ActaTransitoMock`
- datos nacen con el acta
- no son acciones disponibles
- rodado retenido proyecta `LIBERACION_RODADO`
- documentación retenida proyecta `ENTREGA_DOCUMENTACION`

### Inspecciones

- clausura proyecta `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- no usa `ActaTransitoMock`

### Fiscalización

- paralización de obra proyecta `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- no usa `ActaTransitoMock`

### Bromatología

- decomiso es dato propio
- usa `ActaBromatologiaMock`
- no es medida preventiva genérica
- no genera `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- no genera `LIBERACION_RODADO`
- no genera `ENTREGA_DOCUMENTACION`
- posible eje futuro: `LIBERACION_DECOMISO`

## Resultado visual

Después de crear:

Acta creada: ACTA-DEMO-0001  
Bandeja: Actas en enriquecimiento

Acciones:

- Abrir acta
- Crear otra
- Ver bandeja

## Validaciones

Rechazar combinaciones inválidas.

Ejemplos:

| Caso | Resultado |
|---|---|
| Tránsito + decomiso | error |
| Bromatología + rodado retenido | error |
| Bromatología + documentación retenida | error |
| Inspecciones + rodado retenido | error |
| Fiscalización + decomiso | error |
| Dependencia desconocida | error |

Mensaje sugerido:

La combinación seleccionada no corresponde a la dependencia elegida.

## Escenarios mínimos a probar

### Tránsito

Input:

- dependencia: Tránsito
- eje urbano: sí
- rodado retenido: sí
- documentación retenida: sí

Esperado:

- acta creada
- bloqueantes rodado/documentación
- datos tránsito visibles

### Inspecciones

Input:

- dependencia: Inspecciones
- clausura: sí

Esperado:

- bloqueante medida preventiva
- sin datos tránsito

### Fiscalización

Input:

- dependencia: Fiscalización
- paralización: sí

Esperado:

- bloqueante medida preventiva
- sin datos tránsito

### Bromatología

Input:

- dependencia: Bromatología
- decomiso: sí

Esperado:

- dato bromatología visible
- sin bloqueante medida preventiva
- sin bloqueantes tránsito

## Criterio de cierre

Listo cuando:

- crea acta
- genera número
- aparece en bandeja
- abre detalle
- valida combinaciones
- proyecta condiciones correctas
- no confunde decomiso con medida preventiva