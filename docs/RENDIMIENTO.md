# Rendimiento y movimiento

La actualización 16 adapta las fábricas `ScaleProvider` y `FadeThroughProvider` del proyecto público Material Components for Android. Conserva su licencia Apache 2.0 y atribución en `source/android/licenses/MaterialMotion-NOTICE.txt`. Los cambios de sección usan una escala corta y fundido; las rutas internas conservan dirección. Los menús usan animaciones de ventana Android, y los controles responden con una escala de pulsación pequeña y ripple.

## Trabajo retirado de la navegación

- La lectura, descifrado, migración y validación inicial se ejecutan fuera del hilo principal. La pantalla espera una instantánea completa; una navegación posterior durante esa carga conserva el último destino solicitado.
- La revisión cifrada del almacenamiento invalida las instantáneas. También se comprueba el cambio de día para ejecutar los recurrentes. Las copias JSON entregadas a los consumidores son independientes: modificarlas sin guardar no cambia la caché ni el registro.
- Los escritores conservan el bloqueo y la confirmación cifrada originales. La caché no sustituye la persistencia ni omite la validación al modificar datos.
- La ventana, el scroll y la lente GPU se reutilizan entre páginas. Los cambios de paleta o intensidad recrean la superficie cuando corresponde.
- La enumeración de aplicaciones, etiquetas e iconos ocurre en otro hilo. Los nombres y las recomendaciones se calculan antes de ordenar; las listas de apps y selectores largos crean las filas visibles.
- El historial muestra 25 entradas por página. Las entradas restantes siguen disponibles y se conservan en el respaldo.
- Los gráficos fuera de pantalla no ejecutan una entrada simultánea. El cálculo diario evita recorrer todas las transacciones una vez por día.

## Medición reproducible

La fase instrumentada `perf` usa 1.800 movimientos y 1.800 entradas de historial ficticios. Mide lecturas y tiempo de ejecución de navegación en el hilo principal, además de `FrameMetrics`. Restaura el estado cifrado y preferencias al terminar. No mide un Samsung real ni garantiza una frecuencia concreta de fotogramas.

La versión 15 tardó entre 412 y 652 ms en preparar los cambios de sección, y entre 416 y 477 ms en leer el registro de esa prueba. Consulta el informe de entrega para las cifras de la versión final 16. El tiempo de preparación de una página y la duración de un fotograma son métricas distintas.

Los controles conservan los ajustes de animación de Android. Se usan `ScrollView`, `HorizontalScrollView` y `ListView` nativos; no se fuerza una frecuencia de pantalla permanente ni se cambian ajustes de batería del teléfono.

## Fuentes

- [Código de Material Components](https://github.com/material-components/material-components-android)
- [Patrones de movimiento](https://github.com/material-components/material-components-android/blob/master/docs/theming/Motion.md)
- [Diagnóstico de renderizado lento](https://developer.android.com/topic/performance/issues/render)
- [Animaciones de propiedades](https://developer.android.com/develop/ui/views/animations/prop-animation)
- [Frecuencia adaptativa y controles nativos](https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate)
