package com.baoji.core.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes a small avatar off the main thread; falls back to the line person icon. */
@Composable
fun AvatarImage(path: String?, iconSize: Dp, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = path?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(it, bounds)
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= 256 && bounds.outHeight / (sample * 2) >= 256) sample *= 2
                    BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(LineIcons.Person, contentDescription = null, tint = Baoji.colors.secondaryText, modifier = Modifier.size(iconSize))
        }
    }
}
