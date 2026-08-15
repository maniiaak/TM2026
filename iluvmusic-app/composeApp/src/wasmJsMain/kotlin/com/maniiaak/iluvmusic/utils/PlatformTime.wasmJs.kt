package com.maniiaak.iluvmusic.utils

actual fun getCurrentTimestamp(): Long = js("Date.now()")