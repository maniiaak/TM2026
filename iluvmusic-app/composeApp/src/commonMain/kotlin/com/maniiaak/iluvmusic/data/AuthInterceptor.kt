package com.maniiaak.iluvmusic.data

import io.ktor.client.HttpClient
import com.maniiaak.iluvmusic.auth.FirebaseAuthManager

/**
 * Placeholder function for common API. The actual interceptor is implemented in platform-specific code
 * because Ktor 3.x pipeline APIs are not available in commonMain.
 */
@Suppress("UNUSED_PARAMETER")
fun HttpClient.configureAuthInterceptor(firebaseAuthManager: FirebaseAuthManager) {
    // No-op in common - platform-specific implementations add the interceptor
}