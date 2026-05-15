# SwipeCleaner

Aplicación Android para limpiar fotos de la galería mediante un sistema tipo Tinder: deslizar a la derecha para conservar, a la izquierda para enviar a una papelera interna.

## Características

- Swipe a la derecha = conservar (no volverá a aparecer durante X meses, configurable)
- Swipe a la izquierda = mover a papelera interna (no toca el original todavía)
- Papelera interna con vista en cuadrícula y borrado en lote con una única confirmación
- Modo claro / oscuro / sistema
- Filtro por álbum o carpeta
- Contador de espacio realmente liberado
- Botón de "deshacer último swipe"
- Soporte desde Android 8 (API 26) hasta Android 14 (API 34)

## Cómo compilar el APK sin instalar Android Studio (vía GitHub Actions)

### Paso 1 — Crear cuenta en GitHub

Vete a https://github.com y regístrate gratuitamente.

### Paso 2 — Crear un repositorio nuevo

Pulsa el botón "+" arriba a la derecha → "New repository". Ponle un nombre (ej. `swipe-cleaner`), márcalo como **público** (los privados tienen un cupo de minutos al mes; el público es ilimitado para Actions) y créalo.

### Paso 3 — Subir el proyecto

Descarga el `.zip` con todo el código y descomprímelo. En tu repositorio recién creado, pulsa "Add file" → "Upload files" y arrastra **el contenido** de la carpeta (no la carpeta en sí). Asegúrate de incluir la carpeta oculta `.github/` (en algunos sistemas las carpetas que empiezan por punto se ocultan; en Windows actívalo en "Ver → Elementos ocultos"; en Mac, `Cmd+Shift+.`).

Confirma el commit con "Commit changes".

### Paso 4 — Esperar a la compilación

Ve a la pestaña "Actions" del repositorio. Verás una ejecución en curso con un círculo amarillo girando. Tardará entre 5 y 10 minutos la primera vez (las siguientes mucho menos, gracias al caché).

Cuando aparezca la marca verde, abre esa ejecución y baja hasta el apartado "Artifacts". Allí estará `SwipeCleaner-debug-apk`. Descárgalo (es un `.zip`); dentro encontrarás `app-debug.apk`.

### Paso 5 — Instalar en el móvil

1. Envía el `.apk` a tu móvil (Telegram, correo, Google Drive, cable USB...).
2. Ábrelo desde el móvil.
3. Android pedirá habilitar "Instalar aplicaciones desconocidas" para la app desde la que lo abres. Concédelo solo para esta ocasión.
4. Pulsa "Instalar".

Ya tienes SwipeCleaner instalada.

## Permisos que solicita la app

- `READ_MEDIA_IMAGES` (Android 13+) o `READ_EXTERNAL_STORAGE` (Android 12 y anteriores): para leer las fotos de tu galería.

La app NO sube nada a internet. Toda la lógica corre en local.

## Estrategia de eliminación (importante)

Por las restricciones de Scoped Storage de Android, una aplicación NO puede borrar fotos del MediaStore sin que el sistema muestre un diálogo de confirmación. Para que el flujo de swipe sea fluido, la app:

1. Al hacer swipe-izquierda, **copia** la foto a una carpeta privada interna (`Android/data/.../files/trash/`). Esto NO requiere diálogo.
2. La foto aparece en la pantalla de Papelera.
3. Cuando el usuario decide "Vaciar papelera", Android muestra **un único diálogo** para confirmar el borrado de todos los originales a la vez (mediante `MediaStore.createDeleteRequest`).

Esto preserva la fluidez del swipe y ofrece una red de seguridad: nada se borra realmente hasta que tú lo confirmes.

## Estructura del proyecto

```
SwipeCleaner/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/swipecleaner/app/
│       │   ├── MainActivity.kt
│       │   ├── SwipeCleanerApplication.kt
│       │   ├── data/             (Room, MediaStore, DataStore)
│       │   ├── ui/
│       │   │   ├── components/   (SwipeableCard)
│       │   │   ├── screens/      (Swipe, Trash, Settings, Permissions)
│       │   │   ├── theme/        (colores, tipografía, theme)
│       │   │   └── viewmodel/    (ViewModels)
│       │   └── utils/
│       └── res/                  (recursos XML)
├── .github/workflows/build.yml   (CI de GitHub Actions)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Iteraciones futuras

Para modificar la app, edita los archivos directamente desde la web de GitHub (botón del lápiz arriba a la derecha en cada archivo). Cada commit dispara automáticamente una nueva compilación. Descarga el nuevo APK desde Actions e instálalo encima del anterior.

Si quieres firmar el APK para subirlo a Google Play, habría que añadir un keystore y un paso adicional al workflow. Para uso personal, el APK de debug es suficiente.
