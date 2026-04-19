# powershell -ExecutionPolicy Bypass -File .\demo-prototipo-faltas-completa.ps1
chcp 65001
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [System.Text.UTF8Encoding]::new()
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8087/api/prototipo"

$actaFirma = "ACTA-0003"     # flujo firma -> notificación -> análisis
$actaAnalisis = "ACTA-0006"  # flujo cierre / archivo directo desde análisis

function Titulo($texto) {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host $texto -ForegroundColor Yellow
    Write-Host "==================================================" -ForegroundColor Cyan
}

function MostrarJson($obj) {
    $obj | ConvertTo-Json -Depth 10
}

function Get-Json($url) {
    Invoke-RestMethod -Uri $url -Method Get
}

function Post-Json($url) {
    Invoke-RestMethod -Uri $url -Method Post
}

try {
    Titulo "0. RESET INICIAL"
    $r0 = Post-Json "$baseUrl/reset"
    MostrarJson $r0

    Titulo "1. HEALTH"
    $r1 = Get-Json "$baseUrl/health"
    MostrarJson $r1

    Titulo "2. BANDEJAS INICIALES"
    $r2 = Get-Json "$baseUrl/bandejas"
    MostrarJson $r2

    Titulo "3. DETALLE INICIAL ACTA-0003"
    $r3 = Get-Json "$baseUrl/actas/$actaFirma"
    MostrarJson $r3

    Titulo "4. EVENTOS INICIALES ACTA-0003"
    $r4 = Get-Json "$baseUrl/actas/$actaFirma/eventos"
    MostrarJson $r4

    Titulo "5. NOTIFICACIONES INICIALES ACTA-0003"
    $r5 = Get-Json "$baseUrl/actas/$actaFirma/notificaciones"
    MostrarJson $r5

    Titulo "6. ACCION: PASAR A NOTIFICACION (ACTA-0003)"
    $r6 = Post-Json "$baseUrl/actas/$actaFirma/acciones/pasar-a-notificacion"
    MostrarJson $r6

    Titulo "7. DETALLE DESPUES DE PASAR A NOTIFICACION (ACTA-0003)"
    $r7 = Get-Json "$baseUrl/actas/$actaFirma"
    MostrarJson $r7

    Titulo "8. EVENTOS DESPUES DE PASAR A NOTIFICACION (ACTA-0003)"
    $r8 = Get-Json "$baseUrl/actas/$actaFirma/eventos"
    MostrarJson $r8

    Titulo "9. NOTIFICACIONES DESPUES DE PASAR A NOTIFICACION (ACTA-0003)"
    $r9 = Get-Json "$baseUrl/actas/$actaFirma/notificaciones"
    MostrarJson $r9

    Titulo "10. ACCION: REGISTRAR NOTIFICACION POSITIVA (ACTA-0003)"
    $r10 = Post-Json "$baseUrl/actas/$actaFirma/acciones/registrar-notificacion-positiva"
    MostrarJson $r10

    Titulo "11. DETALLE DESPUES DE REGISTRAR NOTIFICACION POSITIVA (ACTA-0003)"
    $r11 = Get-Json "$baseUrl/actas/$actaFirma"
    MostrarJson $r11

    Titulo "12. EVENTOS DESPUES DE REGISTRAR NOTIFICACION POSITIVA (ACTA-0003)"
    $r12 = Get-Json "$baseUrl/actas/$actaFirma/eventos"
    MostrarJson $r12

    Titulo "13. NOTIFICACIONES DESPUES DE REGISTRAR NOTIFICACION POSITIVA (ACTA-0003)"
    $r13 = Get-Json "$baseUrl/actas/$actaFirma/notificaciones"
    MostrarJson $r13

    Titulo "14. BANDEJAS TRAS LLEVAR ACTA-0003 A ANALISIS"
    $r14 = Get-Json "$baseUrl/bandejas"
    MostrarJson $r14

    Titulo "15. ACCION: CERRAR ACTA DESDE ANALISIS (ACTA-0003)"
    $r15 = Post-Json "$baseUrl/actas/$actaFirma/acciones/cerrar-acta"
    MostrarJson $r15

    Titulo "16. DETALLE FINAL FLUJO CIERRE (ACTA-0003)"
    $r16 = Get-Json "$baseUrl/actas/$actaFirma"
    MostrarJson $r16

    Titulo "17. EVENTOS FINALES FLUJO CIERRE (ACTA-0003)"
    $r17 = Get-Json "$baseUrl/actas/$actaFirma/eventos"
    MostrarJson $r17

    Titulo "18. BANDEJAS DESPUES DEL CIERRE"
    $r18 = Get-Json "$baseUrl/bandejas"
    MostrarJson $r18

    Titulo "19. RESET PARA FLUJO DE ARCHIVO"
    $r19 = Post-Json "$baseUrl/reset"
    MostrarJson $r19

    Titulo "20. DETALLE INICIAL ACTA-0006"
    $r20 = Get-Json "$baseUrl/actas/$actaAnalisis"
    MostrarJson $r20

    Titulo "21. EVENTOS INICIALES ACTA-0006"
    $r21 = Get-Json "$baseUrl/actas/$actaAnalisis/eventos"
    MostrarJson $r21

    Titulo "22. NOTIFICACIONES INICIALES ACTA-0006"
    $r22 = Get-Json "$baseUrl/actas/$actaAnalisis/notificaciones"
    MostrarJson $r22

    Titulo "23. ACCION: ARCHIVAR ACTA DESDE ANALISIS (ACTA-0006)"
    $r23 = Post-Json "$baseUrl/actas/$actaAnalisis/acciones/archivar-acta"
    MostrarJson $r23

    Titulo "24. DETALLE FINAL FLUJO ARCHIVO (ACTA-0006)"
    $r24 = Get-Json "$baseUrl/actas/$actaAnalisis"
    MostrarJson $r24

    Titulo "25. EVENTOS FINALES FLUJO ARCHIVO (ACTA-0006)"
    $r25 = Get-Json "$baseUrl/actas/$actaAnalisis/eventos"
    MostrarJson $r25

    Titulo "26. BANDEJAS DESPUES DEL ARCHIVO"
    $r26 = Get-Json "$baseUrl/bandejas"
    MostrarJson $r26

    Write-Host ""
    Write-Host "Demo completa finalizada correctamente." -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "Ocurrió un error durante la demo." -ForegroundColor Red
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
}