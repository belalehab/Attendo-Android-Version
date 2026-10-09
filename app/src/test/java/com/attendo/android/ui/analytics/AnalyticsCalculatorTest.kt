package com.attendo.android.ui.analytics

import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsCalculatorTest {

    private val workspace = "1"

    @Test
    fun testLogicalWeekGrouping_MultipleGroupsPerWeek_CountsAsOneSession() {
        val studentA = Student(id = 1, nationalId = "101", name = "Alice", grade = workspace)
        val studentB = Student(id = 2, nationalId = "102", name = "Bob", grade = workspace)
        val studentC = Student(id = 3, nationalId = "103", name = "Charlie", grade = workspace)
        val studentD = Student(id = 4, nationalId = "104", name = "Diana", grade = workspace)
        val students = listOf(studentA, studentB, studentC, studentD)

        // Week 1 has two groups
        val s1 = "[Grade 1] Math - Week 1 - Lecture - Group 1"
        val s2 = "[Grade 1] Math - Week 1 - Lecture - Group 2"

        val attendances = listOf(
            Attendance(id = 1, nationalId = "101", sessionName = s1, timestamp = "2026-10-01 10:00:00", isExcused = 0),
            Attendance(id = 2, nationalId = "102", sessionName = s2, timestamp = "2026-10-01 11:00:00", isExcused = 0),
            Attendance(id = 3, nationalId = "103", sessionName = s1, timestamp = "2026-10-01 10:00:00", isExcused = 0),
            Attendance(id = 4, nationalId = "103", sessionName = s2, timestamp = "2026-10-01 11:00:00", isExcused = 0) // attended both
        )

        val result = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 3,
            searchQuery = "",
            sessionTypeFilter = "All Sessions",
            workspace = workspace
        )

        // A-01: Logical weeks should collapse to 1 week
        assertEquals(1, result.totalSessions)
        assertEquals(1, result.totalWeeks)

        // Turnout should be 3 unique students in Week 1
        assertEquals(1, result.turnoutTrend.size)
        assertEquals(3, result.turnoutTrend[0].turnout)
        assertEquals(3, result.avgTurnout)

        val statsMap = result.stats.associateBy { it.nationalId }
        
        // Alice attended Group 1 -> Present, absent = 0
        val alice = statsMap["101"]!!
        assertEquals(1, alice.attendedCount)
        assertEquals(0, alice.absentCount)
        assertFalse(alice.isAtRisk)

        // Bob attended Group 2 -> Present, absent = 0
        val bob = statsMap["102"]!!
        assertEquals(1, bob.attendedCount)
        assertEquals(0, bob.absentCount)
        assertFalse(bob.isAtRisk)

        // Charlie attended both -> Present once, absent = 0
        val charlie = statsMap["103"]!!
        assertEquals(1, charlie.attendedCount)
        assertEquals(0, charlie.absentCount)
        assertFalse(charlie.isAtRisk)

        // Diana attended neither -> Absent = 1
        val diana = statsMap["104"]!!
        assertEquals(0, diana.attendedCount)
        assertEquals(1, diana.absentCount)
    }

    @Test
    fun testExcusedAbsenceTracking_DoesNotPenalizeStudent() {
        val student = Student(id = 1, nationalId = "101", name = "Alice", grade = workspace)
        val students = listOf(student)

        val s1 = "[Grade 1] Math - Week 1 - Lecture - Group 1"
        val s2 = "[Grade 1] Math - Week 2 - Lecture - Group 1"

        val attendances = listOf(
            Attendance(id = 1, nationalId = "101", sessionName = s1, timestamp = "2026-10-01 10:00:00", isExcused = 0),
            Attendance(id = 2, nationalId = "101", sessionName = s2, timestamp = "2026-10-08 10:00:00", isExcused = 1, excuseReason = "Medical")
        )

        val result = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 1,
            searchQuery = "",
            sessionTypeFilter = "All Sessions",
            workspace = workspace
        )

        assertEquals(2, result.totalSessions)

        val alice = result.stats.first()
        // A-02: 1 attended, 1 excused -> absent = 2 - (1 + 1) = 0
        assertEquals(1, alice.attendedCount)
        assertEquals(1, alice.excusedCount)
        assertEquals(0, alice.absentCount)
        assertFalse(alice.isAtRisk) // absentCount is 0, so < threshold 1

        // A-03: Sparkline statuses
        assertEquals(listOf("present", "excused"), alice.recentTrend)
    }

    @Test
    fun testStudentSparkline_EnrichedStatuses_AndDepthOf10() {
        val student = Student(id = 1, nationalId = "101", name = "Alice", grade = workspace)
        val students = listOf(student)

        val attendances = mutableListOf<Attendance>()
        for (w in 1..12) {
            val sName = "[Grade 1] Math - Week $w - Lecture - Group 1"
            when (w) {
                1 -> attendances.add(Attendance(id = 100, nationalId = "999", sessionName = sName, timestamp = "2026-01-01 10:00:00", isExcused = 0)) // Alice absent
                2 -> attendances.add(Attendance(id = w, nationalId = "101", sessionName = sName, timestamp = "2026-02-01 10:00:00", isExcused = 0))
                3 -> attendances.add(Attendance(id = w, nationalId = "101", sessionName = sName, timestamp = "2026-03-01 10:00:00", isExcused = 0, bonusPoints = 3))
                4 -> attendances.add(Attendance(id = w, nationalId = "101", sessionName = sName, timestamp = "2026-04-01 10:00:00", isExcused = 1))
                else -> {
                    val monthStr = if (w < 10) "0$w" else "$w"
                    attendances.add(Attendance(id = w, nationalId = "101", sessionName = sName, timestamp = "2026-$monthStr-01 10:00:00", isExcused = 0))
                }
            }
        }

        val result = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 3,
            searchQuery = "",
            sessionTypeFilter = "All Sessions",
            workspace = workspace
        )

        assertEquals(12, result.totalSessions)
        val alice = result.stats.first()

        // A-03: Max depth is 10
        assertEquals(10, alice.recentTrend.size)
        // Weeks 3 to 12
        assertEquals("bonus", alice.recentTrend[0]) // Week 3 has bonus
        assertEquals("excused", alice.recentTrend[1]) // Week 4 has excuse
        for (i in 2..9) {
            assertEquals("present", alice.recentTrend[i]) // Weeks 5-12 present
        }

        // A-04: Bonuses recorded
        assertEquals(3, alice.bonusPoints)
    }

    @Test
    fun testArchivedSessions_AreExcludedFromCalculations() {
        val student = Student(id = 1, nationalId = "101", name = "Alice", grade = workspace)
        val students = listOf(student)

        val s1 = "[Grade 1] Math - Week 1 - Lecture - Group 1"
        val s2Archived = "[Grade 1] Math - Week 2 - Lecture - Group 1"

        val attendances = listOf(
            Attendance(id = 1, nationalId = "101", sessionName = s1, timestamp = "2026-10-01 10:00:00", isExcused = 0, isArchived = 0),
            Attendance(id = 2, nationalId = "101", sessionName = s2Archived, timestamp = "2026-10-08 10:00:00", isExcused = 0, isArchived = 1)
        )

        val result = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 3,
            searchQuery = "",
            sessionTypeFilter = "All Sessions",
            workspace = workspace
        )

        // A-05: Archived session is excluded
        assertEquals(1, result.totalSessions)
        assertEquals(1, result.totalWeeks)
        assertEquals(1, result.stats.first().attendedCount)
    }

    @Test
    fun testSessionTypeFiltering() {
        val student = Student(id = 1, nationalId = "101", name = "Alice", grade = workspace)
        val students = listOf(student)

        val sLecture = "[Grade 1] Math - Week 1 - Lecture - Group 1"
        val sSection = "[Grade 1] Math - Week 1 - Section - Group 1"

        val attendances = listOf(
            Attendance(id = 1, nationalId = "101", sessionName = sLecture, timestamp = "2026-10-01 10:00:00", isExcused = 0),
            Attendance(id = 2, nationalId = "101", sessionName = sSection, timestamp = "2026-10-01 14:00:00", isExcused = 0)
        )

        val lectureOnly = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 3,
            searchQuery = "",
            sessionTypeFilter = "Lecture",
            workspace = workspace
        )
        assertEquals(1, lectureOnly.totalSessions)

        val sectionOnly = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 3,
            searchQuery = "",
            sessionTypeFilter = "Section",
            workspace = workspace
        )
        assertEquals(1, sectionOnly.totalSessions)

        val all = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 3,
            searchQuery = "",
            sessionTypeFilter = "All Sessions",
            workspace = workspace
        )
        assertEquals(2, all.totalSessions)
    }

    @Test
    fun testThreshold_UpdatesAtRiskStatus() {
        val student = Student(id = 1, nationalId = "101", name = "Alice", grade = workspace)
        val students = listOf(student)

        val attendances = listOf(
            Attendance(id = 1, nationalId = "999", sessionName = "[Grade 1] Math - Week 1 - Lecture - Group 1", timestamp = "2026-10-01 10:00:00"),
            Attendance(id = 2, nationalId = "999", sessionName = "[Grade 1] Math - Week 2 - Lecture - Group 1", timestamp = "2026-10-08 10:00:00"),
            Attendance(id = 3, nationalId = "999", sessionName = "[Grade 1] Math - Week 3 - Lecture - Group 1", timestamp = "2026-10-15 10:00:00")
        )

        // Alice attended 0 out of 3 sessions -> absentCount = 3
        val atThreshold3 = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 3,
            searchQuery = "",
            sessionTypeFilter = "All Sessions",
            workspace = workspace
        )
        assertTrue(atThreshold3.stats.first().isAtRisk)

        val atThreshold4 = AnalyticsCalculator.calculate(
            students = students,
            attendances = attendances,
            threshold = 4,
            searchQuery = "",
            sessionTypeFilter = "All Sessions",
            workspace = workspace
        )
        assertFalse(atThreshold4.stats.first().isAtRisk)
    }
}
