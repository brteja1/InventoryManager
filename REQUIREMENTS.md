# Inventory Manager - Software Requirements Specification (SRS)

`InventoryManager` is a local-first Android app for tracking valuables through photos and metadata. The architecture centers heavily on a clean media pipeline and private local storage.

## 1. Functional Requirements

### Feature 1: The Visual Catalog (Core Asset Management)
- **Photo-First Capture**: Launch camera directly or select from gallery. Primary photo serves as the visual identifier. Includes runtime permission handling for camera access.
- **Multi-Image Support**: Support a primary display image plus multiple supplementary photos per asset.
- **Item Deletion**: Ability to permanently delete an item and its associated photos from the database and storage.
- **Metadata Fields**:
    - **Item Name / Description**: Required visual identifier.
    - **Estimated Value**: Currency-aware value (Default: **INR**).
    - **Purchase / Acquisition Date**: Entry via an integrated **Calendar/Date Picker** (Format: **DD-MM-YYYY**).
    - **Physical Location & Container**: Hierarchical location tracking (e.g., Room → Container).
- **Visual Tag Management**: Tags are managed as interactive colored badges. Users can add new tags via a dedicated "+" dialog that suggests existing tags or allows creating new ones. Individual tags can be removed with a single tap.

### Feature 2: Smart Data Entry & Editing
- **Context-Aware Autocomplete**:
    - **Location**: Suggestions based on existing entries.
    - **Container**: Filtered suggestions based on the selected Location.
    - **Currency & Tags**: Global suggestions from across all items.
- **Visual Change Tracking**: Modified fields in the editor are subtly highlighted (primary-tinted background) to distinguish unsaved changes from original values.
- **Improved Responsiveness**: High-performance text handling for autocomplete fields to ensure smooth typing and backspace behavior.

### Feature 3: Browsing & Visual Search
- **List-Based Dashboard**: Scannable list of items with primary photo thumbnails, separated by visible horizontal dividers for clarity.
- **Fuzzy Search**: Instant filtering by name, location, container, or tag. Disabled when the inventory is empty. Placeholder text indicates image search capability.
- **Dynamic Empty States**: Distinct views for "No items yet" (empty database) vs. "Nothing found" (no search matches).

### Feature 4: Data Security & Backup
- **Biometric Authentication**: Integration with Android BiometricPrompt for app launch protection. Quick-lock action available in the top app bar.
- **Encrypted Backup & Restore**:
    - **Export**: Package database and images into an encrypted `.zip.enc` file in the Downloads folder.
    - **Import**: Restore the full database and image library from an encrypted backup file.
    - **Navigation Drawer**: Dedicated side menu for maintenance actions (Import/Export).

### Feature 5: Semantic Image Search
- **Image-to-Image Search**: Search for items by providing a photo from the gallery or capturing a new one via a consolidated camera icon menu.
- **Match Filtering**: Results are filtered to omit items with less than a 20% semantic similarity score.
- **Automatic Indexing**: Embeddings are generated and stored automatically whenever an item is saved.
- **On-Device Privacy**: All ML processing and vector storage happens locally on the device.

## 2. Non-Functional Requirements
- **Database Integrity & Persistence**: Ensure existing inventory data is preserved across app updates. Automatically migrate data from older schemas to new ones during database upgrades, preventing data loss.
- **Local-First Architecture**: 100% offline operation. No third-party servers.
- **Privacy**: Photos stored in app-private internal storage, isolated from system gallery.
- **Performance**: Automatic image compression to **WebP** to minimize storage footprint.
- **Database Integrity**: Automatic schema migration support with destructive fallback for rapid development.

## 3. Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Persistence**: Room Database (SQLite)
- **Media**: CameraX, Coil, ImageStorageManager (WebP)
- **Security**: Android KeyStore (AES/GCM), Biometric Library
