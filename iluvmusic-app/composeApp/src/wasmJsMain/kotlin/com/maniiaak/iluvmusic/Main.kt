package com.maniiaak.iluvmusic

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeViewport
import com.maniiaak.iluvmusic.di.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        initKoin()
        ComposeViewport("composeApp") {
            CompositionLocalProvider(LocalDensity provides Density(density = 1.5f)) {
                App()
            }
        }
    } catch (e: Throwable) {
        println("Startup failure: ${e.message}")
        e.printStackTrace()
    }
}