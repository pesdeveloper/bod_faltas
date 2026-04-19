# powershell -ExecutionPolicy Bypass -File .\probar-prototipo-faltas.ps1
chcp 65001
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [System.Text.UTF8Encoding]::new()

irm http://localhost:8087/api/prototipo/reset -Method Post
irm http://localhost:8087/api/prototipo/bandejas
irm http://localhost:8087/api/prototipo/actas/ACTA-0003
irm http://localhost:8087/api/prototipo/actas/ACTA-0003/acciones/pasar-a-notificacion -Method Post
irm http://localhost:8087/api/prototipo/actas/ACTA-0003

$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8087/api/prototipo"
$actaId = "ACTA-0003"

function Titulo($texto) {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host $texto -ForegroundColor Yellow
    Write-Host "==================================================" -ForegroundColor Cyan
}

function MostrarJson($obj) {
    $obj | ConvertTo-Json -Depth 10
}

try {
    Titulo "1. HEALTH"
    $r1 = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
    MostrarJson $r1

    Titulo "2. BANDEJAS"
    $r2 = Invoke-RestMethod -Uri "$baseUrl/bandejas" -Method Get
    MostrarJson $r2

    Titulo "3. ACTAS EN PENDIENTE_FIRMA"
    $r3 = Invoke-RestMethod -Uri "$baseUrl/bandejas/PENDIENTE_FIRMA/actas" -Method Get
    MostrarJson $r3

    Titulo "4. DETALLE ANTES DE LA ACCION"
    $r4 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId" -Method Get
    MostrarJson $r4

    Titulo "5. EVENTOS ANTES DE LA ACCION"
    $r5 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId/eventos" -Method Get
    MostrarJson $r5

    Titulo "6. NOTIFICACIONES ANTES DE LA ACCION"
    $r6 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId/notificaciones" -Method Get
    MostrarJson $r6

    Titulo "7. EJECUTAR ACCION pasar-a-notificacion"
    $r7 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId/acciones/pasar-a-notificacion" -Method Post
    MostrarJson $r7

    Titulo "8. DETALLE DESPUES DE LA ACCION"
    $r8 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId" -Method Get
    MostrarJson $r8

    Titulo "9. EVENTOS DESPUES DE LA ACCION"
    $r9 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId/eventos" -Method Get
    MostrarJson $r9

    Titulo "10. NOTIFICACIONES DESPUES DE LA ACCION"
    $r10 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId/notificaciones" -Method Get
    MostrarJson $r10

    Titulo "11. ACTAS EN PENDIENTE_NOTIFICACION"
    $r11 = Invoke-RestMethod -Uri "$baseUrl/bandejas/PENDIENTE_NOTIFICACION/actas" -Method Get
    MostrarJson $r11

    Titulo "12. RESET"
    $r12 = Invoke-RestMethod -Uri "$baseUrl/reset" -Method Post
    MostrarJson $r12

    Titulo "13. DETALLE DESPUES DEL RESET"
    $r13 = Invoke-RestMethod -Uri "$baseUrl/actas/$actaId" -Method Get
    MostrarJson $r13

    Write-Host ""
    Write-Host "Prueba completada." -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "Ocurrio un error durante la prueba." -ForegroundColor Red
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