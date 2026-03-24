package org.musicbox.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform