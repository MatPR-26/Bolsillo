param(
 [string]$Sdk="$PSScriptRoot/../.tools/sdk",
 [string]$Jdk="$PSScriptRoot/../.tools/jdk/jdk-17.0.20.1+1",
 [string]$Out="$PSScriptRoot/../dist",
 [string]$Key="$PSScriptRoot/../.signing/bolsillo-local.jks",
 [string]$CredentialFile="$PSScriptRoot/../.signing/signing-credential.dpapi"
)
$ErrorActionPreference='Stop'
$taskOldStore=$env:BOLSILLO_STORE_PASSWORD
$taskOldKey=$env:BOLSILLO_KEY_PASSWORD
$taskBytes=$null
$taskCredential=$null
try {
 if ([string]::IsNullOrWhiteSpace($env:BOLSILLO_STORE_PASSWORD)) {
  if (!(Test-Path -LiteralPath $CredentialFile)) { throw 'Configura la credencial privada local o las variables de firma del proceso.' }
  Add-Type -AssemblyName System.Security
  $taskBytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes([IO.Path]::GetFullPath($CredentialFile)),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
  $taskCredential=[Text.Encoding]::UTF8.GetString($taskBytes)|ConvertFrom-Json
  if ([string]::IsNullOrWhiteSpace($taskCredential.storePassword) -or [string]::IsNullOrWhiteSpace($taskCredential.keyPassword)) { throw 'Credencial local incompleta.' }
  $env:BOLSILLO_STORE_PASSWORD=$taskCredential.storePassword
  $env:BOLSILLO_KEY_PASSWORD=$taskCredential.keyPassword
 }
 & "$PSScriptRoot/../source/android/build.ps1" -Sdk $Sdk -Jdk $Jdk -Out $Out -Key $Key
} finally {
 $env:BOLSILLO_STORE_PASSWORD=$taskOldStore
 $env:BOLSILLO_KEY_PASSWORD=$taskOldKey
 if ($null -ne $taskBytes) { [Array]::Clear($taskBytes,0,$taskBytes.Length) }
 $taskCredential=$null
}
