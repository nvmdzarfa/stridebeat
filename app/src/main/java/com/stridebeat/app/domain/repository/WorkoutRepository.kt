package com.stridebeat.app.domain.repository

import com.stridebeat.app.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

/**
 * Interface repositori data sesi olahraga (Clean Architecture).
 * Implementasi konkret berada di layer data/repository (Room DB).
 */
interface WorkoutRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    suspend fun getSessionById(id: String): WorkoutSession?
    suspend fun saveSession(session: WorkoutSession)
    suspend fun deleteSession(id: String)
}
