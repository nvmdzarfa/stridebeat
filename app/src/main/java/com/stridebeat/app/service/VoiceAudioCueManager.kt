package com.stridebeat.app.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.stridebeat.app.data.local.WorkoutPreferences
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.util.VoiceCueFormatter
import kotlinx.coroutines.*
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manajer Voice Audio Cues berbasis Android TextToSpeech (TTS).
 * Menyuarakan panduan lari (KM split, durasi, pace, status workout) dengan dukungan:
 * - Bahasa Indonesia otomatis (fallback ke default device jika bahasa Indonesia belum terpasang).
 * - Audio Ducking: Otomatis mengecilkan volume ExoPlayer saat suara berbicara, dan mengembalikannya saat selesai.
 * - Safety Timeout: Menjamin volume musik selalu kembali normal meski engine TTS perangkat hang.
 */
class VoiceAudioCueManager(
    private val context: Context,
    private val workoutPreferences: WorkoutPreferences
) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val pendingMessages = mutableListOf<String>()

    private val isDucked = AtomicBoolean(false)
    private var duckingTimeoutJob: Job? = null

    @Volatile
    var isEnabled: Boolean = true
        private set

    init {
        // Observasi status preferensi panduan suara secara reaktif
        scope.launch {
            workoutPreferences.voiceCueEnabled.collect { enabled ->
                isEnabled = enabled
            }
        }

        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val indoLocale = Locale("id", "ID")
                val langResult = engine.setLanguage(indoLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.getDefault())
                }
                engine.setPitch(1.0f)
                engine.setSpeechRate(1.02f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        applyAudioDucking(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        applyAudioDucking(false)
                    }

                    override fun onError(utteranceId: String?) {
                        applyAudioDucking(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        applyAudioDucking(false)
                    }
                })

                isTtsInitialized = true

                // Ucapkan antrean pesan yang tertunda saat TTS sedang inisialisasi
                synchronized(pendingMessages) {
                    for (msg in pendingMessages) {
                        speakInternal(msg, flush = false)
                    }
                    pendingMessages.clear()
                }
            }
        }
    }

    private fun applyAudioDucking(duck: Boolean) {
        scope.launch {
            if (duck) {
                if (isDucked.compareAndSet(false, true)) {
                    MusicPlaybackService.setVolumeDucking(context, true)
                }
                // Safety timer: maksimal 10 detik volume mengecil agar musik tidak tertahan kecil jika TTS error
                duckingTimeoutJob?.cancel()
                duckingTimeoutJob = scope.launch {
                    delay(10000L)
                    if (isDucked.compareAndSet(true, false)) {
                        MusicPlaybackService.setVolumeDucking(context, false)
                    }
                }
            } else {
                duckingTimeoutJob?.cancel()
                if (isDucked.compareAndSet(true, false)) {
                    MusicPlaybackService.setVolumeDucking(context, false)
                }
            }
        }
    }

    fun speak(text: String, flush: Boolean = false) {
        if (!isEnabled || text.isBlank()) return

        if (!isTtsInitialized) {
            synchronized(pendingMessages) {
                if (flush) pendingMessages.clear()
                pendingMessages.add(text)
            }
            return
        }

        speakInternal(text, flush)
    }

    private fun speakInternal(text: String, flush: Boolean) {
        val utteranceId = "stridebeat_cue_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, params, utteranceId)
    }

    fun speakWorkoutStarted(workoutType: WorkoutType) {
        speak(VoiceCueFormatter.formatWorkoutStarted(workoutType), flush = true)
    }

    fun speakWorkoutPaused() {
        speak(VoiceCueFormatter.formatWorkoutPaused(), flush = true)
    }

    fun speakWorkoutResumed() {
        speak(VoiceCueFormatter.formatWorkoutResumed(), flush = true)
    }

    fun speakWorkoutCompleted(distanceMeters: Double, durationMs: Long) {
        speak(VoiceCueFormatter.formatWorkoutCompleted(distanceMeters, durationMs), flush = true)
    }

    fun speakKmSplit(km: Int, splitDurationMs: Long, averagePaceMinPerKm: Double?) {
        speak(VoiceCueFormatter.formatKmSplit(km, splitDurationMs, averagePaceMinPerKm), flush = false)
    }

    fun shutdown() {
        duckingTimeoutJob?.cancel()
        applyAudioDucking(false)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tts = null
        isTtsInitialized = false
        scope.cancel()
    }
}
