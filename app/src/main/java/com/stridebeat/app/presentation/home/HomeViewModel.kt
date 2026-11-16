package com.stridebeat.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class WeeklyStats(
    val totalDistanceKm: Double = 0.0,
    val totalDurationMinutes: Int = 0,
    val sessionCount: Int = 0
)

data class HomeUiState(
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val lastSession: WorkoutSession? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = workoutRepository.getAllSessions()
        .map { allSessions ->
            val realSessions = allSessions.filterNot { it.id.startsWith("sample") }
            
            // Hitung statistik 7 hari terakhir (atau awal minggu Senin)
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfWeekMs = cal.timeInMillis

            val sessionsThisWeek = realSessions.filter { it.startedAt >= startOfWeekMs }
            val totalDistanceMeters = sessionsThisWeek.sumOf { it.distanceMeters }
            val totalDurationMs = sessionsThisWeek.sumOf { it.durationMs }

            val weekly = WeeklyStats(
                totalDistanceKm = totalDistanceMeters / 1000.0,
                totalDurationMinutes = (totalDurationMs / (60 * 1000L)).toInt(),
                sessionCount = sessionsThisWeek.size
            )

            val latest = realSessions.maxByOrNull { it.startedAt }

            HomeUiState(
                weeklyStats = weekly,
                lastSession = latest,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )
}
