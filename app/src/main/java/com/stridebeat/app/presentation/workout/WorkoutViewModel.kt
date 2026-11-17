package com.stridebeat.app.presentation.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.data.local.WorkoutPreferences
import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.domain.model.WorkoutTarget
import com.stridebeat.app.service.TrackingService
import com.stridebeat.app.service.TrackingStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutPreferences: WorkoutPreferences
) : ViewModel() {

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
    val workoutTarget: StateFlow<WorkoutTarget> = TrackingStateManager.workoutTarget

    private val _selectedTarget = MutableStateFlow(WorkoutTarget())
    val selectedTarget: StateFlow<WorkoutTarget> = _selectedTarget.asStateFlow()

    val voiceCueEnabled: StateFlow<Boolean> = workoutPreferences.voiceCueEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val voiceCueIntervalKm: StateFlow<Double> = workoutPreferences.voiceCueIntervalKm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val autoPauseEnabled: StateFlow<Boolean> = workoutPreferences.autoPauseEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isKmUnit: StateFlow<Boolean> = workoutPreferences.isKmUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            workoutPreferences.autoPauseEnabled.collect { enabled ->
                TrackingStateManager.autoPauseEnabled = enabled
            }
        }
    }

    fun selectTarget(target: WorkoutTarget) {
        _selectedTarget.value = target
    }

    fun setVoiceCueEnabled(enabled: Boolean) {
        viewModelScope.launch {
            workoutPreferences.setVoiceCueEnabled(enabled)
        }
    }

    fun setVoiceCueIntervalKm(interval: Double) {
        viewModelScope.launch {
            workoutPreferences.setVoiceCueIntervalKm(interval)
        }
    }

    fun setAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            workoutPreferences.setAutoPauseEnabled(enabled)
            TrackingStateManager.autoPauseEnabled = enabled
        }
    }

    fun setIsKmUnit(isKm: Boolean) {
        viewModelScope.launch {
            workoutPreferences.setIsKmUnit(isKm)
        }
    }

    fun startWorkout(
        context: Context,
        type: WorkoutType = WorkoutType.RUN,
        target: WorkoutTarget = _selectedTarget.value
    ) {
        TrackingService.start(context, type, target)
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
