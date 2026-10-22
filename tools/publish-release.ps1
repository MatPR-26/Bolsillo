param(
 [string]$Sdk="$PSScriptRoot/../.tools/sdk",
 [string]$Jdk="$PSScriptRoot/../.tools/jdk/jdk-17.0.20.1+1",
 [string]$Gh='gh',
 [string]$NotesFile="$PSScriptRoot/../CHANGELOG.md",
 [string]$Apk='',
 [string]$BuildInfo='',
 [switch]$Publish
)
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath("$PSScriptRoot/..")
[xml]$taskManifest=Get-Content -LiteralPath "$taskRoot/source/android/AndroidManifest.xml" -Raw
$taskCode=[int]$taskManifest.manifest.GetAttribute('versionCode','http://schemas.android.com/apk/res/android')
$taskTag="v$taskCode.0"
[xml]$taskFeed=Get-Content -LiteralPath "$taskRoot/source/android/res/values/updates.xml" -Raw
$taskRepo=[string]$taskFeed.resources.string.'#text'
if ($taskRepo -notmatch '^[A-Za-z0-9-]+/[A-Za-z0-9_.-]+$') { throw 'Repositorio inválido' }
if ([string]::IsNullOrEmpty($Apk)) { $Apk="$taskRoot/dist/Bolsillo-$taskCode.apk" }
if ([string]::IsNullOrEmpty($BuildInfo)) { $BuildInfo="$taskRoot/dist/build-info.json" }
$taskIdentity=Get-Content -LiteralPath $BuildInfo -Raw | ConvertFrom-Json
$taskHash=(Get-FileHash -LiteralPath $Apk -Algorithm SHA256).Hash.ToLowerInvariant()
if ($taskIdentity.apkSha256 -ne $taskHash -or $taskIdentity.versionCode -ne $taskCode) { throw 'El APK no coincide con la compilación actual' }
foreach($taskSource in $taskIdentity.productionFiles) {
 if ($taskSource.file -notmatch '^source/android/(src/|res/|licenses/|AndroidManifest\.xml$)' -or $taskSource.file -match '(^|/)\.\.(/|$)') { throw 'Ruta inesperada en la identidad' }
 if ((Get-FileHash -LiteralPath (Join-Path $taskRoot $taskSource.file) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $taskSource.sha256) { throw "Código cambiado desde la compilación: $($taskSource.file)" }
}
$taskBt=Join-Path $Sdk 'build-tools/android-16'
if (!(Test-Path $taskBt)) { $taskBt=Join-Path $Sdk 'build-tools/36.0.0' }
$taskSignature=& "$Jdk/bin/java.exe" -jar "$taskBt/lib/apksigner.jar" verify --print-certs $Apk
if ($LASTEXITCODE -ne 0 -or ($taskSignature -join "`n") -notmatch 'certificate SHA-256 digest: 55328fdbbbb057047b1ce35dc1541c9bac300f4233bce90bb3121478faafe526') { throw 'Firma distinta de la distribución oficial' }
$taskCommit=(& git -C $taskRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Guarda y sube primero el código revisado' }
if (@(& git -C $taskRoot status --porcelain).Count -gt 0) { throw 'El repositorio tiene cambios pendientes' }
$taskRemote=(& $Gh api "repos/$taskRepo/commits/main" --jq '.sha').Trim()
if ($LASTEXITCODE -ne 0 -or $taskRemote -ne $taskCommit) { throw 'Sube a main exactamente el código que compilaste antes de publicar' }
& $Gh release view $taskTag --repo $taskRepo --json tagName 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) { throw 'La versión ya existe. Aumenta versionCode; no sobrescribas un APK distribuido.' }
$taskSum=Join-Path ([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($Apk))) 'SHA256SUMS.txt'
[IO.File]::WriteAllText($taskSum,"$taskHash  Bolsillo-$taskCode.apk`n",[Text.UTF8Encoding]::new($false))
& $Gh release create $taskTag --repo $taskRepo --target $taskCommit --title "Bolsillo $taskCode" --notes-file $NotesFile --draft $Apk $taskSum
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la release' }
if ($Publish) {
 & $Gh release edit $taskTag --repo $taskRepo --draft=false --latest
 if ($LASTEXITCODE -ne 0) { throw 'No se pudo publicar; la release sigue como borrador' }
}
& $Gh release view $taskTag --repo $taskRepo --json url,isDraft,assets
