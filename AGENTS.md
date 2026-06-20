# AGENTS.md

## Project Context

`InventoryManager` is a local-first Android app for tracking valuables through photos and metadata.

The current scaffold is Kotlin + Jetpack Compose + Room, with placeholders and wiring for:

- CameraX capture
- Gallery import
- Internal image storage and WebP compression
- Room-backed inventory metadata
- Optional biometric app lock
- Encrypted export/import backup

## Product Requirements

The app should support:

- Photo-first inventory management
- One primary photo plus supplemental images per item
- Metadata fields for:
  - name / description
  - estimated value and currency
  - acquisition date
  - room / container location
  - category tags
- Fast search by name, location, or tag
- Location-based filtering
- Offline operation with no third-party server dependency
- Local storage of images in app-private internal storage
- Encrypted backup export

## Current Architecture

- `app/src/main/java/com/inventorymanager/app/MainActivity.kt`
  - Entry activity
  - Creates the ViewModel and hands it to Compose

- `app/src/main/java/com/inventorymanager/app/InventoryAppContainer.kt`
  - Simple manual dependency container
  - Builds Room database and image storage helper

- `app/src/main/java/com/inventorymanager/app/data/local/`
  - Room database, entities, DAOs, and repository
  - Inventory metadata is the source of truth

- `app/src/main/java/com/inventorymanager/app/data/media/ImageStorageManager.kt`
  - Saves and deletes images under `context.filesDir/inventory_images`
  - Compresses images to WebP

- `app/src/main/java/com/inventorymanager/app/data/security/`
  - Handles biometric unlock gating
  - Packages and encrypts backup exports into Downloads

- `app/src/main/java/com/inventorymanager/app/ui/`
  - Compose UI for inventory list and editor
  - ViewModel owns query state, editor state, and save flow

## Important Files

- Requirements: [REQUIREMENTS.md](/linuxdev/localgit/InventoryManager/REQUIREMENTS.md)
- Android Studio import guide: [ANDROID_STUDIO.md](/linuxdev/localgit/InventoryManager/ANDROID_STUDIO.md)
- App build file: [app/build.gradle.kts](/linuxdev/localgit/InventoryManager/app/build.gradle.kts)
- Manifest: [app/src/main/AndroidManifest.xml](/linuxdev/localgit/InventoryManager/app/src/main/AndroidManifest.xml)

## Working Conventions

- Prefer local-first implementations.
- Store images inside internal storage, not shared gallery paths.
- Keep Room as the metadata source of truth.
- Use Compose for UI changes.
- Use `apply_patch` for file edits.
- Avoid deleting user-created work unless explicitly requested.
- Do not add network services or analytics; the app is intended to stay offline by default.
- Keep backup exports encrypted and local.
- Keep the app locked on launch unless the biometric gate succeeds.

## Build Notes

- The repo includes a Gradle wrapper.
- Android Studio should open the repository root directly.
- The local machine's system Gradle is too old for the current Android stack, so wrapper-based sync/build is the intended path.
