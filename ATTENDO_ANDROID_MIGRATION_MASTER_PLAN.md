# ATTENDO ANDROID MIGRATION MASTER PLAN

## 1. Executive Summary
This document serves as the authoritative blueprint for migrating the **Attendo** desktop application (React/Tauri) to a native Android application using Kotlin and Jetpack Compose. Based on a comprehensive forensic audit of the existing codebase, this plan details every feature, screen, data model, and user workflow. It translates desktop-specific interactions into native Android paradigms while strictly preserving the business logic, visual identity, and offline-first capabilities of the original software.

## 2. Attendo Desktop Architecture Overview
The current Attendo application is a standalone desktop application built with:
*   **Frontend**: React (TypeScript), Vite, Tailwind CSS, Recharts, Lucide Icons.
*   **Backend Wrapper**: Tauri (Rust).
*   **Storage**: Local SQLite database via `tauri-plugin-sql`.
*   **Key Native Integrations**: 
    *   File system access (CSV imports, Excel/PDF/ZIP exports).
    *   Hardware ID generation for licensing.
    *   Local network Axum HTTP Server (used to turn mobile phones into companion scanners for the desktop).
    *   Audio API for scanning feedback beeps.

## 3. Complete Feature Inventory

| ID | Feature | Description | Current Location | Dependencies | Data Used | User Actions | Priority | Android Considerations |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| ATT-001 | License Validation | Verifies JWT license bound to Hardware ID | `App.tsx` (Boot), Vault | Hardware ID logic | `license_token`, `last_opened_timestamp` | Input token | Critical | Android limits hardware ID; use `Settings.Secure.ANDROID_ID` or Widevine. |
| ATT-002 | Setup Wizard | First-time configuration of university, grades (workspaces) | `App.tsx` | DB Settings | `setup_complete`, `setup_grades` | Fill forms, Save | Critical | Direct Port |
| ATT-003 | Workspace Selector | Select active "Grade" (workspace) on launch | `App.tsx` | Setup Wizard | `activeWorkspace` | Click workspace | High | Direct Port |
| ATT-004 | Start/Stop Session | Initializes a new attendance recording session | Scanner Tab | Active Workspace | Session Name, Timestamp | Click Start/Stop | Critical | Direct Port |
| ATT-005 | Camera Scanning | Reads QR codes via webcam | Scanner Tab | Camera Hardware | QR Payload (National ID) | Align QR | Critical | Migrate to CameraX + ML Kit |
| ATT-006 | Mobile Companion Scanner | Local HTTP server allowing phone to act as scanner | Scanner Tab | Tauri local IP, Axum Server | QR Payload | Connect via URL | Low | **NOT APPLICABLE**. The Android app *is* the mobile scanner. |
| ATT-007 | Manual ID Entry | Fallback input for 14-digit national IDs | Scanner Tab | Keyboard | 14-digit ID | Type & Submit | High | Direct Port |
| ATT-008 | Rapid Paper-to-Digital | Modal for fast, keyboard-driven batch entry | Scanner Tab | `handleManualSubmit` | Text input | Paste text/IDs | Medium | Adapt to mobile paste / multiline text field. |
| ATT-009 | Cold Call Randomizer | Randomly selects a scanned student for questions | Scanner Tab | Active Session List | `scannedStudents` | Click "Cold Call" | Low | Direct Port |
| ATT-010 | Session Auto-Export | Silently backups session to PDF/Excel on end | Scanner Tab | JSZip, ExcelJS, JSPDF | Session Data | End Session | High | Requires Android Storage Access Framework (SAF). |
| ATT-011 | Roster Import (CSV) | Batch import students via CSV | Roster Tab | File System | CSV file | Upload File | High | Use Android Document Picker Intent. |
| ATT-012 | Add Single Student | Manually add a student to roster | Roster Tab | DB | Name, ID | Fill form | Medium | Direct Port |
| ATT-013 | Edit Student Name | Rename existing student | Roster Tab | DB | Student ID, New Name | Click edit | Medium | Direct Port |
| ATT-014 | Archive Student | Soft delete student | Roster Tab | DB | `is_deleted` flag | Context Menu / Batch | Medium | Migrate right-click to Long-Press (Contextual Action Bar). |
| ATT-015 | Restore Student | Recover archived student | Roster Tab | DB | `is_deleted` flag | Context Menu / Batch | Medium | Direct Port |
| ATT-016 | Permanent Delete | Hard delete student + all attendance | Roster Tab | DB | Student ID | Confirm Dialog | High | Direct Port |
| ATT-017 | Generate QR Bundle | Exports ZIP of generated QR codes for roster | Roster Tab | Canvas, JSZip | `student.qrPayload` | Click Generate | High | Generate bitmaps via Android Canvas, compress to ZIP via `java.util.zip`. |
| ATT-018 | View Past Sessions | List all recorded sessions | History Tab | DB | `attendance` table | View List | High | Direct Port |
| ATT-019 | Session Audit Ledger | View/Edit attendees of a specific past session | History Tab | DB | Session ID | Click Session | High | Direct Port |
| ATT-020 | Toggle Attendance | Retroactively mark present/absent | History Tab | DB | `national_id`, `session` | Toggle Switch | High | Direct Port |
| ATT-021 | Mark Excused | Mark absence as excused with reason | History Tab | DB | `is_excused`, `excuse_reason` | Click Excuse | Medium | Direct Port |
| ATT-022 | Add Bonus Points | Add +N bonus points retroactively | History Tab | DB | `bonus_points` | Click (+ / -) | Medium | Direct Port |
| ATT-023 | Re-Export Session | Generate PDF/Excel for past session | History Tab | File System | Session attendees | Click Export | Medium | Requires SAF. |
| ATT-024 | Analytics Matrix | Master table of attendance across weeks | Analytics Tab | DB | All attendance data | View Table | High | Requires horizontal scrolling UI on mobile. |
| ATT-025 | "At Risk" Calculation | Flags students exceeding absence threshold | Analytics Tab | DB | `absence_threshold` | Auto-calculates | High | Direct Port |
| ATT-026 | Export Master Report | Exports Analytics Matrix to Excel/PDF | Analytics Tab | File System | Matrix data | Click Export | High | Requires SAF. |
| ATT-027 | Global Search | Context-aware search across current tab | Top Bar / Global | `searchQuery` state | Input text | Type | High | Direct Port |
| ATT-028 | Command Palette | Quick action menu (Ctrl+K) | Global | Keyboard | System actions | Press shortcut | Low | Map to a FAB or search bar focus. |
| ATT-029 | Database Backup | Exports raw SQLite DB | Vault Tab | File System | `attendo_core.db` | Click Backup | High | Export via SAF. |
| ATT-030 | Import & Merge | Merge offline backup resolving conflicts | Vault Tab | File System, DB | `.attdb` file | Click Import | Critical | Direct Port |
| ATT-031 | Factory Reset | Wipes all database tables | Vault Tab | DB | All tables | Confirm Dialog | High | Direct Port |
| ATT-032 | Hardware Feedback | Beeps and Haptics on successful/failed scans | Scanner Tab | AudioContext | N/A | Scan QR | High | Use Android `ToneGenerator` and `Vibrator`. |

## 4. Complete Screen / UX Inventory

| Screen ID | Screen Name | Purpose | Main Components | Navigation | Desktop-Specific Behavior | Android Equivalent |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| SCR-001 | License Boot | License validation | Input field, Activate Button | Transitions to Welcome/Setup | N/A | Direct Port |
| SCR-002 | Setup Wizard | Initial config | Form inputs, Select dropdowns | Next/Save | N/A | Direct Port |
| SCR-003 | Workspace Select | Pick Grade | Grid of Grade cards | Goes to Dashboard | N/A | Direct Port |
| SCR-004 | Global Dashboard | Shell for tabs | Sidebar, Top Titlebar, Tab Content | Sidebar clicks | Sidebar, Custom Titlebar, Ctrl+Up/Down | Bottom Navigation Bar. TopAppBar. |
| SCR-005 | Scanner View | Active scanning | Video feed, Stats, Manual Input | N/A | Webcam feed, Hardware beeps, Ctrl+Space | CameraX Preview, UI overlays. |
| SCR-006 | Roster View | Student list | Data Table, Search, Action Buttons | N/A | Right-click context menus, Multi-select via Shift | RecyclerView, Long-press for Contextual Action Mode. |
| SCR-007 | History View | Session list | List of cards, Filter dropdowns | Opens Audit Ledger | Delete key for batch delete | RecyclerView, Swipe-to-delete or Long-press. |
| SCR-008 | Audit Ledger | Edit past session | Modal/Overlay, Attendee list | Close (Escape) | N/A | Full-screen Dialog or Navigation route. |
| SCR-009 | Analytics View | Matrix report | Wide Data Table, Export Buttons | N/A | Horizontal scroll, mouse hover tooltips | Bidirectional scrolling table (LazyRow inside LazyColumn). |
| SCR-010 | Vault | Settings & DB | Configuration panels, Backup buttons | N/A | File explorer popups | Android System Picker Intents. |

## 5. Business Logic & Data Model

### Entities & Relationships (SQLite / Room Target)
1.  **Student (`students`)**:
    *   `id` (PK, AutoIncrement)
    *   `name` (String, Not Null)
    *   `national_id` (String, Unique)
    *   `grade` (String) - Maps to Workspace
    *   `status` (String)
    *   `is_deleted` (Integer, 0/1) - Soft delete
2.  **Attendance (`attendance`)**:
    *   `id` (PK, AutoIncrement)
    *   `national_id` (String) - FK to Student
    *   `session_name` (String) - Grouping key
    *   `timestamp` (DateTime)
    *   `is_excused` (Integer, 0/1)
    *   `bonus_points` (Integer)
    *   `is_archived` (Integer, 0/1)
    *   `excuse_reason` (String, Nullable)
    *   `audit_trail` (String, JSON array)
3.  **Settings (`settings`)**:
    *   `key` (String, PK)
    *   `value` (String)

### Critical Business Rules
*   **Tamper Protection**: System clock regression (comparing current time against `last_opened_timestamp` in Settings) triggers a lockdown.
*   **QR Hashing**: QR payloads are not raw IDs. They are `ID|Hash`. The Hash is a SHA-256 of `[NationalID] + [SecretKey] + [AcademicYear]`. Scanning must validate this hash.
*   **Merge Conflict Logic**: When importing an external database, if a student has conflicting attendance records (e.g., marked present locally but absent externally), the user is presented with a Conflict Resolution UI to pick 'Keep Local' or 'Overwrite'.
*   **"At Risk" Calculation**: `Total Sessions - (Attended + Excused) >= absence_threshold` (default 3).

## 6. Design System Specification
The Android version must implement the following tokens using Jetpack Compose `MaterialTheme`:

*   **Colors**:
    *   Background Main: `#0f172a` (Slate 900)
    *   Background Secondary: `#0a0f1c` (Darker Slate)
    *   Primary Accent: `#2dd4bf` (Teal 400)
    *   Secondary Accent: `#818cf8` (Indigo 400)
    *   Danger: `#f43f5e` (Rose 500)
    *   Warning: `#fbbf24` (Amber 400)
    *   Text Primary: `#ffffff`
    *   Text Secondary: `#94a3b8` (Slate 400)
*   **Typography**: Clean, sans-serif (Inter or Roboto equivalent). Monospace font required for National IDs.
*   **Shapes**: Heavy use of rounded corners (`RoundedCornerShape(16.dp)` or `12.dp`).
*   **Icons**: Match desktop Lucide icons with equivalent Material Icons (Rounded variant).

## 7. Desktop → Android Compatibility Analysis

| Desktop Interaction | Why It Must Change | Android Adaptation | Functionality Preserved? |
| :--- | :--- | :--- | :--- |
| Keyboard Shortcuts (Ctrl+S, Ctrl+E, etc.) | Mobile devices lack physical keyboards. | Migrate to Floating Action Buttons (FAB), TopAppBar icons, or BottomSheet menus. | Yes, via different UI/UX. |
| Right-Click Context Menus | No mouse pointer. | Implement Long-Press to trigger a Contextual Action Bar or a BottomSheet with options. | Yes. |
| Multi-Select via Shift+Click | No keyboard modifiers. | Long-press to enter selection mode, then tap items to toggle selection. | Yes. |
| Local HTTP Server for Mobile Scanning | The Android device *is* the mobile scanner. | Deprecate the network server feature. Use CameraX directly on the device. | Yes, superior native experience. |
| File System Access (fs module) | Android uses sandboxed storage. | Use `ActivityResultContracts.CreateDocument` and `OpenDocument` (SAF) for saving/loading PDFs, Excels, and DB backups. | Yes. |
| Wide Analytics Table | Screen width is limited. | Implement a 2D scrolling layout (fixed left column for Student Name, horizontally scrolling columns for sessions). | Yes. |

## 8. Recommended Android Architecture
*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose
*   **Architecture Pattern**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF).
*   **Database**: Room Persistence Library. Provides exact structural parity with the desktop's SQLite tables.
*   **Dependency Injection**: Hilt.
*   **Camera**: CameraX + ML Kit Barcode Scanning API (Fast, on-device, replaces HTML5Qrcode).
*   **Coroutines & Flow**: For asynchronous DB operations and reactive UI updates.
*   **Exporting (Excel/PDF)**: `Apache POI` for Android (Excel) and Android's native `PrintedPdfDocument` or `iText` for PDF generation.
*   **Storage**: Storage Access Framework (SAF) exclusively to avoid `MANAGE_EXTERNAL_STORAGE` permission issues.

## 9. Implementation Phases

**Phase 0 — Foundation & Architecture**
*   **Purpose**: Setup Android project, Hilt, Room DB schema (matching exact tables), and Navigation graphs.

**Phase 1 — Licensing & Onboarding**
*   **Purpose**: Implement hardware ID generation, JWT validation, system clock tamper check, and Setup Wizard.

**Phase 2 — Core UI Shell & Workspaces**
*   **Purpose**: Build the Bottom Navigation (Scanner, Roster, History, Analytics, Vault), TopAppBar, Workspace selector, and Global Search state.

**Phase 3 — The Scanner Engine**
*   **Purpose**: Integrate CameraX, implement QR SHA-256 hash validation, Session Start/Stop logic, and hardware feedback (ToneGenerator/Vibrator).

**Phase 4 — Roster Management**
*   **Purpose**: Student CRUD, CSV Import (parsing logic), QR Zip Generation, and Long-Press selection mode.

**Phase 5 — History & Audit Ledger**
*   **Purpose**: History list, Audit Ledger dialog, retroactive attendance toggling, excuse/bonus modifiers.

**Phase 6 — Analytics & Exports**
*   **Purpose**: Complex 2D Matrix calculations, "At Risk" logic, PDF/Excel buffer generation via Apache POI / PDF Canvas.

**Phase 7 — Vault & System Safety**
*   **Purpose**: SQLite raw backup/restore via SAF, Database Merge Conflict Resolver logic, Factory Reset.

**Phase 8 — Polish & Parity**
*   **Purpose**: Animations, Dark theme matching, Toast notifications styling, Edge cases.

## 10. Feature-to-Phase Matrix

| Feature ID | Feature | Phase | Risk | Must Match Desktop? |
| :--- | :--- | :--- | :--- | :--- |
| ATT-001 | License Validation | Phase 1 | High | Yes (Security) |
| ATT-002 | Setup Wizard | Phase 1 | Low | Yes |
| ATT-004 | Start/Stop Session | Phase 3 | Critical | Yes |
| ATT-005 | Camera Scanning | Phase 3 | Critical | Yes (Must use CameraX) |
| ATT-010 | Session Auto-Export | Phase 6 | Medium | Yes |
| ATT-011 | Roster Import (CSV) | Phase 4 | Medium | Yes |
| ATT-014 | Archive/Delete Student | Phase 4 | Low | Yes |
| ATT-017 | Generate QR Bundle | Phase 4 | High | Yes |
| ATT-019 | Session Audit Ledger | Phase 5 | Medium | Yes |
| ATT-024 | Analytics Matrix | Phase 6 | High | Yes |
| ATT-026 | Export Master Report | Phase 6 | High | Yes |
| ATT-029 | Database Backup | Phase 7 | Critical | Yes |
| ATT-030 | Import & Merge | Phase 7 | Critical | Yes (Requires precise UI logic) |

## 11. Dependencies & Critical Path
*   **Database (Phase 0)** must perfectly mirror desktop SQLite to ensure exported backups (`.attdb`) are interchangeable between Desktop and Android.
*   **QR Hash Logic (Phase 3)** must use the exact same `[NationalID] + [SecretKey] + [AcademicYear]` SHA-256 algorithm, otherwise the Android app cannot read desktop-generated QRs, and vice versa.

## 12. Migration Risks
*   **Critical**: **SQLite Cross-Compatibility**. The `.attdb` file exported from Desktop must be readable by Android's Room, and vice-versa. Android Room handles SQLite slightly differently regarding metadata tables (`room_master_table`).
    *   *Mitigation*: Provide custom `SupportSQLiteOpenHelper` configurations if needed, or ensure Room migrations don't corrupt pure SQLite tables used by Tauri.
*   **High**: **Export Generation (PDF/Excel)**. JS libraries (ExcelJS, JSPDF) are used on Desktop. Replicating exact layouts in Java/Kotlin libraries is time-consuming.
    *   *Mitigation*: Use strict unit tests comparing output.
*   **Medium**: **Analytics UI Performance**. Rendering a huge matrix on mobile can cause dropped frames.
    *   *Mitigation*: Use `LazyRow` inside `LazyColumn` for efficient recycling.

## 13. Functional Parity Checklist
*   [ ] Hardware ID bounds license securely.
*   [ ] System clock tampering locks the app.
*   [ ] Scanner validates secure QR hashes.
*   [ ] Beeps and haptics fire on scan success/fail.
*   [ ] Cold Call randomizer works.
*   [ ] CSV parsing handles Arabic text encoding correctly (UTF-8 / Windows-1256 fallback).
*   [ ] Merging databases resolves conflicts correctly.
*   [ ] At-Risk calculation exactly matches desktop math.

## 14. Gap Analysis (Second Pass Verification)
During the secondary analysis pass, the following nuances were confirmed:
*   **Arabic Support**: The desktop app includes `Amiri-Regular.ttf` in its assets and explicitly checks for Arabic text `[\u0600-\u06FF]` to apply right-to-left layout in PDF exports. **The Android PDF generator MUST replicate this RTL logic.**
*   **Export Shadow Backups**: The desktop auto-saves backups silently on session end to a specific folder. Android must use WorkManager or a specific user-granted Document Tree URI to save these silently.
*   **Hold-to-Peek Shortcuts**: Desktop has a UI overlay showing shortcuts. **OMITTED ON ANDROID** (No keyboard).
*   **Rapid Paper-to-Digital**: Needs optimization for Android's soft keyboard and screen size.

## 15. Final Migration Summary
The Attendo application is an advanced, offline-first, highly specialized data entry tool. Its migration to Android is highly feasible and will likely result in a superior scanning experience due to native CameraX integration. The primary challenges lie in adapting dense desktop UIs (Analytics Matrix) to mobile screens and swapping JavaScript PDF/Excel generators for Kotlin equivalents while maintaining identical output formatting (especially regarding RTL Arabic text). If the Data Model and DB Schema are preserved 1:1, the resulting Android app will serve as a perfect companion or replacement for the Desktop software.
