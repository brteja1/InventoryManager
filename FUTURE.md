# FUTURE.md - Roadmap and Feature Ideas

## Over-the-Air (OTA) Data Exchange
Implement direct device-to-device transfer of inventory records without relying on cloud services, maintaining the local-first and privacy-focused nature of the app.

### 1. Android Quick Share Integration (Priority)
- **Concept**: Leverage the native Android Sharesheet to send the encrypted `.zip.enc` backup files.
- **Implementation**: Instead of only saving to the Downloads folder, provide a "Share" action that invokes the system Quick Share (formerly Nearby Share).
- **Security**: Utilizes the existing Version 3 (AES/CBC) password-protected encryption.

### 2. QR Code Metadata Sharing
- **Concept**: Share individual item metadata via generated QR codes.
- **Use Case**: Quick transfer of text-based details (UID, name, location) for a single item.
- **Implementation**: Generate high-density QR codes for scanning by another device's camera.

### 3. Local Network (Wi-Fi) Peer-to-Peer
- **Concept**: A "Sync Mode" where devices on the same Wi-Fi network can discover each other and transfer data.
- **Implementation**: Use Network Service Discovery (NSD) for peer finding and a temporary local HTTP/Socket server for high-speed transfer of large media libraries.

### 4. Bluetooth LE Handshake
- **Concept**: Use BLE for initial discovery and pairing, upgrading to a faster transport layer for the actual file payload.
- **Advantage**: Works in environments without an existing Wi-Fi access point.
