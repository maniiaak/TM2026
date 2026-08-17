import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeViewport
import com.maniiaak.iluvmusic.App
import com.maniiaak.iluvmusic.di.initKoin
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        initKoin()
        val screenWidth = window.innerWidth
        val scaleFactor = when {
            screenWidth < 600 -> 1f   // narrow/mobile browser
            screenWidth < 1200 -> 1.5f    // typical laptop
            else -> 2f                // large desktop monitor
        }
        ComposeViewport("composeApp") {
            CompositionLocalProvider(LocalDensity provides Density(density = scaleFactor)) {
                App()
            }
        }
    } catch (e: Throwable) {
        println("Startup failure: ${e.message}")
        e.printStackTrace()
    }
}