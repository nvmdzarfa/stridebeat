package com.stridebeat.app.domain.model

enum class StravaUploadStatus {
    NOT_UPLOADED,
    PENDING,
    UPLOADED,
    FAILED
}

/**
 * Model data sesi olahraga yang merangkum rute GPS, metrik kebugaran, dan lagu yang didengar.
 */
data class WorkoutSession(
    val id: String,
    val type: WorkoutType,
    val startedAt: Long,
    val endedAt: Long? = null,
    val distanceMeters: Double = 0.0,
    val durationMs: Long = 0L,
    val avgPaceMinPerKm: Double? = null,
    val caloriesKcal: Double? = null, // Beri label "(estimasi)" di UI
    val routePoints: List<LatLng> = emptyList(),
    val musicSource: MusicSource? = null,
    val playedTracks: List<PlayedTrack> = emptyList(),
    val stravaActivityId: String? = null,
    val stravaUploadStatus: StravaUploadStatus = if (stravaActivityId != null) StravaUploadStatus.UPLOADED else StravaUploadStatus.NOT_UPLOADED,
    val isImportedFromStrava: Boolean = false,
    val stravaActivityName: String? = null,
    val gpsInterrupted: Boolean = false
)
