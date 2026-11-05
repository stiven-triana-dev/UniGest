$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Configurar-Entorno.ps1')
Push-Location $proyecto
try {
    & $EjecutableMaven clean test
    if ($LASTEXITCODE -ne 0) { throw "Las pruebas fallaron (código $LASTEXITCODE)." }
    Write-Host 'Pruebas completadas correctamente.' -ForegroundColor Green
}
finally {
    Pop-Location
}
