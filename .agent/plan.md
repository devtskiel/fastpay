# Project Plan

Adding Maya Payment Link (Invoice API) feature to the Maya Mini App Showcase. This allows the mini-app to generate shareable payment URLs.

## Project Brief

# Project Brief: Maya Mini App Showcase (Production Transition)

This project transitions the Maya Mini App Showcase from a simulated environment to a production-ready Android application. It replaces mock services with live Maya API integrations for Checkout, Payments, and Profile Sharing, while maintaining a high-energy Material 3 aesthetic and a fully adaptive architecture.

### **Features**
1.  **Live Maya API Integration**: Direct implementation of Maya’s RESTful endpoints for Checkout and Payments, enabling real-world transaction processing and secure payment page redirects.
2.  **Real-Time Wallet & Profile Sync**: Functional balance inquiries and customer-consented profile sharing (OpenID/KYC) using authenticated token exchange with Maya's backend.
3.  **Adaptive Material 3 Interface**: A vibrant, edge-to-edge UI that utilizes **Compose Material Adaptive** to provide optimized layouts for phones, foldables, and tablets.
4.  **Production-Hardened JS Bridge**: A secure communication layer between the hosted HTML5 mini-app and the native environment, designed for live API data handling.
5.  **Security Integration Layer**: A dedicated security implementation that provides a "Testing Only" framework for client-side secret management, with clear architectural paths for future backend-mediated secret storage.
6.  **Maya Payment Link Integration**: Integration of Maya's Invoice API to generate unique, shareable payment URLs (Invoice URLs) directly from the mini-app.

### **High-Level Technical Stack**
*   **Language**: Kotlin
*   **Networking**: **Retrofit** & **OkHttp** for secure, type-safe REST communication.
*   **UI Framework**: **Jetpack Compose** with **Material Design 3** (Energetic/Vibrant theme).
*   **Navigation**: **Jetpack Navigation 3** (State-driven navigation using `NavKey` serialization).
*   **Adaptivity**: **Compose Material Adaptive** library (utilizing `NavigationSuiteScaffold` and `ListDetailPaneScaffold`).
*   **Asynchronous Logic**: **Kotlin Coroutines** for non-blocking network operations.
*   **Data Handling**: **Kotlinx Serialization** for API request/response parsing and navigation route management.

## Implementation Steps

### Task_1_Foundation: Implement Material 3 theme with a vibrant energetic color scheme, enable full Edge-to-Edge display, and set up the Navigation 3 framework to host the application shell.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Material 3 theme with light/dark color schemes applied
  - Edge-to-Edge enabled in MainActivity
  - Navigation 3 framework successfully initialized

### Task_2_WebView_Bridge: Create a WebView container that hosts a local HTML5 mini-app. Implement a secure JavaScript bridge (MayaSDK) using @JavascriptInterface to allow communication between the web environment and Android.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - WebView loads local index.html from assets
  - JavaScript bridge successfully established
  - Kotlinx Serialization used for messaging between JS and Kotlin

### Task_3_Mock_Services_UI: Implement mock services for Maya profile sharing (OpenID) and payment processing. Build Material 3 UI components (BottomSheets/Dialogs) for payment consent and transaction feedback.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Mock profile data returns successfully to the mini-app
  - Payment flow (consent to completion) simulation works
  - Material 3 BottomSheets/Dialogs are used for interactions

### Task_4_Adaptive_Polish_Verify: Apply Compose Material Adaptive for responsive layouts across different device types. Create an adaptive app icon and perform final verification of the app's stability and functionality.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - App layout is responsive on phone and tablet/foldable configurations
  - Adaptive app icon implemented
  - Build passes, all existing tests pass, and app does not crash

### Task_5_Secret_Key_Integration: Configure the project to securely load the Maya Secret Key from local.properties and expose it via BuildConfig. Update the MayaService to use this key for authenticating payment requests.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Maya Secret Key is loaded from local.properties via BuildConfig
  - MayaService uses the key in payment processing logic
  - App correctly warns or handles missing key

### Task_6_Wallet_Transfer_Bridge: Implement a native Wallet dashboard showing balances and transaction history. Expand the Maya SDK bridge to support balance inquiries and P2P transfer execution from the WebView.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Native Wallet dashboard displays mock balance and transactions
  - P2P transfer flow implemented natively
  - JS Bridge supports getBalance and executeTransfer calls

### Task_7_Advanced_Adaptive_Verify: Refine UI using NavigationSuiteScaffold for adaptive navigation and ListDetailPaneScaffold for transaction history. Perform a final Run and Verify step to ensure stability and requirement alignment.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Navigation adapts between Bar and Rail based on window size
  - ListDetailPaneScaffold implemented for history detail view
  - Build passes, app does not crash, and all features meet the brief requirements
  - Instruct critic_agent to verify stability and report UI issues

### Task_8_Maya_API_Integration: Implement a secure networking layer using Retrofit and OkHttp to integrate real Maya API endpoints for Checkout, Payments, and Profile sharing. Update MayaService to handle authenticated REST calls instead of mock responses.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Retrofit interfaces for Maya Checkout and Profile APIs implemented
  - MayaService successfully processes live network requests/responses
  - API errors are handled gracefully with Material 3 UI feedback
  - Network security config and OkHttp logging interceptor integrated

### Task_9_Harden_Verify: Harden the JavaScript bridge for production data handling and synchronize real-time wallet/profile data with the native UI. Perform a final Run and Verify to ensure stability and requirement alignment.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - JS bridge updated to pass live API data to the mini-app
  - Wallet balance and profile sync use live data from Maya APIs
  - Build passes, app does not crash, and all features meet the production brief requirements
  - Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.

### Task_10_Payment_Link_Integration: Integrate Maya's Invoice API to allow the mini-app to generate shareable payment URLs. Update the Retrofit interface, MayaService, and JS bridge to support this feature, and provide a native share UI.
- **Status:** COMPLETED
- **Updates:** Implemented Maya Invoice API integration for generating shareable payment links. Updated MayaApi, MayaService, and MayaSDKBridge. Created a Material 3 Payment Link Dialog with a native sharing feature. Updated the mini-app index.html to include a 'Generate Payment Link' test button. verified end-to-end functionality from bridge call to native sharesheet.
- **Acceptance Criteria:**
  - Retrofit interface includes Invoice API endpoints
  - MayaSDK bridge supports createPaymentLink call
  - Generated payment URLs are displayed in a Material 3 UI and shareable
- **Duration:** N/A

### Task_11_Final_Run_Verify: Perform a final Run and Verify step to ensure the new Payment Link feature and existing Checkout/Profile flows work seamlessly together. Instruct critic_agent to verify application stability and report UI issues.
- **Status:** COMPLETED
- **Updates:** Fixed `Unresolved reference: FastPayBlack` compile error in `ApiKeysScreen.kt` — the `dark_mode.py` script had injected dark-mode theme references without updating the narrow named imports. Replaced with wildcard `import com.example.myapplication.ui.theme.*` (consistent with all other screens). Build now passes: `BUILD SUCCESSFUL in 15s`.
- **Acceptance Criteria:**
  - End-to-end payment link generation works ✅
  - App remains stable with no crashes during all payment flows ✅
  - Build passes and all existing functionality remains intact ✅
- **CompletedTime:** 2026-06-16 21:17 PST

