package com.maniiaak.iluvmusic

import androidx.compose.runtime.Composable

@Composable
actual fun AppContentContainer(content: @Composable () -> Unit) {
    content()
}