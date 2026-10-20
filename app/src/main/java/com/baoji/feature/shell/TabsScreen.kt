package com.baoji.feature.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.AvatarImage
import com.baoji.core.ui.FloatingPill
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.TopBarHeight
import com.baoji.core.ui.TopBarItemSize
import com.baoji.core.ui.pressable
import com.baoji.core.ui.rememberReduceMotion
import com.baoji.data.io.ImportKind
import com.baoji.feature.diet.DietScreen
import com.baoji.feature.home.ExportSheet
import com.baoji.feature.home.HomeScreen
import com.baoji.feature.training.TrainingScreen

@Composable
fun TabsScreen(
    onOpenProfile: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenWeight: () -> Unit,
    onImport: (ImportKind) -> Unit,
    vm: RootViewModel = hiltViewModel(),
) {
    val avatarPath by vm.avatarPath.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    val reduce = rememberReduceMotion()

    Box(Modifier.fillMaxSize().background(Baoji.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 40 }) togetherWith fadeOut(tween(120))
                },
                modifier = Modifier.weight(1f),
                label = "tab",
            ) { current ->
                Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = TopBarHeight)) {
                    when (current) {
                        Tab.TRAINING -> TrainingScreen()
                        Tab.HOME -> HomeScreen(onOpenDiary = onOpenDiary, onOpenWeight = onOpenWeight)
                        Tab.DIET -> DietScreen()
                    }
                }
            }
            BottomBar(tab) { tab = it }
        }

        // Floating top bar: export on home, import on the two content tabs; avatar on the right.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = PagePadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (tab) {
                Tab.HOME -> FloatingPill(LineIcons.Export, "导出") { showExport = true }
                Tab.TRAINING -> FloatingPill(LineIcons.Import, "导入", label = "导入训练计划") { onImport(ImportKind.TRAINING) }
                Tab.DIET -> FloatingPill(LineIcons.Import, "导入", label = "导入饮食计划") { onImport(ImportKind.DIET) }
            }
            Spacer(Modifier.weight(1f))
            Avatar(avatarPath, onClick = onOpenProfile)
        }
    }

    if (showExport) ExportSheet(onDismiss = { showExport = false })
}

@Composable
private fun Avatar(path: String?, onClick: () -> Unit) {
    val c = Baoji.colors
    Box(
        Modifier
            .size(TopBarItemSize)
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(c.card)
            .border(0.5.dp, c.separator, CircleShape)
            .pressable(label = "个人页", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AvatarImage(path, iconSize = 26.dp, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val c = Baoji.colors
    Column(Modifier.fillMaxWidth().background(c.card)) {
        HorizontalDivider(thickness = 0.5.dp, color = c.separator)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SideTab("训练", LineIcons.Dumbbell, current == Tab.TRAINING, Modifier.weight(1f)) { onSelect(Tab.TRAINING) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CenterTab(current == Tab.HOME) { onSelect(Tab.HOME) }
            }
            SideTab("饮食", LineIcons.Bowl, current == Tab.DIET, Modifier.weight(1f)) { onSelect(Tab.DIET) }
        }
    }
}

@Composable
private fun SideTab(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Baoji.colors
    val tint = if (selected) c.primaryText else c.secondaryText
    Column(
        modifier.semantics { this.selected = selected }.pressable(label = label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** Raised round home button that sits above the bar. */
@Composable
private fun CenterTab(selected: Boolean, onClick: () -> Unit) {
    val c = Baoji.colors
    Box(
        Modifier
            .offset(y = (-14).dp)
            .size(60.dp)
            .shadow(8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(if (selected) c.primaryText else c.card)
            .border(0.5.dp, c.separator, CircleShape)
            .semantics { this.selected = selected }
            .pressable(label = "主页", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(LineIcons.Home, contentDescription = null, tint = if (selected) c.background else c.primaryText, modifier = Modifier.size(26.dp))
    }
}
