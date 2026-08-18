package com.maniiaak.iluvmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.window

@Composable
actual fun AppContentContainer(content: @Composable () -> Unit) {
    val screenWidth = window.innerWidth
    val contentWidth = if (screenWidth < 650) screenWidth.dp else 650.dp

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(contentWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}