package com.stridebeat.app

import com.stridebeat.app.data.remote.youtubemusic.StreamResult
import com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicClient
import com.stridebeat.app.domain.model.MusicSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
