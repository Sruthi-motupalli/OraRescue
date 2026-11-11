package com.ora.rescue

import java.time.LocalDate

/**
 * Pure validation utility for free-time windows.
 * Enforces business rules:
 * - Start must be strictly before end (positive duration).
 * - Must reside within a single calendar day (00:00 to 24:00, no accidental cross-midnight).
 * - Dates must be today or in the future.
 * - Windows on the same day must not overlap. Adjacent windows (e.g. 16:00–17:00 and 17:00–18:00) are permitted.
 * - Duplicate windows are rejected.
 */
object TimeWindowValidator {

    fun validateWindow(
        day: Long,
        startMinute: Int,
        endMinute: Int,
        existingWindows: List<TimeWindow>,
        today: LocalDate = LocalDate.now(),
        ignoreId: Long = 0L
    ): String? {
        val todayEpoch = today.toEpochDay()
        if (day < todayEpoch) {
            return "Cannot set availability for past dates."
        }

        if (startMinute < 0 || startMinute > 1439) {
            return "Start time must be between 00:00 and 23:59."
        }
        if (endMinute < 1 || endMinute > 1440) {
            return "End time must be between 00:01 and 24:00."
        }
        if (startMinute >= endMinute) {
            return "Start time (${TimeWindow.formatMinuteOfDay(startMinute)}) must be earlier than end time (${TimeWindow.formatMinuteOfDay(endMinute)})."
        }

        val duration = endMinute - startMinute
        if (duration < 5) {
            return "Window duration must be at least 5 minutes."
        }

        // Check against existing windows for the same day
        for (w in existingWindows.filter { it.day == day && it.id != ignoreId }) {
            if (w.startMinute == startMinute && w.endMinute == endMinute) {
                return "This exact time window (${w.timeRangeString}) is already added."
            }

            // Overlap condition: max(start1, start2) < min(end1, end2)
            // If max(start1, start2) == min(end1, end2), they meet at the boundary and are adjacent, which is allowed.
            val overlapStart = maxOf(startMinute, w.startMinute)
            val overlapEnd = minOf(endMinute, w.endMinute)
            if (overlapStart < overlapEnd) {
                return "Time window (${TimeWindow.formatMinuteOfDay(startMinute)}–${TimeWindow.formatMinuteOfDay(endMinute)}) overlaps with existing window (${w.timeRangeString})."
            }
        }

        return null // Valid!
    }

    /** Returns true if two time intervals overlap (strictly interior intersection). */
    fun overlaps(s1: Int, e1: Int, s2: Int, e2: Int): Boolean {
        return maxOf(s1, s2) < minOf(e1, e2)
    }
}
