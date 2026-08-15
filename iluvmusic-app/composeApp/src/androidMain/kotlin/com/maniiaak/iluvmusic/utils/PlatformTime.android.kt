package com.maniiaak.iluvmusic.utils

import java.lang.System

actual fun getCurrentTimestamp(): Long {
    return System.currentTimeMillis()
}