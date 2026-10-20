# Compilar, probar y publicar

## Entorno local

Se necesitan Windows con PowerShell 7, JDK 17 y Android SDK: plataforma 36, Build Tools 36.0.0 y Platform Tools. Puedes usar las herramientas oficiales de Android Studio. Define `JAVA_HOME` y `ANDROID_HOME` apuntando a tus instalaciones. El código nativo está en `source/android`.

Para compilar y ejecutar pruebas JVM sin una clave de firma:

```powershell
./source/android/build.ps1 -Sdk $env:ANDROID_HOME -Jdk $env:JAVA_HOME -Unsigned
./source/android/test.ps1 -Sdk $env:ANDROID_HOME -Jdk $env:JAVA_HOME
```

El archivo sin firma permite validar recursos y código; no es una actualización instalable. Las pruebas descargan org.json desde Maven Central si falta y comprueban una huella SHA-256 fija. No es una dependencia de red de la app.

## APK instalable

Usa una clave local fuera del repositorio. Para un fork nuevo puedes generar tu propia clave con `keytool`; una actualización de las instalaciones oficiales exige **la misma clave original**. No generes otra si falta la original. Conserva la clave y su respaldo de forma privada.

Los scripts leen `BOLSILLO_STORE_PASSWORD` y, opcionalmente, `BOLSILLO_KEY_PASSWORD` del entorno del proceso. No escribas contraseñas en los scripts ni en archivos del repositorio. Por ejemplo, puedes pedirla sin mostrarla:

```powershell
$taskPassword = Read-Host 'Contraseña de la clave' -AsSecureString
$env:BOLSILLO_STORE_PASSWORD = [System.Net.NetworkCredential]::new('', $taskPassword).Password
./source/android/build.ps1 -Sdk $env:ANDROID_HOME -Jdk $env:JAVA_HOME -Key /ruta/privada/bolsillo.jks
Remove-Item Env:BOLSILLO_STORE_PASSWORD
```

La salida es `dist/Bolsillo-<versionCode>.apk`. El paquete `cl.bolsillo.app.local` se conserva para actualizar las instalaciones anteriores. La firma pública de la distribución oficial es SHA-256 `55328fdbbbb057047b1ce35dc1541c9bac300f4233bce90bb3121478faafe526`.

## Verificación automática

GitHub Actions compila los recursos y el código sin firma y ejecuta las pruebas JVM en cada cambio. No usa ni necesita la clave privada. La revisión de interfaz y del instalador se hace en un emulador local dedicado.

## Instrumentación

`source/android/device-test.ps1` compila e instala una app de pruebas en un **emulador dedicado**. Nunca la ejecutes sobre un teléfono con finanzas reales. Las pruebas JVM cubren parser, contabilidad, abonos, patrones, catálogo, gastos propios y metadatos de actualización. Las fases del dispositivo cubren interfaz, gestos, revisión en lote, widgets, privacidad, GPU y tutorial. Las pruebas visuales y de regresión restauran sus datos; otras fases preparan un registro ficticio.

La fase de instalación de actualizaciones requiere APK de prueba de versión superior, firma correcta y firma ajena para comprobar rechazos. Abre la confirmación real de Android y la cancela; esos APK sintéticos no se publican. El permiso de instalación del emulador debe configurarse antes de iniciar la instrumentación, ya que cambiarlo durante una prueba puede terminar el proceso.

## Publicar la próxima versión

1. Modifica el código y aumenta `android:versionCode` en `source/android/AndroidManifest.xml` (por ejemplo 16), además del nombre de versión. El código debe aumentar en cada APK publicado.
2. Compila con la clave original, ejecuta pruebas y revisa interfaz y conservación de registros.
3. Sube el código revisado, sin claves, archivos de entorno, respaldos, datos del emulador ni adjuntos personales.
4. Publica una release estable, por ejemplo `v16.0`, cuyo asset se llame **exactamente** `Bolsillo-16.apk`. Usa el script `tools/publish-release.ps1` o GitHub CLI. Los borradores y versiones preliminares no aparecen en la app.

El script prepara la release en borrador. Usa `-Publish` solo cuando quieras publicarla. Necesitas GitHub CLI autenticado en el almacén del sistema, sin un token en el código. El script comprueba el APK, su versión y la firma, y publica también `SHA256SUMS.txt`. La app utiliza el digest SHA-256 calculado por GitHub, y vuelve a validar paquete/versión/firma antes de instalar.

```powershell
./tools/publish-release.ps1 -Sdk $env:ANDROID_HOME -Jdk $env:JAVA_HOME -NotesFile docs/notas-version.md -Publish
```

No se sube la clave de firma a GitHub ni a GitHub Actions. Cambiar código sin publicar su APK en Releases no envía una actualización a los teléfonos. Un fork debe cambiar `res/values/updates.xml` y tendrá su propia firma/distribución.
