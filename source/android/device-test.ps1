param([string]$Serial='emulator-5580',[switch]$CompileOnly)
$ErrorActionPreference='Stop'
$base=[IO.Path]::GetFullPath("$PSScriptRoot/../..")
$jdk="$base/.tools/jdk/jdk-17.0.20.1+1"
$sdk="$base/.tools/sdk"
$bt="$sdk/build-tools/android-16"
$out="$base/dist/device-tests"
New-Item -ItemType Directory -Force $out,"$out/classes","$out/dex" | Out-Null
@'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="cl.bolsillo.tests"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><uses-permission android:name="android.permission.POST_NOTIFICATIONS"/><application android:label="Bolsillo Synthetic Tests"><activity android:name="cl.bolsillo.app.Notifier" android:exported="true"/></application><instrumentation android:name="cl.bolsillo.app.DeviceTests" android:targetPackage="cl.bolsillo.app.local"/></manifest>
'@ | Set-Content "$out/AndroidManifest.xml"
function Run([string]$File,[string[]]$Arguments){& $File @Arguments;if($LASTEXITCODE -ne 0){throw "Failed: $File"}}
Run "$bt/aapt2.exe" @('link','-o',"$out/base.apk",'-I',"$sdk/platforms/android-36/android.jar",'--manifest',"$out/AndroidManifest.xml")
Run "$jdk/bin/javac.exe" @('-encoding','UTF-8','-source','8','-target','8','-cp',"$sdk/platforms/android-36/android.jar;$base/dist/production-classes",'-d',"$out/classes","$PSScriptRoot/tests/DeviceTests.java","$PSScriptRoot/tests/Notifier.java","$PSScriptRoot/tests/UiSmoke.java","$PSScriptRoot/tests/QuickWidgetTests.java","$PSScriptRoot/tests/AlertChecks.java","$PSScriptRoot/tests/VisualChecks.java","$PSScriptRoot/tests/CalendarChecks.java","$PSScriptRoot/tests/ControlsChecks.java","$PSScriptRoot/tests/ModernBackChecks.java","$PSScriptRoot/tests/CategoryChecks.java","$PSScriptRoot/tests/PrivacyChecks.java","$PSScriptRoot/tests/SizingChecks.java","$PSScriptRoot/tests/LightPlaneChecks.java","$PSScriptRoot/tests/V14Checks.java","$PSScriptRoot/tests/V15Checks.java","$PSScriptRoot/tests/UpdateInstallChecks.java","$PSScriptRoot/tests/PerformanceChecks.java","$PSScriptRoot/tests/V16Checks.java")
Run "$jdk/bin/jar.exe" @('cf',"$out/tests.jar",'-C',"$out/classes",'.')
Run "$jdk/bin/java.exe" @('-cp',"$bt/lib/d8.jar",'com.android.tools.r8.D8','--lib',"$sdk/platforms/android-36/android.jar",'--min-api','26','--output',"$out/dex","$out/tests.jar")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$out/base.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$out/dex/classes.dex",'classes.dex') | Out-Null}finally{$zip.Dispose()}
Run "$bt/zipalign.exe" @('-f','4',"$out/base.apk","$out/aligned.apk")
Run "$jdk/bin/java.exe" @('-jar',"$bt/lib/apksigner.jar",'sign','--ks',"$base/.signing/bolsillo-local.jks",'--ks-pass','env:BOLSILLO_STORE_PASSWORD','--key-pass','env:BOLSILLO_KEY_PASSWORD','--out',"$out/tests.apk","$out/aligned.apk")
$env:ANDROID_USER_HOME="$base/.tools/android-home"
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'install','-r',"$out/tests.apk")
if($CompileOnly){return}
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'shell','cmd','notification','allow_listener','cl.bolsillo.app.local/cl.bolsillo.app.BankNotificationListener')
$result=& "$sdk/platform-tools/adb.exe" -s $Serial shell am instrument -w cl.bolsillo.tests/cl.bolsillo.app.DeviceTests
$result
if(($result -join "`n") -notmatch 'result=PASS:'){throw 'On-device accounting tests failed'}
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'shell','pm','grant','cl.bolsillo.tests','android.permission.POST_NOTIFICATIONS')
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'shell','am','start','-W','-n','cl.bolsillo.app.local/cl.bolsillo.app.MainActivity')
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'shell','cmd','notification','disallow_listener','cl.bolsillo.app.local/cl.bolsillo.app.BankNotificationListener')
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'shell','cmd','notification','allow_listener','cl.bolsillo.app.local/cl.bolsillo.app.BankNotificationListener')
Start-Sleep -Seconds 2
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'shell','input','keyevent','KEYCODE_HOME')
Run "$sdk/platform-tools/adb.exe" @('-s',$Serial,'shell','am','start','-W','-n','cl.bolsillo.tests/cl.bolsillo.app.Notifier')
Start-Sleep -Seconds 3
$result=& "$sdk/platform-tools/adb.exe" -s $Serial shell am instrument -w -e phase verify cl.bolsillo.tests/cl.bolsillo.app.DeviceTests
$result
if(($result -join "`n") -notmatch 'result=PASS:'){throw 'Real notification delivery test failed'}
