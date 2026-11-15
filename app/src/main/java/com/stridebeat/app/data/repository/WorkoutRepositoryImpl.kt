package com.stridebeat.app.data.repository

import com.stridebeat.app.data.local.room.converter.Converters
import com.stridebeat.app.data.local.room.dao.WorkoutDao
import com.stridebeat.app.data.local.room.entity.WorkoutSessionEntity
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.domain.repository.WorkoutRepository
import com.stridebeat.app.util.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    private val converters = Converters()

    init {
        // Inisialisasi data awal ke Room jika database masih kosong
        CoroutineScope(Dispatchers.IO).launch {
            seedSampleSessionsIfEmpty()
        }
    }

    private suspend fun seedSampleSessionsIfEmpty() {
        // Ambil list pertama secara langsung
        SampleData.sampleSessionHistory.forEach { sample ->
            val existing = workoutDao.getSessionById(sample.id)
            if (existing == null) {
                workoutDao.insertSession(mapToEntity(sample))
            }
        }
    }

    override fun getAllSessions(): Flow<List<WorkoutSession>> {
        return workoutDao.getAllSessions().map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    override suspend fun getSessionById(id: String): WorkoutSession? {
        val entity = workoutDao.getSessionById(id) ?: return null
        return mapToDomain(entity)
    }

    override suspend fun saveSession(session: WorkoutSession) {
        workoutDao.insertSession(mapToEntity(session))
    }

    override suspend fun deleteSession(id: String) {
        workoutDao.deleteSessionById(id)
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
            musicSource = musicSource,
            playedTracks = converters.toPlayedTrackList(entity.playedTracksJson),
            stravaActivityId = entity.stravaActivityId,
            gpsInterrupted = entity.gpsInterrupted
        )
    }
}
