package com.stridebeat.app.domain.model

enum class WorkoutTargetType(val displayName: String) {
    NONE("Bebas"),
    DISTANCE("Jarak"),
    DURATION("Waktu")
}

data class WorkoutTarget(
    val type: WorkoutTargetType = WorkoutTargetType.NONE,
    val targetDistanceMeters: Double = 0.0,
    val targetDurationMs: Long = 0L
) {
    val isActive: Boolean
        get() = when (type) {
            WorkoutTargetType.DISTANCE -> targetDistanceMeters > 0.0
            WorkoutTargetType.DURATION -> targetDurationMs > 0L
            WorkoutTargetType.NONE -> false
        }

    fun calculateProgress(currentDistanceMeters: Double, currentDurationMs: Long): Float {
        if (!isActive) return 0f
        return when (type) {
            WorkoutTargetType.DISTANCE -> {
                if (targetDistanceMeters <= 0.0) 0f
                else (currentDistanceMeters / targetDistanceMeters).toFloat().coerceIn(0f, 1f)
            }
            WorkoutTargetType.DURATION -> {
                if (targetDurationMs <= 0L) 0f
                else (currentDurationMs.toFloat() / targetDurationMs.toFloat()).coerceIn(0f, 1f)
            }
            WorkoutTargetType.NONE -> 0f
        }
    }

    companion object {
        val PRESET_DISTANCES = listOf(
            1000.0 to "1 km",
            3000.0 to "3 km",
            5000.0 to "5 km",
            10000.0 to "10 km"
        )

        val PRESET_DURATIONS = listOf(
            15 * 60 * 1000L to "15 mnt",
            30 * 60 * 1000L to "30 mnt",
            45 * 60 * 1000L to "45 mnt",
            60 * 60 * 1000L to "60 mnt"
        )
    }
}
