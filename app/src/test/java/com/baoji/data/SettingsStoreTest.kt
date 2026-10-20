package com.baoji.data

import com.baoji.data.prefs.ThemeMode
import com.baoji.testing.TestEnv
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsStoreTest {

    private val env = TestEnv()

    @After
    fun tearDown() = env.close()

    @Test
    fun themeFollowsSystemUntilChosen() = runBlocking {
        assertThat(env.settings.themeMode.first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun chosenThemeIsRemembered() = runBlocking {
        env.settings.setThemeMode(ThemeMode.LIGHT)
        assertThat(env.settings.themeMode.first()).isEqualTo(ThemeMode.LIGHT)
        env.settings.setThemeMode(ThemeMode.DARK)
        assertThat(env.settings.themeMode.first()).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun clearingDataGoesBackToSystemTheme() = runBlocking {
        env.settings.setThemeMode(ThemeMode.DARK)
        env.settings.clear()
        assertThat(env.settings.themeMode.first()).isEqualTo(ThemeMode.SYSTEM)
    }
}
