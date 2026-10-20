# Privacidad

Bolsillo guarda cuentas, movimientos, personas, abonos y evidencias en el teléfono, cifrados con AES-GCM y una clave de Android Keystore. No requiere una cuenta de usuario en un servidor de Bolsillo.

## Permisos y datos

- **Acceso a notificaciones:** es opcional. Se filtra por las apps elegidas antes de analizar el contenido. Se guarda información del movimiento y su decisión de revisión; el diagnóstico conserva app, fecha y resultado. No se guarda un archivo con el texto completo de todos tus avisos.
- **Gmail:** solo se analizan avisos visibles de correo cuando lo seleccionas. No hay acceso OAuth al buzón ni descarga de correos históricos.
- **Notificaciones propias:** confirmaciones, revisiones y acceso a la categoría. Puedes ocultar importes y desactivar sus canales.
- **Internet:** consultas HTTPS a GitHub Releases y sus servidores oficiales de descarga. La consulta no incluye movimientos, cuentas bancarias, nombres de personas, archivos de respaldo ni claves del teléfono. Como en toda conexión web, el proveedor recibe metadatos de conexión, como la dirección IP.
- **Instalar paquetes:** se usa únicamente al pedir instalar una actualización de Bolsillo. Se comprueban SHA-256, versión, paquete y misma firma; Android solicita confirmación. No se instalan otras apps ni se automatiza la aceptación.
- **Voz:** se invoca el reconocedor de Android elegido en el teléfono; su proveedor gestiona ese reconocimiento. El resultado se revisa antes de guardarlo.

No hay anuncios, analítica integrada, SDK de seguimiento ni conexión directa con las credenciales de tus bancos. No se solicitan SMS, contactos ni números completos de tarjetas. Sus últimos cuatro dígitos se pueden guardar para identificar cuentas compartidas por tarjetas distintas.

## Respaldo y control

Puedes exportar y restaurar un respaldo desde Ajustes. El JSON exportado es un archivo portable legible: guárdalo en un lugar privado. El almacenamiento automático de Android está desactivado para el registro cifrado, cuya clave está ligada al dispositivo. Desinstalar la app puede eliminar datos y clave: exporta antes de hacerlo o cambiar de teléfono.

Las deudas y los abonos conservan su historial local. Eliminar un perfil sin vínculos no mueve dinero; un perfil con registros se archiva para mantener esos vínculos. Puedes desactivar la captura, retirar el acceso desde Android y borrar datos desde las opciones del sistema.

Si reportas un problema en GitHub, usa un ejemplo ficticio y borra nombres, cuentas, textos personales e importes que no quieras publicar. No adjuntes tu respaldo financiero ni claves.
