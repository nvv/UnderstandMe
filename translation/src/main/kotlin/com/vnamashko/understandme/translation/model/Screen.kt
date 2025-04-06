package com.vnamashko.understandme.translation.model

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object InteractiveTranslate : Screen("interactiveTranslate")
    data object Listen : Screen("listen")
    data object ListenResults : Screen("listenResults")
}