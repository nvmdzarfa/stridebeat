package com.stridebeat.app.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stridebeat.app.data.repository.StravaRepository
import com.stridebeat.app.data.repository.StravaUploadOutcome
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * WorkManager Worker untuk mengunggah sesi olahraga ke Strava di latar belakang (PRD §8.1).
 * Otomatis menunggu jaringan kembali jika offline dan mengirim notifikasi saat selesai/gagal.
 */
class StravaUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StravaWorkerEntryPoint {
        fun stravaRepository(): StravaRepository
    }

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val activityName = inputData.getString(KEY_ACTIVITY_NAME)
        val activityDesc = inputData.getString(KEY_ACTIVITY_DESCRIPTION)

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            StravaWorkerEntryPoint::class.java
        )
        val stravaRepository = entryPoint.stravaRepository()

        return when (val outcome = stravaRepository.executeUpload(sessionId, activityName, activityDesc)) {
            is StravaUploadOutcome.Success -> {
                StravaNotificationHelper.showUploadSuccess(
                    context = applicationContext,
                    activityName = outcome.activityName,
                    activityId = outcome.activityId
                )
                Result.success()
            }
            is StravaUploadOutcome.RetryableFailure -> {
                if (runAttemptCount >= MAX_ATTEMPTS) {
                    stravaRepository.markSessionUploadFailed(sessionId)
                    StravaNotificationHelper.showUploadFailed(
                        context = applicationContext,
                        activityName = outcome.activityName,
                        reason = outcome.reason
                    )
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
            is StravaUploadOutcome.PermanentFailure -> {
                StravaNotificationHelper.showUploadFailed(
                    context = applicationContext,
                    activityName = outcome.activityName,
                    reason = outcome.reason
                )
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_SESSION_ID = "session_id"
        const val KEY_ACTIVITY_NAME = "activity_name"
        const val KEY_ACTIVITY_DESCRIPTION = "activity_description"
        private const val MAX_ATTEMPTS = 3
    }
}
