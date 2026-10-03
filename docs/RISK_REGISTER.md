# Risk Register

## Critical Risks
1. **Database Cross-Compatibility (.attdb)**: [VERIFIED] Room expects oom_master_table. 
   - *Mitigation* [PROPOSED]: The export/import adaptation layer drops/injects this table automatically. Real POC round-trip proves this resolves the issue without breaking Tauri.
2. **QR Hashing Validation**: [VERIFIED] Desktop uses [NationalID] + [SecretKey] + [AcademicYear] -> SHA-256 hex string.
   - *Mitigation* [PROPOSED]: Android must implement exact byte-for-byte SHA-256 equivalent. Verified via test vectors.

## High Risks
1. **PDF Export Parity**: [VERIFIED] Desktop embeds Amiri-Regular.ttf. Android's native PdfDocument cannot embed fonts.
   - *Mitigation* [PROPOSED]: Must use iText or Apache PDFBox to embed the TrueType font.
2. **Merge Algorithm Parity**: [VERIFIED] Desktop merge uses primary keys (
ational_id + session_name) and prompts conflicts if is_excused or onus_points differ. 
   - *Mitigation* [PROPOSED]: Port the exact TS merge loop to Kotlin.

## Medium Risks
1. **Silent Shadow Backups**: [VERIFIED] Android restricts background file writing.
   - *Mitigation* [PROPOSED]: Use WorkManager with ACTION_OPEN_DOCUMENT_TREE persisted URI. Gracefully degrade if URI permission is revoked.
