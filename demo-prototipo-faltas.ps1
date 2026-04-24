# Demo oficial actualizada del prototipo de faltas.
# Requiere el backend levantado en http://localhost:8087
#
# Arranque del backend, desde la raiz del repo:
#   cd .\backend\api-faltas-prototipo
#   mvn spring-boot:run
#
# Ejecucion de la demo, desde la raiz del repo:
#   powershell -ExecutionPolicy Bypass -File .\demo-prototipo-faltas.ps1
#
# Verificacion rapida:
#   Invoke-RestMethod http://localhost:8087/api/prototipo/health
#
# Esta demo es funcional-operativa. No prueba PDF real, edicion real de documentos,
# firma digital real, tesoreria real ni notificacion real. Usa acciones mock de negocio
# para validar bandejas, estados, hechos materiales, cerrabilidad y movimientos.

chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [System.Text.UTF8Encoding]::new()
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8087/api/prototipo"

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------

function Titulo($texto) {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host $texto -ForegroundColor Yellow
    Write-Host "============================================================" -ForegroundColor Cyan
}

function Subtitulo($texto) {
    Write-Host ""
    Write-Host "--- $texto" -ForegroundColor Green
}

function MostrarJson($obj) {
    ($obj | ConvertTo-Json -Depth 30) | Out-Host
}

function Url($path) {
    if ($path.StartsWith("http")) { return $path }
    if ($path.StartsWith("/")) { return "$baseUrl$path" }
    return "$baseUrl/$path"
}

function Get-Json($path) {
    Invoke-RestMethod -Uri (Url $path) -Method Get
}

function Post-Json($path, $body = $null) {
    if ($null -eq $body) {
        return Invoke-RestMethod -Uri (Url $path) -Method Post
    }

    $json = $body | ConvertTo-Json -Depth 30
    return Invoke-RestMethod -Uri (Url $path) -Method Post -ContentType "application/json; charset=utf-8" -Body $json
}

function Post-Conflict($path, $body = $null) {
    try {
        $r = Post-Json $path $body
        MostrarJson $r
        throw "Se esperaba HTTP 409 para $path, pero la llamada fue exitosa."
    }
    catch {
        if ($_.Exception.Response -ne $null) {
            $status = [int]$_.Exception.Response.StatusCode
            if ($status -eq 409) {
                Write-Host "OK: $path devolvio HTTP 409 como se esperaba." -ForegroundColor DarkGreen
                return
            }
            throw "Se esperaba HTTP 409 para $path, pero devolvio HTTP $status."
        }
        throw
    }
}

function Assert-Igual($actual, $esperado, $mensaje) {
    if ($actual -ne $esperado) {
        throw "$mensaje | esperado='$esperado' actual='$actual'"
    }
}

function Assert-True($condicion, $mensaje) {
    if (-not $condicion) { throw $mensaje }
}

function Assert-Contains($coleccion, $valor, $mensaje) {
    $arr = @($coleccion)
    if ($arr -notcontains $valor) {
        throw "$mensaje | no contiene '$valor'. Valores: $($arr -join ', ')"
    }
}

function Mostrar-Detalle($actaId, $titulo) {
    Titulo $titulo
    $detalle = Get-Json "/actas/$actaId"
    MostrarJson $detalle
    return $detalle
}

function Mostrar-Documentos($actaId, $titulo) {
    Titulo $titulo
    $documentos = @(Get-Json "/actas/$actaId/documentos")
    MostrarJson $documentos
    return $documentos
}

function Mostrar-Notificaciones($actaId, $titulo) {
    Titulo $titulo
    $notificaciones = @(Get-Json "/actas/$actaId/notificaciones")
    MostrarJson $notificaciones
    return $notificaciones
}

function Mostrar-Eventos($actaId, $titulo) {
    Titulo $titulo
    $eventos = @(Get-Json "/actas/$actaId/eventos")
    MostrarJson $eventos
    return $eventos
}

function Mostrar-Bandejas($titulo) {
    Titulo $titulo
    $bandejas = @(Get-Json "/bandejas")
    MostrarJson $bandejas
    return $bandejas
}

function Reset-Demo($titulo = "RESET DATASET DEMO") {
    Titulo $titulo
    $reset = Post-Json "/reset"
    MostrarJson $reset
    return $reset
}

function Assert-Pendiente($detalle, $pendiente) {
    Assert-Contains $detalle.cerrabilidad.pendientesBloqueantesCierre $pendiente "Debe existir pendiente bloqueante $pendiente"
}

function Crear-ActaMock($body, $titulo) {
    Titulo $titulo
    $r = Post-Json "/actas/mock" $body
    MostrarJson $r
    Assert-Igual $r.bandejaActual "ACTAS_EN_ENRIQUECIMIENTO" "El acta mock debe nacer en ACTAS_EN_ENRIQUECIMIENTO"
    Assert-True ($r.id -like "ACTA-DEMO-*") "El id demo debe tener prefijo ACTA-DEMO-"
    Assert-Igual $r.numeroActa $r.id "En el prototipo el numeroActa demo debe coincidir con el id"
    return $r
}

function Registrar-Resolutorio($actaId, $tipo, $firmaNotif = $false) {
    $path = "/actas/$actaId/acciones/registrar-resolucion-bloqueo-cierre?tipo=$tipo"
    if ($firmaNotif) { $path = "$path&documentoConCircuitoFirmaNotif=true" }
    return Post-Json $path
}

function Registrar-Cumplimiento($actaId, $tipo) {
    return Post-Json "/actas/$actaId/acciones/registrar-cumplimiento-material-bloqueo-cierre?tipo=$tipo"
}

# -----------------------------------------------------------------------------
# Demo
# -----------------------------------------------------------------------------

try {
    Titulo "0. HEALTH"
    $health = Get-Json "/health"
    MostrarJson $health

    # =========================================================================
    # BLOQUE 1: ALTA MOCK MINIMA EN VIVO
    # =========================================================================

    Reset-Demo "1. RESET PARA ALTA MOCK EN VIVO"

    $actaDemoTransito = Crear-ActaMock @{
        dependencia = "TRANSITO"
        ejeUrbano = $true
        rodadoRetenidoOSecuestrado = $true
        documentacionRetenida = $true
    } "1.1 ALTA MOCK: TRANSITO CON RODADO Y DOCUMENTACION RETENIDA"

    Assert-Pendiente $actaDemoTransito "LIBERACION_RODADO"
    Assert-Pendiente $actaDemoTransito "ENTREGA_DOCUMENTACION"
    Assert-True ($null -ne $actaDemoTransito.datosTransito) "El acta de transito debe exponer datosTransito"
    Assert-Igual $actaDemoTransito.datosTransito.rodadoRetenidoOSecuestrado $true "Debe registrar rodado retenido/secuestrado"
    Assert-Igual $actaDemoTransito.datosTransito.documentacionRetenida $true "Debe registrar documentacion retenida"

    $actaDemoInspecciones = Crear-ActaMock @{
        dependencia = "INSPECCIONES"
        medidaPreventivaClausura = $true
    } "1.2 ALTA MOCK: INSPECCIONES CON CLAUSURA"

    Assert-Pendiente $actaDemoInspecciones "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
    Assert-True ($null -eq $actaDemoInspecciones.datosTransito) "Inspecciones no debe usar ActaTransitoMock"

    $actaDemoFiscalizacion = Crear-ActaMock @{
        dependencia = "FISCALIZACION"
        medidaPreventivaParalizacionObra = $true
    } "1.3 ALTA MOCK: FISCALIZACION CON PARALIZACION DE OBRA"

    Assert-Pendiente $actaDemoFiscalizacion "LEVANTAMIENTO_MEDIDA_PREVENTIVA"

    $actaDemoBromatologia = Crear-ActaMock @{
        dependencia = "BROMATOLOGIA"
        decomisoSustanciasAlimenticias = $true
    } "1.4 ALTA MOCK: BROMATOLOGIA CON DECOMISO"

    Assert-True ($null -ne $actaDemoBromatologia.datosBromatologia) "Bromatologia debe exponer datosBromatologia"
    Assert-Igual $actaDemoBromatologia.datosBromatologia.decomisoSustanciasAlimenticias $true "Debe registrar decomiso de sustancias alimenticias"
    Assert-True (@($actaDemoBromatologia.cerrabilidad.pendientesBloqueantesCierre).Count -eq 0) `
        "El decomiso de bromatologia no debe generar bloqueantes materiales genericos"

    Titulo "1.5 LISTADO DE ACTAS EN ACTAS_EN_ENRIQUECIMIENTO INCLUYE ACTAS DEMO"
    $listaEnriq = @(Get-Json "/bandejas/ACTAS_EN_ENRIQUECIMIENTO/actas")
    MostrarJson $listaEnriq
    Assert-True (@($listaEnriq | Where-Object { $_.id -eq $actaDemoTransito.id }).Count -eq 1) "El acta demo de transito debe aparecer en la bandeja"
    Assert-True (@($listaEnriq | Where-Object { $_.id -eq $actaDemoBromatologia.id }).Count -eq 1) "El acta demo de bromatologia debe aparecer en la bandeja"

    # =========================================================================
    # BLOQUE 2: ACTA-0024, TRANSITO CON HECHOS MATERIALES DESDE DATOS MOCK
    # =========================================================================

    Reset-Demo "2. RESET PARA RECORRIDO ACTA-0024"
    $acta0024 = "ACTA-0024"

    $d24 = Mostrar-Detalle $acta0024 "2.1 DETALLE INICIAL ACTA-0024: TRANSITO DESDE ActaTransitoMock"
    Assert-Igual $d24.bandejaActual "ACTAS_EN_ENRIQUECIMIENTO" "ACTA-0024 debe iniciar en enriquecimiento"
    Assert-Pendiente $d24 "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
    Assert-Pendiente $d24 "LIBERACION_RODADO"
    Assert-Pendiente $d24 "ENTREGA_DOCUMENTACION"
    Assert-True ($null -ne $d24.hechosMateriales.lecturaOperativa) "ACTA-0024 debe tener lecturaOperativa material"

    Titulo "2.2 RESOLUTORIO DE LEVANTAMIENTO CON CIRCUITO FIRMA/NOTIFICACION DOCUMENTAL"
    $r24medida = Registrar-Resolutorio $acta0024 "LEVANTAMIENTO_MEDIDA_PREVENTIVA" $true
    MostrarJson $r24medida
    Assert-Igual $r24medida.pendienteAtendido "LEVANTAMIENTO_MEDIDA_PREVENTIVA" "Debe atender el mismo eje material, no un bloqueante documental paralelo"

    $docs24 = Mostrar-Documentos $acta0024 "2.3 DOCUMENTO RESOLUTIVO NACE PENDIENTE_FIRMA"
    $docFirmaNotif = @($docs24 | Where-Object { $_.tipoDocumento -eq "DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF" })[0]
    Assert-True ($null -ne $docFirmaNotif) "Debe existir DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF"
    Assert-Igual $docFirmaNotif.estadoDocumento "PENDIENTE_FIRMA" "El documento con circuito firma/notif debe nacer PENDIENTE_FIRMA"

    $notifs24a = Mostrar-Notificaciones $acta0024 "2.4 NO SE GENERA NOTIFICACION DE ACTA SOLO POR EMITIR RESOLUTORIO"
    Assert-True (@($notifs24a).Count -eq 0) "No debe generarse notificacion automaticamente antes de firma"

    Titulo "2.5 FIRMA IN-SITU DEL DOCUMENTO RESOLUTIVO"
    $firma24 = Post-Json "/actas/$acta0024/acciones/firmar-documento/$($docFirmaNotif.id)"
    MostrarJson $firma24

    $docs24b = Mostrar-Documentos $acta0024 "2.6 DOCUMENTO FIRMADO, PERO BLOQUEO MATERIAL SIGUE ACTIVO"
    $docFirmado = @($docs24b | Where-Object { $_.id -eq $docFirmaNotif.id })[0]
    Assert-Igual $docFirmado.estadoDocumento "FIRMADO" "El documento debe quedar FIRMADO luego de firmar"

    $d24b = Mostrar-Detalle $acta0024 "2.7 FIRMA NO LIBERA CUMPLIMIENTO MATERIAL"
    Assert-Pendiente $d24b "LEVANTAMIENTO_MEDIDA_PREVENTIVA"

    Titulo "2.8 COMPLETAR RESOLUTORIOS RESTANTES Y CUMPLIMIENTOS MATERIALES"
    $r24rodado = Registrar-Resolutorio $acta0024 "LIBERACION_RODADO"
    MostrarJson $r24rodado
    $r24docs = Registrar-Resolutorio $acta0024 "ENTREGA_DOCUMENTACION"
    MostrarJson $r24docs

    $c24m = Registrar-Cumplimiento $acta0024 "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
    MostrarJson $c24m
    $c24r = Registrar-Cumplimiento $acta0024 "LIBERACION_RODADO"
    MostrarJson $c24r
    $c24d = Registrar-Cumplimiento $acta0024 "ENTREGA_DOCUMENTACION"
    MostrarJson $c24d

    $d24c = Mostrar-Detalle $acta0024 "2.9 ACTA-0024 SIN BLOQUEANTES MATERIALES ACTIVOS"
    Assert-True (@($d24c.cerrabilidad.pendientesBloqueantesCierre).Count -eq 0) "ACTA-0024 no debe tener pendientes materiales luego de cumplimientos"

    # =========================================================================
    # BLOQUE 3: ACTA-0026, MEDIDA PREVENTIVA POSTERIOR EN CONTRAVENCION
    # =========================================================================

    Reset-Demo "3. RESET PARA RECORRIDO ACTA-0026"
    $acta0026 = "ACTA-0026"

    $d26 = Mostrar-Detalle $acta0026 "3.1 DETALLE INICIAL ACTA-0026: CONTRAVENCION SIN MEDIDA INICIAL"
    Assert-True (@($d26.cerrabilidad.pendientesBloqueantesCierre).Count -eq 0) "ACTA-0026 debe iniciar sin bloqueante de medida preventiva"

    Titulo "3.2 ACCION: REGISTRAR MEDIDA PREVENTIVA POSTERIOR"
    $r26 = Post-Json "/actas/$acta0026/acciones/registrar-medida-preventiva-posterior"
    MostrarJson $r26

    $d26b = Mostrar-Detalle $acta0026 "3.3 MEDIDA POSTERIOR GENERA BLOQUEANTE Y LECTURA OPERATIVA"
    Assert-Pendiente $d26b "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
    Assert-True ($d26b.hechosMateriales.lecturaOperativa -match "posterior|tramite|trámite|medida preventiva") `
        "La lectura operativa debe reflejar medida posterior/tramite"

    Titulo "3.4 RESOLUTORIO NO LIBERA HASTA CUMPLIMIENTO MATERIAL"
    $r26res = Registrar-Resolutorio $acta0026 "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
    MostrarJson $r26res
    $d26c = Mostrar-Detalle $acta0026 "3.5 TRAS RESOLUTORIO: BLOQUEANTE SIGUE ACTIVO"
    Assert-Pendiente $d26c "LEVANTAMIENTO_MEDIDA_PREVENTIVA"

    Titulo "3.6 CUMPLIMIENTO MATERIAL DE MEDIDA POSTERIOR"
    $r26cumpl = Registrar-Cumplimiento $acta0026 "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
    MostrarJson $r26cumpl
    $d26d = Mostrar-Detalle $acta0026 "3.7 TRAS CUMPLIMIENTO: CASO CERRABLE SI RESULTADO FINAL YA ES COMPATIBLE"
    Assert-True (@($d26d.cerrabilidad.pendientesBloqueantesCierre).Count -eq 0) "ACTA-0026 debe quedar sin bloqueante material luego del cumplimiento"

    if ($d26d.cerrabilidad.cerrable -eq $true) {
        Titulo "3.8 CIERRE EXPLICITO ACTA-0026"
        $r26cierre = Post-Json "/actas/$acta0026/acciones/cerrar-acta"
        MostrarJson $r26cierre
        $d26final = Mostrar-Detalle $acta0026 "3.9 ACTA-0026 CERRADA"
        Assert-Igual $d26final.bandejaActual "CERRADAS" "ACTA-0026 debe cerrar explicitamente"
    }
    else {
        Write-Host "ACTA-0026 quedo sin bloqueantes, pero no cerrable por resultado final. Se omite cierre." -ForegroundColor DarkYellow
    }

    # =========================================================================
    # BLOQUE 4: RESOLUCIONES EN BANDEJAS OPERATIVAS Y RECHAZOS
    # =========================================================================

    Reset-Demo "4. RESET PARA RESOLUCIONES EN BANDEJAS OPERATIVAS"

    $acta0020 = "ACTA-0020"
    $d20 = Mostrar-Detalle $acta0020 "4.1 ACTA-0020 EN PENDIENTE_FIRMA CON BLOQUEANTE"
    Assert-Igual $d20.bandejaActual "PENDIENTE_FIRMA" "ACTA-0020 debe estar en PENDIENTE_FIRMA"
    Assert-Pendiente $d20 "LEVANTAMIENTO_MEDIDA_PREVENTIVA"

    Titulo "4.2 RESOLUTORIO PERMITIDO EN PENDIENTE_FIRMA"
    $r20 = Registrar-Resolutorio $acta0020 "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
    MostrarJson $r20
    $d20b = Mostrar-Detalle $acta0020 "4.3 BANDEJA CONSERVADA Y BLOQUEANTE SIGUE ACTIVO"
    Assert-Igual $d20b.bandejaActual "PENDIENTE_FIRMA" "Registrar resolutorio no debe mover artificialmente la bandeja"
    Assert-Pendiente $d20b "LEVANTAMIENTO_MEDIDA_PREVENTIVA"

    Titulo "4.4 RECHAZOS DE RESOLUTORIO EN BANDEJAS NO OPERABLES"
    Post-Conflict "/actas/ACTA-0017/acciones/registrar-resolucion-bloqueo-cierre?tipo=LIBERACION_RODADO"
    Post-Conflict "/actas/ACTA-0007/acciones/registrar-resolucion-bloqueo-cierre?tipo=LIBERACION_RODADO"
    Post-Conflict "/actas/ACTA-0008/acciones/registrar-resolucion-bloqueo-cierre?tipo=LIBERACION_RODADO"

    # =========================================================================
    # BLOQUE 5: NOTIFICACIONES NEGATIVAS Y VENCIDAS, DEMO LEGACY AUN UTIL
    # =========================================================================

    Reset-Demo "5. RESET PARA NOTIFICACIONES"

    $actaNotifNeg = "ACTA-0004"
    $dNeg = Mostrar-Detalle $actaNotifNeg "5.1 ACTA-0004 INICIAL"
    Assert-Igual $dNeg.bandejaActual "PENDIENTE_NOTIFICACION" "ACTA-0004 debe iniciar pendiente de notificacion"

    Titulo "5.2 REGISTRAR NOTIFICACION NEGATIVA"
    $rNeg = Post-Json "/actas/$actaNotifNeg/acciones/registrar-notificacion-negativa"
    MostrarJson $rNeg
    Assert-Igual $rNeg.bandejaActual "PENDIENTE_ANALISIS" "Debe volver a analisis"
    Assert-Igual $rNeg.accionPendiente "REINTENTAR_NOTIFICACION" "Debe quedar accion pendiente de reintento"

    Titulo "5.3 REINTENTAR NOTIFICACION"
    $rReint = Post-Json "/actas/$actaNotifNeg/acciones/reintentar-notificacion"
    MostrarJson $rReint
    Assert-Igual $rReint.bandejaActual "PENDIENTE_NOTIFICACION" "Debe volver a pendiente de notificacion"

    $actaVencida = "ACTA-0005"
    $dVenc = Mostrar-Detalle $actaVencida "5.4 ACTA-0005 INICIAL"
    Assert-Igual $dVenc.bandejaActual "EN_NOTIFICACION" "ACTA-0005 debe iniciar en notificacion"

    Titulo "5.5 REGISTRAR NOTIFICACION VENCIDA"
    $rVenc = Post-Json "/actas/$actaVencida/acciones/registrar-notificacion-vencida"
    MostrarJson $rVenc
    Assert-Igual $rVenc.bandejaActual "PENDIENTE_ANALISIS" "Debe volver a analisis"
    Assert-Igual $rVenc.accionPendiente "EVALUAR_NOTIFICACION_VENCIDA" "Debe quedar accion pendiente de evaluar vencimiento"

    Titulo "6. BANDEJAS FINALES"
    $null = Mostrar-Bandejas "6.1 BANDEJAS AL FINAL DE LA DEMO"

    Write-Host ""
    Write-Host "Demo del prototipo finalizada correctamente." -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "Ocurrio un error durante la demo." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red

    if ($_.Exception.Response -ne $null) {
        Write-Host ""
        Write-Host "StatusCode:" -ForegroundColor DarkYellow
        Write-Host $_.Exception.Response.StatusCode.value__

        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $reader.BaseStream.Position = 0
            $reader.DiscardBufferedData()
            $body = $reader.ReadToEnd()
            Write-Host ""
            Write-Host "Response body:" -ForegroundColor DarkYellow
            Write-Host $body
        }
        catch {
        }
    }

    exit 1
}
