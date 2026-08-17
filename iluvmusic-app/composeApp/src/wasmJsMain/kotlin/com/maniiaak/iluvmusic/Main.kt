package com.maniiaak.iluvmusic

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.maniiaak.iluvmusic.di.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        initKoin()
        ComposeViewport("composeApp") {
            App()
        }
    } catch (e: Throwable) {
        println("Startup failure: ${e.message}")
        e.printStackTrace()
    }
}