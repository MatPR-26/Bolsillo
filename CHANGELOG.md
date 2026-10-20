# Cambios

## 16.0 · Fluidez

- Carga inicial y actualización de registros fuera del hilo de interfaz; las pantallas reutilizan una instantánea hasta que cambian los datos o el día.
- Cachés de lectura con copias independientes e invalidación por la revisión cifrada. Se conserva el cifrado y la validación de cada modificación.
- Ventana, desplazamiento y cristal inferior persistentes al navegar; se reconstruyen cuando cambia la apariencia.
- Transiciones de sección y respuesta al tocar adaptadas del código público de Material Components, con cancelación y restauración de escala/posición.
- Apertura y cierre de menús unificados; superficie legible sin desenfocar toda la pantalla.
- Selector de apps en segundo plano, nombres e iconos cargados una vez por consulta y lista de filas visibles. Los selectores largos también crean solo las filas visibles.
- Los gráficos esperan a ser visibles y a que termine la entrada de la página. La curva diaria se calcula en una pasada y conserva los saldos exactos.
- Historial paginado de 25 en 25, sin eliminar ni resumir los registros originales.
- Normalización bancaria preparada una sola vez y filtros de actividad sin trabajo repetido sobre movimientos de otros meses.

Sin cambios en las cuentas, saldos iniciales, movimientos, deudas ni sus registros históricos existentes.

## 15.0

- Cristal por GPU en Android 12+ y transiciones sobre la ventana visible; se cancelan entradas superpuestas y se liberan las capas temporales.
- Los selectores se abren al tocar: deslizar por encima no despliega el menú nativo.
- Mantener pulsado en Revisión permite aprobar o descartar varios avisos y registros pendientes, conservando historial.
- Gastos personales, gráficos, presupuestos y suscripciones excluyen las partes adelantadas por otras personas; los saldos mantienen el cargo completo y sus cobros.
- Registro rápido con confirmación animada y cierre automático por defecto; una preferencia permite continuar registrando.
- Crear una persona desde un gasto abre inmediatamente su monto. Eliminación confirmada de perfiles sin vínculos, archivo de los que tienen historial y orden por fecha, monto o nombre.
- Selección inicial de bancos y catálogo de plantillas con imágenes oficiales; los no elegidos no se agregan. Las cuentas anteriores permanecen intactas.
- Tutorial opcional con foco visual, esquinas atenuadas, ondas suaves de toque y opción de reanudarlo en Automatización.
- Actualizador de GitHub Releases con estado, punto naranja, consulta manual, descarga verificada y confirmación de instalación de Android.

## 14.0

- Regalos y nuevas categorías sin duplicar equivalentes del mismo tipo.
- Tres gráficos deslizables, icono adaptable y widget renovado.
- Avisos propios de Android, acciones de revisión y prueba de notificación.
