# Se carga desde los otros scripts para no guardar secretos en el proyecto.
if ([string]::IsNullOrWhiteSpace($env:DB_URL)) {
    $env:DB_URL = 'jdbc:postgresql://localhost:5432/unigest'
}
if ([string]::IsNullOrWhiteSpace($env:DB_USER)) {
    $env:DB_USER = 'postgres'
}
if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
    $segura = Read-Host 'Contraseña de PostgreSQL' -AsSecureString
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $segura).Password
}
if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
    throw 'PostgreSQL requiere la contraseña DB_PASSWORD.'
}

$posiblesMaven = @(
    'mvn.cmd',
    'mvn'
)
$script:EjecutableMaven = $null
foreach ($candidato in $posiblesMaven) {
    if (Test-Path $candidato) {
        $script:EjecutableMaven = $candidato
        break
    }
    $comando = Get-Command $candidato -ErrorAction SilentlyContinue
    if ($comando) {
        $script:EjecutableMaven = $comando.Source
        break
    }
}
if (-not $script:EjecutableMaven) {
    throw 'No se encontró Maven. Revisa su instalación o agrega mvn al PATH.'
}
