package com.stridebeat.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String,
    val type: String,               // RUN, WALK, RIDE
    val startedAt: Long,
    val endedAt: Long?,
    val distanceMeters: Double,
    val durationMs: Long,
    val avgPaceMinPerKm: Double?,
    val caloriesKcal: Double?,       // Estimasi kalori
    val routePointsJson: String,     // Serialized List<LatLng>
    val musicSource: String?,        // SPOTIFY, YOUTUBE_MUSIC
    val playedTracksJson: String,    // Serialized List<PlayedTrack>
    val stravaActivityId: String?,
    val stravaUploadStatus: String = "NOT_UPLOADED",
    val isImportedFromStrava: Boolean = false,
    val stravaActivityName: String? = null,
    val gpsInterrupted: Boolean
)
