package com.baoji.feature.shell

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.rememberReduceMotion
import com.baoji.data.io.ImportKind
import com.baoji.feature.diary.DiaryEditScreen
import com.baoji.feature.diary.DiaryListScreen
import com.baoji.feature.importer.ImportScreen
import com.baoji.feature.onboarding.OnboardingScreen
import com.baoji.feature.profile.ProfileScreen
import com.baoji.feature.weight.WeightScreen

@Composable
fun AppRoot(vm: RootViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(Baoji.colors.background)) {
        if (state != RootState.LOADING) {
            // Decide the start destination once; onboarding navigates on its own when done.
            val start = remember { if (state == RootState.READY) Routes.TABS else Routes.ONBOARDING }
            AppNavHost(start)
        }
    }
}

@Composable
private fun AppNavHost(start: String) {
    val nav = rememberNavController()
    val reduce = rememberReduceMotion()
    val enter = if (reduce) fadeIn(tween(0)) else fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }
    val exit = if (reduce) fadeOut(tween(0)) else fadeOut(tween(160))
    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { if (reduce) fadeIn(tween(0)) else fadeIn(tween(220)) },
        popExitTransition = { exit },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                nav.navigate(Routes.TABS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.TABS) {
            TabsScreen(
                onOpenProfile = { nav.navigate(Routes.PROFILE) },
                onOpenDiary = { nav.navigate(Routes.DIARY) },
                onOpenWeight = { nav.navigate(Routes.WEIGHT) },
                onImport = { kind -> nav.navigate(Routes.import(kind)) },
            )
        }
        composable(Routes.DIARY) {
            DiaryListScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.diaryEdit(it)) })
        }
        composable(Routes.DIARY_EDIT, arguments = listOf(navArgument("date") { type = NavType.LongType })) {
            DiaryEditScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.WEIGHT) { WeightScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onCleared = {
                    nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(Routes.IMPORT, arguments = listOf(navArgument("kind") { type = NavType.StringType })) { entry ->
            val kind = ImportKind.fromWire(entry.arguments?.getString("kind").orEmpty()) ?: ImportKind.TRAINING
            ImportScreen(kind = kind, onDone = { nav.popBackStack() })
        }
    }
}
