# JustSpent - Financial Sanctuary

JustSpent is an intelligent, completely automated personal finance tracker for Android. Designed with the premium "Aura Ledger" design system (Precision Etherealism), it acts as a passive observer that intercepts payment application closures (e.g., Google Pay, PhonePe) and seamlessly prompts users to record their expenses through an elegant, unobtrusive overlay.

## Key Features
*   **Zero-Input Automation (Observer & Interceptor):** Automatically detects when you close a UPI payment app and acts as a high-fidelity interceptor to quickly capture the transaction details.
*   **The "Financial Sanctuary" Dashboard:** A rich, typography-driven main screen with a fluid progressive breakdown, tonal layering (no divider lines), and a deep navy gradient hero card.
*   **Offline First:** Built entirely on local architecture (Room DB) preventing data from leaving your device unless explicitly exported. 

## Technology Stack
*   **UI:** Jetpack Compose (Material Design 3 with custom Aura Ledger properties)
*   **Architecture:** MVVM Local-First
*   **Services:** Foreground Services & `UsageStatsManager` for passive observation
*   **Data:** Room Persistence Library

## Setup Instructions
1.  Clone the repository.
2.  Ensure you have Android SDK 34 configured.
3.  Grant "Usage Access" permission upon first launch for the automated observer correctly perform foreground app detection.
