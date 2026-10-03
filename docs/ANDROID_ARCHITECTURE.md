# Android Architecture Specification

## Tech Stack
*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose
*   **Navigation**: Jetpack Navigation Compose
*   **Dependency Injection**: Hilt
*   **Concurrency**: Coroutines & StateFlow
*   **Database**: Room
*   **Camera**: CameraX & ML Kit Barcode Scanning API

## Module Responsibilities
*   ui.theme: Port of Tailwind tokens (Slate/Teal).
*   ui.screens: ScannerScreen, RosterScreen, HistoryScreen, AnalyticsScreen, VaultScreen.
*   data.local: AppDatabase, StudentDao, AttendanceDao, SettingsDao.
*   data.repository: Business logic mappings.
*   domain.usecase: ValidateQRUseCase, MergeDatabaseUseCase, CalculateAtRiskUseCase.
*   utils.export: ExcelExporter (Apache POI), PdfExporter (Android PDF Canvas / iText RTL).
