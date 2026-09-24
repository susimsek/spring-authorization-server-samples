$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$hooksDirectory = Join-Path $repositoryRoot '.githooks'
$prePushHook = Join-Path $hooksDirectory 'pre-push'

if (-not (Test-Path -LiteralPath $prePushHook -PathType Leaf)) {
    throw "Pre-push hook bulunamadı: $prePushHook"
}

Push-Location $repositoryRoot
try {
    git config core.hooksPath .githooks
    if ($LASTEXITCODE -ne 0) {
        throw 'Git hooks yolu ayarlanamadı.'
    }
}
finally {
    Pop-Location
}

$userToken = [Environment]::GetEnvironmentVariable('SONARQUBE_TOKEN', 'User')
$processToken = [Environment]::GetEnvironmentVariable('SONARQUBE_TOKEN', 'Process')

Write-Host 'Git hooks etkinleştirildi: .githooks'
if ([string]::IsNullOrWhiteSpace($userToken) -and [string]::IsNullOrWhiteSpace($processToken)) {
    Write-Warning 'SONARQUBE_TOKEN tanımlı değil. SonarCloud taraması pre-push sırasında atlanacaktır.'
    Write-Host 'Tokenı güvenli şekilde Windows User ortam değişkeni olarak tanımlayın; repoya yazmayın.'
} else {
    Write-Host 'SONARQUBE_TOKEN bulundu; pre-push SonarCloud taraması çalıştırabilir.'
}
