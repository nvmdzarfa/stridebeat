package com.stridebeat.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Engine pemutar audio YouTube di latar belakang menggunakan YouTube IFrame Player API.
 * Memutar lagu secara FULL 100% dari awal sampai akhir tanpa batas 30 detik preview,
 * dan tanpa memerlukan akun YouTube Premium.
 */
class YouTubeBackgroundPlayer(
    private val context: Context,
    private val onStateChanged: (isPlaying: Boolean) -> Unit,
    private val onTrackEnded: () -> Unit,
    private val onError: (errorCode: Int) -> Unit
) {
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isPlayerReady = false
    private var pendingVideoId: String? = null

    init {
        mainHandler.post {
            initWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        try {
            if (webView != null) return
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {}

                addJavascriptInterface(AndroidJsBridge(), "AndroidBridge")

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>body { margin: 0; background: #000; overflow: hidden; }</style>
                    </head>
                    <body>
                        <div id="player"></div>
                        <script src="https://www.youtube.com/iframe_api"></script>
                        <script>
                            var player;
                            var isReady = false;
                            var pendingId = null;

                            function onYouTubeIframeAPIReady() {
                                player = new YT.Player('player', {
                                    height: '100%',
                                    width: '100%',
                                    playerVars: {
                                        'autoplay': 1,
                                        'controls': 0,
                                        'playsinline': 1,
                                        'rel': 0,
                                        'fs': 0,
                                        'origin': 'https://www.youtube.com'
                                    },
                                    events: {
                                        'onReady': onPlayerReady,
                                        'onStateChange': onPlayerStateChange,
                                        'onError': onPlayerError
                                    }
                                });
                            }

                            function onPlayerReady(event) {
                                isReady = true;
                                if (pendingId) {
                                    player.loadVideoById(pendingId);
                                    pendingId = null;
                                }
                                if (window.AndroidBridge) window.AndroidBridge.onReady();
                            }

                            function onPlayerStateChange(event) {
                                if (window.AndroidBridge) window.AndroidBridge.onStateChange(event.data);
                            }

                            function onPlayerError(event) {
                                if (window.AndroidBridge) window.AndroidBridge.onError(event.data);
                            }

                            function playVideo(id) {
                                if (isReady && player && player.loadVideoById) {
                                    player.loadVideoById(id);
                                } else {
                                    pendingId = id;
                                }
                            }

                            function resumeVideo() {
                                if (player && player.playVideo) player.playVideo();
                            }

                            function pauseVideo() {
                                if (player && player.pauseVideo) player.pauseVideo();
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play(videoId: String) {
        mainHandler.post {
            if (isPlayerReady && webView != null) {
                webView?.evaluateJavascript("playVideo('$videoId');", null)
            } else {
                pendingVideoId = videoId
                if (webView == null) {
                    initWebView()
                }
            }
        }
    }

    fun resume() {
        mainHandler.post {
            webView?.evaluateJavascript("resumeVideo();", null)
        }
    }

    fun pause() {
        mainHandler.post {
            webView?.evaluateJavascript("pauseVideo();", null)
        }
    }

    fun release() {
        mainHandler.post {
            try {
                webView?.stopLoading()
                webView?.destroy()
                webView = null
                isPlayerReady = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private inner class AndroidJsBridge {
        @JavascriptInterface
        fun onReady() {
            isPlayerReady = true
            pendingVideoId?.let { id ->
                pendingVideoId = null
                play(id)
            }
        }

        @JavascriptInterface
        fun onStateChange(state: Int) {
            // YT.PlayerState: -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (video cued)
            when (state) {
                1 -> onStateChanged(true)
                2 -> onStateChanged(false)
                0 -> {
                    onStateChanged(false)
                    onTrackEnded()
                }
            }
        }

        @JavascriptInterface
        fun onError(errorCode: Int) {
            onError(errorCode)
        }
    }
}
