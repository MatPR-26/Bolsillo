# Color, cristal y movimiento

La interfaz usa componentes Android nativos. Las tarjetas de bancos y servicios comparten una composición de cristal; sus colores se calculan a partir de la imagen local que identifica la marca, sin un diseño de tarjeta por institución.

## Colores derivados de imágenes

`BrandPalette` dibuja cada recurso a 48 × 48 píxeles fuera del hilo de interfaz. Agrupa los tonos en 36 intervalos y pondera población, saturación, brillo y transparencia. Descarta el relleno blanco y los píxeles casi transparentes al buscar colores de marca. El segundo color procede de otro tono suficientemente representado; si la imagen tiene un solo tono, deriva una variación suave. Las imágenes monocromas conservan un tratamiento neutro.

Cada cálculo se guarda en memoria por recurso. Las tarjetas del inicio, Cuentas y las filas de suscripciones reutilizan el resultado. La aplicación no descarga imágenes durante este proceso. El enfoque de extracción, perfiles de color y caché sigue las recomendaciones de la [guía de Palette de Android](https://developer.android.com/develop/ui/views/graphics/palette-colors); la implementación no añade una dependencia AndroidX.

## Luz y personalización

Los colores de marca son el origen del material, no el color directo del texto. Los textos conservan sus roles de primer plano y los fondos combinan el tono calculado con el tema. Separar el color de origen de los roles de superficie y contenido permite adaptar el modo claro y oscuro, como describe el [sistema de color de Material](https://m3.material.io/styles/color/the-color-system/key-colors-tones).

Prisma, Océano y Ámbar siguen disponibles. La paleta personal guarda dos semillas —acento y reflejos— y adapta su contraste. La vista previa no cambia los ajustes hasta pulsar Aplicar. Los colores personalizados se incluyen en el respaldo local y conservan su tipo numérico al restaurarlos.

Las comprobaciones instrumentadas componen las tarjetas de las 16 instituciones en ambos modos y miden el contraste del texto sobre sus píxeles reales. Esto verifica esas superficies; no constituye una certificación global de accesibilidad de toda la aplicación.

## Movimiento y descubrimiento

- Las acciones usan respuesta nativa, ondas discretas desde el punto de contacto y compresión leve.
- Las tarjetas bancarias añaden perspectiva de hasta 1,2 grados al tocar. Se restablece al soltar, cancelar o iniciar un desplazamiento.
- Los carruseles conservan anclajes, página seleccionada y ajuste al cambiar de orientación.
- Las tarjetas y filas usan superficie, reacción al toque y controles específicos para comunicar interacción. Los iconos de edición identifican campos editables; los menús usan puntos. Las flechas se reservan para direcciones, fechas y navegación entre páginas.
- Las animaciones respetan la desactivación de animadores del sistema. No hay animaciones ambientales continuas ni lectura de sensores.

La geometría y los shaders se preparan al medir cada superficie. Los movimientos de toque no recalculan la paleta ni el registro financiero.
