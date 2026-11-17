package com.stridebeat.app.service

import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.util.TrackingUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manajer status pelacakan GPS dan durasi olahraga.
 * Berperan sebagai jembatan reaktif antara Foreground TrackingService dan UI Compose.
 */
object TrackingStateManager {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isAutoPaused = MutableStateFlow(false)
    val isAutoPaused: StateFlow<Boolean> = _isAutoPaused.asStateFlow()

    private val _elapsedDurationMs = MutableStateFlow(0L)
    val elapsedDurationMs: StateFlow<Long> = _elapsedDurationMs.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _currentPaceMinPerKm = MutableStateFlow<Double?>(null)
    val currentPaceMinPerKm: StateFlow<Double?> = _currentPaceMinPerKm.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0.0)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private val _caloriesKcal = MutableStateFlow(0.0)
    val caloriesKcal: StateFlow<Double> = _caloriesKcal.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints.asStateFlow()

    private val _gpsInterrupted = MutableStateFlow(false)
    val gpsInterrupted: StateFlow<Boolean> = _gpsInterrupted.asStateFlow()

    private val _workoutTarget = MutableStateFlow(com.stridebeat.app.domain.model.WorkoutTarget())
    val workoutTarget: StateFlow<com.stridebeat.app.domain.model.WorkoutTarget> = _workoutTarget.asStateFlow()

    var workoutType: WorkoutType = WorkoutType.RUN
    var autoPauseEnabled: Boolean = true

    private var sessionStartTime: Long = 0L
    private var lastRecordedLocation: LatLng? = null
    private var lastMovementTimestamp: Long = 0L

    fun startSession(type: WorkoutType = WorkoutType.RUN, target: com.stridebeat.app.domain.model.WorkoutTarget = com.stridebeat.app.domain.model.WorkoutTarget()) {
        workoutType = type
        sessionStartTime = System.currentTimeMillis()
        lastMovementTimestamp = sessionStartTime
        lastRecordedLocation = null

        _isTracking.value = true
        _isPaused.value = false
        _isAutoPaused.value = false
        _elapsedDurationMs.value = 0L
        _distanceMeters.value = 0.0
        _currentPaceMinPerKm.value = null
        _currentSpeedKmh.value = 0.0
        _caloriesKcal.value = 0.0
        _routePoints.value = emptyList()
        _gpsInterrupted.value = false
        _workoutTarget.value = target
    }

    fun pauseSession(manual: Boolean = true) {
        if (!_isTracking.value) return
        if (manual) {
            _isPaused.value = true
            _isAutoPaused.value = false
        } else {
            _isAutoPaused.value = true
        }
    }

    fun resumeSession() {
        if (!_isTracking.value) return
        _isPaused.value = false
        _isAutoPaused.value = false
        lastMovementTimestamp = System.currentTimeMillis()
    }

    fun addDurationTick(deltaMs: Long) {
        if (_isTracking.value && !_isPaused.value && !_isAutoPaused.value) {
            val newDuration = _elapsedDurationMs.value + deltaMs
            _elapsedDurationMs.value = newDuration

            // Update pace & kalori setiap detik
            _currentPaceMinPerKm.value = TrackingUtils.calculatePaceMinPerKm(newDuration, _distanceMeters.value)
            _caloriesKcal.value = TrackingUtils.estimateCalories(newDuration, _distanceMeters.value)
        }
    }

    fun onNewLocation(lat: Double, lng: Double, speedMetersPerSec: Float, accuracyMeters: Float) {
        if (!_isTracking.value || _isPaused.value) return

        // Filter lokasi yang tidak akurat (misal akurasi lebih buruk dari 25 meter)
        if (accuracyMeters > 25.0f && accuracyMeters > 0) {
            _gpsInterrupted.value = true
            return
        }
        _gpsInterrupted.value = false

        val newPoint = LatLng(lat, lng)
        val lastPoint = lastRecordedLocation

        if (lastPoint != null) {
            val distanceDelta = TrackingUtils.calculateHaversineDistance(lastPoint, newPoint)

            // Minimal pergerakan 1.5 meter untuk menghindari GPS drift saat diam
            if (distanceDelta >= 1.5) {
                _distanceMeters.value += distanceDelta
                _routePoints.value = _routePoints.value + newPoint
                lastRecordedLocation = newPoint
                lastMovementTimestamp = System.currentTimeMillis()

                if (_isAutoPaused.value) {
                    _isAutoPaused.value = false
                }
            } else if (autoPauseEnabled && speedMetersPerSec < 0.3f) {
                // Auto pause jika tidak bergerak selama lebih dari 6 detik
                if (System.currentTimeMillis() - lastMovementTimestamp > 6000 && !_isAutoPaused.value) {
                    _isAutoPaused.value = true
                }
            }
        } else {
            lastRecordedLocation = newPoint
            _routePoints.value = listOf(newPoint)
            lastMovementTimestamp = System.currentTimeMillis()
        }

        // Kecepatan dalam km/h
        _currentSpeedKmh.value = (speedMetersPerSec * 3.6).toDouble()
    }

    fun finishSession(): WorkoutSession {
        val endedAt = System.currentTimeMillis()
        val duration = _elapsedDurationMs.value
        val distance = _distanceMeters.value
        val pace = TrackingUtils.calculatePaceMinPerKm(duration, distance)
        val calories = _caloriesKcal.value
        val points = _routePoints.value
        val interrupted = _gpsInterrupted.value

        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            type = workoutType,
            startedAt = sessionStartTime,
            endedAt = endedAt,
            distanceMeters = distance,
            durationMs = duration,
            avgPaceMinPerKm = pace,
            caloriesKcal = calories,
            routePoints = points,
            musicSource = null,
            playedTracks = emptyList(),
            stravaActivityId = null,
            gpsInterrupted = interrupted
        )

        _isTracking.value = false
        _isPaused.value = false
        _isAutoPaused.value = false

        return session
    }
}
