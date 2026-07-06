package com.polli.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val engine = DesktopEngine()
    application {
        Window(
            onCloseRequest = {
                engine.close()
                exitApplication()
            },
            title = "Polli Desktop",
            state = rememberWindowState(width = 960.dp, height = 720.dp),
        ) {
            PolliDesktopApp(engine = engine)
        }
    }
}
