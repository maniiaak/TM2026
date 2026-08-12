// File: androidMain/kotlin/com/jetbrains/kmpapp/utils/PlatformTime.android.kt
package com.jetbrains.kmpapp.utils

import java.lang.System

actual fun getCurrentTimestamp(): Long {
    return System.currentTimeMillis()
}