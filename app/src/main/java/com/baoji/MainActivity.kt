package com.baoji

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.BaojiTheme
import com.baoji.data.prefs.SettingsStore
import com.baoji.data.prefs.ThemeMode
import com.baoji.feature.shell.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // null until DataStore answers (a few ms); drawing nothing avoids a flash of the wrong theme.
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = null)
            val chosen = mode ?: return@setContent
            val dark = when (chosen) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SystemBarIcons(dark)
            BaojiTheme(dark = dark) {
                AppRoot()
            }
        }
    }

    /** Status/navigation bar icons follow the app's theme, not the system's. */
    @Composable
    private fun SystemBarIcons(dark: Boolean) {
        DisposableEffect(dark) {
            val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            onDispose { }
        }
    }
}
