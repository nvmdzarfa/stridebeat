package com.stridebeat.app.data.repository

import android.content.Context
import com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicConfig
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import com.stridebeat.app.domain.repository.MusicRepository
import com.stridebeat.app.service.MusicPlaybackService
import com.stridebeat.app.util.TrackMatchingEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MusicRepository {

    private val _activeSource = MutableStateFlow(MusicSource.SPOTIFY)
    override val activeSource: StateFlow<MusicSource> = _activeSource.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    override val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _availablePlaylists = MutableStateFlow<List<WorkoutPlaylist>>(emptyList())
    override val availablePlaylists: StateFlow<List<WorkoutPlaylist>> = _availablePlaylists.asStateFlow()

    private val _activePlaylist = MutableStateFlow<WorkoutPlaylist?>(null)
    override val activePlaylist: StateFlow<WorkoutPlaylist?> = _activePlaylist.asStateFlow()

    private val _playedTracksInSession = MutableStateFlow<List<PlayedTrack>>(emptyList())
    override val playedTracksInSession: StateFlow<List<PlayedTrack>> = _playedTracksInSession.asStateFlow()

    private var currentTrackIndex = 0

    init {
        initializeCuratedPlaylists()
    }

    private fun initializeCuratedPlaylists() {
        val runningTracks = listOf(
            Track(
                id = "track_run_1",
                isrc = "USUM71703861",
                title = "Midnight City",
                artist = "M83",
                album = "Hurry Up, We're Dreaming",
                durationMs = 244000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/74/a2/d3/74a2d361-c6bb-9880-a8d1-d999833d2891/mzaf_15783493292556356203.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:1DoBx4h69vQ62jJvWbZ5j5",
                    MusicSource.YOUTUBE_MUSIC to "ytm_midnight_city"
                )
            ),
            Track(
                id = "track_run_2",
                isrc = "USUM71607172",
                title = "Starboy (feat. Daft Punk)",
                artist = "The Weeknd",
                album = "Starboy",
                durationMs = 230000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/11/71/d6/1171d6ad-3c96-e027-2af6-58028426588c/mzaf_15137631797407745471.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:7MXVkk9YM5FZ0wWACBRio2",
                    MusicSource.YOUTUBE_MUSIC to "ytm_starboy"
                )
            ),
            Track(
                id = "track_run_3",
                isrc = "FR9W11200123",
                title = "Run Boy Run",
                artist = "Woodkid",
                album = "The Golden Age",
                durationMs = 213000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/04/d2/94/04d294be-6311-baea-7217-99f0b4b7b3d4/mzaf_9016561572988079797.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:0Bo5cQoU6hLpZ5a3c2i1O4",
                    MusicSource.YOUTUBE_MUSIC to "ytm_run_boy_run"
                )
            ),
            Track(
                id = "track_run_4",
                isrc = "USUM70738491",
                title = "Stronger",
                artist = "Kanye West",
                album = "Graduation",
                durationMs = 311000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/9e/cc/69/9ecc6918-a8dc-354f-909f-ccc20a0a7a33/mzaf_7863921970418240507.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:4fzsfWzRhPawzqhXZwn925",
                    MusicSource.YOUTUBE_MUSIC to "ytm_stronger"
                )
            ),
            Track(
                id = "track_run_5",
                isrc = "GBARL1300054",
                title = "Pompeii",
                artist = "Bastille",
                album = "Bad Blood",
                durationMs = 214000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/b3/3d/fb/b33dfbb6-dac3-b97c-b239-18cf3688fdb7/mzaf_8177734646947686437.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:3LmpQiJ12G98cu624FNoTJ",
                    MusicSource.YOUTUBE_MUSIC to "ytm_pompeii"
                )
            ),
            Track(
                id = "track_run_6",
                isrc = "SEUM71101032",
                title = "Levels",
                artist = "Avicii",
                album = "Levels - Single",
                durationMs = 195000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/66/dc/bd/66dcbd68-b557-bdfd-f2c2-a05491758783/mzaf_13654489760720011690.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:levels_avicii",
                    MusicSource.YOUTUBE_MUSIC to "ytm_levels_avicii"
                )
            )
        )

        val cardioTracks = listOf(
            Track(
                id = "track_cardio_1",
                isrc = "GBAYE0601477",
                title = "Can't Stop",
                artist = "Red Hot Chili Peppers",
                album = "By the Way",
                durationMs = 269000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/b4/37/77/b43777c1-aa65-2d44-6130-7560392981af/mzaf_10231906724003007738.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:3ZOEytgrvLwQaqXreDs2Jx",
                    MusicSource.YOUTUBE_MUSIC to "ytm_cant_stop"
                )
            ),
            Track(
                id = "track_cardio_2",
                isrc = "GBUM71029605",
                title = "Lights",
                artist = "Ellie Goulding",
                album = "Bright Lights",
                durationMs = 244000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/eb/ca/b8/ebcab8fd-433b-93a6-4fe5-a9cb434d97a9/mzaf_13395952697862141833.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:11ozH05lM8vT4vB2B1wN3e",
                    MusicSource.YOUTUBE_MUSIC to "ytm_lights"
                )
            ),
            Track(
                id = "track_cardio_3",
                isrc = "FRZ030000001",
                title = "One More Time",
                artist = "Daft Punk",
                album = "Discovery",
                durationMs = 320000L,
                coverUrl = null,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/5d/93/d8/5d93d83f-ad1e-da4d-1d79-9937bdff24ec/mzaf_14396932211949300852.plus.aac.p.m4a",
                platformIds = mapOf(
                    MusicSource.SPOTIFY to "spotify:track:0DiWol3AO6WpXZgp0goxAV",
                    MusicSource.YOUTUBE_MUSIC to "ytm_one_more_time"
                )
            )
        )

        val playlists = listOf(
            WorkoutPlaylist(
                id = "pl_running_1",
                name = "StrideBeat Running High BPM (160+)",
                description = "Ritme stabil untuk menjaga pace lari 5'30\" - 6'30\"",
                source = MusicSource.SPOTIFY,
                tracks = runningTracks
            ),
            WorkoutPlaylist(
                id = "pl_cardio_2",
                name = "Power Cardio Beats & Phonk",
                description = "Dentuman bass bertenaga untuk latihan interval dan tanjakan",
                source = MusicSource.YOUTUBE_MUSIC,
                tracks = cardioTracks
            )
        )

        _availablePlaylists.value = playlists
        val defaultPlaylist = playlists.first()
        _activePlaylist.value = defaultPlaylist
        if (defaultPlaylist.tracks.isNotEmpty()) {
            currentTrackIndex = 0
            _currentTrack.value = defaultPlaylist.tracks.first()
            _playbackPositionMs.value = 0L
        }
    }

    override suspend fun play() {
        _isPlaying.value = true
        _currentTrack.value?.let { track ->
            recordPlayedTrack(track)
            MusicPlaybackService.update(context, track, true, _activeSource.value)
        }
    }

    override suspend fun pause() {
        _isPlaying.value = false
        _currentTrack.value?.let { track ->
            MusicPlaybackService.update(context, track, false, _activeSource.value)
        }
    }

    override suspend fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    override suspend fun skipToNext() {
        val playlist = _activePlaylist.value ?: return
        if (playlist.tracks.isEmpty()) return

        currentTrackIndex = (currentTrackIndex + 1) % playlist.tracks.size
        val nextTrack = playlist.tracks[currentTrackIndex]

        // Periksa ketersediaan lagu di platform aktif
        if (!TrackMatchingEngine.hasPlatformSupport(nextTrack, _activeSource.value)) {
            // Lewati jika lagu tidak tersedia di platform ini (PRD §6.4)
            currentTrackIndex = (currentTrackIndex + 1) % playlist.tracks.size
        }

        val trackToPlay = playlist.tracks[currentTrackIndex]
        _currentTrack.value = trackToPlay
        _playbackPositionMs.value = 0L

        if (_isPlaying.value) {
            recordPlayedTrack(trackToPlay)
            MusicPlaybackService.update(context, trackToPlay, true, _activeSource.value)
        }
    }

    override suspend fun skipToPrevious() {
        val playlist = _activePlaylist.value ?: return
        if (playlist.tracks.isEmpty()) return

        currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else playlist.tracks.size - 1
        val prevTrack = playlist.tracks[currentTrackIndex]

        _currentTrack.value = prevTrack
        _playbackPositionMs.value = 0L

        if (_isPlaying.value) {
            recordPlayedTrack(prevTrack)
            MusicPlaybackService.update(context, prevTrack, true, _activeSource.value)
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        _playbackPositionMs.value = positionMs
    }

    /**
     * Beralih sumber musik di tengah sesi (US-02).
     * Sesuai PRD §7.5: Ganti sumber TIDAK MERESET timer/jarak tracking GPS.
     */
    override suspend fun switchSource(newSource: MusicSource): Boolean {
        if (newSource == MusicSource.YOUTUBE_MUSIC && !YouTubeMusicConfig.isYouTubeMusicEnabled) {
            return false
        }

        val oldSource = _activeSource.value
        if (oldSource == newSource) return true

        _activeSource.value = newSource

        // Lakukan pencocokan lagu saat beralih platform
        val current = _currentTrack.value
        if (current != null) {
            val supported = TrackMatchingEngine.hasPlatformSupport(current, newSource)
            if (!supported) {
                // Cari lagu pengganti di platform baru
                skipToNext()
            } else {
                recordPlayedTrack(current)
                if (_isPlaying.value) {
                    MusicPlaybackService.update(context, current, true, newSource)
                }
            }
        }

        return true
    }

    override suspend fun selectPlaylist(playlist: WorkoutPlaylist) {
        _activePlaylist.value = playlist
        if (playlist.tracks.isNotEmpty()) {
            currentTrackIndex = 0
            _currentTrack.value = playlist.tracks.first()
            _playbackPositionMs.value = 0L
        }
    }

    override suspend fun playTrack(track: Track) {
        val playlist = _activePlaylist.value
        if (playlist != null) {
            val index = playlist.tracks.indexOfFirst { it.id == track.id }
            if (index != -1) {
                currentTrackIndex = index
            }
        }
        _currentTrack.value = track
        _playbackPositionMs.value = 0L
        play()
    }

    private fun recordPlayedTrack(track: Track) {
        val played = PlayedTrack(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            source = _activeSource.value,
            playedAt = System.currentTimeMillis()
        )

        // Hindari duplikasi jika lagu yang sama dicatat berturut-turut
        val currentList = _playedTracksInSession.value
        if (currentList.isEmpty() || currentList.last().trackId != track.id) {
            _playedTracksInSession.value = currentList + played
        }
    }

    override suspend fun searchOnlineTracks(query: String): List<Track> {
        return com.stridebeat.app.data.remote.music.OnlineMusicSearchEngine.searchTracks(query)
    }

    override suspend fun clearSessionTracks() {
        _playedTracksInSession.value = emptyList()
    }
}
