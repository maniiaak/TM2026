package com.maniiaak.iluvmusic.config

import com.maniiaak.iluvmusic.BuildConfig

actual object ApiConfig {
    actual val BASE_URL: String = "${BuildConfig.API_BASE_URL}/api"
}