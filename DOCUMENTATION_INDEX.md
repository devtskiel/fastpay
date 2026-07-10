# SwiftPay Integration Documentation

## Overview
This project is powered by **SwiftPay** (Netbank Infrastructure).

## Core Components

### 1. `SwiftPayService.kt`
- **Purpose**: Main service class for interacting with the SwiftPay API.
- **Base URL**: `https://api.netbank.ph/` (Production)
- **Features**:
  - Checkout creation
  - Payment status retrieval
  - Payment link generation
  - Wallet balance inquiry
  - Transaction history
  - Dynamic QR Ph generation (National Standard)
  - Disbursement (Payouts)

### 2. `SwiftPayApi.kt` & `SwiftPayApiModels.kt`
- **Purpose**: Retrofit interface and data models for SwiftPay API.
- **Endpoints**:
  - `/v1/collect/checkout`
  - `/v1/collect/qr/payments`
  - `/v1/collect/payment-links`
  - `/v1/account/balance`

### 3. `SwiftPayCredentials.kt`
- **Purpose**: Manage API keys (Public Key, Secret Key, MID).
- **Configuration**: Loaded from `local.properties` or `BuildConfig`.

## Migration Notes
- Replaced `MAYA_` keys with `SWIFTPAY_` keys in `local.properties`.
- Updated `BuildConfig` to expose SwiftPay credentials.
- Updated UI components from `MayaDialog` to `SwiftPayDialog`.
- Changed WebView Bridge from `MayaSDK` to `SwiftPaySDK`.

## Security
- **Basic Auth**: Secret Key used for server-side calls.
- **Public Key**: Used for client-side tokenization.
- **Encryption**: All sensitive card data is tokenized before payment processing.

## Contact
For technical support regarding SwiftPay integration, please contact `support@swiftpay.ph`.
