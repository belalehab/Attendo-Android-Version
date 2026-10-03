# Requirements Traceability Matrix

| ID | Feature | Target Phase | Target Component | Test Strategy | Parity Verification | Status |
|---|---|---|---|---|---|---|
| ATT-001 | License Validation | Phase 1 | LicenseViewModel, SettingsRepo | Unit test JWT logic | Mock hardware ID vs Settings.Secure | ⚠️ Missing precise HW ID equivalent |
| ATT-002 | Setup Wizard | Phase 1 | SetupScreen, SettingsRepo | UI Tests | Compare fields | Active |
| ATT-003 | Workspace Selector | Phase 2 | WorkspaceSelector, MainViewModel | UI Tests | Check visual layout | Active |
| ATT-004 | Start/Stop Session | Phase 3 | ScannerViewModel, Room DB | Unit Tests | DB insertion check | Active |
| ATT-005 | Camera Scanning | Phase 3 | CameraXPreview, MLKitAnalyzer | Instrumentation | Scan identical QR | Active |
| ATT-006 | Mobile Companion Scanner | N/A | N/A (App IS scanner) | N/A | Feature obsolete | ✅ Dropped natively |
| ATT-007 | Manual ID Entry | Phase 3 | ManualEntryDialog | Unit Tests | String processing check | Active |
| ATT-008 | Rapid Paper-to-Digital | Phase 3 | RapidEntryBottomSheet | UI Tests | UX comparison | ⚠️ Needs adaptation for touch |
| ATT-009 | Cold Call Randomizer | Phase 3 | ScannerViewModel | Unit Tests | Random distribution | Active |
| ATT-010 | Session Auto-Export | Phase 6 | WorkManager, Apache POI | Instrumentation | Binary compare XLSX | ⚠️ SAF silent export tricky |
| ATT-011 | Roster Import (CSV) | Phase 4 | RosterViewModel, CSVParser | Unit Tests | Compare DB states | Active |
| ATT-012 | Add Single Student | Phase 4 | AddStudentDialog | UI Tests | Check DB | Active |
| ATT-013 | Edit Student Name | Phase 4 | EditStudentDialog | UI Tests | Check DB | Active |
| ATT-014 | Archive Student | Phase 4 | RosterViewModel | Unit Tests | is_deleted == 1 | Active |
| ATT-015 | Restore Student | Phase 4 | RosterViewModel | Unit Tests | is_deleted == 0 | Active |
| ATT-016 | Permanent Delete | Phase 4 | RosterViewModel | Unit Tests | Check cascade | Active |
| ATT-017 | Generate QR Bundle | Phase 4 | Canvas, ZipOutputStream | Instrumentation | Unzip and compare | Active |
| ATT-018 | View Past Sessions | Phase 5 | HistoryScreen | UI Tests | Check list items | Active |
| ATT-019 | Session Audit Ledger | Phase 5 | AuditLedgerDialog | UI Tests | Check toggles | Active |
| ATT-020 | Toggle Attendance | Phase 5 | HistoryViewModel | Unit Tests | Check DB state | Active |
| ATT-021 | Mark Excused | Phase 5 | HistoryViewModel | Unit Tests | Check DB state | Active |
| ATT-022 | Add Bonus Points | Phase 5 | HistoryViewModel | Unit Tests | Check math | Active |
| ATT-023 | Re-Export Session | Phase 5 | ExportUseCase | Instrumentation | File parity check | Active |
| ATT-024 | Analytics Matrix | Phase 6 | AnalyticsScreen | UI Tests | Scroll behavior | Active |
| ATT-025 | 'At Risk' Calculation | Phase 6 | AnalyticsViewModel | Unit Tests | Compare algorithm | Active |
| ATT-026 | Export Master Report | Phase 6 | ExportUseCase, JSPDF/POI | Instrumentation | File parity check | ⚠️ RTL PDF rendering risk |
| ATT-027 | Global Search | Phase 2 | MainViewModel | Unit Tests | Filter lists | Active |
| ATT-028 | Command Palette | Phase 2 | SearchBar/BottomBar | UI Tests | Accessibility check | ⚠️ Adapted to native search |
| ATT-029 | Database Backup | Phase 7 | DatabaseManager | Instrumentation | Export and SHA check | Active |
| ATT-030 | Import & Merge | Phase 7 | MergeUseCase | Unit Tests | Desktop interoperability | ⚠️ DB structure critical |
| ATT-031 | Factory Reset | Phase 7 | DatabaseManager | Unit Tests | Check empty tables | Active |
| ATT-032 | Hardware Feedback | Phase 3 | Vibrator, ToneGenerator | Manual | Audio parity | Active |
