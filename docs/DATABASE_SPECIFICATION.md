# Database Specification

## SQLite / Room Equivalence

The desktop app uses 	auri-plugin-sql mapping to a raw SQLite database.
To ensure interoperability (.attdb files), Android Room Entities must perfectly match:

### Entity: students
`kotlin
@Entity(tableName = "students", indices = [Index(value = ["national_id"], unique = true)])
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "national_id") val nationalId: String?,
    @ColumnInfo(name = "grade") val grade: String?,
    @ColumnInfo(name = "status", defaultValue = "'offline'") val status: String? = "offline",
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Int? = 0
)
`

### Entity: attendance
`kotlin
@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "national_id") val nationalId: String?,
    @ColumnInfo(name = "session_name") val sessionName: String?,
    @ColumnInfo(name = "timestamp", defaultValue = "CURRENT_TIMESTAMP") val timestamp: String?,
    @ColumnInfo(name = "is_excused", defaultValue = "0") val isExcused: Int? = 0,
    @ColumnInfo(name = "bonus_points", defaultValue = "0") val bonusPoints: Int? = 0,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Int? = 0,
    @ColumnInfo(name = "excuse_reason") val excuseReason: String? = null,
    @ColumnInfo(name = "audit_trail", defaultValue = "'[]'") val auditTrail: String? = "[]"
)
`

### Entity: settings
`kotlin
@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String?
)
`

## Critical Interoperability Gates
1. **Types**: Android Room maps SQLite INTEGER to Kotlin Int or Long. We must use Int in Kotlin to match is_deleted and is_excused instead of Boolean.
2. **Room Metadata**: Room injects oom_master_table. Tauri ignores this. But when Android imports a Tauri DB, it lacks oom_master_table. Android will crash during Room.databaseBuilder().build() if it cannot verify the identity hash. We must implement a pre-packaged DB callback or run CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT) with the correct hash before Room opens the file.
