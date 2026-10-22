param(
 [string]$Sdk = "$PSScriptRoot/../../.tools/sdk",
 [string]$Jdk = "$PSScriptRoot/../../.tools/jdk/jdk-17.0.20.1+1",
 [string]$Out = "$PSScriptRoot/../../dist",
 [string]$Key = "$PSScriptRoot/../../.signing/bolsillo-local.jks",
 [switch]$Unsigned
)
$ErrorActionPreference = 'Stop'
if (!$Unsigned -and !(Test-Path -LiteralPath $Key)) { throw 'Falta la clave de firma local. No se genera una clave nueva para una actualización.' }
if (!$Unsigned -and [string]::IsNullOrWhiteSpace($env:BOLSILLO_STORE_PASSWORD)) { throw 'Configura BOLSILLO_STORE_PASSWORD en esta sesión; nunca lo guardes en el repositorio.' }
if ([string]::IsNullOrWhiteSpace($env:BOLSILLO_KEY_PASSWORD)) { $env:BOLSILLO_KEY_PASSWORD = $env:BOLSILLO_STORE_PASSWORD }

[xml]$taskManifest = Get-Content -LiteralPath "$PSScriptRoot/AndroidManifest.xml" -Raw
$taskVersion = [int]$taskManifest.manifest.GetAttribute('versionCode','http://schemas.android.com/apk/res/android')
if ($taskVersion -lt 1) { throw 'Versión de Android inválida' }
$taskApkName = "Bolsillo-$taskVersion.apk"
$bt = Join-Path $Sdk 'build-tools/android-16'
if (!(Test-Path $bt)) { $bt = Join-Path $Sdk 'build-tools/36.0.0' }
$platform = Join-Path $Sdk 'platforms/android-36/android.jar'
$java = Join-Path $Jdk 'bin/java.exe'
$javac = Join-Path $Jdk 'bin/javac.exe'
function Run([string]$File, [string[]]$Arguments) {
 & $File @Arguments
 if ($LASTEXITCODE -ne 0) { throw "Falló $File ($LASTEXITCODE)" }
}
# Rebuild derived files so deleted classes cannot remain in an update APK.
$outputRoot = [IO.Path]::GetFullPath($Out).TrimEnd([IO.Path]::DirectorySeparatorChar)
foreach ($buildLeaf in @('generated','production-classes','dex')) {
 $buildTarget = [IO.Path]::GetFullPath((Join-Path $outputRoot $buildLeaf))
 if (![string]::Equals((Split-Path $buildTarget -Parent),$outputRoot,[StringComparison]::OrdinalIgnoreCase)) { throw 'Directorio de compilación fuera de la salida' }
 if (Test-Path -LiteralPath $buildTarget) { Remove-Item -LiteralPath $buildTarget -Recurse -Force }
}
New-Item -ItemType Directory -Force $Out,"$Out/generated","$Out/production-classes","$Out/dex" | Out-Null
Run "$bt/aapt2.exe" @('compile','--dir',"$PSScriptRoot/res",'-o',"$Out/resources.zip")
Run "$bt/aapt2.exe" @('link','-o',"$Out/base.apk",'-I',$platform,'--manifest',"$PSScriptRoot/AndroidManifest.xml",'--custom-package','cl.bolsillo.app','--java',"$Out/generated","$Out/resources.zip",'--min-sdk-version','26','--target-sdk-version','36')
$sources = @(Get-ChildItem "$PSScriptRoot/src","$Out/generated" -Filter '*.java' -Recurse | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' })
[IO.File]::WriteAllLines("$Out/sources.txt",$sources,[Text.UTF8Encoding]::new($false))
Run $javac @('-encoding','UTF-8','-source','8','-target','8','-classpath',$platform,'-d',"$Out/production-classes","@$Out/sources.txt")
Run (Join-Path $Jdk 'bin/jar.exe') @('cf',"$Out/classes.jar",'-C',"$Out/production-classes",'cl/bolsillo/app')
Run $java @('-cp',"$bt/lib/d8.jar",'com.android.tools.r8.D8','--lib',$platform,'--min-api','26','--output',"$Out/dex","$Out/classes.jar")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open("$Out/base.apk",[IO.Compression.ZipArchiveMode]::Update)
try {
 Get-ChildItem "$Out/dex" -Filter '*.dex' | ForEach-Object { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,$_.Name) | Out-Null }
 $taskLicenses=Join-Path $PSScriptRoot 'licenses'
 Get-ChildItem -LiteralPath $taskLicenses -File -Recurse | ForEach-Object {
  $taskEntry='assets/licenses/'+$_.FullName.Substring($taskLicenses.Length+1).Replace('\','/')
  [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,$taskEntry) | Out-Null
 }
} finally { $zip.Dispose() }
Run "$bt/zipalign.exe" @('-f','4',"$Out/base.apk","$Out/aligned.apk")
if ($Unsigned) { Copy-Item -LiteralPath "$Out/aligned.apk" -Destination "$Out/Bolsillo-$taskVersion-unsigned.apk" -Force; Write-Output 'Compilación sin firma; solo para validar código.'; return }
Run $java @('-jar',"$bt/lib/apksigner.jar",'sign','--ks',$Key,'--ks-pass','env:BOLSILLO_STORE_PASSWORD','--key-pass','env:BOLSILLO_KEY_PASSWORD','--out',"$Out/$taskApkName","$Out/aligned.apk")
Run $java @('-jar',"$bt/lib/apksigner.jar",'verify','--verbose',"$Out/$taskApkName")
Get-FileHash "$Out/$taskApkName" -Algorithm SHA256




$taskRoot=[IO.Path]::GetFullPath("$PSScriptRoot/../..")
$taskProduction=@(Get-Item -LiteralPath "$PSScriptRoot/AndroidManifest.xml") + @(Get-ChildItem -LiteralPath "$PSScriptRoot/src","$PSScriptRoot/res","$PSScriptRoot/licenses" -File -Recurse)
$taskFiles=@($taskProduction | Sort-Object FullName | ForEach-Object {
 [ordered]@{file=$_.FullName.Substring($taskRoot.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
})
[ordered]@{versionCode=$taskVersion;package='cl.bolsillo.app.local';apkSha256=(Get-FileHash -LiteralPath "$Out/$taskApkName" -Algorithm SHA256).Hash.ToLowerInvariant();productionFiles=$taskFiles} | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$Out/build-info.json" -Encoding UTF8
