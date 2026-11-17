package com.stridebeat.app

import com.stridebeat.app.data.local.PlaylistStorage
import com.stridebeat.app.data.remote.youtubemusic.StreamResult
import com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicClient
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeMusicParsingTest {

    @Test
    fun testParseSearchResponseExtractsTracksCorrectly() {
        val sampleSearchJson = """
        {
          "contents": {
            "tabbedSearchResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "musicShelfRenderer": {
                              "title": { "runs": [ { "text": "Songs" } ] },
                              "contents": [
                                {
                                  "musicResponsiveListItemRenderer": {
                                    "flexColumns": [
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              {
                                                "text": "Blinding Lights",
                                                "navigationEndpoint": {
                                                  "watchEndpoint": {
                                                    "videoId": "4NRXx6U8ABQ"
                                                  }
                                                }
                                              }
                                            ]
                                          }
                                        }
                                      },
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              { "text": "The Weeknd" },
                                              { "text": " • " },
                                              { "text": "After Hours" },
                                              { "text": " • " },
                                              { "text": "3:20" }
                                            ]
                                          }
                                        }
                                      }
                                    ],
                                    "thumbnail": {
                                      "musicThumbnailRenderer": {
                                        "thumbnail": {
                                          "thumbnails": [
                                            { "url": "https://lh3.googleusercontent.com/test_cover=w120-h120" }
                                          ]
                                        }
                                      }
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val tracks = YouTubeMusicClient.parseSearchResponse(sampleSearchJson)
        assertEquals(1, tracks.size)
        val track = tracks[0]
        assertEquals("ytm_4NRXx6U8ABQ", track.id)
        assertEquals("Blinding Lights", track.title)
        assertEquals("The Weeknd", track.artist)
        assertEquals("After Hours", track.album)
        assertEquals(200000L, track.durationMs) // 3m 20s = 200,000 ms
        assertEquals("4NRXx6U8ABQ", track.platformIds[MusicSource.YOUTUBE_MUSIC])
    }

    @Test
    fun testParsePlayerAudioUrlReturnsPlayableDirectUrlWhenAvailable() {
        val samplePlayerJson = """
        {
          "playabilityStatus": {
            "status": "OK"
          },
          "streamingData": {
            "adaptiveFormats": [
              {
                "itag": 140,
                "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                "bitrate": 131072,
                "url": "https://rr1---sn-direct-stream.googlevideo.com/videoplayback?expire=123"
              }
            ]
          }
        }
        """.trimIndent()

        val result = YouTubeMusicClient.parsePlayerAudioUrl(samplePlayerJson, "4NRXx6U8ABQ")
        assertTrue(result is StreamResult.Playable)
        val playable = result as StreamResult.Playable
        assertEquals("https://rr1---sn-direct-stream.googlevideo.com/videoplayback?expire=123", playable.audioUrl)
    }

    @Test
    fun testParsePlayerAudioUrlReturnsUnavailableWhenOnlySignatureCipherPresent() {
        val sampleCipherJson = """
        {
          "playabilityStatus": {
            "status": "OK"
          },
          "streamingData": {
            "adaptiveFormats": [
              {
                "itag": 140,
                "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                "signatureCipher": "s=abcde&sp=sig&url=https://googlevideo.com/..."
              }
            ]
          }
        }
        """.trimIndent()

        val result = YouTubeMusicClient.parsePlayerAudioUrl(sampleCipherJson, "cipher_vid")
        assertTrue(result is StreamResult.Unavailable)
        val unavailable = result as StreamResult.Unavailable
        assertEquals("https://music.youtube.com/watch?v=cipher_vid", unavailable.externalUri)
    }

    @Test
    fun testParsePlayerAudioUrlReturnsUnavailableWhenPlayabilityStatusNotOk() {
        val sampleBlockedJson = """
        {
          "playabilityStatus": {
            "status": "UNPLAYABLE",
            "reason": "This video is not available."
          }
        }
        """.trimIndent()

        val result = YouTubeMusicClient.parsePlayerAudioUrl(sampleBlockedJson, "blocked_vid")
        assertTrue(result is StreamResult.Unavailable)
        val unavailable = result as StreamResult.Unavailable
        assertTrue(unavailable.reason.contains("tidak tersedia"))
    }

    @Test
    fun testParsePlayerAudioUrlHandlesMalformedJsonGracefullyWithoutThrowing() {
        val malformed = "{ invalid json"
        val result = YouTubeMusicClient.parsePlayerAudioUrl(malformed, "broken_id")
        assertTrue(result is StreamResult.Unavailable)
    }

    @Test
    fun testExtractPlaylistIdFromVariousUrlFormats() {
        val id = "PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"

        // 1. YouTube Music URL
        val ytmUrl = "https://music.youtube.com/playlist?list=$id"
        assertEquals(id, YouTubeMusicClient.extractPlaylistId(ytmUrl))

        // 2. YouTube Desktop URL
        val ytUrl = "https://www.youtube.com/playlist?list=$id"
        assertEquals(id, YouTubeMusicClient.extractPlaylistId(ytUrl))

        // 3. Watch URL with list param
        val watchUrl = "https://www.youtube.com/watch?v=kPa7bsKwL-c&list=$id"
        assertEquals(id, YouTubeMusicClient.extractPlaylistId(watchUrl))

        // 4. Raw playlist ID
        assertEquals(id, YouTubeMusicClient.extractPlaylistId(id))

        // 5. Quoted raw ID
        assertEquals(id, YouTubeMusicClient.extractPlaylistId("\"$id\""))

        // 6. Invalid URL or non-playlist URL
        assertNull(YouTubeMusicClient.extractPlaylistId("https://youtube.com/"))
        assertNull(YouTubeMusicClient.extractPlaylistId(""))
        assertNull(YouTubeMusicClient.extractPlaylistId("short"))
    }

    @Test
    fun testParsePlaylistBrowseResponseWithLockupViewModel() {
        val sampleBrowseJson = """
        {
          "metadata": {
            "playlistMetadataRenderer": {
              "title": "Workout Running Pop Hits"
            }
          },
          "contents": {
            "twoColumnBrowseResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "itemSectionRenderer": {
                              "contents": [
                                {
                                  "lockupViewModel": {
                                    "contentId": "kPa7bsKwL-c",
                                    "metadata": {
                                      "lockupMetadataViewModel": {
                                        "title": { "content": "Die With A Smile" },
                                        "metadata": {
                                          "contentMetadataViewModel": {
                                            "metadataRows": [
                                              {
                                                "metadataParts": [
                                                  { "text": { "content": "Lady Gaga & Bruno Mars" } }
                                                ]
                                              }
                                            ]
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val playlist = YouTubeMusicClient.parsePlaylistBrowseResponse(sampleBrowseJson, "PLTest123")
        assertNotNull(playlist)
        assertEquals("ytpl_PLTest123", playlist?.id)
        assertEquals("Workout Running Pop Hits", playlist?.name)
        assertEquals(1, playlist?.tracks?.size)

        val track = playlist?.tracks?.get(0)
        assertEquals("ytm_kPa7bsKwL-c", track?.id)
        assertEquals("Die With A Smile", track?.title)
        assertEquals("Lady Gaga & Bruno Mars", track?.artist)
        assertEquals("https://i.ytimg.com/vi/kPa7bsKwL-c/hqdefault.jpg", track?.coverUrl)
        assertEquals("kPa7bsKwL-c", track?.platformIds?.get(MusicSource.YOUTUBE_MUSIC))
    }

    @Test
    fun testParsePlaylistBrowseResponseWithPlaylistVideoRenderer() {
        val sampleClassicJson = """
        {
          "header": {
            "playlistHeaderRenderer": {
              "title": { "runs": [ { "text": "Classic Workout Beats" } ] }
            }
          },
          "contents": {
            "twoColumnBrowseResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "playlistVideoListRenderer": {
                              "contents": [
                                {
                                  "playlistVideoRenderer": {
                                    "videoId": "abc123xyz00",
                                    "title": { "runs": [ { "text": "Midnight Runner" } ] },
                                    "shortBylineText": { "runs": [ { "text": "Electronic Artist" } ] },
                                    "lengthSeconds": "240",
                                    "thumbnail": {
                                      "thumbnails": [
                                        { "url": "https://i.ytimg.com/vi/abc123xyz00/hqdefault.jpg" }
                                      ]
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val playlist = YouTubeMusicClient.parsePlaylistBrowseResponse(sampleClassicJson, "PLClassic456")
        assertNotNull(playlist)
        assertEquals("ytpl_PLClassic456", playlist?.id)
        assertEquals("Classic Workout Beats", playlist?.name)
        assertEquals(1, playlist?.tracks?.size)

        val track = playlist?.tracks?.get(0)
        assertEquals("ytm_abc123xyz00", track?.id)
        assertEquals("Midnight Runner", track?.title)
        assertEquals("Electronic Artist", track?.artist)
        assertEquals(240000L, track?.durationMs)
    }

    @Test
    fun testExtractAlertMessageWhenPlaylistNotFound() {
        val sampleAlertJson = """
        {
          "alerts": [
            {
              "alertRenderer": {
                "type": "ERROR",
                "text": {
                  "runs": [
                    { "text": "Playlist tidak ada." }
                  ]
                }
              }
            }
          ]
        }
        """.trimIndent()

        val alert = YouTubeMusicClient.extractAlertMessage(sampleAlertJson)
        assertEquals("Playlist tidak ada.", alert)
    }

    @Test
    fun testPlaylistStorageSerializationRoundTrip() {
        val original = listOf(
            WorkoutPlaylist(
                id = "ytpl_test",
                name = "Morning 150 BPM",
                description = "2 lagu diimpor dari YouTube",
                source = MusicSource.YOUTUBE_MUSIC,
                coverUrl = "https://i.ytimg.com/vi/vid1/hqdefault.jpg",
                tracks = listOf(
                    Track(
                        id = "ytm_vid1",
                        isrc = null,
                        title = "Song One",
                        artist = "Artist One",
                        album = "Album One",
                        durationMs = 210000L,
                        coverUrl = "https://i.ytimg.com/vi/vid1/hqdefault.jpg",
                        audioUrl = null,
                        platformIds = mapOf(MusicSource.YOUTUBE_MUSIC to "vid1")
                    ),
                    Track(
                        id = "ytm_vid2",
                        isrc = null,
                        title = "Song Two",
                        artist = "Artist Two",
                        album = "Album Two",
                        durationMs = 180000L,
                        coverUrl = "https://i.ytimg.com/vi/vid2/hqdefault.jpg",
                        audioUrl = null,
                        platformIds = mapOf(MusicSource.YOUTUBE_MUSIC to "vid2")
                    )
                )
            )
        )

        val serialized = PlaylistStorage.serializePlaylists(original)
        assertTrue(serialized.contains("Morning 150 BPM"))
        assertTrue(serialized.contains("Song One"))

        val deserialized = PlaylistStorage.deserializePlaylists(serialized)
        assertEquals(1, deserialized.size)
        val pl = deserialized[0]
        assertEquals("ytpl_test", pl.id)
        assertEquals("Morning 150 BPM", pl.name)
        assertEquals(2, pl.tracks.size)
        assertEquals("Song One", pl.tracks[0].title)
        assertEquals("Song Two", pl.tracks[1].title)
        assertEquals("vid1", pl.tracks[0].platformIds[MusicSource.YOUTUBE_MUSIC])
    }
}
