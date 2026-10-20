param([string]$Jdk="$PSScriptRoot/../../.tools/jdk/jdk-17.0.20.1+1",[string]$Sdk="$PSScriptRoot/../../.tools/sdk")
$ErrorActionPreference='Stop'
$out=[IO.Path]::GetFullPath("$PSScriptRoot/../../dist")
$json=[IO.Path]::GetFullPath("$PSScriptRoot/../../.tools/json.jar")
if (!(Test-Path -LiteralPath $json)) {
 New-Item -ItemType Directory -Force (Split-Path $json) | Out-Null
 Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar' -OutFile $json
}
if ((Get-FileHash -LiteralPath $json -Algorithm SHA256).Hash.ToLowerInvariant() -ne '3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed') { throw 'La huella de la dependencia de pruebas no coincide' }
New-Item -ItemType Directory -Force "$out/test-classes" | Out-Null
$cp="$json;$out/test-classes;$out/production-classes;$Sdk/platforms/android-36/android.jar"
& "$Jdk/bin/javac.exe" -encoding UTF-8 -cp $cp -d "$out/test-classes" "$PSScriptRoot/tests/ParserTest.java" "$PSScriptRoot/tests/LedgerTest.java" "$PSScriptRoot/tests/LearnedRulesTest.java" "$PSScriptRoot/tests/AlertEventsTest.java" "$PSScriptRoot/tests/CatalogChartsTest.java" "$PSScriptRoot/tests/PublicReleaseTest.java"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& "$Jdk/bin/java.exe" -cp $cp ParserTest
if($LASTEXITCODE -ne 0){throw 'Parser tests failed'}
& "$Jdk/bin/java.exe" -cp $cp cl.bolsillo.app.LedgerTest
if($LASTEXITCODE -ne 0){throw 'Accounting tests failed'}



& "$Jdk/bin/java.exe" -cp $cp cl.bolsillo.app.LearnedRulesTest
if($LASTEXITCODE -ne 0){throw 'Local learning tests failed'}
& "$Jdk/bin/java.exe" -cp $cp cl.bolsillo.app.AlertEventsTest
if($LASTEXITCODE -ne 0){throw 'Alert event tests failed'}

& "$Jdk/bin/java.exe" -cp $cp cl.bolsillo.app.CatalogChartsTest
if($LASTEXITCODE -ne 0){throw "Catalog/charts tests failed"}

& "$Jdk/bin/java.exe" -cp $cp cl.bolsillo.app.PublicReleaseTest
if($LASTEXITCODE -ne 0){throw "Public release and shared expense tests failed"}
