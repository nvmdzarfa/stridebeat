package com.stridebeat.app.presentation.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.service.TrackingService
import com.stridebeat.app.service.TrackingStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor() : ViewModel() {

    val isTracking: StateFlow<Boolean> = TrackingStateManager.isTracking
    val isPaused: StateFlow<Boolean> = TrackingStateManager.isPaused
    val isAutoPaused: StateFlow<Boolean> = TrackingStateManager.isAutoPaused
    val elapsedDurationMs: StateFlow<Long> = TrackingStateManager.elapsedDurationMs
    val distanceMeters: StateFlow<Double> = TrackingStateManager.distanceMeters
    val currentPaceMinPerKm: StateFlow<Double?> = TrackingStateManager.currentPaceMinPerKm
    val currentSpeedKmh: StateFlow<Double> = TrackingStateManager.currentSpeedKmh
    val caloriesKcal: StateFlow<Double> = TrackingStateManager.caloriesKcal
    val routePoints: StateFlow<List<LatLng>> = TrackingStateManager.routePoints
    val gpsInterrupted: StateFlow<Boolean> = TrackingStateManager.gpsInterrupted

    fun startWorkout(context: Context, type: WorkoutType = WorkoutType.RUN) {
        TrackingService.start(context, type)
    }

    fun pauseWorkout(context: Context) {
        TrackingService.pause(context)
    }

    fun resumeWorkout(context: Context) {
        TrackingService.resume(context)
    }

    fun finishWorkout(context: Context): WorkoutSession {
        TrackingService.stop(context)
        return TrackingStateManager.finishSession()
    }
}
