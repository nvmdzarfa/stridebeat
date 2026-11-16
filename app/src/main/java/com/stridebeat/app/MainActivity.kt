package com.stridebeat.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridebeat.app.presentation.navigation.StrideBeatNavGraph
import com.stridebeat.app.presentation.settings.AuthViewModel
import com.stridebeat.app.presentation.theme.StrideBeatTheme
import com.stridebeat.app.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle OAuth Callback dari DeepLink jika app diluncurkan oleh browser
        intent?.data?.let { handleDeepLink(it) }

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            StrideBeatTheme(themeMode = themeMode) {
                StrideBeatNavGraph(themeViewModel = themeViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle OAuth Callback saat app sudah berjalan di background
        intent.data?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(uri: Uri) {
        if (uri.scheme == "stridebeat") {
            authViewModel.handleAuthCallback(uri)
        }
    }
}
