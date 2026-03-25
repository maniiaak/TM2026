// File: iosMain/kotlin/com/jetbrains/kmpapp/utils/PlatformTime.ios.kt
package com.jetbrains.kmpapp.utils

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentTimestamp(): Long {
    // NSDate.timeIntervalSince1970 returns seconds (Double), convert to milliseconds (Long)
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

