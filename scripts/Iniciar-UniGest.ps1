$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Configurar-Entorno.ps1')
Push-Location $proyecto
try {
    & $EjecutableMaven spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "UniGest no inició (código $LASTEXITCODE)." }
}
finally {
    Pop-Location
}
