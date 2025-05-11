package com.vnamashko.understandme.translation.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {

    @Serializable
    data object Home : Screen

    @Serializable
    data object InteractiveTranslate : Screen

    @Serializable
    data object Listen : Screen

    @Serializable
    data object ListenResults : Screen
}