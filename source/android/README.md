# Bolsillo · Android nativo

Código Java y recursos Android de la app pública. [Uso e instalación](../../README.md) · [Privacidad](../../PRIVACY.md) · [Compilar y publicar versiones](../../docs/MAINTAINER.md).

- SDK mínimo 26, compilación/target 36 y JDK 17. No depende de un sitio web, WebView ni Gradle.
- Paquete `cl.bolsillo.app.local`, conservado para poder actualizar las instalaciones anteriores.
- `Ledger`, `Finance` y `LocalData`: CLP entero, saldos, tu parte de los gastos, cobros, cuotas y operaciones atómicas.
- `NativeStore`: almacenamiento AES-GCM con clave de Android Keystore. Los datos no forman parte del APK ni del repositorio.
- `BankCatalog` y `BankTemplates`: selección opcional de instituciones al empezar, sin alterar cuentas existentes. `PaymentCards` separa tarjetas del saldo que comparten.
- `BankNotificationListener`, `NotificationPatterns`, `NotificationRouting` y `Detection`: solo apps elegidas, enrutamiento conservador, revisión y deduplicación.
- `ReviewSelection`: mantener pulsado, selección de avisos y movimientos pendientes, aprobación o descarte en lote.
- `PeopleScreen`, `PersonRecords`, `SharesEditor`: perfiles, abonos fechados, archivo conservador y partes compartidas. El resumen resta las partes de otros sin modificar el cargo bancario original.
- `FrostedDock`: RenderNode/RenderEffect en Android 12+; lente pequeña por software en versiones anteriores. El desenfoque no se aplica a los iconos.
- `PageMotion`: una animación sobre la ventana visible, con liberación de la capa temporal y cancelación de entradas superpuestas.
- `DetectionTutorial` y `WaterRipple`: guía opcional que se puede retomar, foco visual y ondas de toque discretas. Nunca concede permisos ni registra movimientos por sí misma.
- `AppUpdates`, `ReleaseFeed` y `UpdateNetwork`: consulta pública de GitHub Releases, HTTPS limitado, SHA-256, paquete/versión/firma y confirmación de Android mediante PackageInstaller.
- `BolsilloWidget` y `QuickEntryDialog`: saldos y registro con cierre automático por defecto, ajustables por instancia/preferencia.

Las pruebas usan ejemplos ficticios. La instrumentación solo debe ejecutarse en un emulador dedicado: algunas fases preparan su propio registro. Las fases visuales y de regresión restauran los datos que encuentran. Nunca se deben ejecutar sobre el teléfono de uso diario.

Las imágenes institucionales son archivos originales con procedencia en `licenses/BankImages-sources.json`; no implican afiliación. Las 60 imágenes de suscripciones y sus licencias están documentadas en `licenses/ServiceImages-sources.json`. Material Symbols Rounded conserva las curvas oficiales de Google y su licencia Apache 2.0. Inter y los recursos conservados de Manrope incluyen su licencia SIL OFL. Las atribuciones también se empaquetan en `assets/licenses/` del APK. No se descargan imágenes ni fuentes durante el uso de la app.
