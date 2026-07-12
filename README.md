# SwiftPay Enterprise - PH Banking Gateway

SwiftPay is a comprehensive, production-ready merchant platform and PH banking gateway (v1.18.0). It provides a full suite of tools for Philippine merchants to accept payments, manage virtual accounts, and execute disbursements via BSP-regulated infrastructure.

## 🚀 Key Features (18 Expected Fixed Features)

1.  **Secure Authentication**: JWT-based login with multi-device session management and idle timeout protection.
2.  **Merchant Hub**: A centralized command center for all business operations, webhooks, and team tools.
3.  **Real-Time Wallet**: Instant balance inquiries and comprehensive transaction ledgers with filtering.
4.  **Virtual Collection Accounts (VCA)**: Generate unique Netbank-backed account numbers for seamless collections.
5.  **Smart Payouts**: Direct disbursements to any Philippine bank or E-Wallet (GCash, Maya, etc.) via InstaPay.
6.  **QR Ph Standard**: Fully compliant dynamic QR Ph generation for national standard collections.
7.  **Payment Links**: One-click link generation for instant checkout via SMS, Email, or Chat.
8.  **Admin Control Panel**: Advanced dashboard for managing merchant registrations and treasury approvals.
9.  **Team Management**: Role-based access control (RBAC) for Admins, Members, and Developers.
10. **Merchant Profile**: Complete business verification, branding (logo management), and settings.
11. **Webhook Engine**: Configurable real-time notifications for automated payment event handling.
12. **Compliance Audit Logs**: Regulatory-grade logging for all sensitive financial operations.
13. **Digital Invoicing**: Professional invoice creation with real-time status tracking (Paid/Pending).
14. **Tap to Pay (NFC)**: Support for contactless card payments using mobile NFC technology.
15. **Developer API Docs**: Built-in documentation and API key management for technical integrations.
16. **Adaptive Theme Engine**: Enterprise-grade Dark Mode and Light Mode support across all 18+ screens.
17. **Security Shield**: Active protection against screenshots/recording and HMAC-SHA256 signature verification.
18. **Authorization Management**: Approve or deny login requests from new devices via primary device push.

## 🛠️ Architecture

-   **Backend**: Node.js Gateway with PostgreSQL persistence, hosted on Render.
-   **Mobile**: Native Android (Kotlin) using Jetpack Compose and Material Design 3.
-   **Security**: HMAC signatures, JWT session tokens, and end-to-end encryption for payment data.

## 📦 Deployment

### Backend (Render)
The gateway is live at: `https://swiftpay-merchant-gateway.onrender.com`

### Android App
To generate the transactional release APK:
```bash
./gradlew assembleRelease
```
The artifact is located at: `app/build/outputs/apk/release/app-release.apk`

---
© 2026 DRL Tech Group. SwiftPay operates in compliance with BSP regulations.
