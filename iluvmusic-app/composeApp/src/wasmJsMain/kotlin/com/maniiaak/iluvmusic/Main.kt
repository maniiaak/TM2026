package com.maniiaak.iluvmusic

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeViewport
import com.maniiaak.iluvmusic.di.initKoin
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        initKoin()
        // Use device pixel ratio for proper scaling, fallback to 1.0
        val density = (window.devicePixelRatio ?: 1.0).coerceIn(1.0, 2.0)
        ComposeViewport("composeApp") {
            CompositionLocalProvider(LocalDensity provides Density(density = density)) {
                App()
            }
        }
    } catch (e: Throwable) {
        println("Startup failure: ${e.message}")
        e.printStackTrace()
    }
}