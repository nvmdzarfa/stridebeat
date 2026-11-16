package com.stridebeat.app.data.remote.music

import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist

/**
 * Daftar Playlist Olahraga Netral Platform bawaan StrideBeat (M6).
 * Lagu di dalam playlist netral tidak terikat secara kaku pada satu URL audio platform,
 * melainkan memiliki ISRC serta judul/artis standar sehingga dapat dicocokkan & diputar
 * baik di SoundCloud maupun YouTube Music (Beta) via TrackMatchingEngine.
 */
object NeutralWorkoutPlaylists {

    val playlists: List<WorkoutPlaylist> = listOf(
        WorkoutPlaylist(
            id = "pl_morning_run",
            name = "Lari Pagi 140+ BPM",
            description = "Beat energik stabil untuk menjaga ritme cadence lari santai di pagi hari.",
            source = null,
            coverUrl = "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=500&auto=format&fit=crop&q=60",
            tracks = listOf(
                Track(
                    id = "sb_tr_01",
                    isrc = "USUM71703861",
                    title = "Midnight City",
                    artist = "M83",
                    album = "Hurry Up, We're Dreaming",
                    durationMs = 243000L
                ),
                Track(
                    id = "sb_tr_02",
                    isrc = "USUG12000658",
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    album = "After Hours",
                    durationMs = 200000L
                ),
                Track(
                    id = "sb_tr_03",
                    isrc = "USAT21301319",
                    title = "Run Boy Run",
                    artist = "Woodkid",
                    album = "The Golden Age",
                    durationMs = 213000L
                ),
                Track(
                    id = "sb_tr_04",
                    isrc = "USUM71304229",
                    title = "Wake Me Up",
                    artist = "Avicii",
                    album = "True",
                    durationMs = 247000L
                )
            )
        ),
        WorkoutPlaylist(
            id = "pl_tempo_boost",
            name = "Tempo Boost",
            description = "Irama bertempo tinggi untuk mendongkrak pace, interval training, dan target PR.",
            source = null,
            coverUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500&auto=format&fit=crop&q=60",
            tracks = listOf(
                Track(
                    id = "sb_tr_05",
                    isrc = "USRC10700540",
                    title = "Stronger",
                    artist = "Kanye West",
                    album = "Graduation",
                    durationMs = 311000L
                ),
                Track(
                    id = "sb_tr_06",
                    isrc = "USUM71607007",
                    title = "Starboy",
                    artist = "The Weeknd",
                    album = "Starboy",
                    durationMs = 230000L
                ),
                Track(
                    id = "sb_tr_07",
                    isrc = "GBARL1800805",
                    title = "Electricity",
                    artist = "Silk City, Dua Lipa",
                    album = "Electricity",
                    durationMs = 238000L
                ),
                Track(
                    id = "sb_tr_08",
                    isrc = "USUM72105123",
                    title = "Industry Baby",
                    artist = "Lil Nas X, Jack Harlow",
                    album = "Montero",
                    durationMs = 212000L
                )
            )
        ),
        WorkoutPlaylist(
            id = "pl_warmup_cooldown",
            name = "Warmup & Cooldown",
            description = "Melodi sejuk untuk peregangan otot, fokus pernapasan, dan pemulihan detak jantung.",
            source = null,
            coverUrl = "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=500&auto=format&fit=crop&q=60",
            tracks = listOf(
                Track(
                    id = "sb_tr_09",
                    isrc = "GBAYE1000781",
                    title = "Intro",
                    artist = "The xx",
                    album = "xx",
                    durationMs = 127000L
                ),
                Track(
                    id = "sb_tr_10",
                    isrc = "GBAYE0200010",
                    title = "Dayvan Cowboy",
                    artist = "Boards of Canada",
                    album = "The Campfire Headphase",
                    durationMs = 300000L
                ),
                Track(
                    id = "sb_tr_11",
                    isrc = "USCJ82000490",
                    title = "Resonance",
                    artist = "HOME",
                    album = "Odyssey",
                    durationMs = 212000L
                )
            )
        )
    )
}
