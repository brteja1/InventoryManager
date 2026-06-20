# Inventory Manager

Android starter project for a local-first, photo-centric valuables catalog.

## Stack

- Kotlin
- Jetpack Compose
- Room
- Coil
- CameraX
- BiometricPrompt

## What Is Scaffolded

- Modern Gradle/Kotlin build setup
- Compose app shell with a scannable grid UI
- Room entity/DAO/database skeleton
- Image loading support for local file paths
- Biometric and CameraX dependencies ready for implementation
- Android Studio import instructions in [ANDROID_STUDIO.md](/linuxdev/localgit/InventoryManager/ANDROID_STUDIO.md)

## Next Steps

1. Wire Room into a repository and ViewModel.
2. Add item creation/edit flows with CameraX and gallery selection.
3. Implement local image compression and internal storage writes.
4. Add encrypted export/import for backup.
5. Add biometric app-lock flow on launch.
