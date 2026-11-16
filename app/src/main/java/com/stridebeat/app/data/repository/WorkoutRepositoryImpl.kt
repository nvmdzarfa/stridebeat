package com.stridebeat.app.data.repository

import com.stridebeat.app.data.local.room.converter.Converters
import com.stridebeat.app.data.local.room.dao.WorkoutDao
import com.stridebeat.app.data.local.room.entity.WorkoutSessionEntity
import com.stridebeat.app.data.remote.firebase.FirebaseSyncManager
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.StravaUploadStatus
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val firebaseSyncManager: FirebaseSyncManager
) : WorkoutRepository {

    private val converters = Converters()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            cleanUpDummySessions()
        }
    }

    private suspend fun cleanUpDummySessions() {
        try {
            workoutDao.deleteSampleSessions()
        } catch (_: Exception) {
            listOf("sample_session_1", "sample_session_2", "sample_session_3", "sample-1", "sample-2", "sample-3").forEach { id ->
                workoutDao.deleteSessionById(id)
            }
        }
    }

    override fun getAllSessions(): Flow<List<WorkoutSession>> {
        return workoutDao.getAllSessions().map { entities ->
            entities
                .filterNot { it.id.startsWith("sample") }
                .map { mapToDomain(it) }
        }
    }

    override suspend fun getSessionById(id: String): WorkoutSession? {
        val entity = workoutDao.getSessionById(id) ?: return null
        return mapToDomain(entity)
    }

    override suspend fun saveSession(session: WorkoutSession) {
        val entity = mapToEntity(session)
        workoutDao.insertSession(entity)
        firebaseSyncManager.uploadWorkoutEntity(entity)
    }

    override suspend fun deleteSession(id: String) {
        workoutDao.deleteSessionById(id)
        firebaseSyncManager.deleteWorkoutFromCloud(id)
    }

    private fun mapToEntity(session: WorkoutSession): WorkoutSessionEntity {
        return WorkoutSessionEntity(
            id = session.id,
            type = session.type.name,
            startedAt = session.startedAt,
            endedAt = session.endedAt,
            distanceMeters = session.distanceMeters,
            durationMs = session.durationMs,
            avgPaceMinPerKm = session.avgPaceMinPerKm,
            caloriesKcal = session.caloriesKcal,
            routePointsJson = converters.fromLatLngList(session.routePoints),
            musicSource = session.musicSource?.name,
            playedTracksJson = converters.fromPlayedTrackList(session.playedTracks),
            stravaActivityId = session.stravaActivityId,
            stravaUploadStatus = session.stravaUploadStatus.name,
            isImportedFromStrava = session.isImportedFromStrava,
            stravaActivityName = session.stravaActivityName,
            gpsInterrupted = session.gpsInterrupted
        )
    }

    private fun mapToDomain(entity: WorkoutSessionEntity): WorkoutSession {
        val workoutType = try {
            WorkoutType.valueOf(entity.type)
        } catch (_: Exception) {
            WorkoutType.RUN
        }

        val musicSource = entity.musicSource?.let {
            try { MusicSource.valueOf(it) } catch (_: Exception) { null }
        }

        val uploadStatus = try {
            StravaUploadStatus.valueOf(entity.stravaUploadStatus)
        } catch (_: Exception) {
            if (!entity.stravaActivityId.isNullOrBlank()) StravaUploadStatus.UPLOADED else StravaUploadStatus.NOT_UPLOADED
        }

        val playedTracks = converters.toPlayedTrackList(entity.playedTracksJson)
        val finalMusicSource = when {
            musicSource == MusicSource.YOUTUBE_MUSIC && playedTracks.isNotEmpty() && playedTracks.all { it.source == MusicSource.SOUNDCLOUD } -> MusicSource.SOUNDCLOUD
            else -> musicSource
        }

        return WorkoutSession(
            id = entity.id,
            type = workoutType,
            startedAt = entity.startedAt,
            endedAt = entity.endedAt,
            distanceMeters = entity.distanceMeters,
            durationMs = entity.durationMs,
            avgPaceMinPerKm = entity.avgPaceMinPerKm,
            caloriesKcal = entity.caloriesKcal,
            routePoints = converters.toLatLngList(entity.routePointsJson),
            musicSource = finalMusicSource,
            playedTracks = playedTracks,
            stravaActivityId = entity.stravaActivityId,
            stravaUploadStatus = uploadStatus,
            isImportedFromStrava = entity.isImportedFromStrava,
            stravaActivityName = entity.stravaActivityName,
            gpsInterrupted = entity.gpsInterrupted
        )
    }

}
