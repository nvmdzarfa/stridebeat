package com.stridebeat.app.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.stridebeat.app.data.local.StravaPreferences
import com.stridebeat.app.data.remote.strava.StravaApiClient
import com.stridebeat.app.data.remote.strava.StravaAuthManager
import com.stridebeat.app.data.remote.strava.StravaRateLimitException
import com.stridebeat.app.data.remote.strava.StravaTokenExpiredException
import com.stridebeat.app.data.remote.strava.StravaTransientException
import com.stridebeat.app.domain.model.StravaUploadStatus
import com.stridebeat.app.domain.repository.AuthRepository
import com.stridebeat.app.domain.repository.WorkoutRepository
import com.stridebeat.app.service.StravaUploadWorker
import com.stridebeat.app.util.StravaFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface StravaUploadOutcome {
    data class Success(val activityId: String, val activityName: String) : StravaUploadOutcome
    data class RetryableFailure(val reason: String, val activityName: String) : StravaUploadOutcome
    data class PermanentFailure(val reason: String, val activityName: String, val isTokenExpired: Boolean = false) : StravaUploadOutcome
}

@Singleton
class StravaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stravaAuthManager: StravaAuthManager,
    private val stravaApiClient: StravaApiClient,
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val stravaPreferences: StravaPreferences
) {
    /**
     * Menandai sesi sebagai PENDING dan menjadwalkan WorkManager untuk upload ke Strava.
     * Jika offline, WorkManager otomatis menunggu hingga koneksi tersedia.
     */
    suspend fun queueUpload(
        sessionId: String,
        customName: String?,
        customDescription: String?
    ): Result<Unit> {
        if (!stravaAuthManager.isConfigured()) {
            return Result.failure(IllegalStateException("STRAVA_CLIENT_ID / SECRET belum dikonfigurasi."))
        }
        val session = workoutRepository.getSessionById(sessionId)
            ?: return Result.failure(IllegalStateException("Sesi olahraga belum tersimpan."))

        val prefix = stravaPreferences.getDefaultActivityPrefixOnce()
        val finalName = customName?.trim().takeIf { !it.isNullOrBlank() }
            ?: StravaFormatter.buildDefaultActivityName(session, prefix)
        val finalDesc = customDescription?.trim().takeIf { !it.isNullOrBlank() }
            ?: StravaFormatter.buildActivityDescription(session)

        workoutRepository.saveSession(
            session.copy(
                stravaUploadStatus = StravaUploadStatus.PENDING,
                stravaActivityName = finalName
            )
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StravaUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    StravaUploadWorker.KEY_SESSION_ID to sessionId,
                    StravaUploadWorker.KEY_ACTIVITY_NAME to finalName,
                    StravaUploadWorker.KEY_ACTIVITY_DESCRIPTION to finalDesc
                )
            )
            .addTag("strava_upload")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "strava_upload_$sessionId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        return Result.success(Unit)
    }

    /**
     * Dipanggil oleh StravaUploadWorker untuk mengeksekusi upload ke Strava API v3.
     */
    suspend fun executeUpload(
        sessionId: String,
        activityName: String?,
        description: String?
    ): StravaUploadOutcome {
        val session = workoutRepository.getSessionById(sessionId)
            ?: return StravaUploadOutcome.PermanentFailure("Sesi tidak ditemukan.", activityName ?: "Sesi StrideBeat")

        val prefix = stravaPreferences.getDefaultActivityPrefixOnce()
        val finalName = activityName?.trim().takeIf { !it.isNullOrBlank() }
            ?: session.stravaActivityName
            ?: StravaFormatter.buildDefaultActivityName(session, prefix)
        val finalDesc = description?.trim().takeIf { !it.isNullOrBlank() }
            ?: StravaFormatter.buildActivityDescription(session)

        if (!session.stravaActivityId.isNullOrBlank() && session.stravaUploadStatus == StravaUploadStatus.UPLOADED) {
            return StravaUploadOutcome.Success(session.stravaActivityId, finalName)
        }

        var tokenResult = stravaAuthManager.getValidAccessToken()
        var token = tokenResult.getOrElse { err ->
            return handleAuthError(session, finalName, err)
        }

        var uploadResult = stravaApiClient.uploadSession(token, session, finalName, finalDesc)
        // Jika 401, coba refresh 1x lalu ulangi sekali saja
        if (uploadResult.isFailure && uploadResult.exceptionOrNull() is StravaTokenExpiredException) {
            val refreshed = stravaAuthManager.refreshAccessToken()
            if (refreshed.isSuccess) {
                token = refreshed.getOrThrow()
                uploadResult = stravaApiClient.uploadSession(token, session, finalName, finalDesc)
            } else {
                return handleAuthError(session, finalName, refreshed.exceptionOrNull()!!)
            }
        }

        return if (uploadResult.isSuccess) {
            val actId = uploadResult.getOrThrow()
            workoutRepository.saveSession(
                session.copy(
                    stravaActivityId = actId,
                    stravaUploadStatus = StravaUploadStatus.UPLOADED,
                    stravaActivityName = finalName
                )
            )
            StravaUploadOutcome.Success(actId, finalName)
        } else {
            val ex = uploadResult.exceptionOrNull()
            when (ex) {
                is StravaTokenExpiredException -> handleAuthError(session, finalName, ex)
                is StravaTransientException, is IOException, is StravaRateLimitException -> {
                    StravaUploadOutcome.RetryableFailure(
                        reason = ex.localizedMessage ?: "Koneksi terputus — akan dicoba ulang.",
                        activityName = finalName
                    )
                }
                else -> {
                    workoutRepository.saveSession(
                        session.copy(
                            stravaUploadStatus = StravaUploadStatus.FAILED,
                            stravaActivityName = finalName
                        )
                    )
                    StravaUploadOutcome.PermanentFailure(
                        reason = ex?.localizedMessage ?: "Upload ke Strava gagal.",
                        activityName = finalName
                    )
                }
            }
        }
    }

    suspend fun markSessionUploadFailed(sessionId: String) {
        val session = workoutRepository.getSessionById(sessionId) ?: return
        if (session.stravaUploadStatus != StravaUploadStatus.UPLOADED) {
            workoutRepository.saveSession(
                session.copy(stravaUploadStatus = StravaUploadStatus.FAILED)
            )
        }
    }

    /**
     * Mengimpor 30 aktivitas terbaru dari Strava ke Room DB & Cloud Firestore tanpa duplikasi (US-13).
     */
    suspend fun importRecentActivities(): Result<Int> {
        if (!stravaAuthManager.isConfigured()) {
            return Result.failure(IllegalStateException("STRAVA_CLIENT_ID / SECRET belum dikonfigurasi."))
        }

        var token = stravaAuthManager.getValidAccessToken().getOrElse { err ->
            if (err is StravaTokenExpiredException) authRepository.markStravaTokenExpired()
            return Result.failure(err)
        }

        var remoteResult = stravaApiClient.fetchRecentActivities(token, perPage = 30)
        if (remoteResult.isFailure && remoteResult.exceptionOrNull() is StravaTokenExpiredException) {
            val refreshed = stravaAuthManager.refreshAccessToken()
            if (refreshed.isSuccess) {
                token = refreshed.getOrThrow()
                remoteResult = stravaApiClient.fetchRecentActivities(token, perPage = 30)
            } else {
                authRepository.markStravaTokenExpired()
                return Result.failure(refreshed.exceptionOrNull()!!)
            }
        }

        if (remoteResult.isFailure) {
            val err = remoteResult.exceptionOrNull()!!
            if (err is StravaTokenExpiredException) authRepository.markStravaTokenExpired()
            return Result.failure(err)
        }

        val remoteActivities = remoteResult.getOrThrow()
        val existingSessions = workoutRepository.getAllSessions().first()
        val nonDuplicates = StravaFormatter.filterNonDuplicateImports(existingSessions, remoteActivities)

        for (imported in nonDuplicates) {
            workoutRepository.saveSession(imported)
        }
        return Result.success(nonDuplicates.size)
    }

    private suspend fun handleAuthError(
        session: com.stridebeat.app.domain.model.WorkoutSession,
        activityName: String,
        err: Throwable
    ): StravaUploadOutcome {
        return if (err is StravaTokenExpiredException) {
            authRepository.markStravaTokenExpired()
            workoutRepository.saveSession(
                session.copy(
                    stravaUploadStatus = StravaUploadStatus.FAILED,
                    stravaActivityName = activityName
                )
            )
            StravaUploadOutcome.PermanentFailure(
                reason = err.localizedMessage ?: "Sesi Strava kedaluwarsa. Login ulang di Pengaturan.",
                activityName = activityName,
                isTokenExpired = true
            )
        } else if (err is IOException) {
            StravaUploadOutcome.RetryableFailure(
                reason = err.localizedMessage ?: "Koneksi internet terputus.",
                activityName = activityName
            )
        } else {
            workoutRepository.saveSession(
                session.copy(
                    stravaUploadStatus = StravaUploadStatus.FAILED,
                    stravaActivityName = activityName
                )
            )
            StravaUploadOutcome.PermanentFailure(
                reason = err.localizedMessage ?: "Gagal terhubung ke Strava.",
                activityName = activityName
            )
        }
    }
}
