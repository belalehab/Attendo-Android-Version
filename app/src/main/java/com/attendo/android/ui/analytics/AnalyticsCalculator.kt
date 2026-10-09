package com.attendo.android.ui.analytics

import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.Student

object AnalyticsCalculator {

    fun getBaseName(sessionName: String): String {
        val parts = sessionName.split(" - ")
        return if (parts.size > 1) {
            parts.dropLast(1).joinToString(" - ")
        } else {
            sessionName
        }
    }

    fun calculate(
        students: List<Student>,
        attendances: List<Attendance>,
        threshold: Int,
        searchQuery: String,
        sessionTypeFilter: String,
        workspace: String
    ): AnalyticsUiState {
        // 1. Exclude archived sessions (A-05) and ensure workspace match
        val activeAttendances = attendances.filter { 
            (it.isArchived ?: 0) == 0 && 
            (workspace.isBlank() || it.sessionName?.startsWith("[Grade $workspace]") == true || it.sessionName?.contains("Grade $workspace") == true)
        }

        // 2. Filter by session type
        val typeFilteredAttendances = if (sessionTypeFilter == "All Sessions") {
            activeAttendances
        } else {
            activeAttendances.filter { it.sessionName?.contains(sessionTypeFilter, ignoreCase = true) == true }
        }

        // 3. Extract distinct sessions sorted chronologically by timestamp
        val sessionWithTime = typeFilteredAttendances
            .filter { !it.sessionName.isNullOrBlank() }
            .groupBy { it.sessionName!! }
            .mapValues { (_, records) -> records.mapNotNull { it.timestamp }.minOrNull() ?: "" }
            .toList()
            .sortedWith(compareBy({ it.second }, { it.first }))
            .map { it.first }

        // 4. Group sessions by logical week (A-01)
        val logicalWeeksMap = LinkedHashMap<String, MutableList<String>>()
        sessionWithTime.forEach { sessionName ->
            val baseName = getBaseName(sessionName)
            logicalWeeksMap.getOrPut(baseName) { mutableListOf() }.add(sessionName)
        }
        val logicalWeeks = logicalWeeksMap.keys.toList()
        val totalSessionsCount = logicalWeeks.size
        val totalWeeks = if (totalSessionsCount > 0) totalSessionsCount else 1

        // 5. Calculate Turnout Trend & Average Turnout
        val turnoutTrend = logicalWeeks.mapIndexed { index, baseName ->
            val groupSessions = logicalWeeksMap[baseName] ?: emptyList()
            val attendeesInWeek = typeFilteredAttendances.filter { record ->
                groupSessions.contains(record.sessionName) && ((record.isExcused ?: 0) == 0)
            }.mapNotNull { it.nationalId }.distinct().size

            val weekMatch = Regex("""Week (\d+)""", RegexOption.IGNORE_CASE).find(baseName)
            val label = weekMatch?.let { "W${it.groupValues[1]}" } ?: "W${index + 1}"

            SessionTurnout(sessionIndex = index, turnout = attendeesInWeek, label = label)
        }
        val avgTurnout = if (turnoutTrend.isNotEmpty()) turnoutTrend.sumOf { it.turnout } / turnoutTrend.size else 0

        // 6. Map each student according to Desktop logic (A-01, A-02, A-03)
        val allAttendance = typeFilteredAttendances
        val query = searchQuery.trim().lowercase()
        val stats = students.filter {
            query.isEmpty() || it.name.lowercase().contains(query) || (it.nationalId?.contains(query) == true)
        }.map { student ->
            var attended = 0
            var excused = 0
            var bonuses = 0

            val sparkline = logicalWeeks.map { baseName ->
                val groupSessions = logicalWeeksMap[baseName] ?: emptyList()
                val records = allAttendance.filter {
                    it.nationalId == student.nationalId && groupSessions.contains(it.sessionName)
                }

                if (records.isEmpty()) {
                    "absent"
                } else {
                    records.forEach { r ->
                        val b = r.bonusPoints ?: 0
                        if (b > 0) bonuses += b
                    }

                    val isPresent = records.any { (it.isExcused ?: 0) == 0 }
                    val isExcused = records.any { (it.isExcused ?: 0) == 1 } && !isPresent

                    if (isExcused) {
                        excused++
                        "excused"
                    } else {
                        attended++
                        if (records.any { (it.bonusPoints ?: 0) > 0 }) "bonus" else "present"
                    }
                }
            }

            val absent = maxOf(0, totalSessionsCount - (attended + excused))
            val isAtRisk = absent >= threshold

            StudentStats(
                studentName = student.name,
                nationalId = student.nationalId ?: "",
                attendedCount = attended,
                absentCount = absent,
                excusedCount = excused,
                bonusPoints = bonuses,
                isAtRisk = isAtRisk,
                recentTrend = sparkline.takeLast(10)
            )
        }.sortedByDescending { it.isAtRisk }

        val totalAtRisk = stats.count { it.isAtRisk }
        val totalSafe = stats.size - totalAtRisk

        return AnalyticsUiState(
            stats = stats,
            totalSessions = totalSessionsCount,
            totalAtRisk = totalAtRisk,
            totalSafe = totalSafe,
            avgTurnout = avgTurnout,
            totalWeeks = totalWeeks,
            currentWeekIndex = 0,
            turnoutTrend = turnoutTrend,
            threshold = threshold,
            searchQuery = searchQuery,
            sessionTypeFilter = sessionTypeFilter
        )
    }
}
