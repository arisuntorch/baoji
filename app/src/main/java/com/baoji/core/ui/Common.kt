package com.baoji.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji

/** Bare icon button: no ripple, 44dp touch target. */
@Composable
fun BareIconButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 24.dp, onClick: () -> Unit) {
    Box(
        modifier.size(44.dp).pressable(label = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Baoji.colors.primaryText, modifier = Modifier.size(size))
    }
}

/** Secondary-page header: back arrow on the left, optional title. */
@Composable
fun PageHeader(title: String?, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        // The 44dp touch box insets its 24dp icon by 10dp; offset so the icon lines up with the page edge.
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = PagePadding - 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BareIconButton(LineIcons.Back, "返回", onClick = onBack)
        Spacer(Modifier.width(4.dp))
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Baoji.colors.primaryText)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun BaojiCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Baoji.colors.card)
            .padding(16.dp),
        content = content,
    )
}

/** A quiet full-width text button, e.g. "＋ 加练". */
@Composable
fun QuietButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(56.dp).pressable(label = text, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Baoji.colors.secondaryText)
    }
}

/** Solid primary-text capsule used for the few explicit actions (save, confirm). */
@Composable
fun SolidButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Baoji.colors
    ActionCapsule(
        text, modifier, enabled, onClick,
        background = if (enabled) c.primaryText else c.fill,
        content = if (enabled) c.background else c.secondaryText,
    )
}

/** Grey capsule with the same size as [SolidButton], for the second action of a pair. */
@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Baoji.colors
    ActionCapsule(
        text, modifier, enabled, onClick,
        background = c.fill,
        content = if (enabled) c.primaryText else c.secondaryText,
    )
}

@Composable
private fun ActionCapsule(
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
    background: Color,
    content: Color,
) {
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(background)
            .pressable(enabled = enabled, label = text, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

/** Floating icon + label capsule for the top bar; same height as the avatar beside it. */
@Composable
fun FloatingPill(icon: ImageVector, text: String, label: String = text, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Baoji.colors
    Row(
        modifier
            .height(TopBarItemSize)
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(c.card)
            .border(0.5.dp, c.separator, CircleShape)
            .pressable(label = label, onClick = onClick)
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.primaryText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.primaryText)
    }
}

/** Large line icon on a soft rounded square; leads each exercise / meal card. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(Baoji.colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Baoji.colors.primaryText, modifier = Modifier.size(34.dp))
    }
}

@Composable
fun OutlineChoice(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Baoji.colors
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) c.primaryText else c.fill)
            .pressable(label = text, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) c.background else c.primaryText)
    }
}

val PagePadding = 20.dp
val CardShape = RoundedCornerShape(16.dp)

/** Gap between stacked bubbles in a list. */
val BubbleGap = 10.dp

/** Height of the floating top-bar controls (import/export pill, avatar). */
val TopBarItemSize = 48.dp

/** Space the tab pages leave at the top for the floating bar. */
val TopBarHeight = TopBarItemSize + 16.dp
val ItemSpacing = Arrangement.spacedBy(12.dp)
