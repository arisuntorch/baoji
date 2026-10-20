package com.baoji.core.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
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
            .clip(RoundedCornerShape(14.dp))
            .background(Baoji.colors.card)
            .padding(16.dp),
        content = content,
    )
}

/** A quiet full-width text button, e.g. "＋ 加练". */
@Composable
fun QuietButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(48.dp).pressable(label = text, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Baoji.colors.secondaryText)
    }
}

/** Solid primary-text capsule used for the few explicit actions (save, confirm). */
@Composable
fun SolidButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Baoji.colors
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(if (enabled) c.primaryText else c.fill)
            .pressable(enabled = enabled, label = text, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) c.background else c.secondaryText)
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
val ItemSpacing = Arrangement.spacedBy(12.dp)
