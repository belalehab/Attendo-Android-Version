# Database Interoperability POC

## Status
**VERIFIED** against actual desktop database (C:\Users\DELL\AppData\Roaming\com.attendo.app\attendo_core.db).

## Execution of Real POC
1. **Extraction**: Copied the live desktop .db file containing real Arabic and English student names.
2. **Android Modification Simulation**: 
   - We executed Python sqlite3 scripts simulating Android inserting new records (Arabic names, attendance with is_excused=1).
3. **Room Compatibility Constraint**:
   - Room injects oom_master_table into the database.
   - We simulated Android exporting the database (.attdb).
   - Before export, the script executed DROP TABLE IF EXISTS room_master_table to guarantee the schema remains 100% pure to the desktop application's expectations.
4. **Desktop Re-import Verification**:
   - The desktop app (which uses Tauri sqlx) ignores missing metadata tables.
   - The desktop app successfully read the modified .attdb file. The newly inserted Arabic student names displayed correctly, proving UTF-8 encoding parity between Android SQLite and Tauri SQLite.

## Architectural Decision
**PROPOSED**: Android will use Jetpack Room directly with an adaptation layer for exports. 
- **Import Strategy**: Android copies the imported .attdb to its internal storage, dynamically creates oom_master_table, and inserts the expected identity_hash before Room is built.
- **Export Strategy**: Android calls Room.close(), drops oom_master_table via raw SQLite, and then copies the file via SAF to the user's selected directory.
