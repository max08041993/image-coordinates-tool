param(
    [string]$OutputDirectory = "target\installer"
)

$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$targetDir = Join-Path $projectRoot "target"
$inputDir = Join-Path $targetDir "jpackage-installer-input"
$installerDir = Join-Path $projectRoot $OutputDirectory
$iconPath = Join-Path $projectRoot "src\main\windows\app-icon.ico"
$associationDir = Join-Path $projectRoot "src\main\windows\associations"
$localWixDir = Join-Path $projectRoot "tools\wix"

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

function Assert-WixToolset {
    if ((Test-Path (Join-Path $localWixDir "candle.exe")) -and (Test-Path (Join-Path $localWixDir "light.exe"))) {
        $env:PATH = "$localWixDir;$env:PATH"
        return
    }

    if ((Get-Command candle.exe -ErrorAction SilentlyContinue) -and (Get-Command light.exe -ErrorAction SilentlyContinue)) {
        return
    }

    throw "WiX Toolset 3.x is required for jpackage Windows installers. Install it system-wide or extract wix314-binaries.zip into tools\wix."
}

Set-Location $projectRoot
Assert-WixToolset

Invoke-Native "mvn" "-q" "package"

if (Test-Path $inputDir) {
    Remove-Item -LiteralPath $inputDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
Copy-Item -LiteralPath (Join-Path $targetDir "image-coordinates-tool-1.0.0.jar") -Destination $inputDir
Copy-Item -LiteralPath (Join-Path $targetDir "lib") -Destination (Join-Path $inputDir "lib") -Recurse

if (Test-Path $installerDir) {
    Remove-Item -LiteralPath $installerDir -Recurse -Force
}

Invoke-Native "jpackage" `
    --type exe `
    --name ImageCoordinatesTool `
    --app-version 1.0.0 `
    --vendor "QA Tools" `
    --dest $installerDir `
    --input $inputDir `
    --icon $iconPath `
    --main-jar image-coordinates-tool-1.0.0.jar `
    --main-class ru.qa.tools.ImageCoordinatesToolLauncher `
    --java-options "-Dfile.encoding=UTF-8" `
    --win-menu `
    --win-menu-group "Image Coordinates Tool" `
    --win-shortcut `
    --win-dir-chooser `
    --win-per-user-install `
    --file-associations (Join-Path $associationDir "png.properties") `
    --file-associations (Join-Path $associationDir "jpg.properties") `
    --file-associations (Join-Path $associationDir "jpeg.properties")

Write-Host "Created installer in: $installerDir"
