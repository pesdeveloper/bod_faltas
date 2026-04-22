# Demo oficial del prototipo de faltas.
# Requiere el backend levantado en http://localhost:8087
#
# Arranque del backend (desde la raiz del repo):
#   cd .\backend\api-faltas-prototipo
#   mvn spring-boot:run
#
# Ejecucion de la demo (desde la raiz del repo):
#   powershell -ExecutionPolicy Bypass -File .\demo-prototipo-faltas.ps1
#
# Flujo principal: ACTA-0013 (multiples piezas requeridas, firma por documento).
# Flujo secundario: ACTA-0006 (archivo directo desde analisis).
# Flujo terciario: ACTA-0004 (notificacion negativa + reintento).
# Flujo cuaternario: ACTA-0005 (notificacion vencida).
# Verificacion rapida:
#   Invoke-RestMethod http://localhost:8087/api/prototipo/health

chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [System.Text.UTF8Encoding]::new()
$ErrorActionPreference = "Stop"

$baseUrl      = "http://localhost:8087/api/prototipo"
$actaCanonica = "ACTA-0013"
$actaArchivo  = "ACTA-0006"

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------

function Titulo($texto) {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host $texto -ForegroundColor Yellow
    Write-Host "==================================================" -ForegroundColor Cyan
}

function MostrarJson($obj) {
    ($obj | ConvertTo-Json -Depth 20) | Out-Host
}

function Get-Json($url) {
    Invoke-RestMethod -Uri $url -Method Get
}

function Post-Json($url) {
    Invoke-RestMethod -Uri $url -Method Post
}

function Assert-Igual($actual, $esperado, $mensaje) {
    if ($actual -ne $esperado) {
        throw "$mensaje | esperado='$esperado' actual='$actual'"
    }
}

function Assert-True($condicion, $mensaje) {
    if (-not $condicion) {
        throw $mensaje
    }
}

function Mostrar-Detalle($actaId, $titulo) {
    Titulo $titulo
    $detalle = Get-Json "$baseUrl/actas/$actaId"
    MostrarJson $detalle
    return $detalle
}

function Mostrar-Eventos($actaId, $titulo) {
    Titulo $titulo
    $eventos = Get-Json "$baseUrl/actas/$actaId/eventos"
    MostrarJson $eventos
    return $eventos
}

function Mostrar-Documentos($actaId, $titulo) {
    Titulo $titulo
    $documentos = Get-Json "$baseUrl/actas/$actaId/documentos"
    MostrarJson $documentos
    return @($documentos)
}

function Mostrar-Notificaciones($actaId, $titulo) {
    Titulo $titulo
    $notificaciones = Get-Json "$baseUrl/actas/$actaId/notificaciones"
    MostrarJson $notificaciones
    return @($notificaciones)
}

function Mostrar-Bandejas($titulo) {
    Titulo $titulo
    $bandejas = Get-Json "$baseUrl/bandejas"
    MostrarJson $bandejas
    return @($bandejas)
}

function Documentos-En-Estado($actaId, $estado) {
    $todos = Get-Json "$baseUrl/actas/$actaId/documentos"
    return @($todos | Where-Object { $_.estadoDocumento -eq $estado })
}

# -----------------------------------------------------------------------------
# Demo
# -----------------------------------------------------------------------------

try {
    # =========================================================================
    # FLUJO PRINCIPAL: ACTA-0013
    # multiples piezas requeridas -> firma por documento -> notificacion -> cierre
    # =========================================================================

    Titulo "1. RESET INICIAL"
    $reset0 = Post-Json "$baseUrl/reset"
    MostrarJson $reset0

    Titulo "2. HEALTH"
    $health = Get-Json "$baseUrl/health"
    MostrarJson $health

    $null = Mostrar-Bandejas "3. BANDEJAS INICIALES"

    $detalleInicial = Mostrar-Detalle $actaCanonica "4. DETALLE INICIAL $actaCanonica"
    Assert-Igual $detalleInicial.id $actaCanonica "La acta canonica no coincide"
    Assert-Igual $detalleInicial.bandejaActual "PENDIENTES_RESOLUCION_REDACCION" `
        "$actaCanonica deberia iniciar en PENDIENTES_RESOLUCION_REDACCION"

    $null = Mostrar-Eventos $actaCanonica "5. EVENTOS INICIALES $actaCanonica"
    $null = Mostrar-Documentos $actaCanonica "6. DOCUMENTOS INICIALES $actaCanonica"
    $null = Mostrar-Notificaciones $actaCanonica "7. NOTIFICACIONES INICIALES $actaCanonica"

    Titulo "8. ACCION: GENERAR MEDIDA PREVENTIVA"
    $r8 = Post-Json "$baseUrl/actas/$actaCanonica/acciones/generar-medida-preventiva"
    MostrarJson $r8

    $detalleTrasMedida = Mostrar-Detalle $actaCanonica "9. DETALLE DESPUES DE GENERAR MEDIDA PREVENTIVA"
    Assert-Igual $detalleTrasMedida.bandejaActual "PENDIENTES_RESOLUCION_REDACCION" `
        "Mientras falten piezas requeridas, $actaCanonica debe permanecer en PENDIENTES_RESOLUCION_REDACCION"
    Assert-True ($detalleTrasMedida.piezasGeneradas -contains "MEDIDA_PREVENTIVA") `
        "MEDIDA_PREVENTIVA deberia figurar como pieza generada"

    Titulo "10. ACCION: GENERAR NOTIFICACION DEL ACTA"
    $r10 = Post-Json "$baseUrl/actas/$actaCanonica/acciones/generar-notificacion-acta"
    MostrarJson $r10

    $detalleTrasNotif = Mostrar-Detalle $actaCanonica "11. DETALLE DESPUES DE GENERAR NOTIFICACION (DEBE PASAR A PENDIENTE_FIRMA)"
    Assert-Igual $detalleTrasNotif.bandejaActual "PENDIENTE_FIRMA" `
        "Al completar todas las piezas requeridas, $actaCanonica debe pasar a PENDIENTE_FIRMA"
    Assert-True ($detalleTrasNotif.piezasGeneradas -contains "NOTIFICACION_ACTA") `
        "NOTIFICACION_ACTA deberia figurar como pieza generada"

    $null = Mostrar-Documentos $actaCanonica "12. DOCUMENTOS TRAS PASAR A PENDIENTE_FIRMA"

    $pendientesFirma = Documentos-En-Estado $actaCanonica "PENDIENTE_FIRMA"
    Assert-True ($pendientesFirma.Count -ge 2) `
        "$actaCanonica deberia tener al menos 2 documentos en PENDIENTE_FIRMA"

    $primerDoc  = $pendientesFirma[0]
    $segundoDoc = $pendientesFirma[1]

    Titulo "13. ACCION: FIRMAR PRIMER DOCUMENTO ($($primerDoc.id))"
    $r13 = Post-Json "$baseUrl/actas/$actaCanonica/acciones/firmar-documento/$($primerDoc.id)"
    MostrarJson $r13

    $detalleTras1Firma = Mostrar-Detalle $actaCanonica "14. DETALLE DESPUES DE FIRMAR UN SOLO DOCUMENTO (DEBE SEGUIR EN PENDIENTE_FIRMA)"
    Assert-Igual $detalleTras1Firma.bandejaActual "PENDIENTE_FIRMA" `
        "$actaCanonica debe permanecer en PENDIENTE_FIRMA hasta firmar todos los documentos"

    $documentosPostFirma1 = Mostrar-Documentos $actaCanonica "14b. DOCUMENTOS DESPUES DE FIRMAR EL PRIMER DOCUMENTO"
    $firmadosParciales    = @($documentosPostFirma1 | Where-Object { $_.estadoDocumento -eq "FIRMADO" })
    $pendientesParciales  = @($documentosPostFirma1 | Where-Object { $_.estadoDocumento -eq "PENDIENTE_FIRMA" })

    Assert-True ($firmadosParciales.Count -ge 1) "Luego de firmar uno, debe haber al menos 1 documento FIRMADO"
    Assert-True ($pendientesParciales.Count -ge 1) "Luego de firmar uno, debe quedar al menos 1 documento PENDIENTE_FIRMA"

    Titulo "15. ACCION: FIRMAR DOCUMENTO RESTANTE ($($segundoDoc.id))"
    $r15 = Post-Json "$baseUrl/actas/$actaCanonica/acciones/firmar-documento/$($segundoDoc.id)"
    MostrarJson $r15

    $detalleTras2Firma = Mostrar-Detalle $actaCanonica "16. DETALLE DESPUES DE FIRMAR TODOS (DEBE PASAR A PENDIENTE_NOTIFICACION)"
    Assert-Igual $detalleTras2Firma.bandejaActual "PENDIENTE_NOTIFICACION" `
        "Al firmar todos los documentos, $actaCanonica debe pasar a PENDIENTE_NOTIFICACION"

    $null = Mostrar-Documentos $actaCanonica "17. DOCUMENTOS DESPUES DE COMPLETAR FIRMAS"
    $null = Mostrar-Eventos $actaCanonica "18. EVENTOS DESPUES DE COMPLETAR FIRMAS"
    $null = Mostrar-Notificaciones $actaCanonica "19. NOTIFICACIONES ANTES DEL ACUSE POSITIVO"

    Titulo "20. ACCION: REGISTRAR NOTIFICACION POSITIVA"
    $r20 = Post-Json "$baseUrl/actas/$actaCanonica/acciones/registrar-notificacion-positiva"
    MostrarJson $r20

    $detalleTrasAcuse = Mostrar-Detalle $actaCanonica "21. DETALLE DESPUES DE REGISTRAR NOTIFICACION POSITIVA (DEBE PASAR A PENDIENTE_ANALISIS)"
    Assert-Igual $detalleTrasAcuse.bandejaActual "PENDIENTE_ANALISIS" `
        "Tras notificacion positiva, $actaCanonica debe pasar a PENDIENTE_ANALISIS"

    $null = Mostrar-Eventos $actaCanonica "22. EVENTOS DESPUES DEL ACUSE POSITIVO"
    $null = Mostrar-Notificaciones $actaCanonica "23. NOTIFICACIONES DESPUES DEL ACUSE POSITIVO"
    $null = Mostrar-Bandejas "24. BANDEJAS DESPUES DE LLEVAR $actaCanonica A ANALISIS"

    Titulo "25. ACCION: CERRAR $actaCanonica DESDE ANALISIS"
    $r25 = Post-Json "$baseUrl/actas/$actaCanonica/acciones/cerrar-acta"
    MostrarJson $r25

    $detalleFinal = Mostrar-Detalle $actaCanonica "26. DETALLE FINAL DE CIERRE $actaCanonica"
    Assert-True ($detalleFinal.estaCerrada -eq $true) "$actaCanonica deberia quedar cerrada"
    Assert-Igual $detalleFinal.bandejaActual "CERRADAS" "$actaCanonica deberia terminar en bandeja CERRADAS"

    $null = Mostrar-Eventos $actaCanonica "27. EVENTOS FINALES $actaCanonica"
    $null = Mostrar-Bandejas "28. BANDEJAS TRAS CIERRE DE $actaCanonica"

    # =========================================================================
    # FLUJO SECUNDARIO: ACTA-0006
    # archivo directo desde PENDIENTE_ANALISIS
    # =========================================================================

    Titulo "A1. RESET PARA FLUJO DE ARCHIVO"
    $resetArch = Post-Json "$baseUrl/reset"
    MostrarJson $resetArch

    $detalleArchInicial = Mostrar-Detalle $actaArchivo "A2. DETALLE INICIAL $actaArchivo"
    Assert-Igual $detalleArchInicial.id $actaArchivo "La acta de archivo no coincide"
    Assert-Igual $detalleArchInicial.bandejaActual "PENDIENTE_ANALISIS" `
        "$actaArchivo deberia iniciar en PENDIENTE_ANALISIS"

    $null = Mostrar-Eventos $actaArchivo "A3. EVENTOS INICIALES $actaArchivo"
    $null = Mostrar-Notificaciones $actaArchivo "A4. NOTIFICACIONES INICIALES $actaArchivo"

    Titulo "A5. ACCION: ARCHIVAR $actaArchivo DESDE ANALISIS"
    $rArch = Post-Json "$baseUrl/actas/$actaArchivo/acciones/archivar-acta"
    MostrarJson $rArch

    $detalleArchFinal = Mostrar-Detalle $actaArchivo "A6. DETALLE FINAL $actaArchivo"
    Assert-Igual $detalleArchFinal.bandejaActual "ARCHIVO" "$actaArchivo deberia terminar en bandeja ARCHIVO"

    $null = Mostrar-Eventos $actaArchivo "A7. EVENTOS FINALES $actaArchivo"
    $null = Mostrar-Bandejas "A8. BANDEJAS FINALES"

    # =========================================================================
    # FLUJO TERCIARIO: ACTA-0004
    # notificacion negativa -> retorno a analisis con accionPendiente
    # filtro por accionPendiente -> reintentar notificacion -> limpieza
    # =========================================================================

    $actaNotifNeg = "ACTA-0004"

    Titulo "B1. RESET PARA FLUJO NOTIFICACION NEGATIVA"
    $resetB = Post-Json "$baseUrl/reset"
    MostrarJson $resetB

    $detalleB2 = Mostrar-Detalle $actaNotifNeg "B2. DETALLE INICIAL $actaNotifNeg"
    Assert-Igual $detalleB2.bandejaActual "PENDIENTE_NOTIFICACION" `
        "$actaNotifNeg deberia iniciar en PENDIENTE_NOTIFICACION"
    Assert-True ($null -eq $detalleB2.accionPendiente -or $detalleB2.accionPendiente -eq "") `
        "$actaNotifNeg no deberia tener accionPendiente al inicio"

    $null = Mostrar-Notificaciones $actaNotifNeg "B3. NOTIFICACIONES INICIALES $actaNotifNeg"

    Titulo "B4. ACCION: REGISTRAR NOTIFICACION NEGATIVA EN $actaNotifNeg"
    $rB4 = Post-Json "$baseUrl/actas/$actaNotifNeg/acciones/registrar-notificacion-negativa"
    MostrarJson $rB4
    Assert-Igual $rB4.bandejaActual "PENDIENTE_ANALISIS" `
        "Tras notificacion negativa, $actaNotifNeg debe pasar a PENDIENTE_ANALISIS"
    Assert-Igual $rB4.accionPendiente "REINTENTAR_NOTIFICACION" `
        "Tras notificacion negativa, accionPendiente debe ser REINTENTAR_NOTIFICACION"

    $detalleB5 = Mostrar-Detalle $actaNotifNeg "B5. DETALLE TRAS NOTIFICACION NEGATIVA"
    Assert-Igual $detalleB5.bandejaActual "PENDIENTE_ANALISIS" `
        "$actaNotifNeg debe estar en PENDIENTE_ANALISIS tras notificacion negativa"
    Assert-Igual $detalleB5.accionPendiente "REINTENTAR_NOTIFICACION" `
        "accionPendiente debe ser REINTENTAR_NOTIFICACION en detalle del acta"

    $notifB6 = Mostrar-Notificaciones $actaNotifNeg "B6. NOTIFICACIONES TRAS NOTIFICACION NEGATIVA"
    $noEntregada = @($notifB6 | Where-Object { $_.estadoNotificacion -eq "NO_ENTREGADA" })
    Assert-True ($noEntregada.Count -ge 1) `
        "$actaNotifNeg deberia tener al menos una notificacion en estado NO_ENTREGADA"

    $eventosB7 = Mostrar-Eventos $actaNotifNeg "B7. EVENTOS TRAS NOTIFICACION NEGATIVA"
    $evtNegativa = @($eventosB7 | Where-Object { $_.tipoEvento -eq "NOTIFICACION_NO_ENTREGADA" })
    Assert-True ($evtNegativa.Count -ge 1) `
        "$actaNotifNeg deberia registrar al menos un evento NOTIFICACION_NO_ENTREGADA"

    Titulo "B8. FILTRO: BANDEJA PENDIENTE_ANALISIS CON accionPendiente=REINTENTAR_NOTIFICACION"
    $filtradosB8 = @(Get-Json "$baseUrl/bandejas/PENDIENTE_ANALISIS/actas?accionPendiente=REINTENTAR_NOTIFICACION")
    MostrarJson $filtradosB8
    $apareceEnFiltro = @($filtradosB8 | Where-Object { $_.id -eq $actaNotifNeg })
    Assert-True ($apareceEnFiltro.Count -eq 1) `
        "$actaNotifNeg debe aparecer en el filtro por accionPendiente=REINTENTAR_NOTIFICACION"

    Titulo "B9. ACCION: REINTENTAR NOTIFICACION EN $actaNotifNeg"
    $rB9 = Post-Json "$baseUrl/actas/$actaNotifNeg/acciones/reintentar-notificacion"
    MostrarJson $rB9
    Assert-Igual $rB9.bandejaActual "PENDIENTE_NOTIFICACION" `
        "Tras reintentar notificacion, $actaNotifNeg debe volver a PENDIENTE_NOTIFICACION"

    $detalleB10 = Mostrar-Detalle $actaNotifNeg "B10. DETALLE TRAS REINTENTO DE NOTIFICACION"
    Assert-Igual $detalleB10.bandejaActual "PENDIENTE_NOTIFICACION" `
        "$actaNotifNeg debe estar en PENDIENTE_NOTIFICACION tras el reintento"
    Assert-True ($null -eq $detalleB10.accionPendiente -or $detalleB10.accionPendiente -eq "") `
        "accionPendiente debe quedar limpia tras el reintento de notificacion"

    $notifB11 = Mostrar-Notificaciones $actaNotifNeg "B11. NOTIFICACIONES TRAS REINTENTO"
    $pendienteEnvio = @($notifB11 | Where-Object { $_.estadoNotificacion -eq "PENDIENTE_ENVIO" })
    Assert-True ($pendienteEnvio.Count -ge 1) `
        "$actaNotifNeg deberia tener al menos una notificacion en PENDIENTE_ENVIO tras reintento"

    $eventosB12 = Mostrar-Eventos $actaNotifNeg "B12. EVENTOS TRAS REINTENTO"
    $evtReintento = @($eventosB12 | Where-Object { $_.tipoEvento -eq "NOTIFICACION_REINTENTADA" })
    Assert-True ($evtReintento.Count -ge 1) `
        "$actaNotifNeg deberia registrar al menos un evento NOTIFICACION_REINTENTADA"

    Titulo "B13. VERIFICAR QUE $actaNotifNeg YA NO APARECE EN EL FILTRO POR accionPendiente"
    $filtradosB13 = @(Get-Json "$baseUrl/bandejas/PENDIENTE_ANALISIS/actas?accionPendiente=REINTENTAR_NOTIFICACION")
    MostrarJson $filtradosB13
    $desaparecio = @($filtradosB13 | Where-Object { $_.id -eq $actaNotifNeg })
    Assert-True ($desaparecio.Count -eq 0) `
        "$actaNotifNeg no debe aparecer en el filtro por accionPendiente=REINTENTAR_NOTIFICACION tras el reintento"

    $null = Mostrar-Bandejas "B14. BANDEJAS FINALES TRAS FLUJO NOTIFICACION NEGATIVA"

    # =========================================================================
    # FLUJO CUATERNARIO: ACTA-0005
    # notificacion vencida -> retorno a analisis con accionPendiente
    # filtro por accionPendiente=EVALUAR_NOTIFICACION_VENCIDA
    # =========================================================================

    $actaVencida = "ACTA-0005"

    Titulo "C1. RESET PARA FLUJO NOTIFICACION VENCIDA"
    $resetC = Post-Json "$baseUrl/reset"
    MostrarJson $resetC

    $detalleC2 = Mostrar-Detalle $actaVencida "C2. DETALLE INICIAL $actaVencida"
    Assert-Igual $detalleC2.id $actaVencida "La acta de notificacion vencida no coincide"
    Assert-Igual $detalleC2.bandejaActual "EN_NOTIFICACION" `
        "$actaVencida deberia iniciar en EN_NOTIFICACION"
    Assert-True ($null -eq $detalleC2.accionPendiente -or $detalleC2.accionPendiente -eq "") `
        "$actaVencida no deberia tener accionPendiente al inicio"

    $null = Mostrar-Notificaciones $actaVencida "C3. NOTIFICACIONES INICIALES $actaVencida"

    Titulo "C4. ACCION: REGISTRAR NOTIFICACION VENCIDA EN $actaVencida"
    $rC4 = Post-Json "$baseUrl/actas/$actaVencida/acciones/registrar-notificacion-vencida"
    MostrarJson $rC4
    Assert-Igual $rC4.bandejaActual "PENDIENTE_ANALISIS" `
        "Tras notificacion vencida, $actaVencida debe pasar a PENDIENTE_ANALISIS"
    Assert-Igual $rC4.accionPendiente "EVALUAR_NOTIFICACION_VENCIDA" `
        "Tras notificacion vencida, accionPendiente debe ser EVALUAR_NOTIFICACION_VENCIDA"

    $detalleC5 = Mostrar-Detalle $actaVencida "C5. DETALLE TRAS NOTIFICACION VENCIDA"
    Assert-Igual $detalleC5.bandejaActual "PENDIENTE_ANALISIS" `
        "$actaVencida debe estar en PENDIENTE_ANALISIS tras notificacion vencida"
    Assert-Igual $detalleC5.accionPendiente "EVALUAR_NOTIFICACION_VENCIDA" `
        "accionPendiente debe ser EVALUAR_NOTIFICACION_VENCIDA en detalle del acta"

    $notifC6 = Mostrar-Notificaciones $actaVencida "C6. NOTIFICACIONES TRAS NOTIFICACION VENCIDA"
    $vencidas = @($notifC6 | Where-Object { $_.estadoNotificacion -eq "VENCIDA" })
    Assert-True ($vencidas.Count -ge 1) `
        "$actaVencida deberia tener al menos una notificacion en estado VENCIDA"

    $eventosC7 = Mostrar-Eventos $actaVencida "C7. EVENTOS TRAS NOTIFICACION VENCIDA"
    $evtVencida = @($eventosC7 | Where-Object { $_.tipoEvento -eq "NOTIFICACION_VENCIDA" })
    Assert-True ($evtVencida.Count -ge 1) `
        "$actaVencida deberia registrar al menos un evento NOTIFICACION_VENCIDA"

    Titulo "C8. FILTRO: BANDEJA PENDIENTE_ANALISIS CON accionPendiente=EVALUAR_NOTIFICACION_VENCIDA"
    $filtradosC8 = @(Get-Json "$baseUrl/bandejas/PENDIENTE_ANALISIS/actas?accionPendiente=EVALUAR_NOTIFICACION_VENCIDA")
    MostrarJson $filtradosC8
    $apareceEnFiltroC = @($filtradosC8 | Where-Object { $_.id -eq $actaVencida })
    Assert-True ($apareceEnFiltroC.Count -eq 1) `
        "$actaVencida debe aparecer en el filtro por accionPendiente=EVALUAR_NOTIFICACION_VENCIDA"

    $null = Mostrar-Bandejas "C9. BANDEJAS FINALES TRAS FLUJO NOTIFICACION VENCIDA"

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