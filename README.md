# Betium Android Application

A complete native Android cryptocurrency wallet and light miner for the **Betium** blockchain, built using modern Android architecture, Jetpack Compose, Material 3, and Bouncy Castle cryptography.

---

## 1. Network & Chain Fundamentals

| Property | Value |
| :--- | :--- |
| **Native Token** | `BETM` |
| **Chain ID** | `947263` |
| **Internal Precision** | 8 decimals (1 BETM = 100,000,000 atomic units) |
| **MetaMask / EVM Factor** | 18 decimals (Conversion factor = 10¹⁰) |
| **Default Node** | `https://crypto.bullettsocial.space` |
| **Curve** | `secp256k1` |

### Address Derivation
- **Native Address (64 hex characters)**: `SHA-256(public_key_bytes)`.
- **Ethereum-compatible Address (0x + 40 hex)**: Last 20 bytes of `Keccak-256(uncompressed_public_key_bytes_without_04_prefix)`.

Both addresses belong to the same wallet and are derived deterministically from the same private key.

---

## 2. Core Features Implemented

1. **Wallet Creation & Backup**:
   - Cryptographically secure 32-byte private key generation on `secp256k1`.
   - 12-word BIP-39 mnemonic phrase generation and validation.
   - Interactive backup confirmation modal.
2. **Wallet Restore**:
   - Restore from 12-word recovery mnemonic phrase.
   - Direct import from raw 64-character hex private key.
3. **Send BETM & Custom Tokens**:
   - Token selector (Native BETM and any auto-detected token).
   - Recipient address input supporting both 64-char Native hex and 0x... Ethereum formats.
   - Amount input with `MAX` calculation.
   - Dynamic network fee calculation (`GET /fees`, default `0.00010000 BETM`).
   - Nonce synchronization (`GET /balance/<address>`).
   - Canonical transaction serialization (`{"amount":..., "fee":..., "nonce":..., "recipient":..., "sender":...}`).
   - RFC 6979 deterministic ECDSA signature.
   - Broadcast to `POST /transaction/new`.
4. **Receive BETM & Tokens**:
   - Dual-tab view for Native 64-hex and 0x Ethereum addresses.
   - High-contrast, dynamic on-device QR Code rendering via ZXing BitMatrix.
   - One-tap copy with clipboard feedback and Android system Share Sheet.
5. **Custom Token Auto-Detection**:
   - Fetches all tokens from `GET /tokens`.
   - Queries user balance for each token via `GET /balance/<address>/<token_id>`.
   - Filter toggle between positive balance only and all tokens.
6. **Device Light Miner**:
   - Background foreground service (`BetiumMinerService`) with persistent notification.
   - Fetches block template from `GET /mine/template?miner=<address>`.
   - Nonce iteration and SHA-256 proof-of-work hash loop checking difficulty target.
   - Submits solved blocks to `POST /block/submit`.
   - Power & thermal monitoring: automatically throttles/pauses when battery is low (<15%) and not charging.
   - Live telemetry: hashrate (`H/s`, `kH/s`), blocks found, total hashes, and scrolling terminal log.
7. **Custom Node URL**:
   - User-configurable node endpoint (supports custom domains, IP addresses, HTTP/HTTPS).
   - "Test Connection" button testing `GET /chain/status` and measuring roundtrip latency in ms.
8. **Custom Theming & Appearance**:
   - Light / Dark / System display modes.
   - Pure OLED Black (`#000000`) high-contrast surface mode.
   - Primary color presets (Betium Gold, Cyber Cyan, Emerald Miner, Electric Violet, Neon Rose, Sunset Orange).
   - Persisted across app restarts using Jetpack DataStore Preferences.
9. **Hardware-Backed Keystore Security**:
   - AES-256 GCM encrypted storage via Android `EncryptedSharedPreferences`.
   - Protected export of private key and seed phrase with confirmation dialogs.
   - Complete wallet wipe capability.

---

## 3. Architecture & Package Structure

```
com.example
├── MainActivity.kt                      # Root Activity, theme provider & bottom navigation
├── data
│   ├── api
│   │   ├── BetiumApiService.kt         # Retrofit endpoints & ApiClientProvider
│   ├── crypto
│   │   ├── Secp256k1Helper.kt          # Curve operations, key derivation & canonical signing
│   │   └── Bip39Helper.kt              # 12-word mnemonic phrase derivation
│   ├── miner
│   │   ├── BetiumMinerService.kt       # Android Foreground Service for light mining
│   │   ├── MinerManager.kt             # Mining state flow & controller
│   │   └── MinerState.kt               # State data model
│   ├── model
│   │   └── BetiumModels.kt             # Moshi JSON data models
│   ├── preferences
│   │   └── UserPreferencesRepository.kt# DataStore preferences repository
│   └── storage
│       └── SecureKeyStorage.kt         # Android Keystore encrypted vault
├── ui
│   ├── components
│   │   └── QrCodeView.kt               # Canvas QR code renderer
│   ├── screens
│   │   ├── OnboardingScreen.kt         # Create & Restore wallet
│   │   ├── DashboardScreen.kt          # Balance, quick actions, tokens & stats
│   │   ├── SendScreen.kt               # Transaction builder & broadcast modal
│   │   ├── ReceiveScreen.kt            # Dual address QR codes & copy/share
│   │   ├── MinerScreen.kt              # Live hashrate gauge, toggle & terminal
│   │   └── SettingsScreen.kt           # Custom node, themes & security export
│   └── theme
│       ├── Color.kt                    # Dynamic color palettes & presets
│       ├── Theme.kt                    # M3 adaptive color schemes
│       └── Type.kt                     # Typography configuration
└── viewmodel
    └── WalletViewModel.kt              # State holder & business logic
```

---

## 4. How to Build & Run

### Prerequisites
- Android SDK 36 (Min SDK 24)
- JDK 17 / 21
- Gradle

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
