# Avisos propios de Bolsillo

Los avisos describen cambios del **registro local**. No consultan el saldo bancario ni añaden movimientos por mostrar una alerta. La app mantiene el historial incluso cuando Android no tiene permiso para mostrar notificaciones.

## Integración

- `LocalAlerts.changed(Context, before, after)`: invocar después de guardar una modificación confirmada. Compara campos financieros; un cambio de nota o una pulsación al escribir no avisa. Agrupa varias modificaciones de una misma operación en un aviso.
- `LocalAlerts.detected(Context, evidence, tx)`: invocar después de guardar una detección. `tx == null` envía revisión; una transacción `posted` envía detección. `evidence.suppressed == true` no genera aviso ni historial de avisos.
- `LocalAlerts.resolved(Context, detectionId, tx)`: marca la revisión anterior resuelta y cancela su aviso activo. La confirmación con transacción se realiza mediante `detected`; descartar utiliza `resolved(..., null)`.
- `AlertSettings.show(MainActivity)`: preferencias y activación contextual del permiso.
- `AlertSettings.history(MainActivity)`: historial por fecha, filtro y páginas de 15 elementos.
- `AlertSettings.open(MainActivity, Intent)`: reconoce un toque de notificación y abre el movimiento concreto o su formulario de revisión. Devuelve `true` si consumió el enlace.
- `LocalAlerts.categoryAction(Context, event)`: agrega **Categoría** en los avisos propios de un gasto o ingreso confirmado en Otros o sin categoría. Abre el selector rápido mediante un `PendingIntent` explícito e inmutable; tocar el aviso no modifica movimientos y queda anotado como apertura en el historial de avisos. Android pide desbloquear cuando corresponde. Los movimientos pendientes o eliminados no reciben esta acción.
- `CategoryShortcut.open(MainActivity, Intent)`: consume el enlace antes del enlace general de avisos. Vuelve a comprobar el registro y abre su mes y selector. Los registros eliminados muestran un mensaje y no se recrean.

`LocalData` integra cambios manuales y registros recurrentes al abrir. `NativeStore` integra registros rápidos y detecciones. Las firmas de los campos financieros impiden anunciar nuevamente el eco de una operación. Una edición real seguida de su reversión sí genera un nuevo aviso.

## Preferencias y almacenamiento

Canales Android: `bolsillo_changes`, `bolsillo_detections` y `bolsillo_verifications`. Preferencias independientes: `alertChanges`, `alertDetections`, `alertVerifications`, `alertHideAmounts`.

`localAlertHistory` y `localAlertSignatures` se guardan dentro del mismo JSON AES/GCM cifrado en AndroidKeyStore que el resto del registro; no hay un archivo sin cifrar, servidor ni API de pago. El historial conserva los últimos 500 avisos. El historial completo de modificaciones financieras sigue en `changeLog`, que es independiente.

El respaldo exportado voluntariamente contiene el historial y las preferencias. No traslada permisos de Android. Preferencias de widgets se restauran solo cuando el ID sigue perteneciendo a un widget activo de Bolsillo; los IDs del launcher no son trasladables entre teléfonos.

En la pantalla bloqueada se usa una versión pública genérica del aviso. Ocultar importes elimina también los montos del texto de las notificaciones del sistema y del historial visible.

La acción rápida se añade a las notificaciones de **Bolsillo**; las notificaciones de otras apps siguen bajo control de sus emisores. Elegir una categoría modifica únicamente ese campo. El cambio en lote exige elegir expresamente los otros registros del mismo comercio. Las ediciones de categoría quedan en el historial de cambios sin emitir otro aviso de dinero.

## Verificación

- `tests/AlertEventsTest.java`: 19 comprobaciones puras de descripciones, firmas, importes grandes, movimientos pendientes y conservación del registro.
- `tests/AlertChecks.java`: pruebas instrumentadas de cifrado, canales, historial, reversión, supresión de duplicados, confirmación y respaldo. Si el emulador tiene permiso, comprueba además notificaciones reales y su texto privado. Restituye el estado y las preferencias previas al terminar.

Documentación oficial utilizada:

- [Permiso POST_NOTIFICATIONS](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Canales de Android](https://developer.android.com/develop/ui/views/notifications/channels)
- [Notificaciones y versión pública](https://developer.android.com/develop/ui/views/notifications/build-notification)
