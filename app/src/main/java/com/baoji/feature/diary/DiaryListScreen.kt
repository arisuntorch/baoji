package com.baoji.feature.diary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.PageHeader
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.pressable

@Composable
fun DiaryListScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, vm: DiaryListViewModel = hiltViewModel()) {
    val c = Baoji.colors
    val items by vm.items.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        PageHeader("日记", onBack)
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = PagePadding),
        ) {
            items(items, key = { it.date.toEpochDay() }) { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pressable(label = "${item.date} 的日记") { onOpen(item.date.toEpochDay()) }
                        .padding(vertical = 14.dp),
                ) {
                    Text(
                        "${item.date.monthValue}/${item.date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.primaryText,
                    )
                    if (item.preview.isNotEmpty()) {
                        Text(
                            item.preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = c.separator)
            }
        }
    }
}
