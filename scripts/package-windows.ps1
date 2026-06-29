param(
    [string]$OutputDirectory = "target\windows"
)

$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$targetDir = Join-Path $projectRoot "target"
$inputDir = Join-Path $targetDir "jpackage-input"
$appImageDir = Join-Path $projectRoot $OutputDirectory

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE"
    }
}

Set-Location $projectRoot

Invoke-Native "mvn" "-q" "package"

if (Test-Path $inputDir) {
    Remove-Item -LiteralPath $inputDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
Copy-Item -LiteralPath (Join-Path $targetDir "image-coordinates-tool-1.0.0.jar") -Destination $inputDir
Copy-Item -LiteralPath (Join-Path $targetDir "lib") -Destination (Join-Path $inputDir "lib") -Recurse

if (Test-Path $appImageDir) {
    Remove-Item -LiteralPath $appImageDir -Recurse -Force
}

Invoke-Native "jpackage" `
    --type app-image `
    --name ImageCoordinatesTool `
    --dest $appImageDir `
    --input $inputDir `
    --main-jar image-coordinates-tool-1.0.0.jar `
    --main-class ru.qa.tools.ImageCoordinatesTool `
    --java-options "-Dfile.encoding=UTF-8"

Write-Host "Created: $(Join-Path $appImageDir 'ImageCoordinatesTool\ImageCoordinatesTool.exe')"
